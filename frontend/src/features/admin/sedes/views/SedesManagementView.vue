<template>
  <div class="sedes-management-view">
    <!-- Formulario de Registro / Edición a Pantalla Completa (Estilo Editorial Maestro) -->
    <SedeRegistrationForm
      v-if="showForm"
      :sede-to-edit="selectedSede"
      @cancel="showForm = false"
      @saved="handleSedeSaved"
    />

    <!-- Directorio y Métricas Territoriales -->
    <template v-else>
      <!-- 1. Toolbar de Acciones Warm Editorial (Sin Banners Invasivos) -->
      <div class="sedes-action-toolbar">
        <div class="toolbar-left-info">
          <span class="badge-kicker">INFRAESTRUCTURA & CONTROL TERRITORIAL // SEDES ADUANERAS</span>
          <h3 class="toolbar-title">Directorio de Sedes y Agencias Territoriales</h3>
        </div>

        <div class="toolbar-actions-group">
          <span class="count-badge font-mono">{{ filteredSedes.length }} de {{ sedes.length }} sedes</span>

          <button
            type="button"
            class="btn-outline-warm"
            @click="loadSedes"
            :disabled="loading"
            title="Recargar datos desde el servidor"
          >
            <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
            <span>Actualizar</span>
          </button>

          <!-- Botón de Exportación JasperReports -->
          <button
            type="button"
            class="btn-outline-warm"
            @click="handleExportPdf"
            :disabled="exportingPdf"
            title="Descargar reporte oficial JasperReports en PDF"
          >
            <i :class="['pi', exportingPdf ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
            <span>Ver Reporte (PDF)</span>
          </button>

          <!-- Botón Crear Nueva Sede -->
          <button
            type="button"
            class="btn-espresso"
            @click="openCreateModal"
          >
            <i class="pi pi-plus"></i>
            <span>+ Nueva Sede</span>
          </button>
        </div>
      </div>

      <!-- 2. Grid de Métricas KPI Técnicas (Estándar Editorial Warm) -->
      <div class="metrics-kpi-grid">
        <div class="kpi-card" @click="loadSedes">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">Sedes Registradas</span>
            <i class="pi pi-building kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ sedes.length }}</span>
            <span class="kpi-badge accent">Agencias</span>
          </div>
          <div class="kpi-footer-note">
            <span>Red territorial operativa nacional</span>
          </div>
        </div>

        <div class="kpi-card success-card">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">Sedes Operativas</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-success">{{ operativasCount }}</span>
            <span class="kpi-badge success">En Servicio</span>
          </div>
          <div class="kpi-footer-note">
            <span>Operaciones aduaneras activas</span>
          </div>
        </div>

        <div class="kpi-card danger-card">
          <div class="kpi-card-header">
            <span class="kpi-num">03</span>
            <span class="kpi-label">Inactivas / Mant.</span>
            <i class="pi pi-exclamation-circle kpi-icon danger"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-danger">{{ inactivasCount }}</span>
            <span class="kpi-badge warning">En Pausa</span>
          </div>
          <div class="kpi-footer-note">
            <span>Terminales fuera de servicio</span>
          </div>
        </div>

        <div class="kpi-card">
          <div class="kpi-card-header">
            <span class="kpi-num">04</span>
            <span class="kpi-label">Jurisdicciones</span>
            <i class="pi pi-map-marker kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ distritosCount }}</span>
            <span class="kpi-badge info">Distritos</span>
          </div>
          <div class="kpi-footer-note">
            <span>Callao, Paita, Matarani, Jorge Chávez</span>
          </div>
        </div>
      </div>

      <!-- 3. Contenedor de Directorio y Tabla Principal -->
      <div class="section-container">

      <!-- Barra de Filtros y Búsqueda -->
      <div class="filters-bar">
        <div class="search-input-wrapper">
          <i class="pi pi-search search-icon"></i>
          <input
            v-model.trim="searchQuery"
            type="text"
            placeholder="Buscar por código, nombre, distrito o dirección..."
            class="table-search-input"
          />
          <button
            v-if="searchQuery"
            type="button"
            class="clear-search-btn"
            @click="searchQuery = ''"
            title="Limpiar búsqueda"
          >
            <i class="pi pi-times"></i>
          </button>
        </div>

        <div class="filter-group">
          <!-- Filtro por Tipo -->
          <select v-model="filterTipo" class="filter-select">
            <option value="">Todos los Tipos</option>
            <option value="SEDE_CENTRAL">Sede Central</option>
            <option value="AGENCIA">Agencia Aduanera</option>
            <option value="SUCURSAL">Sucursal</option>
            <option value="TERMINAL">Terminal / Depósito</option>
          </select>

          <!-- Filtro por Estado -->
          <select v-model="filterEstado" class="filter-select">
            <option value="">Todos los Estados</option>
            <option value="OPERATIVO">Operativo</option>
            <option value="INACTIVO">Inactivo</option>
            <option value="MANTENIMIENTO">Mantenimiento</option>
          </select>
        </div>
      </div>

      <!-- Tabla PrimeVue DataTable -->
      <div class="table-card">
        <DataTable
          :value="filteredSedes"
          :loading="loading"
          responsiveLayout="scroll"
          class="custom-datatable p-datatable-sm"
          paginator
          :rows="10"
          :rowsPerPageOptions="[10, 20, 50]"
          currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} sedes"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        >
          <!-- Empty State -->
          <template #empty>
            <div class="empty-state-box">
              <i class="pi pi-building empty-icon"></i>
              <p class="empty-text">No se encontraron sedes aduaneras registradas.</p>
            </div>
          </template>

          <!-- Código de Sede -->
          <Column field="codigoSede" header="CÓDIGO" :sortable="true" style="min-width: 140px">
            <template #body="{ data }">
              <span class="code-badge">{{ data.codigoSede }}</span>
            </template>
          </Column>

          <!-- Nombre y Tipo -->
          <Column field="nombre" header="NOMBRE DE SEDE" :sortable="true" style="min-width: 240px">
            <template #body="{ data }">
              <div class="sede-info-cell">
                <span class="sede-name-text">{{ data.nombre }}</span>
                <Tag
                  :value="formatTipoLabel(data.tipo)"
                  :severity="getTipoSeverity(data.tipo)"
                  rounded
                  class="tipo-tag"
                />
              </div>
            </template>
          </Column>

          <!-- Distrito & Dirección -->
          <Column field="distrito" header="UBICACIÓN" :sortable="true" style="min-width: 220px">
            <template #body="{ data }">
              <div class="location-cell">
                <span class="distrito-text"><i class="pi pi-map-marker"></i> {{ data.distrito }}</span>
                <span class="direccion-text">{{ data.direccion }}</span>
              </div>
            </template>
          </Column>

          <!-- Teléfono -->
          <Column field="telefono" header="TELÉFONO" style="min-width: 130px">
            <template #body="{ data }">
              <span class="phone-text">
                <i v-if="data.telefono" class="pi pi-phone phone-icon"></i>
                {{ data.telefono || '—' }}
              </span>
            </template>
          </Column>

          <!-- Estado -->
          <Column field="estado" header="ESTADO" :sortable="true" style="min-width: 120px">
            <template #body="{ data }">
              <Tag
                :value="data.estado"
                :severity="getEstadoSeverity(data.estado)"
                rounded
              />
            </template>
          </Column>

          <!-- Acciones -->
          <Column header="ACCIONES" style="min-width: 140px; text-align: right">
            <template #body="{ data }">
              <div class="action-buttons-cell">
                <!-- Editar -->
                <button
                  type="button"
                  class="action-btn edit-action"
                  @click="openEditModal(data)"
                  title="Editar datos de sede"
                >
                  <i class="pi pi-pencil"></i>
                  <span>Editar</span>
                </button>

                <!-- Desactivar (si está operativo) -->
                <button
                  v-if="data.estado === 'OPERATIVO'"
                  type="button"
                  class="action-btn delete-action"
                  @click="handleToggleEstado(data)"
                  title="Desactivar sede"
                >
                  <i class="pi pi-ban"></i>
                  <span>Desactivar</span>
                </button>

                <!-- Reactivar (si está inactivo o en mantenimiento) -->
                <button
                  v-else
                  type="button"
                  class="action-btn reactivate-action"
                  @click="handleToggleEstado(data)"
                  title="Reactivar sede"
                >
                  <i class="pi pi-check-circle"></i>
                  <span>Reactivar</span>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import SedeRegistrationForm from '../components/SedeRegistrationForm.vue'
