<template>
  <BaseModal
    :visible="visible"
    title="Confirmar Entrega en Destino"
    subtitle="Registrar recepción conforme y firma de guía de remisión"
    icon="pi pi-check-circle"
    max-width="540px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <div v-if="despacho" class="despacho-summary mb-4">
        <div class="summary-line">
          <span class="text-xs text-muted">Tracking:</span>
          <span class="font-mono font-semibold">{{ despacho.numeroTracking }}</span>
        </div>
        <div class="summary-line">
          <span class="text-xs text-muted">Destino:</span>
          <span>{{ despacho.direccionLlegada }} ({{ despacho.distritoLlegada }})</span>
        </div>
      </div>

      <div class="form-grid">
        <div class="field-item full-width">
          <label for="ent-fecha">Fecha y Hora de Recepción *</label>
          <input
            id="ent-fecha"
            v-model="form.fechaEntregaConforme"
            type="datetime-local"
            class="text-input"
            required
          />
        </div>

        <div class="field-item">
          <label for="ent-receptor-nombre">Nombre Completo del Receptor *</label>
          <input
            id="ent-receptor-nombre"
            v-model.trim="form.receptorNombre"
            type="text"
            placeholder="Ej. Ing. Carlos Mendoza"
            class="text-input"
            required
            maxlength="150"
          />
        </div>

        <div class="field-item">
          <label for="ent-receptor-dni">DNI / Documento del Receptor *</label>
          <input
            id="ent-receptor-dni"
            v-model.trim="form.receptorDni"
            type="text"
            placeholder="8 dígitos"
            class="text-input font-mono font-bold"
            required
            maxlength="20"
          />
        </div>

        <div class="field-item full-width">
          <label for="ent-contacto">Puesto / Área o Contacto de Recepción</label>
          <input
            id="ent-contacto"
            v-model.trim="form.contactoRecepcion"
            type="text"
            placeholder="Ej. Jefe de Almacén Planta Gambetta"
            class="text-input"
            maxlength="100"
          />
        </div>

        <div class="field-item full-width">
          <label for="ent-incidencia">Incidencias de Descarga / Novedades</label>
          <input
            id="ent-incidencia"
            v-model.trim="form.detalleIncidencia"
            type="text"
            placeholder="Ej. Descarga conforme sin daños en precinto"
            class="text-input"
          />
        </div>

        <div class="field-item full-width">
          <label for="ent-obs">Observaciones Finales / Sello GRE-T</label>
          <textarea
            id="ent-obs"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="Guía de remisión firmada y sellada por el cliente..."
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
          <span>Confirmar Entrega</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { transporteFlotaService } from '../services/transporteFlotaService'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  despacho: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')

const defaultForm = () => {
  const now = new Date().toISOString().slice(0, 16)
  return {
    fechaEntregaConforme: now,
    receptorNombre: '',
    receptorDni: '',
    contactoRecepcion: '',
    detalleIncidencia: 'Sin incidencias. Carga recibida conforme y precintos intactos.',
    observaciones: 'Guía de remisión firmada y sellada por el cliente.'
  }
}

const form = ref(defaultForm())

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      errorMessage.value = ''
      form.value = defaultForm()
      if (props.despacho?.contactoRecepcion) {
        form.value.contactoRecepcion = props.despacho.contactoRecepcion
      }
      if (props.despacho?.receptorNombre) {
        form.value.receptorNombre = props.despacho.receptorNombre
      }
      if (props.despacho?.receptorDni) {
        form.value.receptorDni = props.despacho.receptorDni
      }
    }
  }
)

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  if (!props.despacho) return
  errorMessage.value = ''
  if (!form.value.fechaEntregaConforme || !form.value.receptorNombre || !form.value.receptorDni) {
    errorMessage.value = 'Por favor complete la fecha, el nombre y el DNI del receptor'
    return
  }

  loading.value = true
  try {
    const payload = {
      fechaEntregaConforme: form.value.fechaEntregaConforme,
      receptorNombre: form.value.receptorNombre,
      receptorDni: form.value.receptorDni,
      contactoRecepcion: form.value.contactoRecepcion || null,
      detalleIncidencia: form.value.detalleIncidencia || null,
      observaciones: form.value.observaciones || null
    }

    const despachoId = props.despacho.idDespachoTransporte || props.despacho.idDespacho
    const res = await transporteFlotaService.confirmarEntrega(despachoId, payload)
    emit('saved', res)
    handleClose()
  } catch (err) {
    console.error('Error al confirmar entrega:', err)
    errorMessage.value =
      err.response?.data?.message || 'Error al confirmar la entrega en destino.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.despacho-summary {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
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
