from pathlib import Path
from datetime import datetime, timezone, timedelta
from collections import Counter
import argparse
import base64
import csv
import json
import re
import shutil
import time
import urllib.request
import urllib.error
import urllib.parse

parser = argparse.ArgumentParser()
parser.add_argument('--proyecto', default=r'C:\Users\asus\Music\TransAduanas')
parser.add_argument('--endpoint', default='/api/v1/catalogos/puertos', choices=['/api/v1/catalogos/puertos','/api/v1/catalogos/tipos-carga'])
args = parser.parse_args()

# El mutex vive en Windows y se libera incluso si el proceso termina inesperadamente.
# Evita que dos M04 mezclen sus 60 consultas en el mismo contador global.
import atexit
import ctypes
from ctypes import wintypes
_m04_kernel = ctypes.WinDLL('kernel32', use_last_error=True)
_m04_kernel.CreateMutexW.argtypes = [ctypes.c_void_p, wintypes.BOOL, wintypes.LPCWSTR]
_m04_kernel.CreateMutexW.restype = wintypes.HANDLE
_m04_kernel.WaitForSingleObject.argtypes = [wintypes.HANDLE, wintypes.DWORD]
_m04_kernel.WaitForSingleObject.restype = wintypes.DWORD
_m04_kernel.ReleaseMutex.argtypes = [wintypes.HANDLE]
_m04_kernel.CloseHandle.argtypes = [wintypes.HANDLE]
_m04_handle = _m04_kernel.CreateMutexW(None, False, 'Global\\TransAduanasM04Medicion')
if not _m04_handle:
    raise OSError(ctypes.get_last_error(), 'No se pudo crear el bloqueo de M04.')
_m04_wait = _m04_kernel.WaitForSingleObject(_m04_handle, 0)
if _m04_wait not in (0, 0x80):
    _m04_kernel.CloseHandle(_m04_handle)
    raise SystemExit('M04 no iniciada: ya hay otra medicion en curso o no pudo adquirirse el bloqueo. Espere a que termine y repita una sola vez.')
def _m04_release():
    _m04_kernel.ReleaseMutex(_m04_handle)
    _m04_kernel.CloseHandle(_m04_handle)
atexit.register(_m04_release)
endpoint = args.endpoint
root = Path(args.proyecto)
tz = timezone(timedelta(hours=-5))
created = datetime.now(tz)
marker = 'M04-' + created.strftime('%Y%m%d-%H%M%S') + '-' + __import__('uuid').uuid4().hex[:6]
out = root / 'evidencias-monitoreo' / marker
out.mkdir(parents=True, exist_ok=False)
shutil.copy2(Path(__file__), out / 'ejecutar_prueba_04_prometheus.py')
# Prerrequisitos: una herramienta detenida no se presenta como fallo del backend.
for _nombre, _url in [
    ('Prometheus', 'http://127.0.0.1:9090/-/ready'),
    ('Grafana', 'http://127.0.0.1:3000/api/health'),
    ('Nginx', 'http://127.0.0.1:5180/'),
    ('Metricas del backend', 'http://127.0.0.1:9086/actuator/prometheus'),
]:
    try:
        with urllib.request.urlopen(_url, timeout=10) as _reply:
            if _reply.status != 200:
                raise RuntimeError('Respuesta HTTP distinta de 200')
            _reply.read()
    except (OSError, RuntimeError) as _exc:
        _reason = f'M04 no ejecutada: {_nombre} no esta disponible en {_url}. Inicie Nginx y el monitoreo antes de repetir; no se inicio la carga.'
        (out / 'resultado.json').write_text(json.dumps({
            'CodigoPrueba': 'M04', 'Estado': 'NO_EJECUTADA', 'Identificador': marker,
            'Resultado': _reason, 'Fase': 'Prerrequisitos', 'SolicitudesMedidas': 0,
            'TipoError': type(_exc).__name__, 'InicioLima': created.isoformat(),
        }, ensure_ascii=False, indent=2), encoding='utf-8')
        (out / 'resultado.txt').write_text(_reason + '\n', encoding='utf-8')
        raise SystemExit(_reason)

