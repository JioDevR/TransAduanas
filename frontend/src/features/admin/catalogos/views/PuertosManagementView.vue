<template>
  <div class="puertos-management-view">

    <!-- Vista Completa: Formulario Maestro de Puerto / Terminal -->
    <PuertoRegistrationForm
      v-if="showPuertoForm"
      :puerto-to-edit="selectedPuerto"
      @cancel="showPuertoForm = false"
      @saved="onPuertoSaved"
    />

    <!-- Vista Principal: Directorio y Tablas Maestras de Puertos -->
    <div v-else class="puertos-master-container">

      <!-- 1. Toolbar de Acciones Warm Editorial (Única Fuente de Verdad para Acciones) -->
      <div class="puertos-action-toolbar">
        <div class="toolbar-left-info">
          <span class="badge-kicker">CATÁLOGOS AUXILIARES // TABLAS MAESTRAS ADUANERAS</span>
          <h3 class="toolbar-title">Puertos y Terminales Aduaneros</h3>
        </div>

        <div class="toolbar-actions-group">
          <span class="count-badge font-mono">{{ filteredPuertos.length }} de {{ puertos.length }} terminales</span>

          <!-- Botón Actualizar -->
          <button
            type="button"
            class="btn-outline-warm"
            @click="loadPuertos"
            :disabled="loadingPuertos"
            title="Recargar catálogo de puertos desde el servidor"
          >
            <i :class="['pi pi-sync', { 'pi-spin': loadingPuertos }]"></i>
            <span>Actualizar</span>
          </button>

          <!-- Botón Exportar PDF de Puertos con JasperReports -->
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

          <!-- Botón Principal: + Nuevo Puerto (Único botón de creación) -->
          <button
            type="button"
            class="btn-espresso"
            @click="openCreatePuertoModal"
          >
            <i class="pi pi-plus"></i>
            <span>+ Nuevo Puerto</span>
          </button>
        </div>
      </div>

      <!-- 2. Grid de Métricas KPI Técnicas (Estándar Editorial Warm - 4 Cards) -->
      <div class="metrics-kpi-grid">
        <div class="kpi-card" @click="resetFilters">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">01</span>
            <span class="kpi-label">Puertos Totales</span>
            <i class="pi pi-compass kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ puertos.length }}</span>
            <span class="kpi-badge accent">Nacional / Int.</span>
          </div>
          <div class="kpi-footer-note">
            <span>Terminales marítimos, aéreos y fluviales</span>
          </div>
        </div>

        <div class="kpi-card success-card" @click="filterByTipo('MARITIMO')">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">02</span>
            <span class="kpi-label">Terminales Marítimos</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-success">{{ maritimePortsCount }}</span>
            <span class="kpi-badge success">Marítimos</span>
          </div>
          <div class="kpi-footer-note">
            <span>Callao, Paita, Matarani y otros</span>
          </div>
        </div>

        <div class="kpi-card info-card" @click="filterByTipo('AEREO')">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">03</span>
            <span class="kpi-label">Aéreos & Terrestres</span>
            <i class="pi pi-send kpi-icon info"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-info">{{ otherPortsCount }}</span>
            <span class="kpi-badge info">Aeropuertos</span>
          </div>
          <div class="kpi-footer-note">
            <span>Jorge Chávez y enlaces terrestres</span>
          </div>
        </div>

        <div class="kpi-card" @click="filterByStatus('ACTIVO')">
          <div class="kpi-card-header">
            <span class="kpi-num font-mono">04</span>
            <span class="kpi-label">Terminales Activos</span>
            <i class="pi pi-shield kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-success">{{ activePortsCount }}</span>
            <span class="kpi-badge success">Operativos</span>
          </div>
          <div class="kpi-footer-note">
            <span>Habilitados para trámites DAM y levante</span>
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
              v-model.trim="puertosSearch"
              type="text"
              placeholder="Buscar por código, nombre o país..."
              class="search-input"
            />
            <button
              v-if="puertosSearch"
              type="button"
              class="clear-search-btn"
              @click="puertosSearch = ''"
              title="Limpiar búsqueda"
            >
              <i class="pi pi-times"></i>
            </button>
          </div>

          <!-- Filtro por Tipo de Terminal -->
          <div class="filter-dropdown">
            <select v-model="puertosTypeFilter" class="filter-select" title="Filtrar por tipo de puerto">
              <option value="ALL">Todos los Tipos</option>
              <option value="MARITIMO">Marítimo</option>
              <option value="AEREO">Aéreo</option>
              <option value="TERRESTRE">Terrestre</option>
              <option value="FLUVIAL">Fluvial</option>
            </select>
          </div>

          <!-- Filtro por Estado -->
          <div class="filter-dropdown">
            <select v-model="puertosStatusFilter" class="filter-select" title="Filtrar por estado">
              <option value="ALL">Todos los Estados</option>
              <option value="ACTIVO">Solo Activos</option>
              <option value="INACTIVO">Solo Inactivos</option>
            </select>
          </div>

          <!-- Ordenar Por -->
          <div class="sort-dropdown">
            <select v-model="puertosSortBy" class="filter-select sort-select" title="Ordenar resultados">
              <option value="nombre-asc">Ordenar por: Nombre (A - Z)</option>
              <option value="nombre-desc">Ordenar por: Nombre (Z - A)</option>
              <option value="codigo-asc">Ordenar por: Código (A - Z)</option>
              <option value="pais-asc">Ordenar por: País (A - Z)</option>
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
          :value="sortedAndFilteredPuertos"
          :loading="loadingPuertos"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idPuerto"
          paginator
          :rows="10"
          :rowsPerPageOptions="[10, 20, 50]"
          currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} puertos"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-inbox empty-icon"></i>
              <p class="empty-title">No se encontraron puertos registrados</p>
              <p class="empty-desc">Pruebe ajustando los filtros de búsqueda o registre un nuevo puerto.</p>
            </div>
          </template>

          <template #loading>
            <div class="loading-state">
              <i class="pi pi-spin pi-spinner"></i>
              <span>Cargando catálogo de puertos aduaneros...</span>
            </div>
          </template>

          <Column field="codigoPuerto" header="CÓDIGO PUERTO" :sortable="true" style="width: 150px;">
            <template #body="{ data: puerto }">
              <span class="code-badge font-mono">{{ puerto.codigoPuerto }}</span>
            </template>
          </Column>

          <Column field="nombre" header="NOMBRE DEL PUERTO / TERMINAL" :sortable="true">
            <template #body="{ data: puerto }">
              <div class="port-name-cell">
                <span class="port-name">{{ puerto.nombre }}</span>
                <span class="port-id-sub font-mono">ID: #{{ puerto.idPuerto }}</span>
              </div>
            </template>
          </Column>

          <Column field="pais" header="PAÍS" :sortable="true" style="width: 150px;">
            <template #body="{ data: puerto }">
              <span class="country-pill">{{ puerto.pais }}</span>
            </template>
          </Column>

          <Column field="tipo" header="TIPO" :sortable="true" style="width: 160px;">
            <template #body="{ data: puerto }">
              <Tag
                :class="['type-badge', getPortTypeClass(puerto.tipo)]"
                rounded
              >
                <template #icon>
                  <i :class="getPortTypeIcon(puerto.tipo)"></i>
                </template>
                <span>{{ puerto.tipo }}</span>
              </Tag>
            </template>
          </Column>

          <Column field="estado" header="ESTADO" :sortable="true" style="width: 130px; text-align: center;" headerClass="text-center" bodyClass="text-center">
            <template #body="{ data: puerto }">
              <Tag
                :value="puerto.estado"
                :severity="puerto.estado === 'ACTIVO' ? 'success' : 'danger'"
                rounded
              />
            </template>
          </Column>

          <Column header="ACCIONES" style="width: 180px; text-align: right;" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: puerto }">
              <div class="actions-cell">
                <button
                  type="button"
                  class="action-btn edit-btn"
                  @click="openEditPuertoModal(puerto)"
                  title="Editar información del puerto"
                >
                  <i class="pi pi-pencil"></i>
                  <span>Editar</span>
                </button>

                <button
                  v-if="puerto.estado === 'ACTIVO'"
                  type="button"
                  class="action-btn deactivate-btn"
                  @click="handleDeactivatePuerto(puerto)"
                  title="Desactivar puerto"
                >
                  <i class="pi pi-ban"></i>
                </button>

                <button
                  v-else
                  type="button"
                  class="action-btn reactivate-btn"
                  @click="handleReactivatePuerto(puerto)"
                  title="Reactivar puerto"
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
import PuertoRegistrationForm from '../components/PuertoRegistrationForm.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'

