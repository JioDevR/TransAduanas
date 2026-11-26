from pathlib import Path
from datetime import datetime, timezone, timedelta
from collections import Counter
import argparse
import csv
import json
import math
import os
import re
import shutil
import threading
import time
import urllib.request
import urllib.error
import uuid
import base64
import urllib.parse

parser = argparse.ArgumentParser()
parser.add_argument('--proyecto', default=r'C:\Users\asus\Music\TransAduanas')
args = parser.parse_args()
project = Path(args.proyecto)
tz = timezone(timedelta(hours=-5))
created = datetime.now(tz)
marker = 'M06-' + created.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:6]
out = project / 'evidencias-monitoreo' / marker
out.mkdir(parents=True, exist_ok=False)
backend_log = Path(r'C:\Users\asus\Music\TransAduanas-desplegado\logs\sga-backend.log')
nginx_log = Path(r'C:\Users\asus\Downloads\nginx-1.31.8\nginx-1.31.8\logs\access.log')
for path in (backend_log, nginx_log):
    if not path.is_file():
        raise RuntimeError('Falta un log requerido; no se ejecuta la carga: ' + str(path))
shutil.copy2(Path(__file__), out / 'ejecutar_prueba_06_prometheus.py')
(out / 'criterios-previos.txt').write_text(
    'Objetivo: observar p50, p95 y p99 de la lectura autenticada de Clientes.\n'
    'Carga: 60 GET normales, secuenciales y sin concurrencia, como maximo uno por segundo.\n'
    'Las 60 consultas deben responder HTTP 200 y success=true, sin guardar datos de clientes.\n'
    'Cada consulta debe tener exactamente un evento de acceso y uno Java, vinculados por request_id.\n'
    'Duraciones cliente y backend: disponibles, finitas y no negativas.\n'
    'Percentiles: metodo nearest-rank (rango mas cercano), rango ceil(p*n).\n'
    'Deben actualizarse durante la muestra y cumplir p50 <= p95 <= p99 <= maximo.\n'
    'No se exige igualdad cliente/backend: el cliente incluye red, proxy y lectura de respuesta.\n'
    'No se fija un SLA artificial: aprobar significa medicion verificable, no rendimiento optimo.\n'
    'Visualizacion: percentiles nativos del histograma en Prometheus y fuente de Grafana, filtrados a GET Clientes HTTP 200.\n'
    'Criterios nativos: contador aumenta 60, bucket +Inf aumenta 60, sum aumenta y buckets delta son monotonos.\n'
    'p50/p95/p99 nativos deben ser finitos, no negativos y ordenados; Prometheus y Grafana coinciden con tolerancia numerica 1e-6 s en la muestra final sincronizada.\n'
    'Histograma acumulado desde arranque; nearest-rank solo muestra local. No se exige igualdad entre ambos metodos ni se inventa SLA.\n'
    'Scrape cada 5 s, espera final hasta 20 s. Se conserva histograma antes/despues para distinguir la muestra de la historia.\n', encoding='utf-8')


private = json.loads((Path.home()/'Music/TransAduanas-herramientas/monitoreo/credenciales-locales.json').read_text(encoding='utf-8-sig'))
graf_auth = 'Basic ' + base64.b64encode((private['usuario']+':'+private['password']).encode()).decode()
del private
selector = 'job="transaduanas-backend",uri="/api/v1/clientes",method="GET",status="200"'
counter_expr = 'sum(http_server_requests_seconds_count{'+selector+'})'
expressions = {p: 'histogram_quantile('+str(q)+', sum by (le) (http_server_requests_seconds_bucket{'+selector+'}))' for p,q in [('p50',.5),('p95',.95),('p99',.99)]}
def api(url, payload=None, grafana=False):
    headers = {'Authorization':graf_auth} if grafana else {}
    if payload is not None: headers['Content-Type']='application/json'
    req=urllib.request.Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=headers)
    with urllib.request.urlopen(req,timeout=10) as response:return json.load(response)
def prom(expr):
    data=api('http://127.0.0.1:9090/api/v1/query?'+urllib.parse.urlencode({'query':expr}))['data']['result']
    return float(data[0]['value'][1]) if data else 0.0
