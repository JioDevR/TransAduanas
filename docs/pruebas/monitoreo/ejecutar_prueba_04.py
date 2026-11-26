from pathlib import Path
from datetime import datetime, timezone, timedelta
from collections import Counter
import argparse
import csv
import json
import math
import re
import shutil
import threading
import time
import urllib.request
import urllib.error
import uuid

parser = argparse.ArgumentParser()
parser.add_argument('--proyecto', default=r'C:\Users\asus\Music\TransAduanas')
args = parser.parse_args()
project = Path(args.proyecto)
tz = timezone(timedelta(hours=-5))
created = datetime.now(tz)
marker = 'M04-' + created.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:6]
out = project / 'evidencias-monitoreo' / marker
out.mkdir(parents=True, exist_ok=False)
backend_log = Path(r'C:\Users\asus\Music\TransAduanas-desplegado\logs\sga-backend.log')
nginx_log = Path(r'C:\Users\asus\Downloads\nginx-1.31.8\nginx-1.31.8\logs\access.log')
for path in (backend_log, nginx_log):
    if not path.is_file():
        raise RuntimeError('Falta el log requerido; no se ejecutara la carga: ' + str(path))

(out / 'criterios-previos.txt').write_text(
    'Consultas: 60 GET normales de lectura, secuenciales, como maximo una solicitud por segundo.\n'
    'Ventana objetivo: 60 s; tolerancia de temporizacion del ejecutor hasta 65 s.\n'
    'Exito: 60 HTTP 200/success=true y 60 request_id validos y distintos.\n'
    'Cada ID debe tener exactamente un evento en Nginx y uno en Java.\n'
    'Tasa media observada en Java: entre 0.95 y 1.05 solicitudes/s en la ventana medida.\n'
    'El contador se obtiene leyendo eventos reales del backend, no incrementandolo al enviar peticiones.\n'
    'Panel de esta prueba: monitor local de consola y serie CSV de conteos; no es Grafana/Prometheus.\n'
    'Trafico ajeno se registra aparte y no se atribuye a esta prueba.\n', encoding='utf-8')
shutil.copy2(Path(__file__), out / 'ejecutar_prueba_04.py')

stop = threading.Event()
ready = threading.Event()
lock = threading.Lock()
observed = []
observer_errors = []
snapshots = []
pattern = re.compile(r'^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} .*'
                     r'\bservice=sga-backend\s+request_id=([a-f0-9]{32}) .*'
                     r'\brequest_completed method=GET endpoint=/api/v1/catalogos/puertos status=(\d{3})\b')

def observe():
    try:
        with backend_log.open('rb') as stream:
            stream.seek(0, 2)
            pending = b''
            ready.set()
            while True:
                pending += stream.read()
                parts = pending.split(b'\n')
                pending = parts.pop()
                for raw in parts:
                    line = raw.decode('utf-8', errors='replace').rstrip('\r')
                    match = pattern.search(line)
                    if match:
                        with lock:
                            observed.append({'RequestId': match.group(1), 'HTTP': int(match.group(2)),
                                             'Fecha': line[:23], 'Linea': line,
                                             'ObservadoMonotonic': time.perf_counter()})
                if stop.is_set():
                    break
                stop.wait(.2)
    except Exception as exc:
        observer_errors.append(type(exc).__name__ + ': ' + str(exc))
        ready.set()

thread = threading.Thread(target=observe, name='lector-logs-backend', daemon=True)
thread.start()
if not ready.wait(5) or observer_errors:
    raise RuntimeError('El observador no pudo iniciar: ' + str(observer_errors))
started = datetime.now(tz)
t0 = time.perf_counter()
previous_send = t0 - 1
next_snapshot = 10
requests = []