tools = Path.home() / 'Music/TransAduanas-herramientas/monitoreo'
private = json.loads((tools / 'credenciales-locales.json').read_text(encoding='utf-8-sig'))
auth = 'Basic ' + base64.b64encode((private['usuario'] + ':' + private['password']).encode()).decode()
del private
backend_log = Path.home() / 'Music/TransAduanas-desplegado/logs/sga-backend.log'
access_log = Path.home() / 'Downloads/nginx-1.31.8/nginx-1.31.8/logs/access.log'
selector = f'job="transaduanas-backend",uri="{endpoint}",method="GET",status="200"'
counter_expr = 'sum(http_server_requests_seconds_count{' + selector + '})'
rate_expr = 'sum(rate(http_server_requests_seconds_count{' + selector + '}[30s]))'

def api(url, payload=None, grafana=False):
    headers = {'Authorization': auth} if grafana else {}
    if payload is not None:
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(url, data=json.dumps(payload).encode() if payload is not None else None, headers=headers)
    with urllib.request.urlopen(req, timeout=10) as r:
        return json.load(r)

def raw_metrics():
    with urllib.request.urlopen('http://127.0.0.1:9086/actuator/prometheus', timeout=10) as r:
        text = r.read().decode()
    count = sum(float(line.rsplit(' ', 1)[1]) for line in text.splitlines()
                if line.startswith('http_server_requests_seconds_count{') and
                f'uri="{endpoint}"' in line and 'method="GET"' in line and 'status="200"' in line)
    return count, text

def prom(expr):
    reply = api('http://127.0.0.1:9090/api/v1/query?' + urllib.parse.urlencode({'query': expr}))
    values = reply['data']['result']
    return float(values[0]['value'][1]) if values else None

def grafana_values():
    now = int(time.time() * 1000)
    reply = api('http://127.0.0.1:3000/api/ds/query', {
        'from': str(now - 300000), 'to': str(now),
        'queries': [{'refId': ref, 'datasource': {'uid': 'transaduanas-prometheus', 'type': 'prometheus'},
                     'expr': expr, 'instant': True, 'range': False, 'format': 'time_series',
                     'intervalMs': 5000, 'maxDataPoints': 100}
                    for ref, expr in [('CONTADOR', counter_expr), ('RPS30S', rate_expr)]]
    }, grafana=True)
    result = {}
    for ref, item in reply.get('results', {}).items():
        values = []
        for frame in item.get('frames', []):
            fields = frame.get('schema', {}).get('fields', [])
            arrays = frame.get('data', {}).get('values', [])
            for index, field in enumerate(fields):
                if field.get('type') == 'number' and index < len(arrays):
                    values += [v for v in arrays[index] if isinstance(v, (int, float))]
        result[ref] = values[-1] if values else None
    return result, reply

(out / 'criterios-previos.txt').write_text(
    f'60 GET normales a {endpoint} durante una ventana objetivo de 60 s, sin concurrencia.\n'
    'Ventana medida aceptada entre 60 y 65 s, tolerancia del ejecutor; no es un SLA.\n'
    '60 HTTP 200/success=true y 60 identificadores distintos, con un evento por ID en cada log.\n'
    'El contador nativo Micrometer y el recolectado por Prometheus/Grafana deben aumentar en 60.\n'
    'Se controla trafico ajeno por endpoint; un incremento extra exige explicacion, no se atribuye automaticamente al proyecto.\n'
    'Prometheus tiene scrape_interval=5 s; esperar hasta 20 s para la muestra final.\n'
    'La tasa se consulta con rate(...[30s]); verificar 0.85 a 1.15 req/s en una muestra durante la carga estabilizada (35-58 s).\n'
    'El dashboard general usa una ventana de 5 minutos y todas las rutas: sus valores absolutos pueden diferir.\n'
    'No se usa increase(...[1m]) como contador exacto porque incluye extrapolacion.\n', encoding='utf-8')

# Preparacion fuera de las 60 consultas medidas. Tras reiniciar Java la serie
# HTTP de esta ruta puede no existir hasta recibir su primera consulta.
preparation = {'ConsultaPreparacion': False, 'Endpoint': endpoint, 'Intentos': []}

