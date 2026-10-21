<template>
  <aside class="form-sidebar-column">
    <!-- Tarjeta de Proforma Comercial en Vivo -->
    <div class="preview-proforma-card">
      <div class="card-ribbon">
        <i class="pi pi-file-edit"></i>
        <span>PROFORMA COMERCIAL OFICIAL</span>
      </div>

      <div class="proforma-code-row">
        <span class="doc-code-lg">{{ form.codigoCotizacion || 'COT-2026-BORRADOR' }}</span>
      </div>

      <div class="proforma-client-snippet">
        <span class="proforma-sub-label">CLIENTE DESTINATARIO:</span>
        <h4 class="proforma-client-name">
          {{ selectedClientObject ? selectedClientObject.razonSocial : 'Seleccione un cliente en Paso 1' }}
        </h4>
        <span v-if="selectedClientObject" class="proforma-client-doc">
          {{ selectedClientObject.tipoDocumento }}: {{ selectedClientObject.numeroDocumento }}
        </span>
      </div>

      <div class="proforma-details-list">
        <div class="proforma-row">
          <span class="p-label">Régimen Aduanero:</span>
          <span class="p-val">{{ getRegimenLabel ? getRegimenLabel(form.regimenAduanero) : form.regimenAduanero }}</span>
        </div>

        <div v-if="selectedPuertoEnvio || selectedPuertoDestino" class="proforma-row">
          <span class="p-label">Ruta Aduanera:</span>
          <span class="p-val">{{ selectedPuertoEnvio?.codigoPuerto || 'Origen' }} ➔ {{ selectedPuertoDestino?.codigoPuerto || 'Destino' }}</span>
        </div>

        <div v-if="selectedTipoCarga" class="proforma-row">
          <span class="p-label">Tipo de Carga:</span>
          <span class="p-val">{{ selectedTipoCarga.codigo }}</span>
        </div>

        <div v-if="selectedMercancia" class="proforma-row">
          <span class="p-label">Mercancía:</span>
          <div class="p-val-col">
            <span class="merc-code-text">{{ selectedMercancia.codigo }}</span>
            <span v-if="selectedMercancia.requierePermiso" class="badge-restringida-mini">
              Req: {{ selectedMercancia.entidadReguladora }}
            </span>
            <span v-else class="badge-libre-mini">Libre</span>
          </div>
        </div>

        <div class="proforma-row">
          <span class="p-label">Transporte SGA:</span>
          <span :class="['p-val', form.servicioTransporte ? 'text-accent font-semibold' : '']">
            {{ form.servicioTransporte ? (form.origenProvincia && form.destinoProvincia ? `✓ ${form.origenProvincia} → ${form.destinoProvincia}` : '✓ Interno SGA') : 'Externo' }}
          </span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Vigencia:</span>
          <span class="p-val">{{ form.validezDias }} Días Calendario</span>
        </div>

        <div class="proforma-divider"></div>

        <!-- 1. Base Imponible / Valoración (No entra al total de liquidación SGA) -->
        <div class="proforma-section-header">
          <span class="section-micro-title">VALORACIÓN ADUANERA (BASE)</span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Valor FOB (Mercancía):</span>
          <span class="p-val font-semibold">${{ formatVal(form.valorFob) }}</span>
        </div>

        <div v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'" class="proforma-row">
          <span class="p-label">Seguro Estimado:</span>
          <span class="p-val">${{ formatVal(form.montoSeguroEstimado) }}</span>
        </div>

        <div v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'" class="proforma-row cif-row">
          <span class="p-label">Valor CIF Base:</span>
          <span class="p-val text-accent font-bold">${{ formatVal((Number(form.valorFob) || 0) + (Number(form.montoFleteEstimado) || 0) + (Number(form.montoSeguroEstimado) || 0)) }}</span>
        </div>

        <div class="proforma-divider"></div>

        <!-- 2. Conceptos a Liquidar por Despacho y Logística SGA -->
        <div class="proforma-section-header">
          <span class="section-micro-title">CONCEPTOS A LIQUIDAR (SGA)</span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Flete Internacional:</span>
          <span class="p-val">${{ formatVal(form.montoFleteEstimado) }}</span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Gastos Op. & Agencia:</span>
          <span class="p-val">${{ formatVal(form.montoHonorariosEstimado) }}</span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Tributos SUNAT:</span>
          <span :class="['p-val', form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'text-success' : '']">
            {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Inafecto ($0.00)' : '$' + formatVal(form.montoTributosEstimado) }}
          </span>
        </div>

        <div v-if="form.servicioTransporte && Number(form.costoTransporteInterno) > 0" class="proforma-row">
          <span class="p-label">Flete Transporte Local:</span>
          <span class="p-val text-accent font-semibold">${{ formatVal(form.costoTransporteInterno) }}</span>
        </div>

        <div v-if="docsCount > 0" class="proforma-row">
          <span class="p-label">Documentos:</span>
          <span class="p-val text-info"><i class="pi pi-paperclip"></i> {{ docsCount }} adjunto(s)</span>
        </div>

        <div class="proforma-divider"></div>

        <!-- Total Liquidado (Excluye FOB de mercancía) -->
        <div class="proforma-total-container">
          <div class="proforma-row total-row">
            <span class="p-label-total">TOTAL ESTIMADO:</span>
            <span class="p-val-total">${{ formatVal(form.totalCotizado) }} USD</span>
          </div>
          <span class="total-fob-excluded-hint">
            <i class="pi pi-info-circle"></i>
            {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Flete + Gastos Op. (Excluye valor FOB)' : 'Flete + Gastos Op. + Tributos (Excluye valor FOB)' }}
          </span>
        </div>
      </div>

      <div class="card-footer-stamp">
        <i class="pi pi-check-circle"></i>
        <span>SISTEMA DE GESTIÓN ADUANERA • MÓDULO COMERCIAL</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
const props = defineProps({
  form: {
    type: Object,
    required: true
  },
  selectedClientObject: {
    type: Object,
    default: null
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
  isEditMode: {
    type: Boolean,
    default: false
  },
  submitting: {
    type: Boolean,
    default: false
  },
  isFormReady: {
    type: Boolean,
    default: false
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

defineEmits(['submit', 'cancel'])

const formatVal = (val) => {
  if (props.formatNumber) return props.formatNumber(val)
  return Number(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.form-sidebar-column {
  position: sticky;
  top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
}

.preview-proforma-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.card-ribbon {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--color-primary);
  background: var(--color-primary-subtle);
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-full, 9999px);
  margin-bottom: var(--space-3, 0.75rem);
  align-self: flex-start;
}

.proforma-code-row {
  margin-bottom: var(--space-3, 0.75rem);
}

.doc-code-lg {
  font-family: monospace;
  font-size: var(--font-size-md, 1rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.3rem 0.75rem;
  border-radius: var(--radius-md, 0.5rem);
  display: inline-block;
}

.proforma-client-snippet {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  margin-bottom: var(--space-4, 1rem);
  padding-bottom: var(--space-3, 0.75rem);
  border-bottom: 1px solid var(--color-border-subtle);
}

.proforma-sub-label {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.proforma-client-name {
  font-family: var(--font-sans);
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  line-height: 1.3;
}

.proforma-client-doc {
  font-family: var(--font-mono);
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-primary);
  font-weight: 700;
}

.proforma-details-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  margin-bottom: var(--space-5, 1.25rem);
}

.proforma-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-xs, 0.75rem);
  gap: var(--space-2, 0.5rem);
}

.p-label {
  color: var(--color-text-muted);
  font-weight: 600;
}

.p-val {
  color: var(--color-text-main);
  font-weight: 700;
  text-align: right;
}

.p-val-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.15rem;
}

.merc-code-text {
  font-weight: 700;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-main);
}

.badge-restringida-mini {
  font-size: var(--font-size-2xs, 0.65rem);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
  padding: 0.1rem 0.35rem;
  border-radius: var(--radius-sm, 0.25rem);
  font-weight: 700;
}

.badge-libre-mini {
  font-size: var(--font-size-2xs, 0.65rem);
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
  padding: 0.1rem 0.35rem;
  border-radius: var(--radius-sm, 0.25rem);
  font-weight: 700;
}

.text-accent {
  color: var(--color-primary) !important;
  font-weight: 700;
}

.proforma-divider {
  height: 1px;
  background: var(--color-border-subtle);
  margin: 0.35rem 0;
}

.proforma-section-header {
  padding: 0.2rem 0;
}

.section-micro-title {
  font-size: var(--font-size-2xs, 0.6rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
  text-transform: uppercase;
}

.proforma-total-container {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.total-row {
  padding-top: var(--space-1, 0.25rem);
}

.total-fob-excluded-hint {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-primary);
  font-weight: 600;
  background: var(--color-bg-subtle);
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm, 0.25rem);
  border: 1px dashed var(--color-border);
}

.p-label-total {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--color-text-main);
}

.p-val-total {
  font-family: var(--font-mono);
  font-size: var(--font-size-md, 1.15rem);
  font-weight: 800;
  color: var(--color-primary);
}

.card-footer-stamp {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
  font-weight: 700;
}
</style>
