<template>
  <div class="system-logs-view">
    <!-- 1. Toolbar de Acciones Warm Editorial (Sin Banners Invasivos) -->
    <div class="logs-action-toolbar">
      <div class="toolbar-left-info">
        <span class="badge-kicker">ADMINISTRACIÓN & CONTROL TI // MONITOREO DE SISTEMA</span>
        <h3 class="toolbar-title">Consola de Bitácora y Diagnóstico en Tiempo Real</h3>
      </div>

      <div class="toolbar-actions-group">
        <!-- Toggle Auto-Refresco -->
        <button
          type="button"
          :class="['btn-toggle-auto', { active: isAutoRefresh }]"
          @click="toggleAutoRefresh"
          title="Conmutar actualización automática cada 5 segundos"
        >
          <span class="live-pulse" :class="{ 'pulse-active': isAutoRefresh }"></span>
          <span>{{ isAutoRefresh ? 'Auto (5s)' : 'Pausado' }}</span>
        </button>

        <!-- Botón Refrescar Manual -->
        <button
          type="button"
          class="btn-outline-warm"
          @click="fetchLogs"
          :disabled="loading"
          title="Recargar logs ahora"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>

        <!-- Botón Descargar Log Completo -->
        <button
          type="button"
          class="btn-outline-warm"
          @click="downloadFullLog"
          title="Descargar archivo .log completo"
        >
          <i class="pi pi-download"></i>
          <span>Descargar</span>
        </button>

        <!-- Botón Vaciar / Truncar Log -->
        <button
          type="button"
          class="btn-outline-warm btn-danger-action"
          @click="handleClearLog"
          title="Vaciar archivo de log en servidor"
        >
          <i class="pi pi-trash"></i>
          <span>Vaciar</span>
        </button>
      </div>
    </div>

    <!-- 2. Grid de Métricas KPI Técnicas (Estándar Editorial Warm) -->
    <div class="metrics-kpi-grid">
      <!-- Tarjeta 01: Archivo Activo -->
      <div class="kpi-card" @click="fetchLogs">
        <div class="kpi-card-header">
          <span class="kpi-num">01</span>
          <span class="kpi-label">Archivo de Registro Activo</span>
          <i class="pi pi-file-code kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ logData.fileName || 'sga-backend.log' }}</span>
          <span class="kpi-badge accent">Logback</span>
        </div>
        <div class="kpi-footer-note">
          <span>Rotación: <strong class="font-mono text-muted">RollingFileAppender</strong></span>
        </div>
      </div>

      <!-- Tarjeta 02: Tamaño en Servidor -->
      <div class="kpi-card warning-card">
        <div class="kpi-card-header">
          <span class="kpi-num">02</span>
          <span class="kpi-label">Tamaño en Disco</span>
          <i class="pi pi-database kpi-icon warning"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ formattedFileSize }}</span>
          <span class="kpi-badge warning">En Servidor</span>
        </div>
        <div class="kpi-footer-note">
          <span>Límite de buffer: <strong class="font-mono text-muted">10 MB máx</strong></span>
        </div>
      </div>

      <!-- Tarjeta 03: Líneas en Terminal -->
      <div class="kpi-card success-card">
        <div class="kpi-card-header">
          <span class="kpi-num">03</span>
          <span class="kpi-label">Trazas Filtradas en Pantalla</span>
          <i class="pi pi-list kpi-icon success"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value text-success font-mono">{{ filteredLines.length }}</span>
          <span class="kpi-badge success">En Memoria</span>
        </div>
        <div class="kpi-footer-note">
          <span>Profundidad: <strong class="font-mono text-muted">{{ linesLimit }} líneas</strong></span>
        </div>
      </div>

      <!-- Tarjeta 04: Total Histórico Servidor -->
      <div class="kpi-card pending-card">
        <div class="kpi-card-header">
          <span class="kpi-num">04</span>
          <span class="kpi-label">Total Histórico Servidor</span>
          <i class="pi pi-history kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ logData.totalLines || 0 }}</span>
          <span class="kpi-badge info font-mono">Líneas</span>
        </div>
        <div class="kpi-footer-note">
          <span>Última sincronización: <strong class="font-mono text-muted">{{ lastSyncTime }}</strong></span>
        </div>
      </div>
    </div>

    <!-- 3. Consola Terminal Interactiva -->
    <div class="terminal-card">
      <!-- Toolbar de Controles y Filtros de Terminal -->
      <div class="terminal-toolbar">
        <div class="toolbar-left">
          <!-- Selector de Archivo de Log -->
          <div class="control-group">
            <label class="control-label">Archivo:</label>
            <select v-model="selectedTipo" class="terminal-select" @change="fetchLogs">
              <option value="all">sga-backend.log (General)</option>
              <option value="error">sga-backend-error.log (Solo Errores)</option>
            </select>
          </div>

          <!-- Límite de Líneas -->
          <div class="control-group">
            <label class="control-label">Líneas:</label>
            <select v-model="linesLimit" class="terminal-select" @change="fetchLogs">
              <option :value="100">100</option>
              <option :value="200">200</option>
              <option :value="500">500</option>
              <option :value="1000">1000</option>
            </select>
          </div>

          <!-- Filtro por Nivel -->
          <div class="control-group">
            <label class="control-label">Nivel:</label>
            <select v-model="filterLevel" class="terminal-select">
              <option value="">TODOS</option>
              <option value="ERROR">ERROR</option>
              <option value="WARN">WARN</option>
              <option value="INFO">INFO</option>
              <option value="DEBUG">DEBUG</option>
            </select>
          </div>

          <!-- Buscador de Texto -->
          <div class="search-input-wrapper">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model="searchQuery"
              type="text"
              class="terminal-search-input"
              placeholder="Buscar en trazas..."
            />
            <button
              v-if="searchQuery"
              type="button"
              class="clear-search-btn"
              @click="searchQuery = ''"
            >
              <i class="pi pi-times"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- Cuerpo de Salida de Consola -->
      <div ref="terminalBody" class="terminal-screen">
        <div v-if="loading && (!logData.lines || logData.lines.length === 0)" class="terminal-empty">
          <i class="pi pi-spin pi-spinner"></i>
          <span>Cargando trazas de ejecución desde el backend...</span>
        </div>

        <div v-else-if="filteredLines.length === 0" class="terminal-empty">
          <i class="pi pi-info-circle"></i>
          <span>No se encontraron líneas que coincidan con los filtros aplicados.</span>
        </div>

        <div v-else class="terminal-lines">
          <div
            v-for="(line, idx) in filteredLines"
            :key="idx"
            :class="['terminal-line', getLineClass(line)]"
          >
            <span class="line-number">{{ idx + 1 }}</span>
            <span class="line-text">{{ line }}</span>
          </div>
        </div>
      </div>

      <!-- Barra de Estado Inferior -->
      <div class="terminal-footer">
        <div class="footer-meta">
          <span class="dot-online"></span>
          <span>Ruta Servidor: <code>{{ logData.path || './logs/sga-backend.log' }}</code></span>
        </div>
        <div class="footer-actions">
          <span>Última sincronización: {{ lastSyncTime }}</span>
          <button type="button" class="btn-scroll-bottom" @click="scrollToBottom">
            <i class="pi pi-arrow-down"></i>
            <span>Ir al Final</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { logsService } from '../services/logsService'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'

