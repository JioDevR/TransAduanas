<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('660px'), maxWidth: '95vw' }"
    header="Confirmar Embarque Internacional (Zarpe / Despegue)"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="confirmar-embarque-exportacion-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial || 'Exportador asignado' }}
          </span>
        </div>
        <Tag
          :value="form.tipoTransporteInternacional === 'AEREO' ? 'AÉREO' : 'MARÍTIMO'"
          severity="info"
          class="sga-mini-tag"
          rounded
        />
      </div>

      <div class="sga-banner is-info">
        <i class="pi pi-globe"></i>
        <div>
          <strong>Registro Oficial de Zarpe / Salida del País</strong>
          <p class="embarque-banner-desc">
            Confirme que la carga fue embarcada exitosamente en la nave o aeronave asignada con destino al exterior.
          </p>
        </div>
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label for="embarque-tipo" class="sga-form-label">Vía de Transporte Internacional *</label>
          <Select
            inputId="embarque-tipo"
            v-model="form.tipoTransporteInternacional"
            :options="tipoTransporteOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="embarque-vuelo" class="sga-form-label">
            {{ form.tipoTransporteInternacional === 'AEREO' ? 'N° de Vuelo / Aerolínea *' : 'N° de Viaje / Booking' }}
          </label>
          <InputText
            id="embarque-vuelo"
            v-model.trim="form.numeroVuelo"
            :placeholder="form.tipoTransporteInternacional === 'AEREO' ? 'Ej. LA-2462 / LATAM Cargo' : 'Ej. V.042N / BKG-8812'"
            :required="form.tipoTransporteInternacional === 'AEREO'"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="embarque-nave" class="sga-form-label">
            {{ form.tipoTransporteInternacional === 'AEREO' ? 'Aeropuerto Origen y Nave *' : 'Nombre de Buque / Naviera *' }}
          </label>
          <InputText
            id="embarque-nave"
            v-model.trim="form.naveViaje"
            :placeholder="form.tipoTransporteInternacional === 'AEREO' ? 'Ej. Boeing 767-300F (LIM -> MIA)' : 'Ej. MSC ANTWERP / MSC Mediterranean'"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="embarque-etd" class="sga-form-label">Fecha Real de Zarpe / Salida (ETD) *</label>
          <InputText
            id="embarque-etd"
            v-model="form.fechaZarpeEtd"
            type="date"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="embarque-eta" class="sga-form-label">Fecha Estimada Llegada Exterior (ETA)</label>
          <InputText
            id="embarque-eta"
            v-model="form.fechaEstimadaLlegadaExterior"
            type="date"
            :min="form.fechaZarpeEtd || undefined"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="embarque-destino" class="sga-form-label">Puerto / Aeropuerto de Destino en el Exterior *</label>
          <InputText
            id="embarque-destino"
            v-model.trim="form.puertoDestinoExterior"
            placeholder="Ej. Port of Rotterdam (NL) / Miami Intl Airport (US)"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="embarque-obs" class="sga-form-label">Observaciones de Embarque</label>
          <Textarea
            id="embarque-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Ej. Contenedor estibado en bodega, BL emitido y entregado al exportador..."
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
        form="confirmar-embarque-exportacion-form"
        label="Registrar Embarque Internacional"
        icon="pi pi-check-circle"
        size="small"
        :loading="loading"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { expedienteService } from '@/shared/services/expedienteService'
import { getTodayDateInput } from '@/shared/utils/dateUtils'

const tipoTransporteOptions = [
  { label: 'Marítimo (Buque / Nave)', value: 'MARITIMO' },
  { label: 'Aéreo (Vuelo / Aerolínea)', value: 'AEREO' }
]

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()

const loading = ref(false)
const errorMessage = ref('')

const form = ref({
  tipoTransporteInternacional: 'MARITIMO',
  numeroVuelo: '',
  naveViaje: '',
  fechaZarpeEtd: '',
  fechaEstimadaLlegadaExterior: '',
  puertoDestinoExterior: '',
  observaciones: ''
})

const getTodayString = () => {
  return getTodayDateInput()
}

const getFutureDateString = (days) => {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return getTodayDateInput(d)
}

watch(
  () => [props.visible, props.expediente],
  ([vis, exp]) => {
    if (vis && exp) {
      errorMessage.value = ''
      form.value = {
        tipoTransporteInternacional: exp.tipoTransporteInternacional || (exp.modalidad === 'AEREO' ? 'AEREO' : 'MARITIMO'),
        numeroVuelo: exp.numeroVuelo || '',
        naveViaje: exp.naveViaje || '',
        fechaZarpeEtd: exp.fechaZarpeEtd || getTodayString(),
        fechaEstimadaLlegadaExterior: exp.fechaLlegadaEta || getFutureDateString(14),
        puertoDestinoExterior: exp.puertoDestinoNombre || 'Puerto de Destino Internacional',
        observaciones: 'Carga estibada y confirmada en sistema de línea naviera.'
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
  if (
    form.value.fechaZarpeEtd &&
    form.value.fechaEstimadaLlegadaExterior &&
    form.value.fechaEstimadaLlegadaExterior < form.value.fechaZarpeEtd
  ) {
    errorMessage.value = 'La fecha ETA en destino debe ser igual o posterior a la fecha de zarpe (ETD).'
    return
  }
  loading.value = true
  errorMessage.value = ''

  try {
    const payload = {
      tipoTransporteInternacional: form.value.tipoTransporteInternacional,
      numeroVuelo: form.value.numeroVuelo || null,
      naveViaje: form.value.naveViaje,
      fechaZarpeEtd: form.value.fechaZarpeEtd || null,
      fechaEstimadaLlegadaExterior: form.value.fechaEstimadaLlegadaExterior || null,
      puertoDestinoExterior: form.value.puertoDestinoExterior,
      observaciones: form.value.observaciones
    }

    const res = await expedienteService.confirmarEmbarqueOrigen(props.expediente.idExpediente, payload)
    emit('saved', res)
    handleClose()
  } catch (err) {
    console.error('Error al confirmar embarque:', err)
    errorMessage.value = err.response?.data?.message || err.message || 'Error al registrar embarque de exportación.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.embarque-banner-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  line-height: 1.4;
}

.full-span {
  grid-column: 1 / -1;
}
</style>
