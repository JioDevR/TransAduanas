import { describe, it, expect } from 'vitest'
import {
  parseLocalDate,
  getLocalDateString,
  formatHumanDate,
  formatHumanShort,
  calculateTransitDays,
  canSwapMonthDay,
  swapMonthDay,
  addDaysToDate
} from '../dateUtils'

describe('dateUtils - Utilidades de Fechas y Cronogramas Logísticos', () => {
  it('parseLocalDate parsea fechas YYYY-MM-DD sin desfase horario', () => {
    const d = parseLocalDate('2026-11-12')
    expect(d).not.toBeNull()
    expect(d.getFullYear()).toBe(2026)
    expect(d.getMonth()).toBe(10) // Noviembre (0-indexed)
    expect(d.getDate()).toBe(12)
  })

  it('calculateTransitDays calcula diferencia positiva, 0 (mismo día) y negativa', () => {
    // 21 días (12 Nov a 03 Dic)
    expect(calculateTransitDays('2026-11-12', '2026-12-03')).toBe(21)

    // Mismo día (Vuelo internacional express)
    expect(calculateTransitDays('2026-11-12', '2026-11-12')).toBe(0)

    // 1 día después
    expect(calculateTransitDays('2026-11-12', '2026-11-13')).toBe(1)

    // Invertido (Error usuario: 11 Dic a 12 Mar)
    expect(calculateTransitDays('2026-12-11', '2026-03-12')).toBe(-274)

    // Valores nulos o vacíos
    expect(calculateTransitDays('', '2026-12-03')).toBeNull()
    expect(calculateTransitDays('2026-11-12', null)).toBeNull()
  })

  it('formatHumanDate formatea en español legible', () => {
    const formatted = formatHumanDate('2026-11-12')
    expect(formatted.toLowerCase()).toContain('12 de noviembre de 2026')
    expect(formatted.toLowerCase()).toContain('jueves')
  })

  it('formatHumanShort formatea en formato DD/MM/YYYY', () => {
    expect(formatHumanShort('2026-11-12')).toBe('12/11/2026')
  })

  it('canSwapMonthDay y swapMonthDay corrigen la inversión de mes y día', () => {
    // 2026-12-11 (Diciembre 11) invertido -> 2026-11-12 (Noviembre 12)
    expect(canSwapMonthDay('2026-12-11')).toBe(true)
    expect(swapMonthDay('2026-12-11')).toBe('2026-11-12')

    // 2026-03-12 (Marzo 12) invertido -> 2026-12-03 (Diciembre 3)
    expect(canSwapMonthDay('2026-03-12')).toBe(true)
    expect(swapMonthDay('2026-03-12')).toBe('2026-12-03')

    // Si el día es > 12 (ej. 25 de Noviembre: 2026-11-25), no se puede invertir porque mes 25 no existe
    expect(canSwapMonthDay('2026-11-25')).toBe(false)
    expect(swapMonthDay('2026-11-25')).toBe('2026-11-25')
  })

  it('addDaysToDate suma días correctamente a una fecha', () => {
    expect(addDaysToDate('2026-11-12', 0)).toBe('2026-11-12')
    expect(addDaysToDate('2026-11-12', 1)).toBe('2026-11-13')
    expect(addDaysToDate('2026-11-12', 21)).toBe('2026-12-03')
  })
})
