import { computed, ref } from 'vue'
import { resolvePeriodoRange } from '@/shared/utils/dateUtils'

export { resolvePeriodoRange }

/**
 * Composable de métricas, filtros y datasets Chart.js para el Dashboard Comercial.
 */
export function useComercialDashboard({
  cotizaciones,
  clientes,
  solicitudesSinAsignar,
  referenceDate = new Date()
}) {
  const filterPeriodo = ref('ALL')
  const filterAsesorId = ref('ALL')
  const filterRegimen = ref('ALL')

  const periodoRange = computed(() => resolvePeriodoRange(filterPeriodo.value, referenceDate))

  const asesoresOptions = computed(() => {
    const map = new Map()
    ;(cotizaciones.value || []).forEach((c) => {
      if (c.idUsuarioComercial && c.usuarioComercialNombre) {
        map.set(c.idUsuarioComercial, c.usuarioComercialNombre)
      }
    })
    return [
      { label: 'Todos los asesores', value: 'ALL' },
      ...Array.from(map.entries())
        .map(([id, nombre]) => ({ label: nombre, value: id }))
        .sort((a, b) => a.label.localeCompare(b.label))
    ]
  })

  const regimenesOptions = computed(() => {
    const set = new Set()
    ;(cotizaciones.value || []).forEach((c) => {
      if (c.regimenAduanero) set.add(c.regimenAduanero)
    })
    return [
      { label: 'Todos los regímenes', value: 'ALL' },
      ...Array.from(set)
        .sort()
        .map((r) => ({ label: r.replace(/_/g, ' '), value: r }))
    ]
  })

  const filteredCotizaciones = computed(() => {
    const { desde, hasta } = periodoRange.value
    return (cotizaciones.value || []).filter((c) => {
      if (desde && (!c.fechaEmision || c.fechaEmision < desde)) return false
      if (hasta && (!c.fechaEmision || c.fechaEmision > hasta)) return false
      if (filterAsesorId.value !== 'ALL' && c.idUsuarioComercial !== filterAsesorId.value) return false
      if (filterRegimen.value !== 'ALL' && c.regimenAduanero !== filterRegimen.value) return false
      return true
    })
  })

  const totalCotizaciones = computed(() => filteredCotizaciones.value.length)

  const totalMontoCotizado = computed(() =>
    filteredCotizaciones.value.reduce((acc, c) => acc + (Number(c.totalCotizado) || 0), 0)
  )

  const totalAbonadoConfirmado = computed(() =>
    filteredCotizaciones.value.reduce((acc, c) => acc + (Number(c.totalAbonado) || 0), 0)
  )

  const totalSaldoPendiente = computed(() =>
    filteredCotizaciones.value.reduce((acc, c) => acc + (Number(c.saldoPendiente) || 0), 0)
  )

  const aprobadasCount = computed(() =>
    filteredCotizaciones.value.filter((c) => c.estado === 'APROBADA').length
  )

  const enProcesoCount = computed(() =>
    filteredCotizaciones.value.filter(
      (c) => c.estado === 'PENDIENTE' || c.estado === 'EN_REVISION' || c.estado === 'ENVIADA'
    ).length
  )

  const observadasCount = computed(() =>
    filteredCotizaciones.value.filter((c) => c.estado === 'OBSERVADA' || c.estado === 'REVISION').length
  )

  const rechazadasCount = computed(() =>
    filteredCotizaciones.value.filter((c) => c.estado === 'RECHAZADA' || c.estado === 'ANULADA').length
  )

  const conversionRate = computed(() => {
    if (!totalCotizaciones.value) return '0.0'
    return ((aprobadasCount.value / totalCotizaciones.value) * 100).toFixed(1)
  })

  const ticketPromedio = computed(() => {
    if (!totalCotizaciones.value) return 0
    return totalMontoCotizado.value / totalCotizaciones.value
  })

  const porVencerCount = computed(() => {
    const todayMs = new Date(
      referenceDate.getFullYear(),
      referenceDate.getMonth(),
      referenceDate.getDate()
    ).getTime()
    const threeDaysMs = 3 * 24 * 60 * 60 * 1000

    return filteredCotizaciones.value.filter((c) => {
      if (c.estado !== 'PENDIENTE' && c.estado !== 'EN_REVISION' && c.estado !== 'OBSERVADA') {
        return false
      }
      if (!c.fechaEmision) return false
      const [y, m, d] = String(c.fechaEmision).split('-').map(Number)
      if (!y || !m || !d) return false
      const emisionMs = new Date(y, m - 1, d).getTime()
      const validez = Number(c.validezDias) || 15
      const venceMs = emisionMs + validez * 24 * 60 * 60 * 1000
      return venceMs - todayMs <= threeDaysMs
    }).length
  })

  const clientesEfectivosCount = computed(() =>
    (clientes.value || []).filter((c) => !c.esProspecto).length
  )

  const prospectosCount = computed(() =>
    (clientes.value || []).filter((c) => c.esProspecto).length
  )

  // Gráfico 1 (Dona): Distribución por Estado
  const estadoChartData = computed(() => ({
    labels: ['Aprobadas', 'En Proceso', 'Observadas', 'Rechazadas / Anuladas'],
    datasets: [
      {
        data: [
          aprobadasCount.value,
          enProcesoCount.value,
          observadasCount.value,
          rechazadasCount.value
        ],
        backgroundColor: ['#2e7d32', '#d97706', '#dc2626', '#8c827a'],
        hoverBackgroundColor: ['#256628', '#b45309', '#b91c1c', '#6e655e'],
        borderWidth: 2,
        borderColor: '#ffffff'
      }
    ]
  }))

  // Gráfico 2 (Barras): Evolución Mensual Emitidas vs. Aprobadas
  const evolucionChartData = computed(() => {
    const monthsMap = new Map()
    filteredCotizaciones.value.forEach((c) => {
      if (!c.fechaEmision) return
      const monthKey = String(c.fechaEmision).slice(0, 7) // YYYY-MM
      if (!monthsMap.has(monthKey)) {
        monthsMap.set(monthKey, { emitidas: 0, aprobadas: 0 })
      }
      const bucket = monthsMap.get(monthKey)
      bucket.emitidas++
      if (c.estado === 'APROBADA') bucket.aprobadas++
    })

    const sortedKeys = Array.from(monthsMap.keys()).sort().slice(-6)
    return {
      labels: sortedKeys,
      datasets: [
        {
          label: 'Emitidas',
          backgroundColor: '#9a7f60',
          borderRadius: 6,
          data: sortedKeys.map((k) => monthsMap.get(k).emitidas)
        },
        {
          label: 'Aprobadas',
          backgroundColor: '#2e7d32',
          borderRadius: 6,
          data: sortedKeys.map((k) => monthsMap.get(k).aprobadas)
        }
      ]
    }
  })

  // Gráfico 3 (Barras Horizontales): Volumen Cotizado (USD) por Régimen
  const regimenChartData = computed(() => {
    const map = new Map()
    filteredCotizaciones.value.forEach((c) => {
      const reg = c.regimenAduanero || 'OTROS'
      map.set(reg, (map.get(reg) || 0) + (Number(c.totalCotizado) || 0))
    })

    const sorted = Array.from(map.entries()).sort((a, b) => b[1] - a[1])
    return {
      labels: sorted.map(([reg]) => reg.replace(/_/g, ' ')),
      datasets: [
        {
          label: 'Volumen USD',
          backgroundColor: '#4a433c',
          borderRadius: 6,
          data: sorted.map(([, total]) => Number(total.toFixed(2)))
        }
      ]
    }
  })

  const buildExportParamsForPeriodo = (periodoOverride = null) => {
    const range = periodoOverride
      ? resolvePeriodoRange(periodoOverride, referenceDate)
      : periodoRange.value
    const params = {}
    if (range.desde) params.desde = range.desde
    if (range.hasta) params.hasta = range.hasta
    if (filterAsesorId.value !== 'ALL') params.idUsuarioComercial = filterAsesorId.value
    if (filterRegimen.value !== 'ALL') params.regimen = filterRegimen.value
    return params
  }

  const exportParams = computed(() => buildExportParamsForPeriodo())

  const resetDashboardFilters = () => {
    filterPeriodo.value = 'ALL'
    filterAsesorId.value = 'ALL'
    filterRegimen.value = 'ALL'
  }

  return {
    filterPeriodo,
    filterAsesorId,
    filterRegimen,
    asesoresOptions,
    regimenesOptions,
    filteredCotizaciones,
    totalCotizaciones,
    totalMontoCotizado,
    totalAbonadoConfirmado,
    totalSaldoPendiente,
    aprobadasCount,
    enProcesoCount,
    observadasCount,
    rechazadasCount,
    conversionRate,
    ticketPromedio,
    porVencerCount,
    clientesEfectivosCount,
    prospectosCount,
    solicitudesSinAsignarCount: computed(() => (solicitudesSinAsignar?.value || []).length),
    estadoChartData,
    evolucionChartData,
    regimenChartData,
    exportParams,
    buildExportParamsForPeriodo,
    resetDashboardFilters
  }
}
