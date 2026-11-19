<template>
  <div class="ventas-view">

    <!-- Vista Completa: Corrección Unificada de Cotización Observada -->
    <CotizacionCorrectionView
      v-if="showCorrectionView"
      :cotizacion="selectedCotizacion"
      :clientes="clientes"
      @cancel="closeAllForms"
      @saved="handleCotizacionSaved"
    />

    <!-- Vista Completa: Formulario Maestro de Cotización Comercial (en Pasos) -->
    <CotizacionRegistrationForm
      v-else-if="showCotizacionForm"
      :cotizacion-to-edit="selectedCotizacion"
      :clientes="clientes"
      @cancel="closeAllForms"
      @saved="handleCotizacionSaved"
      @client-created="handleQuickClientCreated"
    />

    <!-- Vista Completa: Formulario Maestro de Cliente / Prospecto -->
    <ClienteRegistrationForm
      v-else-if="showClienteForm"
      :cliente-to-edit="selectedCliente"
      @cancel="closeAllForms"
      @saved="handleClienteSaved"
    />

    <!-- Vista Principal: Tablero Comercial y Listados -->
    <div v-else class="ventas-master-container">

      <!-- ======================================================== -->
      <!-- SUB-OPCIÓN 01: DASHBOARD COMERCIAL DE VENTAS             -->
      <!-- ======================================================== -->
      <ComercialDashboardPanel
        v-if="activeTab === 'dashboard'"
        :cotizaciones="cotizaciones"
        :clientes="clientes"
        :solicitudes-sin-asignar="solicitudesSinAsignar"
        :loading="loading"
        :is-agente-comercial="isAgenteComercial"
        @navigate="navigateTo"
        @refresh="loadData"
      />

      <!-- ======================================================== -->
      <!-- SUB-OPCIÓN 02: REGISTRO DE COTIZACIONES COMERCIALES      -->
      <!-- ======================================================== -->
      <!-- ======================================================== -->
      <!-- SUB-OPCIÓN 02: REGISTRO DE COTIZACIONES COMERCIALES      -->
      <!-- ======================================================== -->
      <CotizacionesTablePanel
        v-if="activeTab === 'cotizaciones'"
        class="fade-in"
        :cotizaciones="filteredCotizaciones"
        :loading="loading"
        :is-agente-comercial="isAgenteComercial"
        v-model:search-cotizacion="searchCotizacion"
        v-model:filter-regimen-cotizacion="filterRegimenCotizacion"
        v-model:filter-estado-cotizacion="filterEstadoCotizacion"
        v-model:filter-asesor-cotizacion="filterAsesorCotizacion"
        v-model:sort-by-cotizacion="sortByCotizacion"
        :has-active-filters="hasActiveCotizacionFilters"
        :asesores-disponibles="asesoresDisponibles"
        :get-cotizacion-label="getCotizacionLabel"
        :get-cotizacion-severity="getCotizacionSeverity"
        :get-estado-pago-label="getEstadoPagoLabel"
        :get-cliente-telefono="getClienteTelefono"
        @reset-filters="resetCotizacionFilters"
        @refresh="loadData"
        @create="openCreateCotizacion"
        @edit="openEditCotizacion"
        @correct="openCorrectionCotizacion"
        @open-docs="openDocumentosCotizacion"
        @view-observation="openViewObservation"
        @notify-whatsapp="notifyObservationViaWhatsApp"
        @delete="handleDeleteCotizacion"
      />

    <!-- TAB 2: CLIENTES & PROSPECTOS -->
    <ClientesDirectoryPanel
      v-if="activeTab === 'clientes'"
      class="fade-in"
      :clientes="filteredClientes"
      :loading="loading"
      v-model:search-cliente="searchCliente"
      v-model:filter-tipo-cliente="filterTipoCliente"
      v-model:filter-estado-cliente="filterEstadoCliente"
      v-model:sort-by-cliente="sortByCliente"
      :has-active-filters="hasActiveClienteFilters"
      :cliente-tiene-usuario-portal="clienteTieneUsuarioPortal"
      @reset-filters="resetClienteFilters"
      @refresh="loadData"
      @create="openCreateCliente"
      @edit="openEditCliente"
      @delete="handleDeleteCliente"
    />

    <!-- TAB 3: SOLICITUDES SIN ASIGNAR (PORTAL B2B) -->
    <SolicitudesSinAsignarPanel
      v-if="activeTab === 'solicitudes'"
      class="fade-in"
      :solicitudes="filteredSolicitudes"
      :total-count="solicitudesSinAsignar.length"
      :loading="loading"
      v-model:search-solicitud="searchSolicitud"
      v-model:filter-regimen-solicitud="filterRegimenSolicitud"
      v-model:sort-by-solicitud="sortBySolicitud"
      :has-active-filters="hasActiveSolicitudFilters"
      :get-cotizacion-label="getCotizacionLabel"
      :get-cotizacion-severity="getCotizacionSeverity"
      @reset-filters="resetSolicitudFilters"
      @refresh="loadData"
      @open-docs="openDocumentosCotizacion"
      @take="openEditCotizacion"
    />

    <!-- Modal: Detalle de Observación de Supervisión Aduanera -->
    <Dialog
      v-model:visible="showObservationDialog"
      modal
      header="Observaciones de Supervisión Aduanera"
      :style="{ width: '500px', maxWidth: '95vw' }"
      class="custom-sga-dialog"
    >
      <div v-if="selectedObservationCotizacion" class="observation-view-content">
        <div class="obs-box-header">
          <span class="badge-kicker">EXPEDIENTE N° {{ selectedObservationCotizacion.codigoCotizacion }}</span>
          <strong class="obs-box-title">{{ selectedObservationCotizacion.clienteRazonSocial || 'Cliente Asignado' }}</strong>
          <div class="obs-client-status">
            <span v-if="selectedObservationCotizacion.clienteTieneUsuario" class="portal-badge origin-badge origin-portal">
              <i class="pi pi-id-card"></i> Cuenta B2B Activa — Notificación enviada al portal del cliente
            </span>
            <span v-else class="presencial-badge origin-badge origin-presencial">
              <i class="pi pi-user"></i> Prospecto sin cuenta B2B — Coordinación presencial o directa con el vendedor
            </span>
          </div>
        </div>
        <div class="obs-box-body">
          <i class="pi pi-exclamation-triangle obs-box-icon"></i>
          <div>
            <span class="obs-box-label">Indicaciones de Supervisión Aduanera:</span>
            <p class="obs-box-text">{{ selectedObservationCotizacion.motivoObservacion || 'Se requieren ajustes en la cotización antes de la aprobación.' }}</p>
          </div>
        </div>
        <p class="obs-box-hint">
          Subsanar los puntos observados haciendo clic en "Corregir Cotización". Al guardar los cambios, la propuesta volverá automáticamente a la bandeja de Supervisión Aduanera para su reevaluación.
        </p>
      </div>
      <template #footer>
        <div class="dialog-footer-actions">
          <button
            v-if="selectedObservationCotizacion && !selectedObservationCotizacion.clienteTieneUsuario && (selectedObservationCotizacion.clienteTelefono || getClienteTelefono(selectedObservationCotizacion.idCliente))"
            type="button"
            class="btn-whatsapp-dialog btn-whatsapp-modal"
            @click="notifyObservationViaWhatsApp(selectedObservationCotizacion)"
          >
            <i class="pi pi-whatsapp"></i>
            <span>Notificar WhatsApp</span>
          </button>
          <button type="button" class="btn-outline-warm" @click="showObservationDialog = false">
            <span>Cerrar</span>
          </button>
          <button
            type="button"
            class="btn-espresso"
            @click="openEditFromObservation"
          >
            <i class="pi pi-pencil"></i>
            <span>Corregir Cotización</span>
          </button>
        </div>
      </template>
    </Dialog>

    <!-- Modal: Gestión Integral de Documentos Adjuntos -->
    <DocumentosManagerModal
      :visible="showDocumentosModal"
      :entidad-tipo="'COTIZACION'"
      :id-entidad="selectedDocCotizacionId"
      :entidad-id="selectedDocCotizacionId"
      :codigo-entidad="selectedDocCotizacionCodigo"
      :titulo-entidad="selectedDocCotizacionTitulo"
      @close="showDocumentosModal = false"
    />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import { comercialService } from '../services/comercialService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { formatRegimen } from '@/shared/utils/formatters'
