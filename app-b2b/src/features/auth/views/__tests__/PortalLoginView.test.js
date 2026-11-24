import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import PortalLoginView from '../PortalLoginView.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

global.localStorage = {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}

describe('PortalLoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renderiza la estructura sgaUI y controles de acceso PrimeVue sin cuentas de demostración', async () => {
    const app = createSSRApp({
      render() {
        return h(PortalLoginView)
      }
    })
    app.use(PrimeVue)

    const html = await renderToString(app)
    expect(html).toContain('sga-auth-page')
    expect(html).toContain('sga-auth-card')
    expect(html).toContain('TransAduanas')
    expect(html).toContain('PORTAL DEL CLIENTE B2B')
    expect(html).toContain('id="username"')
    expect(html).toContain('id="password"')
    expect(html).toContain('id="btn-login-submit"')
    expect(html).toContain('autocomplete="username"')
    expect(html).toContain('autocomplete="current-password"')
    expect(html).not.toContain('sga-auth-demo')
    expect(html).not.toContain('Cuentas de demostración')
    expect(html).not.toContain('Password123!')
  })
})
