# Documentación Técnica del Frontend — TransAduanas (SGA Frontend SPA)

**Sistema de Gestión Aduanera y Logística Internacional**  
_Documento de Especificación de Arquitectura Frontend, Tecnologías, Patrones de Diseño, Sistema Warm Editorial, Enrutamiento y Catálogo de Módulos_

---

## Índice General

1. [Visión General del Frontend](#1-visión-general-del-frontend)
2. [Arquitectura de Software Frontend](#2-arquitectura-de-software-frontend)
   - [2.1 Arquitectura Basada en Características (Feature-Based Architecture)](#21-arquitectura-basada-en-características-feature-based-architecture)
   - [2.2 Diagrama de Flujo de Datos y Componentes](#22-diagrama-de-flujo-de-datos-y-componentes)
   - [2.3 Integración con el Backend mediante Axios](#23-integración-con-el-backend-mediante-axios)
3. [Patrones de Diseño Implementados](#3-patrones-de-diseño-implementados)
4. [Tecnologías y Stack de Dependencias](#4-tecnologías-y-stack-de-dependencias)
   - [4.1 Matriz de Tecnologías Centrales](#41-matriz-de-tecnologías-centrales)
   - [4.2 Inventario de Dependencias (package.json)](#42-inventario-de-dependencias-packagejson)
5. [Estructura del Proyecto Frontend](#5-estructura-del-proyecto-frontend)
6. [Sistema de Diseño "Warm Editorial" y Modos Visuales](#6-sistema-de-diseño-warm-editorial-y-modos-visuales)
   - [6.1 Tokens de Diseño y Variables CSS](#61-tokens-de-diseño-y-variables-css)
   - [6.2 Tipografía Dual (Inter & JetBrains Mono)](#62-tipografía-dual-inter--jetbrains-mono)
   - [6.3 Rejilla de Espaciado Armónico de 4px](#63-rejilla-de-espaciado-armónico-de-4px)
   - [6.4 Preset Personalizado PrimeVue (SgaPreset)](#64-preset-personalizado-primevue-sgapreset)
   - [6.5 Alternancia Nativa Claro / Oscuro (useTheme)](#65-alternancia-nativa-claro--oscuro-usetheme)
7. [Gestión de Estado Global (Pinia Stores)](#7-gestión-de-estado-global-pinia-stores)
   - [7.1 authStore: Control de Sesión, Rol y Permisos](#71-authstore-control-de-sesión-rol-y-permisos)
   - [7.2 navigationStore: Sincronización Modular y Sub-Opciones](#72-navigationstore-sincronización-modular-y-sub-opciones)
8. [Enrutamiento y Seguridad de Navegación (Vue Router)](#8-enrutamiento-y-seguridad-de-navegación-vue-router)
   - [8.1 Guardias de Navegación Globales (Navigation Guards)](#81-guardias-de-navegación-globales-navigation-guards)
   - [8.2 Matriz de Enrutamiento por Roles Institucionales](#82-matriz-de-enrutamiento-por-roles-institucionales)
9. [Catálogo de Módulos Funcionales (Features)](#9-catálogo-de-módulos-funcionales-features)
   - [9.1 Autenticación (LoginView)](#91-autenticación-loginview)
   - [9.2 Shell Maestro y Navegación (AppLayout & AppSidebar)](#92-shell-maestro-y-navegación-applayout--appsidebar)
   - [9.3 Administración TI y Consola del Sistema](#93-administración-ti-y-consola-del-sistema)
   - [9.4 Gerencia General (Flota, Conductores y Sedes)](#94-gerencia-general-flota-conductores-y-sedes)
   - [9.5 Recepción Comercial (Cotizaciones y Clientes)](#95-recepción-comercial-cotizaciones-y-clientes)
   - [9.6 Gestión Aduanera (Expedientes, DAM y SUNAT)](#96-gestión-aduanera-expedientes-dam-y-sunat)
   - [9.7 Agente Aduanero (Depósito y Almacén VGM)](#97-agente-aduanero-depósito-y-almacén-vgm)
   - [9.8 Transporte y Flota (Despacho y Tracking)](#98-transporte-y-flota-despacho-y-tracking)
   - [9.9 Finanzas (Anticipos, Cobranzas y Liquidación)](#99-finanzas-anticipos-cobranzas-y-liquidación)
   - [9.10 Legal y Auditoría Normativa](#910-legal-y-auditoría-normativa)
   - [9.11 Archivo Documental Digital](#911-archivo-documental-digital)
10. [Biblioteca de Componentes Compartidos y Composables](#10-biblioteca-de-componentes-compartidos-y-composables)
11. [Configuración, Entorno y Puesta en Marcha](#11-configuración-entorno-y-puesta-en-marcha)
12. [Portal del Cliente B2B (portal-cliente)](#12-portal-del-cliente-b2b-portal-cliente)
   - [12.1 Arquitectura y Estructura Modular](#121-arquitectura-y-estructura-modular)
   - [12.2 Consumo de Servicios y Endpoints B2B](#122-consumo-de-servicios-y-endpoints-b2b)
   - [12.3 Sistema de Diseño Warm Editorial y Modo Oscuro](#123-sistema-de-diseño-warm-editorial-y-modo-oscuro)

---

## 1. Visión General del Frontend

El frontend **sga-frontend** es una Single Page Application (SPA) empresarial construida con **Vue 3** y **Vite**, concebida para ofrecer una experiencia de usuario de grado corporativo a los ocho perfiles operativos de **TransAduanas (SGA)**.

### Objetivos y Enfoque del Cliente Web:

1. **Rendimiento Ultrarrápido:** Tiempos de carga inmediatos gracias al empaquetador Vite, división de código por rutas (_code-splitting_ dinámico con `import()`) y renderizado reactivo con la Composition API de Vue 3 (`<script setup>`).
2. **Estética Editorial Contemporánea (Warm Editorial):** Interfaz limpia y distintiva sin librerías CSS sobrecargadas, utilizando tokens personalizados con una paleta cálida (arena, latte y espresso) y soporte bidireccional nativo para **Modo Claro** y **Modo Oscuro**.
3. **Segregación Funcional por Roles:** La barra de navegación lateral (_AppSidebar_) y los accesos de ruta se adaptan automáticamente al rol del usuario autenticado, mostrando únicamente los módulos, bandejas y acciones permitidas.
4. **Resiliencia y Manejo Transparente de Sesión:** Interceptores de Axios que inyectan el token JWT Bearer en cada petición, manejan expiraciones de sesión (HTTP 401) redirigiendo a la pantalla de acceso sin bloqueos en el navegador y presentan notificaciones accesibles (_Toasts_) ante anomalías de red o permisos (HTTP 403).

---

## 2. Arquitectura de Software Frontend

### 2.1 Arquitectura Basada en Características (Feature-Based Architecture)

El cliente frontend implementa el patrón de **Arquitectura Basada en Características (Feature-Based Architecture)**, organizando el código en dominios de negocio autónomos y altamente cohesivos en lugar de agrupar todos los archivos por tipo técnico:

```
src/
├── core/         # NÚCLEO HORIZONTAL: Infraestructura transversal, cliente HTTP,
│                 # contratos de roles, layout maestro y enrutador global.
│
├── features/     # MÓDULOS VERTICALES POR DOMINIO (Features de Negocio):
│                 # Cada carpeta encapsula sus vistas, componentes específicos,
│                 # servicios de consumo REST, stores locales y definiciones de ruta.
│
└── shared/       # BIBLIOTECA REUTILIZABLE: Componentes universales (Modales,
                  # Paginadores, Badges, Hero Cards), composables reactivos y utilidades.
```

#### Ventajas de este enfoque:

- **Aislamiento de Cambios:** Las modificaciones en el módulo de _Finanzas_ o _Gestión Aduanera_ no alteran el código de _Recepción Comercial_ o _Administración TI_.
- **Escalabilidad del Equipo:** Múltiples desarrolladores pueden trabajar simultáneamente sobre distintas características sin generar conflictos en archivos monolíticos.
- **Carga Bajo Demanda:** Cada ruta carga exclusivamente el JavaScript y estilos requeridos para su módulo específico mediante importaciones dinámicas.

---

### 2.2 Diagrama de Flujo de Datos y Componentes

```
[ Interacción del Usuario en la SPA ]
                 │
                 ▼
     ┌────────────────────────┐
     │  Vue Router Guard      │  (router.beforeEach)
     │  - requiresAuth        │  Valida presencia de token y permisos por rol.
     │  - to.meta.roles       │  Redirige a /login o al homePath asignado.
     └───────────┬────────────┘
                 │
                 ▼
┌────────────────────────────────────────────────────────┐
│                   AppLayout.vue                        │
│  ┌──────────────────────┐  ┌─────────────────────────┐ │
│  │    AppSidebar.vue    │  │       App Topbar        │ │
│  │ (Menú adaptativo por │  │ (Breadcrumbs, Sede,     │ │
│  │  rol y conmutador)   │  │  Perfil, Toggle Tema)   │ │
│  └──────────────────────┘  └─────────────────────────┘ │
│  ───────────────────────────────────────────────────── │
│             <router-view /> (Página Activa)            │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│             Vistas de Feature (views/*.vue)            │
│  - RecepcionComercialView    - GestionAduaneraView     │
│  - FinanzasView              - TransporteFlotaView     │
│  - UserManagementView        - AgenteAduaneroView      │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
               ▼                          ▼
┌────────────────────────┐      ┌────────────────────────┐
│   Componentes y Modales│      │   Pinia Global Stores  │
│ - CotizacionRegistration│     │ - authStore            │
│ - AperturaExpediente   │      │ - navigationStore      │
│ - DetalleExpediente    │      └────────────────────────┘
└──────────────┬─────────┘
               │
               ▼
┌────────────────────────────────────────────────────────┐
│          Servicios de Dominio (services/*.js)          │
│  - comercialService          - gestionAduaneraService  │
│  - finanzasService           - transporteFlotaService  │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│           Cliente HTTP Base (apiClient.js)             │
│  - Request Interceptor: Inyecta Bearer <sga_token>     │
│  - Response Interceptor: 401 Logout / 403 Bypass       │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
         [ Backend REST API: /api/v1/... ]
```

---

### 2.3 Integración con el Backend mediante Axios

La comunicación HTTP con la API REST de Spring Boot está encapsulada en `src/core/api/apiClient.js`:

1. **Configuración Base:**  
   Define la URL base mediante variables de entorno (`import.meta.env.VITE_API_URL` o valor por defecto `http://localhost:8085/api/v1`), cabeceras estándar JSON y un tiempo de espera (_timeout_) de 15 segundos.
2. **Interceptor de Solicitud (Request Interceptor):**  
   Extrae el token persistido en `localStorage.getItem('sga_token')`. Si está presente, inyecta automáticamente la cabecera:
   ```http
   Authorization: Bearer <token_jwt>
   ```
3. **Interceptor de Respuesta (Response Interceptor):**
   - **Manejo de HTTP 401 (No Autenticado):** Si una petición protegida retorna 401 (token expirado, revocado o inválido) y la solicitud no proviene del formulario de login, se limpian las claves de sesión (`sga_token` y `sga_user`) y se redirige al usuario a `/login` mediante `window.location.replace('/login')`.
   - **Manejo de HTTP 403 (Acceso Denegado):** Los errores de permisos insuficientes no destruyen la sesión del usuario; el error es propagado como una promesa rechazada para que la vista o el composable `useNotification` presente un Toast informativo sin expulsar al operador.

---

## 3. Patrones de Diseño Implementados

| Patrón                                     | Ubicación en el Frontend                                                                       | Propósito y Beneficio                                                                                                                                                                                            |
| :----------------------------------------- | :--------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Component-Driven Development (CDD)**     | `src/shared/components/*` y `src/features/**/components/*`                                     | Construcción de interfaces a partir de componentes visuales aislados y reutilizables con contratos declarativos (`defineProps`, `defineEmits`).                                                                  |
| **Composable Pattern (Functions)**         | `src/shared/composables/*` (`useTheme`, `useNotification`, `useConfirmation`, `usePagination`) | Encapsula y comparte lógica reactiva con estado entre múltiples componentes, sustituyendo mixins obsoletos por funciones puras de Composition API.                                                               |
| **Store Pattern (Single Source of Truth)** | `src/features/auth/stores/authStore.js` y `src/core/stores/navigationStore.js`                 | Almacenes centralizados reactivos con **Pinia** para gobernar el estado de autenticación, usuario en sesión y estado de navegación.                                                                              |
| **Master Layout / Shell Pattern**          | `src/core/layouts/AppLayout.vue`                                                               | Define el armazón estructural de la aplicación (Sidebar vertical colapsable, Topbar dinámico con breadcrumbs y selector de temas) alrededor de `<router-view>`.                                                  |
| **Interceptor / Pipeline Pattern**         | `src/core/api/apiClient.js`                                                                    | Canaliza todas las peticiones y respuestas HTTP a través de interceptores para inyección de tokens y gestión uniforme de errores.                                                                                |
| **Adapter / Mapper Pattern**               | `src/core/auth/roles.js` (`ROLE_LABELS`, `ROLE_MODULE_MAP`, `ROLE_HOME_PATH`)                  | Adapta las constantes técnicas de roles provenientes del backend a etiquetas amigables, nombres de módulos y rutas de destino en el cliente.                                                                     |
| **Wizard / Stepper Pattern**               | `CotizacionRegistrationForm.vue` + `CotizacionStepperNav.vue`                                  | Guía la creación de cotizaciones aduaneras en un flujo de 4 pasos secuenciales validados: Datos de Operación $\rightarrow$ Valores FOB y Flete $\rightarrow$ Tributos/Ad-Valórem $\rightarrow$ Resumen Proforma. |
| **Navigation Guard / Gatekeeper**          | `src/core/router/index.js` (`router.beforeEach`)                                               | Filtra la navegación de rutas verificando credenciales activas y pertenencia a la lista de roles autorizados (`meta.roles`).                                                                                     |

---

## 4. Tecnologías y Stack de Dependencias

### 4.1 Matriz de Tecnologías Centrales

- **Framework Progresivo:** Vue 3 (versión 3.5.42) con Composition API y `<script setup>`.
- **Entorno de Construcción y Servidor Dev:** Vite 8 (versión 8.3.0) con soporte HMR (_Hot Module Replacement_).
- **Enrutador Oficial SPA:** Vue Router 4 (versión 4.6.4) con historial web HTML5.
- **Gestión de Estado Reactivo:** Pinia 4 (versión 4.0.3).
- **Biblioteca de Componentes UI:** PrimeVue 4 (versión 4.5.5) con paquete de temas `@primevue/themes` 4.5.4 (Aura adaptado).
- **Iconografía Vectorial:** PrimeIcons 8 (versión 8.0.2).
- **Cliente HTTP:** Axios (versión 1.20.0).

---

### 4.2 Inventario de Dependencias (`package.json`)

```json
{
  "name": "frontend",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@primevue/themes": "^4.5.4",
    "axios": "^1.20.0",
    "pinia": "^4.0.3",
    "primeicons": "^8.0.2",
    "primevue": "^4.5.5",
    "vue": "^3.5.42",
    "vue-router": "^4.6.4"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.8",
    "vite": "^8.3.0"
  }
}
```

| Paquete              |  Versión  |  Tipo   | Justificación Técnica                                                                                                                     |
| :------------------- | :-------: | :-----: | :---------------------------------------------------------------------------------------------------------------------------------------- |
| `vue`                | `^3.5.42` | Runtime | Núcleo reactivo del sistema con soporte completo de TypeScript/JavaScript moderno, optimización de renderizado y Composition API.         |
| `vite`               | `^8.3.0`  |   Dev   | Servidor de desarrollo de arranque instantáneo y empaquetador para producción basado en Rollup con minificación y optimización de assets. |
| `@vitejs/plugin-vue` | `^6.0.8`  |   Dev   | Compilador oficial de archivos `.vue` (Single File Components - SFC) para Vite.                                                           |
| `vue-router`         | `^4.6.4`  | Runtime | Enrutador client-side para navegación sin recarga de página, soporte de sub-rutas anidadas y guardias globales de seguridad.              |
| `pinia`              | `^4.0.3`  | Runtime | Almacén de estado oficial de Vue con soporte modular, tipado automático, reactividad fina y herramientas de depuración (_Devtools_).      |
| `primevue`           | `^4.5.5`  | Runtime | Suite de componentes de alta fidelidad (Tablas interactivas, Diálogos modales, Toast Notifications, ConfirmDialogs y selectores).         |
| `@primevue/themes`   | `^4.5.4`  | Runtime | Motor de estilizado y tokens de PrimeVue 4 para configurar temas semánticos y modo oscuro.                                                |
| `primeicons`         | `^8.0.2`  | Runtime | Colección completa de glifos vectoriales para señalética de aduanas, transporte, estados, descargas y navegación.                         |
| `axios`              | `^1.20.0` | Runtime | Cliente HTTP basado en promesas con soporte de interceptores globales para inyección de credenciales y cancelación de peticiones.         |

---

## 5. Estructura del Proyecto Frontend

```
frontend/
├── index.html                           # Plantilla HTML base con fuentes Inter y JetBrains Mono
├── package.json                         # Manifiesto de dependencias y scripts de ejecución
├── vite.config.js                       # Configuración Vite (alias @ y proxy inverso al puerto 8085)
└── src/
    ├── main.js                          # Punto de entrada: Registro de Pinia, Router, PrimeVue y Tema
    ├── App.vue                          # Componente raíz con <router-view /> y proveedores de Toast
    ├── style.css                        # Tokens de diseño Warm Editorial, reset y clases globales
    │
    ├── core/                            # Núcleo y Arquitectura Transversal
    │   ├── api/
    │   │   └── apiClient.js             # Instancia Axios configurada con interceptores de Token y 401
    │   ├── auth/
    │   │   └── roles.js                 # Constantes de roles, etiquetas, mapas de módulos y homePaths
    │   ├── layouts/
    │   │   └── AppLayout.vue            # Shell maestro: Topbar dinámico, Sidebar y contenedor de vistas
    │   ├── router/
    │   │   └── index.js                 # Definición del router con Navigation Guard global por roles
    │   └── stores/
    │       └── navigationStore.js       # Store Pinia para conmutación de módulos y sub-pestañas
    │
    ├── features/                        # Módulos Funcionales por Dominio de Negocio
    │   ├── admin/                       # Módulo de Administración TI
    │   │   ├── about/views/             # Vista informativa del sistema (AboutSystemView.vue)
    │   │   ├── catalogos/               # Puertos, Tipos de Carga y Mercancías (vistas, modales, servicios)
    │   │   ├── dashboard/views/         # Panel ejecutivo y métricas generales (AdminDashboardView.vue)
    │   │   ├── portal-users/            # Gestión de cuentas B2B (PortalUserManagementView, modales, service)
    │   │   ├── sedes/                   # Gestión territorial de agencias (SedesManagementView, modal, service)
    │   │   ├── services/logsService.js  # Servicio de consulta en vivo y descarga de logs
    │   │   ├── users/                   # Directorio de operadores y roles (UserManagementView, modales, service)
    │   │   └── views/SystemLogsView.vue # Visor en tiempo real de logs del backend
    │   │
    │   ├── auth/                        # Módulo de Autenticación
    │   │   ├── routes/auth.routes.js    # Ruta /login
    │   │   ├── services/authService.js  # Consumo de /api/v1/auth/login y /me
    │   │   ├── stores/authStore.js      # Estado de sesión, usuario conectado y permisos
    │   │   └── views/LoginView.vue      # Vista de inicio de sesión con estilo editorial
    │   │
    │   ├── gerencia/                    # Módulo de Gerencia General
    │   │   ├── routes/gerencia.routes.js# Rutas protegidas para GERENTE_GENERAL
    │   │   └── views/                   # Dashboard ejecutivo (GerenciaDashboardView.vue), flota y choferes
    │   │
    │   ├── comercial/                   # Módulo de Recepción Comercial
    │   │   ├── components/              # Calculadora 4 pasos, stepper, proforma preview y cliente modal
    │   │   ├── composables/             # useCotizacionCalculator, useModalidadPuertos, useCotizacionCorrection
    │   │   ├── data/peruUbigeo.js       # Catálogo maestro de departamentos, provincias y distritos
    │   │   ├── services/                # comercialService.js, expedienteService.js
    │   │   └── views/                   # RecepcionComercialView.vue
    │   │
    │   ├── aduanas/                     # Módulo de Gestión Aduanera
    │   │   ├── components/              # Apertura DAM, Canal/Levante, Auditoría, Tránsito y Modales
    │   │   ├── services/                # gestionAduaneraService.js, expedienteService.js
    │   │   ├── utils/                   # aprobacionAduaneraUtils.js
    │   │   └── views/                   # GestionAduaneraView.vue
    │   │
    │   ├── almacen/                     # Módulo de Depósito Temporal y Custodia
    │   │   ├── components/              # IngresoAlmacenModal.vue (pesaje balanza VGM)
    │   │   ├── services/                # almacenService.js, expedienteService.js
    │   │   └── views/                   # AgenteAduaneroView.vue
    │   │
    │   ├── transporte/                  # Módulo de Transporte y Flota
    │   │   ├── components/              # AsignarRecursosModal, CrearDespachoModal, ConfirmarEntregaModal
    │   │   ├── services/                # transporteFlotaService.js, expedienteService.js
    │   │   └── views/                   # TransporteFlotaView.vue
    │   │
    │   ├── finanzas/                    # Módulo de Finanzas y Tesorería
    │   │   ├── components/              # RegistroPagoFullView.vue (validación anticipos y comprobantes)
    │   │   ├── services/                # finanzasService.js, expedienteService.js
    │   │   └── views/                   # FinanzasView.vue
    │   │
    │   ├── liquidacion/                 # Módulo de Liquidación Aduanera y Tributaria
    │   │   └── views/                   # LiquidacionAduaneraView.vue
    │   │
    │   ├── legal/                       # Módulo de Asesoría Legal y Normativa
    │   │   ├── components/              # AuditoriaExpedienteModal.vue
    │   │   └── views/                   # LegalAuditoriaView.vue
    │   │
    │   └── documentos/                  # Módulo de Archivo Digital y Gestión Documental
    │       └── services/documentoService.js # Carga multipart, descarga y preview de archivos
    │
    └── shared/                          # Componentes y Utilidades Reutilizables
        ├── components/
        │   ├── AppSidebar.vue           # Barra de navegación lateral colapsable y adaptativa
        │   ├── BaseModal.vue            # Diálogo modal base con animaciones y slots accesibles
        │   ├── BasePagination.vue       # Control de paginación uniforme para tablas
        │   ├── DocumentosManagerModal.vue # Gestor modal para auditar y descargar expedientes digitales
        │   ├── DocumentosUploadPanel.vue  # Zona drag-and-drop para carga de documentos sustentatorios
        │   ├── HeroBannerCard.vue       # Banner editorial con kicker, título descriptivo y badges
        │   └── MetricCard.vue           # Tarjeta de métricas KPI numerada con estilo Warm Editorial
        ├── composables/
        │   ├── useConfirmation.js       # Abstracción para diálogos modales de confirmación
        │   ├── useNotification.js       # Notificaciones Toast enriquecidas (success, warn, error)
        │   ├── usePagination.js         # Composable para lógica de paginación en cliente
        │   └── useTheme.js              # Alternancia y persistencia de Modo Claro / Modo Oscuro
        └── utils/
            └── download.js              # Descarga de blobs y archivos binarios (PDFs, Excel)
```

---

## 6. Sistema de Diseño "Warm Editorial" y Modos Visuales

### 6.1 Tokens de Diseño y Variables CSS

El diseño del frontend prescinde de plantillas genéricas y adopta un **Sistema de Diseño Editorial Cálido (Warm Editorial)** con variables CSS personalizadas definidas en `src/style.css`:

```css
:root {
  /* Paleta Base Cálida */
  --color-bg-base: #f6f3ee; /* Arena muy clara de fondo */
  --color-bg-sidebar: #ede6db; /* Tono latte suave para barra lateral */
  --color-bg-surface: #ffffff; /* Blanco puro para tarjetas y modales */
  --color-bg-hero: #ebdcc9; /* Crema dorado para banners principales */
  --color-bg-subtle: #f2ebe1; /* Fondo sutil para botones y píldoras */

  /* Colores Principales */
  --color-primary: #352f2a; /* Espresso profundo para acciones primarias */
  --color-primary-hover: #484039;
  --color-accent-kicker: #9a7f60; /* Caramelo/bronce para subtítulos en mayúsculas */

  /* Tipografía */
  --color-text-main: #2b2520; /* Carbón tostado profundo para lectura */
  --color-text-muted: #6f6356; /* Gris cálido para textos explicativos */
  --color-text-subtle: #998c7e; /* Avena apagado para etiquetas y fechas */

  /* Estados Funcionales Semánticos */
  --color-success-bg: #eaf6ed;
  --color-success-text: #1e6638;
  --color-warning-bg: #fdf5e6;
  --color-warning-text: #8c5b16;
  --color-danger-bg: #fdf0f0;
  --color-danger-text: #9e2a2b;
  --color-info-bg: #eef4fb;
  --color-info-text: #21598f;
}
```

---

### 6.2 Tipografía Dual (Inter & JetBrains Mono)

Se adopta una combinación tipográfica balanceada:

- **`Inter` (`--font-sans`):** Empleada para todo el cuerpo de texto, encabezados, formularios y modales, garantizando legibilidad en densidades de datos elevadas.
- **`JetBrains Mono` (`--font-mono`):** Utilizada estrictamente para identificadores de negocio, códigos correlativos (`COT-...`, `EXP-...`, `DSP-...`), subpartidas arancelarias NANDINA, números de contenedor, placas vehiculares y montos monetarios en USD y PEN.

---

### 6.3 Rejilla de Espaciado Armónico de 4px

El layout y los componentes están sincronizados mediante una escala modular basada en múltiplos de 4 píxeles:

| Token CSS   | Equivalente en Rem | Píxeles | Uso Principal                                   |
| :---------- | :----------------: | :-----: | :---------------------------------------------- |
| `--space-1` |     `0.25rem`      |   4px   | Micro-separaciones e iconos inline.             |
| `--space-2` |      `0.5rem`      |   8px   | Padding interno de badges y botones compactos.  |
| `--space-3` |     `0.75rem`      |  12px   | Brecha entre controles y filtros de formulario. |
| `--space-4` |      `1.0rem`      |  16px   | Padding interno de tarjetas y celdas de tabla.  |
| `--space-5` |     `1.25rem`      |  20px   | Separación entre secciones secundarias.         |
| `--space-6` |      `1.5rem`      |  24px   | Espaciado principal entre bloques de página.    |
| `--space-8` |      `2.0rem`      |  32px   | Padding general del contenedor de página.       |

---

### 6.4 Preset Personalizado PrimeVue (`SgaPreset`)

En `src/main.js`, PrimeVue se configura mediante `definePreset(Aura, ...)` personalizando la paleta semántica primaria y de superficies con los matices del sistema, asegurando que componentes como diálogos, menús desplegables y calendarios armonicen visualmente:

```javascript
const SgaPreset = definePreset(Aura, {
  semantic: {
    primary: {
      500: "#352f2a", // Espresso característico
      /* Escala completa desde 50 hasta 950 */
    },
    colorScheme: {
      light: { surface: { 0: "#ffffff", 50: "#fbf9f6" /* ... */ } },
      dark: { surface: { 0: "#191815", 50: "#1e1e1b" /* ... */ } },
    },
  },
});
```

---

### 6.5 Alternancia Nativa Claro / Oscuro (`useTheme`)

El composable `useTheme.js` gestiona la alternancia de temas visuales:

- **Detección Automática:** Lee la clave `sga-theme` en `localStorage`; si no existe, consulta la preferencia del sistema operativo mediante `window.matchMedia('(prefers-color-scheme: dark)')`.
- **Clase Raíz:** Aplica o retira la clase `.dark-theme` en el elemento `<html>`.
- **Control Integrado:** El menú desplegable del perfil en el Topbar incorpora un selector visual interactivo para alternar entre el modo cálido claro y el modo carbón-arena oscuro sin recargar la aplicación.

---

## 7. Gestión de Estado Global (Pinia Stores)

### 7.1 `authStore`: Control de Sesión, Rol y Permisos

Ubicado en `src/features/auth/stores/authStore.js`, gobierna la sesión activa:

#### Estado (State):

- `token`: Cadena JWT Bearer recibida al autenticarse.
- `user`: Objeto con los datos del usuario conectado (`idUsuario`, `username`, `nombres`, `apellidos`, `correoInstitucional`, `rol`, `codigoSede`, `nombreSede`).
- `loading`: Indicador booleano de operaciones asíncronas en curso.
- `error`: Mensaje descriptivo de fallos de autenticación.

#### Getters Clave:

- `isAuthenticated`: `true` si existe un token y un usuario con rol normativo válido.
- `userRole`: Retorna el rol actual (e.g. `SUPERVISOR_ADUANERO`).
- `canSwitchModules`: `true` únicamente para `ADMINISTRADOR_TI` y `GERENTE_GENERAL`, permitiéndoles conmutar entre los ocho módulos del sistema.
- `homePath`: Ruta por defecto a la que debe ser dirigido el usuario al autenticarse según la matriz institucional de roles.

#### Acciones (Actions):

- `login(credentials)`: Envía credenciales a `/api/v1/auth/login`, persiste el token y el perfil en `localStorage` y devuelve los datos del usuario.
- `syncCurrentUser()`: Consulta `/api/v1/auth/me` para actualizar la información del perfil en memoria y almacenamiento local.
- `logout()`: Invalida el estado en memoria y elimina `sga_token` y `sga_user` de `localStorage`.

---

### 7.2 `navigationStore`: Sincronización Modular y Sub-Opciones

Ubicado en `src/core/stores/navigationStore.js`, gestiona la navegación interna:

- **`activeModule`:** Módulo seleccionado actualmente (e.g. `comercial`, `gestion-aduanera`, `finanzas`).
- **Sub-opciones por módulo:**
  - `administracionTiSubOption`: `'users' | 'about' | 'logs'`
  - `gerenciaGeneralSubOption`: `'overview' | 'sedes' | 'catalogos' | 'flota' | 'conductores'`
  - `comercialSubOption`: `'dashboard' | 'cotizaciones' | 'clientes' | 'ordenes'`
  - `gestionAduaneraSubOption`: `'cotizaciones' | 'expedientes' | 'declaraciones' | 'clasificacion' | 'tributos'`
  - `agenteAduaneroSubOption`: `'almacen' | 'expedientes' | 'despacho'`
  - `transporteFlotaSubOption`: `'despachos' | 'unidades' | 'conductores'`
  - `finanzasSubOption`: `'liquidaciones' | 'por-liquidar' | 'conciliacion' | 'anticipos'`
  - `legalAuditoriaSubOption`: `'expedientes' | 'auditorias'`
- **Disparadores Modales Directos (`triggers`):** Permiten que botones del Sidebar abran inmediatamente modales específicos (e.g. "Nueva Cotización", "Nuevo Usuario") mediante un contador reactivo `navTick`.

---

## 8. Enrutamiento y Seguridad de Navegación (Vue Router)

### 8.1 Guardias de Navegación Globales (Navigation Guards)

En `src/core/router/index.js`, la guardia `router.beforeEach` ejecuta las siguientes comprobaciones en cada cambio de ruta:

```javascript
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();
  const isAuth = authStore.isAuthenticated;

  // 1. Si accede a la raíz '/', redirigir a su homePath o al login
  if (to.path === "/") {
    return isAuth ? next(authStore.homePath) : next("/login");
  }

  // 2. Si ya está autenticado e intenta ir a /login, redirigir a su homePath
  if (to.path === "/login" && isAuth) {
    return next(authStore.homePath);
  }

  // 3. Si la ruta requiere autenticación y no hay sesión activa
  const requiresAuth = to.matched.some((record) => record.meta?.requiresAuth);
  if (requiresAuth && !isAuth) {
    return next({
      path: "/login",
      query: to.fullPath !== "/" ? { redirect: to.fullPath } : {},
    });
  }

  // 4. Validación de roles autorizados
  if (to.meta?.roles && to.meta.roles.length > 0) {
    if (!to.meta.roles.includes(authStore.userRole)) {
      return next(authStore.homePath);
    }
  }

  // 5. Sincronizar automáticamente el navigationStore con la URL actual
  // ...
  next();
});
```

---

### 8.2 Matriz de Enrutamiento por Roles Institucionales

| Rol Institucional           | Ruta de Inicio (`homePath`)    | Módulos y Rutas Autorizadas                                                                                                                    |
| :-------------------------- | :----------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------- |
| **`ADMINISTRADOR_TI`**      | `/administracion-ti/dashboard` | Acceso global irrestricto a todos los módulos y consolas TI (`/administracion-ti/*`, `/gerencia-general/*`, módulos operativos).               |
| **`GERENTE_GENERAL`**       | `/gerencia-general/dashboard`  | Cuadros de mando, dashboard ejecutivo, sedes, catálogos, flota, choferes y conmutación de supervisión operativa.                               |
| **`AGENTE_COMERCIAL`**      | `/recepcion-comercial`        | Bandeja de cotizaciones, calculadora aduanera y directorio de clientes/prospectos (`/recepcion-comercial`).                                    |
| **`SUPERVISOR_ADUANERO`**   | `/gestion-aduanera`           | Revisión técnica de propuestas, apertura de expedientes DAM, asignación de canal SUNAT (`/gestion-aduanera`, `/gestion-aduanera/liquidacion`). |
| **`AGENTE_ADUANERO`**       | `/agente-aduanero`            | Depósito temporal, control de pesaje balanza VGM y verificación de levante aduanero (`/agente-aduanero`).                                      |
| **`SUPERVISOR_TRANSPORTE`** | `/transporte-flota`           | Programación de órdenes de despacho, asignación de tractos/choferes MTC y tracking en ruta (`/transporte-flota`).                              |
| **`CONTADOR`**              | `/finanzas`                   | Validación de anticipos pre-expediente, balances de liquidación final y cálculo de detracciones SPOT (`/finanzas`).                            |
| **`ASESOR_LEGAL`**          | `/legal-auditoria`            | Fiscalización normativa de expedientes aduaneros, permisos sectoriales (DIGEMID, SENASA) y archivo digital (`/legal-auditoria`).               |

---

## 9. Catálogo de Módulos Funcionales (Features)

### 9.1 Autenticación (`LoginView.vue`)

- Formulario minimalista con inputs reactivos y botón con estado de carga animado (_spinner_).
- Panel desplegable de **credenciales de prueba rápida** para alternar con un clic entre los 8 perfiles del sistema durante auditorías o presentaciones.
- Validación de contraseña, mensajes de error semánticos y redirección inteligente a la ruta solicitada previamente (`?redirect=...`).

---

### 9.2 Shell Maestro y Navegación (`AppLayout.vue` & `AppSidebar.vue`)

- **Barra Lateral Adaptativa (`AppSidebar`):**
  - Identificador visual institucional del SGA.
  - Insignia (_badge_) con el rol y sede asignada del usuario conectado.
  - Para roles de supervisión (`ADMINISTRADOR_TI` y `GERENTE_GENERAL`), despliega un selector modular interactivo para alternar entre los 8 dominios del sistema.
  - Botón de colapso rápido para optimizar el ancho de trabajo en monitores estándar.
- **Barra Superior Dinámica (`AppTopBar`):**
  - Miga de pan (_Breadcrumbs_) y kicker en mayúsculas sincronizados con la sub-opción activa.
  - Título y descripción explicativa del módulo en vista.
  - Píldora informativa de Sede Asignada.
  - Avatar con iniciales del usuario y menú desplegable flotante con botón para alternar Modo Claro / Modo Oscuro y cierre de sesión.

---

### 9.3 Administración TI y Consola del Sistema (`features/admin`)

- **Dashboard Ejecutivo TI (`AdminDashboardView.vue`):**  
  Panel de control de infraestructura, métricas de actividad del sistema, estado de conexión de servicios y atajos de gobernanza.
- **Gestión de Usuarios Operadores (`UserManagementView`):**
  - Resumen KPI en tiempo real: Usuarios Totales, Cuentas Activas, Cuentas Desactivadas y Roles del Sistema.
  - Búsqueda interactiva multitexto (usuario, nombres, correo, cargo, sede).
  - Modal para registro de nuevo operador (`UserRegistrationForm.vue`).
  - Modal para edición de datos y reasignación de rol o sede en caliente.
  - Acciones de baja lógica (`DELETE /api/v1/usuarios/{id}`) y reactivación (`PATCH /api/v1/usuarios/{id}/reactivar`).
  - Botón directo para descargar el padrón oficial generado en PDF por JasperReports en el backend.
- **Gestión de Cuentas B2B del Portal (`PortalUserManagementView.vue`):**
  - Directorio exclusivo para habilitar o suspender cuentas de acceso al Portal del Cliente B2B para importadores/exportadores.
  - Modal de alta y vinculación a cliente (`PortalUserForm.vue`).
  - Reseteo seguro de contraseña temporal (`PortalUserResetPasswordModal.vue`).
  - Modal de auditoría y trazabilidad de accesos (`PortalUserAuditModal.vue`).
- **Bitácora y Consola de Logs TI (`SystemLogsView`):**
  - Visor interactivo en vivo del archivo `sga-backend.log`.
  - Selector de profundidad de líneas (50, 100, 200, 500 líneas).
  - Descarga directa del archivo de log para soporte forense y botón para vaciar la bitácora.
- **Sedes Territoriales (`SedesManagementView`):**
  - Directorio de agencias (Callao, Paita, Matarani, Aeropuerto Jorge Chávez).
  - Modal para alta de nueva sede (`SedeRegistrationForm.vue`) y exportación oficial en PDF.
- **Catálogos Auxiliares (`PuertosManagementView`, `TiposCargaManagementView`, `MercanciasManagementView`):**
  - Mantenimiento unificado de puertos/terminales marítimos y aéreos, tipos de carga aduanera y catálogo de mercancías con partidas NANDINA.

---

### 9.4 Gerencia General (`features/gerencia`)

- **Dashboard Ejecutivo Gerencial (`GerenciaDashboardView.vue`):**  
  Cuadros de mando consolidados con KPIs clave de agenciamiento aduanero, volumen financiero facturado, distribución de canales SUNAT y estado de flota.
- **Padrón de Conductores Acreditados MTC:**
  - Directorio de choferes profesionales con tipo y vigencia de licencia de conducir MTC.
  - Monitoreo de estado operativo (`DISPONIBLE`, `EN_RUTA`, `DESCANSO`).
- **Parque Automotor y Flota Terrestre:**
  - Listado de tractocamiones, semirremolques y plataformas aduaneras.
  - Control de placas, número de ejes, capacidad de carga útil en toneladas y vigencia de pólizas de seguro SOAT.

---

### 9.5 Recepción Comercial (`features/comercial`)

- **Dashboard Comercial & Ventas (`RecepcionComercialView.vue`):**  
  Tarjetas de métricas consolidadas (Volumen cotizado en USD, Tasa de conversión, Propuestas en evaluación).
- **Calculadora y Asistente de Cotización en 4 Pasos (`CotizacionRegistrationForm.vue`):**
  1. _Paso 1 - Datos de la Operación:_ Selección de cliente (o creación rápida en modal), régimen (Importación para el Consumo / Exportación Definitiva), puerto de origen/destino y tipo de carga.
  2. _Paso 2 - Logística y Flete:_ Registro de valor FOB en USD, divisa, peso bruto en kilogramos, volumen en CBM y estimación de flete internacional.
  3. _Paso 3 - Tributos y Aranceles:_ Subpartida arancelaria NANDINA de 10 dígitos, porcentaje de Ad-Valórem (0%, 6%, 11%), cálculo automático de base imponible, IGV SUNAT, seguro y honorarios de agenciamiento.
  4. _Paso 4 - Proforma y Emisión:_ Vista previa formal de la cotización comercial con código correlativo generado (`COT-YYYY-XXXX`) y desglose contable antes del guardado definitivo.
- **Directorio de Clientes & Prospectos (`ClienteRegistrationForm.vue`):**  
  Cartera comercial con consulta y validación de RUC, Razón Social, dirección fiscal y contacto operativo.

---

### 9.6 Gestión Aduanera (`features/aduanas`)

- **Bandeja de Revisión Técnica de Cotizaciones (`GestionAduaneraView.vue`):**  
  Auditoría exhaustiva de propuestas formuladas por el área comercial. Permite aprobar, observar o rechazar técnicamente la operación.
- **Apertura de Expediente Aduanero DAM (`AperturaExpedienteModal.vue` / `AperturaExpedienteView.vue`):**  
  Aplica en interfaz la **Compuerta Financiera**: solo permite aperturar el expediente aduanero si la cotización cuenta con un pago o anticipo aprobado por Finanzas. Genera código `EXP-YYYY-XXXX`.
- **Control de Canal SUNAT y Levante (`CanalLevanteModal.vue`):**  
  Asignación del número de DAM oficial (118-YYYY-10-XXXXXX) y semáforo de riesgo SUNAT:
  - `VERDE`: Levante automático sin revisión documental ni física.
  - `NARANJA`: Revisión documental previa por vista de aduana.
  - `ROJO`: Reconocimiento físico obligatorio y verificación de precintos en depósito temporal.
- **Operaciones de Cierre y Diligencias:**  
  Modales para tránsito, diligencia de campo, solicitud de transporte y cierre formal del expediente aduanero.

---

### 9.7 Depósito Temporal y Almacén (`features/almacen`)

- **Control de Depósito Temporal y Almacén (`AgenteAduaneroView.vue`):**
  - Registro de ingreso de carga al almacén aduanero (`IngresoAlmacenModal.vue`).
  - Registro obligatorio de pesaje en báscula certificada VGM (_Verified Gross Mass_) según enmienda SOLAS.
  - Autorización formal de salida de carga vinculando la validación del levante aduanero.

---

### 9.8 Transporte y Flota (`features/transporte`)

- **Programación de Despachos Terrestres (`CrearDespachoModal.vue`):**  
  Generación de orden de transporte con código correlativo de tracking `TRK-YYYY-XXXX`.
- **Asignación de Recursos (`AsignarRecursosModal.vue`):**  
  Asigna chofer con licencia MTC vigente y tracto disponible.
- **Control de Ruta y Tracking en Vivo (`TransporteFlotaView.vue`):**
  - Marcado de salida del depósito temporal e inicio de tránsito a planta del cliente.
  - Registro de incidencias en ruta con notificación instantánea.
  - Confirmación de entrega conforme (`ConfirmarEntregaModal.vue`), liberando automáticamente el vehículo y al chofer para nuevos servicios.

---

### 9.9 Finanzas y Tesorería (`features/finanzas`)

- **Bandeja de Anticipos Pre-Expediente (`FinanzasView.vue` & `RegistroPagoFullView.vue`):**  
  Inspección y validación de comprobantes de pago bancarios subidos por el cliente o área comercial. Su aprobación formal desbloquea la compuerta financiera para apertura del expediente aduanero.
- **Conciliación Bancaria y Métricas:**  
  Indicadores de volumen facturado, saldo por cobrar y cobranzas recaudadas en USD y PEN.

---

### 9.10 Liquidación Aduanera y Tributaria (`features/liquidacion`)

- **Liquidación Final y Cierre Contable (`LiquidacionAduaneraView.vue`):**  
  - Consolidación de tributos SUNAT, gastos de almacenaje, fletes locales y honorarios profesionales con desglose de IGV (18%) y retención/detracción SPOT (12% cuando `totalPen >= 700.00`).
  - Emisión de código correlativo `LIQ-YYYY-XXXX`.
  - Generación de actas y constancias de pago.

---

### 9.11 Asesoría Legal y Auditoría Normativa (`features/legal`)

- **Auditoría Regulatoria (`LegalAuditoriaView.vue` & `AuditoriaExpedienteModal.vue`):**  
  - Fiscalización jurídica de expedientes aduaneros cerrados o en trámite.
  - Inspección de permisos sectoriales obligatorios (VUCE, DIGEMID para fármacos, SENASA para alimentos/agro, SUCAMEC para materiales restringidos).
  - Verificación de plazos máximos para evitar abandono legal de mercancías ante SUNAT.

---

### 9.12 Archivo Documental Digital (`features/documentos`)

- Carga de archivos sustentatorios mediante zona interactiva de arrastre (_Drag & Drop_) o explorador del sistema (`DocumentosUploadPanel.vue`).
- Clasificación por categorías normativas: Factura Comercial, Packing List, Conocimiento de Embarque (BL), DAM, Comprobante de Pago, Ticket de Balanza VGM.
- Modal de gestión documental (`DocumentosManagerModal.vue`) con previsualización embebida (`/preview`) en el navegador y descarga directa (`/descargar`).

---

## 10. Biblioteca de Componentes Compartidos y Composables

### 10.1 Componentes Compartidos (`src/shared/components`)

| Componente               | Archivo                      | Descripción Funcional                                                                                                      |
| :----------------------- | :--------------------------- | :------------------------------------------------------------------------------------------------------------------------- |
| `AppSidebar`             | `AppSidebar.vue`             | Barra lateral adaptativa con acordeón modular, insignias de rol, sede y colapso responsive.                                |
| `BaseModal`              | `BaseModal.vue`              | Diálogo modal estándar con backdrop desenfocado (_backdrop-blur_), header con slots de título y cierre por tecla `Escape`. |
| `BasePagination`         | `BasePagination.vue`         | Barra de navegación por páginas numéricas con selector de elementos por página (10, 25, 50, 100).                          |
| `HeroBannerCard`         | `HeroBannerCard.vue`         | Cabecera destacada estilo Warm Editorial con kicker bronce, título principal, descripción y slots de métricas.             |
| `MetricCard`             | `MetricCard.vue`             | Tarjeta KPI numerada (`01`, `02`, `03`) con valor numérico, etiqueta descriptiva y badge contextual.                       |
| `DocumentosUploadPanel`  | `DocumentosUploadPanel.vue`  | Zona de arrastre y soltado de archivos múltiples (Multipart) con barra de progreso y selección de categorías.              |
| `DocumentosManagerModal` | `DocumentosManagerModal.vue` | Visor modal de archivo digital con tabla de documentos adjuntos, tamaño, usuario de subida y botones de preview/descarga.  |

---

### 10.2 Composables Reactivos (`src/shared/composables`)

1. **`useTheme()`:** Alterna entre el tema claro editorial y el tema oscuro carbón/arena, sincronizando la clase `.dark-theme` en `<html>` y persistiendo la selección en `localStorage`.
2. **`useNotification()`:** Abstracción reactiva sobre el servicio `ToastService` de PrimeVue para emitir notificaciones consistentes (`showSuccess`, `showError`, `showWarn`, `showInfo`) con títulos y tiempos de visibilidad predeterminados.
3. **`useConfirmation()`:** Abstracción sobre `ConfirmationService` de PrimeVue para lanzar diálogos modales de confirmación antes de operaciones críticas (bajas lógicas, rechazos de expedientes, anulación de pagos).
4. **`usePagination()`:** Calcula páginas totales, índices de inicio/fin y páginas recortadas a partir de una lista reactiva en memoria.

---

## 11. Configuración, Entorno y Puesta en Marcha

### 11.1 Configuración de Vite (`vite.config.js`)

El empaquetador Vite incluye la resolución del alias `@` hacia `./src` y un proxy inverso para redirigir las peticiones `/api` al backend local de Spring Boot:

```javascript
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 5180,
    proxy: {
      "/api": {
        target: "http://localhost:8085",
        changeOrigin: true,
      },
    },
  },
});
```

---

### 11.2 Requisitos del Entorno

- **Node.js:** Versión 18.x o superior (LTS recomendado).
- **Gestor de Paquetes:** `npm` versión 9.x o superior (o `pnpm` / `yarn`).
- **Backend SGA:** Servicio Spring Boot ejecutándose en el puerto `8085` (`http://localhost:8085`).

---

### 11.3 Instrucciones de Ejecución y Compilación

```bash
# 1. Navegar al directorio del frontend
cd frontend

# 2. Instalar dependencias del proyecto
npm install

# 3. Iniciar el servidor de desarrollo local con Hot Module Replacement
npm run dev

# 4. Compilar y optimizar para despliegue en producción
npm run build

# 5. Previsualizar localmente el paquete de producción generado en /dist
npm run preview
```

Una vez iniciado el servidor de desarrollo, la aplicación estará accesible en:  
`http://localhost:5180` (o el puerto asignado por Vite, usualmente `5173`/`5180`).

---

## 12. Portal del Cliente B2B (`portal-cliente/`)

### 12.1 Arquitectura y Estructura Modular

El **Portal del Cliente** es una Single Page Application (SPA) independiente alojada en el directorio [`portal-cliente/`](file:///home/jiodev/Documents/Proyects/TransAduanas/portal-cliente), concebida específicamente para los clientes corporativos (importadores y exportadores) de TransAduanas.

Implementa una arquitectura desacoplada basada en dominios de autoservicio:

```
portal-cliente/src/
├── core/                                  # Infraestructura transversal del portal
│   ├── api/apiClient.js                   # Instancia Axios con inyección de Bearer token y redirección en 401
│   ├── layouts/PortalLayout.vue           # Shell corporativo (Topbar con RUC/Razón Social y navegación)
│   ├── router/index.js                    # Enrutamiento protegido para clientes con Navigation Guards
│   └── stores/authStore.js                # Pinia store de autenticación B2B y perfil empresarial
│
├── features/                              # Módulos de autoservicio B2B
│   ├── auth/                              # Autenticación y cambio de credenciales
│   │   └── views/
│   │       ├── PortalLoginView.vue        # Login B2B (RUC/Usuario + Contraseña)
│   │       └── PortalCambioPasswordObligatorioView.vue # Cambio forzoso de clave temporal
│   ├── cotizaciones/                      # Proformas y cotizaciones aduaneras
│   │   ├── services/portalCotizacionService.js
│   │   └── views/CotizacionesListView.vue # Bandeja de cotizaciones y aceptación formal con 1 clic
│   ├── expedientes/                       # Trazabilidad operativa de despachos
│   │   ├── services/portalExpedienteService.js
│   │   └── views/ExpedientesTrackingView.vue # Tracking en vivo (DAM, canal SUNAT, hitos y docs)
│   ├── pagos/                             # Obligaciones financieras y comprobantes
│   │   ├── services/portalPagoService.js
│   │   └── views/
│   │       ├── CobrosPagosView.vue        # Resumen financiero, anticipos y liquidaciones
│   │       └── RegistrarPagoView.vue      # Carga multipart de vouchers y subsanación
│   └── notificaciones/                    # Centro de avisos y notificaciones operativas
│       ├── services/portalNotificationService.js
│       └── views/PortalNotificacionesView.vue # Alertas de cambios de canal SUNAT, levante y pagos
│
└── shared/                                # Biblioteca de componentes reutilizables
    ├── components/                        # BaseModal.vue, BasePagination.vue
    ├── composables/                       # useNotification.js, useConfirmation.js, useTheme.js
    └── utils/                             # download.js (descarga de BL, facturas, vouchers y actas)
```

### 12.2 Consumo de Servicios y Endpoints B2B

El portal consume exclusivamente los endpoints del controlador `PortalController.java` bajo la ruta base `/api/v1/portal/**`:
- **Autenticación y Perfil:** `/api/v1/portal/auth/login`, `/api/v1/portal/auth/cambiar-password`, `/api/v1/portal/perfil`
- **Gestión de Cotizaciones:** `/api/v1/portal/cotizaciones`, `/api/v1/portal/cotizaciones/{id}`, `/api/v1/portal/cotizaciones/solicitar`, `/api/v1/portal/cotizaciones/{id}/aceptar`
- **Trazabilidad de Expedientes y Documentos:** `/api/v1/portal/expedientes`, `/api/v1/portal/expedientes/{id}`, `/api/v1/portal/expedientes/{id}/documentos`
- **Centro Financiero de Pagos:** `/api/v1/portal/pagos/resumen`, `/api/v1/portal/pagos/obligaciones-pendientes`, `/api/v1/portal/pagos/anticipos`, `/api/v1/portal/pagos/liquidaciones`, `/api/v1/portal/pagos/registrar-voucher`, `/api/v1/portal/pagos/upload-comprobante`, `/api/v1/portal/pagos/anticipos/{idPago}/corregir`
- **Notificaciones en Tiempo Real:** `/api/v1/portal/notificaciones`, `/api/v1/portal/notificaciones/resumen`, `/api/v1/portal/notificaciones/{id}/leer`, `/api/v1/portal/notificaciones/marcar-todas-leidas`

### 12.3 Sistema de Diseño Warm Editorial y Modo Oscuro

El Portal del Cliente mantiene paridad visual estricta con el sistema principal:
- **Tokens Warm Editorial:** Paleta cálida basada en variables CSS (`style.css` y `design-system.css`) con tonos arena (`#f6f3ee`), latte (`#ede6db`), crema (`#ebdcc9`) y espresso (`#352f2a`).
- **Tipografía Dual:** Inter para interfaces de lectura y JetBrains Mono para RUC, números de DAM, subpartidas NANDINA y montos monetarios en USD y PEN.
- **Preset PrimeVue Aura Personalizado:** Adaptado a los componentes corporativos sin sobrecarga de dependencias externas.
- **Soporte Bidireccional Claro / Oscuro:** Alternancia nativa gobernada por el composable `useTheme.js`, sincronizada con la clase CSS `.dark-theme` sobre `<html>` y persistida en `localStorage`.
