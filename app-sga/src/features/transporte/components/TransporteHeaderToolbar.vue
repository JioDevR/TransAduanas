<template>
  <header class="transporte-header-toolbar">
    <div class="toolbar-left">
      <div class="toolbar-brand">
        <span class="badge-kicker">MÓDULO 05 // LOGÍSTICA TERRESTRE PERÚ</span>
        <h1 class="toolbar-title">Transporte, Flota y Despachos MTC</h1>
      </div>
      <div class="toolbar-quick-tags">
        <span class="status-indicator-live">
          <span class="live-dot pulse"></span>
          PATIO CENTRAL CALLAO
        </span>
        <span class="stat-badge">
          <i class="pi pi-compass text-amber-500"></i>
          <span class="font-mono font-semibold">{{ enRutaCount }}</span> en ruta
        </span>
        <span class="stat-badge">
          <i class="pi pi-truck text-emerald-600"></i>
          <span class="font-mono font-semibold">{{ disponiblesCount }}</span> tractos listos
        </span>
      </div>
    </div>

    <div class="toolbar-right">
      <button
        type="button"
        class="btn-outline-warm"
        @click="$emit('recargar')"
        :disabled="loading"
        title="Sincronizar despachos y flota"
      >
        <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
        <span>Actualizar</span>
      </button>

      <button
        type="button"
        class="btn-espresso"
        @click="$emit('programar-despacho')"
      >
        <i class="pi pi-plus"></i>
        <span>+ Programar Despacho</span>
      </button>
    </div>
  </header>
</template>

<script setup>
defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  enRutaCount: {
    type: Number,
    default: 0
  },
  disponiblesCount: {
    type: Number,
    default: 0
  }
})

defineEmits(['recargar', 'programar-despacho'])
</script>

<style scoped>
.transporte-header-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.toolbar-left {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.badge-kicker {
  font-family: var(--font-mono, monospace);
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-text-subtle);
  text-transform: uppercase;
}

.toolbar-title {
  margin: 0;
  font-family: var(--font-sans);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.02em;
}

.toolbar-quick-tags {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.status-indicator-live {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-family: var(--font-mono);
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-success-text);
  background: var(--color-success-bg);
  padding: 0.25rem 0.5rem;
  border-radius: 9999px;
  border: 1px solid var(--color-success-border);
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success-text);
}

.live-dot.pulse {
  animation: pulse-dot 1.8s infinite;
}

@keyframes pulse-dot {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}

.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  border: 1px solid var(--color-border);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  padding: 0.625rem 1rem;
  border-radius: var(--radius-md, 0.5rem);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.btn-espresso:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: transparent;
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius-md, 0.5rem);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.18s ease;
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
}
</style>
