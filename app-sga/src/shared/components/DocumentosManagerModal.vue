<template>
  <Dialog
    :visible="visible"
    modal
    :header="modalTitle"
    :style="{ width: dialogWidth, maxWidth: '95vw' }"
    class="custom-sga-dialog"
    @update:visible="(val) => !val && handleClose()"
  >
    <DocumentosUploadPanel
      mode="live"
      :entidad-tipo="entidadTipo"
      :id-entidad="resolvedEntidadId"
      :entidad-id="resolvedEntidadId"
      :codigo-entidad="codigoEntidad"
      :titulo-entidad="tituloEntidad"
      @updated="$emit('updated')"
    />

    <template #footer>
      <div class="sga-dialog-footer">
        <Button
          type="button"
          label="Listo / Cerrar"
          severity="secondary"
          outlined
          size="small"
          @click="handleClose"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import { useResponsive } from '@/shared/composables/useResponsive'
import DocumentosUploadPanel from './DocumentosUploadPanel.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  entidadTipo: { type: String, default: 'GENERAL' },
  idEntidad: { type: Number, default: null },
  entidadId: { type: Number, default: null },
  codigoEntidad: { type: String, default: '' },
  tituloEntidad: { type: String, default: '' }
})

const emit = defineEmits(['close', 'updated'])

const { getDialogWidth } = useResponsive()
const dialogWidth = computed(() => getDialogWidth('880px'))

const resolvedEntidadId = computed(() => props.entidadId ?? props.idEntidad ?? null)

const modalTitle = computed(() => {
  return `Expediente Documentario: ${props.codigoEntidad || 'General'}`
})

const handleClose = () => {
  emit('close')
}
</script>
