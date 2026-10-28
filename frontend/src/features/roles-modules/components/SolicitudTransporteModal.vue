<template>
  <BaseModal
    :visible="visible"
    title="Solicitar Despacho Terrestre a Destino"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — Cliente: ${expediente?.clienteRazonSocial || ''}`"
    icon="pi pi-truck"
    max-width="600px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Info Banner -->
      <div class="transporte-info-banner">
        <i class="pi pi-send text-lg"></i>
        <div class="banner-texts">
          <strong>Handoff Operativo a Supervisor de Transporte y Flota</strong>
          <p>
            Al confirmar, se generará una orden en estado <strong>PENDIENTE DE ASIGNACIÓN</strong> en la bandeja de Transporte y Flota para la programación de camión, plataforma y chofer autorizado.
          </p>
        </div>
      </div>

      <div class="transporte-fields-grid">
        <div class="field-item full-width">
          <label for="dir-partida">Punto de Partida (Terminal / Depósito) *</label>
          <input
            id="dir-partida"
            v-model.trim="form.direccionPartida"
            type="text"
            class="text-input"
            required
            placeholder="Ej. DP World Callao - Terminal Sur / Almacén Ransa"
          />
        </div>

        <div class="field-item">
          <label for="dir-llegada">Dirección de Destino / Entrega *</label>
          <input
            id="dir-llegada"
            v-model.trim="form.direccionLlegada"
            type="text"
            class="text-input"
            required
            placeholder="Ej. Av. Los Industriales 450, Almacén Central"
          />
        </div>

        <div class="field-item">
          <label for="distrito-llegada">Distrito de Llegada *</label>
          <input
            id="distrito-llegada"
            v-model.trim="form.distritoLlegada"
            type="text"
            class="text-input"
            required
            placeholder="Ej. Ate / Callao / San Luis"
          />
        </div>

        <div class="field-item">
          <label for="contacto-recep">Contacto de Recepción</label>
          <input
            id="contacto-recep"
            v-model.trim="form.contactoRecepcion"
            type="text"
            class="text-input"
            placeholder="Ej. Juan Pérez - Jefe Almacén (999-888-777)"
          />
        </div>

        <div class="field-item">
          <label for="fecha-prog">Fecha Programada de Traslado *</label>
          <input
            id="fecha-prog"
            v-model="form.fechaProgramada"
            type="datetime-local"
            class="text-input"
            required
          />
        </div>

        <div class="field-item full-width">
          <label for="obs-trans">Instrucciones Especiales de Descarga</label>
          <textarea
            id="obs-trans"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="Requiere rampa hidráulica, montacargas en destino o ventana horaria restringida..."
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
          <i v-else class="pi pi-send"></i>
          <span>Enviar Solicitud a Transporte</span>
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
  direccionPartida: 'Terminal Portuario DP World / APM Terminals Callao',
  direccionLlegada: '',
  distritoLlegada: '',
  contactoRecepcion: '',
  fechaProgramada: '',
  observaciones: ''
})

watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      // Formatear mañana a las 08:00 AM como sugerencia por defecto
      const tomorrow = new Date()
      tomorrow.setDate(tomorrow.getDate() + 1)
      tomorrow.setHours(8, 0, 0, 0)
      const tzOffset = tomorrow.getTimezoneOffset() * 60000
      const localISOTime = new Date(tomorrow.getTime() - tzOffset).toISOString().slice(0, 16)

      form.value = {
        direccionPartida: 'Terminal Portuario DP World / APM Terminals Callao',
        direccionLlegada: exp.clienteDireccion || 'Almacén del Cliente',
        distritoLlegada: exp.clienteDistrito || 'Callao',
        contactoRecepcion: exp.contactoExteriorNombre || exp.clienteRazonSocial || '',
        fechaProgramada: localISOTime,
        observaciones: `Carga: Contenedor ${exp.numeroContenedor || 'FCL'} / Precinto: ${exp.precintoAduanero || 'S/N'}`
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
      direccionPartida: form.value.direccionPartida,
      direccionLlegada: form.value.direccionLlegada,
      distritoLlegada: form.value.distritoLlegada,
      contactoRecepcion: form.value.contactoRecepcion || null,
      fechaProgramada: `${form.value.fechaProgramada}:00`,
      observaciones: form.value.observaciones || null
    }

    const updated = await expedienteService.solicitarTransporte(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al enviar solicitud de despacho a Transporte.'
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

.transporte-info-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  padding: 0.85rem 1rem;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
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

.transporte-fields-grid {
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
  background: #1e3a5f;
  border: 1px solid #142842;
  color: #ffffff;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-save:hover:not(:disabled) {
  background: #2c5282;
}

.btn-modal-save:disabled,
.btn-modal-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
