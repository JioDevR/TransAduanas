#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_lib.sh
source "$SCRIPT_DIR/_lib.sh"

PROYECTO="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
RAIZ="$(mon_default_desplegado)"
SERVICIO="${MON_SERVICIO:-transaduanas-backend}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --proyecto) PROYECTO="$2"; shift 2 ;;
    --raiz) RAIZ="$2"; shift 2 ;;
    --servicio) SERVICIO="$2"; shift 2 ;;
    -h|--help)
      echo "Uso: $0 [--proyecto RUTA] [--raiz RUTA] [--servicio NOMBRE]"
      exit 0
      ;;
    *) mon_fail "Argumento desconocido: $1" ;;
  esac
done

JAR="$PROYECTO/backend-sga/target/sga-backend-0.5.4.jar"
[[ -f "$JAR" ]] || mon_fail "Falta $JAR."

SALIDA="$(find "$PROYECTO/evidencias-monitoreo" -maxdepth 1 -type d -name 'integracion-prometheus-grafana-*' 2>/dev/null | sort -r | head -n 1 || true)"
if [[ -z "$SALIDA" ]]; then
  SALIDA="$PROYECTO/evidencias-monitoreo/integracion-prometheus-grafana-$(mon_lima_stamp)"
  mkdir -p "$SALIDA"
fi

exec > >(tee -a "$SALIDA/aplicacion.log") 2>&1

read -r old_ver old_hash _ < <(mon_estado_info "$RAIZ/estado.json")
[[ "$old_ver" == "0.5.3" ]] || mon_fail "Se esperaba 0.5.3 antes de aplicar la integracion (actual: $old_ver)."

HASH="$(sha256sum "$JAR" | awk '{print toupper($1)}')"
cp -f "$RAIZ/estado.json" "$SALIDA/estado-antes.json"

on_error() {
  local msg="$1"
  read -r _ act_hash ant_hash < <(mon_estado_info "$RAIZ/estado.json") || true
  if [[ "${act_hash:-}" == "$HASH" && "${ant_hash:-}" == "$old_hash" ]]; then
    mon_revert_jar "$RAIZ" "$SERVICIO" || echo "AVISO: No se confirmo la recuperacion. Revisar servicio y logs." >&2
  fi
  printf 'FALLIDO: %s\n' "$msg" >"$SALIDA/aplicacion-resultado.txt"
  mon_fail "$msg"
}

mon_deploy_jar "$RAIZ" "$JAR" "0.5.4" "$SERVICIO" || on_error "Fallo el despliegue de 0.5.4."

if ! curl -fsS --max-time 10 "http://127.0.0.1:9086/actuator/prometheus" | grep -q 'jvm_memory_used_bytes'; then
  on_error "No se confirmaron metricas reales en el puerto separado."
fi

CODE="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "http://127.0.0.1:5180/api/v1/catalogos/puertos" || true)"
[[ "$CODE" == "200" ]] || on_error "El proxy de negocio no responde (HTTP $CODE)."

CUR_HASH="$(sha256sum "$RAIZ/current/app.jar" | awk '{print toupper($1)}')"
[[ "$CUR_HASH" == "$HASH" ]] || on_error "El JAR activo no coincide."

cp -f "$RAIZ/estado.json" "$SALIDA/estado-despues.json"
printf 'CORRECTO: 0.5.4 activa; metricas JVM reales en 127.0.0.1:9086; negocio HTTP 200; anterior 0.5.3 conservada. SHA256=%s\n' "$HASH" >"$SALIDA/aplicacion-resultado.txt"
cat "$SALIDA/aplicacion-resultado.txt"
