import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useAduanasExpedientes } from '../useAduanasExpedientes'

const mockExpedientes = [
  {
    idExpediente: 1,
    codigoExpediente: 'EXP-2026-0001',
    codigoCotizacion: 'COT-2026-0001',
    clienteRazonSocial: 'Importadora Norte SAC',
    clienteNumeroDocumento: '20111222333',
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    canalSunat: 'PENDIENTE',
    estadoExpediente: 'ASIGNADO',
    idAgenteAduanero: 10,
    agenteAduaneroNombre: 'Carlos Ruiz',
    numeroDam: null
  },
  {
    idExpediente: 2,
    codigoExpediente: 'EXP-2026-0002',
    codigoCotizacion: 'COT-2026-0002',
    clienteRazonSocial: 'AgroPeru SAC',
    clienteNumeroDocumento: '201929290',
    regimenAduanero: 'EXPORTACION_DEFINITIVA',
    canalSunat: 'VERDE',
    estadoExpediente: 'LEVANTE_AUTORIZADO',
    fechaLevanteAutorizacion: '2026-11-18T10:00:00',
    idAgenteAduanero: 20,
    agenteAduaneroNombre: 'Ana Torres',
    numeroDam: '118-2026-40-001234'
  }
]

describe('useAduanasExpedientes', () => {
  it('calcula conteos por fase y filtra por agente asignado', () => {
    const {
      filterAgente,
      agentesDisponibles,
      misExpedientesCount,
      faseCounts,
      filteredExpedientes
    } = useAduanasExpedientes({
      expedientes: ref(mockExpedientes),
      authStore: { userRole: 'SUPERVISOR_ADUANERO', user: { idUsuario: 10 } }
    })

    expect(agentesDisponibles.value).toHaveLength(2)
    expect(misExpedientesCount.value).toBe(1)
    expect(faseCounts.value.TODOS).toBe(2)
    expect(faseCounts.value.PENDIENTE_CANAL).toBe(1)
    expect(faseCounts.value.LEVANTE_AUTORIZADO).toBe(1)

    filterAgente.value = 'MINE'
    expect(filteredExpedientes.value).toHaveLength(1)
    expect(filteredExpedientes.value[0].codigoExpediente).toBe('EXP-2026-0001')
  })

  it('filtra por canal, régimen y búsqueda de texto, y restablece filtros', () => {
    const {
      filterCanal,
      filterRegimen,
      searchQuery,
      filteredExpedientes,
      hasActiveFilters,
      resetFilters
    } = useAduanasExpedientes({
      expedientes: ref(mockExpedientes),
      authStore: { userRole: 'SUPERVISOR_ADUANERO', user: { idUsuario: 10 } }
    })

    filterCanal.value = 'VERDE'
    filterRegimen.value = 'EXPORTACION'
    searchQuery.value = 'agroperu'

    expect(hasActiveFilters.value).toBe(true)
    expect(filteredExpedientes.value).toHaveLength(1)
    expect(filteredExpedientes.value[0].idExpediente).toBe(2)

    resetFilters()
    expect(hasActiveFilters.value).toBe(false)
    expect(filteredExpedientes.value).toHaveLength(2)
  })
})
