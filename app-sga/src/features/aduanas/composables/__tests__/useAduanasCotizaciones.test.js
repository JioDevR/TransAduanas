import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ref } from 'vue'
import { useAduanasCotizaciones } from '../useAduanasCotizaciones'
import { gestionAduaneraService } from '../../services/gestionAduaneraService'

vi.mock('../../services/gestionAduaneraService', () => ({
  gestionAduaneraService: {
    revisarCotizacion: vi.fn()
  }
}))

describe('useAduanasCotizaciones', () => {
  const sampleCotizaciones = [
    {
      idCotizacion: 1,
      codigoCotizacion: 'COT-001',
      estado: 'PENDIENTE',
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      clienteRazonSocial: 'Importadora Alfa SAC',
      clienteNumeroDocumento: '20100000001',
      totalCotizado: 1500
    },
    {
      idCotizacion: 2,
      codigoCotizacion: 'COT-002',
      estado: 'OBSERVADA',
      regimenAduanero: 'EXPORTACION_DEFINITIVA',
      clienteRazonSocial: 'Exportadora Beta SAC',
      clienteNumeroDocumento: '20200000002',
      totalCotizado: 3200
    },
    {
      idCotizacion: 3,
      codigoCotizacion: 'COT-003',
      estado: 'APROBADA',
      tienePagoAprobado: true,
      tieneExpediente: false,
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      clienteRazonSocial: 'Logística Gamma SAC',
      clienteNumeroDocumento: '20300000003',
      totalCotizado: 900
    }
  ]

  let notify
  let confirmAction
  let onReload

  beforeEach(() => {
    vi.clearAllMocks()
    notify = {
      showSuccess: vi.fn(),
      showError: vi.fn(),
      showInfo: vi.fn()
    }
    confirmAction = vi.fn().mockResolvedValue(true)
    onReload = vi.fn().mockResolvedValue()
  })

  it('clasifica, filtra por sub-cola, régimen, búsqueda y ordena', () => {
    const cotizaciones = ref(sampleCotizaciones)
    const {
      subQueueCotizaciones,
      searchQuery,
      filterRegimen,
      sortBy,
      cotizacionesClasificadas,
      filteredCotizaciones,
      hasActiveFilters,
      resetFilters
    } = useAduanasCotizaciones({
      cotizaciones,
      authStore: { user: { idUsuario: 7 } },
      notify,
      confirmAction,
      onReload
    })

    expect(cotizacionesClasificadas.value.nuevas).toHaveLength(1)
    expect(cotizacionesClasificadas.value.subsanadas).toHaveLength(1)
    expect(cotizacionesClasificadas.value.listasApertura).toHaveLength(1)
    expect(filteredCotizaciones.value.map((c) => c.codigoCotizacion)).toEqual(['COT-001'])

    subQueueCotizaciones.value = 'todas'
    sortBy.value = 'AMOUNT_DESC'
    expect(filteredCotizaciones.value.map((c) => c.codigoCotizacion)).toEqual(['COT-002', 'COT-001', 'COT-003'])

    filterRegimen.value = 'IMPORTACION_DEFINITIVA'
    expect(filteredCotizaciones.value.map((c) => c.codigoCotizacion)).toEqual(['COT-001', 'COT-003'])

    searchQuery.value = 'Gamma'
    expect(filteredCotizaciones.value.map((c) => c.codigoCotizacion)).toEqual(['COT-003'])
    expect(hasActiveFilters.value).toBe(true)

    resetFilters()
    expect(hasActiveFilters.value).toBe(false)
    expect(filteredCotizaciones.value).toHaveLength(3)
  })

  it('aprueba cotización tras confirmación y recarga datos', async () => {
    gestionAduaneraService.revisarCotizacion.mockResolvedValue({})
    const cotizaciones = ref(sampleCotizaciones)
    const { aprobarCotizacion } = useAduanasCotizaciones({
      cotizaciones,
      authStore: { user: { idUsuario: 11 } },
      notify,
      confirmAction,
      onReload
    })

    const result = await aprobarCotizacion(sampleCotizaciones[0])

    expect(result).toBe(true)
    expect(confirmAction).toHaveBeenCalledTimes(1)
    expect(gestionAduaneraService.revisarCotizacion).toHaveBeenCalledWith(1, {
      idAgenteRevisor: 11,
      estado: 'APROBADA',
      motivoObservacion: null,
      observaciones: 'Aprobada formalmente por Supervisión Aduanera.'
    })
    expect(notify.showSuccess).toHaveBeenCalled()
    expect(notify.showInfo).toHaveBeenCalled()
    expect(onReload).toHaveBeenCalledTimes(1)
  })

  it('cancela aprobación si el usuario rechaza el diálogo de confirmación', async () => {
    confirmAction.mockResolvedValue(false)
    const cotizaciones = ref(sampleCotizaciones)
    const { aprobarCotizacion } = useAduanasCotizaciones({
      cotizaciones,
      authStore: { user: { idUsuario: 11 } },
      notify,
      confirmAction,
      onReload
    })

    const result = await aprobarCotizacion(sampleCotizaciones[0])

    expect(result).toBe(false)
    expect(gestionAduaneraService.revisarCotizacion).not.toHaveBeenCalled()
  })

  it('inicia en "todas" cuando el usuario tiene habilitado el Gestor de Módulos', () => {
    const cotizaciones = ref(sampleCotizaciones)
    const { subQueueCotizaciones, filteredCotizaciones } = useAduanasCotizaciones({
      cotizaciones,
      authStore: { user: { idUsuario: 1 }, canSwitchModules: true },
      notify,
      confirmAction,
      onReload
    })

    expect(subQueueCotizaciones.value).toBe('todas')
    expect(filteredCotizaciones.value).toHaveLength(3)
  })
})
