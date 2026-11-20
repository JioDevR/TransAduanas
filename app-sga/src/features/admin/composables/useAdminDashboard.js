import { ref, computed } from 'vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { ROLE_LABELS } from '@/core/auth/roles'
import { sistemaService } from '../services/sistemaService'
import { userService } from '../users/services/userService'

export function formatBytes(bytes = 0) {
  const val = Number(bytes || 0)
  if (val < 1024) return `${val} B`
  if (val < 1024 * 1024) return `${(val / 1024).toFixed(1)} KB`
  if (val < 1024 * 1024 * 1024) return `${(val / (1024 * 1024)).toFixed(2)} MB`
  return `${(val / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

export function formatUptime(seconds = 0) {
  const s = Math.max(0, Number(seconds || 0))
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d > 0) return `${d}d ${h}h ${m}m`
  if (h > 0) return `${h}h ${m}m`
  return `${m}m ${s % 60}s`
}

export function useAdminDashboard() {
  const authStore = useAuthStore()

  const loading = ref(false)
  const filterDias = ref(7)
  const filterModulo = ref('TODOS')
  const pingLatency = ref(0)
  const lastCheckedTime = ref('--:--:--')
  const dashboardData = ref(null)

  const diasOptions = [
    { label: 'Hoy (24h)', value: 1 },
    { label: 'Últimos 7 días', value: 7 },
    { label: 'Últimos 14 días', value: 14 },
    { label: 'Últimos 30 días', value: 30 }
  ]

  const moduloOptions = [
    { label: 'Todos los módulos', value: 'TODOS' },
    { label: 'Seguridad TI', value: 'SEGURIDAD_TI' },
    { label: 'Portal B2B', value: 'PORTAL_B2B' },
    { label: 'Logs TI', value: 'LOGS_TI' }
  ]

  const loadDashboard = async () => {
    loading.value = true
    const start = performance.now()
    try {
      if (authStore.isAdministradorTI) {
        dashboardData.value = await sistemaService.getDashboard({
          dias: filterDias.value,
          modulo: filterModulo.value
        })
      } else {
        const [estado, users] = await Promise.all([
          sistemaService.getEstado(),
          userService.getUsers(false)
        ])
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
    sistema,
    isBackendHealthy,
    rolesChartData,
    actividadChartData,
    activeUsersPct,
    hasActiveFilters,
    resetFilters,
    loadDashboard
  }
}

export default useAdminDashboard
