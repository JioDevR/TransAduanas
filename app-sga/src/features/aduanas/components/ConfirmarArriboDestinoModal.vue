<template>
  <BaseModal
    :visible="visible"
    title="Confirmar Arribo a Destino"
    :subtitle="`${expediente?.codigoExpediente || ''} — ${expediente?.clienteRazonSocial || ''}`"
    :icon="modalidadIcon"
    max-width="640px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body arribo-modal-body">
      <!-- 1. Tarjeta de Contexto Operativo del Despacho -->
      <div class="route-context-card">
        <div class="route-header">
          <span class="route-kicker">{{ routeKickerLabel }}</span>
          <span class="modalidad-tag">
            <i :class="modalidadIcon"></i>
            <span>{{ expediente?.modalidad || 'TRANSPORTE INTERNACIONAL' }}</span>
          </span>
        </div>

        <div class="route-track-display">
          <div class="port-point origin">
            <span class="port-role">{{ originRoleLabel }}</span>
            <span class="port-name">{{ getPuertoOrigen }}</span>
            <span class="port-country" v-if="expediente?.puertoEnvioPais">{{ expediente.puertoEnvioPais }}</span>
          </div>

          <div class="route-path-line">
            <div class="connector-track"></div>
            <div class="route-badge">
              <i class="pi pi-check-circle"></i>
              <span>{{ arriboStatusLabel }}</span>
            </div>
          </div>

          <div class="port-point destination">
            <span class="port-role">{{ destinationRoleLabel }}</span>
            <span class="port-name">{{ getPuertoDestino }}</span>
            <span class="port-country">Perú</span>
          </div>
        </div>

        <div class="logistics-pills-row" v-if="expediente?.naveViaje || expediente?.numeroContenedor || expediente?.precintoAduanero">
          <span class="pill-item" v-if="expediente?.naveViaje">
            <i :class="modalidadIcon"></i>
            <span>{{ isAereo ? 'Vuelo' : 'Nave' }}: <strong>{{ expediente.naveViaje }}</strong></span>
          </span>
          <span class="pill-item" v-if="expediente?.numeroContenedor">
            <i class="pi pi-box"></i>
            <span>{{ isAereo ? 'Pallet/AWB' : 'Contenedor' }}: <strong class="font-mono">{{ expediente.numeroContenedor }}</strong></span>
          </span>
          <span class="pill-item" v-if="expediente?.precintoAduanero">
            <i class="pi pi-shield"></i>
            <span>Precinto: <strong class="font-mono">{{ expediente.precintoAduanero }}</strong></span>
          </span>
        </div>
      </div>

      <!-- 2. Campos de Registro de Arribo -->
      <div class="form-section">
        <div class="form-section-header">
          <i class="pi pi-building"></i>
          <span>{{ sectionTitleLabel }}</span>
        </div>

        <div class="form-grid single-col">
          <div class="field-item">
            <label for="terminal-destino">{{ labelTerminal }} <span class="req">*</span></label>
            <input
              id="terminal-destino"
              v-model.trim="form.terminalAlmacenDestino"
              type="text"
              class="text-input"
              required
              list="terminales-callao"
              :placeholder="placeholderTerminal"
            />
            <datalist id="terminales-callao">
              <template v-if="isAereo">
                <option value="Talma Servicios Aeroportuarios (Aeropuerto Jorge Chávez)" />
                <option value="Shohin Depósito Temporal Aéreo" />
                <option value="Swissport Cargo Perú" />
                <option value="Servicios Aeroportuarios Andinos (SAASA)" />
              </template>
              <template v-else-if="isTerrestre">
                <option value="Ransa Depósito Aduanero Terrestre" />
                <option value="Terminal Terrestre Tacna / Puno" />
                <option value="Neptunia Almacén Temporal" />
              </template>
              <template v-else>
                <option value="DP World Callao (Muelle Sur)" />
                <option value="APM Terminals Callao (Muelle Norte)" />
                <option value="Imupesa Depósito Temporal" />
                <option value="Ransa Depósito Aduanero" />
                <option value="Neptunia Almacén Temporal" />
              </template>
            </datalist>
          </div>

          <div class="field-item">
            <label for="fecha-arribo">{{ labelFechaArribo }} <span class="req">*</span></label>
            <input
              id="fecha-arribo"
              v-model="form.fechaArriboAduana"
              type="datetime-local"
              class="text-input font-mono"
              required
            />
          </div>

          <div class="field-item">
            <label for="arribo-obs">Observaciones de Descarga & Manifiesto</label>
            <textarea
              id="arribo-obs"
              v-model.trim="form.observaciones"
              rows="2"
              :placeholder="placeholderObservaciones"
              class="textarea-input"
            ></textarea>
          </div>
        </div>
      </div>

      <!-- Aviso Explicativo del Hito SUNAT -->
      <div class="hito-notice-box">
        <i class="pi pi-info-circle"></i>
        <span>
          Al confirmar la llegada al país, el expediente quedará habilitado para que el
          <strong>Supervisor Aduanero</strong> registre el <strong>Canal SUNAT</strong> (Verde, Naranja o Rojo).
        </span>
      </div>

      <!-- Footer de Acciones -->
      <div class="modal-actions-footer">
        <button type="button" class="btn-modal-cancel" @click="handleClose" :disabled="loading">
          Cancelar
        </button>
        <button type="submit" class="btn-modal-submit" :disabled="loading">
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-check"></i>
          <span>Confirmar Llegada a Destino</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { expedienteService } from '../services/expedienteService'

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

