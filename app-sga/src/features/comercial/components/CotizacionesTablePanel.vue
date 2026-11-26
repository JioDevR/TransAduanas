<template>
  <ComercialListShell v-model:viewMode="viewMode">
    <template #header-right>
      <Button
        type="button"
        label="Nueva Cotización"
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
          placeholder="Buscar por N° cotización, cliente o RUC..."
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
        v-model="estadoModel"
        :options="estadoFilterOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
      />

      <Select
        v-if="!isAgenteComercial"
        v-model="asesorModel"
        :options="asesorFilterOptions"
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
        :title="selectedRow ? 'Ver / adjuntar documentos' : 'Seleccione una cotización'"
        @click="$emit('open-docs', selectedRow)"
      />

      <Button
        type="button"
        :label="editActionLabel"
        :icon="isSelectedObserved ? 'pi pi-file-edit' : 'pi pi-pencil'"
        severity="secondary"
        outlined
        size="small"
        :disabled="!selectedRow"
        :title="selectedRow ? editActionLabel : 'Seleccione una cotización'"
        @click="handleEditSelected"
      />

      <Button
        type="button"
        label="Enviar a Aduanas"
        icon="pi pi-send"
        severity="info"
        outlined
        size="small"
        :disabled="!canSendToAduanas"
        :title="sendToAduanasTitle"
        @click="$emit('send-aduanas', selectedRow)"
      />

      <Button
        type="button"
        label="WhatsApp"
        icon="pi pi-whatsapp"
        severity="success"
        outlined
        size="small"
        :disabled="!selectedRow || !canNotifyWhatsApp"
        :title="whatsAppTitle"
        @click="$emit('notify-whatsapp', selectedRow)"
      />

      <Button
        v-if="canDeleteSelected"
        type="button"
        icon="pi pi-trash"
        severity="danger"
        outlined
        size="small"
        title="Eliminar cotización"
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
          <i class="pi pi-file-edit sga-empty-icon"></i>
          <p class="sga-empty-title">No hay cotizaciones registradas</p>
          <p class="sga-empty-desc">Haga clic en "+ Nueva Cotización" para emitir una nueva oferta comercial.</p>
        </div>
      </template>

      <Column header="N° COTIZACIÓN">
        <template #body="{ data: c }">
          <span class="sga-code-cell font-mono">{{ c.codigoCotizacion }}</span>
        </template>
      </Column>

      <Column header="CLIENTE / PROSPECTO">
        <template #body="{ data: c }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ c.clienteRazonSocial || 'Cliente N/A' }}</span>
            <div class="sga-cell-meta">
              <span class="sga-cell-sub font-mono">{{ c.clienteNumeroDocumento }}</span>
              <Tag
                :value="c.clienteTieneUsuario ? 'Portal B2B' : 'Presencial'"
                :severity="c.clienteTieneUsuario ? 'info' : 'secondary'"
                class="sga-mini-tag"
              />
            </div>
          </div>
        </template>
      </Column>

      <Column v-if="!isAgenteComercial" header="ASESOR COMERCIAL">
        <template #body="{ data: c }">
          <span v-if="c.usuarioComercialNombre" class="sga-cell-muted">{{ c.usuarioComercialNombre }}</span>
          <Tag v-else value="Sin Asignar" severity="warn" rounded />
        </template>
      </Column>

      <Column header="RÉGIMEN ADUANERO">
        <template #body="{ data: c }">
          <span class="sga-cell-muted">{{ formatRegimenShort(c.regimenAduanero) }}</span>
        </template>
      </Column>

      <Column header="TOTAL COTIZADO">
        <template #body="{ data: c }">
          <Tag
            v-if="Number(c.totalCotizado || 0) <= 0"
            value="Por liquidar ($0.00)"
            severity="warn"
            rounded
          />
          <span v-else class="sga-amount-cell font-mono">${{ formatUsd(c.totalCotizado) }}</span>
        </template>
      </Column>

      <Column header="FECHA EMISIÓN">
        <template #body="{ data: c }">
          <span class="sga-date-cell font-mono">{{ c.fechaEmision }}</span>
        </template>
      </Column>

      <Column header="ESTADO">
        <template #body="{ data: c }">
          <div class="sga-status-stack">
            <Tag
              :value="getCotizacionLabel(c.estado)"
              :severity="getCotizacionSeverity(c.estado)"
              rounded
            />
            <button
              v-if="isCotizacionObservada(c) && c.motivoObservacion"
              type="button"
              class="sga-obs-link"
              title="Ver observación de Supervisión Aduanera"
              @click.stop="$emit('view-observation', c)"
            >
              <i class="pi pi-exclamation-triangle"></i>
              <span>Ver motivo</span>
            </button>
          </div>
        </template>
      </Column>

      <Column header="PAGO / ANTICIPO">
        <template #body="{ data: c }">
          <div v-if="c.estado === 'APROBADA'" class="sga-cell-stack">
            <span :class="['sga-pay-label', c.totalAbonado > 0 ? 'sga-text-success' : 'sga-text-info']">
              {{ getEstadoPagoLabel(c.estadoPago) }}
            </span>
            <span v-if="c.totalAbonado > 0" class="sga-cell-sub font-mono sga-text-success">
              Abono: ${{ formatUsd(c.totalAbonado) }}
            </span>
            <span v-else class="sga-cell-sub">Sin abono confirmado</span>
          </div>
          <span v-else class="sga-text-muted">—</span>
        </template>
      </Column>

      <Column header="EXPEDIENTE ADUANERO">
        <template #body="{ data: c }">
          <span v-if="c.tieneExpediente" class="sga-exp-link font-mono sga-text-success">
            <i class="pi pi-folder"></i>
            <span>{{ c.codigoExpediente }}</span>
          </span>
          <span v-else-if="c.estado === 'APROBADA' && c.tienePagoAprobado" class="sga-text-success sga-cell-status">
            Aprobada (Pago Validado)
          </span>
          <span v-else-if="c.estado === 'APROBADA'" class="sga-text-warn sga-cell-status">
            Aprobada (Requiere Pago Finanzas)
          </span>
          <span v-else class="sga-text-muted">—</span>
        </template>
      </Column>
    </DataTable>

    <CotizacionesCanvasGrid
      v-else
      v-model:selection="selectedRow"
      :cotizaciones="cotizaciones"
      :loading="loading"
      :is-agente-comercial="isAgenteComercial"
      :get-cotizacion-label="getCotizacionLabel"
      :get-cotizacion-severity="getCotizacionSeverity"
      :get-estado-pago-label="getEstadoPagoLabel"
      @open-item="(c) => onRowDblClick({ data: c })"
      @view-observation="(c) => $emit('view-observation', c)"
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
import CotizacionesCanvasGrid from './CotizacionesCanvasGrid.vue'
import { formatUsd, formatRegimenShort, isCotizacionObservada } from '../utils/comercialFormatters'

