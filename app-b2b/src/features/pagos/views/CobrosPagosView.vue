<template>
  <div class="cobros-pagos-view fade-in">
    <!-- 1. BLOQUE PRIORITARIO (solo visible si existen comprobantes observados) -->
    <div v-if="anticiposObservados.length > 0" ref="observadosSectionRef">
      <ObservadosBandejaPanel
        :anticipos="anticiposObservados"
        @fix-payment="irASubsanarPago"
        @view-voucher="verComprobante"
      />
    </div>

    <!-- 2. BLOQUE PRINCIPAL: CUENTAS POR PAGAR (SALDOS PENDIENTES) -->
    <SgaListShell
      v-model:viewMode="viewModeObligaciones"
      :title="`Cuentas por Pagar (${obligaciones.length})`"
    >
      <template #header-right>
        <Button
          type="button"
          label="Registrar Comprobante ($ USD)"
          icon="pi pi-plus"
          size="small"
          @click="irARegistrarPago()"
        />
      </template>

      <template #filters>
        <IconField class="sga-filter-search">
          <InputIcon class="pi pi-search" />
          <InputText
            v-model.trim="filtroObligacionTexto"
            placeholder="Buscar por código, mercancía o DAM..."
            aria-label="Buscar obligaciones pendientes por código, mercancía o DAM"
            size="small"
            fluid
          />
        </IconField>

        <Select
          v-model="filtroTipoObligacion"
          :options="tipoObligacionOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          class="sga-filter-select"
          aria-label="Filtrar por tipo de obligación"
        />

        <Button
          v-if="hasActiveObligacionFilters"
          type="button"
          icon="pi pi-filter-slash"
          severity="secondary"
          outlined
          size="small"
          title="Limpiar filtros"
          aria-label="Limpiar filtros"
          @click="resetObligacionFilters"
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
          @click="cargarTodo"
        />
      </template>

      <DataTable
        v-if="viewModeObligaciones === 'table'"
        :value="obligacionesFiltradas"
        :loading="loading"
        dataKey="idReferencia"
        responsiveLayout="scroll"
        paginator
        :rows="8"
        :rowsPerPageOptions="[8, 15, 30]"
        currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} obligaciones"
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-check-circle sga-empty-icon sga-text-success"></i>
            <p class="sga-empty-title">No registra saldos pendientes de pago</p>
            <p class="sga-empty-desc">Todas sus operaciones y despachos se encuentran al día en USD.</p>
          </div>
        </template>

        <Column header="DOCUMENTO">
          <template #body="{ data: o }">
            <span class="sga-code-cell sga-mono">{{ o.codigo }}</span>
          </template>
        </Column>

        <Column header="TIPO">
          <template #body="{ data: o }">
            <Tag
              :value="getTipoObligacionMeta(o.tipoObligacion).label"
              :severity="getTipoObligacionMeta(o.tipoObligacion).severity"
              class="sga-mini-tag"
            />
          </template>
        </Column>

        <Column header="MERCANCÍA & OPERACIÓN">
          <template #body="{ data: o }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ o.descripcion || 'Operación de Comercio Exterior' }}</span>
              <span v-if="o.numeroDam" class="sga-cell-sub sga-mono">
                DAM: {{ o.numeroDam }}
              </span>
            </div>
          </template>
        </Column>

        <Column header="TOTAL CONTRATADO">
          <template #body="{ data: o }">
            <span class="sga-amount-cell sga-mono">$ {{ formatCurrency(o.totalContratadoUsd) }}</span>
          </template>
        </Column>

        <Column header="AMORTIZADO">
          <template #body="{ data: o }">
            <span class="sga-amount-cell sga-mono sga-text-success">
              $ {{ formatCurrency(o.totalAmortizadoUsd) }}
            </span>
          </template>
        </Column>

        <Column header="SALDO PENDIENTE">
          <template #body="{ data: o }">
            <span class="sga-amount-cell sga-mono sga-text-warn">
              $ {{ formatCurrency(o.saldoPendienteUsd) }}
            </span>
          </template>
        </Column>

        <Column header="ESTADO PAGO">
          <template #body="{ data: o }">
            <Tag
              :value="formatEstadoPago(o.estadoPago)"
              :severity="getPagoTagSeverity(o.estadoPago)"
              rounded
            />
          </template>
        </Column>

        <Column header="ACCIÓN">
          <template #body="{ data: o }">
            <Button
              v-if="o.estadoPago === 'OBSERVADO' || o.estadoPago === 'RECHAZADO'"
              type="button"
              label="Subsanar"
              icon="pi pi-pencil"
              severity="danger"
              size="small"
              @click="subsanarAbonoCotizacion(o)"
            />
            <Button
              v-else
              type="button"
              :label="o.tipoObligacion === 'COTIZACION' ? 'Abonar' : 'Amortizar'"
              :icon="o.tipoObligacion === 'COTIZACION' ? 'pi pi-wallet' : 'pi pi-credit-card'"
              size="small"
              @click="handlePayObligacion(o)"
            />
          </template>
        </Column>
      </DataTable>

      <template v-else>
        <ObligacionesCanvasGrid
          :obligaciones="paginatedObligaciones"
          :loading="loading"
          @pay="handlePayObligacion"
          @fix-payment="subsanarAbonoCotizacion"
        />

        <BasePagination
          v-if="obligacionesFiltradas.length > pageSize"
          v-model:currentPage="currentObligacionesPage"
          :total-pages="totalObligacionesPages"
          :total-items="obligacionesFiltradas.length"
          :start-index="startIndexObligaciones"
          :end-index="endIndexObligaciones"
          item-label="obligaciones"
        />
      </template>
    </SgaListShell>

    <!-- 3. BLOQUE INFERIOR: HISTORIAL DE COMPROBANTES -->
    <div ref="historialSectionRef">
      <SgaListShell
        v-model:viewMode="viewModeHistorial"
        :title="`Historial de Comprobantes (${anticipos.length})`"
      >
        <template #filters>
          <IconField class="sga-filter-search">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model.trim="filtroHistorialTexto"
              placeholder="Buscar por código de pago, cotización o N° operación..."
              aria-label="Buscar en historial por código de pago, cotización o número de operación"
              size="small"
              fluid
            />
          </IconField>

          <Select
            v-model="filtroHistorialEstado"
            :options="historialEstadoOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            class="sga-filter-select"
            aria-label="Filtrar por estado contable"
          />

          <Button
            v-if="hasActiveHistorialFilters"
            type="button"
            icon="pi pi-filter-slash"
            severity="secondary"
            outlined
            size="small"
            title="Limpiar filtros"
            aria-label="Limpiar filtros"
            @click="resetHistorialFilters"
          />
        </template>

        <DataTable
          v-if="viewModeHistorial === 'table'"
          :value="anticiposFiltrados"
          :loading="loading"
          dataKey="idPago"
          responsiveLayout="scroll"
          paginator
          :rows="8"
          :rowsPerPageOptions="[8, 15, 30]"
          currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} comprobantes"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        >
          <template #empty>
            <div class="sga-empty-state">
              <i class="pi pi-folder-open sga-empty-icon"></i>
              <p class="sga-empty-title">No se encontraron comprobantes</p>
              <p class="sga-empty-desc">
                No existen registros que coincidan con los filtros aplicados o aún no ha registrado abonos en USD.
              </p>
            </div>
          </template>

          <Column header="CÓDIGO PAGO">
            <template #body="{ data: p }">
              <span class="sga-code-cell sga-mono">{{ p.codigoPago }}</span>
            </template>
          </Column>

          <Column header="DOCUMENTO">
            <template #body="{ data: p }">
              <span class="sga-code-cell sga-mono">{{ p.codigoCotizacion || '—' }}</span>
            </template>
          </Column>

          <Column header="TIPO ABONO">
            <template #body="{ data: p }">
              <Tag :value="p.tipoAbono || 'ABONO'" severity="secondary" class="sga-mini-tag" />
            </template>
          </Column>

          <Column header="MONTO ABONADO">
            <template #body="{ data: p }">
              <span class="sga-amount-cell sga-mono">$ {{ formatCurrency(p.montoAbonado) }}</span>
            </template>
          </Column>

          <Column header="BANCO ORIGEN / DESTINO">
            <template #body="{ data: p }">
              <span class="sga-cell-muted">
                {{ p.bancoOrigen || 'Emisor' }} → {{ p.bancoDestino }}
              </span>
            </template>
          </Column>

          <Column header="N° OPERACIÓN">
            <template #body="{ data: p }">
              <span class="sga-code-cell sga-mono">{{ p.numeroOperacion }}</span>
            </template>
          </Column>

          <Column header="FECHA">
            <template #body="{ data: p }">
              <span class="sga-date-cell sga-mono">{{ p.fechaOperacion }}</span>
            </template>
          </Column>

          <Column header="ESTADO FINANZAS">
            <template #body="{ data: p }">
              <Tag
                :value="formatEstadoPago(p.estadoPago)"
                :severity="getPagoTagSeverity(p.estadoPago)"
                rounded
              />
            </template>
          </Column>

          <Column header="ACCIONES">
            <template #body="{ data: p }">
              <div class="b2b-table-actions">
                <Button
                  v-if="p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO'"
                  type="button"
                  label="Subsanar"
                  icon="pi pi-pencil"
                  severity="danger"
                  size="small"
                  @click="irASubsanarPago(p.idPago)"
                />
                <Button
                  v-if="p.idDocumentoComprobante"
                  type="button"
                  label="Voucher"
                  icon="pi pi-file"
                  severity="secondary"
                  outlined
                  size="small"
                  @click="verComprobante(p.idDocumentoComprobante)"
                />
              </div>
            </template>
          </Column>
        </DataTable>

        <template v-else>
          <HistorialVouchersCanvasGrid
            :anticipos="paginatedAnticipos"
            :loading="loading"
            @fix-payment="irASubsanarPago"
            @view-voucher="verComprobante"
          />

          <BasePagination
            v-if="anticiposFiltrados.length > pageSize"
            v-model:currentPage="currentHistorialPage"
            :total-pages="totalHistorialPages"
            :total-items="anticiposFiltrados.length"
            :start-index="startIndexHistorial"
            :end-index="endIndexHistorial"
            item-label="vouchers"
          />
        </template>
      </SgaListShell>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import BasePagination from '@/shared/components/BasePagination.vue'
