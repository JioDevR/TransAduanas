<template>
  <div class="mercancias-management-view">

    <!-- Vista Principal: Directorio y Tablas Maestras de Mercancías -->
    <div class="mercancias-master-container">

      <!-- 1. Toolbar de Acciones Warm Editorial (Única Fuente de Verdad para Acciones) -->
      <div class="mercancias-action-toolbar">
        <div class="toolbar-left-info">
          <span class="badge-kicker">NOMENCLATURA & RESTRICCIONES // SUNAT VUCE</span>
          <h3 class="toolbar-title">Mercancías y Subpartidas Arancelarias</h3>
        </div>

        <div class="toolbar-actions-group">
          <span class="count-badge font-mono">{{ filteredMercancias.length }} de {{ mercancias.length }} subpartidas</span>

          <!-- Botón Actualizar -->
          <button
            type="button"
            class="btn-outline-warm"
            @click="loadMercancias"
            :disabled="loadingMercancias"
            title="Recargar catálogo de mercancías desde el servidor"
          >
            <i :class="['pi pi-sync', { 'pi-spin': loadingMercancias }]"></i>
            <span>Actualizar</span>
          </button>

          <!-- Botón Exportar PDF de Mercancías -->
          <button
            type="button"
            class="btn-outline-warm"
            @click="handleExportPdf"
            :disabled="exportingPdf"
            title="Descargar reporte oficial de subpartidas arancelarias en PDF"
          >
            <i :class="['pi', exportingPdf ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
            <span>Ver Reporte (PDF)</span>
          </button>

          <!-- Botón Principal: + Nueva Mercancía (Único botón de creación) -->
          <button
            type="button"
            class="btn-espresso"
            @click="openCreateMercanciaModal"
          >
            <i class="pi pi-plus"></i>
            <span>+ Nueva Mercancía</span>
          </button>
        </div>
      </div>

      <!-- 2. Grid de Métricas KPI Técnicas (Estándar Editorial Warm - 4 Cards) -->
      <div class="metrics-kpi-grid">
        <div class="kpi-card" @click="resetFilters">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">01</span>
            <span class="kpi-label">Partidas Totales</span>
            <i class="pi pi-tags kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ mercancias.length }}</span>
            <span class="kpi-badge accent">NANDINA</span>
          </div>
          <div class="kpi-footer-note">
            <span>Subpartidas nacionales homologadas SUNAT</span>
          </div>
        </div>

        <div class="kpi-card warning-card" @click="filterByVuce(true)">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">02</span>
            <span class="kpi-label">Control VUCE</span>
            <i class="pi pi-exclamation-triangle kpi-icon warning"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-warning">{{ vuceCount }}</span>
            <span class="kpi-badge warning">Restringidas</span>
          </div>
          <div class="kpi-footer-note">
            <span>Requieren permisos DIGEMID, SENASA o SANIPES</span>
          </div>
        </div>

        <div class="kpi-card success-card" @click="filterByVuce(false)">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">03</span>
            <span class="kpi-label">Libre Tránsito</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-success">{{ libreCount }}</span>
            <span class="kpi-badge success">Sin Restricción</span>
          </div>
          <div class="kpi-footer-note">
            <span>Despacho aduanero ordinario sin autorizaciones</span>
          </div>
        </div>

        <div class="kpi-card info-card" @click="filterByStatus('ACTIVO')">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">04</span>
            <span class="kpi-label">Partidas Activas</span>
            <i class="pi pi-shield kpi-icon info"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-info">{{ activasCount }}</span>
            <span class="kpi-badge info">Vigentes</span>
          </div>
          <div class="kpi-footer-note">
            <span>Disponibles para apertura de DAM y liquidación</span>
          </div>
        </div>
      </div>

      <!-- 3. Controles de Tabla: Buscar, Filtros y Ordenar por (Sin botón duplicado) -->
      <div class="table-controls-bar">
        <div class="controls-left">
          <!-- Campo de Búsqueda -->
          <div class="search-box">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model.trim="mercanciasSearch"
              type="text"
              placeholder="Buscar por código NANDINA, descripción o categoría..."
              class="search-input"
            />
            <button
              v-if="mercanciasSearch"
              type="button"
              class="clear-search-btn"
              @click="mercanciasSearch = ''"
              title="Limpiar búsqueda"
            >
              <i class="pi pi-times"></i>
            </button>
          </div>

          <!-- Filtro por Categoría -->
          <!-- Filtro por Categoría Dinámico (Extraído de los registros reales) -->
          <div class="filter-dropdown">
            <select v-model="mercanciasCategoriaFilter" class="filter-select" title="Filtrar por categoría">
              <option value="ALL">Todas las Categorías ({{ availableCategories.length }})</option>
              <option v-for="cat in availableCategories" :key="cat" :value="cat">
                {{ cat }}
              </option>
            </select>
          </div>

          <!-- Filtro por Control VUCE -->
          <div class="filter-dropdown">
            <select v-model="mercanciasVuceFilter" class="filter-select" title="Filtrar por control VUCE">
              <option value="ALL">Todos los Controles</option>
              <option value="RESTRICTED">Solo Restringidos VUCE</option>
              <option value="FREE">Solo Libre Tránsito</option>
            </select>
          </div>

          <!-- Filtro por Estado -->
          <div class="filter-dropdown">
            <select v-model="mercanciasStatusFilter" class="filter-select" title="Filtrar por estado">
              <option value="ALL">Todos los Estados</option>
              <option value="ACTIVO">Solo Activos</option>
              <option value="INACTIVO">Solo Inactivos</option>
            </select>
          </div>

          <!-- Ordenar Por -->
          <div class="sort-dropdown">
            <select v-model="mercanciasSortBy" class="filter-select sort-select" title="Ordenar resultados">
              <option value="codigo-asc">Ordenar por: Código NANDINA (A - Z)</option>
              <option value="nombre-asc">Ordenar por: Descripción (A - Z)</option>
              <option value="nombre-desc">Ordenar por: Descripción (Z - A)</option>
              <option value="arancel-desc">Ordenar por: Arancel (Mayor a Menor)</option>
              <option value="arancel-asc">Ordenar por: Arancel (Menor a Mayor)</option>
              <option value="categoria-asc">Ordenar por: Categoría (A - Z)</option>
              <option value="estado-asc">Ordenar por: Estado (Activos primero)</option>
            </select>
          </div>
        </div>

        <div class="controls-right">
          <!-- Botón Limpiar Filtros si hay alguno aplicado -->
          <button
            v-if="hasActiveFilters"
            type="button"
            class="btn-clear-filters"
            @click="resetFilters"
            title="Restablecer filtros"
          >
            <i class="pi pi-filter-slash"></i>
            <span>Limpiar Filtros</span>
          </button>
        </div>
      </div>

      <!-- 4. Tabla PrimeVue DataTable con Ordenación y Paginación -->
      <div class="data-table-container">
        <DataTable
          :value="sortedAndFilteredMercancias"
          :loading="loadingMercancias"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idMercancia"
          paginator
          :rows="15"
          :rowsPerPageOptions="[15, 30, 50]"
          currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} mercancías"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-tags empty-icon"></i>
              <p class="empty-title">No se encontraron mercancías registradas</p>
              <p class="empty-desc">Agregue subpartidas arancelarias para la clasificación en cotizaciones y DAM.</p>
            </div>
          </template>

          <template #loading>
            <div class="loading-state">
              <i class="pi pi-spin pi-spinner"></i>
              <span>Cargando catálogo arancelario NANDINA...</span>
            </div>
          </template>

          <Column field="codigo" header="CÓDIGO / PARTIDA" :sortable="true" style="width: 170px;">
            <template #body="{ data: m }">
              <span class="code-badge font-mono">{{ m.codigo }}</span>
            </template>
          </Column>

          <Column field="nombre" header="DESCRIPCIÓN DE MERCANCÍA" :sortable="true">
            <template #body="{ data: m }">
              <div class="port-name-cell">
                <span class="port-name">{{ m.nombre }}</span>
                <span class="port-id-sub font-mono">Subpartida #{{ m.idMercancia }}</span>
              </div>
            </template>
          </Column>

          <Column field="categoria" header="CATEGORÍA" :sortable="true" style="width: 180px;">
            <template #body="{ data: m }">
              <span class="country-pill">{{ m.categoria }}</span>
            </template>
          </Column>

          <Column field="requierePermiso" header="CONTROL VUCE" :sortable="true" style="width: 210px;">
            <template #body="{ data: m }">
              <Tag
                v-if="m.requierePermiso"
                :value="`Restringido (${m.entidadReguladora || 'VUCE'})`"
                severity="warn"
                rounded
              />
              <Tag
                v-else
                value="Libre Comercialización"
                severity="success"
                rounded
              />
            </template>
          </Column>

          <Column field="arancelBasePct" header="ARANCEL BASE" :sortable="true" style="width: 140px; text-align: right;" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: m }">
              <span class="arancel-pct font-mono font-bold">{{ Number(m.arancelBasePct || 0).toFixed(2) }}%</span>
            </template>
          </Column>

          <Column field="estado" header="ESTADO" :sortable="true" style="width: 120px; text-align: center;" headerClass="text-center" bodyClass="text-center">
            <template #body="{ data: m }">
              <Tag
                :value="m.estado"
                :severity="m.estado === 'ACTIVO' ? 'success' : 'danger'"
                rounded
              />
            </template>
          </Column>

          <Column header="ACCIONES" style="width: 140px; text-align: right;" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: m }">
              <div class="actions-cell">
                <button
                  type="button"
                  class="action-btn edit-btn"
                  @click="openEditMercanciaModal(m)"
                  title="Editar mercancía"
                >
                  <i class="pi pi-pencil"></i>
                  <span>Editar</span>
                </button>

                <button
                  v-if="m.estado === 'ACTIVO'"
                  type="button"
                  class="action-btn deactivate-btn"
                  @click="handleDeactivateMercancia(m)"
                  title="Desactivar mercancía"
                >
                  <i class="pi pi-ban"></i>
                </button>

                <button
                  v-else
                  type="button"
                  class="action-btn reactivate-btn"
                  @click="handleReactivateMercancia(m)"
                  title="Reactivar mercancía"
                >
                  <i class="pi pi-check-circle"></i>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>

    </div>

    <!-- Modal Formulario de Mercancía -->
    <MercanciaFormModal
      :visible="showMercanciaModal"
      :mercancia-to-edit="selectedMercancia"
      :existing-categories="availableCategories"
      @close="showMercanciaModal = false"
      @saved="onMercanciaSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import mercanciaService from '../services/mercanciaService'
