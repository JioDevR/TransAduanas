import { ref } from 'vue'
import { portalExpedienteService } from '../services/portalExpedienteService'
import { useNotification } from '@/shared/composables/useNotification'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'

/**
 * Composable de detalle y seguimiento en tiempo real para la vista dedicada de Tracking de Expediente.
 */
export function useExpedienteTrackingDetail(getId) {
  const { showSuccess, showError, showInfo } = useNotification()

  const expediente = ref(null)
  const documentos = ref([])
  const loading = ref(true)
  const loadingDocs = ref(false)
  const refreshing = ref(false)
  const savingSimple = ref(false)

  async function cargarDetalle({ silent = false } = {}) {
    const id = typeof getId === 'function' ? getId() : getId
    if (!id) {
      loading.value = false
      expediente.value = null
      documentos.value = []
      return
    }

    if (silent) {
      refreshing.value = true
    } else {
      loading.value = true
    }
    loadingDocs.value = true

    try {
      const [expResult, docsResult] = await Promise.allSettled([
        portalExpedienteService.obtenerExpedientePorId(id),
        portalExpedienteService.listarDocumentosExpediente(id)
      ])

      if (expResult.status === 'fulfilled') {
        expediente.value = expResult.value || null
      } else {
        expediente.value = null
        showError(
          expResult.reason?.response?.data?.message ||
            'No se pudo obtener el tracking del expediente solicitado.'
        )
      }

      if (docsResult.status === 'fulfilled') {
        documentos.value = docsResult.value || []
      } else {
        documentos.value = []
      }

      if (silent && expResult.status === 'fulfilled' && expResult.value) {
        showInfo('Estado del expediente actualizado.', 'Tracking al día')
      }
    } finally {
      loading.value = false
      loadingDocs.value = false
      refreshing.value = false
    }
  }

  async function guardarDatosCliente({ idExpediente, payload }) {
    if (!idExpediente) return null
    savingSimple.value = true
    try {
      const actualizado = await portalExpedienteService.actualizarDatosCliente(idExpediente, payload)
      if (actualizado) {
        expediente.value = actualizado
      }
      showSuccess('Datos operativos actualizados correctamente.', 'Expediente Actualizado')
      return actualizado
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudieron actualizar los datos del expediente.')
      return null
    } finally {
      savingSimple.value = false
    }
  }

  async function handleDescargar(doc) {
    if (!doc?.idDocumento) return
    try {
      const blob = await portalExpedienteService.descargarDocumento(doc.idDocumento)
      downloadBlob(
        blob,
        doc.nombreOriginal || `documento-${doc.idDocumento}.pdf`,
        doc.formatoMime || 'application/pdf'
      )
      showSuccess(`Descargando ${doc.nombreOriginal || 'documento'}`, 'Bóveda Digital')
    } catch {
      showError('Error al descargar el documento del servidor')
    }
  }

  async function handlePrevisualizar(doc) {
    if (!doc?.idDocumento) return
    try {
      const blob = await portalExpedienteService.previsualizarDocumento(doc.idDocumento)
      openBlobInNewTab(blob, doc.formatoMime || 'application/pdf')
    } catch {
      showError('Error al previsualizar el documento en el navegador')
    }
  }

  async function handleVerInformeConformidad(exp = expediente.value) {
    if (!exp?.idExpediente) return
    try {
      const blob = await portalExpedienteService.descargarInformeConformidadPdf(exp.idExpediente, {
        download: false
      })
      openBlobInNewTab(blob, 'application/pdf')
    } catch {
      showError('No se pudo abrir el Informe de Conformidad de Entrega')
    }
  }

  async function handleDescargarInformeConformidad(exp = expediente.value) {
    if (!exp?.idExpediente) return
    try {
      const blob = await portalExpedienteService.descargarInformeConformidadPdf(exp.idExpediente, {
        download: true
      })
      downloadBlob(blob, `informe-conformidad-${exp.codigoExpediente}.pdf`, 'application/pdf')
    } catch {
      showError('No se pudo descargar el Informe de Conformidad de Entrega')
    }
  }

  async function copiarCodigo(valor, label = 'Código') {
    if (!valor) return
    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(String(valor))
        showSuccess(`${label} ${valor} copiado al portapapeles.`, 'Copiado', 2200)
      }
    } catch {
      showError('No se pudo copiar al portapapeles.')
    }
  }

  return {
    expediente,
    documentos,
    loading,
    loadingDocs,
    refreshing,
    savingSimple,
    cargarDetalle,
    guardarDatosCliente,
    handleDescargar,
    handlePrevisualizar,
    handleVerInformeConformidad,
    handleDescargarInformeConformidad,
    copiarCodigo
  }
}