import ClienteRegistrationForm from '../components/ClienteRegistrationForm.vue'
import CotizacionRegistrationForm from '../components/CotizacionRegistrationForm.vue'
import CotizacionCorrectionView from '../components/CotizacionCorrectionView.vue'
import CotizacionesTablePanel from '../components/CotizacionesTablePanel.vue'
import ComercialDashboardPanel from '../components/ComercialDashboardPanel.vue'
import SolicitudesSinAsignarPanel from '../components/SolicitudesSinAsignarPanel.vue'
import ClientesDirectoryPanel from '../components/ClientesDirectoryPanel.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isAgenteComercial = computed(() => authStore.userRole === 'AGENTE_COMERCIAL')
const navigationStore = useNavigationStore()
const { showSuccess, showError, showInfo, showWarn } = useNotification()
const { confirmDelete } = useConfirmation()

const activeTab = ref('dashboard')
const loading = ref(false)

const showClienteForm = ref(false)
const selectedCliente = ref(null)

const showCotizacionForm = ref(false)
const showCorrectionView = ref(false)
const selectedCotizacion = ref(null)

const navigateTo = (subOption) => {
  closeAllForms()
  activeTab.value = subOption
  navigationStore.setComercialSubOption(subOption)
  if (route.query.tab !== subOption) {
    router.replace({ query: { ...route.query, tab: subOption } })
  }
}

const getEstadoPagoLabel = (st) => {
  switch (st) {
    case 'ADELANTO_APROBADO': return 'Anticipo Aprobado'
    case 'PAGADO_TOTAL': return 'Pagado 100%'
    case 'PENDIENTE_REVISION': return 'En Revisión Finanzas'
    case 'OBSERVADO': return 'Pago Observado'
    default: return 'Pendiente de Pago'
  }
}

const getEstadoPagoSeverity = (st) => {
  switch (st) {
    case 'ADELANTO_APROBADO':
    case 'PAGADO_TOTAL':
      return 'success'
    case 'PENDIENTE_REVISION':
      return 'warn'
    case 'OBSERVADO':
      return 'danger'
    default:
      return 'info'
  }
}

const showObservationDialog = ref(false)
const selectedObservationCotizacion = ref(null)

