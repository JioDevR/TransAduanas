<template>
  <div class="almacenamiento-view fade-in">
    <!-- 1. Filtros sgaUI -->
    <section class="sga-card-surface sga-filters-card">
      <div class="sga-filters-grid">
        <div class="sga-filter-field">
          <label class="sga-field-label">Buscar archivo o ruta</label>
          <InputText
            v-model.trim="filters.buscar"
            placeholder="Nombre, ruta relativa o usuario..."
            size="small"
            fluid
            @keyup.enter="loadStorage(0)"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Entidad</label>
          <Select
            v-model="filters.entidad"
            :options="entidadOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @change="loadStorage(0)"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Categoría</label>
          <Select
            v-model="filters.categoria"
            :options="categoriaOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @change="loadStorage(0)"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Integridad</label>
          <Select
            v-model="filters.integridad"
            :options="integridadOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @change="loadStorage(0)"
          />
        </div>

        <div class="sga-filter-reset">
          <Button
            label="Filtrar"
            icon="pi pi-search"
            size="small"
            @click="loadStorage(0)"
          />
          <Button
            v-if="hasFilters"
            label="Limpiar"
            icon="pi pi-filter-slash"
            severity="secondary"
            text
            size="small"
            @click="resetFilters"
          />
        </div>
      </div>
    </section>

    <!-- 2. Inventario de Archivos (Solo Tabla sgaUI) -->
    <section class="sga-list-panel">
      <div class="sga-panel-header">
        <div class="sga-panel-title-group">
          <h2 class="sga-panel-title">Archivos Almacenados</h2>
        </div>

        <div class="sga-dash-actions">
          <Button
            v-if="authStore.isAdministradorTI && (storage.archivosHuerfanosEnDisco || 0) > 0"
            :label="`Depurar Huérfanos (${storage.archivosHuerfanosEnDisco})`"
            icon="pi pi-trash"
            severity="warn"
            outlined
            size="small"
            :loading="purging"
            @click="handleDepurarHuerfanos"
          />
          <Button
            label="Actualizar"
            icon="pi pi-sync"
            severity="secondary"
            outlined
            size="small"
            :loading="loading"
            @click="loadStorage(0)"
          />
        </div>
      </div>

      <div class="sga-card-surface sga-table-card">
        <div class="sga-table-toolbar">
          <div class="sga-toolbar-filters">
            <span class="sga-status-note sga-text-muted">
              Cruce entre los archivos físicos en disco local y los metadatos persistidos en base de datos.
            </span>
          </div>
        </div>

        <DataTable
          :value="storage.items || []"
          :loading="loading"
          size="small"
          stripedRows
          responsiveLayout="scroll"
          dataKey="rutaRelativa"
        >
          <template #empty>
            <div class="sga-empty-state">
              <i class="pi pi-folder-open sga-empty-icon"></i>
              <p class="sga-empty-title">Sin archivos en almacenamiento</p>
              <p class="sga-empty-desc">No se encontraron registros para los filtros seleccionados.</p>
            </div>
          </template>

          <Column header="Archivo">
            <template #body="{ data }">
              <div class="stg-cell-stack">
                <strong>{{ data.nombreOriginal }}</strong>
                <small class="sga-mono sga-text-muted">{{ data.extension }} · {{ data.formatoMime || '—' }}</small>
              </div>
            </template>
          </Column>

          <Column header="Entidad / Categoría">
            <template #body="{ data }">
              <div class="stg-cell-stack">
                <span class="sga-mono">{{ data.entidadTipo }}</span>
                <small class="sga-text-muted">{{ data.categoria }}</small>
              </div>
            </template>
          </Column>

          <Column header="Ruta Relativa">
            <template #body="{ data }">
              <span class="sga-mono stg-path-text">{{ data.rutaRelativa || '—' }}</span>
            </template>
          </Column>

          <Column header="Tamaño" style="width: 110px">
            <template #body="{ data }">
              <span class="sga-mono">{{ formatBytes(data.tamanoBytes) }}</span>
            </template>
          </Column>

          <Column header="Origen / Fecha" style="width: 165px">
            <template #body="{ data }">
              <div class="stg-cell-stack">
                <span class="sga-mono">@{{ data.usuarioCreador || 'SISTEMA' }}</span>
                <small class="sga-mono sga-text-muted">{{ formatFecha(data.fechaCreacion) }}</small>
              </div>
            </template>
          </Column>

          <Column header="Integridad" style="width: 145px">
            <template #body="{ data }">
              <Tag
                :value="integridadLabel(data.estadoIntegridad)"
                :severity="integridadSeverity(data.estadoIntegridad)"
                rounded
              />
            </template>
          </Column>
        </DataTable>

        <BasePagination
          v-if="(storage.totalElements || 0) > 0"
          :current-page="page + 1"
          :total-pages="Math.max(1, storage.totalPages || 0)"
          :total-items="storage.totalElements || 0"
          :start-index="page * pageSize + 1"
          :end-index="Math.min((page + 1) * pageSize, storage.totalElements || 0)"
          item-label="archivos"
          :disabled="loading"
          @prev="loadStorage(page - 1)"
          @next="loadStorage(page + 1)"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import BasePagination from '@/shared/components/BasePagination.vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { sistemaService } from '../services/sistemaService'
