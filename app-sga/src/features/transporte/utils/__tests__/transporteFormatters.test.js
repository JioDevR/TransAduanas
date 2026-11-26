import { describe, it, expect } from 'vitest'
import {
  formatEstadoDespacho,
  getEstadoDespachoSeverity,
  formatTipoTraslado,
  formatTipoVehiculo,
  formatEstadoFlota,
  getEstadoFlotaSeverity,
  formatEstadoConductor,
  getEstadoConductorSeverity,
  formatPesoKg,
  formatKilometraje
} from '../transporteFormatters'

describe('transporteFormatters', () => {
  it('retorna severidades válidas de PrimeVue 4 sin usar warning', () => {
    const valid = new Set(['primary', 'secondary', 'success', 'info', 'warn', 'danger', 'contrast'])
    for (const est of ['PENDIENTE_ASIGNACION', 'PROGRAMADO', 'EN_RUTA', 'ENTREGADO', 'CANCELADO']) {
      expect(valid.has(getEstadoDespachoSeverity(est))).toBe(true)
    }
    for (const est of ['DISPONIBLE', 'EN_RUTA', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO']) {
      expect(valid.has(getEstadoFlotaSeverity(est))).toBe(true)
    }
    for (const est of ['DISPONIBLE', 'EN_RUTA', 'DESCANSO', 'SUSPENDIDO']) {
      expect(valid.has(getEstadoConductorSeverity(est))).toBe(true)
    }
  })

  it('formatea estados, tipos de traslado, vehículos y métricas', () => {
    expect(formatEstadoDespacho('PENDIENTE_ASIGNACION')).toBe('Pendiente Asignación')
    expect(formatEstadoDespacho('ENTREGADO')).toBe('Entregado Conforme')
    expect(formatTipoTraslado('RECOJO_EXPORTACION')).toBe('Recojo Exportación')
    expect(formatTipoVehiculo('CAMION_BARANDA')).toBe('Camión Baranda')
    expect(formatEstadoFlota('MANTENIMIENTO')).toBe('Mantenimiento')
    expect(formatEstadoConductor('SUSPENDIDO')).toBe('Suspendido')
    expect(formatPesoKg(32000)).toContain('32')
    expect(formatKilometraje(145000)).toContain('km')
  })
})
