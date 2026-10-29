import apiClient from '@/core/api/apiClient'

export const portalUserService = {
  /**
   * Listar todos los usuarios del portal con filtros opcionales
   * @param {Object} params { idCliente, soloActivos, buscar }
   */
  async getPortalUsers(params = {}) {
    const response = await apiClient.get('/admin/usuarios-portal', { params })
    return response.data.data
  },

  /**
   * Obtener detalle de usuario del portal por ID
   * @param {Number} id ID del usuario cliente
   */
  async getPortalUserById(id) {
    const response = await apiClient.get(`/admin/usuarios-portal/${id}`)
    return response.data.data
  },

  /**
   * Aprovisionar nuevo usuario del portal B2B
   * @param {Object} userData Payload de UsuarioClienteCreateDTO
   */
  async createPortalUser(userData) {
    const response = await apiClient.post('/admin/usuarios-portal', userData)
    return response.data.data
  },

  /**
   * Actualizar datos y perfil de un usuario del portal
   * @param {Number} id ID del usuario cliente
   * @param {Object} userData Payload de UsuarioClienteUpdateDTO
   */
  async updatePortalUser(id, userData) {
    const response = await apiClient.put(`/admin/usuarios-portal/${id}`, userData)
    return response.data.data
  },

  /**
   * Baja lógica por estado (desactivar usuario del portal)
   * @param {Number} id ID del usuario cliente
   */
  async deletePortalUser(id) {
    const response = await apiClient.delete(`/admin/usuarios-portal/${id}`)
    return response.data
  },

  /**
   * Reactivar un usuario previamente desactivado
   * @param {Number} id ID del usuario cliente
   */
  async reactivatePortalUser(id) {
    const response = await apiClient.patch(`/admin/usuarios-portal/${id}/reactivar`)
    return response.data
  },

  /**
   * Restablecer la contraseña del usuario del portal
   * @param {Number} id ID del usuario cliente
   * @param {Object} passwordData Payload de UsuarioClienteResetPasswordDTO
   */
  async resetPassword(id, passwordData) {
    const response = await apiClient.patch(`/admin/usuarios-portal/${id}/reset-password`, passwordData)
    return response.data
  },

  /**
   * Obtener lista de clientes empresariales activos para vincular usuarios
   */
  async getClientesActivos() {
    const response = await apiClient.get('/clientes', {
      params: { soloActivos: true }
    })
    return response.data.data
  }
}

export default portalUserService
