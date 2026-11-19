<template>
  <BaseModal
    :visible="visible"
    title="Programación de Despacho de Transporte Terrestre"
    subtitle="Emisión de orden de traslado local con levante aduanero y guía de remisión"
    icon="pi pi-truck"
    max-width="750px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <div class="form-grid">
        <!-- Expediente Aduanero -->
        <div class="field-item full-width">
          <label for="desp-expediente">Expediente Aduanero (Con Levante Autorizado) *</label>
          <Select
            inputId="desp-expediente"
            v-model="form.idExpediente"
            :options="expedienteOptions"
            optionLabel="label"
            optionValue="value"
            filter
            placeholder="Seleccione Expediente Aduanero..."
            fluid
          />
          <span class="field-hint">Solo se pueden despachar expedientes que cuenten con levante o retiro de depósito</span>
        </div>

        <!-- Asignación de Recursos Opcional Inmediata -->
        <div class="field-item">
          <label for="desp-chofer">Conductor Asignado</label>
          <Select
            inputId="desp-chofer"
            v-model="form.idConductor"
            :options="conductorOptions"
            optionLabel="label"
            optionValue="value"
            filter
            showClear
            placeholder="Sin conductor asignado (Pendiente)"
            fluid
          />
        </div>

        <div class="field-item">
          <label for="desp-unidad">Unidad / Tracto Asignado</label>
          <Select
            inputId="desp-unidad"
            v-model="form.idUnidad"
            :options="unidadOptions"
            optionLabel="label"
            optionValue="value"
            filter
            showClear
            placeholder="Sin unidad asignada (Pendiente)"
            fluid
          />
        </div>

        <!-- Guía de Remisión -->
        <div class="field-item">
          <label for="desp-guia">Guía de Remisión de Transporte (GRE-T) *</label>
          <InputText
            id="desp-guia"
            v-model.trim="form.guiaRemisionTransporte"
            placeholder="Ej. T001-0004819"
            class="font-mono uppercase"
            required
            maxlength="30"
            fluid
          />
        </div>

        <!-- Fecha Programada -->
        <div class="field-item">
          <label for="desp-fecha">Fecha y Hora Programada de Salida *</label>
          <input
            id="desp-fecha"
            v-model="form.fechaProgramada"
            type="datetime-local"
            class="text-input"
            required
          />
        </div>

        <!-- Dirección Partida (Origen) -->
        <div class="field-item full-width">
          <label for="desp-dir-salida">Dirección de Partida (Terminal / Depósito) *</label>
          <InputText
            id="desp-dir-salida"
            v-model.trim="form.direccionPartida"
            placeholder="Ej. Av. Contralmirante Mora 590, Callao"
            required
            maxlength="200"
            fluid
          />
        </div>

        <!-- Dirección Llegada (Destino) -->
        <div class="field-item">
          <label for="desp-dir-llegada">Dirección de Destino / Planta Cliente *</label>
          <InputText
            id="desp-dir-llegada"
            v-model.trim="form.direccionLlegada"
            placeholder="Ej. Av. Los Industriales 1240, Parque Industrial"
            required
            maxlength="200"
            fluid
          />
        </div>

        <div class="field-item">
          <label for="desp-dist-llegada">Distrito de Llegada *</label>
          <InputText
            id="desp-dist-llegada"
            v-model.trim="form.distritoLlegada"
            placeholder="Ej. Lurín"
            required
            maxlength="100"
            fluid
          />
        </div>

        <!-- Contacto Recepción -->
        <div class="field-item full-width">
          <label for="desp-contacto">Persona de Contacto en Recepción</label>
          <InputText
            id="desp-contacto"
            v-model.trim="form.contactoRecepcion"
            placeholder="Ej. Ing. Carlos Mendoza (Jefe de Almacén Planta)"
            maxlength="100"
            fluid
          />
        </div>

        <!-- Observaciones -->
        <div class="field-item full-width">
          <label for="desp-obs">Instrucciones de Ruta</label>
          <Textarea
            id="desp-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Precauciones de pesaje, precintos de seguridad o ventanas de descarga..."
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
          label="Programar Despacho"
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
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import BaseModal from '@/shared/components/BaseModal.vue'
import { transporteFlotaService } from '../services/transporteFlotaService'
import { expedienteService } from '../services/expedienteService'
import { getNowDateTimeInput } from '@/shared/utils/dateUtils'

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
const conductoresList = ref([])
const unidadesList = ref([])

