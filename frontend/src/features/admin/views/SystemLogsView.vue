<template>
  <div class="system-logs-view">
    <!-- 1. Hero Banner Editorial Cálido Reutilizable -->
    <HeroBannerCard
      kicker="ADMINISTRACIÓN & CONTROL TI // MONITOREO DE SISTEMA"
      title="Consola de Bitácora y Diagnóstico en Tiempo Real"
      description="Monitoreo de trazas de ejecución de Spring Boot 3, eventos de seguridad JWT, excepciones no controladas y rotación de archivos Logback."
    />

    <!-- 2. Métricas y Estado del Archivo de Log -->
    <div class="metrics-grid">
      <MetricCard
        index="01"
        label="Archivo Activo"
        :value="logData.fileName || 'sga-backend.log'"
        severity="accent"
      />
      <MetricCard
        index="02"
        label="Tamaño en Servidor"
        :value="formattedFileSize"
        severity="warning"
      />
      <MetricCard
        index="03"
        label="Líneas en Terminal"
        :value="filteredLines.length"
        severity="success"
      />
      <MetricCard
        index="04"
        label="Total Histórico Servidor"
        :value="logData.totalLines || 0"
        severity="pending"
      />
    </div>

    <!-- 3. Consola Terminal Interactiva -->
    <div class="terminal-card">
      <!-- Toolbar Superior de la Terminal -->
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

        <div class="toolbar-right">
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
            class="btn-terminal-action"
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
            class="btn-terminal-action"
            @click="downloadFullLog"
            title="Descargar archivo .log completo"
          >
            <i class="pi pi-download"></i>
            <span>Descargar</span>
          </button>

          <!-- Botón Vaciar / Truncar Log -->
          <button
            type="button"
            class="btn-terminal-action btn-danger-term"
            @click="handleClearLog"
            title="Vaciar archivo de log en servidor"
          >
            <i class="pi pi-trash"></i>
            <span>Vaciar</span>
          </button>
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
import HeroBannerCard from '@/shared/components/HeroBannerCard.vue'
import MetricCard from '@/shared/components/MetricCard.vue'
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
  gap: 1.5rem;
  padding-bottom: 2.5rem;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 1rem;
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
  background: #191815;
  border: 1px solid #443e35;
  color: #ede6db;
  padding: 0.35rem 0.65rem;
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
