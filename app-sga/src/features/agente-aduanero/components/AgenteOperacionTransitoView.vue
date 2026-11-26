<template>
  <div class="operacion-transito-view fade-in">
    <!-- Cabecera contextual (sgaUI: sin título duplicado) -->
    <header class="sga-form-header">
      <div class="transito-header-left">
        <Button
          type="button"
          label="Volver"
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          size="small"
          @click="$emit('back')"
        />

        <span v-if="expediente" class="transito-code font-mono">
          {{ expediente.codigoExpediente }}
        </span>

        <Tag
          v-if="expediente"
          :value="formatEstadoAgente(expediente.estadoExpediente)"
          :severity="getExpedienteSeverity(expediente.estadoExpediente)"
          rounded
        />
      </div>

      <div class="sga-form-actions">
        <SelectButton
          v-model="activeMode"
          :options="availableModes"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
          size="small"
        >
          <template #option="{ option }">
            <i :class="option.icon"></i>
            <span>{{ option.label }}</span>
          </template>
        </SelectButton>
      </div>
    </header>

    <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
      <i class="pi pi-exclamation-triangle"></i>
      <span>{{ errorMessage }}</span>
    </div>

    <!-- Workspace Split-Screen (sgaUI) -->
    <div v-if="expediente" class="sga-split-layout">
      <div class="sga-split-main">
        <!-- MODO 1: SALIDA DE ORIGEN (Importación) -->
        <form
          v-if="activeMode === 'salida' && !isExport"
          class="sga-card-surface transito-form-card"
          @submit.prevent="handleSalidaSubmit"
        >
          <div class="form-card-head">
            <div>
              <span class="sga-kicker">HITO OPERATIVO 01 / 03</span>
              <h3 class="sga-chart-title">Confirmar Salida de Origen &amp; Embarque</h3>
            </div>
          </div>

          <div class="sga-grid-2">
            <div class="sga-form-field">
              <label class="sga-form-label" for="salida-linea">{{ labelTransportista }} *</label>
              <InputText
                id="salida-linea"
                v-model.trim="salidaForm.lineaTransporte"
                :placeholder="placeholderTransportista"
                class="uppercase"
                required
                maxlength="60"
                size="small"
                fluid
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="salida-nave">{{ labelNaveViaje }} *</label>
              <InputText
                id="salida-nave"
                v-model.trim="salidaForm.nombreNaveViaje"
                :placeholder="placeholderNaveViaje"
                class="uppercase font-mono"
                required
                maxlength="70"
                size="small"
                fluid
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="salida-cont">{{ labelUnidadCarga }} *</label>
              <InputText
                id="salida-cont"
                :modelValue="salidaForm.numeroContenedor"
                :placeholder="placeholderUnidadCarga"
                class="uppercase font-mono"
                required
                maxlength="30"
                size="small"
                fluid
                @update:modelValue="salidaForm.numeroContenedor = ($event || '').toUpperCase()"
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="salida-precinto">{{ labelPrecinto }} *</label>
              <InputText
                id="salida-precinto"
                :modelValue="salidaForm.precintoAduanero"
                :placeholder="placeholderPrecinto"
                class="uppercase font-mono"
                required
                maxlength="50"
                size="small"
                fluid
                @update:modelValue="salidaForm.precintoAduanero = ($event || '').toUpperCase()"
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="salida-zarpe">{{ labelFechaZarpe }} *</label>
              <InputText
                id="salida-zarpe"
                v-model="salidaForm.fechaZarpeEtd"
                type="date"
                required
                size="small"
                fluid
              />
              <span v-if="salidaForm.fechaZarpeEtd" class="sga-cell-sub">
                {{ formatHumanDate(salidaForm.fechaZarpeEtd) }}
              </span>
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="salida-eta">Fecha Estimada Llegada (ETA)</label>
              <InputText
                id="salida-eta"
                v-model="salidaForm.fechaLlegadaEta"
                type="date"
                :min="salidaForm.fechaZarpeEtd || undefined"
                size="small"
                fluid
              />
              <div v-if="salidaForm.fechaZarpeEtd" class="eta-presets-row">
                <Button
                  v-for="d in [0, 1, 3, 7, 15, 30]"
                  :key="d"
                  type="button"
                  :label="d === 0 ? 'Hoy' : `+${d}d`"
                  severity="secondary"
                  outlined
                  size="small"
                  @click="aplicarPresetSalidaEta(d)"
                />
              </div>
              <span v-if="salidaForm.fechaLlegadaEta" class="sga-cell-sub">
                {{ formatHumanDate(salidaForm.fechaLlegadaEta) }}
              </span>
            </div>
          </div>

          <div
            v-if="tiempoTransitoCalculado"
            :class="['sga-banner', tiempoTransitoCalculado.esValido ? 'is-info' : 'is-danger']"
          >
            <i :class="tiempoTransitoCalculado.esValido ? 'pi pi-clock' : 'pi pi-exclamation-triangle'"></i>
            <div class="transit-calc-row">
              <span>{{ tiempoTransitoCalculado.texto }}</span>
              <Button
                v-if="!tiempoTransitoCalculado.esValido && tiempoTransitoCalculado.puedeInvertir"
                type="button"
                label="Invertir Día/Mes"
                icon="pi pi-arrows-h"
                severity="secondary"
                size="small"
                @click="invertirMesDiaSalida"
              />
            </div>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="salida-obs">Bitácora de Embarque</label>
            <Textarea
              id="salida-obs"
              v-model.trim="salidaForm.observaciones"
              rows="3"
              autoResize
              placeholder="Escalas técnicas, notas de estiba o transbordos programados..."
              fluid
            />
          </div>

          <footer class="sga-dialog-footer">
            <Button
              type="button"
              label="Cancelar"
              severity="secondary"
              outlined
              size="small"
              :disabled="loading"
              @click="$emit('back')"
            />
            <Button
              type="submit"
              label="Confirmar Salida de Origen"
              icon="pi pi-send"
              size="small"
              :loading="loading"
              :disabled="Boolean(tiempoTransitoCalculado && !tiempoTransitoCalculado.esValido)"
            />
          </footer>
        </form>

        <!-- MODO 2: ACTUALIZAR DATOS DE TRÁNSITO -->
        <form
          v-else-if="activeMode === 'transito'"
          class="sga-card-surface transito-form-card"
          @submit.prevent="handleTransitoSubmit"
        >
          <div class="form-card-head">
            <div>
              <span class="sga-kicker">{{ isExport ? 'SEGUIMIENTO DE EMBARQUE' : 'HITO OPERATIVO 02 / 03' }}</span>
              <h3 class="sga-chart-title">Actualizar Seguimiento de Travesía</h3>
            </div>
          </div>

          <div class="sga-grid-2">
            <div class="sga-form-field">
              <label class="sga-form-label" for="tr-nave">{{ labelNaveViaje }} *</label>
              <InputText
                id="tr-nave"
                v-model.trim="transitoForm.naveViaje"
                :placeholder="placeholderNaveViaje"
                class="uppercase font-mono"
                required
                maxlength="100"
                size="small"
                fluid
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="tr-cont">{{ labelUnidadCarga }} *</label>
              <InputText
                id="tr-cont"
                :modelValue="transitoForm.numeroContenedor"
                :placeholder="placeholderUnidadCarga"
                class="uppercase font-mono"
                required
                maxlength="30"
                size="small"
                fluid
                @update:modelValue="transitoForm.numeroContenedor = ($event || '').toUpperCase()"
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="tr-precinto">{{ labelPrecinto }} *</label>
              <InputText
                id="tr-precinto"
                :modelValue="transitoForm.precintoAduanero"
                :placeholder="placeholderPrecinto"
                class="uppercase font-mono"
                required
                maxlength="50"
                size="small"
                fluid
                @update:modelValue="transitoForm.precintoAduanero = ($event || '').toUpperCase()"
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="tr-zarpe">{{ labelFechaZarpe }}</label>
              <InputText
                id="tr-zarpe"
                v-model="transitoForm.fechaZarpeEtd"
                type="date"
                size="small"
                fluid
              />
            </div>

            <div class="sga-form-field full-span">
              <label class="sga-form-label" for="tr-eta">Fecha Estimada Llegada (ETA)</label>
              <InputText
                id="tr-eta"
                v-model="transitoForm.fechaLlegadaEta"
                type="date"
                :min="transitoForm.fechaZarpeEtd || undefined"
                size="small"
                fluid
              />
            </div>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="tr-obs">Bitácora &amp; Observaciones</label>
            <Textarea
              id="tr-obs"
              v-model.trim="transitoForm.observaciones"
              rows="3"
              autoResize
              placeholder="Novedades en ruta, cambios de escala o avisos de recalada..."
              fluid
            />
          </div>

          <footer class="sga-dialog-footer">
            <Button
              type="button"
              label="Cancelar"
              severity="secondary"
              outlined
              size="small"
              :disabled="loading"
              @click="$emit('back')"
            />
            <Button
              type="submit"
              label="Guardar Cambios de Tránsito"
              icon="pi pi-save"
              size="small"
              :loading="loading"
            />
          </footer>
        </form>

        <!-- MODO 3: CONFIRMAR ARRIBO A DESTINO (Importación) -->
        <form
          v-else-if="activeMode === 'arribo' && !isExport"
          class="sga-card-surface transito-form-card"
          @submit.prevent="handleArriboSubmit"
        >
          <div class="form-card-head">
            <div>
              <span class="sga-kicker">HITO OPERATIVO 03 / 03</span>
              <h3 class="sga-chart-title">Confirmar Arribo a Destino &amp; Terminal</h3>
            </div>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="arribo-terminal">{{ labelTerminal }} *</label>
            <InputText
              id="arribo-terminal"
              v-model.trim="arriboForm.terminalAlmacenDestino"
              :placeholder="placeholderTerminal"
              required
              size="small"
              fluid
            />
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="arribo-fecha">{{ labelFechaArribo }} *</label>
            <InputText
              id="arribo-fecha"
              v-model="arriboForm.fechaArriboAduana"
              type="datetime-local"
              class="font-mono"
              required
              size="small"
              fluid
            />
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="arribo-obs">Observaciones de Descarga &amp; Manifiesto</label>
            <Textarea
              id="arribo-obs"
              v-model.trim="arriboForm.observaciones"
              rows="3"
              autoResize
              :placeholder="placeholderObservacionesArribo"
              fluid
            />
          </div>

          <div class="sga-banner is-info">
            <i class="pi pi-shield"></i>
            <div>
              <span class="sga-micro-label">ACTIVACIÓN DE CANAL SUNAT</span>
              <p class="banner-sub-text">
                Al confirmar la llegada al país, el Supervisor Aduanero quedará habilitado para registrar DAM y Canal SUNAT.
              </p>
            </div>
          </div>

          <footer class="sga-dialog-footer">
            <Button
              type="button"
              label="Cancelar"
              severity="secondary"
              outlined
              size="small"
              :disabled="loading"
              @click="$emit('back')"
            />
            <Button
              type="submit"
              label="Confirmar Llegada a Destino"
              icon="pi pi-check"
              size="small"
              :loading="loading"
            />
          </footer>
        </form>
      </div>

      <div class="sga-split-aside">
        <AgenteTransitoContextAside :expediente="expediente" :active-mode="activeMode" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import AgenteTransitoContextAside from './transito/AgenteTransitoContextAside.vue'
