<template>
  <SgaListShell v-model:viewMode="viewMode">
    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchModel"
          placeholder="Buscar por expediente, DAM, cliente, contenedor o agente..."
          size="small"
          fluid
        />
      </IconField>

      <Select
        v-model="agenteModel"
        :options="agenteFilterOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
      />

      <Select
        v-model="regimenModel"
        :options="regimenFilterOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
      />

      <Select
        v-model="canalModel"
        :options="canalFilterOptions"
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

      <div class="sga-queue-buttons" role="tablist" aria-label="Etapas operativas de expedientes">
        <Button
          v-for="f in faseItems"
          :key="f.value"
          type="button"
          role="tab"
          :aria-selected="fase === f.value"
          :label="f.label"
          :badge="String(f.count)"
          :badgeSeverity="f.badgeSeverity"
          :severity="fase === f.value ? 'primary' : 'secondary'"
          :outlined="fase !== f.value"
          size="small"
          @click="$emit('update:fase', f.value)"
        />
      </div>

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
        label="Auditar"
        icon="pi pi-eye"
        size="small"
        :disabled="!selectedRow"
        :title="selectedRow ? 'Ver Ficha y Auditoría Integral del Expediente' : 'Seleccione un expediente'"
        @click="$emit('inspect', selectedRow)"
      />

      <Button
        type="button"
        label="Docs"
        icon="pi pi-paperclip"
        severity="secondary"
        outlined
        size="small"
        :disabled="!selectedRow"
        :title="selectedRow ? 'Gestionar documentos del expediente' : 'Seleccione un expediente'"
        @click="$emit('open-docs', selectedRow)"
      />

      <Button
        v-if="selectedAcciones?.puedeActualizarTransito"
        type="button"
        label="Tránsito"
        icon="pi pi-globe"
        severity="secondary"
        outlined
        size="small"
        title="Actualizar datos de tránsito, contenedor y precinto"
        @click="$emit('open-transito', selectedRow)"
      />

      <Button
        v-if="selectedAcciones?.puedeAsignarCanal"
        type="button"
        label="DAM/Canal"
        icon="pi pi-compass"
        severity="info"
        outlined
        size="small"
        title="Registrar Canal SUNAT y Declaración DAM"
        @click="$emit('open-canal', selectedRow)"
      />
      <Button
        v-else-if="selectedAcciones?.requiereArriboParaCanal"
        type="button"
        label="Esperando Arribo"
        icon="pi pi-clock"
        severity="secondary"
        outlined
        size="small"
        disabled
        title="El agente aduanero debe confirmar la llegada a puerto de destino antes de asignar Canal SUNAT"
      />

      <Button
        v-if="selectedAcciones?.puedeLevante && selectedRow?.canalSunat && selectedRow?.canalSunat !== 'PENDIENTE'"
        type="button"
        label="Levante"
        icon="pi pi-shield"
        severity="success"
        outlined
        size="small"
        title="Autorizar Levante Aduanero"
        @click="$emit('open-levante', selectedRow)"
      />

      <Button
        v-if="selectedAcciones?.puedeRetiro"
        type="button"
        label="Retiro"
        icon="pi pi-building"
        severity="secondary"
        outlined
        size="small"
        title="Registrar acta de retiro en terminal"
        @click="$emit('open-retiro', selectedRow)"
      />

      <Button
        v-if="selectedAcciones?.puedeCerrar"
        type="button"
        label="Cerrar"
        icon="pi pi-lock"
        severity="success"
        outlined
        size="small"
        title="Conformidad y cierre final del expediente"
        @click="$emit('open-cierre', selectedRow)"
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
      :value="expedientes"
      :loading="loading"
      selectionMode="single"
      dataKey="idExpediente"
      responsiveLayout="scroll"
      paginator
      :rows="15"
      :rowsPerPageOptions="[15, 30, 50]"
      currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} expedientes"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
      @row-dblclick="onRowDblClick"
    >
      <template #empty>
        <div class="sga-empty-state">
          <i class="pi pi-folder-open sga-empty-icon"></i>
          <p class="sga-empty-title">No hay expedientes aduaneros con este criterio</p>
          <p class="sga-empty-desc">
            Los expedientes se generan a partir de cotizaciones comerciales aprobadas con pago verificado.
          </p>
        </div>
      </template>

      <Column header="N° EXPEDIENTE">
        <template #body="{ data: exp }">
          <div class="sga-cell-stack">
            <span class="sga-code-cell font-mono">{{ exp.codigoExpediente }}</span>
            <span class="sga-cell-sub font-mono">{{ exp.codigoCotizacion || '—' }}</span>
          </div>
        </template>
      </Column>

      <Column header="CLIENTE / RUC">
        <template #body="{ data: exp }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ exp.clienteRazonSocial || '—' }}</span>
            <span class="sga-cell-sub font-mono">Doc: {{ exp.clienteNumeroDocumento || '—' }}</span>
          </div>
        </template>
      </Column>

      <Column header="RÉGIMEN">
        <template #body="{ data: exp }">
          <span class="sga-cell-muted">{{ formatRegimenShort(exp.regimenAduanero) }}</span>
        </template>
      </Column>

      <Column header="DAM & CANAL SUNAT">
        <template #body="{ data: exp }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary font-mono">{{ exp.numeroDam || 'Sin DAM aún' }}</span>
            <Tag
              :value="exp.canalSunat || 'PENDIENTE'"
              :severity="getCanalSeverity(exp.canalSunat)"
              rounded
            />
          </div>
        </template>
      </Column>

      <Column header="CONTENEDOR / NAVE">
        <template #body="{ data: exp }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary font-mono">{{ exp.numeroContenedor || '—' }}</span>
            <span class="sga-cell-sub">{{ exp.naveViaje || 'Sin nave asignada' }}</span>
          </div>
        </template>
      </Column>

      <Column header="ESTADO TRÁMITE">
        <template #body="{ data: exp }">
          <Tag
            :value="exp.estadoExpediente || 'ASIGNADO'"
            :severity="getExpedienteSeverity(exp.estadoExpediente)"
            rounded
          />
        </template>
      </Column>

      <Column header="AGENTE ADUANERO">
        <template #body="{ data: exp }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ exp.agenteAduaneroNombre || 'Sin asignar' }}</span>
            <span
              v-if="exp.idAgenteAduanero && exp.idAgenteAduanero === currentUserId"
              class="sga-cell-sub sga-text-success"
            >
              Asignado a mí
            </span>
          </div>
        </template>
      </Column>

      <Column header="LEVANTE">
        <template #body="{ data: exp }">
          <span v-if="exp.fechaLevanteAutorizacion" class="sga-status-note sga-text-success">
            <i class="pi pi-check-circle"></i> Autorizado
          </span>
          <span v-else class="sga-cell-muted">Pendiente</span>
        </template>
      </Column>
    </DataTable>

    <AduanasExpedientesCanvasGrid
      v-else
      v-model:selection="selectedRow"
      :expedientes="expedientes"
      :loading="loading"
      @inspect="$emit('inspect', $event)"
    />
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
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import AduanasExpedientesCanvasGrid from './AduanasExpedientesCanvasGrid.vue'
import { obtenerAccionesPermitidasExpediente } from '../utils/aduanasWorkflowUtils'
import {
  formatRegimenShort,
  getCanalSeverity,
  getExpedienteSeverity
} from '../utils/aduanasFormatters'

