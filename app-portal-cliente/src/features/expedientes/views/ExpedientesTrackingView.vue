<template>
  <div class="expedientes-view">
    <!-- Header de Sección -->
    <div class="view-header">
      <div class="header-titles">
        <span class="view-kicker">OPERACIONES ADUANERAS SUNAT</span>
        <h1 class="view-title">Expedientes & Tracking DAM</h1>
        <p class="view-subtitle">Seguimiento en vivo de sus trámites aduaneros, asignación de canales SUNAT, control de depósito temporal y transporte.</p>
      </div>
      <div class="header-actions">
        <button class="btn-secondary" @click="cargarExpedientes">
          <i class="pi pi-refresh" :class="{ 'pi-spin': loading }"></i>
          <span>Actualizar Tracking</span>
        </button>
      </div>
    </div>

    <!-- Métricas KPI con numeración 01..04 Warm Editorial -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">01</span>
          <div class="kpi-icon-box">
            <i class="pi pi-box"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL EXPEDIENTES</span>
          <span class="kpi-value font-mono">{{ expedientes.length }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">02</span>
          <div class="kpi-icon-box success">
            <i class="pi pi-check-circle"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">CANAL VERDE (INMEDIATO)</span>
          <span class="kpi-value font-mono">{{ totalCanalVerde }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">03</span>
          <div class="kpi-icon-box warning">
            <i class="pi pi-clock"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">EN EVALUACIÓN ADUANERA</span>
          <span class="kpi-value font-mono">{{ totalEnEvaluacion }}</span>
        </div>
      </div>

      <div class="kpi-card highlighted">
        <div class="kpi-top">
          <span class="kpi-idx">04</span>
          <div class="kpi-icon-box accent">
            <i class="pi pi-shield"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">LEVANTE AUTORIZADO</span>
          <span class="kpi-value font-mono">{{ totalConLevante }}</span>
        </div>
      </div>
    </div>

    <!-- Estado de Carga -->
    <div v-if="loading" class="loading-state">
      <i class="pi pi-spin pi-spinner"></i>
      <span>Consultando estado de despachos aduaneros...</span>
    </div>

    <!-- Estado Vacío -->
    <div v-else-if="expedientes.length === 0" class="empty-state">
      <i class="pi pi-box"></i>
      <h3>No tiene expedientes aduaneros activos</h3>
      <p>Sus cotizaciones aprobadas pasarán a esta bandeja tan pronto como se registre el anticipo y se aperture el expediente aduanero.</p>
    </div>

    <div v-else class="tracking-layout">
      <!-- Selector de Expedientes (Sidebar de Despachos) -->
      <aside class="expedientes-list">
        <h3 class="list-title">Despachos Aduaneros ({{ expedientes.length }})</h3>
        <div class="cards-scroller">
          <div 
            v-for="exp in expedientes" 
            :key="exp.idExpediente" 
            class="exp-item-card" 
            :class="{ active: selectedExpediente?.idExpediente === exp.idExpediente }"
            role="button"
            tabindex="0"
            @click="seleccionarExpediente(exp)"
            @keydown.enter="seleccionarExpediente(exp)"
            @keydown.space.prevent="seleccionarExpediente(exp)"
          >
            <div class="exp-top-row">
              <span class="exp-code font-mono">{{ exp.codigoExpediente }}</span>
              <span class="canal-pill" :class="exp.canalSunat?.toLowerCase()">
                Canal {{ exp.canalSunat }}
              </span>
            </div>
            <div class="exp-cargo-title">{{ exp.nombreMercancia || 'Carga General' }}</div>
            <div class="exp-dam-row">
              <span class="label">DAM:</span>
              <span class="val font-mono">{{ exp.numeroDam || 'Pendiente' }}</span>
            </div>
            <div class="exp-footer-meta">
              <span class="date font-mono">{{ formatDate(exp.fechaApertura) }}</span>
              <span class="state-text">{{ formatEstado(exp.estadoExpediente) }}</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- Panel de Trazabilidad y Tracking Detallado -->
      <section class="tracking-detail" v-if="selectedExpediente">
        <!-- Tarjeta Principal del Expediente -->
        <div class="exp-main-card">
          <div class="exp-header-row">
            <div class="exp-id-info">
              <span class="kicker-exp">DECLARACIÓN ADUANERA DE MERCANCÍAS</span>
              <h2 class="dam-title font-mono">{{ selectedExpediente.numeroDam || 'En Proceso de Numeración' }}</h2>
              <div class="exp-codes-sub">
                <span class="code-badge font-mono">{{ selectedExpediente.codigoExpediente }}</span>
                <span>• Cotización ref: <strong class="font-mono">{{ selectedExpediente.codigoCotizacion }}</strong></span>
                <span>• Agente Responsable: <strong>{{ selectedExpediente.agenteAduaneroNombre || 'Asignado' }}</strong></span>
              </div>
            </div>

            <!-- Semáforo Oficial Canal SUNAT -->
            <div class="sunat-traffic-light" :class="selectedExpediente.canalSunat?.toLowerCase()">
              <div class="light-icon">
                <i v-if="selectedExpediente.canalSunat === 'VERDE'" class="pi pi-check-circle"></i>
                <i v-else-if="selectedExpediente.canalSunat === 'NARANJA'" class="pi pi-file"></i>
                <i v-else-if="selectedExpediente.canalSunat === 'ROJO'" class="pi pi-search"></i>
                <i v-else class="pi pi-clock"></i>
              </div>
              <div class="light-text">
                <span class="light-label">CANAL SUNAT</span>
                <span class="light-val">{{ selectedExpediente.canalSunat || 'PENDIENTE' }}</span>
                <span class="light-desc">{{ getCanalDescription(selectedExpediente.canalSunat) }}</span>
              </div>
            </div>
          </div>

          <!-- Línea de Tiempo Visual (Custom Stepper) -->
          <div class="stepper-section">
            <h4 class="section-title">Línea de Tiempo Operativa</h4>
            <div class="stepper-container">
              <div class="step completed">
                <div class="step-circle"><i class="pi pi-check"></i></div>
                <div class="step-label">Cotización</div>
                <div class="step-date">Aprobada</div>
              </div>

              <div class="step-line active"></div>

              <div class="step completed">
                <div class="step-circle"><i class="pi pi-check"></i></div>
                <div class="step-label">Anticipo</div>
                <div class="step-date">Conciliado</div>
              </div>

              <div class="step-line active"></div>

              <div class="step" :class="{ completed: selectedExpediente.numeroDam, active: !selectedExpediente.numeroDam }">
                <div class="step-circle">
                  <i v-if="selectedExpediente.numeroDam" class="pi pi-check"></i>
                  <span v-else>3</span>
                </div>
                <div class="step-label">Numeración DAM</div>
                <div class="step-date">{{ selectedExpediente.numeroDam ? 'Conforme' : 'En trámite' }}</div>
              </div>

              <div class="step-line" :class="{ active: selectedExpediente.canalSunat && selectedExpediente.canalSunat !== 'PENDIENTE' }"></div>

              <div class="step" :class="{ completed: selectedExpediente.canalSunat && selectedExpediente.canalSunat !== 'PENDIENTE' }">
                <div class="step-circle">
                  <i v-if="selectedExpediente.canalSunat && selectedExpediente.canalSunat !== 'PENDIENTE'" class="pi pi-check"></i>
                  <span v-else>4</span>
                </div>
                <div class="step-label">Canal SUNAT</div>
                <div class="step-date">{{ selectedExpediente.canalSunat || 'Asignando' }}</div>
              </div>

              <div class="step-line" :class="{ active: selectedExpediente.fechaLevanteAutorizacion }"></div>

              <div class="step" :class="{ completed: selectedExpediente.fechaLevanteAutorizacion }">
                <div class="step-circle">
                  <i v-if="selectedExpediente.fechaLevanteAutorizacion" class="pi pi-check"></i>
                  <span v-else>5</span>
                </div>
                <div class="step-label">Levante Aduanero</div>
                <div class="step-date">{{ selectedExpediente.fechaLevanteAutorizacion ? 'Autorizado' : 'Pendiente' }}</div>
              </div>
            </div>
          </div>

          <!-- Bloques Operativos: Almacén y Transporte -->
          <div class="operations-grid">
            <!-- Bloque 1: Almacén Temporal -->
            <div class="operation-card">
              <div class="op-card-header">
                <div class="op-icon warehouse">
                  <i class="pi pi-building"></i>
                </div>
                <div>
                  <h4 class="op-title">Depósito Temporal & Almacenaje</h4>
                  <span class="op-sub">{{ selectedExpediente.depositoTemporal || 'Por definir' }}</span>
                </div>
              </div>

              <div class="op-card-body" v-if="selectedExpediente.tieneAlmacen">
                <div class="op-metric-row">
                  <span class="op-lbl">Ubicación / Bahía:</span>
                  <span class="op-val">{{ selectedExpediente.ubicacionBahia || 'Bahía General' }}</span>
                </div>
                <div class="op-metric-row">
                  <span class="op-lbl">Bultos y Peso Bruto:</span>
                  <span class="op-val font-mono">{{ selectedExpediente.cantidadBultos }} bultos • {{ formatWeight(selectedExpediente.pesoBrutoKg) }} kg</span>
                </div>

                <!-- Alerta de Días Libres en Almacén -->
                <div class="warehouse-countdown-alert" :class="getDaysAlertClass(selectedExpediente.diasRestantesAlmacen)">
                  <div class="alert-icon">
                    <i class="pi pi-exclamation-triangle"></i>
                  </div>
                  <div class="alert-info">
                    <div class="days-remaining">
                      <strong>{{ selectedExpediente.diasRestantesAlmacen != null ? selectedExpediente.diasRestantesAlmacen : 15 }} días restantes</strong>
                      <span>de sobreestadía libre</span>
                    </div>
                    <div class="date-deadline font-mono">
                      Fecha límite de retiro: <strong>{{ selectedExpediente.fechaLimiteRetiro || 'Consultar agente' }}</strong>
                    </div>
                  </div>
                </div>
              </div>

              <div class="op-card-empty" v-else>
                <i class="pi pi-info-circle"></i>
                <span>Carga en tránsito internacional hacia puerto de destino.</span>
              </div>
            </div>

            <!-- Bloque 2: Transporte Terrestre y Distribución -->
            <div class="operation-card">
              <div class="op-card-header">
                <div class="op-icon transport">
                  <i class="pi pi-truck"></i>
                </div>
                <div>
                  <h4 class="op-title">Transporte Local & Distribución</h4>
                  <span class="op-sub">Despacho de última milla a planta</span>
                </div>
              </div>

              <div class="op-card-body" v-if="selectedExpediente.tieneTransporte">
                <div class="op-metric-row">
                  <span class="op-lbl">Código de Tracking:</span>
                  <span class="op-val font-mono font-bold">{{ selectedExpediente.codigoTracking }}</span>
                </div>
                <div class="op-metric-row">
                  <span class="op-lbl">Dirección de Entrega:</span>
                  <span class="op-val">{{ selectedExpediente.direccionLlegada }}, {{ selectedExpediente.distritoLlegada }}</span>
                </div>
                <div class="op-metric-row">
                  <span class="op-lbl">Fecha Programada:</span>
                  <span class="op-val font-mono">{{ formatDateTime(selectedExpediente.fechaProgramadaTransporte) }}</span>
                </div>
                <div class="op-metric-row">
                  <span class="op-lbl">Estado del Traslado:</span>
                  <span class="status-pill">{{ selectedExpediente.estadoTransporte }}</span>
                </div>
              </div>

              <div class="op-card-empty" v-else>
                <i class="pi pi-info-circle"></i>
                <span>Asignación de unidad y chofer una vez obtenido el levante aduanero.</span>
              </div>
            </div>
          </div>

          <!-- Bóveda Documental Conectada -->
          <div class="documents-section">
            <div class="docs-section-header">
              <h4 class="section-title">Bóveda Documental del Expediente</h4>
              <span class="docs-count font-mono" v-if="documentosExpediente.length > 0">
                {{ documentosExpediente.length }} archivo(s) timbrado(s)
              </span>
            </div>

            <div v-if="loadingDocs" class="docs-loading">
              <i class="pi pi-spin pi-spinner"></i>
              <span>Consultando bóveda de documentos digitales...</span>
            </div>

            <div v-else-if="documentosExpediente.length === 0" class="docs-empty-state">
              <i class="pi pi-folder-open"></i>
              <p>No se registran documentos adjuntos adicionales para este expediente.</p>
              <span class="sub-hint">Los documentos DAM timbrados por SUNAT aparecerán aquí en cuanto sean emitidos.</span>
            </div>

            <div v-else class="docs-list">
              <div 
                v-for="doc in documentosExpediente" 
                :key="doc.idDocumento" 
                class="doc-item"
              >
                <div class="doc-icon">
                  <i :class="getDocumentIcon(doc.formatoMime)"></i>
                </div>
                <div class="doc-meta">
                  <span class="doc-name">{{ doc.nombreOriginal || doc.descripcion || 'Documento Oficial' }}</span>
                  <span class="doc-sub font-mono">
                    {{ formatFileSize(doc.tamanoBytes) }} • {{ formatDate(doc.fechaSubida) }}
                  </span>
                </div>
                <div class="doc-actions">
                  <button 
                    type="button" 
                    class="btn-doc-action view" 
                    title="Previsualizar en el Navegador"
                    @click="handlePrevisualizar(doc)"
                  >
                    <i class="pi pi-eye"></i>
                    <span>Ver</span>
                  </button>
                  <button 
                    type="button" 
                    class="btn-doc-action download" 
                    title="Descargar Archivo Oficial"
                    @click="handleDescargar(doc)"
                  >
                    <i class="pi pi-download"></i>
                    <span>Descargar</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { portalExpedienteService } from '../services/portalExpedienteService'
import { useNotification } from '@/shared/composables/useNotification'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'

const { showSuccess, showError } = useNotification()

const expedientes = ref([])
const loading = ref(true)
const selectedExpediente = ref(null)

const documentosExpediente = ref([])
const loadingDocs = ref(false)

const totalCanalVerde = computed(() => {
  return expedientes.value.filter(e => e.canalSunat === 'VERDE').length
})

const totalEnEvaluacion = computed(() => {
  return expedientes.value.filter(e => e.canalSunat === 'NARANJA' || e.canalSunat === 'ROJO' || !e.canalSunat || e.canalSunat === 'PENDIENTE').length
})

const totalConLevante = computed(() => {
  return expedientes.value.filter(e => Boolean(e.fechaLevanteAutorizacion)).length
})

onMounted(() => {
  cargarExpedientes()
})

function seleccionarExpediente(exp) {
  selectedExpediente.value = exp
}

async function cargarExpedientes() {
  loading.value = true
  try {
    const data = await portalExpedienteService.listarExpedientes()
    expedientes.value = data || []
    if (expedientes.value.length > 0) {
      if (!selectedExpediente.value || !expedientes.value.find(e => e.idExpediente === selectedExpediente.value.idExpediente)) {
        selectedExpediente.value = expedientes.value[0]
      }
    }
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar los expedientes aduaneros')
  } finally {
    loading.value = false
  }
}

watch(() => selectedExpediente.value?.idExpediente, async (newId) => {
  if (newId) {
    await cargarDocumentos(newId)
  } else {
    documentosExpediente.value = []
  }
}, { immediate: true })

async function cargarDocumentos(idExpediente) {
  loadingDocs.value = true
  try {
    const docs = await portalExpedienteService.listarDocumentosExpediente(idExpediente)
    documentosExpediente.value = docs || []
  } catch (err) {
    console.debug('Error al listar documentos del expediente:', err)
    documentosExpediente.value = []
  } finally {
    loadingDocs.value = false
  }
}

async function handleDescargar(doc) {
  try {
    const blob = await portalExpedienteService.descargarDocumento(doc.idDocumento)
    downloadBlob(blob, doc.nombreOriginal || `documento-${doc.idDocumento}.pdf`, doc.formatoMime || 'application/pdf')
    showSuccess(`Descargando ${doc.nombreOriginal || 'documento'}`, 'Bóveda Digital')
  } catch (err) {
    showError('Error al descargar el documento del servidor')
  }
}

async function handlePrevisualizar(doc) {
  try {
    const blob = await portalExpedienteService.previsualizarDocumento(doc.idDocumento)
    openBlobInNewTab(blob, doc.formatoMime || 'application/pdf')
  } catch (err) {
    showError('Error al previsualizar el documento en el navegador')
  }
}

function getDocumentIcon(mime) {
  if (!mime) return 'pi pi-file'
  if (mime.includes('pdf')) return 'pi pi-file-pdf'
  if (mime.includes('image')) return 'pi pi-image'
  if (mime.includes('sheet') || mime.includes('excel')) return 'pi pi-file-excel'
  return 'pi pi-file'
}

function formatFileSize(bytes) {
  if (!bytes) return '0 KB'
  const kb = bytes / 1024
  if (kb < 1024) return `${kb.toFixed(1)} KB`
  return `${(kb / 1024).toFixed(2)} MB`
}

function formatDate(dt) {
  if (!dt) return '-'
  return dt.substring(0, 10)
}

function formatDateTime(dt) {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 16)
}

function formatEstado(st) {
  if (!st) return ''
  return st.replace('_', ' ')
}

function formatWeight(w) {
  if (w == null) return '0'
  return Number(w).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function getCanalDescription(canal) {
  switch (canal) {
    case 'VERDE': return 'Levante automático e inmediato sin revisión documental'
    case 'NARANJA': return 'Revisión documentaria técnica por especialista SUNAT'
    case 'ROJO': return 'Aforo físico obligatorio en depósito temporal'
    default: return 'Pendiente de asignación por el sistema SUNAT'
  }
}

function getDaysAlertClass(days) {
  if (days == null) return 'normal'
  if (days <= 3) return 'danger'
  if (days <= 7) return 'warning'
  return 'normal'
}
</script>

<style scoped>
.expedientes-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
}

/* Header */
.view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.view-kicker {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
}

.view-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0.2rem 0;
  letter-spacing: -0.02em;
}

