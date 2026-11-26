"""Prueba 13: reversion planificada de copias de JAR y datos, sin cambiar produccion."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
import base64,csv,hashlib,hmac,json,os,re,secrets,shutil,socket,subprocess,time,urllib.request,zipfile,sys

import argparse
ap=argparse.ArgumentParser(description='Prueba 13 - Reversion de cambios en laboratorio')
ap.add_argument('--reanudar-preparacion',action='store_true')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
opt=ap.parse_args()
PROJECT=Path(opt.proyecto).resolve();DEPLOY=ruta_despliegue(PROJECT)
P12=PROJECT/'evidencias-mantenimiento/Evidencia_Prueba_12'
p12result=json.loads((P12/'resultado.json').read_text(encoding='utf-8'))
assert p12result['estado']=='Exitoso' and p12result['procesos_laboratorio_detenidos']
P12_PRIVATE=Path(json.loads((P12/'configuracion_laboratorio.json').read_text(encoding='utf-8'))['privado']).resolve()
LABROOT=ruta_laboratorio(PROJECT)
assert P12_PRIVATE.is_relative_to(LABROOT.resolve())
for port in [23306,28086,29086,24306,38086,39086]:
    with socket.socket() as s:
        s.settimeout(.3);assert s.connect_ex(('127.0.0.1',port))!=0,'Puerto de laboratorio ocupado: '+str(port)
def artifact_sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda:f.read(1048576),b''):h.update(b)
    return h.hexdigest()
state=json.loads((DEPLOY/'estado.json').read_text(encoding='utf-8-sig'));artifacts={}
for label in ['Actual','Anterior']:
    item=state[label];p=Path(item['Ruta']).resolve()
    assert p.is_relative_to((DEPLOY/'releases').resolve())
    digest=artifact_sha(p);assert digest==item['Hash'].lower()
    artifacts[label]={'version':item['Version'],'ruta':str(p),'sha256':digest}
assert artifacts['Actual']['sha256']!=artifacts['Anterior']['sha256']
assert artifact_sha(DEPLOY/'current/app.jar')==artifacts['Actual']['sha256']
with zipfile.ZipFile(artifacts['Actual']['ruta']) as a,zipfile.ZipFile(artifacts['Anterior']['ruta']) as b:
    an=set(a.namelist());bn=set(b.namelist())
    entities=sorted(n for n in an|bn if n.startswith('BOOT-INF/classes/pe/sga/entity/') and n.endswith('.class'))
    same=[n for n in entities if n in an and n in bn and a.read(n)==b.read(n)]
    migrations=sorted(n for n in an|bn if 'db/migration/' in n or 'db/changelog/' in n)
    changed=[n for n in an|bn if n.startswith('BOOT-INF/classes/') and (n not in an or n not in bn or a.read(n)!=b.read(n))]
    library_versions={label:[n.rsplit('/',1)[-1] for n in z.namelist() if n.startswith('BOOT-INF/lib/pdfbox-')] for label,z in [('Actual',a),('Anterior',b)]}
assert entities and len(entities)==len(same) and not migrations,'Compatibilidad de esquema no confirmada.'
OUT=PROJECT/'evidencias-mantenimiento/Evidencia_Prueba_13';case=2
if not opt.reanudar_preparacion:
    while OUT.exists():OUT=OUT.with_name(f'Evidencia_Prueba_13_{case}');case+=1
PRIVATE=LABROOT/OUT.name
assert PRIVATE.resolve().is_relative_to(LABROOT.resolve())
if opt.reanudar_preparacion:
    assert OUT.is_dir() and {p.name for p in OUT.iterdir()}=={'configuracion_laboratorio.json','servicios_antes.json'}
    assert PRIVATE.is_dir() and not any(p.is_file() for p in PRIVATE.rglob('*')),'No reanudar una instancia que ya tuvo datos.'
else:
    assert not PRIVATE.exists()
    OUT.mkdir(parents=True);PRIVATE.mkdir(parents=True)
q=str(PRIVATE).replace("'","''")
aclcode="$taskP='"+q+"';$taskSid=[Security.Principal.WindowsIdentity]::GetCurrent().User;$taskAcl=[Security.AccessControl.DirectorySecurity]::new();$taskAcl.SetOwner($taskSid);$taskAcl.SetAccessRuleProtection($true,$false);foreach($taskAllowed in @($taskSid,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'),[Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))){$taskAcl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($taskAllowed,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))};[IO.Directory]::SetAccessControl($taskP,$taskAcl);if(-not [IO.Directory]::GetAccessControl($taskP).AreAccessRulesProtected){throw 'ACL no protegida'}"
acl=subprocess.run([str(powershell()),'-NoProfile','-Command',aclcode],capture_output=True,timeout=15)
assert acl.returncode==0,'No se confirmo el acceso restringido.'
APP=PRIVATE/'aplicacion';DATA=PRIVATE/'mysql-data';DOCS=PRIVATE/'documentos'
for d in [APP,APP/'current',APP/'releases',DOCS]:d.mkdir(exist_ok=opt.reanudar_preparacion)
CFG={'prueba':'PM13','PM12':str(P12),'privado':str(PRIVATE),'mysql_port':24306,'app_port':38086,'metrics_port':39086,'artefactos':artifacts,'entidades_clases':len(entities),'entidades_identicas':len(same),'migraciones_empaquetadas':migrations,'clases_diferentes':changed,'pdfbox':library_versions}
(OUT/'configuracion_laboratorio.json').write_text(json.dumps(CFG,ensure_ascii=False,indent=2),encoding='utf-8')
source_data=P12_PRIVATE/'mysql-data';source_hashes={};cold_source_unchanged=False

MYSQLBASE=mysql_home();MYSQL=MYSQLBASE/'bin/mysql.exe';MYSQLD=MYSQLBASE/'bin/mysqld.exe';ADMIN=MYSQLBASE/'bin/mysqladmin.exe';DUMP=MYSQLBASE/'bin/mysqldump.exe'
JAVA=(java_home()/'bin/java.exe');PS=Path(str(powershell()))
TZ=timezone(timedelta(hours=-5));START=datetime.now(TZ)
root_password=secrets.token_hex(32);app_password=secrets.token_hex(32)
client=PRIVATE/'cliente-laboratorio.cnf';bootstrap=PRIVATE/'bootstrap.sql'
mysql_process=None;java_process=None;mysql_log=None;java_log=None;confirmed=False;error='';cleanup_errors=[];phases={};checks=[]
def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda:f.read(1048576),b''):h.update(b)
    return h.hexdigest()
def jsonwrite(name,data):
    p=OUT/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
def csvread(p):
    with p.open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f))
def csvwrite(p,rows):
    with p.open('w',encoding='utf-8-sig',newline='') as f:
        if rows:
            w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
def psjson(code):
    r=subprocess.run([str(PS),'-NoProfile','-Command',code],env={k:v for k,v in os.environ.items() if k!='PSModulePath'},capture_output=True,timeout=15)
    if r.returncode:raise RuntimeError('No se pudo consultar el estado Windows; no se cambian servicios.')
    return json.loads(r.stdout.decode('utf-8-sig',errors='replace'))
def services():return psjson("@(Get-CimInstance Win32_Service -Filter \"Name='MySQL84' OR Name='TransAduanasBackend'\" | Select-Object Name,State,ProcessId) | ConvertTo-Json -Compress")
def listening(port):
    with socket.socket() as s:s.settimeout(.3);return s.connect_ex(('127.0.0.1',port))==0
def api(path,token=None,original=False):
    port=8086 if original else CFG['app_port']
    with urllib.request.urlopen(urllib.request.Request(f'http://127.0.0.1:{port}'+path,headers={'Authorization':'Bearer '+token} if token else {}),timeout=10) as r:return r.status,r.read()
def args():return [str(MYSQL),'--defaults-file='+str(client),'--no-login-paths','--host=127.0.0.1','--port='+str(CFG['mysql_port']),'--protocol=TCP','--ssl-mode=REQUIRED','--batch','--raw','--skip-column-names','--default-character-set=utf8mb4']
def sql(text):
    r=subprocess.run(args(),input=text.encode('utf-8'),capture_output=True,timeout=20,creationflags=subprocess.CREATE_NO_WINDOW)
    if r.returncode:
        (PRIVATE/'error-cliente.log').write_bytes(r.stderr);raise RuntimeError('Consulta del laboratorio devolvió error; detalles privados guardados.')
    return r.stdout.decode('utf-8')
def guard():
    port,folder=sql('SELECT @@port,@@datadir;').strip().split('\t')
    assert int(port)==24306 and Path(folder).resolve()==DATA.resolve() and DATA.resolve().is_relative_to(PRIVATE.resolve())
    return {'puerto':int(port),'datadir':str(Path(folder).resolve()),'PID':mysql_process.pid}
def canonical(raw):
    result=[]
    for line in raw.decode('utf-8-sig').splitlines():
        if line.startswith('--') or not line.strip():continue
        line=re.sub(r'^(\s*`[^`]+`\s+(?:varchar\(\d+\)|char\(\d+\)|(?:tiny|medium|long)?text)) CHARACTER SET utf8mb4 (COLLATE utf8mb4_[A-Za-z0-9_]+)',r'\1 \2',line)
        result.append(line.rstrip())
    return '\n'.join(result)
def token(subject,secret):
    enc=lambda obj:base64.urlsafe_b64encode(json.dumps(obj,separators=(',',':')).encode()).rstrip(b'=')
    now=int(time.time());body=enc({'alg':'HS256','typ':'JWT'})+b'.'+enc({'sub':subject,'tipoUsuario':'PERSONAL_INTERNO','iat':now,'exp':now+600})
    return (body+b'.'+base64.urlsafe_b64encode(hmac.new(secret.encode(),body,hashlib.sha256).digest()).rstrip(b'=')).decode()
def counts():
    return {t:int(n) for t,n in (line.split('\t') for line in sql(' UNION ALL '.join(f"SELECT '{t}',COUNT(*) FROM `{DB}`.`{t}`" for t in expected)+';').strip().splitlines())}
def export_and_compare(label):
    dest=PRIVATE/(label+'-base.sql')
    cmd=[str(DUMP),'--defaults-file='+str(client),'--no-login-paths','--host=127.0.0.1','--port=24306','--protocol=TCP','--ssl-mode=REQUIRED','--default-character-set=utf8mb4','--single-transaction','--skip-lock-tables','--routines','--events','--triggers','--no-tablespaces','--hex-blob','--set-gtid-purged=OFF','--databases',DB,'--result-file='+str(dest)]
    r=subprocess.run(cmd,capture_output=True,timeout=20,creationflags=subprocess.CREATE_NO_WINDOW)
    (PRIVATE/(label+'-dump-error.log')).write_bytes(r.stderr);assert r.returncode==0
    return canonical(dest.read_bytes())==expected_sql
def stop_java():
    global java_process,java_log
    if java_process is not None and java_process.poll() is None:java_process.terminate();java_process.wait(timeout=12)
    if java_log:java_log.close();java_log=None
    assert not listening(CFG['app_port']) and not listening(CFG['metrics_port'])
def run_phase(label,version):
    global java_process,java_log
    secret=base64.urlsafe_b64encode(os.urandom(32)).decode().rstrip('=')
    env=os.environ.copy()
    for k in list(env):
        if k.startswith(('SPRING_','DB_','JPA_','JWT_','STORAGE_','SERVER_','MONITORING_')) or k in ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','JAVA_OPTS','LOG_PATH']:env.pop(k,None)
    jdbc=f'jdbc:mysql://127.0.0.1:24306/{DB}?sslMode=REQUIRED&serverTimezone=America/Lima'
    env.update(DB_URL=jdbc,SPRING_DATASOURCE_URL=jdbc,DB_USERNAME='pm13_lector',SPRING_DATASOURCE_USERNAME='pm13_lector',DB_PASSWORD=app_password,SPRING_DATASOURCE_PASSWORD=app_password,JWT_SECRET=secret,STORAGE_DIR=str(DOCS),CORS_ALLOWED_ORIGINS='http://127.0.0.1:38086',LOG_PATH=str(APP/'logs'/label))
    cmd=[str(JAVA),'-Xms64m','-Xmx256m','-DLOG_PATH='+str(APP/'logs'/label),'-jar',str(APP/'current/app.jar'),'--spring.profiles.active=staging','--spring.config.location=classpath:/application.properties,classpath:/application-staging.properties','--spring.config.import=','--server.address=127.0.0.1','--server.port=38086','--management.server.address=127.0.0.1','--management.server.port=39086','--spring.jpa.hibernate.ddl-auto=validate','--spring.jpa.show-sql=false','--spring.datasource.hikari.read-only=true','--app.data-initializer.enabled=false','--app.storage.upload-dir='+str(DOCS)]
    java_log=(PRIVATE/(label+'-aplicacion.log')).open('wb')
    java_process=subprocess.Popen(cmd,cwd=APP,env=env,stdout=java_log,stderr=java_log,creationflags=subprocess.CREATE_NO_WINDOW)
    limit=time.perf_counter()+60
    while True:
        if java_process.poll() is not None:raise RuntimeError('El backend de laboratorio '+version+' se cerró al iniciar.')
        if listening(38086):
            try:
                status,body=api('/api/v1/health')
                if status==200 and json.loads(body).get('data',{}).get('status')=='UP':break
            except OSError:pass
        if time.perf_counter()>limit:raise TimeoutError('La versión '+version+' no alcanzó salud en el laboratorio.')
        time.sleep(.5)
    admin=sql(f"SELECT username FROM `{DB}`.usuario WHERE rol='ADMINISTRADOR_TI' AND estado_activo=1 ORDER BY id_usuario LIMIT 1;").strip();assert admin
    auth=token(admin,secret);queries=[]
    for name,path,table in [('clientes','/api/v1/clientes','cliente'),('cotizaciones','/api/v1/cotizaciones','cotizacion'),('expedientes','/api/v1/expedientes','expediente'),('pagos','/api/v1/finanzas/anticipos','cotizacion_pago'),('documentos','/api/v1/documentos',None)]:
        status,body=api(path,auth);answer=json.loads(body)
        (PRIVATE/(label+'-'+name+'.json')).write_bytes(body)
        num=len(answer['data']) if isinstance(answer.get('data'),list) else None
        expected_num=expected[table] if table else int(sql(f'SELECT COUNT(*) FROM `{DB}`.documento_adjunto WHERE activo=1;').strip())
        queries.append({'version':version,'consulta':name,'http':status,'success':answer.get('success'),'registros':num,'esperados':expected_num})
    assert all(q['http']==200 and q['success'] and q['registros']==q['esperados'] for q in queries)
    connections=psjson(f"@(Get-NetTCPConnection -OwningProcess {java_process.pid} -State Established -ErrorAction SilentlyContinue | Where-Object RemotePort -eq 24306 | Select-Object RemoteAddress,RemotePort,OwningProcess)|ConvertTo-Json -Compress")
    if isinstance(connections,dict):connections=[connections]
    assert connections and all(c['RemotePort']==24306 for c in connections)
    jsonwrite(label+'-conexion.json',connections)
    same_data=export_and_compare(label)
    assert counts()==expected
    return {'version':version,'PID':java_process.pid,'JAR_sha256':sha(APP/'current/app.jar'),'salud':'UP','consultas':queries,'SQL_equivalente_al_respaldo':same_data,'registros':sum(expected.values()),'validacion_JPA':'validate; arranque exitoso','cuenta_base':'pm13_lector solo SELECT'}

protected=[]
for folder in ['backend-sga/src','app-sga/src','app-b2b/src','app-landing/src','db','uploads','.github','monitoreo','scripts']:
    protected.extend(p for p in (PROJECT/folder).rglob('*') if p.is_file() and p.suffix.lower() not in ['.log','.lock'])
for folder in ['backend-sga','app-sga','app-b2b','app-landing']:
    protected.extend(p for p in (PROJECT/folder).glob('*') if p.is_file() and p.suffix!='.md')
before={p.relative_to(PROJECT).as_posix():sha(p) for p in sorted(set(protected))}
deploy_before={rel:sha(DEPLOY/rel) for rel in ['estado.json','current/app.jar']}
services_before=services();jsonwrite('servicios_antes.json',services_before)
original_running=next(v for v in services_before if v['Name']=='TransAduanasBackend')['State']=='Running'
health_before=json.loads(api('/api/v1/health',original=True)[1])['data']['status'] if original_running else 'Servicio detenido antes de la prueba'
assert health_before=='UP' if original_running else not listening(8086)
jsonwrite('estado_original_inicial.json',{'backend_ejecutandose':original_running,'salud':health_before,'ninguna_accion_sobre_servicios':True})
memory=psjson('(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory');assert int(memory)>=1572864
try:
    print('PM13: copiando datos restaurados y detenidos de PM12 a un nuevo laboratorio.',flush=True)
    assert not listening(23306) and not DATA.exists()
    source_data=P12_PRIVATE/'mysql-data';assert source_data.is_dir()
    source_files=[p for p in source_data.rglob('*') if p.is_file()]
    assert all(not p.is_symlink() and not (hasattr(p,'is_junction') and p.is_junction()) for p in source_data.rglob('*'))
    source_hashes={p.relative_to(source_data).as_posix():sha(p) for p in source_files}
    shutil.copytree(source_data,DATA)
    assert all(sha(DATA/rel)==h for rel,h in source_hashes.items())
    source_docs=P12_PRIVATE/'restaurado/documentos'
    for p in source_docs.rglob('*'):
        if p.is_file():
            dest=DOCS/p.relative_to(source_docs);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest)
    manifest=json.loads((P12_PRIVATE/'restaurado/manifiesto.json').read_text(encoding='utf-8'))
    DB=manifest['base_de_datos'];expected=manifest['tablas_y_filas'];expected_sql=canonical((P12_PRIVATE/'restaurado/SGAdb.sql').read_bytes())
    assert re.fullmatch(r'[A-Za-z0-9_]+',DB)
    docs_before={r:sha(DOCS/r) for r in manifest['documentos']};assert all(docs_before[r]==m['sha256'] for r,m in manifest['documentos'].items())
    for label,a in CFG['artefactos'].items():
        source=Path(a['ruta']);assert sha(source)==a['sha256']
        dest=APP/'releases'/('backend-'+a['version']+'.jar');shutil.copy2(source,dest);assert sha(dest)==a['sha256']
    for port in [24306,38086,39086]:
        with socket.socket() as s:s.bind(('127.0.0.1',port))
    client.write_text('[client]\nuser=root\npassword="'+root_password+'"\nhost=127.0.0.1\nport=24306\n',encoding='utf-8')
    bootstrap.write_text("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+root_password+"';\n",encoding='utf-8')
    mysqlcmd=[str(MYSQLD),'--no-defaults','--basedir='+str(MYSQLBASE),'--datadir='+str(DATA),'--port=24306','--bind-address=127.0.0.1','--mysqlx=OFF','--performance-schema=OFF','--skip-log-bin','--event-scheduler=OFF','--local-infile=OFF','--innodb-buffer-pool-size=32M','--innodb-redo-log-capacity=16M','--init-file='+str(bootstrap),'--console']
    mysql_log=(PRIVATE/'mysql-proceso.log').open('wb')
    mysql_process=subprocess.Popen(mysqlcmd,cwd=PRIVATE,stdout=mysql_log,stderr=mysql_log,creationflags=subprocess.CREATE_NO_WINDOW)
    limit=time.perf_counter()+40
    while not listening(24306):
        if mysql_process.poll() is not None:raise RuntimeError('MySQL copiado se cerró al iniciar.')
        if time.perf_counter()>limit:raise TimeoutError('MySQL de PM13 no alcanzó escucha.')
        time.sleep(.3)
    isolation=guard();confirmed=True;bootstrap.unlink();jsonwrite('aislamiento_mysql.json',isolation)
    assert counts()==expected
    guard();sql("CREATE USER 'pm13_lector'@'127.0.0.1' IDENTIFIED BY '"+app_password+"'; GRANT SELECT ON `"+DB+"`.* TO 'pm13_lector'@'127.0.0.1';")
    assert (CFG['entidades_clases']>0 and CFG['entidades_clases']==CFG['entidades_identicas']) and not CFG['migraciones_empaquetadas']
    current=CFG['artefactos']['Actual'];previous=CFG['artefactos']['Anterior']
    shutil.copy2(APP/'releases'/('backend-'+current['version']+'.jar'),APP/'current/app.jar')
    labstate={'Actual':current['version'],'Anterior':previous['version'],'sha256_actual':sha(APP/'current/app.jar'),'alcance':'Solo este laboratorio'}
    jsonwrite('estado_laboratorio_antes.json',labstate)
    print('PM13: comprobando '+current['version']+' con la copia restaurada.',flush=True)
    phases['antes']=run_phase('antes',current['version']);jsonwrite('fase_antes.json',phases['antes'])
    revert_clock=time.perf_counter()
    stop_java()
    assert (APP/'current/app.jar').resolve().is_relative_to(PRIVATE.resolve())
    shutil.copy2(APP/'releases'/('backend-'+previous['version']+'.jar'),APP/'current/app.jar')
    assert sha(APP/'current/app.jar')==previous['sha256']
    jsonwrite('estado_laboratorio_despues.json',{'Actual':previous['version'],'Anterior':current['version'],'sha256_actual':sha(APP/'current/app.jar'),'alcance':'Solo este laboratorio'})
    print('PM13: JAR del laboratorio revertido a '+previous['version']+'; comprobando consultas.',flush=True)
    phases['despues']=run_phase('despues',previous['version']);jsonwrite('fase_despues.json',phases['despues'])
    revert_seconds=round(time.perf_counter()-revert_clock,6)
    docs_after={r:sha(DOCS/r) for r in docs_before}
    jsonwrite('verificacion_datos.json',{'tablas':len(expected),'filas':sum(expected.values()),'SQL_antes_equivalente':phases['antes']['SQL_equivalente_al_respaldo'],'SQL_despues_equivalente':phases['despues']['SQL_equivalente_al_respaldo'],'documentos':len(docs_before),'hashes_documentales_iguales':docs_before==docs_after,'reversion_base_datos':'No necesaria en este cambio: entidades idénticas, sin migraciones empaquetadas y ambas versiones validaron el mismo esquema. No se importó ni eliminó la base del laboratorio para hacer pasar la prueba.','segundos_reversion_y_comprobacion':revert_seconds})
    checks=[('C01','Artefactos actual y anterior conservados con hashes correctos',phases['antes']['JAR_sha256']==current['sha256'] and phases['despues']['JAR_sha256']==previous['sha256']),('C02','Cambio real del JAR current solo en el laboratorio',phases['antes']['version']==current['version'] and phases['despues']['version']==previous['version'] and current['sha256']!=previous['sha256']),('C03','Misma base y esquema compatibles con ambas versiones',CFG['entidades_clases']==CFG['entidades_identicas'] and not CFG['migraciones_empaquetadas'] and counts()==expected),('C04','Datos completos preservados antes y después de revertir',phases['antes']['SQL_equivalente_al_respaldo'] and phases['despues']['SQL_equivalente_al_respaldo']),('C05','Consultas de clientes, cotizaciones, expedientes, pagos y documentos correctas en ambas versiones',all(q['http']==200 and q['success'] and q['registros']==q['esperados'] for p in phases.values() for q in p['consultas'])),('C06','Documentos conservan sus hashes y no se alteraron para revertir',docs_before==docs_after),('C07','Base y aplicación usan únicamente puertos y rutas de laboratorio',confirmed and isolation['puerto']==24306)]
    print('PM13: reversión comprobada; cerrando solo los procesos creados.',flush=True)
except Exception as exc:
    error=type(exc).__name__+': '+str(exc);jsonwrite('error_ejecucion.json',{'error':error,'nota':'Revisar si corresponde al ejecutor o a una incompatibilidad; no se alteran recursos originales.'})
finally:
    try:stop_java()
    except Exception as exc:cleanup_errors.append('Backend laboratorio: '+type(exc).__name__)
    if mysql_process is not None and mysql_process.poll() is None:
        try:
            if confirmed:
                guard();r=subprocess.run([str(ADMIN),'--defaults-file='+str(client),'--no-login-paths','--host=127.0.0.1','--port=24306','--protocol=TCP','--ssl-mode=REQUIRED','shutdown'],capture_output=True,timeout=12,creationflags=subprocess.CREATE_NO_WINDOW)
                (PRIVATE/'mysql-cierre.log').write_bytes(r.stdout+r.stderr);assert r.returncode==0
                mysql_process.wait(timeout=15)
            else:mysql_process.terminate();mysql_process.wait(timeout=12)
        except Exception as exc:
            cleanup_errors.append('MySQL laboratorio: '+type(exc).__name__)
            if mysql_process.poll() is None:mysql_process.terminate();mysql_process.wait(timeout=12)
    if mysql_log:mysql_log.close()
    if java_log:java_log.close()
    for p in [client,bootstrap]:
        if p.exists():p.unlink()

after={rel:sha(PROJECT/rel) for rel in before};deploy_after={rel:sha(DEPLOY/rel) for rel in deploy_before}
services_after=services();jsonwrite('servicios_despues.json',services_after)
health_original=json.loads(api('/api/v1/health',original=True)[1])['data']['status'] if original_running else ('Servicio detenido antes de la prueba' if not listening(8086) else 'Estado cambio')
closed=all(not listening(port) for port in [24306,38086,39086])
cold_source_unchanged=all(sha(source_data/rel)==h for rel,h in source_hashes.items())
checks.extend([('C08','Originales mantienen archivos, versión, estado de despliegue y PID',before==after and deploy_before==deploy_after and services_before==services_after and health_original==health_before),('C09','Copia fuente PM12 permanece intacta',cold_source_unchanged),('C10','Procesos propios cerrados y credenciales temporales retiradas',closed and not cleanup_errors and not client.exists() and not bootstrap.exists())])
csvwrite(OUT/'criterios_PM13.csv',[{'criterio':c,'verificacion':d,'estado':'Cumple' if v else 'No cumple'} for c,d,v in checks])
csvwrite(OUT/'integridad_archivos_operativos.csv',[{'archivo':rel,'sha256_antes':before[rel],'sha256_despues':after[rel],'estado':'Sin cambios' if before[rel]==after[rel] else 'Cambió'} for rel in before])
csvwrite(OUT/'consultas_versiones.csv',[q for phase in phases.values() for q in phase['consultas']])
ok=not error and len(checks)==10 and all(v for _,_,v in checks);now=datetime.now(TZ).isoformat(timespec='seconds')
result={'prueba':'PM13 Reversión de cambios','estado':'Exitoso' if ok else 'Falló','fecha_lima':now,'proyecto_original':str(PROJECT),'version_original_conservada':CFG['artefactos']['Actual']['version'],'reversion_laboratorio':CFG['artefactos']['Actual']['version']+' -> '+CFG['artefactos']['Anterior']['version'],'fases':phases,'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':10,'archivos_operativos_sin_cambios':len(before) if before==after else None,'despliegue_original_sin_cambios':deploy_before==deploy_after,'servicios_originales_mismos_PID':services_before==services_after,'salud_original':health_original,'laboratorio_detenido':closed,'errores_limpieza':cleanup_errors,'error_ejecucion':error,'herramientas':['Java 21 y JAR conservados del despliegue','MySQL 8.4 aislado','Python shutil/subprocess/hashlib','TLS y JWT exclusivo de laboratorio','PowerShell CIM/TCP','JSON y CSV'],'fallos_provocados':False,'alcance':'Reversión planificada del artefacto en laboratorio, sin despliegue fallido provocado. Se mantuvo la misma copia de datos: no hubo migración y ambas versiones validaron el esquema. No se probó el comando Revertir del gestor del servicio Windows sobre producción ni su recuperación automática frente a errores. El retorno a 0.5.5 revierte tambien PDFBox al paquete anterior; solo se hace en laboratorio y no se recomienda desplegar esa dependencia anterior. No se evaluaron todas las pantallas ni la seguridad de la version antigua.'}
jsonwrite('resultado.json',result)

result['copia_PM12_sin_cambios']=cold_source_unchanged
result['laboratorio_privado']=str(PRIVATE)
jsonwrite('resultado.json',result)
lines=['Prueba 13 - Reversion de cambios',
    'Version inicial (copia): '+CFG['artefactos']['Actual']['version'],
    'Version recuperada (copia): '+CFG['artefactos']['Anterior']['version'],
    'Cambio de archivo JAR verificado: '+('SI' if len(phases)==2 and phases['antes']['JAR_sha256']!=phases['despues']['JAR_sha256'] else 'NO'),
    'Salud de ambas versiones: '+('UP / UP' if len(phases)==2 else 'No completado'),
    'Consultas correctas: '+str(sum(q['http']==200 and q['success'] and q['registros']==q['esperados'] for p in phases.values() for q in p['consultas']))+'/10',
    f'Datos conservados: {len(expected)} tablas y {sum(expected.values())} registros' if len(phases)==2 and all(p['SQL_equivalente_al_respaldo'] for p in phases.values()) else 'Conservacion de datos: revisar resultado.json',
    'Documentos sin cambios: '+str(len(docs_before))+'/'+str(len(manifest['documentos'])) if 'docs_after' in globals() and docs_before==docs_after else 'Documentos: verificacion incompleta',
    'Despliegue original sin cambios: '+('SI' if result['despliegue_original_sin_cambios'] else 'NO'),
    'Backend original: '+('activo, sin reinicios' if original_running else 'detenido antes y despues'),
    'Servicios originales sin reinicios: '+('SI' if result['servicios_originales_mismos_PID'] else 'NO'),
    'Copia de la prueba 12 sin cambios: '+('SI' if cold_source_unchanged else 'NO'),
    'Procesos de laboratorio detenidos: '+('SI' if closed else 'NO'),
    'Verificaciones: '+str(result['criterios_cumplidos'])+'/10',
    'Estado: '+('EXITOSO' if ok else 'FALLO')]
(OUT/'resultado.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8-sig')
print('\n'.join(lines),flush=True)
if error:print('Diagnostico: '+error)
sys.exit(0 if ok else 1)
