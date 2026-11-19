import { ref } from 'vue'
import { describe, it, expect } from 'vitest'
import { resolvePeriodoRange, useComercialDashboard } from '../useComercialDashboard'

describe('useComercialDashboard', () => {
  const refDate = new Date(2026, 10, 18) // 2026-11-18

  const sampleCotizaciones = () =>
    ref([
      {
        idCotizacion: 1,
        codigoCotizacion: 'COT-001',
        fechaEmision: '2026-11-17',
        validezDias: 15,
        estado: 'APROBADA',
        regimenAduanero: 'IMPORTACION_DEFINITIVA',
        idUsuarioComercial: 10,
        usuarioComercialNombre: 'Carlos Perez',
        totalCotizado: 5000,
        totalAbonado: 2000,
        saldoPendiente: 3000
      },
      {
        idCotizacion: 2,
        codigoCotizacion: 'COT-002',
        fechaEmision: '2026-11-16',
        validezDias: 15,
        estado: 'EN_REVISION',
        regimenAduanero: 'IMPORTACION_DEFINITIVA',
        idUsuarioComercial: 10,
        usuarioComercialNombre: 'Carlos Perez',
        totalCotizado: 3000,
        totalAbonado: 0,
        saldoPendiente: 3000
      },
      {
        idCotizacion: 3,
        codigoCotizacion: 'COT-003',
        fechaEmision: '2026-11-01',
        validezDias: 15, // vence 2026-11-16 -> ya vencida respecto a 2026-11-18
        estado: 'OBSERVADA',
        regimenAduanero: 'EXPORTACION_DEFINITIVA',
        idUsuarioComercial: 20,
        usuarioComercialNombre: 'Lucia Gomez',
        totalCotizado: 2000,
        totalAbonado: 0,
        saldoPendiente: 2000
      },
      {
        idCotizacion: 4,
        codigoCotizacion: 'COT-004',
        fechaEmision: '2026-10-05',
        validezDias: 15,
        estado: 'ANULADA',
        regimenAduanero: 'EXPORTACION_DEFINITIVA',
        idUsuarioComercial: 20,
        usuarioComercialNombre: 'Lucia Gomez',
        totalCotizado: 1000,
        totalAbonado: 0,
        saldoPendiente: 0
      }
    ])

  it('calcula correctamente KPIs y estados incluyendo EN_REVISION y OBSERVADA', () => {
    const cotizaciones = sampleCotizaciones()
    const clientes = ref([
      { idCliente: 1, esProspecto: false },
      { idCliente: 2, esProspecto: true }
    ])
    const sinAsignar = ref([{ idCotizacion: 99 }])

    const dash = useComercialDashboard({
      cotizaciones,
      clientes,
      solicitudesSinAsignar: sinAsignar,
      referenceDate: refDate
    })

    expect(dash.totalCotizaciones.value).toBe(4)
    expect(dash.aprobadasCount.value).toBe(1)
    expect(dash.enProcesoCount.value).toBe(1) // EN_REVISION
    expect(dash.observadasCount.value).toBe(1)
    expect(dash.rechazadasCount.value).toBe(1)
    expect(dash.conversionRate.value).toBe('25.0')
    expect(dash.totalMontoCotizado.value).toBe(11000)
    expect(dash.totalAbonadoConfirmado.value).toBe(2000)
    expect(dash.totalSaldoPendiente.value).toBe(8000)
    expect(dash.porVencerCount.value).toBe(1)
    expect(dash.clientesEfectivosCount.value).toBe(1)
    expect(dash.prospectosCount.value).toBe(1)
    expect(dash.solicitudesSinAsignarCount.value).toBe(1)
  })

  it('filtra por período MES_ACTUAL, asesor y régimen y genera exportParams consistentes', () => {
    const dash = useComercialDashboard({
      cotizaciones: sampleCotizaciones(),
      clientes: ref([]),
      solicitudesSinAsignar: ref([]),
      referenceDate: refDate
    })

    dash.filterPeriodo.value = 'MES_ACTUAL'
    expect(dash.totalCotizaciones.value).toBe(3) // excluye 2026-10-05
    expect(dash.exportParams.value).toEqual({
      desde: '2026-11-01',
      hasta: '2026-11-18'
    })

    dash.filterAsesorId.value = 10
    expect(dash.totalCotizaciones.value).toBe(2)

    dash.filterRegimen.value = 'EXPORTACION_DEFINITIVA'
    expect(dash.totalCotizaciones.value).toBe(0)

    dash.resetDashboardFilters()
    expect(dash.totalCotizaciones.value).toBe(4)
  })

  it('construye datasets de Chart.js para estados, evolución mensual y regímenes', () => {
    const dash = useComercialDashboard({
      cotizaciones: sampleCotizaciones(),
      clientes: ref([]),
      solicitudesSinAsignar: ref([]),
      referenceDate: refDate
    })

    expect(dash.estadoChartData.value.datasets[0].data).toEqual([1, 1, 1, 1])
    expect(dash.evolucionChartData.value.labels).toEqual(['2026-10', '2026-11'])
    expect(dash.evolucionChartData.value.datasets[0].data).toEqual([1, 3])
    expect(dash.evolucionChartData.value.datasets[1].data).toEqual([0, 1])
    expect(dash.regimenChartData.value.labels).toEqual([
      'IMPORTACION DEFINITIVA',
      'EXPORTACION DEFINITIVA'
    ])
    expect(dash.regimenChartData.value.datasets[0].data).toEqual([8000, 3000])
  })

  it('resuelve rangos de fechas de HOY, 7D y MES_ACTUAL y construye params de exportación por tipo', () => {
    expect(resolvePeriodoRange('HOY', refDate)).toEqual({
      desde: '2026-11-18',
      hasta: '2026-11-18'
    })
    expect(resolvePeriodoRange('7D', refDate)).toEqual({
      desde: '2026-11-12',
      hasta: '2026-11-18'
    })

    const dash = useComercialDashboard({
      cotizaciones: sampleCotizaciones(),
      clientes: ref([]),
      solicitudesSinAsignar: ref([]),
      referenceDate: refDate
    })
    dash.filterAsesorId.value = 10

    expect(dash.buildExportParamsForPeriodo('HOY')).toEqual({
      desde: '2026-11-18',
      hasta: '2026-11-18',
      idUsuarioComercial: 10
    })
    expect(dash.buildExportParamsForPeriodo('7D')).toEqual({
      desde: '2026-11-12',
      hasta: '2026-11-18',
      idUsuarioComercial: 10
    })
    expect(dash.buildExportParamsForPeriodo('MES_ACTUAL')).toEqual({
      desde: '2026-11-01',
      hasta: '2026-11-18',
      idUsuarioComercial: 10
    })
  })
})
