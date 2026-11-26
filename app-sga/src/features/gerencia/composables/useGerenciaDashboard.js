import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { openBlobInNewTab } from '@/shared/utils/download'
import sedeService from '@/features/admin/sedes/services/sedeService'
import catalogoService from '@/features/admin/catalogos/services/catalogoService'
import { transporteFlotaService } from '@/features/transporte/services/transporteFlotaService'
import { computeGerenciaDashboardMetrics, unwrapListPayload } from '../utils/gerenciaMetricsUtils'

const PDF_DOWNLOADERS = {
  sedes: {
    fn: () => sedeService.downloadPdf(),
    success: 'Reporte oficial de sedes abierto en nueva pestaña',
    error: 'No se pudo generar el reporte oficial de sedes'
  },
  puertos: {
    fn: () => catalogoService.downloadPuertosPdf(),
    success: 'Reporte oficial de puertos abierto en nueva pestaña',
    error: 'No se pudo generar el reporte oficial de puertos'
  },
  'tipos-carga': {
    fn: () => catalogoService.downloadTiposCargaPdf(),
    success: 'Reporte oficial de tipos de carga abierto en nueva pestaña',
    error: 'No se pudo generar el reporte oficial de tipos de carga'
  },
  mercancias: {
    fn: () => catalogoService.downloadMercanciasPdf(),
    success: 'Reporte oficial de partidas arancelarias abierto en nueva pestaña',
    error: 'No se pudo generar el reporte oficial de mercancías'
  }
}

export function useGerenciaDashboard() {
  const router = useRouter()
  const navigationStore = useNavigationStore()
  const { showSuccess, showError } = useNotification()

  const loading = ref(false)
  const exportingType = ref(null)
  const failedSources = ref([])
  const lastUpdatedAt = ref(null)

  const sedesList = ref([])
  const puertosList = ref([])
  const tiposCargaList = ref([])
  const mercanciasList = ref([])
  const unidadesList = ref([])
  const conductoresList = ref([])

  const metrics = computed(() =>
    computeGerenciaDashboardMetrics({
      sedes: sedesList.value,
      puertos: puertosList.value,
      tiposCarga: tiposCargaList.value,
      mercancias: mercanciasList.value,
      unidades: unidadesList.value,
      conductores: conductoresList.value
    })
  )

  const loadDashboardData = async () => {
    loading.value = true
    failedSources.value = []
    try {
      const descriptors = [
        { key: 'Sedes', target: sedesList, promise: sedeService.getSedes(false) },
        { key: 'Puertos', target: puertosList, promise: catalogoService.getPuertos(false) },
        { key: 'Tipos de Carga', target: tiposCargaList, promise: catalogoService.getTiposCarga(false) },
        { key: 'Mercancías', target: mercanciasList, promise: catalogoService.getMercancias(false) },
        {
          key: 'Flota',
          target: unidadesList,
          promise: transporteFlotaService.getUnidades({ soloActivos: false })
        },
        {
          key: 'Conductores',
          target: conductoresList,
          promise: transporteFlotaService.getConductores(false)
        }
      ]

      const results = await Promise.allSettled(descriptors.map((d) => d.promise))
      const failed = []

      results.forEach((res, idx) => {
        if (res.status === 'fulfilled') {
          descriptors[idx].target.value = unwrapListPayload(res.value)
        } else {
          descriptors[idx].target.value = []
          failed.push(descriptors[idx].key)
        }
      })

      failedSources.value = failed
      if (failed.length > 0) {
        showError(`No se pudieron actualizar: ${failed.join(', ')}`)
      }
      lastUpdatedAt.value = new Date().toLocaleTimeString('es-PE', {
        hour: '2-digit',
        minute: '2-digit'
      })
    } catch (err) {
      console.error('Error cargando métricas de Gerencia General:', err)
      showError('Error al consultar métricas ejecutivas de Gerencia General')
    } finally {
      loading.value = false
    }
  }

  const goToSubOption = (option) => {
    if (option === 'flota' || option === 'conductores') {
      const targetTab = option === 'flota' ? 'unidades' : 'conductores'
      navigationStore.setActiveModule('transporte-flota')
      navigationStore.setTransporteFlotaSubOption(targetTab)
      router.push(`/transporte-flota?tab=${targetTab}`)
      return
    }

    const pathMap = {
      dashboard: '/gerencia-general/dashboard',
      sedes: '/gerencia-general/sedes',
      puertos: '/gerencia-general/puertos',
      'tipos-carga': '/gerencia-general/tipos-carga',
      mercancias: '/gerencia-general/mercancias',
      catalogos: '/gerencia-general/puertos'
    }
    navigationStore.setActiveModule('gerencia-general')
    navigationStore.setGerenciaGeneralSubOption(option)
    router.push(pathMap[option] || '/gerencia-general/dashboard')
  }

  const exportCatalogoPdf = async (type) => {
    const config = PDF_DOWNLOADERS[type]
    if (!config) return
    exportingType.value = type
    try {
      const blob = await config.fn()
      openBlobInNewTab(blob)
      showSuccess(config.success)
    } catch (err) {
      console.error(`Error exportando PDF (${type}):`, err)
      showError(config.error)
    } finally {
      exportingType.value = null
    }
  }

  return {
    loading,
    exportingType,
    failedSources,
    lastUpdatedAt,
    sedesList,
    metrics,
    loadDashboardData,
    goToSubOption,
    exportCatalogoPdf
  }
}
