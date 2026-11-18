import { ref, computed, watch, onMounted } from 'vue'
import { FilterService } from '@primevue/core/api'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { comercialService } from '../services/comercialService'
import { catalogoService } from '@/features/admin/catalogos/services/catalogoService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { getDepartamentosPeru, getProvinciasPorDepartamento } from '../data/peruUbigeo'
import {
  calcularValorCif,
  calcularMontoAdValorem,
  calcularMontoIgv,
  calcularMontoPercepcion,
  calcularLiquidacionIntegral
} from './useCotizacionCalculator'
import { useModalidadPuertos } from './useModalidadPuertos'

export const REGIMEN_OPTIONS = [
  {
    value: 'IMPORTACION_DEFINITIVA',
    codigo: 'REG-10',
    label: 'Importación para el Consumo',
    desc: 'Ingreso legal de mercancías para consumo definitivo previo pago de tributos.'
  },
  {
    value: 'EXPORTACION_DEFINITIVA',
    codigo: 'REG-40',
    label: 'Exportación Definitiva',
    desc: 'Salida de mercancías del territorio aduanero para uso o consumo en el exterior.'
  }
]

const DESCRIPCIONES_TIPO_CARGA_FALLBACK = {
  CARGA_FCL: 'Contenedor exclusivo de 20 o 40 pies para un único consignatario',
  CARGA_LCL: 'Carga agrupada en contenedor compartido para múltiples importadores',
  CARGA_REEFER: 'Mercancías con cadena de frío continua y atmósfera controlada',
  CARGA_RO_RO: 'Vehículos y maquinaria autopropulsada embarcada por rampas',
  CARGA_PROYECTO: 'Bienes sobredimensionados o con peso extrapesado fuera de estándar',
  CARGA_GRANEL_SOLIDO: 'Sólidos transportados a granel en bodegas sin empaque unitario',
  CARGA_GRANEL_LIQUIDO: 'Líquidos transportados en buques tanque o tanques cisterna ISO',
  CARGA_PELIGROSA_IMO: 'Sustancias peligrosas reguladas según código marítimo internacional IMO',
  CARGA_VALORADA: 'Carga de alto valor comercial sujeta a protocolo de seguridad y custodia',
  CARGA_AEREA_SUELTA: 'Bultos y cajas sueltas estibadas en bodegas inferiores del avión (belly cargo)',
  CARGA_AEREA_ULD_PALLET: 'Pallet aeronáutico de aluminio estandarizado IATA con red de sujeción',
  CARGA_AEREA_ULD_CONT: 'Contenedor liviano adaptado a la curvatura del fuselaje de la aeronave',
  CARGA_AEREA_COURIER: 'Envíos de entrega rápida y comercio electrónico internacional',
  CARGA_AEREA_PERECIBLE: 'Fármacos y perecibles en contenedor activo refrigerado con monitoreo continuo',
  CARGA_AEREA_DGR_IATA: 'Mercancías peligrosas reguladas bajo la estricta normativa técnica IATA DGR',
  CARGA_AEREA_VALORADA: 'Metales preciosos, joyas y divisas en bóveda blindada aeroportuaria con escolta',
  CARGA_AEREA_AVI: 'Transporte aéreo de mascotas y animales vivos en cabina o bodega presurizada'
}

export const normalizeSearchText = (text) => {
  if (!text) return ''
  return text
    .toString()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

let smartFilterRegistered = false
const ensureSmartFilterRegistered = () => {
  if (smartFilterRegistered) return
  FilterService.register('smart', (value, filter) => {
    if (filter === undefined || filter === null || filter === '') return true
    if (value === undefined || value === null) return false
    const normVal = normalizeSearchText(value)
    const tokens = normalizeSearchText(filter).split(/\s+/).filter(Boolean)
    if (tokens.length === 0) return true
    return tokens.every((tok) => normVal.includes(tok))
  })
  smartFilterRegistered = true
}

const unwrapList = (res) => {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.data)) return res.data
  return []
}

