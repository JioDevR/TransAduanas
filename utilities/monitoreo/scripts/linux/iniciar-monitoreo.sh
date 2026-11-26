#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_lib.sh
source "$SCRIPT_DIR/_lib.sh"

HERRAMIENTAS="$(mon_default_herramientas)"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --herramientas) HERRAMIENTAS="$2"; shift 2 ;;
    -h|--help)
      echo "Uso: $0 [--herramientas RUTA]"
      exit 0
      ;;
    *) mon_fail "Argumento desconocido: $1" ;;
  esac
done

MANIFEST="$HERRAMIENTAS/versiones.json"
[[ -f "$MANIFEST" ]] || mon_fail "Falta $MANIFEST. Ejecuta instalar_herramientas o configura --herramientas."

PROM_EXE="$(mon_manifest_exe "$MANIFEST" prometheus)" || mon_fail "Falta prometheus en versiones.json."
GRAFANA_EXE="$(mon_manifest_exe "$MANIFEST" grafana)" || mon_fail "Falta grafana en versiones.json."
BLACKBOX_EXE="$(mon_manifest_exe "$MANIFEST" blackbox)" || mon_fail "Falta Blackbox Exporter en versiones.json."

DATOS="$HERRAMIENTAS/datos"
LOGS="$DATOS/logs"
mkdir -p "$LOGS" "$DATOS/prometheus" "$DATOS/grafana"

SECRET_PATH="$HERRAMIENTAS/credenciales-locales.json"
if [[ ! -f "$SECRET_PATH" ]]; then
  python3 - "$SECRET_PATH" <<'PY'
import base64, json, os, secrets, sys, uuid
data = {
    "usuario": "admin",
    "password": uuid.uuid4().hex + uuid.uuid4().hex,
    "secret_key": base64.b64encode(os.urandom(48)).decode("ascii"),
}
path = sys.argv[1]
fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
with os.fdopen(fd, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2)
PY
  chmod 600 "$SECRET_PATH"
fi

read -r GRAFANA_ADMIN_PASS GRAFANA_SECRET_KEY < <(python3 - "$SECRET_PATH" <<'PY'
import json, sys
d = json.load(open(sys.argv[1], encoding="utf-8-sig"))
print(d["password"], d["secret_key"])
PY
)

iniciar_proceso() {
  local nombre="$1" exe="$2" puerto="$3" config="$4"
  shift 4
  [[ -x "$exe" ]] || mon_fail "No existe o no es ejecutable: $exe"

  local resolved_exe owner_pid actual_exe actual_cmd
  resolved_exe="$(readlink -f "$exe")"
  owner_pid="$(mon_port_pid "$puerto")"

  if [[ -n "$owner_pid" ]]; then
    actual_exe="$(mon_proc_exe "$owner_pid")"
    actual_cmd="$(mon_proc_cmdline "$owner_pid")"
    if [[ "$actual_exe" != "$resolved_exe" || "$actual_cmd" != *"$config"* ]]; then
      mon_fail "Puerto $puerto ocupado por otro proceso (PID $owner_pid). No se detuvo."
    fi
    printf '%s\t%s\t%s\t%s\n' "$nombre" "$owner_pid" "$resolved_exe" "$puerto"
    return 0
  fi

  local workdir pid
  workdir="$(dirname "$resolved_exe")"
  (
    cd "$workdir"
    nohup "$resolved_exe" "$@" >"$LOGS/${nombre}-out.log" 2>"$LOGS/${nombre}-error.log" < /dev/null &
    echo $!
  ) | {
    read -r pid
    printf '%s\t%s\t%s\t%s\n' "$nombre" "$pid" "$resolved_exe" "$puerto"
  }
}

MON_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PCONFIG="$MON_DIR/herramientas/prometheus/prometheus.yml"
GCONFIG="$MON_DIR/herramientas/grafana/grafana.ini"
BCONFIG="$MON_DIR/herramientas/blackbox/blackbox.yml"

ENTRIES=()
ENTRIES+=("$(iniciar_proceso blackbox "$BLACKBOX_EXE" 9115 "$BCONFIG" \
  "--config.file=$BCONFIG" "--web.listen-address=127.0.0.1:9115")")
ENTRIES+=("$(iniciar_proceso prometheus "$PROM_EXE" 9090 "$PCONFIG" \
  "--config.file=$PCONFIG" \
  "--storage.tsdb.path=$DATOS/prometheus" \
  "--storage.tsdb.retention.time=2d" \
  "--storage.tsdb.retention.size=256MB" \
  "--web.listen-address=127.0.0.1:9090")")

GRAFANA_HOME="$(dirname "$(dirname "$(readlink -f "$GRAFANA_EXE")")")"
GRAFANA_ARGS=("--homepath=$GRAFANA_HOME" "--config=$GCONFIG")
if [[ "$(basename "$GRAFANA_EXE")" != "grafana-server" ]]; then
  GRAFANA_ARGS=("server" "${GRAFANA_ARGS[@]}")
fi

ENTRIES+=("$(
  export MONITORING_GRAFANA_DATA="$DATOS/grafana"
  export MONITORING_GRAFANA_LOGS="$LOGS"
  export MONITORING_GRAFANA_PROVISIONING="$MON_DIR/herramientas/grafana/provisioning"
  export MONITORING_DASHBOARDS_DIR="$MON_DIR/herramientas/grafana/dashboards"
  export MONITORING_HOME_DASHBOARD="$MON_DIR/herramientas/grafana/dashboards/transaduanas.json"
  export MONITORING_GRAFANA_ADMIN_PASSWORD="$GRAFANA_ADMIN_PASS"
  export MONITORING_GRAFANA_SECRET_KEY="$GRAFANA_SECRET_KEY"
  iniciar_proceso grafana "$GRAFANA_EXE" 3000 "$GCONFIG" "${GRAFANA_ARGS[@]}"
)")

unset GRAFANA_ADMIN_PASS GRAFANA_SECRET_KEY

printf '%s\n' "${ENTRIES[@]}" | python3 - "$HERRAMIENTAS/procesos.json" <<'PY'
import json, sys
items = []
for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    nombre, pid, exe, puerto = line.split("\t")
    items.append({"nombre": nombre, "processId": int(pid), "ejecutable": exe, "puerto": int(puerto)})
with open(sys.argv[1], "w", encoding="utf-8") as f:
    json.dump(items, f, indent=2)
PY

for url in "http://127.0.0.1:9090/-/ready" "http://127.0.0.1:3000/api/health"; do
  mon_wait_http_200 "$url" 45 || mon_fail "No se confirmo el arranque: $url. Consultar logs en $LOGS"
done

echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000/d/transaduanas-monitoreo"
echo "Lectura del panel: acceso local como Viewer. Credencial administrativa privada conservada fuera del proyecto."

"$SCRIPT_DIR/iniciar-logs.sh" --herramientas "$HERRAMIENTAS"
