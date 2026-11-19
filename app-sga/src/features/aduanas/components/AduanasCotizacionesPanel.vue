<template>
  <SgaListShell v-model:viewMode="viewMode">
    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchModel"
          placeholder="Buscar por código, cliente o RUC..."
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

      <div class="sga-queue-buttons" role="tablist" aria-label="Filtros rápidos de bandeja">
        <Button
          v-for="q in subQueueItems"
          :key="q.value"
          type="button"
          role="tab"
          :aria-selected="subQueue === q.value"
          :label="q.label"
          :badge="String(q.count)"
          :badgeSeverity="q.badgeSeverity"
          :severity="subQueue === q.value ? 'primary' : 'secondary'"
          :outlined="subQueue !== q.value"
          size="small"
          @click="$emit('update:subQueue', q.value)"
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
        :title="selectedRow ? 'Auditar en vista completa con documentos' : 'Seleccione una cotización'"
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
        :title="selectedRow ? 'Gestionar documentos y anexos' : 'Seleccione una cotización'"
        @click="$emit('open-docs', selectedRow)"
      />

      <Button
        v-if="canApproveSelected"
        type="button"
        label="Aprobar"
        icon="pi pi-check"
        severity="success"
        outlined
        size="small"
        title="Aprobar técnicamente para apertura de Expediente Aduanero"
        @click="$emit('approve', selectedRow)"
      />

      <Button
        v-if="canObserveSelected"
        type="button"
        label="Observar"
        icon="pi pi-file-edit"
        severity="warn"
        outlined
        size="small"
        title="Devolver para revisión a Ventas"
        @click="$emit('observe', selectedRow)"
      />

      <Button
        v-if="canRejectSelected"
        type="button"
        icon="pi pi-times"
        severity="danger"
        outlined
        size="small"
        title="Rechazar cotización"
        @click="$emit('reject', selectedRow)"
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
          <p class="sga-empty-title">No hay cotizaciones con este criterio</p>
          <p class="sga-empty-desc">
            Las cotizaciones emitidas por los ejecutivos de ventas aparecerán aquí para su validación técnica.
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

      <Column header="COMERCIAL">
        <template #body="{ data: c }">
          <span class="sga-cell-muted">{{ c.usuarioComercialNombre || 'Asesor Comercial' }}</span>
        </template>
      </Column>

      <Column header="RÉGIMEN ADUANERO">
        <template #body="{ data: c }">
          <span class="sga-cell-muted">{{ formatRegimenShort(c.regimenAduanero) }}</span>
        </template>
      </Column>

      <Column header="TOTAL COTIZADO" headerClass="text-right" bodyClass="text-right">
        <template #body="{ data: c }">
          <span class="sga-amount-cell font-mono">${{ formatUsd(c.totalCotizado) }}</span>
        </template>
      </Column>

      <Column header="ESTADO">
        <template #body="{ data: c }">
          <Tag
            :value="getEstadoCotizacionLabel(c.estado)"
            :severity="getEstadoCotizacionSeverity(c.estado)"
            rounded
          />
        </template>
      </Column>

      <Column header="PAGO / ANTICIPO">
        <template #body="{ data: c }">
          <div v-if="c.estado === 'APROBADA'" class="sga-cell-stack">
            <Tag
              :value="getEstadoPagoLabel(c.estadoPago)"
              :severity="getEstadoPagoSeverity(c.estadoPago)"
              rounded
            />
            <span v-if="c.totalAbonado > 0" class="sga-cell-sub font-mono sga-text-success">
              Abono: ${{ formatUsd(c.totalAbonado) }}
            </span>
            <span v-else class="sga-cell-sub">Sin abono confirmado</span>
          </div>
          <span v-else class="sga-cell-muted">—</span>
        </template>
      </Column>

      <Column header="EXPEDIENTE ADUANERO">
        <template #body="{ data: c }">
          <span v-if="c.tieneExpediente" class="sga-code-cell font-mono sga-text-success">
            <i class="pi pi-folder"></i> {{ c.codigoExpediente }}
          </span>
          <span
            v-else-if="c.estado === 'APROBADA' && c.tienePagoAprobado"
            class="sga-status-note sga-text-success"
          >
            Listo para Expediente
          </span>
          <span
            v-else-if="c.estado === 'APROBADA'"
            class="sga-status-note sga-text-warn"
          >
            Requiere Pago Finanzas
          </span>
          <span v-else class="sga-cell-muted">—</span>
        </template>
      </Column>
    </DataTable>

    <AduanasCotizacionesCanvasGrid
      v-else
      v-model:selection="selectedRow"
      :cotizaciones="cotizaciones"
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
import AduanasCotizacionesCanvasGrid from './AduanasCotizacionesCanvasGrid.vue'
import {
  formatUsd,
  formatRegimenShort,
  getEstadoCotizacionLabel,
  getEstadoCotizacionSeverity,
  getEstadoPagoLabel,
  getEstadoPagoSeverity,
  isCotizacionRevisable,
  isCotizacionObservable
} from '../utils/aduanasFormatters'

const props = defineProps({
  cotizaciones: { type: Array, default: () => [] },
  clasificadas: {
    type: Object,
    default: () => ({
      nuevas: [],
      subsanadas: [],
      pendientesPago: [],
      listasApertura: [],
      historico: []
    })
  },
  totalCount: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
  subQueue: { type: String, default: 'nuevas' },
  searchQuery: { type: String, default: '' },
  filterRegimen: { type: String, default: 'ALL' },
  sortBy: { type: String, default: 'RECENT' },
  hasActiveFilters: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:subQueue',
  'update:searchQuery',
  'update:filterRegimen',
  'update:sortBy',
  'reset-filters',
  'refresh',
  'inspect',
  'open-docs',
  'approve',
  'observe',
  'reject'
])

const viewMode = ref('table')
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

const subQueueItems = computed(() => [
  {
    value: 'nuevas',
    label: 'Por auditar',
    count: props.clasificadas.nuevas?.length || 0,
    badgeSeverity: 'warn'
  },
  {
    value: 'subsanadas',
    label: 'Corregidos',
    count: props.clasificadas.subsanadas?.length || 0,
    badgeSeverity: 'danger'
  },
  {
    value: 'historico',
    label: 'Concluidas',
    count: props.clasificadas.historico?.length || 0,
    badgeSeverity: 'secondary'
  },
  {
    value: 'todas',
    label: 'Todos',
    count: props.totalCount || 0,
    badgeSeverity: 'contrast'
  }
])

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

const canApproveSelected = computed(() => isCotizacionRevisable(selectedRow.value?.estado))
const canRejectSelected = computed(() => isCotizacionRevisable(selectedRow.value?.estado))
const canObserveSelected = computed(() => isCotizacionObservable(selectedRow.value?.estado))

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
    emit('inspect', event.data)
  }
}
</script>


