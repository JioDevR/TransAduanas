<template>
  <BaseModal
    :visible="visible"
    title="Registrar Salida de Origen"
    :subtitle="`${expediente?.codigoExpediente || ''} — ${expediente?.clienteRazonSocial || ''}`"
    icon="pi pi-send"
    max-width="640px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body salida-modal-body">
      <!-- 1. Tarjeta de Contexto Operativo y Ruta -->
      <div class="route-context-card">
        <div class="route-header">
          <span class="route-kicker">RUTA DE EMBARQUE INTERNACIONAL</span>
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

        <div class="route-metadata-row" v-if="expediente?.mercanciaNombre || expediente?.tipoCargaNombre">
          <span class="meta-item" v-if="expediente?.mercanciaNombre">
            <i class="pi pi-box"></i>
            <span>Mercancía: <strong>{{ expediente.mercanciaNombre }}</strong></span>
          </span>
          <span class="meta-item" v-if="expediente?.tipoCargaNombre">
            <i class="pi pi-tag"></i>
            <span>Tipo: <strong>{{ expediente.tipoCargaNombre }}</strong></span>
          </span>
        </div>
      </div>

      <!-- 2. Bloque: Transporte & Unidad de Carga -->
      <div class="form-section">
        <div class="form-section-header">
          <i class="pi pi-compass"></i>
          <span>1. Transporte & Unidad de Carga</span>
        </div>

        <div class="form-grid">
          <div class="field-item">
            <label for="linea-transporte">{{ labelTransportista }} <span class="req">*</span></label>
            <input
              id="linea-transporte"
              v-model.trim="form.lineaTransporte"
              type="text"
              list="lineas-comunes"
              :placeholder="placeholderTransportista"
              class="text-input uppercase"
              required
              maxlength="50"
            />
            <datalist id="lineas-comunes">
              <template v-if="isAereo">
                <option value="LATAM CARGO" />
                <option value="ATLAS AIR" />
                <option value="KLM CARGO" />
                <option value="AVIANCA CARGO" />
                <option value="LUFTHANSA CARGO" />
              </template>
              <template v-else-if="isTerrestre">
                <option value="RANSA LOGÍSTICA" />
                <option value="CRUZ DEL SUR CARGO" />
                <option value="TRANSPORTES HNOS. MEZA" />
              </template>
              <template v-else>
                <option value="MSC (Mediterranean Shipping Company)" />
                <option value="MAERSK LINE" />
                <option value="CMA CGM" />
                <option value="HAPAG-LLOYD" />
                <option value="EVERGREEN MARINE" />
                <option value="ONE (Ocean Network Express)" />
                <option value="COSCO SHIPPING" />
              </template>
            </datalist>
          </div>

          <div class="field-item">
            <label for="nave-viaje">{{ labelNaveViaje }} <span class="req">*</span></label>
            <input
              id="nave-viaje"
              v-model.trim="form.nombreNaveViaje"
              type="text"
              :placeholder="placeholderNaveViaje"
              class="text-input uppercase font-mono"
              required
              maxlength="70"
            />
          </div>

          <div class="field-item">
            <label for="num-contenedor">{{ labelUnidadCarga }} <span class="req">*</span></label>
            <input
              id="num-contenedor"
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
            <label for="precinto-origen">{{ labelPrecinto }} <span class="req">*</span></label>
            <input
              id="precinto-origen"
              v-model="form.precintoAduanero"
              type="text"
              :placeholder="placeholderPrecinto"
              class="text-input uppercase font-mono"
              required
              maxlength="50"
              @input="form.precintoAduanero = form.precintoAduanero.toUpperCase()"
            />
          </div>
        </div>
      </div>

      <!-- 3. Bloque: Fechas de Salida & Arribo Estimado -->
      <div class="form-section">
        <div class="form-section-header">
          <i class="pi pi-calendar"></i>
          <span>2. Cronograma de Salida & Arribo</span>
        </div>

        <div class="form-grid">
          <div class="field-item">
            <label for="fecha-zarpe">{{ labelFechaZarpe }} <span class="req">*</span></label>
            <input
              id="fecha-zarpe"
              v-model="form.fechaZarpeEtd"
              type="date"
              class="text-input"
              required
            />
            <div class="date-human-hint" v-if="form.fechaZarpeEtd">
              <i class="pi pi-calendar-clock"></i>
              <span>{{ formatHumanDate(form.fechaZarpeEtd) }}</span>
            </div>
          </div>

          <div class="field-item">
            <div class="field-label-row">
              <label for="fecha-eta">Fecha Estimada de Llegada (ETA)</label>
              <div class="eta-presets" v-if="form.fechaZarpeEtd">
                <span class="presets-lbl">Sugerencias:</span>
                <button type="button" class="btn-eta-chip" @click="aplicarPresetEta(0)" title="Arribo el mismo día">Mismo día</button>
                <button type="button" class="btn-eta-chip" @click="aplicarPresetEta(1)" title="Llegada al día siguiente">+1d</button>
                <button type="button" class="btn-eta-chip" @click="aplicarPresetEta(3)" title="+3 días">+3d</button>
                <button type="button" class="btn-eta-chip" @click="aplicarPresetEta(7)" title="+7 días">+7d</button>
                <button type="button" class="btn-eta-chip" @click="aplicarPresetEta(15)" title="+15 días">+15d</button>
                <button type="button" class="btn-eta-chip" @click="aplicarPresetEta(30)" title="+30 días">+30d</button>
              </div>
            </div>
            <input
              id="fecha-eta"
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

          <!-- Indicador dinámico de tiempo de tránsito -->
          <div class="field-item full-width" v-if="tiempoTransitoInfo">
            <div :class="['lead-time-pill', tiempoTransitoInfo.esValido ? 'valid' : 'invalid']">
              <div class="lead-time-content">
                <i :class="tiempoTransitoInfo.esValido ? 'pi pi-clock' : 'pi pi-exclamation-triangle'"></i>
                <span>{{ tiempoTransitoInfo.texto }}</span>
              </div>
              <button
                v-if="!tiempoTransitoInfo.esValido && tiempoTransitoInfo.puedeInvertir"
                type="button"
                class="btn-swap-dates-action"
                @click="invertirMesDiaForm"
                title="Si tu navegador tomó DD como MM, haz clic aquí para corregirlo con un solo clic"
              >
                <i class="pi pi-arrows-h"></i>
                <span>Corregir formato (Invertir Día y Mes)</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 4. Bloque: Bitácora de Embarque -->
      <div class="form-section">
        <div class="form-section-header">
          <i class="pi pi-file-edit"></i>
          <span>3. Bitácora de Embarque (Opcional)</span>
        </div>

        <div class="field-item full-width">
          <textarea
            id="obs-salida"
            v-model.trim="form.observaciones"
            rows="2"
            placeholder="Notas de embarque, escalas programadas, transbordos o precintos adicionales..."
            class="textarea-input"
          ></textarea>
        </div>
      </div>

      <!-- Aviso explicativo del hito -->
      <div class="hito-notice-box">
        <i class="pi pi-info-circle"></i>
        <span>
          Al confirmar la salida, el expediente pasará a estado <strong>EN TRÁNSITO</strong>
          y el cliente podrá consultar el seguimiento en su portal B2B.
        </span>
      </div>

      <!-- Acciones del Modal -->
      <div class="modal-actions-footer">
        <button
          type="button"
          class="btn-modal-cancel"
          @click="handleClose"
          :disabled="loading"
        >
          Cancelar
        </button>
        <button
          type="submit"
          class="btn-modal-submit"
          :disabled="loading || (tiempoTransitoInfo && !tiempoTransitoInfo.esValido)"
        >
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-send"></i>
          <span>Confirmar Salida de Origen</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { expedienteService } from '../services/expedienteService'
