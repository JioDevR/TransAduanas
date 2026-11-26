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

JAR="$PROYECTO/backend-sga/target/sga-backend-0.5.5.jar"
[[ -f "$JAR" ]] || mon_fail "Falta $JAR."

SALIDA="$PROYECTO/evidencias-monitoreo/correccion-M07-0.5.5"
mkdir -p "$SALIDA"

if [[ "$(id -u)" -ne 0 ]] && command -v systemctl >/dev/null 2>&1 && systemctl list-unit-files "${SERVICIO}.service" >/dev/null 2>&1; then
  mon_fail "Se requiere root (sudo) para actualizar el servicio systemd $SERVICIO."
fi

exec > >(tee -a "$SALIDA/aplicacion.log") 2>&1

read -r old_ver old_hash _ < <(mon_estado_info "$RAIZ/estado.json")
HASH="$(sha256sum "$JAR" | awk '{print toupper($1)}')"

on_error() {
  local msg="$1"
  read -r _ act_hash ant_hash < <(mon_estado_info "$RAIZ/estado.json") || true
  if [[ "$old_hash" != "$HASH" && "${act_hash:-}" == "$HASH" && "${ant_hash:-}" == "$old_hash" ]]; then
    mon_revert_jar "$RAIZ" "$SERVICIO" || echo "AVISO: No se confirmo la recuperacion. Revisar servicio y logs." >&2
  fi
  printf 'NO CONFIRMADO: %s\n' "$msg" >"$SALIDA/aplicacion-resultado.txt"
  mon_fail "$msg"
}

if [[ "$old_ver" == "0.5.5" && "$old_hash" == "$HASH" ]]; then
  echo "La correccion 0.5.5 ya esta desplegada; no se reinicia el servicio."
else
  [[ "$old_ver" == "0.5.4" ]] || mon_fail "Se esperaba 0.5.4 antes de desplegar esta correccion (actual: $old_ver)."
  [[ -f "$SALIDA/estado-antes.json" ]] || cp -f "$RAIZ/estado.json" "$SALIDA/estado-antes.json"
  mon_deploy_jar "$RAIZ" "$JAR" "0.5.5" "$SERVICIO" || on_error "Fallo el despliegue de 0.5.5."
fi

CUR_HASH="$(sha256sum "$RAIZ/current/app.jar" | awk '{print toupper($1)}')"
[[ "$CUR_HASH" == "$HASH" ]] || on_error "El JAR activo no coincide."

if ! curl -fsS --max-time 10 "http://127.0.0.1:9086/actuator/prometheus" | grep -q 'hikaricp_connections'; then
  on_error "No se confirmaron las metricas del pool JDBC."
fi

for url in "http://127.0.0.1:5173/" "http://127.0.0.1:5174/" "http://127.0.0.1:5180/" "http://127.0.0.1:5180/api/v1/catalogos/puertos"; do
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$url" || true)"
  [[ "$code" == "200" ]] || on_error "Fallo la comprobacion HTTP de $url (HTTP $code)."
done

cp -f "$RAIZ/estado.json" "$SALIDA/estado-despues.json"
printf 'DESPLEGADO: 0.5.5; SHA256=%s; health UP, tres interfaces y catalogo HTTP 200, metricas Hikari disponibles. Falta repetir M07 para cerrar la incidencia.\n' "$HASH" >"$SALIDA/aplicacion-resultado.txt"
cat "$SALIDA/aplicacion-resultado.txt"
