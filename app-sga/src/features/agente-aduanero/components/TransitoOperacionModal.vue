<template>
  <Dialog
    :visible="visible"
    modal
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('720px'), maxWidth: '96vw' }"
    :header="dialogTitle"
    @update:visible="(val) => { if (!val && !loading) $emit('close') }"
  >
    <div v-if="expediente" class="sga-dialog-body">
      <div class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <Tag
            :value="formatEstadoAgente(expediente.estadoExpediente)"
            :severity="getExpedienteSeverity(expediente.estadoExpediente)"
            rounded
          />
          <Tag
            :value="isExport ? 'Exportación' : 'Importación'"
            :severity="isExport ? 'success' : 'info'"
            class="sga-mini-tag"
            rounded
          />
        </div>

        <SelectButton
          v-if="!isExport && availableModes.length > 1"
          v-model="activeMode"
          :options="availableModes"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
          size="small"
        />
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <!-- MODO 1: SALIDA DE ORIGEN (Importación) -->
      <form
        v-if="activeMode === 'salida' && !isExport"
        class="sga-dialog-body"
        @submit.prevent="handleSalidaSubmit"
      >
        <div class="sga-grid-2">
          <div class="sga-form-field">
            <label class="sga-form-label" for="m-salida-linea">{{ labelTransportista }} *</label>
            <InputText
              id="m-salida-linea"
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
            <label class="sga-form-label" for="m-salida-nave">{{ labelNaveViaje }} *</label>
            <InputText
              id="m-salida-nave"
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
            <label class="sga-form-label" for="m-salida-cont">{{ labelUnidadCarga }} *</label>
            <InputText
              id="m-salida-cont"
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
            <label class="sga-form-label" for="m-salida-precinto">{{ labelPrecinto }} *</label>
            <InputText
              id="m-salida-precinto"
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
            <label class="sga-form-label" for="m-salida-zarpe">{{ labelFechaZarpe }} *</label>
            <InputText
              id="m-salida-zarpe"
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
            <label class="sga-form-label" for="m-salida-eta">Fecha Estimada Llegada (ETA)</label>
            <InputText
              id="m-salida-eta"
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
          <label class="sga-form-label" for="m-salida-obs">Bitácora de Embarque</label>
          <Textarea
            id="m-salida-obs"
            v-model.trim="salidaForm.observaciones"
            rows="2"
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
            @click="$emit('close')"
          />
          <Button
            type="submit"
            label="Confirmar Envío de Origen Exitoso"
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
        class="sga-dialog-body"
        @submit.prevent="handleTransitoSubmit"
      >
        <div class="sga-grid-2">
          <div class="sga-form-field">
            <label class="sga-form-label" for="m-tr-nave">{{ labelNaveViaje }} *</label>
            <InputText
              id="m-tr-nave"
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
            <label class="sga-form-label" for="m-tr-cont">{{ labelUnidadCarga }} *</label>
            <InputText
              id="m-tr-cont"
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
            <label class="sga-form-label" for="m-tr-precinto">{{ labelPrecinto }} *</label>
            <InputText
              id="m-tr-precinto"
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
            <label class="sga-form-label" for="m-tr-zarpe">{{ labelFechaZarpe }}</label>
            <InputText
              id="m-tr-zarpe"
              v-model="transitoForm.fechaZarpeEtd"
              type="date"
              size="small"
              fluid
            />
          </div>

          <div class="sga-form-field full-span">
            <label class="sga-form-label" for="m-tr-eta">Fecha Estimada Llegada (ETA)</label>
            <InputText
              id="m-tr-eta"
              v-model="transitoForm.fechaLlegadaEta"
              type="date"
              :min="transitoForm.fechaZarpeEtd || undefined"
              size="small"
              fluid
            />
          </div>
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="m-tr-obs">Bitácora &amp; Observaciones</label>
          <Textarea
            id="m-tr-obs"
            v-model.trim="transitoForm.observaciones"
            rows="2"
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
            @click="$emit('close')"
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
        class="sga-dialog-body"
        @submit.prevent="handleArriboSubmit"
      >
        <div class="sga-form-field">
          <label class="sga-form-label" for="m-arribo-terminal">{{ labelTerminal }} *</label>
          <InputText
            id="m-arribo-terminal"
            v-model.trim="arriboForm.terminalAlmacenDestino"
            :placeholder="placeholderTerminal"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="m-arribo-fecha">{{ labelFechaArribo }} *</label>
          <InputText
            id="m-arribo-fecha"
            v-model="arriboForm.fechaArriboAduana"
            type="datetime-local"
            class="font-mono"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="m-arribo-obs">Observaciones de Descarga &amp; Manifiesto</label>
          <Textarea
            id="m-arribo-obs"
            v-model.trim="arriboForm.observaciones"
            rows="2"
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
              Al confirmar la llegada, Gestión Aduanera quedará habilitada para numerar DAM y asignar Canal SUNAT, y el Portal B2B mostrará "Arribado en Puerto".
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
            @click="$emit('close')"
          />
          <Button
            type="submit"
            label="Confirmar Llegada a Destino"
            icon="pi pi-check"
            severity="success"
            size="small"
            :loading="loading"
          />
        </footer>
      </form>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { expedienteService } from '@/shared/services/expedienteService'
import { useResponsive } from '@/shared/composables/useResponsive'
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
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null },
  initialMode: { type: String, default: 'salida' }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()

const isExport = computed(() => isExportacion(props.expediente))
const activeMode = ref('salida')
const loading = ref(false)
const errorMessage = ref('')

const dialogTitle = computed(() => {
  if (activeMode.value === 'arribo') return 'Confirmar Arribo a Destino & Terminal'
  if (activeMode.value === 'transito') return 'Actualizar Seguimiento de Tránsito'
  return 'Registrar Envío de Origen Exitoso'
})

const availableModes = computed(() => {
  if (isExport.value) {
    return [{ label: 'Datos de Tránsito', value: 'transito' }]
  }
  return [
    { label: 'Salida Origen', value: 'salida' },
    { label: 'Tránsito', value: 'transito' },
    { label: 'Arribo', value: 'arribo' }
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
  if (isAereo.value) return 'Ej. LATAM Cargo, Atlas Air...'
  if (isTerrestre.value) return 'Ej. Ransa, Cruz del Sur Cargo...'
  return 'Ej. MSC, Maersk, Hapag-Lloyd...'
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
  if (isAereo.value) return 'Ej. Talma Aeropuerto / SAASA'
  if (isTerrestre.value) return 'Ej. Ransa Depósito'
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
  () => [props.visible, props.expediente, props.initialMode],
  ([vis, exp, mode]) => {
    if (!vis || !exp) return
    initializeForms(exp)
    activeMode.value = isExport.value ? 'transito' : (mode || 'salida')
  },
  { immediate: true }
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
    emit('saved', { mode: 'salida', data: updated })
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
    emit('saved', { mode: 'transito', data: updated })
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
    emit('saved', { mode: 'arribo', data: updated })
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar el arribo a destino.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
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
