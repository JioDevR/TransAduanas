<template>
  <div class="agente-portal-view fade-in">
    <!-- ESTACIÓN DE CONTROL DE HITOS / FICHA DEL EXPEDIENTE -->
    <AgenteExpedienteInformeView
      v-if="selectedExpedienteParaInforme"
      :expediente="selectedExpedienteParaInforme"
      :readonly="informeReadonlyMode"
      :open-expedientes="openExpedientes"
      @back="cerrarInformeCompleto"
      @switch-to-manage="handleSwitchToManage"
      @select-expediente="openInformeGestion"
      @salida-origen="openSalidaOrigenModal"
      @arribo-destino="openArriboDestinoModal"
      @transito="openTransitoModal"
      @diligencia="openDiligenciaModal"
      @solicitar-transporte="openSolicitudTransporteModal"
      @retiro-directo="openRetiroDirectoModal"
      @ingreso-almacen="openIngresoAlmacenModal"
      @recojo-exportacion="openRecojoExportacionModal"
      @ingreso-origen="openIngresoOrigenModal"
      @embarque="openEmbarqueModal"
      @recepcion-exterior="openRecepcionExteriorModal"
      @verificacion-origen="handleVerificacionOrigen"
      @cuadrilla-rojo="openCuadrillaRojoModal"
      @finalizar-rojo="handleFinalizarRojo"
      @enviar-sunat="openSunatNaranjaModal"
      @entregado-almacen-verde="handleEntregadoAlmacenVerde"
      @informe-conformidad="openInformeConformidadModal"
      @descargar-informe="handleDescargarInforme"
      @marcar-estado="handleMarcarEstadoRapido"
      @marcar-estado-custom="openMarcarEstadoModal"
      @finalizar-expediente="openFinalizarModal"
      @documentos="openDocumentosExpediente"
    />

    <!-- VISTA 01: DASHBOARD OPERATIVO DEL AGENTE -->
    <AgenteDashboardPanel
      v-else-if="activeView === 'dashboard'"
      :expedientes="expedientes"
      :selectedAgenteId="selectedAgenteId"
      :agentesList="agentesList"
      :esSupervisorOAdmin="esSupervisorOAdmin"
      :loading="loading"
      @navigate="handleNavigateFromDashboard"
      @inspect="handleSwitchToManage"
      @refresh="loadData"
      @update:selectedAgenteId="selectedAgenteId = $event"
    />

    <!-- VISTA 02: EXPEDIENTES ASIGNADOS (SOLO LECTURA · CANVAS) -->
    <!-- VISTA 03: GESTIÓN DE EXPEDIENTES (CANVAS SIMPLIFICADO + HITOS POR EXPEDIENTE) -->
    <AgenteBandejaPanel
      v-else
      :mode="activeView === 'gestion' ? 'manage' : 'readonly'"
      :expedientes="filteredExpedientes"
      :kpiCounts="activeView === 'gestion' ? gestionKpiCounts : kpiCounts"
      v-model:phaseFilter="phaseFilter"
      v-model:searchQuery="searchQuery"
      v-model:filterRegimen="filterRegimen"
      v-model:filterCanal="filterCanal"
      v-model:sortBy="sortBy"
      v-model:selectedAgenteId="selectedAgenteId"
      :agentesList="agentesList"
      :esSupervisorOAdmin="esSupervisorOAdmin"
      :hasActiveFilters="hasActiveFilters"
      :loading="loading"
      @reset-filters="resetFilters"
      @refresh="loadData"
      @detalle="handleOpenFromBandeja"
      @documentos="openDocumentosExpediente"
    />

    <!-- MODAL UNIFICADO DE SALIDA DE ORIGEN / TRÁNSITO / ARRIBO -->
    <TransitoOperacionModal
      :visible="showTransitoModal"
      :expediente="selectedExpedienteParaAccion"
      :initial-mode="transitoOperacionMode"
      @close="showTransitoModal = false"
      @saved="onOperacionTransitoSaved"
    />

    <!-- MODALES OPERATIVOS (IMPORTACIÓN Y COMUNES) -->
    <DiligenciaCampoModal
      :visible="showDiligenciaModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showDiligenciaModal = false"
      @saved="onDiligenciaSaved"
    />

    <SolicitudTransporteModal
      :visible="showSolicitudTransporteModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showSolicitudTransporteModal = false"
      @saved="onSolicitudTransporteSaved"
    />

    <RetiroDirectoModal
      :visible="showRetiroDirectoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRetiroDirectoModal = false"
      @saved="onRetiroDirectoSaved"
    />

    <IngresoAlmacenModal
      :visible="showIngresoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showIngresoModal = false"
      @saved="onIngresoSaved"
    />

    <!-- MODALES OPERATIVOS DE EXPORTACIÓN -->
    <SolicitudRecojoExportacionModal
      :visible="showRecojoExportacionModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRecojoExportacionModal = false"
      @saved="onRecojoExportacionSaved"
    />

    <ConfirmarIngresoOrigenModal
      :visible="showIngresoOrigenModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showIngresoOrigenModal = false"
      @saved="onIngresoOrigenSaved"
    />

    <ConfirmarEmbarqueExportacionModal
      :visible="showEmbarqueModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showEmbarqueModal = false"
      @saved="onEmbarqueSaved"
    />

    <RecepcionDestinoExteriorModal
      :visible="showRecepcionExteriorModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRecepcionExteriorModal = false"
      @saved="onRecepcionExteriorSaved"
    />

    <!-- MODALES DE CANALES SUNAT, CONFORMIDAD Y CIERRE -->
    <CanalRojoCitaModal
      :visible="showCuadrillaRojoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showCuadrillaRojoModal = false"
      @saved="onCuadrillaRojoSaved"
    />

    <CanalNaranjaSunatModal
      :visible="showSunatNaranjaModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showSunatNaranjaModal = false"
      @saved="onSunatNaranjaSaved"
    />

    <InformeConformidadModal
      :visible="showInformeConformidadModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showInformeConformidadModal = false"
      @saved="onInformeConformidadSaved"
    />

    <CierreExpedienteModal
      :visible="showCierreModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showCierreModal = false"
      @saved="onCierreSaved"
    />

    <Dialog
      v-model:visible="showMarcarEstadoModal"
      modal
      header="Registrar Estado y Marca de Tiempo"
      :style="{ width: getDialogWidth('500px'), maxWidth: '95vw' }"
    >
      <form id="marcar-estado-form" class="sga-dialog-body" @submit.prevent="submitMarcarEstadoCustom">
        <div v-if="selectedExpediente" class="sga-dialog-context">
          <div class="sga-dialog-context-meta">
            <span class="sga-code-cell font-mono">{{ selectedExpediente.codigoExpediente }}</span>
            <span class="sga-dialog-context-sub">
              {{ selectedExpediente.clienteRazonSocial || 'Expediente aduanero' }}
            </span>
          </div>
          <Tag
            :value="selectedExpediente.estadoExpediente || 'EN_PROCESO'"
            severity="info"
            class="sga-mini-tag"
            rounded
          />
        </div>

        <div class="sga-form-field">
          <label for="manual-estado-titulo" class="sga-form-label">Estado / Hito *</label>
          <InputText
            id="manual-estado-titulo"
            v-model.trim="manualEstadoForm.titulo"
            placeholder="Ej. Revisión Aduana / En Espera de Tránsito"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="manual-estado-desc" class="sga-form-label">Detalle / Observación</label>
          <Textarea
            id="manual-estado-desc"
            v-model.trim="manualEstadoForm.descripcion"
            rows="3"
            autoResize
            placeholder="Describa la actualización operativa o marca de tiempo a registrar..."
            size="small"
            fluid
          />
        </div>
      </form>
      <template #footer>
        <Button type="button" label="Cancelar" severity="secondary" outlined size="small" @click="showMarcarEstadoModal = false" />
        <Button type="submit" form="marcar-estado-form" label="Guardar Marca" icon="pi pi-check" size="small" :disabled="!manualEstadoForm.titulo" />
      </template>
    </Dialog>

    <!-- GESTOR DE DOCUMENTOS DIGITALES ADJUNTOS -->
    <DocumentosManagerModal
      :visible="showDocumentosModal"
      entidad-tipo="EXPEDIENTE"
      :entidad-id="selectedDocEntidadId"
      :codigo-entidad="selectedDocCodigoEntidad"
      :titulo-entidad="selectedDocTituloEntidad"
      @close="showDocumentosModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { useNotification } from '@/shared/composables/useNotification'
