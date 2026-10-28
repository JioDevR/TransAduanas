<template>
  <BaseModal
    :visible="visible"
    title="Confirmar Embarque Internacional (Exportación)"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — DAM: ${expediente?.numeroDam || 'S/N'}`"
    icon="pi pi-send"
    max-width="600px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Info Banner Embarque -->
      <div class="embarque-info-banner">
        <i class="pi pi-compass text-lg"></i>
        <div class="banner-texts">
          <strong>Confirmación de Carga Estibada y Zarpe de Nave</strong>
          <p>
            Habiéndose otorgado el <strong>LEVANTE AUTORIZADO</strong>, se certifica el embarque físico del contenedor o bultos a bordo del buque/aeronave de salida para el inicio del tránsito internacional hacia el país de destino.
          </p>
        </div>
      </div>

      <div class="embarque-fields-grid">
        <div class="field-item full-width">
          <label for="embarque-nave">Nave y Número de Viaje *</label>
          <input
            id="embarque-nave"
            v-model.trim="form.naveViaje"
            type="text"
            class="text-input"
            required
            placeholder="Ej. MSC CAPELLA / V. 2402W"
          />
        </div>

        <div class="field-item">
          <label for="embarque-zarpe">Fecha y Hora de Zarpe Real (ETD) *</label>
          <input
            id="embarque-zarpe"
            v-model="form.fechaZarpeEtd"
            type="date"
            class="text-input"
            required
          />
        </div>

        <div class="field-item">
          <label for="embarque-eta">ETA Estimado en Puerto Destino *</label>
          <input
            id="embarque-eta"
            v-model="form.fechaEstimadaLlegadaExterior"
            type="date"
            class="text-input"
            required
          />
        </div>

        <div class="field-item full-width">
          <label for="embarque-puerto">Puerto / Aeropuerto de Destino Exterior *</label>
          <input
            id="embarque-puerto"
            v-model.trim="form.puertoDestinoExterior"
            type="text"
            class="text-input"
            required
            placeholder="Ej. Port of Long Beach, USA / Rotterdam, Netherlands"
          />
        </div>

        <div class="field-item full-width">
          <label for="embarque-obs">Detalles de Manifiesto / Observaciones</label>
          <textarea
            id="embarque-obs"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="BL marítimo matriz emitido a bordo, estiba en cubierta/bodega conforme..."
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
          <i v-else class="pi pi-check-circle"></i>
          <span>Registrar Embarque Internacional</span>
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
  naveViaje: '',
  fechaZarpeEtd: '',
  fechaEstimadaLlegadaExterior: '',
  puertoDestinoExterior: '',
  observaciones: ''
})

const getTodayString = () => {
  return new Date().toISOString().slice(0, 10)
}

const getFutureDateString = (days) => {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      errorMessage.value = ''
      form.value = {
        naveViaje: exp.naveViaje || 'MSC CAPELLA / V. 2402W',
        fechaZarpeEtd: exp.fechaZarpeEtd || getTodayString(),
        fechaEstimadaLlegadaExterior: exp.fechaLlegadaEta || getFutureDateString(14),
        puertoDestinoExterior: 'Puerto de Destino Internacional',
        observaciones: 'Carga estibada y confirmada en sistema de línea naviera.'
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
      naveViaje: form.value.naveViaje,
      fechaZarpeEtd: form.value.fechaZarpeEtd || null,
      fechaEstimadaLlegadaExterior: form.value.fechaEstimadaLlegadaExterior || null,
      puertoDestinoExterior: form.value.puertoDestinoExterior,
      observaciones: form.value.observaciones
    }

    const res = await expedienteService.confirmarEmbarqueOrigen(props.expediente.idExpediente, payload)
    emit('saved', res)
    handleClose()
  } catch (err) {
    console.error('Error al confirmar embarque:', err)
    errorMessage.value = err.response?.data?.message || err.message || 'Error al registrar embarque de exportación.'
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

.embarque-info-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.25);
  border-radius: 8px;
  padding: 12px 14px;
  color: var(--text-color, #e2e8f0);
}

.embarque-info-banner i {
  color: #818cf8;
  margin-top: 2px;
}

.banner-texts strong {
  display: block;
  font-size: 0.85rem;
  font-weight: 700;
  color: #a5b4fc;
  margin-bottom: 3px;
}

.banner-texts p {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.35;
  color: var(--text-muted, #94a3b8);
}

.embarque-fields-grid {
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
  border-color: #6366f1;
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
  background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
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
