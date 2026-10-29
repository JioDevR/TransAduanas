<template>
  <div class="tipos-carga-management-view">

    <!-- Vista Completa: Formulario Maestro de Tipo de Carga -->
    <TipoCargaRegistrationForm
      v-if="showTipoCargaForm"
      :tipo-to-edit="selectedTipo"
      @cancel="showTipoCargaForm = false"
      @saved="onTipoSaved"
    />

    <!-- Vista Principal: Directorio y Tablas Maestras de Tipos de Carga -->
    <div v-else class="tipos-master-container">

      <!-- 1. Toolbar de Acciones Warm Editorial (Única Fuente de Verdad para Acciones) -->
      <div class="tipos-action-toolbar">
        <div class="toolbar-left-info">
          <span class="badge-kicker">CATÁLOGOS AUXILIARES // LOGÍSTICA DE TRANSPORTE</span>
          <h3 class="toolbar-title">Tipos de Carga & Contenedores Sugeridos</h3>
        </div>

        <div class="toolbar-actions-group">
          <span class="count-badge font-mono">{{ filteredTiposCarga.length }} de {{ tiposCarga.length }} clasificaciones</span>

          <!-- Botón Actualizar -->
          <button
            type="button"
            class="btn-outline-warm"
            @click="loadTiposCarga"
            :disabled="loadingTipos"
            title="Recargar catálogo de tipos de carga desde el servidor"
          >
            <i :class="['pi pi-sync', { 'pi-spin': loadingTipos }]"></i>
            <span>Actualizar</span>
          </button>

          <!-- Botón Exportar PDF de Tipos de Carga -->
          <button
            type="button"
            class="btn-outline-warm"
            @click="handleExportPdf"
            :disabled="exportingPdf"
            title="Descargar reporte oficial de tipos de carga en PDF"
          >
            <i :class="['pi', exportingPdf ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
            <span>Ver Reporte (PDF)</span>
          </button>

          <!-- Botón Principal: + Nuevo Tipo de Carga (Único botón de creación) -->
          <button
            type="button"
            class="btn-espresso"
            @click="openCreateTipoModal"
          >
            <i class="pi pi-plus"></i>
            <span>+ Nuevo Tipo Carga</span>
          </button>
        </div>
      </div>

      <!-- 2. Grid de Métricas KPI Técnicas (Estándar Editorial Warm - 4 Cards) -->
      <div class="metrics-kpi-grid">
        <div class="kpi-card" @click="resetFilters">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">01</span>
            <span class="kpi-label">Tipos Registrados</span>
            <i class="pi pi-box kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ tiposCarga.length }}</span>
            <span class="kpi-badge accent">Clasificaciones</span>
          </div>
          <div class="kpi-footer-note">
            <span>Contenedores 20', 40' Dry, Reefer, Suelta</span>
          </div>
        </div>

        <div class="kpi-card success-card" @click="filterByStatus('ACTIVO')">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">02</span>
            <span class="kpi-label">Cargas Activas</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-success">{{ activasCount }}</span>
            <span class="kpi-badge success">Habilitadas</span>
          </div>
          <div class="kpi-footer-note">
            <span>Disponibles para cotización y despacho</span>
          </div>
        </div>

        <div class="kpi-card warning-card" @click="filterByContainer('40')">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">03</span>
            <span class="kpi-label">Contenedor FCL</span>
            <i class="pi pi-table kpi-icon warning"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-warning">{{ fclCount }}</span>
            <span class="kpi-badge warning font-mono">20' / 40' Dry & Reefer</span>
          </div>
          <div class="kpi-footer-note">
            <span>Carga contenerizada estándar e ISO</span>
          </div>
        </div>

        <div class="kpi-card info-card" @click="filterByContainer('SUELTA')">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">04</span>
            <span class="kpi-label">Carga Suelta / LCL</span>
            <i class="pi pi-inbox kpi-icon info"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-info">{{ lclCount }}</span>
            <span class="kpi-badge info">Consolidada</span>
          </div>
          <div class="kpi-footer-note">
            <span>Carga fraccionada, paletizada y granel</span>
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
              v-model.trim="tiposSearch"
              type="text"
              placeholder="Buscar por código o descripción de carga..."
              class="search-input"
            />
            <button
              v-if="tiposSearch"
              type="button"
              class="clear-search-btn"
              @click="tiposSearch = ''"
              title="Limpiar búsqueda"
            >
              <i class="pi pi-times"></i>
            </button>
          </div>

          <!-- Filtro por Tipo de Contenedor -->
          <div class="filter-dropdown">
            <select v-model="tiposContainerFilter" class="filter-select" title="Filtrar por contenedor sugerido">
              <option value="ALL">Todos los Contenedores</option>
              <option value="20">Contenedores 20'</option>
              <option value="40">Contenedores 40'</option>
              <option value="REEFER">Reefer (Refrigerado)</option>
              <option value="SUELTA">Carga Suelta / No Aplica</option>
            </select>
          </div>

          <!-- Filtro por Estado -->
          <div class="filter-dropdown">
            <select v-model="tiposStatusFilter" class="filter-select" title="Filtrar por estado">
              <option value="ALL">Todos los Estados</option>
              <option value="ACTIVO">Solo Activos</option>
              <option value="INACTIVO">Solo Inactivos</option>
            </select>
          </div>

          <!-- Ordenar Por -->
          <div class="sort-dropdown">
            <select v-model="tiposSortBy" class="filter-select sort-select" title="Ordenar resultados">
              <option value="nombre-asc">Ordenar por: Descripción (A - Z)</option>
              <option value="nombre-desc">Ordenar por: Descripción (Z - A)</option>
              <option value="codigo-asc">Ordenar por: Código de Carga (A - Z)</option>
              <option value="tara-desc">Ordenar por: Tara (Mayor a Menor)</option>
              <option value="tara-asc">Ordenar por: Tara (Menor a Mayor)</option>
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
          :value="sortedAndFilteredTiposCarga"
          :loading="loadingTipos"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idTipoCarga"
          paginator
          :rows="15"
          :rowsPerPageOptions="[15, 30, 50]"
          currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} tipos de carga"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-inbox empty-icon"></i>
              <p class="empty-title">No se encontraron tipos de carga registrados</p>
              <p class="empty-desc">Pruebe ajustando los filtros o registre un nuevo tipo de carga.</p>
            </div>
          </template>

          <template #loading>
            <div class="loading-state">
              <i class="pi pi-spin pi-spinner"></i>
              <span>Cargando catálogo de tipos de carga...</span>
            </div>
          </template>

          <Column field="codigo" header="CÓDIGO CARGA" :sortable="true" style="width: 170px;">
            <template #body="{ data: tipo }">
              <span class="code-badge font-mono">{{ tipo.codigo }}</span>
            </template>
          </Column>

          <Column field="nombre" header="DESCRIPCIÓN DE MERCANCÍA / CARGA" :sortable="true">
            <template #body="{ data: tipo }">
              <div class="port-name-cell">
                <span class="port-name">{{ tipo.nombre }}</span>
                <span v-if="tipo.descripcion" class="port-desc-sub">{{ tipo.descripcion }}</span>
                <span class="port-id-sub font-mono">Clasificación Técnica #{{ tipo.idTipoCarga }}</span>
              </div>
            </template>
          </Column>

          <Column field="tipoContenedorSugerido" header="CONTENEDOR SUGERIDO" :sortable="true" style="width: 190px;">
            <template #body="{ data: tipo }">
              <span class="container-pill">{{ tipo.tipoContenedorSugerido || 'Estándar / N/A' }}</span>
            </template>
          </Column>

          <Column field="taraSugeridaKg" header="TARA SUGERIDA" :sortable="true" style="width: 160px; text-align: right;" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: tipo }">
              <span class="tara-val font-mono font-bold">{{ formatNumber(tipo.taraSugeridaKg) }} kg</span>
            </template>
          </Column>

          <Column field="estado" header="ESTADO" :sortable="true" style="width: 130px; text-align: center;" headerClass="text-center" bodyClass="text-center">
            <template #body="{ data: tipo }">
              <Tag
                :value="tipo.estado"
                :severity="tipo.estado === 'ACTIVO' ? 'success' : 'danger'"
                rounded
              />
            </template>
          </Column>

          <Column header="ACCIONES" style="width: 180px; text-align: right;" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: tipo }">
              <div class="actions-cell">
                <button
                  type="button"
                  class="action-btn edit-btn"
                  @click="openEditTipoModal(tipo)"
                  title="Editar especificaciones de carga"
                >
                  <i class="pi pi-pencil"></i>
                  <span>Editar</span>
                </button>

                <button
                  v-if="tipo.estado === 'ACTIVO'"
                  type="button"
                  class="action-btn deactivate-btn"
                  @click="handleDeactivateTipo(tipo)"
                  title="Desactivar tipo de carga"
                >
                  <i class="pi pi-ban"></i>
                </button>

                <button
                  v-else
                  type="button"
                  class="action-btn reactivate-btn"
                  @click="handleReactivateTipo(tipo)"
                  title="Reactivar tipo de carga"
                >
                  <i class="pi pi-check-circle"></i>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import catalogoService from '../services/catalogoService'
