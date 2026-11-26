<template>
  <SgaListShell
    v-model:viewMode="viewMode"
    title="Monitoreo & Bandeja de Despachos Terrestres"
  >
    <template #header-right>
      <div class="sga-toolbar-actions">
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
        <Button
          type="button"
          label="Programar Despacho"
          icon="pi pi-plus"
          size="small"
          @click="$emit('programar')"
        />
      </div>
    </template>

    <template #sub-bar>
      <div class="sga-queue-buttons" role="tablist" aria-label="Estados de despacho">
        <Button
          v-for="tab in queueTabs"
          :key="tab.value"
          type="button"
          role="tab"
          :aria-selected="filterEstado === tab.value"
          :label="tab.label"
          :badge="String(tab.count)"
          :badgeSeverity="tab.badgeSeverity"
          :severity="filterEstado === tab.value ? 'primary' : 'secondary'"
          :outlined="filterEstado !== tab.value"
          size="small"
          @click="filterEstado = tab.value"
        />
      </div>
    </template>

    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchQuery"
          placeholder="Buscar por tracking, expediente, cliente, placa o distrito..."
          size="small"
          fluid
        />
      </IconField>

      <Select
        v-model="filterTipo"
        :options="tipoFilterOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
      />

      <Button
        v-if="hasActiveFilters"
        type="button"
        icon="pi pi-filter-slash"
        severity="secondary"
        outlined
        size="small"
        title="Limpiar filtros"
        @click="resetFilters"
      />
    </template>

    <template #actions>
      <Button
        type="button"
        label="Hoja de Ruta"
        icon="pi pi-eye"
        severity="secondary"
        outlined
        size="small"
        :disabled="!selectedRow"
        @click="$emit('detalle', selectedRow)"
      />
      <Button
        v-if="selectedRow?.estadoTransporte === 'PENDIENTE_ASIGNACION'"
        type="button"
        label="Aceptar Solicitud"
        icon="pi pi-thumbs-up"
        severity="info"
        size="small"
        @click="$emit('aceptar', selectedRow)"
      />
      <Button
        v-if="canAsignar"
        type="button"
        :label="selectedRow?.estadoTransporte === 'PROGRAMADO' ? 'Reasignar' : 'Asignar'"
        icon="pi pi-user-plus"
        severity="warn"
        size="small"
        @click="$emit('asignar', selectedRow)"
      />
      <Button
        v-if="selectedRow?.estadoTransporte === 'PROGRAMADO'"
        type="button"
        label="Iniciar Ruta"
        icon="pi pi-send"
        size="small"
        @click="$emit('iniciar-ruta', selectedRow)"
      />
      <Button
        v-if="selectedRow?.estadoTransporte === 'PROGRAMADO' || selectedRow?.estadoTransporte === 'EN_RUTA'"
        type="button"
        label="Incidencia"
        icon="pi pi-exclamation-triangle"
        severity="warn"
        outlined
        size="small"
        @click="$emit('incidencia', selectedRow)"
      />
      <Button
        v-if="selectedRow?.estadoTransporte === 'EN_RUTA'"
        type="button"
        label="Confirmar Entrega"
        icon="pi pi-check"
        severity="success"
        size="small"
        @click="$emit('entregar', selectedRow)"
      />
      <Button
        v-if="canAnular"
        type="button"
        label="Anular"
        icon="pi pi-ban"
        severity="danger"
        outlined
        size="small"
        @click="$emit('anular', selectedRow)"
      />
    </template>

    <DespachosCanvasGrid
      v-if="viewMode === 'canvas'"
      v-model:selection="selectedRow"
      :despachos="filteredDespachos"
      :loading="loading"
      @detalle="$emit('detalle', $event)"
      @asignar="$emit('asignar', $event)"
      @iniciar-ruta="$emit('iniciar-ruta', $event)"
      @entregar="$emit('entregar', $event)"
    />

    <DataTable
      v-else
      v-model:selection="selectedRow"
      :value="filteredDespachos"
      :loading="loading"
      selectionMode="single"
      dataKey="idDespachoTransporte"
      paginator
      :rows="10"
      :rowsPerPageOptions="[10, 20, 50]"
      responsiveLayout="scroll"
      @row-dblclick="({ data }) => $emit('detalle', data)"
    >
      <template #empty>
        <div class="sga-empty-state">
          <i class="pi pi-truck sga-empty-icon"></i>
          <p class="sga-empty-title">No se encontraron despachos</p>
          <p class="sga-empty-desc">Ajuste los filtros o programe un nuevo despacho terrestre.</p>
        </div>
      </template>

      <Column header="TRACKING & TIPO">
        <template #body="{ data: d }">
          <div class="sga-cell-stack">
            <span class="sga-code-cell font-mono">{{ d.codigoTracking }}</span>
            <Tag
              :value="formatTipoTraslado(d.tipoTraslado)"
              :severity="getTipoTrasladoSeverity(d.tipoTraslado)"
              class="sga-mini-tag"
              rounded
            />
          </div>
        </template>
      </Column>

      <Column header="EXPEDIENTE / CLIENTE">
        <template #body="{ data: d }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ d.clienteRazonSocial || 'Cliente' }}</span>
            <span class="sga-cell-sub font-mono">
              {{ d.codigoExpediente || '—' }}
              <template v-if="d.numeroContenedor"> · {{ d.numeroContenedor }}</template>
            </span>
          </div>
        </template>
      </Column>

      <Column header="UNIDAD & CONDUCTOR">
        <template #body="{ data: d }">
          <div class="sga-cell-stack">
            <span class="sga-code-cell font-mono">{{ d.unidadPlacaTracto || 'Sin asignar' }}</span>
            <span class="sga-cell-sub">{{ d.conductorNombre || 'Sin chofer' }}</span>
          </div>
        </template>
      </Column>

      <Column header="RUTA">
        <template #body="{ data: d }">
          <div class="sga-cell-stack">
            <span class="sga-cell-meta">{{ d.distritoLlegada }}</span>
            <span class="sga-cell-sub">{{ d.direccionLlegada }}</span>
          </div>
        </template>
      </Column>

      <Column header="PROGRAMACIÓN">
        <template #body="{ data: d }">
          <div class="sga-cell-stack">
            <span class="sga-date-cell font-mono">{{ formatDateTime(d.fechaProgramada) }}</span>
            <span v-if="d.guiaRemisionTransporte" class="sga-cell-sub font-mono">
              GRE-T: {{ d.guiaRemisionTransporte }}
            </span>
          </div>
        </template>
      </Column>

      <Column header="ESTADO">
        <template #body="{ data: d }">
          <Tag
            :value="formatEstadoDespacho(d.estadoTransporte)"
            :severity="getEstadoDespachoSeverity(d.estadoTransporte)"
            rounded
          />
        </template>
      </Column>
    </DataTable>
  </SgaListShell>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import DespachosCanvasGrid from './DespachosCanvasGrid.vue'
