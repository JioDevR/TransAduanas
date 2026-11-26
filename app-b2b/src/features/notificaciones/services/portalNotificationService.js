import apiClient from '@/core/api/apiClient'

export const portalNotificationService = {
  /**
   * Obtener lista de notificaciones del cliente autenticado.
   * @returns {Promise<Array>}
   */
  async getNotificaciones() {
    const response = await apiClient.get('/portal/notificaciones')
    return response.data?.data || []
  },

  /**
   * Obtener cantidad de notificaciones no leídas.
   * @returns {Promise<number>}
   */
  async getResumenNoLeidas() {
    const response = await apiClient.get('/portal/notificaciones/resumen')
    return typeof response.data?.data === 'number' ? response.data.data : 0
  },

  /**
   * Marcar una notificación específica como leída.
   * @param {number|string} id
   */
  async marcarComoLeida(id) {
    const response = await apiClient.patch(`/portal/notificaciones/${id}/leer`)
    return response.data
  },

  /**
   * Marcar todas las notificaciones del cliente como leídas.
   */
  async marcarTodasComoLeidas() {
    const response = await apiClient.patch('/portal/notificaciones/marcar-todas-leidas')
    return response.data
  }
}

export default portalNotificationService
