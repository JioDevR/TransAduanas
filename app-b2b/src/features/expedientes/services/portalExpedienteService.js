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
   * Obtiene el detalle actualizado de un expediente aduanero.
   * @param {number|string} id
   */
  async obtenerExpedientePorId(id) {
    const response = await apiClient.get(`/portal/expedientes/${id}`)
    return response.data?.data || null
  },

  /**
   * Genera un expediente simple desde el Portal B2B completando el contacto exterior.
   * @param {Object} payload
   */
  async crearExpedienteSimple(payload) {
    const response = await apiClient.post('/portal/expedientes/simple', payload)
    return response.data?.data || response.data
  },

  /**
   * Actualiza los datos restantes del cliente (contacto exterior, contenedor, etc.).
   * @param {number|string} id
   * @param {Object} payload
   */
  async actualizarDatosCliente(id, payload) {
    const response = await apiClient.patch(`/portal/expedientes/${id}/datos-cliente`, payload)
    return response.data?.data || response.data
  },

  /**
   * Descarga o previsualiza el Informe de Conformidad de Entrega en PDF.
   * @param {number|string} id
   * @param {{ download?: boolean }} options
   */
  async descargarInformeConformidadPdf(id, { download = false } = {}) {
    const response = await apiClient.get(`/portal/expedientes/${id}/informe-conformidad/pdf`, {
      params: { download },
      responseType: 'blob'
    })
    return response.data
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