const { showSuccess, showError, showInfo } = useNotification()
const { confirmDelete } = useConfirmation()

const loading = ref(false)
const selectedTipo = ref('all')
const linesLimit = ref(200)
const filterLevel = ref('')
const searchQuery = ref('')
const isAutoRefresh = ref(false)
const lastSyncTime = ref('—')
const terminalBody = ref(null)

const logData = ref({
  fileName: '',
  path: '',
  sizeBytes: 0,
  totalLines: 0,
  lines: []
})

let autoRefreshTimer = null

// Formateo de bytes a KB / MB
const formattedFileSize = computed(() => {
  const bytes = logData.value.sizeBytes || 0
  if (bytes === 0) return '0 KB'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
})

// Filtrado de líneas en tiempo real
const filteredLines = computed(() => {
  let lines = logData.value.lines || []

  if (filterLevel.value) {
    const lvl = filterLevel.value.toUpperCase()
    lines = lines.filter((line) => line.includes(` ${lvl} `) || line.includes(`[${lvl}]`))
  }

  if (searchQuery.value && searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase().trim()
    lines = lines.filter((line) => line.toLowerCase().includes(q))
  }

  return lines
})

// Cargar líneas del backend
const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await logsService.getLogs({
      tipo: selectedTipo.value,
      lines: linesLimit.value
    })
    logData.value = res || { lines: [] }
    lastSyncTime.value = new Date().toLocaleTimeString('es-PE', { hour12: false })
  } catch (err) {
    showError('No se pudo conectar con el servicio de logs: ' + (err.response?.data?.message || err.message))
  } finally {
    loading.value = false
  }
}

