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
lock=(root/'monitoreo/M12-ejecucion.lock').open('a+b');lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
except OSError:raise RuntimeError('M12 ya esta en curso; no se inicia otro laboratorio.') from None
tz=timezone(timedelta(hours=-5));created=datetime.now(tz)
marker='M12-'+created.strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6]
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
for port in [13306,18086,19086]:
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
        with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'TransAduanas-M12-Disponibilidad-MySQL'}),timeout=6) as response:
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

(out/'criterios-previos.txt').write_text(
    'M12 disponibilidad MySQL desde una consulta de negocio real, con health de aplicacion que no consulta la base.\n'
    'Backend: mismo JAR activo del proyecto, puerto 18086 y management 19086; heap maximo 256 MiB. MySQL 8.4 aislado en 13306, buffer pool 32 MiB, datos en carpeta nueva fuera del proyecto.\n'
    'Se crea esquema vacio y un puerto sintetico exclusivamente en la instancia 13306; inicializador demo desactivado. No se exportan datos ni credenciales de la base original.\n'
    'Normal: catalogo HTTP 200/success=true con dato sintetico, health HTTP 200/data.status=UP, dos probe_success=1.\n'
    'Fallo controlado: cerrar solo MySQL de laboratorio; verificar puerto 13306 cerrado, catalogo HTTP 5xx, health HTTP 200/UP, sondeo negocio=0 y health=1.\n'
    'La regla DisponibilidadHTTPFallida debe pasar pending y firing solo para el sondeo de negocio; for=15s, scrape/evaluacion cada 5s. Espera maxima 60s, no SLA.\n'
    'Grafana debe mostrar sondeo negocio=0, health=1 y una alerta firing etiquetada con esta ejecucion.\n'
    'Recuperacion: reiniciar la misma instancia MySQL aislada, sin reiniciar backend; catalogo 200, dos sondeos=1 y sin alertas pending/firing; espera hasta 60s.\n'
    'Servicios habituales MySQL84/TransAduanasBackend deben mantener PID y estado; paginas y API habituales deben seguir 200.\n'
    'No se simula disponibilidad cambiando respuestas del backend: el driver real debe perder y recuperar su conexion MySQL. Se recogen logs y requestId de la consulta.\n'
    'Solo se comprueba la ruta /api/v1/health del proyecto; no se afirma que /actuator/health continue UP. No se prueban notificaciones externas.\n'
    'Al terminar retirar targets, detener solo procesos creados y borrar archivo temporal de credenciales. Conservar logs y datos sinteticos fuera del proyecto.\n',encoding='utf-8')

password=secrets.token_hex(32);db_name='m12_'+uuid.uuid4().hex[:12]
mysql_process=None;backend_process=None;published=False;scenario_started=False;error='';cleanup_errors=[]
mysql_handles=[];backend_handle=None
def mysql_start():
    global mysql_process
    log=(out/('mysql-proceso-'+str(len(mysql_handles)+1)+'.log')).open('wb');mysql_handles.append(log)
    mysql_process=subprocess.Popen([str(mysqld),'--no-defaults','--basedir='+str(mysql_base),'--datadir='+str(data),
        '--port=13306','--bind-address=127.0.0.1','--mysqlx=OFF','--performance-schema=OFF','--skip-log-bin',
        '--innodb-buffer-pool-size=32M','--innodb-redo-log-capacity=16M','--console'],stdout=log,stderr=log,cwd=lab,creationflags=subprocess.CREATE_NO_WINDOW)
    limit=time.perf_counter()+45
    while not listening(13306):
        if mysql_process.poll() is not None:raise RuntimeError('MySQL de laboratorio no inicio; revisar su log.')
        if time.perf_counter()>limit:raise RuntimeError('MySQL de laboratorio no alcanzo escucha en 13306.')
        time.sleep(.5)
def mysql_sql(sql,initial=False):
    options=['--no-defaults','--no-login-paths','--host=127.0.0.1','--port=13306','--protocol=TCP','--user=root','--password='] if initial else ['--defaults-file='+str(client_config),'--no-login-paths']
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

