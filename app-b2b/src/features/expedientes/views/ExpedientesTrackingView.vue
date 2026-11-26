<template>
  <div class="expedientes-view fade-in">
    <SgaListShell v-model:viewMode="viewMode">
      <template #filters>
        <IconField class="sga-filter-search">
          <InputIcon class="pi pi-search" />
          <InputText
            v-model.trim="searchQuery"
            placeholder="Buscar por expediente, DAM, contenedor o mercancía..."
            aria-label="Buscar expedientes por número, DAM, contenedor o mercancía"
            size="small"
            fluid
          />
        </IconField>

        <Select
          v-model="filterFase"
          :options="faseOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          class="sga-filter-select"
          aria-label="Filtrar por estado del expediente"
        />

        <Select
          v-model="filterRegimen"
          :options="regimenOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          class="sga-filter-select"
          aria-label="Filtrar por régimen"
        />

        <Select
          v-model="filterCanal"
          :options="canalOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          class="sga-filter-select"
          aria-label="Filtrar por canal SUNAT"
        />

        <Select
          v-model="sortBy"
          :options="sortOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          class="sga-filter-select sga-sort-select"
          aria-label="Ordenar expedientes"
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
          v-if="cotizacionesElegibles.length > 0"
          type="button"
          :label="`Generar Expediente Simple (${cotizacionesElegibles.length})`"
          icon="pi pi-folder-plus"
          severity="success"
          size="small"
          @click="abrirModalExpedienteSimple(null)"
        />
        <Button
          type="button"
          label="Actualizar"
          icon="pi pi-sync"
          severity="secondary"
          outlined
          size="small"
          :loading="loading"
          @click="cargarExpedientes"
        />
      </template>

      <!-- Vista Tabla PrimeVue -->
      <DataTable
        v-if="viewMode === 'table'"
        :value="expedientesFiltrados"
        :loading="loading"
        dataKey="idExpediente"
        responsiveLayout="scroll"
        paginator
        :rows="10"
        :rowsPerPageOptions="[10, 20, 50]"
        currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} expedientes"
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        @row-dblclick="({ data }) => abrirDetalle(data)"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-box sga-empty-icon"></i>
            <p class="sga-empty-title">No se encontraron expedientes</p>
            <p class="sga-empty-desc">
              Sus cotizaciones aprobadas con pago validado aparecerán aquí en cuanto se aperture la carpeta aduanera.
            </p>
          </div>
        </template>

        <Column header="EXPEDIENTE">
          <template #body="{ data: exp }">
            <span class="sga-code-cell sga-mono">{{ exp.codigoExpediente }}</span>
          </template>
        </Column>

        <Column header="MERCANCÍA & RÉGIMEN">
          <template #body="{ data: exp }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ exp.nombreMercancia || 'Carga General' }}</span>
              <span class="sga-cell-sub">
                {{ isExportExpediente(exp) ? 'Exportación' : 'Importación' }}
                <template v-if="exp.naveViaje"> • {{ exp.naveViaje }}</template>
              </span>
            </div>
          </template>
        </Column>

        <Column header="DAM SUNAT">
          <template #body="{ data: exp }">
            <span class="sga-code-cell sga-mono">{{ exp.numeroDam || 'En trámite' }}</span>
          </template>
        </Column>

        <Column header="CANAL SUNAT">
          <template #body="{ data: exp }">
            <Tag
              :value="getCanalMeta(exp.canalSunat).label"
              :severity="getCanalMeta(exp.canalSunat).severity"
              rounded
            />
          </template>
        </Column>

        <Column header="FASE OPERATIVA">
          <template #body="{ data: exp }">
            <Tag
              :value="getExpedientePhase(exp).label"
              :severity="getExpedientePhase(exp).severity"
              rounded
            />
          </template>
        </Column>

        <Column header="ALMACÉN / TRANSPORTE">
          <template #body="{ data: exp }">
            <div class="sga-cell-stack">
              <span v-if="exp.tieneAlmacen && exp.diasRestantesAlmacen != null && exp.estadoExpediente !== 'CERRADO'" class="sga-cell-sub sga-mono">
                {{ exp.diasRestantesAlmacen }}d libres almacén
              </span>
              <span v-if="exp.tieneTransporte" class="sga-cell-sub">
                Transporte: {{ exp.estadoTransporte || 'PROGRAMADO' }}
              </span>
              <span v-if="!exp.tieneAlmacen && !exp.tieneTransporte" class="sga-text-muted">—</span>
            </div>
          </template>
        </Column>

        <Column header="APERTURA">
          <template #body="{ data: exp }">
            <span class="sga-date-cell sga-mono">{{ formatDate(exp.fechaApertura) }}</span>
          </template>
        </Column>

        <Column header="ACCIONES">
          <template #body="{ data: exp }">
            <Button
              type="button"
              label="Ver Tracking"
              icon="pi pi-arrow-right"
              iconPos="right"
              severity="secondary"
              outlined
              size="small"
              @click="abrirDetalle(exp)"
            />
          </template>
        </Column>
      </DataTable>

      <!-- Vista Canvas -->
      <template v-else>
        <ExpedientesB2BCanvasGrid
          :expedientes="paginatedCanvasExpedientes"
          :loading="loading"
          @open-detail="abrirDetalle"
        />

        <BasePagination
          v-if="totalItems > pageSize"
          v-model:currentPage="currentPage"
          :total-pages="totalPages"
          :total-items="totalItems"
          :start-index="startIndex"
          :end-index="endIndex"
          item-label="expedientes"
        />
      </template>
    </SgaListShell>

    <!-- Modal de Generación de Expediente Simple / Completar Datos -->
    <ExpedienteSimpleModal
      :visible="showSimpleModal"
      :expediente="expedienteParaEditarDatos"
      :cotizaciones-elegibles="cotizacionesElegibles"
      :saving="savingSimple"
      @close="showSimpleModal = false"
      @submit="handleSubmitSimple"
    />
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
import ExpedientesB2BCanvasGrid from '../components/ExpedientesB2BCanvasGrid.vue'
import ExpedienteSimpleModal from '../components/ExpedienteSimpleModal.vue'
import { usePortalExpedientes } from '../composables/usePortalExpedientes'
import { formatDate } from '@/shared/utils/formatters'
import {
  isExportExpediente,
  getExpedientePhase,
  getCanalMeta
} from '../utils/expedienteTrackingUtils'

