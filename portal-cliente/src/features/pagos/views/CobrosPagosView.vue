<template>
  <div class="cobros-pagos-view">
    <!-- Header de Sección -->
    <div class="view-header">
      <div class="header-titles">
        <span class="view-kicker">CONTROL FINANCIERO Y COBRANZAS // USD</span>
        <h1 class="view-title">Cobros, Pagos & Liquidaciones</h1>
        <p class="view-subtitle">Consulte su estado de cuenta empresarial en Dólares ($ USD), gestione anticipos de cotizaciones y amortice la deuda heredada de sus expedientes.</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="irARegistrarPago()">
          <i class="pi pi-plus-circle"></i>
          <span>Registrar Comprobante de Pago ($ USD)</span>
        </button>
      </div>
    </div>

    <!-- Tarjetas de Resumen Financiero en USD con numeración 01..04 Warm Editorial -->
    <div class="kpi-grid" v-if="resumen">
      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">01</span>
          <div class="kpi-icon-box">
            <i class="pi pi-file"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL CONTRATADO / COTIZADO</span>
          <span class="kpi-value font-mono">$ {{ formatCurrency(resumen.totalCotizadoUsd) }}</span>
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
          <span class="kpi-label">TOTAL AMORTIZADO (USD)</span>
          <span class="kpi-value font-mono success-text">$ {{ formatCurrency(resumen.totalAbonadoUsd) }}</span>
        </div>
      </div>

      <div class="kpi-card highlighted">
        <div class="kpi-top">
          <span class="kpi-idx">03</span>
          <div class="kpi-icon-box warning">
            <i class="pi pi-wallet"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">SALDO PENDIENTE CONSOLIDADO</span>
          <span class="kpi-value font-mono warning-text">$ {{ formatCurrency(resumen.saldoPendienteUsd) }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">04</span>
          <div class="kpi-icon-box accent">
            <i class="pi pi-receipt"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">VOUCHERS REGISTRADOS</span>
          <span class="kpi-value font-mono">{{ anticipos.length }}</span>
          <span class="kpi-sub-text" v-if="resumen.pagosPendientesRevision > 0">
            {{ resumen.pagosPendientesRevision }} pendiente(s) de conciliación
          </span>
          <span class="kpi-sub-text" v-else>Conciliaciones al día</span>
        </div>
      </div>
    </div>

    <!-- Pestañas de Navegación Financiera -->
    <div class="tabs-container">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'cotizaciones' }" 
        @click="activeTab = 'cotizaciones'"
      >
        <i class="pi pi-clock"></i>
        <span>Cotizaciones por Iniciar (Pre-Expediente)</span>
        <span class="tab-badge font-mono">{{ cotizacionesPreExpediente.length }}</span>
      </button>

      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'expedientes' }" 
        @click="activeTab = 'expedientes'"
      >
        <i class="pi pi-compass"></i>
        <span>Expedientes en Trámite (Deuda Heredada)</span>
        <span class="tab-badge font-mono">{{ expedientesHeredados.length }}</span>
      </button>

      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'historial' }" 
        @click="activeTab = 'historial'"
      >
        <i class="pi pi-history"></i>
        <span>Historial de Vouchers Bancarios</span>
        <span class="tab-badge font-mono">{{ anticipos.length }}</span>
      </button>
    </div>

    <!-- TAB 1: Cotizaciones Pre-Expediente -->
    <div v-if="activeTab === 'cotizaciones'" class="table-card">
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando cotizaciones pendientes de anticipo...</span>
      </div>

      <div v-else-if="cotizacionesPreExpediente.length === 0" class="empty-state">
        <i class="pi pi-check-circle success-text" style="font-size: 2rem;"></i>
        <h3>No hay cotizaciones pendientes de anticipo</h3>
        <p>Todas sus cotizaciones aprobadas ya cuentan con expediente aperturado o no registran saldo pendiente.</p>
      </div>

      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>CÓDIGO COTIZACIÓN</th>
              <th>MERCANCÍA / DESCRIPCIÓN</th>
              <th class="text-right">TOTAL COTIZADO ($)</th>
              <th class="text-right">TOTAL AMORTIZADO ($)</th>
              <th class="text-right">SALDO PENDIENTE ($)</th>
              <th class="text-right">ANTICIPO SUGERIDO (50%)</th>
              <th class="text-center">ESTADO PAGO</th>
              <th class="text-center">ACCIÓN</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in cotizacionesPreExpediente" :key="c.idReferencia">
              <td class="font-mono font-bold">{{ c.codigo }}</td>
              <td>{{ c.descripcion }}</td>
              <td class="text-right font-mono">$ {{ formatCurrency(c.totalContratadoUsd) }}</td>
              <td class="text-right font-mono success-text">$ {{ formatCurrency(c.totalAmortizadoUsd) }}</td>
              <td class="text-right font-mono font-bold warning-text">$ {{ formatCurrency(c.saldoPendienteUsd) }}</td>
              <td class="text-right font-mono highlight-cell">$ {{ formatCurrency(c.montoSugeridoAnticipoUsd) }}</td>
              <td class="text-center">
                <span class="status-pill" :class="getPagoStatusClass(c.estadoPago)">
                  {{ formatEstadoPago(c.estadoPago) }}
                </span>
              </td>
              <td class="text-center">
                <button 
                  type="button" 
                  class="btn-action-primary" 
                  @click="irARegistrarPago('cotizacion', c.idCotizacion || c.idReferencia)"
                >
                  <i class="pi pi-wallet"></i>
                  <span>Abonar Anticipo</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- TAB 2: Expedientes con Deuda Heredada -->
    <div v-if="activeTab === 'expedientes'" class="table-card">
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando expedientes con deuda heredada...</span>
      </div>

      <div v-else-if="expedientesHeredados.length === 0" class="empty-state">
        <i class="pi pi-check-circle success-text" style="font-size: 2rem;"></i>
        <h3>No hay expedientes con saldo deudor pendiente</h3>
        <p>Todos sus despachos aduaneros se encuentran al día con sus liquidaciones y amortizaciones en USD.</p>
      </div>

      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>CÓDIGO EXPEDIENTE</th>
              <th>DAM / DUA</th>
              <th>COTIZACIÓN ORIGEN</th>
              <th>CANAL SUNAT</th>
              <th class="text-right">DEUDA CONSOLIDADA ($)</th>
              <th class="text-right">AMORTIZADO ($)</th>
              <th class="text-right">SALDO PENDIENTE ($)</th>
              <th class="text-center">ACCIÓN</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in expedientesHeredados" :key="e.idReferencia">
              <td class="font-mono font-bold">{{ e.codigoExpediente }}</td>
              <td class="font-mono">{{ e.numeroDam || 'En numeración' }}</td>
              <td class="font-mono">{{ e.codigoCotizacion }}</td>
              <td>
                <span v-if="e.canalSunat" class="canal-badge" :class="e.canalSunat.toLowerCase()">
                  {{ e.canalSunat }}
                </span>
                <span v-else class="text-muted font-sm">Pendiente</span>
              </td>
              <td class="text-right font-mono font-bold">$ {{ formatCurrency(e.totalContratadoUsd) }}</td>
              <td class="text-right font-mono success-text">$ {{ formatCurrency(e.totalAmortizadoUsd) }}</td>
              <td class="text-right font-mono font-bold warning-text">$ {{ formatCurrency(e.saldoPendienteUsd) }}</td>
              <td class="text-center">
                <button 
                  type="button" 
                  class="btn-action-primary" 
                  @click="irARegistrarPago('expediente', e.idExpediente || e.idReferencia)"
                >
                  <i class="pi pi-credit-card"></i>
                  <span>Amortizar / Pagar Saldo</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- TAB 3: Historial de Vouchers Bancarios -->
    <div v-if="activeTab === 'historial'" class="table-card">
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando bitácora de vouchers bancarios...</span>
      </div>

      <div v-else-if="anticipos.length === 0" class="empty-state">
        <i class="pi pi-folder-open"></i>
        <h3>No se registran comprobantes aún</h3>
        <p>Cuando realice un abono bancario en Dólares y registre el comprobante, aparecerá en esta bandeja para seguimiento contable.</p>
      </div>

      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>CÓDIGO PAGO</th>
              <th>DOCUMENTO</th>
              <th>TIPO ABONO</th>
              <th class="text-right">MONTO ABONADO</th>
              <th>BANCO ORIGEN / DESTINO</th>
              <th class="font-mono">N° OPERACIÓN</th>
              <th>FECHA OPERACIÓN</th>
              <th class="text-center">ESTADO FINANZAS</th>
              <th class="text-center">ACCIONES</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in paginatedAnticipos" :key="p.idPago" :class="{ 'row-observed': p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO' }">
              <td class="font-mono font-bold">{{ p.codigoPago }}</td>
              <td class="font-mono">{{ p.codigoCotizacion }}</td>
              <td>
                <span class="abono-tag">{{ p.tipoAbono }}</span>
              </td>
              <td class="text-right font-mono font-bold highlight-cell">
                $ {{ formatCurrency(p.montoAbonado) }}
              </td>
              <td>
                <div class="bank-box">
                  <span class="bank-origin">{{ p.bancoOrigen || 'Bco. Emisor' }}</span>
                  <i class="pi pi-arrow-right"></i>
                  <span class="bank-dest">{{ p.bancoDestino }}</span>
                </div>
              </td>
              <td class="font-mono">{{ p.numeroOperacion }}</td>
              <td class="font-mono">{{ p.fechaOperacion }}</td>
              <td class="text-center">
                <span class="status-pill" :class="getPagoStatusClass(p.estadoPago)">
                  {{ formatEstadoPago(p.estadoPago) }}
                </span>
                <div v-if="(p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO') && p.observacionFinanzas" class="obs-inline-callout">
                  <i class="pi pi-exclamation-triangle"></i>
                  <span>{{ p.observacionFinanzas }}</span>
                </div>
              </td>
              <td class="text-center">
                <div class="action-btn-group">
                  <button 
                    v-if="p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO'"
                    type="button" 
                    class="btn-action-correct" 
                    @click="abrirModalCorreccion(p)"
                    title="Subsanar Voucher o Corregir Pago"
                  >
                    <i class="pi pi-pencil"></i>
                    <span>Subsanar</span>
                  </button>

                  <!-- Botón previsualizar comprobante conectado a portalPagoService -->
                  <button 
                    v-if="p.idDocumentoComprobante" 
                    type="button" 
                    class="btn-action-view"
                    title="Ver Voucher Bancario Oficial"
                    @click="verComprobante(p.idDocumentoComprobante)"
                  >
                    <i class="pi pi-file"></i>
                    <span>Voucher</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Paginador Compartido de Historial -->
        <BasePagination 
          v-if="totalAnticipos > pageSize"
          v-model:currentPage="currentHistorialPage"
          :total-pages="totalHistorialPages"
          :total-items="totalAnticipos"
          :start-index="startIndexHistorial"
          :end-index="endIndexHistorial"
          item-label="vouchers"
        />
      </div>
    </div>

    <!-- Modal para Subsanar / Corregir Pago Observado -->
    <div v-if="modalCorreccionVisible" class="modal-backdrop" @click.self="modalCorreccionVisible = false">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title-box">
            <span class="modal-kicker text-danger">SUBSANACIÓN DE PAGO OBSERVADO</span>
            <h2 class="modal-title font-mono">Corregir Registro de Pago ({{ pagoSeleccionadoParaCorreccion?.codigoPago }})</h2>
          </div>
          <button class="close-btn" @click="modalCorreccionVisible = false">
            <i class="pi pi-times"></i>
          </button>
        </div>

        <form @submit.prevent="enviarCorreccion" class="modal-body">
          <div class="alert-box-obs" v-if="pagoSeleccionadoParaCorreccion?.observacionFinanzas">
            <i class="pi pi-info-circle"></i>
            <div>
              <strong>Observación emitida por Finanzas:</strong>
              <p>{{ pagoSeleccionadoParaCorreccion.observacionFinanzas }}</p>
            </div>
          </div>

          <div class="form-grid">
            <div class="form-field">
              <label>Monto Abonado ($ USD) *</label>
              <input 
                v-model.number="correccionForm.montoAbonado" 
                type="number" 
                step="0.01" 
                min="0.01" 
                required 
                class="custom-input font-mono"
              />
            </div>

            <div class="form-field">
              <label>Fecha de Operación Bancaria *</label>
              <input 
                v-model="correccionForm.fechaOperacion" 
                type="date" 
                required 
                class="custom-input font-mono"
              />
            </div>

            <div class="form-field">
              <label>Banco Emisor (Origen) *</label>
              <input 
                v-model="correccionForm.bancoOrigen" 
                type="text" 
                placeholder="Ej. BCP, BBVA, Interbank..." 
                required 
                class="custom-input"
              />
            </div>

            <div class="form-field">
              <label>N° Constancia / Operación Bancaria *</label>
              <input 
                v-model="correccionForm.numeroOperacion" 
                type="text" 
                placeholder="Número correlativo de voucher" 
                required 
                class="custom-input font-mono"
              />
            </div>

            <div class="form-field full">
              <label>Reemplazar Archivo de Voucher (Opcional si es el mismo)</label>
              <div class="file-dropzone">
                <input 
                  type="file" 
                  id="voucher-corr-input" 
                  accept="application/pdf,image/png,image/jpeg,image/webp" 
                  @change="onFileCorreccionSelected"
                  class="file-input-hidden"
                />
                <label for="voucher-corr-input" class="dropzone-label">
                  <i class="pi pi-upload"></i>
                  <span v-if="!correccionArchivoSeleccionado">Haga clic para adjuntar nuevo comprobante o voucher</span>
                  <span v-else class="file-selected-text font-mono font-bold">{{ correccionArchivoSeleccionado.name }}</span>
                </label>
              </div>
            </div>

            <div class="form-field full">
              <label>Sustento / Aclaración de la Corrección:</label>
              <textarea 
                v-model="correccionForm.observaciones" 
                rows="2" 
                placeholder="Explique las correcciones realizadas respecto a la observación contable..."
                class="custom-textarea"
              ></textarea>
            </div>
          </div>

          <div class="modal-footer" style="padding: 1.25rem 0 0 0; margin-top: 1rem;">
            <button type="button" class="btn-secondary" @click="modalCorreccionVisible = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary" :disabled="enviandoCorreccion">
              <i v-if="enviandoCorreccion" class="pi pi-spin pi-spinner"></i>
              <span v-else>Guardar Corrección y Reenviar a Revisión</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { portalPagoService } from '../services/portalPagoService'
import { useNotification } from '@/shared/composables/useNotification'
import { openBlobInNewTab } from '@/shared/utils/download'
import BasePagination from '@/shared/components/BasePagination.vue'

const router = useRouter()
const { showSuccess, showError } = useNotification()

const resumen = ref(null)
const obligaciones = ref([])
const anticipos = ref([])
const activeTab = ref('cotizaciones')
const loading = ref(true)

// Paginación de Historial
const currentHistorialPage = ref(1)
const pageSize = ref(8)

const totalAnticipos = computed(() => anticipos.value.length)
const totalHistorialPages = computed(() => Math.ceil(totalAnticipos.value / pageSize.value) || 1)
const startIndexHistorial = computed(() => (totalAnticipos.value === 0 ? 0 : (currentHistorialPage.value - 1) * pageSize.value + 1))
const endIndexHistorial = computed(() => Math.min(currentHistorialPage.value * pageSize.value, totalAnticipos.value))

const paginatedAnticipos = computed(() => {
  const start = (currentHistorialPage.value - 1) * pageSize.value
  return anticipos.value.slice(start, start + pageSize.value)
})

// Variables para Subsanación / Corrección de Pago
const modalCorreccionVisible = ref(false)
const enviandoCorreccion = ref(false)
const pagoSeleccionadoParaCorreccion = ref(null)
const correccionArchivoSeleccionado = ref(null)

const correccionForm = ref({
  idPago: null,
  montoAbonado: null,
  moneda: 'USD',
  metodoPago: 'TRANSFERENCIA_BANCARIA',
  bancoOrigen: 'BCP',
  bancoDestino: 'BCP Dolares TransAduanas',
  numeroOperacion: '',
  fechaOperacion: new Date().toISOString().substring(0, 10),
  observaciones: '',
  idDocumentoComprobante: null
})

onMounted(() => {
  cargarTodo()
})

async function cargarTodo() {
  loading.value = true
  try {
    const [dataResumen, dataObligaciones, dataAnticipos] = await Promise.all([
      portalPagoService.obtenerResumen(),
      portalPagoService.listarObligaciones(),
      portalPagoService.listarAnticipos()
    ])

    if (dataResumen) resumen.value = dataResumen
    if (dataObligaciones) obligaciones.value = dataObligaciones
    if (dataAnticipos) anticipos.value = dataAnticipos

    // Si no hay cotizaciones pero hay expedientes, activar pestaña de expedientes
    if (cotizacionesPreExpediente.value.length === 0 && expedientesHeredados.value.length > 0) {
      activeTab.value = 'expedientes'
    }
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar información financiera')
  } finally {
    loading.value = false
  }
}

const cotizacionesPreExpediente = computed(() => {
  return obligaciones.value.filter(o => o.tipoObligacion === 'COTIZACION')
})

const expedientesHeredados = computed(() => {
  return obligaciones.value.filter(o => o.tipoObligacion === 'EXPEDIENTE')
})

function formatCurrency(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatEstadoPago(pago) {
  if (!pago) return 'PENDIENTE'
  return pago.replace('_', ' ')
}

function getPagoStatusClass(st) {
  switch (st) {
    case 'PAGADO':
    case 'CONCILIADO':
    case 'PAGADO_TOTAL':
      return 'success'
    case 'PENDIENTE':
    case 'REGISTRADO':
    case 'PAGADO_PARCIAL':
      return 'warning'
    case 'OBSERVADO':
    case 'RECHAZADO':
      return 'danger'
    default:
      return 'neutral'
  }
}

function irARegistrarPago(tipo, id) {
  if (tipo && id) {
    router.push({ path: '/pagos/registrar', query: { tipo, id } })
  } else {
    router.push('/pagos/registrar')
  }
}

async function verComprobante(idDocumento) {
  try {
    const blob = await portalPagoService.previsualizarComprobante(idDocumento)
    openBlobInNewTab(blob, 'application/pdf')
  } catch (err) {
    showError('No se pudo visualizar el comprobante de pago')
  }
}

function onFileCorreccionSelected(e) {
  const file = e.target.files[0]
  if (file) {
    correccionArchivoSeleccionado.value = file
  }
}

function abrirModalCorreccion(pago) {
  pagoSeleccionadoParaCorreccion.value = pago
  correccionArchivoSeleccionado.value = null
  correccionForm.value = {
    idPago: pago.idPago,
    montoAbonado: pago.montoAbonado,
    moneda: 'USD',
    metodoPago: pago.metodoPago || 'TRANSFERENCIA_BANCARIA',
    bancoOrigen: pago.bancoOrigen || 'BCP',
    bancoDestino: pago.bancoDestino || 'BCP Dolares TransAduanas',
    numeroOperacion: pago.numeroOperacion || '',
    fechaOperacion: pago.fechaOperacion || new Date().toISOString().substring(0, 10),
    observaciones: '',
    idDocumentoComprobante: pago.idDocumentoComprobante || null
  }
  modalCorreccionVisible.value = true
}

async function enviarCorreccion() {
  enviandoCorreccion.value = true
  try {
    if (correccionArchivoSeleccionado.value) {
      const docRes = await portalPagoService.subirComprobante(correccionArchivoSeleccionado.value)
      correccionForm.value.idDocumentoComprobante = docRes?.idDocumento || docRes?.id
    }

    const idPago = correccionForm.value.idPago
    const payload = {
      monto: Number(correccionForm.value.montoAbonado),
      fechaPago: correccionForm.value.fechaOperacion,
      bancoOrigen: correccionForm.value.bancoOrigen,
      numeroOperacion: correccionForm.value.numeroOperacion,
      idDocumentoComprobante: correccionForm.value.idDocumentoComprobante,
      sustentoSubsanacion: correccionForm.value.observaciones
    }

    const res = await portalPagoService.corregirPago(idPago, payload)
    if (res) {
      modalCorreccionVisible.value = false
      showSuccess('Pago subsanado exitosamente. Ha retornado a Finanzas para su revisión contable.', 'Subsanación Enviada')
      await cargarTodo()
    }
  } catch (err) {
    showError(err.response?.data?.message || 'Error al enviar subsanación de pago')
  } finally {
    enviandoCorreccion.value = false
  }
}
</script>

<style scoped>
.cobros-pagos-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
}

/* Header */
.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.view-kicker {
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  display: block;
  margin-bottom: 0.25rem;
}

.view-title {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-primary);
  margin: 0;
}

