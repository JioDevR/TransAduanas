import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAgenteBandeja } from '../useAgenteBandeja'
import { expedienteService } from '@/shared/services/expedienteService'

vi.mock('@/shared/services/expedienteService', () => ({
  expedienteService: {
    getExpedientes: vi.fn()
  }
}))

vi.mock('@/features/admin/users/services/userService', () => ({
  userService: {
    getAgentesAduaneros: vi.fn().mockResolvedValue([
      { idUsuario: 10, nombre: 'Carlos Agente', apellidos: 'Pérez' },
      { idUsuario: 11, nombre: 'Ana Agente', apellidos: 'López' }
    ])
  }
}))

let mockAuthUser = { idUsuario: 10, rol: 'AGENTE_ADUANERO', nombre: 'Carlos Agente' }

vi.mock('@/features/auth/stores/authStore', () => ({
  useAuthStore: () => ({
    user: mockAuthUser
  })
}))

describe('useAgenteBandeja', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockAuthUser = { idUsuario: 10, rol: 'AGENTE_ADUANERO', nombre: 'Carlos Agente' }
  })

  it('carga expedientes asignados al agente autenticado', async () => {
    expedienteService.getExpedientes.mockResolvedValueOnce([
      { idExpediente: 1, codigoExpediente: 'EXP-01', idAgenteAduanero: 10, estadoExpediente: 'ASIGNADO', canalSunat: 'PENDIENTE' },
      { idExpediente: 2, codigoExpediente: 'EXP-02', idAgenteAduanero: 10, estadoExpediente: 'LEVANTE_AUTORIZADO', canalSunat: 'VERDE', fechaLevanteAutorizacion: '2026-11-10' }
    ])

    const { expedientes, loadData, kpiCounts } = useAgenteBandeja()
    await loadData()

    expect(expedienteService.getExpedientes).toHaveBeenCalledWith({ idAgente: 10 })
    expect(expedientes.value.length).toBe(2)
    expect(kpiCounts.value.todos).toBe(2)
    expect(kpiCounts.value.transito).toBe(1)
    expect(kpiCounts.value.levante).toBe(1)
    expect(kpiCounts.value.inspeccion).toBe(0)
    expect(kpiCounts.value.cerrados).toBe(0)
  })

  it('filtra correctamente por phaseFilter sin fugar cerrados ni levantes a inspeccion', async () => {
    expedienteService.getExpedientes.mockResolvedValueOnce([
      { idExpediente: 1, estadoExpediente: 'ASIGNADO', canalSunat: 'PENDIENTE' },
      { idExpediente: 2, estadoExpediente: 'INSPECCION', canalSunat: 'ROJO' },
      { idExpediente: 3, estadoExpediente: 'LEVANTE_AUTORIZADO', canalSunat: 'ROJO', fechaLevanteAutorizacion: '2026-11-15T10:00:00' },
      { idExpediente: 4, estadoExpediente: 'CERRADO', canalSunat: 'ROJO', fechaLevanteAutorizacion: '2026-11-10T10:00:00' }
    ])

    const { loadData, phaseFilter, filteredExpedientes, kpiCounts } = useAgenteBandeja()
    await loadData()

    expect(kpiCounts.value.inspeccion).toBe(1)
    expect(kpiCounts.value.levante).toBe(1)
    expect(kpiCounts.value.cerrados).toBe(1)

    phaseFilter.value = 'inspeccion'
    expect(filteredExpedientes.value.map((e) => e.idExpediente)).toEqual([2])

    phaseFilter.value = 'transito'
    expect(filteredExpedientes.value.map((e) => e.idExpediente)).toEqual([1])

    phaseFilter.value = 'levante'
    expect(filteredExpedientes.value.map((e) => e.idExpediente)).toEqual([3])

    phaseFilter.value = 'cerrados'
    expect(filteredExpedientes.value.map((e) => e.idExpediente)).toEqual([4])

    phaseFilter.value = 'todos'
    expect(filteredExpedientes.value.length).toBe(4)
  })

  it('filtra por régimen, canal, ordenamiento y limpia filtros con resetFilters()', async () => {
    expedienteService.getExpedientes.mockResolvedValueOnce([
      { idExpediente: 1, codigoExpediente: 'EXP-IMP-02', regimenAduanero: 'IMPORTACION_DEFINITIVA', canalSunat: 'VERDE', clienteRazonSocial: 'ACME SAC', fechaApertura: '2026-11-01T10:00:00' },
      { idExpediente: 2, codigoExpediente: 'EXP-EXP-01', regimenAduanero: 'EXPORTACION_DEFINITIVA', canalSunat: 'ROJO', clienteRazonSocial: 'GLOBAL EXPORT', fechaApertura: '2026-11-05T10:00:00' }
    ])

    const { loadData, filterRegimen, filterCanal, searchQuery, sortBy, hasActiveFilters, resetFilters, filteredExpedientes } = useAgenteBandeja()
    await loadData()

    expect(filteredExpedientes.value[0].idExpediente).toBe(2)

    sortBy.value = 'CODE_ASC'
    expect(filteredExpedientes.value[0].codigoExpediente).toBe('EXP-EXP-01')

    filterRegimen.value = 'EXPORTACION'
    expect(hasActiveFilters.value).toBe(true)
    expect(filteredExpedientes.value.length).toBe(1)
    expect(filteredExpedientes.value[0].codigoExpediente).toBe('EXP-EXP-01')

    filterRegimen.value = ''
    filterCanal.value = 'VERDE'
    expect(filteredExpedientes.value.length).toBe(1)
    expect(filteredExpedientes.value[0].codigoExpediente).toBe('EXP-IMP-02')

    filterCanal.value = ''
    searchQuery.value = 'global'
    expect(filteredExpedientes.value.length).toBe(1)
    expect(filteredExpedientes.value[0].clienteRazonSocial).toBe('GLOBAL EXPORT')

    resetFilters()
    expect(hasActiveFilters.value).toBe(false)
    expect(filteredExpedientes.value.length).toBe(2)
  })

  it('permite a un supervisor/admin auditar agentes y cambiarVista entre dashboard, expedientes y fases', async () => {
    mockAuthUser = { idUsuario: 99, rol: 'SUPERVISOR_ADUANERO', nombre: 'Supervisor General' }
    expedienteService.getExpedientes.mockResolvedValueOnce([
      { idExpediente: 101, codigoExpediente: 'EXP-101', idAgenteAduanero: 11 }
    ])

    const { esSupervisorOAdmin, selectedAgenteId, loadData, cambiarVista, activeView, phaseFilter } = useAgenteBandeja()
    expect(esSupervisorOAdmin.value).toBe(true)

    cambiarVista('inspeccion')
    expect(activeView.value).toBe('expedientes')
    expect(phaseFilter.value).toBe('inspeccion')

    cambiarVista('gestion')
    expect(activeView.value).toBe('gestion')
    expect(phaseFilter.value).toBe('inspeccion')

    cambiarVista('expedientes')
    expect(activeView.value).toBe('expedientes')
    expect(phaseFilter.value).toBe('todos')

    cambiarVista('dashboard')
    expect(activeView.value).toBe('dashboard')

    selectedAgenteId.value = 11
    await loadData()
    expect(expedienteService.getExpedientes).toHaveBeenCalledWith({ idAgente: 11 })
  })

  it('en vista gestion excluye expedientes cerrados y anulados automáticamente', async () => {
    expedienteService.getExpedientes.mockResolvedValueOnce([
      { idExpediente: 1, estadoExpediente: 'ASIGNADO', canalSunat: 'PENDIENTE' },
      { idExpediente: 2, estadoExpediente: 'CERRADO', canalSunat: 'VERDE' },
      { idExpediente: 3, estadoExpediente: 'ANULADO', canalSunat: 'PENDIENTE' }
    ])

    const { loadData, cambiarVista, filteredExpedientes, gestionKpiCounts } = useAgenteBandeja()
    await loadData()
    cambiarVista('gestion')

    expect(gestionKpiCounts.value.todos).toBe(1)
    expect(filteredExpedientes.value.map((e) => e.idExpediente)).toEqual([1])
  })
})
