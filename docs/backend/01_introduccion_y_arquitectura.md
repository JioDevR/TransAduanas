# 1. Introducción y Arquitectura del Backend — SGA

**Sistema de Gestión Aduanera - Transaduanas**  
*Guía Integral de Arquitectura para Perfiles Técnicos y de Negocio*

---

## Índice del Capítulo
1. [¿Qué es el Backend y qué problema resuelve? (Explicación para no técnicos)](#1-qué-es-el-backend-y-qué-problema-resuelve-explicación-para-no-técnicos)
2. [Principios Rectores del Sistema](#2-principios-rectores-del-sistema)
3. [Arquitectura de Software en Capas (N-Tier Architecture)](#3-arquitectura-de-software-en-capas-n-tier-architecture)
4. [Flujo de una Petición de Extremo a Extremo](#4-flujo-de-una-petición-de-extremo-a-extremo)
5. [Patrones de Diseño Implementados](#5-patrones-de-diseño-implementados)
6. [Estructura Física del Código Fuente](#6-estructura-física-del-código-fuente)

---

## 1. ¿Qué es el Backend y qué problema resuelve? 

### El Problema Real que Resuelve en Aduanas y Transporte
En el comercio exterior peruano (importaciones y exportaciones), las empresas se enfrentan a múltiples riesgos:
1. **Pérdidas de dinero por trámites sin pago previo:** Es común que un operador comience a tramitar un despacho aduanero costoso sin que el cliente haya depositado el anticipo o los derechos arancelarios.
2. **Multas de la SUNAT y el MTC:** Errores en la Declaración Aduanera de Mercancías (DAM), discrepancias en el peso de contenedores según la enmienda internacional SOLAS (VGM), o asignación de choferes sin brevete profesional vigente ante el MTC acarrean multas elevadas y retención de carga.
3. **Pérdida de historial y auditoría fiscal:** En aduanas está prohibido por ley "borrar" documentos. Cualquier cambio debe dejar huella de quién lo hizo, a qué hora y por qué.

El backend del **Sistema de Gestión Aduanera - Transaduanas** es el cerebro que automatiza todas estas reglas:
- **No permite avanzar sin dinero validado:** Si el contador no valida el anticipo, el sistema bloquea automáticamente la creación del expediente ante SUNAT (*Compuerta Financiera*).
- **Controla el peso en báscula:** Exige registrar el ticket de balanza certificada VGM antes de dar salida a un camión.
- **Auditoría inmutable (*Soft Delete*):** Nunca elimina un registro de la base de datos; únicamente los marca como inactivos o anulados para que ante una fiscalización de SUNAT todo esté respaldado.
- **Portal de autoservicio para el cliente:** El importador no necesita llamar por teléfono; entra con su RUC a su portal, ve en qué canal salió su carga (Verde, Naranja o Rojo), descarga sus facturas y registra sus abonos bancarios.

---

## 2. Principios Rectores del Sistema

| Principio | Para qué sirve (Explicación Sencilla) | Detalle Técnico de Ingeniería |
| :--- | :--- | :--- |
| **API RESTful Stateless** | El servidor no guarda memoria de quién está conectado; cada petición lleva su propio "pase de entrada" seguro. | Arquitectura basada en tokens JWT Bearer (`Authorization: Bearer <token>`). No se emplean sesiones HTTP en servidor (`SessionCreationPolicy.STATELESS`), permitiendo escalabilidad horizontal sin compartir estado en memoria. |
| **Desacoplamiento DTO / Entidad** | Nunca mostramos la estructura íntima de la base de datos a internet; usamos "sobres cerrados" de información. | Las entidades JPA (`@Entity`) jamás se retornan directamente en los controladores. Se utilizan `RequestDTO` (validados con Jakarta Validation `@Valid`) y `ResponseDTO` mapeados explícitamente para evitar fuga de campos internos y vulnerabilidades de *Mass Assignment*. |
| **Envoltorio Unificado `ApiResponseDTO<T>`** | Todas las respuestas del sistema tienen siempre la misma forma predecible, facilitando el trabajo del frontend y aplicaciones móviles. | Todo endpoint retorna una estructura JSON estándar: `{ "success": boolean, "message": string, "data": T, "timestamp": string }`. |
| **Tolerancia Cero a Borrado Físico (*Soft Delete*)** | En aduanas no existe la tecla "eliminar". Todo queda registrado para SUNAT. | Ninguna entidad ejecuta `DELETE FROM ...`. Se aplican banderas lógicas (`estado_activo = false`) y cambios de estado de dominio (`ANULADO`, `CANCELADO`), garantizando la preservación histórica de auditoría fiscal. |
| **Compuertas Financieras Obligatorias** | Nadie trabaja gratis ni se asumen riesgos crediticios no autorizados. | Regla estricta a nivel de capa de servicio: un expediente operativo solo puede ser instanciado si la cotización comercial previa tiene un anticipo en estado `VALIDADO` por el área contable. |

---

## 3. Arquitectura de Software en Capas (N-Tier Architecture)

El backend sigue una arquitectura multicapa estricta orientada a dominio (*Domain-Driven Layered Architecture*), donde el flujo de dependencias siempre va de arriba hacia abajo:

```
                  ┌─────────────────────────────────────────┐
                  │          CLIENTE HTTP (Frontend)        │
                  │  (Vue 3 SGA Operativo / Portal Cliente) │
                  └────────────────────┬────────────────────┘
                                       │ Petición HTTP (JSON / Multipart)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           FILTROS DE SEGURIDAD                          │
│   JwtAuthenticationFilter ──▶ Valida firma criptográfica HMAC-SHA256    │
│   SecurityContextHolder   ──▶ Asigna rol, usuario autenticado e ID      │
└──────────────────────────────────────┬──────────────────────────────────┘
                                       │ Usuario validado
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      CAPA 1: CONTROLADORES (Controller)                 │
│  - 21 Controladores REST (@RestController)                              │
│  - Autorización por Roles mediante SpEL (@PreAuthorize)                 │
│  - Validación de contratos de entrada (@Valid, @NotNull, @Positive)     │
│  - Transforma Entidades a ResponseDTOs y responde con ApiResponseDTO    │
└──────────────────────────────────────┬──────────────────────────────────┘
                                       │ Invoca métodos de negocio (DTOs)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        CAPA 2: SERVICIOS (Service)                      │
│  - Interfaces (I...Service) e Implementaciones (...ServiceImpl)         │
│  - Reglas de Negocio, Compuertas Financieras y Cálculos (IGV, SPOT 12%) │
│  - Gestión de Transacciones ACID (@Transactional)                       │
│  - Resolución segura de usuario autenticado (sin IDs fijos)             │
│  - Generación de correlativos deterministas (COT-, EXP-, DSP-)          │
└──────────────────────────────────────┬──────────────────────────────────┘
                                       │ Invoca operaciones de persistencia
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       CAPA 3: REPOSITORIOS (Repository)                 │
│  - Interfaces Spring Data JPA (JpaRepository<Entity, ID>)               │
│  - Consultas derivadas automáticas (findByNumeroDocumento, existsBy...) │
│  - Consultas JPQL personalizadas con filtros de Soft Delete             │
└──────────────────────────────────────┬──────────────────────────────────┘
                                       │ Mapeo Objeto-Relacional (ORM Hibernate)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       CAPA 4: BASE DE DATOS (Database)                  │
│  - MySQL 8.4 LTS InnoDB (17 tablas normalizadas)                        │
│  - Integridad referencial con Llaves Foráneas (FK)                      │
│  - Codificación utf8mb4 / utf8mb4_unicode_ci y Zona Horaria -05:00     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Flujo de una Petición de Extremo a Extremo

Veamos qué ocurre exactamente cuando un **Agente Comercial** crea una nueva cotización:

1. **Llegada al Servidor:** El navegador envía un `POST /api/v1/cotizaciones` con el token JWT en la cabecera `Authorization: Bearer eyJhbG...` y un cuerpo JSON con los datos de la carga, ruta y cliente.
2. **Filtro de Seguridad (`JwtAuthenticationFilter`):** El filtro extrae el token, verifica que la firma digital sea válida y que no haya expirado. Si es legítimo, extrae el nombre de usuario (`comercial`), busca sus roles en la base de datos y coloca la credencial en el `SecurityContext`.
3. **Controlador (`CotizacionController`):**
   - `@PreAuthorize(SecurityRoles.PUEDE_CREAR_COTIZACION)`: Spring Security verifica que el rol `AGENTE_COMERCIAL` tenga permiso para ejecutar este método. Si fuera un transportista o chofer, respondería inmediatamente con `403 Forbidden`.
   - `@Valid CotizacionRequestDTO`: Spring valida que campos obligatorios como peso, puerto de origen, puerto de destino y RUC del cliente no vengan vacíos ni con números negativos.
4. **Capa de Servicio (`CotizacionServiceImpl`):**
   - Obtiene el usuario autenticado real desde el contexto de seguridad (garantizando auditoría precisa).
   - Genera el código correlativo oficial (ej. `COT-2026-00042`).
   - Aplica las fórmulas de cálculo de flete base, recargos y seguro.
   - Aplica el cálculo de tributos aduaneros estimados (Ad-Valórem según partida arancelaria, IGV 18%, IPM 2%).
   - Instancia la entidad `Cotizacion` en estado `BORRADOR` o `ENVIADA`.
5. **Capa de Persistencia (`CotizacionRepository`):**
   - Hibernate genera la sentencia SQL `INSERT INTO cotizacion (...) VALUES (...)` dentro de una transacción atómica `@Transactional`. Si la base de datos falla, se hace *rollback* automático y nada queda inconsistente.
6. **Respuesta al Cliente:** El servicio transforma la entidad persistida a un `CotizacionResponseDTO` y el controlador lo envuelve en un `ApiResponseDTO<CotizacionResponseDTO>`, retornando un código `HTTP 201 Created` al frontend con un mensaje legible: *"Cotización COT-2026-00042 registrada exitosamente"*.

---

## 5. Patrones de Diseño Implementados

El código del backend no es un conjunto de scripts desordenados; implementa patrones de diseño de nivel empresarial:

| Patrón | Nombre Técnico | ¿Cómo se aplica en este proyecto? |
| :--- | :--- | :--- |
| **Builder** | *Creational Pattern* (`@Builder` de Lombok) | Permite construir entidades y DTOs complejos (como `Cotizacion` o `Expediente` con más de 20 atributos) de forma inmutable, fluida y sin constructores gigantes con parámetros confusos. |
| **Data Transfer Object (DTO)** | *Architectural Pattern* | Separa los contratos de entrada (`RequestDTO`) y salida (`ResponseDTO`) del modelo de datos (`Entity`). Protege la base de datos de modificaciones maliciosas desde el cliente. |
| **Wrapper / Uniform Response** | *API Design Pattern* (`ApiResponseDTO<T>`) | Estandariza la respuesta de los 21 controladores para que el cliente web o móvil siempre reciba los mismos campos clave (`success`, `message`, `data`, `timestamp`). |
| **Controller-Service-Repository** | *Layered Pattern (N-Tier)* | Aísla la lógica HTTP (controlador), las reglas de negocio (servicio) y las consultas SQL (repositorio), facilitando las pruebas unitarias y el mantenimiento a largo plazo. |
| **Global Exception Handler** | *Interception / AOP Pattern* (`@ControllerAdvice`) | Centraliza la captura de todos los errores y excepciones (`ResourceNotFoundException`, `BadRequestException`, validaciones `@Valid`). Evita que aparezcan trazas feas de código Java (*Stack Traces*) en el navegador, entregando en su lugar mensajes limpios y semánticos con códigos HTTP exactos (400, 401, 403, 404, 500). |
| **Inyección de Dependencias** | *Dependency Injection (IoC)* | Se utiliza inyección por constructor en todos los controladores y servicios, lo que elimina el acoplamiento directo y garantiza que las clases sean 100% testeables con mocks. |

---

## 6. Estructura Física del Código Fuente

El código Java está organizado dentro del paquete base `pe.sga` en [`backend/src/main/java/pe/sga/`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/):

```
backend/src/main/java/pe/sga/
├── config/                                # Clases de configuración de infraestructura
│   ├── CorsConfig.java                    # Configuración de políticas de orígenes cruzados (CORS)
│   ├── DataInitializer.java              # Aprovisionamiento de usuario admin y cuentas demo
│   ├── JwtAuthenticationFilter.java      # Interceptor de cabeceras Bearer JWT
│   ├── JwtTokenProvider.java              # Firma HMAC-SHA256 y extracción de claims JWT
│   ├── SecurityConfig.java                # Cadena de filtros de Spring Security (FilterChain)
│   └── SecurityRoles.java                 # Expresiones SpEL centralizadas para @PreAuthorize
│
├── controller/                            # 21 Controladores REST que exponen la API
│   ├── AuthController.java                # Inicio de sesión (/api/v1/auth/login)
│   ├── PortalController.java              # Endpoints B2B para clientes (/api/v1/portal/...)
│   ├── UsuarioClienteAdminController.java # Administración de cuentas del Portal B2B
│   ├── AdminLogsController.java           # Lectura y streaming de logs del sistema
│   ├── CotizacionController.java          # Gestión comercial y cotizador de 4 pasos
│   ├── CotizacionPagoController.java      # Registro y validación contable de pagos
│   ├── ExpedienteController.java          # Trámite aduanero, DAM y asignación SUNAT
│   ├── ControlAlmacenController.java      # Depósito temporal y pesaje báscula VGM
│   ├── DespachoTransporteController.java  # Planificación de flota y tracking en ruta
│   ├── LiquidacionFinancieraController.java# Liquidación tributaria, gastos e IGV/SPOT
│   ├── DocumentoController.java           # Carga y descarga multipart de archivos
│   ├── ClienteController.java             # Padrón de empresas clientes y prospectos
│   ├── ConductorController.java           # Padrón de choferes con brevete MTC
│   ├── UnidadTransporteController.java    # Padrón de tractos, camiones y semirremolques
│   ├── SedeController.java                # Sedes corporativas, terminales y puertos
│   ├── CatalogoPuertoController.java      # Puertos y aeropuertos nacionales/mundiales
│   ├── CatalogoTipoCargaController.java   # Tipos de carga (FCL, LCL, Reefer, Peligrosa)
│   ├── CatalogoMercanciaController.java   # Subpartidas arancelarias NANDINA
│   ├── UsuarioController.java             # Gestión de personal de los 8 roles internos
│   ├── RolController.java                 # Catálogo de roles y permisos
│   └── HealthController.java              # Monitoreo de estado operativo (/api/v1/health)
│
├── dto/                                   # Objetos de Transferencia de Datos
│   ├── request/                           # Contratos de entrada con validaciones @Valid
│   ├── response/                          # Contratos de salida formateados para el cliente
│   ├── portal/                            # DTOs específicos del Portal de Clientes B2B
│   └── reporte/                           # DTOs para plantillas PDF (Jasper y Thymeleaf)
│
├── entity/                                # 17 Entidades persistentes JPA (Tablas MySQL)
│   └── enums/                             # Enumeraciones tipadas (RolUsuario, EstadoCotizacion...)
│
├── exception/                             # Manejo global y excepciones de dominio
│   ├── GlobalExceptionHandler.java        # Capturador central de excepciones (@ControllerAdvice)
│   ├── BadRequestException.java           # Errores de reglas de negocio (HTTP 400)
│   └── ResourceNotFoundException.java     # Recursos inexistentes (HTTP 404)
│
├── repository/                            # 17 Repositorios Spring Data JPA
│
└── service/                               # Capa de lógica de negocio
    ├── I...Service.java                   # Interfaces de contrato
    └── impl/...ServiceImpl.java           # Implementaciones transaccionales (@Service)
```
