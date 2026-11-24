<template>
  <div class="gestion-aduanera-view">
    <!-- ============================================================== -->
    <!-- VISTA COMPLETA: AUDITORÍA TÉCNICA DEL EXPEDIENTE / COTIZACIÓN   -->
    <!-- ============================================================== -->
    <AuditoriaTecnicaPanel
      v-if="showAuditoriaCompleta && (inspectedCotizacion || inspectedExpediente)"
      :cotizacion="inspectedCotizacion"
      :expediente="inspectedExpediente"
      :processing="processingAction"
      @back="closeAuditoriaCompleta"
      @approve="handleApproveFromAuditoria"
      @observe="openRevisionDialog($event, 'observe')"
      @reject="openRevisionDialog($event, 'reject')"
      @open-apertura="handleAperturaFromAuditoria"
      @doc-updated="loadData"
    />

    <!-- ============================================================== -->
    <!-- VISTA PRINCIPAL: BANDEJA Y GESTIÓN MAESTRA ADUANERA            -->
    <!-- ============================================================== -->
    <div v-else class="supervision-master-container">
      <!-- ============================================================== -->
      <!-- PESTAÑA 01: DASHBOARD OPERATIVO ADUANERO                       -->
      <!-- ============================================================== -->
      <div v-if="activeTab === 'dashboard'" class="tab-content-section fade-in">
        <DashboardAduaneroPanel
          :cotizaciones="cotizaciones"
          :expedientes="expedientes"
          :loading="loading"
          @refresh="loadData"
          @navigate="handleDashboardNavigate"
        />
      </div>

      <!-- ============================================================== -->
      <!-- PESTAÑA 02: BANDEJA DE COTIZACIONES ADUANERAS                  -->
      <!-- ============================================================== -->
      <AduanasCotizacionesPanel
        v-else-if="activeTab === 'cotizaciones'"
        class="fade-in"
        :cotizaciones="filteredCotizaciones"
        :clasificadas="cotizacionesClasificadas"
        :total-count="cotizaciones.length"
        :loading="loading"
        v-model:sub-queue="subQueueCotizaciones"
        v-model:search-query="searchQuery"
        v-model:filter-regimen="filterRegimenCotizacion"
        v-model:sort-by="sortByCotizacion"
        :has-active-filters="hasActiveCotizacionFilters"
        @reset-filters="resetCotizacionFilters"
        @refresh="loadData"
        @inspect="openInspectModal"
        @open-docs="openDocumentosCotizacion"
        @approve="confirmApprove"
        @observe="openRevisionDialog($event, 'observe')"
        @reject="openRevisionDialog($event, 'reject')"
      />

      <!-- ============================================================== -->
      <!-- PESTAÑA 03: APERTURA FORMAL DE EXPEDIENTES ADUANEROS           -->
      <!-- ============================================================== -->
      <AduanasAperturaPanel
        v-else-if="activeTab === 'apertura'"
        class="fade-in"
        :cotizaciones="filteredApertura"
        :loading="loading"
        v-model:search-query="aperturaSearchQuery"
        v-model:filter-regimen="aperturaFilterRegimen"
        v-model:sort-by="aperturaSortBy"
        :has-active-filters="hasActiveAperturaFilters"
        @reset-filters="resetAperturaFilters"
        @refresh="loadData"
        @open-apertura="goToApertura"
      />

      <!-- ============================================================== -->
      <!-- PESTAÑA 04: GESTIÓN INTEGRAL DE EXPEDIENTES ADUANEROS          -->
      <!-- ============================================================== -->
      <AduanasExpedientesPanel
        v-else-if="activeTab === 'expedientes'"
        class="fade-in"
        :expedientes="filteredExpedientes"
        :fase-counts="faseCounts"
        :agentes-disponibles="agentesAduanerosDisponibles"
        :mis-expedientes-count="misExpedientesCount"
        :current-user-id="authStore.user?.idUsuario || null"
        :loading="loading"
        v-model:fase="faseExpediente"
        v-model:filter-agente="filterAgenteExpediente"
        v-model:filter-regimen="filterRegimenExpediente"
        v-model:filter-canal="filterCanalExpediente"
        v-model:search-query="searchExpedienteQuery"
        v-model:sort-by="sortByExpediente"
        :has-active-filters="hasActiveExpedienteFilters"
        @reset-filters="resetExpedienteFilters"
        @refresh="loadData"
        @inspect="openAuditarExpediente"
        @open-docs="openDocumentosExpediente"
        @open-transito="openActualizarTransitoModal"
        @open-canal="openCanalModal"
        @open-levante="openLevanteModal"
        @open-retiro="openRetiroDirectoModal"
        @open-cierre="openCierreModal"
      />
    </div>

    <!-- ============================================================== -->
    <!-- MODALES OPERATIVOS AUXILIARES (APERTURA, DETALLE, CANAL, ETC.) -->
    <!-- ============================================================== -->
    <!-- Modal Unificado: Observar o Rechazar Cotización desde Bandeja -->
    <CotizacionRevisionDialog
      v-model:visible="showRevisionDialog"
      :mode="revisionDialogMode"
      :cotizacion="actionCotizacion"
      :loading="processingAction"
      @confirm="handleRevisionConfirm"
    />

    <!-- Modal: Asignación de Canal SUNAT / Levante Aduanero -->
    <CanalLevanteModal
      :visible="showCanalLevanteModal"
      :expediente="selectedExpedienteParaTramite"
      :action-type="canalLevanteActionType"
      @close="showCanalLevanteModal = false"
      @saved="onCanalLevanteSaved"
    />

    <!-- Modal: Gestión Integral de Documentos Adjuntos -->
    <DocumentosManagerModal
      :visible="showDocumentosModal"
      :entidad-tipo="selectedDocEntidadTipo"
      :entidad-id="selectedDocEntidadId"
      :codigo-entidad="selectedDocCodigoEntidad"
      :titulo-entidad="selectedDocTituloEntidad"
      @close="showDocumentosModal = false"
    />

    <!-- Modal: Registro de Retiro Directo en Terminal -->
    <RetiroDirectoModal
      :visible="showRetiroDirectoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRetiroDirectoModal = false"
      @saved="onRetiroDirectoSaved"
    />


    <!-- Modal: Conformidad y Cierre Definitivo de Expediente -->
    <CierreExpedienteModal
      :visible="showCierreExpedienteModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showCierreExpedienteModal = false"
      @saved="onCierreExpedienteSaved"
    />

    <!-- Modal: Actualización de Tránsito, Contenedor y Precinto Diferidos -->
    <ActualizarTransitoModal
      :visible="showActualizarTransitoModal"
      :expediente="selectedExpedienteParaTransito"
      @close="showActualizarTransitoModal = false"
      @updated="onTransitoUpdated"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gestionAduaneraService } from '../services/gestionAduaneraService'
