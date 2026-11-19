<template>
  <div class="sga-apertura-view">
    <header class="sga-apertura-header">
      <div class="sga-apertura-title-group">
        <h1 class="sga-apertura-title">
          Aperturar Expediente
          <span v-if="cotizacion?.codigoCotizacion" class="font-mono sga-apertura-code">
            · {{ cotizacion.codigoCotizacion }}
          </span>
        </h1>
      </div>

      <div class="sga-apertura-actions">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          :disabled="submitting"
          @click="handleCancel"
        />

        <Button
          type="button"
          label="Formalizar y Aperturar Expediente"
          icon="pi pi-folder-open"
          size="small"
          :loading="submitting"
          :disabled="!isFormReady"
          @click="handleSubmit"
        />
      </div>
    </header>

    <Message v-if="errorMessage" severity="error" @close="errorMessage = ''">
      {{ errorMessage }}
    </Message>

    <Message v-else-if="!loading && eligibilityWarning" severity="warn" :closable="false">
      {{ eligibilityWarning }}
    </Message>

    <div v-if="loading" class="sga-card-surface sga-apertura-loading">
      <i class="pi pi-spin pi-spinner sga-loading-icon"></i>
      <span>Cargando cotización y especialistas acreditados...</span>
    </div>

    <div v-else-if="cotizacion" class="sga-apertura-grid">
      <form class="sga-apertura-main" @submit.prevent="handleSubmit">
        <section class="sga-card-surface sga-form-section">
          <div class="sga-section-head">
            <span class="sga-step-num font-mono">01</span>
            <div>
              <h2 class="sga-section-title">Especialista Asignado & Contacto Exterior</h2>
              <p class="sga-section-desc">
                Agente aduanero que liderará el despacho ante SUNAT y enlace con el embarcador/proveedor.
              </p>
            </div>
          </div>

          <div class="sga-form-grid">
            <div class="sga-field sga-field-full">
              <label for="exp-agente" class="sga-field-label">AGENTE ADUANERO RESPONSABLE *</label>
              <Select
                inputId="exp-agente"
                v-model="form.idAgenteAduanero"
                :options="agentesOptions"
                optionLabel="label"
                optionValue="idUsuario"
                placeholder="Seleccione un Agente Aduanero Acreditado"
                filter
                size="small"
                fluid
              />
              <small class="sga-field-hint">
                Especialista acreditado que gestionará la numeración de DAM, aforo e inspecciones.
              </small>
            </div>

            <div class="sga-field">
              <label for="exp-contacto-nombre" class="sga-field-label">CONTACTO EN EL EXTERIOR</label>
              <InputText
                id="exp-contacto-nombre"
                v-model.trim="form.contactoExteriorNombre"
                placeholder="Ej. Shanghai Logistics Co. / Hans Gruber"
                maxlength="150"
                size="small"
                fluid
              />
            </div>

            <div class="sga-field">
              <label for="exp-contacto-tel" class="sga-field-label">TELÉFONO / WHATSAPP INTERNACIONAL</label>
              <InputText
                id="exp-contacto-tel"
                v-model.trim="form.contactoExteriorTelefono"
                placeholder="Ej. +86 21 5555 1234"
                maxlength="30"
                size="small"
                class="font-mono"
                fluid
              />
            </div>

            <div class="sga-field sga-field-full">
              <label for="exp-contacto-correo" class="sga-field-label">CORREO ELECTRÓNICO DE CONTACTO</label>
              <InputText
                id="exp-contacto-correo"
                v-model.trim="form.contactoExteriorCorreo"
                type="email"
                placeholder="Ej. operations@shanghailogistics.com"
                maxlength="100"
                size="small"
                class="font-mono"
                fluid
              />
            </div>
          </div>
        </section>

        <section class="sga-card-surface sga-form-section">
          <div class="sga-section-head">
            <span class="sga-step-num font-mono">02</span>
            <div>
              <h2 class="sga-section-title">Tránsito Internacional & Datos de Manifiesto</h2>
              <p class="sga-section-desc">
                Fechas estimadas y datos de contenedor o precinto (pueden completarse o rectificarse durante el tránsito).
              </p>
            </div>
          </div>

          <div class="sga-form-grid">
            <div class="sga-field">
              <label for="exp-etd" class="sga-field-label">FECHA ESTIMADA DE ZARPE (ETD)</label>
              <InputText
                id="exp-etd"
                v-model="form.fechaZarpeEtd"
                type="date"
                size="small"
                class="font-mono"
                fluid
              />
            </div>

            <div class="sga-field">
              <label for="exp-eta" class="sga-field-label">FECHA ESTIMADA DE LLEGADA (ETA)</label>
              <InputText
                id="exp-eta"
                v-model="form.fechaLlegadaEta"
                type="date"
                size="small"
                class="font-mono"
                fluid
              />
            </div>

            <div class="sga-field sga-field-full">
              <label for="exp-nave" class="sga-field-label">NAVE / EMBARCACIÓN / N° VUELO</label>
              <InputText
                id="exp-nave"
                v-model.trim="form.naveViaje"
                placeholder="Ej. MSC Gülsün / Vuelo Latam Cargo LA-770"
                maxlength="100"
                size="small"
                fluid
              />
            </div>

            <div class="sga-field">
              <label for="exp-contenedor" class="sga-field-label">N° CONTENEDOR (OPCIONAL)</label>
              <InputText
                id="exp-contenedor"
                v-model.trim="form.numeroContenedor"
                placeholder="Ej. MSCU-928472-1"
                maxlength="30"
                size="small"
                class="font-mono sga-uppercase"
                fluid
              />
              <small class="sga-field-hint">Actualizable en tránsito o llegada a depósito.</small>
            </div>

            <div class="sga-field">
              <label for="exp-precinto" class="sga-field-label">N° PRECINTO ADUANERO (OPCIONAL)</label>
              <InputText
                id="exp-precinto"
                v-model.trim="form.precintoAduanero"
                placeholder="Ej. ML-PE-992147"
                maxlength="50"
                size="small"
                class="font-mono sga-uppercase"
                fluid
              />
              <small class="sga-field-hint">Actualizable en tránsito o llegada a depósito.</small>
            </div>

            <div class="sga-field sga-field-full">
              <label for="exp-obs" class="sga-field-label">INSTRUCCIONES OPERATIVAS / OBSERVACIONES</label>
              <Textarea
                id="exp-obs"
                v-model.trim="form.observaciones"
                rows="3"
                placeholder="Anotaciones sobre depósito temporal asignado, requerimiento de inspección previa o prioridad de despacho..."
                size="small"
                fluid
              />
            </div>
          </div>
        </section>
      </form>

      <aside class="sga-apertura-aside">
        <div class="sga-card-surface sga-summary-card">
          <div class="sga-summary-top">
            <span class="sga-summary-kicker">COTIZACIÓN ORIGEN</span>
            <Tag
              :value="isCotizacionElegible ? 'Lista para Apertura' : (cotizacion.estado || 'No Elegible')"
              :severity="isCotizacionElegible ? 'success' : 'warn'"
              rounded
            />
          </div>

          <div class="sga-summary-code font-mono">{{ cotizacion.codigoCotizacion }}</div>

          <div class="sga-summary-rows">
            <div class="sga-summary-row">
              <span class="sga-summary-label">CLIENTE</span>
              <strong class="sga-summary-val">{{ cotizacion.clienteRazonSocial || '—' }}</strong>
              <span class="sga-summary-sub font-mono">RUC/Doc: {{ cotizacion.clienteNumeroDocumento || '—' }}</span>
            </div>

            <div class="sga-summary-row">
              <span class="sga-summary-label">RÉGIMEN ADUANERO</span>
              <span class="sga-summary-val">{{ formatRegimenShort(cotizacion.regimenAduanero) }}</span>
            </div>

            <div class="sga-summary-row">
              <span class="sga-summary-label">RUTA / PUERTOS</span>
              <span class="sga-summary-val">
                {{ cotizacion.puertoEnvioNombre || 'Origen' }} → {{ cotizacion.puertoDestinoNombre || 'Destino' }}
              </span>
            </div>

            <div class="sga-summary-row">
              <span class="sga-summary-label">AGENTE ASIGNADO</span>
              <span class="sga-summary-val">
                {{ selectedAgente ? `${selectedAgente.nombres} ${selectedAgente.apellidos}` : 'Por seleccionar' }}
              </span>
            </div>
          </div>

          <div class="sga-summary-totals">
            <div class="sga-total-line">
              <span>Total Cotizado</span>
              <strong class="font-mono">${{ formatUsd(cotizacion.totalCotizado) }} USD</strong>
            </div>
            <div class="sga-total-line sga-text-success">
              <span>Abono Finanzas</span>
              <strong class="font-mono">${{ formatUsd(cotizacion.totalAbonado) }} USD</strong>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useAperturaExpedienteForm } from '../composables/useAperturaExpedienteForm'