def native():
    with urllib.request.urlopen('http://127.0.0.1:9086/actuator/prometheus',timeout=10) as response:text=response.read().decode()
    counts={'count':0.0,'sum':0.0};buckets={}
    for line in text.splitlines():
        if not line.startswith('http_server_requests_seconds_') or not all(t in line for t in ['uri="/api/v1/clientes"','method="GET"','status="200"']):continue
        value=float(line.rsplit(' ',1)[1])
        for field in counts:
            if line.startswith('http_server_requests_seconds_'+field+'{'):counts[field]+=value
        if line.startswith('http_server_requests_seconds_bucket{'):
            le=re.search(r'le="([^"]+)"',line).group(1);buckets[le]=buckets.get(le,0)+value
    return counts,buckets,text
native_before,buckets_before,metrics_before=native()
if prom('up{job="transaduanas-backend"}') != 1:raise RuntimeError('Target no disponible: no se inicia la carga.')
for attempt in range(11):
    prom_before=prom(counter_expr)
    if prom_before == native_before['count']:break
    time.sleep(1)
else:raise RuntimeError('Linea base no sincronizada: no se inicia la carga.')
native_snapshots=[];graf_raw=[]
def native_panel():
    now=int(time.time()*1000)
    reply=api('http://127.0.0.1:3000/api/ds/query',{'from':str(now-300000),'to':str(now),'queries':[{'refId':p,'datasource':{'uid':'transaduanas-prometheus','type':'prometheus'},'expr':expr,'instant':True,'range':False,'format':'time_series','intervalMs':5000,'maxDataPoints':100} for p,expr in expressions.items()]},True)
    row={'Segundos':round(time.perf_counter()-t0,3),'ContadorPrometheus':prom(counter_expr)}
    for p,expr in expressions.items():
        values=[]
        for frame in reply.get('results',{}).get(p,{}).get('frames',[]):
            for i,field in enumerate(frame.get('schema',{}).get('fields',[])):
                if field.get('type')=='number':values.extend(frame['data']['values'][i])
        row['Grafana_'+p+'_s']=values[-1] if values else None
        row['Prometheus_'+p+'_s']=prom(expr)
    native_snapshots.append(row);graf_raw.append(reply)
    print('Percentiles nativos Grafana (ms): '+str({p:round(row['Grafana_'+p+'_s']*1000,3) if isinstance(row['Grafana_'+p+'_s'],(int,float)) else None for p in expressions}),flush=True)
    return row

password = os.environ.get('MONITOR_PASSWORD')
username = os.environ.get('MONITOR_USERNAME', 'admin')
if not password:
    raise RuntimeError('Falta credencial de laboratorio en MONITOR_PASSWORD. No se ha ejecutado la prueba.')
print('M06: iniciando una sesion normal; no se guardan credenciales ni token.', flush=True)
body = json.dumps({'username': username, 'password': password}).encode('utf-8')
login_req = urllib.request.Request('http://127.0.0.1:5180/api/v1/auth/login', data=body,
                                   headers={'Content-Type': 'application/json',
                                            'User-Agent': 'TransAduanas-Prueba-Monitoreo-06'}, method='POST')
try:
    with urllib.request.urlopen(login_req, timeout=15) as response:
        login = json.load(response)
    token = login.get('data', {}).get('token')
    if not token:
        raise RuntimeError('La respuesta de autenticacion no incluye token.')
except Exception as exc:
    (out / 'preparacion.txt').write_text(
        'No se pudo iniciar la sesion de laboratorio. No se ejecutaron las consultas de latencia.\n'
        'Tipo de error: ' + type(exc).__name__, encoding='utf-8')
    raise RuntimeError('La autenticacion no pudo completarse; no se registra un falso fallo de latencia.') from None
del password, body, login

def pct(values, fraction):
    if not values:
        return None
    ordered = sorted(values)
    return ordered[max(0, math.ceil(fraction * len(ordered)) - 1)]

def stats(values):
    return {label: round(pct(values, p), 3) if values else None
            for label, p in [('p50', .5), ('p95', .95), ('p99', .99)]}

stop = threading.Event()
ready = threading.Event()
lock = threading.Lock()
observed = []
observer_errors = []
pattern = re.compile(r'^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} .*'
                     r'\bservice=sga-backend\s+request_id=([a-f0-9]{32}) .*'
                     r'\brequest_completed method=GET endpoint=/api/v1/clientes status=(\d{3}) duration_ms=(\d+)\b')

