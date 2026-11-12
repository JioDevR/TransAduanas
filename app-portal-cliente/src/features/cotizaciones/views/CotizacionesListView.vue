<template>
  <div class="cotizaciones-view">
    <!-- Vista completa de Solicitud de Cotización (Portal B2B Stepper) -->
    <PortalCotizacionForm
      v-if="showSolicitudForm"
      @cancel="showSolicitudForm = false"
      @saved="handleSolicitudSaved"
    />

    <!-- Contenido de Listado y Métricas -->
    <template v-else>
      <!-- Header de Sección -->
      <div class="view-header">
        <div class="header-titles">
          <span class="view-kicker">MÓDULO COMERCIAL B2B</span>
          <h1 class="view-title">Mis Cotizaciones Logísticas</h1>
          <p class="view-subtitle">Consulte sus cotizaciones de importación y exportación, desglose de costos y aprobación de servicios.</p>
        </div>
        <div class="header-actions">
          <button class="btn-primary" @click="showSolicitudForm = true">
            <i class="pi pi-plus"></i>
            <span>Solicitar Nueva Cotización</span>
          </button>
        </div>
      </div>

      <!-- Tarjetas Métricas KPI con numeración 01..04 Warm Editorial -->
      <div class="kpi-grid">
        <div class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-idx">01</span>
            <div class="kpi-icon-box">
              <i class="pi pi-file"></i>
            </div>
          </div>
          <div class="kpi-content">
            <span class="kpi-label">TOTAL COTIZACIONES</span>
            <span class="kpi-value font-mono">{{ cotizaciones.length }}</span>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-idx">02</span>
            <div class="kpi-icon-box success">
              <i class="pi pi-check-circle"></i>
            </div>
          </div>
          <div class="kpi-content">
            <span class="kpi-label">APROBADAS PARA DESPACHO</span>
            <span class="kpi-value font-mono">{{ cotizacionesAprobadas }}</span>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-top">
            <span class="kpi-idx">03</span>
            <div class="kpi-icon-box warning">
              <i class="pi pi-clock"></i>
            </div>
          </div>
          <div class="kpi-content">
            <span class="kpi-label">EN EVALUACIÓN TÉCNICA</span>
            <span class="kpi-value font-mono">{{ cotizacionesPendientes }}</span>
          </div>
        </div>

        <div class="kpi-card highlighted">
          <div class="kpi-top">
            <span class="kpi-idx">04</span>
            <div class="kpi-icon-box accent">
              <i class="pi pi-dollar"></i>
            </div>
          </div>
          <div class="kpi-content">
            <span class="kpi-label">VALOR TOTAL COTIZADO</span>
            <span class="kpi-value font-mono">$ {{ formatCurrency(totalMontoCotizado) }}</span>
          </div>
        </div>
      </div>

      <!-- Barra de Filtros -->
      <div class="filters-bar">
        <div class="search-box">
          <i class="pi pi-search"></i>
          <input 
            v-model="filtroTexto" 
            type="text" 
            placeholder="Buscar por código, mercancía o puerto..." 
            aria-label="Buscar cotizaciones por código, mercancía o puerto"
            class="search-input"
          />
        </div>

        <div class="filter-group">
          <label for="filtro-regimen-cotizaciones">Régimen:</label>
          <select id="filtro-regimen-cotizaciones" v-model="filtroRegimen" aria-label="Filtrar por régimen" class="custom-select">
            <option value="">Todos los regímenes</option>
            <option value="IMPORTACION_DEFINITIVA">Importación Definitiva</option>
            <option value="EXPORTACION_DEFINITIVA">Exportación Definitiva</option>
          </select>
        </div>

        <div class="filter-group">
          <label for="filtro-estado-cotizaciones">Estado:</label>
          <select id="filtro-estado-cotizaciones" v-model="filtroEstado" aria-label="Filtrar por estado" class="custom-select">
            <option value="">Todos los estados</option>
            <option value="APROBADA">Aprobadas</option>
            <option value="PENDIENTE">En Evaluación</option>
            <option value="OBSERVADA">Observadas</option>
          </select>
        </div>
      </div>

      <!-- Tabla de Cotizaciones -->
      <div class="table-card">
        <div v-if="loading" class="loading-state">
          <i class="pi pi-spin pi-spinner"></i>
          <span>Cargando cotizaciones de su empresa...</span>
        </div>

        <div v-else-if="cotizacionesFiltradas.length === 0" class="empty-state">
          <i class="pi pi-folder-open"></i>
          <h3>No se encontraron cotizaciones</h3>
          <p>No hay registros que coincidan con los filtros seleccionados o aún no ha solicitado cotizaciones.</p>
        </div>

        <div v-else class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th>CÓDIGO</th>
                <th>MERCANCÍA & CARGA</th>
                <th>RÉGIMEN</th>
                <th>RUTA LOGÍSTICA</th>
                <th class="text-right">VALOR FOB</th>
                <th class="text-right">TOTAL ESTIMADO</th>
                <th class="text-center">ESTADO</th>
                <th class="text-center">ESTADO PAGO</th>
                <th class="text-center">ACCIONES</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in paginatedCotizaciones" :key="c.idCotizacion">
                <td class="font-mono font-bold">{{ c.codigoCotizacion }}</td>
                <td>
                  <div class="item-name">{{ c.nombreMercancia || 'Mercancía General' }}</div>
                  <div class="item-sub font-mono">{{ c.codigoMercancia || 'Sin partida' }} • {{ c.nombreTipoCarga || 'General' }}</div>
                </td>
                <td>
                  <span class="regimen-tag" :class="c.regimenAduanero?.toLowerCase()">
                    {{ formatRegimen(c.regimenAduanero) }}
                  </span>
                </td>
                <td>
                  <div class="route-box">
                    <span>{{ c.nombrePuertoEnvio || 'Origen' }}</span>
                    <i class="pi pi-arrow-right"></i>
                    <span>{{ c.nombrePuertoDestino || 'Destino' }}</span>
                  </div>
                </td>
                <td class="text-right font-mono">$ {{ formatCurrency(c.valorFob) }}</td>
                <td class="text-right font-mono font-bold highlight-cell">
                  <span v-if="esPendienteLiquidacion(c)" class="pendiente-tag">Por cotizar</span>
                  <span v-else>$ {{ formatCurrency(c.totalCotizado) }}</span>
                </td>
                <td class="text-center">
                  <span class="status-badge" :class="getStatusClass(c.estado)">
                    {{ c.estado }}
                  </span>
                </td>
                <td class="text-center">
                  <span class="pago-badge" :class="getPagoClass(c.estadoPago)">
                    {{ formatPago(c.estadoPago) }}
                  </span>
                </td>
                <td class="text-center actions-cell">
                  <button class="btn-table-action" @click="verDetalle(c)" title="Ver Desglose Completo">
                    <i class="pi pi-eye"></i>
                    <span>Detalle</span>
                  </button>
                  <button 
                    v-if="c.estado === 'APROBADA' && (c.estadoPago === 'OBSERVADO' || c.estadoPago === 'RECHAZADO')" 
                    class="btn-table-action btn-table-action--observe" 
                    @click="irASubsanarPago(c)" 
                    title="Subsanar Abono Bancario Observado por Finanzas"
                  >
                    <i class="pi pi-pencil"></i>
                    <span>Subsanar</span>
                  </button>
                  <button 
                    v-else-if="c.estado === 'APROBADA' && c.estadoPago !== 'PAGADO_TOTAL'" 
                    class="btn-table-action btn-table-action--pay" 
                    @click="irAPagar(c)" 
                    title="Registrar Anticipo o Pago Completo"
                  >
                    <i class="pi pi-wallet"></i>
                    <span>Abonar</span>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Paginador Compartido -->
          <BasePagination 
            v-if="totalItems > pageSize"
            v-model:currentPage="currentPage"
            :total-pages="totalPages"
            :total-items="totalItems"
            :start-index="startIndex"
            :end-index="endIndex"
            item-label="cotizaciones"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { portalCotizacionService } from '../services/portalCotizacionService'
