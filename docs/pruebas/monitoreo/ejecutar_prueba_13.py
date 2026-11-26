from pathlib import Path
from datetime import datetime, timezone, timedelta
import argparse
import base64
import csv
import hashlib
import json
import msvcrt
import os
import secrets
import shutil
import socket
import subprocess
import threading
import time
import urllib.request
import urllib.error
import urllib.parse
import uuid

parser=argparse.ArgumentParser()
parser.add_argument('--proyecto',default=r'C:\Users\asus\Music\TransAduanas')
args=parser.parse_args();root=Path(args.proyecto)
lock=(root/'monitoreo/M13-ejecucion.lock').open('a+b');lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
except OSError:raise RuntimeError('M13 ya esta en curso; no se inicia otro laboratorio.') from None
tz=timezone(timedelta(hours=-5));created=datetime.now(tz)
marker='M13-'+created.strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6]
out=root/'evidencias-monitoreo'/marker;out.mkdir(parents=True,exist_ok=False)
shutil.copy2(__file__,out/Path(__file__).name)
tools=Path.home()/'Music/TransAduanas-herramientas/monitoreo'
lab=tools/'laboratorios'/marker;lab.mkdir(parents=True,exist_ok=False)
data=lab/'mysql-data'
client_config=lab/'cliente-privado.cnf'
mysql_base=Path(r'C:\Program Files\MySQL\MySQL Server 8.4')
mysqld=mysql_base/'bin/mysqld.exe';mysql=mysql_base/'bin/mysql.exe';mysqladmin=mysql_base/'bin/mysqladmin.exe'
java=Path(r'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin\java.exe')
jar=Path.home()/'Music/TransAduanas-desplegado/current/app.jar'
jar_hash=hashlib.sha256(jar.read_bytes()).hexdigest()
targets_file=root/'monitoreo/prometheus/objetivos-pruebas.json'
original_targets=targets_file.read_text(encoding='utf-8-sig')
if json.loads(original_targets):raise RuntimeError('Hay objetivos de otro laboratorio; no se modifican.')
for port in [13307,18087,19087]:
    with socket.socket() as check:
        try:check.bind(('127.0.0.1',port))
        except OSError:raise RuntimeError('Puerto del laboratorio ocupado: '+str(port)) from None
def listening(port):
    with socket.socket() as check:check.settimeout(.5);return check.connect_ex(('127.0.0.1',port))==0
def ps_json(code):
    result=subprocess.run(['powershell.exe','-NoProfile','-Command',code],capture_output=True,text=True,check=True)
    return json.loads(result.stdout.strip().lstrip('\ufeff'))
original_services=ps_json("@{Servicios=@(Get-CimInstance Win32_Service -Filter \"Name='MySQL84' OR Name='TransAduanasBackend'\" | Select-Object Name,State,ProcessId); MemoriaDisponibleKB=(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory} | ConvertTo-Json -Depth 4 -Compress")
(out/'servicios-iniciales.json').write_text(json.dumps(original_services,indent=2),encoding='utf-8')
if original_services['MemoriaDisponibleKB']<1572864:raise RuntimeError('Menos de 1.5 GiB disponibles: no se inicia el laboratorio.')
private=json.loads((tools/'credenciales-locales.json').read_text(encoding='utf-8-sig'))
graf_auth='Basic '+base64.b64encode((private['usuario']+':'+private['password']).encode()).decode();del private
def api(url,payload=None,grafana=False):
    headers={'Authorization':graf_auth} if grafana else {}
    if payload is not None:headers['Content-Type']='application/json'
    request=urllib.request.Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=headers)
    with urllib.request.urlopen(request,timeout=10) as response:return json.load(response)
def query(expr,when=None):
    params={'query':expr}
    if when is not None:params['time']=when
    return api('http://127.0.0.1:9090/api/v1/query?'+urllib.parse.urlencode(params))['data']['result']
def http(url):
    status=0;body={};rid=''
    try:
        with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'TransAduanas-M13-Disponibilidad-MySQL'}),timeout=6) as response:
            status=response.status;rid=response.headers.get('X-Request-ID','');body=json.load(response)
    except urllib.error.HTTPError as exc:
        status=exc.code;rid=exc.headers.get('X-Request-ID','')
        try:body=json.load(exc)
        except Exception:pass
    except Exception:pass
    return {'HTTP':status,'SuccessJSON':body.get('success') is True,'HealthUP':body.get('data',{}).get('status')=='UP' if isinstance(body.get('data'),dict) else False,
        'CatalogoConDatos':isinstance(body.get('data'),list) and len(body['data'])>0,'RequestId':rid}
