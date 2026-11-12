# Archivos que controlan la seguridad

Las rutas de esta tabla parten de la raíz de TransAduanas. Los nombres abreviados del backend están bajo `backend/src/main/java/pe/sga/`.

| Archivo | Qué controla | Qué prueba lo comprueba |
| --- | --- | --- |
| `config/SecurityConfig.java` | Cadena de filtros, rutas públicas, exigencia de autenticación y respuestas 401/403. | SEC01 a SEC03. |
| `config/JwtTokenProvider.java` y `backend/src/main/resources/application.properties` | Firma, validación, vencimiento y clave de respaldo de JWT. | SEC02 y SEC04. La firma con la clave de respaldo permitió acceso. |
| `config/JwtAuthenticationFilter.java` | Carga usuarios y roles desde los repositorios. Comprueba el estado de la cuenta y, en el portal, de su empresa. | SIS05 a SIS07. |
| `config/SecurityRoles.java` y `@PreAuthorize` en los controladores | Permisos para cada rol y operación. | SEC03, FE05, SIS02 a SIS04. |
| `config/CorsConfig.java` | Orígenes, métodos y encabezados que acepta CORS. | SEC08. |
| `controller/AuthController.java`, `controller/PortalController.java` y `service/impl/PortalServiceImpl.java` | Inicio de sesión y acceso al perfil empresarial con la identidad autenticada. | SEC05, SEC07, SIS01 y SIS06. |
| `controller/UsuarioClienteAdminController.java` | Baja y reactivación de las cuentas del portal. | SIS04 a SIS08. |
| `controller/DocumentoController.java`, `service/impl/DocumentoServiceImpl.java` y `service/impl/LocalStorageServiceImpl.java` | Verificación del propietario y manejo del archivo en disco. | SEC10 y SIS08 cubren propiedad. El recorrido de rutas de disco queda fuera de esos dos casos. |
| `exception/GlobalExceptionHandler.java` | Traduce errores de la aplicación a respuestas HTTP. | SEC06 registró 500 ante cuerpos JSON inválidos. |
| `config/DataInitializer.java` | Aprovisiona la cuenta inicial y los datos demo. | SEC05. |
| `app-sga/src/core/router/index.js`, `app-sga/src/core/router/roles.routes.js`, `app-sga/src/core/api/apiClient.js` y `app-sga/src/features/auth/stores/authStore.js` | Guardias de navegación, roles visibles, almacenamiento de sesión y uso del JWT desde el navegador. | FE01 a FE08. El backend comprueba los permisos aunque se intente acceder directamente a la API. |
| `app-portal-cliente/src/core/router/index.js`, `app-portal-cliente/src/core/api/apiClient.js` y `app-portal-cliente/src/core/stores/authStore.js` | Navegación y sesión B2B. Usa claves de almacenamiento diferentes de las del SGA. | SIS01 a SIS08 cubren la API B2B. La UI del portal no formó parte de la prueba en navegador. |
| `db/schema.sql` y `db/docker-compose.yml` | El esquema define claves, relaciones y restricciones. Compose configura la instancia y sus credenciales locales. | BD01 a BD08 comprueban permisos concedidos a una cuenta del laboratorio. `seguridad-mysql.mjs` carga el esquema en una instancia separada. |

La prueba general llama al ejecutor HTTP, al navegador, al flujo del sistema y a MySQL. La colección de Postman se ejecuta por separado.
