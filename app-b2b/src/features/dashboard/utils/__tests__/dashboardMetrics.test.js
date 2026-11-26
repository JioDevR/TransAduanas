import { describe, it, expect } from 'vitest'
import {
  computeCotizacionStats,
  computeExpedienteStats,
  computeCobrosStats,
  formatEstadoExpediente,
  getExpedienteStatusClass
} from '../dashboardMetrics'

describe('dashboardMetrics', () => {
  describe('computeCotizacionStats', () => {
    it('agrupa estados, calcula tasa de aprobación, monto total y ordena recientes', () => {
      const cotizaciones = [
        { idCotizacion: 1, codigoCotizacion: 'COT-01', estado: 'PENDIENTE', totalCotizado: 1000, fechaEmision: '2026-11-01' },
        { idCotizacion: 2, codigoCotizacion: 'COT-02', estado: 'APROBADA', aceptadaPorCliente: false, totalCotizado: 2500, fechaEmision: '2026-11-15' },
        { idCotizacion: 3, codigoCotizacion: 'COT-03', estado: 'APROBADA', aceptadaPorCliente: true, totalCotizado: 1500, fechaEmision: '2026-11-18' },
        { idCotizacion: 4, codigoCotizacion: 'COT-04', estado: 'OBSERVADA', totalCotizado: 500, fechaEmision: '2026-11-10' },
        { idCotizacion: 5, codigoCotizacion: 'COT-05', estado: 'RECHAZADA', totalCotizado: 200, fechaEmision: '2026-11-05' }
      ]

      const stats = computeCotizacionStats(cotizaciones)

      expect(stats.total).toBe(5)
      expect(stats.pendiente).toBe(1)
      expect(stats.aprobada).toBe(2)
      expect(stats.observada).toBe(1)
      expect(stats.rechazada).toBe(1)
      expect(stats.aprobadasPorAceptar).toBe(1)
      expect(stats.aprobacionPct).toBe(40)
      expect(stats.totalCotizadoUsd).toBe(5700)
      expect(stats.recientes.map(c => c.codigoCotizacion)).toEqual(['COT-03', 'COT-02', 'COT-04', 'COT-05'])
    })

    it('retorna ceros de forma segura para arreglos vacíos o nulos', () => {
      const stats = computeCotizacionStats(null)
      expect(stats.total).toBe(0)
      expect(stats.aprobacionPct).toBe(0)
      expect(stats.recientes).toEqual([])
    })
  })

  describe('computeExpedienteStats', () => {
    it('calcula abiertos, levantes, etapas, canales SUNAT y alertas de almacén', () => {
      const expedientes = [
        {
          idExpediente: 1,
          codigoExpediente: 'EXP-01',
          estadoExpediente: 'EN_TRAMITE',
          canalSunat: 'NARANJA',
          diasRestantesAlmacen: 2,
          fechaApertura: '2026-11-12T10:00:00'
        },
        {
          idExpediente: 2,
          codigoExpediente: 'EXP-02',
          estadoExpediente: 'LEVANTE_AUTORIZADO',
          canalSunat: 'VERDE',
          tieneTransporte: true,
          estadoTransporte: 'SOLICITADO',
          fechaApertura: '2026-11-18T10:00:00'
        },
        {
          idExpediente: 3,
          codigoExpediente: 'EXP-03',
          estadoExpediente: 'CERRADO',
          canalSunat: 'ROJO',
          fechaLevanteAutorizacion: '2026-11-05T12:00:00',
          diasRestantesAlmacen: 1,
          fechaApertura: '2026-11-01T10:00:00'
        }
      ]

      const stats = computeExpedienteStats(expedientes)

      expect(stats.total).toBe(3)
      expect(stats.abiertos).toBe(2)
      expect(stats.cerrados).toBe(1)
      expect(stats.conLevante).toBe(2)
      expect(stats.enTransporte).toBe(1)
      expect(stats.alertaAlmacen).toBe(1)
      expect(stats.porEstado.EN_TRAMITE).toBe(1)
      expect(stats.porEstado.LEVANTE_AUTORIZADO).toBe(1)
      expect(stats.porEstado.CERRADO).toBe(1)
      expect(stats.canales).toEqual({ VERDE: 1, NARANJA: 1, ROJO: 1, PENDIENTE: 0 })
      expect(stats.recientes[0].codigoExpediente).toBe('EXP-02')
    })
  })

  describe('computeCobrosStats', () => {
    it('consolida totales USD, progreso de amortización y clasifica comprobantes', () => {
      const stats = computeCobrosStats({
        resumenPagos: {
          totalCotizadoUsd: 4000,
          totalAbonadoUsd: 3000,
          saldoPendienteUsd: 1000
        },
        obligaciones: [
          { idReferencia: 1, saldoPendienteUsd: 1000 },
          { idReferencia: 2, saldoPendienteUsd: 0 }
        ],
        anticipos: [
          { idPago: 1, estadoPago: 'ADELANTO_APROBADO' },
          { idPago: 2, estadoPago: 'PENDIENTE_REVISION' },
          { idPago: 3, estadoPago: 'OBSERVADO' }
        ]
      })

      expect(stats.totalCotizadoUsd).toBe(4000)
      expect(stats.totalAbonadoUsd).toBe(3000)
      expect(stats.saldoPendienteUsd).toBe(1000)
      expect(stats.amortizacionPct).toBe(75)
      expect(stats.pagosValidados).toBe(1)
      expect(stats.pagosEnRevision).toBe(1)
      expect(stats.pagosObservados).toBe(1)
      expect(stats.totalObligacionesPendientes).toBe(1)
      expect(stats.proximasObligaciones).toHaveLength(1)
    })

    it('calcula totales desde obligaciones si resumenPagos es nulo', () => {
      const stats = computeCobrosStats({
        resumenPagos: null,
        obligaciones: [
          { totalContratadoUsd: 1000, totalAmortizadoUsd: 500, saldoPendienteUsd: 500 }
        ],
        anticipos: []
      })

      expect(stats.totalCotizadoUsd).toBe(1000)
      expect(stats.totalAbonadoUsd).toBe(500)
      expect(stats.saldoPendienteUsd).toBe(500)
      expect(stats.amortizacionPct).toBe(50)
    })
  })

  describe('formatEstadoExpediente y getExpedienteStatusClass', () => {
    it('formatea etiquetas y clases semánticas correctamente', () => {
      expect(formatEstadoExpediente('LEVANTE_AUTORIZADO')).toBe('Levante Autorizado')
      expect(formatEstadoExpediente('EN_TRAMITE')).toBe('En Trámite')
      expect(getExpedienteStatusClass('LEVANTE_AUTORIZADO')).toBe('success')
      expect(getExpedienteStatusClass('INSPECCION')).toBe('warning')
      expect(getExpedienteStatusClass('ANULADO')).toBe('danger')
    })
  })
})
