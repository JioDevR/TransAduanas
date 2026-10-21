<template>
  <BaseModal
    :visible="visible"
    title="Registro de Pago / Anticipo de Cotización"
    :subtitle="modalSubtitle"
    icon="pi pi-wallet"
    max-width="720px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <!-- Selector de Cotización si no se pasó una por prop (Ej. llamado desde Finanzas) -->
      <div v-if="!cotizacion" class="field-item full-width cotizacion-picker-box">
        <label for="cotizacion-select" class="picker-label">
          <i class="pi pi-list"></i>
          <span>Seleccionar Cotización Aprobada por Cobrar *</span>
        </label>
        <select
          id="cotizacion-select"
          v-model="selectedCotizacionId"
          class="select-input picker-select"
          @change="onCotizacionSelected"
          required
        >
          <option :value="null">-- Seleccione una Cotización Aprobada pendiente de pago --</option>
          <option
            v-for="c in cotizacionesDisponibles"
            :key="c.idCotizacion"
            :value="c.idCotizacion"
          >
            {{ c.codigoCotizacion }} — {{ c.clienteRazonSocial || 'Cliente' }} (Total: ${{ Number(c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }} | Saldo: ${{ Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }})
          </option>
        </select>
        <span v-if="loadingCotizaciones" class="picker-helper">
          <i class="pi pi-spin pi-spinner"></i> Cargando cotizaciones aprobadas...
        </span>
        <span v-else-if="cotizacionesDisponibles.length === 0" class="picker-helper helper-warn">
          No hay cotizaciones aprobadas con saldo pendiente de pago.
        </span>
      </div>

      <!-- 1. Resumen de Cotización Activa -->
      <div v-if="activeCotizacion" class="summary-pill-card">
        <div class="summary-col">
          <span class="summary-label">COTIZACIÓN / CLIENTE</span>
          <strong class="summary-val">{{ activeCotizacion.codigoCotizacion }} — {{ activeCotizacion.clienteRazonSocial || 'Cliente' }}</strong>
          <span class="summary-sub">RUC: {{ activeCotizacion.clienteNumeroDocumento }}</span>
        </div>
        <div class="summary-col">
          <span class="summary-label">TOTAL COTIZADO</span>
          <strong class="summary-val text-accent">${{ Number(activeCotizacion.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</strong>
        </div>
        <div class="summary-col">
          <span class="summary-label">SALDO PENDIENTE</span>
          <strong class="summary-val text-pending">${{ Number(activeCotizacion.saldoPendiente != null ? activeCotizacion.saldoPendiente : activeCotizacion.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</strong>
        </div>
      </div>

      <!-- 2. Aviso Informativo de Flujo Financiero -->
      <div class="info-alert-box">
        <i class="pi pi-shield info-icon"></i>
        <div class="info-content">
          <strong>Validación Previa Requerida por Finanzas</strong>
          <p>El abono ingresado pasará a la bandeja de Finanzas. Una vez que el analista contable corrobore los fondos en cuenta bancaria y apruebe el comprobante, el sistema <strong>desbloqueará automáticamente la creación del Expediente Aduanero</strong>.</p>
        </div>
      </div>

      <div class="form-grid">
        <!-- Tipo de Abono -->
        <div class="field-item full-width">
          <label for="pago-tipo-abono">Modalidad de Abono *</label>
          <div class="type-abono-selector">
            <button
              type="button"
              class="type-pill"
              :class="{ active: form.tipoAbono === 'ADELANTO' }"
              @click="setTipoAbono('ADELANTO')"
            >
              <i class="pi pi-percentage"></i>
              <span>Adelanto / Anticipo Inicial</span>
            </button>
            <button
              type="button"
              class="type-pill"
              :class="{ active: form.tipoAbono === 'PAGO_TOTAL' }"
              @click="setTipoAbono('PAGO_TOTAL')"
            >
              <i class="pi pi-check-circle"></i>
              <span>Pago Total (100%)</span>
            </button>
            <button
              type="button"
              class="type-pill"
              :class="{ active: form.tipoAbono === 'GARANTIA_CREDITO' }"
              @click="setTipoAbono('GARANTIA_CREDITO')"
            >
              <i class="pi pi-briefcase"></i>
              <span>Línea de Crédito / Fianza</span>
            </button>
          </div>
        </div>

        <!-- Botones de Porcentaje Rápido -->
        <div v-if="form.tipoAbono === 'ADELANTO'" class="field-item full-width quick-percent-wrap">
          <span class="quick-label">Calcular sugerido:</span>
          <button type="button" class="btn-quick-pct" @click="aplicarPorcentaje(0.30)">30% ($ {{ calcularMontoSugerido(0.30) }})</button>
          <button type="button" class="btn-quick-pct" @click="aplicarPorcentaje(0.50)">50% ($ {{ calcularMontoSugerido(0.50) }})</button>
          <button type="button" class="btn-quick-pct" @click="aplicarPorcentaje(1.00)">100% Total</button>
        </div>

        <!-- Moneda y Monto Abonado -->
        <div class="field-item">
          <label for="pago-moneda">Moneda de Pago *</label>
          <select id="pago-moneda" v-model="form.moneda" class="select-input" required>
            <option value="USD">Dólares Americanos (USD $)</option>
            <option value="PEN">Soles Peruanos (PEN S/)</option>
          </select>
        </div>

        <div class="field-item">
          <label for="pago-monto">Monto Abonado *</label>
          <div class="input-with-prefix">
            <span class="currency-prefix">{{ form.moneda === 'PEN' ? 'S/' : '$' }}</span>
            <input
              id="pago-monto"
              v-model.number="form.montoAbonado"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="0.00"
              class="text-input"
              required
            />
          </div>
        </div>

        <!-- Tipo de Cambio y Equivalente -->
        <div class="field-item">
          <label for="pago-tc">Tipo de Cambio Oficial (S/)</label>
          <input
            id="pago-tc"
            v-model.number="form.tipoCambio"
            type="number"
            step="0.0001"
            min="1.0"
            class="text-input"
          />
        </div>

        <div class="field-item">
          <label>Monto Equivalente en Soles</label>
          <div class="equivalent-display">
            <strong>S/ {{ montoEquivalentePenDisplay }}</strong>
            <span class="tc-hint">Convertido a T.C. {{ form.tipoCambio || 3.75 }}</span>
          </div>
        </div>

        <!-- Método de Pago y Banco Destino -->
        <div class="field-item">
          <label for="pago-metodo">Método de Pago *</label>
          <select id="pago-metodo" v-model="form.metodoPago" class="select-input" required>
            <option value="TRANSFERENCIA_BANCARIA">Transferencia Bancaria Directa</option>
            <option value="DEPOSITO_VENTANILLA">Depósito en Ventanilla / Agente</option>
            <option value="TRANSFERENCIA_CCI">Transferencia Interbancaria (CCI)</option>
            <option value="CHEQUE_GERENCIA">Cheque de Gerencia</option>
            <option value="PAGO_LINK">Pago con Tarjeta / Link Digital</option>
            <option value="CARTA_FIANZA">Carta Fianza Bancaria</option>
          </select>
        </div>

        <div class="field-item">
          <label for="pago-banco-destino">Cuenta Bancaria Receptora (SGA) *</label>
          <select id="pago-banco-destino" v-model="form.bancoDestino" class="select-input" required>
            <option value="BCP Dólares Cta Cte 191-0023412-1-44">BCP Cta Cte USD (191-0023412-1-44)</option>
            <option value="BCP Soles Cta Cte 191-0023412-0-43">BCP Cta Cte Soles (191-0023412-0-43)</option>
            <option value="BBVA Dólares Cta Cte 0011-0182-0100034567">BBVA Cta Cte USD (0011-0182-0100034567)</option>
            <option value="BBVA Soles Cta Cte 0011-0182-0100034568">BBVA Cta Cte Soles (0011-0182-0100034568)</option>
            <option value="Interbank Dólares Cta Cte 200-3001245678">Interbank Cta Cte USD (200-3001245678)</option>
            <option value="Scotiabank Soles Cta Cte 000-4567890">Scotiabank Soles (000-4567890)</option>
          </select>
        </div>

        <!-- Banco Origen y N° de Operación -->
        <div class="field-item">
          <label for="pago-banco-origen">Banco Emisor del Cliente</label>
          <input
            id="pago-banco-origen"
            v-model.trim="form.bancoOrigen"
            type="text"
            placeholder="Ej. BCP, BBVA, Interbank, Banco del Exterior..."
            class="text-input"
            maxlength="80"
          />
        </div>

        <div class="field-item">
          <label for="pago-operacion">N° de Operación Bancaria *</label>
          <input
            id="pago-operacion"
            v-model.trim="form.numeroOperacion"
            type="text"
            placeholder="Ej. OP-4829104 / Ref 99283"
            class="text-input"
            required
            maxlength="60"
          />
        </div>

        <!-- Fecha de Operación -->
        <div class="field-item">
          <label for="pago-fecha">Fecha del Depósito / Transferencia *</label>
          <input
            id="pago-fecha"
            v-model="form.fechaOperacion"
            type="date"
            class="text-input"
            required
          />
        </div>

        <!-- Subida de Voucher / Comprobante -->
        <div class="field-item full-width voucher-upload-box">
          <label class="voucher-label">
            <i class="pi pi-paperclip"></i>
            <span>Adjuntar Comprobante Bancario (Voucher / Boleta)</span>
          </label>
          <div class="file-dropzone" @click="triggerFileInput">
            <input
              ref="fileInputRef"
              type="file"
              accept=".pdf,image/png,image/jpeg,image/jpg"
              class="hidden-file-input"
              @change="onFileSelected"
            />
            <div v-if="!selectedFile" class="dropzone-placeholder">
              <i class="pi pi-cloud-upload upload-icon"></i>
              <span class="upload-title">Haga clic para subir el voucher o comprobante</span>
              <span class="upload-sub">Formatos permitidos: PDF, PNG, JPG (Máx. 10MB)</span>
            </div>
            <div v-else class="file-selected-badge">
              <i class="pi pi-file-pdf file-icon"></i>
              <div class="file-meta">
                <span class="file-name">{{ selectedFile.name }}</span>
                <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
              </div>
              <button type="button" class="btn-remove-file" @click.stop="clearSelectedFile" title="Quitar comprobante">
                <i class="pi pi-times"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-actions">
        <button type="button" class="btn-modal-cancel" @click="handleClose" :disabled="loading">
          Cancelar
        </button>
        <button
          type="submit"
          class="btn-modal-save"
          :disabled="loading || !form.montoAbonado || !form.numeroOperacion"
        >
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-send"></i>
          <span>Registrar y Enviar a Finanzas</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { comercialService } from '../services/comercialService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { useAuthStore } from '@/features/auth/stores/authStore'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  cotizacion: {
    type: Object,
    default: null
  },
  cotizaciones: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'saved'])
const authStore = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')
const fileInputRef = ref(null)
const selectedFile = ref(null)

const selectedCotizacionId = ref(null)
const cotizacionesDisponibles = ref([])
const loadingCotizaciones = ref(false)

const activeCotizacion = computed(() => {
  if (props.cotizacion) return props.cotizacion
  return cotizacionesDisponibles.value.find(c => c.idCotizacion === selectedCotizacionId.value) || null
})

const defaultForm = () => ({
  tipoAbono: 'ADELANTO',
  moneda: 'USD',
  montoAbonado: null,
  tipoCambio: 3.75,
  metodoPago: 'TRANSFERENCIA_BANCARIA',
  bancoOrigen: '',
  bancoDestino: 'BCP Dólares Cta Cte 191-0023412-1-44',
  numeroOperacion: '',
  fechaOperacion: new Date().toISOString().substring(0, 10),
  idDocumentoComprobante: null
})

const form = ref(defaultForm())

const modalSubtitle = computed(() => {
  if (activeCotizacion.value?.codigoCotizacion) {
    return `Abono de fondos para la Cotización ${activeCotizacion.value.codigoCotizacion}`
  }
  return 'Ingrese los detalles del depósito bancario para revisión de Finanzas'
})

const montoEquivalentePenDisplay = computed(() => {
  const monto = Number(form.value.montoAbonado || 0)
  const tc = Number(form.value.tipoCambio || 3.75)
  if (form.value.moneda === 'PEN') {
    return monto.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }
  return (monto * tc).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
})

watch(() => props.visible, async (val) => {
  if (val) {
    selectedCotizacionId.value = null
    resetForm()
    if (!props.cotizacion) {
      if (props.cotizaciones && props.cotizaciones.length > 0) {
        cotizacionesDisponibles.value = props.cotizaciones.filter(c => c.estado === 'APROBADA' && c.estadoPago !== 'PAGADO_TOTAL')
      } else {
        await loadCotizacionesAprobadas()
      }
    }
  }
})

async function loadCotizacionesAprobadas() {
  loadingCotizaciones.value = true
  try {
    const list = await comercialService.getCotizaciones()
    cotizacionesDisponibles.value = (list || []).filter(c => c.estado === 'APROBADA' && c.estadoPago !== 'PAGADO_TOTAL')
  } catch (err) {
    console.error('Error cargando cotizaciones aprobadas:', err)
  } finally {
    loadingCotizaciones.value = false
  }
}

function onCotizacionSelected() {
  if (activeCotizacion.value) {
    aplicarMontoInicial()
  }
}

function resetForm() {
  form.value = defaultForm()
  selectedFile.value = null
  errorMessage.value = ''
  aplicarMontoInicial()
}

function aplicarMontoInicial() {
  const target = activeCotizacion.value
  if (target) {
    const saldo = Number(target.saldoPendiente != null ? target.saldoPendiente : target.totalCotizado || 0)
    form.value.montoAbonado = Math.round(saldo * 0.3 * 100) / 100
  }
}

function setTipoAbono(tipo) {
  form.value.tipoAbono = tipo
  if (tipo === 'PAGO_TOTAL' && activeCotizacion.value) {
    const total = Number(activeCotizacion.value.saldoPendiente != null ? activeCotizacion.value.saldoPendiente : activeCotizacion.value.totalCotizado || 0)
    form.value.montoAbonado = total
  }
}

function calcularMontoSugerido(pct) {
  if (!activeCotizacion.value) return '0.00'
  const total = Number(activeCotizacion.value.totalCotizado || 0)
  return (total * pct).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function aplicarPorcentaje(pct) {
  if (!activeCotizacion.value) return
  const total = Number(activeCotizacion.value.totalCotizado || 0)
  form.value.montoAbonado = Math.round(total * pct * 100) / 100
}

function triggerFileInput() {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

function onFileSelected(event) {
  const file = event.target.files?.[0]
  if (file) {
    if (file.size > 10 * 1024 * 1024) {
      errorMessage.value = 'El archivo supera el límite máximo permitido de 10 MB.'
      return
    }
    selectedFile.value = file
    errorMessage.value = ''
  }
}

function clearSelectedFile() {
  selectedFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function handleClose() {
  emit('close')
}

async function handleSubmit() {
  const targetCotizacion = activeCotizacion.value
  if (!targetCotizacion?.idCotizacion) {
    errorMessage.value = 'Debe seleccionar una cotización aprobada para registrar el pago.'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    let idDocComprobante = null

    // 1. Si adjuntó archivo, subirlo primero
    if (selectedFile.value) {
      try {
        const uploadResult = await documentoService.upload([selectedFile.value], {
          categoria: 'COMPROBANTE_PAGO',
          descripcion: `Voucher depósito cotización ${targetCotizacion.codigoCotizacion} - Op: ${form.value.numeroOperacion}`,
          entidadTipo: 'COTIZACION',
          idEntidad: targetCotizacion.idCotizacion,
          idUsuario: authStore.user?.idUsuario || 1
        })
        if (Array.isArray(uploadResult) && uploadResult.length > 0) {
          idDocComprobante = uploadResult[0].idDocumento
        }
      } catch (err) {
        console.warn('No se pudo subir comprobante, se registrará el pago sin adjunto:', err)
      }
    }

    // 2. Registrar el pago en el backend
    const payload = {
      idCotizacion: targetCotizacion.idCotizacion,
      tipoAbono: form.value.tipoAbono,
      montoAbonado: Number(form.value.montoAbonado),
      moneda: form.value.moneda,
      tipoCambio: Number(form.value.tipoCambio || 3.75),
      metodoPago: form.value.metodoPago,
      bancoOrigen: form.value.bancoOrigen,
      bancoDestino: form.value.bancoDestino,
      numeroOperacion: form.value.numeroOperacion,
      fechaOperacion: form.value.fechaOperacion,
      idDocumentoComprobante: idDocComprobante
    }

    const nuevoPago = await comercialService.registrarPagoCotizacion(targetCotizacion.idCotizacion, payload)

    emit('saved', { pago: nuevoPago, cotizacion: targetCotizacion })
    handleClose()
  } catch (error) {
    console.error('Error registrando pago:', error)
    errorMessage.value = error.response?.data?.message || 'Error al registrar el comprobante de pago. Verifique los datos.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.form-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 0.5rem 0;
}

.cotizacion-picker-box {
  background: #fdfbf7;
  border: 1.5px dashed var(--color-border-card, #dcd5c7);
  padding: 1rem 1.25rem;
  border-radius: 8px;
}

.picker-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-title, #1c1917);
  margin-bottom: 0.5rem;
}

.picker-select {
  width: 100%;
  font-size: 0.875rem;
  font-weight: 600;
}

.picker-helper {
  display: block;
  font-size: 0.75rem;
  color: var(--color-text-muted, #78716c);
  margin-top: 0.375rem;
}

.helper-warn {
  color: #b45309;
  font-weight: 600;
}

.summary-pill-card {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 1rem;
  background: var(--color-bg-card, #f8f6f0);
  border: 1px solid var(--color-border-card, #e2dcd0);
  border-radius: 8px;
  padding: 0.875rem 1.25rem;
}

.summary-col {
  display: flex;
  flex-direction: column;
}

.summary-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-muted, #78716c);
  text-transform: uppercase;
}

.summary-val {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-title, #1c1917);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.summary-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted, #78716c);
}

.text-accent {
  color: var(--color-accent-copper, #c2410c);
}

.text-pending {
  color: #b45309;
}

.info-alert-box {
  display: flex;
  gap: 0.875rem;
  padding: 0.875rem 1rem;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  color: #166534;
}

.info-icon {
  font-size: 1.375rem;
  margin-top: 0.125rem;
  color: #15803d;
}

.info-content strong {
  display: block;
  font-size: 0.8125rem;
  margin-bottom: 0.25rem;
}

.info-content p {
  margin: 0;
  font-size: 0.75rem;
  line-height: 1.4;
  color: #14532d;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.field-item.full-width {
  grid-column: 1 / -1;
}

label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary, #44403c);
}

.type-abono-selector {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.625rem;
}

.type-pill {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.625rem 0.75rem;
  background: var(--color-bg-input, #ffffff);
  border: 1px solid var(--color-border-card, #e2dcd0);
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary, #44403c);
  cursor: pointer;
  transition: all 0.2s ease;
}

.type-pill:hover {
  border-color: var(--color-accent-copper, #c2410c);
  color: var(--color-accent-copper, #c2410c);
}

.type-pill.active {
  background: var(--color-accent-copper, #c2410c);
  border-color: var(--color-accent-copper, #c2410c);
  color: #ffffff;
}

.quick-percent-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.quick-label {
  font-size: 0.75rem;
  color: var(--color-text-muted, #78716c);
}

.btn-quick-pct {
  padding: 0.25rem 0.625rem;
  background: #f5f5f4;
  border: 1px solid #d6d3d1;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #44403c;
  cursor: pointer;
}

.btn-quick-pct:hover {
  background: #e7e5e4;
  border-color: #a8a29e;
}

.text-input,
.select-input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  background: var(--color-bg-input, #ffffff);
  border: 1px solid var(--color-border-card, #e2dcd0);
  border-radius: 6px;
  font-size: 0.875rem;
  color: var(--color-text-primary, #1c1917);
  outline: none;
  transition: border-color 0.2s;
}

.text-input:focus,
.select-input:focus {
  border-color: var(--color-accent-copper, #c2410c);
}

.input-with-prefix {
  position: relative;
  display: flex;
  align-items: center;
}

.currency-prefix {
  position: absolute;
  left: 0.875rem;
  font-weight: 700;
  color: var(--color-text-muted, #78716c);
}

.input-with-prefix .text-input {
  padding-left: 2.25rem;
}

.equivalent-display {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0.5rem 0.875rem;
  background: #f5f5f4;
  border: 1px solid #e7e5e4;
  border-radius: 6px;
  min-height: 42px;
}

.equivalent-display strong {
  font-size: 0.9375rem;
  color: #15803d;
}

.tc-hint {
  font-size: 0.6875rem;
  color: #78716c;
}

.voucher-upload-box {
  margin-top: 0.25rem;
}

.voucher-label {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  margin-bottom: 0.375rem;
}

.file-dropzone {
  border: 2px dashed var(--color-border-card, #d6d3d1);
  border-radius: 8px;
  padding: 1.25rem;
  text-align: center;
  background: #fafaf9;
  cursor: pointer;
  transition: all 0.2s;
}

.file-dropzone:hover {
  border-color: var(--color-accent-copper, #c2410c);
  background: #fff7ed;
}

.hidden-file-input {
  display: none;
}

.dropzone-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.upload-icon {
  font-size: 1.75rem;
  color: var(--color-accent-copper, #c2410c);
}

.upload-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #44403c;
}

.upload-sub {
  font-size: 0.6875rem;
  color: #78716c;
}

.file-selected-badge {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #ffffff;
  padding: 0.625rem 1rem;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  text-align: left;
}

.file-icon {
  font-size: 1.5rem;
  color: #dc2626;
}

.file-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.file-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #1c1917;
}

.file-size {
  font-size: 0.6875rem;
  color: #78716c;
}

.btn-remove-file {
  background: transparent;
  border: none;
  color: #78716c;
  cursor: pointer;
  font-size: 0.875rem;
  padding: 0.25rem;
}

.btn-remove-file:hover {
  color: #dc2626;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border-card, #e2dcd0);
}

.btn-modal-cancel {
  padding: 0.625rem 1.25rem;
  background: transparent;
  border: 1px solid var(--color-border-card, #d6d3d1);
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-secondary, #57534e);
  cursor: pointer;
}

.btn-modal-cancel:hover {
  background: #f5f5f4;
}

.btn-modal-save {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.5rem;
  background: var(--color-accent-copper, #c2410c);
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 700;
  color: #ffffff;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-modal-save:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-modal-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .type-abono-selector {
    grid-template-columns: 1fr;
  }
  .summary-pill-card {
    grid-template-columns: 1fr;
  }
}
</style>
