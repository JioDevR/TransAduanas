from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,base64,csv,ctypes,hashlib,json,msvcrt,os,re,shutil,subprocess,sys,time,uuid
from ctypes import wintypes as wt
import urllib.request,urllib.error,urllib.parse

p=argparse.ArgumentParser()
p.add_argument('--proyecto',default=r'C:\Users\asus\Music\TransAduanas')
p.add_argument('--autorizar-carga-real-host',action='store_true')
a=p.parse_args();root=Path(a.proyecto)
lock=(root/'monitoreo/M14-ejecucion.lock').open('a+b');lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
except OSError:raise RuntimeError('Ya existe una M14 en curso.') from None
created=datetime.now(timezone(timedelta(hours=-5)))
marker=('M14-' if a.autorizar_carga_real_host else 'preparacion-M14-ejecutor-')+created.strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6]
out=root/'evidencias-monitoreo'/marker;out.mkdir(parents=True,exist_ok=False)
shutil.copy2(__file__,out/Path(__file__).name)
worker=Path(__file__).with_name('worker_cpu_m14.py')
if not worker.exists():raise RuntimeError('Falta worker_cpu_m14.py; no se genera carga.')
source_rule=(root/'monitoreo/prometheus/reglas.yml').read_text(encoding='utf-8-sig')
if not re.search(r'(?s)- alert: CPUHostAlta\s+expr: system_cpu_usage\{job="transaduanas-backend"\} > 0.85\s+for: 5m',source_rule):raise RuntimeError('La regla CPU no coincide con el alcance previsto.')
private=json.loads((Path.home()/'Music/TransAduanas-herramientas/monitoreo/credenciales-locales.json').read_text(encoding='utf-8-sig'))
auth='Basic '+base64.b64encode((private['usuario']+':'+private['password']).encode()).decode();del private
def api(url,payload=None,grafana=False):
    headers={'Authorization':auth} if grafana else {}
    if payload is not None:headers['Content-Type']='application/json'
    request=urllib.request.Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=headers)
    with urllib.request.urlopen(request,timeout=8) as r:return json.load(r)
def number(expr,when=None):
    params={'query':expr}
    if when is not None:params['time']=when
    result=api('http://127.0.0.1:9090/api/v1/query?'+urllib.parse.urlencode(params))['data']['result']
    return float(result[0]['value'][1]) if result else None
def service_snapshot():
    command="@{Servicios=@(Get-CimInstance Win32_Service -Filter \"Name='MySQL84' OR Name='TransAduanasBackend'\"|Select-Object Name,State,ProcessId); MemoriaKB=(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory; CPUs=(Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors}|ConvertTo-Json -Depth 4 -Compress"
    r=subprocess.run(['powershell.exe','-NoProfile','-Command',command],capture_output=True,text=True,check=True)
    return json.loads(r.stdout.strip().lstrip('\ufeff'))
def http_status(url):
    try:
        with urllib.request.urlopen(url,timeout=5) as r:return r.status
    except urllib.error.HTTPError as e:return e.code
    except Exception:return 0
initial=service_snapshot();cpus=initial['CPUs']
(out/'servicios-iniciales.json').write_text(json.dumps(initial,indent=2),encoding='utf-8')
if initial['MemoriaKB']<1572864:raise RuntimeError('Menos de 1.5 GiB disponibles; no se genera carga.')
if not 1<=cpus<=32:raise RuntimeError('Topologia CPU no contemplada; no se genera carga.')
if number('up{job="transaduanas-backend"}')!=1:raise RuntimeError('El backend no esta recolectandose; no se genera carga.')
if number('system_cpu_count{job="transaduanas-backend"}')!=cpus:raise RuntimeError('No coinciden procesadores Windows y Micrometer.')
live=[(5173,'/'),(5174,'/'),(5180,'/'),(5180,'/api/v1/catalogos/puertos')]
if any(http_status('http://127.0.0.1:'+str(port)+path)!=200 for port,path in live):raise RuntimeError('No hay linea base de disponibilidad; no se genera carga.')
if not a.autorizar_carga_real_host:
    (out/'preparacion.txt').write_text('Ejecutor y disponibilidad comprobados sin generar carga. Se necesita autorizar carga CPU del host compartido para ejecutar M14 completa; validacion de reglas promtool es evidencia separada.\n',encoding='utf-8')
    print('PREPARADO: '+str(cpus)+' CPUs logicas; servicios y metricas disponibles. NO se genero carga. Evidencias: '+str(out));raise SystemExit(0)

k=ctypes.WinDLL('kernel32',use_last_error=True)
k.GetSystemTimes.argtypes=[ctypes.POINTER(wt.FILETIME)]*3;k.GetSystemTimes.restype=wt.BOOL
def ticks(v):return (v.dwHighDateTime<<32)+v.dwLowDateTime
def system_times():
    idle,ker,user=[wt.FILETIME() for _ in range(3)]
    if not k.GetSystemTimes(ctypes.byref(idle),ctypes.byref(ker),ctypes.byref(user)):raise ctypes.WinError(ctypes.get_last_error())
    return ticks(idle),ticks(ker)+ticks(user)
