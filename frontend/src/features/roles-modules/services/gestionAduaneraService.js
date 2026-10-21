import apiClient from '@/core/api/apiClient'

export const gestionAduaneraService = {
  // ==========================================
  // BANDEJA DE AUDITORÍA Y COTIZACIONES
  // ==========================================
  async getCotizaciones(params = {}) {
    const response = await apiClient.get('/cotizaciones', { params })
    return response.data.data
  },

  async getCotizacionById(id) {
    const response = await apiClient.get(`/cotizaciones/${id}`)
    return response.data.data
  },

  /**
   * Revisión Técnica oficial de Agente Aduanero (Paso 2)
   * @param {Number} id ID de cotización
   * @param {Object} payload { idAgenteRevisor, estado, motivoObservacion, observaciones }
   */
  async revisarCotizacion(id, payload) {
    const response = await apiClient.patch(`/cotizaciones/${id}/revision`, payload)
    return response.data.data
  },

  async cambiarEstadoCotizacion(id, { estado, motivoObservacion = '' }) {
    const response = await apiClient.patch(`/cotizaciones/${id}/estado`, {
      estado,
      motivoObservacion
    })
    return response.data.data
  },

  // ==========================================
  // GESTIÓN DE EXPEDIENTES ADUANEROS
  // ==========================================
  async getExpedientes(params = {}) {
    const response = await apiClient.get('/expedientes', { params })
    return response.data.data
  },

  async getExpedienteById(id) {
    const response = await apiClient.get(`/expedientes/${id}`)
    return response.data.data
  },

  async getExpedienteByCotizacion(idCotizacion) {
    const response = await apiClient.get(`/expedientes/cotizacion/${idCotizacion}`)
    return response.data.data
  },

  async crearExpediente(data) {
    const response = await apiClient.post('/expedientes', data)
    return response.data.data
  },

  async asignarCanal(id, payload) {
    const response = await apiClient.patch(`/expedientes/${id}/canal`, payload)
    return response.data.data
  },

  async autorizarLevante(id, payload) {
    const response = await apiClient.patch(`/expedientes/${id}/levante`, payload)
    return response.data.data
  }
}

export const supervisionService = gestionAduaneraService
export default gestionAduaneraService
