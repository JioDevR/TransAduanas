<template>
  <article class="sga-card-surface sga-chart-card">
    <div class="sga-chart-head">
      <div>
        <span class="sga-kicker">OPERACIONES Y TRACKING DAM</span>
        <h3 class="sga-chart-title">Expedientes Aduaneros por Etapa</h3>
      </div>
      <Button
        label="Ver tablero"
        icon="pi pi-arrow-right"
        iconPos="right"
        severity="secondary"
        text
        size="small"
        @click="$emit('navigate', '/expedientes')"
      />
    </div>

    <div v-if="stats.total > 0" class="chart-and-canales">
      <div class="sga-chart-box">
        <Chart
          type="bar"
          :data="chartData"
          :options="chartOptions"
          class="sga-chart-canvas"
        />
      </div>

      <div class="canal-row">
        <span class="canal-label">Canales SUNAT:</span>
        <span class="canal-chip is-green">Verde: {{ stats.canales.VERDE }}</span>
        <span class="canal-chip is-orange">Naranja: {{ stats.canales.NARANJA }}</span>
        <span class="canal-chip is-red">Rojo: {{ stats.canales.ROJO }}</span>
        <span v-if="stats.canales.PENDIENTE > 0" class="canal-chip is-muted">
          Pend.: {{ stats.canales.PENDIENTE }}
        </span>
      </div>
    </div>

    <div v-else class="empty-block">
      <i class="pi pi-box"></i>
      <p>No tiene expedientes aduaneros aperturados todavía.</p>
      <span class="empty-sub">Se generarán automáticamente al aprobarse su anticipo de cotización.</span>
    </div>

    <!-- Últimos expedientes -->
    <div v-if="stats.recientes.length > 0" class="recent-section">
      <span class="sga-kicker">EXPEDIENTES RECIENTES</span>
      <ul class="recent-list">
        <li
          v-for="exp in stats.recientes"
          :key="exp.idExpediente"
          class="recent-row"
          tabindex="0"
          role="button"
          @click="$emit('navigate', '/expedientes')"
          @keydown.enter.prevent="$emit('navigate', '/expedientes')"
        >
          <div class="recent-row-main">
            <div class="recent-code-line">
              <span class="recent-code sga-mono">{{ exp.codigoExpediente }}</span>
              <span v-if="exp.numeroDam" class="dam-tag sga-mono">DAM {{ exp.numeroDam }}</span>
            </div>
            <span class="recent-sub">{{ exp.nombreMercancia || exp.codigoCotizacion || 'Operación aduanera' }}</span>
          </div>
          <div class="recent-row-right">
            <span :class="['mini-badge', getExpedienteStatusClass(exp.estadoExpediente)]">
              {{ formatEstadoExpediente(exp.estadoExpediente) }}
            </span>
            <span v-if="exp.estadoTransporte" class="transporte-sub">
              Transporte: {{ exp.estadoTransporte }}
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
import { getPortalChartPalette, buildBarOptions } from '../composables/portalChartTheme'
import { formatEstadoExpediente, getExpedienteStatusClass } from '../utils/dashboardMetrics'

const props = defineProps({
  stats: { type: Object, required: true }
})

defineEmits(['navigate'])

const { isDark } = useTheme()

const palette = computed(() => getPortalChartPalette(isDark.value))
const chartOptions = computed(() => buildBarOptions(palette.value))

const chartData = computed(() => ({
  labels: ['Asignado', 'En Trámite', 'Inspección', 'Levante', 'Cerrado'],
  datasets: [
    {
      label: 'Expedientes',
      data: [
        props.stats.porEstado.ASIGNADO,
        props.stats.porEstado.EN_TRAMITE,
        props.stats.porEstado.INSPECCION,
        props.stats.porEstado.LEVANTE_AUTORIZADO,
        props.stats.porEstado.CERRADO
      ],
      backgroundColor: [
        palette.value.muted,
        palette.value.info,
        palette.value.warning,
        palette.value.primary,
        palette.value.success
      ],
      borderRadius: 6,
      maxBarThickness: 36
    }
  ]
}))
</script>

<style scoped>
.chart-and-canales {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.canal-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
}

.canal-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-text-muted);
  margin-right: 0.15rem;
}

.canal-chip {
  font-size: 0.71rem;
  font-weight: 600;
  padding: 0.18rem 0.55rem;
  border-radius: var(--radius-pill);
  border: 1px solid transparent;
}

.canal-chip.is-green {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.canal-chip.is-orange {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.canal-chip.is-red {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.canal-chip.is-muted {
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
  gap: 0.45rem;
}

.empty-block i {
  font-size: 1.65rem;
  color: var(--color-accent-kicker);
}

.empty-block p {
  margin: 0;
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.empty-sub {
  font-size: 0.76rem;
  color: var(--color-text-muted);
  max-width: 280px;
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
  gap: 0.12rem;
}

.recent-code-line {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.recent-code {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.dam-tag {
  font-size: 0.66rem;
  padding: 0.08rem 0.4rem;
  border-radius: var(--radius-xs);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
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

.mini-badge.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
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

.transporte-sub {
  font-size: 0.68rem;
  color: var(--color-text-subtle);
}
</style>
