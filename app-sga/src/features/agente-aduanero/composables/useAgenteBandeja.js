import { ref, computed } from 'vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { expedienteService } from '@/shared/services/expedienteService'
import { userService } from '@/features/admin/users/services/userService'
import { belongsToAgentePhase } from '../utils/agenteWorkflowUtils'

export function useAgenteBandeja() {
  const authStore = useAuthStore()

  const loading = ref(false)
  const errorMessage = ref('')
  const expedientes = ref([])
  const agentesList = ref([])

  // Pestaña activa ('dashboard' | 'todos' | 'transito' | 'inspeccion' | 'levante')
  const activeTab = ref('dashboard')

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

  const kpiCounts = computed(() => {
    const list = expedientes.value || []
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
      levante
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
    const tab = activeTab.value

    const list = (expedientes.value || []).filter((exp) => {
      if (tab !== 'dashboard' && !belongsToAgentePhase(exp, tab)) {
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

  const cambiarTab = (tabKey) => {
    activeTab.value = tabKey === 'expedientes' ? 'todos' : (tabKey || 'dashboard')
  }

  return {
    loading,
    errorMessage,
    expedientes,
    agentesList,
    activeTab,
    filterRegimen,
    filterCanal,
    searchQuery,
    sortBy,
    selectedAgenteId,
    esSupervisorOAdmin,
    hasActiveFilters,
    resetFilters,
    kpiCounts,
    filteredExpedientes,
    loadAgentes,
    loadData,
    cambiarTab
  }
}