import ObligacionesCanvasGrid from '../components/ObligacionesCanvasGrid.vue'
import HistorialVouchersCanvasGrid from '../components/HistorialVouchersCanvasGrid.vue'
import ObservadosBandejaPanel from '../components/ObservadosBandejaPanel.vue'
import { usePortalPagos } from '../composables/usePortalPagos'
import { getPagoTagSeverity, getTipoObligacionMeta } from '../utils/pagoFormUtils'
import { formatCurrency, formatEstadoPago } from '@/shared/utils/formatters'

const {
  obligaciones,
  anticipos,
  requestedSection,
  loading,
  filtroTipoObligacion,
  filtroObligacionTexto,
  filtroHistorialTexto,
  filtroHistorialEstado,
  cotizacionesPreExpediente,
  expedientesHeredados,
  hasActiveObligacionFilters,
  obligacionesFiltradas,
  anticiposObservados,
  hasActiveHistorialFilters,
  anticiposFiltrados,
  resetObligacionFilters,
  resetHistorialFilters,
  cargarTodo,
  subsanarAbonoCotizacion,
  irASubsanarPago,
  irARegistrarPago,
  verComprobante
} = usePortalPagos()

const viewModeObligaciones = ref('canvas')
const viewModeHistorial = ref('table')
const pageSize = ref(9)
const currentObligacionesPage = ref(1)
const currentHistorialPage = ref(1)

