# 1. Introducción y Arquitectura de la Aplicación SGA

**Sistema de Gestión Aduanera - Transaduanas (`app-sga`)**  
*Manual de Arquitectura Frontend para Perfiles Técnicos y de Negocio*

---

## Índice del Capítulo
1. [¿Qué es `app-sga` y a quién sirve? (Explicación para no técnicos)](#1-qué-es-app-sga-y-a-quién-sirve-explicación-para-no-técnicos)
2. [Principios de Arquitectura Frontend](#2-principios-de-arquitectura-frontend)
3. [Estructura Basada en Características (Feature-Based Architecture)](#3-estructura-basada-en-características-feature-based-architecture)
4. [Filosofía Visual: Sistema de Diseño "Warm Editorial"](#4-filosofía-visual-sistema-de-diseño-warm-editorial)
5. [Árbol Físico de Directorios de `app-sga/src/`](#5-árbol-físico-de-directorios-de-app-sgasrc)

---

## 1. ¿Qué es `app-sga` y a quién sirve? (Explicación para no técnicos)

### La Analogía del Panel de Control de una Torre de Aeropuerto
Imagina la cabina de control de un aeropuerto internacional:
- Hay radares para los controladores aéreos, consolas de combustible para los mecánicos, pantallas de aduanas para la policía fiscal y tableros financieros para la administración. Cada especialista tiene su propia pantalla personalizada con las herramientas exactas que necesita para su trabajo, sin estorbar a los demás pero todos compartiendo la misma información en tiempo real.

**`app-sga`** es exactamente eso para la agencia de aduanas y transporte:
Es la aplicación web interna (Single Page Application - SPA) donde trabajan los **8 perfiles operativos** de Transaduanas:
1. El **Vendedor (Agente Comercial)** cotiza fletes internacionales y calcula los impuestos estimados de la SUNAT en un asistente de 4 pasos.
2. El **Contador** revisa los comprobantes bancarios y desbloquea los expedientes al validar el dinero.
3. El **Supervisor Aduanero** gestiona la DAM ante SUNAT y registra si la carga salió en canal Verde, Naranja o Rojo.
4. El **Operador de Almacén** pesa los contenedores en básculas certificadas VGM según la norma internacional SOLAS.
5. El **Jefe de Transporte** asigna tractos y choferes con brevete profesional MTC y emite la guía de remisión.
6. El **Asesor Legal** vigila que no falte ningún permiso de DIGEMID, SENASA o SUCAMEC.
7. El **Gerente General** supervisa los indicadores de rentabilidad, flota y clientes.
8. El **Administrador TI** gobierna usuarios, sedes, catálogos y revisa la consola de logs del sistema en vivo.

---

## 2. Principios de Arquitectura Frontend

| Principio | Para qué sirve (Explicación Sencilla) | Detalle Técnico de Ingeniería |
| :--- | :--- | :--- |
| **Single Page Application (SPA)** | La página nunca se "recarga en blanco" al navegar; los cambios de pantalla son instantáneos como en una app de escritorio. | Construida con **Vue 3** y **Vue Router 4** con navegación dinámica asíncrona mediante `import()` (*lazy-loading* de rutas). |
| **Composition API `<script setup>`** | Código ordenado por funcionalidad y no por tipos de variables, facilitando reutilizar lógica entre vistas. | Utiliza la sintaxis concisa de Vue 3.5+ con composables (`useCotizacionCalculator`, `useNotification`, `useTheme`). |
| **Almacén de Estado con Pinia** | Toda la aplicación recuerda quién es el usuario conectado y qué rol tiene sin tener que preguntárselo al servidor en cada clic. | Stores reactivos tipados: `authStore` (gestión de sesión y token JWT) y `navigationStore` (sincronización modular). |
| **Segregación Estricta por Roles** | Cada empleado ve únicamente los menús y botones correspondientes a su cargo. | Guardias de navegación globales en el router (`beforeEach`) y barra lateral reactiva (`AppSidebar.vue`) que filtra opciones por `ROLES`. |
| **Modo Claro / Modo Oscuro Nativo** | Comodidad visual para operadores que trabajan jornadas continuas de día o de noche en almacenes y oficinas. | Implementado mediante variables CSS nativas y el composable `useTheme` sin recargar hojas de estilo. |

---

## 3. Estructura Basada en Características (Feature-Based Architecture)

A diferencia de los proyectos antiguos donde todos los componentes se mezclaban en una sola carpeta gigante, `app-sga` organiza el código **por dominio de negocio (*Features*)**:

```
app-sga/src/
├── core/                                  # El núcleo común (infraestructura horizontal)
│   ├── api/apiClient.js                   # Axios configurado con interceptores JWT y logout en 401
│   ├── auth/roles.js                      # Constantes y etiquetas de los 8 roles institucionales
│   ├── layouts/AppLayout.vue              # Shell maestro (Topbar + Sidebar colapsable)
│   ├── router/                            # Enrutador con guardias de seguridad por rol
│   └── stores/                            # Stores globales Pinia (navigationStore)
│
├── features/                              # Módulos verticales por proceso aduanero/logístico
│   ├── admin/                             # Administración TI (Usuarios, Sedes, Catálogos, Logs)
│   ├── auth/                              # Autenticación (LoginView, authStore, authService)
│   ├── gerencia/                          # Gerencia General (Dashboard Ejecutivo, Flota, Conductores)
│   ├── comercial/                         # Comercial (Clientes, Cotizador de 4 pasos, Proformas)
│   ├── aduanas/                           # Gestión Aduanera (Expedientes, DAM, Canal SUNAT)
│   ├── almacen/                           # Depósito Temporal (Ingreso almacén, Báscula VGM, Levante)
│   ├── transporte/                        # TMS y Despachos (Flota pesada, Choferes MTC, Rutas)
│   ├── finanzas/                          # Finanzas (Validación de anticipos y compuerta de pago)
│   ├── liquidacion/                       # Liquidaciones (Tributos SUNAT, Gastos, SPOT 12%)
│   ├── legal/                             # Asesoría Legal (Fiscalización normativa y permisos)
│   └── documentos/                        # Servicios y componentes de archivo digital
│
└── shared/                                # Biblioteca transversal de componentes y composables
    ├── components/                        # BaseModal, BasePagination, HeroBannerCard, MetricCard
    └── composables/                       # useNotification (Toast), useConfirmation, useTheme
```

---

## 4. Filosofía Visual: Sistema de Diseño "Warm Editorial"

La interfaz visual de `app-sga` se aparta conscientemente de las plantillas genéricas azuladas o grises estériles, adoptando una estética **Warm Editorial (Minimalismo Editorial Cálido)**:
- **Colores de Inspiración Terrestre y Portuaria:** Tonos arena (`--color-sand-*`), latte/lino (`--color-latte-*`) y café espresso (`--color-espresso-*`), combinados con acentos de advertencia y semáforos oficiales de la aduana (Verde SUNAT, Naranja Documentario, Rojo Aforo).
- **Tipografía Dual Estratégica:**
  - **Inter (`var(--font-sans)`):** Diseñada para pantallas de alta densidad; máxima legibilidad en tablas extensas y formularios.
  - **JetBrains Mono (`var(--font-mono)`):** Tipografía monoespaciada empleada para códigos normativos (`COT-2026-0001`, `EXP-2026-0001`), partidas arancelarias NANDINA, RUCs y cifras monetarias en dólares (`$ USD`), garantizando alineación vertical perfecta de dígitos.
- **Rejilla Estricta de 4px:** Todos los espaciados, paddings y márgenes siguen múltiplos matemáticos de 4px (`--space-1` = 4px, `--space-2` = 8px, `--space-4` = 16px, `--space-6` = 24px), garantizando un ritmo visual armónico.

---

## 5. Árbol Físico de Directorios de `app-sga/src/`

```
app-sga/
├── index.html                             # Plantilla HTML raíz con precarga de fuentes
├── package.json                           # Dependencias npm (Vue 3.5, Vite 8, PrimeVue 4, Pinia 4)
├── vite.config.js                         # Configuración Vite (puerto 5180, proxy /api hacia :8085)
└── src/
    ├── App.vue                            # Componente raíz con Toast y confirmaciones globales
    ├── main.js                            # Inicialización de Vue, Pinia, Router y PrimeVue
    ├── style.css                          # Variables del Sistema Warm Editorial y utilidades CSS
    ├── core/                              # Layout, Router, Roles y API Client
    ├── features/                          # Los 10 módulos funcionales del negocio
    └── shared/                            # Componentes reutilizables accesibles y composables
```
