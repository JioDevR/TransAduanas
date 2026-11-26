# Guía de monitoreo — TransAduanas (SGA)

El monitoreo se compone de **Prometheus** (métricas y alertas), **Grafana** (paneles), **Blackbox Exporter** (disponibilidad HTTP), **Loki** (almacén de logs) y **Grafana Alloy** (recolector de logs). Hay dos formas de usarlo:

1. **[Con Docker Compose](#1-instalación-con-docker-compose)** (Linux, macOS o Windows con Docker): un solo comando levanta todo el stack junto a la aplicación.
2. **[Con los scripts en modo desarrollo y laboratorio](#2-monitoreo-en-modo-desarrollo-y-con-scripts)** (Linux con `.sh` o Windows con `.ps1`): pruebas directas sobre tu entorno local (`:8085`, `:9086`, `:3306`) y operación del stack local.

## Índice

- [1. Instalación con Docker Compose](#1-instalación-con-docker-compose)
  - [1.1 Qué se instala](#11-qué-se-instala)
  - [1.2 Requisitos previos](#12-requisitos-previos)
  - [1.3 Paso 1: Variables de entorno](#13-paso-1-variables-de-entorno)
  - [1.4 Paso 2: Levantar aplicación y monitoreo](#14-paso-2-levantar-aplicación-y-monitoreo)
  - [1.5 Paso 3: Verificar](#15-paso-3-verificar)
  - [1.6 Operación diaria](#16-operación-diaria)
  - [1.7 Solución de problemas](#17-solución-de-problemas)
- [2. Monitoreo en modo desarrollo y con scripts](#2-monitoreo-en-modo-desarrollo-y-con-scripts)
  - [2.1 Cómo levantar el proyecto local con monitoreo](#21-cómo-levantar-el-proyecto-local-con-monitoreo)
  - [2.2 Prueba funcional rápida en modo desarrollo (`DEV`)](#22-prueba-funcional-rápida-en-modo-desarrollo-dev)
  - [2.3 Pruebas `M01`, `M02`, `M05` y `M01–M15`](#23-pruebas-m01-m02-m05-y-m01m15)
  - [2.4 Iniciar y detener el stack local sin Docker](#24-iniciar-y-detener-el-stack-local-sin-docker)
- [3. Revisión de scripts (necesarios vs innecesarios) y funcionamiento](#3-revisión-de-scripts-necesarios-vs-innecesarios-y-funcionamiento)
  - [3.1 Cómo funcionan los scripts Bash en Linux](#31-cómo-funcionan-los-scripts-bash-en-linux)
  - [3.2 Scripts de operación del stack (`utilities/monitoreo/scripts/`)](#32-scripts-de-operación-del-stack-utilitiesmonitoreoscripts)
  - [3.3 Scripts de pruebas funcionales (`utilities/pruebas/monitoreo/`)](#33-scripts-de-pruebas-funcionales-utilitiespruebasmonitoreo)
  - [3.4 Scripts prescindibles, obsoletos o auxiliares de documentación](#34-scripts-prescindibles-obsoletos-o-auxiliares-de-documentación)
- [4. Qué se mide](#4-qué-se-mide)

---

# 1. Instalación con Docker Compose

## 1.1 Qué se instala

El archivo [`docker-compose.monitoreo.yml`](../docker-compose.monitoreo.yml) se **combina** con [`docker-compose.yml`](../docker-compose.yml) y agrega seis piezas:

```text
 sga-backend :8085 (negocio)
      │  :9086 (solo red interna Docker)  /actuator/prometheus
      ▼
 prometheus :9090 ◄──── blackbox :9115 ──► sondea app-landing / app-b2b / app-sga / API
      │   ▲
      │   └── loki :3100 ◄── alloy :12345 ◄── sga-logs (backend) + stdout de los Nginx
      ▼
 grafana :3000  (dashboard "TransAduanas" de 18 paneles)
```

| Servicio | Imagen | Puerto en el host | Función |
| :-- | :-- | :-- | :-- |
| `prometheus` | `prom/prometheus` | `127.0.0.1:9090` | Recolecta cada 5 s, retención 2 días / 256 MB, evalúa 9 alertas |
| `grafana` | `grafana/grafana-oss` | `127.0.0.1:3000` | Dashboard y consulta de logs |
| `blackbox` | `prom/blackbox-exporter` | — (interno) | Sondeo HTTP de las 3 interfaces y de `/api/v1/catalogos/puertos` |
| `loki` | `grafana/loki` | — (interno) | Almacén de logs, retención 48 h |
| `alloy` | `grafana/alloy` | — (interno) | Lee `sga-backend.log` y la salida de los Nginx |

Además, el archivo activa en `sga-backend` el perfil `monitoreo` y hace que su puerto de métricas (`9086`) escuche en la red interna de Docker. **Ese puerto no se publica al host**; solo Prometheus lo alcanza.

Las configuraciones adaptadas a Docker están en [`utilities/monitoreo/herramientas/docker/`](../utilities/monitoreo/herramientas/docker/). El dashboard, las reglas de alerta y `blackbox.yml` se reutilizan tal cual desde `utilities/monitoreo/herramientas/`.

## 1.2 Requisitos previos

| Herramienta | Verificación |
| :-- | :-- |
| Docker con Compose v2 | `docker --version` y `docker compose version` |
| Git | `git --version` |

- La aplicación debe poder levantarse con Docker Compose (ver [instalación, sección 2](instalacion.md#2-instalación-con-docker-compose)).
- Puertos libres en el host: `3000` y `9090` (además de `3306`, `8085`, `5173`, `5174`, `5180`).
- Unos 1,5 GiB de RAM libres para el stack completo.
- `alloy` monta `/var/run/docker.sock` en **solo lectura** para leer los logs de los Nginx. Esto requiere Docker; con Podman, el socket es otro y los logs de los frontends no se recolectarían (los del backend sí).

## 1.3 Paso 1: Variables de entorno

Si aún no tienes `.env`, créalo desde el ejemplo y añade la contraseña de administrador de Grafana:

```bash
cp .env.example .env
```

Variables necesarias (las primeras ya existen para la aplicación):

```env
MYSQL_ROOT_PASSWORD=...
DB_USERNAME=sga_user
DB_PASSWORD=...
JWT_SECRET=...

# Monitoreo
GRAFANA_ADMIN_PASSWORD=una_clave_larga_y_aleatoria
```

Opcionalmente puedes fijar versiones de imagen con `PROMETHEUS_VERSION`, `GRAFANA_VERSION`, `LOKI_VERSION`, `ALLOY_VERSION` y `BLACKBOX_VERSION`. Los valores por defecto son los del laboratorio (Prometheus 3.13.5, Grafana 13.3.1, Loki 3.7.10, Alloy 1.22.0, Blackbox 0.29.1).

> [!IMPORTANT]
> `.env` contiene secretos y no debe subirse al repositorio.

## 1.4 Paso 2: Levantar aplicación y monitoreo

Desde la raíz del repositorio:

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoreo.yml up -d --build
```

Para evitar escribir los dos archivos cada vez, puedes exportar la variable:

```bash
export COMPOSE_FILE=docker-compose.yml:docker-compose.monitoreo.yml
docker compose up -d --build
```

> [!NOTE]
> Al reconstruir, el backend se recrea con el perfil `monitoreo`. Si ya tenías la aplicación levantada, Compose solo recrea los contenedores que cambian.

## 1.5 Paso 3: Verificar

```bash
docker compose ps
```

Deben estar `running`: `sga-backend`, `sga-prometheus`, `sga-grafana`, `sga-loki`, `sga-alloy`, `sga-blackbox` y los demás servicios de la aplicación.

| Comprobación | Cómo | Resultado esperado |
| :-- | :-- | :-- |
| Prometheus listo | `curl http://localhost:9090/-/ready` | `Prometheus Server is Ready.` |
| Targets | Abrir <http://localhost:9090/targets> | `transaduanas-backend`, `transaduanas-http` (4 sondeos), `transaduanas-loki` y `transaduanas-alloy` en **UP** |
| Métricas del backend | `docker compose exec prometheus wget -qO- http://sga-backend:9086/actuator/prometheus \| head` | Líneas de métricas |
| Grafana | Abrir <http://localhost:3000/d/transaduanas-monitoreo> | Dashboard con series reales (acceso anónimo como Viewer) |
| Alertas | <http://localhost:9090/alerts> | 9 reglas del grupo `transaduanas` (en verde si todo funciona) |
| Logs | En Grafana → *Explore* → datasource **Loki TransAduanas** → `{job="transaduanas-logs"}` | Eventos de `sga-backend` y de los Nginx |

Para editar dashboards o administrar Grafana, inicia sesión en <http://localhost:3000/login> con usuario `admin` y la contraseña de `GRAFANA_ADMIN_PASSWORD`.

Genera algo de tráfico para ver datos (por ejemplo, abre <http://localhost:5180> y navega, o ejecuta):

```bash
for i in $(seq 1 20); do curl -s -o /dev/null http://localhost:5180/api/v1/catalogos/puertos; done
```

## 1.6 Operación diaria

```bash
docker compose logs -f prometheus alloy      # logs de las herramientas
docker compose restart prometheus            # tras editar reglas.yml o prometheus.yml
docker compose stop prometheus grafana loki alloy blackbox   # parar solo el monitoreo
docker compose down                          # detener todo conservando datos
docker compose down -v                       # detener y BORRAR todos los volúmenes
```

> [!WARNING]
> `docker compose down -v` elimina también las series de Prometheus, los logs de Loki y los datos de Grafana, además de la base de datos.

Para cambiar las reglas de alerta edita [`reglas.yml`](../utilities/monitoreo/herramientas/prometheus/reglas.yml) y reinicia `prometheus`. Para cambiar el dashboard edita [`transaduanas.json`](../utilities/monitoreo/herramientas/grafana/dashboards/transaduanas.json); Grafana lo recarga cada 10 s.

## 1.7 Solución de problemas

| Síntoma | Causa probable y solución |
| :-- | :-- |
| `required variable GRAFANA_ADMIN_PASSWORD is missing` | Falta la variable en `.env` |
| Target `transaduanas-backend` en **DOWN** | El backend no arrancó con el perfil `monitoreo`. Revisa `docker compose logs sga-backend` y que usaste ambos archivos `-f` |
| Targets `transaduanas-http` en **DOWN** | Los frontends aún no responden o el contenedor no se llama `app-sga` / `app-b2b` / `app-landing`. Revisa `docker compose ps` |
| Grafana sin datos | Espera 15–30 s tras el arranque y genera tráfico; confirma los targets en <http://localhost:9090/targets> |
| Sin logs del backend en Loki | Alloy no puede leer el volumen `sga-logs`. Revisa `docker compose logs alloy` |
| Sin logs de los Nginx en Loki | El socket de Docker no está accesible (Podman o permisos). Los logs del backend siguen funcionando |
| Alerta `IngestaLogsInterrumpida` activa | Pasaron 2 min sin líneas de log. Genera tráfico: es normal en un sistema completamente inactivo |
| `Bind ... port is already allocated` en 3000/9090 | Otro servicio usa el puerto. Cambia el mapeo en `docker-compose.monitoreo.yml` |

---

# 2. Monitoreo en modo desarrollo y con scripts

Cuando trabajas en **modo desarrollo** ([instalación, sección 1](instalacion.md#1-instalación-para-desarrollo)), todo corre localmente sin Nginx ni contenedores:
- **MySQL** en `:3306`
- **Backend** (`backend-sga`) en `:8085` (negocio) y `:9086` (métricas, con el perfil `monitoreo`)
- **Logs** en `backend-sga/logs/sga-backend.log`
- **Frontends Vite** (`npm run dev`, opcionales para pruebas de API): `app-sga` (`:5180`), `app-b2b` (`:5174`), `app-landing` (`:5173`)

Los scripts Bash (`.sh`) fueron adaptados para detectar primero este entorno de desarrollo (`:8085` y `backend-sga/logs/sga-backend.log`) sin exigir Nginx ni rutas Windows.

## 2.1 Cómo levantar el proyecto local con monitoreo

1. Levanta MySQL (`:3306`) y configura `backend-sga/application-local.properties` como indica la [guía de instalación](instalacion.md#15-paso-2-backend).
2. Inicia el backend activando el perfil `monitoreo` desde `backend-sga/`:

```bash
cd backend-sga
SPRING_PROFILES_ACTIVE=monitoreo ./mvnw spring-boot:run
```

En Windows (PowerShell):

```powershell
cd backend-sga
$env:SPRING_PROFILES_ACTIVE="monitoreo"; .\mvnw.cmd spring-boot:run
```

3. Comprueba que las métricas respondan en `9086` y que `/actuator/env` siga bloqueado:

```bash
curl -s http://127.0.0.1:9086/actuator/prometheus | head -n 5
curl -i http://127.0.0.1:9086/actuator/env   # debe devolver 403/401
```

4. *(Opcional)* Inicia los frontends con `npm run dev` en `app-sga` (`:5180`), `app-b2b` (`:5174`) y `app-landing` (`:5173`).

## 2.2 Prueba funcional rápida en modo desarrollo (`DEV`)

El script [`probar-monitoreo-desarrollo.sh`](../utilities/pruebas/monitoreo/probar-monitoreo-desarrollo.sh) valida de forma automática todo el monitoreo local en una sola ejecución:

```bash
# Prueba básica (salud, seguridad Actuator, 10 peticiones, contador Micrometer y logs locales)
./utilities/pruebas/monitoreo/probar-monitoreo-desarrollo.sh

# Incluyendo login con usuario de seed.sql y verificación del pool MySQL (HikariCP)
./utilities/pruebas/monitoreo/probar-monitoreo-desarrollo.sh --con-auth
```

También puedes lanzarlo desde el orquestador único:

```bash
./utilities/pruebas/monitoreo/ejecutar-caso-monitoreo.sh --codigo DEV
```

Qué comprueba:
1. Que `:8085/api/v1/health` y `:9086/actuator/prometheus` estén arriba (si `:8085` responde pero `:9086` no, termina con código `3` `NO_EJECUTADA` explicando que falta `SPRING_PROFILES_ACTIVE=monitoreo`).
2. Que `:8085/actuator/prometheus` y `:9086/actuator/env` estén bloqueados.
3. Envía 10 `GET` reales a `/api/v1/catalogos/puertos` (por Vite `:5180` si está activo, o directo a `:8085`), verifica incremento `>= 10` en `http_server_requests_seconds_count` y busca los 10 `X-Request-ID` en `backend-sga/logs/sga-backend.log`.
4. Con `--con-auth`, hace login en `/api/v1/auth/login`, consulta `/api/v1/clientes` y verifica métricas `hikaricp_connections`.
5. Si tienes Prometheus (`:9090`), Grafana (`:3000`) o Loki (`:3100`) levantados, también valida que reciban la serie y tengan el dashboard disponible.

## 2.3 Pruebas `M01`, `M02`, `M05` y `M01–M15`

Las pruebas Bash locales funcionan directamente en modo desarrollo y generan evidencias en `evidencias-monitoreo/`:

```bash
./utilities/pruebas/monitoreo/ejecutar-caso-monitoreo.sh --codigo M01
./utilities/pruebas/monitoreo/ejecutar-caso-monitoreo.sh --codigo M02
./utilities/pruebas/monitoreo/ejecutar-caso-monitoreo.sh --codigo M05

# Consultar la última evidencia guardada (DEV o M01..M15)
./utilities/pruebas/monitoreo/mostrar-evidencia-monitoreo.sh --codigo DEV
```

- **`M01` (`ejecutar-prueba-01.sh`):** en modo desarrollo no exige Nginx; envía 10 `GET` (`:5180` o `:8085`) y valida por `X-Request-ID`, fecha, `service=sga-backend` y endpoint en `backend-sga/logs/sga-backend.log`.
- **`M02` (`ejecutar-prueba-02.sh`):** valida estructura (`fecha`, nivel `INFO`, `service=sga-backend`, `request_id`) en `sga-backend.log` (y Nginx si está presente).
- **`M05` (`ejecutar-prueba-05.sh`):** en `--modo auto` detecta `:8085` (modo `dev`) y verifica Java (`:8085`), MySQL (`:3306`), `:9086` (opcional) y Vite (`:5173/:5174/:5180`, opcionales). Con `--modo prod` mantiene la validación de Nginx (`nginx.pid`) y `:8086`.
- **`M03`, `M04`, `M06–M15` (`.py` / `.ps1`):** corresponden al laboratorio completo Windows (muchos asumen `C:\Users\asus\...`, `nginx/access.log` o MySQL en `Program Files`). Ver detalles en [`utilities/pruebas/monitoreo/README.md`](../utilities/pruebas/monitoreo/README.md).

## 2.4 Iniciar y detener el stack local sin Docker

Si prefieres ejecutar Prometheus, Grafana, Blackbox, Loki y Alloy localmente como binarios sin Docker:
- **Menú interactivo multiplataforma:** `python3 ./utilities/monitoreo/monitoreo.py` detecta Linux o Windows y lista los scripts disponibles.
- **Linux (Bash):** `./utilities/monitoreo/scripts/linux/iniciar-monitoreo.sh` y `./utilities/monitoreo/scripts/linux/detener-monitoreo.sh` (requiere los binarios Linux registrados en `$HOME/Music/TransAduanas-herramientas/monitoreo/versiones.json` y rutas locales ajustadas en `loki.yml` / `config.alloy`).
- **Windows (PowerShell):** descarga primero los binarios con `python .\utilities\monitoreo\instalar_herramientas.py` y ejecuta `.\utilities\monitoreo\scripts\win\Iniciar-Monitoreo.ps1` / `Detener-Monitoreo.ps1`.

---

# 3. Revisión de scripts (necesarios vs innecesarios) y funcionamiento

## 3.1 Cómo funcionan los scripts Bash en Linux

- **Biblioteca compartida ([`_lib.sh`](../utilities/monitoreo/scripts/linux/_lib.sh)):** centraliza la lectura JSON con `python3` (sin requerir `jq`), la detección automática del modo desarrollo (`mon_detect_api_base` prueba `:5180` → `:8085` → `:8086`; `mon_detect_backend_log` busca primero `backend-sga/logs/sga-backend.log`), la inspección de puertos (`ss -ltnp`), la verificación de proceso en `/proc/<pid>/exe` y `/proc/<pid>/cmdline` antes de enviar `SIGTERM`, y marcas de tiempo en zona Lima (`UTC-5`).
- **Variables de entorno opcionales:** `MON_HERRAMIENTAS` (`$HOME/Music/TransAduanas-herramientas/monitoreo`), `MON_DESPLEGADO` (`$HOME/Music/TransAduanas-desplegado`), `MON_NGINX_DIR`, `MON_BACKEND_LOG`, `MON_SERVICIO` y `MON_SERVICIO_MYSQL`.

## 3.2 Scripts de operación del stack (`utilities/monitoreo/scripts/`)

| Script (`.sh` / `.ps1`) | ¿Necesario en desarrollo? | Cómo funciona |
| :-- | :-- | :-- |
| `monitoreo.py` | **Opcional** (lanzador) | Menú interactivo que detecta Linux/Windows, escanea `scripts/linux` o `scripts/win` y ejecuta el script elegido. |
| [`_lib.sh`](../utilities/monitoreo/scripts/linux/_lib.sh) | **Necesario** (Linux) | Biblioteca compartida de funciones auxiliares para todos los `.sh`. |
| [`iniciar-monitoreo.sh`](../utilities/monitoreo/scripts/linux/iniciar-monitoreo.sh) / `Iniciar-Monitoreo.ps1` | **Solo sin Docker** | Crea `credenciales-locales.json` (`chmod 600`), inicia Blackbox (`:9115`), Prometheus (`:9090`), Grafana (`:3000`), espera `/ready` y llama a `iniciar-logs.sh`. Si usas `docker-compose.monitoreo.yml`, **no hace falta**. |
| [`detener-monitoreo.sh`](../utilities/monitoreo/scripts/linux/detener-monitoreo.sh) / `Detener-Monitoreo.ps1` | **Solo sin Docker** | Lee `procesos.json`, verifica ejecutable de cada PID antes de detenerlo y llama a `detener-logs.sh`. |
| [`iniciar-logs.sh`](../utilities/monitoreo/scripts/linux/iniciar-logs.sh) / [`detener-logs.sh`](../utilities/monitoreo/scripts/linux/detener-logs.sh) (+ `.ps1`) | **Solo sin Docker** | Arranca y detiene Loki (`:3100`) y Alloy (`:12345`) verificando PID y línea de comandos en `procesos-logs.json`. |
| `instalar_herramientas.py` | **Solo Windows sin Docker** | Descarga binarios `.exe` para Windows amd64, valida SHA256 y genera `versiones.json`. En Linux o Docker es innecesario. |
| `verificar_integracion.py` | **Solo laboratorio Windows** | Valida integración contra `:5180`, `:9086`, `:9090` y `:3000`, pero exige `estado.json` con versión `0.5.4` y `credenciales-locales.json`. En desarrollo usa `probar-monitoreo-desarrollo.sh`. |

## 3.3 Scripts de pruebas funcionales (`utilities/pruebas/monitoreo/`)

| Script | ¿Necesario en desarrollo? | Cómo funciona |
| :-- | :-- | :-- |
| [`probar-monitoreo-desarrollo.sh`](../utilities/pruebas/monitoreo/probar-monitoreo-desarrollo.sh) | **Necesario (recomendado)** | Prueba funcional pensada para modo desarrollo (`:8085` + `:9086`). Verifica salud, seguridad Actuator, 10 peticiones reales, aumento en Micrometer, correlación `X-Request-ID` en `sga-backend.log`, `--con-auth` (HikariCP) y stack opcional (`9090`/`3000`/`3100`). |
| [`ejecutar-caso-monitoreo.sh`](../utilities/pruebas/monitoreo/ejecutar-caso-monitoreo.sh) / `Ejecutar-CasoMonitoreo.ps1` | **Necesario** | Orquestador único (`--codigo DEV` o `M01..M15`). Gestiona credenciales temporales para `M06`/`M07`/`M10` y códigos de salida (`3` = `NO_EJECUTADA`). |
| [`mostrar-evidencia-monitoreo.sh`](../utilities/pruebas/monitoreo/mostrar-evidencia-monitoreo.sh) / `Mostrar-EvidenciaMonitoreo.ps1` | **Necesario** | Inspecciona la evidencia más reciente (`DEV-*` o `M*-*`) con `resultado.json` dentro de `evidencias-monitoreo/`. |
| [`ejecutar-prueba-01.sh`](../utilities/pruebas/monitoreo/ejecutar-prueba-01.sh) / `.ps1` | **Necesario** (adaptado a dev) | 10 `GET` secuenciales; valida registro en `sga-backend.log` (y Nginx si existe). |
| [`ejecutar-prueba-02.sh`](../utilities/pruebas/monitoreo/ejecutar-prueba-02.sh) / `.ps1` | **Necesario** (adaptado a dev) | 3 `GET` y correlación de `X-Request-ID` con formato Logback (`INFO`, `service=sga-backend`). |
| [`ejecutar-prueba-05.sh`](../utilities/pruebas/monitoreo/ejecutar-prueba-05.sh) / `.ps1` | **Necesario** (adaptado a dev) | 2 muestreos de procesos y puertos (`:8085`, `:3306`, `:9086` y Vite en modo `dev`; `:8086` y Nginx en modo `prod`). |
| `ejecutar_prueba_03.py`, `04_prometheus.py`, `06_prometheus.py`, `07`–`15.py` (+ `worker_cpu_m14.py`) | **Solo laboratorio Windows** | Pruebas formales de rotación, conteo/latencia en Prometheus, JDBC, JVM/GC, CPU, almacenamiento, Blackbox aislado, MySQL aislado, carga CPU y caída de Loki/Alloy. Varias dependen de `C:\Users\asus\...`, Nginx y servicios Windows. |

## 3.4 Scripts prescindibles, obsoletos o auxiliares de documentación

| Script | Motivo |
| :-- | :-- |
| `iniciar-nginx-seguro.sh` / `Iniciar-NginxSeguro.ps1` | **Innecesario en desarrollo:** en local los frontends corren con Vite (`npm run dev`), no con Nginx. |
| `aplicar-disponibilidad.sh` / `Aplicar-Disponibilidad.ps1` | **Histórico / innecesario:** Blackbox Exporter ya viene integrado por defecto en `iniciar-monitoreo` y en `docker-compose.monitoreo.yml`. |
| `actualizar-backend-metricas.sh` / `Actualizar-Backend-Metricas.ps1` | **Histórico / innecesario:** migración puntual del laboratorio `0.5.3 → 0.5.4`. |
| `actualizar-backend-jdbc.sh` / `Actualizar-Backend-Jdbc.ps1` | **Histórico / innecesario:** migración puntual del laboratorio `0.5.4 → 0.5.5`. |
| `ejecutar-m09-administrador.sh` / `Ejecutar-M09-Administrador.ps1` | **Solo Windows:** wrapper creado para lanzar `M09` con privilegios de administrador en Windows. |
| `ejecutar_prueba_04.py` y `ejecutar_prueba_06.py` | **Obsoletos:** versiones previas con CSV antes de integrar Prometheus; reemplazados por `ejecutar_prueba_04_prometheus.py` y `ejecutar_prueba_06_prometheus.py`. |
| `verificar_preparacion.py` | **Opcional:** suite `unittest` interna para probar las validaciones previas de `M04`, `M09`, `M10` y `M14` en Python. |
| `crear_manual.py`, `actualizar_manual_monitoreo.py`, `actualizar_registros.py`, `contenido_breve.py`, `contenido_didactico.py`, `crear_tablas_finales.py`, `herramientas_por_prueba.py`, `resultados_y_muestras.py`, `leer_almacenamiento_backend.py` | **No son de monitoreo en ejecución:** scripts auxiliares usados para redactar y generar los informes `.docx`/`.pdf` de la Semana 16. |

---

# 4. Qué se mide

**Dashboard (18 paneles):** recolección del backend, solicitudes acumuladas y por segundo, proporción HTTP 5xx, latencias p50/p95/p99, tasa por endpoint, memoria JVM, CPU del host/proceso, tiempo de GC, conexiones del pool MySQL (Hikari), hilos Java, disponibilidad HTTP, ingesta y búsqueda de logs, y alertas activas.

**Alertas** ([`reglas.yml`](../utilities/monitoreo/herramientas/prometheus/reglas.yml)):

| Alerta | Condición | Severidad |
| :-- | :-- | :-- |
| `BackendSinMetricas` | Backend no recolectable 30 s | critical |
| `TasaErroresHTTPAlta` | > 1 % de respuestas 5xx durante 5 min | critical |
| `LatenciaP95Alta` | p95 > 500 ms durante 5 min | warning |
| `CPUHostAlta` | CPU > 85 % durante 5 min | warning |
| `DisponibilidadHTTPFallida` | `probe_success == 0` durante 15 s | critical |
| `SondeoHTTPNoDisponible` | Blackbox no recolectable 30 s | critical |
| `IngestaLogsInterrumpida` | 2 min sin líneas de log con backend y Loki activos | critical |
| `RecolectorLogsNoDisponible` | Alloy no recolectable 30 s | warning |
| `ReceptorLogsNoDisponible` | Loki no disponible 30 s | critical |

> [!NOTE]
> Las alertas se visualizan en Prometheus y Grafana. No hay Alertmanager ni notificaciones por correo o Slack. Los percentiles del panel son aproximaciones del histograma de Micrometer (ventana de 5 min) y `up` solo indica que se pudo leer el endpoint de métricas, no que todas las operaciones de negocio funcionen.

**Privacidad:** los logs HTTP incluyen `service`, `request_id` y estado, sin query strings, cuerpos ni credenciales; el seguimiento JDBC correlaciona por `request_id` y `jdbc_span_id` sin registrar SQL ni parámetros. El puerto `9086` solo expone `health` y `prometheus`; `/actuator/env` y el resto permanecen bloqueados.
