import { ref, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'

export const useNotification = () => {
  const toast = useToast()
  const notify = (severity, summary, detail = '') =>
    toast.add({ severity, summary, detail, life: 2500 })
  return {
    success: (m, d) => notify('success', m, d),
    error: (m, d) => notify('error', m, d),
    warn: (m, d) => notify('warn', m, d),
    info: (m, d) => notify('info', m, d)
  }
}

export const useConfirmation = () => {
  const confirm = useConfirm()
  return {
    confirmAction: ({ message, header = 'Confirmar', accept }) =>
      confirm.require({ message, header, accept })
  }
}

export const usePagination = (itemsRef, pageSize = 10) => {
  const currentPage = ref(1)
  const totalItems = computed(() => (itemsRef?.value || []).length)
  const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize)))
  const startIndex = computed(() => (totalItems.value ? (currentPage.value - 1) * pageSize + 1 : 0))
  const endIndex = computed(() => Math.min(currentPage.value * pageSize, totalItems.value))
  const paginatedItems = computed(() => {
    const list = itemsRef?.value || []
    const start = (currentPage.value - 1) * pageSize
    return list.slice(start, start + pageSize)
  })
  return { currentPage, totalItems, totalPages, startIndex, endIndex, paginatedItems }
}

export const useCotizacionCalculator = () => ({
  calcularTotales: () => ({ subtotal: 1000, igv: 180, total: 1180 })
})

export const formatMoney = (v = 0) => `$ ${Number(v || 0).toFixed(2)}`
export const calcularValorCif = (fob = 0, flete = 0, seguro = 0) => Number(fob) + Number(flete) + Number(seguro)
export const calcularMontoAdValorem = (cif = 0, tasa = 0) => cif * (tasa / 100)
export const calcularMontoIgv = (base = 0) => base * 0.18
export const calcularMontoPercepcion = (base = 0, tasa = 3.5) => base * (tasa / 100)
export const calcularLiquidacionIntegral = () => ({ cif: 1000, adValorem: 60, igv: 190.8, percepcion: 43.78, total: 1294.58 })

export const useModalidadPuertos = () => ({
  puertosFiltrados: computed(() => [
    { idPuerto: 1, nombre: 'Puerto del Callao', codigo: 'PECLL' },
    { idPuerto: 2, nombre: 'Puerto de Paita', codigo: 'PEPAI' }
  ])
})

export const useCotizacionCorrection = () => ({
  loading: ref(false),
  saving: ref(false),
  form: ref({ regimenAduanero: 'IMPORTACION', items: [] }),
  guardarCorreccion: async () => {}
})
