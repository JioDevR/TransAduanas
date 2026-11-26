"""PM05: simulación y limpieza real únicamente de archivos sintéticos de laboratorio."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,subprocess,json,csv,hashlib,os,time,zipfile

ap=argparse.ArgumentParser(description='Prueba 5 - Limpieza de temporales')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
root=Path(ap.parse_args().proyecto).resolve()
out=root/'evidencias-mantenimiento/Evidencia_Prueba_5';n=2
while out.exists():out=root/'evidencias-mantenimiento'/f'Evidencia_Prueba_5_{n}';n+=1
out.mkdir(parents=True,exist_ok=False)
lab=out/'laboratorio';lab.mkdir()
(lab/'.transaduanas-laboratorio.json').write_text(json.dumps({'Prueba':'PM05','Raiz':str(lab)},ensure_ascii=False),encoding='utf-8')
fixtures=[('temporales/vencido_1.tmp',20),('temporales/vencido_2.temp',15),('temporales/reciente.tmp',1),('temporales/reciente.temp',0),('temporales/nota.txt',20),('temporales/documento.pdf',20),('temporales/respaldo.zip',20),('temporales/subcarpeta/antiguo.tmp',20),('documentos/factura.txt',20),('documentos/anexo.tmp',20),('respaldos/copia.zip',20),('uploads/comprobante.tmp',20),('otro.txt',20)]
for rel,age in fixtures:
    p=lab/rel;p.parent.mkdir(parents=True,exist_ok=True)
    if p.suffix=='.zip':
        with zipfile.ZipFile(p,'w') as z:z.writestr('prueba.txt','Archivo sintetico de PM05')
    else:p.write_text('Archivo sintetico de PM05: '+rel+'\n',encoding='utf-8')
    timestamp=time.time()-age*86400;os.utime(p,(timestamp,timestamp))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before={rel:sha(lab/rel) for rel,_ in fixtures}
script=root/'scripts/mantenimiento/Limpiar-Temporales.ps1';script_hash=sha(script)
assert lab.resolve().is_relative_to(out.resolve()) and lab.parent==out and (lab/'temporales').is_dir()
def execute(phase,simulate):
    dest=out/phase;dest.mkdir()
    cmd=[str(powershell()),'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(script),'-CarpetaLaboratorio',str(lab),'-DirectorioResultados',str(dest),'-DiasRetencion','7']
    if simulate:cmd.append('-WhatIf')
    r=subprocess.run(cmd,capture_output=True,timeout=30)
    (dest/'salida.txt').write_bytes(r.stdout+r.stderr)
    files=list(dest.glob('limpieza-*.json'))
    if len(files)!=1:raise RuntimeError('La fase '+phase+' no produjo un resumen; revisar su salida.')
    return r.returncode,json.loads(files[0].read_text(encoding='utf-8-sig'))
sim_code,sim=execute('simulacion',True)
simulation_ok=sim_code==0 and sim['Simulacion'] and sim['Eliminados']==0 and all((lab/r).is_file() and sha(lab/r)==h for r,h in before.items())
if not simulation_ok:raise RuntimeError('La simulacion no cumple; no se ejecuta limpieza real.')
real_code,real=execute('limpieza',False)
expected={'temporales/vencido_1.tmp','temporales/vencido_2.temp'}
deleted={r for r in before if not (lab/r).exists()}
preserved=set(before)-expected
same=sum((lab/r).is_file() and sha(lab/r)==before[r] for r in preserved)
rows=[{'archivo':rel,'antiguedad_dias':age,'antes':'Existe','despues':'Eliminado' if rel in deleted else 'Conservado','hash_antes':before[rel],'hash_despues':sha(lab/rel) if (lab/rel).is_file() else ''} for rel,age in fixtures]
checks=[('C01','Simulacion sin eliminar archivos',simulation_ok),('C02','Limpieza real registrada sin error',real_code==0 and real['Estado']=='Exitoso' and not real['Simulacion']),('C03','Solo temporales vencidos directos eliminados',deleted==expected and real['Eliminados']==2),('C04','Temporales recientes conservados',all((lab/r).is_file() for r in ['temporales/reciente.tmp','temporales/reciente.temp'])),('C05','Extensiones protegidas conservadas',all((lab/r).is_file() for r in ['temporales/nota.txt','temporales/documento.pdf','temporales/respaldo.zip'])),('C06','Subcarpeta sin recorrer',(lab/'temporales/subcarpeta/antiguo.tmp').is_file()),('C07','Documentos, respaldos y uploads del laboratorio intactos',same==11),('C08','Script operativo sin modificaciones',sha(script)==script_hash)]
ok=all(v for _,_,v in checks)
criteria=[{'codigo':code,'comprobacion':label,'resultado':'CUMPLE' if passed else 'NO CUMPLE'} for code,label,passed in checks]
for name,data in [('archivos.csv',rows),('criterios.csv',criteria)]:
    with (out/name).open('w',encoding='utf-8-sig',newline='') as f:w=csv.DictWriter(f,fieldnames=list(data[0]));w.writeheader();w.writerows(data)
result={'prueba':'PM05','nombre':'Limpieza de temporales','fecha_lima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),'estado':'EXITOSO' if ok else 'FALLO','retencion_dias':7,'archivos_de_prueba':13,'eliminados':len(deleted),'conservados':13-len(deleted),'conservados_con_hash_identico':same,'simulacion_correcta':simulation_ok,'duracion_limpieza_segundos':real['DuracionSegundos'],'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':8,'criterios':criteria,'archivos':rows,'laboratorio':str(lab),'fallos_provocados':False,'alcance':'Archivos sinteticos con edades controladas. Se ejecuta el script real solo en el laboratorio marcado; no prueba limpieza productiva, enlaces o fallos de archivos bloqueados. No reinicia servicios ni accede a MySQL.'}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
lines=['Prueba 5 - Limpieza de temporales','','Retencion: 7 dias','Simulacion: 13 archivos conservados','','Limpieza realizada en el laboratorio:',f'{"Archivo":<36}{"Dias":>5}  Resultado']
lines.extend(f'{r["archivo"]:<36}{r["antiguedad_dias"]:>5}  {r["despues"]}' for r in rows)
lines+=['',f'Archivos eliminados: {len(deleted)}',f'Archivos conservados: {13-len(deleted)}',f'Archivos conservados sin cambios: {same}/11',f'Criterios cumplidos: {result["criterios_cumplidos"]}/8','Estado: '+result['estado']]
text='\n'.join(lines);(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if ok else 1)
