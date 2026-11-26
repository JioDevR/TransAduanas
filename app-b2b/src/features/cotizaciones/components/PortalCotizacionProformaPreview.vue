<template>
  <aside class="sga-split-aside">
    <div class="sga-card-surface proforma-card">
      <div class="proforma-top">
        <span class="sga-kicker">PROFORMA DE SOLICITUD</span>
        <Tag value="BORRADOR B2B" severity="secondary" class="sga-mini-tag sga-mono" />
      </div>

      <div class="proforma-client">
        <span class="sga-micro-label">EMPRESA SOLICITANTE</span>
        <h4 class="proforma-client-name">
          {{ empresa?.razonSocial || empresa?.usuarioNombre || 'Cliente B2B' }}
        </h4>
        <span class="sga-mono proforma-ruc">RUC: {{ empresa?.ruc || '—' }}</span>
      </div>

      <div class="proforma-list">
        <div class="proforma-row">
          <span class="p-label">Régimen</span>
          <span class="p-val">{{ getRegimenText(form?.regimenAduanero) }}</span>
        </div>

        <div v-if="selectedPuertoEnvio || selectedPuertoDestino" class="proforma-row">
          <span class="p-label">Ruta</span>
          <span class="p-val sga-mono">
            {{ selectedPuertoEnvio?.codigoPuerto || 'Origen' }} → {{ selectedPuertoDestino?.codigoPuerto || 'Destino' }}
          </span>
        </div>

        <div v-if="selectedTipoCarga" class="proforma-row">
          <span class="p-label">Tipo de Carga</span>
          <span class="p-val">{{ selectedTipoCarga.codigo || selectedTipoCarga.nombre }}</span>
        </div>

        <div v-if="selectedMercancia" class="proforma-row">
          <span class="p-label">Partida</span>
          <span class="p-val sga-mono">{{ selectedMercancia.codigo }}</span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Transporte SGA</span>
          <span class="p-val">
            {{ form?.servicioTransporte ? (form.origenProvincia && form.destinoProvincia ? `${form.origenProvincia} → ${form.destinoProvincia}` : 'Incluido') : 'No requerido' }}
          </span>
        </div>

        <div class="proforma-row">
          <span class="p-label">Vigencia</span>
          <span class="p-val sga-mono">{{ form?.validezDias || 15 }} días</span>
        </div>

        <div v-if="docsCount > 0" class="proforma-row">
          <span class="p-label">Documentos</span>
          <span class="p-val sga-text-info">{{ docsCount }} adjunto(s)</span>
        </div>
      </div>

      <div class="proforma-divider"></div>

      <div class="proforma-list">
        <span class="sga-micro-label">VALORES DECLARADOS</span>
        <div class="proforma-row">
          <span class="p-label">Valor FOB</span>
          <span class="p-val sga-mono">
            {{ Number(form?.valorFob) > 0 ? `$ ${formatVal(form?.valorFob)}` : 'Por declarar' }}
          </span>
        </div>
        <div class="proforma-row">
          <span class="p-label">Flete Internacional</span>
          <span class="p-val sga-mono">
            {{ Number(form?.montoFleteEstimado) > 0 ? `$ ${formatVal(form?.montoFleteEstimado)}` : 'Por cotizar' }}
          </span>
        </div>
      </div>

      <div class="proforma-divider"></div>

      <div class="proforma-total-box">
        <span class="sga-micro-label">LIQUIDACIÓN COMERCIAL</span>
        <div class="proforma-total-row">
          <span>Total Estimado</span>
          <strong class="sga-mono">Por liquidar</strong>
        </div>
        <p class="proforma-hint">
          Seguro, gastos de agencia y tributos SUNAT serán liquidados por su asesor comercial.
        </p>
      </div>
    </div>
  </aside>
</template>

<script setup>
import Tag from 'primevue/tag'
import { formatCurrency, formatRegimen } from '@/shared/utils/formatters'

const props = defineProps({
  form: { type: Object, required: true },
  empresa: { type: Object, default: () => ({}) },
  selectedPuertoEnvio: { type: Object, default: null },
  selectedPuertoDestino: { type: Object, default: null },
  selectedTipoCarga: { type: Object, default: null },
  selectedMercancia: { type: Object, default: null },
  docsCount: { type: Number, default: 0 },
  getRegimenLabel: { type: Function, default: null },
  formatNumber: { type: Function, default: null }
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
.proforma-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.proforma-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.proforma-client {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.proforma-client-name {
  margin: 0;
  font-size: 0.94rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.proforma-ruc {
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--color-accent-kicker);
}

.proforma-list {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.proforma-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
  font-size: 0.8rem;
}

.p-label {
  color: var(--color-text-muted);
}

.p-val {
  color: var(--color-text-main);
  font-weight: 600;
  text-align: right;
}

.proforma-divider {
  height: 1px;
  background: var(--color-border-subtle);
}

.proforma-total-box {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.8rem 0.9rem;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.proforma-total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.proforma-hint {
  margin: 0;
  font-size: 0.72rem;
  line-height: 1.4;
  color: var(--color-text-muted);
}
</style>
