<template>
  <Dialog
    :visible="visible"
    modal
    :header="dialogHeader"
    :style="{ width: getDialogWidth('600px') }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="anticipo" class="sga-dialog-body">
      <!-- Resumen del Abono -->
      <div class="sga-summary-card">
        <div class="sga-summary-row">
          <span class="sga-summary-lbl">Cotización</span>
          <strong class="sga-summary-val font-mono">
            {{ anticipo.codigoCotizacion }} — {{ anticipo.clienteRazonSocial || 'Cliente' }}
          </strong>
        </div>
        <div class="sga-summary-row">
          <span class="sga-summary-lbl">Operación Bancaria</span>
          <span class="sga-summary-val font-mono">
            {{ anticipo.bancoDestino }} (N° {{ anticipo.numeroOperacion }})
          </span>
        </div>
        <div class="sga-summary-row">
          <span class="sga-summary-lbl">Fecha Operación</span>
          <span class="sga-summary-val font-mono">{{ anticipo.fechaOperacion || 'No especificada' }}</span>
        </div>
        <div class="sga-summary-row">
          <span class="sga-summary-lbl">Monto Abonado</span>
          <strong class="sga-summary-val font-mono sga-text-success">
            {{ montoFormateado }}
            <span v-if="anticipo.moneda === 'USD' && anticipo.montoEquivalentePen" class="sga-cell-sub">
              (Equiv: {{ formatPen(anticipo.montoEquivalentePen) }})
            </span>
          </strong>
        </div>
        <div class="sga-summary-row">
          <span class="sga-summary-lbl">Modalidad</span>
          <span class="sga-summary-val">{{ formatTipoAbono(anticipo.tipoAbono) }}</span>
        </div>
      </div>

      <!-- Modo APROBAR -->
      <template v-if="accion === 'APROBAR'">
        <div class="sga-banner is-success">
          <i class="pi pi-check-circle sga-banner-icon"></i>
          <div>
            <strong>Conformidad Contable:</strong> Al aprobar este pago, la cotización quedará
            habilitada financieramente y se desbloqueará la Apertura de Expediente Aduanero.
          </div>
        </div>

        <label class="sga-cotejo-box" for="fin-cotejo-check">
          <Checkbox
            inputId="fin-cotejo-check"
            :modelValue="confirmacionCotejo"
            binary
            @update:modelValue="$emit('update:confirmacionCotejo', $event)"
          />
          <span class="sga-cotejo-text">
            He verificado en la cuenta bancaria de TransAduanas que el importe y número de operación
            son fidedignos y están acreditados en el extracto.
          </span>
        </label>

        <div class="sga-form-field">
          <label for="fin-obs-aprobacion" class="sga-form-label">Anotación Contable / Auditoría (Opcional)</label>
          <Textarea
            id="fin-obs-aprobacion"
            :modelValue="observacionFinanzas"
            rows="2"
            fluid
            placeholder="Ej. Abono verificado conforme en extracto bancario BCP..."
            @update:modelValue="$emit('update:observacionFinanzas', $event)"
          />
        </div>
      </template>

      <!-- Modo OBSERVAR / RECHAZAR -->
      <template v-else>
        <div :class="['sga-banner', accion === 'RECHAZAR' ? 'is-danger' : 'is-warn']">
          <i :class="[accion === 'RECHAZAR' ? 'pi pi-times-circle' : 'pi pi-exclamation-triangle', 'sga-banner-icon']"></i>
          <div>
            <strong>{{ accion === 'RECHAZAR' ? 'Rechazo Definitivo:' : 'Observación Subsanable:' }}</strong>
            Se notificará al importador en el Portal B2B sobre este comprobante.
          </div>
        </div>

        <div class="sga-form-field">
          <span class="sga-form-label">Motivos Frecuentes</span>
          <div class="sga-chips-row">
            <Button
              v-for="m in motivosObservacionOptions"
              :key="m.value"
              type="button"
              :label="m.label"
              :severity="observacionFinanzas === m.value ? 'primary' : 'secondary'"
              :outlined="observacionFinanzas !== m.value"
              size="small"
              @click="$emit('update:observacionFinanzas', m.value)"
            />
          </div>
        </div>

        <div class="sga-form-field">
          <label for="fin-obs-detalle" class="sga-form-label">
            Detalle del Motivo (Obligatorio) *
          </label>
          <Textarea
            id="fin-obs-detalle"
            :modelValue="observacionFinanzas"
            rows="3"
            fluid
            placeholder="Indique con claridad qué debe corregir el cliente en su comprobante o transferencia..."
            @update:modelValue="$emit('update:observacionFinanzas', $event)"
          />
        </div>
      </template>
    </div>

    <template #footer>
      <div class="sga-dialog-footer">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          :disabled="submitting"
          @click="$emit('update:visible', false)"
        />
        <Button
          type="button"
          :label="submitLabel"
          :icon="submitIcon"
          :severity="submitSeverity"
          size="small"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="$emit('confirm')"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { catalogoService } from '@/features/admin/catalogos/services/catalogoService'
