# Documentación Técnica del Backend — Sistema de Gestión Aduanera - Transaduanas

**Sistema de Gestión Aduanera - Transaduanas (SGA Backend API)**  
*Documento Maestro de Arquitectura, Tecnologías, Patrones, Seguridad y Catálogo de Servicios REST*

---

## 📚 Estructura de la Documentación Modular del Backend

Para facilitar la lectura tanto a perfiles de negocio como a ingenieros de software, la documentación técnica del backend se encuentra desglosada en **6 capítulos especializados** dentro del directorio [`docs/backend/`](backend):

| Capítulo | Enlace | Contenido Principal |
| :--- | :--- | :--- |
| **01. Introducción y Arquitectura** | [`docs/backend/01_introduccion_y_arquitectura.md`](backend/01_introduccion_y_arquitectura.md) | Visión para no técnicos (analogías), principios rectores, arquitectura multicapa N-Tier, ciclo de vida de una petición HTTP, patrones de diseño (Builder, DTO, Wrapper, Global Handler) y árbol del código. |
| **02. Dependencias y Librerías** | [`docs/backend/02_dependencias_y_librerias.md`](backend/02_dependencias_y_librerias.md) | Inventario exhaustivo del `pom.xml`, versiones de Spring Boot 4.1.2, Java 21 LTS, JJWT 0.13.0, Lombok, Thymeleaf, OpenHTMLtoPDF y plugins del compilador. |
| **03. Seguridad, JWT y Roles** | [`docs/backend/03_seguridad_jwt_y_roles.md`](backend/03_seguridad_jwt_y_roles.md) | Modelo stateless JWT (HMAC-SHA256), cadena de filtros `SecurityConfig`, matriz de los 8 roles institucionales + Portal B2B (`ROLE_CLIENTE_PORTAL`), expresiones SpEL en `SecurityRoles` y resolución segura del usuario autenticado. |
| **04. Reglas de Negocio y Flujos** | [`docs/backend/04_reglas_de_negocio_y_flujos.md`](backend/04_reglas_de_negocio_y_flujos.md) | Cotizador de 4 pasos, fórmulas de flete y aranceles NANDINA, Compuerta Financiera Pre-Expediente (*Payment Gate*), pagos (`EstadoPagoCotizacion`) y deuda heredada, canales SUNAT (DAM), pesaje VGM, flota MTC (`TRK-YYYY-XXXX`), liquidación SPOT (12%) y Soft Delete. |
| **05. Catálogo de la API REST** | [`docs/backend/05_catalogo_completo_api_rest.md`](backend/05_catalogo_completo_api_rest.md) | Inventario completo de los **21 Controladores REST** con sus rutas, verbos HTTP (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`), seguridad requerida y descripción funcional de cada servicio. |
| **06. Reportes PDF y Archivos** | [`docs/backend/06_reportes_pdf_y_archivos.md`](backend/06_reportes_pdf_y_archivos.md) | Motor de reportería PDF (**Thymeleaf + OpenHTMLtoPDF 1.0.10**), almacenamiento seguro de archivos multipart (`LocalStorageServiceImpl`, 25 MB) y logging rotativo con `logback-spring.xml`. |

---

## Índice General del Documento Maestro

1. [Visión General del Sistema Backend](#1-visión-general-del-sistema-backend)
2. [Arquitectura de Software en Capas](#2-arquitectura-de-software-en-capas)
3. [Resumen del Stack Tecnológico y Dependencias](#3-resumen-del-stack-tecnológico-y-dependencias)
4. [Modelo de Seguridad y Roles Institucionales](#4-modelo-de-seguridad-y-roles-institucionales)
5. [Puntos Críticos y Reglas de Negocio Centrales](#5-puntos-críticos-y-reglas-de-negocio-centrales)
6. [Resumen del Catálogo de 21 Controladores REST](#6-resumen-del-catálogo-de-21-controladores-rest)
7. [Motor de Reportes PDF (Thymeleaf + OpenHTMLtoPDF)](#7-motor-de-reportes-pdf-thymeleaf--openhtmltopdf)

---

## 1. Visión General del Sistema Backend

El backend **sga-backend** es el motor central del **Sistema de Gestión Aduanera - Transaduanas**, una solución corporativa de alto rendimiento desarrollada en **Java 21 LTS** sobre **Spring Boot 4.1.2** para gobernar, automatizar y auditar integralmente las operaciones de agenciamiento aduanero, depósito temporal, transporte de carga pesada y liquidación financiera en el Perú, en estricta conformidad con las normativas de la **SUNAT** (Superintendencia Nacional de Aduanas y de Administración Tributaria) y el **MTC** (Ministerio de Transportes y Comunicaciones).

### ¿Qué hace en lenguaje sencillo?
- **Evita pérdidas financieras:** No permite iniciar ningún trámite aduanero costoso si el cliente no ha depositado el anticipo y este ha sido formalmente validado por el contador (*Compuerta Financiera*).
- **Controla el peso en báscula:** Exige el pesaje obligatorio en balanza certificada VGM según la enmienda internacional SOLAS antes de autorizar la salida de camiones.
- **Auditoría fiscal inmutable (*Soft Delete*):** Nunca borra datos físicos de la base de datos; preserva el historial completo para auditorías de SUNAT.
- **Conecta a los clientes en tiempo real:** Ofrece una API específica para el **Portal del Cliente B2B**, donde los importadores/exportadores pueden consultar el tracking de sus expedientes (canal Verde, Naranja, Rojo), ver sus cuotas pendientes y subir sus comprobantes de pago.

---

## 2. Arquitectura de Software en Capas

El backend adopta un diseño multicapa desacoplado (*N-Tier Architecture*) que aísla estrictamente la presentación HTTP, la lógica de negocio y el acceso a la base de datos relacional:

```
[ Petición HTTP / JSON ] ──▶ [ JwtAuthenticationFilter ] (Valida firma HMAC-SHA256)
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 1. CAPA DE CONTROLADORES (@RestController)                              │
│    - Validación declarativa de DTOs con @Valid (Jakarta Validation)    │
│    - Autorización por Roles mediante SpEL (@PreAuthorize)              │
│    - Envoltorio estándar: ApiResponseDTO<T> { success, message, data } │
└─────────────────────────────────────┬──────────────────────────────────┘
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. CAPA DE SERVICIOS (Interfaces @Service)                             │
│    - Reglas de negocio del dominio y compuertas financieras            │
│    - Control transaccional ACID (@Transactional)                       │
│    - Resolución dinámica del usuario autenticado (sin IDs fijos)       │
│    - Fórmulas de flete, tributos aduaneros, IGV y SPOT (12%)           │
└─────────────────────────────────────┬──────────────────────────────────┘
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. CAPA DE REPOSITORIOS (Spring Data JPA)                              │
│    - Consultas derivadas automáticas y consultas JPQL personalizadas   │
│    - Filtros automáticos de Soft Delete (estado_activo = true)         │
└─────────────────────────────────────┬──────────────────────────────────┘
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. BASE DE DATOS RELACIONAL (MySQL 8.4 LTS InnoDB)                     │
│    - 17 tablas normalizadas, llaves foráneas y codificación utf8mb4    │
└────────────────────────────────────────────────────────────────────────┘
```

👉 *Para conocer en detalle cada capa, diagramas de secuencia y patrones de diseño, consulta:*  
**[Capítulo 01: Introducción y Arquitectura (`docs/backend/01_introduccion_y_arquitectura.md`)](backend/01_introduccion_y_arquitectura.md)**.

---

## 3. Resumen del Stack Tecnológico y Dependencias

Todas las librerías son gestionadas a través de **Apache Maven** en [`backend/pom.xml`](../backend/pom.xml):

| Dependencia | Versión | Rol en el Sistema |
| :--- | :---: | :--- |
| **Java JDK** | **21 LTS** | Plataforma base con hilos virtuales, records y alto rendimiento. |
| **Spring Boot WebMVC** | **4.1.2** | Exposición de endpoints RESTful con servidor Tomcat y serialización Jackson. |
| **Spring Boot Data JPA** | **4.1.2** | Mapeo Objeto-Relacional con Hibernate ORM y transacciones `@Transactional`. |
| **Spring Boot Security** | **4.1.2** | Motor de seguridad con hashing BCrypt y evaluación de expresiones SpEL. |
| **Spring Boot Validation** | **4.1.2** | Validación declarativa de DTOs (`@NotNull`, `@NotBlank`, `@Positive`). |
| **MySQL Connector/J** | Runtime | Conector JDBC nativo para MySQL 8.4 InnoDB. |
| **JJWT (API, Impl, Jackson)** | **0.13.0** | Generación y verificación criptográfica de tokens Bearer HMAC-SHA256. |
| **Project Lombok** | **1.18.x** | Eliminación de boilerplate con anotaciones `@Builder`, `@Getter`, `@Setter`. |
| **Thymeleaf + OpenHTMLtoPDF** | **1.0.10** | Motor de plantillas HTML5/CSS3 para diseño y renderizado vectorial de reportes corporativos en PDF. |

👉 *Para conocer para qué sirve cada librería y qué pasaría si no estuviera, consulta:*  
**[Capítulo 02: Dependencias y Librerías (`docs/backend/02_dependencias_y_librerias.md`)](backend/02_dependencias_y_librerias.md)**.

---

## 4. Modelo de Seguridad y Roles Institucionales

El sistema implementa seguridad **Stateless** basada en tokens JWT con una vigencia de 24 horas y soporte para **8 roles institucionales internos** más el **acceso corporativo B2B**:

```
                               ┌─────────────────────────┐
                               │     ADMINISTRADOR TI    │
                               │  (Gobernanza Total TI)  │
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
                                            ▲
                                            │ Extranet
                                ┌───────────────────────┐
                                │      CLIENTE B2B      │
                                │  (RUC / Portal Web)   │
                                └───────────────────────┘
```

- **Expresiones SpEL Centralizadas:** La clase [`SecurityRoles.java`](../backend/src/main/java/pe/sga/config/SecurityRoles.java) concentra todas las reglas de autorización para `@PreAuthorize`.
- **Resolución Segura del Usuario:** En todas las operaciones de pago, validación y cambios de estado se obtiene la identidad del usuario directamente desde el `SecurityContextHolder`, eliminando IDs hardcodeados.

👉 *Para revisar la matriz de permisos y el ciclo de vida del token JWT, consulta:*  
**[Capítulo 03: Seguridad, JWT y Roles (`docs/backend/03_seguridad_jwt_y_roles.md`)](backend/03_seguridad_jwt_y_roles.md)**.

---

## 5. Puntos Críticos y Reglas de Negocio Centrales

1. **Compuerta Financiera Pre-Expediente (*Financial Payment Gate*):** Bloqueo en la creación de expedientes aduaneros si la cotización previa no se encuentra en estado `ADELANTO_APROBADO` / `PAGADO_TOTAL` ni cuenta con un abono `APROBADO` por el Contador.
2. **Deuda Heredada (`/api/v1/portal/pagos/obligaciones-pendientes`):** Cálculo consolidado de cuotas pendientes de cotizaciones y facturas de liquidación con saldo por pagar.
3. **Trámite DAM y Canales SUNAT:** Asignación de canal Verde (levante inmediato), Naranja (revisión documental) o Rojo (aforo físico) y aprovisionamiento automático de credenciales para clientes nuevos.
4. **Almacén y Báscula VGM:** Verificación de masa bruta según SOLAS y retención de carga ante discrepancias de peso.
5. **Transporte TMS y Brevete MTC:** Despacho (`TRK-YYYY-XXXX`) con choferes profesionales (A-IIIa/b/c) y unidades con CITV vigente.
6. **Liquidación y Detracción SPOT (12%):** Conciliación final de gastos reales y retención de detracción obligatoria para el Banco de la Nación.
7. **Tolerancia Cero a Borrado Físico (*Soft Delete*):** Mantenimiento histórico de registros mediante banderas lógicas para auditorías fiscales.

👉 *Para profundizar en las fórmulas matemáticas, cálculos de tributos y compuertas, consulta:*  
**[Capítulo 04: Reglas de Negocio y Flujos (`docs/backend/04_reglas_de_negocio_y_flujos.md`)](backend/04_reglas_de_negocio_y_flujos.md)**.

---

## 6. Resumen del Catálogo de 21 Controladores REST

El backend expone sus servicios organizados en 21 controladores RESTful:

| Controlador | Ruta Base | Funcionalidad Principal |
| :--- | :--- | :--- |
| [`AuthController`](../backend/src/main/java/pe/sga/controller/AuthController.java) | `/api/v1/auth` | Inicio de sesión (`/login`) y consulta del usuario autenticado (`/me`). |
| [`PortalController`](../backend/src/main/java/pe/sga/controller/PortalController.java) | `/api/v1/portal` | **Portal B2B:** Login/cambio de contraseña, perfil, cotizaciones, expedientes, pagos con voucher (`/pagos/...`) y notificaciones. |
| [`UsuarioClienteAdminController`](../backend/src/main/java/pe/sga/controller/UsuarioClienteAdminController.java) | `/api/v1/admin/usuarios-portal` | Gobernanza de cuentas B2B (creación, reseteo de claves, activación y reporte PDF). |
| [`AdminLogsController`](../backend/src/main/java/pe/sga/controller/AdminLogsController.java) | `/api/v1/admin/logs` | Consola de logs (`GET /`), descarga (`/download`) y limpieza de bitácora (`POST /clear`). |
| [`UsuarioController`](../backend/src/main/java/pe/sga/controller/UsuarioController.java) | `/api/v1/usuarios` | Gestión de cuentas y personal interno de los 8 roles institucionales, operadores y reporte PDF. |
| [`SedeController`](../backend/src/main/java/pe/sga/controller/SedeController.java) | `/api/v1/sedes` | Administración de sedes corporativas y terminales portuarios con reporte PDF (`/exportar-pdf`). |
| [`RolController`](../backend/src/main/java/pe/sga/controller/RolController.java) | `/api/v1/roles` | Consulta de solo lectura (`GET`) del catálogo maestro de los 8 roles institucionales. |
| [`CatalogoPuertoController`](../backend/src/main/java/pe/sga/controller/CatalogoPuertoController.java) | `/api/v1/catalogos/puertos` | Catálogo de puertos y aeropuertos nacionales y mundiales con reporte PDF (`/exportar-pdf`). |
| [`CatalogoTipoCargaController`](../backend/src/main/java/pe/sga/controller/CatalogoTipoCargaController.java) | `/api/v1/catalogos/tipos-carga` | Catálogo de tipologías de carga (FCL, LCL, Reefer, IMO) con reporte PDF (`/exportar-pdf`). |
| [`CatalogoMercanciaController`](../backend/src/main/java/pe/sga/controller/CatalogoMercanciaController.java) | `/api/v1/catalogos/mercancias` | Catálogo de subpartidas NANDINA, aranceles, permisos sectoriales y reporte PDF (`/exportar-pdf`). |
| [`CotizacionController`](../backend/src/main/java/pe/sga/controller/CotizacionController.java) | `/api/v1/cotizaciones` | Calculadora comercial de 4 pasos, asignación de asesor, revisión técnica y cambio de estado. |
| [`CotizacionPagoController`](../backend/src/main/java/pe/sga/controller/CotizacionPagoController.java) | `/api/v1` | Registro de anticipos (`/cotizaciones/{idCotizacion}/pagos`) y validación contable (`/finanzas/anticipos/...`) para liberar la compuerta financiera. |
| [`ExpedienteController`](../backend/src/main/java/pe/sga/controller/ExpedienteController.java) | `/api/v1/expedientes` | Apertura de expediente aduanero, numeración DAM, canal SUNAT, diligencia, levante y hitos de traslado. |
| [`ControlAlmacenController`](../backend/src/main/java/pe/sga/controller/ControlAlmacenController.java) | `/api/v1/almacen` | Recepción de carga en depósito temporal, balanza certificada VGM y autorización de salida. |
| [`DespachoTransporteController`](../backend/src/main/java/pe/sga/controller/DespachoTransporteController.java) | `/api/v1/transporte/despachos` | Asignación de flota pesada, tracking `TRK-YYYY-XXXX`, incidencias y confirmación de entrega. |
| [`ConductorController`](../backend/src/main/java/pe/sga/controller/ConductorController.java) | `/api/v1/transporte/conductores` | Padrón de choferes con brevete profesional MTC y control de vigencias. |
| [`UnidadTransporteController`](../backend/src/main/java/pe/sga/controller/UnidadTransporteController.java) | `/api/v1/transporte/unidades` | Padrón de tractos, camiones y semirremolques con CITV y póliza de seguro. |
| [`LiquidacionFinancieraController`](../backend/src/main/java/pe/sga/controller/LiquidacionFinancieraController.java) | `/api/v1/finanzas/liquidaciones` | Métricas, cálculo estimado, liquidación de tributos, gastos operativos, IGV y SPOT (12%). |
| [`ClienteController`](../backend/src/main/java/pe/sga/controller/ClienteController.java) | `/api/v1/clientes` | Padrón maestro de clientes importadores/exportadores, prospectos y conversión a cliente. |
| [`DocumentoController`](../backend/src/main/java/pe/sga/controller/DocumentoController.java) | `/api/v1/documentos` | Carga multipart (`/upload`), listado, previsualización (`/preview`) y descarga (`/descargar`) de archivos digitales. |
| [`HealthController`](../backend/src/main/java/pe/sga/controller/HealthController.java) | `/api/v1/health` | Verificación de salud y estado operativo (*liveness probe*). |

👉 *Para consultar los parámetros JSON, métodos HTTP y respuestas de cada endpoint, consulta:*  
**[Capítulo 05: Catálogo Completo de la API REST (`docs/backend/05_catalogo_completo_api_rest.md`)](backend/05_catalogo_completo_api_rest.md)**.

---

## 7. Motor de Reportes PDF (Thymeleaf + OpenHTMLtoPDF)

El backend genera documentos PDF corporativos en memoria mediante el motor unificado **Thymeleaf + OpenHTMLtoPDF (`1.0.10`)**, diseñando las vistas en HTML5/CSS3 con el sistema *Warm Editorial* (ubicadas en `backend/src/main/resources/templates/reports/`):

- [`ti-listado-base.html`](../backend/src/main/resources/templates/reports/ti-listado-base.html) (layout base TI)
- [`reporte-usuarios.html`](../backend/src/main/resources/templates/reports/reporte-usuarios.html)
- [`reporte-usuarios-portal.html`](../backend/src/main/resources/templates/reports/reporte-usuarios-portal.html)
- [`reporte-sedes.html`](../backend/src/main/resources/templates/reports/reporte-sedes.html)
- [`reporte-puertos.html`](../backend/src/main/resources/templates/reports/reporte-puertos.html)
- [`reporte-tipos-carga.html`](../backend/src/main/resources/templates/reports/reporte-tipos-carga.html)
- [`reporte-mercancias.html`](../backend/src/main/resources/templates/reports/reporte-mercancias.html)

👉 *Para revisar la implementación del motor de PDFs, la bóveda multipart y la bitácora rotativa, consulta:*  
**[Capítulo 06: Reportes PDF y Archivos (`docs/backend/06_reportes_pdf_y_archivos.md`)](backend/06_reportes_pdf_y_archivos.md)**.
