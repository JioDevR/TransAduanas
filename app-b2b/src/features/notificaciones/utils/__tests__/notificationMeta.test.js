import { describe, it, expect } from 'vitest'
import {
  getNotificationMeta,
  normalizeNotificationLink,
  formatRelativeTime
} from '../notificationMeta'

describe('notificationMeta', () => {
  describe('getNotificationMeta', () => {
    it('retorna metadatos específicos para todos los tipos emitidos por el backend', () => {
      expect(getNotificationMeta('ACCESO_CREADO')).toMatchObject({
        icon: 'pi pi-key',
        color: 'accent',
        label: 'Credencial de Acceso'
      })
      expect(getNotificationMeta('COTIZACION_APROBADA')).toMatchObject({
        icon: 'pi pi-check-circle',
        color: 'success'
      })
      expect(getNotificationMeta('COTIZACION_OBSERVADA')).toMatchObject({
        icon: 'pi pi-exclamation-triangle',
        color: 'danger'
      })
      expect(getNotificationMeta('DAM')).toMatchObject({
        icon: 'pi pi-box',
        color: 'info'
      })
      expect(getNotificationMeta('TRANSPORTE')).toMatchObject({
        icon: 'pi pi-truck',
        color: 'info',
        label: 'Transporte'
      })
      expect(getNotificationMeta('PAGO')).toMatchObject({
        icon: 'pi pi-credit-card',
        color: 'warning'
      })
    })

    it('es insensible a mayúsculas/espacios y usa fallback SISTEMA para tipos desconocidos', () => {
      expect(getNotificationMeta(' transporte ')).toMatchObject({ color: 'info', label: 'Transporte' })
      expect(getNotificationMeta('DESCONOCIDO')).toMatchObject({ icon: 'pi pi-bell', color: 'neutral' })
      expect(getNotificationMeta(null)).toMatchObject({ icon: 'pi pi-bell', color: 'neutral' })
    })
  })

  describe('normalizeNotificationLink', () => {
    it('normaliza rutas heredadas /portal-b2b hacia /expedientes', () => {
      expect(normalizeNotificationLink('/portal-b2b?tab=seguimiento&tracking=TRK-2026-0001')).toBe('/expedientes')
    })

    it('anula enlaces hacia la antigua vista /notificaciones', () => {
      expect(normalizeNotificationLink('/notificaciones')).toBeNull()
      expect(normalizeNotificationLink('/notificaciones?filtro=no-leidas')).toBeNull()
    })

    it('preserva rutas válidas del portal B2B y agrega barra inicial si falta', () => {
      expect(normalizeNotificationLink('/pagos?tab=historial&subsanarPago=12')).toBe('/pagos?tab=historial&subsanarPago=12')
      expect(normalizeNotificationLink('cotizaciones')).toBe('/cotizaciones')
      expect(normalizeNotificationLink('')).toBeNull()
      expect(normalizeNotificationLink(null)).toBeNull()
    })
  })

  describe('formatRelativeTime', () => {
    const now = new Date('2026-11-20T15:00:00Z')

    it('formatea segundos, minutos, horas y días en español', () => {
      expect(formatRelativeTime('2026-11-20T14:59:30Z', now)).toBe('Ahora mismo')
      expect(formatRelativeTime('2026-11-20T14:45:00Z', now)).toBe('Hace 15 min')
      expect(formatRelativeTime('2026-11-20T12:00:00Z', now)).toBe('Hace 3 h')
      expect(formatRelativeTime('2026-11-19T10:00:00Z', now)).toBe('Ayer')
      expect(formatRelativeTime('2026-11-17T15:00:00Z', now)).toBe('Hace 3 d')
    })

    it('maneja valores nulos o inválidos de forma segura', () => {
      expect(formatRelativeTime(null, now)).toBe('—')
      expect(formatRelativeTime('fecha-invalida', now)).toBe('fecha-invalida')
    })
  })
})
