<template>
  <div class="agente-header-toolbar">
    <div class="toolbar-left-info">
      <span class="badge-kicker">DESPACHO ADUANERO // BANDEJA DE CAMPO</span>
      <h3 class="toolbar-title">Bandeja de Expedientes Asignados</h3>
    </div>

    <div class="toolbar-controls-group">
      <!-- Selector de Agente (Exclusivo para Supervisores y Administradores) -->
      <div v-if="esSupervisorOAdmin" class="supervisor-select-wrap">
        <label for="filter-agente-sel" class="supervisor-label">
          <i class="pi pi-user text-xs"></i>
          <span>Auditar:</span>
        </label>
        <select
          id="filter-agente-sel"
          :value="selectedAgenteId"
          class="supervisor-select"
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

      <!-- Filtro por Régimen -->
      <select
        :value="filterRegimen"
        class="filter-select"
        @change="$emit('update:filterRegimen', $event.target.value)"
      >
        <option value="">Todos los Regímenes</option>
        <option value="IMPORTACION">Importación para el Consumo</option>
        <option value="EXPORTACION">Exportación Definitiva</option>
      </select>

      <!-- Filtro por Canal SUNAT -->
      <select
        :value="filterCanal"
        class="filter-select"
        @change="$emit('update:filterCanal', $event.target.value)"
      >
        <option value="">Todos los Canales</option>
        <option value="VERDE">Canal VERDE (Libre)</option>
        <option value="NARANJA">Canal NARANJA (Documental)</option>
        <option value="ROJO">Canal ROJO (Aforo Físico)</option>
        <option value="PENDIENTE">Canal Pendiente</option>
      </select>

      <!-- Búsqueda rápida -->
      <div class="search-input-wrap">
        <i class="pi pi-search search-icon"></i>
        <input
          :value="searchQuery"
          type="text"
          class="search-input"
          placeholder="Buscar expediente, DAM, cliente, nave..."
          @input="$emit('update:searchQuery', $event.target.value)"
        />
      </div>

      <!-- Botón de refresco manual -->
      <button
        type="button"
        class="refresh-btn"
        title="Actualizar datos de la bandeja"
        :disabled="loading"
        @click="$emit('refresh')"
      >
        <i :class="['pi pi-refresh', { 'pi-spin': loading }]"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  searchQuery: {
    type: String,
    default: ''
  },
  filterRegimen: {
    type: String,
    default: ''
  },
  filterCanal: {
    type: String,
    default: ''
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

defineEmits([
  'update:searchQuery',
  'update:filterRegimen',
  'update:filterCanal',
  'update:selectedAgenteId',
  'refresh'
])
</script>

<style scoped>
.agente-header-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.25rem;
  padding: 1rem 1.25rem;
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 8px;
  flex-wrap: wrap;
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-family: var(--font-mono, monospace);
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #e0c287);
  text-transform: uppercase;
}

.toolbar-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--color-text-main, #f6f2ea);
}

.toolbar-controls-group {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  flex-wrap: wrap;
}

.supervisor-select-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
}

.supervisor-label {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-accent-kicker, #e0c287);
  white-space: nowrap;
}

.supervisor-select {
  border: none;
  background: transparent;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main, #f6f2ea);
  outline: none;
  cursor: pointer;
}

.filter-select {
  height: 38px;
  padding: 0 0.75rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-main, #f6f2ea);
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
  border-radius: 6px;
  outline: none;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.filter-select:focus {
  border-color: var(--color-primary, #d6b476);
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-subtle, #968e7e);
  pointer-events: none;
}

.search-input {
  height: 38px;
  width: 250px;
  padding: 0 0.75rem 0 2.25rem;
  font-size: 0.8125rem;
  color: var(--color-text-main, #f6f2ea);
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
  border-radius: 6px;
  outline: none;
  transition: border-color 0.15s ease, width 0.2s ease;
}

.search-input:focus {
  border-color: var(--color-primary, #d6b476);
  width: 280px;
}

.refresh-btn {
  height: 38px;
  width: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
  border-radius: 6px;
  color: var(--color-text-muted, #c4bcab);
  cursor: pointer;
  transition: all 0.15s ease;
}

.refresh-btn:hover:not(:disabled) {
  color: var(--color-primary, #d6b476);
  border-color: var(--color-primary, #d6b476);
  background: var(--color-bg-subtle, #2e2d27);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
