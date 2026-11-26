#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_lib.sh
source "$SCRIPT_DIR/_lib.sh"

MON_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PROYECTO="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
HERRAMIENTAS="$(mon_default_herramientas)"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --herramientas) HERRAMIENTAS="$2"; shift 2 ;;
    --proyecto) PROYECTO="$2"; shift 2 ;;
    -h|--help)
      echo "Uso: $0 [--herramientas RUTA] [--proyecto RUTA]"
      exit 0
      ;;
    *) mon_fail "Argumento desconocido: $1" ;;
  esac
done

MANIFEST="$HERRAMIENTAS/versiones.json"
PROM_EXE="$(mon_manifest_exe "$MANIFEST" prometheus)" || mon_fail "Falta prometheus en versiones.json."
PROMTOOL="$(dirname "$(readlink -f "$PROM_EXE")")/promtool"
[[ -x "$PROMTOOL" ]] || mon_fail "Falta promtool junto a $PROM_EXE."

CONFIG="$MON_DIR/herramientas/prometheus/prometheus.yml"
EVIDENCIA="$PROYECTO/evidencias-monitoreo/integracion-disponibilidad-M11"
mkdir -p "$EVIDENCIA"

"$PROMTOOL" check config "$CONFIG" >"$EVIDENCIA/validacion-configuracion.txt" 2>&1 \
  || mon_fail "La configuracion Prometheus no paso su validacion."

"$SCRIPT_DIR/iniciar-monitoreo.sh" --herramientas "$HERRAMIENTAS"

PROM_PID="$(python3 - "$HERRAMIENTAS/procesos.json" <<'PY'
import json, sys
for entry in json.load(open(sys.argv[1], encoding="utf-8-sig")):
    if entry.get("nombre") == "prometheus":
        print(entry["processId"])
        sys.exit(0)
sys.exit(1)
PY
)" || mon_fail "No se encontro Prometheus en procesos.json."

mon_stop_verified_pid "prometheus" "$PROM_PID" "$PROM_EXE" "$CONFIG"
for ((i = 0; i < 15; i++)); do
  kill -0 "$PROM_PID" 2>/dev/null || break
  sleep 1
done

"$SCRIPT_DIR/iniciar-monitoreo.sh" --herramientas "$HERRAMIENTAS"
printf 'Aplicados sondeos HTTP con Blackbox; backend y Nginx habituales sin reinicio.\n' >"$EVIDENCIA/aplicacion.txt"