// Modal de Gestión de Documentos
const showDocumentosModal = ref(false)
const selectedDocCotizacionId = ref(null)
const selectedDocCotizacionCodigo = ref('')
const selectedDocCotizacionTitulo = ref('')

const openDocumentosCotizacion = (cotizacion) => {
  if (!cotizacion) return
  selectedDocCotizacionId.value = cotizacion.idCotizacion
  selectedDocCotizacionCodigo.value = cotizacion.codigoCotizacion || `COT-${cotizacion.idCotizacion}`
  selectedDocCotizacionTitulo.value = `${cotizacion.codigoCotizacion || 'Cotización'} — ${cotizacion.clienteRazonSocial || 'Cliente'}`
  showDocumentosModal.value = true
}

const openViewObservation = (cotizacion) => {
  selectedObservationCotizacion.value = cotizacion
  showObservationDialog.value = true
}

const openEditFromObservation = () => {
  if (selectedObservationCotizacion.value) {
    const c = selectedObservationCotizacion.value
    showObservationDialog.value = false
    openCorrectionCotizacion(c)
  }
}

const clienteTieneUsuarioPortal = (cl) => {
  if (!cl) return false
  if (typeof cl.tieneUsuario === 'boolean') return cl.tieneUsuario
  return cotizaciones.value.some(c => c.idCliente === cl.idCliente && c.clienteTieneUsuario)
}

const getClienteTelefono = (idCliente) => {
  if (!idCliente || !clientes.value) return ''
  const cli = clientes.value.find(item => item.idCliente === idCliente)
  return cli?.telefonoContacto || ''
}

const notifyObservationViaWhatsApp = (cotizacion) => {
  const tel = cotizacion.clienteTelefono || getClienteTelefono(cotizacion.idCliente) || ''
  const digitsOnly = tel.replace(/[^0-9]/g, '')
  if (!digitsOnly) {
    showToast('El cliente no tiene registrado número de teléfono/celular de contacto.', 'warn')
    return
  }
  const phone = digitsOnly.startsWith('51') ? digitsOnly : `51${digitsOnly}`
  const reason = cotizacion.motivoObservacion || 'Se requieren ajustes documentarios y técnicos en la propuesta comercial.'
  const msg = `Hola *${cotizacion.clienteRazonSocial || 'Estimado Cliente'}*, le saluda TransAduanas SGA.\n\nLe informamos que su cotización aduanera *${cotizacion.codigoCotizacion}* ha sido revisada por Supervisión Técnica y presenta la siguiente observación:\n\n📌 *Motivo:* ${reason}\n\nPor favor facilítenos los datos o documentos solicitados para subsanar la cotización y continuar con su trámite.\n\nAtentamente,\n*Área Comercial - TransAduanas*`

  const url = `https://wa.me/${phone}?text=${encodeURIComponent(msg)}`
  window.open(url, '_blank')
}

// Cerrar todos los formularios de vista completa y volver a los listados
const closeAllForms = () => {
  showCorrectionView.value = false
  showCotizacionForm.value = false
  showClienteForm.value = false
  selectedCotizacion.value = null
  selectedCliente.value = null
}

// Sincronizar activeTab y resetear formularios con query tab de la URL
watch(
  () => route.query.tab,
  (newTab) => {
    const resolvedTab = newTab === 'ordenes' ? 'cotizaciones' : newTab
    if (resolvedTab && ['dashboard', 'cotizaciones', 'solicitudes', 'clientes'].includes(resolvedTab)) {
      activeTab.value = resolvedTab
      if (navigationStore.comercialSubOption !== resolvedTab) {
        navigationStore.setComercialSubOption(resolvedTab)
      }
      closeAllForms()
    }
  },
  { immediate: true }
)

// Sincronizar activeTab y resetear formularios con sub-opción de navegación Comercial y navTick
watch(
  () => [navigationStore.comercialSubOption, navigationStore.navTick],
  ([newSub]) => {
    const resolvedSub = newSub === 'ordenes' ? 'cotizaciones' : newSub
    if (resolvedSub && ['dashboard', 'cotizaciones', 'solicitudes', 'clientes'].includes(resolvedSub)) {
      if (activeTab.value !== resolvedSub) {
        activeTab.value = resolvedSub
        closeAllForms()
      }
    }
  }
)

// Escuchar disparadores de acciones desde el Sidebar
watch(
  () => [navigationStore.comercialActionTrigger, navigationStore.navTick],
  ([action]) => {
    if (action === 'nueva-cotizacion') {
      openCreateCotizacion()
      setTimeout(() => {
        if (navigationStore.comercialActionTrigger === 'nueva-cotizacion') {
          navigationStore.comercialActionTrigger = null
        }
      }, 50)
    } else if (action === 'nuevo-cliente') {
      openCreateCliente()
      setTimeout(() => {
        if (navigationStore.comercialActionTrigger === 'nuevo-cliente') {
          navigationStore.comercialActionTrigger = null
        }
      }, 50)
    }
  }
)

const cotizaciones = ref([])
const solicitudesSinAsignar = ref([])
const clientes = ref([])

