import { ref, computed } from 'vue'
import finanzasService from '../services/finanzasService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { getNowDateTimeInput } from '@/shared/utils/dateUtils'
import { DEFAULT_TIPO_CAMBIO, calculateLiquidacionTotals } from '../utils/finanzasCalculations'

const DEFAULT_EMISION_FORM = () => ({
  idExpediente: null,
  totalTributosSunat: 0,
  gastosAlmacenPuerto: 0,
  fleteTransporteLocal: 0,
  honorariosAgencia: 0,
  tipoCambio: DEFAULT_TIPO_CAMBIO,
  aplicaDetraccion: true,
  porcentajeDetraccion: 12.0,
  numeroFactura: '',
  observaciones: ''
})

const DEFAULT_PAGO_FORM = (liquidacion = null) => ({
  metodoPago: liquidacion?.metodoPago || 'TRANSFERENCIA_BANCARIA',
  bancoDestino: liquidacion?.bancoDestino || 'BCP',
  numeroOperacion: liquidacion?.numeroOperacion || '',
  numeroFactura: liquidacion?.numeroFactura || '',
  fechaPago: liquidacion?.fechaPago
    ? String(liquidacion.fechaPago).slice(0, 16)
    : getNowDateTimeInput(),
  observaciones: ''
})

/**
 * Composable para emisión, edición, cobranza bancaria y anulación de liquidaciones financieras.
 */