import MercanciaFormModal from '../components/MercanciaFormModal.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { openBlobInNewTab } from '@/shared/utils/download'

const { showSuccess, showError, showInfo, showWarn } = useNotification()
const { confirmAction } = useConfirmation()

// Estado General
const mercancias = ref([])
const loadingMercancias = ref(false)
const exportingPdf = ref(false)

// Filtros y Ordenación
const mercanciasSearch = ref('')
const mercanciasCategoriaFilter = ref('ALL')
const mercanciasVuceFilter = ref('ALL')
const mercanciasStatusFilter = ref('ALL')
const mercanciasSortBy = ref('codigo-asc')

// Modal Formulario
const showMercanciaModal = ref(false)
const selectedMercancia = ref(null)

const showToast = (message, type = 'success') => {
  if (type === 'error') {
    showError(message)
  } else if (type === 'warn' || type === 'warning') {
    showWarn(message)
  } else if (type === 'info') {
    showInfo(message)
  } else {
    showSuccess(message)
  }
}

// Métricas Computadas
const activasCount = computed(() => {
  return mercancias.value.filter((m) => m.estado === 'ACTIVO').length
})

const vuceCount = computed(() => {
  return mercancias.value.filter((m) => m.requierePermiso).length
})

// Categorías dinámicas reales presentes en la base de datos
const availableCategories = computed(() => {
  const cats = new Set()
  mercancias.value.forEach((m) => {
    if (m.categoria && m.categoria.trim()) {
      cats.add(m.categoria.trim())
    }
  })
  return Array.from(cats).sort((a, b) => a.localeCompare(b))
})