import TipoCargaRegistrationForm from '../components/TipoCargaRegistrationForm.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { openBlobInNewTab } from '@/shared/utils/download'

const { showSuccess, showError, showInfo, showWarn } = useNotification()
const { confirmAction } = useConfirmation()

// Estado General
const tiposCarga = ref([])
const loadingTipos = ref(false)
const exportingPdf = ref(false)

// Filtros y Ordenación
const tiposSearch = ref('')
const tiposStatusFilter = ref('ALL')
const tiposContainerFilter = ref('ALL')
const tiposSortBy = ref('nombre-asc')

// Formulario Modal/Completo
const showTipoCargaForm = ref(false)
const selectedTipo = ref(null)

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
  return tiposCarga.value.filter((t) => t.estado === 'ACTIVO').length
})

const fclCount = computed(() => {
  return tiposCarga.value.filter((t) => {
    const s = (t.tipoContenedorSugerido || '').toUpperCase()
    return s.includes('20') || s.includes('40') || s.includes('REEFER') || s.includes('DRY')
  }).length
})

const lclCount = computed(() => {
  return tiposCarga.value.filter((t) => {
    const s = (t.tipoContenedorSugerido || '').toUpperCase()
    return s.includes('SUELTA') || s.includes('N/A') || !t.tipoContenedorSugerido
  }).length
})

