<template>
  <div class="subsanar-pago-view">
    <!-- Barra Superior de Navegación -->
    <div class="top-nav-bar">
      <div class="nav-left">
        <button type="button" class="btn-back" @click="volver">
          <i class="pi pi-arrow-left"></i>
          <span>Volver a Cobros y Pagos</span>
        </button>
        <div class="breadcrumbs">
          <span class="crumb-link" @click="volver">Cobros y Pagos</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-current font-mono">{{ pago?.codigoPago || 'Subsanar Pago' }}</span>
        </div>
      </div>

      <div class="nav-actions" v-if="pago?.idCotizacion">
        <button type="button" class="btn-action-outline" @click="verCotizacion(pago.idCotizacion)">
          <i class="pi pi-file"></i>
          <span>Ver Cotización ({{ pago.codigoCotizacion }})</span>
        </button>
      </div>
    </div>

    <!-- Estado de Carga -->
    <div v-if="loading" class="loading-state-card">
      <i class="pi pi-spin pi-spinner text-3xl"></i>
      <p>Cargando información del comprobante observado...</p>
    </div>

    <!-- Error al Cargar -->
    <div v-else-if="!pago" class="empty-state-card">
      <i class="pi pi-exclamation-circle text-4xl text-danger mb-2"></i>
      <h3>Comprobante no encontrado</h3>
      <p>No se encontró el registro de pago o no pertenece a su cuenta empresarial.</p>
      <button type="button" class="btn-primary mt-3" @click="volver">Volver a Cobros y Pagos</button>
    </div>

    <!-- Vista Principal de Subsanación -->
    <div v-else class="subsanar-content-wrap">
      <!-- Encabezado de la Subsanación -->
      <div class="payment-header-card">
        <div class="header-main-info">
          <span class="header-kicker">SUBSANACIÓN DE COMPROBANTE BANCARIO // FINANZAS</span>
          <h1 class="payment-title font-mono">{{ pago.codigoPago }}</h1>
          <p class="payment-subtitle">
            Abono vinculado a la Cotización <strong>{{ pago.codigoCotizacion || 'COTIZACIÓN' }}</strong>
            <span class="sub-sep">•</span>
            Tipo: <strong>{{ pago.tipoAbono || 'ANTICIPO' }}</strong>
          </p>
        </div>

        <div class="header-badges">
          <div class="badge-block">
            <span class="badge-label">Estado Auditoría:</span>
            <span class="status-pill danger">
              <i class="pi pi-exclamation-circle"></i>
              {{ formatEstado(pago.estadoPago) }}
            </span>
          </div>
          <div class="badge-block">
            <span class="badge-label">Moneda:</span>
            <span class="currency-tag font-mono">DÓLARES ($ USD)</span>
          </div>
        </div>
      </div>

      <!-- Grid Principal a 2 Columnas -->
      <div class="subsanar-grid">
        <!-- COLUMNA IZQUIERDA: Auditoría de Finanzas y Datos Anteriores -->
        <div class="col-audit">
          <!-- Tarjeta 1: Dictamen de Observación Contable -->
          <div class="audit-card observation-card">
            <div class="card-title-bar warning-bar">
              <i class="pi pi-exclamation-triangle"></i>
              <h3>Observación Emitida por Finanzas</h3>
            </div>
            <div class="observation-body">
              <p class="observation-text">
                {{ pago.observacionFinanzas || 'El comprobante registrado no cumple con los requerimientos contables o los datos bancarios no coinciden con los extractos de la cuenta receptora.' }}
              </p>
              <div class="observation-hint">
                <i class="pi pi-info-circle"></i>
                <span>Por favor verifique el monto abonado, la fecha exacta de la transferencia, el número de operación y adjunte un voucher nítido.</span>
              </div>
            </div>
          </div>

          <!-- Tarjeta 2: Datos Registrados Originalmente -->
          <div class="audit-card">
            <div class="card-title-bar">
              <i class="pi pi-history"></i>
              <h3>Datos Registrados Originalmente</h3>
            </div>
            <div class="prev-data-grid">
              <div class="prev-item">
                <span class="prev-lbl">Monto Abonado</span>
                <strong class="prev-val font-mono highlight">$ {{ formatCurrency(pago.montoAbonado) }} USD</strong>
              </div>
              <div class="prev-item">
                <span class="prev-lbl">N° Constancia / Operación</span>
                <strong class="prev-val font-mono">{{ pago.numeroOperacion || 'Sin número' }}</strong>
              </div>
              <div class="prev-item">
                <span class="prev-lbl">Fecha de Operación</span>
                <span class="prev-val font-mono">{{ pago.fechaOperacion || 'N/A' }}</span>
              </div>
              <div class="prev-item">
                <span class="prev-lbl">Banco Emisor (Origen)</span>
                <span class="prev-val">{{ pago.bancoOrigen || 'N/A' }}</span>
              </div>
              <div class="prev-item full">
                <span class="prev-lbl">Banco Receptor SGA</span>
                <span class="prev-val">{{ pago.bancoDestino || 'BCP Dolares TransAduanas' }}</span>
              </div>
              <div v-if="pago.fechaRegistro" class="prev-item full">
                <span class="prev-lbl">Fecha de Registro en Portal</span>
                <span class="prev-val font-mono font-sm">{{ formatFechaCompleta(pago.fechaRegistro) }}</span>
              </div>
            </div>
          </div>

          <!-- Tarjeta 3: Voucher Anterior Adjunto -->
          <div class="audit-card">
            <div class="card-title-bar">
              <i class="pi pi-file-pdf"></i>
              <h3>Comprobante / Voucher Anterior</h3>
            </div>
            <div class="prev-voucher-body">
              <template v-if="pago.idDocumentoComprobante">
                <div class="voucher-info-box">
                  <div class="voucher-file-icon">
                    <i class="pi pi-file"></i>
                  </div>
                  <div class="voucher-file-meta">
                    <span class="voucher-file-name">{{ pago.documentoComprobanteNombre || 'comprobante_abono.pdf' }}</span>
                    <span class="voucher-file-sub">Voucher registrado previamente</span>
                  </div>
                </div>
                <div class="voucher-actions">
                  <button type="button" class="btn-voucher-action" @click="verComprobante(pago.idDocumentoComprobante)">
                    <i class="pi pi-eye"></i>
                    <span>Ver Voucher Actual</span>
                  </button>
                  <button type="button" class="btn-voucher-action secondary" @click="descargarComprobante(pago.idDocumentoComprobante)">
                    <i class="pi pi-download"></i>
                    <span>Descargar</span>
                  </button>
                </div>
              </template>
              <template v-else>
                <div class="no-voucher-note">
                  <i class="pi pi-info-circle"></i>
                  <span>No se registró voucher digital en el comprobante inicial. Es necesario adjuntar uno nuevo.</span>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- COLUMNA DERECHA: Formulario de Subsanación y Carga de Comprobante -->
        <div class="col-form">
          <div class="form-card">
            <div class="form-header">
              <div class="form-header-title">
                <i class="pi pi-pencil"></i>
                <h2>Formulario Oficial de Subsanación</h2>
              </div>
              <p class="form-header-desc">
                Ingrese las correcciones requeridas por Finanzas. Los cambios serán auditados con prioridad por el equipo contable.
              </p>
            </div>

            <form @submit.prevent="guardarSubsanacion" class="rectification-form">
              <div class="form-fields-grid">
                <!-- Monto Abonado -->
                <div class="form-group">
                  <label for="montoAbonado" class="form-label">
                    Monto Real Abonado ($ USD) <span class="required">*</span>
                  </label>
                  <div class="input-prefix-wrap">
                    <span class="input-prefix font-mono">$</span>
                    <input
                      id="montoAbonado"
                      v-model.number="form.montoAbonado"
                      type="number"
                      step="0.01"
                      min="0.01"
                      required
                      placeholder="0.00"
                      class="form-control font-mono"
                    />
                  </div>
                  <span class="field-hint">Monto exacto reflejado en el comprobante bancario.</span>
                </div>

                <!-- Fecha de Operación -->
                <div class="form-group">
                  <label for="fechaOperacion" class="form-label">
                    Fecha de Operación Bancaria <span class="required">*</span>
                  </label>
                  <input
                    id="fechaOperacion"
                    v-model="form.fechaOperacion"
                    type="date"
                    required
                    class="form-control font-mono"
                  />
                  <span class="field-hint">Fecha en que se ejecutó el abono o transferencia.</span>
                </div>

                <!-- Banco Emisor -->
                <div class="form-group">
                  <label for="bancoOrigen" class="form-label">
                    Banco Emisor (Su Banco) <span class="required">*</span>
                  </label>
                  <input
                    id="bancoOrigen"
                    v-model="form.bancoOrigen"
                    type="text"
                    required
                    placeholder="Ej. BCP, BBVA, Interbank, Scotiabank..."
                    class="form-control"
                  />
                  <span class="field-hint">Entidad financiera desde donde se envió el abono.</span>
                </div>

                <!-- N° de Operación -->
                <div class="form-group">
                  <label for="numeroOperacion" class="form-label">
                    N° de Constancia / Operación <span class="required">*</span>
                  </label>
                  <input
                    id="numeroOperacion"
                    v-model="form.numeroOperacion"
                    type="text"
                    required
                    placeholder="Ej. 0004928172"
                    class="form-control font-mono"
                  />
                  <span class="field-hint">Código de operación único generado por su banco.</span>
                </div>

                <!-- Banco Destino SGA (Informativo / Seleccionable) -->
                <div class="form-group full">
                  <label for="bancoDestino" class="form-label">
                    Cuenta Bancaria Receptora TransAduanas
                  </label>
                  <input
                    id="bancoDestino"
                    v-model="form.bancoDestino"
                    type="text"
                    class="form-control"
                    readonly
                  />
                  <span class="field-hint">Cuenta de destino institucional validada para el depósito.</span>
                </div>

                <!-- Archivo de Voucher / Dropzone -->
                <div class="form-group full">
                  <label class="form-label">
                    Nuevo Archivo de Voucher / Constancia de Pago
                    <span v-if="!pago.idDocumentoComprobante" class="required">*</span>
                    <span v-else class="text-muted">(Opcional si mantiene el comprobante previo)</span>
                  </label>

                  <div 
                    class="voucher-dropzone" 
                    :class="{ 'has-file': nuevoArchivo, 'is-dragging': isDragging }"
                    @dragover.prevent="isDragging = true"
                    @dragleave.prevent="isDragging = false"
                    @drop.prevent="onDropArchivo"
                  >
                    <input
                      type="file"
                      id="nuevo-voucher-input"
                      accept="application/pdf,image/png,image/jpeg,image/webp"
                      @change="onArchivoSeleccionado"
                      class="hidden-file-input"
                    />

                    <template v-if="!nuevoArchivo">
                      <label for="nuevo-voucher-input" class="dropzone-inner-label">
                        <div class="drop-icon">
                          <i class="pi pi-cloud-upload"></i>
                        </div>
                        <div class="drop-text">
                          <strong>Haga clic para seleccionar o arrastre el nuevo archivo de voucher</strong>
                          <span>Formatos admitidos: PDF, JPG, PNG o WEBP (Máximo 10 MB)</span>
                        </div>
                      </label>
                    </template>

                    <template v-else>
                      <div class="file-preview-card">
                        <div class="file-preview-left">
                          <div class="preview-icon">
                            <i class="pi pi-file-check"></i>
                          </div>
                          <div class="preview-meta">
                            <strong class="preview-filename font-mono">{{ nuevoArchivo.name }}</strong>
                            <span class="preview-filesize font-mono">{{ formatFileSize(nuevoArchivo.size) }}</span>
                          </div>
                        </div>
                        <button type="button" class="btn-remove-file" @click="quitarArchivo" title="Quitar archivo">
                          <i class="pi pi-times"></i>
                        </button>
                      </div>
                    </template>
                  </div>
                </div>

                <!-- Sustento / Aclaración de Subsanación -->
                <div class="form-group full">
                  <label for="sustento" class="form-label">
                    Sustento & Aclaración para Finanzas <span class="required">*</span>
                  </label>
                  <textarea
                    id="sustento"
                    v-model="form.observaciones"
                    rows="3"
                    required
                    placeholder="Explique detalladamente las correcciones realizadas respecto a la observación contable..."
                    class="form-control form-textarea"
                  ></textarea>
                  <span class="field-hint">Esta nota será revisada directamente por el analista de Finanzas asignado.</span>
                </div>
              </div>

              <!-- Barra de Acciones del Formulario -->
              <div class="form-actions-bar">
                <button
                  type="button"
                  class="btn-secondary"
                  :disabled="enviando"
                  @click="volver"
                >
                  Cancelar
                </button>

                <button
                  type="submit"
                  class="btn-primary-action"
                  :disabled="enviando"
                >
                  <i v-if="enviando" class="pi pi-spin pi-spinner"></i>
                  <i v-else class="pi pi-send"></i>
                  <span>{{ enviando ? 'Enviando a Revisión Contable...' : 'Guardar Corrección y Reenviar a Revisión' }}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { portalPagoService } from '../services/portalPagoService'
