import { formatCurrency } from './formatters'

const DEFAULT_PORTAL_URL = import.meta.env?.VITE_PORTAL_URL || 'http://localhost:5174'
const DEFAULT_TEMP_PASSWORD = 'Password123!'

const ESTADO_RESUMEN_LABELS = {
  APROBADA: 'Aprobada',
  OBSERVADA: 'Observada (requiere subsanación)',
  REVISION: 'Observada (requiere subsanación)',
  PENDIENTE: 'En revisión técnica',
  ENVIADA: 'En revisión técnica',
  EN_REVISION: 'En evaluación técnica',
  RECHAZADA: 'Rechazada',
  ANULADA: 'Anulada'
}

/**
 * Normaliza un número telefónico de Perú para enlaces wa.me.
 *
 * @param {string} rawPhone
 * @returns {string}
 */
export function formatPeruWhatsAppPhone(rawPhone) {
  const digits = String(rawPhone || '').replace(/[^0-9]/g, '')
  if (!digits) return ''
  if (digits.startsWith('51') && digits.length >= 11) return digits
  return `51${digits}`
}

/**
 * Genera un mensaje resumido de WhatsApp con el estado de la cotización
 * y, si es el primer ingreso al Portal B2B, incluye las credenciales temporales.
 *
 * @param {Object} cotizacion
 * @param {{ portalUrl?: string }} [options]
 * @returns {string}
 */
export function buildCotizacionWhatsAppMessage(cotizacion = {}, { portalUrl = DEFAULT_PORTAL_URL } = {}) {
  const cliente = cotizacion?.clienteRazonSocial || 'Estimado Cliente'
  const codigo = cotizacion?.codigoCotizacion || (cotizacion?.idCotizacion ? `COT-${cotizacion.idCotizacion}` : 'S/C')
  const estado = String(cotizacion?.estado || 'PENDIENTE').toUpperCase()
  const estadoLabel = ESTADO_RESUMEN_LABELS[estado] || estado
  const total = formatCurrency(cotizacion?.totalCotizado || 0)

  const lines = [
    `Hola *${cliente}*, le saluda *TransAduanas SGA*.`,
    `Resumen de su cotización *${codigo}*: Estado *${estadoLabel}* | Total: *$${total} USD*.`
  ]

  if (['OBSERVADA', 'REVISION'].includes(estado) && cotizacion?.motivoObservacion) {
    lines.push(`📌 *Observación:* ${cotizacion.motivoObservacion}`)
  } else if (estado === 'RECHAZADA' && cotizacion?.motivoObservacion) {
    lines.push(`📌 *Motivo:* ${cotizacion.motivoObservacion}`)
  }

  if (estado === 'APROBADA' && cotizacion?.clienteTieneUsuario) {
    if (cotizacion?.clienteDebeCambiarPassword) {
      const username = cotizacion?.clienteNumeroDocumento || 'su RUC'
      lines.push(
        `🔐 *Acceso Portal B2B (Primer ingreso):* Usuario: *${username}* | Clave temporal: *${DEFAULT_TEMP_PASSWORD}* | Portal: ${portalUrl} _(deberá cambiar su clave al ingresar)_.`
      )
    } else {
      lines.push(`🔗 Consulte su trámite en el Portal B2B: ${portalUrl}`)
    }
  }

  return lines.join('\n')
}

/**
 * Construye el enlace de WhatsApp y abre una nueva pestaña con el mensaje prellenado.
 *
 * @param {Object} cotizacion
 * @param {{ fallbackPhone?: string, portalUrl?: string }} [options]
 * @returns {{ ok: boolean, phone?: string, message?: string, url?: string, reason?: string }}
 */
export function openCotizacionWhatsApp(cotizacion, { fallbackPhone = '', portalUrl } = {}) {
  if (!cotizacion) {
    return { ok: false, reason: 'Seleccione una cotización.' }
  }

  const phone = formatPeruWhatsAppPhone(cotizacion.clienteTelefono || fallbackPhone)
  if (!phone) {
    return { ok: false, reason: 'El cliente no tiene registrado un número de teléfono o celular.' }
  }

  const message = buildCotizacionWhatsAppMessage(cotizacion, { portalUrl })
  const url = `https://wa.me/${phone}?text=${encodeURIComponent(message)}`

  if (typeof window !== 'undefined' && typeof window.open === 'function') {
    window.open(url, '_blank', 'noopener')
  }

  return { ok: true, phone, message, url }
}
