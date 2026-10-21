<template>
  <div class="cliente-registration-form-view">
    <!-- 1. Header Bar de Navegación y Acciones -->
    <div class="form-header-bar">
      <div class="header-titles">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('cancel')"
          title="Regresar al directorio comercial"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver a Clientes & Prospectos</span>
        </button>

        <div class="kicker-row">
          <span class="badge-kicker">CARTERA COMERCIAL & CLIENTES</span>
          <span :class="['mode-pill', isEditMode ? 'mode-edit' : 'mode-create']">
            <i :class="isEditMode ? 'pi pi-pencil' : 'pi pi-plus'"></i>
            <span>{{ isEditMode ? 'MODO EDICIÓN' : 'MODO REGISTRO' }}</span>
          </span>
        </div>

        <h1 class="form-main-title">
          {{ isEditMode ? `Editar Cliente: ${form.razonSocial}` : 'Registrar Nuevo Cliente o Prospecto' }}
        </h1>
        <p class="form-main-desc">
          Complete la información tributaria oficial (RUC/DNI), datos del representante legal y canales de contacto para la emisión de cotizaciones y despachos aduaneros.
        </p>
      </div>

      <div class="header-actions">
        <button
          type="button"
          class="btn-outline-warm"
          @click="$emit('cancel')"
          :disabled="submitting"
        >
          <span>Cancelar</span>
        </button>
        <button
          type="button"
          class="btn-espresso"
          @click="handleSubmit"
          :disabled="submitting || !isFormReady"
        >
          <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-check"></i>
          <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Cliente' }}</span>
        </button>
      </div>
    </div>

    <!-- Banner de Error si la validación falla -->
    <div v-if="errorMessage" class="error-banner">
      <i class="pi pi-exclamation-triangle"></i>
      <div class="error-content">
        <strong>Error:</strong>
        <span>{{ errorMessage }}</span>
      </div>
      <button type="button" class="error-close-btn" @click="errorMessage = ''">
        <i class="pi pi-times"></i>
      </button>
    </div>

    <!-- 2. Grid Principal -->
    <div class="form-content-grid">
      <!-- Columna Principal: Bloques de Formulario -->
      <div class="form-blocks-column">
        <!-- BLOQUE 01: IDENTIFICACIÓN TRIBUTARIA Y FISCAL -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Identificación Fiscal y Registro Legal</h3>
              <p class="block-sub">Datos registrados ante SUNAT para personas jurídicas o naturales con negocio.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Tipo de Documento -->
            <div class="field-group">
              <label class="field-label">Tipo de Documento Oficial *</label>
              <div class="doc-types-row">
                <button
                  type="button"
                  :class="['doc-type-btn', { active: form.tipoDocumento === 'RUC' }]"
                  @click="form.tipoDocumento = 'RUC'"
                >
                  <i class="pi pi-building"></i>
                  <span>RUC (Persona Jurídica)</span>
                </button>
                <button
                  type="button"
                  :class="['doc-type-btn', { active: form.tipoDocumento === 'DNI' }]"
                  @click="form.tipoDocumento = 'DNI'"
                >
                  <i class="pi pi-id-card"></i>
                  <span>DNI (Natural)</span>
                </button>
                <button
                  type="button"
                  :class="['doc-type-btn', { active: form.tipoDocumento === 'CE' }]"
                  @click="form.tipoDocumento = 'CE'"
                >
                  <i class="pi pi-globe"></i>
                  <span>Carnet Extranjería</span>
                </button>
                <button
                  type="button"
                  :class="['doc-type-btn', { active: form.tipoDocumento === 'PASAPORTE' }]"
                  @click="form.tipoDocumento = 'PASAPORTE'"
                >
                  <i class="pi pi-book"></i>
                  <span>Pasaporte</span>
                </button>
              </div>
            </div>

            <!-- Número de Documento -->
            <div class="field-group highlight-group">
              <div class="label-with-badge">
                <label for="c-num-doc" class="field-label">Número de Documento ({{ form.tipoDocumento }}) *</label>
                <span class="kicker-mini-badge">{{ getDocValidationHint }}</span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-hashtag input-icon"></i>
                <input
                  id="c-num-doc"
                  v-model.trim="form.numeroDocumento"
                  type="text"
                  :placeholder="form.tipoDocumento === 'RUC' ? 'Ej. 20100070970' : 'Ej. 45678912'"
                  class="form-input doc-num-input"
                  required
                  :maxlength="form.tipoDocumento === 'RUC' ? 11 : 15"
                  :disabled="submitting"
                />
              </div>
              <p class="field-helper-text">
                {{ form.tipoDocumento === 'RUC' ? 'Debe contener exactamente 11 dígitos numéricos registrados en SUNAT.' : 'Ingrese el número del documento de identificación del titular.' }}
              </p>
            </div>

            <!-- Razón Social o Nombre Completo -->
            <div class="field-group">
              <label for="c-razon" class="field-label">Razón Social o Nombre Completo *</label>
              <div class="input-icon-box">
                <i class="pi pi-briefcase input-icon"></i>
                <input
                  id="c-razon"
                  v-model.trim="form.razonSocial"
                  type="text"
                  placeholder="Ej. IMPORTADORA & EXPORTADORA ANDINA S.A.C."
                  class="form-input"
                  required
                  maxlength="150"
                  :disabled="submitting"
                />
              </div>
              <p class="field-helper-text">
                Nombre oficial según ficha RUC o documento legal que figurará en las facturas y DUA aduanera.
              </p>
            </div>
          </div>
        </section>

        <!-- BLOQUE 02: CONTACTO Y DIRECCIÓN FISCAL -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Ubicación Fiscal y Canales de Comunicación</h3>
              <p class="block-sub">Coordenadas del representante comercial para el envío de liquidaciones y notificaciones.</p>
            </div>
          </div>

          <div class="block-body">
            <div class="fields-two-cols">
              <!-- Representante de Contacto -->
              <div class="field-group">
                <label for="c-contacto" class="field-label">Contacto / Representante Comercial</label>
                <div class="input-icon-box">
                  <i class="pi pi-user input-icon"></i>
                  <input
                    id="c-contacto"
                    v-model.trim="form.contactoRepresentante"
                    type="text"
                    placeholder="Ej. Lic. Carlos Mendoza"
                    class="form-input"
                    maxlength="100"
                    :disabled="submitting"
                  />
                </div>
              </div>

              <!-- Teléfono de Contacto -->
              <div class="field-group">
                <label for="c-telefono" class="field-label">Teléfono Móvil o Central</label>
                <div class="input-icon-box">
                  <i class="pi pi-phone input-icon"></i>
                  <input
                    id="c-telefono"
                    v-model.trim="form.telefonoContacto"
                    type="text"
                    placeholder="Ej. +51 987 654 321"
                    class="form-input"
                    maxlength="20"
                    :disabled="submitting"
                  />
                </div>
              </div>
            </div>

            <!-- Correo Institucional -->
            <div class="field-group highlight-group">
              <div class="label-with-badge">
                <label for="c-correo" class="field-label">Correo Electrónico Institucional *</label>
                <span class="kicker-mini-badge">FACTURACIÓN & NOTIFICACIÓN</span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-envelope input-icon"></i>
                <input
                  id="c-correo"
                  v-model.trim="form.correoContacto"
                  type="email"
                  placeholder="Ej. operaciones@empresa.com"
                  class="form-input"
                  required
                  maxlength="100"
                  :disabled="submitting"
                />
              </div>
              <p class="field-helper-text">
                En esta casilla se remitirán las cotizaciones comerciales y avisos de llegada de carga.
              </p>
            </div>

            <!-- Dirección Fiscal -->
            <div class="field-group">
              <label for="c-direccion" class="field-label">Dirección Fiscal / Domicilio Legal</label>
              <div class="input-icon-box">
                <i class="pi pi-map-marker input-icon"></i>
                <input
                  id="c-direccion"
                  v-model.trim="form.direccionFiscal"
                  type="text"
                  placeholder="Ej. Av. Elmer Faucett 2823, Callao"
                  class="form-input"
                  maxlength="200"
                  :disabled="submitting"
                />
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 03: CLASIFICACIÓN COMERCIAL (GESTIÓN AUTOMÁTICA DEL SISTEMA) -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Clasificación en Cartera Comercial (Automática)</h3>
              <p class="block-sub">El estado de la cuenta se gestiona automáticamente por el ciclo de cotización y aprobación.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Si es nuevo registro -->
            <div v-if="!isEditMode" class="classification-auto-banner prospect-banner">
              <div class="auto-banner-icon prospect-icon">
                <i class="pi pi-clock"></i>
              </div>
              <div class="auto-banner-content">
                <div class="auto-banner-header">
                  <h4 class="auto-banner-title">Prospecto Comercial (Lead en Evaluación)</h4>
                  <span class="auto-banner-badge badge-prospect">ASIGNACIÓN INICIAL AUTOMÁTICA</span>
                </div>
                <p class="auto-banner-desc">
                  Todo nuevo registro ingresa al sistema como <strong>Prospecto Comercial</strong>.
                  Una vez emitida y <strong>aprobada su primera cotización aduanera</strong>, el sistema lo promoverá de forma automática a <strong>Cliente Efectivo</strong>.
                </p>
              </div>
            </div>

            <!-- Si es modo edición -->
            <div v-else class="classification-auto-banner" :class="form.esProspecto ? 'prospect-banner' : 'client-banner'">
              <div class="auto-banner-icon" :class="form.esProspecto ? 'prospect-icon' : 'client-icon'">
                <i :class="form.esProspecto ? 'pi pi-clock' : 'pi pi-check-circle'"></i>
              </div>
              <div class="auto-banner-content">
                <div class="auto-banner-header">
                  <h4 class="auto-banner-title">
                    {{ form.esProspecto ? 'Prospecto Comercial (Lead en Evaluación)' : 'Cliente Efectivo (Cuenta Formalizada)' }}
                  </h4>
                  <span :class="['auto-banner-badge', form.esProspecto ? 'badge-prospect' : 'badge-client']">
                    {{ form.esProspecto ? 'EN PROCESO DE FORMALIZACIÓN' : 'CUENTA FORMALIZADA' }}
                  </span>
                </div>
                <p class="auto-banner-desc" v-if="form.esProspecto">
                  Este registro se encuentra clasificado como <strong>Prospecto Comercial</strong>. Se promoverá automáticamente a <strong>Cliente Efectivo</strong> tan pronto como se apruebe una cotización comercial asociada.
                </p>
                <p class="auto-banner-desc" v-else>
                  Esta cuenta ha sido promovida a <strong>Cliente Efectivo</strong> tras la aprobación de propuestas aduaneras. Posee historial comercial formalizado.
                </p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- Columna Lateral Sticky: Previsualización en Vivo de Ficha de Cliente -->
      <aside class="form-sidebar-column">
        <!-- Tarjeta de Ficha de Cliente en Vivo -->
        <div class="preview-credential-card">
          <div class="card-ribbon">
            <i class="pi pi-shield"></i>
            <span>EXPEDIENTE COMERCIAL</span>
          </div>

          <div class="card-avatar-box">
            <div class="avatar-circle">
              <i class="pi pi-building"></i>
            </div>
          </div>

          <div class="card-identity">
            <h4 class="card-client-name">
              {{ form.razonSocial || 'Razón Social de la Empresa' }}
            </h4>
            <div class="card-doc-pill">
              <span>{{ form.tipoDocumento }}: {{ form.numeroDocumento || '—' }}</span>
            </div>
          </div>

          <div class="card-details-list">
            <div class="card-detail-item">
              <span class="detail-label">Representante:</span>
              <span class="detail-val">{{ form.contactoRepresentante || 'No asignado' }}</span>
            </div>

            <div class="card-detail-item">
              <span class="detail-label">Correo:</span>
              <span class="detail-val highlight-val">{{ form.correoContacto || 'correo@ejemplo.com' }}</span>
            </div>

            <div class="card-detail-item">
              <span class="detail-label">Teléfono:</span>
              <span class="detail-val">{{ form.telefonoContacto || 'Sin teléfono' }}</span>
            </div>

            <div class="card-detail-item">
              <span class="detail-label">Clasificación:</span>
              <span :class="['status-pill-badge', form.esProspecto ? 'badge-prospect' : 'badge-client']">
                {{ form.esProspecto ? 'PROSPECTO' : 'CLIENTE EFECTIVO' }}
              </span>
            </div>
          </div>

          <div class="card-footer-stamp">
            <i class="pi pi-verified"></i>
            <span>SISTEMA DE GESTIÓN ADUANERA • MÓDULO COMERCIAL</span>
          </div>
        </div>

        <!-- Tarjeta de Control de Calidad y Validación -->
        <div class="checklist-card">
          <h4 class="checklist-title">
            <i class="pi pi-list-check"></i>
            <span>Validación de Expediente</span>
          </h4>

          <ul class="checklist-items">
            <li :class="['checklist-row', { valid: isDocumentoValid }]">
              <i :class="isDocumentoValid ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Número de {{ form.tipoDocumento }} válido</span>
            </li>
            <li :class="['checklist-row', { valid: !!form.razonSocial && form.razonSocial.length >= 3 }]">
              <i :class="form.razonSocial && form.razonSocial.length >= 3 ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Razón Social completa</span>
            </li>
            <li :class="['checklist-row', { valid: isEmailValid }]">
              <i :class="isEmailValid ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Correo electrónico institucional</span>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  clienteToEdit: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['cancel', 'saved'])