def panel_snapshot():
    elapsed = time.perf_counter() - t0
    with lock:
        total = len(observed)
    snapshot = {'Segundos': round(elapsed, 3), 'ContadorEventosBackend': total,
                'RPSPromedio': round(total / elapsed, 4) if elapsed else 0}
    snapshots.append(snapshot)
    line = f'Panel local: t={elapsed:.1f}s; contador backend={total}; tasa media={snapshot["RPSPromedio"]:.3f} req/s'
    with (out / 'panel-en-vivo.txt').open('a', encoding='utf-8') as f:
        f.write(line + '\n')
    print(line, flush=True)

print('M04: observador de logs iniciado; 60 consultas de lectura, sin concurrencia.', flush=True)
try:
    for n in range(1, 61):
        due = max(t0 + n - 1, previous_send + 1)
        while time.perf_counter() < due:
            time.sleep(min(.1, due - time.perf_counter()))
        previous_send = time.perf_counter()
        sent = datetime.now(tz)
        url = f'http://127.0.0.1:5180/api/v1/catalogos/puertos?monitoreo={marker}&consulta={n}'
        status, success, request_id, error = 0, False, '', ''
        req = urllib.request.Request(url, headers={'User-Agent': 'TransAduanas-Prueba-Monitoreo-04'})
        begin = time.perf_counter()
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                status = response.status
                request_id = response.headers.get('X-Request-ID', '')
                success = bool(json.load(response).get('success'))
        except urllib.error.HTTPError as exc:
            status, error = exc.code, str(exc)
        except Exception as exc:
            error = type(exc).__name__ + ': ' + str(exc)
        requests.append({'Consulta': n, 'FechaLima': sent.isoformat(), 'HTTP': status,
                         'SuccessJSON': success, 'RequestId': request_id,
                         'DuracionMs': round((time.perf_counter() - begin) * 1000, 3),
                         'URL': url, 'Error': error})
        if time.perf_counter() - t0 >= next_snapshot:
            panel_snapshot()
            next_snapshot += 10
    finish_at = max(t0 + 60, previous_send + 1)
    while time.perf_counter() < finish_at:
        time.sleep(min(.1, finish_at - time.perf_counter()))
    window = time.perf_counter() - t0
    # Allow the file writer/observer to finish; this grace period is not part of the rate window.
    time.sleep(.5)
finally:
    stop.set()
    thread.join(5)
panel_snapshot()

client_ids = Counter(r['RequestId'] for r in requests if re.fullmatch('[a-f0-9]{32}', r['RequestId']))
with lock:
    all_observed = list(observed)
test_events = [e for e in all_observed if e['RequestId'] in client_ids]
backend_ids = Counter(e['RequestId'] for e in test_events)
other_events = [e for e in all_observed if e['RequestId'] not in client_ids]
access = [line for line in nginx_log.read_text(encoding='utf-8').splitlines() if marker in line]
nginx_ids = Counter()
for line in access:
    match = re.search(r'\brequest_id="([a-f0-9]{32})"', line)
    if match:
        nginx_ids[match.group(1)] += 1

