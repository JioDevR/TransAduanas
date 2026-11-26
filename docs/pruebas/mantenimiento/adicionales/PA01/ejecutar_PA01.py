"""PA01 adicional: disponibilidad e integracion de lectura, sin cambiar PM01-PM15."""
from pathlib import Path
from datetime import datetime,timezone,timedelta
from html.parser import HTMLParser
from urllib.parse import urlsplit,urljoin
import argparse,base64,csv,hashlib,json,os,re,secrets,shutil,socket,subprocess,sys,time,urllib.request,xml.etree.ElementTree as ET
sys.path.insert(0,str(Path(__file__).resolve().parents[2]))
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
ap=argparse.ArgumentParser(description='PA01 - Disponibilidad y conectividad en entorno temporal')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)));a=ap.parse_args()
PROJECT=Path(a.proyecto).resolve();DEPLOY=ruta_despliegue(PROJECT);HERE=Path(__file__).resolve().parent
PS=Path(str(powershell()));MYSQL=(mysql_home()/'bin/mysql.exe');JAVA=(java_home()/'bin/java.exe')
PORTS=[8085,5180,5174,19085];OUT=PROJECT/'evidencias-mantenimiento/Evidencia_Adicional_1';i=2;LABROOT=ruta_laboratorio(PROJECT)
while OUT.exists() or (LABROOT/OUT.name).exists():OUT=PROJECT/f'evidencias-mantenimiento/Evidencia_Adicional_1_{i}';i+=1
PRIVATE=LABROOT/OUT.name
assert PRIVATE.resolve().is_relative_to(LABROOT.resolve())
OUT.mkdir(parents=True);PRIVATE.mkdir(parents=True)
procs=[];handles=[];client=PRIVATE/'cliente-lectura.cnf';error='';cleanup_errors=[];db_before=None;db_after=None;source_before={};service_before=None;source_after={};service_after=None;counts={};assets={};api_results=[];details={};flags={f'C{i:02}':False for i in range(1,13)}
labels=['Copia del backend responde HTTP 200 y UP','Conexion SQL con cuenta propia y TLS','Tablas reales del proyecto consultadas en lectura','Catalogos del backend coinciden con los recuentos SQL','SGA entrega HTML y sus recursos compilados','B2B entrega HTML y sus recursos compilados','Ruta API de SGA comunica con backend y base de datos','Ruta API de B2B comunica con backend y base de datos','Arranque y consultas quedan registrados en el laboratorio','Contenido de las tablas conservado antes y despues','Fuentes, PM01-PM15, despliegue y servicios originales sin cambios','Procesos propios cerrados y credencial temporal retirada']
def write(name,obj):(OUT/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2),encoding='utf-8')
def sha(p):
 h=hashlib.sha256()
 with Path(p).open('rb') as f:
  for block in iter(lambda:f.read(1048576),b''):h.update(block)
 return h.hexdigest()
def ps(code):
 r=subprocess.run([str(PS),'-NoProfile','-NonInteractive','-Command',"$ErrorActionPreference='Stop';"+code],env={k:v for k,v in os.environ.items() if k!='PSModulePath'},capture_output=True,timeout=20)
 if r.returncode:raise RuntimeError('No se pudo completar una consulta Windows. No se cambian servicios.')
 return json.loads(r.stdout.decode('utf-8-sig'))
def services():return ps("@(Get-CimInstance Win32_Service -Filter \"Name='MySQL84' OR Name='TransAduanasBackend' OR Name='TransAduanasMantenimiento'\"|Select-Object Name,State,ProcessId,StartName)|ConvertTo-Json -Compress")
def listening(port):
 with socket.socket() as s:s.settimeout(.2);return s.connect_ex(('127.0.0.1',port))==0
def query(sql):
 # Una transaccion de lectura en cada sesion; no se emiten cambios de negocio.
 text='SET SESSION TRANSACTION READ ONLY; START TRANSACTION READ ONLY;\n'+sql+'\nROLLBACK;'
 r=subprocess.run([str(MYSQL),'--defaults-file='+str(client),'--no-login-paths','--protocol=TCP','--ssl-mode=REQUIRED','--connect-timeout=5','--batch','--raw','--skip-column-names','--default-character-set=utf8mb4','--database='+DB],input=text.encode('utf-8'),capture_output=True,timeout=25,creationflags=subprocess.CREATE_NO_WINDOW)
 if r.returncode:
  (PRIVATE/'mysql-error.log').write_bytes(r.stderr);raise RuntimeError('Consulta de lectura SQL no completada; detalle en registro privado.')
 return r.stdout