def wait_http(url,predicate,timeout):
    limit=time.perf_counter()+timeout
    while True:
        row=http(url)
        if predicate(row):return True
        if time.perf_counter()>=limit:return False
        time.sleep(1)

password=secrets.token_hex(32);db_name='m13_'+uuid.uuid4().hex[:12]
mysql_process=None;backend_process=None;published=False;scenario_started=False;error='';cleanup_errors=[]
mysql_handles=[];backend_handle=None
def mysql_start():
    global mysql_process
    log=(out/('mysql-proceso-'+str(len(mysql_handles)+1)+'.log')).open('wb');mysql_handles.append(log)
    mysql_process=subprocess.Popen([str(mysqld),'--no-defaults','--basedir='+str(mysql_base),'--datadir='+str(data),
        '--port=13307','--bind-address=127.0.0.1','--mysqlx=OFF','--performance-schema=OFF','--skip-log-bin',
        '--innodb-buffer-pool-size=32M','--innodb-redo-log-capacity=16M','--console'],stdout=log,stderr=log,cwd=lab,creationflags=subprocess.CREATE_NO_WINDOW)
    limit=time.perf_counter()+45
    while not listening(13307):
        if mysql_process.poll() is not None:raise RuntimeError('MySQL de laboratorio no inicio; revisar su log.')
        if time.perf_counter()>limit:raise RuntimeError('MySQL de laboratorio no alcanzo escucha en 13307.')
        time.sleep(.5)
def mysql_sql(sql,initial=False):
    options=['--no-defaults','--no-login-paths','--host=127.0.0.1','--port=13307','--protocol=TCP','--user=root','--password='] if initial else ['--defaults-file='+str(client_config),'--no-login-paths']
    result=subprocess.run([str(mysql),*options,'--batch','--skip-column-names'],input=sql.encode(),capture_output=True,timeout=20,creationflags=subprocess.CREATE_NO_WINDOW)
    if result.returncode:raise RuntimeError('La operacion SQL del laboratorio no se completo; no se imprimen sentencias ni credenciales.')
    return result.stdout.decode(errors='replace')
def mysql_stop():
    if mysql_process is None or mysql_process.poll() is not None:return
    result=subprocess.run([str(mysqladmin),'--defaults-file='+str(client_config),'--no-login-paths','shutdown'],capture_output=True,timeout=20,creationflags=subprocess.CREATE_NO_WINDOW)
    if result.returncode:raise RuntimeError('No se confirmo el cierre normal de MySQL de laboratorio.')
    mysql_process.wait(timeout=20)
def publish_targets(value):
    temp=targets_file.with_suffix('.tmp');temp.write_text(value,encoding='utf-8');temp.replace(targets_file)

# M13: same project and original alert rule, evaluated by an isolated Prometheus.
import re
import math
manifest=json.loads((tools/'versiones.json').read_text(encoding='utf-8-sig'))
prom_exe=Path(next(item['ejecutable'] for item in manifest if item['nombre']=='prometheus'))
with socket.socket() as check:
    try:check.bind(('127.0.0.1',19090))
    except OSError:raise RuntimeError('Puerto Prometheus laboratorio 19090 ocupado.') from None
rules_path=root/'monitoreo/prometheus/reglas.yml'
rules_original=rules_path.read_text(encoding='utf-8-sig')
match=re.search(r'(?ms)^      - alert: TasaErroresHTTPAlta\n.*?(?=^      - alert:|\Z)',rules_original)
if not match:raise RuntimeError('No se encontro la regla original TasaErroresHTTPAlta.')
rule_block=match.group(0).rstrip()+'\n'
if 'for: 5m' not in rule_block or '[5m]' not in rule_block or '> 0.01' not in rule_block:
    raise RuntimeError('La regla actual no coincide con el plan 1% y cinco minutos; no se cambia por otra.')
