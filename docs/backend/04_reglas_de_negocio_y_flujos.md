# 4. Reglas de Negocio, Compuertas y Flujos Operativos

**Sistema de Gestión Aduanera - Transaduanas**  
*Lógica de Dominio, Cálculos Fiscales y Control de Procesos Aduaneros*

---

## Índice del Capítulo
1. [El Ciclo de Vida Operativo Completo (Visión General)](#1-el-ciclo-de-vida-operativo-completo-visión-general)
2. [Módulo Comercial y Calculadora de Cotizaciones](#2-módulo-comercial-y-calculadora-de-cotizaciones)
   - [2.1 El Flujo Asistido de 4 Pasos](#21-el-flujo-asistido-de-4-pasos)
   - [2.2 Fórmulas de Flete y Estimación de Tributos SUNAT](#22-fórmulas-de-flete-y-estimación-de-tributos-sunat)
3. [Compuerta Financiera Pre-Expediente (Financial Payment Gate)](#3-compuerta-financiera-pre-expediente-financial-payment-gate)
4. [Gestión de Pagos, Anticipos y Deuda Heredada](#4-gestión-de-pagos-anticipos-y-deuda-heredada)
   - [4.1 Registro de Abonos y Subida de Vouchers](#41-registro-de-abonos-y-subida-de-vouchers)
   - [4.2 Consulta de Obligaciones Pendientes (Deuda Heredada)](#42-consulta-de-obligaciones-pendientes-deuda-heredada)
5. [Gestión Aduanera y Trámite DAM (Declaración Aduanera)](#5-gestión-aduanera-y-trámite-dam-declaración-aduanera)
   - [5.1 Numeración y Canales de Control SUNAT (Verde, Naranja, Rojo)](#51-numeración-y-canales-de-control-sunat-verde-naranja-rojo)
   - [5.2 Aprovisionamiento Automático de Credenciales B2B](#52-aprovisionamiento-automático-de-credenciales-b2b)
6. [Depósito Temporal y Balanza Certificada VGM](#6-depósito-temporal-y-balanza-certificada-vgm)
7. [Despacho de Transporte y Flota MTC](#7-despacho-de-transporte-y-flota-mtc)
8. [Liquidación Financiera Final, IGV y SPOT (12%)](#8-liquidación-financiera-final-igv-y-spot-12)
9. [Política de Tolerancia Cero a Borrado Físico (Soft Delete)](#9-política-de-tolerancia-cero-a-borrado-físico-soft-delete)

---

## 1. El Ciclo de Vida Operativo Completo (Visión General)

El sistema sincroniza los 8 roles institucionales en una cadena de valor unificada. Ninguna etapa puede saltarse ni ejecutarse a ciegas:

```
┌─────────────────────────┐
│ 1. RECEPCIÓN COMERCIAL  │  Agente Comercial: Registra cliente y cotiza flete + tributos
└────────────┬────────────┘
             │ Cotización COT-2026-XXXX en estado ENVIADA
             ▼
┌─────────────────────────┐
│ 2. COMPUERTA FINANCIERA │  Contador: Valida voucher de anticipo bancario
└────────────┬────────────┘
             │ ¡PAGO VALIDADO! (Se desbloquea la creación del trámite aduanero)
             ▼
┌─────────────────────────┐
│ 3. GESTIÓN ADUANERA     │  Supervisor Aduanero: Apertura expediente y asigna DAM
└────────────┬────────────┘  SUNAT asigna Canal: VERDE / NARANJA / ROJO
             │
             ├────────────────────────────────────────┐
             ▼                                        ▼
┌─────────────────────────┐              ┌─────────────────────────┐
│ 4. DEPÓSITO TEMPORAL    │              │ 5. ASESORÍA LEGAL       │
│ Pesaje en Báscula VGM   │              │ Valida permisos MTC,    │
│ Verificación de Levante │              │ DIGEMID, SENASA, SUCAMEC│
└────────────┬────────────┘              └────────────┬────────────┘
             │                                        │
             └───────────────────┬────────────────────┘
                                 │ Autorización de Salida de Recinto
                                 ▼
┌─────────────────────────────────────────┐
│ 6. DESPACHO DE TRANSPORTE Y RUTA        │  Supervisor de Transporte: Asigna tracto,
│ Emisión de Guía de Remisión MTC y GPS   │  chofer con brevete profesional y monitorea
└────────────────────┬────────────────────┘
                     │ Carga entregada al cliente final
                     ▼
┌─────────────────────────────────────────┐
│ 7. LIQUIDACIÓN FINANCIERA DEFINITIVA    │  Contador: Concilia gastos reales, tributos,
│ Cálculo de IGV (18%) y SPOT (12%)       │  emite factura final y cierra expediente
└─────────────────────────────────────────┘
```

---

## 2. Módulo Comercial y Calculadora de Cotizaciones

### 2.1 El Flujo Asistido de 4 Pasos
En el backend, el servicio `CotizacionServiceImpl` procesa la creación de cotizaciones a través de un contrato estructurado en 4 etapas:
1. **Paso 1: Datos Generales y Cliente:** RUC/Razón Social del cliente (importador/exportador), régimen (Importación para el Consumo / Exportación Definitiva) y modalidad de transporte (Marítimo / Aéreo).
2. **Paso 2: Ruta y Puertos:** Puerto de origen, puerto de destino (Callao, Paita, Matarani, Chancay, etc.) y fecha estimada de arribo (*ETA*).
3. **Paso 3: Detalle de Carga:** Tipo de carga (FCL 20'/40', LCL, Reefer, Carga Peligrosa IMO), clasificación arancelaria NANDINA (mercancía), peso bruto en toneladas y volumen en $m^3$.
4. **Paso 4: Desglose de Costos y Servicios:** Selección de flete internacional, agenciamiento aduanero, seguro de carga, cuadrilla de estiba, pesaje VGM y flete terrestre local.

### 2.2 Fórmulas de Flete y Estimación de Tributos SUNAT
El backend ejecuta cálculos deterministas en dólares americanos (`USD`):

$$\text{Valor CIF Estimado} = \text{Valor FOB de Mercancía} + \text{Flete Internacional} + \text{Seguro}$$

$$\text{Ad-Valórem} = \text{Valor CIF} \times \% \text{ Arancel NANDINA (0\%, 6\% u 11\%)}$$

$$\text{Base Imponible IGV/IPM} = \text{Valor CIF} + \text{Ad-Valórem} + \text{Otros Derechos}$$

$$\text{Tributos Estimados SUNAT} = \text{Ad-Valórem} + (\text{Base Imponible} \times 18\%)$$

El correlativo se autogenera mediante un algoritmo determinista con bloqueo transaccional: `COT-YYYY-XXXX` (ej. `COT-2026-00015`).

---

## 3. Compuerta Financiera Pre-Expediente (Financial Payment Gate)

> [!CAUTION]
> **Regla de Negocio Crítica:**  
> Ningún usuario, ni siquiera el Gerente General o el Administrador TI, puede abrir un expediente aduanero si la cotización comercial previa no cuenta con un pago formalmente registrado y **en estado `VALIDADO` por el área contable**.

En `ExpedienteServiceImpl.java`, la compuerta se ejecuta antes de cualquier inserción:

```java
// 1. Buscar la cotización vinculada
Cotizacion cotizacion = cotizacionRepository.findById(dto.getIdCotizacion())
    .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada"));

// 2. Verificar que tenga pagos y que al menos uno esté VALIDADO por contabilidad
boolean tienePagoValidado = cotizacionPagoRepository
    .existsByCotizacion_IdCotizacionAndEstadoPago(cotizacion.getIdCotizacion(), EstadoPago.VALIDADO);

if (!tienePagoValidado) {
    throw new BadRequestException("COMPUERTA FINANCIERA BLOQUEADA: La cotización " 
        + cotizacion.getCodigoCotizacion() 
        + " no cuenta con un pago validado por Finanzas/Contador. Trámite no autorizado.");
}
```

Esta compuerta erradica el riesgo de que la empresa asuma costos portuarios o derechos arancelarios por clientes morosos.

---

## 4. Gestión de Pagos, Anticipos y Deuda Heredada

### 4.1 Registro de Abonos y Subida de Vouchers
Los pagos pueden originarse de dos formas:
1. **Registro Interno (Contador):** El contador registra el abono directamente en el módulo de Finanzas tras recibir la confirmación de la cuenta bancaria corporativa.
2. **Registro Externo (Portal del Cliente B2B):** El importador entra a su portal, adjunta el voucher bancario (PDF, PNG o JPG de hasta 10 MB) e ingresa el banco, número de operación y monto.

El backend almacena el comprobante en disco mediante `StorageServiceImpl` y crea el registro en `cotizacion_pago` con estado inicial `PENDIENTE_VALIDACION`. Cuando el contador lo aprueba, el estado cambia a `VALIDADO`, guardando la fecha y el ID del contador auditor.

### 4.2 Consulta de Obligaciones Pendientes (Deuda Heredada)
Recientemente se implementó el endpoint:  
`GET /api/v1/portal/obligaciones-pendientes`

Este servicio calcula la **deuda viva** de un cliente B2B agrupando:
- **Cotizaciones con saldo pendiente:** Monto total cotizado menos los abonos validados.
- **Liquidaciones emitidas no pagadas:** Facturas de gastos operativos y tributos SUNAT emitidas tras la entrega de la carga que aún tienen saldo por cobrar.

Esto permite que el cliente B2B visualice en una sola pantalla todas sus obligaciones financieras consolidadas sin tener que cruzar documentos manualmente.

---

## 5. Gestión Aduanera y Trámite DAM (Declaración Aduanera)

### 5.1 Numeración y Canales de Control SUNAT (Verde, Naranja, Rojo)
Una vez superada la compuerta financiera, el **Supervisor Aduanero** genera el expediente (`EXP-YYYY-XXXX`). Al transmitirse la información telemática a la SUNAT, se obtiene la numeración oficial de la DAM (Declaración Aduanera de Mercancías) y el semáforo aduanero de control aleatorio:

| Canal SUNAT | Significado Operativo | Procedimiento en el Sistema |
| :---: | :--- | :--- |
| **VERDE** | Levante Automático | La mercancía no requiere revisión física ni documentaria. El sistema autoriza directamente la salida de almacén y pase a transporte. |
| **NARANJA** | Revisión Documentaria | Un especialista de la aduana revisa facturas, BL y certificados. El sistema exige adjuntar el acta de conformidad documentaria antes de autorizar el levante. |
| **ROJO** | Reconocimiento Físico | La aduana inspecciona físicamente el contenedor en depósito temporal. El sistema exige registrar la fecha de aforo físico y el acta de inspección sin incidencias. |

### 5.2 Aprovisionamiento Automático de Credenciales B2B
Cuando un expediente es aprobado para un cliente nuevo que nunca ha utilizado la plataforma, el backend ejecuta automáticamente una rutina de bienvenida:
1. Verifica si el cliente ya tiene una cuenta en la tabla `usuario_cliente`.
2. Si no existe, genera una cuenta de acceso al **Portal del Cliente B2B** utilizando el RUC de la empresa como nombre de usuario y una contraseña temporal segura con bandera `debe_cambiar_password = true`.
3. Retorna en la respuesta del expediente las credenciales generadas para que el operador se las entregue al cliente importador.

---

## 6. Depósito Temporal y Balanza Certificada VGM

El módulo de almacén (`ControlAlmacenController` y `ControlAlmacenServiceImpl`) garantiza el cumplimiento de la enmienda **SOLAS** (*Safety of Life at Sea*) de la Organización Marítima Internacional:
- **Tara del Contenedor:** Peso propio del contenedor vacío (registrado en placa CSC).
- **Peso Neto de la Carga:** Peso de la mercancía con sus paletas y embalaje.
- **Masa Bruta Verificada (VGM):** $\text{VGM} = \text{Peso de Mercancía} + \text{Tara del Contenedor}$.
- **Ticket de Báscula Certificada:** Se registra el número de ticket de pesaje, la balanza fiscal utilizada y la fecha/hora exacta de pesaje. Si la diferencia de peso supera el margen de tolerancia aduanero, el sistema emite una alerta de discrepancia de peso.

---

## 7. Despacho de Transporte y Flota MTC

El módulo de transporte (`DespachoTransporteController` / `DespachoTransporteServiceImpl`) gobierna la operación de la flota pesada (TMS):
1. **Validación de Conductor MTC:** Verifica que el chofer asignado tenga brevete profesional vigente (A-IIIa, A-IIIb o A-IIIc) y no registre sanciones graves.
2. **Asignación de Unidad:** Vincula el tracto-camión y la carreta/semirremolque con su respectivo Certificado de Inspección Técnica Vehicular (CITV) y póliza de seguro de carga.
3. **Generación de Correlativo:** Emite el despacho oficial con formato `DSP-YYYY-XXXX`.
4. **Emisión de Guía de Remisión:** Genera los datos para la Guía de Remisión Transportista electrónica requerida por SUNAT para el tránsito por carreteras nacionales.
5. **Tracking de Estados:** Controla la secuencia `PROGRAMADO` $\rightarrow$ `EN_RUTA` $\rightarrow$ `ENTREGADO`.

---

## 8. Liquidación Financiera Final, IGV y SPOT (12%)

Al concluir la entrega física, el **Contador** procesa la liquidación financiera (`LiquidacionFinancieraController`):
- Consolida los tributos reales pagados a SUNAT (aranceles, IGV/IPM, percepción).
- Consolida los gastos de agenciamiento, almacenaje, sobrestadía y transporte local.
- Aplica el **Sistema de Pago de Obligaciones Tributarias (SPOT - Detracciones SUNAT)**:
  - En servicios de transporte de carga y agenciamiento aduanero sujetos al sistema de detracciones en Perú, se aplica la tasa legal obligatoria del **12%** cuando el importe de la operación supera los S/ 700.
  - El backend calcula de forma desglosada el Monto Neto a Pagar y el Monto de la Constancia de Detracción a abonar en la cuenta del Banco de la Nación.

---

## 9. Política de Tolerancia Cero a Borrado Físico (Soft Delete)

En el comercio exterior peruano, una empresa de aduanas debe conservar expedientes, facturas y guías por un mínimo de **5 años** para efectos de auditorías y fiscalizaciones de la SUNAT.

Por esta razón, **ningún controlador ni servicio del backend ejecuta sentencias SQL `DELETE`**:
- Clientes, usuarios, choferes y camiones tienen una columna booleana `estado_activo`. Cuando se da de baja un registro, se ejecuta un `UPDATE tabla SET estado_activo = false`.
- Cotizaciones, expedientes y liquidaciones manejan estados de ciclo de vida (`ANULADO`, `CANCELADO`, `INACTIVO`).
- Los repositorios aplican filtros automáticos (`WHERE estado_activo = true`) para no mostrar registros dados de baja en las pantallas habituales, pero manteniendo intacta la integridad referencial y las auditorías históricas.
