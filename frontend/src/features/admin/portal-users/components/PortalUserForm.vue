<template>
  <div class="portal-user-form-view">
    <!-- 1. Barra Superior de Encabezado (Form Header Bar - Estándar Oficial SGA) -->
    <div class="form-header-bar">
      <div class="header-breadcrumb-area">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('cancel')"
          :disabled="submitting"
          title="Regresar a la lista de usuarios del portal"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver al Directorio B2B</span>
        </button>

        <div class="header-titles">
          <div class="kicker-row">
            <span class="badge-kicker">PLANTILLA DE ACCESO B2B // PORTAL DE CLIENTES</span>
            <span :class="['mode-pill', isEditMode ? 'mode-edit' : 'mode-create']">
              <i :class="isEditMode ? 'pi pi-user-edit' : 'pi pi-user-plus'"></i>
              {{ isEditMode ? `MODO EDICIÓN (ID #${userToEdit?.idUsuarioCliente})` : 'NUEVO USUARIO CLIENTE' }}
            </span>
          </div>
          <h2 class="form-main-title">
            {{ isEditMode ? `Ficha B2B: @${form.username}` : 'Aprovisionamiento de Usuario para Portal de Clientes' }}
          </h2>
          <p class="form-main-desc">
            {{
              isEditMode
                ? 'Actualice los datos personales, cargo, rol del portal y estado de la cuenta empresarial.'
                : 'Asocie una empresa cliente activa, configure credenciales de acceso y defina el rol operativo en el portal.'
            }}
          </p>
        </div>
      </div>

      <div class="header-actions">
        <button
          type="button"
          class="btn-outline-warm"
          @click="$emit('cancel')"
          :disabled="submitting"
        >
          <i class="pi pi-times"></i>
          <span>Descartar</span>
        </button>

        <button
          form="portal-user-form"
          type="submit"
          class="btn-espresso"
          :disabled="submitting"
        >
          <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-check"></i>
          <span>{{ isEditMode ? 'Guardar Cambios' : 'Crear Usuario Portal' }}</span>
        </button>
      </div>
    </div>

    <!-- Error Alert Banner -->
    <transition name="slide-fade">
      <div v-if="errorMessage" class="error-banner">
        <i class="pi pi-exclamation-triangle"></i>
        <div class="error-content">
          <strong>Error de Validación:</strong>
          <span>{{ errorMessage }}</span>
        </div>
        <button type="button" class="error-close-btn" @click="errorMessage = ''">
          <i class="pi pi-times"></i>
        </button>
      </div>
    </transition>

    <!-- 2. Grid Principal: Formulario Estructurado + Live Preview (1fr 400px) -->
    <div class="form-content-grid">
      <!-- Columna Principal: Bloques de Campos -->
      <form id="portal-user-form" @submit.prevent="handleSubmit" class="form-blocks-column">
        <!-- BLOQUE 01: VINCULACIÓN EMPRESARIAL -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Empresa Cliente Asociada</h3>
              <p class="block-sub">
                Entidad corporativa a la que pertenecerá esta cuenta y cuyos datos aduaneros podrá consultar.
              </p>
            </div>
          </div>

          <div class="block-body">
            <div v-if="isEditMode" class="locked-client-card">
              <i class="pi pi-lock lock-icon"></i>
              <div>
                <span class="locked-label">Empresa Vinculada (No modificable):</span>
                <div class="locked-name font-bold">{{ userToEdit.razonSocialCliente }}</div>
                <div class="locked-ruc font-mono text-muted">
                  {{ userToEdit.tipoDocumentoCliente || 'RUC' }}: {{ userToEdit.rucCliente }} (ID #{{ userToEdit.idCliente }})
                </div>
              </div>
            </div>

            <div v-else class="field-row">
              <label class="field-label" for="cliente-select">Seleccione la Empresa Cliente *</label>
              <select
                id="cliente-select"
                v-model="form.idCliente"
                class="filter-select"
                required
                :disabled="submitting || loadingClientes"
              >
                <option value="" disabled>-- Seleccione una empresa importadora / exportadora --</option>
                <option v-for="c in clientes" :key="c.idCliente" :value="c.idCliente">
                  {{ c.numeroDocumento }} — {{ c.razonSocial }}
                </option>
              </select>
              <span v-if="selectedClienteObj" class="field-helper font-mono">
                <i class="pi pi-check text-success"></i> Dirección Fiscal: {{ selectedClienteObj.direccionFiscal || 'Registrada en SUNAT' }}
              </span>
            </div>
          </div>
        </section>

        <!-- BLOQUE 02: CREDENCIALES Y ROL EN EL PORTAL -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Credenciales de Acceso & Perfil B2B</h3>
              <p class="block-sub">
                Identificador único de acceso al portal y privilegios sobre expedientes y cotizaciones.
              </p>
            </div>
          </div>

          <div class="block-body">
            <div class="field-group">
              <label class="field-label" for="username-input">Nombre de Usuario (Login B2B) *</label>
              <div class="input-with-prefix">
                <span class="prefix">@</span>
                <input
                  id="username-input"
                  v-model="form.username"
                  type="text"
                  class="custom-input font-mono"
                  placeholder="ej. 20370146994 o ruc_empresa"
                  required
                  :disabled="isEditMode || submitting"
                />
              </div>
              <span class="field-helper">Identificador de inicio de sesión de la empresa (se sugiere su RUC).</span>
            </div>
            <div v-if="!isEditMode" class="password-creation-section">
              <div class="field-group">
                <label class="field-label" for="create-password">Contraseña Temporal de Acceso *</label>
                <div class="password-input-row">
                  <input
                    id="create-password"
                    v-model="form.password"
                    :type="showPassword ? 'text' : 'password'"
                    class="custom-input font-mono"
                    placeholder="Mínimo 6 caracteres..."
                    required
                    :disabled="submitting"
                  />
                  <button
                    type="button"
                    class="btn-icon-toggle"
                    @click="showPassword = !showPassword"
                    title="Mostrar / Ocultar"
                  >
                    <i :class="showPassword ? 'pi pi-eye-slash' : 'pi pi-eye'"></i>
                  </button>
                  <button
                    type="button"
                    class="btn-outline-warm btn-gen"
                    @click="generateRandomPassword"
                    :disabled="submitting"
                  >
                    <i class="pi pi-bolt"></i>
                    <span>Generar</span>
                  </button>
                </div>
              </div>

              <div class="checkbox-group">
                <label class="custom-checkbox-label">
                  <input
                    type="checkbox"
                    v-model="form.debeCambiarPassword"
                    class="custom-checkbox"
                    :disabled="submitting"
                  />
                  <span>Forzar al cliente a cambiar la contraseña en su primer inicio de sesión</span>
                </label>
              </div>
            </div>

            <!-- Estado Activo (En Edición) -->
            <div v-if="isEditMode" class="status-edit-row">
              <label class="field-label">Estado de la Cuenta *</label>
              <div class="status-pill-selector">
                <button
                  type="button"
                  :class="['status-opt-btn', { active: form.estadoActivo }]"
                  @click="form.estadoActivo = true"
                >
                  <i class="pi pi-check-circle"></i>
                  <span>Cuenta Activa (Acceso Vigente)</span>
                </button>
                <button
                  type="button"
                  :class="['status-opt-btn btn-danger-opt', { active: !form.estadoActivo }]"
                  @click="form.estadoActivo = false"
                >
                  <i class="pi pi-times-circle"></i>
                  <span>Cuenta Suspendida (Baja Lógica)</span>
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 03: INFORMACIÓN DEL REPRESENTANTE -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Datos del Representante y Comunicación Legal</h3>
              <p class="block-sub">
                Información de contacto oficial de la empresa para notificaciones aduaneras y cotizaciones.
              </p>
            </div>
          </div>

          <div class="block-body">
            <div class="field-group">
              <label class="field-label" for="contacto-input">Contacto / Representante Legal *</label>
              <input
                id="contacto-input"
                v-model="form.contactoRepresentante"
                type="text"
                class="custom-input"
                placeholder="ej. Lic. Carlos Mendoza"
                required
                :disabled="submitting"
              />
            </div>

            <div class="fields-two-cols">
              <div class="field-group">
                <label class="field-label" for="email-input">Correo Electrónico Institucional *</label>
                <input
                  id="email-input"
                  v-model="form.correoElectronico"
                  type="email"
                  class="custom-input font-mono"
                  placeholder="ej. operaciones@empresa.com"
                  required
                  :disabled="submitting"
                />
              </div>

              <div class="field-group">
                <label class="field-label" for="telefono-input">Teléfono o Celular de Contacto</label>
                <input
                  id="telefono-input"
                  v-model="form.telefonoContacto"
                  type="text"
                  class="custom-input"
                  placeholder="ej. +51 987 654 321"
                  :disabled="submitting"
                />
              </div>
            </div>
          </div>
        </section>
      </form>

      <!-- Columna Lateral: Live Preview Card (400px estándar) -->
      <aside class="form-preview-column">
        <div class="preview-sticky-card">
          <div class="preview-kicker">
            <i class="pi pi-id-card"></i>
            <span>VISTA PREVIA DE CREDENCIAL B2B</span>
          </div>

          <div class="preview-avatar-box">
            <div class="preview-avatar">
              {{ previewInitials }}
            </div>
            <div class="preview-user-details">
              <h4 class="preview-name">{{ form.contactoRepresentante || 'Representante Legal' }}</h4>
              <span class="preview-handle font-mono">@{{ form.username || 'usuario' }}</span>
            </div>
          </div>

          <div class="preview-info-list">
            <div class="preview-item">
              <span class="p-label">Empresa:</span>
              <span class="p-val font-bold">{{ previewClienteName }}</span>
            </div>
            <div class="preview-item">
              <span class="p-label">Acceso:</span>
              <span class="p-val font-medium">Cuenta Corporativa B2B</span>
            </div>
            <div class="preview-item">
              <span class="p-label">Contacto:</span>
              <span class="p-val">{{ form.contactoRepresentante || 'Representante Legal' }}</span>
            </div>
            <div class="preview-item">
              <span class="p-label">Correo:</span>
              <span class="p-val font-mono text-muted">{{ form.correoElectronico || 'sin-correo@...' }}</span>
            </div>
            <div class="preview-item">
              <span class="p-label">Teléfono:</span>
              <span class="p-val">{{ form.telefonoContacto || 'No registrado' }}</span>
            </div>
            <div class="preview-item">
              <span class="p-label">Estado:</span>
              <Tag
                :value="form.estadoActivo ? 'Activa' : 'Suspendida'"
                :severity="form.estadoActivo ? 'success' : 'danger'"
                rounded
              />
            </div>
          </div>

          <div class="preview-footer-note">
            <i class="pi pi-shield"></i>
            <span>Autenticación aislada para el Portal B2B de TransAduanas.</span>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import Tag from 'primevue/tag'
import { portalUserService } from '../services/portalUserService'
import { useNotification } from '@/shared/composables/useNotification'

const props = defineProps({
  userToEdit: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['cancel', 'saved'])
const { showSuccess, showError } = useNotification()

const isEditMode = computed(() => !!props.userToEdit)
const clientes = ref([])
const loadingClientes = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)

const form = reactive({
  idCliente: '',
  username: '',
  password: '',
  contactoRepresentante: '',
  correoElectronico: '',
  telefonoContacto: '',
  estadoActivo: true,
  debeCambiarPassword: true
})

onMounted(async () => {
  await loadClientes()
  if (isEditMode.value) {
    populateEditData(props.userToEdit)
  }
})

watch(
  () => props.userToEdit,
  (val) => {
    if (val) populateEditData(val)
  }
)

const loadClientes = async () => {
  loadingClientes.value = true
  try {
    clientes.value = await portalUserService.getClientesActivos()
  } catch (err) {
    showError('No se pudo cargar la lista de empresas clientes')
  } finally {
    loadingClientes.value = false
  }
}

watch(
  () => form.idCliente,
  (newId) => {
    if (!isEditMode.value && newId) {
      const cli = clientes.value.find((c) => c.idCliente === Number(newId))
      if (cli) {
        if (!form.username) form.username = cli.numeroDocumento || ''
        if (!form.contactoRepresentante) form.contactoRepresentante = cli.contactoRepresentante || ''
        if (!form.correoElectronico) form.correoElectronico = cli.correoContacto || ''
        if (!form.telefonoContacto) form.telefonoContacto = cli.telefonoContacto || ''
      }
    }
  }
)

const populateEditData = (u) => {
  form.idCliente = u.idCliente
  form.username = u.username
  form.contactoRepresentante = u.contactoRepresentante || u.nombres || ''
  form.correoElectronico = u.correoElectronico || ''
  form.telefonoContacto = u.telefonoContacto || ''
  form.estadoActivo = u.estadoActivo
  form.debeCambiarPassword = u.debeCambiarPassword
}

const selectedClienteObj = computed(() => {
  if (!form.idCliente) return null
  return clientes.value.find((c) => c.idCliente === Number(form.idCliente)) || null
})

const previewClienteName = computed(() => {
  if (isEditMode.value) return props.userToEdit?.razonSocialCliente || '—'
  return selectedClienteObj.value?.razonSocial || 'Seleccione una empresa'
})

const previewInitials = computed(() => {
  const parts = (form.contactoRepresentante || '').trim().split(/\s+/)
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase()
  } else if (parts.length === 1 && parts[0]) {
    return parts[0].substring(0, 2).toUpperCase()
  }
  return 'B2B'
})

