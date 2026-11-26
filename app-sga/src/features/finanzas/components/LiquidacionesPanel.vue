<template>
  <SgaListShell
    v-model:viewMode="viewMode"
    :title="subTab === 'emitidas' ? 'Bandeja de Liquidaciones Aduaneras' : 'Expedientes Pendientes de Liquidación'"
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
          label="Emitir Liquidación"
          icon="pi pi-plus"
          size="small"
          @click="$emit('emitir', null)"
        />
      </div>
    </template>

    <template #sub-bar>
      <div class="sga-queue-buttons" role="tablist" aria-label="Vista de liquidaciones">
        <Button
          type="button"
          role="tab"
          :aria-selected="subTab === 'emitidas'"
          label="Liquidaciones Emitidas"
          icon="pi pi-receipt"
          :badge="String(liquidaciones.length)"
          badgeSeverity="secondary"
          :severity="subTab === 'emitidas' ? 'primary' : 'secondary'"
          :outlined="subTab !== 'emitidas'"
          size="small"
          @click="$emit('update:subTab', 'emitidas')"
        />
        <Button
          type="button"
          role="tab"
          :aria-selected="subTab === 'por-liquidar'"
          label="Expedientes por Liquidar"
          icon="pi pi-clock"
          :badge="String(expedientesPorLiquidar.length)"
          :badgeSeverity="expedientesPorLiquidar.length > 0 ? 'warn' : 'secondary'"
          :severity="subTab === 'por-liquidar' ? 'primary' : 'secondary'"
          :outlined="subTab !== 'por-liquidar'"
          size="small"
          @click="$emit('update:subTab', 'por-liquidar')"
        />
      </div>
    </template>

    <template #filters>
      <IconField class="sga-filter-search">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchQuery"
          :placeholder="
            subTab === 'emitidas'
              ? 'Buscar por código, expediente, DAM, factura o cliente...'
              : 'Buscar expediente por código, DAM o cliente...'
          "
          size="small"
          fluid
        />
      </IconField>

      <Select
        v-if="subTab === 'emitidas'"
        v-model="filterEstado"
        :options="estadoOptions"
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
      <template v-if="subTab === 'emitidas'">
        <Button
          type="button"
          label="Ver Detalle"
          icon="pi pi-eye"
          severity="secondary"
          outlined
          size="small"
          :disabled="!selectedRow"
          @click="$emit('detalle', selectedRow)"
        />
        <Button
          v-if="isPendingSelected"
          type="button"
          label="Editar"
          icon="pi pi-pencil"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('editar', selectedRow)"
        />
        <Button
          v-if="canPaySelected"
          type="button"
          :label="selectedRow?.estadoPago === 'EN_CONCILIACION' ? 'Conciliar Cobro' : 'Registrar Cobro'"
          icon="pi pi-dollar"
          severity="success"
          size="small"
          @click="$emit('pagar', selectedRow)"
        />
        <Button
          type="button"
          label="Docs"
          icon="pi pi-folder-open"
          severity="secondary"
          outlined
          size="small"
          :disabled="!selectedRow?.idExpediente"
          @click="$emit('open-docs', selectedRow)"
        />
        <Button
          v-if="isPendingSelected"
          type="button"
          label="Anular"
          icon="pi pi-trash"
          severity="danger"
          outlined
          size="small"
          @click="$emit('anular', selectedRow)"
        />
      </template>

      <template v-else>
        <Button
          type="button"
          label="Emitir Liquidación"
          icon="pi pi-receipt"
          size="small"
          :disabled="!selectedExpediente"
          @click="$emit('emitir', selectedExpediente)"
        />
      </template>
    </template>

    <!-- SUB-TAB 1: LIQUIDACIONES EMITIDAS -->
    <template v-if="subTab === 'emitidas'">
      <DataTable
        v-if="viewMode === 'table'"
        v-model:selection="selectedRow"
        :value="filteredLiquidaciones"
        :loading="loading"
        selectionMode="single"
        dataKey="idLiquidacion"
        responsiveLayout="scroll"
        paginator
        :rows="12"
        :rowsPerPageOptions="[12, 25, 50]"
        currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} liquidaciones"
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-receipt sga-empty-icon"></i>
            <p class="sga-empty-title">No hay liquidaciones registradas</p>
            <p class="sga-empty-desc">Emita una liquidación aduanera vinculada a un expediente operativo.</p>
          </div>
        </template>

        <Column header="N° LIQUIDACIÓN">
          <template #body="{ data: l }">
            <span class="sga-code-cell font-mono">{{ l.codigoLiquidacion }}</span>
          </template>
        </Column>

        <Column header="EXPEDIENTE / DAM">
          <template #body="{ data: l }">
            <div class="sga-cell-stack">
              <span class="sga-code-cell font-mono">{{ l.codigoExpediente || 'EXP-SGA' }}</span>
              <div class="sga-cell-meta">
                <span class="sga-cell-sub font-mono">DAM: {{ l.numeroDam || 'S/N' }}</span>
                <Tag
                  v-if="l.canalSunat"
                  :value="l.canalSunat"
                  :severity="getCanalSeverity(l.canalSunat)"
                  class="sga-mini-tag"
                  rounded
                />
              </div>
            </div>
          </template>
        </Column>

        <Column header="CLIENTE IMPORTADOR">
          <template #body="{ data: l }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ l.clienteRazonSocial || 'Cliente no asignado' }}</span>
              <span class="sga-cell-sub font-mono">RUC: {{ l.clienteNumeroDocumento || '—' }}</span>
            </div>
          </template>
        </Column>

        <Column header="TRIBUTOS SUNAT" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: l }">
            <span class="sga-amount-cell font-mono">{{ formatPen(l.totalTributosSunat) }}</span>
          </template>
        </Column>

        <Column header="SERVICIOS + IGV" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: l }">
            <div class="sga-cell-stack">
              <span class="sga-amount-cell font-mono">
                {{ formatPen(Number(l.subtotalImponible || 0) + Number(l.igvMonto || 0)) }}
              </span>
              <span v-if="l.aplicaDetraccion && Number(l.montoDetraccion) > 0" class="sga-cell-sub font-mono">
                SPOT: {{ formatPen(l.montoDetraccion) }}
              </span>
            </div>
          </template>
        </Column>

        <Column header="TOTAL NETO" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: l }">
            <div class="sga-cell-stack">
              <span class="sga-amount-cell font-mono">{{ formatPen(l.totalGeneralPen) }}</span>
              <span class="sga-cell-sub font-mono">{{ formatUsd(l.totalGeneralUsd) }}</span>
            </div>
          </template>
        </Column>

        <Column header="ESTADO">
          <template #body="{ data: l }">
            <Tag
              :value="formatEstadoLiquidacion(l.estadoPago)"
              :severity="getEstadoLiquidacionSeverity(l.estadoPago)"
              rounded
            />
          </template>
        </Column>

        <Column header="ACCIONES" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: l }">
            <div class="sga-row-actions">
              <Button
                v-if="l.estadoPago === 'PENDIENTE' || l.estadoPago === 'EN_CONCILIACION'"
                type="button"
                :label="l.estadoPago === 'EN_CONCILIACION' ? 'Conciliar' : 'Cobrar'"
                icon="pi pi-dollar"
                severity="success"
                size="small"
                @click="$emit('pagar', l)"
              />
              <Button
                v-if="l.estadoPago === 'PENDIENTE'"
                type="button"
                icon="pi pi-pencil"
                severity="secondary"
                outlined
                size="small"
                title="Editar liquidación"
                @click="$emit('editar', l)"
              />
              <Button
                type="button"
                icon="pi pi-eye"
                severity="secondary"
                outlined
                size="small"
                title="Ver detalle e imprimir"
                @click="$emit('detalle', l)"
              />
              <Button
                type="button"
                icon="pi pi-folder-open"
                severity="secondary"
                outlined
                size="small"
                title="Documentos del expediente"
                :disabled="!l.idExpediente"
                @click="$emit('open-docs', l)"
              />
              <Button
                v-if="l.estadoPago === 'PENDIENTE'"
                type="button"
                icon="pi pi-trash"
                severity="danger"
                outlined
                size="small"
                title="Anular liquidación"
                @click="$emit('anular', l)"
              />
            </div>
          </template>
        </Column>
      </DataTable>

      <LiquidacionesCanvasGrid
        v-else
        v-model:selection="selectedRow"
        :liquidaciones="filteredLiquidaciones"
        :loading="loading"
        @detalle="$emit('detalle', $event)"
        @pagar="$emit('pagar', $event)"
      />
    </template>

    <!-- SUB-TAB 2: EXPEDIENTES POR LIQUIDAR -->
    <template v-else>
      <DataTable
        v-if="viewMode === 'table'"
        v-model:selection="selectedExpediente"
        :value="filteredExpedientes"
        :loading="loading"
        selectionMode="single"
        dataKey="idExpediente"
        responsiveLayout="scroll"
        paginator
        :rows="12"
        :rowsPerPageOptions="[12, 25, 50]"
        currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} expedientes"
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-check-circle sga-empty-icon sga-text-success"></i>
            <p class="sga-empty-title">Todos los expedientes cuentan con liquidación</p>
            <p class="sga-empty-desc">No hay expedientes pendientes de emisión financiera en este momento.</p>
          </div>
        </template>

        <Column header="EXPEDIENTE">
          <template #body="{ data: e }">
            <span class="sga-code-cell font-mono">{{ e.codigoExpediente }}</span>
          </template>
        </Column>

        <Column header="CLIENTE IMPORTADOR">
          <template #body="{ data: e }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ e.clienteRazonSocial || 'Cliente no asignado' }}</span>
              <span class="sga-cell-sub font-mono">Doc: {{ e.clienteNumeroDocumento || '—' }}</span>
            </div>
          </template>
        </Column>

        <Column header="DAM & CANAL SUNAT">
          <template #body="{ data: e }">
            <div class="sga-cell-meta">
              <span class="sga-code-cell font-mono">{{ e.numeroDam || 'S/N' }}</span>
              <Tag
                v-if="e.canalSunat"
                :value="e.canalSunat"
                :severity="getCanalSeverity(e.canalSunat)"
                rounded
              />
            </div>
          </template>
        </Column>

        <Column header="ESTADO OPERATIVO">
          <template #body="{ data: e }">
            <Tag :value="formatEstadoLiquidacion(e.estado)" :severity="getEstadoLiquidacionSeverity(e.estado)" rounded />
          </template>
        </Column>

        <Column header="ACCIÓN" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: e }">
            <Button
              type="button"
              label="Emitir Liquidación"
              icon="pi pi-receipt"
              size="small"
              @click="$emit('emitir', e)"
            />
          </template>
        </Column>
      </DataTable>

      <div v-else class="sga-canvas-wrapper">
        <div v-if="filteredExpedientes.length === 0" class="sga-canvas-state">
          <i class="pi pi-check-circle sga-state-icon sga-text-success"></i>
          <p class="sga-state-title">Sin expedientes por liquidar</p>
        </div>
        <div v-else class="sga-canvas-grid">
          <article
            v-for="e in filteredExpedientes"
            :key="e.idExpediente"
            :class="['sga-canvas-card', { 'is-selected': selectedExpediente?.idExpediente === e.idExpediente }]"
            @click="selectedExpediente = e"
          >
            <header class="sga-card-top">
              <span class="sga-card-code font-mono">{{ e.codigoExpediente }}</span>
              <Tag
                v-if="e.canalSunat"
                :value="e.canalSunat"
                :severity="getCanalSeverity(e.canalSunat)"
                rounded
              />
            </header>
            <div class="sga-card-client">
              <h4 class="sga-client-title">{{ e.clienteRazonSocial || 'Cliente' }}</h4>
              <span class="sga-client-doc font-mono">DAM: {{ e.numeroDam || 'S/N' }}</span>
            </div>
            <footer class="sga-card-footer">
              <Tag :value="formatEstadoLiquidacion(e.estado)" severity="info" rounded />
              <Button
                type="button"
                label="Emitir"
                icon="pi pi-receipt"
                size="small"
                @click.stop="$emit('emitir', e)"
              />
            </footer>
          </article>
        </div>
      </div>
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
import { getCanalSeverity } from '@/shared/utils/formatters'
import {
  formatPen,
  formatUsd,
  formatEstadoLiquidacion,
  getEstadoLiquidacionSeverity
} from '../utils/finanzasFormatters'
import LiquidacionesCanvasGrid from './LiquidacionesCanvasGrid.vue'

