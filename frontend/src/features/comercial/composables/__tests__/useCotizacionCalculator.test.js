import { describe, it, expect } from 'vitest'
import {
  calcularValorCif,
  calcularMontoAdValorem,
  calcularMontoIgv,
  calcularMontoPercepcion,
  calcularTotalTributos,
  calcularLiquidacionIntegral,
  formatMoney
} from '../useCotizacionCalculator'

describe('useCotizacionCalculator', () => {
  describe('calcularValorCif', () => {
    it('debe calcular CIF sumando FOB, Flete y Seguro correctamente', () => {
      const cif = calcularValorCif(10000, 1500, 75)
      expect(cif).toBe(11575.00)
    })

    it('debe manejar valores nulos o no numéricos como 0', () => {
      const cif = calcularValorCif(null, undefined, 'abc')
      expect(cif).toBe(0)
    })
  })

  describe('calcularMontoAdValorem', () => {
    it('debe calcular 6% de Ad-Valórem sobre CIF para importación', () => {
      const adv = calcularMontoAdValorem(10000, 6, 'IMPORTACION_DEFINITIVA')
      expect(adv).toBe(600.00)
    })

    it('debe retornar 0 para exportación definitiva', () => {
      const adv = calcularMontoAdValorem(10000, 6, 'EXPORTACION_DEFINITIVA')
      expect(adv).toBe(0)
    })
  })

  describe('calcularMontoIgv', () => {
    it('debe calcular el 18% (16% IGV + 2% IPM) sobre base CIF + Ad-Valórem', () => {
      // CIF: 10,000, Ad-Valórem: 600 -> Base: 10,600 * 0.18 = 1908
      const igv = calcularMontoIgv(10000, 600, 'IMPORTACION_DEFINITIVA')
      expect(igv).toBe(1908.00)
    })

    it('debe retornar 0 para exportación definitiva', () => {
      const igv = calcularMontoIgv(10000, 600, 'EXPORTACION_DEFINITIVA')
      expect(igv).toBe(0)
    })
  })

  describe('calcularMontoPercepcion', () => {
    it('debe calcular 3.5% de Percepción sobre (CIF + Ad-Valórem + IGV)', () => {
      // CIF: 10,000, Adv: 600, IGV: 1908 -> Base: 12508 * 0.035 = 437.78
      const per = calcularMontoPercepcion(10000, 600, 1908, 3.5, 'IMPORTACION_DEFINITIVA')
      expect(per).toBe(437.78)
    })

    it('debe retornar 0 si la tasa de percepción es 0 o negativa', () => {
      const per = calcularMontoPercepcion(10000, 600, 1908, 0, 'IMPORTACION_DEFINITIVA')
      expect(per).toBe(0)
    })
  })

  describe('calcularTotalTributos', () => {
    it('debe sumar Ad-Valórem, IGV y Percepción', () => {
      const total = calcularTotalTributos(600, 1908, 437.78)
      expect(total).toBe(2945.78)
    })
  })

  describe('calcularLiquidacionIntegral', () => {
    it('debe calcular liquidación completa de importación con seguro automático SUNAT (0.75%)', () => {
      const resultado = calcularLiquidacionIntegral({
        regimen: 'IMPORTACION_DEFINITIVA',
        valorFob: 20000,
        montoFleteEstimado: 2500,
        montoHonorariosEstimado: 350,
        tasaAdValorem: 6,
        tasaPercepcion: 3.5,
        servicioTransporte: true,
        costoTransporteInterno: 400
      })

      // Seguro auto: 20000 * 0.0075 = 150
      expect(resultado.montoSeguro).toBe(150.00)
      // CIF = 20000 + 2500 + 150 = 22650
      expect(resultado.valorCif).toBe(22650.00)
      // Ad-Valórem 6% de 22650 = 1359.00
      expect(resultado.montoAdValorem).toBe(1359.00)
      // IGV 18% de (22650 + 1359 = 24009) = 4321.62
      expect(resultado.montoIgv).toBe(4321.62)
      // Percepción 3.5% de (24009 + 4321.62 = 28330.62) = 991.57
      expect(resultado.montoPercepcion).toBe(991.57)
      // Tributos = 1359 + 4321.62 + 991.57 = 6672.19
      expect(resultado.montoTributos).toBe(6672.19)
      // Total Cotizado = Flete (2500) + Honorarios (350) + Tributos (6672.19) + Transporte (400) = 9922.19
      expect(resultado.totalCotizado).toBe(9922.19)
    })

    it('debe liquidar exportación sin tributos aduaneros', () => {
      const resultado = calcularLiquidacionIntegral({
        regimen: 'EXPORTACION_DEFINITIVA',
        valorFob: 50000,
        montoFleteEstimado: 3000,
        montoHonorariosEstimado: 500,
        tasaAdValorem: 6,
        tasaPercepcion: 3.5,
        servicioTransporte: false,
        costoTransporteInterno: 0
      })

      expect(resultado.montoTributos).toBe(0)
      expect(resultado.montoAdValorem).toBe(0)
      expect(resultado.montoIgv).toBe(0)
      // Total = Flete (3000) + Honorarios (500) = 3500
      expect(resultado.totalCotizado).toBe(3500.00)
    })
  })

  describe('formatMoney', () => {
    it('debe formatear montos en USD y PEN adecuadamente', () => {
      expect(formatMoney(12500.5, 'USD')).toBe('$12,500.50')
      expect(formatMoney(12500.5, 'PEN')).toBe('S/ 12,500.50')
      expect(formatMoney(null, 'USD')).toBe('$0.00')
    })
  })
})
