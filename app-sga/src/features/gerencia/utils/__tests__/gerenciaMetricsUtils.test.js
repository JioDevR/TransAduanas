import { describe, it, expect } from 'vitest'
import { computeGerenciaDashboardMetrics } from '../gerenciaMetricsUtils'

describe('gerenciaMetricsUtils', () => {
  it('calcula métricas ejecutivas reales de los 4 catálogos maestros', () => {
    const metrics = computeGerenciaDashboardMetrics({
      sedes: [
        { idSede: 1, estado: 'OPERATIVO', distrito: 'Callao' },
        { idSede: 2, estado: 'MANTENIMIENTO', distrito: 'Lima' },
        { idSede: 3, estado: 'OPERATIVO', distrito: 'Callao' }
      ],
      puertos: [
        { idPuerto: 1, tipo: 'MARITIMO', estado: 'ACTIVO' },
        { idPuerto: 2, tipo: 'AEREO', estado: 'ACTIVO' },
        { idPuerto: 3, tipo: 'TERRESTRE', estado: 'INACTIVO' }
      ],
      tiposCarga: [
        { idTipoCarga: 1, codigo: 'IMP-FCL', estado: 'ACTIVO' },
        { idTipoCarga: 2, codigo: 'EXP-LCL', estado: 'INACTIVO' }
      ],
      mercancias: [
        { idMercancia: 1, estado: 'ACTIVO', requierePermiso: true, arancelBasePct: 6 },
        { idMercancia: 2, estado: 'ACTIVO', requierePermiso: false, arancelBasePct: 0 },
        { idMercancia: 3, estado: 'INACTIVO', requierePermiso: false, arancelBasePct: 11 }
      ]
    })

    expect(metrics.sedesCount).toBe(3)
    expect(metrics.sedesOperativasCount).toBe(2)
    expect(metrics.distritosCount).toBe(2)

    expect(metrics.puertosCount).toBe(3)
    expect(metrics.puertosActivosCount).toBe(2)
    expect(metrics.puertosMaritimosCount).toBe(1)
    expect(metrics.puertosAereosCount).toBe(1)

    expect(metrics.cargasCount).toBe(2)
    expect(metrics.cargasActivasCount).toBe(1)

    expect(metrics.mercanciasCount).toBe(3)
    expect(metrics.mercanciasActivasCount).toBe(2)
    expect(metrics.mercanciasVuceCount).toBe(1)

    expect(metrics.coberturaChartData.datasets[0].data).toEqual([3, 3, 2, 3])
    expect(metrics.disponibilidadChartData.datasets[0].data).toEqual([2, 2, 1, 2])
  })

  it('soporta entradas vacías o nulas sin lanzar errores', () => {
    const metrics = computeGerenciaDashboardMetrics({})
    expect(metrics.sedesCount).toBe(0)
    expect(metrics.mercanciasCount).toBe(0)
    expect(metrics.coberturaChartData.datasets[0].data).toEqual([0, 0, 0, 0])
  })
})
