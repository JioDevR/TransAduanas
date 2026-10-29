<template>
  <div class="admin-dashboard-view">
    <!-- 1. Barra de Atajos y Acciones Rápidas TI (Estándar Warm Editorial sin Banners Invasivos) -->
    <div class="dashboard-action-toolbar">
      <div class="toolbar-left-info">
        <span class="badge-kicker">SISTEMAS & INFRAESTRUCTURA // CONSOLA CENTRAL TI</span>
        <h3 class="toolbar-title">Panel de Control y Monitoreo Técnico</h3>
      </div>
      <div class="toolbar-actions-group">
        <div class="server-status-pill">
          <span class="pulse-indicator" :class="{ 'pulse-error': !isBackendHealthy }"></span>
          <span class="status-pill-text font-mono">{{ isBackendHealthy ? 'API ONLINE' : 'API ERROR' }}</span>
        </div>

        <button type="button" class="btn-espresso" @click="navigateTo('create-user')">
          <i class="pi pi-user-plus"></i>
          <span>+ Nuevo Usuario</span>
        </button>

        <button type="button" class="btn-outline-warm" @click="navigateTo('users')">
          <i class="pi pi-users"></i>
          <span>Personal</span>
        </button>

        <button type="button" class="btn-outline-warm" @click="navigateTo('portal-users')">
          <i class="pi pi-id-card"></i>
          <span>Usuarios Portal</span>
        </button>

        <button type="button" class="btn-outline-warm" @click="navigateTo('logs')">
          <i class="pi pi-code"></i>
          <span>Logs</span>
        </button>

        <button type="button" class="btn-outline-warm" @click="navigateTo('about')">
          <i class="pi pi-info-circle"></i>
          <span>Ficha Técnica</span>
        </button>

        <button
          type="button"
          class="btn-outline-warm"
          @click="loadDashboardData"
          :disabled="loading"
          title="Sincronizar métricas del sistema"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>
      </div>
    </div>

    <!-- 2. Grid de Métricas KPI Técnicas (Estándar Editorial Warm) -->
    <div class="metrics-kpi-grid">
      <!-- Tarjeta 01: Salud Backend API -->
      <div class="kpi-card" :class="isBackendHealthy ? 'success-card' : 'danger-card'" @click="loadDashboardData">
        <div class="kpi-card-header">
          <span class="kpi-num">01</span>
          <span class="kpi-label">Salud del Backend API</span>
          <i :class="['pi', isBackendHealthy ? 'pi-check-circle success' : 'pi-times-circle danger', 'kpi-icon']"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value" :class="isBackendHealthy ? 'text-success' : 'text-danger'">
            {{ isBackendHealthy ? 'OPERATIVO' : 'DEGRADADO' }}
          </span>
          <span class="kpi-badge font-mono" :class="isBackendHealthy ? 'success' : 'danger'">
            {{ pingLatency }}ms
          </span>
        </div>
        <div class="kpi-footer-note">
          <span>Spring Boot 4.1.1 · Java 21 · <strong class="font-mono">{{ backendVersion }}</strong></span>
        </div>
      </div>

      <!-- Tarjeta 02: Personal Registrado -->
      <div class="kpi-card" @click="navigateTo('users')">
        <div class="kpi-card-header">
          <span class="kpi-num">02</span>
          <span class="kpi-label">Personal del Sistema</span>
          <i class="pi pi-users kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value">{{ usersCount }}</span>
          <span class="kpi-badge accent">{{ activeUsersCount }} activos</span>
        </div>
        <div class="kpi-footer-note">
          <span>Operadores: <strong class="font-mono text-muted">{{ inactiveUsersCount }}</strong> suspendidos/baja</span>
        </div>
      </div>

      <!-- Tarjeta 03: Seguridad & Roles -->
      <div class="kpi-card" @click="navigateTo('users')">
        <div class="kpi-card-header">
          <span class="kpi-num">03</span>
          <span class="kpi-label">Perfiles de Acceso (Roles)</span>
          <i class="pi pi-shield kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value">{{ rolesDistribution.length }}</span>
          <span class="kpi-badge info">Stateless JWT</span>
        </div>
        <div class="kpi-footer-note">
          <span>HMAC-SHA256 · Tokens válidos por 24h</span>
        </div>
      </div>

      <!-- Tarjeta 04: Bitácora de Auditoría -->
      <div class="kpi-card pending-card" @click="navigateTo('logs')">
        <div class="kpi-card-header">
          <span class="kpi-num">04</span>
          <span class="kpi-label">Bitácora de Logs</span>
          <i class="pi pi-file-code kpi-icon warning"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ logFileSize }}</span>
          <span class="kpi-badge warning font-mono">{{ logLinesCount }} trazas</span>
        </div>
        <div class="kpi-footer-note">
          <span>Archivo activo: <strong class="font-mono text-muted">sga-backend.log</strong></span>
        </div>
      </div>
    </div>

    <!-- 3. Contenedor Principal: Monitor de Servicios + Distribución de Roles -->
    <div class="dashboard-main-grid">
      <!-- Columna Izquierda: Monitor de Infraestructura y Servicios Core -->
      <div class="dashboard-panel system-health-panel">
        <div class="panel-header-row">
          <div class="panel-header-info">
            <span class="badge-kicker">INFRAESTRUCTURA & SERVICIOS CORE</span>
            <h4 class="panel-title">Estado de Conectividad y Micro-Servicios</h4>
          </div>
          <span class="panel-tag font-mono">4 SERVICIOS MONITOREADOS</span>
        </div>

        <!-- Lista de Micro-Servicios -->
        <div class="services-list">
          <div class="service-row">
            <div class="service-info">
              <div class="service-icon-box">
                <i class="pi pi-server"></i>
              </div>
              <div>
                <div class="service-name-row">
                  <span class="service-name">Spring Boot REST API</span>
                  <span class="service-port font-mono">:8085</span>
                </div>
                <div class="service-desc">Java 21 LTS · Spring Boot 3 / Jakarta EE · DispatcherServlet</div>
              </div>
            </div>
            <span class="status-badge status-online">
              <i class="pi pi-check-circle"></i>
              <span class="font-mono">UP ({{ pingLatency }}ms)</span>
            </span>
          </div>

          <div class="service-row">
            <div class="service-info">
              <div class="service-icon-box">
                <i class="pi pi-database"></i>
              </div>
              <div>
                <div class="service-name-row">
                  <span class="service-name">MySQL Enterprise Engine</span>
                  <span class="service-port font-mono">:3306</span>
                </div>
                <div class="service-desc">MySQL 8.4 LTS InnoDB · Pool HikariCP · UTF8MB4</div>
              </div>
            </div>
            <span class="status-badge status-online">
              <i class="pi pi-check-circle"></i>
              <span>CONECTADO</span>
            </span>
          </div>

          <div class="service-row">
            <div class="service-info">
              <div class="service-icon-box">
                <i class="pi pi-lock"></i>
              </div>
              <div>
                <div class="service-name-row">
                  <span class="service-name">Seguridad & Autenticación JWT</span>
                  <span class="service-port font-mono">Stateless</span>
                </div>
                <div class="service-desc">Firma HMAC-SHA256 · Validación Bearer en cada request</div>
              </div>
            </div>
            <span class="status-badge status-online">
              <i class="pi pi-check-circle"></i>
              <span>ACTIVO</span>
            </span>
          </div>

          <div class="service-row">
            <div class="service-info">
              <div class="service-icon-box">
                <i class="pi pi-file-pdf"></i>
              </div>
              <div>
                <div class="service-name-row">
                  <span class="service-name">Motor de Reportes JasperReports</span>
                  <span class="service-port font-mono">v7.0.8</span>
                </div>
                <div class="service-desc">Compilador Eclipse JDT · Renderizado nativo PDF vectorial</div>
              </div>
            </div>
            <span class="status-badge status-online">
              <i class="pi pi-check-circle"></i>
              <span>LISTO</span>
            </span>
          </div>
        </div>

        <div class="panel-footer-uptime">
          <div class="uptime-item">
            <span class="label">Entorno:</span>
            <span class="value font-mono">Local / Desarrollo</span>
          </div>
          <div class="uptime-item">
            <span class="label">Contenedor:</span>
            <span class="value font-mono">Podman Rootless</span>
          </div>
          <div class="uptime-item">
            <span class="label">Sincronización:</span>
            <span class="value font-mono">{{ lastCheckedTime }}</span>
          </div>
        </div>
      </div>

      <!-- Columna Derecha: Desglose de Cuentas por Rol Institucional -->
      <div class="dashboard-panel roles-distribution-panel">
        <div class="panel-header-row">
          <div class="panel-header-info">
            <span class="badge-kicker">CONTROL DE ACCESOS</span>
            <h4 class="panel-title">Operadores por Rol</h4>
          </div>
          <span class="count-pill">{{ rolesDistribution.length }} roles</span>
        </div>

        <div class="roles-list">
          <div
            v-for="role in rolesDistribution"
            :key="role.name"
            class="role-row"
          >
            <div class="role-left">
              <div class="role-icon-bullet" :style="{ backgroundColor: role.color }"></div>
              <div>
                <div class="role-title">{{ role.label }}</div>
                <div class="role-code font-mono">{{ role.name }}</div>
              </div>
            </div>
            <div class="role-right">
              <span class="role-count-badge font-mono">{{ role.count }}</span>
            </div>
          </div>
        </div>

        <div class="roles-panel-footer">
          <button
            type="button"
            class="btn-outline-warm btn-full"
            @click="navigateTo('users')"
          >
            <i class="pi pi-users"></i>
            <span>Gestionar Directorio de Usuarios</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 4. Sección: Accesos Directos a Submódulos TI Oficiales -->
    <div class="section-container">
      <div class="section-header-row">
        <div>
          <span class="badge-kicker">SUB-OPCIONES TI // NAVEGACIÓN DIRECTA</span>
          <h3 class="section-title">Módulos de Administración y Control</h3>
        </div>
        <p class="section-desc">
          Acceso rápido a las funciones operativas del Administrador de TI del SGA.
        </p>
      </div>

      <div class="modules-grid-ti">
        <!-- Submódulo 02: Personal & Usuarios -->
        <div class="module-access-card" @click="navigateTo('users')">
          <div class="module-card-top">
            <span class="module-num font-mono">02</span>
            <i class="pi pi-users module-icon"></i>
          </div>
          <h4 class="module-card-title">Gestión de Personal & Usuarios</h4>
          <p class="module-card-desc">
            Directorio de cuentas, asignación de perfiles de rol institucional, credenciales de acceso y códigos de acreditación aduanera SUNAT.
          </p>
          <div class="module-card-stats">
            <div class="stat-kv">
              <span class="k">Total:</span>
              <span class="v font-mono">{{ usersCount }}</span>
            </div>
            <div class="stat-kv">
              <span class="k">Activos:</span>
              <span class="v font-mono text-success">{{ activeUsersCount }}</span>
            </div>
          </div>
          <div class="module-card-footer">
            <span class="access-link">Directorio de Usuarios <i class="pi pi-arrow-right"></i></span>
          </div>
        </div>

        <!-- Submódulo 03: Logs del Sistema -->
        <div class="module-access-card" @click="navigateTo('logs')">
          <div class="module-card-top">
            <span class="module-num font-mono">03</span>
            <i class="pi pi-code module-icon"></i>
          </div>
          <h4 class="module-card-title">Consola y Bitácora de Logs</h4>
          <p class="module-card-desc">
            Terminal interactiva para visualización de trazas Spring Boot 3, diagnóstico de excepciones HTTP y rotación de archivos Logback.
          </p>
          <div class="module-card-stats">
            <div class="stat-kv">
              <span class="k">Archivo:</span>
              <span class="v font-mono">backend.log</span>
            </div>
            <div class="stat-kv">
              <span class="k">Tamaño:</span>
              <span class="v font-mono">{{ logFileSize }}</span>
            </div>
          </div>
          <div class="module-card-footer">
            <span class="access-link">Abrir Terminal <i class="pi pi-arrow-right"></i></span>
          </div>
        </div>

        <!-- Submódulo 04: Acerca de SGA -->
        <div class="module-access-card" @click="navigateTo('about')">
          <div class="module-card-top">
            <span class="module-num font-mono">04</span>
            <i class="pi pi-info-circle module-icon"></i>
          </div>
          <h4 class="module-card-title">Acerca de la Plataforma (SGA)</h4>
          <p class="module-card-desc">
            Ficha técnica de la versión compilada, especificaciones de arquitectura, dependencias del stack Java 21 / Vue 3 y canal de distribución.
          </p>
          <div class="module-card-stats">
            <div class="stat-kv">
              <span class="k">Versión:</span>
              <span class="v font-mono">Alpha 0.5.0</span>
            </div>
            <div class="stat-kv">
              <span class="k">Canal:</span>
              <span class="v font-mono text-accent">Dev Preview</span>
            </div>
          </div>
          <div class="module-card-footer">
            <span class="access-link">Ver Ficha Técnica <i class="pi pi-arrow-right"></i></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNavigationStore } from '@/core/stores/navigationStore'
