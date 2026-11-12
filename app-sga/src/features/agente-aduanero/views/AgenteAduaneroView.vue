<template>
  <div class="agente-portal-view fade-in">
    <!-- ============================================================== -->
    <!-- CASO 0A: INFORME DETALLADO A PANTALLA COMPLETA (FULL-SCREEN)   -->
    <!-- ============================================================== -->
    <AgenteExpedienteInformeView
      v-if="selectedExpedienteParaInforme"
      :expediente="selectedExpedienteParaInforme"
      @back="cerrarInformeCompleto"
      @salida-origen="openSalidaOrigenView"
      @arribo-destino="openArriboDestinoView"
      @transito="openTransitoView"
      @diligencia="openDiligenciaModal"
      @solicitar-transporte="openSolicitudTransporteModal"
      @retiro-directo="openRetiroDirectoModal"
      @ingreso-almacen="openIngresoAlmacenModal"
      @recojo-exportacion="openRecojoExportacionModal"
      @ingreso-origen="openIngresoOrigenModal"
      @embarque="openEmbarqueModal"
      @recepcion-exterior="openRecepcionExteriorModal"
      @documentos="openDocumentosExpediente"
    />

    <!-- ============================================================== -->
    <!-- CASO 0B: VISTA OPERATIVA DE TRÁNSITO / SALIDA / ARRIBO (FULL)  -->
    <!-- ============================================================== -->
    <AgenteOperacionTransitoView
      v-else-if="selectedExpedienteParaOperacionTransito"
      :expediente="selectedExpedienteParaOperacionTransito"
      :initial-mode="transitoOperacionMode"
      @back="cerrarOperacionTransito"
      @saved="onOperacionTransitoSaved"
    />

    <!-- ============================================================== -->
    <!-- CASO A: DASHBOARD INICIAL DEL AGENTE (SOLO AQUÍ ESTÁN LOS KPIS)-->
    <!-- ============================================================== -->
    <AgenteDashboardPanel
      v-else-if="activeTab === 'dashboard'"
      :expedientes="expedientes"
      :counts="kpiCounts"
      :selectedAgenteId="selectedAgenteId"
      :agentesList="agentesList"
      :esSupervisorOAdmin="esSupervisorOAdmin"
      :loading="loading"
      @navigate="handleTabChange"
      @refresh="loadData"
      @update:selectedAgenteId="selectedAgenteId = $event"
    />

    <!-- ============================================================== -->
    <!-- CASO B: BANDEJA DE EXPEDIENTES (EXPEDIENTES, TRÁNSITO, AFORO, LEVANTE) -->
    <!-- ============================================================== -->
    <template v-else>
      <!-- 1. Barra de Herramientas, Conmutador Canvas/Tabla y Filtros Superiores -->
      <AgenteHeaderToolbar
        v-model:viewMode="viewMode"
        v-model:searchQuery="searchQuery"
        v-model:filterRegimen="filterRegimen"
        v-model:filterCanal="filterCanal"
        v-model:selectedAgenteId="selectedAgenteId"
        :agentesList="agentesList"
        :esSupervisorOAdmin="esSupervisorOAdmin"
        :loading="loading"
        @refresh="loadData"
      />

      <!-- 2. Vista Principal: Canvas de Tarjetas (Por defecto) -->
      <AgenteExpedientesCanvas
        v-if="viewMode === 'canvas'"
        :expedientes="filteredExpedientes"
        :loading="loading"
        :active-tab="activeTab"
        @detalle="openInformeCompleto"
        @salida-origen="openSalidaOrigenView"
        @arribo-destino="openArriboDestinoView"
        @transito="openTransitoView"
        @diligencia="openDiligenciaModal"
        @solicitar-transporte="openSolicitudTransporteModal"
        @retiro-directo="openRetiroDirectoModal"
        @ingreso-almacen="openIngresoAlmacenModal"
        @recojo-exportacion="openRecojoExportacionModal"
        @ingreso-origen="openIngresoOrigenModal"
        @embarque="openEmbarqueModal"
        @recepcion-exterior="openRecepcionExteriorModal"
        @documentos="openDocumentosExpediente"
      />

      <!-- 3. Vista Alternativa: Tabla Detallada -->
      <AgenteExpedientesTable
        v-else
        :expedientes="filteredExpedientes"
        :loading="loading"
        :active-tab="activeTab"
        @detalle="openInformeCompleto"
        @salida-origen="openSalidaOrigenView"
        @arribo-destino="openArriboDestinoView"
        @transito="openTransitoView"
        @diligencia="openDiligenciaModal"
        @solicitar-transporte="openSolicitudTransporteModal"
        @retiro-directo="openRetiroDirectoModal"
        @ingreso-almacen="openIngresoAlmacenModal"
        @recojo-exportacion="openRecojoExportacionModal"
        @ingreso-origen="openIngresoOrigenModal"
        @embarque="openEmbarqueModal"
        @recepcion-exterior="openRecepcionExteriorModal"
        @documentos="openDocumentosExpediente"
      />
    </template>

    <!-- ============================================================== -->
    <!-- MODALES OPERATIVOS AUXILIARES (IMPORTACIÓN Y COMUNES)           -->
    <!-- ============================================================== -->
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
    <!-- GESTOR DE DOCUMENTOS DIGITALES ADJUNTOS                        -->
    <!-- ============================================================== -->
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
import AgenteDashboardPanel from '../components/AgenteDashboardPanel.vue'
import AgenteHeaderToolbar from '../components/AgenteHeaderToolbar.vue'
import AgenteExpedientesCanvas from '../components/AgenteExpedientesCanvas.vue'
import AgenteExpedientesTable from '../components/AgenteExpedientesTable.vue'
import AgenteExpedienteInformeView from '../components/AgenteExpedienteInformeView.vue'
import AgenteOperacionTransitoView from '../components/AgenteOperacionTransitoView.vue'

