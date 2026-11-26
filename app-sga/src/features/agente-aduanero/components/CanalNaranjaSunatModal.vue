<template>
  <BaseModal
    :visible="visible"
    title="Canal NARANJA — Envío Simulado de Documentos a SUNAT"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — DAM: ${expediente?.numeroDam || 'S/N'}`"
    icon="pi pi-send"
    max-width="560px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form class="form-body" @submit.prevent="handleSubmit">
      <div class="field-item">
        <label for="sunat-docs">Documentos Transmitidos a SUNAT (Teledespacho Simulado) *</label>
        <InputText
          id="sunat-docs"
          v-model.trim="form.documentosIncluidos"
          required
          placeholder="Factura Comercial, Packing List, BL/AWB, Certificado de Origen"
          fluid
        />
      </div>

      <div class="field-item">
        <label for="sunat-obs">Sustento / Observaciones para Especialista SUNAT</label>
        <Textarea
          id="sunat-obs"
          v-model.trim="form.observaciones"
          rows="3"
          placeholder="Documentación sustentatoria completa conforme a subpartida arancelaria..."
          fluid
        />
      </div>

      <div class="modal-actions">
        <Button type="button" label="Cancelar" severity="secondary" outlined @click="handleClose" :disabled="loading" />
        <Button type="submit" label="Enviar Documentos a SUNAT" icon="pi pi-cloud-upload" severity="warn" :loading="loading" />
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import BaseModal from '@/shared/components/BaseModal.vue'
import { expedienteService } from '@/shared/services/expedienteService'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')
const form = ref({
  documentosIncluidos: 'Factura Comercial, Packing List, BL/AWB y DAM',
  observaciones: ''
})

watch(
  () => props.visible,
  (vis) => {
    if (vis) {
      errorMessage.value = ''
      form.value = {
        documentosIncluidos: 'Factura Comercial, Packing List, BL/AWB y DAM',
        observaciones: 'Revisión documentaria Canal Naranja transmitida directamente a SUNAT.'
      }
    }
  }
)

const handleClose = () => {
  errorMessage.value = ''
  emit('close')
}

const handleSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await expedienteService.enviarSunatSimulado(props.expediente.idExpediente, form.value)
    emit('saved', res)
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || 'No se pudo transmitir documentos a SUNAT.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.form-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.field-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.field-item label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-main);
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
}
</style>