const libreCount = computed(() => {
  return mercancias.value.filter((m) => !m.requierePermiso).length
})

const hasActiveFilters = computed(() => {
  return Boolean(
    mercanciasSearch.value ||
    mercanciasCategoriaFilter.value !== 'ALL' ||
    mercanciasVuceFilter.value !== 'ALL' ||
    mercanciasStatusFilter.value !== 'ALL' ||
    mercanciasSortBy.value !== 'codigo-asc'
  )
})

const resetFilters = () => {
  mercanciasSearch.value = ''
  mercanciasCategoriaFilter.value = 'ALL'
  mercanciasVuceFilter.value = 'ALL'
  mercanciasStatusFilter.value = 'ALL'
  mercanciasSortBy.value = 'codigo-asc'
}

const filterByVuce = (isRestricted) => {
  mercanciasVuceFilter.value = isRestricted ? 'RESTRICTED' : 'FREE'
}

const filterByStatus = (status) => {
  mercanciasStatusFilter.value = status
}

// Filtrado de Mercancías
const filteredMercancias = computed(() => {
  return mercancias.value.filter((m) => {
    const q = mercanciasSearch.value.toLowerCase().trim()
    const matchesSearch =
      !q ||
      m.codigo?.toLowerCase().includes(q) ||
      m.nombre?.toLowerCase().includes(q) ||
      m.categoria?.toLowerCase().includes(q)

    const matchesCategory =
      mercanciasCategoriaFilter.value === 'ALL' ||
      (m.categoria && m.categoria.trim().toLowerCase() === mercanciasCategoriaFilter.value.trim().toLowerCase())

    const matchesStatus =
      mercanciasStatusFilter.value === 'ALL' || m.estado === mercanciasStatusFilter.value

    let matchesVuce = true
    if (mercanciasVuceFilter.value === 'RESTRICTED') {
      matchesVuce = Boolean(m.requierePermiso)
    } else if (mercanciasVuceFilter.value === 'FREE') {
      matchesVuce = !m.requierePermiso
    }

    return matchesSearch && matchesCategory && matchesStatus && matchesVuce
  })
})