// Normalizador de texto para búsquedas (ignora tildes, mayúsculas y espacios extras)
const normalizeSearchText = (text) => {
  if (!text) return ''
  return text
    .toString()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

// Filtros y ordenación para Cotizaciones (Sub-opción 02)
const searchCotizacion = ref('')
const filterRegimenCotizacion = ref('ALL')
const filterEstadoCotizacion = ref('')
const filterAsesorCotizacion = ref('ALL')
const sortByCotizacion = ref('fecha-desc')

// Filtros y ordenación para Solicitudes Sin Asignar (Sub-opción 03)
const searchSolicitud = ref('')
const filterRegimenSolicitud = ref('ALL')
const sortBySolicitud = ref('fecha-desc')

// Filtros y ordenación para Clientes (Sub-opción 04)
const searchCliente = ref('')
const filterTipoCliente = ref('ALL')
const filterEstadoCliente = ref('ALL')
const sortByCliente = ref('nombre-asc')

// Banderas de filtros activos
const hasActiveCotizacionFilters = computed(() => {
  return (
    !!searchCotizacion.value ||
    filterRegimenCotizacion.value !== 'ALL' ||
    !!filterEstadoCotizacion.value ||
    (!isAgenteComercial.value && filterAsesorCotizacion.value !== 'ALL') ||
    sortByCotizacion.value !== 'fecha-desc'
  )
})

const hasActiveSolicitudFilters = computed(() => {
  return (
    !!searchSolicitud.value ||
    filterRegimenSolicitud.value !== 'ALL' ||
    sortBySolicitud.value !== 'fecha-desc'
  )
})

const hasActiveClienteFilters = computed(() => {
  return (
    !!searchCliente.value ||
    filterTipoCliente.value !== 'ALL' ||
    filterEstadoCliente.value !== 'ALL' ||
    sortByCliente.value !== 'nombre-asc'
  )
})

// Acciones de restablecimiento de filtros
const resetCotizacionFilters = () => {
  searchCotizacion.value = ''
  filterRegimenCotizacion.value = 'ALL'
  filterEstadoCotizacion.value = ''
  filterAsesorCotizacion.value = 'ALL'
  sortByCotizacion.value = 'fecha-desc'
}

const resetSolicitudFilters = () => {
  searchSolicitud.value = ''
  filterRegimenSolicitud.value = 'ALL'
  sortBySolicitud.value = 'fecha-desc'
}

const resetClienteFilters = () => {
  searchCliente.value = ''
  filterTipoCliente.value = 'ALL'
  filterEstadoCliente.value = 'ALL'
  sortByCliente.value = 'nombre-asc'
}

const showToast = (message, type = 'success') => {
  if (type === 'error') {
    showError(message)
  } else if (type === 'warn' || type === 'warning') {
    showWarn(message)
  } else if (type === 'info') {
    showInfo(message)
  } else {
    showSuccess(message)
  }
}

const asesoresDisponibles = computed(() => {
  const map = new Map()
  cotizaciones.value.forEach((c) => {
    if (c.idUsuarioComercial && c.usuarioComercialNombre) {
      map.set(c.idUsuarioComercial, c.usuarioComercialNombre)
    }
  })
  return Array.from(map.entries())
    .map(([id, nombre]) => ({ id, nombre }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre))
})

const filteredCotizaciones = computed(() => {
  let list = cotizaciones.value.filter((c) => {
    // 1. Filtro Estado
    if (filterEstadoCotizacion.value) {
      if (filterEstadoCotizacion.value === 'ENVIADA' || filterEstadoCotizacion.value === 'PENDIENTE') {
        if (c.estado !== 'ENVIADA' && c.estado !== 'PENDIENTE') return false
      } else if (filterEstadoCotizacion.value === 'REVISION' || filterEstadoCotizacion.value === 'OBSERVADA') {
        if (c.estado !== 'REVISION' && c.estado !== 'OBSERVADA') return false
      } else if (c.estado !== filterEstadoCotizacion.value) {
        return false
      }
    }

    // 2. Filtro Régimen
    if (filterRegimenCotizacion.value !== 'ALL') {
      if (c.regimenAduanero !== filterRegimenCotizacion.value) return false
    }

    // 3. Filtro Asesor Comercial (para Gerencia / Admin TI)
    if (!isAgenteComercial.value && filterAsesorCotizacion.value !== 'ALL') {
      if (filterAsesorCotizacion.value === 'UNASSIGNED') {
        if (c.idUsuarioComercial) return false
      } else if (String(c.idUsuarioComercial) !== filterAsesorCotizacion.value) {
        return false
      }
    }

    // 4. Búsqueda inteligente (ignora tildes, tokens)
    const q = normalizeSearchText(searchCotizacion.value)
    if (q) {
      const tokens = q.split(/\s+/).filter(Boolean)
      const corpus = normalizeSearchText(
        `${c.codigoCotizacion || ''} ${c.clienteRazonSocial || ''} ${c.clienteNumeroDocumento || ''} ${c.usuarioComercialNombre || ''} ${c.regimenAduanero || ''} ${c.estado || ''}`
      )
      if (!tokens.every((t) => corpus.includes(t))) return false
    }

    return true
  })

  // 5. Ordenar
  return list.sort((a, b) => {
    switch (sortByCotizacion.value) {
      case 'fecha-asc':
        return new Date(a.fechaEmision || 0) - new Date(b.fechaEmision || 0)
      case 'monto-desc':
        return (Number(b.totalCotizado) || 0) - (Number(a.totalCotizado) || 0)
      case 'monto-asc':
        return (Number(a.totalCotizado) || 0) - (Number(b.totalCotizado) || 0)
      case 'codigo-asc':
        return (a.codigoCotizacion || '').localeCompare(b.codigoCotizacion || '')
      case 'cliente-asc':
        return (a.clienteRazonSocial || '').localeCompare(b.clienteRazonSocial || '')
      case 'fecha-desc':
      default:
        return new Date(b.fechaEmision || 0) - new Date(a.fechaEmision || 0) || (b.idCotizacion || 0) - (a.idCotizacion || 0)
    }
  })
})

const filteredSolicitudes = computed(() => {
  let list = solicitudesSinAsignar.value.filter((s) => {
    if (filterRegimenSolicitud.value !== 'ALL') {
      if (s.regimenAduanero !== filterRegimenSolicitud.value) return false
    }

    const q = normalizeSearchText(searchSolicitud.value)
    if (q) {
      const tokens = q.split(/\s+/).filter(Boolean)
      const corpus = normalizeSearchText(
        `${s.codigoCotizacion || ''} ${s.clienteRazonSocial || ''} ${s.clienteNumeroDocumento || ''} ${s.mercanciaNombre || ''} ${s.regimenAduanero || ''}`
      )
      if (!tokens.every((t) => corpus.includes(t))) return false
    }

    return true
  })

  return list.sort((a, b) => {
    switch (sortBySolicitud.value) {
      case 'fecha-asc':
        return new Date(a.fechaEmision || 0) - new Date(b.fechaEmision || 0)
      case 'monto-desc':
        return (Number(b.totalCotizado) || 0) - (Number(a.totalCotizado) || 0)
      case 'cliente-asc':
        return (a.clienteRazonSocial || '').localeCompare(b.clienteRazonSocial || '')
      case 'fecha-desc':
      default:
        return new Date(b.fechaEmision || 0) - new Date(a.fechaEmision || 0) || (b.idCotizacion || 0) - (a.idCotizacion || 0)
    }
  })
})

const filteredClientes = computed(() => {
  let list = clientes.value.filter((cl) => {
    // 1. Tipo
    if (filterTipoCliente.value === 'PROSPECTO' && !cl.esProspecto) return false
    if (filterTipoCliente.value === 'CLIENTE' && cl.esProspecto) return false

    // 2. Estado
    if (filterEstadoCliente.value === 'ACTIVO' && cl.estado === 'INACTIVO') return false
    if (filterEstadoCliente.value === 'INACTIVO' && cl.estado !== 'INACTIVO') return false

    // 3. Búsqueda inteligente
    const q = normalizeSearchText(searchCliente.value)
    if (q) {
      const tokens = q.split(/\s+/).filter(Boolean)
      const corpus = normalizeSearchText(
        `${cl.numeroDocumento || ''} ${cl.razonSocial || ''} ${cl.contactoRepresentante || ''} ${cl.correoContacto || ''} ${cl.telefonoContacto || ''} ${cl.direccionFiscal || ''}`
      )
      if (!tokens.every((t) => corpus.includes(t))) return false
    }

    return true
  })

  // 4. Ordenar
  return list.sort((a, b) => {
    switch (sortByCliente.value) {
      case 'nombre-desc':
        return (b.razonSocial || '').localeCompare(a.razonSocial || '')
      case 'doc-asc':
        return (a.numeroDocumento || '').localeCompare(b.numeroDocumento || '')
      case 'tipo-asc':
        return (b.esProspecto ? 1 : 0) - (a.esProspecto ? 1 : 0)
      case 'nombre-asc':
      default:
        return (a.razonSocial || '').localeCompare(b.razonSocial || '')
    }
  })
})

const loadData = async () => {
  loading.value = true
  try {
    const [cotsRes, sinAsignarRes, clsRes] = await Promise.allSettled([
      comercialService.getCotizaciones(),
      comercialService.getCotizacionesSinAsignar(),
      comercialService.getClientes()
    ])
    if (cotsRes.status === 'fulfilled') cotizaciones.value = cotsRes.value || []
    if (sinAsignarRes.status === 'fulfilled') solicitudesSinAsignar.value = sinAsignarRes.value || []
    if (clsRes.status === 'fulfilled') clientes.value = clsRes.value || []

    const firstFailure = [cotsRes, sinAsignarRes, clsRes].find((r) => r.status === 'rejected')
    if (firstFailure) {
      showToast(firstFailure.reason?.response?.data?.message || 'Error al conectar con la API de ventas.', 'error')
    }
  } finally {
    loading.value = false
  }
}

// ==========================================
// CLIENTES ACTIONS
// ==========================================
const openCreateCliente = () => {
  selectedCliente.value = null
  showClienteForm.value = true
}

const openEditCliente = (cliente) => {
  selectedCliente.value = { ...cliente }
  showClienteForm.value = true
}

const handleClienteSaved = async ({ isEdit, clienteId, payload, done }) => {
  try {
    if (isEdit) {
      await comercialService.updateCliente(clienteId, payload)
      showToast('Cliente actualizado correctamente.')
    } else {
      await comercialService.createCliente(payload)
      showToast('Nuevo cliente registrado exitosamente.')
    }
    showClienteForm.value = false
    await loadData()
  } catch (err) {
    showToast(err.response?.data?.message || 'Error al guardar cliente.', 'error')
  } finally {
    done?.()
  }
}

const handleDeleteCliente = async (cliente) => {
  const confirmed = await confirmDelete(
    `¿Desea eliminar el cliente ${cliente.razonSocial}?`,
    'Confirmar Eliminación de Cliente'
  )
  if (!confirmed) return

  try {
    await comercialService.deleteCliente(cliente.idCliente)
    showToast('Cliente desactivado correctamente.', 'info')
    await loadData()
  } catch (err) {
    showToast(err.response?.data?.message || 'No se pudo eliminar el cliente.', 'error')
  }
}

// ==========================================
// COTIZACIONES ACTIONS
// ==========================================
const openCreateCotizacion = () => {
  selectedCotizacion.value = null
  showCotizacionForm.value = true
}

const openCorrectionCotizacion = (cotizacion) => {
  selectedCotizacion.value = { ...cotizacion }
  showCorrectionView.value = true
  showCotizacionForm.value = false
  showClienteForm.value = false
  showObservationDialog.value = false
}

const openEditCotizacion = (cotizacion) => {
  if (cotizacion?.estado === 'REVISION' || cotizacion?.estado === 'OBSERVADA') {
    openCorrectionCotizacion(cotizacion)
    return
  }
  selectedCotizacion.value = { ...cotizacion }
  showCotizacionForm.value = true
}

const handleQuickClientCreated = async (newClient) => {
  await loadData()
}

const handleCotizacionSaved = async ({ isEdit, cotizacionId, payload, documentos, done }) => {
  try {
    const wasUnassigned = isEdit && selectedCotizacion.value && !selectedCotizacion.value.idUsuarioComercial
    const wasObservada = isEdit && selectedCotizacion.value && (selectedCotizacion.value.estado === 'OBSERVADA' || selectedCotizacion.value.estado === 'REVISION')

    if (isEdit) {
      await comercialService.updateCotizacion(cotizacionId, payload)
      if (cotizacionId && documentos && documentos.length > 0) {
        for (const doc of documentos) {
          try {
            await documentoService.upload([doc.file], {
              categoria: doc.categoria,
              descripcion: doc.descripcion || '',
              entidadTipo: 'COTIZACION',
              idEntidad: cotizacionId,
              idUsuario: authStore.user?.idUsuario || 1
            })
          } catch (docErr) {
            console.error('Error al subir documento sustentatorio en edición:', docErr)
          }
        }
      }
      if (wasUnassigned) {
        showToast('Solicitud liquidada y asignada exitosamente a su cartera comercial.', 'success')
      } else if (wasObservada) {
        showToast('Cotización actualizada y correcciones enviadas a Supervisión Aduanera.', 'success')
      } else {
        showToast('Cotización actualizada correctamente.', 'success')
      }
    } else {
      const created = await comercialService.createCotizacion(payload)
      const newId = created?.idCotizacion

      if (newId && documentos && documentos.length > 0) {
        let uploadedCount = 0
        for (const doc of documentos) {
          try {
            await documentoService.upload([doc.file], {
              categoria: doc.categoria,
              descripcion: doc.descripcion || '',
              entidadTipo: 'COTIZACION',
              idEntidad: newId,
              idUsuario: authStore.user?.idUsuario || 1
            })
            uploadedCount++
          } catch (docErr) {
            console.error('Error al subir documento sustentatorio:', docErr)
          }
        }
        showToast(`Cotización registrada exitosamente con ${uploadedCount} documento(s) adjunto(s).`)
      } else {
        showToast('Cotización registrada exitosamente.')
      }
    }
    closeAllForms()
    await loadData()
  } catch (err) {
    showToast(err.response?.data?.message || 'Error al guardar cotización.', 'error')
  } finally {
    done?.()
  }
}

const handleDeleteCotizacion = async (cotizacion) => {
  const confirmed = await confirmDelete(
    `¿Desea eliminar la cotización ${cotizacion.codigoCotizacion}?`,
    'Confirmar Eliminación de Cotización'
  )
  if (!confirmed) return

  try {
    await comercialService.deleteCotizacion(cotizacion.idCotizacion)
    showToast('Cotización eliminada exitosamente.', 'info')
    await loadData()
  } catch (err) {
    showToast(err.response?.data?.message || 'No se puede eliminar la cotización.', 'error')
  }
}

const getCotizacionSeverity = (estado) => {
  if (estado === 'APROBADA') return 'success'
  if (estado === 'RECHAZADA' || estado === 'ANULADA') return 'danger'
  if (estado === 'REVISION' || estado === 'OBSERVADA') return 'warn'
  if (estado === 'EN_REVISION') return 'info'
  return 'contrast'
}

const getCotizacionLabel = (estado) => {
  const map = {
    ENVIADA: 'Pendiente',
    PENDIENTE: 'Pendiente',
    EN_REVISION: 'En Revisión',
    REVISION: 'Observada',
    OBSERVADA: 'Observada',
    APROBADA: 'Aprobada',
    RECHAZADA: 'Rechazada',
    ANULADA: 'Anulada'
  }
  return map[estado] || estado
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.ventas-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   ESTRUCTURA Y DASHBOARD COMERCIAL (LENGUAJE EDITORIAL CÁLIDO SGA)
   ========================================================================== */
.ventas-master-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.dashboard-comercial-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

/* 1. Barra de Atajos Rápidos Comerciales */
.dashboard-action-toolbar {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1rem 1.35rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  box-shadow: var(--shadow-subtle);
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.toolbar-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

/* 2. Grid de Métricas Comerciales Avanzadas */
.metrics-commercial-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.25rem;
}

@media (max-width: 1200px) {
  .metrics-commercial-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-commercial-grid {
    grid-template-columns: 1fr;
  }
}

.commercial-kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem 1.45rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: var(--shadow-subtle);
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
}

.commercial-kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-border-strong);
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-num {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 4px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
}

