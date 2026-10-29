<template>
  <div class="login-page">
    <div class="login-wrapper">
      <div class="login-card">
        <!-- Logo y Cabecera de Marca -->
        <div class="brand-header">
          <div class="brand-logo" title="TransAduanas - Portal de Clientes">
            <i class="pi pi-shield"></i>
          </div>
          <h1 class="brand-title">TransAduanas</h1>
          <p class="brand-portal-tag">PORTAL DEL CLIENTE B2B</p>
          <p class="brand-subtitle">Plataforma de Consulta y Seguimiento Aduanero</p>
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
            <label for="username">RUC o Usuario Empresarial</label>
            <div class="input-wrapper">
              <i class="pi pi-building input-icon"></i>
              <input
                id="username"
                v-model.trim="form.username"
                type="text"
                placeholder="Ej. 20511894520 o lgelectronics"
                autocomplete="username"
                required
                :disabled="loading"
                class="custom-input"
              />
            </div>
            <span class="field-hint">Puede ingresar con su número de RUC de 11 dígitos o su usuario registrado.</span>
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
                aria-label="Alternar visibilidad"
                tabindex="-1"
              >
                <i :class="showPassword ? 'pi pi-eye-slash' : 'pi pi-eye'"></i>
              </button>
            </div>
          </div>

          <div class="form-options">
            <label class="remember-label">
              <input type="checkbox" v-model="rememberMe" />
              <span>Recordar RUC en este equipo</span>
            </label>
          </div>

          <button
            type="submit"
            id="btn-login-submit"
            class="submit-button"
            :disabled="loading || !isFormValid"
          >
            <i v-if="loading" class="pi pi-spin pi-spinner"></i>
            <span v-else>
              Ingresar al Portal Empresarial
              <i class="pi pi-arrow-right" style="margin-left: 0.5rem;"></i>
            </span>
          </button>
        </form>

        <!-- Sugerencia / Credenciales de Prueba -->
        <div v-if="isDev" class="demo-box">
          <div class="demo-title">
            <i class="pi pi-info-circle"></i>
            <span>Cuentas de demostración disponibles:</span>
          </div>
          <div class="demo-item" @click="fillDemo('20511894520', 'Password123!')">
            <span class="demo-name">LG Electronics:</span>
            <span class="demo-val">RUC: 20511894520 / Password123!</span>
          </div>
          <div class="demo-item" @click="fillDemo('20370146994', 'Password123!')">
            <span class="demo-name">Camposol S.A.:</span>
            <span class="demo-val">RUC: 20370146994 / Password123!</span>
          </div>
        </div>

        <div class="footer-note">
          <p>¿Problemas para acceder a su cuenta empresarial? Contacte a su Agente Comercial asignado o escríbanos a <strong>soporte@transaduanas.pe</strong></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/core/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const isDev = import.meta.env.DEV

const form = reactive({
  username: '',
  password: ''
})

const showPassword = ref(false)
const rememberMe = ref(true)
const errorMessage = ref('')
const loading = ref(false)

const isFormValid = computed(() => {
  return form.username.trim().length > 0 && form.password.length > 0
})

onMounted(() => {
  const savedRuc = localStorage.getItem('sga_portal_remember_ruc')
  if (savedRuc) {
    form.username = savedRuc
  }
})

function fillDemo(user, pwd) {
  form.username = user
  form.password = pwd
  errorMessage.value = ''
}

async function handleSubmit() {
  if (!isFormValid.value || loading.value) return

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await authStore.login(form.username, form.password)
    if (res.success) {
      if (rememberMe.value) {
        localStorage.setItem('sga_portal_remember_ruc', form.username)
      } else {
        localStorage.removeItem('sga_portal_remember_ruc')
      }
      router.push('/cotizaciones')
    } else {
      errorMessage.value = res.message || 'Credenciales incorrectas'
    }
  } catch (err) {
    errorMessage.value = 'Ocurrió un error inesperado al conectar con el servidor'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-bg-base, #f6f3ee);
  padding: 1.5rem;
}

.login-wrapper {
  width: 100%;
  max-width: 480px;
}

.login-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 12px;
  padding: 2.5rem 2.25rem;
  box-shadow: 0 4px 20px rgba(43, 37, 32, 0.05);
}

.brand-header {
  text-align: center;
  margin-bottom: 2rem;
}

.brand-logo {
  width: 52px;
  height: 52px;
  background: var(--color-primary, #352f2a);
  color: #ffffff;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.6rem;
  margin: 0 auto 1rem;
}

.brand-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main, #2b2520);
  margin: 0 0 0.35rem 0;
  letter-spacing: -0.02em;
}

.brand-portal-tag {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #9a7f60);
  background: var(--color-bg-subtle, #f2ebe1);
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
  margin: 0 0 0.5rem 0;
}

.brand-subtitle {
  font-size: 0.85rem;
  color: var(--color-text-muted, #6f6356);
  margin: 0;
}

.alert-box {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  font-size: 0.82rem;
  margin-bottom: 1.5rem;
}

.alert-box.error {
  background: var(--color-danger-bg, #fdf0f0);
  color: var(--color-danger-text, #9e2a2b);
  border: 1px solid var(--color-danger-border, #f6c8c8);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-group label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-main, #2b2520);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 0.85rem;
  color: var(--color-text-subtle, #998c7e);
  font-size: 1rem;
}

.custom-input {
  width: 100%;
  padding: 0.75rem 0.85rem 0.75rem 2.5rem;
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 6px;
  background: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  font-size: 0.88rem;
  font-family: var(--font-sans);
  transition: all 0.2s ease;
}

.custom-input:focus {
  outline: none;
  border-color: var(--color-primary, #352f2a);
  box-shadow: 0 0 0 3px rgba(53, 47, 42, 0.08);
}

.toggle-pwd-btn {
  position: absolute;
  right: 0.75rem;
  background: none;
  border: none;
  color: var(--color-text-subtle, #998c7e);
  cursor: pointer;
  padding: 0.25rem;
}

.field-hint {
  font-size: 0.7rem;
  color: var(--color-text-muted, #6f6356);
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.8rem;
}

.remember-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  color: var(--color-text-muted, #6f6356);
}

.submit-button {
  background: var(--color-primary, #352f2a);
  color: var(--color-primary-text, #ffffff);
  border: none;
  border-radius: 6px;
  padding: 0.85rem 1.25rem;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease;
  margin-top: 0.5rem;
}

.submit-button:hover:not(:disabled) {
  background: var(--color-primary-hover, #484039);
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.demo-box {
  margin-top: 1.75rem;
  padding: 0.85rem;
  background: var(--color-bg-subtle, #f2ebe1);
  border: 1px dashed var(--color-border-strong, #d5c8b6);
  border-radius: 6px;
  font-size: 0.75rem;
}

.demo-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-weight: 600;
  color: var(--color-accent-kicker, #9a7f60);
  margin-bottom: 0.45rem;
}

.demo-item {
  cursor: pointer;
  padding: 0.25rem 0.4rem;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  transition: background 0.15s ease;
}

.demo-item:hover {
  background: rgba(255, 255, 255, 0.6);
}

.demo-name {
  font-weight: 600;
  color: var(--color-text-main, #2b2520);
}

.demo-val {
  font-family: var(--font-mono);
  color: var(--color-text-muted, #6f6356);
}

.footer-note {
  margin-top: 1.75rem;
  text-align: center;
  font-size: 0.74rem;
  color: var(--color-text-subtle, #998c7e);
  line-height: 1.4;
}

.footer-note strong {
  color: var(--color-text-main, #2b2520);
}
</style>
