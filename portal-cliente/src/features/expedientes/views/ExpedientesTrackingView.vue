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
            v-for="e in expedientes" 
            :key="e.idExpediente" 
            class="exp-item-card" 
            :class="{ active: selectedExpediente?.idExpediente === e.idExpediente }"
            @click="selectedExpediente = e"
          >
            <div class="exp-top-row">
              <span class="exp-code font-mono">{{ e.codigoExpediente }}</span>
              <span class="canal-pill" :class="e.canalSunat?.toLowerCase()">
                Canal {{ e.canalSunat }}
              </span>
            </div>
            <div class="exp-cargo-title">{{ e.nombreMercancia || 'Carga General' }}</div>
            <div class="exp-dam-row">
              <span class="label">DAM:</span>
              <span class="val font-mono">{{ e.numeroDam || 'Pendiente' }}</span>
            </div>
            <div class="exp-footer-meta">
              <span class="date">{{ formatDate(e.fechaApertura) }}</span>
              <span class="state-text">{{ formatEstado(e.estadoExpediente) }}</span>
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
                    <div class="date-deadline">
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
                  <span class="op-val">{{ formatDateTime(selectedExpediente.fechaProgramadaTransporte) }}</span>
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

          <!-- Documentación Aduanera Descargable -->
          <div class="documents-section">
            <h4 class="section-title">Bóveda Documental del Expediente</h4>
            <div class="docs-list">
              <div class="doc-item">
                <div class="doc-icon"><i class="pi pi-file-pdf"></i></div>
                <div class="doc-meta">
                  <span class="doc-name">Declaración Aduanera de Mercancías (DAM)</span>
                  <span class="doc-sub font-mono">{{ selectedExpediente.numeroDam || 'DAM-SUNAT' }}.pdf</span>
                </div>
                <button class="btn-download" @click="descargarDoc('DAM')">
                  <i class="pi pi-download"></i>
                  <span>Descargar</span>
                </button>
              </div>

              <div class="doc-item" v-if="selectedExpediente.fechaLevanteAutorizacion">
                <div class="doc-icon"><i class="pi pi-shield"></i></div>
                <div class="doc-meta">
                  <span class="doc-name">Autorización de Levante de Carga</span>
                  <span class="doc-sub font-mono">Levante_Autorizado.pdf</span>
                </div>
                <button class="btn-download" @click="descargarDoc('LEVANTE')">
                  <i class="pi pi-download"></i>
                  <span>Descargar</span>
                </button>
              </div>

              <div class="doc-item" v-if="selectedExpediente.tieneTransporte">
                <div class="doc-icon"><i class="pi pi-map"></i></div>
                <div class="doc-meta">
                  <span class="doc-name">Guía de Remisión de Transporte Local</span>
                  <span class="doc-sub font-mono">GRT-{{ selectedExpediente.codigoTracking }}.pdf</span>
                </div>
                <button class="btn-download" @click="descargarDoc('GUIA')">
                  <i class="pi pi-download"></i>
                  <span>Descargar</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '@/core/api/apiClient'

const expedientes = ref([])
const loading = ref(true)
const selectedExpediente = ref(null)

onMounted(() => {
  cargarExpedientes()
})

async function cargarExpedientes() {
  loading.value = true
  try {
    const res = await apiClient.get('/portal/expedientes')
    if (res.data?.data) {
      expedientes.value = res.data.data
      if (expedientes.value.length > 0 && !selectedExpediente.value) {
        selectedExpediente.value = expedientes.value[0]
      }
    }
  } catch (err) {
    console.error('Error al cargar expedientes', err)
  } finally {
    loading.value = false
  }
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

function descargarDoc(tipo) {
  alert(`Descargando documento oficial ${tipo} timbrado por TransAduanas / SUNAT...`)
}
</script>

<style scoped>
.expedientes-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.view-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
}

.view-kicker {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #9a7f60);
}

.view-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main, #2b2520);
  margin: 0.2rem 0;
  letter-spacing: -0.02em;
}

