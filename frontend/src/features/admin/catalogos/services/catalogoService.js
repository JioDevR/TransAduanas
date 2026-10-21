import apiClient from '@/core/api/apiClient'

export const catalogoService = {
  // ==========================================
  // CATÁLOGO DE PUERTOS
  // ==========================================
  async getPuertos(soloActivos = false) {
    const response = await apiClient.get('/catalogos/puertos', {
      params: { soloActivos }
    })
    return response.data
  },

  async getPuertoById(id) {
    const response = await apiClient.get(`/catalogos/puertos/${id}`)
    return response.data
  },

  async createPuerto(data) {
    const response = await apiClient.post('/catalogos/puertos', data)
    return response.data
  },

  async updatePuerto(id, data) {
    const response = await apiClient.put(`/catalogos/puertos/${id}`, data)
    return response.data
  },

  async deletePuerto(id) {
    const response = await apiClient.delete(`/catalogos/puertos/${id}`)
    return response.data
  },

  async reactivarPuerto(id) {
    const response = await apiClient.patch(`/catalogos/puertos/${id}/reactivar`)
    return response.data
  },

  /**
   * Descargar reporte de puertos en PDF generado con JasperReports
   */
  async downloadPuertosPdf() {
    const response = await apiClient.get('/catalogos/puertos/exportar-pdf', {
      responseType: 'blob'
    })
    return response.data
  },

  // ==========================================
  // CATÁLOGO DE TIPOS DE CARGA
  // ==========================================
  async getTiposCarga(soloActivos = false) {
    const response = await apiClient.get('/catalogos/tipos-carga', {
      params: { soloActivos }
    })
    return response.data
  },

  async getTipoCargaById(id) {
    const response = await apiClient.get(`/catalogos/tipos-carga/${id}`)
    return response.data
  },

  async createTipoCarga(data) {
    const response = await apiClient.post('/catalogos/tipos-carga', data)
    return response.data
  },

  async updateTipoCarga(id, data) {
    const response = await apiClient.put(`/catalogos/tipos-carga/${id}`, data)
    return response.data
  },

  async deleteTipoCarga(id) {
    const response = await apiClient.delete(`/catalogos/tipos-carga/${id}`)
    return response.data
  },

  async reactivarTipoCarga(id) {
    const response = await apiClient.patch(`/catalogos/tipos-carga/${id}/reactivar`)
    return response.data
  },

  // ==========================================
  // CATÁLOGO DE MERCANCÍAS (PARTIDAS ARANCELARIAS)
  // ==========================================
  async getMercancias(soloActivos = false, categoria = '') {
    const response = await apiClient.get('/catalogos/mercancias', {
      params: { soloActivos, categoria: categoria || undefined }
    })
    return response.data
  },

  async getMercanciaById(id) {
    const response = await apiClient.get(`/catalogos/mercancias/${id}`)
    return response.data
  }
}

export default catalogoService
