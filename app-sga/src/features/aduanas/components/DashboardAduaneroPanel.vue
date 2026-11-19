<template>
  <div class="aduanas-dashboard-panel fade-in">
    <!-- 1. Cabecera Directa (sgaUI) -->
    <header class="sga-dash-header">
      <div>
        <h2 class="section-title">Hola {{ nombreUsuario }}</h2>
      </div>

      <div class="sga-dash-actions">
        <Button
          label="Actualizar"
          icon="pi pi-sync"
          severity="secondary"
          outlined
          size="small"
          :loading="loading"
          aria-label="Actualizar métricas"
          title="Actualizar métricas"
          @click="$emit('refresh')"
        />
      </div>
    </header>

    <!-- 2. Barra de Filtros del Dashboard (sgaUI) -->
    <section class="sga-card-surface sga-filters-card">
      <div class="sga-filters-grid">
        <div class="sga-filter-field">
          <label class="sga-field-label">Período en Pantalla</label>
          <Select
            v-model="filterPeriodo"
            :options="periodoOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Agente Aduanero</label>
          <Select
            v-model="filterAgenteId"
            :options="agentesOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Régimen Aduanero</label>
          <Select
            v-model="filterRegimen"
            :options="regimenesOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div v-if="hasActiveFilters" class="sga-filter-reset">
          <Button
            label="Limpiar filtros"
            icon="pi pi-filter-slash"
            severity="secondary"
            text
            size="small"
            @click="resetDashboardFilters"
          />
        </div>
      </div>
    </section>

    <!-- 3. Grid de 6 KPIs Operativos (sgaUI) -->
    <section class="sga-kpis-grid">
      <article class="sga-kpi-card is-warn" @click="$emit('navigate', 'cotizaciones', 'nuevas')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">Nuevas por Auditar</span>
          <i class="pi pi-clock sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ cotizacionesClasificadas.nuevas.length }}</span>
          <span class="sga-kpi-pill is-warn">Revisión Inicial</span>
        </div>
        <p class="sga-kpi-foot">
          Total cotizaciones: <strong class="sga-mono">{{ filteredCotizaciones.length }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card is-danger" @click="$emit('navigate', 'cotizaciones', 'subsanadas')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">Subsanadas / Reingresos</span>
          <i class="pi pi-exclamation-triangle sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ cotizacionesClasificadas.subsanadas.length }}</span>
          <span class="sga-kpi-pill is-danger">Prioridad Alta</span>
        </div>
        <p class="sga-kpi-foot">
          Correcciones enviadas por Comercial
        </p>
      </article>

      <article class="sga-kpi-card is-success" @click="$emit('navigate', 'apertura')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">Listas para Apertura</span>
          <i class="pi pi-folder-plus sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ cotizacionesClasificadas.listasApertura.length }}</span>
          <span class="sga-kpi-pill is-success">Pago Confirmado</span>
        </div>
        <p class="sga-kpi-foot">
          En espera de pago Finanzas: <strong class="sga-mono">{{ cotizacionesClasificadas.pendientesPago.length }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card is-warn" @click="$emit('navigate', 'expedientes', 'PENDIENTE_CANAL')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Por Asignar Canal</span>
          <i class="pi pi-compass sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ expedientesPendienteCanal.length }}</span>
          <span class="sga-kpi-pill is-warn">Esperando DAM</span>
        </div>
        <p class="sga-kpi-foot">
          En numeración o tránsito inicial
        </p>
      </article>

      <article class="sga-kpi-card" @click="$emit('navigate', 'expedientes', 'CONTROL_SUNAT')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">05</span>
          <span class="sga-kpi-label">En Control SUNAT</span>
          <i class="pi pi-shield sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ expedientesControlSunat.length }}</span>
          <span class="sga-kpi-pill">Aforo / Doc.</span>
        </div>
        <p class="sga-kpi-foot">
          Verde <strong class="sga-mono">{{ canalCounts.VERDE }}</strong> ·
          Naranja <strong class="sga-mono">{{ canalCounts.NARANJA }}</strong> ·
          Rojo <strong class="sga-mono">{{ canalCounts.ROJO }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card is-success" @click="$emit('navigate', 'expedientes', 'LEVANTE_AUTORIZADO')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">06</span>
          <span class="sga-kpi-label">Levante Autorizado</span>
          <i class="pi pi-check-circle sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ expedientesLevante.length }}</span>
          <span class="sga-kpi-pill is-success">{{ levanteRate }}% Liberados</span>
        </div>
        <p class="sga-kpi-foot">
          Cerrados: <strong class="sga-mono">{{ expedientesCerrados.length }}</strong> de <strong class="sga-mono">{{ totalExpedientes }}</strong>
        </p>
      </article>
    </section>

    <!-- 4. Gráficos Operativos (Chart.js + sgaUI) -->
    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">CONTROL SUNAT</span>
            <h3 class="sga-chart-title">Distribución por Canal</h3>
          </div>
          <Tag :value="`${totalExpedientes} exp.`" severity="secondary" rounded />
        </div>
        <div class="sga-chart-box sga-doughnut-wrap">
          <template v-if="totalExpedientes > 0">
            <Chart
              type="doughnut"
              :data="themedCanalesChartData"
              :options="doughnutOptions"
              class="sga-chart-canvas"
            />
            <div class="sga-doughnut-center" aria-hidden="true">
              <span class="sga-doughnut-pct">{{ levanteRate }}%</span>
              <span class="sga-doughnut-sub">Levante</span>
            </div>
          </template>
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-pie sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin expedientes en el período seleccionado</p>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">TENDENCIA MENSUAL</span>
            <h3 class="sga-chart-title">Cotizaciones vs. Expedientes</h3>
          </div>
        </div>
        <div class="sga-chart-box">
          <Chart
            v-if="tendenciaChartData.labels.length > 0"
            type="line"
            :data="themedTendenciaChartData"
            :options="lineOptions"
            class="sga-chart-canvas"
          />
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-line sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin historial mensual para graficar</p>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">FLUJO OPERATIVO</span>
            <h3 class="sga-chart-title">Expedientes por Etapa</h3>
          </div>
        </div>
        <div class="sga-chart-box">
          <Chart
            v-if="totalExpedientes > 0"
            type="bar"
            :data="themedEtapasChartData"
            :options="horizontalBarOptions"
            class="sga-chart-canvas"
          />
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-bar sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin expedientes registrados</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, toRef } from 'vue'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useTheme } from '@/shared/composables/useTheme'
import { useAduanasDashboard } from '../composables/useAduanasDashboard'

const props = defineProps({
  cotizaciones: { type: Array, default: () => [] },
  expedientes: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['navigate', 'refresh'])

const authStore = useAuthStore()
const { isDark } = useTheme()

const nombreUsuario = computed(() => {
  const raw = authStore.user?.nombres || authStore.user?.username || 'Usuario'
  return String(raw).trim().split(/\s+/)[0] || 'Usuario'
})

const periodoOptions = [
  { label: 'Todo el histórico', value: 'ALL' },
  { label: 'Hoy (Diario)', value: 'HOY' },
  { label: 'Últimos 7 días (Semanal)', value: '7D' },
  { label: 'Este mes (Mensual)', value: 'MES_ACTUAL' },
  { label: 'Últimos 30 días', value: '30D' }
]

const {
  filterPeriodo,
  filterAgenteId,
  filterRegimen,
  agentesOptions,
  regimenesOptions,
  filteredCotizaciones,
  cotizacionesClasificadas,
  expedientesPendienteCanal,
  expedientesControlSunat,
  expedientesLevante,
  expedientesCerrados,
  canalCounts,
  totalExpedientes,
  levanteRate,
  canalesChartData,
  tendenciaChartData,
  etapasChartData,
  hasActiveFilters,
  resetDashboardFilters
} = useAduanasDashboard({
  cotizaciones: toRef(props, 'cotizaciones'),
  expedientes: toRef(props, 'expedientes')
})

const chartPalette = computed(() =>
  isDark.value
    ? {
        text: '#c4bcab',
        grid: 'rgba(255, 255, 255, 0.08)',
        surfaceBorder: '#2b2622',
        barStage: '#f0b892',
        doughnutColors: ['#7a7267', '#5cb874', '#f0b892', '#e57373'],
        lineCotizaciones: '#f0b892',
        fillCotizaciones: 'rgba(240, 184, 146, 0.18)',
        lineExpedientes: '#5cb874',
        fillExpedientes: 'rgba(92, 184, 116, 0.18)'
      }
    : {
        text: '#6f6356',
        grid: '#ede6db',
        surfaceBorder: '#ffffff',
        barStage: '#352f2a',
        doughnutColors: ['#9c9288', '#2e7d32', '#d48252', '#c2413b'],
        lineCotizaciones: '#d48252',
        fillCotizaciones: 'rgba(212, 130, 82, 0.16)',
        lineExpedientes: '#2e7d32',
        fillExpedientes: 'rgba(46, 125, 50, 0.14)'
      }
)

const sharedTooltipStyle = {
  backgroundColor: '#2b2622',
  titleColor: '#f6f2ea',
  bodyColor: '#f6f2ea',
  borderColor: '#4a433c',
  borderWidth: 1,
  padding: 10,
  cornerRadius: 8,
  displayColors: true,
  boxPadding: 4
}

const themedCanalesChartData = computed(() => {
  const base = canalesChartData.value
  return {
    ...base,
    datasets: base.datasets.map((ds) => ({
      ...ds,
      backgroundColor: chartPalette.value.doughnutColors,
      hoverBackgroundColor: chartPalette.value.doughnutColors,
      borderColor: chartPalette.value.surfaceBorder,
      borderWidth: 2,
      borderRadius: 6,
      spacing: 3
    }))
  }
})

const themedTendenciaChartData = computed(() => {
  const base = tendenciaChartData.value
  return {
    ...base,
    datasets: base.datasets.map((ds, idx) => {
      const isCot = idx === 0
      const stroke = isCot ? chartPalette.value.lineCotizaciones : chartPalette.value.lineExpedientes
      const fill = isCot ? chartPalette.value.fillCotizaciones : chartPalette.value.fillExpedientes
      return {
        ...ds,
        borderColor: stroke,
        backgroundColor: fill,
        pointBackgroundColor: stroke,
        pointBorderColor: chartPalette.value.surfaceBorder,
        pointBorderWidth: 2,
        pointRadius: 3.5,
        pointHoverRadius: 6,
        borderWidth: 2.5,
        tension: 0.4,
        fill: true
      }
    })
  }
})

const themedEtapasChartData = computed(() => {
  const base = etapasChartData.value
  return {
    ...base,
    datasets: base.datasets.map((ds) => ({
      ...ds,
      backgroundColor: chartPalette.value.barStage,
      borderRadius: 999,
      borderSkipped: false,
      barThickness: 14
    }))
  }
})

const doughnutOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '74%',
  plugins: {
    tooltip: sharedTooltipStyle,
    legend: {
      position: 'bottom',
      labels: {
        color: chartPalette.value.text,
        usePointStyle: true,
        pointStyle: 'circle',
        boxWidth: 8,
        padding: 14,
        font: { size: 11, family: 'Inter, sans-serif' }
      }
    }
  }
}))

const lineOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index', intersect: false },
  plugins: {
    tooltip: sharedTooltipStyle,
    legend: {
      position: 'bottom',
      labels: {
        color: chartPalette.value.text,
        usePointStyle: true,
        pointStyle: 'circle',
        boxWidth: 8,
        padding: 12,
        font: { size: 11, family: 'Inter, sans-serif' }
      }
    }
  },
  scales: {
    x: {
      border: { display: false },
      ticks: { color: chartPalette.value.text, font: { size: 11 } },
      grid: { display: false }
    },
    y: {
      beginAtZero: true,
      border: { display: false, dash: [4, 4] },
      ticks: { color: chartPalette.value.text, precision: 0, font: { size: 11 } },
      grid: { color: chartPalette.value.grid }
    }
  }
}))

const horizontalBarOptions = computed(() => ({
  indexAxis: 'y',
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: sharedTooltipStyle
  },
  scales: {
    x: {
      beginAtZero: true,
      border: { display: false, dash: [4, 4] },
      ticks: { color: chartPalette.value.text, precision: 0, font: { size: 11 } },
      grid: { color: chartPalette.value.grid }
    },
    y: {
      border: { display: false },
      ticks: { color: chartPalette.value.text, font: { size: 11 } },
      grid: { display: false }
    }
  }
}))
</script>

