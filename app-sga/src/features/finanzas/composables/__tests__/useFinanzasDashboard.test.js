import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useFinanzasDashboard } from '../useFinanzasDashboard'

describe('useFinanzasDashboard', () => {
  it('calcula métricas, embudo y balance financiero con filtro de período', () => {
    const cotizacionesAprobadas = ref([
      { idCotizacion: 1, fechaEmision: '2026-11-24', totalCotizado: 1000, totalAbonado: 0, saldoPendiente: 1000, estadoPago: 'SIN_PAGO' },
      { idCotizacion: 2, fechaEmision: '2026-11-20', totalCotizado: 2000, totalAbonado: 600, saldoPendiente: 1400, estadoPago: 'ADELANTO_APROBADO', habilitadoParaExpediente: true },
      { idCotizacion: 3, fechaEmision: '2026-10-01', totalCotizado: 500, totalAbonado: 500, saldoPendiente: 0, estadoPago: 'PAGADO_TOTAL', habilitadoParaExpediente: true }
    ])
    const anticiposList = ref([
      { idPago: 10, fechaOperacion: '2026-11-24', estadoPago: 'PENDIENTE_REVISION' }
    ])
    const liquidacionesList = ref([
      { idLiquidacion: 1, fechaEmision: '2026-11-24', estadoPago: 'PAGADO', totalGeneralPen: 1500, totalGeneralUsd: 400, totalTributosSunat: 500, montoDetraccion: 120 },
      { idLiquidacion: 2, fechaEmision: '2026-10-05', estadoPago: 'PENDIENTE', totalGeneralPen: 800, totalGeneralUsd: 213.33, totalTributosSunat: 200, montoDetraccion: 72 }
    ])
    const expedientesPorLiquidar = ref([{ idExpediente: 99 }])
    const metricas = ref({
      totalCobradoPen: 1500,
      cuentasPorCobrarPen: 800,
      totalTributosSunatPen: 700,
      totalDetraccionesPen: 192,
      totalLiquidaciones: 2,
      liquidacionesPendientes: 1,
      liquidacionesPagadas: 1,
      tipoCambioOficial: 3.75
    })

    const dash = useFinanzasDashboard({
      cotizacionesAprobadas,
      anticiposList,
      liquidacionesList,
      expedientesPorLiquidar,
      metricas,
      referenceDate: new Date(2026, 10, 24)
    })

    expect(dash.anticiposPendientesCount.value).toBe(1)
    expect(dash.expedientesPorLiquidarCount.value).toBe(1)
    expect(dash.tasaRecaudacionCarteraPct.value).toBe(31) // 1100 / 3500 = 31.4%
    expect(dash.cotizacionesPrioritarias.value).toHaveLength(2)

    dash.filterPeriodo.value = 'HOY'
    expect(dash.filteredCotizaciones.value).toHaveLength(1)
    expect(dash.liquidacionesStats.value.cobradoPen).toBe(1500)
    expect(dash.liquidacionesStats.value.porCobrarPen).toBe(0)
  })
})
