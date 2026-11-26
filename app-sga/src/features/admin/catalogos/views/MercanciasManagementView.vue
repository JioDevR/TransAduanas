<template>
  <div class="catalogo-page fade-in">
    <section class="sga-kpis-grid">
      <article
        class="sga-kpi-card is-primary"
        :class="{ 'is-active-filter': !hasActiveFilters }"
        @click="resetFilters"
      >
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">Partidas Totales</span>
          <i class="pi pi-tags sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value sga-mono">{{ items.length }}</span>
          <span class="sga-kpi-pill">{{ uniqueCategories.length }} cat.</span>
        </div>
      </article>

      <article
        class="sga-kpi-card is-warn"
        :class="{ 'is-active-filter': filters.permiso === 'SI' }"
        @click="toggleFilter('permiso', 'SI')"
      >
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">Restringidas VUCE</span>
          <i class="pi pi-shield sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value sga-mono">{{ vuceCount }}</span>
          <span class="sga-kpi-pill is-warn">Permiso Req.</span>
        </div>
      </article>

      <article
        class="sga-kpi-card is-success"
        :class="{ 'is-active-filter': filters.permiso === 'NO' }"
        @click="toggleFilter('permiso', 'NO')"
      >
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">Libre Comercio</span>
          <i class="pi pi-check-circle sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value sga-mono">{{ libresCount }}</span>
          <span class="sga-kpi-pill is-success">Sin VUCE</span>
        </div>
      </article>

      <article
        class="sga-kpi-card is-info"
        :class="{ 'is-active-filter': filters.estado === 'ACTIVO' }"
        @click="toggleFilter('estado', 'ACTIVO')"
      >
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Habilitadas</span>
          <i class="pi pi-check-square sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value sga-mono">{{ activasCount }}</span>
          <span class="sga-kpi-pill is-info">Activas</span>
        </div>
      </article>
    </section>

    <SgaListShell v-model:viewMode="viewMode" title="Mercancías y Subpartidas Arancelarias">
      <template #header-right>
        <div class="sga-dash-actions">
          <Tag :value="`${filteredItems.length} de ${items.length}`" severity="secondary" class="sga-mono" rounded />
          <Button icon="pi pi-sync" severity="secondary" outlined size="small" :loading="loading" aria-label="Actualizar" @click="loadItems" />
          <Button label="PDF" icon="pi pi-file-pdf" severity="secondary" outlined size="small" :loading="exportingPdf" @click="exportPdf" />
          <Button label="Nueva Partida" icon="pi pi-plus" size="small" @click="openCreate" />
        </div>
      </template>

      <template #filters>
        <IconField class="sga-filter-search">
          <InputIcon class="pi pi-search" />
          <InputText v-model="filters.search" size="small" placeholder="Buscar partida, mercancía o entidad VUCE..." fluid />
        </IconField>
        <Select v-model="filters.categoria" :options="categoriaFilterOptions" optionLabel="label" optionValue="value" filter size="small" class="sga-filter-select" />
        <Select v-model="filters.permiso" :options="permisoFilterOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select" />
        <Select v-model="filters.estado" :options="estadoOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select" />
        <Select v-model="filters.sortBy" :options="sortOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select sga-sort-select" />
        <Button v-if="hasActiveFilters" icon="pi pi-filter-slash" label="Limpiar" severity="secondary" text size="small" @click="resetFilters" />
      </template>

      <div v-if="filteredItems.length === 0 && !loading" class="sga-empty-state">
        <i class="pi pi-tags sga-empty-icon"></i>
        <p class="sga-empty-title">No se encontraron partidas arancelarias</p>
        <Button v-if="hasActiveFilters" label="Restablecer Filtros" severity="secondary" outlined size="small" @click="resetFilters" />
      </div>

      <div v-else-if="viewMode === 'canvas'" class="sga-canvas-wrapper">
        <div class="sga-canvas-grid">
          <article v-for="merc in filteredItems" :key="merc.idMercancia" class="sga-card-surface sga-canvas-card">
            <div class="sga-card-top">
              <span class="sga-card-code sga-mono">{{ merc.codigo }}</span>
              <Tag :value="merc.estado" :severity="getCatalogoEstadoSeverity(merc.estado)" rounded />
            </div>
            <div class="sga-card-client">
              <h4 class="sga-client-title">{{ merc.nombre }}</h4>
              <p class="sga-client-meta">{{ merc.categoria }}</p>
            </div>
            <div class="sga-card-meta-grid">
              <div class="sga-meta-item">
                <span class="sga-meta-label">Control VUCE</span>
                <Tag
                  :value="merc.requierePermiso ? (merc.entidadReguladora || 'VUCE') : 'Libre'"
                  :severity="getPermisoVuceSeverity(merc.requierePermiso)"
                  rounded
                />
              </div>
              <div class="sga-meta-item">
                <span class="sga-meta-label">Arancel Base</span>
                <span class="sga-meta-val sga-mono">{{ formatArancelPct(merc.arancelBasePct) }}</span>
              </div>
            </div>
            <div class="sga-card-footer">
              <span class="sga-footer-left sga-text-muted">{{ merc.categoria }}</span>
              <CatalogoRowActions
                :is-active="merc.estado === 'ACTIVO'"
                :label="merc.codigo"
                @edit="openEdit(merc)"
                @toggle="toggleEstado(merc)"
              />
            </div>
          </article>
        </div>
      </div>

      <div v-else class="sga-table-scroll">
        <DataTable :value="filteredItems" :loading="loading" paginator :rows="12" dataKey="idMercancia" size="small" stripedRows>
          <Column field="codigo" header="PARTIDA" sortable>
            <template #body="{ data }">
              <span class="sga-code-cell sga-mono">{{ data.codigo }}</span>
            </template>
          </Column>
          <Column field="nombre" header="MERCANCÍA" sortable>
            <template #body="{ data }">
              <div class="sga-cell-stack">
                <span class="sga-cell-primary">{{ data.nombre }}</span>
                <span class="sga-cell-sub">{{ data.categoria }}</span>
              </div>
            </template>
          </Column>
          <Column field="requierePermiso" header="CONTROL VUCE" sortable>
            <template #body="{ data }">
              <Tag
                :value="data.requierePermiso ? `VUCE · ${data.entidadReguladora || 'REGULADA'}` : 'Libre Comercio'"
                :severity="getPermisoVuceSeverity(data.requierePermiso)"
                rounded
              />
            </template>
          </Column>
          <Column field="arancelBasePct" header="ARANCEL" sortable>
            <template #body="{ data }">
              <span class="sga-mono">{{ formatArancelPct(data.arancelBasePct) }}</span>
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
                :label="data.codigo"
                @edit="openEdit(data)"
                @toggle="toggleEstado(data)"
              />
            </template>
          </Column>
        </DataTable>
      </div>
    </SgaListShell>

    <MercanciaFormModal
      :visible="showForm"
      :mercancia-to-edit="editingItem"
      :existing-categories="uniqueCategories"
      @close="closeForm"
      @saved="onSaved"
    />
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
import MercanciaFormModal from '../components/MercanciaFormModal.vue'
import CatalogoRowActions from '../components/CatalogoRowActions.vue'
import catalogoService from '../services/catalogoService'
import { useCatalogoList } from '../composables/useCatalogoList'
import {
  mergeNormalizedCategories,
  getPermisoVuceSeverity,
  getCatalogoEstadoSeverity,
  formatArancelPct
} from '@/features/gerencia/utils/gerenciaFormatters'