const hasActiveFilters = computed(() => {
  return Boolean(
    tiposSearch.value ||
    tiposStatusFilter.value !== 'ALL' ||
    tiposContainerFilter.value !== 'ALL' ||
    tiposSortBy.value !== 'nombre-asc'
  )
})

const resetFilters = () => {
  tiposSearch.value = ''
  tiposStatusFilter.value = 'ALL'
  tiposContainerFilter.value = 'ALL'
  tiposSortBy.value = 'nombre-asc'
}

const filterByStatus = (status) => {
  tiposStatusFilter.value = status
}

const filterByContainer = (tipoKeyword) => {
  tiposContainerFilter.value = tipoKeyword
}

const formatNumber = (num) => {
  if (num === null || num === undefined) return '0.00'
  return Number(num).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// Filtrado de Tipos de Carga
const filteredTiposCarga = computed(() => {
  return tiposCarga.value.filter((t) => {
    const q = tiposSearch.value.toLowerCase().trim()
    const matchesSearch =
      !q ||
      t.codigo?.toLowerCase().includes(q) ||
      t.nombre?.toLowerCase().includes(q) ||
      t.tipoContenedorSugerido?.toLowerCase().includes(q)

    const matchesStatus =
      tiposStatusFilter.value === 'ALL' || t.estado === tiposStatusFilter.value

    let matchesContainer = true
    if (tiposContainerFilter.value !== 'ALL') {
      const c = (t.tipoContenedorSugerido || '').toUpperCase()
      if (tiposContainerFilter.value === 'SUELTA') {
        matchesContainer = c.includes('SUELTA') || c.includes('N/A') || !t.tipoContenedorSugerido
      } else {
        matchesContainer = c.includes(tiposContainerFilter.value)
      }
    }

    return matchesSearch && matchesStatus && matchesContainer
  })
})

// Ordenación Aplicada
const sortedAndFilteredTiposCarga = computed(() => {
  const list = [...filteredTiposCarga.value]
  switch (tiposSortBy.value) {
    case 'nombre-asc':
      return list.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''))
    case 'nombre-desc':
      return list.sort((a, b) => (b.nombre || '').localeCompare(a.nombre || ''))
    case 'codigo-asc':
      return list.sort((a, b) => (a.codigo || '').localeCompare(b.codigo || ''))
    case 'tara-desc':
      return list.sort((a, b) => Number(b.taraSugeridaKg || 0) - Number(a.taraSugeridaKg || 0))
    case 'tara-asc':
      return list.sort((a, b) => Number(a.taraSugeridaKg || 0) - Number(b.taraSugeridaKg || 0))
    case 'estado-asc':
      return list.sort((a, b) => (a.estado === 'ACTIVO' ? -1 : 1))
    default:
      return list
  }
})

