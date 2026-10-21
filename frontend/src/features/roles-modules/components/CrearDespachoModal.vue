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
          <select id="desp-expediente" v-model="form.idExpediente" class="select-input" required>
            <option :value="null" disabled>Seleccione Expediente Aduanero...</option>
            <option v-for="exp in expedientesList" :key="exp.idExpediente" :value="exp.idExpediente">
              {{ exp.codigoExpediente }} — {{ exp.clienteRazonSocial }} (DAM: {{ exp.numeroDam || 'S/N' }} - Levante: {{ exp.fechaLevanteAutorizacion ? 'AUTORIZADO' : 'PENDIENTE' }})
            </option>
          </select>
          <span class="field-hint">Solo se pueden despachar expedientes que cuenten con levante o retiro de depósito</span>
        </div>

        <!-- Asignación de Recursos Opcional Inmediata -->
        <div class="field-item">
          <label for="desp-chofer">Conductor Asignado</label>
          <select id="desp-chofer" v-model="form.idConductor" class="select-input">
            <option :value="null">Sin conductor asignado (Pendiente)</option>
            <option v-for="c in conductoresList" :key="c.idConductor" :value="c.idConductor">
              {{ c.nombres }} {{ c.apellidos }} (Brevete: {{ c.numeroLicencia }})
            </option>
          </select>
        </div>

        <div class="field-item">
          <label for="desp-unidad">Unidad / Tracto Asignado</label>
          <select id="desp-unidad" v-model="form.idUnidad" class="select-input">
            <option :value="null">Sin unidad asignada (Pendiente)</option>
            <option v-for="u in unidadesList" :key="u.idUnidad" :value="u.idUnidad">
              Placa: {{ u.placaTracto }} ({{ u.marcaModelo || 'Tracto' }} - {{ u.estadoOperativo }})
            </option>
          </select>
        </div>

        <!-- Guía de Remisión -->
        <div class="field-item">
          <label for="desp-guia">Guía de Remisión de Transporte (GRE-T) *</label>
          <input
            id="desp-guia"
            v-model.trim="form.guiaRemisionTransporte"
            type="text"
            placeholder="Ej. T001-0004819"
            class="text-input font-mono uppercase"
            required
            maxlength="30"
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
          <input
            id="desp-dir-salida"
            v-model.trim="form.direccionPartida"
            type="text"
            placeholder="Ej. Av. Contralmirante Mora 590, Callao"
            class="text-input"
            required
            maxlength="200"
          />
        </div>

        <!-- Dirección Llegada (Destino) -->
        <div class="field-item">
          <label for="desp-dir-llegada">Dirección de Destino / Planta Cliente *</label>
          <input
            id="desp-dir-llegada"
            v-model.trim="form.direccionLlegada"
            type="text"
            placeholder="Ej. Av. Los Industriales 1240, Parque Industrial"
            class="text-input"
            required
            maxlength="200"
          />
        </div>

        <div class="field-item">
          <label for="desp-dist-llegada">Distrito de Llegada *</label>
          <input
            id="desp-dist-llegada"
            v-model.trim="form.distritoLlegada"
            type="text"
            placeholder="Ej. Lurín"
            class="text-input"
            required
            maxlength="100"
          />
        </div>

        <!-- Contacto Recepción -->
        <div class="field-item full-width">
          <label for="desp-contacto">Persona de Contacto en Recepción</label>
          <input
            id="desp-contacto"
            v-model.trim="form.contactoRecepcion"
            type="text"
            placeholder="Ej. Ing. Carlos Mendoza (Jefe de Almacén Planta)"
            class="text-input"
            maxlength="100"
          />
        </div>

        <!-- Observaciones -->
        <div class="field-item full-width">
          <label for="desp-obs">Instrucciones de Ruta</label>
          <textarea
            id="desp-obs"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="Precauciones de pesaje, precintos de seguridad o ventanas de descarga..."
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
          <span>Programar Despacho</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { transporteFlotaService } from '../services/transporteFlotaService'
import { expedienteService } from '../services/expedienteService'

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

const defaultForm = () => {
  const now = new Date()
  now.setHours(now.getHours() + 2)
  const fechaIso = now.toISOString().slice(0, 16)

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