def database_snapshot():
 rows={}
 for table in tables:
  count=int(query(f'SELECT COUNT(*) FROM `{table}`;').strip());raw=query(f'SELECT * FROM `{table}` ORDER BY 1;')
  rows[table]={'registros':count,'sha256_contenido':hashlib.sha256(raw).hexdigest()}
 return rows
def get(port,path):
 start=time.perf_counter()
 with urllib.request.urlopen(f'http://127.0.0.1:{port}'+path,timeout=12) as r:
  data=r.read();headers=dict(r.headers);code=r.status
 api_results.append({'puerto':port,'ruta':path,'http':code,'bytes':len(data),'segundos':round(time.perf_counter()-start,4)})
 return code,data,headers
def get_json(port,path):
 code,data,_=get(port,path);return code,json.loads(data)
def wait_for(proc,port,label):
 limit=time.monotonic()+90
 while time.monotonic()<limit:
  if proc.poll() is not None:raise RuntimeError(label+' se cerro antes de estar disponible; revisar registro privado.')
  if listening(port):
   try:
    code,data=get_json(port,'/api/v1/health')
    if code==200 and data.get('success') is True and data.get('data',{}).get('status')=='UP':return data
   except (OSError,ValueError):pass
  time.sleep(.5)
 raise TimeoutError(label+' no estuvo disponible dentro del limite.')
def start(cmd,name,env,cwd):
 log=(PRIVATE/(name+'.log')).open('wb');handles.append(log)
 proc=subprocess.Popen(cmd,env=env,cwd=cwd,stdout=log,stderr=log,creationflags=subprocess.CREATE_NO_WINDOW);procs.append((name,proc));return proc
class AssetsParser(HTMLParser):
 def __init__(self):super().__init__();self.urls=[];self.mount=False
 def handle_starttag(self,tag,attrs):
  v=dict(attrs)
  if tag=='div' and v.get('id')=='app':self.mount=True
  if tag=='script' and v.get('type')=='module' and v.get('src'):self.urls.append(v['src'])
  if tag=='link' and v.get('rel') in ['stylesheet','modulepreload'] and v.get('href'):self.urls.append(v['href'])
def check_frontend(folder,port):
 code,body,headers=get(port,'/');p=AssetsParser();p.feed(body.decode('utf-8'))
 entries=[]
 for path in p.urls:
  parsed=urlsplit(path)
  if parsed.scheme or parsed.netloc:continue
  rel=parsed.path.lstrip('/');file=(folder/rel).resolve()
  if not file.is_relative_to(folder.resolve()) or not file.is_file():raise RuntimeError('Recurso compilado local ausente.')
  status,data,h=get(port,parsed.path)
  entries.append({'archivo':rel,'http':status,'bytes':len(data),'coincide_con_archivo':hashlib.sha256(data).hexdigest()==sha(file)})
 return {'html_200':code==200,'contenedor_app':p.mount,'servidor_PA01':headers.get('X-Prueba')=='PA01','recursos':entries,'correcto':code==200 and p.mount and bool(entries) and all(x['http']==200 and x['coincide_con_archivo'] for x in entries)}
def protected_files():
 files=[]
 for rel in ['backend-sga/src','app-sga/src','app-b2b/src','app-landing/src','app-sga/dist','app-b2b/dist','docs/pruebas/mantenimiento','scripts/mantenimiento','db','uploads']:
  files.extend(p for p in (PROJECT/rel).rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix not in ['.log','.lock'])
 for rel in ['backend-sga/pom.xml','app-sga/package.json','app-sga/package-lock.json','app-b2b/package.json','app-b2b/package-lock.json']:
  files.append(PROJECT/rel)
 files.extend([DEPLOY/'current/app.jar',DEPLOY/'estado.json',DEPLOY/'config/backend-seguro.json',DEPLOY/'seguridad/backend.dpapi',(ruta_herramientas(PROJECT)/'winsw/TransAduanasBackend.xml')])
 return {str(p):sha(p) for p in sorted(set(files)) if p.is_file()}
