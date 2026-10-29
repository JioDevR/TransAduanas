# 4. Catálogo de Módulos, Vistas y Componentes de `app-sga`

**Sistema de Gestión Aduanera - Transaduanas (`app-sga`)**  
*Guía Detallada de los Módulos de Negocio en `features/`*

---

## Índice del Capítulo
1. [Módulo de Administración TI (`features/admin/`)](#1-módulo-de-administración-ti-featuresadmin)
2. [Módulo de Autenticación (`features/auth/`)](#2-módulo-de-autenticación-featuresauth)
3. [Módulo de Gerencia General (`features/gerencia/`)](#3-módulo-de-gerencia-general-featuresgerencia)
4. [Módulo de Recepción Comercial (`features/comercial/`)](#4-módulo-de-recepción-comercial-featurescomercial)
5. [Módulo de Gestión Aduanera (`features/aduanas/`)](#5-módulo-de-gestión-aduanera-featuresaduanas)
6. [Módulo de Depósito Temporal y Almacén (`features/almacen/`)](#6-módulo-de-depósito-temporal-y-almacén-featuresalmacen)
7. [Módulo de Transporte y Flota TMS (`features/transporte/`)](#7-módulo-de-transporte-y-flota-tms-featurestransporte)
8. [Módulo de Finanzas y Control de Pagos (`features/finanzas/`)](#8-módulo-de-finanzas-y-control-de-pagos-featuresfinanzas)
9. [Módulo de Liquidación Aduanera (`features/liquidacion/`)](#9-módulo-de-liquidación-aduanera-featuresliquidacion)
10. [Módulo de Asesoría Legal y Normativa (`features/legal/`)](#10-módulo-de-asesoría-legal-y-normativa-featureslegal)
11. [Módulo de Archivo Documental Digital (`features/documentos/`)](#11-módulo-de-archivo-documental-digital-featuresdocumentos)

---

## 1. Módulo de Administración TI (`features/admin/`)

Ruta base: `/administracion-ti/*`  
Seguridad: `ROLE_ADMINISTRADOR_TI`

Este módulo centraliza el control de infraestructura y gobernanza de la plataforma a través de 6 vistas:

1. **Dashboard TI (`AdminDashboardView.vue`):** Muestra tarjetas de métricas (*MetricCard*) con el estado de salud de la base de datos, servidores de desarrollo, usuarios activos por rol y accesos rápidos a catálogos y logs.
2. **Gestión de Personal Interno (`UsersManagementView.vue` + `UserFormModal.vue`):** Permite dar de alta a nuevos colaboradores, asignar su rol institucional (de los 8 posibles), su sede de trabajo y credenciales. Al eliminar un usuario, ejecuta *Soft Delete* desactivándolo sin borrar su historial de auditoría.
3. **Gestión de Usuarios del Portal B2B (`UsuariosPortalManagementView.vue` + `UsuarioPortalFormModal.vue`):** Permite al administrador consultar las cuentas de las empresas importadoras/exportadoras creadas en la tabla `usuario_cliente`, resetear contraseñas temporales y habilitar o bloquear accesos.
4. **Sedes Corporativas (`SedesManagementView.vue` + `SedeFormModal.vue`):** Muestra el padrón de las 6 sedes territoriales y terminales portuarios (Central, San Isidro, Callao Marítimo, Callao Aéreo, Paita, Matarani) con botón de descarga del padrón oficial en PDF.
5. **Catálogos Maestros (`CatalogosManagementView.vue`):**
   - **Pestaña Puertos (`PuertosManagementView.vue`):** Directorio de los 80 puertos y aeropuertos mundiales con búsqueda por código UN/LOCODE.
   - **Pestaña Tipos de Carga (`TiposCargaManagementView.vue`):** Catálogo de las 17 modalidades (FCL, LCL, Reefer, Peligrosa IMO).
   - **Pestaña Mercancías NANDINA (`MercanciasManagementView.vue`):** Buscador de las 52 subpartidas de 10 dígitos, mostrando aranceles base y entidad reguladora sectorial (MTC, SENASA, DIGEMID).
6. **Consola del Sistema y Logs en Vivo (`AdminLogsView.vue`):** Visor en tiempo real de las últimas líneas del archivo `sga-backend.log` con botón de recarga automática (*polling*) y descarga del archivo histórico de auditoría.
7. **Acerca del Sistema (`AboutSystemView.vue`):** Muestra versiones de Spring Boot, Java, Vue 3, Vite y créditos arquitectónicos.

---

## 2. Módulo de Autenticación (`features/auth/`)

Ruta: `/login`

- **Vista: `LoginView.vue`**
- **Servicio: `authService.js`**
- **Store: `authStore.js`**
- **Funcionalidad:**
  - Formulario de acceso con diseño centrado en tarjeta *Warm Editorial*.
  - Validación de campos obligatorios en el cliente (nombre de usuario y contraseña).
  - Al recibir el token JWT y los datos del perfil desde el backend, almacena el token en `localStorage` y en el estado reactivo de Pinia.
  - Detecta el rol institucional y redirige al colaborador directamente a su pantalla de aterrizaje designada (`authStore.homePath`).
  - Si el usuario intentaba ingresar a una URL protegida específica, el login lo redirige a esa ruta original tras autenticarse con éxito.

---

## 3. Módulo de Gerencia General (`features/gerencia/`)

Ruta: `/gerencia-general/dashboard`  
Seguridad: `ROLE_GERENTE_GENERAL`, `ROLE_ADMINISTRADOR_TI`

- **Vista: `GerenciaDashboardView.vue`**
- **Funcionalidad:**
  - Cuadros de mando ejecutivos con indicadores clave de rendimiento (KPIs): número de cotizaciones activas, valor FOB cotizado acumulado, expedientes aduaneros en curso y facturación de liquidaciones.
  - Auditoría global de la flota pesada (unidades operativas vs en mantenimiento) y padrón de choferes MTC.
  - Visualización del volumen de operaciones distribuidas por sede territorial.

---

## 4. Módulo de Recepción Comercial (`features/comercial/`)

Ruta: `/recepcion-comercial`  
Seguridad: `ROLE_AGENTE_COMERCIAL`, `ROLE_GERENTE_GENERAL`, `ROLE_ADMINISTRADOR_TI`

- **Vista Principal: `RecepcionComercialView.vue`**
- **Componentes Clave:**
  - **Pestaña Clientes (`ClienteRegistrationForm.vue`):** Formulario para registrar nuevas empresas importadoras y exportadoras, validando RUC de 11 dígitos, razón social, dirección fiscal y datos de contacto comercial.
  - **Asistente de Cotizaciones de 4 Pasos (`CotizacionRegistrationForm.vue`):**
    - Guiado por la barra de navegación secuencial `CotizacionStepperNav.vue`.
    - **Paso 1:** Selección de cliente y régimen de aduanas.
    - **Paso 2:** Selección de modalidad (Marítimo/Aéreo) y puertos filtrados automáticamente mediante el composable `useModalidadPuertos.js`.
    - **Paso 3:** Selección de tipo de carga, partida arancelaria NANDINA, peso bruto y volumen.
    - **Paso 4:** Desglose interactivo de costos calculado en vivo por `useCotizacionCalculator.js`.
  - **Vista Previa de Proforma (`CotizacionProformaPreview.vue`):** Muestra el documento comercial listo para ser descargado en PDF o enviado al cliente.
  - **Corrección de Cotizaciones (`CotizacionCorrectionView.vue`):** Pantalla especializada para ajustar cotizaciones que fueron devueltas o rechazadas por discrepancias de costos.

---

## 5. Módulo de Gestión Aduanera (`features/aduanas/`)

Ruta: `/gestion-aduanera`  
Seguridad: `ROLE_SUPERVISOR_ADUANERO`, `ROLE_GERENTE_GENERAL`, `ROLE_ADMINISTRADOR_TI`

- **Vista Principal: `GestionAduaneraView.vue`**
- **Servicio: `expedienteService.js`**
- **Funcionalidad:**
  - **Bandeja de Cotizaciones Aprobadas:** Muestra las cotizaciones comerciales listas para ser convertidas en expedientes formales.
  - **Verificación de la Compuerta Financiera:** El sistema consulta al backend si la cotización cuenta con un pago en estado `VALIDADO`. Si no cuenta con pago contable, el botón de apertura aparece inhabilitado con un distintivo de advertencia.
  - **Apertura de Expediente:** Genera el correlativo `EXP-YYYY-XXXX` y registra la fecha de arribo de la nave.
  - **Asignación de DAM y Canal SUNAT:** Permite al supervisor registrar el número de DAM y marcar el canal asignado por SUNAT (Verde, Naranja o Rojo).
  - **Alerta de Credenciales B2B:** Si el expediente pertenece a una empresa nueva, el modal presenta un mensaje emergente con el usuario (RUC) y contraseña temporal generada para que el supervisor se las entregue al cliente importador.

---

## 6. Módulo de Depósito Temporal y Almacén (`features/almacen/`)

Ruta: `/agente-aduanero`  
Seguridad: `ROLE_AGENTE_ADUANERO`, `ROLE_SUPERVISOR_ADUANERO`, `ROLE_ADMINISTRADOR_TI`

- **Vista Principal: `AgenteAduaneroView.vue`**
- **Servicio: `almacenService.js`**
- **Funcionalidad:**
  - **Control de Ingreso a Recinto:** Registro de fecha y hora de ingreso del contenedor o carga suelta al depósito temporal.
  - **Pesaje en Báscula Certificada VGM:** Modal interactivo para ingresar la tara del contenedor y el peso de la mercancía. Calcula automáticamente la Masa Bruta Verificada (VGM) en cumplimiento de la enmienda SOLAS y exige adjuntar el ticket de balanza fiscal.
  - **Autorización de Retiro:** Verifica que la carga cuente con levante autorizado por SUNAT antes de emitir el pase de salida hacia la unidad de transporte terrestre.

---

## 7. Módulo de Transporte y Flota TMS (`features/transporte/`)

Ruta: `/transporte-flota`  
Seguridad: `ROLE_SUPERVISOR_TRANSPORTE`, `ROLE_GERENTE_GENERAL`, `ROLE_ADMINISTRADOR_TI`

- **Vista Principal: `TransporteFlotaView.vue`**
- **Componentes y Modales:**
  - **Pestaña Flota (`FlotaManagementPanel.vue`):** Directorio de tractos, camiones y semirremolques con placas, marca, configuración de ejes y vigencia de pólizas de seguro.
  - **Pestaña Conductores (`ConductoresManagementPanel.vue`):** Padrón de choferes profesionales con validación de categoría de brevete MTC (A-IIIa, A-IIIb, A-IIIc).
  - **Modal de Creación de Despacho (`CrearDespachoModal.vue`):** Vincula un expediente aduanero con una orden de despacho `DSP-YYYY-XXXX`.
  - **Modal de Asignación de Recursos (`AsignarRecursosModal.vue`):** Selecciona el tracto y chofer disponible para el viaje.
  - **Modal de Confirmación de Entrega (`ConfirmarEntregaModal.vue`):** Registra la hora de llegada al almacén del cliente y permite adjuntar la Guía de Remisión firmada.

---

## 8. Módulo de Finanzas y Control de Pagos (`features/finanzas/`)

Ruta: `/finanzas`  
Seguridad: `ROLE_CONTADOR`, `ROLE_GERENTE_GENERAL`, `ROLE_ADMINISTRADOR_TI`

- **Vista Principal: `FinanzasView.vue`**
- **Componente Auxiliar: `RegistroPagoFullView.vue`**
- **Servicio: `finanzasService.js`**
- **Funcionalidad:**
  - **Bandeja de Anticipos Pendientes:** Lista los pagos registrados por agentes comerciales o subidos directamente por los clientes desde el Portal B2B (`PENDIENTE_VALIDACION`).
  - **Visor de Vouchers:** Permite al contador abrir y verificar el comprobante bancario (PDF o imagen de hasta 10 MB) en un diálogo modal sin salir del sistema.
  - **Botón de Validación Contable:** Al hacer clic en "Validar Abono", el contador confirma el ingreso de fondos a la cuenta corporativa, cambiando el estado a `VALIDADO` y **liberando la compuerta financiera** para que Aduanas pueda crear el expediente.
  - **Registro de Pagos Internos:** Permite al contador ingresar abonos manuales recibidos por caja o transferencias interbancarias directas.

---

## 9. Módulo de Liquidación Aduanera (`features/liquidacion/`)

Ruta: `/gestion-aduanera/liquidacion`  
Seguridad: `ROLE_CONTADOR`, `ROLE_SUPERVISOR_ADUANERO`, `ROLE_ADMINISTRADOR_TI`

- **Vista Principal: `LiquidacionAduaneraView.vue`**
- **Funcionalidad:**
  - Conciliación de tributos reales pagados ante la SUNAT frente a la estimación inicial de la cotización.
  - Detalle de gastos de agenciamiento, almacenamiento portuario, sobrestadía de contenedor y flete local.
  - Cálculo automático de **IGV (18%)** y desglose de la detracción obligatoria **SPOT (12%)**.
  - Emisión de la Hoja de Liquidación Financiera en formato PDF para el cobro del saldo al cliente.
  - Cierre formal del expediente operativo.

---

## 10. Módulo de Asesoría Legal y Normativa (`features/legal/`)

Ruta: `/legal-auditoria`  
Seguridad: `ROLE_ASESOR_LEGAL`, `ROLE_GERENTE_GENERAL`, `ROLE_ADMINISTRADOR_TI`

- **Vista Principal: `LegalAuditoriaView.vue`**
- **Funcionalidad:**
  - Lista de verificación regulatoria (*Checklist*) de mercancías restringidas.
  - Fiscalización de resoluciones y registros sanitarios de DIGEMID (medicamentos y cosméticos), SENASA (alimentos y agroexportación), SUCAMEC (químicos y explosivos) y PRODUCE.
  - Control de alertas de vencimiento de plazos legales aduaneros para evitar abandono legal de mercancías en depósito temporal.

---

## 11. Módulo de Archivo Documental Digital (`features/documentos/`)

- **Componentes:** `DocumentosUploadPanel.vue` y `DocumentosManagerModal.vue`
- **Servicio:** `documentoService.js`
- **Funcionalidad:**
  - Bóveda documental que permite adjuntar archivos polimórficos a cotizaciones, expedientes y liquidaciones.
  - Admite Facturas Comerciales, Packing Lists, Conocimientos de Embarque (Bill of Lading - BL), Guías de Remisión Transportista, Vouchers de Pago y Tickets de Balanza VGM.
  - Integra la utilidad `download.js` para descargar y visualizar archivos de forma segura revocando las URLs temporales en memoria para no saturar el navegador.
