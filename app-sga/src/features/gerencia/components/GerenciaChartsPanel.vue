<template>
  <section class="sga-charts-grid">
    <article class="sga-card-surface sga-chart-card">
      <div class="sga-chart-head">
        <div>
          <span class="sga-kicker">COBERTURA DE MAESTROS</span>
          <h3 class="sga-chart-title">Composición de Red &amp; Catálogos</h3>
        </div>
        <Tag :value="`${totalRegistros} registros`" severity="secondary" class="sga-mono" rounded />
      </div>

      <div class="sga-chart-box sga-doughnut-wrap">
        <template v-if="totalRegistros > 0">
          <Chart
            type="doughnut"
            :data="themedCoberturaData"
            :options="doughnutOptions"
            class="sga-chart-canvas"
          />
          <div class="sga-doughnut-center" aria-hidden="true">
            <span class="sga-doughnut-pct sga-mono">{{ totalRegistros }}</span>
            <span class="sga-doughnut-sub">Maestros</span>
          </div>
        </template>
        <div v-else class="sga-empty-state">
          <i class="pi pi-chart-pie sga-empty-icon"></i>
          <p class="sga-empty-desc">Sin registros en catálogos maestros</p>
        </div>
      </div>
    </article>

    <article class="sga-card-surface sga-chart-card">
      <div class="sga-chart-head">
        <div>
          <span class="sga-kicker">DISPONIBILIDAD TRANSVERSAL</span>
          <h3 class="sga-chart-title">Activos vs Inactivos por Dominio</h3>
        </div>
        <Tag
          :value="`${metrics.totalCapacidadToneladas} Ton. flota`"
          severity="info"
          class="sga-mono"
          rounded
        />
      </div>

      <div class="sga-chart-box">
        <Chart
          v-if="hasAnyDomainData"
          type="bar"
          :data="themedDisponibilidadData"
          :options="barOptions"
          class="sga-chart-canvas"
        />
        <div v-else class="sga-empty-state">
          <i class="pi pi-chart-bar sga-empty-icon"></i>
          <p class="sga-empty-desc">Sin datos operativos para graficar</p>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import Chart from 'primevue/chart'
import Tag from 'primevue/tag'
import { useTheme } from '@/shared/composables/useTheme'

const props = defineProps({
  metrics: {
    type: Object,
    required: true
  }
})

const { isDark } = useTheme()

const totalRegistros = computed(
  () =>
    (props.metrics.sedesCount || 0) +
    (props.metrics.puertosCount || 0) +
    (props.metrics.cargasCount || 0) +
    (props.metrics.mercanciasCount || 0)
)

const hasAnyDomainData = computed(
  () =>
    totalRegistros.value > 0 ||
    (props.metrics.unidadesCount || 0) > 0 ||
    (props.metrics.conductoresCount || 0) > 0
)

const palette = computed(() =>
  isDark.value
    ? {
        text: '#d6cfc7',
        grid: 'rgba(255, 255, 255, 0.08)',
        border: '#2b2622',
        doughnut: ['#d4a373', '#7aa2d4', '#e09f5a', '#6bc08c'],
        active: '#6bc08c',
        inactive: '#4a433d'
      }
    : {
        text: '#5c534a',
        grid: 'rgba(60, 48, 36, 0.08)',
        border: '#ffffff',
        doughnut: ['#4a3728', '#8c5a3c', '#c88a4b', '#3a6b56'],
        active: '#3a6b56',
        inactive: '#dcd4cb'
      }
)

const themedCoberturaData = computed(() => {
  const base = props.metrics.coberturaChartData
  return {
    labels: base.labels,
    datasets: [
      {
        ...base.datasets[0],
        backgroundColor: palette.value.doughnut,
        borderColor: palette.value.border,
        borderWidth: 2
      }
    ]
  }
})

const themedDisponibilidadData = computed(() => {
  const base = props.metrics.disponibilidadChartData
  return {
    labels: base.labels,
    datasets: [
      { ...base.datasets[0], backgroundColor: palette.value.active },
      { ...base.datasets[1], backgroundColor: palette.value.inactive }
    ]
  }
})

const doughnutOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '66%',
  plugins: {
    legend: {
      position: 'bottom',
      labels: { color: palette.value.text, boxWidth: 12, padding: 14, font: { size: 11 } }
    }
  }
}))

const barOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom',
      labels: { color: palette.value.text, boxWidth: 12, padding: 12, font: { size: 11 } }
    }
  },
  scales: {
    x: {
      stacked: true,
      ticks: { color: palette.value.text, font: { size: 11 } },
      grid: { display: false }
    },
    y: {
      stacked: true,
      beginAtZero: true,
      ticks: { color: palette.value.text, precision: 0, font: { size: 11 } },
      grid: { color: palette.value.grid }
    }
  }
}))
</script>
