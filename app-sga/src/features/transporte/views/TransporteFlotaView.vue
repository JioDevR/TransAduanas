<template>
  <div class="transporte-flota-shell">
    <!-- 1. Barra de Herramientas Superior Warm Editorial -->
    <TransporteHeaderToolbar
      :loading="loadingGlobal"
      :en-ruta-count="enRutaCount"
      :disponibles-count="tractosDisponiblesCount"
      @recargar="recargarTodo"
      @programar-despacho="openCrearDespachoModal"
    />

    <!-- 2. Pestañas de Navegación del Módulo Sincronizadas con Sidebar -->
    <nav class="transporte-tabs-nav" aria-label="Navegación Transporte">
      <button
        type="button"
        :class="['tab-btn', { active: currentTab === 'dashboard' }]"
        @click="switchTab('dashboard')"
      >
        <span class="tab-num">01</span>
        <i class="pi pi-chart-bar"></i>
        <span>Dashboard Operativo</span>
      </button>

      <button
        type="button"
        :class="['tab-btn', { active: currentTab === 'despachos' }]"
        @click="switchTab('despachos')"
      >
        <span class="tab-num">02</span>
        <i class="pi pi-list"></i>
        <span>Monitoreo & Despachos</span>
        <span class="tab-badge">{{ despachosList.length }}</span>
      </button>

      <button
        type="button"
        :class="['tab-btn', { active: currentTab === 'cabina' }]"
        @click="switchTab('cabina')"
      >
        <span class="tab-num">03</span>
        <i class="pi pi-compass"></i>
        <span>Cabina de Ruta</span>
        <span v-if="enRutaCount > 0" class="tab-badge-pulse">{{ enRutaCount }} EN RUTA</span>
      </button>

      <button
        type="button"
        :class="['tab-btn', { active: currentTab === 'unidades' }]"
        @click="switchTab('unidades')"
      >
        <span class="tab-num">04</span>
        <i class="pi pi-truck"></i>
        <span>Parque de Flota</span>
        <span class="tab-badge">{{ unidadesList.length }}</span>
      </button>

      <button
        type="button"
        :class="['tab-btn', { active: currentTab === 'conductores' }]"
        @click="switchTab('conductores')"
      >
        <span class="tab-num">05</span>
        <i class="pi pi-users"></i>
        <span>Directorio Choferes</span>
        <span class="tab-badge">{{ conductoresList.length }}</span>
      </button>
    </nav>

    <!-- 3. Contenedores de Sub-paneles -->
    <main class="transporte-content-area">
      <!-- Sub-panel 01: Dashboard Operativo -->
      <TransporteDashboardPanel
        v-if="currentTab === 'dashboard'"
        :despachos="despachosList"
        :unidades="unidadesList"
        :conductores="conductoresList"
        :loading="loadingGlobal"
        @select-filter="handleFilterFromDashboard"
        @go-tab="switchTab"
      />

      <!-- Sub-panel 02: Bandeja de Despachos -->
      <TransporteDespachosTable
        v-else-if="currentTab === 'despachos'"
        :despachos-list="despachosList"
        :loading="loadingGlobal"
        :initial-filter="despachosTableFilter"
        @asignar-recursos="openAsignarRecursos"
        @iniciar-ruta="handleIniciarRutaDirecto"
        @confirmar-entrega="openConfirmarEntrega"
        @ver-cabina="handleVerEnCabina"
        @reportar-incidencia="openReportarIncidencia"
      />

      <!-- Sub-panel 03: Cabina de Ruta -->
      <CabinaRutaConductorPanel
        v-else-if="currentTab === 'cabina'"
        :despacho="miDespachoActivo"
        :despachos-list="despachosList"
        :preselected-id="cabinaPreselectedId"
        @iniciar-ruta="handleIniciarRutaDirecto"
        @confirmar-entrega="openConfirmarEntrega"
        @reportar-incidencia="openReportarIncidencia"
        @go-despachos="switchTab('despachos')"
      />

      <!-- Sub-panel 04: Parque de Flota MTC -->
      <FlotaManagementPanel
        v-else-if="currentTab === 'unidades'"
      />

      <!-- Sub-panel 05: Directorio de Choferes Profesionales -->
      <ConductoresManagementPanel
        v-else-if="currentTab === 'conductores'"
      />
    </main>

    <!-- 4. Modales de Negocio -->
    <CrearDespachoModal
      :visible="showCrearDespachoModal"
      @close="showCrearDespachoModal = false"
      @saved="onDespachoGuardado"
    />

    <AsignarRecursosModal
      :visible="showAsignarRecursosModal"
      :despacho="selectedDespacho"
      @close="showAsignarRecursosModal = false"
      @saved="onDespachoGuardado"
    />

    <ConfirmarEntregaModal
      :visible="showConfirmarEntregaModal"
      :despacho="selectedDespacho"
      @close="showConfirmarEntregaModal = false"
      @saved="onEntregaConfirmada"
    />

    <!-- Modal Reportar Incidencia -->
    <Dialog
      v-model:visible="showIncidenciaModal"
      modal
      header="BITÁCORA // REGISTRAR INCIDENCIA EN RUTA"
      :style="{ width: '520px' }"
    >
      <form @submit.prevent="submitIncidencia" class="incidencia-form">
        <p class="incidencia-subtitle">
          Despacho: <strong class="font-mono">{{ selectedDespacho?.codigoTracking }}</strong>
        </p>
        <div class="form-group">
          <label class="form-label">Detalle de la eventualidad / novedad en ruta</label>
          <textarea
            v-model="incidenciaTexto"
            rows="4"
            class="form-textarea"
            placeholder="Describa la incidencia (control aduanero SUNAT, congestión portuaria, desperfecto técnico, desvío autorizado)..."
            required
          ></textarea>
        </div>
        <div class="modal-footer-actions">
          <button type="button" class="btn-outline-warm" @click="showIncidenciaModal = false">
            Cancelar
          </button>
          <button type="submit" class="btn-espresso" :disabled="savingIncidencia">
            <i v-if="savingIncidencia" class="pi pi-spin pi-spinner"></i>
            <span>Asentar Reporte</span>
          </button>
        </div>
      </form>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Dialog from 'primevue/dialog'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { transporteFlotaService } from '../services/transporteFlotaService'