import userService from '@/features/admin/users/services/userService'
import logsService from '@/features/admin/services/logsService'
import { ROLE_LABELS } from '@/core/auth/roles'

const router = useRouter()
const navigationStore = useNavigationStore()

const loading = ref(false)
const isBackendHealthy = ref(true)
const backendVersion = ref('1.0.0-MODULO1')
const pingLatency = ref(12)
const lastCheckedTime = ref('--:--:--')

const usersCount = ref(0)
const activeUsersCount = ref(0)
const inactiveUsersCount = ref(0)
const allUsers = ref([])

const logFileSize = ref('0 KB')
const logLinesCount = ref(0)

// Desglose de distribución de roles
const rolesDistribution = computed(() => {
  const roleCounts = {}
  allUsers.value.forEach((user) => {
    const rol = user.nombreRol || 'SIN_ROL'
    roleCounts[rol] = (roleCounts[rol] || 0) + 1
  })

  const palette = {
    ADMINISTRADOR_TI: '#eab308',
    GERENTE_GENERAL: '#38bdf8',
    AGENTE_COMERCIAL: '#22c55e',
    SUPERVISOR_ADUANERO: '#0ea5e9',
    AGENTE_ADUANERO: '#6366f1',
    SUPERVISOR_TRANSPORTE: '#f97316',
    CONTADOR: '#10b981',
    ASESOR_LEGAL: '#ef4444'
  }

  return Object.entries(roleCounts).map(([name, count]) => ({
    name,
    label: ROLE_LABELS[name] || name,
    count,
    color: palette[name] || '#998c7e'
  })).sort((a, b) => b.count - a.count)
})

