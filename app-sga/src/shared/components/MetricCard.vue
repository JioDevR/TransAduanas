<template>
  <div :class="['metric-card', { 'highlight-card': highlight }]" @click="$emit('click')">
    <div class="metric-top">
      <span v-if="index" class="metric-idx">{{ index }}</span>
      <span class="metric-label">{{ label }}</span>
    </div>
    <div :class="['metric-val', severityClass]">{{ value }}</div>
    <div v-if="sublabel || $slots.sub" class="metric-sub">
      <slot name="sub">{{ sublabel }}</slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  index: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    default: 0
  },
  sublabel: {
    type: String,
    default: ''
  },
  highlight: {
    type: Boolean,
    default: false
  },
  severity: {
    type: String,
    default: '',
    validator: (v) => ['', 'accent', 'success', 'warning', 'pending', 'danger'].includes(v)
  }
})

defineEmits(['click'])

const severityClass = computed(() => {
  if (!props.severity) return ''
  return `text-${props.severity}`
})
</script>

<style scoped>
.metric-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem 1.45rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.5rem;
  box-shadow: var(--shadow-subtle);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
}

.metric-card.highlight-card {
  border-color: var(--color-accent-kicker);
}

.metric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.metric-idx {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 6px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
}

.metric-label {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  font-weight: 600;
}

.metric-val {
  font-size: 1.85rem;
  font-weight: 700;
  color: var(--color-text-main);
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.metric-val.text-accent {
  color: var(--color-accent-kicker);
}

.metric-val.text-success {
  color: var(--color-success-text);
}

.metric-val.text-warning {
  color: var(--color-warning-text);
}

.metric-val.text-pending {
  color: #d97706;
}

.metric-val.text-danger {
  color: var(--color-danger-text);
}

.metric-sub {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
  line-height: 1.35;
}
</style>
