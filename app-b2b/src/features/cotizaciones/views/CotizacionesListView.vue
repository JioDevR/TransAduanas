<template>
  <div class="cotizaciones-view fade-in">
    <!-- Wizard Split-Screen de Solicitud de Cotización -->
    <PortalCotizacionForm
      v-if="showSolicitudForm"
      @cancel="showSolicitudForm = false"
      @saved="handleSolicitudSaved"
    />

    <!-- Listado Dual (Canvas / Tabla) sgaUI -->
    <SgaListShell v-else v-model:viewMode="viewMode">
        <template #header-right>
          <Button
            type="button"
            label="Solicitar Nueva Cotización"
            icon="pi pi-plus"
            size="small"
            @click="showSolicitudForm = true"
          />
        </template>

        <template #filters>
          <IconField class="sga-filter-search">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model.trim="filtroTexto"
              placeholder="Buscar por código, mercancía o puerto..."
              aria-label="Buscar cotizaciones por código, mercancía o puerto"
              size="small"
              fluid
            />
          </IconField>

          <Select
            v-model="filtroRegimen"
            :options="regimenOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            class="sga-filter-select"
            aria-label="Filtrar por régimen"
          />

          <Select
            v-model="filtroEstado"
            :options="estadoOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            class="sga-filter-select"
            aria-label="Filtrar por estado"
          />

          <Select
            v-model="ordenamiento"
            :options="sortOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            class="sga-filter-select sga-sort-select"
            aria-label="Ordenar cotizaciones"
          />

          <Button
            v-if="hasActiveFilters"
            type="button"
            icon="pi pi-filter-slash"
            severity="secondary"
            outlined
            size="small"
            title="Limpiar filtros"
            aria-label="Limpiar filtros"
            @click="resetFilters"
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
            @click="cargarCotizaciones"
          />
        </template>

        <!-- Vista Tabla PrimeVue -->
        <DataTable
          v-if="viewMode === 'table'"
          :value="cotizacionesOrdenadas"
          :loading="loading"
          dataKey="idCotizacion"
          responsiveLayout="scroll"
          paginator
          :rows="10"
          :rowsPerPageOptions="[10, 20, 50]"
          currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} cotizaciones"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
          @row-dblclick="({ data }) => verDetalle(data)"
        >
          <template #empty>
            <div class="sga-empty-state">
              <i class="pi pi-folder-open sga-empty-icon"></i>
              <p class="sga-empty-title">No se encontraron cotizaciones</p>
              <p class="sga-empty-desc">
                No hay registros que coincidan con los filtros seleccionados o aún no ha solicitado cotizaciones.
              </p>
            </div>
          </template>

          <Column header="CÓDIGO">
            <template #body="{ data: c }">
              <span class="sga-code-cell sga-mono">{{ c.codigoCotizacion }}</span>
            </template>
          </Column>

          <Column header="MERCANCÍA & CARGA">
            <template #body="{ data: c }">
              <div class="sga-cell-stack">
                <span class="sga-cell-primary">{{ c.nombreMercancia || 'Mercancía General' }}</span>
                <span class="sga-cell-sub sga-mono">
                  {{ c.codigoMercancia || 'Sin partida' }} • {{ c.nombreTipoCarga || 'General' }}
                </span>
              </div>
            </template>
          </Column>

          <Column header="RÉGIMEN">
            <template #body="{ data: c }">
              <Tag :value="formatRegimen(c.regimenAduanero) || 'Aduana'" severity="secondary" class="sga-mini-tag" />
            </template>
          </Column>

          <Column header="RUTA LOGÍSTICA">
            <template #body="{ data: c }">
              <span class="sga-cell-muted">
                {{ c.nombrePuertoEnvio || 'Origen' }} → {{ c.nombrePuertoDestino || 'Destino' }}
              </span>
            </template>
          </Column>

          <Column header="VALOR FOB">
            <template #body="{ data: c }">
              <span class="sga-amount-cell sga-mono">$ {{ formatCurrency(c.valorFob) }}</span>
            </template>
          </Column>

          <Column header="TOTAL ESTIMADO">
            <template #body="{ data: c }">
              <Tag
                v-if="esPendienteLiquidacion(c)"
                value="Por cotizar"
                severity="warn"
                rounded
              />
              <span v-else class="sga-amount-cell sga-mono">$ {{ formatCurrency(c.totalCotizado) }}</span>
            </template>
          </Column>

          <Column header="ESTADO">
            <template #body="{ data: c }">
              <Tag
                :value="formatEstadoCotizacion(c.estado)"
                :severity="getEstadoSeverity(c.estado)"
                rounded
              />
            </template>
          </Column>

          <Column header="ESTADO PAGO">
            <template #body="{ data: c }">
              <Tag
                :value="formatPago(c.estadoPago)"
                :severity="getPagoSeverity(c.estadoPago)"
                rounded
              />
            </template>
          </Column>

          <Column header="ACCIONES">
            <template #body="{ data: c }">
              <div class="b2b-table-actions">
                <Button
                  type="button"
                  label="Detalle"
                  icon="pi pi-eye"
                  severity="secondary"
                  outlined
                  size="small"
                  @click="verDetalle(c)"
                />
                <Button
                  v-if="c.estado === 'APROBADA' && (c.estadoPago === 'OBSERVADO' || c.estadoPago === 'RECHAZADO')"
                  type="button"
                  label="Subsanar"
                  icon="pi pi-pencil"
                  severity="danger"
                  outlined
                  size="small"
                  @click="irASubsanarPago(c)"
                />
                <Button
                  v-else-if="c.estado === 'APROBADA' && c.estadoPago !== 'PAGADO_TOTAL'"
                  type="button"
                  label="Abonar"
                  icon="pi pi-wallet"
                  severity="success"
                  outlined
                  size="small"
                  @click="irAPagar(c)"
                />
              </div>
            </template>
          </Column>
        </DataTable>

        <!-- Vista Canvas -->
        <template v-else>
          <CotizacionesB2BCanvasGrid
            :cotizaciones="paginatedCanvasCotizaciones"
            :loading="loading"
            @view-detail="verDetalle"
            @pay="irAPagar"
            @fix-payment="irASubsanarPago"
          />

          <BasePagination
            v-if="totalItems > pageSize"
            v-model:currentPage="currentPage"
            :total-pages="totalPages"
            :total-items="totalItems"
            :start-index="startIndex"
            :end-index="endIndex"
            item-label="cotizaciones"
          />
        </template>
    </SgaListShell>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
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
import CotizacionesB2BCanvasGrid from '../components/CotizacionesB2BCanvasGrid.vue'
import PortalCotizacionForm from '../components/PortalCotizacionForm.vue'
import { portalCotizacionService } from '../services/portalCotizacionService'
import { useNotification } from '@/shared/composables/useNotification'
import { esPendienteLiquidacion } from '../utils/portalCotizacionFormUtils'
import {
  formatCurrency,
  formatRegimen,
  formatEstadoCotizacion,
  formatEstadoPago as formatPago
} from '@/shared/utils/formatters'

