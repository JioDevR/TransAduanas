import { ref, computed } from 'vue'
import { filtrarExpedientesPorFase } from '../utils/aduanasWorkflowUtils'

/**
 * Composable para la bandeja de Gestión Integral de Expedientes Aduaneros.
 * Centraliza filtros por agente, etapa operativa, régimen, canal SUNAT, búsqueda y ordenamiento.
 */
export function useAduanasExpedientes({ expedientes, authStore }) {
  const isAgenteAduanero = computed(() => {
    return authStore?.userRole === 'AGENTE_ADUANERO' || authStore?.user?.rol === 'AGENTE_ADUANERO'
  })

  const faseExpediente = ref('TODOS')
  const filterAgente = ref(isAgenteAduanero.value ? 'MINE' : 'ALL')
  const filterRegimen = ref('ALL')
  const filterCanal = ref('ALL')
  const searchQuery = ref('')
  const sortBy = ref('RECENT')

  const hasActiveFilters = computed(() => {
    return Boolean(
      searchQuery.value.trim() ||
      filterRegimen.value !== 'ALL' ||
      filterCanal.value !== 'ALL' ||
      sortBy.value !== 'RECENT' ||
      filterAgente.value !== (isAgenteAduanero.value ? 'MINE' : 'ALL')
    )
  })

  const resetFilters = () => {
    searchQuery.value = ''
    filterRegimen.value = 'ALL'
    filterCanal.value = 'ALL'
    sortBy.value = 'RECENT'
    filterAgente.value = isAgenteAduanero.value ? 'MINE' : 'ALL'
  }

  const agentesDisponibles = computed(() => {
    const map = new Map()
    ;(expedientes.value || []).forEach((exp) => {
      if (exp.idAgenteAduanero && exp.agenteAduaneroNombre) {
        map.set(exp.idAgenteAduanero, exp.agenteAduaneroNombre)
      }
    })
    return Array.from(map.entries())
      .map(([id, nombre]) => ({ id, nombre }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre))
  })

  const misExpedientesCount = computed(() => {
    const myId = authStore?.user?.idUsuario
    if (!myId) return 0
    return (expedientes.value || []).filter((e) => e.idAgenteAduanero === myId).length
  })

  const baseExpedientes = computed(() => {
    const list = expedientes.value || []
    if (filterAgente.value === 'MINE') {
      const myId = authStore?.user?.idUsuario
      return list.filter((e) => e.idAgenteAduanero === myId)
    }
    if (filterAgente.value === 'UNASSIGNED') {
      return list.filter((e) => !e.idAgenteAduanero)
    }
    if (filterAgente.value !== 'ALL') {
      return list.filter((e) => String(e.idAgenteAduanero) === String(filterAgente.value))
    }
    return list
  })

  const faseCounts = computed(() => ({
    TODOS: baseExpedientes.value.length,
    PENDIENTE_CANAL: filtrarExpedientesPorFase(baseExpedientes.value, 'PENDIENTE_CANAL').length,
    CONTROL_SUNAT: filtrarExpedientesPorFase(baseExpedientes.value, 'CONTROL_SUNAT').length,
    LEVANTE_AUTORIZADO: filtrarExpedientesPorFase(baseExpedientes.value, 'LEVANTE_AUTORIZADO').length,
    CERRADOS: filtrarExpedientesPorFase(baseExpedientes.value, 'CERRADOS').length
  }))

  const filteredExpedientes = computed(() => {
    let list = [...filtrarExpedientesPorFase(baseExpedientes.value, faseExpediente.value)]

    if (filterRegimen.value && filterRegimen.value !== 'ALL') {
      const regFilter = filterRegimen.value.toUpperCase()
      list = list.filter((e) => (e.regimenAduanero || '').toUpperCase().includes(regFilter))
    }

    if (filterCanal.value && filterCanal.value !== 'ALL') {
      list = list.filter((e) => {
        if (filterCanal.value === 'PENDIENTE') {
          return !e.canalSunat || e.canalSunat === 'PENDIENTE'
        }
        return e.canalSunat === filterCanal.value
      })
    }

    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      list = list.filter((e) => {
        return (
          e.codigoExpediente?.toLowerCase().includes(q) ||
          e.codigoCotizacion?.toLowerCase().includes(q) ||
          e.clienteRazonSocial?.toLowerCase().includes(q) ||
          e.clienteNumeroDocumento?.toLowerCase().includes(q) ||
          e.numeroDam?.toLowerCase().includes(q) ||
          e.numeroContenedor?.toLowerCase().includes(q) ||
          e.naveViaje?.toLowerCase().includes(q) ||
          e.agenteAduaneroNombre?.toLowerCase().includes(q)
        )
      })
    }

    list.sort((a, b) => {
      switch (sortBy.value) {
        case 'OLDEST':
          return (a.idExpediente || 0) - (b.idExpediente || 0)
        case 'CODE_ASC':
          return String(a.codigoExpediente || '').localeCompare(String(b.codigoExpediente || ''))
        case 'RECENT':
        default:
          return (b.idExpediente || 0) - (a.idExpediente || 0)
      }
    })

    return list
  })

  return {
    faseExpediente,
    filterAgente,
    filterRegimen,
    filterCanal,
    searchQuery,
    sortBy,
    hasActiveFilters,
    resetFilters,
    agentesDisponibles,
    misExpedientesCount,
    baseExpedientes,
    faseCounts,
    filteredExpedientes
  }
}
