<template>
  <SgaListShell v-model:viewMode="viewMode">
    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchModel"
          placeholder="Buscar por código, cliente, RUC o puerto..."
          size="small"
          fluid
        />
      </IconField>

      <Select
        v-model="regimenModel"
        :options="regimenFilterOptions"
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
        label="Aperturar Expediente"
        icon="pi pi-folder-open"
        size="small"
        :disabled="!selectedRow"
        :title="selectedRow ? 'Ir a vista de apertura de expediente' : 'Seleccione una cotización'"
        @click="$emit('open-apertura', selectedRow)"
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

    <AduanasAperturaCanvasGrid
      v-if="viewMode === 'canvas'"
      v-model:selection="selectedRow"
      :cotizaciones="cotizaciones"
      :loading="loading"
      @open-apertura="$emit('open-apertura', $event)"
    />

    <DataTable
      v-else
      v-model:selection="selectedRow"
      :value="cotizaciones"
      :loading="loading"
      selectionMode="single"
      dataKey="idCotizacion"
      responsiveLayout="scroll"
      paginator
      :rows="15"
      :rowsPerPageOptions="[15, 30, 50]"
      currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} cotizaciones"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
      @row-dblclick="onRowDblClick"
    >
      <template #empty>
        <div class="sga-empty-state">
          <i class="pi pi-check-circle sga-empty-icon"></i>
          <p class="sga-empty-title">No hay cotizaciones pendientes de apertura</p>
          <p class="sga-empty-desc">
            Las cotizaciones aprobadas técnicamente con abono verificado en Finanzas aparecerán aquí para formalizar su expediente.
          </p>
        </div>
      </template>

      <Column header="N° COTIZACIÓN">
        <template #body="{ data: c }">
          <div class="sga-cell-stack">
            <span class="sga-code-cell font-mono">{{ c.codigoCotizacion }}</span>
            <span class="sga-cell-sub font-mono">{{ c.fechaEmision || '—' }}</span>
          </div>
        </template>
      </Column>

      <Column header="CLIENTE / RUC">
        <template #body="{ data: c }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ c.clienteRazonSocial || 'Cliente no asignado' }}</span>
            <span class="sga-cell-sub font-mono">RUC/Doc: {{ c.clienteNumeroDocumento || '—' }}</span>
          </div>
        </template>
      </Column>

      <Column header="RÉGIMEN">
        <template #body="{ data: c }">
          <span class="sga-cell-muted">{{ formatRegimenShort(c.regimenAduanero) }}</span>
        </template>
      </Column>

      <Column header="RUTA / PUERTOS">
        <template #body="{ data: c }">
          <span class="sga-cell-muted">
            {{ c.puertoEnvioNombre || 'Origen' }} → {{ c.puertoDestinoNombre || 'Destino' }}
          </span>
        </template>
      </Column>

      <Column header="TOTAL COTIZADO" headerClass="text-right" bodyClass="text-right">
        <template #body="{ data: c }">
          <span class="sga-amount-cell font-mono">${{ formatUsd(c.totalCotizado) }}</span>
        </template>
      </Column>

      <Column header="ABONO FINANZAS" headerClass="text-right" bodyClass="text-right">
        <template #body="{ data: c }">
          <span class="sga-amount-cell font-mono sga-text-success">${{ formatUsd(c.totalAbonado) }}</span>
        </template>
      </Column>
    </DataTable>
  </SgaListShell>
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
import SgaListShell from '@/shared/components/SgaListShell.vue'
import AduanasAperturaCanvasGrid from './AduanasAperturaCanvasGrid.vue'
import { formatUsd, formatRegimenShort } from '../utils/aduanasFormatters'

const props = defineProps({
  cotizaciones: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  searchQuery: { type: String, default: '' },
  filterRegimen: { type: String, default: 'ALL' },
  sortBy: { type: String, default: 'RECENT' },
  hasActiveFilters: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:searchQuery',
  'update:filterRegimen',
  'update:sortBy',
  'reset-filters',
  'refresh',
  'open-apertura'
])

const viewMode = ref('canvas')
const selectedRow = ref(null)

const searchModel = computed({
  get: () => props.searchQuery,
  set: (val) => emit('update:searchQuery', val)
})

const regimenModel = computed({
  get: () => props.filterRegimen,
  set: (val) => emit('update:filterRegimen', val)
})

const sortModel = computed({
  get: () => props.sortBy,
  set: (val) => emit('update:sortBy', val)
})

const regimenFilterOptions = [
  { label: 'Todos los Regímenes', value: 'ALL' },
  { label: 'Importación (REG-10)', value: 'IMPORTACION_DEFINITIVA' },
  { label: 'Exportación (REG-40)', value: 'EXPORTACION_DEFINITIVA' }
]

const sortOptions = [
  { label: 'Más Recientes', value: 'RECENT' },
  { label: 'Más Antiguas', value: 'OLDEST' },
  { label: 'Mayor Monto (USD)', value: 'AMOUNT_DESC' },
  { label: 'Menor Monto (USD)', value: 'AMOUNT_ASC' },
  { label: 'N° Cotización (A-Z)', value: 'CODE_ASC' }
]

watch(
  () => props.cotizaciones,
  (rows) => {
    if (!selectedRow.value) return
    const updated = (rows || []).find((r) => r.idCotizacion === selectedRow.value.idCotizacion)
    selectedRow.value = updated || null
  }
)

const onRowDblClick = (event) => {
  if (event?.data) {
    emit('open-apertura', event.data)
  }
}
</script>
