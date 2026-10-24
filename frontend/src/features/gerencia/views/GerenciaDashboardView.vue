<template>
  <div class="gerencia-dashboard-view">
    <!-- 1. Toolbar de Acciones Ejecutivas Warm Editorial (Sin Banners Invasivos) -->
    <div class="gerencia-action-toolbar">
      <div class="toolbar-left-info">
        <span class="badge-kicker">GERENCIA GENERAL // CONSOLA EJECUTIVA CENTRAL</span>
        <h3 class="toolbar-title">Panel de Control y Monitoreo General</h3>
      </div>

      <div class="toolbar-actions-group">
        <div class="server-status-pill">
          <span class="pulse-indicator"></span>
          <span class="status-pill-text font-mono">OPERACIONES ONLINE</span>
        </div>

        <button
          type="button"
          class="btn-outline-warm"
          @click="loadDashboardData"
          :disabled="loading"
          title="Sincronizar métricas consolidadas"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>

        <button
          type="button"
          class="btn-outline-warm"
          @click="exportSedesPdf"
          :disabled="exportingSedes"
          title="Descargar reporte oficial JasperReports de Sedes"
        >
          <i :class="['pi', exportingSedes ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
          <span>Reporte Sedes</span>
        </button>

        <button
          type="button"
          class="btn-outline-warm"
          @click="exportPuertosPdf"
          :disabled="exportingPuertos"
          title="Descargar catálogo oficial JasperReports de Puertos"
        >
          <i :class="['pi', exportingPuertos ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
          <span>Reporte Puertos</span>
        </button>

        <button
          type="button"
          class="btn-espresso"
          @click="goToSubOption('sedes')"
        >
          <i class="pi pi-building"></i>
          <span>Gestionar Sedes</span>
        </button>
      </div>
    </div>

    <!-- 2. Rejilla de Métricas KPI Ejecutivas (Warm Editorial 4 Columnas) -->
    <div class="metrics-kpi-grid">
      <!-- KPI 01: Sedes Territoriales -->
      <div class="kpi-card" @click="goToSubOption('sedes')">
        <div class="kpi-card-header">
          <span class="kpi-num">01</span>
          <span class="kpi-label">Sedes & Agencias</span>
          <i class="pi pi-building kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ sedesCount }}</span>
          <span class="kpi-badge success">{{ operativasSedesCount }} activas</span>
        </div>
        <div class="kpi-footer-note">
          <span>Jurisdicción en <strong class="font-mono text-muted">{{ distritosCount }}</strong> distritos aduaneros</span>
        </div>
      </div>

      <!-- KPI 02: Puertos y Terminales -->
      <div class="kpi-card" @click="goToSubOption('catalogos')">
        <div class="kpi-card-header">
          <span class="kpi-num">02</span>
          <span class="kpi-label">Puertos & Terminales</span>
          <i class="pi pi-compass kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ puertosCount }}</span>
          <span class="kpi-badge accent">{{ maritimosCount }} marítimos</span>
        </div>
        <div class="kpi-footer-note">
          <span>{{ aereosCount }} aéreos / fluviales · <strong class="font-mono text-muted">{{ cargasCount }}</strong> tipos de carga</span>
        </div>
      </div>

      <!-- KPI 03: Parque Automotor MTC -->
      <div class="kpi-card" @click="goToSubOption('flota')">
        <div class="kpi-card-header">
          <span class="kpi-num">03</span>
          <span class="kpi-label">Parque de Flota</span>
          <i class="pi pi-truck kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ unidadesCount }}</span>
          <span class="kpi-badge info">{{ tractosCount }} tractos</span>
        </div>
        <div class="kpi-footer-note">
          <span><strong class="font-mono text-success">{{ flotaOperativaCount }}</strong> operativas en ruta / patio</span>
        </div>
      </div>

      <!-- KPI 04: Padrón de Conductores -->
      <div class="kpi-card" @click="goToSubOption('conductores')">
        <div class="kpi-card-header">
          <span class="kpi-num">04</span>
          <span class="kpi-label">Conductores MTC</span>
          <i class="pi pi-id-card kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ conductoresCount }}</span>
          <span class="kpi-badge warning">{{ conductoresDisponiblesCount }} disponibles</span>
        </div>
        <div class="kpi-footer-note">
          <span>Acreditados MTC · Licencias profesionales</span>
        </div>
      </div>
    </div>

    <!-- 3. Contenedor Principal: Desglose de Red Territorial + Estado de Flota -->
    <div class="dashboard-main-grid">
      <!-- Columna Izquierda: Red de Sedes Operativas -->
      <div class="dashboard-panel sedes-panel">
        <div class="panel-header-row">
          <div class="panel-header-info">
            <span class="badge-kicker">RED TERRITORIAL // CONTROL OPERATIVO</span>
            <h4 class="panel-title">Agencias y Sedes Aduaneras</h4>
          </div>
          <button
            type="button"
            class="btn-link-action"
            @click="goToSubOption('sedes')"
          >
            <span>Ver Directorio</span>
            <i class="pi pi-arrow-right"></i>
          </button>
        </div>

        <div v-if="sedesList.length === 0" class="empty-panel-box">
          <i class="pi pi-building"></i>
          <span>No hay sedes registradas en el sistema</span>
        </div>

        <div v-else class="sedes-list-flow">
          <div
            v-for="s in sedesList.slice(0, 5)"
            :key="s.idSede"
            class="sede-summary-row"
          >
            <div class="sede-left-meta">
              <span class="sede-code-badge font-mono">{{ s.codigoSede }}</span>
              <div class="sede-names">
                <span class="sede-title">{{ s.nombre || s.nombreSede }}</span>
                <span class="sede-distrito">{{ s.distrito || 'Jurisdicción no asignada' }}</span>
              </div>
            </div>
            <div class="sede-right-meta">
              <span :class="['status-chip', s.estado === 'OPERATIVO' ? 'status-chip-success' : 'status-chip-muted']">
                {{ s.estado || 'OPERATIVO' }}
              </span>
            </div>
          </div>
        </div>

        <div class="panel-footer-meta">
          <span>Sincronización en vivo: <strong class="font-mono">{{ lastCheckedTime }}</strong></span>
          <span class="font-mono">{{ sedesList.length }} sedes registradas</span>
        </div>
      </div>

      <!-- Columna Derecha: Disponibilidad de Flota y Parque Automotor -->
      <div class="dashboard-panel flota-panel">
        <div class="panel-header-row">
          <div class="panel-header-info">
            <span class="badge-kicker">LOGÍSTICA TERRESTRE // GESTIÓN DE ACTIVOS</span>
            <h4 class="panel-title">Capacidad Operativa de Transporte</h4>
          </div>
          <button
            type="button"
            class="btn-link-action"
            @click="goToSubOption('flota')"
          >
            <span>Padrón de Flota</span>
            <i class="pi pi-arrow-right"></i>
          </button>
        </div>

        <!-- Barras de Distribución de Flota -->
        <div class="fleet-breakdown">
          <div class="breakdown-card">
            <div class="breakdown-top">
              <span class="k">Tracto Camiones:</span>
              <span class="v font-mono">{{ tractosCount }} unidades</span>
            </div>
            <div class="progress-bar-bg">
              <div
                class="progress-bar-fill accent"
                :style="{ width: unidadesCount > 0 ? `${(tractosCount / unidadesCount) * 100}%` : '0%' }"
              ></div>
            </div>
          </div>

          <div class="breakdown-card">
            <div class="breakdown-top">
              <span class="k">Plataformas y Carretas:</span>
              <span class="v font-mono">{{ plataformasCount }} unidades</span>
            </div>
            <div class="progress-bar-bg">
              <div
                class="progress-bar-fill info"
                :style="{ width: unidadesCount > 0 ? `${(plataformasCount / unidadesCount) * 100}%` : '0%' }"
              ></div>
            </div>
          </div>

          <div class="breakdown-card">
            <div class="breakdown-top">
              <span class="k">Furgones y Barandas:</span>
              <span class="v font-mono">{{ furgonesCount }} unidades</span>
            </div>
            <div class="progress-bar-bg">
              <div
                class="progress-bar-fill success"
                :style="{ width: unidadesCount > 0 ? `${(furgonesCount / unidadesCount) * 100}%` : '0%' }"
              ></div>
            </div>
          </div>

          <div class="fleet-quick-stats">
            <div class="stat-box">
              <span class="stat-num font-mono text-success">{{ flotaOperativaCount }}</span>
              <span class="stat-lbl">Disponibles / Ruta</span>
            </div>
            <div class="stat-box">
              <span class="stat-num font-mono text-warning">{{ flotaTallerCount }}</span>
              <span class="stat-lbl">En Taller / Mant.</span>
            </div>
            <div class="stat-box">
              <span class="stat-num font-mono">{{ conductoresCount }}</span>
              <span class="stat-lbl">Choferes MTC</span>
            </div>
          </div>
        </div>

        <div class="panel-footer-meta">
          <span>Capacidad de Carga Agregada: <strong class="font-mono">{{ totalCapacidadToneladas }} Tn</strong></span>
          <button
            type="button"
            class="btn-text-action"
            @click="goToSubOption('conductores')"
          >
            Ver Choferes MTC <i class="pi pi-arrow-right"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- 4. Secciones y Sub-opciones de Gerencia General (Acceso Rápido) -->
    <div class="section-container">
      <div class="section-header-row">
        <div>
          <span class="badge-kicker">SUB-MÓDULOS GERENCIALES // NAVEGACIÓN DIRECTA</span>
          <h3 class="section-title">Gobernanza Territorial, Catálogos y Transporte</h3>
        </div>
        <p class="section-desc">
          Acceso centralizado a los módulos operativos de alta dirección del Sistema de Gestión Aduanera.
        </p>
      </div>

      <div class="modules-grid-gerencia">
        <!-- Submódulo 02: Sedes Territoriales -->
        <div class="module-access-card" @click="goToSubOption('sedes')">
          <div class="module-card-top">
            <span class="module-num font-mono">02</span>
            <i class="pi pi-building module-icon"></i>
          </div>
          <h4 class="module-card-title">Sedes & Agencias Territoriales</h4>
          <p class="module-card-desc">
            Directorio maestro de sucursales, terminales de depósito, agencias de aduana y puntos de enlace a nivel nacional.
          </p>
          <div class="module-card-stats">
            <div class="stat-kv">
              <span class="k">Total Sedes:</span>
              <span class="v font-mono">{{ sedesCount }}</span>
            </div>
            <div class="stat-kv">
              <span class="k">Operativas:</span>
              <span class="v font-mono text-success">{{ operativasSedesCount }}</span>
            </div>
          </div>
          <div class="module-card-footer">
            <span class="access-link">Directorio de Sedes <i class="pi pi-arrow-right"></i></span>
          </div>
        </div>

        <!-- Submódulo 03: Catálogos Auxiliares -->
        <div class="module-access-card" @click="goToSubOption('catalogos')">
          <div class="module-card-top">
            <span class="module-num font-mono">03</span>
            <i class="pi pi-compass module-icon"></i>
          </div>
          <h4 class="module-card-title">Catálogos Auxiliares</h4>
          <p class="module-card-desc">
            Tablas normativas de puertos marítimos, aéreos y fluviales, tipos de carga aduanera y clasificación de mercancías.
          </p>
          <div class="module-card-stats">
            <div class="stat-kv">
              <span class="k">Puertos:</span>
              <span class="v font-mono">{{ puertosCount }}</span>
            </div>
            <div class="stat-kv">
              <span class="k">Tipos Carga:</span>
              <span class="v font-mono text-accent">{{ cargasCount }}</span>
            </div>
          </div>
          <div class="module-card-footer">
            <span class="access-link">Ver Tablas Maestras <i class="pi pi-arrow-right"></i></span>
          </div>
        </div>

        <!-- Submódulo 04: Parque de Flota -->
        <div class="module-access-card" @click="goToSubOption('flota')">
          <div class="module-card-top">
            <span class="module-num font-mono">04</span>
            <i class="pi pi-truck module-icon"></i>
          </div>
          <h4 class="module-card-title">Parque Automotor & Flota MTC</h4>
          <p class="module-card-desc">
            Control de tractos, semirremolques, plataformas, vigencia de pólizas SOAT, certificados CITV y habilitaciones técnicas MTC.
          </p>
          <div class="module-card-stats">
            <div class="stat-kv">
              <span class="k">Unidades:</span>
              <span class="v font-mono">{{ unidadesCount }}</span>
            </div>
            <div class="stat-kv">
              <span class="k">Disponibles:</span>
              <span class="v font-mono text-success">{{ flotaOperativaCount }}</span>
            </div>
          </div>
          <div class="module-card-footer">
            <span class="access-link">Control de Flota <i class="pi pi-arrow-right"></i></span>
          </div>
        </div>

        <!-- Submódulo 05: Conductores MTC -->
        <div class="module-access-card" @click="goToSubOption('conductores')">
          <div class="module-card-top">
            <span class="module-num font-mono">05</span>
            <i class="pi pi-id-card module-icon"></i>
          </div>
          <h4 class="module-card-title">Plantilla de Conductores MTC</h4>
          <p class="module-card-desc">
            Padrón de choferes profesionales autorizados, categorías de licencia A-IIIb / A-IIIc y vinculación con la aplicación móvil.
          </p>
          <div class="module-card-stats">
            <div class="stat-kv">
              <span class="k">Choferes:</span>
              <span class="v font-mono">{{ conductoresCount }}</span>
            </div>
            <div class="stat-kv">
              <span class="k">Activos:</span>
              <span class="v font-mono text-success">{{ conductoresDisponiblesCount }}</span>
            </div>
          </div>
          <div class="module-card-footer">
            <span class="access-link">Directorio de Choferes <i class="pi pi-arrow-right"></i></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNavigationStore } from '@/core/stores/navigationStore'
