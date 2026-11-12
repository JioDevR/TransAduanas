<template>
  <BaseModal
    :visible="visible"
    title="Registrar Intervención / Diligencia de Campo (SUNAT)"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — DAM: ${expediente?.numeroDam || 'Pendiente'} — Canal: ${expediente?.canalSunat || ''}`"
    icon="pi pi-search-plus"
    max-width="600px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Banner Informativo según Canal SUNAT -->
      <div :class="['canal-badge-banner', (expediente?.canalSunat || 'ROJO').toLowerCase()]">
        <i class="pi pi-exclamation-circle text-lg"></i>
        <div class="banner-texts">
          <strong>{{ expediente?.canalSunat === 'ROJO' ? 'Canal ROJO: Reconocimiento Físico Obligatorio' : 'Canal NARANJA: Revisión Documentaria' }}</strong>
          <p v-if="expediente?.canalSunat === 'ROJO'">
            Diligencia presencial en depósito temporal con el Especialista SUNAT. Verificación de bultos, marcas, precintos y aforo físico de mercancías.
          </p>
          <p v-else>
            Cotejo documentario ante SUNAT de factura comercial, BL/guía aérea, packing list y autorizaciones sectoriales.
          </p>
        </div>
      </div>

      <div class="field-item">
        <label for="diligencia-detalle">Acta y Detalle de la Diligencia Aduanera *</label>
        <textarea
          id="diligencia-detalle"
          v-model.trim="form.detalleInspeccion"
          rows="4"
          placeholder="Ej. Reconocimiento físico concluido a satisfacción con Especialista SUNAT. Precinto PE-SUNAT intacto, bultos cotejados sin discrepancias..."
          class="textarea-input"
          required
        ></textarea>
        <span class="field-hint">Ingrese los hallazgos técnicos, número de acta de aforo o dictamen del especialista SUNAT</span>
      </div>

      <div class="field-item">
        <label for="diligencia-obs">Observaciones Adicionales / Incidencias</label>
        <textarea
          id="diligencia-obs"
          v-model.trim="form.observaciones"
          rows="2"
          placeholder="Sin incidencias aduaneras / Cumple con regulaciones vigentes..."
          class="textarea-input"
        ></textarea>
      </div>

      <div class="modal-actions">
        <button type="button" class="btn-modal-cancel" @click="handleClose" :disabled="loading">
          Cancelar
        </button>
        <button type="submit" class="btn-modal-save" :disabled="loading">
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-check"></i>
          <span>Registrar Diligencia Superada</span>
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
  detalleInspeccion: '',
  observaciones: ''
})

watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      form.value = {
        detalleInspeccion: exp.detalleInspeccion || '',
        observaciones: ''
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
      detalleInspeccion: form.value.detalleInspeccion,
      observaciones: form.value.observaciones || null
    }

    const updated = await expedienteService.registrarDiligencia(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar la diligencia de campo aduanera.'
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

.canal-badge-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  padding: 0.85rem 1rem;
  border-radius: 6px;
}

.canal-badge-banner.rojo {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger-text);
}

.canal-badge-banner.naranja {
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  color: var(--color-warning-text);
}

.canal-badge-banner.verde {
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  color: var(--color-success-text);
}

.banner-texts strong {
  display: block;
  font-size: 0.85rem;
  margin-bottom: 0.2rem;
}

.banner-texts p {
  margin: 0;
  font-size: 0.775rem;
  opacity: 0.9;
  line-height: 1.35;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.field-item label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.field-hint {
  font-size: 0.725rem;
  color: var(--color-text-muted);
}

.textarea-input {
  width: 100%;
  padding: 0.65rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  font-size: 0.875rem;
  resize: vertical;
  transition: all 0.2s ease;
}

.textarea-input:focus {
  outline: none;
  border-color: var(--color-primary);
  background: var(--color-bg-surface);
  box-shadow: 0 0 0 2px rgba(53, 47, 42, 0.15);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border-subtle);
}

.btn-modal-cancel {
  padding: 0.6rem 1.2rem;
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-modal-cancel:hover:not(:disabled) {
  background: var(--color-bg-subtle);
}

.btn-modal-save {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.25rem;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  color: var(--color-primary-text);
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-save:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-modal-save:disabled,
.btn-modal-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
