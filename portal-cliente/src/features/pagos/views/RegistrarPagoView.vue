<template>
  <div class="registrar-pago-view">
    <!-- Header de Navegación -->
    <div class="page-topbar">
      <button class="btn-back" @click="volver">
        <i class="pi pi-arrow-left"></i>
        <span>Volver a Cobros & Pagos</span>
      </button>
      <div class="topbar-status-chip" v-if="obligacionSeleccionada">
        <span class="chip-type" :class="obligacionSeleccionada.tipoObligacion === 'EXPEDIENTE_HEREDADO' ? 'chip-exp' : 'chip-cot'">
          {{ obligacionSeleccionada.tipoObligacion === 'EXPEDIENTE_HEREDADO' ? 'EXPEDIENTE ADUANERO (DEUDA HEREDADA)' : 'ANTICIPO DE COTIZACIÓN (PRE-EXPEDIENTE)' }}
        </span>
        <span class="chip-code font-mono">{{ obligacionSeleccionada.codigo }}</span>
      </div>
    </div>

    <div class="page-header">
      <div class="header-titles">
        <span class="view-kicker">CONCILIACIÓN BANCARIA // MONEDA USD</span>
        <h1 class="view-title">Registrar Comprobante / Voucher de Pago</h1>
        <p class="view-subtitle">
          Realice su abono bancario en Dólares Americanos ($ USD) para dar apertura al despacho o cancelar la deuda consolidada del expediente.
        </p>
      </div>
    </div>

    <div v-if="loadingObligaciones" class="loading-state">
      <i class="pi pi-spin pi-spinner"></i>
      <span>Cargando detalle de obligaciones financieras...</span>
    </div>

    <div v-else-if="!obligacionSeleccionada && obligaciones.length === 0" class="empty-state">
      <i class="pi pi-check-circle success-icon"></i>
      <h2>No tiene obligaciones pendientes de pago</h2>
      <p>Todas sus cotizaciones y expedientes se encuentran al día con sus abonos y liquidaciones en USD.</p>
      <button class="btn-primary" @click="volver">Volver al Tablero Principal</button>
    </div>

    <!-- Layout a Dos Columnas -->
    <div v-else class="payment-workspace-grid">
      <!-- Columna Izquierda: Ficha de la Obligación -->
      <div class="col-summary">
        <div class="summary-card">
          <div class="summary-card-header">
            <span class="card-kicker">DOCUMENTO A AMORTIZAR</span>
            <!-- Selector de Obligación si hay varias -->
            <div class="document-picker" v-if="obligaciones.length > 1">
              <label for="select-obligation" class="picker-label">Cambiar Documento:</label>
              <select id="select-obligation" v-model="idReferenciaSeleccionada" class="custom-select" @change="cambiarObligacion">
                <option v-for="ob in obligaciones" :key="ob.idReferencia + ob.tipoObligacion" :value="ob.idReferencia">
                  {{ ob.codigo }} • {{ ob.descripcion }} (Saldo: $ {{ formatCurrency(ob.saldoPendienteUsd) }})
                </option>
              </select>
            </div>
            <h2 class="doc-code font-mono">{{ obligacionSeleccionada?.codigo }}</h2>
            <span class="doc-desc">{{ obligacionSeleccionada?.descripcion }}</span>
          </div>

          <div class="summary-meta-grid">
            <div class="meta-item" v-if="obligacionSeleccionada?.codigoCotizacion">
              <span class="meta-label">COTIZACIÓN ORIGEN</span>
              <span class="meta-val font-mono">{{ obligacionSeleccionada.codigoCotizacion }}</span>
            </div>
            <div class="meta-item" v-if="obligacionSeleccionada?.numeroDam">
              <span class="meta-label">N° DAM / DUA</span>
              <span class="meta-val font-mono">{{ obligacionSeleccionada.numeroDam }}</span>
            </div>
            <div class="meta-item" v-if="obligacionSeleccionada?.canalSunat">
              <span class="meta-label">CANAL SUNAT</span>
              <span class="canal-badge" :class="obligacionSeleccionada.canalSunat.toLowerCase()">
                {{ obligacionSeleccionada.canalSunat }}
              </span>
            </div>
            <div class="meta-item">
              <span class="meta-label">ESTADO ACTUAL</span>
              <span class="status-pill">{{ formatEstado(obligacionSeleccionada?.estadoPago) }}</span>
            </div>
          </div>

          <!-- Desglose Financiero en USD -->
          <div class="financial-breakdown">
            <h3 class="breakdown-title">Estado de Cuenta (Dólares USD)</h3>
            <div class="breakdown-row">
              <span class="b-label">Total Contratado / Cotizado:</span>
              <span class="b-value font-mono">$ {{ formatCurrency(obligacionSeleccionada?.totalContratadoUsd) }}</span>
            </div>
            <div class="breakdown-row">
              <span class="b-label">Total Amortizado a la Fecha:</span>
              <span class="b-value font-mono success-text">$ {{ formatCurrency(obligacionSeleccionada?.totalAmortizadoUsd) }}</span>
            </div>
            <div class="breakdown-divider"></div>
            <div class="breakdown-row highlight">
              <span class="b-label">Saldo Pendiente Actual:</span>
              <span class="b-value font-mono warning-text">$ {{ formatCurrency(obligacionSeleccionada?.saldoPendienteUsd) }}</span>
            </div>
          </div>

          <!-- Mensaje Explicativo de Contexto -->
          <div class="info-callout" :class="obligacionSeleccionada?.tipoObligacion === 'EXPEDIENTE_HEREDADO' ? 'exp-info' : 'cot-info'">
            <i class="pi" :class="obligacionSeleccionada?.tipoObligacion === 'EXPEDIENTE_HEREDADO' ? 'pi-info-circle' : 'pi-bolt'"></i>
            <div class="info-text">
              <strong v-if="obligacionSeleccionada?.tipoObligacion === 'EXPEDIENTE_HEREDADO'">
                Deuda Heredada en Expediente
              </strong>
              <strong v-else>
                Anticipo para Apertura Operativa
              </strong>
              <p v-if="obligacionSeleccionada?.tipoObligacion === 'EXPEDIENTE_HEREDADO'">
                Este expediente consolida el saldo pendiente de cotización junto con los gastos y tributos aduaneros en curso. Puede abonar el 50%, cancelar el saldo total o registrar abonos progresivos.
              </p>
              <p v-else>
                El registro del anticipo (mínimo sugerido 50%) permite a Finanzas validar los fondos y autorizar de inmediato la apertura formal del Expediente y numeración de DAM.
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Columna Derecha: Formulario Interactivo -->
      <div class="col-form">
        <form @submit.prevent="confirmarYRegistrar" class="payment-form-card">
          <!-- 1. Modalidad y Monto de Abono -->
          <div class="form-section">
            <div class="section-badge-header">
              <span class="step-num">1</span>
              <div>
                <h3 class="section-title">Modalidad y Monto del Abono (USD)</h3>
                <p class="section-subtitle">Seleccione un porcentaje sugerido o digite una cantidad personalizada en Dólares.</p>
              </div>
            </div>

            <!-- Chips de Preajuste Rápido -->
            <div class="presets-row">
              <button 
                type="button" 
                class="preset-chip"
                :class="{ active: presetSeleccionado === '50' }"
                @click="aplicarPreset(50)"
              >
                <div class="chip-main">
                  <i class="pi pi-percentage"></i>
                  <span>50% Anticipo / Mitad</span>
                </div>
                <span class="chip-amount font-mono">$ {{ formatCurrency(monto50Calculado) }}</span>
              </button>

              <button 
                type="button" 
                class="preset-chip"
                :class="{ active: presetSeleccionado === '100' }"
                @click="aplicarPreset(100)"
              >
                <div class="chip-main">
                  <i class="pi pi-check-circle"></i>
                  <span>100% Pago Completo</span>
                </div>
                <span class="chip-amount font-mono">$ {{ formatCurrency(saldoPendienteMax) }}</span>
              </button>

              <button 
                type="button" 
                class="preset-chip"
                :class="{ active: presetSeleccionado === 'custom' }"
                @click="activarMontoLibre"
              >
                <div class="chip-main">
                  <i class="pi pi-sliders-h"></i>
                  <span>Monto Menor / Libre</span>
                </div>
                <span class="chip-amount">Personalizado</span>
              </button>
            </div>

            <!-- Input Numérico con validación -->
            <div class="form-field-group mt-3">
              <label for="input-monto">Monto a Transferir / Abonar ($ USD) *</label>
              <div class="input-currency-wrapper">
                <span class="currency-prefix">$</span>
                <input 
                  id="input-monto"
                  v-model.number="form.montoAbonado" 
                  type="number" 
                  step="0.01" 
                  min="0.01"
                  :max="saldoPendienteMax"
                  placeholder="0.00" 
                  required 
                  class="custom-input font-mono input-lg"
                  @input="onMontoManualInput"
                />
                <span class="currency-tag">USD</span>
              </div>
              <span class="input-hint" v-if="errorMontoExcedido" style="color: var(--color-danger, #b3261e);">
                El monto ingresado excede el saldo pendiente ($ {{ formatCurrency(saldoPendienteMax) }}).
              </span>
            </div>

            <!-- Widget Simulador de Saldo Proyectado -->
            <div class="simulation-box">
              <div class="sim-item">
                <span class="sim-lbl">Saldo Actual</span>
                <span class="sim-val font-mono">$ {{ formatCurrency(saldoPendienteMax) }}</span>
              </div>
              <div class="sim-operator">-</div>
              <div class="sim-item">
                <span class="sim-lbl">Abono a Registrar</span>
                <span class="sim-val font-mono success-text">$ {{ formatCurrency(form.montoAbonado || 0) }}</span>
              </div>
              <div class="sim-operator">=</div>
              <div class="sim-item highlight">
                <span class="sim-lbl">Saldo Restante Estimado</span>
                <span class="sim-val font-mono" :class="saldoRestanteEstimado <= 0 ? 'success-text' : 'warning-text'">
                  $ {{ formatCurrency(Math.max(0, saldoRestanteEstimado)) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 2. Cuentas Receptoras de TransAduanas en USD -->
          <div class="form-section">
            <div class="section-badge-header">
              <span class="step-num">2</span>
              <div>
                <h3 class="section-title">Cuenta Receptora de TransAduanas (USD)</h3>
                <p class="section-subtitle">Seleccione la cuenta corriente en Dólares a la que realizó la transferencia.</p>
              </div>
            </div>

            <div class="bank-accounts-grid">
              <div 
                v-for="banco in cuentasBancariasUsd" 
                :key="banco.id"
                class="bank-account-card"
                :class="{ selected: form.bancoDestino === banco.nombre }"
                @click="form.bancoDestino = banco.nombre"
              >
                <div class="bank-card-header">
                  <div class="bank-badge-title">
                    <span class="bank-logo-text">{{ banco.banco }}</span>
                    <span class="bank-curr-pill">USD</span>
                  </div>
                  <i class="pi pi-check" v-if="form.bancoDestino === banco.nombre"></i>
                </div>
                <div class="bank-card-numbers font-mono">
                  <div class="acc-row">
                    <span class="lbl">Cta Cte:</span>
                    <span class="val">{{ banco.numeroCuenta }}</span>
                  </div>
                  <div class="acc-row">
                    <span class="lbl">CCI:</span>
                    <span class="val">{{ banco.cci }}</span>
                  </div>
                </div>
                <div class="bank-titular">TransAduanas Logística S.A.C.</div>
              </div>
            </div>
          </div>

          <!-- 3. Datos de la Operación Bancaria -->
          <div class="form-section">
            <div class="section-badge-header">
              <span class="step-num">3</span>
              <div>
                <h3 class="section-title">Datos de la Operación y Comprobante</h3>
                <p class="section-subtitle">Ingrese el número de constancia bancaria y adjunte el voucher emitido.</p>
              </div>
            </div>

            <div class="form-fields-grid">
              <div class="form-field">
                <label for="banco-origen">Banco Emisor (Su Banco) *</label>
                <select id="banco-origen" v-model="form.bancoOrigen" required class="custom-select">
                  <option value="BCP">Banco de Crédito del Perú (BCP)</option>
                  <option value="BBVA">BBVA Perú</option>
                  <option value="INTERBANK">Interbank</option>
                  <option value="SCOTIABANK">Scotiabank Perú</option>
                  <option value="BANBIF">BanBif</option>
                  <option value="BANCO_PICHINCHA">Banco Pichincha</option>
                  <option value="BANCO_COMERCIO">Banco de Comercio</option>
                  <option value="TRANSFERENCIA_EXTERIOR">Transferencia Internacional / Swift</option>
                </select>
              </div>

              <div class="form-field">
                <label for="numero-operacion">N° de Operación Bancaria *</label>
                <input 
                  id="numero-operacion"
                  v-model.trim="form.numeroOperacion" 
                  type="text" 
                  placeholder="Ej. OP-4928172 o N° Referencia" 
                  required 
                  class="custom-input font-mono"
                />
              </div>

              <div class="form-field">
                <label for="fecha-operacion">Fecha de la Transferencia *</label>
                <input 
                  id="fecha-operacion"
                  v-model="form.fechaOperacion" 
                  type="date" 
                  :max="fechaHoy"
                  required 
                  class="custom-input"
                />
              </div>

              <div class="form-field">
                <label for="metodo-pago">Modalidad de Transferencia *</label>
                <select id="metodo-pago" v-model="form.metodoPago" required class="custom-select">
                  <option value="TRANSFERENCIA_BANCARIA">Transferencia Bancaria Vía Web/App</option>
                  <option value="TRANSFERENCIA_INTERBANCARIA">Transferencia Interbancaria (CCI)</option>
                  <option value="DEPOSITO_EN_VENTANILLA">Depósito en Ventanilla / Agente</option>
                  <option value="TRANSFERENCIA_INTERNACIONAL">Transferencia Swift Exterior</option>
                </select>
              </div>
            </div>

            <!-- Zona de Carga de Comprobante / Voucher -->
            <div class="voucher-upload-section mt-3">
              <label class="field-label-bold">Adjuntar Voucher Bancario (PDF o Imagen) *</label>
              
              <div 
                class="file-dropzone" 
                :class="{ 'file-attached': archivoVoucher != null }"
                @dragover.prevent
                @drop.prevent="onFileDrop"
              >
                <input 
                  type="file" 
                  id="voucher-file-input" 
                  accept="application/pdf,image/png,image/jpeg,image/webp" 
                  class="file-input-hidden"
                  @change="onFileSelected"
                />

                <label for="voucher-file-input" class="dropzone-inner" v-if="!archivoVoucher">
                  <div class="dropzone-icon">
                    <i class="pi pi-upload"></i>
                  </div>
                  <div class="dropzone-texts">
                    <span class="primary-text">Haga clic para adjuntar comprobante o arrastre el archivo aquí</span>
                    <span class="sub-text">Formatos permitidos: PDF, PNG, JPG, WEBP (Hasta 10 MB)</span>
                  </div>
                </label>

                <div class="file-preview-bar" v-else>
                  <div class="file-icon-box">
                    <i class="pi" :class="archivoVoucher.type.includes('pdf') ? 'pi-file-pdf text-danger' : 'pi-image text-primary'"></i>
                  </div>
                  <div class="file-info-details">
                    <span class="file-name font-mono">{{ archivoVoucher.name }}</span>
                    <span class="file-size">{{ (archivoVoucher.size / 1024).toFixed(1) }} KB</span>
                  </div>
                  <button type="button" class="btn-remove-file" @click="quitarArchivo" title="Quitar archivo">
                    <i class="pi pi-times"></i>
                  </button>
                </div>
              </div>
            </div>

            <!-- Observaciones / Comentarios Adicionales -->
            <div class="form-field mt-3">
              <label for="observaciones">Observaciones o Notas para Finanzas (Opcional):</label>
              <textarea 
                id="observaciones"
                v-model="form.observaciones" 
                rows="2" 
                placeholder="Indique detalles adicionales, referencia de pago empresarial o sucursal bancaria..."
                class="custom-textarea"
              ></textarea>
            </div>
          </div>

          <!-- Acciones de Envío -->
          <div class="form-footer-actions">
            <button type="button" class="btn-secondary" @click="volver">
              Cancelar
            </button>

            <button type="submit" class="btn-primary btn-submit" :disabled="enviando || errorMontoExcedido">
              <i v-if="enviando" class="pi pi-spin pi-spinner"></i>
              <i v-else class="pi pi-send"></i>
              <span v-if="enviando">Registrando y Enviando Comprobante...</span>
              <span v-else>Confirmar y Registrar Abono Bancario ($ USD)</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { portalPagoService } from '../services/portalPagoService'
import { useNotification } from '@/shared/composables/useNotification'

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

const cuentasBancariasUsd = [
  {
    id: 'bcp-usd',
    banco: 'Banco de Crédito BCP',
    nombre: 'BCP Dolares TransAduanas',
    numeroCuenta: '191-289381-1-09',
    cci: '002-19100289381109-54'
  },
  {
    id: 'bbva-usd',
    banco: 'BBVA Perú',
    nombre: 'BBVA Dolares TransAduanas',
    numeroCuenta: '0011-0182-010049281',
    cci: '011-182-00010049281-22'
  },
  {
    id: 'interbank-usd',
    banco: 'Interbank',
    nombre: 'Interbank Dolares TransAduanas',
    numeroCuenta: '200-300189281',
    cci: '003-200-00300189281-19'
  }
]

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

onMounted(async () => {
  await cargarObligaciones()
})

async function cargarObligaciones() {
  loadingObligaciones.value = true
  try {
    const data = await portalPagoService.listarObligaciones()
    if (data) {
      obligaciones.value = data

      // Seleccionar según query params o el primer elemento
      const qTipo = route.query.tipo?.toString().toUpperCase()
      const qId = Number(route.query.id)

      let seleccion = null
      if (qId) {
        if (qTipo === 'EXPEDIENTE') {
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
  } finally {
    loadingObligaciones.value = false
  }
}

const obligacionSeleccionada = computed(() => {
  return obligaciones.value.find(o => o.idReferencia === idReferenciaSeleccionada.value) || null
})

const saldoPendienteMax = computed(() => {
  return obligacionSeleccionada.value ? Number(obligacionSeleccionada.value.saldoPendienteUsd || 0) : 0
})

const monto50Calculado = computed(() => {
  return Number((saldoPendienteMax.value / 2).toFixed(2))
})

const saldoRestanteEstimado = computed(() => {
  const monto = Number(form.value.montoAbonado || 0)
  return Number((saldoPendienteMax.value - monto).toFixed(2))
})

const errorMontoExcedido = computed(() => {
  const monto = Number(form.value.montoAbonado || 0)
  return monto > saldoPendienteMax.value && saldoPendienteMax.value > 0
})

function aplicarObligacion(ob) {
  form.value.tipoObligacion = ob.tipoObligacion
  form.value.idReferencia = ob.idReferencia
  // Aplicar preset del 50% por defecto
  aplicarPreset(50)
}

function cambiarObligacion() {
  const ob = obligacionSeleccionada.value
  if (ob) {
    aplicarObligacion(ob)
  }
}

function aplicarPreset(pct) {
  presetSeleccionado.value = String(pct)
  if (pct === 50) {
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
  if (val === monto50Calculado.value) {
    presetSeleccionado.value = '50'
  } else if (val === saldoPendienteMax.value) {
    presetSeleccionado.value = '100'
  } else {
    presetSeleccionado.value = 'custom'
  }
}

function onFileSelected(e) {
  const file = e.target.files?.[0]
  if (file) {
    archivoVoucher.value = file
  }
}

function onFileDrop(e) {
  const file = e.dataTransfer?.files?.[0]
  if (file) {
    archivoVoucher.value = file
  }
}

function quitarArchivo() {
  archivoVoucher.value = null
  const input = document.getElementById('voucher-file-input')
  if (input) input.value = ''
}

async function subirComprobante(file) {
  const docRes = await portalPagoService.subirComprobante(file)
  return docRes?.idDocumento || docRes?.id
}

async function confirmarYRegistrar() {
  if (!form.value.montoAbonado || form.value.montoAbonado <= 0) {
    showWarn('Por favor ingrese un monto válido a abonar.')
    return
  }

  if (errorMontoExcedido.value) {
    showError('El monto ingresado excede el saldo pendiente de la obligación.')
    return
  }

  if (!archivoVoucher.value) {
    showWarn('Debe adjuntar la constancia o voucher bancario en formato PDF o Imagen.')
    return
  }

  enviando.value = true
  try {
    // 1. Subir voucher si está seleccionado
    if (archivoVoucher.value) {
      const docId = await subirComprobante(archivoVoucher.value)
      form.value.idDocumentoComprobante = docId
    }

    // 2. Forzar moneda USD
    form.value.moneda = 'USD'

    const res = await portalPagoService.registrarVoucher(form.value)
    if (res) {
      showSuccess(`¡Voucher ${res.codigoPago || ''} registrado con éxito! Ha sido remitido a conciliación contable en Finanzas.`, 'Voucher Registrado', 4500)
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

function formatCurrency(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatEstado(st) {
  if (!st) return 'PENDIENTE'
  return st.replace(/_/g, ' ')
}
</script>

<style scoped>
.registrar-pago-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding-bottom: 3rem;
}

.page-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.5rem 1rem;
  color: var(--color-text-main, #2b2520);
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back:hover {
  background: var(--color-bg-subtle, #f2ebe1);
  border-color: var(--color-primary, #352f2a);
}

.topbar-status-chip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 20px;
  padding: 0.35rem 0.85rem;
}

.chip-type {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  padding: 0.2rem 0.5rem;
  border-radius: 12px;
}

.chip-type.chip-cot {
  background: var(--color-warning-bg, #fdf5e6);
  color: var(--color-warning-text, #8c5b16);
}

.chip-type.chip-exp {
  background: var(--color-info-bg, #eef4fc);
  color: var(--color-info-text, #1b4d89);
}

.chip-code {
  font-weight: 800;
  font-size: 0.85rem;
  color: var(--color-text-main, #2b2520);
}

.page-header {
  border-bottom: 1px solid var(--color-border, #e4dacb);
  padding-bottom: 1rem;
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
  margin-top: 0.35rem;
}

/* Layout a Dos Columnas */
.payment-workspace-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 1.75rem;
  align-items: start;
}

@media (max-width: 1024px) {
  .payment-workspace-grid {
    grid-template-columns: 1fr;
  }
}

/* Columna Izquierda */
.summary-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  box-shadow: 0 2px 6px rgba(0,0,0,0.03);
}

.summary-card-header .card-kicker {
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle, #998c7e);
  display: block;
  margin-bottom: 0.5rem;
}

.document-picker {
  margin-bottom: 0.75rem;
}

.picker-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted, #6f6356);
  margin-bottom: 0.25rem;
  display: block;
}

.doc-code {
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--color-text-main, #2b2520);
  margin: 0;
}

.doc-desc {
  font-size: 0.85rem;
  color: var(--color-text-muted, #6f6356);
  display: block;
  margin-top: 0.25rem;
}

.summary-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  padding: 0.75rem;
  background: var(--color-bg-base, #f6f3ee);
  border-radius: 6px;
}

.meta-item {
  display: flex;
  flex-direction: column;
}

.meta-label {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--color-text-subtle, #998c7e);
}

.meta-val {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
}

.canal-badge {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 800;
  text-align: center;
  width: fit-content;
}

.canal-badge.verde { background: #e6f4ea; color: #137333; }
.canal-badge.naranja { background: #fef7e0; color: #b06000; }
.canal-badge.rojo { background: #fce8e6; color: #c5221f; }

.status-pill {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--color-text-muted, #6f6356);
}

.financial-breakdown {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  border-top: 1px dashed var(--color-border, #e4dacb);
  padding-top: 1rem;
}

.breakdown-title {
  font-size: 0.78rem;
  font-weight: 800;
  color: var(--color-primary, #352f2a);
  margin: 0 0 0.5rem 0;
}

.breakdown-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
}

.breakdown-row.highlight {
  font-size: 0.95rem;
  font-weight: 800;
}

.breakdown-divider {
  border-bottom: 1px solid var(--color-border, #e4dacb);
  margin: 0.25rem 0;
}

.success-text { color: var(--color-success-text, #1e6638); }
.warning-text { color: var(--color-warning-text, #8c5b16); }

.info-callout {
  display: flex;
  gap: 0.75rem;
  padding: 0.85rem;
  border-radius: 6px;
  font-size: 0.8rem;
}

.info-callout.cot-info {
  background: var(--color-warning-bg, #fdf5e6);
  border: 1px solid var(--color-warning-border, #f8dab3);
  color: var(--color-warning-text, #8c5b16);
}

.info-callout.exp-info {
  background: var(--color-info-bg, #eef4fc);
  border: 1px solid var(--color-info-border, #b8d4f6);
  color: var(--color-info-text, #1b4d89);
}

.info-callout p {
  margin: 0.25rem 0 0 0;
  line-height: 1.35;
}

/* Columna Derecha: Formulario */
.payment-form-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 10px;
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
  box-shadow: 0 2px 6px rgba(0,0,0,0.03);
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-badge-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  border-bottom: 1px solid var(--color-border-subtle, #f0eae1);
  padding-bottom: 0.75rem;
}

.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-primary, #352f2a);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 800;
  flex-shrink: 0;
}

.section-title {
  font-size: 1.05rem;
  font-weight: 800;
  color: var(--color-primary, #352f2a);
  margin: 0;
}

.section-subtitle {
  font-size: 0.8rem;
  color: var(--color-text-muted, #6f6356);
  margin: 0.15rem 0 0 0;
}

/* Presets Chips */
.presets-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
}

@media (max-width: 640px) {
  .presets-row {
    grid-template-columns: 1fr;
  }
}

.preset-chip {
  background: var(--color-bg-base, #f6f3ee);
  border: 2px solid var(--color-border, #e4dacb);
  border-radius: 8px;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.preset-chip:hover {
  border-color: var(--color-primary, #352f2a);
  background: var(--color-bg-subtle, #f2ebe1);
}

.preset-chip.active {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(214, 180, 118, 0.2);
}

.chip-main {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
}

.chip-amount {
  font-size: 0.95rem;
  font-weight: 800;
  color: var(--color-primary, #352f2a);
}

/* Input Numérico */
.input-currency-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.currency-prefix {
  position: absolute;
  left: 1rem;
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--color-text-muted, #6f6356);
}

.input-lg {
  padding-left: 2.25rem;
  padding-right: 3.5rem;
  font-size: 1.25rem;
  font-weight: 800;
  height: 48px;
}

.currency-tag {
  position: absolute;
  right: 1rem;
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--color-text-subtle, #998c7e);
}

/* Simulador de Saldo */
.simulation-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-bg-base, #f6f3ee);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 8px;
  padding: 0.75rem 1rem;
  gap: 0.5rem;
}

.sim-item {
  display: flex;
  flex-direction: column;
}

.sim-lbl {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-text-subtle, #998c7e);
}

.sim-val {
  font-size: 1rem;
  font-weight: 800;
  color: var(--color-text-main, #2b2520);
}

.sim-operator {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--color-text-muted, #6f6356);
}

/* Cuentas Bancarias Grid */
.bank-accounts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
}

@media (max-width: 768px) {
  .bank-accounts-grid {
    grid-template-columns: 1fr;
  }
}

.bank-account-card {
  background: var(--color-bg-base, #f6f3ee);
  border: 2px solid var(--color-border, #e4dacb);
  border-radius: 8px;
  padding: 0.85rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  transition: all 0.2s ease;
}

.bank-account-card:hover {
  border-color: var(--color-primary, #352f2a);
}

.bank-account-card.selected {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(214, 180, 118, 0.2);
}

.bank-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bank-badge-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.bank-logo-text {
  font-size: 0.85rem;
  font-weight: 800;
  color: var(--color-primary, #352f2a);
}

.bank-curr-pill {
  font-size: 0.65rem;
  font-weight: 800;
  background: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.bank-card-numbers {
  font-size: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.acc-row {
  display: flex;
  justify-content: space-between;
}

.acc-row .lbl { color: var(--color-text-subtle, #998c7e); }
.acc-row .val { font-weight: 700; color: var(--color-text-main, #2b2520); }

.bank-titular {
  font-size: 0.65rem;
  color: var(--color-text-subtle, #998c7e);
  border-top: 1px dashed var(--color-border, #e4dacb);
  padding-top: 0.35rem;
}

/* Campos de Formulario */
.form-fields-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

@media (max-width: 640px) {
  .form-fields-grid {
    grid-template-columns: 1fr;
  }
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-field label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
}

.field-label-bold {
  font-size: 0.82rem;
  font-weight: 800;
  color: var(--color-text-main, #2b2520);
  display: block;
  margin-bottom: 0.35rem;
}

.custom-input, .custom-select, .custom-textarea {
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.65rem 0.85rem;
  font-size: 0.9rem;
  color: var(--color-text-main, #2b2520);
  background: var(--color-bg-surface, #ffffff);
  width: 100%;
}

.custom-input:focus, .custom-select:focus, .custom-textarea:focus {
  outline: none;
  border-color: var(--color-primary, #352f2a);
  box-shadow: 0 0 0 3px rgba(53, 47, 42, 0.1);
}

/* Dropzone de Archivo */
.file-dropzone {
  border: 2px dashed var(--color-border, #e4dacb);
  border-radius: 8px;
  background: var(--color-bg-base, #f6f3ee);
  padding: 1.25rem;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s ease;
}

.file-dropzone:hover {
  border-color: var(--color-primary, #352f2a);
  background: var(--color-bg-subtle, #f2ebe1);
}

.file-dropzone.file-attached {
  border-style: solid;
  border-color: var(--color-success-border, #bce5c6);
  background: var(--color-success-bg, #eaf6ed);
}

.file-input-hidden {
  display: none;
}

.dropzone-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.dropzone-icon {
  font-size: 1.75rem;
  color: var(--color-accent-kicker, #9a7f60);
}

.dropzone-texts .primary-text {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
  display: block;
}

.dropzone-texts .sub-text {
  font-size: 0.72rem;
  color: var(--color-text-muted, #6f6356);
}

.file-preview-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  justify-content: space-between;
}

.file-icon-box {
  font-size: 1.5rem;
}

.file-info-details {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  flex: 1;
}

.file-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
}

.file-size {
  font-size: 0.72rem;
  color: var(--color-text-muted, #6f6356);
}

.btn-remove-file {
  background: none;
  border: none;
  color: var(--color-text-muted, #6f6356);
  cursor: pointer;
  font-size: 1rem;
  padding: 0.25rem;
}

.btn-remove-file:hover {
  color: var(--color-danger, #b3261e);
}

/* Botones de Pie */
.form-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  border-top: 1px solid var(--color-border, #e4dacb);
  padding-top: 1.25rem;
}

.btn-primary {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: none;
  border-radius: 6px;
  padding: 0.75rem 1.5rem;
  font-weight: 700;
  font-size: 0.95rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: transparent;
  color: var(--color-text-muted, #6f6356);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  padding: 0.75rem 1.25rem;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary:hover {
  background: var(--color-bg-base, #f6f3ee);
  color: var(--color-text-main, #2b2520);
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 10px;
  gap: 1rem;
  text-align: center;
}

.success-icon {
  font-size: 2.5rem;
  color: var(--color-success-text, #1e6638);
}
</style>
