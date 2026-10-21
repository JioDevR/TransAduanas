# Transaduanas Platform
**Plataforma Integral de Gestión Aduanera y Logística Internacional**  
*Basado en la arquitectura del proyecto de SGA (Sistema de Gestión Aduanera)*

---

## 1. Visión General del Proyecto

**Transaduanas Platform** es una solución corporativa de alto rendimiento concebida para digitalizar, gobernar y sincronizar de extremo a extremo las operaciones aduaneras, logísticas y de transporte terrestre en el Perú, en estricto cumplimiento con las normativas de la **SUNAT** (Superintendencia Nacional de Aduanas y de Administración Tributaria) y el **MTC** (Ministerio de Transportes y Comunicaciones).

La plataforma erradica los silos operativos entre áreas comerciales, contables, agenciamiento aduanero, depósito temporal y transporte de carga pesada, integrando **compuertas financieras automatizadas**, **trazabilidad documental polimórfica** y una política estricta de **Tolerancia Cero a Eliminación Física (Soft Delete)** para preservar auditorías fiscales.

---

## 2. Puntos Críticos y Reglas de Negocio del Dominio

1. **Compuerta Financiera Pre-Expediente (*Financial Payment Gate*):**  
   Ningún expediente aduanero operativo puede ser creado en base de datos si la cotización comercial previa no cuenta con un pago o anticipo formalmente registrado y **validado por el área de Finanzas/Contador**.
2. **Tolerancia Cero a Borrado Físico (*Soft Delete*):**  
   Ningún registro de cliente, usuario, conductor, unidad de transporte, cotización, expediente o liquidación es eliminado mediante sentencias `DELETE`. Se aplican banderas lógicas (`estado_activo = false`) o transiciones de estado (`ANULADO`, `INACTIVO`).
3. **Segregación Funcional por 8 Roles Institucionales:**  
   Control estricto de accesos a nivel de endpoints con Spring Security (`@PreAuthorize` mediante SpEL) y a nivel de rutas en el cliente web (Navigation Guards de Vue Router).
4. **Archivo Documental Centralizado:**  
   Almacenamiento y vinculación de archivos digitales sustentatorios (Invoice, Packing List, BL, Guías, Vouchers, Tickets de Balanza VGM) vinculados a cualquier entidad del sistema.
5. **Reportería Oficial Impresa:**  
   Emisión de padrones, certificados y liquidaciones aduaneras en formato PDF corporativo de alta resolución compilados dinámicamente con **JasperReports**.

---

## 3. Tecnologías y Dependencias del Proyecto

El sistema adopta una arquitectura desacoplada y moderna, utilizando componentes probados de grado empresarial:

### 3.1 Backend: Java 21 & Spring Boot 4.1.x
Gestionado a través de **Apache Maven** (`backend/pom.xml`):

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

### 3.2 Frontend: Vue 3 & Vite 8
Gestionado a través de **Node.js / npm** (`frontend/package.json`):

| Dependencia | Versión | Propósito Técnico y Justificación |
| :--- | :---: | :--- |
| `vue` | ^3.5.42 | Framework progresivo reactivo utilizado con la Composition API y sintaxis concisa `<script setup>`. |
| `vite` | ^8.3.0 | Entorno de desarrollo ultrarrápido con Hot Module Replacement (HMR) y empaquetador de producción optimizado vía Rollup. |
| `@vitejs/plugin-vue` | ^6.0.8 | Plugin oficial de compilación de Single File Components (SFC `.vue`) para Vite. |
| `vue-router` | ^4.6.4 | Enrutador oficial SPA con soporte de navegación vigilada (*Navigation Guards*) por JWT y matriz de roles. |
| `pinia` | ^4.0.3 | Almacén de estado global reactivo y tipado (Manejo de sesión `authStore` y sincronización modular `navigationStore`). |
| `primevue` | ^4.5.5 | Suite integral de componentes de interfaz (Diálogos modales, tablas avanzadas, inputs accesibles y confirmaciones). |
| `@primevue/themes` | ^4.5.4 | Sistema de temas y estilos base integrados para PrimeVue 4. |
| `primeicons` | ^8.0.2 | Librería oficial de glifos vectoriales para señalética de aduanas, transporte, estados y acciones. |
| `axios` | ^1.20.0 | Cliente HTTP basado en promesas; configurado con interceptores para inyección de token Bearer y deslogueo en 401. |

