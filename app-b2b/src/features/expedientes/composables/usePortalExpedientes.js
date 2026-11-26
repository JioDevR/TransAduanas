import { ref, computed } from 'vue'
import { portalExpedienteService } from '../services/portalExpedienteService'
import { portalCotizacionService } from '@/features/cotizaciones/services/portalCotizacionService'
import { useNotification } from '@/shared/composables/useNotification'
import { isExportExpediente } from '../utils/expedienteTrackingUtils'

/**
 * Composable de gestión de listado, filtros y creación de Expedientes Simples B2B.
 */
export function usePortalExpedientes() {
  const { showSuccess, showError } = useNotification()

  const expedientes = ref([])
  const cotizacionesElegibles = ref([])
  const loading = ref(true)
  const savingSimple = ref(false)

  const searchQuery = ref('')
  const filterRegimen = ref('TODOS')
  const filterCanal = ref('TODOS')
  const filterFase = ref('ABIERTOS')
  const sortBy = ref('recientes')

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
      const [data, cots] = await Promise.all([
        portalExpedienteService.listarExpedientes(),
        typeof portalCotizacionService.listarCotizaciones === 'function'
          ? portalCotizacionService.listarCotizaciones().catch(() => [])
          : Promise.resolve([])
      ])
      expedientes.value = data || []
      const idsConExp = new Set((data || []).map(e => e.idCotizacion).filter(Boolean))
      cotizacionesElegibles.value = (cots || []).filter(c =>
        c.estado === 'APROBADA' &&
        !c.tieneExpediente &&
        !idsConExp.has(c.idCotizacion) &&
        (c.estadoPago === 'ADELANTO_APROBADO' || c.estadoPago === 'PAGADO_TOTAL' || Number(c.totalAbonado || 0) > 0)
      )
    } catch (err) {
      showError(err.response?.data?.message || 'Error al cargar los expedientes aduaneros')
    } finally {
      loading.value = false
    }
  }

  async function guardarExpedienteSimple({ isEdit, idExpediente, payload }) {
    savingSimple.value = true
    try {
      let result
      if (isEdit && idExpediente) {
        result = await portalExpedienteService.actualizarDatosCliente(idExpediente, payload)
        showSuccess('Datos del expediente actualizados correctamente.', 'Expediente Actualizado')
      } else {
        result = await portalExpedienteService.crearExpedienteSimple(payload)
        showSuccess(`Expediente simple ${result?.codigoExpediente || ''} creado. Gestión Aduanera asignará su agente.`, 'Expediente Creado')
      }
      await cargarExpedientes()
      return result
    } catch (err) {
      showError(err.response?.data?.message || 'No se pudo guardar el expediente.')
      return null
    } finally {
      savingSimple.value = false
    }
  }

  return {
    expedientes,
    cotizacionesElegibles,
    loading,
    savingSimple,
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
    resetFilters,
    cargarExpedientes,
    guardarExpedienteSimple
  }
}
