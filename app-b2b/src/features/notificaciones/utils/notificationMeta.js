export const NOTIFICATION_TYPES_META = {
  ACCESO_CREADO: {
    icon: 'pi pi-key',
    color: 'accent',
    label: 'Credencial de Acceso'
  },
  SEGURIDAD: {
    icon: 'pi pi-lock',
    color: 'accent',
    label: 'Seguridad'
  },
  COTIZACION_APROBADA: {
    icon: 'pi pi-check-circle',
    color: 'success',
    label: 'Cotización Aprobada'
  },
  COTIZACION_OBSERVADA: {
    icon: 'pi pi-exclamation-triangle',
    color: 'danger',
    label: 'Cotización Observada'
  },
  DAM: {
    icon: 'pi pi-box',
    color: 'info',
    label: 'Despacho DAM'
  },
  TRANSPORTE: {
    icon: 'pi pi-truck',
    color: 'info',
    label: 'Transporte'
  },
  PAGO: {
    icon: 'pi pi-credit-card',
    color: 'warning',
    label: 'Cobros y Pagos'
  },
  SISTEMA: {
    icon: 'pi pi-bell',
    color: 'neutral',
    label: 'Aviso del Sistema'
  }
}

const DEFAULT_META = NOTIFICATION_TYPES_META.SISTEMA

/**
 * Devuelve la configuración visual (ícono, tono semántico y etiqueta) de un tipo de notificación.
 * @param {string|null|undefined} tipo
 * @returns {{ icon: string, color: string, label: string }}
 */
export function getNotificationMeta(tipo) {
  if (!tipo) return DEFAULT_META
  const normalized = String(tipo).trim().toUpperCase()
  return NOTIFICATION_TYPES_META[normalized] || DEFAULT_META
}

/**
 * Normaliza enlaces heredados o inválidos del backend a rutas válidas del router B2B.
 * @param {string|null|undefined} enlace
 * @returns {string|null}
 */
export function normalizeNotificationLink(enlace) {
  if (!enlace || typeof enlace !== 'string') return null
  const trimmed = enlace.trim()
  if (!trimmed) return null

  // Compatibilidad con enlaces heredados de versiones previas del backend
  if (trimmed.startsWith('/portal-b2b')) {
    return '/expedientes'
  }

  // La vista de página completa de notificaciones fue reemplazada por el menú desplegable
  if (trimmed === '/notificaciones' || trimmed.startsWith('/notificaciones?')) {
    return null
  }

  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

/**
 * Formatea una fecha ISO a tiempo relativo compacto en español.
 * @param {string|Date|null|undefined} iso
 * @param {Date} [now]
 * @returns {string}
 */
export function formatRelativeTime(iso, now = new Date()) {
  if (!iso) return '—'
  const date = iso instanceof Date ? iso : new Date(iso)
  if (isNaN(date.getTime())) return String(iso)

  const diffMs = now.getTime() - date.getTime()
  if (diffMs < 0) return 'Ahora mismo'

  const diffMinutes = Math.floor(diffMs / 60000)
  if (diffMinutes < 1) return 'Ahora mismo'
  if (diffMinutes < 60) return `Hace ${diffMinutes} min`

  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) return `Hace ${diffHours} h`

  const diffDays = Math.floor(diffHours / 24)
  if (diffDays === 1) return 'Ayer'
  if (diffDays < 7) return `Hace ${diffDays} d`

  return date.toLocaleDateString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}
