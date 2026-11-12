<template>
  <BaseModal
    :visible="visible"
    title="Revisión de Conformidad y Cierre Final de Expediente"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — DAM: ${expediente?.numeroDam || 'S/N'}`"
    icon="pi pi-check-circle"
    max-width="600px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Checklist de Verificación de Cierre -->
      <div class="cierre-checklist-card">
        <h4 class="checklist-title">
          {{ isExportacion ? 'Checklist de Cierre de Despacho de Exportación' : 'Checklist de Cierre de Despacho de Importación' }}
        </h4>
        
        <!-- Checklist 1: Levante Aduanero -->
        <div class="check-row">
          <i :class="['pi text-sm', expediente?.fechaLevanteAutorizacion ? 'pi-check-circle text-success' : 'pi-times-circle text-danger']"></i>
          <div class="check-info">
            <span class="check-label">{{ isExportacion ? 'Levante Aduanero de Exportación Autorizado' : 'Levante Aduanero Autorizado' }}</span>
            <span class="check-sub font-mono">{{ expediente?.fechaLevanteAutorizacion ? `Concedido el ${formatDate(expediente.fechaLevanteAutorizacion)}` : 'Pendiente de autorización' }}</span>
          </div>
        </div>

        <!-- Checklist 2: Embarque o Entrega Terrestre -->
        <div v-if="isExportacion" class="check-row">
          <i :class="['pi text-sm', embarqueExportacionVerificado ? 'pi-check-circle text-success' : 'pi-exclamation-triangle text-warning']"></i>
          <div class="check-info">
            <span class="check-label">Embarque Internacional a Bordo de Nave</span>
            <span class="check-sub">
              {{ embarqueExportacionVerificado ? `Nave: ${expediente?.naveViaje || 'Confirmada'} — Carga estibada y zarpada a destino` : 'Pendiente de confirmación de embarque' }}
            </span>
          </div>
        </div>
        <div v-else class="check-row">
          <i :class="['pi text-sm', entregaVerificada ? 'pi-check-circle text-success' : 'pi-exclamation-triangle text-warning']"></i>
          <div class="check-info">
            <span class="check-label">Entrega de Carga Verificada</span>
            <span v-if="expediente?.servicioTransporte" class="check-sub">
              Transporte SGA: Tracking <span class="font-mono">{{ expediente?.codigoTrackingTransporte || '—' }}</span> ({{ expediente?.estadoTransporte || 'PENDIENTE' }})
            </span>
            <span v-else class="check-sub">
              Retiro por el Cliente en Terminal Extraportuario
            </span>
          </div>
        </div>

        <!-- Checklist 3: Legajo y Liquidación -->
        <div class="check-row">
          <i class="pi pi-check-circle text-success text-sm"></i>
          <div class="check-info">
            <span class="check-label">{{ isExportacion ? 'Legajo Digital de Exportación & Regularización' : 'Tributos y Liquidación Aduanera' }}</span>
            <span class="check-sub">{{ isExportacion ? 'DAM de exportación regularizada y legajo archivado conforme' : 'Tributos cancelados ante SUNAT conforme' }}</span>
          </div>
        </div>
      </div>

      <div class="field-item">
        <label for="cierre-obs">Acta de Conformidad / Observaciones de Cierre Definitivo *</label>
        <textarea
          id="cierre-obs"
          v-model.trim="form.observacionesCierre"
          rows="3"
          placeholder="Ej. Se revisaron conformidades de levante aduanero, entrega a satisfacción y archivo de legajo digital del despacho. Expediente cerrado y archivado conforme."
          class="textarea-input"
          required
        ></textarea>
        <span class="field-hint">Esta acción cambiará el estado del expediente a CERRADO y notificará formalmente al cliente.</span>
      </div>

      <div class="modal-actions">
        <button type="button" class="btn-modal-cancel" @click="handleClose" :disabled="loading">
          Cancelar
        </button>
        <button type="submit" class="btn-modal-save" :disabled="loading">
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-lock"></i>
          <span>Confirmar Cierre de Expediente</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
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
  observacionesCierre: 'Conformidad técnica y liquidación aduanera verificada. Despacho concluido y archivado formalmente.'
})

const isExportacion = computed(() => {
  const reg = props.expediente?.regimenAduanero || ''
  return reg.toUpperCase().includes('EXPORT')
})

const embarqueExportacionVerificado = computed(() => {
  if (!props.expediente) return false
  const obs = props.expediente.observaciones || ''
  return Boolean(
    props.expediente.naveViaje ||
    props.expediente.fechaZarpeEtd ||
    obs.includes('EMBARQUE') ||
    obs.includes('RECEPCIÓN DESTINO')
  )
})

const entregaVerificada = computed(() => {
  if (!props.expediente) return false
  if (props.expediente.servicioTransporte) {
    return props.expediente.estadoTransporte === 'ENTREGADO'
  }
  // Si no requirió transporte interno, se asume retiro en terminal
  return true
})

watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      form.value.observacionesCierre = 'Conformidad técnica y liquidación aduanera verificada. Despacho concluido y archivado formalmente.'
    }
  },
  { immediate: true }
)

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

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
      observacionesCierre: form.value.observacionesCierre
    }

    const updated = await expedienteService.cerrarExpediente(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al cerrar el expediente aduanero.'
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

.cierre-checklist-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 6px;
}

.checklist-title {
  margin: 0 0 0.25rem 0;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.check-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.35rem 0;
  border-bottom: 1px dashed var(--color-border-subtle);
}

.check-row:last-child {
  border-bottom: none;
}

.check-info {
  display: flex;
  flex-direction: column;
}

.check-label {
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.check-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.text-success {
  color: var(--color-success-text);
}

.text-danger {
  color: var(--color-danger-text);
}

.text-warning {
  color: var(--color-warning-text);
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
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  color: var(--color-success-text);
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-save:hover:not(:disabled) {
  filter: brightness(0.95);
}

.btn-modal-save:disabled,
.btn-modal-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
