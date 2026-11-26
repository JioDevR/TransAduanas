<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('700px'), maxWidth: '95vw' }"
    header="Registro de Ingreso a Depósito Temporal (Balanza VGM)"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="ingreso-almacen-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial || 'Cliente asignado' }}
          </span>
        </div>
        <Tag
          :value="`Canal ${expediente.canalSunat || 'PENDIENTE'}`"
          :severity="expediente.canalSunat === 'ROJO' ? 'danger' : (expediente.canalSunat === 'NARANJA' ? 'warn' : 'success')"
          class="sga-mini-tag"
          rounded
        />
      </div>

      <div class="sga-banner is-info">
        <i class="pi pi-box"></i>
        <span>
          Control de pesaje verificado SOLAS (Balanza VGM), verificación de precinto y ubicación de bahía en depósito temporal.
        </span>
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label for="alm-expediente" class="sga-form-label">Expediente Aduanero Vinculado *</label>
          <Select
            inputId="alm-expediente"
            v-model="form.idExpediente"
            :options="expedienteOptions"
            optionLabel="label"
            optionValue="value"
            filter
            placeholder="Seleccione Expediente..."
            size="small"
            fluid
          />
          <span class="sga-field-hint">Expediente aduanero al que corresponde la carga</span>
        </div>

        <div class="sga-form-field">
          <label for="alm-sede" class="sga-form-label">Sede / Depósito Autorizado *</label>
          <Select
            inputId="alm-sede"
            v-model="form.idSedeDeposito"
            :options="sedeOptions"
            optionLabel="label"
            optionValue="value"
            filter
            placeholder="Seleccione Terminal/Depósito..."
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="alm-ticket" class="sga-form-label">Ticket de Pesaje Balanza VGM *</label>
          <InputText
            id="alm-ticket"
            v-model.trim="form.ticketBalanzaVgm"
            placeholder="Ej. TKT-VGM-2026-90412"
            class="font-mono"
            required
            maxlength="30"
            size="small"
            fluid
          />
          <span class="sga-field-hint">Certificado VGM conforme al convenio SOLAS</span>
        </div>

        <div class="sga-form-field">
          <label for="alm-tipo-ingreso" class="sga-form-label">Modalidad de Ingreso *</label>
          <Select
            inputId="alm-tipo-ingreso"
            v-model="form.tipoIngreso"
            :options="tipoIngresoOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="alm-peso-bruto" class="sga-form-label">Peso Bruto Registrado (kg) *</label>
          <InputNumber
            inputId="alm-peso-bruto"
            v-model="form.pesoBrutoRegistradoKg"
            :min="1"
            :minFractionDigits="2"
            :maxFractionDigits="2"
            suffix=" kg"
            placeholder="Ej. 24500.00"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="alm-peso-tara" class="sga-form-label">Peso Tara Contenedor (kg)</label>
          <InputNumber
            inputId="alm-peso-tara"
            v-model="form.pesoTaraRegistradoKg"
            :min="0"
            :minFractionDigits="2"
            :maxFractionDigits="2"
            suffix=" kg"
            placeholder="Ej. 3850.00"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="alm-bultos" class="sga-form-label">Cantidad de Bultos / Pallets *</label>
          <InputNumber
            inputId="alm-bultos"
            v-model="form.cantidadBultos"
            :min="1"
            :useGrouping="false"
            placeholder="Ej. 540"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="alm-bahia" class="sga-form-label">Ubicación de Bahía / Patio</label>
          <InputText
            id="alm-bahia"
            v-model.trim="form.ubicacionBahia"
            placeholder="Ej. BAHIA-D4 / RACK-12"
            class="uppercase font-mono"
            maxlength="30"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="alm-dias-libres" class="sga-form-label">Días Libres de Almacenaje (Terminal)</label>
          <InputNumber
            inputId="alm-dias-libres"
            v-model="form.diasLibresAlmacenaje"
            :min="1"
            :max="30"
            :useGrouping="false"
            size="small"
            fluid
          />
          <span class="sga-field-hint">Días sin sobrestadía antes de generar sobrecostos</span>
        </div>

        <div class="sga-form-field">
          <label for="alm-precinto" class="sga-form-label">¿Precinto Verificado Físicamente?</label>
          <Select
            inputId="alm-precinto"
            v-model="form.precintoVerificado"
            :options="precintoOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="alm-obs" class="sga-form-label">Observaciones del Depósito</label>
          <Textarea
            id="alm-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Anotaciones de estado físico del contenedor o embalaje..."
            size="small"
            fluid
          />
        </div>
      </div>
    </form>

    <template #footer>
      <Button
        type="button"
        label="Cancelar"
        severity="secondary"
        outlined
        size="small"
        :disabled="loading"
        @click="handleClose"
      />
      <Button
        type="submit"
        form="ingreso-almacen-form"
        label="Registrar Ingreso VGM"
        icon="pi pi-check"
        size="small"
        :loading="loading"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
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
const { getDialogWidth } = useResponsive()

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
  errorMessage.value = ''
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

.full-span {
  grid-column: 1 / -1;
}
</style>
