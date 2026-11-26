import { describe, it, expect } from 'vitest'
import {
  formatEstadoAgente,
  getCanalSeverity,
  getExpedienteSeverity,
  getCanalLabel,
  getCanalHelpText
} from '../agenteFormatters'

describe('agenteFormatters', () => {
  it('retorna severidades válidas de PrimeVue 4 (warn para NARANJA)', () => {
    expect(getCanalSeverity('VERDE')).toBe('success')
    expect(getCanalSeverity('NARANJA')).toBe('warn')
    expect(getCanalSeverity('ROJO')).toBe('danger')
    expect(getCanalSeverity('PENDIENTE')).toBe('contrast')
    expect(getCanalSeverity(null)).toBe('contrast')
  })

  it('mapea los estados reales del enum EstadoExpediente', () => {
    expect(formatEstadoAgente('ASIGNADO')).toBe('Asignado')
    expect(formatEstadoAgente('EN_TRAMITE')).toBe('En Tránsito')
    expect(formatEstadoAgente('INSPECCION')).toBe('En Inspección')
    expect(formatEstadoAgente('LEVANTE_AUTORIZADO')).toBe('Levante Autorizado')
    expect(formatEstadoAgente('CERRADO')).toBe('Cerrado')
    expect(formatEstadoAgente('ANULADO')).toBe('Anulado')
  })

  it('retorna severidad de expediente acorde a PrimeVue 4', () => {
    expect(getExpedienteSeverity('ASIGNADO')).toBe('info')
    expect(getExpedienteSeverity('INSPECCION')).toBe('warn')
    expect(getExpedienteSeverity('LEVANTE_AUTORIZADO')).toBe('success')
    expect(getExpedienteSeverity('CERRADO')).toBe('secondary')
    expect(getExpedienteSeverity('ANULADO')).toBe('danger')
  })

  it('retorna texto de canal y ayuda contextual para PENDIENTE con y sin arribo', () => {
    expect(getCanalLabel({ canalSunat: 'PENDIENTE', fechaArriboAduana: null })).toBe('En espera de arribo')
    expect(getCanalLabel({ canalSunat: 'PENDIENTE', fechaArriboAduana: '2026-11-20T08:00:00' })).toBe('Canal Pendiente')
    expect(getCanalHelpText({ canalSunat: 'VERDE' })).toContain('Levante automático')
  })
})