def observe():
    try:
        with backend_log.open('rb') as f:
            f.seek(0, 2)
            pending = b''
            ready.set()
            while True:
                pending += f.read()
                lines = pending.split(b'\n')
                pending = lines.pop()
                for raw in lines:
                    line = raw.decode('utf-8', errors='replace').rstrip('\r')
                    m = pattern.search(line)
                    if m:
                        with lock:
                            observed.append({'RequestId': m.group(1), 'HTTP': int(m.group(2)),
                                             'DuracionMs': int(m.group(3)), 'Linea': line})
                if stop.is_set():
                    break
                stop.wait(.1)
    except Exception as exc:
        observer_errors.append(type(exc).__name__ + ': ' + str(exc))
        ready.set()

thread = threading.Thread(target=observe, name='latencias-logback', daemon=True)
thread.start()
if not ready.wait(5) or observer_errors:
    raise RuntimeError('El lector de logs no pudo iniciar; no se ejecuta la carga.')
started = datetime.now(tz)
t0 = time.perf_counter()
previous_send = t0 - 1
next_snapshot = 10
requests = []
snapshots = []

def panel():
    elapsed = time.perf_counter() - t0
    known_ids = {r['RequestId'] for r in requests}
    with lock:
        server = [e['DuracionMs'] for e in observed if e['RequestId'] in known_ids]
    client = [r['DuracionClienteMs'] for r in requests if r['HTTP'] == 200 and r['SuccessJSON']]
    c, s = stats(client), stats(server)
    row = {'Segundos': round(elapsed, 3), 'MuestrasCliente': len(client), 'MuestrasBackend': len(server),
           **{'Cliente_' + k + '_ms': v for k, v in c.items()},
           **{'Backend_' + k + '_ms': v for k, v in s.items()}}
    snapshots.append(row)
    native_panel()
    message = (f'Panel local: t={elapsed:.1f}s; cliente n={len(client)} p50/p95/p99={c["p50"]}/{c["p95"]}/{c["p99"]} ms; '
               f'backend n={len(server)} p50/p95/p99={s["p50"]}/{s["p95"]}/{s["p99"]} ms')
    with (out / 'panel-en-vivo.txt').open('a', encoding='utf-8') as f:
        f.write(message + '\n')
    print(message, flush=True)

print('M06: 60 lecturas de Clientes, una por segundo y sin concurrencia.', flush=True)
try:
    for n in range(1, 61):
        due = max(t0 + n - 1, previous_send + 1)
        while True:
            remaining = due - time.perf_counter()
            if remaining <= 0:
                break
            time.sleep(min(.1, remaining))
        previous_send = time.perf_counter()
        sent = datetime.now(tz)
        url = f'http://127.0.0.1:5180/api/v1/clientes?soloActivos=true&monitoreo={marker}&consulta={n}'
        status, success, request_id, error, is_list = 0, False, '', '', False
        req = urllib.request.Request(url, headers={'Authorization': 'Bearer ' + token,
                                                   'User-Agent': 'TransAduanas-Prueba-Monitoreo-06'})
        begin = time.perf_counter()
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                status = response.status
                request_id = response.headers.get('X-Request-ID', '')
                data = json.load(response)
                success = bool(data.get('success'))
                is_list = isinstance(data.get('data'), list)
                del data
        except urllib.error.HTTPError as exc:
            status, error = exc.code, str(exc)
        except Exception as exc:
            error = type(exc).__name__ + ': ' + str(exc)
        requests.append({'Consulta': n, 'FechaLima': sent.isoformat(), 'HTTP': status,
                         'SuccessJSON': success and is_list, 'RequestId': request_id,
                         'DuracionClienteMs': round((time.perf_counter() - begin) * 1000, 6), 'Error': error})
        if time.perf_counter() - t0 >= next_snapshot:
            panel()
            next_snapshot += 10
    finish = max(t0 + 60, previous_send + 1)
    while True:
        remaining = finish - time.perf_counter()
        if remaining <= 0:
            break
        time.sleep(min(.1, remaining))
    window = time.perf_counter() - t0
    time.sleep(.5)
finally:
    stop.set()
    thread.join(5)
del token
native_after,buckets_after,metrics_after=native()
for attempt in range(11):
    prom_after=prom(counter_expr)
    if prom_after == native_after['count']:break
    time.sleep(2)
panel()

ids = Counter(r['RequestId'] for r in requests if re.fullmatch('[a-f0-9]{32}', r['RequestId']))
with lock:
    server_events = [e for e in observed if e['RequestId'] in ids]
    foreign = sum(e['RequestId'] not in ids for e in observed)
