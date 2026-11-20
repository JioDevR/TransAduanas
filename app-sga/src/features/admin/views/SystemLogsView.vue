<template>
  <div class="system-logs-view fade-in">
    <!-- 1. Barra de Estado y Acciones sgaUI (Título único en Topbar) -->
    <header class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag :value="logData.fileName || 'sga-backend.log'" severity="secondary" />
        <span class="sga-kpi-pill sga-mono">Sync {{ lastSyncTime }}</span>
      </div>

      <div class="sga-dash-actions">
        <Button
          :label="isAutoRefresh ? 'Auto (5s)' : 'Pausado'"
          :icon="isAutoRefresh ? 'pi pi-pause' : 'pi pi-play'"
          :severity="isAutoRefresh ? 'success' : 'secondary'"
          :outlined="!isAutoRefresh"
          size="small"
          @click="toggleAutoRefresh"
        />
        <Button
          label="Actualizar"
          icon="pi pi-sync"
          severity="secondary"
          outlined
          size="small"
          :loading="loading"
          @click="fetchLogs"
        />
        <Button
          label="Descargar"
          icon="pi pi-download"
          severity="secondary"
          outlined
          size="small"
          @click="downloadFullLog"
        />
        <Button
          label="Vaciar"
          icon="pi pi-trash"
          severity="danger"
          outlined
          size="small"
          @click="handleClearLog"
        />
      </div>
    </header>

    <!-- 2. KPIs sgaUI -->
    <section class="sga-kpis-grid">
      <article class="sga-kpi-card" @click="fetchLogs">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">Archivo Activo</span>
          <i class="pi pi-file-code sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">{{ logData.fileName || 'sga-backend.log' }}</span>
          <span class="sga-kpi-pill">Logback</span>
        </div>
        <p class="sga-kpi-foot">RollingFileAppender activo</p>
      </article>

      <article class="sga-kpi-card is-warn">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">Tamaño en Disco</span>
          <i class="pi pi-database sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ formattedFileSize }}</span>
          <span class="sga-kpi-pill is-warn">En Servidor</span>
        </div>
        <p class="sga-kpi-foot">Buffer lectura máx. 2000 líneas</p>
      </article>

      <article class="sga-kpi-card is-success">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">Trazas Filtradas</span>
          <i class="pi pi-list sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ filteredLines.length }}</span>
          <span class="sga-kpi-pill is-success">{{ linesLimit }} límite</span>
        </div>
        <p class="sga-kpi-foot">Líneas visibles tras filtro</p>
      </article>

      <article class="sga-kpi-card">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Histórico Archivo</span>
          <i class="pi pi-history sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ logData.totalLines || 0 }}</span>
          <span class="sga-kpi-pill">{{ lastSyncTime }}</span>
        </div>
        <p class="sga-kpi-foot">Total de líneas registradas</p>
      </article>
    </section>

    <!-- 3. Filtros y Consola Terminal (sgaUI) -->
    <section class="sga-card-surface sga-filters-card">
      <div class="sga-filters-grid">
        <div class="sga-filter-field">
          <label class="sga-field-label">Archivo de Log</label>
          <Select
            v-model="selectedTipo"
            :options="tipoOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @change="fetchLogs"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Cantidad de Líneas</label>
          <Select
            v-model="linesLimit"
            :options="limitOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @change="fetchLogs"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Nivel</label>
          <Select
            v-model="filterLevel"
            :options="levelOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Buscar en trazas</label>
          <InputText
            v-model="searchQuery"
            placeholder="Expresión o texto..."
            size="small"
            fluid
          />
        </div>
      </div>
    </section>

    <section class="sga-card-surface terminal-card">
      <div ref="terminalBody" class="terminal-screen">
        <div v-if="loading && (!logData.lines || logData.lines.length === 0)" class="sga-empty-state">
          <i class="pi pi-spin pi-spinner sga-empty-icon"></i>
          <p class="sga-empty-desc">Cargando trazas de ejecución...</p>
        </div>

        <div v-else-if="filteredLines.length === 0" class="sga-empty-state">
          <i class="pi pi-info-circle sga-empty-icon"></i>
          <p class="sga-empty-desc">No se encontraron líneas con los filtros aplicados.</p>
        </div>

        <div v-else class="terminal-lines">
          <div
            v-for="(line, idx) in filteredLines"
            :key="idx"
            :class="['terminal-line', getLineClass(line)]"
          >
            <span class="line-number sga-mono">{{ idx + 1 }}</span>
            <span class="line-text sga-mono">{{ line }}</span>
          </div>
        </div>
      </div>

      <div class="terminal-footer">
        <span class="sga-text-muted">Ruta: <code>{{ logData.path || './logs/sga-backend.log' }}</code></span>
        <Button
          label="Ir al Final"
          icon="pi pi-arrow-down"
          severity="secondary"
          outlined
          size="small"
          @click="scrollToBottom"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { logsService } from '../services/logsService'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'

