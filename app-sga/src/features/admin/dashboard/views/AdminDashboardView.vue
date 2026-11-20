<template>
  <div class="admin-dashboard-view fade-in">
    <!-- 1. Barra de Estado y Acciones sgaUI (Título único en Topbar) -->
    <header class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag
          :value="isBackendHealthy ? `API ONLINE · ${pingLatency}ms` : 'API DEGRADADA'"
          :severity="isBackendHealthy ? 'success' : 'danger'"
          rounded
        />
        <span class="sga-kpi-pill sga-mono">Sync {{ lastCheckedTime }}</span>
      </div>

      <div class="sga-dash-actions">
        <Button
          label="Nuevo Usuario"
          icon="pi pi-user-plus"
          size="small"
          @click="navigateTo('create-user')"
        />
        <Button
          label="PDF Personal"
          icon="pi pi-file-pdf"
          severity="secondary"
          outlined
          size="small"
          :loading="exportingPdf"
          @click="handleExportPdf"
        />
        <Button
          label="PDF Portal"
          icon="pi pi-file-pdf"
          severity="secondary"
          outlined
          size="small"
          :loading="exportingPortalPdf"
          @click="handleExportPortalPdf"
        />
        <Button
          v-if="authStore.isAdministradorTI"
          label="Auditoría"
          icon="pi pi-shield"
          severity="secondary"
          outlined
          size="small"
          @click="navigateTo('auditoria')"
        />
        <Button
          v-if="authStore.isAdministradorTI"
          label="Logs TI"
          icon="pi pi-code"
          severity="secondary"
          outlined
          size="small"
          @click="navigateTo('logs')"
        />
        <Button
          label="Actualizar"
          icon="pi pi-sync"
          severity="secondary"
          outlined
          size="small"
          :loading="loading"
          @click="loadDashboard"
        />
      </div>
    </header>

    <!-- 2. Filtros del Dashboard (sgaUI) -->
    <section v-if="authStore.isAdministradorTI" class="sga-card-surface sga-filters-card">
      <div class="sga-filters-grid">
        <div class="sga-filter-field">
          <label class="sga-field-label">Ventana de Actividad</label>
          <Select
            v-model="filterDias"
            :options="diasOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @change="loadDashboard"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Módulo Auditado</label>
          <Select
            v-model="filterModulo"
            :options="moduloOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @change="loadDashboard"
          />
        </div>

        <div v-if="hasActiveFilters" class="sga-filter-reset">
          <Button
            label="Restablecer"
            icon="pi pi-filter-slash"
            severity="secondary"
            text
            size="small"
            @click="handleResetFilters"
          />
        </div>
      </div>
    </section>

    <!-- 3. Rejilla de 6 KPIs Técnicos Accionables (sgaUI) -->
    <section class="sga-kpis-grid">
      <article
        class="sga-kpi-card"
        :class="isBackendHealthy ? 'is-success' : 'is-danger'"
        @click="loadDashboard"
      >
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">API &amp; Base de Datos</span>
          <i :class="['pi', isBackendHealthy ? 'pi-check-circle' : 'pi-times-circle', 'sga-kpi-icon']"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">
            {{ isBackendHealthy ? 'OPERATIVO' : 'DEGRADADO' }}
          </span>
          <span class="sga-kpi-pill" :class="isBackendHealthy ? 'is-success' : 'is-danger'">
            BD {{ sistema.bdLatenciaMs ?? 0 }}ms
          </span>
        </div>
        <p class="sga-kpi-foot">
          {{ sistema.bdMotor || 'MySQL' }} · Uptime <strong class="sga-mono">{{ formatUptime(sistema.uptimeSegundos) }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card" @click="navigateTo('users')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">Personal Interno</span>
          <i class="pi pi-users sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ dashboardData?.usuariosTotal ?? 0 }}</span>
          <span class="sga-kpi-pill is-success">{{ dashboardData?.usuariosActivos ?? 0 }} activos</span>
        </div>
        <p class="sga-kpi-foot">
          Baja: <strong class="sga-mono">{{ dashboardData?.usuariosInactivos ?? 0 }}</strong> ·
          Admin TI: <strong class="sga-mono">{{ dashboardData?.distribucionRoles?.ADMINISTRADOR_TI ?? 0 }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card" @click="navigateTo('portal-users')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">Cuentas Portal B2B</span>
          <i class="pi pi-id-card sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ dashboardData?.portalUsuariosTotal ?? 0 }}</span>
          <span class="sga-kpi-pill">{{ dashboardData?.portalEmpresasVinculadas ?? 0 }} empresas</span>
        </div>
        <p class="sga-kpi-foot">
          Activas: <strong class="sga-mono">{{ dashboardData?.portalUsuariosActivos ?? 0 }}</strong> ·
          Baja: <strong class="sga-mono">{{ dashboardData?.portalUsuariosInactivos ?? 0 }}</strong> ·
          Cambio clave: <strong class="sga-mono">{{ dashboardData?.portalCambioPendiente ?? 0 }}</strong>
        </p>
      </article>

      <article
        class="sga-kpi-card"
        :class="(dashboardData?.loginsFallidosPeriodo || 0) > 0 ? 'is-warn' : ''"
        @click="navigateTo('auditoria')"
      >
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Auditoría ({{ filterDias }}d)</span>
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
          Éxitos: <strong class="sga-mono">{{ dashboardData?.auditoriaExitosPeriodo ?? 0 }}</strong> ·
          Alertas: <strong class="sga-mono">{{ dashboardData?.auditoriaFallosPeriodo ?? 0 }}</strong>
        </p>
      </article>

      <article class="sga-kpi-card" @click="navigateTo('almacenamiento')">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">05</span>
          <span class="sga-kpi-label">JVM &amp; Almacenamiento</span>
          <i class="pi pi-database sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ sistema.jvmHeapPorcentaje ?? 0 }}%</span>
          <span class="sga-kpi-pill">
            {{ formatBytes(sistema.storageTamanoBytes) }}
          </span>
        </div>
        <p class="sga-kpi-foot">
          Heap <strong class="sga-mono">{{ sistema.jvmHeapUsadoMb ?? 0 }}/{{ sistema.jvmHeapMaximoMb ?? 0 }} MB</strong> ·
          {{ sistema.storageArchivosDisco ?? 0 }} archivos
        </p>
      </article>

      <article
        class="sga-kpi-card"
        :class="(dashboardData?.logErroresLineas || 0) > 0 ? 'is-warn' : ''"
        @click="navigateTo('logs')"
      >
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">06</span>
          <span class="sga-kpi-label">Bitácora de Logs</span>
          <i class="pi pi-file-code sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">{{ formatBytes(dashboardData?.logSizeBytes) }}</span>
          <span
            class="sga-kpi-pill"
            :class="(dashboardData?.logErroresLineas || 0) > 0 ? 'is-warn' : ''"
          >
            {{ dashboardData?.logErroresLineas ?? 0 }} errores
          </span>
        </div>
        <p class="sga-kpi-foot">
          {{ dashboardData?.logArchivoNombre || 'sga-backend.log' }} · <strong class="sga-mono">{{ dashboardData?.logTotalLineas ?? 0 }}</strong> trazas
        </p>
      </article>
    </section>

    <!-- 4. Gráficos Chart.js (sgaUI) -->
    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">CONTROL DE ACCESOS</span>
            <h3 class="sga-chart-title">Operadores por Rol Institucional</h3>
          </div>
          <Button
            label="Ver Usuarios"
            icon="pi pi-users"
            severity="secondary"
            text
            size="small"
            @click="navigateTo('users')"
          />
        </div>
        <div class="sga-chart-box sga-doughnut-wrap">
          <Chart
            v-if="rolesChartData.labels.length > 0"
            type="doughnut"
            :data="themedRolesChartData"
            :options="doughnutOptions"
            class="sga-chart-canvas"
          />
          <div v-if="rolesChartData.labels.length > 0" class="sga-doughnut-center">
            <span class="sga-doughnut-pct">{{ activeUsersPct }}%</span>
            <span class="sga-doughnut-sub">Activos</span>
          </div>
          <div v-else class="sga-empty-state">
            <i class="pi pi-users sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin operadores registrados</p>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">SEGURIDAD &amp; AUDITORÍA</span>
            <h3 class="sga-chart-title">Eventos por Día ({{ filterDias }} días)</h3>
          </div>
          <Button
            v-if="authStore.isAdministradorTI"
            label="Ver Bitácora"
            icon="pi pi-arrow-right"
            iconPos="right"
            severity="secondary"
            text
            size="small"
            @click="navigateTo('auditoria')"
          />
        </div>
        <div class="sga-chart-box">
          <Chart
            v-if="actividadChartData.labels.length > 0"
            type="bar"
            :data="themedActividadChartData"
            :options="barOptions"
            class="sga-chart-canvas"
          />
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-bar sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin actividad registrada en el período</p>
          </div>
        </div>
      </article>
    </section>

    <!-- 5. Detalle Operativo: Eventos Recientes + Estado Real de Infraestructura -->
    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">TRAZABILIDAD EN VIVO</span>
            <h3 class="sga-chart-title">Últimos Eventos de Auditoría</h3>
          </div>
          <Button
            v-if="authStore.isAdministradorTI"
            label="Explorar todo"
            severity="secondary"
            outlined
            size="small"
            @click="navigateTo('auditoria')"
          />
        </div>

        <DataTable
          :value="dashboardData?.eventosRecientes || []"
          size="small"
          stripedRows
          responsiveLayout="scroll"
        >
          <template #empty>
            <div class="sga-empty-state">
              <i class="pi pi-history sga-empty-icon"></i>
              <p class="sga-empty-desc">Aún no hay eventos registrados.</p>
            </div>
          </template>
          <Column header="Fecha / Actor">
            <template #body="{ data }">
              <div class="ti-event-actor">
                <span class="sga-mono">{{ data.usuarioActor }}</span>
                <small class="sga-text-muted">{{ formatFecha(data.fecha) }}</small>
              </div>
            </template>
          </Column>
          <Column header="Acción">
            <template #body="{ data }">
              <div class="ti-event-action">
                <span>{{ data.accion }}</span>
                <small class="sga-text-muted">{{ data.detalle || data.modulo }}</small>
              </div>
            </template>
          </Column>
          <Column header="Estado" style="width: 100px">
            <template #body="{ data }">
              <Tag
                :value="data.resultado"
                :severity="data.resultado === 'EXITO' ? 'success' : 'danger'"
                rounded
              />
            </template>
          </Column>
        </DataTable>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">DIAGNÓSTICO DEL SERVIDOR</span>
            <h3 class="sga-chart-title">Infraestructura Verificada</h3>
          </div>
          <span class="sga-kpi-pill sga-mono">Sync {{ lastCheckedTime }}</span>
        </div>

        <div class="ti-infra-list">
          <div class="ti-infra-row">
            <div>
              <strong>Motor Base de Datos</strong>
              <p class="sga-text-muted">{{ sistema.bdMotor || '—' }} {{ sistema.bdVersion || '' }}</p>
            </div>
            <Tag
              :value="sistema.bdConectada ? `UP · ${sistema.bdLatenciaMs}ms` : 'DOWN'"
              :severity="sistema.bdConectada ? 'success' : 'danger'"
              rounded
            />
          </div>

          <div class="ti-infra-row">
            <div>
              <strong>Almacenamiento Local</strong>
              <p class="sga-text-muted sga-mono">{{ sistema.storageDirectorio || './uploads/documentos' }}</p>
            </div>
            <Tag
              :value="sistema.storageDisponible ? `${sistema.storageArchivosDisco ?? 0} archivos` : 'NO DISP.'"
              :severity="sistema.storageDisponible ? 'success' : 'warn'"
              rounded
            />
          </div>

          <div class="ti-infra-row">
            <div>
              <strong>Runtime &amp; Versión API</strong>
              <p class="sga-text-muted">Java {{ sistema.javaVersion || '21' }} · {{ sistema.osNombre || 'Linux' }}</p>
            </div>
            <Tag :value="`v${sistema.appVersion || '0.5.1'} (${sistema.perfilActivo || 'default'})`" severity="info" rounded />
          </div>

          <div class="ti-infra-row">
            <div>
              <strong>Capacidad de Disco</strong>
              <p class="sga-text-muted">
                Libre: <span class="sga-mono">{{ sistema.discoEspacioLibreMb ?? 0 }} MB</span> /
                Total: <span class="sga-mono">{{ sistema.discoEspacioTotalMb ?? 0 }} MB</span>
              </p>
            </div>
            <Button
              label="Ficha Técnica"
              icon="pi pi-info-circle"
              severity="secondary"
              outlined
              size="small"
              @click="navigateTo('about')"
            />
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useTheme } from '@/shared/composables/useTheme'
import { useNotification } from '@/shared/composables/useNotification'
import { openBlobInNewTab } from '@/shared/utils/download'
import { userService } from '../../users/services/userService'
import { portalUserService } from '../../portal-users/services/portalUserService'
import { useAdminDashboard, formatBytes, formatUptime } from '../../composables/useAdminDashboard'

const router = useRouter()
const authStore = useAuthStore()
const navigationStore = useNavigationStore()
const { isDark } = useTheme()
const { showError } = useNotification()
const exportingPdf = ref(false)
const exportingPortalPdf = ref(false)

const handleExportPdf = async () => {
  if (exportingPdf.value) return
  exportingPdf.value = true
  try {
    const blobData = await userService.downloadPdf(true)
    openBlobInNewTab(blobData, 'reporte-usuarios-activos.pdf')
  } catch {
    showError('No se pudo generar el reporte PDF de usuarios internos.')
  } finally {
    exportingPdf.value = false
  }
}

const handleExportPortalPdf = async () => {
  if (exportingPortalPdf.value) return
  exportingPortalPdf.value = true
  try {
    const blobData = await portalUserService.downloadPdf(true)
    openBlobInNewTab(blobData, 'reporte-usuarios-portal-activos.pdf')
  } catch {
    showError('No se pudo generar el reporte PDF del portal B2B.')
  } finally {
    exportingPortalPdf.value = false
  }
}

const {
  loading,
  filterDias,
  filterModulo,
  diasOptions,
  moduloOptions,
  pingLatency,
  lastCheckedTime,
  dashboardData,
  sistema,
  isBackendHealthy,
  rolesChartData,
  actividadChartData,
  activeUsersPct,
  hasActiveFilters,
  resetFilters,
  loadDashboard
} = useAdminDashboard()

const handleResetFilters = () => {
  resetFilters()
  loadDashboard()
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
    logs: '/administracion-ti/logs',
    about: '/administracion-ti/acerca-de'
  }
  router.push(pathMap[subOption] || '/administracion-ti/dashboard')
}