.view-subtitle {
  font-size: 0.85rem;
  color: var(--color-text-muted, #6f6356);
  margin: 0;
}

.btn-secondary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.15rem;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  color: var(--color-text-main, #2b2520);
  border-radius: 6px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary:hover {
  background: var(--color-bg-subtle, #f2ebe1);
}

/* Layout en dos columnas */
.tracking-layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 1024px) {
  .tracking-layout {
    grid-template-columns: 1fr;
  }
}

/* Sidebar de despachos */
.expedientes-list {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.list-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary, #352f2a);
  margin: 0;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border-subtle, #ede5d8);
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
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 8px;
  background: var(--color-bg-base, #f6f3ee);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  transition: all 0.15s ease;
}

.exp-item-card:hover {
  background: var(--color-bg-subtle, #f2ebe1);
  border-color: var(--color-border-strong, #d5c8b6);
}

.exp-item-card.active {
  background: var(--color-bg-surface, #ffffff);
  border-color: var(--color-primary, #352f2a);
  box-shadow: 0 4px 12px rgba(53, 47, 42, 0.08);
}

.exp-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exp-code {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary, #352f2a);
}

.canal-pill {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.canal-pill.verde {
  background: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  border: 1px solid var(--color-success-border, #bce5c6);
}

.canal-pill.naranja {
  background: var(--color-warning-bg, #fdf5e6);
  color: var(--color-warning-text, #8c5b16);
  border: 1px solid var(--color-warning-border, #f8dab3);
}

.canal-pill.rojo {
  background: var(--color-danger-bg, #fdf0f0);
  color: var(--color-danger-text, #9e2a2b);
  border: 1px solid var(--color-danger-border, #f6c8c8);
}

.exp-cargo-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-main, #2b2520);
}

.exp-dam-row {
  font-size: 0.75rem;
  color: var(--color-text-muted, #6f6356);
  display: flex;
  gap: 0.35rem;
}

.exp-footer-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.7rem;
  color: var(--color-text-subtle, #998c7e);
  margin-top: 0.25rem;
  border-top: 1px dashed var(--color-border-subtle, #ede5d8);
  padding-top: 0.35rem;
}

/* Panel Principal de Tracking */
.tracking-detail {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.exp-main-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 12px;
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  box-shadow: 0 4px 20px rgba(43, 37, 32, 0.03);
}

.exp-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1.5rem;
  border-bottom: 1px solid var(--color-border, #e4dacb);
  padding-bottom: 1.5rem;
}

.kicker-exp {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker, #9a7f60);
  letter-spacing: 0.05em;
}

.dam-title {
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--color-text-main, #2b2520);
  margin: 0.25rem 0;
}

.exp-codes-sub {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: var(--color-text-muted, #6f6356);
}

.code-badge {
  background: var(--color-bg-subtle, #f2ebe1);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-weight: 700;
  color: var(--color-primary, #352f2a);
}

/* Semáforo SUNAT */
.sunat-traffic-light {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 1.25rem;
  border-radius: 10px;
  border: 1px solid var(--color-border, #e4dacb);
}

.sunat-traffic-light.verde {
  background: var(--color-success-bg, #eaf6ed);
  border-color: var(--color-success-border, #bce5c6);
  color: var(--color-success-text, #1e6638);
}

.sunat-traffic-light.naranja {
  background: var(--color-warning-bg, #fdf5e6);
  border-color: var(--color-warning-border, #f8dab3);
  color: var(--color-warning-text, #8c5b16);
}

.sunat-traffic-light.rojo {
  background: var(--color-danger-bg, #fdf0f0);
  border-color: var(--color-danger-border, #f6c8c8);
  color: var(--color-danger-text, #9e2a2b);
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
  opacity: 0.8;
}

.light-val {
  font-size: 1.2rem;
  font-weight: 800;
}

.light-desc {
  font-size: 0.72rem;
  opacity: 0.9;
}

/* Stepper */
.stepper-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1rem;
  padding: 0.5rem 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.35rem;
  position: relative;
  z-index: 2;
}

.step-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-bg-base, #f6f3ee);
  border: 2px solid var(--color-border, #e4dacb);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-text-muted, #6f6356);
}

.step.completed .step-circle {
  background: var(--color-primary, #352f2a);
  border-color: var(--color-primary, #352f2a);
  color: #ffffff;
}

.step.active .step-circle {
  background: var(--color-bg-surface, #ffffff);
  border-color: var(--color-accent-kicker, #9a7f60);
  color: var(--color-accent-kicker, #9a7f60);
  box-shadow: 0 0 0 3px rgba(154, 127, 96, 0.2);
}

.step-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-main, #2b2520);
}

.step-date {
  font-size: 0.68rem;
  color: var(--color-text-muted, #6f6356);
}

.step-line {
  flex: 1;
  height: 2px;
  background: var(--color-border, #e4dacb);
  margin: 0 0.5rem;
  position: relative;
  top: -12px;
  z-index: 1;
}

.step-line.active {
  background: var(--color-primary, #352f2a);
}

/* Grid Operativo */
.operations-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

@media (max-width: 768px) {
  .operations-grid {
    grid-template-columns: 1fr;
  }
}

.operation-card {
  background: var(--color-bg-base, #f6f3ee);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.op-card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  border-bottom: 1px solid var(--color-border-subtle, #ede5d8);
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
  background: var(--color-bg-surface, #ffffff);
  color: var(--color-primary, #352f2a);
  border: 1px solid var(--color-border, #e4dacb);
}

.op-icon.transport {
  background: var(--color-bg-surface, #ffffff);
  color: var(--color-accent-kicker, #9a7f60);
  border: 1px solid var(--color-border, #e4dacb);
}

.op-title {
  margin: 0;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
}

.op-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted, #6f6356);
}

.op-card-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.op-metric-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
}

.op-lbl {
  color: var(--color-text-muted, #6f6356);
}

.op-val {
  font-weight: 600;
  color: var(--color-text-main, #2b2520);
}

.status-pill {
  padding: 0.15rem 0.5rem;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-primary, #352f2a);
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
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  color: var(--color-text-main, #2b2520);
}

.warehouse-countdown-alert.warning {
  background: var(--color-warning-bg, #fdf5e6);
  border: 1px solid var(--color-warning-border, #f8dab3);
  color: var(--color-warning-text, #8c5b16);
}

.warehouse-countdown-alert.danger {
  background: var(--color-danger-bg, #fdf0f0);
  border: 1px solid var(--color-danger-border, #f6c8c8);
  color: var(--color-danger-text, #9e2a2b);
}

.days-remaining {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  font-size: 0.82rem;
}

.date-deadline {
  font-size: 0.72rem;
  opacity: 0.85;
}

.op-card-empty {
  padding: 1.5rem;
  text-align: center;
  font-size: 0.78rem;
  color: var(--color-text-muted, #6f6356);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
}

/* Bóveda documental */
.docs-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 0.85rem;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: var(--color-bg-base, #f6f3ee);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 8px;
}

.doc-icon {
  font-size: 1.4rem;
  color: var(--color-primary, #352f2a);
}

.doc-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.doc-name {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-main, #2b2520);
}

.doc-sub {
  font-size: 0.68rem;
  color: var(--color-text-muted, #6f6356);
}

.btn-download {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  color: var(--color-text-main, #2b2520);
  transition: all 0.15s ease;
}

.btn-download:hover {
  background: var(--color-primary, #352f2a);
  color: #ffffff;
}

.section-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary, #352f2a);
  margin: 0 0 0.85rem 0;
  border-bottom: 1px solid var(--color-border-subtle, #ede5d8);
  padding-bottom: 0.35rem;
}

.empty-state, .loading-state {
  padding: 4rem 1.5rem;
  text-align: center;
  color: var(--color-text-muted, #6f6356);
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 12px;
}

.empty-state i, .loading-state i {
  font-size: 2.2rem;
  margin-bottom: 0.5rem;
}

.font-mono { font-family: var(--font-mono); }
.font-bold { font-weight: 700; }
</style>
