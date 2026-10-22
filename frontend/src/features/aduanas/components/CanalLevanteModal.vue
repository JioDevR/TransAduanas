<template>
  <BaseModal
    :visible="visible"
    :title="actionType === 'canal' ? 'Asignar Canal SUNAT y Declaración DAM' : 'Autorizar Levante Aduanero'"
    :subtitle="`Expediente Aduanero: ${expediente?.codigoExpediente || ''} — DAM: ${expediente?.numeroDam || 'Pendiente'}`"
    :icon="actionType === 'canal' ? 'pi pi-compass' : 'pi pi-check-circle'"
    max-width="580px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Modal: ASIGNAR CANAL SUNAT & DAM -->
      <div v-if="actionType === 'canal'" class="canal-form-grid">
        <div class="field-item">
          <label for="dam-num">Número de Declaración Aduanera (DAM) *</label>
          <input
            id="dam-num"
            v-model.trim="canalForm.numeroDam"
            type="text"
            placeholder="Ej. 118-2026-10-123456"
            class="text-input font-mono"
            required
            maxlength="30"
          />
          <span class="field-hint">Numeración oficial emitida por la Intendencia de Aduana</span>
        </div>

        <div class="field-item">
          <label class="field-label">Canal de Control SUNAT Asignado *</label>
          <div class="canales-options">
            <label :class="['canal-option-card verde', { selected: canalForm.canalSunat === 'VERDE' }]">
              <input type="radio" v-model="canalForm.canalSunat" value="VERDE" />
              <div class="canal-dot bg-green"></div>
              <div class="canal-texts">
                <strong>Canal VERDE</strong>
                <span>Levante automático sin revisión documental ni física</span>
              </div>
            </label>

            <label :class="['canal-option-card naranja', { selected: canalForm.canalSunat === 'NARANJA' }]">
              <input type="radio" v-model="canalForm.canalSunat" value="NARANJA" />
              <div class="canal-dot bg-orange"></div>
              <div class="canal-texts">
                <strong>Canal NARANJA</strong>
                <span>Revisión documentaria requerida por especialista SUNAT</span>
              </div>
            </label>

            <label :class="['canal-option-card rojo', { selected: canalForm.canalSunat === 'ROJO' }]">
              <input type="radio" v-model="canalForm.canalSunat" value="ROJO" />
              <div class="canal-dot bg-red"></div>
              <div class="canal-texts">
                <strong>Canal ROJO</strong>
                <span>Reconocimiento físico obligatorio en depósito temporal</span>
              </div>
            </label>
          </div>
        </div>
      </div>

      <!-- Modal: AUTORIZAR LEVANTE -->
      <div v-else class="levante-form-grid">
        <div class="levante-info-banner">
          <i class="pi pi-shield"></i>
          <div>
            <strong>Conformidad Aduanera y Legal</strong>
            <p>Al sellar el levante, se autoriza el retiro de la mercancía del depósito temporal una vez verificado el pago de tributos aduaneros.</p>
          </div>
        </div>

        <div class="field-item full-width">
          <label for="levante-detalle">Detalle de Inspección / Diligencia Aduanera</label>
          <textarea
            id="levante-detalle"
            v-model.trim="levanteForm.detalleInspeccion"
            rows="3"
            placeholder="Diligencia conforme por especialista SUNAT. Precintos intactos y bultos verificados según manifiesto..."
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
          <span>{{ actionType === 'canal' ? 'Registrar Canal SUNAT' : 'Autorizar Levante' }}</span>
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
  visible: {
    type: Boolean,
    default: false
  },
  expediente: {
    type: Object,
    default: null
  },
  actionType: {
    type: String, // 'canal' | 'levante'
    default: 'canal'
  }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')

const canalForm = ref({
  numeroDam: '',
  canalSunat: 'VERDE'
})

const levanteForm = ref({
  detalleInspeccion: 'Inspección conforme. Sin incidencias físicas ni documentarias.'
})

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      errorMessage.value = ''
      if (props.expediente) {
        canalForm.value = {
          numeroDam: props.expediente.numeroDam || '',
          canalSunat: props.expediente.canalSunat === 'PENDIENTE' ? 'VERDE' : (props.expediente.canalSunat || 'VERDE')
        }
        levanteForm.value = {
          detalleInspeccion: props.expediente.detalleInspeccion || 'Inspección conforme. Sin incidencias físicas ni documentarias.'
        }
      }
    }
  }
)

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  if (!props.expediente) return
  errorMessage.value = ''

  loading.value = true
  try {
    let result
    if (props.actionType === 'canal') {
      if (!canalForm.value.numeroDam || !canalForm.value.canalSunat) {
        errorMessage.value = 'Debe ingresar el número de DAM y seleccionar el canal SUNAT.'
        loading.value = false
        return
      }
      result = await expedienteService.asignarCanal(props.expediente.idExpediente, canalForm.value)
    } else {
      result = await expedienteService.autorizarLevante(props.expediente.idExpediente, levanteForm.value)
    }

    emit('saved', result)
    handleClose()
  } catch (err) {
    console.error('Error al actualizar trámite aduanero:', err)
    errorMessage.value =
      err.response?.data?.message || 'Error al procesar la actualización del expediente.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.canales-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.canal-option-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.875rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--color-bg-surface);
}

.canal-option-card:hover {
  border-color: var(--color-accent-kicker);
}

.canal-option-card.selected {
  border-color: var(--color-accent-kicker);
  background: var(--color-bg-subtle, rgba(194, 65, 12, 0.05));
}

.canal-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
}

.bg-green { background: #22c55e; box-shadow: 0 0 8px rgba(34, 197, 94, 0.4); }
.bg-orange { background: #f97316; box-shadow: 0 0 8px rgba(249, 115, 22, 0.4); }
.bg-red { background: #ef4444; box-shadow: 0 0 8px rgba(239, 68, 68, 0.4); }

.canal-texts {
  display: flex;
  flex-direction: column;
}

.canal-texts strong {
  font-size: 0.875rem;
  color: var(--color-text-main);
}

.canal-texts span {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.levante-info-banner {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.25);
  border-radius: var(--radius-md, 8px);
  padding: 1rem;
  margin-bottom: 1.25rem;
  color: var(--color-text-main);
}

.levante-info-banner i {
  color: #22c55e;
  font-size: 1.25rem;
  margin-top: 0.2rem;
}

.levante-info-banner strong {
  display: block;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.levante-info-banner p {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
  line-height: 1.4;
}

.textarea-input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  font-family: inherit;
  font-size: 0.875rem;
  resize: vertical;
}
</style>
