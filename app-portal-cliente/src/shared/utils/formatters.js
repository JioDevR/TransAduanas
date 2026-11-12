export const MAX_FILE_SIZE = 10 * 1024 * 1024
export const ALLOWED_MIME_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp']

/**
 * Formatea un monto numérico como moneda con 2 decimales y separador de miles ('1,250.50').
 * @param {number|string|null|undefined} val
 * @returns {string}
 */
export function formatCurrency(val) {
  if (val === null || val === undefined || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * Formatea el código de régimen aduanero en etiqueta legible.
 * @param {string|null|undefined} reg
 * @returns {string}
 */
export function formatRegimen(reg) {
  if (!reg) return ''
  if (reg === 'IMPORTACION_DEFINITIVA') return 'Importación Definitiva'
  if (reg === 'EXPORTACION_DEFINITIVA') return 'Exportación Definitiva'
  return String(reg).replace(/_/g, ' ')
}

/**
 * Formatea el estado comercial de una cotización.
 * @param {string|null|undefined} est
 * @returns {string}
 */
export function formatEstadoCotizacion(est) {
  if (!est) return 'PENDIENTE'
  return String(est).replace(/_/g, ' ')
}

/**
 * Formatea el estado de pago o abono financiero.
 * @param {string|null|undefined} est
 * @returns {string}
 */
export function formatEstadoPago(est) {
  if (!est) return 'PENDIENTE'
  if (est === 'SIN_PAGO') return 'SIN PAGO'
  if (est === 'PENDIENTE_REVISION') return 'EN REVISIÓN'
  if (est === 'ADELANTO_APROBADO') return 'ADELANTO VALIDADO'
  if (est === 'OBSERVADO') return 'PAGO OBSERVADO'
  return String(est).replace(/_/g, ' ')
}

/**
 * Retorna la clase de color semántica para el estado de una cotización.
 * @param {string|null|undefined} est
 * @returns {'success'|'warning'|'danger'|'neutral'}
 */
export function getCotizacionStatusClass(est) {
  switch (est) {
    case 'APROBADA':
      return 'success'
    case 'PENDIENTE':
      return 'warning'
    case 'OBSERVADA':
    case 'RECHAZADA':
      return 'danger'
    default:
      return 'neutral'
  }
}

/**
 * Retorna la clase de color semántica para el estado de pago.
 * @param {string|null|undefined} est
 * @returns {'success'|'info'|'warning'|'danger'|'neutral'}
 */
export function getPagoStatusClass(est) {
  switch (est) {
    case 'PAGADO':
    case 'CONCILIADO':
    case 'PAGADO_TOTAL':
      return 'success'
    case 'ADELANTO_APROBADO':
    case 'PENDIENTE_REVISION':
      return 'info'
    case 'PENDIENTE':
    case 'REGISTRADO':
    case 'PAGADO_PARCIAL':
      return 'warning'
    case 'OBSERVADO':
    case 'RECHAZADO':
      return 'danger'
    default:
      return 'neutral'
  }
}

/**
 * Formatea un tamaño en bytes a representación legible (B, KB, MB, GB).
 * @param {number|null|undefined} bytes
 * @returns {string}
 */
export function formatBytes(bytes) {
  if (!bytes || isNaN(bytes) || Number(bytes) <= 0) return '0 B'
  const b = Number(bytes)
  const k = 1024
  if (b < k) return `${b} B`
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.min(Math.floor(Math.log(b) / Math.log(k)), sizes.length - 1)
  return `${(b / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`
}

/**
 * Formatea una cadena ISO a fecha corta YYYY-MM-DD.
 * @param {string|Date|null|undefined} iso
 * @returns {string}
 */
export function formatDate(iso) {
  if (!iso) return '—'
  const str = String(iso)
  if (/^\d{4}-\d{2}-\d{2}/.test(str)) {
    return str.substring(0, 10)
  }
  try {
    const d = new Date(iso)
    if (isNaN(d.getTime())) return str
    return d.toISOString().substring(0, 10)
  } catch {
    return str
  }
}

/**
 * Formatea una cadena ISO a fecha y hora localizada es-PE.
 * @param {string|Date|null|undefined} iso
 * @returns {string}
 */
export function formatDateTime(iso) {
  if (!iso) return '—'
  try {
    const d = new Date(iso)
    if (isNaN(d.getTime())) return String(iso)
    return d.toLocaleString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return String(iso)
  }
}

/**
 * Valida que un archivo de comprobante/voucher cumpla con tamaño máximo (10 MB) y MIME permitido.
 * @param {File|{ size: number, type: string }|null|undefined} file
 * @param {(msg: string) => void} [onWarn]
 * @returns {boolean}
 */
export function validarArchivoVoucher(file, onWarn) {
  if (!file) return false
  if (file.size > MAX_FILE_SIZE) {
    if (typeof onWarn === 'function') {
      onWarn('El archivo excede el tamaño máximo permitido de 10 MB.')
    }
    return false
  }
  if (!ALLOWED_MIME_TYPES.includes(file.type)) {
    if (typeof onWarn === 'function') {
      onWarn('Solo se permiten archivos en formato PDF, PNG, JPG o WEBP.')
    }
    return false
  }
  return true
}

export default {
  MAX_FILE_SIZE,
  ALLOWED_MIME_TYPES,
  formatCurrency,
  formatRegimen,
  formatEstadoCotizacion,
  formatEstadoPago,
  getCotizacionStatusClass,
  getPagoStatusClass,
  formatBytes,
  formatDate,
  formatDateTime,
  validarArchivoVoucher
}
