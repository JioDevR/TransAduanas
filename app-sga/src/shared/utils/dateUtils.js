const MESES_ES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'setiembre', 'octubre', 'noviembre', 'diciembre'
]

const DIAS_ES = [
  'domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'
]

const pad = (n) => String(n).padStart(2, '0')

/**
 * Parsea una fecha YYYY-MM-DD a un objeto Date local (a las 00:00:00 locales).
 * Evita el desfase por UTC de new Date("YYYY-MM-DD").
 */
export function parseLocalDate(dateStr) {
  if (!dateStr) return null
  const str = String(dateStr).trim().substring(0, 10)
  const parts = str.split('-')
  if (parts.length === 3) {
    const y = parseInt(parts[0], 10)
    const m = parseInt(parts[1], 10) - 1
    const d = parseInt(parts[2], 10)
    if (!isNaN(y) && !isNaN(m) && !isNaN(d)) {
      return new Date(y, m, d)
    }
  }
  const fallback = new Date(dateStr)
  return isNaN(fallback.getTime()) ? null : fallback
}

/**
 * Retorna la fecha local en formato YYYY-MM-DD.
 * Corrige el bug de new Date().toISOString() que devuelve el día siguiente en horas de la noche (GMT-5).
 */
export function getLocalDateString(date = new Date()) {
  const d = date instanceof Date ? date : new Date(date)
  if (isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/**
 * Formatea una fecha YYYY-MM-DD a texto legible en español.
 * Ej: "2026-11-12" -> "Jueves, 12 de noviembre de 2026"
 */
export function formatHumanDate(dateStr, { includeWeekday = true } = {}) {
  const d = parseLocalDate(dateStr)
  if (!d) return ''
  const dia = d.getDate()
  const mes = MESES_ES[d.getMonth()]
  const anio = d.getFullYear()
  if (includeWeekday) {
    const diaSemana = DIAS_ES[d.getDay()]
    const capSemana = diaSemana.charAt(0).toUpperCase() + diaSemana.slice(1)
    return `${capSemana}, ${dia} de ${mes} de ${anio}`
  }
  return `${dia} de ${mes} de ${anio}`
}

/**
 * Formato corto latinoamericano DD/MM/YYYY.
 */
export function formatHumanShort(dateStr) {
  const d = parseLocalDate(dateStr)
  if (!d) return ''
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

/**
 * Calcula los días de diferencia entre dos fechas YYYY-MM-DD.
 * Retorna entero (positivo, 0 para mismo día, negativo si fin < inicio).
 */
export function calculateTransitDays(dateStartStr, dateEndStr) {
  if (!dateStartStr || !dateEndStr) return null
  const d1 = parseLocalDate(dateStartStr)
  const d2 = parseLocalDate(dateEndStr)
  if (!d1 || !d2 || isNaN(d1.getTime()) || isNaN(d2.getTime())) return null
  const diffTime = d2.getTime() - d1.getTime()
  return Math.round(diffTime / (1000 * 3600 * 24))
}

/**
 * Determina si una fecha YYYY-MM-DD tiene mes y día <= 12 e invertibles.
 * Útil para detectar cuando un navegador en inglés confunde DD/MM con MM/DD.
 */
export function canSwapMonthDay(dateStr) {
  if (!dateStr) return false
  const parts = String(dateStr).trim().split('-')
  if (parts.length !== 3) return false
  const m = parseInt(parts[1], 10)
  const d = parseInt(parts[2], 10)
  return m <= 12 && d <= 12 && m !== d
}

/**
 * Invierte el mes y el día en una fecha YYYY-MM-DD.
 * Ej: "2026-12-11" -> "2026-11-12"
 */
export function swapMonthDay(dateStr) {
  if (!canSwapMonthDay(dateStr)) return dateStr
  const parts = String(dateStr).trim().split('-')
  return `${parts[0]}-${pad(parts[2])}-${pad(parts[1])}`
}

/**
 * Suma N días a una fecha YYYY-MM-DD y retorna la nueva fecha YYYY-MM-DD.
 */
export function addDaysToDate(dateStr, days = 0) {
  const d = parseLocalDate(dateStr)
  if (!d) return ''
  d.setDate(d.getDate() + days)
  return getLocalDateString(d)
}