.kpi-label {
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--color-text-muted);
  flex: 1;
}

.kpi-icon {
  font-size: 1.05rem;
  color: var(--color-text-subtle);
}

.kpi-icon.success {
  color: var(--color-success-text);
}

.kpi-icon.warning {
  color: var(--color-warning-text);
}

.kpi-icon.danger {
  color: var(--color-danger-text);
}

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-value {
  font-size: 2.15rem;
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1;
  letter-spacing: -0.02em;
}

.kpi-badge {
  font-size: 0.725rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill);
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
}

.kpi-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.kpi-badge.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
}

.kpi-badge.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
}

.kpi-badge.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border-color: var(--color-info-border);
}

.kpi-footer-note {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
  border-top: 1px dashed var(--color-border-subtle);
  padding-top: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 3. Paneles Analíticos */
.dashboard-analytics-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

@media (max-width: 960px) {
  .dashboard-analytics-row {
    grid-template-columns: 1fr;
  }
}

.analytics-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.35rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-shadow: var(--shadow-subtle);
}

.card-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.card-main-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0.15rem 0 0;
  letter-spacing: -0.01em;
}

.analytics-stat-pill {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
}

.pipeline-progress-bar {
  display: flex;
  height: 12px;
  border-radius: var(--radius-pill);
  overflow: hidden;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  margin: 0.35rem 0;
}

