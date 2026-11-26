"""Rotación y retención representativa con Logback real, solo en laboratorio."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
from collections import Counter
import csv,gzip,hashlib,json,os,re,shutil,subprocess,time,zipfile
import xml.etree.ElementTree as ET
project=Path(__file__).resolve().parents[3];base=project
tz=timezone(timedelta(hours=-5));started=datetime.now(tz);watch=time.perf_counter()
out=project/'evidencias-mantenimiento/Evidencia_Prueba_6';number=2
while out.exists():out=project/'evidencias-mantenimiento'/f'Evidencia_Prueba_6_{number}';number+=1
out.mkdir(parents=True,exist_ok=False)
for d in ['lib','classes','logs-laboratorio/archived']:(out/d).mkdir(parents=True,exist_ok=True)
logs=out/'logs-laboratorio';marker=out.name
def hfile(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda:f.read(1048576),b''):h.update(b)
    return h.hexdigest()
def csvwrite(p,rows):
    with p.open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
protected=[]
for folder in ['backend-sga/src','app-sga/src','app-b2b/src','app-landing/src','db','uploads','.github','monitoreo','scripts']:
    protected.extend(p for p in (project/folder).rglob('*') if p.is_file() and p.suffix.lower() not in ['.log','.lock'])
for folder in ['backend-sga','app-sga','app-b2b','app-landing']:
    protected.extend(p for p in (project/folder).glob('*') if p.is_file() and p.suffix!='.md')
before={p.relative_to(project).as_posix():hfile(p) for p in sorted(set(protected))}
ns={'m':'http://maven.apache.org/POM/4.0.0'}
version=ET.parse(project/'backend-sga/pom.xml').getroot().find('m:version',ns).text
jar=(ruta_despliegue(raiz_proyecto(__file__))/'current/app.jar');jarhash=hfile(jar)
source=(project/'backend-sga/src/main/resources/logback-spring.xml').read_bytes()
with zipfile.ZipFile(jar) as z:
    embedded=z.read('BOOT-INF/classes/logback-spring.xml')
    libs=[n for n in z.namelist() if n.startswith('BOOT-INF/lib/') and any(Path(n).name.startswith(x) for x in ['logback-classic-','logback-core-','slf4j-api-'])]
    assert len(libs)==3
    for n in libs:(out/'lib'/Path(n).name).write_bytes(z.read(n))
same=embedded.replace(b'\r\n',b'\n')==source.replace(b'\r\n',b'\n')
assert same,'La configuración del JAR y fuente difiere; revisar antes de atribuir resultados a la misma versión.'
(out/'logback-original.xml').write_bytes(embedded)
tree=ET.ElementTree(ET.fromstring(embedded));cfg=tree.getroot();cfg.set('scan','false')
for p in cfg.findall('property'):
    if p.get('name')=='LOG_PATH':p.set('value',logs.as_posix())
for p in cfg.findall('.//maxFileSize'):p.text='64KB'
for p in cfg.findall('root'):
    for ref in list(p.findall('appender-ref')):
        if ref.get('ref')=='CONSOLE':p.remove(ref)
config=out/'logback-laboratorio.xml';tree.write(config,encoding='utf-8',xml_declaration=True)
policy=[]
for a in cfg.findall('appender'):
    rp=a.find('rollingPolicy')
    if rp is not None:policy.append({'appender':a.get('name'),'maxHistory':rp.findtext('maxHistory'),'totalSizeCap':rp.findtext('totalSizeCap'),'maxFileSize_laboratorio':rp.findtext('maxFileSize')})
assert {p['appender']:p['maxHistory'] for p in policy}=={'FILE_ALL':'30','FILE_ERROR':'60'}
fixtures=[]
# Fechas alejadas del límite y dentro de la ventana de barrido inicial de cada política.
for stream,days,keep in [('general',10,True),('general',35,False),('errores',10,True),('errores',35,True),('errores',65,False)]:
    date=(started-timedelta(days=days)).strftime('%Y-%m-%d')
    stem='sga-backend' if stream=='general' else 'sga-backend-error'
    p=logs/'archived'/f'{stem}-{date}.0.log.gz'
    with gzip.open(p,'wt',encoding='utf-8') as f:f.write(f'ARCHIVO_SINTETICO_RETENCION_PM06 stream={stream} dias={days}\n')
    ts=(started-timedelta(days=days)).timestamp();os.utime(p,(ts,ts))
    fixtures.append({'archivo':p.relative_to(out).as_posix(),'flujo':stream,'antiguedad_dias':days,'esperado':'Conservar' if keep else 'Eliminar','sha256_antes':hfile(p)})
csvwrite(out/'retencion_antes.csv',fixtures)
(out/'criterios_previos.txt').write_text('Configuración fuente y JAR iguales; 3000 eventos INFO/WARN sintéticos numerados; recuperar 3000 eventos generales y 1500 WARN, sin pérdidas ni duplicados por flujo; ambos flujos deben rotar a gzip legible, conservar primer evento archivado y último activo; los controles generales de 10/35 días deben conservarse/eliminarse, los de errores 10/35/65 deben conservarse/conservarse/eliminarse; cero errores internos de Logback; archivos originales sin cambios. Límite de tamaño reducido solo en laboratorio; no se prueba el límite productivo de 50 MB ni el transcurso real de 60 días. Los WARN son mensajes de control, no incidentes de aplicación.',encoding='utf-8')
(out/'adaptaciones_laboratorio.txt').write_text('LOG_PATH aislado; maxFileSize 50MB -> 64KB; scan=false; consola desactivada; zona de la JVM de laboratorio America/Lima. Se mantienen los appenders, filtros, maxHistory 30/60, totalSizeCap 2GB/1GB y cleanHistoryOnStart original. No se cambia el reloj del equipo.',encoding='utf-8')
jdk=(java_home()/'bin');java=str(jdk/'java.exe');javac=str(jdk/'javac.exe');assert Path(java).is_file() and Path(javac).is_file(),'Se requiere JDK 21 local.'
probe=project/'docs/pruebas/mantenimiento/PM06/RotationMaintenanceProbe.java';shutil.copy2(probe,out/probe.name)
cp=os.pathsep.join([str(out/'classes'),str(out/'lib'/'*')])
compilecmd=[javac,'-encoding','UTF-8','-cp',cp,'-d',str(out/'classes'),str(out/probe.name)]
compiled=subprocess.run(compilecmd,cwd=out,capture_output=True,timeout=30)
(out/'compilacion.txt').write_bytes(compiled.stdout+compiled.stderr)
assert compiled.returncode==0,'Error del ejecutor: no atribuirlo al proyecto.'
cmd=[java,'-Duser.timezone=America/Lima','-Xms32m','-Xmx96m','-cp',cp,'RotationMaintenanceProbe',str(config),marker,'3000']
r=subprocess.run(cmd,cwd=out,capture_output=True,timeout=45)
(out/'ejecucion_java.txt').write_bytes(r.stdout+r.stderr)
(out/'comandos.json').write_text(json.dumps({'compilacion':compilecmd,'ejecucion':cmd},ensure_ascii=False,indent=2),encoding='utf-8')
counts={'general':Counter(),'errores':Counter()};locations={'general':{},'errores':{}};inventory=[];malformed=[]
for p in sorted(logs.rglob('*.log'))+sorted(logs.rglob('*.log.gz')):
    stream='errores' if p.name.startswith('sga-backend-error') else 'general'
    text=gzip.open(p,'rt',encoding='utf-8').read() if p.suffix=='.gz' else p.read_text(encoding='utf-8')
    found=[]
    for line in text.splitlines():
        if f'maintenance_test={marker} ' not in line:continue
        m=re.search(r'\bseq=(\d+) kind=(\S+) payload=(X+)\s*$',line)
        if not m or len(m.group(3))!=384:malformed.append(p.name);continue
        n=int(m.group(1));counts[stream][n]+=1;found.append(n)
        locations[stream].setdefault(n,[]).append(p.relative_to(out).as_posix())
    inventory.append({'archivo':p.relative_to(out).as_posix(),'flujo':stream,'bytes':p.stat().st_size,'gzip':p.suffix=='.gz','eventos_PM06':len(found),'primero':min(found) if found else '', 'ultimo':max(found) if found else ''})
csvwrite(out/'inventario_logs.csv',inventory)
retention=[]
for f in fixtures:
    p=out/f['archivo'];exists=p.exists();current=hfile(p) if exists else ''
    ok=(exists and current==f['sha256_antes']) if f['esperado']=='Conservar' else not exists
    retention.append({**f,'existe_despues':exists,'sha256_despues':current,'resultado':'Cumple' if ok else 'No cumple'})
csvwrite(out/'retencion_despues.csv',retention)
stats={}
sequences=[]
for stream,expected in [('general',set(range(1,3001))),('errores',set(range(2,3001,2)))]:
    counter=counts[stream]
    stats[stream]={'esperados':len(expected),'recuperados':sum(counter.values()),'unicos':len(counter),'perdidos':sorted(expected-set(counter)),'duplicados':{n:c for n,c in counter.items() if c>1},'extras':sorted(set(counter)-expected),'gzip_generados':sum(f['gzip'] and f['flujo']==stream and f['eventos_PM06']>0 for f in inventory),'primero_archivado':any(p.endswith('.gz') for p in locations[stream].get(min(expected),[])),'ultimo_activo':any(p.endswith('sga-backend'+('-error' if stream=='errores' else '')+'.log') for p in locations[stream].get(max(expected),[]))}
    for n in sorted(expected):sequences.append({'flujo':stream,'secuencia':n,'apariciones':counter[n],'archivos':'; '.join(locations[stream].get(n,[]))})
csvwrite(out/'secuencias_verificadas.csv',sequences)
after={rel:hfile(project/rel) for rel in before};unchanged=before==after and hfile(jar)==jarhash
csvwrite(out/'integridad_archivos_operativos.csv',[{'archivo':rel,'sha256_antes':before[rel],'sha256_despues':after[rel],'estado':'Sin cambios' if before[rel]==after[rel] else 'Cambió'} for rel in before])
criteria=[
('C01','Configuración del JAR coincide con la fuente y mantiene políticas diferenciadas',same and {p['appender']:p['maxHistory'] for p in policy}=={'FILE_ALL':'30','FILE_ERROR':'60'}),
('C02','Ejecución Java con cero errores internos de Logback',r.returncode==0 and b'errores_internos_Logback=0' in r.stdout),
('C03','Rotación y compresión legibles en ambos flujos',all(s['gzip_generados']>0 for s in stats.values()) and not malformed),
('C04','Continuidad exacta y sin duplicados por flujo',all(not s['perdidos'] and not s['duplicados'] and not s['extras'] and s['recuperados']==s['esperados'] for s in stats.values())),
('C05','Primer evento archivado y último en activo para ambos flujos',all(s['primero_archivado'] and s['ultimo_activo'] for s in stats.values())),
('C06','Retención diferenciada sobre cinco archivos históricos sintéticos',all(f['resultado']=='Cumple' for f in retention)),
('C07','Fuentes, configuración, JAR y datos originales sin cambios',unchanged),
]
csvwrite(out/'criterios_PM06.csv',[{'criterio':c,'verificacion':d,'resultado':'Cumple' if ok else 'No cumple'} for c,d,ok in criteria])
ok=all(x for _,_,x in criteria);finished=datetime.now(tz)
result={'prueba':'PM06 Rotación de logs','fecha_lima':finished.isoformat(timespec='seconds'),'estado':'Exitoso' if ok else 'Falló','proyecto':str(project),'version_backend':version,'herramientas':['Java 21 y javac','Logback y SLF4J extraídos del JAR del proyecto','Python','gzip','hashlib SHA256'],'dependencias':[Path(n).name for n in libs],'sha256_JAR':jarhash,'estadisticas':stats,'retencion_controles_cumplidos':sum(f['resultado']=='Cumple' for f in retention),'criterios_cumplidos':sum(x for _,_,x in criteria),'archivos_operativos_sin_cambios':len(before) if unchanged else None,'duracion_total_segundos':round(time.perf_counter()-watch,6),'fallos_provocados':False,'alcance':'Límite de laboratorio 64KB, INFO y WARN sintéticos sin fallo de aplicación; retención 30/60 sobre archivos con fechas conocidas y limpieza propia de Logback al rotar. No se acredita límite productivo 50MB, topes 2GB/1GB, transcurso real de días, reinicio del servicio ni medianoche real. La JVM aislada usa America/Lima; no se cambió el reloj del equipo. Solo se escribieron archivos bajo el laboratorio de evidencias.'}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')

lines=['Prueba 6 - Rotacion de logs','','Limite de laboratorio: 64 KB','',f'{"Registro":<18}{"Esperados":>10}{"Leidos":>10}{"Perdidos":>10}{"Duplicados":>12}']
for stream,label in [('general','General'),('errores','Avisos (WARN)')]:
    values=stats[stream]
    lines.append(f'{label:<18}{values["esperados"]:>10}{values["recuperados"]:>10}{len(values["perdidos"]):>10}{len(values["duplicados"]):>12}')
lines+=['',f'Archivos comprimidos: {stats["general"]["gzip_generados"]} generales y {stats["errores"]["gzip_generados"]} de avisos','Lectura de comprimidos: '+('CORRECTA' if not malformed else 'REVISAR'),'Continuidad del registro: '+('CORRECTA' if all(v['primero_archivado'] and v['ultimo_activo'] for v in stats.values()) else 'REVISAR'),'','Retencion de archivos de prueba:',f'{"Politica":<21}{"Antiguedad":>12}  Resultado']
for item in retention:
    policy='General (30 dias)' if item['flujo']=='general' else 'Avisos (60 dias)'
    action='Conservado' if item['existe_despues'] else 'Eliminado'
    lines.append(f'{policy:<21}{str(item["antiguedad_dias"])+" dias":>12}  {action}')
lines+=['',f'Controles de retencion: {result["retencion_controles_cumplidos"]}/5',f'Errores internos de Logback: {0 if r.returncode==0 and b"errores_internos_Logback=0" in r.stdout else "REVISAR"}',f'Criterios cumplidos: {result["criterios_cumplidos"]}/7','Estado: '+('EXITOSO' if ok else 'FALLO')]
text='\n'.join(lines);(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if ok else 1)
