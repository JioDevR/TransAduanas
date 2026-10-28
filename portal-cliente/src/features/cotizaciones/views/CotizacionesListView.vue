<template>
  <div class="cotizaciones-view">
    <!-- Header de Sección -->
    <div class="view-header">
      <div class="header-titles">
        <span class="view-kicker">MÓDULO COMERCIAL B2B</span>
        <h1 class="view-title">Mis Cotizaciones Logísticas</h1>
        <p class="view-subtitle">Consulte sus cotizaciones de importación y exportación, desglose de costos y aprobación de servicios.</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="openSolicitudModal">
          <i class="pi pi-plus"></i>
          <span>Solicitar Nueva Cotización</span>
        </button>
      </div>
    </div>

    <!-- Tarjetas Métricas KPI -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon-box">
          <i class="pi pi-file"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL COTIZACIONES</span>
          <span class="kpi-value">{{ cotizaciones.length }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon-box success">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">APROBADAS PARA DESPACHO</span>
          <span class="kpi-value">{{ cotizacionesAprobadas }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon-box warning">
          <i class="pi pi-clock"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">EN EVALUACIÓN TÉCNICA</span>
          <span class="kpi-value">{{ cotizacionesPendientes }}</span>
        </div>
      </div>

      <div class="kpi-card highlighted">
        <div class="kpi-icon-box accent">
          <i class="pi pi-dollar"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">VALOR TOTAL COTIZADO</span>
          <span class="kpi-value mono">$ {{ formatCurrency(totalMontoCotizado) }}</span>
        </div>
      </div>
    </div>

    <!-- Barra de Filtros -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="pi pi-search"></i>
        <input 
          v-model="filtroTexto" 
          type="text" 
          placeholder="Buscar por código, mercancía o puerto..." 
          class="search-input"
        />
      </div>

      <div class="filter-group">
        <label>Régimen:</label>
        <select v-model="filtroRegimen" class="custom-select">
          <option value="">Todos los regímenes</option>
          <option value="IMPORTACION_DEFINITIVA">Importación Definitiva</option>
          <option value="EXPORTACION_DEFINITIVA">Exportación Definitiva</option>
        </select>
      </div>

      <div class="filter-group">
        <label>Estado:</label>
        <select v-model="filtroEstado" class="custom-select">
          <option value="">Todos los estados</option>
          <option value="APROBADA">Aprobadas</option>
          <option value="PENDIENTE">En Evaluación</option>
          <option value="OBSERVADA">Observadas</option>
        </select>
      </div>
    </div>

    <!-- Tabla de Cotizaciones -->
    <div class="table-card">
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando cotizaciones de su empresa...</span>
      </div>

      <div v-else-if="cotizacionesFiltradas.length === 0" class="empty-state">
        <i class="pi pi-folder-open"></i>
        <h3>No se encontraron cotizaciones</h3>
        <p>No hay registros que coincidan con los filtros seleccionados o aún no ha solicitado cotizaciones.</p>
      </div>

      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>CÓDIGO</th>
              <th>MERCANCÍA & CARGA</th>
              <th>RÉGIMEN</th>
              <th>RUTA LOGÍSTICA</th>
              <th class="text-right">VALOR FOB</th>
              <th class="text-right">TOTAL ESTIMADO</th>
              <th class="text-center">ESTADO</th>
              <th class="text-center">ESTADO PAGO</th>
              <th class="text-center">ACCIONES</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in cotizacionesFiltradas" :key="c.idCotizacion">
              <td class="font-mono font-bold">{{ c.codigoCotizacion }}</td>
              <td>
                <div class="item-name">{{ c.nombreMercancia || 'Mercancía General' }}</div>
                <div class="item-sub mono">{{ c.codigoMercancia || 'Sin partida' }} • {{ c.nombreTipoCarga || 'General' }}</div>
              </td>
              <td>
                <span class="regimen-tag" :class="c.regimenAduanero?.toLowerCase()">
                  {{ formatRegimen(c.regimenAduanero) }}
                </span>
              </td>
              <td>
                <div class="route-box">
                  <span>{{ c.nombrePuertoEnvio || 'Origen' }}</span>
                  <i class="pi pi-arrow-right"></i>
                  <span>{{ c.nombrePuertoDestino || 'Destino' }}</span>
                </div>
              </td>
              <td class="text-right font-mono">$ {{ formatCurrency(c.valorFob) }}</td>
              <td class="text-right font-mono font-bold highlight-cell">$ {{ formatCurrency(c.totalCotizado) }}</td>
              <td class="text-center">
                <span class="status-badge" :class="getStatusClass(c.estado)">
                  {{ c.estado }}
                </span>
              </td>
              <td class="text-center">
                <span class="pago-badge" :class="getPagoClass(c.estadoPago)">
                  {{ formatPago(c.estadoPago) }}
                </span>
              </td>
              <td class="text-center">
                <button class="btn-table-action" @click="verDetalle(c)" title="Ver Desglose Completo">
                  <i class="pi pi-eye"></i>
                  <span>Detalle</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal de Detalle de Cotización -->
    <div v-if="modalDetalleVisible" class="modal-backdrop" @click.self="modalDetalleVisible = false">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title-box">
            <span class="modal-kicker">DETALLE DE COTIZACIÓN COMERCIAL</span>
            <h2 class="modal-title font-mono">{{ cotizacionSeleccionada?.codigoCotizacion }}</h2>
          </div>
          <button class="close-btn" @click="modalDetalleVisible = false">
            <i class="pi pi-times"></i>
          </button>
        </div>

        <div class="modal-body" v-if="cotizacionSeleccionada">
          <!-- Información General -->
          <div class="detail-section">
            <h4 class="section-title">Información de la Operación</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">Mercancía:</span>
                <span class="detail-val">{{ cotizacionSeleccionada.nombreMercancia }} ({{ cotizacionSeleccionada.codigoMercancia }})</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Tipo de Carga:</span>
                <span class="detail-val">{{ cotizacionSeleccionada.nombreTipoCarga }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Régimen Aduanero:</span>
                <span class="detail-val">{{ formatRegimen(cotizacionSeleccionada.regimenAduanero) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Ruta Puerto:</span>
                <span class="detail-val">{{ cotizacionSeleccionada.nombrePuertoEnvio }} &rarr; {{ cotizacionSeleccionada.nombrePuertoDestino }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Transporte Local:</span>
                <span class="detail-val">{{ cotizacionSeleccionada.servicioTransporte ? 'Sí (Incluido a almacén)' : 'No incluido' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Validez Comercial:</span>
                <span class="detail-val">{{ cotizacionSeleccionada.validezDias }} días (Emitida: {{ cotizacionSeleccionada.fechaEmision }})</span>
              </div>
            </div>
          </div>

          <!-- Desglose Económico y Liquidación Estimada -->
          <div class="detail-section">
            <h4 class="section-title">Desglose de Costos y Tributos (USD)</h4>
            <div class="cost-table">
              <div class="cost-row">
                <span>Valor Mercancía (FOB estimado):</span>
                <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.valorFob) }}</span>
              </div>
              <div class="cost-row">
                <span>Flete Internacional Estimado:</span>
                <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.montoFlete) }}</span>
              </div>
              <div class="cost-row">
                <span>Seguro de Transporte Estimado:</span>
                <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.montoSeguroEstimado) }}</span>
              </div>
              <div class="cost-row">
                <span>Honorarios de Agenciamiento Aduanero:</span>
                <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.montoHonorarios) }}</span>
              </div>
              <div class="cost-row highlight">
                <span>Tributos Aduaneros Estimados (Aranceles + IGV):</span>
                <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.montoTributos) }}</span>
              </div>
              <div class="cost-row total-row">
                <span>TOTAL ESTIMADO DEL SERVICIO:</span>
                <span class="font-mono total-amount">$ {{ formatCurrency(cotizacionSeleccionada.totalCotizado) }}</span>
              </div>
            </div>
          </div>

          <!-- Estado de Pagos y Anticipos -->
          <div class="detail-section">
            <h4 class="section-title">Control Financiero de Anticipos</h4>
            <div class="financial-status-box">
              <div class="fin-col">
                <span class="fin-label">Total Cotizado:</span>
                <span class="fin-val font-mono">$ {{ formatCurrency(cotizacionSeleccionada.totalCotizado) }}</span>
              </div>
              <div class="fin-col">
                <span class="fin-label">Total Abonado:</span>
                <span class="fin-val font-mono success">$ {{ formatCurrency(cotizacionSeleccionada.totalAbonado) }}</span>
              </div>
              <div class="fin-col">
                <span class="fin-label">Saldo Pendiente:</span>
                <span class="fin-val font-mono warning">$ {{ formatCurrency(cotizacionSeleccionada.saldoPendiente) }}</span>
              </div>
            </div>
          </div>

          <!-- Banner de Observación Técnica de Aduanas -->
          <div v-if="cotizacionSeleccionada.estado === 'OBSERVADA'" class="alert-box-observation">
            <div class="alert-obs-header">
              <i class="pi pi-exclamation-triangle"></i>
              <strong>Observación Técnica de Aduanas Emitida</strong>
            </div>
            <p class="alert-obs-message">
              {{ cotizacionSeleccionada.motivoObservacion || 'Se requieren correcciones técnicas o precisiones documentarias en la propuesta comercial.' }}
            </p>
            <div class="alert-obs-footer">
              <i class="pi pi-info-circle"></i>
              <span>Su asesor comercial asignado se encuentra coordinando con usted la subsanación de estos requisitos antes del reingreso a supervisión.</span>
            </div>
          </div>

          <!-- Observaciones -->
          <div v-if="cotizacionSeleccionada.observaciones" class="observations-box">
            <i class="pi pi-info-circle"></i>
            <span>{{ cotizacionSeleccionada.observaciones }}</span>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-secondary" @click="modalDetalleVisible = false">
            Cerrar
          </button>
          <button 
            v-if="cotizacionSeleccionada?.estado === 'APROBADA'" 
            class="btn-primary" 
            @click="aceptarCotizacion(cotizacionSeleccionada.idCotizacion)"
            :disabled="aceptando"
          >
            <i class="pi pi-check"></i>
            <span>Aceptar y Autorizar Despacho</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Modal de Solicitar Nueva Cotización -->
    <div v-if="modalSolicitudVisible" class="modal-backdrop" @click.self="modalSolicitudVisible = false">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title-box">
            <span class="modal-kicker">NUEVA OPERACIÓN</span>
            <h2 class="modal-title">Solicitar Cotización de Agenciamiento</h2>
          </div>
          <button class="close-btn" @click="modalSolicitudVisible = false">
            <i class="pi pi-times"></i>
          </button>
        </div>

        <form @submit.prevent="enviarSolicitud" class="modal-body">
          <div class="form-grid">
            <div class="form-field full">
              <label>Régimen Aduanero *</label>
              <select v-model="solicitudForm.regimenAduanero" required class="custom-select">
                <option value="IMPORTACION_DEFINITIVA">Importación Definitiva</option>
                <option value="EXPORTACION_DEFINITIVA">Exportación Definitiva</option>
              </select>
            </div>

            <div class="form-field full">
              <label>Valor FOB Estimado (USD) *</label>
              <input 
                v-model.number="solicitudForm.valorFob" 
                type="number" 
                step="0.01" 
                min="1" 
                placeholder="Ej. 25000.00" 
                required 
                class="custom-input font-mono"
              />
            </div>

            <div class="form-field">
              <label>Puerto de Embarque (Origen):</label>
              <select v-model="solicitudForm.idPuertoEnvio" class="custom-select">
                <option :value="null">Seleccione puerto...</option>
                <option :value="12">Puerto de Shanghái (China)</option>
                <option :value="20">Puerto de Los Ángeles (EE.UU.)</option>
                <option :value="27">Puerto de Róterdam (Países Bajos)</option>
              </select>
            </div>

            <div class="form-field">
              <label>Puerto de Llegada (Destino):</label>
              <select v-model="solicitudForm.idPuertoDestino" class="custom-select">
                <option :value="1">Callao - Muelle Sur (DP World)</option>
                <option :value="2">Callao - Muelle Norte (APM Terminals)</option>
                <option :value="3">Puerto de Paita (Piura)</option>
              </select>
            </div>

            <div class="form-field full">
              <label class="checkbox-label">
                <input type="checkbox" v-model="solicitudForm.servicioTransporte" />
                <span>Requiero servicio de transporte terrestre hasta almacén / planta</span>
              </label>
            </div>

            <div class="form-field full">
              <label>Detalle de la Carga u Observaciones:</label>
              <textarea 
                v-model="solicitudForm.observaciones" 
                rows="3" 
                placeholder="Describa el tipo de mercancía, volumen en bultos o requerimientos especiales..."
                class="custom-textarea"
              ></textarea>
            </div>
          </div>

          <div class="modal-footer" style="padding: 1rem 0 0 0; margin-top: 1rem;">
            <button type="button" class="btn-secondary" @click="modalSolicitudVisible = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary" :disabled="enviandoSolicitud">
              <i v-if="enviandoSolicitud" class="pi pi-spin pi-spinner"></i>
              <span v-else>Enviar Solicitud a Evaluación</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import apiClient from '@/core/api/apiClient'

