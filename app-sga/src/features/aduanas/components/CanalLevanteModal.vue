<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('560px'), maxWidth: '95vw' }"
    :header="actionType === 'canal' ? 'Asignar Canal SUNAT y Declaración DAM' : 'Autorizar Levante Aduanero'"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="canal-levante-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <!-- Ficha contextual del expediente (sgaUI) -->
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span v-if="expediente.clienteRazonSocial" class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial }}
          </span>
        </div>
        <Tag
          :value="expediente.numeroDam ? `DAM ${expediente.numeroDam}` : 'DAM Pendiente'"
          :severity="expediente.numeroDam ? 'info' : 'secondary'"
          class="font-mono sga-mini-tag"
          rounded
        />
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <!-- Modal: ASIGNAR CANAL SUNAT & DAM -->
      <template v-if="actionType === 'canal'">
        <div class="sga-form-field">
          <label for="dam-num" class="sga-form-label">Número de Declaración Aduanera (DAM) *</label>
          <InputText
            id="dam-num"
            :modelValue="canalForm.numeroDam"
            placeholder="Ej. 118-2026-10-123456"
            class="font-mono uppercase"
            required
            maxlength="30"
            size="small"
            fluid
            @update:modelValue="canalForm.numeroDam = ($event || '').toUpperCase().trimStart()"
          />
          <span class="sga-field-hint">Numeración oficial emitida por la Intendencia de Aduana</span>
        </div>

        <div class="sga-form-field">
          <span class="sga-form-label">Canal de Control SUNAT Asignado *</span>
          <div class="sga-choice-list" role="radiogroup" aria-label="Canal de Control SUNAT">
            <label
              v-for="opt in CANAL_OPTIONS"
              :key="opt.value"
              :for="`canal-opt-${opt.value}`"
              :class="[
                'sga-choice-card',
                opt.toneClass,
                { 'is-selected': canalForm.canalSunat === opt.value }
              ]"
            >
              <RadioButton
                :inputId="`canal-opt-${opt.value}`"
                v-model="canalForm.canalSunat"
                name="canalSunat"
                :value="opt.value"
              />
              <span :class="['sga-choice-dot', opt.dotClass]" aria-hidden="true"></span>
              <div class="sga-choice-texts">
                <strong>{{ opt.title }}</strong>
                <span>{{ opt.description }}</span>
              </div>
              <Tag
                v-if="canalForm.canalSunat === opt.value"
                value="Seleccionado"
                :severity="opt.severity"
                class="sga-mini-tag sga-choice-badge"
                rounded
              />
            </label>
          </div>
        </div>
      </template>

      <!-- Modal: AUTORIZAR LEVANTE -->
      <template v-else>
        <div class="sga-banner is-success">
          <i class="pi pi-shield"></i>
          <div>
            <strong>Conformidad Aduanera y Legal</strong>
            <p class="levante-banner-desc">
              Al sellar el levante, se autoriza el retiro de la mercancía del depósito temporal una vez verificado el pago de tributos aduaneros.
            </p>
          </div>
        </div>

        <div class="sga-form-field">
          <label for="levante-detalle" class="sga-form-label">Detalle de Inspección / Diligencia Aduanera</label>
          <Textarea
            id="levante-detalle"
            v-model.trim="levanteForm.detalleInspeccion"
            rows="3"
            autoResize
            placeholder="Diligencia conforme por especialista SUNAT. Precintos intactos y bultos verificados según manifiesto..."
            size="small"
            fluid
          />
        </div>
      </template>
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
        form="canal-levante-form"
        :label="actionType === 'canal' ? 'Registrar Canal SUNAT' : 'Autorizar Levante'"
        icon="pi pi-check"
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
import RadioButton from 'primevue/radiobutton'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { expedienteService } from '../services/expedienteService'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  expediente: {
    type: Object,
    default: null
  },
  actionType: {
    type: String, // 'canal' | 'levante'
    default: 'canal'
  }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()

const CANAL_OPTIONS = [
  {
    value: 'VERDE',
    title: 'Canal VERDE',
    description: 'Levante automático sin revisión documental ni física',
    toneClass: 'is-success',
    dotClass: 'is-green',
    severity: 'success'
  },
  {
    value: 'NARANJA',
    title: 'Canal NARANJA',
    description: 'Revisión documentaria requerida por especialista SUNAT',
    toneClass: 'is-warn',
    dotClass: 'is-orange',
    severity: 'warn'
  },
  {
    value: 'ROJO',
    title: 'Canal ROJO',
    description: 'Reconocimiento físico obligatorio en depósito temporal',
    toneClass: 'is-danger',
    dotClass: 'is-red',
    severity: 'danger'
  }
]

const loading = ref(false)
const errorMessage = ref('')

const canalForm = ref({
  numeroDam: '',
  canalSunat: 'VERDE'
})

const levanteForm = ref({
  detalleInspeccion: 'Inspección conforme. Sin incidencias físicas ni documentarias.'
})

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      errorMessage.value = ''
      if (props.expediente) {
        canalForm.value = {
          numeroDam: props.expediente.numeroDam || '',
          canalSunat: props.expediente.canalSunat === 'PENDIENTE' ? 'VERDE' : (props.expediente.canalSunat || 'VERDE')
        }
        levanteForm.value = {
          detalleInspeccion: props.expediente.detalleInspeccion || 'Inspección conforme. Sin incidencias físicas ni documentarias.'
        }
      }
    }
  }
)

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  if (!props.expediente) return
  errorMessage.value = ''

  loading.value = true
  try {
    let result
    if (props.actionType === 'canal') {
      const damTrim = (canalForm.value.numeroDam || '').trim()
      if (!damTrim || !canalForm.value.canalSunat) {
        errorMessage.value = 'Debe ingresar el número de DAM y seleccionar el canal SUNAT.'
        loading.value = false
        return
      }
      result = await expedienteService.asignarCanal(props.expediente.idExpediente, {
        numeroDam: damTrim,
        canalSunat: canalForm.value.canalSunat
      })
    } else {
      result = await expedienteService.autorizarLevante(props.expediente.idExpediente, levanteForm.value)
    }

    emit('saved', result)
    handleClose()
  } catch (err) {
    console.error('Error al actualizar trámite aduanero:', err)
    errorMessage.value =
      err.response?.data?.message || 'Error al procesar la actualización del expediente.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.uppercase {
  text-transform: uppercase;
}

.levante-banner-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  line-height: 1.45;
}
</style>
