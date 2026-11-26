import { ref, computed, watch, onMounted } from 'vue'
import { FilterService } from '@primevue/core/api'
import { useAuthStore } from '@/core/stores/authStore'
import { useNotification } from '@/shared/composables/useNotification'
import { portalCotizacionService } from '../services/portalCotizacionService'
import { useModalidadPuertos } from './useModalidadPuertos'
import { getDepartamentosPeru, getProvinciasPorDepartamento } from '../data/peruUbigeo'
import {
  REGIMEN_OPTIONS,
  DESCRIPCIONES_TIPO_CARGA_FALLBACK,
  matchesSmartFilter,
  createDefaultPortalSolicitudForm,
  validatePortalCotizacionForm,
  buildPortalSolicitudPayload
} from '../utils/portalCotizacionFormUtils'

FilterService.register('smart', matchesSmartFilter)

/**
 * Composable de estado, catálogos y envío para el Wizard de Solicitud de Cotización B2B.
 */
export function usePortalCotizacionForm(emit) {
  const authStore = useAuthStore()
  const { showError, showWarn } = useNotification()

  const currentStep = ref(1)
  const submitting = ref(false)
  const loadingCatalogos = ref(false)
  const queuedDocs = ref([])
  const form = ref(createDefaultPortalSolicitudForm())

  const puertos = ref([])
  const tiposCarga = ref([])
  const mercancias = ref([])

  const regimen = computed(() => form.value.regimenAduanero)

  const {
    modalidadTransporte,
    paisOrigen,
    paisDestino,
    paisesOrigenDisponibles,
    paisesDestinoDisponibles,
    puertosOrigenFiltrados,
    puertosDestinoFiltrados,
    setModalidad,
    aplicarReglasRegimen
  } = useModalidadPuertos({
    puertos,
    regimen,
    form
  })

  const departamentosPeru = getDepartamentosPeru()

  const provinciasOrigen = computed(() => {
    return getProvinciasPorDepartamento(form.value.origenDepartamento)
  })

  const provinciasDestino = computed(() => {
    return getProvinciasPorDepartamento(form.value.destinoDepartamento)
  })

  watch(
    () => form.value.origenDepartamento,
    (newDep) => {
      const validProvs = getProvinciasPorDepartamento(newDep)
      if (!validProvs.includes(form.value.origenProvincia)) {
        form.value.origenProvincia = validProvs[0] || ''
      }
    }
  )

  watch(
    () => form.value.destinoDepartamento,
    (newDep) => {
      const validProvs = getProvinciasPorDepartamento(newDep)
      if (!validProvs.includes(form.value.destinoProvincia)) {
        form.value.destinoProvincia = validProvs[0] || ''
      }
    }
  )

  const selectedPuertoEnvio = computed(() => {
    return puertos.value.find((p) => p.idPuerto === form.value.idPuertoEnvio) || null
  })

  const selectedPuertoDestino = computed(() => {
    return puertos.value.find((p) => p.idPuerto === form.value.idPuertoDestino) || null
  })

  const selectedTipoCarga = computed(() => {
    return tiposCarga.value.find((t) => t.idTipoCarga === form.value.idTipoCarga) || null
  })

  const selectedMercancia = computed(() => {
    return mercancias.value.find((m) => m.idMercancia === form.value.idMercancia) || null
  })

  const empresaData = computed(() => ({
    razonSocial: authStore.empresaNombre,
    ruc: authStore.ruc,
    usuarioNombre: authStore.usuarioNombre
  }))

  const isFormReady = computed(() => {
    return validatePortalCotizacionForm(form.value).valid
  })

  const handleSelectRegimen = (regValue) => {
    if (form.value.regimenAduanero === regValue) return
    form.value.regimenAduanero = regValue
    aplicarReglasRegimen(regValue)
  }

  onMounted(async () => {
    loadingCatalogos.value = true
    try {
      const [pRes, tcRes, mRes] = await Promise.allSettled([
        portalCotizacionService.obtenerPuertos(true),
        portalCotizacionService.obtenerTiposCarga(true),
        portalCotizacionService.obtenerMercancias(true)
      ])

      const unwrap = (r) => (r.status === 'fulfilled' && Array.isArray(r.value) ? r.value : [])

      puertos.value = unwrap(pRes).map((p) => ({
        ...p,
        searchCorpus: `${p.codigoPuerto || ''} ${p.nombre || ''} ${p.pais || ''} ${p.tipo || ''}`
      }))

      tiposCarga.value = unwrap(tcRes).map((t) => {
        const desc = t.descripcion || DESCRIPCIONES_TIPO_CARGA_FALLBACK[t.codigo] || ''
        return {
          ...t,
          descripcion: desc,
          searchCorpus: `${t.codigo || ''} ${t.nombre || ''} ${desc}`
        }
      })

      mercancias.value = unwrap(mRes).map((m) => ({
        ...m,
        searchCorpus: `${m.codigo || ''} ${m.nombre || ''} ${m.categoria || ''} ${m.entidadReguladora || ''}`
      }))

      aplicarReglasRegimen()
    } catch (err) {
      console.error('Error cargando catálogos para portal:', err)
    } finally {
      loadingCatalogos.value = false
    }
  })

  const handleSubmit = async () => {
    const valResult = validatePortalCotizacionForm(form.value)
    if (!valResult.valid) {
      if (valResult.step) {
        currentStep.value = valResult.step
      }
      showWarn(valResult.error || 'Por favor verifique los datos del formulario.')
      return
    }

    submitting.value = true
    try {
      const payload = buildPortalSolicitudPayload(form.value)
      const created = await portalCotizacionService.solicitarCotizacion(payload)

      let docsResult = { uploaded: [], failed: [] }
      if (created?.idCotizacion && queuedDocs.value && queuedDocs.value.length > 0) {
        docsResult = await portalCotizacionService.subirDocumentosCotizacion(
          created.idCotizacion,
          queuedDocs.value
        )
      }

      emit('saved', { cotizacion: created, docsResult })
    } catch (err) {
      showError(err.response?.data?.message || err.message || 'Error al enviar la solicitud')
    } finally {
      submitting.value = false
    }
  }

  return {
    currentStep,
    submitting,
    loadingCatalogos,
    queuedDocs,
    form,
    REGIMEN_OPTIONS,
    tiposCarga,
    mercancias,
    modalidadTransporte,
    paisOrigen,
    paisDestino,
    paisesOrigenDisponibles,
    paisesDestinoDisponibles,
    puertosOrigenFiltrados,
    puertosDestinoFiltrados,
    departamentosPeru,
    provinciasOrigen,
    provinciasDestino,
    selectedPuertoEnvio,
    selectedPuertoDestino,
    selectedTipoCarga,
    selectedMercancia,
    empresaData,
    isFormReady,
    setModalidad,
    handleSelectRegimen,
    handleSubmit
  }
}