<style scoped>
.aduanas-dashboard-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-dash-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 0.85rem;
}

.sga-dash-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.sga-filters-card {
  padding: var(--sga-subpanel-padding, 1.25rem 1.35rem);
}

.sga-filters-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
}

.sga-filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 200px;
  flex: 1 1 200px;
  max-width: 260px;
}

.sga-field-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.sga-filter-reset {
  display: flex;
  align-items: center;
}

.sga-kpis-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.sga-kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--sga-card-radius, 14px);
  padding: var(--sga-subpanel-padding, 1.25rem 1.35rem);
  box-shadow: var(--shadow-subtle);
  cursor: pointer;
  transition: transform var(--transition-fast, 0.15s ease), border-color var(--transition-fast, 0.15s ease);
}

.sga-kpi-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-primary);
}

.sga-kpi-head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.65rem;
}

.sga-kpi-index {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
}

.sga-kpi-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-main);
  flex: 1;
}

.sga-kpi-icon {
  font-size: 0.95rem;
  color: var(--color-accent-kicker);
}

.sga-kpi-card.is-success .sga-kpi-icon { color: var(--color-success-text); }
.sga-kpi-card.is-warn .sga-kpi-icon { color: var(--color-warning-text); }
.sga-kpi-card.is-danger .sga-kpi-icon { color: var(--color-danger-text); }

