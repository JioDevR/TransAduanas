<template>
  <BaseModal
    :visible="visible"
    title="Registro de Ingreso a Depósito Temporal (Balanza VGM)"
    subtitle="Control de pesaje verificado SOLAS, precinto y ubicación de carga aduanera"
    icon="pi pi-box"
    max-width="700px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <div class="form-grid">
        <!-- Expediente Aduanero -->
        <div class="field-item">
          <label for="alm-expediente">Expediente Aduanero Vinculado *</label>
          <select id="alm-expediente" v-model="form.idExpediente" class="select-input" required>
            <option :value="null" disabled>Seleccione Expediente...</option>
            <option v-for="exp in expedientesList" :key="exp.idExpediente" :value="exp.idExpediente">
              {{ exp.codigoExpediente }} — {{ exp.clienteRazonSocial || 'Cliente' }} (Canal: {{ exp.canalSunat || 'PENDIENTE' }})
            </option>
          </select>
          <span class="field-hint">Expediente aduanero al que corresponde la carga</span>
        </div>

        <!-- Sede / Depósito Temporal -->
        <div class="field-item">
          <label for="alm-sede">Sede / Depósito Autorizado *</label>
          <select id="alm-sede" v-model="form.idSedeDeposito" class="select-input" required>
            <option :value="null" disabled>Seleccione Terminal/Depósito...</option>
            <option v-for="s in sedesList" :key="s.idSede" :value="s.idSede">
              {{ s.codigoSede }} — {{ s.nombre }} ({{ s.distrito }})
            </option>
          </select>
        </div>

        <!-- Ticket de Balanza VGM -->
        <div class="field-item">
          <label for="alm-ticket">Ticket de Pesaje Balanza VGM *</label>
          <input
            id="alm-ticket"
            v-model.trim="form.ticketBalanzaVgm"
            type="text"
            placeholder="Ej. TKT-VGM-2026-90412"
            class="text-input font-mono"
            required
            maxlength="30"
          />
          <span class="field-hint">Certificado VGM conforme al convenio SOLAS</span>
        </div>

        <!-- Tipo de Ingreso -->
        <div class="field-item">
          <label for="alm-tipo-ingreso">Modalidad de Ingreso *</label>
          <select id="alm-tipo-ingreso" v-model="form.tipoIngreso" class="select-input" required>
            <option value="DIRECTO_VERDE">DIRECTO_VERDE (Levante Inmediato)</option>
            <option value="POST_INSPECCION">POST_INSPECCION (Revisión Naranja/Roja)</option>
          </select>
        </div>

        <!-- Peso Bruto Registrado (kg) -->
        <div class="field-item">
          <label for="alm-peso-bruto">Peso Bruto Registrado (kg) *</label>
          <input
            id="alm-peso-bruto"
            v-model.number="form.pesoBrutoRegistradoKg"
            type="number"
            step="0.01"
            min="1"
            placeholder="Ej. 24500.00"
            class="text-input"
            required
          />
        </div>

        <!-- Peso Tara (kg) -->
        <div class="field-item">
          <label for="alm-peso-tara">Peso Tara Contenedor (kg)</label>
          <input
            id="alm-peso-tara"
            v-model.number="form.pesoTaraRegistradoKg"
            type="number"
            step="0.01"
            min="0"
            placeholder="Ej. 3850.00"
            class="text-input"
          />
        </div>

        <!-- Cantidad de Bultos -->
        <div class="field-item">
          <label for="alm-bultos">Cantidad de Bultos / Pallets *</label>
          <input
            id="alm-bultos"
            v-model.number="form.cantidadBultos"
            type="number"
            min="1"
            placeholder="Ej. 540"
            class="text-input"
            required
          />
        </div>

        <!-- Ubicación en Bahía / Almacén -->
        <div class="field-item">
          <label for="alm-bahia">Ubicación de Bahía / Patio</label>
          <input
            id="alm-bahia"
            v-model.trim="form.ubicacionBahia"
            type="text"
            placeholder="Ej. BAHIA-D4 / RACK-12"
            class="text-input uppercase font-mono"
            maxlength="30"
          />
        </div>

        <!-- Días Libres de Almacenaje -->
        <div class="field-item">
          <label for="alm-dias-libres">Días Libres de Almacenaje (Terminal)</label>
          <input
            id="alm-dias-libres"
            v-model.number="form.diasLibresAlmacenaje"
            type="number"
            min="1"
            max="30"
            class="text-input"
          />
          <span class="field-hint">Días sin sobrestadía antes de generar sobrecostos</span>
        </div>

        <!-- Precinto Verificado -->
        <div class="field-item">
          <label for="alm-precinto">¿Precinto Verificado Físicamente?</label>
          <select id="alm-precinto" v-model="form.precintoVerificado" class="select-input">
            <option :value="true">Sí (Precinto Conforme e Intacto)</option>
            <option :value="false">No (Observación / Pendiente)</option>
          </select>
        </div>

        <!-- Observaciones -->
        <div class="field-item full-width">
          <label for="alm-obs">Observaciones del Depósito</label>
          <textarea
            id="alm-obs"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="Anotaciones de estado físico del contenedor o embalaje..."
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
          <span>Registrar Ingreso VGM</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { almacenService } from '../services/almacenService'
