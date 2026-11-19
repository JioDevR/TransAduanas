import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import PortalCotizacionForm from '../PortalCotizacionForm.vue'
import { portalCotizacionService } from '../../services/portalCotizacionService'

let storage = {}
global.localStorage = {
  getItem: vi.fn((key) => storage[key] ?? null),
  setItem: vi.fn((key, value) => { storage[key] = String(value) }),
  removeItem: vi.fn((key) => { delete storage[key] }),
  clear: vi.fn(() => { storage = {} })
}

describe('PortalCotizacionForm Component', () => {
  let pinia

  beforeEach(() => {
    storage = {}
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  const setupComponent = async (props = {}) => {
    let exposedInstance = null
    const originalSetup = PortalCotizacionForm.setup
    PortalCotizacionForm.setup = (p, ctx) => {
      const wrappedCtx = {
        ...ctx,
        expose: (exposed) => {
          exposedInstance = exposed
          if (ctx.expose) ctx.expose(exposed)
        }
      }
      return originalSetup(p, wrappedCtx)
    }

    try {
      const app = createSSRApp({
        render() {
          return h(PortalCotizacionForm, props)
        }
      })
      app.use(pinia)
      app.use(PrimeVue)
      app.use(ToastService)
      await renderToString(app)
      return exposedInstance
    } finally {
      PortalCotizacionForm.setup = originalSetup
    }
  }

  it('bloquea envio si valorFob es cero o invalido y salta al paso 3', async () => {
    vi.spyOn(portalCotizacionService, 'obtenerPuertos').mockResolvedValue([])
    vi.spyOn(portalCotizacionService, 'obtenerTiposCarga').mockResolvedValue([])
    vi.spyOn(portalCotizacionService, 'obtenerMercancias').mockResolvedValue([])
    const solicitarSpy = vi.spyOn(portalCotizacionService, 'solicitarCotizacion').mockResolvedValue({ idCotizacion: 99 })

    const exposed = await setupComponent()

    expect(exposed).toBeDefined()
    expect(exposed.currentStep.value).toBe(1)
    expect(exposed.form.value.valorFob).toBe(0)

    // Intenta enviar con valorFob = 0
    await exposed.handleSubmit()

    expect(solicitarSpy).not.toHaveBeenCalled()
    expect(exposed.currentStep.value).toBe(3)
  })

  it('envia payload completo con valorFob y montoFleteEstimado sin inventar otros costos y sube documentos en cola', async () => {
    vi.spyOn(portalCotizacionService, 'obtenerPuertos').mockResolvedValue([])
    vi.spyOn(portalCotizacionService, 'obtenerTiposCarga').mockResolvedValue([])
    vi.spyOn(portalCotizacionService, 'obtenerMercancias').mockResolvedValue([])

    const mockCreated = { idCotizacion: 99, codigoCotizacion: 'COT-2026-0099' }
    const solicitarSpy = vi.spyOn(portalCotizacionService, 'solicitarCotizacion').mockResolvedValue(mockCreated)
    const subirSpy = vi.spyOn(portalCotizacionService, 'subirDocumentosCotizacion').mockResolvedValue({
      uploaded: [{ name: 'factura.pdf' }],
      failed: []
    })

    let emittedSaved = null
    const exposed = await setupComponent({
      onSaved: (payload) => {
        emittedSaved = payload
      }
    })

    const fakeFile = { name: 'factura.pdf', size: 1024, type: 'application/pdf' }
    exposed.form.value.valorFob = 22000
    exposed.form.value.montoFleteEstimado = 1850
    exposed.form.value.servicioTransporte = true
    exposed.form.value.origenDepartamento = 'CALLAO'
    exposed.form.value.origenProvincia = 'Callao'
    exposed.form.value.destinoDepartamento = 'LIMA'
    exposed.form.value.destinoProvincia = 'Lima (Metropolitana)'
    exposed.queuedDocs.value = [
      { file: fakeFile, categoria: 'FACTURA', descripcion: 'Invoice' }
    ]

    await exposed.handleSubmit()

    expect(solicitarSpy).toHaveBeenCalledTimes(1)
    expect(solicitarSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        regimenAduanero: 'IMPORTACION_DEFINITIVA',
        valorFob: 22000,
        montoFleteEstimado: 1850,
        servicioTransporte: true,
        origenDepartamento: 'CALLAO',
        origenProvincia: 'Callao',
        destinoDepartamento: 'LIMA',
        destinoProvincia: 'Lima (Metropolitana)'
      })
    )

    // Verificar que no se inventaron costos en el payload
    const calledPayload = solicitarSpy.mock.calls[0][0]
    expect(calledPayload.montoSeguroEstimado).toBeUndefined()
    expect(calledPayload.costoTransporteInterno).toBeUndefined()
    expect(calledPayload.montoHonorarios).toBeUndefined()

    expect(subirSpy).toHaveBeenCalledTimes(1)
    expect(subirSpy).toHaveBeenCalledWith(99, exposed.queuedDocs.value)

    expect(emittedSaved).toEqual({
      cotizacion: mockCreated,
      docsResult: {
        uploaded: [{ name: 'factura.pdf' }],
        failed: []
      }
    })
  })

  it('si falla un documento tras crear la cotizacion emite saved con alerta parcial y no duplica solicitarCotizacion', async () => {
    vi.spyOn(portalCotizacionService, 'obtenerPuertos').mockResolvedValue([])
    vi.spyOn(portalCotizacionService, 'obtenerTiposCarga').mockResolvedValue([])
    vi.spyOn(portalCotizacionService, 'obtenerMercancias').mockResolvedValue([])

    const mockCreated = { idCotizacion: 100, codigoCotizacion: 'COT-2026-0100' }
    const solicitarSpy = vi.spyOn(portalCotizacionService, 'solicitarCotizacion').mockResolvedValue(mockCreated)
    const subirSpy = vi.spyOn(portalCotizacionService, 'subirDocumentosCotizacion').mockResolvedValue({
      uploaded: [],
      failed: [{ name: 'bl.pdf', error: '500 Internal Error' }]
    })

    let emittedSaved = null
    const exposed = await setupComponent({
      onSaved: (payload) => {
        emittedSaved = payload
      }
    })

    const fakeFile = { name: 'bl.pdf', size: 2048, type: 'application/pdf' }
    exposed.form.value.valorFob = 15000
    exposed.queuedDocs.value = [
      { file: fakeFile, categoria: 'TRANSPORTE_BL', descripcion: 'Bill of Lading' }
    ]

    await exposed.handleSubmit()

    expect(solicitarSpy).toHaveBeenCalledTimes(1)
    expect(subirSpy).toHaveBeenCalledTimes(1)
    expect(subirSpy).toHaveBeenCalledWith(100, exposed.queuedDocs.value)

    expect(emittedSaved).toBeDefined()
    expect(emittedSaved.cotizacion).toEqual(mockCreated)
    expect(emittedSaved.docsResult.failed).toHaveLength(1)
    expect(emittedSaved.docsResult.failed[0].name).toBe('bl.pdf')
  })
})
