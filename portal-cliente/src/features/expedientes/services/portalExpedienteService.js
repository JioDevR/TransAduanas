import apiClient from '@/core/api/apiClient'

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
   * Obtiene la trazabilidad detallada de un expediente (hitos, canal de control, levante).
   * @param {number|string} id - ID del expediente
   */
  async obtenerDetalle(id) {
    const response = await apiClient.get(`/portal/expedientes/${id}`)
    return response.data?.data || null
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
  async descargarDocumento(idDocumento) {
    const response = await apiClient.get(`/documentos/${idDocumento}/descargar`, {
      responseType: 'blob'
    })
    return response.data
  },

  /**
   * Obtiene el blob de un documento para previsualización inmediata en navegador.
   * @param {number|string} idDocumento - ID del documento adjunto
   */
  async previsualizarDocumento(idDocumento) {
    const response = await apiClient.get(`/documentos/${idDocumento}/preview`, {
      responseType: 'blob'
    })
    return response.data
  }
}

export default portalExpedienteService
