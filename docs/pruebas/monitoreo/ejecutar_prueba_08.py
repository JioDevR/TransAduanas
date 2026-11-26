from pathlib import Path
from datetime import datetime, timezone, timedelta
import argparse
import base64
import csv
import json
import math
import re
import shutil
import time
import urllib.request
import urllib.error
import urllib.parse
import uuid

parser=argparse.ArgumentParser()
parser.add_argument('--proyecto',default=r'C:\Users\asus\Music\TransAduanas')
args=parser.parse_args()
root=Path(args.proyecto)
tz=timezone(timedelta(hours=-5))
created=datetime.now(tz)
marker='M08-'+created.strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6]
out=root/'evidencias-monitoreo'/marker
out.mkdir(parents=True,exist_ok=False)
shutil.copy2(__file__,out/Path(__file__).name)
private=json.loads((Path.home()/'Music/TransAduanas-herramientas/monitoreo/credenciales-locales.json').read_text(encoding='utf-8-sig'))
auth='Basic '+base64.b64encode((private['usuario']+':'+private['password']).encode()).decode()
del private
job='job="transaduanas-backend"'
expressions={
    'HEAP':'sum(jvm_memory_used_bytes{'+job+',area="heap"})',
    'NONHEAP':'sum(jvm_memory_used_bytes{'+job+',area="nonheap"})',
    'GCCOUNT':'sum(jvm_gc_pause_seconds_count{'+job+'})',
    'GCSUM':'sum(jvm_gc_pause_seconds_sum{'+job+'})',
    'GCRATE':'sum(rate(jvm_gc_pause_seconds_sum{'+job+'}[5m]))',
    'UP':'up{'+job+'}',
    'START':'process_start_time_seconds{'+job+'}'
}
(out/'criterios-previos.txt').write_text(
    'M08: memoria JVM y pausas GC bajo carga de lectura baja, 60 GET al catalogo de puertos, secuenciales y una por segundo.\n'
    '60 respuestas HTTP 200/success=true y IDs validos distintos; sin modificar datos de negocio ni configuracion.\n'
    'Metricas nativas heap/nonheap y contadores GC presentes, finitos, no negativos; heap > 0.\n'
    'Siete muestras en Prometheus/Grafana (antes, cada 10 s y final), con valores finitos, target UP y mismo inicio de JVM.\n'
    'Para comparar Prometheus con Grafana se fija el mismo instante de evaluacion; tolerancia numerica 1e-9 relativa o 1e-9 absoluta.\n'
    'Los contadores GC no deben retroceder; si no hay pausas nuevas se registra cero, no un fallo artificial. No se invoca System.gc ni jcmd GC.run.\n'
    'La ultima recoleccion Prometheus debe incluir al menos los contadores GC observados al finalizar; espera hasta 20 s por scrape de 5 s.\n'
    'No se compara el heap vivo contra una muestra anterior de Prometheus; sus instantes difieren.\n'
    'No se fija un SLA ni se exige que aumente el heap. No acredita ausencia de fugas, estres, full GC ni comportamiento de larga duracion.\n',encoding='utf-8')

def api(url,payload=None,grafana=False):
    headers={'Authorization':auth} if grafana else {}
    if payload is not None:headers['Content-Type']='application/json'
    request=urllib.request.Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=headers)
    with urllib.request.urlopen(request,timeout=10) as response:return json.load(response)

def prom(expr,when=None):
    params={'query':expr}
    if when is not None:params['time']=str(when)
    reply=api('http://127.0.0.1:9090/api/v1/query?'+urllib.parse.urlencode(params))
    values=reply['data']['result']
    return float(values[0]['value'][1]) if values else None

def native():
    with urllib.request.urlopen('http://127.0.0.1:9086/actuator/prometheus',timeout=10) as response:text=response.read().decode()
    values={'HEAP':0.0,'NONHEAP':0.0,'GCCOUNT':0.0,'GCSUM':0.0,'START':None}
    found=set()
    for line in text.splitlines():
        field=None
        if line.startswith('jvm_memory_used_bytes{'):
            field='HEAP' if 'area="heap"' in line else 'NONHEAP' if 'area="nonheap"' in line else None
        elif line.startswith('jvm_gc_pause_seconds_count{'):field='GCCOUNT'
        elif line.startswith('jvm_gc_pause_seconds_sum{'):field='GCSUM'
        elif line.startswith('process_start_time_seconds{'):field='START'
        if field:
            value=float(line.rsplit(' ',1)[1]);found.add(field)
            if field=='START':values[field]=value
            else:values[field]+=value
    return values,text,found

