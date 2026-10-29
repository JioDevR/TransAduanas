import { describe, it, expect } from 'vitest'
import { getCredencialesPostAprobacionNotification } from '../aprobacionAduaneraUtils'

describe('aprobacionAduaneraUtils', () => {
  it('genera notificación de nuevas credenciales cuando el cliente no tiene usuario', () => {
    const cotizacion = {
      clienteNumeroDocumento: '20601234567',
      clienteTieneUsuario: false
    }
    const notif = getCredencialesPostAprobacionNotification(cotizacion)
    expect(notif.title).toBe('Credenciales de Acceso Creadas')
    expect(notif.message).toContain('20601234567')
    expect(notif.message).toContain('Password123!')
    expect(notif.life).toBe(8000)
  })

  it('informa credenciales existentes cuando el cliente ya tiene usuario', () => {
    const cotizacion = {
      clienteNumeroDocumento: '20609876543',
      clienteTieneUsuario: true
    }
    const notif = getCredencialesPostAprobacionNotification(cotizacion)
    expect(notif.title).toBe('Acceso Portal B2B')
    expect(notif.message).toContain('20609876543')
    expect(notif.message).toContain('ya cuenta con credenciales activas')
    expect(notif.life).toBe(5000)
  })

  it('maneja cliente sin número de documento de forma segura', () => {
    const notif = getCredencialesPostAprobacionNotification({})
    expect(notif.message).toContain('RUC del cliente')
  })
})
