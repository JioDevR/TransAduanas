import { computed, ref } from 'vue'
import { resolvePeriodoRange } from '@/shared/utils/dateUtils'
import {
  clasificarCotizacionesAduaneras,
  filtrarExpedientesPorFase
} from '../utils/aduanasWorkflowUtils'
import { formatRegimenShort } from '../utils/aduanasFormatters'

const extractIsoDate = (val) => {
  if (!val) return ''
  return String(val).slice(0, 10)
}

/**
 * Composable de métricas, filtros y datasets Chart.js para el Dashboard de Gestión Aduanera.
 */
export function useAduanasDashboard({
  cotizaciones,
  expedientes,
  referenceDate = new Date()
}) {
  const filterPeriodo = ref('ALL')
  const filterAgenteId = ref('ALL')
  const filterRegimen = ref('ALL')

  const periodoRange = computed(() => resolvePeriodoRange(filterPeriodo.value, referenceDate))

  const agentesOptions = computed(() => {
    const map = new Map()
    ;(expedientes.value || []).forEach((e) => {
      if (e.idAgenteAduanero && e.agenteAduaneroNombre) {
        map.set(e.idAgenteAduanero, e.agenteAduaneroNombre)
      }
    })
    return [
      { label: 'Todos los agentes', value: 'ALL' },
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
    ;(expedientes.value || []).forEach((e) => {
      if (e.regimenAduanero) set.add(e.regimenAduanero)
    })
    return [
      { label: 'Todos los regímenes', value: 'ALL' },
      ...Array.from(set)
        .sort()
        .map((r) => ({ label: formatRegimenShort(r), value: r }))
    ]
  })

  const filteredCotizaciones = computed(() => {
    const { desde, hasta } = periodoRange.value
    return (cotizaciones.value || []).filter((c) => {
      const fecha = extractIsoDate(c.fechaEmision)
      if (desde && (!fecha || fecha < desde)) return false
      if (hasta && (!fecha || fecha > hasta)) return false
      if (filterRegimen.value !== 'ALL' && c.regimenAduanero !== filterRegimen.value) return false
      return true
    })
  })

  const filteredExpedientes = computed(() => {
    const { desde, hasta } = periodoRange.value
    return (expedientes.value || []).filter((e) => {
      const fecha = extractIsoDate(e.fechaApertura || e.fechaCreacion)
      if (desde && (!fecha || fecha < desde)) return false
      if (hasta && (!fecha || fecha > hasta)) return false
      if (filterAgenteId.value !== 'ALL' && e.idAgenteAduanero !== filterAgenteId.value) return false
      if (filterRegimen.value !== 'ALL' && e.regimenAduanero !== filterRegimen.value) return false
      return true
    })
  })

  const cotizacionesClasificadas = computed(() => {
    return clasificarCotizacionesAduaneras(filteredCotizaciones.value)
  })

  const expedientesPendienteCanal = computed(() =>
    filtrarExpedientesPorFase(filteredExpedientes.value, 'PENDIENTE_CANAL')
  )

  const expedientesControlSunat = computed(() =>
    filtrarExpedientesPorFase(filteredExpedientes.value, 'CONTROL_SUNAT')
  )

  const expedientesLevante = computed(() =>
    filtrarExpedientesPorFase(filteredExpedientes.value, 'LEVANTE_AUTORIZADO')
  )

  const expedientesCerrados = computed(() =>
    filtrarExpedientesPorFase(filteredExpedientes.value, 'CERRADOS')
  )

  const canalCounts = computed(() => {
    const counts = { PENDIENTE: 0, VERDE: 0, NARANJA: 0, ROJO: 0 }
    filteredExpedientes.value.forEach((e) => {
      const c = e.canalSunat || 'PENDIENTE'
      if (counts[c] !== undefined) {
        counts[c] += 1
      } else {
        counts.PENDIENTE += 1
      }
    })
    return counts
  })

  const totalExpedientes = computed(() => filteredExpedientes.value.length)

  const levanteRate = computed(() => {
    if (!totalExpedientes.value) return 0
    const liberados = expedientesLevante.value.length + expedientesCerrados.value.length
    return Math.round((liberados / totalExpedientes.value) * 100)
  })

  const canalesChartData = computed(() => ({
    labels: ['Pendiente', 'Verde', 'Naranja', 'Rojo'],
    datasets: [
      {
        data: [
          canalCounts.value.PENDIENTE,
          canalCounts.value.VERDE,
          canalCounts.value.NARANJA,
          canalCounts.value.ROJO
        ]
      }
    ]
  }))

  const tendenciaChartData = computed(() => {
    const map = new Map()
    const ensureMonth = (ym) => {
      if (!map.has(ym)) {
        map.set(ym, { cotizaciones: 0, expedientes: 0 })
      }
      return map.get(ym)
    }

    filteredCotizaciones.value.forEach((c) => {
      const fecha = extractIsoDate(c.fechaEmision)
      if (fecha.length >= 7) {
        ensureMonth(fecha.slice(0, 7)).cotizaciones += 1
      }
    })

    filteredExpedientes.value.forEach((e) => {
      const fecha = extractIsoDate(e.fechaApertura || e.fechaCreacion)
      if (fecha.length >= 7) {
        ensureMonth(fecha.slice(0, 7)).expedientes += 1
      }
    })

    const months = Array.from(map.keys()).sort().slice(-6)
    const monthNames = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']
    const labels = months.map((ym) => {
      const [y, m] = ym.split('-')
      const idx = Number(m) - 1
      return `${monthNames[idx] || m} ${y.slice(2)}`
    })

    return {
      labels,
      datasets: [
        {
          label: 'Cotizaciones',
          data: months.map((ym) => map.get(ym).cotizaciones),
          tension: 0.35
        },
        {
          label: 'Expedientes',
          data: months.map((ym) => map.get(ym).expedientes),
          tension: 0.35
        }
      ]
    }
  })

  const etapasChartData = computed(() => ({
    labels: ['Por Asignar Canal', 'Control SUNAT', 'Levante Autorizado', 'Cerrados'],
    datasets: [
      {
        label: 'Expedientes',
        data: [
          expedientesPendienteCanal.value.length,
          expedientesControlSunat.value.length,
          expedientesLevante.value.length,
          expedientesCerrados.value.length
        ]
      }
    ]
  }))

  const hasActiveFilters = computed(
    () =>
      filterPeriodo.value !== 'ALL' ||
      filterAgenteId.value !== 'ALL' ||
      filterRegimen.value !== 'ALL'
  )

  const resetDashboardFilters = () => {
    filterPeriodo.value = 'ALL'
    filterAgenteId.value = 'ALL'
    filterRegimen.value = 'ALL'
  }

  return {
    filterPeriodo,
    filterAgenteId,
    filterRegimen,
    agentesOptions,
    regimenesOptions,
    filteredCotizaciones,
    filteredExpedientes,
    cotizacionesClasificadas,
    expedientesPendienteCanal,
    expedientesControlSunat,
    expedientesLevante,
    expedientesCerrados,
    canalCounts,
    totalExpedientes,
    levanteRate,
    canalesChartData,
    tendenciaChartData,
    etapasChartData,
    hasActiveFilters,
    resetDashboardFilters
  }
}
