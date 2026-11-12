/**
 * Formatea un monto numérico con 2 decimales y separador de miles.
 * Si se indica `currency` ('PEN' o 'USD'), antepone el símbolo monetario correspondiente.
 */
export function formatCurrency(val, currency = '') {
  const num = Number(val)
  const safeNum = val === null || val === undefined || val === '' || isNaN(num) ? 0 : num
  const cur = String(currency || '').toUpperCase()

  if (cur === 'PEN') {
    return 'S/ ' + safeNum.toLocaleString('es-PE', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })
  }

  const formatted = safeNum.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })

  if (cur === 'USD') {
    return `$ ${formatted}`
  }

  return formatted
}

/**
 * Formatea un número con 2 decimales en formato local es-PE.
 */
export function formatNumber(val) {
  if (val === null || val === undefined || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('es-PE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * Formatea un código de régimen aduanero a su etiqueta legible.
 */
export function formatRegimen(reg) {
  if (!reg) return '—'
  const map = {
    IMPORTACION_DEFINITIVA: 'Importación Definitiva',
    EXPORTACION_DEFINITIVA: 'Exportación Definitiva',
    DEPOSITO_ADUANERO: 'Depósito Aduanero'
  }
  return map[reg] || String(reg).replace(/_/g, ' ')
}

/**
 * Formatea un código de estado operativo, financiero o logístico.
 */
export function formatEstado(est) {
  if (!est) return '—'
  const key = String(est).toUpperCase()
  const map = {
    PENDIENTE: 'Pendiente de Cobro',
    PAGADO: 'Cobrado / Conforme',
    ANULADO: 'Anulado',
    PROGRAMADO: 'Programado',
    ASIGNADO: 'Asignado',
    EN_RUTA: 'En Ruta',
    ENTREGADO: 'Entregado',
    INCIDENCIA: 'Con Incidencia',
    CANCELADO: 'Cancelado',
    APERTURADO: 'Aperturado',
    EN_TRANSITO: 'En Tránsito',
    ARRIBADO: 'Arribado',
    EN_DESPACHO: 'En Despacho',
    LEVANTE_AUTORIZADO: 'Levante Autorizado',
    CERRADO: 'Cerrado'
  }
  return map[key] || String(est).replace(/_/g, ' ')
}

/**
 * Retorna la severidad PrimeVue para el canal de control SUNAT.
 */
export function getCanalSeverity(canal) {
  switch (String(canal || '').toUpperCase()) {
    case 'VERDE':
      return 'success'
    case 'NARANJA':
      return 'warn'
    case 'ROJO':
      return 'danger'
    default:
      return 'info'
  }
}

/**
 * Retorna la severidad PrimeVue para estados de entidades SGA (sedes, pagos, despachos, catálogos).
 */
export function getEstadoSeverity(est) {
  switch (String(est || '').toUpperCase()) {
    case 'OPERATIVO':
    case 'ACTIVO':
    case 'PAGADO':
    case 'ENTREGADO':
    case 'DISPONIBLE':
    case 'APROBADA':
    case 'APROBADO':
    case 'LEVANTE_AUTORIZADO':
      return 'success'
    case 'MANTENIMIENTO':
    case 'PENDIENTE':
    case 'PENDIENTE_REVISION':
    case 'PROGRAMADO':
    case 'REVISION':
    case 'OBSERVADA':
    case 'DESCANSO':
      return 'warn'
    case 'INACTIVO':
    case 'ANULADO':
    case 'ANULADA':
    case 'RECHAZADA':
    case 'RECHAZADO':
    case 'OBSERVADO':
    case 'INCIDENCIA':
    case 'CANCELADO':
    case 'SUSPENDIDO':
    case 'FUERA_DE_SERVICIO':
      return 'danger'
    case 'EN_RUTA':
    case 'ASIGNADO':
    case 'EN_TRANSITO':
    case 'EN_REVISION':
    default:
      return 'info'
  }
}

/**
 * Obtiene las iniciales (2 letras) de un nombre completo o par (nombres, apellidos).
 */
export function getInitials(name = '', apellido = '') {
  const first = String(name || '').trim()
  const last = String(apellido || '').trim()
  if (first && last) {
    return `${first.charAt(0)}${last.charAt(0)}`.toUpperCase()
  }
  if (!first) return 'US'
  const parts = first.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) {
    return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase()
  }
  return first.substring(0, 2).toUpperCase()
}

export default {
  formatCurrency,
  formatNumber,
  formatRegimen,
  formatEstado,
  getCanalSeverity,
  getEstadoSeverity,
  getInitials
}
