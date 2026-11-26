<template>
  <div class="catalogo-page fade-in">
    <SedeRegistrationForm
      v-if="showForm"
      :sede-to-edit="editingItem"
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
            <span class="sga-kpi-label">Sedes Registradas</span>
            <i class="pi pi-building sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ items.length }}</span>
            <span class="sga-kpi-pill">Todas</span>
          </div>
        </article>

        <article
          class="sga-kpi-card is-success"
          :class="{ 'is-active-filter': filters.estado === 'OPERATIVO' }"
          @click="toggleFilter('estado', 'OPERATIVO')"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">02</span>
            <span class="sga-kpi-label">Operativas</span>
            <i class="pi pi-check-circle sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ operativasCount }}</span>
            <span class="sga-kpi-pill is-success">En Servicio</span>
          </div>
        </article>

        <article
          class="sga-kpi-card is-warn"
          :class="{ 'is-active-filter': filters.estado === 'MANTENIMIENTO' }"
          @click="toggleFilter('estado', 'MANTENIMIENTO')"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">03</span>
            <span class="sga-kpi-label">Mantenimiento</span>
            <i class="pi pi-wrench sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ mantenimientoCount }}</span>
            <span class="sga-kpi-pill is-warn">En Pausa</span>
          </div>
        </article>

        <article
          class="sga-kpi-card is-info"
          :class="{ 'is-active-filter': filters.tipo === 'SEDE_CENTRAL' }"
          @click="toggleFilter('tipo', 'SEDE_CENTRAL')"
        >
          <div class="sga-kpi-head">
            <span class="sga-kpi-index">04</span>
            <span class="sga-kpi-label">Sedes Centrales</span>
            <i class="pi pi-sitemap sga-kpi-icon"></i>
          </div>
          <div class="sga-kpi-stat">
            <span class="sga-kpi-value sga-mono">{{ centralesCount }}</span>
            <span class="sga-kpi-pill is-info">{{ distritosCount }} distritos</span>
          </div>
        </article>
      </section>

      <SgaListShell v-model:viewMode="viewMode" title="Directorio de Sedes y Agencias">
        <template #header-right>
          <div class="sga-dash-actions">
            <Tag :value="`${filteredItems.length} de ${items.length}`" severity="secondary" class="sga-mono" rounded />
            <Button icon="pi pi-sync" severity="secondary" outlined size="small" :loading="loading" aria-label="Actualizar" @click="loadItems" />
            <Button label="PDF" icon="pi pi-file-pdf" severity="secondary" outlined size="small" :loading="exportingPdf" @click="exportPdf" />
            <Button label="Nueva Sede" icon="pi pi-plus" size="small" @click="openCreate" />
          </div>
        </template>

        <template #filters>
          <IconField class="sga-filter-search">
            <InputIcon class="pi pi-search" />
            <InputText v-model="filters.search" size="small" placeholder="Buscar código, nombre, distrito..." fluid />
          </IconField>
          <Select v-model="filters.tipo" :options="tipoOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select" />
          <Select v-model="filters.estado" :options="estadoOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select" />
          <Select v-model="filters.sortBy" :options="sortOptions" optionLabel="label" optionValue="value" size="small" class="sga-filter-select sga-sort-select" />
          <Button v-if="hasActiveFilters" icon="pi pi-filter-slash" label="Limpiar" severity="secondary" text size="small" @click="resetFilters" />
        </template>

        <div v-if="filteredItems.length === 0 && !loading" class="sga-empty-state">
          <i class="pi pi-building sga-empty-icon"></i>
          <p class="sga-empty-title">No se encontraron sedes con los filtros aplicados</p>
          <Button v-if="hasActiveFilters" label="Restablecer Filtros" severity="secondary" outlined size="small" @click="resetFilters" />
        </div>

        <div v-else-if="viewMode === 'canvas'" class="sga-canvas-wrapper">
          <div class="sga-canvas-grid">
            <article v-for="sede in filteredItems" :key="sede.idSede" class="sga-card-surface sga-canvas-card">
              <div class="sga-card-top">
                <span class="sga-card-code sga-mono">{{ sede.codigoSede }}</span>
                <Tag :value="sede.estado" :severity="getSedeEstadoSeverity(sede.estado)" rounded />
              </div>
              <div class="sga-card-client">
                <h4 class="sga-client-title">{{ sede.nombre }}</h4>
                <p class="sga-client-meta">{{ formatSedeTipo(sede.tipo) }} · {{ sede.distrito }}</p>
              </div>
              <div class="sga-card-meta-grid">
                <div class="sga-meta-item full-span">
                  <span class="sga-meta-label">Dirección</span>
                  <span class="sga-meta-val">{{ sede.direccion }}</span>
                </div>
                <div class="sga-meta-item">
                  <span class="sga-meta-label">Teléfono</span>
                  <span class="sga-meta-val sga-mono">{{ sede.telefono || 'Sin registro' }}</span>
                </div>
              </div>
              <div class="sga-card-footer">
                <span class="sga-footer-left sga-text-muted">{{ formatSedeTipo(sede.tipo) }}</span>
                <CatalogoRowActions
                  :is-active="sede.estado === 'OPERATIVO'"
                  :label="sede.codigoSede"
                  @edit="openEdit(sede)"
                  @toggle="toggleEstado(sede)"
                />
              </div>
            </article>
          </div>
        </div>

        <div v-else class="sga-table-scroll">
          <DataTable :value="filteredItems" :loading="loading" paginator :rows="12" dataKey="idSede" size="small" stripedRows>
            <Column field="codigoSede" header="CÓDIGO" sortable>
              <template #body="{ data }">
                <span class="sga-code-cell sga-mono">{{ data.codigoSede }}</span>
              </template>
            </Column>
            <Column field="nombre" header="SEDE / DIRECCIÓN" sortable>
              <template #body="{ data }">
                <div class="sga-cell-stack">
                  <span class="sga-cell-primary">{{ data.nombre }}</span>
                  <span class="sga-cell-sub">{{ data.direccion }}</span>
                </div>
              </template>
            </Column>
            <Column field="distrito" header="DISTRITO" sortable />
            <Column field="tipo" header="TIPO" sortable>
              <template #body="{ data }">
                <Tag :value="formatSedeTipo(data.tipo)" :icon="getSedeTipoIcon(data.tipo)" severity="secondary" rounded />
              </template>
            </Column>
            <Column field="estado" header="ESTADO" sortable>
              <template #body="{ data }">
                <Tag :value="data.estado" :severity="getSedeEstadoSeverity(data.estado)" rounded />
              </template>
            </Column>
            <Column header="ACCIONES" style="width: 110px">
              <template #body="{ data }">
                <CatalogoRowActions
                  :is-active="data.estado === 'OPERATIVO'"
                  :label="data.codigoSede"
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
import SedeRegistrationForm from '../components/SedeRegistrationForm.vue'
import CatalogoRowActions from '@/features/admin/catalogos/components/CatalogoRowActions.vue'
import sedeService from '../services/sedeService'
import { useCatalogoList } from '@/features/admin/catalogos/composables/useCatalogoList'
import {
  formatSedeTipo,
  getSedeTipoIcon,
  getSedeEstadoSeverity
} from '@/features/gerencia/utils/gerenciaFormatters'

