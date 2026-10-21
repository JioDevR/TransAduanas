import apiClient from '@/core/api/apiClient'

export const comercialService = {
  // ==========================================
  // CLIENTES & PROSPECTOS
  // ==========================================
  async getClientes(params = {}) {
    const response = await apiClient.get('/clientes', { params })
    return response.data.data
  },

  async getClienteById(id) {
    const response = await apiClient.get(`/clientes/${id}`)
    return response.data.data
  },

  async getClienteByDocumento(numeroDocumento) {
    const response = await apiClient.get(`/clientes/documento/${numeroDocumento}`)
    return response.data.data
  },

  async createCliente(data) {
    const response = await apiClient.post('/clientes', data)
    return response.data.data
  },

  async updateCliente(id, data) {
    const response = await apiClient.put(`/clientes/${id}`, data)
    return response.data.data
  },

  async deleteCliente(id) {
    const response = await apiClient.delete(`/clientes/${id}`)
    return response.data
  },

  async reactivarCliente(id) {
    const response = await apiClient.patch(`/clientes/${id}/reactivar`)
    return response.data
  },

  async convertirACliente(id) {
    const response = await apiClient.patch(`/clientes/${id}/convertir-cliente`)
    return response.data.data
  },

  // ==========================================
  // COTIZACIONES
  // ==========================================
  async getCotizaciones(params = {}) {
    const response = await apiClient.get('/cotizaciones', { params })
    return response.data.data
  },

  async getCotizacionById(id) {
    const response = await apiClient.get(`/cotizaciones/${id}`)
    return response.data.data
  },

  async getCotizacionByCodigo(codigo) {
    const response = await apiClient.get(`/cotizaciones/codigo/${codigo}`)
    return response.data.data
  },

  async createCotizacion(data) {
    const response = await apiClient.post('/cotizaciones', data)
    return response.data.data
  },

  async updateCotizacion(id, data) {
    const response = await apiClient.put(`/cotizaciones/${id}`, data)
    return response.data.data
  },

  async revisarCotizacion(id, payload) {
    const response = await apiClient.patch(`/cotizaciones/${id}/revision`, payload)
    return response.data.data
  },

  async updateCotizacionEstado(id, payload) {
    const response = await apiClient.patch(`/cotizaciones/${id}/estado`, payload)
    return response.data.data
  },

  async deleteCotizacion(id) {
    const response = await apiClient.delete(`/cotizaciones/${id}`)
    return response.data
  },

  // ==========================================
  // PAGOS Y ANTICIPOS DE COTIZACIÓN
  // ==========================================
  async registrarPagoCotizacion(idCotizacion, data) {
    const response = await apiClient.post(`/cotizaciones/${idCotizacion}/pagos`, data)
    return response.data.data
  },

  async getPagosCotizacion(idCotizacion) {
    const response = await apiClient.get(`/cotizaciones/${idCotizacion}/pagos`)
    return response.data.data
  },

  // ==========================================
  // EXPEDIENTES ADUANEROS OPERATIVOS
  // ==========================================
  async createExpediente(data) {
    const response = await apiClient.post('/expedientes', data)
    return response.data.data
  },

  async updateExpedienteEstado(id, data) {
    const response = await apiClient.put(`/expedientes/${id}`, data)
    return response.data.data
  },

  async deleteExpediente(id) {
    const response = await apiClient.delete(`/expedientes/${id}`)
    return response.data
  },
}

export const ventasService = comercialService
export default comercialService