const cotizaciones = ref([])
const loading = ref(true)
const filtroTexto = ref('')
const filtroRegimen = ref('')
const filtroEstado = ref('')

const modalDetalleVisible = ref(false)
const cotizacionSeleccionada = ref(null)
const aceptando = ref(false)

const modalSolicitudVisible = ref(false)
const enviandoSolicitud = ref(false)
const solicitudForm = ref({
  regimenAduanero: 'IMPORTACION_DEFINITIVA',
  valorFob: null,
  idPuertoEnvio: 12,
  idPuertoDestino: 1,
  servicioTransporte: true,
  observaciones: ''
})

onMounted(() => {
  cargarCotizaciones()
})

async function cargarCotizaciones() {
  loading.value = true
  try {
    const res = await apiClient.get('/portal/cotizaciones')
    if (res.data?.data) {
      cotizaciones.value = res.data.data
    }
  } catch (err) {
    console.error('Error al cargar cotizaciones', err)
  } finally {
    loading.value = false
  }
}

const cotizacionesFiltradas = computed(() => {
  return cotizaciones.value.filter(c => {
    const matchRegimen = !filtroRegimen.value || c.regimenAduanero === filtroRegimen.value
    const matchEstado = !filtroEstado.value || c.estado === filtroEstado.value
    const query = filtroTexto.value.toLowerCase().trim()
    const matchTexto = !query 
      || c.codigoCotizacion?.toLowerCase().includes(query)
      || c.nombreMercancia?.toLowerCase().includes(query)
      || c.nombrePuertoDestino?.toLowerCase().includes(query)

    return matchRegimen && matchEstado && matchTexto
  })
})

