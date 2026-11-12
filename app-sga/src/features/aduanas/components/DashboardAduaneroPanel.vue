<template>
  <div class="dashboard-aduanero-panel fade-in">
    <!-- 1. Cabecera Ejecutiva del Dashboard -->
    <div class="dashboard-hero-header">
      <div class="hero-text">
        <div class="kicker-row">
          <span class="badge-kicker">CONTROL OPERATIVO // GESTIÓN ADUANERA</span>
          <span class="status-live-pill">
            <span class="live-dot"></span>
            <span>MONITOREO EN TIEMPO REAL</span>
          </span>
        </div>
        <h2 class="hero-title">Centro de Control y Supervisión Aduanera</h2>
        <p class="hero-subtitle">
          Auditoría técnica de propuestas, formalización de expedientes con abono validado y trazabilidad de despacho ante SUNAT.
        </p>
      </div>

      <div class="hero-actions">
        <button
          type="button"
          class="btn-outline-warm"
          @click="$emit('refresh')"
          :disabled="loading"
          title="Actualizar datos del servidor"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>
      </div>
    </div>

    <!-- 2. Grid de KPIs: Auditoría Comercial & Pre-Expediente -->
    <div class="dashboard-section-block">
      <div class="section-badge-title">
        <i class="pi pi-inbox"></i>
        <span>1. BANDEJA DE AUDITORÍA TÉCNICA Y COMPUERTA FINANCIERA</span>
      </div>

      <div class="dashboard-kpis-grid">
        <!-- KPI 01: Nuevas por Auditar -->
        <div
          class="dash-kpi-card pending-card clickable"
          @click="$emit('navigate', 'cotizaciones', 'nuevas')"
          title="Ver cotizaciones nuevas ingresadas desde Ventas"
        >
          <div class="kpi-top">
            <span class="kpi-index">01</span>
            <span class="kpi-label">Nuevas por Auditar</span>
            <i class="pi pi-clock kpi-icon warning"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-warning">{{ metrics.nuevas.length }}</span>
            <span class="kpi-pill warning">Revisión Inicial</span>
          </div>
          <div class="kpi-footer">
            <span>Propuestas entrantes sin auditar</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>

        <!-- KPI 02: Subsanaciones / Reingresos -->
        <div
          class="dash-kpi-card alert-card clickable"
          @click="$emit('navigate', 'cotizaciones', 'subsanadas')"
          title="Ver cotizaciones observadas subsanadas por Comercial"
        >
          <div class="kpi-top">
            <span class="kpi-index">02</span>
            <span class="kpi-label">Subsanadas / Reingresos</span>
            <i class="pi pi-exclamation-triangle kpi-icon danger"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-danger">{{ metrics.subsanadas.length }}</span>
            <span class="kpi-pill danger">Prioridad Alta</span>
          </div>
          <div class="kpi-footer">
            <span>Correcciones enviadas por Ventas</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>

        <!-- KPI 03: En Espera de Pago Finanzas -->
        <div
          class="dash-kpi-card wait-card clickable"
          @click="$emit('navigate', 'cotizaciones', 'pendientesPago')"
          title="Cotizaciones aprobadas técnicamente esperando pago contable"
        >
          <div class="kpi-top">
            <span class="kpi-index">03</span>
            <span class="kpi-label">En Espera de Pago</span>
            <i class="pi pi-wallet kpi-icon muted"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number">{{ metrics.pendientesPago.length }}</span>
            <span class="kpi-pill muted">Pendiente Finanzas</span>
          </div>
          <div class="kpi-footer">
            <span>Con visto bueno técnico aduanero</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>

        <!-- KPI 04: Listas para Aperturar -->
        <div
          class="dash-kpi-card success-card clickable highlight"
          @click="$emit('navigate', 'apertura')"
          title="Cotizaciones con pago verificado listas para generar expediente"
        >
          <div class="kpi-top">
            <span class="kpi-index">04</span>
            <span class="kpi-label">Listas para Apertura</span>
            <i class="pi pi-folder-plus kpi-icon primary"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-primary">{{ metrics.listasApertura.length }}</span>
            <span class="kpi-pill primary">Pago Confirmado</span>
          </div>
          <div class="kpi-footer">
            <span>Compuerta de apertura habilitada</span>
            <i class="pi pi-bolt"></i>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. Grid de KPIs: Despacho Aduanero ante SUNAT -->
    <div class="dashboard-section-block">
      <div class="section-badge-title">
        <i class="pi pi-folder-open"></i>
        <span>2. GESTIÓN OPERATIVA ANTE SUNAT Y ESTADO DE EXPEDIENTES</span>
      </div>

      <div class="dashboard-kpis-grid">
        <!-- KPI 05: Pendiente Canal SUNAT / DAM -->
        <div
          class="dash-kpi-card pending-card clickable"
          @click="$emit('navigate', 'expedientes', 'PENDIENTE_CANAL')"
          title="Expedientes sin DAM o pendientes de asignación de canal"
        >
          <div class="kpi-top">
            <span class="kpi-index">05</span>
            <span class="kpi-label">Por Asignar Canal</span>
            <i class="pi pi-compass kpi-icon warning"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-warning">{{ expedientesPorCanalPendiente.length }}</span>
            <span class="kpi-pill warning">Esperando DAM/SUNAT</span>
          </div>
          <div class="kpi-footer">
            <span>En numeración o tránsito inicial</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>

        <!-- KPI 06: En Control / Aforo SUNAT -->
        <div
          class="dash-kpi-card info-card clickable"
          @click="$emit('navigate', 'expedientes', 'CONTROL_SUNAT')"
          title="Expedientes con Canal Verde, Naranja o Rojo en curso"
        >
          <div class="kpi-top">
            <span class="kpi-index">06</span>
            <span class="kpi-label">En Control SUNAT</span>
            <i class="pi pi-shield kpi-icon info"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-info">{{ expedientesEnControl.length }}</span>
            <span class="kpi-pill info">Aforo / Documentario</span>
          </div>
          <div class="kpi-footer">
            <span>Verde ({{ countCanal('VERDE') }}) | Naranja ({{ countCanal('NARANJA') }}) | Rojo ({{ countCanal('ROJO') }})</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>

        <!-- KPI 07: Levante Autorizado -->
        <div
          class="dash-kpi-card success-card clickable"
          @click="$emit('navigate', 'expedientes', 'LEVANTE_AUTORIZADO')"
          title="Carga liberada por SUNAT lista para retiro y transporte"
        >
          <div class="kpi-top">
            <span class="kpi-index">07</span>
            <span class="kpi-label">Levante Autorizado</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number text-success">{{ expedientesConLevante.length }}</span>
            <span class="kpi-pill success">Mercancía Liberada</span>
          </div>
          <div class="kpi-footer">
            <span>Listos para retiro o entrega final</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>

        <!-- KPI 08: Expedientes Cerrados -->
        <div
          class="dash-kpi-card clickable"
          @click="$emit('navigate', 'expedientes', 'CERRADOS')"
          title="Expedientes con despacho aduanero y liquidación concluida"
        >
          <div class="kpi-top">
            <span class="kpi-index">08</span>
            <span class="kpi-label">Expedientes Cerrados</span>
            <i class="pi pi-lock kpi-icon"></i>
          </div>
          <div class="kpi-body">
            <span class="kpi-number">{{ expedientesCerrados.length }}</span>
            <span class="kpi-pill">Concluidos</span>
          </div>
          <div class="kpi-footer">
            <span>Despacho finalizado</span>
            <i class="pi pi-arrow-right"></i>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. Paneles de Distribución y Atajos Operativos -->
    <div class="dashboard-analytics-row">
      <!-- Tarjeta 1: Desglose de Canales SUNAT -->
      <div class="analytics-card">
        <div class="card-head">
          <h4 class="card-title">
            <i class="pi pi-shield"></i>
            <span>Distribución de Canales SUNAT</span>
          </h4>
          <span class="card-sub">{{ expedientes.length }} expedientes en total</span>
        </div>

        <div class="canal-breakdown-list">
          <div class="canal-row" @click="$emit('navigate', 'expedientes', 'CONTROL_SUNAT')">
            <div class="canal-badge-box verde">
              <i class="pi pi-check"></i>
              <span>CANAL VERDE</span>
            </div>
            <div class="canal-bar-wrap">
              <div
                class="canal-bar-fill verde"
                :style="{ width: getPercentage(countCanal('VERDE'), expedientes.length) + '%' }"
              ></div>
            </div>
            <span class="canal-count font-mono">{{ countCanal('VERDE') }}</span>
          </div>

          <div class="canal-row" @click="$emit('navigate', 'expedientes', 'CONTROL_SUNAT')">
            <div class="canal-badge-box naranja">
              <i class="pi pi-file"></i>
              <span>CANAL NARANJA</span>
            </div>
            <div class="canal-bar-wrap">
              <div
                class="canal-bar-fill naranja"
                :style="{ width: getPercentage(countCanal('NARANJA'), expedientes.length) + '%' }"
              ></div>
            </div>
            <span class="canal-count font-mono">{{ countCanal('NARANJA') }}</span>
          </div>

          <div class="canal-row" @click="$emit('navigate', 'expedientes', 'CONTROL_SUNAT')">
            <div class="canal-badge-box rojo">
              <i class="pi pi-search"></i>
              <span>CANAL ROJO</span>
            </div>
            <div class="canal-bar-wrap">
              <div
                class="canal-bar-fill rojo"
                :style="{ width: getPercentage(countCanal('ROJO'), expedientes.length) + '%' }"
              ></div>
            </div>
            <span class="canal-count font-mono">{{ countCanal('ROJO') }}</span>
          </div>

          <div class="canal-row" @click="$emit('navigate', 'expedientes', 'PENDIENTE_CANAL')">
            <div class="canal-badge-box pendiente">
              <i class="pi pi-clock"></i>
              <span>POR NUMERAR / PENDIENTE</span>
            </div>
            <div class="canal-bar-wrap">
              <div
                class="canal-bar-fill pendiente"
                :style="{ width: getPercentage(expedientesPorCanalPendiente.length, expedientes.length) + '%' }"
              ></div>
            </div>
            <span class="canal-count font-mono">{{ expedientesPorCanalPendiente.length }}</span>
          </div>
        </div>
      </div>

      <!-- Tarjeta 2: Atajos y Acciones Rápidas del Operario -->
      <div class="analytics-card quick-actions-card">
        <div class="card-head">
          <h4 class="card-title">
            <i class="pi pi-compass"></i>
            <span>Atajos de Acción Rápida</span>
          </h4>
          <span class="card-sub">Navegación directa por prioridad</span>
        </div>

        <div class="shortcuts-actions-grid">
          <button
            type="button"
            class="shortcut-btn primary"
            @click="$emit('navigate', 'apertura')"
          >
            <div class="btn-icon">
              <i class="pi pi-folder-plus"></i>
            </div>
            <div class="btn-text">
              <strong>Aperturar Expedientes</strong>
              <span>{{ metrics.listasApertura.length }} con pago confirmado</span>
            </div>
            <i class="pi pi-chevron-right"></i>
          </button>

          <button
            type="button"
            class="shortcut-btn warning"
            @click="$emit('navigate', 'cotizaciones', 'nuevas')"
          >
            <div class="btn-icon">
              <i class="pi pi-eye"></i>
            </div>
            <div class="btn-text">
              <strong>Auditar Cotizaciones Nuevas</strong>
              <span>{{ metrics.nuevas.length }} en cola de revisión</span>
            </div>
            <i class="pi pi-chevron-right"></i>
          </button>

          <button
            type="button"
            class="shortcut-btn danger"
            @click="$emit('navigate', 'cotizaciones', 'subsanadas')"
          >
            <div class="btn-icon">
              <i class="pi pi-file-edit"></i>
            </div>
            <div class="btn-text">
              <strong>Revisar Subsanaciones</strong>
              <span>{{ metrics.subsanadas.length }} reingresadas por ventas</span>
            </div>
            <i class="pi pi-chevron-right"></i>
          </button>

          <button
            type="button"
            class="shortcut-btn success"
            @click="$emit('navigate', 'expedientes', 'LEVANTE_AUTORIZADO')"
          >
            <div class="btn-icon">
              <i class="pi pi-shield"></i>
            </div>
            <div class="btn-text">
              <strong>Coordinar Retiros / Levante</strong>
              <span>{{ expedientesConLevante.length }} mercancías liberadas</span>
            </div>
            <i class="pi pi-chevron-right"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  clasificarCotizacionesAduaneras,
  filtrarExpedientesPorFase
} from '../utils/aduanasWorkflowUtils'

