"""Revisión documental PM02. No ejecuta los procedimientos del runbook."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
from urllib.parse import unquote,urlsplit
import argparse,csv,hashlib,json,re

ap=argparse.ArgumentParser(description='Prueba 2 - Guia de mantenimiento')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
root=Path(ap.parse_args().proyecto).resolve()
out=root/'evidencias-mantenimiento/Evidencia_Prueba_2';n=2
while out.exists():out=root/'evidencias-mantenimiento'/f'Evidencia_Prueba_2_{n}';n+=1
out.mkdir(parents=True,exist_ok=False)
run=root/'docs/RUNBOOK_MAINTENANCE.md';check=root/'docs/CHECKLIST.md'
references=['monitoreo/Iniciar-Monitoreo.ps1','monitoreo/Detener-Monitoreo.ps1','docs/pruebas/despliegue/kit-11-15/Gestionar-Despliegue.ps1']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
before={str(p):sha(p) for p in [run,check]+[root/r for r in references] if p.is_file()}
r=run.read_text(encoding='utf-8-sig') if run.is_file() else ''
c=check.read_text(encoding='utf-8-sig') if check.is_file() else ''
tasks=re.findall(r'^## (T\d\d) (.+?)\n(.*?)(?=^## |\Z)',r,re.M|re.S)
fields=['Responsable:','Frecuencia:','Comandos:','Verificación:','Recuperación:','Evidencia:','Situación:']
task_checks=[{'tarea':t,'nombre':name,'campos_ausentes':'; '.join(f for f in fields if f not in body)} for t,name,body in tasks]
links=[]
for p,text in [(run,r),(check,c)]:
    fence=False
    for number,line in enumerate(text.splitlines(),1):
        if line.lstrip().startswith(('```','~~~')):fence=not fence;continue
        if fence:continue
        for m in re.finditer(r'(?<!!)\[[^\]]*\]\(([^)]+)\)',line):
            u=urlsplit(m.group(1).strip().strip('<>'))
            if u.scheme or u.netloc or not u.path:continue
            dest=(p.parent/unquote(u.path)).resolve()
            links.append({'documento':p.name,'linea':number,'destino':str(dest),'accesible':dest.exists()})
checks=[
('C01','Guia de mantenimiento',bool(r) and 'Alcance' in r),
('C02','Lista de comprobacion y calendario',bool(c) and all(s in c for s in ['Diario','Semanal','Mensual'])),
('C03','Datos completos en cada tarea',bool(tasks) and all(not t['campos_ausentes'] for t in task_checks)),
('C04','Preparacion y cierre de las tareas',all(s in r for s in ['Preparación','Cierre','Detener la tarea'])),
('C05','Tareas incluidas en la lista',bool(tasks) and all(t in c for t,_,_ in tasks)),
('C06','Enlaces y scripts de referencia',bool(links) and all(l['accesible'] for l in links) and all((root/p).is_file() for p in references)),
('C07','Procedimientos y automatizacion diferenciados',all(s in r for s in ['Pendiente de implementar','PM11','PM12','PM13','cron','systemd'])),
]
unchanged=all(Path(p).is_file() and sha(Path(p))==h for p,h in before.items())
ok=all(v for _,_,v in checks) and unchanged
criteria=[{'codigo':i,'comprobacion':label,'resultado':'CUMPLE' if v else 'NO CUMPLE'} for i,label,v in checks]
result={'prueba':'PM02','nombre':'Guia de mantenimiento','fecha_lima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),'estado':'EXITOSO' if ok else 'FALLO','documentos_revisados':2,'tareas_revisadas':len(tasks),'enlaces_revisados':len(links),'enlaces_ausentes':sum(not l['accesible'] for l in links),'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':7,'archivos_inspeccionados_sin_cambios':unchanged,'criterios':criteria,'alcance':'Revisión de estructura, campos, referencias y distinción entre procedimientos y automatizaciones. No acredita ejecución ni implantación de todas las tareas; no ejecuta los comandos documentados ni modifica documentos, servicios o datos.'}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
for name,rows in [('criterios.csv',criteria),('tareas.csv',task_checks),('enlaces.csv',links)]:
    if rows:
        with (out/name).open('w',encoding='utf-8-sig',newline='') as f:w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
lines=['Prueba 2 - Guia de mantenimiento','']
lines.extend(label+': '+('CUMPLE' if v else 'NO CUMPLE') for _,label,v in checks)
lines+=['',f'Documentos revisados: 2',f'Tareas revisadas: {len(tasks)}',f'Enlaces revisados: {len(links)} | No encontrados: {result["enlaces_ausentes"]}',f'Criterios cumplidos: {result["criterios_cumplidos"]}/7','Estado: '+result['estado']]
text='\n'.join(lines);(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if ok else 1)
