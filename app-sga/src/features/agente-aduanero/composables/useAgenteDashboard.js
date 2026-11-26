import { computed, ref } from 'vue'
import { resolvePeriodoRange } from '@/shared/utils/dateUtils'
import { formatRegimenShort } from '../utils/agenteFormatters'
import {
  belongsToAgentePhase,
  getAccionesPermitidasAgente,
  isExpedienteCerrado,
  isExportacion
} from '../utils/agenteWorkflowUtils'

const extractIsoDate = (val) => {
  if (!val) return ''
  return String(val).slice(0, 10)
}

/**
 * Composable de métricas, filtros, datasets Chart.js y cola prioritaria
 * para el Dashboard del Agente Aduanero.
 */
export function useAgenteDashboard({ expedientes, referenceDate = new Date() }) {
  const filterPeriodo = ref('ALL')
  const filterRegimen = ref('ALL')

  const periodoRange = computed(() => resolvePeriodoRange(filterPeriodo.value, referenceDate))

  const regimenesOptions = computed(() => {
    const set = new Set()
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

  const filteredExpedientes = computed(() => {
    const { desde, hasta } = periodoRange.value
    return (expedientes.value || []).filter((e) => {
      const fecha = extractIsoDate(e.fechaApertura || e.fechaAsignacion)
      if (desde && (!fecha || fecha < desde)) return false
      if (hasta && (!fecha || fecha > hasta)) return false
      if (filterRegimen.value !== 'ALL' && e.regimenAduanero !== filterRegimen.value) return false
      return true
    })
  })

  const counts = computed(() => {
    const list = filteredExpedientes.value
    let transito = 0
    let inspeccion = 0
    let levante = 0
    let cerrados = 0

    for (const exp of list) {
      if (belongsToAgentePhase(exp, 'transito')) transito++
      if (belongsToAgentePhase(exp, 'inspeccion')) inspeccion++
      if (belongsToAgentePhase(exp, 'levante')) levante++
      if (isExpedienteCerrado(exp)) cerrados++
    }

    return {
      todos: list.length,
      transito,
      inspeccion,
      levante,
      cerrados
    }
  })

  const canalCounts = computed(() => {
    const res = { PENDIENTE: 0, VERDE: 0, NARANJA: 0, ROJO: 0 }
    filteredExpedientes.value.forEach((e) => {
      const c = e.canalSunat || 'PENDIENTE'
      if (res[c] !== undefined) res[c] += 1
      else res.PENDIENTE += 1
    })
    return res
  })

  const regimenCounts = computed(() => {
    let importacion = 0
    let exportacion = 0
    filteredExpedientes.value.forEach((e) => {
      if (isExportacion(e)) exportacion++
      else importacion++
    })
    return { importacion, exportacion }
  })

  const levanteRate = computed(() => {
    const total = counts.value.todos
    if (!total) return 0
    return Math.round(((counts.value.levante + counts.value.cerrados) / total) * 100)
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

  const etapasChartData = computed(() => ({
    labels: ['En Tránsito / Arribo', 'Aforo SUNAT', 'Levante & Despacho', 'Cerrados'],
    datasets: [
      {
        label: 'Expedientes',
        data: [
          counts.value.transito,
          counts.value.inspeccion,
          counts.value.levante,
          counts.value.cerrados
        ]
      }
    ]
  }))

  const despachosPrioritarios = computed(() => {
    const items = []
    for (const exp of filteredExpedientes.value) {
      if (isExpedienteCerrado(exp)) continue
      const acc = getAccionesPermitidasAgente(exp)

      if (acc.puedeDiligencia) {
        items.push({
          expediente: exp,
          tab: 'inspeccion',
          severity: 'warn',
          icon: 'pi pi-exclamation-triangle',
          motivo: `Requiere Diligencia Canal ${exp.canalSunat || 'SUNAT'}`,
          rank: 1
        })
      } else if (acc.puedeSolicitarTransporte) {
        items.push({
          expediente: exp,
          tab: 'levante',
          severity: 'success',
          icon: 'pi pi-truck',
          motivo: 'Levante otorgado · Registrar transporte externo',
          rank: 2
        })
      } else if (acc.puedeRetiroDirecto) {
        items.push({
          expediente: exp,
          tab: 'levante',
          severity: 'success',
          icon: 'pi pi-check',
          motivo: 'Levante otorgado · Registrar retiro directo',
          rank: 2
        })
      } else if (acc.puedeEmbarque) {
        items.push({
          expediente: exp,
          tab: 'levante',
          severity: 'success',
          icon: 'pi pi-send',
          motivo: 'Levante exportación · Confirmar embarque',
          rank: 2
        })
      } else if (acc.puedeRecepcionExterior) {
        items.push({
          expediente: exp,
          tab: 'levante',
          severity: 'info',
          icon: 'pi pi-check-circle',
          motivo: 'Embarcado · Confirmar entrega exterior',
          rank: 3
        })
      } else if (acc.puedeConfirmarArribo) {
        items.push({
          expediente: exp,
          tab: 'transito',
          severity: 'info',
          icon: 'pi pi-compass',
          motivo: 'En navegación · Confirmar arribo a puerto',
          rank: 4
        })
      } else if (acc.puedeSalidaOrigen) {
        items.push({
          expediente: exp,
          tab: 'transito',
          severity: 'secondary',
          icon: 'pi pi-send',
          motivo: 'Asignado · Registrar salida de origen',
          rank: 5
        })
      }
    }

    return items.sort((a, b) => a.rank - b.rank).slice(0, 6)
  })

  const hasActiveFilters = computed(
    () => filterPeriodo.value !== 'ALL' || filterRegimen.value !== 'ALL'
  )

  const resetDashboardFilters = () => {
    filterPeriodo.value = 'ALL'
    filterRegimen.value = 'ALL'
  }

  return {
    filterPeriodo,
    filterRegimen,
    regimenesOptions,
    filteredExpedientes,
    counts,
    canalCounts,
    regimenCounts,
    levanteRate,
    canalesChartData,
    etapasChartData,
    despachosPrioritarios,
    hasActiveFilters,
    resetDashboardFilters
  }
}