const submitting = ref(false)
const errorMessage = ref('')

const defaultForm = () => ({
  tipoDocumento: 'RUC',
  numeroDocumento: '',
  razonSocial: '',
  contactoRepresentante: '',
  direccionFiscal: '',
  correoContacto: '',
  telefonoContacto: '',
  esProspecto: true
})

const form = ref(defaultForm())

const isEditMode = computed(() => !!props.clienteToEdit && !!props.clienteToEdit.idCliente)

watch(
  () => props.clienteToEdit,
  (newVal) => {
    errorMessage.value = ''
    if (newVal && newVal.idCliente) {
      form.value = {
        tipoDocumento: newVal.tipoDocumento || 'RUC',
        numeroDocumento: newVal.numeroDocumento || '',
        razonSocial: newVal.razonSocial || '',
        contactoRepresentante: newVal.contactoRepresentante || '',
        direccionFiscal: newVal.direccionFiscal || '',
        correoContacto: newVal.correoContacto || '',
        telefonoContacto: newVal.telefonoContacto || '',
        esProspecto: newVal.esProspecto ?? true
      }
    } else {
      form.value = defaultForm()
    }
  },
  { immediate: true }
)

const getDocValidationHint = computed(() => {
  if (form.value.tipoDocumento === 'RUC') return '11 DÍGITOS'
  if (form.value.tipoDocumento === 'DNI') return '8 DÍGITOS'
  return 'ALFANUMÉRICO'
})