export function useLiquidacionForm({ onMutated } = {}) {
  const authStore = useAuthStore()
  const { showSuccess, showWarn, showError } = useNotification()
  const { confirmAction } = useConfirmation()

  // Estado del formulario de emisión / edición
  const showFormModal = ref(false)
  const isEditMode = ref(false)
  const editingLiquidacionId = ref(null)
  const isExpedienteFixed = ref(false)
  const loadingEstimado = ref(false)
  const submittingForm = ref(false)
  const form = ref(DEFAULT_EMISION_FORM())

  // Estado del modal de cobranza
  const showPagoModal = ref(false)
  const selectedLiquidacionPago = ref(null)
  const pagoForm = ref(DEFAULT_PAGO_FORM())
  const voucherFile = ref(null)
  const submittingPago = ref(false)

  const totals = computed(() => calculateLiquidacionTotals(form.value))

  async function fetchEstimado(idExpediente) {
    if (!idExpediente) return
    loadingEstimado.value = true
    try {
      const est = await finanzasService.calcularEstimado(idExpediente)
      if (est) {
        form.value.totalTributosSunat = Number(est.totalTributosSunatEstimado || 0)
        form.value.gastosAlmacenPuerto = Number(est.gastosAlmacenPuertoEstimado || 0)
        form.value.fleteTransporteLocal = Number(est.fleteTransporteLocalEstimado || 0)
        form.value.honorariosAgencia = Number(est.honorariosAgenciaEstimado || 0)
        form.value.tipoCambio = Number(est.tipoCambioSugerido || DEFAULT_TIPO_CAMBIO)
        form.value.aplicaDetraccion = est.aplicaDetraccion !== false
      }
    } catch {
      showWarn('No se pudo precargar el estimado automático; ingrese los montos manualmente.')
    } finally {
      loadingEstimado.value = false
    }
  }

  async function openEmitirModal(expediente = null) {
    isEditMode.value = false
    editingLiquidacionId.value = null
    form.value = DEFAULT_EMISION_FORM()

    if (expediente?.idExpediente) {
      isExpedienteFixed.value = true
      form.value.idExpediente = expediente.idExpediente
      showFormModal.value = true
      await fetchEstimado(expediente.idExpediente)
    } else {
      isExpedienteFixed.value = false
      showFormModal.value = true
    }
  }

  function openEditarModal(liquidacion) {
    if (!liquidacion) return
    isEditMode.value = true
    editingLiquidacionId.value = liquidacion.idLiquidacion
    isExpedienteFixed.value = true
    form.value = {
      idExpediente: liquidacion.idExpediente,
      totalTributosSunat: Number(liquidacion.totalTributosSunat || 0),
      gastosAlmacenPuerto: Number(liquidacion.gastosAlmacenPuerto || 0),
      fleteTransporteLocal: Number(liquidacion.fleteTransporteLocal || 0),
      honorariosAgencia: Number(liquidacion.honorariosAgencia || 0),
      tipoCambio: Number(liquidacion.tipoCambio || DEFAULT_TIPO_CAMBIO),
      aplicaDetraccion: liquidacion.aplicaDetraccion !== false,
      porcentajeDetraccion: Number(liquidacion.porcentajeDetraccion || 12),
      numeroFactura: liquidacion.numeroFactura || '',
      observaciones: liquidacion.observaciones || ''
    }
    showFormModal.value = true
  }

  async function onExpedienteSelected() {
    if (!form.value.idExpediente || isEditMode.value) return
    await fetchEstimado(form.value.idExpediente)
  }

  async function submitLiquidacion() {
    if (!form.value.idExpediente) {
      showError('Debe seleccionar un expediente aduanero.')
      return
    }
    submittingForm.value = true
    try {
      if (isEditMode.value && editingLiquidacionId.value) {
        const updated = await finanzasService.updateLiquidacion(editingLiquidacionId.value, form.value)
        showSuccess(
          `Liquidación ${updated?.codigoLiquidacion || ''} actualizada correctamente.`,
          'Liquidación Actualizada'
        )
      } else {
        const created = await finanzasService.emitirLiquidacion(form.value)
        showSuccess(
          `Liquidación ${created?.codigoLiquidacion || ''} emitida exitosamente.`,
          'Liquidación Emitida'
        )
      }
      showFormModal.value = false
      if (typeof onMutated === 'function') {
        await onMutated()
      }
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudo guardar la liquidación financiera.')
    } finally {
      submittingForm.value = false
    }
  }

  function openPagoModal(liquidacion) {
    if (!liquidacion) return
    selectedLiquidacionPago.value = liquidacion
    pagoForm.value = DEFAULT_PAGO_FORM(liquidacion)
    voucherFile.value = null
    showPagoModal.value = true
  }

  async function submitPago() {
    if (!selectedLiquidacionPago.value || !pagoForm.value.numeroOperacion?.trim()) {
      showError('El número de operación bancaria es obligatorio.')
      return
    }

    submittingPago.value = true
    try {
      let idDocumentoVoucher = null
      if (voucherFile.value && selectedLiquidacionPago.value.idExpediente) {
        try {
          const uploaded = await documentoService.upload([voucherFile.value], {
            categoria: 'COMPROBANTE_PAGO',
            descripcion: `Voucher cobro liquidación ${selectedLiquidacionPago.value.codigoLiquidacion} - Op: ${pagoForm.value.numeroOperacion}`,
            entidadTipo: 'EXPEDIENTE',
            idEntidad: selectedLiquidacionPago.value.idExpediente,
            idUsuario: authStore.user?.idUsuario || 1
          })
          if (Array.isArray(uploaded) && uploaded.length > 0) {
            idDocumentoVoucher = uploaded[0].idDocumento
          }
        } catch {
          showWarn('No se pudo adjuntar el voucher; el cobro se registrará sin archivo digital.')
        }
      }

      const payload = {
        ...pagoForm.value,
        numeroOperacion: pagoForm.value.numeroOperacion.trim(),
        numeroFactura: pagoForm.value.numeroFactura?.trim() || null,
        observaciones: pagoForm.value.observaciones?.trim() || null,
        idDocumentoVoucher
      }

      await finanzasService.registrarPago(selectedLiquidacionPago.value.idLiquidacion, payload)
      showPagoModal.value = false
      showSuccess(
        `Cobro registrado conforme para la liquidación ${selectedLiquidacionPago.value.codigoLiquidacion}.`,
        'Cobro Registrado'
      )

      if (typeof onMutated === 'function') {
        await onMutated()
      }
    } catch (err) {
      showError(err.response?.data?.message || 'Error al registrar el cobro bancario.')
    } finally {
      submittingPago.value = false
    }
  }

  async function confirmAnular(liquidacion) {
    if (!liquidacion) return
    const confirmed = await confirmAction({
      message: `¿Está seguro de anular la liquidación ${liquidacion.codigoLiquidacion}? Esta acción no se puede deshacer.`,
      header: 'Confirmar Anulación de Liquidación',
      acceptLabel: 'Anular',
      acceptClass: 'sga-confirm-delete-btn'
    })
    if (!confirmed) return

    try {
      await finanzasService.anularLiquidacion(liquidacion.idLiquidacion)
      showSuccess(`Liquidación ${liquidacion.codigoLiquidacion} anulada correctamente.`)
      if (typeof onMutated === 'function') {
        await onMutated()
      }
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudo anular la liquidación.')
    }
  }

  return {
    showFormModal,
    isEditMode,
    isExpedienteFixed,
    loadingEstimado,
    submittingForm,
    form,
    totals,
    openEmitirModal,
    openEditarModal,
    onExpedienteSelected,
    submitLiquidacion,
    showPagoModal,
    selectedLiquidacionPago,
    pagoForm,
    voucherFile,
    submittingPago,
    openPagoModal,
    submitPago,
    confirmAnular
  }
}
