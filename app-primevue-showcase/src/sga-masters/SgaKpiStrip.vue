<template>
  <div class="sga-kpi-strip">
    <button
      v-for="item in items"
      :key="item.key"
      type="button"
      :class="['kpi-card', `sev-${item.severity || 'neutral'}`, { active: activeKey === item.key }]"
      @click="$emit('select', activeKey === item.key ? null : item.key)"
    >
      <div class="kpi-top">
        <span class="kpi-label">{{ item.label }}</span>
        <i v-if="item.icon" :class="['kpi-icon', item.icon]"></i>
      </div>
      <div class="kpi-value font-mono">{{ item.value }}</div>
      <div v-if="item.hint" class="kpi-hint">{{ item.hint }}</div>
    </button>
  </div>
</template>

<script setup>
defineProps({
  items: { type: Array, required: true },
  activeKey: { type: String, default: null }
})

defineEmits(['select'])
</script>

<style scoped>
.sga-kpi-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 0.85rem;
}

.kpi-card {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 1rem 1.1rem;
  border-radius: var(--radius-lg, 14px);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  text-align: left;
  cursor: pointer;
  transition: transform 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
}

.kpi-card:hover {
  transform: translateY(-1px);
  border-color: var(--color-primary);
  box-shadow: var(--shadow-subtle);
}

.kpi-card.active {
  border-color: var(--color-primary);
  background: var(--p-highlight-background, rgba(168, 93, 40, 0.1));
}

.kpi-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.kpi-label {
  font-size: 0.76rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.kpi-icon {
  font-size: 1rem;
  color: var(--color-primary);
}

.kpi-value {
  font-size: 1.55rem;
  font-weight: 700;
  color: var(--color-text-main);
  font-variant-numeric: tabular-nums;
}

.kpi-hint {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sev-success .kpi-icon { color: var(--color-success-text); }
.sev-warn .kpi-icon { color: var(--color-warning-text); }
.sev-danger .kpi-icon { color: var(--color-danger-text); }
.sev-info .kpi-icon { color: var(--color-info-text); }
</style>