const isDocumentoValid = computed(() => {
  const doc = form.value.numeroDocumento.trim()
  if (!doc) return false
  if (form.value.tipoDocumento === 'RUC') return /^\d{11}$/.test(doc)
  if (form.value.tipoDocumento === 'DNI') return /^\d{8}$/.test(doc)
  return doc.length >= 4
})

const isEmailValid = computed(() => {
  const email = form.value.correoContacto.trim()
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
})

const isFormReady = computed(() => {
  return isDocumentoValid.value && form.value.razonSocial.trim().length >= 3 && isEmailValid.value
})

const handleSubmit = () => {
  errorMessage.value = ''
  if (!isFormReady.value) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*) con datos válidos'
    return
  }

  submitting.value = true
  emit('saved', {
    isEdit: isEditMode.value,
    clienteId: props.clienteToEdit?.idCliente,
    payload: { ...form.value }
  })
}
</script>

<style scoped>
.cliente-registration-form-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

/* 1. Header Bar */
.form-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-5, 1.25rem);
  padding: var(--space-5, 1.25rem) var(--space-6, 1.5rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  box-shadow: var(--shadow-subtle);
  flex-wrap: wrap;
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 820px;
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: 0.725rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  padding: 0;
  margin-bottom: 0.15rem;
  transition: color var(--transition-fast);
}

