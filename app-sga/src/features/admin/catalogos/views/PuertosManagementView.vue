<template>
  <div class="catalogo-page fade-in">
    <PuertoRegistrationForm
      v-if="showForm"
      :puerto-to-edit="editingItem"
      @cancel="closeForm"
      @saved="onSaved"
    />

    <template v-else>
      <section class="sga-kpis-grid">
        <article
          class="sga-kpi-card is-primary"
          :class="{ 'is-active-filter': !hasActiveFilters }"
          @click="resetFilters"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">01</span>
            <span class="sga-kpi-label">Puertos Totales</span>
            <i class="pi pi-globe sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ items.length }}</span>
            <span class="sga-kpi-pill">UN/LOCODE</span>
          </div>
        </article>

        <article
          class="sga-kpi-card is-info"
          :class="{ 'is-active-filter': filters.tipo === 'MARITIMO' }"
          @click="toggleFilter('tipo', 'MARITIMO')"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">02</span>
            <span class="sga-kpi-label">Marítimos</span>
            <i class="pi pi-compass sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ maritimosCount }}</span>
            <span class="sga-kpi-pill is-info">Nodos Mar</span>
          </div>
        </article>

        <article
          class="sga-kpi-card is-warn"
          :class="{ 'is-active-filter': filters.tipo === 'AEREO' }"
          @click="toggleFilter('tipo', 'AEREO')"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">03</span>
            <span class="sga-kpi-label">Aéreos</span>
            <i class="pi pi-send sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ aereosCount }}</span>
            <span class="sga-kpi-pill is-warn">IATA</span>
          </div>
        </article>

        <article
          class="sga-kpi-card is-success"
          :class="{ 'is-active-filter': filters.estado === 'ACTIVO' }"
          @click="toggleFilter('estado', 'ACTIVO')"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">04</span>
            <span class="sga-kpi-label">Habilitados</span>
            <i class="pi pi-check-circle sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ activosCount }}</span>
            <span class="sga-kpi-pill is-success">Activos</span>
          </div>
        </article>
      </section>

      <SgaListShell v-model:viewMode="viewMode" title="Puertos y Terminales Aduaneros">
        <template #header-right>
          <div class="sga-dash-actions">
            <Tag :value="`${filteredItems.length} de ${items.length}`" severity="secondary" class="sga-mono" rounded />
            <Button icon="pi pi-sync" severity="secondary" outlined size="small" :loading="loading" aria-label="Actualizar" @click="loadItems" />
            <Button label="PDF" icon="pi pi-file-pdf" severity="secondary" outlined size="small" :loading="exportingPdf" @click="exportPdf" />
            <Button label="Nuevo Puerto" icon="pi pi-plus" size="small" @click="openCreate" />
          </div>
        </template>

        <template #filters>
          <IconField class="sga-filter-search">
            <InputIcon class="pi pi-search" />
            <InputText v-model="filters.search" size="small" placeholder="Buscar código UN/LOCODE, nombre o país..." fluid />
          </IconField>
          <Select v-model="filters.tipo" :options="tipoOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select" />
          <Select v-model="filters.estado" :options="estadoOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select" />
          <Select v-model="filters.sortBy" :options="sortOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select sga-sort-select" />
          <Button v-if="hasActiveFilters" icon="pi pi-filter-slash" label="Limpiar" severity="secondary" text size="small" @click="resetFilters" />
        </template>

        <div v-if="filteredItems.length === 0 && !loading" class="sga-empty-state">
          <i class="pi pi-compass sga-empty-icon"></i>
          <p class="sga-empty-title">No se encontraron puertos con los filtros aplicados</p>
          <Button v-if="hasActiveFilters" label="Restablecer Filtros" severity="secondary" outlined size="small" @click="resetFilters" />
        </div>

        <div v-else-if="viewMode === 'canvas'" class="sga-canvas-wrapper">
          <div class="sga-canvas-grid">
            <article v-for="puerto in filteredItems" :key="puerto.idPuerto" class="sga-card-surface sga-canvas-card">
              <div class="sga-card-top">
                <span class="sga-card-code sga-mono">{{ puerto.codigoPuerto }}</span>
                <Tag :value="puerto.estado" :severity="getCatalogoEstadoSeverity(puerto.estado)" rounded />
              </div>
              <div class="sga-card-client">
                <h4 class="sga-client-title">{{ puerto.nombre }}</h4>
                <p class="sga-client-meta">{{ puerto.pais }}</p>
              </div>
              <div class="sga-card-footer">
                <Tag :value="formatPuertoTipo(puerto.tipo)" :icon="getPuertoTipoIcon(puerto.tipo)" :severity="getPuertoTipoSeverity(puerto.tipo)" rounded />
                <CatalogoRowActions
                  :is-active="puerto.estado === 'ACTIVO'"
                  :label="puerto.codigoPuerto"
                  @edit="openEdit(puerto)"
                  @toggle="toggleEstado(puerto)"
                />
              </div>
            </article>
          </div>
        </div>

        <div v-else class="sga-table-scroll">
          <DataTable :value="filteredItems" :loading="loading" paginator :rows="12" dataKey="idPuerto" size="small" stripedRows>
            <Column field="codigoPuerto" header="CÓDIGO" sortable>
              <template #body="{ data }">
                <span class="sga-code-cell sga-mono">{{ data.codigoPuerto }}</span>
              </template>
            </Column>
            <Column field="nombre" header="PUERTO / TERMINAL" sortable />
            <Column field="pais" header="PAÍS" sortable />
            <Column field="tipo" header="MODALIDAD" sortable>
              <template #body="{ data }">
                <Tag :value="formatPuertoTipo(data.tipo)" :icon="getPuertoTipoIcon(data.tipo)" :severity="getPuertoTipoSeverity(data.tipo)" rounded />
              </template>
            </Column>
            <Column field="estado" header="ESTADO" sortable>
              <template #body="{ data }">
                <Tag :value="data.estado" :severity="getCatalogoEstadoSeverity(data.estado)" rounded />
              </template>
            </Column>
            <Column header="ACCIONES" style="width: 110px">
              <template #body="{ data }">
                <CatalogoRowActions
                  :is-active="data.estado === 'ACTIVO'"
                  :label="data.codigoPuerto"
                  @edit="openEdit(data)"
                  @toggle="toggleEstado(data)"
                />
              </template>
            </Column>
          </DataTable>
        </div>
      </SgaListShell>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import PuertoRegistrationForm from '../components/PuertoRegistrationForm.vue'