const cotizacionesAprobadas = computed(() => {
  return cotizaciones.value.filter(c => c.estado === 'APROBADA').length
})

const cotizacionesPendientes = computed(() => {
  return cotizaciones.value.filter(c => c.estado === 'PENDIENTE').length
})

const totalMontoCotizado = computed(() => {
  return cotizaciones.value.reduce((acc, c) => acc + (Number(c.totalCotizado) || 0), 0)
})

function formatCurrency(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatRegimen(reg) {
  if (!reg) return ''
  return reg.replace('_', ' ')
}

function formatPago(pago) {
  if (!pago) return 'SIN PAGO'
  return pago.replace('_', ' ')
}

function getStatusClass(st) {
  switch (st) {
    case 'APROBADA': return 'success'
    case 'PENDIENTE': return 'warning'
    case 'OBSERVADA': return 'danger'
    case 'RECHAZADA': return 'danger'
    default: return 'neutral'
  }
}

function getPagoClass(pago) {
  switch (pago) {
    case 'PAGADO_TOTAL': return 'success'
    case 'PAGADO_PARCIAL': return 'info'
    default: return 'neutral'
  }
}

function verDetalle(c) {
  cotizacionSeleccionada.value = c
  modalDetalleVisible.value = true
}

async function aceptarCotizacion(id) {
  aceptando.value = true
  try {
    const res = await apiClient.patch(`/portal/cotizaciones/${id}/aceptar`)
    if (res.data?.data) {
      modalDetalleVisible.value = false
      cargarCotizaciones()
    }
  } catch (err) {
    alert(err.response?.data?.message || 'Error al aceptar cotización')
  } finally {
    aceptando.value = false
  }
}

function openSolicitudModal() {
  solicitudForm.value = {
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    valorFob: null,
    idPuertoEnvio: 12,
    idPuertoDestino: 1,
    servicioTransporte: true,
    observaciones: ''
  }
  modalSolicitudVisible.value = true
}

async function enviarSolicitud() {
  enviandoSolicitud.value = true
  try {
    const res = await apiClient.post('/portal/cotizaciones/solicitar', solicitudForm.value)
    if (res.data?.data) {
      modalSolicitudVisible.value = false
      cargarCotizaciones()
    }
  } catch (err) {
    alert(err.response?.data?.message || 'Error al enviar solicitud')
  } finally {
    enviandoSolicitud.value = false
  }
}
</script>

<style scoped>
.cotizaciones-view {
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

.btn-primary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.15rem;
  background: var(--color-primary, #352f2a);
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover, #484039);
}

.btn-secondary {
  padding: 0.65rem 1.15rem;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  color: var(--color-text-main, #2b2520);
  border-radius: 6px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1rem;
}

.kpi-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 2px 8px rgba(43, 37, 32, 0.02);
}

.kpi-card.highlighted {
  border-color: var(--color-border-strong, #d5c8b6);
  background: linear-gradient(180deg, var(--color-bg-surface, #ffffff) 0%, var(--color-bg-subtle, #f2ebe1) 100%);
}

.kpi-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: var(--color-bg-base, #f6f3ee);
  border: 1px solid var(--color-border-subtle, #ede5d8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  color: var(--color-primary, #352f2a);
}

.kpi-icon-box.success {
  background: var(--color-success-bg, #eaf6ed);
  border-color: var(--color-success-border, #bce5c6);
  color: var(--color-success-text, #1e6638);
}

.kpi-icon-box.warning {
  background: var(--color-warning-bg, #fdf5e6);
  border-color: var(--color-warning-border, #f8dab3);
  color: var(--color-warning-text, #8c5b16);
}

.kpi-icon-box.accent {
  background: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-accent-kicker, #9a7f60);
}

.kpi-content {
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle, #998c7e);
}

.kpi-value {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main, #2b2520);
}

.kpi-value.mono {
  font-family: var(--font-mono);
}

/* Filtros */
.filters-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 8px;
  padding: 0.85rem 1rem;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 260px;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.45rem 0.75rem;
  background: var(--color-bg-base, #f6f3ee);
}

.search-box i {
  color: var(--color-text-subtle, #998c7e);
}

.search-input {
  border: none;
  background: transparent;
  width: 100%;
  font-size: 0.82rem;
  color: var(--color-text-main, #2b2520);
  outline: none;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--color-text-muted, #6f6356);
}

.custom-select {
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  background: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  font-size: 0.82rem;
  outline: none;
}

/* Tabla */
.table-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 10px;
  overflow: hidden;
}

.table-responsive {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.82rem;
}

.data-table th {
  background: var(--color-bg-subtle, #f2ebe1);
  padding: 0.75rem 1rem;
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-text-muted, #6f6356);
  letter-spacing: 0.05em;
  border-bottom: 1px solid var(--color-border, #e4dacb);
}

.data-table td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-border-subtle, #ede5d8);
  color: var(--color-text-main, #2b2520);
}

.data-table tr:hover {
  background: var(--color-bg-base, #f6f3ee);
}

.item-name {
  font-weight: 600;
}

.item-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted, #6f6356);
}

.route-box {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.78rem;
}

.route-box i {
  font-size: 0.65rem;
  color: var(--color-accent-kicker, #9a7f60);
}

.regimen-tag {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: capitalize;
}

.regimen-tag.importacion_definitiva {
  background: var(--color-info-bg, #eef4fb);
  color: var(--color-info-text, #21598f);
  border: 1px solid var(--color-info-border, #cadff4);
}

.regimen-tag.exportacion_definitiva {
  background: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  border: 1px solid var(--color-success-border, #bce5c6);
}

.status-badge {
  display: inline-block;
  padding: 0.2rem 0.55rem;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 700;
}

.status-badge.success {
  background: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  border: 1px solid var(--color-success-border, #bce5c6);
}

.status-badge.warning {
  background: var(--color-warning-bg, #fdf5e6);
  color: var(--color-warning-text, #8c5b16);
  border: 1px solid var(--color-warning-border, #f8dab3);
}

.status-badge.info {
  background: var(--color-info-bg, #eef4fb);
  color: var(--color-info-text, #21598f);
  border: 1px solid var(--color-info-border, #cadff4);
}

.pago-badge {
  font-size: 0.7rem;
  font-weight: 600;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.pago-badge.success {
  color: var(--color-success-text, #1e6638);
}

.pago-badge.info {
  color: var(--color-info-text, #21598f);
}

.pago-badge.neutral {
  color: var(--color-text-subtle, #998c7e);
}

.btn-table-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  background: var(--color-bg-base, #f6f3ee);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-main, #2b2520);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-table-action:hover {
  background: var(--color-primary, #352f2a);
  color: #ffffff;
  border-color: var(--color-primary, #352f2a);
}

/* Modales */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.modal-card {
  background: var(--color-bg-surface, #ffffff);
  border-radius: 12px;
  width: 100%;
  max-width: 680px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--color-border, #e4dacb);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker, #9a7f60);
}

.modal-title {
  margin: 0.15rem 0 0 0;
  font-size: 1.25rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  color: var(--color-text-subtle, #998c7e);
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.section-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary, #352f2a);
  margin: 0 0 0.65rem 0;
  border-bottom: 1px solid var(--color-border-subtle, #ede5d8);
  padding-bottom: 0.35rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0.65rem 1rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
}

.detail-label {
  font-size: 0.72rem;
  color: var(--color-text-muted, #6f6356);
}

.detail-val {
  font-size: 0.82rem;
  font-weight: 600;
}

.cost-table {
  background: var(--color-bg-base, #f6f3ee);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 8px;
  overflow: hidden;
}

.cost-row {
  display: flex;
  justify-content: space-between;
  padding: 0.55rem 0.85rem;
  border-bottom: 1px solid var(--color-border-subtle, #ede5d8);
  font-size: 0.82rem;
}

.cost-row.highlight {
  background: rgba(228, 218, 203, 0.3);
  font-weight: 600;
}

.cost-row.total-row {
  background: var(--color-bg-sidebar, #ede6db);
  font-weight: 700;
  border-bottom: none;
  font-size: 0.95rem;
}

.total-amount {
  color: var(--color-primary, #352f2a);
}

.financial-status-box {
  display: flex;
  justify-content: space-between;
  background: var(--color-bg-base, #f6f3ee);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 8px;
  padding: 0.75rem 1rem;
}

.fin-col {
  display: flex;
  flex-direction: column;
}

.fin-label {
  font-size: 0.72rem;
  color: var(--color-text-muted, #6f6356);
}

.fin-val {
  font-size: 0.95rem;
  font-weight: 700;
}

.fin-val.success {
  color: var(--color-success-text, #1e6638);
}

.fin-val.warning {
  color: var(--color-warning-text, #8c5b16);
}

.observations-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 0.85rem;
  background: var(--color-warning-bg, #fdf5e6);
  border: 1px solid var(--color-warning-border, #f8dab3);
  border-radius: 6px;
  font-size: 0.78rem;
  color: var(--color-warning-text, #8c5b16);
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--color-border, #e4dacb);
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-field.full {
  grid-column: span 2;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-field label {
  font-size: 0.8rem;
  font-weight: 600;
}

.custom-textarea {
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.65rem;
  font-family: var(--font-sans);
  font-size: 0.82rem;
}

.custom-input {
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.55rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.82rem;
  cursor: pointer;
}

.empty-state, .loading-state {
  padding: 3rem 1.5rem;
  text-align: center;
  color: var(--color-text-muted, #6f6356);
}

.empty-state i, .loading-state i {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.text-right { text-align: right; }
.text-center { text-align: center; }
.font-mono { font-family: var(--font-mono); }
.font-bold { font-weight: 700; }

.alert-box-observation {
  background: rgba(220, 38, 38, 0.08);
  border: 1px solid rgba(220, 38, 38, 0.25);
  border-radius: 6px;
  padding: 0.85rem 1rem;
  margin-bottom: 1.25rem;
}

.alert-obs-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #dc2626;
  font-size: 0.875rem;
  margin-bottom: 0.4rem;
}

.alert-obs-message {
  font-size: 0.8125rem;
  color: var(--color-text-main, #2c251e);
  line-height: 1.45;
  margin: 0 0 0.5rem 0;
  padding-left: 1.35rem;
}

.alert-obs-footer {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: var(--color-text-muted, #6f6356);
  border-top: 1px dashed rgba(220, 38, 38, 0.2);
  padding-top: 0.4rem;
  padding-left: 1.35rem;
}
</style>
