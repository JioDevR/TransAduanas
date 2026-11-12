<template>
  <div class="cotizacion-detalle-view">
    <!-- Barra Superior de Navegación y Acciones -->
    <div class="top-nav-bar">
      <div class="nav-left">
        <button type="button" class="btn-back" @click="volver">
          <i class="pi pi-arrow-left"></i>
          <span>Volver a Mis Cotizaciones</span>
        </button>
        <div class="breadcrumbs">
          <span class="crumb-link" @click="volver">Mis Cotizaciones</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-current font-mono">{{ cotizacion?.codigoCotizacion || 'Cargando...' }}</span>
        </div>
      </div>

      <div class="nav-actions" v-if="cotizacion">
        <!-- Botón Autorizar Cotización si está aprobada comercialmente y aún no fue aceptada -->
        <button
          v-if="cotizacion.estado === 'APROBADA' && !cotizacion.aceptadaPorCliente"
          type="button"
          class="btn-action-authorize"
          :disabled="aceptando"
          @click="autorizarCotizacion"
        >
          <i :class="aceptando ? 'pi pi-spin pi-spinner' : 'pi pi-check-circle'"></i>
          <span>Autorizar y Proceder a Pagar</span>
        </button>

        <!-- Botón Subsanar si el pago fue observado -->
        <button
          v-if="cotizacion.estadoPago === 'OBSERVADO' || cotizacion.estadoPago === 'RECHAZADO'"
          type="button"
          class="btn-action-observe"
          @click="irASubsanar"
        >
          <i class="pi pi-pencil"></i>
          <span>Subsanar Abono Bancario</span>
        </button>

        <!-- Botón Abonar si está aprobada y no tiene pago completo -->
        <button
          v-else-if="cotizacion.estado === 'APROBADA' && cotizacion.estadoPago !== 'PAGADO_TOTAL'"
          type="button"
          class="btn-action-pay"
          @click="irAPagar"
        >
          <i class="pi pi-wallet"></i>
          <span>Abonar Anticipo</span>
        </button>

        <!-- Botón Imprimir / PDF -->
        <button type="button" class="btn-action-outline" @click="imprimirCotizacion">
          <i class="pi pi-print"></i>
          <span>Imprimir / PDF</span>
        </button>
      </div>
    </div>

    <!-- Estado de Carga -->
    <div v-if="loading" class="loading-state-card">
      <i class="pi pi-spin pi-spinner text-3xl"></i>
      <p>Cargando detalle de la cotización comercial...</p>
    </div>

    <!-- Error al Cargar -->
    <div v-else-if="!cotizacion" class="empty-state-card">
      <i class="pi pi-exclamation-circle text-4xl text-danger mb-2"></i>
      <h3>No se pudo cargar la cotización</h3>
      <p>Es posible que el código ingresado no exista o no pertenezca a su cuenta empresarial.</p>
      <button type="button" class="btn-primary mt-3" @click="volver">Volver al Listado</button>
    </div>

    <!-- Contenido Principal de la Vista Completa -->
    <div v-else class="detail-content-wrap">
      <!-- Cabecera de la Cotización con Metadatos Clave -->
      <div class="quote-header-card">
        <div class="quote-header-main">
          <div class="quote-title-box">
            <span class="quote-kicker">PROPUESTA LOGÍSTICA & ADUANERA OFICIAL</span>
            <h1 class="quote-code font-mono">{{ cotizacion.codigoCotizacion }}</h1>
            <p class="quote-subtitle">
              Emitida para <strong>{{ cotizacion.razonSocialCliente || 'Cliente Importador' }}</strong>
              (RUC: <span class="font-mono">{{ cotizacion.rucCliente || '-' }}</span>)
            </p>
          </div>

          <div class="quote-badges">
            <div class="badge-item">
              <span class="badge-label">Estado Técnico:</span>
              <span class="status-pill" :class="getStatusClass(cotizacion.estado)">
                {{ cotizacion.estado }}
              </span>
            </div>
            <div class="badge-item">
              <span class="badge-label">Estado Financiero:</span>
              <span class="pago-pill" :class="getPagoClass(cotizacion.estadoPago)">
                {{ formatPago(cotizacion.estadoPago) }}
              </span>
            </div>
          </div>
        </div>

        <!-- Banner de Observación Técnica de Aduanas (si aplica) -->
        <div v-if="cotizacion.estado === 'OBSERVADA'" class="alert-box-full observation mb-3">
          <i class="pi pi-exclamation-triangle alert-icon"></i>
          <div class="alert-content">
            <h4 class="alert-title">Observación Técnica de Aduanas Emitida</h4>
            <p class="alert-desc">
              {{ cotizacion.motivoObservacion || 'Se requieren correcciones técnicas o precisiones documentarias en la propuesta comercial.' }}
            </p>
            <span class="alert-footnote">
              Su asesor comercial asignado se encuentra coordinando con usted la subsanación de estos requisitos.
            </span>
          </div>
        </div>

        <!-- Banner de Abono Bancario Observado por Finanzas (si aplica) -->
        <div v-if="cotizacion.estadoPago === 'OBSERVADO' || cotizacion.estadoPago === 'RECHAZADO'" class="alert-box-full payment-obs mb-3">
          <i class="pi pi-exclamation-circle alert-icon"></i>
          <div class="alert-content">
            <h4 class="alert-title">Abono Bancario Observado por Auditoría Contable</h4>
            <p class="alert-desc">
              El área de Finanzas ha observado el comprobante o los datos de transferencia registrados para esta cotización. Por favor subsane la información para autorizar de inmediato la apertura del expediente aduanero.
            </p>
            <button type="button" class="btn-subsanar-banner" @click="irASubsanar">
              <i class="pi pi-pencil"></i>
              <span>Subsanar Abono Bancario Ahora</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Grid Principal a 2 Columnas -->
      <div class="detail-two-col-grid">
        <!-- COLUMNA IZQUIERDA (62%): Ficha Técnica y Desglose Financiero -->
        <div class="col-left">
          <!-- Tarjeta 1: Ficha de la Operación Logística -->
          <div class="panel-card">
            <div class="card-header-row">
              <div class="header-icon-title">
                <i class="pi pi-box"></i>
                <h3 class="card-title">Información de la Mercancía & Operación</h3>
              </div>
              <span class="header-tag">{{ formatRegimen(cotizacion.regimenAduanero) }}</span>
            </div>

            <div class="info-grid">
              <div class="info-item">
                <span class="info-lbl">Mercancía</span>
                <strong class="info-val">{{ cotizacion.nombreMercancia || 'Mercancía General' }}</strong>
                <span class="info-sub font-mono">Cód: {{ cotizacion.codigoMercancia || 'N/A' }}</span>
              </div>

              <div class="info-item">
                <span class="info-lbl">Tipo de Carga</span>
                <strong class="info-val">{{ cotizacion.nombreTipoCarga || 'Carga General' }}</strong>
              </div>

              <div class="info-item">
                <span class="info-lbl">Ruta Portuaria / Aeroportuaria</span>
                <strong class="info-val">
                  {{ cotizacion.nombrePuertoEnvio || 'Puerto Origen' }} &rarr; {{ cotizacion.nombrePuertoDestino || 'Puerto Destino' }}
                </strong>
              </div>

              <div class="info-item">
                <span class="info-lbl">Asesor Comercial Asignado</span>
                <strong class="info-val">{{ cotizacion.usuarioComercialNombre || 'Pendiente de asignación' }}</strong>
              </div>

              <div class="info-item">
                <span class="info-lbl">Servicio de Transporte Terrestre</span>
                <strong class="info-val">
                  {{ cotizacion.servicioTransporte ? (cotizacion.costoTransporteInterno ? `$ ${formatCurrency(cotizacion.costoTransporteInterno)} (SGA)` : 'Incluido (Tarifa calculada)') : 'No incluido' }}
                </strong>
                <span v-if="cotizacion.servicioTransporte" class="info-sub">
                  Tramo: {{ cotizacion.origenProvincia || cotizacion.origenDepartamento || 'Origen' }} &rarr; {{ cotizacion.destinoProvincia || cotizacion.destinoDepartamento || 'Destino' }}
                </span>
              </div>

              <div class="info-item">
                <span class="info-lbl">Validez Comercial</span>
                <strong class="info-val">{{ cotizacion.validezDias || 30 }} días</strong>
                <span class="info-sub">Fecha de Emisión: {{ cotizacion.fechaEmision || 'En evaluación' }}</span>
              </div>
            </div>
          </div>

          <!-- Tarjeta 2: Desglose Oficial de Costos y Tributos (USD) -->
          <div class="panel-card">
            <div class="card-header-row">
              <div class="header-icon-title">
                <i class="pi pi-calculator"></i>
                <h3 class="card-title">Desglose Oficial de Costos y Tributos (USD)</h3>
              </div>
              <span class="header-tag font-mono">Moneda: Dólares ($ USD)</span>
            </div>

            <div class="cost-table-full">
              <div class="cost-row">
                <div class="cost-concept">
                  <span class="concept-name">Valor Mercancía (FOB estimado)</span>
                  <span class="concept-desc">Base imponible preliminar para determinación de aranceles</span>
                </div>
                <div class="cost-val font-mono">$ {{ formatCurrency(cotizacion.valorFob) }}</div>
              </div>

              <div class="cost-row">
                <div class="cost-concept">
                  <span class="concept-name">Flete Internacional Estimado</span>
                  <span class="concept-desc">Flete marítimo / aéreo internacional</span>
                </div>
                <div class="cost-val font-mono">
                  {{ Number(cotizacion.montoFlete) > 0 ? '$ ' + formatCurrency(cotizacion.montoFlete) : (esPendienteLiquidacion(cotizacion) ? 'Pendiente por asesor' : '$ 0.00') }}
                </div>
              </div>

              <div class="cost-row">
                <div class="cost-concept">
                  <span class="concept-name">Seguro de Transporte Estimado</span>
                  <span class="concept-desc">Póliza o tabla de seguro aduanero SUNAT</span>
                </div>
                <div class="cost-val font-mono">
                  {{ esPendienteLiquidacion(cotizacion) && !(Number(cotizacion.montoSeguroEstimado) > 0) ? 'Pendiente por asesor' : '$ ' + formatCurrency(cotizacion.montoSeguroEstimado) }}
                </div>
              </div>

              <div class="cost-row">
                <div class="cost-concept">
                  <span class="concept-name">Honorarios de Agenciamiento Aduanero</span>
                  <span class="concept-desc">Servicio integral de despacho y gestión técnica aduanera TransAduanas</span>
                </div>
                <div class="cost-val font-mono">
                  {{ esPendienteLiquidacion(cotizacion) && !(Number(cotizacion.montoHonorarios) > 0) ? 'Pendiente por asesor' : '$ ' + formatCurrency(cotizacion.montoHonorarios) }}
                </div>
              </div>

              <div v-if="cotizacion.servicioTransporte" class="cost-row">
                <div class="cost-concept">
                  <span class="concept-name">Flete Terrestre Local (Flota SGA)</span>
                  <span class="concept-desc">Transporte desde terminal portuario hasta almacén de destino nacional</span>
                </div>
                <div class="cost-val font-mono">
                  {{ esPendienteLiquidacion(cotizacion) && !(Number(cotizacion.costoTransporteInterno) > 0) ? 'Pendiente por asesor' : '$ ' + formatCurrency(cotizacion.costoTransporteInterno) }}
                </div>
              </div>

              <div class="cost-row highlight-tributos">
                <div class="cost-concept">
                  <span class="concept-name">Tributos Aduaneros Estimados (Aranceles + IGV + IPM)</span>
                  <span class="concept-desc">Liquidación preliminar según subpartida nacional SUNAT</span>
                </div>
                <div class="cost-val font-mono font-bold">
                  {{ cotizacion.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Inafecto ($ 0.00)' : (esPendienteLiquidacion(cotizacion) && !(Number(cotizacion.montoTributos) > 0) ? 'Pendiente por asesor' : '$ ' + formatCurrency(cotizacion.montoTributos)) }}
                </div>
              </div>

              <div class="cost-row total-row-full">
                <div class="cost-concept">
                  <span class="concept-name font-bold">TOTAL ESTIMADO DEL SERVICIO LOGÍSTICO:</span>
                  <span class="concept-desc">Total en dólares líquidos sujeto a liquidación final aduanera</span>
                </div>
                <div class="cost-val font-mono total-amount-big">
                  {{ esPendienteLiquidacion(cotizacion) ? 'En liquidación ($ 0.00)' : '$ ' + formatCurrency(cotizacion.totalCotizado) }}
                </div>
              </div>
            </div>
          </div>

          <!-- Observaciones Adicionales si existen -->
          <div v-if="cotizacion.observaciones" class="panel-card">
            <h4 class="card-subtitle-small">
              <i class="pi pi-comment mr-2"></i>
              <span>Observaciones Comerciales y Anotaciones del Servicio</span>
            </h4>
            <p class="notes-text">{{ cotizacion.observaciones }}</p>
          </div>
        </div>

        <!-- COLUMNA DERECHA (38%): Control de Anticipos y Gestor Documental Digital -->
        <div class="col-right">
          <!-- Tarjeta: Estado Contable & Anticipos -->
          <div class="panel-card">
            <div class="card-header-row">
              <div class="header-icon-title">
                <i class="pi pi-wallet"></i>
                <h3 class="card-title">Control Financiero & Anticipo</h3>
              </div>
            </div>

            <div class="finance-kpis">
              <div class="fin-kpi-item">
                <span class="fin-kpi-lbl">Total Cotizado</span>
                <strong class="fin-kpi-val font-mono">$ {{ formatCurrency(cotizacion.totalCotizado) }}</strong>
              </div>

              <div class="fin-kpi-item">
                <span class="fin-kpi-lbl">Total Amortizado</span>
                <strong class="fin-kpi-val font-mono text-success">$ {{ formatCurrency(cotizacion.totalAbonado) }}</strong>
              </div>

              <div class="fin-kpi-item highlight-saldo">
                <span class="fin-kpi-lbl">Saldo Pendiente</span>
                <strong class="fin-kpi-val font-mono text-warning">$ {{ formatCurrency(cotizacion.saldoPendiente) }}</strong>
              </div>
            </div>

            <!-- Botones de Acción Financiera Directa -->
            <div class="finance-actions-box mt-3">
              <button
                v-if="cotizacion.estadoPago === 'OBSERVADO' || cotizacion.estadoPago === 'RECHAZADO'"
                type="button"
                class="btn-full-danger"
                @click="irASubsanar"
              >
                <i class="pi pi-pencil"></i>
                <span>Subsanar Abono Observado</span>
              </button>

              <button
                v-else-if="cotizacion.estado === 'APROBADA' && cotizacion.estadoPago !== 'PAGADO_TOTAL'"
                type="button"
                class="btn-full-primary"
                @click="irAPagar"
              >
                <i class="pi pi-wallet"></i>
                <span>Registrar Comprobante / Anticipo</span>
              </button>

              <button
                type="button"
                class="btn-full-outline"
                @click="irAHistorialPagos"
              >
                <i class="pi pi-history"></i>
                <span>Ver Historial de Vouchers Bancarios</span>
              </button>
            </div>
          </div>

          <!-- Tarjeta: Documentación Técnica y Sustento Digital a Pantalla Completa -->
          <div class="panel-card doc-panel-card">
            <div class="card-header-row">
              <div class="header-icon-title">
                <i class="pi pi-folder-open"></i>
                <h3 class="card-title">Expediente Documental Digital</h3>
              </div>
            </div>

            <p class="panel-desc">
              Adjunte facturas comerciales, BL/guías aéreas o certificados de origen para sustentar el despacho aduanero.
            </p>

            <PortalDocumentosUploadPanel
              mode="live"
              :id-cotizacion="cotizacion.idCotizacion"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { portalCotizacionService } from '../services/portalCotizacionService'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import PortalDocumentosUploadPanel from '../components/PortalDocumentosUploadPanel.vue'
import { esPendienteLiquidacion } from '../utils/portalCotizacionFormUtils'
import {
  formatCurrency,
  formatRegimen,
  formatEstadoPago as formatPago,
  getCotizacionStatusClass as getStatusClass,
  getPagoStatusClass as getPagoClass
} from '@/shared/utils/formatters'

const router = useRouter()
const route = useRoute()
const { showSuccess, showError, showWarn } = useNotification()
const { confirmAction } = useConfirmation()

const cotizacion = ref(null)
const loading = ref(true)
const aceptando = ref(false)

onMounted(() => {
  cargarDetalle()
})

async function cargarDetalle() {
  const id = route.params.id
  if (!id) {
    showError('No se especificó la cotización a consultar.')
    volver()
    return
  }

  loading.value = true
  try {
    const data = await portalCotizacionService.obtenerDetalle(id)
    if (data) {
      cotizacion.value = data
    } else {
      showError('No se encontró información para la cotización seleccionada.')
    }
  } catch (err) {
    console.error('Error al cargar detalle de cotización:', err)
    showError(err.response?.data?.message || 'Error al obtener la información de la cotización.')
  } finally {
    loading.value = false
  }
}

function volver() {
  router.push('/cotizaciones')
}

function irAPagar() {
  if (!cotizacion.value) return
  router.push({
    path: '/pagos/registrar',
    query: { tipo: 'COTIZACION', id: cotizacion.value.idCotizacion }
  })
}

function irASubsanar() {
  if (!cotizacion.value) return
  router.push({
    path: '/pagos',
    query: { tab: 'historial', idCotizacion: cotizacion.value.idCotizacion }
  })
}

function irAHistorialPagos() {
  router.push({
    path: '/pagos',
    query: { tab: 'historial' }
  })
}

async function autorizarCotizacion() {
  if (!cotizacion.value) return
  if (cotizacion.value.aceptadaPorCliente) {
    showWarn('Esta cotización ya fue autorizada formalmente.')
    return
  }

  const confirmed = await confirmAction({
    header: 'Autorizar Cotización Comercial',
    message: `¿Está seguro de autorizar la cotización ${cotizacion.value.codigoCotizacion} por un monto de $ ${formatCurrency(cotizacion.value.totalCotizado)} USD? Al aceptar, se generará la orden de pago pendiente para que pueda registrar su anticipo o pago completo ante Finanzas.`,
    icon: 'pi pi-check-circle',
    acceptLabel: 'Autorizar y Proceder a Pagar',
    rejectLabel: 'Cancelar',
    acceptClass: 'p-button-success'
  })

  if (!confirmed) return

  aceptando.value = true
  try {
    const res = await portalCotizacionService.aceptarCotizacion(cotizacion.value.idCotizacion)
    if (res) {
      showSuccess(`Cotización ${cotizacion.value.codigoCotizacion} autorizada exitosamente. Proceda al registro del anticipo.`, 'Operación Autorizada', 4000)
      await cargarDetalle()
      router.push({
        path: '/pagos/registrar',
        query: { tipo: 'COTIZACION', id: cotizacion.value.idCotizacion }
      })
    }
  } catch (err) {
    showError(err.response?.data?.message || 'Error al autorizar cotización')
  } finally {
    aceptando.value = false
  }
}

function imprimirCotizacion() {
  window.print()
}
</script>

<style scoped>
.cotizacion-detalle-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
  animation: fadeIn 0.25s ease-in-out;
}

/* Barra Superior de Navegación */
.top-nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--color-border);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  flex-wrap: wrap;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.45rem 0.85rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-back:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
}