const permisoFilterOptions = [
  { label: 'VUCE: Todas', value: '' },
  { label: 'Requiere Permiso VUCE', value: 'SI' },
  { label: 'Libre Comercialización', value: 'NO' }
]

const estadoOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Activo', value: 'ACTIVO' },
  { label: 'Inactivo', value: 'INACTIVO' }
]

const sortOptions = [
  { label: 'Partida (A-Z)', value: 'CODIGO_ASC' },
  { label: 'Nombre (A-Z)', value: 'NOMBRE_ASC' },
  { label: 'Mayor Arancel', value: 'ARANCEL_DESC' }
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
  entityLabel: 'Mercancía',
  getId: (m) => m.idMercancia,
  getCode: (m) => m.codigo,
  loadFn: () => catalogoService.getMercancias(false),
  deleteFn: (id) => catalogoService.deleteMercancia(id),
  reactivateFn: (id) => catalogoService.reactivarMercancia(id),
  pdfFn: () => catalogoService.downloadMercanciasPdf(),
  initialFilters: { search: '', categoria: '', permiso: '', estado: '', sortBy: 'CODIGO_ASC' },
  filterItem: (m, f) => {
    if (f.estado && m.estado !== f.estado) return false
    if (f.categoria && String(m.categoria || '').trim().toUpperCase() !== f.categoria) return false
    if (f.permiso === 'SI' && !m.requierePermiso) return false
    if (f.permiso === 'NO' && m.requierePermiso) return false
    if (f.search) {
      const q = f.search.toLowerCase()
      const hay = `${m.codigo || ''} ${m.nombre || ''} ${m.categoria || ''} ${m.entidadReguladora || ''}`.toLowerCase()
      if (!hay.includes(q)) return false
    }
    return true
  },
  sortItems: (list, sortBy) => {
    const copy = [...list]
    if (sortBy === 'NOMBRE_ASC') return copy.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''))
    if (sortBy === 'ARANCEL_DESC') return copy.sort((a, b) => Number(b.arancelBasePct || 0) - Number(a.arancelBasePct || 0))
    return copy.sort((a, b) => (a.codigo || '').localeCompare(b.codigo || ''))
  }
})

const uniqueCategories = computed(() =>
  mergeNormalizedCategories(items.value.map((m) => m.categoria))
)

const categoriaFilterOptions = computed(() => [
  { label: 'Todas las categorías', value: '' },
  ...uniqueCategories.value.map((c) => ({ label: c, value: c }))
])

const vuceCount = computed(() => items.value.filter((m) => Boolean(m.requierePermiso)).length)
const libresCount = computed(() => items.value.filter((m) => !m.requierePermiso).length)
const activasCount = computed(() => items.value.filter((m) => m.estado === 'ACTIVO').length)

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