import {
  parseLocalDate,
  getLocalDateString,
  formatHumanDate,
  calculateTransitDays,
  canSwapMonthDay,
  swapMonthDay,
  addDaysToDate
} from '@/shared/utils/dateUtils'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')

const form = ref({
  lineaTransporte: '',
  nombreNaveViaje: '',
  numeroContenedor: '',
  precintoAduanero: '',
  fechaZarpeEtd: '',
  fechaLlegadaEta: '',
  observaciones: ''
})

// Detección de Modalidad
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

// Textos adaptables según modalidad
const originRoleLabel = computed(() => {
  if (isAereo.value) return 'AEROPUERTO ORIGEN'
  if (isTerrestre.value) return 'TERMINAL ORIGEN'
  return 'PUERTO ORIGEN (POL)'
})

const destinationRoleLabel = computed(() => {
  if (isAereo.value) return 'AEROPUERTO DESTINO'
  if (isTerrestre.value) return 'TERMINAL DESTINO'
  return 'PUERTO DESTINO (POD)'
})

const travesiaLabel = computed(() => {
  if (isAereo.value) return 'Vuelo Internacional'
  if (isTerrestre.value) return 'Tránsito Terrestre'
  return 'Travesía Internacional'
})

const labelTransportista = computed(() => {
  if (isAereo.value) return 'Aerolínea / Transportista'
  if (isTerrestre.value) return 'Empresa de Transporte'
  return 'Línea Naviera / Transportista'
})

