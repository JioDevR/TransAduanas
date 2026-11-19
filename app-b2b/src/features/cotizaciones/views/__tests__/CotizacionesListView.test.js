import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import CotizacionesListView from '../CotizacionesListView.vue'
import { portalCotizacionService } from '../../services/portalCotizacionService'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  }),
  useRoute: () => ({
    query: {}
  })
}))

vi.mock('../../services/portalCotizacionService', () => ({
  portalCotizacionService: {
    listarCotizaciones: vi.fn()
  }
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showWarn: vi.fn()
  })
}))

describe('CotizacionesListView Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renderiza la lista de cotizaciones sin errores de renderizado', async () => {
    portalCotizacionService.listarCotizaciones.mockResolvedValue([
      {
        idCotizacion: 1,
        codigoCotizacion: 'COT-2026-0001',
        nombreMercancia: 'Maquinaria Pesada',
        codigoMercancia: '8429.51.00',
        nombreTipoCarga: 'General',
        regimenAduanero: 'IMPORTACION_DEFINITIVA',
        nombrePuertoEnvio: 'Shanghai',
        nombrePuertoDestino: 'Callao',
        valorFob: 50000,
        totalCotizado: 12500,
        estado: 'APROBADA',
        estadoPago: 'PENDIENTE',
        idUsuarioComercial: 4
      },
      {
        idCotizacion: 2,
        codigoCotizacion: 'COT-2026-0002',
        nombreMercancia: 'Textiles',
        codigoMercancia: '6109.10.00',
        nombreTipoCarga: 'General',
        regimenAduanero: 'EXPORTACION_DEFINITIVA',
        nombrePuertoEnvio: 'Callao',
        nombrePuertoDestino: 'Miami',
        valorFob: 20000,
        totalCotizado: 0,
        estado: 'PENDIENTE',
        estadoPago: 'SIN_PAGO',
        idUsuarioComercial: null
      }
    ])

    const app = createSSRApp({
      render() {
        return h(CotizacionesListView)
      }
    })

    const html = await renderToString(app)
    expect(html).toContain('Mis Cotizaciones Logísticas')
    expect(html).toBeTypeOf('string')
  })
})
