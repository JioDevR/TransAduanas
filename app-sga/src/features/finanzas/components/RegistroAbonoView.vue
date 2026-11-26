<template>
  <div class="sga-abono-view fade-in">
    <!-- 1. Cabecera Directa (sga-forms.css) -->
    <header class="sga-form-header">
      <div class="sga-abono-title-group">
        <Button
          type="button"
          label="Volver"
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          size="small"
          :disabled="loading"
          @click="$emit('cancel')"
        />
        <h2 class="sga-form-title">
          {{ activeCotizacion ? `Abono · ${activeCotizacion.codigoCotizacion}` : 'Registrar Abono / Anticipo' }}
        </h2>
      </div>

      <div class="sga-form-actions">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          :disabled="loading"
          @click="$emit('cancel')"
        />
        <Button
          type="button"
          label="Registrar y Enviar a Finanzas"
          icon="pi pi-send"
          size="small"
          :loading="loading"
          :disabled="!isFormValid || loading"
          @click="handleSubmit"
        />
      </div>
    </header>

    <Message v-if="errorMessage" severity="error" @close="errorMessage = ''">
      {{ errorMessage }}
    </Message>

    <div class="sga-banner is-info">
      <i class="pi pi-shield sga-banner-icon"></i>
      <div>
        <strong>Validación Previa Requerida:</strong> Una vez aprobado el comprobante bancario (mínimo 30% de cobertura),
        el sistema desbloqueará automáticamente la Apertura de Expediente Aduanero.
      </div>
    </div>

    <!-- 2. Layout Split-Screen (sga-forms.css) -->
    <div class="sga-split-layout">
      <form class="sga-split-main" @submit.prevent="handleSubmit">
        <!-- BLOQUE 01: COTIZACIÓN COMERCIAL -->
        <section class="sga-card-surface sga-form-block">
          <div class="sga-section-head">
            <span class="sga-step-num font-mono">01</span>
            <div class="sga-section-head-copy">
              <h3 class="sga-section-title">Cotización Comercial Vinculada</h3>
              <p class="sga-section-desc">
                Selección de orden aprobada y verificación del saldo pendiente de cobranza.
              </p>
            </div>
            <Button
              v-if="activeCotizacion"
              type="button"
              :label="cambiarCotizacionActiva ? 'Mantener' : 'Cambiar'"
              icon="pi pi-sync"
              severity="secondary"
              outlined
              size="small"
              @click="toggleCambiarCotizacion"
            />
          </div>

          <div v-if="!activeCotizacion || cambiarCotizacionActiva" class="sga-form-field">
            <label for="f-abono-cotizacion" class="sga-form-label">Cotización Aprobada por Cobrar *</label>
            <Select
              inputId="f-abono-cotizacion"
              v-model="selectedCotizacionId"
              :options="cotizacionOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Seleccione una cotización aprobada..."
              filter
              size="small"
              fluid
              :loading="loadingCotizaciones"
              @change="onCotizacionSelected"
            />
          </div>

          <div v-if="activeCotizacion" class="sga-cot-hero">
            <div class="sga-cot-hero-top">
              <div>
                <span class="sga-kicker">COTIZACIÓN APROBADA</span>
                <div class="sga-cot-hero-code font-mono">{{ activeCotizacion.codigoCotizacion }}</div>
                <div class="sga-cell-primary">{{ activeCotizacion.clienteRazonSocial || 'Cliente Importador' }}</div>
                <div class="sga-cell-sub font-mono">RUC/Doc: {{ activeCotizacion.clienteNumeroDocumento || '—' }}</div>
              </div>
              <Tag :value="formatRegimen(activeCotizacion.regimenAduanero)" severity="secondary" rounded />
            </div>

            <div class="sga-grid-3 sga-cot-hero-metrics">
              <div class="sga-meta-item">
                <span class="sga-meta-label">TOTAL COTIZADO</span>
                <span class="sga-total-amount font-mono">{{ formatUsd(balance.totalCotizado) }}</span>
              </div>
              <div class="sga-meta-item">
                <span class="sga-meta-label">ABONADO</span>
                <span class="sga-total-amount font-mono sga-text-success">{{ formatUsd(balance.totalAbonado) }}</span>
              </div>
              <div class="sga-meta-item">
                <span class="sga-meta-label">SALDO PENDIENTE</span>
                <span class="sga-total-amount font-mono sga-text-warn">{{ formatUsd(balance.saldoActual) }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 02: MODALIDAD & MONTO -->
        <section class="sga-card-surface sga-form-block">
          <div class="sga-section-head">
            <span class="sga-step-num font-mono">02</span>
            <div>
              <h3 class="sga-section-title">Modalidad de Abono y Monto Transferido</h3>
              <p class="sga-section-desc">
                Condición de cobro (anticipo de apertura, pago total o afianzamiento) e importe.
              </p>
            </div>
          </div>

          <div class="sga-grid-3">
            <button
              v-for="mod in MODALIDADES"
              :key="mod.value"
              type="button"
              :class="['sga-modality-card', { 'is-active': form.tipoAbono === mod.value }]"
              @click="setTipoAbono(mod.value)"
            >
              <div class="sga-modality-top">
                <i :class="mod.icon"></i>
                <Tag :value="mod.badge" :severity="form.tipoAbono === mod.value ? 'primary' : 'secondary'" rounded />
              </div>
              <strong class="sga-modality-title">{{ mod.title }}</strong>
              <span class="sga-cell-sub">{{ mod.desc }}</span>
            </button>
          </div>

          <div v-if="form.tipoAbono === 'ADELANTO' && activeCotizacion" class="sga-quick-pct-bar">
            <span class="sga-form-label">Sugerido rápido:</span>
            <Button
              type="button"
              :label="`30% Mín (${formatUsd(calcularMontoSugerido(0.3))})`"
              severity="primary"
              outlined
              size="small"
              @click="aplicarPorcentaje(0.3)"
            />
            <Button
              type="button"
              :label="`50% (${formatUsd(calcularMontoSugerido(0.5))})`"
              severity="secondary"
              outlined
              size="small"
              @click="aplicarPorcentaje(0.5)"
            />
            <Button
              type="button"
              label="100% Saldo"
              severity="secondary"
              outlined
              size="small"
              @click="aplicarPorcentaje(1)"
            />
          </div>

          <div class="sga-grid-2">
            <div class="sga-form-field">
              <label for="f-pago-moneda" class="sga-form-label">Moneda de Pago *</label>
              <Select
                inputId="f-pago-moneda"
                v-model="form.moneda"
                :options="MONEDA_OPTIONS"
                optionLabel="label"
                optionValue="value"
                size="small"
                fluid
                :disabled="loading"
              />
            </div>

            <div class="sga-form-field">
              <label for="f-pago-monto" class="sga-form-label">Monto Abonado *</label>
              <InputNumber
                inputId="f-pago-monto"
                v-model="form.montoAbonado"
                mode="currency"
                :currency="form.moneda"
                :locale="form.moneda === 'PEN' ? 'es-PE' : 'en-US'"
                :min="0.01"
                :minFractionDigits="2"
                :maxFractionDigits="2"
                size="small"
                fluid
                :disabled="loading"
              />
            </div>

            <div class="sga-form-field">
              <label for="f-pago-tc" class="sga-form-label">Tipo de Cambio Oficial (S/)</label>
              <InputNumber
                inputId="f-pago-tc"
                v-model="form.tipoCambio"
                :min="1"
                :minFractionDigits="2"
                :maxFractionDigits="4"
                size="small"
                fluid
                :disabled="loading"
              />
            </div>

            <div class="sga-form-field">
              <span class="sga-form-label">Equivalente en Soles (PEN)</span>
              <div class="sga-equiv-box font-mono">
                {{ formatPen(montoEquivalentePen) }}
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 03: DATOS BANCARIOS -->
        <section class="sga-card-surface sga-form-block">
          <div class="sga-section-head">
            <span class="sga-step-num font-mono">03</span>
            <div>
              <h3 class="sga-section-title">Datos de la Transacción Bancaria</h3>
              <p class="sga-section-desc">
                Trazabilidad oficial del abono en cuentas recaudadoras autorizadas de TransAduanas.
              </p>
            </div>
          </div>

          <div class="sga-grid-2">
            <div class="sga-form-field">
              <label for="f-pago-metodo" class="sga-form-label">Método de Pago *</label>
              <Select
                inputId="f-pago-metodo"
                v-model="form.metodoPago"
                :options="METODOS_PAGO_COTIZACION"
                optionLabel="label"
                optionValue="value"
                size="small"
                fluid
                :disabled="loading"
              />
            </div>

            <div class="sga-form-field">
              <label for="f-pago-banco-dest" class="sga-form-label">Cuenta Bancaria Receptora (SGA) *</label>
              <Select
                inputId="f-pago-banco-dest"
                v-model="form.bancoDestino"
                :options="CUENTAS_RECAUDADORAS_SGA"
                optionLabel="label"
                optionValue="value"
                size="small"
                fluid
                :disabled="loading"
              />
            </div>

            <div class="sga-form-field">
              <label for="f-pago-banco-orig" class="sga-form-label">Banco Emisor del Cliente</label>
              <InputText
                id="f-pago-banco-orig"
                v-model.trim="form.bancoOrigen"
                placeholder="Ej. BCP, BBVA, Interbank..."
                maxlength="80"
                size="small"
                fluid
                :disabled="loading"
              />
            </div>

            <div class="sga-form-field">
              <label for="f-pago-operacion" class="sga-form-label">N° de Operación Bancaria *</label>
              <InputText
                id="f-pago-operacion"
                v-model.trim="form.numeroOperacion"
                placeholder="Ej. OP-4829104"
                maxlength="60"
                size="small"
                class="font-mono"
                fluid
                :disabled="loading"
              />
            </div>

            <div class="sga-form-field">
              <label for="f-pago-fecha" class="sga-form-label">Fecha de Depósito / Transferencia *</label>
              <InputText
                id="f-pago-fecha"
                v-model="form.fechaOperacion"
                type="date"
                size="small"
                class="font-mono"
                fluid
                :disabled="loading"
              />
            </div>

            <div class="sga-form-field">
              <span class="sga-form-label">Estado Inicial en Flujo</span>
              <div class="sga-equiv-box">
                <Tag value="PENDIENTE DE REVISIÓN" severity="warn" rounded />
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 04: COMPROBANTE & OBSERVACIONES -->
        <section class="sga-card-surface sga-form-block">
          <div class="sga-section-head">
            <span class="sga-step-num font-mono">04</span>
            <div>
              <h3 class="sga-section-title">Comprobante Digital y Observaciones</h3>
              <p class="sga-section-desc">
                Adjunte el voucher o constancia de transferencia (PDF, PNG o JPG hasta 10 MB).
              </p>
            </div>
          </div>

          <div
            :class="['sga-dropzone', { 'has-file': Boolean(selectedFile), 'is-dragging': isDragging }]"
            @click="triggerFileInput"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="onFileDrop"
          >
            <input
              ref="fileInputRef"
              type="file"
              accept=".pdf,image/png,image/jpeg,image/jpg"
              class="sga-hidden-input"
              @change="onFileSelected"
            />

            <div v-if="!selectedFile" class="sga-dropzone-empty">
              <i class="pi pi-cloud-upload sga-dropzone-icon"></i>
              <strong>Haga clic o arrastre el voucher bancario aquí</strong>
              <span class="sga-cell-sub">Formatos permitidos: PDF, PNG, JPG (Máx. 10 MB)</span>
            </div>

            <div v-else class="sga-dropzone-selected" @click.stop>
              <div class="sga-file-meta">
                <i :class="selectedFile.type.includes('pdf') ? 'pi pi-file-pdf' : 'pi pi-image'" class="sga-file-icon"></i>
                <div>
                  <strong class="font-mono">{{ selectedFile.name }}</strong>
                  <div class="sga-cell-sub font-mono">{{ formatFileSize(selectedFile.size) }}</div>
                </div>
              </div>
              <div class="sga-toolbar-actions">
                <Button
                  type="button"
                  label="Cambiar"
                  icon="pi pi-pencil"
                  severity="secondary"
                  outlined
                  size="small"
                  @click.stop="triggerFileInput"
                />
                <Button
                  type="button"
                  icon="pi pi-trash"
                  severity="danger"
                  outlined
                  size="small"
                  title="Quitar archivo"
                  @click.stop="clearFile"
                />
              </div>
            </div>
          </div>

          <div class="sga-form-field">
            <label for="f-pago-obs" class="sga-form-label">Observaciones / Glosa Contable</label>
            <Textarea
              id="f-pago-obs"
              v-model="form.observaciones"
              rows="3"
              fluid
              placeholder="Notas adicionales de cuenta origen, cheque o instrucciones de tesorería..."
              :disabled="loading"
            />
          </div>
        </section>
      </form>

      <!-- ASIDE STICKY: BALANCE PROYECTADO & CHECKLIST -->
      <aside class="sga-split-aside">
        <div class="sga-aside-stack">
          <section class="sga-card-surface sga-aside-card">
            <span class="sga-kicker">BALANCE PROYECTADO</span>
            <h3 class="sga-aside-title font-mono">
              {{ activeCotizacion ? activeCotizacion.codigoCotizacion : 'SIN SELECCIÓN' }}
            </h3>
            <p class="sga-cell-sub">
              {{ activeCotizacion ? activeCotizacion.clienteRazonSocial : 'Seleccione una cotización aprobada' }}
            </p>

            <div class="sga-aside-rows">
              <div class="sga-aside-row">
                <span>Total Cotizado</span>
                <strong class="font-mono">{{ formatUsd(balance.totalCotizado) }}</strong>
              </div>
              <div class="sga-aside-row">
                <span>Abonos Aprobados</span>
                <span class="font-mono">{{ formatUsd(balance.totalAbonado) }}</span>
              </div>
              <div class="sga-aside-row">
                <span>Saldo Actual</span>
                <span class="font-mono sga-text-warn">{{ formatUsd(balance.saldoActual) }}</span>
              </div>

              <hr class="sga-aside-divider" />

              <div class="sga-aside-row is-highlight">
                <span>Nuevo Abono</span>
                <strong class="font-mono">
                  + {{ form.moneda === 'PEN' ? formatPen(form.montoAbonado) : formatUsd(form.montoAbonado) }}
                </strong>
              </div>
              <div v-if="form.moneda === 'PEN' && form.montoAbonado" class="sga-aside-row">
                <span class="sga-cell-sub">Equivalente USD</span>
                <span class="font-mono sga-cell-sub">{{ formatUsd(balance.nuevoAbonoUsd) }}</span>
              </div>
              <div class="sga-aside-row">
                <span>Saldo Proyectado</span>
                <strong
                  class="font-mono"
                  :class="balance.saldoProyectado <= 0 ? 'sga-text-success' : 'sga-text-warn'"
                >
                  {{ formatUsd(balance.saldoProyectado) }}
                </strong>
              </div>
            </div>

            <div class="sga-coverage-box">
              <div class="sga-aside-row">
                <span class="sga-form-label">Cobertura Proyectada</span>
                <strong
                  class="font-mono"
                  :class="balance.cumpleUmbralApertura ? 'sga-text-success' : 'sga-text-warn'"
                >
                  {{ balance.coberturaProyectadaPct }}%
                </strong>
              </div>
              <ProgressBar :value="balance.coberturaProyectadaPct" :showValue="false" class="sga-cov-progress" />
            </div>

            <div :class="['sga-banner', balance.cumpleUmbralApertura ? 'is-success' : 'is-warn']">
              <i :class="[balance.cumpleUmbralApertura ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle', 'sga-banner-icon']"></i>
              <div>
                <strong>
                  {{ balance.cumpleUmbralApertura ? 'Cumple Umbral de Apertura (30%)' : 'Anticipo Menor al 30% Mínimo' }}
                </strong>
                <div>
                  {{
                    balance.cumpleUmbralApertura
                      ? 'Al aprobarse este comprobante se habilitará la Apertura de Expediente.'
                      : 'Cubrirá parte del saldo pero requerirá completar el 30% o autorización.'
                  }}
                </div>
              </div>
            </div>
          </section>

          <section class="sga-card-surface sga-aside-card">
            <span class="sga-kicker">VERIFICACIÓN</span>
            <h4 class="sga-aside-subtitle">Requisitos de Envío</h4>
            <ul class="sga-checklist">
              <li v-for="item in checklistItems" :key="item.label" class="sga-check-item">
                <i
                  :class="[
                    item.done ? 'pi pi-check-circle sga-text-success' : item.required ? 'pi pi-circle sga-text-muted' : 'pi pi-info-circle sga-text-info'
                  ]"
                ></i>
                <span :class="{ 'sga-check-done': item.done }">{{ item.label }}</span>
              </li>
            </ul>
          </section>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, toRef, onMounted } from 'vue'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import ProgressBar from 'primevue/progressbar'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { formatRegimen } from '@/shared/utils/formatters'
