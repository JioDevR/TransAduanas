<template>
  <section class="sga-card-surface cobros-card" aria-label="Resumen financiero y cobros">
    <div class="sga-chart-head">
      <div>
        <span class="sga-kicker">COBRANZAS Y ESTADO FINANCIERO // USD</span>
        <h3 class="sga-chart-title">Cobros, Amortización y Obligaciones Pendientes</h3>
      </div>

      <div class="cobros-head-actions">
        <Button
          label="Registrar Pago"
          icon="pi pi-plus"
          size="small"
          @click="$emit('navigate', '/pagos/registrar')"
        />
        <Button
          label="Ir a Cobros"
          icon="pi pi-arrow-right"
          iconPos="right"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('navigate', '/pagos')"
        />
      </div>
    </div>

    <!-- Alerta de vouchers observados -->
    <div v-if="stats.pagosObservados > 0" class="sga-banner is-danger">
      <i class="pi pi-exclamation-triangle"></i>
      <div class="banner-text">
        <strong>{{ stats.pagosObservados }} comprobante(s) observados o rechazados por Finanzas.</strong>
        <span>Revise el motivo y subsane el abono para evitar retrasos en su despacho.</span>
      </div>
      <Button
        label="Subsanar ahora"
        severity="danger"
        size="small"
        @click="$emit('navigate', '/pagos?tab=observados')"
      />
    </div>

    <!-- Métricas de amortización + gráfico de barras / progreso -->
    <div class="cobros-grid">
      <div class="cobros-summary-panel">
        <div class="money-row">
          <div class="money-stat">
            <span class="sga-kicker">TOTAL CONTRATADO</span>
            <strong class="money-val sga-mono">$ {{ formatCurrency(stats.totalCotizadoUsd) }}</strong>
          </div>
          <div class="money-stat">
            <span class="sga-kicker">TOTAL AMORTIZADO</span>
            <strong class="money-val is-success sga-mono">$ {{ formatCurrency(stats.totalAbonadoUsd) }}</strong>
          </div>
          <div class="money-stat">
            <span class="sga-kicker">SALDO PENDIENTE</span>
            <strong class="money-val is-warn sga-mono">$ {{ formatCurrency(stats.saldoPendienteUsd) }}</strong>
          </div>
        </div>

        <div class="progress-block">
          <div class="progress-header">
            <span>Progreso de amortización</span>
            <strong class="sga-mono">{{ stats.amortizacionPct }}%</strong>
          </div>
          <div class="progress-track" role="progressbar" :aria-valuenow="stats.amortizacionPct" aria-valuemin="0" aria-valuemax="100">
            <div class="progress-fill" :style="{ width: `${stats.amortizacionPct}%` }"></div>
          </div>
        </div>

        <div class="vouchers-chips">
          <span class="v-chip is-success">
            <i class="pi pi-check-circle"></i>
            Validados: {{ stats.pagosValidados }}
          </span>
          <span class="v-chip is-info">
            <i class="pi pi-clock"></i>
            En revisión: {{ stats.pagosEnRevision }}
          </span>
          <span v-if="stats.pagosObservados > 0" class="v-chip is-danger">
            <i class="pi pi-exclamation-circle"></i>
            Observados: {{ stats.pagosObservados }}
          </span>
        </div>
      </div>

      <div class="obligaciones-panel">
        <div class="obligaciones-head">
          <span class="sga-kicker">PRÓXIMAS OBLIGACIONES POR PAGAR</span>
          <span class="ob-count sga-mono">{{ stats.totalObligacionesPendientes }}</span>
        </div>

        <div v-if="stats.proximasObligaciones.length === 0" class="obligaciones-empty">
          <i class="pi pi-verified"></i>
          <span>No registra saldos pendientes por abonar.</span>
        </div>

        <ul v-else class="obligaciones-list">
          <li
            v-for="ob in stats.proximasObligaciones"
            :key="`${ob.tipoObligacion}-${ob.idReferencia}`"
            class="ob-row"
          >
            <div class="ob-main">
              <span class="ob-code sga-mono">{{ ob.codigo || ob.codigoCotizacion || ob.codigoExpediente }}</span>
              <span class="ob-desc">{{ ob.descripcion || 'Anticipo de servicio aduanero' }}</span>
            </div>

            <div class="ob-right">
              <div class="ob-amount-wrap">
                <span class="ob-amount sga-mono">$ {{ formatCurrency(ob.saldoPendienteUsd) }}</span>
                <span class="ob-status">{{ formatEstadoPago(ob.estadoPago) }}</span>
              </div>
              <Button
                label="Abonar"
                size="small"
                outlined
                @click="$emit('navigate', '/pagos/registrar')"
              />
            </div>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup>
import Button from 'primevue/button'
import { formatCurrency, formatEstadoPago } from '@/shared/utils/formatters'

defineProps({
  stats: { type: Object, required: true }
})

defineEmits(['navigate'])
</script>

<style scoped>
.cobros-card {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.cobros-head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.banner-text {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  flex: 1;
}

.banner-text span {
  font-size: 0.78rem;
  opacity: 0.9;
}

.cobros-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  gap: 1.25rem;
}

.cobros-summary-panel,
.obligaciones-panel {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: 1.1rem 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.money-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.money-stat {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.money-val {
  font-size: 1.15rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-main);
}

.money-val.is-success {
  color: var(--color-success-text);
}

.money-val.is-warn {
  color: var(--color-warning-text);
}

.progress-block {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.progress-header strong {
  color: var(--color-text-main);
}

.progress-track {
  height: 9px;
  border-radius: var(--radius-pill);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, var(--color-primary), var(--color-success-text));
  transition: width 0.35s ease;
}

.vouchers-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.v-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.22rem 0.6rem;
  border-radius: var(--radius-pill);
  border: 1px solid transparent;
}

.v-chip.is-success {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.v-chip.is-info {
  background: var(--color-info-bg);
  border-color: var(--color-info-border);
  color: var(--color-info-text);
}

.v-chip.is-danger {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.obligaciones-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ob-count {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: var(--radius-pill);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
}

.obligaciones-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.25rem;
  font-size: 0.82rem;
  color: var(--color-success-text);
}

.obligaciones-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ob-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.6rem 0.8rem;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
}

.ob-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.ob-code {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.ob-desc {
  font-size: 0.74rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ob-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.ob-amount-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.ob-amount {
  font-size: 0.82rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-main);
}

.ob-status {
  font-size: 0.66rem;
  font-weight: 600;
  color: var(--color-warning-text);
}

@media (max-width: 960px) {
  .cobros-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .money-row {
    grid-template-columns: 1fr;
  }

  .ob-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .ob-right {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