import sedeService from '../services/sedeService'
import { downloadBlob } from '@/shared/utils/download'

const sedes = ref([])
const loading = ref(false)
const exportingPdf = ref(false)
const searchQuery = ref('')
const filterTipo = ref('')
const filterEstado = ref('')

const showForm = ref(false)
const selectedSede = ref(null)

const operativasCount = computed(() => {
  return sedes.value.filter((s) => s.estado === 'OPERATIVO').length
})

const inactivasCount = computed(() => {
  return sedes.value.filter((s) => s.estado !== 'OPERATIVO').length
})

const distritosCount = computed(() => {
  const set = new Set(sedes.value.map((s) => s.distrito).filter(Boolean))
  return set.size
})

const filteredSedes = computed(() => {
  let list = [...sedes.value]

  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(
      (s) =>
        (s.codigoSede && s.codigoSede.toLowerCase().includes(q)) ||
        (s.nombre && s.nombre.toLowerCase().includes(q)) ||
        (s.distrito && s.distrito.toLowerCase().includes(q)) ||
        (s.direccion && s.direccion.toLowerCase().includes(q))
    )
  }

  if (filterTipo.value) {
    list = list.filter((s) => s.tipo === filterTipo.value)
  }

  if (filterEstado.value) {
    list = list.filter((s) => s.estado === filterEstado.value)
  }

  return list
})

