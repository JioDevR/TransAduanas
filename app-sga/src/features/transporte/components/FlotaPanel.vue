<template>
  <SgaListShell
    v-model:viewMode="viewMode"
    title="Parque Automotor & Flota de Transporte MTC"
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
          label="Ingresar Unidad"
          icon="pi pi-plus"
          size="small"
          @click="$emit('crear')"
        />
      </div>
    </template>

    <template #sub-bar>
      <div class="sga-queue-buttons" role="tablist" aria-label="Estado operativo de flota">
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
          placeholder="Buscar por placa tracto, carreta o marca/modelo..."
          size="small"
          fluid
        />
      </IconField>

      <Select
        v-model="filterTipo"
        :options="tipoFilterOptions"
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
        label="Desactivar"
        icon="pi pi-ban"
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

    <FlotaCanvasGrid
      v-if="viewMode === 'canvas'"
      v-model:selection="selectedRow"
      :unidades="filteredUnidades"
      :loading="loading"
      @editar="$emit('editar', $event)"
    />

    <DataTable
      v-else
      v-model:selection="selectedRow"
      :value="filteredUnidades"
      :loading="loading"
      selectionMode="single"
      dataKey="idUnidad"
      paginator
      :rows="10"
      :rowsPerPageOptions="[10, 20, 50]"
      responsiveLayout="scroll"
      @row-dblclick="({ data }) => $emit('editar', data)"
    >
      <template #empty>
        <div class="sga-empty-state">
          <i class="pi pi-truck sga-empty-icon"></i>
          <p class="sga-empty-title">No se encontraron unidades</p>
          <p class="sga-empty-desc">Registre una unidad vehicular o ajuste los filtros activos.</p>
        </div>
      </template>

      <Column header="PLACA / CARRETA">
        <template #body="{ data: u }">
          <div class="sga-cell-stack">
            <span class="sga-code-cell font-mono">{{ u.placaTracto }}</span>
            <span v-if="u.placaCarreta" class="sga-cell-sub font-mono">Carreta: {{ u.placaCarreta }}</span>
          </div>
        </template>
      </Column>

      <Column header="MARCA & TIPO">
        <template #body="{ data: u }">
          <div class="sga-cell-stack">
            <span class="sga-cell-primary">{{ u.marcaModelo }}</span>
            <span class="sga-cell-sub">
              {{ formatTipoVehiculo(u.tipoVehiculo) }}
              <template v-if="u.anoFabricacion"> · {{ u.anoFabricacion }}</template>
            </span>
          </div>
        </template>
      </Column>

      <Column header="CAPACIDAD & ODÓMETRO">
        <template #body="{ data: u }">
          <div class="sga-cell-stack">
            <span class="sga-cell-meta font-mono">{{ formatPesoKg(u.capacidadCargaKg) }}</span>
            <span class="sga-cell-sub font-mono">{{ formatKilometraje(u.kilometrajeActual) }}</span>
          </div>
        </template>
      </Column>

      <Column header="SOAT / CITV">
        <template #body="{ data: u }">
          <div class="sga-cell-stack">
            <span :class="['sga-date-cell font-mono', docTextClass(u.soatVencimiento)]">
              SOAT: {{ formatDate(u.soatVencimiento) }}
            </span>
            <span :class="['sga-cell-sub font-mono', docTextClass(u.citvVencimiento)]">
              CITV: {{ formatDate(u.citvVencimiento) }}
            </span>
          </div>
        </template>
      </Column>

      <Column header="CONDICIÓN">
        <template #body="{ data: u }">
          <Tag
            :value="u.estadoActivo ? formatEstadoFlota(u.estadoOperativo) : 'Inactiva'"
            :severity="u.estadoActivo ? getEstadoFlotaSeverity(u.estadoOperativo) : 'secondary'"
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
import FlotaCanvasGrid from './FlotaCanvasGrid.vue'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { useNotification } from '@/shared/composables/useNotification'
import { transporteFlotaService } from '../services/transporteFlotaService'
import { isFechaVencida, isFechaPorVencer } from '../utils/transporteAlerts'
import {
  TIPO_VEHICULO_OPTIONS,
  formatDate,
  formatTipoVehiculo,
  formatEstadoFlota,
  getEstadoFlotaSeverity,
  formatPesoKg,
  formatKilometraje
} from '../utils/transporteFormatters'