const placeholderTransportista = computed(() => {
  if (isAereo.value) return 'Ej. LATAM Cargo, Atlas Air, KLM...'
  if (isTerrestre.value) return 'Ej. Ransa, Cruz del Sur...'
  return 'Ej. MSC, Maersk, Hapag-Lloyd...'
})

const labelNaveViaje = computed(() => {
  if (isAereo.value) return 'N° de Vuelo / Guía AWB'
  if (isTerrestre.value) return 'Placa Tracto / N° de Guía'
  return 'Nave / Buque y N° de Viaje'
})

const placeholderNaveViaje = computed(() => {
  if (isAereo.value) return 'Ej. LA2401 - Vuelo Cargo'
  if (isTerrestre.value) return 'Ej. V1A-820 / Remolque R2B'
  return 'Ej. MSC INES - V. 402W'
})

const labelUnidadCarga = computed(() => {
  if (isAereo.value) return 'N° Pallet / Bulto / Contenedor Aéreo'
  if (isTerrestre.value) return 'N° Semirremolque / Furgón'
  return 'N° de Contenedor (ISO 6346)'
})

const placeholderUnidadCarga = computed(() => {
  if (isAereo.value) return 'Ej. PMC-48291-LA'
  if (isTerrestre.value) return 'Ej. FUR-90218'
  return 'Ej. MSKU-549281-0'
})

const labelPrecinto = computed(() => {
  if (isAereo.value) return 'Precinto de Seguridad / Sello'
  return 'Precinto Naviero / Sello de Origen'
})

const placeholderPrecinto = computed(() => {
  if (isAereo.value) return 'Ej. SL-892100'
  return 'Ej. ML-PE-892100'
})

const labelFechaZarpe = computed(() => {
  if (isAereo.value) return 'Fecha Real de Despegue / Salida (ETD)'
  if (isTerrestre.value) return 'Fecha de Salida de Terminal (ETD)'
  return 'Fecha Real de Salida / Zarpe (ETD)'
})

// Puertos formateados
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

// Cálculo dinámico del Lead Time y validación de cronograma
const tiempoTransitoInfo = computed(() => {
  const { fechaZarpeEtd, fechaLlegadaEta } = form.value
  if (!fechaZarpeEtd || !fechaLlegadaEta) return null

  const diffDays = calculateTransitDays(fechaZarpeEtd, fechaLlegadaEta)
  if (diffDays === null) return null

  if (diffDays < 0) {
    const invertible = canSwapMonthDay(fechaZarpeEtd) || canSwapMonthDay(fechaLlegadaEta)
    return {
      esValido: false,
      puedeInvertir: invertible,
      texto: 'La fecha estimada de arribo (ETA) debe ser igual o posterior a la fecha de salida.'
    }
  }

  if (diffDays === 0) {
    return {
      esValido: true,
      texto: isAereo.value
        ? 'Arribo el mismo día — Vuelo internacional directo / Courier Express.'
        : 'Arribo estimado el mismo día de la salida (tránsito local/directo).'
    }
  }

  if (diffDays === 1) {
    return {
      esValido: true,
      texto: 'Tiempo estimado: 1 día de trayecto internacional.'
    }
  }

  return {
    esValido: true,
    texto: `Tiempo estimado: ${diffDays} días de trayecto internacional.`
  }
})

