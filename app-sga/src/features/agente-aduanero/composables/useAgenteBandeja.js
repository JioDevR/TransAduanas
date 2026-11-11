import { ref, computed } from 'vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { expedienteService } from '@/shared/services/expedienteService'
import { userService } from '@/features/admin/users/services/userService'

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
  const selectedAgenteId = ref(null)

  // Roles que tienen privilegios de supervisión global sobre las bandejas
  const esSupervisorOAdmin = computed(() => {
    const rol = authStore.user?.rol
    return rol === 'ADMINISTRADOR_TI' || rol === 'GERENTE_GENERAL' || rol === 'SUPERVISOR_ADUANERO'
  })

  // Carga de catálogo de agentes para selector de supervisión
  const loadAgentes = async () => {
    if (!esSupervisorOAdmin.value) return
    try {
      const agentes = await userService.getAgentesAduaneros()
      agentesList.value = agentes || []
    } catch (e) {
      console.error('Error cargando padrón de agentes aduaneros:', e)
    }
  }

  // Carga principal de expedientes
  const loadData = async () => {
    loading.value = true
    errorMessage.value = ''
    try {
      let params = {}
      if (esSupervisorOAdmin.value) {
        if (selectedAgenteId.value) {
          params.idAgente = Number(selectedAgenteId.value)
        }
      } else {
        const idAgente = authStore.user?.idUsuario || -1
        params.idAgente = idAgente
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

  // Métricas dinámicas calculadas para los KPIs
  const kpiCounts = computed(() => {
    const list = expedientes.value || []
    let transito = 0
    let inspeccion = 0
    let levante = 0

    for (const exp of list) {
      const est = exp.estadoExpediente
      const canal = exp.canalSunat

      if (est === 'ASIGNADO' || est === 'EN_TRAMITE') {
        transito++
      }

      if (canal === 'ROJO' || canal === 'NARANJA' || est === 'INSPECCION') {
        inspeccion++
      }

      if (est === 'LEVANTE_AUTORIZADO' || !!exp.fechaLevanteAutorizacion) {
        levante++
      }
    }

    return {
      todos: list.length,
      transito,
      inspeccion,
      levante
    }
  })

  // Filtrado reactivo integral
  const filteredExpedientes = computed(() => {
    const q = searchQuery.value?.toLowerCase().trim()
    const regimen = filterRegimen.value
    const canal = filterCanal.value
    const tab = activeTab.value

    return (expedientes.value || []).filter((exp) => {
      // 1. Filtro por fase / pestaña activa
      if (tab === 'transito') {
        if (exp.estadoExpediente !== 'ASIGNADO' && exp.estadoExpediente !== 'EN_TRAMITE') return false
      } else if (tab === 'inspeccion') {
        const enAforo = exp.canalSunat === 'ROJO' || exp.canalSunat === 'NARANJA' || exp.estadoExpediente === 'INSPECCION'
        if (!enAforo) return false
      } else if (tab === 'levante') {
        const conLevante = exp.estadoExpediente === 'LEVANTE_AUTORIZADO' || !!exp.fechaLevanteAutorizacion
        if (!conLevante) return false
      }

      // 2. Filtro por Régimen
      if (regimen) {
        const regStr = (exp.regimenAduanero || '').toUpperCase()
        if (regimen === 'EXPORTACION' && !regStr.includes('EXPORT')) return false
        if (regimen === 'IMPORTACION' && !regStr.includes('IMPORT')) return false
      }

      // 3. Filtro por Canal SUNAT
      if (canal && exp.canalSunat !== canal) {
        return false
      }

      // 4. Búsqueda por texto libre
      if (q) {
        const match =
          exp.codigoExpediente?.toLowerCase().includes(q) ||
          exp.codigoCotizacion?.toLowerCase().includes(q) ||
          exp.clienteRazonSocial?.toLowerCase().includes(q) ||
          exp.clienteNumeroDocumento?.toLowerCase().includes(q) ||
          exp.numeroDam?.toLowerCase().includes(q) ||
          exp.numeroContenedor?.toLowerCase().includes(q) ||
          exp.naveViaje?.toLowerCase().includes(q)
        if (!match) return false
      }

      return true
    })
  })

  const cambiarTab = (tabKey) => {
    activeTab.value = tabKey
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
    selectedAgenteId,
    esSupervisorOAdmin,
    kpiCounts,
    filteredExpedientes,
    loadAgentes,
    loadData,
    cambiarTab
  }
}