const props = defineProps({
  liquidaciones: { type: Array, default: () => [] },
  expedientesPorLiquidar: { type: Array, default: () => [] },
  subTab: { type: String, default: 'emitidas' },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:subTab', 'refresh', 'emitir', 'editar', 'pagar', 'detalle', 'open-docs', 'anular'])

const viewMode = ref('table')
const filterEstado = ref('')
const searchQuery = ref('')
const selectedRow = ref(null)
const selectedExpediente = ref(null)

const estadoOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Pendientes de cobro', value: 'PENDIENTE' },
  { label: 'En Conciliación (B2B)', value: 'EN_CONCILIACION' },
  { label: 'Cobrados / Pagados', value: 'PAGADO' },
  { label: 'Anulados', value: 'ANULADO' }
]

const filteredLiquidaciones = computed(() =>
  (props.liquidaciones || []).filter((l) => {
    if (filterEstado.value && l.estadoPago !== filterEstado.value) return false
    const q = searchQuery.value.trim().toLowerCase()
    if (!q) return true
    return (
      (l.codigoLiquidacion && l.codigoLiquidacion.toLowerCase().includes(q)) ||
      (l.codigoExpediente && l.codigoExpediente.toLowerCase().includes(q)) ||
      (l.numeroDam && l.numeroDam.toLowerCase().includes(q)) ||
      (l.clienteRazonSocial && l.clienteRazonSocial.toLowerCase().includes(q)) ||
      (l.numeroFactura && l.numeroFactura.toLowerCase().includes(q))
    )
  })
)

const filteredExpedientes = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return props.expedientesPorLiquidar || []
  return (props.expedientesPorLiquidar || []).filter(
    (e) =>
      (e.codigoExpediente && e.codigoExpediente.toLowerCase().includes(q)) ||
      (e.numeroDam && e.numeroDam.toLowerCase().includes(q)) ||
      (e.clienteRazonSocial && e.clienteRazonSocial.toLowerCase().includes(q)) ||
      (e.clienteNumeroDocumento && e.clienteNumeroDocumento.toLowerCase().includes(q))
  )
})

const hasActiveFilters = computed(() => Boolean(searchQuery.value.trim()) || filterEstado.value !== '')
const isPendingSelected = computed(() => selectedRow.value?.estadoPago === 'PENDIENTE')
const canPaySelected = computed(() =>
  selectedRow.value?.estadoPago === 'PENDIENTE' || selectedRow.value?.estadoPago === 'EN_CONCILIACION'
)

function resetFilters() {
  searchQuery.value = ''
  filterEstado.value = ''
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
