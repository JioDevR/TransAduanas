from pathlib import Path
from datetime import datetime, timezone, timedelta
import argparse
import base64
import csv
import hashlib
import json
import msvcrt
import shutil
import socket
import subprocess
import time
import urllib.request
import urllib.error
import urllib.parse
import uuid

parser=argparse.ArgumentParser()
parser.add_argument('--proyecto',default=r'C:\Users\asus\Music\TransAduanas')
args=parser.parse_args();root=Path(args.proyecto)
lock=(root/'monitoreo/M11-ejecucion.lock').open('a+b');lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
except OSError:raise RuntimeError('M11 ya esta en curso; no se inicia otra simulacion.') from None
tz=timezone(timedelta(hours=-5));created=datetime.now(tz)
marker='M11-'+created.strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6]
out=root/'evidencias-monitoreo'/marker;out.mkdir(parents=True,exist_ok=False)
shutil.copy2(__file__,out/Path(__file__).name)
lab=out/'nginx-laboratorio'
(lab/'conf').mkdir(parents=True);(lab/'logs').mkdir()
(lab/'temp').mkdir()
nginx=Path.home()/'Downloads/nginx-1.31.8/nginx-1.31.8/nginx.exe'
original_config=nginx.parent/'conf/nginx.conf'
original_hash=hashlib.sha256(original_config.read_bytes()).hexdigest()
targets_file=root/'monitoreo/prometheus/objetivos-pruebas.json'
original_targets=targets_file.read_text(encoding='utf-8-sig')
if json.loads(original_targets):raise RuntimeError('Hay objetivos de otro laboratorio; no se modifican.')
private=json.loads((Path.home()/'Music/TransAduanas-herramientas/monitoreo/credenciales-locales.json').read_text(encoding='utf-8-sig'))
auth='Basic '+base64.b64encode((private['usuario']+':'+private['password']).encode()).decode();del private
services=[('app-landing',15173,'/','pagina','app-landing'),('app-b2b',15174,'/','pagina','app-b2b'),
          ('app-sga',15180,'/','pagina','app-sga'),('api-catalogo-puertos',15180,'/api/v1/catalogos/puertos','api','app-sga')]
live=[('app-landing',5173,'/'),('app-b2b',5174,'/'),('app-sga',5180,'/'),('api-catalogo-puertos',5180,'/api/v1/catalogos/puertos')]
for port in [15173,15174,15180,15999]:
    with socket.socket() as test:
        try:test.bind(('127.0.0.1',port))
        except OSError:raise RuntimeError('Puerto de laboratorio ocupado: '+str(port)) from None

def fetch(url,payload=None,grafana=False):
    headers={'Authorization':auth} if grafana else {}
    if payload is not None:headers['Content-Type']='application/json'
    req=urllib.request.Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=headers)
    with urllib.request.urlopen(req,timeout=10) as response:return json.load(response)
def query(expr,when=None):
    params={'query':expr}
    if when is not None:params['time']=when
    return fetch('http://127.0.0.1:9090/api/v1/query?'+urllib.parse.urlencode(params))['data']['result']
def http(url):
    try:
        with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'TransAduanas-M11-Validacion'}),timeout=4) as response:
            code=response.status;body=response.read()
        valid=(b'<html' in body.lower()) if '/api/' not in url else json.loads(body).get('success') is True
        return code,valid
    except urllib.error.HTTPError as exc:return exc.code,False
    except Exception:return 0,False
def nginx_cmd(*args,check=True):
    result=subprocess.run([str(nginx),'-p',lab.as_posix()+'/', '-c','conf/nginx.conf',*args],cwd=nginx.parent,
        capture_output=True,text=True,creationflags=subprocess.CREATE_NO_WINDOW)
    with (out/'nginx-comandos.log').open('a',encoding='utf-8') as file:file.write(str(args)+'\n'+result.stdout+result.stderr+'\n')
    if check:result.check_returncode()
    return result
def config(fault=False):
    text='worker_processes 1;\nerror_log logs/error.log;\npid logs/nginx.pid;\nevents { worker_connections 128; }\nhttp {\n'
    text+=' include "'+(nginx.parent/'conf/mime.types').as_posix()+'";\n access_log logs/access.log;\n'
    for frontend,port in [('app-landing',15173),('app-b2b',15174),('app-sga',15180)]:
        path=(lab/'raiz-inexistente'/frontend) if fault else root/frontend/'dist'
        text+=' server { listen 127.0.0.1:'+str(port)+'; server_name localhost; root "'+path.as_posix()+'"; index index.html;\n'
        text+=' location / { try_files $uri $uri/ /index.html; }\n'
        text+=' location /api/ { proxy_pass http://127.0.0.1:'+('15999' if fault else '8086')+'; proxy_set_header X-Request-ID $request_id; proxy_set_header Host $host; }\n }\n'
    return text+'}\n'
