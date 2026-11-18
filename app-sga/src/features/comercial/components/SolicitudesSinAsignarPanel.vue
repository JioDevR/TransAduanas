<template>
  <ComercialListShell v-model:viewMode="viewMode">
    <template #header-right>
      <Tag
        :value="`Disponibles: ${totalCount}`"
        :severity="totalCount > 0 ? 'warn' : 'secondary'"
        rounded
      />
    </template>

    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchModel"
          placeholder="Buscar por N° solicitud, cliente, RUC o mercancía..."
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
        label="Docs"
        icon="pi pi-paperclip"
        severity="secondary"
        outlined
        size="small"
        :disabled="!selectedRow"
        :title="selectedRow ? 'Ver documentos adjuntos' : 'Seleccione una solicitud'"
        @click="$emit('open-docs', selectedRow)"
      />

      <Button
        type="button"
        label="Tomar y Liquidar"
        icon="pi pi-user-plus"
        size="small"
        :disabled="!selectedRow"
        :title="selectedRow ? 'Revisar y liquidar precios antes de tomarla' : 'Seleccione una solicitud'"
        @click="$emit('take', selectedRow)"
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
      :value="solicitudes"
      :loading="loading"
      selectionMode="single"
      dataKey="idCotizacion"
      responsiveLayout="scroll"
      paginator
      :rows="15"
      :rowsPerPageOptions="[15, 30, 50]"
      currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} solicitudes"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
      @row-dblclick="onRowDblClick"
    >
      <template #empty>
        <div class="sga-empty-state">
          <i class="pi pi-inbox sga-empty-icon"></i>
          <p class="sga-empty-title">No hay solicitudes pendientes por asignar</p>
          <p class="sga-empty-desc">Las nuevas cotizaciones enviadas desde el Portal B2B aparecerán aquí.</p>
        </div>
      </template>

      <Column header="N° SOLICITUD">
        <template #body="{ data: s }">
          <span class="sga-code-cell font-mono">{{ s.codigoCotizacion }}</span>
        </template>
      </Column>

      <Column header="CLIENTE B2B">
        <template #body="{ data: s }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ s.clienteRazonSocial || 'Cliente N/A' }}</span>
            <div class="sga-cell-meta">
              <span class="sga-cell-sub font-mono">{{ s.clienteNumeroDocumento }}</span>
              <Tag value="Portal B2B" severity="info" class="sga-mini-tag" />
            </div>
          </div>
        </template>
      </Column>

      <Column header="OPERACIÓN & CARGA">
        <template #body="{ data: s }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ formatRegimenShort(s.regimenAduanero) }}</span>
            <span v-if="s.mercanciaNombre" class="sga-cell-sub">{{ s.mercanciaNombre }}</span>
            <span v-if="s.puertoEnvioNombre || s.puertoDestinoNombre" class="sga-cell-sub">
              {{ s.puertoEnvioNombre || 'Origen' }} → {{ s.puertoDestinoNombre || 'Destino' }}
            </span>
          </div>
        </template>
      </Column>

      <Column header="FOB / TOTAL ESTIMADO">
        <template #body="{ data: s }">
          <div class="sga-cell-stack">
            <template v-if="Number(s.totalCotizado || 0) <= 0">
              <Tag value="Por liquidar" severity="warn" rounded />
              <span class="sga-cell-sub font-mono">
                FOB: ${{ formatUsd(s.valorFob) }}
                <template v-if="Number(s.montoFleteEstimado || 0) > 0">
                  · Flete decl.: ${{ formatUsd(s.montoFleteEstimado) }}
                </template>
              </span>
            </template>
            <template v-else>
              <span class="sga-amount-cell font-mono">${{ formatUsd(s.totalCotizado) }}</span>
              <span class="sga-cell-sub font-mono">FOB: ${{ formatUsd(s.valorFob) }}</span>
            </template>
          </div>
        </template>
      </Column>

      <Column header="FECHA EMISIÓN">
        <template #body="{ data: s }">
          <span class="sga-date-cell font-mono">{{ s.fechaEmision }}</span>
        </template>
      </Column>

      <Column header="ESTADO">
        <template #body="{ data: s }">
          <Tag
            :value="getCotizacionLabel(s.estado)"
            :severity="getCotizacionSeverity(s.estado)"
            rounded
          />
        </template>
      </Column>
    </DataTable>

    <SolicitudesCanvasGrid
      v-else
      v-model:selection="selectedRow"
      :solicitudes="solicitudes"
      :loading="loading"
      :get-cotizacion-label="getCotizacionLabel"
      :get-cotizacion-severity="getCotizacionSeverity"
      @take-item="(s) => $emit('take', s)"
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
import SolicitudesCanvasGrid from './SolicitudesCanvasGrid.vue'
import { formatUsd, formatRegimenShort } from '../utils/comercialFormatters'

const props = defineProps({
  solicitudes: { type: Array, default: () => [] },
  totalCount: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
  searchSolicitud: { type: String, default: '' },
  filterRegimenSolicitud: { type: String, default: 'ALL' },
  sortBySolicitud: { type: String, default: 'fecha-desc' },
  hasActiveFilters: { type: Boolean, default: false },
  getCotizacionLabel: { type: Function, required: true },
  getCotizacionSeverity: { type: Function, required: true }
})

const emit = defineEmits([
  'update:searchSolicitud',
  'update:filterRegimenSolicitud',
  'update:sortBySolicitud',
  'reset-filters',
  'refresh',
  'open-docs',
  'take'
])

const selectedRow = ref(null)
const viewMode = ref('canvas')

watch(
  () => props.solicitudes,
  (list) => {
    if (!selectedRow.value) return
    const updated = list.find((item) => item.idCotizacion === selectedRow.value.idCotizacion)
    selectedRow.value = updated || null
  }
)

const searchModel = computed({
  get: () => props.searchSolicitud,
  set: (v) => emit('update:searchSolicitud', v)
})

const regimenModel = computed({
  get: () => props.filterRegimenSolicitud,
  set: (v) => emit('update:filterRegimenSolicitud', v)
})

const sortModel = computed({
  get: () => props.sortBySolicitud,
  set: (v) => emit('update:sortBySolicitud', v)
})

const regimenFilterOptions = [
  { label: 'Todos los Regímenes', value: 'ALL' },
  { label: 'Importación (REG-10)', value: 'IMPORTACION_DEFINITIVA' },
  { label: 'Exportación (REG-40)', value: 'EXPORTACION_DEFINITIVA' }
]

const sortOptions = [
  { label: 'Ordenar por: Más recientes', value: 'fecha-desc' },
  { label: 'Ordenar por: Más antiguas', value: 'fecha-asc' },
  { label: 'Ordenar por: Monto (Mayor a menor)', value: 'monto-desc' },
  { label: 'Ordenar por: Cliente (A - Z)', value: 'cliente-asc' }
]

const onRowDblClick = ({ data }) => {
  if (data) emit('take', data)
}
</script>
