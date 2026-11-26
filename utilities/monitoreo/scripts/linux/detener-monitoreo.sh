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

STATE="$HERRAMIENTAS/procesos.json"
[[ -f "$STATE" ]] || mon_fail "No hay registro de procesos del stack ($STATE)."

while IFS=$'\t' read -r nombre pid exe; do
  mon_stop_verified_pid "$nombre" "$pid" "$exe"
done < <(python3 - "$STATE" <<'PY'
import json, sys
for entry in json.load(open(sys.argv[1], encoding="utf-8-sig")):
    print(f"{entry['nombre']}\t{entry['processId']}\t{entry['ejecutable']}")
PY
)

echo "El backend y sus datos no fueron detenidos ni eliminados."

"$SCRIPT_DIR/detener-logs.sh" --herramientas "$HERRAMIENTAS"
