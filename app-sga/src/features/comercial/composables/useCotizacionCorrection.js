import { ref, computed, watch } from 'vue'
import { useModalidadPuertos } from './useModalidadPuertos'
import {
  calcularValorCif,
  calcularMontoAdValorem,
  calcularMontoIgv,
  calcularMontoPercepcion,
  calcularLiquidacionIntegral,
  formatMoney
} from './useCotizacionCalculator'
import {
  REGIMEN_OPTIONS,
  getRegimenLabel,
  getTipoCargaDescripcionFallback,
  ensureSmartFilterRegistered
} from '../utils/cotizacionCatalogos'
import { normalizeDepartamentoPeru, normalizeProvinciaPeru } from '../data/peruUbigeo'

/**
 * Composable que encapsula la gestión de estado y recálculos automáticos
 * para la vista unificada de corrección de cotizaciones observadas.
 *
 * @param {Object} options
 * @param {import('vue').Ref<Object>} options.cotizacion - Objeto de la cotización observada
 * @param {import('vue').Ref<Array>} [options.puertos] - Lista completa de puertos de catálogo
 * @param {import('vue').Ref<Array>} [options.tiposCarga] - Lista de tipos de carga
 * @param {import('vue').Ref<Array>} [options.mercancias] - Lista de mercancías arancelarias
 * @param {import('vue').Ref<Array>} [options.clientes] - Lista de clientes disponibles
 */
