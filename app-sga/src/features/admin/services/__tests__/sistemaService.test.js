import { describe, it, expect, vi, beforeEach } from 'vitest'
import { sistemaService } from '../sistemaService'
import apiClient from '@/core/api/apiClient'

vi.mock('@/core/api/apiClient', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('sistemaService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getDashboard consulta /admin/sistema/dashboard con parámetros', async () => {
    const payload = { usuariosTotal: 10 }
    apiClient.get.mockResolvedValueOnce({ data: { data: payload } })

    const res = await sistemaService.getDashboard({ dias: 14, modulo: 'SEGURIDAD_TI' })

    expect(apiClient.get).toHaveBeenCalledWith('/admin/sistema/dashboard', {
      params: { dias: 14, modulo: 'SEGURIDAD_TI' }
    })
    expect(res).toEqual(payload)
  })

  it('getEstado consulta /admin/sistema/estado', async () => {
    const estado = { bdConectada: true }
    apiClient.get.mockResolvedValueOnce({ data: { data: estado } })

    const res = await sistemaService.getEstado()

    expect(apiClient.get).toHaveBeenCalledWith('/admin/sistema/estado')
    expect(res).toEqual(estado)
  })

  it('getRolesMatriz consulta /admin/sistema/roles-matriz', async () => {
    const matriz = [{ codigo: 'ADMINISTRADOR_TI' }]
    apiClient.get.mockResolvedValueOnce({ data: { data: matriz } })

    const res = await sistemaService.getRolesMatriz()

    expect(apiClient.get).toHaveBeenCalledWith('/admin/sistema/roles-matriz')
    expect(res).toEqual(matriz)
  })
})
