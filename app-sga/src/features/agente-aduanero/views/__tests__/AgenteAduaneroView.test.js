import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h, reactive } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'

import AgenteAduaneroView from '../AgenteAduaneroView.vue'
import AgenteDashboardPanel from '../../components/AgenteDashboardPanel.vue'
import AgenteBandejaPanel from '../../components/AgenteBandejaPanel.vue'
import AgenteExpedientesCanvasGrid from '../../components/AgenteExpedientesCanvasGrid.vue'
import AgenteExpedientesTable from '../../components/AgenteExpedientesTable.vue'
import AgenteAccionesCell from '../../components/AgenteAccionesCell.vue'
import AgenteExpedienteInformeView from '../../components/AgenteExpedienteInformeView.vue'
import AgenteOperacionTransitoView from '../../components/AgenteOperacionTransitoView.vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'

vi.stubGlobal('localStorage', {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
})

const mockRoute = reactive({ path: '/agente-aduanero', query: {} })

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({ push: vi.fn() })
}))

vi.mock('@/shared/services/expedienteService', () => ({
  expedienteService: {
    getExpedientes: vi.fn().mockResolvedValue([])
  }
}))

vi.mock('@/features/admin/users/services/userService', () => ({
  userService: {
    getAgentesAduaneros: vi.fn().mockResolvedValue([])
  }
}))

async function renderComponent(comp, props = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.user = { idUsuario: 10, rol: 'AGENTE_ADUANERO', nombres: 'Carlos', apellidos: 'Ruiz' }

  const app = createSSRApp({
    render: () => h(comp, props)
  })
  app.use(pinia)
  app.use(PrimeVue)
  app.use(ToastService)
  app.use(ConfirmationService)
  return renderToString(app)
}

describe('Módulo Agente Aduanero - Sub-opciones, Informe y Tránsito sgaUI', () => {
  const sampleExpImport = {
    idExpediente: 1,
    codigoExpediente: 'EXP-2026-0001',
    codigoCotizacion: 'COT-2026-0001',
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    estadoExpediente: 'EN_TRAMITE',
    canalSunat: 'PENDIENTE',
    clienteRazonSocial: 'Importadora Alpha SAC',
    clienteNumeroDocumento: '20555666777',
    fechaArriboAduana: '2026-11-22T09:30:00',
    observaciones: '[ARRIBO A PUERTO DE DESTINO] Arribo registrado el 22/11/2026 09:30 en terminal: DP World Callao'
  }

  const sampleExpExportEmbarcado = {
    idExpediente: 2,
    codigoExpediente: 'EXP-2026-0002',
    codigoCotizacion: 'COT-2026-0002',
    regimenAduanero: 'EXPORTACION_DEFINITIVA',
    estadoExpediente: 'LEVANTE_AUTORIZADO',
    canalSunat: 'VERDE',
    clienteRazonSocial: 'AgroExport Gamma SA',
    clienteNumeroDocumento: '20111222333',
    observaciones: '[EMBARQUE CONFIRMADO] Nave/Viaje: MAERSK 101'
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockRoute.query = {}
  })

  it('renderiza AgenteDashboardPanel con estructura sgaUI y sin título duplicado', async () => {
    const html = await renderComponent(AgenteDashboardPanel, {
      expedientes: [sampleExpImport, sampleExpExportEmbarcado]
    })
    expect(html).toContain('sga-dash-header')
    expect(html).toContain('sga-kpis-grid')
    expect(html).toContain('Semáforo de Canales')
    expect(html).not.toContain('Panel de Control del Agente Aduanero')
  })

  it('renderiza AgenteBandejaPanel con las 4 colas operativas y SgaListShell', async () => {
    for (const tab of ['todos', 'transito', 'inspeccion', 'levante']) {
      const html = await renderComponent(AgenteBandejaPanel, {
        expedientes: [sampleExpImport, sampleExpExportEmbarcado],
        kpiCounts: { todos: 2, transito: 1, inspeccion: 0, levante: 1 },
        activeTab: tab
      })
      expect(html).toContain('sga-list-panel')
      expect(html).toContain('Tránsito / Arribo')
      expect(html).toContain('Aforo SUNAT')
      expect(html).toContain('Levante &amp; Despacho')
    }
  })

  it('renderiza AgenteAccionesCell con Entrega Exterior cuando Exportación tiene [EMBARQUE CONFIRMADO]', async () => {
    const html = await renderComponent(AgenteAccionesCell, {
      expediente: sampleExpExportEmbarcado,
      activeTab: 'levante'
    })
    expect(html).toContain('Entrega Exterior')
    expect(html).not.toContain('Confirmar Embarque')
  })

  it('renderiza AgenteExpedientesCanvasGrid mostrando Esperando Canal cuando canalSunat es PENDIENTE y ya arribó', async () => {
    const html = await renderComponent(AgenteExpedientesCanvasGrid, {
      expedientes: [sampleExpImport],
      activeTab: 'transito'
    })
    expect(html).toContain('Esperando Canal')
    expect(html).toContain('Canal Pendiente')
  })

  it('renderiza AgenteExpedienteInformeView con terminal extraído de bitácora y sin campos fantasma', async () => {
    const html = await renderComponent(AgenteExpedienteInformeView, {
      expediente: sampleExpImport
    })
    expect(html).toContain('DP World Callao')
    expect(html).toContain('EXP-2026-0001')
    expect(html).not.toContain('Especialista SUNAT Asignado')
  })

  it('renderiza AgenteOperacionTransitoView detectando régimen EXPORTACION correctamente', async () => {
    const html = await renderComponent(AgenteOperacionTransitoView, {
      expediente: sampleExpExportEmbarcado,
      initialMode: 'transito'
    })
    expect(html).toContain('20111222333')
    expect(html).toContain('Datos de Tránsito')
    expect(html).not.toContain('1. Salida Origen')
    expect(html).not.toContain('bulto(s)')
  })

  it('AgenteAduaneroView soporta todas las sub-opciones por query param y limpia vista full-screen', async () => {
    mockRoute.query = { tab: 'inspeccion' }
    const html = await renderComponent(AgenteAduaneroView)
    expect(html).toContain('sga-list-panel')

    const navStore = useNavigationStore()
    expect(navStore.agenteAduaneroSubOption).toBe('inspeccion')
  })

  it('exporta AgenteExpedientesTable correctamente', () => {
    expect(AgenteExpedientesTable).toBeDefined()
    expect(typeof AgenteExpedientesTable).toBe('object')
  })
})
