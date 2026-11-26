import { describe, it, expect } from 'vitest'
import {
  formatTipoAbono,
  getEstadoAnticipoLabel,
  getEstadoAnticipoSeverity,
  formatEstadoPagoCotizacionLabel,
  formatEstadoPagoCotizacionSeverity,
  formatMetodoPagoLabel
} from '../finanzasFormatters'

describe('finanzasFormatters', () => {
  it('formatea tipos de abono', () => {
    expect(formatTipoAbono('ADELANTO')).toBe('Adelanto / Anticipo')
    expect(formatTipoAbono('PAGO_TOTAL')).toBe('Pago Total (100%)')
    expect(formatTipoAbono('GARANTIA_CREDITO')).toBe('Línea de Crédito')
  })

  it('mapea etiquetas y severidades de estado de anticipo', () => {
    expect(getEstadoAnticipoLabel('PENDIENTE_REVISION')).toBe('Pendiente Revisión')
    expect(getEstadoAnticipoSeverity('PENDIENTE_REVISION')).toBe('warn')
    expect(getEstadoAnticipoLabel('APROBADO')).toBe('Aprobado / Conforme')
    expect(getEstadoAnticipoSeverity('APROBADO')).toBe('success')
    expect(getEstadoAnticipoLabel('RECHAZADO')).toBe('Rechazado')
    expect(getEstadoAnticipoSeverity('RECHAZADO')).toBe('danger')
  })

  it('mapea etiquetas y severidades de pago de cotización', () => {
    expect(formatEstadoPagoCotizacionLabel('PAGADO_TOTAL')).toBe('Cancelado 100%')
    expect(formatEstadoPagoCotizacionSeverity('PAGADO_TOTAL')).toBe('success')
    expect(formatEstadoPagoCotizacionLabel('SIN_PAGO')).toBe('Sin Abono')
    expect(formatEstadoPagoCotizacionSeverity('SIN_PAGO')).toBe('secondary')
  })

  it('formatea métodos de pago legibles', () => {
    expect(formatMetodoPagoLabel('TRANSFERENCIA_CCI')).toBe('Transferencia CCI')
    expect(formatMetodoPagoLabel('DEPOSITO_EN_VENTANILLA')).toBe('Depósito en Ventanilla')
  })
})