import { useResponsive } from '@/shared/composables/useResponsive'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { expedienteService } from '@/shared/services/expedienteService'
import { downloadBlob } from '@/shared/utils/download'
import { useAgenteBandeja } from '../composables/useAgenteBandeja'

import AgenteDashboardPanel from '../components/AgenteDashboardPanel.vue'
import AgenteBandejaPanel from '../components/AgenteBandejaPanel.vue'
import AgenteExpedienteInformeView from '../components/AgenteExpedienteInformeView.vue'
import TransitoOperacionModal from '../components/TransitoOperacionModal.vue'
import CanalRojoCitaModal from '../components/CanalRojoCitaModal.vue'
import CanalNaranjaSunatModal from '../components/CanalNaranjaSunatModal.vue'
import InformeConformidadModal from '../components/InformeConformidadModal.vue'

import DiligenciaCampoModal from '@/features/aduanas/components/DiligenciaCampoModal.vue'
import SolicitudTransporteModal from '@/features/aduanas/components/SolicitudTransporteModal.vue'
import RetiroDirectoModal from '@/features/aduanas/components/RetiroDirectoModal.vue'
import IngresoAlmacenModal from '@/features/almacen/components/IngresoAlmacenModal.vue'
import SolicitudRecojoExportacionModal from '@/features/aduanas/components/SolicitudRecojoExportacionModal.vue'
import ConfirmarIngresoOrigenModal from '@/features/aduanas/components/ConfirmarIngresoOrigenModal.vue'
import ConfirmarEmbarqueExportacionModal from '@/features/aduanas/components/ConfirmarEmbarqueExportacionModal.vue'
import RecepcionDestinoExteriorModal from '@/features/aduanas/components/RecepcionDestinoExteriorModal.vue'
import CierreExpedienteModal from '@/features/aduanas/components/CierreExpedienteModal.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'

