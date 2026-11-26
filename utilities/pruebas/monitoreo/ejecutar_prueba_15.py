from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,base64,csv,json,msvcrt,re,shutil,subprocess,threading,time,urllib.request,urllib.parse,uuid

p=argparse.ArgumentParser();p.add_argument('--proyecto',default=r'C:\Users\asus\Music\TransAduanas');a=p.parse_args();root=Path(a.proyecto)
lock=(root/'monitoreo/M15-ejecucion.lock').open('a+b');lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
except OSError:raise RuntimeError('M15 ya esta en curso.') from None
created=datetime.now(timezone(timedelta(hours=-5)));marker='M15-'+created.strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6]
out=root/'evidencias-monitoreo'/marker;out.mkdir(parents=True,exist_ok=False);shutil.copy2(__file__,out/Path(__file__).name)
tools=Path.home()/'Music/TransAduanas-herramientas/monitoreo'
backend=Path.home()/'Music/TransAduanas-desplegado/logs/sga-backend.log'
original_size=backend.stat().st_size
private=json.loads((tools/'credenciales-locales.json').read_text(encoding='utf-8-sig'));auth='Basic '+base64.b64encode((private['usuario']+':'+private['password']).encode()).decode();del private
def api(url,payload=None,grafana=False):
    headers={'Authorization':auth} if grafana else {}
    if payload is not None:headers['Content-Type']='application/json'
    for attempt in range(2):
        try:
            with urllib.request.urlopen(urllib.request.Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=headers),timeout=12) as r:return json.load(r)
        except (TimeoutError,urllib.error.URLError):
            if attempt:raise
            time.sleep(.5)
def query(expr,when=None):
    params={'query':expr}
    if when is not None:params['time']=when
    rows=api('http://127.0.0.1:9090/api/v1/query?'+urllib.parse.urlencode(params))['data']['result']
    return float(rows[0]['value'][1]) if rows else None
def receiver():
    for attempt in range(2):
        try:
            with urllib.request.urlopen('http://127.0.0.1:3100/metrics',timeout=10) as r:text=r.read().decode()
            break
        except (TimeoutError,urllib.error.URLError):
            if attempt:raise
            time.sleep(.5)
    return sum(float(line.rsplit(' ',1)[1]) for line in text.splitlines() if line.startswith('loki_distributor_lines_received_total{'))
def ps_json(command):
    r=subprocess.run(['powershell.exe','-NoProfile','-Command',command],capture_output=True,text=True,check=True)
    return json.loads(r.stdout.strip().lstrip('\ufeff'))
service_command="@(Get-CimInstance Win32_Service -Filter \"Name='MySQL84' OR Name='TransAduanasBackend'\" | Select-Object Name,State,ProcessId) | ConvertTo-Json -Compress"
initial_services=ps_json(service_command);(out/'servicios-iniciales.json').write_text(json.dumps(initial_services,indent=2),encoding='utf-8')
(out/'criterios-previos.txt').write_text(
 'M15: recoleccion real de archivos Java y Nginx con Grafana Alloy y recepcion/consulta Loki.\n'
 'Generar un GET normal de catalogo cada cinco segundos, sin concurrencia, durante normal, pausa y recuperacion.\n'
 'Normal: eventos buscables en Loki, contador de lineas crece y alerta IngestaLogsInterrumpida inactiva.\n'
 'Interrumpir solo el proceso Alloy identificado por ejecutable, PID y config. No detener Loki, backend, MySQL o Nginx.\n'
 'Durante la pausa, consultas HTTP 200 y archivo backend deben seguir creciendo; contador Loki permanece sin cambios por al menos 120 s.\n'
 'La regla usa increase(loki_distributor_lines_received_total[2m]) == 0 con backend y Loki UP; for=5s, scrape/evaluacion5s. No se sustituye por la alerta de up del recolector.\n'
 'Observar IngestaLogsInterrumpida en firing en Prometheus y Grafana; plazo maximo220s.\n'
 'Restaurar Alloy con su mismo storage de posiciones; ingesta vuelve a crecer y alerta queda inactiva, maximo90s.\n'
 'Los requestId generados durante la pausa deben poder buscarse en Loki tras recuperar, junto con request_completed nativo. Se exige al menos una ocurrencia por ID; registrar duplicados posibles sin confundirlos con perdida.\n'
 'Logs usan timestamp de ingesta en Loki; el timestamp original permanece dentro de la linea. La comprobacion de pausa y alerta es con contador del receptor, no con fecha de los eventos originales.\n'
 'Las reglas, datos originales y servicios habituales permanecen activos; dejar Alloy recuperado y conservar evidencias. No se prueban notificaciones externas.\n',encoding='utf-8')
