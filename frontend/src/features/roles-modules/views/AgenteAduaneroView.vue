<template>
  <div class="logistica-view">
    <!-- 1. Hero Banner Card Editorial Cálido Reutilizable -->
    <HeroBannerCard
      kicker="AGENTE ADUANERO // DEPÓSITOS TEMPORALES & ALMACÉN"
      title="Agente Aduanero - Custodia de Carga, Control de Pesaje y Retiro"
      description="Supervisión de ingresos a depósitos autorizados, certificación de masa bruta verificada (SOLAS VGM), alerta de días libres sin sobrestadía y autorización de salida tras levante."
    />

    <!-- 2. Métricas de Almacén Reutilizando MetricCard -->
    <div class="metrics-grid">
      <MetricCard index="01" label="En Custodia / Patio" :value="enCustodiaCount" severity="pending" highlight />
      <MetricCard index="02" label="Salidas Autorizadas" :value="salidasCount" severity="success" />
      <MetricCard index="03" label="Días Libres por Vencer" :value="proximosAVencerCount" severity="warning" />
      <MetricCard index="04" label="Total Registros VGM" :value="inventario.length" severity="accent" />
    </div>

    <!-- 3. Contenedor de Listado y Controles -->
    <div class="tab-content-section">
      <div class="section-header-row">
        <div>
          <span class="badge-kicker">INVENTARIO EN CUSTODIA</span>
          <h3 class="section-title">Contenedores y Carga en Depósitos Temporales</h3>
        </div>

        <div class="section-controls">
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

          <!-- Botón de Registro VGM -->
          <button type="button" class="btn-primary-action" @click="showIngresoModal = true">
            <i class="pi pi-plus"></i>
            <span>+ Ingreso VGM</span>
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
import HeroBannerCard from '@/shared/components/HeroBannerCard.vue'
import MetricCard from '@/shared/components/MetricCard.vue'

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
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
}

.hero-banner-card {
  position: relative;
  background: linear-gradient(135deg, #1c1917 0%, #1a2a30 60%, #083b47 100%);
  border: 1px solid rgba(6, 182, 212, 0.25);
  border-radius: var(--radius-lg, 12px);
  padding: 1.75rem 2rem;
  color: #fafaf9;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 820px;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #06b6d4;
  display: inline-block;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
}

.hero-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 0.5rem;
  line-height: 1.25;
}

.hero-desc {
  font-size: 0.875rem;
  line-height: 1.5;
  color: #d6d3d1;
  margin: 0;
}

.hero-watermark {
  position: absolute;
  right: 1.5rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 7rem;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.03);
  user-select: none;
  pointer-events: none;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4, 1rem);
}

.metric-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 1rem 1.25rem;
  box-shadow: var(--shadow-sm);
}

.metric-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.metric-idx {
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--color-text-muted);
}

.metric-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-subtle);
}

.metric-val {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.text-pending { color: #d97706; }
.text-success { color: #16a34a; }
.text-warning { color: #dc2626; }
.text-accent { color: var(--color-accent-copper, #c2410c); }

.tab-content-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
}

.section-controls {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.filter-select, .search-input {
  padding: 0.5rem 0.875rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  font-size: 0.875rem;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  color: var(--color-text-muted);
}

.search-input {
  padding-left: 2.25rem;
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
}

.btn-primary-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--color-accent-kicker, #c2410c);
  border: 1px solid var(--color-accent-kicker, #c2410c);
  border-radius: var(--radius-md, 8px);
  color: #fff;
  font-weight: 700;
  font-size: 0.875rem;
  cursor: pointer;
}

.table-container {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.ticket-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.client-cell {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.client-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.weight-cell {
  display: flex;
  flex-direction: column;
}

.days-pill {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
  border-radius: 4px;
  font-weight: 700;
  font-size: 0.75rem;
}

.days-warning {
  background: rgba(239, 68, 68, 0.15);
  color: #dc2626;
}

.actions-wrapper {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.action-btn-outline {
  padding: 0.35rem 0.65rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  background: none;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
}

.action-btn-outline.approve {
  border-color: #16a34a;
  color: #16a34a;
}

.btn-danger-action {
  border-color: #dc2626;
  color: #dc2626;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
}

.empty-icon {
  font-size: 2.5rem;
  color: var(--color-text-muted);
  margin-bottom: 0.75rem;
}

.empty-title {
  font-size: 1rem;
  font-weight: 700;
  margin: 0 0 0.25rem;
}

.empty-desc {
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
  margin: 0;
}
</style>