native_before,raw_before,found_before=native()
if prom(expressions['UP'])!=1:raise RuntimeError('Target no disponible; no se inicia la carga.')
dashboard=api('http://127.0.0.1:3000/api/dashboards/uid/transaduanas-monitoreo',grafana=True)
panels=dashboard['dashboard']['panels']
panel_evidence=[p for p in panels if any('jvm_memory_used_bytes' in t.get('expr','') or 'jvm_gc_pause_seconds_sum' in t.get('expr','') for t in p.get('targets',[]))]
if len(panel_evidence)<2:raise RuntimeError('Faltan paneles de memoria o GC; no se inicia la carga.')
(out/'paneles-configurados.json').write_text(json.dumps(panel_evidence,ensure_ascii=False,indent=2),encoding='utf-8')
snapshots=[];raw_graf=[];issues=[]
t0=time.perf_counter()
def snapshot():
    # Fixed evaluation instant avoids comparing different scrapes across an update.
    now_ms=int(time.time()*1000)-1000
    reply=api('http://127.0.0.1:3000/api/ds/query',{
        'from':str(now_ms-300000),'to':str(now_ms),
        'queries':[{'refId':key,'datasource':{'uid':'transaduanas-prometheus','type':'prometheus'},'expr':expr,
                    'instant':True,'range':False,'format':'time_series','intervalMs':5000,'maxDataPoints':100}
                   for key,expr in expressions.items()]},True)
    row={'Segundos':round(time.perf_counter()-t0,3),'InstanteEvaluacionUnix':now_ms/1000}
    for key,expr in expressions.items():
        values=[]
        item=reply.get('results',{}).get(key,{})
        if item.get('error'):issues.append(key+': error en consulta Grafana')
        for frame in item.get('frames',[]):
            for index,field in enumerate(frame.get('schema',{}).get('fields',[])):
                if field.get('type')=='number':values.extend(frame.get('data',{}).get('values',[])[index])
        row['Grafana_'+key]=values[-1] if values else None
        row['Prometheus_'+key]=prom(expr,now_ms/1000)
    snapshots.append(row);raw_graf.append(reply)
    heap=row['Grafana_HEAP']
    print('M08 t='+str(row['Segundos'])+'s; heap MiB='+str(round(heap/1048576,3) if isinstance(heap,(int,float)) else None)+
          '; pausas GC acumuladas='+str(row['Grafana_GCCOUNT'])+'; segundos GC acumulados='+str(row['Grafana_GCSUM']),flush=True)
    return row

snapshot()
requests=[]
previous=t0-1
next_panel=10
print('M08: 60 lecturas normales, una por segundo; sin forzar GC.',flush=True)
for number in range(1,61):
    due=max(t0+number-1,previous+1)
    time.sleep(max(0,due-time.perf_counter()))
    previous=time.perf_counter()
    status=0;success=False;rid='';error=''
    try:
        request=urllib.request.Request('http://127.0.0.1:5180/api/v1/catalogos/puertos?monitoreo='+marker+'&consulta='+str(number),headers={'User-Agent':'TransAduanas-M08-JVM-GC'})
        with urllib.request.urlopen(request,timeout=5) as response:
            status=response.status;rid=response.headers.get('X-Request-ID','')
            success=json.load(response).get('success') is True
    except urllib.error.HTTPError as exc:status=exc.code;error='HTTP '+str(exc.code)
    except Exception as exc:error=type(exc).__name__
    requests.append({'Consulta':number,'SegundosEnvio':round(previous-t0,6),'HTTP':status,'SuccessJSON':success,'RequestId':rid,'Error':error})
    if time.perf_counter()-t0>=next_panel:
        snapshot();next_panel+=10
time.sleep(max(0,max(t0+60,previous+1)-time.perf_counter()))
window=time.perf_counter()-t0
native_after,raw_after,found_after=native()
for attempt in range(11):
    pc=prom(expressions['GCCOUNT']);ps=prom(expressions['GCSUM'])
    if pc is not None and ps is not None and pc>=native_after['GCCOUNT'] and ps+1e-12>=native_after['GCSUM']:break
    time.sleep(2)
