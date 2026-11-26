"""PM08: tres procesos controlados prueban el bloqueo del proyecto en laboratorio."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,subprocess,json,time,csv,hashlib

ap=argparse.ArgumentParser(description='Prueba 8 - Bloqueo de concurrencia')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
root=Path(ap.parse_args().proyecto).resolve()
out=root/'evidencias-mantenimiento/Evidencia_Prueba_8';n=2
while out.exists():out=root/'evidencias-mantenimiento'/f'Evidencia_Prueba_8_{n}';n+=1
out.mkdir(parents=True,exist_ok=False)
lab=out/'laboratorio';lab.mkdir()
(lab/'laboratorio.json').write_text(json.dumps({'Prueba':'PM08','Raiz':str(lab)}),encoding='utf-8')
(lab/'contador.txt').write_text('0',encoding='utf-8')
helper=root/'scripts/mantenimiento/Invoke-ConBloqueo.ps1'
worker=root/'docs/pruebas/mantenimiento/PM08/Proceso-Prueba.ps1'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before=sha(helper)
ps=str(powershell())
def command(name,hold=False):
    cmd=[ps,'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(worker),'-Laboratorio',str(lab),'-Helper',str(helper),'-Instancia',name]
    return cmd+(['-EsperarLiberacion'] if hold else [])
def read(name):return json.loads((lab/name).read_text(encoding='utf-8-sig'))
def counter():return int((lab/'contador.txt').read_text(encoding='utf-8-sig'))
def stamp(value):return datetime.fromisoformat(value)
codes={};proc_a=None;log_a=None
try:
    log_a=(out/'salida_A.txt').open('wb')
    proc_a=subprocess.Popen(command('A',True),stdout=log_a,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    deadline=time.monotonic()+12
    while not (lab/'entrada-A.json').is_file():
        if proc_a.poll() is not None:raise RuntimeError('La instancia A termino antes de adquirir el bloqueo.')
        if time.monotonic()>deadline:raise RuntimeError('No se confirmo la entrada de A.')
        time.sleep(.05)
    before_b=counter();a_live_before=proc_a.poll() is None
    b=subprocess.run(command('B'),capture_output=True,timeout=12)
    (out/'salida_B.txt').write_bytes(b.stdout+b.stderr);codes['B']=b.returncode
    after_b=counter();a_live_after=proc_a.poll() is None
    summary_b=read('resumen-B.json')
    (lab/'continuar-A').write_text('Continuar',encoding='utf-8')
    codes['A']=proc_a.wait(timeout=15);log_a.close();log_a=None
    after_a=counter()
    c=subprocess.run(command('C'),capture_output=True,timeout=15)
    (out/'salida_C.txt').write_bytes(c.stdout+c.stderr);codes['C']=c.returncode
    summaries={name:read('resumen-'+name+'.json') for name in ['A','B','C']}
    entries={name:read('entrada-'+name+'.json') for name in ['A','C']}
    exits={name:read('salida-'+name+'.json') for name in ['A','C']}
    overlap_test=a_live_before and a_live_after and stamp(entries['A']['Entrada'])<=stamp(summary_b['FechaInicio'])<=stamp(summary_b['FechaFin'])<=stamp(exits['A']['Salida'])
    b_no_action=not (lab/'entrada-B.json').exists() and not (lab/'salida-B.json').exists()
    final=counter()
    no_overlap=stamp(exits['A']['Salida'])<=stamp(entries['C']['Entrada'])
    checks=[('C01','Tres procesos finalizaron sin errores',all(code==0 for code in codes.values()) and len({r['Proceso'] for r in summaries.values()})==3),('C02','A adquirio el bloqueo',summaries['A']['Estado']=='Exitoso' and summaries['A']['Adquirido']),('C03','B intento entrar mientras A mantenia el bloqueo',overlap_test),('C04','B se omitio sin ejecutar la accion',summaries['B']['Estado']=='OmitidaPorBloqueo' and not summaries['B']['Adquirido'] and summaries['B']['CodigoNativo'] in [32,33] and b_no_action and before_b==after_b==0),('C05','Contador refleja solo las acciones de A y C',after_a==1 and final==2),('C06','C entro despues de liberar A',summaries['C']['Estado']=='Exitoso' and summaries['C']['Adquirido'] and no_overlap and (lab/'tarea.lock').is_file()),('C07','Helper del proyecto sin modificaciones',before==sha(helper))]
    ok=all(v for _,_,v in checks)
    rows=[{'instancia':name,'proceso':summaries[name]['Proceso'],'estado':summaries[name]['Estado'],'bloqueo_adquirido':summaries[name]['Adquirido'],'codigo_salida':codes[name]} for name in ['A','B','C']]
    criteria=[{'codigo':code,'comprobacion':label,'resultado':'CUMPLE' if passed else 'NO CUMPLE'} for code,label,passed in checks]
    for name,data in [('procesos.csv',rows),('criterios.csv',criteria)]:
        with (out/name).open('w',encoding='utf-8-sig',newline='') as f:w=csv.DictWriter(f,fieldnames=list(data[0]));w.writeheader();w.writerows(data)
    result={'prueba':'PM08','nombre':'Bloqueo de concurrencia','fecha_lima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),'estado':'EXITOSO' if ok else 'FALLO','procesos':rows,'contador_inicial':0,'contador_antes_B':before_b,'contador_despues_B':after_b,'contador_despues_A':after_a,'contador_final':final,'B_intento_durante_A':overlap_test,'sin_solapamiento':no_overlap,'entradas':entries,'salidas':exits,'criterios':criteria,'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':7,'fallos_provocados':False,'alcance':'Procesos de laboratorio usan el helper real. La omision de B es el resultado esperado de un bloqueo ocupado. Se verifica liberacion normal, no liberacion por caida o excepcion. No se alteran servicios, base de datos ni scripts operativos.'}
    lines=['Prueba 8 - Bloqueo de concurrencia','',f'{"Instancia":<12}{"Proceso":>9}  Resultado']
    labels={'A':'Ejecutada; adquirio el bloqueo','B':'Omitida; bloqueo ocupado por A','C':'Ejecutada; adquirio el bloqueo liberado'}
    for name in ['A','B','C']:lines.append(f'{name:<12}{summaries[name]["Proceso"]:>9}  {labels[name]}')
    lines+=['','B intento entrar mientras A ejecutaba: '+('SI' if overlap_test else 'NO'),'B modifico el contador: '+('NO' if before_b==after_b==0 and b_no_action else 'REVISAR'),'C ejecuto despues de liberar A: '+('SI' if no_overlap and summaries['C']['Adquirido'] else 'NO'),'','Contador inicial: 0',f'Contador despues de A: {after_a}',f'Contador final: {final}','Acciones esperadas: 2',f'Criterios cumplidos: {result["criterios_cumplidos"]}/7','Estado: '+result['estado']]
except (OSError,RuntimeError,subprocess.TimeoutExpired,ValueError) as error:
    result={'prueba':'PM08','estado':'NO COMPLETADA','diagnostico':str(error),'alcance':'No se afirma un fallo de la aplicacion; revisar las salidas del laboratorio.'}
    lines=['Prueba 8 - Bloqueo de concurrencia','',str(error),'Estado: NO COMPLETADA']
finally:
    if proc_a is not None and proc_a.poll() is None:
        (lab/'continuar-A').write_text('Continuar',encoding='utf-8')
        proc_a.wait(timeout=30)
    if log_a is not None:log_a.close()
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
text='\n'.join(lines);(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if result['estado']=='EXITOSO' else 1)
