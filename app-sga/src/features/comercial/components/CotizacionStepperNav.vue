<template>
  <nav class="sga-stepper-bar" aria-label="Pasos de la cotización">
    <button
      v-for="step in steps"
      :key="step.num"
      type="button"
      :class="[
        'sga-stepper-item',
        {
          'is-active': currentStep === step.num,
          'is-completed': currentStep !== step.num && step.completed
        }
      ]"
      @click="$emit('update:currentStep', step.num)"
    >
      <span class="sga-step-circle">
        <i v-if="currentStep > step.num && step.completed" class="pi pi-check"></i>
        <span v-else>{{ step.num }}</span>
      </span>

      <div class="sga-step-meta">
        <span class="sga-step-kicker">{{ step.kicker }}</span>
        <span class="sga-step-title">{{ step.title }}</span>
      </div>
    </button>
  </nav>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
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

const steps = computed(() => [
  {
    num: 1,
    kicker: 'PASO 01',
    title: 'Cliente Solicitante',
    completed: !!props.form.idCliente
  },
  {
    num: 2,
    kicker: 'PASO 02',
    title: 'Régimen & Plazos',
    completed: !!props.form.regimenAduanero
  },
  {
    num: 3,
    kicker: 'PASO 03',
    title: 'Documentación',
    completed: props.docsCount > 0 || props.currentStep > 3
  },
  {
    num: 4,
    kicker: 'PASO 04',
    title: 'Costos & Liquidación',
    completed: Number(props.form.totalCotizado) > 0
  }
])
</script>

<style scoped>
.sga-stepper-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 0.75rem;
  padding: 0.85rem var(--sga-card-padding-x, 2rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--sga-card-radius, 14px);
  overflow-x: auto;
}

.sga-stepper-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: transparent;
  border: none;
  padding: 0.35rem 0.5rem;
  border-radius: 10px;
  cursor: pointer;
  text-align: left;
  transition: background-color var(--transition-fast);
}

.sga-stepper-item:hover {
  background: var(--color-bg-subtle);
}

.sga-step-circle {
  width: 26px;
  height: 26px;
  border-radius: 9999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  flex-shrink: 0;
  border: 1px solid var(--color-border-strong);
  color: var(--color-text-subtle);
  background: transparent;
  transition: all var(--transition-fast);
}

.sga-step-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sga-step-kicker {
  font-size: 0.625rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--color-text-subtle);
  line-height: 1.2;
}

.sga-step-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-subtle);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Activo */
.sga-stepper-item.is-active .sga-step-circle {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
}

.sga-stepper-item.is-active .sga-step-kicker {
  color: var(--color-text-muted);
}

.sga-stepper-item.is-active .sga-step-title {
  color: var(--color-text-main);
  font-weight: 700;
}

/* Completado */
.sga-stepper-item.is-completed .sga-step-circle {
  border-color: var(--color-success-text);
  color: var(--color-success-text);
  background: transparent;
}

.sga-stepper-item.is-completed .sga-step-kicker {
  color: var(--color-success-text);
}

.sga-stepper-item.is-completed .sga-step-title {
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .sga-stepper-bar {
    grid-template-columns: repeat(4, minmax(180px, 1fr));
  }
}
</style>
