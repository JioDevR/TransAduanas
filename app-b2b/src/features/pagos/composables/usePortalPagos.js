import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { portalPagoService } from '../services/portalPagoService'
import { isCotizacionObligacion } from '../utils/pagoFormUtils'
import { useNotification } from '@/shared/composables/useNotification'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'

/**
 * Composable de estado y filtros para Cobros y Pagos del Portal B2B.
 */
export function usePortalPagos() {
  const router = useRouter()
  const route = useRoute()
  const { showError, showWarn } = useNotification()

  const obligaciones = ref([])
  const anticipos = ref([])
  const liquidaciones = ref([])
  const facturas = ref([])
  const requestedSection = ref(null)
  const loading = ref(true)

  // Filtros de Cuentas por Pagar
  const filtroTipoObligacion = ref('TODOS')
  const filtroObligacionTexto = ref('')

  // Filtros de Historial
  const filtroHistorialTexto = ref('')
  const filtroHistorialEstado = ref('')

  const cotizacionesPreExpediente = computed(() =>
    obligaciones.value.filter(o => isCotizacionObligacion(o.tipoObligacion))
  )

  const expedientesHeredados = computed(() =>
    obligaciones.value.filter(o => o.tipoObligacion === 'EXPEDIENTE' || o.tipoObligacion === 'EXPEDIENTE_HEREDADO')
  )

  const hasActiveObligacionFilters = computed(() =>
    Boolean(filtroObligacionTexto.value || filtroTipoObligacion.value !== 'TODOS')
  )

  function resetObligacionFilters() {
    filtroObligacionTexto.value = ''
    filtroTipoObligacion.value = 'TODOS'
  }

  const obligacionesFiltradas = computed(() => {
    return obligaciones.value.filter(o => {
      if (filtroTipoObligacion.value === 'COTIZACION' && !isCotizacionObligacion(o.tipoObligacion)) {
        return false
      }
      if (
        filtroTipoObligacion.value === 'EXPEDIENTE' &&
        o.tipoObligacion !== 'EXPEDIENTE' &&
        o.tipoObligacion !== 'EXPEDIENTE_HEREDADO'
      ) {
        return false
      }
      const query = filtroObligacionTexto.value.toLowerCase().trim()
      if (query) {
        const matchCod = o.codigo?.toLowerCase().includes(query)
        const matchDesc = o.descripcion?.toLowerCase().includes(query)
        const matchDam = o.numeroDam?.toLowerCase().includes(query)
        return matchCod || matchDesc || matchDam
      }
      return true
    })
  })

  const anticiposObservados = computed(() =>
    anticipos.value.filter(a => a.estadoPago === 'OBSERVADO' || a.estadoPago === 'RECHAZADO')
  )

  const hasActiveHistorialFilters = computed(() =>
    Boolean(filtroHistorialTexto.value || filtroHistorialEstado.value)
  )

  function resetHistorialFilters() {
    filtroHistorialTexto.value = ''
    filtroHistorialEstado.value = ''
  }

  const anticiposFiltrados = computed(() => {
    return anticipos.value.filter(p => {
      if (filtroHistorialEstado.value) {
        if (
          filtroHistorialEstado.value === 'OBSERVADO' &&
          p.estadoPago !== 'OBSERVADO' &&
          p.estadoPago !== 'RECHAZADO'
        ) {
          return false
        }
        if (filtroHistorialEstado.value === 'VALIDADO') {
          const validados = ['APROBADO', 'ADELANTO_APROBADO', 'PAGADO_TOTAL', 'VALIDADO']
          if (!validados.includes(p.estadoPago)) return false
        } else if (
          filtroHistorialEstado.value !== 'OBSERVADO' &&
          p.estadoPago !== filtroHistorialEstado.value
        ) {
          return false
        }
      }
      const query = filtroHistorialTexto.value.toLowerCase().trim()
      if (query) {
        const matchCod = p.codigoPago?.toLowerCase().includes(query)
        const matchCot = p.codigoCotizacion?.toLowerCase().includes(query)
        const matchOp = p.numeroOperacion?.toLowerCase().includes(query)
        return matchCod || matchCot || matchOp
      }
      return true
    })
  })

  function verificarQuerySubsanacion() {
    if (route.query.tab) {
      const tabParam = String(route.query.tab)
      if (tabParam === 'observados' || tabParam === 'historial') {
        requestedSection.value = tabParam
      }
    }

    if (route.query.subsanarPago) {
      const idPago = Number(route.query.subsanarPago)
      router.push(`/pagos/subsanar/${idPago}`)
      return
    }

    if (route.query.idCotizacion) {
      const idCot = Number(route.query.idCotizacion)
      const pago = anticipos.value.find(
        p => p.idCotizacion === idCot && (p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO')
      )
      if (pago) {
        router.push(`/pagos/subsanar/${pago.idPago}`)
      } else {
        requestedSection.value = 'observados'
      }
    }
  }

  watch(() => route.query, verificarQuerySubsanacion)

  async function cargarTodo() {
    loading.value = true
    try {
      const [dataObligaciones, dataAnticipos, dataFacturas, dataLiquidaciones] = await Promise.all([
        portalPagoService.listarObligaciones(),
        portalPagoService.listarAnticipos(),
        typeof portalPagoService.listarFacturas === 'function'
          ? portalPagoService.listarFacturas().catch(() => [])
          : Promise.resolve([]),
        typeof portalPagoService.listarLiquidaciones === 'function'
          ? portalPagoService.listarLiquidaciones().catch(() => [])
          : Promise.resolve([])
      ])

      obligaciones.value = dataObligaciones || []
      anticipos.value = dataAnticipos || []
      facturas.value = dataFacturas || []
      liquidaciones.value = dataLiquidaciones || []

      verificarQuerySubsanacion()
    } catch (err) {
      showError(err.response?.data?.message || 'Error al cargar información financiera')
    } finally {
      loading.value = false
    }
  }

  function subsanarAbonoCotizacion(o) {
    if (!o) return
    const idCot = o.idCotizacion || o.idReferencia
    const pago = anticipos.value.find(
      p => p.idCotizacion === idCot && (p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO')
    )
    if (pago) {
      router.push(`/pagos/subsanar/${pago.idPago}`)
    } else {
      requestedSection.value = 'observados'
      showWarn('Localizando abono en la bandeja de correcciones...')
    }
  }

  function irASubsanarPago(idPago) {
    if (!idPago) return
    router.push(`/pagos/subsanar/${idPago}`)
  }

  function irARegistrarPago(tipo, id) {
    if (tipo && id) {
      router.push({ path: '/pagos/registrar', query: { tipo, id } })
    } else {
      router.push('/pagos/registrar')
    }
  }

  async function verComprobante(idDocumento) {
    try {
      const blob = await portalPagoService.previsualizarComprobante(idDocumento)
      openBlobInNewTab(blob, 'application/pdf')
    } catch {
      showError('No se pudo visualizar el comprobante de pago')
    }
  }

  async function verFacturaPdf(idComprobante, numeroCompleto = '') {
    if (!idComprobante) return
    try {
      const blob = await portalPagoService.descargarFacturaPdf(idComprobante, { download: false })
      const opened = openBlobInNewTab(blob, 'application/pdf')
      if (!opened) {
        downloadBlob(blob, `${numeroCompleto || 'factura-' + idComprobante}.pdf`, 'application/pdf')
      }
    } catch {
      showError('No se pudo generar la vista previa de la factura')
    }
  }

  async function descargarFacturaPdf(idComprobante, numeroCompleto = '') {
    if (!idComprobante) return
    try {
      const blob = await portalPagoService.descargarFacturaPdf(idComprobante, { download: true })
      downloadBlob(blob, `${numeroCompleto || 'factura-' + idComprobante}.pdf`, 'application/pdf')
    } catch {
      showError('No se pudo descargar el PDF de la factura')
    }
  }

  return {
    obligaciones,
    anticipos,
    liquidaciones,
    facturas,
    requestedSection,
    loading,
    filtroTipoObligacion,
    filtroObligacionTexto,
    filtroHistorialTexto,
    filtroHistorialEstado,
    cotizacionesPreExpediente,
    expedientesHeredados,
    hasActiveObligacionFilters,
    obligacionesFiltradas,
    anticiposObservados,
    hasActiveHistorialFilters,
    anticiposFiltrados,
    resetObligacionFilters,
    resetHistorialFilters,
    cargarTodo,
    subsanarAbonoCotizacion,
    irASubsanarPago,
    irARegistrarPago,
    verComprobante,
    verFacturaPdf,
    descargarFacturaPdf
  }
}
