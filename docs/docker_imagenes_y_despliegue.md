# Guía Paso a Paso: Compilación en Imagen Docker y Orquestación con Docker Compose

Documentación paso a paso para **compilar directamente dentro de las imágenes** (Multi-Stage Builds) el backend Java y las 3 aplicaciones frontend, y levantar todo el ecosistema junto a MySQL 8.4 mediante `docker-compose.yml`.

> [!TIP]
> Con este enfoque **no necesitas instalar Java, Maven ni Node.js en el servidor o máquina destino**: Docker (o Podman) compila el `.jar` y los `dist/` dentro de una etapa temporal y genera imágenes finales ligeras solo con lo necesario para ejecutar.

---

## 1. Requisitos Previos

Solo necesitas tener instalado **Docker** (o **Podman**):

```bash
# Con Docker
docker --version
docker compose version

# O con Podman
podman --version
podman compose version
```

> [!NOTE]
> Todas las imágenes base de esta guía usan nombres completos de registro (`docker.io/library/...`), por lo que funcionan igual en **Docker** y en **Podman**.

---

## 2. Paso 1: Crear los `.dockerignore`

Antes de construir las imágenes, evita que se copien carpetas pesadas o credenciales locales al contexto de build.

### 2.1 `backend/.dockerignore`

```gitignore
target/
logs/
uploads/
application-local.properties
.git/
```

### 2.2 `.dockerignore` para `app-sga/`, `app-portal-cliente/` y `landing/`

Crea el mismo archivo `.dockerignore` dentro de cada una de las 3 carpetas frontend:

```gitignore
node_modules/
dist/
.git/
```

---

## 3. Paso 2: `Dockerfile` del Backend Java (Multi-Stage)

Crea el archivo `backend/Dockerfile`. La etapa `builder` compila `sga-backend-0.5.0.jar` usando el Maven Wrapper del proyecto (`mvnw`) y JDK 21; la etapa final copia únicamente el `.jar` sobre JRE 21 Alpine:

```dockerfile
# =============================================================================
# Etapa 1: Compilación del .jar con JDK 21 + Maven Wrapper
# =============================================================================
FROM docker.io/library/eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copiar descriptor Maven y wrapper primero para aprovechar la caché de capas
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copiar código fuente y compilar el JAR ejecutable
COPY src src
RUN ./mvnw clean package -DskipTests

# =============================================================================
# Etapa 2: Imagen final ligera solo con JRE 21
# =============================================================================
FROM docker.io/library/eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el JAR generado en la etapa anterior
COPY --from=builder /build/target/sga-backend-0.5.0.jar app.jar

# Directorios para almacenamiento de documentos (STORAGE_DIR) y logs
RUN mkdir -p /app/uploads/documentos /app/logs

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

---

## 4. Paso 3: `nginx.conf` y `Dockerfile` de los Frontends (Multi-Stage)

La etapa `builder` instala dependencias con `npm ci` y compila el bundle estático (`npm run build`); la etapa final sirve `dist/` con Nginx 1.27 Alpine.

### 4.1 `app-sga/nginx.conf` y `app-portal-cliente/nginx.conf`

Crea el archivo `nginx.conf` dentro de `app-sga/` y dentro de `app-portal-cliente/` con el siguiente contenido (incluye proxy reverso `/api/` hacia `sga-backend:8085` y fallback SPA):

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # Proxy reverso hacia el contenedor del backend
    location /api/ {
        proxy_pass http://sga-backend:8085/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 100M;
    }

    # Fallback SPA para Vue Router (createWebHistory)
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 4.2 `app-sga/Dockerfile`

En [`app-sga/src/core/api/apiClient.js`](../app-sga/src/core/api/apiClient.js), `VITE_API_URL` por defecto apunta a `http://localhost:8085/api/v1`. Al pasarlo como `ARG VITE_API_URL=/api/v1` durante la compilación en Docker, las peticiones pasan limpiamente por el proxy de Nginx:

```dockerfile
# Etapa 1: Compilación con Node 22
FROM docker.io/library/node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

# Etapa 2: Servidor estático Nginx
FROM docker.io/library/nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```

### 4.3 `app-portal-cliente/Dockerfile`

En [`app-portal-cliente/src/core/api/apiClient.js`](../app-portal-cliente/src/core/api/apiClient.js), `VITE_API_URL` ya usa `/api/v1` por defecto, pero se mantiene el `ARG` por si necesitas personalizarlo:

```dockerfile
# Etapa 1: Compilación con Node 22
FROM docker.io/library/node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

# Etapa 2: Servidor estático Nginx
FROM docker.io/library/nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```

### 4.4 `landing/nginx.conf` y `landing/Dockerfile`

Crea `landing/nginx.conf`:

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

Crea `landing/Dockerfile` (incluye los `ARG` de [`landing/src/views/LandingView.vue`](../landing/src/views/LandingView.vue) para enlazar hacia SGA y Portal Cliente):

