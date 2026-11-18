<template>
  <div class="agente-header-toolbar">
    <div class="toolbar-left-info">
      <span class="badge-kicker">DESPACHO ADUANERO // BANDEJA DE CAMPO</span>
      <h3 class="toolbar-title">Bandeja de Expedientes Asignados</h3>
    </div>

    <div class="toolbar-controls-group">
      <!-- Conmutador de Vista: Canvas vs Tabla -->
      <SelectButton
        :modelValue="viewMode"
        :options="viewModeOptions"
        optionLabel="label"
        optionValue="value"
        :allowEmpty="false"
        size="small"
        @update:modelValue="$emit('update:viewMode', $event)"
      >
        <template #option="{ option }">
          <i :class="option.icon"></i>
          <span>{{ option.label }}</span>
        </template>
      </SelectButton>

      <!-- Selector de Agente (Exclusivo para Supervisores y Administradores) -->
      <div v-if="esSupervisorOAdmin" class="supervisor-select-wrap">
        <label for="filter-agente-sel" class="supervisor-label">
          <i class="pi pi-user text-xs"></i>
          <span>Auditar:</span>
        </label>
        <Select
          inputId="filter-agente-sel"
          :modelValue="selectedAgenteId"
          :options="agenteOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          @update:modelValue="$emit('update:selectedAgenteId', $event)"
        />
      </div>

      <!-- Filtro por Régimen -->
      <Select
        :modelValue="filterRegimen"
        :options="regimenOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        @update:modelValue="$emit('update:filterRegimen', $event)"
      />

      <!-- Filtro por Canal SUNAT -->
      <Select
        :modelValue="filterCanal"
        :options="canalOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        @update:modelValue="$emit('update:filterCanal', $event)"
      />

      <!-- Búsqueda rápida -->
      <IconField>
        <InputIcon class="pi pi-search" />
        <InputText
          :modelValue="searchQuery"
          size="small"
          placeholder="Buscar expediente, DAM, cliente, nave..."
          @update:modelValue="$emit('update:searchQuery', $event)"
        />
      </IconField>

      <!-- Botón de refresco manual -->
      <Button
        type="button"
        icon="pi pi-refresh"
        severity="secondary"
        outlined
        size="small"
        title="Actualizar datos de la bandeja"
        :loading="loading"
        @click="$emit('refresh')"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import SelectButton from 'primevue/selectbutton'

const viewModeOptions = [
  { label: 'Canvas', value: 'canvas', icon: 'pi pi-th-large' },
  { label: 'Tabla', value: 'table', icon: 'pi pi-list' }
]

const regimenOptions = [
  { label: 'Todos los Regímenes', value: '' },
  { label: 'Importación para el Consumo', value: 'IMPORTACION' },
  { label: 'Exportación Definitiva', value: 'EXPORTACION' }
]

const canalOptions = [
  { label: 'Todos los Canales', value: '' },
  { label: 'Canal VERDE (Libre)', value: 'VERDE' },
  { label: 'Canal NARANJA (Documental)', value: 'NARANJA' },
  { label: 'Canal ROJO (Aforo Físico)', value: 'ROJO' },
  { label: 'Canal Pendiente', value: 'PENDIENTE' }
]

const props = defineProps({
  viewMode: {
    type: String,
    default: 'canvas'
  },
  searchQuery: {
    type: String,
    default: ''
  },
  filterRegimen: {
    type: String,
    default: ''
  },
  filterCanal: {
    type: String,
    default: ''
  },
  selectedAgenteId: {
    type: [Number, String, null],
    default: null
  },
  agentesList: {
    type: Array,
    default: () => []
  },
  esSupervisorOAdmin: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits([
  'update:viewMode',
  'update:searchQuery',
  'update:filterRegimen',
  'update:filterCanal',
  'update:selectedAgenteId',
  'refresh'
])

const agenteOptions = computed(() => [
  { label: '★ Todos los Agentes', value: null },
  ...props.agentesList.map((ag) => ({
    label: `${ag.nombre} ${ag.apellidos || ''}`.trim(),
    value: ag.idUsuario
  }))
])
</script>

<style scoped>
.agente-header-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 0.85rem 1.25rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  box-shadow: var(--shadow-subtle);
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.badge-kicker {
  font-family: var(--font-sans);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  text-transform: uppercase;
}

.toolbar-title {
  margin: 0;
  font-family: var(--font-sans);
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.01em;
}

.toolbar-controls-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

/* Conmutador de Vista */
.view-mode-toggle {
  display: inline-flex;
  align-items: center;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  padding: 2px;
}

.toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  border-radius: 4px;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.toggle-btn:hover {
  color: var(--color-text-main);
}

.toggle-btn.active {
  background: var(--color-primary);
  color: var(--color-primary-text);
  box-shadow: var(--shadow-subtle);
}

.supervisor-select-wrap {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0 0.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  height: 38px;
}

.supervisor-label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.supervisor-select {
  height: 100%;
  border: none;
  background-color: var(--color-bg-surface);
  padding-right: 2.25rem;
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  color: var(--color-text-main);
  outline: none;
  cursor: pointer;
}

.filter-select {
  height: 38px;
  padding: 0 2.25rem 0 0.75rem;
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 500;
  color: var(--color-text-main);
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  outline: none;
  cursor: pointer;
  transition: border-color var(--transition-fast, 0.15s ease);
}

.filter-select:focus {
  border-color: var(--color-primary);
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-subtle);
  pointer-events: none;
}

.search-input {
  height: 38px;
  width: 230px;
  padding: 0 0.75rem 0 2.25rem;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  outline: none;
  transition: border-color var(--transition-fast, 0.15s ease), width 0.2s ease;
}

.search-input::placeholder {
  color: var(--color-text-subtle);
}

.search-input:focus {
  border-color: var(--color-primary);
  width: 260px;
}

.refresh-btn {
  height: 38px;
  width: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.refresh-btn:hover:not(:disabled) {
  color: var(--color-text-main);
  border-color: var(--color-border-strong);
  background: var(--color-bg-subtle);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
