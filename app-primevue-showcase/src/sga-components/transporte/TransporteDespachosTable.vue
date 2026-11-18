<template>
  <div class="transporte-despachos-table-view">
    <!-- Barra de Filtros y Búsqueda -->
    <div class="table-controls-bar">
      <div class="filter-pills-group">
        <button
          type="button"
          :class="['filter-pill', { active: activeStatusFilter === '' }]"
          @click="setStatusFilter('')"
        >
          <span>Todos</span>
          <span class="pill-count font-mono">{{ despachosList.length }}</span>
        </button>

        <button
          type="button"
          :class="['filter-pill warning', { active: activeStatusFilter === 'PENDIENTE_ASIGNACION' }]"
          @click="setStatusFilter('PENDIENTE_ASIGNACION')"
        >
          <span>Pendientes de Recursos</span>
          <span class="pill-count font-mono">{{ countByStatus('PENDIENTE_ASIGNACION') }}</span>
        </button>

        <button
          type="button"
          :class="['filter-pill info', { active: activeStatusFilter === 'PROGRAMADO' }]"
          @click="setStatusFilter('PROGRAMADO')"
        >
          <span>Programados</span>
          <span class="pill-count font-mono">{{ countByStatus('PROGRAMADO') }}</span>
        </button>

        <button
          type="button"
          :class="['filter-pill accent', { active: activeStatusFilter === 'EN_RUTA' }]"
          @click="setStatusFilter('EN_RUTA')"
        >
          <span>En Ruta Activa</span>
          <span class="pill-count font-mono">{{ countByStatus('EN_RUTA') }}</span>
        </button>

        <button
          type="button"
          :class="['filter-pill success', { active: activeStatusFilter === 'ENTREGADO' }]"
          @click="setStatusFilter('ENTREGADO')"
        >
          <span>Entregados Conformes</span>
          <span class="pill-count font-mono">{{ countByStatus('ENTREGADO') }}</span>
        </button>
      </div>

      <div class="search-box-wrap">
        <i class="pi pi-search search-icon"></i>
        <input
          v-model="searchQuery"
          type="text"
          class="search-input"
          placeholder="Buscar por TRK, expediente, placa o distrito..."
        />
        <button
          v-if="searchQuery"
          type="button"
          class="clear-search-btn"
          @click="searchQuery = ''"
        >
          <i class="pi pi-times"></i>
        </button>
      </div>
    </div>

    <!-- Tabla Principal PrimeVue -->
    <div class="table-container-card">
      <DataTable
        :value="filteredDespachos"
        :loading="loading"
        responsiveLayout="scroll"
        class="sga-editorial-datatable"
        :paginator="true"
        :rows="10"
        :rowsPerPageOptions="[10, 20, 50]"
      >
        <template #empty>
          <div class="empty-state-box">
            <i class="pi pi-inbox empty-icon"></i>
            <p>No se encontraron despachos con los filtros seleccionados.</p>
          </div>
        </template>

        <Column field="codigoTracking" header="Código Tracking" sortable>
          <template #body="{ data: d }">
            <div class="tracking-cell font-mono">
              <span class="tracking-code font-bold">{{ d.codigoTracking }}</span>
              <span class="tipo-traslado-tag">{{ formatTipoTraslado(d.tipoTraslado) }}</span>
            </div>
          </template>
        </Column>

        <Column header="Expediente / Cliente" sortable field="expediente.codigoExpediente">
          <template #body="{ data: d }">
            <div class="expediente-cell">
              <span class="font-mono font-semibold">{{ d.expediente?.codigoExpediente || 'S/N' }}</span>
              <span class="cliente-name truncate">{{ d.expediente?.cliente?.razonSocial || 'Cliente no disponible' }}</span>
            </div>
          </template>
        </Column>

        <Column header="Unidad & Chofer">
          <template #body="{ data: d }">
            <div v-if="d.unidad || d.conductor" class="recursos-cell">
              <div v-if="d.unidad" class="recurso-line">
                <i class="pi pi-truck icon-muted"></i>
                <span class="font-mono font-semibold">{{ d.unidad.placaTracto }}</span>
                <span v-if="d.unidad.placaCarreta" class="font-mono text-tertiary">/ {{ d.unidad.placaCarreta }}</span>
              </div>
              <div v-if="d.conductor" class="recurso-line">
                <i class="pi pi-user icon-muted"></i>
                <span>{{ d.conductor.nombres }} {{ d.conductor.apellidos }}</span>
              </div>
            </div>
            <div v-else class="sin-asignar-cell">
              <span class="badge-sin-asignar">Sin recursos asignados</span>
            </div>
          </template>
        </Column>

        <Column header="Ruta (Partida -> Destino)">
          <template #body="{ data: d }">
            <div class="ruta-cell">
              <span class="origen-text text-truncate">{{ d.direccionPartida }}</span>
              <i class="pi pi-arrow-right ruta-arrow"></i>
              <span class="destino-text font-semibold text-truncate">
                {{ d.distritoLlegada ? d.distritoLlegada + ' - ' : '' }}{{ d.direccionLlegada }}
              </span>
            </div>
          </template>
        </Column>

        <Column field="estadoTransporte" header="Estado" sortable>
          <template #body="{ data: d }">
            <Tag :value="formatEstado(d.estadoTransporte)" :severity="getEstadoSeverity(d.estadoTransporte)" rounded />
          </template>
        </Column>

        <Column field="fechaProgramada" header="Programación" sortable>
          <template #body="{ data: d }">
            <span class="font-mono text-sm">{{ formatDate(d.fechaProgramada) }}</span>
          </template>
        </Column>

        <Column header="Acciones Operativas" bodyClass="actions-column">
          <template #body="{ data: d }">
            <div class="row-actions-group">
              <!-- Asignar recursos -->
              <button
                v-if="d.estadoTransporte === 'PENDIENTE_ASIGNACION'"
                type="button"
                class="btn-row-action primary"
                @click="$emit('asignar-recursos', d)"
                title="Asignar tracto, carreta y chofer"
              >
                <i class="pi pi-user-plus"></i>
                <span>Asignar</span>
              </button>

              <!-- Iniciar ruta -->
              <button
                v-if="d.estadoTransporte === 'PROGRAMADO'"
                type="button"
                class="btn-row-action send"
                @click="$emit('iniciar-ruta', d)"
                title="Iniciar salida a ruta"
              >
                <i class="pi pi-send"></i>
                <span>Salida</span>
              </button>

              <!-- Confirmar entrega -->
              <button
                v-if="d.estadoTransporte === 'EN_RUTA'"
                type="button"
                class="btn-row-action success"
                @click="$emit('confirmar-entrega', d)"
                title="Registrar entrega conforme"
              >
                <i class="pi pi-check"></i>
                <span>Entregar</span>
              </button>

              <!-- Ver en cabina -->
              <button
                type="button"
                class="btn-row-icon"
                @click="$emit('ver-cabina', d)"
                title="Supervisar en cabina de ruta"
              >
                <i class="pi pi-compass"></i>
              </button>

              <!-- Reportar incidencia -->
              <button
                type="button"
                class="btn-row-icon danger"
                @click="$emit('reportar-incidencia', d)"
                title="Registrar novedad / incidencia"
              >
                <i class="pi pi-exclamation-circle"></i>
              </button>
            </div>
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'