const expedienteOptions = computed(() =>
  expedientesList.value.map((exp) => ({
    label: `${exp.codigoExpediente} — ${exp.clienteRazonSocial} (DAM: ${exp.numeroDam || 'S/N'} - Levante: ${exp.fechaLevanteAutorizacion ? 'AUTORIZADO' : 'PENDIENTE'})`,
    value: exp.idExpediente
  }))
)

const conductorOptions = computed(() =>
  conductoresList.value.map((c) => ({
    label: `${c.nombres} ${c.apellidos} (Brevete: ${c.numeroLicencia})`,
    value: c.idConductor
  }))
)

const unidadOptions = computed(() =>
  unidadesList.value.map((u) => ({
    label: `Placa: ${u.placaTracto} (${u.marcaModelo || 'Tracto'} - ${u.estadoOperativo})`,
    value: u.idUnidad
  }))
)

const defaultForm = () => {
  const now = new Date()
  now.setHours(now.getHours() + 2)
  const fechaIso = getNowDateTimeInput(now)

  return {
    idExpediente: null,
    idConductor: null,
    idUnidad: null,
    guiaRemisionTransporte: '',
    direccionPartida: 'Av. Elmer Faucett 2820, Terminal Portuario Callao',
    direccionLlegada: '',
    distritoLlegada: 'Lima',
    contactoRecepcion: '',
    fechaProgramada: fechaIso,
    observaciones: ''
  }
}

const form = ref(defaultForm())

const loadAux = async () => {
  try {
    const [exps, conds, unids] = await Promise.all([
      expedienteService.getExpedientes(),
      transporteFlotaService.getConductores(true),
      transporteFlotaService.getUnidades({ soloActivos: true })
    ])
    expedientesList.value = exps || []
    conductoresList.value = conds || []
    unidadesList.value = unids || []
    if (expedientesList.value.length > 0 && !form.value.idExpediente) {
      form.value.idExpediente = expedientesList.value[0].idExpediente
    }
  } catch (err) {
    console.error('Error cargando listas de transporte:', err)
  }
}

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      errorMessage.value = ''
      form.value = defaultForm()
      loadAux()
    }
  }
)

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!form.value.idExpediente || !form.value.guiaRemisionTransporte || !form.value.direccionPartida || !form.value.direccionLlegada || !form.value.distritoLlegada) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*)'
    return
  }

  loading.value = true
  try {
    const payload = {
      idExpediente: form.value.idExpediente,
      tipoTraslado: 'ENTREGA_IMPORTACION',
      idConductor: form.value.idConductor,
      idUnidad: form.value.idUnidad,
      guiaRemisionTransporte: form.value.guiaRemisionTransporte,
      direccionPartida: form.value.direccionPartida,
      direccionLlegada: form.value.direccionLlegada,
      distritoLlegada: form.value.distritoLlegada,
      contactoRecepcion: form.value.contactoRecepcion || null,
      fechaProgramada: form.value.fechaProgramada,
      observaciones: form.value.observaciones || null
    }

    const creado = await transporteFlotaService.createDespacho(payload)
    emit('saved', creado)
    handleClose()
  } catch (err) {
    console.error('Error al programar despacho:', err)
    errorMessage.value =
      err.response?.data?.message || 'Error al programar despacho terrestre.'
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
