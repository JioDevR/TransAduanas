<template>
  <div class="agente-portal-view fade-in">
    <!-- 1. Barra de Herramientas y Filtros Superiores -->
    <AgenteHeaderToolbar
      v-model:searchQuery="searchQuery"
      v-model:filterRegimen="filterRegimen"
      v-model:filterCanal="filterCanal"
      v-model:selectedAgenteId="selectedAgenteId"
      :agentesList="agentesList"
      :esSupervisorOAdmin="esSupervisorOAdmin"
      :loading="loading"
      @refresh="loadData"
    />

    <!-- 2. Tarjetas KPI Interactivas / Filtros Rápidos de Fases -->
    <AgenteKpiGrid
      :counts="kpiCounts"
      :activeTab="activeTab"
      @select-tab="handleTabChange"
    />

    <!-- 3. Tabla Principal de Expedientes Asignados -->
    <AgenteExpedientesTable
      :expedientes="filteredExpedientes"
      :loading="loading"
      @transito="openTransitoModal"
      @diligencia="openDiligenciaModal"
      @solicitar-transporte="openSolicitudTransporteModal"
      @retiro-directo="openRetiroDirectoModal"
      @ingreso-almacen="openIngresoAlmacenModal"
      @recojo-exportacion="openRecojoExportacionModal"
      @ingreso-origen="openIngresoOrigenModal"
      @embarque="openEmbarqueModal"
      @recepcion-exterior="openRecepcionExteriorModal"
      @documentos="openDocumentosExpediente"
      @detalle="openDetalleExp"
    />

    <!-- ============================================================== -->
    <!-- MODALES OPERATIVOS AUXILIARES (IMPORTACIÓN Y COMUNES)           -->
    <!-- ============================================================== -->
    <!-- Modal: Actualizar Tránsito y Embarque -->
    <ActualizarTransitoModal
      :visible="showTransitoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showTransitoModal = false"
      @saved="onTransitoSaved"
    />

    <!-- Modal: Diligencia / Inspección de Campo SUNAT -->
    <DiligenciaCampoModal
      :visible="showDiligenciaModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showDiligenciaModal = false"
      @saved="onDiligenciaSaved"
    />

    <!-- Modal: Solicitar Despacho Terrestre a Transporte y Flota -->
    <SolicitudTransporteModal
      :visible="showSolicitudTransporteModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showSolicitudTransporteModal = false"
      @saved="onSolicitudTransporteSaved"
    />

    <!-- Modal: Acta de Retiro Directo en Terminal -->
    <RetiroDirectoModal
      :visible="showRetiroDirectoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRetiroDirectoModal = false"
      @saved="onRetiroDirectoSaved"
    />

    <!-- Modal: Registro de Ingreso a Depósito / Balanza VGM -->
    <IngresoAlmacenModal
      :visible="showIngresoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showIngresoModal = false"
      @saved="onIngresoSaved"
    />

    <!-- ============================================================== -->
    <!-- MODALES OPERATIVOS ESPECÍFICOS DE EXPORTACIÓN                  -->
    <!-- ============================================================== -->
    <!-- Modal: Solicitar Recojo en Planta del Exportador -->
    <SolicitudRecojoExportacionModal
      :visible="showRecojoExportacionModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRecojoExportacionModal = false"
      @saved="onRecojoExportacionSaved"
    />

    <!-- Modal: Confirmar Ingreso a Depósito / Zona Primaria -->
    <ConfirmarIngresoOrigenModal
      :visible="showIngresoOrigenModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showIngresoOrigenModal = false"
      @saved="onIngresoOrigenSaved"
    />

    <!-- Modal: Confirmar Embarque Internacional -->
    <ConfirmarEmbarqueExportacionModal
      :visible="showEmbarqueModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showEmbarqueModal = false"
      @saved="onEmbarqueSaved"
    />

    <!-- Modal: Confirmar Recepción Conforme en Destino Exterior -->
    <RecepcionDestinoExteriorModal
      :visible="showRecepcionExteriorModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRecepcionExteriorModal = false"
      @saved="onRecepcionExteriorSaved"
    />

    <!-- ============================================================== -->
    <!-- MODALES COMUNES: DETALLE Y DOCUMENTOS DIGITALES               -->
    <!-- ============================================================== -->
    <!-- Modal: Detalle Integral del Expediente -->
    <DetalleExpedienteModal
      :visible="showDetalleModal"
      :expediente="selectedExpedienteParaDetalle"
      @close="showDetalleModal = false"
      @open-docs="openDocumentosExpediente"
    />

    <!-- Modal: Gestor de Documentos Digitales Adjuntos -->
    <DocumentosManagerModal
      :visible="showDocumentosModal"
      entidad-tipo="EXPEDIENTE"
      :id-entidad="selectedDocEntidadId"
      :entidad-id="selectedDocEntidadId"
      :codigo-entidad="selectedDocCodigoEntidad"
      :titulo-entidad="selectedDocTituloEntidad"
      @close="showDocumentosModal = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNotification } from '@/shared/composables/useNotification'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useAgenteBandeja } from '../composables/useAgenteBandeja'