// Ordenación Aplicada
const sortedAndFilteredMercancias = computed(() => {
  const list = [...filteredMercancias.value]
  switch (mercanciasSortBy.value) {
    case 'codigo-asc':
      return list.sort((a, b) => (a.codigo || '').localeCompare(b.codigo || ''))
    case 'nombre-asc':
      return list.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''))
    case 'nombre-desc':
      return list.sort((a, b) => (b.nombre || '').localeCompare(a.nombre || ''))
    case 'arancel-desc':
      return list.sort((a, b) => Number(b.arancelBasePct || 0) - Number(a.arancelBasePct || 0))
    case 'arancel-asc':
      return list.sort((a, b) => Number(a.arancelBasePct || 0) - Number(b.arancelBasePct || 0))
    case 'categoria-asc':
      return list.sort((a, b) => (a.categoria || '').localeCompare(b.categoria || ''))
    case 'estado-asc':
      return list.sort((a, b) => (a.estado === 'ACTIVO' ? -1 : 1))
    default:
      return list
  }
})

// Cargar Mercancías
const loadMercancias = async () => {
  loadingMercancias.value = true
  try {
    const res = await mercanciaService.getMercancias()
    mercancias.value = Array.isArray(res) ? res : (res?.data || res?.content || [])
  } catch (error) {
    console.error('Error cargando mercancías:', error)
    showToast('Error al conectar con el catálogo de mercancías', 'error')
  } finally {
    loadingMercancias.value = false
  }
}

// Acciones Mercancías
const openCreateMercanciaModal = () => {
  selectedMercancia.value = null
  showMercanciaModal.value = true
}