const props = defineProps({
  expedientes: { type: Array, default: () => [] },
  faseCounts: {
    type: Object,
    default: () => ({
      TODOS: 0,
      PENDIENTE_CANAL: 0,
      CONTROL_SUNAT: 0,
      LEVANTE_AUTORIZADO: 0,
      CERRADOS: 0
    })
  },
  agentesDisponibles: { type: Array, default: () => [] },
  misExpedientesCount: { type: Number, default: 0 },
  currentUserId: { type: [Number, String], default: null },
  loading: { type: Boolean, default: false },
  fase: { type: String, default: 'TODOS' },
  filterAgente: { type: String, default: 'ALL' },
  filterRegimen: { type: String, default: 'ALL' },
  filterCanal: { type: String, default: 'ALL' },
  searchQuery: { type: String, default: '' },
  sortBy: { type: String, default: 'RECENT' },
  hasActiveFilters: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:fase',
  'update:filterAgente',
  'update:filterRegimen',
  'update:filterCanal',
  'update:searchQuery',
  'update:sortBy',
  'reset-filters',
  'refresh',
  'inspect',
  'open-docs',
  'open-transito',
  'open-canal',
  'open-levante',
  'open-retiro',
  'open-cierre'
])

const viewMode = ref('table')
const selectedRow = ref(null)