import { useNotification } from '@/shared/composables/useNotification'
import { openBlobInNewTab, downloadBlob } from '@/shared/utils/download'
import {
  formatCurrency,
  formatEstadoPago as formatEstado,
  formatDateTime as formatFechaCompleta,
  formatBytes as formatFileSize,
  validarArchivoVoucher
} from '@/shared/utils/formatters'

const route = useRoute()
const router = useRouter()
const { showSuccess, showError, showWarn } = useNotification()

const loading = ref(true)
const enviando = ref(false)
const pago = ref(null)
const isDragging = ref(false)
const nuevoArchivo = ref(null)

const form = ref({
  montoAbonado: null,
  fechaOperacion: '',
  bancoOrigen: '',
  bancoDestino: 'BCP Dolares TransAduanas',
  numeroOperacion: '',
  observaciones: ''
})

onMounted(async () => {
  await cargarPago()
})

async function cargarPago() {
  const idPago = route.params.id
  if (!idPago) {
    loading.value = false
    return
  }

  loading.value = true
  try {
    const data = await portalPagoService.obtenerPagoPorId(idPago)
    if (data) {
      pago.value = data
      form.value = {
        montoAbonado: Number(data.montoAbonado) || null,
        fechaOperacion: data.fechaOperacion || new Date().toISOString().substring(0, 10),
        bancoOrigen: data.bancoOrigen || '',
        bancoDestino: data.bancoDestino || 'BCP Dolares TransAduanas',
        numeroOperacion: data.numeroOperacion || '',
        observaciones: ''
      }
    } else {
      showError('No se encontró el registro de pago especificado')
    }
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar información del pago')
  } finally {
    loading.value = false
  }
}

