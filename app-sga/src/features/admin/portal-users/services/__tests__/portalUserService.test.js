import { describe, it, expect, vi, beforeEach } from 'vitest'
import { portalUserService } from '../portalUserService'
import apiClient from '@/core/api/apiClient'

vi.mock('@/core/api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn()
  }
}))

describe('portalUserService PDF report', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('downloadPdf defaults soloActivos to true and requests blob response', async () => {
    const fakeBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    apiClient.get.mockResolvedValueOnce({ data: fakeBlob })

    const result = await portalUserService.downloadPdf()

    expect(apiClient.get).toHaveBeenCalledWith('/admin/usuarios-portal/exportar-pdf', {
      params: { soloActivos: true },
      responseType: 'blob'
    })
    expect(result).toBe(fakeBlob)
  })

  it('downloadPdf passes explicit soloActivos=false when requested', async () => {
    const fakeBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    apiClient.get.mockResolvedValueOnce({ data: fakeBlob })

    const result = await portalUserService.downloadPdf(false)

    expect(apiClient.get).toHaveBeenCalledWith('/admin/usuarios-portal/exportar-pdf', {
      params: { soloActivos: false },
      responseType: 'blob'
    })
    expect(result).toBe(fakeBlob)
  })
})
