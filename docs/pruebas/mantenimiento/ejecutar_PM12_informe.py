"""Restauración real PM12 exclusivamente en procesos y rutas de laboratorio."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
import base64,csv,hashlib,hmac,io,json,os,re,secrets,shutil,socket,subprocess,time,urllib.request,urllib.error,zipfile
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent/'PM12'))
import respaldo_runtime as backup_runtime

import argparse
ap=argparse.ArgumentParser(description='Prueba 12 - Restauracion del respaldo')
ap.add_argument('--reanudar-preparacion',action='store_true')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
ap.add_argument('--evidencias-originales',default=str(evidencias_originales()))
args=ap.parse_args();PROJECT=Path(args.proyecto).resolve();BASE=Path(args.evidencias_originales)
OUT=PROJECT/'evidencias-mantenimiento/Evidencia_Prueba_12';case=2
if args.reanudar_preparacion:
    assert OUT.is_dir() and not any(OUT.iterdir()),'Solo se reanuda una preparacion vacia.'
else:
    while OUT.exists():OUT=PROJECT/'evidencias-mantenimiento'/f'Evidencia_Prueba_12_{case}';case+=1
    OUT.mkdir(parents=True,exist_ok=False)
PRIVATE=ruta_laboratorio(PROJECT)/OUT.name
if args.reanudar_preparacion:
    assert PRIVATE.is_dir() and not any(PRIVATE.iterdir()),'No se reutiliza una instancia con datos.'
else:
    assert not PRIVATE.exists(),'No reutilizar un laboratorio anterior.'
    PRIVATE.mkdir(parents=True)
assert PRIVATE.resolve().is_relative_to((ruta_laboratorio(PROJECT)).resolve())
# Restringir antes de descifrar datos reales o crear credenciales temporales.
quoted=str(PRIVATE).replace("'","''")
aclcode="$taskP='"+quoted+"';$taskSid=[Security.Principal.WindowsIdentity]::GetCurrent().User;$taskAcl=[Security.AccessControl.DirectorySecurity]::new();$taskAcl.SetOwner($taskSid);$taskAcl.SetAccessRuleProtection($true,$false);foreach($taskAllowed in @($taskSid,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'),[Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))){$taskAcl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($taskAllowed,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))};[IO.Directory]::SetAccessControl($taskP,$taskAcl);if(-not [IO.Directory]::GetAccessControl($taskP).AreAccessRulesProtected){throw 'ACL no protegida'}"
acl=subprocess.run([str(powershell()),'-NoProfile','-Command',aclcode],capture_output=True,timeout=15)
assert acl.returncode==0,'No se confirmo el acceso privado; no se descifran datos.'
RESTORE=PRIVATE/'restaurado';DATA=PRIVATE/'mysql-data';APP=PRIVATE/'aplicacion'
for folder in [RESTORE,APP,PRIVATE/'mysql-files']:folder.mkdir()
CONFIG={'mysql_port':23306,'app_port':28086,'metrics_port':29086,'backup_pm11':str(BASE/'PM11_20261122-222724/resultado_PM11.json'),'privado':str(PRIVATE),'objetivos_RTO_RPO':'En revision; se miden tiempos sin declarar un SLA.'}
(OUT/'configuracion_laboratorio.json').write_text(json.dumps(CONFIG,ensure_ascii=False,indent=2),encoding='utf-8')

MYSQLBASE=mysql_home();MYSQL=MYSQLBASE/'bin/mysql.exe';MYSQLD=MYSQLBASE/'bin/mysqld.exe';ADMIN=MYSQLBASE/'bin/mysqladmin.exe';DUMP=MYSQLBASE/'bin/mysqldump.exe'
JAVA=(java_home()/'bin/java.exe')
PS=Path(str(powershell()))
TZ=timezone(timedelta(hours=-5));START=datetime.now(TZ);CLOCK=time.perf_counter()
root_password=secrets.token_hex(32);app_password=secrets.token_hex(32)
root_cnf=PRIVATE/'cliente-laboratorio.cnf';bootstrap=PRIVATE/'bootstrap-laboratorio.sql'
mysql_process=None;java_process=None;mysql_log=None;java_log=None;lab_confirmed=False
evidence={};failure='';cleanup_errors=[];checks=[]
def sha(p):return backup_runtime.sha(p)
def writejson(name,value):
    p=OUT/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf-8')
def csvwrite(name,rows):
    p=OUT/name
    with p.open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
def psjson(code):
    r=subprocess.run([str(PS),'-NoProfile','-Command',code],env={k:v for k,v in os.environ.items() if k!='PSModulePath'},capture_output=True,timeout=12)
    if r.returncode:raise RuntimeError('No se pudo consultar estado Windows; sin acciones sobre servicios.')
    return json.loads(r.stdout.decode('utf-8-sig',errors='replace'))
def services():return psjson("@(Get-CimInstance Win32_Service -Filter \"Name='MySQL84' OR Name='TransAduanasBackend'\" | Select-Object Name,State,ProcessId) | ConvertTo-Json -Compress")
def listening(port):
    with socket.socket() as s:s.settimeout(.3);return s.connect_ex(('127.0.0.1',port))==0
def http(path,token=None,lab=True):
    port=CONFIG['app_port'] if lab else 8086
    headers={'Authorization':'Bearer '+token} if token else {}
    req=urllib.request.Request(f'http://127.0.0.1:{port}'+path,headers=headers)
    with urllib.request.urlopen(req,timeout=10) as r:return r.status,r.read()
def mysqlargs():
    return [str(MYSQL),'--defaults-file='+str(root_cnf),'--no-login-paths','--host=127.0.0.1','--port='+str(CONFIG['mysql_port']),'--protocol=TCP','--ssl-mode=REQUIRED','--batch','--raw','--skip-column-names','--default-character-set=utf8mb4']
def sql(query,timeout=20):
    r=subprocess.run(mysqlargs(),input=query.encode('utf-8'),capture_output=True,timeout=timeout,creationflags=subprocess.CREATE_NO_WINDOW)
    if r.returncode:
        (PRIVATE/'error-cliente-laboratorio.txt').write_bytes(r.stderr)
        raise RuntimeError('SQL de laboratorio devolvió un error; detalles privados conservados.')
    return r.stdout.decode('utf-8')
def guard():
    text=sql('SELECT @@port,@@datadir;').strip().split('\t')
    assert int(text[0])==CONFIG['mysql_port'] and Path(text[1]).resolve()==DATA.resolve(),'El importador no apunta al datadir/puerto del laboratorio.'
    assert DATA.resolve().is_relative_to(PRIVATE.resolve()) and CONFIG['mysql_port']!=3306
    return {'puerto':int(text[0]),'datadir':str(Path(text[1]).resolve())}
def tablecounts():
    return {t:int(n) for t,n in (line.split('\t') for line in sql(' UNION ALL '.join(f"SELECT '{t}',COUNT(*) FROM `{DB}`.`{t}`" for t in manifest['tablas_y_filas'])+';').strip().splitlines())}
def jwt(subject,secret):
    def enc(value):return base64.urlsafe_b64encode(json.dumps(value,separators=(',',':')).encode()).rstrip(b'=')
    now=int(time.time());head=enc({'alg':'HS256','typ':'JWT'});body=enc({'sub':subject,'tipoUsuario':'PERSONAL_INTERNO','iat':now,'exp':now+600})
    unsigned=head+b'.'+body;sig=base64.urlsafe_b64encode(hmac.new(secret.encode(),unsigned,hashlib.sha256).digest()).rstrip(b'=')
    return (unsigned+b'.'+sig).decode()
def normalized_dump(data):
    lines=[]
    for line in data.decode('utf-8-sig').splitlines():
        if line.startswith('--') or not line.strip():continue
        # COLLATE utf8mb4_* ya determina el charset; solo normalizar sintaxis
        # redundante inmediatamente después de un tipo de columna de texto.
        line=re.sub(r'^(\s*`[^`]+`\s+(?:varchar\(\d+\)|char\(\d+\)|(?:tiny|medium|long)?text)) CHARACTER SET utf8mb4 (COLLATE utf8mb4_[A-Za-z0-9_]+)',r'\1 \2',line)
        lines.append(line.rstrip())
    return '\n'.join(lines)

protected=[]
for folder in ['backend-sga/src','app-sga/src','app-b2b/src','app-landing/src','db','uploads','.github','monitoreo','scripts']:
    protected.extend(p for p in (PROJECT/folder).rglob('*') if p.is_file() and p.suffix.lower() not in ['.log','.lock'])
for folder in ['backend-sga','app-sga','app-b2b','app-landing']:
    protected.extend(p for p in (PROJECT/folder).glob('*') if p.is_file() and p.suffix!='.md')
before={p.relative_to(PROJECT).as_posix():sha(p) for p in sorted(set(protected))}
services_before=services();writejson('servicios_antes.json',services_before)
memory=psjson('(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory')
assert int(memory)>=1572864,'Memoria insuficiente para agregar el laboratorio con seguridad.'
original_health_before=json.loads(http('/api/v1/health',lab=False)[1]);assert original_health_before['data']['status']=='UP'
try:
    print('PM12: verificando y descifrando el respaldo privado de PM11.',flush=True)
    previous=json.loads(Path(CONFIG['backup_pm11']).read_text(encoding='utf-8'))
    encrypted=Path(previous['respaldo_cifrado']);keyfile=Path(previous['clave_protegida'])
    assert sha(encrypted)==previous['sha256_respaldo_cifrado']
    encrypted_hash_before=sha(encrypted);key_hash_before=sha(keyfile)
    raw=encrypted.read_bytes();header=b'TRANSADUNAS-PM11-AES256GCM-v1\n';assert raw.startswith(header)
    key=backup_runtime.dpapi(keyfile.read_bytes(),False)
    plain=AESGCM(key).decrypt(raw[len(header):len(header)+12],raw[len(header)+12:],header)
    with zipfile.ZipFile(io.BytesIO(plain)) as z:
        assert z.testzip() is None and len(z.namelist())==len(set(z.namelist()))
        manifest=json.loads(z.read('manifiesto.json'));DB=manifest['base_de_datos'];assert re.fullmatch(r'[A-Za-z0-9_]+',DB)
        allowed={'SGAdb.sql','manifiesto.json'}|{'documentos/'+rel for rel in manifest['documentos']}
        assert set(z.namelist())==allowed
        for info in z.infolist():
            dest=(RESTORE/info.filename).resolve();assert dest.is_relative_to(RESTORE.resolve()) and not info.is_dir()
            dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z.read(info))
    del key,plain,raw
    sqlfile=RESTORE/'SGAdb.sql';original_sql=sqlfile.read_bytes()
    assert sha(sqlfile)==manifest['sql']['sha256']
    docs_ok=all(sha(RESTORE/'documentos'/rel)==meta['sha256'] for rel,meta in manifest['documentos'].items())
    assert docs_ok
    # Validar alcance del SQL antes de importarlo; el cliente deshabilita comandos de shell.
    text=original_sql.decode('utf-8')
    uses=re.findall(r'^USE\s+`([^`]+)`\s*;',text,re.M);assert uses and set(uses)=={DB}
    for line in text.splitlines():
        if re.match(r'^\s*(?:CREATE|DROP)\s+DATABASE\b',line,re.I):assert re.search(r'`'+re.escape(DB)+r'`',line)
        assert not re.match(r'^\s*(?:CREATE\s+USER|ALTER\s+USER|GRANT\s|INSTALL\s+PLUGIN|LOAD\s+DATA|SOURCE\s|SYSTEM\s|SET\s+(?:@@GLOBAL|GLOBAL\s)|\\!)',line,re.I),'SQL fuera del alcance de restauración.'
    restore_files_seconds=time.perf_counter()-CLOCK
    for port in [CONFIG['mysql_port'],CONFIG['app_port'],CONFIG['metrics_port']]:
        with socket.socket() as s:s.bind(('127.0.0.1',port))
    assert not DATA.exists(),'No reutilizar datos de otra ejecución.'
    root_cnf.write_text('[client]\nuser=root\npassword="'+root_password+'"\nhost=127.0.0.1\nport='+str(CONFIG['mysql_port'])+'\n',encoding='utf-8')
    bootstrap.write_text("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+root_password+"';\n",encoding='utf-8')
    print('PM12: inicializando MySQL nuevo, sin instalar ni detener servicios.',flush=True)
    initcmd=[str(MYSQLD),'--no-defaults','--initialize-insecure','--basedir='+str(MYSQLBASE),'--datadir='+str(DATA),'--innodb-buffer-pool-size=32M','--innodb-redo-log-capacity=16M','--console']
    initialized=subprocess.run(initcmd,capture_output=True,timeout=45,creationflags=subprocess.CREATE_NO_WINDOW)
    (PRIVATE/'mysql-inicializacion.log').write_bytes(initialized.stdout+initialized.stderr)
    assert initialized.returncode==0,'MySQL aislado no pudo inicializarse.'
    mysqlcmd=[str(MYSQLD),'--no-defaults','--basedir='+str(MYSQLBASE),'--datadir='+str(DATA),'--port='+str(CONFIG['mysql_port']),'--bind-address=127.0.0.1','--mysqlx=OFF','--performance-schema=OFF','--skip-log-bin','--event-scheduler=OFF','--local-infile=OFF','--secure-file-priv='+str(PRIVATE/'mysql-files'),'--innodb-buffer-pool-size=32M','--innodb-redo-log-capacity=16M','--init-file='+str(bootstrap),'--console']
    mysql_log=(PRIVATE/'mysql-proceso.log').open('wb')
    mysql_process=subprocess.Popen(mysqlcmd,cwd=PRIVATE,stdout=mysql_log,stderr=mysql_log,creationflags=subprocess.CREATE_NO_WINDOW)
    deadline=time.perf_counter()+40
    while not listening(CONFIG['mysql_port']):
        if mysql_process.poll() is not None:raise RuntimeError('MySQL aislado se cerró durante el arranque.')
        if time.perf_counter()>deadline:raise TimeoutError('MySQL aislado no alcanzó escucha dentro del plazo.')
        time.sleep(.3)
    isolation=guard();lab_confirmed=True;bootstrap.unlink()
    isolation['pid_mysql']=mysql_process.pid;writejson('aislamiento_mysql.json',isolation)
    print('PM12: destino confirmado en puerto 23306 y datadir privado; importando la copia.',flush=True)
    guard() # Verificación inmediata antes de importar.
    with sqlfile.open('rb') as source:
        imported=subprocess.run(mysqlargs()+['--binary-mode'],stdin=source,capture_output=True,timeout=30,creationflags=subprocess.CREATE_NO_WINDOW)
    (PRIVATE/'importacion.log').write_bytes(imported.stdout+imported.stderr)
    assert imported.returncode==0,'La importación devolvió un error; no se atribuye éxito.'
    counts=tablecounts();assert counts==manifest['tablas_y_filas'],'Los recuentos restaurados difieren del respaldo.'
    # Comparación de una reexportación de la base restaurada, antes de iniciar la aplicación.
    redump=PRIVATE/'reexportacion.sql'
    exportcmd=[str(DUMP),'--defaults-file='+str(root_cnf),'--no-login-paths','--host=127.0.0.1','--port='+str(CONFIG['mysql_port']),'--protocol=TCP','--ssl-mode=REQUIRED','--default-character-set=utf8mb4','--single-transaction','--skip-lock-tables','--routines','--events','--triggers','--no-tablespaces','--hex-blob','--set-gtid-purged=OFF','--databases',DB,'--result-file='+str(redump)]
    exported=subprocess.run(exportcmd,capture_output=True,timeout=20,creationflags=subprocess.CREATE_NO_WINDOW)
    (PRIVATE/'reexportacion-errores.log').write_bytes(exported.stderr);assert exported.returncode==0
    same_dump=normalized_dump(original_sql)==normalized_dump(redump.read_bytes())
    writejson('comparacion_sql.json',{'reexportacion_normalizada_identica':same_dump,'normalizacion':'Comentarios --, vacías, espacios finales y CHARACTER SET utf8mb4 redundante junto a COLLATE utf8mb4_* en columnas char/varchar/text. No se cambian INSERT, valores, tipos, colaciones ni restricciones.','sha256_original_normalizado':hashlib.sha256(normalized_dump(original_sql).encode()).hexdigest(),'sha256_restaurado_normalizado':hashlib.sha256(normalized_dump(redump.read_bytes()).encode()).hexdigest()})
    fk_rows=sql("SELECT CONSTRAINT_NAME,TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='"+DB+"' AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY CONSTRAINT_NAME,ORDINAL_POSITION;").strip().splitlines()
    groups={}
    for row in fk_rows:
        name,table,col,parent,pcol=row.split('\t');groups.setdefault((name,table,parent),[]).append((col,pcol))
    relationships=[]
    for (name,table,parent),columns in groups.items():
        join=' AND '.join(f'c.`{a}`=p.`{b}`' for a,b in columns)
        nonnull=' AND '.join(f'c.`{a}` IS NOT NULL' for a,b in columns)
        orphan=int(sql(f'SELECT COUNT(*) FROM `{DB}`.`{table}` c LEFT JOIN `{DB}`.`{parent}` p ON {join} WHERE {nonnull} AND p.`{columns[0][1]}` IS NULL;').strip())
        relationships.append({'restriccion':name,'tabla':table,'referenciada':parent,'huerfanos':orphan})
    csvwrite('relaciones_restauradas.csv',relationships);assert all(r['huerfanos']==0 for r in relationships)
    logical_restore_seconds=time.perf_counter()-CLOCK
    # Usuario de aplicación SOLO SELECT; root se reserva al importador del laboratorio.
    guard()
    sql("CREATE USER 'pm12_lector'@'127.0.0.1' IDENTIFIED BY '"+app_password+"'; GRANT SELECT ON `"+DB+"`.* TO 'pm12_lector'@'127.0.0.1';")
    active=(ruta_despliegue(raiz_proyecto(__file__))/'current/app.jar')
    jarhash=sha(active)
    backup_jar=active.parents[1]/'releases'/manifest['sha256_JAR_activo'].upper()/'app.jar'
    assert backup_jar.is_file() and sha(backup_jar).lower()==manifest['sha256_JAR_activo'].lower()
    with zipfile.ZipFile(active) as current_jar,zipfile.ZipFile(backup_jar) as recorded_jar:
        entity_names=[n for n in recorded_jar.namelist() if n.startswith('BOOT-INF/classes/pe/sga/entity/') and n.endswith('.class')]
        assert entity_names and all(current_jar.read(n)==recorded_jar.read(n) for n in entity_names),'El modelo de datos difiere del respaldo; revisar compatibilidad.'
    writejson('compatibilidad_JAR.json',{'JAR_actual_copiado':jarhash,'JAR_de_referencia':manifest['sha256_JAR_activo'],'entidades_identicas':len(entity_names),'nota':'El parche de bibliotecas no cambia las entidades; JPA validate comprobara el esquema restaurado.'})

    shutil.copy2(active,APP/'backend.jar');assert sha(APP/'backend.jar')==jarhash
    secret=base64.urlsafe_b64encode(os.urandom(32)).decode().rstrip('=')
    env=os.environ.copy()
    for name in list(env):
        if name.startswith(('SPRING_','DB_','JPA_','JWT_','STORAGE_','SERVER_','MONITORING_')) or name in ['JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','JAVA_OPTS','LOG_PATH']:env.pop(name,None)
    jdbc=f'jdbc:mysql://127.0.0.1:{CONFIG["mysql_port"]}/{DB}?sslMode=REQUIRED&serverTimezone=America/Lima'
    env.update(DB_URL=jdbc,SPRING_DATASOURCE_URL=jdbc,DB_USERNAME='pm12_lector',SPRING_DATASOURCE_USERNAME='pm12_lector',DB_PASSWORD=app_password,SPRING_DATASOURCE_PASSWORD=app_password,JWT_SECRET=secret,STORAGE_DIR=str(RESTORE/'documentos'),CORS_ALLOWED_ORIGINS=f'http://127.0.0.1:{CONFIG["app_port"]}',LOG_PATH=str(APP/'logs'))
    javacmd=[str(JAVA),'-Xms64m','-Xmx256m','-DLOG_PATH='+str(APP/'logs'),'-jar',str(APP/'backend.jar'),'--spring.profiles.active=staging','--spring.config.location=classpath:/application.properties,classpath:/application-staging.properties','--spring.config.import=','--server.address=127.0.0.1','--server.port='+str(CONFIG['app_port']),'--management.server.address=127.0.0.1','--management.server.port='+str(CONFIG['metrics_port']),'--spring.jpa.hibernate.ddl-auto=validate','--spring.jpa.show-sql=false','--spring.datasource.hikari.read-only=true','--app.data-initializer.enabled=false','--app.storage.upload-dir='+str(RESTORE/'documentos')]
    writejson('configuracion_aplicacion_no_secreta.json',{'jdbc':jdbc,'almacenamiento':str(RESTORE/'documentos'),'puerto':CONFIG['app_port'],'puerto_metricas':CONFIG['metrics_port'],'cuenta_db':'pm12_lector con SELECT','JPA':'validate','inicializador':False,'sha256_JAR':jarhash,'configuracion_importada':'Solo classpath del JAR; importaciones de archivos externos desactivadas','clave_JWT':'Nueva y exclusiva de laboratorio; no se registra su valor.'})
    java_log=(PRIVATE/'aplicacion.log').open('wb')
    print('PM12: tablas y relaciones comprobadas; iniciando copia del backend en 28086.',flush=True)
    java_process=subprocess.Popen(javacmd,cwd=APP,env=env,stdout=java_log,stderr=java_log,creationflags=subprocess.CREATE_NO_WINDOW)
    deadline=time.perf_counter()+65
    while True:
        if java_process.poll() is not None:raise RuntimeError('La aplicación de laboratorio terminó al iniciar; revisar su log privado.')
        if listening(CONFIG['app_port']):
            try:
                status,body=http('/api/v1/health')
                if status==200 and json.loads(body).get('data',{}).get('status')=='UP':break
            except (OSError,urllib.error.URLError):pass
        if time.perf_counter()>deadline:raise TimeoutError('El backend aislado no alcanzó salud en el plazo.')
        time.sleep(.5)
    admin=sql(f"SELECT username FROM `{DB}`.usuario WHERE rol='ADMINISTRADOR_TI' AND estado_activo=1 ORDER BY id_usuario LIMIT 1;").strip();assert admin
    token=jwt(admin,secret);requests=[]
    for label,path,table in [('clientes','/api/v1/clientes','cliente'),('cotizaciones','/api/v1/cotizaciones','cotizacion'),('expedientes','/api/v1/expedientes','expediente'),('pagos','/api/v1/finanzas/anticipos','cotizacion_pago'),('documentos activos','/api/v1/documentos',None)]:
        status,body=http(path,token);response=json.loads(body)
        (PRIVATE/(label.replace(' ','-')+'-respuesta.json')).write_bytes(body)
        data=response.get('data');count=len(data) if isinstance(data,list) else None
        expected=counts[table] if table else int(sql(f'SELECT COUNT(*) FROM `{DB}`.documento_adjunto WHERE activo=1;').strip())
        requests.append({'consulta':label,'ruta':path,'http':status,'success':response.get('success'),'registros_obtenidos':count,'esperados':expected})
    csvwrite('consultas_aplicacion.csv',requests)
    assert all(r['http']==200 and r['success'] is True and r['registros_obtenidos']==r['esperados'] for r in requests),'Una consulta de la aplicación no coincide con la base restaurada.'
    doc_rows=sql(f'SELECT id_documento,ruta_almacenamiento,activo FROM `{DB}`.documento_adjunto ORDER BY id_documento;').strip().splitlines()
    doc_checks=[];downloads=[]
    for row in doc_rows:
        docid,rel,active_flag=row.split('\t');target=(RESTORE/'documentos'/rel).resolve()
        assert target.is_relative_to((RESTORE/'documentos').resolve()),'La ruta documental apunta fuera del almacenamiento de laboratorio.'
        exists=target.is_file();meta=manifest['documentos'].get(rel)
        match=exists and meta is not None and sha(target)==meta['sha256']
        doc_checks.append({'id_documento':docid,'archivo':rel,'activo':active_flag,'archivo_restaurado':exists,'hash_coincide':match})
        if active_flag=='1' and match:
            status,data=http('/api/v1/documentos/'+docid+'/descargar',token)
            downloads.append({'id_documento':docid,'http':status,'hash_coincide':hashlib.sha256(data).hexdigest()==meta['sha256']})
    if doc_checks:csvwrite('referencias_documentales.csv',doc_checks)
    if downloads:csvwrite('descargas_documentales.csv',downloads)
    assert all(r['hash_coincide'] for r in doc_checks),'Hay referencias documentales sin archivo restaurado.'
    assert all(r['http']==200 and r['hash_coincide'] for r in downloads)
    connections=psjson(f"@(Get-NetTCPConnection -OwningProcess {java_process.pid} -State Established -ErrorAction SilentlyContinue | Where-Object RemotePort -eq {CONFIG['mysql_port']} | Select-Object LocalAddress,RemoteAddress,RemotePort,OwningProcess) | ConvertTo-Json -Compress")
    if isinstance(connections,dict):connections=[connections]
    assert connections and all(c['RemotePort']==CONFIG['mysql_port'] for c in connections)
    writejson('conexion_backend_aislada.json',connections)
    # Solo SELECT: las consultas no deben alterar la base restaurada.
    counts_after=tablecounts();assert counts_after==counts
    # Reexportar después de consultar para detectar cambios de contenido, además del recuento.
    exported=subprocess.run(exportcmd,capture_output=True,timeout=20,creationflags=subprocess.CREATE_NO_WINDOW);assert exported.returncode==0
    same_after=normalized_dump(original_sql)==normalized_dump(redump.read_bytes())
    recovery_seconds=time.perf_counter()-CLOCK
    snapshots=datetime.fromisoformat(manifest['consistencia']['fecha_lima_adquisicion'])
    age_at_start=(START-snapshots).total_seconds()
    evidence={'tablas':len(counts),'filas_totales':sum(counts.values()),'conteos':counts,'relaciones':len(relationships),'relaciones_sin_huerfanos':all(r['huerfanos']==0 for r in relationships),'documentos_archivo':len(manifest['documentos']),'documentos_hash_correcto':docs_ok,'referencias_documentales':len(doc_checks),'documentos_activos_descargados':len(downloads),'consultas_http':requests,'reexportacion_identica_antes':same_dump,'reexportacion_identica_despues':same_after,'tiempo_restauracion_sql_y_archivos_segundos':round(logical_restore_seconds,6),'tiempo_hasta_verificacion_aplicacion_segundos':round(recovery_seconds,6),'antiguedad_respaldo_inicio_segundos':round(age_at_start,6),'objetivos_RTO_RPO':'No confirmados: no se declara cumplimiento de un objetivo ni pérdida real frente a un incidente.','PID_mysql_laboratorio':mysql_process.pid,'PID_backend_laboratorio':java_process.pid,'puerto_mysql':CONFIG['mysql_port'],'puerto_backend':CONFIG['app_port'],'datadir':str(DATA),'archivos_originales_no_conectados':'El importador solo usó root aleatorio en 23306. Nunca conectó a 3306.'}
    writejson('verificaciones_restauracion.json',evidence)
    checks=[('C01','Respaldo cifrado autenticado y SQL/documentos con hash correcto',docs_ok),('C02','Instancia MySQL separada y destino comprobado antes de importar',lab_confirmed and isolation['puerto']==23306 and Path(isolation['datadir']).resolve()==DATA.resolve()),('C03','Recuentos exactos en todas las tablas',counts==manifest['tablas_y_filas']),('C04','SQL reexportado equivalente con datos identicos y charset redundante normalizado',same_dump and same_after),('C05','Relaciones restauradas sin huérfanos',all(r['huerfanos']==0 for r in relationships)),('C06','Archivos y referencias documentales restaurados con hash correcto',docs_ok and all(r['hash_coincide'] for r in doc_checks)),('C07','Backend aislado consulta clientes, cotizaciones, expedientes, pagos y documentos',all(r['http']==200 and r['success'] for r in requests)),('C08','Conexión del backend solo al MySQL de laboratorio',bool(connections))]
    print('PM12: comprobación funcional completada; cerrando solo los procesos del laboratorio.',flush=True)
except Exception as exc:
    failure=type(exc).__name__+': '+str(exc)
    writejson('error_ejecucion.json',{'fecha_lima':datetime.now(TZ).isoformat(),'error':failure,'nota':'No se atribuye automáticamente a la aplicación: revisar el ejecutor y los logs privados. Se ejecuta limpieza de procesos propios.'})
finally:
    if java_process is not None and java_process.poll() is None:
        try:java_process.terminate();java_process.wait(timeout=12)
        except Exception as exc:cleanup_errors.append('Cierre del backend de laboratorio: '+type(exc).__name__)
    if mysql_process is not None and mysql_process.poll() is None:
        try:
            if lab_confirmed:
                guard()
                stopped=subprocess.run([str(ADMIN),'--defaults-file='+str(root_cnf),'--no-login-paths','--host=127.0.0.1','--port='+str(CONFIG['mysql_port']),'--protocol=TCP','--ssl-mode=REQUIRED','shutdown'],capture_output=True,timeout=12,creationflags=subprocess.CREATE_NO_WINDOW)
                (PRIVATE/'mysql-cierre.log').write_bytes(stopped.stdout+stopped.stderr)
                if stopped.returncode:raise RuntimeError('Cierre normal no confirmado.')
                mysql_process.wait(timeout=15)
            else:mysql_process.terminate();mysql_process.wait(timeout=12)
        except Exception as exc:
            cleanup_errors.append('Cierre MySQL de laboratorio: '+type(exc).__name__)
            if mysql_process.poll() is None:mysql_process.terminate();mysql_process.wait(timeout=12)
    if mysql_log:mysql_log.close()
    if java_log:java_log.close()
    for temp in [root_cnf,bootstrap]:
        if temp.exists():temp.unlink()

services_after=services();writejson('servicios_despues.json',services_after)
after={rel:sha(PROJECT/rel) for rel in before};unchanged=before==after
csvwrite('integridad_archivos_operativos.csv',[{'archivo':rel,'sha256_antes':before[rel],'sha256_despues':after[rel],'estado':'Sin cambios' if before[rel]==after[rel] else 'Cambió'} for rel in before])
original_health_after=json.loads(http('/api/v1/health',lab=False)[1])
stopped_all=all(not listening(p) for p in [CONFIG['mysql_port'],CONFIG['app_port'],CONFIG['metrics_port']])
checks.extend([('C09','Servicios originales conservan PID y estado y siguen saludables',services_before==services_after and original_health_after['data']['status']=='UP'),('C10','Código, configuración, documentos y respaldo originales sin cambios',unchanged and sha(encrypted)==encrypted_hash_before and sha(keyfile)==key_hash_before),('C11','Procesos propios detenidos y credenciales temporales eliminadas',stopped_all and not cleanup_errors and not root_cnf.exists() and not bootstrap.exists())])
csvwrite('criterios_PM12.csv',[{'criterio':c,'verificacion':d,'resultado':'Cumple' if v else 'No cumple'} for c,d,v in checks])
ok=not failure and all(v for _,_,v in checks) and len(checks)==11
now=datetime.now(TZ).isoformat(timespec='seconds')
result={'prueba':'PM12 Restauración integral','estado':'Exitoso' if ok else 'Falló','fecha_lima':now,'proyecto_original':str(PROJECT),'laboratorio':str(OUT),'herramientas':['MySQL 8.4 mysqld/mysql/mysqldump/mysqladmin','Java 21 y JAR original copiado','TLS','AES-256-GCM y DPAPI','PowerShell CIM y conexiones TCP','Python zipfile/hashlib/HTTP'],'verificaciones':evidence,'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':11,'archivos_operativos_sin_cambios':len(before) if unchanged else None,'servicios_originales_mismos_PID':services_before==services_after,'salud_original_despues':original_health_after['data']['status'],'procesos_laboratorio_detenidos':stopped_all,'errores_limpieza':cleanup_errors,'error_ejecucion':failure,'fallos_provocados':False,'limites':'Restauración funcional local en procesos separados con puertos loopback y cuenta de aplicación solo SELECT. No es un simulacro de caída ni una restauración sobre producción. No se probaron todas las pantallas o flujos de negocio ni se crearon operaciones ausentes en el respaldo. RTO/RPO objetivo sin confirmar. La antigüedad del respaldo es exposición temporal, no pérdida de datos real medida ante un incidente.'}
result['laboratorio_privado']=str(PRIVATE)
result['restauracion_nueva']=True
writejson('resultado.json',result)
lines=['Prueba 12 - Restauracion del respaldo','',f'MySQL del laboratorio: puerto {CONFIG["mysql_port"]}',f'Backend del laboratorio: puerto {CONFIG["app_port"]}','']
if evidence:
    lines += [f'Tablas restauradas: {evidence["tablas"]}',f'Registros restaurados: {evidence["filas_totales"]}',f'Relaciones sin registros huerfanos: {evidence["relaciones"]}' if evidence['relaciones_sin_huerfanos'] else 'Relaciones: REVISAR',f'Documentos recuperados: {evidence["documentos_archivo"]}','Integridad SQL y documentos: '+('CORRECTA' if evidence['documentos_hash_correcto'] and evidence['reexportacion_identica_antes'] and evidence['reexportacion_identica_despues'] else 'REVISAR'),'','Consultas al backend restaurado:',f'{"Consulta":<22}{"HTTP":>6}{"Registros":>12}']
    for request in evidence['consultas_http']:
        lines.append(f'{request["consulta"]:<22}{request["http"]:>6}{request["registros_obtenidos"]:>12}')
    lines += ['',f'Restauracion y validacion de datos: {evidence["tiempo_restauracion_sql_y_archivos_segundos"]:.2f} segundos',f'Ensayo completo hasta validar la aplicacion: {evidence["tiempo_hasta_verificacion_aplicacion_segundos"]:.2f} segundos']
lines += ['','Servicios originales sin reinicios: '+('SI' if services_before==services_after else 'REVISAR'),'Archivos originales sin cambios: '+('SI' if unchanged else 'NO'),'Procesos del laboratorio detenidos: '+('SI' if stopped_all else 'NO'),f'Criterios cumplidos: {result["criterios_cumplidos"]}/11','Estado: '+('EXITOSO' if ok else 'FALLO')]
if failure:lines += ['Diagnostico: '+failure]
text='\n'.join(lines);(OUT/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if ok else 1)