const loadSedes = async () => {
  loading.value = true
  try {
    const data = await sedeService.getSedes(false)
    sedes.value = data || []
  } catch (err) {
    console.error('Error al cargar sedes:', err)
  } finally {
    loading.value = false
  }
}

const openCreateModal = () => {
  selectedSede.value = null
  showForm.value = true
}

const openEditModal = (sede) => {
  selectedSede.value = sede
  showForm.value = true
}

const handleSedeSaved = () => {
  showForm.value = false
  loadSedes()
}

const handleToggleEstado = async (sede) => {
  try {
    if (sede.estado === 'OPERATIVO') {
      await sedeService.deleteSede(sede.idSede)
    } else {
      await sedeService.reactivateSede(sede.idSede)
    }
    await loadSedes()
  } catch (err) {
    console.error('Error al cambiar estado de sede:', err)
  }
}

const handleExportPdf = async () => {
  exportingPdf.value = true
  try {
    const blob = await sedeService.downloadPdf()
    downloadBlob(blob, `reporte-sedes-operativas-${new Date().toISOString().slice(0, 10)}.pdf`)
  } catch (err) {
    console.error('Error al descargar reporte de sedes:', err)
  } finally {
    exportingPdf.value = false
  }
}

const formatTipoLabel = (tipo) => {
  const map = {
    SEDE_CENTRAL: 'Central',
    AGENCIA: 'Agencia',
    SUCURSAL: 'Sucursal',
    TERMINAL: 'Terminal'
  }
  return map[tipo] || tipo
}

const getTipoSeverity = (tipo) => {
  switch (tipo) {
    case 'SEDE_CENTRAL':
      return 'info'
    case 'AGENCIA':
      return 'success'
    case 'TERMINAL':
      return 'warn'
    default:
      return 'secondary'
  }
}

const getEstadoSeverity = (estado) => {
  switch (estado) {
    case 'OPERATIVO':
      return 'success'
    case 'MANTENIMIENTO':
      return 'warn'
    case 'INACTIVO':
    default:
      return 'danger'
  }
}

onMounted(() => {
  loadSedes()
})
</script>

<style scoped>
.sedes-management-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR DE ACCIONES WARM EDITORIAL
   ========================================================================== */
.sedes-action-toolbar {
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
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-pill, 9999px);
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

.kpi-icon.danger {
  color: var(--color-danger-text, #ef4444);
}

.kpi-icon.success {
  color: var(--color-success-text, #16a34a);
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

.text-danger {
  color: var(--color-danger-text, #ef4444) !important;
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
  background: var(--color-bg-surface);
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
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

/* 3. Sección Principal */
.section-container {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: 1.75rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.section-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  margin-bottom: 1.25rem;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.section-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.count-badge {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.4rem 0.75rem;
  border-radius: var(--radius-pill);
}

/* Barra de Filtros */
.filters-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 300px;
}

.search-icon {
  position: absolute;
  left: 0.85rem;
  color: var(--color-text-subtle);
  font-size: 0.875rem;
  pointer-events: none;
}

.table-search-input {
  width: 100%;
  padding: 0.6rem 2.2rem 0.6rem 2.4rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-main);
  font-size: 0.85rem;
  font-family: inherit;
  outline: none;
  transition: all var(--transition-fast);
}

.table-search-input:focus {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(214, 180, 118, 0.2);
}

.clear-search-btn {
  position: absolute;
  right: 0.65rem;
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.2rem;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.filter-select {
  padding: 0.6rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-main);
  font-size: 0.825rem;
  font-family: inherit;
  cursor: pointer;
  outline: none;
}

.filter-select:focus {
  border-color: var(--color-primary);
}

.table-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

:deep(.p-datatable .p-paginator) {
  background: var(--color-bg-subtle) !important;
  border-top: 1px solid var(--color-border-subtle) !important;
  color: var(--color-text-muted) !important;
}

/* Celdas Custom en Tabla */
.code-badge {
  font-family: monospace;
  font-size: 0.775rem;
  font-weight: 800;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
  border: 1px solid var(--color-border-subtle);
}

.sede-info-cell {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.sede-name-text {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.tipo-tag {
  align-self: flex-start;
  font-size: 0.65rem !important;
  padding: 0.1rem 0.45rem !important;
}

.location-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.distrito-text {
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.distrito-text i {
  color: var(--color-accent-kicker);
  font-size: 0.8rem;
}

.direccion-text {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.phone-text {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.phone-icon {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.action-buttons-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
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

.edit-action {
  background: var(--color-bg-subtle);
  border-color: var(--color-border);
  color: var(--color-text-main);
}

.edit-action:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.delete-action {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.delete-action:hover {
  filter: brightness(1.1);
}

.reactivate-action {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.reactivate-action:hover {
  filter: brightness(1.1);
}

.empty-state-box {
  padding: 3rem 1rem;
  text-align: center;
  color: var(--color-text-subtle);
}

.empty-icon {
  font-size: 2.25rem;
  margin-bottom: 0.75rem;
  color: var(--color-accent-kicker);
}

.empty-text {
  font-size: 0.9rem;
  margin: 0;
}

@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  .section-header-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