.pipeline-segment {
  height: 100%;
  transition: width 0.4s ease;
}

.pipeline-segment.approved {
  background: #15803d;
}

.pipeline-segment.pending {
  background: #b45309;
}

.pipeline-segment.observed {
  background: #eab308;
}

.pipeline-segment.rejected {
  background: #b91c1c;
}

.pipeline-legend {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.65rem;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-dot.approved {
  background: #15803d;
}

.legend-dot.pending {
  background: #b45309;
}

.legend-dot.observed {
  background: #eab308;
}

.legend-dot.rejected {
  background: #b91c1c;
}

.legend-count {
  margin-left: auto;
  color: var(--color-text-main);
  font-weight: 700;
}

.regimen-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.regimen-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.55rem 0.85rem;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-subtle);
}

.regimen-info {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.regimen-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.regimen-count {
  font-size: 0.725rem;
  color: var(--color-text-muted);
}

.regimen-total {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.empty-mini {
  padding: 1rem;
  text-align: center;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

/* 4. Actividad Comercial Reciente */
.recent-commercial-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.35rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-shadow: var(--shadow-subtle);
}

.btn-sm {
  padding: 0.4rem 0.85rem;
  font-size: 0.785rem;
}

.recent-table-wrapper {
  overflow-x: auto;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
}

.compact-commercial-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.84rem;
  text-align: left;
}

.compact-commercial-table thead th {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  padding: 0.65rem 0.85rem;
  font-weight: 700;
  font-size: 0.725rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--color-border);
}

