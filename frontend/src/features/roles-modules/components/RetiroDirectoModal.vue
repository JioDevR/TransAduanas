<template>
  <BaseModal
    :visible="visible"
    title="Registrar Acta de Retiro Directo en Terminal"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — Cliente: ${expediente?.clienteRazonSocial || ''}`"
    icon="pi pi-building"
    max-width="580px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Info Banner -->
      <div class="retiro-info-banner">
        <i class="pi pi-info-circle text-lg"></i>
        <div class="banner-texts">
          <strong>Retiro Directo por el Cliente (Sin Transporte SGA)</strong>
          <p>
            El cliente retira la mercancía liberada directamente del depósito temporal / terminal portuario con su propia unidad de transporte.
          </p>
        </div>
      </div>

      <div class="retiro-fields-grid">
        <div class="field-item">
          <label for="terminal-dep">Terminal Extraportuario / Almacén *</label>
          <input
            id="terminal-dep"
            v-model.trim="form.terminalAlmacen"
            type="text"
            class="text-input"
            required
            placeholder="Ej. DPW Callao / Almacén Temporal Ransa"
          />
        </div>

        <div class="field-item">
          <label for="acta-num">Número de Ticket / Acta de Salida</label>
          <input
            id="acta-num"
            v-model.trim="form.numeroActaRetiro"
            type="text"
            class="text-input font-mono"
            placeholder="Ej. ACT-2026-9812"
          />
        </div>

        <div class="field-item">
          <label for="receptor-nombre">Nombre de Quien Recibe / Conductor *</label>
          <input
            id="receptor-nombre"
            v-model.trim="form.receptorNombre"
            type="text"
            class="text-input"
            required
            placeholder="Ej. Carlos Mendoza (Transportes Externos)"
          />
        </div>

        <div class="field-item">
          <label for="receptor-dni">DNI / Documento del Receptor *</label>
          <input
            id="receptor-dni"
            v-model.trim="form.receptorDni"
            type="text"
            class="text-input font-mono"
            required
            placeholder="Ej. 45678901"
          />
        </div>

        <div class="field-item">
          <label for="fecha-retiro">Fecha y Hora de Retiro Conforme</label>
          <input
            id="fecha-retiro"
            v-model="form.fechaRetiro"
            type="datetime-local"
            class="text-input"
          />
        </div>

        <div class="field-item full-width">
          <label for="obs-retiro">Observaciones del Acta de Recepción Externa</label>
          <textarea
            id="obs-retiro"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="Placa del vehículo externo, precintos cortados conforme, condición de embalaje..."
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
          <span>Registrar Acta de Retiro</span>
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
  terminalAlmacen: 'Terminal Portuario Callao',
  numeroActaRetiro: '',
  receptorNombre: '',
  receptorDni: '',
  fechaRetiro: '',
  observaciones: ''
})

watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      const now = new Date()
      const tzOffset = now.getTimezoneOffset() * 60000
      const localISOTime = new Date(now.getTime() - tzOffset).toISOString().slice(0, 16)

      form.value = {
        terminalAlmacen: 'Terminal Portuario Callao',
        numeroActaRetiro: '',
        receptorNombre: exp.contactoExteriorNombre || '',
        receptorDni: '',
        fechaRetiro: localISOTime,
        observaciones: 'Retiro conforme de carga por el cliente en terminal extraportuario.'
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
      terminalAlmacen: form.value.terminalAlmacen,
      numeroActaRetiro: form.value.numeroActaRetiro || null,
      receptorNombre: form.value.receptorNombre,
      receptorDni: form.value.receptorDni,
      fechaRetiro: form.value.fechaRetiro ? `${form.value.fechaRetiro}:00` : null,
      observaciones: form.value.observaciones || null
    }

    const updated = await expedienteService.registrarRetiroDirecto(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar el acta de retiro en terminal.'
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

.retiro-info-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  padding: 0.85rem 1rem;
  background: #fdf8f6;
  border: 1px solid #fed7aa;
  color: #9a3412;
  border-radius: 6px;
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

.retiro-fields-grid {
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