import {
  formatDateTime,
  formatEstadoDespacho,
  getEstadoDespachoSeverity,
  formatTipoTraslado,
  getTipoTrasladoSeverity
} from '../utils/transporteFormatters'

const props = defineProps({
  despachos: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  initialEstado: { type: String, default: 'ALL' }
})

defineEmits(['refresh', 'programar', 'detalle', 'aceptar', 'asignar', 'iniciar-ruta', 'incidencia', 'entregar', 'anular'])

const viewMode = ref('table')
const selectedRow = ref(null)
const searchQuery = ref('')
const filterEstado = ref(props.initialEstado || 'ALL')
const filterTipo = ref('ALL')

watch(
  () => props.initialEstado,
  (val) => {
    filterEstado.value = val || 'ALL'
  }
)

const tipoFilterOptions = [
  { label: 'Todos los traslados', value: 'ALL' },
  { label: 'Entrega Importación', value: 'ENTREGA_IMPORTACION' },
  { label: 'Recojo Exportación', value: 'RECOJO_EXPORTACION' }
]

const counts = computed(() => {
  const map = {
    ALL: props.despachos.length,
    PENDIENTE_ASIGNACION: 0,
    ACEPTADO: 0,
    PROGRAMADO: 0,
    EN_RUTA: 0,
    ENTREGADO: 0,
    CANCELADO: 0
  }
  for (const d of props.despachos) {
    const est = String(d.estadoTransporte || '').toUpperCase()
    if (est in map) map[est]++
  }
  return map
})

const queueTabs = computed(() => [
  { label: 'Todos', value: 'ALL', count: counts.value.ALL, badgeSeverity: 'secondary' },
  {
    label: 'Pendientes',
    value: 'PENDIENTE_ASIGNACION',
    count: counts.value.PENDIENTE_ASIGNACION,
    badgeSeverity: counts.value.PENDIENTE_ASIGNACION > 0 ? 'warn' : 'secondary'
  },
  { label: 'Aceptados', value: 'ACEPTADO', count: counts.value.ACEPTADO, badgeSeverity: 'info' },
  { label: 'Programados', value: 'PROGRAMADO', count: counts.value.PROGRAMADO, badgeSeverity: 'info' },
  { label: 'En Ruta', value: 'EN_RUTA', count: counts.value.EN_RUTA, badgeSeverity: 'info' },
  { label: 'Entregados', value: 'ENTREGADO', count: counts.value.ENTREGADO, badgeSeverity: 'success' },
  { label: 'Cancelados', value: 'CANCELADO', count: counts.value.CANCELADO, badgeSeverity: 'danger' }
])

const hasActiveFilters = computed(
  () => Boolean(searchQuery.value) || filterEstado.value !== 'ALL' || filterTipo.value !== 'ALL'
)

const resetFilters = () => {
  searchQuery.value = ''
  filterEstado.value = 'ALL'
  filterTipo.value = 'ALL'
}

const filteredDespachos = computed(() => {
  const q = searchQuery.value.toLowerCase()
  return props.despachos.filter((d) => {
    if (filterEstado.value !== 'ALL' && d.estadoTransporte !== filterEstado.value) return false
    if (filterTipo.value !== 'ALL' && d.tipoTraslado !== filterTipo.value) return false
    if (!q) return true
    const haystack = [
      d.codigoTracking,
      d.codigoExpediente,
      d.clienteRazonSocial,
      d.unidadPlacaTracto,
      d.conductorNombre,
      d.distritoLlegada,
      d.direccionLlegada,
      d.guiaRemisionTransporte
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(q)
  })
})

const canAsignar = computed(() => {
  const est = selectedRow.value?.estadoTransporte
  return est === 'PENDIENTE_ASIGNACION' || est === 'ACEPTADO' || est === 'PROGRAMADO'
})

const canAnular = computed(() => {
  const est = selectedRow.value?.estadoTransporte
  return Boolean(selectedRow.value) && est !== 'ENTREGADO' && est !== 'CANCELADO'
})
</script>