export function useCotizacionCorrection({
  cotizacion,
  puertos = ref([]),
  tiposCarga = ref([]),
  mercancias = ref([]),
  clientes = ref([])
}) {
  ensureSmartFilterRegistered()

  const form = ref({
    idCotizacion: null,
    codigoCotizacion: '',
    idCliente: null,
    clienteRazonSocial: '',
    clienteNumeroDocumento: '',
    idUsuarioComercial: 1,
    validezDias: 15,
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
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

  const motivoObservacion = ref('')
  const notaSubsanacion = ref('')
  const seguroManual = ref(false)

  // Integración de modalidad de transporte y cascada de puertos
  const regimenComputed = computed(() => form.value.regimenAduanero)
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
    regimen: regimenComputed,
    form
  })

  // Objetos seleccionados para vista previa y subcomponentes sgaUI
  const selectedClientObject = computed(() => {
    const list = Array.isArray(clientes?.value) ? clientes.value : []
    const match = list.find((c) => c.idCliente === form.value.idCliente)
    if (match) return match
    if (form.value.clienteRazonSocial) {
      return {
        idCliente: form.value.idCliente,
        razonSocial: form.value.clienteRazonSocial,
        tipoDocumento: 'DOC',
        numeroDocumento: form.value.clienteNumeroDocumento || 'S/D'
      }
    }
    return null
  })

  const selectedPuertoEnvio = computed(() => {
    if (!form.value.idPuertoEnvio) return null
    const list = Array.isArray(puertos?.value) ? puertos.value : []
    return list.find((p) => p.idPuerto === form.value.idPuertoEnvio) || null
  })

  const selectedPuertoDestino = computed(() => {
    if (!form.value.idPuertoDestino) return null
    const list = Array.isArray(puertos?.value) ? puertos.value : []
    return list.find((p) => p.idPuerto === form.value.idPuertoDestino) || null
  })

  const selectedTipoCarga = computed(() => {
    if (!form.value.idTipoCarga) return null
    const list = Array.isArray(tiposCarga?.value) ? tiposCarga.value : []
    const found = list.find((tc) => tc.idTipoCarga === form.value.idTipoCarga)
    if (!found) return null
    return { ...found, descripcion: found.descripcion || getTipoCargaDescripcionFallback(found.codigo) }
  })

  const selectedMercancia = computed(() => {
    if (!form.value.idMercancia) return null
    const list = Array.isArray(mercancias?.value) ? mercancias.value : []
    return list.find((m) => m.idMercancia === form.value.idMercancia) || null
  })

  // Sincronizar formulario cuando cambia la cotización recibida
  const initFromCotizacion = (c) => {
    if (!c) return
    motivoObservacion.value = c.motivoObservacion || ''
    notaSubsanacion.value = ''
    seguroManual.value = Number(c.montoSeguroEstimado || 0) > 0

    const origDep = normalizeDepartamentoPeru(c.origenDepartamento, 'CALLAO')
    const destDep = normalizeDepartamentoPeru(c.destinoDepartamento, 'LIMA')

    form.value = {
      idCotizacion: c.idCotizacion || null,
      codigoCotizacion: c.codigoCotizacion || '',
      idCliente: c.idCliente || null,
      clienteRazonSocial: c.clienteRazonSocial || c.cliente?.razonSocial || '',
      clienteNumeroDocumento: c.clienteNumeroDocumento || c.cliente?.numeroDocumento || '',
      idUsuarioComercial: c.idUsuarioComercial || 1,
      validezDias: c.validezDias || 15,
      regimenAduanero: c.regimenAduanero || 'IMPORTACION_DEFINITIVA',
      idPuertoEnvio: c.idPuertoEnvio || null,
      idPuertoDestino: c.idPuertoDestino || null,
      idTipoCarga: c.idTipoCarga || null,
      idMercancia: c.idMercancia || null,
      servicioTransporte: c.servicioTransporte === true,
      origenDepartamento: origDep,
      origenProvincia: normalizeProvinciaPeru(origDep, c.origenProvincia, 'Callao'),
      destinoDepartamento: destDep,
      destinoProvincia: normalizeProvinciaPeru(destDep, c.destinoProvincia, 'Lima (Metropolitana)'),
      costoTransporteInterno: Number(c.costoTransporteInterno || 0),
      valorFob: Number(c.valorFob || 0),
      montoFleteEstimado: Number(c.montoFleteEstimado || 0),
      montoSeguroEstimado: Number(c.montoSeguroEstimado || 0),
      montoHonorariosEstimado: Number(c.montoHonorariosEstimado || 0),
      tasaAdValorem: Number(c.tasaAdValorem || 0),
      tasaPercepcion: Number(c.tasaPercepcion || 0),
      montoTributosEstimado: Number(c.montoTributosEstimado || 0),
      totalCotizado: Number(c.totalCotizado || 0),
      observaciones: c.observaciones || ''
    }

    if (puertos.value && puertos.value.length > 0) {
      inferirDesdePuertos()
    }
    recalcularLiquidacion()
  }

  // Cálculos económicos reactivos
  const valorCif = computed(() => {
    return calcularValorCif(form.value.valorFob, form.value.montoFleteEstimado, form.value.montoSeguroEstimado)
  })

  const montoAdValorem = computed(() => {
    return calcularMontoAdValorem(valorCif.value, form.value.tasaAdValorem, form.value.regimenAduanero)
  })

  const montoIgv = computed(() => {
    return calcularMontoIgv(valorCif.value, montoAdValorem.value, form.value.regimenAduanero)
  })

  const montoPercepcion = computed(() => {
    return calcularMontoPercepcion(
      valorCif.value,
      montoAdValorem.value,
      montoIgv.value,
      form.value.tasaPercepcion,
      form.value.regimenAduanero
    )
  })

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

  const handleSeguroManualInput = (val) => {
    if (val !== undefined) form.value.montoSeguroEstimado = Number(val) || 0
    seguroManual.value = true
    recalcularLiquidacion()
  }

  const resetearSeguroAutomatico = () => {
    seguroManual.value = false
    recalcularLiquidacion()
  }

  // Observador de cambios en insumos económicos para recalcular automáticamente
  watch(
    () => [
      form.value.valorFob,
      form.value.montoFleteEstimado,
      form.value.montoSeguroEstimado,
      form.value.montoHonorariosEstimado,
      form.value.tasaAdValorem,
      form.value.tasaPercepcion,
      form.value.servicioTransporte,
      form.value.costoTransporteInterno,
      form.value.regimenAduanero
    ],
    () => {
      recalcularLiquidacion()
    }
  )

  watch(
    () => cotizacion.value,
    (newVal) => {
      if (newVal) {
        initFromCotizacion(newVal)
      }
    },
    { immediate: true }
  )

  watch(
    () => puertos.value,
    (newPuertos) => {
      if (newPuertos && newPuertos.length > 0 && form.value.idCotizacion) {
        inferirDesdePuertos()
      }
    }
  )

  // Validación de corrección
  const validateCorrection = () => {
    if (!form.value.idCliente) {
      return { valid: false, error: 'La cotización debe tener un cliente asignado.' }
    }
    if (!form.value.valorFob || Number(form.value.valorFob) <= 0) {
      return { valid: false, error: 'El Valor FOB de mercancía debe ser mayor a $0.00 USD.' }
    }
    if (Number(form.value.totalCotizado) <= 0) {
      return { valid: false, error: 'El total cotizado debe ser mayor a $0.00 USD.' }
    }
    return { valid: true, error: '' }
  }

  // Construcción del payload de actualización
  const buildSubmissionPayload = () => {
    let finalObservaciones = form.value.observaciones || ''
    if (notaSubsanacion.value && notaSubsanacion.value.trim()) {
      const nota = `[Subsanación Comercial]: ${notaSubsanacion.value.trim()}`
      finalObservaciones = finalObservaciones ? `${finalObservaciones} | ${nota}` : nota
    }

    return {
      ...form.value,
      observaciones: finalObservaciones
    }
  }

  return {
    form,
    motivoObservacion,
    notaSubsanacion,
    seguroManual,
    regimenOptions: REGIMEN_OPTIONS,
    modalidadTransporte,
    paisOrigen,
    paisDestino,
    paisesOrigenDisponibles,
    paisesDestinoDisponibles,
    puertosOrigenFiltrados,
    puertosDestinoFiltrados,
    selectedClientObject,
    selectedPuertoEnvio,
    selectedPuertoDestino,
    selectedTipoCarga,
    selectedMercancia,
    setModalidad,
    aplicarReglasRegimen,
    handleSeguroManualInput,
    resetearSeguroAutomatico,
    valorCif,
    montoAdValorem,
    montoIgv,
    montoPercepcion,
    recalcularLiquidacion,
    validateCorrection,
    buildSubmissionPayload,
    getRegimenLabel,
    formatNumber: formatMoney
  }
}
