import { describe, it, expect, vi, beforeEach } from 'vitest'
import { auditoriaService } from '../auditoriaService'
import apiClient from '@/core/api/apiClient'

vi.mock('@/core/api/apiClient', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('auditoriaService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('listarEventos limpia parámetros vacíos y llama a /admin/auditoria', async () => {
    const data = { items: [], totalElements: 0 }
    apiClient.get.mockResolvedValueOnce({ data: { data } })

    const res = await auditoriaService.listarEventos({
      buscar: 'admin',
      modulo: '',
      resultado: null,
      page: 0,
      size: 20
    })

    expect(apiClient.get).toHaveBeenCalledWith('/admin/auditoria', {
      params: { buscar: 'admin', page: 0, size: 20 }
    })
    expect(res).toEqual(data)
  })
})