import { useRegistroAbonoForm } from '../composables/useRegistroAbonoForm'
import {
  CUENTAS_RECAUDADORAS_SGA,
  METODOS_PAGO_COTIZACION,
  formatPen,
  formatUsd
} from '../utils/finanzasFormatters'

const props = defineProps({
  cotizacion: { type: Object, default: null },
  cotizaciones: { type: Array, default: () => [] }
})

const emit = defineEmits(['cancel', 'saved'])

const MODALIDADES = [
  {
    value: 'ADELANTO',
    title: 'Adelanto / Anticipo',
    desc: 'Abono parcial para apertura de expediente.',
    badge: '30% MÍN',
    icon: 'pi pi-percentage'
  },
  {
    value: 'PAGO_TOTAL',
    title: 'Pago Total (100%)',
    desc: 'Cancelación total del saldo comercial.',
    badge: '100%',
    icon: 'pi pi-check-circle'
  },
  {
    value: 'GARANTIA_CREDITO',
    title: 'Línea / Carta Fianza',
    desc: 'Respaldo financiero o crédito otorgado.',
    badge: 'AFIANZADO',
    icon: 'pi pi-briefcase'
  }
]

const MONEDA_OPTIONS = [
  { label: 'Dólares Americanos (USD $)', value: 'USD' },
  { label: 'Soles Peruanos (PEN S/)', value: 'PEN' }
]

