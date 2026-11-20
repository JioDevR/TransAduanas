<template>
  <aside class="sga-proforma-aside">
    <div class="sga-proforma-card">
      <div class="sga-proforma-header">
        <span class="sga-proforma-code">{{ form.codigoCotizacion || 'COT - 2026 - BORRADOR' }}</span>
        <div class="sga-proforma-client">
          <span class="sga-micro-label">CLIENTE DESTINATARIO:</span>
          <p v-if="selectedClientObject" class="sga-client-name">
            {{ selectedClientObject.razonSocial }}
            <span class="sga-client-doc">({{ selectedClientObject.tipoDocumento }}: {{ selectedClientObject.numeroDocumento }})</span>
          </p>
        </div>
      </div>

      <div class="sga-proforma-divider"></div>

      <div class="sga-proforma-section">
        <div class="sga-proforma-row">
          <span class="sga-row-label">Régimen Aduanero:</span>
          <span class="sga-row-val">{{ regimenDisplay }}</span>
        </div>

        <div class="sga-proforma-row">
          <span class="sga-row-label">Modalidad de Transporte:</span>
          <span class="sga-row-val">{{ modalidadDisplay }}</span>
        </div>

        <div class="sga-proforma-row">
          <span class="sga-row-label">País de Origen:</span>
          <span class="sga-row-val">{{ paisOrigen || '-' }}</span>
        </div>

        <div class="sga-proforma-row">
          <span class="sga-row-label">País de Destino:</span>
          <span class="sga-row-val">{{ paisDestino || '-' }}</span>
        </div>

        <div v-if="selectedPuertoEnvio || selectedPuertoDestino" class="sga-proforma-row">
          <span class="sga-row-label">Ruta Aduanera:</span>
          <span class="sga-row-val font-mono">{{ selectedPuertoEnvio?.codigoPuerto || '-' }} → {{ selectedPuertoDestino?.codigoPuerto || '-' }}</span>
        </div>

        <div class="sga-proforma-row">
          <span class="sga-row-label">Tipo de Carga:</span>
          <span class="sga-row-val">{{ tipoCargaDisplay }}</span>
        </div>

        <div class="sga-proforma-row">
          <span class="sga-row-label">Mercancía:</span>
          <span class="sga-row-val">{{ mercanciaDisplay }}</span>
        </div>

        <div class="sga-proforma-row">
          <span class="sga-row-label">Transporte SGA:</span>
          <span class="sga-row-val">{{ transporteDisplay }}</span>
        </div>

        <div class="sga-proforma-row">
          <span class="sga-row-label">Vigencia:</span>
          <span class="sga-row-val">{{ form.validezDias ? `${form.validezDias} Días` : '-' }}</span>
        </div>

        <div v-if="docsCount > 0" class="sga-proforma-row">
          <span class="sga-row-label">Documentos:</span>
          <span class="sga-row-val">{{ docsCount }} adjunto(s)</span>
        </div>
      </div>

      <div class="sga-proforma-divider"></div>

      <div class="sga-proforma-section">
        <span class="sga-section-kicker">VALORACIÓN ADUANERA</span>

        <div class="sga-proforma-row">
          <span class="sga-row-label">Valor FOB (Mercancía):</span>
          <span class="sga-row-num">${{ formatVal(form.valorFob) }}</span>
        </div>

        <div v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'" class="sga-proforma-row">
          <span class="sga-row-label">Seguro Estimado:</span>
          <span class="sga-row-num">${{ formatVal(form.montoSeguroEstimado) }}</span>
        </div>

        <div v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'" class="sga-proforma-row">
          <span class="sga-row-label">Valor CIF Base:</span>
          <span class="sga-row-num is-strong">${{ formatVal(valorCifBase) }}</span>
        </div>
      </div>

      <div class="sga-proforma-divider"></div>

      <div class="sga-proforma-section">
        <span class="sga-section-kicker">CONCEPTOS A LIQUIDAR (SGA)</span>

        <div class="sga-proforma-row">
          <span class="sga-row-label">Flete Internacional:</span>
          <span class="sga-row-num">${{ formatVal(form.montoFleteEstimado) }}</span>
        </div>

        <div class="sga-proforma-row">
          <span class="sga-row-label">Gastos Op. &amp; Agencia:</span>
          <span class="sga-row-num">${{ formatVal(form.montoHonorariosEstimado) }}</span>
        </div>

        <div class="sga-proforma-row">
          <span class="sga-row-label">Tributos SUNAT:</span>
          <span class="sga-row-num">
            {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Inafecto ($0.00)' : `$${formatVal(form.montoTributosEstimado)}` }}
          </span>
        </div>

        <div v-if="form.servicioTransporte && Number(form.costoTransporteInterno) > 0" class="sga-proforma-row">
          <span class="sga-row-label">Flete Transporte Local:</span>
          <span class="sga-row-num">${{ formatVal(form.costoTransporteInterno) }}</span>
        </div>
      </div>

      <div class="sga-proforma-divider"></div>

      <div class="sga-proforma-total">
        <span class="sga-total-label">TOTAL ESTIMADO:</span>
        <div class="sga-total-value">
          <span class="sga-total-amount">${{ formatVal(form.totalCotizado) }}</span>
          <span class="sga-total-currency">USD</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  form: {
    type: Object,
    required: true
  },
  selectedClientObject: {
    type: Object,
    default: null
  },
  modalidadTransporte: {
    type: String,
    default: ''
  },
  paisOrigen: {
    type: String,
    default: ''
  },
  paisDestino: {
    type: String,
    default: ''
  },
  selectedPuertoEnvio: {
    type: Object,
    default: null
  },
  selectedPuertoDestino: {
    type: Object,
    default: null
  },
  selectedTipoCarga: {
    type: Object,
    default: null
  },
  selectedMercancia: {
    type: Object,
    default: null
  },
  getRegimenLabel: {
    type: Function,
    default: null
  },
  formatNumber: {
    type: Function,
    default: null
  },
  docsCount: {
    type: Number,
    default: 0
  }
})