const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction } = useConfirmation()

// Estado General
const puertos = ref([])
const loadingPuertos = ref(false)
const exportingPdf = ref(false)

// Filtros y Ordenación
const puertosSearch = ref('')
const puertosTypeFilter = ref('ALL')
const puertosStatusFilter = ref('ALL')
const puertosSortBy = ref('nombre-asc')

// Formulario Modal/Completo
const showPuertoForm = ref(false)
const selectedPuerto = ref(null)

const showToast = (message, type = 'success') => {
  if (type === 'error') {
    showError(message)
  } else if (type === 'info') {
    showInfo(message)
  } else {
    showSuccess(message)
  }
}

// Métricas Computadas
const maritimePortsCount = computed(() => {
  return puertos.value.filter((p) => p.tipo === 'MARITIMO' && p.estado === 'ACTIVO').length
})

const otherPortsCount = computed(() => {
  return puertos.value.filter((p) => p.tipo !== 'MARITIMO').length
})

const activePortsCount = computed(() => {
  return puertos.value.filter((p) => p.estado === 'ACTIVO').length
})

const hasActiveFilters = computed(() => {
  return Boolean(
    puertosSearch.value ||
    puertosTypeFilter.value !== 'ALL' ||
    puertosStatusFilter.value !== 'ALL' ||
    puertosSortBy.value !== 'nombre-asc'
  )
})

