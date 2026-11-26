<template>
  <SgaListShell
    v-model:viewMode="viewMode"
    title="Directorio de Conductores Profesionales MTC"
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
          label="Ingresar Conductor"
          icon="pi pi-user-plus"
          size="small"
          @click="$emit('crear')"
        />
      </div>
    </template>

    <template #sub-bar>
      <div class="sga-queue-buttons" role="tablist" aria-label="Estado operativo de conductores">
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
          placeholder="Buscar por nombres, apellidos, DNI o número de licencia..."
          size="small"
          fluid
        />
      </IconField>

      <Select
        v-model="filterLicencia"
        :options="licenciaFilterOptions"
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
      <Button
        type="button"
        label="Editar"
        icon="pi pi-pencil"
        severity="secondary"
        outlined
        size="small"
        :disabled="!selectedRow"
        @click="$emit('editar', selectedRow)"
      />
      <Button
        v-if="selectedRow && selectedRow.estadoActivo"
        type="button"
        label="Dar de Baja"
        icon="pi pi-user-minus"
        severity="danger"
        outlined
        size="small"
        @click="handleToggleActivo(selectedRow)"
      />
      <Button
        v-else-if="selectedRow && !selectedRow.estadoActivo"
        type="button"
        label="Reactivar"
        icon="pi pi-refresh"
        severity="success"
        size="small"
        @click="handleToggleActivo(selectedRow)"
      />
    </template>

    <ConductoresCanvasGrid
      v-if="viewMode === 'canvas'"
      v-model:selection="selectedRow"
      :conductores="filteredConductores"
      :loading="loading"
      @editar="$emit('editar', $event)"
    />

    <DataTable
      v-else
      v-model:selection="selectedRow"
      :value="filteredConductores"
      :loading="loading"
      selectionMode="single"
      dataKey="idConductor"
      paginator
      :rows="10"
      :rowsPerPageOptions="[10, 20, 50]"
      responsiveLayout="scroll"
      @row-dblclick="({ data }) => $emit('editar', data)"
    >
      <template #empty>
        <div class="sga-empty-state">
          <i class="pi pi-users sga-empty-icon"></i>
          <p class="sga-empty-title">No se encontraron conductores</p>
          <p class="sga-empty-desc">Registre conductores profesionales o ajuste los filtros activos.</p>
        </div>
      </template>

      <Column header="CONDUCTOR">
        <template #body="{ data: c }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ c.nombreCompleto || `${c.nombres} ${c.apellidos}` }}</span>
            <span class="sga-cell-sub font-mono">DNI: {{ c.numeroDocumento || '—' }}</span>
          </div>
        </template>
      </Column>

      <Column header="BREVETE MTC">
        <template #body="{ data: c }">
          <div class="sga-cell-stack">
            <span class="sga-code-cell font-mono">{{ c.numeroLicencia }}</span>
            <Tag :value="c.tipoLicencia" severity="secondary" class="sga-mini-tag" rounded />
          </div>
        </template>
      </Column>

      <Column header="VENCIMIENTO LICENCIA">
        <template #body="{ data: c }">
          <span :class="['sga-date-cell font-mono', docTextClass(c.fechaVencimientoLicencia)]">
            {{ formatDate(c.fechaVencimientoLicencia) }}
          </span>
        </template>
      </Column>

      <Column header="CONTACTO">
        <template #body="{ data: c }">
          <span class="sga-cell-meta font-mono">{{ c.telefono || '—' }}</span>
        </template>
      </Column>

      <Column header="CONDICIÓN">
        <template #body="{ data: c }">
          <Tag
            :value="c.estadoActivo ? formatEstadoConductor(c.estadoOperativo) : 'Inactivo'"
            :severity="c.estadoActivo ? getEstadoConductorSeverity(c.estadoOperativo) : 'secondary'"
            rounded
          />
        </template>
      </Column>
    </DataTable>
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
import ConductoresCanvasGrid from './ConductoresCanvasGrid.vue'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { useNotification } from '@/shared/composables/useNotification'
import { transporteFlotaService } from '../services/transporteFlotaService'
import { isFechaVencida, isFechaPorVencer } from '../utils/transporteAlerts'
import {
  CATEGORIA_LICENCIA_OPTIONS,
  formatDate,
  formatEstadoConductor,
  getEstadoConductorSeverity
} from '../utils/transporteFormatters'

