<template>
  <Dialog
    :visible="visible"
    modal
    :header="isEditMode ? 'Editar Liquidación Aduanera' : 'Emisión de Liquidación Aduanera & Balance de Gastos'"
    :style="{ width: getDialogWidth('850px') }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="sga-dialog-body">
      <div class="sga-form-field">
        <label for="f-liq-exp" class="sga-form-label">Expediente Aduanero Asociado *</label>
        <Select
          inputId="f-liq-exp"
          v-model="form.idExpediente"
          :options="expedienteOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Seleccione un expediente aduanero..."
          filter
          size="small"
          fluid
          :disabled="loadingEstimado || isExpedienteFixed"
          @change="$emit('expediente-change')"
        />
      </div>

      <div v-if="selectedExpedienteInfo" class="sga-exp-banner">
        <div class="sga-meta-item">
          <span class="sga-meta-label">CLIENTE IMPORTADOR</span>
          <span class="sga-meta-val">
            {{ selectedExpedienteInfo.clienteRazonSocial }} ({{ selectedExpedienteInfo.clienteNumeroDocumento || '—' }})
          </span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">CANAL SUNAT / DAM</span>
          <span class="sga-meta-val font-mono">
            {{ selectedExpedienteInfo.canalSunat || 'PENDIENTE' }} · DAM: {{ selectedExpedienteInfo.numeroDam || 'S/N' }}
          </span>
        </div>
      </div>

      <!-- Rubros en Soles -->
      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label for="f-liq-tributos" class="sga-form-label">Tributos Liquidados SUNAT (S/)</label>
          <InputNumber
            inputId="f-liq-tributos"
            v-model="form.totalTributosSunat"
            mode="currency"
            currency="PEN"
            locale="es-PE"
            :min="0"
            :minFractionDigits="2"
            size="small"
            fluid
          />
          <small class="sga-cell-sub">Ad-Valorem, IGV e IPM aduanero (no gravado internamente).</small>
        </div>

        <div class="sga-form-field">
          <label for="f-liq-almacen" class="sga-form-label">Gastos de Almacén & Terminal (S/)</label>
          <InputNumber
            inputId="f-liq-almacen"
            v-model="form.gastosAlmacenPuerto"
            mode="currency"
            currency="PEN"
            locale="es-PE"
            :min="0"
            :minFractionDigits="2"
            size="small"
            fluid
          />
          <small class="sga-cell-sub">Custodia, pesaje VGM y servicios de depósito temporal.</small>
        </div>

        <div class="sga-form-field">
          <label for="f-liq-flete" class="sga-form-label">Flete de Transporte Local (S/)</label>
          <InputNumber
            inputId="f-liq-flete"
            v-model="form.fleteTransporteLocal"
            mode="currency"
            currency="PEN"
            locale="es-PE"
            :min="0"
            :minFractionDigits="2"
            size="small"
            fluid
          />
          <small class="sga-cell-sub">Traslado en unidad autorizada desde puerto/almacén a planta.</small>
        </div>

        <div class="sga-form-field">
          <label for="f-liq-honorarios" class="sga-form-label">Honorarios de Agencia Aduanera (S/)</label>
          <InputNumber
            inputId="f-liq-honorarios"
            v-model="form.honorariosAgencia"
            mode="currency"
            currency="PEN"
            locale="es-PE"
            :min="0"
            :minFractionDigits="2"
            size="small"
            fluid
          />
          <small class="sga-cell-sub">Comisión profesional por agenciamiento y despacho.</small>
        </div>
      </div>

      <!-- Parámetros Tributarios -->
      <div class="sga-grid-3">
        <div class="sga-form-field">
          <label for="f-liq-tc" class="sga-form-label">Tipo de Cambio (S/ por USD)</label>
          <InputNumber
            inputId="f-liq-tc"
            v-model="form.tipoCambio"
            :min="1"
            :minFractionDigits="2"
            :maxFractionDigits="4"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="f-liq-factura" class="sga-form-label">N° Factura / Boleta</label>
          <InputText
            id="f-liq-factura"
            v-model.trim="form.numeroFactura"
            placeholder="F001-0000421"
            maxlength="30"
            size="small"
            class="font-mono"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <span class="sga-form-label">Detracción SPOT SUNAT</span>
          <label class="sga-spot-check" for="f-liq-spot">
            <Checkbox inputId="f-liq-spot" v-model="form.aplicaDetraccion" binary />
            <span>Aplica SPOT (12% si ≥ S/ 700)</span>
          </label>
        </div>
      </div>

      <!-- Resumen Tributario Live -->
      <div class="sga-totals-box">
        <div class="sga-total-line">
          <span>Subtotal Servicios Gravados</span>
          <span class="font-mono">{{ formatPen(totals.subtotalImponible) }}</span>
        </div>
        <div class="sga-total-line">
          <span>IGV de Servicios (18%)</span>
          <span class="font-mono">{{ formatPen(totals.igvMonto) }}</span>
        </div>
        <div class="sga-total-line">
          <span>Tributos SUNAT (Tasa Aduanera)</span>
          <span class="font-mono">{{ formatPen(form.totalTributosSunat) }}</span>
        </div>
        <div v-if="form.aplicaDetraccion" class="sga-total-line sga-text-warn">
          <span>
            Detracción SPOT Banco de la Nación (12%)
            <small v-if="!totals.superaUmbralSpot" class="sga-cell-sub">(No supera S/ 700.00)</small>
          </span>
          <span class="font-mono">- {{ formatPen(totals.montoDetraccion) }}</span>
        </div>
        <div class="sga-total-line is-grand">
          <span>TOTAL GENERAL A COBRAR (PEN)</span>
          <span class="font-mono">{{ formatPen(totals.totalGeneralPen) }}</span>
        </div>
        <div class="sga-total-line sga-cell-sub">
          <span>Equivalente Consolidado en Dólares (USD)</span>
          <span class="font-mono">{{ formatUsd(totals.totalGeneralUsd) }}</span>
        </div>
      </div>

      <div class="sga-form-field">
        <label for="f-liq-obs" class="sga-form-label">Observaciones y Términos</label>
        <Textarea
          id="f-liq-obs"
          v-model="form.observaciones"
          rows="2"
          fluid
          placeholder="Detalles complementarios, plazos de crédito o notas al cliente..."
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
          :disabled="submitting"
          @click="$emit('update:visible', false)"
        />
        <Button
          type="button"
          :label="isEditMode ? 'Guardar Cambios' : 'Emitir Liquidación'"
          icon="pi pi-check"
          size="small"
          :loading="submitting"
          :disabled="submitting || !form.idExpediente"
          @click="$emit('submit')"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { formatPen, formatUsd } from '../../utils/finanzasFormatters'

