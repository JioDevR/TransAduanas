import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import CotizacionDetalleView from '../CotizacionDetalleView.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ params: { id: '42' } })
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showWarn: vi.fn()
  })
}))

vi.mock('@/shared/composables/useConfirmation', () => ({
  useConfirmation: () => ({
    confirmAction: vi.fn()
  })
}))

describe('CotizacionDetalleView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renderiza la vista de detalle de cotización sin errores SSR', async () => {
    const app = createSSRApp({
      render() {
        return h(CotizacionDetalleView)
      }
    })
    app.use(PrimeVue)

    const html = await renderToString(app)
    expect(html).toContain('cotizacion-detalle-view')
    expect(html).toContain('Volver')
  })
})
