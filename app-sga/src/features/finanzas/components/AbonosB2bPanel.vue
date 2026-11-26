<template>
  <SgaListShell v-model:viewMode="viewMode" title="Bandeja de Auditoría de Abonos B2B">
    <template #header-right>
      <div class="sga-toolbar-actions">
        <Tag
          v-if="counts.pendientes > 0"
          :value="`${counts.pendientes} por validar`"
          severity="warn"
          rounded
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
      </div>
    </template>

    <template #sub-bar>
      <div class="sga-queue-buttons" role="tablist" aria-label="Estados de abonos B2B">
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
          placeholder="Buscar por pago, cotización, cliente, banco u operación..."
          size="small"
          fluid
        />
      </IconField>

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
        label="Ver Voucher"
        icon="pi pi-file-pdf"
        severity="secondary"
        outlined
        size="small"
        :disabled="!selectedRow || (!selectedRow.idDocumentoComprobante && !selectedRow.documentoComprobanteUrl)"
        @click="$emit('preview-voucher', selectedRow)"
      />
      <Button
        v-if="isPendingSelected"
        type="button"
        label="Aprobar"
        icon="pi pi-check"
        severity="success"
        size="small"
        @click="$emit('validate', selectedRow, 'APROBAR')"
      />
      <Button
        v-if="isPendingSelected"
        type="button"
        label="Observar"
        icon="pi pi-eye"
        severity="warn"
        outlined
        size="small"
        @click="$emit('validate', selectedRow, 'OBSERVAR')"
      />
      <Button
        v-if="isPendingSelected"
        type="button"
        label="Rechazar"
        icon="pi pi-ban"
        severity="danger"
        outlined
        size="small"
        @click="$emit('validate', selectedRow, 'RECHAZAR')"
      />
    </template>

    <DataTable
      v-if="viewMode === 'table'"
      v-model:selection="selectedRow"
      :value="filteredAnticipos"
      :loading="loading"
      selectionMode="single"
      dataKey="idPago"
      responsiveLayout="scroll"
      paginator
      :rows="12"
      :rowsPerPageOptions="[12, 25, 50]"
      currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} abonos"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
    >
      <template #empty>
        <div class="sga-empty-state">
          <i class="pi pi-verified sga-empty-icon"></i>
          <p class="sga-empty-title">No hay abonos en este estado</p>
          <p class="sga-empty-desc">
            Los comprobantes y pagos que registren los clientes desde el Portal B2B aparecerán aquí para revisión contable.
          </p>
        </div>
      </template>

      <Column header="N° PAGO">
        <template #body="{ data: a }">
          <div class="sga-cell-stack">
            <span class="sga-code-cell font-mono">{{ a.codigoPago }}</span>
            <span class="sga-cell-sub font-mono">{{ formatDateTime(a.fechaRegistro) }}</span>
          </div>
        </template>
      </Column>

      <Column header="COTIZACIÓN ORIGEN">
        <template #body="{ data: a }">
          <div class="sga-cell-stack">
            <span class="sga-code-cell font-mono">{{ a.codigoCotizacion }}</span>
            <span class="sga-cell-sub font-mono">Total: {{ formatUsd(a.totalCotizado) }}</span>
          </div>
        </template>
      </Column>

      <Column header="CLIENTE IMPORTADOR">
        <template #body="{ data: a }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ a.clienteRazonSocial || 'Cliente no asignado' }}</span>
            <span class="sga-cell-sub font-mono">Doc: {{ a.clienteNumeroDocumento || '—' }}</span>
          </div>
        </template>
      </Column>

      <Column header="MONTO ABONADO" headerClass="text-right" bodyClass="text-right">
        <template #body="{ data: a }">
          <div class="sga-cell-stack">
            <span class="sga-amount-cell font-mono sga-text-success">
              {{ a.moneda === 'PEN' ? formatPen(a.montoAbonado) : formatUsd(a.montoAbonado) }}
            </span>
            <span v-if="a.moneda === 'USD' && a.montoEquivalentePen" class="sga-cell-sub font-mono">
              Equiv: {{ formatPen(a.montoEquivalentePen) }}
            </span>
          </div>
        </template>
      </Column>

      <Column header="BANCO & N° OPERACIÓN">
        <template #body="{ data: a }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ a.bancoDestino }}</span>
            <span class="sga-cell-sub font-mono">Op: {{ a.numeroOperacion }} ({{ a.fechaOperacion }})</span>
          </div>
        </template>
      </Column>

      <Column header="MODALIDAD">
        <template #body="{ data: a }">
          <Tag :value="formatTipoAbono(a.tipoAbono)" severity="info" rounded />
        </template>
      </Column>

      <Column header="ESTADO FINANZAS">
        <template #body="{ data: a }">
          <div class="sga-cell-stack">
            <Tag
              :value="getEstadoAnticipoLabel(a.estadoPago)"
              :severity="getEstadoAnticipoSeverity(a.estadoPago)"
              rounded
            />
            <span v-if="a.numeroFacturaEmitida" class="sga-cell-sub font-mono">
              Fact: {{ a.numeroFacturaEmitida }}
            </span>
          </div>
        </template>
      </Column>

      <Column header="ACCIONES" headerClass="text-right" bodyClass="text-right">
        <template #body="{ data: a }">
          <div class="sga-row-actions">
            <Button
              v-if="a.idDocumentoComprobante || a.documentoComprobanteUrl"
              type="button"
              label="Voucher"
              icon="pi pi-file-pdf"
              severity="secondary"
              outlined
              size="small"
              @click="$emit('preview-voucher', a)"
            />
            <template v-if="a.estadoPago === 'PENDIENTE_REVISION'">
              <Button
                type="button"
                icon="pi pi-check"
                severity="success"
                size="small"
                title="Aprobar abono"
                @click="$emit('validate', a, 'APROBAR')"
              />
              <Button
                type="button"
                icon="pi pi-eye"
                severity="warn"
                outlined
                size="small"
                title="Observar abono"
                @click="$emit('validate', a, 'OBSERVAR')"
              />
              <Button
                type="button"
                icon="pi pi-ban"
                severity="danger"
                outlined
                size="small"
                title="Rechazar abono"
                @click="$emit('validate', a, 'RECHAZAR')"
              />
            </template>
            <span v-else class="sga-cell-sub">
              {{ a.usuarioFinanzasNombre ? `Por ${a.usuarioFinanzasNombre}` : 'Validado' }}
            </span>
          </div>
        </template>
      </Column>
    </DataTable>

    <AbonosCanvasGrid
      v-else
      v-model:selection="selectedRow"
      :anticipos="filteredAnticipos"
      :loading="loading"
      @preview-voucher="$emit('preview-voucher', $event)"
    />
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
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import { formatDateTime } from '@/shared/utils/dateUtils'
import AbonosCanvasGrid from './AbonosCanvasGrid.vue'
import {
  formatPen,
  formatUsd,
  formatTipoAbono,
  getEstadoAnticipoLabel,
  getEstadoAnticipoSeverity
} from '../utils/finanzasFormatters'

