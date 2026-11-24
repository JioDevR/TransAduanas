import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { useAuthStore } from '@/core/stores/authStore'
import PortalCambioPasswordObligatorioView from '../PortalCambioPasswordObligatorioView.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showWarn: vi.fn(),
    showInfo: vi.fn()
  })
}))

global.localStorage = {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}

async function renderView(userData) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore(pinia)
  authStore.token = 'jwt-token'
  authStore.user = userData

  const app = createSSRApp({
    render() {
      return h(PortalCambioPasswordObligatorioView)
    }
  })
  app.use(pinia)
  app.use(PrimeVue)
  return renderToString(app)
}

describe('PortalCambioPasswordObligatorioView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza estructura sgaUI, ficha corporativa y checklist sin exponer contraseñas de ejemplo', async () => {
    const html = await renderView({
      username: '20511894520',
      ruc: '20511894520',
      razonSocial: 'LG Electronics Perú S.A.',
      debeCambiarPassword: true
    })

    expect(html).toContain('sga-auth-page')
    expect(html).toContain('sga-auth-wrapper--wide')
    expect(html).toContain('sga-auth-card')
    expect(html).toContain('sga-auth-entity')
    expect(html).toContain('LG Electronics Perú S.A.')
    expect(html).toContain('20511894520')
    expect(html).toContain('sga-auth-checklist')
    expect(html).toContain('autocomplete="current-password"')
    expect(html).toContain('autocomplete="new-password"')
    expect(html).toContain('id="btn-update-pwd-submit"')
    expect(html).not.toContain('Password123!')
  })

  it('omite etiquetas de RUC o usuario cuando el perfil no incluye esos metadatos', async () => {
    const html = await renderView({
      razonSocial: 'Importadora Sin Metadatos S.A.C.',
      debeCambiarPassword: true
    })

    expect(html).toContain('Importadora Sin Metadatos S.A.C.')
    expect(html).not.toContain('RUC:')
    expect(html).not.toContain('Usuario: @')
  })

  it('omite separador y etiqueta de usuario cuando solo hay RUC disponible', async () => {
    const html = await renderView({
      ruc: '20511894520',
      razonSocial: 'Solo RUC S.A.C.',
      debeCambiarPassword: true
    })

    expect(html).toContain('RUC: 20511894520')
    expect(html).not.toContain('Usuario: @')
    expect(html).not.toContain('sga-auth-entity-sep')
  })
})
