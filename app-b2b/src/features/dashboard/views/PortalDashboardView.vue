<template>
  <div class="portal-dashboard-view fade-in">
    <div v-if="hasPartialErrors && !loading" class="sga-banner is-warn">
      <i class="pi pi-info-circle"></i>
      <span>Algunos módulos del resumen no pudieron cargarse completamente.</span>
    </div>

    <!-- Estado inicial de carga -->
    <div v-if="loading && !lastUpdatedAt" class="dashboard-skeletons">
      <div class="sga-card-surface skeleton-box is-banner"></div>
      <div class="sga-kpis-grid">
        <div v-for="n in 4" :key="n" class="sga-kpi-card skeleton-box is-kpi"></div>
      </div>
      <div class="sga-charts-grid">
        <div class="sga-card-surface skeleton-box is-panel"></div>
        <div class="sga-card-surface skeleton-box is-panel"></div>
      </div>
    </div>

    <template v-else>
      <!-- 1. Datos de la Empresa Cliente -->
      <DashboardCompanyBanner :empresa="empresaInfo" />

      <!-- 2. KPIs Ejecutivos (01..04) -->
      <DashboardKpiGrid
        :cotizacion-stats="cotizacionStats"
        :expediente-stats="expedienteStats"
        :cobros-stats="cobrosStats"
        :alertas-totales="alertasTotales"
        @navigate="navigate"
      />

      <!-- 3. Gráficos y Resumen de Cotizaciones + Expedientes Aduaneros -->
      <section class="sga-charts-grid" aria-label="Cotizaciones y expedientes">
        <DashboardCotizacionesCard
          :stats="cotizacionStats"
          @navigate="navigate"
        />
        <DashboardExpedientesCard
          :stats="expedienteStats"
          @navigate="navigate"
        />
      </section>

      <!-- 4. Cobros, Amortización y Obligaciones Pendientes -->
      <DashboardCobrosCard
        :stats="cobrosStats"
        @navigate="navigate"
      />
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePortalDashboard } from '../composables/usePortalDashboard'
import DashboardCompanyBanner from '../components/DashboardCompanyBanner.vue'
import DashboardKpiGrid from '../components/DashboardKpiGrid.vue'
import DashboardCotizacionesCard from '../components/DashboardCotizacionesCard.vue'
import DashboardExpedientesCard from '../components/DashboardExpedientesCard.vue'
import DashboardCobrosCard from '../components/DashboardCobrosCard.vue'

const router = useRouter()

const {
  loading,
  partialErrors,
  lastUpdatedAt,
  empresaInfo,
  cotizacionStats,
  expedienteStats,
  cobrosStats,
  alertasTotales,
  loadDashboard
} = usePortalDashboard()

const hasPartialErrors = computed(() => Object.keys(partialErrors.value || {}).length > 0)

function navigate(path) {
  router.push(path)
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.portal-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
  font-family: var(--font-sans);
}

.dashboard-skeletons {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.skeleton-box {
  position: relative;
  overflow: hidden;
  background: var(--color-bg-surface);
  opacity: 0.7;
}

.skeleton-box.is-banner {
  height: 112px;
}

.skeleton-box.is-kpi {
  height: 120px;
}

.skeleton-box.is-panel {
  height: 360px;
}
</style>
