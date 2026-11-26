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
  },

  /**
   * Obtiene diagnóstico detallado de almacenamiento y cruce BD vs disco
   * @param {Object} params { buscar, entidad, categoria, integridad, page, size }
   */
  async getAlmacenamiento(params = {}) {
    const cleanParams = Object.fromEntries(
      Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined)
    )
    const response = await apiClient.get('/admin/sistema/almacenamiento', { params: cleanParams })
    return response.data.data
  },

  /**
   * Depura archivos físicos huérfanos en el directorio local de almacenamiento
   */
  async depurarHuerfanos() {
    const response = await apiClient.post('/admin/sistema/almacenamiento/depurar-huerfanos')
    return response.data.data
  },

  /**
   * Descarga el informe técnico PDF de estado del sistema y almacenamiento
   * @param {number} dias
   */
  async downloadPdf(dias = 7) {
    const response = await apiClient.get('/admin/sistema/exportar-pdf', {
      params: { dias },
      responseType: 'blob'
    })
    return response.data
  }
}

export default sistemaService
