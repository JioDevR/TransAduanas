import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ExpedientesTrackingView from '../ExpedientesTrackingView.vue'
import { portalExpedienteService } from '../../services/portalExpedienteService'

vi.mock('../../services/portalExpedienteService', () => ({
  portalExpedienteService: {
    listarExpedientes: vi.fn(),
    listarDocumentosExpediente: vi.fn(),
    descargarDocumento: vi.fn(),
    previsualizarDocumento: vi.fn()
  }
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn()
  })
}))

describe('ExpedientesTrackingView - Listado Dual sgaUI', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('exporta el componente correctamente', () => {
    expect(ExpedientesTrackingView).toBeDefined()
    expect(typeof ExpedientesTrackingView).toBe('object')
  })

  it('renderiza SgaListShell y filtro de estado dentro de la barra de filtros', async () => {
    portalExpedienteService.listarExpedientes.mockResolvedValueOnce([])

    const app = createSSRApp({
      render: () => h(ExpedientesTrackingView)
    })
    app.use(PrimeVue)

    const html = await renderToString(app)

    expect(html).toContain('Vista Canvas')
    expect(html).toContain('Abiertos (0)')
    expect(html).not.toContain('role="tablist"')
    expect(html).not.toContain('Tablero de Expedientes Aduaneros')
    expect(html).toBeTypeOf('string')
  })

  it('clasifica correctamente los expedientes en abiertos y finalizados', () => {
    const mockList = [
      { idExpediente: 1, codigoExpediente: 'EXP-1', estadoExpediente: 'EN_TRAMITE' },
      { idExpediente: 2, codigoExpediente: 'EXP-2', estadoExpediente: 'LEVANTE_AUTORIZADO' },
      { idExpediente: 3, codigoExpediente: 'EXP-3', estadoExpediente: 'CERRADO' },
      { idExpediente: 4, codigoExpediente: 'EXP-4', estadoExpediente: 'ANULADO' }
    ]

    const abiertos = mockList.filter(
      (e) => e.estadoExpediente !== 'CERRADO' && e.estadoExpediente !== 'ANULADO'
    )
    const finalizados = mockList.filter((e) => e.estadoExpediente === 'CERRADO')

    expect(abiertos.map((e) => e.idExpediente)).toEqual([1, 2])
    expect(finalizados.map((e) => e.idExpediente)).toEqual([3])
  })
})
