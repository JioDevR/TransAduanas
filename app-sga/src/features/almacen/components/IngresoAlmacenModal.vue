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
          <Select
            inputId="alm-expediente"
            v-model="form.idExpediente"
            :options="expedienteOptions"
            optionLabel="label"
            optionValue="value"
            filter
            placeholder="Seleccione Expediente..."
            fluid
          />
          <span class="field-hint">Expediente aduanero al que corresponde la carga</span>
        </div>

        <!-- Sede / Depósito Temporal -->
        <div class="field-item">
          <label for="alm-sede">Sede / Depósito Autorizado *</label>
          <Select
            inputId="alm-sede"
            v-model="form.idSedeDeposito"
            :options="sedeOptions"
            optionLabel="label"
            optionValue="value"
            filter
            placeholder="Seleccione Terminal/Depósito..."
            fluid
          />
        </div>

        <!-- Ticket de Balanza VGM -->
        <div class="field-item">
          <label for="alm-ticket">Ticket de Pesaje Balanza VGM *</label>
          <InputText
            id="alm-ticket"
            v-model.trim="form.ticketBalanzaVgm"
            placeholder="Ej. TKT-VGM-2026-90412"
            class="font-mono"
            required
            maxlength="30"
            fluid
          />
          <span class="field-hint">Certificado VGM conforme al convenio SOLAS</span>
        </div>

        <!-- Tipo de Ingreso -->
        <div class="field-item">
          <label for="alm-tipo-ingreso">Modalidad de Ingreso *</label>
          <Select
            inputId="alm-tipo-ingreso"
            v-model="form.tipoIngreso"
            :options="tipoIngresoOptions"
            optionLabel="label"
            optionValue="value"
            fluid
          />
        </div>

        <!-- Peso Bruto Registrado (kg) -->
        <div class="field-item">
          <label for="alm-peso-bruto">Peso Bruto Registrado (kg) *</label>
          <InputNumber
            inputId="alm-peso-bruto"
            v-model="form.pesoBrutoRegistradoKg"
            :min="1"
            :minFractionDigits="2"
            :maxFractionDigits="2"
            suffix=" kg"
            placeholder="Ej. 24500.00"
            fluid
          />
        </div>

        <!-- Peso Tara (kg) -->
        <div class="field-item">
          <label for="alm-peso-tara">Peso Tara Contenedor (kg)</label>
          <InputNumber
            inputId="alm-peso-tara"
            v-model="form.pesoTaraRegistradoKg"
            :min="0"
            :minFractionDigits="2"
            :maxFractionDigits="2"
            suffix=" kg"
            placeholder="Ej. 3850.00"
            fluid
          />
        </div>

        <!-- Cantidad de Bultos -->
        <div class="field-item">
          <label for="alm-bultos">Cantidad de Bultos / Pallets *</label>
          <InputNumber
            inputId="alm-bultos"
            v-model="form.cantidadBultos"
            :min="1"
            :useGrouping="false"
            placeholder="Ej. 540"
            fluid
          />
        </div>

        <!-- Ubicación en Bahía / Almacén -->
        <div class="field-item">
          <label for="alm-bahia">Ubicación de Bahía / Patio</label>
          <InputText
            id="alm-bahia"
            v-model.trim="form.ubicacionBahia"
            placeholder="Ej. BAHIA-D4 / RACK-12"
            class="uppercase font-mono"
            maxlength="30"
            fluid
          />
        </div>

        <!-- Días Libres de Almacenaje -->
        <div class="field-item">
          <label for="alm-dias-libres">Días Libres de Almacenaje (Terminal)</label>
          <InputNumber
            inputId="alm-dias-libres"
            v-model="form.diasLibresAlmacenaje"
            :min="1"
            :max="30"
            :useGrouping="false"
            fluid
          />
          <span class="field-hint">Días sin sobrestadía antes de generar sobrecostos</span>
        </div>

        <!-- Precinto Verificado -->
        <div class="field-item">
          <label for="alm-precinto">¿Precinto Verificado Físicamente?</label>
          <Select
            inputId="alm-precinto"
            v-model="form.precintoVerificado"
            :options="precintoOptions"
            optionLabel="label"
            optionValue="value"
            fluid
          />
        </div>

        <!-- Observaciones -->
        <div class="field-item full-width">
          <label for="alm-obs">Observaciones del Depósito</label>
          <Textarea
            id="alm-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Anotaciones de estado físico del contenedor o embalaje..."
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
          label="Registrar Ingreso VGM"
          icon="pi pi-check"
          :loading="loading"
        />
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import BaseModal from '@/shared/components/BaseModal.vue'
import { almacenService } from '../services/almacenService'
import { expedienteService } from '@/shared/services/expedienteService'
import { sedeService } from '@/features/admin/sedes/services/sedeService'

const tipoIngresoOptions = [
  { label: 'DIRECTO_VERDE (Levante Inmediato)', value: 'DIRECTO_VERDE' },
  { label: 'POST_INSPECCION (Revisión Naranja/Roja)', value: 'POST_INSPECCION' }
]

const precintoOptions = [
  { label: 'Sí (Precinto Conforme e Intacto)', value: true },
  { label: 'No (Observación / Pendiente)', value: false }
]

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  expediente: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')
const expedientesList = ref([])
const sedesList = ref([])

const expedienteOptions = computed(() =>
  expedientesList.value.map((exp) => ({
    label: `${exp.codigoExpediente} — ${exp.clienteRazonSocial || 'Cliente'} (Canal: ${exp.canalSunat || 'PENDIENTE'})`,
    value: exp.idExpediente
  }))
)

const sedeOptions = computed(() =>
  sedesList.value.map((s) => ({
    label: `${s.codigoSede} — ${s.nombre} (${s.distrito})`,
    value: s.idSede
  }))
)

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
    const sedesPromise = sedeService.getSedes(true)
    const expsPromise = props.expediente?.idExpediente
      ? Promise.resolve([props.expediente])
      : expedienteService.getExpedientes()
    const [exps, sedes] = await Promise.all([expsPromise, sedesPromise])
    expedientesList.value = exps || []
    sedesList.value = sedes || []
    if (sedesList.value.length > 0 && !form.value.idSedeDeposito) {
      form.value.idSedeDeposito = sedesList.value[0].idSede
    }
    if (props.expediente?.idExpediente) {
      form.value.idExpediente = props.expediente.idExpediente
    } else if (expedientesList.value.length > 0 && !form.value.idExpediente) {
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
      if (props.expediente?.idExpediente) {
        form.value.idExpediente = props.expediente.idExpediente
      }
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
