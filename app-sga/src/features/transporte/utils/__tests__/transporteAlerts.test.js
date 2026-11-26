import { describe, it, expect } from 'vitest'
import {
  diasParaVencer,
  isFechaVencida,
  isFechaPorVencer,
  buildAlertasMtc,
  buildTransporteKpis
} from '../transporteAlerts'

describe('transporteAlerts', () => {
  const refDate = new Date(2026, 10, 24) // 2026-11-24

  it('detecta fechas vencidas y por vencer sin desfase UTC', () => {
    expect(diasParaVencer('2026-11-24', refDate)).toBe(0)
    expect(diasParaVencer('2026-11-23', refDate)).toBe(-1)
    expect(isFechaVencida('2026-11-23', refDate)).toBe(true)
    expect(isFechaVencida('2026-11-24', refDate)).toBe(false)
    expect(isFechaPorVencer('2026-12-10', 30, refDate)).toBe(true)
    expect(isFechaPorVencer('2027-02-01', 30, refDate)).toBe(false)
  })

  it('construye todas las alertas MTC ordenadas sin truncar silenciosamente', () => {
    const unidades = [
      { idUnidad: 1, placaTracto: 'A1A-111', soatVencimiento: '2026-11-20', citvVencimiento: '2026-12-05', estadoActivo: true },
      { idUnidad: 2, placaTracto: 'B2B-222', soatVencimiento: '2026-12-01', citvVencimiento: '2028-01-01', estadoActivo: true },
      { idUnidad: 3, placaTracto: 'C3C-333', soatVencimiento: '2026-11-01', estadoActivo: false } // inactiva se ignora
    ]
    const conductores = [
      { idConductor: 1, nombreCompleto: 'Wilmer Galarza', numeroLicencia: 'Q60345678', tipoLicencia: 'A-IIIb', fechaVencimientoLicencia: '2026-11-26', estadoActivo: true },
      { idConductor: 2, nombreCompleto: 'Juan Castaneda', numeroLicencia: 'Q60678901', tipoLicencia: 'A-IIIb', fechaVencimientoLicencia: '2026-12-15', estadoActivo: true },
      { idConductor: 3, nombreCompleto: 'Felix Paredes', numeroLicencia: 'Q60567890', tipoLicencia: 'A-IIIc', fechaVencimientoLicencia: '2026-12-20', estadoActivo: true }
    ]

    const alertas = buildAlertasMtc(unidades, conductores, refDate)
    expect(alertas).toHaveLength(6)
    expect(alertas[0].vencido).toBe(true)
    expect(alertas[0].severity).toBe('danger')
    expect(alertas[1].tipo).toBe('LICENCIA')
    expect(alertas[1].severity).toBe('warn')
  })

  it('calcula KPIs unificados diferenciando unidades disponibles y tractos disponibles', () => {
    const despachos = [
      { estadoTransporte: 'PENDIENTE_ASIGNACION' },
      { estadoTransporte: 'PROGRAMADO' },
      { estadoTransporte: 'EN_RUTA' },
      { estadoTransporte: 'ENTREGADO' }
    ]
    const unidades = [
      { tipoVehiculo: 'TRACTO', estadoOperativo: 'DISPONIBLE', estadoActivo: true },
      { tipoVehiculo: 'FURGON', estadoOperativo: 'DISPONIBLE', estadoActivo: true },
      { tipoVehiculo: 'TRACTO', estadoOperativo: 'EN_RUTA', estadoActivo: true },
      { tipoVehiculo: 'PLATAFORMA', estadoOperativo: 'MANTENIMIENTO', estadoActivo: true }
    ]
    const conductores = [
      { tipoLicencia: 'A-IIIc', estadoOperativo: 'DISPONIBLE', estadoActivo: true },
      { tipoLicencia: 'A-IIIb', estadoOperativo: 'EN_RUTA', estadoActivo: true }
    ]
    const kpis = buildTransporteKpis(despachos, unidades, conductores, [{ vencido: true }])

    expect(kpis.despachosEnRuta).toBe(1)
    expect(kpis.despachosPendientes).toBe(1)
    expect(kpis.unidadesDisponibles).toBe(2)
    expect(kpis.tractosDisponibles).toBe(1)
    expect(kpis.unidadesMantenimiento).toBe(1)
    expect(kpis.conductoresDisponibles).toBe(1)
    expect(kpis.alertasVencidas).toBe(1)
  })
})