import sedeService from '@/features/admin/sedes/services/sedeService'
import catalogoService from '@/features/admin/catalogos/services/catalogoService'
import { transporteFlotaService } from '@/features/roles-modules/services/transporteFlotaService'
import { downloadBlob } from '@/shared/utils/download'

const router = useRouter()
const navigationStore = useNavigationStore()

const loading = ref(false)
const exportingSedes = ref(false)
const exportingPuertos = ref(false)
const lastCheckedTime = ref('--:--:--')

// Métricas de Sedes
const sedesCount = ref(0)
const operativasSedesCount = ref(0)
const distritosCount = ref(0)
const sedesList = ref([])

// Métricas de Catálogos
const puertosCount = ref(0)
const maritimosCount = ref(0)
const aereosCount = ref(0)
const cargasCount = ref(0)

// Métricas de Flota y Transporte
const unidadesCount = ref(0)
const tractosCount = ref(0)
const plataformasCount = ref(0)
const furgonesCount = ref(0)
const flotaOperativaCount = ref(0)
const flotaTallerCount = ref(0)
const totalCapacidadToneladas = ref(0)

// Métricas de Conductores
const conductoresCount = ref(0)
const conductoresDisponiblesCount = ref(0)

const loadDashboardData = async () => {
  loading.value = true
  try {
    const [sedesRes, puertosRes, cargasRes, unidadesRes, conductoresRes] = await Promise.allSettled([
      sedeService.getSedes(false),
      catalogoService.getPuertos(false),
      catalogoService.getTiposCarga(false),
      transporteFlotaService.getUnidades({ soloActivos: false }),
      transporteFlotaService.getConductores(false)
    ])

    // 1. Sedes
    if (sedesRes.status === 'fulfilled' && Array.isArray(sedesRes.value)) {
      sedesList.value = sedesRes.value
      sedesCount.value = sedesRes.value.length
      operativasSedesCount.value = sedesRes.value.filter((s) => s.estado === 'OPERATIVO').length
      const distSet = new Set(sedesRes.value.map((s) => s.distrito).filter(Boolean))
      distritosCount.value = distSet.size
    }

    // 2. Puertos
    if (puertosRes.status === 'fulfilled') {
      const pData = puertosRes.value?.data || puertosRes.value || []
      const list = Array.isArray(pData) ? pData : []
      puertosCount.value = list.length
      maritimosCount.value = list.filter((p) => (p.tipo || p.tipoPuerto) === 'MARITIMO').length
      aereosCount.value = list.filter((p) => {
        const t = p.tipo || p.tipoPuerto
        return t === 'AEREO' || t === 'FLUVIAL' || t === 'TERRESTRE'
      }).length
    }

    // 3. Tipos de Carga
    if (cargasRes.status === 'fulfilled') {
      const cData = cargasRes.value?.data || cargasRes.value || []
      cargasCount.value = Array.isArray(cData) ? cData.length : 0
    }

    // 4. Unidades de Transporte
    if (unidadesRes.status === 'fulfilled') {
      const uData = unidadesRes.value?.data || unidadesRes.value || []
      const uList = Array.isArray(uData) ? uData : []
      unidadesCount.value = uList.length
      tractosCount.value = uList.filter((u) => u.tipoVehiculo === 'TRACTO').length
      plataformasCount.value = uList.filter((u) => u.tipoVehiculo === 'PLATAFORMA').length
      furgonesCount.value = uList.filter((u) => u.tipoVehiculo === 'FURGON' || u.tipoVehiculo === 'CAMION_BARANDA').length
      flotaOperativaCount.value = uList.filter((u) => u.estadoOperativo === 'DISPONIBLE' || u.estadoOperativo === 'EN_RUTA').length
      flotaTallerCount.value = uList.filter((u) => u.estadoOperativo === 'MANTENIMIENTO' || u.estadoOperativo === 'FUERA_DE_SERVICIO').length

      const totalKg = uList.reduce((acc, u) => acc + (Number(u.capacidadCargaKg) || 0), 0)
      totalCapacidadToneladas.value = Math.round(totalKg / 1000)
    }

    // 5. Conductores
    if (conductoresRes.status === 'fulfilled') {
      const condData = conductoresRes.value?.data || conductoresRes.value || []
      const cList = Array.isArray(condData) ? condData : []
      conductoresCount.value = cList.length
      conductoresDisponiblesCount.value = cList.filter((c) => c.estadoOperativo === 'DISPONIBLE').length
    }

    const now = new Date()
    lastCheckedTime.value = now.toLocaleTimeString()
  } catch (err) {
    console.error('Error cargando métricas ejecutivas de Gerencia General:', err)
  } finally {
    loading.value = false
  }
}

