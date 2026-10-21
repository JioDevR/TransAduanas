import apiClient from '@/core/api/apiClient'

export const almacenService = {
  /**
   * Listar inventario en almacén y depósito temporal
   * @param {Object} params { soloActivos, estado, idExpediente }
   */
  async getAlmacen(params = {}) {
    const response = await apiClient.get('/almacen', { params })
    return response.data.data
  },

  /**
   * Buscar registro de almacén por ID
   * @param {Number} id ID del registro
   */
  async getAlmacenById(id) {
    const response = await apiClient.get(`/almacen/${id}`)
    return response.data.data
  },

  /**
   * Consultar almacén vinculado a un expediente aduanero
   * @param {Number} idExpediente ID del expediente
   */
  async getAlmacenByExpediente(idExpediente) {
    const response = await apiClient.get(`/almacen/expediente/${idExpediente}`)
    return response.data.data
  },

  /**
   * Registrar nuevo ingreso de contenedor / balanza VGM
   * @param {Object} data ControlAlmacenRequestDTO
   */
  async createIngresoAlmacen(data) {
    const response = await apiClient.post('/almacen', data)
    return response.data.data
  },

  /**
   * Actualizar datos de ubicación o bultos en almacén
   * @param {Number} id ID del registro
   * @param {Object} data ControlAlmacenRequestDTO
   */
  async updateAlmacen(id, data) {
    const response = await apiClient.put(`/almacen/${id}`, data)
    return response.data.data
  },

  /**
   * Autorizar salida de depósito temporal tras verificación de levante y pagos
   * @param {Number} id ID del registro
   * @param {Object} payload ControlAlmacenSalidaDTO { observaciones }
   */
  async autorizarSalida(id, payload = {}) {
    const response = await apiClient.patch(`/almacen/${id}/autorizar-salida`, payload)
    return response.data.data
  },

  /**
   * Anulación lógica del registro de almacén
   * @param {Number} id ID del registro
   */
  async deleteAlmacen(id) {
    const response = await apiClient.delete(`/almacen/${id}`)
    return response.data
  }
}

export default almacenService
