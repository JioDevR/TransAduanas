<template>
  <div class="transporte-dashboard-panel">
    <!-- 1. Grid de KPIs Operativos -->
    <div class="kpi-grid">
      <div class="kpi-card accent-card" @click="$emit('select-filter', 'EN_RUTA')">
        <div class="kpi-card-header">
          <span class="kpi-num">01</span>
          <span class="kpi-label">Despachos En Tránsito</span>
          <i class="pi pi-compass kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ enRutaCount }}</span>
          <span class="kpi-badge info">En Ruta</span>
        </div>
        <div class="kpi-footer-note">
          <span>Camiones en traslado hacia cliente o puerto</span>
        </div>
      </div>

      <div class="kpi-card warning-card" @click="$emit('select-filter', 'PENDIENTE_ASIGNACION')">
        <div class="kpi-card-header">
          <span class="kpi-num">02</span>
          <span class="kpi-label">Pendientes Asignación</span>
          <i class="pi pi-clock kpi-icon text-amber-600"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono text-amber-600">{{ pendientesCount }}</span>
          <span class="kpi-badge warning">Por Atender</span>
        </div>
        <div class="kpi-footer-note">
          <span>Requieren asignar tracto, carreta y chofer</span>
        </div>
      </div>

      <div class="kpi-card success-card" @click="$emit('go-tab', 'unidades')">
        <div class="kpi-card-header">
          <span class="kpi-num">03</span>
          <span class="kpi-label">Tractos en Patio</span>
          <i class="pi pi-truck kpi-icon text-emerald-600"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono text-emerald-600">{{ disponiblesCount }}</span>
          <span class="kpi-badge success">Disponibles</span>
        </div>
        <div class="kpi-footer-note">
          <span>Listos para enganche y salida inmediata</span>
        </div>
      </div>

      <div class="kpi-card" @click="$emit('select-filter', 'ENTREGADO')">
        <div class="kpi-card-header">
          <span class="kpi-num">04</span>
          <span class="kpi-label">Entregas Conformes</span>
          <i class="pi pi-check-circle kpi-icon text-slate-600"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ entregadosCount }}</span>
          <span class="kpi-badge neutral">Finalizados</span>
        </div>
        <div class="kpi-footer-note">
          <span>Traslados concluidos con recepción y DNI</span>
        </div>
      </div>
    </div>

    <!-- 2. Alertas Técnicas Preventivas MTC (SOAT / CITV / Brevete) -->
    <div v-if="alertasDocumentarias.length > 0" class="alerts-section">
      <div class="alerts-header">
        <div class="alerts-title-group">
          <i class="pi pi-exclamation-triangle text-amber-600"></i>
          <h4>Alertas Técnicas Preventivas MTC</h4>
        </div>
        <span class="alerts-counter font-mono">{{ alertasDocumentarias.length }} advertencia(s)</span>
      </div>

      <div class="alerts-list">
        <div
          v-for="al in alertasDocumentarias"
          :key="al.id"
          :class="['alert-card', al.tipo]"
        >
          <div class="alert-icon-wrap">
            <i :class="al.tipo === 'danger' ? 'pi pi-times-circle' : 'pi pi-clock'"></i>
          </div>
          <div class="alert-content">
            <div class="alert-tagline">
              <span class="font-mono font-bold">{{ al.recurso }}</span>
              <span class="alert-badge">{{ al.entidad }}</span>
            </div>
            <p class="alert-text">{{ al.mensaje }}</p>
          </div>
          <div class="alert-action">
            <button
              type="button"
              class="btn-subtle-alert"
              @click="$emit('go-tab', al.targetTab)"
            >
              Auditar
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. Acciones Rápidas & Guía Operativa -->
    <div class="quick-nav-grid">
      <div class="quick-action-card" @click="$emit('go-tab', 'despachos')">
        <div class="action-icon-box">
          <i class="pi pi-list"></i>
        </div>
        <div class="action-meta">
          <h5>Bandeja de Despachos</h5>
          <p>Supervisar órdenes por estado, asignar recursos e iniciar rutas.</p>
        </div>
        <i class="pi pi-arrow-right action-arrow"></i>
      </div>

      <div class="quick-action-card" @click="$emit('go-tab', 'cabina')">
        <div class="action-icon-box">
          <i class="pi pi-send"></i>
        </div>
        <div class="action-meta">
          <h5>Cabina de Ruta Activa</h5>
          <p>Panel táctico para monitorear viajes en curso o despachar como chofer.</p>
        </div>
        <i class="pi pi-arrow-right action-arrow"></i>
      </div>

      <div class="quick-action-card" @click="$emit('go-tab', 'unidades')">
        <div class="action-icon-box">
          <i class="pi pi-car"></i>
        </div>
        <div class="action-meta">
          <h5>Parque de Flota MTC</h5>
          <p>Inventario de tractos, carretas, SOAT, revisiones CITV y odómetros.</p>
        </div>
        <i class="pi pi-arrow-right action-arrow"></i>
      </div>

      <div class="quick-action-card" @click="$emit('go-tab', 'conductores')">
        <div class="action-icon-box">
          <i class="pi pi-users"></i>
        </div>
        <div class="action-meta">
          <h5>Directorio de Choferes</h5>
          <p>Padrón de choferes profesionales, categorías de brevete y usuarios.</p>
        </div>
        <i class="pi pi-arrow-right action-arrow"></i>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  despachos: {
    type: Array,
    default: () => []
  },
  unidades: {
    type: Array,
    default: () => []
  },
  conductores: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select-filter', 'go-tab'])

