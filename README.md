# Sistema de Gestión Aduanera - Transaduanas

---

## Índice General

1. [Visión General del Proyecto](#1-visión-general-del-proyecto)
2. [Documentación Técnica Detallada (Docs)](#2-documentación-técnica-detallada-docs)
3. [Puntos Críticos y Reglas de Negocio del Dominio](#3-puntos-críticos-y-reglas-de-negocio-del-dominio)
4. [Tecnologías y Stack del Proyecto](#4-tecnologías-y-stack-del-proyecto)
   - [4.1 Backend: Java 21 & Spring Boot 4.1.2](#41-backend-java-21--spring-boot-412)
   - [4.2 Frontend Interno & Portal B2B: Vue 3 & Vite](#42-frontend-interno--portal-b2b-vue-3--vite)
   - [4.3 Base de Datos: MySQL 8.4 InnoDB](#43-base-de-datos-mysql-84-innodb)
5. [Arquitectura de Software del Sistema](#5-arquitectura-de-software-del-sistema)
   - [5.1 Arquitectura del Backend (Spring Boot N-Tier Orientada a Dominio)](#51-arquitectura-del-backend-spring-boot-n-tier-orientada-a-dominio)
   - [5.2 Arquitectura del Frontend (Vue 3 Feature-Based Architecture)](#52-arquitectura-del-frontend-vue-3-feature-based-architecture)
6. [Matriz de los 8 Roles Institucionales](#6-matriz-de-los-8-roles-institucionales)
7. [Estructura del Repositorio](#7-estructura-del-repositorio)
8. [Instalación y Despliegue (Entorno de Desarrollo)](#8-instalación-y-despliegue-entorno-de-desarrollo)

---

## 1. Visión General del Proyecto

**Sistema de Gestión Aduanera - Transaduanas** es una solución corporativa de alto rendimiento concebida para digitalizar, gobernar y sincronizar de extremo a extremo las operaciones aduaneras, logísticas y de transporte terrestre en el Perú, en estricto cumplimiento con las normativas de la **SUNAT** (Superintendencia Nacional de Aduanas y de Administración Tributaria) y el **MTC** (Ministerio de Transportes y Comunicaciones).

La plataforma erradica los silos operativos entre áreas comerciales, contables, agenciamiento aduanero, depósito temporal y transporte de carga pesada, integrando **compuertas financieras automatizadas**, **trazabilidad documental polimórfica**, un **Portal B2B de autogestión para clientes importadores/exportadores** y una política estricta de **Tolerancia Cero a Eliminación Física (Soft Delete)** para preservar auditorías fiscales.

---

## 2. Documentación Técnica Detallada (Docs)

El repositorio cuenta con especificaciones técnicas completas y guías especializadas ubicadas en la carpeta [`docs/`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs):

| Documento                                | Enlace                                                                                                        | Descripción y Contenido Clave                                                                                                                                                                                                                                                                                              |
| :--------------------------------------- | :------------------------------------------------------------------------------------------------------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Guía de Instalación (Local & Docker)** | [`docs/instalacion.md`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/instalacion.md)              | **1. Instalación Local (Entorno de Desarrollo):** carga de `schema.sql` y `seed.sql` en MySQL local, configuración de `application-local.properties`, arranque de Backend/Frontends y credenciales de prueba. **2. Instalación con Docker:** _(en implementación)_.                                                        |
| **Documentación Técnica del Backend**    | [`docs/backend.md`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/backend.md) y [`docs/backend/`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/backend) | **Documento Maestro y 6 Capítulos Especializados:** Arquitectura N-Tier, inventario de dependencias `pom.xml`, seguridad RBAC con JWT, catálogo de 21 controladores REST, compuerta financiera, deuda heredada, motor híbrido de reportes PDF y logging rotativo. |
| **Documentación Técnica del Frontend**   | [`docs/frontend.md`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/frontend.md), [`docs/app-sga/`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/app-sga) y [`docs/app-portal-clientes/`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/app-portal-clientes) | **Documento Maestro y Capítulos Separados:** Frontend Interno SGA (`app-sga/`, 6 capítulos) y Portal del Cliente B2B (`app-portal-cliente/`, 5 capítulos), arquitectura Feature-Based, Pinia stores, Navigation Guards, tokens Warm Editorial y catálogo de módulos. |
| **Especificación de Lenguaje de Diseño** | [`docs/lenguaje_diseño.md`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/lenguaje_dise%C3%B1o.md) | Sistema de diseño **Warm Editorial**, tokens CSS de color (arena, latte, espresso), tipografía dual (Inter y JetBrains Mono), rejilla de 4px, preset de PrimeVue Aura y guía de implementación de Modo Claro / Modo Oscuro.                                                                                                |

---

## 3. Puntos Críticos y Reglas de Negocio del Dominio

1. **Compuerta Financiera Pre-Expediente (_Financial Payment Gate_):**  
   Ningún expediente aduanero operativo puede ser creado en base de datos si la cotización comercial previa no cuenta con un pago o anticipo formalmente registrado y **validado por el área de Finanzas/Contador**.
2. **Tolerancia Cero a Borrado Físico (_Soft Delete_):**  
   Ningún registro de cliente, usuario, conductor, unidad de transporte, cotización, expediente o liquidación es eliminado mediante sentencias SQL `DELETE`. Se aplican banderas lógicas (`estado_activo = false`) o transiciones de estado terminal (`ANULADO`, `INACTIVO`).
3. **Segregación Funcional por 8 Roles Institucionales + Portal B2B:**  
   Control estricto de accesos a nivel de endpoints con Spring Security (`@PreAuthorize` mediante SpEL en `SecurityRoles.java`) y a nivel de rutas en el cliente web (Navigation Guards en `router.beforeEach`).
4. **Archivo Documental Centralizado:**  
   Almacenamiento y vinculación de archivos digitales sustentatorios (Factura Comercial, Packing List, BL, Guías de Remisión, Vouchers de Pago, Tickets de Balanza VGM) vinculados polimórficamente a cotizaciones o expedientes.
5. **Motor Híbrido de Reportería Oficial PDF:**  
   Emisión de padrones, certificados, cotizaciones y liquidaciones aduaneras en formato PDF corporativo mediante **JasperReports 7.0.8** y plantillas **Thymeleaf + OpenHTMLtoPDF**.
6. **Pesaje Certificado VGM y Control de Almacén:**  
   Custodia aduanera en depósito temporal con pesaje obligatorio en báscula certificada VGM (_Verified Gross Mass_) según enmienda SOLAS y verificación de canal SUNAT previo a la autorización de salida.

---

## 4. Tecnologías y Stack del Proyecto

El sistema adopta una arquitectura desacoplada y moderna, utilizando componentes probados de grado empresarial:

### 4.1 Backend: Java 21 & Spring Boot 4.1.2

Gestionado a través de **Apache Maven** ([`backend/pom.xml`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/pom.xml)):

| Grupo / Artefacto                                         | Versión | Propósito Técnico y Justificación                                                                                                  |
| :-------------------------------------------------------- | :-----: | :--------------------------------------------------------------------------------------------------------------------------------- |
| `org.springframework.boot:spring-boot-starter-data-jpa`   |  4.1.2  | Capa de persistencia relacional ORM con Hibernate, soporte transaccional `@Transactional` y repositorios Spring Data JPA.          |
| `org.springframework.boot:spring-boot-starter-security`   |  4.1.2  | Motor de seguridad corporativo; control de filtros HTTP, cifrado de contraseñas con BCrypt y autorización por roles mediante SpEL. |
| `org.springframework.boot:spring-boot-starter-validation` |  4.1.2  | Validaciones declarativas de contratos de entrada en DTOs con Jakarta Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Positive`).  |
| `org.springframework.boot:spring-boot-starter-webmvc`     |  4.1.2  | Framework web para la exposición de endpoints RESTful con serialización y deserialización JSON vía Jackson.                        |
| `org.springframework.boot:spring-boot-starter-thymeleaf`  |  4.1.2  | Motor de plantillas HTML del lado del servidor para la estructuración de documentos y reportes corporativos.                       |
| `com.openhtmltopdf:openhtmltopdf-pdfbox`                  | 1.0.10  | Motor de conversión de plantillas HTML/CSS a documentos PDF de alta fidelidad.                                                     |
| `net.sf.jasperreports:jasperreports` (+ `pdf`, `jdt`)     |  7.0.8  | Motor central de compilación y renderizado dinámico de reportes empresariales vectoriales en PDF.                                  |
| `io.jsonwebtoken:jjwt-api` (+ `impl`, `jackson`)          | 0.13.0  | API estándar para la creación, firma criptográfica HMAC-SHA256 y validación stateless de tokens JWT Bearer.                        |
| `com.mysql:mysql-connector-j`                             | Runtime | Driver JDBC oficial para comunicación de alto rendimiento con el motor MySQL 8.4 InnoDB.                                           |
| `org.projectlombok:lombok`                                | 1.18.x  | Reducción de código repetitivo (Boilerplate) para getters, setters, constructores y patrones Builder en DTOs y entidades.          |

---

### 4.2 Frontends del Ecosistema (Interno, Portal B2B y Landing): Vue 3 & Vite

Gestionados a través de **Node.js / npm** como proyectos completamente independientes en [`app-sga/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga), [`app-portal-cliente/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente) y [`landing/`](file:///home/jiodev/Documents/Proyects/TransAduanas/landing):

| Dependencia                     | Versión (`app-sga` / `app-portal-cliente`) | Propósito Técnico y Justificación                                                                         |
| :------------------------------ | :-------------------------------------: | :-------------------------------------------------------------------------------------------------------- |
| `vue`                           |            ^3.5.42 / ^3.5.13            | Framework progresivo reactivo utilizado con la Composition API y sintaxis `<script setup>`.               |
| `vite`                          |             ^8.3.0 / ^6.2.0             | Entorno de desarrollo ultrarrápido con Hot Module Replacement (HMR) y empaquetador Rollup.                |
| `vue-router`                    |             ^4.6.4 / ^4.5.0             | Enrutador oficial SPA con soporte de navegación vigilada (_Navigation Guards_) por JWT y matriz de roles. |
| `pinia`                         |             ^4.0.3 / ^3.0.1             | Almacén de estado global reactivo (`authStore` y `navigationStore`).                                      |
| `primevue` + `@primevue/themes` |             ^4.5.5 / ^4.3.1             | Suite de componentes de interfaz con el preset **Aura** adaptado al sistema _Warm Editorial_.             |
| `primeicons`                    |             ^8.0.2 / ^7.0.0             | Librería oficial de iconografía vectorial para operaciones aduaneras y logísticas.                        |
| `axios`                         |            ^1.20.0 / ^1.7.9             | Cliente HTTP con interceptores para inyección automática de token Bearer y cierre de sesión ante 401.     |
| `vitest` / `playwright`         |            ^5.0.5 / ^1.64.1             | Herramientas de pruebas unitarias y pruebas End-to-End (E2E).                                             |

---

### 4.3 Base de Datos: MySQL 8.4 InnoDB

- **Motor:** MySQL 8.4 LTS InnoDB (`SGAdb`).
- **Juego de Caracteres:** `utf8mb4` con colación `utf8mb4_unicode_ci` y zona horaria `-05:00` (Perú).
- **Aislamiento y Transaccionalidad:** Cumplimiento ACID completo con llaves foráneas e integridad referencial en 17 tablas normalizadas.
- **Despliegue:** Contenedor Docker/Podman preconfigurado ([`db/docker-compose.yml`](file:///home/jiodev/Documents/Proyects/TransAduanas/db/docker-compose.yml)) o instancia local en el puerto `3306`.

---

## 5. Arquitectura de Software del Sistema

### 5.1 Arquitectura del Backend (Spring Boot N-Tier Orientada a Dominio)

El backend implementa un diseño multicapa desacoplado donde ninguna capa superior conoce detalles de implementación de la inferior, asegurando alta cohesión y bajo acoplamiento:

```
[ Petición HTTP: /api/v1/... ]
             │
             ▼
┌───────────────────────────────────────┐
│          CONTROLLER LAYER             │  21 Controladores REST (@RestController, @PreAuthorize, @Valid)
│ (Validación de entrada y contratos)   │  Mapeo de DTOs y respuesta unificada ApiResponseDTO<T>
└──────────────────┬────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────┐
│           SERVICE LAYER               │  Interfaces (IService) + Implementaciones (@Service)
│   (Reglas de Negocio y Transacciones) │  @Transactional, control de compuertas y borrado lógico
└──────────────────┬────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────┐
│          REPOSITORY LAYER             │  Spring Data JPA (JpaRepository)
│        (Acceso a Datos SQL)           │  Queries derivadas, soft-delete y filtros activos
└──────────────────┬────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────┐
│            DATABASE LAYER             │  MySQL 8.4 InnoDB (17 tablas relacionales normalizadas)
└───────────────────────────────────────┘
```

#### Principios Clave del Backend:

1. **Desacoplamiento Estricto DTO / Entidad:** Las entidades JPA nunca se exponen en los endpoints. La capa de controladores se comunica únicamente mediante `RequestDTO` (validados con `@Valid` y Jakarta Validation) y `ResponseDTO`.
2. **Envoltorio Estándar `ApiResponseDTO<T>`:** Todas las respuestas JSON del sistema siguen una estructura predecible con campos `success`, `message`, `data` y `timestamp`.
3. **Seguridad Stateless JWT con SpEL:** Autenticación mediante cabecera `Authorization: Bearer <token>`. La clase `SecurityRoles.java` centraliza las expresiones de autorización evaluadas por Spring Security antes de ejecutar cualquier método del controlador.
4. **Manejo Centralizado de Excepciones:** `GlobalExceptionHandler` intercepta fallos de validación, recursos no encontrados (`ResourceNotFoundException`), violaciones de reglas de negocio (`BadRequestException`) y denegaciones de acceso (`AccessDeniedException`), retornando códigos HTTP semánticos (400, 401, 403, 404, 500).

---

### 5.2 Arquitectura del Frontend (Vue 3 Feature-Based Architecture)

El cliente web está diseñado como una Single Page Application (SPA) modular por dominios funcionales, con renderizado fluido y responsivo:

```
app-sga/src/
├── core/                                  # Núcleo horizontal de la aplicación
│   ├── api/apiClient.js                   # Instancia Axios con Interceptors (Token + 401 Redirect)
│   ├── auth/roles.js                      # Constantes de roles, rutas asignadas y etiquetas
│   ├── layouts/AppLayout.vue              # Shell maestro contenedor (Topbar + Sidebar colapsable)
│   ├── router/index.js                    # Router con Guardia de Seguridad Global (beforeEach)
│   └── stores/                            # Estado global Pinia (authStore, navigationStore)
│
├── features/                              # Módulos verticales por dominio de negocio
│   ├── admin/                             # Administración TI: Usuarios, Sedes, Catálogos, Logs y Usuarios Portal
│   ├── auth/                              # Autenticación: LoginView.vue y LandingView.vue
│   ├── gerencia/                          # Gerencia General: Dashboard Ejecutivo, Flota y Choferes
│   ├── comercial/                         # Recepción Comercial: Clientes, Calculadora y Cotizaciones
│   ├── aduanas/                           # Gestión Aduanera: Apertura, Expedientes, Canal DAM y Cierre
│   ├── almacen/                           # Depósito Temporal: Ingreso de Carga y Balanza VGM
│   ├── transporte/                        # Despachos en Ruta: Flota Tractos, Choferes MTC y Tracking
│   ├── finanzas/                          # Validación Financiera: Anticipos y Detracciones SPOT
│   ├── liquidacion/                       # Liquidaciones Finales: Tributos SUNAT, Gastos y Actas
│   ├── legal/                             # Asesoría Legal: Auditoría Regulatoria y Normativa
│   └── documentos/                        # Servicios y componentes de archivo documental
│
└── shared/                                # Biblioteca de componentes reutilizables
    ├── components/                        # BaseModal, BasePagination, HeroBannerCard, MetricCard
    └── composables/                       # useNotification (Toast), useConfirmation, useTheme
```

#### Principios Clave del Frontend:

1. **Shell Maestro y Navegación Dinámica:** `AppLayout.vue` gobierna la estructura de la aplicación. `AppSidebar.vue` detecta automáticamente el rol del usuario conectado y despliega exclusivamente los módulos y sub-opciones autorizadas. Los roles con privilegios de supervisión global (`ADMINISTRADOR_TI` y `GERENTE_GENERAL`) disponen de un selector de contexto para alternar entre los 8 módulos del sistema.
2. **Sistema de Diseño _Warm Editorial_:** Diseño editorial contemporáneo sin frameworks CSS pesados. Utiliza **Vanilla CSS** con variables personalizadas (`style.css`), rejilla de espaciados estricta de 4px (`--space-*`), tipografía dual (Inter para lectura, JetBrains Mono para códigos/montos) y soporte nativo para alternar entre **Modo Claro** y **Modo Oscuro**.
3. **Flujo de Peticiones y Manejo de Sesión:** `apiClient.js` inyecta automáticamente el token JWT almacenado en `localStorage`. Si el backend responde con `401 Unauthorized`, el interceptor elimina los datos de sesión y redirige inmediatamente al usuario a `/login` sin recargar la página.

---

## 6. Matriz de los 8 Roles Institucionales

```
                               ┌─────────────────────────┐
                               │     ADMINISTRADOR TI    │
                               │   (Acceso Total TI)     │
                               └────────────┬────────────┘
                                            │
                               ┌────────────▼────────────┐
                               │     GERENTE GENERAL     │
                               │  (Visión Global & KPIs) │
                               └────────────┬────────────┘
                                            │
         ┌───────────────────┬──────────────┼───────────────────┬───────────────────┐
         ▼                   ▼              ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────┐ ┌─────────────────┐ ┌─────────────────┐
│ AGENTE COMERCIAL│ │ SUPERVISOR ADU. │ │ AGENTE  │ │ SUPERVISOR TRASP│ │    CONTADOR     │
│ (Cotizaciones)  │ │(Expedientes/DAM)│ │ ADUAN.  │ │(Flota/Despachos)│ │(Finanzas/Cobros)│
└─────────────────┘ └─────────────────┘ └─────────┘ └─────────────────┘ └─────────────────┘
                                            │
                                            ▼
                                ┌───────────────────────┐
                                │     ASESOR LEGAL      │
                                │ (Auditoría Normativa) │
                                └───────────────────────┘
```

1. **`ADMINISTRADOR_TI`:** Gobernanza de cuentas internas, cuentas del Portal B2B, roles, sedes, catálogos y consola de logs del sistema.
2. **`GERENTE_GENERAL`:** Cuadros de mando ejecutivos, auditoría de flota, conductores y supervisión general.
3. **`AGENTE_COMERCIAL`:** Registro de clientes/prospectos y calculadora de cotizaciones de 4 pasos con flete y ad-valórem.
4. **`SUPERVISOR_ADUANERO`:** Auditoría de cotizaciones, apertura formal de expedientes y trámite de DAM con canal SUNAT.
5. **`AGENTE_ADUANERO`:** Control de pesaje balanza VGM en depósito temporal y verificación de levante aduanero.
6. **`SUPERVISOR_TRANSPORTE`:** Asignación de tractos y choferes MTC, emisión de guías de remisión y tracking en ruta.
7. **`CONTADOR`:** Validación obligatoria de anticipos pre-expediente, cálculo de detracciones SPOT y liquidación de gastos.
8. **`ASESOR_LEGAL`:** Fiscalización de permisos sectoriales (DIGEMID, SENASA, MTC, SUCAMEC), contratos y cumplimiento normativo.

---

## 7. Estructura del Repositorio

```
TransAduanas/
├── backend/                                   # API Backend (Spring Boot 4.1.2 / Java 21)
│   ├── pom.xml                                # Dependencias y configuración Maven
│   ├── application-local.properties.example   # Plantilla para variables y credenciales locales
│   └── src/main/                              # Código fuente Java (21 controllers, 17 entities) y recursos
│
├── app-sga/                                   # Frontend Interno Operativo (Vue 3 / Vite 8 / Puerto 5180)
│   ├── package.json                           # Dependencias npm y scripts
│   ├── vite.config.js                         # Servidor dev (5180) y proxy inverso hacia :8085
│   └── src/                                   # Módulos de los 8 roles institucionales
│
├── app-portal-cliente/                        # Portal del Cliente B2B (Vue 3 / Vite 6 / Puerto 5174)
│   ├── package.json                           # SPA corporativa para clientes importadores/exportadores
│   ├── vite.config.js                         # Servidor dev (5174) y proxy inverso hacia :8085
│   └── src/                                   # Vistas de tracking, cotizaciones, pagos y notificaciones B2B
│
├── landing/                                   # Landing Page Pública Independiente (Vue 3 / Vite 6 / Puerto 5173)
│   ├── package.json                           # SPA independiente para difusión comercial y accesos al ecosistema
│   ├── vite.config.js                         # Servidor dev en puerto 5173
│   └── src/                                   # Vista de inicio, tokens editoriales y enlaces desacoplados
│
├── db/                                        # Base de Datos MySQL 8.4 InnoDB
│   ├── docker-compose.yml                     # Contenedor MySQL 8.4 con carga automática de scripts
│   ├── schema.sql                             # Script DDL con las 17 tablas del modelo relacional
│   └── seed.sql                               # Datos semilla completos (sedes, catálogos, usuarios, clientes)
│
├── docs/                                      # Documentación Técnica Especializada
│   ├── instalacion.md                         # Guía paso a paso de instalación (Local y Docker)
│   ├── backend.md                             # Documento maestro de arquitectura del Backend
│   ├── backend/                               # Capítulos especializados del Backend (6 módulos)
│   ├── frontend.md                            # Documento maestro de arquitectura Frontend
│   ├── app-sga/                               # Capítulos especializados del Frontend Interno SGA (6 módulos)
│   ├── app-portal-clientes/                   # Capítulos especializados del Portal B2B (5 módulos)
│   └── lenguaje_diseño.md                     # Guía de estilos y tokens del Sistema Warm Editorial
│
└── README.md                                  # Documento maestro de arquitectura y visión general
```

---

## 8. Instalación y Despliegue (Entorno de Desarrollo)

> [!IMPORTANT]
> **Guía de Instalación Separada (`docs/instalacion.md`):**  
> La documentación de instalación se encuentra estructurada en dos secciones dentro de [`docs/instalacion.md`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/instalacion.md):
>
> 1. **Instalación Local (Entorno de Desarrollo):** Con todos los pasos necesarios para configurar y probar el proyecto completo de manera local.
> 2. **Instalación con Docker:** _(Sección reservada, pendiente de implementación)._
>
> 👉 **[Ver Guía Completa de Instalación (`docs/instalacion.md`)](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/instalacion.md)**

### Resumen Rápido de Servicios (Instalación Local)

| Servicio                       | Directorio                                                                              | Tecnología                  | Puerto Local | Comando de Arranque Rápido                                           |
| :----------------------------- | :-------------------------------------------------------------------------------------- | :-------------------------- | :----------: | :------------------------------------------------------------------- |
| **1. Base de Datos (`SGAdb`)** | [`db/`](file:///home/jiodev/Documents/Proyects/TransAduanas/db)                         | MySQL 8.4 InnoDB            |    `3306`    | `mysql -u root -p < db/schema.sql && mysql -u root -p < db/seed.sql` |
| **2. Backend API REST**        | [`backend/`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend)               | Spring Boot 4.1.2 / Java 21 |    `8085`    | `cd backend && mvn spring-boot:run`                                  |
| **3. Frontend Interno (SGA)**  | [`app-sga/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga)             | Vue 3 + Vite 8              |    `5180`    | `cd app-sga && npm install && npm run dev`                          |
| **4. Portal del Cliente B2B**  | [`app-portal-cliente/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente) | Vue 3 + Vite 6              |    `5174`    | `cd app-portal-cliente && npm install && npm run dev`                    |

Para configurar `application-local.properties`, revisar los requisitos previos o consultar las credenciales de los **8 roles institucionales** y cuentas RUC del **Portal B2B**, dirígete a [`docs/instalacion.md`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/instalacion.md).