def baseline_not_ready(reason):
    preparation['Motivo'] = reason
    (out / 'preparacion-linea-base.json').write_text(
        json.dumps(preparation, ensure_ascii=False, indent=2), encoding='utf-8')
    message = 'M04 no ejecutada: ' + reason + ' No se iniciaron las 60 consultas medidas.'
    (out / 'resultado.json').write_text(json.dumps({
        'CodigoPrueba': 'M04', 'Estado': 'NO_EJECUTADA', 'Identificador': marker,
        'Resultado': message, 'Fase': 'Preparacion de linea base',
        'SolicitudesMedidas': 0, 'InicioLima': created.isoformat(),
    }, ensure_ascii=False, indent=2), encoding='utf-8')
    (out / 'resultado.txt').write_text(message + '\n', encoding='utf-8')
    raise SystemExit(message)

native_before, text_before = raw_metrics()
has_counter = any(
    line.startswith('http_server_requests_seconds_count{') and
    f'uri="{endpoint}"' in line and 'method="GET"' in line and 'status="200"' in line
    for line in text_before.splitlines())
if not has_counter:
    preparation['ConsultaPreparacion'] = True
    preparation['IdentificadorConsulta'] = 'PREPARACION-M04-' + __import__('uuid').uuid4().hex
    try:
        prep_url = 'http://127.0.0.1:5180' + endpoint + '?' + urllib.parse.urlencode({
            'monitoreo': preparation['IdentificadorConsulta'], 'consulta': 0})
        prep_request = urllib.request.Request(prep_url, headers={
            'User-Agent': 'TransAduanas-M04-Preparacion'})
        with urllib.request.urlopen(prep_request, timeout=10) as reply:
            preparation['HTTPPreparacion'] = reply.status
            if reply.status != 200 or json.load(reply).get('success') is not True:
                baseline_not_ready('La consulta de preparacion no respondio correctamente.')
    except (OSError, ValueError) as exc:
        baseline_not_ready('No se pudo preparar el contador: ' + type(exc).__name__ + '.')
    print('M04: una consulta de preparacion para crear el contador; no forma parte de las 60 medidas.', flush=True)

print('M04: esperando la misma linea base en backend, Prometheus y Grafana (hasta 30 s).', flush=True)
deadline = time.monotonic() + 30
stable_baselines = 0
last_baseline = None
while True:
    attempt = {}
    try:
        native_before, text_before = raw_metrics()
        prom_before = prom(counter_expr)
        graf_before, graf_reply_before = grafana_values()
        target_up = prom('up{job="transaduanas-backend"}')
        attempt = {'Backend': native_before, 'Prometheus': prom_before,
                   'Grafana': graf_before.get('CONTADOR'), 'TargetUP': target_up}
        aligned = (target_up == 1 and prom_before is not None and
                   prom_before == native_before == graf_before.get('CONTADOR'))
        stable_baselines = stable_baselines + 1 if aligned and last_baseline == native_before else (1 if aligned else 0)
        last_baseline = native_before
    except (OSError, ValueError, KeyError) as exc:
        attempt['Error'] = type(exc).__name__
        stable_baselines = 0
    preparation['Intentos'].append(attempt)
    if stable_baselines >= 2:
        break
    if time.monotonic() >= deadline:
        baseline_not_ready('La recoleccion no se sincronizo; revisar preparacion-linea-base.json y el target de Prometheus.')
    time.sleep(1)
preparation['Estado'] = 'LISTA'
(out / 'preparacion-linea-base.json').write_text(
    json.dumps(preparation, ensure_ascii=False, indent=2), encoding='utf-8')
initial_offset = backend_log.stat().st_size
start = datetime.now(tz)
t0 = time.perf_counter()
last_send = t0 - 1
next_panel = 15
requests = []
snapshots = []
graf_replies = []
print(f'M04 nativa: contador inicial {native_before:.0f}; 60 lecturas a una por segundo.', flush=True)

