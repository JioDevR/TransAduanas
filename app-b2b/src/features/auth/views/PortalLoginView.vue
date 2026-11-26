<template>
  <div class="sga-auth-page">
    <div class="sga-auth-wrapper">
      <div class="sga-auth-card">
        <!-- Logo y Cabecera de Marca -->
        <div class="sga-auth-brand">
          <div class="sga-auth-logo" title="TransAduanas - Portal de Clientes">
            <i class="pi pi-shield"></i>
          </div>
          <h1 class="sga-auth-title">TransAduanas</h1>
          <Tag value="PORTAL DEL CLIENTE B2B" severity="secondary" class="sga-auth-kicker" />
          <p class="sga-auth-subtitle">Plataforma de Consulta y Seguimiento Aduanero</p>
        </div>

        <!-- Mensaje de Error -->
        <Message v-if="errorMessage" severity="error" :closable="false" class="sga-auth-alert">
          {{ errorMessage }}
        </Message>

        <!-- Formulario -->
        <form @submit.prevent="handleSubmit" class="sga-auth-form">
          <div class="sga-auth-field">
            <label for="username" class="sga-auth-label">RUC o Usuario Empresarial</label>
            <IconField>
              <InputIcon class="pi pi-building" />
              <InputText
                id="username"
                v-model.trim="form.username"
                type="text"
                placeholder="Ej. 20511894520 o lgelectronics"
                autocomplete="username"
                required
                :disabled="loading"
                fluid
              />
            </IconField>
            <small class="sga-auth-hint">
              Puede ingresar con su número de RUC de 11 dígitos o su usuario registrado.
            </small>
          </div>

          <div class="sga-auth-field">
            <label for="password" class="sga-auth-label">Contraseña</label>
            <IconField>
              <InputIcon class="pi pi-lock" />
              <Password
                inputId="password"
                v-model="form.password"
                placeholder="••••••••"
                :feedback="false"
                toggleMask
                :disabled="loading"
                :inputProps="{ autocomplete: 'current-password', required: true }"
                fluid
              />
            </IconField>
          </div>

          <div class="sga-auth-options">
            <div class="sga-auth-checkbox-row">
              <Checkbox inputId="remember-ruc" v-model="rememberMe" binary :disabled="loading" />
              <label for="remember-ruc">Recordar RUC en este equipo</label>
            </div>
          </div>

          <Button
            type="submit"
            id="btn-login-submit"
            label="Ingresar al Portal Empresarial"
            icon="pi pi-arrow-right"
            iconPos="right"
            :loading="loading"
            :disabled="loading || !isFormValid"
            class="sga-auth-submit"
            fluid
          />
        </form>

        <div class="sga-auth-footer">
          ¿Problemas para acceder a su cuenta empresarial? Contacte a su Agente Comercial asignado o escríbanos a
          <strong>soporte@transaduanas.pe</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Password from 'primevue/password'
import Tag from 'primevue/tag'
import { useAuthStore } from '@/core/stores/authStore'
import '../../../../../sgaUI/sga-auth.css'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: ''
})

const rememberMe = ref(true)
const errorMessage = ref('')
const loading = ref(false)

const isFormValid = computed(() => {
  return form.username.trim().length > 0 && form.password.length > 0
})

onMounted(() => {
  if (typeof localStorage === 'undefined') return
  const savedRuc = localStorage.getItem('sga_portal_remember_ruc')
  if (savedRuc) {
    form.username = savedRuc
  }
})

async function handleSubmit() {
  if (!isFormValid.value || loading.value) return

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await authStore.login(form.username, form.password)
    if (res.success) {
      if (typeof localStorage !== 'undefined') {
        if (rememberMe.value) {
          localStorage.setItem('sga_portal_remember_ruc', form.username)
        } else {
          localStorage.removeItem('sga_portal_remember_ruc')
        }
      }
      await router.push('/dashboard')
    } else {
      errorMessage.value = res.message || 'Credenciales incorrectas'
    }
  } catch {
    errorMessage.value = 'Ocurrió un error inesperado al conectar con el servidor'
  } finally {
    loading.value = false
  }
}
</script>
