<template>
  <BaseModal
    :visible="visible"
    title="Actualizar Datos de Tránsito Internacional"
    :subtitle="`${expediente?.codigoExpediente || ''} — ${expediente?.clienteRazonSocial || ''}`"
    :icon="modalidadIcon"
    max-width="640px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body transito-modal-body">
      <!-- 1. Ficha de Ruta y Contexto Operativo -->
      <div class="route-context-card">
        <div class="route-header">
          <span class="route-kicker">SEGUIMIENTO DE TRAVESÍA // CONTROL DE EMBARQUE</span>
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
              <i :class="modalidadIcon"></i>
              <span>{{ travesiaLabel }}</span>
            </div>
          </div>

          <div class="port-point destination">
            <span class="port-role">{{ destinationRoleLabel }}</span>
            <span class="port-name">{{ getPuertoDestino }}</span>
            <span class="port-country">Perú</span>
          </div>
        </div>
      </div>

      <!-- 2. Formulario de Actualización de Travesía -->
      <div class="form-section">
        <div class="form-section-header">
          <i class="pi pi-sliders-h"></i>
          <span>Datos del Medio de Transporte & Unidad de Carga</span>
        </div>

        <div class="form-grid">
          <div class="field-item">
            <label for="trans-nave">{{ labelNaveViaje }} <span class="req">*</span></label>
            <input
              id="trans-nave"
              v-model.trim="form.naveViaje"
              type="text"
              :placeholder="placeholderNaveViaje"
              class="text-input uppercase font-mono"
              required
              maxlength="100"
            />
          </div>

          <div class="field-item">
            <label for="trans-cont">{{ labelUnidadCarga }} <span class="req">*</span></label>
            <input
              id="trans-cont"
              v-model="form.numeroContenedor"
              type="text"
              :placeholder="placeholderUnidadCarga"
              class="text-input uppercase font-mono"
              required
              maxlength="30"
              @input="form.numeroContenedor = form.numeroContenedor.toUpperCase()"
            />
          </div>

          <div class="field-item">
            <label for="trans-precinto">{{ labelPrecinto }} <span class="req">*</span></label>
            <input
              id="trans-precinto"
              v-model="form.precintoAduanero"
              type="text"
              :placeholder="placeholderPrecinto"
              class="text-input uppercase font-mono"
              required
              maxlength="50"
              @input="form.precintoAduanero = form.precintoAduanero.toUpperCase()"
            />
          </div>

          <div class="field-item">
            <label for="trans-zarpe">{{ labelFechaZarpe }}</label>
            <input
              id="trans-zarpe"
              v-model="form.fechaZarpeEtd"
              type="date"
              class="text-input"
            />
            <div class="date-human-hint" v-if="form.fechaZarpeEtd">
              <i class="pi pi-calendar-clock"></i>
              <span>{{ formatHumanDate(form.fechaZarpeEtd) }}</span>
            </div>
          </div>

          <div class="field-item full-width">
            <label for="trans-eta">Fecha Estimada de Llegada (ETA)</label>
            <input
              id="trans-eta"
              v-model="form.fechaLlegadaEta"
              type="date"
              :min="form.fechaZarpeEtd || undefined"
              class="text-input"
            />
            <div class="date-human-hint" v-if="form.fechaLlegadaEta">
              <i class="pi pi-calendar-clock"></i>
              <span>{{ formatHumanDate(form.fechaLlegadaEta) }}</span>
            </div>
          </div>

          <div class="field-item full-width">
            <label for="trans-obs">Bitácora & Observaciones de Navegación</label>
            <textarea
              id="trans-obs"
              v-model.trim="form.observaciones"
              rows="2"
              placeholder="Novedades en ruta, escalas intermedias, aviso de recalada o reprogramación..."
              class="textarea-input"
            ></textarea>
          </div>
        </div>
      </div>

      <div class="modal-actions-footer">
        <button type="button" class="btn-modal-cancel" @click="handleClose" :disabled="loading">
          Cancelar
        </button>
        <button type="submit" class="btn-modal-submit" :disabled="loading">
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-save"></i>
          <span>Guardar Cambios</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { expedienteService } from '../services/expedienteService'
import { formatHumanDate } from '@/shared/utils/dateUtils'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
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

const travesiaLabel = computed(() => {
  if (isAereo.value) return 'En Vuelo'
  if (isTerrestre.value) return 'En Ruta'
  return 'En Navegación'
})

