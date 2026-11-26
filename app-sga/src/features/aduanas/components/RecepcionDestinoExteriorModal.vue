<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('620px'), maxWidth: '95vw' }"
    header="Confirmar Recepción de Carga en País de Destino"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="recepcion-destino-exterior-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial || 'Exportador asignado' }}
          </span>
        </div>
        <Tag
          value="DESTINO EXTERIOR"
          severity="success"
          class="sga-mini-tag"
          rounded
        />
      </div>

      <div class="sga-banner is-success">
        <i class="pi pi-check-square"></i>
        <div>
          <strong>Entrega Internacional Completada</strong>
          <p class="recepcion-banner-desc">
            Registre la conformidad del consignatario en destino exterior para cerrar la trazabilidad logística de exportación.
          </p>
        </div>
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field full-span">
          <label for="recep-consignatario" class="sga-form-label">Consignatario / Receptor en Destino Exterior *</label>
          <InputText
            id="recep-consignatario"
            v-model.trim="form.consignatarioExterior"
            placeholder="Nombre de la empresa compradora o agente en destino"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="recep-doc" class="sga-form-label">Documento de Entrega / Recibo *</label>
          <InputText
            id="recep-doc"
            v-model.trim="form.documentoRecepcion"
            placeholder="Ej. POD-EXT-9910 / BL Endosado"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="recep-fecha" class="sga-form-label">Fecha y Hora de Recepción *</label>
          <InputText
            id="recep-fecha"
            v-model="form.fechaRecepcionDestino"
            type="datetime-local"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="recep-archivo" class="sga-form-label">Documento / Acta de Conformidad en Destino (opcional)</label>
          <input
            id="recep-archivo"
            type="file"
            accept=".pdf,image/*"
            class="sga-file-input"
            @change="onConformidadSelected"
          />
          <span v-if="archivoConformidad" class="sga-field-hint is-selected">
            Archivo adjunto: {{ archivoConformidad.name }}
          </span>
          <span v-else class="sga-field-hint">
            Puede adjuntar el POD firmado o acta de recepción en formato PDF o imagen.
          </span>
        </div>

        <div class="sga-form-field full-span">
          <label for="recep-obs" class="sga-form-label">Observaciones de Entrega Final</label>
          <Textarea
            id="recep-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Ej. Mercancía recepcionada intacta en puerto/almacén de destino..."
            size="small"
            fluid
          />
        </div>
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
        form="recepcion-destino-exterior-form"
        label="Confirmar Recepción en Destino"
        icon="pi pi-check"
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
import { documentoService } from '@/features/documentos/services/documentoService'
import { getNowDateTimeInput, toLocalDateTimePayload } from '@/shared/utils/dateUtils'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()

const loading = ref(false)
const errorMessage = ref('')
const archivoConformidad = ref(null)

const onConformidadSelected = (event) => {
  archivoConformidad.value = event?.target?.files?.[0] || null
}

const form = ref({
  consignatarioExterior: '',
  documentoRecepcion: '',
  fechaRecepcionDestino: '',
  observaciones: ''
})

const getNowISOString = () => {
  return getNowDateTimeInput()
}

watch(
  () => [props.visible, props.expediente],
  ([vis, exp]) => {
    if (vis && exp) {
      errorMessage.value = ''
      archivoConformidad.value = null
      form.value = {
        consignatarioExterior: exp.contactoExteriorNombre || 'Consignatario Internacional Registrado',
        documentoRecepcion: 'BL Endosado / Factura Internacional',
        fechaRecepcionDestino: getNowISOString(),
        observaciones: 'Recepción conforme en destino exterior sin observaciones.'
      }
    }
  },
  { immediate: true }
)

const handleClose = () => {
  errorMessage.value = ''
  archivoConformidad.value = null
  emit('close')
}

const handleSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  try {
    let idDocumentoConformidad = null
    if (archivoConformidad.value) {
      const uploaded = await documentoService.upload([archivoConformidad.value], {
        categoria: 'CONFORMIDAD_DESTINO',
        descripcion: `Conformidad en país de destino — ${form.value.consignatarioExterior}`,
        entidadTipo: 'EXPEDIENTE',
        idEntidad: props.expediente.idExpediente
      })
      const firstDoc = Array.isArray(uploaded) ? uploaded[0] : uploaded
      idDocumentoConformidad = firstDoc?.idDocumento || null
    }

    const payload = {
      consignatarioExterior: form.value.consignatarioExterior,
      documentoRecepcion: form.value.documentoRecepcion,
      fechaRecepcionDestino: toLocalDateTimePayload(form.value.fechaRecepcionDestino),
      idDocumentoConformidad,
      observaciones: form.value.observaciones
    }

    const res = await expedienteService.confirmarRecepcionExterior(props.expediente.idExpediente, payload)
    emit('saved', res)
    handleClose()
  } catch (err) {
    console.error('Error al confirmar recepción exterior:', err)
    errorMessage.value = err.response?.data?.message || err.message || 'Error al registrar recepción en destino exterior.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.recepcion-banner-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  line-height: 1.4;
}

.full-span {
  grid-column: 1 / -1;
}

.sga-file-input {
  width: 100%;
  padding: 0.45rem 0.65rem;
  border: 1px dashed var(--color-border-strong, var(--color-border));
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  font-size: 0.8rem;
  cursor: pointer;
}

.sga-field-hint.is-selected {
  color: var(--color-success-text);
  font-weight: 600;
}
</style>