const aplicarPresetEta = (dias) => {
  if (!form.value.fechaZarpeEtd) return
  form.value.fechaLlegadaEta = addDaysToDate(form.value.fechaZarpeEtd, dias)
}

const invertirMesDiaForm = () => {
  if (canSwapMonthDay(form.value.fechaZarpeEtd)) {
    form.value.fechaZarpeEtd = swapMonthDay(form.value.fechaZarpeEtd)
  }
  if (canSwapMonthDay(form.value.fechaLlegadaEta)) {
    form.value.fechaLlegadaEta = swapMonthDay(form.value.fechaLlegadaEta)
  }
}

// Cargar datos previos
watch(
  () => props.expediente,
  (exp) => {
    if (exp) {
      let linea = ''
      let nave = exp.naveViaje || ''
      if (nave.includes(' - ')) {
        const parts = nave.split(' - ')
        linea = parts[0]
        nave = parts.slice(1).join(' - ')
      }

      const todayStr = getLocalDateString(new Date())
      form.value = {
        lineaTransporte: linea,
        nombreNaveViaje: nave,
        numeroContenedor: (exp.numeroContenedor || '').toUpperCase(),
        precintoAduanero: (exp.precintoAduanero || '').toUpperCase(),
        fechaZarpeEtd: exp.fechaZarpeEtd || todayStr,
        fechaLlegadaEta: exp.fechaLlegadaEta || '',
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
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  if (!form.value.fechaZarpeEtd) {
    errorMessage.value = 'Debe indicar la fecha real de salida de origen.'
    loading.value = false
    return
  }

  const linea = form.value.lineaTransporte.trim()
  const nave = form.value.nombreNaveViaje.trim()
  const naveCompleta = linea && !nave.includes(linea) ? `${linea} - ${nave}` : (nave || linea)

  try {
    const updated = await expedienteService.confirmarSalidaOrigen(props.expediente.idExpediente, {
      fechaZarpeReal: form.value.fechaZarpeEtd,
      naveViaje: naveCompleta,
      numeroContenedor: form.value.numeroContenedor.trim(),
      precintoAduanero: form.value.precintoAduanero.trim(),
      fechaLlegadaEta: form.value.fechaLlegadaEta || null,
      observaciones: form.value.observaciones || null
    })

    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar la salida de origen del despacho.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* Contenedor principal del modal con padding armonioso */
.salida-modal-body {
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
  color: var(--color-accent-kicker);
}

.route-badge i {
  font-size: 0.625rem;
}

.route-metadata-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.7rem;
  color: var(--color-text-muted);
  border-top: 1px dashed var(--color-border);
  padding-top: 0.45rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.meta-item strong {
  color: var(--color-text-main);
}

/* 2. Secciones del Formulario (sin tarjetas anidadas pesadas) */
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

/* Grilla de dos columnas uniforme */
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

/* Indicador de Lead Time */
.lead-time-pill {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.45rem 0.75rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.75rem;
  font-weight: 700;
  width: 100%;
  box-sizing: border-box;
}

.lead-time-content {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.lead-time-pill.valid {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.lead-time-pill.invalid {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.btn-swap-dates-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.55rem;
  border-radius: var(--radius-sm, 4px);
  background: var(--color-bg-surface);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
  font-size: 0.6875rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-swap-dates-action:hover {
  background: var(--color-danger-text);
  color: #ffffff;
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

.field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.25rem;
}

.eta-presets {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.presets-lbl {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  font-weight: 500;
  margin-right: 0.15rem;
}

.btn-eta-chip {
  padding: 0.15rem 0.45rem;
  font-size: 0.6875rem;
  font-weight: 600;
  border-radius: 9999px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-eta-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-bg-surface);
}

/* Aviso de Hito */
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
  .salida-modal-body {
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
