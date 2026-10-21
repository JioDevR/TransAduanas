<template>
  <div class="login-page">
    <div class="login-wrapper">
      <!-- Tarjeta Minimalista de Login -->
      <div class="login-card">
        <!-- Brand Header -->
        <div class="brand-header">
          <div class="brand-logo">
            <i class="pi pi-shield"></i>
          </div>
          <h1 class="brand-title">SGA</h1>
          <p class="brand-subtitle">Sistema de Gestión Aduanera</p>
        </div>

        <!-- Mensaje de Error -->
        <transition name="fade">
          <div v-if="errorMessage" class="alert-box error">
            <i class="pi pi-exclamation-circle"></i>
            <span>{{ errorMessage }}</span>
          </div>
        </transition>

        <!-- Formulario -->
        <form @submit.prevent="handleSubmit" class="login-form">
          <div class="form-group">
            <label for="username">Usuario</label>
            <div class="input-wrapper">
              <i class="pi pi-user input-icon"></i>
              <input
                id="username"
                v-model.trim="form.username"
                type="text"
                placeholder="Ej. admin"
                autocomplete="username"
                required
                :disabled="loading"
                class="custom-input"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="password">Contraseña</label>
            <div class="input-wrapper">
              <i class="pi pi-lock input-icon"></i>
              <input
                id="password"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="••••••••"
                autocomplete="current-password"
                required
                :disabled="loading"
                class="custom-input"
              />
              <button
                type="button"
                class="toggle-pwd-btn"
                @click="showPassword = !showPassword"
                tabindex="-1"
              >
                <i :class="showPassword ? 'pi pi-eye-slash' : 'pi pi-eye'"></i>
              </button>
            </div>
          </div>

          <button
            type="submit"
            class="submit-button"
            :disabled="loading || !isFormValid"
          >
            <i v-if="loading" class="pi pi-spin pi-spinner"></i>
            <span v-else>Iniciar Sesión</span>
          </button>
        </form>

        <!-- Ayuda Rápida de Acceso por Roles -->
        <div class="quick-credentials">
          <p class="quick-title">Accesos Rápidos de Prueba por Rol</p>
          <div class="credentials-group">
            <div class="credentials-tag" @click="fillAdminCredentials" title="Acceso con rol ADMINISTRADOR_TI (todos los módulos)">
              <span>Admin TI: <strong>admin</strong></span>
              <span class="sep">•</span>
              <span><strong>Password123!</strong></span>
              <i class="pi pi-shield copy-icon"></i>
            </div>
            <div class="credentials-tag" @click="fillComercialCredentials" title="Acceso con rol AGENTE_COMERCIAL (Recepción Comercial)">
              <span>Comercial: <strong>comercial</strong></span>
              <span class="sep">•</span>
              <span><strong>Password123!</strong></span>
              <i class="pi pi-briefcase copy-icon"></i>
            </div>
          </div>
        </div>

        <div class="footer-note">
          Plataforma SIGAD &copy; 2026 • Acceso Restringido y Auditado
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: ''
})

const showPassword = ref(false)
const errorMessage = ref('')
const loading = computed(() => authStore.loading)

const isFormValid = computed(() => {
  return form.username.trim().length > 0 && form.password.length > 0
})

const fillAdminCredentials = () => {
  form.username = 'admin'
  form.password = 'Password123!'
  errorMessage.value = ''
}

const fillComercialCredentials = () => {
  form.username = 'comercial'
  form.password = 'Password123!'
  errorMessage.value = ''
}

const handleSubmit = async () => {
  if (!isFormValid.value) return
  errorMessage.value = ''

  try {
    const user = await authStore.login({
      username: form.username,
      password: form.password
    })

    // Redirección directa según rol
    const redirectParam = route.query.redirect
    if (redirectParam && redirectParam !== '/') {
      router.push(redirectParam)
    } else {
      router.push(authStore.homePath)
    }
  } catch (err) {
    errorMessage.value = err.message || 'Credenciales inválidas o servidor no disponible.'
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-base);
  padding: 1.5rem;
  font-family: inherit;
}

.login-wrapper {
  width: 100%;
  max-width: 440px;
}

.login-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: 2.75rem 2.25rem;
  box-shadow: var(--shadow-elevated);
  color: var(--color-text-main);
}

.brand-header {
  text-align: center;
  margin-bottom: 2rem;
}

.brand-logo {
  width: 52px;
  height: 52px;
  background: var(--color-primary);
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.85rem;
  box-shadow: 0 4px 12px rgba(53, 47, 42, 0.25);
}

.brand-logo i {
  font-size: 1.6rem;
  color: #efe7dd;
}

.brand-title {
  font-size: 1.85rem;
  font-weight: 700;
  letter-spacing: -0.025em;
  margin: 0;
  color: var(--color-text-main);
}

.brand-subtitle {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin-top: 0.35rem;
  margin-bottom: 0;
}

.alert-box {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  margin-bottom: 1.5rem;
}

.alert-box.error {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger-text);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.form-group label {
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 0.95rem;
  color: var(--color-text-subtle);
  font-size: 1rem;
  pointer-events: none;
}

.custom-input {
  width: 100%;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.75rem 2.5rem 0.75rem 2.6rem;
  color: var(--color-text-main);
  font-size: 0.925rem;
  outline: none;
  transition: all 0.2s ease;
}

.custom-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(53, 47, 42, 0.08);
}

.custom-input::placeholder {
  color: var(--color-text-subtle);
}

.toggle-pwd-btn {
  position: absolute;
  right: 0.75rem;
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease;
}

.toggle-pwd-btn:hover {
  color: var(--color-text-main);
}

.submit-button {
  background: var(--color-primary);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md);
  padding: 0.85rem;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  box-shadow: 0 2px 8px rgba(53, 47, 42, 0.2);
  transition: all 0.2s ease;
  margin-top: 0.5rem;
}

.submit-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.submit-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.quick-credentials {
  margin-top: 1.85rem;
  padding-top: 1.25rem;
  border-top: 1px dashed var(--color-border);
  text-align: center;
}

.quick-title {
  font-size: 0.725rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-accent-kicker);
  font-weight: 700;
  margin: 0 0 0.5rem 0;
}

.credentials-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: stretch;
}

.credentials-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #ede6dc;
  border: 1px solid var(--color-border);
  color: #4a3e33;
  padding: 0.45rem 0.85rem;
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.credentials-tag:hover {
  background: #e2d7c7;
  border-color: #cbbead;
}

.credentials-tag .sep {
  opacity: 0.4;
}

.copy-icon {
  font-size: 0.75rem;
  margin-left: 0.25rem;
  opacity: 0.8;
}

.footer-note {
  text-align: center;
  font-size: 0.725rem;
  color: var(--color-text-subtle);
  margin-top: 1.5rem;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
