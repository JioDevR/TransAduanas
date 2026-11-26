# Guía de compilación — TransAduanas (SGA)

Cómo compilar la aplicación completa y obtener los artefactos listos para desplegar. El orden es: **backend** → **frontends**.

| # | Componente | Directorio | Artefacto generado |
| :-: | :-- | :-- | :-- |
| 1 | Backend API | [`backend-sga/`](../backend-sga) | `backend-sga/target/sga-backend-<versión>.jar` |
| 2 | SGA interno | [`app-sga/`](../app-sga) | `app-sga/dist/` |
| 3 | Portal B2B | [`app-b2b/`](../app-b2b) | `app-b2b/dist/` |
| 4 | Landing pública | [`app-landing/`](../app-landing) | `app-landing/dist/` |

## Índice

- [0. Requisitos previos](#0-requisitos-previos)
- [1. Backend (JAR)](#1-backend-jar)
- [2. Frontends](#2-frontends)
  - [2.1 Variables de entorno de compilación](#21-variables-de-entorno-de-compilación)
  - [2.2 SGA interno](#22-sga-interno-app-sga)
  - [2.3 Portal B2B](#23-portal-b2b-app-b2b)
  - [2.4 Landing](#24-landing-app-landing)
  - [2.5 Previsualizar los builds](#25-previsualizar-los-builds)
- [3. Compilar todo de una vez](#3-compilar-todo-de-una-vez)
- [4. Checklist de verificación](#4-checklist-de-verificación)
- [5. Solución de problemas](#5-solución-de-problemas)

---

## 0. Requisitos previos

| Herramienta | Versión | Verificación |
| :-- | :-: | :-- |
| Java JDK | 21 | `java -version` |
| Node.js | 20 LTS o superior | `node -v` |
| npm | 9 o superior | `npm -v` |

> [!NOTE]
> No necesitas Maven instalado: el backend incluye el wrapper `mvnw` (`mvnw.cmd` en Windows). La primera compilación descarga dependencias, por lo que requiere conexión a internet.

> [!TIP]
> Si `java -version` no muestra la versión 21, apunta `JAVA_HOME` al JDK 21 antes de compilar.

---

## 1. Backend (JAR)

Genera un JAR ejecutable (Spring Boot, Tomcat embebido) con todas las dependencias. La versión del artefacto está definida en [`backend-sga/pom.xml`](../backend-sga/pom.xml) (`<version>`).

### Paso 1: Entrar al directorio

```bash
cd backend-sga
```

### Paso 2: Permisos del wrapper (solo Linux / macOS, primera vez)

```bash
chmod +x mvnw
```

### Paso 3: Verificar Java y Maven Wrapper

```bash
./mvnw -version          # Linux / macOS
.\mvnw.cmd -version      # Windows
```

### Paso 4: Compilar y empaquetar

**Opción A — con pruebas (recomendada):**

```bash
./mvnw clean package          # Linux / macOS
.\mvnw.cmd clean package      # Windows
```

**Opción B — sin pruebas** (cuando ya las validaste):

```bash
./mvnw clean package -DskipTests          # Linux / macOS
.\mvnw.cmd clean package -DskipTests      # Windows
```

Las pruebas usan H2 en memoria, por lo que **no requieren MySQL activo**.

### Paso 5: Verificar el artefacto

Al terminar debe aparecer `BUILD SUCCESS`.

```bash
ls -lh target/*.jar
```

| Archivo | Descripción |
| :-- | :-- |
| `target/sga-backend-<versión>.jar` | JAR ejecutable con dependencias. Es el que se despliega. |
| `target/sga-backend-<versión>.jar.original` | JAR sin dependencias embebidas. No se usa. |

### Paso 6 (opcional): Probar el JAR

```bash
java -jar target/sga-backend-<versión>.jar
```

La configuración se toma de `application-local.properties` (ubicado en el directorio desde donde ejecutas el comando, basado en [`application-local.properties.example`](../backend-sga/application-local.properties.example)) o de variables de entorno:

| Variable | Descripción |
| :-- | :-- |
| `SERVER_PORT` | Puerto HTTP (por defecto `8085`) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a MySQL |
| `JWT_SECRET` | Clave de firma de los JWT. **Cámbiala en producción** |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos de los frontends |
| `STORAGE_DIR` | Carpeta de archivos subidos |

Comprobación: `http://localhost:8085/api/v1/health`.

---

## 2. Frontends

Las tres aplicaciones son proyectos Vue 3 + Vite **independientes**: no hay workspaces en la raíz, así que los comandos se ejecutan dentro de cada carpeta.

### 2.1 Variables de entorno de compilación

Las variables `VITE_*` se **incrustan al compilar**. Si el destino no es `localhost`, defínelas en un archivo `.env.production` dentro de la carpeta de cada app **antes** de `npm run build`.

| Aplicación | Variable | Valor por defecto | Uso |
| :-- | :-- | :-- | :-- |
| `app-sga` | `VITE_API_URL` | `http://localhost:8085/api/v1` | URL base de la API |
| `app-sga` | `VITE_PORTAL_URL` | `http://localhost:5174` | Enlace al Portal B2B en mensajes de WhatsApp |
| `app-b2b` | `VITE_API_URL` | `/api/v1` | URL base de la API (ruta relativa: requiere proxy inverso `/api`) |
| `app-landing` | `VITE_SGA_APP_URL` | `http://localhost:5180` | Enlace al SGA interno |
| `app-landing` | `VITE_PORTAL_CLIENTE_URL` | `http://localhost:5174` | Enlace al Portal B2B |

Ejemplo (`app-sga/.env.production`):

```properties
VITE_API_URL=https://sga.midominio.pe/api/v1
VITE_PORTAL_URL=https://portal.midominio.pe
```

### 2.2 SGA interno (`app-sga`)

```bash
cd app-sga
npm ci                # instala exactamente lo del lockfile
npm test              # opcional: pruebas unitarias (Vitest)
npm run build
```

Salida: `app-sga/dist/`.

### 2.3 Portal B2B (`app-b2b`)

```bash
cd app-b2b
npm ci
npm test              # opcional
npm run build
```

Salida: `app-b2b/dist/`.

### 2.4 Landing (`app-landing`)

```bash
cd app-landing
npm ci
npm run build
```

Salida: `app-landing/dist/`. Puedes partir de [`.env.example`](../app-landing/.env.example) para definir las URLs de destino.

### 2.5 Previsualizar los builds

Para comprobar el contenido de `dist/` antes de publicarlo, ejecuta en cualquiera de las tres carpetas:

```bash
npm run preview
```

---

## 3. Compilar todo de una vez

Desde la raíz del repositorio (Linux / macOS). El orden es backend primero y el script se detiene ante el primer error:

```bash
set -e
(cd backend-sga && ./mvnw clean package -DskipTests)
(cd app-sga     && npm ci && npm run build)
(cd app-b2b     && npm ci && npm run build)
(cd app-landing && npm ci && npm run build)
```

En Windows (PowerShell):

```powershell
$ErrorActionPreference = 'Stop'
Push-Location backend-sga; .\mvnw.cmd clean package -DskipTests; Pop-Location
foreach ($app in 'app-sga','app-b2b','app-landing') {
  Push-Location $app; npm ci; npm run build; Pop-Location
}
```

> [!NOTE]
> En PowerShell, un comando externo que falla no siempre detiene el script. Si necesitas cortar ante errores, revisa `$LASTEXITCODE` después de cada paso.

---

## 4. Checklist de verificación

- [ ] `backend-sga/target/sga-backend-<versión>.jar` existe.
- [ ] `app-sga/dist/index.html`, `app-b2b/dist/index.html` y `app-landing/dist/index.html` existen.
- [ ] Las variables `VITE_*` apuntan al entorno de destino y no a `localhost`.
- [ ] El JAR arranca y `GET /api/v1/health` responde.

---

## 5. Solución de problemas

| Problema | Causa probable | Solución |
| :-- | :-- | :-- |
| `Permission denied: ./mvnw` | Falta permiso de ejecución | `chmod +x mvnw` dentro de `backend-sga/` |
| `release version 21 not supported` | `JAVA_HOME` apunta a un JDK anterior | Instala JDK 21 y ajusta `JAVA_HOME` |
| Errores de getters, setters o `@Builder` | Falló el procesamiento de Lombok | `./mvnw clean compile` para regenerar las clases |
| Fallan pruebas del backend en `package` | Código o prueba rota | Corrige la prueba, o usa `-DskipTests` solo si ya las validaste |
| `vite: command not found` | No se instalaron dependencias en esa carpeta | `npm ci` dentro de la app |
| `npm ci` falla por lockfile | `package.json` y `package-lock.json` desincronizados | Ejecuta `npm install` y versiona el lockfile actualizado |
| Peticiones `/api/v1` fallan en producción | `VITE_API_URL` apunta a `localhost` o falta el proxy `/api` | Define `VITE_API_URL` y recompila, o configura el proxy inverso |
| Pantalla en blanco al recargar una ruta | El servidor no redirige al `index.html` | En Nginx: `try_files $uri $uri/ /index.html;` |
