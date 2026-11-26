#!/usr/bin/env bash
# Utilidades compartidas para los scripts Bash de monitoreo en Linux.

mon_fail() {
  echo "ERROR: $*" >&2
  exit 1
}

mon_default_herramientas() {
  printf '%s\n' "${MON_HERRAMIENTAS:-$HOME/Music/TransAduanas-herramientas/monitoreo}"
}

mon_default_desplegado() {
  printf '%s\n' "${MON_DESPLEGADO:-$HOME/Music/TransAduanas-desplegado}"
}

mon_lima_iso() {
  python3 -c 'from datetime import datetime, timezone, timedelta; print(datetime.now(timezone(timedelta(hours=-5))).isoformat())'
}

mon_lima_stamp() {
  python3 -c 'from datetime import datetime, timezone, timedelta; print(datetime.now(timezone(timedelta(hours=-5))).strftime("%Y%m%d-%H%M%S"))'
}

mon_short_uuid() {
  python3 -c 'import uuid; print(uuid.uuid4().hex[:6])'
}

# Devuelve el primer PID que escucha en un puerto TCP local (o vacio si no hay).
mon_port_pid() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltnp "sport = :${port}" 2>/dev/null \
      | sed -n 's/.*pid=\([0-9][0-9]*\).*/\1/p' \
      | head -n 1
  elif command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"${port}" -sTCP:LISTEN -t 2>/dev/null | head -n 1
  fi
}

# Obtiene ejecutable y linea de comandos de un PID desde /proc o ps.
mon_proc_exe() {
  local pid="$1"
  if [[ -L "/proc/$pid/exe" ]]; then
    readlink -f "/proc/$pid/exe" 2>/dev/null || true
  else
    ps -p "$pid" -o comm= 2>/dev/null | awk '{print $1}' || true
  fi
}

mon_proc_cmdline() {
  local pid="$1"
  if [[ -r "/proc/$pid/cmdline" ]]; then
    tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null || true
  else
    ps -p "$pid" -o args= 2>/dev/null || true
  fi
}

# Verifica que un PID siga vivo y corresponda al ejecutable (y opcionalmente config) antes de detenerlo.
mon_stop_verified_pid() {
  local nombre="$1"
  local pid="$2"
  local expected_exe="$3"
  local expected_substr="${4:-}"

  if [[ -z "$pid" ]] || ! kill -0 "$pid" 2>/dev/null; then
    return 0
  fi

  local actual_exe actual_cmd resolved_expected
  actual_exe="$(mon_proc_exe "$pid")"
  resolved_expected="$(readlink -f "$expected_exe" 2>/dev/null || printf '%s' "$expected_exe")"
  actual_cmd="$(mon_proc_cmdline "$pid")"

  if [[ "$actual_exe" != "$resolved_expected" ]]; then
    mon_fail "El PID $pid ($nombre) corresponde a otro ejecutable ($actual_exe); no se detuvo."
  fi
  if [[ -n "$expected_substr" && "$actual_cmd" != *"$expected_substr"* ]]; then
    mon_fail "El proceso $pid ($nombre) no coincide con la configuracion esperada; no se detuvo."
  fi

  kill "$pid" || mon_fail "No se pudo detener $nombre (PID $pid)."
  echo "Detenido: $nombre (PID $pid)"
}

# Espera hasta N segundos a que una URL devuelva HTTP 200.
mon_wait_http_200() {
  local url="$1"
  local timeout_s="${2:-45}"
  local i code
  for ((i = 0; i < timeout_s; i++)); do
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "$url" || true)"
    if [[ "$code" == "200" ]]; then
      return 0
    fi
    sleep 1
  done
  return 1
}

# Lee la ruta del ejecutable de un componente desde versiones.json.
mon_manifest_exe() {
  local manifest_path="$1"
  local nombre="$2"
  python3 - "$manifest_path" "$nombre" <<'PY'
import json, sys
data = json.load(open(sys.argv[1], encoding="utf-8-sig"))
for item in data:
    if item.get("nombre") == sys.argv[2]:
        print(item.get("ejecutable", ""))
        sys.exit(0)
sys.exit(1)
PY
}

# Imprime "<Actual.Version>\t<Actual.Hash>\t<Anterior.Hash>" desde estado.json.
mon_estado_info() {
  local estado_path="$1"
  python3 - "$estado_path" <<'PY'
import json, sys
d = json.load(open(sys.argv[1], encoding="utf-8-sig"))
act = d.get("Actual") or {}
ant = d.get("Anterior") or {}
print(f"{act.get('Version','')}\t{act.get('Hash','')}\t{ant.get('Hash','')}")
PY
}

