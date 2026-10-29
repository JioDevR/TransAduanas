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

    <!-- Tarjetas de Resumen Financiero en USD -->
    <div class="kpi-grid" v-if="resumen">
      <div class="kpi-card">
        <div class="kpi-icon-box">
          <i class="pi pi-file"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL CONTRATADO / COTIZADO</span>
          <span class="kpi-value mono">$ {{ formatCurrency(resumen.totalCotizadoUsd) }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon-box success">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL AMORTIZADO (USD)</span>
          <span class="kpi-value mono success-text">$ {{ formatCurrency(resumen.totalAbonadoUsd) }}</span>
        </div>
      </div>

      <div class="kpi-card highlighted">
        <div class="kpi-icon-box warning">
          <i class="pi pi-wallet"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">SALDO PENDIENTE CONSOLIDADO</span>
          <span class="kpi-value mono warning-text">$ {{ formatCurrency(resumen.saldoPendienteUsd) }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon-box accent">
          <i class="pi pi-receipt"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">VOUCHERS REGISTRADOS</span>
          <span class="kpi-value mono">{{ anticipos.length }}</span>
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
        <span class="tab-badge">{{ cotizacionesPreExpediente.length }}</span>
      </button>

      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'expedientes' }" 
        @click="activeTab = 'expedientes'"
      >
        <i class="pi pi-compass"></i>
        <span>Expedientes en Trámite (Deuda Heredada)</span>
        <span class="tab-badge">{{ expedientesHeredados.length }}</span>
      </button>

      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'historial' }" 
        @click="activeTab = 'historial'"
      >
        <i class="pi pi-history"></i>
        <span>Historial de Vouchers Bancarios</span>
        <span class="tab-badge">{{ anticipos.length }}</span>
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
            <tr v-for="p in anticipos" :key="p.idPago" :class="{ 'row-observed': p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO' }">
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
              <td>{{ p.fechaOperacion }}</td>
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

                  <a 
                    v-if="p.documentoComprobanteUrl" 
                    :href="p.documentoComprobanteUrl" 
                    target="_blank" 
                    class="btn-action-view"
                    title="Ver Voucher Adjunto"
                  >
                    <i class="pi pi-external-link"></i>
                  </a>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal para Subsanar / Corregir Pago Observado -->
    <div v-if="modalCorreccionVisible" class="modal-backdrop" @click.self="modalCorreccionVisible = false">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title-box">
            <span class="modal-kicker text-danger">SUBSANACIÓN DE PAGO OBSERVADO</span>
            <h2 class="modal-title">Corregir Registro de Pago ({{ pagoSeleccionadoParaCorreccion?.codigoPago }})</h2>
          </div>
          <button class="close-btn" @click="modalCorreccionVisible = false">
            <i class="pi pi-times"></i>
          </button>
        </div>

        <form @submit.prevent="enviarCorreccion" class="modal-body">
          <div class="alert-box-obs mb-3" v-if="pagoSeleccionadoParaCorreccion?.observacionFinanzas">
            <i class="pi pi-exclamation-triangle"></i>
            <div>
              <strong>Observación emitida por Finanzas:</strong>
              <p>{{ pagoSeleccionadoParaCorreccion.observacionFinanzas }}</p>
            </div>
          </div>

          <div class="form-grid">
            <div class="form-field">
              <label>Monto Real Abonado ($ USD) *</label>
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
              <label>Banco Emisor (Su Banco):</label>
              <select v-model="correccionForm.bancoOrigen" class="custom-select">
                <option value="BCP">Banco de Crédito del Perú (BCP)</option>
                <option value="BBVA">BBVA Perú</option>
                <option value="INTERBANK">Interbank</option>
                <option value="SCOTIABANK">Scotiabank</option>
                <option value="BANBIF">BanBif</option>
              </select>
            </div>

            <div class="form-field">
              <label>Cuenta Receptora TransAduanas *</label>
              <select v-model="correccionForm.bancoDestino" required class="custom-select">
                <option value="BCP Dolares TransAduanas">BCP Dólares (Cta: 191-289381-1-09)</option>
                <option value="BBVA Dolares TransAduanas">BBVA Dólares (Cta: 0011-0182-010049281)</option>
                <option value="Interbank Dolares TransAduanas">Interbank Dólares (Cta: 200-300189281)</option>
              </select>
            </div>

            <div class="form-field">
              <label>Número de Operación Bancaria *</label>
              <input 
                v-model.trim="correccionForm.numeroOperacion" 
                type="text" 
                required 
                class="custom-input font-mono"
              />
            </div>

            <div class="form-field">
              <label>Fecha de Operación *</label>
              <input 
                v-model="correccionForm.fechaOperacion" 
                type="date" 
                required 
                class="custom-input"
              />
            </div>

            <div class="form-field full">
              <label>Reemplazar Voucher Bancario (Nuevo PDF o Imagen):</label>
              <div class="file-dropzone">
                <input 
                  type="file" 
                  id="correccion-file-input"
                  accept="application/pdf,image/png,image/jpeg,image/webp" 
                  @change="onCorreccionFileSelected"
                  class="file-input-hidden"
                />
                <label for="correccion-file-input" class="file-dropzone-label">
                  <i class="pi pi-upload"></i>
                  <span v-if="!correccionArchivoSeleccionado">Haga clic para adjuntar comprobante corregido (opcional)</span>
                  <span v-else class="file-selected-name">
                    <i class="pi pi-file"></i> {{ correccionArchivoSeleccionado.name }} ({{ (correccionArchivoSeleccionado.size / 1024).toFixed(1) }} KB)
                  </span>
                </label>
              </div>
            </div>

            <div class="form-field full">
              <label>Explicación de la Subsanación:</label>
              <textarea 
                v-model="correccionForm.observaciones" 
                rows="2" 
                placeholder="Indique las correcciones realizadas o aclaraciones para el Contador..."
                class="custom-textarea"
              ></textarea>
            </div>
          </div>

          <div class="modal-footer" style="padding: 1rem 0 0 0; margin-top: 1rem;">
            <button type="button" class="btn-secondary" @click="modalCorreccionVisible = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary" :disabled="enviandoCorreccion">
              <i v-if="enviandoCorreccion" class="pi pi-spin pi-spinner"></i>
              <span v-else>Re-enviar a Revisión de Finanzas</span>
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
import apiClient from '@/core/api/apiClient'