const resetFilters = () => {
  puertosSearch.value = ''
  puertosTypeFilter.value = 'ALL'
  puertosStatusFilter.value = 'ALL'
  puertosSortBy.value = 'nombre-asc'
}

const filterByTipo = (tipo) => {
  puertosTypeFilter.value = tipo
}

const filterByStatus = (status) => {
  puertosStatusFilter.value = status
}

// Filtrado de Puertos
const filteredPuertos = computed(() => {
  return puertos.value.filter((p) => {
    const q = puertosSearch.value.toLowerCase().trim()
    const matchesSearch =
      !q ||
      p.codigoPuerto?.toLowerCase().includes(q) ||
      p.nombre?.toLowerCase().includes(q) ||
      p.pais?.toLowerCase().includes(q)

    const matchesType =
      puertosTypeFilter.value === 'ALL' || p.tipo === puertosTypeFilter.value

    const matchesStatus =
      puertosStatusFilter.value === 'ALL' || p.estado === puertosStatusFilter.value

    return matchesSearch && matchesType && matchesStatus
  })
})

// Ordenación Aplicada
const sortedAndFilteredPuertos = computed(() => {
  const list = [...filteredPuertos.value]
  switch (puertosSortBy.value) {
    case 'nombre-asc':
      return list.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''))
    case 'nombre-desc':
      return list.sort((a, b) => (b.nombre || '').localeCompare(a.nombre || ''))
    case 'codigo-asc':
      return list.sort((a, b) => (a.codigoPuerto || '').localeCompare(b.codigoPuerto || ''))
    case 'pais-asc':
      return list.sort((a, b) => (a.pais || '').localeCompare(b.pais || ''))
    case 'estado-asc':
      return list.sort((a, b) => (a.estado === 'ACTIVO' ? -1 : 1))
    default:
      return list
  }
})