const VALID_QUERY_TABS = ['dashboard', 'expedientes', 'gestion', 'todos', 'transito', 'inspeccion', 'levante', 'cerrados']

const route = useRoute()
const router = useRouter()
const navigationStore = useNavigationStore()
const { showSuccess, showError } = useNotification()
const { getDialogWidth } = useResponsive()

const selectedExpedienteParaInforme = ref(null)

const {
  loading,
  errorMessage,
  expedientes,
  openExpedientes,
  agentesList,
  activeView,
  phaseFilter,
  filterRegimen,
  filterCanal,
  searchQuery,
  sortBy,
  selectedAgenteId,
  esSupervisorOAdmin,
  hasActiveFilters,
  resetFilters,
  kpiCounts,
  gestionKpiCounts,
  filteredExpedientes,
  loadAgentes,
  loadData,
  cambiarVista
} = useAgenteBandeja()

const informeReadonlyMode = computed(() => activeView.value !== 'gestion')

const selectedExpedienteParaAccion = ref(null)

const showTransitoModal = ref(false)
const transitoOperacionMode = ref('salida')
const showDiligenciaModal = ref(false)
const showSolicitudTransporteModal = ref(false)
const showRetiroDirectoModal = ref(false)
const showIngresoModal = ref(false)
const showRecojoExportacionModal = ref(false)
const showIngresoOrigenModal = ref(false)
const showEmbarqueModal = ref(false)
const showRecepcionExteriorModal = ref(false)
const showCuadrillaRojoModal = ref(false)
const showSunatNaranjaModal = ref(false)
const showInformeConformidadModal = ref(false)
const showCierreModal = ref(false)
const showMarcarEstadoModal = ref(false)
const manualEstadoForm = ref({ titulo: '', descripcion: '' })

