<template>
  <SgaListShell viewMode="canvas" :showViewToggle="false" :title="panelTitle">
    <template #header-right>
      <div class="sga-dash-meta">
        <Tag
          :value="mode === 'readonly' ? 'Solo Lectura · Canvas' : 'Operación de Hitos'"
          :icon="mode === 'readonly' ? 'pi pi-eye' : 'pi pi-sliders-h'"
          :severity="mode === 'readonly' ? 'secondary' : 'info'"
          rounded
        />
        <Tag
          :value="`${expedientes.length} expedientes`"
          severity="secondary"
          class="font-mono"
          rounded
        />
      </div>
    </template>

    <template #sub-bar>
      <div class="sga-queue-buttons" role="tablist" aria-label="Fases operativas del Agente Aduanero">
        <Button
          v-for="q in phaseQueues"
          :key="q.value"
          type="button"
          role="tab"
          :aria-selected="phaseFilter === q.value"
          :label="q.label"
          :badge="String(q.count)"
          :badgeSeverity="q.badgeSeverity"
          :severity="phaseFilter === q.value ? 'primary' : 'secondary'"
          :outlined="phaseFilter !== q.value"
          size="small"
          @click="$emit('update:phaseFilter', q.value)"
        />
      </div>
    </template>

    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          :modelValue="searchQuery"
          size="small"
          placeholder="Buscar expediente, DAM, cliente, nave..."
          fluid
          @update:modelValue="$emit('update:searchQuery', $event)"
        />
      </IconField>

      <Select
        v-if="esSupervisorOAdmin"
        :modelValue="selectedAgenteId"
        :options="agenteOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
        @update:modelValue="$emit('update:selectedAgenteId', $event)"
      />

      <Select
        :modelValue="filterRegimen"
        :options="regimenOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
        @update:modelValue="$emit('update:filterRegimen', $event)"
      />

      <Select
        :modelValue="filterCanal"
        :options="canalOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
        @update:modelValue="$emit('update:filterCanal', $event)"
      />

      <Select
        :modelValue="sortBy"
        :options="sortOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select sga-sort-select"
        @update:modelValue="$emit('update:sortBy', $event)"
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
        label="Actualizar"
        icon="pi pi-sync"
        severity="secondary"
        outlined
        size="small"
        :loading="loading"
        @click="$emit('refresh')"
      />
    </template>

    <AgenteExpedientesCanvasGrid
      :expedientes="expedientes"
      :loading="loading"
      :mode="mode"
      @detalle="$emit('detalle', $event)"
      @documentos="$emit('documentos', $event)"
    />
  </SgaListShell>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import AgenteExpedientesCanvasGrid from './AgenteExpedientesCanvasGrid.vue'

const props = defineProps({
  expedientes: { type: Array, default: () => [] },
  kpiCounts: {
    type: Object,
    default: () => ({ todos: 0, transito: 0, inspeccion: 0, levante: 0, cerrados: 0 })
  },
  mode: {
    type: String,
    default: 'readonly' // 'readonly' | 'manage'
  },
  phaseFilter: { type: String, default: 'todos' },
  searchQuery: { type: String, default: '' },
  filterRegimen: { type: String, default: '' },
  filterCanal: { type: String, default: '' },
  sortBy: { type: String, default: 'RECENT' },
  selectedAgenteId: { type: [Number, String, null], default: null },
  agentesList: { type: Array, default: () => [] },
  esSupervisorOAdmin: { type: Boolean, default: false },
  hasActiveFilters: { type: Boolean, default: false },
  loading: { type: Boolean, default: false }
})

defineEmits([
  'update:phaseFilter',
  'update:searchQuery',
  'update:filterRegimen',
  'update:filterCanal',
  'update:sortBy',
  'update:selectedAgenteId',
  'reset-filters',
  'refresh',
  'detalle',
  'documentos'
])

const panelTitle = computed(() =>
  props.mode === 'manage' ? 'Seleccione un Expediente a Gestionar' : 'Vista General de Expedientes (Canvas)'
)

const phaseQueues = computed(() => {
  const base = [
    { value: 'todos', label: 'Todos', count: props.kpiCounts.todos || 0, badgeSeverity: 'contrast' },
    { value: 'transito', label: 'Tránsito / Arribo', count: props.kpiCounts.transito || 0, badgeSeverity: 'info' },
    { value: 'inspeccion', label: 'Aforo SUNAT', count: props.kpiCounts.inspeccion || 0, badgeSeverity: 'warn' },
    { value: 'levante', label: 'Levante & Despacho', count: props.kpiCounts.levante || 0, badgeSeverity: 'success' }
  ]
  if (props.mode === 'readonly') {
    base.push({ value: 'cerrados', label: 'Cerrados', count: props.kpiCounts.cerrados || 0, badgeSeverity: 'secondary' })
  }
  return base
})

const regimenOptions = [
  { label: 'Todos los Regímenes', value: '' },
  { label: 'Importación (REG-10)', value: 'IMPORTACION' },
  { label: 'Exportación (REG-40)', value: 'EXPORTACION' }
]

const canalOptions = [
  { label: 'Todos los Canales', value: '' },
  { label: 'Canal Pendiente', value: 'PENDIENTE' },
  { label: 'Canal Verde', value: 'VERDE' },
  { label: 'Canal Naranja', value: 'NARANJA' },
  { label: 'Canal Rojo', value: 'ROJO' }
]

const sortOptions = [
  { label: 'Más Recientes', value: 'RECENT' },
  { label: 'Más Antiguos', value: 'OLDEST' },
  { label: 'N° Expediente (A-Z)', value: 'CODE_ASC' }
]

const agenteOptions = computed(() => [
  { label: 'Todos los Agentes', value: null },
  ...(props.agentesList || []).map((ag) => ({
    label: `${ag.nombre || ag.nombres || ''} ${ag.apellidos || ''}`.trim(),
    value: ag.idUsuario
  }))
])
</script>