import { expedienteService } from '@/shared/services/expedienteService'
import { isExportacion, extractTerminalBitacora } from '../utils/agenteWorkflowUtils'
import { formatEstadoAgente, getExpedienteSeverity } from '../utils/agenteFormatters'
import {
  getLocalDateString,
  getNowDateTimeInput,
  formatHumanDate,
  calculateTransitDays,
  canSwapMonthDay,
  swapMonthDay,
  addDaysToDate,
  toLocalDateTimePayload
} from '@/shared/utils/dateUtils'

const props = defineProps({
  expediente: { type: Object, required: true },
  initialMode: { type: String, default: 'salida' }
})

const emit = defineEmits(['back', 'saved'])

const isExport = computed(() => isExportacion(props.expediente))
const activeMode = ref(isExport.value ? 'transito' : (props.initialMode || 'salida'))
const loading = ref(false)
const errorMessage = ref('')

const availableModes = computed(() => {
  if (isExport.value) {
    return [{ label: 'Datos de Tránsito', value: 'transito', icon: 'pi pi-globe' }]
  }
  return [
    { label: '1. Salida Origen', value: 'salida', icon: 'pi pi-send' },
    { label: '2. Tránsito', value: 'transito', icon: 'pi pi-globe' },
    { label: '3. Confirmar Arribo', value: 'arribo', icon: 'pi pi-compass' }
  ]
})

