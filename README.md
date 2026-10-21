# Transaduanas Platform

**Plataforma Integral de Gestión Aduanera y Logística Internacional**  
*Basado en la arquitectura del proyecto SGA (Sistema de Gestión Aduanera)*

---

## Índice General

1. [Visión General del Proyecto](#1-visión-general-del-proyecto)
2. [Documentación Técnica Detallada (Docs)](#2-documentación-técnica-detallada-docs)
3. [Puntos Críticos y Reglas de Negocio del Dominio](#3-puntos-críticos-y-reglas-de-negocio-del-dominio)
4. [Tecnologías y Stack del Proyecto](#4-tecnologías-y-stack-del-proyecto)
   - [4.1 Backend: Java 21 & Spring Boot 4.1.x](#41-backend-java-21--spring-boot-41x)
   - [4.2 Frontend: Vue 3 & Vite 8](#42-frontend-vue-3--vite-8)
   - [4.3 Base de Datos: MySQL 8.4 InnoDB](#43-base-de-datos-mysql-84-innodb)
5. [Arquitectura de Software del Sistema](#5-arquitectura-de-software-del-sistema)
   - [5.1 Arquitectura del Backend (Spring Boot N-Tier Orientada a Dominio)](#51-arquitectura-del-backend-spring-boot-n-tier-orientada-a-dominio)
   - [5.2 Arquitectura del Frontend (Vue 3 Feature-Based Architecture)](#52-arquitectura-del-frontend-vue-3-feature-based-architecture)
6. [Matriz de los 8 Roles Institucionales](#6-matriz-de-los-8-roles-institucionales)
7. [Estructura del Repositorio](#7-estructura-del-repositorio)
8. [Guía de Inicio Rápido y Ejecución Local](#8-guía-de-inicio-rápido-y-ejecución-local)
   - [8.1 Requisitos Previos](#81-requisitos-previos)
   - [8.2 Puesta en Marcha del Backend](#82-puesta-en-marcha-del-backend)
   - [8.3 Puesta en Marcha del Frontend](#83-puesta-en-marcha-del-frontend)
   - [8.4 Credenciales de Arranque del Sistema](#84-credenciales-de-arranque-del-sistema)

---

## 1. Visión General del Proyecto

**Transaduanas Platform** es una solución corporativa de alto rendimiento concebida para digitalizar, gobernar y sincronizar de extremo a extremo las operaciones aduaneras, logísticas y de transporte terrestre en el Perú, en estricto cumplimiento con las normativas de la **SUNAT** (Superintendencia Nacional de Aduanas y de Administración Tributaria) y el **MTC** (Ministerio de Transportes y Comunicaciones).

La plataforma erradica los silos operativos entre áreas comerciales, contables, agenciamiento aduanero, depósito temporal y transporte de carga pesada, integrando **compuertas financieras automatizadas**, **trazabilidad documental polimórfica** y una política estricta de **Tolerancia Cero a Eliminación Física (Soft Delete)** para preservar auditorías fiscales.

---

## 2. Documentación Técnica Detallada (Docs)

El repositorio cuenta con especificaciones técnicas completas y exhaustivas ubicadas en la carpeta [`docs/`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs):

| Documento | Enlace | Descripción y Contenido Clave |
| :--- | :--- | :--- |
| **Documentación Técnica del Backend** | [`docs/backend.md`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/backend.md) | Arquitectura N-Tier, patrones de diseño (Builder, Strategy, Wrapper, Soft Delete), dependencias `pom.xml`, modelo de seguridad RBAC con SpEL, catálogo de 19 controladores REST, motor de cálculo contable (IGV y SPOT 12%), JasperReports en memoria y logging rotativo. |
| **Documentación Técnica del Frontend** | [`docs/frontend.md`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/frontend.md) | Arquitectura por características (*Feature-Based*), Pinia Stores (`authStore`, `navigationStore`), Vue Router con Navigation Guards, integración Axios con Bearer y logout en 401, catálogo de componentes y modales de los 8 módulos operativos. |
| **Especificación de Lenguaje de Diseño** | [`docs/lenguaje_diseño.md`](file:///home/jiodev/Documents/Proyects/TransAduanas/docs/lenguaje_dise%C3%B1o.md) | Sistema de diseño **Warm Editorial**, tokens CSS de color (arena, latte, espresso), tipografía dual (Inter y JetBrains Mono), rejilla de 4px, preset de PrimeVue Aura y guía de implementación de Modo Claro / Modo Oscuro. |

---

## 3. Puntos Críticos y Reglas de Negocio del Dominio

1. **Compuerta Financiera Pre-Expediente (*Financial Payment Gate*):**  
   Ningún expediente aduanero operativo puede ser creado en base de datos si la cotización comercial previa no cuenta con un pago o anticipo formalmente registrado y **validado por el área de Finanzas/Contador**.
2. **Tolerancia Cero a Borrado Físico (*Soft Delete*):**  
   Ningún registro de cliente, usuario, conductor, unidad de transporte, cotización, expediente o liquidación es eliminado mediante sentencias SQL `DELETE`. Se aplican banderas lógicas (`estado_activo = false`) o transiciones de estado terminal (`ANULADO`, `INACTIVO`).
3. **Segregación Funcional por 8 Roles Institucionales:**  
   Control estricto de accesos a nivel de endpoints con Spring Security (`@PreAuthorize` mediante SpEL en `SecurityRoles.java`) y a nivel de rutas en el cliente web (Navigation Guards en `router.beforeEach`).
4. **Archivo Documental Centralizado:**  
   Almacenamiento y vinculación de archivos digitales sustentatorios (Factura Comercial, Packing List, BL, Guías de Remisión, Vouchers de Pago, Tickets de Balanza VGM) vinculados polimórficamente a cotizaciones o expedientes.
5. **Reportería Oficial Impresa:**  
   Emisión de padrones, certificados y reportes aduaneros en formato PDF corporativo vectorial compilados dinámicamente con **JasperReports 7.0.8** en memoria.
6. **Pesaje Certificado VGM y Control de Almacén:**  
   Custodia aduanera en depósito temporal con pesaje obligatorio en báscula certificada VGM (*Verified Gross Mass*) según enmienda SOLAS y verificación de canal SUNAT previo a la autorización de salida.

---

## 4. Tecnologías y Stack del Proyecto

El sistema adopta una arquitectura desacoplada y moderna, utilizando componentes probados de grado empresarial:

### 4.1 Backend: Java 21 & Spring Boot 4.1.x
Gestionado a través de **Apache Maven** ([`backend/pom.xml`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/pom.xml)):

| Grupo / Artefacto | Versión | Propósito Técnico y Justificación |
| :--- | :---: | :--- |
| `org.springframework.boot:spring-boot-starter-data-jpa` | 4.1.1 | Capa de persistencia relacional ORM con Hibernate, soporte transaccional `@Transactional` y repositorios Spring Data JPA. |
| `org.springframework.boot:spring-boot-starter-security` | 4.1.1 | Motor de seguridad corporativo; control de filtros HTTP, cifrado de contraseñas con BCrypt y autorización por roles mediante SpEL. |
| `org.springframework.boot:spring-boot-starter-validation` | 4.1.1 | Validaciones declarativas de contratos de entrada en DTOs con Jakarta Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Positive`). |
| `org.springframework.boot:spring-boot-starter-webmvc` | 4.1.1 | Framework web para la exposición de endpoints RESTful con serialización y deserialización JSON vía Jackson. |
| `com.mysql:mysql-connector-j` | Runtime | Driver JDBC oficial para comunicación de alto rendimiento con el motor MySQL 8.4 InnoDB. |
| `org.projectlombok:lombok` | 1.18.x | Reducción de código repetitivo (Boilerplate) para getters, setters, constructores y patrones Builder en DTOs y entidades. |
| `net.sf.jasperreports:jasperreports` | 7.0.8 | Motor central de compilación y renderizado de plantillas de reportes dinámicos empresariales. |
| `net.sf.jasperreports:jasperreports-pdf` | 7.0.8 | Módulo de renderizado de salida para exportación nativa a formato PDF corporativo con tablas vectoriales. |
| `net.sf.jasperreports:jasperreports-jdt` | 7.0.8 | Compilador JDT Eclipse integrado para expresiones Java dentro de los reportes JasperReports. |
| `io.jsonwebtoken:jjwt-api` | 0.13.0 | API estándar para la creación, firma criptográfica HMAC-SHA256 y parseo de tokens JWT Bearer. |
| `io.jsonwebtoken:jjwt-impl` | 0.13.0 | Implementación del motor JWT de JJWT para procesamiento stateless. |
| `io.jsonwebtoken:jjwt-jackson` | 0.13.0 | Serializador Jackson para el manejo de claims y payloads JSON dentro del token JWT. |

---

### 4.2 Frontend: Vue 3 & Vite 8
Gestionado a través de **Node.js / npm** ([`frontend/package.json`](file:///home/jiodev/Documents/Proyects/TransAduanas/frontend/package.json)):

| Dependencia | Versión | Propósito Técnico y Justificación |
| :--- | :---: | :--- |
| `vue` | ^3.5.42 | Framework progresivo reactivo utilizado con la Composition API y sintaxis concisa `<script setup>`. |
| `vite` | ^8.3.0 | Entorno de desarrollo ultrarrápido con Hot Module Replacement (HMR) y empaquetador de producción optimizado vía Rollup. |
| `@vitejs/plugin-vue` | ^6.0.8 | Plugin oficial de compilación de Single File Components (SFC `.vue`) para Vite. |
| `vue-router` | ^4.6.4 | Enrutador oficial SPA con soporte de navegación vigilada (*Navigation Guards*) por JWT y matriz de roles. |
| `pinia` | ^4.0.3 | Almacén de estado global reactivo y tipado (Manejo de sesión `authStore` y sincronización modular `navigationStore`). |
| `primevue` | ^4.5.5 | Suite integral de componentes de interfaz (Diálogos modales, tablas avanzadas, inputs accesibles y confirmaciones). |
| `@primevue/themes` | ^4.5.4 | Sistema de temas y estilos base integrados para PrimeVue 4 (Preset Aura personalizado). |
| `primeicons` | ^8.0.2 | Librería oficial de glifos vectoriales para señalética de aduanas, transporte, estados y acciones. |
| `axios` | ^1.20.0 | Cliente HTTP basado en promesas; configurado con interceptores para inyección de token Bearer y deslogueo en 401. |

---

### 4.3 Base de Datos: MySQL 8.4 InnoDB
* **Motor:** MySQL 8.4 LTS InnoDB.
* **Juego de Caracteres:** `utf8mb4` con colación `utf8mb4_unicode_ci` para soporte internacional completo.
* **Aislamiento y Transaccionalidad:** Cumplimiento ACID completo con llaves foráneas `ON UPDATE CASCADE ON DELETE RESTRICT`.
* **Entorno de Despliegue:** Contenedor OCI (Docker / Podman) o servidor local en el puerto `3306:3306`.

---

## 5. Arquitectura de Software del Sistema

### 5.1 Arquitectura del Backend (Spring Boot N-Tier Orientada a Dominio)

El backend implementa un diseño multicapa desacoplado donde ninguna capa superior conoce detalles de implementación de la inferior, asegurando alta cohesión y bajo acoplamiento:

```
[ Petición HTTP: /api/v1/... ]
             │
             ▼
┌───────────────────────────────────────┐
│          CONTROLLER LAYER             │  @RestController, @PreAuthorize, @Valid
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
│            DATABASE LAYER             │  MySQL 8.4 InnoDB (16 tablas relacionales normalizadas)
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
frontend/src/
├── core/                                  # Núcleo horizontal de la aplicación
│   ├── api/apiClient.js                   # Instancia Axios con Interceptors (Token + 401 Redirect)
│   ├── auth/roles.js                      # Constantes de roles, rutas asignadas y etiquetas
│   ├── layouts/AppLayout.vue              # Shell maestro contenedor (Topbar + Sidebar colapsable)
│   ├── router/index.js                    # Router con Guardia de Seguridad Global (beforeEach)
│   └── stores/                            # Estado global Pinia (authStore, navigationStore)
│
├── features/                              # Módulos verticales por dominio de negocio
│   ├── admin/                             # Administración TI: Usuarios, Sedes, Catálogos y Logs
│   ├── auth/                              # Autenticación: LoginView.vue
│   ├── gerencia/                          # Gerencia General: Padrón de Flota y Choferes MTC
│   ├── roles-modules/                     # Módulos Operativos Centrales:
│   │   ├── services/                      # Servicios HTTP de dominio (comercial, aduanas, finanzas)
│   │   ├── components/                    # Modales y formularios de alta densidad (Cotizacion, DAM, Pagos)
│   │   └── views/                         # Vistas de módulo (Comercial, Aduanas, Transporte, Finanzas)
│   └── documentos/                        # Servicios y componentes de archivo documental
│
└── shared/                                # Biblioteca de componentes reutilizables
    ├── components/                        # BaseModal, BasePagination, HeroBannerCard, MetricCard
    └── composables/                       # useNotification (Toast), useConfirmation, useTheme
```

#### Principios Clave del Frontend:
1. **Shell Maestro y Navegación Dinámica:** `AppLayout.vue` gobierna la estructura de la aplicación. `AppSidebar.vue` detecta automáticamente el rol del usuario conectado y despliega exclusivamente los módulos y sub-opciones autorizadas. Los roles con privilegios de supervisión global (`ADMINISTRADOR_TI` y `GERENTE_GENERAL`) disponen de un selector de contexto para alternar entre los 8 módulos del sistema.
2. **Sistema de Diseño *Warm Editorial*:** Diseño editorial contemporáneo sin frameworks CSS pesados. Utiliza **Vanilla CSS** con variables personalizadas (`style.css`), rejilla de espaciados estricta de 4px (`--space-*`), tipografía dual (Inter para lectura, JetBrains Mono para códigos/montos) y soporte nativo para alternar entre **Modo Claro** y **Modo Oscuro**.
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

1. **`ADMINISTRADOR_TI`:** Gobernanza de cuentas, roles, sedes territoriales y consola de logs del sistema.
2. **`GERENTE_GENERAL`:** Cuadros de mando ejecutivos, auditoría de flota, conductores y supervisión general.
3. **`AGENTE_COMERCIAL`:** Registro de clientes, prospectos y calculadora de cotizaciones de 4 pasos con flete y ad-valórem.
4. **`SUPERVISOR_ADUANERO`:** Auditoría de cotizaciones, apertura formal de expedientes y trámite de DAM con canal SUNAT.
5. **`AGENTE_ADUANERO`:** Control de pesaje balanza VGM en depósito temporal y verificación de levante aduanero.
6. **`SUPERVISOR_TRANSPORTE`:** Asignación de tractos y choferes MTC, emisión de guías de remisión y tracking en ruta.
7. **`CONTADOR`:** Validación obligatoria de anticipos pre-expediente, cálculo de detracciones SPOT y liquidación de gastos.
8. **`ASESOR_LEGAL`:** Fiscalización de permisos sectoriales (DIGEMID, SENASA), contratos y cumplimiento de plazos.

---

## 7. Estructura del Repositorio

```
TransAduanas/
├── backend/                             # Código fuente del Backend (Spring Boot 4.1.x / Java 21)
│   ├── pom.xml                          # Configuración Maven de dependencias y plugins
│   └── src/main/                        # Fuentes Java y recursos de configuración
│
├── frontend/                            # Código fuente del Frontend (Vue 3 / Vite 8 / PrimeVue)
│   ├── package.json                     # Manifiesto de dependencias npm y scripts
│   ├── vite.config.js                   # Configuración del servidor Vite y proxy inverso
│   └── src/                             # Componentes, vistas, stores y estilos Warm Editorial
│
├── docs/                                # Documentación Técnica Especializada
│   ├── backend.md                       # Especificación técnica profunda del Backend
│   ├── frontend.md                      # Especificación técnica profunda del Frontend
│   └── lenguaje_diseño.md               # Guía de estilos y tokens del Sistema Warm Editorial
│
├── db/                                  # Scripts de base de datos MySQL InnoDB
└── README.md                            # Documento maestro del proyecto
```

---

## 8. Guía de Inicio Rápido y Ejecución Local

### 8.1 Requisitos Previos

- **Java JDK:** Versión 21 LTS instalada (`java -version`).
- **Apache Maven:** Versión 3.9+ instalada (`mvn -version`).
- **Node.js:** Versión 18.x o 20.x LTS instalada (`node -v`).
- **npm:** Versión 9+ instalada (`npm -v`).
- **MySQL Server:** Versión 8.0 o 8.4 LTS activa en el puerto `3306`.

---

### 8.2 Puesta en Marcha del Backend

```bash
# 1. Ingresar a la carpeta del backend
cd backend

# 2. Compilar el proyecto y descargar dependencias
mvn clean compile

# 3. Iniciar el servidor Spring Boot (puerto 8085)
mvn spring-boot:run
```

El servidor estará operativo en:  
`http://localhost:8085`  
Verificación de salud: `http://localhost:8085/api/v1/health`

---

### 8.3 Puesta en Marcha del Frontend

En otra terminal:

```bash
# 1. Ingresar a la carpeta del frontend
cd frontend

# 2. Instalar las dependencias de Node.js
npm install

# 3. Iniciar el servidor de desarrollo Vite con Hot Module Replacement
npm run dev
```

La aplicación web estará disponible en su navegador en:  
`http://localhost:5180` (o `http://localhost:5173`)

---

### 8.4 Credenciales de Arranque del Sistema

Al iniciarse el backend por primera vez, la clase `DataInitializer` verifica y auto-aprovisiona los roles del sistema, la sede base y la cuenta administrativa inicial:

- **Usuario:** `admin`
- **Contraseña:** `Password123!`
- **Rol:** `ADMINISTRADOR_TI`

> [!NOTE]
> Una vez iniciada la sesión con el usuario `admin`, este puede crear las cuentas adicionales para los demás roles (`GERENTE_GENERAL`, `AGENTE_COMERCIAL`, `SUPERVISOR_ADUANERO`, `CONTADOR`, etc.) directamente desde el **Gestor de Usuarios** en la ruta `/administracion-ti/usuarios`.