const { showSuccess, showError, showInfo } = useNotification()
const { confirmDelete } = useConfirmation()

const loading = ref(false)
const selectedTipo = ref('all')
const linesLimit = ref(200)
const filterLevel = ref('ALL')
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

const tipoOptions = [
  { label: 'sga-backend.log (General)', value: 'all' },
  { label: 'sga-backend-error.log (Errores)', value: 'error' }
]

const limitOptions = [
  { label: '100 líneas', value: 100 },
  { label: '200 líneas', value: 200 },
  { label: '500 líneas', value: 500 },
  { label: '1000 líneas', value: 1000 }
]

const levelOptions = [
  { label: 'Todos los niveles', value: 'ALL' },
  { label: 'ERROR', value: 'ERROR' },
  { label: 'WARN', value: 'WARN' },
  { label: 'INFO', value: 'INFO' },
  { label: 'DEBUG', value: 'DEBUG' }
]

let autoRefreshTimer = null

const formattedFileSize = computed(() => {
  const bytes = logData.value.sizeBytes || 0
  if (bytes === 0) return '0 KB'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
})

const filteredLines = computed(() => {
  let lines = logData.value.lines || []
  if (filterLevel.value && filterLevel.value !== 'ALL') {
    const lvl = filterLevel.value.toUpperCase()
    lines = lines.filter((line) => line.includes(` ${lvl} `) || line.includes(`[${lvl}]`))
  }
  if (searchQuery.value && searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase().trim()
    lines = lines.filter((line) => line.toLowerCase().includes(q))
  }
  return lines
})

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

const getLineClass = (line) => {
  if (line.includes(' ERROR ')) return 'level-error'
  if (line.includes(' WARN ')) return 'level-warn'
  if (line.includes(' INFO ')) return 'level-info'
  if (line.includes(' DEBUG ')) return 'level-debug'
  return ''
}

const downloadFullLog = async () => {
  try {
    await logsService.downloadLog(selectedTipo.value)
    showSuccess('Descarga del archivo de log iniciada.')
  } catch (err) {
    showError('Error al descargar el archivo de log: ' + err.message)
  }
}

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

const scrollToBottom = () => {
  nextTick(() => {
    if (terminalBody.value) {
      terminalBody.value.scrollTop = terminalBody.value.scrollHeight
    }
  })
}

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
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
})
</script>

<style scoped>
.system-logs-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.terminal-card {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.terminal-screen {
  background: #1a1714;
  color: #e6dfd5;
  height: 460px;
  overflow-y: auto;
  padding: 0.9rem 1rem;
  font-size: 0.78rem;
  line-height: 1.5;
}

.terminal-lines {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.terminal-line {
  display: flex;
  gap: 0.75rem;
  white-space: pre-wrap;
  word-break: break-word;
}

.line-number {
  color: #7a7064;
  min-width: 2.5rem;
  text-align: right;
  user-select: none;
}

.level-error .line-text { color: #fca5a5; }
.level-warn .line-text { color: #fcd34d; }
.level-info .line-text { color: #7ee79d; }
.level-debug .line-text { color: #93c5fd; }

.terminal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.25rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  flex-wrap: wrap;
}
</style>