(out/'regla-original.yml').write_text(rule_block,encoding='utf-8')
(lab/'regla-error.yml').write_text('groups:\n  - name: transaduanas\n    rules:\n'+rule_block,encoding='utf-8')
(lab/'prometheus.yml').write_text('global:\n  scrape_interval: 5s\n  evaluation_interval: 5s\nrule_files: [regla-error.yml]\nscrape_configs:\n  - job_name: transaduanas-backend\n    metrics_path: /actuator/prometheus\n    scrape_timeout: 4s\n    static_configs:\n      - targets: ["127.0.0.1:19087"]\n        labels: {entorno: m13-aislado, ejecucion: '+marker+'}\n',encoding='utf-8')
expr=re.search(r'^        expr: (.+)$',rule_block,re.M).group(1)
ratio_expr=expr.rsplit(' > 0.01',1)[0]
ds_uid='lab-'+marker.lower()
prom_process=None;prom_handle=None;ds_created=False
requests=[];timeline=[];graf_raw=[];pending_at=None;firing_at=None
threshold_start=None;firing_elapsed=None
normal=fault=recovered=None
expressions={'PORCENTAJE':'('+ratio_expr+') * 100',
    'FIRING':'count(ALERTS{alertname="TasaErroresHTTPAlta",alertstate="firing"}) or vector(0)'}
(out/'criterios-previos.txt').write_text(
    'M13: mismo JAR del proyecto; MySQL aislado 13307, backend 18087/19087 y Prometheus aislado 19090. Grafana usa datasource temporal especifica.\n'
    'Copiar exactamente la regla TasaErroresHTTPAlta, sin reducir for=5m ni ventana rate[5m] ni umbral >0.01.\n'
    'La regla original incluye todas las rutas HTTP, tambien Actuator, en el denominador; se conserva ese alcance.\n'
    'Carga baja: por ciclo de cinco segundos, un GET health y un GET catalogo, secuenciales y sin concurrencia.\n'
    'Normal: ambas consultas HTTP 200, tasa 5xx=0 y alerta inactiva.\n'
    'Estimulo: detener solo MySQL laboratorio; health sigue 200 y catalogo genera 500 reales. Mantener tasa >1% durante cinco minutos completos de evaluacion.\n'
    'Observar pending y firing reales; activeAt de la alerta debe indicar al menos 300 s cuando firing. No se modifica reloj ni contadores. Plazo maximo de fallo 390 s.\n'
    'La tasa debe permanecer >1% en todas las observaciones desde pending hasta firing, con target UP=1 desde la linea base establecida. Las lecturas de arranque previas a la primera recoleccion no indican una caida.\n'
    'Grafana debe confirmar porcentaje >1 y una alerta firing en el datasource del laboratorio.\n'
    'Recuperar MySQL sin reiniciar backend ni Prometheus; continuar GET normales hasta tasa <=1% y ninguna alerta pending/firing. Espera maxima 370 s por ventana real de cinco minutos.\n'
    'Las rutas y servicios habituales deben conservar disponibilidad y PID. No incorporar metricas del laboratorio al Prometheus habitual.\n'
    'Retirar datasource temporal de Grafana, detener solo procesos creados y borrar archivo privado de conexion; conservar evidencias y TSDB aislada fuera del proyecto.\n'
    'Los 500 son estimulo previsto; no se cuentan como incidencia inesperada. No se valida entrega de correo o notificacion externa.\n',encoding='utf-8')

def lab_query(expression,when=None):
    params={'query':expression}
    if when is not None:params['time']=when
    return api('http://127.0.0.1:19090/api/v1/query?'+urllib.parse.urlencode(params))['data']['result']
def number(expression,when=None):
    data=lab_query(expression,when)
    return float(data[0]['value'][1]) if data else None
