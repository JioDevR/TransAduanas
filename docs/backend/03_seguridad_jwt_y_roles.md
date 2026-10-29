# 3. Seguridad, Control de Acceso (RBAC) y Tokens JWT

**Sistema de Gestión Aduanera - Transaduanas**  
*Modelo de Seguridad Empresarial, Autenticación Stateless y Matriz de Roles*

---

## Índice del Capítulo
1. [¿Cómo funciona la seguridad del sistema? (Explicación para no técnicos)](#1-cómo-funciona-la-seguridad-del-sistema-explicación-para-no-técnicos)
2. [Arquitectura de Autenticación Stateless (JWT)](#2-arquitectura-de-autenticación-stateless-jwt)
   - [2.1 Anatomía del Token JWT](#21-anatomía-del-token-jwt)
   - [2.2 Ciclo de Vida del Token: Del Login a la Petición](#22-ciclo-de-vida-del-token-del-login-a-la-petición)
3. [Componentes del Módulo de Seguridad](#3-componentes-del-módulo-de-seguridad)
   - [3.1 `SecurityConfig.java`: Cadena de Filtros](#31-securityconfigjava-cadena-de-filtros)
   - [3.2 `JwtAuthenticationFilter.java`: Intercepción de Cabeceras](#32-jwtauthenticationfilterjava-intercepción-de-cabeceras)
   - [3.3 `JwtTokenProvider.java`: Generación y Firma Criptográfica](#33-jwttokenproviderjava-generación-y-firma-criptográfica)
   - [3.4 `CorsConfig.java`: Política de Orígenes Permitidos](#34-corsconfigjava-política-de-orígenes-permitidos)
4. [Matriz de los 8 Roles Institucionales + Acceso B2B](#4-matriz-de-los-8-roles-institucionales--acceso-b2b)
5. [Centralización de Permisos con SpEL (`SecurityRoles.java`)](#5-centralización-de-permisos-con-spel-securityrolesjava)
6. [Resolución Segura del Usuario Autenticado en Servicios](#6-resolución-segura-del-usuario-autenticado-en-servicios)
7. [Protección de Contraseñas (Hashing BCrypt)](#7-protección-de-contraseñas-hashing-bcrypt)

---

## 1. ¿Cómo funciona la seguridad del sistema? (Explicación para no técnicos)

### La Analogía del Fotocheck Digital con Chip Criptográfico
Imagina un edificio corporativo de alta seguridad donde funcionan un banco, una agencia de aduanas y un depósito fiscal:
1. **La Puerta de Entrada (Login):** Llegas a recepción, muestras tu DNI o RUC y tu clave secreta. Si todo coincide, la recepción no te da una llave física de metal; te entrega un **fotocheck digital con chip**.
2. **El Fotocheck (Token JWT):** En el chip del fotocheck está grabado tu nombre, tu cargo exacto (por ejemplo, "Contador de la Sede Callao") y un sello digital inviolable que solo la máquina de recepción sabe firmar.
3. **Pase por los Molinetes (Filtros de Seguridad):** Cada vez que quieres entrar a una oficina (hacer clic en un botón del sistema), pasas el fotocheck por el molinete. El molinete lee el chip en una fracción de milisegundo:
   - Si eres **Agente Comercial**, el molinete te deja entrar a la oficina de cotizaciones, pero si intentas entrar a la bóveda de pagos o a la oficina de aduanas, la puerta se traba y suena una alarma roja (`HTTP 403 Forbidden`).
   - Si eres un **Cliente Importador**, tu fotocheck solo te permite entrar a la sala de espera virtual (Portal B2B) para ver tus propias cargas y registrar comprobantes de pago; no puedes ver la información de otras empresas.
4. **Vigencia Temporal:** El fotocheck tiene una validez de 24 horas. Si pasa ese tiempo, el chip caduca automáticamente y debes volver a identificarte en recepción.

---

## 2. Arquitectura de Autenticación Stateless (JWT)

A diferencia de las aplicaciones tradicionales que guardan la sesión del usuario en la memoria del servidor (lo que satura la memoria cuando hay miles de usuarios y causa problemas si hay varios servidores), este backend es **100% Stateless (Sin Estado)**:

```
[ Cliente Web / Navegador ]                     [ Backend SGA (Spring Boot) ]
           │                                                  │
           │ 1. POST /api/v1/auth/login {user, pass}          │
           ├─────────────────────────────────────────────────▶│
           │                                                  │ ── Verifica BCrypt
           │ 2. Retorna Token JWT Bearer                      │ ── Genera JWT (HMAC-SHA256)
           │◀─────────────────────────────────────────────────┤
           │                                                  │
           │ 3. GET /api/v1/expedientes                       │
           │    Header: Authorization: Bearer <token>         │
           ├─────────────────────────────────────────────────▶│
           │                                                  │ ── JwtFilter extrae token
           │                                                  │ ── Valida firma con secret
           │                                                  │ ── Inyecta en SecurityContext
           │                                                  │ ── @PreAuthorize valida rol
           │ 4. Respuesta JSON con Expedientes                │ ── Ejecuta consulta SQL
           │◀─────────────────────────────────────────────────┤
```

### 2.1 Anatomía del Token JWT
El token JWT es una cadena de texto dividida en tres partes separadas por puntos (`Header.Payload.Signature`):

1. **Encabezado (*Header*):** Especifica el tipo de token (`JWT`) y el algoritmo de cifrado empleado (`HS256` = HMAC-SHA256).
2. **Carga Útil (*Payload / Claims*):** Contiene los datos públicos del usuario autenticado:
   - `sub` (Subject): Nombre de usuario institucional (`admin`, `comercial`) o RUC del cliente B2B (`20370146994`).
   - `roles`: Arreglo de roles asignados (ej. `["ROLE_ADMINISTRADOR_TI"]`).
   - `idUsuario` / `idCliente`: Identificador primario en la base de datos para trazabilidad.
   - `iat` (Issued At): Momento exacto en que fue emitido.
   - `exp` (Expiration): Momento exacto en que expira (por defecto 86,400,000 ms = 24 horas).
3. **Firma Digital (*Signature*):** Es el resultado de combinar el Header + Payload con la clave secreta `jwt.secret` mediante el algoritmo HMAC-SHA256. Si un usuario malintencionado intenta alterar su rol de `CLIENTE` a `ADMINISTRADOR_TI` en el navegador, la firma ya no coincidirá y el backend rechazará la petición con `401 Unauthorized`.

---

## 3. Componentes del Módulo de Seguridad

Todos los componentes de seguridad se encuentran centralizados en [`backend/src/main/java/pe/sga/config/`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/config/):

### 3.1 `SecurityConfig.java`: Cadena de Filtros
Esta clase define las reglas de acceso perimetral mediante un `@Bean` de `SecurityFilterChain`:
- **Desactivación de CSRF:** Al ser una API REST stateless que consume tokens en cabeceras y no utiliza cookies de sesión, el ataque CSRF queda mitigado por diseño.
- **Política de Sesión:** `SessionCreationPolicy.STATELESS`. Spring no crea sesiones `HttpSession` en memoria.
- **Rutas Públicas Autorizadas (`permitAll`):**
  - `/api/v1/auth/**`: Endpoint de inicio de sesión (`/login`).
  - `/api/v1/health`: Monitor de salud del servidor para balanceadores de carga o Docker.
  - `/api/v1/portal/auth/**`: Acceso de autenticación específico para clientes B2B.
- **Rutas Protegidas:** Cualquier otra ruta requiere obligatoriamente un token JWT válido (`anyRequest().authenticated()`).

### 3.2 `JwtAuthenticationFilter.java`: Intercepción de Cabeceras
Se ejecuta una vez por cada petición HTTP entrante (`OncePerRequestFilter`):
1. Lee la cabecera `Authorization`.
2. Comprueba que empiece con el prefijo `Bearer `.
3. Extrae la cadena del token y se la entrega a `JwtTokenProvider`.
4. Si el token es válido, extrae el `username` y las autoridades del usuario.
5. Construye un objeto `UsernamePasswordAuthenticationToken` y lo almacena en el `SecurityContextHolder.getContext().setAuthentication(...)`.

### 3.3 `JwtTokenProvider.java`: Generación y Firma Criptográfica
- Utiliza la clave secreta inyectada desde `jwt.secret`.
- Genera tokens al iniciar sesión en `generateToken(Authentication auth)`.
- Valida la integridad del token en `validateToken(String token)`.
- Maneja excepciones de tokens expirados (`ExpiredJwtException`), firmas inválidas (`SignatureException`), tokens malformados (`MalformedJwtException`) o no soportados.

### 3.4 `CorsConfig.java`: Política de Orígenes Permitidos
Los navegadores web bloquean por seguridad peticiones de un puerto a otro (CORS). `CorsConfig` habilita de forma controlada la comunicación:
- **Orígenes permitidos:** `http://localhost:5180` (Frontend Interno), `http://localhost:5174` (Portal Cliente B2B), `http://localhost:5173` (Puerto alternativo Vite).
- **Métodos HTTP autorizados:** `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- **Cabeceras autorizadas:** `Authorization`, `Content-Type`, `X-Requested-With`, `Accept`.
- **Exposición de cabeceras:** Permite que el frontend lea `Content-Disposition` para saber el nombre de los PDFs descargados.

---

## 4. Matriz de los 8 Roles Institucionales + Acceso B2B

La enumeración `RolUsuario` ([`backend/src/main/java/pe/sga/entity/enums/RolUsuario.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/entity/enums/RolUsuario.java)) define estrictamente las 8 identidades operativas de la organización, sumadas al perfil externo de clientes:

| Rol Institucional | Nombre en Sistema | Alcance Operativo en el Backend |
| :--- | :--- | :--- |
| **Administrador TI** | `ROLE_ADMINISTRADOR_TI` | Acceso irrestricto. Gestión de usuarios internos, sedes, catálogos maestros, streaming de logs del sistema y gobernanza de cuentas B2B. |
| **Gerente General** | `ROLE_GERENTE_GENERAL` | Supervisión ejecutiva y toma de decisiones. Cuadros de mando, auditoría global de flota pesada, padrón de conductores y control de expedientes. |
| **Agente Comercial** | `ROLE_AGENTE_COMERCIAL` | Front-office comercial. Registro de clientes y prospectos, emisión de cotizaciones marítimas/aéreas y seguimiento comercial. |
| **Supervisor Aduanero** | `ROLE_SUPERVISOR_ADUANERO` | Control técnico aduanero. Auditoría de cotizaciones validadas, apertura formal de expedientes, registro de numeración DAM y canal SUNAT. |
| **Agente Aduanero** | `ROLE_AGENTE_ADUANERO` | Operación de campo en depósito temporal. Registro de ingreso de carga a almacén aduanero, pesaje certificado en báscula VGM y levante aduanero. |
| **Supervisor de Transporte** | `ROLE_SUPERVISOR_TRANSPORTE` | Operación logística terrestre (TMS). Asignación de tractos y choferes con brevete MTC, emisión de guías de remisión y tracking en ruta. |
| **Contador** | `ROLE_CONTADOR` | Custodia financiera y fiscal. Validación obligatoria de anticipos pre-expediente, control de detracciones SPOT (12%) y liquidaciones de gastos. |
| **Asesor Legal** | `ROLE_ASESOR_LEGAL` | Cumplimiento regulatorio. Fiscalización de permisos sectoriales obligatorios (DIGEMID, SENASA, SUCAMEC, PRODUCE) y auditoría contractual. |
| **Cliente B2B** *(Extranet)* | `ROLE_CLIENTE` | Importador o exportador externo. Consulta de estado de sus cargas, descarga de documentos oficiales y registro de pagos bancarios. |

---

## 5. Centralización de Permisos con SpEL (`SecurityRoles.java`)

Para evitar tener cadenas de texto mágicas y desordenadas repartidas por cientos de controladores, el sistema utiliza la clase [`SecurityRoles.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/config/SecurityRoles.java):

```java
public final class SecurityRoles {
    public static final String TI = "hasRole('ADMINISTRADOR_TI')";
    public static final String GERENCIA = "hasAnyRole('ADMINISTRADOR_TI', 'GERENTE_GENERAL')";
    public static final String COMERCIAL = "hasAnyRole('ADMINISTRADOR_TI', 'AGENTE_COMERCIAL')";
    public static final String ADUANAS = "hasAnyRole('ADMINISTRADOR_TI', 'SUPERVISOR_ADUANERO', 'AGENTE_ADUANERO')";
    public static final String FINANZAS = "hasAnyRole('ADMINISTRADOR_TI', 'CONTADOR')";
    public static final String TRANSPORTE = "hasAnyRole('ADMINISTRADOR_TI', 'SUPERVISOR_TRANSPORTE')";
    public static final String PORTAL_CLIENTE = "hasRole('CLIENTE')";
    // Expresiones compuestas de negocio
    public static final String PUEDE_CREAR_EXPEDIENTE = "hasAnyRole('ADMINISTRADOR_TI', 'SUPERVISOR_ADUANERO')";
    public static final String PUEDE_VALIDAR_PAGO = "hasAnyRole('ADMINISTRADOR_TI', 'CONTADOR')";
    ...
}
```

En los controladores, se protege cada método de forma limpia y legible:

```java
@PostMapping("/expedientes")
@PreAuthorize(SecurityRoles.PUEDE_CREAR_EXPEDIENTE)
public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> crearExpediente(@Valid @RequestBody ExpedienteRequestDTO dto) {
    ...
}
```

---

## 6. Resolución Segura del Usuario Autenticado en Servicios

> [!IMPORTANT]
> **Mejora Reciente de Seguridad:**  
> En versiones preliminares, algunas operaciones (como la subida de vouchers de pago o validación financiera) tenían identificadores fijos de prueba (`usuarioId = 1`). Esto ha sido completamente erradicado.

En la arquitectura actual, los servicios resuelven la identidad del usuario de manera dinámica y segura directamente desde el contexto de Spring Security:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String currentUsername = auth.getName();

Usuario usuarioActual = usuarioRepository.findByUsername(currentUsername)
    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado en la sesión"));
```

**Beneficios de esta implementación:**
1. **Auditoría Fidedigna:** Cada registro modificado almacena exactamente el ID del usuario real que presionó el botón en ese segundo, sin posibilidad de suplantación.
2. **Soporte Híbrido Interno / Portal B2B:** Cuando un pago es registrado desde el **Portal del Cliente B2B**, el sistema detecta que el usuario pertenece al rol `CLIENTE`, asocia el pago automáticamente a su empresa (`Cliente`) y deja el campo `id_usuario_registro` como `NULL`, marcando el origen como `PORTAL_WEB`. Cuando un **Contador** interno lo valida, se vincula el ID del contador responsable.

---

## 7. Protección de Contraseñas (Hashing BCrypt)

Ninguna contraseña se almacena jamás en texto plano en la base de datos `SGAdb`:
- Se utiliza el algoritmo **BCrypt** con factor de coste de trabajo (*work factor*) 10.
- BCrypt incorpora automáticamente una **sal aleatoria (*salt*)** de 16 bytes a cada hash, lo que significa que incluso si dos usuarios tienen la misma contraseña (por ejemplo, `Password123!`), sus textos cifrados en la columna `password_hash` serán completamente diferentes, impidiendo ataques basados en tablas arcoíris (*Rainbow Tables*).
- Las contraseñas cumplen políticas de robustez: mínimo 8 caracteres, al menos una mayúscula, un número y un carácter especial.
