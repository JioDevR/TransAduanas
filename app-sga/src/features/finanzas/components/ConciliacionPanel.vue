<template>
  <SgaListShell
    v-model:viewMode="viewMode"
    title="Facturación Electrónica & Conciliación Bancaria"
  >
    <template #header-right>
      <div class="sga-toolbar-actions">
        <Tag
          :value="`Facturado: ${formatPen(totalFacturadoPen)}`"
          severity="success"
          class="font-mono"
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
      <div class="sga-queue-buttons" role="tablist" aria-label="Colas de facturación y conciliación">
        <Button
          type="button"
          role="tab"
          :aria-selected="activeQueue === 'facturas'"
          label="Comprobantes Emitidos"
          icon="pi pi-file-pdf"
          :badge="String(facturas.length)"
          badgeSeverity="secondary"
          :severity="activeQueue === 'facturas' ? 'primary' : 'secondary'"
          :outlined="activeQueue !== 'facturas'"
          size="small"
          @click="activeQueue = 'facturas'"
        />
        <Button
          type="button"
          role="tab"
          :aria-selected="activeQueue === 'abonos-sin-factura'"
          label="Abonos por Facturar"
          icon="pi pi-receipt"
          :badge="String(abonosSinFactura.length)"
          :badgeSeverity="abonosSinFactura.length > 0 ? 'warn' : 'secondary'"
          :severity="activeQueue === 'abonos-sin-factura' ? 'primary' : 'secondary'"
          :outlined="activeQueue !== 'abonos-sin-factura'"
          size="small"
          @click="activeQueue = 'abonos-sin-factura'"
        />
        <Button
          type="button"
          role="tab"
          :aria-selected="activeQueue === 'por-conciliar'"
          label="Liquidaciones B2B por Conciliar"
          icon="pi pi-shield"
          :badge="String(liquidacionesEnConciliacion.length)"
          :badgeSeverity="liquidacionesEnConciliacion.length > 0 ? 'info' : 'secondary'"
          :severity="activeQueue === 'por-conciliar' ? 'primary' : 'secondary'"
          :outlined="activeQueue !== 'por-conciliar'"
          size="small"
          @click="activeQueue = 'por-conciliar'"
        />
      </div>
    </template>

    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchQuery"
          placeholder="Buscar por N° factura, referencia, cliente o RUC..."
          size="small"
          fluid
        />
      </IconField>

      <Select
        v-if="activeQueue === 'facturas'"
        v-model="filterEstado"
        :options="estadoOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        class="sga-filter-select"
      />

      <Select
        v-if="activeQueue === 'facturas'"
        v-model="filterOrigen"
        :options="origenOptions"
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
      <template v-if="activeQueue === 'facturas'">
        <Button
          type="button"
          label="Ver PDF"
          icon="pi pi-eye"
          severity="secondary"
          outlined
          size="small"
          :disabled="!selectedFactura"
          @click="$emit('preview-pdf', selectedFactura)"
        />
        <Button
          type="button"
          label="Descargar"
          icon="pi pi-download"
          size="small"
          :disabled="!selectedFactura"
          @click="$emit('download-pdf', selectedFactura)"
        />
        <Button
          v-if="selectedFactura?.estado === 'EMITIDA'"
          type="button"
          label="Anular"
          icon="pi pi-ban"
          severity="danger"
          outlined
          size="small"
          @click="$emit('anular-factura', selectedFactura)"
        />
      </template>
    </template>

    <!-- COLA 1: COMPROBANTES EMITIDOS -->
    <template v-if="activeQueue === 'facturas'">
      <DataTable
        v-if="viewMode === 'table'"
        v-model:selection="selectedFactura"
        :value="filteredFacturas"
        :loading="loading"
        selectionMode="single"
        dataKey="idComprobante"
        responsiveLayout="scroll"
        paginator
        :rows="12"
        :rowsPerPageOptions="[12, 25, 50]"
        currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} comprobantes"
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-file-pdf sga-empty-icon"></i>
            <p class="sga-empty-title">No hay facturas ni boletas emitidas</p>
            <p class="sga-empty-desc">
              Cada abono aprobado o liquidación cobrada genera su comprobante oficial aquí automáticamente.
            </p>
          </div>
        </template>

        <Column header="COMPROBANTE">
          <template #body="{ data: f }">
            <div class="sga-cell-stack">
              <span class="sga-code-cell font-mono">{{ f.numeroCompleto }}</span>
              <span class="sga-cell-sub">{{ f.tipoComprobante }} · {{ formatDateTime(f.fechaEmision) }}</span>
            </div>
          </template>
        </Column>

        <Column header="ORIGEN / REFERENCIA">
          <template #body="{ data: f }">
            <div class="sga-cell-stack">
              <Tag
                :value="f.origenTipo === 'LIQUIDACION_ADUANERA' ? 'Liquidación' : 'Abono Cotización'"
                :severity="f.origenTipo === 'LIQUIDACION_ADUANERA' ? 'info' : 'secondary'"
                class="sga-mini-tag"
              />
              <span class="sga-cell-sub font-mono">{{ f.codigoReferencia }}</span>
            </div>
          </template>
        </Column>

        <Column header="CLIENTE / RUC">
          <template #body="{ data: f }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ f.clienteRazonSocial }}</span>
              <span class="sga-cell-sub font-mono">Doc: {{ f.clienteNumeroDocumento }}</span>
            </div>
          </template>
        </Column>

        <Column header="BASE + IGV" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: f }">
            <div class="sga-cell-stack">
              <span class="sga-amount-cell font-mono">{{ formatMonto(f.subtotalGravado, f.moneda) }}</span>
              <span class="sga-cell-sub font-mono">IGV: {{ formatMonto(f.igvMonto, f.moneda) }}</span>
            </div>
          </template>
        </Column>

        <Column header="TOTAL" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: f }">
            <div class="sga-cell-stack">
              <span class="sga-amount-cell font-mono sga-text-success">
                {{ formatMonto(f.totalComprobante, f.moneda) }}
              </span>
              <span v-if="f.moneda === 'USD'" class="sga-cell-sub font-mono">
                {{ formatPen(f.totalEquivalentePen) }}
              </span>
            </div>
          </template>
        </Column>

        <Column header="ESTADO">
          <template #body="{ data: f }">
            <Tag :value="f.estado" :severity="f.estado === 'ANULADA' ? 'danger' : 'success'" rounded />
          </template>
        </Column>

        <Column header="ACCIONES" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: f }">
            <div class="sga-row-actions">
              <Button
                type="button"
                label="Ver PDF"
                icon="pi pi-eye"
                severity="secondary"
                outlined
                size="small"
                @click="$emit('preview-pdf', f)"
              />
              <Button
                type="button"
                icon="pi pi-download"
                size="small"
                title="Descargar PDF"
                @click="$emit('download-pdf', f)"
              />
              <Button
                v-if="f.estado === 'EMITIDA'"
                type="button"
                icon="pi pi-ban"
                severity="danger"
                outlined
                size="small"
                title="Anular comprobante"
                @click="$emit('anular-factura', f)"
              />
            </div>
          </template>
        </Column>
      </DataTable>

      <FacturasCanvasGrid
        v-else
        v-model:selection="selectedFactura"
        :facturas="filteredFacturas"
        :loading="loading"
        @preview-pdf="$emit('preview-pdf', $event)"
        @download-pdf="$emit('download-pdf', $event)"
      />
    </template>

    <!-- COLA 2: ABONOS APROBADOS SIN FACTURA -->
    <template v-else-if="activeQueue === 'abonos-sin-factura'">
      <DataTable
        :value="filteredAbonosPendientes"
        :loading="loading"
        dataKey="idPago"
        responsiveLayout="scroll"
        paginator
        :rows="12"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-check-circle sga-empty-icon sga-text-success"></i>
            <p class="sga-empty-title">Todos los abonos aprobados están facturados</p>
            <p class="sga-empty-desc">
              Cada vez que Finanzas aprueba un abono B2B o de caja, la factura se genera automáticamente.
            </p>
          </div>
        </template>

        <Column header="N° PAGO">
          <template #body="{ data: a }">
            <div class="sga-cell-stack">
              <span class="sga-code-cell font-mono">{{ a.codigoPago }}</span>
              <span class="sga-cell-sub font-mono">{{ a.codigoCotizacion }}</span>
            </div>
          </template>
        </Column>

        <Column header="CLIENTE">
          <template #body="{ data: a }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ a.clienteRazonSocial }}</span>
              <span class="sga-cell-sub font-mono">Doc: {{ a.clienteNumeroDocumento || '—' }}</span>
            </div>
          </template>
        </Column>

        <Column header="MONTO ABONADO" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: a }">
            <span class="sga-amount-cell font-mono sga-text-success">
              {{ formatMonto(a.montoAbonado, a.moneda) }}
            </span>
          </template>
        </Column>

        <Column header="OPERACIÓN">
          <template #body="{ data: a }">
            <span class="sga-cell-sub font-mono">{{ a.bancoDestino }} · {{ a.numeroOperacion }}</span>
          </template>
        </Column>

        <Column header="ACCIÓN" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: a }">
            <Button
              type="button"
              label="Generar Factura"
              icon="pi pi-file-plus"
              size="small"
              :loading="emittingId === a.idPago"
              @click="$emit('emitir-abono', a)"
            />
          </template>
        </Column>
      </DataTable>
    </template>

    <!-- COLA 3: LIQUIDACIONES B2B EN CONCILIACIÓN -->
    <template v-else>
      <DataTable
        :value="filteredConciliaciones"
        :loading="loading"
        dataKey="idLiquidacion"
        responsiveLayout="scroll"
        paginator
        :rows="12"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-verified sga-empty-icon sga-text-success"></i>
            <p class="sga-empty-title">Sin liquidaciones pendientes de conciliación</p>
            <p class="sga-empty-desc">
              Los pagos de liquidación registrados por clientes en el Portal B2B aparecerán aquí para cotejo.
            </p>
          </div>
        </template>

        <Column header="LIQUIDACIÓN">
          <template #body="{ data: l }">
            <div class="sga-cell-stack">
              <span class="sga-code-cell font-mono">{{ l.codigoLiquidacion }}</span>
              <span class="sga-cell-sub font-mono">{{ l.codigoExpediente }}</span>
            </div>
          </template>
        </Column>

        <Column header="CLIENTE">
          <template #body="{ data: l }">
            <span class="sga-cell-primary">{{ l.clienteRazonSocial }}</span>
          </template>
        </Column>

        <Column header="BANCO & OPERACIÓN">
          <template #body="{ data: l }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ l.bancoDestino || l.metodoPago }}</span>
              <span class="sga-cell-sub font-mono">Op: {{ l.numeroOperacion || '—' }}</span>
            </div>
          </template>
        </Column>

        <Column header="TOTAL (PEN)" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: l }">
            <span class="sga-amount-cell font-mono">{{ formatPen(l.totalGeneralPen) }}</span>
          </template>
        </Column>

        <Column header="ACCIONES" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: l }">
            <div class="sga-row-actions">
              <Button
                v-if="l.idDocumentoVoucher"
                type="button"
                label="Voucher"
                icon="pi pi-file-pdf"
                severity="secondary"
                outlined
                size="small"
                @click="$emit('preview-voucher-doc', l)"
              />
              <Button
                type="button"
                label="Conciliar & Facturar"
                icon="pi pi-check-circle"
                severity="success"
                size="small"
                @click="$emit('conciliar-liquidacion', l)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </template>
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
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import { formatDateTime } from '@/shared/utils/dateUtils'
import { round2 } from '../utils/finanzasCalculations'
import { formatPen, formatUsd } from '../utils/finanzasFormatters'
import FacturasCanvasGrid from './FacturasCanvasGrid.vue'

