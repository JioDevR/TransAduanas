<template>
  <section class="sga-card-surface sga-audit-section">
    <header class="sga-audit-section-head">
      <span :class="['sga-step-num', { 'font-mono': Boolean(step) }]">
        <i v-if="icon" :class="icon"></i>
        <template v-else>{{ step }}</template>
      </span>
      <div class="sga-head-text">
        <h3 class="sga-audit-section-title">{{ title }}</h3>
        <p v-if="subtitle" class="sga-audit-section-desc">{{ subtitle }}</p>
      </div>
      <slot name="head-right" />
    </header>

    <slot />
  </section>
</template>

<script setup>
defineProps({
  step: { type: String, default: '' },
  icon: { type: String, default: '' },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped>
.sga-audit-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-audit-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.875rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
  flex-wrap: wrap;
}

.sga-head-text {
  flex: 1;
  min-width: 220px;
}

.sga-step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 9999px;
  background: var(--p-highlight-background);
  color: var(--p-highlight-color);
  font-size: 0.75rem;
  font-weight: 700;
}

.sga-audit-section-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: var(--color-text-main);
}

.sga-audit-section-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
}
</style>