const isAereo = computed(() => {
  const m = (props.expediente?.modalidad || '').toUpperCase()
  return m.includes('AER')
})

const isTerrestre = computed(() => {
  const m = (props.expediente?.modalidad || '').toUpperCase()
  return m.includes('TERR')
})

const modalidadIcon = computed(() => {
  if (isAereo.value) return 'pi pi-send'
  if (isTerrestre.value) return 'pi pi-truck'
  return 'pi pi-compass'
})

const routeKickerLabel = computed(() => {
  if (isAereo.value) return 'HITO OPERATIVO // ATERRIZAJE & ARRIBO NACIONAL'
  if (isTerrestre.value) return 'HITO OPERATIVO // LLEGADA A TERMINAL NACIONAL'
  return 'HITO OPERATIVO // ATRAQUE & LLEGADA A PUERTO NACIONAL'
})

const originRoleLabel = computed(() => {
  if (isAereo.value) return 'AEROPUERTO ORIGEN'
  if (isTerrestre.value) return 'TERMINAL ORIGEN'
  return 'PUERTO ORIGEN'
})

const destinationRoleLabel = computed(() => {
  if (isAereo.value) return 'AEROPUERTO DESTINO'
  if (isTerrestre.value) return 'TERMINAL DESTINO'
  return 'PUERTO DESTINO'
})

const arriboStatusLabel = computed(() => {
  if (isAereo.value) return 'Aterrizaje Conforme'
  if (isTerrestre.value) return 'Llegada Conforme'
  return 'Arribo Conforme'
})

const sectionTitleLabel = computed(() => {
  if (isAereo.value) return 'Datos de Aterrizaje & Terminal Aéreo de Descarga'
  if (isTerrestre.value) return 'Datos de Llegada & Almacén de Descarga'
  return 'Datos de Atraque & Terminal Portuario de Descarga'
})

const labelTerminal = computed(() => {
  if (isAereo.value) return 'Terminal Aeroportuario / Depósito Temporal de Llegada'
  if (isTerrestre.value) return 'Terminal Terrestre / Depósito Temporal'
  return 'Terminal Portuario / Depósito Temporal de Llegada'
})

const placeholderTerminal = computed(() => {
  if (isAereo.value) return 'Ej. Talma Aeropuerto / Shohin / SAASA'
  if (isTerrestre.value) return 'Ej. Ransa Depósito / Terminal Terrestre'
  return 'Ej. DP World Callao / APM Terminals Callao'
})

const labelFechaArribo = computed(() => {
  if (isAereo.value) return 'Fecha y Hora Real de Aterrizaje en Destino'
  return 'Fecha y Hora Real de Arribo a Puerto / Destino'
})

const placeholderObservaciones = computed(() => {
  if (isAereo.value) return 'Vuelo aterrizado, bultos y guías AWB verificados, sin novedades en pista...'
  return 'Nave atracada en muelle, precintos verificados, manifiesto aduanero conforme sin faltantes...'
})

const getPuertoOrigen = computed(() => {
  const exp = props.expediente
  if (!exp) return 'Punto de Origen'
  return exp.puertoEnvioNombre || (exp.puertoEnvioPais ? `Puerto (${exp.puertoEnvioPais})` : 'Punto de Origen')
})