def take_snapshot(phase):
    global pending_at,firing_at,firing_elapsed
    alerts=[a for a in api('http://127.0.0.1:19090/api/v1/alerts')['data']['alerts'] if a['labels'].get('alertname')=='TasaErroresHTTPAlta']
    state=alerts[0]['state'] if alerts else 'inactive'
    active_at=alerts[0].get('activeAt') if alerts else None
    active_seconds=None
    if active_at:
        active_time=datetime.fromisoformat(active_at.replace('Z','+00:00'))
        active_seconds=(datetime.now(timezone.utc)-active_time).total_seconds()
    if state=='pending' and pending_at is None:pending_at=time.perf_counter()
    if state=='firing' and firing_at is None:
        firing_at=time.perf_counter();firing_elapsed=active_seconds
    ts=int(time.time()*1000)-1000
    reply=api('http://127.0.0.1:3000/api/ds/query',{'from':str(ts-300000),'to':str(ts),
        'queries':[{'refId':ref,'datasource':{'uid':ds_uid,'type':'prometheus'},'expr':expression,
            'instant':True,'range':False,'format':'time_series','intervalMs':5000,'maxDataPoints':100} for ref,expression in expressions.items()]},True)
    values={}
    for ref in expressions:
        numbers=[]
        for frame in reply.get('results',{}).get(ref,{}).get('frames',[]):
            for i,field in enumerate(frame.get('schema',{}).get('fields',[])):
                if field.get('type')=='number':numbers.extend(frame['data']['values'][i])
        values[ref]=numbers[-1] if numbers else None
    normal_status=[http('http://127.0.0.1:'+str(port)+path)['HTTP'] for port,path in [(5173,'/'),(5174,'/'),(5180,'/'),(5180,'/api/v1/catalogos/puertos')]]
    row={'Fase':phase,'Segundos':round(time.perf_counter()-t0,3),'EstadoAlerta':state,'ActiveAt':active_at or '',
        'SegundosDesdeActiveAt':active_seconds,'PorcentajePrometheus':number(expressions['PORCENTAJE'],ts/1000),
        'PorcentajeGrafana':values['PORCENTAJE'],'AlertasGrafana':values['FIRING'],
        'BackendLabUP':number('up{job="transaduanas-backend"}'),'HTTPHabituales200':sum(code==200 for code in normal_status)}
    timeline.append(row);graf_raw.append(reply)
    (out/('estado-'+str(len(timeline))+'.json')).write_text(json.dumps({'Resumen':row,'Alertas':alerts},indent=2),encoding='utf-8')
    print(f'M13 {phase}: t={row["Segundos"]}s; tasa5xx={row["PorcentajePrometheus"]}%; alerta={state}; desde activeAt={active_seconds}; Grafana firing={values["FIRING"]}; habitual200={row["HTTPHabituales200"]}/4',flush=True)
    return row

def request_cycle(phase):
    for name,path in [('health','/api/v1/health'),('catalogo','/api/v1/catalogos/puertos')]:
        row=http('http://127.0.0.1:18087'+path)
        requests.append({'Fase':phase,'Tipo':name,'Segundos':round(time.perf_counter()-t0,3),**row})
    # Preserve every actual HTTP outcome even if a later orchestration step fails.
    with (out/'consultas.jsonl').open('a',encoding='utf-8') as f:
        for row in requests[-2:]:f.write(json.dumps(row)+'\n')

def phase_loop(phase,timeout,predicate):
    started=time.perf_counter();next_cycle=started;next_snapshot=started
    while True:
        time.sleep(max(0,next_cycle-time.perf_counter()))
        request_cycle(phase);next_cycle=max(next_cycle+5,time.perf_counter())
        if time.perf_counter()>=next_snapshot:
            row=take_snapshot(phase);next_snapshot=time.perf_counter()+10
            if predicate(row):return row
            if time.perf_counter()-started>timeout:return row

