<template>
  <div class="catalogo-row-actions">
    <Button
      type="button"
      icon="pi pi-pencil"
      severity="secondary"
      text
      rounded
      size="small"
      :aria-label="`Editar ${label}`"
      @click="$emit('edit')"
    />
    <Button
      type="button"
      :icon="isActive ? 'pi pi-ban' : 'pi pi-refresh'"
      :severity="isActive ? 'danger' : 'success'"
      text
      rounded
      size="small"
      :aria-label="isActive ? `Desactivar ${label}` : `Reactivar ${label}`"
      @click="$emit('toggle')"
    />
  </div>
</template>

<script setup>
import Button from 'primevue/button'

defineProps({
  isActive: { type: Boolean, required: true },
  label: { type: String, default: 'registro' }
})

defineEmits(['edit', 'toggle'])
</script>

<style scoped>
.catalogo-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
</style>