selector='job="transaduanas-http",ejecucion="'+marker+'"'
business_selector=selector+',servicio="m12-consulta-mysql"'
health_selector=selector+',servicio="m12-health-aplicacion"'
expressions={'NEGOCIO':'sum(probe_success{'+business_selector+'})','HEALTH':'sum(probe_success{'+health_selector+'})',
    'ALERTAS':'count(ALERTS{alertname="DisponibilidadHTTPFallida",alertstate="firing",ejecucion="'+marker+'"}) or vector(0)'}
timeline=[];graf_raw=[];pending_business=False;pending_health=False;request_results=[]
t0=time.perf_counter()
def snapshot(phase):
    global pending_business,pending_health
    business=http('http://127.0.0.1:18086/api/v1/catalogos/puertos')
    health=http('http://127.0.0.1:18086/api/v1/health')
    request_results.append({'Fase':phase,'Negocio':business,'Health':health})
    probes=query('probe_success{'+selector+'}')
    probe_map={p['metric']['servicio']:float(p['value'][1]) for p in probes}
    alerts=[a for a in api('http://127.0.0.1:9090/api/v1/alerts')['data']['alerts'] if a.get('labels',{}).get('ejecucion')==marker and a.get('labels',{}).get('alertname')=='DisponibilidadHTTPFallida']
    pending_business=pending_business or any(a['state']=='pending' and a['labels'].get('servicio')=='m12-consulta-mysql' for a in alerts)
    pending_health=pending_health or any(a['state']=='pending' and a['labels'].get('servicio')=='m12-health-aplicacion' for a in alerts)
    ts=int(time.time()*1000)-1000
    reply=api('http://127.0.0.1:3000/api/ds/query',{'from':str(ts-300000),'to':str(ts),
        'queries':[{'refId':ref,'datasource':{'uid':'transaduanas-prometheus','type':'prometheus'},'expr':expr,'instant':True,'range':False,'format':'time_series','intervalMs':5000,'maxDataPoints':100} for ref,expr in expressions.items()]},True)
    values={}
    for ref in expressions:
        numbers=[]
        for frame in reply.get('results',{}).get(ref,{}).get('frames',[]):
            for i,field in enumerate(frame.get('schema',{}).get('fields',[])):
                if field.get('type')=='number':numbers.extend(frame['data']['values'][i])
        values[ref]=numbers[-1] if numbers else None
    normals=[http('http://127.0.0.1:'+str(port)+path)['HTTP'] for port,path in [(5173,'/'),(5174,'/'),(5180,'/'),(5180,'/api/v1/catalogos/puertos')]]
    row={'Fase':phase,'Segundos':round(time.perf_counter()-t0,3),'MySQLLabEscuchando':listening(13306),
        'NegocioHTTP':business['HTTP'],'NegocioSuccess':business['SuccessJSON'],'CatalogoConDatoSintetico':business['CatalogoConDatos'],
        'HealthHTTP':health['HTTP'],'HealthUP':health['HealthUP'],'SondeoNegocio':probe_map.get('m12-consulta-mysql'),
        'SondeoHealth':probe_map.get('m12-health-aplicacion'),'Pending':sum(a['state']=='pending' for a in alerts),'Firing':sum(a['state']=='firing' for a in alerts),
        'GrafanaNegocio':values['NEGOCIO'],'GrafanaHealth':values['HEALTH'],'GrafanaAlertas':values['ALERTAS'],'HTTPHabituales200':sum(code==200 for code in normals)}
    timeline.append(row);graf_raw.append(reply)
    (out/('estado-'+str(len(timeline))+'.json')).write_text(json.dumps({'Resumen':row,'Probes':probes,'Alertas':alerts,'SolicitudNegocio':business},indent=2),encoding='utf-8')
    print(f'M12 {phase}: MySQL lab={row["MySQLLabEscuchando"]}; negocio HTTP {row["NegocioHTTP"]}; health UP={row["HealthUP"]}; pending/firing={row["Pending"]}/{row["Firing"]}; Grafana={values}; habitual200={row["HTTPHabituales200"]}/4',flush=True)
    return row