const props = defineProps({
  anticipos: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  initialEstado: { type: String, default: 'PENDIENTE_REVISION' }
})

defineEmits(['refresh', 'preview-voucher', 'validate'])

const viewMode = ref('table')
const filterEstado = ref(props.initialEstado)
const searchQuery = ref('')
const selectedRow = ref(null)

watch(
  () => props.initialEstado,
  (val) => {
    if (val !== undefined) filterEstado.value = val
  }
)

const counts = computed(() => {
  const list = props.anticipos || []
  return {
    todos: list.length,
    pendientes: list.filter((a) => a.estadoPago === 'PENDIENTE_REVISION').length,
    aprobados: list.filter((a) => a.estadoPago === 'APROBADO').length,
    observados: list.filter((a) => a.estadoPago === 'OBSERVADO').length,
    rechazados: list.filter((a) => a.estadoPago === 'RECHAZADO').length
  }
})

const queueTabs = computed(() => [
  { label: 'Pendientes', value: 'PENDIENTE_REVISION', count: counts.value.pendientes, badgeSeverity: 'warn' },
  { label: 'Todos', value: '', count: counts.value.todos, badgeSeverity: 'secondary' },
  { label: 'Aprobados', value: 'APROBADO', count: counts.value.aprobados, badgeSeverity: 'success' },
  { label: 'Observados', value: 'OBSERVADO', count: counts.value.observados, badgeSeverity: 'danger' },
  { label: 'Rechazados', value: 'RECHAZADO', count: counts.value.rechazados, badgeSeverity: 'danger' }
])

const filteredAnticipos = computed(() => {
  let list = props.anticipos || []
  if (filterEstado.value) {
    list = list.filter((a) => a.estadoPago === filterEstado.value)
  }
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    list = list.filter(
      (a) =>
        (a.codigoCotizacion && a.codigoCotizacion.toLowerCase().includes(q)) ||
        (a.codigoPago && a.codigoPago.toLowerCase().includes(q)) ||
        (a.clienteRazonSocial && a.clienteRazonSocial.toLowerCase().includes(q)) ||
        (a.numeroOperacion && a.numeroOperacion.toLowerCase().includes(q)) ||
        (a.bancoDestino && a.bancoDestino.toLowerCase().includes(q))
    )
  }
  return list
})

const hasActiveFilters = computed(() => Boolean(searchQuery.value.trim()) || filterEstado.value !== 'PENDIENTE_REVISION')

const isPendingSelected = computed(() => selectedRow.value?.estadoPago === 'PENDIENTE_REVISION')

function resetFilters() {
  searchQuery.value = ''
  filterEstado.value = 'PENDIENTE_REVISION'
}
</script>

<style scoped>
.sga-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.35rem;
}
</style>
