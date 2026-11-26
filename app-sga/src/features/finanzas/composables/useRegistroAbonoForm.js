import { ref, computed, watch } from 'vue'
import finanzasService from '../services/finanzasService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNotification } from '@/shared/composables/useNotification'
import { getTodayDateInput } from '@/shared/utils/dateUtils'
import {
  DEFAULT_TIPO_CAMBIO,
  round2,
  convertAmountToPen,
  calculateCotizacionBalance
} from '../utils/finanzasCalculations'
import { formatUsd } from '../utils/finanzasFormatters'

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024

/**
 * Composable de estado, validación y cálculo para el formulario Split-Screen
 * de registro de abono / anticipo pre-expediente.
 */
export function useRegistroAbonoForm({
  cotizacionProp,
  cotizacionesProp,
  onSaved
}) {
  const authStore = useAuthStore()
  const { showSuccess, showWarn } = useNotification()

  const loading = ref(false)
  const loadingCotizaciones = ref(false)
  const errorMessage = ref('')

  const selectedFile = ref(null)
  const selectedCotizacionId = ref(cotizacionProp?.value?.idCotizacion || null)
  const cotizacionesDisponibles = ref([])
  const cambiarCotizacionActiva = ref(false)

  const form = ref({
    tipoAbono: 'ADELANTO',
    moneda: 'USD',
    montoAbonado: null,
    tipoCambio: DEFAULT_TIPO_CAMBIO,
    metodoPago: 'TRANSFERENCIA_BANCARIA',
    bancoOrigen: '',
    bancoDestino: 'BCP Dólares Cta Cte 191-0023412-1-44',
    numeroOperacion: '',
    fechaOperacion: getTodayDateInput(),
    observaciones: ''
  })

  function filterCotizacionesCobrables(list = []) {
    return (list || []).filter((c) => c.estado === 'APROBADA' && c.estadoPago !== 'PAGADO_TOTAL')
  }

  const activeCotizacion = computed(() => {
    if (cotizacionProp?.value && !cambiarCotizacionActiva.value && !selectedCotizacionId.value) {
      return cotizacionProp.value
    }
    if (selectedCotizacionId.value) {
      return (
        cotizacionesDisponibles.value.find(
          (c) => Number(c.idCotizacion) === Number(selectedCotizacionId.value)
        ) ||
        cotizacionProp?.value ||
        null
      )
    }
    return cotizacionProp?.value || null
  })

  const cotizacionOptions = computed(() =>
    cotizacionesDisponibles.value.map((c) => {
      const saldo = c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0
      return {
        label: `${c.codigoCotizacion} — ${c.clienteRazonSocial || 'Cliente'} (Saldo: ${formatUsd(saldo)})`,
        value: c.idCotizacion,
        raw: c
      }
    })
  )

  const balance = computed(() =>
    calculateCotizacionBalance(
      activeCotizacion.value,
      form.value.montoAbonado,
      form.value.moneda,
      form.value.tipoCambio
    )
  )

  const montoEquivalentePen = computed(() =>
    convertAmountToPen(form.value.montoAbonado, form.value.moneda, form.value.tipoCambio)
  )

  const isFormValid = computed(() => {
    const hasCotizacion = Boolean(activeCotizacion.value?.idCotizacion)
    const hasMonto = Number(form.value.montoAbonado) > 0
    const hasOperacion = Boolean(form.value.numeroOperacion?.trim())
    const hasBancoDestino = Boolean(form.value.bancoDestino)
    const hasFecha = Boolean(form.value.fechaOperacion)
    return hasCotizacion && hasMonto && hasOperacion && hasBancoDestino && hasFecha
  })

  const checklistItems = computed(() => [
    { label: 'Cotización aprobada seleccionada', done: Boolean(activeCotizacion.value), required: true },
    { label: 'Modalidad de abono definida', done: Boolean(form.value.tipoAbono), required: true },
    { label: 'Monto abonado válido (> 0)', done: Number(form.value.montoAbonado) > 0, required: true },
    { label: 'N° de operación bancaria', done: Boolean(form.value.numeroOperacion?.trim()), required: true },
    { label: 'Cuenta receptora SGA', done: Boolean(form.value.bancoDestino), required: true },
    { label: 'Fecha de depósito / transferencia', done: Boolean(form.value.fechaOperacion), required: true },
    { label: 'Comprobante digital adjunto', done: Boolean(selectedFile.value), required: false }
  ])

  async function loadCotizacionesAprobadas() {
    loadingCotizaciones.value = true
    try {
      const list = await finanzasService.getCotizacionesAprobadas()
      cotizacionesDisponibles.value = filterCotizacionesCobrables(list)
    } catch {
      errorMessage.value = 'No se pudieron cargar las cotizaciones aprobadas.'
    } finally {
      loadingCotizaciones.value = false
    }
  }

  function aplicarMontoInicial() {
    const target = activeCotizacion.value
    if (target && !form.value.montoAbonado) {
      const total = Number(target.totalCotizado || 0)
      const saldo = Number(target.saldoPendiente != null ? target.saldoPendiente : total)
      const sugerido30 = round2(total * 0.3)
      form.value.montoAbonado = Math.min(sugerido30, saldo)
    }
  }

  function setTipoAbono(tipo) {
    form.value.tipoAbono = tipo
    if (!activeCotizacion.value) return
    if (tipo === 'PAGO_TOTAL') {
      const saldo = Number(
        activeCotizacion.value.saldoPendiente != null
          ? activeCotizacion.value.saldoPendiente
          : activeCotizacion.value.totalCotizado || 0
      )
      form.value.montoAbonado = round2(saldo)
    } else if (tipo === 'ADELANTO') {
      aplicarPorcentaje(0.3)
    }
  }

  function calcularMontoSugerido(pct) {
    if (!activeCotizacion.value) return 0
    return round2(Number(activeCotizacion.value.totalCotizado || 0) * pct)
  }

  function aplicarPorcentaje(pct) {
    if (!activeCotizacion.value) return
    const total = Number(activeCotizacion.value.totalCotizado || 0)
    const saldo = Number(activeCotizacion.value.saldoPendiente != null ? activeCotizacion.value.saldoPendiente : total)
    const targetUsd = pct >= 1 ? saldo : round2(total * pct)
    form.value.montoAbonado =
      form.value.moneda === 'PEN'
        ? convertAmountToPen(targetUsd, 'USD', form.value.tipoCambio)
        : targetUsd
  }

  function toggleCambiarCotizacion() {
    cambiarCotizacionActiva.value = !cambiarCotizacionActiva.value
    if (cambiarCotizacionActiva.value && cotizacionesDisponibles.value.length === 0) {
      loadCotizacionesAprobadas()
    }
  }

  function onCotizacionSelected() {
    form.value.montoAbonado = null
    if (activeCotizacion.value) {
      aplicarMontoInicial()
    }
  }

  function processSelectedFile(file) {
    if (!file) return
    if (file.size > MAX_FILE_SIZE_BYTES) {
      errorMessage.value = 'El archivo supera el límite máximo permitido de 10 MB.'
      return
    }
    selectedFile.value = file
    errorMessage.value = ''
  }

  function clearSelectedFile() {
    selectedFile.value = null
  }

  async function init() {
    if (cotizacionesProp?.value && cotizacionesProp.value.length > 0) {
      cotizacionesDisponibles.value = filterCotizacionesCobrables(cotizacionesProp.value)
    } else {
      await loadCotizacionesAprobadas()
    }

    if (cotizacionProp?.value) {
      selectedCotizacionId.value = cotizacionProp.value.idCotizacion
      aplicarMontoInicial()
    }
  }

  if (cotizacionProp) {
    watch(
      cotizacionProp,
      (c) => {
        if (c) {
          selectedCotizacionId.value = c.idCotizacion
          cambiarCotizacionActiva.value = false
          form.value.montoAbonado = null
          aplicarMontoInicial()
        }
      },
      { immediate: true }
    )
  }

  if (cotizacionesProp) {
    watch(
      cotizacionesProp,
      (list) => {
        if (list && list.length > 0) {
          cotizacionesDisponibles.value = filterCotizacionesCobrables(list)
        }
      },
      { immediate: true }
    )
  }

  async function handleSubmit() {
    const targetCotizacion = activeCotizacion.value
    if (!targetCotizacion?.idCotizacion) {
      errorMessage.value = 'Debe seleccionar una cotización aprobada para registrar el pago.'
      return
    }
    if (!form.value.montoAbonado || Number(form.value.montoAbonado) <= 0) {
      errorMessage.value = 'Ingrese un monto abonado válido superior a cero.'
      return
    }
    if (!form.value.numeroOperacion?.trim()) {
      errorMessage.value = 'El número de operación bancaria es obligatorio.'
      return
    }

    loading.value = true
    errorMessage.value = ''

    try {
      let idDocComprobante = null

      if (selectedFile.value) {
        try {
          const uploadResult = await documentoService.upload([selectedFile.value], {
            categoria: 'COMPROBANTE_PAGO',
            descripcion: `Voucher depósito cotización ${targetCotizacion.codigoCotizacion} - Op: ${form.value.numeroOperacion}`,
            entidadTipo: 'COTIZACION',
            idEntidad: targetCotizacion.idCotizacion,
            idUsuario: authStore.user?.idUsuario || 1
          })
          if (Array.isArray(uploadResult) && uploadResult.length > 0) {
            idDocComprobante = uploadResult[0].idDocumento
          }
        } catch {
          showWarn(
            'El archivo adjunto no pudo cargarse; el abono se registrará sin comprobante digital.',
            'Adjunto no subido'
          )
        }
      }

      const payload = {
        idCotizacion: targetCotizacion.idCotizacion,
        tipoAbono: form.value.tipoAbono,
        montoAbonado: Number(form.value.montoAbonado),
        moneda: form.value.moneda,
        tipoCambio: Number(form.value.tipoCambio || DEFAULT_TIPO_CAMBIO),
        metodoPago: form.value.metodoPago,
        bancoOrigen: form.value.bancoOrigen || '',
        bancoDestino: form.value.bancoDestino,
        numeroOperacion: form.value.numeroOperacion.trim(),
        fechaOperacion: form.value.fechaOperacion,
        observaciones: form.value.observaciones ? form.value.observaciones.trim() : null,
        idDocumentoComprobante: idDocComprobante
      }

      const nuevoPago = await finanzasService.registrarPagoCotizacion(
        targetCotizacion.idCotizacion,
        payload
      )

      showSuccess(
        `Abono registrado para ${targetCotizacion.codigoCotizacion} y enviado a validación contable.`,
        'Abono Registrado'
      )

      if (typeof onSaved === 'function') {
        await onSaved({ pago: nuevoPago, cotizacion: targetCotizacion })
      }
    } catch (err) {
      errorMessage.value =
        err.response?.data?.message ||
        'Error al registrar el comprobante de pago. Verifique los datos bancarios.'
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    loadingCotizaciones,
    errorMessage,
    selectedFile,
    selectedCotizacionId,
    cotizacionesDisponibles,
    cotizacionOptions,
    cambiarCotizacionActiva,
    activeCotizacion,
    form,
    balance,
    montoEquivalentePen,
    isFormValid,
    checklistItems,
    init,
    setTipoAbono,
    calcularMontoSugerido,
    aplicarPorcentaje,
    toggleCambiarCotizacion,
    onCotizacionSelected,
    processSelectedFile,
    clearSelectedFile,
    handleSubmit
  }
}