def wait_phase(phase,timeout,predicate):
    until=time.perf_counter()+timeout
    while True:
        row=snapshot(phase)
        if predicate(row):return row
        if time.perf_counter()>until:return row
        time.sleep(2)

normal=fault=recovered=None
try:
    print('M12: inicializando MySQL aislado en 13306, sin leer la base habitual.',flush=True)
    with (out/'mysql-inicializacion.log').open('wb') as log:
        initialized=subprocess.run([str(mysqld),'--no-defaults','--initialize-insecure','--basedir='+str(mysql_base),'--datadir='+str(data),'--console'],stdout=log,stderr=log,cwd=lab,timeout=120,creationflags=subprocess.CREATE_NO_WINDOW)
    if initialized.returncode:raise RuntimeError('No se inicializo el directorio aislado de MySQL; revisar log.')
    mysql_start()
    # SQL only on the new isolated database; secret is passed via stdin, never argv or evidence.
    mysql_sql('CREATE DATABASE '+db_name+' CHARACTER SET utf8mb4; ALTER USER \'root\'@\'localhost\' IDENTIFIED BY \''+password+'\';',initial=True)
    client_config.write_text('[client]\nhost=127.0.0.1\nport=13306\nprotocol=TCP\nuser=root\npassword='+password+'\n',encoding='utf-8')
    acl_env=os.environ.copy();acl_env['M12_SECRET_PATH']=str(client_config)
    acl_code="$ErrorActionPreference='Stop'; $acl=[System.Security.AccessControl.FileSecurity]::new(); $acl.SetAccessRuleProtection($true,$false); foreach($sid in @([Security.Principal.WindowsIdentity]::GetCurrent().User,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'),[Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))){$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','Allow'))}; [System.IO.File]::SetAccessControl($env:M12_SECRET_PATH,$acl)"
    acl=subprocess.run(['powershell.exe','-NoProfile','-Command',acl_code],env=acl_env,capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW)
    if acl.returncode:raise RuntimeError('No se restringieron permisos del archivo privado de laboratorio.')
    env=os.environ.copy();env.update({'SERVER_PORT':'18086','MONITORING_PORT':'19086',
        'DB_URL':'jdbc:mysql://127.0.0.1:13306/'+db_name+'?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima&connectTimeout=1000&socketTimeout=1500',
        'DB_USERNAME':'root','DB_PASSWORD':password,'JWT_SECRET':secrets.token_urlsafe(64),
        'CORS_ALLOWED_ORIGINS':'http://localhost:5180','STORAGE_DIR':str(lab/'uploads'),'LOG_PATH':str(out/'backend-logs')})
    backend_handle=(out/'backend-consola.log').open('wb')
    backend_process=subprocess.Popen([str(java),'-Xms64m','-Xmx256m','-jar',str(jar),'--spring.profiles.active=staging',
        '--server.address=127.0.0.1','--spring.config.import=optional:file:./m12-inexistente.properties',
        '--spring.jpa.hibernate.ddl-auto=create','--app.data-initializer.enabled=false',
        '--spring.datasource.hikari.maximum-pool-size=2','--spring.datasource.hikari.minimum-idle=1',
        '--spring.datasource.hikari.connection-timeout=2000','--spring.datasource.hikari.validation-timeout=1000',
        '--spring.datasource.hikari.max-lifetime=30000','--spring.datasource.hikari.keepalive-time=0'],
        env=env,cwd=lab,stdout=backend_handle,stderr=backend_handle,creationflags=subprocess.CREATE_NO_WINDOW)
    del env
    print('M12: iniciando el mismo backend 0.5.5 en 18086, conectado solo a MySQL 13306.',flush=True)
    if not wait_http('http://127.0.0.1:18086/api/v1/health',lambda r:r['HTTP']==200 and r['HealthUP'],75):raise RuntimeError('Backend aislado no alcanzo health UP; revisar log.')
    mysql_sql("USE "+db_name+"; INSERT INTO catalogo_puerto (codigo_puerto,nombre,pais,tipo,estado) VALUES ('M12-"+uuid.uuid4().hex[:8]+"','Puerto sintetico M12','Peru','MARITIMO','ACTIVO');")
    targets=[{'targets':['http://127.0.0.1:18086'+path],
        'labels':{'servicio':service,'entorno':'m12-aislado','modulo':'api','prueba':'M12','ejecucion':marker}} for service,path in [('m12-consulta-mysql','/api/v1/catalogos/puertos'),('m12-health-aplicacion','/api/v1/health')]]
    publish_targets(json.dumps(targets));published=True
    normal=wait_phase('normal',40,lambda r:r['NegocioHTTP']==200 and r['NegocioSuccess'] and r['CatalogoConDatoSintetico'] and r['HealthUP'] and r['SondeoNegocio']==r['SondeoHealth']==r['GrafanaNegocio']==r['GrafanaHealth']==1 and r['Firing']==r['Pending']==r['GrafanaAlertas']==0)
    if normal['SondeoNegocio']!=1 or normal['NegocioHTTP']!=200:raise RuntimeError('No se establecio la linea base; no se induce la caida MySQL.')
    scenario_started=True
    print('M12: deteniendo solamente MySQL de laboratorio (13306).',flush=True);mysql_stop()
    fault=wait_phase('mysql-interrumpido',60,lambda r:not r['MySQLLabEscuchando'] and 500<=r['NegocioHTTP']<600 and r['HealthHTTP']==200 and r['HealthUP'] and r['SondeoNegocio']==r['GrafanaNegocio']==0 and r['SondeoHealth']==r['GrafanaHealth']==1 and r['Firing']==r['GrafanaAlertas']==1)
    print('M12: recuperando MySQL de laboratorio; backend aislado sigue en el mismo proceso.',flush=True);mysql_start()
    recovered=wait_phase('recuperacion',60,lambda r:r['MySQLLabEscuchando'] and r['NegocioHTTP']==200 and r['NegocioSuccess'] and r['CatalogoConDatoSintetico'] and r['HealthUP'] and r['SondeoNegocio']==r['SondeoHealth']==r['GrafanaNegocio']==r['GrafanaHealth']==1 and r['Firing']==r['Pending']==r['GrafanaAlertas']==0)