t0=time.perf_counter()
try:
    print('M13: inicializando MySQL aislado en 13307, sin leer la base habitual.',flush=True)
    with (out/'mysql-inicializacion.log').open('wb') as log:
        initialized=subprocess.run([str(mysqld),'--no-defaults','--initialize-insecure','--basedir='+str(mysql_base),'--datadir='+str(data),'--console'],stdout=log,stderr=log,cwd=lab,timeout=120,creationflags=subprocess.CREATE_NO_WINDOW)
    if initialized.returncode:raise RuntimeError('No se inicializo el directorio aislado de MySQL; revisar log.')
    mysql_start()
    # SQL only on the new isolated database; secret is passed via stdin, never argv or evidence.
    mysql_sql('CREATE DATABASE '+db_name+' CHARACTER SET utf8mb4; ALTER USER \'root\'@\'localhost\' IDENTIFIED BY \''+password+'\';',initial=True)
    client_config.write_text('[client]\nhost=127.0.0.1\nport=13307\nprotocol=TCP\nuser=root\npassword='+password+'\n',encoding='utf-8')
    acl_env=os.environ.copy();acl_env['M13_SECRET_PATH']=str(client_config)
    acl_code="$ErrorActionPreference='Stop'; $acl=[System.Security.AccessControl.FileSecurity]::new(); $acl.SetAccessRuleProtection($true,$false); foreach($sid in @([Security.Principal.WindowsIdentity]::GetCurrent().User,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'),[Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))){$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','Allow'))}; [System.IO.File]::SetAccessControl($env:M13_SECRET_PATH,$acl)"
    acl=subprocess.run(['powershell.exe','-NoProfile','-Command',acl_code],env=acl_env,capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW)
    if acl.returncode:raise RuntimeError('No se restringieron permisos del archivo privado de laboratorio.')
    env=os.environ.copy();env.update({'SERVER_PORT':'18087','MONITORING_PORT':'19087',
        'DB_URL':'jdbc:mysql://127.0.0.1:13307/'+db_name+'?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima&connectTimeout=1000&socketTimeout=1500',
        'DB_USERNAME':'root','DB_PASSWORD':password,'JWT_SECRET':secrets.token_urlsafe(64),
        'CORS_ALLOWED_ORIGINS':'http://localhost:5180','STORAGE_DIR':str(lab/'uploads'),'LOG_PATH':str(out/'backend-logs')})
    backend_handle=(out/'backend-consola.log').open('wb')
    backend_process=subprocess.Popen([str(java),'-Xms64m','-Xmx256m','-jar',str(jar),'--spring.profiles.active=staging',
        '--server.address=127.0.0.1','--spring.config.import=optional:file:./m13-inexistente.properties',
        '--spring.jpa.hibernate.ddl-auto=create','--app.data-initializer.enabled=false',
        '--spring.datasource.hikari.maximum-pool-size=2','--spring.datasource.hikari.minimum-idle=1',
        '--spring.datasource.hikari.connection-timeout=2000','--spring.datasource.hikari.validation-timeout=1000',
        '--spring.datasource.hikari.max-lifetime=30000','--spring.datasource.hikari.keepalive-time=0'],
        env=env,cwd=lab,stdout=backend_handle,stderr=backend_handle,creationflags=subprocess.CREATE_NO_WINDOW)
    del env
    print('M13: iniciando el mismo backend 0.5.5 en 18087, conectado solo a MySQL 13307.',flush=True)
    if not wait_http('http://127.0.0.1:18087/api/v1/health',lambda r:r['HTTP']==200 and r['HealthUP'],75):raise RuntimeError('Backend aislado no alcanzo health UP; revisar log.')
    mysql_sql("USE "+db_name+"; INSERT INTO catalogo_puerto (codigo_puerto,nombre,pais,tipo,estado) VALUES ('M13-"+uuid.uuid4().hex[:8]+"','Puerto sintetico M13','Peru','MARITIMO','ACTIVO');")
    validation=subprocess.run([str(prom_exe.with_name('promtool.exe')),'check','config',str(lab/'prometheus.yml')],capture_output=True,text=True)
    (out/'validacion-prometheus.txt').write_text(validation.stdout+validation.stderr,encoding='utf-8')
    if validation.returncode:raise RuntimeError('Configuracion Prometheus aislada invalida.')
    prom_handle=(out/'prometheus.log').open('wb')
    prom_process=subprocess.Popen([str(prom_exe),'--config.file='+str(lab/'prometheus.yml'),
        '--storage.tsdb.path='+str(lab/'prometheus-data'),'--web.listen-address=127.0.0.1:19090',
        '--storage.tsdb.retention.time=2d','--storage.tsdb.retention.size=64MB'],cwd=lab,stdout=prom_handle,stderr=prom_handle,creationflags=subprocess.CREATE_NO_WINDOW)
    limit=time.perf_counter()+20
    while not listening(19090):
        if prom_process.poll() is not None or time.perf_counter()>limit:raise RuntimeError('Prometheus aislado no inicio.')
        time.sleep(.5)
    api('http://127.0.0.1:3000/api/datasources',{'uid':ds_uid,'name':'Laboratorio '+marker,'type':'prometheus','access':'proxy','url':'http://127.0.0.1:19090','isDefault':False,'jsonData':{'httpMethod':'POST','timeInterval':'5s'}},True)
    ds_created=True
    normal=phase_loop('normal',40,lambda row:row['BackendLabUP']==1 and row['PorcentajePrometheus']==row['PorcentajeGrafana']==0 and row['EstadoAlerta']=='inactive')
    if normal['PorcentajePrometheus']!=0 or normal['BackendLabUP']!=1:raise RuntimeError('No se obtuvo linea base estable; no se induce fallo.')
    scenario_started=True
    print('M13: deteniendo MySQL aislado; se mantiene la regla for=5m y la ventana rate[5m].',flush=True)
    mysql_stop()
    fault=phase_loop('errores-controlados',390,lambda row:row['EstadoAlerta']=='firing' and row['AlertasGrafana']==1 and row['PorcentajeGrafana'] is not None and row['PorcentajeGrafana']>1 and row['SegundosDesdeActiveAt']>=300)
    print('M13: recuperando MySQL; se espera el cierre con la ventana real rate[5m].',flush=True)
    mysql_start()
    recovered=phase_loop('recuperacion',370,lambda row:row['PorcentajePrometheus'] is not None and row['PorcentajePrometheus']<=1 and row['PorcentajeGrafana'] is not None and row['PorcentajeGrafana']<=1 and row['EstadoAlerta']=='inactive' and row['AlertasGrafana']==0)
