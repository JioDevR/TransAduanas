<template>
  <Dialog
    :visible="visible"
    modal
    :header="config.header"
    :style="{ width: '540px', maxWidth: '95vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="sga-revision-dialog-body">
      <p class="sga-revision-desc">
        {{ config.descriptionPrefix }}
        <strong>{{ cotizacion?.codigoCotizacion }}</strong>{{ config.descriptionSuffix }}
      </p>

      <div class="sga-form-field">
        <label class="sga-form-label" :for="textareaId">{{ config.fieldLabel }}</label>
        <Textarea
          :id="textareaId"
          v-model="reason"
          rows="4"
          fluid
          :placeholder="config.placeholder"
        />
      </div>
    </div>

    <template #footer>
      <div class="sga-dialog-footer">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('update:visible', false)"
        />
        <Button
          type="button"
          :label="config.submitLabel"
          :icon="config.submitIcon"
          :severity="config.submitSeverity"
          size="small"
          :loading="loading"
          :disabled="!reason.trim() || loading"
          @click="handleSubmit"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import Button from 'primevue/button'

const MODE_CONFIG = {
  observe: {
    header: 'Solicitar Corrección a Ventas (Revisión)',
    descriptionPrefix: 'Especifique las observaciones técnicas o financieras para que el asesor comercial corrija la cotización ',
    descriptionSuffix: '. Una vez corregida, volverá automáticamente a su bandeja para aprobación.',
    fieldLabel: 'Detalle de Observación / Motivo de Revisión',
    placeholder: 'Ejemplo: Rectificar partida arancelaria para régimen definitivo o adjuntar póliza de flete estimada...',
    submitLabel: 'Enviar a Revisión',
    submitIcon: 'pi pi-send',
    submitSeverity: 'warn'
  },
  reject: {
    header: 'Rechazar Propuesta Comercial',
    descriptionPrefix: 'Indique la razón formal por la cual la cotización ',
    descriptionSuffix: ' no es viable técnica ni legalmente.',
    fieldLabel: 'Motivo de Rechazo Aduanero',
    placeholder: 'Indique la causal de rechazo (incompatibilidad de régimen, restricciones de SUNAT, etc.)...',
    submitLabel: 'Confirmar Rechazo',
    submitIcon: 'pi pi-times',
    submitSeverity: 'danger'
  }
}

const props = defineProps({
  visible: { type: Boolean, default: false },
  mode: {
    type: String,
    default: 'observe',
    validator: (v) => ['observe', 'reject'].includes(v)
  },
  cotizacion: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'confirm'])

const reason = ref('')
const textareaId = computed(() => `revision-reason-${props.mode}`)
const config = computed(() => MODE_CONFIG[props.mode] || MODE_CONFIG.observe)

watch(
  () => [props.visible, props.cotizacion, props.mode],
  ([isVisible]) => {
    if (isVisible) {
      reason.value = props.mode === 'observe' ? props.cotizacion?.motivoObservacion || '' : ''
    }
  }
)

const handleSubmit = () => {
  const trimmed = reason.value.trim()
  if (!trimmed) return
  emit('confirm', {
    cotizacion: props.cotizacion,
    mode: props.mode,
    reason: trimmed
  })
}
</script>

<style scoped>
.sga-revision-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sga-revision-desc {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--color-text-muted);
}

.sga-form-field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.sga-form-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.sga-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