import {
  MOTIVOS_OBSERVACION_ABONO,
  formatPen,
  formatUsd,
  formatTipoAbono
} from '../../utils/finanzasFormatters'

const motivosObservacionOptions = ref(MOTIVOS_OBSERVACION_ABONO)

onMounted(async () => {
  try {
    const lista = await catalogoService.getMotivosObservacion('ABONO')
    if (Array.isArray(lista) && lista.length > 0) {
      motivosObservacionOptions.value = lista.map((m) => ({
        label: m.descripcion,
        value: m.descripcion
      }))
    }
  } catch {
    // Mantiene respaldo local si el catálogo no responde
  }
})

const props = defineProps({
  visible: { type: Boolean, default: false },
  anticipo: { type: Object, default: null },
  accion: { type: String, default: 'APROBAR' },
  observacionFinanzas: { type: String, default: '' },
  confirmacionCotejo: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false }
})

defineEmits([
  'update:visible',
  'update:observacionFinanzas',
  'update:confirmacionCotejo',
  'confirm'
])

const { getDialogWidth } = useResponsive()

const dialogHeader = computed(() => {
  if (props.accion === 'APROBAR') return 'Aprobación de Pago / Anticipo Bancario'
  if (props.accion === 'RECHAZAR') return 'Rechazo de Comprobante Bancario'
  return 'Observación de Comprobante de Pago'
})

const montoFormateado = computed(() => {
  if (!props.anticipo) return ''
  return props.anticipo.moneda === 'PEN'
    ? formatPen(props.anticipo.montoAbonado)
    : formatUsd(props.anticipo.montoAbonado)
})

const canSubmit = computed(() => {
  if (props.submitting) return false
  if (props.accion === 'APROBAR') return props.confirmacionCotejo
  return Boolean(props.observacionFinanzas && props.observacionFinanzas.trim())
})

const submitLabel = computed(() => {
  if (props.accion === 'APROBAR') return 'Confirmar Aprobación Bancaria'
  if (props.accion === 'RECHAZAR') return 'Confirmar Rechazo'
  return 'Notificar Observación al Cliente'
})

const submitIcon = computed(() => {
  if (props.accion === 'APROBAR') return 'pi pi-check'
  if (props.accion === 'RECHAZAR') return 'pi pi-ban'
  return 'pi pi-send'
})

const submitSeverity = computed(() => {
  if (props.accion === 'APROBAR') return 'success'
  if (props.accion === 'RECHAZAR') return 'danger'
  return 'warn'
})
</script>

<style scoped>
.sga-summary-card {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.9rem 1.05rem;
  border-radius: 10px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.sga-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
  font-size: 0.85rem;
}

.sga-summary-lbl {
  color: var(--color-text-muted);
  font-weight: 500;
}

.sga-summary-val {
  color: var(--color-text-main);
  text-align: right;
}

.sga-banner-icon {
  font-size: 1.1rem;
  margin-top: 0.1rem;
  flex-shrink: 0;
}

.sga-cotejo-box {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  padding: 0.85rem 1rem;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-base);
  cursor: pointer;
}

.sga-cotejo-text {
  font-size: 0.83rem;
  line-height: 1.45;
  color: var(--color-text-main);
}

.sga-chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
</style>
