# 2. Inventario de Dependencias y Librerías de `app-portal-cliente`

**Sistema de Gestión Aduanera - Transaduanas (`app-portal-cliente`)**  
*Análisis Técnico y Justificación de Componentes de `package.json`*

---

## Índice del Capítulo
1. [Inventario General de Librerías](#1-inventario-general-de-librerías)
2. [Catálogo Detallado de Dependencias](#2-catálogo-detallado-de-dependencias)
   - [2.1 Vue 3 & Vite 6 (Núcleo de la Aplicación)](#21-vue-3--vite-6-núcleo-de-la-aplicación)
   - [2.2 Vue Router & Pinia (Navegación y Sesión B2B)](#22-vue-router--pinia-navegación-y-sesión-b2b)
   - [2.3 PrimeVue 4 & PrimeIcons (Componentes Visuales B2B)](#23-primevue-4--primeicons-componentes-visuales-b2b)
   - [2.4 Axios (Comunicación con la API REST)](#24-axios-comunicación-con-la-api-rest)
   - [2.5 Vitest (Pruebas Unitarias de Stores y Servicios)](#25-vitest-pruebas-unitarias-de-stores-y-servicios)
3. [Configuración del Servidor y Proxy (`vite.config.js`)](#3-configuración-del-servidor-y-proxy-viteconfigjs)

---

## 1. Inventario General de Librerías

El archivo [`app-portal-cliente/package.json`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/package.json) define las siguientes dependencias:

| Librería | Versión | Rol en el Portal B2B |
| :--- | :---: | :--- |
| **`vue`** | `^3.5.13` | Framework reactivo de interfaz con sintaxis concisa `<script setup>`. |
| **`vite`** | `^6.2.0` | Servidor ultrarrápido de desarrollo local y empaquetador Rollup. |
| **`@vitejs/plugin-vue`** | `^5.2.1` | Plugin oficial para compilar Single File Components (`.vue`). |
| **`vue-router`** | `^4.5.0` | Enrutador SPA con guardias de seguridad para cambio obligatorio de contraseña. |
| **`pinia`** | `^3.0.1` | Almacén de estado reactivo para credenciales, RUC de la empresa y token JWT. |
| **`primevue`** | `^4.3.1` | Suite de componentes visuales (diálogos modales, tablas, badges de estado). |
| **`@primevue/themes`** | `^4.5.4` | Sistema de estilos base (Preset Aura adaptado a Warm Editorial). |
| **`primeicons`** | `^7.0.0` | Iconografía vectorial SVG para aduanas, documentos y finanzas. |
| **`axios`** | `^1.7.9` | Cliente HTTP con interceptores automáticos para Bearer token y manejo de 401. |
| **`vitest`** | `^5.0.5` | Motor de pruebas unitarias automatizadas para stores y funciones auxiliares. |

---

## 2. Catálogo Detallado de Dependencias

### 2.1 Vue 3 & Vite 6 (Núcleo de la Aplicación)

#### `vue` (^3.5.13)
- **¿Para qué sirve?** Renderiza de forma fluida el catálogo de cotizaciones, la línea de tiempo de expedientes y el formulario de pagos bancarios.
- **Detalle Técnico:** Emplea reactividad nativa mediante *Proxies*, ciclo de vida de componentes (`onMounted`, `computed`, `ref`) y enlace bidireccional (`v-model`).

#### `vite` (^6.2.0) & `@vitejs/plugin-vue` (^5.2.1)
- **¿Para qué sirve?** Levanta el servidor local en el puerto `5174` en menos de 300 ms y recompila los componentes al instante cada vez que se guarda un archivo.

---

### 2.2 Vue Router & Pinia (Navegación y Sesión B2B)

#### `vue-router` (^4.5.0)
- **¿Para qué sirve?** Conduce al cliente por las pantallas de `/cotizaciones`, `/expedientes`, `/pagos`, `/pagos/registrar` y `/notificaciones`.
- **Regla Especial de Dominio:** Si una empresa inicia sesión por primera vez con sus credenciales temporales, el router intercepta cualquier navegación y la fuerza a permanecer en `/cambiar-password-obligatorio` hasta que el usuario establezca su contraseña definitiva.

#### `pinia` (^3.0.1)
- **¿Para qué sirve?** Guarda en la memoria del navegador los datos de la empresa:
  - RUC de la empresa (`username`).
  - Razón social y representante legal.
  - Token JWT y bandera `debeCambiarPassword`.

---

### 2.3 PrimeVue 4 & PrimeIcons (Componentes Visuales B2B)

#### `primevue` (^4.3.1) & `@primevue/themes` (^4.5.4)
- **¿Para qué sirve?** Entrega componentes ya optimizados para la experiencia de autoservicio corporativo:
  - Diálogos modales accesibles para confirmación de pagos y vista previa de documentos.
  - Indicadores visuales (*Tag/Badge*) para los canales de la SUNAT (Verde, Naranja, Rojo).
  - Componentes de carga de archivos para vouchers bancarios de hasta 10 MB.

#### `primeicons` (^7.0.0)
- **¿Para qué sirve?** Proporciona la iconografía de la interfaz (`pi-file-pdf` para facturas, `pi-check-circle` para levantes autorizados, `pi-exclamation-triangle` para aforos físicos).

---

### 2.4 Axios (Comunicación con la API REST)

#### `axios` (^1.7.9)
- **¿Para qué sirve?** Realiza las consultas y envíos de datos hacia los endpoints de `/api/v1/portal/...`.
- **Comportamiento:** Si un cliente B2B pasa más de 24 horas inactivo, el interceptor de Axios detecta el código `401 Unauthorized` del backend, borra las credenciales en caché y lo envía a la pantalla de login con un aviso amigable.

---

### 2.5 Vitest (Pruebas Unitarias de Stores y Servicios)

#### `vitest` (^5.0.5)
- **¿Para qué sirve?** Ejecuta pruebas automáticas de la lógica de sesión de `authStore`, validando que no se guarden tokens corruptos y que el RUC sea debidamente formateado antes de enviarlo al servidor.

---

## 3. Configuración del Servidor y Proxy (`vite.config.js`)

El archivo [`app-portal-cliente/vite.config.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/vite.config.js) configura el entorno de ejecución:

```javascript
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5174,        // Puerto dedicado para el Portal B2B
    host: '0.0.0.0',   // Permite acceso desde la red local o dispositivos móviles
    proxy: {
      '/api': {
        target: 'http://localhost:8085', // Redirige las peticiones hacia el backend Spring Boot
        changeOrigin: true
      }
    }
  },
  test: {
    globals: true,
    environment: 'node'
  }
})
```
