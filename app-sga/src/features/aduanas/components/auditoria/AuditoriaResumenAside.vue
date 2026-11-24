<template>
  <div class="sga-audit-aside-stack">
    <!-- Tarjeta Proforma Oficial Resumida -->
    <div class="sga-card-surface sga-proforma-card">
      <div class="sga-proforma-top">
        <span class="sga-kicker">PROFORMA AUDITADA</span>
        <span class="sga-proforma-code font-mono">{{ data.codigoPrincipal }}</span>
      </div>

      <div class="sga-proforma-total-box">
        <span class="sga-micro-label">TOTAL COTIZADO OPERACIÓN</span>
        <span class="sga-proforma-total font-mono">${{ formatUsd(data.totalCotizado) }}</span>
        <span class="sga-proforma-currency">Dólares Americanos (USD)</span>
      </div>

      <div class="sga-proforma-stats">
        <div class="sga-stat-row">
          <span class="sga-stat-label">Valor CIF Estimado</span>
          <span class="sga-stat-val font-mono">${{ formatUsd(liquidacion.valorCif) }}</span>
        </div>
        <div class="sga-stat-row">
          <span class="sga-stat-label">Tributos SUNAT (Est.)</span>
          <span class="sga-stat-val font-mono">${{ formatUsd(liquidacion.totalTributos) }}</span>
        </div>
        <div class="sga-stat-row">
          <span class="sga-stat-label">Honorarios SGA</span>
          <span class="sga-stat-val font-mono">${{ formatUsd(data.montoHonorariosEstimado) }}</span>
        </div>
        <div class="sga-stat-row">
          <span class="sga-stat-label">Canal SUNAT</span>
          <Tag
            :value="data.canalSunat || 'PENDIENTE'"
            :severity="getCanalSeverity(data.canalSunat)"
            rounded
          />
        </div>
      </div>
    </div>

    <!-- Checklist Dinámico de Auditoría Técnica -->
    <div class="sga-card-surface sga-checklist-card">
      <div class="sga-checklist-head">
        <div class="sga-checklist-title-wrap">
          <i class="pi pi-check-square"></i>
          <h3 class="sga-checklist-title">Checklist Normativo</h3>
        </div>
        <Tag
          :value="`${passedCount}/${checklist.length}`"
          :severity="passedCount === checklist.length ? 'success' : 'warn'"
          class="font-mono"
          rounded
        />
      </div>

      <p class="sga-checklist-desc">
        Criterios evaluados automáticamente sobre la ficha técnica y el archivo documental:
      </p>

      <ul class="sga-checklist-list">
        <li
          v-for="item in checklist"
          :key="item.id"
          class="sga-checklist-item"
        >
          <i
            :class="[
              'pi sga-check-icon',
              item.valid ? 'pi-check-circle sga-text-success' : 'pi-exclamation-circle sga-text-warn'
            ]"
          ></i>
          <div class="sga-check-content">
            <span class="sga-check-label">{{ item.label }}</span>
            <span class="sga-check-detail font-mono">{{ item.detail }}</span>
          </div>
        </li>
      </ul>

      <div class="sga-checklist-stamp">
        <i class="pi pi-verified"></i>
        <div>
          <strong>Supervisión Aduanera SGA</strong>
          <small>Validación de conformidad normativa SUNAT</small>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'
import { formatUsd, getCanalSeverity } from '../../utils/aduanasFormatters'

const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  liquidacion: {
    type: Object,
    required: true
  },
  checklist: {
    type: Array,
    default: () => []
  }
})

const passedCount = computed(() => props.checklist.filter((item) => item.valid).length)
</script>

<style scoped>
.sga-audit-aside-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-proforma-card,
.sga-checklist-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sga-proforma-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.sga-proforma-code {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sga-proforma-total-box {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 1rem 1.15rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
}

.sga-proforma-total {
  font-size: 1.6rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
  color: var(--color-text-main);
}

.sga-proforma-currency {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-proforma-stats {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}

.sga-stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  font-size: 0.85rem;
}

.sga-stat-label {
  color: var(--color-text-muted);
}

.sga-stat-val {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-main);
}

.sga-checklist-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.sga-checklist-title-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-text-main);
}

.sga-checklist-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}

.sga-checklist-desc {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-subtle);
  line-height: 1.45;
}

.sga-checklist-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.sga-checklist-item {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  font-size: 0.8125rem;
  color: var(--color-text-main);
}

.sga-check-icon {
  margin-top: 0.15rem;
  flex-shrink: 0;
}

.sga-check-content {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  min-width: 0;
}

.sga-check-label {
  line-height: 1.35;
}

.sga-check-detail {
  font-size: 0.72rem;
  color: var(--color-text-subtle);
}

.sga-checklist-stamp {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem 0.9rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.sga-checklist-stamp strong,
.sga-checklist-stamp small {
  display: block;
}
</style>
