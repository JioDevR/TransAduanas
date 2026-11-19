<template>
  <Card class="sga-form-block">
    <template #title>
      <div class="block-header">
        <span class="block-index">{{ index }}</span>
        <div>
          <div class="block-title">{{ title }}</div>
          <p v-if="subtitle" class="block-subtitle">{{ subtitle }}</p>
        </div>
      </div>
    </template>
    <template #content>
      <div class="block-body">
        <slot />
      </div>
    </template>
  </Card>
</template>

<script setup>
import Card from 'primevue/card'

defineProps({
  index: { type: String, required: true },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped>
.block-header {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
}

.block-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--color-primary);
  color: var(--color-primary-text);
  font-family: var(--font-mono);
  font-size: 0.78rem;
  font-weight: 700;
  flex-shrink: 0;
}

.block-title {
  font-size: 1.02rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.block-subtitle {
  margin: 0.2rem 0 0;
  font-size: 0.82rem;
  font-weight: 400;
  color: var(--color-text-muted);
}

.block-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
</style>