const props = defineProps({
  visible: { type: Boolean, default: false },
  isEditMode: { type: Boolean, default: false },
  isExpedienteFixed: { type: Boolean, default: false },
  loadingEstimado: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false },
  form: { type: Object, required: true },
  totals: { type: Object, required: true },
  expedientes: { type: Array, default: () => [] }
})

defineEmits(['update:visible', 'expediente-change', 'submit'])

const { getDialogWidth } = useResponsive()

const expedienteOptions = computed(() =>
  (props.expedientes || []).map((e) => ({
    label: `${e.codigoExpediente} — ${e.clienteRazonSocial || 'Cliente'} (DAM: ${e.numeroDam || 'S/N'})`,
    value: e.idExpediente
  }))
)

const selectedExpedienteInfo = computed(() => {
  if (!props.form?.idExpediente) return null
  return (props.expedientes || []).find((e) => e.idExpediente === props.form.idExpediente) || null
})
</script>

<style scoped>
.sga-exp-banner {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  border-radius: 10px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.sga-spot-check {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 36px;
  font-size: 0.82rem;
  color: var(--color-text-main);
  cursor: pointer;
}

.sga-totals-box {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  padding: 1rem 1.15rem;
  border-radius: 12px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  font-size: 0.85rem;
}

.sga-total-line {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
}

.sga-total-line.is-grand {
  margin-top: 0.35rem;
  padding-top: 0.55rem;
  border-top: 1px solid var(--color-border);
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-primary);
}
</style>