const showDocumentosModal = ref(false)
const selectedDocEntidadId = ref(null)
const selectedDocCodigoEntidad = ref('')
const selectedDocTituloEntidad = ref('')

const handleOpenFromBandeja = (exp) => {
  selectedExpedienteParaInforme.value = exp
}

const openInformeGestion = (exp) => {
  selectedExpedienteParaInforme.value = exp
}

const handleSwitchToManage = (exp) => {
  cambiarVista('gestion')
  navigationStore.setAgenteAduaneroSubOption('gestion')
  if (route.query.tab !== 'gestion') {
    router.push({ path: '/agente-aduanero', query: { tab: 'gestion' } })
  }
  selectedExpedienteParaInforme.value = exp
}

const cerrarInformeCompleto = () => {
  selectedExpedienteParaInforme.value = null
}

const syncExpedienteEnInforme = (actualizado) => {
  if (
    selectedExpedienteParaInforme.value &&
    actualizado?.idExpediente === selectedExpedienteParaInforme.value.idExpediente
  ) {
    selectedExpedienteParaInforme.value = { ...selectedExpedienteParaInforme.value, ...actualizado }
  }
}

const refreshAndSync = async (actualizado = null) => {
  syncExpedienteEnInforme(actualizado)
  await loadData()
  if (selectedExpedienteParaInforme.value) {
    const fresh = (expedientes.value || []).find(
      (e) => e.idExpediente === selectedExpedienteParaInforme.value.idExpediente
    )
    if (fresh) selectedExpedienteParaInforme.value = fresh
  }
}

const handleNavigateFromDashboard = (target) => {
  selectedExpedienteParaInforme.value = null
  cambiarVista(target)
  const navOpt = target === 'dashboard' ? 'dashboard' : (target === 'gestion' ? 'gestion' : 'expedientes')
  navigationStore.setAgenteAduaneroSubOption(navOpt)
  const targetQueryTab = target === 'todos' ? 'expedientes' : target
  if (route.query.tab !== targetQueryTab) {
    router.push({
      path: '/agente-aduanero',
      query: target === 'dashboard' ? {} : { tab: targetQueryTab }
    })
  }
}

const openSalidaOrigenModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  transitoOperacionMode.value = 'salida'
  showTransitoModal.value = true
}

const openArriboDestinoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  transitoOperacionMode.value = 'arribo'
  showTransitoModal.value = true
}

const openTransitoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  transitoOperacionMode.value = 'transito'
  showTransitoModal.value = true
}

const onOperacionTransitoSaved = async ({ mode, data: actualizado }) => {
  showTransitoModal.value = false
  if (mode === 'salida') {
    showSuccess(
      `Salida de origen confirmada para ${actualizado?.codigoExpediente || 'el despacho'}. Ya visible en Tracking Portal B2B.`
    )
  } else if (mode === 'arribo') {
    showSuccess(
      `Arribo a puerto de destino confirmado para ${actualizado?.codigoExpediente || 'el expediente'}. Habilitado para Canal SUNAT.`
    )
  } else {
    showSuccess(`Tránsito y datos de embarque actualizados para ${actualizado?.codigoExpediente || 'el expediente'}.`)
  }
  await refreshAndSync(actualizado)
}

const openDiligenciaModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showDiligenciaModal.value = true
}

const onDiligenciaSaved = async (actualizado) => {
  showDiligenciaModal.value = false
  showSuccess(`Acta de diligencia registrada con éxito para ${actualizado?.codigoExpediente || 'el expediente'}.`)
  await refreshAndSync(actualizado)
}

const openSolicitudTransporteModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showSolicitudTransporteModal.value = true
}

const onSolicitudTransporteSaved = async (actualizado) => {
  showSolicitudTransporteModal.value = false
  showSuccess('Solicitud de despacho terrestre enviada a Flota y Transporte exitosamente.')
  await refreshAndSync(actualizado)
}

const openRetiroDirectoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRetiroDirectoModal.value = true
}

const onRetiroDirectoSaved = async (actualizado) => {
  showRetiroDirectoModal.value = false
  showSuccess('Acta de retiro directo en terminal registrada con éxito.')
  await refreshAndSync(actualizado)
}

const openIngresoAlmacenModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showIngresoModal.value = true
}

const onIngresoSaved = async (creado) => {
  showIngresoModal.value = false
  showSuccess(`Ingreso VGM y custodia de depósito registrado: ${creado?.ticketBalanzaVgm || ''}`)
  await refreshAndSync()
}

const openRecojoExportacionModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRecojoExportacionModal.value = true
}

const onRecojoExportacionSaved = async (act) => {
  showRecojoExportacionModal.value = false
  showSuccess(`Recojo en planta programado exitosamente para ${act?.codigoExpediente || 'el expediente'}.`)
  await refreshAndSync(act)
}

const openIngresoOrigenModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showIngresoOrigenModal.value = true
}

const onIngresoOrigenSaved = async (act) => {
  showIngresoOrigenModal.value = false
  showSuccess(`Ingreso a terminal / zona primaria confirmado para ${act?.codigoExpediente || 'el expediente'}.`)
  await refreshAndSync(act)
}

const openEmbarqueModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showEmbarqueModal.value = true
}

const onEmbarqueSaved = async (act) => {
  showEmbarqueModal.value = false
  showSuccess(`Embarque internacional confirmado para ${act?.codigoExpediente || 'el expediente'}.`)
  await refreshAndSync(act)
}

const openRecepcionExteriorModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRecepcionExteriorModal.value = true
}

const onRecepcionExteriorSaved = async (act) => {
  showRecepcionExteriorModal.value = false
  showSuccess(`Recepción en destino exterior confirmada para ${act?.codigoExpediente || 'el expediente'}.`)
  await refreshAndSync(act)
}

const handleVerificacionOrigen = async (exp) => {
  if (!exp?.idExpediente) return
  try {
    const act = await expedienteService.confirmarVerificacionOrigen(exp.idExpediente, {})
    showSuccess(`Verificación en origen confirmada para ${exp.codigoExpediente}.`)
    await refreshAndSync(act)
  } catch (err) {
    showError(err?.response?.data?.message || 'No se pudo confirmar verificación en origen.')
  }
}

const openCuadrillaRojoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showCuadrillaRojoModal.value = true
}

const onCuadrillaRojoSaved = async (act) => {
  showCuadrillaRojoModal.value = false
  showSuccess(`Cuadrilla y cita física registradas para ${act?.codigoExpediente || 'el expediente'}.`)
  await refreshAndSync(act)
}

const handleFinalizarRojo = async (exp) => {
  if (!exp?.idExpediente) return
  try {
    const act = await expedienteService.finalizarRevisionRojo(exp.idExpediente, {
      detalleInspeccion: exp.detalleInspeccion || 'Inspección física de Canal Rojo concluida conforme.'
    })
    showSuccess(`Revisión física Canal Rojo finalizada y levante autorizado para ${exp.codigoExpediente}.`)
    await refreshAndSync(act)
  } catch (err) {
    showError(err?.response?.data?.message || 'No se pudo finalizar la revisión física.')
  }
}

const openSunatNaranjaModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showSunatNaranjaModal.value = true
}

const onSunatNaranjaSaved = async (act) => {
  showSunatNaranjaModal.value = false
  showSuccess(`Documentos transmitidos a SUNAT (${act?.codigoTransmisionSunat || 'SIM'}). Levante habilitado.`)
  await refreshAndSync(act)
}

const handleEntregadoAlmacenVerde = async (exp) => {
  if (!exp?.idExpediente) return
  try {
    const act = await expedienteService.marcarEntregadoAlmacenVerde(exp.idExpediente, {})
    showSuccess(`Carga con Canal Verde marcada como entregada en almacén (${exp.codigoExpediente}).`)
    await refreshAndSync(act)
  } catch (err) {
    showError(err?.response?.data?.message || 'No se pudo marcar entrega en almacén.')
  }
}

const openInformeConformidadModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showInformeConformidadModal.value = true
}

