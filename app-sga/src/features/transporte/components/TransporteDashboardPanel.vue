<template>
  <div class="transporte-dashboard-panel fade-in">
    <!-- 1. Cabecera Directa (sgaUI) -->
    <header class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag value="CONTROL LOGÍSTICO MTC" severity="success" rounded />
        <Tag
          :value="`${kpis.unidadesDisponibles}/${kpis.unidadesActivas} unidades disponibles`"
          severity="secondary"
          class="font-mono"
          rounded
        />
        <Button
          v-if="kpis.despachosPendientes > 0"
          type="button"
          :label="`${kpis.despachosPendientes} pendientes de asignar`"
          icon="pi pi-clock"
          severity="warn"
          outlined
          size="small"
          @click="$emit('select-filter', 'PENDIENTE_ASIGNACION')"
        />
        <Button
          v-if="kpis.alertasVencidas > 0"
          type="button"
          :label="`${kpis.alertasVencidas} doc. vencido(s)`"
          icon="pi pi-exclamation-triangle"
          severity="danger"
          outlined
          size="small"
          @click="$emit('navigate', alertasMtc[0]?.targetTab || 'unidades')"
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
          label="Programar Despacho"
          icon="pi pi-plus"
          size="small"
          @click="$emit('programar-despacho')"
        />
      </div>
    </header>

    <!-- 2. Grid de 4 KPIs Unificados (sgaUI) -->
    <section class="sga-kpis-grid">
      <article class="sga-kpi-card" @click="$emit('select-filter', 'EN_RUTA')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">Despachos En Tránsito</span>
          <i class="pi pi-compass sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ kpis.despachosEnRuta }}</span>
          <span class="sga-kpi-pill">{{ kpis.despachosProgramados }} prog.</span>
        </div>
        <p class="sga-kpi-foot">Unidades circulando hacia planta o terminal</p>
      </article>

      <article class="sga-kpi-card is-warn" @click="$emit('select-filter', 'PENDIENTE_ASIGNACION')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">Pendientes de Asignar</span>
          <i class="pi pi-inbox sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ kpis.despachosPendientes }}</span>
          <span class="sga-kpi-pill is-warn">{{ kpis.despachosTotal }} total</span>
        </div>
        <p class="sga-kpi-foot">Solicitudes de Aduanas esperando unidad y chofer</p>
      </article>

      <article class="sga-kpi-card is-success" @click="$emit('navigate', 'unidades')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">Flota Disponible en Patio</span>
          <i class="pi pi-truck sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ kpis.unidadesDisponibles }}</span>
          <span class="sga-kpi-pill is-success">{{ kpis.tractosDisponibles }} tractos</span>
        </div>
        <p class="sga-kpi-foot">
          {{ kpis.conductoresDisponibles }} choferes disponibles · {{ kpis.unidadesMantenimiento }} en taller
        </p>
      </article>

      <article class="sga-kpi-card is-success" @click="$emit('select-filter', 'ENTREGADO')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Entregas Conformes</span>
          <i class="pi pi-check-circle sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ kpis.despachosEntregados }}</span>
          <span class="sga-kpi-pill">{{ kpis.conductoresActivos }} choferes</span>
        </div>
        <p class="sga-kpi-foot">Traslados completados con acta de recepción</p>
      </article>
    </section>

    <!-- 3. Gráficos & Alertas Preventivas MTC -->
    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">CICLO OPERATIVO</span>
            <h3 class="sga-chart-title">Distribución de Despachos Terrestres</h3>
          </div>
          <Button
            type="button"
            label="Ver bandeja"
            icon="pi pi-arrow-right"
            iconPos="right"
            severity="secondary"
            text
            size="small"
            @click="$emit('navigate', 'despachos')"
          />
        </div>

        <div class="sga-chart-box sga-doughnut-wrap">
          <template v-if="despachos.length > 0">
            <Chart
              type="doughnut"
              :data="despachosChartData"
              :options="doughnutOptions"
              class="sga-chart-canvas"
            />
            <div class="sga-doughnut-center" aria-hidden="true">
              <span class="sga-doughnut-pct">{{ despachos.length }}</span>
              <span class="sga-doughnut-sub">Despachos</span>
            </div>
          </template>
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-pie sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin despachos registrados para graficar</p>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">ALERTAS TÉCNICAS MTC (30 DÍAS)</span>
            <h3 class="sga-chart-title">Vencimientos SOAT, CITV & Licencias</h3>
          </div>
          <Tag
            :value="`${alertasMtc.length} alerta(s)`"
            :severity="alertasMtc.length > 0 ? (kpis.alertasVencidas > 0 ? 'danger' : 'warn') : 'success'"
            rounded
          />
        </div>

        <div v-if="alertasMtc.length === 0" class="sga-empty-state">
          <i class="pi pi-verified sga-empty-icon sga-text-success"></i>
          <p class="sga-empty-title">Documentación MTC al día</p>
          <p class="sga-empty-desc">Todas las unidades y conductores activos tienen SOAT, CITV y brevetes vigentes.</p>
        </div>

        <div v-else class="alertas-scroll-list">
          <button
            v-for="a in alertasMtc"
            :key="a.id"
            type="button"
            class="alerta-row-btn"
            @click="$emit('navigate', a.targetTab)"
          >
            <div class="alerta-row-main">
              <strong class="alerta-row-title">{{ a.titulo }}</strong>
              <span class="sga-cell-sub">{{ a.subtitulo }}</span>
            </div>
            <div class="alerta-row-meta">
              <Tag
                :value="a.vencido ? `Vencido (${Math.abs(a.diasRestantes)}d)` : `${a.diasRestantes}d`"
                :severity="a.severity"
                rounded
              />
              <span class="sga-cell-sub font-mono">{{ formatDate(a.vencimiento) }}</span>
            </div>
          </button>
        </div>
      </article>
    </section>

    <!-- 4. Despachos Activos en Operación -->
    <section class="sga-card-surface sga-table-card">
      <div class="sga-table-toolbar">
        <div>
          <span class="sga-kicker">SEGUIMIENTO EN CURSO</span>
          <h3 class="sga-chart-title">Despachos en Ruta y Programados</h3>
        </div>
        <Button
          type="button"
          label="Ir a Monitoreo"
          icon="pi pi-arrow-right"
          iconPos="right"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('navigate', 'despachos')"
        />
      </div>

      <DataTable
        :value="despachosActivos"
        :loading="loading"
        responsiveLayout="scroll"
        dataKey="idDespachoTransporte"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-truck sga-empty-icon"></i>
            <p class="sga-empty-title">Sin traslados activos en este momento</p>
            <p class="sga-empty-desc">Los despachos programados o en ruta aparecerán aquí para seguimiento directo.</p>
          </div>
        </template>

        <Column header="TRACKING">
          <template #body="{ data: d }">
            <div class="sga-cell-stack">
              <span class="sga-code-cell font-mono">{{ d.codigoTracking }}</span>
              <span class="sga-cell-sub font-mono">{{ d.codigoExpediente || '—' }}</span>
            </div>
          </template>
        </Column>

        <Column header="CLIENTE & DESTINO">
          <template #body="{ data: d }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ d.clienteRazonSocial || 'Cliente' }}</span>
              <span class="sga-cell-sub">{{ d.distritoLlegada }} · {{ d.direccionLlegada }}</span>
            </div>
          </template>
        </Column>

        <Column header="UNIDAD & CONDUCTOR">
          <template #body="{ data: d }">
            <div class="sga-cell-stack">
              <span class="sga-code-cell font-mono">{{ d.unidadPlacaTracto || 'Sin asignar' }}</span>
              <span class="sga-cell-sub">{{ d.conductorNombre || 'Sin chofer' }}</span>
            </div>
          </template>
        </Column>

        <Column header="ESTADO">
          <template #body="{ data: d }">
            <Tag
              :value="formatEstadoDespacho(d.estadoTransporte)"
              :severity="getEstadoDespachoSeverity(d.estadoTransporte)"
              rounded
            />
          </template>
        </Column>

        <Column header="ACCIÓN" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: d }">
            <Button
              type="button"
              label="Hoja de Ruta"
              icon="pi pi-eye"
              severity="secondary"
              outlined
              size="small"
              @click="$emit('ver-detalle', d)"
            />
          </template>
        </Column>
      </DataTable>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Tag from 'primevue/tag'