```dockerfile
# Etapa 1: Compilación con Node 22
FROM docker.io/library/node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_SGA_APP_URL=http://localhost:5180
ARG VITE_PORTAL_CLIENTE_URL=http://localhost:5174
ENV VITE_SGA_APP_URL=${VITE_SGA_APP_URL}
ENV VITE_PORTAL_CLIENTE_URL=${VITE_PORTAL_CLIENTE_URL}
RUN npm run build

# Etapa 2: Servidor estático Nginx
FROM docker.io/library/nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```

---

## 5. Paso 4: Archivo `docker-compose.yml` en la Raíz

Crea `docker-compose.yml` en la raíz del proyecto integrando la base de datos de [`db/docker-compose.yml`](../db/docker-compose.yml) junto con la compilación (`build`) de los 4 servicios:

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
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-123gato456}
      MYSQL_DATABASE: SGAdb
      MYSQL_USER: ${DB_USERNAME:-admin}
      MYSQL_PASSWORD: ${DB_PASSWORD:-123gato456}
    ports:
      - "3306:3306"
    volumes:
      - sga-mysql-data:/var/lib/mysql
      - ./db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro
      - ./db/seed.sql:/docker-entrypoint-initdb.d/02-seed.sql:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 10

  sga-backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    image: transaduanas/sga-backend:0.5.0
    container_name: sga-backend
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SERVER_PORT: 8085
      DB_URL: jdbc:mysql://mysql:3306/SGAdb?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true
      DB_USERNAME: ${DB_USERNAME:-admin}
      DB_PASSWORD: ${DB_PASSWORD:-123gato456}
      JWT_SECRET: ${JWT_SECRET:-sga_platform_secret_key_super_segura_2026_jwt_token_auth}
      CORS_ALLOWED_ORIGINS: http://localhost:5180,http://localhost:5174,http://localhost:5173
      STORAGE_DIR: /app/uploads/documentos
    ports:
      - "8085:8085"
    volumes:
      - sga-uploads:/app/uploads/documentos

  app-sga:
    build:
      context: ./app-sga
      dockerfile: Dockerfile
      args:
        VITE_API_URL: /api/v1
    image: transaduanas/app-sga:1.3.1
    container_name: sga-frontend
    restart: unless-stopped
    depends_on:
      - sga-backend
    ports:
      - "5180:80"

  app-portal-cliente:
    build:
      context: ./app-portal-cliente
      dockerfile: Dockerfile
      args:
        VITE_API_URL: /api/v1
    image: transaduanas/app-portal-cliente:1.3.1
    container_name: sga-portal-cliente
    restart: unless-stopped
    depends_on:
      - sga-backend
    ports:
      - "5174:80"

  landing:
    build:
      context: ./landing
      dockerfile: Dockerfile
      args:
        VITE_SGA_APP_URL: http://localhost:5180
        VITE_PORTAL_CLIENTE_URL: http://localhost:5174
    image: transaduanas/landing:1.3.1
    container_name: sga-landing
    restart: unless-stopped
    ports:
      - "5173:80"

volumes:
  sga-mysql-data:
    name: sga-mysql-data
  sga-uploads:
    name: sga-uploads
```

---

## 6. Paso 5: Compilar las Imágenes (Backend + Frontends) y Levantar Todo

Desde la raíz del repositorio puedes compilar las **4 imágenes** (el backend Java y los 3 frontends Vue/Vite) en un solo paso o servicio por servicio:

### Opción A: Compilar y levantar todo en un solo comando

```bash
# Con Docker (compila sga-backend, app-sga, app-portal-cliente y landing, luego levanta los 5 contenedores)
docker compose up -d --build

# O con Podman
podman compose up -d --build
```

### Opción B: Compilar primero cada imagen por separado y luego levantar

```bash
# 1. Compilar imagen del Backend Java (.jar dentro de Docker)
docker compose build sga-backend

# 2. Compilar imágenes de los 3 Frontends (npm ci + npm run build dentro de Docker)
docker compose build app-sga
docker compose build app-portal-cliente
docker compose build landing

# 3. Levantar todos los contenedores (incluyendo MySQL 8.4)
docker compose up -d
```

*(Si usas **Podman**, reemplaza `docker compose` por `podman compose` en los comandos anteriores).*

### Verificar que todo está levantado

```bash
docker compose ps
```

Accesos disponibles en el navegador:

| Servicio | URL |
| :--- | :--- |
| **Landing Page** | `http://localhost:5173` |
| **SGA Operativo** | `http://localhost:5180` |
| **Portal Cliente B2B** | `http://localhost:5174` |
| **Backend API REST** | `http://localhost:8085` |
| **MySQL 8.4 (`SGAdb`)** | `localhost:3306` |

---

## 7. Comandos Útiles de Mantenimiento

```bash
# Ver logs en tiempo real del backend
docker compose logs -f sga-backend

# Recompilar y reiniciar solo un servicio tras cambios de código
docker compose up -d --build sga-backend
docker compose up -d --build app-sga

# Detener todo sin borrar la base de datos ni archivos subidos
docker compose down

# Exportar una imagen individual generada con Podman para cargarla en Docker
podman save --format docker-archive -o sga-backend.tar transaduanas/sga-backend:0.5.0
docker load -i sga-backend.tar
```
