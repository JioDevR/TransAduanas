import { ref, computed } from 'vue'
import { gestionAduaneraService } from '../services/gestionAduaneraService'
import { expedienteService } from '../services/expedienteService'
import { userService } from '@/features/admin/users/services/userService'

/**
 * Composable para la vista separada de Apertura y Formalización de Expediente Aduanero.
 * Gestiona la carga de la cotización objetivo, agentes aduaneros acreditados y envío del formulario.
 */
export function useAperturaExpedienteForm({ authStore }) {
  const loading = ref(false)
  const submitting = ref(false)
  const errorMessage = ref('')
  const cotizacion = ref(null)
  const agentesList = ref([])

  const isCurrentUserAgente = computed(() => {
    return authStore?.userRole === 'AGENTE_ADUANERO' || authStore?.user?.rol === 'AGENTE_ADUANERO'
  })

  const form = ref({
    idAgenteAduanero: isCurrentUserAgente.value ? authStore?.user?.idUsuario || null : null,
    contactoExteriorNombre: '',
    contactoExteriorTelefono: '',
    contactoExteriorCorreo: '',
    fechaZarpeEtd: '',
    fechaLlegadaEta: '',
    numeroContenedor: '',
    precintoAduanero: '',
    naveViaje: '',
    observaciones: ''
  })

  const agentesOptions = computed(() => {
    return (agentesList.value || []).map((u) => ({
      ...u,
      label: `${u.nombres || ''} ${u.apellidos || ''} (${u.codigoSunat ? u.codigoSunat + ' / ' : ''}@${u.username})`.trim()
    }))
  })

  const selectedAgente = computed(() => {
    return agentesOptions.value.find((a) => a.idUsuario === form.value.idAgenteAduanero) || null
  })

  const isCotizacionElegible = computed(() => {
    const c = cotizacion.value
    if (!c) return false
    return c.estado === 'APROBADA' && !c.tieneExpediente && Boolean(c.tienePagoAprobado)
  })

  const eligibilityWarning = computed(() => {
    const c = cotizacion.value
    if (!c) return ''
    if (c.tieneExpediente) {
      return `Esta cotización ya cuenta con el expediente ${c.codigoExpediente || ''} formalizado.`
    }
    if (c.estado !== 'APROBADA') {
      return `La cotización está en estado ${c.estado}. Debe estar APROBADA técnicamente antes de aperturar expediente.`
    }
    if (!c.tienePagoAprobado) {
      return 'La cotización aún no cuenta con abono verificado en Finanzas para autorizar la apertura.'
    }
    return ''
  })

  const isFormReady = computed(() => {
    return Boolean(
      cotizacion.value?.idCotizacion &&
      isCotizacionElegible.value &&
      form.value.idAgenteAduanero &&
      !loading.value &&
      !submitting.value
    )
  })

  const loadData = async (idCotizacion) => {
    loading.value = true
    errorMessage.value = ''
    try {
      const [cotData, agentes] = await Promise.all([
        gestionAduaneraService.getCotizacionById(idCotizacion),
        userService.getAgentesAduaneros()
      ])

      cotizacion.value = cotData || null
      agentesList.value = agentes || []

      if (isCurrentUserAgente.value && authStore?.user?.idUsuario) {
        form.value.idAgenteAduanero = authStore.user.idUsuario
      } else if (agentesList.value.length > 0 && !form.value.idAgenteAduanero) {
        form.value.idAgenteAduanero = agentesList.value[0].idUsuario
      }
    } catch (err) {
      errorMessage.value = err?.response?.data?.message || 'No se pudo cargar la información de la cotización para apertura.'
    } finally {
      loading.value = false
    }
  }

  const submit = async () => {
    errorMessage.value = ''
    if (!cotizacion.value?.idCotizacion || !form.value.idAgenteAduanero) {
      errorMessage.value = 'Debe seleccionar el agente aduanero responsable para formalizar el expediente.'
      return null
    }
    if (!isCotizacionElegible.value) {
      errorMessage.value = eligibilityWarning.value || 'La cotización no es elegible para apertura.'
      return null
    }

    submitting.value = true
    try {
      const payload = {
        idCotizacion: cotizacion.value.idCotizacion,
        idAgenteAduanero: form.value.idAgenteAduanero,
        contactoExteriorNombre: form.value.contactoExteriorNombre || null,
        contactoExteriorTelefono: form.value.contactoExteriorTelefono || null,
        contactoExteriorCorreo: form.value.contactoExteriorCorreo || null,
        fechaZarpeEtd: form.value.fechaZarpeEtd || null,
        fechaLlegadaEta: form.value.fechaLlegadaEta || null,
        numeroContenedor: form.value.numeroContenedor || null,
        precintoAduanero: form.value.precintoAduanero || null,
        naveViaje: form.value.naveViaje || null,
        observaciones: form.value.observaciones || null
      }
      return await expedienteService.createExpediente(payload)
    } catch (err) {
      errorMessage.value = err?.response?.data?.message || 'Error al formalizar el expediente aduanero.'
      return null
    } finally {
      submitting.value = false
    }
  }

  return {
    loading,
    submitting,
    errorMessage,
    cotizacion,
    agentesOptions,
    selectedAgente,
    form,
    isCotizacionElegible,
    eligibilityWarning,
    isFormReady,
    loadData,
    submit
  }
}
