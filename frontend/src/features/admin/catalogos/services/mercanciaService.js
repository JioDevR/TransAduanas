import apiClient from '@/core/api/apiClient'

export const mercanciaService = {
  /**
   * Listar mercancías con filtros opcionales
   * @param {Object} params { soloActivas, categoria, requierePermiso }
   */
  async getMercancias(params = {}) {
    const response = await apiClient.get('/catalogos/mercancias', { params })
    return response.data.data
  },

  /**
   * Buscar mercancía por ID
   * @param {Number} id ID de la mercancía
   */
  async getMercanciaById(id) {
    const response = await apiClient.get(`/catalogos/mercancias/${id}`)
    return response.data.data
  },

  /**
   * Buscar mercancía por código arancelario / técnico
   * @param {String} codigo Código de la mercancía
   */
  async getMercanciaByCodigo(codigo) {
    const response = await apiClient.get(`/catalogos/mercancias/codigo/${codigo}`)
    return response.data.data
  },

  /**
   * Registrar nueva mercancía / partida
   * @param {Object} data CatalogoMercanciaRequestDTO
   */
  async createMercancia(data) {
    const response = await apiClient.post('/catalogos/mercancias', data)
    return response.data.data
  },

  /**
   * Actualizar clasificación o arancel de mercancía
   * @param {Number} id ID de la mercancía
   * @param {Object} data CatalogoMercanciaRequestDTO
   */
  async updateMercancia(id, data) {
    const response = await apiClient.put(`/catalogos/mercancias/${id}`, data)
    return response.data.data
  },

  /**
   * Borrado lógico (estado = INACTIVO)
   * @param {Number} id ID de la mercancía
   */
  async deleteMercancia(id) {
    const response = await apiClient.delete(`/catalogos/mercancias/${id}`)
    return response.data
  },

  /**
   * Reactivar mercancía
   * @param {Number} id ID de la mercancía
   */
  async reactivarMercancia(id) {
    const response = await apiClient.patch(`/catalogos/mercancias/${id}/reactivar`)
    return response.data
  }
}

export default mercanciaService
