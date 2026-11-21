import { describe, it, expect } from 'vitest'
import {
  calcularMontoPorcentaje,
  getPagoTagSeverity,
  getTipoObligacionMeta,
  validarRegistroPagoForm,
  validarSubsanacionPagoForm
} from '../pagoFormUtils'

describe('pagoFormUtils', () => {
  it('calcula montos proporcionales al 50% y 100% redondeados a 2 decimales', () => {
    expect(calcularMontoPorcentaje(1000, 50)).toBe(500)
    expect(calcularMontoPorcentaje(333.33, 50)).toBe(166.67)
    expect(calcularMontoPorcentaje(750.5, 100)).toBe(750.5)
    expect(calcularMontoPorcentaje(0, 50)).toBe(0)
  })

  it('asigna severidades y etiquetas correctas para estados de pago y tipo de obligación', () => {
    expect(getPagoTagSeverity('PAGADO_TOTAL')).toBe('success')
    expect(getPagoTagSeverity('ADELANTO_APROBADO')).toBe('success')
    expect(getPagoTagSeverity('PENDIENTE_REVISION')).toBe('info')
    expect(getPagoTagSeverity('OBSERVADO')).toBe('danger')
    expect(getPagoTagSeverity('SIN_PAGO')).toBe('warn')

    expect(getTipoObligacionMeta('COTIZACION')).toEqual({
      label: 'Anticipo Pre-Expediente',
      severity: 'info'
    })
    expect(getTipoObligacionMeta('EXPEDIENTE')).toEqual({
      label: 'Despacho Aduanero',
      severity: 'secondary'
    })
  })

  it('valida el formulario de registro de pago', () => {
    expect(
      validarRegistroPagoForm({
        montoAbonado: 0,
        saldoPendienteMax: 500,
        numeroOperacion: 'OP-1',
        bancoOrigen: 'BCP',
        archivoVoucher: { name: 'v.pdf' }
      }).valid
    ).toBe(false)

    expect(
      validarRegistroPagoForm({
        montoAbonado: 600,
        saldoPendienteMax: 500,
        numeroOperacion: 'OP-1',
        bancoOrigen: 'BCP',
        archivoVoucher: { name: 'v.pdf' }
      }).valid
    ).toBe(false)

    expect(
      validarRegistroPagoForm({
        montoAbonado: 500,
        saldoPendienteMax: 500,
        numeroOperacion: 'OP-1',
        bancoOrigen: 'BCP',
        archivoVoucher: null
      }).valid
    ).toBe(false)

    expect(
      validarRegistroPagoForm({
        montoAbonado: 500,
        saldoPendienteMax: 500,
        numeroOperacion: 'OP-123',
        bancoOrigen: 'BCP',
        archivoVoucher: { name: 'v.pdf' }
      })
    ).toEqual({ valid: true })
  })

  it('valida el formulario de subsanación de pago', () => {
    expect(
      validarSubsanacionPagoForm({
        montoAbonado: 300,
        numeroOperacion: 'OP-99',
        bancoOrigen: 'BBVA',
        observaciones: '',
        tieneComprobantePrevio: true,
        nuevoArchivo: null
      }).valid
    ).toBe(false)

    expect(
      validarSubsanacionPagoForm({
        montoAbonado: 300,
        numeroOperacion: 'OP-99',
        bancoOrigen: 'BBVA',
        observaciones: 'Se adjunta constancia legible',
        tieneComprobantePrevio: false,
        nuevoArchivo: null
      }).valid
    ).toBe(false)

    expect(
      validarSubsanacionPagoForm({
        montoAbonado: 300,
        numeroOperacion: 'OP-99',
        bancoOrigen: 'BBVA',
        observaciones: 'Se adjunta constancia legible',
        tieneComprobantePrevio: true,
        nuevoArchivo: null
      })
    ).toEqual({ valid: true })
  })
})
