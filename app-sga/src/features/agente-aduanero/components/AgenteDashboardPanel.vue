<template>
  <div class="agente-dashboard-panel fade-in">
    <!-- 1. Cabecera Ejecutiva del Dashboard -->
    <div class="dashboard-hero-header">
      <div class="hero-text">
        <div class="kicker-row">
          <span class="badge-kicker">OPERACIONES ADUANERAS // AGENTE EN CAMPO</span>
          <span class="status-live-pill">
            <span class="live-dot"></span>
            <span>MONITOREO EN TIEMPO REAL</span>
          </span>
        </div>
        <h2 class="hero-title">Panel de Control del Agente Aduanero</h2>
        <p class="hero-subtitle">
          Supervisión de despachos asignados, trazabilidad de arribo, actas de diligencia ante SUNAT y autorizaciones de retiro.
        </p>
      </div>

      <div class="hero-actions">
        <!-- Selector de supervisión si es admin/supervisor -->
        <div v-if="esSupervisorOAdmin" class="supervisor-wrap">
          <label for="dash-agente-sel" class="supervisor-lbl">
            <i class="pi pi-user text-xs"></i>
            <span>Auditar:</span>
          </label>
          <select
            id="dash-agente-sel"
            :value="selectedAgenteId"
            class="supervisor-sel"
            @change="$emit('update:selectedAgenteId', $event.target.value ? Number($event.target.value) : null)"
          >
            <option :value="null">★ Todos los Agentes</option>
            <option
              v-for="ag in agentesList"
              :key="ag.idUsuario"
              :value="ag.idUsuario"
            >
              {{ ag.nombre }} {{ ag.apellidos || '' }}
            </option>
          </select>
        </div>

        <button
          type="button"
          class="btn-refresh"
          title="Actualizar datos"
          :disabled="loading"
          @click="$emit('refresh')"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>
      </div>
    </div>

    <!-- 2. Los 4 Recuadros KPI Principales (Dashboard Inicial) -->
    <div class="dashboard-section-block">
      <div class="section-badge-title">
        <i class="pi pi-th-large"></i>
        <span>1. ESTADO GENERAL DE DESPACHOS ASIGNADOS</span>
      </div>

      <div class="dashboard-kpis-grid">
        <!-- KPI 01: Total Asignados -->
        <div
          class="dash-kpi-card accent-card clickable"
          @click="$emit('navigate', 'todos')"
          title="Ver todos los expedientes asignados en la bandeja"
        >
          <div class="kpi-top">
            <span class="kpi-index">01</span>
            <span class="kpi-label">Total Asignados</span>
            <i class="pi pi-folder-open kpi-icon accent"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-accent">{{ counts.todos }}</span>
            <span class="kpi-pill accent">En Bandeja</span>
          </div>
          <div class="kpi-footer">
            <span>Ver listado completo</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>

        <!-- KPI 02: En Tránsito / Arribo -->
        <div
          class="dash-kpi-card pending-card clickable"
          @click="$emit('navigate', 'transito')"
          title="Ver despachos en tránsito internacional o recojo"
        >
          <div class="kpi-top">
            <span class="kpi-index">02</span>
            <span class="kpi-label">En Tránsito / Arribo</span>
            <i class="pi pi-compass kpi-icon pending"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-pending">{{ counts.transito }}</span>
            <span class="kpi-pill pending">Nave / Recojo</span>
          </div>
          <div class="kpi-footer">
            <span>Aguardando arribo o DAM</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>

        <!-- KPI 03: Inspección / Aforo SUNAT -->
        <div
          class="dash-kpi-card warning-card clickable"
          @click="$emit('navigate', 'inspeccion')"
          title="Ver despachos en canal Naranja o Rojo que requieren aforo"
        >
          <div class="kpi-top">
            <span class="kpi-index">03</span>
            <span class="kpi-label">Inspección / Aforo</span>
            <i class="pi pi-search-plus kpi-icon warning"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-warning">{{ counts.inspeccion }}</span>
            <span class="kpi-pill warning">Canal Naranja/Rojo</span>
          </div>
          <div class="kpi-footer">
            <span>Diligencia pendiente</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>

        <!-- KPI 04: Levante Concedido -->
        <div
          class="dash-kpi-card success-card clickable"
          @click="$emit('navigate', 'levante')"
          title="Ver mercancías liberadas listas para entrega o embarque"
        >
          <div class="kpi-top">
            <span class="kpi-index">04</span>
            <span class="kpi-label">Levante Concedido</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-success">{{ counts.levante }}</span>
            <span class="kpi-pill success">Listo Salida</span>
          </div>
          <div class="kpi-footer">
            <span>Retiro / Embarque</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. Distribución y Semáforo de Canales SUNAT -->
    <div class="dashboard-analytics-grid">
      <!-- Desglose por Régimen -->
      <div class="analytics-card">
        <div class="card-head">
          <i class="pi pi-chart-pie card-icon"></i>
          <h4>Distribución por Régimen</h4>
        </div>
        <div class="regimen-stats">
          <div class="stat-row">
            <div class="stat-info">
              <span class="stat-name">Importación para el Consumo</span>
              <span class="stat-val font-mono">{{ countImportacion }} ({{ getPercent(countImportacion, counts.todos) }}%)</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill import" :style="{ width: `${getPercent(countImportacion, counts.todos)}%` }"></div>
            </div>
          </div>

          <div class="stat-row">
            <div class="stat-info">
              <span class="stat-name">Exportación Definitiva</span>
              <span class="stat-val font-mono">{{ countExportacion }} ({{ getPercent(countExportacion, counts.todos) }}%)</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill export" :style="{ width: `${getPercent(countExportacion, counts.todos)}%` }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Semáforo de Canales SUNAT -->
      <div class="analytics-card">
        <div class="card-head">
          <i class="pi pi-shield card-icon"></i>
          <h4>Semáforo de Canales SUNAT</h4>
        </div>
        <div class="canal-grid">
          <div class="canal-badge-box verde">
            <span class="canal-lbl">VERDE</span>
            <span class="canal-num font-mono">{{ countCanal('VERDE') }}</span>
            <span class="canal-sub">Levante Inmediato</span>
          </div>
          <div class="canal-badge-box naranja">
            <span class="canal-lbl">NARANJA</span>
            <span class="canal-num font-mono">{{ countCanal('NARANJA') }}</span>
            <span class="canal-sub">Revisión Documental</span>
          </div>
          <div class="canal-badge-box rojo">
            <span class="canal-lbl">ROJO</span>
            <span class="canal-num font-mono">{{ countCanal('ROJO') }}</span>
            <span class="canal-sub">Aforo Físico</span>
          </div>
          <div class="canal-badge-box pendiente">
            <span class="canal-lbl">PENDIENTE</span>
            <span class="canal-num font-mono">{{ countCanal('PENDIENTE') }}</span>
            <span class="canal-sub">Por Numerar</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. Despachos Prioritarios que requieren Acción -->
    <div class="dashboard-section-block">
      <div class="section-badge-title">
        <i class="pi pi-bolt"></i>
        <span>2. DESPACHOS QUE REQUIEREN ACCIÓN INMEDIATA</span>
      </div>

      <div v-if="despachosPrioritarios.length === 0" class="empty-priority-card">
        <i class="pi pi-check-circle text-success text-xl"></i>
        <span>No hay despachos con incidencias o aforos pendientes en este momento. Todos los trámites se encuentran al día.</span>
      </div>

      <div v-else class="priority-list">
        <div
          v-for="exp in despachosPrioritarios"
          :key="exp.idExpediente"
          class="priority-item-card"
        >
          <div class="item-left">
            <div class="item-code font-mono font-bold">{{ exp.codigoExpediente }}</div>
            <div class="item-client">{{ exp.clienteRazonSocial || 'Cliente' }}</div>
            <div class="item-meta">
              <span class="meta-tag">{{ (exp.regimenAduanero || '').toUpperCase().includes('EXPORT') ? 'EXPORTACIÓN' : 'IMPORTACIÓN' }}</span>
              <span v-if="exp.numeroDam" class="meta-tag font-mono">DAM: {{ exp.numeroDam }}</span>
            </div>
          </div>

          <div class="item-center">
            <span
              v-if="exp.canalSunat === 'ROJO' || exp.canalSunat === 'NARANJA'"
              class="priority-badge alert"
            >
              <i class="pi pi-exclamation-triangle"></i>
              Requiere Diligencia {{ exp.canalSunat }}
            </span>
            <span
              v-else-if="exp.estadoExpediente === 'LEVANTE_AUTORIZADO'"
              class="priority-badge success"
            >
              <i class="pi pi-check"></i>
              Levante Otorgado (Listo Salida)
            </span>
            <span v-else class="priority-badge info">
              <i class="pi pi-info-circle"></i>
              {{ exp.estadoExpediente }}
            </span>
          </div>

          <div class="item-right">
            <button
              type="button"
              class="btn-action-goto"
              @click="$emit('navigate', getTabForExp(exp))"
            >
              <span>Ir al Despacho</span>
              <i class="pi pi-arrow-right"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  expedientes: {
    type: Array,
    default: () => []
  },
  counts: {
    type: Object,
    default: () => ({
      todos: 0,
      transito: 0,
      inspeccion: 0,
      levante: 0
    })
  },
  selectedAgenteId: {
    type: [Number, String, null],
    default: null
  },
  agentesList: {
    type: Array,
    default: () => []
  },
  esSupervisorOAdmin: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['navigate', 'refresh', 'update:selectedAgenteId'])

