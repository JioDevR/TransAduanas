<template>
  <div class="login-page">
    <div class="login-wrapper">
      <!-- Tarjeta Minimalista de Login -->
      <div class="login-card">
        <!-- Brand Header -->
        <div class="brand-header">
          <div class="brand-logo" title="TransAduanas SGA">
            <i class="pi pi-shield"></i>
          </div>
          <h1 class="brand-title">TransAduanas - SGA</h1>
          <p class="brand-subtitle">Sistema de Gestión Aduanera & Logística</p>
        </div>

        <!-- Mensaje de Error -->
        <transition name="fade">
          <div v-if="errorMessage" class="alert-box error" role="alert">
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
                :title="showPassword ? 'Ocultar contraseña' : 'Ver contraseña'"
                aria-label="Alternar visibilidad de contraseña"
                tabindex="-1"
              >
                <i :class="showPassword ? 'pi pi-eye-slash' : 'pi pi-eye'"></i>
              </button>
            </div>
          </div>

          <button
            type="submit"
            id="btn-login-submit"
            class="submit-button"
            :disabled="loading || !isFormValid"
          >
            <i v-if="loading" class="pi pi-spin pi-spinner"></i>
            <span v-else>Iniciar Sesión</span>
          </button>
        </form>

        <div class="footer-note">
          TransAduanas SGA &copy; 2026 &bull; Acceso Restringido y Auditado
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { getHomeRouteForRole } from '@/core/auth/roles'

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

const handleSubmit = async () => {
  if (!isFormValid.value) return
  errorMessage.value = ''

  try {
    await authStore.login({
      username: form.username,
      password: form.password
    })

    // Redirección directa según rol sanitizando redirect
    const redirectParam = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') && !route.query.redirect.startsWith('//')
      ? route.query.redirect
      : null
    const defaultHome = getHomeRouteForRole(authStore.userRole) || authStore.homePath
    const targetPath = redirectParam && redirectParam !== '/login' ? redirectParam : defaultHome
    await router.push(targetPath)
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
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(214, 180, 118, 0.08) 0%, transparent 65%);
  pointer-events: none;
  z-index: 0;
}

.login-wrapper {
  position: relative;
  z-index: 1;
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
  position: relative;
  overflow: hidden;
}

.login-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--color-primary), var(--color-accent-kicker), transparent);
}

.brand-header {
  text-align: center;
  margin-bottom: 2rem;
}

.brand-logo {
  width: 54px;
  height: 54px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-strong);
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.85rem;
  box-shadow: var(--shadow-subtle);
  transition: transform 0.25s ease;
}

.brand-logo:hover {
  transform: scale(1.04);
}

.brand-logo i {
  font-size: 1.65rem;
  color: var(--color-primary);
}

.brand-title {
  font-size: 1.65rem;
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
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.75rem 2.5rem 0.75rem 2.6rem;
  color: var(--color-text-main);
  font-size: 0.925rem;
  outline: none;
  transition: all 0.2s ease;
}

.custom-input:focus {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(214, 180, 118, 0.15);
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
  color: var(--color-primary-text);
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
  margin-top: 0.5rem;
}

.submit-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
}

.submit-button:active:not(:disabled) {
  transform: translateY(0);
}

.submit-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.footer-note {
  text-align: center;
  font-size: 0.725rem;
  color: var(--color-text-subtle);
  margin-top: 1.75rem;
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
