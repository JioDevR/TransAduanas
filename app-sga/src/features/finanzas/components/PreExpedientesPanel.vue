<template>
  <SgaListShell v-model:viewMode="viewMode" title="Cartera de Cotizaciones Aprobadas">
    <template #header-right>
      <div class="sga-toolbar-actions">
        <Button
          v-if="anticiposPendientesCount > 0"
          type="button"
          :label="`Validar Vouchers (${anticiposPendientesCount})`"
          icon="pi pi-shield"
          severity="warn"
          outlined
          size="small"
          @click="$emit('go-abonos-b2b')"
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
        <Button
          type="button"
          label="Registrar Abono"
          icon="pi pi-plus"
          size="small"
          @click="$emit('registrar-abono', null)"
        />
      </div>
    </template>

    <template #sub-bar>
      <div class="sga-queue-buttons" role="tablist" aria-label="Filtro de cartera pre-expediente">
        <Button
          v-for="q in queueTabs"
          :key="q.value"
          type="button"
          role="tab"
          :aria-selected="filterEstado === q.value"
          :label="q.label"
          :badge="String(q.count)"
          :badgeSeverity="q.badgeSeverity"
          :severity="filterEstado === q.value ? 'primary' : 'secondary'"
          :outlined="filterEstado !== q.value"
          size="small"
          @click="filterEstado = q.value"
        />
      </div>
    </template>

    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchQuery"
          placeholder="Buscar por código, RUC, cliente, régimen o mercancía..."
          size="small"
          fluid
        />
      </IconField>

      <Tag
        :value="`Saldo por cobrar: ${formatUsd(carteraSummary.totalSaldoPendienteUsd)}`"
        severity="warn"
        class="font-mono"
        rounded
      />
      <Tag
        :value="`Listas p/ apertura: ${carteraSummary.habilitadasCount}`"
        severity="success"
        class="font-mono"
        rounded
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
        label="Ver Abonos"
        icon="pi pi-list"
        severity="secondary"
        outlined
        size="small"
        :disabled="!selectedRow"
        @click="$emit('ver-historial', selectedRow)"
      />
      <Button
        type="button"
        label="Agregar Abono"
        icon="pi pi-wallet"
        size="small"
        :disabled="!selectedRow"
        @click="$emit('registrar-abono', selectedRow)"
      />
    </template>

    <DataTable
      v-if="viewMode === 'table'"
      v-model:selection="selectedRow"
      :value="filteredCotizaciones"
      :loading="loading"
      selectionMode="single"
      dataKey="idCotizacion"
      responsiveLayout="scroll"
      paginator
      :rows="12"
      :rowsPerPageOptions="[12, 25, 50]"
      currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} cotizaciones"
      paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
    >
      <template #empty>
        <div class="sga-empty-state">
          <i class="pi pi-folder-open sga-empty-icon"></i>
          <p class="sga-empty-title">No se encontraron cotizaciones con este criterio</p>
          <p class="sga-empty-desc">
            Las cotizaciones comerciales con aprobación técnica aparecerán aquí para control y cobranza de anticipos.
          </p>
        </div>
      </template>

      <Column header="COTIZACIÓN">
        <template #body="{ data: c }">
          <div class="sga-cell-stack">
            <span class="sga-code-cell font-mono">{{ c.codigoCotizacion }}</span>
            <span class="sga-cell-sub font-mono">{{ formatDate(c.fechaEmision) }}</span>
          </div>
        </template>
      </Column>

      <Column header="CLIENTE IMPORTADOR">
        <template #body="{ data: c }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ c.clienteRazonSocial || 'Cliente no asignado' }}</span>
            <span class="sga-cell-sub font-mono">RUC/Doc: {{ c.clienteNumeroDocumento || '—' }}</span>
          </div>
        </template>
      </Column>

      <Column header="RÉGIMEN & CARGA">
        <template #body="{ data: c }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ formatRegimen(c.regimenAduanero) }}</span>
            <span class="sga-cell-sub">{{ c.mercanciaNombre || c.tipoCargaNombre || 'Carga General' }}</span>
          </div>
        </template>
      </Column>

      <Column header="TOTAL COTIZADO" headerClass="text-right" bodyClass="text-right">
        <template #body="{ data: c }">
          <span class="sga-amount-cell font-mono">{{ formatUsd(c.totalCotizado) }}</span>
        </template>
      </Column>

      <Column header="ABONADO & COBERTURA">
        <template #body="{ data: c }">
          <div class="sga-cov-cell">
            <span class="sga-amount-cell font-mono sga-text-success">{{ formatUsd(c.totalAbonado) }}</span>
            <ProgressBar :value="getCoberturaPct(c)" :showValue="false" class="sga-mini-progress" />
            <span class="sga-cell-sub font-mono">{{ getCoberturaPct(c) }}% cubierto</span>
          </div>
        </template>
      </Column>

      <Column header="SALDO PENDIENTE" headerClass="text-right" bodyClass="text-right">
        <template #body="{ data: c }">
          <div class="sga-cell-stack">
            <span
              class="sga-amount-cell font-mono"
              :class="getSaldo(c) > 0 ? 'sga-text-warn' : 'sga-text-success'"
            >
              {{ formatUsd(getSaldo(c)) }}
            </span>
            <span class="sga-cell-sub font-mono">≈ {{ formatPen(getSaldo(c) * tipoCambio) }}</span>
          </div>
        </template>
      </Column>

      <Column header="ESTADO FINANZAS">
        <template #body="{ data: c }">
          <Tag
            :value="formatEstadoPagoCotizacionLabel(c.estadoPago)"
            :severity="formatEstadoPagoCotizacionSeverity(c.estadoPago)"
            rounded
          />
        </template>
      </Column>

      <Column header="EXPEDIENTE">
        <template #body="{ data: c }">
          <Tag v-if="c.tieneExpediente" :value="c.codigoExpediente" severity="info" rounded />
          <Tag v-else-if="c.habilitadoParaExpediente" value="Listo Apertura" severity="success" rounded />
          <Tag v-else value="Requiere Anticipo" severity="warn" rounded />
        </template>
      </Column>

      <Column header="ACCIONES" headerClass="text-right" bodyClass="text-right">
        <template #body="{ data: c }">
          <div class="sga-row-actions">
            <Button
              type="button"
              label="Abonar"
              icon="pi pi-wallet"
              size="small"
              @click="$emit('registrar-abono', c)"
            />
            <Button
              type="button"
              label="Abonos"
              icon="pi pi-list"
              severity="secondary"
              outlined
              size="small"
              @click="$emit('ver-historial', c)"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <CotizacionesCobroCanvasGrid
      v-else
      v-model:selection="selectedRow"
      :cotizaciones="filteredCotizaciones"
      :loading="loading"
      @registrar-abono="$emit('registrar-abono', $event)"
      @ver-historial="$emit('ver-historial', $event)"
    />
  </SgaListShell>