.compact-commercial-table tbody tr {
  border-bottom: 1px solid var(--color-border-subtle);
  transition: background var(--transition-fast);
}

.compact-commercial-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.compact-commercial-table tbody td {
  padding: 0.65rem 0.85rem;
  color: var(--color-text-main);
  vertical-align: middle;
}

.text-success {
  color: var(--color-success-text) !important;
}

.text-warning {
  color: var(--color-warning-text) !important;
}

.text-danger {
  color: var(--color-danger-text) !important;
}

/* Contenido de cada Tab */
.tab-content-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 1rem;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.section-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.search-mini-input {
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.55rem 0.85rem;
  font-size: 0.825rem;
  color: var(--color-text-main);
  outline: none;
}

.search-mini-input:focus {
  border-color: var(--color-primary);
}

/* Tablas */
.table-container {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-subtle);
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.86rem;
}

.custom-table thead th {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  font-weight: 700;
  font-size: 0.725rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0.95rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
}

.custom-table tbody tr {
  border-bottom: 1px solid var(--color-border-subtle);
  transition: background var(--transition-fast);
  background: var(--color-bg-surface);
}

.custom-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.custom-table tbody td {
  padding: 0.95rem 1.25rem;
  vertical-align: middle;
}

.doc-code {
  font-weight: 700;
  color: var(--color-text-main);
  font-family: var(--font-mono);
  font-size: 0.85rem;
}

.doc-code-os {
  font-weight: 700;
  color: var(--color-success-text);
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 0.85rem;
}

.client-cell {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-weight: 600;
  color: var(--color-text-main);
}

