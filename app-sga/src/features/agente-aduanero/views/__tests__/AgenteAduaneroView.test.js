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
import AgenteAccionesCell from '../../components/AgenteAccionesCell.vue'
import AgenteExpedienteInformeView from '../../components/AgenteExpedienteInformeView.vue'
import TransitoOperacionModal from '../../components/TransitoOperacionModal.vue'
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

describe('Módulo Agente Aduanero - 3 Sub-opciones (Dashboard, Asignados Solo Lectura, Gestión)', () => {
  const sampleExpImportAsignado = {
    idExpediente: 10,
    codigoExpediente: 'EXP-2026-0010',
    codigoCotizacion: 'COT-2026-0010',
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    estadoExpediente: 'ASIGNADO',
    canalSunat: 'PENDIENTE',
    clienteRazonSocial: 'Textiles Pacífico SAC',
    clienteNumeroDocumento: '20444555666'
  }

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

  it('renderiza AgenteBandejaPanel en modo readonly (solo Canvas, sin toggle de Tabla)', async () => {
    const html = await renderComponent(AgenteBandejaPanel, {
      mode: 'readonly',
      expedientes: [sampleExpImport, sampleExpExportEmbarcado],
      kpiCounts: { todos: 2, transito: 1, inspeccion: 0, levante: 1, cerrados: 0 },
      phaseFilter: 'todos'
    })
    expect(html).toContain('Solo Lectura · Canvas')
    expect(html).toContain('Ver Ficha')
    expect(html).toContain('Cerrados')
    expect(html).not.toContain('Cambiar modo de vista')
    expect(html).not.toContain('>Gestionar<')
  })

  it('renderiza AgenteBandejaPanel en modo manage (Canvas simplificado, sin Cerrados, con botón Gestionar)', async () => {
    const html = await renderComponent(AgenteBandejaPanel, {
      mode: 'manage',
      expedientes: [sampleExpImportAsignado],
      kpiCounts: { todos: 1, transito: 1, inspeccion: 0, levante: 0, cerrados: 0 },
      phaseFilter: 'todos'
    })
    expect(html).toContain('Operación de Hitos')
    expect(html).toContain('SIGUIENTE HITO')
    expect(html).toContain('Registrar Envío de Origen Exitoso')
    expect(html).toContain('Gestionar')
    expect(html).not.toContain('Cambiar modo de vista')
    expect(html).not.toContain('>Cerrados<')
  })

  it('AgenteAccionesCell distingue correctamente modo readonly y modo manage', async () => {
    const htmlReadonly = await renderComponent(AgenteAccionesCell, {
      expediente: sampleExpImportAsignado,
      mode: 'readonly'
    })
    expect(htmlReadonly).toContain('Ver Ficha')
    expect(htmlReadonly).not.toContain('Gestionar')

    const htmlManage = await renderComponent(AgenteAccionesCell, {
      expediente: sampleExpImportAsignado,
      mode: 'manage'
    })
    expect(htmlManage).toContain('Gestionar')
    expect(htmlManage).not.toContain('Ver Ficha')
  })

  it('renderiza AgenteExpedientesCanvasGrid adaptado a readonly y manage', async () => {
    const htmlReadonly = await renderComponent(AgenteExpedientesCanvasGrid, {
      expedientes: [sampleExpImport],
      mode: 'readonly'
    })
    expect(htmlReadonly).toContain('agente-mini-stepper')
    expect(htmlReadonly).toContain('Ver Ficha')

    const htmlManage = await renderComponent(AgenteExpedientesCanvasGrid, {
      expedientes: [sampleExpImport],
      mode: 'manage'
    })
    expect(htmlManage).toContain('Esperando Canal SUNAT')
    expect(htmlManage).toContain('Gestionar')
  })

  it('AgenteExpedienteInformeView separa Ficha del Expediente (readonly) de Flujo Operativo (gestión)', async () => {
    const htmlReadonly = await renderComponent(AgenteExpedienteInformeView, {
      expediente: sampleExpImportAsignado,
      readonly: true
    })
    expect(htmlReadonly).toContain('Cliente &amp; Operación Comercial')
    expect(htmlReadonly).toContain('Ir a Gestionar')
    expect(htmlReadonly).not.toContain('Flujo Operativo del Despacho')
    expect(htmlReadonly).not.toContain('Registrar Hito Manual')

    const htmlManage = await renderComponent(AgenteExpedienteInformeView, {
      expediente: sampleExpImportAsignado,
      readonly: false,
      openExpedientes: [sampleExpImportAsignado, sampleExpImport]
    })
    expect(htmlManage).toContain('Flujo Operativo')
    expect(htmlManage).toContain('Ficha del Expediente')
    expect(htmlManage).toContain('Flujo Operativo del Despacho')
    expect(htmlManage).toContain('Registrar Envío de Origen Exitoso')
    expect(htmlManage).toContain('Reflejo en vivo Portal B2B')
  })

  it('exporta TransitoOperacionModal correctamente', () => {
    expect(TransitoOperacionModal).toBeDefined()
    expect(typeof TransitoOperacionModal).toBe('object')
  })

  it('AgenteAduaneroView soporta ?tab=gestion y normaliza ?tab=inspeccion a expedientes', async () => {
    mockRoute.query = { tab: 'gestion' }
    const htmlGestion = await renderComponent(AgenteAduaneroView)
    expect(htmlGestion).toContain('Operación de Hitos')
    expect(useNavigationStore().agenteAduaneroSubOption).toBe('gestion')

    mockRoute.query = { tab: 'inspeccion' }
    const htmlReadonly = await renderComponent(AgenteAduaneroView)
    expect(htmlReadonly).toContain('Solo Lectura · Canvas')
    expect(useNavigationStore().agenteAduaneroSubOption).toBe('expedientes')
  })
})
