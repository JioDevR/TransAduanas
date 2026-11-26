#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
# shellcheck source=../../monitoreo/scripts/linux/_lib.sh
source "$REPO_ROOT/utilities/monitoreo/scripts/linux/_lib.sh"

PROYECTO="$REPO_ROOT"
MODO="auto"
NGINX_DIR="${MON_NGINX_DIR:-/etc/nginx}"
SERVICIO_BACKEND="${MON_SERVICIO:-transaduanas-backend}"
SERVICIO_MYSQL="${MON_SERVICIO_MYSQL:-mysql}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --proyecto) PROYECTO="$2"; shift 2 ;;
    --modo) MODO="$2"; shift 2 ;;
    --nginx) NGINX_DIR="$2"; shift 2 ;;
    --servicio-backend) SERVICIO_BACKEND="$2"; shift 2 ;;
    --servicio-mysql) SERVICIO_MYSQL="$2"; shift 2 ;;
    -h|--help)
      echo "Uso: $0 [--proyecto RUTA] [--modo auto|dev|prod] [--nginx RUTA] [--servicio-backend NAME] [--servicio-mysql NAME]"
      exit 0
      ;;
    *) mon_fail "Argumento desconocido: $1" ;;
  esac
done

[[ "$MODO" =~ ^(auto|dev|prod)$ ]] || mon_fail "--modo debe ser auto, dev o prod."

if [[ "$MODO" == "auto" ]]; then
  if [[ -n "$(mon_port_pid 8085)" ]]; then
    MODO="dev"
  elif [[ -n "$(mon_port_pid 8086)" ]]; then
    MODO="prod"
  else
    MODO="dev"
  fi
fi

INICIO="$(mon_lima_iso)"
MARCA="M05-$(mon_lima_stamp)-$(mon_short_uuid)"
SALIDA="$PROYECTO/evidencias-monitoreo/$MARCA"
mkdir -p "$SALIDA"

python3 - "$MARCA" "$SALIDA" "$MODO" "$NGINX_DIR" "$SERVICIO_BACKEND" "$SERVICIO_MYSQL" "$INICIO" <<'PY'
import csv, json, os, re, subprocess, time, sys
from datetime import datetime, timezone, timedelta

marca, salida, modo, nginx_dir, svc_backend, svc_mysql, inicio = sys.argv[1:8]
tz = timezone(timedelta(hours=-5))

if modo == "dev":
    criterios = [
        "Modo desarrollo local: identificar Java (:8085) y MySQL (:3306) obligatoriamente.",
        "Verificar si el puerto de metricas (:9086) esta escuchando (perfil monitoreo) y que frontends Vite (:5173/:5174/:5180) estan activos.",
        "Realizar dos muestreos separados por dos segundos sin detener ni reiniciar procesos.",
    ]
else:
    criterios = [
        "Modo produccion/despliegue: identificar Java (:8086), MySQL (:3306) y Nginx (:5173/:5174/:5180) por ejecutable, PID y supervisor/maestro.",
        "Realizar dos muestreos separados por dos segundos sin detener ni reiniciar componentes.",
    ]

