import { ref, computed, unref, watch } from 'vue'

/**
 * Composable reutilizable para gestión reactiva de paginación en cliente.
 *
 * @param {import('vue').Ref<Array> | Array} items - Colección reactiva o arreglo a paginar.
 * @param {number} initialPageSize - Cantidad de elementos por página (default: 8).
 */
export function usePagination(items, initialPageSize = 8) {
  const currentPage = ref(1)
  const pageSize = ref(initialPageSize)

  const itemsList = computed(() => {
    return unref(items) || []
  })

  const totalItems = computed(() => itemsList.value.length)

  const totalPages = computed(() => {
    return Math.ceil(totalItems.value / pageSize.value) || 1
  })

  // Asegura que currentPage no quede fuera de rango si la lista se filtra o reduce
  watch(totalPages, (maxPages) => {
    if (currentPage.value > maxPages) {
      currentPage.value = maxPages
    }
    if (currentPage.value < 1) {
      currentPage.value = 1
    }
  })

  const paginatedItems = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return itemsList.value.slice(start, start + pageSize.value)
  })

  // Índices para visualización ("Mostrando X a Y de Z")
  const startIndex = computed(() => {
    if (totalItems.value === 0) return 0
    return (currentPage.value - 1) * pageSize.value + 1
  })

  const endIndex = computed(() => {
    return Math.min(currentPage.value * pageSize.value, totalItems.value)
  })

  const canPrev = computed(() => currentPage.value > 1)
  const canNext = computed(() => currentPage.value < totalPages.value)

  const prevPage = () => {
    if (canPrev.value) currentPage.value--
  }

  const nextPage = () => {
    if (canNext.value) currentPage.value++
  }

  const goToPage = (pageNumber) => {
    const page = Math.max(1, Math.min(pageNumber, totalPages.value))
    currentPage.value = page
  }

  const resetPage = () => {
    currentPage.value = 1
  }

  return {
    currentPage,
    pageSize,
    totalItems,
    totalPages,
    paginatedItems,
    startIndex,
    endIndex,
    canPrev,
    canNext,
    prevPage,
    nextPage,
    goToPage,
    resetPage
  }
}

export default usePagination