// Modales operativos compartidos
import DiligenciaCampoModal from '@/features/aduanas/components/DiligenciaCampoModal.vue'
import SolicitudTransporteModal from '@/features/aduanas/components/SolicitudTransporteModal.vue'
import RetiroDirectoModal from '@/features/aduanas/components/RetiroDirectoModal.vue'
import IngresoAlmacenModal from '@/features/almacen/components/IngresoAlmacenModal.vue'
import SolicitudRecojoExportacionModal from '@/features/aduanas/components/SolicitudRecojoExportacionModal.vue'
import ConfirmarIngresoOrigenModal from '@/features/aduanas/components/ConfirmarIngresoOrigenModal.vue'
import ConfirmarEmbarqueExportacionModal from '@/features/aduanas/components/ConfirmarEmbarqueExportacionModal.vue'
import RecepcionDestinoExteriorModal from '@/features/aduanas/components/RecepcionDestinoExteriorModal.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'

const route = useRoute()
const router = useRouter()
const navigationStore = useNavigationStore()
const { showSuccess, showError } = useNotification()

// Modo de visualización: 'canvas' (por defecto) o 'table'
const viewMode = ref('canvas')

// Expediente activo para el informe a pantalla completa
const selectedExpedienteParaInforme = ref(null)

// Expediente activo para operaciones de tránsito a pantalla completa
const selectedExpedienteParaOperacionTransito = ref(null)
const transitoOperacionMode = ref('salida')
const previoEstabaEnInforme = ref(false)

