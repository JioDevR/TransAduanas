import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useAduanasDashboard } from '../useAduanasDashboard'

const mockCotizaciones = [
  {
    idCotizacion: 1,
    fechaEmision: '2026-11-18',
    estado: 'ENVIADA',
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    tieneExpediente: false,
    tienePagoAprobado: false
  },
  {
    idCotizacion: 2,
    fechaEmision: '2026-11-10',
    estado: 'APROBADA',
    regimenAduanero: 'EXPORTACION_DEFINITIVA',
    tieneExpediente: false,
    tienePagoAprobado: true
  }
]

const mockExpedientes = [
  {
    idExpediente: 10,
    fechaApertura: '2026-11-18',
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    canalSunat: 'PENDIENTE',
    estadoExpediente: 'ASIGNADO',
    idAgenteAduanero: 5,
    agenteAduaneroNombre: 'Ana Torres'
  },
  {
    idExpediente: 11,
    fechaApertura: '2026-10-15',
    regimenAduanero: 'EXPORTACION_DEFINITIVA',
    canalSunat: 'VERDE',
    estadoExpediente: 'LEVANTE_AUTORIZADO',
    idAgenteAduanero: 8,
    agenteAduaneroNombre: 'Carlos Ruiz'
  }
]

describe('useAduanasDashboard', () => {
  it('calcula métricas, tasa de levante y datasets de gráficos', () => {
    const {
      cotizacionesClasificadas,
      expedientesPendienteCanal,
      expedientesLevante,
      canalCounts,
      levanteRate,
      canalesChartData,
      tendenciaChartData,
      etapasChartData
    } = useAduanasDashboard({
      cotizaciones: ref(mockCotizaciones),
      expedientes: ref(mockExpedientes),
      referenceDate: new Date(2026, 10, 18)
    })

    expect(cotizacionesClasificadas.value.nuevas).toHaveLength(1)
    expect(cotizacionesClasificadas.value.listasApertura).toHaveLength(1)
    expect(expedientesPendienteCanal.value).toHaveLength(1)
    expect(expedientesLevante.value).toHaveLength(1)
    expect(canalCounts.value).toEqual({ PENDIENTE: 1, VERDE: 1, NARANJA: 0, ROJO: 0 })
    expect(levanteRate.value).toBe(50)
    expect(canalesChartData.value.datasets[0].data).toEqual([1, 1, 0, 0])
    expect(tendenciaChartData.value.labels).toEqual(['Oct 26', 'Nov 26'])
    expect(etapasChartData.value.datasets[0].data).toEqual([1, 0, 1, 0])
  })

  it('filtra por período, agente y régimen, y restablece filtros', () => {
    const {
      filterPeriodo,
      filterAgenteId,
      filterRegimen,
      filteredCotizaciones,
      filteredExpedientes,
      hasActiveFilters,
      resetDashboardFilters
    } = useAduanasDashboard({
      cotizaciones: ref(mockCotizaciones),
      expedientes: ref(mockExpedientes),
      referenceDate: new Date(2026, 10, 18)
    })

    filterPeriodo.value = 'HOY'
    expect(filteredCotizaciones.value).toHaveLength(1)
    expect(filteredExpedientes.value).toHaveLength(1)
    expect(hasActiveFilters.value).toBe(true)

    filterPeriodo.value = 'ALL'
    filterAgenteId.value = 8
    filterRegimen.value = 'EXPORTACION_DEFINITIVA'
    expect(filteredExpedientes.value).toHaveLength(1)
    expect(filteredExpedientes.value[0].idExpediente).toBe(11)

    resetDashboardFilters()
    expect(hasActiveFilters.value).toBe(false)
    expect(filteredCotizaciones.value).toHaveLength(2)
    expect(filteredExpedientes.value).toHaveLength(2)
  })
})