import { expedienteService } from '../services/expedienteService'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import AduanasAperturaPanel from '../components/AduanasAperturaPanel.vue'
import AduanasExpedientesPanel from '../components/AduanasExpedientesPanel.vue'
import CanalLevanteModal from '../components/CanalLevanteModal.vue'
import RetiroDirectoModal from '../components/RetiroDirectoModal.vue'
import CierreExpedienteModal from '../components/CierreExpedienteModal.vue'
import ActualizarTransitoModal from '../components/ActualizarTransitoModal.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import AuditoriaTecnicaPanel from '../components/auditoria/AuditoriaTecnicaPanel.vue'
import DashboardAduaneroPanel from '../components/DashboardAduaneroPanel.vue'
import AduanasCotizacionesPanel from '../components/AduanasCotizacionesPanel.vue'
import CotizacionRevisionDialog from '../components/CotizacionRevisionDialog.vue'
import { useAduanasCotizaciones } from '../composables/useAduanasCotizaciones'
import { useAduanasApertura } from '../composables/useAduanasApertura'
import { useAduanasExpedientes } from '../composables/useAduanasExpedientes'
import { filtrarCotizacionesParaSupervision } from '../utils/supervisionCotizacionUtils'


const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const navigationStore = useNavigationStore()

const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction } = useConfirmation()

const loading = ref(false)
const activeTab = ref('dashboard') // 'dashboard' | 'cotizaciones' | 'apertura' | 'expedientes'
const cotizaciones = ref([])
const expedientes = ref([])

const {
  faseExpediente,
  filterAgente: filterAgenteExpediente,
  filterRegimen: filterRegimenExpediente,
  filterCanal: filterCanalExpediente,
  searchQuery: searchExpedienteQuery,
  sortBy: sortByExpediente,
  hasActiveFilters: hasActiveExpedienteFilters,
  resetFilters: resetExpedienteFilters,
  agentesDisponibles: agentesAduanerosDisponibles,
  misExpedientesCount,
  faseCounts,
  filteredExpedientes
} = useAduanasExpedientes({ expedientes, authStore })

// Vista Completa de Auditoría
const showAuditoriaCompleta = ref(false)
const inspectedCotizacion = ref(null)
const inspectedExpediente = ref(null)

// Modales Operativos Auxiliares
const showCanalLevanteModal = ref(false)
const selectedExpedienteParaTramite = ref(null)
const canalLevanteActionType = ref('canal')

