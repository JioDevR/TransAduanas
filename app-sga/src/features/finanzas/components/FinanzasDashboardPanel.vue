<template>
  <div class="finanzas-dashboard-panel fade-in">
    <!-- 1. Cabecera Directa (sgaUI) -->
    <header class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag value="TESORERÍA ONLINE" severity="success" rounded />
        <Tag :value="`T.C. Ref: S/ ${tipoCambioOficial.toFixed(2)}`" severity="secondary" class="font-mono" rounded />
        <Button
          v-if="anticiposPendientesCount > 0"
          type="button"
          :label="`${anticiposPendientesCount} abonos por validar`"
          icon="pi pi-shield"
          severity="warn"
          outlined
          size="small"
          @click="$emit('navigate', 'abonos-b2b')"
        />
        <Button
          v-if="expedientesPorLiquidarCount > 0"
          type="button"
          :label="`${expedientesPorLiquidarCount} exp. por liquidar`"
          icon="pi pi-receipt"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('navigate', 'liquidaciones', 'por-liquidar')"
        />
      </div>

      <div class="sga-dash-actions">
        <Button
          type="button"
          label="Actualizar"
          icon="pi pi-sync"
          severity="secondary"
          outlined
          size="small"
          :loading="loading"
          @click="$emit('refresh')"
        />
        <Button
          type="button"
          label="Registrar Abono"
          icon="pi pi-wallet"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('registrar-abono', null)"
        />
        <Button
          type="button"
          label="Nueva Liquidación"
          icon="pi pi-plus"
          size="small"
          @click="$emit('emitir-liquidacion', null)"
        />
      </div>
    </header>

    <!-- 2. Barra de Filtros -->
    <section class="sga-card-surface sga-filters-card">
      <div class="sga-filters-grid">
        <div class="sga-filter-field">
          <label for="fin-dash-periodo" class="sga-field-label">Período en Pantalla</label>
          <Select
            inputId="fin-dash-periodo"
            v-model="filterPeriodo"
            :options="periodoOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div v-if="hasActiveFilters" class="sga-filter-reset">
          <Button
            type="button"
            label="Limpiar filtro"
            icon="pi pi-filter-slash"
            severity="secondary"
            text
            size="small"
            @click="resetDashboardFilters"
          />
        </div>
      </div>
    </section>

    <!-- 3. Grid de 4 KPIs Ejecutivos (sgaUI) -->
    <section class="sga-kpis-grid">
      <article class="sga-kpi-card is-success" @click="$emit('navigate', 'conciliacion')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">Recaudación Conforme</span>
          <i class="pi pi-check-circle sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">{{ formatPen(liquidacionesStats.cobradoPen) }}</span>
          <span class="sga-kpi-pill is-success">{{ liquidacionesStats.pagadasCount }} liq.</span>
        </div>
        <p class="sga-kpi-foot">
          Equiv. aprox: <strong class="sga-mono">{{ formatUsd(liquidacionesStats.cobradoPen / tipoCambioOficial) }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card is-warn" @click="$emit('navigate', 'pre-expedientes')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">Saldos Pre-Expediente</span>
          <i class="pi pi-wallet sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">{{ formatUsd(carteraSummary.totalSaldoPendienteUsd) }}</span>
          <span class="sga-kpi-pill is-warn">{{ carteraSummary.conSaldoPendienteCount }} cot.</span>
        </div>
        <p class="sga-kpi-foot">
          Recaudado: <strong class="sga-mono">{{ formatUsd(carteraSummary.totalAbonadoUsd) }}</strong> de {{ formatUsd(carteraSummary.totalCotizadoUsd) }}
        </p>
      </article>

      <article class="sga-kpi-card" @click="$emit('navigate', 'liquidaciones')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">Liquidaciones por Cobrar</span>
          <i class="pi pi-receipt sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">{{ formatPen(liquidacionesStats.porCobrarPen) }}</span>
          <span class="sga-kpi-pill">{{ liquidacionesStats.pendientesCount }} pend.</span>
        </div>
        <p class="sga-kpi-foot">
          Por liquidar: <strong class="sga-mono">{{ expedientesPorLiquidarCount }} expedientes</strong>
        </p>
      </article>

      <article class="sga-kpi-card" @click="$emit('navigate', 'liquidaciones')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Tributos SUNAT & SPOT</span>
          <i class="pi pi-shield sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">{{ formatPen(liquidacionesStats.tributosSunatPen) }}</span>
          <span class="sga-kpi-pill">SPOT 12%</span>
        </div>
        <p class="sga-kpi-foot">
          Fondo SPOT: <strong class="sga-mono">{{ formatPen(liquidacionesStats.detraccionesPen) }}</strong>
        </p>
      </article>
    </section>

    <!-- 4. Gráficos Chart.js (sgaUI) -->
    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">CONTROL PRE-OPERATIVO</span>
            <h3 class="sga-chart-title">Embudo de Cobranza de Cotizaciones</h3>
          </div>
          <Button
            type="button"
            label="Ver Cartera"
            icon="pi pi-arrow-right"
            iconPos="right"
            severity="secondary"
            text
            size="small"
            @click="$emit('navigate', 'pre-expedientes')"
          />
        </div>

        <div class="sga-chart-box sga-doughnut-wrap">
          <template v-if="filteredCotizaciones.length > 0">
            <Chart
              type="doughnut"
              :data="themedEmbudoChartData"
              :options="doughnutOptions"
              class="sga-chart-canvas"
            />
            <div class="sga-doughnut-center" aria-hidden="true">
              <span class="sga-doughnut-pct">{{ tasaRecaudacionCarteraPct }}%</span>
              <span class="sga-doughnut-sub">Cobrado</span>
            </div>
          </template>
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-pie sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin cotizaciones aprobadas en el período</p>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">FLUJO POST-DESPACHO</span>
            <h3 class="sga-chart-title">Balance de Liquidaciones & Tributos (S/)</h3>
          </div>
          <Button
            type="button"
            label="Liquidaciones"
            icon="pi pi-arrow-right"
            iconPos="right"
            severity="secondary"
            text
            size="small"
            @click="$emit('navigate', 'liquidaciones')"
          />
        </div>

        <div class="sga-chart-box">
          <Chart
            v-if="hasBalanceChartValues"
            type="bar"
            :data="themedBalanceChartData"
            :options="barOptions"
            class="sga-chart-canvas"
          />
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-bar sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin liquidaciones emitidas para graficar</p>
          </div>
        </div>
      </article>
    </section>

    <!-- 5. Atención Inmediata: Cotizaciones en Espera de Cobro o Validación -->
    <section class="sga-card-surface sga-table-card">
      <div class="sga-table-toolbar">
        <div>
          <span class="sga-kicker">ATENCIÓN INMEDIATA</span>
          <h3 class="sga-chart-title">Cotizaciones Aprobadas en Espera de Cobro o Validación</h3>
        </div>
        <div class="sga-toolbar-actions">
          <Button
            type="button"
            :label="`Ver todas (${filteredCotizaciones.length})`"
            icon="pi pi-arrow-right"
            iconPos="right"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('navigate', 'pre-expedientes')"
          />
        </div>
      </div>

      <DataTable
        :value="cotizacionesPrioritarias"
        :loading="loading"
        responsiveLayout="scroll"
        dataKey="idCotizacion"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-check-circle sga-empty-icon sga-text-success"></i>
            <p class="sga-empty-title">Cartera al día</p>
            <p class="sga-empty-desc">No hay cotizaciones aprobadas con saldos pendientes en este período.</p>
          </div>
        </template>

        <Column header="COTIZACIÓN">
          <template #body="{ data: c }">
            <div class="sga-cell-stack">
              <span class="sga-code-cell font-mono">{{ c.codigoCotizacion }}</span>
              <span class="sga-cell-sub font-mono">{{ formatDate(c.fechaEmision) }}</span>
            </div>
          </template>
        </Column>

        <Column header="CLIENTE IMPORTADOR">
          <template #body="{ data: c }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ c.clienteRazonSocial || 'Cliente no asignado' }}</span>
              <span class="sga-cell-sub font-mono">Doc: {{ c.clienteNumeroDocumento || '—' }}</span>
            </div>
          </template>
        </Column>

        <Column header="TOTAL COTIZADO" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: c }">
            <span class="sga-amount-cell font-mono">{{ formatUsd(c.totalCotizado) }}</span>
          </template>
        </Column>

        <Column header="SALDO PENDIENTE" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: c }">
            <span class="sga-amount-cell font-mono sga-text-warn">
              {{ formatUsd(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado) }}
            </span>
          </template>
        </Column>

        <Column header="ESTADO FINANZAS">
          <template #body="{ data: c }">
            <Tag
              :value="formatEstadoPagoCotizacionLabel(c.estadoPago)"
              :severity="formatEstadoPagoCotizacionSeverity(c.estadoPago)"
              rounded
            />
          </template>
        </Column>

        <Column header="ACCIONES" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: c }">
            <div class="sga-row-actions">
              <Button
                type="button"
                label="Abonar"
                icon="pi pi-wallet"
                size="small"
                @click="$emit('registrar-abono', c)"
              />
              <Button
                type="button"
                label="Historial"
                icon="pi pi-list"
                severity="secondary"
                outlined
                size="small"
                @click="$emit('ver-historial', c)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </section>
  </div>