const tipoOptions = [
  { label: 'Todos los tipos', value: '' },
  { label: 'Sede Central', value: 'SEDE_CENTRAL' },
  { label: 'Agencia Operativa', value: 'AGENCIA' },
  { label: 'Terminal / Almacén', value: 'TERMINAL' }
]

const estadoOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Operativo', value: 'OPERATIVO' },
  { label: 'Mantenimiento', value: 'MANTENIMIENTO' },
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
  entityLabel: 'Sede',
  getId: (s) => s.idSede,
  getCode: (s) => s.codigoSede,
  activeStatus: 'OPERATIVO',
  loadFn: () => sedeService.getSedes(false),
  deleteFn: (id) => sedeService.deleteSede(id),
  reactivateFn: (id) => sedeService.reactivateSede(id),
  pdfFn: () => sedeService.downloadPdf(),
  initialFilters: { search: '', tipo: '', estado: '', sortBy: 'CODIGO_ASC' },
  filterItem: (s, f) => {
    if (f.tipo && s.tipo !== f.tipo) return false
    if (f.estado && s.estado !== f.estado) return false
    if (f.search) {
      const q = f.search.toLowerCase()
      const hay = `${s.codigoSede || ''} ${s.nombre || ''} ${s.distrito || ''} ${s.direccion || ''}`.toLowerCase()
      if (!hay.includes(q)) return false
    }
    return true
  },
  sortItems: (list, sortBy) => {
    const copy = [...list]
    if (sortBy === 'NOMBRE_ASC') return copy.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''))
    return copy.sort((a, b) => (a.codigoSede || '').localeCompare(b.codigoSede || ''))
  },
  messages: {
    created: 'Nueva sede territorial registrada exitosamente',
    updated: 'Sede territorial actualizada exitosamente'
  }
})

const operativasCount = computed(() => items.value.filter((s) => s.estado === 'OPERATIVO').length)
const mantenimientoCount = computed(() => items.value.filter((s) => s.estado !== 'OPERATIVO').length)
const centralesCount = computed(() => items.value.filter((s) => s.tipo === 'SEDE_CENTRAL').length)
const distritosCount = computed(() => new Set(items.value.map((s) => s.distrito).filter(Boolean)).size)

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