const router = useRouter()

const resumen = ref(null)
const obligaciones = ref([])
const anticipos = ref([])
const activeTab = ref('cotizaciones')
const loading = ref(true)

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
    const [resResumen, resObligaciones, resAnticipos] = await Promise.all([
      apiClient.get('/portal/pagos/resumen'),
      apiClient.get('/portal/pagos/obligaciones-pendientes'),
      apiClient.get('/portal/pagos/anticipos')
    ])

    if (resResumen.data?.data) resumen.value = resResumen.data.data
    if (resObligaciones.data?.data) obligaciones.value = resObligaciones.data.data
    if (resAnticipos.data?.data) anticipos.value = resAnticipos.data.data

    // Si no hay cotizaciones pero hay expedientes, activar pestaña de expedientes
    if (cotizacionesPreExpediente.value.length === 0 && expedientesHeredados.value.length > 0) {
      activeTab.value = 'expedientes'
    }
  } catch (err) {
    console.error('Error al cargar datos financieros', err)
  } finally {
    loading.value = false
  }
}

const cotizacionesPreExpediente = computed(() => {
  return obligaciones.value.filter(o => o.tipoObligacion === 'ANTICIPO_COTIZACION' || !o.tieneExpediente)
})

const expedientesHeredados = computed(() => {
  return obligaciones.value.filter(o => o.tipoObligacion === 'EXPEDIENTE_HEREDADO' || o.tieneExpediente)
})

function irARegistrarPago(tipo, id) {
  if (tipo && id) {
    router.push({ name: 'RegistrarPago', query: { tipo, id } })
  } else {
    router.push({ name: 'RegistrarPago' })
  }
}

