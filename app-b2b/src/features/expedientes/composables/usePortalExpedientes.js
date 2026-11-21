import { ref, computed, watch } from 'vue'
import { portalExpedienteService } from '../services/portalExpedienteService'
import { useNotification } from '@/shared/composables/useNotification'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'
import { isExportExpediente } from '../utils/expedienteTrackingUtils'

/**
 * Composable de gestión de estado, filtros y bóveda documental para Expedientes B2B.
 */
export function usePortalExpedientes() {
  const { showSuccess, showError } = useNotification()

  const expedientes = ref([])
  const loading = ref(true)

  const searchQuery = ref('')
  const filterRegimen = ref('TODOS')
  const filterCanal = ref('TODOS')
  const filterFase = ref('ABIERTOS')
  const sortBy = ref('recientes')

  const selectedExpediente = ref(null)
  const showDetailModal = ref(false)
  const documentosExpediente = ref([])
  const loadingDocs = ref(false)

  const abiertosCount = computed(() =>
    expedientes.value.filter(e => e.estadoExpediente !== 'CERRADO' && e.estadoExpediente !== 'ANULADO').length
  )

  const finalizadosCount = computed(() =>
    expedientes.value.filter(e => e.estadoExpediente === 'CERRADO').length
  )

  const todosCount = computed(() => expedientes.value.length)

  const hasActiveFilters = computed(() => {
    return Boolean(
      searchQuery.value ||
      filterFase.value !== 'ABIERTOS' ||
      filterRegimen.value !== 'TODOS' ||
      filterCanal.value !== 'TODOS' ||
      sortBy.value !== 'recientes'
    )
  })

  function resetFilters() {
    searchQuery.value = ''
    filterFase.value = 'ABIERTOS'
    filterRegimen.value = 'TODOS'
    filterCanal.value = 'TODOS'
    sortBy.value = 'recientes'
  }

  function matchesBaseFilters(exp) {
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase()
      const matchCode = (exp.codigoExpediente || '').toLowerCase().includes(q)
      const matchDam = (exp.numeroDam || '').toLowerCase().includes(q)
      const matchCargo = (exp.nombreMercancia || '').toLowerCase().includes(q)
      const matchCont = (exp.numeroContenedor || '').toLowerCase().includes(q)
      const matchNave = (exp.naveViaje || '').toLowerCase().includes(q)
      if (!matchCode && !matchDam && !matchCargo && !matchCont && !matchNave) {
        return false
      }
    }

    if (filterRegimen.value !== 'TODOS') {
      const isExp = isExportExpediente(exp)
      if (filterRegimen.value === 'IMPORTACION' && isExp) return false
      if (filterRegimen.value === 'EXPORTACION' && !isExp) return false
    }

    if (filterCanal.value !== 'TODOS') {
      const canal = (exp.canalSunat || 'PENDIENTE').toUpperCase()
      if (canal !== filterCanal.value) return false
    }

    return true
  }

  const expedientesFiltrados = computed(() => {
    const filtered = expedientes.value.filter(exp => {
      if (filterFase.value === 'ABIERTOS') {
        if (exp.estadoExpediente === 'CERRADO' || exp.estadoExpediente === 'ANULADO') return false
      } else if (filterFase.value === 'FINALIZADOS') {
        if (exp.estadoExpediente !== 'CERRADO') return false
      }
      return matchesBaseFilters(exp)
    })

    const list = [...filtered]
    switch (sortBy.value) {
      case 'dam-asc':
        return list.sort((a, b) => String(a.numeroDam || 'ZZZ').localeCompare(String(b.numeroDam || 'ZZZ')))
      case 'codigo-asc':
        return list.sort((a, b) => String(a.codigoExpediente || '').localeCompare(String(b.codigoExpediente || '')))
      default:
        return list.sort((a, b) => {
          const da = a.fechaApertura ? new Date(a.fechaApertura).getTime() : 0
          const db = b.fechaApertura ? new Date(b.fechaApertura).getTime() : 0
          return db - da || (Number(b.idExpediente) || 0) - (Number(a.idExpediente) || 0)
        })
    }
  })

  async function cargarExpedientes() {
    loading.value = true
    try {
      const data = await portalExpedienteService.listarExpedientes()
      expedientes.value = data || []
    } catch (err) {
      showError(err.response?.data?.message || 'Error al cargar los expedientes aduaneros')
    } finally {
      loading.value = false
    }
  }

  function abrirDetalle(exp) {
    selectedExpediente.value = exp
    showDetailModal.value = true
  }

  function cerrarDetalle() {
    showDetailModal.value = false
  }

  watch(
    () => selectedExpediente.value?.idExpediente,
    async (newId) => {
      if (newId) {
        await cargarDocumentos(newId)
      } else {
        documentosExpediente.value = []
      }
    }
  )

  async function cargarDocumentos(idExpediente) {
    loadingDocs.value = true
    try {
      const docs = await portalExpedienteService.listarDocumentosExpediente(idExpediente)
      documentosExpediente.value = docs || []
    } catch (err) {
      console.debug('Error al listar documentos del expediente:', err)
      documentosExpediente.value = []
    } finally {
      loadingDocs.value = false
    }
  }

  async function handleDescargar(doc) {
    try {
      const blob = await portalExpedienteService.descargarDocumento(doc.idDocumento)
      downloadBlob(blob, doc.nombreOriginal || `documento-${doc.idDocumento}.pdf`, doc.formatoMime || 'application/pdf')
      showSuccess(`Descargando ${doc.nombreOriginal || 'documento'}`, 'Bóveda Digital')
    } catch {
      showError('Error al descargar el documento del servidor')
    }
  }

  async function handlePrevisualizar(doc) {
    try {
      const blob = await portalExpedienteService.previsualizarDocumento(doc.idDocumento)
      openBlobInNewTab(blob, doc.formatoMime || 'application/pdf')
    } catch {
      showError('Error al previsualizar el documento en el navegador')
    }
  }

  return {
    expedientes,
    loading,
    searchQuery,
    filterRegimen,
    filterCanal,
    filterFase,
    sortBy,
    abiertosCount,
    finalizadosCount,
    todosCount,
    hasActiveFilters,
    expedientesFiltrados,
    selectedExpediente,
    showDetailModal,
    documentosExpediente,
    loadingDocs,
    resetFilters,
    cargarExpedientes,
    abrirDetalle,
    cerrarDetalle,
    handleDescargar,
    handlePrevisualizar
  }
}