def snapshot():
    elapsed = time.perf_counter() - t0
    values, reply = grafana_values()
    pcount = prom(counter_expr)
    row = {'Segundos': round(elapsed, 3), 'ContadorPrometheus': pcount,
           'ContadorGrafana': values.get('CONTADOR'), 'RPS30sGrafana': values.get('RPS30S')}
    snapshots.append(row)
    graf_replies.append({'Segundos': row['Segundos'], 'Respuesta': reply})
    print(f'Prometheus/Grafana: t={elapsed:.1f}s; contador={pcount}/{values.get("CONTADOR")}; RPS(30s)={values.get("RPS30S")}', flush=True)

for n in range(1, 61):
    due = max(t0 + n - 1, last_send + 1)
    while True:
        remaining = due - time.perf_counter()
        if remaining <= 0:
            break
        time.sleep(min(.1, remaining))
    last_send = time.perf_counter()
    status, success, request_id, error = 0, False, '', ''
    url = f'http://127.0.0.1:5180{endpoint}?monitoreo={marker}&consulta={n}'
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'TransAduanas-M04-Prometheus-Grafana'})
        with urllib.request.urlopen(req, timeout=5) as response:
            status = response.status
            request_id = response.headers.get('X-Request-ID', '')
            success = bool(json.load(response).get('success'))
    except urllib.error.HTTPError as exc:
        status, error = exc.code, str(exc)
    except Exception as exc:
        error = type(exc).__name__ + ': ' + str(exc)
    requests.append({'Consulta': n, 'HTTP': status, 'SuccessJSON': success,
                     'RequestId': request_id, 'SegundosEnvio': round(last_send-t0, 6), 'Error': error})
    if time.perf_counter() - t0 >= next_panel:
        snapshot()
        next_panel += 15
finish_at = max(t0 + 60, last_send + 1)
while True:
    remaining = finish_at - time.perf_counter()
    if remaining <= 0:
        break
    time.sleep(min(.1, remaining))
window = time.perf_counter() - t0
native_after, text_after = raw_metrics()
for _ in range(10):
    prom_after = prom(counter_expr)
    if prom_after == native_after:
        break
    time.sleep(2)
snapshot()
graf_after, graf_reply_after = grafana_values()
ids = Counter(r['RequestId'] for r in requests if re.fullmatch('[a-f0-9]{32}', r['RequestId']))
nginx_lines = [line for line in access_log.read_text(encoding='utf-8').splitlines() if marker in line]
nginx_ids = Counter()
for line in nginx_lines:
    m = re.search(r'\brequest_id="([a-f0-9]{32})"', line)
    if m:
        nginx_ids[m.group(1)] += 1
with backend_log.open('rb') as f:
    f.seek(initial_offset)
    new_lines = f.read().decode('utf-8', errors='replace').splitlines()
backend_lines = []
backend_ids = Counter()
foreign = 0
for line in new_lines:
    if f'request_completed method=GET endpoint={endpoint} status=200 ' not in line:
        continue
    m = re.search(r'\brequest_id=([a-f0-9]{32})\b', line)
    if m and m.group(1) in ids:
        backend_ids[m.group(1)] += 1
        backend_lines.append(line)
    else:
        foreign += 1
stable_rates = [r['RPS30sGrafana'] for r in snapshots if 35 <= r['Segundos'] <= 58 and r['RPS30sGrafana'] is not None]
rate_ok = bool(stable_rates) and all(.85 <= r <= 1.15 for r in stable_rates)
http_ok = sum(r['HTTP'] == 200 and r['SuccessJSON'] for r in requests)
delta_native = native_after-native_before
delta_prom = prom_after-prom_before
delta_graf = graf_after['CONTADOR']-graf_before['CONTADOR'] if graf_after.get('CONTADOR') is not None else None
ok = (len(requests) == 60 and http_ok == 60 and len(ids) == 60 and all(v == 1 for v in ids.values())
      and len(nginx_lines) == 60 and ids == nginx_ids == backend_ids
      and delta_native == delta_prom == delta_graf == 60 and foreign == 0
      and 60 <= window <= 65 and rate_ok and prom('up{job="transaduanas-backend"}') == 1)

def save_csv(name, rows, fields):
    with (out/name).open('w', encoding='utf-8-sig', newline='') as f:
        writer=csv.DictWriter(f, fieldnames=fields)
        writer.writeheader();writer.writerows(rows)

