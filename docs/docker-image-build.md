# Guía de imágenes Docker — TransAduanas (SGA)

Cómo construir las imágenes Docker del proyecto paso a paso y levantar el ecosistema completo con Docker Compose. La compilación ocurre **dentro** de las imágenes (multi-stage build), así que no necesitas Java, Maven ni Node.js en la máquina que construye.

> [!IMPORTANT]
> El repositorio **todavía no incluye** los `Dockerfile`, `nginx.conf` ni el `docker-compose.yml` de esta guía. Los pasos 2 a 6 indican qué archivos crear. Para desplegar el sistema con ellos, sigue la [instalación con Docker Compose](instalacion.md#2-instalación-con-docker-compose).

| Imagen | Origen | Base final | Puerto |
| :-- | :-- | :-- | :-: |
| `transaduanas/sga-backend` | [`backend-sga/`](../backend-sga) | Temurin JRE 21 (Alpine) | 8085 |
| `transaduanas/app-sga` | [`app-sga/`](../app-sga) | Nginx 1.27 (Alpine) | 5180 → 80 |
| `transaduanas/app-b2b` | [`app-b2b/`](../app-b2b) | Nginx 1.27 (Alpine) | 5174 → 80 |
| `transaduanas/app-landing` | [`app-landing/`](../app-landing) | Nginx 1.27 (Alpine) | 5173 → 80 |
| MySQL 8.4 | imagen oficial | `mysql:8.4` | 3306 |

## Índice

- [1. Requisitos previos](#1-requisitos-previos)
- [2. Paso 1: Archivos `.dockerignore`](#2-paso-1-archivos-dockerignore)
- [3. Paso 2: Imagen del backend](#3-paso-2-imagen-del-backend)
- [4. Paso 3: Imágenes de los frontends](#4-paso-3-imágenes-de-los-frontends)
- [5. Paso 4: Variables de entorno (`.env`)](#5-paso-4-variables-de-entorno-env)
- [6. Paso 5: `docker-compose.yml`](#6-paso-5-docker-composeyml)
- [7. Paso 6: Construir y levantar](#7-paso-6-construir-y-levantar)
- [8. Verificación](#8-verificación)
- [9. Mantenimiento](#9-mantenimiento)
- [10. Solución de problemas](#10-solución-de-problemas)

---

## 1. Requisitos previos

Solo necesitas Docker con el plugin Compose, o Podman:

```bash
docker --version && docker compose version
# o
podman --version && podman compose version
```

Las imágenes base usan nombres completos (`docker.io/library/...`), así que funcionan igual en Docker y en Podman. Si usas Podman, reemplaza `docker compose` por `podman compose`.

Los comandos se ejecutan desde la **raíz del repositorio**.

---

## 2. Paso 1: Archivos `.dockerignore`

Evitan copiar carpetas pesadas y credenciales locales al contexto de build.

**`backend-sga/.dockerignore`**

```gitignore
target/
logs/
uploads/
application-local.properties
.git/
```

**`app-sga/.dockerignore`, `app-b2b/.dockerignore` y `app-landing/.dockerignore`** (el mismo contenido en las tres):

```gitignore
node_modules/
dist/
.env*
.git/
```

> [!NOTE]
> Si quieres que el `.env.production` de una app se use en el build, quita `.env*` de su `.dockerignore`. Esta guía no lo necesita: pasa las variables como `ARG`.

---

## 3. Paso 2: Imagen del backend

Crea **`backend-sga/Dockerfile`**. La etapa `builder` compila el JAR con JDK 21 y el Maven Wrapper. La etapa final copia solo el JAR sobre un JRE y corre sin privilegios de root.

```dockerfile
# ---------- Etapa 1: compilación ----------
FROM docker.io/library/eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Descriptor y wrapper primero, para aprovechar la caché de capas
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Código fuente y empaquetado
COPY src src
RUN ./mvnw clean package -DskipTests -B \
    && cp target/sga-backend-*.jar /build/app.jar

# ---------- Etapa 2: ejecución ----------
FROM docker.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S sga && adduser -S sga -G sga \
    && mkdir -p /app/uploads/documentos /app/logs \
    && chown -R sga:sga /app

COPY --from=builder --chown=sga:sga /build/app.jar app.jar

ENV LOG_PATH=/app/logs
USER sga
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Puntos clave:

- `cp target/sga-backend-*.jar` evita fijar la versión del `pom.xml`: el Dockerfile no se rompe al subirla. El patrón no coincide con el archivo `.jar.original`.
- Se usa `-DskipTests` porque las pruebas se validan antes (`./mvnw test`). Quítalo si quieres ejecutarlas dentro del build; no necesitan MySQL, usan H2.
- `LOG_PATH` apunta a una carpeta con permisos para el usuario `sga` (lo lee `logback-spring.xml`).

---

## 4. Paso 3: Imágenes de los frontends

Los tres frontends siguen el mismo patrón: `npm ci` + `npm run build` en Node 22 y publicación de `dist/` con Nginx.

> [!NOTE]
> Vite 8 requiere Node 20.19 o superior (o 22.12+). Por eso se usa `node:22-alpine`.

### 4.1 `nginx.conf` con proxy a la API

Crea el mismo archivo en **`app-sga/nginx.conf`** y en **`app-b2b/nginx.conf`**. Reenvía `/api/` al contenedor del backend y activa el fallback para las rutas de la SPA:

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # Proxy inverso hacia el backend
    location /api/ {
        proxy_pass http://sga-backend:8085/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 100M;
    }

    # Fallback SPA (Vue Router en modo history)
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

`client_max_body_size 100M` coincide con el límite de carga del backend (`max-request-size=100MB`).

### 4.2 `nginx.conf` de la landing

La landing no usa la API. Crea **`app-landing/nginx.conf`**:

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 4.3 `Dockerfile` de `app-sga`

`app-sga` usa por defecto `http://localhost:8085/api/v1`. Con `VITE_API_URL=/api/v1` las peticiones pasan por el proxy de Nginx, sin CORS. Crea **`app-sga/Dockerfile`**:

```dockerfile
# ---------- Etapa 1: compilación ----------
FROM docker.io/library/node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_API_URL=/api/v1
ARG VITE_PORTAL_URL=http://localhost:5174
ENV VITE_API_URL=${VITE_API_URL} \
    VITE_PORTAL_URL=${VITE_PORTAL_URL}
RUN npm run build

# ---------- Etapa 2: servidor estático ----------
FROM docker.io/library/nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```

### 4.4 `Dockerfile` de `app-b2b`

`app-b2b` ya usa `/api/v1` por defecto. Se mantiene el `ARG` por si necesitas cambiarlo. Crea **`app-b2b/Dockerfile`**:

```dockerfile
# ---------- Etapa 1: compilación ----------
FROM docker.io/library/node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

# ---------- Etapa 2: servidor estático ----------
FROM docker.io/library/nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```

### 4.5 `Dockerfile` de `app-landing`

Recibe las URLs a las que enlaza. Crea **`app-landing/Dockerfile`**:

```dockerfile
# ---------- Etapa 1: compilación ----------
FROM docker.io/library/node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_SGA_APP_URL=http://localhost:5180
ARG VITE_PORTAL_CLIENTE_URL=http://localhost:5174
ENV VITE_SGA_APP_URL=${VITE_SGA_APP_URL} \
    VITE_PORTAL_CLIENTE_URL=${VITE_PORTAL_CLIENTE_URL}
RUN npm run build

# ---------- Etapa 2: servidor estático ----------
FROM docker.io/library/nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```

> [!WARNING]
> Las variables `VITE_*` se incrustan **al construir**. Si cambias las URLs públicas, debes reconstruir la imagen; reiniciar el contenedor no basta.

---

## 5. Paso 4: Variables de entorno (`.env`)

Compose lee un archivo `.env` en la raíz del repositorio. Crea **`.env.example`** como plantilla y cópialo a `.env`:

```properties
# Base de datos
MYSQL_ROOT_PASSWORD=cambia_esta_clave_root
DB_USERNAME=sga_user
DB_PASSWORD=cambia_esta_clave_app

# Seguridad (usa una cadena larga y aleatoria, mínimo 32 caracteres)
JWT_SECRET=cambia_este_secreto_largo_y_aleatorio

# URLs públicas (las usa la landing para sus enlaces)
SGA_PUBLIC_URL=http://localhost:5180
PORTAL_PUBLIC_URL=http://localhost:5174
```

```bash
cp .env.example .env
```

> [!CAUTION]
> Agrega `.env` al `.gitignore` y no lo subas al repositorio. Usa claves distintas a las del ejemplo en cualquier entorno que no sea tu equipo local. Puedes generar un secreto con `openssl rand -base64 48`.

---

## 6. Paso 5: `docker-compose.yml`

Crea **`docker-compose.yml`** en la raíz. Define MySQL (con `schema.sql` y `seed.sql` cargados en el primer arranque), el backend y los tres frontends:

```yaml
services:
  mysql:
    image: docker.io/library/mysql:8.4
    container_name: sga-mysql
    restart: unless-stopped
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=-05:00
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:?define MYSQL_ROOT_PASSWORD en .env}
      MYSQL_DATABASE: SGAdb
      MYSQL_USER: ${DB_USERNAME:?define DB_USERNAME en .env}
      MYSQL_PASSWORD: ${DB_PASSWORD:?define DB_PASSWORD en .env}
    ports:
      - "3306:3306"
    volumes:
      - sga-mysql-data:/var/lib/mysql
      - ./db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro
      - ./db/seed.sql:/docker-entrypoint-initdb.d/02-seed.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h localhost -p$$MYSQL_ROOT_PASSWORD --silent"]
      interval: 10s
      timeout: 5s
      retries: 10

  sga-backend:
    build:
      context: ./backend-sga
    image: transaduanas/sga-backend:latest
    container_name: sga-backend
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SERVER_PORT: 8085
      DB_URL: jdbc:mysql://mysql:3306/SGAdb?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET:?define JWT_SECRET en .env}
      CORS_ALLOWED_ORIGINS: ${SGA_PUBLIC_URL:-http://localhost:5180},${PORTAL_PUBLIC_URL:-http://localhost:5174}
      STORAGE_DIR: /app/uploads/documentos
      JPA_SHOW_SQL: "false"
    ports:
      - "8085:8085"
    volumes:
      - sga-uploads:/app/uploads/documentos
      - sga-logs:/app/logs

  app-sga:
    build:
      context: ./app-sga
      args:
        VITE_API_URL: /api/v1
        VITE_PORTAL_URL: ${PORTAL_PUBLIC_URL:-http://localhost:5174}
    image: transaduanas/app-sga:latest
    container_name: sga-frontend
    restart: unless-stopped
    depends_on:
      - sga-backend
    ports:
      - "5180:80"

  app-b2b:
    build:
      context: ./app-b2b
      args:
        VITE_API_URL: /api/v1
    image: transaduanas/app-b2b:latest
    container_name: sga-portal-b2b
    restart: unless-stopped
    depends_on:
      - sga-backend
    ports:
      - "5174:80"

  app-landing:
    build:
      context: ./app-landing
      args:
        VITE_SGA_APP_URL: ${SGA_PUBLIC_URL:-http://localhost:5180}
        VITE_PORTAL_CLIENTE_URL: ${PORTAL_PUBLIC_URL:-http://localhost:5174}
    image: transaduanas/app-landing:latest
    container_name: sga-landing
    restart: unless-stopped
    ports:
      - "5173:80"

volumes:
  sga-mysql-data:
  sga-uploads:
  sga-logs:
```

> [!NOTE]
> Los scripts de `/docker-entrypoint-initdb.d/` solo se ejecutan cuando el volumen `sga-mysql-data` está **vacío**. Si cambias `schema.sql` o `seed.sql` después, debes recrear el volumen (ver [Mantenimiento](#9-mantenimiento)).

---

## 7. Paso 6: Construir y levantar

Valida primero que el archivo y el `.env` sean correctos:

```bash
docker compose config --quiet && echo "OK"
```

**Opción A — todo en un comando:**

```bash
docker compose up -d --build
```

**Opción B — construir cada imagen por separado:**

```bash
docker compose build sga-backend
docker compose build app-sga
docker compose build app-b2b
docker compose build app-landing

docker compose up -d
```

La primera construcción tarda varios minutos porque descarga imágenes base y dependencias. Las siguientes aprovechan la caché de capas.

Para etiquetar una versión concreta de una imagen:

```bash
docker tag transaduanas/sga-backend:latest transaduanas/sga-backend:0.5.1
```

---

## 8. Verificación

```bash
docker compose ps
```

El estado esperado es `healthy` para `sga-mysql` y `running` para el resto.

```bash
curl http://localhost:8085/api/v1/health
```

| Servicio | URL |
| :-- | :-- |
| Landing | `http://localhost:5173` |
| SGA interno | `http://localhost:5180` |
| Portal B2B | `http://localhost:5174` |
| API REST | `http://localhost:8085/api/v1` |
| MySQL | `localhost:3306` |

Prueba de extremo a extremo: entra a `http://localhost:5180` con un usuario del seed (ver [credenciales de prueba](instalacion.md#110-credenciales-de-prueba)). Si inicia sesión, el proxy de Nginx, el backend y la base de datos funcionan.

---

## 9. Mantenimiento

```bash
# Logs del backend en tiempo real
docker compose logs -f sga-backend

# Reconstruir y reiniciar un solo servicio tras cambios de código
docker compose up -d --build sga-backend
docker compose up -d --build app-sga

# Detener todo conservando los datos (BD y archivos subidos)
docker compose down

# Reiniciar desde cero: BORRA la base de datos y los archivos subidos
docker compose down -v

# Exportar una imagen para cargarla en otra máquina
docker save -o sga-backend.tar transaduanas/sga-backend:latest
docker load -i sga-backend.tar
# Con Podman: podman save --format docker-archive -o sga-backend.tar ...
```

> [!CAUTION]
> `docker compose down -v` elimina los volúmenes, es decir, todos los datos. Hazlo solo si quieres reiniciar la base con `seed.sql`.

---

## 10. Solución de problemas

| Problema | Causa probable | Solución |
| :-- | :-- | :-- |
| `required variable ... is missing` al ejecutar compose | Falta el archivo `.env` o una variable | `cp .env.example .env` y completa los valores |
| El backend se reinicia en bucle | MySQL aún no está listo, o credenciales distintas entre `.env` y el volumen | Revisa `docker compose logs sga-backend`. Si cambiaste las claves después del primer arranque, recrea el volumen con `down -v` |
| `Access denied for user` | `DB_USERNAME`/`DB_PASSWORD` cambiaron tras inicializar MySQL | Recrea el volumen `sga-mysql-data` |
| La base está vacía | `schema.sql`/`seed.sql` no se ejecutaron porque el volumen ya existía | `docker compose down -v` y vuelve a levantar |
| `502 Bad Gateway` en `/api/` | El backend no está activo o no se llama `sga-backend` | Comprueba `docker compose ps` y el nombre del servicio en `nginx.conf` |
| Error de CORS | La URL del navegador no está en `CORS_ALLOWED_ORIGINS` | Ajusta `SGA_PUBLIC_URL`/`PORTAL_PUBLIC_URL` y reinicia el backend |
| Los enlaces de la landing apuntan a `localhost` | Las URLs se incrustan al construir | Cambia las variables en `.env` y ejecuta `up -d --build app-landing` |
| `npm ci` falla en el build | `package-lock.json` desincronizado | Ejecuta `npm install` localmente y versiona el lockfile |
| `Permission denied` al escribir en `/app/uploads` | El volumen se creó con otro propietario | `docker compose down -v` y recrea, o ajusta el propietario del volumen |
| Puerto ya en uso (`3306`, `8085`, `517x`, `5180`) | Otro servicio local usa el puerto | Detén ese servicio o cambia el puerto publicado en `docker-compose.yml` |