const createDefaultQuickClient = () => ({
  tipoDocumento: 'RUC',
  numeroDocumento: '',
  razonSocial: '',
  direccionFiscal: '',
  contactoRepresentante: '',
  correoContacto: '',
  telefonoContacto: '',
  esProspecto: true
})

export function useCotizacionForm(props, emit) {
  ensureSmartFilterRegistered()
  const authStore = useAuthStore()

  const currentStep = ref(1)
  const submitting = ref(false)
  const errorMessage = ref('')
  const loadingCatalogos = ref(false)

  const queuedDocs = ref([])
  const existingDocsCount = ref(0)
  const seguroManual = ref(false)

  const puertos = ref([])
  const tiposCarga = ref([])
  const mercancias = ref([])

  const localClientsList = ref([...(props.clientes || [])])
  watch(
    () => props.clientes,
    (newClients) => {
      localClientsList.value = [...(newClients || [])]
    },
    { deep: true }
  )

  const clientSearchQuery = ref('')
  const showQuickClientForm = ref(false)
  const savingQuickClient = ref(false)
  const lookingUpClient = ref(false)
  const quickClientError = ref('')
  const quickClient = ref(createDefaultQuickClient())

  const defaultForm = () => ({
    codigoCotizacion: '',
    idCliente: '',
    idUsuarioComercial: authStore.user?.idUsuario || 1,
    validezDias: null,
    regimenAduanero: '',
    idPuertoEnvio: null,
    idPuertoDestino: null,
    idTipoCarga: null,
    idMercancia: null,
    servicioTransporte: false,
    origenDepartamento: 'CALLAO',
    origenProvincia: 'Callao',
    destinoDepartamento: 'LIMA',
    destinoProvincia: 'Lima (Metropolitana)',
    costoTransporteInterno: 0,
    valorFob: 0,
    montoFleteEstimado: 0,
    montoSeguroEstimado: 0,
    montoHonorariosEstimado: 0,
    tasaAdValorem: 0,
    tasaPercepcion: 0,
    montoTributosEstimado: 0,
    totalCotizado: 0,
    observaciones: ''
  })

  const form = ref(defaultForm())

  const isEditMode = computed(() => !!props.cotizacionToEdit && !!props.cotizacionToEdit.idCotizacion)
  const isUnassignedClaim = computed(() => isEditMode.value && !props.cotizacionToEdit?.idUsuarioComercial)

  const departamentosList = computed(() => getDepartamentosPeru())
  const provinciasOrigenList = computed(() => getProvinciasPorDepartamento(form.value.origenDepartamento))
  const provinciasDestinoList = computed(() => getProvinciasPorDepartamento(form.value.destinoDepartamento))

  const onOrigenDepartamentoChange = () => {
    const provs = getProvinciasPorDepartamento(form.value.origenDepartamento)
    form.value.origenProvincia = provs.length > 0 ? provs[0] : ''
  }

  const onDestinoDepartamentoChange = () => {
    const provs = getProvinciasPorDepartamento(form.value.destinoDepartamento)
    form.value.destinoProvincia = provs.length > 0 ? provs[0] : ''
  }

  const selectedClientObject = computed(() => {
    if (!form.value.idCliente) return null
    return localClientsList.value.find((c) => c.idCliente === form.value.idCliente) || null
  })

  const selectedMercancia = computed(() => {
    if (!form.value.idMercancia) return null
    return mercancias.value.find((m) => m.idMercancia === form.value.idMercancia) || null
  })

  const selectedPuertoEnvio = computed(() => {
    if (!form.value.idPuertoEnvio) return null
    return puertos.value.find((p) => p.idPuerto === form.value.idPuertoEnvio) || null
  })

  const selectedPuertoDestino = computed(() => {
    if (!form.value.idPuertoDestino) return null
    return puertos.value.find((p) => p.idPuerto === form.value.idPuertoDestino) || null
  })

  const getDescripcionFallback = (codigo) => {
    return (codigo && DESCRIPCIONES_TIPO_CARGA_FALLBACK[codigo]) || ''
  }

  const selectedTipoCarga = computed(() => {
    if (!form.value.idTipoCarga) return null
    const found = tiposCarga.value.find((tc) => tc.idTipoCarga === form.value.idTipoCarga)
    if (!found) return null
    return { ...found, descripcion: found.descripcion || getDescripcionFallback(found.codigo) }
  })

  const regimenAduaneroComputed = computed(() => form.value.regimenAduanero)
  const {
    modalidadTransporte,
    paisOrigen,
    paisDestino,
    paisesOrigenDisponibles,
    paisesDestinoDisponibles,
    puertosOrigenFiltrados,
    puertosDestinoFiltrados,
    setModalidad,
    aplicarReglasRegimen,
    inferirDesdePuertos
  } = useModalidadPuertos({
    puertos,
    regimen: regimenAduaneroComputed,
    form
  })

  const loadCatalogos = async () => {
    loadingCatalogos.value = true
    try {
      const [puertosRes, tiposCargaRes, mercanciasRes] = await Promise.allSettled([
        catalogoService.getPuertos(true),
        catalogoService.getTiposCarga(true),
        catalogoService.getMercancias(true)
      ])
      if (puertosRes.status === 'fulfilled') {
        puertos.value = unwrapList(puertosRes.value).map((p) => ({
          ...p,
          searchCorpus: `${p.codigoPuerto || ''} ${p.nombre || ''} ${p.pais || ''} ${p.tipo || ''}`
        }))
        if (props.cotizacionToEdit?.idCotizacion) {
          inferirDesdePuertos()
        } else {
          aplicarReglasRegimen()
        }
      }
      if (tiposCargaRes.status === 'fulfilled') {
        tiposCarga.value = unwrapList(tiposCargaRes.value).map((t) => {
          const desc = t.descripcion || getDescripcionFallback(t.codigo)
          return {
            ...t,
            descripcion: desc,
            searchCorpus: `${t.codigo || ''} ${t.nombre || ''} ${desc}`
          }
        })
      }
      if (mercanciasRes.status === 'fulfilled') {
        mercancias.value = unwrapList(mercanciasRes.value).map((m) => ({
          ...m,
          searchCorpus: `${m.codigo || ''} ${m.nombre || ''} ${m.categoria || ''} ${m.entidadReguladora || ''}`
        }))
      }
    } catch (err) {
      console.error('Error cargando catálogos para cotización:', err)
    } finally {
      loadingCatalogos.value = false
    }
  }

  const handleSelectRegimen = (regValue) => {
    form.value.regimenAduanero = regValue
    aplicarReglasRegimen(regValue)
  }

  const handlePaisOrigenChange = () => {
    if (form.value.idPuertoEnvio) {
      const valid = puertosOrigenFiltrados.value.some((p) => p.idPuerto === form.value.idPuertoEnvio)
      if (!valid) form.value.idPuertoEnvio = null
    }
  }

  const handlePaisDestinoChange = () => {
    if (form.value.idPuertoDestino) {
      const valid = puertosDestinoFiltrados.value.some((p) => p.idPuerto === form.value.idPuertoDestino)
      if (!valid) form.value.idPuertoDestino = null
    }
  }

  onMounted(() => {
    loadCatalogos()
  })

  watch(
    () => props.cotizacionToEdit,
    async (val) => {
      errorMessage.value = ''
      queuedDocs.value = []
      seguroManual.value = false

      if (val && val.idCotizacion) {
        currentStep.value = !val.idUsuarioComercial ? 4 : 1
        form.value = {
          codigoCotizacion: val.codigoCotizacion || '',
          idCliente: val.idCliente || '',
          idUsuarioComercial: val.idUsuarioComercial || authStore.user?.idUsuario || 1,
          validezDias: val.validezDias || 15,
          regimenAduanero: val.regimenAduanero || 'IMPORTACION_DEFINITIVA',
          idPuertoEnvio: val.idPuertoEnvio || null,
          idPuertoDestino: val.idPuertoDestino || null,
          idTipoCarga: val.idTipoCarga || null,
          idMercancia: val.idMercancia || null,
          servicioTransporte: val.servicioTransporte === true,
          origenDepartamento: val.origenDepartamento || 'CALLAO',
          origenProvincia: val.origenProvincia || 'Callao',
          destinoDepartamento: val.destinoDepartamento || 'LIMA',
          destinoProvincia: val.destinoProvincia || 'Lima (Metropolitana)',
          costoTransporteInterno: Number(val.costoTransporteInterno || 0),
          valorFob: Number(val.valorFob || 0),
          montoFleteEstimado: Number(val.montoFleteEstimado || 0),
          montoSeguroEstimado: Number(val.montoSeguroEstimado || 0),
          montoHonorariosEstimado: Number(val.montoHonorariosEstimado || 0),
          tasaAdValorem: Number(val.tasaAdValorem || 0),
          tasaPercepcion: Number(val.tasaPercepcion || 0),
          montoTributosEstimado: Number(val.montoTributosEstimado || 0),
          totalCotizado: Number(val.totalCotizado || 0),
          observaciones: val.observaciones || ''
        }

        if (form.value.montoSeguroEstimado > 0) {
          seguroManual.value = true
        }

        if (puertos.value.length > 0) {
          inferirDesdePuertos()
        }

        try {
          const docs = await documentoService.getDocumentos({
            entidadTipo: 'COTIZACION',
            idCotizacion: val.idCotizacion
          })
          existingDocsCount.value = Array.isArray(docs) ? docs.length : 0
        } catch {
          existingDocsCount.value = 0
        }
      } else {
        form.value = defaultForm()
        existingDocsCount.value = 0
        aplicarReglasRegimen()
      }
    },
    { immediate: true }
  )

  const filteredAvailableClients = computed(() => {
    const query = normalizeSearchText(clientSearchQuery.value)
    if (!query) return localClientsList.value

    const tokens = query.split(/\s+/).filter(Boolean)
    return localClientsList.value.filter((c) => {
      const corpus = normalizeSearchText(
        `${c.numeroDocumento || ''} ${c.razonSocial || ''} ${c.correoContacto || ''} ${c.contactoRepresentante || ''} ${c.telefonoContacto || ''} ${c.esProspecto ? 'prospecto' : 'cliente'}`
      )
      return tokens.every((tok) => corpus.includes(tok))
    })
  })

  const selectClient = (client) => {
    form.value.idCliente = client.idCliente
    clientSearchQuery.value = ''
    showQuickClientForm.value = false
  }

  const clearSelectedClient = () => {
    form.value.idCliente = ''
  }

  const resetQuickClient = () => {
    quickClient.value = createDefaultQuickClient()
    quickClientError.value = ''
  }

  const handleLookupClientByDoc = async () => {
    const doc = (quickClient.value.numeroDocumento || '').trim()
    if (!doc) return
    quickClientError.value = ''
    lookingUpClient.value = true
    try {
      const existing = await comercialService.getClienteByDocumento(doc)
      if (existing?.idCliente) {
        if (!localClientsList.value.some((c) => c.idCliente === existing.idCliente)) {
          localClientsList.value.unshift(existing)
        }
        selectClient(existing)
      }
    } catch {
      // No existe en el directorio aún; el usuario puede completar el registro rápido
    } finally {
      lookingUpClient.value = false
    }
  }

  const handleSaveQuickClient = async () => {
    quickClientError.value = ''
    if (!quickClient.value.numeroDocumento || !quickClient.value.razonSocial || !quickClient.value.correoContacto) {
      quickClientError.value = 'Complete los campos obligatorios: Documento, Razón Social y Correo.'
      return
    }

    savingQuickClient.value = true
    try {
      const createdClient = await comercialService.createCliente(quickClient.value)
      localClientsList.value.unshift(createdClient)
      form.value.idCliente = createdClient.idCliente
      showQuickClientForm.value = false
      resetQuickClient()
      emit('client-created', createdClient)
    } catch (err) {
      quickClientError.value = err.response?.data?.message || 'Error al registrar nuevo cliente.'
    } finally {
      savingQuickClient.value = false
    }
  }

  const valorCif = computed(() =>
    calcularValorCif(form.value.valorFob, form.value.montoFleteEstimado, form.value.montoSeguroEstimado)
  )

  const montoAdValorem = computed(() =>
    calcularMontoAdValorem(valorCif.value, form.value.tasaAdValorem, form.value.regimenAduanero)
  )

  const montoIgv = computed(() =>
    calcularMontoIgv(valorCif.value, montoAdValorem.value, form.value.regimenAduanero)
  )

  const montoPercepcion = computed(() =>
    calcularMontoPercepcion(valorCif.value, montoAdValorem.value, montoIgv.value, form.value.tasaPercepcion, form.value.regimenAduanero)
  )

  const recalcularLiquidacion = () => {
    const result = calcularLiquidacionIntegral({
      regimen: form.value.regimenAduanero,
      valorFob: form.value.valorFob,
      montoFleteEstimado: form.value.montoFleteEstimado,
      montoSeguroEstimado: form.value.montoSeguroEstimado,
      seguroManual: seguroManual.value,
      montoHonorariosEstimado: form.value.montoHonorariosEstimado,
      tasaAdValorem: form.value.tasaAdValorem,
      tasaPercepcion: form.value.tasaPercepcion,
      servicioTransporte: form.value.servicioTransporte,
      costoTransporteInterno: form.value.costoTransporteInterno
    })

    if (form.value.regimenAduanero === 'EXPORTACION_DEFINITIVA') {
      form.value.tasaAdValorem = 0
      form.value.tasaPercepcion = 0
      form.value.montoTributosEstimado = 0
      form.value.totalCotizado = result.totalCotizado
    } else {
      if (!seguroManual.value) {
        form.value.montoSeguroEstimado = result.montoSeguro
      }
      form.value.montoTributosEstimado = result.montoTributos
      form.value.totalCotizado = result.totalCotizado
    }
  }

  const handleMercanciaChange = () => {
    if (selectedMercancia.value) {
      if (selectedMercancia.value.arancelBasePct !== undefined && selectedMercancia.value.arancelBasePct !== null) {
        const pct = Number(selectedMercancia.value.arancelBasePct)
        if ([0, 6, 11].includes(pct)) {
          form.value.tasaAdValorem = pct
        }
      }
      recalcularLiquidacion()
    }
  }

  const handleSeguroManualInput = (val) => {
    if (val !== undefined) form.value.montoSeguroEstimado = Number(val) || 0
    seguroManual.value = true
    recalcularLiquidacion()
  }

  const resetearSeguroAutomatico = () => {
    seguroManual.value = false
    recalcularLiquidacion()
  }

  watch(
    () => [
      form.value.valorFob,
      form.value.montoFleteEstimado,
      form.value.montoSeguroEstimado,
      form.value.montoHonorariosEstimado,
      form.value.tasaAdValorem,
      form.value.tasaPercepcion,
      form.value.regimenAduanero,
      form.value.servicioTransporte,
      form.value.costoTransporteInterno
    ],
    () => {
      recalcularLiquidacion()
    }
  )

  const formatNumber = (num) => {
    if (num === null || num === undefined || isNaN(num)) return '0.00'
    return Number(num).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }

  const getRegimenLabel = (val) => {
    const match = REGIMEN_OPTIONS.find((r) => r.value === val)
    return match ? `${match.label} (${match.codigo})` : val
  }

  const isFormReady = computed(() => {
    return (
      !!form.value.idCliente &&
      !!form.value.regimenAduanero &&
      form.value.validezDias > 0 &&
      Number(form.value.valorFob) > 0 &&
      Number(form.value.totalCotizado) > 0
    )
  })

  const canContinueCurrentStep = computed(() => {
    if (currentStep.value === 1) return !!form.value.idCliente
    if (currentStep.value === 4) return isFormReady.value
    return true
  })

  const goToNextStep = () => {
    errorMessage.value = ''
    if (currentStep.value === 1 && !form.value.idCliente) {
      errorMessage.value = 'Seleccione o registre un cliente antes de continuar.'
      return
    }
    if (currentStep.value < 4) {
      currentStep.value += 1
    }
  }

  const goToPrevStep = () => {
    errorMessage.value = ''
    if (currentStep.value > 1) {
      currentStep.value -= 1
    }
  }

  const handleSubmit = () => {
    errorMessage.value = ''
    if (!form.value.idCliente) {
      errorMessage.value = 'Debe seleccionar o registrar un cliente en el Paso 01.'
      currentStep.value = 1
      return
    }

    if (!form.value.regimenAduanero) {
      errorMessage.value = 'Debe seleccionar un régimen aduanero en el Paso 02.'
      currentStep.value = 2
      return
    }

    if (!form.value.validezDias || form.value.validezDias <= 0) {
      errorMessage.value = 'Debe indicar la validez de la oferta en el Paso 02.'
      currentStep.value = 2
      return
    }

    if (!form.value.valorFob || form.value.valorFob <= 0) {
      errorMessage.value = 'El Valor de Mercancía (FOB) es obligatorio y debe ser mayor a $0.00 USD.'
      currentStep.value = 4
      return
    }

    if (form.value.totalCotizado <= 0) {
      errorMessage.value = 'El total cotizado debe ser mayor a $0.00 USD.'
      currentStep.value = 4
      return
    }

    submitting.value = true
    emit('saved', {
      isEdit: isEditMode.value,
      cotizacionId: props.cotizacionToEdit?.idCotizacion,
      payload: {
        ...form.value,
        idUsuarioComercial: isEditMode.value
          ? (props.cotizacionToEdit?.idUsuarioComercial || form.value.idUsuarioComercial || authStore.user?.idUsuario || 1)
          : (authStore.user?.idUsuario || form.value.idUsuarioComercial || 1)
      },
      documentos: queuedDocs.value,
      done: () => {
        submitting.value = false
      }
    })
  }

  const setSubmitting = (val) => {
    submitting.value = val
  }

  return {
    currentStep,
    submitting,
    errorMessage,
    loadingCatalogos,
    queuedDocs,
    existingDocsCount,
    seguroManual,
    puertos,
    tiposCarga,
    mercancias,
    localClientsList,
    clientSearchQuery,
    filteredAvailableClients,
    showQuickClientForm,
    savingQuickClient,
    lookingUpClient,
    quickClientError,
    quickClient,
    regimenOptions: REGIMEN_OPTIONS,
    form,
    isEditMode,
    isUnassignedClaim,
    departamentosList,
    provinciasOrigenList,
    provinciasDestinoList,
    onOrigenDepartamentoChange,
    onDestinoDepartamentoChange,
    selectedClientObject,
    selectedMercancia,
    selectedPuertoEnvio,
    selectedPuertoDestino,
    selectedTipoCarga,
    getDescripcionFallback,
    modalidadTransporte,
    paisOrigen,
    paisDestino,
    paisesOrigenDisponibles,
    paisesDestinoDisponibles,
    puertosOrigenFiltrados,
    puertosDestinoFiltrados,
    setModalidad,
    handleSelectRegimen,
    handlePaisOrigenChange,
    handlePaisDestinoChange,
    selectClient,
    clearSelectedClient,
    resetQuickClient,
    handleLookupClientByDoc,
    handleSaveQuickClient,
    valorCif,
    montoAdValorem,
    montoIgv,
    montoPercepcion,
    handleMercanciaChange,
    handleSeguroManualInput,
    resetearSeguroAutomatico,
    formatNumber,
    getRegimenLabel,
    isFormReady,
    canContinueCurrentStep,
    goToNextStep,
    goToPrevStep,
    handleSubmit,
    setSubmitting
  }
}
