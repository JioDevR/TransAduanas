<template>
  <div class="mandatory-password-page">
    <div class="mandatory-wrapper">
      <div class="mandatory-card">
        <!-- Logo y Cabecera de Seguridad -->
        <div class="brand-header">
          <div class="security-badge-icon" title="Seguridad TransAduanas">
            <i class="pi pi-shield"></i>
          </div>
          <h1 class="brand-title">Primer Inicio de Sesión</h1>
          <div class="security-pill">
            <i class="pi pi-lock"></i>
            <span>ACTUALIZACIÓN OBLIGATORIA DE CONTRASEÑA</span>
          </div>
          <p class="brand-subtitle">
            Por estrictas políticas de confidencialidad y control aduanero B2B, debe reemplazar su contraseña temporal antes de ingresar a la plataforma.
          </p>
        </div>

        <!-- Ficha de la Empresa Autenticada -->
        <div class="account-summary-box">
          <div class="account-icon">
            <i class="pi pi-building"></i>
          </div>
          <div class="account-info">
            <h4 class="account-title">{{ authStore.empresaNombre }}</h4>
            <div class="account-meta font-mono">
              <span>RUC: {{ authStore.ruc }}</span>
              <span class="sep">•</span>
              <span>Usuario: @{{ authStore.user?.username }}</span>
            </div>
          </div>
        </div>

        <!-- Alerta de Error -->
        <transition name="fade">
          <div v-if="errorMessage" class="alert-box error" role="alert">
            <i class="pi pi-exclamation-circle"></i>
            <span>{{ errorMessage }}</span>
          </div>
        </transition>

        <!-- Formulario de Cambio de Clave -->
        <form @submit.prevent="handleSubmit" class="password-form">
          <!-- Contraseña Temporal Actual -->
          <div class="form-group">
            <label for="current-pwd">Contraseña Temporal Asignada *</label>
            <div class="input-wrapper">
              <i class="pi pi-key input-icon"></i>
              <input
                id="current-pwd"
                v-model="form.passwordActual"
                :type="showCurrent ? 'text' : 'password'"
                placeholder="Ingrese la contraseña temporal recibida..."
                required
                :disabled="loading"
                class="custom-input font-mono"
              />
              <button
                type="button"
                class="toggle-pwd-btn"
                @click="showCurrent = !showCurrent"
                :title="showCurrent ? 'Ocultar contraseña' : 'Ver contraseña'"
                :aria-label="showCurrent ? 'Ocultar contraseña actual' : 'Ver contraseña actual'"
                tabindex="-1"
              >
                <i :class="showCurrent ? 'pi pi-eye-slash' : 'pi pi-eye'"></i>
              </button>
            </div>
            <span class="field-hint">La clave provista por el equipo comercial (ej. Password123!).</span>
          </div>

          <!-- Nueva Contraseña -->
          <div class="form-group">
            <label for="new-pwd">Nueva Contraseña Privada *</label>
            <div class="input-wrapper">
              <i class="pi pi-lock input-icon"></i>
              <input
                id="new-pwd"
                v-model="form.nuevaPassword"
                :type="showNew ? 'text' : 'password'"
                placeholder="Mínimo 6 caracteres..."
                required
                :disabled="loading"
                class="custom-input font-mono"
              />
              <button
                type="button"
                class="toggle-pwd-btn"
                @click="showNew = !showNew"
                :title="showNew ? 'Ocultar contraseña' : 'Ver contraseña'"
                :aria-label="showNew ? 'Ocultar nueva contraseña' : 'Ver nueva contraseña'"
                tabindex="-1"
              >
                <i :class="showNew ? 'pi pi-eye-slash' : 'pi pi-eye'"></i>
              </button>
            </div>
          </div>

          <!-- Confirmación de Nueva Contraseña -->
          <div class="form-group">
            <label for="confirm-pwd">Confirmar Nueva Contraseña *</label>
            <div class="input-wrapper">
              <i class="pi pi-lock-open input-icon"></i>
              <input
                id="confirm-pwd"
                v-model="form.confirmarPassword"
                :type="showConfirm ? 'text' : 'password'"
                placeholder="Repita la nueva contraseña..."
                required
                :disabled="loading"
                class="custom-input font-mono"
              />
              <button
                type="button"
                class="toggle-pwd-btn"
                @click="showConfirm = !showConfirm"
                :title="showConfirm ? 'Ocultar contraseña' : 'Ver contraseña'"
                :aria-label="showConfirm ? 'Ocultar confirmación de contraseña' : 'Ver confirmación de contraseña'"
                tabindex="-1"
              >
                <i :class="showConfirm ? 'pi pi-eye-slash' : 'pi pi-eye'"></i>
              </button>
            </div>
          </div>

          <!-- Checklist de Validación -->
          <div class="pwd-rules-box">
            <div :class="['rule-row', { valid: isLengthValid }]">
              <i :class="isLengthValid ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Al menos 6 caracteres</span>
            </div>
            <div :class="['rule-row', { valid: isMatchValid }]">
              <i :class="isMatchValid ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Las contraseñas coinciden</span>
            </div>
            <div :class="['rule-row', { valid: isDifferentFromCurrent }]">
              <i :class="isDifferentFromCurrent ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Es distinta a la contraseña temporal</span>
            </div>
          </div>

          <button
            type="submit"
            id="btn-update-pwd-submit"
            class="submit-button"
            :disabled="loading || !isFormValid"
          >
            <i v-if="loading" class="pi pi-spin pi-spinner"></i>
            <span v-else>
              Guardar Nueva Contraseña & Acceder al Portal
              <i class="pi pi-arrow-right" style="margin-left: 0.5rem;"></i>
            </span>
          </button>
        </form>

        <div class="logout-footer">
          <button type="button" class="btn-text-logout" @click="handleLogout">
            <i class="pi pi-sign-out"></i>
            <span>Cerrar sesión y volver más tarde</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/core/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')
