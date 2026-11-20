<template>
  <section class="sga-step-card">
    <DocumentosUploadPanel
      :mode="isUnassignedClaim ? 'deferred' : (isEditMode ? 'live' : 'deferred')"
      :modelValue="queuedDocs"
      :entidad-tipo="'COTIZACION'"
      :id-entidad="cotizacionId"
      :entidad-id="cotizacionId"
      :codigo-entidad="codigoCotizacion || 'NUEVA'"
      :titulo-entidad="clientName || 'Propuesta Comercial'"
      :permiso-requerido="permisoRequerido"
      :entidad-reguladora="entidadReguladora"
      :hide-header-banner="true"
      @update:modelValue="$emit('update:queuedDocs', $event)"
    />
  </section>
</template>

<script setup>
import DocumentosUploadPanel from '@/shared/components/DocumentosUploadPanel.vue'

defineProps({
  queuedDocs: { type: Array, default: () => [] },
  isUnassignedClaim: { type: Boolean, default: false },
  isEditMode: { type: Boolean, default: false },
  cotizacionId: { type: [Number, String], default: null },
  codigoCotizacion: { type: String, default: '' },
  clientName: { type: String, default: '' },
  permisoRequerido: { type: Boolean, default: false },
  entidadReguladora: { type: String, default: '' }
})

defineEmits(['update:queuedDocs'])
</script>

<style scoped>
.sga-step-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--sga-card-radius, 14px);
  padding: var(--sga-card-padding-y, 1.75rem) var(--sga-card-padding-x, 2rem);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
</style>
