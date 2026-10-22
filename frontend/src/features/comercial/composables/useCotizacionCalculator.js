/**
 * SGA - Motor de Valoración Aduanera y Liquidación Tarifaria (SUNAT)
 * Composable especializado para cálculo de Valor CIF, Tributos de Importación,
 * Exoneraciones de Exportación y Liquidación Comercial Total.
 */

/**
 * Calcula el Valor CIF (Costo, Seguro y Flete)
 * @param {number} valorFob
 * @param {number} flete
 * @param {number} seguro
 * @returns {number}
 */
export function calcularValorCif(valorFob, flete, seguro) {
  const fob = Math.max(0, Number(valorFob) || 0)
  const flt = Math.max(0, Number(flete) || 0)
  const seg = Math.max(0, Number(seguro) || 0)
  return Number((fob + flt + seg).toFixed(2))
}

/**
 * Calcula el Monto Ad-Valórem (Arancel SUNAT)
 * @param {number} cif
 * @param {number} tasaAdValorem - Porcentaje (ej. 0, 6, 11)
 * @param {string} regimen - 'IMPORTACION_DEFINITIVA' | 'EXPORTACION_DEFINITIVA' etc.
 * @returns {number}
 */
export function calcularMontoAdValorem(cif, tasaAdValorem, regimen = 'IMPORTACION_DEFINITIVA') {
  if (regimen === 'EXPORTACION_DEFINITIVA') return 0
  const tasa = Math.max(0, Number(tasaAdValorem) || 0)
  const baseCif = Math.max(0, Number(cif) || 0)
  return Number((baseCif * (tasa / 100)).toFixed(2))
}

/**
 * Calcula el IGV (16%) + IPM (2%) = 18% acumulado sobre (CIF + Ad-Valórem)
 * @param {number} cif
 * @param {number} montoAdValorem
 * @param {string} regimen
 * @returns {number}
 */
export function calcularMontoIgv(cif, montoAdValorem, regimen = 'IMPORTACION_DEFINITIVA') {
  if (regimen === 'EXPORTACION_DEFINITIVA') return 0
  const base = Math.max(0, Number(cif) || 0) + Math.max(0, Number(montoAdValorem) || 0)
  return Number((base * 0.18).toFixed(2))
}

/**
 * Calcula la Percepción SUNAT sobre (CIF + Ad-Valórem + IGV/IPM)
 * @param {number} cif
 * @param {number} montoAdValorem
 * @param {number} montoIgv
 * @param {number} tasaPercepcion - Porcentaje (ej. 3.5, 5, 10)
 * @param {string} regimen
 * @returns {number}
 */
export function calcularMontoPercepcion(cif, montoAdValorem, montoIgv, tasaPercepcion, regimen = 'IMPORTACION_DEFINITIVA') {
  if (regimen === 'EXPORTACION_DEFINITIVA') return 0
  const tasa = Math.max(0, Number(tasaPercepcion) || 0)
  if (tasa <= 0) return 0
  const base = Math.max(0, Number(cif) || 0) + Math.max(0, Number(montoAdValorem) || 0) + Math.max(0, Number(montoIgv) || 0)
  return Number((base * (tasa / 100)).toFixed(2))
}

/**
 * Calcula la suma total de tributos aduaneros estimados
 * @param {number} adValorem
 * @param {number} igv
 * @param {number} percepcion
 * @returns {number}
 */
export function calcularTotalTributos(adValorem, igv, percepcion) {
  const adv = Math.max(0, Number(adValorem) || 0)
  const tax = Math.max(0, Number(igv) || 0)
  const per = Math.max(0, Number(percepcion) || 0)
  return Number((adv + tax + per).toFixed(2))
}

/**
 * Calcula el Total Cotizado de la Operación
 * @param {Object} params
 * @returns {{ valorCif: number, montoSeguro: number, montoAdValorem: number, montoIgv: number, montoPercepcion: number, montoTributos: number, totalCotizado: number }}
 */
export function calcularLiquidacionIntegral(params) {
  const {
    regimen = 'IMPORTACION_DEFINITIVA',
    valorFob = 0,
    montoFleteEstimado = 0,
    montoSeguroEstimado = 0,
    seguroManual = false,
    montoHonorariosEstimado = 0,
    tasaAdValorem = 0,
    tasaPercepcion = 0,
    servicioTransporte = false,
    costoTransporteInterno = 0
  } = params || {}

  const fob = Math.max(0, Number(valorFob) || 0)
  const flete = Math.max(0, Number(montoFleteEstimado) || 0)
  const honorarios = Math.max(0, Number(montoHonorariosEstimado) || 0)
  const costoTransp = servicioTransporte ? Math.max(0, Number(costoTransporteInterno) || 0) : 0

  if (regimen === 'EXPORTACION_DEFINITIVA') {
    return {
      valorCif: fob,
      montoSeguro: 0,
      montoAdValorem: 0,
      montoIgv: 0,
      montoPercepcion: 0,
      montoTributos: 0,
      totalCotizado: Number((flete + honorarios + costoTransp).toFixed(2))
    }
  }

  // Seguro referencial tabla SUNAT ~0.75% del FOB si no fue editado manualmente
  const seguro = seguroManual ? Math.max(0, Number(montoSeguroEstimado) || 0) : Number((fob * 0.0075).toFixed(2))
  const cif = calcularValorCif(fob, flete, seguro)
  const adValorem = calcularMontoAdValorem(cif, tasaAdValorem, regimen)
  const igv = calcularMontoIgv(cif, adValorem, regimen)
  const percepcion = calcularMontoPercepcion(cif, adValorem, igv, tasaPercepcion, regimen)
  const tributos = calcularTotalTributos(adValorem, igv, percepcion)
  const total = Number((flete + honorarios + tributos + costoTransp).toFixed(2))

  return {
    valorCif: cif,
    montoSeguro: seguro,
    montoAdValorem: adValorem,
    montoIgv: igv,
    montoPercepcion: percepcion,
    montoTributos: tributos,
    totalCotizado: total
  }
}

/**
 * Formateador auxiliar de moneda
 * @param {number} num
 * @param {string} currency
 * @returns {string}
 */
export function formatMoney(num, currency = 'USD') {
  if (num === null || num === undefined || isNaN(num)) return '$0.00'
  const val = Number(num).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return currency === 'USD' ? `$${val}` : `S/ ${val}`
}

/**
 * Composable principal de cálculo para componentes Vue
 */
export function useCotizacionCalculator() {
  return {
    calcularValorCif,
    calcularMontoAdValorem,
    calcularMontoIgv,
    calcularMontoPercepcion,
    calcularTotalTributos,
    calcularLiquidacionIntegral,
    formatMoney
  }
}
