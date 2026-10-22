<template>
  <div class="logistica-view">
    <!-- 1. Toolbar de Acciones Warm Editorial (Sin Banners Invasivos) -->
    <div class="agente-action-toolbar">
      <div class="toolbar-left-info">
        <span class="badge-kicker">AGENTE ADUANERO // DEPÓSITOS TEMPORALES & ALMACÉN</span>
        <h3 class="toolbar-title">Custodia de Carga, Control de Pesaje VGM y Retiro</h3>
      </div>

      <div class="toolbar-actions-group">
        <span class="count-badge font-mono">{{ filteredInventario.length }} de {{ inventario.length }} registros</span>

        <button
          type="button"
          class="btn-outline-warm"
          @click="loadData"
          :disabled="loading"
          title="Recargar inventario desde el servidor"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>

        <button
          type="button"
          class="btn-espresso"
          @click="showIngresoModal = true"
        >
          <i class="pi pi-plus"></i>
          <span>+ Ingreso VGM</span>
        </button>
      </div>
    </div>

    <!-- 2. Grid de Métricas KPI Técnicas (Warm Editorial) -->
    <div class="metrics-kpi-grid">
      <div class="kpi-card pending-card" @click="filterEstado = 'EN_CUSTODIA'">
        <div class="kpi-card-header">
          <span class="kpi-num">01</span>
          <span class="kpi-label">En Custodia / Patio</span>
          <i class="pi pi-box kpi-icon pending"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono text-pending">{{ enCustodiaCount }}</span>
          <span class="kpi-badge pending">En Depósito</span>
        </div>
        <div class="kpi-footer-note">
          <span>Contenedores en bahía y almacén</span>
        </div>
      </div>

      <div class="kpi-card success-card" @click="filterEstado = 'SALIDA_AUTORIZADA'">
        <div class="kpi-card-header">
          <span class="kpi-num">02</span>
          <span class="kpi-label">Salidas Autorizadas</span>
          <i class="pi pi-check-circle kpi-icon success"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono text-success">{{ salidasCount }}</span>
          <span class="kpi-badge success">Con Levante</span>
        </div>
        <div class="kpi-footer-note">
          <span>Carga con pase de salida aduanero</span>
        </div>
      </div>

      <div class="kpi-card warning-card">
        <div class="kpi-card-header">
          <span class="kpi-num">03</span>
          <span class="kpi-label">Días Libres por Vencer</span>
          <i class="pi pi-exclamation-triangle kpi-icon warning"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono text-warning">{{ proximosAVencerCount }}</span>
          <span class="kpi-badge warning">Riesgo Sobrestadía</span>
        </div>
        <div class="kpi-footer-note">
          <span>Menos de 3 días sin penalidad</span>
        </div>
      </div>

      <div class="kpi-card accent-card" @click="filterEstado = ''">
        <div class="kpi-card-header">
          <span class="kpi-num">04</span>
          <span class="kpi-label">Total Registros VGM</span>
          <i class="pi pi-receipt kpi-icon accent"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ inventario.length }}</span>
          <span class="kpi-badge accent">Histórico</span>
        </div>
        <div class="kpi-footer-note">
          <span>Tickets de pesaje y balanza certificados</span>
        </div>
      </div>
    </div>

    <!-- 3. Contenedor de Listado y Controles -->
    <div class="content-table-card">
      <div class="card-header-bar">
        <div>
          <span class="badge-kicker">INVENTARIO EN CUSTODIA</span>
          <h3 class="section-title">Contenedores y Carga en Depósitos Temporales</h3>
        </div>

        <div class="header-actions">
          <!-- Filtro por Estado -->
          <select v-model="filterEstado" class="filter-select">
            <option value="">Todos los Estados</option>
            <option value="EN_CUSTODIA">En Custodia</option>
            <option value="SALIDA_AUTORIZADA">Salida Autorizada</option>
            <option value="RETIRADO">Retirado del Depósito</option>
            <option value="ANULADO">Anulados</option>
          </select>

          <!-- Búsqueda Rápida -->
          <div class="search-input-wrap">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model="searchQuery"
              type="text"
              class="search-input"
              placeholder="Buscar por ticket, expediente o bahía..."
            />
          </div>

          <!-- Botón de Recarga -->
          <button type="button" class="btn-outline-warm" @click="loadData" :disabled="loading">
            <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
            <span>Actualizar</span>
          </button>
        </div>
      </div>

      <!-- Tabla de Almacén con PrimeVue DataTable -->
      <div class="table-container">
        <DataTable
          :value="filteredInventario"
          :loading="loading"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idControlAlmacen"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-box empty-icon"></i>
              <p class="empty-title">No hay carga registrada en depósito</p>
              <p class="empty-desc">Registre el ingreso de contenedores y tickets de balanza VGM para su control aduanero.</p>
            </div>
          </template>

          <Column header="Ticket VGM">
            <template #body="{ data: item }">
              <div class="ticket-cell">
                <i class="pi pi-receipt"></i>
                <span class="font-mono font-bold">{{ item.ticketBalanzaVgm }}</span>
              </div>
            </template>
          </Column>

          <Column header="Expediente Aduanero">
            <template #body="{ data: item }">
              <div class="client-cell">
                <span class="font-bold text-xs font-mono">{{ item.codigoExpediente }}</span>
                <span class="client-sub">{{ item.tipoIngreso === 'DIRECTO_VERDE' ? 'Levante Directo' : 'Post-Inspección' }}</span>
              </div>
            </template>
          </Column>

          <Column header="Sede / Depósito">
            <template #body="{ data: item }">
              <div class="client-cell">
                <span class="client-name">{{ item.nombreSede || 'Depósito Temporal' }}</span>
                <span class="client-sub">Bahía: {{ item.ubicacionBahia || 'S/A' }}</span>
              </div>
            </template>
          </Column>

          <Column header="Pesaje VGM (kg)">
            <template #body="{ data: item }">
              <div class="weight-cell">
                <span class="font-bold text-xs">{{ Number(item.pesoBrutoRegistradoKg || 0).toLocaleString('es-PE', { minimumFractionDigits: 2 }) }} kg</span>
                <span class="text-muted-dash text-xs">Tara: {{ Number(item.pesoTaraRegistradoKg || 0).toLocaleString('es-PE') }} kg ({{ item.cantidadBultos }} bultos)</span>
              </div>
            </template>
          </Column>

          <Column header="Precinto">
            <template #body="{ data: item }">
              <Tag
                :value="item.precintoVerificado ? 'Conforme' : 'Observado'"
                :severity="item.precintoVerificado ? 'success' : 'warn'"
                rounded
              />
            </template>
          </Column>

          <Column header="Días Libres">
            <template #body="{ data: item }">
              <div class="days-cell">
                <span :class="['days-pill', { 'days-warning': item.diasLibresAlmacenaje <= 3 }]">
                  {{ item.diasLibresAlmacenaje }} Días
                </span>
                <span class="client-sub text-xs" v-if="item.fechaLimiteRetiro">
                  Límite: {{ item.fechaLimiteRetiro }}
                </span>
              </div>
            </template>
          </Column>

          <Column header="Estado">
            <template #body="{ data: item }">
              <Tag
                :value="getAlmacenLabel(item.estadoAlmacen)"
                :severity="getAlmacenSeverity(item.estadoAlmacen)"
                rounded
              />
            </template>
          </Column>

          <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: item }">
              <div class="actions-wrapper">
                <button
                  v-if="item.estadoAlmacen === 'EN_CUSTODIA'"
                  type="button"
                  class="action-btn-outline approve"
                  title="Autorizar salida de depósito tras pago y levante"
                  @click="handleAutorizarSalida(item)"
                >
                  <i class="pi pi-check"></i>
                  <span>Autorizar Salida</span>
                </button>

                <button
                  v-if="item.estadoAlmacen !== 'ANULADO'"
                  type="button"
                  class="action-btn-outline btn-danger-action"
                  title="Anular registro de almacén"
                  @click="handleDeleteAlmacen(item)"
                >
                  <i class="pi pi-trash"></i>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <!-- Modal de Ingreso a Almacén / Balanza VGM -->
    <IngresoAlmacenModal
      :visible="showIngresoModal"
      @close="showIngresoModal = false"
      @saved="onIngresoSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { almacenService } from '../services/almacenService'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import IngresoAlmacenModal from '../components/IngresoAlmacenModal.vue'

