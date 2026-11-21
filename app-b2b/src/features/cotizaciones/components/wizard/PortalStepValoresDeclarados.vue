<template>
  <section class="sga-card-surface wizard-step-card">
    <div class="two-col-grid">
      <div class="sga-form-field">
        <label for="portal-fob" class="sga-form-label">Valor de Mercancía (FOB Factura) USD *</label>
        <InputNumber
          id="portal-fob"
          v-model="form.valorFob"
          mode="currency"
          currency="USD"
          locale="en-US"
          :min="0"
          :minFractionDigits="2"
          :maxFractionDigits="2"
          size="small"
          fluid
          :disabled="submitting"
        />
        <span class="field-hint">Monto de la factura comercial o proforma del proveedor.</span>
      </div>

      <div class="sga-form-field">
        <label for="portal-flete" class="sga-form-label">Flete Internacional Estimado (USD)</label>
        <InputNumber
          id="portal-flete"
          v-model="form.montoFleteEstimado"
          mode="currency"
          currency="USD"
          locale="en-US"
          :min="0"
          :minFractionDigits="2"
          :maxFractionDigits="2"
          size="small"
          fluid
          :disabled="submitting"
        />
        <span class="field-hint">Flete marítimo o aéreo estimado (opcional).</span>
      </div>
    </div>

    <div class="advisory-block">
      <div class="advisory-head">
        <i class="pi pi-lock"></i>
        <span class="sga-kicker">CONCEPTOS LIQUIDADOS POR ASESOR COMERCIAL SGA</span>
      </div>
      <p class="advisory-sub">
        Los siguientes rubros serán calculados conforme a tarifas corporativas, subpartida nacional y pólizas al recibir su solicitud:
      </p>

      <div class="advisory-grid">
        <div class="advisory-card">
          <span class="sga-micro-label">Seguro de Transporte</span>
          <strong class="sga-mono advisory-amount">$ 0.00</strong>
          <span class="advisory-note">Asignado por asesor según póliza aplicable</span>
        </div>

        <div class="advisory-card">
          <span class="sga-micro-label">Gastos Operativos & Agencia</span>
          <strong class="sga-mono advisory-amount">$ 0.00</strong>
          <span class="advisory-note">Comisión de despacho y gastos portuarios</span>
        </div>

        <div v-if="form.servicioTransporte" class="advisory-card">
          <span class="sga-micro-label">Flete Terrestre SGA</span>
          <strong class="sga-mono advisory-amount">$ 0.00</strong>
          <span class="advisory-note">{{ form.origenProvincia }} → {{ form.destinoProvincia }}</span>
        </div>

        <div class="advisory-card">
          <span class="sga-micro-label">Tributos SUNAT</span>
          <strong class="sga-mono advisory-amount">$ 0.00</strong>
          <span class="advisory-note">
            {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Inafecto en exportación' : 'Ad-Valórem, IGV e IPM según partida' }}
          </span>
        </div>
      </div>
    </div>

    <div class="sga-banner is-info">
      <i class="pi pi-shield"></i>
      <div>
        <strong>Evaluación Comercial Garantizada:</strong>
        TransAduanas revisará su solicitud en un plazo máximo de 24 horas hábiles y emitirá la liquidación oficial para su autorización.
      </div>
    </div>

    <div class="step-footer-actions">
      <Button
        type="button"
        label="Anterior: Documentación"
        icon="pi pi-arrow-left"
        severity="secondary"
        outlined
        size="small"
        :disabled="submitting"
        @click="$emit('prev')"
      />
      <Button
        type="button"
        label="Enviar Solicitud"
        icon="pi pi-send"
        size="small"
        :loading="submitting"
        :disabled="!isFormReady"
        @click="$emit('submit')"
      />
    </div>
  </section>
</template>

<script setup>
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'

defineProps({
  form: { type: Object, required: true },
  submitting: { type: Boolean, default: false },
  isFormReady: { type: Boolean, default: false }
})

defineEmits(['prev', 'submit'])
</script>

<style scoped>
.wizard-step-card {
  display: flex;
  flex-direction: column;
  gap: 1.35rem;
}

.two-col-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.field-hint {
  font-size: 0.74rem;
  color: var(--color-text-muted);
}

.advisory-block {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: 1.1rem 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.advisory-head {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  color: var(--color-accent-kicker);
}

.advisory-sub {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.advisory-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem;
}

.advisory-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.8rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.advisory-amount {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.advisory-note {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.step-footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}

@media (max-width: 768px) {
  .two-col-grid {
    grid-template-columns: 1fr;
  }
}
</style>