requests=[];timeline=[];graf_raw=[];stop=threading.Event();phase='normal';generator_errors=[]
t0=time.perf_counter();last_counter=receiver();last_change=t0
def generate():
    n=0;due=time.perf_counter()
    while not stop.is_set():
        if stop.wait(max(0,due-time.perf_counter())):break
        n+=1;row={'Consulta':n,'Fase':phase,'Segundos':round(time.perf_counter()-t0,3),'HTTP':0,'SuccessJSON':False,'RequestId':''}
        try:
            with urllib.request.urlopen(urllib.request.Request('http://127.0.0.1:5180/api/v1/catalogos/puertos?monitoreo='+marker+'&consulta='+str(n),headers={'User-Agent':'TransAduanas-M15-Ingesta'}),timeout=6) as r:
                row['HTTP']=r.status;row['RequestId']=r.headers.get('X-Request-ID','');row['SuccessJSON']=json.load(r).get('success') is True
        except Exception as exc:generator_errors.append(type(exc).__name__)
        requests.append(row)
        with (out/'consultas.jsonl').open('a',encoding='utf-8') as f:f.write(json.dumps(row)+'\n')
        due=max(due+5,time.perf_counter())
thread=threading.Thread(target=generate,daemon=True);thread.start()
expressions={'LINEAS':'sum(loki_distributor_lines_received_total{job="transaduanas-loki"})','FIRING':'count(ALERTS{alertname="IngestaLogsInterrumpida",alertstate="firing"}) or vector(0)'}
def snapshot(name):
    global last_counter,last_change
    count=receiver();now=time.perf_counter()
    if count!=last_counter:last_change=now;last_counter=count
    alerts=[x for x in api('http://127.0.0.1:9090/api/v1/alerts')['data']['alerts'] if x['labels'].get('alertname')=='IngestaLogsInterrumpida']
    ts=int(time.time()*1000)-1000
    reply=api('http://127.0.0.1:3000/api/ds/query',{'from':str(ts-300000),'to':str(ts),'queries':[{'refId':ref,'datasource':{'uid':'transaduanas-prometheus','type':'prometheus'},'expr':expr,'instant':True,'range':False,'format':'time_series','intervalMs':5000,'maxDataPoints':100} for ref,expr in expressions.items()]},True)
    values={}
    for ref in expressions:
        samples=[]
        for frame in reply.get('results',{}).get(ref,{}).get('frames',[]):
            for i,field in enumerate(frame.get('schema',{}).get('fields',[])):
                if field.get('type')=='number':samples.extend(frame['data']['values'][i])
        values[ref]=samples[-1] if samples else None
    row={'Fase':name,'Segundos':round(now-t0,3),'LineasLokiNativas':count,'SegundosSinCambioContador':round(now-last_change,3),
        'EstadoAlerta':alerts[0]['state'] if alerts else 'inactive','GrafanaFiring':values['FIRING'],'GrafanaLineas':values['LINEAS'],
        'BackendUP':query('up{job="transaduanas-backend"}'),'LokiUP':query('up{job="transaduanas-loki"}'),'AlloyUP':query('up{job="transaduanas-alloy"}'),
        'BytesLogBackend':backend.stat().st_size,'ConsultasGeneradas':len(requests)}
    timeline.append(row);graf_raw.append(reply)
    (out/('estado-'+str(len(timeline))+'.json')).write_text(json.dumps({'Resumen':row,'Alertas':alerts},indent=2),encoding='utf-8')
    print(f'M15 {name}: lineas receptor={count}; sin cambio={row["SegundosSinCambioContador"]}s; alerta={row["EstadoAlerta"]}; Grafana firing={row["GrafanaFiring"]}; HTTP200={sum(r["HTTP"]==200 for r in requests)}/{len(requests)}',flush=True)
    return row