const enRutaCount = computed(() => {
  return props.despachos.filter(d => (d.estadoTransporte || '').toUpperCase() === 'EN_RUTA').length
})

const pendientesCount = computed(() => {
  return props.despachos.filter(d => (d.estadoTransporte || '').toUpperCase() === 'PENDIENTE_ASIGNACION').length
})

const disponiblesCount = computed(() => {
  return props.unidades.filter(u => u.estadoActivo && (u.estadoOperativo || '').toUpperCase() === 'DISPONIBLE').length
})

const entregadosCount = computed(() => {
  return props.despachos.filter(d => (d.estadoTransporte || '').toUpperCase() === 'ENTREGADO').length
})

const alertasDocumentarias = computed(() => {
  const alerts = []
  const today = new Date()
  const in30Days = new Date()
  in30Days.setDate(today.getDate() + 30)

  // 1. Alertas de Unidades
  props.unidades.forEach(u => {
    if (!u.estadoActivo) return

    const soatStr = u.fechaVencimientoSoat || u.soatVencimiento
    if (soatStr) {
      const soatDate = new Date(soatStr)
      if (soatDate < today) {
        alerts.push({
          id: `soat-exp-${u.idUnidad}`,
          recurso: u.placaTracto,
          entidad: 'UNIDAD',
          tipo: 'danger',
          mensaje: `SOAT vencido el ${soatStr.substring(0, 10)}. Unidad no autorizada para circular.`,
          targetTab: 'unidades'
        })
      } else if (soatDate <= in30Days) {
        alerts.push({
          id: `soat-warn-${u.idUnidad}`,
          recurso: u.placaTracto,
          entidad: 'UNIDAD',
          tipo: 'warning',
          mensaje: `SOAT por vencer el ${soatStr.substring(0, 10)} (dentro de los próximos 30 días).`,
          targetTab: 'unidades'
        })
      }
    }

    const citvStr = u.fechaVencimientoCitv || u.citvVencimiento
    if (citvStr) {
      const citvDate = new Date(citvStr)
      if (citvDate < today) {
        alerts.push({
          id: `citv-exp-${u.idUnidad}`,
          recurso: u.placaTracto,
          entidad: 'UNIDAD',
          tipo: 'danger',
          mensaje: `Revisión Técnica CITV vencida el ${citvStr.substring(0, 10)}.`,
          targetTab: 'unidades'
        })
      } else if (citvDate <= in30Days) {
        alerts.push({
          id: `citv-warn-${u.idUnidad}`,
          recurso: u.placaTracto,
          entidad: 'UNIDAD',
          tipo: 'warning',
          mensaje: `Revisión Técnica CITV por vencer el ${citvStr.substring(0, 10)}.`,
          targetTab: 'unidades'
        })
      }
    }
  })

  // 2. Alertas de Choferes
  props.conductores.forEach(c => {
    if (!c.estadoActivo || !c.fechaVencimientoLicencia) return
    const licDate = new Date(c.fechaVencimientoLicencia)
    const licStr = c.fechaVencimientoLicencia.substring(0, 10)
    if (licDate < today) {
      alerts.push({
        id: `lic-exp-${c.idConductor}`,
        recurso: `${c.nombres} ${c.apellidos}`,
        entidad: 'CONDUCTOR',
        tipo: 'danger',
        mensaje: `Licencia ${c.numeroLicencia} vencida el ${licStr}. Operador no habilitado.`,
        targetTab: 'conductores'
      })
    } else if (licDate <= in30Days) {
      alerts.push({
        id: `lic-warn-${c.idConductor}`,
        recurso: `${c.nombres} ${c.apellidos}`,
        entidad: 'CONDUCTOR',
        tipo: 'warning',
        mensaje: `Licencia ${c.numeroLicencia} próxima a vencer el ${licStr}.`,
        targetTab: 'conductores'
      })
    }
  })

  return alerts.slice(0, 4)
})
</script>