# Allow the fixed evaluation instant to include the last scrape.
time.sleep(1.1)
snapshot()
def finite(value):return isinstance(value,(int,float)) and math.isfinite(value) and value>=0
required={'HEAP','NONHEAP','GCCOUNT','GCSUM','START'}
checks={
    'HTTP60Correctos':len(requests)==60 and all(r['HTTP']==200 and r['SuccessJSON'] for r in requests),
    'IDs60Distintos':len({r['RequestId'] for r in requests if re.fullmatch('[a-f0-9]{32}',r['RequestId'])})==60,
    'MetricasNativasValidas':required<=found_before and required<=found_after and all(finite(v) for v in list(native_before.values())+list(native_after.values())) and native_before['HEAP']>0 and native_after['HEAP']>0,
    'SieteMuestras':len(snapshots)>=7,
    'MetricasRecolectadasValidas':all(finite(row[engine+'_'+key]) for row in snapshots for engine in ['Grafana','Prometheus'] for key in expressions),
    'PrometheusGrafanaCoinciden':all(finite(row['Grafana_'+key]) and finite(row['Prometheus_'+key]) and math.isclose(row['Grafana_'+key],row['Prometheus_'+key],rel_tol=1e-9,abs_tol=1e-9) for row in snapshots for key in expressions),
    'JVMEstable':native_before['START']==native_after['START'] and all(row['Prometheus_START']==native_before['START'] and row['Prometheus_UP']==1 for row in snapshots),
    'GCNoRetrocede':all(all(finite(a['Prometheus_'+key]) and finite(b['Prometheus_'+key]) and b['Prometheus_'+key]>=a['Prometheus_'+key] for a,b in zip(snapshots,snapshots[1:])) for key in ['GCCOUNT','GCSUM']),
    'GCFInalRecolectado':finite(snapshots[-1]['Prometheus_GCCOUNT']) and finite(snapshots[-1]['Prometheus_GCSUM']) and snapshots[-1]['Prometheus_GCCOUNT']>=native_after['GCCOUNT'] and snapshots[-1]['Prometheus_GCSUM']+1e-12>=native_after['GCSUM'],
    'SinErroresConsultasGrafana':not issues
}
ok=all(checks.values())
heap=[row['Prometheus_HEAP'] for row in snapshots if finite(row['Prometheus_HEAP'])]
delta_gc=native_after['GCCOUNT']-native_before['GCCOUNT']
delta_seconds=native_after['GCSUM']-native_before['GCSUM']
text=(f'{sum(r["HTTP"]==200 and r["SuccessJSON"] for r in requests)}/60 lecturas correctas; {len(snapshots)} muestras. '
      f'Heap observado en Prometheus/Grafana: {min(heap)/1048576:.3f} a {max(heap)/1048576:.3f} MiB. '
      f'Pausas GC nuevas entre capturas nativas: {delta_gc:g}; tiempo acumulado adicional: {delta_seconds*1000:.3f} ms. '
      'Sin GC forzado.')
result={'Prueba':8,'CodigoPrueba':'M08','Nombre':'Memoria JVM y pausas de GC','Importancia':'Media',
    'Identificador':marker,'InicioLima':created.isoformat(),'FinLima':datetime.now(tz).isoformat(),
    'Ejecucion':'REALIZADA: 60 lecturas secuenciales y muestreo de memoria y GC en Micrometer, Prometheus y fuente de Grafana.',
    'Herramientas':['Spring Boot Actuator / Micrometer JVM','Prometheus 3.13.5','Grafana OSS 13.3.1','Python (HTTP, muestreo y contraste)'],
    'Estado':'EXITOSO' if ok else 'FALLO','Resultado':text,
    'Incidencia':'' if ok else 'Verificaciones incumplidas: '+', '.join(key for key,value in checks.items() if not value),
    'Verificaciones':checks,'MetricasNativasAntes':native_before,'MetricasNativasDespues':native_after,
    'PausasGCNuevas':delta_gc,'TiempoGCAdicionalMs':round(delta_seconds*1000,6),
    'HeapMinimoMiB':round(min(heap)/1048576,6),'HeapMaximoMiB':round(max(heap)/1048576,6),
    'VentanaEnvioSegundos':window,'Muestras':len(snapshots),'ConsultasPromQL':expressions,
    'Alcance':'Recoleccion y consulta de memoria y pausas de GC bajo baja carga. Cero pausas nuevas es una observacion valida; no se exige un evento GC artificial. No acredita ausencia de fugas, estres, full GC ni estabilidad prolongada. El heap nativo y el recolectado tienen instantes distintos; se contrastan Prometheus y Grafana con el mismo instante de evaluacion.',
    'Cambios':'Sin cambios de configuracion, servicios o datos de negocio. No se invoco recoleccion manual ni se reinicio la JVM.'}
for name,data in [('resultado.json',result),('grafana-respuestas.json',raw_graf)]:
    (out/name).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
for name,data in [('metricas-antes.prom',raw_before),('metricas-despues.prom',raw_after)]:
    (out/name).write_text(data,encoding='utf-8')
for name,rows in [('consultas.csv',requests),('muestras-prometheus-grafana.csv',snapshots)]:
    with (out/name).open('w',encoding='utf-8-sig',newline='') as file:
        writer=csv.DictWriter(file,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
(out/'resultado.txt').write_text('M08 - MEMORIA JVM Y PAUSAS DE GC\nESTADO: '+result['Estado']+'\n'+text+'\n'+result['Alcance']+'\n',encoding='utf-8')
print('Estado: '+result['Estado']+'\n'+text+'\nEvidencias: '+str(out),flush=True)
