import { describe, it, expect } from 'vitest'
import {
  REGIMEN_OPTIONS,
  DESCRIPCIONES_TIPO_CARGA_FALLBACK,
  CATEGORIAS_DOCUMENTO,
  normalizeSearchText,
  matchesSmartFilter,
  createDefaultPortalSolicitudForm,
  validatePortalCotizacionForm,
  buildPortalSolicitudPayload,
  esPendienteLiquidacion
} from '../portalCotizacionFormUtils'

describe('portalCotizacionFormUtils', () => {
  describe('constantes y diccionarios', () => {
    it('expone opciones de régimen aduanero válidas', () => {
      expect(REGIMEN_OPTIONS).toHaveLength(2)
      expect(REGIMEN_OPTIONS.map((r) => r.value)).toEqual([
        'IMPORTACION_DEFINITIVA',
        'EXPORTACION_DEFINITIVA'
      ])
    })

    it('expone fallbacks de tipos de carga marítimos y aéreos', () => {
      expect(DESCRIPCIONES_TIPO_CARGA_FALLBACK.CARGA_FCL).toBeDefined()
      expect(DESCRIPCIONES_TIPO_CARGA_FALLBACK.CARGA_AEREA_SUELTA).toBeDefined()
    })

    it('expone categorías documentales requeridas para el portal B2B', () => {
      const valores = Array.isArray(CATEGORIAS_DOCUMENTO)
        ? CATEGORIAS_DOCUMENTO.map((c) => (typeof c === 'string' ? c : c.value))
        : Object.keys(CATEGORIAS_DOCUMENTO)

      expect(valores).toContain('GENERAL')
      expect(valores).toContain('FACTURA')
      expect(valores).toContain('PACKING_LIST')
      expect(valores).toContain('TRANSPORTE_BL')
      expect(valores).toContain('CERTIFICADO')
      expect(valores).toContain('PERMISO_ESPECIAL')
      expect(valores).toContain('IDENTIFICACION')
    })
  })

  describe('validación y normalización', () => {
    it('bloquea envío si valorFob es cero o negativo y retorna step 3', () => {
      const form = createDefaultPortalSolicitudForm()
      form.valorFob = 0
      const res = validatePortalCotizacionForm(form)
      expect(res.valid).toBe(false)
      expect(res.step).toBe(3)
      expect(res.error).toContain('FOB')

      form.valorFob = -50
      const resNeg = validatePortalCotizacionForm(form)
      expect(resNeg.valid).toBe(false)
      expect(resNeg.step).toBe(3)
      expect(resNeg.error).toContain('FOB')
    })

    it('bloquea envío si montoFleteEstimado es negativo y retorna step 3', () => {
      const form = createDefaultPortalSolicitudForm()
      form.valorFob = 5000
      form.montoFleteEstimado = -10
      const res = validatePortalCotizacionForm(form)
      expect(res.valid).toBe(false)
      expect(res.step).toBe(3)
    })

    it('valida que régimen aduanero esté presente en step 1', () => {
      const form = createDefaultPortalSolicitudForm()
      form.valorFob = 5000
      form.regimenAduanero = ''
      const res = validatePortalCotizacionForm(form)
      expect(res.valid).toBe(false)
      expect(res.step).toBe(1)
    })

    it('valida que validezDias esté entre 1 y 90 en step 1', () => {
      const form = createDefaultPortalSolicitudForm()
      form.valorFob = 5000
      form.validezDias = 0
      expect(validatePortalCotizacionForm(form).valid).toBe(false)
      expect(validatePortalCotizacionForm(form).step).toBe(1)

      form.validezDias = 91
      expect(validatePortalCotizacionForm(form).valid).toBe(false)
      expect(validatePortalCotizacionForm(form).step).toBe(1)

      form.validezDias = 30
      expect(validatePortalCotizacionForm(form).valid).toBe(true)
    })

    it('permite flete internacional vacío o cero normalizándolo a 0 en el payload', () => {
      const form = createDefaultPortalSolicitudForm()
      form.valorFob = 15000
      form.montoFleteEstimado = null
      expect(validatePortalCotizacionForm(form).valid).toBe(true)
      const payload = buildPortalSolicitudPayload(form)
      expect(payload.valorFob).toBe(15000)
      expect(payload.montoFleteEstimado).toBe(0)
      expect(payload.validezDias).toBe(15)
    })

    it('bloquea envío si servicioTransporte es true y falta departamento o provincia y retorna step 1', () => {
      const form = createDefaultPortalSolicitudForm()
      form.valorFob = 10000
      form.servicioTransporte = true
      form.origenDepartamento = ''
      const res = validatePortalCotizacionForm(form)
      expect(res.valid).toBe(false)
      expect(res.step).toBe(1)
    })

    it('limpia campos de Ubigeo a null en buildPortalSolicitudPayload cuando servicioTransporte es false', () => {
      const form = createDefaultPortalSolicitudForm()
      form.valorFob = 8500
      form.montoFleteEstimado = 1200
      form.servicioTransporte = false
      form.origenDepartamento = 'CALLAO'
      form.destinoProvincia = 'Lima (Metropolitana)'
      const payload = buildPortalSolicitudPayload(form)
      expect(payload.servicioTransporte).toBe(false)
      expect(payload.origenDepartamento).toBeNull()
      expect(payload.origenProvincia).toBeNull()
      expect(payload.destinoDepartamento).toBeNull()
      expect(payload.destinoProvincia).toBeNull()
      expect(payload.montoFleteEstimado).toBe(1200)
    })

    it('detecta cotizaciones pendientes de liquidación cuando falta asesor o totalCotizado <= 0', () => {
      expect(esPendienteLiquidacion({ idUsuarioComercial: null, totalCotizado: 0 })).toBe(true)
      expect(esPendienteLiquidacion({ idUsuarioComercial: 4, totalCotizado: 0 })).toBe(true)
      expect(esPendienteLiquidacion({ idUsuarioComercial: null, totalCotizado: 950 })).toBe(true)
      expect(esPendienteLiquidacion({ idUsuarioComercial: 4, totalCotizado: 950 })).toBe(false)
    })

    it('filtra sin tildes ni mayúsculas con matchesSmartFilter', () => {
      expect(matchesSmartFilter('CALLAO MUELLE SUR (PERÚ)', 'peru muelle')).toBe(true)
      expect(matchesSmartFilter('Aeropuerto Jorge Chávez', 'chavez aero')).toBe(true)
      expect(matchesSmartFilter('Shanghái', 'shanghai')).toBe(true)
      expect(matchesSmartFilter('Callao', 'miami')).toBe(false)
    })

    it('maneja valores vacíos en normalizeSearchText y matchesSmartFilter', () => {
      expect(normalizeSearchText(null)).toBe('')
      expect(normalizeSearchText(undefined)).toBe('')
      expect(matchesSmartFilter('Callao', '')).toBe(true)
      expect(matchesSmartFilter(null, 'test')).toBe(false)
    })
  })
})
