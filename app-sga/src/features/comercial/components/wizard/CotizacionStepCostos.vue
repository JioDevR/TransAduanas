<template>
  <section class="sga-step-card">
    <!-- 1. FOB & Flete Internacional -->
    <div class="sga-costs-grid">
      <div class="sga-field">
        <label for="cot-fob" class="sga-field-label">VALOR DE MERCANCÍA (FOB USD) *</label>
        <InputNumber
          inputId="cot-fob"
          v-model="form.valorFob"
          mode="currency"
          currency="USD"
          locale="en-US"
          :min="0"
          :disabled="submitting"
          size="small"
          fluid
        />
        <span class="sga-field-hint">Base imponible de factura comercial del proveedor.</span>
      </div>

      <div class="sga-field">
        <label for="cot-flete" class="sga-field-label">FLETE INTERNACIONAL ESTIMADO (USD)</label>
        <InputNumber
          inputId="cot-flete"
          v-model="form.montoFleteEstimado"
          mode="currency"
          currency="USD"
          locale="en-US"
          :min="0"
          :disabled="submitting"
          size="small"
          fluid
        />
        <span class="sga-field-hint">Costo del flete marítimo o aéreo internacional.</span>
      </div>
    </div>

    <!-- 2. Seguro Estimado & Valor CIF -->
    <div class="sga-costs-grid">
      <div class="sga-field">
        <div class="sga-label-row">
          <label for="cot-seguro" class="sga-field-label">SEGURO ESTIMADO (USD)</label>
          <span v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'" class="sga-mode-tag">
            {{ seguroManual ? 'MANUAL' : 'AUTO 0.75%' }}
          </span>
        </div>
        <InputNumber
          inputId="cot-seguro"
          :modelValue="form.montoSeguroEstimado"
          mode="currency"
          currency="USD"
          locale="en-US"
          :min="0"
          :disabled="submitting"
          size="small"
          fluid
          @update:modelValue="$emit('seguro-manual-input', $event)"
        />
        <div class="sga-seguro-hint-row">
          <span class="sga-field-hint">
            {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Opcional según Incoterm.' : 'Referencial SUNAT 0.75% del FOB o editable.' }}
          </span>
          <Button
            v-if="seguroManual && form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'"
            type="button"
            label="Restablecer 0.75%"
            link
            size="small"
            @click="$emit('reset-seguro')"
          />
        </div>
      </div>

      <div class="sga-field">
        <label class="sga-field-label">
          {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'VALOR OFICIAL DE EXPORTACIÓN (DAM 40)' : 'VALOR CIF BASE (FOB + FLETE + SEGURO)' }}
        </label>
        <div class="sga-cif-box">
          <span class="sga-cif-amount font-mono">
            ${{ formatNumber(form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? form.valorFob : valorCif) }}
          </span>
          <span class="sga-cif-currency">USD</span>
        </div>
        <span class="sga-field-hint">
          {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Referencia aduanera de salida.' : 'Base imponible para cálculo de tributos SUNAT.' }}
        </span>
      </div>
    </div>

    <!-- 3. Gastos Operativos y Transporte Local -->
    <div class="sga-costs-grid">
      <div class="sga-field">
        <label for="cot-honorarios" class="sga-field-label">GASTOS OPERATIVOS Y AGENCIA (USD) *</label>
        <InputNumber
          inputId="cot-honorarios"
          v-model="form.montoHonorariosEstimado"
          mode="currency"
          currency="USD"
          locale="en-US"
          :min="0"
          :disabled="submitting"
          size="small"
          fluid
        />
        <span class="sga-field-hint">Honorarios aduaneros, visto bueno y almacén temporal.</span>
      </div>

      <div v-if="form.servicioTransporte" class="sga-field">
        <label for="cot-transporte" class="sga-field-label">
          FLETE TRANSPORTE LOCAL ({{ form.origenProvincia || 'Origen' }} → {{ form.destinoProvincia || 'Destino' }})
        </label>
        <InputNumber
          inputId="cot-transporte"
          v-model="form.costoTransporteInterno"
          mode="currency"
          currency="USD"
          locale="en-US"
          :min="0"
          :disabled="submitting"
          size="small"
          fluid
        />
        <span class="sga-field-hint">Traslado terrestre local con flota SGA.</span>
      </div>
    </div>

    <!-- 4. Tributos SUNAT -->
    <Message v-if="form.regimenAduanero === 'EXPORTACION_DEFINITIVA'" severity="info" :closable="false">
      <strong>Exportación Definitiva (REG-40) — Inafecto de Tributos SUNAT:</strong>
      Ad-Valórem 0%, IGV/IPM 0%, Percepción 0% (Total tributos: $0.00 USD).
    </Message>

    <div v-else class="sga-tributos-panel">
      <div class="sga-tributos-header">
        <div>
          <h4 class="sga-tributos-title">Desglose de Tributos SUNAT (USD)</h4>
          <span class="sga-field-hint">Calculado sobre Valor CIF (${{ formatNumber(valorCif) }} USD)</span>
        </div>
        <div class="sga-tributos-total">
          <span class="sga-micro-label">TRIBUTOS ESTIMADOS</span>
          <span class="sga-tributo-amount font-mono">${{ formatNumber(form.montoTributosEstimado) }} USD</span>
        </div>
      </div>

      <div class="sga-tributos-grid">
        <div class="sga-tributo-box">
          <label for="cot-tasa-advalorem" class="sga-field-label">AD-VALÓREM *</label>
          <Select
            inputId="cot-tasa-advalorem"
            v-model="form.tasaAdValorem"
            :options="adValoremOptions"
            optionLabel="label"
            optionValue="value"
            :disabled="submitting"
            size="small"
            fluid
          />
          <div class="sga-tributo-calc">
            <span>Monto:</span>
            <strong class="font-mono">${{ formatNumber(montoAdValorem) }}</strong>
          </div>
        </div>

        <div class="sga-tributo-box">
          <span class="sga-field-label">IGV (16%) + IPM (2%)</span>
          <div class="sga-fixed-rate">18.00% Fija SUNAT</div>
          <div class="sga-tributo-calc">
            <span>Monto:</span>
            <strong class="font-mono">${{ formatNumber(montoIgv) }}</strong>
          </div>
        </div>

        <div class="sga-tributo-box">
          <label for="cot-tasa-percepcion" class="sga-field-label">PERCEPCIÓN SUNAT</label>
          <Select
            inputId="cot-tasa-percepcion"
            v-model="form.tasaPercepcion"
            :options="percepcionOptions"
            optionLabel="label"
            optionValue="value"
            :disabled="submitting"
            size="small"
            fluid
          />
          <div class="sga-tributo-calc">
            <span>Monto:</span>
            <strong class="font-mono">${{ formatNumber(montoPercepcion) }}</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. Total Liquidado -->
    <div class="sga-total-banner">
      <div>
        <span class="sga-total-kicker">IMPORTE TOTAL LIQUIDADO</span>
        <p class="sga-field-hint">
          {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Flete + Gastos Operativos (excluye FOB)' : 'Flete + Gastos Operativos + Tributos (excluye FOB)' }}
        </p>
      </div>
      <div class="sga-total-right">
        <span class="sga-total-num font-mono">${{ formatNumber(form.totalCotizado) }}</span>
        <span class="sga-total-curr">USD</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Message from 'primevue/message'

defineProps({
  form: { type: Object, required: true },
  seguroManual: { type: Boolean, default: false },
  valorCif: { type: Number, default: 0 },
  montoAdValorem: { type: Number, default: 0 },
  montoIgv: { type: Number, default: 0 },
  montoPercepcion: { type: Number, default: 0 },
  formatNumber: { type: Function, required: true },
  submitting: { type: Boolean, default: false }
})

defineEmits(['seguro-manual-input', 'reset-seguro'])

const adValoremOptions = [
  { label: '0% (Insumos / Libre arancel)', value: 0 },
  { label: '6% (Bienes intermedios)', value: 6 },
  { label: '11% (Bienes de consumo)', value: 11 }
]

const percepcionOptions = [
  { label: '0% (Sin percepción)', value: 0 },
  { label: '3.5% (Importador habitual)', value: 3.5 },
  { label: '5% (Bienes usados)', value: 5 },
  { label: '10% (Primera importación)', value: 10 }
]
</script>

<style scoped>
.sga-step-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--sga-card-radius, 14px);
  padding: var(--sga-card-padding-y, 1.75rem) var(--sga-card-padding-x, 2rem);
  display: flex;
  flex-direction: column;
  gap: 1.35rem;
}

