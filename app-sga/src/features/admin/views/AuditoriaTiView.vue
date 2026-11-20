<template>
  <div class="auditoria-ti-view fade-in">
    <!-- 1. Barra de Estado y Acciones sgaUI (Título único en Topbar) -->
    <header class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag :value="`${totalElements} eventos registrados`" severity="secondary" />
      </div>

      <div class="sga-dash-actions">
        <Button
          label="Actualizar"
          icon="pi pi-sync"
          severity="secondary"
          outlined
          size="small"
          :loading="loading"
          @click="fetchEventos(0)"
        />
      </div>
    </header>

    <!-- 2. Filtros sgaUI -->
    <section class="sga-card-surface sga-filters-card">
      <div class="sga-filters-grid">
        <div class="sga-filter-field">
          <label class="sga-field-label">Buscar actor / acción / detalle</label>
          <InputText
            v-model.trim="filters.buscar"
            placeholder="Usuario, acción o detalle..."
            size="small"
            fluid
            @keyup.enter="fetchEventos(0)"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Módulo</label>
          <Select
            v-model="filters.modulo"
            :options="moduloOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @change="fetchEventos(0)"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Resultado</label>
          <Select
            v-model="filters.resultado"
            :options="resultadoOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @change="fetchEventos(0)"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Desde</label>
          <InputText
            v-model="filters.fechaDesde"
            type="date"
            size="small"
            fluid
            @change="fetchEventos(0)"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Hasta</label>
          <InputText
            v-model="filters.fechaHasta"
            type="date"
            size="small"
            fluid
            @change="fetchEventos(0)"
          />
        </div>

        <div class="sga-filter-reset">
          <Button
            label="Filtrar"
            icon="pi pi-search"
            size="small"
            @click="fetchEventos(0)"
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

    <!-- 3. Listado Dual sgaUI (SgaListShell) -->
    <SgaListShell v-model:viewMode="viewMode" title="Bitácora Estructurada">
      <template #filters>
        <span class="sga-status-note sga-text-muted">
          Trazabilidad persistente en base de datos (accesos, cuentas, contraseñas y logs).
        </span>
      </template>

      <div v-if="viewMode === 'table'">
        <DataTable
          :value="items"
          :loading="loading"
          size="small"
          stripedRows
          responsiveLayout="scroll"
          dataKey="idEvento"
        >
          <template #empty>
            <div class="sga-empty-state">
              <i class="pi pi-shield sga-empty-icon"></i>
              <p class="sga-empty-title">Sin eventos de auditoría</p>
              <p class="sga-empty-desc">No se encontraron registros para los filtros seleccionados.</p>
            </div>
          </template>

          <Column header="Fecha y Hora" style="width: 155px">
            <template #body="{ data }">
              <span class="sga-mono">{{ formatFecha(data.fecha) }}</span>
            </template>
          </Column>

          <Column header="Actor / Rol">
            <template #body="{ data }">
              <div class="aud-cell-stack">
                <strong class="sga-mono">@{{ data.usuarioActor }}</strong>
                <small class="sga-text-muted">{{ data.rolActor || 'SIN ROL' }}</small>
              </div>
            </template>
          </Column>

          <Column header="Módulo / Acción">
            <template #body="{ data }">
              <div class="aud-cell-stack">
                <span class="sga-mono">{{ data.accion }}</span>
                <small class="sga-text-muted">{{ data.modulo }} · {{ data.entidad || '—' }}</small>
              </div>
            </template>
          </Column>

          <Column header="Detalle">
            <template #body="{ data }">
              <span>{{ data.detalle || '—' }}</span>
            </template>
          </Column>

          <Column header="IP" style="width: 120px">
            <template #body="{ data }">
              <span class="sga-mono sga-text-muted">{{ data.ipOrigen || '—' }}</span>
            </template>
          </Column>

          <Column header="Resultado" style="width: 105px">
            <template #body="{ data }">
              <Tag
                :value="data.resultado"
                :severity="data.resultado === 'EXITO' ? 'success' : 'danger'"
                rounded
              />
            </template>
          </Column>

          <Column header="" style="width: 80px">
            <template #body="{ data }">
              <Button
                icon="pi pi-eye"
                severity="secondary"
                text
                size="small"
                title="Ver detalle completo"
                @click="openDetail(data)"
              />
            </template>
          </Column>
        </DataTable>
      </div>

      <div v-else class="sga-canvas-grid">
        <article v-for="ev in items" :key="ev.idEvento" class="sga-canvas-card">
          <div class="sga-card-top">
            <span class="sga-card-code sga-mono">#{{ ev.idEvento }} · {{ ev.accion }}</span>
            <Tag
              :value="ev.resultado"
              :severity="ev.resultado === 'EXITO' ? 'success' : 'danger'"
              rounded
            />
          </div>
          <div class="sga-card-meta-grid">
            <div class="sga-meta-item">
              <span class="sga-meta-label">ACTOR</span>
              <span class="sga-meta-val sga-mono">@{{ ev.usuarioActor }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">MÓDULO</span>
              <span class="sga-meta-val">{{ ev.modulo }}</span>
            </div>
            <div class="sga-meta-item full-span">
              <span class="sga-meta-label">DETALLE</span>
              <span class="sga-meta-val">{{ ev.detalle || '—' }}</span>
            </div>
          </div>
          <div class="sga-card-footer">
            <small class="sga-mono sga-text-muted">{{ formatFecha(ev.fecha) }}</small>
            <Button label="Detalle" size="small" outlined severity="secondary" @click="openDetail(ev)" />
          </div>
        </article>
      </div>

      <BasePagination
        v-if="totalElements > 0"
        :current-page="page + 1"
        :total-pages="Math.max(1, totalPages)"
        :total-items="totalElements"
        :start-index="page * pageSize + 1"
        :end-index="Math.min((page + 1) * pageSize, totalElements)"
        item-label="eventos"
        :disabled="loading"
        @prev="fetchEventos(page - 1)"
        @next="fetchEventos(page + 1)"
      />
    </SgaListShell>

    <!-- Modal Detalle Evento (sga-forms.css) -->
    <Dialog
      v-model:visible="showDetailModal"
      modal
      header="Detalle de Evento de Auditoría"
      :style="{ width: '540px', maxWidth: '95vw' }"
    >
      <div v-if="selectedEvent" class="sga-dialog-body">
        <div class="sga-banner" :class="selectedEvent.resultado === 'EXITO' ? 'is-success' : 'is-danger'">
          <i :class="selectedEvent.resultado === 'EXITO' ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle'"></i>
          <div>
            <strong class="sga-mono">{{ selectedEvent.accion }}</strong>
            <div>{{ selectedEvent.detalle || 'Sin descripción adicional' }}</div>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">ID EVENTO</span>
            <span class="sga-meta-val sga-mono">#{{ selectedEvent.idEvento }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">FECHA</span>
            <span class="sga-meta-val sga-mono">{{ formatFecha(selectedEvent.fecha) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">USUARIO ACTOR</span>
            <span class="sga-meta-val sga-mono">@{{ selectedEvent.usuarioActor }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">ROL ACTOR</span>
            <span class="sga-meta-val">{{ selectedEvent.rolActor || '—' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">ENTIDAD / ID</span>
            <span class="sga-meta-val sga-mono">{{ selectedEvent.entidad || '—' }} #{{ selectedEvent.idEntidad || '—' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">IP ORIGEN</span>
            <span class="sga-meta-val sga-mono">{{ selectedEvent.ipOrigen || '—' }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="sga-dialog-footer">
          <Button label="Cerrar" severity="secondary" outlined size="small" @click="showDetailModal = false" />
        </div>
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import BasePagination from '@/shared/components/BasePagination.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { auditoriaService } from '../services/auditoriaService'

const { showError } = useNotification()

const viewMode = ref('table')
const loading = ref(false)
const items = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const pageSize = 20

const showDetailModal = ref(false)
const selectedEvent = ref(null)

const filters = reactive({
  buscar: '',
  modulo: 'ALL',
  resultado: 'ALL',
  fechaDesde: '',
  fechaHasta: ''
})

const moduloOptions = [
  { label: 'Todos los módulos', value: 'ALL' },
  { label: 'Seguridad TI', value: 'SEGURIDAD_TI' },
  { label: 'Portal B2B', value: 'PORTAL_B2B' },
  { label: 'Logs TI', value: 'LOGS_TI' }
]

const resultadoOptions = [
  { label: 'Todos los resultados', value: 'ALL' },
  { label: 'Éxito', value: 'EXITO' },
  { label: 'Fallo / Alerta', value: 'FALLO' }
]

const hasFilters = computed(() =>
  Boolean(
    filters.buscar ||
      filters.modulo !== 'ALL' ||
      filters.resultado !== 'ALL' ||
      filters.fechaDesde ||
      filters.fechaHasta
  )
)

const fetchEventos = async (targetPage = 0) => {
  loading.value = true
  try {
    const res = await auditoriaService.listarEventos({
      buscar: filters.buscar,
      modulo: filters.modulo === 'ALL' ? '' : filters.modulo,
      resultado: filters.resultado === 'ALL' ? '' : filters.resultado,
      fechaDesde: filters.fechaDesde,
      fechaHasta: filters.fechaHasta,
      page: Math.max(0, targetPage),
      size: pageSize
    })
    items.value = res.items || []
    totalElements.value = res.totalElements || 0
    totalPages.value = res.totalPages || 0
    page.value = res.page || 0
  } catch (err) {
    showError(err.response?.data?.message || 'Error al consultar la auditoría TI')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.buscar = ''
  filters.modulo = 'ALL'
  filters.resultado = 'ALL'
  filters.fechaDesde = ''
  filters.fechaHasta = ''
  fetchEventos(0)
}

const openDetail = (ev) => {
  selectedEvent.value = ev
  showDetailModal.value = true
}

const formatFecha = (iso) => {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  fetchEventos(0)
})
</script>

<style scoped>
.auditoria-ti-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.aud-cell-stack {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}
</style>
