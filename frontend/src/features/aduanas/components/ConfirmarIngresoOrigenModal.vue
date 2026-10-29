<template>
  <BaseModal
    :visible="visible"
    title="Confirmar Ingreso a Depósito Temporal / Zona Primaria"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — DAM: ${expediente?.numeroDam || 'Por asignar'}`"
    icon="pi pi-inbox"
    max-width="600px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Info Banner Ingreso Origen -->
      <div class="ingreso-info-banner">
        <i class="pi pi-check-circle text-lg"></i>
        <div class="banner-texts">
          <strong>Validación de Ingreso a Zona Primaria y Certificación VGM</strong>
          <p>
            Al registrar el ingreso formal de la carga al depósito aduanero de origen con su ticket de balanza/VGM y contenedor precintado, el expediente pasará a <strong>EN TRÁMITE</strong> para la asignación inmediata de Canal de Control SUNAT.
          </p>
        </div>
      </div>

      <div class="ingreso-fields-grid">
        <div class="field-item full-width">
          <label for="ingreso-terminal">Depósito Temporal / Terminal de Salida *</label>
          <input
            id="ingreso-terminal"
            v-model.trim="form.terminalAlmacenOrigen"
            type="text"
            class="text-input"
            required
            placeholder="Ej. DP World Callao - Muelle Sur / APM Terminals"
          />
        </div>

        <div class="field-item">
          <label for="ingreso-ticket">N° Ticket de Balanza / Certificado VGM *</label>
          <input
            id="ingreso-ticket"
            v-model.trim="form.numeroTicketIngreso"
            type="text"
            class="text-input"
            required
            placeholder="Ej. TKT-VGM-2026-90812"
          />
        </div>

        <div class="field-item">
          <label for="ingreso-fecha">Fecha y Hora de Ingreso *</label>
          <input
            id="ingreso-fecha"
            v-model="form.fechaIngreso"
            type="datetime-local"
            class="text-input"
            required
          />
        </div>

        <div class="field-item">
          <label for="ingreso-contenedor">N° Contenedor *</label>
          <input
            id="ingreso-contenedor"
            v-model.trim="form.numeroContenedor"
            type="text"
            class="text-input font-mono"
            required
            placeholder="Ej. MSKU9876543"
          />
        </div>

        <div class="field-item">
          <label for="ingreso-precinto">N° Precinto Aduanero / Botella *</label>
          <input
            id="ingreso-precinto"
            v-model.trim="form.precintoAduanero"
            type="text"
            class="text-input font-mono"
            required
            placeholder="Ej. SUNAT-PR-098231"
          />
        </div>

        <div class="field-item full-width">
          <label for="ingreso-obs">Observaciones de Recepción / Patio</label>
          <textarea
            id="ingreso-obs"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="Contenedor sin abolladuras, precintos de seguridad intactos, peso verificado en balanza..."
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
          <span>Confirmar Ingreso a Terminal</span>
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
  terminalAlmacenOrigen: 'DP World Callao - Muelle Sur',
  numeroTicketIngreso: '',
  fechaIngreso: '',
  numeroContenedor: '',
  precintoAduanero: '',
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
      fechaIngreso: form.value.fechaIngreso ? new Date(form.value.fechaIngreso).toISOString() : null,
      numeroContenedor: form.value.numeroContenedor,
      precintoAduanero: form.value.precintoAduanero,
      observaciones: form.value.observaciones
    }

    const res = await expedienteService.confirmarIngresoOrigen(props.expediente.idExpediente, payload)
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
.form-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ingreso-info-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  border-radius: 8px;
  padding: 12px 14px;
  color: var(--color-text-main);
}

.ingreso-info-banner i {
  color: var(--color-success-text);
  margin-top: 2px;
}

.banner-texts strong {
  display: block;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-success-text);
  margin-bottom: 3px;
}

.banner-texts p {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.35;
  color: var(--color-text-muted);
}

.ingreso-fields-grid {
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
  color: var(--color-text-muted);
}

.text-input,
.textarea-input {
  width: 100%;
  padding: 8px 12px;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-main);
  font-size: 0.85rem;
  transition: border-color 0.15s ease;
}

.text-input:focus,
.textarea-input:focus {
  outline: none;
  border-color: var(--color-primary);
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
  border-top: 1px solid var(--color-border);
}

.btn-modal-cancel {
  padding: 8px 16px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-main);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-modal-cancel:hover:not(:disabled) {
  background: var(--color-bg-muted);
}

.btn-modal-save {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: var(--color-primary);
  border: none;
  border-radius: 6px;
  color: var(--color-primary-text);
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