.view-subtitle {
  font-size: 0.9rem;
  color: var(--color-text-muted);
  margin-top: 0.25rem;
  max-width: 750px;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: none;
  border-radius: 6px;
  padding: 0.75rem 1.25rem;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
  border-color: var(--color-warning-border);
  background: var(--color-warning-bg);
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
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.success-text { color: var(--color-success-text); }
.warning-text { color: var(--color-warning-text); }

.kpi-sub-text {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

/* Tabs */
.tabs-container {
  display: flex;
  gap: 0.5rem;
  border-bottom: 1px solid var(--color-border);
  overflow-x: auto;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-text-muted);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.tab-btn:hover {
  color: var(--color-text-main);
}

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-badge {
  background: var(--color-bg-subtle);
  padding: 0.1rem 0.45rem;
  border-radius: 12px;
  font-size: 0.72rem;
  font-weight: 700;
}

/* Tabla */
.table-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
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
  background: var(--color-bg-base);
  padding: 0.75rem 1rem;
  font-weight: 700;
  font-size: 0.7rem;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle);
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

.data-table td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.data-table tr.row-observed {
  background: var(--color-danger-bg);
}

.highlight-cell {
  background: var(--color-bg-base);
}

.btn-action-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: none;
  border-radius: 6px;
  padding: 0.45rem 0.85rem;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-action-primary:hover {
  background: var(--color-primary-hover);
}

.btn-action-correct {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
  border-radius: 4px;
  padding: 0.35rem 0.65rem;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-action-view {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  border-radius: 4px;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  color: var(--color-primary);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-action-view:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.action-btn-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.abono-tag {
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  background: var(--color-bg-base);
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--color-primary);
}

.bank-box {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.78rem;
}

.bank-box i {
  font-size: 0.65rem;
  color: var(--color-accent-kicker);
}

.canal-badge {
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-size: 0.68rem;
  font-weight: 700;
}

.canal-badge.verde { background: var(--color-success-bg); color: var(--color-success-text); }
.canal-badge.naranja { background: var(--color-warning-bg); color: var(--color-warning-text); }
.canal-badge.rojo { background: var(--color-danger-bg); color: var(--color-danger-text); }

.status-pill {
  padding: 0.2rem 0.55rem;
  border-radius: 12px;
  font-size: 0.68rem;
  font-weight: 700;
  display: inline-block;
}

.status-pill.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.status-pill.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.status-pill.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
}

.status-pill.neutral {
  background: var(--color-bg-base);
  color: var(--color-text-muted);
}

.obs-inline-callout {
  margin-top: 0.35rem;
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  color: var(--color-danger-text);
  background: var(--color-danger-bg);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

/* Modales */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  width: 100%;
  max-width: 650px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: var(--shadow-elevated, 0 10px 25px rgba(0,0,0,0.2));
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 800;
  margin: 0.25rem 0 0 0;
  color: var(--color-primary);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.1rem;
  color: var(--color-text-muted);
  cursor: pointer;
}

.modal-body {
  padding: 1.5rem;
}

.alert-box-obs {
  display: flex;
  gap: 0.75rem;
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  border-radius: 6px;
  padding: 0.75rem;
  color: var(--color-danger-text);
  font-size: 0.85rem;
  margin-bottom: 1.25rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-field.full {
  grid-column: span 2;
}

.form-field label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.custom-input, .custom-select, .custom-textarea {
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.6rem 0.75rem;
  font-size: 0.85rem;
  font-family: inherit;
  outline: none;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
}

.custom-input:focus, .custom-select:focus, .custom-textarea:focus {
  border-color: var(--color-primary);
}

.file-dropzone {
  border: 2px dashed var(--color-border);
  border-radius: 6px;
  padding: 1rem;
  text-align: center;
  background: var(--color-bg-base);
}

.file-input-hidden {
  display: none;
}

.dropzone-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.file-selected-text {
  color: var(--color-success-text);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  border-top: 1px solid var(--color-border);
  padding-top: 1rem;
}

.btn-secondary {
  background: transparent;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.65rem 1rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-secondary:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.loading-state, .empty-state {
  padding: 3.5rem 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  text-align: center;
  color: var(--color-text-muted);
}

@media (max-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-field.full {
    grid-column: span 1;
  }
}
</style>
