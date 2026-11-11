import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useAgenteBandeja } from '../useAgenteBandeja'
import { expedienteService } from '@/shared/services/expedienteService'
import { userService } from '@/features/admin/users/services/userService'

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
  })

  it('filtra correctamente por pestaña activa (transito, inspeccion, levante)', async () => {
    expedienteService.getExpedientes.mockResolvedValueOnce([
      { idExpediente: 1, estadoExpediente: 'ASIGNADO', canalSunat: 'PENDIENTE' },
      { idExpediente: 2, estadoExpediente: 'INSPECCION', canalSunat: 'ROJO' },
      { idExpediente: 3, estadoExpediente: 'LEVANTE_AUTORIZADO', canalSunat: 'VERDE' }
    ])

    const { loadData, activeTab, filteredExpedientes } = useAgenteBandeja()
    await loadData()

    activeTab.value = 'inspeccion'
    expect(filteredExpedientes.value.length).toBe(1)
    expect(filteredExpedientes.value[0].idExpediente).toBe(2)

    activeTab.value = 'transito'
    expect(filteredExpedientes.value.length).toBe(1)
    expect(filteredExpedientes.value[0].idExpediente).toBe(1)

    activeTab.value = 'levante'
    expect(filteredExpedientes.value.length).toBe(1)
    expect(filteredExpedientes.value[0].idExpediente).toBe(3)

    activeTab.value = 'todos'
    expect(filteredExpedientes.value.length).toBe(3)
  })

  it('filtra por régimen, canal y término de búsqueda', async () => {
    expedienteService.getExpedientes.mockResolvedValueOnce([
      { idExpediente: 1, codigoExpediente: 'EXP-IMP-01', regimenAduanero: 'IMPORTACION', canalSunat: 'VERDE', clienteRazonSocial: 'ACME SAC' },
      { idExpediente: 2, codigoExpediente: 'EXP-EXP-02', regimenAduanero: 'EXPORTACION', canalSunat: 'ROJO', clienteRazonSocial: 'GLOBAL EXPORT' }
    ])

    const { loadData, filterRegimen, filterCanal, searchQuery, filteredExpedientes } = useAgenteBandeja()
    await loadData()

    filterRegimen.value = 'EXPORTACION'
    expect(filteredExpedientes.value.length).toBe(1)
    expect(filteredExpedientes.value[0].codigoExpediente).toBe('EXP-EXP-02')

    filterRegimen.value = ''
    filterCanal.value = 'VERDE'
    expect(filteredExpedientes.value.length).toBe(1)
    expect(filteredExpedientes.value[0].codigoExpediente).toBe('EXP-IMP-01')

    filterCanal.value = ''
    searchQuery.value = 'global'
    expect(filteredExpedientes.value.length).toBe(1)
    expect(filteredExpedientes.value[0].clienteRazonSocial).toBe('GLOBAL EXPORT')
  })

  it('permite a un supervisor/admin auditar agentes específicos', async () => {
    mockAuthUser = { idUsuario: 99, rol: 'SUPERVISOR_ADUANERO', nombre: 'Supervisor General' }
    expedienteService.getExpedientes.mockResolvedValueOnce([
      { idExpediente: 101, codigoExpediente: 'EXP-101', idAgenteAduanero: 11 }
    ])

    const { esSupervisorOAdmin, selectedAgenteId, loadData } = useAgenteBandeja()
    expect(esSupervisorOAdmin.value).toBe(true)

    selectedAgenteId.value = 11
    await loadData()
    expect(expedienteService.getExpedientes).toHaveBeenCalledWith({ idAgente: 11 })
  })
})
