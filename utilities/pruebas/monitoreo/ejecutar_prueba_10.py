from pathlib import Path
from datetime import datetime, timezone, timedelta
import argparse
import base64
import csv
import hashlib
import json
import math
import msvcrt
import os
import re
import shutil
import subprocess
import threading
import time
import urllib.request
import urllib.error
import urllib.parse
import uuid
import xml.etree.ElementTree as ET

parser=argparse.ArgumentParser()
parser.add_argument('--proyecto',default=r'C:\Users\asus\Music\TransAduanas')
args=parser.parse_args()
root=Path(args.proyecto)
lock=(root/'monitoreo/M10-ejecucion.lock').open('a+b')
lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
except OSError:raise RuntimeError('M10 ya esta en ejecucion; no se inicia otra carga.') from None
tz=timezone(timedelta(hours=-5));created=datetime.now(tz)
marker='M10-'+created.strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6]
out=root/'evidencias-monitoreo'/marker;out.mkdir(parents=True,exist_ok=False)
shutil.copy2(__file__,out/Path(__file__).name)
from leer_almacenamiento_backend import leer_almacenamiento_backend
try:
    storage,storage_source=leer_almacenamiento_backend(Path.home()/'Music/TransAduanas-herramientas/winsw/TransAduanasBackend.xml')
except (OSError,ValueError,RuntimeError,subprocess.TimeoutExpired):
    reason='M10 no ejecutada: no se pudo identificar la ruta de almacenamiento de la configuracion actual; no se subieron archivos.'
    (out/'resultado.json').write_text(json.dumps({'CodigoPrueba':'M10','Estado':'NO_EJECUTADA','Identificador':marker,'Resultado':reason,'Fase':'Preparacion','InicioLima':created.isoformat()},ensure_ascii=False,indent=2),encoding='utf-8')
    raise SystemExit(reason) from None
shutil.copy2(Path(__file__).with_name('leer_almacenamiento_backend.py'),out/'leer_almacenamiento_backend.py')
(out/'origen-almacenamiento.json').write_text(json.dumps({'Directorio':str(storage),'Origen':storage_source},ensure_ascii=False,indent=2),encoding='utf-8')
if storage.drive.upper()!='C:':raise RuntimeError('El almacenamiento no corresponde al volumen C:; revisar antes de ejecutar.')
private=json.loads((Path.home()/'Music/TransAduanas-herramientas/monitoreo/credenciales-locales.json').read_text(encoding='utf-8-sig'))
graf_auth='Basic '+base64.b64encode((private['usuario']+':'+private['password']).encode()).decode();del private
backend=Path.home()/'Music/TransAduanas-desplegado/logs/sga-backend.log'
nginx=Path.home()/'Downloads/nginx-1.31.8/nginx-1.31.8/logs/access.log'
selector='job="transaduanas-backend",method="POST",uri="/api/v1/documentos/upload",status="201"'
expressions={'CARGAS':'sum(http_server_requests_seconds_count{'+selector+'})',
    'LIBRE':'sum(disk_free_bytes{job="transaduanas-backend"})','TOTAL':'sum(disk_total_bytes{job="transaduanas-backend"})'}
def api(url,payload=None,headers=None,method=None):
    h=dict(headers or {})
    if payload is not None:h['Content-Type']='application/json'
    request=urllib.request.Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=h,method=method)
    with urllib.request.urlopen(request,timeout=15) as response:return response.status,dict(response.headers),json.load(response)
def prom(expr,when=None):
    params={'query':expr}
    if when is not None:params['time']=when
    _,_,reply=api('http://127.0.0.1:9090/api/v1/query?'+urllib.parse.urlencode(params))
    values=reply['data']['result'];return float(values[0]['value'][1]) if values else None
def native():
    with urllib.request.urlopen('http://127.0.0.1:9086/actuator/prometheus',timeout=10) as response:text=response.read().decode()
    count=sum(float(line.rsplit(' ',1)[1]) for line in text.splitlines() if line.startswith('http_server_requests_seconds_count{') and all(label in line for label in ['method="POST"','uri="/api/v1/documentos/upload"','status="201"']))
    return count,text