const navigationStore = useNavigationStore()
const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction, confirmDelete } = useConfirmation()

const loading = ref(false)
const inventario = ref([])
const filterEstado = ref('')
const searchQuery = ref('')
const showIngresoModal = ref(false)

watch(
  () => [navigationStore.agenteAduaneroSubOption, navigationStore.navTick],
  ([sub]) => {
    if (sub === 'recepcion') {
      filterEstado.value = 'EN_CUSTODIA'
    } else if (sub === 'bultos') {
      filterEstado.value = 'SALIDA_AUTORIZADA'
    } else {
      filterEstado.value = ''
    }
  },
  { immediate: true }
)

// Métricas
const enCustodiaCount = computed(() => {
  return inventario.value.filter((i) => i.estadoAlmacen === 'EN_CUSTODIA').length
})

const salidasCount = computed(() => {
  return inventario.value.filter((i) => i.estadoAlmacen === 'SALIDA_AUTORIZADA' || i.estadoAlmacen === 'RETIRADO').length
})

const proximosAVencerCount = computed(() => {
  return inventario.value.filter((i) => i.estadoAlmacen === 'EN_CUSTODIA' && i.diasLibresAlmacenaje <= 3).length
})

// Filtrado
const filteredInventario = computed(() => {
  return inventario.value.filter((item) => {
    if (filterEstado.value && item.estadoAlmacen !== filterEstado.value) {
      return false
    }

    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase().trim()
      const match =
        item.ticketBalanzaVgm?.toLowerCase().includes(q) ||
        item.codigoExpediente?.toLowerCase().includes(q) ||
        item.ubicacionBahia?.toLowerCase().includes(q) ||
        item.nombreSede?.toLowerCase().includes(q)
      if (!match) return false
    }

    return true
  })
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await almacenService.getAlmacen()
    inventario.value = data || []
  } catch (err) {
    showError(err.response?.data?.message || 'Error al conectar con el módulo de almacén.')
  } finally {
    loading.value = false
  }
}

