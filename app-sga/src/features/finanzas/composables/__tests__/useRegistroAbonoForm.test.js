import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ref } from 'vue'
import { useRegistroAbonoForm } from '../useRegistroAbonoForm'

vi.mock('@/features/auth/stores/authStore', () => ({
  useAuthStore: () => ({ user: { idUsuario: 5 } })
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: vi.fn(),
    showWarn: vi.fn(),
    showError: vi.fn()
  })
}))

vi.mock('../../services/finanzasService', () => ({
  default: {
    getCotizacionesAprobadas: vi.fn(async () => []),
    registrarPagoCotizacion: vi.fn(async (_id, payload) => ({ idPago: 99, ...payload }))
  }
}))

vi.mock('@/features/documentos/services/documentoService', () => ({
  documentoService: {
    upload: vi.fn(async () => [{ idDocumento: 77 }])
  }
}))

describe('useRegistroAbonoForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('precarga 30% de la cotización al inicializar y calcula cobertura proyectada', async () => {
    const cotizacion = ref({
      idCotizacion: 10,
      codigoCotizacion: 'COT-2026-0010',
      estado: 'APROBADA',
      totalCotizado: 1000,
      totalAbonado: 0,
      saldoPendiente: 1000
    })
    const cotizaciones = ref([cotizacion.value])

    const formApi = useRegistroAbonoForm({
      cotizacionProp: cotizacion,
      cotizacionesProp: cotizaciones
    })

    await formApi.init()

    expect(formApi.form.value.montoAbonado).toBe(300)
    expect(formApi.balance.value.coberturaProyectadaPct).toBe(30)
    expect(formApi.balance.value.cumpleUmbralApertura).toBe(true)

    formApi.setTipoAbono('PAGO_TOTAL')
    expect(formApi.form.value.montoAbonado).toBe(1000)
    expect(formApi.balance.value.saldoProyectado).toBe(0)
  })

  it('rechaza archivos que superen 10 MB', () => {
    const formApi = useRegistroAbonoForm({
      cotizacionProp: ref(null),
      cotizacionesProp: ref([])
    })

    formApi.processSelectedFile({ name: 'grande.pdf', size: 11 * 1024 * 1024 })
    expect(formApi.selectedFile.value).toBeNull()
    expect(formApi.errorMessage.value).toContain('10 MB')
  })
})