const props = defineProps({
  cotizaciones: {
    type: Array,
    default: () => []
  },
  expedientes: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['refresh', 'navigate'])

// Métricas de cotizaciones clasificadas
const metrics = computed(() => {
  return clasificarCotizacionesAduaneras(props.cotizaciones)
})

// Expedientes por fases operativas
const expedientesPorCanalPendiente = computed(() =>
  filtrarExpedientesPorFase(props.expedientes, 'PENDIENTE_CANAL')
)

const expedientesEnControl = computed(() =>
  filtrarExpedientesPorFase(props.expedientes, 'CONTROL_SUNAT')
)

const expedientesConLevante = computed(() =>
  filtrarExpedientesPorFase(props.expedientes, 'LEVANTE_AUTORIZADO')
)

const expedientesCerrados = computed(() =>
  filtrarExpedientesPorFase(props.expedientes, 'CERRADOS')
)

const countCanal = (canal) => {
  return props.expedientes.filter((e) => e && e.canalSunat === canal).length
}

const getPercentage = (count, total) => {
  if (!total || total === 0) return 0
  return Math.min(100, Math.round((count / total) * 100))
}
</script>

<style scoped>
.dashboard-aduanero-panel {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding-bottom: 2rem;
}

.dashboard-hero-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.25rem 1.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #8a4b26);
  text-transform: uppercase;
}