const goToSubOption = (option) => {
  navigationStore.setActiveModule('gerencia-general')
  const pathMap = {
    sedes: '/gerencia-general/sedes',
    puertos: '/gerencia-general/puertos',
    'tipos-carga': '/gerencia-general/tipos-carga',
    mercancias: '/gerencia-general/mercancias',
    catalogos: '/gerencia-general/puertos',
    flota: '/gerencia-general/flota',
    conductores: '/gerencia-general/conductores'
  }
  navigationStore.setGerenciaGeneralSubOption(option)
  const target = pathMap[option] || '/gerencia-general/dashboard'
  router.push(target)
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
    const blob = await catalogoService.downloadPuertosPdf()
    downloadBlob(blob, `reporte-puertos-sga-${new Date().toISOString().slice(0, 10)}.pdf`)
  } catch (err) {
    console.error('Error al exportar puertos PDF:', err)
  } finally {
    exportingPuertos.value = false
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.gerencia-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR EDITORIAL (WARM EDITORIAL)
   ========================================================================== */
.gerencia-action-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-4, 1rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle);
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.badge-kicker {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  text-transform: uppercase;
}

.toolbar-title {
  margin: 0;
  font-size: var(--font-size-lg, 1.125rem);
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.015em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.server-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill, 9999px);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted);
}