const router = useRouter()

const viewMode = ref('canvas')
const currentPage = ref(1)
const pageSize = ref(9)

const {
  loading,
  savingSimple,
  cotizacionesElegibles,
  searchQuery,
  filterRegimen,
  filterCanal,
  filterFase,
  sortBy,
  abiertosCount,
  finalizadosCount,
  todosCount,
  hasActiveFilters,
  expedientesFiltrados,
  resetFilters,
  cargarExpedientes,
  guardarExpedienteSimple
} = usePortalExpedientes()

const showSimpleModal = ref(false)
const expedienteParaEditarDatos = ref(null)

function abrirDetalle(exp) {
  if (!exp?.idExpediente) return
  router.push(`/expedientes/${exp.idExpediente}`)
}

function abrirModalExpedienteSimple(exp = null) {
  expedienteParaEditarDatos.value = exp || null
  showSimpleModal.value = true
}

async function handleSubmitSimple(formPayload) {
  const res = await guardarExpedienteSimple(formPayload)
  if (res) {
    showSimpleModal.value = false
  }
}

const faseOptions = computed(() => [
  { label: `Abiertos (${abiertosCount.value})`, value: 'ABIERTOS' },
  { label: `Finalizados (${finalizadosCount.value})`, value: 'FINALIZADOS' },
  { label: `Todos (${todosCount.value})`, value: 'TODOS' }
])

const regimenOptions = [
  { label: 'Todos los Regímenes', value: 'TODOS' },
  { label: 'Importación', value: 'IMPORTACION' },
  { label: 'Exportación', value: 'EXPORTACION' }
]

const canalOptions = [
  { label: 'Todos los Canales', value: 'TODOS' },
  { label: 'Canal Verde', value: 'VERDE' },
  { label: 'Canal Naranja', value: 'NARANJA' },
  { label: 'Canal Rojo', value: 'ROJO' },
  { label: 'Canal Pendiente', value: 'PENDIENTE' }
]

const sortOptions = [
  { label: 'Ordenar: Más recientes', value: 'recientes' },
  { label: 'Ordenar: N° DAM (A - Z)', value: 'dam-asc' },
  { label: 'Ordenar: Código (A - Z)', value: 'codigo-asc' }
]

const totalItems = computed(() => expedientesFiltrados.value.length)
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value) || 1)
const startIndex = computed(() => (totalItems.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const endIndex = computed(() => Math.min(currentPage.value * pageSize.value, totalItems.value))

const paginatedCanvasExpedientes = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return expedientesFiltrados.value.slice(start, start + pageSize.value)
})

watch([searchQuery, filterRegimen, filterCanal, filterFase, sortBy], () => {
  currentPage.value = 1
})

onMounted(() => {
  cargarExpedientes()
})
</script>

<style scoped>
.expedientes-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
  font-family: var(--font-sans);
}
</style>