.sga-kpi-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.sga-kpi-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sga-kpi-card.is-success .sga-kpi-value { color: var(--color-success-text); }
.sga-kpi-card.is-warn .sga-kpi-value { color: var(--color-warning-text); }
.sga-kpi-card.is-danger .sga-kpi-value { color: var(--color-danger-text); }

.sga-kpi-pill {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.18rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-text-muted);
}

.sga-kpi-pill.is-success {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.sga-kpi-pill.is-warn {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.sga-kpi-pill.is-danger {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.sga-kpi-foot {
  margin: 0;
  font-size: 0.76rem;
  color: var(--color-text-muted);
}

.sga-mono {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.sga-charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.sga-chart-card {
  padding: var(--sga-subpanel-padding, 1.25rem 1.35rem);
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.sga-chart-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.sga-kicker {
  display: block;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-accent-kicker);
}

.sga-chart-title {
  margin: 0.15rem 0 0;
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: var(--color-text-main);
}

.sga-chart-box {
  position: relative;
  height: 240px;
}

.sga-chart-canvas {
  width: 100%;
  height: 100%;
}

.sga-doughnut-wrap {
  position: relative;
}

.sga-doughnut-center {
  position: absolute;
  top: 41%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
}

.sga-doughnut-pct {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--color-text-main);
  line-height: 1.1;
}

.sga-doughnut-sub {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .sga-filters-card,
  .sga-kpi-card,
  .sga-chart-card {
    padding: 1.1rem;
  }
}
</style>