.pulse-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-success-text);
  box-shadow: 0 0 0 2px var(--color-success-border);
  animation: pulse-dot 2s infinite ease-in-out;
}

@keyframes pulse-dot {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}

/* ==========================================================================
   2. REJILLA DE TARJETAS KPI (WARM EDITORIAL)
   ========================================================================== */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-3, 0.75rem);
  cursor: pointer;
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.kpi-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-accent-kicker);
  box-shadow: var(--shadow-elevated);
}

.kpi-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kpi-num {
  font-family: var(--font-mono, monospace);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.kpi-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-right: auto;
  margin-left: 0.65rem;
}

.kpi-icon {
  font-size: 1.1rem;
  color: var(--color-text-subtle);
}

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.kpi-badge {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kpi-badge.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  border: 1px solid var(--color-border-subtle);
}

.kpi-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.kpi-badge.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.kpi-badge.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.kpi-footer-note {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-subtle);
  border-top: 1px dashed var(--color-border-subtle);
  padding-top: var(--space-2, 0.5rem);
}

/* ==========================================================================
   3. PANELES DE CONTROL PRINCIPAL (2 COLUMNAS)
   ========================================================================== */
.dashboard-main-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-5, 1.25rem);
}

.dashboard-panel {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.35rem;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-card);
}

