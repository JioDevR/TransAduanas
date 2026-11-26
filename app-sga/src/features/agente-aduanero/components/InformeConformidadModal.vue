<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('580px'), maxWidth: '95vw' }"
    header="Emitir Informe de Conformidad de Entrega"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="informe-conformidad-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span v-if="expediente.clienteRazonSocial" class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial }}
          </span>
        </div>
        <Tag
          :value="expediente.numeroDam ? `DAM ${expediente.numeroDam}` : 'Conformidad'"
          severity="info"
          class="font-mono sga-mini-tag"
          rounded
        />
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label for="conf-receptor" class="sga-form-label">Nombre del Receptor Conforme *</label>
          <InputText
            id="conf-receptor"
            v-model.trim="form.receptorNombre"
            required
            placeholder="Ej. Carlos Mendoza (Jefe de Almacén)"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="conf-dni" class="sga-form-label">DNI / Documento</label>
          <InputText
            id="conf-dni"
            v-model.trim="form.receptorDni"
            placeholder="Ej. 42918273"
            class="font-mono"
            size="small"
            fluid
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label for="conf-fecha" class="sga-form-label">Fecha y Hora de Entrega Conforme *</label>
        <InputText
          id="conf-fecha"
          v-model="form.fechaEntrega"
          type="datetime-local"
          class="font-mono"
          required
          size="small"
          fluid
        />
      </div>

      <div class="sga-form-field">
        <label for="conf-detalle" class="sga-form-label">Redacción del Informe de Conformidad *</label>
        <Textarea
          id="conf-detalle"
          v-model.trim="form.detalleConformidad"
          rows="4"
          autoResize
          required
          placeholder="Se deja constancia de la entrega total y conforme de la mercancía en las instalaciones del cliente, verificando precintos y bultos sin daños..."
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
        form="informe-conformidad-form"
        label="Emitir Informe de Conformidad"
        icon="pi pi-check"
        severity="success"
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