const props = defineProps({
  conductores: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['refresh', 'crear', 'editar'])

const { confirmAction } = useConfirmation()
const { showSuccess, showError } = useNotification()

const viewMode = ref('table')
const selectedRow = ref(null)
const searchQuery = ref('')
const filterEstado = ref('ACTIVOS')
const filterLicencia = ref('ALL')

const licenciaFilterOptions = [
  { label: 'Todas las categorías', value: 'ALL' },
  ...CATEGORIA_LICENCIA_OPTIONS
]

const queueTabs = computed(() => {
  const activos = props.conductores.filter((c) => c.estadoActivo !== false)
  const inactivos = props.conductores.filter((c) => c.estadoActivo === false)
  return [
    { label: 'Activos', value: 'ACTIVOS', count: activos.length, badgeSeverity: 'secondary' },
    {
      label: 'Disponibles',
      value: 'DISPONIBLE',
      count: activos.filter((c) => c.estadoOperativo === 'DISPONIBLE').length,
      badgeSeverity: 'success'
    },
    {
      label: 'En Ruta',
      value: 'EN_RUTA',
      count: activos.filter((c) => c.estadoOperativo === 'EN_RUTA').length,
      badgeSeverity: 'info'
    },
    {
      label: 'Descanso / Susp.',
      value: 'NO_DISP',
      count: activos.filter((c) => ['DESCANSO', 'SUSPENDIDO'].includes(c.estadoOperativo)).length,
      badgeSeverity: 'warn'
    },
    { label: 'Inactivos', value: 'INACTIVOS', count: inactivos.length, badgeSeverity: 'secondary' }
  ]
})

const hasActiveFilters = computed(
  () => Boolean(searchQuery.value) || filterEstado.value !== 'ACTIVOS' || filterLicencia.value !== 'ALL'
)

const resetFilters = () => {
  searchQuery.value = ''
  filterEstado.value = 'ACTIVOS'
  filterLicencia.value = 'ALL'
}

const filteredConductores = computed(() => {
  const q = searchQuery.value.toLowerCase()
  return props.conductores.filter((c) => {
    if (filterEstado.value === 'INACTIVOS') {
      if (c.estadoActivo !== false) return false
    } else {
      if (c.estadoActivo === false) return false
      if (filterEstado.value === 'NO_DISP') {
        if (!['DESCANSO', 'SUSPENDIDO'].includes(c.estadoOperativo)) return false
      } else if (filterEstado.value !== 'ACTIVOS' && c.estadoOperativo !== filterEstado.value) {
        return false
      }
    }

    if (filterLicencia.value !== 'ALL' && c.tipoLicencia !== filterLicencia.value) return false
    if (!q) return true

    const haystack = [c.nombres, c.apellidos, c.nombreCompleto, c.numeroDocumento, c.numeroLicencia]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(q)
  })
})

const docTextClass = (fecha) => {
  if (isFechaVencida(fecha)) return 'sga-text-danger'
  if (isFechaPorVencer(fecha, 30)) return 'sga-text-warn'
  return ''
}

const handleToggleActivo = (conductor) => {
  const desactivar = Boolean(conductor.estadoActivo)
  const nombre = conductor.nombreCompleto || `${conductor.nombres} ${conductor.apellidos}`
  confirmAction({
    header: desactivar ? 'Dar de Baja a Conductor' : 'Reactivar Conductor',
    message: desactivar
      ? `¿Desactivar a ${nombre}? No podrá ser asignado a nuevos despachos.`
      : `¿Reactivar a ${nombre} en el directorio MTC?`,
    acceptLabel: desactivar ? 'Dar de Baja' : 'Reactivar',
    rejectLabel: 'Cancelar',
    acceptClass: desactivar ? 'p-button-danger' : undefined,
    onAccept: async () => {
      try {
        if (desactivar) {
          await transporteFlotaService.deleteConductor(conductor.idConductor)
          showSuccess(`Conductor ${nombre} dado de baja.`)
        } else {
          await transporteFlotaService.reactivarConductor(conductor.idConductor)
          showSuccess(`Conductor ${nombre} reactivado.`)
        }
        selectedRow.value = null
        emit('refresh')
      } catch (err) {
        showError(err?.response?.data?.message || err?.message || 'No se pudo actualizar el estado del conductor')
      }
    }
  })
}
</script>
