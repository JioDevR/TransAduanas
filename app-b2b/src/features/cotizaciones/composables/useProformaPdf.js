import { ref } from 'vue'
import { portalCotizacionService } from '../services/portalCotizacionService'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'
import { useNotification } from '@/shared/composables/useNotification'

const DEFAULT_PDF_ERROR = 'No se pudo generar la proforma PDF de la cotización.'
const INVALID_PDF_ERROR = 'El servidor devolvió un archivo PDF vacío o inválido.'

function buildProformaFilename(cotizacion) {
  const raw = cotizacion?.codigoCotizacion ? String(cotizacion.codigoCotizacion).trim() : ''
  const normalized = raw.replace(/[^A-Za-z0-9._-]/g, '-').replace(/^-+|-+$|^\.+|\.+$/g, '')
  const safeCode = normalized && /[A-Za-z0-9]/.test(normalized)
    ? normalized
    : `cotizacion-${cotizacion?.idCotizacion || 'proforma'}`
  return `proforma-${safeCode}.pdf`
}

async function extractBlobErrorMessage(blob) {
  if (!(blob instanceof Blob)) return null
  try {
    const text = await blob.text()
    const parsed = JSON.parse(text)
    if (parsed?.message) return parsed.message
  } catch {
    // El Blob de error no contiene JSON válido
  }
  return null
}

async function resolveErrorMessage(err) {
  const data = err?.response?.data
  if (data instanceof Blob) {
    return (await extractBlobErrorMessage(data)) || DEFAULT_PDF_ERROR
  }
  return data?.message || err?.message || DEFAULT_PDF_ERROR
}

async function validatePdfPayload(blob) {
  if (!blob) {
    throw new Error(INVALID_PDF_ERROR)
  }
  if (blob instanceof Blob) {
    if (blob.size <= 0) {
      throw new Error(INVALID_PDF_ERROR)
    }
    const mime = String(blob.type || '').toLowerCase()
    if (mime && !mime.includes('pdf') && mime !== 'application/octet-stream') {
      const jsonMessage = await extractBlobErrorMessage(blob)
      throw new Error(jsonMessage || INVALID_PDF_ERROR)
    }
  }
}

/**
 * Composable compartido para abrir/imprimir y descargar proformas PDF generadas en el backend.
 */
export function useProformaPdf() {
  const { showError, showInfo } = useNotification()
  const pdfAction = ref(null)
  const activeCotizacionId = ref(null)

  async function executePdf(cotizacion, download) {
    if (!cotizacion?.idCotizacion || pdfAction.value) return

    pdfAction.value = download ? 'download' : 'preview'
    activeCotizacionId.value = cotizacion.idCotizacion
    try {
      const blob = await portalCotizacionService.descargarProformaPdf(cotizacion.idCotizacion, { download })
      await validatePdfPayload(blob)

      const filename = buildProformaFilename(cotizacion)
      if (download) {
        downloadBlob(blob, filename, 'application/pdf')
      } else {
        const opened = openBlobInNewTab(blob, 'application/pdf')
        if (!opened) {
          downloadBlob(blob, filename, 'application/pdf')
          showInfo('El navegador bloqueó la vista previa; se descargó la proforma PDF.', 'Proforma Descargada')
        }
      }
    } catch (err) {
      showError(await resolveErrorMessage(err))
    } finally {
      pdfAction.value = null
      activeCotizacionId.value = null
    }
  }

  function abrirProformaPdf(cotizacion) {
    return executePdf(cotizacion, false)
  }

  function descargarProformaPdf(cotizacion) {
    return executePdf(cotizacion, true)
  }

  return {
    pdfAction,
    activeCotizacionId,
    abrirProformaPdf,
    descargarProformaPdf
  }
}

export default useProformaPdf
