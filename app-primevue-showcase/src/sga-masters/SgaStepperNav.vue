<template>
  <div class="sga-stepper-bar">
    <template v-for="(step, idx) in steps" :key="step.id">
      <button
        type="button"
        :class="[
          'stepper-item',
          {
            active: modelValue === step.id,
            completed: step.completed && modelValue !== step.id
          }
        ]"
        @click="$emit('update:modelValue', step.id)"
      >
        <span class="step-badge">
          <i v-if="step.completed && modelValue !== step.id" class="pi pi-check"></i>
          <span v-else>{{ idx + 1 }}</span>
        </span>
        <div class="step-meta">
          <span class="step-kicker">PASO 0{{ idx + 1 }}</span>
          <span class="step-title">{{ step.title }}</span>
        </div>
      </button>

      <div v-if="idx < steps.length - 1" class="step-divider"></div>
    </template>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: [Number, String], required: true },
  steps: {
    type: Array,
    required: true
  }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.sga-stepper-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.85rem 1.15rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 14px);
  overflow-x: auto;
}

.stepper-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.5rem 0.85rem;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all 0.15s ease;
  text-align: left;
  white-space: nowrap;
}

.stepper-item:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.stepper-item.active {
  background: var(--p-highlight-background, rgba(168, 93, 40, 0.12));
  border-color: var(--color-primary);
  color: var(--color-text-main);
}

.step-badge {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: 0.78rem;
  font-weight: 700;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
}

.stepper-item.active .step-badge {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
}

.stepper-item.completed .step-badge {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.step-meta {
  display: flex;
  flex-direction: column;
}

.step-kicker {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  opacity: 0.75;
}

.step-title {
  font-size: 0.85rem;
  font-weight: 600;
}

.step-divider {
  flex: 1;
  min-width: 16px;
  height: 1px;
  background: var(--color-border);
}
</style>