import { formatBytes } from '../composables/useAdminDashboard'

const authStore = useAuthStore()
const { showSuccess, showError } = useNotification()
const { confirmAction } = useConfirmation()

const loading = ref(false)
const purging = ref(false)
const page = ref(0)
const pageSize = 20

const storage = ref({
  archivosHuerfanosEnDisco: 0,
  bytesHuerfanosEnDisco: 0,
  items: [],
  totalElements: 0,
  totalPages: 0,
  page: 0
})

const filters = reactive({
  buscar: '',
  entidad: 'ALL',
  categoria: 'ALL',
  integridad: 'ALL'
})

const entidadOptions = [
  { label: 'Todas las entidades', value: 'ALL' },
  { label: 'Expediente', value: 'EXPEDIENTE' },
  { label: 'Cotización', value: 'COTIZACION' },
  { label: 'Cliente', value: 'CLIENTE' },
  { label: 'General', value: 'GENERAL' },
  { label: 'Sin registro (Huérfano)', value: 'SIN_REGISTRO' }
]

const categoriaOptions = [
  { label: 'Todas las categorías', value: 'ALL' },
  { label: 'Factura', value: 'FACTURA' },
  { label: 'Packing List', value: 'PACKING_LIST' },
  { label: 'Transporte / BL', value: 'TRANSPORTE_BL' },
  { label: 'Certificado', value: 'CERTIFICADO' },
  { label: 'Permiso Especial', value: 'PERMISO_ESPECIAL' },
  { label: 'Identificación', value: 'IDENTIFICACION' },
  { label: 'General', value: 'GENERAL' },
  { label: 'Huérfano en disco', value: 'HUERFANO' }
]

const integridadOptions = [
  { label: 'Todos los estados', value: 'ALL' },
  { label: 'Sincronizado', value: 'SINCRONIZADO' },
  { label: 'Baja lógica', value: 'BAJA_LOGICA' },
  { label: 'Faltante en disco', value: 'FALTANTE_DISCO' },
  { label: 'Huérfano en disco', value: 'HUERFANO' }
]

const hasFilters = computed(() =>
  Boolean(
    filters.buscar ||
      filters.entidad !== 'ALL' ||
      filters.categoria !== 'ALL' ||
      filters.integridad !== 'ALL'
  )
)

const loadStorage = async (targetPage = 0) => {
  loading.value = true
  try {
    const res = await sistemaService.getAlmacenamiento({
      buscar: filters.buscar,
      entidad: filters.entidad === 'ALL' ? '' : filters.entidad,
      categoria: filters.categoria === 'ALL' ? '' : filters.categoria,
      integridad: filters.integridad === 'ALL' ? '' : filters.integridad,
      page: Math.max(0, targetPage),
      size: pageSize
    })
    storage.value = res || storage.value
    page.value = res?.page ?? 0
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar el diagnóstico de almacenamiento')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.buscar = ''
  filters.entidad = 'ALL'
  filters.categoria = 'ALL'
  filters.integridad = 'ALL'
  loadStorage(0)
}

const handleDepurarHuerfanos = async () => {
  const count = storage.value.archivosHuerfanosEnDisco || 0
  const bytes = formatBytes(storage.value.bytesHuerfanosEnDisco || 0)
  const confirmed = await confirmAction({
    header: 'Confirmar Depuración de Huérfanos',
    message: `Se eliminarán ${count} archivo(s) físico(s) en disco que no tienen registro en base de datos (${bytes}). Esta acción quedará registrada en Registro TI.`,
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Depurar',
    acceptClass: 'p-button-warning'
  })
  if (!confirmed) return

  purging.value = true
  try {
    const res = await sistemaService.depurarHuerfanos()
    showSuccess(`Depuración completada: ${res?.archivosEliminados ?? 0} archivo(s) eliminado(s) (${formatBytes(res?.bytesLiberados ?? 0)}).`)
    await loadStorage(0)
  } catch (err) {
    showError(err.response?.data?.message || 'No se pudo completar la depuración de almacenamiento')
  } finally {
    purging.value = false
  }
}

const formatFecha = (iso) => {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const integridadLabel = (estado) => {
  switch (estado) {
    case 'SINCRONIZADO':
      return 'Sincronizado'
    case 'BAJA_LOGICA':
      return 'Baja lógica'
    case 'FALTANTE_DISCO':
      return 'Falta en disco'
    case 'HUERFANO':
      return 'Huérfano'
    default:
      return estado || '—'
  }
}

const integridadSeverity = (estado) => {
  switch (estado) {
    case 'SINCRONIZADO':
      return 'success'
    case 'BAJA_LOGICA':
      return 'secondary'
    case 'FALTANTE_DISCO':
      return 'danger'
    case 'HUERFANO':
      return 'warn'
    default:
      return 'secondary'
  }
}

onMounted(() => {
  loadStorage(0)
})
</script>

<style scoped>
.almacenamiento-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.stg-cell-stack {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.stg-path-text {
  font-size: 0.78rem;
  word-break: break-all;
}
</style>
