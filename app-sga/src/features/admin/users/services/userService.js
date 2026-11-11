import apiClient from '@/core/api/apiClient'

export const userService = {
  /**
   * Listar todos los usuarios
   * @param {Boolean} soloActivos Filtrar solo usuarios activos
   */
  async getUsers(soloActivos = false) {
    const response = await apiClient.get('/usuarios', {
      params: { soloActivos }
    })
    return response.data.data
  },

  /**
   * Listar operadores internos activos para asignación operativa en módulos
   */
  async getOperadores() {
    const response = await apiClient.get('/usuarios/operadores')
    return response.data.data
  },

  /**
   * Alias en español para getUsers
   */
  async getUsuarios(soloActivos = false) {
    return this.getUsers(soloActivos)
  },

  /**
   * Obtener detalle de usuario por ID
   * @param {Number} id ID del usuario
   */
  async getUserById(id) {
    const response = await apiClient.get(`/usuarios/${id}`)
    return response.data.data
  },

  /**
   * Crear un nuevo usuario en el sistema
   * @param {Object} userData Payload de UsuarioCreateDTO
   */
  async createUser(userData) {
    const response = await apiClient.post('/usuarios', userData)
    return response.data.data
  },

  /**
   * Actualizar datos y/o rol de un usuario existente
   * @param {Number} id ID del usuario
   * @param {Object} userData Payload de UsuarioUpdateDTO
   */
  async updateUser(id, userData) {
    const response = await apiClient.put(`/usuarios/${id}`, userData)
    return response.data.data
  },

  /**
   * Eliminación lógica (desactivar usuario por estado)
   * @param {Number} id ID del usuario
   */
  async deleteUser(id) {
    const response = await apiClient.delete(`/usuarios/${id}`)
    return response.data
  },

  /**
   * Reactivar un usuario previamente desactivado
   * @param {Number} id ID del usuario
   */
  async reactivateUser(id) {
    const response = await apiClient.patch(`/usuarios/${id}/reactivar`)
    return response.data
  },

  /**
   * Obtener catálogo de roles disponibles
   */
  async getRoles() {
    const response = await apiClient.get('/roles')
    return response.data.data
  },

  /**
   * Obtener catálogo de sedes operativas
   */
  async getSedes() {
    const response = await apiClient.get('/sedes')
    return response.data.data
  },

  /**
   * Descargar listado de usuarios internos en PDF (plantilla HTML corporativa TI)
   * @param {Boolean} soloActivos Filtrar solo usuarios activos (default: true)
   */
  async downloadPdf(soloActivos = true) {
    const response = await apiClient.get('/usuarios/exportar-pdf', {
      params: { soloActivos },
      responseType: 'blob'
    })
    return response.data
  }
}

export default userService