.panel-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.15rem;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.panel-title {
  margin: 0.2rem 0 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.01em;
}

.btn-link-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: transparent;
  border: none;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  cursor: pointer;
  transition: gap var(--transition-fast);
}

.btn-link-action:hover {
  gap: 0.55rem;
}

/* Lista de Sedes */
.sedes-list-flow {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  flex: 1;
}

.sede-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.65rem 0.85rem;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm, 6px);
  transition: border-color var(--transition-fast);
}

.sede-summary-row:hover {
  border-color: var(--color-border-strong);
}

.sede-left-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sede-code-badge {
  font-size: 0.725rem;
  font-weight: 800;
  color: var(--color-accent-kicker);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.sede-names {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.sede-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sede-distrito {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
}

.status-chip {
  font-size: 0.675rem;
  font-weight: 800;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  letter-spacing: 0.04em;
}

.status-chip-success {
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  color: var(--color-success-text);
}

.status-chip-muted {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
}

/* Desglose de Flota */
.fleet-breakdown {
  display: flex;
  flex-direction: column;
  gap: 0.95rem;
  flex: 1;
}

.breakdown-card {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.breakdown-top {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
}

.breakdown-top .k {
  color: var(--color-text-muted);
  font-weight: 600;
}

.breakdown-top .v {
  font-weight: 700;
  color: var(--color-text-main);
}

.progress-bar-bg {
  width: 100%;
  height: 6px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-pill, 9999px);
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  border-radius: var(--radius-pill, 9999px);
  transition: width 0.4s ease;
}

.progress-bar-fill.accent {
  background: var(--color-accent-kicker);
}

.progress-bar-fill.info {
  background: var(--color-info-text);
}

.progress-bar-fill.success {
  background: var(--color-success-text);
}

.fleet-quick-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.65rem;
  margin-top: 0.5rem;
}

.stat-box {
  background: var(--color-bg-base);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm, 6px);
  padding: 0.65rem 0.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.2rem;
}

