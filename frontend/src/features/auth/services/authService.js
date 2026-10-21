import apiClient from '@/core/api/apiClient'

export const authService = {
  /**
   * Autenticación de usuario con credenciales
   * @param {Object} credentials { username, password }
   * @returns {Promise<Object>} Datos del usuario y token JWT
   */
  async login(credentials) {
    const response = await apiClient.post('/auth/login', credentials)
    // El backend responde con formato ApiResponseDTO<AuthResponseDTO>
    return response.data.data
  },

  /**
   * Obtiene la información del usuario autenticado actual
   * @returns {Promise<Object>} Datos del perfil
   */
  async getMe() {
    const response = await apiClient.get('/auth/me')
    return response.data.data
  }
}

export default authService