const countImportacion = computed(() => {
  return (props.expedientes || []).filter((e) =>
    (e.regimenAduanero || '').toUpperCase().includes('IMPORT')
  ).length
})

const countExportacion = computed(() => {
  return (props.expedientes || []).filter((e) =>
    (e.regimenAduanero || '').toUpperCase().includes('EXPORT')
  ).length
})

const countCanal = (canal) => {
  return (props.expedientes || []).filter((e) => {
    if (canal === 'PENDIENTE') {
      return !e.canalSunat || e.canalSunat === 'PENDIENTE'
    }
    return e.canalSunat === canal
  }).length
}

const getPercent = (count, total) => {
  if (!total || total === 0) return 0
  return Math.min(100, Math.round((count / total) * 100))
}

const despachosPrioritarios = computed(() => {
  return (props.expedientes || [])
    .filter((e) => {
      const enAforo = e.canalSunat === 'ROJO' || e.canalSunat === 'NARANJA' || e.estadoExpediente === 'INSPECCION'
      const listoRetiro = e.estadoExpediente === 'LEVANTE_AUTORIZADO' && !e.idDespachoTransporte
      return enAforo || listoRetiro
    })
    .slice(0, 5)
})

const getTabForExp = (exp) => {
  if (exp.canalSunat === 'ROJO' || exp.canalSunat === 'NARANJA' || exp.estadoExpediente === 'INSPECCION') {
    return 'inspeccion'
  }
  if (exp.estadoExpediente === 'LEVANTE_AUTORIZADO') {
    return 'levante'
  }
  return 'todos'
}
</script>

