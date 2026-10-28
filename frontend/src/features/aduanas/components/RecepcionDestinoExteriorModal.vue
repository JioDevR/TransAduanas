<template>
  <BaseModal
    :visible="visible"
    title="Confirmar Recepción Conforme en Destino Exterior"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — DAM: ${expediente?.numeroDam || 'S/N'}`"
    icon="pi pi-check-circle"
    max-width="600px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Info Banner Recepción Exterior -->
      <div class="recepcion-info-banner">
        <i class="pi pi-verified text-lg"></i>
        <div class="banner-texts">
          <strong>Certificación de Entrega a Consignatario Internacional</strong>
          <p>
            Al registrar la recepción conforme por parte del comprador/importador en el país de destino (con documento de entrega o BL endosado), el ciclo operativo queda cumplido y listo para el cierre formal del Supervisor Aduanero.
          </p>
        </div>
      </div>

      <div class="recepcion-fields-grid">
        <div class="field-item full-width">
          <label for="recep-consignatario">Nombre de Consignatario o Agente Receptor en Destino *</label>
          <input
            id="recep-consignatario"
            v-model.trim="form.consignatarioExterior"
            type="text"
            class="text-input"
            required
            placeholder="Ej. Global Logistics Corp. / Hans Müller GmbH"
          />
        </div>

        <div class="field-item">
          <label for="recep-doc">Documento de Entrega / BL Retirado *</label>
          <input
            id="recep-doc"
            v-model.trim="form.documentoRecepcion"
            type="text"
            class="text-input"
            required
            placeholder="Ej. BL-END-99824 / POD-RECEIPT"
          />
        </div>

        <div class="field-item">
          <label for="recep-fecha">Fecha y Hora de Recepción *</label>
          <input
            id="recep-fecha"
            v-model="form.fechaRecepcionDestino"
            type="datetime-local"
            class="text-input"
            required
          />
        </div>

        <div class="field-item full-width">
          <label for="recep-obs">Observaciones de Conformidad del Destinatario</label>
          <textarea
            id="recep-obs"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="Mercadería recibida en bodega de destino sin novedades ni faltantes, precinto cortado en presencia del importador..."
            class="textarea-input"
          ></textarea>
        </div>
      </div>

      <div class="modal-actions">
        <button type="button" class="btn-modal-cancel" @click="handleClose" :disabled="loading">
          Cancelar
        </button>
        <button type="submit" class="btn-modal-save" :disabled="loading">
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-check"></i>
          <span>Registrar Conformidad de Recepción</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { expedienteService } from '../services/expedienteService'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')

const form = ref({
  consignatarioExterior: '',
  documentoRecepcion: '',
  fechaRecepcionDestino: '',
  observaciones: ''
})

const getNowISOString = () => {
  return new Date().toISOString().slice(0, 16)
}

watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      errorMessage.value = ''
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
  emit('close')
}

const handleSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  try {
    const payload = {
      consignatarioExterior: form.value.consignatarioExterior,
      documentoRecepcion: form.value.documentoRecepcion,
      fechaRecepcionDestino: form.value.fechaRecepcionDestino ? new Date(form.value.fechaRecepcionDestino).toISOString() : null,
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
.form-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recepcion-info-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 8px;
  padding: 12px 14px;
  color: var(--text-color, #e2e8f0);
}

.recepcion-info-banner i {
  color: #10b981;
  margin-top: 2px;
}

.banner-texts strong {
  display: block;
  font-size: 0.85rem;
  font-weight: 700;
  color: #34d399;
  margin-bottom: 3px;
}

.banner-texts p {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.35;
  color: var(--text-muted, #94a3b8);
}

.recepcion-fields-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-item.full-width {
  grid-column: 1 / -1;
}

.field-item label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted, #94a3b8);
}

.text-input,
.textarea-input {
  width: 100%;
  padding: 8px 12px;
  background: var(--bg-surface-elevated, #1e293b);
  border: 1px solid var(--border-color, #334155);
  border-radius: 6px;
  color: var(--text-color, #f8fafc);
  font-size: 0.85rem;
  transition: border-color 0.15s ease;
}

.text-input:focus,
.textarea-input:focus {
  outline: none;
  border-color: #10b981;
}

.textarea-input {
  resize: vertical;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--border-color, #334155);
}

.btn-modal-cancel {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--border-color, #334155);
  border-radius: 6px;
  color: var(--text-color, #e2e8f0);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-modal-cancel:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.05);
}

.btn-modal-save {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  border: none;
  border-radius: 6px;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.btn-modal-save:hover:not(:disabled) {
  opacity: 0.92;
}

.btn-modal-save:disabled,
.btn-modal-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
