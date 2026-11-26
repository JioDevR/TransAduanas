#!/usr/bin/env bash
set -euo pipefail

# Prueba funcional integral de monitoreo para MODO DESARROLLO LOCAL.
# Verifica:
#  1. Salud y seguridad del backend local (:8085 negocio + :9086 metricas, MySQL :3306, Vite opcional).
#  2. Envio de 10 peticiones reales y aumento >= 10 en Micrometer (/actuator/prometheus).
#  3. Correlacion X-Request-ID en backend-sga/logs/sga-backend.log.
#  4. Opcional (--con-auth): login real y verificacion de metricas del pool HikariCP (MySQL).
#  5. Opcional (si estan arriba): integracion con Prometheus (:9090), Grafana (:3000) y Loki (:3100).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
# shellcheck source=../../monitoreo/scripts/linux/_lib.sh
source "$REPO_ROOT/utilities/monitoreo/scripts/linux/_lib.sh"

PROYECTO="$REPO_ROOT"
BACKEND_URL="${MON_DEV_BACKEND:-http://127.0.0.1:8085}"
METRICS_URL="${MON_DEV_METRICS:-http://127.0.0.1:9086}"
BASE_URL=""
CON_AUTH=0
AUTH_USER="${MONITOR_USERNAME:-admin}"
AUTH_PASS="${MONITOR_PASSWORD:-Password123!}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --proyecto) PROYECTO="$2"; shift 2 ;;
    --backend-url) BACKEND_URL="$2"; shift 2 ;;
    --metrics-url) METRICS_URL="$2"; shift 2 ;;
    --base-url) BASE_URL="$2"; shift 2 ;;
    --con-auth) CON_AUTH=1; shift ;;
    --usuario) AUTH_USER="$2"; CON_AUTH=1; shift 2 ;;
    --password) AUTH_PASS="$2"; CON_AUTH=1; shift 2 ;;
    -h|--help)
      echo "Uso: $0 [--proyecto RUTA] [--backend-url URL] [--metrics-url URL] [--base-url URL] [--con-auth] [--usuario USER] [--password PASS]"
      exit 0
      ;;
    *) mon_fail "Argumento desconocido: $1" ;;
  esac
done

INICIO="$(mon_lima_iso)"
MARCA="DEV-$(mon_lima_stamp)-$(mon_short_uuid)"
SALIDA="$PROYECTO/evidencias-monitoreo/$MARCA"
mkdir -p "$SALIDA"

LOG_BACKEND="$(mon_detect_backend_log "$PROYECTO" || true)"

python3 - "$MARCA" "$SALIDA" "$BACKEND_URL" "$METRICS_URL" "$BASE_URL" "$LOG_BACKEND" "$CON_AUTH" "$AUTH_USER" "$AUTH_PASS" "$INICIO" <<'PY'
import json, math, re, sys, time, urllib.error, urllib.parse, urllib.request
from datetime import datetime, timezone, timedelta

(marca, salida, backend_url, metrics_url, base_url, log_backend,
 con_auth_s, auth_user, auth_pass, inicio) = sys.argv[1:11]
con_auth = con_auth_s == "1"
tz = timezone(timedelta(hours=-5))

def http_req(url, method="GET", body=None, headers=None, timeout=5):
    hdrs = dict(headers or {})
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        hdrs.setdefault("Content-Type", "application/json")
    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, dict(r.headers), r.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read().decode("utf-8", errors="replace")
    except Exception as e:
        return 0, {}, str(e)

def abort_prereq(motivo):
    res = {
        "Identificador": marca,
        "Modo": "Desarrollo local",
        "InicioLima": inicio,
        "FinLima": datetime.now(tz).isoformat(),
        "Estado": "NO_EJECUTADA",
        "Resultado": motivo,
        "Alcance": "Prueba funcional de monitoreo en modo desarrollo local.",
    }
    with open(f"{salida}/resultado.json", "w", encoding="utf-8") as f:
        json.dump(res, f, indent=2, ensure_ascii=False)
    print(f"NO_EJECUTADA: {motivo}")
    print(f"Evidencias: {salida}")
    sys.exit(3)

# 1. Salud del backend y perfil monitoreo
st_health, _, body_health = http_req(f"{backend_url}/api/v1/health")
if st_health != 200:
    abort_prereq(
        f"El backend local no responde en {backend_url}/api/v1/health (HTTP {st_health}). "
        "Inicia MySQL y ejecuta: cd backend-sga && SPRING_PROFILES_ACTIVE=monitoreo ./mvnw spring-boot:run"
    )

