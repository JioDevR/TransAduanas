import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '../notificationStore'
import { portalNotificationService } from '../../services/portalNotificationService'

vi.mock('../../services/portalNotificationService', () => ({
  portalNotificationService: {
    getNotificaciones: vi.fn(),
    getResumenNoLeidas: vi.fn(),
    marcarComoLeida: vi.fn(),
    marcarTodasComoLeidas: vi.fn()
  }
}))

describe('notificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('carga notificaciones y sincroniza unreadCount y badgeLabel', async () => {
    portalNotificationService.getNotificaciones.mockResolvedValueOnce([
      { idNotificacion: 1, titulo: 'Aprobada', leido: false },
      { idNotificacion: 2, titulo: 'Pago', leido: true },
      { idNotificacion: 3, titulo: 'Transporte', leido: false }
    ])

    const store = useNotificationStore()
    await store.fetchNotifications()

    expect(store.hasLoaded).toBe(true)
    expect(store.items).toHaveLength(3)
    expect(store.unreadCount).toBe(2)
    expect(store.badgeLabel).toBe('2')

    store.setFilter('UNREAD')
    expect(store.filteredItems).toHaveLength(2)
  })

  it('formatea badgeLabel como 99+ cuando supera 99 no leídas', () => {
    const store = useNotificationStore()
    store.unreadCount = 120
    expect(store.badgeLabel).toBe('99+')

    store.unreadCount = 0
    expect(store.badgeLabel).toBeUndefined()
  })

  it('actualiza de forma optimista al marcar como leída y revierte si falla la API', async () => {
    portalNotificationService.getNotificaciones.mockResolvedValueOnce([
      { idNotificacion: 10, titulo: 'Aviso', leido: false }
    ])
    portalNotificationService.marcarComoLeida.mockRejectedValueOnce(new Error('Network error'))

    const store = useNotificationStore()
    await store.fetchNotifications()
    expect(store.unreadCount).toBe(1)

    const result = await store.markAsRead(10)

    expect(result.success).toBe(false)
    expect(store.items[0].leido).toBe(false)
    expect(store.unreadCount).toBe(1)
  })

  it('marca todas como leídas y revierte estados individuales si falla', async () => {
    portalNotificationService.getNotificaciones.mockResolvedValueOnce([
      { idNotificacion: 1, titulo: 'Uno', leido: false },
      { idNotificacion: 2, titulo: 'Dos', leido: true },
      { idNotificacion: 3, titulo: 'Tres', leido: false }
    ])
    portalNotificationService.marcarTodasComoLeidas.mockRejectedValueOnce({
      response: { data: { message: 'Error servidor' } }
    })

    const store = useNotificationStore()
    await store.fetchNotifications()

    const failRes = await store.markAllAsRead()
    expect(failRes.success).toBe(false)
    expect(store.unreadCount).toBe(2)
    expect(store.items.map(i => i.leido)).toEqual([false, true, false])

    portalNotificationService.marcarTodasComoLeidas.mockResolvedValueOnce({})
    const okRes = await store.markAllAsRead()
    expect(okRes.success).toBe(true)
    expect(store.unreadCount).toBe(0)
    expect(store.items.every(i => i.leido)).toBe(true)
  })

  it('fetchUnreadCount refresca la lista en segundo plano si ya estaba cargada y el servidor reporta cambios', async () => {
    portalNotificationService.getNotificaciones
      .mockResolvedValueOnce([{ idNotificacion: 1, titulo: 'Inicial', leido: false }])
      .mockResolvedValueOnce([
        { idNotificacion: 2, titulo: 'Nueva', leido: false },
        { idNotificacion: 1, titulo: 'Inicial', leido: false }
      ])
    portalNotificationService.getResumenNoLeidas.mockResolvedValueOnce(2)

    const store = useNotificationStore()
    await store.fetchNotifications()
    await store.fetchUnreadCount()

    expect(portalNotificationService.getNotificaciones).toHaveBeenCalledTimes(2)
    expect(store.items).toHaveLength(2)
    expect(store.unreadCount).toBe(2)
  })

  it('ejecuta polling periódico ligero y se detiene al hacer reset()', async () => {
    vi.useFakeTimers()
    portalNotificationService.getResumenNoLeidas.mockResolvedValue(4)

    const store = useNotificationStore()
    store.startPolling(10000)

    await vi.advanceTimersByTimeAsync(10000)
    expect(portalNotificationService.getResumenNoLeidas).toHaveBeenCalledTimes(1)

    store.reset()
    await vi.advanceTimersByTimeAsync(20000)
    expect(portalNotificationService.getResumenNoLeidas).toHaveBeenCalledTimes(1)
    expect(store.unreadCount).toBe(0)
    expect(store.items).toEqual([])
  })
})