const generateRandomPassword = () => {
  const chars = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789!@#$%&*'
  let result = ''
  for (let i = 0; i < 10; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  form.password = result
  showPassword.value = true
}

const handleSubmit = async () => {
  errorMessage.value = ''

  if (!isEditMode.value && !form.idCliente) {
    errorMessage.value = 'Debe seleccionar una empresa cliente vinculada'
    return
  }

  if (!form.username || form.username.trim().length < 3) {
    errorMessage.value = 'El nombre de usuario debe contener al menos 3 caracteres'
    return
  }

  if (!isEditMode.value && (!form.password || form.password.trim().length < 6)) {
    errorMessage.value = 'La contraseña temporal debe tener al menos 6 caracteres'
    return
  }

  if (!form.contactoRepresentante || !form.correoElectronico) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*)'
    return
  }

  submitting.value = true

  try {
    if (isEditMode.value) {
      await portalUserService.updatePortalUser(props.userToEdit.idUsuarioCliente, {
        username: form.username.trim().toLowerCase(),
        contactoRepresentante: form.contactoRepresentante.trim(),
        correoElectronico: form.correoElectronico.trim().toLowerCase(),
        telefonoContacto: form.telefonoContacto ? form.telefonoContacto.trim() : null,
        estadoActivo: form.estadoActivo,
        debeCambiarPassword: form.debeCambiarPassword
      })
      showSuccess(`Usuario @${form.username} actualizado exitosamente`)
    } else {
      await portalUserService.createPortalUser({
        idCliente: Number(form.idCliente),
        username: form.username.trim().toLowerCase(),
        password: form.password.trim(),
        contactoRepresentante: form.contactoRepresentante.trim(),
        correoElectronico: form.correoElectronico.trim().toLowerCase(),
        telefonoContacto: form.telefonoContacto ? form.telefonoContacto.trim() : null,
        debeCambiarPassword: form.debeCambiarPassword
      })
      showSuccess(`Usuario @${form.username} aprovisionado correctamente`)
    }

    emit('saved')
  } catch (err) {
    const msg = err.response?.data?.message || 'Error al guardar el usuario en el portal'
    errorMessage.value = msg
    showError(msg)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.portal-user-form-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   1. BARRA SUPERIOR DE ENCABEZADO (FORM HEADER BAR - SGA ESTÁNDAR)
   ========================================================================== */
.form-header-bar {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem 1.5rem;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  box-shadow: var(--shadow-subtle);
}

.header-breadcrumb-area {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: none;
  border: none;
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  color: var(--color-accent-kicker);
  cursor: pointer;
  padding: 0;
  transition: opacity var(--transition-fast, 150ms);
}

.btn-back-link:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.25rem;
}