// Clasificación de color por nivel de log
const getLineClass = (line) => {
  if (line.includes(' ERROR ')) return 'level-error'
  if (line.includes(' WARN ')) return 'level-warn'
  if (line.includes(' INFO ')) return 'level-info'
  if (line.includes(' DEBUG ')) return 'level-debug'
  return ''
}

// Descargar archivo completo
const downloadFullLog = async () => {
  try {
    await logsService.downloadLog(selectedTipo.value)
    showSuccess('Descarga del archivo de log iniciada.')
  } catch (err) {
    showError('Error al descargar el archivo de log: ' + err.message)
  }
}

// Vaciar log en servidor
const handleClearLog = async () => {
  const confirmed = await confirmDelete(
    `¿Desea vaciar completamente el archivo ${selectedTipo.value === 'error' ? 'sga-backend-error.log' : 'sga-backend.log'} en el servidor?`,
    'Confirmar Vaciado de Log'
  )
  if (!confirmed) return

  try {
    await logsService.clearLog(selectedTipo.value)
    showInfo('Archivo de log vaciado exitosamente en el servidor.')
    await fetchLogs()
  } catch (err) {
    showError('Error al vaciar archivo de log: ' + (err.response?.data?.message || err.message))
  }
}

// Desplazamiento automático al fondo de la pantalla
const scrollToBottom = () => {
  nextTick(() => {
    if (terminalBody.value) {
      terminalBody.value.scrollTop = terminalBody.value.scrollHeight
    }
  })
}

// Conmutar auto-refresco
const toggleAutoRefresh = () => {
  isAutoRefresh.value = !isAutoRefresh.value
  if (isAutoRefresh.value) {
    autoRefreshTimer = setInterval(fetchLogs, 5000)
    showInfo('Actualización automática activada (cada 5s).')
  } else {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
    showInfo('Actualización automática pausada.')
  }
}

onMounted(() => {
  fetchLogs()
})

onUnmounted(() => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
  }
})
</script>

<style scoped>
.system-logs-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR DE ACCIONES WARM EDITORIAL
   ========================================================================== */
.logs-action-toolbar {
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
  text-transform: uppercase;
  color: var(--color-accent-kicker);
}

.toolbar-title {
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.015em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

/* ==========================================================================
   2. REJILLA DE TARJETAS KPI (WARM EDITORIAL)
   ========================================================================== */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
}

