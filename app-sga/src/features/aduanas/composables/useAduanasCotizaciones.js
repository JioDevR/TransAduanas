import { ref, computed } from 'vue'
import { gestionAduaneraService } from '../services/gestionAduaneraService'
import { clasificarCotizacionesAduaneras } from '../utils/aduanasWorkflowUtils'
import { getCredencialesPostAprobacionNotification } from '../utils/aprobacionAduaneraUtils'

/**
 * Composable para la Bandeja de Cotizaciones en Gestión Aduanera.
 * Centraliza clasificación en sub-colas, filtrado, ordenamiento y acciones de revisión técnica.
 */
export function useAduanasCotizaciones({
  cotizaciones,
  authStore,
  notify,
  confirmAction,
  onReload
}) {
  const subQueueCotizaciones = ref(authStore?.canSwitchModules ? 'todas' : 'nuevas')
  const searchQuery = ref('')
  const filterRegimen = ref('ALL')
  const sortBy = ref('RECENT')
  const processingAction = ref(false)

  const cotizacionesClasificadas = computed(() => {
    return clasificarCotizacionesAduaneras(cotizaciones.value)
  })

  const hasActiveFilters = computed(() => {
    return Boolean(searchQuery.value.trim() || filterRegimen.value !== 'ALL' || sortBy.value !== 'RECENT')
  })

  const resetFilters = () => {
    searchQuery.value = ''
    filterRegimen.value = 'ALL'
    sortBy.value = 'RECENT'
  }

  const filteredCotizaciones = computed(() => {
    const clasif = cotizacionesClasificadas.value
    const queueMap = {
      nuevas: clasif.nuevas,
      subsanadas: clasif.subsanadas,
      pendientesPago: clasif.pendientesPago,
      listasApertura: clasif.listasApertura,
      historico: clasif.historico,
      todas: cotizaciones.value
    }

    let list = [...(queueMap[subQueueCotizaciones.value] || cotizaciones.value || [])]

    if (filterRegimen.value && filterRegimen.value !== 'ALL') {
      list = list.filter((c) => c.regimenAduanero === filterRegimen.value)
    }

    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      list = list.filter((c) => {
        return (
          c.codigoCotizacion?.toLowerCase().includes(q) ||
          c.clienteRazonSocial?.toLowerCase().includes(q) ||
          c.clienteNumeroDocumento?.toLowerCase().includes(q) ||
          c.usuarioComercialNombre?.toLowerCase().includes(q) ||
          c.codigoExpediente?.toLowerCase().includes(q) ||
          c.mercanciaCodigo?.toLowerCase().includes(q)
        )
      })
    }

    list.sort((a, b) => {
      switch (sortBy.value) {
        case 'OLDEST':
          return (a.idCotizacion || 0) - (b.idCotizacion || 0)
        case 'AMOUNT_DESC':
          return Number(b.totalCotizado || 0) - Number(a.totalCotizado || 0)
        case 'AMOUNT_ASC':
          return Number(a.totalCotizado || 0) - Number(b.totalCotizado || 0)
        case 'CODE_ASC':
          return String(a.codigoCotizacion || '').localeCompare(String(b.codigoCotizacion || ''))
        case 'RECENT':
        default:
          return (b.idCotizacion || 0) - (a.idCotizacion || 0)
      }
    })

    return list
  })

  const ejecutarRevisionTecnica = async ({
    cotizacion,
    estado,
    motivoObservacion = null,
    observaciones = ''
  }) => {
    if (!cotizacion?.idCotizacion) return false
    processingAction.value = true
    try {
      await gestionAduaneraService.revisarCotizacion(cotizacion.idCotizacion, {
        idAgenteRevisor: authStore.user?.idUsuario || 1,
        estado,
        motivoObservacion,
        observaciones
      })

      if (estado === 'APROBADA') {
        notify.showSuccess(`¡Cotización ${cotizacion.codigoCotizacion} Aprobada! Lista para apertura de Expediente Aduanero.`)
        const notifCred = getCredencialesPostAprobacionNotification(cotizacion)
        notify.showInfo(notifCred.message, notifCred.title, notifCred.life)
      } else if (estado === 'OBSERVADA') {
        notify.showInfo(`Cotización ${cotizacion.codigoCotizacion} devuelta como Observada para corrección por Ventas.`)
      } else if (estado === 'RECHAZADA') {
        notify.showInfo(`Cotización ${cotizacion.codigoCotizacion} marcada como Rechazada.`)
      }

      if (onReload) {
        await onReload()
      }
      return true
    } catch (err) {
      notify.showError(err?.response?.data?.message || 'No se pudo procesar la revisión de la cotización.')
      return false
    } finally {
      processingAction.value = false
    }
  }

  const aprobarCotizacion = async (cotizacion) => {
    if (!cotizacion) return false
    const confirmed = await confirmAction({
      message: `¿Confirma la aprobación de la Cotización ${cotizacion.codigoCotizacion}? Al aprobar, la propuesta queda formalizada técnicamente para apertura de Expediente Aduanero.`,
      header: 'Aprobación Técnica Aduanera',
      icon: 'pi pi-verified',
      acceptLabel: 'Aprobar Propuesta',
      rejectLabel: 'Cancelar',
      acceptClass: 'sga-confirm-accept-btn',
      rejectClass: 'sga-confirm-reject-btn'
    })
    if (!confirmed) return false

    return ejecutarRevisionTecnica({
      cotizacion,
      estado: 'APROBADA',
      motivoObservacion: null,
      observaciones: 'Aprobada formalmente por Supervisión Aduanera.'
    })
  }

  return {
    subQueueCotizaciones,
    searchQuery,
    filterRegimen,
    sortBy,
    processingAction,
    cotizacionesClasificadas,
    filteredCotizaciones,
    hasActiveFilters,
    resetFilters,
    ejecutarRevisionTecnica,
    aprobarCotizacion
  }
}
