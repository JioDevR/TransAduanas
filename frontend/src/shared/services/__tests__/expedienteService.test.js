import { describe, it, expect, vi, beforeEach } from 'vitest'
import { expedienteService } from '../expedienteService'
import apiClient from '@/core/api/apiClient'
import { expedienteService as aduanasService } from '@/features/aduanas/services/expedienteService'
import { expedienteService as almacenService } from '@/features/almacen/services/expedienteService'
import { expedienteService as comercialService } from '@/features/comercial/services/expedienteService'
import { expedienteService as finanzasService } from '@/features/finanzas/services/expedienteService'
import { expedienteService as transporteService } from '@/features/transporte/services/expedienteService'

vi.mock('@/core/api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn()
  }
}))

describe('expedienteService canonical & re-exports', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('re-exports reference the exact same instance across all 5 features', () => {
    expect(aduanasService).toBe(expedienteService)
    expect(almacenService).toBe(expedienteService)
    expect(comercialService).toBe(expedienteService)
    expect(finanzasService).toBe(expedienteService)
    expect(transporteService).toBe(expedienteService)
  })

  it('getExpedientes delegates to GET /expedientes with params', async () => {
    const mockData = [{ idExpediente: 1 }]
    apiClient.get.mockResolvedValueOnce({ data: { data: mockData } })

    const result = await expedienteService.getExpedientes({ estado: 'EN_TRAMITE' })
    expect(apiClient.get).toHaveBeenCalledWith('/expedientes', { params: { estado: 'EN_TRAMITE' } })
    expect(result).toEqual(mockData)
  })

  it('createExpediente delegates to POST /expedientes with payload', async () => {
    const payload = { idCotizacion: 123 }
    const created = { idExpediente: 99, ...payload }
    apiClient.post.mockResolvedValueOnce({ data: { data: created } })

    const result = await expedienteService.createExpediente(payload)
    expect(apiClient.post).toHaveBeenCalledWith('/expedientes', payload)
    expect(result).toEqual(created)
  })

  it('updateExpediente delegates to PUT /expedientes/:id with payload', async () => {
    const payload = { numeroContenedor: 'MSCU1234567' }
    apiClient.put.mockResolvedValueOnce({ data: { data: { id: 5, ...payload } } })

    const result = await expedienteService.updateExpediente(5, payload)
    expect(apiClient.put).toHaveBeenCalledWith('/expedientes/5', payload)
    expect(result).toEqual({ id: 5, ...payload })
  })

  it('cerrarExpediente delegates to PATCH /expedientes/:id/cerrar', async () => {
    const payload = { observacionesCierre: 'OK' }
    apiClient.patch.mockResolvedValueOnce({ data: { data: { id: 10, estado: 'CERRADO' } } })

    const result = await expedienteService.cerrarExpediente(10, payload)
    expect(apiClient.patch).toHaveBeenCalledWith('/expedientes/10/cerrar', payload)
    expect(result).toEqual({ id: 10, estado: 'CERRADO' })
  })
})
