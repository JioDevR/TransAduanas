#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
# shellcheck source=../../monitoreo/scripts/linux/_lib.sh
source "$REPO_ROOT/utilities/monitoreo/scripts/linux/_lib.sh"

PROYECTO="$REPO_ROOT"
BASE_URL=""
NGINX_DIR=""
LOG_BACKEND=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --proyecto) PROYECTO="$2"; shift 2 ;;
    --base-url) BASE_URL="$2"; shift 2 ;;
    --nginx) NGINX_DIR="$2"; shift 2 ;;
    --log-backend) LOG_BACKEND="$2"; shift 2 ;;
    -h|--help)
      echo "Uso: $0 [--proyecto RUTA] [--base-url URL] [--nginx RUTA_NGINX_O_LOGS] [--log-backend RUTA]"
      exit 0
      ;;
    *) mon_fail "Argumento desconocido: $1" ;;
  esac
done

if [[ -z "$BASE_URL" ]]; then
  BASE_URL="$(mon_detect_api_base)" || mon_fail "Backend no responde en :5180, :8085 ni :8086. Inicia el backend en modo desarrollo."
fi

if [[ -z "$LOG_BACKEND" ]]; then
  LOG_BACKEND="$(mon_detect_backend_log "$PROYECTO" || true)"
fi

ACCESO=""
if [[ -n "$NGINX_DIR" ]]; then
  if [[ -f "$NGINX_DIR/logs/access.log" ]]; then
    ACCESO="$NGINX_DIR/logs/access.log"
  elif [[ -f "$NGINX_DIR/access.log" ]]; then
    ACCESO="$NGINX_DIR/access.log"
  else
    mon_fail "No se encontro access.log en $NGINX_DIR."
  fi
elif [[ -f "/var/log/nginx/access.log" ]]; then
  ACCESO="/var/log/nginx/access.log"
fi

if [[ -z "$LOG_BACKEND" && -z "$ACCESO" ]]; then
  mon_fail "No se encontro sga-backend.log (ej. backend-sga/logs/sga-backend.log) ni Nginx access.log."
fi

MARCA="M01-$(mon_lima_stamp)-$(mon_short_uuid)"
SALIDA="$PROYECTO/evidencias-monitoreo/$MARCA"
mkdir -p "$SALIDA"
INICIO="$(mon_lima_iso)"

python3 - "$MARCA" "$SALIDA" "$BASE_URL" "$ACCESO" "$LOG_BACKEND" "$INICIO" <<'PY'
import csv, json, re, time, urllib.error, urllib.request, sys
from datetime import datetime, timezone, timedelta

marca, salida, base_url, acceso_path, backend_path, inicio = sys.argv[1:7]
tz = timezone(timedelta(hours=-5))
filas = []

for n in range(1, 11):
    uri = f"{base_url}/api/v1/catalogos/puertos?monitoreo={marca}&consulta={n}"
    t0 = time.perf_counter()
    codigo, exito, req_id, err = 0, False, "", ""
    try:
        req = urllib.request.Request(uri, headers={"User-Agent": "TransAduanas-Prueba-Monitoreo-01"})
        with urllib.request.urlopen(req, timeout=10) as r:
            codigo = r.status
            body = json.loads(r.read().decode("utf-8"))
            exito = bool(body.get("success"))
            req_id = r.headers.get("X-Request-ID", "") or ""
    except urllib.error.HTTPError as e:
        codigo = e.code
        err = str(e)
    except Exception as e:
        err = str(e)
    ms = round((time.perf_counter() - t0) * 1000, 2)
    filas.append({
        "Consulta": n,
        "Fecha": datetime.now(tz).isoformat(),
        "URL": uri,
        "HTTP": codigo,
        "SuccessJSON": exito,
        "RequestId": req_id,
        "DuracionMs": ms,
        "Error": err,
    })
    print(f"Consulta {n}/10: HTTP {codigo}; success={exito}; request_id={req_id}")
    if n < 10:
        time.sleep(1)

time.sleep(0.7)