with open(f"{salida}/criterios-previos.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(criterios) + "\n")

def es_descendiente(proceso: int, ancestro: int, mapa: dict) -> bool:
    actual = proceso
    visitados = set()
    for _ in range(30):
        if actual == ancestro and actual != 0:
            return True
        if actual == 0 or actual in visitados or actual not in mapa:
            return False
        visitados.add(actual)
        actual = mapa[actual]["ParentProcessId"]
    return False

def systemd_info(name: str):
    try:
        out = subprocess.check_output(
            ["systemctl", "show", name, "--property=ActiveState,SubState,MainPID", "--no-pager"],
            text=True, stderr=subprocess.DEVNULL
        )
        kv = dict(line.split("=", 1) for line in out.splitlines() if "=" in line)
        active = kv.get("ActiveState", "unknown")
        sub = kv.get("SubState", "")
        pid = int(kv.get("MainPID", "0") or "0")
        if active == "inactive" and pid == 0:
            return None
        state = "Running" if active == "active" else f"{active}/{sub}"
        return {"Name": name, "State": state, "ProcessId": pid}
    except Exception:
        return None

def tomar_muestra(numero: int):
    ps_out = subprocess.check_output(["ps", "-eo", "pid=,ppid=,comm="], text=True)
    mapa = {}
    for line in ps_out.splitlines():
        parts = line.split(None, 2)
        if len(parts) < 3:
            continue
        pid, ppid, comm = int(parts[0]), int(parts[1]), parts[2].strip()
        mapa[pid] = {"Name": comm, "ProcessId": pid, "ParentProcessId": ppid}

    ss_out = subprocess.check_output(["ss", "-ltnp"], text=True)
    escuchas = []
    target_ports = {8085, 8086, 9086, 3306, 5173, 5174, 5180}
    for line in ss_out.splitlines():
        m_addr = re.search(r"\s(\S+):(\d+)\s", line)
        if not m_addr:
            continue
        addr, port = m_addr.group(1), int(m_addr.group(2))
        if port not in target_ports:
            continue
        for pid_s in re.findall(r"pid=(\d+)", line):
            escuchas.append({"LocalAddress": addr, "LocalPort": port, "OwningProcess": int(pid_s)})

    servicios = [s for s in (systemd_info(svc_backend), systemd_info(svc_mysql)) if s is not None]

    maestro = 0
    for cand in (f"{nginx_dir}/logs/nginx.pid", "/run/nginx.pid", "/var/run/nginx.pid"):
        if os.path.isfile(cand):
            txt = open(cand, encoding="utf-8").read().strip()
            if txt.isdigit():
                maestro = int(txt)
                break
    maestro_valido = maestro != 0 and maestro in mapa and mapa[maestro]["Name"] == "nginx"

    if modo == "dev":
        objetivos = [
            {"Componente": "Backend Java (dev)", "Puerto": 8085, "Servicio": "", "Ejecutables": {"java"}, "Requerido": True},
            {"Componente": "MySQL", "Puerto": 3306, "Servicio": svc_mysql, "Ejecutables": {"mysqld", "mariadbd"}, "Requerido": True},
            {"Componente": "Actuator metricas", "Puerto": 9086, "Servicio": "", "Ejecutables": {"java"}, "Requerido": False},
            {"Componente": "Frontend Landing (Vite)", "Puerto": 5173, "Servicio": "", "Ejecutables": {"node", "vite", "MainThread"}, "Requerido": False},
            {"Componente": "Frontend B2B (Vite)", "Puerto": 5174, "Servicio": "", "Ejecutables": {"node", "vite", "MainThread"}, "Requerido": False},
            {"Componente": "Frontend SGA (Vite)", "Puerto": 5180, "Servicio": "", "Ejecutables": {"node", "vite", "MainThread"}, "Requerido": False},
        ]
    else:
        objetivos = [
            {"Componente": "Backend Java", "Puerto": 8086, "Servicio": svc_backend, "Ejecutables": {"java"}, "Requerido": True},
            {"Componente": "MySQL", "Puerto": 3306, "Servicio": svc_mysql, "Ejecutables": {"mysqld", "mariadbd"}, "Requerido": True},
            {"Componente": "Nginx landing", "Puerto": 5173, "Servicio": "", "Ejecutables": {"nginx"}, "Requerido": True},
            {"Componente": "Nginx portal B2B", "Puerto": 5174, "Servicio": "", "Ejecutables": {"nginx"}, "Requerido": True},
            {"Componente": "Nginx SGA", "Puerto": 5180, "Servicio": "", "Ejecutables": {"nginx"}, "Requerido": True},
        ]

    filas = []
    for obj in objetivos:
        puertos = [e for e in escuchas if e["LocalPort"] == obj["Puerto"]]
        svc = [s for s in servicios if s["Name"] == obj["Servicio"]] if obj["Servicio"] else []

        if modo == "dev":
            padre = puertos[0]["OwningProcess"] if puertos else 0
            estado = "Activo en local" if puertos else ("Opcional no iniciado" if not obj["Requerido"] else "No escuchando")
            svc_ok = True if puertos else (not obj["Requerido"])
        elif obj["Servicio"]:
            if len(svc) == 1 and svc[0]["ProcessId"] > 0:
                padre = svc[0]["ProcessId"]
                estado = svc[0]["State"]
                svc_ok = estado == "Running"
            elif puertos and puertos[0]["OwningProcess"] in mapa and mapa[puertos[0]["OwningProcess"]]["Name"] in obj["Ejecutables"]:
                padre = puertos[0]["OwningProcess"]
                estado = "Proceso activo"
                svc_ok = True
            else:
                padre = 0
                estado = svc[0]["State"] if svc else "No encontrado"
                svc_ok = False
        else:
            padre = maestro
            estado = "Proceso maestro presente" if maestro_valido else "Maestro no identificado"
            svc_ok = maestro_valido

        if not puertos:
            filas.append({
                "Muestra": numero, "Componente": obj["Componente"], "Ejecutable": "",
                "PID": 0, "PIDSupervisor": padre, "Estado": estado, "Puerto": obj["Puerto"],
                "Direccion": "", "Escucha": False, "Requerido": obj["Requerido"],
                "ParentescoCorrecto": False, "Correcto": (not obj["Requerido"]),
            })
        for p in puertos:
            owner = p["OwningProcess"]
            existe = owner in mapa
            nombre = mapa[owner]["Name"] if existe else ""
            rel = True if modo == "dev" else es_descendiente(owner, padre, mapa)
            # En dev, si existe el proceso en el puerto requerido, valida por ejecutable conocido o java/node.
            exe_ok = (nombre in obj["Ejecutables"]) or (modo == "dev" and not obj["Requerido"])
            ok = svc_ok and existe and exe_ok and rel
            filas.append({
                "Muestra": numero, "Componente": obj["Componente"], "Ejecutable": nombre,
                "PID": owner, "PIDSupervisor": padre, "Estado": estado, "Puerto": obj["Puerto"],
                "Direccion": p["LocalAddress"], "Escucha": True, "Requerido": obj["Requerido"],
                "ParentescoCorrecto": rel, "Correcto": ok,
            })

    muestra = {
        "Muestra": numero,
        "Modo": modo,
        "FechaLima": datetime.now(tz).isoformat(),
        "Servicios": servicios,
        "MaestroNginx": maestro,
        "Puertos": escuchas,
        "Inventario": filas,
    }
    with open(f"{salida}/muestra-{numero}.json", "w", encoding="utf-8") as f:
        json.dump(muestra, f, indent=2, ensure_ascii=False)
    with open(f"{salida}/inventario-{numero}.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
        w.writeheader()
        w.writerows(filas)
    return muestra

m1 = tomar_muestra(1)
time.sleep(2)
m2 = tomar_muestra(2)

filas = m1["Inventario"] + m2["Inventario"]
ok = all(f["Correcto"] for f in filas)
estado = "EXITOSO" if ok else "FALLO"
detalle = "; ".join(f"{f['Componente']}: {f['Ejecutable'] or '-'}, PID {f['PID']}, puerto {f['Puerto']}, {f['Estado']}" for f in m2["Inventario"])
razon = f"Inventario ({modo}) verificado en dos muestreos. {detalle}" if ok else f"Uno o mas componentes requeridos en modo {modo} no estan escuchando."

resultado = {
    "Prueba": 5,
    "CodigoPrueba": "M05",
    "Modo": modo,
    "Nombre": "Inventario de procesos",
    "Importancia": "Baja",
    "Identificador": marca,
    "InicioLima": inicio,
    "FinLima": datetime.now(tz).isoformat(),
    "Herramientas": ["Bash / Python", "ps (/proc)", "ss -ltnp", "systemctl"],
    "Estado": estado,
    "Resultado": razon,
    "Muestreos": 2,
    "FilasVerificadas": len(filas),
    "FilasCorrectas": sum(1 for f in filas if f["Correcto"]),
    "InventarioFinal": m2["Inventario"],
}
with open(f"{salida}/resultado.json", "w", encoding="utf-8") as f:
    json.dump(resultado, f, indent=2, ensure_ascii=False)
with open(f"{salida}/inventario-completo.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
    w.writeheader()
    w.writerows(filas)

resumen = [
    f"PRUEBA 05 - INVENTARIO DE PROCESOS (MODO: {modo})",
    f"ESTADO: {estado}",
    f"RESULTADO: {razon}",
]
with open(f"{salida}/resultado.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(resumen) + "\n")

print(f"Modo: {modo}")
print(f"{'Componente':25} {'Ejecutable':12} {'PID':8} {'Puerto':6} {'Req':4} {'Estado':22} {'Correcto'}")
for f_row in m2["Inventario"]:
    print(f"{f_row['Componente']:25} {f_row['Ejecutable']:12} {str(f_row['PID']):8} {str(f_row['Puerto']):6} {str(f_row['Requerido']):4} {f_row['Estado']:22} {f_row['Correcto']}")
print(f"Estado: {estado}")
print(f"Evidencias: {salida}")
if not ok:
    sys.exit(1)
PY

cp "$0" "$SALIDA/ejecutar-prueba-05.sh"
