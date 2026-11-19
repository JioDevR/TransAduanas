import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAperturaExpedienteForm } from '../useAperturaExpedienteForm'
import { gestionAduaneraService } from '../../services/gestionAduaneraService'
import { expedienteService } from '../../services/expedienteService'
import { userService } from '@/features/admin/users/services/userService'

vi.mock('../../services/gestionAduaneraService', () => ({
  gestionAduaneraService: {
    getCotizacionById: vi.fn()
  }
}))

vi.mock('../../services/expedienteService', () => ({
  expedienteService: {
    createExpediente: vi.fn()
  }
}))

vi.mock('@/features/admin/users/services/userService', () => ({
  userService: {
    getAgentesAduaneros: vi.fn()
  }
}))

describe('useAperturaExpedienteForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('carga cotización elegible, preselecciona agente y envía payload limpio', async () => {
    gestionAduaneraService.getCotizacionById.mockResolvedValue({
      idCotizacion: 22,
      codigoCotizacion: 'COT-0022',
      estado: 'APROBADA',
      tieneExpediente: false,
      tienePagoAprobado: true
    })
    userService.getAgentesAduaneros.mockResolvedValue([
      { idUsuario: 5, nombres: 'Ana', apellidos: 'Torres', username: 'atorres', codigoSunat: 'SUN-05' }
    ])
    expedienteService.createExpediente.mockResolvedValue({
      idExpediente: 99,
      codigoExpediente: 'EXP-2026-0099'
    })

    const {
      cotizacion,
      form,
      isCotizacionElegible,
      isFormReady,
      loadData,
      submit
    } = useAperturaExpedienteForm({
      authStore: { userRole: 'SUPERVISOR_ADUANERO', user: { idUsuario: 1 } }
    })

    await loadData(22)

    expect(cotizacion.value?.codigoCotizacion).toBe('COT-0022')
    expect(form.value.idAgenteAduanero).toBe(5)
    expect(isCotizacionElegible.value).toBe(true)
    expect(isFormReady.value).toBe(true)

    form.value.naveViaje = 'MSC Gülsün'
    const creado = await submit()

    expect(expedienteService.createExpediente).toHaveBeenCalledWith({
      idCotizacion: 22,
      idAgenteAduanero: 5,
      contactoExteriorNombre: null,
      contactoExteriorTelefono: null,
      contactoExteriorCorreo: null,
      fechaZarpeEtd: null,
      fechaLlegadaEta: null,
      numeroContenedor: null,
      precintoAduanero: null,
      naveViaje: 'MSC Gülsün',
      observaciones: null
    })
    expect(creado?.codigoExpediente).toBe('EXP-2026-0099')
  })

  it('bloquea apertura si la cotización no tiene pago aprobado o ya tiene expediente', async () => {
    gestionAduaneraService.getCotizacionById.mockResolvedValue({
      idCotizacion: 30,
      codigoCotizacion: 'COT-0030',
      estado: 'APROBADA',
      tieneExpediente: false,
      tienePagoAprobado: false
    })
    userService.getAgentesAduaneros.mockResolvedValue([
      { idUsuario: 5, nombres: 'Ana', apellidos: 'Torres', username: 'atorres' }
    ])

    const { isCotizacionElegible, eligibilityWarning, isFormReady, loadData, submit } =
      useAperturaExpedienteForm({
        authStore: { userRole: 'SUPERVISOR_ADUANERO', user: { idUsuario: 1 } }
      })

    await loadData(30)

    expect(isCotizacionElegible.value).toBe(false)
    expect(eligibilityWarning.value).toContain('Finanzas')
    expect(isFormReady.value).toBe(false)

    const res = await submit()
    expect(res).toBeNull()
    expect(expedienteService.createExpediente).not.toHaveBeenCalled()
  })
})
