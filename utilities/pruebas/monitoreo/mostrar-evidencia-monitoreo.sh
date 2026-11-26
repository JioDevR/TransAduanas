#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
# shellcheck source=../../monitoreo/scripts/linux/_lib.sh
source "$REPO_ROOT/utilities/monitoreo/scripts/linux/_lib.sh"

CODIGO=""
PROYECTO="$REPO_ROOT"
ABRIR=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    -c|--codigo) CODIGO="$2"; shift 2 ;;
    -p|--proyecto) PROYECTO="$2"; shift 2 ;;
    --abrir) ABRIR=1; shift ;;
    -h|--help)
      echo "Uso: $0 --codigo DEV|M01..M15 [--proyecto RUTA] [--abrir]"
      exit 0
      ;;
    *) mon_fail "Argumento desconocido: $1" ;;
  esac
done

[[ "$CODIGO" =~ ^(DEV|M(0[1-9]|1[0-5]))$ ]] || mon_fail "Debe indicar --codigo DEV o M01..M15."

EV_DIR="$PROYECTO/evidencias-monitoreo"
[[ -d "$EV_DIR" ]] || mon_fail "No existe $EV_DIR."

CARPETA="$(
  find "$EV_DIR" -maxdepth 1 -type d -name "${CODIGO}-*" 2>/dev/null \
    | sort -r \
    | while read -r dir; do
        if [[ -f "$dir/resultado.json" ]]; then
          printf '%s\n' "$dir"
          break
        fi
      done
)"

[[ -n "$CARPETA" ]] || mon_fail "No hay resultado completo para $CODIGO. Revisar preparacion.txt o los logs del intento."

python3 - "$CODIGO" "$CARPETA/resultado.json" <<'PY'
import json, sys
codigo, path = sys.argv[1], sys.argv[2]
r = json.load(open(path, encoding="utf-8-sig"))
for k, v in [
    ("Codigo", codigo),
    ("Ejecucion", r.get("Identificador", "")),
    ("Fecha", r.get("InicioLima", "")),
    ("Estado", r.get("Estado", "")),
    ("Resultado", r.get("Resultado", "")),
    ("Alcance", r.get("Alcance", "")),
]:
    print(f"{k:10}: {v}")
PY

echo
ls -lh "$CARPETA"
echo
echo "Carpeta: $CARPETA"
echo "Compruebe la fecha: una preparacion incompleta no reemplaza un resultado anterior."

if [[ "$ABRIR" -eq 1 ]] && command -v xdg-open >/dev/null 2>&1; then
  xdg-open "$CARPETA" >/dev/null 2>&1 &
fi
