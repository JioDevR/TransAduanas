import { ref, computed } from 'vue'
import { transporteFlotaService } from '../services/transporteFlotaService'
import { buildAlertasMtc, buildTransporteKpis } from '../utils/transporteAlerts'
import { useNotification } from '@/shared/composables/useNotification'

export function useTransporteData() {
  const { showError } = useNotification()

  const despachosList = ref([])
  const unidadesList = ref([])
  const conductoresList = ref([])
  const loading = ref(false)

  const alertasMtc = computed(() => buildAlertasMtc(unidadesList.value, conductoresList.value))
  const kpis = computed(() =>
    buildTransporteKpis(despachosList.value, unidadesList.value, conductoresList.value, alertasMtc.value)
  )

  const loadAllData = async () => {
    loading.value = true
    try {
      const [despachos, unidades, conductores] = await Promise.all([
        transporteFlotaService.getDespachos(),
        transporteFlotaService.getUnidades({ soloActivos: false }),
        transporteFlotaService.getConductores(false)
      ])
      despachosList.value = Array.isArray(despachos) ? despachos : []
      unidadesList.value = Array.isArray(unidades) ? unidades : []
      conductoresList.value = Array.isArray(conductores) ? conductores : []
    } catch (err) {
      showError(err?.response?.data?.message || err?.message || 'Error al sincronizar datos de Transporte y Flota')
    } finally {
      loading.value = false
    }
  }

  return {
    despachosList,
    unidadesList,
    conductoresList,
    loading,
    alertasMtc,
    kpis,
    loadAllData
  }
}
