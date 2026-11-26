import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'

vi.stubGlobal('localStorage', {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
})

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

vi.mock('@/features/admin/sedes/services/sedeService', () => ({
  default: { getSedes: vi.fn().mockResolvedValue([]), downloadPdf: vi.fn() }
}))

vi.mock('@/features/admin/catalogos/services/catalogoService', () => ({
  default: {
    getPuertos: vi.fn().mockResolvedValue([]),
    getTiposCarga: vi.fn().mockResolvedValue([]),
    getMercancias: vi.fn().mockResolvedValue([]),
    downloadPuertosPdf: vi.fn(),
    downloadTiposCargaPdf: vi.fn(),
    downloadMercanciasPdf: vi.fn()
  }
}))

vi.mock('@/features/transporte/services/transporteFlotaService', () => ({
  transporteFlotaService: {
    getUnidades: vi.fn().mockResolvedValue([]),
    getConductores: vi.fn().mockResolvedValue([])
  }
}))

import GerenciaDashboardView from '../GerenciaDashboardView.vue'
import { useAuthStore } from '@/features/auth/stores/authStore'

describe('GerenciaDashboardView (sgaUI)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('usa sga-dashboard.css, respeta Single-Title Rule e incluye las 4 sub-opciones reales y PDFs', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.user = { idUsuario: 1, rol: 'GERENTE_GENERAL', nombres: 'Ana', apellidos: 'Torres' }

    const app = createSSRApp({
      render: () => h(GerenciaDashboardView)
    })
    app.use(pinia)
    app.use(PrimeVue)
    app.use(ToastService)

    const html = await renderToString(app)

    expect(html).toContain('sga-dash-header')
    expect(html).toContain('sga-kpis-grid')
    expect(html).toContain('sga-kpi-card')
    expect(html).toContain('sga-charts-grid')

    // Regla de Título Único: sin <h1> ni título redundante debajo del topbar
    expect(html).not.toContain('<h1')
    expect(html).not.toContain('Dashboard Ejecutivo de Gerencia')

    // Accesos a los 4 maestros reales de Gerencia General + puente a Flota
    expect(html).toContain('Sedes')
    expect(html).toContain('Puertos &amp; Terminales')
    expect(html).toContain('Tipos de Carga')
    expect(html).toContain('Mercancías &amp; Partidas')
    expect(html).toContain('Flota y Conductores')
  })
})
