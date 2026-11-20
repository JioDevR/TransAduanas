import { ref, computed } from 'vue'

/**
 * Composable para la bandeja de Apertura de Expedientes en Gestión Aduanera.
 * Reutiliza la cola `listasApertura` ya clasificada por `useAduanasCotizaciones`
 * para evitar peticiones redundantes al backend.
 */
export function useAduanasApertura({ cotizacionesListas }) {
  const searchQuery = ref('')
  const filterRegimen = ref('ALL')
  const sortBy = ref('RECENT')

  const hasActiveFilters = computed(() => {
    return Boolean(
      searchQuery.value.trim() ||
      filterRegimen.value !== 'ALL' ||
      sortBy.value !== 'RECENT'
    )
  })

  const resetFilters = () => {
    searchQuery.value = ''
    filterRegimen.value = 'ALL'
    sortBy.value = 'RECENT'
  }

  const filteredApertura = computed(() => {
    let list = [...(cotizacionesListas.value || [])]

    if (filterRegimen.value && filterRegimen.value !== 'ALL') {
      list = list.filter((c) => c.regimenAduanero === filterRegimen.value)
    }

    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      list = list.filter((c) => {
        return (
          c.codigoCotizacion?.toLowerCase().includes(q) ||
          c.clienteRazonSocial?.toLowerCase().includes(q) ||
          c.clienteNumeroDocumento?.toLowerCase().includes(q) ||
          c.puertoEnvioNombre?.toLowerCase().includes(q) ||
          c.puertoDestinoNombre?.toLowerCase().includes(q)
        )
      })
    }

    list.sort((a, b) => {
      switch (sortBy.value) {
        case 'OLDEST':
          return (a.idCotizacion || 0) - (b.idCotizacion || 0)
        case 'AMOUNT_DESC':
          return Number(b.totalCotizado || 0) - Number(a.totalCotizado || 0)
        case 'AMOUNT_ASC':
          return Number(a.totalCotizado || 0) - Number(b.totalCotizado || 0)
        case 'CODE_ASC':
          return String(a.codigoCotizacion || '').localeCompare(String(b.codigoCotizacion || ''))
        case 'RECENT':
        default:
          return (b.idCotizacion || 0) - (a.idCotizacion || 0)
      }
    })

    return list
  })

  return {
    searchQuery,
    filterRegimen,
    sortBy,
    hasActiveFilters,
    resetFilters,
    filteredApertura
  }
}