const isAereo = computed(() => (props.expediente?.modalidad || '').toUpperCase().includes('AER'))
const isTerrestre = computed(() => (props.expediente?.modalidad || '').toUpperCase().includes('TERR'))

const labelTransportista = computed(() => {
  if (isAereo.value) return 'Aerolínea / Transportista'
  if (isTerrestre.value) return 'Empresa de Transporte'
  return 'Línea Naviera / Transportista'
})

const placeholderTransportista = computed(() => {
  if (isAereo.value) return 'Ej. LATAM Cargo, Atlas Air, KLM Cargo...'
  if (isTerrestre.value) return 'Ej. Ransa, Cruz del Sur Cargo...'
  return 'Ej. MSC, Maersk, Hapag-Lloyd, CMA CGM...'
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
  if (isAereo.value) return 'N° Pallet / Contenedor Aéreo'
  if (isTerrestre.value) return 'N° Semirremolque / Furgón'
  return 'N° de Contenedor (ISO 6346)'
})

const placeholderUnidadCarga = computed(() => {
  if (isAereo.value) return 'Ej. PMC-48291-LA'
  if (isTerrestre.value) return 'Ej. FUR-90218'
  return 'Ej. MSKU-549281-0'
})

const labelPrecinto = computed(() =>
  isAereo.value ? 'Precinto de Seguridad' : 'Precinto Naviero / Sello'
)