const props = defineProps({
  cotizaciones: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  isAgenteComercial: { type: Boolean, default: false },
  searchCotizacion: { type: String, default: '' },
  filterRegimenCotizacion: { type: String, default: 'ALL' },
  filterEstadoCotizacion: { type: String, default: '' },
  filterAsesorCotizacion: { type: String, default: 'ALL' },
  sortByCotizacion: { type: String, default: 'fecha-desc' },
  hasActiveFilters: { type: Boolean, default: false },
  asesoresDisponibles: { type: Array, default: () => [] },
  getCotizacionLabel: { type: Function, required: true },
  getCotizacionSeverity: { type: Function, required: true },
  getEstadoPagoLabel: { type: Function, required: true },
  getClienteTelefono: { type: Function, required: true }
})

const emit = defineEmits([
  'update:searchCotizacion',
  'update:filterRegimenCotizacion',
  'update:filterEstadoCotizacion',
  'update:filterAsesorCotizacion',
  'update:sortByCotizacion',
  'reset-filters',
  'refresh',
  'create',
  'edit',
  'correct',
  'open-docs',
  'view-observation',
  'send-aduanas',
  'notify-whatsapp',
  'delete'
])

const selectedRow = ref(null)
const viewMode = ref('canvas')

watch(
  () => props.cotizaciones,
  (list) => {
    if (!selectedRow.value) return
    const updated = list.find((item) => item.idCotizacion === selectedRow.value.idCotizacion)
    selectedRow.value = updated || null
  }
)

const searchModel = computed({
  get: () => props.searchCotizacion,
  set: (v) => emit('update:searchCotizacion', v)
})

const regimenModel = computed({
  get: () => props.filterRegimenCotizacion,
  set: (v) => emit('update:filterRegimenCotizacion', v)
})

const estadoModel = computed({
  get: () => props.filterEstadoCotizacion,
  set: (v) => emit('update:filterEstadoCotizacion', v)
})

const asesorModel = computed({
  get: () => props.filterAsesorCotizacion,
  set: (v) => emit('update:filterAsesorCotizacion', v)
})