import { useNotification } from '@/shared/composables/useNotification'
import BasePagination from '@/shared/components/BasePagination.vue'
import PortalCotizacionForm from '../components/PortalCotizacionForm.vue'
import { esPendienteLiquidacion } from '../utils/portalCotizacionFormUtils'
import {
  formatCurrency,
  formatRegimen,
  formatEstadoPago as formatPago,
  getCotizacionStatusClass as getStatusClass,
  getPagoStatusClass as getPagoClass
} from '@/shared/utils/formatters'

const router = useRouter()
const { showSuccess, showError, showWarn } = useNotification()

const showSolicitudForm = ref(false)
const cotizaciones = ref([])
const loading = ref(true)
const filtroTexto = ref('')
const filtroRegimen = ref('')
const filtroEstado = ref('')

// Paginación
const currentPage = ref(1)
const pageSize = ref(10)

const cotizacionesFiltradas = computed(() => {
  return cotizaciones.value.filter(c => {
    const matchRegimen = !filtroRegimen.value || c.regimenAduanero === filtroRegimen.value
    const matchEstado = !filtroEstado.value || c.estado === filtroEstado.value
    const query = filtroTexto.value.toLowerCase().trim()
    const matchTexto = !query 
      || c.codigoCotizacion?.toLowerCase().includes(query)
      || c.nombreMercancia?.toLowerCase().includes(query)
      || c.nombrePuertoDestino?.toLowerCase().includes(query)

    return matchRegimen && matchEstado && matchTexto
  })
})

