<template>
  <div class="catalogo-page fade-in">
    <TipoCargaRegistrationForm
      v-if="showForm"
      :tipo-to-edit="editingItem"
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
            <span class="sga-kpi-label">Tipos Registrados</span>
            <i class="pi pi-box sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ items.length }}</span>
            <span class="sga-kpi-pill">Catálogo</span>
          </div>
        </article>

        <article
          class="sga-kpi-card is-success"
          :class="{ 'is-active-filter': filters.estado === 'ACTIVO' }"
          @click="toggleFilter('estado', 'ACTIVO')"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">02</span>
            <span class="sga-kpi-label">Activos</span>
            <i class="pi pi-check-circle sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ activosCount }}</span>
            <span class="sga-kpi-pill is-success">Vigentes</span>
          </div>
        </article>

        <article
          class="sga-kpi-card is-info"
          :class="{ 'is-active-filter': filters.regimen === 'IMP' }"
          @click="toggleFilter('regimen', 'IMP')"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">03</span>
            <span class="sga-kpi-label">Importación (IMP)</span>
            <i class="pi pi-arrow-down-left sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ importacionCount }}</span>
            <span class="sga-kpi-pill is-info">Ingreso</span>
          </div>
        </article>

        <article
          class="sga-kpi-card is-warn"
          :class="{ 'is-active-filter': filters.regimen === 'EXP' }"
          @click="toggleFilter('regimen', 'EXP')"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">04</span>
            <span class="sga-kpi-label">Exportación (EXP)</span>
            <i class="pi pi-arrow-up-right sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ exportacionCount }}</span>
            <span class="sga-kpi-pill is-warn">Salida</span>
          </div>
        </article>
      </section>

      <SgaListShell v-model:viewMode="viewMode" title="Catálogo de Tipos de Carga">
        <template #header-right>
          <div class="sga-dash-actions">
            <Tag :value="`${filteredItems.length} de ${items.length}`" severity="secondary" class="sga-mono" rounded />
            <Button icon="pi pi-sync" severity="secondary" outlined size="small" :loading="loading" aria-label="Actualizar" @click="loadItems" />
            <Button label="PDF" icon="pi pi-file-pdf" severity="secondary" outlined size="small" :loading="exportingPdf" @click="exportPdf" />
            <Button label="Nuevo Tipo" icon="pi pi-plus" size="small" @click="openCreate" />
          </div>
        </template>

        <template #filters>
          <IconField class="sga-filter-search">
            <InputIcon class="pi pi-search" />
            <InputText v-model="filters.search" size="small" placeholder="Buscar por código, nombre o descripción..." fluid />
          </IconField>
          <Select v-model="filters.regimen" :options="regimenOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select" />
          <Select v-model="filters.estado" :options="estadoOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select" />
          <Select v-model="filters.sortBy" :options="sortOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select sga-sort-select" />
          <Button v-if="hasActiveFilters" icon="pi pi-filter-slash" label="Limpiar" severity="secondary" text size="small" @click="resetFilters" />
        </template>

        <div v-if="filteredItems.length === 0 && !loading" class="sga-empty-state">
          <i class="pi pi-box sga-empty-icon"></i>
          <p class="sga-empty-title">No se encontraron tipos de carga</p>
          <Button v-if="hasActiveFilters" label="Restablecer Filtros" severity="secondary" outlined size="small" @click="resetFilters" />
        </div>

        <div v-else-if="viewMode === 'canvas'" class="sga-canvas-wrapper">
          <div class="sga-canvas-grid">
            <article v-for="tipo in filteredItems" :key="tipo.idTipoCarga" class="sga-card-surface sga-canvas-card">
              <div class="sga-card-top">
                <span class="sga-card-code sga-mono">{{ tipo.codigo }}</span>
                <Tag :value="tipo.estado" :severity="getCatalogoEstadoSeverity(tipo.estado)" rounded />
              </div>
              <div class="sga-card-client">
                <h4 class="sga-client-title">{{ tipo.nombre }}</h4>
                <p class="sga-client-meta">{{ tipo.descripcion || 'Sin descripción registrada' }}</p>
              </div>
              <div class="sga-card-footer">
                <Tag :value="getTipoCargaRegimenLabel(tipo.codigo)" :severity="getTipoCargaRegimenSeverity(tipo.codigo)" rounded />
                <CatalogoRowActions
                  :is-active="tipo.estado === 'ACTIVO'"
                  :label="tipo.codigo"
                  @edit="openEdit(tipo)"
                  @toggle="toggleEstado(tipo)"
                />
              </div>
            </article>
          </div>
        </div>

        <div v-else class="sga-table-scroll">
          <DataTable :value="filteredItems" :loading="loading" paginator :rows="12" dataKey="idTipoCarga" size="small" stripedRows>
            <Column field="codigo" header="CÓDIGO" sortable>
              <template #body="{ data }">
                <span class="sga-code-cell sga-mono">{{ data.codigo }}</span>
              </template>
            </Column>
            <Column field="nombre" header="TIPO DE CARGA" sortable />
            <Column header="RÉGIMEN" style="width: 170px">
              <template #body="{ data }">
                <Tag :value="getTipoCargaRegimenLabel(data.codigo)" :severity="getTipoCargaRegimenSeverity(data.codigo)" rounded />
              </template>
            </Column>
            <Column field="descripcion" header="DESCRIPCIÓN">
              <template #body="{ data }">
                <span class="sga-cell-sub">{{ data.descripcion || 'Sin descripción' }}</span>
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
import TipoCargaRegistrationForm from '../components/TipoCargaRegistrationForm.vue'
import CatalogoRowActions from '../components/CatalogoRowActions.vue'
import catalogoService from '../services/catalogoService'
import { useCatalogoList } from '../composables/useCatalogoList'
import {
  classifyTipoCargaRegimen,
  getTipoCargaRegimenLabel,
  getTipoCargaRegimenSeverity,
  getCatalogoEstadoSeverity
} from '@/features/gerencia/utils/gerenciaFormatters'

