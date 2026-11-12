import { describe, it, expect, beforeEach, vi } from 'vitest'
import portalCotizacionService from '../portalCotizacionService'
import apiClient from '@/core/api/apiClient'

vi.mock('@/core/api/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn()
  }
}))

describe('portalCotizacionService', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  describe('solicitarCotizacion', () => {
    it('debe enviar POST /portal/cotizaciones/solicitar con flete estimado, validez, transporte y ubigeo', async () => {
      const payload = {
        idMercancia: 1,
        idTipoCarga: 2,
        puertoOrigen: 'CALLAO',
        puertoDestino: 'VALPARAISO',
        valorCifUSD: 25000,
        pesoKg: 1200,
        montoFleteEstimado: 1500,
        validezDias: 15,
        servicioTransporte: true,
        ubigeoOrigen: '150101',
        ubigeoDestino: '070101'
      }

      const mockResponseData = {
        idCotizacion: 42,
        codigoCotizacion: 'COT-2026-00042',
        ...payload
      }

      apiClient.post.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockResponseData
        }
      })

      const result = await portalCotizacionService.solicitarCotizacion(payload)

      expect(apiClient.post).toHaveBeenCalledTimes(1)
      expect(apiClient.post).toHaveBeenCalledWith('/portal/cotizaciones/solicitar', payload)
      expect(result).toEqual(mockResponseData)
    })
  })

  describe('subirDocumentosCotizacion', () => {
    it('debe enviar cada documento a POST /documentos/upload con FormData correcto y header multipart', async () => {
      const file1 = new File(['content1'], 'packing-list.pdf', { type: 'application/pdf' })
      const file2 = new File(['content2'], 'factura-comercial.pdf', { type: 'application/pdf' })

      const queuedDocs = [
        { file: file1, categoria: 'PACKING_LIST', descripcion: 'Lista de empaque' },
        { file: file2, categoria: 'FACTURA', descripcion: 'Factura proforma' }
      ]

      const doc1Response = { idDocumento: 101, nombreOriginal: 'packing-list.pdf' }
      const doc2Response = { idDocumento: 102, nombreOriginal: 'factura-comercial.pdf' }

      apiClient.post
        .mockResolvedValueOnce({ data: { success: true, data: [doc1Response] } })
        .mockResolvedValueOnce({ data: { success: true, data: [doc2Response] } })

      const result = await portalCotizacionService.subirDocumentosCotizacion(42, queuedDocs)

      expect(apiClient.post).toHaveBeenCalledTimes(2)

      // Verificación de la primera llamada
      const firstCallArgs = apiClient.post.mock.calls[0]
      expect(firstCallArgs[0]).toBe('/documentos/upload')
      const firstFormData = firstCallArgs[1]
      expect(firstFormData).toBeInstanceOf(FormData)
      expect(firstFormData.get('files')).toEqual(file1)
      expect(firstFormData.get('categoria')).toBe('PACKING_LIST')
      expect(firstFormData.get('descripcion')).toBe('Lista de empaque')
      expect(firstFormData.get('entidadTipo')).toBe('COTIZACION')
      expect(firstFormData.get('idEntidad')).toBe('42')
      expect(firstCallArgs[2]).toEqual({
        headers: { 'Content-Type': 'multipart/form-data' }
      })

      // Verificación de la segunda llamada
      const secondCallArgs = apiClient.post.mock.calls[1]
      expect(secondCallArgs[0]).toBe('/documentos/upload')
      const secondFormData = secondCallArgs[1]
      expect(secondFormData).toBeInstanceOf(FormData)
      expect(secondFormData.get('files')).toEqual(file2)
      expect(secondFormData.get('categoria')).toBe('FACTURA')
      expect(secondFormData.get('descripcion')).toBe('Factura proforma')
      expect(secondFormData.get('entidadTipo')).toBe('COTIZACION')
      expect(secondFormData.get('idEntidad')).toBe('42')
      expect(secondCallArgs[2]).toEqual({
        headers: { 'Content-Type': 'multipart/form-data' }
      })

      // Resultado consolidado
      expect(result).toEqual({
        uploaded: [doc1Response, doc2Response],
        failed: []
      })
    })

    it('debe manejar fallos parciales sin abortar el resto de documentos', async () => {
      const file1 = new File(['content1'], 'bueno.pdf', { type: 'application/pdf' })
      const file2 = new File(['content2'], 'malo.pdf', { type: 'application/pdf' })
      const file3 = new File(['content3'], 'bueno2.pdf', { type: 'application/pdf' })

      const queuedDocs = [
        { file: file1, categoria: 'GENERAL', descripcion: 'Doc 1' },
        { file: file2, categoria: 'GENERAL', descripcion: 'Doc Fallido' },
        { file: file3, categoria: 'GENERAL', descripcion: 'Doc 3' }
      ]

      const doc1Response = { idDocumento: 201, nombreOriginal: 'bueno.pdf' }
      const doc3Response = { idDocumento: 203, nombreOriginal: 'bueno2.pdf' }

      apiClient.post
        .mockResolvedValueOnce({ data: { success: true, data: [doc1Response] } })
        .mockRejectedValueOnce({
          response: { data: { message: 'Formato no permitido o archivo dañado' } }
        })
        .mockResolvedValueOnce({ data: { success: true, data: [doc3Response] } })

      const result = await portalCotizacionService.subirDocumentosCotizacion('42', queuedDocs)

      expect(apiClient.post).toHaveBeenCalledTimes(3)
      expect(result.uploaded).toEqual([doc1Response, doc3Response])
      expect(result.failed).toHaveLength(1)
      expect(result.failed[0]).toEqual({
        name: 'malo.pdf',
        error: 'Formato no permitido o archivo dañado'
      })
    })

    it('debe retornar arrays vacíos si queuedDocs está vacío o es null', async () => {
      const res1 = await portalCotizacionService.subirDocumentosCotizacion(42, [])
      expect(res1).toEqual({ uploaded: [], failed: [] })
      expect(apiClient.post).not.toHaveBeenCalled()

      const res2 = await portalCotizacionService.subirDocumentosCotizacion(42, null)
      expect(res2).toEqual({ uploaded: [], failed: [] })
      expect(apiClient.post).not.toHaveBeenCalled()
    })
  })

  describe('listarDocumentosCotizacion', () => {
    it('debe llamar GET /documentos con entidadTipo COTIZACION e idCotizacion', async () => {
      const mockDocs = [
        { idDocumento: 1, nombreOriginal: 'guia.pdf', categoria: 'GUIA' },
        { idDocumento: 2, nombreOriginal: 'factura.pdf', categoria: 'FACTURA' }
      ]

      apiClient.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockDocs
        }
      })

      const result = await portalCotizacionService.listarDocumentosCotizacion(42)

      expect(apiClient.get).toHaveBeenCalledTimes(1)
      expect(apiClient.get).toHaveBeenCalledWith('/documentos', {
        params: {
          entidadTipo: 'COTIZACION',
          idCotizacion: 42
        }
      })
      expect(result).toEqual(mockDocs)
    })

    it('debe retornar un array vacío si la respuesta no contiene data', async () => {
      apiClient.get.mockResolvedValueOnce({ data: null })
      const result = await portalCotizacionService.listarDocumentosCotizacion(42)
      expect(result).toEqual([])
    })
  })

  describe('descargarDocumento', () => {
    it('debe llamar GET /documentos/{id}/descargar con responseType blob', async () => {
      const mockBlob = new Blob(['archivo binario'], { type: 'application/pdf' })

      apiClient.get.mockResolvedValueOnce({
        data: mockBlob
      })

      const result = await portalCotizacionService.descargarDocumento(15)

      expect(apiClient.get).toHaveBeenCalledTimes(1)
      expect(apiClient.get).toHaveBeenCalledWith('/documentos/15/descargar', {
        responseType: 'blob'
      })
      expect(result).toBe(mockBlob)
    })
  })

  describe('previsualizarDocumento', () => {
    it('debe llamar GET /documentos/{id}/preview con responseType blob', async () => {
      const mockBlob = new Blob(['preview binario'], { type: 'image/png' })

      apiClient.get.mockResolvedValueOnce({
        data: mockBlob
      })

      const result = await portalCotizacionService.previsualizarDocumento(18)

      expect(apiClient.get).toHaveBeenCalledTimes(1)
      expect(apiClient.get).toHaveBeenCalledWith('/documentos/18/preview', {
        responseType: 'blob'
      })
      expect(result).toBe(mockBlob)
    })
  })

  describe('Métodos existentes de portalCotizacionService', () => {
    it('listarCotizaciones debe llamar GET /portal/cotizaciones', async () => {
      const mockList = [{ idCotizacion: 1 }, { idCotizacion: 2 }]
      apiClient.get.mockResolvedValueOnce({ data: { data: mockList } })

      const res = await portalCotizacionService.listarCotizaciones()
      expect(apiClient.get).toHaveBeenCalledWith('/portal/cotizaciones')
      expect(res).toEqual(mockList)
    })

    it('obtenerDetalle debe llamar GET /portal/cotizaciones/{id}', async () => {
      const mockDetalle = { idCotizacion: 5, codigo: 'COT-005' }
      apiClient.get.mockResolvedValueOnce({ data: { data: mockDetalle } })

      const res = await portalCotizacionService.obtenerDetalle(5)
      expect(apiClient.get).toHaveBeenCalledWith('/portal/cotizaciones/5')
      expect(res).toEqual(mockDetalle)
    })

    it('aceptarCotizacion debe llamar PATCH y soportar fallback a POST si es 405', async () => {
      apiClient.patch.mockRejectedValueOnce({ response: { status: 405 } })
      apiClient.post.mockResolvedValueOnce({ data: { data: { idCotizacion: 7, estado: 'ACEPTADA' } } })

      const res = await portalCotizacionService.aceptarCotizacion(7)
      expect(apiClient.patch).toHaveBeenCalledWith('/portal/cotizaciones/7/aceptar')
      expect(apiClient.post).toHaveBeenCalledWith('/portal/cotizaciones/7/aceptar')
      expect(res).toEqual({ idCotizacion: 7, estado: 'ACEPTADA' })
    })

    it('obtenerPuertos debe llamar GET /catalogos/puertos con soloActivos', async () => {
      apiClient.get.mockResolvedValueOnce({ data: { data: [{ idPuerto: 1 }] } })
      const res = await portalCotizacionService.obtenerPuertos(true)
      expect(apiClient.get).toHaveBeenCalledWith('/catalogos/puertos', { params: { soloActivos: true } })
      expect(res).toEqual([{ idPuerto: 1 }])
    })

    it('obtenerMercancias debe llamar GET /catalogos/mercancias con soloActivos', async () => {
      apiClient.get.mockResolvedValueOnce({ data: { data: [{ idMercancia: 1 }] } })
      const res = await portalCotizacionService.obtenerMercancias(false)
      expect(apiClient.get).toHaveBeenCalledWith('/catalogos/mercancias', { params: { soloActivos: false } })
      expect(res).toEqual([{ idMercancia: 1 }])
    })

    it('obtenerTiposCarga debe llamar GET /catalogos/tipos-carga con soloActivos', async () => {
      apiClient.get.mockResolvedValueOnce({ data: { data: [{ idTipoCarga: 1 }] } })
      const res = await portalCotizacionService.obtenerTiposCarga(true)
      expect(apiClient.get).toHaveBeenCalledWith('/catalogos/tipos-carga', { params: { soloActivos: true } })
      expect(res).toEqual([{ idTipoCarga: 1 }])
    })
  })
})