const props = defineProps({
  despachosList: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  initialFilter: {
    type: String,
    default: ''
  }
})

defineEmits(['asignar-recursos', 'iniciar-ruta', 'confirmar-entrega', 'ver-cabina', 'reportar-incidencia'])

const activeStatusFilter = ref(props.initialFilter)
const searchQuery = ref('')

const setStatusFilter = (status) => {
  activeStatusFilter.value = status
}

const countByStatus = (status) => {
  return props.despachosList.filter(d => (d.estadoTransporte || '').toUpperCase() === status).length
}

const filteredDespachos = computed(() => {
  return props.despachosList.filter(d => {
    const matchStatus = !activeStatusFilter.value || (d.estadoTransporte || '').toUpperCase() === activeStatusFilter.value
    const q = searchQuery.value.trim().toLowerCase()
    const matchSearch =
      !q ||
      (d.codigoTracking && d.codigoTracking.toLowerCase().includes(q)) ||
      (d.expediente?.codigoExpediente && d.expediente.codigoExpediente.toLowerCase().includes(q)) ||
      (d.expediente?.cliente?.razonSocial && d.expediente.cliente.razonSocial.toLowerCase().includes(q)) ||
      (d.unidad?.placaTracto && d.unidad.placaTracto.toLowerCase().includes(q)) ||
      (d.distritoLlegada && d.distritoLlegada.toLowerCase().includes(q)) ||
      (d.direccionLlegada && d.direccionLlegada.toLowerCase().includes(q))

    return matchStatus && matchSearch
  })
})