const router = useRouter()
const { showSuccess, showError, showWarn } = useNotification()

const showSolicitudForm = ref(false)
const viewMode = ref('canvas')
const cotizaciones = ref([])
const loading = ref(true)
const filtroTexto = ref('')
const filtroRegimen = ref('')
const filtroEstado = ref('')
const ordenamiento = ref('recientes')

const currentPage = ref(1)
const pageSize = ref(9)

const regimenOptions = [
  { label: 'Todos los regímenes', value: '' },
  { label: 'Importación Definitiva', value: 'IMPORTACION_DEFINITIVA' },
  { label: 'Exportación Definitiva', value: 'EXPORTACION_DEFINITIVA' }
]

const estadoOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Aprobadas', value: 'APROBADA' },
  { label: 'En Evaluación', value: 'PENDIENTE' },
  { label: 'Observadas', value: 'OBSERVADA' },
  { label: 'Rechazadas', value: 'RECHAZADA' }
]

const sortOptions = [
  { label: 'Ordenar: Más recientes', value: 'recientes' },
  { label: 'Ordenar: Mayor monto', value: 'monto-desc' },
  { label: 'Ordenar: Menor monto', value: 'monto-asc' },
  { label: 'Ordenar: Código (A - Z)', value: 'codigo-asc' }
]

const hasActiveFilters = computed(() => {
  return Boolean(filtroTexto.value || filtroRegimen.value || filtroEstado.value || ordenamiento.value !== 'recientes')
})