.btn-back-link:hover {
  color: var(--color-primary);
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.badge-kicker {
  font-size: 0.625rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-xs, 0.25rem);
  border: 1px solid var(--color-border-subtle);
}

.mode-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 0.15rem 0.55rem;
  border-radius: var(--radius-pill);
}

.mode-create {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.mode-edit {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.form-main-title {
  font-family: var(--font-sans);
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.015em;
  line-height: 1.25;
  margin: 0.15rem 0 0;
}

.form-main-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.45;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.55rem 1.15rem;
  font-size: 0.84rem;
  font-weight: 600;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-outline-warm:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-bg-subtle);
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.55rem 1.25rem;
  font-size: 0.84rem;
  font-weight: 600;
  border-radius: var(--radius-md, 0.5rem);
  border: none;
  background: var(--color-primary);
  color: var(--color-primary-text);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: 0 2px 6px rgba(53, 47, 42, 0.2);
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.btn-espresso:disabled,
.btn-outline-warm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* Banner de Error */
.error-banner {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background: var(--color-danger-subtle);
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-lg, 0.75rem);
  color: var(--color-danger);
  font-size: var(--font-size-sm, 0.875rem);
}

.error-content {
  flex: 1;
}

.error-close-btn {
  background: none;
  border: none;
  color: var(--color-danger);
  cursor: pointer;
  padding: 0.25rem;
}

