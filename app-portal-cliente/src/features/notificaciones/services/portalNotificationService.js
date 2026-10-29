import { ref } from 'vue'
import apiClient from '@/core/api/apiClient'

export const unreadNotificationsCount = ref(0)

export const portalNotificationService = {
  unreadCount: unreadNotificationsCount,

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
    const count = typeof response.data?.data === 'number' ? response.data.data : 0
    unreadNotificationsCount.value = count
    return count
  },

  /**
   * Marcar una notificación específica como leída
   */
  async marcarComoLeida(id) {
    const response = await apiClient.patch(`/portal/notificaciones/${id}/leer`)
    if (unreadNotificationsCount.value > 0) {
      unreadNotificationsCount.value--
    }
    return response.data
  },

  /**
   * Marcar todas las notificaciones del cliente como leídas
   */
  async marcarTodasComoLeidas() {
    const response = await apiClient.patch('/portal/notificaciones/marcar-todas-leidas')
    unreadNotificationsCount.value = 0
    return response.data
  }
}

export default portalNotificationService