import { formatUsd, formatRegimenShort } from '../utils/aduanasFormatters'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { showSuccess } = useNotification()

const {
  loading,
  submitting,
  errorMessage,
  cotizacion,
  agentesOptions,
  selectedAgente,
  form,
  isCotizacionElegible,
  eligibilityWarning,
  isFormReady,
  loadData,
  submit
} = useAperturaExpedienteForm({ authStore })

onMounted(() => {
  const idCotizacion = route.params.idCotizacion
  if (idCotizacion) {
    loadData(idCotizacion)
  } else {
    errorMessage.value = 'No se especificó la cotización para aperturar expediente.'
  }
})

const handleCancel = () => {
  router.push({ path: '/gestion-aduanera', query: { tab: 'apertura' } })
}

const handleSubmit = async () => {
  const creado = await submit()
  if (creado) {
    showSuccess(`¡Expediente ${creado.codigoExpediente} aperturado exitosamente!`)
    router.push({ path: '/gestion-aduanera', query: { tab: 'expedientes' } })
  }
}
</script>

<style scoped>
.sga-apertura-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sga-apertura-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.sga-apertura-title {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-main);
}

.sga-apertura-code {
  color: var(--p-primary-color);
}

.sga-apertura-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sga-apertura-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  min-height: 220px;
  color: var(--color-text-subtle);
}

