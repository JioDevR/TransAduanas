import apiClient from '@/core/api/apiClient'

export const catalogoService = {
  // ==========================================
  // CATÁLOGO DE PUERTOS
  // ==========================================
  async getPuertos(soloActivos = false) {
    const response = await apiClient.get('/catalogos/puertos', {
      params: { soloActivos }
    })
    return response.data?.data ?? response.data
  },

  async getPuertoById(id) {
    const response = await apiClient.get(`/catalogos/puertos/${id}`)
    return response.data?.data ?? response.data
  },

  async createPuerto(data) {
    const response = await apiClient.post('/catalogos/puertos', data)
    return response.data?.data ?? response.data
  },

  async updatePuerto(id, data) {
    const response = await apiClient.put(`/catalogos/puertos/${id}`, data)
    return response.data?.data ?? response.data
  },

  async deletePuerto(id) {
    const response = await apiClient.delete(`/catalogos/puertos/${id}`)
    return response.data
  },

  async reactivarPuerto(id) {
    const response = await apiClient.patch(`/catalogos/puertos/${id}/reactivar`)
    return response.data?.data ?? response.data
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
    return response.data?.data ?? response.data
  },

  async getTipoCargaById(id) {
    const response = await apiClient.get(`/catalogos/tipos-carga/${id}`)
    return response.data?.data ?? response.data
  },

  async createTipoCarga(data) {
    const response = await apiClient.post('/catalogos/tipos-carga', data)
    return response.data?.data ?? response.data
  },

  async updateTipoCarga(id, data) {
    const response = await apiClient.put(`/catalogos/tipos-carga/${id}`, data)
    return response.data?.data ?? response.data
  },

  async deleteTipoCarga(id) {
    const response = await apiClient.delete(`/catalogos/tipos-carga/${id}`)
    return response.data
  },

  async reactivarTipoCarga(id) {
    const response = await apiClient.patch(`/catalogos/tipos-carga/${id}/reactivar`)
    return response.data?.data ?? response.data
  },

  // ==========================================
  // CATÁLOGO DE MERCANCÍAS (PARTIDAS ARANCELARIAS)
  // ==========================================
  async getMercancias(paramsOrSoloActivos = false, categoria = '') {
    const params = typeof paramsOrSoloActivos === 'object'
      ? paramsOrSoloActivos
      : { soloActivos: paramsOrSoloActivos, categoria: categoria || undefined }
    const response = await apiClient.get('/catalogos/mercancias', { params })
    return response.data?.data ?? response.data
  },

  async getMercanciaById(id) {
    const response = await apiClient.get(`/catalogos/mercancias/${id}`)
    return response.data?.data ?? response.data
  },

  async getMercanciaByCodigo(codigo) {
    const response = await apiClient.get(`/catalogos/mercancias/codigo/${codigo}`)
    return response.data?.data ?? response.data
  },

  async createMercancia(data) {
    const response = await apiClient.post('/catalogos/mercancias', data)
    return response.data?.data ?? response.data
  },

  async updateMercancia(id, data) {
    const response = await apiClient.put(`/catalogos/mercancias/${id}`, data)
    return response.data?.data ?? response.data
  },

  async deleteMercancia(id) {
    const response = await apiClient.delete(`/catalogos/mercancias/${id}`)
    return response.data
  },

  async reactivarMercancia(id) {
    const response = await apiClient.patch(`/catalogos/mercancias/${id}/reactivar`)
    return response.data?.data ?? response.data
  }
}

export default catalogoService
