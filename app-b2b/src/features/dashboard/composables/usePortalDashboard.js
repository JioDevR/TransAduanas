import { ref, computed } from 'vue'
import { useAuthStore } from '@/core/stores/authStore'
import { portalDashboardService } from '../services/portalDashboardService'
import {
  computeCotizacionStats,
  computeExpedienteStats,
  computeCobrosStats
} from '../utils/dashboardMetrics'

/**
 * Composable de orquestación para el Dashboard B2B.
 */
export function usePortalDashboard() {
  const authStore = useAuthStore()

  const loading = ref(false)
  const perfil = ref(null)
  const cotizaciones = ref([])
  const expedientes = ref([])
  const resumenPagos = ref(null)
  const obligaciones = ref([])
  const anticipos = ref([])
  const partialErrors = ref({})
  const lastUpdatedAt = ref(null)

  const empresaInfo = computed(() => {
    const p = perfil.value || {}
    const u = authStore.user || {}

    return {
      razonSocial: p.razonSocial || authStore.empresaNombre || 'Empresa Cliente',
      ruc: p.ruc || authStore.ruc || '—',
      representante:
        p.contactoRepresentante ||
        [p.nombres, p.apellidos].filter(Boolean).join(' ') ||
        authStore.usuarioNombre ||
        'Representante B2B',
      cargo: p.cargoTitulo || 'Acceso Cliente B2B',
      direccionFiscal: p.direccionFiscal || 'No registrada',
      correoContacto: p.correoContacto || p.correoInstitucional || u.correoElectronico || '—',
      telefonoContacto: p.telefonoContacto || p.telefono || '—'
    }
  })

  const cotizacionStats = computed(() => computeCotizacionStats(cotizaciones.value))
  const expedienteStats = computed(() => computeExpedienteStats(expedientes.value))
  const cobrosStats = computed(() =>
    computeCobrosStats({
      resumenPagos: resumenPagos.value,
      obligaciones: obligaciones.value,
      anticipos: anticipos.value
    })
  )

  const alertasTotales = computed(() => {
    return (
      cotizacionStats.value.observada +
      cobrosStats.value.pagosObservados +
      expedienteStats.value.alertaAlmacen
    )
  })

  async function loadDashboard() {
    loading.value = true
    try {
      const data = await portalDashboardService.cargarDatosDashboard()
      perfil.value = data.perfil
      cotizaciones.value = data.cotizaciones
      expedientes.value = data.expedientes
      resumenPagos.value = data.resumenPagos
      obligaciones.value = data.obligaciones
      anticipos.value = data.anticipos
      partialErrors.value = data.errors || {}
      lastUpdatedAt.value = new Date()
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    partialErrors,
    lastUpdatedAt,
    empresaInfo,
    cotizacionStats,
    expedienteStats,
    cobrosStats,
    alertasTotales,
    loadDashboard
  }
}