import { expedienteService } from '../services/expedienteService'
import { sedeService } from '@/features/admin/sedes/services/sedeService'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')
const expedientesList = ref([])
const sedesList = ref([])

const defaultForm = () => ({
  idExpediente: null,
  idSedeDeposito: null,
  ticketBalanzaVgm: '',
  pesoBrutoRegistradoKg: 0,
  pesoTaraRegistradoKg: 0,
  cantidadBultos: 1,
  ubicacionBahia: '',
  tipoIngreso: 'DIRECTO_VERDE',
  precintoVerificado: true,
  diasLibresAlmacenaje: 7,
  observaciones: ''
})

const form = ref(defaultForm())

const loadAuxData = async () => {
  try {
    const [exps, sedes] = await Promise.all([
      expedienteService.getExpedientes(),
      sedeService.getSedes(true)
    ])
    expedientesList.value = exps || []
    sedesList.value = sedes || []
    if (sedesList.value.length > 0 && !form.value.idSedeDeposito) {
      form.value.idSedeDeposito = sedesList.value[0].idSede
    }
    if (expedientesList.value.length > 0 && !form.value.idExpediente) {
      form.value.idExpediente = expedientesList.value[0].idExpediente
    }
  } catch (err) {
    console.error('Error cargando listas auxiliares para almacén:', err)
  }
}

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      errorMessage.value = ''
      form.value = defaultForm()
      loadAuxData()
    }
  }
)

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!form.value.idExpediente || !form.value.idSedeDeposito || !form.value.ticketBalanzaVgm || form.value.pesoBrutoRegistradoKg <= 0) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*)'
    return
  }

  loading.value = true
  try {
    const payload = {
      idExpediente: form.value.idExpediente,
      idSedeDeposito: form.value.idSedeDeposito,
      ticketBalanzaVgm: form.value.ticketBalanzaVgm,
      pesoBrutoRegistradoKg: Number(form.value.pesoBrutoRegistradoKg),
      pesoTaraRegistradoKg: Number(form.value.pesoTaraRegistradoKg || 0),
      cantidadBultos: Number(form.value.cantidadBultos),
      ubicacionBahia: form.value.ubicacionBahia || null,
      tipoIngreso: form.value.tipoIngreso,
      precintoVerificado: form.value.precintoVerificado,
      diasLibresAlmacenaje: Number(form.value.diasLibresAlmacenaje || 7),
      observaciones: form.value.observaciones || null
    }

    const resultado = await almacenService.createIngresoAlmacen(payload)
    emit('saved', resultado)
    handleClose()
  } catch (err) {
    console.error('Error al registrar ingreso de almacén:', err)
    errorMessage.value =
      err.response?.data?.message || 'Error al registrar ingreso a depósito temporal.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.uppercase {
  text-transform: uppercase;
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