const searchModel = computed({
  get: () => props.searchQuery,
  set: (val) => emit('update:searchQuery', val)
})

const agenteModel = computed({
  get: () => props.filterAgente,
  set: (val) => emit('update:filterAgente', val)
})

const regimenModel = computed({
  get: () => props.filterRegimen,
  set: (val) => emit('update:filterRegimen', val)
})

const canalModel = computed({
  get: () => props.filterCanal,
  set: (val) => emit('update:filterCanal', val)
})

const sortModel = computed({
  get: () => props.sortBy,
  set: (val) => emit('update:sortBy', val)
})

const agenteFilterOptions = computed(() => {
  const opts = [{ label: 'Todos los Agentes', value: 'ALL' }]
  if (props.currentUserId) {
    opts.push({ label: `Mis Expedientes (${props.misExpedientesCount})`, value: 'MINE' })
  }
  opts.push({ label: 'Sin Agente Asignado', value: 'UNASSIGNED' })
  ;(props.agentesDisponibles || []).forEach((a) => {
    opts.push({ label: a.nombre, value: String(a.id) })
  })
  return opts
})

const regimenFilterOptions = [
  { label: 'Todos los Regímenes', value: 'ALL' },
  { label: 'Importación (REG-10)', value: 'IMPORTACION' },
  { label: 'Exportación (REG-40)', value: 'EXPORTACION' }
]

const canalFilterOptions = [
  { label: 'Todos los Canales', value: 'ALL' },
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

const faseItems = computed(() => [
  { value: 'TODOS', label: 'Todos', count: props.faseCounts.TODOS || 0, badgeSeverity: 'contrast' },
  { value: 'PENDIENTE_CANAL', label: 'Por Asignar Canal', count: props.faseCounts.PENDIENTE_CANAL || 0, badgeSeverity: 'warn' },
  { value: 'CONTROL_SUNAT', label: 'Control SUNAT', count: props.faseCounts.CONTROL_SUNAT || 0, badgeSeverity: 'info' },
  { value: 'LEVANTE_AUTORIZADO', label: 'Levante', count: props.faseCounts.LEVANTE_AUTORIZADO || 0, badgeSeverity: 'success' },
  { value: 'CERRADOS', label: 'Cerrados', count: props.faseCounts.CERRADOS || 0, badgeSeverity: 'secondary' }
])

const selectedAcciones = computed(() => {
  return selectedRow.value ? obtenerAccionesPermitidasExpediente(selectedRow.value) : null
})

watch(
  () => props.expedientes,
  (rows) => {
    if (!selectedRow.value) return
    const updated = (rows || []).find((r) => r.idExpediente === selectedRow.value.idExpediente)
    selectedRow.value = updated || null
  }
)

const onRowDblClick = (event) => {
  if (event?.data) {
    emit('inspect', event.data)
  }
}
</script>

<style scoped>
.sga-queue-buttons {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.sga-status-note {
  font-size: 0.8rem;
  font-weight: 600;
}
</style>
