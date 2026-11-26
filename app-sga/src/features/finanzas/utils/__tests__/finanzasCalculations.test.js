import { describe, it, expect } from 'vitest'
import {
  calculateLiquidacionTotals,
  convertAmountToUsd,
  convertAmountToPen,
  calculateCotizacionBalance,
  summarizeCotizacionesCartera
} from '../finanzasCalculations'

describe('finanzasCalculations', () => {
  it('calcula totales de liquidación aplicando SPOT 12% cuando total PEN >= 700', () => {
    const res = calculateLiquidacionTotals({
      totalTributosSunat: 1000,
      gastosAlmacenPuerto: 400,
      fleteTransporteLocal: 300,
      honorariosAgencia: 300,
      tipoCambio: 3.75,
      aplicaDetraccion: true,
      porcentajeDetraccion: 12
    })

    expect(res.subtotalImponible).toBe(1000)
    expect(res.igvMonto).toBe(180)
    expect(res.totalGeneralPen).toBe(2180)
    expect(res.totalGeneralUsd).toBe(581.33)
    expect(res.superaUmbralSpot).toBe(true)
    expect(res.montoDetraccion).toBe(141.6)
    expect(res.netoCuentaCorrientePen).toBe(2038.4)
  })

  it('omite detracción SPOT cuando el total general en PEN es menor a S/ 700.00', () => {
    const res = calculateLiquidacionTotals({
      totalTributosSunat: 100,
      gastosAlmacenPuerto: 100,
      fleteTransporteLocal: 100,
      honorariosAgencia: 200,
      tipoCambio: 3.75,
      aplicaDetraccion: true
    })

    // subtotal = 400, igv = 72, total = 572 < 700
    expect(res.totalGeneralPen).toBe(572)
    expect(res.superaUmbralSpot).toBe(false)
    expect(res.montoDetraccion).toBe(0)
    expect(res.netoCuentaCorrientePen).toBe(572)
  })

  it('convierte montos entre PEN y USD respetando el tipo de cambio', () => {
    expect(convertAmountToUsd(375, 'PEN', 3.75)).toBe(100)
    expect(convertAmountToUsd(250, 'USD', 3.75)).toBe(250)
    expect(convertAmountToPen(100, 'USD', 3.75)).toBe(375)
    expect(convertAmountToPen(150, 'PEN', 3.75)).toBe(150)
  })

  it('calcula balance proyectado y umbral del 30% de apertura', () => {
    const cot = { totalCotizado: 1000, totalAbonado: 100, saldoPendiente: 900 }
    const below = calculateCotizacionBalance(cot, 150, 'USD', 3.75)
    expect(below.coberturaProyectadaPct).toBe(25)
    expect(below.cumpleUmbralApertura).toBe(false)
    expect(below.saldoProyectado).toBe(750)

    const ready = calculateCotizacionBalance(cot, 750, 'PEN', 3.75) // 200 USD -> 300/1000 = 30%
    expect(ready.nuevoAbonoUsd).toBe(200)
    expect(ready.coberturaProyectadaPct).toBe(30)
    expect(ready.cumpleUmbralApertura).toBe(true)
    expect(ready.saldoProyectado).toBe(700)
  })

  it('resume métricas de cartera de cotizaciones aprobadas', () => {
    const summary = summarizeCotizacionesCartera([
      { totalCotizado: 1000, totalAbonado: 0, saldoPendiente: 1000, estadoPago: 'SIN_PAGO' },
      { totalCotizado: 2000, totalAbonado: 500, saldoPendiente: 1500, estadoPago: 'PENDIENTE_REVISION' },
      { totalCotizado: 1500, totalAbonado: 600, saldoPendiente: 900, estadoPago: 'ADELANTO_APROBADO', habilitadoParaExpediente: true },
      { totalCotizado: 500, totalAbonado: 500, saldoPendiente: 0, estadoPago: 'PAGADO_TOTAL', habilitadoParaExpediente: true }
    ])

    expect(summary.totalCotizadoUsd).toBe(5000)
    expect(summary.totalAbonadoUsd).toBe(1600)
    expect(summary.totalSaldoPendienteUsd).toBe(3400)
    expect(summary.sinAbonoCount).toBe(1)
    expect(summary.enRevisionCount).toBe(1)
    expect(summary.adelantoConformeCount).toBe(1)
    expect(summary.pagadoTotalCount).toBe(1)
    expect(summary.habilitadasCount).toBe(2)
    expect(summary.conSaldoPendienteCount).toBe(3)
  })
})