import TransporteHeaderToolbar from '../components/TransporteHeaderToolbar.vue'
import TransporteDashboardPanel from '../components/TransporteDashboardPanel.vue'
import TransporteDespachosTable from '../components/TransporteDespachosTable.vue'
import CabinaRutaConductorPanel from '../components/CabinaRutaConductorPanel.vue'
import FlotaManagementPanel from '../components/FlotaManagementPanel.vue'
import ConductoresManagementPanel from '../components/ConductoresManagementPanel.vue'
import CrearDespachoModal from '../components/CrearDespachoModal.vue'
import AsignarRecursosModal from '../components/AsignarRecursosModal.vue'
import ConfirmarEntregaModal from '../components/ConfirmarEntregaModal.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'

const route = useRoute()
const router = useRouter()
const navigationStore = useNavigationStore()
const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction } = useConfirmation()

// Estados reactivos globales
const despachosList = ref([])
const unidadesList = ref([])
const conductoresList = ref([])
const miDespachoActivo = ref(null)
const loadingGlobal = ref(false)

// Navegación de pestañas
const currentTab = computed(() => {
  return navigationStore.transporteFlotaSubOption || 'dashboard'
})

const despachosTableFilter = ref('')
const cabinaPreselectedId = ref(null)

// Modales
const showCrearDespachoModal = ref(false)
const showAsignarRecursosModal = ref(false)
const showConfirmarEntregaModal = ref(false)
const showIncidenciaModal = ref(false)

const selectedDespacho = ref(null)
const incidenciaTexto = ref('')
const savingIncidencia = ref(false)

// Métricas computadas
const enRutaCount = computed(() => {
  return despachosList.value.filter(d => (d.estadoTransporte || '').toUpperCase() === 'EN_RUTA').length
})

const tractosDisponiblesCount = computed(() => {
  return unidadesList.value.filter(u => u.estadoActivo && (u.estadoOperativo || '').toUpperCase() === 'DISPONIBLE').length
})

// Carga inicial y sincronización
const switchTab = (tab) => {
  let target = tab
  if (tab === 'camion') target = 'cabina'
  if (tab === 'viajes') target = 'despachos'

  navigationStore.setActiveModule('transporte-flota')
  navigationStore.setTransporteFlotaSubOption(target)

  if (route.query.tab !== target) {
    router.push({ path: '/transporte-flota', query: { tab: target } })
  }
}

watch(() => route.query.tab, (newTab) => {
  if (newTab && newTab !== navigationStore.transporteFlotaSubOption) {
    navigationStore.setTransporteFlotaSubOption(newTab)
  }
}, { immediate: true })

watch(() => route.query.id, (newId) => {
  if (newId) {
    cabinaPreselectedId.value = newId
  }
}, { immediate: true })

const recargarTodo = async () => {
  loadingGlobal.value = true
  try {
    const [despachos, unidades, conductores] = await Promise.all([
      transporteFlotaService.getDespachos(),
      transporteFlotaService.getUnidades(),
      transporteFlotaService.getConductores()
    ])

    despachosList.value = Array.isArray(despachos) ? despachos : []
    unidadesList.value = Array.isArray(unidades) ? unidades : []
    conductoresList.value = Array.isArray(conductores) ? conductores : []

    try {
      miDespachoActivo.value = await transporteFlotaService.getMiDespachoActivo()
    } catch {
      miDespachoActivo.value = null
    }
  } catch (err) {
    showError(err.message || 'Error al sincronizar datos de transporte')
  } finally {
    loadingGlobal.value = false
  }
}