const fileInputRef = ref(null)
const isDragging = ref(false)

const {
  loading,
  loadingCotizaciones,
  errorMessage,
  selectedFile,
  selectedCotizacionId,
  cotizacionOptions,
  cambiarCotizacionActiva,
  activeCotizacion,
  form,
  balance,
  montoEquivalentePen,
  isFormValid,
  checklistItems,
  init,
  setTipoAbono,
  calcularMontoSugerido,
  aplicarPorcentaje,
  toggleCambiarCotizacion,
  onCotizacionSelected,
  processSelectedFile,
  clearSelectedFile,
  handleSubmit
} = useRegistroAbonoForm({
  cotizacionProp: toRef(props, 'cotizacion'),
  cotizacionesProp: toRef(props, 'cotizaciones'),
  onSaved: (payload) => emit('saved', payload)
})

onMounted(() => {
  init()
})

function triggerFileInput() {
  fileInputRef.value?.click()
}

function onFileSelected(event) {
  processSelectedFile(event.target.files?.[0])
}

function onFileDrop(event) {
  isDragging.value = false
  processSelectedFile(event.dataTransfer?.files?.[0])
}

function clearFile() {
  clearSelectedFile()
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`
}
</script>

<style scoped>
.sga-abono-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-abono-title-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.sga-form-block {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.sga-section-head {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.sga-section-head-copy {
  flex: 1;
  min-width: 0;
}

.sga-step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  flex-shrink: 0;
}

.sga-section-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sga-section-desc {
  margin: 0.15rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.sga-cot-hero {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  padding: 1rem 1.15rem;
  border-radius: 12px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.sga-cot-hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.sga-cot-hero-code {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-primary);
}

.sga-cot-hero-metrics {
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}

.sga-modality-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.35rem;
  padding: 0.85rem 1rem;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-base);
  color: var(--color-text-main);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}

.sga-modality-card:hover {
  border-color: var(--color-border-strong);
}

.sga-modality-card.is-active {
  border-color: var(--color-primary);
  background: var(--color-bg-subtle);
}

.sga-modality-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  color: var(--color-primary);
}

.sga-modality-title {
  font-size: 0.875rem;
  font-weight: 600;
}

.sga-quick-pct-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.sga-equiv-box {
  display: flex;
  align-items: center;
  min-height: 38px;
  padding: 0.45rem 0.75rem;
  border-radius: 10px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  font-weight: 600;
  color: var(--color-text-main);
}

.sga-dropzone {
  border: 1.5px dashed var(--color-border-strong);
  border-radius: 12px;
  padding: 1.5rem 1.25rem;
  background: var(--color-bg-base);
  cursor: pointer;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}

.sga-dropzone:hover,
.sga-dropzone.is-dragging {
  border-color: var(--color-primary);
  background: var(--color-bg-subtle);
}

.sga-hidden-input {
  display: none;
}

.sga-dropzone-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  text-align: center;
}

.sga-dropzone-icon {
  font-size: 1.75rem;
  color: var(--color-primary);
}

.sga-dropzone-selected {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.sga-file-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sga-file-icon {
  font-size: 1.5rem;
  color: var(--color-primary);
}

.sga-aside-stack {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sga-aside-card {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.sga-aside-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-primary);
}

.sga-aside-subtitle {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sga-aside-rows {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  font-size: 0.84rem;
}

.sga-aside-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.5rem;
}

.sga-aside-row.is-highlight {
  color: var(--color-primary);
  font-weight: 600;
}

.sga-aside-divider {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 0.25rem 0;
}

.sga-coverage-box {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.sga-cov-progress {
  height: 7px;
}

.sga-checklist {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  font-size: 0.82rem;
}

.sga-check-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-text-muted);
}

.sga-check-done {
  color: var(--color-text-main);
  font-weight: 500;
}

.sga-banner-icon {
  font-size: 1.1rem;
  margin-top: 0.1rem;
  flex-shrink: 0;
}
</style>