import CatalogoRowActions from '../components/CatalogoRowActions.vue'
import catalogoService from '../services/catalogoService'
import { useCatalogoList } from '../composables/useCatalogoList'
import {
  formatPuertoTipo,
  getPuertoTipoIcon,
  getPuertoTipoSeverity,
  getCatalogoEstadoSeverity
} from '@/features/gerencia/utils/gerenciaFormatters'

const tipoOptions = [
  { label: 'Todas las modalidades', value: '' },
  { label: 'Marítimo', value: 'MARITIMO' },
  { label: 'Aéreo', value: 'AEREO' },
  { label: 'Terrestre', value: 'TERRESTRE' },
  { label: 'Fluvial', value: 'FLUVIAL' }
]

const estadoOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Activo', value: 'ACTIVO' },
  { label: 'Inactivo', value: 'INACTIVO' }
]

const sortOptions = [
  { label: 'Código (A-Z)', value: 'CODIGO_ASC' },
  { label: 'Nombre (A-Z)', value: 'NOMBRE_ASC' },
  { label: 'País (A-Z)', value: 'PAIS_ASC' }
]

const {
  items,
  loading,
  exportingPdf,
  showForm,
  editingItem,
  viewMode,
  filters,
  hasActiveFilters,
  filteredItems,
  toggleFilter,
  resetFilters,
  loadItems,
  openCreate,
  openEdit,
  closeForm,
  onSaved,
  toggleEstado,
  exportPdf
} = useCatalogoList({
  entityLabel: 'Puerto',
  getId: (p) => p.idPuerto,
  getCode: (p) => p.codigoPuerto,
  loadFn: () => catalogoService.getPuertos(false),
  deleteFn: (id) => catalogoService.deletePuerto(id),
  reactivateFn: (id) => catalogoService.reactivarPuerto(id),
  pdfFn: () => catalogoService.downloadPuertosPdf(),
  initialFilters: { search: '', tipo: '', estado: '', sortBy: 'CODIGO_ASC' },
  filterItem: (p, f) => {
    const tipo = p.tipo || p.tipoPuerto
    if (f.tipo && tipo !== f.tipo) return false
    if (f.estado && p.estado !== f.estado) return false
    if (f.search) {
      const q = f.search.toLowerCase()
      const hay = `${p.codigoPuerto || ''} ${p.nombre || ''} ${p.pais || ''}`.toLowerCase()
      if (!hay.includes(q)) return false
    }
    return true
  },
  sortItems: (list, sortBy) => {
    const copy = [...list]
    if (sortBy === 'NOMBRE_ASC') return copy.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''))
    if (sortBy === 'PAIS_ASC') return copy.sort((a, b) => (a.pais || '').localeCompare(b.pais || ''))
    return copy.sort((a, b) => (a.codigoPuerto || '').localeCompare(b.codigoPuerto || ''))
  },
  messages: {
    created: 'Nuevo puerto registrado en el catálogo',
    updated: 'Puerto actualizado exitosamente'
  }
})

const maritimosCount = computed(() => items.value.filter((p) => (p.tipo || p.tipoPuerto) === 'MARITIMO').length)
const aereosCount = computed(() => items.value.filter((p) => (p.tipo || p.tipoPuerto) === 'AEREO').length)
const activosCount = computed(() => items.value.filter((p) => p.estado === 'ACTIVO').length)

onMounted(loadItems)
</script>

<style scoped>
.catalogo-page {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
}

.is-active-filter {
  outline: 2px solid var(--sga-accent, #8c5a3c);
  outline-offset: 1px;
}
</style>
