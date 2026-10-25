import apiClient from '@/core/api/apiClient'

export const portalNotificationService = {
  /**
   * Obtener lista completa de notificaciones del cliente
   */
  async getNotificaciones() {
    const response = await apiClient.get('/portal/notificaciones')
    return response.data?.data || []
  },

  /**
   * Obtener resumen con cantidad de no leídas
   */
  async getResumenNoLeidas() {
    const response = await apiClient.get('/portal/notificaciones/resumen')
    return response.data?.data || 0
  },

  /**
   * Marcar una notificación específica como leída
   */
  async marcarComoLeida(id) {
    const response = await apiClient.patch(`/portal/notificaciones/${id}/leer`)
    return response.data
  },

  /**
   * Marcar todas las notificaciones del cliente como leídas
   */
  async marcarTodasComoLeidas() {
    const response = await apiClient.patch('/portal/notificaciones/marcar-todas-leidas')
    return response.data
  }
}
