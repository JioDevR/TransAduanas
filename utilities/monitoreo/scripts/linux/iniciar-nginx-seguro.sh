#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_lib.sh
source "$SCRIPT_DIR/_lib.sh"

NGINX_DIR="${MON_NGINX_DIR:-/etc/nginx}"
NGINX_BIN="${MON_NGINX_BIN:-$(command -v nginx || true)}"
PIDFILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --nginx-dir) NGINX_DIR="$2"; shift 2 ;;
    --nginx-bin) NGINX_BIN="$2"; shift 2 ;;
    --pidfile) PIDFILE="$2"; shift 2 ;;
    -h|--help)
      echo "Uso: $0 [--nginx-dir RUTA] [--nginx-bin BINARIO] [--pidfile RUTA]"
      exit 0
      ;;
    *) mon_fail "Argumento desconocido: $1" ;;
  esac
done

[[ -n "$NGINX_BIN" && -x "$NGINX_BIN" ]] || mon_fail "No se encontro binario ejecutable de nginx."
NGINX_BIN="$(readlink -f "$NGINX_BIN")"
if [[ -z "$PIDFILE" ]]; then
  if [[ -f "$NGINX_DIR/logs/nginx.pid" ]]; then
    PIDFILE="$NGINX_DIR/logs/nginx.pid"
  else
    PIDFILE="/run/nginx.pid"
  fi
fi

PUERTOS=(5173 5174 5180)
LOCKFILE="/tmp/transaduanas-nginx-inicio.lock"

comprobar_instancia() {
  python3 - "$NGINX_BIN" "$PIDFILE" "${PUERTOS[@]}" <<'PY'
import os, re, subprocess, sys

nginx_bin = os.path.realpath(sys.argv[1])
pidfile = sys.argv[2]
puertos = [int(p) for p in sys.argv[3:]]

ps_out = subprocess.check_output(["ps", "-eo", "pid=,ppid=,comm="], text=True)
procs = []
for line in ps_out.splitlines():
    parts = line.split(None, 2)
    if len(parts) < 3:
        continue
    pid, ppid, comm = int(parts[0]), int(parts[1]), parts[2].strip()
    if comm != "nginx":
        continue
    try:
        exe = os.path.realpath(f"/proc/{pid}/exe")
    except OSError:
        exe = nginx_bin
    if exe == nginx_bin:
        procs.append((pid, ppid))

ss_out = subprocess.check_output(["ss", "-ltnp"], text=True)
escuchas = {}
for line in ss_out.splitlines():
    m_port = re.search(r":(\d+)\s", line)
    if not m_port:
        continue
    port = int(m_port.group(1))
    if port not in puertos:
        continue
    pids = [int(p) for p in re.findall(r"pid=(\d+)", line)]
    escuchas.setdefault(port, set()).update(pids)

if not procs:
    if any(escuchas.get(p) for p in puertos):
        raise SystemExit("ERROR: Puertos web ocupados sin instancia Nginx identificada. No se inicia otro proceso.")
    sys.exit(10)

ids = {p for p, _ in procs}
maestros = [p for p, ppid in procs if ppid not in ids]
if len(maestros) != 1:
    raise SystemExit("ERROR: Existen varias instancias o procesos Nginx sin maestro unico. Revisar M05 antes de iniciar.")
maestro = maestros[0]

if not os.path.isfile(pidfile):
    raise SystemExit("ERROR: Nginx ya esta iniciado pero falta nginx.pid. No se crea otra instancia.")
pid_txt = open(pidfile, encoding="utf-8").read().strip()
if pid_txt != str(maestro):
    raise SystemExit("ERROR: nginx.pid no coincide con el maestro actual. No se crea otra instancia.")

for p in puertos:
    owners = escuchas.get(p, set())
    if not owners or any(o not in ids for o in owners):
        raise SystemExit(f"ERROR: Puerto {p} ausente o perteneciente a otra instancia. Revisar M05.")

sys.exit(0)
PY
}

exec 9>"$LOCKFILE"
flock -n 9 || mon_fail "Ya hay un inicio de Nginx en curso."

set +e
comprobar_instancia
rc=$?
set -e
if [[ $rc -eq 0 ]]; then
  echo "Nginx ya esta iniciado: maestro unico y puertos correctos. No se abrio otra instancia."
  exit 0
elif [[ $rc -ne 10 ]]; then
  exit "$rc"
fi

"$NGINX_BIN" -t || mon_fail "Nginx rechazo la configuracion. No se inicio."
if [[ -d "$NGINX_DIR" ]]; then
  (cd "$NGINX_DIR" && "$NGINX_BIN")
else
  "$NGINX_BIN"
fi

listo=0
for ((i = 0; i < 20; i++)); do
  sleep 0.5
  set +e
  comprobar_instancia
  rc=$?
  set -e
  if [[ $rc -eq 0 ]]; then
    listo=1
    break
  elif [[ $rc -ne 10 && $i -eq 19 ]]; then
    exit "$rc"
  fi
done

[[ "$listo" -eq 1 ]] || mon_fail "Nginx no completo el inicio. Revisar logs/error.log antes de reintentar."
echo "Nginx iniciado y verificado: maestro unico y puertos 5173, 5174 y 5180."