const formatEstado = (est) => {
  const map = {
    PENDIENTE_ASIGNACION: 'Pendiente Recursos',
    PROGRAMADO: 'Programado',
    EN_RUTA: 'En Ruta',
    ENTREGADO: 'Entregado Conforme',
    CANCELADO: 'Cancelado'
  }
  return map[est] || est
}

const getEstadoSeverity = (est) => {
  switch (est) {
    case 'PENDIENTE_ASIGNACION': return 'warn'
    case 'PROGRAMADO': return 'info'
    case 'EN_RUTA': return 'primary'
    case 'ENTREGADO': return 'success'
    case 'CANCELADO': return 'danger'
    default: return 'secondary'
  }
}

const formatTipoTraslado = (tipo) => {
  if (tipo === 'ENTREGA_IMPORTACION') return 'Importación'
  if (tipo === 'RECOJO_EXPORTACION') return 'Exportación'
  return tipo || 'Traslado Local'
}

const formatDate = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  return d.toLocaleDateString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.transporte-despachos-table-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.table-controls-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.filter-pills-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: 9999px;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
}

.filter-pill:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.filter-pill.active {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

.pill-count {
  font-size: 0.6875rem;
  background: var(--color-bg-muted);
  padding: 0.1rem 0.35rem;
  border-radius: 9999px;
}

.filter-pill.active .pill-count {
  background: rgba(255, 255, 255, 0.2);
  color: var(--color-primary-text);
}

.search-box-wrap {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 280px;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  color: var(--color-text-subtle);
  font-size: 0.875rem;
}

.search-input {
  width: 100%;
  padding: 0.5rem 2rem 0.5rem 2.25rem;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border);
  font-size: 0.8125rem;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
}

.clear-search-btn {
  position: absolute;
  right: 0.5rem;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-text-subtle);
  padding: 0.25rem;
}

.table-container-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.tracking-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.tipo-traslado-tag {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
}

.expediente-cell {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.cliente-name {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  max-width: 180px;
}

.recursos-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.75rem;
}

.recurso-line {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.icon-muted {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.badge-sin-asignar {
  font-size: 0.6875rem;
  font-style: italic;
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.ruta-cell {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  max-width: 240px;
}

.ruta-arrow {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
}

.row-actions-group {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.btn-row-action {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.3rem 0.625rem;
  border-radius: var(--radius-sm, 0.375rem);
  font-size: 0.6875rem;
  font-weight: 600;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-row-action.primary {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
}

.btn-row-action.primary:hover {
  filter: brightness(0.95);
}

.btn-row-action.send {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border-color: var(--color-info-border);
}

.btn-row-action.send:hover {
  filter: brightness(0.95);
}

.btn-row-action.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.btn-row-action.success:hover {
  filter: brightness(0.95);
}

.btn-row-icon {
  width: 1.875rem;
  height: 1.875rem;
  border-radius: var(--radius-sm, 0.375rem);
  border: 1px solid var(--color-border);
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all 0.15s ease;
}

.btn-row-icon:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.btn-row-icon.danger:hover {
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
  background: var(--color-danger-bg);
}

.empty-state-box {
  padding: 3rem 1rem;
  text-align: center;
  color: var(--color-text-subtle);
}

.empty-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}
</style>
