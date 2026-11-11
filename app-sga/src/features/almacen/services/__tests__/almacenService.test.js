import { describe, it, expect, vi, beforeEach } from 'vitest'
import apiClient from '@/core/api/apiClient'
import { almacenService } from '../almacenService'

vi.mock('@/core/api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn()
  }
}))

describe('almacenService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('autorizarSalida envía PATCH a /almacen/{id}/autorizar-salida', async () => {
    apiClient.patch.mockResolvedValueOnce({
      data: { data: { idControlAlmacen: 1, estadoAlmacen: 'DESPACHADO' } }
    })
    const res = await almacenService.autorizarSalida(1, { observaciones: 'Pase conforme' })
    expect(apiClient.patch).toHaveBeenCalledWith('/almacen/1/autorizar-salida', { observaciones: 'Pase conforme' })
    expect(res.estadoAlmacen).toBe('DESPACHADO')
  })

  it('no expone endpoint fantasma registrarRetiro que causa 404', () => {
    expect(almacenService.registrarRetiro).toBeUndefined()
  })
})
