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
      <!-- 1. Hero Banner Card -->
      <HeroBannerCard
        title="Gestión de sedes, agencias y depósitos aduaneros"
        kicker="INFRAESTRUCTURA & CONTROL TERRITORIAL"
        description="Supervisión de sucursales operativas a nivel nacional, terminales de despacho y puntos de enlace logístico asignados a colaboradores."
      />

      <!-- 2. Tarjetas de Métricas Rápidas (01, 02, 03, 04) -->
      <div class="metrics-grid">
        <MetricCard index="01" label="Total Sedes Registradas" :value="sedes.length" />
        <MetricCard index="02" label="Sedes Operativas" :value="operativasCount" severity="success" />
        <MetricCard index="03" label="Inactivas / Mantenimiento" :value="inactivasCount" severity="danger" />
        <MetricCard index="04" label="Jurisdicciones / Distritos" :value="distritosCount" severity="accent" />
      </div>

    <!-- 3. Toolbar y Tabla Principal -->
    <div class="section-container">
      <div class="section-header-row">
        <div>
          <span class="badge-kicker">DIRECTORIO TERRITORIAL</span>
          <h3 class="section-title">Infraestructura de Sedes y Agencias</h3>
        </div>

        <div class="section-controls">
          <span class="count-badge">{{ filteredSedes.length }} de {{ sedes.length }} sedes</span>

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
import HeroBannerCard from '@/shared/components/HeroBannerCard.vue'
import MetricCard from '@/shared/components/MetricCard.vue'

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
  gap: 1.75rem;
  animation: fadeIn 0.3s ease-out;
}

/* 1. Hero Banner */
.hero-banner-card {
  position: relative;
  background: linear-gradient(135deg, #1c1917 0%, #292524 60%, #453406 100%);
  border: 1px solid rgba(234, 179, 8, 0.25);
  border-radius: var(--radius-lg, 12px);
  padding: 1.75rem 2rem;
  color: #fafaf9;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.hero-content {
  max-width: 780px;
  position: relative;
  z-index: 2;
}

.badge-kicker {
  display: inline-block;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #eab308;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
}

.hero-title {
  font-size: 1.65rem;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 0.5rem;
  letter-spacing: -0.015em;
  line-height: 1.25;
}

.hero-desc {
  font-size: 0.875rem;
  color: #d6d3d1;
  margin: 0;
  line-height: 1.5;
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

/* 2. Métricas Rápidas */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}

.metric-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.metric-top {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  margin-bottom: 0.65rem;
}

.metric-idx {
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--color-accent-kicker);
}

.metric-label {
  font-size: 0.775rem;
  font-weight: 600;
  color: var(--color-text-subtle);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.metric-val {
  font-size: 2.25rem;
  font-weight: 800;
  color: var(--color-text-main);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.metric-val-active {
  color: var(--color-success-text);
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