except Exception as exc:error=type(exc).__name__+': '+str(exc)
finally:
    if published:
        try:publish_targets(original_targets);time.sleep(3)
        except Exception as exc:cleanup_errors.append('retirada targets: '+type(exc).__name__)
    if backend_process is not None and backend_process.poll() is None:
        backend_process.terminate()
        try:backend_process.wait(timeout=20)
        except subprocess.TimeoutExpired:backend_process.kill();backend_process.wait();cleanup_errors.append('backend lab requirio cierre forzado')
    if backend_handle:backend_handle.close()
    if mysql_process is not None and mysql_process.poll() is None:
        try:
            if client_config.exists():mysql_stop()
            else:raise RuntimeError('Falta config privada para cierre normal.')
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
checks={
    'NormalNegocioCorrecto':bool(normal and normal['NegocioHTTP']==200 and normal['CatalogoConDatoSintetico']),
    'MySQLLabRealmenteDetenido':bool(fault and not fault['MySQLLabEscuchando']),
    'NegocioFallaMientrasHealthUP':bool(fault and 500<=fault['NegocioHTTP']<600 and fault['HealthHTTP']==200 and fault['HealthUP']),
    'SondeosDistinguenNegocioYHealth':bool(fault and fault['SondeoNegocio']==fault['GrafanaNegocio']==0 and fault['SondeoHealth']==fault['GrafanaHealth']==1),
    'AlertaNegocioPendingYFiring':pending_business and bool(fault and fault['Firing']==fault['GrafanaAlertas']==1),
    'SinAlertaHealthFalsa':not pending_health,
    'RecuperacionSinReiniciarBackend':bool(recovered and recovered['NegocioHTTP']==200 and recovered['CatalogoConDatoSintetico'] and recovered['SondeoNegocio']==recovered['GrafanaNegocio']==1 and recovered['Firing']==recovered['Pending']==recovered['GrafanaAlertas']==0),
    'ServiciosHabitualesMismosPID':before_services==after_services and all(state=='Running' for state,pid in after_services.values()),
    'RutasHabitualesDisponibles':bool(timeline) and all(row['HTTPHabituales200']==4 for row in timeline),
    'JarActivoSinCambios':hashlib.sha256(jar.read_bytes()).hexdigest()==jar_hash,
    'TargetsRetirados':targets_file.read_text(encoding='utf-8-sig')==original_targets,
    'ProcesosLabDetenidos':all(not listening(port) for port in [13306,18086,19086]),
    'SecretoTemporalEliminado':not client_config.exists(),
    'SinErroresEjecutorOLimpieza':not error and not cleanup_errors
}
if not scenario_started:
    (out/'preparacion.txt').write_text('M12 NO EJECUTADA: no se indujo la caida MySQL. Error de preparacion: '+error+'\nLimpieza: '+json.dumps(cleanup_errors),encoding='utf-8')
    print('Preparacion incompleta; no se registra un fallo del proyecto. Evidencias: '+str(out),flush=True)
    raise SystemExit(1)
