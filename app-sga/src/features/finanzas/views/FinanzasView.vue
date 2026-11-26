<template>
  <div class="finanzas-view">
    <!-- Vista Split-Screen: Registro de Abono / Anticipo Pre-Expediente -->
    <RegistroAbonoView
      v-if="showRegistroAbono"
      :cotizacion="selectedCotizacionForPago"
      :cotizaciones="cotizacionesAprobadas"
      @cancel="closeRegistroAbono"
      @saved="handleAbonoSaved"
    />

    <!-- Vistas Operativas por Sub-Opción de Finanzas -->
    <div v-else class="finanzas-main">
      <FinanzasDashboardPanel
        v-if="activeTab === 'dashboard'"
        :cotizaciones-aprobadas="cotizacionesAprobadas"
        :anticipos-list="anticiposList"
        :liquidaciones-list="liquidacionesList"
        :expedientes-por-liquidar="expedientesPorLiquidar"
        :metricas="metricas"
        :loading="loading"
        @refresh="loadAllData"
        @navigate="switchTab"
        @registrar-abono="openRegistroAbono"
        @emitir-liquidacion="openEmitirModal"
        @ver-historial="openHistorialCotizacion"
      />

      <AbonosB2bPanel
        v-else-if="activeTab === 'abonos-b2b'"
        class="fade-in"
        :anticipos="anticiposList"
        :loading="loadingAnticipos"
        :initial-estado="abonosInitialEstado"
        @refresh="loadAllData"
        @preview-voucher="openVoucherPreview"
        @validate="openValidar"
      />

      <PreExpedientesPanel
        v-else-if="activeTab === 'pre-expedientes'"
        class="fade-in"
        :cotizaciones="cotizacionesAprobadas"
        :anticipos-pendientes-count="anticiposPendientesCount"
        :tipo-cambio="Number(metricas.tipoCambioOficial || 3.75)"
        :loading="loadingCotizaciones"
        @refresh="loadAllData"
        @registrar-abono="openRegistroAbono"
        @ver-historial="openHistorialCotizacion"
        @go-abonos-b2b="switchTab('abonos-b2b')"
      />

      <LiquidacionesPanel
        v-else-if="activeTab === 'liquidaciones'"
        class="fade-in"
        v-model:sub-tab="subTabLiquidacion"
        :liquidaciones="liquidacionesList"
        :expedientes-por-liquidar="expedientesPorLiquidar"
        :loading="loadingLiquidaciones || loadingExpedientes"
        @refresh="loadAllData"
        @emitir="openEmitirModal"
        @editar="openEditarModal"
        @pagar="openPagoModal"
        @detalle="openDetalleModal"
        @open-docs="openDocumentos"
        @anular="confirmAnular"
      />

      <ConciliacionPanel
        v-else-if="activeTab === 'conciliacion'"
        class="fade-in"
        :facturas="facturasList"
        :abonos-sin-factura="abonosSinFactura"
        :liquidaciones-en-conciliacion="liquidacionesEnConciliacion"
        :emitting-id="emittingFacturaId"
        :loading="loadingFacturas || loadingLiquidaciones || loadingAnticipos"
        @refresh="loadAllData"
        @preview-pdf="handlePreviewFacturaPdf"
        @download-pdf="handleDownloadFacturaPdf"
        @anular-factura="handleAnularFactura"
        @emitir-abono="handleEmitirFacturaAbono"
        @conciliar-liquidacion="openPagoModal"
        @preview-voucher-doc="openDocumentos"
      />
    </div>

    <!-- Modales Operativos -->
    <LiquidacionFormDialog
      v-model:visible="showFormModal"
      :is-edit-mode="isEditMode"
      :is-expediente-fixed="isExpedienteFixed"
      :loading-estimado="loadingEstimado"
      :submitting="submittingForm"
      :form="form"
      :totals="totals"
      :expedientes="expedientesParaFormulario"
      @expediente-change="onExpedienteSelected"
      @submit="submitLiquidacion"
    />

    <LiquidacionPagoDialog
      v-model:visible="showPagoModal"
      :liquidacion="selectedLiquidacionPago"
      :pago-form="pagoForm"
      v-model:voucher-file="voucherFile"
      :submitting="submittingPago"
      @submit="submitPago"
    />

    <LiquidacionDetalleDialog
      v-model:visible="showDetalleModal"
      :liquidacion="selectedLiquidacionDetalle"
    />

    <VoucherPreviewDialog
      v-model:visible="showVoucherModal"
      :anticipo="voucherAnticipo"
      :blob-url="voucherBlobUrl"
      :mime-type="voucherMimeType"
      :loading="loadingVoucher"
      @close="closeVoucherPreview"
      @download="descargarVoucherActual"
      @validate="openValidar"
    />

    <ValidarAbonoDialog
      v-model:visible="showValidarModal"
      :anticipo="selectedAnticipo"
      :accion="validarAccion"
      v-model:observacion-finanzas="observacionFinanzas"
      v-model:confirmacion-cotejo="confirmacionCotejo"
      :submitting="submittingValidacion"
      @confirm="submitValidar"
    />

    <HistorialAbonosDialog
      v-model:visible="showHistorialCotizacionModal"
      :cotizacion="selectedCotizacionHistorial"
      :pagos="pagosCotizacionList"
      :loading="loadingPagosCotizacion"
      @add-abono="openRegistroAbono"
      @preview-voucher="openVoucherPreview"
      @validate="openValidar"
    />

    <DocumentosManagerModal
      :visible="showDocsModal"
      entidad-tipo="EXPEDIENTE"
      :id-entidad="selectedDocEntidadId"
      :entidad-id="selectedDocEntidadId"
      :codigo-entidad="selectedDocCodigo"
      :titulo-entidad="selectedDocTitulo"
      @close="showDocsModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import finanzasService from '../services/finanzasService'