const placeholderPrecinto = computed(() => (isAereo.value ? 'Ej. SL-892100' : 'Ej. ML-PE-892100'))

const labelFechaZarpe = computed(() => {
  if (isAereo.value) return 'Fecha Real Despegue (ETD)'
  if (isTerrestre.value) return 'Fecha Salida Terminal (ETD)'
  return 'Fecha Real Zarpe (ETD)'
})

const labelTerminal = computed(() => {
  if (isAereo.value) return 'Terminal Aeroportuario / Depósito Llegada'
  if (isTerrestre.value) return 'Terminal Terrestre / Depósito'
  return 'Terminal Portuario / Depósito Llegada'
})

const placeholderTerminal = computed(() => {
  if (isAereo.value) return 'Ej. Talma Aeropuerto / Shohin / SAASA'
  if (isTerrestre.value) return 'Ej. Ransa Depósito / Terminal Terrestre'
  return 'Ej. DP World Callao / APM Terminals Callao'
})

const labelFechaArribo = computed(() =>
  isAereo.value ? 'Fecha y Hora de Aterrizaje' : 'Fecha y Hora Real de Arribo'
)

const placeholderObservacionesArribo = computed(() =>
  isAereo.value
    ? 'Vuelo aterrizado, bultos y guías AWB verificados sin daños...'
    : 'Nave atracada en muelle, precintos verificados sin faltantes...'
)

const salidaForm = ref({
  lineaTransporte: '',
  nombreNaveViaje: '',
  numeroContenedor: '',
  precintoAduanero: '',
  fechaZarpeEtd: '',
  fechaLlegadaEta: '',
  observaciones: ''
})

const transitoForm = ref({
  naveViaje: '',
  numeroContenedor: '',
  precintoAduanero: '',
  fechaZarpeEtd: '',
  fechaLlegadaEta: '',
  observaciones: ''
})

const arriboForm = ref({
  terminalAlmacenDestino: '',
  fechaArriboAduana: '',
  observaciones: ''
})

const initializeForms = (exp) => {
  if (!exp) return
  errorMessage.value = ''

  let linea = ''
  let nave = exp.naveViaje || ''
  if (nave.includes(' - ')) {
    const parts = nave.split(' - ')
    linea = parts[0]
    nave = parts.slice(1).join(' - ')
  }

  salidaForm.value = {
    lineaTransporte: linea,
    nombreNaveViaje: nave,
    numeroContenedor: (exp.numeroContenedor || '').toUpperCase(),
    precintoAduanero: (exp.precintoAduanero || '').toUpperCase(),
    fechaZarpeEtd: exp.fechaZarpeEtd || getLocalDateString(new Date()),
    fechaLlegadaEta: exp.fechaLlegadaEta || '',
    observaciones: ''
  }

  transitoForm.value = {
    naveViaje: exp.naveViaje || '',
    numeroContenedor: (exp.numeroContenedor || '').toUpperCase(),
    precintoAduanero: (exp.precintoAduanero || '').toUpperCase(),
    fechaZarpeEtd: exp.fechaZarpeEtd || '',
    fechaLlegadaEta: exp.fechaLlegadaEta || '',
    observaciones: exp.observaciones || ''
  }

  const defaultTerminal = isAereo.value
    ? 'Talma Servicios Aeroportuarios (Aeropuerto Jorge Chávez)'
    : 'DP World Callao - Muelle Sur'

  arriboForm.value = {
    terminalAlmacenDestino: extractTerminalBitacora(exp.observaciones) || defaultTerminal,
    fechaArriboAduana: exp.fechaArriboAduana
      ? String(exp.fechaArriboAduana).slice(0, 16)
      : getNowDateTimeInput(),
    observaciones: ''
  }
}