// Modal de Actualización de Tránsito Diferido
const showActualizarTransitoModal = ref(false)
const selectedExpedienteParaTransito = ref(null)

const openActualizarTransitoModal = (exp) => {
  selectedExpedienteParaTransito.value = exp
  showActualizarTransitoModal.value = true
}

const onTransitoUpdated = () => {
  showActualizarTransitoModal.value = false
  showSuccess('Datos de embarque y tránsito actualizados correctamente.')
  loadData()
}

// Modal de Gestión de Documentos Adjuntos
const showDocumentosModal = ref(false)
const selectedDocEntidadTipo = ref('COTIZACION')
const selectedDocEntidadId = ref(null)
const selectedDocCodigoEntidad = ref('')
const selectedDocTituloEntidad = ref('')

const openDocumentosCotizacion = (cotizacion) => {
  if (!cotizacion) return
  selectedDocEntidadTipo.value = 'COTIZACION'
  selectedDocEntidadId.value = cotizacion.idCotizacion
  selectedDocCodigoEntidad.value = cotizacion.codigoCotizacion || `COT-${cotizacion.idCotizacion}`
  selectedDocTituloEntidad.value = `${cotizacion.codigoCotizacion || 'Cotización'} — ${cotizacion.clienteRazonSocial || 'Cliente'}`
  showDocumentosModal.value = true
}

const openDocumentosExpediente = (expediente) => {
  if (!expediente) return
  selectedDocEntidadTipo.value = 'EXPEDIENTE'
  selectedDocEntidadId.value = expediente.idExpediente
  selectedDocCodigoEntidad.value = expediente.codigoExpediente || `EXP-${expediente.idExpediente}`
  selectedDocTituloEntidad.value = `${expediente.codigoExpediente || 'Expediente'} — ${expediente.clienteRazonSocial || 'Cliente'}`
  showDocumentosModal.value = true
}

// Modal unificado de revisión (Observar / Rechazar)
const showRevisionDialog = ref(false)
const revisionDialogMode = ref('observe')
const actionCotizacion = ref(null)

// Sincronización reactiva con el menú lateral (Sidebar) y URL
const switchTab = (tabName) => {
  activeTab.value = tabName
  navigationStore.setGestionAduaneraSubOption(tabName)
  if (route.query.tab !== tabName) {
    router.replace({ query: { ...route.query, tab: tabName } })
  }
}

const handleDashboardNavigate = (tab, subFilter) => {
  switchTab(tab)
  if (tab === 'cotizaciones' && subFilter) {
    subQueueCotizaciones.value = subFilter
  } else if (tab === 'expedientes' && subFilter) {
    faseExpediente.value = subFilter
  }
}

watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab && ['dashboard', 'cotizaciones', 'apertura', 'expedientes'].includes(newTab)) {
      activeTab.value = newTab
      if (navigationStore.gestionAduaneraSubOption !== newTab) {
        navigationStore.setGestionAduaneraSubOption(newTab)
      }
    }
  },
  { immediate: true }
)

watch(
  () => [navigationStore.gestionAduaneraSubOption, navigationStore.navTick],
  ([newSub]) => {
    if (newSub && ['dashboard', 'cotizaciones', 'apertura', 'expedientes'].includes(newSub)) {
      if (activeTab.value !== newSub) {
        activeTab.value = newSub
      }
    }
  }
)

const goToApertura = (cotizacion = null) => {
  if (cotizacion?.idCotizacion) {
    router.push({
      name: 'AperturaExpedienteFormView',
      params: { idCotizacion: cotizacion.idCotizacion }
    })
    return
  }
  switchTab('apertura')
}

const openCanalModal = (exp) => {
  selectedExpedienteParaTramite.value = exp
  canalLevanteActionType.value = 'canal'
  showCanalLevanteModal.value = true
}

const openLevanteModal = (exp) => {
  selectedExpedienteParaTramite.value = exp
  canalLevanteActionType.value = 'levante'
  showCanalLevanteModal.value = true
}

const onCanalLevanteSaved = (actualizado) => {
  showCanalLevanteModal.value = false
  showSuccess(`Expediente ${actualizado.codigoExpediente} actualizado conforme.`)
  loadData()
}

// Retiro Directo en Terminal y Cierre Final
const showRetiroDirectoModal = ref(false)
const showCierreExpedienteModal = ref(false)
const selectedExpedienteParaAccion = ref(null)

const openRetiroDirectoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRetiroDirectoModal.value = true
}

const onRetiroDirectoSaved = () => {
  showRetiroDirectoModal.value = false
  showSuccess('Acta de retiro directo en terminal registrada exitosamente.')
  loadData()
}

const openCierreModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showCierreExpedienteModal.value = true
}