import { useFinanzasData } from '../composables/useFinanzasData'
import { useAbonosValidation } from '../composables/useAbonosValidation'
import { useLiquidacionForm } from '../composables/useLiquidacionForm'

import FinanzasDashboardPanel from '../components/FinanzasDashboardPanel.vue'
import AbonosB2bPanel from '../components/AbonosB2bPanel.vue'
import PreExpedientesPanel from '../components/PreExpedientesPanel.vue'
import LiquidacionesPanel from '../components/LiquidacionesPanel.vue'
import ConciliacionPanel from '../components/ConciliacionPanel.vue'
import RegistroAbonoView from '../components/RegistroAbonoView.vue'

import LiquidacionFormDialog from '../components/dialogs/LiquidacionFormDialog.vue'
import LiquidacionPagoDialog from '../components/dialogs/LiquidacionPagoDialog.vue'
import LiquidacionDetalleDialog from '../components/dialogs/LiquidacionDetalleDialog.vue'
import VoucherPreviewDialog from '../components/dialogs/VoucherPreviewDialog.vue'
import ValidarAbonoDialog from '../components/dialogs/ValidarAbonoDialog.vue'
import HistorialAbonosDialog from '../components/dialogs/HistorialAbonosDialog.vue'

const route = useRoute()
const router = useRouter()
const navigationStore = useNavigationStore()
const { showSuccess, showError } = useNotification()
const { confirmAction } = useConfirmation()

const emittingFacturaId = ref(null)

const activeTab = ref('dashboard')
const subTabLiquidacion = ref('emitidas')
const abonosInitialEstado = ref('PENDIENTE_REVISION')

const showRegistroAbono = ref(false)
const selectedCotizacionForPago = ref(null)

const showHistorialCotizacionModal = ref(false)
const selectedCotizacionHistorial = ref(null)
const pagosCotizacionList = ref([])
const loadingPagosCotizacion = ref(false)

const showDetalleModal = ref(false)
const selectedLiquidacionDetalle = ref(null)

const showDocsModal = ref(false)
const selectedDocEntidadId = ref(null)
const selectedDocCodigo = ref('')
const selectedDocTitulo = ref('')

const {
  liquidacionesList,
  expedientesList,
  anticiposList,
  cotizacionesAprobadas,
  facturasList,
  metricas,
  loading,
  loadingLiquidaciones,
  loadingExpedientes,
  loadingAnticipos,
  loadingCotizaciones,
  loadingFacturas,
  expedientesPorLiquidar,
  liquidacionesEnConciliacion,
  abonosSinFactura,
  loadAllData,
  loadFacturas,
  refreshPreOperativeData,
  refreshLiquidacionesData
} = useFinanzasData()

const {
  showValidarModal,
  selectedAnticipo,
  validarAccion,
  observacionFinanzas,
  confirmacionCotejo,
  submittingValidacion,
  showVoucherModal,
  voucherAnticipo,
  voucherBlobUrl,
  voucherMimeType,
  loadingVoucher,
  openValidar,
  submitValidar,
  openVoucherPreview,
  closeVoucherPreview,
  descargarVoucherActual
} = useAbonosValidation({
  onValidated: async () => {
    await refreshPreOperativeData()
    if (selectedCotizacionHistorial.value) {
      await reloadHistorialCotizacion(selectedCotizacionHistorial.value.idCotizacion)
    }
  }
})