const observadosSectionRef = ref(null)
const historialSectionRef = ref(null)

const tipoObligacionOptions = computed(() => [
  { label: `Todos los Saldos (${obligaciones.value.length})`, value: 'TODOS' },
  { label: `Anticipos de Cotización (${cotizacionesPreExpediente.value.length})`, value: 'COTIZACION' },
  { label: `Saldos de Despacho DAM (${expedientesHeredados.value.length})`, value: 'EXPEDIENTE' }
])

const historialEstadoOptions = [
  { label: 'Todos los comprobantes', value: '' },
  { label: 'En Revisión Contable', value: 'PENDIENTE_REVISION' },
  { label: 'Validados / Conciliados', value: 'VALIDADO' },
  { label: 'Observados / Por Subsanar', value: 'OBSERVADO' }
]

const totalObligacionesPages = computed(() => Math.ceil(obligacionesFiltradas.value.length / pageSize.value) || 1)
const startIndexObligaciones = computed(() => (obligacionesFiltradas.value.length === 0 ? 0 : (currentObligacionesPage.value - 1) * pageSize.value + 1))
const endIndexObligaciones = computed(() => Math.min(currentObligacionesPage.value * pageSize.value, obligacionesFiltradas.value.length))
const paginatedObligaciones = computed(() => {
  const start = (currentObligacionesPage.value - 1) * pageSize.value
  return obligacionesFiltradas.value.slice(start, start + pageSize.value)
})

const totalHistorialPages = computed(() => Math.ceil(anticiposFiltrados.value.length / pageSize.value) || 1)
const startIndexHistorial = computed(() => (anticiposFiltrados.value.length === 0 ? 0 : (currentHistorialPage.value - 1) * pageSize.value + 1))
const endIndexHistorial = computed(() => Math.min(currentHistorialPage.value * pageSize.value, anticiposFiltrados.value.length))
const paginatedAnticipos = computed(() => {
  const start = (currentHistorialPage.value - 1) * pageSize.value
  return anticiposFiltrados.value.slice(start, start + pageSize.value)
})

watch([filtroTipoObligacion, filtroObligacionTexto], () => {
  currentObligacionesPage.value = 1
})

watch([filtroHistorialTexto, filtroHistorialEstado], () => {
  currentHistorialPage.value = 1
})

watch(requestedSection, async (section) => {
  if (!section) return
  await nextTick()
  const target = section === 'observados' ? observadosSectionRef.value : historialSectionRef.value
  if (target && typeof target.scrollIntoView === 'function') {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
  requestedSection.value = null
})

function handlePayObligacion(o) {
  const tipo = o.tipoObligacion === 'COTIZACION' ? 'cotizacion' : 'expediente'
  const id = o.idCotizacion || o.idExpediente || o.idReferencia
  irARegistrarPago(tipo, id)
}

onMounted(() => {
  cargarTodo()
})
</script>

<style scoped>
.cobros-pagos-view {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  width: 100%;
  font-family: var(--font-sans);
}

.b2b-table-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  white-space: nowrap;
}
</style>