const onCierreExpedienteSaved = () => {
  showCierreExpedienteModal.value = false
  showSuccess('Expediente aduanero cerrado formalmente con conformidad.')
  loadData()
}


// Cargar Datos
const loadData = async () => {
  loading.value = true
  try {
    const [cotsRes, expsRes] = await Promise.allSettled([
      gestionAduaneraService.getCotizaciones(),
      expedienteService.getExpedientes()
    ])
    if (cotsRes.status === 'fulfilled') cotizaciones.value = filtrarCotizacionesParaSupervision(cotsRes.value || [])
    if (expsRes.status === 'fulfilled') expedientes.value = expsRes.value || []

    const firstFailure = [cotsRes, expsRes].find((r) => r.status === 'rejected')
    if (firstFailure) {
      showError(firstFailure.reason?.response?.data?.message || 'Error al sincronizar datos de Supervisión.')
    }

    // Si había una cotización o expediente inspeccionado en vista completa, refrescar su dato
    if (inspectedCotizacion.value) {
      const updated = cotizaciones.value.find((c) => c.idCotizacion === inspectedCotizacion.value.idCotizacion)
      if (updated) inspectedCotizacion.value = updated
    }
    if (inspectedExpediente.value) {
      const updatedExp = expedientes.value.find((e) => e.idExpediente === inspectedExpediente.value.idExpediente)
      if (updatedExp) inspectedExpediente.value = updatedExp
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

const {
  subQueueCotizaciones,
  searchQuery,
  filterRegimen: filterRegimenCotizacion,
  sortBy: sortByCotizacion,
  processingAction,
  cotizacionesClasificadas,
  filteredCotizaciones,
  hasActiveFilters: hasActiveCotizacionFilters,
  resetFilters: resetCotizacionFilters,
  ejecutarRevisionTecnica,
  aprobarCotizacion
} = useAduanasCotizaciones({
  cotizaciones,
  authStore,
  notify: { showSuccess, showError, showInfo },
  confirmAction,
  onReload: loadData
})

const {
  searchQuery: aperturaSearchQuery,
  filterRegimen: aperturaFilterRegimen,
  sortBy: aperturaSortBy,
  hasActiveFilters: hasActiveAperturaFilters,
  resetFilters: resetAperturaFilters,
  filteredApertura
} = useAduanasApertura({
  cotizacionesListas: computed(() => cotizacionesClasificadas.value.listasApertura || [])
})

// ==========================================
// ACCIONES DE AUDITORÍA Y SUPERVISIÓN EN VISTA COMPLETA
// ==========================================
const openInspectModal = (cotizacion) => {
  inspectedCotizacion.value = cotizacion
  inspectedExpediente.value = null
  showAuditoriaCompleta.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const openAuditarExpediente = (exp) => {
  inspectedExpediente.value = exp
  inspectedCotizacion.value = null
  showAuditoriaCompleta.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const closeAuditoriaCompleta = () => {
  showAuditoriaCompleta.value = false
  inspectedCotizacion.value = null
  inspectedExpediente.value = null
}

const confirmApprove = async (cotizacion) => {
  await aprobarCotizacion(cotizacion)
}

const handleApproveFromAuditoria = async (cotizacion) => {
  await confirmApprove(cotizacion)
  if (inspectedCotizacion.value) {
    const updated = cotizaciones.value.find((c) => c.idCotizacion === cotizacion.idCotizacion)
    if (updated) inspectedCotizacion.value = updated
  }
}

const handleAperturaFromAuditoria = (cotizacion) => {
  closeAuditoriaCompleta()
  goToApertura(cotizacion)
}

// Acciones unificadas de revisión (Observar / Rechazar) desde Bandeja o Auditoría
const openRevisionDialog = (cotizacion, mode) => {
  actionCotizacion.value = cotizacion
  revisionDialogMode.value = mode
  showRevisionDialog.value = true
}

const handleRevisionConfirm = async ({ cotizacion, mode, reason }) => {
  const isObserve = mode === 'observe'
  const ok = await ejecutarRevisionTecnica({
    cotizacion,
    estado: isObserve ? 'OBSERVADA' : 'RECHAZADA',
    motivoObservacion: reason,
    observaciones: isObserve
      ? 'Observaciones emitidas por Supervisión Aduanera.'
      : 'Rechazo técnico emitido por Supervisión Aduanera.'
  })
  if (ok) {
    showRevisionDialog.value = false
    if (showAuditoriaCompleta.value) {
      closeAuditoriaCompleta()
    }
  }
}
</script>

<style scoped>
.gestion-aduanera-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
}

.supervision-master-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

/* Animaciones */
.fade-in {
  animation: fadeIn 0.25s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