</template>

<script setup>
import { computed, toRef } from 'vue'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { useTheme } from '@/shared/composables/useTheme'
import { formatDate } from '@/shared/utils/dateUtils'
import { useFinanzasDashboard } from '../composables/useFinanzasDashboard'
import {
  formatPen,
  formatUsd,
  formatEstadoPagoCotizacionLabel,
  formatEstadoPagoCotizacionSeverity
} from '../utils/finanzasFormatters'

const props = defineProps({
  cotizacionesAprobadas: { type: Array, default: () => [] },
  anticiposList: { type: Array, default: () => [] },
  liquidacionesList: { type: Array, default: () => [] },
  expedientesPorLiquidar: { type: Array, default: () => [] },
  metricas: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false }
})

defineEmits(['refresh', 'navigate', 'registrar-abono', 'emitir-liquidacion', 'ver-historial'])

const { isDark } = useTheme()

const {
  filterPeriodo,
  periodoOptions,
  hasActiveFilters,
  resetDashboardFilters,
  tipoCambioOficial,
  filteredCotizaciones,
  carteraSummary,
  liquidacionesStats,
  anticiposPendientesCount,
  expedientesPorLiquidarCount,
  tasaRecaudacionCarteraPct,
  embudoChartData,
  balanceChartData,
  cotizacionesPrioritarias
} = useFinanzasDashboard({
  cotizacionesAprobadas: toRef(props, 'cotizacionesAprobadas'),
  anticiposList: toRef(props, 'anticiposList'),
  liquidacionesList: toRef(props, 'liquidacionesList'),
  expedientesPorLiquidar: toRef(props, 'expedientesPorLiquidar'),
  metricas: toRef(props, 'metricas')
})