ok=all(checks.values())
summary=('MySQL aislado 13306: disponible -> detenido -> recuperado. Consulta de catalogo: HTTP '+str(normal['NegocioHTTP'] if normal else None)+' -> '+str(fault['NegocioHTTP'] if fault else None)+' -> '+str(recovered['NegocioHTTP'] if recovered else None)+'. '
    'Durante la caida, /api/v1/health mantuvo UP='+str(fault['HealthUP'] if fault else None)+'. '
    'Alerta negocio pending='+str(pending_business)+', firing='+str(fault['Firing'] if fault else None)+', activas tras recuperar='+str(recovered['Firing'] if recovered else None)+'.')
result={'Prueba':12,'CodigoPrueba':'M12','Nombre':'Disponibilidad de MySQL','Importancia':'Alta','Identificador':marker,
    'InicioLima':created.isoformat(),'FinLima':datetime.now(tz).isoformat(),
    'Ejecucion':'REALIZADA: MySQL y backend del proyecto aislados, interrupcion real de la instancia 13306 y recuperacion sin reiniciar el backend de prueba.',
    'Herramientas':['MySQL Server 8.4 y clientes mysql/mysqladmin','Java 21 / JAR TransAduanas 0.5.5','Blackbox Exporter 0.29.1','Prometheus 3.13.5','Grafana OSS 13.3.1','Logback / requestId y JDBC','Python y Windows CIM'],
    'Estado':'EXITOSO' if ok else 'FALLO','Resultado':summary,'Incidencia':'' if ok else 'Verificaciones incumplidas: '+', '.join(key for key,value in checks.items() if not value),
    'Verificaciones':checks,'ErrorEjecutor':error,'ErroresLimpieza':cleanup_errors,'SHA256Jar':jar_hash,
    'Alcance':'Se verifica la dependencia de una consulta real al catalogo en una instancia MySQL sintetica, con el mismo JAR del proyecto y la regla de disponibilidad existente. Health corresponde especificamente a /api/v1/health, que no consulta la base. No se afirma UP en /actuator/health, no se ejecuta mysql_exporter ni se validan notificaciones externas. El fallo MySQL es controlado y esperado, no una incidencia natural del servicio habitual.',
    'Cambios':'Sin cambios de codigo, JAR activo, configuracion o datos de negocio originales. Esquema y dato sintetico solo en MySQL 13306, conservados fuera del proyecto; procesos detenidos y archivo privado temporal eliminado. Targets de laboratorio retirados. Logs, mediciones y series historicas conservados.'}
for name,value in [('resultado.json',result),('grafana-respuestas.json',graf_raw),('consultas-http.json',request_results)]:
    (out/name).write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf-8')
with (out/'linea-tiempo.csv').open('w',encoding='utf-8-sig',newline='') as file:
    writer=csv.DictWriter(file,fieldnames=list(timeline[0]));writer.writeheader();writer.writerows(timeline)
(out/'resultado.txt').write_text('M12 - DISPONIBILIDAD DE MYSQL\nESTADO: '+result['Estado']+'\n'+summary+'\n'+result['Alcance']+'\n'+result['Cambios']+'\n',encoding='utf-8')
print('Estado: '+result['Estado']+'\n'+summary+'\nEvidencias: '+str(out),flush=True)