except Exception as exc:error=type(exc).__name__+': '+str(exc)
finally:
    if ds_created:
        try:
            request=urllib.request.Request('http://127.0.0.1:3000/api/datasources/uid/'+ds_uid,method='DELETE',headers={'Authorization':graf_auth})
            with urllib.request.urlopen(request,timeout=10) as response:response.read()
        except Exception as exc:cleanup_errors.append('retiro datasource: '+type(exc).__name__)
    if prom_process is not None and prom_process.poll() is None:
        prom_process.terminate()
        try:prom_process.wait(timeout=15)
        except subprocess.TimeoutExpired:prom_process.kill();prom_process.wait();cleanup_errors.append('cierre Prometheus lab forzado')
    if prom_handle:prom_handle.close()
    if backend_process is not None and backend_process.poll() is None:
        backend_process.terminate()
        try:backend_process.wait(timeout=20)
        except subprocess.TimeoutExpired:backend_process.kill();backend_process.wait();cleanup_errors.append('cierre backend lab forzado')
    if backend_handle:backend_handle.close()
    if mysql_process is not None and mysql_process.poll() is None:
        try:mysql_stop()
        except Exception as exc:
            mysql_process.terminate()
            try:mysql_process.wait(timeout=15)
            except subprocess.TimeoutExpired:mysql_process.kill();mysql_process.wait()
            cleanup_errors.append('cierre MySQL lab: '+type(exc).__name__)
    for handle in mysql_handles:handle.close()
    if client_config.exists():client_config.unlink()
    del password

final_services=ps_json("@(Get-CimInstance Win32_Service -Filter \"Name='MySQL84' OR Name='TransAduanasBackend'\" | Select-Object Name,State,ProcessId) | ConvertTo-Json -Compress")
(out/'servicios-finales.json').write_text(json.dumps(final_services,indent=2),encoding='utf-8')
before_services={s['Name']:(s['State'],s['ProcessId']) for s in original_services['Servicios']}
after_services={s['Name']:(s['State'],s['ProcessId']) for s in final_services}
sustained=[row for row in timeline if row['Fase']=='errores-controlados' and row['EstadoAlerta'] in ['pending','firing']]
checks={
    'ReglaOriginalSinCambios':rules_path.read_text(encoding='utf-8-sig')==rules_original,
    'NormalSinErrores':bool(normal and normal['PorcentajePrometheus']==0 and normal['EstadoAlerta']=='inactive'),
    'PendingObservado':pending_at is not None,
    'FiringDespuesDe300s':firing_elapsed is not None and firing_elapsed>=300,
    'TasaSobre1DurantePending':bool(sustained) and all(row['PorcentajePrometheus'] is not None and row['PorcentajePrometheus']>1 for row in sustained),
    'GrafanaConfirmaAlerta':bool(fault and fault['AlertasGrafana']==1 and fault['PorcentajeGrafana']>1),
    'RecuperacionConVentanaReal':bool(recovered and recovered['PorcentajePrometheus']<=1 and recovered['PorcentajeGrafana']<=1 and recovered['EstadoAlerta']=='inactive' and recovered['AlertasGrafana']==0),
    'ErroresHTTPReales':any(row['Tipo']=='catalogo' and row['HTTP']==500 for row in requests if row['Fase']=='errores-controlados'),
    'HealthPermaneceDisponible':all(row['HTTP']==200 and row['HealthUP'] for row in requests if row['Tipo']=='health'),
    'BackendLabSiempreRecolectable':bool(normal and normal['BackendLabUP']==1) and all(row['BackendLabUP']==1 for row in timeline if normal and row['Segundos']>=normal['Segundos']),
    'ServiciosHabitualesMismosPID':before_services==after_services and all(state=='Running' for state,pid in after_services.values()),
    'RutasHabitualesDisponibles':bool(timeline) and all(row['HTTPHabituales200']==4 for row in timeline),
    'JarActivoSinCambios':hashlib.sha256(jar.read_bytes()).hexdigest()==jar_hash,
    'TargetsHabitualesIntactos':targets_file.read_text(encoding='utf-8-sig')==original_targets,
    'ProcesosLabDetenidos':all(not listening(port) for port in [13307,18087,19087,19090]),
    'SecretoTemporalEliminado':not client_config.exists(),
    'SinErroresEjecutorOLimpieza':not error and not cleanup_errors
}
if not scenario_started:
    (out/'preparacion.txt').write_text('M13 NO EJECUTADA: no se indujeron errores. Preparacion: '+error+'\nLimpieza: '+json.dumps(cleanup_errors),encoding='utf-8')
    print('Preparacion incompleta. Evidencias: '+str(out),flush=True);raise SystemExit(1)
