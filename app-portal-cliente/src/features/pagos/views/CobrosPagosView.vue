<template>
  <div class="cobros-pagos-view">
    <!-- Header de Sección -->
    <div class="view-header">
      <div class="header-titles">
        <span class="view-kicker">CONTROL FINANCIERO Y COBRANZAS // USD</span>
        <h1 class="view-title">Cobros, Pagos & Conciliaciones</h1>
        <p class="view-subtitle">Consulte su estado de cuenta empresarial en Dólares ($ USD), gestione anticipos de cotizaciones, subsane observaciones contables y revise su bitácora de comprobantes.</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="irARegistrarPago()">
          <i class="pi pi-plus-circle"></i>
          <span>Registrar Comprobante de Pago ($ USD)</span>
        </button>
      </div>
    </div>

    <!-- Banner de Alerta de Pagos Observados (Acceso Directo a Subsanación) -->
    <div v-if="anticiposObservados.length > 0" class="observed-payments-banner">
      <div class="banner-icon">
        <i class="pi pi-exclamation-triangle"></i>
      </div>
      <div class="banner-body">
        <h4 class="banner-title">
          Atención: Tiene {{ anticiposObservados.length }} abono(s) bancario(s) con observaciones de Finanzas
        </h4>
        <p class="banner-desc">
          El área contable ha solicitado rectificar datos o adjuntar nuevo comprobante. Ingrese a la pestaña de correcciones para regularizar su despacho.
        </p>
      </div>
      <div class="banner-actions">
        <button type="button" class="btn-banner-subsanar" @click="activeTab = 'observados'">
          <i class="pi pi-exclamation-circle"></i>
          <span>Ver Observaciones ({{ anticiposObservados.length }})</span>
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

    <!-- Pestañas de Navegación Financiera Reorganizadas (Flujo Natural B2B) -->
    <div class="tabs-container">
      <!-- PESTAÑA 1: Cuentas por Pagar (Unificada) -->
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'cuentas-por-pagar' }" 
        @click="activeTab = 'cuentas-por-pagar'"
      >
        <i class="pi pi-wallet"></i>
        <span>Cuentas por Pagar (Saldos)</span>
        <span class="tab-badge font-mono">{{ obligaciones.length }}</span>
      </button>

      <!-- PESTAÑA 2: Requieren Corrección / Observados (Prioridad de Acción) -->
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'observados', 'has-alerts': anticiposObservados.length > 0 }" 
        @click="activeTab = 'observados'"
      >
        <i class="pi pi-exclamation-circle"></i>
        <span>Requieren Corrección</span>
        <span class="tab-badge" :class="anticiposObservados.length > 0 ? 'badge-danger font-mono' : 'font-mono'">
          {{ anticiposObservados.length }}
        </span>
      </button>

      <!-- PESTAÑA 3: Historial de Comprobantes (Bitácora Completa) -->
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'historial' }" 
        @click="activeTab = 'historial'"
      >
        <i class="pi pi-history"></i>
        <span>Historial de Comprobantes</span>
        <span class="tab-badge font-mono">{{ anticipos.length }}</span>
      </button>
    </div>

    <!-- ========================================================================= -->
    <!-- TAB 1: CUENTAS POR PAGAR (UNIFICADA CON PASTILLAS DE FILTRO)              -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'cuentas-por-pagar'" class="table-card">
      <!-- Barra de Filtros y Búsqueda de Cuentas por Pagar -->
      <div class="subfilter-toolbar">
        <div class="segmented-filters">
          <button 
            type="button" 
            class="seg-btn" 
            :class="{ active: filtroTipoObligacion === 'TODOS' }" 
            @click="filtroTipoObligacion = 'TODOS'"
          >
            <span>Todos los Saldos</span>
            <span class="seg-count">{{ obligaciones.length }}</span>
          </button>
          <button 
            type="button" 
            class="seg-btn" 
            :class="{ active: filtroTipoObligacion === 'COTIZACION' }" 
            @click="filtroTipoObligacion = 'COTIZACION'"
          >
            <span>Anticipos de Cotización</span>
            <span class="seg-count">{{ cotizacionesPreExpediente.length }}</span>
          </button>
          <button 
            type="button" 
            class="seg-btn" 
            :class="{ active: filtroTipoObligacion === 'EXPEDIENTE' }" 
            @click="filtroTipoObligacion = 'EXPEDIENTE'"
          >
            <span>Saldos de Despacho (DAM)</span>
            <span class="seg-count">{{ expedientesHeredados.length }}</span>
          </button>
        </div>

        <div class="search-input-wrap">
          <i class="pi pi-search"></i>
          <input 
            v-model="filtroObligacionTexto" 
            type="text" 
            placeholder="Buscar por código, mercancía o DAM..." 
            class="filter-search-input"
          />
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando cuentas por pagar y saldos pendientes...</span>
      </div>

      <div v-else-if="obligacionesFiltradas.length === 0" class="empty-state">
        <i class="pi pi-check-circle success-text" style="font-size: 2.25rem;"></i>
        <h3>No registra saldos pendientes de pago</h3>
        <p>Todas sus operaciones, cotizaciones aprobadas y despachos aduaneros se encuentran al día con sus amortizaciones en USD.</p>
      </div>

      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>DOCUMENTO</th>
              <th>TIPO</th>
              <th>MERCANCÍA & OPERACIÓN</th>
              <th class="text-right">TOTAL CONTRATADO ($)</th>
              <th class="text-right">AMORTIZADO ($)</th>
              <th class="text-right">SALDO PENDIENTE ($)</th>
              <th class="text-center">ESTADO PAGO</th>
              <th class="text-center">ACCIÓN</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in paginatedObligaciones" :key="o.idReferencia + o.tipoObligacion">
              <td class="font-mono font-bold">{{ o.codigo }}</td>
              <td>
                <span class="obligation-type-chip" :class="o.tipoObligacion === 'COTIZACION' ? 'type-cot' : 'type-exp'">
                  {{ o.tipoObligacion === 'COTIZACION' ? 'Anticipo Pre-Expediente' : 'Despacho Aduanero' }}
                </span>
              </td>
              <td>
                <div class="desc-main">{{ o.descripcion || 'Operación de Comercio Exterior' }}</div>
                <div v-if="o.tipoObligacion === 'EXPEDIENTE'" class="desc-sub font-mono">
                  DAM: {{ o.numeroDam || 'En trámite' }}
                  <span v-if="o.canalSunat" class="canal-badge ml-1" :class="o.canalSunat.toLowerCase()">
                    Canal {{ o.canalSunat }}
                  </span>
                </div>
              </td>
              <td class="text-right font-mono">$ {{ formatCurrency(o.totalContratadoUsd) }}</td>
              <td class="text-right font-mono success-text">$ {{ formatCurrency(o.totalAmortizadoUsd) }}</td>
              <td class="text-right font-mono font-bold warning-text highlight-cell">
                $ {{ formatCurrency(o.saldoPendienteUsd) }}
              </td>
              <td class="text-center">
                <span class="status-pill" :class="getPagoStatusClass(o.estadoPago)">
                  {{ formatEstadoPago(o.estadoPago) }}
                </span>
              </td>
              <td class="text-center">
                <template v-if="o.estadoPago === 'OBSERVADO' || o.estadoPago === 'RECHAZADO'">
                  <button 
                    type="button" 
                    class="btn-action-correct" 
                    @click="subsanarAbonoCotizacion(o)"
                    title="Subsanar comprobante observado por Finanzas"
                  >
                    <i class="pi pi-pencil"></i>
                    <span>Subsanar Abono</span>
                  </button>
                </template>
                <template v-else>
                  <button 
                    type="button" 
                    class="btn-action-primary" 
                    @click="irARegistrarPago(o.tipoObligacion === 'COTIZACION' ? 'cotizacion' : 'expediente', o.idCotizacion || o.idExpediente || o.idReferencia)"
                  >
                    <i :class="o.tipoObligacion === 'COTIZACION' ? 'pi pi-wallet' : 'pi pi-credit-card'"></i>
                    <span>{{ o.tipoObligacion === 'COTIZACION' ? 'Abonar Anticipo' : 'Amortizar Saldo' }}</span>
                  </button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Paginador Compartido de Cuentas por Pagar -->
        <BasePagination 
          v-if="totalObligacionesFiltradas > pageSizeObligaciones"
          v-model:currentPage="currentObligacionesPage"
          :total-pages="totalObligacionesPages"
          :total-items="totalObligacionesFiltradas"
          :start-index="startIndexObligaciones"
          :end-index="endIndexObligaciones"
          item-label="obligaciones"
        />
      </div>
    </div>

    <!-- ========================================================================= -->
    <!-- TAB 2: REQUIEREN CORRECCIÓN / OBSERVADOS (BANDEJA DE ACCIÓN DIRECTA)      -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'observados'" class="corrections-container">
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Verificando comprobantes observados...</span>
      </div>

      <!-- Estado Limpio: Sin observaciones -->
      <div v-else-if="anticiposObservados.length === 0" class="empty-state-card success">
        <div class="empty-icon-circle success">
          <i class="pi pi-check-circle"></i>
        </div>
        <h3>¡Sus comprobantes están al día!</h3>
        <p>No registra abonos bancarios observados por Finanzas. Todos sus depósitos han sido conciliados exitosamente o se encuentran en revisión contable regular.</p>
        <button type="button" class="btn-secondary mt-3" @click="activeTab = 'historial'">
          <i class="pi pi-history"></i>
          <span>Consultar Historial de Vouchers</span>
        </button>
      </div>

      <!-- Lista de Tarjetas de Abonos Observados -->
      <div v-else class="observed-cards-list">
        <div class="observed-section-intro">
          <i class="pi pi-info-circle"></i>
          <span>Los siguientes comprobantes registraron observaciones contables. Haga clic en <strong>Subsanar</strong> para rectificar los datos o adjuntar un nuevo voucher en pantalla completa.</span>
        </div>

        <div v-for="p in anticiposObservados" :key="p.idPago" class="observed-audit-card">
          <div class="obs-card-header">
            <div class="obs-card-titles">
              <span class="obs-kicker font-mono">PAGO OBSERVADO // {{ p.codigoPago }}</span>
              <h3 class="obs-title font-mono">Cotización: {{ p.codigoCotizacion || 'N/A' }}</h3>
              <span class="obs-date font-mono">Registrado el: {{ p.fechaOperacion || 'N/A' }}</span>
            </div>

            <div class="obs-card-amount">
              <span class="amount-lbl">Monto Declarado</span>
              <span class="amount-val font-mono">$ {{ formatCurrency(p.montoAbonado) }} USD</span>
              <span class="status-pill danger">OBSERVADO POR FINANZAS</span>
            </div>
          </div>

          <!-- Dictamen Contable Emitido -->
          <div class="obs-dictamen-box">
            <div class="dictamen-icon">
              <i class="pi pi-exclamation-triangle"></i>
            </div>
            <div class="dictamen-content">
              <strong>Motivo de la Observación de Auditoría Contable:</strong>
              <p class="dictamen-text">
                {{ p.observacionFinanzas || 'El voucher bancario es ilegible o el número de operación no coincide con los extractos de la cuenta bancaria.' }}
              </p>
            </div>
          </div>

          <!-- Metadatos de la Transferencia Bancaria Registrada -->
          <div class="obs-card-meta-row">
            <div class="meta-item">
              <span class="meta-lbl">Banco Emisor (Origen):</span>
              <span class="meta-val">{{ p.bancoOrigen || 'N/A' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-lbl">Banco Destino SGA:</span>
              <span class="meta-val">{{ p.bancoDestino }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-lbl">N° Operación:</span>
              <span class="meta-val font-mono">{{ p.numeroOperacion }}</span>
            </div>
            <div class="meta-item" v-if="p.idDocumentoComprobante">
              <span class="meta-lbl">Comprobante Anterior:</span>
              <button type="button" class="btn-link-voucher" @click="verComprobante(p.idDocumentoComprobante)">
                <i class="pi pi-file"></i>
                <span>Ver Voucher Observado</span>
              </button>
            </div>
          </div>

          <!-- Barra de Acción Directa -->
          <div class="obs-card-footer">
            <button type="button" class="btn-subsanar-primary" @click="irASubsanarPago(p.idPago)">
              <i class="pi pi-pencil"></i>
              <span>Subsanar Abono en Pantalla Completa</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========================================================================= -->
    <!-- TAB 3: HISTORIAL DE COMPROBANTES (BITÁCORA Y CONCILIACIÓN)                -->
    <!-- ========================================================================= -->
    <div v-if="activeTab === 'historial'" class="table-card">
      <!-- Barra de Filtros del Historial -->
      <div class="subfilter-toolbar">
        <div class="search-input-wrap">
          <i class="pi pi-search"></i>
          <input 
            v-model="filtroHistorialTexto" 
            type="text" 
            placeholder="Buscar por código de pago, cotización o N° operación..." 
            class="filter-search-input"
          />
        </div>

        <div class="select-filter-wrap">
          <label>Estado Contable:</label>
          <select v-model="filtroHistorialEstado" class="custom-select font-sm">
            <option value="">Todos los comprobantes</option>
            <option value="PENDIENTE_REVISION">En Revisión Contable</option>
            <option value="VALIDADO">Validados / Conciliados</option>
            <option value="OBSERVADO">Observados / Por Subsanar</option>
          </select>
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando bitácora de vouchers bancarios...</span>
      </div>

      <div v-else-if="anticiposFiltrados.length === 0" class="empty-state">
        <i class="pi pi-folder-open"></i>
        <h3>No se encontraron comprobantes</h3>
        <p>No existen registros que coincidan con los filtros aplicados o aún no ha registrado transferencias bancarias en USD.</p>
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
                    @click="irASubsanarPago(p.idPago)"
                    title="Subsanar Voucher o Corregir Pago"
                  >
                    <i class="pi pi-pencil"></i>
                    <span>Subsanar</span>
                  </button>

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
          v-if="totalAnticiposFiltrados > pageSize"
          v-model:currentPage="currentHistorialPage"
          :total-pages="totalHistorialPages"
          :total-items="totalAnticiposFiltrados"
          :start-index="startIndexHistorial"
          :end-index="endIndexHistorial"
          item-label="vouchers"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { portalPagoService } from '../services/portalPagoService'
import { useNotification } from '@/shared/composables/useNotification'
import { openBlobInNewTab } from '@/shared/utils/download'
import BasePagination from '@/shared/components/BasePagination.vue'
import {
  formatCurrency,
  formatEstadoPago,
  getPagoStatusClass
} from '@/shared/utils/formatters'

const router = useRouter()
const route = useRoute()
const { showError, showWarn } = useNotification()

const resumen = ref(null)
const obligaciones = ref([])
const anticipos = ref([])
const activeTab = ref('cuentas-por-pagar')
const loading = ref(true)

// Filtros de Cuentas por Pagar
const filtroTipoObligacion = ref('TODOS')
const filtroObligacionTexto = ref('')
const currentObligacionesPage = ref(1)
const pageSizeObligaciones = ref(8)

// Filtros de Historial
const filtroHistorialTexto = ref('')
const filtroHistorialEstado = ref('')
const currentHistorialPage = ref(1)
const pageSize = ref(8)

// Obligaciones divididas por tipo
const cotizacionesPreExpediente = computed(() => {
  return obligaciones.value.filter(o => o.tipoObligacion === 'COTIZACION')
})

const expedientesHeredados = computed(() => {
  return obligaciones.value.filter(o => o.tipoObligacion === 'EXPEDIENTE')
})

// Filtrado de Obligaciones
const obligacionesFiltradas = computed(() => {
  return obligaciones.value.filter(o => {
    if (filtroTipoObligacion.value !== 'TODOS' && o.tipoObligacion !== filtroTipoObligacion.value) {
      return false
    }
    const query = filtroObligacionTexto.value.toLowerCase().trim()
    if (query) {
      const matchCod = o.codigo?.toLowerCase().includes(query)
      const matchDesc = o.descripcion?.toLowerCase().includes(query)
      const matchDam = o.numeroDam?.toLowerCase().includes(query)
      return matchCod || matchDesc || matchDam
    }
    return true
  })
})

const totalObligacionesFiltradas = computed(() => obligacionesFiltradas.value.length)
const totalObligacionesPages = computed(() => Math.ceil(totalObligacionesFiltradas.value / pageSizeObligaciones.value) || 1)
const startIndexObligaciones = computed(() => (totalObligacionesFiltradas.value === 0 ? 0 : (currentObligacionesPage.value - 1) * pageSizeObligaciones.value + 1))
const endIndexObligaciones = computed(() => Math.min(currentObligacionesPage.value * pageSizeObligaciones.value, totalObligacionesFiltradas.value))

const paginatedObligaciones = computed(() => {
  const start = (currentObligacionesPage.value - 1) * pageSizeObligaciones.value
  return obligacionesFiltradas.value.slice(start, start + pageSizeObligaciones.value)
})

watch([filtroTipoObligacion, filtroObligacionTexto], () => {
  currentObligacionesPage.value = 1
})

// Anticipos Observados o Rechazados por Finanzas
const anticiposObservados = computed(() => {
  return anticipos.value.filter(a => a.estadoPago === 'OBSERVADO' || a.estadoPago === 'RECHAZADO')
})

// Filtrado de Historial
const anticiposFiltrados = computed(() => {
  return anticipos.value.filter(p => {
    if (filtroHistorialEstado.value) {
      if (filtroHistorialEstado.value === 'OBSERVADO' && p.estadoPago !== 'OBSERVADO' && p.estadoPago !== 'RECHAZADO') {
        return false
      }
      if (filtroHistorialEstado.value !== 'OBSERVADO' && p.estadoPago !== filtroHistorialEstado.value) {
        return false
      }
    }
    const query = filtroHistorialTexto.value.toLowerCase().trim()
    if (query) {
      const matchCod = p.codigoPago?.toLowerCase().includes(query)
      const matchCot = p.codigoCotizacion?.toLowerCase().includes(query)
      const matchOp = p.numeroOperacion?.toLowerCase().includes(query)
      return matchCod || matchCot || matchOp
    }
    return true
  })
})

const totalAnticiposFiltrados = computed(() => anticiposFiltrados.value.length)
const totalHistorialPages = computed(() => Math.ceil(totalAnticiposFiltrados.value / pageSize.value) || 1)
const startIndexHistorial = computed(() => (totalAnticiposFiltrados.value === 0 ? 0 : (currentHistorialPage.value - 1) * pageSize.value + 1))
const endIndexHistorial = computed(() => Math.min(currentHistorialPage.value * pageSize.value, totalAnticiposFiltrados.value))

const paginatedAnticipos = computed(() => {
  const start = (currentHistorialPage.value - 1) * pageSize.value
  return anticiposFiltrados.value.slice(start, start + pageSize.value)
})

watch([filtroHistorialTexto, filtroHistorialEstado], () => {
  currentHistorialPage.value = 1
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

    verificarQuerySubsanacion()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar información financiera')
  } finally {
    loading.value = false
  }
}

watch(() => route.query, () => {
  verificarQuerySubsanacion()
})

function verificarQuerySubsanacion() {
  if (route.query.tab) {
    const tabParam = String(route.query.tab)
    if (tabParam === 'observados') {
      activeTab.value = 'observados'
    } else if (tabParam === 'historial') {
      activeTab.value = 'historial'
    } else {
      activeTab.value = 'cuentas-por-pagar'
    }
  }

  if (route.query.subsanarPago) {
    const idPago = Number(route.query.subsanarPago)
    router.push(`/pagos/subsanar/${idPago}`)
    return
  }

  if (route.query.idCotizacion) {
    const idCot = Number(route.query.idCotizacion)
    const pago = anticipos.value.find(p => p.idCotizacion === idCot && (p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO'))
    if (pago) {
      router.push(`/pagos/subsanar/${pago.idPago}`)
    } else {
      activeTab.value = 'observados'
    }
  }
}

function subsanarAbonoCotizacion(o) {
  if (!o) return
  const idCot = o.idCotizacion || o.idReferencia
  const pago = anticipos.value.find(p => p.idCotizacion === idCot && (p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO'))
  if (pago) {
    router.push(`/pagos/subsanar/${pago.idPago}`)
  } else {
    activeTab.value = 'observados'
    showWarn('Localizando abono en la bandeja de correcciones...')
  }
}

function irASubsanarPago(idPago) {
  if (!idPago) return
  router.push(`/pagos/subsanar/${idPago}`)
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

/* Banner de Pagos Observados */
.observed-payments-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.25rem;
  padding: 1rem 1.25rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-warning-text);
  margin-top: 0.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.observed-payments-banner .banner-icon {
  font-size: 1.5rem;
  color: var(--color-warning-text);
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.observed-payments-banner .banner-body {
  flex: 1;
}

.observed-payments-banner .banner-title {
  font-size: 0.95rem;
  font-weight: 700;
  margin: 0 0 0.25rem 0;
  color: inherit;
}

.observed-payments-banner .banner-desc {
  font-size: 0.8125rem;
  margin: 0;
  line-height: 1.4;
  opacity: 0.92;
}

.observed-payments-banner .banner-actions {
  flex-shrink: 0;
}

.btn-banner-subsanar {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-warning-text);
  color: #1e1e1b;
  border: none;
  padding: 0.55rem 1.1rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-banner-subsanar:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .observed-payments-banner {
    flex-direction: column;
    align-items: flex-start;
  }
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
  padding: 0.75rem 1.15rem;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-text-muted);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.tab-btn:hover {
  color: var(--color-text-main);
}

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-btn.has-alerts {
  color: var(--color-danger);
}

.tab-btn.has-alerts.active {
  border-bottom-color: var(--color-danger);
}

.tab-badge {
  background: var(--color-bg-subtle);
  padding: 0.15rem 0.5rem;
  border-radius: 12px;
  font-size: 0.72rem;
  font-weight: 700;
}

.tab-badge.badge-danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

/* Sub-toolbar de filtros dentro de la pestaña */
.subfilter-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: var(--color-bg-base);
  border-bottom: 1px solid var(--color-border);
}

.segmented-filters {
  display: flex;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.2rem;
  gap: 0.2rem;
}

.seg-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.4rem 0.85rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.seg-btn:hover {
  color: var(--color-text-main);
}

.seg-btn.active {
  background: var(--color-bg-subtle);
  color: var(--color-primary);
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.seg-count {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  background: var(--color-bg-base);
  padding: 0.1rem 0.35rem;
  border-radius: 9999px;
  border: 1px solid var(--color-border);
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 280px;
}

.search-input-wrap i {
  position: absolute;
  left: 0.75rem;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.filter-search-input {
  width: 100%;
  padding: 0.5rem 0.75rem 0.5rem 2.1rem;
  font-size: 0.8125rem;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 6px);
  outline: none;
  transition: all 0.15s ease;
}

.filter-search-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(194, 91, 59, 0.1);
}

.select-filter-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.custom-select {
  padding: 0.45rem 0.75rem;
  border-radius: 6px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  font-size: 0.8125rem;
  outline: none;
}

.custom-select:focus {
  border-color: var(--color-primary);
}

/* Chips de tipo de obligación */
.obligation-type-chip {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.obligation-type-chip.type-cot {
  background: var(--color-bg-subtle);
  color: var(--color-primary);
  border: 1px solid var(--color-border);
}

.obligation-type-chip.type-exp {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
}

.desc-main {
  font-weight: 600;
  color: var(--color-text-main);
}

.desc-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  margin-top: 0.15rem;
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
  transition: all 0.15s ease;
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
  padding: 0.45rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-action-correct:hover {
  filter: brightness(1.08);
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
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}

.obs-inline-callout {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.7rem;
  color: var(--color-danger-text);
  margin-top: 0.25rem;
  line-height: 1.25;
}

/* ========================================================================= */
/* ESTILOS DE LA PESTAÑA 2: BANDEJA DE OBSERVADOS / REQUIEREN CORRECCIÓN     */
/* ========================================================================= */
.corrections-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.observed-section-intro {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.85rem 1.25rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  font-size: 0.8125rem;
  color: var(--color-text-main);
}

.observed-section-intro i {
  color: var(--color-warning-text);
  font-size: 1.1rem;
}

.observed-cards-list {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.observed-audit-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-lg, 10px);
  padding: 1.5rem 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.obs-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.obs-card-titles {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.obs-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-danger-text);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.obs-title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.obs-date {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.obs-card-amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.amount-lbl {
  font-size: 0.6875rem;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-weight: 600;
}

.amount-val {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--color-primary);
}

.obs-dictamen-box {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-md, 8px);
}

.dictamen-icon {
  font-size: 1.35rem;
  color: var(--color-danger-text);
  margin-top: 2px;
}

.dictamen-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.dictamen-content strong {
  font-size: 0.8125rem;
  color: var(--color-danger-text);
}

.dictamen-text {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.45;
  color: var(--color-text-main);
}

.obs-card-meta-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  padding: 0.85rem 1rem;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 6px);
}

@media (max-width: 900px) {
  .obs-card-meta-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.meta-lbl {
  font-size: 0.6875rem;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-weight: 600;
}

.meta-val {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.btn-link-voucher {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: none;
  border: none;
  color: var(--color-primary);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}

.btn-link-voucher:hover {
  color: var(--color-primary-hover);
}

.obs-card-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 0.5rem;
}

.btn-subsanar-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.35rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: #fff;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md, 6px);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-subsanar-primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

/* Empty States */
.empty-state,
.empty-state-card {
  padding: 3.5rem 2rem;
  text-align: center;
  color: var(--color-text-muted);
}

.empty-state-card.success {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 10px);
}

.empty-icon-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem auto;
  font-size: 1.75rem;
}

.empty-icon-circle.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.empty-state h3,
.empty-state-card h3 {
  color: var(--color-text-main);
  margin: 0.5rem 0;
  font-size: 1.15rem;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 3rem 1rem;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.15rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 6px);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-secondary:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-hover);
}
</style>
