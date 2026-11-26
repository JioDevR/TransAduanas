<template>
  <div class="gerencia-dashboard-view fade-in">
    <!-- Cabecera ejecutiva directa (sgaUI: título único en .sga-topbar) -->
    <header class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag
          :value="`Hola, ${nombreUsuario}`"
          icon="pi pi-user"
          severity="secondary"
          rounded
        />
        <Tag
          :value="`${metrics.sedesOperativasCount}/${metrics.sedesCount} sedes operativas`"
          severity="success"
          class="sga-mono"
          rounded
        />
        <Tag
          v-if="lastUpdatedAt"
          :value="`Actualizado ${lastUpdatedAt}`"
          icon="pi pi-clock"
          severity="contrast"
          class="sga-mono"
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
          @click="loadDashboardData"
        />
        <Button
          label="PDF Sedes"
          icon="pi pi-file-pdf"
          severity="secondary"
          outlined
          size="small"
          :loading="exportingType === 'sedes'"
          @click="exportCatalogoPdf('sedes')"
        />
        <Button
          label="PDF Partidas"
          icon="pi pi-file-pdf"
          severity="secondary"
          outlined
          size="small"
          :loading="exportingType === 'mercancias'"
          @click="exportCatalogoPdf('mercancias')"
        />
        <Button
          label="Gestionar Sedes"
          icon="pi pi-building"
          size="small"
          @click="goToSubOption('sedes')"
        />
      </div>
    </header>

    <div v-if="failedSources.length > 0" class="sga-banner is-warn">
      <i class="pi pi-exclamation-triangle"></i>
      <span>Algunos indicadores no pudieron cargarse ({{ failedSources.join(', ') }}). Use Actualizar para reintentar.</span>
    </div>

    <!-- KPIs Ejecutivos de los 4 Maestros de Gerencia General -->
    <section class="sga-kpis-grid">
      <article class="sga-kpi-card is-primary" @click="goToSubOption('sedes')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">Sedes &amp; Agencias</span>
          <i class="pi pi-building sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value sga-mono">{{ metrics.sedesCount }}</span>
          <span class="sga-kpi-pill is-success">{{ metrics.sedesOperativasCount }} operativas</span>
        </div>
        <p class="sga-kpi-foot">
          Cobertura en <strong class="sga-mono">{{ metrics.distritosCount }}</strong> distritos
        </p>
      </article>

      <article class="sga-kpi-card is-info" @click="goToSubOption('puertos')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">Puertos &amp; Terminales</span>
          <i class="pi pi-compass sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value sga-mono">{{ metrics.puertosCount }}</span>
          <span class="sga-kpi-pill is-info">{{ metrics.puertosMaritimosCount }} marítimos</span>
        </div>
        <p class="sga-kpi-foot">
          <strong class="sga-mono">{{ metrics.puertosAereosCount }}</strong> aéreos ·
          <strong class="sga-mono">{{ metrics.puertosActivosCount }}</strong> activos
        </p>
      </article>

      <article class="sga-kpi-card is-success" @click="goToSubOption('tipos-carga')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">Tipos de Carga</span>
          <i class="pi pi-box sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value sga-mono">{{ metrics.cargasCount }}</span>
          <span class="sga-kpi-pill is-success">{{ metrics.cargasActivasCount }} activos</span>
        </div>
        <p class="sga-kpi-foot">Clasificaciones operativas vigentes</p>
      </article>

      <article class="sga-kpi-card is-warn" @click="goToSubOption('mercancias')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Mercancías &amp; Partidas</span>
          <i class="pi pi-tags sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value sga-mono">{{ metrics.mercanciasCount }}</span>
          <span class="sga-kpi-pill is-warn">{{ metrics.mercanciasVuceCount }} con VUCE</span>
        </div>
        <p class="sga-kpi-foot">
          Habilitadas: <strong class="sga-mono">{{ metrics.mercanciasActivasCount }}</strong> partidas
        </p>
      </article>
    </section>

    <!-- Gráficos Analíticos Chart.js -->
    <GerenciaChartsPanel :metrics="metrics" />

    <!-- Directorio de Sub-opciones y puente a Transporte -->
    <GerenciaSubmodulesGrid
      :metrics="metrics"
      :exporting-type="exportingType"
      @navigate="goToSubOption"
      @export-pdf="exportCatalogoPdf"
    />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useGerenciaDashboard } from '../composables/useGerenciaDashboard'
import GerenciaChartsPanel from '../components/GerenciaChartsPanel.vue'
import GerenciaSubmodulesGrid from '../components/GerenciaSubmodulesGrid.vue'

const authStore = useAuthStore()

const nombreUsuario = computed(() => {
  const raw = authStore.user?.nombres || authStore.user?.nombre || authStore.user?.username || 'Gerencia'
  return String(raw).trim().split(/\s+/)[0] || 'Gerencia'
})

const {
  loading,
  exportingType,
  failedSources,
  lastUpdatedAt,
  metrics,
  loadDashboardData,
  goToSubOption,
  exportCatalogoPdf
} = useGerenciaDashboard()

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.gerencia-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding-bottom: 2.5rem;
  width: 100%;
}
</style>