function resetFilters() {
  filtroTexto.value = ''
  filtroRegimen.value = ''
  filtroEstado.value = ''
  ordenamiento.value = 'recientes'
}

const cotizacionesFiltradas = computed(() => {
  return cotizaciones.value.filter(c => {
    const matchRegimen = !filtroRegimen.value || c.regimenAduanero === filtroRegimen.value
    const matchEstado = !filtroEstado.value || c.estado === filtroEstado.value
    const query = filtroTexto.value.toLowerCase().trim()
    const matchTexto = !query
      || c.codigoCotizacion?.toLowerCase().includes(query)
      || c.nombreMercancia?.toLowerCase().includes(query)
      || c.nombrePuertoEnvio?.toLowerCase().includes(query)
      || c.nombrePuertoDestino?.toLowerCase().includes(query)

    return matchRegimen && matchEstado && matchTexto
  })
})

const cotizacionesOrdenadas = computed(() => {
  const list = [...cotizacionesFiltradas.value]
  switch (ordenamiento.value) {
    case 'monto-desc':
      return list.sort((a, b) => (Number(b.totalCotizado) || 0) - (Number(a.totalCotizado) || 0))
    case 'monto-asc':
      return list.sort((a, b) => (Number(a.totalCotizado) || 0) - (Number(b.totalCotizado) || 0))
    case 'codigo-asc':
      return list.sort((a, b) => String(a.codigoCotizacion || '').localeCompare(String(b.codigoCotizacion || '')))
    default:
      return list.sort((a, b) => (Number(b.idCotizacion) || 0) - (Number(a.idCotizacion) || 0))
  }
})

const totalItems = computed(() => cotizacionesOrdenadas.value.length)
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value) || 1)
const startIndex = computed(() => (totalItems.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const endIndex = computed(() => Math.min(currentPage.value * pageSize.value, totalItems.value))

const paginatedCanvasCotizaciones = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return cotizacionesOrdenadas.value.slice(start, start + pageSize.value)
})

watch([filtroTexto, filtroRegimen, filtroEstado, ordenamiento], () => {
  currentPage.value = 1
})

function getEstadoSeverity(estado) {
  switch (estado) {
    case 'APROBADA':
      return 'success'
    case 'PENDIENTE':
    case 'EN_REVISION':
      return 'warn'
    case 'OBSERVADA':
    case 'RECHAZADA':
      return 'danger'
    default:
      return 'secondary'
  }
}

function getPagoSeverity(estadoPago) {
  switch (estadoPago) {
    case 'PAGADO_TOTAL':
    case 'APROBADO':
    case 'ADELANTO_APROBADO':
      return 'success'
    case 'PENDIENTE_REVISION':
      return 'info'
    case 'OBSERVADO':
    case 'RECHAZADO':
      return 'danger'
    default:
      return 'warn'
  }
}

onMounted(async () => {
  await cargarCotizaciones()
})

async function cargarCotizaciones() {
  loading.value = true
  try {
    const data = await portalCotizacionService.listarCotizaciones()
    cotizaciones.value = data || []
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar cotizaciones')
  } finally {
    loading.value = false
  }
}

async function handleSolicitudSaved({ cotizacion, docsResult }) {
  showSolicitudForm.value = false
  if (docsResult?.failed?.length > 0) {
    showWarn(
      `Cotización ${cotizacion?.codigoCotizacion || ''} generada, pero algunos documentos no pudieron subirse. Puede reintentar desde el detalle del expediente.`,
      'Solicitud Parcial'
    )
  } else {
    showSuccess(
      `Solicitud de cotización ${cotizacion?.codigoCotizacion || ''} remitida con éxito a evaluación comercial.`,
      'Solicitud Enviada'
    )
  }
  await cargarCotizaciones()
}

function verDetalle(c) {
  if (!c) return
  router.push(`/cotizaciones/${c.idCotizacion}`)
}

function irAPagar(c) {
  if (!c) return
  router.push({
    path: '/pagos/registrar',
    query: { tipo: 'COTIZACION', id: c.idCotizacion }
  })
}

function irASubsanarPago(c) {
  if (!c) return
  router.push({
    path: '/pagos',
    query: { tab: 'historial', idCotizacion: c.idCotizacion }
  })
}
</script>

<style scoped>
.cotizaciones-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
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