const labelNaveViaje = computed(() => {
  if (isAereo.value) return 'N° de Vuelo / Guía AWB'
  if (isTerrestre.value) return 'Placa Tracto / Remolque'
  return 'Nave / Buque y N° de Viaje'
})

const placeholderNaveViaje = computed(() => {
  if (isAereo.value) return 'Ej. LA2401 - Vuelo Cargo'
  if (isTerrestre.value) return 'Ej. V1A-820 / Remolque R2B'
  return 'Ej. MSC INES - V. 402W'
})

const labelUnidadCarga = computed(() => {
  if (isAereo.value) return 'N° Pallet / Bulto / Guía'
  if (isTerrestre.value) return 'N° Semirremolque / Furgón'
  return 'N° de Contenedor'
})

const placeholderUnidadCarga = computed(() => {
  if (isAereo.value) return 'Ej. PMC-48291-LA'
  if (isTerrestre.value) return 'Ej. FUR-90218'
  return 'Ej. MEDU-123456-7'
})

const labelPrecinto = computed(() => {
  if (isAereo.value) return 'Precinto de Seguridad / Sello'
  return 'Precinto / Sello de Seguridad'
})

const placeholderPrecinto = computed(() => {
  if (isAereo.value) return 'Ej. SL-998877'
  return 'Ej. PE-ML-998877'
})

const labelFechaZarpe = computed(() => {
  if (isAereo.value) return 'Fecha de Despegue / Salida (ETD)'
  if (isTerrestre.value) return 'Fecha de Salida (ETD)'
  return 'Fecha de Zarpe / Salida (ETD)'
})

const getPuertoOrigen = computed(() => {
  const exp = props.expediente
  if (!exp) return 'Punto de Origen'
  return exp.puertoEnvioNombre || (exp.puertoEnvioPais ? `Puerto (${exp.puertoEnvioPais})` : 'Punto de Origen')
})

const getPuertoDestino = computed(() => {
  const exp = props.expediente
  if (!exp) return 'Puerto del Callao'
  return exp.puertoDestinoNombre || 'Puerto del Callao (Perú)'
})

const form = ref({
  naveViaje: '',
  numeroContenedor: '',
  precintoAduanero: '',
  fechaZarpeEtd: '',
  fechaLlegadaEta: '',
  observaciones: ''
})

watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      form.value = {
        naveViaje: exp.naveViaje || '',
        numeroContenedor: (exp.numeroContenedor || '').toUpperCase(),
        precintoAduanero: (exp.precintoAduanero || '').toUpperCase(),
        fechaZarpeEtd: exp.fechaZarpeEtd || '',
        fechaLlegadaEta: exp.fechaLlegadaEta || '',
        observaciones: exp.observaciones || ''
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
      naveViaje: form.value.naveViaje,
      numeroContenedor: form.value.numeroContenedor.trim(),
      precintoAduanero: form.value.precintoAduanero.trim(),
      fechaZarpeEtd: form.value.fechaZarpeEtd || null,
      fechaLlegadaEta: form.value.fechaLlegadaEta || null,
      observaciones: form.value.observaciones || null
    }
    const updated = await expedienteService.updateExpediente(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al actualizar datos de travesía.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* Contenedor del formulario con espaciado equilibrado */
.transito-modal-body {
  padding: 1.25rem 1.5rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  color: var(--color-text-main);
  box-sizing: border-box;
}

/* 1. Ficha de Ruta */
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
  color: var(--color-accent-kicker);
}

.route-badge i {
  font-size: 0.625rem;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.65rem 0.85rem;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.field-item.full-width {
  grid-column: span 2;
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
  background-color: var(--color-bg-surface);
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

.text-input.uppercase {
  text-transform: uppercase;
}

.font-mono {
  font-family: var(--font-mono);
}

.textarea-input {
  resize: vertical;
  min-height: 52px;
}

.modal-actions-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.25rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}

.btn-modal-cancel {
  padding: 0.5rem 1rem;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-muted);
  font-weight: 600;
  font-size: 0.8125rem;
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
  font-weight: 700;
  font-size: 0.8125rem;
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

.date-human-hint {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin-top: 0.35rem;
  font-size: 0.75rem;
  color: var(--color-accent-kicker, #b8860b);
  font-weight: 600;
}

@media (max-width: 600px) {
  .transito-modal-body {
    padding: 1rem;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .field-item.full-width {
    grid-column: span 1;
  }
}
</style>