// Cargar Puertos
const loadPuertos = async () => {
  loadingPuertos.value = true
  try {
    const res = await catalogoService.getPuertos(false)
    puertos.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.error('Error cargando puertos:', error)
    showToast('Error al conectar con el catálogo de puertos', 'error')
  } finally {
    loadingPuertos.value = false
  }
}

// Descargar Reporte PDF
const handleExportPdf = async () => {
  exportingPdf.value = true
  try {
    const blobData = await catalogoService.downloadPuertosPdf()
    const url = window.URL.createObjectURL(new Blob([blobData], { type: 'application/pdf' }))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `reporte-puertos-aduaneros-${new Date().toISOString().slice(0, 10)}.pdf`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    showToast('Reporte oficial de puertos generado exitosamente')
  } catch (err) {
    console.error('Error al descargar reporte de puertos:', err)
    showToast('No se pudo generar el reporte oficial de puertos', 'error')
  } finally {
    exportingPdf.value = false
  }
}

// Helpers Visuales
const getPortTypeClass = (tipo) => {
  const map = {
    MARITIMO: 'type-maritimo',
    AEREO: 'type-aereo',
    TERRESTRE: 'type-terrestre',
    FLUVIAL: 'type-fluvial'
  }
  return map[tipo] || 'type-maritimo'
}

const getPortTypeIcon = (tipo) => {
  const map = {
    MARITIMO: 'pi pi-compass',
    AEREO: 'pi pi-send',
    TERRESTRE: 'pi pi-map-marker',
    FLUVIAL: 'pi pi-compass'
  }
  return map[tipo] || 'pi pi-compass'
}

// Acciones Puertos
const openCreatePuertoModal = () => {
  selectedPuerto.value = null
  showPuertoForm.value = true
}

const openEditPuertoModal = (puerto) => {
  selectedPuerto.value = { ...puerto }
  showPuertoForm.value = true
}

const onPuertoSaved = (payload) => {
  showPuertoForm.value = false
  showToast(payload?.isEdit ? 'Puerto actualizado exitosamente' : 'Nuevo puerto registrado en el catálogo')
  loadPuertos()
}

const handleDeactivatePuerto = async (puerto) => {
  const confirmed = await confirmAction({
    message: `¿Desea marcar el puerto ${puerto.codigoPuerto} — ${puerto.nombre} como inactivo?`,
    header: 'Confirmar Desactivación de Puerto',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Desactivar',
    acceptClass: 'p-button-danger'
  })
  if (!confirmed) return

  try {
    await catalogoService.deletePuerto(puerto.idPuerto)
    showToast(`Puerto ${puerto.codigoPuerto} marcado como inactivo`)
    loadPuertos()
  } catch (error) {
    console.error('Error desactivando puerto:', error)
    showToast('No se pudo desactivar el puerto', 'error')
  }
}

const handleReactivatePuerto = async (puerto) => {
  try {
    await catalogoService.reactivarPuerto(puerto.idPuerto)
    showToast(`Puerto ${puerto.codigoPuerto} reactivado exitosamente`)
    loadPuertos()
  } catch (error) {
    console.error('Error reactivando puerto:', error)
    showToast('No se pudo reactivar el puerto', 'error')
  }
}

onMounted(() => {
  loadPuertos()
})
</script>

<style scoped>
.puertos-management-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

.puertos-master-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR DE ACCIONES WARM EDITORIAL
   ========================================================================== */
.puertos-action-toolbar {
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
  max-width: 360px;
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

.type-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.25rem 0.65rem;
  letter-spacing: 0.04em;
}

.type-maritimo {
  background: #e0f2fe !important;
  color: #0369a1 !important;
  border: 1px solid #bae6fd !important;
}

.type-aereo {
  background: #fce7f3 !important;
  color: #be185d !important;
  border: 1px solid #fbcfe8 !important;
}

.type-terrestre {
  background: #fef3c7 !important;
  color: #92400e !important;
  border: 1px solid #fde68a !important;
}

.type-fluvial {
  background: #dcfce7 !important;
  color: #15803d !important;
  border: 1px solid #bbf7d0 !important;
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