function onArchivoSeleccionado(e) {
  const file = e.target.files?.[0]
  if (file && validarArchivoVoucher(file, showWarn)) {
    nuevoArchivo.value = file
  } else if (file) {
    e.target.value = ''
  }
}

function onDropArchivo(e) {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file && validarArchivoVoucher(file, showWarn)) {
    nuevoArchivo.value = file
  }
}

function quitarArchivo() {
  nuevoArchivo.value = null
  const input = document.getElementById('nuevo-voucher-input')
  if (input) input.value = ''
}

async function verComprobante(idDocumento) {
  if (!idDocumento) return
  try {
    const blob = await portalPagoService.previsualizarComprobante(idDocumento)
    openBlobInNewTab(blob, 'application/pdf')
  } catch (err) {
    showError('No se pudo visualizar el comprobante de pago')
  }
}

async function descargarComprobante(idDocumento) {
  if (!idDocumento) return
  try {
    const blob = await portalPagoService.descargarComprobante(idDocumento)
    downloadBlob(blob, pago.value?.documentoComprobanteNombre || `voucher_${pago.value?.codigoPago || 'abono'}.pdf`)
  } catch (err) {
    showError('No se pudo descargar el comprobante de pago')
  }
}

async function guardarSubsanacion() {
  if (!pago.value) return

  // Validaciones
  if (!form.value.montoAbonado || form.value.montoAbonado <= 0) {
    showWarn('Debe ingresar un monto abonado válido mayor a 0.')
    return
  }
  if (!form.value.numeroOperacion?.trim()) {
    showWarn('Debe registrar el número de operación bancaria.')
    return
  }
  if (!form.value.bancoOrigen?.trim()) {
    showWarn('Debe indicar el banco emisor.')
    return
  }
  if (!form.value.observaciones?.trim()) {
    showWarn('Debe incluir un sustento o aclaración de la corrección para Finanzas.')
    return
  }
  if (!pago.value.idDocumentoComprobante && !nuevoArchivo.value) {
    showWarn('Debe adjuntar un archivo de voucher bancario para subsanar el registro.')
    return
  }

  enviando.value = true
  try {
    let idDoc = pago.value.idDocumentoComprobante

    // Si seleccionó nuevo archivo, subirlo primero
    if (nuevoArchivo.value) {
      const docRes = await portalPagoService.subirComprobante(nuevoArchivo.value)
      idDoc = docRes?.idDocumento || docRes?.id
    }

    const payload = {
      montoAbonado: Number(form.value.montoAbonado),
      moneda: 'USD',
      metodoPago: pago.value.metodoPago || 'TRANSFERENCIA_BANCARIA',
      bancoOrigen: form.value.bancoOrigen.trim(),
      bancoDestino: form.value.bancoDestino || 'BCP Dolares TransAduanas',
      numeroOperacion: form.value.numeroOperacion.trim(),
      fechaOperacion: form.value.fechaOperacion,
      observaciones: form.value.observaciones.trim(),
      idDocumentoComprobante: idDoc ? Number(idDoc) : null
    }

    await portalPagoService.corregirPago(pago.value.idPago, payload)
    showSuccess(
      `Comprobante ${pago.value.codigoPago} subsanado con éxito y devuelto a auditoría contable.`,
      'Subsanación Registrada'
    )

    // Redirigir de regreso al historial de cobros y pagos
    router.push({
      path: '/pagos',
      query: { tab: 'historial' }
    })
  } catch (err) {
    showError(err.response?.data?.message || 'Error al guardar la subsanación del comprobante')
  } finally {
    enviando.value = false
  }
}

