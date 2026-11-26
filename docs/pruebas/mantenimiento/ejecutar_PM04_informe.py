"""PM04: comprueba el registro real de la consulta y un fallo natural anterior."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,subprocess,json,csv,hashlib,shutil

ap=argparse.ArgumentParser(description='Prueba 4 - Registro de ejecucion')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
ap.add_argument('--fallo-anterior',default=str(Path(__file__).resolve().parent/'referencias/PM04_fallo_historico.json'))
a=ap.parse_args();root=Path(a.proyecto).resolve()
out=root/'evidencias-mantenimiento/Evidencia_Prueba_4';n=2
while out.exists():out=root/'evidencias-mantenimiento'/f'Evidencia_Prueba_4_{n}';n+=1
out.mkdir(parents=True,exist_ok=False)
script=root/'scripts/mantenimiento/Consultar-Disponibilidad.ps1'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before=sha(script)
cmd=[str(powershell()),'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(script),'-DirectorioResultados',str(out)]
process=subprocess.run(cmd,capture_output=True,timeout=35)
(out/'salida_script.txt').write_bytes(process.stdout+process.stderr)
records=list(out.glob('resumen-*.json'))
if len(records)!=1:raise RuntimeError('La consulta no genero un unico registro. Revisar salida_script.txt.')
actual=json.loads(records[0].read_text(encoding='utf-8-sig'))
previous_path=Path(a.fallo_anterior);previous=json.loads(previous_path.read_text(encoding='utf-8-sig'))
shutil.copy2(previous_path,out/'fallo_anterior.json')
fields=['NombreTarea','FechaInicio','FechaFin','DuracionSegundos','Estado','CodigoSalida']
def complete(record):
    return all(k in record for k in fields) and bool(record['NombreTarea']) and datetime.fromisoformat(record['FechaFin'])>=datetime.fromisoformat(record['FechaInicio']) and record['DuracionSegundos']>=0
all_checks=bool(actual.get('Comprobaciones')) and all(x['Cumple'] for x in actual['Comprobaciones'])
consistent=(actual['Estado']=='Exitoso')==all_checks and actual['CodigoSalida']==(0 if all_checks else 1) and process.returncode==actual['CodigoSalida']
real_failure=previous['Estado']=='Fallo' and previous['CodigoSalida']==1 and not previous.get('Simulacion') and any(e.get('Error')=='System.UnauthorizedAccessException' for e in previous.get('Eventos',[]))
unchanged=before==sha(script)
ok=complete(actual) and complete(previous) and consistent and real_failure and unchanged
result={'prueba':'PM04','nombre':'Registro de ejecucion','fecha_lima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),'estado':'EXITOSO' if ok else 'FALLO','ejecucion_actual':actual,'fallo_anterior':previous,'origen_fallo_anterior':str(previous_path),'sha256_fallo_anterior':sha(previous_path),'campos_exigidos':fields,'campos_actual_completos':complete(actual),'campos_fallo_anterior_completos':complete(previous),'estado_y_codigo_coherentes':consistent,'fallo_natural_acreditado':real_failure,'script_sin_modificar':unchanged,'fallo_provocado':False,'alcance':'Ejecuta solo una consulta de disponibilidad. Contrasta su registro nuevo con un fallo de limpieza observado el 23/11/2026 en PM15. No reproduce ese fallo, no ejecuta limpieza ni detiene o inicia servicios. Un fallo de la tarea no equivale a un fallo del registrador si se captura coherentemente.'}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
def stamp(value):return datetime.fromisoformat(value).strftime('%d/%m/%Y %H:%M:%S')
lines=['Prueba 4 - Registro de ejecucion','','Consulta realizada ahora','Tarea: Revision de disponibilidad','Inicio: '+stamp(actual['FechaInicio']),'Fin: '+stamp(actual['FechaFin']),f'Duracion: {actual["DuracionSegundos"]:.3f} segundos','Estado de la tarea: '+actual['Estado'].upper(),'Codigo de salida: '+str(actual['CodigoSalida'])]
for check in actual['Comprobaciones']:lines.append(check['Nombre']+': '+(check.get('Obtenido') or check.get('Error') or 'Sin respuesta'))
lines+=['','Registro de fallo anterior (23/11/2026)','Tarea: Limpieza de temporales','Inicio: '+stamp(previous['FechaInicio']),'Fin: '+stamp(previous['FechaFin']),f'Duracion: {previous["DuracionSegundos"]:.3f} segundos','Estado de la tarea: FALLO','Codigo de salida: 1','Error registrado: UnauthorizedAccessException','','Campos del registro actual: '+('6/6' if complete(actual) else 'INCOMPLETO'),'Campos del fallo anterior: '+('6/6' if complete(previous) else 'INCOMPLETO'),'Estado de la prueba: '+result['estado']]
text='\n'.join(lines);(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if ok else 1)