save_csv('consultas.csv',requests,list(requests[0]))
save_csv('panel-prometheus-grafana.csv',snapshots,list(snapshots[0]))
save_csv('conteos-por-solicitud.csv',
         [{'Consulta':r['Consulta'],'RequestId':r['RequestId'],'Nginx':nginx_ids[r['RequestId']],'Java':backend_ids[r['RequestId']]} for r in requests],
         ['Consulta','RequestId','Nginx','Java'])
(out/'metricas-antes.prom').write_text(text_before,encoding='utf-8')
(out/'metricas-despues.prom').write_text(text_after,encoding='utf-8')
(out/'eventos-nginx.log').write_text('\n'.join(nginx_lines)+'\n',encoding='utf-8')
(out/'eventos-backend.log').write_text('\n'.join(backend_lines)+'\n',encoding='utf-8')
(out/'grafana-respuestas.json').write_text(json.dumps({'Antes':graf_reply_before,'Durante':graf_replies,'Despues':graf_reply_after},ensure_ascii=False,indent=2),encoding='utf-8')
result_text=(f'{http_ok}/60 consultas correctas; contador del catalogo {native_before:.0f} -> {native_after:.0f}. '
             f'Incrementos: Micrometer={delta_native:.0f}, Prometheus={delta_prom:.0f}, Grafana={delta_graf}. '
             f'Tasa Grafana/Prometheus en ventana de 30s durante carga estable: {stable_rates}. '
             f'Ventana de envio: {window:.3f}s; 60 eventos en cada log: {len(backend_lines)}/{len(nginx_lines)}.')
result={
    'Prueba':4,'CodigoPrueba':'M04','Nombre':'Conteo de solicitudes','Importancia':'Baja','Identificador':marker,
    'InicioLima':start.isoformat(),'FinLima':datetime.now(tz).isoformat(),
    'Ejecucion':'REALIZADA: 60 lecturas secuenciales; contadores nativos Micrometer, Prometheus y fuente de Grafana.',
    'Herramientas':['Spring Boot Actuator / Micrometer','Prometheus 3.13.5','Grafana OSS 13.3.1',
                    'Nginx y Logback para contraste','Script de apoyo en Python (HTTP y validacion)'],
    'Estado':'EXITOSO' if ok else 'FALLO','Resultado':result_text,
    'Incidencia':'' if ok else 'No se cumplieron todos los criterios de conteo o tasa. Revisar diferencia de contadores, scrape, trafico ajeno y CSV antes de atribuir la causa.',
    'HTTP200Correctos':http_ok,'ContadorNativoAntes':native_before,'ContadorNativoDespues':native_after,
    'ContadorPrometheusAntes':prom_before,'ContadorPrometheusDespues':prom_after,
    'ContadorGrafanaAntes':graf_before['CONTADOR'],'ContadorGrafanaDespues':graf_after['CONTADOR'],
    'IncrementoMicrometer':delta_native,'IncrementoPrometheus':delta_prom,'IncrementoGrafana':delta_graf,
    'RPSDuranteCargaGrafana':stable_rates,'TasaRPSComprobada':rate_ok,'VentanaSegundos':window,
    'TraficoAjeno':foreign,'EventosJava':len(backend_lines),'EventosNginx':len(nginx_lines),
    'ConsultaContador':counter_expr,'ConsultaTasa':rate_expr,'EndpointPrueba':endpoint,
    'Alcance':f'Conteo nativo del endpoint GET {endpoint}. Tasa PromQL en 30s durante carga estable; dashboard general usa todas las rutas y 5m. Scrape cada 5s; los tiempos de actualizacion no son instantaneos.',
    'Cambios':'Sin cambios de configuracion, servicios ni datos de negocio. Se conservan las ejecuciones anteriores de M04.'
}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
(out/'resultado.txt').write_text('PRUEBA 04 - PROMETHEUS Y GRAFANA\nESTADO: '+result['Estado']+'\nRESULTADO: '+result_text+
    '\nALCANCE: '+result['Alcance']+'\nSe conserva M04 local. M06 no se ha repetido en esta ejecucion; M07-M15 siguen pendientes.\n',encoding='utf-8')
print('Estado: '+result['Estado']+'\nResultado: '+result_text+'\nEvidencias: '+str(out),flush=True)