const props = defineProps({
  facturas: { type: Array, default: () => [] },
  abonosSinFactura: { type: Array, default: () => [] },
  liquidacionesEnConciliacion: { type: Array, default: () => [] },
  emittingId: { type: Number, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits([
  'refresh',
  'preview-pdf',
  'download-pdf',
  'anular-factura',
  'emitir-abono',
  'conciliar-liquidacion',
  'preview-voucher-doc'
])

const viewMode = ref('table')
const activeQueue = ref('facturas')
const searchQuery = ref('')
const filterEstado = ref('EMITIDA')
const filterOrigen = ref('')
const selectedFactura = ref(null)

const estadoOptions = [
  { label: 'Emitidas', value: 'EMITIDA' },
  { label: 'Todas', value: '' },
  { label: 'Anuladas', value: 'ANULADA' }
]

const origenOptions = [
  { label: 'Todos los orígenes', value: '' },
  { label: 'Abono Cotización', value: 'ABONO_COTIZACION' },
  { label: 'Liquidación Aduanera', value: 'LIQUIDACION_ADUANERA' }
]

const filteredFacturas = computed(() =>
  (props.facturas || []).filter((f) => {
    if (filterEstado.value && f.estado !== filterEstado.value) return false
    if (filterOrigen.value && f.origenTipo !== filterOrigen.value) return false
    const q = searchQuery.value.trim().toLowerCase()
    if (!q) return true
    return (
      (f.numeroCompleto && f.numeroCompleto.toLowerCase().includes(q)) ||
      (f.codigoReferencia && f.codigoReferencia.toLowerCase().includes(q)) ||
      (f.clienteRazonSocial && f.clienteRazonSocial.toLowerCase().includes(q)) ||
      (f.clienteNumeroDocumento && f.clienteNumeroDocumento.toLowerCase().includes(q))
    )
  })
)

const filteredAbonosPendientes = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return props.abonosSinFactura || []
  return (props.abonosSinFactura || []).filter(
    (a) =>
      (a.codigoPago && a.codigoPago.toLowerCase().includes(q)) ||
      (a.codigoCotizacion && a.codigoCotizacion.toLowerCase().includes(q)) ||
      (a.clienteRazonSocial && a.clienteRazonSocial.toLowerCase().includes(q))
  )
})

const filteredConciliaciones = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return props.liquidacionesEnConciliacion || []
  return (props.liquidacionesEnConciliacion || []).filter(
    (l) =>
      (l.codigoLiquidacion && l.codigoLiquidacion.toLowerCase().includes(q)) ||
      (l.codigoExpediente && l.codigoExpediente.toLowerCase().includes(q)) ||
      (l.clienteRazonSocial && l.clienteRazonSocial.toLowerCase().includes(q))
  )
})

const totalFacturadoPen = computed(() =>
  round2(
    (props.facturas || [])
      .filter((f) => f.estado === 'EMITIDA')
      .reduce((acc, f) => acc + Number(f.totalEquivalentePen || 0), 0)
  )
)

const hasActiveFilters = computed(
  () => Boolean(searchQuery.value.trim()) || filterEstado.value !== 'EMITIDA' || filterOrigen.value !== ''
)

function resetFilters() {
  searchQuery.value = ''
  filterEstado.value = 'EMITIDA'
  filterOrigen.value = ''
}

function formatMonto(val, moneda) {
  return String(moneda).toUpperCase() === 'USD' ? formatUsd(val) : formatPen(val)
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
