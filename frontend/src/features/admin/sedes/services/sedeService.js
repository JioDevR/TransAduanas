import apiClient from '@/core/api/apiClient'

export const sedeService = {
  /**
   * Listar todas las sedes o solo las operativas
   * @param {Boolean} soloOperativas
   */
  async getSedes(soloOperativas = false) {
    const response = await apiClient.get('/sedes', {
      params: { soloOperativas }
    })
    return response.data.data
  },

  /**
   * Buscar sede por ID
   * @param {Number} id
   */
  async getSedeById(id) {
    const response = await apiClient.get(`/sedes/${id}`)
    return response.data.data
  },

  /**
   * Crear nueva sede
   * @param {Object} sedeData
   */
  async createSede(sedeData) {
    const response = await apiClient.post('/sedes', sedeData)
    return response.data
  },

  /**
   * Actualizar sede existente
   * @param {Number} id
   * @param {Object} sedeData
   */
  async updateSede(id, sedeData) {
    const response = await apiClient.put(`/sedes/${id}`, sedeData)
    return response.data
  },

  /**
   * Desactivar sede (baja lógica por estado)
   * @param {Number} id
   */
  async deleteSede(id) {
    const response = await apiClient.delete(`/sedes/${id}`)
    return response.data
  },

  /**
   * Reactivar sede inactiva
   * @param {Number} id
   */
  async reactivateSede(id) {
    const response = await apiClient.patch(`/sedes/${id}/reactivar`)
    return response.data
  },

  /**
   * Descargar reporte de sedes en PDF generado con JasperReports
   */
  async downloadPdf() {
    const response = await apiClient.get('/sedes/exportar-pdf', {
      responseType: 'blob'
    })
    return response.data
  }
}

export default sedeService
