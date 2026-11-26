"""Repite PM01 por lectura; guarda evidencias nuevas sin corregir documentos."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
from urllib.parse import urlsplit,unquote
import argparse,csv,hashlib,json,posixpath,re,uuid

parser=argparse.ArgumentParser(description='PM01 - revision documental de TransAduanas')
parser.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
args=parser.parse_args();root=Path(args.proyecto).resolve()
scope=['README.md','docs/instalacion.md','docs/frontend.md','app-sga/README.md','docs/pruebas/README.md','docs/backend.md','docs/manual_usuario.md','docs/inventario_documentacion.md']
sources=['backend-sga/pom.xml','backend-sga/src/main/resources/application.properties','app-sga/vite.config.js','backend-sga/src/main/java/pe/sga/controller/HealthController.java']
now=datetime.now(timezone(timedelta(hours=-5)))
out=root/'evidencias-mantenimiento'/'Evidencia_Prueba_1'
numero=2
while out.exists():
    out=root/'evidencias-mantenimiento'/f'Evidencia_Prueba_1_{numero}'
    numero+=1
out.mkdir(parents=True,exist_ok=False)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(rel):
    p=root/rel
    return p.read_text(encoding='utf-8-sig') if p.is_file() else ''
before={p:sha(root/p) for p in scope+sources if (root/p).is_file()}
links=[]
for p in scope:
    fenced=False
    for line_num,line in enumerate(read(p).splitlines(),1):
        if line.lstrip().startswith(('```','~~~')):fenced=not fenced;continue
        if fenced:continue
        for m in re.finditer(r'(?<!!)\[[^\]]*\]\(([^)]+)\)',line):
            url=urlsplit(m.group(1).strip().strip('<>'))
            if url.scheme or url.netloc or not url.path:continue
            rel=posixpath.normpath(posixpath.join(posixpath.dirname(p),unquote(url.path)))
            links.append({'documento':p,'linea':line_num,'destino':rel,'existe':(root/rel).exists()})
manual=read('docs/manual_usuario.md');version_match=re.search(r'<artifactId>sga-backend</artifactId>\s*<version>([^<]+)',read('backend-sga/pom.xml'));version=version_match.group(1) if version_match else 'NO IDENTIFICADA'
checks=[
 ('C01','Guia de instalacion presente y legible',bool(read('docs/instalacion.md'))),
 ('C02','Manual de usuario con operaciones y version',all(s in manual for s in ['Acceso al sistema','Consultar cotizaciones','Registrar y subsanar un pago','Consultar expedientes','Referencias de la version'.replace('version','versión'),version])),
 ('C03','Destinos locales de enlaces accesibles',bool(links) and all(r['existe'] for r in links)),
 ('C04','Puerto de desarrollo coherente (8085)','http://localhost:8085/api/v1' in read('app-sga/README.md') and 'localhost:8080' not in read('app-sga/README.md') and 'server.port=${SERVER_PORT:8085}' in read('backend-sga/src/main/resources/application.properties') and "target: 'http://localhost:8085'" in read('app-sga/vite.config.js')),
 ('C05','Guia compatible con version del codigo',f'Compatibilidad revisada con el código {version}' in read('docs/instalacion.md')),
 ('C06','JWT obligatorio documentado sin mostrar valores','JWT_SECRET es obligatorio' in read('docs/instalacion.md') and 'jwt.secret=${JWT_SECRET}' in read('backend-sga/src/main/resources/application.properties')),
 ('C07','Ruta de salud coherente con controlador','/api/v1/health' in read('docs/instalacion.md') and '@RequestMapping("/api/v1/health")' in read('backend-sga/src/main/java/pe/sga/controller/HealthController.java')),
]
after={p:sha(root/p) for p in before};unchanged=before==after
ok=all(c for _,_,c in checks) and unchanged
rows=[{'codigo':c,'comprobacion':d,'resultado':'CUMPLE' if v else 'NO CUMPLE'} for c,d,v in checks]
for name,data in [('criterios.csv',rows),('enlaces.csv',links)]:
    if data:
        with (out/name).open('w',encoding='utf-8-sig',newline='') as f:w=csv.DictWriter(f,fieldnames=list(data[0]));w.writeheader();w.writerows(data)
result={'prueba':'PM01','nombre':'Revision de documentos','tipo':'Ejecucion nueva de solo lectura','fecha_lima':now.isoformat(),'estado':'EXITOSO' if ok else 'FALLO','documentos_revisados':len(scope),'enlaces_revisados':len(links),'enlaces_ausentes':sum(not r['existe'] for r in links),'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':7,'version_codigo':version,'archivos_inspeccionados_sin_cambios':len(before) if unchanged else 0,'criterios':rows,'alcance':'Revision estatica documental. Puerto 8085 de desarrollo; no modifica ni evalua el puerto 8086 del despliegue staging. No inicia servicios ni conecta a la base de datos.'}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')

titulos=['Guia de instalacion','Manual de usuario','Enlaces de documentos','Puerto de desarrollo (8085)','Version documentada','Configuracion JWT','Ruta de salud']
lines=['Prueba 1 - Revision de documentos','']
lines.extend(f'{titulo}: {criterio["resultado"]}' for titulo,criterio in zip(titulos,result['criterios']))
lines+=['',f'Documentos revisados: {result["documentos_revisados"]}',f'Enlaces revisados: {result["enlaces_revisados"]} | No encontrados: {result["enlaces_ausentes"]}',f'Criterios cumplidos: {result["criterios_cumplidos"]}/{result["criterios_totales"]}','Estado: '+result['estado']]
text='\n'.join(lines)
(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if ok else 1)