const totalItems = computed(() => cotizacionesFiltradas.value.length)
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value) || 1)
const startIndex = computed(() => (totalItems.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const endIndex = computed(() => Math.min(currentPage.value * pageSize.value, totalItems.value))

const paginatedCotizaciones = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return cotizacionesFiltradas.value.slice(start, start + pageSize.value)
})

watch([filtroTexto, filtroRegimen, filtroEstado], () => {
  currentPage.value = 1
})

const cotizacionesAprobadas = computed(() => {
  return cotizaciones.value.filter(c => c.estado === 'APROBADA').length
})

const cotizacionesPendientes = computed(() => {
  return cotizaciones.value.filter(c => c.estado === 'PENDIENTE').length
})

const totalMontoCotizado = computed(() => {
  return cotizaciones.value.reduce((acc, c) => acc + (Number(c.totalCotizado) || 0), 0)
})

onMounted(async () => {
  await cargarCotizaciones()
})

async function cargarCotizaciones() {
  loading.value = true
  try {
    const data = await portalCotizacionService.listarCotizaciones()
    cotizaciones.value = data || []
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar cotizaciones')
  } finally {
    loading.value = false
  }
}

async function handleSolicitudSaved({ cotizacion, docsResult }) {
  showSolicitudForm.value = false
  if (docsResult?.failed?.length > 0) {
    showWarn(
      `Cotización ${cotizacion?.codigoCotizacion || ''} generada, pero algunos documentos no pudieron subirse. Puede reintentar desde el detalle del expediente.`,
      'Solicitud Parcial'
    )
  } else {
    showSuccess(
      `Solicitud de cotización ${cotizacion?.codigoCotizacion || ''} remitida con éxito a evaluación comercial.`,
      'Solicitud Enviada'
    )
  }
  await cargarCotizaciones()
}

function verDetalle(c) {
  if (!c) return
  router.push(`/cotizaciones/${c.idCotizacion}`)
}

function irAPagar(c) {
  if (!c) return
  router.push({
    path: '/pagos/registrar',
    query: { tipo: 'COTIZACION', id: c.idCotizacion }
  })
}

function irASubsanarPago(c) {
  if (!c) return
  router.push({
    path: '/pagos',
    query: { tab: 'historial', idCotizacion: c.idCotizacion }
  })
}
</script>

