"""PM09: repetir dos veces el mismo mantenimiento sin efectos adicionales."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,subprocess,json,csv,hashlib,os,time,zipfile

ap=argparse.ArgumentParser(description='Prueba 9 - Repeticion del mantenimiento')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
root=Path(ap.parse_args().proyecto).resolve()
out=root/'evidencias-mantenimiento/Evidencia_Prueba_9';n=2
while out.exists():out=root/'evidencias-mantenimiento'/f'Evidencia_Prueba_9_{n}';n+=1
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

def snapshot():
    return {'archivos':{p.relative_to(lab).as_posix():{'sha256':sha(p),'bytes':p.stat().st_size,'modificado_ns':p.stat().st_mtime_ns} for p in sorted(lab.rglob('*')) if p.is_file()},'carpetas':sorted(p.relative_to(lab).as_posix() for p in lab.rglob('*') if p.is_dir())}
dest=out/'registros';dest.mkdir()
cmd=[str(powershell()),'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(script),'-CarpetaLaboratorio',str(lab),'-DirectorioResultados',str(dest),'-DiasRetencion','7']
def execute(number):
    prior={p.name for p in dest.glob('limpieza-*.json')}
    result=subprocess.run(cmd,capture_output=True,timeout=30)
    (out/f'salida_{number}.txt').write_bytes(result.stdout+result.stderr)
    new=[p for p in dest.glob('limpieza-*.json') if p.name not in prior]
    if len(new)!=1:raise RuntimeError('La ejecucion '+str(number)+' no genero un resumen unico.')
    return result.returncode,json.loads(new[0].read_text(encoding='utf-8-sig')),list(cmd)
initial=snapshot()
code1,first,args1=execute(1);after_first=snapshot()
code2,second,args2=execute(2);after_second=snapshot()
expected={'temporales/vencido_1.tmp','temporales/vencido_2.temp'}
deleted=set(initial['archivos'])-set(after_first['archivos'])
kept=set(before)-expected
controls=sum((lab/p).is_file() and sha(lab/p)==before[p] for p in kept)
checks=[('C01','Mismo script, comando, origen y retencion',args1==args2),('C02','Primera ejecucion elimina solo los dos vencidos',code1==0 and first['Estado']=='Exitoso' and first['Eliminados']==2 and deleted==expected),('C03','Segunda ejecucion sin nuevas eliminaciones',code2==0 and second['Estado']=='Exitoso' and second['Eliminados']==0),('C04','Estado final identico tras ambas ejecuciones',after_first==after_second),('C05','Once archivos protegidos sin modificaciones',controls==11),('C06','Sin archivos ni carpetas duplicados',set(after_first['archivos'])==set(after_second['archivos']) and after_first['carpetas']==after_second['carpetas']),('C07','Script y marca de laboratorio intactos',sha(script)==script_hash and initial['archivos']['.transaduanas-laboratorio.json']==after_second['archivos']['.transaduanas-laboratorio.json'])]
criteria=[{'codigo':c,'comprobacion':label,'resultado':'CUMPLE' if value else 'NO CUMPLE'} for c,label,value in checks]
ok=all(v for _,_,v in checks)
result={'prueba':'PM09','nombre':'Repeticion del mantenimiento','fecha_lima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),'estado':'EXITOSO' if ok else 'FALLO','herramientas':['PowerShell','Limpiar-Temporales.ps1 del proyecto','Python','SHA256'],'primera_ejecucion':{'archivos_antes':13,'eliminados':first['Eliminados'],'conservados':len(set(before)&set(after_first['archivos'])),'codigo_salida':code1,'estado_tarea':first['Estado']},'segunda_ejecucion':{'archivos_antes':len(set(before)&set(after_first['archivos'])),'eliminados':second['Eliminados'],'conservados':len(set(before)&set(after_second['archivos'])),'codigo_salida':code2,'estado_tarea':second['Estado']},'comando_identico':args1==args2,'estado_final_identico':after_first==after_second,'controles_intactos':controls,'criterios':criteria,'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':7,'fallos_provocados':False,'alcance':'Dos ejecuciones reales sobre el mismo laboratorio y con los mismos parametros. Verifica idempotencia de la limpieza, no de todas las tareas del sistema. Adaptacion Windows; no se ejecuto Ansible. No modifica servicios ni base de datos.'}
for name,data in [('resultado.json',result),('inventario_inicial.json',initial),('inventario_primera.json',after_first),('inventario_segunda.json',after_second),('comandos.json',{'primero':args1,'segundo':args2})]:(out/name).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
with (out/'criterios.csv').open('w',encoding='utf-8-sig',newline='') as f:w=csv.DictWriter(f,fieldnames=list(criteria[0]));w.writeheader();w.writerows(criteria)
lines=['Prueba 9 - Repeticion del mantenimiento','','Tarea: Limpieza de temporales','Retencion: 7 dias','',f'{"Ejecucion":<16}{"Antes":>8}{"Eliminados":>12}{"Conservados":>13}{"Codigo":>9}']
for name,key in [('Primera','primera_ejecucion'),('Segunda','segunda_ejecucion')]:
    data=result[key];lines.append(f'{name:<16}{data["archivos_antes"]:>8}{data["eliminados"]:>12}{data["conservados"]:>13}{data["codigo_salida"]:>9}')
lines+=['','Mismo comando y parametros: '+('SI' if args1==args2 else 'NO'),'Estado final tras ambas ejecuciones: '+('IGUAL' if after_first==after_second else 'DIFERENTE'),f'Archivos protegidos sin cambios: {controls}/11','Archivos o carpetas duplicados: '+('NO' if checks[5][2] else 'REVISAR'),f'Criterios cumplidos: {result["criterios_cumplidos"]}/7','Estado: '+result['estado']]
text='\n'.join(lines);(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if ok else 1)
