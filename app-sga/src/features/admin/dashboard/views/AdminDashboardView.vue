<template>
  <div class="admin-dashboard-view fade-in">
    <TiHeaderBar
      :active-tab="activeTab"
      :tab-options="tabOptions"
      @update:active-tab="activeTab = $event"
    />

    <TiKpiGrid
      :is-backend-healthy="isBackendHealthy"
      :sistema="sistema"
      :dashboard-data="dashboardData"
      :storage="storage"
      :filter-dias="filterDias"
      :integrity-alerts-count="integrityAlertsCount"
      :has-integrity-alerts="hasIntegrityAlerts"
      @navigate="navigateTo"
    />

    <TiSecurityTab
      v-if="activeTab === 'resumen'"
      :is-admin-ti="authStore.isAdministradorTI"
      :filter-dias="filterDias"
      :filter-modulo="filterModulo"
      :dias-options="diasOptions"
      :modulo-options="moduloOptions"
      :has-active-filters="hasActiveFilters"
      :roles-chart-data="themedRolesChartData"
      :actividad-chart-data="themedActividadChartData"
      :doughnut-options="doughnutOptions"
      :bar-options="barOptions"
      :eventos-recientes="dashboardData?.eventosRecientes || []"
      :sistema="sistema"
      :last-checked-time="lastCheckedTime"
      @update:filter-dias="filterDias = $event"
      @update:filter-modulo="filterModulo = $event"
      @filter-change="handleLoadDashboard"
      @reset-filters="handleResetFilters"
      @navigate="navigateTo"
    />

    <TiStorageInfraTab
      v-else-if="activeTab === 'almacenamiento'"
      :storage="storage"
      :sistema="sistema"
      :has-integrity-alerts="hasIntegrityAlerts"
      :integrity-alerts-count="integrityAlertsCount"
      :storage-entidad-chart-data="themedStorageEntidadChartData"
      :storage-extension-chart-data="themedStorageExtensionChartData"
      :bar-options="barOptions"
      :doughnut-options="doughnutOptions"
      @navigate="navigateTo"
    />

    <TiReportsPanel
      v-else-if="activeTab === 'reportes' && authStore.isAdministradorTI"
      :filter-dias="filterDias"
      :filter-modulo="filterModulo"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useTheme } from '@/shared/composables/useTheme'
import { useNotification } from '@/shared/composables/useNotification'
import { useAdminDashboard } from '../../composables/useAdminDashboard'
import { getTiChartPalette, buildDoughnutOptions, buildBarOptions } from '../composables/tiChartTheme'
import TiHeaderBar from '../components/TiHeaderBar.vue'
import TiKpiGrid from '../components/TiKpiGrid.vue'
import TiSecurityTab from '../components/TiSecurityTab.vue'
import TiStorageInfraTab from '../components/TiStorageInfraTab.vue'
import TiReportsPanel from '../components/TiReportsPanel.vue'

const router = useRouter()
const authStore = useAuthStore()
const navigationStore = useNavigationStore()
const { isDark } = useTheme()
const { showError } = useNotification()

const activeTab = ref('resumen')

const tabOptions = computed(() => {
  const base = [
    { label: 'Resumen y Seguridad', value: 'resumen' },
    { label: 'Almacenamiento e Infra', value: 'almacenamiento' }
  ]
  if (authStore.isAdministradorTI) {
    base.push({ label: 'Centro de Reportes', value: 'reportes' })
  }
  return base
})

const {
  filterDias,
  filterModulo,
  diasOptions,
  moduloOptions,
  lastCheckedTime,
  dashboardData,
  storageDiag,
  sistema,
  isBackendHealthy,
  rolesChartData,
  actividadChartData,
  storageEntidadChartData,
  storageExtensionChartData,
  hasActiveFilters,
  resetFilters,
  loadDashboard
} = useAdminDashboard()

const storage = computed(() => storageDiag.value || {})
const integrityAlertsCount = computed(
  () => (storage.value.archivosFaltantesEnDisco || 0) + (storage.value.archivosHuerfanosEnDisco || 0)
)
const hasIntegrityAlerts = computed(() => integrityAlertsCount.value > 0)

const handleLoadDashboard = async () => {
  try {
    await loadDashboard()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar métricas del Dashboard TI.')
  }
}

const handleResetFilters = () => {
  resetFilters()
  handleLoadDashboard()
}

const navigateTo = (subOption) => {
  navigationStore.setActiveModule('administracion-ti')
  if (subOption === 'create-user') {
    navigationStore.setAdministracionTiSubOption('create-user')
    router.push('/administracion-ti/usuarios')
    return
  }
  navigationStore.setAdministracionTiSubOption(subOption)
  const pathMap = {
    users: '/administracion-ti/usuarios',
    'portal-users': '/administracion-ti/usuarios-portal',
    auditoria: '/administracion-ti/auditoria',
    almacenamiento: '/administracion-ti/almacenamiento',
    about: '/administracion-ti/acerca-de'
  }
  router.push(pathMap[subOption] || '/administracion-ti/dashboard')
}

const palette = computed(() => getTiChartPalette(isDark.value))
const doughnutOptions = computed(() => buildDoughnutOptions(palette.value))
const barOptions = computed(() => buildBarOptions(palette.value))

const themedRolesChartData = computed(() => ({
  ...rolesChartData.value,
  datasets: rolesChartData.value.datasets.map((ds) => ({
    ...ds,
    backgroundColor: palette.value.doughnut,
    borderColor: palette.value.surfaceBorder,
    borderWidth: 2
  }))
}))

const themedActividadChartData = computed(() => ({
  ...actividadChartData.value,
  datasets: actividadChartData.value.datasets.map((ds, idx) => ({
    ...ds,
    backgroundColor: idx === 0 ? palette.value.ok : palette.value.fail,
    borderRadius: 6
  }))
}))

const themedStorageEntidadChartData = computed(() => ({
  ...storageEntidadChartData.value,
  datasets: storageEntidadChartData.value.datasets.map((ds) => ({
    ...ds,
    backgroundColor: palette.value.doughnut[0],
    borderRadius: 6
  }))
}))

const themedStorageExtensionChartData = computed(() => ({
  ...storageExtensionChartData.value,
  datasets: storageExtensionChartData.value.datasets.map((ds) => ({
    ...ds,
    backgroundColor: palette.value.doughnut,
    borderColor: palette.value.surfaceBorder,
    borderWidth: 2
  }))
}))

onMounted(() => {
  handleLoadDashboard()
})
</script>

<style scoped>
.admin-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
</style>
