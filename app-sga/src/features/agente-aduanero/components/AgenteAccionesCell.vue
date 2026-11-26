<template>
  <div class="sga-agente-actions">
    <template v-if="mode === 'readonly'">
      <Button
        type="button"
        label="Ver Ficha"
        icon="pi pi-eye"
        severity="secondary"
        outlined
        size="small"
        title="Consultar ficha completa del expediente en modo lectura"
        @click.stop="$emit('detalle', expediente)"
      />
    </template>

    <template v-else>
      <Button
        type="button"
        icon="pi pi-paperclip"
        severity="secondary"
        outlined
        size="small"
        title="Bóveda de Documentos Digitales"
        aria-label="Documentos"
        @click.stop="$emit('documentos', expediente)"
      />

      <Button
        type="button"
        label="Gestionar"
        icon="pi pi-sliders-h"
        size="small"
        title="Abrir estación de hitos y acciones del expediente"
        @click.stop="$emit('detalle', expediente)"
      />
    </template>
  </div>
</template>

<script setup>
import Button from 'primevue/button'

defineProps({
  expediente: { type: Object, required: true },
  mode: {
    type: String,
    default: 'manage' // 'readonly' | 'manage'
  }
})

defineEmits(['documentos', 'detalle'])
</script>

<style scoped>
.sga-agente-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.45rem;
  flex-wrap: wrap;
}
</style>
