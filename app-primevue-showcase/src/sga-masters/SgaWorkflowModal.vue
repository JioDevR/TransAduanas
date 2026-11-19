<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: maxWidth, maxWidth: '95vw' }"
    @update:visible="(val) => { if (!val && !loading) $emit('close') }"
  >
    <template #header>
      <div class="wf-header">
        <div v-if="icon" class="wf-header-icon">
          <i :class="icon"></i>
        </div>
        <div>
          <div class="wf-title">{{ title }}</div>
          <div v-if="subtitle" class="wf-subtitle">{{ subtitle }}</div>
        </div>
      </div>
    </template>

    <form class="wf-form" @submit.prevent="$emit('submit')">
      <Message v-if="errorMessage" severity="error" :closable="false">
        {{ errorMessage }}
      </Message>

      <Message v-if="bannerText" :severity="bannerSeverity" :closable="false">
        <div class="wf-banner-content">
          <strong v-if="bannerTitle">{{ bannerTitle }}</strong>
          <span>{{ bannerText }}</span>
        </div>
      </Message>

      <div class="wf-grid">
        <slot />
      </div>

      <div class="wf-actions">
        <Button
          type="button"
          :label="cancelLabel"
          severity="secondary"
          outlined
          :disabled="loading"
          @click="$emit('close')"
        />
        <Button
          type="submit"
          :label="submitLabel"
          :icon="submitIcon"
          :loading="loading"
        />
      </div>
    </form>
  </Dialog>
</template>

<script setup>
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Message from 'primevue/message'

defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '' },
  maxWidth: { type: String, default: '600px' },
  loading: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
  bannerTitle: { type: String, default: '' },
  bannerText: { type: String, default: '' },
  bannerSeverity: { type: String, default: 'info' },
  cancelLabel: { type: String, default: 'Cancelar' },
  submitLabel: { type: String, default: 'Confirmar' },
  submitIcon: { type: String, default: 'pi pi-check' }
})

defineEmits(['close', 'submit'])
</script>

<style scoped>
.wf-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.wf-header-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--p-highlight-background, rgba(168, 93, 40, 0.12));
  color: var(--color-primary);
  font-size: 1.15rem;
  flex-shrink: 0;
}

.wf-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.wf-subtitle {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  margin-top: 0.15rem;
}

.wf-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.wf-banner-content {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  font-size: 0.85rem;
}

.wf-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem 1rem;
}

.wf-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}

@media (max-width: 640px) {
  .wf-grid {
    grid-template-columns: 1fr;
  }
}
</style>
