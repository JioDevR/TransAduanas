import { describe, it, expect, vi, afterEach } from 'vitest'
import {
  formatCurrency,
  formatNumber,
  formatRegimen,
  formatEstado,
  getCanalSeverity,
  getEstadoSeverity,
  getInitials
} from '../formatters'
import {
  formatDate,
  formatDateTime,
  formatDateShort,
  getTodayDateInput,
  getNowDateTimeInput
} from '../dateUtils'

describe('shared/utils/formatters y dateUtils (fechas locales)', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  describe('formatCurrency', () => {
    it('formatea valores numéricos con 2 decimales y soporte opcional de prefijo de moneda', () => {
      expect(formatCurrency(1250.5)).toBe('1,250.50')
      expect(formatCurrency(0)).toBe('0.00')
      expect(formatCurrency(1500, 'PEN')).toBe('S/ 1,500.00')
      expect(formatCurrency(1500, 'USD')).toBe('$ 1,500.00')
    })

    it('retorna 0.00 (o con prefijo) ante valores nulos o inválidos', () => {
      expect(formatCurrency(null)).toBe('0.00')
      expect(formatCurrency(undefined)).toBe('0.00')
      expect(formatCurrency('abc')).toBe('0.00')
      expect(formatCurrency(null, 'PEN')).toBe('S/ 0.00')
    })
  })

  describe('formatNumber', () => {
    it('formatea números con 2 decimales', () => {
      expect(formatNumber(2200)).toMatch(/2[,.]200\.00/)
      expect(formatNumber(0)).toBe('0.00')
      expect(formatNumber(null)).toBe('0.00')
      expect(formatNumber(undefined)).toBe('0.00')
    })
  })

  describe('formatRegimen', () => {
    it('formatea códigos de régimen aduanero', () => {
      expect(formatRegimen('IMPORTACION_DEFINITIVA')).toBe('Importación Definitiva')
      expect(formatRegimen('EXPORTACION_DEFINITIVA')).toBe('Exportación Definitiva')
      expect(formatRegimen('DEPOSITO_ADUANERO')).toBe('Depósito Aduanero')
      expect(formatRegimen('ADMISION_TEMPORAL')).toBe('ADMISION TEMPORAL')
      expect(formatRegimen('')).toBe('—')
      expect(formatRegimen(null)).toBe('—')
    })
  })

  describe('formatEstado y getEstadoSeverity', () => {
    it('formatea estados operativos y financieros', () => {
      expect(formatEstado('EN_RUTA')).toBe('En Ruta')
      expect(formatEstado('ENTREGADO')).toBe('Entregado')
      expect(formatEstado('PENDIENTE')).toBe('Pendiente de Cobro')
      expect(formatEstado('PAGADO')).toBe('Cobrado / Conforme')
      expect(formatEstado(null)).toBe('—')
    })

    it('retorna severidad PrimeVue adecuada para estados', () => {
      expect(getEstadoSeverity('OPERATIVO')).toBe('success')
      expect(getEstadoSeverity('ACTIVO')).toBe('success')
      expect(getEstadoSeverity('PAGADO')).toBe('success')
      expect(getEstadoSeverity('ENTREGADO')).toBe('success')
      expect(getEstadoSeverity('MANTENIMIENTO')).toBe('warn')
      expect(getEstadoSeverity('PENDIENTE')).toBe('warn')
      expect(getEstadoSeverity('INACTIVO')).toBe('danger')
      expect(getEstadoSeverity('ANULADO')).toBe('danger')
      expect(getEstadoSeverity('EN_RUTA')).toBe('info')
    })
  })

  describe('getCanalSeverity', () => {
    it('retorna severidad PrimeVue correcta según canal SUNAT', () => {
      expect(getCanalSeverity('VERDE')).toBe('success')
      expect(getCanalSeverity('NARANJA')).toBe('warn')
      expect(getCanalSeverity('ROJO')).toBe('danger')
      expect(getCanalSeverity('PENDIENTE')).toBe('info')
      expect(getCanalSeverity(null)).toBe('info')
    })
  })

  describe('getInitials', () => {
    it('obtiene iniciales a partir de nombre completo o nombres y apellidos', () => {
      expect(getInitials('Carlos Mendoza')).toBe('CM')
      expect(getInitials('Ana', 'Torres')).toBe('AT')
      expect(getInitials('Admin')).toBe('AD')
      expect(getInitials('')).toBe('US')
      expect(getInitials(null)).toBe('US')
    })
  })

  describe('dateUtils - getTodayDateInput y getNowDateTimeInput sin desfase UTC', () => {
    it('getTodayDateInput devuelve YYYY-MM-DD en hora local incluso pasadas las 19:00 GMT-5', () => {
      // Simulamos 2026-11-12 a las 22:30 hora local (en UTC ya sería 2026-11-13T03:30:00Z)
      const localLateNight = new Date(2026, 10, 12, 22, 30, 0)
      expect(getTodayDateInput(localLateNight)).toBe('2026-11-12')
    })

    it('getNowDateTimeInput devuelve YYYY-MM-DDTHH:mm en hora local sin desfase UTC', () => {
      const localLateNight = new Date(2026, 10, 12, 22, 45, 0)
      expect(getNowDateTimeInput(localLateNight)).toBe('2026-11-12T22:45')
    })

    it('formatDate, formatDateTime y formatDateShort formatean fechas de forma segura', () => {
      expect(formatDate(null)).toBe('—')
      expect(formatDate('2026-11-12')).toContain('2026')
      expect(formatDateTime(null)).toBe('—')
      expect(formatDateShort(null)).toBe('—')
      expect(formatDateShort('2026-11-12')).toBe('12/11/2026')
    })
  })
})
