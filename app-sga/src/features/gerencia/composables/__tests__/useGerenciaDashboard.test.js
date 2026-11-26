import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.stubGlobal('localStorage', {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
})

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush })
}))

const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()
vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: mockShowSuccess,
    showError: mockShowError
  })
}))

const mockOpenBlob = vi.fn()
vi.mock('@/shared/utils/download', () => ({
  openBlobInNewTab: (...args) => mockOpenBlob(...args)
}))

const mockGetSedes = vi.fn()
const mockDownloadSedesPdf = vi.fn()
vi.mock('@/features/admin/sedes/services/sedeService', () => ({
  default: {
    getSedes: (...args) => mockGetSedes(...args),
    downloadPdf: (...args) => mockDownloadSedesPdf(...args)
  }
}))

const mockGetPuertos = vi.fn()
const mockGetTiposCarga = vi.fn()
const mockGetMercancias = vi.fn()
const mockDownloadPuertosPdf = vi.fn()
const mockDownloadTiposCargaPdf = vi.fn()
const mockDownloadMercanciasPdf = vi.fn()
vi.mock('@/features/admin/catalogos/services/catalogoService', () => ({
  default: {
    getPuertos: (...args) => mockGetPuertos(...args),
    getTiposCarga: (...args) => mockGetTiposCarga(...args),
    getMercancias: (...args) => mockGetMercancias(...args),
    downloadPuertosPdf: (...args) => mockDownloadPuertosPdf(...args),
    downloadTiposCargaPdf: (...args) => mockDownloadTiposCargaPdf(...args),
    downloadMercanciasPdf: (...args) => mockDownloadMercanciasPdf(...args)
  }
}))

const mockGetUnidades = vi.fn()
const mockGetConductores = vi.fn()
vi.mock('@/features/transporte/services/transporteFlotaService', () => ({
  transporteFlotaService: {
    getUnidades: (...args) => mockGetUnidades(...args),
    getConductores: (...args) => mockGetConductores(...args)
  }
}))

import { useGerenciaDashboard } from '../useGerenciaDashboard'
import { useNavigationStore } from '@/core/stores/navigationStore'

describe('useGerenciaDashboard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockGetSedes.mockResolvedValue([{ idSede: 1, nombre: 'Callao', distrito: 'Callao', estado: 'OPERATIVO' }])
    mockGetPuertos.mockResolvedValue([{ idPuerto: 1, tipo: 'MARITIMO', estado: 'ACTIVO' }])
    mockGetTiposCarga.mockResolvedValue([{ idTipoCarga: 1, codigo: 'IMP-FCL', estado: 'ACTIVO' }])
    mockGetMercancias.mockResolvedValue([{ idMercancia: 1, estado: 'ACTIVO', requierePermiso: true }])
    mockGetUnidades.mockResolvedValue([{ idUnidad: 1, estadoOperativo: 'DISPONIBLE', capacidadCargaKg: 20000 }])
    mockGetConductores.mockResolvedValue([{ idConductor: 1, estadoOperativo: 'DISPONIBLE' }])
  })

  it('carga en paralelo los 6 servicios incluyendo mercancías y calcula métricas', async () => {
    const { loadDashboardData, metrics, sedesList, failedSources } = useGerenciaDashboard()
    await loadDashboardData()

    expect(mockGetMercancias).toHaveBeenCalledWith(false)
    expect(sedesList.value).toHaveLength(1)
    expect(failedSources.value).toEqual([])
    expect(metrics.value.sedesCount).toBe(1)
    expect(metrics.value.puertosCount).toBe(1)
    expect(metrics.value.cargasCount).toBe(1)
    expect(metrics.value.mercanciasCount).toBe(1)
    expect(metrics.value.mercanciasVuceCount).toBe(1)
    expect(metrics.value.unidadesCount).toBe(1)
    expect(metrics.value.conductoresCount).toBe(1)
  })

  it('notifica error parcial cuando falla uno de los servicios sin perder los demás', async () => {
    mockGetMercancias.mockRejectedValueOnce(new Error('503'))
    const { loadDashboardData, metrics, failedSources } = useGerenciaDashboard()
    await loadDashboardData()

    expect(metrics.value.sedesCount).toBe(1)
    expect(metrics.value.mercanciasCount).toBe(0)
    expect(failedSources.value).toContain('Mercancías')
    expect(mockShowError).toHaveBeenCalled()
  })

  it('navega a todas las sub-opciones reales y al puente transversal de flota', () => {
    const nav = useNavigationStore()
    const { goToSubOption } = useGerenciaDashboard()

    goToSubOption('mercancias')
    expect(nav.activeModule).toBe('gerencia-general')
    expect(nav.gerenciaGeneralSubOption).toBe('mercancias')
    expect(mockPush).toHaveBeenCalledWith('/gerencia-general/mercancias')

    goToSubOption('tipos-carga')
    expect(nav.gerenciaGeneralSubOption).toBe('tipos-carga')
    expect(mockPush).toHaveBeenCalledWith('/gerencia-general/tipos-carga')

    goToSubOption('flota')
    expect(nav.activeModule).toBe('transporte-flota')
    expect(nav.transporteFlotaSubOption).toBe('unidades')
    expect(mockPush).toHaveBeenCalledWith('/transporte-flota?tab=unidades')
  })

  it('exporta PDF para cualquiera de los 4 catálogos y notifica éxito o error', async () => {
    mockDownloadTiposCargaPdf.mockResolvedValueOnce(new Blob(['pdf']))
    mockDownloadMercanciasPdf.mockRejectedValueOnce(new Error('PDF fail'))

    const { exportCatalogoPdf } = useGerenciaDashboard()
    await exportCatalogoPdf('tipos-carga')
    expect(mockOpenBlob).toHaveBeenCalledTimes(1)
    expect(mockShowSuccess).toHaveBeenCalled()

    await exportCatalogoPdf('mercancias')
    expect(mockShowError).toHaveBeenCalled()
  })
})
