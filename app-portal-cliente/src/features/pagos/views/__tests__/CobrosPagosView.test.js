import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import CobrosPagosView from '../CobrosPagosView.vue'
import { portalPagoService } from '../../services/portalPagoService'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  }),
  useRoute: () => ({
    query: {}
  })
}))

vi.mock('../../services/portalPagoService', () => ({
  portalPagoService: {
    obtenerResumen: vi.fn(),
    listarObligaciones: vi.fn(),
    listarAnticipos: vi.fn(),
    previsualizarComprobante: vi.fn()
  }
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showWarn: vi.fn()
  })
}))

describe('CobrosPagosView Component - Reorganización Opción 1', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renderiza correctamente las 3 pestañas reorganizadas (Cuentas por Pagar, Requieren Corrección, Historial)', async () => {
    portalPagoService.obtenerResumen.mockResolvedValue({
      totalCotizadoUsd: 25000,
      totalAbonadoUsd: 10000,
      saldoPendienteUsd: 15000,
      pagosPendientesRevision: 1
    })

    portalPagoService.listarObligaciones.mockResolvedValue([
      {
        idReferencia: 1,
        tipoObligacion: 'COTIZACION',
        codigo: 'COT-2026-0005',
        descripcion: 'Carga de Maquinaria',
        totalContratadoUsd: 12000,
        totalAmortizadoUsd: 4000,
        saldoPendienteUsd: 8000,
        estadoPago: 'PENDIENTE'
      },
      {
        idReferencia: 2,
        tipoObligacion: 'EXPEDIENTE',
        codigo: 'EXP-2026-0002',
        descripcion: 'Despacho Aduanero DAM',
        numeroDam: '118-2026-10-001234',
        canalSunat: 'VERDE',
        totalContratadoUsd: 13000,
        totalAmortizadoUsd: 6000,
        saldoPendienteUsd: 7000,
        estadoPago: 'PAGADO_PARCIAL'
      }
    ])

    portalPagoService.listarAnticipos.mockResolvedValue([
      {
        idPago: 10,
        codigoPago: 'PAG-COT-2026-0003',
        codigoCotizacion: 'COT-2026-0005',
        tipoAbono: 'ANTICIPO_50',
        montoAbonado: 4000,
        bancoOrigen: 'BCP',
        bancoDestino: 'BCP Dolares TransAduanas',
        numeroOperacion: '001928374',
        fechaOperacion: '2026-11-10',
        estadoPago: 'OBSERVADO',
        observacionFinanzas: 'El voucher no es legible'
      }
    ])

    const app = createSSRApp({
      render() {
        return h(CobrosPagosView)
      }
    })

    const html = await renderToString(app)
    expect(html).toContain('Cuentas por Pagar (Saldos)')
    expect(html).toContain('Requieren Corrección')
    expect(html).toContain('Historial de Comprobantes')
    expect(html).toContain('Anticipos de Cotización')
    expect(html).toContain('Saldos de Despacho (DAM)')
  })
})