.status-live-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.15rem 0.5rem;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 700;
  color: #059669;
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
  color: var(--color-text-main);
  letter-spacing: -0.02em;
}

.hero-subtitle {
  margin: 0.25rem 0 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
  max-width: 800px;
}

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
  color: var(--color-text-muted);
  text-transform: uppercase;
}

.dashboard-kpis-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 1rem;
}

.dash-kpi-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 1.125rem 1.25rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  position: relative;
  overflow: hidden;
}

.dash-kpi-card.clickable {
  cursor: pointer;
}

.dash-kpi-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-accent-copper, #c27847);
}

.dash-kpi-card.highlight {
  border-color: rgba(194, 120, 71, 0.4);
  background: linear-gradient(to bottom right, var(--color-bg-surface), rgba(194, 120, 71, 0.03));
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.kpi-index {
  font-size: 0.75rem;
  font-weight: 800;
  font-family: var(--font-mono);
  color: var(--color-text-muted);
  opacity: 0.7;
}

.kpi-label {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
  flex: 1;
  margin-left: 0.5rem;
}

.kpi-icon {
  font-size: 1.1rem;
}

.kpi-icon.warning { color: #d97706; }
.kpi-icon.danger { color: #dc2626; }
.kpi-icon.primary { color: var(--color-accent-kicker, #8a4b26); }
.kpi-icon.info { color: #2563eb; }
.kpi-icon.success { color: #16a34a; }
.kpi-icon.muted { color: var(--color-text-muted); }

.kpi-body {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.kpi-number {
  font-size: 2rem;
  font-weight: 800;
  line-height: 1;
  font-family: var(--font-mono);
  color: var(--color-text-main);
}

.text-warning { color: #d97706 !important; }
.text-danger { color: #dc2626 !important; }
.text-primary { color: var(--color-accent-kicker, #8a4b26) !important; }
.text-info { color: #2563eb !important; }
.text-success { color: #16a34a !important; }

.kpi-pill {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  background: var(--color-bg-subtle);
  color: var(--color-text-subtle);
}

.kpi-pill.warning { background: rgba(217, 119, 6, 0.1); color: #d97706; }
.kpi-pill.danger { background: rgba(220, 38, 38, 0.1); color: #dc2626; }
.kpi-pill.primary { background: rgba(138, 75, 38, 0.1); color: var(--color-accent-kicker, #8a4b26); }
.kpi-pill.info { background: rgba(37, 99, 235, 0.1); color: #2563eb; }
.kpi-pill.success { background: rgba(22, 163, 74, 0.1); color: #16a34a; }

.kpi-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.75rem;
  color: var(--color-text-subtle);
  border-top: 1px dashed var(--color-border);
  padding-top: 0.6rem;
}

.kpi-footer i {
  font-size: 0.75rem;
  color: var(--color-accent-copper, #c27847);
}

.dashboard-analytics-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

@media (max-width: 900px) {
  .dashboard-analytics-row {
    grid-template-columns: 1fr;
  }
}

.analytics-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.75rem;
}

.card-title {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 800;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.canal-breakdown-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.canal-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  padding: 0.4rem 0.5rem;
  border-radius: var(--radius-sm);
  transition: background 0.15s ease;
}

.canal-row:hover {
  background: var(--color-bg-subtle);
}

.canal-badge-box {
  width: 140px;
  font-size: 0.6875rem;
  font-weight: 800;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.canal-badge-box.verde { background: rgba(22, 163, 74, 0.12); color: #16a34a; }
.canal-badge-box.naranja { background: rgba(234, 88, 12, 0.12); color: #ea580c; }
.canal-badge-box.rojo { background: rgba(220, 38, 38, 0.12); color: #dc2626; }
.canal-badge-box.pendiente { background: var(--color-bg-subtle); color: var(--color-text-muted); }

.canal-bar-wrap {
  flex: 1;
  height: 8px;
  background: var(--color-bg-subtle);
  border-radius: 999px;
  overflow: hidden;
}

.canal-bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
}

.canal-bar-fill.verde { background: #16a34a; }
.canal-bar-fill.naranja { background: #ea580c; }
.canal-bar-fill.rojo { background: #dc2626; }
.canal-bar-fill.pendiente { background: #94a3b8; }

.canal-count {
  font-size: 0.8125rem;
  font-weight: 800;
  width: 32px;
  text-align: right;
  color: var(--color-text-main);
}

.shortcuts-actions-grid {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.shortcut-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
}

.shortcut-btn:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-accent-copper, #c27847);
  transform: translateX(3px);
}

.shortcut-btn .btn-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  flex-shrink: 0;
}

.shortcut-btn.primary .btn-icon { background: rgba(138, 75, 38, 0.12); color: var(--color-accent-kicker, #8a4b26); }
.shortcut-btn.warning .btn-icon { background: rgba(217, 119, 6, 0.12); color: #d97706; }
.shortcut-btn.danger .btn-icon { background: rgba(220, 38, 38, 0.12); color: #dc2626; }
.shortcut-btn.success .btn-icon { background: rgba(22, 163, 74, 0.12); color: #16a34a; }

.shortcut-btn .btn-text {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.shortcut-btn .btn-text strong {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.shortcut-btn .btn-text span {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
}

.shortcut-btn i.pi-chevron-right {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}
</style>
