<template>
  <Card class="sga-summary-card">
    <template #title>
      <div class="summary-header">
        <Tag v-if="badge" :value="badge" severity="secondary" class="summary-badge" />
        <div class="summary-title">{{ title }}</div>
      </div>
    </template>
    <template #content>
      <div class="summary-rows">
        <div v-for="item in items" :key="item.label" class="summary-row">
          <span class="summary-label">{{ item.label }}</span>
          <span :class="['summary-value', { 'font-mono': item.mono }]">
            {{ item.value || '—' }}
          </span>
        </div>
      </div>
      <div v-if="$slots.footer" class="summary-footer">
        <slot name="footer" />
      </div>
    </template>
  </Card>
</template>

<script setup>
import Card from 'primevue/card'
import Tag from 'primevue/tag'

defineProps({
  title: { type: String, required: true },
  badge: { type: String, default: 'FICHA EN VIVO' },
  items: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.summary-header {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.summary-badge {
  align-self: flex-start;
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.06em;
}

.summary-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.summary-rows {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
  padding-bottom: 0.55rem;
  border-bottom: 1px dashed var(--color-border-subtle);
}

.summary-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.summary-label {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.summary-value {
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--color-text-main);
  text-align: right;
}

.summary-footer {
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}
</style>
