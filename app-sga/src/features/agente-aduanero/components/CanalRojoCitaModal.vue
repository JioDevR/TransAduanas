<template>
  <BaseModal
    :visible="visible"
    title="Canal ROJO — Solicitar Cuadrilla y Registrar Cita de Aforo"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — DAM: ${expediente?.numeroDam || 'S/N'}`"
    icon="pi pi-users"
    max-width="560px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form class="form-body" @submit.prevent="handleSubmit">
      <div class="field-item">
        <label for="rojo-cuadrilla">Cuadrilla / Personal de Inspección Física *</label>
        <InputText
          id="rojo-cuadrilla"
          v-model.trim="form.cuadrillaDetalle"
          required
          placeholder="Ej. Cuadrilla B-04 (3 estibadores + inspector de aforo)"
          fluid
        />
      </div>

      <div class="two-col">
        <div class="field-item">
          <label for="rojo-fecha">Fecha y Hora de Cita SUNAT *</label>
          <input
            id="rojo-fecha"
            v-model="form.fechaCitaInspeccion"
            type="datetime-local"
            class="text-input"
            required
          />
        </div>

        <div class="field-item">
          <label for="rojo-codigo">Código / Ticket de Cita (Opcional)</label>
          <InputText
            id="rojo-codigo"
            v-model.trim="form.codigoCitaInspeccion"
            placeholder="Ej. CITA-AFORO-9021"
            fluid
          />
        </div>
      </div>

      <div class="field-item">
        <label for="rojo-obs">Observaciones de Programación</label>
        <Textarea
          id="rojo-obs"
          v-model.trim="form.observaciones"
          rows="2"
          placeholder="Zona de reconocimiento físico en almacén temporal..."
          fluid
        />
      </div>

      <div class="modal-actions">
        <Button type="button" label="Cancelar" severity="secondary" outlined @click="handleClose" :disabled="loading" />
        <Button type="submit" label="Solicitar Cuadrilla y Registrar Cita" icon="pi pi-calendar-plus" severity="danger" :loading="loading" />
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
  cuadrillaDetalle: '',
  fechaCitaInspeccion: '',
  codigoCitaInspeccion: '',
  observaciones: ''
})

watch(
  () => props.visible,
  (vis) => {
    if (vis) {
      errorMessage.value = ''
      form.value = {
        cuadrillaDetalle: props.expediente?.cuadrillaDetalle || 'Cuadrilla de Aforo Físico TransAduanas',
        fechaCitaInspeccion: getNowDateTimeInput(),
        codigoCitaInspeccion: props.expediente?.codigoCitaInspeccion || '',
        observaciones: ''
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
    const res = await expedienteService.solicitarCuadrillaYCitaRojo(props.expediente.idExpediente, {
      cuadrillaDetalle: form.value.cuadrillaDetalle,
      fechaCitaInspeccion: toLocalDateTimePayload(form.value.fechaCitaInspeccion),
      codigoCitaInspeccion: form.value.codigoCitaInspeccion || null,
      observaciones: form.value.observaciones || null
    })
    emit('saved', res)
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || 'No se pudo registrar la cuadrilla y cita.'
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