function volver() {
  router.push('/pagos')
}

function verCotizacion(idCotizacion) {
  router.push(`/cotizaciones/${idCotizacion}`)
}
</script>

<style scoped>
.subsanar-pago-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
  max-width: 1360px;
  margin: 0 auto;
}

/* Barra superior */
.top-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--color-border);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-back:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-hover);
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
}

.crumb-link:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.crumb-sep {
  color: var(--color-border);
}

.crumb-current {
  color: var(--color-text-main);
  font-weight: 700;
}

.btn-action-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-action-outline:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* Estados de carga y vacío */
.loading-state-card,
.empty-state-card {
  padding: 4rem 2rem;
  text-align: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-text-muted);
}

.empty-state-card h3 {
  color: var(--color-text-main);
  margin: 0.5rem 0;
  font-size: 1.125rem;
}

/* Encabezado */
.payment-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.5rem 1.75rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.header-kicker {
  display: block;
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-warning-text);
  margin-bottom: 0.25rem;
}

.payment-title {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-text-main);
  letter-spacing: -0.02em;
}

.payment-subtitle {
  margin: 0.375rem 0 0 0;
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.sub-sep {
  margin: 0 0.5rem;
  color: var(--color-border);
}

.header-badges {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.badge-block {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.badge-label {
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.status-pill.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.currency-tag {
  display: inline-block;
  padding: 0.375rem 0.75rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
}

/* Grid Principal */
.subsanar-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 1024px) {
  .subsanar-grid {
    grid-template-columns: 1fr;
  }
}

/* Columna Izquierda (Auditoría) */
.col-audit {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.audit-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.card-title-bar {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.875rem 1.25rem;
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border);
}

.card-title-bar i {
  color: var(--color-primary);
  font-size: 0.9375rem;
}

.card-title-bar h3 {
  margin: 0;
  font-size: 0.8125rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-text-main);
}

.card-title-bar.warning-bar {
  background: var(--color-warning-bg);
  border-bottom: 1px solid var(--color-warning-border);
}

.card-title-bar.warning-bar i,
.card-title-bar.warning-bar h3 {
  color: var(--color-warning-text);
}

.observation-body {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.observation-text {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.55;
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
  padding: 0.875rem 1rem;
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-warning);
}

.observation-hint {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.75rem;
  line-height: 1.45;
  color: var(--color-text-muted);
}

.observation-hint i {
  color: var(--color-warning-text);
  margin-top: 2px;
}

/* Grilla de datos anteriores */
.prev-data-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  padding: 1.25rem;
}

.prev-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.prev-item.full {
  grid-column: span 2;
}

.prev-lbl {
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.prev-val {
  font-size: 0.875rem;
  color: var(--color-text-main);
}

.prev-val.highlight {
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--color-primary);
}

/* Voucher anterior */
.prev-voucher-body {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.voucher-info-box {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.75rem 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.voucher-file-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1.125rem;
}

.voucher-file-meta {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  overflow: hidden;
}

.voucher-file-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.voucher-file-sub {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.voucher-actions {
  display: flex;
  gap: 0.75rem;
}

.btn-voucher-action {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-voucher-action:hover {
  opacity: 0.9;
}

.btn-voucher-action.secondary {
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border-color: var(--color-border);
}

.btn-voucher-action.secondary:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-hover);
}

.no-voucher-note {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  padding: 0.75rem 1rem;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md);
}

/* Columna Derecha: Formulario */
.col-form {
  display: flex;
  flex-direction: column;
}

.form-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1.75rem 2rem;
}

