# 3. Núcleo, Autenticación y Enrutamiento de `app-sga`

**Sistema de Gestión Aduanera - Transaduanas (`app-sga`)**  
*Manejo de Sesión, Interceptores HTTP, Stores de Pinia y Guardias de Seguridad*

---

## Índice del Capítulo
1. [El Núcleo del Frontend (`core/`)](#1-el-núcleo-del-frontend-core)
2. [Cliente HTTP con Interceptores (`apiClient.js`)](#2-cliente-http-con-interceptores-apiclientjs)
   - [2.1 Inyección Automática del Token Bearer](#21-inyección-automática-del-token-bearer)
   - [2.2 Manejo de Expiración de Sesión (HTTP 401)](#22-manejo-de-expiración-de-sesión-http-401)
3. [Definición de Roles y Rutas Base (`roles.js`)](#3-definición-de-roles-y-rutas-base-rolesjs)
4. [Gestión de Estado Global (Stores de Pinia)](#4-gestión-de-estado-global-stores-de-pinia)
   - [4.1 `authStore`: Control de Sesión y Credenciales](#41-authstore-control-de-sesión-y-credenciales)
   - [4.2 `navigationStore`: Módulo Activo y Sidebar](#42-navigationstore-módulo-activo-y-sidebar)
5. [Enrutamiento y Guardias de Navegación (`router/index.js`)](#5-enrutamiento-y-guardias-de-navegación-routerindexjs)
   - [5.1 Guardia Global `router.beforeEach`](#51-guardia-global-routerbeforeeach)
   - [5.2 Estructura de Rutas por Dominio](#52-estructura-de-rutas-por-dominio)
6. [El Shell Maestro Contenedor (`AppLayout.vue`)](#6-el-shell-maestro-contenedor-applayoutvue)

---

## 1. El Núcleo del Frontend (`core/`)

La carpeta [`app-sga/src/core/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/core/) contiene los servicios horizontales indispensables para que toda la aplicación funcione:
- La conexión con el backend mediante Axios.
- La tabla de roles y rutas asignadas.
- La shell visual común que envuelve a todas las páginas (*Layout*).
- El enrutador que decide quién puede ver cada pantalla.

---

## 2. Cliente HTTP con Interceptores (`apiClient.js`)

Todas las comunicaciones con la API REST del backend pasan obligatoriamente por la instancia de Axios centralizada en [`app-sga/src/core/api/apiClient.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/core/api/apiClient.js).

### 2.1 Inyección Automática del Token Bearer
Para que los desarrolladores no tengan que escribir manualmente el token de seguridad en cada petición, Axios intercepta cada solicitud antes de que salga hacia internet:

```javascript
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sga_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)
```

### 2.2 Manejo de Expiración de Sesión (HTTP 401)
Si el usuario deja la computadora desatendida y su token JWT de 24 horas caduca, el backend responde con `401 Unauthorized`. El interceptor de respuesta captura este evento automáticamente:

```javascript
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Limpiar datos de sesión en localStorage
      localStorage.removeItem('sga_token')
      localStorage.removeItem('sga_user')
      // Redirigir de inmediato al login sin recargar la pantalla
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
```

---

## 3. Definición de Roles y Rutas Base (`roles.js`)

El archivo [`app-sga/src/core/auth/roles.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/core/auth/roles.js) estandariza los 8 roles institucionales y asocia a cada uno su ruta de aterrizaje (*Home Path*):

| Constante de Rol | Etiqueta Visual | Ruta de Inicio por Defecto (`homePath`) |
| :--- | :--- | :--- |
| `ADMINISTRADOR_TI` | Administrador TI | `/administracion-ti/dashboard` |
| `GERENTE_GENERAL` | Gerente General | `/gerencia-general/dashboard` |
| `AGENTE_COMERCIAL` | Agente Comercial | `/recepcion-comercial` |
| `SUPERVISOR_ADUANERO`| Supervisor Aduanero | `/gestion-aduanera` |
| `AGENTE_ADUANERO` | Agente Aduanero | `/agente-aduanero` |
| `SUPERVISOR_TRANSPORTE`| Supervisor de Transporte | `/transporte-flota` |
| `CONTADOR` | Contador | `/finanzas` |
| `ASESOR_LEGAL` | Asesor Legal | `/legal-auditoria` |

---

## 4. Gestión de Estado Global (Stores de Pinia)

### 4.1 `authStore`: Control de Sesión y Credenciales
Ubicado en [`app-sga/src/features/auth/stores/authStore.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/features/auth/stores/authStore.js):

- **Estado (`State`):**
  - `token`: Cadena JWT almacenada de forma persistente en `localStorage`.
  - `user`: Objeto con nombres, apellidos, correo institucional, sede y rol.
- **Getters Reactivos:**
  - `isAuthenticated`: Retorna `true` si hay un token válido.
  - `userRole`: Retorna la clave del rol (ej. `ROLE_AGENTE_COMERCIAL`).
  - `homePath`: Retorna la URL a la que debe redirigirse al usuario al iniciar sesión.
- **Acciones (`Actions`):**
  - `login(username, password)`: Envía las credenciales a `/api/v1/auth/login`, guarda el token y perfil, y devuelve la respuesta.
  - `logout()`: Elimina el token de `localStorage`, resetea el estado en memoria y navega a `/login`.

### 4.2 `navigationStore`: Módulo Activo y Sidebar
Ubicado en [`app-sga/src/core/stores/navigationStore.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/core/stores/navigationStore.js):

- **Estado:**
  - `activeModule`: Identificador del módulo abierto (ej. `comercial`, `aduanas`, `finanzas`).
  - `activeSubOption`: Pestaña interna seleccionada (ej. `cotizaciones`, `clientes`, `flota`).
  - `sidebarCollapsed`: Estado visual de la barra de navegación lateral (expandida o modo iconos compactos).
- **Sincronización:** Mantiene sincronizada la barra de navegación con la URL actual del navegador, evitando que el menú resalte una opción incorrecta al usar los botones "Atrás" o "Adelante" del navegador.

---

## 5. Enrutamiento y Guardias de Navegación (`router/index.js`)

El archivo [`app-sga/src/core/router/index.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/core/router/index.js) configura el enrutador con historial HTML5 (`createWebHistory()`).

### 5.1 Guardia Global `router.beforeEach`
Antes de que cualquier página se renderice en pantalla, el guardia global ejecuta una verificación en 4 pasos:

1. **Título Dinámico:** Actualiza la pestaña del navegador con `document.title = to.meta.title`.
2. **Control de Entrada a Raíz (`/`):** Si el usuario entra a la URL raíz sin ruta, el router evalúa: si no está autenticado lo envía a `/login`; si está autenticado, lo redirige inmediatamente a su pantalla de trabajo (`authStore.homePath`).
3. **Protección de Rutas Autenticadas:** Si una ruta tiene `meta.requiresAuth = true` y no hay sesión activa, redirige a `/login` guardando la URL solicitada en la variable `?redirect=...` para regresar a ella tras autenticarse con éxito.
4. **Verificación Estricta de Permisos por Rol:**
   ```javascript
   if (to.meta.roles && to.meta.roles.length > 0) {
     const userRole = authStore.userRole
     if (!to.meta.roles.includes(userRole)) {
       // El usuario no tiene permiso: redirigir a su propio homePath
       return next(authStore.homePath)
     }
   }
   ```
5. **Sincronización con el Menú:** Notifica a `navigationStore` qué módulo debe marcarse como activo en la barra lateral.

### 5.2 Estructura de Rutas por Dominio
Para mantener el archivo principal limpio, las rutas se importan modularmente:
- `authRoutes`: Pantallas públicas de inicio de sesión (`/login`).
- `adminRoutes`: Rutas exclusivas del Administrador TI (`/administracion-ti/*`).
- `gerenciaRoutes`: Cuadros de mando y auditoría (`/gerencia-general/*`).
- `rolesRoutes`: Rutas operativas de los roles de negocio ([`roles.routes.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/core/router/roles.routes.js)).

---

## 6. El Shell Maestro Contenedor (`AppLayout.vue`)

Todas las pantallas operativas se renderizan dentro de [`app-sga/src/core/layouts/AppLayout.vue`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/core/layouts/AppLayout.vue):

```
┌────────────────────────────────────────────────────────────────────────┐
│  BARRA SUPERIOR (Topbar)                                               │
│  [Logo SGA] [Buscador Rápido]       [Modo Oscuro] [Notificaciones] [Usuario]│
├───────────────┬────────────────────────────────────────────────────────┤
│ BARRA LATERAL │ CONTENIDO DINÁMICO (<router-view>)                     │
│ (AppSidebar)  │                                                        │
│               │  Aquí se renderiza cada vista:                         │
│  - Comercial  │  - Cotizador de 4 pasos                                │
│  - Aduanas    │  - Expedientes y Canales DAM                           │
│  - Almacén    │  - Depósito y Báscula VGM                              │
│  - Transporte │  - Despachos y Flota MTC                               │
│  - Finanzas   │  - Validación de Pagos y Liquidaciones                 │
│  - Legal      │                                                        │
│  - Ajustes    │                                                        │
└───────────────┴────────────────────────────────────────────────────────┘
```

- **Topbar:** Muestra el nombre del colaborador, su sede de operaciones, selector de contexto (para roles de supervisión como Administrador TI y Gerente General) y el conmutador de **Modo Claro / Modo Oscuro**.
- **Sidebar Reactivo (`AppSidebar.vue`):** Lee el rol del usuario y muestra únicamente las secciones a las que tiene acceso legal y operativo.
