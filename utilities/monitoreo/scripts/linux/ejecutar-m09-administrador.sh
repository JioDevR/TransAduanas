#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MON_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PROYECTO="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
PYTHON_BIN="${PYTHON_BIN:-python3}"

if [[ -f "$PROYECTO/utilities/pruebas/monitoreo/ejecutar_prueba_09.py" ]]; then
  PRUEBA_SCRIPT="$PROYECTO/utilities/pruebas/monitoreo/ejecutar_prueba_09.py"
else
  PRUEBA_SCRIPT="$PROYECTO/docs/pruebas/monitoreo/ejecutar_prueba_09.py"
fi

SALIDA="$MON_DIR/M09-ejecucion.log"
"$PYTHON_BIN" -u "$PRUEBA_SCRIPT" --proyecto "$PROYECTO" >"$SALIDA" 2>&1
rc=$?
exit "$rc"
