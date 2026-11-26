<template>
  <div class="registrar-pago-view fade-in">
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
        <div v-if="obligacionSeleccionada" class="header-doc-meta">
          <Tag
            :value="getTipoObligacionMeta(obligacionSeleccionada.tipoObligacion).label"
            :severity="getTipoObligacionMeta(obligacionSeleccionada.tipoObligacion).severity"
            class="sga-mini-tag"
          />
          <span class="sga-mono doc-pill">{{ obligacionSeleccionada.codigo }}</span>
        </div>
      </div>

      <div v-if="obligacionSeleccionada" class="sga-form-actions">
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
          type="button"
          label="Confirmar y Registrar Abono"
          icon="pi pi-send"
          size="small"
          :loading="enviando"
          :disabled="errorMontoExcedido"
          @click="confirmarYRegistrar"
        />
      </div>
    </header>

    <!-- Cargando -->
    <div v-if="loadingObligaciones" class="sga-card-surface state-card">
      <i class="pi pi-spin pi-spinner state-icon"></i>
      <span>Cargando detalle de obligaciones financieras...</span>
    </div>

    <!-- Sin obligaciones pendientes -->
    <div v-else-if="!obligacionSeleccionada && obligaciones.length === 0" class="sga-card-surface state-card">
      <i class="pi pi-check-circle state-icon sga-text-success"></i>
      <h3>No tiene obligaciones pendientes de pago</h3>
      <p>Todas sus cotizaciones y expedientes se encuentran al día con sus abonos en USD.</p>
      <Button type="button" label="Volver a Cobros y Pagos" size="small" @click="volver" />
    </div>

    <!-- Layout Split-Screen sga-forms.css -->
    <div v-else class="sga-split-layout">
      <form class="sga-split-main" @submit.prevent="confirmarYRegistrar">
        <!-- Bloque 1: Obligación y Monto -->
        <section class="sga-card-surface form-card">
          <div class="block-head">
            <span class="sga-kicker">PASO 01 // MONEDA USD</span>
            <h3 class="sga-chart-title">Documento a Amortizar y Monto del Abono</h3>
          </div>

          <div v-if="obligaciones.length > 1" class="sga-form-field">
            <label for="select-obligation" class="sga-form-label">Seleccionar Documento *</label>
            <Select
              id="select-obligation"
              v-model="idReferenciaSeleccionada"
              :options="obligacionSelectOptions"
              optionLabel="label"
              optionValue="value"
              size="small"
              fluid
              @change="cambiarObligacion"
            />
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label">Modalidad de Abono Rápido (Anticipo &gt; 30%)</label>
            <div class="presets-grid">
              <button
                type="button"
                :class="['preset-card', { 'is-selected': presetSeleccionado === '35' }]"
                @click="aplicarPreset(35)"
              >
                <span class="preset-label">35% Anticipo (&gt; 30%)</span>
                <strong class="sga-mono preset-val">$ {{ formatCurrency(monto35Calculado) }}</strong>
              </button>

              <button
                type="button"
                :class="['preset-card', { 'is-selected': presetSeleccionado === '50' }]"
                @click="aplicarPreset(50)"
              >
                <span class="preset-label">50% Mitad</span>
                <strong class="sga-mono preset-val">$ {{ formatCurrency(monto50Calculado) }}</strong>
              </button>

              <button
                type="button"
                :class="['preset-card', { 'is-selected': presetSeleccionado === '100' }]"
                @click="aplicarPreset(100)"
              >
                <span class="preset-label">100% Saldo Completo</span>
                <strong class="sga-mono preset-val">$ {{ formatCurrency(saldoPendienteMax) }}</strong>
              </button>

              <button
                type="button"
                :class="['preset-card', { 'is-selected': presetSeleccionado === 'custom' }]"
                @click="activarMontoLibre"
              >
                <span class="preset-label">Monto Libre</span>
                <strong class="preset-val">Personalizado</strong>
              </button>
            </div>
          </div>

          <div class="sga-form-field">
            <label for="input-monto" class="sga-form-label">Monto a Transferir / Abonar ($ USD) *</label>
            <InputNumber
              id="input-monto"
              v-model="form.montoAbonado"
              mode="currency"
              currency="USD"
              locale="en-US"
              :min="0.01"
              :minFractionDigits="2"
              :maxFractionDigits="2"
              size="small"
              fluid
              @update:modelValue="onMontoManualInput"
            />
            <span v-if="errorMontoExcedido" class="error-hint">
              El monto ingresado excede el saldo pendiente ($ {{ formatCurrency(saldoPendienteMax) }}).
            </span>
            <span v-else-if="errorAnticipoMinimo" class="error-hint">
              El anticipo inicial debe ser mayor al 30% del total cotizado (&gt; $ {{ formatCurrency(umbral30Usd) }} USD) o cubrir el 100% del saldo.
            </span>
            <span v-else-if="requiereMinimo30" class="sga-cell-sub">
              Anticipo inicial mínimo: mayor al 30% del total (&gt; $ {{ formatCurrency(umbral30Usd) }} USD).
            </span>
          </div>
        </section>

        <!-- Bloque 2: Cuenta Receptora TransAduanas -->
        <section class="sga-card-surface form-card">
          <div class="block-head">
            <span class="sga-kicker">PASO 02 // CUENTA RECEPTORA</span>
            <h3 class="sga-chart-title">Cuenta Corriente TransAduanas (USD)</h3>
          </div>

          <div class="banks-grid">
            <div
              v-for="banco in CUENTAS_BANCARIAS_USD"
              :key="banco.id"
              role="button"
              tabindex="0"
              :class="['bank-card', { 'is-selected': form.bancoDestino === banco.nombre }]"
              @click="form.bancoDestino = banco.nombre"
              @keydown.enter.prevent="form.bancoDestino = banco.nombre"
              @keydown.space.prevent="form.bancoDestino = banco.nombre"
            >
              <div class="bank-top">
                <strong>{{ banco.banco }}</strong>
                <Tag value="USD" severity="secondary" class="sga-mini-tag sga-mono" />
              </div>
              <div class="bank-nums sga-mono">
                <span>Cta: {{ banco.numeroCuenta }}</span>
                <span>CCI: {{ banco.cci }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Bloque 3: Datos de Operación y Voucher -->
        <section class="sga-card-surface form-card">
          <div class="block-head">
            <span class="sga-kicker">PASO 03 // COMPROBANTE</span>
            <h3 class="sga-chart-title">Datos de Transferencia y Voucher Bancario</h3>
          </div>

          <div class="two-col-grid">
            <div class="sga-form-field">
              <label for="banco-origen" class="sga-form-label">Banco Emisor (Su Banco) *</label>
              <Select
                id="banco-origen"
                v-model="form.bancoOrigen"
                :options="BANCOS_ORIGEN_OPTIONS"
                optionLabel="label"
                optionValue="value"
                size="small"
                fluid
              />
            </div>

            <div class="sga-form-field">
              <label for="numero-operacion" class="sga-form-label">N° de Operación Bancaria *</label>
              <InputText
                id="numero-operacion"
                v-model.trim="form.numeroOperacion"
                placeholder="Ej. OP-4928172"
                size="small"
                fluid
              />
            </div>

            <div class="sga-form-field">
              <label for="fecha-operacion" class="sga-form-label">Fecha de Transferencia *</label>
              <InputText
                id="fecha-operacion"
                v-model="form.fechaOperacion"
                type="date"
                :max="fechaHoy"
                size="small"
                fluid
              />
            </div>

            <div class="sga-form-field">
              <label for="metodo-pago" class="sga-form-label">Modalidad de Transferencia *</label>
              <Select
                id="metodo-pago"
                v-model="form.metodoPago"
                :options="METODOS_PAGO_OPTIONS"
                optionLabel="label"
                optionValue="value"
                size="small"
                fluid
              />
            </div>
          </div>

          <PagoVoucherUploadField v-model="archivoVoucher" />

          <div class="sga-form-field">
            <label for="observaciones" class="sga-form-label">Notas para Finanzas (Opcional)</label>
            <Textarea
              id="observaciones"
              v-model="form.observaciones"
              rows="2"
              placeholder="Referencia de pago empresarial o sucursal bancaria..."
              size="small"
              fluid
            />
          </div>
        </section>
      </form>

      <!-- Columna Lateral Sticky: Resumen Financiero en Vivo -->
      <aside class="sga-split-aside">
        <div class="sga-card-surface summary-card">
          <span class="sga-kicker">ESTADO DE CUENTA EN VIVO</span>
          <h3 class="summary-code sga-mono">{{ obligacionSeleccionada?.codigo }}</h3>
          <p class="summary-desc">{{ obligacionSeleccionada?.descripcion }}</p>

          <div class="summary-rows">
            <div class="summary-row">
              <span>Total Contratado</span>
              <strong class="sga-mono">$ {{ formatCurrency(obligacionSeleccionada?.totalContratadoUsd) }}</strong>
            </div>
            <div class="summary-row">
              <span>Amortizado a la Fecha</span>
              <strong class="sga-mono sga-text-success">$ {{ formatCurrency(obligacionSeleccionada?.totalAmortizadoUsd) }}</strong>
            </div>
            <div class="summary-row">
              <span>Saldo Actual</span>
              <strong class="sga-mono sga-text-warn">$ {{ formatCurrency(saldoPendienteMax) }}</strong>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-row">
              <span>Abono a Registrar</span>
              <strong class="sga-mono sga-text-success">- $ {{ formatCurrency(form.montoAbonado || 0) }}</strong>
            </div>
            <div class="summary-row is-total">
              <span>Saldo Restante Estimado</span>
              <strong class="sga-mono" :class="saldoRestanteEstimado <= 0 ? 'sga-text-success' : 'sga-text-warn'">
                $ {{ formatCurrency(Math.max(0, saldoRestanteEstimado)) }}
              </strong>
            </div>
          </div>

          <div class="sga-banner is-info">
            <i class="pi pi-info-circle"></i>
            <span v-if="obligacionSeleccionada?.tipoObligacion === 'EXPEDIENTE_HEREDADO'">
              Consolida saldo de cotización y gastos aduaneros. Puede abonar parcial o totalmente.
            </span>
            <span v-else>
              Al validarse este abono en Finanzas, quedará habilitada la apertura formal de su expediente DAM.
            </span>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import PagoVoucherUploadField from '../components/PagoVoucherUploadField.vue'
import { portalPagoService } from '../services/portalPagoService'
import { useNotification } from '@/shared/composables/useNotification'
import { formatCurrency } from '@/shared/utils/formatters'
import {
  CUENTAS_BANCARIAS_USD,
  BANCOS_ORIGEN_OPTIONS,
  METODOS_PAGO_OPTIONS,
  calcularMontoPorcentaje,
  getTipoObligacionMeta,
  validarRegistroPagoForm
} from '../utils/pagoFormUtils'

const route = useRoute()
const router = useRouter()
const { showSuccess, showError, showWarn } = useNotification()

const loadingObligaciones = ref(true)
const enviando = ref(false)
const obligaciones = ref([])
const idReferenciaSeleccionada = ref(null)
const presetSeleccionado = ref('50')
const archivoVoucher = ref(null)

const fechaHoy = new Date().toISOString().substring(0, 10)

const form = ref({
  tipoObligacion: 'ANTICIPO_COTIZACION',
  idReferencia: null,
  montoAbonado: null,
  moneda: 'USD',
  metodoPago: 'TRANSFERENCIA_BANCARIA',
  bancoOrigen: 'BCP',
  bancoDestino: 'BCP Dolares TransAduanas',
  numeroOperacion: '',
  fechaOperacion: fechaHoy,
  observaciones: '',
  idDocumentoComprobante: null
})

const obligacionSelectOptions = computed(() =>
  obligaciones.value.map(ob => ({
    value: ob.idReferencia,
    label: `${ob.codigo} • ${ob.descripcion} (Saldo: $ ${formatCurrency(ob.saldoPendienteUsd)})`
  }))
)

const obligacionSeleccionada = computed(() =>
  obligaciones.value.find(o => o.idReferencia === idReferenciaSeleccionada.value) || null
)

const saldoPendienteMax = computed(() =>
  obligacionSeleccionada.value ? Number(obligacionSeleccionada.value.saldoPendienteUsd || 0) : 0
)

const totalContratadoObligacion = computed(() =>
  obligacionSeleccionada.value ? Number(obligacionSeleccionada.value.totalContratadoUsd || 0) : 0
)

const totalAmortizadoObligacion = computed(() =>
  obligacionSeleccionada.value ? Number(obligacionSeleccionada.value.totalAmortizadoUsd || 0) : 0
)

const requiereMinimo30 = computed(() =>
  totalAmortizadoObligacion.value === 0 && totalContratadoObligacion.value > 0
)

const umbral30Usd = computed(() =>
  calcularMontoPorcentaje(totalContratadoObligacion.value, 30)
)

const monto35Calculado = computed(() => {
  const base = totalContratadoObligacion.value > 0 ? totalContratadoObligacion.value : saldoPendienteMax.value
  return Math.min(saldoPendienteMax.value, calcularMontoPorcentaje(base, 35))
})

const monto50Calculado = computed(() => calcularMontoPorcentaje(saldoPendienteMax.value, 50))

const saldoRestanteEstimado = computed(() =>
  Number((saldoPendienteMax.value - Number(form.value.montoAbonado || 0)).toFixed(2))
)

const errorMontoExcedido = computed(() => {
  const monto = Number(form.value.montoAbonado || 0)
  return monto > saldoPendienteMax.value && saldoPendienteMax.value > 0
})

const errorAnticipoMinimo = computed(() => {
  if (!requiereMinimo30.value || saldoPendienteMax.value <= 0) return false
  const monto = Number(form.value.montoAbonado || 0)
  return monto > 0 && monto < saldoPendienteMax.value && monto <= umbral30Usd.value
})

async function cargarObligaciones() {
  loadingObligaciones.value = true
  try {
    const data = await portalPagoService.listarObligaciones()
    if (data) {
      obligaciones.value = data

      const qTipo = route.query.tipo?.toString().toUpperCase()
      const qId = Number(route.query.id)

      let seleccion = null
      if (qId) {
        if (qTipo === 'EXPEDIENTE' || qTipo === 'EXPEDIENTE_HEREDADO') {
          seleccion = obligaciones.value.find(o => o.idExpediente === qId)
        } else {
          seleccion = obligaciones.value.find(o => o.idCotizacion === qId || o.idReferencia === qId)
        }
      }

      if (!seleccion && obligaciones.value.length > 0) {
        seleccion = obligaciones.value[0]
      }

      if (seleccion) {
        idReferenciaSeleccionada.value = seleccion.idReferencia
        aplicarObligacion(seleccion)
      }
    }
  } catch (err) {
    console.error('Error al cargar obligaciones pendientes', err)
    showError('No se pudieron recuperar las obligaciones de pago. Verifique su conexión.')
  } finally {
    loadingObligaciones.value = false
  }
}

function aplicarObligacion(ob) {
  form.value.tipoObligacion = ob.tipoObligacion
  form.value.idReferencia = ob.idReferencia
  aplicarPreset(35)
}

function cambiarObligacion() {
  if (obligacionSeleccionada.value) {
    aplicarObligacion(obligacionSeleccionada.value)
  }
}

function aplicarPreset(pct) {
  presetSeleccionado.value = String(pct)
  if (pct === 35) {
    form.value.montoAbonado = monto35Calculado.value
  } else if (pct === 50) {
    form.value.montoAbonado = monto50Calculado.value
  } else if (pct === 100) {
    form.value.montoAbonado = saldoPendienteMax.value
  }
}

function activarMontoLibre() {
  presetSeleccionado.value = 'custom'
}

function onMontoManualInput() {
  const val = Number(form.value.montoAbonado || 0)
  if (val === monto35Calculado.value) {
    presetSeleccionado.value = '35'
  } else if (val === monto50Calculado.value) {
    presetSeleccionado.value = '50'
  } else if (val === saldoPendienteMax.value) {
    presetSeleccionado.value = '100'
  } else {
    presetSeleccionado.value = 'custom'
  }
}

async function confirmarYRegistrar() {
  const check = validarRegistroPagoForm({
    montoAbonado: form.value.montoAbonado,
    saldoPendienteMax: saldoPendienteMax.value,
    totalContratadoUsd: totalContratadoObligacion.value,
    totalAmortizadoUsd: totalAmortizadoObligacion.value,
    numeroOperacion: form.value.numeroOperacion,
    bancoOrigen: form.value.bancoOrigen,
    archivoVoucher: archivoVoucher.value
  })

  if (!check.valid) {
    showWarn(check.error)
    return
  }

  enviando.value = true
  try {
    const docRes = await portalPagoService.subirComprobante(archivoVoucher.value)
    form.value.idDocumentoComprobante = docRes?.idDocumento || docRes?.id
    form.value.moneda = 'USD'

    const res = await portalPagoService.registrarVoucher(form.value)
    if (res) {
      showSuccess(
        `¡Voucher ${res.codigoPago || ''} registrado con éxito! Ha sido remitido a conciliación contable en Finanzas.`,
        'Voucher Registrado',
        4500
      )
      router.push('/pagos')
    }
  } catch (err) {
    console.error('Error al registrar voucher', err)
    showError(err.response?.data?.message || 'Error al registrar el comprobante de pago.')
  } finally {
    enviando.value = false
  }
}

function volver() {
  router.push('/pagos')
}

onMounted(cargarObligaciones)
</script>

<style scoped>
.registrar-pago-view {
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

.form-card,
.summary-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.block-head {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.presets-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.preset-card {
  padding: 0.75rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  text-align: left;
  cursor: pointer;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}

.preset-card.is-selected {
  border-color: var(--color-primary);
  background: var(--color-bg-surface);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.preset-label {
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.preset-val {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.error-hint {
  font-size: 0.76rem;
  font-weight: 600;
  color: var(--color-danger-text);
}

.banks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 0.75rem;
}

.bank-card {
  padding: 0.85rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  cursor: pointer;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}

.bank-card.is-selected {
  border-color: var(--color-primary);
  background: var(--color-bg-surface);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.bank-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  font-size: 0.84rem;
  color: var(--color-text-main);
}

.bank-nums {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  font-size: 0.74rem;
  color: var(--color-text-muted);
}

.two-col-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.summary-code {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.summary-desc {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.summary-rows {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

.summary-row strong {
  color: var(--color-text-main);
}

.summary-row.is-total {
  padding: 0.65rem 0.75rem;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  font-weight: 700;
  color: var(--color-text-main);
}

.summary-divider {
  height: 1px;
  background: var(--color-border-subtle);
}

@media (max-width: 720px) {
  .presets-grid,
  .two-col-grid {
    grid-template-columns: 1fr;
  }
}
</style>
