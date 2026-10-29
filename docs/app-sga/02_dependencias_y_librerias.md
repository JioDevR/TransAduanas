# 2. Inventario de Dependencias y Librerías de `app-sga`

**Sistema de Gestión Aduanera - Transaduanas (`app-sga`)**  
*Explicación Técnica y Funcional del `package.json`*

---

## Índice del Capítulo
1. [¿Qué es `package.json` y npm? (Explicación para no técnicos)](#1-qué-es-packagejson-y-npm-explicación-para-no-técnicos)
2. [Matriz de Dependencias de Producción y Desarrollo](#2-matriz-de-dependencias-de-producción-y-desarrollo)
3. [Catálogo Detallado de Librerías](#3-catálogo-detallado-de-librerías)
   - [3.1 Framework Central: Vue 3.5 & Vite 8](#31-framework-central-vue-35--vite-8)
   - [3.2 Enrutamiento y Estado Global: Vue Router & Pinia](#32-enrutamiento-y-estado-global-vue-router--pinia)
   - [3.3 Suite de Componentes: PrimeVue 4 & PrimeIcons](#33-suite-de-componentes-primevue-4--primeicons)
   - [3.4 Cliente HTTP y Comunicación: Axios](#34-cliente-http-y-comunicación-axios)
   - [3.5 Herramientas de Testing y Calidad: Vitest & Playwright](#35-herramientas-de-testing-y-calidad-vitest--playwright)
4. [Configuración del Empaquetador (`vite.config.js`)](#4-configuración-del-empaquetador-viteconfigjs)

---

## 1. ¿Qué es `package.json` y npm? (Explicación para no técnicos)

### La Analogía de las Piezas Prefabricadas de un Automóvil
Cuando una fábrica automotriz ensambla un vehículo moderno, no inventa los neumáticos de caucho, el vidrio templado de las lunas ni el chip del computador de a bordo; compra piezas certificadas de proveedores líderes mundiales y las ensambla sobre el chasis.

En el desarrollo web moderno:
- **`npm`** (*Node Package Manager*) es el almacén global de componentes de software para JavaScript.
- El archivo [`app-sga/package.json`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/package.json) es el manifiesto técnico donde se especifican exactamente qué librerías probadas utiliza la aplicación interna.
- Cuando ejecutas `npm install`, npm descarga e instala de forma segura cada pieza en la carpeta `node_modules/`.

---

## 2. Matriz de Dependencias de Producción y Desarrollo

| Paquete | Versión en `app-sga` | Rol en la Aplicación |
| :--- | :---: | :--- |
| **`vue`** | `^3.5.42` | Motor reactivo y framework central con Composition API y sintaxis `<script setup>`. |
| **`vite`** | `^8.3.0` | Servidor de desarrollo ultrarrápido (HMR) y empaquetador de producción Rollup. |
| **`@vitejs/plugin-vue`** | `^6.0.8` | Compilador de archivos SFC (*Single File Components* `.vue`) para Vite. |
| **`vue-router`** | `^4.6.4` | Enrutador SPA con soporte de navegación vigilada (*Navigation Guards*) por rol y JWT. |
| **`pinia`** | `^4.0.3` | Almacén de estado global reactivo (sesión en `authStore` y módulos en `navigationStore`). |
| **`primevue`** | `^4.5.5` | Biblioteca de componentes de interfaz (modales accesibles, tablas, selectores, botones). |
| **`@primevue/themes`** | `^4.5.4` | Sistema de diseño base con el preset **Aura** adaptado a la estética *Warm Editorial*. |
| **`primeicons`** | `^8.0.2` | Librería oficial de iconografía vectorial SVG para señalética aduanera y logística. |
| **`axios`** | `^1.20.0` | Cliente de peticiones HTTP con interceptores para inyección de token Bearer y logout ante 401. |
| **`vitest`** | `^5.0.5` | Suite de pruebas unitarias ultrarrápida nativa de Vite compatible con Jest. |
| **`playwright`** | `^1.64.1` | Motor de pruebas End-to-End (E2E) para automatizar navegación real en navegadores Chromium/Firefox. |

---

## 3. Catálogo Detallado de Librerías

### 3.1 Framework Central: Vue 3.5 & Vite 8

#### `vue` (^3.5.42)
- **¿Para qué sirve?** Es el corazón de la interfaz gráfica. Se encarga de que cuando cambie un dato en la pantalla (por ejemplo, el usuario cambia el peso de un contenedor en una cotización), todos los cálculos de flete y arancel se actualicen al instante sin necesidad de recargar la página.
- **Detalle Técnico:** Emplea el motor reactivo basado en *Proxies* de JavaScript moderno, el algoritmo de reconciliación de *Virtual DOM* y la Composition API con `<script setup>`, permitiendo crear componentes modulares y fuertemente tipados.
- **¿Qué pasaría sin ella?** Habría que manipular el DOM manualmente con JavaScript puro (como en los años 2000), requiriendo 10 veces más código y generando interfaces lentas y difíciles de mantener.

#### `vite` (^8.3.0) & `@vitejs/plugin-vue` (^6.0.8)
- **¿Para qué sirve?** Es el motor de arranque y compilación ultrarrápido para los programadores. Permite que cuando un desarrollador modifica una línea de código, el cambio se refleje en el navegador en menos de 50 milisegundos sin perder el estado de la pantalla (*Hot Module Replacement - HMR*).
- **Detalle Técnico:** Sirve el código fuente mediante módulos nativos de JavaScript (*ES Modules* en el navegador) en desarrollo, y en producción genera un paquete altamente optimizado mediante Rollup con división inteligente de código (*code-splitting*).

---

### 3.2 Enrutamiento y Estado Global: Vue Router & Pinia

#### `vue-router` (^4.6.4)
- **¿Para qué sirve?** Controla a qué pantalla puede entrar cada usuario según su URL (por ejemplo, `/recepcion-comercial` o `/gestion-aduanera`). Si un usuario sin rol de contador intenta entrar directamente por la URL a `/finanzas`, el router lo expulsa y lo redirige a su módulo autorizado.
- **Detalle Técnico:** Enrutador oficial de Vue con historial HTML5 (`createWebHistory()`), rutas dinámicas con *lazy-loading* (`() => import(...)`) y un guardia global `router.beforeEach` que evalúa los metadatos de rol (`meta.roles`) contra el rol activo en `authStore`.

#### `pinia` (^4.0.3)
- **¿Para qué sirve?** Es la memoria central compartida de la aplicación. Guarda quién ha iniciado sesión, su token de seguridad, su sede de trabajo y qué pestaña tiene abierta.
- **Detalle Técnico:** Almacén oficial recomendado para Vue 3. Ofrece tipado estricto, persistencia en `localStorage`, integración nativa con Vue DevTools y reactividad directa sin mutaciones engorrosas como las del antiguo Vuex.

---

### 3.3 Suite de Componentes: PrimeVue 4 & PrimeIcons

#### `primevue` (^4.5.5) & `@primevue/themes` (^4.5.4)
- **¿Para qué sirve?** Proporciona componentes visuales avanzados ya listos y accesibles (cuadros de diálogo modales con foco atrapado, tablas con ordenamiento de columnas, calendarios con selección de fechas ETA, selectores de puertos y alertas flotantes *Toast*).
- **Detalle Técnico:** Se utiliza PrimeVue versión 4 configurada en modo *unstyled / styled with preset*, integrando el preset **Aura** adaptado a las variables CSS del lenguaje *Warm Editorial* en `main.js`.
- **¿Qué pasaría sin ella?** Habría que programar desde cero la accesibilidad por teclado (tecla `ESC` para cerrar modales, navegación por pestañas con `Tab`), perdiendo semanas de desarrollo y arriesgando fallos de accesibilidad.

#### `primeicons` (^8.0.2)
- **¿Para qué sirve?** Biblioteca de glifos e iconos vectoriales (`pi-truck` para camiones, `pi-box` para paquetes, `pi-shield` para aduanas, `pi-dollar` para cobros).

---

### 3.4 Cliente HTTP y Comunicación: Axios

#### `axios` (^1.20.0)
- **¿Para qué sirve?** Es el mensajero que envía y recibe información desde el backend de Spring Boot (`http://localhost:8085`).
- **Detalle Técnico:** Instanciado en [`app-sga/src/core/api/apiClient.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/core/api/apiClient.js):
  - **Interceptor de Petición:** Lee el token JWT guardado en `localStorage` y lo inyecta automáticamente en la cabecera `Authorization: Bearer <token>`.
  - **Interceptor de Respuesta:** Si el backend responde con un código `401 Unauthorized` (sesión caducada), Axios limpia los datos de sesión de Pinia y redirige de inmediato a la pantalla de `/login` sin recargar la página.

---

### 3.5 Herramientas de Testing y Calidad: Vitest & Playwright

#### `vitest` (^5.0.5)
- **¿Para qué sirve?** Permite ejecutar pruebas automáticas en milisegundos para comprobar que las matemáticas del cotizador comercial, los cálculos de flete y los guardias de seguridad por roles funcionen siempre de forma correcta sin romper nada tras una actualización.
- **Comando:** `npm run test`

#### `playwright` (^1.64.1)
- **¿Para qué sirve?** Abre navegadores reales de forma automatizada (Chrome, Firefox, Safari) para simular el comportamiento de un usuario humano haciendo clic, llenando formularios de cotización y verificando que los modales se abran y cierren correctamente.

---

## 4. Configuración del Empaquetador (`vite.config.js`)

El archivo [`app-sga/vite.config.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/vite.config.js) incluye optimizaciones clave:

```javascript
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)) // Alias @ para importar desde src
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          // División de librerías para no saturar la descarga inicial del usuario
          if (id.includes('node_modules/vue') || id.includes('node_modules/vue-router') || id.includes('node_modules/pinia')) {
            return 'vendor-vue'
          }
          if (id.includes('node_modules/primevue') || id.includes('node_modules/@primevue')) {
            return 'vendor-primevue'
          }
          if (id.includes('node_modules/axios')) {
            return 'vendor-http'
          }
        }
      }
    }
  },
  server: {
    port: 5180, // Puerto de ejecución de app-sga
    proxy: {
      '/api': {
        target: 'http://localhost:8085', // Redirección automática de llamadas hacia el backend
        changeOrigin: true
      }
    }
  }
})
```

- **División de Chunks (`manualChunks`):** Separa las librerías grandes en archivos independientes (`vendor-vue.js`, `vendor-primevue.js`, `vendor-http.js`). El navegador del usuario solo descarga estas librerías una sola vez y las conserva en caché, haciendo que las visitas posteriores carguen de forma casi instantánea.
- **Proxy Inverso Local:** Todas las peticiones que empiezan con `/api` son redirigidas de forma transparente a `http://localhost:8085`, evitando problemas de orígenes cruzados (CORS) durante el desarrollo local.
