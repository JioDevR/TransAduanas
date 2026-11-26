import { ref, reactive, computed, watch } from 'vue'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { openBlobInNewTab } from '@/shared/utils/download'
import { unwrapListPayload } from '@/features/gerencia/utils/gerenciaMetricsUtils'

/**
 * Composable CRUD genérico para las vistas maestras de Gerencia General
 * (Sedes, Puertos, Tipos de Carga, Mercancías). Evita duplicación de carga,
 * filtros, toggle de estado, exportación PDF y cierre de formularios vía navTick.
 */
export function useCatalogoList(options) {
  const {
    entityLabel = 'Registro',
    getId,
    getCode,
    getName = (item) => item?.nombre || '',
    activeStatus = 'ACTIVO',
    loadFn,
    deleteFn,
    reactivateFn,
    pdfFn,
    initialFilters = { search: '', sortBy: 'DEFAULT' },
    filterItem = () => true,
    sortItems = (list) => list,
    messages = {}
  } = options

  const navigationStore = useNavigationStore()
  const { showSuccess, showError } = useNotification()
  const { confirmAction } = useConfirmation()

  const items = ref([])
  const loading = ref(false)
  const exportingPdf = ref(false)
  const showForm = ref(false)
  const editingItem = ref(null)
  const viewMode = ref('table')

  const defaults = { ...initialFilters }
  const filters = reactive({ ...initialFilters })

  const filterKeys = Object.keys(defaults).filter((k) => k !== 'sortBy')

  const hasActiveFilters = computed(() =>
    filterKeys.some((k) => filters[k] !== defaults[k] && filters[k] !== '' && filters[k] !== null)
  )

  const filteredItems = computed(() => {
    const filtered = items.value.filter((item) => filterItem(item, filters))
    return sortItems(filtered, filters.sortBy)
  })

  const toggleFilter = (key, value) => {
    filters[key] = filters[key] === value ? defaults[key] : value
  }

  const resetFilters = () => {
    for (const k of Object.keys(defaults)) {
      filters[k] = defaults[k]
    }
  }

  const loadItems = async () => {
    loading.value = true
    try {
      const res = await loadFn()
      items.value = unwrapListPayload(res)
    } catch (err) {
      console.error(`Error cargando ${entityLabel}:`, err)
      showError(messages.loadError || `Error al conectar con el catálogo de ${entityLabel.toLowerCase()}`)
    } finally {
      loading.value = false
    }
  }

  const openCreate = () => {
    editingItem.value = null
    showForm.value = true
  }

  const openEdit = (item) => {
    editingItem.value = { ...item }
    showForm.value = true
  }

  const closeForm = () => {
    showForm.value = false
    editingItem.value = null
  }

  const onSaved = async (payload = {}) => {
    closeForm()
    const isEdit = Boolean(payload?.isEdit)
    showSuccess(
      isEdit
        ? (messages.updated || `${entityLabel} actualizado exitosamente`)
        : (messages.created || `${entityLabel} registrado exitosamente`)
    )
    await loadItems()
  }

  const toggleEstado = async (item) => {
    const id = getId(item)
    const code = getCode(item)
    const name = getName(item)
    const isActive = item?.estado === activeStatus

    if (isActive) {
      const confirmed = await confirmAction({
        header: `Confirmar Desactivación de ${entityLabel}`,
        message: `¿Desea desactivar ${code}${name ? ` — ${name}` : ''}?`,
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Desactivar',
        acceptClass: 'p-button-danger'
      })
      if (!confirmed) return
    }

    try {
      if (isActive) {
        await deleteFn(id)
        showSuccess(messages.deactivated?.(code) || `${entityLabel} ${code} desactivado`)
      } else {
        await reactivateFn(id)
        showSuccess(messages.reactivated?.(code) || `${entityLabel} ${code} reactivado exitosamente`)
      }
      await loadItems()
    } catch (err) {
      console.error(`Error cambiando estado de ${entityLabel}:`, err)
      showError(err?.response?.data?.message || `No se pudo actualizar el estado de ${code}`)
    }
  }

  const exportPdf = async () => {
    if (!pdfFn) return
    exportingPdf.value = true
    try {
      const blob = await pdfFn()
      openBlobInNewTab(blob)
      showSuccess(messages.pdfSuccess || `Reporte oficial de ${entityLabel.toLowerCase()} abierto en nueva pestaña`)
    } catch (err) {
      console.error(`Error generando PDF de ${entityLabel}:`, err)
      showError(messages.pdfError || `No se pudo generar el reporte oficial de ${entityLabel.toLowerCase()}`)
    } finally {
      exportingPdf.value = false
    }
  }

  watch(
    () => [navigationStore.gerenciaGeneralSubOption, navigationStore.navTick],
    () => {
      if (showForm.value) closeForm()
    }
  )

  return {
    items,
    loading,
    exportingPdf,
    showForm,
    editingItem,
    viewMode,
    filters,
    hasActiveFilters,
    filteredItems,
    toggleFilter,
    resetFilters,
    loadItems,
    openCreate,
    openEdit,
    closeForm,
    onSaved,
    toggleEstado,
    exportPdf
  }
}