panels=[];graf_raw=[]
def panel():
    ts=int(time.time()*1000)-1000
    _,_,reply=api('http://127.0.0.1:3000/api/ds/query',{'from':str(ts-300000),'to':str(ts),
        'queries':[{'refId':key,'datasource':{'uid':'transaduanas-prometheus','type':'prometheus'},'expr':expr,'instant':True,'range':False,'format':'time_series','intervalMs':5000,'maxDataPoints':100} for key,expr in expressions.items()]},{'Authorization':graf_auth})
    row={'InstanteEvaluacionUnix':ts/1000}
    for key,expr in expressions.items():
        values=[]
        for frame in reply.get('results',{}).get(key,{}).get('frames',[]):
            for i,field in enumerate(frame.get('schema',{}).get('fields',[])):
                if field.get('type')=='number':values.extend(frame['data']['values'][i])
        row['Grafana_'+key]=values[-1] if values else 0 if key=='CARGAS' else None
        value=prom(expr,ts/1000);row['Prometheus_'+key]=value if value is not None else 0 if key=='CARGAS' else None
    panels.append(row);graf_raw.append(reply)
    return row
def disk():
    command="$d=Get-CimInstance Win32_PerfRawData_PerfDisk_LogicalDisk -Filter \"Name='C:'\"; if(-not $d){throw 'Contadores disco no disponibles'}; $d | Select-Object Name,DiskWriteBytesPersec,DiskWritesPersec,AvgDisksecPerWrite,AvgDisksecPerWrite_Base,Frequency_PerfTime,Timestamp_PerfTime | ConvertTo-Json -Compress"
    completed=subprocess.run(['powershell.exe','-NoProfile','-Command',command],capture_output=True,text=True,check=True)
    return json.loads(completed.stdout.strip().lstrip('\ufeff'))

(out/'criterios-previos.txt').write_text(
    'M10: tres cargas normales de archivos sinteticos TXT de 524288 bytes, separados por diez segundos, sin concurrencia.\n'
    'Se crean documentos GENERAL no asociados a clientes, cotizaciones ni expedientes reales.\n'
    'Cada carga debe responder HTTP 201/success=true; tamano API, archivo almacenado y descarga deben coincidir con SHA256 del contenido sintetico.\n'
    'Un evento de almacenamiento y uno HTTP Java por requestId; un acceso Nginx por carga.\n'
    'Micrometer, Prometheus y consultas a la fuente Grafana deben registrar un incremento de tres POST exitosos. Espera final hasta 20 s por scrape.\n'
    'Contadores de disco C: muestreados cada cinco segundos por unos 40 s. Bytes escritos y escrituras deben crecer; la latencia media debe ser calculable en intervalos con operaciones.\n'
    'Bytes/s = delta DiskWriteBytesPersec / delta tiempo. Latencia = ((delta AvgDisksecPerWrite)/Frequency_PerfTime)/delta AvgDisksecPerWrite_Base. Manejar wrap de numerador y base uint32.\n'
    'Referencia formula: https://learn.microsoft.com/en-us/dotnet/api/system.diagnostics.performancecountertype\n'
    'Los contadores del volumen incluyen otros procesos, logs y evidencia; no se atribuyen exclusivamente los bytes o latencia del disco a las cargas. Cache del SO impide exigir igualdad bytes fisicos vs archivo.\n'
    'El tiempo HTTP no es latencia fisica de disco. No se fuerza flush ni saturacion y no se inventa un SLA.\n'
    'El panel no tiene exporter de I/O Windows: la actividad/latencia del volumen se registra en consola/CSV; Prometheus/Grafana recogen HTTP y espacio de disco.\n'
    'Al terminar se anulan solo los tres IDs creados mediante DELETE de la API. Su borrado es logico: los archivos fisicos quedan preservados por la aplicacion.\n',encoding='utf-8')
disk()  # Check permissions before authenticating or creating documents.
if prom('up{job="transaduanas-backend"}')!=1:raise RuntimeError('Target no disponible; carga no iniciada.')
password=os.environ.get('MONITOR_PASSWORD')
if not password:raise RuntimeError('Falta credencial de laboratorio; carga no iniciada.')
_,_,login=api('http://127.0.0.1:5180/api/v1/auth/login',{'username':os.environ.get('MONITOR_USERNAME','admin'),'password':password})
token=login['data']['token'];del login,password
native_before,metrics_before=native()
for attempt in range(11):
    prom_before=prom(expressions['CARGAS']) or 0
    if prom_before==native_before:break
    time.sleep(1)
else:raise RuntimeError('Linea base HTTP no sincronizada; no se inicia carga.')
panel_before=panel()
offset=backend.stat().st_size
raw_disk=[disk()];disk_errors=[]
ready=threading.Event();stop=threading.Event()
t0=time.perf_counter()
def sample_disk():
    ready.set();next_due=t0+5
    try:
        while not stop.is_set():
            if stop.wait(max(0,next_due-time.perf_counter())):break
            raw_disk.append(disk());next_due+=5
    except Exception as exc:disk_errors.append(type(exc).__name__)
