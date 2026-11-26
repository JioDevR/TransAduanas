#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
# shellcheck source=../../monitoreo/scripts/linux/_lib.sh
source "$REPO_ROOT/utilities/monitoreo/scripts/linux/_lib.sh"

PROYECTO="$REPO_ROOT"
BASE_URL=""
NGINX_DIR=""
LOG_JAVA=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --proyecto) PROYECTO="$2"; shift 2 ;;
    --base-url) BASE_URL="$2"; shift 2 ;;
    --nginx) NGINX_DIR="$2"; shift 2 ;;
    --log-backend) LOG_JAVA="$2"; shift 2 ;;
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

if [[ -z "$LOG_JAVA" ]]; then
  LOG_JAVA="$(mon_detect_backend_log "$PROYECTO")" || mon_fail "No se encontro sga-backend.log (ej. backend-sga/logs/sga-backend.log)."
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

INICIO="$(mon_lima_iso)"
MARCA="M02-$(mon_lima_stamp)-$(mon_short_uuid)"
SALIDA="$PROYECTO/evidencias-monitoreo/$MARCA"
mkdir -p "$SALIDA"

python3 - "$MARCA" "$SALIDA" "$BASE_URL" "$ACCESO" "$LOG_JAVA" "$INICIO" <<'PY'
import csv, json, re, time, urllib.error, urllib.request, sys
from collections import deque
from datetime import datetime, timezone, timedelta

marca, salida, base_url, acceso_path, java_path, inicio = sys.argv[1:7]
tz = timezone(timedelta(hours=-5))

