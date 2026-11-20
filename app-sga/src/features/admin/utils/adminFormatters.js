/**
 * Utilidades de formato y presentación para el módulo Administración TI.
 */

export function formatBytes(bytes = 0) {
  const val = Number(bytes || 0)
  if (val < 1024) return `${val} B`
  if (val < 1024 * 1024) return `${(val / 1024).toFixed(1)} KB`
  if (val < 1024 * 1024 * 1024) return `${(val / (1024 * 1024)).toFixed(2)} MB`
  return `${(val / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

export function formatUptime(seconds = 0) {
  const s = Math.max(0, Number(seconds || 0))
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d > 0) return `${d}d ${h}h ${m}m`
  if (h > 0) return `${h}h ${m}m`
  return `${m}m ${s % 60}s`
}

export function getInitials(nombres = '', apellidos = '', fallback = 'U') {
  const n = String(nombres || '').trim().charAt(0)
  const a = String(apellidos || '').trim().charAt(0)
  return (n + a).toUpperCase() || fallback
}

export function getWarmAvatarStyle(seed = '') {
  let hash = 0
  const text = String(seed || '')
  for (let i = 0; i < text.length; i++) {
    hash = text.charCodeAt(i) + ((hash << 5) - hash)
  }
  const warmHues = [25, 35, 45, 18, 195, 140]
  const hue = warmHues[Math.abs(hash) % warmHues.length]
  return { background: `hsl(${hue}, 45%, 38%)`, color: '#ffffff' }
}
