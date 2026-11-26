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
[[ -f "$MANIFEST" ]] || mon_fail "Falta $MANIFEST."

LOGS="$HERRAMIENTAS/datos/logs"
mkdir -p "$LOGS"

iniciar_log_proceso() {
  local nombre="$1" puerto="$2" config="$3"
  shift 3
  local exe resolved_exe owner_pid actual_exe actual_cmd workdir pid
  exe="$(mon_manifest_exe "$MANIFEST" "$nombre")" || mon_fail "Falta el binario de $nombre"
  [[ -x "$exe" ]] || mon_fail "Falta el binario ejecutable de $nombre: $exe"
  resolved_exe="$(readlink -f "$exe")"

  owner_pid="$(mon_port_pid "$puerto")"
  if [[ -n "$owner_pid" ]]; then
    actual_exe="$(mon_proc_exe "$owner_pid")"
    actual_cmd="$(mon_proc_cmdline "$owner_pid")"
    if [[ "$actual_exe" != "$resolved_exe" || "$actual_cmd" != *"$config"* ]]; then
      mon_fail "Puerto $puerto ocupado por otro proceso (PID $owner_pid)."
    fi
    printf '%s\t%s\t%s\t%s\t%s\n' "$nombre" "$owner_pid" "$resolved_exe" "$puerto" "$config"
    return 0
  fi

  workdir="$(dirname "$resolved_exe")"
  (
    cd "$workdir"
    nohup "$resolved_exe" "$@" >"$LOGS/${nombre}-out.log" 2>"$LOGS/${nombre}-error.log" < /dev/null &
    echo $!
  ) | {
    read -r pid
    printf '%s\t%s\t%s\t%s\t%s\n' "$nombre" "$pid" "$resolved_exe" "$puerto" "$config"
  }
}

MON_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
LCONFIG="$MON_DIR/herramientas/loki/loki.yml"
ACONFIG="$MON_DIR/herramientas/alloy/config.alloy"

ENTRIES=()
ENTRIES+=("$(GOMEMLIMIT=128MiB GOGC=60 iniciar_log_proceso loki 3100 "$LCONFIG" "-config.file=$LCONFIG")")
ENTRIES+=("$(GOMEMLIMIT=96MiB GOGC=60 iniciar_log_proceso alloy 12345 "$ACONFIG" \
  run "$ACONFIG" --server.http.listen-addr=127.0.0.1:12345 "--storage.path=$HERRAMIENTAS/datos/alloy" --disable-reporting)")

printf '%s\n' "${ENTRIES[@]}" | python3 - "$HERRAMIENTAS/procesos-logs.json" <<'PY'
import json, sys
items = []
for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    nombre, pid, exe, puerto, cfg = line.split("\t")
    items.append({"nombre": nombre, "processId": int(pid), "ejecutable": exe, "puerto": int(puerto), "config": cfg})
with open(sys.argv[1], "w", encoding="utf-8") as f:
    json.dump(items, f, indent=2)
PY

for url in "http://127.0.0.1:3100/ready" "http://127.0.0.1:12345/-/ready"; do
  mon_wait_http_200 "$url" 60 || mon_fail "No se confirmo $url; revisar logs de Loki/Alloy."
done

echo "Loki disponible en 127.0.0.1:3100; Alloy en 127.0.0.1:12345."
