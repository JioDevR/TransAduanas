<template>
  <div class="stepper-nav-bar">
    <button
      type="button"
      :class="['stepper-step-btn', { active: currentStep === 1, completed: currentStep > 1 || !!form.idCliente }]"
      @click="$emit('update:currentStep', 1)"
    >
      <span class="step-badge">
        <i v-if="form.idCliente" class="pi pi-check"></i>
        <span v-else>1</span>
      </span>
      <div class="step-info">
        <span class="step-label">PASO 01</span>
        <span class="step-title">Cliente Solicitante</span>
      </div>
    </button>

    <div class="stepper-divider"></div>

    <button
      type="button"
      :class="['stepper-step-btn', { active: currentStep === 2, completed: currentStep > 2 || !!form.regimenAduanero }]"
      @click="$emit('update:currentStep', 2)"
    >
      <span class="step-badge">
        <i v-if="form.regimenAduanero && currentStep > 2" class="pi pi-check"></i>
        <span v-else>2</span>
      </span>
      <div class="step-info">
        <span class="step-label">PASO 02</span>
        <span class="step-title">Régimen & Plazos</span>
      </div>
    </button>

    <div class="stepper-divider"></div>

    <button
      type="button"
      :class="['stepper-step-btn', { active: currentStep === 3, completed: currentStep > 3 || docsCount > 0 }]"
      @click="$emit('update:currentStep', 3)"
    >
      <span class="step-badge">
        <i v-if="docsCount > 0 || currentStep > 3" class="pi pi-check"></i>
        <span v-else>3</span>
      </span>
      <div class="step-info">
        <span class="step-label">PASO 03</span>
        <span class="step-title">Documentación</span>
      </div>
    </button>

    <div class="stepper-divider"></div>

    <button
      type="button"
      :class="['stepper-step-btn', { active: currentStep === 4, completed: form.totalCotizado > 0 }]"
      @click="$emit('update:currentStep', 4)"
    >
      <span class="step-badge">
        <i v-if="form.totalCotizado > 0" class="pi pi-check"></i>
        <span v-else>4</span>
      </span>
      <div class="step-info">
        <span class="step-label">PASO 04</span>
        <span class="step-title">Costos & Liquidación</span>
      </div>
    </button>
  </div>
</template>

<script setup>
defineProps({
  currentStep: {
    type: Number,
    required: true
  },
  form: {
    type: Object,
    required: true
  },
  docsCount: {
    type: Number,
    default: 0
  }
})

defineEmits(['update:currentStep'])
</script>

<style scoped>
.stepper-nav-bar {
  display: flex;
  align-items: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 0.65rem 1.25rem;
  box-shadow: var(--shadow-subtle);
  gap: var(--space-4, 1rem);
  overflow-x: auto;
}

.stepper-step-btn {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
  border-radius: var(--radius-md, 0.5rem);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.stepper-step-btn:hover {
  background: var(--color-bg-subtle);
}

.step-badge {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  font-weight: 700;
  font-size: var(--font-size-xs, 0.75rem);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.stepper-step-btn.active .step-badge {
  background: var(--color-primary);
  color: var(--color-primary-text, #ffffff);
  border-color: var(--color-primary);
}

.stepper-step-btn.completed .step-badge {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.step-info {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.step-label {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
}

.step-title {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.stepper-step-btn.active .step-title {
  color: var(--color-text-main);
  font-weight: 800;
}

.stepper-divider {
  flex: 1;
  height: 2px;
  background: var(--color-border-subtle);
  min-width: 1.5rem;
}
</style>