previous=system_times();time.sleep(1)
expressions={'CPU':'system_cpu_usage{job="transaduanas-backend"} * 100','FIRING':'count(ALERTS{alertname="CPUHostAlta",alertstate="firing"}) or vector(0)'}
timeline=[];raw_graf=[];workers=[];pending_seen=False;firing_seconds=None
t0=time.perf_counter();error='';cleanup_errors=[];fault=None;recovered=None;normal=None
def snapshot(phase):
    global previous,pending_seen,firing_seconds
    current=system_times();delta=current[1]-previous[1]
    native=(1-(current[0]-previous[0])/delta)*100 if delta>0 else None;previous=current
    alerts=[x for x in api('http://127.0.0.1:9090/api/v1/alerts')['data']['alerts'] if x['labels'].get('alertname')=='CPUHostAlta']
    state=alerts[0]['state'] if alerts else 'inactive'
    active_at=alerts[0].get('activeAt') if alerts else None
    seconds=(datetime.now(timezone.utc)-datetime.fromisoformat(active_at.replace('Z','+00:00'))).total_seconds() if active_at else None
    pending_seen=pending_seen or state=='pending'
    if state=='firing' and firing_seconds is None:firing_seconds=seconds
    ts=int(time.time()*1000)-1000
    reply=api('http://127.0.0.1:3000/api/ds/query',{'from':str(ts-300000),'to':str(ts),'queries':[{'refId':ref,'datasource':{'uid':'transaduanas-prometheus','type':'prometheus'},'expr':expr,'instant':True,'range':False,'format':'time_series','intervalMs':5000,'maxDataPoints':100} for ref,expr in expressions.items()]},True)
    values={}
    for ref in expressions:
        samples=[]
        for frame in reply.get('results',{}).get(ref,{}).get('frames',[]):
            for i,field in enumerate(frame.get('schema',{}).get('fields',[])):
                if field.get('type')=='number':samples.extend(frame['data']['values'][i])
        values[ref]=samples[-1] if samples else None
    statuses=[http_status('http://127.0.0.1:'+str(port)+path) for port,path in live]
    row={'Fase':phase,'Segundos':round(time.perf_counter()-t0,3),'CPUWindowsPct':native,'CPUPrometheusPct':number(expressions['CPU'],ts/1000),'CPUGrafanaPct':values['CPU'],
        'EstadoAlerta':state,'ActiveAt':active_at or '','SegundosDesdeActiveAt':seconds,'GrafanaFiring':values['FIRING'],'HTTP200':sum(code==200 for code in statuses),'BackendUP':number('up{job="transaduanas-backend"}')}
    timeline.append(row);raw_graf.append(reply)
    (out/('estado-'+str(len(timeline))+'.json')).write_text(json.dumps({'Resumen':row,'Alertas':alerts},indent=2),encoding='utf-8')
    print(f'M14 {phase}: Windows CPU={native}; Prometheus={row["CPUPrometheusPct"]}%; alerta={state}; activeAt={seconds}; HTTP200={row["HTTP200"]}/4',flush=True)
    return row
def stop_workers():
    for worker_process in workers:
        if worker_process.poll() is None:worker_process.terminate()
    for worker_process in workers:
        try:worker_process.wait(timeout=10)
        except subprocess.TimeoutExpired:worker_process.kill();worker_process.wait();cleanup_errors.append('Un worker requirio cierre forzado.')
def wait_phase(phase,timeout,predicate):
    limit=time.perf_counter()+timeout;bad=0
    while True:
        if (out/'DETENER.txt').exists():raise RuntimeError('Carga cancelada mediante DETENER.txt.')
        row=snapshot(phase)
        bad=bad+1 if row['HTTP200']!=4 or row['BackendUP']!=1 else 0
        if bad>=2:raise RuntimeError('Dos muestras de disponibilidad incorrectas; carga detenida automaticamente.')
        if predicate(row):return row
        if time.perf_counter()>limit:return row
        if phase=='carga-real' and any(process.poll() is not None for process in workers):raise RuntimeError('Un worker termino antes del cierre previsto; se detiene la carga.')
        time.sleep(5)