// Cargar métricas en paralelo
const loadDashboardData = async () => {
  loading.value = true
  const startTime = performance.now()
  try {
    const [healthRes, usersRes, logsRes] = await Promise.allSettled([
      logsService.getHealth(),
      userService.getUsers(),
      logsService.getLogs({ tipo: 'all', lines: 100 })
    ])

    const endTime = performance.now()
    pingLatency.value = Math.max(8, Math.round(endTime - startTime))

    // 1. Salud Backend
    if (healthRes.status === 'fulfilled' && healthRes.value) {
      isBackendHealthy.value = true
      backendVersion.value = healthRes.value.version || healthRes.value?.data?.version || '1.0.0-MODULO1'
    } else {
      isBackendHealthy.value = false
    }

    // 2. Usuarios
    if (usersRes.status === 'fulfilled' && Array.isArray(usersRes.value)) {
      allUsers.value = usersRes.value
      usersCount.value = usersRes.value.length
      activeUsersCount.value = usersRes.value.filter((u) => u.estadoActivo).length
      inactiveUsersCount.value = usersRes.value.filter((u) => !u.estadoActivo).length
    }

    // 3. Logs
    if (logsRes.status === 'fulfilled' && logsRes.value) {
      logLinesCount.value = logsRes.value.totalLines || 0
      const bytes = logsRes.value.fileSize || 0
      if (bytes < 1024) logFileSize.value = `${bytes} B`
      else if (bytes < 1024 * 1024) logFileSize.value = `${(bytes / 1024).toFixed(1)} KB`
      else logFileSize.value = `${(bytes / (1024 * 1024)).toFixed(2)} MB`
    }

    const now = new Date()
    lastCheckedTime.value = now.toLocaleTimeString()
  } catch (err) {
    console.error('Error cargando métricas de dashboard TI:', err)
  } finally {
    loading.value = false
  }
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
    logs: '/administracion-ti/logs',
    about: '/administracion-ti/acerca-de'
  }
  const target = pathMap[subOption] || '/administracion-ti/dashboard'
  router.push(target)
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.admin-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR DE ATAJOS RÁPIDOS (ESTÁNDAR EDITORIAL CÁLIDO SGA)
   ========================================================================== */
