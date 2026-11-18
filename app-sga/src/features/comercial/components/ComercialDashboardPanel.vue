<template>
  <div class="comercial-dashboard-panel fade-in">
    <!-- 1. Cabecera Directa + Descarga de Informes PDF (sgaUI) -->
    <header class="sga-dash-header">
      <div>
        <h2 class="section-title">Hola {{ nombreUsuario }}</h2>
      </div>

      <div class="sga-dash-actions">
        <Button
          label="Informe Diario"
          icon="pi pi-file-pdf"
          severity="secondary"
          outlined
          size="small"
          :loading="exportingTipo === 'HOY'"
          :disabled="Boolean(exportingTipo)"
          @click="handleExportReport('HOY')"
        />
        <Button
          label="Informe Semanal"
          icon="pi pi-file-pdf"
          severity="secondary"
          outlined
          size="small"
          :loading="exportingTipo === '7D'"
          :disabled="Boolean(exportingTipo)"
          @click="handleExportReport('7D')"
        />
        <Button
          label="Informe Mensual"
          icon="pi pi-file-pdf"
          size="small"
          :loading="exportingTipo === 'MES_ACTUAL'"
          :disabled="Boolean(exportingTipo)"
          @click="handleExportReport('MES_ACTUAL')"
        />
        <Button
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

        <div v-if="!isAgenteComercial" class="sga-filter-field">
          <label class="sga-field-label">Asesor Comercial</label>
          <Select
            v-model="filterAsesorId"
            :options="asesoresOptions"
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

    <!-- 3. Grid de 6 KPIs Comerciales (sgaUI) -->
    <section class="sga-kpis-grid">
      <article class="sga-kpi-card" @click="$emit('navigate', 'cotizaciones')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">Cotizaciones Emitidas</span>
          <i class="pi pi-file-edit sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ totalCotizaciones }}</span>
          <span class="sga-kpi-pill">{{ formatUsd(totalMontoCotizado, 0) }}</span>
        </div>
        <p class="sga-kpi-foot">
          Ticket promedio: <strong class="sga-mono">{{ formatUsd(ticketPromedio) }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card is-success" @click="$emit('navigate', 'cotizaciones')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">Propuestas Aprobadas</span>
          <i class="pi pi-check-circle sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ aprobadasCount }}</span>
          <span class="sga-kpi-pill is-success">{{ conversionRate }}% Conversión</span>
        </div>
        <p class="sga-kpi-foot">
          Abonos confirmados: <strong class="sga-mono">{{ formatUsd(totalAbonadoConfirmado) }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card is-warn" @click="$emit('navigate', 'cotizaciones')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">En Proceso / Revisión</span>
          <i class="pi pi-clock sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ enProcesoCount }}</span>
          <span class="sga-kpi-pill is-warn">Pendiente + Revisión</span>
        </div>
        <p class="sga-kpi-foot">
          Por vencer (≤3 días): <strong class="sga-mono">{{ porVencerCount }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card is-danger" @click="$emit('navigate', 'cotizaciones')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Observadas</span>
          <i class="pi pi-exclamation-triangle sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ observadasCount }}</span>
          <span class="sga-kpi-pill is-danger">Requiere Ajuste</span>
        </div>
        <p class="sga-kpi-foot">
          Rechazadas / Anuladas: <strong class="sga-mono">{{ rechazadasCount }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card" @click="$emit('navigate', 'cotizaciones')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">05</span>
          <span class="sga-kpi-label">Saldo Por Cobrar</span>
          <i class="pi pi-wallet sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">{{ formatUsd(totalSaldoPendiente, 0) }}</span>
          <span class="sga-kpi-pill">USD Pendiente</span>
        </div>
        <p class="sga-kpi-foot">
          Brecha entre cotizado y abonado
        </p>
      </article>

      <article class="sga-kpi-card" @click="$emit('navigate', 'clientes')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">06</span>
          <span class="sga-kpi-label">Cartera &amp; B2B</span>
          <i class="pi pi-users sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ clientes.length }}</span>
          <span class="sga-kpi-pill">{{ clientesEfectivosCount }} Efectivos</span>
        </div>
        <p class="sga-kpi-foot">
          {{ prospectosCount }} prospectos · {{ solicitudesSinAsignarCount }} B2B sin asignar
        </p>
      </article>
    </section>

    <!-- 4. Gráficos Sencillos e Informativos (Chart.js + sgaUI) -->
    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">EMBUDO COMERCIAL</span>
            <h3 class="sga-chart-title">Distribución por Estado</h3>
          </div>
          <Tag :value="`${totalCotizaciones} cot.`" severity="secondary" rounded />
        </div>
        <div class="sga-chart-box sga-doughnut-wrap">
          <template v-if="totalCotizaciones > 0">
            <Chart
              type="doughnut"
              :data="themedEstadoChartData"
              :options="doughnutOptions"
              class="sga-chart-canvas"
            />
            <div class="sga-doughnut-center" aria-hidden="true">
              <span class="sga-doughnut-pct">{{ conversionRate }}%</span>
              <span class="sga-doughnut-sub">Conversión</span>
            </div>
          </template>
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-pie sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin datos en el período seleccionado</p>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">TENDENCIA MENSUAL</span>
            <h3 class="sga-chart-title">Emitidas vs. Aprobadas</h3>
          </div>
        </div>
        <div class="sga-chart-box">
          <Chart
            v-if="evolucionChartData.labels.length > 0"
            type="line"
            :data="themedEvolucionChartData"
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
            <span class="sga-kicker">VOLUMEN POR RÉGIMEN</span>
            <h3 class="sga-chart-title">Monto Cotizado (USD)</h3>
          </div>
        </div>
        <div class="sga-chart-box">
          <Chart
            v-if="regimenChartData.labels.length > 0"
            type="bar"
            :data="themedRegimenChartData"
            :options="horizontalBarOptions"
            class="sga-chart-canvas"
          />
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-bar sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin regímenes registrados</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, toRef } from 'vue'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { comercialService } from '../services/comercialService'
import { useComercialDashboard } from '../composables/useComercialDashboard'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { openBlobInNewTab } from '@/shared/utils/download'
import { useNotification } from '@/shared/composables/useNotification'
import { useTheme } from '@/shared/composables/useTheme'

const props = defineProps({
  cotizaciones: { type: Array, default: () => [] },
  clientes: { type: Array, default: () => [] },
  solicitudesSinAsignar: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  isAgenteComercial: { type: Boolean, default: false }
})

defineEmits(['navigate', 'refresh'])

const { showError } = useNotification()
const { isDark } = useTheme()
const authStore = useAuthStore()
const nombreUsuario = computed(() => {
  const raw = authStore.user?.nombres || authStore.user?.username || 'Usuario'
  return String(raw).trim().split(/\s+/)[0] || 'Usuario'
})
const exportingTipo = ref(null)

const periodoOptions = [
  { label: 'Todo el histórico', value: 'ALL' },
  { label: 'Hoy (Diario)', value: 'HOY' },
  { label: 'Últimos 7 días (Semanal)', value: '7D' },
  { label: 'Este mes (Mensual)', value: 'MES_ACTUAL' },
  { label: 'Últimos 30 días', value: '30D' }
]

const {
  filterPeriodo,
  filterAsesorId,
  filterRegimen,
  asesoresOptions,
  regimenesOptions,
  totalCotizaciones,
  totalMontoCotizado,
  totalAbonadoConfirmado,
  totalSaldoPendiente,
  aprobadasCount,
  enProcesoCount,
  observadasCount,
  rechazadasCount,
  conversionRate,
  ticketPromedio,
  porVencerCount,
  clientesEfectivosCount,
  prospectosCount,
  solicitudesSinAsignarCount,
  estadoChartData,
  evolucionChartData,
  regimenChartData,
  buildExportParamsForPeriodo,
  resetDashboardFilters
} = useComercialDashboard({
  cotizaciones: toRef(props, 'cotizaciones'),
  clientes: toRef(props, 'clientes'),
  solicitudesSinAsignar: toRef(props, 'solicitudesSinAsignar')
})

const hasActiveFilters = computed(
  () => filterPeriodo.value !== 'ALL' || filterAsesorId.value !== 'ALL' || filterRegimen.value !== 'ALL'
)

const formatUsd = (amount, decimals = 2) =>
  `$${Number(amount || 0).toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })}`

// Colores adaptativos para Chart.js según polaridad sgaUI
const chartPalette = computed(() =>
  isDark.value
    ? {
        text: '#c4bcab',
        grid: 'rgba(255, 255, 255, 0.08)',
        surfaceBorder: '#2b2622',
        barRegimen: '#f0b892',
        doughnutColors: ['#5cb874', '#f0b892', '#e57373', '#7a7267'],
        lineEmitidas: '#f0b892',
        fillEmitidas: 'rgba(240, 184, 146, 0.18)',
        lineAprobadas: '#5cb874',
        fillAprobadas: 'rgba(92, 184, 116, 0.18)'
      }
    : {
        text: '#6f6356',
        grid: '#ede6db',
        surfaceBorder: '#ffffff',
        barRegimen: '#352f2a',
        doughnutColors: ['#2e7d32', '#d48252', '#c2413b', '#9c9288'],
        lineEmitidas: '#d48252',
        fillEmitidas: 'rgba(212, 130, 82, 0.16)',
        lineAprobadas: '#2e7d32',
        fillAprobadas: 'rgba(46, 125, 50, 0.14)'
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

const themedEstadoChartData = computed(() => {
  const base = estadoChartData.value
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

const themedEvolucionChartData = computed(() => {
  const base = evolucionChartData.value
  return {
    ...base,
    datasets: base.datasets.map((ds, idx) => {
      const isEmitidas = idx === 0
      const stroke = isEmitidas ? chartPalette.value.lineEmitidas : chartPalette.value.lineAprobadas
      const fill = isEmitidas ? chartPalette.value.fillEmitidas : chartPalette.value.fillAprobadas
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

const themedRegimenChartData = computed(() => {
  const base = regimenChartData.value
  return {
    ...base,
    datasets: base.datasets.map((ds) => ({
      ...ds,
      backgroundColor: chartPalette.value.barRegimen,
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
    tooltip: {
      ...sharedTooltipStyle,
      callbacks: {
        label: (ctx) => ` ${formatUsd(ctx.parsed.x)}`
      }
    }
  },
  scales: {
    x: {
      beginAtZero: true,
      border: { display: false, dash: [4, 4] },
      ticks: {
        color: chartPalette.value.text,
        font: { size: 11 },
        callback: (val) => `$${Number(val).toLocaleString('en-US')}`
      },
      grid: { color: chartPalette.value.grid }
    },
    y: {
      border: { display: false },
      ticks: { color: chartPalette.value.text, font: { size: 11 } },
      grid: { display: false }
    }
  }
}))

const handleExportReport = async (periodoTipo) => {
  if (exportingTipo.value) return
  exportingTipo.value = periodoTipo
  try {
    const params = buildExportParamsForPeriodo(periodoTipo)
    const blob = await comercialService.exportarReporteComercialPdf(params)
    openBlobInNewTab(blob)
  } catch (err) {
    showError('No se pudo generar el informe comercial en PDF')
  } finally {
    exportingTipo.value = null
  }
}
</script>

<style scoped>
.comercial-dashboard-panel {
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

.sga-kpi-value.is-compact {
  font-size: 1.3rem;
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
