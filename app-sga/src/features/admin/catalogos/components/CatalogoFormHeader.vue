<template>
  <header class="sga-card-surface sga-form-header">
    <div class="catalogo-form-head-info">
      <div class="catalogo-form-pills">
        <Tag
          :value="isEditMode ? 'MODO EDICIÓN' : 'MODO REGISTRO'"
          :icon="isEditMode ? 'pi pi-pencil' : 'pi pi-plus'"
          :severity="isEditMode ? 'warn' : 'success'"
          rounded
        />
        <span class="sga-kicker">{{ kicker }}</span>
      </div>
      <p class="catalogo-form-subtitle">{{ subtitle }}</p>
    </div>

    <div class="sga-form-actions">
      <Button
        type="button"
        label="Volver al Listado"
        icon="pi pi-arrow-left"
        severity="secondary"
        outlined
        size="small"
        :disabled="submitting"
        @click="$emit('cancel')"
      />
      <Button
        type="button"
        :label="submitLabel"
        icon="pi pi-check"
        size="small"
        :loading="submitting"
        :disabled="submitting || !ready"
        @click="$emit('submit')"
      />
    </div>
  </header>
</template>

<script setup>
import Button from 'primevue/button'
import Tag from 'primevue/tag'

defineProps({
  isEditMode: { type: Boolean, default: false },
  kicker: { type: String, required: true },
  subtitle: { type: String, required: true },
  submitLabel: { type: String, required: true },
  submitting: { type: Boolean, default: false },
  ready: { type: Boolean, default: false }
})

defineEmits(['cancel', 'submit'])
</script>

<style scoped>
.catalogo-form-head-info {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.catalogo-form-pills {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.catalogo-form-subtitle {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--sga-text-primary);
}
</style>
