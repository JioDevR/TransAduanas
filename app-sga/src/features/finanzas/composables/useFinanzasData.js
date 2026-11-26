import { ref, computed } from 'vue'
import finanzasService from '../services/finanzasService'
import { expedienteService } from '../services/expedienteService'
import { useNotification } from '@/shared/composables/useNotification'

const DEFAULT_METRICAS = () => ({
  totalCobradoPen: 0,
  totalCobradoUsd: 0,
  cuentasPorCobrarPen: 0,
  cuentasPorCobrarUsd: 0,
  totalTributosSunatPen: 0,
  totalDetraccionesPen: 0,
  totalLiquidaciones: 0,
  liquidacionesPendientes: 0,
  liquidacionesPagadas: 0,
  liquidacionesAnuladas: 0,
  tipoCambioOficial: 3.75
})

/**
 * Composable centralizado para la carga, refresco y estado compartido
 * del módulo de Finanzas y Tesorería.
 */
export function useFinanzasData() {
  const { showError } = useNotification()

  const liquidacionesList = ref([])
  const expedientesList = ref([])
  const anticiposList = ref([])
  const cotizacionesAprobadas = ref([])
  const facturasList = ref([])
  const metricas = ref(DEFAULT_METRICAS())

  const loadingLiquidaciones = ref(false)
  const loadingExpedientes = ref(false)
  const loadingAnticipos = ref(false)
  const loadingCotizaciones = ref(false)
  const loadingFacturas = ref(false)
  const loadingMetricas = ref(false)

  const loading = computed(
    () =>
      loadingLiquidaciones.value ||
      loadingExpedientes.value ||
      loadingAnticipos.value ||
      loadingCotizaciones.value ||
      loadingFacturas.value ||
      loadingMetricas.value
  )

  const expedientesPorLiquidar = computed(() => {
    const idsConLiquidacion = new Set(
      liquidacionesList.value
        .filter((l) => String(l.estadoPago || '').toUpperCase() !== 'ANULADO')
        .map((l) => l.idExpediente)
        .filter(Boolean)
    )
    return expedientesList.value.filter((e) => !idsConLiquidacion.has(e.idExpediente))
  })

  const pagosRealizadosList = computed(() =>
    liquidacionesList.value.filter((l) => String(l.estadoPago || '').toUpperCase() === 'PAGADO')
  )

  const liquidacionesEnConciliacion = computed(() =>
    liquidacionesList.value.filter((l) => String(l.estadoPago || '').toUpperCase() === 'EN_CONCILIACION')
  )

  const abonosSinFactura = computed(() => {
    const idsFacturados = new Set(
      facturasList.value
        .filter((f) => String(f.estado || '').toUpperCase() === 'EMITIDA' && f.idPagoCotizacion)
        .map((f) => f.idPagoCotizacion)
    )
    return anticiposList.value.filter(
      (a) =>
        String(a.estadoPago || '').toUpperCase() === 'APROBADO' &&
        !a.idComprobanteFactura &&
        !idsFacturados.has(a.idPago)
    )
  })

  async function loadLiquidaciones() {
    loadingLiquidaciones.value = true
    try {
      const data = await finanzasService.getLiquidaciones()
      liquidacionesList.value = Array.isArray(data) ? data : []
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudieron cargar las liquidaciones financieras.')
    } finally {
      loadingLiquidaciones.value = false
    }
  }

  async function loadMetricas() {
    loadingMetricas.value = true
    try {
      const data = await finanzasService.getMetricas()
      if (data) {
        metricas.value = { ...DEFAULT_METRICAS(), ...data }
      }
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudieron cargar las métricas financieras.')
    } finally {
      loadingMetricas.value = false
    }
  }

  async function loadExpedientes() {
    loadingExpedientes.value = true
    try {
      const data = await expedienteService.getExpedientes()
      expedientesList.value = Array.isArray(data) ? data : []
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudieron cargar los expedientes aduaneros.')
    } finally {
      loadingExpedientes.value = false
    }
  }

  async function loadAnticipos() {
    loadingAnticipos.value = true
    try {
      const data = await finanzasService.getAnticipos()
      anticiposList.value = Array.isArray(data) ? data : []
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudieron cargar los abonos B2B.')
    } finally {
      loadingAnticipos.value = false
    }
  }

  async function loadCotizacionesAprobadas() {
    loadingCotizaciones.value = true
    try {
      const data = await finanzasService.getCotizacionesAprobadas()
      cotizacionesAprobadas.value = Array.isArray(data) ? data : []
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudieron cargar las cotizaciones aprobadas.')
    } finally {
      loadingCotizaciones.value = false
    }
  }

  async function loadFacturas() {
    loadingFacturas.value = true
    try {
      const data = await finanzasService.getFacturas()
      facturasList.value = Array.isArray(data) ? data : []
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudieron cargar los comprobantes emitidos.')
    } finally {
      loadingFacturas.value = false
    }
  }

  async function loadAllData() {
    await Promise.all([
      loadLiquidaciones(),
      loadMetricas(),
      loadExpedientes(),
      loadAnticipos(),
      loadCotizacionesAprobadas(),
      loadFacturas()
    ])
  }

  async function refreshPreOperativeData() {
    await Promise.all([loadAnticipos(), loadCotizacionesAprobadas(), loadMetricas(), loadFacturas()])
  }

  async function refreshLiquidacionesData() {
    await Promise.all([loadLiquidaciones(), loadMetricas(), loadExpedientes(), loadFacturas()])
  }

  return {
    liquidacionesList,
    expedientesList,
    anticiposList,
    cotizacionesAprobadas,
    facturasList,
    metricas,
    loading,
    loadingLiquidaciones,
    loadingExpedientes,
    loadingAnticipos,
    loadingCotizaciones,
    loadingFacturas,
    expedientesPorLiquidar,
    pagosRealizadosList,
    liquidacionesEnConciliacion,
    abonosSinFactura,
    loadAllData,
    loadLiquidaciones,
    loadMetricas,
    loadExpedientes,
    loadAnticipos,
    loadCotizacionesAprobadas,
    loadFacturas,
    refreshPreOperativeData,
    refreshLiquidacionesData
  }
}