/* 2. Grid de Contenido */
.form-content-grid {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: var(--space-6, 1.5rem);
  align-items: start;
}

@media (max-width: 1080px) {
  .form-content-grid {
    grid-template-columns: 1fr;
  }
}

/* Bloques de Formulario */
.form-blocks-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.form-block-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem) var(--space-8, 2rem);
  box-shadow: var(--shadow-sm);
}

.block-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4, 1rem);
  margin-bottom: var(--space-6, 1.5rem);
  padding-bottom: var(--space-4, 1rem);
  border-bottom: 1px solid var(--color-border-subtle);
}

.block-idx {
  font-family: var(--font-mono);
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-subtle);
  width: 2.25rem;
  height: 2.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md, 0.5rem);
  flex-shrink: 0;
}

.block-header-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.block-title {
  font-size: var(--font-size-md, 1.05rem);
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
}

.block-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
}

.block-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
}

/* Grupos de Campos */
.field-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.fields-two-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4, 1rem);
}

@media (max-width: 640px) {
  .fields-two-cols {
    grid-template-columns: 1fr;
  }
}

.label-with-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.field-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-main);
}

.kicker-mini-badge {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-sm, 0.25rem);
}

.input-icon-box {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 0.9rem;
  color: var(--color-text-muted);
  font-size: 0.95rem;
  pointer-events: none;
}

.form-input {
  width: 100%;
  padding: 0.7rem 0.9rem 0.7rem 2.5rem;
  font-size: var(--font-size-sm, 0.875rem);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  transition: all var(--transition-fast);
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  background: var(--color-bg-surface);
  box-shadow: 0 0 0 2px var(--color-primary-subtle);
}