.view-subtitle {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin: 0;
}

.btn-secondary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.15rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  border-radius: 6px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.75rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-border-strong);
}

.kpi-card.highlighted {
  border-color: var(--color-accent-kicker);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.kpi-idx {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 6px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
  font-family: var(--font-mono);
}

.kpi-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  color: var(--color-primary);
}

.kpi-icon-box.success {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.kpi-icon-box.warning {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.kpi-icon-box.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
}

.kpi-content {
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle);
}

.kpi-value {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin-top: 0.15rem;
}

/* Layout General */
.tracking-layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 1.5rem;
  align-items: start;
}

/* Sidebar de despachos */
.expedientes-list {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.list-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary);
  margin: 0;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.cards-scroller {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: 700px;
  overflow-y: auto;
}

.exp-item-card {
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-base);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  transition: all 0.2s ease;
}

.exp-item-card:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.exp-item-card.active {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(53, 47, 42, 0.08);
}

.exp-item-card:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.exp-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exp-code {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary);
}

.canal-pill {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.canal-pill.verde {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.canal-pill.naranja {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.canal-pill.rojo {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.exp-cargo-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.exp-dam-row {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  display: flex;
  gap: 0.35rem;
}

.exp-footer-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.7rem;
  color: var(--color-text-subtle);
  margin-top: 0.25rem;
  border-top: 1px dashed var(--color-border-subtle);
  padding-top: 0.35rem;
}

/* Panel de Tracking Detallado */
.tracking-detail {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.exp-main-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.exp-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1.5rem;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 1.5rem;
}

.kicker-exp {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  letter-spacing: 0.05em;
}

.dam-title {
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0.25rem 0;
}

.exp-codes-sub {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.code-badge {
  background: var(--color-bg-subtle);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-weight: 700;
  color: var(--color-primary);
}

/* Semáforo SUNAT */
.sunat-traffic-light {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 1.25rem;
  border-radius: 10px;
  border: 1px solid var(--color-border);
}

.sunat-traffic-light.verde {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.sunat-traffic-light.naranja {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.sunat-traffic-light.rojo {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.light-icon {
  font-size: 2rem;
}

.light-text {
  display: flex;
  flex-direction: column;
}

.light-label {
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  opacity: 0.85;
}

.light-val {
  font-size: 1.15rem;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.light-desc {
  font-size: 0.72rem;
  opacity: 0.85;
  max-width: 200px;
  line-height: 1.25;
}

/* Stepper */
.stepper-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.stepper-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 0;
  overflow-x: auto;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-width: 100px;
}

.step-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-bg-base);
  border: 2px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.step.completed .step-circle {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
}

.step.active .step-circle {
  background: var(--color-bg-surface);
  border-color: var(--color-accent-kicker);
  color: var(--color-accent-kicker);
  box-shadow: 0 0 0 3px rgba(154, 127, 96, 0.2);
}

.step-label {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.step-date {
  font-size: 0.68rem;
  color: var(--color-text-muted);
}

.step-line {
  flex: 1;
  height: 2px;
  background: var(--color-border);
  margin: 0 0.5rem;
  position: relative;
  top: -14px;
}

.step-line.active {
  background: var(--color-primary);
}

/* Operaciones Grid */
.operations-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

.operation-card {
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.op-card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  border-bottom: 1px solid var(--color-border-subtle);
  padding-bottom: 0.75rem;
}

.op-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.15rem;
}

.op-icon.warehouse {
  background: var(--color-bg-surface);
  color: var(--color-primary);
  border: 1px solid var(--color-border);
}

.op-icon.transport {
  background: var(--color-bg-surface);
  color: var(--color-accent-kicker);
  border: 1px solid var(--color-border);
}

.op-title {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.op-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.op-card-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.op-metric-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.78rem;
}

.op-lbl {
  color: var(--color-text-muted);
}

.op-val {
  font-weight: 600;
  color: var(--color-text-main);
}

.status-pill {
  padding: 0.15rem 0.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-primary);
}

/* Contador de días libres */
.warehouse-countdown-alert {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  margin-top: 0.5rem;
}

.warehouse-countdown-alert.normal {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
}

.warehouse-countdown-alert.warning {
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  color: var(--color-warning-text);
}

.warehouse-countdown-alert.danger {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger-text);
}

.days-remaining {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  font-size: 0.82rem;
}

.date-deadline {
  font-size: 0.72rem;
}

.op-card-empty {
  padding: 1.5rem 0.75rem;
  text-align: center;
  font-size: 0.78rem;
  color: var(--color-text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
}

/* Bóveda Documental */
.documents-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.docs-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.docs-count {
  font-size: 0.75rem;
  color: var(--color-accent-kicker);
  font-weight: 700;
}

.docs-loading,
.docs-empty-state {
  background: var(--color-bg-base);
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  padding: 2rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.docs-empty-state i {
  font-size: 2rem;
  opacity: 0.5;
}

.docs-empty-state .sub-hint {
  font-size: 0.72rem;
  color: var(--color-text-subtle);
}

.docs-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.doc-item:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.doc-icon {
  font-size: 1.4rem;
  color: var(--color-primary);
}

.doc-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.doc-name {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.doc-sub {
  font-size: 0.68rem;
  color: var(--color-text-muted);
}

.doc-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-doc-action {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  color: var(--color-text-main);
  transition: all 0.15s ease;
}

.btn-doc-action.download:hover {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

.btn-doc-action.view:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.section-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary);
  margin: 0;
  border-bottom: 1px solid var(--color-border-subtle);
  padding-bottom: 0.35rem;
}

.loading-state, .empty-state {
  padding: 4rem 1.5rem;
  text-align: center;
  color: var(--color-text-muted);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
}

.empty-state i {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
  opacity: 0.4;
}

@media (max-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .tracking-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  .operations-grid {
    grid-template-columns: 1fr;
  }
}
</style>