st_m_health, _, _ = http_req(f"{metrics_url}/actuator/health")
st_m_prom, _, prom_before_txt = http_req(f"{metrics_url}/actuator/prometheus")
if st_m_health != 200 or st_m_prom != 200:
    abort_prereq(
        f"El backend responde en {backend_url}, pero no expone metricas en {metrics_url}/actuator/prometheus (HTTP {st_m_prom}). "
        "Arranca el backend con el perfil monitoreo: cd backend-sga && SPRING_PROFILES_ACTIVE=monitoreo ./mvnw spring-boot:run"
    )

# Seguridad de Actuator
st_biz_prom, _, _ = http_req(f"{backend_url}/actuator/prometheus")
st_env, _, _ = http_req(f"{metrics_url}/actuator/env")
seguridad_ok = (st_biz_prom >= 400 or st_biz_prom == 0) and st_env >= 400

# Deteccion de Vite frontend (5180 proxy o directo 8085)
frontends = {}
for name, url in [("app-landing", "http://127.0.0.1:5173/"), ("app-b2b", "http://127.0.0.1:5174/"), ("app-sga", "http://127.0.0.1:5180/")]:
    st_f, _, _ = http_req(url, timeout=2)
    frontends[name] = st_f == 200

if not base_url:
    st_proxy, _, _ = http_req("http://127.0.0.1:5180/api/v1/catalogos/puertos", timeout=2)
    base_url = "http://127.0.0.1:5180" if st_proxy == 200 else backend_url

def contar_puertos_prom(metrics_txt: str) -> float:
    total = 0.0
    for line in metrics_txt.splitlines():
        if (line.startswith("http_server_requests_seconds_count{")
                and 'uri="/api/v1/catalogos/puertos"' in line
                and 'method="GET"' in line
                and 'status="200"' in line):
            total += float(line.rsplit(" ", 1)[1])
    return total

antes = contar_puertos_prom(prom_before_txt)

# 2. Envio de 10 peticiones reales
consultas = []
for i in range(1, 11):
    u = f"{base_url}/api/v1/catalogos/puertos?monitoreo={marca}&consulta={i}"
    st, hdrs, b = http_req(u, headers={"User-Agent": "TransAduanas-Prueba-Dev"}, timeout=10)
    ok_json = False
    try:
        ok_json = bool(json.loads(b).get("success"))
    except Exception:
        pass
    rid = hdrs.get("X-Request-ID") or hdrs.get("x-request-id") or ""
    consultas.append({"Consulta": i, "HTTP": st, "Success": ok_json, "RequestId": rid})

time.sleep(0.5)
_, _, prom_after_txt = http_req(f"{metrics_url}/actuator/prometheus")
despues = contar_puertos_prom(prom_after_txt)
delta = despues - antes
metricas_ok = delta >= 10

# 3. Correlacion en sga-backend.log
logs_verificados = 0
if log_backend:
    try:
        with open(log_backend, encoding="utf-8", errors="replace") as f:
            lines = f.readlines()[-1500:]
        for c in consultas:
            rid = c["RequestId"]
            if rid and any(f"request_id={rid} " in ln and "request_completed " in ln and "service=sga-backend" in ln for ln in lines):
                logs_verificados += 1
    except Exception:
        pass
logs_ok = (logs_verificados == 10) if log_backend else None

# 4. Flujo autenticado + HikariCP (opcional)
auth_res = None
if con_auth:
    st_login, _, body_login = http_req(f"{base_url}/api/v1/auth/login", method="POST", body={"username": auth_user, "password": auth_pass}, timeout=10)
    token = ""
    try:
        parsed = json.loads(body_login)
        token = (parsed.get("data") or {}).get("token") or parsed.get("token") or ""
    except Exception:
        pass
    st_cli = 0
    if st_login == 200 and token:
        st_cli, _, _ = http_req(f"{base_url}/api/v1/clientes", headers={"Authorization": f"Bearer {token}"}, timeout=10)
    _, _, prom_auth_txt = http_req(f"{metrics_url}/actuator/prometheus")
    hikari_present = "hikaricp_connections" in prom_auth_txt
    auth_res = {"LoginHTTP": st_login, "ClientesHTTP": st_cli, "HikariMetricas": hikari_present, "OK": st_login == 200 and st_cli == 200 and hikari_present}

# 5. Stack opcional (Prometheus, Grafana, Loki)
stack = {}
st_p, _, _ = http_req("http://127.0.0.1:9090/-/ready", timeout=2)
if st_p == 200:
    prom_query = None
    expr = 'sum(http_server_requests_seconds_count{job="transaduanas-backend",uri="/api/v1/catalogos/puertos",status="200",method="GET"})'
    for _ in range(7):
        _, _, qbody = http_req("http://127.0.0.1:9090/api/v1/query?" + urllib.parse.urlencode({"query": expr}), timeout=3)
        try:
            res_list = json.loads(qbody)["data"]["result"]
            if res_list:
                prom_query = float(res_list[0]["value"][1])
                if prom_query >= despues:
                    break
        except Exception:
            pass
        time.sleep(2)
    _, _, rbody = http_req("http://127.0.0.1:9090/api/v1/rules", timeout=3)
    reglas = 0
    try:
        for grp in json.loads(rbody)["data"]["groups"]:
            if grp.get("name") == "transaduanas":
                reglas = len(grp.get("rules", []))
    except Exception:
        pass
    stack["prometheus"] = {"activo": True, "serie_valor": prom_query, "sincronizado": prom_query is not None and prom_query >= despues, "reglas_transaduanas": reglas}
