import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useAgenteDashboard } from '../useAgenteDashboard'

describe('useAgenteDashboard', () => {
  const sampleExpedientes = [
    {
      idExpediente: 1,
      codigoExpediente: 'EXP-2026-0001',
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      estadoExpediente: 'INSPECCION',
      canalSunat: 'ROJO',
      fechaApertura: '2026-11-24T09:00:00'
    },
    {
      idExpediente: 2,
      codigoExpediente: 'EXP-2026-0002',
      regimenAduanero: 'EXPORTACION_DEFINITIVA',
      estadoExpediente: 'LEVANTE_AUTORIZADO',
      canalSunat: 'VERDE',
      fechaLevanteAutorizacion: '2026-11-23T12:00:00',
      fechaApertura: '2026-11-20T09:00:00',
      observaciones: '[EMBARQUE CONFIRMADO] Nave: MAERSK'
    },
    {
      idExpediente: 3,
      codigoExpediente: 'EXP-2026-0003',
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      estadoExpediente: 'LEVANTE_AUTORIZADO',
      canalSunat: 'VERDE',
      servicioTransporte: false,
      fechaLevanteAutorizacion: '2026-11-22T12:00:00',
      fechaApertura: '2026-11-01T09:00:00',
      observaciones: '[RETIRO DIRECTO CLIENTE] Terminal: DPW'
    },
    {
      idExpediente: 4,
      codigoExpediente: 'EXP-2026-0004',
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      estadoExpediente: 'ASIGNADO',
      canalSunat: 'PENDIENTE',
      fechaApertura: '2026-10-10T09:00:00'
    }
  ]

  it('calcula KPIs, tasa de levante, canales y datasets Chart.js', () => {
    const expedientes = ref(sampleExpedientes)
    const { counts, canalCounts, regimenCounts, levanteRate, canalesChartData, etapasChartData } =
      useAgenteDashboard({ expedientes, referenceDate: new Date(2026, 10, 24) })

    expect(counts.value.todos).toBe(4)
    expect(counts.value.transito).toBe(1)
    expect(counts.value.inspeccion).toBe(1)
    expect(counts.value.levante).toBe(2)

    expect(canalCounts.value.ROJO).toBe(1)
    expect(canalCounts.value.VERDE).toBe(2)
    expect(canalCounts.value.PENDIENTE).toBe(1)

    expect(regimenCounts.value.importacion).toBe(3)
    expect(regimenCounts.value.exportacion).toBe(1)
    expect(levanteRate.value).toBe(50)

    expect(canalesChartData.value.datasets[0].data).toEqual([1, 2, 0, 1])
    expect(etapasChartData.value.datasets[0].data).toEqual([1, 1, 2, 0])
  })

  it('filtra por período y régimen y permite resetear filtros', () => {
    const expedientes = ref(sampleExpedientes)
    const { filterPeriodo, filterRegimen, filteredExpedientes, hasActiveFilters, resetDashboardFilters } =
      useAgenteDashboard({ expedientes, referenceDate: new Date(2026, 10, 24) })

    filterPeriodo.value = '7D'
    expect(filteredExpedientes.value.map((e) => e.idExpediente)).toEqual([1, 2])
    expect(hasActiveFilters.value).toBe(true)

    filterRegimen.value = 'EXPORTACION_DEFINITIVA'
    expect(filteredExpedientes.value.map((e) => e.idExpediente)).toEqual([2])

    resetDashboardFilters()
    expect(hasActiveFilters.value).toBe(false)
    expect(filteredExpedientes.value.length).toBe(4)
  })

  it('excluye de despachosPrioritarios los expedientes que ya tienen retiro directo registrado', () => {
    const expedientes = ref(sampleExpedientes)
    const { despachosPrioritarios } = useAgenteDashboard({
      expedientes,
      referenceDate: new Date(2026, 10, 24)
    })

    const ids = despachosPrioritarios.value.map((item) => item.expediente.idExpediente)
    expect(ids).toContain(1) // Inspección ROJO
    expect(ids).toContain(2) // Exportación embarcada esperando recepción exterior
    expect(ids).not.toContain(3) // Ya tiene retiro directo registrado
  })
})
