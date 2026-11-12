<template>
  <aside class="form-sidebar-column">
    <!-- Tarjeta de Proforma de Solicitud B2B -->
    <div class="preview-proforma-card">
      <div class="card-ribbon">
        <i class="pi pi-file-edit"></i>
        <span>PROFORMA DE SOLICITUD B2B</span>
      </div>

      <div class="proforma-code-row">
        <span class="doc-code-lg">BORRADOR B2B</span>
      </div>

      <div class="proforma-client-snippet">
        <span class="proforma-sub-label">EMPRESA SOLICITANTE:</span>
        <h4 class="proforma-client-name">
          {{ empresa?.razonSocial || empresa?.usuarioNombre || 'Cliente B2B' }}
        </h4>
        <span class="proforma-client-doc">
          RUC: {{ empresa?.ruc || '—' }}
        </span>
      </div>

      <div class="proforma-details-list">
        <div class="proforma-row">
          <span class="p-label">Régimen Aduanero:</span>
          <span class="p-val">{{ getRegimenText(form?.regimenAduanero) }}</span>
        </div>

        <div v-if="selectedPuertoEnvio || selectedPuertoDestino" class="proforma-row">
          <span class="p-label">Ruta Aduanera:</span>
          <span class="p-val">
            {{ selectedPuertoEnvio?.codigoPuerto || selectedPuertoEnvio?.nombrePuerto || 'Origen' }}
            ➔
            {{ selectedPuertoDestino?.codigoPuerto || selectedPuertoDestino?.nombrePuerto || 'Destino' }}
          </span>
        </div>

        <div v-if="selectedTipoCarga" class="proforma-row">
          <span class="p-label">Tipo de Carga:</span>
          <span class="p-val">{{ selectedTipoCarga.codigo || selectedTipoCarga.nombre }}</span>
        </div>

        <div v-if="selectedMercancia" class="proforma-row">
          <span class="p-label">Mercancía:</span>
          <div class="p-val-col">
            <span class="merc-code-text">{{ selectedMercancia.codigo }}</span>
            <span v-if="selectedMercancia.requierePermiso" class="badge-restringida-mini">
              Req: {{ selectedMercancia.entidadReguladora || 'Permiso' }}
            </span>
            <span v-else class="badge-libre-mini">Libre</span>
          </div>
        </div>

        <div class="proforma-row">
          <span class="p-label">Transporte SGA:</span>
          <span :class="['p-val', form?.servicioTransporte ? 'text-accent font-semibold' : '']">
            {{ form?.servicioTransporte ? (form.origenProvincia && form.destinoProvincia ? `✓ ${form.origenProvincia} → ${form.destinoProvincia}` : '✓ Interno SGA') : 'No requerido' }}
          </span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Vigencia:</span>
          <span class="p-val">{{ form?.validezDias || 15 }} Días Calendario</span>
        </div>

        <div v-if="docsCount > 0" class="proforma-row">
          <span class="p-label">Documentos:</span>
          <span class="p-val text-info"><i class="pi pi-paperclip"></i> {{ docsCount }} adjunto(s)</span>
        </div>

        <div class="proforma-divider"></div>

        <!-- 1. VALORES DECLARADOS POR CLIENTE -->
        <div class="proforma-section-header">
          <span class="section-micro-title">VALORES DECLARADOS POR CLIENTE</span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Valor FOB (Mercancía):</span>
          <span class="p-val font-semibold">
            {{ Number(form?.valorFob) > 0 ? '$' + formatVal(form?.valorFob) : 'Por cotizar ($0.00)' }}
          </span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Flete Internacional:</span>
          <span class="p-val font-semibold">
            {{ Number(form?.montoFleteEstimado) > 0 ? '$' + formatVal(form?.montoFleteEstimado) : 'Por cotizar ($0.00)' }}
          </span>
        </div>

        <div class="proforma-divider"></div>

        <!-- 2. LIQUIDACIÓN COMERCIAL (ASIGNADA POR ASESOR) -->
        <div class="proforma-section-header">
          <span class="section-micro-title">LIQUIDACIÓN COMERCIAL (ASIGNADA POR ASESOR)</span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Seguro Estimado:</span>
          <span class="p-val text-muted-val">Por asignar ($0.00)</span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Gastos Op. & Agencia:</span>
          <span class="p-val text-muted-val">Por asignar ($0.00)</span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Tributos SUNAT:</span>
          <span v-if="form?.regimenAduanero === 'EXPORTACION_DEFINITIVA'" class="p-val text-success">
            Inafecto ($0.00)
          </span>
          <span v-else class="p-val text-muted-val">
            Por asignar ($0.00)
          </span>
        </div>

        <div v-if="form?.servicioTransporte" class="proforma-row">
          <span class="p-label">Flete Transporte Local:</span>
          <span class="p-val text-muted-val">Por asignar ($0.00)</span>
        </div>

        <div class="proforma-divider"></div>

        <!-- Total Liquidado B2B -->
        <div class="proforma-total-container">
          <div class="proforma-row total-row">
            <span class="p-label-total">TOTAL ESTIMADO:</span>
            <span class="p-val-total">Pendiente de liquidación ($0.00)</span>
          </div>
          <span class="total-fob-excluded-hint">
            <i class="pi pi-info-circle"></i>
            Liquidación comercial y tributos aduaneros pendientes de revisión técnica por asesor comercial.
          </span>
        </div>
      </div>

      <div class="card-footer-stamp">
        <i class="pi pi-shield"></i>
        <span>PORTAL B2B • PROPUESTA DE COTIZACIÓN</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { formatCurrency, formatRegimen } from '@/shared/utils/formatters'

const props = defineProps({
  form: {
    type: Object,
    required: true
  },
  empresa: {
    type: Object,
    default: () => ({})
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
  docsCount: {
    type: Number,
    default: 0
  },
  getRegimenLabel: {
    type: Function,
    default: null
  },
  formatNumber: {
    type: Function,
    default: null
  }
})

const getRegimenText = (regimen) => {
  if (props.getRegimenLabel) return props.getRegimenLabel(regimen)
  return formatRegimen(regimen) || '—'
}

const formatVal = (val) => {
  if (props.formatNumber) return props.formatNumber(val)
  return formatCurrency(val)
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
  box-shadow: var(--shadow-subtle);
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
  background: var(--color-bg-subtle);
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-full, 9999px);
  margin-bottom: var(--space-3, 0.75rem);
  align-self: flex-start;
}

.proforma-code-row {
  margin-bottom: var(--space-3, 0.75rem);
}

.doc-code-lg {
  font-family: var(--font-mono, monospace);
  font-size: var(--font-size-md, 0.9375rem);
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

.text-muted-val {
  color: var(--color-text-subtle) !important;
  font-weight: 600 !important;
  font-style: italic;
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

.text-info {
  color: var(--color-info-text, #21598f);
  font-weight: 600;
}

.text-success {
  color: var(--color-success-text, #1e6638);
  font-weight: 600;
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
  align-items: flex-start;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
  font-weight: 500;
  background: var(--color-bg-subtle);
  padding: 0.35rem 0.5rem;
  border-radius: var(--radius-sm, 0.25rem);
  border: 1px dashed var(--color-border);
  line-height: 1.35;
}

.total-fob-excluded-hint i {
  color: var(--color-primary);
  margin-top: 0.15rem;
  flex-shrink: 0;
}

.p-label-total {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--color-text-main);
}

.p-val-total {
  font-family: var(--font-sans);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  text-align: right;
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