@media (max-width: 1200px) {
  .metrics-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-kpi-grid {
    grid-template-columns: 1fr;
  }
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
  border-color: var(--color-border-strong);
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

.kpi-icon.warning {
  color: var(--color-warning-text, #d97706);
}

.kpi-icon.success {
  color: var(--color-success-text, #16a34a);
}

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-value {
  font-family: var(--font-sans);
  font-size: 1.65rem;
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.kpi-value.font-mono {
  font-family: var(--font-mono, monospace);
  font-size: 1.35rem;
}

.text-success {
  color: var(--color-success-text, #16a34a) !important;
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

.kpi-badge.warning {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #92400e);
}

.kpi-badge.success {
  background: var(--color-success-bg, #dcfce7);
  color: var(--color-success-text, #166534);
}

.kpi-badge.info {
  background: var(--color-info-bg, #e0f2fe);
  color: var(--color-info-text, #0369a1);
}

.kpi-footer-note {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-subtle);
  border-top: 1px dashed var(--color-border-subtle);
  padding-top: var(--space-2, 0.5rem);
}

/* Botones Oficiales Warm Editorial */
.btn-espresso {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  padding: 0.55rem 1rem;
  border-radius: var(--radius-md, 8px);
  font-weight: 600;
  font-size: var(--font-size-sm, 0.8125rem);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all var(--transition-fast);
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-outline-warm {
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.9rem;
  border-radius: var(--radius-md, 8px);
  font-weight: 600;
  font-size: var(--font-size-sm, 0.8125rem);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all var(--transition-fast);
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.btn-danger-action:hover:not(:disabled) {
  background: var(--color-danger-bg, #fee2e2) !important;
  color: var(--color-danger-text, #b91c1c) !important;
  border-color: var(--color-danger-border, #fca5a5) !important;
}

/* ==========================================================================
   TERMINAL CARD
   ========================================================================== */
.terminal-card {
  background: #171513;
  border: 1px solid #3d3730;
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.28);
  display: flex;
  flex-direction: column;
}

/* Toolbar */
.terminal-toolbar {
  background: #23201c;
  border-bottom: 1px solid #38322b;
  padding: 0.85rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.control-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #a89f91;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.terminal-select {
  appearance: none;
  -webkit-appearance: none;
  background-color: #191815;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%23d6b476'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2.2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.5rem center;
  background-size: 0.85rem 0.85rem;
  border: 1px solid #443e35;
  color: #ede6db;
  padding: 0.35rem 1.75rem 0.35rem 0.65rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  outline: none;
  cursor: pointer;
  transition: border-color var(--transition-fast, 0.15s);
}

.terminal-select:focus {
  border-color: #d6b476;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.65rem;
  font-size: 0.75rem;
  color: #7f7567;
}

.terminal-search-input {
  background: #191815;
  border: 1px solid #443e35;
  color: #f6f2ea;
  padding: 0.35rem 1.75rem 0.35rem 1.85rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  width: 170px;
  outline: none;
  transition: all var(--transition-fast, 0.15s);
}

.terminal-search-input:focus {
  width: 220px;
  border-color: #d6b476;
}

.clear-search-btn {
  position: absolute;
  right: 0.5rem;
  background: transparent;
  border: none;
  color: #7f7567;
  cursor: pointer;
  font-size: 0.75rem;
  padding: 0.15rem;
}

/* Action Buttons */
.btn-terminal-action {
  background: #2b2823;
  border: 1px solid #484136;
  color: #ede6db;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s);
}

.btn-terminal-action:hover:not(:disabled) {
  background: #38342d;
  border-color: #635b4c;
  color: #ffffff;
}

.btn-danger-term:hover:not(:disabled) {
  background: #3b1717;
  border-color: #8c2626;
  color: #fca5a5;
}

.btn-toggle-auto {
  background: #25221e;
  border: 1px solid #443e35;
  color: #a89f91;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s);
}

.btn-toggle-auto.active {
  background: #1b271d;
  border-color: #2e5936;
  color: #86efac;
}

.live-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #736b5e;
}

.live-pulse.pulse-active {
  background: #22c55e;
  box-shadow: 0 0 8px #22c55e;
  animation: pulse-dot 1.5s infinite;
}

@keyframes pulse-dot {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}

/* Terminal Screen */
.terminal-screen {
  background: #12110f;
  min-height: 440px;
  max-height: 580px;
  overflow-y: auto;
  padding: 1rem 1.25rem;
  font-family: var(--font-mono, 'JetBrains Mono', 'Fira Code', 'Courier New', monospace);
  font-size: 0.8125rem;
  line-height: 1.55;
  color: #d1c9bd;
}

.terminal-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 380px;
  gap: 0.85rem;
  color: #786f63;
  font-family: var(--font-sans, sans-serif);
}

.terminal-empty i {
  font-size: 2rem;
}

.terminal-lines {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.terminal-line {
  display: flex;
  gap: 1rem;
  word-break: break-all;
  white-space: pre-wrap;
  border-left: 2px solid transparent;
  padding-left: 0.4rem;
}

.terminal-line:hover {
  background: rgba(255, 255, 255, 0.03);
}

.line-number {
  color: #554f46;
  user-select: none;
  min-width: 38px;
  text-align: right;
  font-size: 0.75rem;
}

.line-text {
  flex: 1;
}

/* Color Coding Levels */
.level-error {
  color: #f87171;
  border-left-color: #ef4444;
  background: rgba(239, 68, 68, 0.04);
}

.level-warn {
  color: #fbbf24;
  border-left-color: #f59e0b;
}

.level-info {
  color: #cbd5e1;
}

.level-debug {
  color: #78716c;
}

/* Footer */
.terminal-footer {
  background: #1c1a17;
  border-top: 1px solid #332d26;
  padding: 0.65rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.75rem;
  color: #8c8273;
}

.footer-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dot-online {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
}

.footer-meta code {
  color: #d6b476;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-scroll-bottom {
  background: transparent;
  border: none;
  color: #d6b476;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  font-weight: 600;
  transition: color var(--transition-fast, 0.15s);
}

.btn-scroll-bottom:hover {
  color: #f6e3ba;
}
</style>
