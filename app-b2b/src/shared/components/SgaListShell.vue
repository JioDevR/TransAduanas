<template>
  <section class="sga-list-panel">
    <div class="sga-panel-header">
      <div class="sga-panel-title-group">
        <h2 class="sga-panel-title">{{ title || (viewMode === 'canvas' ? 'Vista Canvas' : 'Vista Tabla') }}</h2>
        <SelectButton
          :modelValue="viewMode"
          :options="viewModeOptions"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
          size="small"
          aria-label="Cambiar modo de vista"
          @update:modelValue="$emit('update:viewMode', $event)"
        >
          <template #option="{ option }">
            <i :class="option.icon"></i>
            <span>{{ option.label }}</span>
          </template>
        </SelectButton>
      </div>

      <slot name="header-right" />
    </div>

    <slot name="sub-bar" />

    <div class="sga-card-surface sga-table-card">
      <div class="sga-table-toolbar">
        <div class="sga-toolbar-filters">
          <slot name="filters" />
        </div>

        <div class="sga-toolbar-actions">
          <slot name="actions" />
        </div>
      </div>

      <slot />
    </div>
  </section>
</template>

<script setup>
import SelectButton from 'primevue/selectbutton'

defineProps({
  viewMode: {
    type: String,
    default: 'canvas'
  },
  title: {
    type: String,
    default: ''
  }
})

defineEmits(['update:viewMode'])

const viewModeOptions = [
  { label: 'Canvas', value: 'canvas', icon: 'pi pi-th-large' },
  { label: 'Tabla', value: 'table', icon: 'pi pi-list' }
]
</script>
