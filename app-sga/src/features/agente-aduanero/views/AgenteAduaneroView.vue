<template>
  <div class="agente-portal-view fade-in">
    <!-- CASO 0A: INFORME DETALLADO A PANTALLA COMPLETA -->
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

    <!-- CASO 0B: VISTA OPERATIVA DE TRÁNSITO / SALIDA / ARRIBO -->
    <AgenteOperacionTransitoView
      v-else-if="selectedExpedienteParaOperacionTransito"
      :expediente="selectedExpedienteParaOperacionTransito"
      :initial-mode="transitoOperacionMode"
      @back="cerrarOperacionTransito"
      @saved="onOperacionTransitoSaved"
    />

    <!-- CASO A: DASHBOARD OPERATIVO DEL AGENTE -->
    <AgenteDashboardPanel
      v-else-if="activeTab === 'dashboard'"
      :expedientes="expedientes"
      :selectedAgenteId="selectedAgenteId"
      :agentesList="agentesList"
      :esSupervisorOAdmin="esSupervisorOAdmin"
      :loading="loading"
      @navigate="handleTabChange"
      @inspect="openInformeCompleto"
      @refresh="loadData"
      @update:selectedAgenteId="selectedAgenteId = $event"
    />

    <!-- CASO B: BANDEJA UNIFICADA sgaUI (todos | transito | inspeccion | levante) -->
    <AgenteBandejaPanel
      v-else
      :expedientes="filteredExpedientes"
      :kpiCounts="kpiCounts"
      :activeTab="activeTab"
      v-model:searchQuery="searchQuery"
      v-model:filterRegimen="filterRegimen"
      v-model:filterCanal="filterCanal"
      v-model:sortBy="sortBy"
      v-model:selectedAgenteId="selectedAgenteId"
      :agentesList="agentesList"
      :esSupervisorOAdmin="esSupervisorOAdmin"
      :hasActiveFilters="hasActiveFilters"
      :loading="loading"
      @select-tab="handleTabChange"
      @reset-filters="resetFilters"
      @refresh="loadData"
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
      :style="{ width: '480px', maxWidth: '95vw' }"
    >
      <div class="sga-form-field">
        <label for="manual-estado-titulo" class="sga-form-label">Estado / Hito *</label>
        <InputText id="manual-estado-titulo" v-model.trim="manualEstadoForm.titulo" placeholder="Ej. Revisión Aduana / En Espera de Tránsito" size="small" fluid />
      </div>
      <div class="sga-form-field" style="margin-top: 0.75rem;">
        <label for="manual-estado-desc" class="sga-form-label">Detalle / Observación</label>
        <Textarea id="manual-estado-desc" v-model.trim="manualEstadoForm.descripcion" rows="3" size="small" fluid />
      </div>
      <template #footer>
        <Button type="button" label="Cancelar" severity="secondary" outlined size="small" @click="showMarcarEstadoModal = false" />
        <Button type="button" label="Guardar Marca" icon="pi pi-check" size="small" :disabled="!manualEstadoForm.titulo" @click="submitMarcarEstadoCustom" />
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
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import { useNotification } from '@/shared/composables/useNotification'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { expedienteService } from '@/shared/services/expedienteService'
import { downloadBlob } from '@/shared/utils/download'
import { useAgenteBandeja } from '../composables/useAgenteBandeja'

import AgenteDashboardPanel from '../components/AgenteDashboardPanel.vue'
import AgenteBandejaPanel from '../components/AgenteBandejaPanel.vue'
import AgenteExpedienteInformeView from '../components/AgenteExpedienteInformeView.vue'
import AgenteOperacionTransitoView from '../components/AgenteOperacionTransitoView.vue'
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

const VALID_TABS = ['dashboard', 'todos', 'expedientes', 'transito', 'inspeccion', 'levante']

const route = useRoute()
const router = useRouter()
const navigationStore = useNavigationStore()
const { showSuccess, showError } = useNotification()

const selectedExpedienteParaInforme = ref(null)
const selectedExpedienteParaOperacionTransito = ref(null)
const transitoOperacionMode = ref('salida')
const previoEstabaEnInforme = ref(false)

const {
  loading,
  errorMessage,
  expedientes,
  agentesList,
  activeTab,
  filterRegimen,
  filterCanal,
  searchQuery,
  sortBy,
  selectedAgenteId,
  esSupervisorOAdmin,
  hasActiveFilters,
  resetFilters,
  kpiCounts,
  filteredExpedientes,
  loadAgentes,
  loadData,
  cambiarTab
} = useAgenteBandeja()

const selectedExpedienteParaAccion = ref(null)

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

const clearFullScreenViews = () => {
  selectedExpedienteParaInforme.value = null
  selectedExpedienteParaOperacionTransito.value = null
  previoEstabaEnInforme.value = false
}

const openInformeCompleto = (exp) => {
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

const handleTabChange = (tabKey) => {
  clearFullScreenViews()
  const canonical = tabKey === 'expedientes' ? 'todos' : tabKey
  cambiarTab(canonical)
  navigationStore.setAgenteAduaneroSubOption(canonical)
  if (route.query.tab !== canonical) {
    router.push({
      path: '/agente-aduanero',
      query: canonical === 'dashboard' ? {} : { tab: canonical }
    })
  }
}

const openSalidaOrigenView = (exp) => {
  previoEstabaEnInforme.value = Boolean(selectedExpedienteParaInforme.value)
  selectedExpedienteParaInforme.value = null
  selectedExpedienteParaOperacionTransito.value = exp
  transitoOperacionMode.value = 'salida'
}

const openArriboDestinoView = (exp) => {
  previoEstabaEnInforme.value = Boolean(selectedExpedienteParaInforme.value)
  selectedExpedienteParaInforme.value = null
  selectedExpedienteParaOperacionTransito.value = exp
  transitoOperacionMode.value = 'arribo'
}

const openTransitoView = (exp) => {
  previoEstabaEnInforme.value = Boolean(selectedExpedienteParaInforme.value)
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

const onOperacionTransitoSaved = async (actualizado) => {
  const exp = actualizado || selectedExpedienteParaOperacionTransito.value
  selectedExpedienteParaOperacionTransito.value = null

  if (previoEstabaEnInforme.value && exp) {
    selectedExpedienteParaInforme.value = exp
  }
  previoEstabaEnInforme.value = false

  if (transitoOperacionMode.value === 'salida') {
    showSuccess(
      `Salida de origen confirmada para ${actualizado?.codigoExpediente || 'el despacho'}. Expediente en tránsito internacional.`
    )
  } else if (transitoOperacionMode.value === 'arribo') {
    showSuccess(
      `Arribo a puerto de destino confirmado para ${actualizado?.codigoExpediente || 'el expediente'}. Habilitado para asignación de Canal SUNAT.`
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
    clearFullScreenViews()
    if (newTab && VALID_TABS.includes(newTab)) {
      const canonical = newTab === 'expedientes' ? 'todos' : newTab
      cambiarTab(canonical)
      navigationStore.setAgenteAduaneroSubOption(canonical)
    } else if (!newTab) {
      cambiarTab('dashboard')
      navigationStore.setAgenteAduaneroSubOption('dashboard')
    }
  },
  { immediate: true }
)

watch(
  () => [navigationStore.agenteAduaneroSubOption, navigationStore.navTick],
  ([newSub]) => {
    clearFullScreenViews()
    if (newSub && VALID_TABS.includes(newSub)) {
      const canonical = newSub === 'expedientes' ? 'todos' : newSub
      if (activeTab.value !== canonical) {
        cambiarTab(canonical)
      }
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