</template>

<script setup>
import { ref, computed } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import ProgressBar from 'primevue/progressbar'
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import { formatDate } from '@/shared/utils/dateUtils'
import { formatRegimen } from '@/shared/utils/formatters'
import { summarizeCotizacionesCartera } from '../utils/finanzasCalculations'
import {
  formatPen,
  formatUsd,
  formatEstadoPagoCotizacionLabel,
  formatEstadoPagoCotizacionSeverity
} from '../utils/finanzasFormatters'
import CotizacionesCobroCanvasGrid from './CotizacionesCobroCanvasGrid.vue'

const props = defineProps({
  cotizaciones: { type: Array, default: () => [] },
  anticiposPendientesCount: { type: Number, default: 0 },
  tipoCambio: { type: Number, default: 3.75 },
  loading: { type: Boolean, default: false }
})

defineEmits(['refresh', 'registrar-abono', 'ver-historial', 'go-abonos-b2b'])

const viewMode = ref('table')
const filterEstado = ref('')
const searchQuery = ref('')
const selectedRow = ref(null)

const carteraSummary = computed(() => summarizeCotizacionesCartera(props.cotizaciones))

const queueTabs = computed(() => [
  { label: 'Todas', value: '', count: props.cotizaciones.length, badgeSeverity: 'secondary' },
  { label: 'Con Saldo', value: 'PENDIENTES', count: carteraSummary.value.conSaldoPendienteCount, badgeSeverity: 'warn' },
  { label: 'En Revisión', value: 'EN_REVISION', count: carteraSummary.value.enRevisionCount, badgeSeverity: 'warn' },
  { label: 'Adelanto Conforme', value: 'HABILITADAS', count: carteraSummary.value.adelantoConformeCount, badgeSeverity: 'success' },
  { label: '100% Canceladas', value: 'PAGADAS', count: carteraSummary.value.pagadoTotalCount, badgeSeverity: 'success' }
])

const getSaldo = (c) => Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0)

const getCoberturaPct = (c) => {
  const total = Number(c.totalCotizado || 0)
  if (!total) return 0
  return Math.min(100, Math.round((Number(c.totalAbonado || 0) / total) * 100))
}

const filteredCotizaciones = computed(() =>
  (props.cotizaciones || []).filter((c) => {
    const saldo = getSaldo(c)
    if (filterEstado.value === 'PENDIENTES' && saldo <= 0.01) return false
    if (filterEstado.value === 'EN_REVISION' && c.estadoPago !== 'PENDIENTE_REVISION') return false
    if (filterEstado.value === 'HABILITADAS' && !(c.estadoPago === 'ADELANTO_APROBADO' || c.tienePagoAprobado)) return false
    if (filterEstado.value === 'PAGADAS' && c.estadoPago !== 'PAGADO_TOTAL') return false

    const q = searchQuery.value.trim().toLowerCase()
    if (!q) return true
    return (
      (c.codigoCotizacion && c.codigoCotizacion.toLowerCase().includes(q)) ||
      (c.clienteRazonSocial && c.clienteRazonSocial.toLowerCase().includes(q)) ||
      (c.clienteNumeroDocumento && c.clienteNumeroDocumento.toLowerCase().includes(q)) ||
      (c.regimenAduanero && c.regimenAduanero.toLowerCase().includes(q)) ||
      (c.mercanciaNombre && c.mercanciaNombre.toLowerCase().includes(q))
    )
  })
)

const hasActiveFilters = computed(() => Boolean(searchQuery.value.trim()) || filterEstado.value !== '')

function resetFilters() {
  searchQuery.value = ''
  filterEstado.value = ''
}
</script>

<style scoped>
.sga-cov-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 110px;
}

.sga-mini-progress {
  height: 5px;
}

.sga-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.35rem;
}
</style>