// Handlers de modales y acciones operativas
const openCrearDespachoModal = () => {
  showCrearDespachoModal.value = true
}

const openAsignarRecursos = (despacho) => {
  selectedDespacho.value = despacho
  showAsignarRecursosModal.value = true
}

const openConfirmarEntrega = (despacho) => {
  selectedDespacho.value = despacho
  showConfirmarEntregaModal.value = true
}

const openReportarIncidencia = (despacho) => {
  selectedDespacho.value = despacho
  incidenciaTexto.value = ''
  showIncidenciaModal.value = true
}

const handleIniciarRutaDirecto = async (despacho) => {
  const ok = await confirmAction({
    header: 'Iniciar Ruta de Transporte',
    message: `¿Confirma la salida a ruta del despacho ${despacho.codigoTracking}? La unidad y el conductor pasarán a estado EN RUTA y se notificará al cliente.`,
    acceptLabel: 'Iniciar Ruta',
    severity: 'info'
  })
  if (!ok) return

  try {
    await transporteFlotaService.iniciarRuta(despacho.idDespachoTransporte)
    showSuccess(`Ruta iniciada para despacho ${despacho.codigoTracking}`)
    await recargarTodo()
  } catch (err) {
    showError(err.response?.data?.message || err.message || 'Error al iniciar ruta')
  }
}

const handleVerEnCabina = (despacho) => {
  cabinaPreselectedId.value = despacho.idDespachoTransporte
  selectedDespacho.value = despacho
  switchTab('cabina')
}

const handleFilterFromDashboard = (status) => {
  despachosTableFilter.value = status
  switchTab('despachos')
}

const submitIncidencia = async () => {
  if (!selectedDespacho.value || !incidenciaTexto.value.trim()) return
  savingIncidencia.value = true
  try {
    await transporteFlotaService.reportarIncidencia(
      selectedDespacho.value.idDespachoTransporte,
      incidenciaTexto.value.trim()
    )
    showSuccess('Incidencia registrada exitosamente en la bitácora')
    showIncidenciaModal.value = false
    await recargarTodo()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al asentar incidencia')
  } finally {
    savingIncidencia.value = false
  }
}

const onDespachoGuardado = async () => {
  showSuccess('Operación guardada exitosamente')
  await recargarTodo()
}

const onEntregaConfirmada = async () => {
  showSuccess('Entrega conforme asentada y unidad liberada')
  await recargarTodo()
}

onMounted(() => {
  recargarTodo()
})
</script>

<style scoped>
.transporte-flota-shell {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.5rem;
  max-width: 1440px;
  margin: 0 auto;
}

.transporte-tabs-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  overflow-x: auto;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
  padding-bottom: 0.5rem;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  border-radius: var(--radius-md, 0.5rem);
  background: transparent;
  border: 1px solid transparent;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-secondary, #5c544d);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.tab-btn:hover {
  background: var(--bg-hover, #f5f4f0);
  color: var(--text-primary, #1e1b18);
}

.tab-btn.active {
  background: var(--bg-surface, #ffffff);
  color: #b45309;
  border-color: #dcd7ce;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.tab-num {
  font-family: var(--font-mono, monospace);
  font-size: 0.6875rem;
  color: var(--text-tertiary, #9ca3af);
}

.tab-badge {
  font-family: var(--font-mono, monospace);
  font-size: 0.6875rem;
  background: var(--bg-hover, #f5f4f0);
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  color: var(--text-secondary, #5c544d);
}

.tab-badge-pulse {
  font-family: var(--font-mono, monospace);
  font-size: 0.6875rem;
  font-weight: 700;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 0.1rem 0.5rem;
  border-radius: 9999px;
  border: 1px solid #bfdbfe;
}

.transporte-content-area {
  min-height: 500px;
}

/* Modales */
.incidencia-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.incidencia-subtitle {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-secondary, #5c544d);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-primary, #1e1b18);
}

.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color, #dcd7ce);
  border-radius: var(--radius-md, 0.5rem);
  font-size: 0.875rem;
  font-family: inherit;
  resize: vertical;
}

.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #2b2621;
  color: #ffffff;
  border: 1px solid #1a1714;
  padding: 0.625rem 1rem;
  border-radius: var(--radius-md, 0.5rem);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: transparent;
  color: var(--text-primary, #2b2621);
  border: 1px solid var(--border-color, #dcd7ce);
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius-md, 0.5rem);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}
</style>
