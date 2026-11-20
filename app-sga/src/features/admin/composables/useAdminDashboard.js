import { ref, computed } from 'vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { ROLE_LABELS } from '@/core/auth/roles'
import { sistemaService } from '../services/sistemaService'
import { userService } from '../users/services/userService'
import { formatBytes, formatUptime } from '../utils/adminFormatters'

export { formatBytes, formatUptime }

export function useAdminDashboard() {
  const authStore = useAuthStore()

  const loading = ref(false)
  const filterDias = ref(7)
  const filterModulo = ref('TODOS')
  const pingLatency = ref(0)
  const lastCheckedTime = ref('--:--:--')
  const dashboardData = ref(null)
  const storageDiag = ref(null)

  const diasOptions = [
    { label: 'Hoy (24h)', value: 1 },
    { label: 'Últimos 7 días', value: 7 },
    { label: 'Últimos 14 días', value: 14 },
    { label: 'Últimos 30 días', value: 30 }
  ]

  const moduloOptions = [
    { label: 'Todos los módulos', value: 'TODOS' },
    { label: 'Seguridad TI', value: 'SEGURIDAD_TI' },
    { label: 'Comercial', value: 'COMERCIAL' },
    { label: 'Aduanas', value: 'ADUANAS' },
    { label: 'Finanzas', value: 'FINANZAS' },
    { label: 'Almacén', value: 'ALMACEN' },
    { label: 'Transporte', value: 'TRANSPORTE' },
    { label: 'Portal B2B', value: 'PORTAL_B2B' },
    { label: 'Documentos', value: 'DOCUMENTOS' },
    { label: 'Maestros', value: 'MAESTROS' },
    { label: 'Logs TI', value: 'LOGS_TI' }
  ]

  const loadDashboard = async () => {
    loading.value = true
    const start = performance.now()
    try {
      const storagePromise =
        typeof sistemaService.getAlmacenamiento === 'function'
          ? sistemaService.getAlmacenamiento({ page: 0, size: 1 }).catch(() => null)
          : Promise.resolve(null)

      if (authStore.isAdministradorTI) {
        const [dashRes, storRes] = await Promise.all([
          sistemaService.getDashboard({
            dias: filterDias.value,
            modulo: filterModulo.value
          }),
          storagePromise
        ])
        dashboardData.value = dashRes
        storageDiag.value = storRes
      } else {
        const [estado, users, storRes] = await Promise.all([
          sistemaService.getEstado(),
          userService.getUsers(false),
          storagePromise
        ])
        storageDiag.value = storRes
        const distRoles = {}
        ;(users || []).forEach((u) => {
          const r = u.nombreRol || 'SIN_ROL'
          distRoles[r] = (distRoles[r] || 0) + 1
        })
        dashboardData.value = {
          sistema: estado,
          usuariosTotal: (users || []).length,
          usuariosActivos: (users || []).filter((u) => u.estadoActivo).length,
          usuariosInactivos: (users || []).filter((u) => !u.estadoActivo).length,
          distribucionRoles: distRoles,
          portalUsuariosTotal: 0,
          portalUsuariosActivos: 0,
          portalEmpresasVinculadas: 0,
          logArchivoNombre: 'sga-backend.log',
          logSizeBytes: 0,
          logTotalLineas: 0,
          logErroresLineas: 0,
          diasPeriodo: filterDias.value,
          auditoriaTotalPeriodo: 0,
          auditoriaExitosPeriodo: 0,
          auditoriaFallosPeriodo: 0,
          loginsFallidosPeriodo: 0,
          actividadDiaria: [],
          eventosRecientes: []
        }
      }
      pingLatency.value = Math.max(1, Math.round(performance.now() - start))
      lastCheckedTime.value = new Date().toLocaleTimeString()
    } finally {
      loading.value = false
    }
  }

  const sistema = computed(() => dashboardData.value?.sistema || {})
  const isBackendHealthy = computed(() => Boolean(sistema.value?.bdConectada))

  const rolesChartData = computed(() => {
    const dist = dashboardData.value?.distribucionRoles || {}
    const entries = Object.entries(dist).sort((a, b) => b[1] - a[1])
    return {
      labels: entries.map(([rol]) => ROLE_LABELS[rol] || rol),
      datasets: [
        {
          data: entries.map(([, count]) => count)
        }
      ]
    }
  })

  const actividadChartData = computed(() => {
    const serie = dashboardData.value?.actividadDiaria || []
    return {
      labels: serie.map((item) => item.fecha?.slice(5) || item.fecha),
      datasets: [
        {
          label: 'Exitosos',
          data: serie.map((item) => item.exitos || 0)
        },
        {
          label: 'Fallidos / Alertas',
          data: serie.map((item) => item.fallos || 0)
        }
      ]
    }
  })

  const activeUsersPct = computed(() => {
    const total = dashboardData.value?.usuariosTotal || 0
    if (!total) return 0
    return Math.round(((dashboardData.value?.usuariosActivos || 0) * 100) / total)
  })

  const storageEntidadChartData = computed(() => {
    const list = storageDiag.value?.porEntidad || []
    return {
      labels: list.map((g) => g.etiqueta),
      datasets: [
        {
          label: 'Archivos activos',
          data: list.map((g) => g.cantidad)
        }
      ]
    }
  })

  const storageExtensionChartData = computed(() => {
    const list = (storageDiag.value?.porExtension || []).slice(0, 7)
    return {
      labels: list.map((g) => `${g.clave} (${g.cantidad})`),
      datasets: [
        {
          data: list.map((g) => g.tamanoBytes)
        }
      ]
    }
  })

  const hasActiveFilters = computed(() => filterDias.value !== 7 || filterModulo.value !== 'TODOS')

  const resetFilters = () => {
    filterDias.value = 7
    filterModulo.value = 'TODOS'
  }

  return {
    loading,
    filterDias,
    filterModulo,
    diasOptions,
    moduloOptions,
    pingLatency,
    lastCheckedTime,
    dashboardData,
    storageDiag,
    sistema,
    isBackendHealthy,
    rolesChartData,
    actividadChartData,
    storageEntidadChartData,
    storageExtensionChartData,
    activeUsersPct,
    hasActiveFilters,
    resetFilters,
    loadDashboard
  }
}

export default useAdminDashboard