const {
  showFormModal,
  isEditMode,
  isExpedienteFixed,
  loadingEstimado,
  submittingForm,
  form,
  totals,
  openEmitirModal,
  openEditarModal,
  onExpedienteSelected,
  submitLiquidacion,
  showPagoModal,
  selectedLiquidacionPago,
  pagoForm,
  voucherFile,
  submittingPago,
  openPagoModal,
  submitPago,
  confirmAnular
} = useLiquidacionForm({
  onMutated: refreshLiquidacionesData
})

const anticiposPendientesCount = computed(
  () => anticiposList.value.filter((a) => a.estadoPago === 'PENDIENTE_REVISION').length
)

const expedientesParaFormulario = computed(() =>
  isEditMode.value || isExpedienteFixed.value ? expedientesList.value : expedientesPorLiquidar.value
)

function openRegistroAbono(cotizacion = null) {
  selectedCotizacionForPago.value = cotizacion
  showHistorialCotizacionModal.value = false
  showRegistroAbono.value = true
}

function closeRegistroAbono() {
  showRegistroAbono.value = false
  selectedCotizacionForPago.value = null
}

async function handleAbonoSaved() {
  closeRegistroAbono()
  await refreshPreOperativeData()
  if (selectedCotizacionHistorial.value) {
    await reloadHistorialCotizacion(selectedCotizacionHistorial.value.idCotizacion)
  }
}

async function reloadHistorialCotizacion(idCotizacion) {
  loadingPagosCotizacion.value = true
  try {
    const data = await finanzasService.getPagosCotizacion(idCotizacion)
    pagosCotizacionList.value = Array.isArray(data) ? data : []
  } catch {
    showError('No se pudo cargar el historial de abonos de la cotización.')
  } finally {
    loadingPagosCotizacion.value = false
  }
}

async function openHistorialCotizacion(cotizacion) {
  if (!cotizacion) return
  selectedCotizacionHistorial.value = cotizacion
  showHistorialCotizacionModal.value = true
  await reloadHistorialCotizacion(cotizacion.idCotizacion)
}

function openDetalleModal(liquidacion) {
  if (!liquidacion) return
  selectedLiquidacionDetalle.value = liquidacion
  showDetalleModal.value = true
}

function openDocumentos(item) {
  if (!item?.idExpediente) return
  selectedDocEntidadId.value = item.idExpediente
  selectedDocCodigo.value = item.codigoLiquidacion || item.codigoExpediente || ''
  selectedDocTitulo.value = `Documentación de Liquidación (${selectedDocCodigo.value})`
  showDocsModal.value = true
}

async function handlePreviewFacturaPdf(factura) {
  if (!factura?.idComprobante) return
  try {
    const blob = await finanzasService.getFacturaPdfBlob(factura.idComprobante, { download: false })
    openBlobInNewTab(blob, 'application/pdf')
  } catch {
    showError('No se pudo previsualizar el PDF del comprobante.')
  }
}

async function handleDownloadFacturaPdf(factura) {
  if (!factura?.idComprobante) return
  try {
    const blob = await finanzasService.getFacturaPdfBlob(factura.idComprobante, { download: true })
    const filename = `${factura.numeroCompleto || 'comprobante-' + factura.idComprobante}.pdf`
    downloadBlob(blob, filename, 'application/pdf')
  } catch {
    showError('No se pudo descargar el PDF del comprobante.')
  }
}

async function handleEmitirFacturaAbono(abono) {
  if (!abono?.idPago) return
  emittingFacturaId.value = abono.idPago
  try {
    const emitida = await finanzasService.emitirFacturaAbono(abono.idPago)
    showSuccess(
      `Comprobante ${emitida?.numeroCompleto || ''} generado para el abono ${abono.codigoPago}.`,
      'Factura Emitida'
    )
    await Promise.all([loadFacturas(), refreshPreOperativeData()])
  } catch (err) {
    showError(err.response?.data?.message || 'No se pudo generar la factura para el abono.')
  } finally {
    emittingFacturaId.value = null
  }
}

async function handleAnularFactura(factura) {
  if (!factura?.idComprobante) return
  const confirmed = await confirmAction({
    message: `¿Está seguro de anular el comprobante ${factura.numeroCompleto}? Podrá reemitir uno nuevo luego.`,
    header: 'Confirmar Anulación de Comprobante',
    acceptLabel: 'Anular',
    acceptClass: 'sga-confirm-delete-btn'
  })
  if (!confirmed) return
  try {
    await finanzasService.anularFactura(factura.idComprobante)
    showSuccess(`Comprobante ${factura.numeroCompleto} anulado correctamente.`)
    await Promise.all([loadFacturas(), refreshPreOperativeData()])
  } catch (err) {
    showError(err.response?.data?.message || 'No se pudo anular el comprobante.')
  }
}

