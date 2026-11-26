<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('660px'), maxWidth: '95vw' }"
    header="Confirmar Ingreso a Terminal Portuario / Aéreo de Origen"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="confirmar-ingreso-origen-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial || 'Exportador asignado' }}
          </span>
        </div>
        <Tag
          v-if="expediente.numeroContenedor"
          :value="expediente.numeroContenedor"
          severity="secondary"
          class="sga-mini-tag font-mono"
          rounded
        />
      </div>

      <div class="sga-banner is-success">
        <i class="pi pi-warehouse"></i>
        <div>
          <strong>Recepción de Carga en Zona Primaria</strong>
          <p class="ingreso-banner-desc">
            Confirme que la carga arribó e ingresó físicamente al depósito temporal o terminal de embarque en origen.
          </p>
        </div>
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field full-span">
          <label for="ingreso-terminal" class="sga-form-label">Terminal / Almacén de Recepción en Origen *</label>
          <InputText
            id="ingreso-terminal"
            v-model.trim="form.terminalAlmacenOrigen"
            placeholder="Ej. DP World Callao / Talma Servicios Aeroportuarios"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="ingreso-ticket" class="sga-form-label">N° Ticket de Ingreso / Gate-In *</label>
          <InputText
            id="ingreso-ticket"
            v-model.trim="form.numeroTicketIngreso"
            placeholder="Ej. GATE-2026-88120"
            class="font-mono"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="ingreso-fecha" class="sga-form-label">Fecha y Hora de Ingreso *</label>
          <InputText
            id="ingreso-fecha"
            v-model="form.fechaIngreso"
            type="datetime-local"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="ingreso-contenedor" class="sga-form-label">N° Contenedor / Unidad</label>
          <InputText
            id="ingreso-contenedor"
            v-model.trim="form.numeroContenedor"
            placeholder="Ej. MSCU9128412"
            class="font-mono"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="ingreso-precinto" class="sga-form-label">N° Precinto Aduanero / Seguridad</label>
          <InputText
            id="ingreso-precinto"
            v-model.trim="form.precintoAduanero"
            placeholder="Ej. SEAL-90218"
            class="font-mono"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="ingreso-obs" class="sga-form-label">Observaciones de Recepción / Pesaje</label>
          <Textarea
            id="ingreso-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Ej. Contenedor recepcionado sin daños físicos, balanza VGM conforme..."
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="ingreso-fotos" class="sga-form-label">Evidencia Fotográfica de Almacén (opcional)</label>
          <input
            id="ingreso-fotos"
            type="file"
            multiple
            accept="image/*,.pdf"
            class="sga-file-input"
            @change="onFotosSelected"
          />
          <span v-if="fotosSeleccionadas.length > 0" class="sga-field-hint is-selected">
            {{ fotosSeleccionadas.length }} archivo(s) seleccionado(s) para adjuntar al almacén.
          </span>
          <span v-else class="sga-field-hint">
            Puede adjuntar fotografías del precinto, ticket de balanza o recepción en formato imagen o PDF.
          </span>
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
        form="confirmar-ingreso-origen-form"
        label="Confirmar Ingreso a Terminal"
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
const fotosSeleccionadas = ref([])

const onFotosSelected = (event) => {
  fotosSeleccionadas.value = Array.from(event?.target?.files || [])
}

const form = ref({
  terminalAlmacenOrigen: 'DP World Callao - Muelle Sur',
  numeroTicketIngreso: '',
  fechaIngreso: '',
  numeroContenedor: '',
  precintoAduanero: '',
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
      fotosSeleccionadas.value = []
      form.value = {
        terminalAlmacenOrigen: 'DP World Callao - Muelle Sur',
        numeroTicketIngreso: '',
        fechaIngreso: getNowISOString(),
        numeroContenedor: exp.numeroContenedor || '',
        precintoAduanero: exp.precintoAduanero || '',
        observaciones: 'Ingreso verificado en garita de control y pesaje registrado conforme.'
      }
    }
  },
  { immediate: true }
)

const handleClose = () => {
  errorMessage.value = ''
  fotosSeleccionadas.value = []
  emit('close')
}

const handleSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  try {
    const payload = {
      terminalAlmacenOrigen: form.value.terminalAlmacenOrigen,
      numeroTicketIngreso: form.value.numeroTicketIngreso,
      fechaIngreso: toLocalDateTimePayload(form.value.fechaIngreso),
      numeroContenedor: form.value.numeroContenedor,
      precintoAduanero: form.value.precintoAduanero,
      observaciones: form.value.observaciones
    }

    const res = await expedienteService.confirmarIngresoOrigen(props.expediente.idExpediente, payload)
    if (fotosSeleccionadas.value.length > 0) {
      await documentoService.upload(fotosSeleccionadas.value, {
        categoria: 'FOTO_ALMACEN',
        descripcion: `Evidencia fotográfica recepción almacén (${form.value.terminalAlmacenOrigen})`,
        entidadTipo: 'EXPEDIENTE',
        idEntidad: props.expediente.idExpediente
      })
    }
    emit('saved', res)
    handleClose()
  } catch (err) {
    console.error('Error al confirmar ingreso a terminal:', err)
    errorMessage.value = err.response?.data?.message || err.message || 'Error al confirmar ingreso a zona primaria.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ingreso-banner-desc {
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
