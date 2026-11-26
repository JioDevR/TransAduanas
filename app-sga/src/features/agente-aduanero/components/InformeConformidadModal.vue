<template>
  <BaseModal
    :visible="visible"
    title="Redactar Informe de Conformidad de Entrega al Cliente"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — ${expediente?.clienteRazonSocial || ''}`"
    icon="pi pi-file-check"
    max-width="580px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form class="form-body" @submit.prevent="handleSubmit">
      <div class="two-col">
        <div class="field-item">
          <label for="conf-receptor">Nombre del Receptor Conforme *</label>
          <InputText
            id="conf-receptor"
            v-model.trim="form.receptorNombre"
            required
            placeholder="Ej. Carlos Mendoza (Jefe de Almacén)"
            fluid
          />
        </div>

        <div class="field-item">
          <label for="conf-dni">DNI / Documento</label>
          <InputText
            id="conf-dni"
            v-model.trim="form.receptorDni"
            placeholder="Ej. 42918273"
            fluid
          />
        </div>
      </div>

      <div class="field-item">
        <label for="conf-fecha">Fecha y Hora de Entrega Conforme *</label>
        <input
          id="conf-fecha"
          v-model="form.fechaEntrega"
          type="datetime-local"
          class="text-input"
          required
        />
      </div>

      <div class="field-item">
        <label for="conf-detalle">Redacción del Informe de Conformidad *</label>
        <Textarea
          id="conf-detalle"
          v-model.trim="form.detalleConformidad"
          rows="4"
          required
          placeholder="Se deja constancia de la entrega total y conforme de la mercancía en las instalaciones del cliente, verificando precintos y bultos sin daños..."
          fluid
        />
      </div>

      <div class="modal-actions">
        <Button type="button" label="Cancelar" severity="secondary" outlined @click="handleClose" :disabled="loading" />
        <Button type="submit" label="Emitir Informe de Conformidad" icon="pi pi-check" severity="success" :loading="loading" />
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
import { getNowDateTimeInput, toLocalDateTimePayload } from '@/shared/utils/dateUtils'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')
const form = ref({
  receptorNombre: '',
  receptorDni: '',
  fechaEntrega: '',
  detalleConformidad: ''
})

watch(
  () => props.visible,
  (vis) => {
    if (vis) {
      errorMessage.value = ''
      form.value = {
        receptorNombre: props.expediente?.receptorConformidadNombre || props.expediente?.clienteRazonSocial || '',
        receptorDni: props.expediente?.receptorConformidadDni || '',
        fechaEntrega: getNowDateTimeInput(),
        detalleConformidad:
          props.expediente?.informeConformidadTexto ||
          'Se certifica la entrega conforme de la mercancía al cliente en destino final, verificando integridad de bultos, documentación aduanera y cierre operativo satisfactorio.'
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
    const res = await expedienteService.emitirInformeConformidad(props.expediente.idExpediente, {
      receptorNombre: form.value.receptorNombre,
      receptorDni: form.value.receptorDni || null,
      fechaEntrega: toLocalDateTimePayload(form.value.fechaEntrega),
      detalleConformidad: form.value.detalleConformidad
    })
    emit('saved', res)
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || 'No se pudo emitir el informe de conformidad.'
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
.two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
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
.text-input {
  width: 100%;
  padding: 0.5rem 0.65rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
}
</style>