healthy=config(False);faulty=config(True)
(out/'nginx-saludable.conf').write_text(healthy,encoding='utf-8');(out/'nginx-fallo.conf').write_text(faulty,encoding='utf-8')
(lab/'conf/nginx.conf').write_text(healthy,encoding='utf-8')
(out/'criterios-previos.txt').write_text(
    'M11 disponibilidad de tres paginas y API a traves de Nginx; copia aislada en 15173,15174,15180. Se usa el mismo dist y backend real de lectura.\n'
    'La configuracion y puertos habituales 5173,5174,5180 no se modifican. Backend y MySQL no se detienen.\n'
    'Fase normal: cuatro HTTP 200 con contenido valido, cuatro probe_success=1 y sin alertas del laboratorio.\n'
    'Fallo controlado: raiz estatica inexistente -> 500 por redireccion interna; upstream API sin listener en 15999 -> 502.\n'
    'Las cuatro metricas probe_success deben cambiar a 0 y la regla DisponibilidadHTTPFallida pasar por pending y firing para cada objetivo.\n'
    'Regla for=15s, scrape/evaluacion cada 5s. Plazo del ejecutor: hasta 60s para detectar; no es un SLA del proyecto.\n'
    'Grafana consulta sum(probe_success) y count(ALERTS firing) por ejecucion; debe mostrar 0 objetivos sanos y cuatro alertas en el fallo.\n'
    'Recuperacion: restaurar la configuracion saludable, cuatro HTTP 200, probe_success=1 y ninguna alerta activa/pending de M11; hasta 40s.\n'
    'Puertos habituales deben seguir respondiendo 200 en todas las observaciones, con backend up=1.\n'
    'Se comprueban alertas de estado Prometheus y su consulta en Grafana; no se declara correo, notificaciones externas ni Alertmanager.\n'
    'Los 500/502 esperados son estimulo controlado, no incidencias imprevistas ni prueba fallida por si mismos.\n'
    'Al terminar, retirar solo los targets del laboratorio, restaurar su config y detener solo su Nginx. Mantener el sondeo habitual y su historial.\n',encoding='utf-8')

selector='job="transaduanas-http",ejecucion="'+marker+'"'
probe_expr='sum(probe_success{'+selector+'})'
alert_expr='count(ALERTS{alertname="DisponibilidadHTTPFallida",alertstate="firing",ejecucion="'+marker+'"}) or vector(0)'
timeline=[];graf_raw=[];pending_seen=set();http_rows=[];state_files=[]
t0=time.perf_counter()
def snapshot(phase):
    probes=query('probe_success{'+selector+'}')
    status=query('probe_http_status_code{'+selector+'}')
    _,all_alerts=None,fetch('http://127.0.0.1:9090/api/v1/alerts')['data']['alerts']
    alerts=[a for a in all_alerts if a.get('labels',{}).get('ejecucion')==marker and a.get('labels',{}).get('alertname')=='DisponibilidadHTTPFallida']
    pending_seen.update(a['labels']['servicio'] for a in alerts if a['state']=='pending')
    now_ms=int(time.time()*1000)-1000
    reply=fetch('http://127.0.0.1:3000/api/ds/query',{'from':str(now_ms-300000),'to':str(now_ms),
        'queries':[{'refId':ref,'datasource':{'uid':'transaduanas-prometheus','type':'prometheus'},'expr':expr,'instant':True,'range':False,'format':'time_series','intervalMs':5000,'maxDataPoints':100} for ref,expr in [('SANOS',probe_expr),('ALERTAS',alert_expr)]]},True)
    values={}
    for ref in ['SANOS','ALERTAS']:
        numbers=[]
        for frame in reply.get('results',{}).get(ref,{}).get('frames',[]):
            for i,field in enumerate(frame.get('schema',{}).get('fields',[])):
                if field.get('type')=='number':numbers.extend(frame['data']['values'][i])
        values[ref]=numbers[-1] if numbers else None
    normal=[{'Servicio':name,'HTTP':http('http://127.0.0.1:'+str(port)+path)[0]} for name,port,path in live]
    for row in normal:http_rows.append({'Fase':phase,'Entorno':'aplicacion',**row})
    up=query('up{job="transaduanas-backend"}')
    row={'Fase':phase,'Segundos':round(time.perf_counter()-t0,3),'Sondeos':len(probes),
        'SanosPrometheus':sum(float(p['value'][1]) for p in probes),'SanosGrafana':values['SANOS'],
        'AlertasPending':sum(a['state']=='pending' for a in alerts),'AlertasFiring':sum(a['state']=='firing' for a in alerts),
        'AlertasFiringGrafana':values['ALERTAS'],'HTTPHabituales200':sum(r['HTTP']==200 for r in normal),
        'BackendUP':float(up[0]['value'][1]) if up else None}
    timeline.append(row);graf_raw.append({'Fase':phase,'Segundos':row['Segundos'],'Respuesta':reply})
    (out/('estado-'+str(len(timeline))+'.json')).write_text(json.dumps({'Resumen':row,'Probes':probes,'EstadosHTTP':status,'Alertas':alerts},indent=2),encoding='utf-8')
    print(f'M11 {phase}: sanos={row["SanosPrometheus"]}/4; pending={row["AlertasPending"]}, firing={row["AlertasFiring"]}; Grafana alertas={values["ALERTAS"]}; rutas habituales200={row["HTTPHabituales200"]}/4',flush=True)
    return row
