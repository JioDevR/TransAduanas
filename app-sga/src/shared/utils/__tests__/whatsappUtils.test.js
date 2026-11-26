import { describe, it, expect, vi } from 'vitest'
import {
  formatPeruWhatsAppPhone,
  buildCotizacionWhatsAppMessage,
  openCotizacionWhatsApp
} from '../whatsappUtils'

describe('whatsappUtils', () => {
  it('normaliza teléfonos de Perú anteponiendo 51 cuando falta', () => {
    expect(formatPeruWhatsAppPhone('987-654-321')).toBe('51987654321')
    expect(formatPeruWhatsAppPhone('+51 987 654 321')).toBe('51987654321')
    expect(formatPeruWhatsAppPhone('3434534534')).toBe('513434534534')
    expect(formatPeruWhatsAppPhone('')).toBe('')
  })

  it('genera mensaje resumido con credenciales temporales cuando es primer acceso al portal', () => {
    const msg = buildCotizacionWhatsAppMessage(
      {
        codigoCotizacion: 'COT-2026-0005',
        clienteRazonSocial: 'VOLCANIC MOTORS',
        clienteNumeroDocumento: '2039398389',
        estado: 'APROBADA',
        totalCotizado: 15106.09,
        clienteTieneUsuario: true,
        clienteDebeCambiarPassword: true
      },
      { portalUrl: 'http://localhost:5174' }
    )

    expect(msg).toContain('COT-2026-0005')
    expect(msg).toContain('Aprobada')
    expect(msg).toContain('$15,106.09 USD')
    expect(msg).toContain('Usuario: *2039398389*')
    expect(msg).toContain('Clave temporal: *Password123!*')
    expect(msg).toContain('http://localhost:5174')
  })

  it('omite credenciales temporales si el cliente ya regularizó su contraseña', () => {
    const msg = buildCotizacionWhatsAppMessage({
      codigoCotizacion: 'COT-2026-0001',
      clienteRazonSocial: 'Camposol S.A.',
      clienteNumeroDocumento: '20370146994',
      estado: 'APROBADA',
      totalCotizado: 5000,
      clienteTieneUsuario: true,
      clienteDebeCambiarPassword: false
    })

    expect(msg).toContain('Aprobada')
    expect(msg).not.toContain('Password123!')
    expect(msg).toContain('Portal B2B')
  })

  it('incluye observación en cotizaciones observadas y abre enlace wa.me', () => {
    const openSpy = vi.fn()
    vi.stubGlobal('window', { open: openSpy })

    try {
      const res = openCotizacionWhatsApp({
        codigoCotizacion: 'COT-2026-0099',
        clienteRazonSocial: 'Andina SAC',
        clienteTelefono: '999888777',
        estado: 'OBSERVADA',
        totalCotizado: 2500,
        motivoObservacion: 'Adjuntar packing list firmado.'
      })

      expect(res.ok).toBe(true)
      expect(res.phone).toBe('51999888777')
      expect(res.message).toContain('Adjuntar packing list firmado.')
      expect(openSpy).toHaveBeenCalledTimes(1)
      expect(openSpy.mock.calls[0][0]).toContain('https://wa.me/51999888777?text=')
    } finally {
      vi.unstubAllGlobals()
    }
  })

  it('retorna error legible si el cliente no tiene teléfono registrado', () => {
    const res = openCotizacionWhatsApp({ codigoCotizacion: 'COT-1', estado: 'PENDIENTE' })
    expect(res.ok).toBe(false)
    expect(res.reason).toContain('teléfono')
  })
})