def save_csv(name, rows, columns):
    with (out / name).open('w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)

save_csv('consultas.csv', requests, list(requests[0]))
save_csv('panel-en-vivo.csv', snapshots, ['Segundos', 'ContadorEventosBackend', 'RPSPromedio'])
(out / 'eventos-backend.log').write_text('\n'.join(e['Linea'] for e in test_events) + '\n', encoding='utf-8')
(out / 'eventos-nginx.log').write_text('\n'.join(access) + '\n', encoding='utf-8')
correlation = [{'Consulta': r['Consulta'], 'RequestId': r['RequestId'], 'HTTP': r['HTTP'],
                'EventosNginx': nginx_ids[r['RequestId']], 'EventosBackend': backend_ids[r['RequestId']]}
               for r in requests]
save_csv('conteos-por-solicitud.csv', correlation, ['Consulta', 'RequestId', 'HTTP', 'EventosNginx', 'EventosBackend'])
buckets = []
for i in range(math.ceil(window / 10)):
    low, high = i * 10, min((i + 1) * 10, window)
    count = sum(low <= (datetime.strptime(e['Fecha'], '%Y-%m-%d %H:%M:%S.%f').replace(tzinfo=tz) - started).total_seconds() < high
                for e in test_events)
    buckets.append({'DesdeSegundo': low, 'HastaSegundo': round(high, 3),
                    'EventosBackendDePrueba': count, 'RPSIntervalo': round(count / (high - low), 4)})
save_csv('tasa-por-intervalo.csv', buckets, ['DesdeSegundo', 'HastaSegundo', 'EventosBackendDePrueba', 'RPSIntervalo'])

http_ok = sum(r['HTTP'] == 200 and r['SuccessJSON'] for r in requests)
rate = len(test_events) / window
ok = (len(requests) == 60 and http_ok == 60 and len(client_ids) == 60
      and all(n == 1 for n in client_ids.values()) and len(access) == 60
      and client_ids == backend_ids == nginx_ids and len(test_events) == 60
      and sum(e['HTTP'] == 200 for e in test_events) == 60
      and 60 <= window <= 65 and .95 <= rate <= 1.05
      and not observer_errors and not thread.is_alive())
text = (f'{len(requests)} consultas enviadas; {http_ok} respuestas correctas; '
        f'{len(access)} eventos Nginx y {len(test_events)} eventos Java de la prueba, '
        f'con {len(client_ids)} IDs distintos. Ventana: {window:.3f} s; tasa: {rate:.4f} req/s.')
result = {
    'Prueba': 4, 'CodigoPrueba': 'M04', 'Nombre': 'Conteo de solicitudes', 'Importancia': 'Baja',
    'Identificador': marker, 'InicioLima': started.isoformat(), 'FinLima': datetime.now(tz).isoformat(),
    'Ejecucion': 'REALIZADA: observador independiente de logs Java, 60 GET secuenciales y contraste con Nginx.',
    'Herramientas': ['Python (urllib y lector de logs en hilo independiente)',
                    'Logback del backend', 'Nginx access.log', 'Panel local de consola y series CSV'],
    'Estado': 'EXITOSO' if ok else 'FALLO', 'Resultado': text,
    'Incidencia': '' if ok else 'Al menos un conteo, correlacion, respuesta o criterio de temporizacion no coincide; revisar CSV y errores del observador.',
    'Consultas': len(requests), 'RespuestasCorrectas': http_ok, 'EventosNginx': len(access),
    'EventosBackendDePrueba': len(test_events), 'IDsDistintos': len(client_ids),
    'VentanaSegundos': window, 'RPSBackend': rate, 'EventosAjenosDetectados': len(other_events),
    'ContadorVentanaInicial': 0, 'ContadorVentanaFinal': len(all_observed),
    'ErroresObservador': observer_errors,
    'Alcance': 'Conteo y tasa de una ventana local medidos a partir de eventos reales del backend, mostrados en consola y CSV. No acredita un contador Micrometer ni un panel Grafana/Prometheus instalado.',
    'AdaptacionHerramienta': 'Se eligio un monitor local ligero por ausencia de un stack de metricas; el alcance y los criterios se registraron antes de enviar solicitudes.',
    'Cambios': 'Sin cambios de configuracion, codigo, servicios ni datos de negocio. Consultas publicas de lectura y evidencias.'
}
(out / 'resultado.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
(out / 'resultado.txt').write_text(
    'PRUEBA 04 - CONTEO DE SOLICITUDES\n'
    f'ESTADO: {result["Estado"]}\nRESULTADO: {text}\n'
    f'HERRAMIENTAS: {"; ".join(result["Herramientas"])}\n'
    f'Contador de ventana: 0 -> {len(all_observed)}; eventos ajenos: {len(other_events)}\n'
    f'ALCANCE: {result["Alcance"]}\n'
    'El contador se obtiene de logs del servidor; no se incremento al enviar consultas.\n'
    'Las pruebas 05 a 15 siguen pendientes.\n', encoding='utf-8')
print(f'Estado: {result["Estado"]}\nResultado: {text}\nEvidencias: {out}', flush=True)