const sortModel = computed({
  get: () => props.sortByCotizacion,
  set: (v) => emit('update:sortByCotizacion', v)
})

const regimenFilterOptions = [
  { label: 'Todos los Regímenes', value: 'ALL' },
  { label: 'Importación (REG-10)', value: 'IMPORTACION_DEFINITIVA' },
  { label: 'Exportación (REG-40)', value: 'EXPORTACION_DEFINITIVA' }
]

const estadoFilterOptions = [
  { label: 'Todos los Estados', value: '' },
  { label: 'Pendientes de Revisión', value: 'PENDIENTE' },
  { label: 'Observadas (Para Corrección)', value: 'OBSERVADA' },
  { label: 'Aprobadas', value: 'APROBADA' },
  { label: 'Rechazadas', value: 'RECHAZADA' },
  { label: 'Anuladas', value: 'ANULADA' }
]

const asesorFilterOptions = computed(() => [
  { label: 'Todos los Asesores', value: 'ALL' },
  { label: 'Sin Asignar', value: 'UNASSIGNED' },
  ...props.asesoresDisponibles.map((a) => ({ label: a.nombre, value: String(a.id) }))
])

const sortOptions = [
  { label: 'Ordenar por: Fecha (Más recientes)', value: 'fecha-desc' },
  { label: 'Ordenar por: Fecha (Más antiguas)', value: 'fecha-asc' },
  { label: 'Ordenar por: Monto (Mayor a menor)', value: 'monto-desc' },
  { label: 'Ordenar por: Monto (Menor a mayor)', value: 'monto-asc' },
  { label: 'Ordenar por: N° Cotización (A - Z)', value: 'codigo-asc' },
  { label: 'Ordenar por: Cliente (A - Z)', value: 'cliente-asc' }
]

const isSelectedObserved = computed(() => isCotizacionObservada(selectedRow.value))

const editActionLabel = computed(() => {
  if (!selectedRow.value) return 'Editar'
  if (isSelectedObserved.value) return 'Corregir'
  if (Number(selectedRow.value.totalCotizado || 0) <= 0) return 'Completar'
  return 'Editar'
})

const canDeleteSelected = computed(() => {
  if (!selectedRow.value) return false
  return !selectedRow.value.tieneExpediente && selectedRow.value.estado !== 'APROBADA'
})

const canSendToAduanas = computed(() => {
  const c = selectedRow.value
  if (!c) return false
  return ['PENDIENTE', 'OBSERVADA'].includes(c.estado) && Number(c.totalCotizado || 0) > 0
})

const sendToAduanasTitle = computed(() => {
  if (!selectedRow.value) return 'Seleccione una cotización'
  if (Number(selectedRow.value.totalCotizado || 0) <= 0) return 'Complete la liquidación económica primero'
  if (!['PENDIENTE', 'OBSERVADA'].includes(selectedRow.value.estado)) return 'La cotización ya fue enviada o procesada'
  return 'Enviar cotización a Gestión Aduanera para revisión técnica'
})

const canNotifyWhatsApp = computed(() => {
  const c = selectedRow.value
  if (!c) return false
  const raw = c.clienteTelefono || props.getClienteTelefono(c.idCliente) || ''
  return Boolean(String(raw).replace(/[^0-9]/g, ''))
})

const whatsAppTitle = computed(() => {
  if (!selectedRow.value) return 'Seleccione una cotización'
  if (!canNotifyWhatsApp.value) return 'El cliente no tiene teléfono registrado'
  return 'Enviar resumen de estado por WhatsApp al cliente'
})

const handleEditSelected = () => {
  if (!selectedRow.value) return
  if (isSelectedObserved.value) {
    emit('correct', selectedRow.value)
  } else {
    emit('edit', selectedRow.value)
  }
}

const onRowDblClick = ({ data }) => {
  if (!data) return
  if (isCotizacionObservada(data)) {
    emit('correct', data)
  } else {
    emit('edit', data)
  }
}
</script>

<style scoped>
.sga-status-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.3rem;
}

.sga-obs-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  background: transparent;
  border: none;
  padding: 0;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-warning-text);
  cursor: pointer;
}

.sga-pay-label {
  font-size: 0.75rem;
  font-weight: 600;
}

.sga-cell-status {
  font-size: 0.75rem;
  font-weight: 600;
}

.sga-exp-link {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.85em;
  font-weight: 600;
  white-space: nowrap;
}
</style>
