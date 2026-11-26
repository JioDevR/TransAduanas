<template>
  <Dialog
    :visible="visible"
    modal
    header="Registro de Cobranza & Operación Bancaria"
    :style="{ width: getDialogWidth('620px') }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="liquidacion" class="sga-dialog-body">
      <div class="sga-pago-banner">
        <div>
          <span class="sga-kicker">LIQUIDACIÓN A COBRAR</span>
          <div class="sga-code-cell font-mono">{{ liquidacion.codigoLiquidacion }}</div>
          <div class="sga-cell-primary">{{ liquidacion.clienteRazonSocial }}</div>
        </div>
        <div class="sga-pago-banner-right">
          <span class="sga-meta-label">TOTAL GENERAL</span>
          <strong class="sga-total-amount font-mono sga-text-success">
            {{ formatPen(liquidacion.totalGeneralPen) }}
          </strong>
          <span v-if="liquidacion.aplicaDetraccion" class="sga-cell-sub font-mono">
            SPOT (12%): {{ formatPen(liquidacion.montoDetraccion) }}
          </span>
        </div>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label for="f-cobro-metodo" class="sga-form-label">Medio de Pago *</label>
          <Select
            inputId="f-cobro-metodo"
            v-model="pagoForm.metodoPago"
            :options="METODOS_PAGO_LIQUIDACION"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="f-cobro-banco" class="sga-form-label">Banco de Destino *</label>
          <Select
            inputId="f-cobro-banco"
            v-model="pagoForm.bancoDestino"
            :options="BANCOS_DESTINO_LIQUIDACION"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="f-cobro-op" class="sga-form-label">N° de Operación Bancaria *</label>
          <InputText
            id="f-cobro-op"
            v-model.trim="pagoForm.numeroOperacion"
            placeholder="Ej. OP-90823412"
            maxlength="50"
            size="small"
            class="font-mono"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="f-cobro-fact" class="sga-form-label">N° de Factura Emitida</label>
          <InputText
            id="f-cobro-fact"
            v-model.trim="pagoForm.numeroFactura"
            placeholder="F001-000421"
            maxlength="30"
            size="small"
            class="font-mono"
            fluid
          />
        </div>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label for="f-cobro-fecha" class="sga-form-label">Fecha y Hora de Pago *</label>
          <InputText
            id="f-cobro-fecha"
            v-model="pagoForm.fechaPago"
            type="datetime-local"
            size="small"
            class="font-mono"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <span class="sga-form-label">Voucher Bancario (Opcional)</span>
          <div class="sga-voucher-picker">
            <input
              ref="voucherInputRef"
              type="file"
              accept=".pdf,image/png,image/jpeg,image/jpg"
              class="sga-hidden-input"
              @change="onFileChange"
            />
            <Button
              type="button"
              :label="voucherFile ? voucherFile.name : 'Adjuntar Voucher'"
              icon="pi pi-paperclip"
              severity="secondary"
              outlined
              size="small"
              @click="voucherInputRef?.click()"
            />
            <Button
              v-if="voucherFile"
              type="button"
              icon="pi pi-times"
              severity="danger"
              text
              size="small"
              title="Quitar voucher"
              @click="clearVoucher"
            />
          </div>
        </div>
      </div>

      <div class="sga-form-field">
        <label for="f-cobro-obs" class="sga-form-label">Observaciones de Cobranza</label>
        <Textarea
          id="f-cobro-obs"
          v-model="pagoForm.observaciones"
          rows="2"
          fluid
          placeholder="Notas de abono, número de cuenta origen, etc..."
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
          label="Confirmar Cobro Conforme"
          icon="pi pi-check-circle"
          severity="success"
          size="small"
          :loading="submitting"
          :disabled="submitting || !pagoForm.numeroOperacion?.trim()"
          @click="$emit('submit')"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import {
  METODOS_PAGO_LIQUIDACION,
  BANCOS_DESTINO_LIQUIDACION,
  formatPen
} from '../../utils/finanzasFormatters'

defineProps({
  visible: { type: Boolean, default: false },
  liquidacion: { type: Object, default: null },
  pagoForm: { type: Object, required: true },
  voucherFile: { type: Object, default: null },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'update:voucherFile', 'submit'])

const { getDialogWidth } = useResponsive()
const voucherInputRef = ref(null)

function onFileChange(event) {
  const file = event.target.files?.[0] || null
  emit('update:voucherFile', file)
}

function clearVoucher() {
  emit('update:voucherFile', null)
  if (voucherInputRef.value) voucherInputRef.value.value = ''
}
</script>

<style scoped>
.sga-pago-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 0.9rem 1.05rem;
  border-radius: 12px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.sga-pago-banner-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.15rem;
}

.sga-voucher-picker {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.sga-hidden-input {
  display: none;
}
</style>