.doc-num-input {
  font-family: monospace;
  font-weight: 800;
  letter-spacing: 0.06em;
  font-size: var(--font-size-md, 1rem);
}

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.field-helper-text {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.4;
}

/* Tipos de Documento Selector */
.doc-types-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--space-2, 0.5rem);
}

.doc-type-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.65rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.doc-type-btn:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-border-subtle);
}

.doc-type-btn.active {
  background: var(--color-primary-subtle);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* Clasificación Automática Banner */
.classification-auto-banner {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4, 1rem);
  padding: var(--space-5, 1.25rem) var(--space-6, 1.5rem);
  border-radius: var(--radius-lg, 0.75rem);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  transition: all var(--transition-fast);
}

.classification-auto-banner.prospect-banner {
  border-color: rgba(217, 119, 6, 0.35);
  background: rgba(217, 119, 6, 0.05);
}

.classification-auto-banner.client-banner {
  border-color: rgba(16, 185, 129, 0.35);
  background: rgba(16, 185, 129, 0.05);
}

.auto-banner-icon {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.35rem;
  flex-shrink: 0;
}

.auto-banner-icon.prospect-icon {
  background: var(--color-warning-subtle);
  color: var(--color-warning);
}

.auto-banner-icon.client-icon {
  background: var(--color-success-subtle);
  color: var(--color-success);
}

.auto-banner-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  flex: 1;
}

.auto-banner-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.auto-banner-title {
  font-size: var(--font-size-base, 1rem);
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
}

.auto-banner-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.65rem;
  border-radius: var(--radius-full, 9999px);
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.auto-banner-badge.badge-prospect {
  background: var(--color-warning-subtle);
  color: var(--color-warning);
  border: 1px solid rgba(217, 119, 6, 0.3);
}

.auto-banner-badge.badge-client {
  background: var(--color-success-subtle);
  color: var(--color-success);
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.auto-banner-desc {
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0;
}

/* 3. Columna Lateral Sticky */
.form-sidebar-column {
  position: sticky;
  top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
}

/* Tarjeta Credencial de Cliente */
.preview-credential-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.card-ribbon {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--color-primary);
  background: var(--color-primary-subtle);
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-full, 9999px);
  margin-bottom: var(--space-4, 1rem);
}

.card-avatar-box {
  margin-bottom: var(--space-3, 0.75rem);
}

.avatar-circle {
  width: 4rem;
  height: 4rem;
  border-radius: var(--radius-lg, 0.75rem);
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  border: 2px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
}

.card-identity {
  margin-bottom: var(--space-4, 1rem);
}

.card-client-name {
  font-family: var(--font-sans);
  font-size: var(--font-size-md, 1.1rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 var(--space-2, 0.5rem) 0;
  line-height: 1.3;
}

.card-doc-pill {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 0.25rem 0.75rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  color: var(--color-text-main);
}

.card-details-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-3, 0.75rem);
  margin-bottom: var(--space-5, 1.25rem);
  padding: var(--space-4, 1rem);
  background: var(--color-bg-subtle);
  border-radius: var(--radius-lg, 0.75rem);
  border: 1px solid var(--color-border-subtle);
}

.card-detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-xs, 0.75rem);
  gap: var(--space-2, 0.5rem);
}

.detail-label {
  color: var(--color-text-muted);
  font-weight: 600;
}

.detail-val {
  color: var(--color-text-main);
  font-weight: 700;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.highlight-val {
  color: var(--color-primary);
  font-weight: 800;
}

.status-pill-badge {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  letter-spacing: 0.06em;
  padding: 0.15rem 0.55rem;
  border-radius: var(--radius-pill);
}

.badge-client {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.badge-prospect {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.card-footer-stamp {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
  font-weight: 700;
}

/* Tarjeta Checklist */
.checklist-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle);
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
}

.checklist-title {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-main);
  margin: 0;
}

.checklist-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.checklist-row {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  transition: color var(--transition-fast);
}

.checklist-row.valid {
  color: var(--color-success-text);
  font-weight: 600;
}
</style>
