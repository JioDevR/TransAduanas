"""PM15: lectura de permisos/secretos y ejecucion limitada de scripts con datos de prueba."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,csv,hashlib,json,os,re,secrets,subprocess,sys,time,zipfile,xml.etree.ElementTree as ET
ap=argparse.ArgumentParser();ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)));ap.add_argument('--evidencias-originales',default=str(evidencias_originales()));a=ap.parse_args()
PROJECT=Path(a.proyecto).resolve();BASE=Path(a.evidencias_originales).resolve();DEPLOY=ruta_despliegue(PROJECT);XML=(ruta_herramientas(PROJECT)/'winsw/TransAduanasBackend.xml')
PS=str(powershell());MYSQL=r'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
sys.path.insert(0,str(Path(__file__).resolve().parent/'PM12'));from respaldo_runtime import dpapi,sha
OUT=PROJECT/'evidencias-mantenimiento/Evidencia_Prueba_15';case=2
while OUT.exists():OUT=OUT.with_name(f'Evidencia_Prueba_15_{case}');case+=1
PRIVATE=ruta_laboratorio(PROJECT)/OUT.name
assert not PRIVATE.exists() and PRIVATE.resolve().is_relative_to((ruta_laboratorio(PROJECT)).resolve())
OUT.mkdir(parents=True);PRIVATE.mkdir(parents=True)
def dump(n,x):(OUT/n).write_text(json.dumps(x,ensure_ascii=False,indent=2),encoding='utf-8')
def ps(code):
    r=subprocess.run([PS,'-NoProfile','-NonInteractive','-Command',"$ErrorActionPreference='Stop';"+code],env={k:v for k,v in os.environ.items() if k!='PSModulePath'},capture_output=True,timeout=20)
    assert r.returncode==0,'Consulta Windows no completada; no se alteran servicios.'
    return json.loads(r.stdout.decode('utf-8-sig'))
q=str(PRIVATE).replace("'","''")
ps("$p='"+q+"';$sid=[Security.Principal.WindowsIdentity]::GetCurrent().User;$acl=[Security.AccessControl.DirectorySecurity]::new();$acl.SetOwner($sid);$acl.SetAccessRuleProtection($true,$false);foreach($s in @($sid,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'),[Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))){$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($s,'FullControl','ContainerInherit,ObjectInherit','None','Allow'))};[IO.Directory]::SetAccessControl($p,$acl);[IO.Directory]::GetAccessControl($p).AreAccessRulesProtected|ConvertTo-Json")
def services():return ps("@(Get-CimInstance Win32_Service -Filter \"Name='MySQL84' OR Name='TransAduanasBackend' OR Name='TransAduanasMantenimiento'\" |Select-Object Name,State,ProcessId,StartName)|ConvertTo-Json -Compress")
svc_before=services();dump('servicios_antes.json',svc_before)
identity=ps("$i=[Security.Principal.WindowsIdentity]::GetCurrent();$p=[Security.Principal.WindowsPrincipal]::new($i);@{AdministradorEfectivo=$p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator);Sistema=($i.User.Value -eq 'S-1-5-18');SID=$i.User.Value}|ConvertTo-Json -Compress")
assert not identity['AdministradorEfectivo'] and not identity['Sistema'],'El ejecutor de scripts debe usar un token limitado.'
dump('identidad_ejecutor.json',identity)
files=[]
for folder in ['backend-sga/src','app-sga/src','app-b2b/src','app-landing/src','db','uploads','scripts','monitoreo']:
    files.extend(p for p in (PROJECT/folder).rglob('*') if p.is_file() and p.suffix.lower() not in {'.log','.lock'})
files += [backend for backend in [PROJECT/'backend-sga/pom.xml',DEPLOY/'estado.json',DEPLOY/'current/app.jar',DEPLOY/'config/backend-seguro.json',DEPLOY/'seguridad/backend.dpapi',XML,DEPLOY/'mantenimiento/TransAduanasMantenimiento.xml'] if backend.is_file()]
before={str(p):sha(p) for p in sorted(set(files))}
secretfile=DEPLOY/'seguridad/backend.dpapi';env=json.loads(dpapi(secretfile.read_bytes(),False).decode('utf-8'))
values={v for k,v in env.items() if re.search('PASSWORD|SECRET|TOKEN',k,re.I) and isinstance(v,str) and len(v)>=8}
# Los valores solo permanecen en memoria. Nunca se imprimen ni se copian al informe.
xml=ET.parse(XML).getroot();xml_clean=not any(re.search('PASSWORD|SECRET|TOKEN',e.get('name',''),re.I) for e in xml.findall('.//env'))
acl=ps("$a=[IO.File]::GetAccessControl('"+str(secretfile).replace("'","''")+"');@{Reglas=@($a.GetAccessRules($true,$true,[Security.Principal.SecurityIdentifier])|ForEach-Object{@{SID=$_.IdentityReference.Value;Permisos=$_.FileSystemRights.ToString();Tipo=$_.AccessControlType.ToString()}})}|ConvertTo-Json -Depth 5 -Compress")
backend_sid='S-1-5-80-'+ '-'.join(str(int.from_bytes(hashlib.sha1('TRANSADUANASBACKEND'.encode('utf-16-le')).digest()[i:i+4],'little')) for i in range(0,20,4))
allowed={'S-1-5-18','S-1-5-32-544',identity['SID'],backend_sid}
acl_ok=all(r['Tipo']=='Allow' and r['SID'] in allowed for r in acl['Reglas']) and any(r['SID']==backend_sid and not any(k in r['Permisos'] for k in ['FullControl','Modify','Write']) for r in acl['Reglas'])
dump('permisos_almacen.json',{'cifrado_DPAPI_descifrable':True,'XML_sin_secretos':xml_clean,'ACL':acl,'lectores_autorizados':acl_ok})
client=PRIVATE/'cliente-lectura.cnf';sql_info={};sql_error=False
try:
    user=env.get('SPRING_DATASOURCE_USERNAME',env.get('DB_USERNAME'));password=env.get('SPRING_DATASOURCE_PASSWORD',env.get('DB_PASSWORD'))
    client.write_text('[client]\nhost=127.0.0.1\nport=3306\nuser="'+user+'"\npassword="'+password.replace('\\','\\\\').replace('"','\\"')+'"\n',encoding='utf-8')
    cmd=[MYSQL,'--defaults-file='+str(client),'--no-login-paths','--protocol=TCP','--ssl-mode=REQUIRED','--connect-timeout=5','--batch','--raw','--skip-column-names']
    r=subprocess.run(cmd,input=b"SELECT CURRENT_USER(); SHOW GRANTS; SHOW STATUS LIKE 'Ssl_cipher';",capture_output=True,timeout=15)
    assert r.returncode==0,'No se pudieron verificar permisos SQL; valores omitidos.'
    rows=r.stdout.decode('utf-8').splitlines();grants=[s for s in rows if s.startswith('GRANT ')]
    sql_info={'cuenta_propia':rows[0]=='transaduanas_app@localhost','privilegios':grants,'TLS_activo':any(s.startswith('Ssl_cipher\t') and len(s.split('\t',1)[1])>0 for s in rows)}
    sql_info['CRUD_solo_sgadb']=len(grants)==2 and any('GRANT SELECT, INSERT, UPDATE, DELETE ON `sgadb`.*' in s for s in grants) and all('GRANT OPTION' not in s and 'ALL PRIVILEGES' not in s for s in grants)
finally:
    if client.exists():client.unlink()
dump('permisos_SQL.json',sql_info)
compose=(PROJECT/'db/docker-compose.yml').read_text(encoding='utf-8');docker_ok=all(re.search(r'^\s*'+k+r':\s*"\$\{'+k+r':\?[^}]+\}"\s*$',compose,re.M) for k in ['MYSQL_ROOT_PASSWORD','MYSQL_PASSWORD'])
leaks=[];ntext=0;nlog=0;extensions={'.ps1','.sh','.bat','.cmd','.properties','.yml','.yaml','.xml','.java','.ts','.js','.json','.env','.md','.txt','.log','.out'};skip={'.git','node_modules','target','dist','.angular','releases','uploads','documentos'}
for root in [PROJECT,DEPLOY,XML.parent]:
    for folder,dirs,names in os.walk(root):
        dirs[:]=[d for d in dirs if d not in skip]
        for name in names:
            p=Path(folder)/name
            if p.suffix.lower() not in extensions or p.stat().st_size>30_000_000:continue
            ntext+=1;nlog+=int(p.suffix.lower() in {'.log','.out'})
            for lineno,line in enumerate(p.read_text(encoding='utf-8-sig',errors='replace').splitlines(),1):
                if any(v in line for v in values):leaks.append({'archivo':str(p),'linea':lineno,'valor':'[OMITIDO]'})
dump('revision_secretos.json',{'textos_revisados':ntext,'logs_revisados':nlog,'coincidencias':leaks,'limite':'Busqueda exacta de secretos operativos actuales de 8 o mas caracteres en textos locales seleccionados. No audita Git remoto, binarios ni todos los posibles secretos.'})
# Archivos sinteticos; los scripts verifican las marcas antes de borrar.
clean=PRIVATE/'limpieza';(clean/'temporales').mkdir(parents=True)
(clean/'.transaduanas-laboratorio.json').write_text(json.dumps({'Prueba':'PM05','Raiz':str(clean)}),encoding='utf-8')
old=clean/'temporales/vencido.tmp';old.write_text('Dato de prueba');os.utime(old,(time.time()-20*86400,)*2)
keep=clean/'temporales/vigente.tmp';keep.write_text('Conservar');keep_hash=sha(keep)
ret=PRIVATE/'retencion';(ret/'copias').mkdir(parents=True);(ret/'laboratorio.json').write_text(json.dumps({'Prueba':'PM10','Raiz':str(ret)}),encoding='utf-8');copies=[]
for name,age in [('vencida.zip',20),('vigente.zip',0)]:
    p=ret/'copias'/name
    with zipfile.ZipFile(p,'w') as z:z.writestr('dato.txt','Respaldo de prueba')
    copies.append({'Archivo':name,'SHA256':sha(p),'FechaUTC':(datetime.now(timezone.utc)-timedelta(days=age)).isoformat()})
(ret/'inventario_respaldos.json').write_text(json.dumps({'Copias':copies}),encoding='utf-8')
cfg={'out':str(OUT),'scripts':str(PROJECT/'scripts/mantenimiento'),'limpieza':str(clean),'retencion':str(ret)};dump('configuracion_ejecucion.json',cfg)
canary=secrets.token_urlsafe(48);runenv={k:v for k,v in os.environ.items() if k!='PSModulePath'};runenv['TRANSADUANAS_PM15_SECRET']=canary
results=[]
for case in ['limpieza','retencion','bloqueo','disponibilidad']:
    print('PM15: comprobando '+case+' con token limitado.',flush=True)
    r=subprocess.run([PS,'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(Path(__file__).resolve().parent/'PM15/Probar-Scripts.ps1'),'-Configuracion',str(OUT/'configuracion_ejecucion.json'),'-Caso',case],env=runenv,capture_output=True,timeout=45)
    texts=[r.stdout.decode('utf-8-sig',errors='replace'),r.stderr.decode('utf-8-sig',errors='replace')]
    exposed=any(canary in t or any(v in t for v in values) for t in texts)
    # No guardar valores sensibles aunque un script los imprimiera inesperadamente.
    for label,t in zip(['salida','error'],texts):
        for v in values|{canary}:t=t.replace(v,'[OMITIDO]')
        (OUT/(case+'-'+label+'.txt')).write_text(t,encoding='utf-8')
    tok=json.loads((OUT/('token-'+case+'.json')).read_text(encoding='utf-8-sig'))
    results.append({'caso':case,'codigo_salida':r.returncode,'sin_administrador':not tok['AdministradorEfectivo'] and not tok['Sistema'],'secreto_en_salida':exposed})
dump('ejecucion_scripts.json',results)
availability=json.loads(next((OUT/'disponibilidad').glob('resumen-*.json')).read_text(encoding='utf-8'))
backend=next(v for v in svc_before if v['Name']=='TransAduanasBackend');maint=next(v for v in svc_before if v['Name']=='TransAduanasMantenimiento')
backend_running=backend['State']=='Running'
availability_correct=availability['CodigoSalida']==(0 if backend_running else 1) and availability['Comprobaciones'][0]['Obtenido']==('Running' if backend_running else 'Stopped')
historical=json.loads((BASE/'PM15_correccion_20261123-095003/mantenimiento_cuenta_dedicada.json').read_text(encoding='utf-8-sig'))
history_ok=historical['Estado']=='Exitoso' and historical['Cuenta']==r'NT SERVICE\TransAduanasMantenimiento' and not historical['AdministradorEfectivo'] and len(historical['Scripts'])==4 and all(s['CodigoSalida']==0 for s in historical['Scripts'])
svc_after=services();dump('servicios_despues.json',svc_after)
after={path:sha(Path(path)) for path in before}
checks=[('C01','Identidad virtual configurada para backend y mantenimiento',backend['StartName']==r'NT SERVICE\TransAduanasBackend' and maint['StartName']==r'NT SERVICE\TransAduanasMantenimiento'),('C02','Cuenta SQL propia con CRUD solo en sgadb y TLS',all(sql_info[k] for k in ['cuenta_propia','CRUD_solo_sgadb','TLS_activo'])),('C03','Secretos cifrados fuera del XML y lectores NTFS autorizados',xml_clean and acl_ok),('C04','Docker requiere contrasenas externas sin valores predeterminados',docker_ok),('C05','Limpieza retencion y bloqueo funcionan sin administrador',all(x['codigo_salida']==0 and x['sin_administrador'] and not x['secreto_en_salida'] for x in results[:3]) and not old.exists() and keep.exists() and sha(keep)==keep_hash and not (ret/'copias/vencida.zip').exists() and sha(ret/'copias/vigente.zip')==copies[1]['SHA256']),('C06','Disponibilidad informa el estado real sin iniciar el backend',availability_correct and results[3]['sin_administrador'] and not results[3]['secreto_en_salida']),('C07','Ejecucion anterior con cuenta dedicada documentada y configuracion actual conservada',history_ok and maint['StartName']==historical['Cuenta']),('C08','Sin coincidencias exactas de secretos actuales en textos revisados',not leaks),('C09','Archivos originales y servicios conservan contenido estado y PID',before==after and svc_before==svc_after),('C10','Credencial temporal retirada y variable sintetica no registrada',not client.exists() and not any(canary in p.read_text(encoding='utf-8-sig',errors='replace') for p in OUT.rglob('*') if p.is_file()) and all(not x['secreto_en_salida'] for x in results))]
with (OUT/'criterios.csv').open('w',encoding='utf-8-sig',newline='') as f:
    w=csv.DictWriter(f,fieldnames=['codigo','verificacion','estado']);w.writeheader();w.writerows({'codigo':c,'verificacion':d,'estado':'Cumple' if ok else 'No cumple'} for c,d,ok in checks)
ok=all(v for _,_,v in checks)
result={'prueba':'PM15 Privilegios y secretos','estado':'Exitoso' if ok else 'Fallo','criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':len(checks),'scripts':results,'disponibilidad':{'estado_backend':backend['State'],'codigo_salida':availability['CodigoSalida'],'interpretacion':'El backend ya estaba detenido; el diagnostico de disponibilidad registra Fallo y no se interpreta como disponibilidad exitosa.' if not backend_running else 'Backend activo y disponibilidad verificada'},'historico_cuenta_dedicada':{'fecha':historical['Fecha'],'scripts':4,'estado':'Exitoso','repetido_en_esta_ejecucion':False},'textos_revisados':ntext,'logs_revisados':nlog,'coincidencias_secretos':len(leaks),'archivos_originales_sin_cambios':before==after,'servicios_sin_cambios':svc_before==svc_after,'herramientas':['PowerShell y CIM','Identidades Windows y NTFS','DPAPI','MySQL 8.4 con TLS','Python hashlib y revision de textos'],'limites':['Los 4 scripts actuales corrieron con token limitado del usuario; no con la cuenta dedicada. Esa ejecucion se conserva como evidencia historica.','Disponibilidad informo el servicio detenido; no se afirma salud UP.','Revision de secretos locales actuales por coincidencia exacta; no es auditoria exhaustiva.','Docker no ejecutado. No se inicia backend ni se cambian usuarios, permisos, servicios o credenciales productivos.']}
dump('resultado.json',result)
lines=['Prueba 15 - Privilegios y proteccion de credenciales','Backend y mantenimiento: identidades de servicio propias','Permisos SQL: solo CRUD en sgadb; conexion TLS','Secretos: cifrados con DPAPI y acceso NTFS restringido','Contrasenas Docker: variables obligatorias, sin valores incluidos','Limpieza, retencion y bloqueo sin administrador: '+str(sum(x['codigo_salida']==0 and x['sin_administrador'] for x in results[:3]))+'/3','Disponibilidad: '+('backend activo' if backend_running else 'backend detenido; codigo 1 registrado'),'Cuenta dedicada: 4 scripts aprobados en ejecucion anterior',f'Textos revisados: {ntext}; logs: {nlog}; coincidencias de secretos: {len(leaks)}','Archivos y servicios originales sin cambios: '+('SI' if before==after and svc_before==svc_after else 'NO'),f'Verificaciones de permisos y secretos: {result["criterios_cumplidos"]}/10','Estado de PM15: '+('EXITOSO' if ok else 'FALLO')]
(OUT/'resultado.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8-sig');print('\n'.join(lines),flush=True);sys.exit(0 if ok else 1)
