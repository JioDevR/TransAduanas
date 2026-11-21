<template>
  <div class="subsanar-pago-view fade-in">
    <!-- Cabecera Directa sgaUI -->
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
        <div v-if="pago" class="header-doc-meta">
          <Tag :value="formatEstado(pago.estadoPago)" severity="danger" rounded />
          <span class="sga-mono doc-pill">{{ pago.codigoPago }}</span>
        </div>
      </div>

      <div v-if="pago" class="sga-form-actions">
        <Button
          v-if="pago.idCotizacion"
          type="button"
          label="Ver Cotización"
          icon="pi pi-file"
          severity="secondary"
          outlined
          size="small"
          @click="verCotizacion(pago.idCotizacion)"
        />
        <Button
          type="button"
          label="Guardar Corrección"
          icon="pi pi-send"
          size="small"
          :loading="enviando"
          @click="guardarSubsanacion"
        />
      </div>
    </header>

    <!-- Cargando -->
    <div v-if="loading" class="sga-card-surface state-card">
      <i class="pi pi-spin pi-spinner state-icon"></i>
      <span>Cargando información del comprobante observado...</span>
    </div>

    <!-- Error / No encontrado -->
    <div v-else-if="!pago" class="sga-card-surface state-card">
      <i class="pi pi-exclamation-circle state-icon is-danger"></i>
      <h3>Comprobante no encontrado</h3>
      <p>No se encontró el registro de pago o no pertenece a su cuenta empresarial.</p>
      <Button type="button" label="Volver a Cobros y Pagos" size="small" @click="volver" />
    </div>

    <!-- Layout Split-Screen sga-forms.css -->
    <div v-else class="sga-split-layout">
      <form class="sga-split-main" @submit.prevent="guardarSubsanacion">
        <section class="sga-card-surface form-card">
          <div class="block-head">
            <span class="sga-kicker">RECTIFICACIÓN DE ABONO BANCARIO // USD</span>
            <h3 class="sga-chart-title">Formulario Oficial de Subsanación</h3>
          </div>

          <div class="two-col-grid">
            <div class="sga-form-field">
              <label for="montoAbonado" class="sga-form-label">Monto Real Abonado ($ USD) *</label>
              <InputNumber
                id="montoAbonado"
                v-model="form.montoAbonado"
                mode="currency"
                currency="USD"
                locale="en-US"
                :min="0.01"
                :minFractionDigits="2"
                :maxFractionDigits="2"
                size="small"
                fluid
                :disabled="enviando"
              />
            </div>

            <div class="sga-form-field">
              <label for="numeroOperacion" class="sga-form-label">N° Operación / Constancia *</label>
              <InputText
                id="numeroOperacion"
                v-model.trim="form.numeroOperacion"
                placeholder="Ej. OP-4928172"
                size="small"
                fluid
                :disabled="enviando"
              />
            </div>

            <div class="sga-form-field">
              <label for="fechaOperacion" class="sga-form-label">Fecha de Operación *</label>
              <InputText
                id="fechaOperacion"
                v-model="form.fechaOperacion"
                type="date"
                size="small"
                fluid
                :disabled="enviando"
              />
            </div>

            <div class="sga-form-field">
              <label for="bancoOrigen" class="sga-form-label">Banco Emisor (Su Banco) *</label>
              <InputText
                id="bancoOrigen"
                v-model.trim="form.bancoOrigen"
                placeholder="Ej. BCP, BBVA, Interbank..."
                size="small"
                fluid
                :disabled="enviando"
              />
            </div>
          </div>

          <PagoVoucherUploadField
            v-model="nuevoArchivo"
            input-id="nuevo-voucher-input"
            label="Nuevo Voucher Bancario (Opcional si solo rectifica datos)"
            prompt-title="Subir nuevo comprobante corregido o arrastrar archivo aquí"
          />

          <div class="sga-form-field">
            <label for="sustento" class="sga-form-label">Sustento & Aclaración para Finanzas *</label>
            <Textarea
              id="sustento"
              v-model="form.observaciones"
              rows="3"
              placeholder="Explique detalladamente las correcciones realizadas respecto a la observación contable..."
              size="small"
              fluid
              :disabled="enviando"
            />
          </div>

          <div class="form-footer">
            <Button
              type="button"
              label="Cancelar"
              severity="secondary"
              outlined
              size="small"
              :disabled="enviando"
              @click="volver"
            />
            <Button
              type="submit"
              label="Guardar Corrección y Reenviar"
              icon="pi pi-send"
              size="small"
              :loading="enviando"
            />
          </div>
        </section>
      </form>

      <!-- Columna Lateral Sticky: Dictamen Contable + Datos Originales -->
      <aside class="sga-split-aside aside-stack">
        <div class="sga-banner is-danger">
          <i class="pi pi-exclamation-triangle"></i>
          <div class="dictamen-body">
            <strong>Observación Emitida por Finanzas</strong>
            <p>
              {{ pago.observacionFinanzas || 'El comprobante registrado no cumple con los requerimientos contables o los datos no coinciden con extractos bancarios.' }}
            </p>
          </div>
        </div>

        <section class="sga-card-surface aside-card">
          <span class="sga-kicker">DATOS REGISTRADOS ORIGINALMENTE</span>
          <div class="prev-rows">
            <div class="prev-row">
              <span>Monto Abonado</span>
              <strong class="sga-mono">$ {{ formatCurrency(pago.montoAbonado) }} USD</strong>
            </div>
            <div class="prev-row">
              <span>N° Operación</span>
              <strong class="sga-mono">{{ pago.numeroOperacion || 'Sin número' }}</strong>
            </div>
            <div class="prev-row">
              <span>Fecha</span>
              <strong class="sga-mono">{{ pago.fechaOperacion || 'N/A' }}</strong>
            </div>
            <div class="prev-row">
              <span>Banco Emisor</span>
              <strong>{{ pago.bancoOrigen || 'N/A' }}</strong>
            </div>
            <div class="prev-row">
              <span>Banco Receptor SGA</span>
              <strong>{{ pago.bancoDestino || 'BCP Dolares TransAduanas' }}</strong>
            </div>
            <div v-if="pago.fechaRegistro" class="prev-row">
              <span>Registrado</span>
              <strong class="sga-mono">{{ formatFechaCompleta(pago.fechaRegistro) }}</strong>
            </div>
          </div>
        </section>

        <section class="sga-card-surface aside-card">
          <span class="sga-kicker">COMPROBANTE ANTERIOR</span>
          <template v-if="pago.idDocumentoComprobante">
            <span class="sga-mono prev-filename">{{ pago.documentoComprobanteNombre || 'comprobante_abono.pdf' }}</span>
            <div class="voucher-btns">
              <Button
                type="button"
                label="Ver Voucher"
                icon="pi pi-eye"
                severity="secondary"
                outlined
                size="small"
                @click="verComprobante(pago.idDocumentoComprobante)"
              />
              <Button
                type="button"
                label="Descargar"
                icon="pi pi-download"
                severity="secondary"
                outlined
                size="small"
                @click="descargarComprobante(pago.idDocumentoComprobante)"
              />
            </div>
          </template>
          <p v-else class="no-voucher-msg">
            No se registró voucher digital inicial. Debe adjuntar uno nuevo.
          </p>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import PagoVoucherUploadField from '../components/PagoVoucherUploadField.vue'