def wait_phase(name,timeout,predicate):
    until=time.perf_counter()+timeout
    while True:
        row=snapshot(name)
        if predicate(row):return row
        if time.perf_counter()>until:return row
        time.sleep(5)
def restart():
    state_path=tools/'procesos-logs.json'
    entries=json.loads(state_path.read_text(encoding='utf-8-sig'))
    entry=next(x for x in entries if x['nombre']=='alloy')
    # Launch the persistent collector directly with file handles, not inherited capture pipes.
    env=__import__('os').environ.copy();env.update({'GOMEMLIMIT':'96MiB','GOGC':'60'})
    stdout=(out/'alloy-recuperacion-out.log').open('wb');stderr=(out/'alloy-recuperacion-error.log').open('wb')
    process=subprocess.Popen([entry['ejecutable'],'run',entry['config'],'--server.http.listen-addr=127.0.0.1:12345','--storage.path='+str(tools/'datos/alloy'),'--disable-reporting'],
        cwd=Path(entry['ejecutable']).parent,env=env,stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)
    stdout.close();stderr.close()
    entry['processId']=process.pid
    state_path.write_text(json.dumps(entries,indent=2),encoding='utf-8')
    until=time.perf_counter()+40
    while True:
        try:
            with urllib.request.urlopen('http://127.0.0.1:12345/-/ready',timeout=2) as response:
                if response.status==200:break
        except Exception:pass
        if process.poll() is not None or time.perf_counter()>until:raise RuntimeError('No se confirmo el inicio directo de Alloy.')
        time.sleep(.5)
    (out/'recuperacion-alloy.txt').write_text('Alloy recuperado mediante Popen con salida a archivo y storage de posiciones conservado. PID='+str(process.pid),encoding='utf-8')
def loki_logs(expr):
    params={'query':expr,'start':str(int((created.timestamp()-30)*1e9)),'end':str(int(time.time()*1e9)),'limit':5000,'direction':'forward'}
    data=api('http://127.0.0.1:3100/loki/api/v1/query_range?'+urllib.parse.urlencode(params))
    return [line for stream in data['data']['result'] for _,line in stream['values']]

normal=fault=recovered=None;interrupted=False;error='';cleanup_errors=[];loki_lines=[];loki_graf_reply={}
try:
    initial_count=last_counter
    normal=wait_phase('normal',40,lambda r:r['LineasLokiNativas']>initial_count and r['EstadoAlerta']=='inactive' and r['AlloyUP']==r['BackendUP']==r['LokiUP']==1 and len(requests)>=5)
    sample_id=next((r['RequestId'] for r in requests if r['HTTP']==200),'')
    if not sample_id or not loki_logs('{job="transaduanas-logs",service_name="sga-backend"} |= "request_completed" |= "'+sample_id+'"'):raise RuntimeError('No se confirmo busqueda inicial de evento; no se interrumpe Alloy.')
    entries=json.loads((tools/'procesos-logs.json').read_text(encoding='utf-8-sig'));entry=next(x for x in entries if x['nombre']=='alloy')
    verify=ps_json('Get-CimInstance Win32_Process -Filter "ProcessId='+str(entry['processId'])+'" | Select-Object ProcessId,ExecutablePath,CommandLine | ConvertTo-Json -Compress')
    if verify['ExecutablePath']!=entry['ejecutable'] or entry['config'] not in verify['CommandLine']:raise RuntimeError('No coincide el proceso Alloy; no se interrumpe.')
    phase='recolector-interrumpido';size_at_pause=backend.stat().st_size
    subprocess.run(['powershell.exe','-NoProfile','-Command','Stop-Process -Id '+str(entry['processId'])+' -ErrorAction Stop'],capture_output=True,check=True)
    interrupted=True;last_change=time.perf_counter();last_counter=receiver()
    fault=wait_phase(phase,220,lambda r:r['SegundosSinCambioContador']>=120 and r['EstadoAlerta']=='firing' and r['GrafanaFiring']==1 and r['BackendUP']==r['LokiUP']==1 and r['AlloyUP']==0)
    phase='recuperacion';restart();interrupted=False
    recovered=wait_phase(phase,90,lambda r:r['LineasLokiNativas']>fault['LineasLokiNativas'] and r['EstadoAlerta']=='inactive' and r['GrafanaFiring']==0 and r['AlloyUP']==1)
    stop.set();thread.join(10)
    ids=[r['RequestId'] for r in requests if re.fullmatch('[a-f0-9]{32}',r['RequestId'])]
    expr='{job="transaduanas-logs",service_name="sga-backend"} |= "request_completed" |~ "'+'|'.join(ids)+'"'
    until=time.perf_counter()+30
    while True:
        loki_lines=loki_logs(expr)
        if all(any(rid in line for line in loki_lines) for rid in ids) or time.perf_counter()>until:break
        time.sleep(2)
    ts=int(time.time()*1000)
    loki_graf_reply=api('http://127.0.0.1:3000/api/ds/query',{'from':str(int((created.timestamp()-30)*1000)),'to':str(ts),
        'queries':[{'refId':'LOGS','datasource':{'uid':'transaduanas-loki','type':'loki'},'expr':expr,'queryType':'range','maxLines':5000,'direction':'forward'}]},True)