const regimenOptions = [
  { label: 'Todos los regímenes', value: '' },
  { label: 'Importación (IMP)', value: 'IMP' },
  { label: 'Exportación (EXP)', value: 'EXP' },
  { label: 'Uso General (GEN)', value: 'GEN' },
  { label: 'Especial / Regulada', value: 'ESP' }
]

const estadoOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Activo', value: 'ACTIVO' },
  { label: 'Inactivo', value: 'INACTIVO' }
]

const sortOptions = [
  { label: 'Código (A-Z)', value: 'CODIGO_ASC' },
  { label: 'Nombre (A-Z)', value: 'NOMBRE_ASC' }
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
  entityLabel: 'Tipo de carga',
  getId: (t) => t.idTipoCarga,
  getCode: (t) => t.codigo,
  loadFn: () => catalogoService.getTiposCarga(false),
  deleteFn: (id) => catalogoService.deleteTipoCarga(id),
  reactivateFn: (id) => catalogoService.reactivarTipoCarga(id),
  pdfFn: () => catalogoService.downloadTiposCargaPdf(),
  initialFilters: { search: '', regimen: '', estado: '', sortBy: 'CODIGO_ASC' },
  filterItem: (t, f) => {
    if (f.estado && t.estado !== f.estado) return false
    if (f.regimen && classifyTipoCargaRegimen(t.codigo) !== f.regimen) return false
    if (f.search) {
      const q = f.search.toLowerCase()
      const hay = `${t.codigo || ''} ${t.nombre || ''} ${t.descripcion || ''}`.toLowerCase()
      if (!hay.includes(q)) return false
    }
    return true
  },
  sortItems: (list, sortBy) => {
    const copy = [...list]
    if (sortBy === 'NOMBRE_ASC') return copy.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''))
    return copy.sort((a, b) => (a.codigo || '').localeCompare(b.codigo || ''))
  }
})

const activosCount = computed(() => items.value.filter((t) => t.estado === 'ACTIVO').length)
const importacionCount = computed(() => items.value.filter((t) => classifyTipoCargaRegimen(t.codigo) === 'IMP').length)
const exportacionCount = computed(() => items.value.filter((t) => classifyTipoCargaRegimen(t.codigo) === 'EXP').length)

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
