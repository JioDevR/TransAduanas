import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useLiquidacionForm } from '../useLiquidacionForm'

const mocks = vi.hoisted(() => ({
  calcularEstimado: vi.fn(),
  emitirLiquidacion: vi.fn(),
  updateLiquidacion: vi.fn(),
  registrarPago: vi.fn(),
  anularLiquidacion: vi.fn(),
  showSuccess: vi.fn(),
  showError: vi.fn(),
  confirmAction: vi.fn(async () => true)
}))

vi.mock('@/features/auth/stores/authStore', () => ({
  useAuthStore: () => ({ user: { idUsuario: 2 } })
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: mocks.showSuccess,
    showWarn: vi.fn(),
    showError: mocks.showError
  })
}))

vi.mock('@/shared/composables/useConfirmation', () => ({
  useConfirmation: () => ({
    confirmAction: mocks.confirmAction
  })
}))

vi.mock('@/features/documentos/services/documentoService', () => ({
  documentoService: { upload: vi.fn(async () => []) }
}))

vi.mock('../../services/finanzasService', () => ({
  default: {
    calcularEstimado: mocks.calcularEstimado,
    emitirLiquidacion: mocks.emitirLiquidacion,
    updateLiquidacion: mocks.updateLiquidacion,
    registrarPago: mocks.registrarPago,
    anularLiquidacion: mocks.anularLiquidacion
  }
}))

describe('useLiquidacionForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('precarga estimado al abrir emisión para un expediente y calcula totales con umbral SPOT', async () => {
    mocks.calcularEstimado.mockResolvedValue({
      totalTributosSunatEstimado: 1200,
      gastosAlmacenPuertoEstimado: 450,
      fleteTransporteLocalEstimado: 300,
      honorariosAgenciaEstimado: 250,
      tipoCambioSugerido: 3.75,
      aplicaDetraccion: true
    })

    const onMutated = vi.fn()
    const api = useLiquidacionForm({ onMutated })
    await api.openEmitirModal({ idExpediente: 44 })

    expect(api.showFormModal.value).toBe(true)
    expect(api.isEditMode.value).toBe(false)
    expect(api.totals.value.subtotalImponible).toBe(1000)
    expect(api.totals.value.igvMonto).toBe(180)
    expect(api.totals.value.totalGeneralPen).toBe(2380)
    expect(api.totals.value.montoDetraccion).toBe(141.6)

    mocks.emitirLiquidacion.mockResolvedValue({ codigoLiquidacion: 'LIQ-2026-0044' })
    await api.submitLiquidacion()

    expect(mocks.emitirLiquidacion).toHaveBeenCalledTimes(1)
    expect(mocks.showSuccess).toHaveBeenCalled()
    expect(onMutated).toHaveBeenCalledTimes(1)
  })

  it('permite editar una liquidación pendiente invocando updateLiquidacion', async () => {
    const onMutated = vi.fn()
    const api = useLiquidacionForm({ onMutated })

    api.openEditarModal({
      idLiquidacion: 8,
      idExpediente: 12,
      totalTributosSunat: 500,
      gastosAlmacenPuerto: 200,
      fleteTransporteLocal: 100,
      honorariosAgencia: 300,
      tipoCambio: 3.75,
      aplicaDetraccion: true,
      porcentajeDetraccion: 12,
      numeroFactura: 'F001-123'
    })

    expect(api.isEditMode.value).toBe(true)
    mocks.updateLiquidacion.mockResolvedValue({ codigoLiquidacion: 'LIQ-2026-0008' })
    await api.submitLiquidacion()

    expect(mocks.updateLiquidacion).toHaveBeenCalledWith(8, expect.objectContaining({ idExpediente: 12 }))
    expect(onMutated).toHaveBeenCalledTimes(1)
  })
})