import { useTheme } from '@/shared/composables/useTheme'
import {
  formatDate,
  formatEstadoDespacho,
  getEstadoDespachoSeverity
} from '../utils/transporteFormatters'

const props = defineProps({
  despachos: { type: Array, default: () => [] },
  alertasMtc: { type: Array, default: () => [] },
  kpis: { type: Object, required: true },
  loading: { type: Boolean, default: false }
})

defineEmits(['refresh', 'navigate', 'select-filter', 'programar-despacho', 'ver-detalle'])

const { isDark } = useTheme()

const despachosActivos = computed(() =>
  props.despachos
    .filter((d) => ['EN_RUTA', 'PROGRAMADO', 'PENDIENTE_ASIGNACION'].includes(d.estadoTransporte))
    .slice(0, 6)
)

const chartPalette = computed(() =>
  isDark.value
    ? {
        text: '#e6dfd5',
        surfaceBorder: '#24201c',
        colors: ['#f59e0b', '#38bdf8', '#d48252', '#4ade80', '#f87171']
      }
    : {
        text: '#4a433c',
        surfaceBorder: '#ffffff',
        colors: ['#d97706', '#21598f', '#b45309', '#1e6638', '#b91c1c']
      }
)

const despachosChartData = computed(() => ({
  labels: ['Pendiente', 'Programado', 'En Ruta', 'Entregado', 'Cancelado'],
  datasets: [
    {
      data: [
        props.kpis.despachosPendientes || 0,
        props.kpis.despachosProgramados || 0,
        props.kpis.despachosEnRuta || 0,
        props.kpis.despachosEntregados || 0,
        props.kpis.despachosCancelados || 0
      ],
      backgroundColor: chartPalette.value.colors,
      borderColor: chartPalette.value.surfaceBorder,
      borderWidth: 2,
      borderRadius: 6,
      spacing: 3
    }
  ]
}))

const doughnutOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '70%',
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
    }
  }
}))
</script>

<style scoped>
.transporte-dashboard-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.alertas-scroll-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 240px;
  overflow-y: auto;
  padding-right: 0.25rem;
}

.alerta-row-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.65rem 0.85rem;
  border-radius: 10px;
  border: 1px solid var(--color-border-subtle);
  background: var(--color-bg-subtle);
  cursor: pointer;
  text-align: left;
  transition: border-color var(--transition-fast);
}

.alerta-row-btn:hover {
  border-color: var(--color-primary);
}

.alerta-row-main,
.alerta-row-meta {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.alerta-row-meta {
  align-items: flex-end;
  flex-shrink: 0;
}

.alerta-row-title {
  font-size: 0.84rem;
  color: var(--color-text-main);
}
</style>