const onInformeConformidadSaved = async (act) => {
  showInformeConformidadModal.value = false
  showSuccess(`Informe de conformidad de entrega emitido para ${act?.codigoExpediente || 'el expediente'}.`)
  await refreshAndSync(act)
}

const handleDescargarInforme = async (exp) => {
  if (!exp?.idExpediente) return
  try {
    const blob = await expedienteService.descargarInformeConformidadPdf(exp.idExpediente)
    downloadBlob(blob, `informe-conformidad-${exp.codigoExpediente}.pdf`, 'application/pdf')
  } catch {
    showError('No se pudo descargar el PDF del informe de conformidad.')
  }
}

const handleMarcarEstadoRapido = async ({ expediente, codigoEstado, titulo }) => {
  if (!expediente?.idExpediente) return
  try {
    const act = await expedienteService.registrarEventoManual(expediente.idExpediente, {
      codigoEstado,
      titulo,
      descripcion: `Estado marcado manualmente por Agente Aduanero: ${titulo}`
    })
    showSuccess(`Estado "${titulo}" registrado en ${expediente.codigoExpediente}.`)
    await refreshAndSync(act)
  } catch (err) {
    showError(err?.response?.data?.message || 'No se pudo registrar el estado.')
  }
}

const openMarcarEstadoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  manualEstadoForm.value = { titulo: '', descripcion: '' }
  showMarcarEstadoModal.value = true
}

const submitMarcarEstadoCustom = async () => {
  const exp = selectedExpedienteParaAccion.value
  if (!exp?.idExpediente || !manualEstadoForm.value.titulo) return
  try {
    const codigoEstado = manualEstadoForm.value.titulo
      .toUpperCase()
      .replace(/[^A-Z0-9]+/g, '_')
      .slice(0, 50)
    const act = await expedienteService.registrarEventoManual(exp.idExpediente, {
      codigoEstado: codigoEstado || 'HITO_MANUAL',
      titulo: manualEstadoForm.value.titulo,
      descripcion: manualEstadoForm.value.descripcion || null
    })
    showMarcarEstadoModal.value = false
    showSuccess(`Hito "${manualEstadoForm.value.titulo}" registrado.`)
    await refreshAndSync(act)
  } catch (err) {
    showError(err?.response?.data?.message || 'No se pudo guardar el hito manual.')
  }
}

const openFinalizarModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showCierreModal.value = true
}

const onCierreSaved = async (act) => {
  showCierreModal.value = false
  showSuccess(`Expediente ${act?.codigoExpediente || ''} finalizado con conformidad.`)
  await refreshAndSync(act)
}

const openDocumentosExpediente = (exp) => {
  if (!exp) return
  selectedDocEntidadId.value = exp.idExpediente
  selectedDocCodigoEntidad.value = exp.codigoExpediente || `EXP-${exp.idExpediente}`
  selectedDocTituloEntidad.value = `${exp.codigoExpediente} — ${exp.clienteRazonSocial || 'Cliente'}`
  showDocumentosModal.value = true
}

watch(selectedAgenteId, () => {
  loadData()
})

watch(
  () => route.query.tab,
  (newTab) => {
    selectedExpedienteParaInforme.value = null
    if (newTab && VALID_QUERY_TABS.includes(newTab)) {
      cambiarVista(newTab)
      const navOpt = newTab === 'dashboard' ? 'dashboard' : (newTab === 'gestion' ? 'gestion' : 'expedientes')
      navigationStore.setAgenteAduaneroSubOption(navOpt)
    } else {
      cambiarVista('dashboard')
      navigationStore.setAgenteAduaneroSubOption('dashboard')
    }
  },
  { immediate: true }
)

watch(
  () => [navigationStore.agenteAduaneroSubOption, navigationStore.navTick],
  ([newSub]) => {
    selectedExpedienteParaInforme.value = null
    if (newSub === 'dashboard') {
      if (activeView.value !== 'dashboard') cambiarVista('dashboard')
    } else if (newSub === 'gestion') {
      if (activeView.value !== 'gestion') cambiarVista('gestion')
    } else if (newSub) {
      if (activeView.value !== 'expedientes') cambiarVista('expedientes')
    }
  }
)

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
