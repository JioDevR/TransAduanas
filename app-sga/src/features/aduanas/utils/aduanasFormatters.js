import { formatCurrency, formatRegimen } from '@/shared/utils/formatters'

/**
 * Formatea un monto numérico USD con 2 decimales (sin símbolo, para anteponer `$`).
 */
export const formatUsd = (val) => formatCurrency(val)

/**
 * Formatea el régimen aduanero en etiqueta corta legible para listados operativos.
 */
export const formatRegimenShort = (regimen) => {
  if (regimen === 'IMPORTACION_DEFINITIVA') return 'Importación (REG-10)'
  if (regimen === 'EXPORTACION_DEFINITIVA') return 'Exportación (REG-40)'
  return formatRegimen(regimen)
}

const ESTADO_COTIZACION_LABELS = {
  ENVIADA: 'Por Revisar (Pendiente)',
  PENDIENTE: 'Por Revisar (Pendiente)',
  EN_REVISION: 'En Evaluación Técnica',
  REVISION: 'Observada (Para Corrección)',
  OBSERVADA: 'Observada (Para Corrección)',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  ANULADA: 'Anulada'
}

export const getEstadoCotizacionLabel = (estado) => {
  return ESTADO_COTIZACION_LABELS[estado] || estado || '—'
}

export const getEstadoCotizacionSeverity = (estado) => {
  if (estado === 'APROBADA') return 'success'
  if (estado === 'RECHAZADA' || estado === 'ANULADA') return 'danger'
  if (estado === 'REVISION' || estado === 'OBSERVADA') return 'warn'
  if (estado === 'EN_REVISION') return 'info'
  return 'contrast'
}

const ESTADO_PAGO_LABELS = {
  ADELANTO_APROBADO: 'Anticipo Aprobado',
  PAGADO_TOTAL: 'Pagado 100%',
  PENDIENTE_REVISION: 'En Revisión Finanzas',
  OBSERVADO: 'Pago Observado'
}

export const getEstadoPagoLabel = (estadoPago) => {
  return ESTADO_PAGO_LABELS[estadoPago] || 'Pendiente de Pago'
}

export const getEstadoPagoSeverity = (estadoPago) => {
  switch (estadoPago) {
    case 'ADELANTO_APROBADO':
    case 'PAGADO_TOTAL':
      return 'success'
    case 'PENDIENTE_REVISION':
      return 'warn'
    case 'OBSERVADO':
      return 'danger'
    default:
      return 'info'
  }
}

export const isCotizacionRevisable = (estado) => {
  return ['PENDIENTE', 'EN_REVISION', 'ENVIADA', 'REVISION', 'OBSERVADA'].includes(estado)
}

export const isCotizacionObservable = (estado) => {
  return ['PENDIENTE', 'EN_REVISION', 'ENVIADA'].includes(estado)
}