const formatFecha = (iso) => {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const palette = computed(() =>
  isDark.value
    ? {
        text: '#c4bcab',
        grid: 'rgba(255, 255, 255, 0.08)',
        surfaceBorder: '#2b2622',
        doughnut: ['#f0b892', '#5cb874', '#38bdf8', '#eab308', '#a78bfa', '#fb923c', '#f87171', '#94a3b8'],
        ok: '#5cb874',
        fail: '#f87171'
      }
    : {
        text: '#6f6356',
        grid: '#ede6db',
        surfaceBorder: '#ffffff',
        doughnut: ['#d48252', '#2e7d32', '#21598f', '#8c5b16', '#6366f1', '#c2413b', '#0d9488', '#6f6356'],
        ok: '#2e7d32',
        fail: '#9e2a2b'
      }
)

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

const doughnutOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '68%',
  plugins: {
    legend: {
      position: 'bottom',
      labels: { color: palette.value.text, boxWidth: 10, padding: 12, font: { size: 11 } }
    }
  }
}))

const barOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top',
      align: 'end',
      labels: { color: palette.value.text, boxWidth: 10, font: { size: 11 } }
    }
  },
  scales: {
    x: { grid: { display: false }, ticks: { color: palette.value.text } },
    y: { beginAtZero: true, ticks: { precision: 0, color: palette.value.text }, grid: { color: palette.value.grid } }
  }
}))

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.admin-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.ti-event-actor,
.ti-event-action {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.ti-infra-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ti-infra-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md, 8px);
}

.ti-infra-row p {
  margin: 0.15rem 0 0;
  font-size: 0.78rem;
}
</style>
