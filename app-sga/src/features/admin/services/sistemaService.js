import apiClient from '@/core/api/apiClient'

export const sistemaService = {
  /**
   * Obtiene métricas agregadas para el Dashboard TI
   * @param {Object} params { dias: number, modulo: string }
   */
  async getDashboard(params = {}) {
    const response = await apiClient.get('/admin/sistema/dashboard', { params })
    return response.data.data
  },

  /**
   * Obtiene el estado técnico real (BD, JVM, Storage)
   */
  async getEstado() {
    const response = await apiClient.get('/admin/sistema/estado')
    return response.data.data
  },

  /**
   * Obtiene la matriz de roles, permisos y conteo de operadores
   */
  async getRolesMatriz() {
    const response = await apiClient.get('/admin/sistema/roles-matriz')
    return response.data.data
  }
}

export default sistemaService
