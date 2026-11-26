import { ref, computed } from 'vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { expedienteService } from '@/shared/services/expedienteService'
import { userService } from '@/features/admin/users/services/userService'
import { belongsToAgentePhase, isExpedienteCerrado } from '../utils/agenteWorkflowUtils'

const PHASE_FILTERS = ['todos', 'transito', 'inspeccion', 'levante', 'cerrados']

export function useAgenteBandeja() {
  const authStore = useAuthStore()

  const loading = ref(false)
  const errorMessage = ref('')
  const expedientes = ref([])
  const agentesList = ref([])

  // Vista activa en sidebar ('dashboard' | 'expedientes' | 'gestion') y filtro de fase interno
  const activeView = ref('dashboard')
  const phaseFilter = ref('todos')

  // Filtros reactivos
  const filterRegimen = ref('')
  const filterCanal = ref('')
  const searchQuery = ref('')
  const sortBy = ref('RECENT')
  const selectedAgenteId = ref(null)

  const esSupervisorOAdmin = computed(() => {
    const rol = authStore.user?.rol
    return rol === 'ADMINISTRADOR_TI' || rol === 'GERENTE_GENERAL' || rol === 'SUPERVISOR_ADUANERO'
  })

  const loadAgentes = async () => {
    if (!esSupervisorOAdmin.value) return
    try {
      const agentes = await userService.getAgentesAduaneros()
      agentesList.value = agentes || []
    } catch (e) {
      console.error('Error cargando padrón de agentes aduaneros:', e)
    }
  }

  const loadData = async () => {
    loading.value = true
    errorMessage.value = ''
    try {
      const params = {}
      if (esSupervisorOAdmin.value) {
        if (selectedAgenteId.value) {
          params.idAgente = Number(selectedAgenteId.value)
        }
      } else {
        params.idAgente = authStore.user?.idUsuario || -1
      }

      const data = await expedienteService.getExpedientes(params)
      expedientes.value = data || []
    } catch (err) {
      errorMessage.value = err.response?.data?.message || 'Error al conectar con el servicio de expedientes.'
      console.error('Error cargando bandeja de agente aduanero:', err)
    } finally {
      loading.value = false
    }
  }

  const openExpedientes = computed(() =>
    (expedientes.value || []).filter((exp) => !isExpedienteCerrado(exp))
  )

  const kpiCounts = computed(() => {
    const list = expedientes.value || []
    let transito = 0
    let inspeccion = 0
    let levante = 0
    let cerrados = 0

    for (const exp of list) {
      if (belongsToAgentePhase(exp, 'transito')) transito++
      if (belongsToAgentePhase(exp, 'inspeccion')) inspeccion++
      if (belongsToAgentePhase(exp, 'levante')) levante++
      if (belongsToAgentePhase(exp, 'cerrados')) cerrados++
    }

    return {
      todos: list.length,
      transito,
      inspeccion,
      levante,
      cerrados
    }
  })

  const gestionKpiCounts = computed(() => {
    const list = openExpedientes.value
    let transito = 0
    let inspeccion = 0
    let levante = 0

    for (const exp of list) {
      if (belongsToAgentePhase(exp, 'transito')) transito++
      if (belongsToAgentePhase(exp, 'inspeccion')) inspeccion++
      if (belongsToAgentePhase(exp, 'levante')) levante++
    }

    return {
      todos: list.length,
      transito,
      inspeccion,
      levante,
      cerrados: 0
    }
  })

  const hasActiveFilters = computed(() => {
    return Boolean(
      filterRegimen.value ||
      filterCanal.value ||
      searchQuery.value?.trim() ||
      sortBy.value !== 'RECENT'
    )
  })

  const resetFilters = () => {
    filterRegimen.value = ''
    filterCanal.value = ''
    searchQuery.value = ''
    sortBy.value = 'RECENT'
  }

  const filteredExpedientes = computed(() => {
    const q = searchQuery.value?.toLowerCase().trim()
    const regimen = filterRegimen.value
    const canal = filterCanal.value
    const phase = phaseFilter.value
    const baseList = activeView.value === 'gestion' ? openExpedientes.value : (expedientes.value || [])

    const list = baseList.filter((exp) => {
      if (!belongsToAgentePhase(exp, phase)) {
        return false
      }

      if (regimen) {
        const regStr = (exp.regimenAduanero || '').toUpperCase()
        if (regimen === 'EXPORTACION' && !regStr.includes('EXPORT')) return false
        if (regimen === 'IMPORTACION' && !regStr.includes('IMPORT')) return false
      }

      if (canal) {
        const expCanal = exp.canalSunat || 'PENDIENTE'
        if (expCanal !== canal) return false
      }

      if (q) {
        const match =
          exp.codigoExpediente?.toLowerCase().includes(q) ||
          exp.codigoCotizacion?.toLowerCase().includes(q) ||
          exp.clienteRazonSocial?.toLowerCase().includes(q) ||
          exp.clienteNumeroDocumento?.toLowerCase().includes(q) ||
          exp.numeroDam?.toLowerCase().includes(q) ||
          exp.numeroContenedor?.toLowerCase().includes(q) ||
          exp.naveViaje?.toLowerCase().includes(q) ||
          exp.agenteAduaneroNombre?.toLowerCase().includes(q)
        if (!match) return false
      }

      return true
    })

    const sorted = [...list]
    if (sortBy.value === 'CODE_ASC') {
      sorted.sort((a, b) => (a.codigoExpediente || '').localeCompare(b.codigoExpediente || ''))
    } else if (sortBy.value === 'OLDEST') {
      sorted.sort((a, b) => {
        const ta = new Date(a.fechaApertura || 0).getTime() || a.idExpediente || 0
        const tb = new Date(b.fechaApertura || 0).getTime() || b.idExpediente || 0
        return ta - tb
      })
    } else {
      sorted.sort((a, b) => {
        const ta = new Date(a.fechaApertura || 0).getTime() || a.idExpediente || 0
        const tb = new Date(b.fechaApertura || 0).getTime() || b.idExpediente || 0
        return tb - ta
      })
    }

    return sorted
  })

  const cambiarVista = (target) => {
    if (!target || target === 'dashboard') {
      activeView.value = 'dashboard'
      return
    }
    if (target === 'gestion') {
      activeView.value = 'gestion'
      if (phaseFilter.value === 'cerrados') {
        phaseFilter.value = 'todos'
      }
      return
    }
    activeView.value = 'expedientes'
    if (target === 'expedientes') {
      phaseFilter.value = 'todos'
    } else if (PHASE_FILTERS.includes(target)) {
      phaseFilter.value = target
    } else {
      phaseFilter.value = 'todos'
    }
  }

  return {
    loading,
    errorMessage,
    expedientes,
    openExpedientes,
    agentesList,
    activeView,
    phaseFilter,
    filterRegimen,
    filterCanal,
    searchQuery,
    sortBy,
    selectedAgenteId,
    esSupervisorOAdmin,
    hasActiveFilters,
    resetFilters,
    kpiCounts,
    gestionKpiCounts,
    filteredExpedientes,
    loadAgentes,
    loadData,
    cambiarVista
  }
}