<style scoped>
.transporte-dashboard-panel {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 1rem;
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border-color: var(--color-border-strong);
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-num {
  font-family: var(--font-mono);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-subtle);
}

.kpi-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.kpi-icon {
  font-size: 1.125rem;
  color: var(--color-text-subtle);
}

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.kpi-value {
  font-size: 2.25rem;
  font-weight: 700;
  line-height: 1;
  color: var(--color-text-main);
}

.kpi-badge {
  font-family: var(--font-mono);
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  text-transform: uppercase;
}

.kpi-badge.info { background: var(--color-info-bg); color: var(--color-info-text); border: 1px solid var(--color-info-border); }
.kpi-badge.warning { background: var(--color-warning-bg); color: var(--color-warning-text); border: 1px solid var(--color-warning-border); }
.kpi-badge.success { background: var(--color-success-bg); color: var(--color-success-text); border: 1px solid var(--color-success-border); }
.kpi-badge.neutral { background: var(--color-bg-muted); color: var(--color-text-muted); }

.kpi-footer-note {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

/* Alertas Técnicas */
.alerts-section {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.25rem 1.5rem;
}

.alerts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.alerts-title-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.alerts-title-group h4 {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.alerts-counter {
  font-size: 0.75rem;
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  font-weight: 600;
}

.alerts-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 0.75rem;
}

.alert-card {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid;
}

.alert-card.warning {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.alert-card.danger {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.alert-icon-wrap {
  margin-top: 0.125rem;
  font-size: 1.125rem;
}

.alert-content {
  flex: 1;
}

.alert-tagline {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  margin-bottom: 0.25rem;
}

.alert-badge {
  font-size: 0.625rem;
  padding: 0.1rem 0.375rem;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.08);
  font-weight: 700;
}

.alert-text {
  margin: 0;
  font-size: 0.75rem;
  line-height: 1.4;
}

.btn-subtle-alert {
  background: transparent;
  border: 1px solid currentColor;
  color: inherit;
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-subtle-alert:hover {
  background: rgba(0, 0, 0, 0.06);
}

/* Quick Nav */
.quick-nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1rem;
}

.quick-action-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.125rem 1.25rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-action-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.action-icon-box {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  color: var(--color-text-main);
}

.action-meta {
  flex: 1;
}

.action-meta h5 {
  margin: 0 0 0.25rem 0;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.action-meta p {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.35;
}

.action-arrow {
  color: var(--color-text-subtle);
  font-size: 0.875rem;
  transition: transform 0.2s ease;
}

.quick-action-card:hover .action-arrow {
  transform: translateX(4px);
  color: var(--color-primary);
}
</style>
