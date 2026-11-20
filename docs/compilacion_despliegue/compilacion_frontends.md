# Guía de Compilación de Frontends y Apps (Vue 3 + Vite)

Guía paso a paso para instalar dependencias, configurar variables de entorno de compilación y generar los bundles de producción (`dist/`) de las aplicaciones frontend de **TransAduanas Platform**.

---

## 1. Inventario de Aplicaciones Frontend

El repositorio contiene **3 aplicaciones** principales basadas en **Vue 3 + Vite**:

| Aplicación | Directorio | Propósito | Puerto Dev | Comando Build | Salida |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **SGA Operativo** | [`app-sga/`](../app-sga) | Sistema interno de gestión aduanera (Vite 8.3.0) | `5180` | `npm run build` | `app-sga/dist/` |
| **Portal Cliente B2B** | [`app-portal-cliente/`](../app-portal-cliente) | Portal web para clientes B2B (Vite 6.2.0) | `5174` | `npm run build` | `app-portal-cliente/dist/` |
| **Landing Page** | [`landing/`](../landing) | Sitio público corporativo y accesos (Vite 6.2.0) | `5173` | `npm run build` | `landing/dist/` |

---

## 2. Requisitos Previos

Verifica que tengas instalados **Node.js** y **npm**:

```bash
node -v
npm -v
```

> [!NOTE]
> Cada aplicación gestiona sus dependencias de forma independiente con su propio `package.json` y `package-lock.json` (no es un monorepo con workspaces en la raíz). Debes ejecutar los comandos dentro de la carpeta de cada aplicación.

---

## 3. Variables de Entorno de Compilación (`VITE_*`)

En Vite, las variables `VITE_*` se incrustan **en tiempo de compilación** (`npm run build`). Si vas a compilar para un servidor distinto a `localhost`, define un archivo `.env.production` (o `.env`) dentro de la carpeta de cada app antes de ejecutar `npm run build`, o pásalas en línea al comando.

| Aplicación | Variable | Valor por Defecto | Archivo donde se lee |
| :--- | :--- | :--- | :--- |
| `app-sga` | `VITE_API_URL` | `http://localhost:8085/api/v1` | [`app-sga/src/core/api/apiClient.js`](../app-sga/src/core/api/apiClient.js) |
| `app-portal-cliente` | `VITE_API_URL` | `/api/v1` | [`app-portal-cliente/src/core/api/apiClient.js`](../app-portal-cliente/src/core/api/apiClient.js) |
| `landing` | `VITE_SGA_APP_URL` | `http://localhost:5180` | [`landing/src/views/LandingView.vue`](../landing/src/views/LandingView.vue) |
| `landing` | `VITE_PORTAL_CLIENTE_URL` | `http://localhost:5174` | [`landing/src/views/LandingView.vue`](../landing/src/views/LandingView.vue) |

---

## 4. Pasos de Compilación por Aplicación

### 4.1 SGA Operativo (`app-sga`)

1. Ingresa al directorio:
   ```bash
   cd app-sga
   ```
2. Instala las dependencias exactas del lockfile:
   ```bash
   npm ci
   ```
   *(Usa `npm install` si estás en desarrollo y necesitas actualizar dependencias).*
3. *(Opcional)* Ejecuta las pruebas unitarias con Vitest:
   ```bash
   npm test
   ```
4. Compila el bundle de producción:
   ```bash
   npm run build
   ```
5. Verifica la salida generada en `app-sga/dist/`:
   - Incluye `index.html` y `assets/` con chunks separados configurados en [`app-sga/vite.config.js`](../app-sga/vite.config.js) (`vendor-vue`, `vendor-primevue`, `vendor-http`).

---

### 4.2 Portal del Cliente B2B (`app-portal-cliente`)

1. Ingresa al directorio:
   ```bash
   cd app-portal-cliente
   ```
2. Instala las dependencias:
   ```bash
   npm ci
   ```
3. *(Opcional)* Ejecuta las pruebas unitarias con Vitest:
   ```bash
   npm test
   ```
4. Compila el bundle de producción:
   ```bash
   npm run build
   ```
5. Verifica la salida generada en `app-portal-cliente/dist/`.

---

### 4.3 Landing Page (`landing`)

1. Ingresa al directorio:
   ```bash
   cd landing
   ```
2. Instala las dependencias:
   ```bash
   npm ci
   ```
3. Compila el bundle de producción:
   ```bash
   npm run build
   ```
4. Verifica la salida generada en `landing/dist/`.

---

## 5. Previsualización Local del Build Generado

Para comprobar que el contenido estático generado en `dist/` funciona correctamente antes de desplegarlo en un servidor web (Nginx, Apache, Caddy, etc.), ejecuta dentro de cualquiera de las 3 carpetas:

```bash
npm run preview
```

---

## 6. Compilación Rápida de Todas las Apps (desde la raíz)

Si deseas compilar las tres aplicaciones principales en secuencia desde la raíz del repositorio:

```bash
(cd app-sga && npm ci && npm run build) && \
(cd app-portal-cliente && npm ci && npm run build) && \
(cd landing && npm ci && npm run build)
```

---

## 7. Solución de Problemas Frecuentes

| Problema | Causa Probable | Solución |
| :--- | :--- | :--- |
| `vite: command not found` | No se instalaron los paquetes en esa carpeta. | Ejecuta `npm ci` (o `npm install`) dentro de la carpeta de la app correspondiente. |
| Peticiones `/api/v1` fallan en producción | `VITE_API_URL` apuntando a `localhost` o sin proxy reverso. | Define `VITE_API_URL` en `.env.production` antes de `npm run build` o configura el proxy `/api` en tu servidor web (Nginx). |
| Pantalla en blanco al recargar rutas SPA | El servidor web no redirige rutas a `index.html`. | Configura fallback SPA (`try_files $uri $uri/ /index.html;` en Nginx). |