with open(f"{salida}/consultas.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
    w.writeheader()
    w.writerows(filas)

eventos_nginx = []
if acceso_path:
    with open(acceso_path, encoding="utf-8", errors="replace") as f:
        eventos_nginx = [line.rstrip("\r\n") for line in f if marca in line]
    if eventos_nginx:
        with open(f"{salida}/eventos-nginx.log", "w", encoding="utf-8") as f:
            f.write("\n".join(eventos_nginx) + "\n")

eventos_backend = []
backend_por_fecha_endpoint = []
backend_por_servicio = []
if backend_path:
    req_ids = [r["RequestId"] for r in filas if r["RequestId"]]
    fechas_iso = sorted({datetime.fromisoformat(x["Fecha"]).strftime("%Y-%m-%d") for x in filas})
    with open(backend_path, encoding="utf-8", errors="replace") as f:
        for line in f:
            s = line.rstrip("\r\n")
            if any(f"request_id={rid} " in s for rid in req_ids) or marca in s:
                eventos_backend.append(s)
    if eventos_backend:
        with open(f"{salida}/eventos-backend.log", "w", encoding="utf-8") as f:
            f.write("\n".join(eventos_backend) + "\n")
    backend_por_fecha_endpoint = [
        l for l in eventos_backend
        if "endpoint=/api/v1/catalogos/puertos" in l and any(l.startswith(dt) for dt in fechas_iso)
    ]
    backend_por_servicio = [l for l in backend_por_fecha_endpoint if re.search(r"\bservice=sga-backend\b", l)]

http_ok = sum(1 for x in filas if x["HTTP"] == 200 and x["SuccessJSON"])
req_ids_unicos = len({x["RequestId"] for x in filas if x["RequestId"]})

if acceso_path and len(eventos_nginx) == 10:
    fechas = sorted({datetime.fromisoformat(x["Fecha"]).strftime("%d/%b/%Y") for x in filas})
    por_fecha_endpoint = [l for l in eventos_nginx if "GET /api/v1/catalogos/puertos?" in l and any(dt in l for dt in fechas)]
    por_servicio = [l for l in por_fecha_endpoint if re.search(r'\bservice="sga-backend"(?:\s|$)', l, re.I)]
    ordinales = sorted({int(m.group(1)) for l in eventos_nginx if (m := re.search(r"[?&]consulta=(\d+)", l))})
    fuente = "Nginx + Backend" if backend_path else "Nginx"
    aprobado = http_ok == 10 and len(por_fecha_endpoint) == 10 and len(ordinales) == 10 and len(por_servicio) == 10
else:
    fuente = "Backend local (modo desarrollo, sin Nginx)"
    por_fecha_endpoint = backend_por_fecha_endpoint
    por_servicio = backend_por_servicio
    ordinales = list(range(1, req_ids_unicos + 1))
    aprobado = http_ok == 10 and req_ids_unicos == 10 and len(backend_por_fecha_endpoint) == 10 and len(backend_por_servicio) == 10

estado = "EXITOSO" if aprobado else "FALLO"
razon = (
    f"Diez consultas registradas y recuperables por fecha, servicio=sga-backend y endpoint ({fuente})."
    if aprobado else
    f"No se cumplieron todos los criterios en {fuente}. Consultar los conteos y evidencias."
)

resultado = {
    "Prueba": 1,
    "Nombre": "Ingesta y busqueda de logs",
    "Modo": fuente,
    "BaseURL": base_url,
    "Importancia": "Baja",
    "Ejecucion": "REALIZADA: diez GET de lectura, secuenciales, separados por un segundo",
    "InicioLima": inicio,
    "FinLima": datetime.now(tz).isoformat(),
    "Identificador": marca,
    "Herramientas": ["Bash / Python urllib", fuente, "Busqueda por fecha, servicio y X-Request-ID"],
    "Consultas": 10,
    "HTTP200": sum(1 for x in filas if x["HTTP"] == 200),
    "RespuestaNegocioCorrecta": sum(1 for x in filas if x["SuccessJSON"]),
    "EventosNginx": len(eventos_nginx),
    "EventosBackend": len(eventos_backend),
    "OrdinalesORequestIdUnicos": len(ordinales),
    "BusquedaFechaEndpoint": len(por_fecha_endpoint),
    "EventosConCampoServicio": len(por_servicio),
    "Estado": estado,
    "Resultado": razon,
    "Alcance": "Almacenamiento y busqueda local de logs en el proyecto.",
    "Cambios": "Sin cambios de configuracion, codigo, servicios o datos de negocio.",
}
with open(f"{salida}/resultado.json", "w", encoding="utf-8") as f:
    json.dump(resultado, f, indent=2, ensure_ascii=False)

resumen = [
    "PRUEBA 01 - INGESTA Y BUSQUEDA DE LOGS",
    f"Modo: {fuente} ({base_url})",
    f"Inicio Lima: {resultado['InicioLima']}",
    f"Fin Lima: {resultado['FinLima']}",
    f"Identificador: {marca}",
    f"Consultas HTTP 200: {resultado['HTTP200']}/10",
    f"Respuestas JSON success=true: {resultado['RespuestaNegocioCorrecta']}/10",
    f"Eventos backend encontrados: {len(eventos_backend)}/10",
    f"Eventos Nginx encontrados: {len(eventos_nginx)}",
    f"Busqueda por fecha y endpoint: {len(por_fecha_endpoint)}/10",
    f"Eventos con campo servicio: {len(por_servicio)}/10",
    f"ESTADO: {estado}",
    f"RESULTADO: {razon}",
]
with open(f"{salida}/resultado.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(resumen) + "\n")

print(f"Modo: {fuente} ({base_url})")
print(f"Estado: {estado}")
print(f"Resultado: {razon}")
print(f"Evidencias: {salida}")
if not aprobado:
    sys.exit(1)
PY

cp "$0" "$SALIDA/ejecutar-prueba-01.sh"