criterios = [
    "Consultas normales: tres GET de lectura con HTTP 200 y success=true.",
    "Eventos HTTP de Java: fecha, nivel nativo de Logback (INFO), servicio (sga-backend) e identificador de solicitud (request_id).",
    "El X-Request-ID de la respuesta HTTP debe coincidir con el registro en sga-backend.log (y con Nginx si esta presente).",
    "No se exige request_id a eventos de arranque o tareas sin una solicitud HTTP asociada.",
]
with open(f"{salida}/criterios-previos.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(criterios) + "\n")

consultas = []
for n in range(1, 4):
    uri = f"{base_url}/api/v1/catalogos/puertos?monitoreo={marca}&consulta={n}"
    codigo, success, req_id, err = 0, False, "", ""
    try:
        req = urllib.request.Request(uri, headers={"User-Agent": "TransAduanas-Prueba-Monitoreo-02"})
        with urllib.request.urlopen(req, timeout=10) as r:
            codigo = r.status
            body = json.loads(r.read().decode("utf-8"))
            success = bool(body.get("success"))
            req_id = r.headers.get("X-Request-ID", "") or ""
    except urllib.error.HTTPError as e:
        codigo = e.code
        err = str(e)
    except Exception as e:
        err = str(e)
    consultas.append({
        "Consulta": n,
        "Fecha": datetime.now(tz).isoformat(),
        "HTTP": codigo,
        "SuccessJSON": success,
        "RequestIdRespuesta": req_id,
        "URL": uri,
        "Error": err,
    })
    print(f"Consulta {n}/3: HTTP {codigo}; success={success}; request_id={req_id}")
    if n < 3:
        time.sleep(1)

time.sleep(0.7)

with open(f"{salida}/consultas.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(consultas[0].keys()))
    w.writeheader()
    w.writerows(consultas)

eventos_nginx = []
if acceso_path:
    with open(acceso_path, encoding="utf-8", errors="replace") as f:
        eventos_nginx = [l.rstrip("\r\n") for l in f if marca in l]
    if eventos_nginx:
        with open(f"{salida}/eventos-nginx.log", "w", encoding="utf-8") as f:
            f.write("\n".join(eventos_nginx) + "\n")
con_nginx = bool(acceso_path and len(eventos_nginx) == 3)

with open(java_path, encoding="utf-8", errors="replace") as f:
    java_tail = list(deque((l.rstrip("\r\n") for l in f), maxlen=1000))

verificacion = []
java_de_prueba = []

for c in consultas:
    rid = c["RequestIdRespuesta"]
    acceso_m, backend_m = [], []
    if re.fullmatch(r"[a-f0-9]{32}", rid):
        if con_nginx:
            acceso_m = [l for l in eventos_nginx if f'request_id="{rid}"' in l]
        backend_m = [l for l in java_tail if f"request_id={rid} " in l and "request_completed " in l]

    ln = "\n".join(acceso_m)
    lj = "\n".join(backend_m)
    fecha_n = bool(re.search(r"\[\d{2}/[A-Za-z]{3}/\d{4}:\d{2}:\d{2}:\d{2} [+-]\d{4}\]", ln))
    fecha_j = bool(re.match(r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} ", lj))
    m_lvl = re.search(r"\s(TRACE|DEBUG|INFO|WARN|ERROR)\s+service=", lj)
    nivel = m_lvl.group(1) if m_lvl else ""

    ok_n = (
        (not con_nginx)
        or (len(acceso_m) == 1 and fecha_n and bool(re.search(r'\bservice="sga-backend"', ln)) and '" 200 ' in ln)
    )
    ok_j = (
        len(backend_m) == 1
        and fecha_j
        and nivel == "INFO"
        and bool(re.search(r"\bservice=sga-backend\b", lj))
        and "endpoint=/api/v1/catalogos/puertos status=200 " in lj
    )
    verificacion.append({
        "Consulta": c["Consulta"],
        "RequestId": rid,
        "LogNginxValido": ok_n if con_nginx else "N/A (modo desarrollo)",
        "EventosJava": len(backend_m),
        "FechaJavaValida": fecha_j,
        "NivelJava": nivel,
        "LogJavaValido": ok_j,
        "CorrelacionCorrecta": ok_n and ok_j,
    })
    java_de_prueba.extend(backend_m)

with open(f"{salida}/eventos-backend.log", "w", encoding="utf-8") as f:
    f.write("\n".join(java_de_prueba) + ("\n" if java_de_prueba else ""))

with open(f"{salida}/campos-verificados.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(verificacion[0].keys()))
    w.writeheader()
    w.writerows(verificacion)

correctas = sum(1 for v in verificacion if v["CorrelacionCorrecta"])
unicos = len({c["RequestIdRespuesta"] for c in consultas if c["RequestIdRespuesta"]})
http_ok = sum(1 for c in consultas if c["HTTP"] == 200 and c["SuccessJSON"])
ok = http_ok == 3 and correctas == 3 and unicos == 3
estado = "EXITOSO" if ok else "FALLO"
modo = "Nginx + Backend" if con_nginx else "Backend local (modo desarrollo)"
razon = (
    f"Tres consultas normales HTTP 200. Tres eventos Java con fecha, nivel INFO nativo, servicio y request_id coincidentes ({modo})."
    if ok else
    "No se cumplieron todos los criterios de estructura y correlacion. Ver campos-verificados.csv."
)

resultado = {
    "Prueba": 2,
    "CodigoPrueba": "M02",
    "Modo": modo,
    "BaseURL": base_url,
    "Nombre": "Estructura de los registros",
    "Importancia": "Baja",
    "Identificador": marca,
    "InicioLima": inicio,
    "FinLima": datetime.now(tz).isoformat(),
    "Ejecucion": "REALIZADA: tres GET normales de lectura, secuenciales",
    "Herramientas": ["Bash / Python urllib", "Logback sga-backend.log", "MDC y filtro HTTP de Spring"],
    "Consultas": 3,
    "HTTP200": http_ok,
    "EventosNginx": len(eventos_nginx),
    "EventosJava": len(java_de_prueba),
    "CorrelacionesCorrectas": correctas,
    "RequestIdUnicos": unicos,
    "Estado": estado,
    "Resultado": razon,
    "Criterios": criterios,
    "Alcance": "Estructura y correlacion de eventos HTTP en logs locales.",
}
with open(f"{salida}/resultado.json", "w", encoding="utf-8") as f:
    json.dump(resultado, f, indent=2, ensure_ascii=False)

resumen = [
    "PRUEBA 02 - ESTRUCTURA DE LOS REGISTROS",
    f"Modo: {modo} ({base_url})",
    f"ESTADO: {estado}",
    f"RESULTADO: {razon}",
    f"Consultas HTTP 200: {http_ok}/3",
    f"Eventos Java: {len(java_de_prueba)}/3",
    f"Correlaciones correctas: {correctas}/3",
    f"IDs unicos: {unicos}/3",
]
with open(f"{salida}/resultado.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(resumen) + "\n")

print(f"Modo: {modo} ({base_url})")
print(f"Estado: {estado}")
print(f"Resultado: {razon}")
print(f"Evidencias: {salida}")
if not ok:
    sys.exit(1)
PY

cp "$0" "$SALIDA/ejecutar-prueba-02.sh"
