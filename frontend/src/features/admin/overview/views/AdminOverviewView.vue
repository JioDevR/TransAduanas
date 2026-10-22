<template>
  <div class="admin-overview-view">
    <!-- 1. Hero Banner Principal -->
    <div class="hero-banner-card">
      <div class="hero-content">
        <span class="badge-kicker">PANEL DE CONTROL CENTRAL // TI & AUDITORÍA</span>
        <h2 class="hero-title">Vista General del Sistema de Gestión Aduanera</h2>
        <p class="hero-desc">
          Monitoreo unificado de infraestructura, personal operativo autorizado, asignación de sedes territoriales y catálogos auxiliares de comercio exterior.
        </p>
      </div>
      <div class="hero-watermark">
        <span>SGA</span>
      </div>
    </div>

    <!-- 2. Tarjetas de Métricas Rápidas Consolidadas -->
    <div class="metrics-grid">
      <!-- 01: Usuarios -->
      <div class="metric-card" @click="goToSubOption('users')">
        <div class="metric-top">
          <span class="metric-idx">01</span>
          <span class="metric-label">Usuarios Registrados</span>
        </div>
        <div class="metric-val">{{ usersCount }}</div>
        <div class="metric-sub">
          <span class="sub-highlight">{{ activeUsersCount }} activos</span> · {{ inactiveUsersCount }} inactivos
        </div>
      </div>

      <!-- 02: Sedes -->
      <div class="metric-card" @click="goToSubOption('sedes')">
        <div class="metric-top">
          <span class="metric-idx">02</span>
          <span class="metric-label">Sedes & Agencias</span>
        </div>
        <div class="metric-val">{{ sedesCount }}</div>
        <div class="metric-sub">
          <span class="sub-highlight">{{ operativasSedesCount }} operativas</span> · {{ distritosCount }} distritos
        </div>
      </div>

      <!-- 03: Puertos -->
      <div class="metric-card" @click="goToSubOption('catalogos')">
        <div class="metric-top">
          <span class="metric-idx">03</span>
          <span class="metric-label">Puertos Aduaneros</span>
        </div>
        <div class="metric-val">{{ puertosCount }}</div>
        <div class="metric-sub">
          <span class="sub-highlight">{{ maritimosCount }} marítimos</span> · {{ aereosCount }} aéreos
        </div>
      </div>

      <!-- 04: Tipos de Carga -->
      <div class="metric-card" @click="goToSubOption('catalogos')">
        <div class="metric-top">
          <span class="metric-idx">04</span>
          <span class="metric-label">Tipos de Carga</span>
        </div>
        <div class="metric-val">{{ cargasCount }}</div>
        <div class="metric-sub">
          <span>Clasificación arancelaria y contenedores</span>
        </div>
      </div>
    </div>

    <!-- 3. Módulos y Sub-opciones de Administración con Exportación JasperReports -->
    <div class="suboptions-grid">
      <!-- Subopción 01: Usuarios -->
      <div class="suboption-card">
        <div class="suboption-header">
          <div class="suboption-idx-box">
            <i class="pi pi-users"></i>
          </div>
          <div>
            <span class="badge-kicker">SUB-OPCIÓN 01</span>
            <h3 class="suboption-title">Gestión de Personal & Usuarios</h3>
          </div>
        </div>

        <p class="suboption-desc">
          Administración de cuentas de operadores, asignación de roles institucionales (Comercial, Aduanas, Transporte, Legal, etc.) y auditoría de accesos.
        </p>

        <div class="suboption-stats">
          <div class="stat-pill">
            <span class="stat-k">Total Cuentas:</span>
            <span class="stat-v">{{ usersCount }}</span>
          </div>
          <div class="stat-pill">
            <span class="stat-k">Activas:</span>
            <span class="stat-v stat-v-success">{{ activeUsersCount }}</span>
          </div>
        </div>

        <div class="suboption-actions">
          <button
            type="button"
            class="btn-outline-warm"
            @click="exportUsuariosPdf"
            :disabled="exportingUsers"
            title="Descargar reporte oficial JasperReports en PDF"
          >
            <i :class="['pi', exportingUsers ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
            <span>Exportar Reporte (PDF)</span>
          </button>
          <button
            type="button"
            class="btn-espresso"
            @click="goToSubOption('users')"
          >
            <span>Gestionar Usuarios</span>
            <i class="pi pi-arrow-right"></i>
          </button>
        </div>
      </div>

      <!-- Subopción 02: Sedes -->
      <div class="suboption-card">
        <div class="suboption-header">
          <div class="suboption-idx-box">
            <i class="pi pi-building"></i>
          </div>
          <div>
            <span class="badge-kicker">SUB-OPCIÓN 02</span>
            <h3 class="suboption-title">Infraestructura & Sedes Operativas</h3>
          </div>
        </div>

        <p class="suboption-desc">
          Configuración territorial de sucursales, terminales portuarios, depósitos autorizados y oficinas administrativas a nivel nacional.
        </p>

        <div class="suboption-stats">
          <div class="stat-pill">
            <span class="stat-k">Total Sedes:</span>
            <span class="stat-v">{{ sedesCount }}</span>
          </div>
          <div class="stat-pill">
            <span class="stat-k">Operativas:</span>
            <span class="stat-v stat-v-success">{{ operativasSedesCount }}</span>
          </div>
        </div>

        <div class="suboption-actions">
          <button
            type="button"
            class="btn-outline-warm"
            @click="exportSedesPdf"
            :disabled="exportingSedes"
            title="Descargar reporte oficial JasperReports en PDF"
          >
            <i :class="['pi', exportingSedes ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
            <span>Exportar Reporte (PDF)</span>
          </button>
          <button
            type="button"
            class="btn-espresso"
            @click="goToSubOption('sedes')"
          >
            <span>Gestionar Sedes</span>
            <i class="pi pi-arrow-right"></i>
          </button>
        </div>
      </div>

      <!-- Subopción 03: Catálogos (Puertos y Cargas) -->
      <div class="suboption-card">
        <div class="suboption-header">
          <div class="suboption-idx-box">
            <i class="pi pi-compass"></i>
          </div>
          <div>
            <span class="badge-kicker">SUB-OPCIÓN 03</span>
            <h3 class="suboption-title">Catálogos Auxiliares Aduaneros</h3>
          </div>
        </div>

        <p class="suboption-desc">
          Catálogo maestro de terminales portuarios marítimos, aéreos y terrestres, junto con especificaciones de tara y tipos de contenedores.
        </p>

        <div class="suboption-stats">
          <div class="stat-pill">
            <span class="stat-k">Puertos:</span>
            <span class="stat-v">{{ puertosCount }}</span>
          </div>
          <div class="stat-pill">
            <span class="stat-k">Tipos Carga:</span>
            <span class="stat-v">{{ cargasCount }}</span>
          </div>
        </div>

        <div class="suboption-actions">
          <button
            type="button"
            class="btn-outline-warm"
            @click="exportPuertosPdf"
            :disabled="exportingPuertos"
            title="Descargar reporte oficial JasperReports en PDF"
          >
            <i :class="['pi', exportingPuertos ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
            <span>Exportar Reporte (PDF)</span>
          </button>
          <button
            type="button"
            class="btn-espresso"
            @click="goToSubOption('catalogos')"
          >
            <span>Gestionar Catálogos</span>
            <i class="pi pi-arrow-right"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNavigationStore } from '@/core/stores/navigationStore'