.badge-kicker {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-accent-kicker);
}

.mode-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.6rem;
  border-radius: var(--radius-pill, 9999px);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
}

.mode-create {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.mode-edit {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
}

.form-main-title {
  margin: 0.25rem 0 0 0;
  font-family: var(--font-sans);
  font-size: var(--font-size-2xl, 1.45rem);
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.015em;
}

.form-main-desc {
  margin: 0.25rem 0 0 0;
  font-size: var(--font-size-base, 0.875rem);
  color: var(--color-text-muted);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

/* Banner de Error */
.error-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1.25rem;
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-danger-text);
}

.error-content {
  flex: 1;
  font-size: var(--font-size-sm, 0.8125rem);
}

.error-close-btn {
  background: none;
  border: none;
  color: var(--color-danger-text);
  cursor: pointer;
  padding: 0.25rem;
}

/* ==========================================================================
   2. REJILLA DEL FORMULARIO + PREVIEW LATERAL (1fr 400px ESTÁNDAR)
   ========================================================================== */
.form-content-grid {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: var(--space-6, 1.5rem);
  align-items: flex-start;
  width: 100%;
}

@media (max-width: 1080px) {
  .form-content-grid {
    grid-template-columns: 1fr;
  }
}

.form-blocks-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.form-block-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.5rem;
  box-shadow: var(--shadow-subtle);
}