const props = defineProps({
  unidades: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['refresh', 'crear', 'editar'])

const { confirmAction } = useConfirmation()
const { showSuccess, showError } = useNotification()

const viewMode = ref('table')
const selectedRow = ref(null)
const searchQuery = ref('')
const filterEstado = ref('ACTIVOS')
const filterTipo = ref('ALL')

const tipoFilterOptions = [
  { label: 'Todos los tipos', value: 'ALL' },
  ...TIPO_VEHICULO_OPTIONS
]

const queueTabs = computed(() => {
  const activas = props.unidades.filter((u) => u.estadoActivo !== false)
  const inactivas = props.unidades.filter((u) => u.estadoActivo === false)
  return [
    { label: 'Activas', value: 'ACTIVOS', count: activas.length, badgeSeverity: 'secondary' },
    {
      label: 'Disponibles',
      value: 'DISPONIBLE',
      count: activas.filter((u) => u.estadoOperativo === 'DISPONIBLE').length,
      badgeSeverity: 'success'
    },
    {
      label: 'En Ruta',
      value: 'EN_RUTA',
      count: activas.filter((u) => u.estadoOperativo === 'EN_RUTA').length,
      badgeSeverity: 'info'
    },
    {
      label: 'Mantenimiento',
      value: 'MANTENIMIENTO',
      count: activas.filter((u) => ['MANTENIMIENTO', 'FUERA_DE_SERVICIO'].includes(u.estadoOperativo)).length,
      badgeSeverity: 'warn'
    },
    { label: 'Inactivas', value: 'INACTIVOS', count: inactivas.length, badgeSeverity: 'secondary' }
  ]
})

const hasActiveFilters = computed(
  () => Boolean(searchQuery.value) || filterEstado.value !== 'ACTIVOS' || filterTipo.value !== 'ALL'
)

const resetFilters = () => {
  searchQuery.value = ''
  filterEstado.value = 'ACTIVOS'
  filterTipo.value = 'ALL'
}

const filteredUnidades = computed(() => {
  const q = searchQuery.value.toLowerCase()
  return props.unidades.filter((u) => {
    if (filterEstado.value === 'INACTIVOS') {
      if (u.estadoActivo !== false) return false
    } else {
      if (u.estadoActivo === false) return false
      if (filterEstado.value === 'MANTENIMIENTO') {
        if (!['MANTENIMIENTO', 'FUERA_DE_SERVICIO'].includes(u.estadoOperativo)) return false
      } else if (filterEstado.value !== 'ACTIVOS' && u.estadoOperativo !== filterEstado.value) {
        return false
      }
    }

    if (filterTipo.value !== 'ALL' && u.tipoVehiculo !== filterTipo.value) return false
    if (!q) return true

    const haystack = [u.placaTracto, u.placaCarreta, u.marcaModelo, u.tipoVehiculo]
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

const handleToggleActivo = (unidad) => {
  const desactivar = Boolean(unidad.estadoActivo)
  confirmAction({
    header: desactivar ? 'Desactivar Unidad de Flota' : 'Reactivar Unidad de Flota',
    message: desactivar
      ? `¿Desactivar la unidad ${unidad.placaTracto}? No podrá ser asignada a nuevos despachos.`
      : `¿Reactivar la unidad ${unidad.placaTracto} en el parque automotor?`,
    acceptLabel: desactivar ? 'Desactivar' : 'Reactivar',
    rejectLabel: 'Cancelar',
    acceptClass: desactivar ? 'p-button-danger' : undefined,
    onAccept: async () => {
      try {
        if (desactivar) {
          await transporteFlotaService.deleteUnidad(unidad.idUnidad)
          showSuccess(`Unidad ${unidad.placaTracto} desactivada.`)
        } else {
          await transporteFlotaService.reactivarUnidad(unidad.idUnidad)
          showSuccess(`Unidad ${unidad.placaTracto} reactivada.`)
        }
        selectedRow.value = null
        emit('refresh')
      } catch (err) {
        showError(err?.response?.data?.message || err?.message || 'No se pudo actualizar el estado de la unidad')
      }
    }
  })
}
</script>