// Composable central
const {
  loading,
  errorMessage,
  expedientes,
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

const showDiligenciaModal = ref(false)
const showSolicitudTransporteModal = ref(false)
const showRetiroDirectoModal = ref(false)
const showIngresoModal = ref(false)
const showRecojoExportacionModal = ref(false)
const showIngresoOrigenModal = ref(false)
const showEmbarqueModal = ref(false)
const showRecepcionExteriorModal = ref(false)

// Documentos
const showDocumentosModal = ref(false)
const selectedDocEntidadId = ref(null)
const selectedDocCodigoEntidad = ref('')
const selectedDocTituloEntidad = ref('')

// Navegación de informe a pantalla completa
const openInformeCompleto = (exp) => {
  selectedExpedienteParaInforme.value = exp
}

const cerrarInformeCompleto = () => {
  selectedExpedienteParaInforme.value = null
}

// Sincronizar actualización en el informe completo si está abierto
const syncExpedienteEnInforme = (actualizado) => {
  if (selectedExpedienteParaInforme.value && actualizado?.idExpediente === selectedExpedienteParaInforme.value.idExpediente) {
    selectedExpedienteParaInforme.value = { ...selectedExpedienteParaInforme.value, ...actualizado }
  }
}

// Sincronización de pestañas con router y navigationStore
const handleTabChange = (tabKey) => {
  selectedExpedienteParaInforme.value = null
  selectedExpedienteParaOperacionTransito.value = null
  previoEstabaEnInforme.value = false
  cambiarTab(tabKey)
  navigationStore.setAgenteAduaneroSubOption(tabKey)
  if (route.query.tab !== tabKey) {
    router.push({ path: '/agente-aduanero', query: { tab: tabKey } })
  }
}

// Handlers de Operaciones de Tránsito a Pantalla Completa
const openSalidaOrigenView = (exp) => {
  previoEstabaEnInforme.value = !!selectedExpedienteParaInforme.value
  selectedExpedienteParaInforme.value = null
  selectedExpedienteParaOperacionTransito.value = exp
  transitoOperacionMode.value = 'salida'
}

const openArriboDestinoView = (exp) => {
  previoEstabaEnInforme.value = !!selectedExpedienteParaInforme.value
  selectedExpedienteParaInforme.value = null
  selectedExpedienteParaOperacionTransito.value = exp
  transitoOperacionMode.value = 'arribo'
}

const openTransitoView = (exp) => {
  previoEstabaEnInforme.value = !!selectedExpedienteParaInforme.value
  selectedExpedienteParaInforme.value = null
  selectedExpedienteParaOperacionTransito.value = exp
  transitoOperacionMode.value = 'transito'
}

const cerrarOperacionTransito = () => {
  const exp = selectedExpedienteParaOperacionTransito.value
  selectedExpedienteParaOperacionTransito.value = null
  if (previoEstabaEnInforme.value && exp) {
    selectedExpedienteParaInforme.value = exp
  }
  previoEstabaEnInforme.value = false
}

const onOperacionTransitoSaved = (actualizado) => {
  const exp = actualizado || selectedExpedienteParaOperacionTransito.value
  selectedExpedienteParaOperacionTransito.value = null

  if (previoEstabaEnInforme.value && exp) {
    selectedExpedienteParaInforme.value = exp
  }
  previoEstabaEnInforme.value = false

  syncExpedienteEnInforme(actualizado)

  if (transitoOperacionMode.value === 'salida') {
    showSuccess(`Salida de origen confirmada para ${actualizado?.codigoExpediente || 'el despacho'}. Expediente en tránsito internacional.`)
  } else if (transitoOperacionMode.value === 'arribo') {
    showSuccess(`Arribo a puerto de destino confirmado para ${actualizado?.codigoExpediente || 'el expediente'}. Habilitado para asignación de Canal SUNAT.`)
  } else {
    showSuccess(`Tránsito y datos de embarque actualizados para ${actualizado?.codigoExpediente || 'el expediente'}.`)
  }
  loadData()
}

const openDiligenciaModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showDiligenciaModal.value = true
}

const onDiligenciaSaved = (actualizado) => {
  showDiligenciaModal.value = false
  syncExpedienteEnInforme(actualizado)
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
  syncExpedienteEnInforme(act)
  showSuccess(`Recojo en planta programado exitosamente para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openIngresoOrigenModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showIngresoOrigenModal.value = true
}

const onIngresoOrigenSaved = (act) => {
  showIngresoOrigenModal.value = false
  syncExpedienteEnInforme(act)
  showSuccess(`Ingreso a terminal / zona primaria confirmado para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openEmbarqueModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showEmbarqueModal.value = true
}

const onEmbarqueSaved = (act) => {
  showEmbarqueModal.value = false
  syncExpedienteEnInforme(act)
  showSuccess(`Embarque internacional confirmado para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openRecepcionExteriorModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRecepcionExteriorModal.value = true
}

const onRecepcionExteriorSaved = (act) => {
  showRecepcionExteriorModal.value = false
  syncExpedienteEnInforme(act)
  showSuccess(`Recepción en destino exterior confirmada para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

// Handlers de Documentos
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
    selectedExpedienteParaInforme.value = null
    if (newTab && ['dashboard', 'todos', 'expedientes', 'transito', 'inspeccion', 'levante'].includes(newTab)) {
      cambiarTab(newTab === 'expedientes' ? 'todos' : newTab)
      navigationStore.setAgenteAduaneroSubOption(newTab)
    } else if (!newTab) {
      cambiarTab('dashboard')
      navigationStore.setAgenteAduaneroSubOption('dashboard')
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