// Componentes de la vista
import AgenteHeaderToolbar from '../components/AgenteHeaderToolbar.vue'
import AgenteKpiGrid from '../components/AgenteKpiGrid.vue'
import AgenteExpedientesTable from '../components/AgenteExpedientesTable.vue'

// Modales operativos compartidos
import ActualizarTransitoModal from '@/features/aduanas/components/ActualizarTransitoModal.vue'
import DiligenciaCampoModal from '@/features/aduanas/components/DiligenciaCampoModal.vue'
import SolicitudTransporteModal from '@/features/aduanas/components/SolicitudTransporteModal.vue'
import RetiroDirectoModal from '@/features/aduanas/components/RetiroDirectoModal.vue'
import IngresoAlmacenModal from '@/features/almacen/components/IngresoAlmacenModal.vue'
import SolicitudRecojoExportacionModal from '@/features/aduanas/components/SolicitudRecojoExportacionModal.vue'
import ConfirmarIngresoOrigenModal from '@/features/aduanas/components/ConfirmarIngresoOrigenModal.vue'
import ConfirmarEmbarqueExportacionModal from '@/features/aduanas/components/ConfirmarEmbarqueExportacionModal.vue'
import RecepcionDestinoExteriorModal from '@/features/aduanas/components/RecepcionDestinoExteriorModal.vue'
import DetalleExpedienteModal from '@/features/aduanas/components/DetalleExpedienteModal.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'

const route = useRoute()
const router = useRouter()
const navigationStore = useNavigationStore()
const { showSuccess, showError } = useNotification()

// Composable central
const {
  loading,
  errorMessage,
  agentesList,
  activeTab,
  filterRegimen,
  filterCanal,
  searchQuery,
  selectedAgenteId,
  esSupervisorOAdmin,
  kpiCounts,
  filteredExpedientes,
  loadAgentes,
  loadData,
  cambiarTab
} = useAgenteBandeja()

// Modales operativos
const selectedExpedienteParaAccion = ref(null)
const selectedExpedienteParaDetalle = ref(null)

const showTransitoModal = ref(false)
const showDiligenciaModal = ref(false)
const showSolicitudTransporteModal = ref(false)
const showRetiroDirectoModal = ref(false)
const showIngresoModal = ref(false)
const showRecojoExportacionModal = ref(false)
const showIngresoOrigenModal = ref(false)
const showEmbarqueModal = ref(false)
const showRecepcionExteriorModal = ref(false)
const showDetalleModal = ref(false)

// Documentos
const showDocumentosModal = ref(false)
const selectedDocEntidadId = ref(null)
const selectedDocCodigoEntidad = ref('')
const selectedDocTituloEntidad = ref('')

