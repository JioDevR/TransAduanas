<template>
  <BaseModal
    :visible="visible"
    title="Actualizar Embarque y Tránsito Internacional"
    :subtitle="`Expediente Aduanero: ${expediente?.codigoExpediente || ''} — Cliente: ${expediente?.clienteRazonSocial || ''}`"
    icon="pi pi-globe"
    max-width="580px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <div class="transito-grid">
        <div class="field-item">
          <label for="nave-viaje">Nave / Buque y Número de Viaje *</label>
          <input
            id="nave-viaje"
            v-model.trim="form.naveViaje"
            type="text"
            placeholder="Ej. MSC INES - V. 402W"
            class="text-input"
            required
            maxlength="100"
          />
        </div>

        <div class="field-item">
          <label for="num-contenedor">Número de Contenedor *</label>
          <input
            id="num-contenedor"
            v-model.trim="form.numeroContenedor"
            type="text"
            placeholder="Ej. MEDU1234567"
            class="text-input font-mono"
            required
            maxlength="30"
          />
        </div>

        <div class="field-item">
          <label for="precinto-aduana">Precinto Aduanero de Origen *</label>
          <input
            id="precinto-aduana"
            v-model.trim="form.precintoAduanero"
            type="text"
            placeholder="Ej. PE-SUNAT-998877"
            class="text-input font-mono"
            required
            maxlength="50"
          />
        </div>

        <div class="field-item">
          <label for="fecha-zarpe">Fecha de Zarpe en Origen (ETD)</label>
          <input
            id="fecha-zarpe"
            v-model="form.fechaZarpeEtd"
            type="date"
            class="text-input"
          />
        </div>

        <div class="field-item">
          <label for="fecha-eta">Fecha Estimada de Llegada (ETA)</label>
          <input
            id="fecha-eta"
            v-model="form.fechaLlegadaEta"
            type="date"
            class="text-input"
          />
        </div>

        <div class="field-item">
          <label for="fecha-arribo">Fecha Real de Arribo a Aduana</label>
          <input
            id="fecha-arribo"
            v-model="form.fechaArriboAduana"
            type="datetime-local"
            class="text-input"
          />
        </div>

        <div class="field-item full-width">
          <label for="obs-transito">Observaciones de Travesía / Puerto</label>
          <textarea
            id="obs-transito"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="Bitácora de navegación, escala en trasbordo o notas de inspección previa..."
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
          <i v-else class="pi pi-save"></i>
          <span>Guardar Datos de Embarque</span>
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
  numeroContenedor: '',
  precintoAduanero: '',
  fechaZarpeEtd: '',
  fechaLlegadaEta: '',
  fechaArriboAduana: '',
  observaciones: ''
})

watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      form.value = {
        naveViaje: exp.naveViaje || '',
        numeroContenedor: exp.numeroContenedor || '',
        precintoAduanero: exp.precintoAduanero || '',
        fechaZarpeEtd: exp.fechaZarpeEtd || '',
        fechaLlegadaEta: exp.fechaLlegadaEta || '',
        fechaArriboAduana: exp.fechaArriboAduana ? exp.fechaArriboAduana.substring(0, 16) : '',
        observaciones: exp.observaciones || ''
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
      numeroContenedor: form.value.numeroContenedor,
      precintoAduanero: form.value.precintoAduanero,
      fechaZarpeEtd: form.value.fechaZarpeEtd || null,
      fechaLlegadaEta: form.value.fechaLlegadaEta || null,
      fechaArriboAduana: form.value.fechaArriboAduana ? `${form.value.fechaArriboAduana}:00` : null,
      observaciones: form.value.observaciones || null
    }

    const updated = await expedienteService.updateExpediente(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al actualizar los datos de tránsito internacional.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.form-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 0.5rem 0;
}

.transito-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.field-item.full-width {
  grid-column: 1 / -1;
}

.field-item label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #3b2f2f;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.text-input,
.textarea-input {
  width: 100%;
  padding: 0.6rem 0.85rem;
  border: 1px solid #d4c5b9;
  border-radius: 4px;
  background: #fdfbf7;
  color: #1a1515;
  font-size: 0.875rem;
  transition: all 0.2s ease;
}

.text-input:focus,
.textarea-input:focus {
  outline: none;
  border-color: #78350f;
  background: #ffffff;
  box-shadow: 0 0 0 2px rgba(120, 53, 15, 0.15);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid #ebdcd0;
}

.btn-modal-cancel {
  padding: 0.6rem 1.2rem;
  background: transparent;
  border: 1px solid #c7b299;
  color: #5c4033;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-modal-cancel:hover:not(:disabled) {
  background: #f5ede6;
}

.btn-modal-save {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.25rem;
  background: #2b1d16;
  border: 1px solid #1a100a;
  color: #ffffff;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-save:hover:not(:disabled) {
  background: #422d22;
}

.btn-modal-save:disabled,
.btn-modal-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
