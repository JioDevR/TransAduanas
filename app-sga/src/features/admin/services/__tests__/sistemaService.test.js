import { describe, it, expect, vi, beforeEach } from 'vitest'
import { sistemaService } from '../sistemaService'
import apiClient from '@/core/api/apiClient'

vi.mock('@/core/api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
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

  it('getAlmacenamiento limpia parámetros vacíos y consulta /admin/sistema/almacenamiento', async () => {
    const storage = { disponible: true, items: [] }
    apiClient.get.mockResolvedValueOnce({ data: { data: storage } })

    const res = await sistemaService.getAlmacenamiento({
      buscar: 'factura',
      entidad: '',
      integridad: 'SINCRONIZADO',
      page: 0,
      size: 20
    })

    expect(apiClient.get).toHaveBeenCalledWith('/admin/sistema/almacenamiento', {
      params: { buscar: 'factura', integridad: 'SINCRONIZADO', page: 0, size: 20 }
    })
    expect(res).toEqual(storage)
  })

  it('depurarHuerfanos invoca POST /admin/sistema/almacenamiento/depurar-huerfanos', async () => {
    const payload = { archivosEliminados: 2, bytesLiberados: 2048 }
    apiClient.post.mockResolvedValueOnce({ data: { data: payload } })

    const res = await sistemaService.depurarHuerfanos()

    expect(apiClient.post).toHaveBeenCalledWith('/admin/sistema/almacenamiento/depurar-huerfanos')
    expect(res).toEqual(payload)
  })
})
