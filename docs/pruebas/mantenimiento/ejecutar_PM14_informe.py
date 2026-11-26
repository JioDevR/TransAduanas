"""Prueba 14: regresion de dependencias en dos copias con H2 y datos de prueba."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git,programa_maven
from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,csv,hashlib,json,os,re,shutil,subprocess,sys,time,urllib.request,zipfile,xml.etree.ElementTree as ET
ap=argparse.ArgumentParser()
ap.add_argument('--reanudar-preparacion',action='store_true')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
ap.add_argument('--evidencias-originales',default=str(evidencias_originales()))
a=ap.parse_args();PROJECT=Path(a.proyecto).resolve();BASE=Path(a.evidencias_originales).resolve();backend=PROJECT/'backend-sga'
OUT=PROJECT/'evidencias-mantenimiento/Evidencia_Prueba_14';case=2
if not a.reanudar_preparacion:
    while OUT.exists():OUT=OUT.with_name(f'Evidencia_Prueba_14_{case}');case+=1
LAB=ruta_laboratorio(PROJECT)/OUT.name
assert LAB.resolve().is_relative_to((ruta_laboratorio(PROJECT)).resolve())
if a.reanudar_preparacion:
    old=json.loads((OUT/'resultado.json').read_text(encoding='utf-8'))
    assert old['baseline']['tests']==0 and not old['parche']
    assert 'Cannot access central-pm14' in (OUT/'baseline-maven.log').read_text(encoding='utf-8',errors='replace')
else:
    assert not LAB.exists()
    OUT.mkdir(parents=True);LAB.mkdir(parents=True)
selected=['JwtTokenProviderTest','SecurityConfigTest','SystemApiTest','BusinessFunctionalCasesTest','PortalServiceImplTest','ExpedienteServiceImplTest','LocalStorageServiceTest','CotizacionPagoServiceImplTest','ReporteComercialPdfTest']
testrel='src/test/java/pe/sga/service/ReporteComercialPdfTest.java'
original_test=(backend/testrel).read_text(encoding='utf-8-sig')
modified=original_test.replace('void exportarReporteComercialPdf_generaPdfValidoConMetricas() {','void exportarReporteComercialPdf_generaPdfValidoConMetricas() throws Exception {')
anchor='        assertTrue("%PDF-".equals(header), "El archivo generado debe iniciar con cabecera %PDF-");'
assert anchor in modified
extra='\n        // Verificaciones adicionales PM14 solo en ambas copias de laboratorio.\n        java.nio.file.Path output = java.nio.file.Path.of("target", "pm14-reportes", "reporte-comercial.pdf");\n        java.nio.file.Files.createDirectories(output.getParent());\n        java.nio.file.Files.write(output, bytes);\n        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(bytes)) {\n            assertTrue(document.getNumberOfPages() > 0, "El reporte debe tener paginas legibles");\n            String content = new org.apache.pdfbox.text.PDFTextStripper().getText(document);\n            assertTrue(content.contains("Reporte Ejecutivo"), "Debe conservar el titulo del reporte");\n            assertTrue(content.contains("Gestión Comercial"), "Debe conservar texto en español");\n        }\n'
modified=modified.replace(anchor,anchor+extra)

original_pom=(backend/'pom.xml').read_text(encoding='utf-8-sig')
def pom_con_pdfbox(text,version):
    match=re.search(r'<dependencyManagement>.*?</dependencyManagement>',text,re.S)
    modules=''.join('<dependency><groupId>org.apache.pdfbox</groupId><artifactId>'+name+'</artifactId><version>'+version+'</version></dependency>' for name in ['pdfbox','fontbox','xmpbox'])
    if match:
        block=match.group(0)
        block=re.sub(r'<dependency>\s*<groupId>org\.apache\.pdfbox</groupId>.*?</dependency>','',block,flags=re.S)
        block=block.replace('</dependencies>',modules+'</dependencies>',1)
        return text[:match.start()]+block+text[match.end():]
    block='<dependencyManagement><dependencies>'+modules+'</dependencies></dependencyManagement>'
    return text.replace('<dependencies>',block+'<dependencies>',1)
baseline_pom=pom_con_pdfbox(original_pom,'2.0.24')
original_pom=pom_con_pdfbox(original_pom,'2.0.37')
for phase in ['baseline','parche']:
    dest=LAB/phase/'backend-sga'
    if a.reanudar_preparacion:
        assert (dest/'src').is_dir()
        for srcfile in (backend/'src').rglob('*'):
            if srcfile.is_file():
                rel=srcfile.relative_to(backend)
                assert (dest/rel).read_bytes()==srcfile.read_bytes() if rel.as_posix()!=testrel else (dest/rel).read_text(encoding='utf-8')==modified
    else:
        dest.mkdir(parents=True)
        shutil.copytree(backend/'src',dest/'src')
    (dest/'pom.xml').write_text(baseline_pom if phase=='baseline' else original_pom,encoding='utf-8')
    (dest/testrel).write_text(modified,encoding='utf-8')
    (dest/'target/tmp').mkdir(parents=True,exist_ok=a.reanudar_preparacion)
settings=LAB/'settings-laboratorio.xml'
settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"><interactiveMode>false</interactiveMode><mirrors><mirror><id>central</id><mirrorOf>*</mirrorOf><url>https://repo.maven.apache.org/maven2</url></mirror></mirrors></settings>',encoding='utf-8')
mavens=[Path(programa_maven())]
CFG={'proyecto':str(PROJECT),'salida':str(OUT),'laboratorio':str(LAB),'maven':str(mavens[0]),'java_home':str(java_home()),'settings':str(settings),'test_clases':selected,'baseline':str(LAB/'baseline/backend-sga'),'parche':str(LAB/'parche/backend-sga'),'modulos_pdfbox':['pdfbox','fontbox','xmpbox']}
(OUT/'configuracion_laboratorio.json').write_text(json.dumps(CFG,ensure_ascii=False,indent=2),encoding='utf-8')
PS=str(powershell())
def sha(p,algorithm='sha256'):
    h=hashlib.new(algorithm)
    with p.open('rb') as f:
        for b in iter(lambda:f.read(1048576),b''):h.update(b)
    return h.hexdigest()
def jsonwrite(name,data):(OUT/name).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
def csvread(p):
    with p.open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f))
def csvwrite(p,rows):
    with p.open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
def services():
    code="@(Get-CimInstance Win32_Service -Filter \"Name='MySQL84' OR Name='TransAduanasBackend'\" | Select-Object Name,State,ProcessId)|ConvertTo-Json -Compress"
    r=subprocess.run([PS,'-NoProfile','-Command',code],env={k:v for k,v in os.environ.items() if k!='PSModulePath'},capture_output=True,timeout=12);assert r.returncode==0
    return json.loads(r.stdout.decode('utf-8-sig'))
def health():
    with urllib.request.urlopen('http://127.0.0.1:8086/api/v1/health',timeout=8) as r:return json.load(r)['data']['status']
services_before=services();jsonwrite('servicios_antes.json',services_before)
original_running=next(v for v in services_before if v['Name']=='TransAduanasBackend')['State']=='Running'
health_before=health() if original_running else 'Detenido antes de la prueba'
assert health_before=='UP' if original_running else True
paths=[]
for folder in ['backend-sga/src','app-sga/src','app-b2b/src','app-landing/src','db','uploads','.github','monitoreo','scripts']:
    paths.extend(p for p in (PROJECT/folder).rglob('*') if p.is_file() and p.suffix.lower() not in ['.log','.lock'])
for folder in ['backend-sga','app-sga','app-b2b','app-landing']:
    paths.extend(p for p in (PROJECT/folder).glob('*') if p.is_file() and p.suffix!='.md')
csvwrite(OUT/'integridad_antes.csv',[{'archivo':p.relative_to(PROJECT).as_posix(),'sha256_antes':sha(p)} for p in sorted(set(paths))])
deploy=ruta_despliegue(raiz_proyecto(__file__))
deployed={rel:sha(deploy/rel) for rel in ['estado.json','current/app.jar']}
checksums=[]
print('PM14: verificando JAR conservados contra los SHA512 de Apache registrados.',flush=True)
prior=BASE/'PM14_20261122-230117'
if not (prior/'descargas_verificadas.csv').is_file():prior=Path(__file__).resolve().parent/'referencias/PM14'
for row in csvread(prior/'descargas_verificadas.csv'):
    jarname=row['modulo']+'-2.0.37.jar'
    candidate=prior/'descargas'/jarname
    if not candidate.is_file():candidate=Path.home()/'.m2/repository/org/apache/pdfbox'/row['modulo']/'2.0.37'/jarname
    actual=sha(candidate,'sha512')
    assert actual==row['sha512_apache']==row['sha512_descarga']
    row.update(sha512_reverificado=actual,verificado=True,origen='Descarga anterior conservada; SHA512 de Apache registrado')
    checksums.append(row)
csvwrite(OUT/'descargas_verificadas.csv',checksums)

def parse_reports(phase):
    folder=Path(CFG[phase])/'target/surefire-reports';suites=[];cases=[]
    for p in sorted(folder.glob('TEST-*.xml')):
        root=ET.parse(p).getroot()
        suites.append({'clase':root.get('name'),'pruebas':int(root.get('tests','0')),'fallos':int(root.get('failures','0')),'errores':int(root.get('errors','0')),'omitidas':int(root.get('skipped','0')),'segundos':root.get('time')})
        for case in root.findall('testcase'):
            state='Falló' if case.find('failure') is not None or case.find('error') is not None else 'Omitida' if case.find('skipped') is not None else 'Exitoso'
            cases.append({'fase':phase,'clase':case.get('classname'),'caso':case.get('name'),'estado':state,'segundos':case.get('time')})
    if suites:csvwrite(OUT/(phase+'-suites.csv'),suites)
    if cases:csvwrite(OUT/(phase+'-casos.csv'),cases)
    return {'suites':suites,'casos':cases,'tests':sum(x['pruebas'] for x in suites),'failures':sum(x['fallos'] for x in suites),'errors':sum(x['errores'] for x in suites),'skipped':sum(x['omitidas'] for x in suites)}
phases={};errors=[]
for phase in ['baseline','parche']:
    print('PM14: ejecutando Maven '+phase+' con H2 y archivos de laboratorio.',flush=True)
    cmd=[PS,'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(Path(__file__).resolve().parent/'PM14/Run-Maven.ps1'),'-Fase',phase,'-Configuracion',str(OUT/'configuracion_laboratorio.json')]
    start=time.perf_counter()
    with (OUT/(phase+'-maven.log')).open('wb') as log:
        r=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=900,creationflags=subprocess.CREATE_NO_WINDOW)
    summary=parse_reports(phase);summary.update(codigo_salida=r.returncode,duracion_total_segundos=round(time.perf_counter()-start,6))
    phases[phase]=summary;jsonwrite('avance_resultados.json',{k:{a:b for a,b in v.items() if a not in ['casos','suites']} for k,v in phases.items()})
    print(f'PM14: {phase}: pruebas={summary["tests"]}; fallos={summary["failures"]}; errores={summary["errors"]}; omitidas={summary["skipped"]}; codigo={r.returncode}.',flush=True)
    if r.returncode!=0 or not summary['tests']:
        errors.append('La fase '+phase+' no completó Maven correctamente; revisar log y distinguir problema del ejecutor de un defecto real.')
        break
packaged={};candidate_lib_hashes=[]
for phase in phases:
    jars=list((Path(CFG[phase])/'target').glob('sga-backend-*.jar'))
    if not jars:continue
    assert len(jars)==1
    with zipfile.ZipFile(jars[0]) as z:
        libs=sorted(Path(n).name for n in z.namelist() if n.startswith('BOOT-INF/lib/') and n.endswith('.jar'))
        packaged[phase]={'jar':str(jars[0]),'sha256':sha(jars[0]),'pdfbox_jars':[n for n in libs if n.startswith(('pdfbox-','fontbox-','xmpbox-'))]}
        if phase=='parche':
            for row in checksums:
                name=row['modulo']+'-2.0.37.jar';inside=z.read('BOOT-INF/lib/'+name)
                candidate_lib_hashes.append({'jar':name,'sha512_empaquetado':hashlib.sha512(inside).hexdigest(),'igual_apache':hashlib.sha512(inside).hexdigest()==row['sha512_apache']})
if candidate_lib_hashes:csvwrite(OUT/'dependencias_empaquetadas.csv',candidate_lib_hashes)
baseline=phases.get('baseline',{});patch=phases.get('parche',{})
same_cases=bool(patch) and {(c['clase'],c['caso']) for c in baseline['casos']}=={(c['clase'],c['caso']) for c in patch['casos']}
before=csvread(OUT/'integridad_antes.csv')
after=[{**row,'sha256_despues':sha(PROJECT/row['archivo'])} for row in before]
csvwrite(OUT/'integridad_despues.csv',after);unchanged=all(r['sha256_antes']==r['sha256_despues'] for r in after)
services_after=services();jsonwrite('servicios_despues.json',services_after)
expected_labels=['JwtTokenProviderTest','SecurityConfigTest','SystemApiTest','BusinessFunctionalCasesTest','PortalServiceImplTest','ExpedienteServiceImplTest','LocalStorageServiceTest','CotizacionPagoServiceImplTest','ReporteComercialPdfTest']
checks=[
('C01','JAR conservados del parche coinciden con SHA512 de Apache registrados',all(r['verificado'] for r in checksums)),
('C02','Baseline completa y aprobada sin omisiones',bool(baseline) and baseline['codigo_salida']==0 and baseline['tests']>0 and baseline['failures']==baseline['errors']==baseline['skipped']==0),
('C03','Mismos casos ejecutados tras actualizar tres módulos PDFBox',same_cases and patch.get('codigo_salida')==0 and patch.get('tests',0)>0 and patch.get('failures')==patch.get('errors')==patch.get('skipped')==0),
('C04','Cobertura de autenticación, permisos, portal, documentos, pagos y expediente',bool(patch) and all(any(s['clase'].endswith('.'+label) for s in patch['suites']) for label in expected_labels) and any(c['caso']=='CP07_aperturaExigePagoYPersiste' and c['estado']=='Exitoso' for c in patch['casos'])),
('C05','JAR candidato incluye únicamente PDFBox/fontbox/xmpbox 2.0.37 y coincide con Apache',len(candidate_lib_hashes)==3 and all(r['igual_apache'] for r in candidate_lib_hashes) and packaged.get('parche',{}).get('pdfbox_jars')==['fontbox-2.0.37.jar','pdfbox-2.0.37.jar','xmpbox-2.0.37.jar']),
('C06','Reporte comercial se genera, abre y conserva texto en ambas versiones',all((Path(CFG[p])/'target/pm14-reportes/reporte-comercial.pdf').is_file() for p in ['baseline','parche']) and all(any(c['clase'].endswith('.ReporteComercialPdfTest') and c['estado']=='Exitoso' for c in phases.get(p,{}).get('casos',[])) for p in ['baseline','parche'])),
('C07','Fuentes y despliegue originales sin cambios; servicios conservan estado y PID',unchanged and services_before==services_after and (not original_running or health()=='UP') and all(sha(deploy/rel)==h for rel,h in deployed.items())),
]
csvwrite(OUT/'criterios_PM14.csv',[{'criterio':c,'verificacion':d,'estado':'Cumple' if v else 'No cumple'} for c,d,v in checks])
ok=not errors and all(v for _,_,v in checks);now=datetime.now(timezone(timedelta(hours=-5))).isoformat(timespec='seconds')
result={'prueba':'PM14 Regresión tras parches','estado':'Exitoso' if ok else 'Falló','fecha_lima':now,'proyecto_original':str(PROJECT),'actualizacion_laboratorio':'PDFBox, FontBox y XMPBox 2.0.24 -> 2.0.37','baseline':{k:v for k,v in baseline.items() if k not in ['casos','suites']},'parche':{k:v for k,v in patch.items() if k not in ['casos','suites']},'mismos_casos':same_cases,'empaquetado':packaged,'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':7,'archivos_operativos_sin_cambios':len(before) if unchanged else None,'servicios_originales_mismos_PID':services_before==services_after,'despliegue_en_esta_ejecucion':False,'parche_previamente_desplegado':True,'backend_original_activo_al_inicio':original_running,'errores':errors,'herramientas':['Java 21','Maven 3.9.16 y Surefire','JUnit 6 y Mockito','Spring Boot Test/MockMvc/HTTP','H2 en memoria','OpenHTMLtoPDF y PDFBox','Python hashlib/zipfile','SHA512 oficial Apache'],'limites':'Nueva comparacion de regresion en copias del codigo actual con ambas versiones de PDFBox. El original ya tiene el parche instalado desde el cierre anterior; esta ejecucion no cambia su POM ni su JAR y no despliega el JAR recompilado. No se probó todo el frontend ni toda combinación de dependencias. Los rechazos esperados son entradas de validación en pruebas aisladas, no errores introducidos en el código ni fallos provocados sobre el servicio real. No se demuestra explotación de una vulnerabilidad por la sola versión antigua de PDFBox.'}
jsonwrite('resultado.json',result)

lines=['Prueba 14 - Regresion tras actualizar dependencias',
'PDFBox, FontBox y XMPBox: 2.0.24 -> 2.0.37',
f'Casos con version anterior: {baseline.get("tests",0)}; fallos: {baseline.get("failures","-")}; errores: {baseline.get("errors","-")}; omitidos: {baseline.get("skipped","-")}',
f'Casos con parche: {patch.get("tests",0)}; fallos: {patch.get("failures","-")}; errores: {patch.get("errors","-")}; omitidos: {patch.get("skipped","-")}',
'Mismos casos ejecutados: '+('SI' if same_cases else 'NO'),
'Reporte PDF generado y legible en ambas versiones: '+('SI' if checks[5][2] else 'NO'),
'Modulos del parche verificados: '+str(sum(r['igual_apache'] for r in candidate_lib_hashes))+'/3',
'Entorno: copias del backend, H2 en memoria y datos de prueba',
'Archivos originales sin cambios: '+('SI' if unchanged and all(sha(deploy/rel)==h for rel,h in deployed.items()) else 'NO'),
'Backend original: '+('activo, sin reinicios' if original_running else 'detenido antes y despues'),
'Verificaciones: '+str(result['criterios_cumplidos'])+'/7',
'Estado: '+('EXITOSO' if ok else 'FALLO')]
(OUT/'resultado.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8-sig')
print('\n'.join(lines),flush=True)
sys.exit(0 if ok else 1)
