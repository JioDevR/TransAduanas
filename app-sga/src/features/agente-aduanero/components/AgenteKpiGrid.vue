<template>
  <div class="agente-kpi-grid">
    <!-- KPI 01: Total Asignados -->
    <div
      :class="['kpi-card', 'accent-card', { active: activeTab === 'todos' }]"
      @click="$emit('select-tab', 'todos')"
    >
      <div class="kpi-card-header">
        <span class="kpi-num">01</span>
        <span class="kpi-label">Total Asignados</span>
        <i class="pi pi-folder-open kpi-icon accent"></i>
      </div>
      <div class="kpi-main-stat">
        <span class="kpi-value font-mono text-accent">{{ counts.todos }}</span>
        <span class="kpi-badge accent">En Bandeja</span>
      </div>
      <div class="kpi-footer-note">
        <span>Despachos asignados activos</span>
      </div>
    </div>

    <!-- KPI 02: En Tránsito / Arribo -->
    <div
      :class="['kpi-card', 'pending-card', { active: activeTab === 'transito' }]"
      @click="$emit('select-tab', 'transito')"
    >
      <div class="kpi-card-header">
        <span class="kpi-num">02</span>
        <span class="kpi-label">En Tránsito / Arribo</span>
        <i class="pi pi-compass kpi-icon pending"></i>
      </div>
      <div class="kpi-main-stat">
        <span class="kpi-value font-mono text-pending">{{ counts.transito }}</span>
        <span class="kpi-badge pending">En Nave / Recojo</span>
      </div>
      <div class="kpi-footer-note">
        <span>Aguardando arribo o DAM</span>
      </div>
    </div>

    <!-- KPI 03: Inspección / Aforo SUNAT -->
    <div
      :class="['kpi-card', 'warning-card', { active: activeTab === 'inspeccion' }]"
      @click="$emit('select-tab', 'inspeccion')"
    >
      <div class="kpi-card-header">
        <span class="kpi-num">03</span>
        <span class="kpi-label">Inspección / Aforo</span>
        <i class="pi pi-search-plus kpi-icon warning"></i>
      </div>
      <div class="kpi-main-stat">
        <span class="kpi-value font-mono text-warning">{{ counts.inspeccion }}</span>
        <span class="kpi-badge warning">Canal Naranja/Rojo</span>
      </div>
      <div class="kpi-footer-note">
        <span>Requiere diligencia</span>
      </div>
    </div>

    <!-- KPI 04: Levante Concedido -->
    <div
      :class="['kpi-card', 'success-card', { active: activeTab === 'levante' }]"
      @click="$emit('select-tab', 'levante')"
    >
      <div class="kpi-card-header">
        <span class="kpi-num">04</span>
        <span class="kpi-label">Levante Concedido</span>
        <i class="pi pi-check-circle kpi-icon success"></i>
      </div>
      <div class="kpi-main-stat">
        <span class="kpi-value font-mono text-success">{{ counts.levante }}</span>
        <span class="kpi-badge success">Listo Salida</span>
      </div>
      <div class="kpi-footer-note">
        <span>Liberado para retiro/embarque</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  counts: {
    type: Object,
    default: () => ({
      todos: 0,
      transito: 0,
      inspeccion: 0,
      levante: 0
    })
  },
  activeTab: {
    type: String,
    default: 'todos'
  }
})

defineEmits(['select-tab'])
</script>

<style scoped>
.agente-kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.kpi-card {
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 8px;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
  user-select: none;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
}

.kpi-card.active {
  border-width: 2px;
}

.kpi-card.accent-card.active {
  border-color: var(--color-accent-copper, #fb923c);
  background: rgba(251, 146, 60, 0.08);
}

.kpi-card.pending-card.active {
  border-color: var(--color-info-text, #93c5fd);
  background: rgba(147, 197, 253, 0.08);
}

.kpi-card.warning-card.active {
  border-color: var(--color-warning-text, #fcd34d);
  background: rgba(252, 211, 77, 0.08);
}

.kpi-card.success-card.active {
  border-color: var(--color-success-text, #7ee79d);
  background: rgba(126, 231, 157, 0.08);
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.kpi-num {
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

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 0.25rem 0 0.5rem 0;
}

.kpi-value {
  font-size: 1.85rem;
  font-weight: 800;
  line-height: 1;
  font-family: var(--font-mono, monospace);
}

.kpi-value.text-accent { color: var(--color-accent-copper, #fb923c); }
.kpi-value.text-pending { color: var(--color-info-text, #93c5fd); }
.kpi-value.text-warning { color: var(--color-warning-text, #fcd34d); }
.kpi-value.text-success { color: var(--color-success-text, #7ee79d); }

.kpi-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.kpi-badge.accent {
  background: rgba(251, 146, 60, 0.15);
  color: var(--color-accent-copper, #fb923c);
  border: 1px solid rgba(251, 146, 60, 0.3);
}

.kpi-badge.pending {
  background: var(--color-info-bg, #172635);
  color: var(--color-info-text, #93c5fd);
  border: 1px solid var(--color-info-border, #254b6d);
}

.kpi-badge.warning {
  background: var(--color-warning-bg, #342914);
  color: var(--color-warning-text, #fcd34d);
  border: 1px solid var(--color-warning-border, #695222);
}

.kpi-badge.success {
  background: var(--color-success-bg, #1c2b1f);
  color: var(--color-success-text, #7ee79d);
  border: 1px solid var(--color-success-border, #2d5738);
}

.kpi-footer-note {
  font-size: 0.6875rem;
  color: var(--color-text-subtle, #968e7e);
  border-top: 1px dashed var(--color-border, #444138);
  padding-top: 0.5rem;
  margin-top: 0.25rem;
}
</style>
