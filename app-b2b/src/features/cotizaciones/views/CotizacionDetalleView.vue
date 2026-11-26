<template>
  <div class="cotizacion-detalle-view fade-in">
    <!-- Cabecera directa sgaUI -->
    <header class="sga-form-header">
      <div class="header-left">
        <Button
          type="button"
          label="Volver"
          icon="pi pi-arrow-left"
          severity="secondary"
          outlined
          size="small"
          @click="volver"
        />
        <div v-if="cotizacion" class="breadcrumb-code">
          <span class="sga-kicker">COTIZACIÓN</span>
          <span class="sga-mono code-pill">{{ cotizacion.codigoCotizacion }}</span>
        </div>
      </div>

      <div v-if="cotizacion" class="sga-form-actions">
        <Button
          v-if="cotizacion.estado === 'APROBADA' && !cotizacion.aceptadaPorCliente"
          type="button"
          label="Solicitar Ajuste / Rechazar"
          icon="pi pi-times-circle"
          severity="danger"
          outlined
          size="small"
          :disabled="aceptando || rechazando"
          @click="abrirRechazoDialog"
        />

        <Button
          v-if="cotizacion.estado === 'APROBADA' && !cotizacion.aceptadaPorCliente"
          type="button"
          label="Autorizar y Pagar"
          icon="pi pi-check-circle"
          severity="success"
          size="small"
          :loading="aceptando"
          @click="autorizarCotizacion"
        />

        <Button
          v-if="cotizacion.estadoPago === 'OBSERVADO' || cotizacion.estadoPago === 'RECHAZADO'"
          type="button"
          label="Subsanar Abono"
          icon="pi pi-pencil"
          severity="danger"
          size="small"
          @click="irASubsanar"
        />

        <Button
          v-else-if="cotizacion.estado === 'APROBADA' && cotizacion.estadoPago !== 'PAGADO_TOTAL'"
          type="button"
          label="Abonar Anticipo"
          icon="pi pi-wallet"
          size="small"
          @click="irAPagar"
        />

        <Button
          type="button"
          label="Ver / Imprimir PDF"
          icon="pi pi-print"
          severity="secondary"
          outlined
          size="small"
          :loading="pdfAction === 'preview'"
          :disabled="!!pdfAction"
          @click="abrirProformaPdf(cotizacion)"
        />

        <Button
          type="button"
          label="Descargar PDF"
          icon="pi pi-file-pdf"
          severity="secondary"
          outlined
          size="small"
          :loading="pdfAction === 'download'"
          :disabled="!!pdfAction"
          @click="descargarProformaPdf(cotizacion)"
        />
      </div>
    </header>

    <!-- Estado de Carga -->
    <div v-if="loading" class="sga-card-surface state-card">
      <i class="pi pi-spin pi-spinner state-icon"></i>
      <p>Cargando detalle de la cotización comercial...</p>
    </div>

    <!-- Error / No encontrada -->
    <div v-else-if="!cotizacion" class="sga-card-surface state-card">
      <i class="pi pi-exclamation-circle state-icon is-danger"></i>
      <h3>No se pudo cargar la cotización</h3>
      <p>Es posible que el código ingresado no exista o no pertenezca a su cuenta empresarial.</p>
      <Button type="button" label="Volver al listado" size="small" @click="volver" />
    </div>

    <!-- Contenido Split-Screen sgaUI -->
    <template v-else>
      <!-- Banners de alerta contextuales -->
      <div v-if="cotizacion.estado === 'OBSERVADA'" class="sga-banner is-warn">
        <i class="pi pi-exclamation-triangle"></i>
        <div class="banner-body">
          <strong>Observación Técnica de Aduanas Emitida</strong>
          <p>
            {{ cotizacion.motivoObservacion || 'Se requieren correcciones técnicas o precisiones documentarias en la propuesta comercial.' }}
          </p>
          <span class="banner-note">Su asesor comercial asignado coordinará la subsanación de estos requisitos.</span>
        </div>
      </div>

      <div v-if="cotizacion.estadoPago === 'OBSERVADO' || cotizacion.estadoPago === 'RECHAZADO'" class="sga-banner is-danger">
        <i class="pi pi-exclamation-circle"></i>
        <div class="banner-body">
          <strong>Abono Bancario Observado por Auditoría Contable</strong>
          <p>
            El área de Finanzas observó el comprobante o los datos de transferencia. Subsane la información para autorizar la apertura del expediente aduanero.
          </p>
        </div>
        <Button
          type="button"
          label="Subsanar Abono"
          icon="pi pi-pencil"
          severity="danger"
          size="small"
          @click="irASubsanar"
        />
      </div>

      <div class="sga-split-layout">
        <!-- Columna Principal -->
        <div class="sga-split-main">
          <!-- Ficha Resumen Superior -->
          <section class="sga-card-surface summary-card">
            <div class="summary-top">
              <div>
                <span class="sga-kicker">PROPUESTA LOGÍSTICA & ADUANERA OFICIAL</span>
                <h2 class="quote-code sga-mono">{{ cotizacion.codigoCotizacion }}</h2>
                <p class="quote-client">
                  Emitida para <strong>{{ cotizacion.razonSocialCliente || 'Cliente B2B' }}</strong>
                  (RUC: <span class="sga-mono">{{ cotizacion.rucCliente || '—' }}</span>)
                </p>
              </div>

              <div class="summary-tags">
                <div class="tag-group">
                  <span class="sga-micro-label">ESTADO TÉCNICO</span>
                  <Tag
                    :value="formatEstadoCotizacion(cotizacion.estado)"
                    :severity="getEstadoSeverity(cotizacion.estado)"
                    rounded
                  />
                </div>
                <div class="tag-group">
                  <span class="sga-micro-label">ESTADO FINANCIERO</span>
                  <Tag
                    :value="formatPago(cotizacion.estadoPago)"
                    :severity="getPagoSeverity(cotizacion.estadoPago)"
                    rounded
                  />
                </div>
              </div>
            </div>
          </section>

          <!-- Tarjeta 1: Mercancía & Operación -->
          <section class="sga-card-surface section-card">
            <div class="card-head">
              <div class="card-title-row">
                <i class="pi pi-box"></i>
                <h3 class="sga-chart-title">Información de la Mercancía & Operación</h3>
              </div>
              <Tag :value="formatRegimen(cotizacion.regimenAduanero)" severity="secondary" class="sga-mini-tag" />
            </div>

            <div class="info-grid">
              <div class="info-item">
                <span class="sga-micro-label">MERCANCÍA</span>
                <strong class="info-val">{{ cotizacion.nombreMercancia || 'Mercancía General' }}</strong>
                <span class="info-sub sga-mono">Cód: {{ cotizacion.codigoMercancia || 'N/A' }}</span>
              </div>

              <div class="info-item">
                <span class="sga-micro-label">TIPO DE CARGA</span>
                <strong class="info-val">{{ cotizacion.nombreTipoCarga || 'Carga General' }}</strong>
              </div>

              <div class="info-item">
                <span class="sga-micro-label">RUTA PORTUARIA / AEROPORTUARIA</span>
                <strong class="info-val">
                  {{ cotizacion.nombrePuertoEnvio || 'Origen' }} → {{ cotizacion.nombrePuertoDestino || 'Destino' }}
                </strong>
              </div>

              <div class="info-item">
                <span class="sga-micro-label">ASESOR COMERCIAL ASIGNADO</span>
                <strong class="info-val">{{ cotizacion.usuarioComercialNombre || 'Pendiente de asignación' }}</strong>
              </div>

              <div class="info-item">
                <span class="sga-micro-label">TRANSPORTE TERRESTRE</span>
                <strong class="info-val">
                  {{ cotizacion.servicioTransporte ? (cotizacion.costoTransporteInterno ? `$ ${formatCurrency(cotizacion.costoTransporteInterno)} (SGA)` : 'Incluido') : 'No incluido' }}
                </strong>
                <span v-if="cotizacion.servicioTransporte" class="info-sub">
                  {{ cotizacion.origenProvincia || cotizacion.origenDepartamento || 'Origen' }} → {{ cotizacion.destinoProvincia || cotizacion.destinoDepartamento || 'Destino' }}
                </span>
              </div>

              <div class="info-item">
                <span class="sga-micro-label">VALIDEZ COMERCIAL</span>
                <strong class="info-val sga-mono">{{ cotizacion.validezDias || 30 }} días</strong>
                <span class="info-sub">Emisión: {{ cotizacion.fechaEmision || 'En evaluación' }}</span>
              </div>
            </div>
          </section>

          <!-- Tarjeta 2: Desglose Oficial de Costos -->
          <section class="sga-card-surface section-card">
            <div class="card-head">
              <div class="card-title-row">
                <i class="pi pi-calculator"></i>
                <h3 class="sga-chart-title">Desglose Oficial de Costos y Tributos (USD)</h3>
              </div>
              <Tag value="Dólares ($ USD)" severity="secondary" class="sga-mini-tag sga-mono" />
            </div>

            <div class="cost-list">
              <div class="cost-row">
                <div class="cost-concept">
                  <strong>Valor Mercancía (FOB estimado)</strong>
                  <span>Base imponible preliminar para determinación de aranceles</span>
                </div>
                <span class="cost-val sga-mono">$ {{ formatCurrency(cotizacion.valorFob) }}</span>
              </div>

              <div class="cost-row">
                <div class="cost-concept">
                  <strong>Flete Internacional Estimado</strong>
                  <span>Flete marítimo / aéreo internacional</span>
                </div>
                <span class="cost-val sga-mono">
                  {{ Number(cotizacion.montoFlete) > 0 ? `$ ${formatCurrency(cotizacion.montoFlete)}` : (esPendienteLiquidacion(cotizacion) ? 'Por asesor' : '$ 0.00') }}
                </span>
              </div>

              <div class="cost-row">
                <div class="cost-concept">
                  <strong>Seguro de Transporte Estimado</strong>
                  <span>Póliza o tabla de seguro aduanero SUNAT</span>
                </div>
                <span class="cost-val sga-mono">
                  {{ esPendienteLiquidacion(cotizacion) && !(Number(cotizacion.montoSeguroEstimado) > 0) ? 'Por asesor' : `$ ${formatCurrency(cotizacion.montoSeguroEstimado)}` }}
                </span>
              </div>

              <div class="cost-row">
                <div class="cost-concept">
                  <strong>Honorarios de Agenciamiento Aduanero</strong>
                  <span>Servicio integral de despacho y gestión técnica TransAduanas</span>
                </div>
                <span class="cost-val sga-mono">
                  {{ esPendienteLiquidacion(cotizacion) && !(Number(cotizacion.montoHonorarios) > 0) ? 'Por asesor' : `$ ${formatCurrency(cotizacion.montoHonorarios)}` }}
                </span>
              </div>

              <div v-if="cotizacion.servicioTransporte" class="cost-row">
                <div class="cost-concept">
                  <strong>Flete Terrestre Local (Flota SGA)</strong>
                  <span>Transporte desde terminal hasta destino nacional</span>
                </div>
                <span class="cost-val sga-mono">
                  {{ esPendienteLiquidacion(cotizacion) && !(Number(cotizacion.costoTransporteInterno) > 0) ? 'Por asesor' : `$ ${formatCurrency(cotizacion.costoTransporteInterno)}` }}
                </span>
              </div>

              <div class="cost-row">
                <div class="cost-concept">
                  <strong>Tributos Aduaneros Estimados (Aranceles + IGV + IPM)</strong>
                  <span>Liquidación preliminar según subpartida nacional SUNAT</span>
                </div>
                <span class="cost-val sga-mono">
                  {{ cotizacion.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Inafecto ($ 0.00)' : (esPendienteLiquidacion(cotizacion) && !(Number(cotizacion.montoTributos) > 0) ? 'Por asesor' : `$ ${formatCurrency(cotizacion.montoTributos)}`) }}
                </span>
              </div>

              <div class="cost-row is-total">
                <div class="cost-concept">
                  <strong>TOTAL ESTIMADO DEL SERVICIO LOGÍSTICO</strong>
                  <span>Sujeto a liquidación final aduanera</span>
                </div>
                <span class="cost-total-val sga-mono">
                  {{ esPendienteLiquidacion(cotizacion) ? 'En liquidación ($ 0.00)' : `$ ${formatCurrency(cotizacion.totalCotizado)}` }}
                </span>
              </div>
            </div>
          </section>

          <!-- Observaciones -->
          <section v-if="cotizacion.observaciones" class="sga-card-surface section-card">
            <div class="card-title-row">
              <i class="pi pi-comment"></i>
              <h3 class="sga-chart-title">Observaciones Comerciales del Servicio</h3>
            </div>
            <p class="notes-text">{{ cotizacion.observaciones }}</p>
          </section>
        </div>

        <!-- Columna Lateral Sticky -->
        <aside class="sga-split-aside aside-stack">
          <section class="sga-card-surface section-card">
            <div class="card-title-row">
              <i class="pi pi-wallet"></i>
              <h3 class="sga-chart-title">Control Financiero & Anticipo</h3>
            </div>

            <div class="finance-list">
              <div class="fin-row">
                <span class="sga-micro-label">TOTAL COTIZADO</span>
                <strong class="sga-mono fin-val">$ {{ formatCurrency(cotizacion.totalCotizado) }}</strong>
              </div>
              <div class="fin-row">
                <span class="sga-micro-label">TOTAL AMORTIZADO</span>
                <strong class="sga-mono fin-val sga-text-success">$ {{ formatCurrency(cotizacion.totalAbonado) }}</strong>
              </div>
              <div class="fin-row is-highlight">
                <span class="sga-micro-label">SALDO PENDIENTE</span>
                <strong class="sga-mono fin-val sga-text-warn">$ {{ formatCurrency(cotizacion.saldoPendiente) }}</strong>
              </div>
            </div>

            <div class="aside-actions">
              <Button
                v-if="cotizacion.estadoPago === 'OBSERVADO' || cotizacion.estadoPago === 'RECHAZADO'"
                type="button"
                label="Subsanar Abono Observado"
                icon="pi pi-pencil"
                severity="danger"
                size="small"
                fluid
                @click="irASubsanar"
              />
              <Button
                v-else-if="cotizacion.estado === 'APROBADA' && cotizacion.estadoPago !== 'PAGADO_TOTAL'"
                type="button"
                label="Registrar Comprobante / Anticipo"
                icon="pi pi-wallet"
                size="small"
                fluid
                @click="irAPagar"
              />
              <Button
                type="button"
                label="Ver Historial de Vouchers"
                icon="pi pi-history"
                severity="secondary"
                outlined
                size="small"
                fluid
                @click="irAHistorialPagos"
              />
            </div>
          </section>

          <section class="sga-card-surface section-card">
            <div class="card-title-row">
              <i class="pi pi-folder-open"></i>
              <h3 class="sga-chart-title">Expediente Documental Digital</h3>
            </div>
            <p class="panel-desc">
              Adjunte facturas comerciales, BL/guías aéreas o certificados de origen para sustentar el despacho.
            </p>
            <PortalDocumentosUploadPanel
              mode="live"
              :id-cotizacion="cotizacion.idCotizacion"
            />
          </section>
        </aside>
      </div>
    </template>

    <Dialog
      v-model:visible="showRechazoDialog"
      modal
      header="Responder Cotización Comercial"
      :style="{ width: '480px', maxWidth: '95vw' }"
    >
      <div class="sga-dialog-body">
        <p class="notes-text">
          Indique si desea solicitar una revisión de costos/condiciones al asesor comercial o rechazar definitivamente la propuesta
          <strong>{{ cotizacion?.codigoCotizacion }}</strong>.
        </p>
        <div class="sga-form-field">
          <label for="rechazo-accion" class="sga-form-label">Acción</label>
          <Select
            id="rechazo-accion"
            v-model="rechazoForm.accion"
            :options="rechazoAccionOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>
        <div class="sga-form-field">
          <label for="rechazo-motivo" class="sga-form-label">Motivo o Detalle *</label>
          <Textarea
            id="rechazo-motivo"
            v-model.trim="rechazoForm.motivo"
            rows="3"
            fluid
            placeholder="Ej. Ajustar flete o modalidad de transporte antes de aprobar..."
          />
        </div>
      </div>
      <template #footer>
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          @click="showRechazoDialog = false"
        />
        <Button
          type="button"
          :label="rechazoForm.accion === 'OBSERVAR' ? 'Solicitar Ajuste' : 'Confirmar Rechazo'"
          :severity="rechazoForm.accion === 'OBSERVAR' ? 'warn' : 'danger'"
          size="small"
          :loading="rechazando"
          :disabled="!rechazoForm.motivo"
          @click="confirmarRechazoCotizacion"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { portalCotizacionService } from '../services/portalCotizacionService'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { useProformaPdf } from '../composables/useProformaPdf'
import PortalDocumentosUploadPanel from '../components/PortalDocumentosUploadPanel.vue'
import { esPendienteLiquidacion } from '../utils/portalCotizacionFormUtils'
import {
  formatCurrency,
  formatRegimen,
  formatEstadoCotizacion,
  formatEstadoPago as formatPago
} from '@/shared/utils/formatters'

const router = useRouter()
const route = useRoute()
const { showSuccess, showError, showWarn } = useNotification()
const { confirmAction } = useConfirmation()
const { pdfAction, abrirProformaPdf, descargarProformaPdf } = useProformaPdf()

const cotizacion = ref(null)
const loading = ref(true)
const aceptando = ref(false)
const rechazando = ref(false)
const showRechazoDialog = ref(false)
const rechazoForm = ref({ accion: 'OBSERVAR', motivo: '' })
const rechazoAccionOptions = [
  { label: 'Solicitar ajuste comercial (revisión)', value: 'OBSERVAR' },
  { label: 'Rechazar propuesta definitivamente', value: 'RECHAZAR' }
]

onMounted(() => {
  cargarDetalle()
})

function getEstadoSeverity(estado) {
  switch (estado) {
    case 'APROBADA':
      return 'success'
    case 'PENDIENTE':
    case 'EN_REVISION':
      return 'warn'
    case 'OBSERVADA':
    case 'RECHAZADA':
      return 'danger'
    default:
      return 'secondary'
  }
}

function getPagoSeverity(estadoPago) {
  switch (estadoPago) {
    case 'PAGADO_TOTAL':
    case 'APROBADO':
    case 'ADELANTO_APROBADO':
      return 'success'
    case 'PENDIENTE_REVISION':
      return 'info'
    case 'OBSERVADO':
    case 'RECHAZADO':
      return 'danger'
    default:
      return 'warn'
  }
}

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

function abrirRechazoDialog() {
  rechazoForm.value = { accion: 'OBSERVAR', motivo: '' }
  showRechazoDialog.value = true
}

async function confirmarRechazoCotizacion() {
  if (!cotizacion.value || !rechazoForm.value.motivo) return
  rechazando.value = true
  try {
    await portalCotizacionService.rechazarCotizacion(cotizacion.value.idCotizacion, {
      accion: rechazoForm.value.accion,
      motivo: rechazoForm.value.motivo
    })
    showRechazoDialog.value = false
    showSuccess(
      rechazoForm.value.accion === 'OBSERVAR'
        ? 'Se envió su solicitud de ajuste al asesor comercial.'
        : 'La cotización fue marcada como rechazada.',
      'Respuesta Registrada'
    )
    await cargarDetalle()
  } catch (err) {
    showError(err.response?.data?.message || 'No se pudo registrar su respuesta sobre la cotización.')
  } finally {
    rechazando.value = false
  }
}
</script>

<style scoped>
.cotizacion-detalle-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
  font-family: var(--font-sans);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  flex-wrap: wrap;
}

.breadcrumb-code {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.code-pill {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.2rem 0.6rem;
  border-radius: var(--radius-pill);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
}

.state-card {
  padding: 3rem 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.65rem;
  color: var(--color-text-muted);
}

.state-icon {
  font-size: 2rem;
  color: var(--color-accent-kicker);
}

.state-icon.is-danger {
  color: var(--color-danger-text);
}

.banner-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.banner-body p {
  margin: 0;
  font-size: 0.82rem;
}

.banner-note {
  font-size: 0.76rem;
  opacity: 0.85;
}

.summary-card,
.section-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
}

.quote-code {
  margin: 0.25rem 0;
  font-size: 1.45rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.quote-client {
  margin: 0;
  font-size: 0.86rem;
  color: var(--color-text-muted);
}

.summary-tags {
  display: flex;
  gap: 1.25rem;
  flex-wrap: wrap;
}

.tag-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.3rem;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  color: var(--color-text-main);
}

.card-title-row i {
  color: var(--color-accent-kicker);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem 1.25rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.info-val {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.info-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.cost-list {
  display: flex;
  flex-direction: column;
}

.cost-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--color-border-subtle);
}

.cost-row:last-child {
  border-bottom: none;
}

.cost-concept {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.cost-concept strong {
  font-size: 0.86rem;
  color: var(--color-text-main);
}

.cost-concept span {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.cost-val {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-main);
  white-space: nowrap;
}

.cost-row.is-total {
  margin-top: 0.35rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
}

.cost-total-val {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-primary);
  white-space: nowrap;
}

.notes-text {
  margin: 0;
  font-size: 0.86rem;
  line-height: 1.5;
  color: var(--color-text-main);
}

.aside-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.finance-list {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.fin-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.fin-row.is-highlight {
  border-color: var(--color-warning-border);
  background: var(--color-warning-bg);
}

.fin-val {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.aside-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.panel-desc {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.45;
  color: var(--color-text-muted);
}

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}

@media print {
  .sga-form-header {
    display: none;
  }
}
</style>
