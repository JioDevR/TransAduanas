<template>
  <div class="cobros-pagos-view">
    <!-- Header de Sección -->
    <div class="view-header">
      <div class="header-titles">
        <span class="view-kicker">CONTROL FINANCIERO Y COBRANZAS</span>
        <h1 class="view-title">Cobros, Pagos & Liquidaciones</h1>
        <p class="view-subtitle">Consulte su estado de cuenta empresarial, historial de anticipos de cotización, liquidaciones aduaneras y registre sus vouchers bancarios.</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="openVoucherModal">
          <i class="pi pi-receipt"></i>
          <span>Registrar Comprobante de Pago</span>
        </button>
      </div>
    </div>

    <!-- Tarjetas de Resumen Financiero -->
    <div class="kpi-grid" v-if="resumen">
      <div class="kpi-card">
        <div class="kpi-icon-box">
          <i class="pi pi-file"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL COTIZADO</span>
          <span class="kpi-value mono">$ {{ formatCurrency(resumen.totalCotizadoUsd) }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon-box success">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL AMORTIZADO</span>
          <span class="kpi-value mono success-text">$ {{ formatCurrency(resumen.totalAbonadoUsd) }}</span>
        </div>
      </div>

      <div class="kpi-card highlighted">
        <div class="kpi-icon-box warning">
          <i class="pi pi-wallet"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">SALDO PENDIENTE TOTAL</span>
          <span class="kpi-value mono warning-text">$ {{ formatCurrency(resumen.saldoPendienteUsd) }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon-box accent">
          <i class="pi pi-percentage"></i>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">LIQUIDACIONES ADUANERAS (PEN)</span>
          <span class="kpi-value mono">S/ {{ formatCurrency(resumen.totalLiquidacionesPen) }}</span>
          <span class="kpi-sub-text">SPOT Detracción: S/ {{ formatCurrency(resumen.totalDetraccionesPen) }}</span>
        </div>
      </div>
    </div>

    <!-- Pestañas de Navegación Financiera -->
    <div class="tabs-container">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'anticipos' }" 
        @click="activeTab = 'anticipos'"
      >
        <i class="pi pi-clock"></i>
        <span>Anticipos de Cotización (Pre-Expediente)</span>
        <span class="tab-badge">{{ anticipos.length }}</span>
      </button>

      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'liquidaciones' }" 
        @click="activeTab = 'liquidaciones'"
      >
        <i class="pi pi-calculator"></i>
        <span>Liquidaciones de Despacho (Tributos & Gastos)</span>
        <span class="tab-badge">{{ liquidaciones.length }}</span>
      </button>
    </div>

    <!-- Contenido de Pestaña 1: Anticipos -->
    <div v-if="activeTab === 'anticipos'" class="table-card">
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando historial de pagos y anticipos...</span>
      </div>

      <div v-else-if="anticipos.length === 0" class="empty-state">
        <i class="pi pi-folder-open"></i>
        <h3>No se registran anticipos</h3>
        <p>Cuando realice un abono bancario de cotización y registre el comprobante, aparecerá en esta bandeja.</p>
      </div>

      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>CÓDIGO PAGO</th>
              <th>COTIZACIÓN</th>
              <th>TIPO ABONO</th>
              <th class="text-right">MONTO ABONADO</th>
              <th>BANCO ORIGEN / DESTINO</th>
              <th class="font-mono">N° OPERACIÓN</th>
              <th>FECHA OPERACIÓN</th>
              <th class="text-center">ESTADO FINANZAS</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in anticipos" :key="p.idPago">
              <td class="font-mono font-bold">{{ p.codigoPago }}</td>
              <td class="font-mono">{{ p.codigoCotizacion }}</td>
              <td>
                <span class="abono-tag">{{ p.tipoAbono }}</span>
              </td>
              <td class="text-right font-mono font-bold highlight-cell">
                {{ p.moneda }} {{ formatCurrency(p.montoAbonado) }}
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
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Contenido de Pestaña 2: Liquidaciones Finales -->
    <div v-if="activeTab === 'liquidaciones'" class="table-card">
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando liquidaciones financieras de despacho...</span>
      </div>

      <div v-else-if="liquidaciones.length === 0" class="empty-state">
        <i class="pi pi-folder-open"></i>
        <h3>No se registran liquidaciones emitidas</h3>
        <p>Una vez obtenido el levante aduanero y culminado el servicio logístico, Finanzas emitirá su liquidación final.</p>
      </div>

      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>CÓDIGO LIQ.</th>
              <th>EXPEDIENTE & DAM</th>
              <th class="text-right">TRIBUTOS SUNAT</th>
              <th class="text-right">ALMACÉN & FLETE</th>
              <th class="text-right">HONORARIOS</th>
              <th class="text-right">DETRACCIÓN (12%)</th>
              <th class="text-right">TOTAL USD</th>
              <th class="text-right">TOTAL PEN</th>
              <th class="text-center">ESTADO</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in liquidaciones" :key="l.idLiquidacion">
              <td class="font-mono font-bold">{{ l.codigoLiquidacion }}</td>
              <td>
                <div class="font-mono font-bold">{{ l.codigoExpediente }}</div>
                <div class="item-sub font-mono">DAM: {{ l.numeroDam || 'Pendiente' }}</div>
              </td>
              <td class="text-right font-mono">$ {{ formatCurrency(l.totalTributosSunat) }}</td>
              <td class="text-right font-mono">$ {{ formatCurrency(Number(l.gastosAlmacenPuerto || 0) + Number(l.fleteTransporteLocal || 0)) }}</td>
              <td class="text-right font-mono">$ {{ formatCurrency(l.honorariosAgencia) }}</td>
              <td class="text-right font-mono">S/ {{ formatCurrency(l.montoDetraccion) }}</td>
              <td class="text-right font-mono font-bold highlight-cell">$ {{ formatCurrency(l.totalGeneralUsd) }}</td>
              <td class="text-right font-mono font-bold">S/ {{ formatCurrency(l.totalGeneralPen) }}</td>
              <td class="text-center">
                <span class="status-pill" :class="getPagoStatusClass(l.estadoPago)">
                  {{ l.estadoPago }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal para Registrar Comprobante de Pago -->
    <div v-if="modalVoucherVisible" class="modal-backdrop" @click.self="modalVoucherVisible = false">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title-box">
            <span class="modal-kicker">CONCILIACIÓN BANCARIA</span>
            <h2 class="modal-title">Registrar Comprobante / Voucher Bancario</h2>
          </div>
          <button class="close-btn" @click="modalVoucherVisible = false">
            <i class="pi pi-times"></i>
          </button>
        </div>

        <form @submit.prevent="enviarVoucher" class="modal-body">
          <div class="form-grid">
            <div class="form-field full">
              <label>Tipo de Obligación a Pagar *</label>
              <select v-model="voucherForm.tipoObligacion" required class="custom-select" @change="actualizarReferencias">
                <option value="ANTICIPO_COTIZACION">Anticipo de Cotización (Pre-Expediente)</option>
                <option value="LIQUIDACION_EXPEDIENTE">Liquidación Final de Despacho</option>
              </select>
            </div>

            <div class="form-field full">
              <label>Referencia de Documento *</label>
              <select v-model="voucherForm.idReferencia" required class="custom-select font-mono">
                <option :value="null">Seleccione documento a amortizar...</option>
                <option v-for="ref in opcionesReferencia" :key="ref.id" :value="ref.id">
                  {{ ref.codigo }} • {{ ref.descripcion }} (Saldo: $ {{ formatCurrency(ref.saldo) }})
                </option>
              </select>
            </div>

            <div class="form-field">
              <label>Monto Transferido / Abonado *</label>
              <input 
                v-model.number="voucherForm.montoAbonado" 
                type="number" 
                step="0.01" 
                min="0.01" 
                placeholder="0.00" 
                required 
                class="custom-input font-mono"
              />
            </div>

            <div class="form-field">
              <label>Moneda *</label>
              <select v-model="voucherForm.moneda" required class="custom-select">
                <option value="USD">Dólares Americanos (USD)</option>
                <option value="PEN">Soles Peruanos (PEN)</option>
              </select>
            </div>

            <div class="form-field">
              <label>Banco Emisor (Su Banco):</label>
              <select v-model="voucherForm.bancoOrigen" class="custom-select">
                <option value="BCP">Banco de Crédito del Perú (BCP)</option>
                <option value="BBVA">BBVA Continental</option>
                <option value="INTERBANK">Interbank</option>
                <option value="SCOTIABANK">Scotiabank</option>
                <option value="BANCO_NACION">Banco de la Nación (Detracción)</option>
              </select>
            </div>

            <div class="form-field">
              <label>Cuenta Receptora de TransAduanas *</label>
              <select v-model="voucherForm.bancoDestino" required class="custom-select">
                <option value="BCP Dolares TransAduanas">BCP Dólares (Cta: 191-289381-1-09)</option>
                <option value="BCP Soles TransAduanas">BCP Soles (Cta: 191-289381-0-08)</option>
                <option value="BBVA Dolares TransAduanas">BBVA Dólares (Cta: 0011-0182-0100)</option>
                <option value="Banco Nacion SPOT">Banco de la Nación SPOT (Cta: 00-000-12891)</option>
              </select>
            </div>

            <div class="form-field">
              <label>Número de Operación Bancaria *</label>
              <input 
                v-model.trim="voucherForm.numeroOperacion" 
                type="text" 
                placeholder="Ej. OP-8823192" 
                required 
                class="custom-input font-mono"
              />
            </div>

            <div class="form-field">
              <label>Fecha de la Transferencia *</label>
              <input 
                v-model="voucherForm.fechaOperacion" 
                type="date" 
                required 
                class="custom-input"
              />
            </div>

            <div class="form-field full">
              <label>Observaciones de Pago:</label>
              <textarea 
                v-model="voucherForm.observaciones" 
                rows="2" 
                placeholder="Indique detalles de abono o número de comprobante..."
                class="custom-textarea"
              ></textarea>
            </div>
          </div>

          <div class="modal-footer" style="padding: 1rem 0 0 0; margin-top: 1rem;">
            <button type="button" class="btn-secondary" @click="modalVoucherVisible = false">
              Cancelar
            </button>
            <button type="submit" class="btn-primary" :disabled="enviandoVoucher">
              <i v-if="enviandoVoucher" class="pi pi-spin pi-spinner"></i>
              <span v-else>Registrar Abono Bancario</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '@/core/api/apiClient'

const resumen = ref(null)
const anticipos = ref([])
const liquidaciones = ref([])
const cotizaciones = ref([])
const activeTab = ref('anticipos')
const loading = ref(true)

const modalVoucherVisible = ref(false)
const enviandoVoucher = ref(false)
const opcionesReferencia = ref([])

const voucherForm = ref({
  tipoObligacion: 'ANTICIPO_COTIZACION',
  idReferencia: null,
  montoAbonado: null,
  moneda: 'USD',
  metodoPago: 'TRANSFERENCIA_BANCARIA',
  bancoOrigen: 'BCP',
  bancoDestino: 'BCP Dolares TransAduanas',
  numeroOperacion: '',
  fechaOperacion: new Date().toISOString().substring(0, 10),
  observaciones: ''
})

onMounted(() => {
  cargarTodo()
})

async function cargarTodo() {
  loading.value = true
  try {
    const [resResumen, resAnticipos, resLiq, resCot] = await Promise.all([
      apiClient.get('/portal/pagos/resumen'),
      apiClient.get('/portal/pagos/anticipos'),
      apiClient.get('/portal/pagos/liquidaciones'),
      apiClient.get('/portal/cotizaciones')
    ])

    if (resResumen.data?.data) resumen.value = resResumen.data.data
    if (resAnticipos.data?.data) anticipos.value = resAnticipos.data.data
    if (resLiq.data?.data) liquidaciones.value = resLiq.data.data
    if (resCot.data?.data) cotizaciones.value = resCot.data.data
  } catch (err) {
    console.error('Error al cargar datos financieros', err)
  } finally {
    loading.value = false
  }
}

function formatCurrency(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatEstadoPago(st) {
  if (!st) return ''
  return st.replace('_', ' ')
}

function getPagoStatusClass(st) {
  switch (st) {
    case 'VALIDADO':
    case 'APROBADO':
    case 'PAGADO':
      return 'success'
    case 'PENDIENTE_REVISION':
    case 'EN_CONCILIACION':
      return 'warning'
    case 'RECHAZADO':
      return 'danger'
    default:
      return 'neutral'
  }
}

function openVoucherModal() {
  actualizarReferencias()
  modalVoucherVisible.value = true
}

function actualizarReferencias() {
  if (voucherForm.value.tipoObligacion === 'ANTICIPO_COTIZACION') {
    opcionesReferencia.value = cotizaciones.value.map(c => ({
      id: c.idCotizacion,
      codigo: c.codigoCotizacion,
      descripcion: c.nombreMercancia || 'Cotización',
      saldo: c.saldoPendiente
    }))
    if (opcionesReferencia.value.length > 0) {
      voucherForm.value.idReferencia = opcionesReferencia.value[0].id
      voucherForm.value.montoAbonado = opcionesReferencia.value[0].saldo
    }
  } else {
    opcionesReferencia.value = liquidaciones.value.map(l => ({
      id: l.idLiquidacion,
      codigo: l.codigoLiquidacion,
      descripcion: `Exp. ${l.codigoExpediente}`,
      saldo: l.totalGeneralUsd
    }))
    if (opcionesReferencia.value.length > 0) {
      voucherForm.value.idReferencia = opcionesReferencia.value[0].id
      voucherForm.value.montoAbonado = opcionesReferencia.value[0].saldo
    }
  }
}

async function enviarVoucher() {
  enviandoVoucher.value = true
  try {
    const res = await apiClient.post('/portal/pagos/registrar-voucher', voucherForm.value)
    if (res.data?.data) {
      modalVoucherVisible.value = false
      cargarTodo()
      alert('Voucher registrado con éxito y enviado a validación contable en Finanzas')
    }
  } catch (err) {
    alert(err.response?.data?.message || 'Error al registrar voucher')
  } finally {
    enviandoVoucher.value = false
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
}

.kpi-card.highlighted {
  border-color: var(--color-warning-border, #f8dab3);
  background: linear-gradient(180deg, var(--color-bg-surface, #ffffff) 0%, var(--color-warning-bg, #fdf5e6) 100%);
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
  font-size: 1.4rem;
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
  font-family: var(--font-mono);
  color: var(--color-text-muted, #6f6356);
}

/* Tabs */
.tabs-container {
  display: flex;
  gap: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e4dacb);
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

.abono-tag {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  background: var(--color-bg-subtle, #f2ebe1);
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
}

.bank-box {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
}

.bank-box i {
  font-size: 0.65rem;
  color: var(--color-accent-kicker, #9a7f60);
}

.bank-origin {
  font-weight: 600;
}

.bank-dest {
  color: var(--color-text-muted, #6f6356);
}

.status-pill {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: 12px;
  font-size: 0.7rem;
  font-weight: 700;
}

.status-pill.success {
  background: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  border: 1px solid var(--color-success-border, #bce5c6);
}

.status-pill.warning {
  background: var(--color-warning-bg, #fdf5e6);
  color: var(--color-warning-text, #8c5b16);
  border: 1px solid var(--color-warning-border, #f8dab3);
}

.status-pill.danger {
  background: var(--color-danger-bg, #fdf0f0);
  color: var(--color-danger-text, #9e2a2b);
  border: 1px solid var(--color-danger-border, #f6c8c8);
}

.status-pill.neutral {
  background: var(--color-bg-base, #f6f3ee);
  color: var(--color-text-muted, #6f6356);
  border: 1px solid var(--color-border, #e4dacb);
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
  max-width: 650px;
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
  font-size: 1.2rem;
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

.custom-select, .custom-input {
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.55rem;
  background: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  font-size: 0.82rem;
}

.custom-textarea {
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.55rem;
  font-family: var(--font-sans);
  font-size: 0.82rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.empty-state, .loading-state {
  padding: 3rem 1.5rem;
  text-align: center;
  color: var(--color-text-muted, #6f6356);
}

.text-right { text-align: right; }
.text-center { text-align: center; }
.font-mono { font-family: var(--font-mono); }
.font-bold { font-weight: 700; }
.highlight-cell { color: var(--color-primary, #352f2a); }
</style>