.sga-costs-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.sga-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.sga-label-row,
.sga-seguro-hint-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.sga-field-label,
.sga-micro-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-text-muted);
}

.sga-mode-tag {
  font-size: 0.625rem;
  font-weight: 700;
  color: var(--color-primary);
}

.sga-field-hint {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-cif-box {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 0.55rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-bg-base);
}

.sga-cif-amount {
  font-size: 1rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-main);
}

.sga-cif-currency {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-tributos-panel {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: var(--sga-subpanel-padding, 1.25rem 1.35rem);
  background: var(--color-bg-base);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sga-tributos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.sga-tributos-title {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sga-tributos-total {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.sga-tributo-amount {
  font-size: 1rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-primary);
}

.sga-tributos-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.85rem;
}

.sga-tributo-box {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-bg-surface);
}

.sga-fixed-rate {
  padding: 0.45rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  background: var(--color-bg-base);
}

.sga-tributo-calc {
  display: flex;
  justify-content: space-between;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.sga-tributo-calc strong {
  color: var(--color-text-main);
}

.sga-total-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-bg-base);
}

.sga-total-kicker {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-text-main);
}

.sga-total-right {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
}

.sga-total-num {
  font-size: 1.35rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-primary);
}

.sga-total-curr {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.font-mono {
  font-family: var(--font-mono);
}

@media (max-width: 900px) {
  .sga-costs-grid,
  .sga-tributos-grid {
    grid-template-columns: 1fr;
  }
}
</style>