const openEditMercanciaModal = (mercancia) => {
  selectedMercancia.value = { ...mercancia }
  showMercanciaModal.value = true
}

const onMercanciaSaved = () => {
  showMercanciaModal.value = false
  showToast('Mercancía guardada exitosamente')
  loadMercancias()
}

const handleDeactivateMercancia = async (mercancia) => {
  const confirmed = await confirmAction({
    message: `¿Desea desactivar la mercancía ${mercancia.codigo} — ${mercancia.nombre}?`,
    header: 'Confirmar Desactivación de Mercancía',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Desactivar',
    acceptClass: 'p-button-danger'
  })
  if (!confirmed) return

  try {
    await mercanciaService.deleteMercancia(mercancia.idMercancia)
    showToast(`Mercancía ${mercancia.codigo} desactivada`)
    loadMercancias()
  } catch (error) {
    console.error('Error desactivando mercancía:', error)
    showToast('No se pudo desactivar la mercancía', 'error')
  }
}

const handleReactivateMercancia = async (mercancia) => {
  try {
    await mercanciaService.reactivarMercancia(mercancia.idMercancia)
    showToast(`Mercancía ${mercancia.codigo} reactivada exitosamente`)
    loadMercancias()
  } catch (error) {
    console.error('Error reactivando mercancía:', error)
    showToast('No se pudo reactivar la mercancía', 'error')
  }
}

// Visualizar / Descargar Reporte PDF en nueva pestaña
const handleExportPdf = async () => {
  exportingPdf.value = true
  try {
    const blobData = await mercanciaService.downloadPdf()
    openBlobInNewTab(blobData)
    showToast('Reporte oficial de subpartidas arancelarias abierto en nueva pestaña')
  } catch (err) {
    console.error('Error al descargar reporte de mercancías:', err)
    showToast('No se pudo generar el reporte de mercancías', 'error')
  } finally {
    exportingPdf.value = false
  }
}

onMounted(() => {
  loadMercancias()
})
</script>

<style scoped>
.mercancias-management-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

.mercancias-master-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR DE ACCIONES WARM EDITORIAL
   ========================================================================== */
.mercancias-action-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-4, 1rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle);
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.badge-kicker {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-accent-kicker);
}

.toolbar-title {
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.015em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.count-badge {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.4rem 0.75rem;
  border-radius: var(--radius-pill);
}

/* ==========================================================================
   2. REJILLA DE TARJETAS KPI (WARM EDITORIAL)
   ========================================================================== */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
}

@media (max-width: 1200px) {
  .metrics-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-kpi-grid {
    grid-template-columns: 1fr;
  }
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-3, 0.75rem);
  cursor: pointer;
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.kpi-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-elevated);
}

.kpi-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kpi-num {
  font-family: var(--font-mono, monospace);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.kpi-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-right: auto;
  margin-left: 0.65rem;
}

.kpi-icon {
  font-size: 1.1rem;
  color: var(--color-text-subtle);
}

