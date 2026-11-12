import apiClient from '@/core/api/apiClient'
import {
  descargarDocumentoBlob,
  previsualizarDocumentoBlob
} from '@/features/cotizaciones/services/portalCotizacionService'

/**
 * Servicio de dominio para Expedientes Aduaneros y Tracking DAM en el Portal del Cliente.
 */
export const portalExpedienteService = {
  /**
   * Obtiene la lista consolidada de expedientes aduaneros del cliente.
   */
  async listarExpedientes() {
    const response = await apiClient.get('/portal/expedientes')
    return response.data?.data || []
  },

  /**
   * Lista los documentos adjuntos asociados a un expediente aduanero (DAM, BL, Facturas).
   * @param {number|string} id - ID del expediente
   */
  async listarDocumentosExpediente(id) {
    const response = await apiClient.get(`/portal/expedientes/${id}/documentos`)
    return response.data?.data || []
  },

  /**
   * Descarga el archivo binario de un documento adjunto.
   * @param {number|string} idDocumento - ID del documento adjunto
   */
  descargarDocumento: descargarDocumentoBlob,

  /**
   * Obtiene el blob de un documento para previsualización inmediata en navegador.
   * @param {number|string} idDocumento - ID del documento adjunto
   */
  previsualizarDocumento: previsualizarDocumentoBlob
}

export default portalExpedienteService