// Cargar Tipos de Carga
const loadTiposCarga = async () => {
  loadingTipos.value = true
  try {
    const res = await catalogoService.getTiposCarga(false)
    tiposCarga.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.error('Error cargando tipos de carga:', error)
    showToast('Error al conectar con el catálogo de tipos de carga', 'error')
  } finally {
    loadingTipos.value = false
  }
}

// Acciones Tipos de Carga
const openCreateTipoModal = () => {
  selectedTipo.value = null
  showTipoCargaForm.value = true
}

const openEditTipoModal = (tipo) => {
  selectedTipo.value = { ...tipo }
  showTipoCargaForm.value = true
}

const onTipoSaved = (payload) => {
  showTipoCargaForm.value = false
  showToast(payload?.isEdit ? 'Tipo de carga actualizado exitosamente' : 'Nuevo tipo de carga registrado')
  loadTiposCarga()
}

const handleDeactivateTipo = async (tipo) => {
  const confirmed = await confirmAction({
    message: `¿Desea marcar el tipo de carga ${tipo.codigo} como inactivo?`,
    header: 'Confirmar Desactivación de Tipo de Carga',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Desactivar',
    acceptClass: 'p-button-danger'
  })
  if (!confirmed) return

  try {
    await catalogoService.deleteTipoCarga(tipo.idTipoCarga)
    showToast(`Tipo de carga ${tipo.codigo} marcado como inactivo`)
    loadTiposCarga()
  } catch (error) {
    console.error('Error desactivando tipo de carga:', error)
    showToast('No se pudo desactivar el tipo de carga', 'error')
  }
}

const handleReactivateTipo = async (tipo) => {
  try {
    await catalogoService.reactivarTipoCarga(tipo.idTipoCarga)
    showToast(`Tipo de carga ${tipo.codigo} reactivado exitosamente`)
    loadTiposCarga()
  } catch (error) {
    console.error('Error reactivando tipo de carga:', error)
    showToast('No se pudo reactivar el tipo de carga', 'error')
  }
}

// Visualizar / Descargar Reporte PDF en nueva pestaña
const handleExportPdf = async () => {
  exportingPdf.value = true
  try {
    const blobData = await catalogoService.downloadTiposCargaPdf()
    openBlobInNewTab(blobData)
    showToast('Reporte oficial de tipos de carga abierto en nueva pestaña')
  } catch (err) {
    console.error('Error al descargar reporte de tipos de carga:', err)
    showToast('No se pudo generar el reporte de tipos de carga', 'error')
  } finally {
    exportingPdf.value = false
  }
}

onMounted(() => {
  loadTiposCarga()
})
</script>

<style scoped>
.tipos-carga-management-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

.tipos-master-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR DE ACCIONES WARM EDITORIAL
   ========================================================================== */
.tipos-action-toolbar {
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

.port-desc-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.3;
}

.port-id-sub {
  font-size: 0.7rem;
  color: var(--color-text-subtle);
}

.container-pill {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.25rem 0.6rem;
  border-radius: var(--radius-sm);
}

.tara-val {
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