function switchTab(tab, sub = null) {
  showRegistroAbono.value = false
  selectedCotizacionForPago.value = null

  if (tab === 'abonos-b2b') {
    activeTab.value = 'abonos-b2b'
    abonosInitialEstado.value = 'PENDIENTE_REVISION'
    navigationStore.setFinanzasSubOption('abonos-b2b')
    if (route.query.tab !== 'abonos-b2b') {
      router.replace({ query: { ...route.query, tab: 'abonos-b2b' } })
    }
    return
  }

  if (tab === 'anticipos' || tab === 'pre-expedientes') {
    activeTab.value = 'pre-expedientes'
    navigationStore.setFinanzasSubOption('pre-expedientes')
    if (route.query.tab !== 'pre-expedientes') {
      router.replace({ query: { ...route.query, tab: 'pre-expedientes' } })
    }
    return
  }

  if (tab === 'por-liquidar' || tab === 'liquidaciones') {
    activeTab.value = 'liquidaciones'
    subTabLiquidacion.value = sub === 'por-liquidar' || tab === 'por-liquidar' ? 'por-liquidar' : 'emitidas'
    navigationStore.setFinanzasSubOption('liquidaciones')
    if (route.query.tab !== 'liquidaciones') {
      router.replace({ query: { ...route.query, tab: 'liquidaciones' } })
    }
    return
  }

  activeTab.value = tab
  navigationStore.setFinanzasSubOption(tab)
  if (route.query.tab !== tab) {
    router.replace({ query: { ...route.query, tab } })
  }
}

function syncFromSubOption(val) {
  if (!navigationStore.finanzasActionTrigger) {
    showRegistroAbono.value = false
    selectedCotizacionForPago.value = null
  }

  if (val === 'abonos-b2b') {
    activeTab.value = 'abonos-b2b'
    abonosInitialEstado.value = 'PENDIENTE_REVISION'
  } else if (val === 'pre-expedientes' || val === 'anticipos') {
    activeTab.value = 'pre-expedientes'
  } else if (val === 'liquidaciones' || val === 'liquidacion' || val === 'por-liquidar') {
    activeTab.value = 'liquidaciones'
    if (val === 'por-liquidar') {
      subTabLiquidacion.value = 'por-liquidar'
    } else if (subTabLiquidacion.value !== 'emitidas') {
      subTabLiquidacion.value = 'emitidas'
    }
  } else if (val === 'conciliacion' || val === 'facturacion') {
    activeTab.value = 'conciliacion'
  } else {
    activeTab.value = 'dashboard'
  }
}

function handleAccionFinanzas(act) {
  if (act === 'registrar-anticipo' || act === 'registrar-abono') {
    activeTab.value = 'pre-expedientes'
    openRegistroAbono(null)
    setTimeout(() => {
      if (navigationStore.finanzasActionTrigger === act) {
        navigationStore.finanzasActionTrigger = null
      }
    }, 50)
  }
}

watch(
  () => route.query.tab,
  (newTab) => {
    const valid = ['dashboard', 'abonos-b2b', 'pre-expedientes', 'anticipos', 'liquidaciones', 'por-liquidar', 'conciliacion', 'facturacion']
    if (newTab && valid.includes(newTab) && navigationStore.finanzasSubOption !== newTab) {
      navigationStore.setFinanzasSubOption(newTab)
    }
  },
  { immediate: true }
)

watch(() => navigationStore.finanzasSubOption, syncFromSubOption, { immediate: true })

watch(
  () => [navigationStore.finanzasActionTrigger, navigationStore.navTick],
  ([act]) => {
    if (act) handleAccionFinanzas(act)
  },
  { immediate: true }
)

watch(
  () => route.query.action,
  (actionQuery) => {
    if (actionQuery) handleAccionFinanzas(actionQuery)
  },
  { immediate: true }
)

onMounted(() => {
  loadAllData()
  if (navigationStore.finanzasActionTrigger) {
    handleAccionFinanzas(navigationStore.finanzasActionTrigger)
  } else if (route.query.action) {
    handleAccionFinanzas(route.query.action)
  }
})
</script>

<style scoped>
.finanzas-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.finanzas-main {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.fade-in {
  animation: finanzasFadeIn 0.2s ease-out;
}

@keyframes finanzasFadeIn {
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
