# Guía de instalación — TransAduanas (SGA)

Esta guía tiene dos modalidades:

1. **[Instalación para desarrollo](#1-instalación-para-desarrollo):** levanta cada servicio de forma nativa en tu máquina.
2. **[Instalación con Docker Compose](#2-instalación-con-docker-compose):** despliega los cinco servicios en contenedores, sin instalar Java, Node.js ni MySQL.

Para agregar Prometheus, Grafana, Loki y Alloy (con Docker Compose o con scripts en Windows), consulta la [guía de monitoreo](monitoreo.md).

## Índice

- [1. Instalación para desarrollo](#1-instalación-para-desarrollo)
  - [1.1 Arquitectura en ejecución](#11-arquitectura-en-ejecución)
  - [1.2 Requisitos previos](#12-requisitos-previos)
  - [1.3 Clonar el repositorio](#13-clonar-el-repositorio)
  - [1.4 Paso 1: Base de datos](#14-paso-1-base-de-datos)
  - [1.5 Paso 2: Backend](#15-paso-2-backend)
  - [1.6 Paso 3: Frontend interno (SGA)](#16-paso-3-frontend-interno-sga)
  - [1.7 Paso 4: Portal B2B](#17-paso-4-portal-b2b)
  - [1.8 Paso 5: Landing (opcional)](#18-paso-5-landing-opcional)
  - [1.9 Servicios y puertos](#19-servicios-y-puertos)
  - [1.10 Credenciales de prueba](#110-credenciales-de-prueba)
  - [1.11 Pruebas automatizadas](#111-pruebas-automatizadas)
  - [1.12 Solución de problemas](#112-solución-de-problemas)
- [2. Instalación con Docker Compose](#2-instalación-con-docker-compose)
  - [2.1 Arquitectura en contenedores](#21-arquitectura-en-contenedores)
  - [2.2 Requisitos previos](#22-requisitos-previos)
  - [2.3 Paso 1: Preparar los archivos de Docker](#23-paso-1-preparar-los-archivos-de-docker)
  - [2.4 Paso 2: Configurar el `.env`](#24-paso-2-configurar-el-env)
  - [2.5 Paso 3: Construir y levantar](#25-paso-3-construir-y-levantar)
  - [2.6 Paso 4: Verificar el despliegue](#26-paso-4-verificar-el-despliegue)
  - [2.7 Credenciales de prueba](#27-credenciales-de-prueba)
  - [2.8 Operación diaria](#28-operación-diaria)
  - [2.9 Solución de problemas](#29-solución-de-problemas)

---

# 1. Instalación para desarrollo

## 1.1 Arquitectura en ejecución

```text
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ app-landing  │   │   app-sga    │   │   app-b2b    │
│    :5173     │   │    :5180     │   │    :5174     │
└──────────────┘   └──────┬───────┘   └──────┬───────┘
                          │  proxy /api → :8085
                          └─────────┬────────┘
                                    ▼
                      ┌───────────────────────────┐
                      │ backend-sga (Spring Boot) │
                      │          :8085            │
                      └─────────────┬─────────────┘
                                    │ JDBC
                                    ▼
                      ┌───────────────────────────┐
                      │  MySQL 8.4 · BD `SGAdb`   │
                      │          :3306            │
                      └───────────────────────────┘
```

## 1.2 Requisitos previos

| Herramienta | Versión | Verificación |
| :-- | :-: | :-- |
| Git | cualquiera | `git --version` |
| Java JDK | 21 | `java -version` |
| Node.js | 20 LTS o superior | `node -v` |
| npm | 9 o superior | `npm -v` |
| MySQL Server | 8.0 / 8.4 | `mysql --version` |

> [!NOTE]
> No necesitas instalar Maven: el backend incluye el wrapper `mvnw` (`mvnw.cmd` en Windows).

## 1.3 Clonar el repositorio

```bash
git clone <URL-del-repositorio> TransAduanas
cd TransAduanas
```

## 1.4 Paso 1: Base de datos

Los scripts están en [`db/`](../db):

- [`schema.sql`](../db/schema.sql): crea la base `SGAdb` (`utf8mb4`) con todas las tablas.
- [`seed.sql`](../db/seed.sql): carga sedes, catálogos, usuarios internos, clientes y cuentas del Portal B2B de prueba.

Desde la raíz del repositorio, ejecútalos en este orden:

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/seed.sql
```

> [!TIP]
> También puedes ejecutar ambos archivos, en el mismo orden, desde MySQL Workbench, DBeaver o DataGrip.

> [!WARNING]
> `schema.sql` crea la base desde cero. Si ya existe `SGAdb` con datos que quieres conservar, haz un respaldo antes de ejecutarlo.

## 1.5 Paso 2: Backend

### A. Credenciales locales

El backend importa automáticamente `backend-sga/application-local.properties`, que está ignorado por Git. Créalo a partir de la plantilla:

```bash
cd backend-sga
cp application-local.properties.example application-local.properties
```

En Windows (PowerShell): `Copy-Item application-local.properties.example application-local.properties`.

Edita el archivo con tus credenciales de MySQL:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/SGAdb?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=tu_contraseña_mysql
```

### B. Variables de entorno (alternativa)

Puedes usar variables de entorno en lugar del archivo anterior:

| Variable | Valor por defecto | Descripción |
| :-- | :-- | :-- |
| `SERVER_PORT` | `8085` | Puerto HTTP del backend |
| `DB_URL` | `jdbc:mysql://localhost:3306/SGAdb?...` | URL JDBC |
| `DB_USERNAME` | `root` | Usuario de MySQL |
| `DB_PASSWORD` | *(vacío)* | Contraseña de MySQL |
| `JPA_DDL_AUTO` | `update` | Modo `ddl-auto` de Hibernate |
| `JPA_SHOW_SQL` | `true` | Muestra el SQL en consola |
| `JWT_SECRET` | *(valor de desarrollo)* | Clave de firma de los JWT |
| `JWT_EXPIRATION_MS` | `86400000` | Vigencia del token (24 h) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:5180,http://localhost:5174` | Orígenes permitidos por CORS |
| `STORAGE_DIR` | `./uploads/documentos` | Carpeta de archivos subidos |

### C. Iniciar el backend

Desde `backend-sga/`:

```bash
./mvnw spring-boot:run          # Linux / macOS
.\mvnw.cmd spring-boot:run      # Windows
```

- API base: `http://localhost:8085/api/v1`
- Health check: `http://localhost:8085/api/v1/health`

Deja esta terminal abierta.

## 1.6 Paso 3: Frontend interno (SGA)

En una **nueva terminal**, desde la raíz del repositorio:

```bash
cd app-sga
npm install
npm run dev
```

Acceso: `http://localhost:5180`. Vite redirige `/api` a `http://localhost:8085`.

## 1.7 Paso 4: Portal B2B

En otra terminal, desde la raíz del repositorio:

```bash
cd app-b2b
npm install
npm run dev
```

Acceso: `http://localhost:5174`.

## 1.8 Paso 5: Landing (opcional)

Sitio público independiente. Copia `.env.example` a `.env` si necesitas ajustar sus variables.

```bash
cd app-landing
npm install
npm run dev
```

Acceso: `http://localhost:5173`.

## 1.9 Servicios y puertos

| Servicio | Directorio | Tecnología | Puerto | URL |
| :-- | :-- | :-- | :-: | :-- |
| Base de datos | [`db/`](../db) | MySQL 8.4 | 3306 | `jdbc:mysql://localhost:3306/SGAdb` |
| Backend API | [`backend-sga/`](../backend-sga) | Spring Boot 4.1 / Java 21 | 8085 | `http://localhost:8085/api/v1/health` |
| Frontend interno | [`app-sga/`](../app-sga) | Vue 3 + Vite | 5180 | `http://localhost:5180` |
| Portal B2B | [`app-b2b/`](../app-b2b) | Vue 3 + Vite | 5174 | `http://localhost:5174` |
| Landing | [`app-landing/`](../app-landing) | Vue 3 + Vite | 5173 | `http://localhost:5173` |

## 1.10 Credenciales de prueba

Todas las cuentas del `seed.sql` usan la contraseña **`Password123!`**. Son solo para desarrollo.

### Usuarios internos (`http://localhost:5180`)

| Rol | Usuario principal | Otros usuarios |
| :-- | :-: | :-- |
| `ADMINISTRADOR_TI` | `admin` | `jramirez`, `mquispeti`, `cbenitez` |
| `GERENTE_GENERAL` | `gerente` | `lsantillan`, `rbelaunde` |
| `AGENTE_COMERCIAL` | `comercial` | `dcastillo`, `nvalverde`, `mrodriguez` |
| `SUPERVISOR_ADUANERO` | `supervisor` | `rcabrera`, `apena`, `dmiranda` |
| `AGENTE_ADUANERO` | `agente` | `fbalbuena`, `lmejia`, `ccordova` |
| `CONTADOR` | `contador` | `rsalazar`, `gmontoya`, `lcornejo` |

### Portal B2B (`http://localhost:5174`)

El usuario es el **RUC de 11 dígitos** de la empresa.

| Empresa | RUC |
| :-- | :-: |
| Camposol S.A. | `20370146994` |
| Danper Trujillo S.A.C. | `20440058771` |
| LG Electronics Peru S.A. | `20511894520` |
| Compania Minera Antamina S.A. | `20330790762` |
| Ferreyros S.A. | `20100055237` |
| Alicorp S.A.A. | `20100055238` |

## 1.11 Pruebas automatizadas

```bash
# Backend (desde backend-sga/)
./mvnw test

# Frontends (desde cada directorio)
cd app-sga && npm run test && npm run build
cd ../app-b2b && npm run test && npm run build
```

Los casos funcionales y el inventario de pruebas están en [`docs/pruebas/`](pruebas/funcionales/README.md).

## 1.12 Solución de problemas

| Síntoma | Causa probable y solución |
| :-- | :-- |
| `Access denied for user 'root'@'localhost'` al iniciar el backend | Revisa `spring.datasource.username` y `spring.datasource.password` en `backend-sga/application-local.properties`. |
| `Unknown database 'SGAdb'` | Ejecuta primero `db/schema.sql` y luego `db/seed.sql` (Paso 1). |
| `Network Error` o `502` al iniciar sesión | El backend no está activo. Comprueba `http://localhost:8085/api/v1/health` antes de usar los frontends. |
| Error de CORS en el navegador | Abre el frontend en uno de los orígenes permitidos (`5173`, `5174`, `5180`) o ajusta `CORS_ALLOWED_ORIGINS`. |
| `Port 8085 already in use` | Cierra el proceso que usa el puerto o define otro con `SERVER_PORT`. |
| `UnsupportedClassVersionError` | Tu JDK es anterior a 21. Verifica `java -version` y `JAVA_HOME`. |

---

# 2. Instalación con Docker Compose

Despliega todo el sistema en contenedores: MySQL, backend, SGA interno, Portal B2B y landing. Las imágenes se compilan dentro de Docker (multi-stage), por lo que solo necesitas Docker o Podman.

> [!IMPORTANT]
> Los `Dockerfile`, los `nginx.conf` y el `docker-compose.yml` se definen en la [guía de imágenes Docker](docker-image-build.md). Si aún no existen en tu copia del repositorio, créalos primero (Paso 1).

## 2.1 Arquitectura en contenedores

```text
 Navegador
    │
    ├─ :5173 ─► app-landing  (Nginx)
    ├─ :5180 ─► app-sga      (Nginx) ──┐
    └─ :5174 ─► app-b2b      (Nginx) ──┤  /api/ → proxy inverso
                                       ▼
                              sga-backend :8085
                                       │ JDBC
                                       ▼
                              mysql :3306 (SGAdb)

 Volúmenes: sga-mysql-data · sga-uploads · sga-logs
```

Los frontends llaman a `/api/v1` en su propio origen y Nginx reenvía la petición al backend, por lo que no hay problemas de CORS.

## 2.2 Requisitos previos

| Herramienta | Verificación |
| :-- | :-- |
| Docker con Compose v2 | `docker --version` y `docker compose version` |
| Podman (alternativa) | `podman --version` y `podman compose version` |
| Git | `git --version` |

- Puertos libres: `3306`, `8085`, `5173`, `5174` y `5180`.
- Conexión a internet en la primera construcción, para descargar imágenes base y dependencias.
- Con Podman, reemplaza `docker compose` por `podman compose` en todos los comandos.

## 2.3 Paso 1: Preparar los archivos de Docker

Desde la raíz del repositorio, comprueba que existan estos archivos:

```bash
ls docker-compose.yml .env.example \
   backend-sga/Dockerfile backend-sga/.dockerignore \
   app-sga/Dockerfile app-sga/nginx.conf app-sga/.dockerignore \
   app-b2b/Dockerfile app-b2b/nginx.conf app-b2b/.dockerignore \
   app-landing/Dockerfile app-landing/nginx.conf app-landing/.dockerignore
```

Si falta alguno, créalo con el contenido de la [guía de imágenes Docker](docker-image-build.md):

| Archivo | Sección de la guía |
| :-- | :-- |
| `.dockerignore` (backend y frontends) | [Paso 1](docker-image-build.md#2-paso-1-archivos-dockerignore) |
| `backend-sga/Dockerfile` | [Paso 2](docker-image-build.md#3-paso-2-imagen-del-backend) |
| `nginx.conf` y `Dockerfile` de cada frontend | [Paso 3](docker-image-build.md#4-paso-3-imágenes-de-los-frontends) |
| `.env.example` | [Paso 4](docker-image-build.md#5-paso-4-variables-de-entorno-env) |
| `docker-compose.yml` | [Paso 5](docker-image-build.md#6-paso-5-docker-composeyml) |

## 2.4 Paso 2: Configurar el `.env`

```bash
cp .env.example .env
```

Edita `.env` y define, como mínimo:

| Variable | Descripción |
| :-- | :-- |
| `MYSQL_ROOT_PASSWORD` | Contraseña del usuario `root` de MySQL |
| `DB_USERNAME` / `DB_PASSWORD` | Usuario y contraseña que usa el backend |
| `JWT_SECRET` | Clave de firma de los tokens (mínimo 32 caracteres) |
| `SGA_PUBLIC_URL` / `PORTAL_PUBLIC_URL` | URLs públicas del SGA y del Portal B2B. Con `localhost` basta para una prueba local |

Genera un secreto aleatorio con:

```bash
openssl rand -base64 48
```

> [!CAUTION]
> No subas `.env` a Git (ya está en el `.gitignore`). Usa claves propias, distintas a las del ejemplo.

Valida la configuración antes de construir:

```bash
docker compose config --quiet && echo "Configuración válida"
```

## 2.5 Paso 3: Construir y levantar

```bash
docker compose up -d --build
```

La primera vez tarda varios minutos; las siguientes aprovechan la caché de capas. Docker hace lo siguiente:

1. Descarga MySQL 8.4 y, con el volumen vacío, ejecuta `db/schema.sql` y `db/seed.sql`.
2. Compila el JAR del backend dentro de la imagen.
3. Compila los tres frontends y los sirve con Nginx.
4. Arranca el backend cuando MySQL está `healthy`.

Para seguir el arranque del backend:

```bash
docker compose logs -f sga-backend
```

Espera la línea `Started SgaApplication` y sal con `Ctrl+C` (los contenedores siguen corriendo).

## 2.6 Paso 4: Verificar el despliegue

```bash
docker compose ps
curl http://localhost:8085/api/v1/health
```

Todos los servicios deben estar en ejecución (`running`) y MySQL en `healthy`.

| Servicio | URL |
| :-- | :-- |
| Landing | `http://localhost:5173` |
| SGA interno | `http://localhost:5180` |
| Portal B2B | `http://localhost:5174` |
| API REST | `http://localhost:8085/api/v1` |
| MySQL | `localhost:3306` |

Prueba de extremo a extremo: entra a `http://localhost:5180` con un usuario de la sección siguiente. Si inicia sesión, el proxy de Nginx, el backend y la base de datos funcionan.

## 2.7 Credenciales de prueba

Son las mismas que en el despliegue para desarrollo: usuarios internos y RUC del Portal B2B con la contraseña `Password123!`. Consúltalas en [1.10 Credenciales de prueba](#110-credenciales-de-prueba).

## 2.8 Operación diaria

```bash
docker compose ps                         # estado de los servicios
docker compose logs -f sga-backend        # logs del backend
docker compose restart sga-backend        # reiniciar un servicio
docker compose up -d --build sga-backend  # reconstruir tras cambios de código
docker compose down                       # detener conservando los datos
docker compose down -v                    # detener y BORRAR base de datos y archivos subidos
```

> [!WARNING]
> `docker compose down -v` elimina los volúmenes. Úsalo solo para reiniciar el sistema desde cero con `seed.sql`.

**Actualizar a una nueva versión:**

```bash
git pull
docker compose up -d --build
```

Los datos se conservan en los volúmenes. Si cambió `schema.sql`, `docker compose up` no lo reaplica: MySQL solo ejecuta los scripts con el volumen vacío.

**Respaldo de la base de datos:**

```bash
docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" SGAdb' > backup-sgadb.sql
```

## 2.9 Solución de problemas

| Síntoma | Causa probable y solución |
| :-- | :-- |
| `required variable ... is missing` | Falta `.env` o una variable. Ejecuta `cp .env.example .env` y complétalo |
| `Bind for 0.0.0.0:<puerto> failed: port is already allocated` | Otro servicio usa ese puerto. Detenlo o cambia el puerto publicado en `docker-compose.yml` |
| El backend se reinicia en bucle | Revisa `docker compose logs sga-backend`. Suele ser MySQL sin iniciar o credenciales distintas a las del volumen |
| `Access denied for user` | Cambiaste `DB_USERNAME` o `DB_PASSWORD` después de crear el volumen. Recréalo con `docker compose down -v` |
| La base está vacía | El volumen ya existía y no se ejecutaron `schema.sql` y `seed.sql`. Usa `docker compose down -v` y vuelve a levantar |
| `502 Bad Gateway` en `/api/` | El backend aún no arrancó o falló. Revisa `docker compose ps` y sus logs |
| Error de CORS | La URL del navegador no coincide con `SGA_PUBLIC_URL` o `PORTAL_PUBLIC_URL`. Ajusta `.env` y reinicia el backend |
| Los enlaces de la landing apuntan a `localhost` | Las URLs se incrustan al construir. Cambia `.env` y ejecuta `docker compose up -d --build app-landing` |
| Falla `npm ci` o `mvnw` durante el build | Sin conexión a internet o `package-lock.json` desincronizado. Revisa el mensaje del paso que falló |