(out/'criterios-previos.txt').write_text('M14 completa en el host Windows compartido, expresamente autorizada. CPUHostAlta original >0.85 y for=5m sin modificar.\nCPU real generada por workers de baja prioridad y afinidad por CPU logica; no se falsifican metricas.\nWindows GetSystemTimes y Micrometer/Prometheus/Grafana deben observar CPU elevada; comprobar pending y firing tras al menos 300 s de activeAt.\nAl quitar carga, verificar CPU <=85% y alerta inactiva. Tiempo maximo workers 390 s; tambien salen si termina su proceso padre.\nCancelar con DETENER.txt; detener por disponibilidad incorrecta en dos muestras. Baseline y reglas deben permanecer intactas; no se prueban notificaciones externas.\n',encoding='utf-8')
try:
    normal=wait_phase('normal',20,lambda r:r['EstadoAlerta']=='inactive' and r['CPUPrometheusPct'] is not None and r['CPUPrometheusPct']<=85)
    if normal['EstadoAlerta']!='inactive' or normal['CPUPrometheusPct']>85:raise RuntimeError('Ya existe CPU elevada o alerta; no se inicia el estimulo.')
    for cpu in range(cpus):
        workers.append(subprocess.Popen([sys.executable,str(worker),'--cpu',str(cpu),'--seconds','390','--parent',str(os.getpid())],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS))
    (out/'workers.json').write_text(json.dumps([{'PID':process.pid,'CPU':cpu,'Prioridad':'BELOW_NORMAL','MaximoSegundos':390} for cpu,process in enumerate(workers)],indent=2),encoding='utf-8')
    fault=wait_phase('carga-real',370,lambda r:r['EstadoAlerta']=='firing' and r['GrafanaFiring']==1 and r['SegundosDesdeActiveAt']>=300 and r['CPUPrometheusPct']>85 and r['CPUWindowsPct']>85)
except Exception as exc:
    error=type(exc).__name__+': '+str(exc)
finally:
    stop_workers()
    try:recovered=wait_phase('recuperacion',60,lambda r:r['EstadoAlerta']=='inactive' and r['GrafanaFiring']==0 and r['CPUPrometheusPct'] is not None and r['CPUPrometheusPct']<=85)
    except Exception as exc:error=(error+'; ' if error else '')+type(exc).__name__+': '+str(exc)

final=service_snapshot();(out/'servicios-finales.json').write_text(json.dumps(final,indent=2),encoding='utf-8')
checks={'PendingReal':pending_seen,'FiringTras300s':firing_seconds is not None and firing_seconds>=300,'CPUHostRealSuperior85':bool(fault and fault['CPUWindowsPct']>85 and fault['CPUPrometheusPct']>85),
    'GrafanaConfirmaAlerta':bool(fault and fault['EstadoAlerta']=='firing' and fault['GrafanaFiring']==1),
    'RecuperacionReal':bool(recovered and recovered['EstadoAlerta']=='inactive' and recovered['GrafanaFiring']==0 and recovered['CPUPrometheusPct']<=85),
    'ReglaOriginalIntacta':(root/'monitoreo/prometheus/reglas.yml').read_text(encoding='utf-8-sig')==source_rule,
    'WorkersDetenidos':all(process.poll() is not None for process in workers),
    'ServiciosMismosPID':initial['Servicios']==final['Servicios'],
    'RutasDisponibles':all(row['HTTP200']==4 and row['BackendUP']==1 for row in timeline),
    'SinErroresLimpieza':not error and not cleanup_errors}
ok=all(checks.values())
result={'Prueba':14,'CodigoPrueba':'M14','Nombre':'Alerta por saturacion de CPU','Importancia':'Alta','Identificador':marker,'InicioLima':created.isoformat(),'FinLima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),
 'Ejecucion':'REALIZADA: carga CPU real acotada sobre el host Windows compartido, no VM.',
 'Herramientas':['Python / workers con prioridad BELOW_NORMAL','Windows GetSystemTimes','Spring Boot Actuator / Micrometer','Prometheus 3.13.5 / CPUHostAlta','Grafana OSS 13.3.1'],
 'Estado':'EXITOSO' if ok else 'FALLO','Resultado':'Firing despues de '+str(firing_seconds)+' s desde activeAt; recuperacion='+str(recovered['EstadoAlerta'] if recovered else None)+'.',
 'Incidencia':'' if ok else 'Verificaciones incumplidas: '+', '.join(k for k,v in checks.items() if not v),'Verificaciones':checks,
 'Alcance':'Host Windows real compartido; sin aislamiento VM. Regla original >85% y for=5m. No se valida entrega de notificaciones externas.',
 'Cambios':'Sin cambios de codigo, datos o configuracion de monitoreo; workers temporales detenidos.','ErroresLimpieza':cleanup_errors,'ErrorEjecutor':error}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
(out/'grafana-respuestas.json').write_text(json.dumps(raw_graf,indent=2),encoding='utf-8')
with (out/'linea-tiempo.csv').open('w',encoding='utf-8-sig',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=list(timeline[0]));writer.writeheader();writer.writerows(timeline)
print('Estado: '+result['Estado']+'\n'+result['Resultado']+'\nEvidencias: '+str(out),flush=True)