function formatCurrency(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatEstadoPago(st) {
  if (!st) return 'PENDIENTE'
  return st.replace(/_/g, ' ')
}

function getPagoStatusClass(st) {
  switch (st) {
    case 'VALIDADO':
    case 'APROBADO':
    case 'PAGADO':
    case 'PAGADO_TOTAL':
    case 'ADELANTO_APROBADO':
      return 'success'
    case 'PENDIENTE_REVISION':
    case 'EN_CONCILIACION':
    case 'PENDIENTE':
      return 'warning'
    case 'OBSERVADO':
    case 'RECHAZADO':
      return 'danger'
    default:
      return 'neutral'
  }
}

function onCorreccionFileSelected(event) {
  const file = event.target.files?.[0]
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

async function subirComprobante(file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await apiClient.post('/portal/pagos/upload-comprobante', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data?.data?.idDocumento
}

async function enviarCorreccion() {
  enviandoCorreccion.value = true
  try {
    if (correccionArchivoSeleccionado.value) {
      const docId = await subirComprobante(correccionArchivoSeleccionado.value)
      correccionForm.value.idDocumentoComprobante = docId
    }

    const idPago = correccionForm.value.idPago
    const res = await apiClient.put(`/portal/pagos/anticipos/${idPago}/corregir`, correccionForm.value)
    if (res.data?.data) {
      modalCorreccionVisible.value = false
      cargarTodo()
      alert('Pago corregido exitosamente. Ha retornado a la bandeja de Finanzas para su revisión.')
    }
  } catch (err) {
    alert(err.response?.data?.message || 'Error al enviar subsanación de pago')
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
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
}

.view-kicker {
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #9a7f60);
  display: block;
  margin-bottom: 0.25rem;
}

.view-title {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-primary, #352f2a);
  margin: 0;
}

.view-subtitle {
  font-size: 0.9rem;
  color: var(--color-text-muted, #6f6356);
  margin-top: 0.25rem;
  max-width: 750px;
}

/* Tarjetas KPI en USD */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
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
}

.kpi-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}

.kpi-card.highlighted {
  border-color: var(--color-warning-border, #f8dab3);
  background: #fffdfa;
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
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--color-text-main, #2b2520);
}

.kpi-value.mono {
  font-family: var(--font-mono);
}

.success-text { color: var(--color-success-text, #1e6638); }
.warning-text { color: var(--color-warning-text, #8c5b16); }

.kpi-sub-text {
  font-size: 0.72rem;
  color: var(--color-text-muted, #6f6356);
}

/* Tabs */
.tabs-container {
  display: flex;
  gap: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e4dacb);
  overflow-x: auto;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-text-muted, #6f6356);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: var(--color-text-main, #2b2520);
}

.tab-btn.active {
  color: var(--color-primary, #352f2a);
  border-bottom-color: var(--color-primary, #352f2a);
}

.tab-badge {
  background: var(--color-bg-subtle, #f2ebe1);
  padding: 0.1rem 0.45rem;
  border-radius: 12px;
  font-size: 0.7rem;
  font-family: var(--font-mono);
}

/* Tabla */
.table-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}

.table-responsive {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
  text-align: left;
}

.data-table th {
  background: var(--color-bg-base, #f6f3ee);
  padding: 0.75rem 1rem;
  font-weight: 700;
  font-size: 0.7rem;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle, #998c7e);
  border-bottom: 1px solid var(--color-border, #e4dacb);
  white-space: nowrap;
}

.data-table td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-border-subtle, #f0eae1);
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background: var(--color-bg-subtle, #faf6f0);
}

.data-table tr.row-observed {
  background: #fff9f9;
}

.highlight-cell {
  background: var(--color-bg-base, #fbf8f4);
}

.text-right { text-align: right; }
.text-center { text-align: center; }

.btn-action-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-primary, #352f2a);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.45rem 0.85rem;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-action-primary:hover {
  background: #1f1b18;
}

.btn-action-correct {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: var(--color-danger-bg, #fde8e6);
  color: var(--color-danger, #b3261e);
  border: 1px solid var(--color-danger-border, #f9c2bd);
  border-radius: 4px;
  padding: 0.35rem 0.65rem;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-action-view {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  background: var(--color-bg-base, #f6f3ee);
  border: 1px solid var(--color-border, #e4dacb);
  color: var(--color-primary, #352f2a);
  text-decoration: none;
}

.action-btn-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.abono-tag {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  background: var(--color-bg-base, #f6f3ee);
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--color-primary, #352f2a);
}

.bank-box {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
}

.canal-badge {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 800;
}

.canal-badge.verde { background: #e6f4ea; color: #137333; }
.canal-badge.naranja { background: #fef7e0; color: #b06000; }
.canal-badge.rojo { background: #fce8e6; color: #c5221f; }

.status-pill {
  display: inline-block;
  padding: 0.2rem 0.6rem;
  border-radius: 12px;
  font-size: 0.7rem;
  font-weight: 700;
}

.status-pill.success {
  background: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
}

.status-pill.warning {
  background: var(--color-warning-bg, #fdf5e6);
  color: var(--color-warning-text, #8c5b16);
}

.status-pill.danger {
  background: var(--color-danger-bg, #fde8e6);
  color: var(--color-danger, #b3261e);
}

.status-pill.neutral {
  background: var(--color-bg-base, #f6f3ee);
  color: var(--color-text-muted, #6f6356);
}

.obs-inline-callout {
  margin-top: 0.35rem;
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  color: var(--color-danger, #b3261e);
  background: #fff0f0;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  text-align: left;
}

/* Modales */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-card {
  background: #fff;
  border-radius: 10px;
  width: 100%;
  max-width: 650px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 25px rgba(0,0,0,0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--color-border, #e4dacb);
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 800;
  margin: 0.25rem 0 0 0;
  color: var(--color-primary, #352f2a);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.1rem;
  color: var(--color-text-muted, #6f6356);
  cursor: pointer;
}

.modal-body {
  padding: 1.5rem;
}

.alert-box-obs {
  display: flex;
  gap: 0.75rem;
  background: #fff2f2;
  border: 1px solid #ffcccc;
  border-radius: 6px;
  padding: 0.75rem;
  color: #c5221f;
  font-size: 0.85rem;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-field.full {
  grid-column: 1 / -1;
}

.custom-input, .custom-select, .custom-textarea {
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.6rem 0.75rem;
  font-size: 0.85rem;
  width: 100%;
}

.file-dropzone {
  border: 2px dashed var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 1rem;
  text-align: center;
  cursor: pointer;
}

.file-input-hidden {
  display: none;
}

.file-dropzone-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
  font-size: 0.8rem;
  color: var(--color-text-muted, #6f6356);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-primary {
  background: var(--color-primary, #352f2a);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.65rem 1.25rem;
  font-weight: 700;
  font-size: 0.875rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-primary:hover:not(:disabled) {
  background: #1f1b18;
}

.btn-secondary {
  background: transparent;
  color: var(--color-text-muted, #6f6356);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.65rem 1rem;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.5rem;
  gap: 0.75rem;
  text-align: center;
  color: var(--color-text-muted, #6f6356);
}
</style>
