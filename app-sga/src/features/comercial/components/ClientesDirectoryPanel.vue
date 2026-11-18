<template>
  <ComercialListShell v-model:viewMode="viewMode">
    <template #header-right>
      <Button
        type="button"
        label="Nuevo Cliente"
        icon="pi pi-plus"
        size="small"
        @click="$emit('create')"
      />
    </template>

    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchModel"
          placeholder="Buscar por RUC/DNI, razón social o contacto..."
          size="small"
          fluid
        />
      </IconField>

      <Select
        v-model="tipoModel"
        :options="tipoFilterOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
      />

      <Select
        v-model="estadoModel"
        :options="estadoFilterOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
      />

      <Select
        v-model="sortModel"
        :options="sortOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select sga-sort-select"
      />

      <Button
        v-if="hasActiveFilters"
        type="button"
        icon="pi pi-filter-slash"
        severity="secondary"
        outlined
        size="small"
        title="Limpiar filtros"
        @click="$emit('reset-filters')"
      />
    </template>

    <template #actions>
      <Button
        type="button"
        label="Editar"
        icon="pi pi-pencil"
        severity="secondary"
        outlined
        size="small"
        :disabled="!selectedRow"
        :title="selectedRow ? 'Editar cliente' : 'Seleccione un cliente'"
        @click="$emit('edit', selectedRow)"
      />

      <Button
        type="button"
        icon="pi pi-trash"
        severity="danger"
        outlined
        size="small"
        :disabled="!selectedRow"
        :title="selectedRow ? 'Eliminar cliente' : 'Seleccione un cliente'"
        @click="$emit('delete', selectedRow)"
      />

      <Button
        type="button"
        label="Actualizar"
        icon="pi pi-sync"
        severity="secondary"
        outlined
        size="small"
        :loading="loading"
        @click="$emit('refresh')"
      />
    </template>

    <DataTable
      v-if="viewMode === 'table'"
      v-model:selection="selectedRow"
      :value="clientes"
      :loading="loading"
      selectionMode="single"
      dataKey="idCliente"
      responsiveLayout="scroll"
      paginator
      :rows="15"
      :rowsPerPageOptions="[15, 30, 50]"
      currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} clientes"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
      @row-dblclick="onRowDblClick"
    >
      <template #empty>
        <div class="sga-empty-state">
          <i class="pi pi-building sga-empty-icon"></i>
          <p class="sga-empty-title">No hay clientes encontrados</p>
          <p class="sga-empty-desc">Registre su primer cliente o prospecto para empezar a cotizar.</p>
        </div>
      </template>

      <Column header="DOCUMENTO">
        <template #body="{ data: cl }">
          <span class="sga-code-cell font-mono">{{ cl.tipoDocumento }}: {{ cl.numeroDocumento }}</span>
        </template>
      </Column>

      <Column header="RAZÓN SOCIAL / CONTACTO">
        <template #body="{ data: cl }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ cl.razonSocial }}</span>
            <div class="sga-cell-meta">
              <span v-if="cl.contactoRepresentante" class="sga-cell-sub">Rep: {{ cl.contactoRepresentante }}</span>
              <Tag
                :value="clienteTieneUsuarioPortal(cl) ? 'Portal B2B' : 'Presencial'"
                :severity="clienteTieneUsuarioPortal(cl) ? 'info' : 'secondary'"
                class="sga-mini-tag"
              />
            </div>
          </div>
        </template>
      </Column>

      <Column header="CORREO INSTITUCIONAL">
        <template #body="{ data: cl }">
          <span class="sga-cell-muted">{{ cl.correoContacto }}</span>
        </template>
      </Column>

      <Column header="TELÉFONO">
        <template #body="{ data: cl }">
          <span class="sga-cell-muted font-mono">{{ cl.telefonoContacto || '—' }}</span>
        </template>
      </Column>

      <Column header="DIRECCIÓN FISCAL">
        <template #body="{ data: cl }">
          <span class="sga-cell-muted">{{ cl.direccionFiscal || '—' }}</span>
        </template>
      </Column>

      <Column header="CLASIFICACIÓN">
        <template #body="{ data: cl }">
          <Tag
            :value="cl.esProspecto ? 'Prospecto' : 'Cliente Efectivo'"
            :severity="cl.esProspecto ? 'warn' : 'success'"
            rounded
          />
        </template>
      </Column>
    </DataTable>

    <ClientesCanvasGrid
      v-else
      v-model:selection="selectedRow"
      :clientes="clientes"
      :loading="loading"
      :cliente-tiene-usuario-portal="clienteTieneUsuarioPortal"
      @edit-item="(cl) => $emit('edit', cl)"
    />
  </ComercialListShell>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import ComercialListShell from './shared/ComercialListShell.vue'
import ClientesCanvasGrid from './ClientesCanvasGrid.vue'

const props = defineProps({
  clientes: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  searchCliente: { type: String, default: '' },
  filterTipoCliente: { type: String, default: 'ALL' },
  filterEstadoCliente: { type: String, default: 'ALL' },
  sortByCliente: { type: String, default: 'nombre-asc' },
  hasActiveFilters: { type: Boolean, default: false },
  clienteTieneUsuarioPortal: { type: Function, required: true }
})

const emit = defineEmits([
  'update:searchCliente',
  'update:filterTipoCliente',
  'update:filterEstadoCliente',
  'update:sortByCliente',
  'reset-filters',
  'refresh',
  'create',
  'edit',
  'delete'
])

const selectedRow = ref(null)
const viewMode = ref('canvas')

watch(
  () => props.clientes,
  (list) => {
    if (!selectedRow.value) return
    const updated = list.find((item) => item.idCliente === selectedRow.value.idCliente)
    selectedRow.value = updated || null
  }
)

const searchModel = computed({
  get: () => props.searchCliente,
  set: (v) => emit('update:searchCliente', v)
})

const tipoModel = computed({
  get: () => props.filterTipoCliente,
  set: (v) => emit('update:filterTipoCliente', v)
})

const estadoModel = computed({
  get: () => props.filterEstadoCliente,
  set: (v) => emit('update:filterEstadoCliente', v)
})

const sortModel = computed({
  get: () => props.sortByCliente,
  set: (v) => emit('update:sortByCliente', v)
})

const tipoFilterOptions = [
  { label: 'Todos (Clientes y Prospectos)', value: 'ALL' },
  { label: 'Solo Prospectos', value: 'PROSPECTO' },
  { label: 'Clientes Efectivos', value: 'CLIENTE' }
]

const estadoFilterOptions = [
  { label: 'Todos los Estados', value: 'ALL' },
  { label: 'Solo Activos', value: 'ACTIVO' },
  { label: 'Solo Inactivos', value: 'INACTIVO' }
]

const sortOptions = [
  { label: 'Ordenar por: Razón Social (A - Z)', value: 'nombre-asc' },
  { label: 'Ordenar por: Razón Social (Z - A)', value: 'nombre-desc' },
  { label: 'Ordenar por: Documento (A - Z)', value: 'doc-asc' },
  { label: 'Ordenar por: Clasificación (Prospectos primero)', value: 'tipo-asc' }
]

const onRowDblClick = ({ data }) => {
  if (data) emit('edit', data)
}
</script>
