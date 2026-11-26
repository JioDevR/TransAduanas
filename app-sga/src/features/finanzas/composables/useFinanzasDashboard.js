import { computed, ref } from 'vue'
import { resolvePeriodoRange } from '@/shared/utils/dateUtils'
import { round2, summarizeCotizacionesCartera } from '../utils/finanzasCalculations'

const extractIsoDate = (val) => {
  if (!val) return ''
  return String(val).slice(0, 10)
}

/**
 * Composable de filtros, métricas KPI y datasets Chart.js para el Dashboard Financiero.
 */
export function useFinanzasDashboard({
  cotizacionesAprobadas,
  anticiposList,
  liquidacionesList,
  expedientesPorLiquidar,
  metricas,
  referenceDate = new Date()
}) {
  const filterPeriodo = ref('ALL')

  const periodoOptions = [
    { label: 'Todo el histórico', value: 'ALL' },
    { label: 'Hoy (Diario)', value: 'HOY' },
    { label: 'Últimos 7 días', value: '7D' },
    { label: 'Este mes (Mensual)', value: 'MES_ACTUAL' },
    { label: 'Últimos 30 días', value: '30D' }
  ]

  const periodoRange = computed(() => resolvePeriodoRange(filterPeriodo.value, referenceDate))

  const filteredCotizaciones = computed(() => {
    const { desde, hasta } = periodoRange.value
    return (cotizacionesAprobadas.value || []).filter((c) => {
      if (!desde && !hasta) return true
      const fecha = extractIsoDate(c.fechaEmision)
      if (desde && (!fecha || fecha < desde)) return false
      if (hasta && (!fecha || fecha > hasta)) return false
      return true
    })
  })

  const filteredLiquidaciones = computed(() => {
    const { desde, hasta } = periodoRange.value
    return (liquidacionesList.value || []).filter((l) => {
      if (!desde && !hasta) return true
      const fecha = extractIsoDate(l.fechaEmision || l.fechaPago)
      if (desde && (!fecha || fecha < desde)) return false
      if (hasta && (!fecha || fecha > hasta)) return false
      return true
    })
  })

  const filteredAnticipos = computed(() => {
    const { desde, hasta } = periodoRange.value
    return (anticiposList.value || []).filter((a) => {
      if (!desde && !hasta) return true
      const fecha = extractIsoDate(a.fechaOperacion || a.fechaRegistro)
      if (desde && (!fecha || fecha < desde)) return false
      if (hasta && (!fecha || fecha > hasta)) return false
      return true
    })
  })

  const tipoCambioOficial = computed(() => {
    const tc = Number(metricas.value?.tipoCambioOficial || 3.75)
    return tc > 0 ? tc : 3.75
  })

  const carteraSummary = computed(() => summarizeCotizacionesCartera(filteredCotizaciones.value))

  const liquidacionesStats = computed(() => {
    if (filterPeriodo.value === 'ALL' && metricas.value && Number(metricas.value.totalLiquidaciones || 0) > 0) {
      return {
        cobradoPen: Number(metricas.value.totalCobradoPen || 0),
        cobradoUsd: Number(metricas.value.totalCobradoUsd || 0),
        porCobrarPen: Number(metricas.value.cuentasPorCobrarPen || 0),
        tributosSunatPen: Number(metricas.value.totalTributosSunatPen || 0),
        detraccionesPen: Number(metricas.value.totalDetraccionesPen || 0),
        pendientesCount: Number(metricas.value.liquidacionesPendientes || 0),
        pagadasCount: Number(metricas.value.liquidacionesPagadas || 0)
      }
    }

    let cobradoPen = 0
    let cobradoUsd = 0
    let porCobrarPen = 0
    let tributosSunatPen = 0
    let detraccionesPen = 0
    let pendientesCount = 0
    let pagadasCount = 0

    for (const lf of filteredLiquidaciones.value) {
      const estado = String(lf.estadoPago || '').toUpperCase()
      if (estado === 'PAGADO') {
        pagadasCount += 1
        cobradoPen += Number(lf.totalGeneralPen || 0)
        cobradoUsd += Number(lf.totalGeneralUsd || 0)
        tributosSunatPen += Number(lf.totalTributosSunat || 0)
        detraccionesPen += Number(lf.montoDetraccion || 0)
      } else if (estado === 'PENDIENTE' || estado === 'EN_CONCILIACION') {
        pendientesCount += 1
        porCobrarPen += Number(lf.totalGeneralPen || 0)
        tributosSunatPen += Number(lf.totalTributosSunat || 0)
        detraccionesPen += Number(lf.montoDetraccion || 0)
      }
    }

    return {
      cobradoPen: round2(cobradoPen),
      cobradoUsd: round2(cobradoUsd),
      porCobrarPen: round2(porCobrarPen),
      tributosSunatPen: round2(tributosSunatPen),
      detraccionesPen: round2(detraccionesPen),
      pendientesCount,
      pagadasCount
    }
  })

  const anticiposPendientesCount = computed(
    () => filteredAnticipos.value.filter((a) => a.estadoPago === 'PENDIENTE_REVISION').length
  )

  const tasaRecaudacionCarteraPct = computed(() => {
    const total = carteraSummary.value.totalCotizadoUsd
    if (!total) return 0
    return Math.min(100, Math.round((carteraSummary.value.totalAbonadoUsd / total) * 100))
  })

  const embudoChartData = computed(() => ({
    labels: ['Sin Abono', 'En Revisión', 'Adelanto Conforme', '100% Pagado'],
    datasets: [
      {
        data: [
          carteraSummary.value.sinAbonoCount,
          carteraSummary.value.enRevisionCount,
          carteraSummary.value.adelantoConformeCount,
          carteraSummary.value.pagadoTotalCount
        ]
      }
    ]
  }))

  const balanceChartData = computed(() => ({
    labels: ['Cobrado', 'Por Cobrar', 'Tributos SUNAT', 'Fondo SPOT 12%'],
    datasets: [
      {
        label: 'Importe (S/)',
        data: [
          liquidacionesStats.value.cobradoPen,
          liquidacionesStats.value.porCobrarPen,
          liquidacionesStats.value.tributosSunatPen,
          liquidacionesStats.value.detraccionesPen
        ]
      }
    ]
  }))

  const cotizacionesPrioritarias = computed(() =>
    filteredCotizaciones.value
      .filter((c) => {
        const saldo = Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0)
        return saldo > 0.01 || c.estadoPago === 'PENDIENTE_REVISION'
      })
      .slice(0, 6)
  )

  const expedientesPorLiquidarCount = computed(() => (expedientesPorLiquidar.value || []).length)

  const hasActiveFilters = computed(() => filterPeriodo.value !== 'ALL')

  const resetDashboardFilters = () => {
    filterPeriodo.value = 'ALL'
  }

  return {
    filterPeriodo,
    periodoOptions,
    hasActiveFilters,
    resetDashboardFilters,
    tipoCambioOficial,
    filteredCotizaciones,
    carteraSummary,
    liquidacionesStats,
    anticiposPendientesCount,
    expedientesPorLiquidarCount,
    tasaRecaudacionCarteraPct,
    embudoChartData,
    balanceChartData,
    cotizacionesPrioritarias
  }
}
