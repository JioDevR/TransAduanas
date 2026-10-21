<template>
  <Dialog
    :visible="visible"
    modal
    :header="modalTitle"
    :style="{ width: '880px', maxWidth: '95vw' }"
    class="custom-sga-dialog"
    @update:visible="val => !val && handleClose()"
  >
    <DocumentosUploadPanel
      mode="live"
      :entidad-tipo="entidadTipo"
      :id-entidad="idEntidad"
      :entidad-id="entidadId"
      :codigo-entidad="codigoEntidad"
      :titulo-entidad="tituloEntidad"
      @updated="$emit('updated')"
    />

    <template #footer>
      <div class="dialog-actions-row">
        <button type="button" class="btn-outline-warm" @click="handleClose">
          <span>Listo / Cerrar</span>
        </button>
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { computed } from 'vue'
import Dialog from 'primevue/dialog'
import DocumentosUploadPanel from './DocumentosUploadPanel.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  entidadTipo: {
    type: String,
    default: 'GENERAL'
  },
  idEntidad: {
    type: Number,
    default: null
  },
  entidadId: {
    type: Number,
    default: null
  },
  codigoEntidad: {
    type: String,
    default: ''
  },
  tituloEntidad: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'updated'])

const modalTitle = computed(() => {
  return `Expediente Documentario: ${props.codigoEntidad || 'General'}`
})

const handleClose = () => {
  emit('close')
}
</script>

<style scoped>
.dialog-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.5rem;
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border-radius: var(--radius-md, 8px);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast, 0.2s);
}

.btn-outline-warm:hover {
  background: var(--color-bg-subtle);
}
</style>
