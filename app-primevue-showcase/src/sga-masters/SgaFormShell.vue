<template>
  <div class="sga-form-shell">
    <div class="shell-header-bar">
      <div class="shell-header-titles">
        <Button
          v-if="backLabel"
          type="button"
          :label="backLabel"
          icon="pi pi-arrow-left"
          text
          size="small"
          class="shell-back-btn"
          @click="$emit('cancel')"
        />

        <div class="shell-kicker-row">
          <Tag v-if="kicker" :value="kicker" severity="secondary" class="shell-kicker-tag" />
          <Tag
            :value="isEditMode ? 'MODO EDICIÓN' : 'MODO REGISTRO'"
            :icon="isEditMode ? 'pi pi-pencil' : 'pi pi-plus'"
            :severity="isEditMode ? 'warn' : 'success'"
            rounded
          />
        </div>

        <h1 class="shell-title">{{ title }}</h1>
        <p v-if="description" class="shell-desc">{{ description }}</p>
      </div>

      <div class="shell-header-actions">
        <Button
          type="button"
          :label="cancelLabel"
          severity="secondary"
          outlined
          :disabled="submitting"
          @click="$emit('cancel')"
        />
        <Button
          type="button"
          :label="submitLabel"
          :icon="submitIcon"
          :loading="submitting"
          :disabled="!isReady"
          @click="$emit('submit')"
        />
      </div>
    </div>

    <Message v-if="errorMessage" severity="error" closable @close="$emit('clear-error')">
      {{ errorMessage }}
    </Message>

    <div class="shell-content-grid">
      <div class="shell-main-column">
        <slot />
      </div>

      <aside v-if="$slots.sidebar" class="shell-sidebar-column">
        <slot name="sidebar" />
      </aside>
    </div>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'

defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  kicker: { type: String, default: '' },
  backLabel: { type: String, default: '' },
  isEditMode: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false },
  isReady: { type: Boolean, default: true },
  errorMessage: { type: String, default: '' },
  cancelLabel: { type: String, default: 'Cancelar' },
  submitLabel: { type: String, default: 'Guardar' },
  submitIcon: { type: String, default: 'pi pi-check' }
})

defineEmits(['cancel', 'submit', 'clear-error'])
</script>

<style scoped>
.sga-form-shell {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.shell-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1.25rem;
  padding: 1.25rem 1.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 14px);
  box-shadow: var(--shadow-subtle);
}

.shell-header-titles {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  max-width: 780px;
}

.shell-back-btn {
  align-self: flex-start;
  padding-left: 0;
}

.shell-kicker-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.shell-kicker-tag {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.06em;
}

.shell-title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-main);
}

.shell-desc {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.shell-header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.shell-content-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 1.25rem;
  align-items: start;
}

.shell-main-column {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.shell-sidebar-column {
  position: sticky;
  top: 5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

@media (max-width: 1024px) {
  .shell-content-grid {
    grid-template-columns: 1fr;
  }

  .shell-sidebar-column {
    position: static;
  }
}
</style>