.crumb-link {
  color: var(--color-text-muted);
  cursor: pointer;
  transition: color 0.15s;
}

.crumb-link:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.crumb-sep {
  color: var(--color-text-subtle);
}

.crumb-current {
  color: var(--color-text-main);
  font-weight: 700;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.btn-action-authorize {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-success-text);
  color: var(--color-bg-surface);
  border: 1px solid var(--color-success-border);
  padding: 0.5rem 1rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action-authorize:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.btn-action-pay {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  padding: 0.5rem 1rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action-pay:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.btn-action-observe {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
  padding: 0.5rem 1rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action-observe:hover {
  background: var(--color-danger-text);
  color: var(--color-bg-surface);
  border-color: var(--color-danger-text);
}

.btn-action-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.85rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-action-outline:hover {
  background: var(--color-bg-subtle);
}

/* Tarjeta Cabecera */
.quote-header-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.5rem 1.75rem;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.quote-header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1.25rem;
  margin-bottom: 1rem;
}

.quote-kicker {
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  display: block;
  margin-bottom: 0.25rem;
}

.quote-code {
  font-size: 1.85rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
  line-height: 1.2;
}

.quote-subtitle {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin: 0.35rem 0 0 0;
}

.quote-badges {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.badge-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.badge-label {
  font-size: 0.72rem;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-weight: 600;
}

.status-pill, .pago-pill {
  display: inline-block;
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill, 9999px);
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.status-pill.success, .pago-pill.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-pill.warning, .pago-pill.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.status-pill.danger, .pago-pill.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.status-pill.info, .pago-pill.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.status-pill.neutral, .pago-pill.neutral {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

/* Alertas a ancho completo */
.alert-box-full {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem 1.25rem;
  border-radius: var(--radius-md, 8px);
}

.alert-box-full.observation {
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  color: var(--color-warning-text);
}

.alert-box-full.payment-obs {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  border-left: 4px solid var(--color-danger-text);
  color: var(--color-danger-text);
}

.alert-icon {
  font-size: 1.5rem;
  margin-top: 0.1rem;
  flex-shrink: 0;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-size: 0.95rem;
  font-weight: 700;
  margin: 0 0 0.25rem 0;
  color: inherit;
}

.alert-desc {
  font-size: 0.8125rem;
  line-height: 1.45;
  color: var(--color-text-main);
  margin: 0 0 0.5rem 0;
}

.alert-footnote {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.btn-subsanar-banner {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-danger-text);
  color: var(--color-bg-surface);
  border: none;
  padding: 0.45rem 0.9rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-subsanar-banner:hover {
  filter: brightness(1.15);
}

/* Grid a 2 Columnas */
.detail-two-col-grid {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: 1.5rem;
  align-items: start;
}

.col-left, .col-right {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Tarjetas de Panel */
.panel-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.15rem;
  border-bottom: 1px solid var(--color-border-subtle);
  padding-bottom: 0.75rem;
}

.header-icon-title {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  color: var(--color-primary);
}

.card-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.header-tag {
  font-size: 0.72rem;
  font-weight: 700;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

.panel-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: -0.5rem 0 1rem 0;
  line-height: 1.4;
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.15rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.info-lbl {
  font-size: 0.72rem;
  text-transform: uppercase;
  color: var(--color-text-muted);
  font-weight: 600;
}

.info-val {
  font-size: 0.875rem;
  color: var(--color-text-main);
  line-height: 1.35;
}

.info-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

/* Tabla de Costos Full */
.cost-table-full {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  overflow: hidden;
}

.cost-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-bg-surface);
}

.cost-row:nth-child(even) {
  background: var(--color-bg-subtle);
}

.cost-concept {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.concept-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.concept-desc {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.cost-val {
  font-size: 0.9rem;
  color: var(--color-text-main);
  text-align: right;
  white-space: nowrap;
}

.cost-row.highlight-tributos {
  background: var(--color-bg-muted);
  border-top: 1px dashed var(--color-border-strong);
}

.cost-row.total-row-full {
  background: var(--color-primary);
  color: var(--color-primary-text);
  padding: 1rem 1.25rem;
  border-bottom: none;
}

.cost-row.total-row-full .concept-name {
  color: var(--color-primary-text);
  font-size: 0.95rem;
}

.cost-row.total-row-full .concept-desc {
  color: var(--color-primary-text);
  opacity: 0.8;
}

.total-amount-big {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--color-primary-text);
}

.card-subtitle-small {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
}

.notes-text {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0;
}

/* KPIs Financieros Laterales */
.finance-kpis {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.75rem;
}

.fin-kpi-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.65rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm, 6px);
}

.fin-kpi-item.highlight-saldo {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
}

.fin-kpi-lbl {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  font-weight: 600;
}

.fin-kpi-val {
  font-size: 1.05rem;
  font-weight: 700;
}

/* Botones Laterales */
.finance-actions-box {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.btn-full-primary {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  padding: 0.65rem 1rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-full-primary:hover {
  filter: brightness(1.1);
}

.btn-full-danger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
  padding: 0.65rem 1rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-full-danger:hover {
  background: var(--color-danger-text);
  color: var(--color-bg-surface);
  border-color: var(--color-danger-text);
}

.btn-full-outline {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  padding: 0.55rem 1rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-full-outline:hover {
  background: var(--color-bg-subtle);
}

/* Estados de Carga y Vacío */
.loading-state-card, .empty-state-card {
  padding: 3rem 1.5rem;
  text-align: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  color: var(--color-text-muted);
}

/* Responsive */
@media (max-width: 992px) {
  .detail-two-col-grid {
    grid-template-columns: 1fr;
  }
}
</style>
