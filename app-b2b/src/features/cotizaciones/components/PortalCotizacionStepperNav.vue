<template>
  <nav class="sga-stepper-bar portal-stepper-3" aria-label="Pasos de la solicitud">
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
    title: 'Régimen & Logística',
    completed: Boolean(props.form?.regimenAduanero && props.form?.idPuertoEnvio && props.form?.idPuertoDestino)
  },
  {
    num: 2,
    kicker: 'PASO 02',
    title: 'Documentación',
    completed: props.docsCount > 0 || props.currentStep > 2
  },
  {
    num: 3,
    kicker: 'PASO 03',
    title: 'Valores Declarados',
    completed: Number(props.form?.valorFob) > 0
  }
])
</script>

<style scoped>
.portal-stepper-3 {
  grid-template-columns: repeat(3, minmax(160px, 1fr));
}

@media (max-width: 768px) {
  .portal-stepper-3 {
    grid-template-columns: repeat(3, minmax(180px, 1fr));
  }
}
</style>
