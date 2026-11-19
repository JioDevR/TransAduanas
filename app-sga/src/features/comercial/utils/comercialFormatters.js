import { formatCurrency } from '@/shared/utils/formatters'

/**
 * Formatea un monto numérico USD con 2 decimales (sin símbolo, para anteponer `$`).
 */
export const formatUsd = (val) => formatCurrency(val)

/**
 * Formatea el régimen aduanero en etiqueta corta legible.
 */
export const formatRegimenShort = (regimen) => {
  if (regimen === 'IMPORTACION_DEFINITIVA') return 'Importación (REG-10)'
  if (regimen === 'EXPORTACION_DEFINITIVA') return 'Exportación (REG-40)'
  return regimen || '-'
}

/**
 * Determina si una cotización está en estado de observación/revisión técnica.
 */
export const isCotizacionObservada = (cotizacion) => {
  return cotizacion?.estado === 'REVISION' || cotizacion?.estado === 'OBSERVADA'
}