try:
 q=str(PRIVATE).replace("'","''")
 assert ps("$p='"+q+"';$sid=[Security.Principal.WindowsIdentity]::GetCurrent().User;$acl=[Security.AccessControl.DirectorySecurity]::new();$acl.SetOwner($sid);$acl.SetAccessRuleProtection($true,$false);foreach($s in @($sid,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'),[Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))){$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($s,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))};[IO.Directory]::SetAccessControl($p,$acl);[IO.Directory]::GetAccessControl($p).AreAccessRulesProtected|ConvertTo-Json")
 for port in PORTS:
  if listening(port):raise RuntimeError(f'Puerto {port} ocupado. No se detiene el proceso existente.')
 for path in [JAVA,MYSQL,DEPLOY/'current/app.jar',HERE/'servidor_frontend.py',HERE/'seguridad_local.py',PROJECT/'app-sga/dist/index.html',PROJECT/'app-b2b/dist/index.html']:
  if not path.is_file():raise RuntimeError('Falta un recurso necesario: '+str(path))
 source_before=protected_files();service_before=services();write('servicios_antes.json',service_before)
 sys.path.insert(0,str(HERE));from seguridad_local import dpapi
 values=json.loads(dpapi((DEPLOY/'seguridad/backend.dpapi').read_bytes(),False).decode('utf-8'))
 url=values.get('SPRING_DATASOURCE_URL',values.get('DB_URL',''));parsed=urlsplit(url.removeprefix('jdbc:'));DB=parsed.path.strip('/')
 assert parsed.hostname in ['localhost','127.0.0.1'] and parsed.port==3306 and re.fullmatch(r'[A-Za-z0-9_]+',DB),'Destino SQL inesperado.'
 user=values.get('SPRING_DATASOURCE_USERNAME',values.get('DB_USERNAME'));password=values.get('SPRING_DATASOURCE_PASSWORD',values.get('DB_PASSWORD'))
 assert user and user.lower()!='root' and password,'Se requiere cuenta SQL propia con credencial protegida.'
 client.write_text('[client]\nhost=127.0.0.1\nport=3306\nuser="'+user+'"\npassword="'+password.replace('\\','\\\\').replace('"','\\"')+'"\n',encoding='utf-8')
 tables=query('SHOW TABLES;').decode('utf-8').splitlines();assert tables and all(re.fullmatch(r'[A-Za-z0-9_]+',t) for t in tables)
 required={'cliente','cotizacion','expediente','documento_adjunto','catalogo_tipo_carga','catalogo_puerto'};assert required<=set(tables),'Tablas de negocio requeridas ausentes.'
 tls=query("SHOW STATUS LIKE 'Ssl_cipher';").decode('utf-8').strip().split('\t');flags['C02']=len(tls)==2 and bool(tls[1])
 print('PA01: consultando tablas reales mediante transacciones de lectura.',flush=True)
 db_before=database_snapshot();counts={k:v['registros'] for k,v in db_before.items()};flags['C03']=required<=counts.keys();write('recuentos_SQL.json',{'base':DB,'tablas':len(counts),'registros':sum(counts.values()),'recuentos':counts,'TLS':flags['C02'],'usuario_root':False,'modo':'START TRANSACTION READ ONLY; SELECT; ROLLBACK'})
 app=PRIVATE/'backend';app.mkdir();(PRIVATE/'documentos').mkdir();shutil.copy2(DEPLOY/'current/app.jar',app/'app.jar');jar_sha=sha(app/'app.jar');assert jar_sha==sha(DEPLOY/'current/app.jar')
 env={k:v for k,v in os.environ.items() if not k.startswith(('SPRING_','DB_','JPA_','JWT_','STORAGE_','SERVER_','MONITORING_')) and k not in ['PSModulePath','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','JAVA_OPTS','LOG_PATH']}
 env.update(DB_URL=url,SPRING_DATASOURCE_URL=url,DB_USERNAME=user,SPRING_DATASOURCE_USERNAME=user,DB_PASSWORD=password,SPRING_DATASOURCE_PASSWORD=password,JWT_SECRET=base64.urlsafe_b64encode(secrets.token_bytes(48)).decode(),STORAGE_DIR=str(PRIVATE/'documentos'),LOG_PATH=str(PRIVATE/'logs'),CORS_ALLOWED_ORIGINS='http://localhost:5180,http://localhost:5174,http://127.0.0.1:5180,http://127.0.0.1:5174')
 args=[str(JAVA),'-Xms64m','-Xmx256m','-DLOG_PATH='+str(PRIVATE/'logs'),'-jar',str(app/'app.jar'),'--spring.profiles.active=staging','--spring.config.location=classpath:/application.properties,classpath:/application-staging.properties','--spring.config.import=','--server.address=127.0.0.1','--server.port=8085','--management.server.address=127.0.0.1','--management.server.port=19085','--spring.jpa.hibernate.ddl-auto=validate','--app.data-initializer.enabled=false','--spring.datasource.hikari.read-only=true','--spring.jpa.show-sql=false','--app.storage.upload-dir='+str(PRIVATE/'documentos')]
 print('PA01: iniciando solo la copia temporal del backend en 8085.',flush=True)
 java=start(args,'backend',env,app);health=wait_for(java,8085,'La copia del backend');flags['C01']=health['data']['status']=='UP';write('salud_backend.json',health)
 catalog={}
 for name,table in [('tipos-carga','catalogo_tipo_carga'),('puertos','catalogo_puerto')]:
  status,result=get_json(8085,'/api/v1/catalogos/'+name);assert status==200 and result.get('success') and isinstance(result.get('data'),list)
  catalog[name]=result['data'];details[name]={'SQL':counts[table],'API':len(result['data'])}
 flags['C04']=all(v['SQL']==v['API'] for v in details.values());write('catalogos_verificados.json',details)
 for name,folder,port,check_id,proxy_id in [('SGA',PROJECT/'app-sga/dist',5180,'C05','C07'),('B2B',PROJECT/'app-b2b/dist',5174,'C06','C08')]:
  print('PA01: comprobando '+name+', HTML, recursos y ruta API.',flush=True)
  proc=start([sys.executable,str(HERE/'servidor_frontend.py'),'--carpeta',str(folder),'--puerto',str(port),'--backend','http://127.0.0.1:8085'],name,os.environ.copy(),PRIVATE)
  wait_for(proc,port,name);assets[name]=check_frontend(folder,port);flags[check_id]=assets[name]['correcto'];proxy_ok=True
  for key in catalog:
   status,payload=get_json(port,'/api/v1/catalogos/'+key);proxy_ok=proxy_ok and status==200 and payload.get('success') is True and payload.get('data')==catalog[key]
  flags[proxy_id]=proxy_ok
 write('frontends_verificados.json',assets)
 # Las conexiones del proceso Java deben ir al MySQL real identificado, no a otra instancia.
 conn=ps(f"@(Get-NetTCPConnection -OwningProcess {java.pid} -State Established -ErrorAction SilentlyContinue | Where-Object RemotePort -eq 3306 | Select-Object RemoteAddress,RemotePort,OwningProcess)|ConvertTo-Json -Compress")
 assert conn,'No se verifico la conexion del backend temporal a MySQL.';write('conexion_backend_SQL.json',conn)
 text=(PRIVATE/'backend.log').read_text(encoding='utf-8',errors='replace');flags['C09']='Started SgaApplication' in text and all(p.poll() is None for _,p in procs) and bool(api_results)
 print('PA01: comprobando integridad de datos y cerrando los procesos propios.',flush=True)
 db_after=database_snapshot();flags['C10']=db_before==db_after
