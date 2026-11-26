<template>
  <div class="transporte-flota-view">
    <TransporteDashboardPanel
      v-if="activeTab === 'dashboard'"
      :despachos="despachosList"
      :alertas-mtc="alertasMtc"
      :kpis="kpis"
      :loading="loading"
      @refresh="loadAllData"
      @navigate="switchTab"
      @select-filter="handleFilterFromDashboard"
      @programar-despacho="showCrearDespachoDialog = true"
      @ver-detalle="openDetalleDialog"
    />

    <DespachosPanel
      v-else-if="activeTab === 'despachos'"
      class="fade-in"
      :despachos="despachosList"
      :loading="loading"
      :initial-estado="despachosInitialEstado"
      @refresh="loadAllData"
      @programar="showCrearDespachoDialog = true"
      @detalle="openDetalleDialog"
      @aceptar="aceptarSolicitud"
      @asignar="openAsignarDialog"
      @iniciar-ruta="iniciarRuta"
      @incidencia="openIncidenciaDialog"
      @entregar="openEntregaDialog"
      @anular="anularDespacho"
    />

    <FlotaPanel
      v-else-if="activeTab === 'unidades'"
      class="fade-in"
      :unidades="unidadesList"
      :loading="loading"
      @refresh="loadAllData"
      @crear="openUnidadForm(null)"
      @editar="openUnidadForm"
    />

    <ConductoresPanel
      v-else-if="activeTab === 'conductores'"
      class="fade-in"
      :conductores="conductoresList"
      :loading="loading"
      @refresh="loadAllData"
      @crear="openConductorForm(null)"
      @editar="openConductorForm"
    />

    <!-- Diálogos Operativos de Despacho -->
    <CrearDespachoDialog
      :visible="showCrearDespachoDialog"
      :unidades="unidadesList"
      :conductores="conductoresList"
      @close="showCrearDespachoDialog = false"
      @saved="loadAllData"
    />

    <AsignarRecursosDialog
      :visible="showAsignarDialog"
      :despacho="selectedDespacho"
      :unidades="unidadesList"
      :conductores="conductoresList"
      @close="showAsignarDialog = false"
      @saved="onActionSaved"
    />

    <ConfirmarEntregaDialog
      :visible="showEntregaDialog"
      :despacho="selectedDespacho"
      @close="showEntregaDialog = false"
      @saved="onActionSaved"
    />

    <IncidenciaDialog
      :visible="showIncidenciaDialog"
      :despacho="selectedDespacho"
      @close="showIncidenciaDialog = false"
      @saved="onActionSaved"
    />

    <DespachoDetalleDialog
      :visible="showDetalleDialog"
      :despacho="selectedDespacho"
      @close="showDetalleDialog = false"
      @asignar="openAsignarDialog"
      @iniciar-ruta="handleIniciarRutaDesdeDetalle"
      @incidencia="openIncidenciaDialog"
      @entregar="openEntregaDialog"
      @anular="handleAnularDesdeDetalle"
    />

    <!-- Diálogos Maestros de Flota & Conductores -->
    <UnidadFormDialog
      :visible="showUnidadFormDialog"
      :unidad="selectedUnidad"
      @close="showUnidadFormDialog = false"
      @saved="loadAllData"
    />

    <ConductorFormDialog
      :visible="showConductorFormDialog"
      :conductor="selectedConductor"
      @close="showConductorFormDialog = false"
      @saved="loadAllData"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useTransporteData } from '../composables/useTransporteData'
import { useDespachoActions } from '../composables/useDespachoActions'
import TransporteDashboardPanel from '../components/TransporteDashboardPanel.vue'
import DespachosPanel from '../components/DespachosPanel.vue'
import FlotaPanel from '../components/FlotaPanel.vue'
import ConductoresPanel from '../components/ConductoresPanel.vue'
import CrearDespachoDialog from '../components/dialogs/CrearDespachoDialog.vue'
import AsignarRecursosDialog from '../components/dialogs/AsignarRecursosDialog.vue'
import ConfirmarEntregaDialog from '../components/dialogs/ConfirmarEntregaDialog.vue'
import IncidenciaDialog from '../components/dialogs/IncidenciaDialog.vue'
import DespachoDetalleDialog from '../components/dialogs/DespachoDetalleDialog.vue'
import UnidadFormDialog from '../components/dialogs/UnidadFormDialog.vue'
import ConductorFormDialog from '../components/dialogs/ConductorFormDialog.vue'

const route = useRoute()
const router = useRouter()
const navigationStore = useNavigationStore()

const { despachosList, unidadesList, conductoresList, loading, alertasMtc, kpis, loadAllData } =
  useTransporteData()

const selectedDespacho = ref(null)
const selectedUnidad = ref(null)
const selectedConductor = ref(null)
const despachosInitialEstado = ref('ALL')

const showCrearDespachoDialog = ref(false)
const showAsignarDialog = ref(false)
const showEntregaDialog = ref(false)
const showIncidenciaDialog = ref(false)
const showDetalleDialog = ref(false)
const showUnidadFormDialog = ref(false)
const showConductorFormDialog = ref(false)

const activeTab = computed(() => navigationStore.transporteFlotaSubOption || 'dashboard')

const switchTab = (tab) => {
  navigationStore.setActiveModule('transporte-flota')
  navigationStore.setTransporteFlotaSubOption(tab)
  if (tab !== 'despachos') {
    despachosInitialEstado.value = 'ALL'
  }
  if (route.query.tab !== navigationStore.transporteFlotaSubOption) {
    router.push({ path: '/transporte-flota', query: { tab: navigationStore.transporteFlotaSubOption } })
  }
}

watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab) {
      navigationStore.setTransporteFlotaSubOption(newTab)
    }
  },
  { immediate: true }
)

const onActionSaved = async () => {
  await loadAllData()
  if (selectedDespacho.value?.idDespachoTransporte) {
    selectedDespacho.value =
      despachosList.value.find(
        (d) => d.idDespachoTransporte === selectedDespacho.value.idDespachoTransporte
      ) || null
  }
}

const { aceptarSolicitud, iniciarRuta, anularDespacho } = useDespachoActions({ onReload: onActionSaved })

const handleFilterFromDashboard = (estado) => {
  despachosInitialEstado.value = estado || 'ALL'
  switchTab('despachos')
}

const openDetalleDialog = (despacho) => {
  selectedDespacho.value = despacho
  showDetalleDialog.value = true
}

const openAsignarDialog = (despacho) => {
  selectedDespacho.value = despacho
  showDetalleDialog.value = false
  showAsignarDialog.value = true
}

const openEntregaDialog = (despacho) => {
  selectedDespacho.value = despacho
  showDetalleDialog.value = false
  showEntregaDialog.value = true
}

const openIncidenciaDialog = (despacho) => {
  selectedDespacho.value = despacho
  showDetalleDialog.value = false
  showIncidenciaDialog.value = true
}

const handleIniciarRutaDesdeDetalle = (despacho) => {
  showDetalleDialog.value = false
  iniciarRuta(despacho)
}

const handleAnularDesdeDetalle = (despacho) => {
  showDetalleDialog.value = false
  anularDespacho(despacho)
}

const openUnidadForm = (unidad) => {
  selectedUnidad.value = unidad
  showUnidadFormDialog.value = true
}

const openConductorForm = (conductor) => {
  selectedConductor.value = conductor
  showConductorFormDialog.value = true
}

onMounted(() => {
  loadAllData()
})
</script>

<style scoped>
.transporte-flota-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
</style>
