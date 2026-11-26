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

const REGIMEN_FULL_LABELS = {
  IMPORTACION_DEFINITIVA: 'Importación para el Consumo (REG-10)',
  EXPORTACION_DEFINITIVA: 'Exportación Definitiva (REG-40)',
  DEPOSITO_ADUANERO: 'Depósito Aduanero (REG-70)',
  TRANSITO_ADUANERO: 'Tránsito Aduanero (REG-80)',
  ADMISION_TEMPORAL: 'Admisión Temporal (REG-20)'
}

export const formatRegimenFull = (regimen) => {
  if (!regimen) return 'Importación para el Consumo (REG-10)'
  return REGIMEN_FULL_LABELS[regimen] || String(regimen).replace(/_/g, ' ')
}

export const getRegimenCode = (regimen) => {
  if (!regimen) return 'REG-10'
  const val = String(regimen).toUpperCase()
  if (val.includes('20') || val.includes('TEMPORAL')) return 'REG-20'
  if (val.includes('40') || val.includes('EXPORTACION')) return 'REG-40'
  if (val.includes('70') || val.includes('DEPOSITO')) return 'REG-70'
  if (val.includes('80') || val.includes('TRANSITO')) return 'REG-80'
  return 'REG-10'
}

export const formatTipoDespacho = (despacho) => {
  if (!despacho) return 'Despacho Anticipado'
  return String(despacho).replace(/_/g, ' ')
}

export const formatIsoDate = (dateStr) => {
  if (!dateStr) return '—'
  return String(dateStr).split('T')[0]
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

export const getCanalSeverity = (canal) => {
  if (canal === 'VERDE') return 'success'
  if (canal === 'NARANJA') return 'warn'
  if (canal === 'ROJO') return 'danger'
  return 'contrast'
}

export const getExpedienteSeverity = (estado) => {
  if (estado === 'LEVANTE_AUTORIZADO' || estado === 'CONCLUIDO' || estado === 'CERRADO') return 'success'
  if (estado === 'ANULADO') return 'danger'
  if (estado === 'CON_CANAL') return 'warn'
  return 'info'
}

const ESTADO_AUDITORIA_LABELS = {
  ...ESTADO_COTIZACION_LABELS,
  ASIGNADO: 'Expediente Asignado',
  EN_TRAMITE: 'En Trámite SUNAT',
  CON_CANAL: 'Con Canal Asignado',
  LEVANTE_AUTORIZADO: 'Levante Autorizado',
  CERRADO: 'Expediente Cerrado',
  ANULADO: 'Anulado'
}

export const getEstadoAuditoriaLabel = (estado) => {
  return ESTADO_AUDITORIA_LABELS[estado] || estado || 'Pendiente'
}

export const getEstadoAuditoriaSeverity = (estado) => {
  if (['APROBADA', 'LEVANTE_AUTORIZADO', 'CERRADO', 'CONCLUIDO'].includes(estado)) return 'success'
  if (['RECHAZADA', 'ANULADA', 'ANULADO'].includes(estado)) return 'danger'
  if (['REVISION', 'OBSERVADA', 'CON_CANAL'].includes(estado)) return 'warn'
  if (['EN_REVISION', 'ASIGNADO', 'EN_TRAMITE'].includes(estado)) return 'info'
  return 'contrast'
}