const hasBalanceChartValues = computed(() =>
  balanceChartData.value.datasets[0].data.some((v) => Number(v) > 0)
)

const chartPalette = computed(() =>
  isDark.value
    ? {
        text: '#c4bcab',
        grid: 'rgba(255, 255, 255, 0.08)',
        surfaceBorder: '#2b2622',
        doughnutColors: ['#7a7267', '#fcd34d', '#f0b892', '#5cb874'],
        barColors: ['#5cb874', '#fcd34d', '#f0b892', '#93c5fd']
      }
    : {
        text: '#6f6356',
        grid: '#ede6db',
        surfaceBorder: '#ffffff',
        doughnutColors: ['#9c9288', '#d97706', '#d48252', '#1e6638'],
        barColors: ['#1e6638', '#d97706', '#d48252', '#21598f']
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

const themedEmbudoChartData = computed(() => ({
  ...embudoChartData.value,
  datasets: embudoChartData.value.datasets.map((ds) => ({
    ...ds,
    backgroundColor: chartPalette.value.doughnutColors,
    hoverBackgroundColor: chartPalette.value.doughnutColors,
    borderColor: chartPalette.value.surfaceBorder,
    borderWidth: 2,
    borderRadius: 6,
    spacing: 3
  }))
}))

const themedBalanceChartData = computed(() => ({
  ...balanceChartData.value,
  datasets: balanceChartData.value.datasets.map((ds) => ({
    ...ds,
    backgroundColor: chartPalette.value.barColors,
    borderRadius: 8,
    borderSkipped: false,
    maxBarThickness: 42
  }))
}))

const doughnutOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '72%',
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        color: chartPalette.value.text,
        usePointStyle: true,
        pointStyle: 'circle',
        padding: 12,
        font: { family: "'Inter', sans-serif", size: 11, weight: '600' }
      }
    },
    tooltip: sharedTooltipStyle
  }
}))

const barOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      ...sharedTooltipStyle,
      callbacks: {
        label: (ctx) => ` ${formatPen(ctx.parsed.y)}`
      }
    }
  },
  scales: {
    x: {
      grid: { display: false },
      ticks: {
        color: chartPalette.value.text,
        font: { family: "'Inter', sans-serif", size: 11, weight: '600' }
      }
    },
    y: {
      beginAtZero: true,
      grid: { color: chartPalette.value.grid },
      ticks: {
        color: chartPalette.value.text,
        font: { family: "'JetBrains Mono', monospace", size: 11 },
        callback: (val) => `S/ ${Number(val).toLocaleString('es-PE')}`
      }
    }
  }
}))
</script>

<style scoped>
.finanzas-dashboard-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.4rem;
}
</style>