def wait_phase(phase,timeout,predicate):
    until=time.perf_counter()+timeout
    while True:
        row=snapshot(phase)
        if predicate(row):return row
        if time.perf_counter()>=until:return row
        time.sleep(2)
def change_config(text):
    (lab/'conf/nginx.conf').write_text(text,encoding='utf-8');nginx_cmd('-t');nginx_cmd('-s','reload')
def phase_http(phase,expected):
    limit=time.perf_counter()+10
    while True:
        statuses=[http('http://127.0.0.1:'+str(port)+path) for _,port,path,_,_ in services]
        if [r[0] for r in statuses]==expected or time.perf_counter()>limit:break
        time.sleep(.3)
    for (name,_,_,_,_),(code,valid) in zip(services,statuses):http_rows.append({'Fase':phase,'Entorno':'m11-aislado','Servicio':name,'HTTP':code,'ContenidoValido':valid})
    return [r[0] for r in statuses]==expected

published=False;started=False;error='';cleanup_errors=[]
normal_row=fault_row=recovered_row=None
http_normal=http_fault=http_recovered=False
try:
    nginx_cmd('-t')
    output=(out/'nginx-inicio.log').open('wb')
    process=subprocess.Popen([str(nginx),'-p',lab.as_posix()+'/', '-c','conf/nginx.conf'],cwd=nginx.parent,stdout=output,stderr=output,creationflags=subprocess.CREATE_NO_WINDOW)
    started=True
    http_normal=phase_http('normal',[200]*4)
    if not http_normal:raise RuntimeError('La copia de laboratorio no alcanzo HTTP 200; no se induce el fallo.')
    targets=[{'targets':['http://127.0.0.1:'+str(port)+path],
        'labels':{'servicio':name,'entorno':'m11-aislado','modulo':module,'prueba':'M11','ejecucion':marker}} for name,port,path,module,_ in services]
    temp=targets_file.with_suffix('.tmp');temp.write_text(json.dumps(targets),encoding='utf-8');temp.replace(targets_file);published=True
    normal_row=wait_phase('normal',40,lambda r:r['Sondeos']==4 and r['SanosPrometheus']==r['SanosGrafana']==4 and r['AlertasFiring']==r['AlertasPending']==r['AlertasFiringGrafana']==0)
    if normal_row['SanosPrometheus']!=4:raise RuntimeError('No se recolectaron los cuatro sondeos normales; no se induce fallo.')
    change_config(faulty)
    http_fault=phase_http('fallo-controlado',[500,500,500,502])
    fault_row=wait_phase('fallo-controlado',60,lambda r:r['SanosPrometheus']==r['SanosGrafana']==0 and r['AlertasFiring']==r['AlertasFiringGrafana']==4)
    change_config(healthy)
    http_recovered=phase_http('recuperacion',[200]*4)
    recovered_row=wait_phase('recuperacion',40,lambda r:r['SanosPrometheus']==r['SanosGrafana']==4 and r['AlertasFiring']==r['AlertasPending']==r['AlertasFiringGrafana']==0)
except Exception as exc:error=type(exc).__name__+': '+str(exc)
finally:
    if started:
        try:change_config(healthy)
        except Exception as exc:cleanup_errors.append('restauracion laboratorio: '+type(exc).__name__)
    if published:
        temp=targets_file.with_suffix('.tmp');temp.write_text(original_targets,encoding='utf-8');temp.replace(targets_file)
        time.sleep(3)
    if started:
        try:nginx_cmd('-s','quit');process.wait(timeout=12);output.close()
        except Exception as exc:cleanup_errors.append('parada laboratorio: '+type(exc).__name__)