watch(
  () => props.expediente,
  (newExp) => initializeForms(newExp),
  { immediate: true }
)

watch(
  () => props.initialMode,
  (mode) => {
    errorMessage.value = ''
    activeMode.value = isExport.value ? 'transito' : (mode || 'salida')
  }
)

watch(activeMode, () => {
  errorMessage.value = ''
})

const tiempoTransitoCalculado = computed(() => {
  const { fechaZarpeEtd, fechaLlegadaEta } = salidaForm.value
  if (!fechaZarpeEtd || !fechaLlegadaEta) return null

  const diffDays = calculateTransitDays(fechaZarpeEtd, fechaLlegadaEta)
  if (diffDays === null) return null

  if (diffDays < 0) {
    return {
      esValido: false,
      puedeInvertir: canSwapMonthDay(fechaZarpeEtd) || canSwapMonthDay(fechaLlegadaEta),
      texto: 'La fecha estimada de arribo (ETA) debe ser igual o posterior a la fecha de salida.'
    }
  }
  if (diffDays === 0) {
    return { esValido: true, texto: 'Arribo estimado el mismo día de la salida.' }
  }
  return {
    esValido: true,
    texto: `Trayecto estimado: ${diffDays} día${diffDays === 1 ? '' : 's'} de viaje internacional.`
  }
})

const aplicarPresetSalidaEta = (dias) => {
  if (!salidaForm.value.fechaZarpeEtd) return
  salidaForm.value.fechaLlegadaEta = addDaysToDate(salidaForm.value.fechaZarpeEtd, dias)
}

const invertirMesDiaSalida = () => {
  if (canSwapMonthDay(salidaForm.value.fechaZarpeEtd)) {
    salidaForm.value.fechaZarpeEtd = swapMonthDay(salidaForm.value.fechaZarpeEtd)
  }
  if (canSwapMonthDay(salidaForm.value.fechaLlegadaEta)) {
    salidaForm.value.fechaLlegadaEta = swapMonthDay(salidaForm.value.fechaLlegadaEta)
  }
}

const handleSalidaSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  const linea = salidaForm.value.lineaTransporte.trim()
  const nave = salidaForm.value.nombreNaveViaje.trim()
  const naveCompleta = linea && !nave.includes(linea) ? `${linea} - ${nave}` : (nave || linea)

  try {
    const updated = await expedienteService.confirmarSalidaOrigen(props.expediente.idExpediente, {
      fechaZarpeReal: salidaForm.value.fechaZarpeEtd,
      naveViaje: naveCompleta,
      numeroContenedor: salidaForm.value.numeroContenedor.trim(),
      precintoAduanero: salidaForm.value.precintoAduanero.trim(),
      fechaLlegadaEta: salidaForm.value.fechaLlegadaEta || null,
      observaciones: salidaForm.value.observaciones || null
    })
    emit('saved', updated)
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar la salida de origen.'
  } finally {
    loading.value = false
  }
}

const handleTransitoSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  try {
    const updated = await expedienteService.updateExpediente(props.expediente.idExpediente, {
      naveViaje: transitoForm.value.naveViaje,
      numeroContenedor: transitoForm.value.numeroContenedor.trim(),
      precintoAduanero: transitoForm.value.precintoAduanero.trim(),
      fechaZarpeEtd: transitoForm.value.fechaZarpeEtd || null,
      fechaLlegadaEta: transitoForm.value.fechaLlegadaEta || null,
      observaciones: transitoForm.value.observaciones || null
    })
    emit('saved', updated)
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al actualizar datos de travesía.'
  } finally {
    loading.value = false
  }
}

const handleArriboSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  try {
    const updated = await expedienteService.confirmarArriboDestino(props.expediente.idExpediente, {
      fechaArriboAduana: toLocalDateTimePayload(arriboForm.value.fechaArriboAduana),
      terminalAlmacenDestino: arriboForm.value.terminalAlmacenDestino,
      observaciones: arriboForm.value.observaciones || null
    })
    emit('saved', updated)
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar el arribo a destino.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.operacion-transito-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
}

.transito-header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.transito-code {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-primary);
}

.transito-form-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-card-head {
  padding-bottom: 0.65rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.full-span {
  grid-column: 1 / -1;
}

.uppercase {
  text-transform: uppercase;
}

.eta-presets-row {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
  margin-top: 0.25rem;
}

.transit-calc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  width: 100%;
}

.banner-sub-text {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
}
</style>
