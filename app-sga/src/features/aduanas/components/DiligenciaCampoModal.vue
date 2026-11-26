<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('600px'), maxWidth: '95vw' }"
    header="Registrar Intervención / Diligencia de Campo (SUNAT)"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="diligencia-campo-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span class="sga-dialog-context-sub font-mono">
            DAM: {{ expediente.numeroDam || 'Pendiente' }}
          </span>
        </div>
        <Tag
          :value="`Canal ${expediente.canalSunat || 'PENDIENTE'}`"
          :severity="expediente.canalSunat === 'ROJO' ? 'danger' : (expediente.canalSunat === 'NARANJA' ? 'warn' : 'success')"
          class="sga-mini-tag"
          rounded
        />
      </div>

      <div :class="['sga-banner', expediente?.canalSunat === 'ROJO' ? 'is-danger' : 'is-warn']">
        <i class="pi pi-exclamation-circle"></i>
        <div>
          <strong>
            {{ expediente?.canalSunat === 'ROJO' ? 'Canal ROJO: Reconocimiento Físico Obligatorio' : 'Canal NARANJA: Revisión Documentaria' }}
          </strong>
          <p class="diligencia-banner-desc">
            {{
              expediente?.canalSunat === 'ROJO'
                ? 'Diligencia presencial en depósito temporal con el Especialista SUNAT. Verificación de bultos, marcas, precintos y aforo físico.'
                : 'Cotejo documentario ante SUNAT de factura comercial, BL/guía aérea, packing list y autorizaciones sectoriales.'
            }}
          </p>
        </div>
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-form-field">
        <label for="diligencia-detalle" class="sga-form-label">Acta y Detalle de la Diligencia Aduanera *</label>
        <Textarea
          id="diligencia-detalle"
          v-model.trim="form.detalleInspeccion"
          rows="4"
          autoResize
          placeholder="Ej. Reconocimiento físico concluido a satisfacción con Especialista SUNAT. Precinto PE-SUNAT intacto, bultos cotejados sin discrepancias..."
          required
          size="small"
          fluid
        />
        <span class="sga-field-hint">
          Ingrese los hallazgos técnicos, número de acta de aforo o dictamen del especialista SUNAT
        </span>
      </div>

      <div class="sga-form-field">
        <label for="diligencia-obs" class="sga-form-label">Observaciones Adicionales / Incidencias</label>
        <Textarea
          id="diligencia-obs"
          v-model.trim="form.observaciones"
          rows="2"
          autoResize
          placeholder="Sin incidencias aduaneras / Cumple con regulaciones vigentes..."
          size="small"
          fluid
        />
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
        form="diligencia-campo-form"
        label="Registrar Diligencia Superada"
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
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { expedienteService } from '../services/expedienteService'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()

const loading = ref(false)
const errorMessage = ref('')

const form = ref({
  detalleInspeccion: '',
  observaciones: ''
})

watch(
  () => [props.visible, props.expediente],
  ([vis, exp]) => {
    if (vis && exp) {
      errorMessage.value = ''
      form.value = {
        detalleInspeccion: exp.detalleInspeccion || '',
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

  try {
    const payload = {
      detalleInspeccion: form.value.detalleInspeccion,
      observaciones: form.value.observaciones || null
    }

    const updated = await expedienteService.registrarDiligencia(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar la diligencia de campo aduanera.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.diligencia-banner-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  line-height: 1.4;
}
</style>
