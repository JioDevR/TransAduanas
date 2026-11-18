<template>
  <header class="sga-wizard-header">
    <h1 class="sga-wizard-title">{{ titleText }}</h1>

    <div class="sga-wizard-actions">
      <Button
        type="button"
        label="Cancelar"
        severity="secondary"
        outlined
        size="small"
        :disabled="submitting"
        @click="$emit('cancel')"
      />

      <Button
        v-if="currentStep < 4"
        type="button"
        label="Continuar"
        icon="pi pi-arrow-right"
        iconPos="right"
        size="small"
        :disabled="submitting || !canContinue"
        @click="$emit('next')"
      />

      <Button
        v-else
        type="button"
        :label="submitLabel"
        :icon="isUnassignedClaim ? 'pi pi-user-plus' : 'pi pi-check'"
        size="small"
        :loading="submitting"
        :disabled="!isFormReady"
        @click="$emit('submit')"
      />
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'

const props = defineProps({
  currentStep: {
    type: Number,
    default: 1
  },
  isEditMode: {
    type: Boolean,
    default: false
  },
  isUnassignedClaim: {
    type: Boolean,
    default: false
  },
  codigoCotizacion: {
    type: String,
    default: ''
  },
  submitting: {
    type: Boolean,
    default: false
  },
  isFormReady: {
    type: Boolean,
    default: false
  },
  canContinue: {
    type: Boolean,
    default: true
  }
})

defineEmits(['cancel', 'next', 'submit'])

const titleText = computed(() => {
  if (props.isUnassignedClaim) {
    return `Liquidar y Tomar Cotización: ${props.codigoCotizacion}`
  }
  if (props.isEditMode) {
    return `Editar Cotización: ${props.codigoCotizacion}`
  }
  return 'Emitir Nueva Cotización Comercial'
})

const submitLabel = computed(() => {
  if (props.isUnassignedClaim) return 'Tomar y Guardar Cotización'
  if (props.isEditMode) return 'Guardar Cambios'
  return 'Emitir Cotización'
})
</script>

<style scoped>
.sga-wizard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.sga-wizard-title {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-main);
}

.sga-wizard-actions {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}
</style>
