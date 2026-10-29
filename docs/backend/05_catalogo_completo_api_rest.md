# 5. Catálogo Completo de Endpoints RESTful (API Reference)

**Sistema de Gestión Aduanera - Transaduanas**  
*Inventario Exhaustivo de los 21 Controladores REST, Rutas, Métodos HTTP y Seguridad*

---

## Índice del Capítulo
1. [Convenciones Globales de la API](#1-convenciones-globales-de-la-api)
2. [Módulo 1: Autenticación y Sesión (`AuthController`)](#2-módulo-1-autenticación-y-sesión-authcontroller)
3. [Módulo 2: Portal del Cliente B2B (`PortalController`)](#3-módulo-2-portal-del-cliente-b2b-portalcontroller)
4. [Módulo 3: Administración TI y Gobernanza](#4-módulo-3-administración-ti-y-gobernanza)
   - [3.1 Usuarios del Portal B2B (`UsuarioClienteAdminController`)](#31-usuarios-del-portal-b2b-usuarioclienteadmincontroller)
   - [3.2 Monitoreo y Consola de Logs (`AdminLogsController`)](#32-monitoreo-y-consola-de-logs-adminlogscontroller)
   - [3.3 Gestión de Personal Interno (`UsuarioController`)](#33-gestión-de-personal-interno-usuariocontroller)
   - [3.4 Sedes y Sucursales (`SedeController`)](#34-sedes-y-sucursales-sedecontroller)
   - [3.5 Roles del Sistema (`RolController`)](#35-roles-del-sistema-rolcontroller)
5. [Módulo 4: Catálogos Maestros (Aduanas y Puertos)](#5-módulo-4-catálogos-maestros-aduanas-y-puertos)
   - [4.1 Puertos y Aeropuertos (`CatalogoPuertoController`)](#41-puertos-y-aeropuertos-catalogopuertocontroller)
   - [4.2 Tipos de Carga (`CatalogoTipoCargaController`)](#42-tipos-de-carga-catalogotipocargacontroller)
   - [4.3 Subpartidas Arancelarias NANDINA (`CatalogoMercanciaController`)](#43-subpartidas-arancelarias-nandina-catalogomercanciacontroller)
6. [Módulo 5: Comercial y Cotizaciones (`CotizacionController`)](#6-módulo-5-comercial-y-cotizaciones-cotizacioncontroller)
7. [Módulo 6: Finanzas y Control de Pagos (`CotizacionPagoController`)](#7-módulo-6-finanzas-y-control-de-pagos-cotizacionpagocontroller)
8. [Módulo 7: Trámite Aduanero y DAM (`ExpedienteController`)](#8-módulo-7-trámite-aduanero-y-dam-expedientecontroller)
9. [Módulo 8: Depósito Temporal y Balanza VGM (`ControlAlmacenController`)](#9-módulo-8-depósito-temporal-y-balanza-vgm-controlalmacencontroller)
10. [Módulo 9: Transporte, Flota y Choferes](#10-módulo-9-transporte-flota-y-choferes)
    - [9.1 Despachos en Ruta (`DespachoTransporteController`)](#91-despachos-en-ruta-despachotransportecontroller)
    - [9.2 Padrón de Conductores MTC (`ConductorController`)](#92-padrón-de-conductores-mtc-conductorcontroller)
    - [9.3 Padrón de Unidades de Transporte (`UnidadTransporteController`)](#93-padrón-de-unidades-de-transporte-unidadtransportecontroller)
11. [Módulo 10: Liquidación Financiera Final (`LiquidacionFinancieraController`)](#11-módulo-10-liquidación-financiera-final-liquidacionfinancieracontroller)
12. [Módulo 11: Padrón de Clientes (`ClienteController`)](#12-módulo-11-padrón-de-clientes-clientecontroller)
13. [Módulo 12: Archivo Documental y Bóveda (`DocumentoController`)](#13-módulo-12-archivo-documental-y-bóveda-documentocontroller)
14. [Módulo 13: Monitoreo y Salud del Servidor (`HealthController`)](#14-módulo-13-monitoreo-y-salud-del-servidor-healthcontroller)

---

## 1. Convenciones Globales de la API

- **Prefijo base común:** `/api/v1`
- **Formato de intercambio:** `application/json;charset=UTF-8` (o `multipart/form-data` para subida de vouchers y PDFs).
- **Cabecera de autenticación obligatoria:**  
  `Authorization: Bearer <token_jwt>` (excepto en rutas marcadas como públicas).
- **Estructura unificada de respuesta:**
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

Controlador: [`AuthController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/AuthController.java)  
Ruta base: `/api/v1/auth`

| Método | Endpoint | Acceso / Rol | Descripción y Contrato |
| :---: | :--- | :---: | :--- |
| `POST` | `/login` | **Público** | Inicia sesión en el sistema. Valida usuario y contraseña cifrada con BCrypt. Retorna token JWT Bearer, rol, nombres y datos de la sede asignada. |
| `GET` | `/me` | Autenticado | Retorna el perfil y permisos del usuario actualmente conectado a partir del token. |

---

## 3. Módulo 2: Portal del Cliente B2B (`PortalController`)

Controlador: [`PortalController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/PortalController.java)  
Ruta base: `/api/v1/portal`  
Seguridad: Exclusivo para clientes autenticados con rol `ROLE_CLIENTE` (o administradores TI).

| Método | Endpoint | Descripción Funcional |
| :---: | :--- | :--- |
| `GET` | `/dashboard` | Resumen ejecutivo del cliente: total de expedientes activos, cotizaciones pendientes, liquidaciones y notificaciones no leídas. |
| `GET` | `/expedientes` | Lista todos los expedientes aduaneros asociados al RUC del cliente, con su estado, canal SUNAT y fecha estimada de entrega. |
| `GET` | `/expedientes/{id}` | Detalle completo de un expediente aduanero: hitos de tracking, DAM, unidad de transporte asignada y documentos descargables. |
| `GET` | `/cotizaciones` | Lista las cotizaciones solicitadas por el cliente con su estado (Borrador, Enviada, Aprobada). |
| `GET` | `/liquidaciones` | Lista las liquidaciones tributarias y financieras emitidas a nombre del cliente. |
| `GET` | `/obligaciones-pendientes` | **Endpoint de Deuda Heredada:** Retorna el consolidado de cuotas pendientes de cotizaciones y liquidaciones con saldo pendiente para ese cliente. |
| `POST` | `/pagos` | Registra un nuevo abono o pago bancario con voucher adjunto (`multipart/form-data`). Valida archivo de hasta 10 MB y coloca el pago en estado `PENDIENTE_VALIDACION`. |
| `GET` | `/notificaciones` | Lista las notificaciones de eventos (levante autorizado, cambio de canal SUNAT, salida de camión). |
| `PUT` | `/notificaciones/{id}/leer`| Marca una notificación como leída. |
| `GET` | `/documentos/descargar/{id}` | Descarga segura de un archivo adjunto perteneciente a un expediente del cliente (factura, BL, packing list). |

---

## 4. Módulo 3: Administración TI y Gobernanza

### 3.1 Usuarios del Portal B2B (`UsuarioClienteAdminController`)
Ruta base: `/api/v1/admin/usuarios-cliente`  
Seguridad: `ROLE_ADMINISTRADOR_TI`

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista todas las cuentas de acceso al Portal B2B vinculadas a clientes RUC. |
| `POST` | `/` | Crea una cuenta de acceso al Portal B2B para una empresa cliente. |
| `PUT` | `/{id}/reset-password` | Restablece la contraseña de una empresa y activa la bandera de cambio obligatorio en el siguiente acceso. |
| `PATCH` | `/{id}/estado` | Habilita o deshabilita el acceso de una empresa al Portal B2B. |

### 3.2 Monitoreo y Consola de Logs (`AdminLogsController`)
Ruta base: `/api/v1/admin/logs`  
Seguridad: `ROLE_ADMINISTRADOR_TI`

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/tail` | Retorna las últimas N líneas en vivo del archivo de bitácora del servidor (`sga-backend.log`). |
| `GET` | `/descargar` | Descarga el archivo de log rotativo comprimido para auditorías técnicas. |

### 3.3 Gestión de Personal Interno (`UsuarioController`)
Ruta base: `/api/v1/usuarios`  
Seguridad: `ROLE_ADMINISTRADOR_TI`

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista el personal activo de la organización (los 8 roles). |
| `POST` | `/` | Registra un nuevo colaborador con su rol, sede, correo institucional y credenciales. |
| `PUT` | `/{id}` | Actualiza datos del personal (sede asignada, teléfono, cargo). |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva la cuenta del colaborador (`estado_activo = false`). |

### 3.4 Sedes y Sucursales (`SedeController`)
Ruta base: `/api/v1/sedes`  
Seguridad: Lectura para todos los roles; escritura para `ADMINISTRADOR_TI`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista las 6 sedes (Sede Central, Callao Marítimo, Callao Aéreo, Paita, Matarani, San Isidro). |
| `POST` | `/` | Registra una nueva sede o terminal portuario. |
| `GET` | `/reporte-pdf` | Genera el padrón oficial de sedes en formato PDF mediante Thymeleaf y OpenHTMLtoPDF. |

### 3.5 Roles del Sistema (`RolController`)
Ruta base: `/api/v1/roles`  
Seguridad: `ROLE_ADMINISTRADOR_TI`

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Retorna el catálogo de los 8 roles institucionales con sus descripciones y permisos asociados. |

---

## 5. Módulo 4: Catálogos Maestros (Aduanas y Puertos)

### 5.1 Puertos y Aeropuertos (`CatalogoPuertoController`)
Ruta base: `/api/v1/catalogos/puertos`

| Método | Endpoint | Seguridad | Descripción |
| :---: | :--- | :---: | :--- |
| `GET` | `/` | Todos | Lista los 80 puertos y aeropuertos marítimos/aéreos registrados. |
| `POST` | `/` | TI | Registra un nuevo puerto o terminal logístico. |
| `GET` | `/reporte-pdf` | Todos | Genera el catálogo maestro de puertos en PDF con bandera de país y tipo. |

### 5.2 Tipos de Carga (`CatalogoTipoCargaController`)
Ruta base: `/api/v1/catalogos/tipos-carga`

| Método | Endpoint | Seguridad | Descripción |
| :---: | :--- | :---: | :--- |
| `GET` | `/` | Todos | Lista las 17 tipologías de carga (FCL, LCL, Reefer, IMO, Proyecto, etc.). |
| `GET` | `/reporte-pdf` | Todos | Genera el reporte corporativo de tipos de carga en PDF. |

### 5.3 Subpartidas Arancelarias NANDINA (`CatalogoMercanciaController`)
Ruta base: `/api/v1/catalogos/mercancias`

| Método | Endpoint | Seguridad | Descripción |
| :---: | :--- | :---: | :--- |
| `GET` | `/` | Todos | Lista las 52 subpartidas NANDINA con arancel base y entidad reguladora. |
| `GET` | `/buscar` | Todos | Búsqueda por código de 10 dígitos (ej. `8517.13.00.00`) o descripción. |
| `GET` | `/reporte-pdf` | Todos | Genera el padrón arancelario oficial en PDF. |

---

## 6. Módulo 5: Comercial y Cotizaciones (`CotizacionController`)

Controlador: [`CotizacionController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/CotizacionController.java)  
Ruta base: `/api/v1/cotizaciones`  
Seguridad: `AGENTE_COMERCIAL`, `GERENTE_GENERAL`, `ADMINISTRADOR_TI`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista cotizaciones con filtros por cliente, rango de fechas y estado. |
| `GET` | `/{id}` | Obtiene el detalle financiero y desglose de costos de una cotización. |
| `POST` | `/` | Registra una nueva cotización calculando flete base, recargos y tributos estimados. |
| `PUT` | `/{id}` | Modifica una cotización en estado borrador. |
| `PATCH` | `/{id}/estado` | Cambia el estado (`BORRADOR`, `ENVIADA`, `APROBADA`, `RECHAZADA`). |
| `GET` | `/{id}/pdf` | Genera la proforma comercial formal de la cotización en formato PDF. |

---

## 7. Módulo 6: Finanzas y Control de Pagos (`CotizacionPagoController`)

Controlador: [`CotizacionPagoController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/CotizacionPagoController.java)  
Ruta base: `/api/v1/pagos`  
Seguridad: `CONTADOR`, `ADMINISTRADOR_TI` (y registro desde portal de clientes).

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/pendientes` | Lista todos los pagos en estado `PENDIENTE_VALIDACION` que esperan revisión contable. |
| `GET` | `/cotizacion/{id}` | Lista los pagos y anticipos realizados a una cotización específica. |
| `POST` | `/` | Registra un pago de forma interna adjuntando comprobante bancario. |
| `PATCH` | `/{id}/validar` | **Acción Clave:** El contador valida formalmente el abono bancario, desbloqueando la compuerta financiera para abrir el expediente. |
| `PATCH` | `/{id}/rechazar` | El contador rechaza el comprobante indicando motivo (ej. "Voucher ilegible o monto discrepante"). |

---

## 8. Módulo 7: Trámite Aduanero y DAM (`ExpedienteController`)

Controlador: [`ExpedienteController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/ExpedienteController.java)  
Ruta base: `/api/v1/expedientes`  
Seguridad: `SUPERVISOR_ADUANERO`, `AGENTE_ADUANERO`, `GERENTE_GENERAL`, `ADMINISTRADOR_TI`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Bandeja general de expedientes aduaneros con filtros por estado y canal SUNAT. |
| `GET` | `/{id}` | Detalle exhaustivo de la operación: DAM, manifiesto, bultos, fechas y auditoría. |
| `POST` | `/` | **Apertura de Expediente:** Valida compuerta de pago y genera código `EXP-YYYY-XXXX`. Si el cliente es nuevo, aprovisiona credenciales B2B. |
| `PATCH` | `/{id}/canal-sunat` | Registra el canal oficial asignado por SUNAT (Verde, Naranja o Rojo). |
| `PATCH` | `/{id}/levante` | Registra la autorización de levante aduanero emitida por SUNAT. |
| `GET` | `/{id}/hoja-tramite-pdf` | Emite la Hoja de Trámite Aduanero oficial en PDF vectorial. |

---

## 9. Módulo 8: Depósito Temporal y Balanza VGM (`ControlAlmacenController`)

Controlador: [`ControlAlmacenController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/ControlAlmacenController.java)  
Ruta base: `/api/v1/almacen`  
Seguridad: `AGENTE_ADUANERO`, `SUPERVISOR_ADUANERO`, `ADMINISTRADOR_TI`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/ingresos` | Lista las cargas actualmente en custodia dentro del depósito temporal. |
| `POST` | `/ingreso` | Registra el ingreso físico de la carga al recinto aduanero. |
| `POST` | `/pesaje-vgm` | Registra el pesaje obligatorio en báscula fiscal (tara, peso neto, VGM certificado). |
| `PATCH` | `/{id}/autorizar-salida` | Valida levante SUNAT y ticket VGM antes de permitir el retiro de la carga hacia transporte. |

---

## 10. Módulo 9: Transporte, Flota y Choferes

### 10.1 Despachos en Ruta (`DespachoTransporteController`)
Ruta base: `/api/v1/despachos`  
Seguridad: `SUPERVISOR_TRANSPORTE`, `GERENTE_GENERAL`, `ADMINISTRADOR_TI`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista los despachos terrestres activos e históricos. |
| `POST` | `/` | Planifica y emite una orden de despacho vinculando expediente, tracto y chofer MTC. |
| `PATCH` | `/{id}/iniciar-ruta` | Cambia estado a `EN_RUTA` y registra hora de salida de terminal. |
| `PATCH` | `/{id}/confirmar-entrega` | Confirma entrega en almacén del cliente y adjunta acta de conformidad. |
| `GET` | `/{id}/guia-remision-pdf` | Emite la Guía de Remisión Transportista electrónica en PDF. |

### 10.2 Padrón de Conductores MTC (`ConductorController`)
Ruta base: `/api/v1/conductores`

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista choferes con categoría de brevete profesional (A-IIIa/b/c) y vigencia. |
| `POST` | `/` | Registra un nuevo conductor validando documento de identidad y licencia. |
| `DELETE` | `/{id}` | *Soft delete*: Desactiva el chofer del padrón activo. |

### 10.3 Padrón de Unidades de Transporte (`UnidadTransporteController`)
Ruta base: `/api/v1/unidades`

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista tractos, camiones y semirremolques con placa, configuración de ejes y CITV. |
| `POST` | `/` | Registra una nueva unidad de transporte con su póliza y vigencia técnica. |

---

## 11. Módulo 10: Liquidación Financiera Final (`LiquidacionFinancieraController`)

Controlador: [`LiquidacionFinancieraController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/LiquidacionFinancieraController.java)  
Ruta base: `/api/v1/liquidaciones`  
Seguridad: `CONTADOR`, `GERENTE_GENERAL`, `ADMINISTRADOR_TI`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista liquidaciones con filtros por estado de cobranza y cliente. |
| `POST` | `/` | Genera la liquidación definitiva calculando tributos reales, gastos, IGV y SPOT (12%). |
| `GET` | `/{id}/pdf` | Genera la Hoja de Liquidación Financiera Oficial en formato PDF corporativo. |
| `PATCH` | `/{id}/cerrar` | Cierra formal y contablemente la operación aduanera. |

---

## 12. Módulo 11: Padrón de Clientes (`ClienteController`)

Controlador: [`ClienteController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/ClienteController.java)  
Ruta base: `/api/v1/clientes`  
Seguridad: `AGENTE_COMERCIAL`, `CONTADOR`, `SUPERVISOR_ADUANERO`, `ADMINISTRADOR_TI`.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Lista el directorio maestro de clientes y prospectos comerciales. |
| `GET` | `/{id}` | Detalle de la empresa, representante legal, dirección fiscal y correo de contacto. |
| `GET` | `/buscar?ruc=...` | Búsqueda rápida por número de RUC (11 dígitos). |
| `POST` | `/` | Registra un nuevo cliente o prospecto importador/exportador. |
| `PUT` | `/{id}` | Actualiza datos de contacto o estado crediticio. |

---

## 13. Módulo 12: Archivo Documental y Bóveda (`DocumentoController`)

Controlador: [`DocumentoController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/DocumentoController.java)  
Ruta base: `/api/v1/documentos`  
Seguridad: Todos los roles autorizados según el documento vinculado.

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `POST` | `/subir` | Sube un archivo digital en formato `multipart/form-data` (máx. 25 MB). Permite vincularlo polimórficamente a una cotización o expediente aduanero. |
| `GET` | `/descargar/{id}` | Descarga el archivo original con su tipo MIME correcto (`application/pdf`, `image/jpeg`). |
| `GET` | `/expediente/{id}` | Lista todos los documentos adjuntos de un expediente (BL, factura, packing list, etc.). |

---

## 14. Módulo 13: Monitoreo y Salud del Servidor (`HealthController`)

Controlador: [`HealthController.java`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/java/pe/sga/controller/HealthController.java)  
Ruta base: `/api/v1/health`  
Seguridad: **Público**

| Método | Endpoint | Descripción |
| :---: | :--- | :--- |
| `GET` | `/` | Comprueba que el servidor Spring Boot esté respondiendo peticiones. Retorna `{ "status": "UP", "timestamp": "..." }`. Utilizado por contenedores Docker y balanceadores de carga para *liveness probes*. |