.form-header {
  margin-bottom: 1.5rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--color-border);
}

.form-header-title {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.form-header-title i {
  color: var(--color-primary);
  font-size: 1.125rem;
}

.form-header-title h2 {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.form-header-desc {
  margin: 0.375rem 0 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.form-fields-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-group.full {
  grid-column: span 2;
}

.form-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.required {
  color: var(--color-danger);
}

.field-hint {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.form-control {
  width: 100%;
  padding: 0.625rem 0.875rem;
  font-size: 0.875rem;
  color: var(--color-text-main);
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  outline: none;
  transition: all 0.15s ease;
}

.form-control:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(194, 91, 59, 0.12);
}

.form-control:read-only {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  cursor: not-allowed;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.input-prefix-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.input-prefix {
  position: absolute;
  left: 0.875rem;
  font-weight: 700;
  color: var(--color-text-muted);
  pointer-events: none;
}

.input-prefix-wrap .form-control {
  padding-left: 1.75rem;
}

/* Dropzone */
.voucher-dropzone {
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  background: var(--color-bg-subtle);
  transition: all 0.2s ease;
  position: relative;
}

.voucher-dropzone:hover,
.voucher-dropzone.is-dragging {
  border-color: var(--color-primary);
  background: rgba(194, 91, 59, 0.04);
}

.voucher-dropzone.has-file {
  border-style: solid;
  border-color: var(--color-border);
  background: var(--color-bg-surface);
}

.hidden-file-input {
  display: none;
}

.dropzone-inner-label {
  display: flex;
  align-items: center;
  gap: 1rem;
  cursor: pointer;
}

.drop-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1.375rem;
}

.drop-text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.drop-text strong {
  font-size: 0.8125rem;
  color: var(--color-text-main);
}

.drop-text span {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.file-preview-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.file-preview-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  overflow: hidden;
}

.preview-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-success-bg);
  color: var(--color-success-text);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
}

.preview-meta {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  overflow: hidden;
}

.preview-filename {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-filesize {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.btn-remove-file {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-remove-file:hover {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

/* Acciones */
.form-actions-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.75rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--color-border);
}

.btn-secondary {
  padding: 0.625rem 1.25rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-hover);
}

.btn-primary-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.5rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: #fff;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-primary-action:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-primary-action:disabled,
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