<style scoped>
.cotizaciones-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
}

/* Header */
.view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.view-kicker {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
}

.view-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0.2rem 0;
  letter-spacing: -0.02em;
}

.view-subtitle {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin: 0;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.15rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: none;
  border-radius: 6px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.75rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-border-strong);
}

.kpi-card.highlighted {
  border-color: var(--color-accent-kicker);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.kpi-idx {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 6px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
  font-family: var(--font-mono);
}

.kpi-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  color: var(--color-primary);
}

.kpi-icon-box.success {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.kpi-icon-box.warning {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.kpi-icon-box.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
}

.kpi-content {
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle);
}

.kpi-value {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin-top: 0.15rem;
}

/* Filtros */
.filters-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.85rem 1rem;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 250px;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.45rem 0.75rem;
  background: var(--color-bg-base);
}

.search-box i {
  color: var(--color-text-subtle);
}

.search-box input {
  border: none;
  background: transparent;
  width: 100%;
  font-size: 0.82rem;
  color: var(--color-text-main);
  outline: none;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.custom-select {
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.45rem 2.25rem 0.45rem 0.65rem;
  background-color: var(--color-bg-surface);
  color: var(--color-text-main);
  font-size: 0.82rem;
  outline: none;
}

/* Tabla */
.table-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
}

.table-responsive {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.82rem;
}

.data-table th {
  background: var(--color-bg-subtle);
  padding: 0.75rem 1rem;
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
  border-bottom: 1px solid var(--color-border);
}

.data-table td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
  color: var(--color-text-main);
}

.data-table tr:hover {
  background: var(--color-bg-subtle);
}

.item-name {
  font-weight: 600;
}

.item-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.route-box {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.78rem;
}

.route-box i {
  font-size: 0.65rem;
  color: var(--color-accent-kicker);
}

.highlight-cell {
  color: var(--color-primary);
}

.regimen-tag {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
}

.regimen-tag.importacion_definitiva {
  background: var(--color-info-bg, var(--color-bg-subtle));
  color: var(--color-info-text, var(--color-primary));
  border: 1px solid var(--color-border-subtle);
}

.regimen-tag.exportacion_definitiva {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-badge {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 12px;
  display: inline-block;
}

.status-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-badge.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.status-badge.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.status-badge.neutral {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.pago-badge {
  font-size: 0.72rem;
  font-weight: 600;
}

.pago-badge.success {
  color: var(--color-success-text);
}

.pago-badge.info {
  color: var(--color-info-text, var(--color-primary));
}

.pago-badge.warning {
  color: var(--color-warning-text, #b78103);
  font-weight: 700;
}

.pago-badge.neutral {
  color: var(--color-text-subtle);
}

.pendiente-tag {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-warning-text, #b78103);
  background: var(--color-warning-bg, #fff8e1);
  border: 1px solid var(--color-warning-border, #ffe082);
  padding: 0.15rem 0.5rem;
  border-radius: 12px;
  display: inline-block;
  letter-spacing: 0.02em;
}

.btn-table-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-main);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-table-action:hover {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

.actions-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.btn-table-action--pay {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
}

.btn-table-action--pay:hover {
  background: var(--color-accent-kicker);
  color: var(--color-primary-text);
  border-color: var(--color-accent-kicker);
}

.btn-table-action--observe {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
  font-weight: 700;
}

.btn-table-action--observe:hover {
  background: var(--color-danger-text);
  color: var(--color-bg-surface);
  border-color: var(--color-danger-text);
}

.loading-state, .empty-state {
  padding: 3rem 1.5rem;
  text-align: center;
  color: var(--color-text-muted);
}

.empty-state i {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
  opacity: 0.4;
}

.empty-state h3 {
  margin: 0.2rem 0;
  color: var(--color-text-main);
}

.empty-state p {
  margin: 0;
  font-size: 0.82rem;
}

@media (max-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-field.full {
    grid-column: span 1;
  }
}
</style>