const formatVal = (val) => {
  if (props.formatNumber) return props.formatNumber(val)
  return Number(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const valorCifBase = computed(() => {
  return (
    (Number(props.form.valorFob) || 0) +
    (Number(props.form.montoFleteEstimado) || 0) +
    (Number(props.form.montoSeguroEstimado) || 0)
  )
})

const regimenDisplay = computed(() => {
  if (!props.form.regimenAduanero) return '-'
  return props.getRegimenLabel ? props.getRegimenLabel(props.form.regimenAduanero) : props.form.regimenAduanero
})

const modalidadDisplay = computed(() => {
  if (!props.form.regimenAduanero && !props.selectedPuertoEnvio && !props.selectedPuertoDestino) {
    return '-'
  }
  if (props.modalidadTransporte === 'AEREO') return 'Carga Aérea'
  if (props.modalidadTransporte === 'MARITIMO') return 'Carga Marítima'
  return '-'
})

const tipoCargaDisplay = computed(() => {
  if (!props.selectedTipoCarga) return '-'
  return `${props.selectedTipoCarga.codigo} — ${props.selectedTipoCarga.nombre}`
})

const mercanciaDisplay = computed(() => {
  if (!props.selectedMercancia) return '-'
  return `${props.selectedMercancia.codigo} — ${props.selectedMercancia.nombre}`
})

const transporteDisplay = computed(() => {
  if (props.form.servicioTransporte) {
    if (props.form.origenProvincia && props.form.destinoProvincia) {
      return `${props.form.origenProvincia} → ${props.form.destinoProvincia}`
    }
    return 'Interno SGA'
  }
  return '-'
})
</script>

<style scoped>
.sga-proforma-aside {
  position: sticky;
  top: 1.5rem;
}

.sga-proforma-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--sga-card-radius, 14px);
  padding: var(--sga-card-padding-y, 1.75rem) var(--sga-card-padding-x, 2rem);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sga-proforma-header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sga-proforma-code {
  font-family: var(--font-mono);
  font-size: 0.9375rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-text-main);
}

.sga-proforma-client {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.sga-micro-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle);
}

.sga-client-name {
  margin: 0;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.sga-client-doc {
  font-weight: 400;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  font-size: 0.75rem;
}

.sga-proforma-divider {
  height: 1px;
  background: var(--color-border);
}

.sga-proforma-section {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.sga-section-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle);
  margin-bottom: 0.15rem;
}

.sga-proforma-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
  font-size: 0.8125rem;
}

.sga-row-label {
  color: var(--color-text-muted);
}

.sga-row-val {
  color: var(--color-text-main);
  text-align: right;
  font-weight: 500;
}

.sga-row-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  color: var(--color-text-muted);
  text-align: right;
}

.sga-row-num.is-strong {
  color: var(--color-text-main);
  font-weight: 700;
}

.sga-proforma-total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding-top: 0.25rem;
}

.sga-total-label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-text-main);
}

.sga-total-value {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
}

.sga-total-amount {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sga-total-currency {
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--color-text-subtle);
}

.font-mono {
  font-family: var(--font-mono);
}
</style>