else:
    stack["prometheus"] = {"activo": False}

st_g, _, _ = http_req("http://127.0.0.1:3000/api/health", timeout=2)
if st_g == 200:
    st_dash, _, _ = http_req("http://127.0.0.1:3000/api/dashboards/uid/transaduanas-monitoreo", timeout=2)
    stack["grafana"] = {"activo": True, "dashboard_disponible": st_dash == 200}
else:
    stack["grafana"] = {"activo": False}

st_l, _, _ = http_req("http://127.0.0.1:3100/ready", timeout=2)
stack["loki"] = {"activo": st_l == 200}

http_ok = sum(1 for c in consultas if c["HTTP"] == 200 and c["Success"]) == 10
req_ids_ok = len({c["RequestId"] for c in consultas if c["RequestId"]}) == 10

ok = (
    http_ok
    and req_ids_ok
    and metricas_ok
    and seguridad_ok
    and (logs_ok is not False)
    and (auth_res is None or auth_res["OK"])
    and (not stack["prometheus"]["activo"] or stack["prometheus"]["sincronizado"])
)
estado = "EXITOSO" if ok else "FALLO"
razon = (
    f"Monitoreo en modo desarrollo verificado ({base_url}): 10/10 HTTP 200, contador +{int(delta)}, seguridad Actuator OK"
    + (f", {logs_verificados}/10 logs correlacionados." if log_backend else ".")
)

resultado = {
    "CodigoPrueba": "DEV",
    "Identificador": marca,
    "Modo": "Desarrollo local",
    "BackendURL": backend_url,
    "MetricsURL": metrics_url,
    "BaseURL": base_url,
    "InicioLima": inicio,
    "FinLima": datetime.now(tz).isoformat(),
    "FrontendsActivos": frontends,
    "SeguridadActuator": {"NegocioPromHTTP": st_biz_prom, "EnvHTTP": st_env, "OK": seguridad_ok},
    "ConsultasOK": sum(1 for c in consultas if c["HTTP"] == 200 and c["Success"]),
    "RequestIdUnicos": len({c["RequestId"] for c in consultas if c["RequestId"]}),
    "ContadorAntes": antes,
    "ContadorDespues": despues,
    "IncrementoMetricas": delta,
    "LogBackend": log_backend or "No encontrado (opcional si solo corre sin archivo)",
    "LogsCorrelacionados": logs_verificados if log_backend else None,
    "AuthYHikari": auth_res,
    "StackOpcional": stack,
    "Estado": estado,
    "Resultado": razon,
    "Alcance": "Prueba funcional de observabilidad en modo desarrollo local (sin requerir Docker ni Nginx).",
}

with open(f"{salida}/resultado.json", "w", encoding="utf-8") as f:
    json.dump(resultado, f, indent=2, ensure_ascii=False)
with open(f"{salida}/resultado.txt", "w", encoding="utf-8") as f:
    f.write(json.dumps(resultado, indent=2, ensure_ascii=False) + "\n")

print(f"Modo desarrollo ({base_url} | metricas: {metrics_url})")
print(f" - HTTP 200 + success : {resultado['ConsultasOK']}/10 | X-Request-ID unicos: {resultado['RequestIdUnicos']}/10")
print(f" - Metricas Prometheus: antes={int(antes)} despues={int(despues)} (+{int(delta)})")
print(f" - Seguridad Actuator : puerto negocio={st_biz_prom}, /actuator/env={st_env} (OK={seguridad_ok})")
if log_backend:
    print(f" - Logs correlacion   : {logs_verificados}/10 en {log_backend}")
if auth_res is not None:
    print(f" - Auth + HikariCP    : login={auth_res['LoginHTTP']}, clientes={auth_res['ClientesHTTP']}, hikari={auth_res['HikariMetricas']}")
print(f" - Stack opcional     : Prometheus={stack['prometheus']['activo']} | Grafana={stack['grafana']['activo']} | Loki={stack['loki']['activo']}")
print(f"Estado: {estado}")
print(f"Evidencias: {salida}")
if not ok:
    sys.exit(1)
PY

cp "$0" "$SALIDA/probar-monitoreo-desarrollo.sh"
