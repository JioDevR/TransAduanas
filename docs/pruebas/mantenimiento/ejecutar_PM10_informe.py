"""PM10: retención real del script del proyecto sobre ZIP sintéticos, en dos laboratorios."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,subprocess,hashlib,json,csv,zipfile,os

ap=argparse.ArgumentParser(description='Prueba 10 - Retencion de respaldos')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
root=Path(ap.parse_args().proyecto).resolve()
out=root/'evidencias-mantenimiento/Evidencia_Prueba_10';n=2
while out.exists():out=root/'evidencias-mantenimiento'/f'Evidencia_Prueba_10_{n}';n+=1
out.mkdir(parents=True,exist_ok=False)
script=root/'scripts/mantenimiento/Retener-Respaldos.ps1'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
script_hash=sha(script);now=datetime.now(timezone.utc)
specs={'mixtas':[('antigua_1.zip',20,False),('antigua_2.zip',10,False),('reciente.zip',3,True),('actual.zip',1,True)],'vencidas':[('antigua.zip',30,False),('ultima.zip',15,True)]}
scenarios={};details=[]
def create_zip(p):
    p.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(p,'w',zipfile.ZIP_DEFLATED) as z:z.writestr('dato_de_prueba.txt','Contenido sintetico para PM10; no es un respaldo de la base de datos.\n')
def run(lab,dest,simulate):
    dest.mkdir(parents=True)
    cmd=[str(powershell()),'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(script),'-CarpetaLaboratorio',str(lab),'-DirectorioResultados',str(dest),'-DiasRetencion','7']
    if simulate:cmd.append('-WhatIf')
    r=subprocess.run(cmd,capture_output=True,timeout=30)
    (dest/'salida.txt').write_bytes(r.stdout+r.stderr)
    summaries=list(dest.glob('retencion-*.json'))
    if len(summaries)!=1:raise RuntimeError('La retencion no genero un resumen unico; revisar salida.txt.')
    return r.returncode,json.loads(summaries[0].read_text(encoding='utf-8-sig'))
for label,spec in specs.items():
    lab=out/'laboratorios'/label;lab.mkdir(parents=True)
    assert lab.resolve().is_relative_to(out.resolve())
    (lab/'laboratorio.json').write_text(json.dumps({'Prueba':'PM10','Raiz':str(lab)}),encoding='utf-8')
    manifest=[]
    for name,age,keep in spec:
        p=lab/'copias'/name;create_zip(p);date=now-timedelta(days=age);os.utime(p,(date.timestamp(),date.timestamp()))
        manifest.append({'Archivo':name,'SHA256':sha(p),'FechaUTC':date.isoformat()})
    (lab/'inventario_respaldos.json').write_text(json.dumps({'Copias':manifest},indent=2),encoding='utf-8')
    controls={}
    for rel in ['copias/fuera_inventario.zip','documentos/comprobante.txt','copias/subcarpeta/extra.zip']:
        p=lab/rel
        if p.suffix=='.zip':create_zip(p)
        else:p.parent.mkdir(parents=True,exist_ok=True);p.write_text('Control sintetico que no debe modificarse.',encoding='utf-8')
        controls[rel]=sha(p)
    before={p.relative_to(lab).as_posix():sha(p) for p in lab.rglob('*') if p.is_file()}
    (out/('inventario_inicial_'+label+'.json')).write_text(json.dumps(before,indent=2),encoding='utf-8')
    sim_code,sim=run(lab,out/'registros'/label/'simulacion',True)
    simulation_ok=sim_code==0 and sim['Simulacion'] and sim['Eliminadas']==0 and all((lab/rel).is_file() and sha(lab/rel)==h for rel,h in before.items())
    if not simulation_ok:raise RuntimeError('La simulacion no cumple; no se inicia la retencion real.')
    real_code,real=run(lab,out/'registros'/label/'ejecucion',False)
    for name,age,keep in spec:
        p=lab/'copias'/name;present=p.is_file();original=next(m['SHA256'] for m in manifest if m['Archivo']==name)
        details.append({'escenario':label,'archivo':name,'antiguedad_dias':age,'esperado':'Conservado' if keep else 'Eliminado','resultado':'Conservado' if present else 'Eliminado','cumple':present==keep and (not present or sha(p)==original),'hash_antes':original,'hash_despues':sha(p) if present else ''})
    valid_kept=True
    for name,_,keep in spec:
        if keep:
            with zipfile.ZipFile(lab/'copias'/name) as z:valid_kept=valid_kept and z.testzip() is None
    scenarios[label]={'codigo_salida':real_code,'estado_tarea':real['Estado'],'eliminadas':real['Eliminadas'],'conservadas':real['Conservadas'],'ultima_protegida':real['UltimaCopiaProtegida'],'simulacion_correcta':simulation_ok,'controles_intactos':sum((lab/p).is_file() and sha(lab/p)==h for p,h in controls.items()),'copias_conservadas_legibles':valid_kept,'sin_eliminacion_fuera_inventario':all((lab/p).is_file() and sha(lab/p)==h for p,h in before.items() if p not in {'copias/'+name for name,_,_ in spec})}
checks=[('C01','Dos simulaciones sin eliminar archivos',all(s['simulacion_correcta'] for s in scenarios.values())),('C02','Ejecuciones reales registradas sin error',all(s['codigo_salida']==0 and s['estado_tarea']=='Exitoso' for s in scenarios.values())),('C03','Copias vigentes conservadas y vencidas depuradas',all(r['cumple'] for r in details if r['escenario']=='mixtas') and scenarios['mixtas']['eliminadas']==2 and scenarios['mixtas']['conservadas']==2),('C04','Ultima copia integra vencida protegida',all(r['cumple'] for r in details if r['escenario']=='vencidas') and scenarios['vencidas']['eliminadas']==1 and scenarios['vencidas']['conservadas']==1 and scenarios['vencidas']['ultima_protegida']=='ultima.zip'),('C05','Contenido de las tres copias conservadas intacto',all(r['cumple'] for r in details if r['esperado']=='Conservado')),('C06','Copias conservadas legibles como ZIP',all(s['copias_conservadas_legibles'] for s in scenarios.values())),('C07','Seis controles ajenos al inventario intactos',sum(s['controles_intactos'] for s in scenarios.values())==6 and all(s['sin_eliminacion_fuera_inventario'] for s in scenarios.values())),('C08','Script operativo sin modificar',sha(script)==script_hash)]
criteria=[{'codigo':c,'comprobacion':d,'resultado':'CUMPLE' if passed else 'NO CUMPLE'} for c,d,passed in checks]
ok=all(v for _,_,v in checks)
result={'prueba':'PM10','nombre':'Retencion de respaldos','fecha_lima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),'estado':'EXITOSO' if ok else 'FALLO','retencion_dias':7,'escenarios':scenarios,'copias_inventariadas':6,'eliminadas':sum(s['eliminadas'] for s in scenarios.values()),'conservadas':sum(s['conservadas'] for s in scenarios.values()),'controles_intactos':sum(s['controles_intactos'] for s in scenarios.values()),'criterios':criteria,'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':8,'archivos':details,'fallos_provocados':False,'alcance':'ZIP sinteticos con fechas UTC de inventario conocidas; no usa respaldos reales ni altera servicios. Retencion de siete dias solo para laboratorio. SHA256 y lectura ZIP no acreditan restauracion MySQL, cifrado ni politica 3-2-1.'}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
for name,rows in [('copias.csv',details),('criterios.csv',criteria)]:
    with (out/name).open('w',encoding='utf-8-sig',newline='') as f:w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
lines=['Prueba 10 - Retencion de respaldos','','Retencion de prueba: 7 dias','Simulacion: sin eliminaciones']
for label,title in [('mixtas','Copias recientes y vencidas'),('vencidas','Todas las copias vencidas')]:
    lines+=['',title,f'{"Archivo":<23}{"Dias":>6}  Resultado']
    for row in details:
        if row['escenario']==label:
            note=' (ultima copia protegida)' if label=='vencidas' and row['archivo']=='ultima.zip' else ''
            lines.append(f'{row["archivo"]:<23}{row["antiguedad_dias"]:>6}  {row["resultado"]}'+note)
lines+=['',f'Copias eliminadas: {result["eliminadas"]}',f'Copias conservadas: {result["conservadas"]}',f'Controles sin cambios: {result["controles_intactos"]}/6','Integridad de copias conservadas: '+('CORRECTA' if checks[4][2] and checks[5][2] else 'REVISAR'),f'Criterios cumplidos: {result["criterios_cumplidos"]}/8','Estado: '+result['estado']]
text='\n'.join(lines);(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if ok else 1)
