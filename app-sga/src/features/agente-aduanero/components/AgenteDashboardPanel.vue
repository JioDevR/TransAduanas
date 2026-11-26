<template>
  <div class="agente-dashboard-panel fade-in">
    <!-- 1. Cabecera Directa (sgaUI: título único vive en .sga-topbar) -->
    <header class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag
          :value="`Hola, ${nombreUsuario}`"
          icon="pi pi-user"
          severity="secondary"
          rounded
        />
        <Tag
          :value="`${counts.todos} despachos en rango`"
          severity="contrast"
          class="font-mono"
          rounded
        />
        <Tag
          v-if="esSupervisorOAdmin && selectedAgenteNombre"
          :value="`Auditando: ${selectedAgenteNombre}`"
          severity="info"
          rounded
        />
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
          @click="$emit('refresh')"
        />
      </div>
    </header>

    <!-- 2. Barra de Filtros del Dashboard (sgaUI) -->
    <section class="sga-card-surface sga-filters-card">
      <div class="sga-filters-grid">
        <div class="sga-filter-field">
          <label class="sga-field-label" for="agente-dash-periodo">Período en Pantalla</label>
          <Select
            inputId="agente-dash-periodo"
            v-model="filterPeriodo"
            :options="periodoOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label" for="agente-dash-regimen">Régimen Aduanero</label>
          <Select
            inputId="agente-dash-regimen"
            v-model="filterRegimen"
            :options="regimenesOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div v-if="esSupervisorOAdmin" class="sga-filter-field">
          <label class="sga-field-label" for="agente-dash-agente">Agente Aduanero</label>
          <Select
            inputId="agente-dash-agente"
            :modelValue="selectedAgenteId"
            :options="agentesOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @update:modelValue="$emit('update:selectedAgenteId', $event)"
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

    <!-- 3. Grid de 4 KPIs Operativos (sgaUI) -->
    <section class="sga-kpis-grid">
      <article class="sga-kpi-card" @click="$emit('navigate', 'todos')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">Expedientes Asignados</span>
          <i class="pi pi-folder-open sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ counts.todos }}</span>
          <span class="sga-kpi-pill">En Bandeja</span>
        </div>
        <p class="sga-kpi-foot">
          Importación <strong class="sga-mono">{{ regimenCounts.importacion }}</strong> ·
          Exportación <strong class="sga-mono">{{ regimenCounts.exportacion }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card" @click="$emit('navigate', 'transito')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">En Tránsito / Arribo</span>
          <i class="pi pi-compass sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ counts.transito }}</span>
          <span class="sga-kpi-pill">Nave / Recojo</span>
        </div>
        <p class="sga-kpi-foot">Aguardando zarpe, arribo o DAM</p>
      </article>

      <article class="sga-kpi-card is-warn" @click="$emit('navigate', 'inspeccion')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">Aforo &amp; Diligencia SUNAT</span>
          <i class="pi pi-search-plus sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ counts.inspeccion }}</span>
          <span class="sga-kpi-pill is-warn">Naranja / Rojo</span>
        </div>
        <p class="sga-kpi-foot">
          Naranja <strong class="sga-mono">{{ canalCounts.NARANJA }}</strong> ·
          Rojo <strong class="sga-mono">{{ canalCounts.ROJO }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card is-success" @click="$emit('navigate', 'levante')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Con Levante &amp; Despacho</span>
          <i class="pi pi-check-circle sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ counts.levante }}</span>
          <span class="sga-kpi-pill is-success">{{ levanteRate }}% Liberados</span>
        </div>
        <p class="sga-kpi-foot">
          Cerrados: <strong class="sga-mono">{{ counts.cerrados }}</strong> de
          <strong class="sga-mono">{{ counts.todos }}</strong>
        </p>
      </article>
    </section>

    <!-- 4. Gráficos Operativos (Chart.js + sgaUI) -->
    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">CONTROL SUNAT</span>
            <h3 class="sga-chart-title">Semáforo de Canales</h3>
          </div>
          <Tag :value="`${counts.todos} exp.`" severity="secondary" rounded />
        </div>
        <div class="sga-chart-box sga-doughnut-wrap">
          <template v-if="counts.todos > 0">
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
            <span class="sga-kicker">FLUJO DE CAMPO</span>
            <h3 class="sga-chart-title">Expedientes por Fase Operativa</h3>
          </div>
        </div>
        <div class="sga-chart-box">
          <Chart
            v-if="counts.todos > 0"
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

    <!-- 5. Cola de Atención Prioritaria -->
    <section class="sga-card-surface sga-priority-section">
      <div class="sga-chart-head">
        <div>
          <span class="sga-kicker">ATENCIÓN INMEDIATA</span>
          <h3 class="sga-chart-title">Despachos con Acción Pendiente</h3>
        </div>
        <Tag
          :value="`${despachosPrioritarios.length} pendientes`"
          :severity="despachosPrioritarios.length > 0 ? 'warn' : 'success'"
          rounded
        />
      </div>

      <div v-if="despachosPrioritarios.length === 0" class="sga-banner is-success">
        <i class="pi pi-check-circle"></i>
        <span>No hay despachos con acciones operativas urgentes pendientes en este momento.</span>
      </div>

      <div v-else class="sga-priority-list">
        <div
          v-for="item in despachosPrioritarios"
          :key="item.expediente.idExpediente"
          class="sga-priority-row"
        >
          <div class="sga-priority-main">
            <div class="sga-cell-meta">
              <span class="sga-code-cell font-mono">{{ item.expediente.codigoExpediente }}</span>
              <Tag
                :value="formatRegimenShort(item.expediente.regimenAduanero)"
                severity="secondary"
                class="sga-mini-tag"
                rounded
              />
              <Tag
                v-if="item.expediente.numeroDam"
                :value="`DAM ${item.expediente.numeroDam}`"
                severity="contrast"
                class="sga-mini-tag font-mono"
                rounded
              />
            </div>
            <span class="sga-cell-primary">{{ item.expediente.clienteRazonSocial || 'Cliente' }}</span>
          </div>

          <Tag :value="item.motivo" :severity="item.severity" :icon="item.icon" rounded />

          <div class="sga-priority-actions">
            <Button
              label="Informe"
              icon="pi pi-file-edit"
              severity="secondary"
              outlined
              size="small"
              @click="$emit('inspect', item.expediente)"
            />
            <Button
              label="Ir a Fase"
              icon="pi pi-arrow-right"
              iconPos="right"
              size="small"
              @click="$emit('navigate', item.tab)"
            />
          </div>
        </div>
      </div>
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
import { useAgenteDashboard } from '../composables/useAgenteDashboard'
import { formatRegimenShort } from '../utils/agenteFormatters'

const props = defineProps({
  expedientes: { type: Array, default: () => [] },
  selectedAgenteId: { type: [Number, String, null], default: null },
  agentesList: { type: Array, default: () => [] },
  esSupervisorOAdmin: { type: Boolean, default: false },
  loading: { type: Boolean, default: false }
})

defineEmits(['navigate', 'inspect', 'refresh', 'update:selectedAgenteId'])

const authStore = useAuthStore()
const { isDark } = useTheme()

const nombreUsuario = computed(() => {
  const raw = authStore.user?.nombres || authStore.user?.nombre || authStore.user?.username || 'Agente'
  return String(raw).trim().split(/\s+/)[0] || 'Agente'
})

const periodoOptions = [
  { label: 'Todo el histórico', value: 'ALL' },
  { label: 'Hoy (Diario)', value: 'HOY' },
  { label: 'Últimos 7 días', value: '7D' },
  { label: 'Este mes', value: 'MES_ACTUAL' },
  { label: 'Últimos 30 días', value: '30D' }
]

const agentesOptions = computed(() => [
  { label: 'Todos los agentes', value: null },
  ...(props.agentesList || []).map((ag) => ({
    label: `${ag.nombre || ag.nombres || ''} ${ag.apellidos || ''}`.trim(),
    value: ag.idUsuario
  }))
])

const selectedAgenteNombre = computed(() => {
  if (!props.selectedAgenteId) return ''
  const found = (props.agentesList || []).find((a) => a.idUsuario === props.selectedAgenteId)
  return found ? `${found.nombre || found.nombres || ''} ${found.apellidos || ''}`.trim() : ''
})

const {
  filterPeriodo,
  filterRegimen,
  regimenesOptions,
  counts,
  canalCounts,
  regimenCounts,
  levanteRate,
  canalesChartData,
  etapasChartData,
  despachosPrioritarios,
  hasActiveFilters,
  resetDashboardFilters
} = useAgenteDashboard({
  expedientes: toRef(props, 'expedientes')
})

const chartPalette = computed(() =>
  isDark.value
    ? {
        text: '#c4bcab',
        grid: 'rgba(255, 255, 255, 0.08)',
        surfaceBorder: '#2b2622',
        barStage: '#f0b892',
        doughnutColors: ['#7a7267', '#5cb874', '#f0b892', '#e57373']
      }
    : {
        text: '#6f6356',
        grid: '#ede6db',
        surfaceBorder: '#ffffff',
        barStage: '#352f2a',
        doughnutColors: ['#9c9288', '#2e7d32', '#d48252', '#c2413b']
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

const themedCanalesChartData = computed(() => ({
  ...canalesChartData.value,
  datasets: canalesChartData.value.datasets.map((ds) => ({
    ...ds,
    backgroundColor: chartPalette.value.doughnutColors,
    hoverBackgroundColor: chartPalette.value.doughnutColors,
    borderColor: chartPalette.value.surfaceBorder,
    borderWidth: 2,
    borderRadius: 6,
    spacing: 3
  }))
}))

const themedEtapasChartData = computed(() => ({
  ...etapasChartData.value,
  datasets: etapasChartData.value.datasets.map((ds) => ({
    ...ds,
    backgroundColor: chartPalette.value.barStage,
    borderRadius: 999,
    borderSkipped: false,
    barThickness: 14
  }))
}))

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
.agente-dashboard-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-priority-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sga-priority-list {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.sga-priority-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.85rem;
  padding: 0.85rem 1rem;
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border);
  background: var(--color-bg-base);
}

.sga-priority-main {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 220px;
  flex: 1;
}

.sga-priority-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
</style>
