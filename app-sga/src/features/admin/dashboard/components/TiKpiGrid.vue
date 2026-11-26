<template>
  <section class="sga-kpis-grid ti-kpi-grid">
    <article
      class="sga-kpi-card"
      :class="isBackendHealthy ? 'is-success' : 'is-danger'"
      @click="$emit('navigate', 'about')"
    >
      <div class="sga-kpi-head">
        <span class="sga-kpi-index">01</span>
        <span class="sga-kpi-label">Salud del Sistema</span>
        <i :class="['pi', isBackendHealthy ? 'pi-check-circle' : 'pi-times-circle', 'sga-kpi-icon']"></i>
      </div>
      <div class="sga-kpi-stat">
        <span class="sga-kpi-value is-compact">
          {{ isBackendHealthy ? 'Operativo' : 'Degradado' }}
        </span>
        <span class="sga-kpi-pill" :class="isBackendHealthy ? 'is-success' : 'is-danger'">
          BD {{ sistema.bdLatenciaMs ?? 0 }} ms
        </span>
      </div>
      <p class="sga-kpi-foot">
        {{ sistema.bdMotor || 'MySQL' }} · Heap <strong>{{ sistema.jvmHeapPorcentaje ?? 0 }}%</strong> · Uptime
        <strong>{{ formatUptime(sistema.uptimeSegundos) }}</strong>
      </p>
    </article>

    <article class="sga-kpi-card" @click="$emit('navigate', 'users')">
      <div class="sga-kpi-head">
        <span class="sga-kpi-index">02</span>
        <span class="sga-kpi-label">Cuentas y Accesos</span>
        <i class="pi pi-users sga-kpi-icon"></i>
      </div>
      <div class="sga-kpi-stat">
        <span class="sga-kpi-value">{{ totalCuentasActivas }}</span>
        <span class="sga-kpi-pill is-success">
          {{ dashboardData?.usuariosTotal ?? 0 }} internos · {{ dashboardData?.portalUsuariosTotal ?? 0 }} B2B
        </span>
      </div>
      <p class="sga-kpi-foot">
        Activas: <strong>{{ dashboardData?.usuariosActivos ?? 0 }}</strong> int /
        <strong>{{ dashboardData?.portalUsuariosActivos ?? 0 }}</strong> B2B ·
        <strong>{{ dashboardData?.portalEmpresasVinculadas ?? 0 }}</strong> empresas
      </p>
    </article>

    <article
      class="sga-kpi-card"
      :class="(dashboardData?.loginsFallidosPeriodo || 0) > 0 ? 'is-warn' : ''"
      @click="$emit('navigate', 'auditoria')"
    >
      <div class="sga-kpi-head">
        <span class="sga-kpi-index">03</span>
        <span class="sga-kpi-label">Seguridad y Auditoría ({{ filterDias }}d)</span>
        <i class="pi pi-shield sga-kpi-icon"></i>
      </div>
      <div class="sga-kpi-stat">
        <span class="sga-kpi-value">{{ dashboardData?.auditoriaTotalPeriodo ?? 0 }}</span>
        <span
          class="sga-kpi-pill"
          :class="(dashboardData?.loginsFallidosPeriodo || 0) > 0 ? 'is-warn' : 'is-success'"
        >
          {{ dashboardData?.loginsFallidosPeriodo ?? 0 }} fallos login
        </span>
      </div>
      <p class="sga-kpi-foot">
        Éxitos: <strong>{{ dashboardData?.auditoriaExitosPeriodo ?? 0 }}</strong> ·
        Alertas: <strong>{{ dashboardData?.auditoriaFallosPeriodo ?? 0 }}</strong>
      </p>
    </article>

    <article
      class="sga-kpi-card"
      :class="hasIntegrityAlerts ? 'is-warn' : 'is-success'"
      @click="$emit('navigate', 'almacenamiento')"
    >
      <div class="sga-kpi-head">
        <span class="sga-kpi-index">04</span>
        <span class="sga-kpi-label">Almacenamiento e Integridad</span>
        <i class="pi pi-database sga-kpi-icon"></i>
      </div>
      <div class="sga-kpi-stat">
        <span class="sga-kpi-value">{{ storage.discoOcupacionPct ?? 0 }}%</span>
        <span class="sga-kpi-pill" :class="hasIntegrityAlerts ? 'is-warn' : 'is-success'">
          {{ integrityAlertsCount === 0 ? 'Sincronizado' : `${integrityAlertsCount} alertas` }}
        </span>
      </div>
      <p class="sga-kpi-foot">
        {{ formatBytes(storage.storageTamanoBytes ?? sistema.storageTamanoBytes) }} ·
        {{ storage.registrosBdActivos ?? sistema.storageDocumentosRegistradosBd ?? 0 }} docs BD ·
        {{ storage.archivosEnDisco ?? sistema.storageArchivosDisco ?? 0 }} disco
      </p>
    </article>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { formatBytes, formatUptime } from '../../composables/useAdminDashboard'

const props = defineProps({
  isBackendHealthy: { type: Boolean, default: false },
  sistema: { type: Object, default: () => ({}) },
  dashboardData: { type: Object, default: () => ({}) },
  storage: { type: Object, default: () => ({}) },
  filterDias: { type: Number, default: 7 },
  integrityAlertsCount: { type: Number, default: 0 },
  hasIntegrityAlerts: { type: Boolean, default: false }
})

defineEmits(['navigate'])

const totalCuentasActivas = computed(
  () => (props.dashboardData?.usuariosActivos || 0) + (props.dashboardData?.portalUsuariosActivos || 0)
)
</script>

<style scoped>
.ti-kpi-grid {
  font-variant-numeric: tabular-nums;
}
</style>
