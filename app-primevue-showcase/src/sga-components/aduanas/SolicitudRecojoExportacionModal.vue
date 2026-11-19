<template>
  <BaseModal
    :visible="visible"
    title="Solicitar Recojo de Carga en Planta (Exportación)"
    :subtitle="`Expediente: ${expediente?.codigoExpediente || ''} — Exportador: ${expediente?.clienteRazonSocial || ''}`"
    icon="pi pi-truck"
    max-width="620px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Info Banner Exportación -->
      <div class="transporte-info-banner">
        <i class="pi pi-directions text-lg"></i>
        <div class="banner-texts">
          <strong>Traslado Terrestre Origen: Planta del Exportador → Depósito Temporal Portuario/Aeroportuario</strong>
          <p>
            Se generará una orden de transporte con tipo <strong>RECOJO_EXPORTACION</strong> para que el Supervisor de Flota asigne tracto, plataforma y conductor para retirar la carga embalada y llevarla a zona primaria.
          </p>
        </div>
      </div>

      <div class="transporte-fields-grid">
        <div class="field-item full-width">
          <label for="recojo-dir-planta">Dirección de Planta / Almacén de Recojo *</label>
          <InputText
            id="recojo-dir-planta"
            v-model.trim="form.direccionRecojoPlanta"
            required
            placeholder="Ej. Carretera Central Km 18.5, Planta Industrial Ate"
            fluid
          />
        </div>

        <div class="field-item">
          <label for="recojo-distrito">Distrito / Localidad *</label>
          <InputText
            id="recojo-distrito"
            v-model.trim="form.distritoRecojo"
            required
            placeholder="Ej. Ate / Lurín / Huachipa"
            fluid
          />
        </div>

        <div class="field-item">
          <label for="recojo-almacen-dest">Almacén / Depósito Temporal Destino *</label>
          <InputText
            id="recojo-almacen-dest"
            v-model.trim="form.almacenDestinoTerminal"
            required
            placeholder="Ej. DP World Callao / APM Terminals / Depósito Ransa"
            fluid
          />
        </div>

        <div class="field-item">
          <label for="recojo-contacto">Contacto en Planta (Despachador) *</label>
          <InputText
            id="recojo-contacto"
            v-model.trim="form.contactoPlanta"
            required
            placeholder="Ej. Carlos Mendoza (Jefe Despacho - 987654321)"
            fluid
          />
        </div>

        <div class="field-item">
          <label for="recojo-fecha-prog">Fecha y Hora Programada de Carga *</label>
          <input
            id="recojo-fecha-prog"
            v-model="form.fechaProgramadaRecojo"
            type="datetime-local"
            class="text-input"
            required
          />
        </div>

        <div class="field-item full-width">
          <label for="recojo-obs">Instrucciones Especiales / Embalaje</label>
          <Textarea
            id="recojo-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Pales filmados, contenedor precintado o bultos sueltos; verificar peso de balanza antes de salida..."
            fluid
          />
        </div>
      </div>

      <div class="modal-actions">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          @click="handleClose"
          :disabled="loading"
        />
        <Button
          type="submit"
          label="Programar Recojo en Planta"
          icon="pi pi-send"
          :loading="loading"
        />
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import BaseModal from '@/shared/components/BaseModal.vue'
import { expedienteService } from '../services/expedienteService'
import { getNowDateTimeInput } from '@/shared/utils/dateUtils'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')

const form = ref({
  direccionRecojoPlanta: '',
  distritoRecojo: '',
  almacenDestinoTerminal: 'DP World Callao - Terminal Portuario',
  contactoPlanta: '',
  fechaProgramadaRecojo: '',
  observaciones: ''
})

const getTomorrowISOString = () => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  tomorrow.setHours(8, 0, 0, 0)
  return getNowDateTimeInput(tomorrow)
}

watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      errorMessage.value = ''
      form.value = {
        direccionRecojoPlanta: exp.clienteDireccion || 'Planta Principal Explotación / Embalaje',
        distritoRecojo: 'Callao',
        almacenDestinoTerminal: 'DP World Callao - Terminal Portuario',
        contactoPlanta: exp.clienteContacto || exp.clienteRazonSocial || '',
        fechaProgramadaRecojo: getTomorrowISOString(),
        observaciones: 'Coordinar con planta para ingreso de unidad y pesaje en balanza.'
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
      direccionRecojoPlanta: form.value.direccionRecojoPlanta,
      distritoRecojo: form.value.distritoRecojo,
      almacenDestinoTerminal: form.value.almacenDestinoTerminal,
      contactoPlanta: form.value.contactoPlanta,
      fechaProgramadaRecojo: form.value.fechaProgramadaRecojo ? new Date(form.value.fechaProgramadaRecojo).toISOString() : null,
      observaciones: form.value.observaciones
    }

    const res = await expedienteService.solicitarRecojoExportacion(props.expediente.idExpediente, payload)
    emit('saved', res)
    handleClose()
  } catch (err) {
    console.error('Error al solicitar recojo en planta:', err)
    errorMessage.value = err.response?.data?.message || err.message || 'Error al enviar orden de recojo a Flota.'
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

.transporte-info-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: var(--color-info-bg);
  border: 1px solid var(--color-info-border);
  border-radius: 8px;
  padding: 12px 14px;
  color: var(--color-text-main);
}

.transporte-info-banner i {
  color: var(--color-info-text);
  margin-top: 2px;
}

.banner-texts strong {
  display: block;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-info-text);
  margin-bottom: 3px;
}

.banner-texts p {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.35;
  color: var(--color-text-muted);
}

.transporte-fields-grid {
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