except Exception as exc:
 error=type(exc).__name__+': '+str(exc)
 # Solo se guardan diagnosticos del ejecutor; las salidas Java/MySQL permanecen privadas.
 write('diagnostico.json',{'error':error,'cambios_forzados':False,'nota':'No se detienen procesos ajenos ni se corrigen resultados para que cumplan.'})
finally:
 if db_before is not None and db_after is None and client.exists():
  try:db_after=database_snapshot();flags['C10']=db_before==db_after
  except Exception:cleanup_errors.append('No se completo la verificacion SQL final.')
 for name,proc in reversed(procs):
  try:
   if proc.poll() is None:proc.terminate();proc.wait(timeout=15)
  except Exception:cleanup_errors.append('No se confirmo el cierre del proceso propio '+name)
 for f in handles:f.close()
 if client.exists():client.unlink()
 try:
  if source_before:source_after={p:sha(Path(p)) for p in source_before}
  if service_before is not None:service_after=services();write('servicios_despues.json',service_after)
  flags['C11']=bool(source_before) and source_before==source_after and service_before==service_after
 except Exception:cleanup_errors.append('No se completo el control de archivos y servicios originales.')
 flags['C12']=not cleanup_errors and all(p.poll() is not None for _,p in procs) and not client.exists() and all(not listening(p) for p in PORTS)