except Exception as exc:error=type(exc).__name__+': '+str(exc)
finally:
    stop.set();thread.join(10)
    if interrupted:
        try:restart()
        except Exception as exc:cleanup_errors.append(type(exc).__name__)

final_services=ps_json(service_command);(out/'servicios-finales.json').write_text(json.dumps(final_services,indent=2),encoding='utf-8')
new_lines=backend.read_text(encoding='utf-8',errors='replace').splitlines()
verification=[]
for row in requests:
    rid=row['RequestId'];native=sum(bool(rid) and 'request_id='+rid in line and 'request_completed ' in line for line in new_lines)
    remote=sum(bool(rid) and rid in line and 'request_completed ' in line for line in loki_lines)
    verification.append({'Consulta':row['Consulta'],'Fase':row['Fase'],'RequestId':rid,'HTTP':row['HTTP'],'EventosHTTPNativos':native,'EventosHTTPenLoki':remote})
graf_lines=[]
for frame in loki_graf_reply.get('results',{}).get('LOGS',{}).get('frames',[]):
    for i,field in enumerate(frame.get('schema',{}).get('fields',[])):
        if field.get('name','').lower()=='line':graf_lines.extend(frame['data']['values'][i])
paused=[row for row in verification if row['Fase']=='recolector-interrumpido']
checks={'NormalIngestaYBusqueda':bool(normal and normal['EstadoAlerta']=='inactive' and normal['AlloyUP']==1),
    'SinIngestaDurante120s':bool(fault and fault['SegundosSinCambioContador']>=120),
    'AplicacionSigueGenerandoLogs':bool(fault and fault['BytesLogBackend']>size_at_pause and len(paused)>0),
    'AlertaPrometheusYGrafana':bool(fault and fault['EstadoAlerta']=='firing' and fault['GrafanaFiring']==1),
    'RecuperacionIngestaYAlerta':bool(recovered and recovered['EstadoAlerta']=='inactive' and recovered['GrafanaFiring']==0 and recovered['AlloyUP']==1),
    'EventosDePausaRecuperados':bool(paused) and all(row['EventosHTTPNativos']>=1 and row['EventosHTTPenLoki']>=1 for row in paused),
    'TodasConsultasCorrectas':bool(requests) and all(row['HTTP']==200 and row['SuccessJSON'] for row in requests),
    'ProductorPeriodicoSinHuecos':all(b['Segundos']-a['Segundos']<=15 for a,b in zip(requests,requests[1:])),
    'TodosEventosBuscables':bool(verification) and all(row['EventosHTTPenLoki']>=1 for row in verification),
    'ConsultaLogsDesdeGrafana':bool(graf_lines) and any('request_completed' in line for line in graf_lines),
    'ServiciosMismosPID':{s['Name']:(s['State'],s['ProcessId']) for s in initial_services}=={s['Name']:(s['State'],s['ProcessId']) for s in final_services},
    'BackendYLokiDisponibles':bool(timeline) and all(row['BackendUP']==row['LokiUP']==1 for row in timeline),
    'SinErroresEjecutor':not error and not cleanup_errors and not generator_errors and not thread.is_alive()}