---

### 3.3 Base de Datos y Contenedores
* **Motor:** MySQL 8.4 LTS InnoDB.
* **Juego de Caracteres:** `utf8mb4` con colación `utf8mb4_unicode_ci` para soporte internacional completo.
* **Aislamiento y Transaccionalidad:** Cumplimiento ACID completo con llaves foráneas `ON UPDATE CASCADE ON DELETE RESTRICT`.
* **Entorno de Despliegue:** Contenedor OCI (Docker / Podman) mapeado en el puerto local `3306:3306`.

---

## 4. Arquitectura de Software del Sistema

### 4.1 Arquitectura del Backend (Spring Boot Multicapa Orientada a Dominio)

El backend implementa un diseño multicapa desacoplado donde ninguna capa superior conoce detalles de implementación de la inferior, asegurando alta cohesión y bajo acoplamiento:

```
[ HTTP Requests: /api/v1/... ]
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
│          REPOSITORY LAYER             │  Spring Data JPA (JpaRepository, JpaSpecification)
│        (Acceso a Datos SQL)           │  Queries personalizadas, soft-delete y filtros activos
└──────────────────┬────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────┐
│            DATABASE LAYER             │  MySQL 8.4 InnoDB (16 tablas relacionales normalizadas)
└───────────────────────────────────────┘
```

#### Principios Clave del Backend:
1. **Desacoplamiento Estricto DTO / Entidad:**  
   Las entidades JPA nunca se exponen en los endpoints. La capa de controladores se comunica únicamente mediante `RequestDTO` (validados con `@Valid` y anotaciones Jakarta) y `ResponseDTO`.
2. **Envoltorio Estándar `ApiResponseDTO<T>`:**  
   Todas las respuestas JSON del sistema siguen una estructura predecible:
   ```json
   {
     "success": true,
     "message": "Operación ejecutada exitosamente",
     "data": { ... },
     "timestamp": "2026-10-21T09:00:00"
   }
   ```
3. **Seguridad Stateless JWT con SpEL:**  
   Autenticación mediante cabecera `Authorization: Bearer <token>`. La clase `SecurityRoles.java` centraliza las expresiones de autorización evaluadas por Spring Security antes de ejecutar cualquier método del controlador:
   ```java
   @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
   @PostMapping
   public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> crear(...)
   ```
4. **Manejo Centralizado de Excepciones:**  
   `GlobalExceptionHandler` intercepta fallos de validación (`MethodArgumentNotValidException`), recursos no encontrados (`ResourceNotFoundException`), violaciones de reglas de negocio (`BadRequestException`) y denegaciones de acceso (`AccessDeniedException`), retornando códigos HTTP semánticos (400, 401, 403, 404, 500) en formato JSON limpio.

---

### 4.2 Arquitectura del Frontend (Vue 3 Modular Basada en Roles)

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
1. **Shell Maestro y Navegación Dinámica:**  
   `AppLayout.vue` gobierna la estructura de la aplicación. `AppSidebar.vue` detecta automáticamente el rol del usuario conectado y despliega exclusivamente los módulos y sub-opciones autorizadas. Los roles con privilegios de supervisión global (`ADMINISTRADOR_TI` y `GERENTE_GENERAL`) disponen de un selector de contexto para alternar entre los 8 módulos del sistema.
2. **Sistema de Diseño *Warm Editorial*:**  
   Diseño editorial contemporáneo sin frameworks CSS pesados. Utiliza **Vanilla CSS** con variables personalizadas (`style.css`), rejilla de espaciados estricta de 4px (`--space-*`), tipografía dual (Inter para lectura, JetBrains Mono para códigos/montos) y soporte nativo para alternar entre **Modo Claro** y **Modo Oscuro**.
3. **Flujo de Peticiones y Manejo de Sesión:**  
   `apiClient.js` inyecta automáticamente el token JWT almacenado en `localStorage`. Si el backend responde con `401 Unauthorized`, el interceptor elimina los datos de sesión y redirige inmediatamente al usuario a `/login` sin recargar la página.

---

## 5. Matriz de los 8 Roles Institucionales

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
