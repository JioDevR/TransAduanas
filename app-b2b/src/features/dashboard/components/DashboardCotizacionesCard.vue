<template>
  <article class="sga-card-surface sga-chart-card">
    <div class="sga-chart-head">
      <div>
        <span class="sga-kicker">GESTIÓN COMERCIAL</span>
        <h3 class="sga-chart-title">Cotizaciones por Estado</h3>
      </div>
      <Button
        label="Ver todas"
        icon="pi pi-arrow-right"
        iconPos="right"
        severity="secondary"
        text
        size="small"
        @click="$emit('navigate', '/cotizaciones')"
      />
    </div>

    <div v-if="stats.total > 0" class="chart-and-legend">
      <div class="sga-chart-box sga-doughnut-wrap">
        <Chart
          type="doughnut"
          :data="chartData"
          :options="chartOptions"
          class="sga-chart-canvas"
        />
        <div class="sga-doughnut-center" aria-hidden="true">
          <span class="sga-doughnut-pct">{{ stats.aprobacionPct }}%</span>
          <span class="sga-doughnut-sub">Aprobadas</span>
        </div>
      </div>

      <div class="status-pills-row">
        <span class="state-chip is-warning">Pendientes: {{ stats.pendiente }}</span>
        <span class="state-chip is-success">Aprobadas: {{ stats.aprobada }}</span>
        <span class="state-chip is-danger">Observadas: {{ stats.observada }}</span>
        <span v-if="stats.rechazada > 0" class="state-chip is-neutral">
          Rechazadas: {{ stats.rechazada }}
        </span>
      </div>
    </div>

    <div v-else class="empty-block">
      <i class="pi pi-file-edit"></i>
      <p>Aún no tiene cotizaciones registradas.</p>
      <Button
        label="Solicitar primera cotización"
        size="small"
        outlined
        @click="$emit('navigate', '/cotizaciones')"
      />
    </div>

    <!-- Últimas cotizaciones -->
    <div v-if="stats.recientes.length > 0" class="recent-section">
      <span class="sga-kicker">ÚLTIMAS COTIZACIONES</span>
      <ul class="recent-list">
        <li
          v-for="cot in stats.recientes"
          :key="cot.idCotizacion"
          class="recent-row"
          tabindex="0"
          role="button"
          @click="$emit('navigate', `/cotizaciones/${cot.idCotizacion}`)"
          @keydown.enter.prevent="$emit('navigate', `/cotizaciones/${cot.idCotizacion}`)"
        >
          <div class="recent-row-main">
            <span class="recent-code sga-mono">{{ cot.codigoCotizacion }}</span>
            <span class="recent-sub">{{ formatRegimen(cot.regimenAduanero) || cot.nombreMercancia || 'Servicio aduanero' }}</span>
          </div>
          <div class="recent-row-right">
            <span class="recent-amount sga-mono">$ {{ formatCurrency(cot.totalCotizado) }}</span>
            <span :class="['mini-badge', getCotizacionStatusClass(cot.estado)]">
              {{ formatEstadoCotizacion(cot.estado) }}
            </span>
          </div>
        </li>
      </ul>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import { useTheme } from '@/shared/composables/useTheme'
import {
  formatCurrency,
  formatRegimen,
  formatEstadoCotizacion,
  getCotizacionStatusClass
} from '@/shared/utils/formatters'
import { getPortalChartPalette, buildDoughnutOptions } from '../composables/portalChartTheme'

const props = defineProps({
  stats: { type: Object, required: true }
})

defineEmits(['navigate'])

const { isDark } = useTheme()

const palette = computed(() => getPortalChartPalette(isDark.value))
const chartOptions = computed(() => buildDoughnutOptions(palette.value))

const chartData = computed(() => ({
  labels: ['Pendientes', 'Aprobadas', 'Observadas', 'Rechazadas'],
  datasets: [
    {
      data: [
        props.stats.pendiente,
        props.stats.aprobada,
        props.stats.observada,
        props.stats.rechazada
      ],
      backgroundColor: [
        palette.value.warning,
        palette.value.success,
        palette.value.danger,
        palette.value.muted
      ],
      borderColor: palette.value.surfaceBorder,
      borderWidth: 2
    }
  ]
}))
</script>

<style scoped>
.chart-and-legend {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.status-pills-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.state-chip {
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.2rem 0.6rem;
  border-radius: var(--radius-pill);
  border: 1px solid transparent;
}

.state-chip.is-warning {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.state-chip.is-success {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.state-chip.is-danger {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.state-chip.is-neutral {
  background: var(--color-bg-subtle);
  border-color: var(--color-border);
  color: var(--color-text-muted);
}

.empty-block {
  padding: 2rem 1rem;
  text-align: center;
  color: var(--color-text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.65rem;
}

.empty-block i {
  font-size: 1.65rem;
  color: var(--color-accent-kicker);
}

.empty-block p {
  margin: 0;
  font-size: 0.84rem;
}

.recent-section {
  border-top: 1px solid var(--color-border-subtle);
  padding-top: 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.recent-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.recent-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.55rem 0.75rem;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  cursor: pointer;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}

.recent-row:hover {
  border-color: var(--color-primary);
  background: var(--color-bg-muted);
}

.recent-row-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.recent-code {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.recent-sub {
  font-size: 0.74rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-row-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.2rem;
  flex-shrink: 0;
}

.recent-amount {
  font-size: 0.78rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-main);
}

.mini-badge {
  font-size: 0.64rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: var(--radius-pill);
  text-transform: uppercase;
}

.mini-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.mini-badge.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.mini-badge.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
}

.mini-badge.neutral {
  background: var(--color-bg-surface);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}
</style>