server_ids = Counter(e['RequestId'] for e in server_events)
access = [line for line in nginx_log.read_text(encoding='utf-8').splitlines() if marker in line]
access_ids = Counter()
for line in access:
    m = re.search(r'\brequest_id="([a-f0-9]{32})"', line)
    if m:
        access_ids[m.group(1)] += 1
client_values = [r['DuracionClienteMs'] for r in requests if r['HTTP'] == 200 and r['SuccessJSON']]
server_values = [e['DuracionMs'] for e in server_events]
c, s = stats(client_values), stats(server_values)
server_by_id = {e['RequestId']: e for e in server_events}
comparison = [{'Consulta': r['Consulta'], 'RequestId': r['RequestId'], 'HTTPCliente': r['HTTP'],
               'DuracionClienteMs': r['DuracionClienteMs'],
               'DuracionBackendMs': server_by_id.get(r['RequestId'], {}).get('DuracionMs', ''),
               'EventosNginx': access_ids[r['RequestId']], 'EventosBackend': server_ids[r['RequestId']]}
              for r in requests]

def save_csv(name, rows, columns):
    with (out / name).open('w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)

save_csv('consultas.csv', requests, list(requests[0]))
save_csv('comparacion-cliente-backend.csv', comparison, list(comparison[0]))
save_csv('panel-percentiles.csv', snapshots, list(snapshots[0]))
save_csv('percentiles.csv', [{'Percentil': p, 'ClienteMs': c[p], 'BackendMs': s[p]} for p in ['p50', 'p95', 'p99']],
         ['Percentil', 'ClienteMs', 'BackendMs'])
(out / 'eventos-nginx.log').write_text('\n'.join(access) + '\n', encoding='utf-8')
(out / 'eventos-backend.log').write_text('\n'.join(e['Linea'] for e in server_events) + '\n', encoding='utf-8')
valid_percentiles = bool(client_values and server_values and
                         0 <= c['p50'] <= c['p95'] <= c['p99'] <= max(client_values) + .001 and
                         0 <= s['p50'] <= s['p95'] <= s['p99'] <= max(server_values) + .001)
ok = (len(requests) == 60 and len(client_values) == 60 and len(server_events) == 60
      and sum(e['HTTP'] == 200 for e in server_events) == 60 and len(access) == 60
      and len(ids) == 60 and all(v == 1 for v in ids.values()) and ids == server_ids == access_ids
      and all(math.isfinite(v) and v >= 0 for v in client_values + server_values)
      and valid_percentiles and len(snapshots) >= 6 and not observer_errors and not thread.is_alive())
last_native=native_snapshots[-1]
delta_buckets={key:value-buckets_before.get(key,0) for key,value in buckets_after.items()}
ordered_buckets=sorted(delta_buckets,key=float)
bucket_values=[delta_buckets[key] for key in ordered_buckets]
native_values=[last_native['Grafana_'+p+'_s'] for p in expressions]
native_checks={
    'IncrementoContadorNativo60':native_after['count']-native_before['count']==60,
    'IncrementoContadorPrometheus60':prom_after-prom_before==60,
    'BucketInfinito60':delta_buckets.get('+Inf')==60,
    'BucketsMonotonos':bool(bucket_values) and all(a<=b for a,b in zip(bucket_values,bucket_values[1:])) and all(v>=0 for v in bucket_values),
    'SumaDuracionesPositiva':native_after['sum']>native_before['sum'],
    'PercentilesValidos':all(isinstance(v,(int,float)) and math.isfinite(v) and v>=0 for v in native_values) and native_values==sorted(native_values) if all(isinstance(v,(int,float)) for v in native_values) else False,
    'GrafanaCoincidePrometheus':all(isinstance(last_native['Grafana_'+p+'_s'],(int,float)) and math.isfinite(last_native['Prometheus_'+p+'_s']) and abs(last_native['Grafana_'+p+'_s']-last_native['Prometheus_'+p+'_s'])<1e-6 for p in expressions)
}
ok = ok and all(native_checks.values())
save_csv('panel-prometheus-grafana.csv',native_snapshots,list(native_snapshots[0]))
save_csv('histograma-delta.csv',[{'LimiteSegundos':key,'ObservacionesMuestra':delta_buckets[key]} for key in ordered_buckets],['LimiteSegundos','ObservacionesMuestra'])
(out/'metricas-antes.prom').write_text(metrics_before,encoding='utf-8')
(out/'metricas-despues.prom').write_text(metrics_after,encoding='utf-8')
(out/'grafana-respuestas.json').write_text(json.dumps(graf_raw,ensure_ascii=False,indent=2),encoding='utf-8')
text = (f'60 consultas previstas; {len(client_values)} respuestas correctas y {len(server_events)} tiempos del backend. '
        f'Cliente p50/p95/p99: {c["p50"]}/{c["p95"]}/{c["p99"]} ms. '
        f'Backend p50/p95/p99: {s["p50"]}/{s["p95"]}/{s["p99"]} ms.')
text += ' Percentiles nativos Grafana p50/p95/p99 (ms): '+str([round(v*1000,3) if isinstance(v,(int,float)) else None for v in native_values])+'.'
result = {
    'Prueba': 6, 'CodigoPrueba': 'M06', 'Nombre': 'Latencia de consultas', 'Importancia': 'Media',
    'Identificador': marker, 'InicioLima': started.isoformat(), 'FinLima': datetime.now(tz).isoformat(),
    'Ejecucion': 'REALIZADA: 60 lecturas autenticadas de Clientes, sin concurrencia, y observador de duraciones Java.',
    'Herramientas': ['Python (urllib, lector de logs y percentiles nearest-rank)',
                    'Logback y filtro HTTP del backend', 'Nginx access.log', 'Spring Boot Actuator / Micrometer', 'Prometheus 3.13.5', 'Grafana OSS 13.3.1'],
    'Estado': 'EXITOSO' if ok else 'FALLO', 'Resultado': text,
    'Incidencia': '' if ok else 'Mediciones, respuestas, correlaciones o percentiles incompletos; revisar las evidencias antes de atribuir la causa.',
    'ValidacionesNativas':native_checks,'ContadorNativoAntes':native_before['count'],'ContadorNativoDespues':native_after['count'],'PercentilesNativosGrafanaMs':{p:round(last_native['Grafana_'+p+'_s']*1000,6) if isinstance(last_native['Grafana_'+p+'_s'],(int,float)) else None for p in expressions},'Consultas': len(requests), 'RespuestasCorrectas': len(client_values), 'EventosBackend': len(server_events),
    'EventosNginx': len(access), 'IDsUnicos': len(ids), 'ActualizacionesPanel': len(snapshots),
    'PercentilesClienteMs': c, 'PercentilesBackendMs': s, 'VentanaSegundos': window,
    'MetodoPercentiles': 'Nearest-rank: valor ordenado en posicion ceil(p*n), contando desde 1.',
    'ResolucionBackend': 'Milisegundos enteros medidos por el filtro HTTP; cliente con reloj monotonic de alta resolucion.',
    'TraficoAjenoDetectado': foreign, 'ErroresObservador': observer_errors,
    'Alcance': 'Latencias de baja carga con histograma Micrometer consultado en Prometheus y fuente de Grafana; percentiles nativos aproximados y acumulados desde arranque, filtrados a GET Clientes HTTP 200. Se conserva histograma antes/despues. Los percentiles locales nearest-rank describen solo las 60 consultas y no tienen que coincidir con la interpolacion del histograma. No acredita un SLA, concurrencia alta ni estabilidad estadistica del p99; con 60 datos p99 corresponde al maximo observado.',
    'Cambios': 'No se crearon ni modificaron clientes. Una autenticacion normal actualiza ultimoAcceso del usuario segun el servicio existente; token y datos personales no se guardan.'
}
(out / 'resultado.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
(out / 'resultado.txt').write_text(
    'PRUEBA 06 - LATENCIA DE CONSULTAS\n'
    f'ESTADO: {result["Estado"]}\nRESULTADO: {text}\n'
    f'IDs unicos: {len(ids)}; eventos Nginx: {len(access)}; actualizaciones del panel: {len(snapshots)}\n'
    f'HERRAMIENTAS: {"; ".join(result["Herramientas"])}\n'
    f'ALCANCE: {result["Alcance"]}\n'
    'Cliente y backend tienen alcances distintos; sus tiempos no tienen que ser iguales.\n'
    'No se definio ni se aplico un umbral artificial para aprobar el rendimiento.\n'
    'La autenticacion puede actualizar ultimoAcceso; no se modificaron clientes.\n'
    'Las pruebas 07 a 15 siguen pendientes.\n', encoding='utf-8')
print(f'Estado: {result["Estado"]}\nResultado: {text}\nEvidencias: {out}', flush=True)
