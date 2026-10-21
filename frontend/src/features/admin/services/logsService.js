import apiClient from '@/core/api/apiClient'

export const logsService = {
  /**
   * Consulta líneas recientes del archivo de log
   * @param {Object} params { tipo: 'all' | 'error', lines: number }
   */
  async getLogs(params = {}) {
    const response = await apiClient.get('/admin/logs', { params })
    return response.data.data
  },

  /**
   * Descarga el archivo de log completo en formato .log
   * @param {string} tipo 'all' | 'error'
   */
  async downloadLog(tipo = 'all') {
    const response = await apiClient.get('/admin/logs/download', {
      params: { tipo },
      responseType: 'blob'
    })
    const filename = tipo === 'error' ? 'sga-backend-error.log' : 'sga-backend.log'
    const blob = new Blob([response.data], { type: 'text/plain' })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(link.href)
  },

  /**
   * Vacía el archivo de log en el servidor
   * @param {string} tipo 'all' | 'error'
   */
  async clearLog(tipo = 'all') {
    const response = await apiClient.post('/admin/logs/clear', null, {
      params: { tipo }
    })
    return response.data
  }
}

export default logsService
