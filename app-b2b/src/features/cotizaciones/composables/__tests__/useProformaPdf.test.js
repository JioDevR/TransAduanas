import { describe, it, expect, vi, beforeEach } from 'vitest'
import { portalCotizacionService } from '../../services/portalCotizacionService'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'
import { useProformaPdf } from '../useProformaPdf'

const mockShowError = vi.fn()
const mockShowInfo = vi.fn()

vi.mock('../../services/portalCotizacionService', () => ({
  portalCotizacionService: {
    descargarProformaPdf: vi.fn()
  }
}))

vi.mock('@/shared/utils/download', () => ({
  downloadBlob: vi.fn(),
  openBlobInNewTab: vi.fn(() => true)
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showError: mockShowError,
    showInfo: mockShowInfo
  })
}))

describe('useProformaPdf', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    openBlobInNewTab.mockReturnValue(true)
  })

  it('abrirProformaPdf solicita inline y abre en nueva pestaña', async () => {
    const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    portalCotizacionService.descargarProformaPdf.mockResolvedValueOnce(pdfBlob)

    const { abrirProformaPdf, pdfAction } = useProformaPdf()
    await abrirProformaPdf({ idCotizacion: 42, codigoCotizacion: 'COT-2026-0042' })

    expect(portalCotizacionService.descargarProformaPdf).toHaveBeenCalledWith(42, { download: false })
    expect(openBlobInNewTab).toHaveBeenCalledWith(pdfBlob, 'application/pdf')
    expect(pdfAction.value).toBeNull()
  })

  it('abrirProformaPdf descarga como respaldo si el navegador bloquea la ventana emergente', async () => {
    const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    portalCotizacionService.descargarProformaPdf.mockResolvedValueOnce(pdfBlob)
    openBlobInNewTab.mockReturnValueOnce(false)

    const { abrirProformaPdf } = useProformaPdf()
    await abrirProformaPdf({ idCotizacion: 42, codigoCotizacion: 'COT-2026-0042' })

    expect(downloadBlob).toHaveBeenCalledWith(pdfBlob, 'proforma-COT-2026-0042.pdf', 'application/pdf')
    expect(mockShowInfo).toHaveBeenCalled()
  })

  it('descargarProformaPdf solicita attachment y descarga con nombre sanitizado y fallback seguro', async () => {
    const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    portalCotizacionService.descargarProformaPdf.mockResolvedValue(pdfBlob)

    const { descargarProformaPdf, activeCotizacionId } = useProformaPdf()
    await descargarProformaPdf({ idCotizacion: 9, codigoCotizacion: 'COT/2026 0009' })
    expect(downloadBlob).toHaveBeenCalledWith(pdfBlob, 'proforma-COT-2026-0009.pdf', 'application/pdf')

    await descargarProformaPdf({ idCotizacion: 10, codigoCotizacion: '///   ' })
    expect(downloadBlob).toHaveBeenCalledWith(pdfBlob, 'proforma-cotizacion-10.pdf', 'application/pdf')
    expect(activeCotizacionId.value).toBeNull()
  })

  it('reporta mensaje de error del Blob JSON o fallback en español si el Blob de error no es JSON', async () => {
    const jsonErrBlob = new Blob([JSON.stringify({ message: 'Cotización no encontrada' })], {
      type: 'application/json'
    })
    portalCotizacionService.descargarProformaPdf.mockRejectedValueOnce({
      response: { data: jsonErrBlob }
    })

    const { descargarProformaPdf } = useProformaPdf()
    await descargarProformaPdf({ idCotizacion: 99, codigoCotizacion: 'COT-404' })
    expect(mockShowError).toHaveBeenCalledWith('Cotización no encontrada')

    const htmlErrBlob = new Blob(['<html>502 Bad Gateway</html>'], { type: 'text/html' })
    portalCotizacionService.descargarProformaPdf.mockRejectedValueOnce({
      message: 'Request failed with status code 502',
      response: { data: htmlErrBlob }
    })
    await descargarProformaPdf({ idCotizacion: 99, codigoCotizacion: 'COT-502' })
    expect(mockShowError).toHaveBeenLastCalledWith('No se pudo generar la proforma PDF de la cotización.')
  })

  it('rechaza Blob vacío o respuesta HTML con estado 200', async () => {
    portalCotizacionService.descargarProformaPdf.mockResolvedValueOnce(new Blob([], { type: 'application/pdf' }))
    const { descargarProformaPdf } = useProformaPdf()
    await descargarProformaPdf({ idCotizacion: 12, codigoCotizacion: 'COT-12' })
    expect(downloadBlob).not.toHaveBeenCalled()
    expect(mockShowError).toHaveBeenCalledWith('El servidor devolvió un archivo PDF vacío o inválido.')

    portalCotizacionService.descargarProformaPdf.mockResolvedValueOnce(
      new Blob(['<html>SPA</html>'], { type: 'text/html' })
    )
    await descargarProformaPdf({ idCotizacion: 12, codigoCotizacion: 'COT-12' })
    expect(downloadBlob).not.toHaveBeenCalled()
    expect(mockShowError).toHaveBeenCalledTimes(2)
  })

  it('evita llamadas duplicadas concurrentes e ignora cotizaciones sin id', async () => {
    let resolvePending
    portalCotizacionService.descargarProformaPdf.mockImplementationOnce(
      () => new Promise((resolve) => { resolvePending = resolve })
    )

    const { descargarProformaPdf } = useProformaPdf()
    await descargarProformaPdf(null)
    expect(portalCotizacionService.descargarProformaPdf).not.toHaveBeenCalled()

    const first = descargarProformaPdf({ idCotizacion: 5, codigoCotizacion: 'COT-5' })
    const second = descargarProformaPdf({ idCotizacion: 6, codigoCotizacion: 'COT-6' })
    resolvePending(new Blob(['%PDF-1.4'], { type: 'application/pdf' }))
    await Promise.all([first, second])

    expect(portalCotizacionService.descargarProformaPdf).toHaveBeenCalledTimes(1)
  })
})