thread=threading.Thread(target=sample_disk,daemon=True);thread.start();ready.wait(5)
uploads=[];cleanups=[]
print('M10: tres archivos sinteticos de 512 KiB; muestras de disco cada cinco segundos.',flush=True)
try:
    for number in range(1,4):
        time.sleep(max(0,t0+5+(number-1)*10-time.perf_counter()))
        name=marker+'-'+str(number)+'.txt'
        line=(marker+' archivo sintetico '+str(number)+' sin datos personales\n').encode()
        content=(line*((524288//len(line))+1))[:524288]
        digest=hashlib.sha256(content).hexdigest()
        boundary='----TransAduanas'+uuid.uuid4().hex
        fields={'categoria':'GENERAL','entidadTipo':'GENERAL','descripcion':'Prueba de monitoreo '+marker}
        parts=[]
        for key,value in fields.items():parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="'+key+'"\r\n\r\n'+value+'\r\n').encode())
        parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="files"; filename="'+name+'"\r\nContent-Type: text/plain\r\n\r\n').encode()+content+b'\r\n')
        body=b''.join(parts)+('--'+boundary+'--\r\n').encode()
        row={'Archivo':name,'Bytes':len(content),'SHA256':digest,'HTTP':0,'SuccessJSON':False,'RequestId':'','IdDocumento':None,'TamanoAPICorrecto':False,'DescargaSHA256Correcta':False,'DuracionHTTPMs':None,'Error':''}
        begin=time.perf_counter()
        try:
            request=urllib.request.Request('http://127.0.0.1:5180/api/v1/documentos/upload?monitoreo='+marker,
                data=body,headers={'Authorization':'Bearer '+token,'Content-Type':'multipart/form-data; boundary='+boundary,'User-Agent':'TransAduanas-M10-Disco-Archivos'})
            with urllib.request.urlopen(request,timeout=20) as response:
                row['HTTP']=response.status;row['RequestId']=response.headers.get('X-Request-ID','');answer=json.load(response)
            row['DuracionHTTPMs']=round((time.perf_counter()-begin)*1000,3)
            row['SuccessJSON']=answer.get('success') is True
            doc=answer.get('data',[{}])[0]
            # Store only test identifiers, not creator or business information.
            if doc.get('nombreOriginal')==name:
                row['IdDocumento']=doc.get('idDocumento');row['TamanoAPICorrecto']=doc.get('tamanoBytes')==len(content)
            del doc,answer
            if row['IdDocumento']:
                req=urllib.request.Request('http://127.0.0.1:5180/api/v1/documentos/'+str(row['IdDocumento'])+'/descargar',headers={'Authorization':'Bearer '+token})
                with urllib.request.urlopen(req,timeout=15) as response:download=response.read()
                row['DescargaSHA256Correcta']=hashlib.sha256(download).hexdigest()==digest;del download
        except urllib.error.HTTPError as exc:row['HTTP']=exc.code;row['Error']='HTTP '+str(exc.code)
        except Exception as exc:row['Error']=type(exc).__name__
        uploads.append(row);del body,content,parts
        print('Carga '+str(number)+': HTTP '+str(row['HTTP'])+'; SHA256 descarga correcto='+str(row['DescargaSHA256Correcta']),flush=True)
        panel()
    time.sleep(max(0,t0+40-time.perf_counter()))
finally:
    stop.set();thread.join(10)
    raw_disk.append(disk())
    # Normal business cleanup: logical annulment of only the test-created documents.
    for row in uploads:
        if row['IdDocumento']:
            cleanup={'IdDocumento':row['IdDocumento'],'HTTP':0,'AnulacionLogica':False,'Error':''}
            try:
                status,_,reply=api('http://127.0.0.1:5180/api/v1/documentos/'+str(row['IdDocumento']),headers={'Authorization':'Bearer '+token},method='DELETE')
                cleanup['HTTP']=status;cleanup['AnulacionLogica']=status==200 and reply.get('success') is True
            except Exception as exc:cleanup['Error']=type(exc).__name__
            cleanups.append(cleanup)
    del token

native_after,metrics_after=native()
for attempt in range(11):
    prom_after=prom(expressions['CARGAS']) or 0
    if prom_after>=native_after:break
    time.sleep(2)
time.sleep(1.1);panel_after=panel()
intervals=[]
for before,after in zip(raw_disk,raw_disk[1:]):
    freq=after['Frequency_PerfTime'];elapsed=(after['Timestamp_PerfTime']-before['Timestamp_PerfTime'])/freq
    count=after['DiskWritesPersec']-before['DiskWritesPersec'];written=after['DiskWriteBytesPersec']-before['DiskWriteBytesPersec']
    base=(after['AvgDisksecPerWrite_Base']-before['AvgDisksecPerWrite_Base'])%(2**32)
    timer=(after['AvgDisksecPerWrite']-before['AvgDisksecPerWrite'])%(2**32)
    intervals.append({'Intervalo':len(intervals)+1,'Segundos':elapsed,'Escrituras':count,'BytesEscritos':written,
        'BytesPorSegundo':written/elapsed if elapsed>0 else None,'LatenciaMediaEscrituraMs':timer/freq/base*1000 if base>0 else None})
with backend.open('rb') as file:file.seek(offset);new_lines=file.read().decode('utf-8',errors='replace').splitlines()
ids={row['RequestId'] for row in uploads if re.fullmatch('[a-f0-9]{32}',row['RequestId'])}
related=[line for line in new_lines if any('request_id='+rid in line for rid in ids)]
access=[line for line in nginx.read_text(encoding='utf-8').splitlines() if marker in line and 'POST /api/v1/documentos/upload?' in line]
storage_checks=[]
for row in uploads:
    stored=[line for line in related if 'request_id='+row['RequestId'] in line and 'Archivo almacenado con éxito: '+row['Archivo']+' -> ' in line]
    completed=[line for line in related if 'request_id='+row['RequestId'] in line and 'request_completed method=POST endpoint=/api/v1/documentos/upload status=201 ' in line]
    matching_access=[line for line in access if 'request_id="'+row['RequestId']+'"' in line and '" 201 ' in line]
    relative=stored[0].split(' -> ',1)[1] if len(stored)==1 else ''
    path=(storage/relative).resolve() if relative else None
    safe=bool(path and path.is_relative_to(storage) and path.is_file() and path.name!=row['Archivo'])
    physical=bool(safe and path.stat().st_size==row['Bytes'] and hashlib.file_digest(path.open('rb'),'sha256').hexdigest()==row['SHA256'])
    storage_checks.append({'Archivo':row['Archivo'],'IdDocumento':row['IdDocumento'],'RequestId':row['RequestId'],'RutaRelativaAlmacenada':relative,
        'EventosAlmacenamiento':len(stored),'EventosHTTPBackend':len(completed),'EventosNginx':len(matching_access),'ArchivoFisicoSHA256Correcto':physical})
def finite(value):return isinstance(value,(int,float)) and math.isfinite(value) and value>=0
checks={
    'TresCargasCorrectas':len(uploads)==3 and all(row['HTTP']==201 and row['SuccessJSON'] and row['TamanoAPICorrecto'] and row['DescargaSHA256Correcta'] for row in uploads),
    'TresArchivosCorrelacionados':len(ids)==3 and all(row['EventosAlmacenamiento']==row['EventosHTTPBackend']==row['EventosNginx']==1 and row['ArchivoFisicoSHA256Correcto'] for row in storage_checks),
    'IncrementoMicrometer3':native_after-native_before==3,
    'IncrementoPrometheus3':prom_after-prom_before==3,
    'IncrementoGrafana3':panel_after['Grafana_CARGAS']-panel_before['Grafana_CARGAS']==3,
    'PrometheusGrafanaCoinciden':all(finite(row['Grafana_'+key]) and finite(row['Prometheus_'+key]) and math.isclose(row['Grafana_'+key],row['Prometheus_'+key],rel_tol=1e-9,abs_tol=1e-9) for row in panels for key in expressions),
    'EspacioDiscoValido':all(0<=row['Prometheus_LIBRE']<=row['Prometheus_TOTAL'] for row in panels),
    'MuestreoDiscoCompleto':len(raw_disk)>=8 and not disk_errors and not thread.is_alive(),
    'ActividadDiscoRegistrada':bool(intervals) and all(row['Segundos']>0 and row['Escrituras']>=0 and row['BytesEscritos']>=0 for row in intervals) and sum(row['BytesEscritos'] for row in intervals)>0,
    'LatenciaDiscoRegistrada':any(finite(row['LatenciaMediaEscrituraMs']) for row in intervals),
    'DocumentosPruebaAnulados':len(cleanups)==3 and all(row['AnulacionLogica'] for row in cleanups)
}
ok=all(checks.values())
latencies=[row['LatenciaMediaEscrituraMs'] for row in intervals if finite(row['LatenciaMediaEscrituraMs'])]
summary=(f'{sum(row["HTTP"]==201 for row in uploads)}/3 cargas HTTP 201 de 512 KiB; '
    f'{sum(row["ArchivoFisicoSHA256Correcto"] for row in storage_checks)}/3 archivos fisicos y descargas verificados. '
    f'{len(raw_disk)} muestras de C:, {sum(row["BytesEscritos"] for row in intervals)} bytes escritos en el volumen. '
    f'Latencia media de escritura por intervalo: {min(latencies):.3f}-{max(latencies):.3f} ms. '
    'Incrementos de cargas Micrometer/Prometheus/Grafana: '+str(native_after-native_before)+'/'+str(prom_after-prom_before)+'/'+str(panel_after['Grafana_CARGAS']-panel_before['Grafana_CARGAS'])+'.')
result={'Prueba':10,'CodigoPrueba':'M10','Nombre':'Actividad de disco y archivos','Importancia':'Media','Identificador':marker,
    'InicioLima':created.isoformat(),'FinLima':datetime.now(tz).isoformat(),
    'Ejecucion':'REALIZADA: tres cargas sinteticas con verificacion de archivos, logs, contadores HTTP y actividad/latencia Windows del volumen C:.',
    'Herramientas':['Windows CIM / contadores PerfDisk LogicalDisk','Nginx y Logback','Spring Boot Actuator / Micrometer','Prometheus 3.13.5','Grafana OSS 13.3.1','Python (HTTP multipart, muestreo, calculo y SHA256)'],
    'Estado':'EXITOSO' if ok else 'FALLO','Resultado':summary,'Incidencia':'' if ok else 'Verificaciones incumplidas: '+', '.join(key for key,value in checks.items() if not value),
    'Verificaciones':checks,'MuestrasDisco':len(raw_disk),'BytesArchivosPrueba':3*524288,'BytesEscritosVolumen':sum(row['BytesEscritos'] for row in intervals),
    'LatenciaMinimaIntervaloMs':min(latencies) if latencies else None,'LatenciaMaximaIntervaloMs':max(latencies) if latencies else None,
    'ContadorCargasAntes':native_before,'ContadorCargasDespues':native_after,'ErroresMuestreoDisco':disk_errors,
    'Alcance':'Los contadores de volumen incluyen otros procesos: su volumen de escritura y latencia no se atribuyen exclusivamente a las cargas. La cache impide exigir igualdad entre bytes fisicos y archivos. Se confirma escritura de la aplicacion mediante hash y logs correlacionados. I/O y latencia Windows se registran en consola/CSV; Grafana consulta HTTP y espacio de disco, sin exporter I/O Windows. No se fuerza flush ni se evalua durabilidad tras corte electrico o capacidad maxima.',
    'Cambios':'Se crearon y anularon logicamente tres documentos GENERAL de prueba sin asociarlos a entidades de negocio. Los archivos fisicos sinteticos (1.5 MiB) se conservan segun el comportamiento de la aplicacion; no se borra directamente informacion del almacenamiento. Login normal puede actualizar ultimoAcceso.'}
for name,data in [('resultado.json',result),('contadores-disco-raw.json',raw_disk),('grafana-respuestas.json',graf_raw),('limpieza-documentos.json',cleanups)]:
    (out/name).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
for name,rows in [('cargas.csv',uploads),('verificacion-archivos.csv',storage_checks),('actividad-latencia-disco.csv',intervals),('prometheus-grafana.csv',panels)]:
    with (out/name).open('w',encoding='utf-8-sig',newline='') as file:
        writer=csv.DictWriter(file,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
for name,text in [('metricas-antes.prom',metrics_before),('metricas-despues.prom',metrics_after),('eventos-backend.log','\n'.join(related)+'\n'),('eventos-nginx.log','\n'.join(access)+'\n')]:
    (out/name).write_text(text,encoding='utf-8')
(out/'resultado.txt').write_text('M10 - ACTIVIDAD DE DISCO Y ARCHIVOS\nESTADO: '+result['Estado']+'\n'+summary+'\n'+result['Alcance']+'\n'+result['Cambios']+'\n',encoding='utf-8')
print('Estado: '+result['Estado']+'\n'+summary+'\nEvidencias: '+str(out),flush=True)