.stat-num {
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.stat-lbl {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
  font-weight: 600;
}

.panel-footer-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.15rem;
  padding-top: 0.75rem;
  border-top: 1px dashed var(--color-border-subtle);
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.btn-text-action {
  background: transparent;
  border: none;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

/* ==========================================================================
   4. SECCIÓN SUB-MÓDULOS DE GERENCIA GENERAL
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

.modules-grid-gerencia {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}

.module-access-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-subtle);
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), border-color var(--transition-fast);
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
  transition: gap var(--transition-fast);
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
  transition: all var(--transition-fast);
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
  transition: all var(--transition-fast);
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.empty-panel-box {
  padding: 2.5rem 1rem;
  text-align: center;
  color: var(--color-text-subtle);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
}

.text-success { color: var(--color-success-text) !important; }
.text-warning { color: var(--color-warning-text) !important; }
.text-accent  { color: var(--color-accent-kicker) !important; }
.text-muted   { color: var(--color-text-muted) !important; }

/* Responsive */
@media (max-width: 1200px) {
  .metrics-kpi-grid,
  .modules-grid-gerencia {
    grid-template-columns: repeat(2, 1fr);
  }
  .dashboard-main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .metrics-kpi-grid,
  .modules-grid-gerencia {
    grid-template-columns: 1fr;
  }
  .toolbar-actions-group {
    width: 100%;
  }
}
</style>
