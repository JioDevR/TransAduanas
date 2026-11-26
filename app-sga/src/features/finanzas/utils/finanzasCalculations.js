export const DEFAULT_TIPO_CAMBIO = 3.75
export const IGV_RATE = 0.18
export const SPOT_MIN_THRESHOLD_PEN = 700
export const APERTURA_MIN_THRESHOLD_PCT = 30

/**
 * Redondea un valor numérico a 2 decimales con precisión comercial.
 */
export function round2(val) {
  const num = Number(val || 0)
  if (!Number.isFinite(num)) return 0
  return Math.round((num + Number.EPSILON) * 100) / 100
}

/**
 * Calcula los totales de una liquidación aduanera replicando exactamente
 * las reglas tributarias del backend (IGV 18% y Detracción SPOT 12% >= S/ 700.00).
 */
export function calculateLiquidacionTotals({
  totalTributosSunat = 0,
  gastosAlmacenPuerto = 0,
  fleteTransporteLocal = 0,
  honorariosAgencia = 0,
  tipoCambio = DEFAULT_TIPO_CAMBIO,
  aplicaDetraccion = true,
  porcentajeDetraccion = 12
} = {}) {
  const tributos = Math.max(0, Number(totalTributosSunat || 0))
  const almacen = Math.max(0, Number(gastosAlmacenPuerto || 0))
  const flete = Math.max(0, Number(fleteTransporteLocal || 0))
  const honorarios = Math.max(0, Number(honorariosAgencia || 0))
  const tc = Number(tipoCambio) > 0 ? Number(tipoCambio) : DEFAULT_TIPO_CAMBIO

  const subtotalImponible = round2(honorarios + almacen + flete)
  const igvMonto = round2(subtotalImponible * IGV_RATE)
  const totalGeneralPen = round2(tributos + subtotalImponible + igvMonto)
  const totalGeneralUsd = tc > 0 ? round2(totalGeneralPen / tc) : 0

  const pctDetraccion = Math.max(0, Number(porcentajeDetraccion ?? 12))
  const superaUmbralSpot = totalGeneralPen >= SPOT_MIN_THRESHOLD_PEN
  const montoDetraccion =
    aplicaDetraccion && superaUmbralSpot
      ? round2((subtotalImponible + igvMonto) * (pctDetraccion / 100))
      : 0

  const netoCuentaCorrientePen = round2(Math.max(0, totalGeneralPen - montoDetraccion))

  return {
    subtotalImponible,
    igvMonto,
    totalGeneralPen,
    totalGeneralUsd,
    superaUmbralSpot,
    montoDetraccion,
    netoCuentaCorrientePen
  }
}

/**
 * Convierte un monto en PEN o USD a su equivalente en USD usando el tipo de cambio indicado.
 */
export function convertAmountToUsd(monto, moneda = 'USD', tipoCambio = DEFAULT_TIPO_CAMBIO) {
  const amount = Number(monto || 0)
  if (!Number.isFinite(amount) || amount <= 0) return 0
  if (String(moneda).toUpperCase() === 'USD') return round2(amount)
  const tc = Number(tipoCambio) > 0 ? Number(tipoCambio) : DEFAULT_TIPO_CAMBIO
  return round2(amount / tc)
}

/**
 * Convierte un monto en PEN o USD a su equivalente en PEN usando el tipo de cambio indicado.
 */
export function convertAmountToPen(monto, moneda = 'USD', tipoCambio = DEFAULT_TIPO_CAMBIO) {
  const amount = Number(monto || 0)
  if (!Number.isFinite(amount) || amount <= 0) return 0
  if (String(moneda).toUpperCase() === 'PEN') return round2(amount)
  const tc = Number(tipoCambio) > 0 ? Number(tipoCambio) : DEFAULT_TIPO_CAMBIO
  return round2(amount * tc)
}

/**
 * Calcula el balance proyectado y el porcentaje de cobertura de una cotización
 * al registrar un nuevo abono bancario.
 */
export function calculateCotizacionBalance(
  cotizacion,
  nuevoAbono = 0,
  moneda = 'USD',
  tipoCambio = DEFAULT_TIPO_CAMBIO
) {
  const totalCotizado = Math.max(0, Number(cotizacion?.totalCotizado || 0))
  const totalAbonado = Math.max(0, Number(cotizacion?.totalAbonado || 0))
  const saldoActual =
    cotizacion?.saldoPendiente != null
      ? Math.max(0, Number(cotizacion.saldoPendiente))
      : round2(Math.max(0, totalCotizado - totalAbonado))

  const nuevoAbonoUsd = convertAmountToUsd(nuevoAbono, moneda, tipoCambio)
  const saldoProyectado = round2(Math.max(0, saldoActual - nuevoAbonoUsd))
  const excedeSaldo = saldoActual > 0 && nuevoAbonoUsd > saldoActual + 0.01

  const coberturaActualPct = totalCotizado > 0 ? Math.min(100, Math.round((totalAbonado / totalCotizado) * 100)) : 0
  const coberturaProyectadaPct =
    totalCotizado > 0 ? Math.min(100, Math.round(((totalAbonado + nuevoAbonoUsd) / totalCotizado) * 100)) : 0
  const coberturaProyectadaExacta =
    totalCotizado > 0 ? ((totalAbonado + nuevoAbonoUsd) / totalCotizado) * 100 : 0

  return {
    totalCotizado,
    totalAbonado,
    saldoActual,
    nuevoAbonoUsd,
    saldoProyectado,
    excedeSaldo,
    coberturaActualPct,
    coberturaProyectadaPct,
    cumpleUmbralApertura: coberturaProyectadaExacta >= APERTURA_MIN_THRESHOLD_PCT
  }
}

/**
 * Consolida indicadores de cartera de cotizaciones aprobadas.
 */
export function summarizeCotizacionesCartera(cotizaciones = []) {
  const list = Array.isArray(cotizaciones) ? cotizaciones : []
  let totalCotizadoUsd = 0
  let totalAbonadoUsd = 0
  let totalSaldoPendienteUsd = 0
  let sinAbonoCount = 0
  let enRevisionCount = 0
  let adelantoConformeCount = 0
  let pagadoTotalCount = 0
  let habilitadasCount = 0
  let conSaldoPendienteCount = 0

  for (const c of list) {
    const cotizado = Number(c.totalCotizado || 0)
    const abonado = Number(c.totalAbonado || 0)
    const saldo = Number(c.saldoPendiente != null ? c.saldoPendiente : cotizado)

    totalCotizadoUsd += cotizado
    totalAbonadoUsd += abonado
    totalSaldoPendienteUsd += saldo

    if (saldo > 0.01) conSaldoPendienteCount += 1
    if (!abonado || abonado <= 0) sinAbonoCount += 1
    if (c.estadoPago === 'PENDIENTE_REVISION') enRevisionCount += 1
    if (c.estadoPago === 'ADELANTO_APROBADO' || c.tienePagoAprobado) adelantoConformeCount += 1
    if (c.estadoPago === 'PAGADO_TOTAL') pagadoTotalCount += 1
    if (c.habilitadoParaExpediente) habilitadasCount += 1
  }

  return {
    totalCotizadoUsd: round2(totalCotizadoUsd),
    totalAbonadoUsd: round2(totalAbonadoUsd),
    totalSaldoPendienteUsd: round2(totalSaldoPendienteUsd),
    sinAbonoCount,
    enRevisionCount,
    adelantoConformeCount,
    pagadoTotalCount,
    habilitadasCount,
    conSaldoPendienteCount
  }
}