.kpi-icon.warning {
  color: var(--color-warning-text, #d97706);
}

.kpi-icon.success {
  color: var(--color-success-text, #16a34a);
}

.kpi-icon.info {
  color: var(--color-info-text, #0369a1);
}

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-value {
  font-family: var(--font-sans);
  font-size: 1.65rem;
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.kpi-value.font-mono {
  font-family: var(--font-mono, monospace);
  font-size: 1.35rem;
}

.text-success {
  color: var(--color-success-text, #16a34a) !important;
}

.text-warning {
  color: var(--color-warning-text, #d97706) !important;
}

.text-info {
  color: var(--color-info-text, #0369a1) !important;
}

.kpi-badge {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kpi-badge.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  border: 1px solid var(--color-border-subtle);
}

.kpi-badge.warning {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #92400e);
}

.kpi-badge.success {
  background: var(--color-success-bg, #dcfce7);
  color: var(--color-success-text, #166534);
}

.kpi-badge.info {
  background: var(--color-info-bg, #e0f2fe);
  color: var(--color-info-text, #0369a1);
}

.kpi-footer-note {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-subtle);
  border-top: 1px dashed var(--color-border-subtle);
  padding-top: var(--space-2, 0.5rem);
}

/* ==========================================================================
   3. BARRA DE CONTROLES: BUSCAR, FILTROS Y ORDENAR
   ========================================================================== */
.table-controls-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 0.85rem 1.25rem;
  box-shadow: var(--shadow-subtle);
}

.controls-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  flex: 1;
}

.controls-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 280px;
  flex: 1;
  max-width: 380px;
}

.search-icon {
  position: absolute;
  left: 0.85rem;
  color: var(--color-text-subtle);
  font-size: 0.85rem;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 0.55rem 2rem 0.55rem 2.25rem;
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 0.825rem;
  color: var(--color-text-main);
  outline: none;
  transition: all var(--transition-fast);
}

.search-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(214, 180, 118, 0.2);
}

.clear-search-btn {
  position: absolute;
  right: 0.5rem;
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-search-btn:hover {
  color: var(--color-text-main);
}

.filter-dropdown, .sort-dropdown {
  position: relative;
}

.filter-select {
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  font-family: var(--font-sans) !important;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main) !important;
  background-color: var(--color-bg-surface) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md) !important;
  padding: 0.55rem 2.25rem 0.55rem 0.85rem !important;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239a7f60' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e") !important;
  background-repeat: no-repeat !important;
  background-position: right 0.75rem center !important;
  background-size: 0.9rem 0.9rem !important;
  cursor: pointer;
  outline: none;
  transition: all var(--transition-fast);
}

.filter-select:focus {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 2px rgba(214, 180, 118, 0.2) !important;
}

.filter-select option {
  background-color: var(--color-bg-surface);
  color: var(--color-text-main);
}

.btn-clear-filters {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.8rem;
  border-radius: var(--radius-md);
  font-size: 0.775rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-clear-filters:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
}

/* ==========================================================================
   4. DATA TABLE PRIME VUE
   ========================================================================== */
.data-table-container {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.code-badge {
  font-family: var(--font-mono, monospace);
  font-size: 0.775rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.port-name-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.port-name {
  font-weight: 600;
  color: var(--color-text-main);
  font-size: 0.85rem;
}

.port-id-sub {
  font-size: 0.7rem;
  color: var(--color-text-subtle);
}

.country-pill {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--color-text-muted);
}

.arancel-pct {
  font-family: var(--font-mono, monospace);
  font-size: 0.825rem;
  color: var(--color-text-main);
}

.actions-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.45rem;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all var(--transition-fast);
}

.edit-btn {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  border-color: var(--color-border);
}

.edit-btn:hover {
  background: var(--color-border-subtle);
  border-color: var(--color-border-strong);
}

.deactivate-btn {
  background: transparent;
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
}

.deactivate-btn:hover {
  background: var(--color-danger-bg);
}

.reactivate-btn {
  background: transparent;
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.reactivate-btn:hover {
  background: var(--color-success-bg);
}

/* Botones Oficiales Warm Editorial */
.btn-espresso {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  padding: 0.55rem 1rem;
  border-radius: var(--radius-md, 8px);
  font-weight: 600;
  font-size: var(--font-size-sm, 0.8125rem);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all var(--transition-fast);
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-outline-warm {
  background-color: var(--color-bg-surface);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.9rem;
  border-radius: var(--radius-md, 8px);
  font-weight: 600;
  font-size: var(--font-size-sm, 0.8125rem);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all var(--transition-fast);
}

.btn-outline-warm:hover:not(:disabled) {
  background-color: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.5rem;
  text-align: center;
}

.empty-icon {
  font-size: 2.25rem;
  color: var(--color-text-subtle);
  margin-bottom: 0.75rem;
}

.empty-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.35rem 0;
}

.empty-desc {
  font-size: 0.825rem;
  color: var(--color-text-muted);
  margin: 0;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 3rem 1.5rem;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}
</style>