import userService from '@/features/admin/users/services/userService'
import sedeService from '@/features/admin/sedes/services/sedeService'
import catalogoService from '@/features/admin/catalogos/services/catalogoService'
import apiClient from '@/core/api/apiClient'
import { downloadBlob } from '@/shared/utils/download'

const router = useRouter()
const navigationStore = useNavigationStore()

const usersCount = ref(0)
const activeUsersCount = ref(0)
const inactiveUsersCount = ref(0)

const sedesCount = ref(0)
const operativasSedesCount = ref(0)
const distritosCount = ref(0)

const puertosCount = ref(0)
const maritimosCount = ref(0)
const aereosCount = ref(0)
const cargasCount = ref(0)

const exportingUsers = ref(false)
const exportingSedes = ref(false)
const exportingPuertos = ref(false)

const loadOverviewMetrics = async () => {
  try {
    const [users, sedes, puertos, cargas] = await Promise.all([
      userService.getUsers(false).catch(() => []),
      sedeService.getSedes(false).catch(() => []),
      catalogoService.getPuertos().catch(() => ({ data: [] })),
      catalogoService.getTiposCarga().catch(() => ({ data: [] }))
    ])

    // Usuarios
    usersCount.value = users.length
    activeUsersCount.value = users.filter((u) => u.estadoActivo).length
    inactiveUsersCount.value = users.filter((u) => !u.estadoActivo).length

    // Sedes
    sedesCount.value = sedes.length
    operativasSedesCount.value = sedes.filter((s) => s.estado === 'OPERATIVO').length
    const distSet = new Set(sedes.map((s) => s.distrito).filter(Boolean))
    distritosCount.value = distSet.size

    // Puertos
    const pList = puertos.data || puertos || []
    puertosCount.value = pList.length
    maritimosCount.value = pList.filter((p) => p.tipo === 'MARITIMO').length
    aereosCount.value = pList.filter((p) => p.tipo === 'AEREO').length

    // Cargas
    const cList = cargas.data || cargas || []
    cargasCount.value = cList.length
  } catch (err) {
    console.error('Error al cargar métricas de vista general:', err)
  }
}