<style scoped>
.agente-dashboard-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding-bottom: 2rem;
}

/* 1. Hero Header */
.dashboard-hero-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.25rem 1.5rem;
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  flex-wrap: wrap;
  gap: 1rem;
}

.hero-text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.25rem;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-family: var(--font-mono, monospace);
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #e0c287);
  text-transform: uppercase;
}

.status-live-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.15rem 0.5rem;
  background: rgba(16, 185, 129, 0.15);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-success-text, #7ee79d);
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  animation: pulse-dot 2s infinite ease-in-out;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.85); }
}

.hero-title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--color-text-main, #f6f2ea);
  letter-spacing: -0.01em;
}

.hero-subtitle {
  margin: 0.15rem 0 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-subtle, #968e7e);
  max-width: 750px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.supervisor-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
}

.supervisor-lbl {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-accent-kicker, #e0c287);
}

.supervisor-sel {
  border: none;
  background: transparent;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main, #f6f2ea);
  outline: none;
  cursor: pointer;
}

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.85rem;
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 6px;
  color: var(--color-text-main, #f6f2ea);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-refresh:hover:not(:disabled) {
  border-color: var(--color-primary, #d6b476);
  color: var(--color-primary, #d6b476);
  background: var(--color-bg-subtle, #2e2d27);
}

.btn-refresh:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 2. Secciones y Grid de KPIs */
.dashboard-section-block {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.section-badge-title {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--color-text-muted, #c4bcab);
  text-transform: uppercase;
}

.dashboard-kpis-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.dash-kpi-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 1.125rem 1.25rem;
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
  user-select: none;
}

.dash-kpi-card.clickable {
  cursor: pointer;
}

.dash-kpi-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
  border-color: var(--color-accent-copper, #fb923c);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.kpi-index {
  font-family: var(--font-mono, monospace);
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-text-subtle, #968e7e);
}

.kpi-label {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
  flex: 1;
  margin-left: 0.5rem;
}

.kpi-icon {
  font-size: 1rem;
}

.kpi-icon.accent { color: var(--color-accent-copper, #fb923c); }
.kpi-icon.pending { color: var(--color-info-text, #93c5fd); }
.kpi-icon.warning { color: var(--color-warning-text, #fcd34d); }
.kpi-icon.success { color: var(--color-success-text, #7ee79d); }

.kpi-body {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 0.25rem 0 0.5rem 0;
}

.kpi-number {
  font-size: 2rem;
  font-weight: 800;
  line-height: 1;
  font-family: var(--font-mono, monospace);
}

.text-accent { color: var(--color-accent-copper, #fb923c); }
.text-pending { color: var(--color-info-text, #93c5fd); }
.text-warning { color: var(--color-warning-text, #fcd34d); }
.text-success { color: var(--color-success-text, #7ee79d); }

.kpi-pill {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.kpi-pill.accent {
  background: rgba(251, 146, 60, 0.15);
  color: var(--color-accent-copper, #fb923c);
  border: 1px solid rgba(251, 146, 60, 0.3);
}

.kpi-pill.pending {
  background: var(--color-info-bg, #172635);
  color: var(--color-info-text, #93c5fd);
  border: 1px solid var(--color-info-border, #254b6d);
}

.kpi-pill.warning {
  background: var(--color-warning-bg, #342914);
  color: var(--color-warning-text, #fcd34d);
  border: 1px solid var(--color-warning-border, #695222);
}

.kpi-pill.success {
  background: var(--color-success-bg, #1c2b1f);
  color: var(--color-success-text, #7ee79d);
  border: 1px solid var(--color-success-border, #2d5738);
}

.kpi-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.6875rem;
  color: var(--color-text-subtle, #968e7e);
  border-top: 1px dashed var(--color-border, #444138);
  padding-top: 0.5rem;
  margin-top: 0.25rem;
}

/* 3. Analytics Grid */
.dashboard-analytics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1rem;
}

.analytics-card {
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 8px;
  padding: 1.25rem;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.card-head h4 {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
}

.card-icon {
  font-size: 0.875rem;
  color: var(--color-primary, #d6b476);
}

.regimen-stats {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.stat-row {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.stat-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.8125rem;
}

.stat-name {
  color: var(--color-text-muted, #c4bcab);
}

.stat-val {
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--color-bg-subtle, #2e2d27);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-fill.import {
  background: #38bdf8;
}

.progress-fill.export {
  background: #4ade80;
}

.canal-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

.canal-badge-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.75rem 0.5rem;
  border-radius: 6px;
  border: 1px solid transparent;
}

.canal-badge-box.verde {
  background: var(--color-success-bg, #1c2b1f);
  border-color: var(--color-success-border, #2d5738);
}
.canal-badge-box.verde .canal-lbl,
.canal-badge-box.verde .canal-num {
  color: var(--color-success-text, #7ee79d);
}

.canal-badge-box.naranja {
  background: var(--color-warning-bg, #342914);
  border-color: var(--color-warning-border, #695222);
}
.canal-badge-box.naranja .canal-lbl,
.canal-badge-box.naranja .canal-num {
  color: var(--color-warning-text, #fcd34d);
}

.canal-badge-box.rojo {
  background: var(--color-danger-bg, #321818);
  border-color: var(--color-danger-border, #652a2a);
}
.canal-badge-box.rojo .canal-lbl,
.canal-badge-box.rojo .canal-num {
  color: var(--color-danger-text, #fca5a5);
}

.canal-badge-box.pendiente {
  background: var(--color-bg-subtle, #2e2d27);
  border-color: var(--color-border, #444138);
}
.canal-badge-box.pendiente .canal-lbl,
.canal-badge-box.pendiente .canal-num {
  color: var(--color-text-muted, #c4bcab);
}

.canal-lbl {
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.canal-num {
  font-size: 1.5rem;
  font-weight: 800;
  line-height: 1.2;
}

.canal-sub {
  font-size: 0.625rem;
  color: var(--color-text-subtle, #968e7e);
}

/* 4. Lista Prioritaria */
.empty-priority-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.25rem;
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 8px;
  font-size: 0.8125rem;
  color: var(--color-text-muted, #c4bcab);
}

.priority-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.priority-item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1.25rem;
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 6px;
  gap: 1rem;
  flex-wrap: wrap;
}

.item-left {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 200px;
}

.item-code {
  font-size: 0.875rem;
  color: var(--color-primary, #d6b476);
}

.item-client {
  font-size: 0.8125rem;
  color: var(--color-text-main, #f6f2ea);
  font-weight: 600;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.15rem;
}

.meta-tag {
  font-size: 0.625rem;
  font-family: var(--font-mono, monospace);
  padding: 0.1rem 0.4rem;
  background: var(--color-bg-subtle, #2e2d27);
  border: 1px solid var(--color-border, #444138);
  border-radius: 3px;
  color: var(--color-text-muted, #c4bcab);
}

.priority-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.priority-badge.alert {
  background: var(--color-danger-bg, #321818);
  color: var(--color-danger-text, #fca5a5);
  border: 1px solid var(--color-danger-border, #652a2a);
}

.priority-badge.success {
  background: var(--color-success-bg, #1c2b1f);
  color: var(--color-success-text, #7ee79d);
  border: 1px solid var(--color-success-border, #2d5738);
}

.priority-badge.info {
  background: var(--color-info-bg, #172635);
  color: var(--color-info-text, #93c5fd);
  border: 1px solid var(--color-info-border, #254b6d);
}

.btn-action-goto {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.75rem;
  background: var(--color-bg-subtle, #2e2d27);
  border: 1px solid var(--color-border, #444138);
  border-radius: 4px;
  color: var(--color-text-main, #f6f2ea);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-action-goto:hover {
  background: var(--color-primary, #d6b476);
  color: var(--color-primary-text, #1d1b15);
  border-color: var(--color-primary, #d6b476);
}
</style>
