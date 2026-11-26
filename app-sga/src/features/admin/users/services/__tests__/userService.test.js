import { describe, it, expect, vi, beforeEach } from 'vitest'
import { userService } from '../userService'
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

describe('userService PDF report', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('downloadPdf defaults soloActivos to true and requests blob response', async () => {
    const fakeBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    apiClient.get.mockResolvedValueOnce({ data: fakeBlob })

    const result = await userService.downloadPdf()

    expect(apiClient.get).toHaveBeenCalledWith('/usuarios/exportar-pdf', {
      params: { soloActivos: true },
      responseType: 'blob'
    })
    expect(result).toBe(fakeBlob)
  })

  it('downloadPdf passes explicit soloActivos=false when requested', async () => {
    const fakeBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    apiClient.get.mockResolvedValueOnce({ data: fakeBlob })

    const result = await userService.downloadPdf(false)

    expect(apiClient.get).toHaveBeenCalledWith('/usuarios/exportar-pdf', {
      params: { soloActivos: false },
      responseType: 'blob'
    })
    expect(result).toBe(fakeBlob)
  })

  it('resetPassword sends PATCH to /usuarios/{id}/reset-password', async () => {
    apiClient.patch.mockResolvedValueOnce({ data: { success: true } })

    const res = await userService.resetPassword(15, 'ClaveTemp2026!')

    expect(apiClient.patch).toHaveBeenCalledWith('/usuarios/15/reset-password', {
      nuevaPassword: 'ClaveTemp2026!'
    })
    expect(res).toEqual({ success: true })
  })
})