checks={
    'NormalCuatroHTTP200':http_normal,
    'NormalSondeosSanos':bool(normal_row and normal_row['SanosPrometheus']==normal_row['SanosGrafana']==4),
    'FalloHTTP500502Esperado':http_fault,
    'FalloCuatroSondeosDetectados':bool(fault_row and fault_row['SanosPrometheus']==fault_row['SanosGrafana']==0),
    'PendingEnCuatroObjetivos':len(pending_seen)==4,
    'FiringEnCuatroObjetivos':bool(fault_row and fault_row['AlertasFiring']==fault_row['AlertasFiringGrafana']==4),
    'RecuperacionCuatroHTTP200':http_recovered,
    'RecuperacionSondeosYAlertas':bool(recovered_row and recovered_row['SanosPrometheus']==recovered_row['SanosGrafana']==4 and recovered_row['AlertasPending']==recovered_row['AlertasFiring']==recovered_row['AlertasFiringGrafana']==0),
    'ServiciosHabitualesDisponibles':bool(timeline) and all(row['HTTPHabituales200']==4 and row['BackendUP']==1 for row in timeline),
    'ConfiguracionNginxHabitualIntacta':hashlib.sha256(original_config.read_bytes()).hexdigest()==original_hash,
    'TargetsLaboratorioRetirados':targets_file.read_text(encoding='utf-8-sig')==original_targets,
    'SinErroresEjecutorOLimpieza':not error and not cleanup_errors
}
if not published:
    (out/'preparacion.txt').write_text('M11 NO EJECUTADA: no se publicaron targets ni se indujo el fallo. Error de preparacion: '+error,encoding='utf-8')
    raise RuntimeError('No se completo la preparacion del laboratorio; revisar preparacion.txt. No se atribuye fallo al proyecto.') from None
ok=all(checks.values())
summary=('Tres paginas y API en laboratorio: HTTP 200 -> 500/502 controlados -> 200. '
    f'Alertas pending observadas en {len(pending_seen)}/4 objetivos; firing en '+str(fault_row['AlertasFiring'] if fault_row else 0)+
    '/4; al recuperar, alertas activas='+str(recovered_row['AlertasFiring'] if recovered_row else None)+'. '
    'Se consultaron estados de sondeo y alertas desde Grafana. Las cuatro rutas habituales permanecieron HTTP 200.' )
result={'Prueba':11,'CodigoPrueba':'M11','Nombre':'Disponibilidad de paginas y API','Importancia':'Alta','Identificador':marker,
    'InicioLima':created.isoformat(),'FinLima':datetime.now(tz).isoformat(),
    'Ejecucion':'REALIZADA: caida y recuperacion controladas en copia Nginx aislada; alertas Prometheus y consultas a la fuente Grafana.',
    'Herramientas':['Nginx 1.31.8 (copia aislada)','Blackbox Exporter 0.29.1','Prometheus 3.13.5','Grafana OSS 13.3.1','Python (HTTP y verificacion de estados)'],
    'Estado':'EXITOSO' if ok else 'FALLO','Resultado':summary,
    'Incidencia':'' if ok else 'Verificaciones incumplidas: '+', '.join(key for key,value in checks.items() if not value),
    'Verificaciones':checks,'ErrorEjecutor':error,'ErroresLimpieza':cleanup_errors,'UmbralAlertaSegundos':15,
    'Alcance':'Escenario aislado con mismos dist y backend real de lectura. Los 500/502 son estimulos deliberados para verificar la deteccion, no fallos naturales imprevistos. El cierre significa desaparicion de alertas activas/pending en Prometheus y Grafana, no entrega de notificacion externa. No se detuvieron el backend, MySQL ni Nginx habitual.',
    'Cambios':'Se integraron sondeos persistentes de cuatro rutas habituales con Blackbox y dos reglas HTTP. La copia Nginx y sus targets temporales se retiraron al finalizar; logs y series historicas se conservan. Sin cambios de datos de negocio ni despliegue backend.'}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
(out/'grafana-respuestas.json').write_text(json.dumps(graf_raw,ensure_ascii=False,indent=2),encoding='utf-8')
for name,rows,fields in [('linea-tiempo.csv',timeline,list(timeline[0]) if timeline else ['Fase']),('comprobaciones-http.csv',http_rows,['Fase','Entorno','Servicio','HTTP','ContenidoValido'])]:
    with (out/name).open('w',encoding='utf-8-sig',newline='') as file:
        writer=csv.DictWriter(file,fieldnames=fields);writer.writeheader();writer.writerows(rows)
(out/'resultado.txt').write_text('M11 - DISPONIBILIDAD PAGINAS Y API\nESTADO: '+result['Estado']+'\n'+summary+'\n'+result['Alcance']+'\n',encoding='utf-8')
print('Estado: '+result['Estado']+'\n'+summary+'\nEvidencias: '+str(out),flush=True)