.block-header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.25rem;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.block-idx {
  font-family: var(--font-mono, monospace);
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.block-title {
  margin: 0;
  font-size: var(--font-size-lg, 1.125rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.block-sub {
  margin: 0.25rem 0 0 0;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
}

.block-body {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.field-group,
.field-row {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.fields-two-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

@media (max-width: 640px) {
  .fields-two-cols {
    grid-template-columns: 1fr;
  }
}

.field-label {
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  color: var(--color-text-main);
}

.custom-input {
  width: 100%;
  padding: 0.65rem 0.85rem;
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main);
  transition: border-color var(--transition-fast, 150ms);
}

.custom-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(216, 185, 125, 0.2);
}

.filter-select {
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  background-color: var(--color-bg-surface) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md, 8px) !important;
  padding: 0.65rem 2.25rem 0.65rem 0.85rem !important;
  color: var(--color-text-main) !important;
  font-size: var(--font-size-sm, 0.8125rem) !important;
  font-weight: 500;
  outline: none;
  cursor: pointer;
  transition: border-color var(--transition-fast, 150ms);
  width: 100%;
}

.filter-select:focus {
  border-color: var(--color-primary) !important;
}

.input-with-prefix {
  display: flex;
  align-items: center;
  position: relative;
}

.input-with-prefix .prefix {
  position: absolute;
  left: 0.75rem;
  color: var(--color-text-subtle);
  font-weight: 700;
}

.input-with-prefix input {
  padding-left: 1.75rem;
}

.field-helper {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
}

.password-input-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-icon-toggle {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.65rem 0.85rem;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
}

.btn-icon-toggle:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.btn-gen {
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.checkbox-group {
  padding-top: 0.25rem;
}

.custom-checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: var(--font-size-sm, 0.8125rem);
  cursor: pointer;
  color: var(--color-text-main);
}

.custom-checkbox {
  accent-color: var(--color-primary);
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.locked-client-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: var(--color-bg-subtle);
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-md, 8px);
}

.lock-icon {
  font-size: 1.5rem;
  color: var(--color-text-subtle);
}

.locked-label {
  font-size: var(--font-size-2xs, 0.6875rem);
  text-transform: uppercase;
  color: var(--color-accent-kicker);
  font-weight: 700;
}

.status-edit-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.status-pill-selector {
  display: flex;
  gap: 0.75rem;
}

.status-opt-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.65rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-surface);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
}

