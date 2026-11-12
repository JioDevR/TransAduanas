# 5. Catálogo Completo de Endpoints RESTful (API Reference)

**Sistema de Gestión Aduanera - Transaduanas**  
*Inventario Exhaustivo de los 21 Controladores REST, Rutas, Métodos HTTP y Seguridad*

---

## Índice del Capítulo
1. [Convenciones Globales de la API](#1-convenciones-globales-de-la-api)
2. [Módulo 1: Autenticación y Sesión (`AuthController`)](#2-módulo-1-autenticación-y-sesión-authcontroller)
3. [Módulo 2: Portal del Cliente B2B (`PortalController`)](#3-módulo-2-portal-del-cliente-b2b-portalcontroller)
4. [Módulo 3: Administración TI y Gobernanza](#4-módulo-3-administración-ti-y-gobernanza)
   - [4.1 Usuarios del Portal B2B (`UsuarioClienteAdminController`)](#41-usuarios-del-portal-b2b-usuarioclienteadmincontroller)
   - [4.2 Monitoreo y Consola de Logs (`AdminLogsController`)](#42-monitoreo-y-consola-de-logs-adminlogscontroller)
   - [4.3 Gestión de Personal Interno (`UsuarioController`)](#43-gestión-de-personal-interno-usuariocontroller)
   - [4.4 Sedes y Sucursales (`SedeController`)](#44-sedes-y-sucursales-sedecontroller)
   - [4.5 Roles del Sistema (`RolController`)](#45-roles-del-sistema-rolcontroller)
5. [Módulo 4: Catálogos Maestros (Aduanas y Puertos)](#5-módulo-4-catálogos-maestros-aduanas-y-puertos)
   - [5.1 Puertos y Aeropuertos (`CatalogoPuertoController`)](#51-puertos-y-aeropuertos-catalogopuertocontroller)
   - [5.2 Tipos de Carga (`CatalogoTipoCargaController`)](#52-tipos-de-carga-catalogotipocargacontroller)
   - [5.3 Subpartidas Arancelarias NANDINA (`CatalogoMercanciaController`)](#53-subpartidas-arancelarias-nandina-catalogomercanciacontroller)
6. [Módulo 5: Comercial y Cotizaciones (`CotizacionController`)](#6-módulo-5-comercial-y-cotizaciones-cotizacioncontroller)
7. [Módulo 6: Finanzas y Control de Pagos (`CotizacionPagoController`)](#7-módulo-6-finanzas-y-control-de-pagos-cotizacionpagocontroller)
8. [Módulo 7: Trámite Aduanero y DAM (`ExpedienteController`)](#8-módulo-7-trámite-aduanero-y-dam-expedientecontroller)
9. [Módulo 8: Depósito Temporal y Balanza VGM (`ControlAlmacenController`)](#9-módulo-8-depósito-temporal-y-balanza-vgm-controlalmacencontroller)
10. [Módulo 9: Transporte, Flota y Choferes](#10-módulo-9-transporte-flota-y-choferes)
    - [10.1 Despachos en Ruta (`DespachoTransporteController`)](#101-despachos-en-ruta-despachotransportecontroller)
    - [10.2 Padrón de Conductores MTC (`ConductorController`)](#102-padrón-de-conductores-mtc-conductorcontroller)
    - [10.3 Padrón de Unidades de Transporte (`UnidadTransporteController`)](#103-padrón-de-unidades-de-transporte-unidadtransportecontroller)
11. [Módulo 10: Liquidación Financiera Final (`LiquidacionFinancieraController`)](#11-módulo-10-liquidación-financiera-final-liquidacionfinancieracontroller)
12. [Módulo 11: Padrón de Clientes (`ClienteController`)](#12-módulo-11-padrón-de-clientes-clientecontroller)
13. [Módulo 12: Archivo Documental y Bóveda (`DocumentoController`)](#13-módulo-12-archivo-documental-y-bóveda-documentocontroller)
14. [Módulo 13: Monitoreo y Salud del Servidor (`HealthController`)](#14-módulo-13-monitoreo-y-salud-del-servidor-healthcontroller)

---

## 1. Convenciones Globales de la API

- **Prefijo base común:** `/api/v1`
- **Formato de intercambio:** `application/json;charset=UTF-8` (o `multipart/form-data` para subida de vouchers y documentos, y `application/pdf` para exportación de reportes).
- **Cabecera de autenticación obligatoria:**  
  `Authorization: Bearer <token_jwt>` (excepto en rutas marcadas como públicas en [`SecurityConfig.java`](../../backend/src/main/java/pe/sga/config/SecurityConfig.java)).
- **Estructura unificada de respuesta (`ApiResponseDTO<T>`):**
  ```json
  {
    "success": true,
    "message": "Operación completada con éxito",
    "data": { ... },
    "timestamp": "2026-10-29T08:30:00"
  }
  ```

---

## 2. Módulo 1: Autenticación y Sesión (`AuthController`)

Controlador: [`AuthController.java`](../../backend/src/main/java/pe/sga/controller/AuthController.java)  
Ruta base: `/api/v1/auth`

| Método | Endpoint | Acceso / Rol | Descripción y Contrato |
| :---: | :--- | :---: | :--- |
| `POST` | `/login` | **Público** | Inicia sesión para usuarios internos del SGA. Valida usuario y contraseña cifrada con BCrypt. Retorna token JWT Bearer, rol, nombres y datos de sede. |
| `GET` | `/me` | `ACCESS_INTERNAL_STAFF` | Retorna el perfil del usuario interno actualmente autenticado a partir del `SecurityContext`. |

---

## 3. Módulo 2: Portal del Cliente B2B (`PortalController`)

Controlador: [`PortalController.java`](../../backend/src/main/java/pe/sga/controller/PortalController.java)  
Ruta base: `/api/v1/portal`  
Seguridad: `POST /auth/login` es **Público**; el resto de endpoints exige `@PreAuthorize("hasRole('CLIENTE_PORTAL')")`.

| Método | Endpoint | Descripción Funcional |
| :---: | :--- | :--- |
| `POST` | `/auth/login` | Inicio de sesión corporativo B2B por RUC/usuario. Retorna token JWT (`ROLE_CLIENTE_PORTAL`) y bandera `debeCambiarPassword`. |
| `POST` | `/auth/cambiar-password` | Cambio de contraseña (obligatorio en primer acceso con clave temporal). |
| `GET` | `/perfil` | Perfil de la empresa cliente vinculada al usuario B2B autenticado. |
| `GET` | `/cotizaciones` | Lista las cotizaciones asociadas a la empresa cliente. |
| `GET` | `/cotizaciones/{id}` | Detalle completo y desglose económico de una cotización del cliente. |
| `POST` | `/cotizaciones/solicitar` | Registra una nueva solicitud de cotización desde el Portal B2B. |
| `PATCH` | `/cotizaciones/{id}/aceptar` | Aceptación formal de la cotización por parte del cliente B2B. |
| `GET` | `/expedientes` | Lista todos los expedientes aduaneros asociados a la empresa cliente. |
| `GET` | `/expedientes/{id}` | Detalle completo de un expediente aduanero: hitos de tracking, DAM, canal SUNAT y transporte asignado. |
| `GET` | `/expedientes/{id}/documentos` | Lista los documentos adjuntos pertenecientes a un expediente del cliente. |
| `GET` | `/pagos/resumen` | Resumen financiero del cliente en USD (total facturado, pagado, saldo y conteos). |
| `GET` | `/pagos/obligaciones-pendientes` | **Endpoint de Deuda Heredada:** Consolidado de cotizaciones y liquidaciones con saldo pendiente de pago. |
| `GET` | `/pagos/anticipos` | Historial de anticipos y vouchers registrados por la empresa. |
| `GET` | `/pagos/liquidaciones` | Lista las liquidaciones financieras emitidas a nombre del cliente. |
| `POST` | `/pagos/registrar-voucher` | Registra un abono mediante payload JSON referenciando un comprobante previamente cargado. |
| `POST` | `/pagos/upload-comprobante` | Registra un abono con voucher adjunto (`multipart/form-data`), dejándolo en estado `PENDIENTE_REVISION`. |
| `PUT` | `/pagos/anticipos/{idPago}/corregir` | Permite subsanar un pago observado/rechazado o pendiente de revisión. |
| `GET` | `/notificaciones` | Lista las notificaciones operativas del cliente (canal SUNAT, levante, pagos, ruta). |
| `GET` | `/notificaciones/resumen` | Conteo de notificaciones no leídas y resumen para el indicador de la barra superior. |
| `PATCH` | `/notificaciones/{id}/leer` | Marca una notificación específica como leída. |
| `PATCH` | `/notificaciones/marcar-todas-leidas` | Marca todas las notificaciones del cliente como leídas. |

---

## 4. Módulo 3: Administración TI y Gobernanza

### 4.1 Usuarios del Portal B2B (`UsuarioClienteAdminController`)
Controlador: [`UsuarioClienteAdminController.java`](../../backend/src/main/java/pe/sga/controller/UsuarioClienteAdminController.java)  
Ruta base: `/api/v1/admin/usuarios-portal`  
Seguridad: Lectura y exportación PDF con `ACCESS_USUARIOS_READ` (`ADMINISTRADOR_TI`, `GERENTE_GENERAL`); escritura con `ACCESS_USUARIOS_WRITE` (`ADMINISTRADOR_TI`).

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista las cuentas de acceso al Portal B2B vinculadas a clientes (admite filtro `soloActivos`). |
| `GET` | `/{id}` | Obtiene el detalle de una cuenta de acceso al Portal B2B. |
| `POST` | `/` | Crea una cuenta de acceso al Portal B2B para una empresa cliente. |
| `PUT` | `/{id}` | Actualiza los datos de la cuenta del Portal B2B. |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva la cuenta B2B. |
| `PATCH` | `/{id}/reactivar` | Reactiva una cuenta B2B previamente desactivada. |
| `PATCH` | `/{id}/reset-password` | Restablece la contraseña y activa la bandera de cambio obligatorio en el siguiente acceso. |
| `GET` | `/exportar-pdf` | Genera el reporte corporativo de usuarios del Portal B2B en formato PDF (Thymeleaf + OpenHTMLtoPDF). |

### 4.2 Monitoreo y Consola de Logs (`AdminLogsController`)
Controlador: [`AdminLogsController.java`](../../backend/src/main/java/pe/sga/controller/AdminLogsController.java)  
Ruta base: `/api/v1/admin/logs`  
Seguridad: `ACCESS_LOGS_READ` (`ADMINISTRADOR_TI`).

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Retorna las últimas N líneas del archivo de bitácora del servidor (`sga-backend.log`). |
| `GET` | `/download` | Descarga el archivo de log para auditorías técnicas. |
| `POST` | `/clear` | Limpia el archivo de bitácora activo en el servidor. |

### 4.3 Gestión de Personal Interno (`UsuarioController`)
Controlador: [`UsuarioController.java`](../../backend/src/main/java/pe/sga/controller/UsuarioController.java)  
Ruta base: `/api/v1/usuarios`  
Seguridad: Lectura y exportación PDF con `ACCESS_USUARIOS_READ`; `/operadores` con `ACCESS_OPERADORES_READ`; escritura con `ACCESS_USUARIOS_WRITE` (`ADMINISTRADOR_TI`).

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista el personal interno de la organización (admite filtro `soloActivos`). |
| `GET` | `/operadores` | Lista operadores internos activos para asignación de cotizaciones/expedientes. |
| `GET` | `/{id}` | Obtiene el detalle de un colaborador por ID. |
| `POST` | `/` | Registra un nuevo colaborador con su rol, sede, correo institucional y credenciales. |
| `PUT` | `/{id}` | Actualiza datos del personal (sede asignada, teléfono, cargo, rol). |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva la cuenta del colaborador (`estado_activo = false`). |
| `PATCH` | `/{id}/reactivar` | Reactiva una cuenta de colaborador desactivada. |
| `GET` | `/exportar-pdf` | Genera el padrón oficial de usuarios internos en formato PDF (Thymeleaf + OpenHTMLtoPDF). |

### 4.4 Sedes y Sucursales (`SedeController`)
Controlador: [`SedeController.java`](../../backend/src/main/java/pe/sga/controller/SedeController.java)  
Ruta base: `/api/v1/sedes`  
Seguridad: Lectura y exportación PDF con `ACCESS_SEDES_CATALOGOS_READ` (`ACCESS_INTERNAL_STAFF`); escritura con `ACCESS_SEDES_CATALOGOS_WRITE` (`ADMINISTRADOR_TI`, `GERENTE_GENERAL`).

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista las sedes territoriales y terminales portuarios (admite filtro `soloActivas`). |
| `GET` | `/{id}` | Obtiene el detalle de una sede por ID. |
| `POST` | `/` | Registra una nueva sede o terminal portuario. |
| `PUT` | `/{id}` | Actualiza los datos de una sede existente. |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva lógicamente una sede. |
| `PATCH` | `/{id}/reactivar` | Reactiva una sede previamente desactivada. |
| `GET` | `/exportar-pdf` | Genera el padrón oficial de sedes en formato PDF mediante Thymeleaf y OpenHTMLtoPDF. |

### 4.5 Roles del Sistema (`RolController`)
Controlador: [`RolController.java`](../../backend/src/main/java/pe/sga/controller/RolController.java)  
Ruta base: `/api/v1/roles`  
Seguridad: `ACCESS_ROLES_READ` (`ADMINISTRADOR_TI`, `GERENTE_GENERAL`). Controlador de **solo lectura (`GET`)**.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Retorna el catálogo maestro de los 8 roles institucionales del sistema. |
| `GET` | `/{id}` | Retorna el detalle de un rol institucional por ID. |

---

## 5. Módulo 4: Catálogos Maestros (Aduanas y Puertos)

### 5.1 Puertos y Aeropuertos (`CatalogoPuertoController`)
Controlador: [`CatalogoPuertoController.java`](../../backend/src/main/java/pe/sga/controller/CatalogoPuertoController.java)  
Ruta base: `/api/v1/catalogos/puertos`  
Seguridad: `GET` públicos de catálogo en `SecurityConfig`; `/exportar-pdf` con `ACCESS_SEDES_CATALOGOS_READ`; escritura con `ACCESS_SEDES_CATALOGOS_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista los puertos y aeropuertos marítimos/aéreos registrados. |
| `GET` | `/{id}` | Obtiene el detalle de un puerto o terminal por ID. |
| `POST` | `/` | Registra un nuevo puerto o terminal logístico. |
| `PUT` | `/{id}` | Actualiza un puerto existente. |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva un puerto del catálogo. |
| `PATCH` | `/{id}/reactivar` | Reactiva un puerto desactivado. |
| `GET` | `/exportar-pdf` | Genera el catálogo maestro de puertos en PDF con Thymeleaf y OpenHTMLtoPDF. |

### 5.2 Tipos de Carga (`CatalogoTipoCargaController`)
Controlador: [`CatalogoTipoCargaController.java`](../../backend/src/main/java/pe/sga/controller/CatalogoTipoCargaController.java)  
Ruta base: `/api/v1/catalogos/tipos-carga`  
Seguridad: `GET` públicos de catálogo en `SecurityConfig`; `/exportar-pdf` con `ACCESS_SEDES_CATALOGOS_READ`; escritura con `ACCESS_SEDES_CATALOGOS_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista las tipologías de carga (FCL, LCL, Reefer, IMO, Proyecto, etc.). |
| `GET` | `/{id}` | Obtiene el detalle de un tipo de carga por ID. |
| `POST` | `/` | Registra un nuevo tipo de carga. |
| `PUT` | `/{id}` | Actualiza un tipo de carga existente. |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva un tipo de carga. |
| `PATCH` | `/{id}/reactivar` | Reactiva un tipo de carga desactivado. |
| `GET` | `/exportar-pdf` | Genera el reporte corporativo de tipos de carga en PDF. |

### 5.3 Subpartidas Arancelarias NANDINA (`CatalogoMercanciaController`)
Controlador: [`CatalogoMercanciaController.java`](../../backend/src/main/java/pe/sga/controller/CatalogoMercanciaController.java)  
Ruta base: `/api/v1/catalogos/mercancias`  
Seguridad: `GET` públicos de catálogo en `SecurityConfig`; `/exportar-pdf` con `ACCESS_SEDES_CATALOGOS_READ`; escritura con `ACCESS_SEDES_CATALOGOS_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista las subpartidas NANDINA con arancel base y entidad reguladora. |
| `GET` | `/{id}` | Obtiene el detalle de una mercancía arancelaria por ID. |
| `GET` | `/codigo/{codigo}` | Búsqueda exacta por código arancelario NANDINA (ej. `8517.13.00.00`). |
| `POST` | `/` | Registra una nueva subpartida arancelaria NANDINA. |
| `PUT` | `/{id}` | Actualiza una subpartida arancelaria. |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva una subpartida arancelaria. |
| `PATCH` | `/{id}/reactivar` | Reactiva una subpartida desactivada. |
| `GET` | `/exportar-pdf` | Genera el padrón arancelario oficial en PDF. |

---

## 6. Módulo 5: Comercial y Cotizaciones (`CotizacionController`)

Controlador: [`CotizacionController.java`](../../backend/src/main/java/pe/sga/controller/CotizacionController.java)  
Ruta base: `/api/v1/cotizaciones`  
Seguridad: Lectura con `ACCESS_CLIENTE_COTIZACION_READ`; creación/edición/asignación/anulación con `ACCESS_CLIENTE_COTIZACION_WRITE`; revisión y cambio de estado con `ACCESS_COTIZACION_APROBACION`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista cotizaciones con filtros por estado o cliente. |
| `GET` | `/sin-asignar` | Lista solicitudes de cotización pendientes de asignación de asesor comercial. |
| `GET` | `/{id}` | Obtiene el detalle financiero y desglose de costos de una cotización. |
| `GET` | `/codigo/{codigo}` | Busca una cotización por su código correlativo (`COT-YYYY-XXXX`). |
| `POST` | `/` | Registra una nueva cotización calculando flete base, recargos y tributos estimados. |
| `PUT` | `/{id}` | Modifica o corrige una cotización existente. |
| `PATCH` | `/{id}/asignar` | Asigna un asesor comercial responsable a la cotización. |
| `PATCH` | `/{id}/revision` | Registra el resultado de la revisión técnica aduanera sobre la cotización. |
| `PATCH` | `/{id}/estado` | Cambia el estado de la cotización dentro de su ciclo de vida. |
| `DELETE` | `/{id}` | *Soft delete*: Anula o da de baja lógica una cotización. |

---

## 7. Módulo 6: Finanzas y Control de Pagos (`CotizacionPagoController`)

Controlador: [`CotizacionPagoController.java`](../../backend/src/main/java/pe/sga/controller/CotizacionPagoController.java)  
Ruta base: `/api/v1`  
Seguridad: Registro de abonos con `ACCESS_COTIZACION_PAGO_WRITE`; consulta por cotización con `ACCESS_CLIENTE_COTIZACION_READ`; consulta de anticipos con `ACCESS_LIQUIDACION_READ`; validación con `ACCESS_LIQUIDACION_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `POST` | `/cotizaciones/{idCotizacion}/pagos` | Registra un abono o anticipo asociado a una cotización. |
| `GET` | `/cotizaciones/{idCotizacion}/pagos` | Lista los pagos y anticipos realizados sobre una cotización específica. |
| `GET` | `/finanzas/anticipos` | Lista los anticipos registrados con filtros operativos. |
| `GET` | `/finanzas/anticipos/pendientes` | Lista todos los pagos en estado `PENDIENTE_REVISION` que esperan validación contable. |
| `GET` | `/finanzas/anticipos/{idPago}` | Obtiene el detalle de un anticipo o voucher específico. |
| `PATCH` | `/finanzas/anticipos/{idPago}/validar` | **Acción Clave:** Aprueba (`APROBADO`), observa (`OBSERVADO`) o rechaza (`RECHAZADO`) el abono, liberando la compuerta financiera cuando corresponde. |

---

## 8. Módulo 7: Trámite Aduanero y DAM (`ExpedienteController`)

Controlador: [`ExpedienteController.java`](../../backend/src/main/java/pe/sga/controller/ExpedienteController.java)  
Ruta base: `/api/v1/expedientes`  
Seguridad: Lectura con `ACCESS_EXPEDIENTE_READ` (`ACCESS_INTERNAL_STAFF`); escritura y transiciones de hito con `ACCESS_EXPEDIENTE_ALMACEN_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Bandeja general de expedientes aduaneros con filtros por estado y canal SUNAT. |
| `GET` | `/{id}` | Detalle exhaustivo de la operación: DAM, manifiesto, bultos, fechas y auditoría. |
| `GET` | `/codigo/{codigo}` | Busca un expediente por su código correlativo (`EXP-YYYY-XXXX`). |
| `GET` | `/cotizacion/{idCotizacion}` | Obtiene el expediente vinculado a una cotización específica. |
| `POST` | `/` | **Apertura de Expediente:** Valida compuerta financiera y genera código `EXP-YYYY-XXXX`. Si el cliente es nuevo, aprovisiona credenciales B2B. |
| `PUT` | `/{id}` | Actualiza los datos operativos de un expediente. |
| `PATCH` | `/{id}/canal` | Registra el canal oficial asignado por SUNAT (`VERDE`, `NARANJA` o `ROJO`). |
| `PATCH` | `/{id}/diligencia` | Registra diligencia documentaria o física de aduanas. |
| `PATCH` | `/{id}/levante` | Registra la autorización de levante aduanero emitida por SUNAT. |
| `POST` | `/{id}/solicitar-recojo-exportacion` | Solicita recojo local de carga para operaciones de exportación. |
| `POST` | `/{id}/confirmar-ingreso-origen` | Confirma ingreso de carga de exportación en almacén/terminal de origen. |
| `PATCH` | `/{id}/confirmar-salida-origen` | Confirma salida de origen hacia terminal de embarque. |
| `PATCH` | `/{id}/confirmar-arribo-destino` | Confirma arribo de nave/vuelo en puerto o aeropuerto de destino. |
| `PATCH` | `/{id}/confirmar-embarque` | Confirma embarque internacional de la carga. |
| `PATCH` | `/{id}/confirmar-recepcion-exterior` | Confirma recepción en destino exterior. |
| `POST` | `/{id}/solicitar-transporte` | Solicita despacho de transporte terrestre asociado al expediente. |
| `POST` | `/{id}/retiro-directo` | Registra retiro directo autorizado por el cliente/aduana. |
| `PATCH` | `/{id}/cerrar` | Cierra formalmente el expediente aduanero. |
| `DELETE` | `/{id}` | *Soft delete*: Anula o da de baja lógica el expediente. |

---

## 9. Módulo 8: Depósito Temporal y Balanza VGM (`ControlAlmacenController`)

Controlador: [`ControlAlmacenController.java`](../../backend/src/main/java/pe/sga/controller/ControlAlmacenController.java)  
Ruta base: `/api/v1/almacen`  
Seguridad: Lectura con `ACCESS_EXPEDIENTE_ALMACEN_READ`; escritura con `ACCESS_EXPEDIENTE_ALMACEN_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista los registros de control en depósito temporal. |
| `GET` | `/{id}` | Obtiene el detalle de un registro de control en almacén. |
| `GET` | `/expediente/{idExpediente}` | Obtiene el control de almacén asociado a un expediente aduanero. |
| `POST` | `/` | Registra el ingreso físico de la carga y pesaje en báscula fiscal VGM. |
| `PUT` | `/{id}` | Actualiza los datos de recepción o pesaje VGM. |
| `PATCH` | `/{id}/autorizar-salida` | Valida levante SUNAT y ticket VGM antes de autorizar el retiro de la carga hacia transporte. |
| `DELETE` | `/{id}` | *Soft delete*: Da de baja lógica un registro de control de almacén. |

---

## 10. Módulo 9: Transporte, Flota y Choferes

### 10.1 Despachos en Ruta (`DespachoTransporteController`)
Controlador: [`DespachoTransporteController.java`](../../backend/src/main/java/pe/sga/controller/DespachoTransporteController.java)  
Ruta base: `/api/v1/transporte/despachos`  
Seguridad: Lectura con `ACCESS_TRANSPORTE_READ`; escritura con `ACCESS_TRANSPORTE_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista los despachos terrestres activos e históricos. |
| `GET` | `/{id}` | Obtiene el detalle de una orden de despacho por ID. |
| `GET` | `/tracking/{codigoTracking}` | Consulta un despacho por su código correlativo de tracking (`TRK-YYYY-XXXX`). |
| `GET` | `/mi-despacho-activo` | Obtiene el despacho activo asociado al operador/usuario en sesión. |
| `GET` | `/mis-despachos` | Lista los despachos asignados al usuario en sesión. |
| `GET` | `/expediente/{idExpediente}` | Obtiene el despacho de transporte vinculado a un expediente aduanero. |
| `POST` | `/` | Planifica y emite una orden de despacho (`TRK-YYYY-XXXX`) vinculando expediente. |
| `PATCH` | `/{id}/asignar-recursos` | Asigna unidad de transporte y conductor con brevete MTC vigente. |
| `PATCH` | `/{id}/iniciar-ruta` | Cambia estado a `EN_RUTA` y registra la salida del terminal. |
| `POST` | `/{id}/incidencia` | Registra una incidencia operativa o de seguridad ocurrida en ruta. |
| `PATCH` | `/{id}/confirmar-entrega` | Confirma la entrega en almacén del cliente y libera recursos. |
| `DELETE` | `/{id}` | *Soft delete*: Anula o da de baja lógica una orden de despacho. |

### 10.2 Padrón de Conductores MTC (`ConductorController`)
Controlador: [`ConductorController.java`](../../backend/src/main/java/pe/sga/controller/ConductorController.java)  
Ruta base: `/api/v1/transporte/conductores`  
Seguridad: Lectura con `ACCESS_TRANSPORTE_READ`; escritura con `ACCESS_TRANSPORTE_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista choferes con categoría de brevete profesional (A-IIIa/b/c) y vigencia. |
| `GET` | `/{id}` | Obtiene el detalle de un conductor por ID. |
| `GET` | `/licencia/{numeroLicencia}` | Busca un conductor por su número de licencia MTC. |
| `POST` | `/` | Registra un nuevo conductor validando documento de identidad y licencia. |
| `PUT` | `/{id}` | Actualiza los datos o vigencia de licencia de un conductor. |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva el chofer del padrón activo. |
| `PATCH` | `/{id}/reactivar` | Reactiva un conductor previamente desactivado. |

### 10.3 Padrón de Unidades de Transporte (`UnidadTransporteController`)
Controlador: [`UnidadTransporteController.java`](../../backend/src/main/java/pe/sga/controller/UnidadTransporteController.java)  
Ruta base: `/api/v1/transporte/unidades`  
Seguridad: Lectura con `ACCESS_TRANSPORTE_READ`; escritura con `ACCESS_TRANSPORTE_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista tractos, camiones y semirremolques con placa, configuración de ejes y CITV. |
| `GET` | `/{id}` | Obtiene el detalle de una unidad de transporte por ID. |
| `GET` | `/placa/{placaTracto}` | Busca una unidad de transporte por número de placa. |
| `POST` | `/` | Registra una nueva unidad de transporte con su póliza y vigencia técnica. |
| `PUT` | `/{id}` | Actualiza los datos técnicos o vigencias de una unidad. |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva la unidad del padrón activo. |
| `PATCH` | `/{id}/reactivar` | Reactiva una unidad previamente desactivada. |

---

## 11. Módulo 10: Liquidación Financiera Final (`LiquidacionFinancieraController`)

Controlador: [`LiquidacionFinancieraController.java`](../../backend/src/main/java/pe/sga/controller/LiquidacionFinancieraController.java)  
Ruta base: `/api/v1/finanzas/liquidaciones`  
Seguridad: Lectura con `ACCESS_LIQUIDACION_READ`; escritura con `ACCESS_LIQUIDACION_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista liquidaciones con filtros por estado de cobranza y cliente. |
| `GET` | `/metricas` | Retorna métricas financieras consolidadas (facturación, cobranzas, saldos y SPOT). |
| `GET` | `/calcular-estimado/{idExpediente}` | Calcula el estimado de liquidación financiera a partir de un expediente aduanero. |
| `GET` | `/{id}` | Obtiene el detalle de una liquidación financiera por ID. |
| `GET` | `/codigo/{codigo}` | Busca una liquidación por su código correlativo (`LIQ-YYYY-XXXX`). |
| `GET` | `/expediente/{idExpediente}` | Obtiene la liquidación asociada a un expediente aduanero. |
| `POST` | `/` | Genera la liquidación definitiva calculando tributos reales, gastos, IGV y SPOT (12%). |
| `PUT` | `/{id}` | Actualiza una liquidación que aún no ha sido pagada ni anulada. |
| `PATCH` | `/{id}/pago` | Registra la confirmación del pago final de la liquidación. |
| `DELETE` | `/{id}` | *Soft delete*: Anula una liquidación financiera (`ANULADO`). |

---

## 12. Módulo 11: Padrón de Clientes (`ClienteController`)

Controlador: [`ClienteController.java`](../../backend/src/main/java/pe/sga/controller/ClienteController.java)  
Ruta base: `/api/v1/clientes`  
Seguridad: Lectura con `ACCESS_CLIENTE_COTIZACION_READ`; escritura con `ACCESS_CLIENTE_COTIZACION_WRITE`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista el directorio maestro de clientes y prospectos comerciales. |
| `GET` | `/{id}` | Detalle de la empresa, representante legal, dirección fiscal y correo de contacto. |
| `GET` | `/documento/{numeroDocumento}` | Búsqueda rápida por número de documento / RUC (11 dígitos). |
| `POST` | `/` | Registra un nuevo cliente o prospecto importador/exportador. |
| `PUT` | `/{id}` | Actualiza datos de contacto o información comercial. |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva un cliente del padrón activo. |
| `PATCH` | `/{id}/reactivar` | Reactiva un cliente previamente desactivado. |
| `PATCH` | `/{id}/convertir-cliente` | Convierte formalmente un prospecto comercial en cliente activo. |

---

## 13. Módulo 12: Archivo Documental y Bóveda (`DocumentoController`)

Controlador: [`DocumentoController.java`](../../backend/src/main/java/pe/sga/controller/DocumentoController.java)  
Ruta base: `/api/v1/documentos`  
Seguridad: Clase protegida con `@PreAuthorize("isAuthenticated()")` (accesible para personal interno y clientes B2B según pertenencia); eliminación restringida a `ACCESS_INTERNAL_STAFF`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `POST` | `/upload` | Sube uno o más archivos digitales en formato `multipart/form-data` (máx. 25 MB por archivo), vinculándolos polimórficamente (`COTIZACION`, `EXPEDIENTE`, etc.). |
| `GET` | `/` | Lista los documentos adjuntos filtrando por `entidadTipo` y su ID asociado. |
| `GET` | `/{id}` | Obtiene los metadatos de un documento registrado. |
| `GET` | `/{id}/descargar` | Descarga el archivo original con su tipo MIME (`Content-Disposition: attachment`). |
| `GET` | `/{id}/preview` | Sirve el archivo en línea (`Content-Disposition: inline`) para previsualización en el navegador. |
| `DELETE` | `/{id}` | *Soft delete* / baja documental (restringido a `ACCESS_INTERNAL_STAFF`). |

---

## 14. Módulo 13: Monitoreo y Salud del Servidor (`HealthController`)

Controlador: [`HealthController.java`](../../backend/src/main/java/pe/sga/controller/HealthController.java)  
Ruta base: `/api/v1/health`  
Seguridad: **Público**

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Comprueba que el servidor Spring Boot esté respondiendo peticiones. Retorna `{ "status": "UP", "timestamp": "..." }`. Utilizado por contenedores Docker y balanceadores de carga para *liveness probes*. |