const onIngresoSaved = (creado) => {
  showIngresoModal.value = false
  showSuccess(`Ingreso VGM registrado: ${creado.ticketBalanzaVgm}`)
  loadData()
}

const handleAutorizarSalida = async (item) => {
  const confirmed = await confirmAction({
    message: `¿Confirma la autorización de retiro de carga para el expediente ${item.codigoExpediente}? Se validará el levante y no adeudo.`,
    header: 'Autorización de Salida de Almacén',
    icon: 'pi pi-check-circle',
    acceptLabel: 'Autorizar Salida',
    acceptClass: 'p-button-success'
  })
  if (!confirmed) return

  try {
    await almacenService.autorizarSalida(item.idControlAlmacen, {
      observaciones: 'Salida autorizada por Logística y Almacén.'
    })
    showSuccess(`Salida de carga autorizada para ${item.codigoExpediente}`)
    loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'No se pudo autorizar la salida de almacén.')
  }
}

const handleDeleteAlmacen = async (item) => {
  const confirmed = await confirmDelete(
    `¿Desea anular el registro de almacén ${item.ticketBalanzaVgm}?`,
    'Confirmar Anulación de Almacén'
  )
  if (!confirmed) return

  try {
    await almacenService.deleteAlmacen(item.idControlAlmacen)
    showInfo(`Registro de almacén ${item.ticketBalanzaVgm} anulado`)
    loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'No se pudo anular el registro.')
  }
}

const getAlmacenLabel = (estado) => {
  const map = {
    EN_CUSTODIA: 'En Custodia',
    SALIDA_AUTORIZADA: 'Salida Autorizada',
    RETIRADO: 'Retirado',
    ANULADO: 'Anulado'
  }
  return map[estado] || estado
}