.status-opt-btn.active {
  border-color: var(--color-success-border);
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.status-opt-btn.btn-danger-opt.active {
  border-color: var(--color-danger-border);
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
}

/* ==========================================================================
   3. COLUMNA LATERAL: LIVE PREVIEW CARD (400px)
   ========================================================================== */
.preview-sticky-card {
  position: sticky;
  top: 1.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.35rem;
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
  box-shadow: var(--shadow-subtle);
}

.preview-kicker {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
}

.preview-avatar-box {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md, 8px);
}

.preview-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-primary-text);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  flex-shrink: 0;
}

.preview-name {
  margin: 0;
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.preview-handle {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-accent-kicker);
}

.preview-info-list {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.preview-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--font-size-sm, 0.8125rem);
  gap: 0.5rem;
}

.p-label {
  color: var(--color-text-muted);
}

.p-val {
  color: var(--color-text-main);
  text-align: right;
}

.preview-footer-note {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-subtle);
  padding-top: 0.75rem;
  border-top: 1px dashed var(--color-border-subtle);
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.5rem 0.95rem;
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.5rem 0.95rem;
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.font-bold {
  font-weight: 700;
}

.text-success {
  color: var(--color-success-text);
}

.text-muted {
  color: var(--color-text-muted);
}
</style>
