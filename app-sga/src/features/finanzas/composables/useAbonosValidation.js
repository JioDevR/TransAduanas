import { ref, onBeforeUnmount } from 'vue'
import finanzasService from '../services/finanzasService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { useNotification } from '@/shared/composables/useNotification'

/**
 * Composable para la validación contable de abonos/anticipos B2B
 * y la previsualización segura de comprobantes (vouchers).
 */
export function useAbonosValidation({ onValidated } = {}) {
  const { showSuccess, showError } = useNotification()

  // Estado de validación
  const showValidarModal = ref(false)
  const selectedAnticipo = ref(null)
  const validarAccion = ref('APROBAR')
  const observacionFinanzas = ref('')
  const confirmacionCotejo = ref(false)
  const submittingValidacion = ref(false)

  // Estado del visor de voucher
  const showVoucherModal = ref(false)
  const voucherAnticipo = ref(null)
  const voucherBlobUrl = ref(null)
  const voucherMimeType = ref('')
  const loadingVoucher = ref(false)

  function revokeVoucherUrl() {
    if (voucherBlobUrl.value && voucherBlobUrl.value.startsWith('blob:')) {
      window.URL.revokeObjectURL(voucherBlobUrl.value)
    }
    voucherBlobUrl.value = null
  }

  onBeforeUnmount(() => {
    revokeVoucherUrl()
  })

  function openValidar(anticipo, accion = 'APROBAR') {
    if (!anticipo) return
    const normalized = String(accion || 'APROBAR').toUpperCase()
    selectedAnticipo.value = anticipo
    validarAccion.value = normalized
    confirmacionCotejo.value = false
    observacionFinanzas.value =
      normalized === 'APROBAR'
        ? 'Abono verificado y conforme en extracto bancario de TransAduanas.'
        : ''
    showValidarModal.value = true
  }

  async function submitValidar() {
    if (!selectedAnticipo.value) return

    const accion = validarAccion.value
    if (accion === 'APROBAR' && !confirmacionCotejo.value) {
      showError('Debe confirmar que ha cotejado la operación en el extracto bancario.')
      return
    }
    if ((accion === 'OBSERVAR' || accion === 'RECHAZAR') && !observacionFinanzas.value?.trim()) {
      showError('Debe ingresar el motivo detallado para notificar al cliente.')
      return
    }

    submittingValidacion.value = true
    try {
      await finanzasService.validarAnticipo(selectedAnticipo.value.idPago, {
        accion,
        observacionFinanzas: observacionFinanzas.value.trim()
      })

      const codigo = selectedAnticipo.value.codigoPago
      showValidarModal.value = false
      if (showVoucherModal.value) {
        closeVoucherPreview()
      }

      if (accion === 'APROBAR') {
        showSuccess(
          `Abono ${codigo} aprobado. La cotización quedó habilitada para apertura de expediente aduanero.`,
          'Abono Aprobado',
          4500
        )
      } else if (accion === 'OBSERVAR') {
        showSuccess(
          `Abono ${codigo} observado. Se notificó al cliente vía Portal B2B para su corrección.`,
          'Abono Observado',
          4500
        )
      } else {
        showSuccess(
          `Abono ${codigo} rechazado y notificado al cliente vía Portal B2B.`,
          'Abono Rechazado',
          4500
        )
      }

      if (typeof onValidated === 'function') {
        await onValidated()
      }
    } catch (err) {
      showError(err.response?.data?.message || 'Error al procesar la validación bancaria.')
    } finally {
      submittingValidacion.value = false
    }
  }

  async function openVoucherPreview(anticipo) {
    if (!anticipo) return
    revokeVoucherUrl()
    voucherAnticipo.value = { ...anticipo }
    voucherMimeType.value = ''
    loadingVoucher.value = true
    showVoucherModal.value = true

    try {
      let idDoc = anticipo.idDocumentoComprobante

      if (!idDoc && anticipo.idCotizacion) {
        const docs = await documentoService.getDocumentos({ idCotizacion: anticipo.idCotizacion })
        if (Array.isArray(docs) && docs.length > 0) {
          const docVoucher =
            docs.find(
              (d) =>
                d.categoria === 'PAGO' ||
                d.categoria === 'COMPROBANTE' ||
                d.categoria === 'COMPROBANTE_PAGO' ||
                d.entidadTipo === 'COTIZACION_PAGO' ||
                d.nombreOriginal?.toLowerCase().includes('voucher') ||
                d.descripcion?.toLowerCase().includes('voucher') ||
                d.descripcion?.toLowerCase().includes('pago')
            ) || docs[0]
          idDoc = docVoucher.idDocumento
          voucherAnticipo.value.idDocumentoComprobante = idDoc
          voucherAnticipo.value.documentoComprobanteNombre = docVoucher.nombreOriginal
        }
      }

      if (idDoc) {
        const res = await documentoService.getPreviewBlobUrl(idDoc)
        voucherBlobUrl.value = res.url
        voucherMimeType.value = res.mimeType || 'application/pdf'
      } else if (anticipo.documentoComprobanteUrl) {
        voucherBlobUrl.value = anticipo.documentoComprobanteUrl
        voucherMimeType.value = anticipo.documentoComprobanteUrl.toLowerCase().endsWith('.pdf')
          ? 'application/pdf'
          : 'image/jpeg'
      } else {
        showError('Esta operación no cuenta con archivo digital adjunto.')
        showVoucherModal.value = false
      }
    } catch {
      showError('No se pudo recuperar la previsualización del comprobante bancario.')
      showVoucherModal.value = false
    } finally {
      loadingVoucher.value = false
    }
  }

  function closeVoucherPreview() {
    revokeVoucherUrl()
    showVoucherModal.value = false
    voucherAnticipo.value = null
  }

  async function descargarVoucherActual() {
    if (!voucherAnticipo.value) return
    const idDoc = voucherAnticipo.value.idDocumentoComprobante
    const nombre =
      voucherAnticipo.value.documentoComprobanteNombre ||
      `Voucher_${voucherAnticipo.value.codigoPago || 'comprobante'}.pdf`

    if (idDoc) {
      try {
        await documentoService.downloadDocumento(idDoc, nombre)
      } catch {
        showError('No se pudo descargar el archivo del comprobante.')
      }
    } else if (voucherBlobUrl.value) {
      const a = document.createElement('a')
      a.href = voucherBlobUrl.value
      a.download = nombre
      a.click()
    }
  }

  return {
    showValidarModal,
    selectedAnticipo,
    validarAccion,
    observacionFinanzas,
    confirmacionCotejo,
    submittingValidacion,
    showVoucherModal,
    voucherAnticipo,
    voucherBlobUrl,
    voucherMimeType,
    loadingVoucher,
    openValidar,
    submitValidar,
    openVoucherPreview,
    closeVoucherPreview,
    descargarVoucherActual
  }
}
