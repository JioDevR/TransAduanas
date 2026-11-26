<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('600px'), maxWidth: '95vw' }"
    header="Revisión de Conformidad y Cierre Final de Expediente"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="cierre-expediente-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span class="sga-dialog-context-sub font-mono">
            DAM: {{ expediente.numeroDam || 'S/N' }}
          </span>
        </div>
        <Tag
          :value="isExportacion ? 'Exportación' : 'Importación'"
          :severity="isExportacion ? 'success' : 'info'"
          class="sga-mini-tag"
          rounded
        />
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="cierre-checklist-card">
        <h4 class="checklist-title">
          {{ isExportacion ? 'Checklist de Cierre de Despacho de Exportación' : 'Checklist de Cierre de Despacho de Importación' }}
        </h4>

        <div class="check-row">
          <i :class="['pi check-icon', expediente?.fechaLevanteAutorizacion ? 'pi-check-circle text-success' : 'pi-times-circle text-danger']"></i>
          <div class="check-info">
            <span class="check-label">{{ isExportacion ? 'Levante Aduanero de Exportación Autorizado' : 'Levante Aduanero Autorizado' }}</span>
            <span class="check-sub font-mono">
              {{ expediente?.fechaLevanteAutorizacion ? `Concedido el ${formatDate(expediente.fechaLevanteAutorizacion)}` : 'Pendiente de autorización' }}
            </span>
          </div>
        </div>

        <div v-if="isExportacion" class="check-row">
          <i :class="['pi check-icon', embarqueExportacionVerificado ? 'pi-check-circle text-success' : 'pi-exclamation-triangle text-warning']"></i>
          <div class="check-info">
            <span class="check-label">Embarque Internacional a Bordo de Nave</span>
            <span class="check-sub">
              {{ embarqueExportacionVerificado ? `Nave: ${expediente?.naveViaje || 'Confirmada'} — Carga estibada y zarpada a destino` : 'Pendiente de confirmación de embarque' }}
            </span>
          </div>
        </div>
        <div v-else class="check-row">
          <i :class="['pi check-icon', entregaVerificada ? 'pi-check-circle text-success' : 'pi-exclamation-triangle text-warning']"></i>
          <div class="check-info">
            <span class="check-label">Entrega de Carga Verificada</span>
            <span v-if="expediente?.servicioTransporte" class="check-sub">
              Transporte SGA: Tracking <span class="font-mono">{{ expediente?.codigoTrackingTransporte || '—' }}</span> ({{ expediente?.estadoTransporte || 'PENDIENTE' }})
            </span>
            <span v-else class="check-sub">
              Retiro por el Cliente en Terminal Extraportuario
            </span>
          </div>
        </div>

        <div class="check-row">
          <i class="pi pi-check-circle text-success check-icon"></i>
          <div class="check-info">
            <span class="check-label">{{ isExportacion ? 'Legajo Digital de Exportación & Regularización' : 'Tributos y Liquidación Aduanera' }}</span>
            <span class="check-sub">{{ isExportacion ? 'DAM de exportación regularizada y legajo archivado conforme' : 'Tributos cancelados ante SUNAT conforme' }}</span>
          </div>
        </div>
      </div>

      <div class="sga-form-field">
        <label for="cierre-obs" class="sga-form-label">Acta de Conformidad / Observaciones de Cierre Definitivo *</label>
        <Textarea
          id="cierre-obs"
          v-model.trim="form.observacionesCierre"
          rows="3"
          autoResize
          placeholder="Ej. Se revisaron conformidades de levante aduanero, entrega a satisfacción y archivo de legajo digital del despacho. Expediente cerrado y archivado conforme."
          required
          size="small"
          fluid
        />
        <span class="sga-field-hint">
          Esta acción cambiará el estado del expediente a CERRADO y notificará formalmente al cliente.
        </span>
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
        form="cierre-expediente-form"
        label="Confirmar Cierre de Expediente"
        icon="pi pi-lock"
        size="small"
        :loading="loading"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
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

const defaultObs = 'Conformidad técnica y liquidación aduanera verificada. Despacho concluido y archivado formalmente.'

const form = ref({
  observacionesCierre: defaultObs
})

const isExportacion = computed(() => {
  const reg = props.expediente?.regimenAduanero || ''
  return reg.toUpperCase().includes('EXPORT')
})

const embarqueExportacionVerificado = computed(() => {
  if (!props.expediente) return false
  const obs = props.expediente.observaciones || ''
  return Boolean(
    props.expediente.naveViaje ||
    props.expediente.fechaZarpeEtd ||
    obs.includes('EMBARQUE') ||
    obs.includes('RECEPCIÓN DESTINO')
  )
})

const entregaVerificada = computed(() => {
  if (!props.expediente) return false
  if (props.expediente.servicioTransporte) {
    return props.expediente.estadoTransporte === 'ENTREGADO'
  }
  return true
})

watch(
  () => [props.visible, props.expediente],
  ([vis, exp]) => {
    if (vis && exp) {
      errorMessage.value = ''
      form.value.observacionesCierre = defaultObs
    }
  },
  { immediate: true }
)

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

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
      observacionesCierre: form.value.observacionesCierre
    }

    const updated = await expedienteService.cerrarExpediente(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al cerrar el expediente aduanero.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.cierre-checklist-card {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md, 10px);
  padding: 0.9rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}

.checklist-title {
  margin: 0 0 0.1rem;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.check-row {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
}

.check-icon {
  margin-top: 0.15rem;
  font-size: 0.9rem;
  flex-shrink: 0;
}

.text-success {
  color: var(--color-success-text);
}

.text-danger {
  color: var(--color-danger-text);
}

.text-warning {
  color: var(--color-warn-text);
}

.check-info {
  display: flex;
  flex-direction: column;
  gap: 0.12rem;
}

.check-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.check-sub {
  font-size: 0.76rem;
  color: var(--color-text-muted);
}
</style>
