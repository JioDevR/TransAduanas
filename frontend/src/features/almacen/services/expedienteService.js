import apiClient from '@/core/api/apiClient'

export const expedienteService = {
  /**
   * Listar todos los expedientes aduaneros con filtros
   * @param {Object} params { soloActivos, canal, estado, idAgente, idCliente }
   */
  async getExpedientes(params = {}) {
    const response = await apiClient.get('/expedientes', { params })
    return response.data.data
  },

  /**
   * Obtener detalle completo de un expediente por ID
   * @param {Number} id ID del expediente
   */
  async getExpedienteById(id) {
    const response = await apiClient.get(`/expedientes/${id}`)
    return response.data.data
  },

  /**
   * Buscar expediente por código correlativo (ej. EXP-2026-0001)
   * @param {String} codigo Código de expediente
   */
  async getExpedienteByCodigo(codigo) {
    const response = await apiClient.get(`/expedientes/codigo/${codigo}`)
    return response.data.data
  },

  /**
   * Buscar expediente vinculado a una cotización comercial
   * @param {Number} idCotizacion ID de la cotización
   */
  async getExpedienteByCotizacion(idCotizacion) {
    const response = await apiClient.get(`/expedientes/cotizacion/${idCotizacion}`)
    return response.data.data
  },

  /**
   * Apertura formal de expediente aduanero a partir de cotización aprobada
   * @param {Object} data ExpedienteCrearDTO
   */
  async createExpediente(data) {
    const response = await apiClient.post('/expedientes', data)
    return response.data.data
  },

  /**
   * Actualizar datos operativos del expediente (fechas ETD/ETA, contenedor, precinto)
   * @param {Number} id ID del expediente
   * @param {Object} data ExpedienteActualizarDTO
   */
  async updateExpediente(id, data) {
    const response = await apiClient.put(`/expedientes/${id}`, data)
    return response.data.data
  },

  /**
   * Registrar asignación de Canal SUNAT (VERDE, NARANJA, ROJO) y número de DAM
   * @param {Number} id ID del expediente
   * @param {Object} payload ExpedienteCanalDTO { canalSunat, numeroDam }
   */
  async asignarCanal(id, payload) {
    const response = await apiClient.patch(`/expedientes/${id}/canal`, payload)
    return response.data.data
  },

  /**
   * Autorizar levante aduanero tras inspección o canal verde
   * @param {Number} id ID del expediente
   * @param {Object} payload ExpedienteLevanteDTO { detalleInspeccion }
   */
  async autorizarLevante(id, payload) {
    const response = await apiClient.patch(`/expedientes/${id}/levante`, payload)
    return response.data.data
  },

  /**
   * Anulación lógica de expediente aduanero
   * @param {Number} id ID del expediente
   */
  async deleteExpediente(id) {
    const response = await apiClient.delete(`/expedientes/${id}`)
    return response.data
  }
}

export default expedienteService
