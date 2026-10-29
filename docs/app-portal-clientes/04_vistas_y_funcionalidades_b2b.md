# 4. Catálogo de Vistas y Funcionalidades B2B de `app-portal-cliente`

**Sistema de Gestión Aduanera - Transaduanas (`app-portal-cliente`)**  
*Módulos de Autoservicio: Tracking DAM, Cotizaciones, Cobros/Pagos y Notificaciones*

---

## Índice del Capítulo
1. [Línea de Tiempo y Tracking Aduanero (`ExpedientesTrackingView.vue`)](#1-línea-de-tiempo-y-tracking-aduanero-expedientestrackingviewvue)
2. [Historial de Cotizaciones y Proformas (`CotizacionesListView.vue`)](#2-historial-de-cotizaciones-y-proformas-cotizacioneslistviewvue)
3. [Estado de Cuenta y Deuda Heredada (`CobrosPagosView.vue`)](#3-estado-de-cuenta-y-deuda-heredada-cobrospagosviewvue)
4. [Pantalla Dedicada de Registro de Pagos (`RegistrarPagoView.vue`)](#4-pantalla-dedicada-de-registro-de-pagos-registrarpagoviewvue)
5. [Centro de Notificaciones y Avisos Operativos (`PortalNotificacionesView.vue`)](#5-centro-de-notificaciones-y-avisos-operativos-portalnotificacionesviewvue)

---

## 1. Línea de Tiempo y Tracking Aduanero (`ExpedientesTrackingView.vue`)

Ruta: `/expedientes`  
Servicio: [`portalExpedienteService.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/features/expedientes/services/portalExpedienteService.js)

Esta vista es el corazón operativo del portal. Permite al cliente importador monitorear el estado exacto de su carga sin necesidad de enviar correos o llamar a la agencia:

### Características Principales:
1. **Línea de Tiempo Gráfica de 6 Hitos:** Muestra visualmente el progreso de la operación mediante círculos interactivos:
   $$\text{Apertura} \longrightarrow \text{DAM Numerada} \longrightarrow \text{Depósito VGM} \longrightarrow \text{Canal SUNAT} \longrightarrow \text{Levante} \longrightarrow \text{En Ruta / Entregado}$$
2. **Semáforo Oficial de la SUNAT:**
   - Si el expediente sale en **Canal Verde**, el sistema muestra un distintivo verde brillante indicando que no hay trabas físicas ni documentarias.
   - Si sale en **Canal Naranja**, indica *"En revisión documentaria por la aduana"*.
   - Si sale en **Canal Rojo**, indica *"Programado para reconocimiento físico de bultos en terminal"*, permitiendo que el cliente planifique sus tiempos de entrega.
3. **Ficha Técnica del Embarque:** Despliega el número de contenedor, tipo de carga (FCL/LCL), peso bruto verificado (VGM), puerto de origen, puerto de destino y la placa del tracto y chofer asignados por el área de transporte.
4. **Bóveda de Documentos Descargables:** Lista los archivos oficiales emitidos para ese expediente (Factura Comercial, Packing List, Bill of Lading - BL, Ticket de Báscula VGM y Guía de Remisión MTC). Al hacer clic en "Descargar", se descarga el archivo original con su nombre oficial.

---

## 2. Historial de Cotizaciones y Proformas (`CotizacionesListView.vue`)

Ruta: `/cotizaciones`  
Servicio: [`portalCotizacionService.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/features/cotizaciones/services/portalCotizacionService.js)

Es la pantalla de inicio por defecto del cliente corporativo:

### Funcionalidades:
- **Listado de Proformas Comerciales:** Tarjetas organizadas por fecha con el código oficial `COT-YYYY-XXXX`.
- **Valores Exclusivamente en Dólares (`$ USD`):** Presenta el valor FOB de la mercancía, el flete internacional, los seguros y los tributos aduaneros estimados de forma transparente.
- **Insignias de Estado:**
  - `ENVIADA`: Cotización emitida por el área comercial esperando revisión del cliente.
  - `APROBADA`: Cotización formalmente aceptada con compuerta de pago habilitada.
  - `EXPEDIENTE_CREADO`: Operación aduanera ya en curso con enlace directo a su expediente.
- **Descarga de Proforma Oficial:** Botón para descargar el documento PDF formal de la cotización para firmas y aprobaciones de gerencia interna del cliente.

---

## 3. Estado de Cuenta y Deuda Heredada (`CobrosPagosView.vue`)

Ruta: `/pagos`  
Servicio: [`portalPagoService.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/features/pagos/services/portalPagoService.js)

> [!IMPORTANT]
> **Rediseño Financiero Reciente (Moneda Única USD & Deuda Heredada):**  
> Para evitar confusiones cambiarias y facilitar el pago a clientes internacionales, todo el módulo financiero del portal fue unificado estrictamente en **Dólares Americanos (`USD`)**.

### Estructura de la Vista:
1. **Tarjetas de Resumen Financiero:**
   - **Total Facturado ($ USD):** Suma total de cotizaciones aprobadas y liquidaciones aduaneras.
   - **Total Pagado ($ USD):** Suma de abonos que ya fueron formalmente validados por el contador de Transaduanas.
   - **Saldo Pendiente ($ USD):** Deuda neta viva que el cliente tiene pendiente de pago.
2. **Pestaña 1: "Obligaciones Pendientes" (Deuda Heredada):**
   - Consume el endpoint `/api/v1/portal/obligaciones-pendientes`.
   - Desglosa las cuotas pendientes tanto de **anticipos de cotización** (para que se pueda abrir el expediente) como de **saldos de liquidación final** de cargas ya entregadas.
   - Cada fila incluye un botón directo de acción: *"Pagar Cuota"*.
3. **Pestaña 2: "Historial de Abonos Registrados":**
   - Muestra todos los comprobantes que la empresa ha subido.
   - Estados: `VALIDADO` (con fecha y sello del contador) o `PENDIENTE_VALIDACION` (en revisión contable).
   - Permite visualizar el voucher bancario adjunto en cualquier momento.

---

## 4. Pantalla Dedicada de Registro de Pagos (`RegistrarPagoView.vue`)

Ruta: `/pagos/registrar`

Es una vista a pantalla completa diseñada para que el cliente realice sus abonos sin distracciones ni errores:

```
┌────────────────────────────────────────────────────────────────────────┐
│  ← Volver a Cobros y Pagos                                             │
│  REGISTRAR ABONO BANCARIO                                              │
├────────────────────────────────────────────────────────────────────────┤
│  1. Selecciona la Obligación o Expediente a amortizar:                 │
│     [ COT-2026-0012 - Importación Maquinaria (Saldo: $ 4,200.00) ▼ ]    │
│                                                                        │
│  2. Monto a Pagar:                                                     │
│     [ Preset 50% ($ 2,100) ]  [ Preset 100% ($ 4,200) ]  [ Otro Monto ]│
│     Input: [ $ 4,200.00 USD ]                                          │
│                                                                        │
│  3. Cuentas Bancarias TransAduanas en USD (Selecciona una):             │
│     ┌────────────────────────┐  ┌────────────────────────┐             │
│     │ BCP Dólares Corriente  │  │ BBVA Dólares Corriente │             │
│     │ Cta: 193-9876543-1-20  │  │ Cta: 0011-0123-0100... │             │
│     │ CCI: 002-193-00987...  │  │ CCI: 011-123-000100... │             │
│     └────────────────────────┘  └────────────────────────┘             │
│                                                                        │
│  4. Datos de la Operación y Comprobante:                               │
│     N° Operación: [ 45892147 ]      Fecha: [ 29/10/2026 ]              │
│                                                                        │
│     ┌────────────────────────────────────────────────────────────┐     │
│     │  Arrastra aquí tu voucher bancario o haz clic para buscar │     │
│     │  Formatos permitidos: PDF, JPG, PNG (Máximo 10 MB)         │     │
│     └────────────────────────────────────────────────────────────┘     │
│                                                                        │
│                                 [ BOTÓN: REGISTRAR ABONO BANCARIO ]    │
└────────────────────────────────────────────────────────────────────────┘
```

- **Presets de Monto en USD:** Permite abonar el 50% de anticipo reglamentario o el 100% total con un solo clic.
- **Validación Estricta de Archivos:** Limita el tamaño del voucher a un máximo de **10 MB** y rechaza archivos que no sean imágenes o PDFs legibles.
- **Resolución Transparente en Backend:** Al enviar el formulario con `multipart/form-data`, el backend asocia automáticamente el pago a la empresa del cliente autenticado, dejando el registro listo para ser validado por el contador.

---

## 5. Centro de Notificaciones y Avisos Operativos (`PortalNotificacionesView.vue`)

Ruta: `/notificaciones`  
Servicio: [`portalNotificationService.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/features/notificaciones/services/portalNotificationService.js)

Funciona como la bandeja de entrada de alertas en tiempo real de la empresa:
- **Eventos Notificados Automáticamente:**
  - *"Su expediente EXP-2026-0045 obtuvo Canal Verde por parte de la SUNAT"*.
  - *"Su contenedor TCLU-482910-2 fue pesado exitosamente en balanza VGM (24.50 TM)"*.
  - *"La unidad de transporte placa T7B-892 inició ruta hacia sus almacenes"*.
  - *"Su abono bancario de $ 3,500.00 fue validado por el área de Finanzas"*.
- **Control de Lectura:** Permite marcar avisos individuales como leídos o usar el botón "Marcar todas como leídas".
- **Enlaces Directos:** Cada notificación incluye un botón de acceso directo que abre el expediente o pago correspondiente.
