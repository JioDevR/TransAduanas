import apiClient from '@/core/api/apiClient'

export const transporteFlotaService = {
  // ==========================================
  // CONDUCTORES
  // ==========================================
  async getConductores(paramsOrSoloActivos = false) {
    const params =
      typeof paramsOrSoloActivos === 'object' && paramsOrSoloActivos !== null
        ? paramsOrSoloActivos
        : { soloActivos: Boolean(paramsOrSoloActivos) }
    const response = await apiClient.get('/transporte/conductores', { params })
    return response.data.data
  },

  async createConductor(data) {
    const response = await apiClient.post('/transporte/conductores', data)
    return response.data.data
  },

  async updateConductor(id, data) {
    const response = await apiClient.put(`/transporte/conductores/${id}`, data)
    return response.data.data
  },

  async deleteConductor(id) {
    const response = await apiClient.delete(`/transporte/conductores/${id}`)
    return response.data
  },

  async reactivarConductor(id) {
    const response = await apiClient.patch(`/transporte/conductores/${id}/reactivar`)
    return response.data
  },

  // ==========================================
  // UNIDADES DE TRANSPORTE (FLOTA MTC)
  // ==========================================
  async getUnidades(params = {}) {
    const response = await apiClient.get('/transporte/unidades', { params })
    return response.data.data
  },

  async createUnidad(data) {
    const response = await apiClient.post('/transporte/unidades', data)
    return response.data.data
  },

  async updateUnidad(id, data) {
    const response = await apiClient.put(`/transporte/unidades/${id}`, data)
    return response.data.data
  },

  async deleteUnidad(id) {
    const response = await apiClient.delete(`/transporte/unidades/${id}`)
    return response.data
  },

  async reactivarUnidad(id) {
    const response = await apiClient.patch(`/transporte/unidades/${id}/reactivar`)
    return response.data
  },

  // ==========================================
  // DESPACHOS TERRESTRES & TRACKING
  // ==========================================
  async getDespachos(params = {}) {
    const response = await apiClient.get('/transporte/despachos', { params })
    return response.data.data
  },

  async getDespachoById(id) {
    const response = await apiClient.get(`/transporte/despachos/${id}`)
    return response.data.data
  },

  async getDespachoByExpediente(idExpediente) {
    const response = await apiClient.get(`/transporte/despachos/expediente/${idExpediente}`)
    return response.data.data
  },

  async createDespacho(data) {
    const response = await apiClient.post('/transporte/despachos', data)
    return response.data.data
  },

  async aceptarSolicitud(id) {
    const response = await apiClient.patch(`/transporte/despachos/${id}/aceptar`)
    return response.data.data
  },

  async asignarRecursos(id, payload) {
    const response = await apiClient.patch(`/transporte/despachos/${id}/asignar-recursos`, payload)
    return response.data.data
  },

  async iniciarRuta(id, payload = {}) {
    const response = await apiClient.patch(`/transporte/despachos/${id}/iniciar-ruta`, payload)
    return response.data.data
  },

  async confirmarEntrega(id, payload) {
    const response = await apiClient.patch(`/transporte/despachos/${id}/confirmar-entrega`, payload)
    return response.data.data
  },

  async reportarIncidencia(id, incidencia) {
    const response = await apiClient.post(`/transporte/despachos/${id}/incidencia`, { incidencia })
    return response.data.data
  },

  async deleteDespacho(id) {
    const response = await apiClient.delete(`/transporte/despachos/${id}`)
    return response.data
  }
}

export const transporteService = transporteFlotaService
export default transporteFlotaService