const getPuertoDestino = computed(() => {
  const exp = props.expediente
  if (!exp) return 'Destino Nacional'
  return exp.puertoDestinoNombre || 'Callao / Lima (Perú)'
})

const getLocalDateTimeString = (date = new Date()) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const form = ref({
  terminalAlmacenDestino: '',
  fechaArriboAduana: '',
  observaciones: ''
})

watch(
  () => props.visible,
  (val) => {
    if (val) {
      errorMessage.value = ''
      const defaultTerminal = isAereo.value
        ? 'Talma Servicios Aeroportuarios (Aeropuerto Jorge Chávez)'
        : 'DP World Callao - Muelle Sur'

      form.value = {
        terminalAlmacenDestino: props.expediente?.terminalAlmacenDestino || defaultTerminal,
        fechaArriboAduana: props.expediente?.fechaArriboAduana
          ? props.expediente.fechaArriboAduana.slice(0, 16)
          : getLocalDateTimeString(),
        observaciones: ''
      }
    }
  }
)

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  if (!props.expediente?.idExpediente) return

  loading.value = true
  errorMessage.value = ''

  try {
    const updated = await expedienteService.confirmarArriboDestino(props.expediente.idExpediente, {
      fechaArriboAduana: form.value.fechaArriboAduana,
      terminalAlmacenDestino: form.value.terminalAlmacenDestino,
      observaciones: form.value.observaciones
    })
    emit('saved', updated)
    handleClose()
  } catch (err) {
    console.error('Error al confirmar arribo a destino:', err)
    errorMessage.value = err.response?.data?.message || 'Error al registrar el arribo de la carga a destino.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* Contenedor del formulario con espaciado equilibrado */
.arribo-modal-body {
  padding: 1.25rem 1.5rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  color: var(--color-text-main);
  box-sizing: border-box;
}

/* 1. Tarjeta de Contexto Operativo */
.route-context-card {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.route-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.route-kicker {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  text-transform: uppercase;
}

.modalidad-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  color: var(--color-primary);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

.route-track-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  padding: 0.25rem 0;
}

.port-point {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  min-width: 0;
  max-width: 42%;
}

.port-point.destination {
  align-items: flex-end;
  text-align: right;
}

.port-role {
  font-size: 0.625rem;
  font-weight: 800;
  color: var(--color-text-subtle);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.port-name {
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.port-country {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.route-path-line {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 90px;
}

.connector-track {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-border);
  transform: translateY(-50%);
  z-index: 1;
}

.route-badge {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.15rem 0.45rem;
  border-radius: 9999px;
  font-size: 0.625rem;
  font-weight: 700;
  color: var(--color-success-text, #059669);
}

.route-badge i {
  font-size: 0.65rem;
}

.logistics-pills-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.7rem;
  color: var(--color-text-muted);
  border-top: 1px dashed var(--color-border);
  padding-top: 0.45rem;
  flex-wrap: wrap;
}

.pill-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.pill-item strong {
  color: var(--color-text-main);
}

/* 2. Sección del Formulario */
.form-section {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.form-section-header {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding-bottom: 0.3rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.form-section-header i {
  color: var(--color-accent-kicker);
  font-size: 0.8rem;
}

.form-grid.single-col {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.field-item label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.req {
  color: var(--color-danger-text);
  font-weight: bold;
}

.text-input,
.textarea-input {
  width: 100%;
  padding: 0.5rem 0.7rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  box-sizing: border-box;
  transition: border-color var(--transition-fast, 0.15s ease), box-shadow var(--transition-fast, 0.15s ease);
}

.text-input:focus,
.textarea-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(180, 83, 9, 0.15);
}

.font-mono {
  font-family: var(--font-mono);
}

.textarea-input {
  resize: vertical;
  min-height: 52px;
}

/* Aviso Hito SUNAT */
.hito-notice-box {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.55rem 0.75rem;
  border-radius: var(--radius-sm, 6px);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.hito-notice-box i {
  color: var(--color-accent-kicker);
  font-size: 0.95rem;
  flex-shrink: 0;
}

/* Acciones Footer */
.modal-actions-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}

.btn-modal-cancel {
  padding: 0.5rem 1rem;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-modal-cancel:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  border-color: var(--color-border-strong);
}

.btn-modal-submit {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.5rem 1.25rem;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-primary-text);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-modal-submit:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-modal-submit:disabled,
.btn-modal-cancel:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 600px) {
  .arribo-modal-body {
    padding: 1rem;
  }
}
</style>