write('integridad_datos.json',{'tablas_antes':db_before,'tablas_despues':db_after,'contenido_sin_cambios':flags['C10'],'datos_personales_incluidos':False})
write('integridad_archivos.json',{'archivos_revisados':len(source_before),'sin_cambios':flags['C11'],'PM01_PM15_modificadas':False if flags['C11'] else 'No confirmado','SHA256':source_before})
write('consultas_HTTP.json',api_results)
criteria=[{'codigo':key,'verificacion':label,'cumple':flags[key]} for key,label in zip(flags,labels)]
with (OUT/'criterios.csv').open('w',encoding='utf-8-sig',newline='') as f:
 w=csv.DictWriter(f,fieldnames=['codigo','verificacion','cumple']);w.writeheader();w.writerows(criteria)
ok=all(flags.values()) and not error
original_state=next((s['State'] for s in (service_before or []) if s['Name']=='TransAduanasBackend'),'No verificado')
result={'prueba':'PA01','nombre':'Disponibilidad y conectividad del sistema','estado':'Exitoso' if ok else 'Fallo','fecha':datetime.now(timezone(timedelta(hours=-5))).isoformat(timespec='seconds'),'entorno':'Copia temporal del JAR y frontends compilados existentes. MySQL original consultado solo mediante lectura. No es disponibilidad del servicio Windows original.','criterios_cumplidos':sum(flags.values()),'criterios_totales':12,'criterios':criteria,'puertos':{'backend_temporal':8085,'SGA_temporal':5180,'B2B_temporal':5174,'MySQL_original':3306},'base':counts,'catalogos':details,'recursos_frontends':{k:len(v['recursos']) for k,v in assets.items()},'backend_original_antes':original_state,'servicios_originales_sin_cambios':service_before==service_after,'datos_sin_cambios':flags['C10'],'archivos_sin_cambios':flags['C11'],'procesos_propios_cerrados':flags['C12'],'error':error,'errores_limpieza':cleanup_errors,'herramientas':['Python 3','Java 21 y JAR actual conservado','MySQL 8.4 con TLS y transacciones de lectura','DPAPI y NTFS','HTTP GET, SHA256 y PowerShell/CIM'],'limites':['No ejecuta npm, Maven ni pruebas de regresion. Usa los dist existentes y verifica su entrega y el proxy local.','No valida inicio de sesion, interaccion visual ni todos los flujos de escritura.','El proxy temporal solo permite GET/HEAD a salud y dos catalogos publicos. No se cambia el proxy del proyecto.','El backend Windows conserva su estado inicial. Una prueba de laboratorio exitosa no demuestra que ese servicio estuviera activo.']}
write('resultado.json',result)
lines=['Prueba adicional 1 - Disponibilidad y conectividad','Entorno: copia temporal del backend y frontends compilados','Backend temporal 8085: '+('HTTP 200 y UP' if flags['C01'] else 'No verificado'),f'MySQL: {len(counts)} tablas y {sum(counts.values())} registros consultados; TLS',f'Catalogos: tipos de carga {details.get("tipos-carga",{}).get("API","-")}; puertos {details.get("puertos",{}).get("API","-")}',f'SGA 5180: HTML y {len(assets.get("SGA",{}).get("recursos",[]))} recursos; API '+('CORRECTA' if flags['C05'] and flags['C07'] else 'REVISAR'),f'B2B 5174: HTML y {len(assets.get("B2B",{}).get("recursos",[]))} recursos; API '+('CORRECTA' if flags['C06'] and flags['C08'] else 'REVISAR'),'Datos originales sin cambios: '+('SI' if flags['C10'] else 'NO CONFIRMADO'),'PM01-PM15 y archivos originales sin cambios: '+('SI' if flags['C11'] else 'NO CONFIRMADO'),'Backend Windows original: '+original_state+'; estado conservado','Procesos temporales detenidos: '+('SI' if flags['C12'] else 'REVISAR'),f'Verificaciones: {sum(flags.values())}/12','Estado: '+('EXITOSO' if ok else 'FALLO')]
(OUT/'resultado.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8-sig');print('\n'.join(lines),flush=True)
if error:print('Diagnostico: '+error)
print('Evidencia: '+str(OUT),flush=True);sys.exit(0 if ok else 1)
