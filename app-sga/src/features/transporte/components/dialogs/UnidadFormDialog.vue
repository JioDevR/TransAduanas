<template>
  <Dialog
    :visible="visible"
    modal
    :header="esEdicion ? `Editar Unidad · ${unidad?.placaTracto}` : 'Registrar Nueva Unidad de Flota'"
    :style="{ width: getDialogWidth('640px') }"
    :closable="!loading"
    @update:visible="(v) => !v && handleClose()"
  >
    <form class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-placa">Placa Tracto / Principal *</label>
          <InputText
            id="unidad-placa"
            v-model.trim="form.placaTracto"
            placeholder="Ej. V1A-780"
            maxlength="15"
            required
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-carreta">Placa Carreta / Remolque</label>
          <InputText
            id="unidad-carreta"
            v-model.trim="form.placaCarreta"
            placeholder="Ej. T1A-981 (Opcional)"
            maxlength="15"
            fluid
          />
        </div>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-marca">Marca y Modelo *</label>
          <InputText
            id="unidad-marca"
            v-model.trim="form.marcaModelo"
            placeholder="Ej. Volvo FH 500 Globetrotter"
            required
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-tipo">Tipo de Vehículo *</label>
          <Select
            inputId="unidad-tipo"
            v-model="form.tipoVehiculo"
            :options="TIPO_VEHICULO_OPTIONS"
            optionLabel="label"
            optionValue="value"
            fluid
          />
        </div>
      </div>

      <div class="sga-grid-3">
        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-capacidad">Capacidad (kg) *</label>
          <InputNumber
            inputId="unidad-capacidad"
            v-model="form.capacidadCargaKg"
            :min="500"
            :max="120000"
            suffix=" kg"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-ano">Año Fabricación</label>
          <InputNumber
            inputId="unidad-ano"
            v-model="form.anoFabricacion"
            :min="1980"
            :max="2035"
            :useGrouping="false"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-km">Kilometraje Actual</label>
          <InputNumber
            inputId="unidad-km"
            v-model="form.kilometrajeActual"
            :min="0"
            suffix=" km"
            fluid
          />
        </div>
      </div>

      <div class="sga-grid-3">
        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-soat">Vencimiento SOAT</label>
          <InputText
            id="unidad-soat"
            v-model="form.soatVencimiento"
            type="date"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-citv">Vencimiento CITV</label>
          <InputText
            id="unidad-citv"
            v-model="form.citvVencimiento"
            type="date"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="unidad-estado">Estado Operativo</label>
          <Select
            inputId="unidad-estado"
            v-model="form.estadoOperativo"
            :options="ESTADO_FLOTA_OPTIONS"
            optionLabel="label"
            optionValue="value"
            fluid
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="unidad-obs">Observaciones Técnicas</label>
        <Textarea
          id="unidad-obs"
          v-model.trim="form.observaciones"
          rows="2"
          placeholder="Equipamiento GPS, suspensión, restricciones de carga..."
          fluid
        />
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger">
        <i class="pi pi-exclamation-circle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-dialog-footer">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          :disabled="loading"
          @click="handleClose"
        />
        <Button
          type="submit"
          :label="esEdicion ? 'Guardar Cambios' : 'Registrar Unidad'"
          icon="pi pi-check"
          :loading="loading"
        />
      </div>
    </form>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { useNotification } from '@/shared/composables/useNotification'
import { transporteFlotaService } from '../../services/transporteFlotaService'
import { TIPO_VEHICULO_OPTIONS, ESTADO_FLOTA_OPTIONS } from '../../utils/transporteFormatters'

const props = defineProps({
  visible: { type: Boolean, default: false },
  unidad: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()
const { showSuccess } = useNotification()

const loading = ref(false)
const errorMessage = ref('')

const esEdicion = computed(() => Boolean(props.unidad?.idUnidad))

const form = ref({
  placaTracto: '',
  placaCarreta: '',
  marcaModelo: '',
  tipoVehiculo: 'TRACTO',
  capacidadCargaKg: 30000,
  anoFabricacion: new Date().getFullYear(),
  kilometrajeActual: 0,
  soatVencimiento: '',
  citvVencimiento: '',
  estadoOperativo: 'DISPONIBLE',
  observaciones: ''
})

watch(
  () => [props.visible, props.unidad],
  ([vis, u]) => {
    if (!vis) return
    errorMessage.value = ''
    if (u?.idUnidad) {
      form.value = {
        placaTracto: u.placaTracto || '',
        placaCarreta: u.placaCarreta || '',
        marcaModelo: u.marcaModelo || '',
        tipoVehiculo: u.tipoVehiculo || 'TRACTO',
        capacidadCargaKg: Number(u.capacidadCargaKg || 30000),
        anoFabricacion: u.anoFabricacion || new Date().getFullYear(),
        kilometrajeActual: Number(u.kilometrajeActual || 0),
        soatVencimiento: u.soatVencimiento ? String(u.soatVencimiento).slice(0, 10) : '',
        citvVencimiento: u.citvVencimiento ? String(u.citvVencimiento).slice(0, 10) : '',
        estadoOperativo: u.estadoOperativo || 'DISPONIBLE',
        observaciones: u.observaciones || ''
      }
    } else {
      form.value = {
        placaTracto: '',
        placaCarreta: '',
        marcaModelo: '',
        tipoVehiculo: 'TRACTO',
        capacidadCargaKg: 30000,
        anoFabricacion: new Date().getFullYear(),
        kilometrajeActual: 0,
        soatVencimiento: '',
        citvVencimiento: '',
        estadoOperativo: 'DISPONIBLE',
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
  loading.value = true
  errorMessage.value = ''
  try {
    const payload = {
      placaTracto: form.value.placaTracto.toUpperCase(),
      placaCarreta: form.value.placaCarreta ? form.value.placaCarreta.toUpperCase() : null,
      marcaModelo: form.value.marcaModelo,
      tipoVehiculo: form.value.tipoVehiculo,
      capacidadCargaKg: form.value.capacidadCargaKg,
      anoFabricacion: form.value.anoFabricacion || null,
      kilometrajeActual: form.value.kilometrajeActual ?? 0,
      soatVencimiento: form.value.soatVencimiento || null,
      citvVencimiento: form.value.citvVencimiento || null,
      estadoOperativo: form.value.estadoOperativo,
      estadoActivo: props.unidad?.estadoActivo ?? true,
      observaciones: form.value.observaciones || null
    }

    if (esEdicion.value) {
      await transporteFlotaService.updateUnidad(props.unidad.idUnidad, payload)
      showSuccess(`Unidad ${payload.placaTracto} actualizada correctamente.`)
    } else {
      await transporteFlotaService.createUnidad(payload)
      showSuccess(`Unidad ${payload.placaTracto} registrada en el parque automotor.`)
    }
    emit('saved')
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || err?.message || 'Error al guardar la unidad.'
  } finally {
    loading.value = false
  }
}
</script>
