import { describe, it, expect } from 'vitest'
import {
  formatUsd,
  formatRegimenShort,
  getEstadoCotizacionLabel,
  getEstadoCotizacionSeverity,
  getEstadoPagoLabel,
  getEstadoPagoSeverity,
  isCotizacionRevisable,
  isCotizacionObservable
} from '../aduanasFormatters'

describe('aduanasFormatters', () => {
  it('formatea montos USD y regímenes cortos', () => {
    expect(formatUsd(1250.5)).toBe('1,250.50')
    expect(formatRegimenShort('IMPORTACION_DEFINITIVA')).toBe('Importación (REG-10)')
    expect(formatRegimenShort('EXPORTACION_DEFINITIVA')).toBe('Exportación (REG-40)')
  })

  it('resuelve etiquetas y severidades de estado de cotización', () => {
    expect(getEstadoCotizacionLabel('ENVIADA')).toBe('Por Revisar (Pendiente)')
    expect(getEstadoCotizacionLabel('APROBADA')).toBe('Aprobada')
    expect(getEstadoCotizacionSeverity('APROBADA')).toBe('success')
    expect(getEstadoCotizacionSeverity('RECHAZADA')).toBe('danger')
    expect(getEstadoCotizacionSeverity('OBSERVADA')).toBe('warn')
  })

  it('resuelve etiquetas y severidades de estado de pago', () => {
    expect(getEstadoPagoLabel('ADELANTO_APROBADO')).toBe('Anticipo Aprobado')
    expect(getEstadoPagoLabel('PAGADO_TOTAL')).toBe('Pagado 100%')
    expect(getEstadoPagoLabel('DESCONOCIDO')).toBe('Pendiente de Pago')
    expect(getEstadoPagoSeverity('PAGADO_TOTAL')).toBe('success')
    expect(getEstadoPagoSeverity('OBSERVADO')).toBe('danger')
  })

  it('evalúa reglas de revisión y observación según estado', () => {
    expect(isCotizacionRevisable('PENDIENTE')).toBe(true)
    expect(isCotizacionRevisable('OBSERVADA')).toBe(true)
    expect(isCotizacionRevisable('APROBADA')).toBe(false)

    expect(isCotizacionObservable('PENDIENTE')).toBe(true)
    expect(isCotizacionObservable('OBSERVADA')).toBe(false)
  })
})
