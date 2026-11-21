<template>
  <header class="sga-form-header">
    <h1 class="sga-form-title">Solicitar Nueva Cotización Aduanera</h1>

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
        v-if="currentStep < 3"
        type="button"
        label="Continuar"
        icon="pi pi-arrow-right"
        iconPos="right"
        size="small"
        :disabled="submitting"
        @click="$emit('next')"
      />

      <Button
        v-else
        type="button"
        label="Enviar Solicitud"
        icon="pi pi-send"
        size="small"
        :loading="submitting"
        :disabled="!isFormReady"
        @click="$emit('submit')"
      />
    </div>
  </header>
</template>

<script setup>
import Button from 'primevue/button'

defineProps({
  currentStep: {
    type: Number,
    default: 1
  },
  submitting: {
    type: Boolean,
    default: false
  },
  isFormReady: {
    type: Boolean,
    default: false
  }
})

defineEmits(['cancel', 'next', 'submit'])
</script>
