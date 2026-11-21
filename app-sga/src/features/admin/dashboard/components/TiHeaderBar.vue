<template>
  <nav class="ti-tabs-bar" aria-label="Vistas del Dashboard TI">
    <SelectButton
      :modelValue="activeTab"
      :options="tabOptions"
      optionLabel="label"
      optionValue="value"
      :allowEmpty="false"
      size="small"
      @update:modelValue="$emit('update:activeTab', $event)"
    />
  </nav>
</template>

<script setup>
import SelectButton from 'primevue/selectbutton'

defineProps({
  activeTab: { type: String, default: 'resumen' },
  tabOptions: { type: Array, default: () => [] }
})

defineEmits(['update:activeTab'])
</script>

<style scoped>
.ti-tabs-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}
</style>
