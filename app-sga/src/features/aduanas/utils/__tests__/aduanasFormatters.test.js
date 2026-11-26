import { describe, it, expect } from 'vitest'
import {
  formatUsd,
  formatRegimenShort,
  formatRegimenFull,
  getRegimenCode,
  formatTipoDespacho,
  formatIsoDate,
  getEstadoCotizacionLabel,
  getEstadoCotizacionSeverity,
  getEstadoPagoLabel,
  getEstadoPagoSeverity,
  isCotizacionRevisable,
  isCotizacionObservable,
  getCanalSeverity,
  getExpedienteSeverity,
  getEstadoAuditoriaLabel,
  getEstadoAuditoriaSeverity
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

  it('resuelve severidades de canal SUNAT y estado de expediente', () => {
    expect(getCanalSeverity('VERDE')).toBe('success')
    expect(getCanalSeverity('NARANJA')).toBe('warn')
    expect(getCanalSeverity('ROJO')).toBe('danger')
    expect(getCanalSeverity('PENDIENTE')).toBe('contrast')

    expect(getExpedienteSeverity('LEVANTE_AUTORIZADO')).toBe('success')
    expect(getExpedienteSeverity('ANULADO')).toBe('danger')
    expect(getExpedienteSeverity('CON_CANAL')).toBe('warn')
    expect(getExpedienteSeverity('ASIGNADO')).toBe('info')
  })

  it('formatea regímenes completos, despacho, fechas y estados unificados de auditoría', () => {
    expect(formatRegimenFull('EXPORTACION_DEFINITIVA')).toBe('Exportación Definitiva (REG-40)')
    expect(getRegimenCode('EXPORTACION_DEFINITIVA')).toBe('REG-40')
    expect(getRegimenCode('ADMISION_TEMPORAL')).toBe('REG-20')
    expect(formatTipoDespacho('DESPACHO_EXCEPCIONAL')).toBe('DESPACHO EXCEPCIONAL')
    expect(formatIsoDate('2026-11-24T10:15:00')).toBe('2026-11-24')
    expect(formatIsoDate(null)).toBe('—')

    expect(getEstadoAuditoriaLabel('LEVANTE_AUTORIZADO')).toBe('Levante Autorizado')
    expect(getEstadoAuditoriaSeverity('APROBADA')).toBe('success')
    expect(getEstadoAuditoriaSeverity('CON_CANAL')).toBe('warn')
  })
})