// Sincronización de pestañas con router y navigationStore
const handleTabChange = (tabKey) => {
  cambiarTab(tabKey)
  navigationStore.setAgenteAduaneroSubOption(tabKey)
  if (route.query.tab !== tabKey) {
    router.push({ path: '/agente-aduanero', query: { tab: tabKey } })
  }
}

// Handlers de Modales de Importación
const openTransitoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showTransitoModal.value = true
}

const onTransitoSaved = (actualizado) => {
  showTransitoModal.value = false
  showSuccess(`Tránsito y datos de embarque actualizados para ${actualizado?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openDiligenciaModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showDiligenciaModal.value = true
}

const onDiligenciaSaved = (actualizado) => {
  showDiligenciaModal.value = false
  showSuccess(`Acta de diligencia registrada con éxito para ${actualizado?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openSolicitudTransporteModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showSolicitudTransporteModal.value = true
}

const onSolicitudTransporteSaved = () => {
  showSolicitudTransporteModal.value = false
  showSuccess('Solicitud de despacho terrestre enviada a Flota y Transporte exitosamente.')
  loadData()
}

const openRetiroDirectoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRetiroDirectoModal.value = true
}

const onRetiroDirectoSaved = () => {
  showRetiroDirectoModal.value = false
  showSuccess('Acta de retiro directo en terminal registrada con éxito.')
  loadData()
}

const openIngresoAlmacenModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showIngresoModal.value = true
}

const onIngresoSaved = (creado) => {
  showIngresoModal.value = false
  showSuccess(`Ingreso VGM y custodia de depósito registrado: ${creado?.ticketBalanzaVgm || ''}`)
  loadData()
}

// Handlers de Exportación
const openRecojoExportacionModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRecojoExportacionModal.value = true
}

const onRecojoExportacionSaved = (act) => {
  showRecojoExportacionModal.value = false
  showSuccess(`Recojo en planta programado exitosamente para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openIngresoOrigenModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showIngresoOrigenModal.value = true
}

const onIngresoOrigenSaved = (act) => {
  showIngresoOrigenModal.value = false
  showSuccess(`Ingreso a terminal / zona primaria confirmado para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openEmbarqueModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showEmbarqueModal.value = true
}

const onEmbarqueSaved = (act) => {
  showEmbarqueModal.value = false
  showSuccess(`Embarque internacional confirmado para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openRecepcionExteriorModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRecepcionExteriorModal.value = true
}

const onRecepcionExteriorSaved = (act) => {
  showRecepcionExteriorModal.value = false
  showSuccess(`Recepción en destino exterior confirmada para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

// Handlers de Detalle y Documentos
const openDetalleExp = (exp) => {
  selectedExpedienteParaDetalle.value = exp
  showDetalleModal.value = true
}

const openDocumentosExpediente = (exp) => {
  if (!exp) return
  selectedDocEntidadId.value = exp.idExpediente
  selectedDocCodigoEntidad.value = exp.codigoExpediente || `EXP-${exp.idExpediente}`
  selectedDocTituloEntidad.value = `${exp.codigoExpediente} — ${exp.clienteRazonSocial || 'Cliente'}`
  showDocumentosModal.value = true
}

// Observar cambio de selector de agente en supervisores
watch(selectedAgenteId, () => {
  loadData()
})

// Observar cambio de query param en ruta
watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab && ['todos', 'transito', 'inspeccion', 'levante'].includes(newTab)) {
      cambiarTab(newTab)
      navigationStore.setAgenteAduaneroSubOption(newTab)
    }
  },
  { immediate: true }
)

// Observar errores en composable
watch(errorMessage, (msg) => {
  if (msg) showError(msg)
})

onMounted(() => {
  navigationStore.setActiveModule('agente-aduanero')
  loadAgentes()
  loadData()
})
</script>

<style scoped>
.agente-portal-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 0.5rem;
}

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