import { portalPagoService } from '../services/portalPagoService'
import { useNotification } from '@/shared/composables/useNotification'
import { openBlobInNewTab, downloadBlob } from '@/shared/utils/download'
import {
  formatCurrency,
  formatEstadoPago as formatEstado,
  formatDateTime as formatFechaCompleta
} from '@/shared/utils/formatters'
import { validarSubsanacionPagoForm } from '../utils/pagoFormUtils'

const route = useRoute()
const router = useRouter()
const { showSuccess, showError, showWarn } = useNotification()

const loading = ref(true)
const enviando = ref(false)
const pago = ref(null)
const nuevoArchivo = ref(null)

const form = ref({
  montoAbonado: null,
  fechaOperacion: '',
  bancoOrigen: '',
  bancoDestino: 'BCP Dolares TransAduanas',
  numeroOperacion: '',
  observaciones: ''
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

async function verComprobante(idDocumento) {
  if (!idDocumento) return
  try {
    const blob = await portalPagoService.previsualizarComprobante(idDocumento)
    openBlobInNewTab(blob, 'application/pdf')
  } catch {
    showError('No se pudo visualizar el comprobante de pago')
  }
}

async function descargarComprobante(idDocumento) {
  if (!idDocumento) return
  try {
    const blob = await portalPagoService.descargarComprobante(idDocumento)
    downloadBlob(blob, pago.value?.documentoComprobanteNombre || `voucher_${pago.value?.codigoPago || 'abono'}.pdf`)
  } catch {
    showError('No se pudo descargar el comprobante de pago')
  }
}

async function guardarSubsanacion() {
  if (!pago.value) return

  const check = validarSubsanacionPagoForm({
    montoAbonado: form.value.montoAbonado,
    numeroOperacion: form.value.numeroOperacion,
    bancoOrigen: form.value.bancoOrigen,
    observaciones: form.value.observaciones,
    tieneComprobantePrevio: Boolean(pago.value.idDocumentoComprobante),
    nuevoArchivo: nuevoArchivo.value
  })

  if (!check.valid) {
    showWarn(check.error)
    return
  }

  enviando.value = true
  try {
    let idDoc = pago.value.idDocumentoComprobante

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
    router.push('/pagos?tab=historial')
  } catch (err) {
    showError(err.response?.data?.message || 'Error al guardar la subsanación del pago')
  } finally {
    enviando.value = false
  }
}

function volver() {
  router.push('/pagos')
}

function verCotizacion(idCotizacion) {
  if (idCotizacion) {
    router.push(`/cotizaciones/${idCotizacion}`)
  }
}

onMounted(cargarPago)
</script>

<style scoped>
.subsanar-pago-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
  font-family: var(--font-sans);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.header-doc-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.doc-pill {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.18rem 0.55rem;
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
}

.state-icon.is-danger {
  color: var(--color-danger-text);
}

.form-card,
.aside-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.block-head {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.two-col-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}

.aside-stack {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.dictamen-body {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.dictamen-body p {
  margin: 0;
  font-size: 0.82rem;
}

.prev-rows {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.prev-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.prev-row strong {
  color: var(--color-text-main);
  text-align: right;
}

.prev-filename {
  font-size: 0.8rem;
  color: var(--color-text-main);
  word-break: break-all;
}

.voucher-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.no-voucher-msg {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

@media (max-width: 720px) {
  .two-col-grid {
    grid-template-columns: 1fr;
  }
}
</style>