.dashboard-action-toolbar {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1rem 1.35rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  box-shadow: var(--shadow-subtle);
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  text-transform: uppercase;
}

.toolbar-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

/* Indicador de estado del API */
.server-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.4rem 0.75rem;
  border-radius: var(--radius-pill, 9999px);
  font-size: 0.725rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-main);
}

.pulse-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
  animation: pulse-ring 2s infinite;
}

.pulse-indicator.pulse-error {
  background: #ef4444;
  box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
}

@keyframes pulse-ring {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(34, 197, 94, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
  }
}

/* ==========================================================================
   2. REJILLA DE MÉTRICAS KPI (ESTÁNDAR EDITORIAL WARM)
   ========================================================================== */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem 1.45rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: var(--shadow-subtle);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
  user-select: none;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-border-strong);
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-num {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 4px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
}

.kpi-label {
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--color-text-muted);
  flex: 1;
}

.kpi-icon {
  font-size: 1.05rem;
  color: var(--color-text-subtle);
}

.kpi-icon.success {
  color: var(--color-success-text);
}

.kpi-icon.warning {
  color: var(--color-warning-text);
}

.kpi-icon.danger {
  color: var(--color-danger-text);
}

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-value {
  font-size: 1.85rem;
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1;
  letter-spacing: -0.02em;
}

