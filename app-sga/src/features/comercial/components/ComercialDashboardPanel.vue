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
</style>