ok=all(checks.values())
summary=(f'{len(requests)} consultas normales; {len(paused)} durante la pausa. Sin cambios del contador receptor durante '+str(fault['SegundosSinCambioContador'] if fault else None)+
    ' s. Alerta IngestaLogsInterrumpida firing y cierre='+str(recovered['EstadoAlerta'] if recovered else None)+
    f'. Eventos de pausa recuperados en Loki: {sum(r["EventosHTTPenLoki"]>=1 for r in paused)}/{len(paused)}; consulta Grafana con {len(graf_lines)} lineas.')
result={'Prueba':15,'CodigoPrueba':'M15','Nombre':'Interrupcion de ingesta de logs','Importancia':'Alta','Identificador':marker,'InicioLima':created.isoformat(),'FinLima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),
    'Ejecucion':'REALIZADA: interrupcion del proceso Alloy mientras continuaban eventos reales de lectura; receptor Loki y aplicacion permanecieron activos.',
    'Herramientas':['Grafana Alloy 1.22.0','Grafana Loki 3.7.10','Prometheus 3.13.5','Grafana OSS 13.3.1','Logback y Nginx','Python / HTTP, requestId y busqueda'],
    'Estado':'EXITOSO' if ok else 'FALLO','Resultado':summary,'Incidencia':'' if ok else 'Verificaciones incumplidas: '+', '.join(k for k,v in checks.items() if not v),
    'Verificaciones':checks,'ErrorEjecutor':error,'ErroresLimpieza':cleanup_errors,'ErroresGenerador':generator_errors,
    'Consultas':len(requests),'EventosDurantePausa':len(paused),'EventosPausaRecuperados':sum(r['EventosHTTPenLoki']>=1 for r in paused),
    'EventosConDuplicadosLoki':sum(r['EventosHTTPenLoki']>1 for r in verification),
    'Alcance':'Ingesta real de archivos locales Java/Nginx en Loki mediante Alloy. La alerta requiere dos minutos sin lineas recibidas y backend/Loki UP, con sondeos que mantienen eventos periodicos. La fecha de Loki es de ingesta; la fecha original esta dentro del mensaje. Se verifica recuperacion al menos una vez por requestId, no una garantia universal de entrega exactamente una vez, ni retencion transcurrida de 48h o notificaciones externas.',
    'Cambios':'Se integraron Loki, Alloy, reglas de ingesta/salud y datasource/paneles de logs. Solo se interrumpio Alloy y quedo recuperado; backend, MySQL, Nginx y Loki no se detuvieron durante la prueba. Sin cambios de datos de negocio; evidencias y posiciones conservadas fuera/dentro del proyecto segun corresponda.'}
for name,data in [('resultado.json',result),('grafana-metricas.json',graf_raw),('grafana-logs.json',loki_graf_reply),('consultas-http.json',requests)]:
    (out/name).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
for name,rows in [('linea-tiempo.csv',timeline),('correlacion-eventos.csv',verification)]:
    with (out/name).open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(rows[0]) if rows else ['Fase']);writer.writeheader();writer.writerows(rows)
(out/'eventos-recuperados-loki.log').write_text('\n'.join(loki_lines)+'\n',encoding='utf-8')
(out/'resultado.txt').write_text('M15 - INTERRUPCION INGESTA LOGS\nESTADO: '+result['Estado']+'\n'+summary+'\n'+result['Alcance']+'\n',encoding='utf-8')
print('Estado: '+result['Estado']+'\n'+summary+'\nEvidencias: '+str(out),flush=True)
