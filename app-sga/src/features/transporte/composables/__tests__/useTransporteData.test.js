import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useTransporteData } from '../useTransporteData'
import { transporteFlotaService } from '../../services/transporteFlotaService'

const showErrorMock = vi.fn()

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showError: showErrorMock,
    showSuccess: vi.fn()
  })
}))

vi.mock('../../services/transporteFlotaService', () => ({
  transporteFlotaService: {
    getDespachos: vi.fn(),
    getUnidades: vi.fn(),
    getConductores: vi.fn()
  }
}))

describe('useTransporteData', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('carga despachos, unidades y conductores en paralelo y calcula KPIs/alertas', async () => {
    transporteFlotaService.getDespachos.mockResolvedValue([
      { idDespachoTransporte: 1, estadoTransporte: 'EN_RUTA' },
      { idDespachoTransporte: 2, estadoTransporte: 'PENDIENTE_ASIGNACION' }
    ])
    transporteFlotaService.getUnidades.mockResolvedValue([
      { idUnidad: 1, placaTracto: 'V1A-780', tipoVehiculo: 'TRACTO', estadoOperativo: 'DISPONIBLE', estadoActivo: true }
    ])
    transporteFlotaService.getConductores.mockResolvedValue([
      { idConductor: 1, nombres: 'Raul', apellidos: 'Cardenas', tipoLicencia: 'A-IIIc', estadoOperativo: 'DISPONIBLE', estadoActivo: true }
    ])

    const state = useTransporteData()
    await state.loadAllData()

    expect(state.despachosList.value).toHaveLength(2)
    expect(state.unidadesList.value).toHaveLength(1)
    expect(state.conductoresList.value).toHaveLength(1)
    expect(state.kpis.value.despachosEnRuta).toBe(1)
    expect(state.kpis.value.tractosDisponibles).toBe(1)
    expect(showErrorMock).not.toHaveBeenCalled()
  })

  it('notifica error si falla la sincronización', async () => {
    transporteFlotaService.getDespachos.mockRejectedValue(new Error('Fallo de red'))
    transporteFlotaService.getUnidades.mockResolvedValue([])
    transporteFlotaService.getConductores.mockResolvedValue([])

    const state = useTransporteData()
    await state.loadAllData()

    expect(showErrorMock).toHaveBeenCalledWith('Fallo de red')
    expect(state.loading.value).toBe(false)
  })
})
