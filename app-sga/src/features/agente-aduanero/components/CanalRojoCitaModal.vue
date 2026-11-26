<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('560px'), maxWidth: '95vw' }"
    header="Canal ROJO — Cuadrilla y Cita de Aforo"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="canal-rojo-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span v-if="expediente.clienteRazonSocial" class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial }}
          </span>
        </div>
        <Tag
          :value="expediente.numeroDam ? `DAM ${expediente.numeroDam}` : 'DAM S/N'"
          severity="danger"
          class="font-mono sga-mini-tag"
          rounded
        />
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-form-field">
        <label for="rojo-cuadrilla" class="sga-form-label">Cuadrilla / Personal de Inspección Física *</label>
        <InputText
          id="rojo-cuadrilla"
          v-model.trim="form.cuadrillaDetalle"
          required
          placeholder="Ej. Cuadrilla B-04 (3 estibadores + inspector de aforo)"
          size="small"
          fluid
        />
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label for="rojo-fecha" class="sga-form-label">Fecha y Hora de Cita SUNAT *</label>
          <InputText
            id="rojo-fecha"
            v-model="form.fechaCitaInspeccion"
            type="datetime-local"
            class="font-mono"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="rojo-codigo" class="sga-form-label">Código / Ticket de Cita (Opcional)</label>
          <InputText
            id="rojo-codigo"
            v-model.trim="form.codigoCitaInspeccion"
            placeholder="Ej. CITA-AFORO-9021"
            class="font-mono uppercase"
            size="small"
            fluid
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label for="rojo-obs" class="sga-form-label">Observaciones de Programación</label>
        <Textarea
          id="rojo-obs"
          v-model.trim="form.observaciones"
          rows="2"
          autoResize
          placeholder="Zona de reconocimiento físico en almacén temporal..."
          size="small"
          fluid
        />
      </div>
    </form>

    <template #footer>
      <Button
        type="button"
        label="Cancelar"
        severity="secondary"
        outlined
        size="small"
        :disabled="loading"
        @click="handleClose"
      />
      <Button
        type="submit"
        form="canal-rojo-form"
        label="Solicitar Cuadrilla y Registrar Cita"
        icon="pi pi-calendar-plus"
        severity="danger"
        size="small"
        :loading="loading"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { expedienteService } from '@/shared/services/expedienteService'
import { getNowDateTimeInput, toLocalDateTimePayload } from '@/shared/utils/dateUtils'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()

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
.uppercase {
  text-transform: uppercase;
}
</style>
