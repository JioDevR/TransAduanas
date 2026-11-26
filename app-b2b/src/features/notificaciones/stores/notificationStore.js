import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { portalNotificationService } from '../services/portalNotificationService'

export const DEFAULT_POLLING_INTERVAL_MS = 60000

/**
 * Store Pinia para el centro de notificaciones desplegable del Portal B2B.
 */
export const useNotificationStore = defineStore('portalNotifications', () => {
  const items = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const loadingAction = ref(false)
  const error = ref(null)
  const hasLoaded = ref(false)
  const activeFilter = ref('ALL')

  let pollTimerId = null
  let visibilityHandler = null
  let focusHandler = null
  let activeFetchPromise = null
  let lastResumenAt = 0

  const filteredItems = computed(() => {
    if (activeFilter.value === 'UNREAD') {
      return items.value.filter(item => !item.leido)
    }
    return items.value
  })

  const badgeLabel = computed(() => {
    if (unreadCount.value <= 0) return undefined
    return unreadCount.value > 99 ? '99+' : String(unreadCount.value)
  })

  function setFilter(filter) {
    activeFilter.value = filter === 'UNREAD' ? 'UNREAD' : 'ALL'
  }

  /**
   * Consulta ligera del contador de notificaciones no leídas.
   * Si el listado ya fue cargado y el conteo cambió en el servidor, refresca la lista en segundo plano.
   */
  async function fetchUnreadCount({ force = true } = {}) {
    const now = Date.now()
    if (!force && now - lastResumenAt < 3000) {
      return unreadCount.value
    }
    lastResumenAt = now

    try {
      const serverCount = await portalNotificationService.getResumenNoLeidas()
      const localUnread = items.value.filter(item => !item.leido).length
      unreadCount.value = serverCount

      if (hasLoaded.value && serverCount !== localUnread) {
        await fetchNotifications({ silent: true })
      }
      return serverCount
    } catch (err) {
      console.debug('Error al obtener resumen de notificaciones:', err)
      return unreadCount.value
    }
  }

  /**
   * Carga las notificaciones del cliente autenticado.
   * Deduplica solicitudes concurrentes.
   * @param {{ silent?: boolean }} [options]
   */
  async function fetchNotifications({ silent = false } = {}) {
    if (activeFetchPromise) return activeFetchPromise

    if (!silent) {
      loading.value = true
    }
    error.value = null

    activeFetchPromise = (async () => {
      try {
        const data = await portalNotificationService.getNotificaciones()
        items.value = Array.isArray(data) ? data : []
        hasLoaded.value = true
        unreadCount.value = items.value.filter(item => !item.leido).length
        return items.value
      } catch (err) {
        if (!silent) {
          error.value = err.response?.data?.message || 'No se pudieron cargar las notificaciones.'
        }
        throw err
      } finally {
        loading.value = false
        activeFetchPromise = null
      }
    })()

    return activeFetchPromise
  }

  /**
   * Marca una notificación como leída con actualización optimista y rollback ante fallo.
   * @param {number|string} idNotificacion
   */
  async function markAsRead(idNotificacion) {
    const target = items.value.find(item => item.idNotificacion === idNotificacion)
    if (target && target.leido) {
      return { success: true }
    }

    const previousCount = unreadCount.value
    if (target) {
      target.leido = true
    }
    if (unreadCount.value > 0) {
      unreadCount.value -= 1
    }

    try {
      await portalNotificationService.marcarComoLeida(idNotificacion)
      return { success: true }
    } catch (err) {
      if (target) {
        target.leido = false
      }
      unreadCount.value = previousCount
      return {
        success: false,
        message: err.response?.data?.message || 'No se pudo marcar la notificación como leída.'
      }
    }
  }

  /**
   * Marca todas las notificaciones como leídas con actualización optimista y rollback ante fallo.
   */
  async function markAllAsRead() {
    if (unreadCount.value === 0 && items.value.every(item => item.leido)) {
      return { success: true }
    }

    const previousStates = items.value.map(item => ({
      idNotificacion: item.idNotificacion,
      leido: item.leido
    }))
    const previousCount = unreadCount.value

    loadingAction.value = true
    items.value.forEach(item => {
      item.leido = true
    })
    unreadCount.value = 0

    try {
      await portalNotificationService.marcarTodasComoLeidas()
      return { success: true }
    } catch (err) {
      const stateById = new Map(previousStates.map(s => [s.idNotificacion, s.leido]))
      items.value.forEach(item => {
        if (stateById.has(item.idNotificacion)) {
          item.leido = stateById.get(item.idNotificacion)
        }
      })
      unreadCount.value = previousCount
      return {
        success: false,
        message: err.response?.data?.message || 'No se pudieron marcar todas las notificaciones como leídas.'
      }
    } finally {
      loadingAction.value = false
    }
  }

  /**
   * Inicia la actualización ligera en tiempo real (polling periódico + foco/visibilidad de pestaña).
   * @param {number} [intervalMs]
   */
  function startPolling(intervalMs = DEFAULT_POLLING_INTERVAL_MS) {
    stopPolling()

    pollTimerId = setInterval(() => {
      if (typeof document !== 'undefined' && document.hidden) return
      fetchUnreadCount()
    }, intervalMs)

    if (typeof document !== 'undefined') {
      visibilityHandler = () => {
        if (!document.hidden) {
          fetchUnreadCount({ force: false })
        }
      }
      document.addEventListener('visibilitychange', visibilityHandler)
    }

    if (typeof window !== 'undefined') {
      focusHandler = () => {
        if (typeof document === 'undefined' || !document.hidden) {
          fetchUnreadCount({ force: false })
        }
      }
      window.addEventListener('focus', focusHandler)
    }
  }

  function stopPolling() {
    if (pollTimerId !== null) {
      clearInterval(pollTimerId)
      pollTimerId = null
    }
    if (typeof document !== 'undefined' && visibilityHandler) {
      document.removeEventListener('visibilitychange', visibilityHandler)
      visibilityHandler = null
    }
    if (typeof window !== 'undefined' && focusHandler) {
      window.removeEventListener('focus', focusHandler)
      focusHandler = null
    }
  }

  function reset() {
    stopPolling()
    items.value = []
    unreadCount.value = 0
    loading.value = false
    loadingAction.value = false
    error.value = null
    hasLoaded.value = false
    activeFilter.value = 'ALL'
    activeFetchPromise = null
    lastResumenAt = 0
  }

  return {
    items,
    unreadCount,
    loading,
    loadingAction,
    error,
    hasLoaded,
    activeFilter,
    filteredItems,
    badgeLabel,
    setFilter,
    fetchUnreadCount,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    startPolling,
    stopPolling,
    reset
  }
})

export default useNotificationStore