const showCurrent = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)

const form = reactive({
  passwordActual: '',
  nuevaPassword: '',
  confirmarPassword: ''
})

const isLengthValid = computed(() => form.nuevaPassword.length >= 6)
const isMatchValid = computed(() => form.nuevaPassword.length > 0 && form.nuevaPassword === form.confirmarPassword)
const isDifferentFromCurrent = computed(() => form.nuevaPassword.length > 0 && form.passwordActual !== form.nuevaPassword)

const isFormValid = computed(() => {
  return form.passwordActual.trim().length > 0 &&
    isLengthValid.value &&
    isMatchValid.value &&
    isDifferentFromCurrent.value
})

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!isFormValid.value) return

  loading.value = true
  try {
    const result = await authStore.cambiarPassword(form.passwordActual.trim(), form.nuevaPassword.trim())
    if (result.success) {
      router.push('/cotizaciones')
    } else {
      errorMessage.value = result.message || 'Error al actualizar la contraseña'
    }
  } catch (err) {
    errorMessage.value = err.message || 'Error de comunicación con el servidor'
  } finally {
    loading.value = false
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.mandatory-password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-base);
  padding: 2rem 1rem;
}

.mandatory-wrapper {
  width: 100%;
  max-width: 520px;
}

.mandatory-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 2.5rem 2rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
}

.brand-header {
  text-align: center;
  margin-bottom: 1.75rem;
}

.security-badge-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 1rem;
  background: rgba(184, 91, 53, 0.12);
  color: var(--color-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  border: 1px solid rgba(184, 91, 53, 0.25);
}

.brand-title {
  font-size: 1.625rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0 0 0.5rem;
  letter-spacing: -0.02em;
}

.security-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: rgba(197, 137, 64, 0.12);
  color: #b45309;
  border: 1px solid rgba(197, 137, 64, 0.3);
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  margin-bottom: 0.75rem;
}

.brand-subtitle {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0;
}

.account-summary-box {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.875rem 1rem;
  margin-bottom: 1.5rem;
}

.account-icon {
  width: 40px;
  height: 40px;
  background: var(--color-primary);
  color: var(--color-primary-text, #fff);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.125rem;
  flex-shrink: 0;
}

.account-title {
  font-size: 0.925rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.2rem;
}

.account-meta {
  font-size: 0.775rem;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.account-meta .sep {
  opacity: 0.5;
}

.alert-box.error {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.85rem 1rem;
  background: rgba(220, 38, 38, 0.08);
  border: 1px solid rgba(220, 38, 38, 0.25);
  border-radius: var(--radius-md, 8px);
  color: #dc2626;
  font-size: 0.85rem;
  margin-bottom: 1.25rem;
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-group label {
  font-size: 0.825rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.input-wrapper {
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

.custom-input {
  width: 100%;
  padding: 0.75rem 2.5rem 0.75rem 2.4rem;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  font-size: 0.9rem;
  transition: all var(--transition-fast, 0.15s ease);
}

.custom-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(184, 91, 53, 0.15);
}

.toggle-pwd-btn {
  position: absolute;
  right: 0.75rem;
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toggle-pwd-btn:hover {
  color: var(--color-text-main);
}

.field-hint {
  font-size: 0.725rem;
  color: var(--color-text-muted);
}

.pwd-rules-box {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0.75rem 0.9rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
}

.rule-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.775rem;
  color: var(--color-text-muted);
  transition: color 0.15s ease;
}

.rule-row.valid {
  color: #16a34a;
  font-weight: 600;
}

.text-success {
  color: #16a34a;
}

.text-muted {
  color: var(--color-text-muted);
}

.submit-button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.85rem 1.25rem;
  background: var(--color-primary);
  color: var(--color-primary-text, #ffffff);
  border: none;
  border-radius: var(--radius-md, 8px);
  font-weight: 700;
  font-size: 0.925rem;
  cursor: pointer;
  margin-top: 0.5rem;
  transition: all var(--transition-fast, 0.15s ease);
}

.submit-button:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
}

.submit-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.logout-footer {
  margin-top: 1.5rem;
  text-align: center;
}

.btn-text-logout {
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  transition: color 0.15s ease;
}

.btn-text-logout:hover {
  color: #dc2626;
}

.font-mono {
  font-family: 'JetBrains Mono', monospace;
}
</style>
