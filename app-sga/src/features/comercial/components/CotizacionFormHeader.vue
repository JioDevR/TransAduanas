<template>
  <header class="sga-form-header">
    <h1 class="sga-form-title">{{ titleText }}</h1>

    <div class="sga-form-actions">
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