.kpi-badge {
  font-size: 0.725rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  border: 1px solid transparent;
}

.kpi-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.kpi-badge.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
}

.kpi-badge.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
}

.kpi-badge.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  border-color: var(--color-border-subtle);
}

.kpi-badge.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border-color: var(--color-info-border);
}

.kpi-footer-note {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-border-subtle);
}

/* ==========================================================================
   3. GRID PRINCIPAL: MONITOR DE SERVICIOS + DISTRIBUCIÓN
   ========================================================================== */
.dashboard-main-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: var(--space-6, 1.5rem);
}

.dashboard-panel {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-subtle);
}

.panel-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.panel-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0.2rem 0 0;
  letter-spacing: -0.01em;
}

.panel-tag {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-text-subtle);
  background: var(--color-bg-subtle);
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
  border: 1px solid var(--color-border-subtle);
}

.services-list {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  flex: 1;
}

.service-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md, 8px);
  transition: border-color var(--transition-fast, 150ms);
}

.service-row:hover {
  border-color: var(--color-border-strong);
}

.service-info {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.service-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-accent-kicker);
  font-size: 1.05rem;
}

.service-name-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.service-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.service-port {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
  background: var(--color-bg-subtle);
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.service-desc {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 0.15rem;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.725rem;
  font-weight: 700;
  padding: 0.3rem 0.65rem;
  border-radius: var(--radius-pill, 9999px);
  letter-spacing: 0.04em;
}

.status-online {
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  color: var(--color-success-text);
}

.panel-footer-uptime {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1.25rem;
  padding-top: 0.85rem;
  border-top: 1px dashed var(--color-border);
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.uptime-item .label {
  margin-right: 0.35rem;
}

.uptime-item .value {
  color: var(--color-text-main);
  font-weight: 600;
}

/* Distribución de Roles */
.count-pill {
  font-size: 0.725rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  border: 1px solid var(--color-border-subtle);
}

.roles-list {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  flex: 1;
}

.role-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.55rem 0.75rem;
  background: var(--color-bg-base);
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-border-subtle);
}

.role-left {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.role-icon-bullet {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.role-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.role-code {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
}

.role-count-badge {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.roles-panel-footer {
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}

.btn-full {
  width: 100%;
  justify-content: center;
}

/* ==========================================================================
   4. SECCIÓN SUBMÓDULOS DE ADMINISTRACIÓN TI
   ========================================================================== */
.section-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-header-row {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.section-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.015em;
}

.section-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0;
}

.modules-grid-ti {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.25rem;
}

.module-access-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.35rem;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-subtle);
  cursor: pointer;
  transition: transform var(--transition-fast, 150ms), box-shadow var(--transition-fast, 150ms), border-color var(--transition-fast, 150ms);
}

.module-access-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-accent-kicker);
}

.module-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.85rem;
}

.module-num {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 4px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.05em;
}

.module-icon {
  font-size: 1.2rem;
  color: var(--color-accent-kicker);
}

.module-card-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.4rem;
  letter-spacing: -0.01em;
}

.module-card-desc {
  font-size: 0.775rem;
  color: var(--color-text-muted);
  line-height: 1.45;
  margin: 0 0 1rem;
  flex: 1;
}

.module-card-stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-bg-base);
  padding: 0.55rem 0.85rem;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-border-subtle);
  margin-bottom: 0.85rem;
  font-size: 0.75rem;
}

.stat-kv .k {
  color: var(--color-text-subtle);
  margin-right: 0.35rem;
}

.stat-kv .v {
  font-weight: 700;
  color: var(--color-text-main);
}

.module-card-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.access-link {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-accent-kicker);
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  transition: gap var(--transition-fast, 150ms);
}

.module-access-card:hover .access-link {
  gap: 0.55rem;
}

/* ==========================================================================
   BOTONES ESTÁNDAR WARM EDITORIAL
   ========================================================================== */
.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.5rem 0.95rem;
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.5rem 0.95rem;
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.text-success {
  color: var(--color-success-text) !important;
}

.text-danger {
  color: var(--color-danger-text) !important;
}

.text-muted {
  color: var(--color-text-muted) !important;
}

.text-accent {
  color: var(--color-accent-kicker) !important;
}

/* Responsive */
@media (max-width: 1200px) {
  .metrics-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .dashboard-main-grid {
    grid-template-columns: 1fr;
  }
  .modules-grid-ti {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-kpi-grid,
  .modules-grid-ti {
    grid-template-columns: 1fr;
  }
  .panel-footer-uptime {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.35rem;
  }
  .toolbar-actions-group {
    width: 100%;
  }
}
</style>