.client-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.regimen-tag {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.total-amount {
  font-weight: 700;
  color: var(--color-text-main);
}

.date-text {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill);
  font-size: 0.725rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.status-pending {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.status-approved {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-rejected {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.status-progress {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.os-linked-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.785rem;
  font-weight: 600;
  color: var(--color-success-text);
}

.btn-generate-os {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.3rem 0.65rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  transition: all var(--transition-fast);
}

.btn-generate-os:hover {
  background: var(--color-bg-muted);
  color: var(--color-text-main);
}

.text-muted-dash {
  color: var(--color-text-subtle);
}

.text-right {
  text-align: right;
}

.actions-wrapper {
  display: inline-flex;
  gap: 0.45rem;
}

.action-btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.3rem 0.75rem;
  border-radius: 6px;
  font-size: 0.785rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn-outline:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
  color: var(--color-text-main);
}

.action-btn-outline.approve:hover {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.action-btn-outline.reject:hover {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.action-btn-outline.finish:hover {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.btn-danger-action:hover {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.doc-badge {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.doc-type {
  font-size: 0.65rem;
  font-weight: 700;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.doc-number {
  font-weight: 700;
  font-family: var(--font-mono);
}

.contact-text,
.address-text {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.client-tag {
  display: inline-flex;
  align-items: center;
  font-size: 0.725rem;
  font-weight: 700;
  padding: 0.15rem 0.55rem;
  border-radius: 4px;
}

.prospect-tag {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.client-effective-tag {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.obs-snippet {
  font-size: 0.785rem;
  color: var(--color-text-muted);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 3.5rem 1.5rem;
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 2.25rem;
  color: var(--color-text-subtle);
  margin-bottom: 0.75rem;
}

.empty-title {
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.empty-desc {
  font-size: 0.8rem;
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
}

@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}

/* Estado Cell & Observation Pill */
.status-cell {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  align-items: flex-start;
}

.obs-warning-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.15rem 0.45rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  color: var(--color-warning-text);
  border-radius: var(--radius-sm, 6px);
  font-size: 0.6875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.obs-warning-pill:hover {
  filter: brightness(0.95);
}

.action-btn-outline.observe-fix {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
  font-weight: 700;
}

.action-btn-outline.observe-fix:hover {
  filter: brightness(0.95);
}

/* Contenido Modal de Observación */
.observation-view-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
  padding: var(--space-2, 0.5rem) 0;
}

.obs-box-header {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding-bottom: var(--space-2, 0.5rem);
  border-bottom: 1px solid var(--color-border);
}

.obs-box-title {
  font-size: 1rem;
  color: var(--color-text-main);
}

.obs-box-body {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-4, 1rem);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-warning-text);
}

.obs-box-icon {
  font-size: 1.5rem;
  margin-top: 0.15rem;
  flex-shrink: 0;
}

.obs-box-label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  display: block;
}

.obs-box-text {
  margin: 0.25rem 0 0 0;
  font-size: 0.875rem;
  line-height: 1.45;
  font-weight: 500;
}

.obs-box-hint {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.45;
  margin: 0;
}

.dialog-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3, 0.75rem);
}

/* ==========================================================================
   Controles de Tablas (Filtros, Búsqueda, Ordenación - Estilo Warm Editorial)
   ========================================================================== */
.table-controls-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 0.85rem 1.25rem;
  box-shadow: var(--shadow-subtle);
}

.controls-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  flex: 1;
}

.controls-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 260px;
  flex: 1;
  max-width: 380px;
}

.search-icon {
  position: absolute;
  left: 0.85rem;
  color: var(--color-text-subtle);
  font-size: 0.85rem;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 0.55rem 2rem 0.55rem 2.25rem;
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 0.825rem;
  color: var(--color-text-main);
  outline: none;
  transition: all var(--transition-fast);
}

.search-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(214, 180, 118, 0.2);
}

.clear-search-btn {
  position: absolute;
  right: 0.5rem;
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-search-btn:hover {
  color: var(--color-text-main);
}

.filter-dropdown, .sort-dropdown {
  position: relative;
}

.filter-select {
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  font-family: var(--font-sans) !important;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main) !important;
  background-color: var(--color-bg-surface) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md) !important;
  padding: 0.55rem 2.25rem 0.55rem 0.85rem !important;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239a7f60' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e") !important;
  background-repeat: no-repeat !important;
  background-position: right 0.75rem center !important;
  background-size: 0.9rem 0.9rem !important;
  cursor: pointer;
  outline: none;
  transition: all var(--transition-fast);
}

.filter-select:focus {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 2px rgba(214, 180, 118, 0.2) !important;
}

.filter-select option {
  background-color: var(--color-bg-surface);
  color: var(--color-text-main);
}

.btn-clear-filters {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.8rem;
  border-radius: var(--radius-md);
  font-size: 0.775rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-clear-filters:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
}

.client-sub-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.15rem;
}

.portal-badge,
.origin-badge.origin-portal {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-sm, 3px);
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.presencial-badge,
.origin-badge.origin-presencial {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-sm, 3px);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.obs-client-status {
  margin-top: 0.4rem;
}

.btn-whatsapp-action,
.btn-whatsapp {
  color: var(--color-success-text) !important;
  border-color: var(--color-success-border) !important;
}

.btn-whatsapp-action:hover,
.btn-whatsapp:hover {
  background-color: #25d366 !important;
  color: #ffffff !important;
  border-color: #25d366 !important;
}

.btn-whatsapp-dialog,
.btn-whatsapp-modal {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #25d366;
  color: #ffffff;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md, 4px);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-whatsapp-dialog:hover,
.btn-whatsapp-modal:hover {
  background-color: #1ebd56;
}
</style>