const getAlmacenSeverity = (estado) => {
  if (estado === 'SALIDA_AUTORIZADA' || estado === 'RETIRADO') return 'success'
  if (estado === 'EN_CUSTODIA') return 'warn'
  if (estado === 'ANULADO') return 'danger'
  return 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.logistica-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
  font-family: var(--font-sans, 'Inter', sans-serif);
}

/* 1. Toolbar de Acciones Warm Editorial */
.agente-action-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-4, 1rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1, 0.25rem);
}

.badge-kicker {
  font-family: var(--font-mono, 'JetBrains Mono', monospace);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #9a7f60);
  text-transform: uppercase;
}

.toolbar-title {
  margin: 0;
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
  letter-spacing: -0.015em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.count-badge {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  padding: 0.35rem 0.65rem;
  background-color: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-text-muted, #6f6356);
  border: 1px solid var(--color-border-subtle, #ede5d8);
  border-radius: var(--radius-pill, 9999px);
}

/* Botones Estándar Warm Editorial */
.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.55rem 1rem;
  background-color: var(--color-primary, #352f2a);
  color: var(--color-primary-text, #ffffff) !important;
  border: 1px solid var(--color-primary, #352f2a);
  border-radius: var(--radius-md, 0.5rem);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.btn-espresso:hover:not(:disabled) {
  background-color: var(--color-primary-hover, #484039);
  border-color: var(--color-primary-hover, #484039);
  transform: translateY(-1px);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.55rem 0.9rem;
  background-color: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-md, 0.5rem);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

.btn-outline-warm:hover:not(:disabled) {
  background-color: var(--color-bg-subtle, #f2ebe1);
  border-color: var(--color-border-strong, #d5c8b6);
  color: var(--color-text-main, #2b2520);
}

/* 2. Grid de Métricas KPI Técnicas */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
}

.kpi-card {
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  cursor: pointer;
  transition: all 0.2s ease-in-out;
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.kpi-card:hover {
  border-color: var(--color-border-strong, #d5c8b6);
  transform: translateY(-2px);
  box-shadow: var(--shadow-card, 0 4px 16px -2px rgba(50, 40, 30, 0.04));
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-num {
  font-family: var(--font-mono, 'JetBrains Mono', monospace);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  color: var(--color-text-subtle, #998c7e);
}

.kpi-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted, #6f6356);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-right: auto;
  margin-left: 0.5rem;
}

.kpi-icon {
  font-size: 1.1rem;
  color: var(--color-text-subtle, #998c7e);
}
.kpi-icon.pending { color: var(--color-info-text, #21598f); }
.kpi-icon.success { color: var(--color-success-text, #1e6638); }
.kpi-icon.warning { color: var(--color-warning-text, #8c5b16); }
.kpi-icon.accent { color: var(--color-accent-kicker, #9a7f60); }

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2, 0.5rem);
  margin-top: 0.15rem;
}

.kpi-value {
  font-size: var(--font-size-3xl, 1.875rem);
  font-weight: 800;
  line-height: 1;
  color: var(--color-text-main, #2b2520);
}

.kpi-badge {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.kpi-badge.success {
  background-color: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  border: 1px solid var(--color-success-border, #bce5c6);
}

.kpi-badge.warning {
  background-color: var(--color-warning-bg, #fdf5e6);
  color: var(--color-warning-text, #8c5b16);
  border: 1px solid var(--color-warning-border, #f8dab3);
}

.kpi-badge.pending {
  background-color: var(--color-info-bg, #eef4fb);
  color: var(--color-info-text, #21598f);
  border: 1px solid var(--color-info-border, #cadff4);
}

.kpi-badge.accent {
  background-color: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-accent-kicker, #9a7f60);
  border: 1px solid var(--color-border, #e4dacb);
}

.kpi-footer-note {
  font-size: var(--font-size-2xs, 0.6875rem);
  color: var(--color-text-subtle, #998c7e);
  border-top: 1px dashed var(--color-border-subtle, #ede5d8);
  padding-top: 0.4rem;
  margin-top: 0.2rem;
}

/* 3. Contenedores de Tabla y Secciones */
.content-table-card {
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4, 1rem);
  flex-wrap: wrap;
  gap: var(--space-3, 0.75rem);
}

.section-title {
  margin: 0;
  font-size: var(--font-size-lg, 1.125rem);
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

/* Selectores Nativos Sin Repetición de Flechas */
.filter-select {
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  font-family: var(--font-sans, 'Inter', sans-serif) !important;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main, #2b2520) !important;
  background-color: var(--color-bg-surface, #ffffff) !important;
  border: 1px solid var(--color-border, #e4dacb) !important;
  border-radius: var(--radius-md, 0.5rem) !important;
  padding: 0.55rem 2.25rem 0.55rem 0.85rem !important;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236f6356' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") !important;
  background-repeat: no-repeat !important;
  background-position: right 0.75rem center !important;
  background-size: 1rem 1rem !important;
  cursor: pointer;
}

.search-input-wrap {
  position: relative;
}
.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-subtle, #998c7e);
  font-size: 0.85rem;
}
.search-input {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: var(--font-size-sm, 0.8125rem);
  padding: 0.55rem 0.85rem 0.55rem 2.25rem;
  border: 1px solid var(--color-border, #e4dacb);
  background-color: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  border-radius: var(--radius-md, 0.5rem);
  width: 260px;
  outline: none;
  transition: border-color 0.15s ease;
}
.search-input:focus, .filter-select:focus {
  border-color: var(--color-border-strong, #d5c8b6);
  box-shadow: 0 0 0 3px rgba(53, 47, 42, 0.08);
}

.table-container {
  background-color: var(--color-bg-surface, #ffffff);
  border-radius: var(--radius-md, 0.5rem);
  overflow: hidden;
  border: 1px solid var(--color-border, #e4dacb);
}

.ticket-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  color: var(--color-text-main, #2b2520);
}

.client-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.client-name {
  font-weight: 700;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main, #2b2520);
}
.client-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #6f6356);
}

.weight-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.days-pill {
  display: inline-block;
  padding: 0.2rem 0.55rem;
  background-color: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  border: 1px solid var(--color-success-border, #bce5c6);
  border-radius: var(--radius-pill, 9999px);
  font-weight: 700;
  font-size: var(--font-size-xs, 0.75rem);
}

.days-warning {
  background-color: var(--color-danger-bg, #fdf0f0);
  color: var(--color-danger-text, #9e2a2b);
  border-color: var(--color-danger-border, #f6c8c8);
}

.actions-wrapper {
  display: flex;
  gap: var(--space-2, 0.5rem);
  justify-content: flex-end;
}

.action-btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-sm, 0.375rem);
  background-color: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.action-btn-outline:hover {
  background-color: var(--color-bg-subtle, #f2ebe1);
  border-color: var(--color-border-strong, #d5c8b6);
}

.action-btn-outline.approve {
  border-color: var(--color-success-border, #bce5c6);
  color: var(--color-success-text, #1e6638);
  background-color: var(--color-success-bg, #eaf6ed);
}

.action-btn-outline.approve:hover {
  filter: brightness(0.95);
}

.btn-danger-action {
  border-color: var(--color-danger-border, #f6c8c8);
  color: var(--color-danger-text, #9e2a2b);
}

.btn-danger-action:hover {
  background-color: var(--color-danger-bg, #fdf0f0);
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
}

.empty-icon {
  font-size: 2.5rem;
  color: var(--color-text-subtle, #998c7e);
  margin-bottom: 0.75rem;
}

.empty-title {
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
  margin: 0 0 0.25rem;
}

.empty-desc {
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-muted, #6f6356);
  margin: 0;
}

/* Modo Oscuro (.dark-theme) */
:global(.dark-theme) .logistica-view .filter-select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23d6b476' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") !important;
}

:global(.dark-theme) .logistica-view .btn-espresso {
  background-color: var(--color-primary, #d6b476);
  color: var(--color-primary-text, #1d1b15) !important;
  border-color: var(--color-primary, #d6b476);
}

@media (max-width: 900px) {
  .metrics-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 600px) {
  .metrics-kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