# Despliega un JAR en $raiz (current/app.jar + releases/<hash> + estado.json) y reinicia el servicio systemd.
mon_deploy_jar() {
  local raiz="$1" jar="$2" version="$3" servicio="${4:-transaduanas-backend}"
  local hash release_dir copia actual estado_file
  hash="$(sha256sum "$jar" | awk '{print toupper($1)}')"
  release_dir="$raiz/releases/$hash"
  actual="$raiz/current/app.jar"
  estado_file="$raiz/estado.json"
  mkdir -p "$raiz/current" "$release_dir" "$raiz/logs" "$raiz/config"
  copia="$release_dir/app.jar"
  [[ -f "$copia" ]] || cp "$jar" "$copia"

  python3 - "$version" "$hash" "$copia" "$release_dir/artefacto.json" <<'PY'
import json, sys
art = {"Version": sys.argv[1], "Hash": sys.argv[2], "Ruta": sys.argv[3]}
json.dump(art, open(sys.argv[4], "w", encoding="utf-8"), indent=2)
PY

  if command -v systemctl >/dev/null 2>&1 && systemctl list-unit-files "${servicio}.service" >/dev/null 2>&1; then
    systemctl stop "$servicio"
    cp -f "$copia" "$actual"
    systemctl start "$servicio"
    mon_wait_http_200 "http://127.0.0.1:8086/api/v1/health" 60 || mon_fail "El backend no alcanzo UP tras desplegar $version."
  else
    cp -f "$copia" "$actual"
  fi

  python3 - "$estado_file" "$version" "$hash" "$copia" <<'PY'
import json, os, sys
estado_file, ver, h, ruta = sys.argv[1:5]
prev = json.load(open(estado_file, encoding="utf-8-sig"))["Actual"] if os.path.exists(estado_file) else None
nuevo = {"Actual": {"Version": ver, "Hash": h, "Ruta": ruta}, "Anterior": prev}
json.dump(nuevo, open(estado_file, "w", encoding="utf-8"), indent=2)
PY
}

# Revierte al release Anterior registrado en estado.json.
mon_revert_jar() {
  local raiz="$1" servicio="${2:-transaduanas-backend}"
  local estado_file="$raiz/estado.json"
  local ant_ruta ant_hash ant_ver
  read -r ant_ver ant_hash ant_ruta < <(python3 - "$estado_file" <<'PY'
import json, sys
ant = json.load(open(sys.argv[1], encoding="utf-8-sig")).get("Anterior")
if not ant:
    sys.exit(1)
print(ant["Version"], ant["Hash"], ant["Ruta"])
PY
) || mon_fail "No existe version anterior para revertir."
  [[ -f "$ant_ruta" ]] || mon_fail "Falta el artefacto anterior: $ant_ruta"
  if command -v systemctl >/dev/null 2>&1 && systemctl list-unit-files "${servicio}.service" >/dev/null 2>&1; then
    systemctl stop "$servicio"
    cp -f "$ant_ruta" "$raiz/current/app.jar"
    systemctl start "$servicio"
    mon_wait_http_200 "http://127.0.0.1:8086/api/v1/health" 60 || true
  else
    cp -f "$ant_ruta" "$raiz/current/app.jar"
  fi
  python3 - "$estado_file" "$ant_ver" "$ant_hash" "$ant_ruta" <<'PY'
import json, sys
estado_file, ver, h, ruta = sys.argv[1:5]
d = json.load(open(estado_file, encoding="utf-8-sig"))
d["Anterior"] = d.get("Actual")
d["Actual"] = {"Version": ver, "Hash": h, "Ruta": ruta}
json.dump(d, open(estado_file, "w", encoding="utf-8"), indent=2)
PY
}

# Detecta la URL base de la API priorizando modo desarrollo (:5180 Vite/proxy o :8085 directo) y luego :8086.
mon_detect_api_base() {
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "http://127.0.0.1:5180/api/v1/catalogos/puertos" || true)"
  if [[ "$code" == "200" ]]; then
    printf '%s\n' "http://127.0.0.1:5180"
    return 0
  fi
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "http://127.0.0.1:8085/api/v1/health" || true)"
  if [[ "$code" == "200" ]]; then
    printf '%s\n' "http://127.0.0.1:8085"
    return 0
  fi
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "http://127.0.0.1:8086/api/v1/health" || true)"
  if [[ "$code" == "200" ]]; then
    printf '%s\n' "http://127.0.0.1:8086"
    return 0
  fi
  return 1
}

# Detecta la ruta de sga-backend.log priorizando modo desarrollo (backend-sga/logs/sga-backend.log).
mon_detect_backend_log() {
  local repo_root="$1"
  local cand
  for cand in \
    "${MON_BACKEND_LOG:-}" \
    "$repo_root/backend-sga/logs/sga-backend.log" \
    "$repo_root/logs/sga-backend.log" \
    "$(mon_default_desplegado)/logs/sga-backend.log"; do
    if [[ -n "$cand" && -f "$cand" ]]; then
      printf '%s\n' "$cand"
      return 0
    fi
  done
  return 1
}