ok=all(checks.values())
summary=('Regla original >1% con rate[5m] y for=5m. Pending observado='+str(pending_at is not None)+
    '; firing tras '+str(round(firing_elapsed,3) if firing_elapsed is not None else None)+' s desde activeAt. '
    'Tasa al confirmar firing='+str(fault['PorcentajePrometheus'] if fault else None)+'%; '
    'recuperacion='+str(recovered['EstadoAlerta'] if recovered else None)+', tasa='+str(recovered['PorcentajePrometheus'] if recovered else None)+'%.')
result={'Prueba':13,'CodigoPrueba':'M13','Nombre':'Alerta por errores HTTP','Importancia':'Alta','Identificador':marker,
    'InicioLima':created.isoformat(),'FinLima':datetime.now(tz).isoformat(),
    'Ejecucion':'REALIZADA: errores 500 reales controlados en backend del proyecto aislado; regla original evaluada sin acortar cinco minutos ni su ventana.',
    'Herramientas':['Java 21 / JAR TransAduanas 0.5.5','MySQL Server 8.4 aislado','Spring Boot Actuator / Micrometer','Prometheus 3.13.5 aislado / regla TasaErroresHTTPAlta','Grafana OSS 13.3.1 / datasource temporal','Logback / requestId','Python y Windows CIM'],
    'Estado':'EXITOSO' if ok else 'FALLO','Resultado':summary,'Incidencia':'' if ok else 'Verificaciones incumplidas: '+', '.join(key for key,value in checks.items() if not value),
    'Verificaciones':checks,'SegundosActiveAtHastaFiring':firing_elapsed,'ExpresionOriginal':expr,'VentanaSegundos':300,'ForSegundos':300,
    'ErrorEjecutor':error,'ErroresLimpieza':cleanup_errors,'Solicitudes':len(requests),'Muestras':len(timeline),
    'Alcance':'Misma regla que el proyecto, evaluada sobre metricas del mismo JAR en un Prometheus aislado. Los errores se producen al perder MySQL del laboratorio, con consultas secuenciales de baja carga. La regla incluye rutas Actuator en el denominador tal como esta configurada. Grafana consulta tasa y estado real de la alerta; no se prueban notificaciones externas ni capacidad maxima.',
    'Cambios':'Sin cambios en backend habitual, datos originales, regla principal o Prometheus habitual. Datasource Grafana temporal retirado, procesos aislados detenidos, archivo privado eliminado. Evidencias y datos sinteticos inactivos conservados.'}
for name,value in [('resultado.json',result),('grafana-respuestas.json',graf_raw),('consultas-http.json',requests)]:
    (out/name).write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf-8')
with (out/'linea-tiempo.csv').open('w',encoding='utf-8-sig',newline='') as file:
    writer=csv.DictWriter(file,fieldnames=list(timeline[0]));writer.writeheader();writer.writerows(timeline)
(out/'resultado.txt').write_text('M13 - ALERTA POR ERRORES HTTP\nESTADO: '+result['Estado']+'\n'+summary+'\n'+result['Alcance']+'\n',encoding='utf-8')
print('Estado: '+result['Estado']+'\n'+summary+'\nEvidencias: '+str(out),flush=True)