const goToSubOption = (option) => {
  if (option === 'users') {
    navigationStore.setActiveModule('administracion-ti')
    navigationStore.setAdministracionTiSubOption('users')
    router.push('/administracion-ti/usuarios')
  } else if (option === 'sedes') {
    navigationStore.setActiveModule('gerencia-general')
    navigationStore.setGerenciaGeneralSubOption('sedes')
    router.push('/gerencia-general/sedes')
  } else if (option === 'catalogos') {
    navigationStore.setActiveModule('gerencia-general')
    navigationStore.setGerenciaGeneralSubOption('catalogos')
    router.push('/gerencia-general/catalogos')
  }
}

const exportUsuariosPdf = async () => {
  exportingUsers.value = true
  try {
    const blob = await userService.downloadPdf()
    downloadBlob(blob, `reporte-usuarios-sga-${new Date().toISOString().slice(0, 10)}.pdf`)
  } catch (err) {
    console.error('Error al exportar usuarios PDF:', err)
  } finally {
    exportingUsers.value = false
  }
}

const exportSedesPdf = async () => {
  exportingSedes.value = true
  try {
    const blob = await sedeService.downloadPdf()
    downloadBlob(blob, `reporte-sedes-sga-${new Date().toISOString().slice(0, 10)}.pdf`)
  } catch (err) {
    console.error('Error al exportar sedes PDF:', err)
  } finally {
    exportingSedes.value = false
  }
}

const exportPuertosPdf = async () => {
  exportingPuertos.value = true
  try {
    const blobData = await catalogoService.downloadPuertosPdf()
    downloadBlob(blobData, `reporte-puertos-sga-${new Date().toISOString().slice(0, 10)}.pdf`)
  } catch (err) {
    console.error('Error al exportar puertos PDF:', err)
  } finally {
    exportingPuertos.value = false
  }
}

onMounted(() => {
  loadOverviewMetrics()
})
</script>

<style scoped>
.admin-overview-view {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  animation: fadeIn 0.3s ease-out;
}

/* 1. Hero Banner */
.hero-banner-card {
  position: relative;
  background: linear-gradient(135deg, #1c1917 0%, #292524 60%, #453406 100%);
  border: 1px solid rgba(234, 179, 8, 0.25);
  border-radius: var(--radius-lg, 12px);
  padding: 1.75rem 2rem;
  color: #fafaf9;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.hero-content {
  max-width: 780px;
  position: relative;
  z-index: 2;
}

.badge-kicker {
  display: inline-block;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #eab308;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
}

.hero-title {
  font-size: 1.65rem;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 0.5rem;
  letter-spacing: -0.015em;
  line-height: 1.25;
}

.hero-desc {
  font-size: 0.875rem;
  color: #d6d3d1;
  margin: 0;
  line-height: 1.5;
}

.hero-watermark {
  position: absolute;
  right: 1.5rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 7rem;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.03);
  user-select: none;
  pointer-events: none;
}

/* 2. Métricas Rápidas */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}

.metric-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.metric-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-border-strong);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.metric-top {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  margin-bottom: 0.65rem;
}

.metric-idx {
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--color-accent-kicker);
}

.metric-label {
  font-size: 0.775rem;
  font-weight: 600;
  color: var(--color-text-subtle);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.metric-val {
  font-size: 2.25rem;
  font-weight: 800;
  color: var(--color-text-main);
  font-variant-numeric: tabular-nums;
  line-height: 1;
  margin-bottom: 0.5rem;
}

.metric-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.sub-highlight {
  color: var(--color-success-text);
  font-weight: 700;
}

/* 3. Grid de Sub-opciones */
.suboptions-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

.suboption-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  transition: all var(--transition-fast);
}

.suboption-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
}

.suboption-header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}

.suboption-idx-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-accent-kicker);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  flex-shrink: 0;
}

.suboption-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  line-height: 1.3;
}

.suboption-desc {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0 0 1.25rem;
}

.suboption-stats {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.3rem 0.65rem;
  border-radius: var(--radius-pill);
  font-size: 0.75rem;
}

.stat-k {
  color: var(--color-text-subtle);
}

.stat-v {
  font-weight: 700;
  color: var(--color-text-main);
}

.stat-v-success {
  color: var(--color-success-text);
}

.suboption-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.suboption-actions button {
  flex: 1;
  justify-content: center;
}

@media (max-width: 1200px) {
  .suboptions-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  .suboption-actions {
    flex-direction: column;
  }
}
</style>