.sga-loading-icon {
  font-size: 1.25rem;
}

.sga-apertura-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 1120px) {
  .sga-apertura-grid {
    grid-template-columns: 1fr;
  }
}

.sga-apertura-main {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sga-form-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-section-head {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.sga-step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 9999px;
  background: var(--p-highlight-background);
  color: var(--p-highlight-color);
  font-size: 0.75rem;
  font-weight: 700;
}

.sga-section-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: var(--color-text-main);
}

.sga-section-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
}

.sga-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.125rem;
}

@media (max-width: 768px) {
  .sga-form-grid {
    grid-template-columns: 1fr;
  }
}

.sga-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.sga-field-full {
  grid-column: 1 / -1;
}

.sga-field-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-text-muted);
}

.sga-field-hint {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-uppercase {
  text-transform: uppercase;
}

.sga-apertura-aside {
  position: sticky;
  top: 1.25rem;
}

.sga-summary-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sga-summary-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.sga-summary-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
}

.sga-summary-code {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sga-summary-rows {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding: 0.85rem 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}

.sga-summary-row {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.sga-summary-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-text-muted);
}

.sga-summary-val {
  font-size: 0.875rem;
  color: var(--color-text-main);
}

.sga-summary-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-summary-totals {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sga-total-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.875rem;
  color: var(--color-text-main);
}
</style>
