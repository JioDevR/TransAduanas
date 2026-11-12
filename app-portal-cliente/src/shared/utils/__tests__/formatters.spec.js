import { describe, it, expect, vi } from 'vitest'
import {
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
} from '../formatters'
import { esPendienteLiquidacion } from '@/features/cotizaciones/utils/portalCotizacionFormUtils'

describe('shared/utils/formatters', () => {
  describe('formatCurrency', () => {
    it('formatea montos numéricos a 2 decimales con separador de miles', () => {
      expect(formatCurrency(1250.5)).toBe('1,250.50')
      expect(formatCurrency(0)).toBe('0.00')
      expect(formatCurrency('9800.1')).toBe('9,800.10')
    })

    it('retorna 0.00 ante valores nulos o inválidos', () => {
      expect(formatCurrency(null)).toBe('0.00')
      expect(formatCurrency(undefined)).toBe('0.00')
      expect(formatCurrency('')).toBe('0.00')
      expect(formatCurrency('abc')).toBe('0.00')
    })
  })

  describe('formatRegimen', () => {
    it('traduce códigos de régimen aduanero a etiquetas legibles', () => {
      expect(formatRegimen('IMPORTACION_DEFINITIVA')).toBe('Importación Definitiva')
      expect(formatRegimen('EXPORTACION_DEFINITIVA')).toBe('Exportación Definitiva')
    })

    it('maneja valores vacíos o regímenes genéricos', () => {
      expect(formatRegimen('')).toBe('')
      expect(formatRegimen(null)).toBe('')
      expect(formatRegimen('ADMISION_TEMPORAL')).toBe('ADMISION TEMPORAL')
    })
  })

  describe('formatEstadoCotizacion y formatEstadoPago', () => {
    it('formatea estados de cotización y pago reemplazando guiones bajos', () => {
      expect(formatEstadoCotizacion('APROBADA')).toBe('APROBADA')
      expect(formatEstadoCotizacion(null)).toBe('PENDIENTE')
      expect(formatEstadoPago('SIN_PAGO')).toBe('SIN PAGO')
      expect(formatEstadoPago('PENDIENTE_REVISION')).toBe('EN REVISIÓN')
      expect(formatEstadoPago('ADELANTO_APROBADO')).toBe('ADELANTO VALIDADO')
      expect(formatEstadoPago('OBSERVADO')).toBe('PAGO OBSERVADO')
      expect(formatEstadoPago('PAGADO_PARCIAL')).toBe('PAGADO PARCIAL')
    })
  })

  describe('getCotizacionStatusClass y getPagoStatusClass', () => {
    it('retorna la clase semántica correcta para estados de cotización', () => {
      expect(getCotizacionStatusClass('APROBADA')).toBe('success')
      expect(getCotizacionStatusClass('PENDIENTE')).toBe('warning')
      expect(getCotizacionStatusClass('OBSERVADA')).toBe('danger')
      expect(getCotizacionStatusClass('RECHAZADA')).toBe('danger')
      expect(getCotizacionStatusClass('DESCONOCIDO')).toBe('neutral')
    })

    it('retorna la clase semántica correcta para estados de pago', () => {
      expect(getPagoStatusClass('PAGADO_TOTAL')).toBe('success')
      expect(getPagoStatusClass('CONCILIADO')).toBe('success')
      expect(getPagoStatusClass('ADELANTO_APROBADO')).toBe('info')
      expect(getPagoStatusClass('PENDIENTE_REVISION')).toBe('info')
      expect(getPagoStatusClass('PENDIENTE')).toBe('warning')
      expect(getPagoStatusClass('OBSERVADO')).toBe('danger')
      expect(getPagoStatusClass('RECHAZADO')).toBe('danger')
      expect(getPagoStatusClass(null)).toBe('neutral')
    })
  })

  describe('formatBytes', () => {
    it('formatea tamaños en bytes, KB, MB y GB', () => {
      expect(formatBytes(0)).toBe('0 B')
      expect(formatBytes(null)).toBe('0 B')
      expect(formatBytes(512)).toBe('512 B')
      expect(formatBytes(2048)).toBe('2.0 KB')
      expect(formatBytes(1048576)).toBe('1.0 MB')
    })
  })

  describe('formatDate y formatDateTime', () => {
    it('formatea fechas ISO y maneja valores vacíos', () => {
      expect(formatDate(null)).toBe('—')
      expect(formatDate('2026-11-12T15:30:00Z')).toBe('2026-11-12')
      expect(formatDateTime(null)).toBe('—')
      expect(formatDateTime('2026-11-12T15:30:00Z')).toContain('2026')
    })
  })

  describe('validarArchivoVoucher', () => {
    it('acepta archivos válidos de hasta 10MB en PDF, JPEG, PNG o WEBP', () => {
      const validPdf = { name: 'voucher.pdf', size: 2 * 1024 * 1024, type: 'application/pdf' }
      const validPng = { name: 'voucher.png', size: 500 * 1024, type: 'image/png' }
      expect(validarArchivoVoucher(validPdf)).toBe(true)
      expect(validarArchivoVoucher(validPng)).toBe(true)
    })

    it('rechaza archivos mayores a 10MB o con MIME no permitido', () => {
      const warnSpy = vi.fn()
      const oversizedFile = {
        name: 'huge.pdf',
        size: 10 * 1024 * 1024 + 1,
        type: 'application/pdf'
      }
      const invalidMimeFile = {
        name: 'script.exe',
        size: 1024,
        type: 'application/x-msdownload'
      }

      expect(validarArchivoVoucher(null, warnSpy)).toBe(false)
      expect(validarArchivoVoucher(oversizedFile, warnSpy)).toBe(false)
      expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('10 MB'))

      warnSpy.mockClear()
      expect(validarArchivoVoucher(invalidMimeFile, warnSpy)).toBe(false)
      expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('PDF, PNG, JPG o WEBP'))
    })
  })

  describe('esPendienteLiquidacion (consistencia lista y detalle)', () => {
    it('evalúa consistentemente si una cotización está pendiente de liquidación comercial', () => {
      expect(esPendienteLiquidacion(null)).toBe(true)
      expect(esPendienteLiquidacion({ idUsuarioComercial: null, totalCotizado: 1500, estado: 'APROBADA' })).toBe(true)
      expect(esPendienteLiquidacion({ idUsuarioComercial: 3, totalCotizado: 0, estado: 'PENDIENTE' })).toBe(true)
      expect(esPendienteLiquidacion({ idUsuarioComercial: 3, totalCotizado: 1500, estado: 'APROBADA' })).toBe(false)
    })
  })
})
