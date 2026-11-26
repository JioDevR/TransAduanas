#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
# shellcheck source=../../monitoreo/scripts/linux/_lib.sh
source "$REPO_ROOT/utilities/monitoreo/scripts/linux/_lib.sh"

CODIGO=""
PROYECTO="$REPO_ROOT"
PYTHON_BIN="${PYTHON_BIN:-python3}"
CARGA_REAL_CPU=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    -c|--codigo) CODIGO="$2"; shift 2 ;;
    -p|--proyecto) PROYECTO="$2"; shift 2 ;;
    --python) PYTHON_BIN="$2"; shift 2 ;;
    --carga-real-cpu) CARGA_REAL_CPU=1; shift ;;
    -h|--help)
      echo "Uso: $0 --codigo DEV|M01..M15 [--proyecto RUTA] [--python BIN] [--carga-real-cpu]"
      exit 0
      ;;
    *) mon_fail "Argumento desconocido: $1" ;;
  esac
done

[[ "$CODIGO" =~ ^(DEV|M(0[1-9]|1[0-5]))$ ]] || mon_fail "Debe indicar --codigo DEV o M01..M15."
if [[ "$CARGA_REAL_CPU" -eq 1 && "$CODIGO" != "M14" ]]; then
  mon_fail "--carga-real-cpu solo corresponde a M14."
fi

declare -A ARCHIVOS=(
  [DEV]="probar-monitoreo-desarrollo.sh"
  [M01]="ejecutar-prueba-01.sh"
  [M02]="ejecutar-prueba-02.sh"
  [M03]="ejecutar_prueba_03.py"
  [M04]="ejecutar_prueba_04_prometheus.py"
  [M05]="ejecutar-prueba-05.sh"
  [M06]="ejecutar_prueba_06_prometheus.py"
  [M07]="ejecutar_prueba_07.py"
  [M08]="ejecutar_prueba_08.py"
  [M09]="ejecutar_prueba_09.py"
  [M10]="ejecutar_prueba_10.py"
  [M11]="ejecutar_prueba_11.py"
  [M12]="ejecutar_prueba_12.py"
  [M13]="ejecutar_prueba_13.py"
  [M14]="ejecutar_prueba_14.py"
  [M15]="ejecutar_prueba_15.py"
)

ARCHIVO="${ARCHIVOS[$CODIGO]}"
if [[ -f "$PROYECTO/utilities/pruebas/monitoreo/$ARCHIVO" ]]; then
  RUTA="$PROYECTO/utilities/pruebas/monitoreo/$ARCHIVO"
elif [[ -f "$PROYECTO/docs/pruebas/monitoreo/$ARCHIVO" ]]; then
  RUTA="$PROYECTO/docs/pruebas/monitoreo/$ARCHIVO"
else
  mon_fail "Falta el ejecutor $ARCHIVO en utilities/pruebas/monitoreo o docs/pruebas/monitoreo."
fi

ANTES_USER_SET="${MONITOR_USERNAME+set}"
ANTES_USER="${MONITOR_USERNAME:-}"
ANTES_PASS_SET="${MONITOR_PASSWORD+set}"
ANTES_PASS="${MONITOR_PASSWORD:-}"

cleanup() {
  if [[ "$ANTES_USER_SET" == "set" ]]; then export MONITOR_USERNAME="$ANTES_USER"; else unset MONITOR_USERNAME || true; fi
  if [[ "$ANTES_PASS_SET" == "set" ]]; then export MONITOR_PASSWORD="$ANTES_PASS"; else unset MONITOR_PASSWORD || true; fi
}
trap cleanup EXIT

if [[ "$CODIGO" == "M06" || "$CODIGO" == "M07" || "$CODIGO" == "M10" ]]; then
  if [[ -z "${MONITOR_USERNAME:-}" || -z "${MONITOR_PASSWORD:-}" ]]; then
    read -r -p "Usuario interno del SGA para esta prueba: " usuario
    [[ -n "${usuario// }" ]] || mon_fail "Se requiere un usuario del SGA."
    read -r -s -p "Contrasena SGA (oculta): " clave
    echo
    export MONITOR_USERNAME="$usuario"
    export MONITOR_PASSWORD="$clave"
    unset clave
  fi
fi

echo "Ejecutando $CODIGO con $ARCHIVO. Una prueba por vez."

if [[ "$RUTA" == *.sh ]]; then
  bash "$RUTA" --proyecto "$PROYECTO"
else
  ARGS=("$RUTA" "--proyecto" "$PROYECTO")
  if [[ "$CODIGO" == "M04" || "$CODIGO" == "M09" ]]; then
    ARGS+=("--endpoint" "/api/v1/catalogos/tipos-carga")
  fi
  if [[ "$CODIGO" == "M14" && "$CARGA_REAL_CPU" -eq 1 ]]; then
    ARGS+=("--autorizar-carga-real-host")
  fi
  set +e
  "$PYTHON_BIN" "${ARGS[@]}"
  rc=$?
  set -e
  if [[ $rc -eq 3 ]]; then
    echo "Prueba no iniciada. Consulte el motivo y la carpeta de evidencias mostrados arriba."
    exit 3
  fi
  if [[ $rc -ne 0 ]]; then
    mon_fail "El ejecutor termino con codigo $rc. Conservar el diagnostico y comprobar resultado.json si existe."
  fi
fi

echo "Revise el Estado y las verificaciones del resultado nuevo; un codigo de proceso 0 no sustituye esa revision."
