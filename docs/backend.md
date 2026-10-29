# Documentación Técnica del Backend — Sistema de Gestión Aduanera - Transaduanas

**Sistema de Gestión Aduanera - Transaduanas**  
*Documento de Especificación de Arquitectura, Tecnologías, Patrones, Seguridad y Catálogo de Servicios REST*

---

## Índice General

1. [Visión General del Sistema](#1-visión-general-del-sistema)
2. [Arquitectura de Software](#2-arquitectura-de-software)
   - [2.1 Arquitectura en Capas (Layered Architecture)](#21-arquitectura-en-capas-layered-architecture)
   - [2.2 Diagrama de Flujo de Datos y Capas](#22-diagrama-de-flujo-de-datos-y-capas)
   - [2.3 Subsistemas Especializados](#23-subsistemas-especializados)
3. [Patrones de Diseño Implementados](#3-patrones-de-diseño-implementados)
4. [Tecnologías y Stack de Dependencias](#4-tecnologías-y-stack-de-dependencias)
   - [4.1 Matriz de Tecnologías Centrales](#41-matriz-de-tecnologías-centrales)
   - [4.2 Inventario de Dependencias (pom.xml)](#42-inventario-de-dependencias-pomxml)
5. [Estructura del Proyecto Backend](#5-estructura-del-proyecto-backend)
6. [Modelo de Seguridad y Control de Acceso (RBAC)](#6-modelo-de-seguridad-y-control-de-acceso-rbac)
   - [6.1 Mecanismo de Autenticación Stateless JWT](#61-mecanismo-de-autenticación-stateless-jwt)
   - [6.2 Matriz de los 8 Roles Institucionales](#62-matriz-de-los-8-roles-institucionales)
   - [6.3 Matriz de Autorización con SpEL (SecurityRoles)](#63-matriz-de-autorización-con-spel-securityroles)
7. [Módulos de Negocio y Reglas de Dominio](#7-módulos-de-negocio-y-reglas-de-dominio)
   - [7.1 Compuerta Financiera Pre-Expediente (Financial Payment Gate)](#71-compuerta-financiera-pre-expediente-financial-payment-gate)
   - [7.2 Política Estricta de Soft Delete (Tolerancia Cero a Borrado Físico)](#72-política-estricta-de-soft-delete-tolerancia-cero-a-borrado-físico)
   - [7.3 Generación Determinista de Códigos Correlativos](#73-generación-determinista-de-códigos-correlativos)
   - [7.4 Motor de Cálculo Financiero y Detracción SPOT](#74-motor-de-cálculo-financiero-y-detracción-spot)
8. [Catálogo de Endpoints RESTful (API Reference)](#8-catálogo-de-endpoints-restful-api-reference)
9. [Manejo Centralizado de Excepciones y Respuestas HTTP](#9-manejo-centralizado-de-excepciones-y-respuestas-http)
10. [Configuración, Perfiles y Puesta en Marcha](#10-configuración-perfiles-y-puesta-en-marcha)

---

## 1. Visión General del Sistema

El backend **sga-backend** es el motor central del **Sistema de Gestión Aduanera - Transaduanas**, una solución corporativa de alto rendimiento desarrollada para gobernar, automatizar y auditar integralmente las operaciones de agenciamiento aduanero, depósito temporal, transporte de carga pesada y liquidación financiera en el Perú, en estricta conformidad con las directivas de la **SUNAT** (Superintendencia Nacional de Aduanas y de Administración Tributaria) y el **MTC** (Ministerio de Transportes y Comunicaciones).

### Principios Fundamentales del Backend:
1. **API RESTful Stateless:** Toda comunicación cliente-servidor se realiza mediante protocolos HTTP seguros, utilizando tokens JSON Web Token (JWT) para mantener la sesión desacoplada del servidor de aplicaciones.
2. **Desacoplamiento Estricto DTO / Entidad:** Las entidades de base de datos (JPA Entities) jamás se exponen directamente a las capas externas, asegurando que la estructura interna de la base de datos no se filtre ni genere acoplamientos no deseados con clientes frontend o APIs externas.
3. **Auditoría y Trazabilidad Total:** Registro persistente de cambios de estado, fechas de modificación, usuarios responsables y un sistema de logging particionado diario.
4. **Compuertas de Negocio (Business Gates):** Imposibilidad de avanzar entre fases operativas (de Cotización a Expediente DAM, de Almacén a Despacho, de Levante a Liquidación) sin el cumplimiento estricto de condiciones operativas y financieras previas.

---

## 2. Arquitectura de Software

### 2.1 Arquitectura en Capas (Layered Architecture)

El backend está estructurado sobre el patrón arquitectónico clásico de **Capas Lógicas Multonivel (N-Tier Layered Architecture)** con orientación a dominios de negocio, garantizando alta cohesión y bajo acoplamiento:

1. **Capa de Configuración y Seguridad (`pe.sga.config`):**  
   Gestiona la infraestructura transversal del framework: ciclo de vida de beans, cadena de filtros de Spring Security, configuración CORS para clientes web, generador y validador de tokens criptográficos JWT y el inicializador de datos de arranque (*seed data*).
2. **Capa de Controladores / Presentación REST (`pe.sga.controller`):**  
   Expone los contratos de comunicación HTTP mediante anotaciones `@RestController`. Es responsable de:
   - Recibir las solicitudes HTTP e interpretar parámetros de ruta, cabeceras y cuerpos JSON.
   - Aplicar autorización declarativa a nivel de método con `@PreAuthorize`.
   - Activar la validación de contratos de entrada con `@Valid`.
   - Delegar inmediatamente la lógica a la capa de servicios y encapsular la respuesta en un envoltorio estandarizado `ApiResponseDTO<T>`.
3. **Capa de Servicios y Reglas de Negocio (`pe.sga.service` / `pe.sga.service.impl`):**  
   Implementa el núcleo de la lógica corporativa. Define contratos de interfaz (`IService`) y sus implementaciones concretas (`ServiceImpl`). Gestiona:
   - Límites transaccionales mediante `@Transactional` (con soporte para lecturas optimizadas `readOnly = true`).
   - Verificación de compuertas y transiciones de estado de negocio.
   - Mapeo bidireccional entre DTOs de transporte y Entidades JPA.
   - Generación de reportes PDF vectoriales y control de almacenamiento de archivos.
4. **Capa de Acceso a Datos / Repositorios (`pe.sga.repository`):**  
   Extiende `JpaRepository<T, ID>` de Spring Data JPA. Abstrae la persistencia SQL mediante métodos derivados de convención, consultas `JPQL` y filtrado por claves compuestas.
5. **Capa de Entidades y Modelo Relacional (`pe.sga.entity`):**  
   Representa las 17 tablas relacionales normalizadas de la base de datos MySQL InnoDB, mapeadas con anotaciones de JPA (`@Entity`, `@Table`, `@ManyToOne`, `@JoinColumn`, etc.).

---

### 2.2 Diagrama de Flujo de Datos y Capas

```
[ Petición HTTP entrante (SPA / Frontend) ]
                   │
                   ▼
       ┌────────────────────────┐
       │     Filtro CORS        │  (CorsConfig) Permite cabeceras, orígenes y métodos seguros
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │ JwtAuthenticationFilter│  Interpreta 'Authorization: Bearer <jwt>'
       └───────────┬────────────┘  Carga SecurityContextHolder con Usuario y Rol
                   │
                   ▼
       ┌────────────────────────┐
       │   Spring Security      │  Evalúa @PreAuthorize con SpEL (SecurityRoles)
       └───────────┬────────────┘  Rechaza no autorizados (401 / 403)
                   │
                   ▼
┌────────────────────────────────────────────────────────┐
│                   CONTROLLER LAYER                     │
│  - Endpoint @RestController                            │
│  - Validación de DTOs de entrada (@Valid)              │
│  - Respuesta uniforme ApiResponseDTO<T>                │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                    SERVICE LAYER                       │
│  - Interfaces (I...Service) e Implementaciones         │
│  - Transacciones (@Transactional)                      │
│  - Compuertas financieras y operativas                 │
│  - JasperReports / LocalStorageService                 │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                   REPOSITORY LAYER                     │
│  - Spring Data JPA (JpaRepository)                     │
│  - Filtros de estado activo (Soft Delete)              │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                    DATABASE LAYER                      │
│  - MySQL 8.4 InnoDB (ACID / Claves Foráneas)           │
└────────────────────────────────────────────────────────┘
```

---

### 2.3 Subsistemas Especializados

#### A. Almacenamiento Seguro de Documentos (`LocalStorageServiceImpl`)
- Provee almacenamiento desacoplado mediante la interfaz `IStorageService`.
- **Prevención de Ataques Path Traversal:** Limpia el nombre del archivo recibido (`StringUtils.cleanPath`) y rechaza secuencias maliciosas (`..`).
- **Nombres Criptográficos Únicos:** Cada archivo persistido en disco recibe un nombre UUID v4 preservando la extensión original en minúsculas, evitando colisiones y sobreescrituras en el sistema de archivos del servidor.
- **Acceso mediante Resource Streaming:** Los endpoints de descarga y vista previa (`/api/v1/documentos/{id}/descargar` y `/api/v1/documentos/{id}/preview`) transmiten los archivos mediante `UrlResource` con cabeceras `Content-Type` dinámicas (MIME) y nombres codificados en UTF-8 (`URLEncoder`).

#### B. Motor de Reportes PDF Híbrido (`ReporteJasperServiceImpl`)
El backend cuenta con una arquitectura de reportería dual optimizada:
1. **JasperReports 7.0.8 (Reporte de Personal y Usuarios):**
   - Generación dinámica de reportes vectoriales de alta fidelidad compilados directamente en memoria sin requerir archivos `.jrxml` en disco.
   - Diseña las plantillas en tiempo de ejecución utilizando la API fluida `JasperDesign` (`JRDesignBand`, `JRDesignTextField`, `JRDesignStaticText`).
   - Compilador JDT Eclipse integrado para evaluar expresiones Java (`UsuarioReporteItemDTO`).
   - **Patrón Singleton y Cache en Memoria:** Utiliza variable volátil con sincronización de doble verificación (*double-checked locking*) para compilar la plantilla una sola vez.
2. **Thymeleaf + OpenHTMLtoPDF (Reportes de Catálogos y Sedes):**
   - Renderizado moderno a partir de plantillas HTML5 semánticas estilizadas con tokens corporativos, convertidas a documentos PDF vectoriales mediante OpenHTMLtoPDF.
   - Aplica a: Sedes territoriales (`reports/reporte-sedes`), Puertos y terminales (`reports/reporte-puertos`), Tipos de carga (`reports/reporte-tipos-carga`) y Mercancías (`reports/reporte-mercancias`).
   - Todos los reportes retornan un flujo binario en memoria (`ByteArrayInputStream`), evitando el I/O en disco.

#### C. Subsistema de Logging y Auditoría (`logback-spring.xml` & `AdminLogsServiceImpl`)
- Configuración con rotación temporal y por tamaño (`SizeAndTimeBasedRollingPolicy`).
- **Partición en Dos Archivos Centrales:**
  - `sga-backend.log`: Registro general diario (límite 50MB por archivo, historial de 30 días, tope total 2GB).
  - `sga-backend-error.log`: Registro filtrado exclusivo de severidad `WARN` y `ERROR` (historial de 60 días, tope 1GB).
- La consola expone el servicio `IAdminLogsService` que permite a los usuarios con rol `ADMINISTRADOR_TI` inspeccionar las últimas N líneas en vivo, descargar el archivo de log o vaciarlo directamente desde la API.

---

## 3. Patrones de Diseño Implementados

| Categoría | Patrón | Implementación en el Proyecto | Beneficio Técnico |
| :--- | :--- | :--- | :--- |
| **Creacional** | **Builder Pattern** | Implementado con la anotación `@Builder` de Lombok en entidades JPA y DTOs (`ApiResponseDTO`, `Expediente`, `Cotizacion`, etc.). | Permite construir objetos complejos con múltiples parámetros de forma legible, inmutable y sin sobrecargar constructores. |
| **Creacional** | **Singleton** | Gestionado nativamente por el contenedor IoC de Spring Boot para todos los componentes anotados con `@Service`, `@Component`, `@Repository` y `@Configuration`. | Garantiza una única instancia eficiente por servicio durante el ciclo de vida de la aplicación. |
| **Estructural** | **Data Transfer Object (DTO)** | Paquete `pe.sga.dto.*` (Request, Response, Reporte). | Oculta la estructura interna de la base de datos, optimiza el payload de red y valida contratos de entrada sin contaminar entidades. |
| **Estructural** | **Adapter / Wrapper** | Clase genérica `ApiResponseDTO<T>`. | Normaliza el contrato de respuesta JSON hacia el frontend con campos predecibles (`success`, `message`, `data`, `timestamp`). |
| **Estructural** | **Filter / Pipeline** | `JwtAuthenticationFilter` que extiende `OncePerRequestFilter`. | Intercepta cada solicitud HTTP antes del despachador de Spring Security para validar la firma del token y poblar el contexto de seguridad. |
| **Comportamiento** | **Strategy Pattern** | Interfaces de servicio (`IExpedienteService`, `IStorageService`, `ICotizacionService`, etc.) desacopladas de sus implementaciones. | Facilita la intercambiabilidad de implementaciones (e.g., sustituir `LocalStorageServiceImpl` por `S3StorageServiceImpl` sin alterar clientes). |
| **Comportamiento** | **Template Method / Interceptor** | `GlobalExceptionHandler` con `@RestControllerAdvice` y métodos `@ExceptionHandler`. | Intercepta excepciones de cualquier controlador y las transforma en respuestas HTTP estructuradas con códigos de estado semánticos. |
| **Dominio / Persistencia** | **Repository Pattern** | Interfaces en `pe.sga.repository.*` que heredan de `JpaRepository`. | Encapsula el acceso y manipulación de datos, separando las reglas de negocio de las operaciones SQL de bajo nivel. |
| **Dominio** | **State Machine / Guarded Transitions** | Enums `EstadoCotizacion`, `EstadoExpediente`, `EstadoPagoCotizacion`. | Controla las transiciones válidas de estados de negocio (e.g., una cotización no pasa a expediente sin pago aprobado). |
| **Persistencia** | **Soft Delete Pattern** | Banderas booleanas (`estadoActivo = false`) o transiciones de estado a `ANULADO` / `INACTIVO`. | Garantiza el cumplimiento de normativas fiscales y auditorías SUNAT al evitar el borrado físico de registros históricos. |

---

## 4. Tecnologías y Stack de Dependencias

### 4.1 Matriz de Tecnologías Centrales

- **Lenguaje de Programación:** Java 21 (Long-Term Support - LTS).
- **Framework Principal:** Spring Boot 4.1.2 (Spring Framework 6.x / Jakarta EE 10).
- **Motor de Persistencia:** Spring Data JPA con Hibernate ORM.
- **Mecanismo de Seguridad:** Spring Security 6.x + JJWT (Java JWT) 0.13.0.
- **Motor de Base de Datos:** MySQL 8.4 LTS InnoDB.
- **Motor de Reportes Vectoriales:** JasperReports 7.0.8 (PDF Engine + Eclipse JDT Compiler) + Thymeleaf & OpenHTMLtoPDF.
- **Gestor de Compilación y Dependencias:** Apache Maven 3.9+.

---

### 4.2 Inventario de Dependencias (`pom.xml`)

A continuación se detalla cada dependencia configurada en el archivo `backend/pom.xml`, su versión y su justificación funcional:

```xml
<properties>
    <java.version>21</java.version>
    <jasperreports.version>7.0.8</jasperreports.version>
    <jjwt.version>0.13.0</jjwt.version>
</properties>
```

| Grupo / Artefacto | Versión | Ámbito (Scope) | Función y Justificación Técnica |
| :--- | :---: | :---: | :--- |
| `org.springframework.boot:spring-boot-starter-data-jpa` | 4.1.2 | Compile | Capa de abstracción de datos relacionales. Incluye Hibernate como proveedor JPA, HikariCP como pool de conexiones de alto rendimiento y soporte de transacciones `@Transactional`. |
| `org.springframework.boot:spring-boot-starter-security` | 4.1.2 | Compile | Motor de seguridad integral para control de acceso, protección de endpoints mediante SpEL, encoders de cifrado de contraseñas (BCrypt) y gestión de sesiones. |
| `org.springframework.boot:spring-boot-starter-validation` | 4.1.2 | Compile | Validador declarativo de modelos de datos mediante anotaciones Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Positive`, etc.). |
| `org.springframework.boot:spring-boot-starter-webmvc` | 4.1.2 | Compile | Servidor web incrustado (Apache Tomcat) y soporte completo para arquitectura MVC y APIs REST con serializador Jackson JSON. |
| `com.mysql:mysql-connector-j` | Runtime | Runtime | Controlador oficial JDBC para la comunicación y ejecución de sentencias sobre bases de datos MySQL 8.x InnoDB. |
| `org.projectlombok:lombok` | 1.18.x | Optional / Provided | Generador de código en tiempo de compilación para reducir código repetitivo (getters, setters, constructores, `toString`, `equals`, `@Builder`, `@Slf4j`). |
| `net.sf.jasperreports:jasperreports` | 7.0.8 | Compile | Biblioteca central de JasperReports para compilación de diseños, inyección de parámetros y llenado de fuentes de datos basadas en JavaBeans (`JRBeanCollectionDataSource`). |
| `net.sf.jasperreports:jasperreports-pdf` | 7.0.8 | Compile | Extensión especializada para renderizar y exportar documentos de JasperReports a formato Adobe PDF con soporte tipográfico y tablas vectoriales. |
| `net.sf.jasperreports:jasperreports-jdt` | 7.0.8 | Compile | Compilador JDT integrado basado en Eclipse para evaluar expresiones en tiempo de generación de reportes en entornos sin un JDK completo en tiempo de ejecución. |
| `io.jsonwebtoken:jjwt-api` | 0.13.0 | Compile | Interfaz y especificación de API estándar para la creación, firma y lectura de tokens JWT. |
| `io.jsonwebtoken:jjwt-impl` | 0.13.0 | Runtime | Implementación interna de JJWT para codificación y decodificación criptográfica HMAC-SHA256. |
| `io.jsonwebtoken:jjwt-jackson` | 0.13.0 | Runtime | Serializador y deserializador basado en Jackson para el manejo eficiente de payloads y claims dentro del token JWT. |

---

## 5. Estructura del Proyecto Backend

La estructura del código fuente está organizada bajo el paquete raíz `pe.sga`:

```
backend/
├── pom.xml                                      # Configuración Maven de dependencias y plugins
└── src/
    └── main/
        ├── java/
        │   └── pe/
        │       └── sga/
        │           ├── SgaApplication.java      # Clase principal con @SpringBootApplication y main()
        │           ├── config/                  # Clases de infraestructura y seguridad
        │           │   ├── CorsConfig.java               # Configuración de políticas CORS globales
        │           │   ├── DataInitializer.java          # Carga de datos base en arranque (CommandLineRunner)
        │           │   ├── JwtAuthenticationFilter.java  # Filtro OncePerRequestFilter para validar tokens JWT
        │           │   ├── JwtTokenProvider.java         # Generación, firma y parseo de tokens JWT (JJWT)
        │           │   ├── SecurityConfig.java           # SecurityFilterChain, filtros y políticas de acceso
        │           │   └── SecurityRoles.java            # Constantes de roles y expresiones SpEL (@PreAuthorize)
        │           ├── controller/              # Controladores REST expuestos en /api/v1/* (21 Controladores)
        │           │   ├── AdminLogsController.java           # Gestión e inspección en vivo de logs del sistema
        │           │   ├── AuthController.java                # Inicio de sesión (/login) y perfil (/me)
        │           │   ├── CatalogoMercanciaController.java   # Mantenimiento de mercancías y partidas
        │           │   ├── CatalogoPuertoController.java      # Mantenimiento de terminales portuarios
        │           │   ├── CatalogoTipoCargaController.java   # Mantenimiento de tipos de carga (FCL, LCL, etc.)
        │           │   ├── ClienteController.java             # Registro y gestión de importadores/exportadores
        │           │   ├── ConductorController.java           # Padrón de choferes con licencia MTC
        │           │   ├── ControlAlmacenController.java      # Custodia, pesaje balanza VGM y retiro
        │           │   ├── CotizacionController.java          # Cotizaciones comerciales de importación/exportación
        │           │   ├── CotizacionPagoController.java      # Registro y validación financiera de anticipos
        │           │   ├── DespachoTransporteController.java  # Despachos, asignación de tractos y tracking
        │           │   ├── DocumentoController.java           # Carga, descarga y preview de archivos adjuntos
        │           │   ├── ExpedienteController.java          # Expedientes aduaneros, DAM y levante SUNAT
        │           │   ├── HealthController.java              # Monitor de estado y salud del servicio REST
        │           │   ├── LiquidacionFinancieraController.java # Liquidaciones finales, gastos y métricas
        │           │   ├── PortalController.java              # API B2B del Portal del Cliente (/api/v1/portal/**)
        │           │   ├── RolController.java                 # Consulta y administración de roles
        │           │   ├── SedeController.java                # Sedes operativas territoriales y reporte PDF
        │           │   ├── UnidadTransporteController.java    # Flota de camiones y tractos aduaneros
        │           │   ├── UsuarioClienteAdminController.java # Gestión admin de usuarios del portal (/api/v1/admin/usuarios-portal/**)
        │           │   └── UsuarioController.java             # Cuentas de usuarios y reporte PDF de personal
        │           ├── dto/                     # Objetos de Transferencia de Datos
        │           │   ├── reporte/             # DTOs proyectados para JasperReports (UsuarioReporteItemDTO)
        │           │   ├── request/             # DTOs de entrada con anotaciones Jakarta Validation
        │           │   └── response/            # DTOs de respuesta JSON y ApiResponseDTO<T>
        │           ├── entity/                  # Entidades JPA (17 Tablas relacionales MySQL InnoDB)
        │           │   ├── CatalogoMercancia.java
        │           │   ├── CatalogoPuerto.java
        │           │   ├── CatalogoTipoCarga.java
        │           │   ├── Cliente.java
        │           │   ├── Conductor.java
        │           │   ├── ControlAlmacen.java
        │           │   ├── Cotizacion.java
        │           │   ├── CotizacionPago.java
        │           │   ├── DespachoTransporte.java
        │           │   ├── DocumentoAdjunto.java
        │           │   ├── Expediente.java
        │           │   ├── LiquidacionFinanciera.java
        │           │   ├── NotificacionCliente.java
        │           │   ├── Sede.java
        │           │   ├── UnidadTransporte.java
        │           │   ├── UsuarioCliente.java
        │           │   ├── Usuario.java
        │           │   └── enums/               # Enumeraciones de dominio (Canal, Estados, RolUsuario)
        │           ├── exception/               # Manejo de Excepciones de Negocio
        │           │   ├── BadRequestException.java       # Errores de validación de negocio (HTTP 400)
        │           │   ├── GlobalExceptionHandler.java    # Interceptor central @RestControllerAdvice
        │           │   └── ResourceNotFoundException.java # Recursos inexistentes (HTTP 404)
        │           ├── repository/              # Repositorios JPA que extienden JpaRepository
        │           └── service/                 # Interfaces de Servicios
        │               └── impl/                # Implementaciones de Servicios con @Transactional
        └── resources/
            ├── application.properties           # Configuración principal de la aplicación
            └── logback-spring.xml               # Configuración de appenders y rotación de logs
```

> [!NOTE]
> Para el arranque inicial del aplicativo, se requiere la clase de arranque estándar en `pe.sga.SgaApplication`:
> ```java
> package pe.sga;
> import org.springframework.boot.SpringApplication;
> import org.springframework.boot.autoconfigure.SpringBootApplication;
> 
> @SpringBootApplication
> public class SgaApplication {
>     public static void main(String[] args) {
>         SpringApplication.run(SgaApplication.class, args);
>     }
> }
> ```

---

## 6. Modelo de Seguridad y Control de Acceso (RBAC)

### 6.1 Mecanismo de Autenticación Stateless JWT

1. **Flujo de Login:** El cliente envía sus credenciales al endpoint `/api/v1/auth/login`.  
   `UsuarioServiceImpl` valida el usuario mediante `usuarioRepository.findByUsername(request.getUsername())` y verifica la contraseña utilizando `BCryptPasswordEncoder.matches()`. Si la cuenta se encuentra activa, delega en `JwtTokenProvider` la creación del token.
2. **Estructura y Claims del Token:**  
   El token se firma criptográficamente con algoritmo **HMAC-SHA256** utilizando una clave simétrica de al menos 256 bits (`jwt.secret`). Incluye en su payload los siguientes claims:
   - `sub`: Nombre de usuario (`username`).
   - `idUsuario`: Clave primaria del usuario autenticado.
   - `rol`: Código normativo del rol (e.g. `ADMINISTRADOR_TI`).
   - `correo`: Correo electrónico institucional.
   - `iat`: Timestamp de emisión (*Issued At*).
   - `exp`: Timestamp de expiración (*Expiration Date*, 24 horas por defecto).
3. **Filtro de Peticiones (`JwtAuthenticationFilter`):**  
   En cada solicitud entrante, extrae la cabecera `Authorization: Bearer <token>`, valida su integridad y vigencia, y registra en el `SecurityContextHolder` una instancia de `UsernamePasswordAuthenticationToken` con la autoridad `ROLE_<NOMBRE_ROL>`, permitiendo a Spring Security evaluar los accesos de forma inmediata.

---

### 6.2 Matriz de los 8 Roles Institucionales

El sistema implementa una segregación estricta de funciones dividida en ocho perfiles:

```
                                ┌─────────────────────────┐
                                │     ADMINISTRADOR TI    │
                                │   (Gobernanza Total)    │
                                └────────────┬────────────┘
                                             │
                                ┌────────────▼────────────┐
                                │     GERENTE GENERAL     │
                                │ (Cuadros de Mando/KPIs) │
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

1. **`ADMINISTRADOR_TI`:** Gobernanza de cuentas, sedes, catálogos maestros y visor en vivo de logs del sistema.
2. **`GERENTE_GENERAL`:** Supervisión ejecutiva, cuadros de mando, padrón de flota y choferes MTC.
3. **`AGENTE_COMERCIAL`:** Creación y gestión de clientes y formulación de cotizaciones aduaneras.
4. **`SUPERVISOR_ADUANERO`:** Auditoría de cotizaciones, apertura formal de expedientes y asignación de canal SUNAT.
5. **`AGENTE_ADUANERO`:** Verificación de pesaje VGM en almacén, recepción documental y trámite de levante aduanero.
6. **`SUPERVISOR_TRANSPORTE`:** Asignación de unidades de transporte y choferes con licencia MTC, despacho y tracking en ruta.
7. **`CONTADOR`:** Validación de anticipos pre-expediente, cálculo de detracciones SPOT y emisión de liquidaciones financieras.
8. **`ASESOR_LEGAL`:** Auditoría jurídica de expedientes, permisos sectoriales (DIGEMID, SENASA, SUCAMEC) y contratos.

---

### 6.3 Matriz de Autorización con SpEL (`SecurityRoles`)

Todas las restricciones de seguridad están centralizadas en la clase `SecurityRoles.java` mediante constantes reutilizables con Spring Expression Language (SpEL):

| Expresión en `SecurityRoles` | Roles Autorizados | Ámbito de Aplicación |
| :--- | :--- | :--- |
| `ACCESS_USUARIOS_READ` | `ADMINISTRADOR_TI`, `GERENTE_GENERAL` | Listado y consulta de cuentas de usuario. |
| `ACCESS_USUARIOS_WRITE` | `ADMINISTRADOR_TI` | Creación, actualización y desactivación de usuarios. |
| `ACCESS_ROLES_READ` | `ADMINISTRADOR_TI`, `GERENTE_GENERAL` | Lectura del catálogo de roles. |
| `ACCESS_LOGS_READ` | `ADMINISTRADOR_TI` | Consulta en vivo, descarga y vaciado de logs. |
| `ACCESS_SEDES_CATALOGOS_READ` | Usuarios autenticados (`isAuthenticated()`) | Consulta de sedes y catálogos (rutas GET públicas también disponibles). |
| `ACCESS_SEDES_CATALOGOS_WRITE`| `ADMINISTRADOR_TI`, `GERENTE_GENERAL` | Modificación de sedes y catálogos auxiliares. |
| `ACCESS_CLIENTE_COTIZACION_READ`| `ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `AGENTE_COMERCIAL`, `SUPERVISOR_ADUANERO`, `AGENTE_ADUANERO`, `CONTADOR`, `ASESOR_LEGAL` | Lectura de cartera de clientes y cotizaciones. |
| `ACCESS_CLIENTE_COTIZACION_WRITE`| `ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `AGENTE_COMERCIAL` | Registro y edición de clientes y cotizaciones. |
| `ACCESS_COTIZACION_APROBACION`| `ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `SUPERVISOR_ADUANERO`, `AGENTE_ADUANERO` | Revisión técnica y aprobación formal de cotizaciones. |
| `ACCESS_EXPEDIENTE_ALMACEN_READ`| `ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `SUPERVISOR_ADUANERO`, `AGENTE_ADUANERO`, `ASESOR_LEGAL`, `SUPERVISOR_TRANSPORTE`, `CONTADOR` | Lectura de expedientes DAM y control de almacén. |
| `ACCESS_EXPEDIENTE_ALMACEN_WRITE`| `ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `SUPERVISOR_ADUANERO`, `AGENTE_ADUANERO` | Apertura de expedientes, registro de canal SUNAT, levante y almacén. |
| `ACCESS_TRANSPORTE_READ` | `ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `SUPERVISOR_TRANSPORTE`, `SUPERVISOR_ADUANERO`, `AGENTE_ADUANERO` | Consulta de flota, choferes y estados de despacho. |
| `ACCESS_TRANSPORTE_WRITE` | `ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `SUPERVISOR_TRANSPORTE` | Programación de despacho, asignación de chofer/unidad, inicio de ruta y entrega. |
| `ACCESS_LIQUIDACION_READ` | `ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `CONTADOR`, `SUPERVISOR_ADUANERO`, `ASESOR_LEGAL` | Consulta de anticipos y liquidaciones financieras. |
| `ACCESS_LIQUIDACION_WRITE` | `ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `CONTADOR`, `SUPERVISOR_ADUANERO` | Validación de pagos, cálculo y emisión de liquidación final. |
| `ACCESS_DOCUMENTOS_READ/WRITE` | Usuarios autenticados (`isAuthenticated()`) | Carga y descarga de archivos de sustento. |

---

## 7. Módulos de Negocio y Reglas de Dominio

### 7.1 Compuerta Financiera Pre-Expediente (Financial Payment Gate)

Es la regla de negocio más crítica de la plataforma. Evita que la agencia incurra en costos operativos y aduaneros sin respaldo financiero previo del importador/exportador:

```
[ Cotización Comercial Registrada ]
                │
                ▼
   [ Revisión Técnica Aprobada ]
   (Estado: APROBADA)
                │
                ▼
┌────────────────────────────────────────────────────────┐
│             COMPUERTA FINANCIERA (PAYMENT GATE)        │
│                                                        │
│ ¿La cotización tiene un pago o anticipo con estado     │
│  APROBADO validado por el área contable/Finanzas?      │
└───────────────────────────┬────────────────────────────┘
                            │
               ┌────────────┴────────────┐
               │ NO                      │ SÍ
               ▼                         ▼
   [ RECHAZO DE OPERACIÓN ]   [ APERTURA DE EXPEDIENTE ]
   HTTP 400 Bad Request       Código: EXP-YYYYMM-XXXX
   "Se requiere un pago       Estado: ASIGNADO
    aprobado por Finanzas"
```

Implementación en `ExpedienteServiceImpl.java`:
```java
// Verificación obligatoria de compuerta financiera
boolean tienePagoAprobado = "ADELANTO_APROBADO".equals(cotizacion.getEstadoPago())
        || "PAGADO_TOTAL".equals(cotizacion.getEstadoPago())
        || cotizacionPagoRepository.existsByCotizacion_IdCotizacionAndEstadoPago(
                cotizacion.getIdCotizacion(), EstadoPagoCotizacion.APROBADO);

if (!tienePagoAprobado) {
    throw new BadRequestException("No es posible aperturar el expediente aduanero. Se requiere un pago o adelanto validado y aprobado por el área de Finanzas.");
}
```

---

### 7.2 Política Estricta de Soft Delete (Tolerancia Cero a Borrado Físico)

En conformidad con las auditorías de SUNAT, **ninguna entidad transaccional o maestra es eliminada físicamente de la base de datos**.

1. **Entidades Maestras (`Cliente`, `Usuario`, `Sede`, `Conductor`, `UnidadTransporte`, `Catalogo*`):**  
   Se desactivan mediante su bandera lógica:
   ```java
   entidad.setEstadoActivo(false); // o estado = "INACTIVO"
   ```
2. **Entidades Transaccionales (`Cotizacion`, `Expediente`, `DespachoTransporte`, `LiquidacionFinanciera`):**  
   Transicionan a su estado terminal de anulación:
   - `Cotizacion`: `estado = EstadoCotizacion.RECHAZADA` o `ANULADA`.
   - `Expediente`: `estadoExpediente = EstadoExpediente.ANULADO`.
   - `DespachoTransporte`: `estado = "CANCELADO"`, liberando simultáneamente el conductor y la unidad asignada.
   - `LiquidacionFinanciera`: `estadoPago = "ANULADO"`.

---

### 7.3 Generación Determinista de Códigos Correlativos

Los identificadores de negocio son correlativos estructurados generados en los servicios mediante el formato: `PREFIJO-YYYY-XXXX` (4 dígitos de año y 4 correlativos secuenciales):

- **Cotizaciones:** `COT-2026-0001`
- **Expedientes Aduaneros:** `EXP-2026-0001`
- **Despachos de Transporte (Tracking):** `TRK-2026-0001`
- **Liquidaciones Financieras:** `LIQ-2026-0001`
- **Comprobantes de Pago / Anticipos:** `PAG-COT-2026-0001`

---

### 7.4 Motor de Cálculo Financiero y Detracción SPOT

El servicio `LiquidacionFinancieraServiceImpl` incorpora la formulación contable estándar peruana:

1. **Base Imponible de Servicios:**  
   $$\text{Subtotal} = \text{Honorarios de Agenciamiento} + \text{Gastos de Almacenaje} + \text{Flete Local}$$
2. **Impuesto General a las Ventas (IGV 18%):**  
   $$\text{IGV} = \text{Subtotal} \times 0.18$$
3. **Total en Moneda Nacional (PEN):**  
   $$\text{Total PEN} = \text{Tributos Aduaneros (Ad-Valórem + IGV SUNAT)} + \text{Subtotal} + \text{IGV Servicios}$$
4. **Conversión Cambiaria a USD:**  
   $$\text{Total USD} = \frac{\text{Total PEN}}{\text{Tipo de Cambio Oficial (3.7500)}}$$
5. **Cálculo de Detracción SPOT (Sistema de Pago de Obligaciones Tributarias - 12%):**  
   Aplica sobre los servicios gravados con IGV cuando el monto total en moneda nacional iguala o supera el umbral legal de S/ 700.00 (`totalPen.compareTo(new BigDecimal("700.00")) >= 0`):
   $$\text{Monto Detracción} = (\text{Subtotal} + \text{IGV}) \times 0.12$$
   Si el total en moneda nacional es inferior a S/ 700.00 (`totalPen < 700.00`), el importe de detracción es S/ 0.00 y no se retiene SPOT.

---

## 8. Catálogo de Endpoints RESTful (API Reference)

Todos los endpoints están prefijados bajo la versión de API `/api/v1`.

### 8.1 Autenticación y Perfil (`/api/v1/auth`)

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Público | Autentica usuario y retorna token JWT Bearer. |
| `GET` | `/api/v1/auth/me` | Autenticado | Retorna los datos y rol del usuario autenticado. |

---

### 8.2 Catálogos Auxiliares (`/api/v1/catalogos/*`)

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/catalogos/puertos` | Público / `permitAll()` | Lista terminales portuarios (soporta filtro `tipo`). |
| `POST` | `/api/v1/catalogos/puertos` | `ACCESS_SEDES_CATALOGOS_WRITE` | Registra un nuevo puerto o terminal. |
| `GET` | `/api/v1/catalogos/tipos-carga` | Público / `permitAll()` | Lista tipos de carga (FCL, LCL, Granel, etc.). |
| `POST` | `/api/v1/catalogos/tipos-carga` | `ACCESS_SEDES_CATALOGOS_WRITE` | Registra un nuevo tipo de carga. |
| `GET` | `/api/v1/catalogos/mercancias` | Público / `permitAll()` | Lista catálogo de mercancías y subpartidas arancelarias. |
| `POST` | `/api/v1/catalogos/mercancias` | `ACCESS_SEDES_CATALOGOS_WRITE` | Registra una nueva mercancía y partida. |

---

### 8.3 Gestión de Clientes (`/api/v1/clientes`)

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/clientes` | `ACCESS_CLIENTE_COTIZACION_READ` | Lista clientes activos (soporta filtro `q` por RUC o Razón Social). |
| `GET` | `/api/v1/clientes/{id}` | `ACCESS_CLIENTE_COTIZACION_READ` | Obtiene un cliente por su ID primario. |
| `POST` | `/api/v1/clientes` | `ACCESS_CLIENTE_COTIZACION_WRITE` | Registra un nuevo cliente importador/exportador con RUC. |
| `PUT` | `/api/v1/clientes/{id}` | `ACCESS_CLIENTE_COTIZACION_WRITE` | Actualiza la información y contacto del cliente. |
| `DELETE` | `/api/v1/clientes/{id}` | `ACCESS_CLIENTE_COTIZACION_WRITE` | Desactiva lógicamente al cliente (`estadoActivo = false`). |

---

### 8.4 Gestión de Cotizaciones Comerciales (`/api/v1/cotizaciones`)

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/cotizaciones` | `ACCESS_CLIENTE_COTIZACION_READ` | Lista cotizaciones con filtros opcionales (`estado`, `idCliente`, `soloActivas`). |
| `GET` | `/api/v1/cotizaciones/{id}` | `ACCESS_CLIENTE_COTIZACION_READ` | Consulta detalle completo de cotización por ID. |
| `GET` | `/api/v1/cotizaciones/codigo/{codigo}` | `ACCESS_CLIENTE_COTIZACION_READ` | Consulta cotización por su código correlativo (`COT-YYYYMM-XXXX`). |
| `POST` | `/api/v1/cotizaciones` | `ACCESS_CLIENTE_COTIZACION_WRITE` | Registra una nueva cotización comercial. |
| `PUT` | `/api/v1/cotizaciones/{id}` | `ACCESS_CLIENTE_COTIZACION_WRITE` | Modifica cotización (restringido si ya está aprobada o facturada). |
| `PATCH` | `/api/v1/cotizaciones/{id}/revision` | `ACCESS_COTIZACION_APROBACION` | Registra la revisión técnica aduanera (Ad-Valórem y viabilidad). |
| `PATCH` | `/api/v1/cotizaciones/{id}/estado` | `ACCESS_COTIZACION_APROBACION` | Actualiza el estado (`APROBADA`, `OBSERVADA`, `RECHAZADA`). |
| `DELETE` | `/api/v1/cotizaciones/{id}` | `ACCESS_CLIENTE_COTIZACION_WRITE` | Anula la cotización (borrado lógico de estado). |

---

### 8.5 Anticipos y Pagos de Cotizaciones

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `POST` | `/api/v1/cotizaciones/{id}/pagos` | `ACCESS_CLIENTE_COTIZACION_WRITE` | Registra el voucher de anticipo y lo remite a validación contable. |
| `GET` | `/api/v1/cotizaciones/{id}/pagos` | `ACCESS_CLIENTE_COTIZACION_READ` | Lista el historial de abonos y anticipos de la cotización. |
| `GET` | `/api/v1/finanzas/anticipos` | `ACCESS_LIQUIDACION_READ` | Lista general de anticipos (filtro por estado: pendiente, aprobado). |
| `GET` | `/api/v1/finanzas/anticipos/pendientes` | `ACCESS_LIQUIDACION_READ` | Bandeja de anticipos pendientes de validación contable. |
| `PATCH` | `/api/v1/finanzas/anticipos/{id}/validar` | `ACCESS_LIQUIDACION_WRITE` | Valida y aprueba el abono financiero (desbloquea la apertura del expediente). |

---

### 8.6 Expedientes Aduaneros y SUNAT DAM (`/api/v1/expedientes`)

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/expedientes` | `ACCESS_EXPEDIENTE_ALMACEN_READ` | Lista expedientes con filtros (`canal`, `estado`, `idAgente`, `idCliente`). |
| `GET` | `/api/v1/expedientes/{id}` | `ACCESS_EXPEDIENTE_ALMACEN_READ` | Obtiene expediente aduanero por ID. |
| `GET` | `/api/v1/expedientes/codigo/{codigo}` | `ACCESS_EXPEDIENTE_ALMACEN_READ` | Consulta expediente por código (`EXP-YYYYMM-XXXX`). |
| `GET` | `/api/v1/expedientes/cotizacion/{id}` | `ACCESS_EXPEDIENTE_ALMACEN_READ` | Obtiene expediente vinculado a una cotización específica. |
| `POST` | `/api/v1/expedientes` | `ACCESS_EXPEDIENTE_ALMACEN_WRITE` | Apertura formal de expediente aduanero (aplica compuerta financiera). |
| `PUT` | `/api/v1/expedientes/{id}` | `ACCESS_EXPEDIENTE_ALMACEN_WRITE` | Actualiza datos de navegación, zarpe, contenedor y contacto en origen. |
| `PATCH` | `/api/v1/expedientes/{id}/canal` | `ACCESS_EXPEDIENTE_ALMACEN_WRITE` | Asigna número oficial de DAM y Canal SUNAT (`VERDE`, `NARANJA`, `ROJO`). |
| `PATCH` | `/api/v1/expedientes/{id}/levante` | `ACCESS_EXPEDIENTE_ALMACEN_WRITE` | Registra autorización de levante aduanero oficial. |
| `DELETE` | `/api/v1/expedientes/{id}` | `ACCESS_EXPEDIENTE_ALMACEN_WRITE` | Anula el expediente aduanero (borrado lógico). |

---

### 8.7 Depósito Temporal y Control de Almacén (`/api/v1/almacen`)

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/almacen` | `ACCESS_EXPEDIENTE_ALMACEN_READ` | Lista registros de custodia y almacenamiento. |
| `GET` | `/api/v1/almacen/expediente/{id}` | `ACCESS_EXPEDIENTE_ALMACEN_READ` | Consulta el estado de almacén de un expediente. |
| `POST` | `/api/v1/almacen` | `ACCESS_EXPEDIENTE_ALMACEN_WRITE` | Registra ingreso de carga a depósito temporal y pesaje VGM. |
| `PATCH` | `/api/v1/almacen/{id}/autorizar-salida` | `ACCESS_EXPEDIENTE_ALMACEN_WRITE` | Autoriza la salida de carga tras verificar levante aduanero. |

---

### 8.8 Transporte, Flota y Despachos (`/api/v1/transporte/*`)

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/transporte/conductores` | `ACCESS_TRANSPORTE_READ` | Lista padrón de choferes con licencia MTC y récord. |
| `POST` | `/api/v1/transporte/conductores` | `ACCESS_TRANSPORTE_WRITE` | Registra un nuevo conductor en el padrón de transporte. |
| `GET` | `/api/v1/transporte/unidades` | `ACCESS_TRANSPORTE_READ` | Lista flota de camiones, tractos y plataformas. |
| `POST` | `/api/v1/transporte/unidades` | `ACCESS_TRANSPORTE_WRITE` | Registra una nueva unidad de transporte con placa MTC. |
| `GET` | `/api/v1/transporte/despachos` | `ACCESS_TRANSPORTE_READ` | Lista órdenes de despacho terrestre y asignaciones. |
| `GET` | `/api/v1/transporte/despachos/tracking/{tracking}` | `ACCESS_TRANSPORTE_READ` | Consulta pública/privada de despacho por código de tracking. |
| `POST` | `/api/v1/transporte/despachos` | `ACCESS_TRANSPORTE_WRITE` | Programa un nuevo despacho de carga. |
| `PATCH` | `/api/v1/transporte/despachos/{id}/asignar-recursos` | `ACCESS_TRANSPORTE_WRITE` | Asigna chofer y tracto disponible al despacho. |
| `PATCH` | `/api/v1/transporte/despachos/{id}/iniciar-ruta` | `ACCESS_TRANSPORTE_WRITE` | Marca la salida del depósito temporal e inicio de ruta terrestre. |
| `PATCH` | `/api/v1/transporte/despachos/{id}/confirmar-entrega` | `ACCESS_TRANSPORTE_WRITE` | Registra entrega conforme y libera chofer y unidad. |
| `POST` | `/api/v1/transporte/despachos/{id}/incidencia` | Autenticado | Reporta incidencia en ruta con notificación inmediata. |

---

### 8.9 Liquidación Financiera (`/api/v1/finanzas/liquidaciones`)

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/finanzas/liquidaciones` | `ACCESS_LIQUIDACION_READ` | Lista liquidaciones financieras emitidas. |
| `GET` | `/api/v1/finanzas/liquidaciones/metricas` | `ACCESS_LIQUIDACION_READ` | Métricas consolidadas (monto liquidado, pendiente, recaudación). |
| `GET` | `/api/v1/finanzas/liquidaciones/calcular-estimado/{idExp}` | `ACCESS_LIQUIDACION_READ` | Calcula liquidación estimada previa (honorarios, almacenaje, IGV, detracción). |
| `POST` | `/api/v1/finanzas/liquidaciones` | `ACCESS_LIQUIDACION_WRITE` | Emite la liquidación financiera final de la operación. |
| `PATCH` | `/api/v1/finanzas/liquidaciones/{id}/pago` | `ACCESS_LIQUIDACION_WRITE` | Registra el pago final y cancela la liquidación financiera. |

---

### 8.10 Archivo Documental Digital (`/api/v1/documentos`)

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `POST` | `/api/v1/documentos/upload` | Autenticado | Sube uno o múltiples archivos (Multipart) asociados a una entidad. |
| `GET` | `/api/v1/documentos` | Autenticado | Lista documentos con filtros polimórficos (`entidadTipo`, `idCotizacion`, etc.). |
| `GET` | `/api/v1/documentos/{id}/descargar` | Autenticado | Descarga el archivo en su formato original (`attachment`). |
| `GET` | `/api/v1/documentos/{id}/preview` | Autenticado | Visualización directa en el navegador (`inline`). |
| `DELETE` | `/api/v1/documentos/{id}` | Autenticado | Elimina el registro del documento y su archivo físico. |

---

### 8.11 Administración TI, Infraestructura y Logs

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/sedes` | `ACCESS_SEDES_CATALOGOS_READ` | Lista todas las sedes operativas. |
| `GET` | `/api/v1/sedes/exportar-pdf` | `ACCESS_SEDES_CATALOGOS_READ` | Genera y descarga reporte en PDF oficial (Thymeleaf + OpenHTMLtoPDF). |
| `GET` | `/api/v1/usuarios` | `ACCESS_USUARIOS_READ` | Lista cuentas de usuario del sistema. |
| `GET` | `/api/v1/usuarios/exportar-pdf` | `ACCESS_USUARIOS_READ` | Exporta padrón oficial de personal en PDF con JasperReports 7.0.8. |
| `GET` | `/api/v1/roles` | `ACCESS_ROLES_READ` | Consulta catálogo de los 8 roles del sistema (`RolUsuario`). |
| `GET` | `/api/v1/admin/logs` | `ACCESS_LOGS_READ` | Inspección de las últimas N líneas del archivo de log. |
| `GET` | `/api/v1/admin/logs/download` | `ACCESS_LOGS_READ` | Descarga el archivo `sga-backend.log`. |
| `POST` | `/api/v1/admin/logs/clear` | `ACCESS_LOGS_READ` | Vacia el archivo de log en el servidor. |
| `GET` | `/api/v1/health` | Público | Monitor de estado del servicio (UP, versión, timestamp). |

---

### 8.12 Portal del Cliente B2B (`/api/v1/portal/**` — `PortalController.java`)

Controlador especializado para empresas importadoras/exportadoras y clientes corporativos con autenticación mediante `UsuarioCliente`:

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `POST` | `/api/v1/portal/auth/login` | Público | Autenticación B2B con credenciales (RUC/username) y entrega de JWT. |
| `POST` | `/api/v1/portal/auth/cambiar-password` | Autenticado Portal | Cambio obligatorio o voluntario de contraseña del cliente. |
| `GET` | `/api/v1/portal/perfil` | Autenticado Portal | Información corporativa y representante legal del cliente en sesión. |
| `GET` | `/api/v1/portal/cotizaciones` | Autenticado Portal | Bandeja de cotizaciones emitidas a nombre de la empresa. |
| `GET` | `/api/v1/portal/cotizaciones/{id}` | Autenticado Portal | Detalle completo de cotización (desglose arancelario y flete). |
| `POST` | `/api/v1/portal/cotizaciones/solicitar` | Autenticado Portal | Formulación y solicitud de nueva proforma aduanera. |
| `PATCH` | `/api/v1/portal/cotizaciones/{id}/aceptar` | Autenticado Portal | Aceptación formal de la propuesta comercial aprobada técnicamente. |
| `GET` | `/api/v1/portal/expedientes` | Autenticado Portal | Listado de expedientes aduaneros activos y concluidos. |
| `GET` | `/api/v1/portal/expedientes/{id}` | Autenticado Portal | Detalle y tracking integral del expediente (DAM, canal SUNAT, hitos). |
| `GET` | `/api/v1/portal/expedientes/{id}/documentos` | Autenticado Portal | Visualización y descarga de sustentos digitales asociados. |
| `GET` | `/api/v1/portal/pagos/resumen` | Autenticado Portal | Resumen financiero (saldo pendiente, total abonado, liquidaciones). |
| `GET` | `/api/v1/portal/pagos/obligaciones-pendientes` | Autenticado Portal | Obligaciones financieras listas para abonar (anticipos y balances). |
| `GET` | `/api/v1/portal/pagos/anticipos` | Autenticado Portal | Historial y estado de validación de anticipos registrados. |
| `GET` | `/api/v1/portal/pagos/liquidaciones` | Autenticado Portal | Historial de liquidaciones finales tributarias de la empresa. |
| `POST` | `/api/v1/portal/pagos/registrar-voucher` | Autenticado Portal | Registro formal de comprobante de pago bancario. |
| `POST` | `/api/v1/portal/pagos/upload-comprobante` | Autenticado Portal | Carga multipart del archivo digital del voucher bancario. |
| `PUT` | `/api/v1/portal/pagos/anticipos/{idPago}/corregir` | Autenticado Portal | Subsanación y recarga de comprobante observado por Finanzas. |
| `GET` | `/api/v1/portal/notificaciones` | Autenticado Portal | Bandeja de avisos y alertas operativas de expedientes y pagos. |
| `GET` | `/api/v1/portal/notificaciones/resumen` | Autenticado Portal | Conteo de avisos y notificaciones no leídas. |
| `PATCH` | `/api/v1/portal/notificaciones/{id}/leer` | Autenticado Portal | Marca una notificación específica como leída. |
| `PATCH` | `/api/v1/portal/notificaciones/marcar-todas-leidas` | Autenticado Portal | Marca todo el historial de notificaciones como leído. |

---

### 8.13 Administración de Usuarios del Portal B2B (`/api/v1/admin/usuarios-portal/**` — `UsuarioClienteAdminController.java`)

Control administrativo exclusivo para la gobernanza de accesos de clientes externos:

| Método | Endpoint | Permiso SpEL | Descripción |
| :---: | :--- | :--- | :--- |
| `GET` | `/api/v1/admin/usuarios-portal` | `ACCESS_USUARIOS_READ` | Directorio de usuarios de empresas clientes con búsqueda y filtros. |
| `GET` | `/api/v1/admin/usuarios-portal/{id}` | `ACCESS_USUARIOS_READ` | Detalle del usuario del portal y empresa vinculada. |
| `POST` | `/api/v1/admin/usuarios-portal` | `ACCESS_USUARIOS_WRITE` | Creación y provisión de credenciales de acceso para clientes. |
| `PUT` | `/api/v1/admin/usuarios-portal/{id}` | `ACCESS_USUARIOS_WRITE` | Actualización de datos, nombres y correo de notificación. |
| `DELETE` | `/api/v1/admin/usuarios-portal/{id}` | `ACCESS_USUARIOS_WRITE` | Desactivación lógica de la cuenta del portal (`estadoActivo = false`). |
| `PATCH` | `/api/v1/admin/usuarios-portal/{id}/reactivar` | `ACCESS_USUARIOS_WRITE` | Reactivación formal de la cuenta del cliente. |
| `PATCH` | `/api/v1/admin/usuarios-portal/{id}/reset-password` | `ACCESS_USUARIOS_WRITE` | Reseteo temporal y asignación de nueva contraseña de acceso. |

---

## 9. Manejo Centralizado de Excepciones y Respuestas HTTP

Todas las respuestas del sistema se estandarizan a través de `ApiResponseDTO<T>`:

```json
{
  "success": true,
  "message": "Operación ejecutada exitosamente",
  "data": { ... },
  "timestamp": "2026-10-21T16:00:00.000"
}
```

La clase `GlobalExceptionHandler` (`@RestControllerAdvice`) traduce las anomalías a respuestas con el código HTTP semántico correspondiente:

| Excepción Java | Código HTTP | Mensaje de Error Emitido |
| :--- | :---: | :--- |
| `ResourceNotFoundException` | `404 Not Found` | Mensaje descriptivo del recurso faltante con su ID o código. |
| `BadRequestException` | `400 Bad Request` | Violación de regla de negocio o compuerta operativa/financiera. |
| `BadCredentialsException` | `401 Unauthorized` | "Credenciales inválidas: usuario o contraseña incorrectos". |
| `AccessDeniedException` | `403 Forbidden` | "Acceso denegado: no cuenta con permisos suficientes para esta operación". |
| `MethodArgumentNotValidException` | `400 Bad Request` | Mapa clave-valor con cada campo que no superó la validación Jakarta. |
| `Exception` (Genérica no controlada) | `500 Internal Server Error` | "Error interno del servidor: [descripción técnica]". |

---

## 10. Configuración, Perfiles y Puesta en Marcha

### 10.1 Variables de Configuración (`application.properties`)

El backend soporta inyección por variables de entorno con valores por defecto seguros para desarrollo:

```properties
spring.application.name=sga-backend
server.port=${SERVER_PORT:8085}

# Conexión MySQL InnoDB
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/SGAdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Configuración Hibernate / JPA
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
spring.jpa.show-sql=${JPA_SHOW_SQL:true}
spring.jpa.properties.hibernate.format_sql=true

# Seguridad y Criptografía JWT
jwt.secret=${JWT_SECRET:sga_platform_secret_key_super_segura_2026_jwt_token_auth}
jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}

# Almacenamiento de Archivos y Documentos
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=25MB
spring.servlet.multipart.max-request-size=100MB
app.storage.upload-dir=${STORAGE_DIR:./uploads/documentos}
```

---

### 10.2 Inicializador de Datos de Arranque (`DataInitializer`)

Al arrancar la aplicación por primera vez, `DataInitializer` verifica la existencia de registros esenciales y auto-aprovisiona:

1. **Los 8 Roles Normativos:** Gobernados íntegramente a través del enum de dominio `pe.sga.entity.enums.RolUsuario` (sin tabla relacional `rol`), mapeando descripciones y cargos normativos.
2. **Sede Central Base:** Crea la sede inicial `SED-PRUEBA` (Callao, OPERATIVO).
3. **Catálogos Mínimos:** Registra terminal portuario (APM Terminals Callao) y tipos de carga marítima/aérea por defecto.
4. **Superusuario Administrador:**
   - **Username:** `admin`
   - **Password inicial:** `Password123!` (almacenado con hash BCrypt)
   - **Rol:** `ADMINISTRADOR_TI` (acceso y dashboard en `/administracion-ti/dashboard`)
5. **Cuentas Demo para el Portal del Cliente B2B:**
   - **Camposol S.A.:** RUC `20370146994` / Contraseña `Password123!`
   - **LG Electronics Peru S.A.:** RUC `20511894520` / Contraseña `Password123!`

---

### 10.3 Instrucciones de Compilación y Ejecución

#### Requisitos del Entorno:
- Java JDK 21 instalado (`java -version`).
- Apache Maven 3.9+ instalado (`mvn -version`).
- Instancia activa de MySQL 8.4 escuchando en el puerto `3306` con la base de datos `SGAdb`.

#### Comandos de Construcción y Ejecución:

```bash
# Navegar al directorio del backend
cd backend

# Compilar el proyecto y descargar dependencias
mvn clean compile

# Empaquetar el artefacto JAR omitiendo pruebas unitarias si aplica
mvn clean package -DskipTests

# Ejecutar el servidor Spring Boot en el puerto configurado (8085)
mvn spring-boot:run
```

Una vez iniciado el backend, el monitor de salud responderá en:  
`http://localhost:8085/api/v1/health`
