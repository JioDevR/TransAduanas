import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import CobrosPagosView from '../CobrosPagosView.vue'
import ObservadosBandejaPanel from '../../components/ObservadosBandejaPanel.vue'
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

describe('CobrosPagosView Component - Estándar sgaUI', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renderiza las 3 secciones y SgaListShell sin widgets KPI duplicados', async () => {
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
    app.use(PrimeVue)

    const html = await renderToString(app)
    expect(html).toContain('Cuentas por Pagar (0)')
    expect(html).toContain('Historial de Comprobantes (0)')
    expect(html).toContain('Registrar Comprobante ($ USD)')
    expect(html).toContain('Canvas')
    expect(html).toContain('Tabla')
    expect(html).not.toContain('TOTAL CONTRATADO / COTIZADO')
  })

  it('ObservadosBandejaPanel renderiza comprobantes observados y motivo contable', async () => {
    const app = createSSRApp({
      render() {
        return h(ObservadosBandejaPanel, {
          anticipos: [
            {
              idPago: 10,
              codigoPago: 'PAG-COT-2026-0003',
              codigoCotizacion: 'COT-2026-0005',
              montoAbonado: 4000,
              bancoOrigen: 'BCP',
              bancoDestino: 'BCP Dolares TransAduanas',
              numeroOperacion: '001928374',
              fechaOperacion: '2026-11-10',
              observacionFinanzas: 'El voucher no es legible'
            }
          ]
        })
      }
    })
    app.use(PrimeVue)

    const html = await renderToString(app)
    expect(html).toContain('Requieren Corrección')
    expect(html).toContain('PAG-COT-2026-0003')
    expect(html).toContain('El voucher no es legible')
    expect(html).toContain('Subsanar Abono')
  })
})
