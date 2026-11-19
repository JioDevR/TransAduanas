<template>
  <div class="sga-auth-page">
    <div class="sga-auth-wrapper">
      <div class="sga-auth-card">
        <!-- Encabezado de Marca -->
        <div class="sga-auth-brand">
          <div class="sga-auth-logo" title="TransAduanas SGA">
            <i class="pi pi-shield"></i>
          </div>
          <h1 class="sga-auth-title">TransAduanas - SGA</h1>
          <p class="sga-auth-subtitle">Sistema de Gestión Aduanera & Logística</p>
        </div>

        <!-- Mensaje de Error -->
        <Message v-if="errorMessage" severity="error" :closable="false" class="sga-auth-alert">
          {{ errorMessage }}
        </Message>

        <!-- Formulario -->
        <form @submit.prevent="handleSubmit" class="sga-auth-form">
          <div class="sga-auth-field">
            <label for="username" class="sga-auth-label">Usuario</label>
            <IconField>
              <InputIcon class="pi pi-user" />
              <InputText
                id="username"
                v-model.trim="form.username"
                type="text"
                placeholder="Ej. admin"
                autocomplete="username"
                required
                :disabled="loading"
                fluid
              />
            </IconField>
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
                required
                :disabled="loading"
                :inputProps="{ autocomplete: 'current-password' }"
                fluid
              />
            </IconField>
          </div>

          <Button
            type="submit"
            id="btn-login-submit"
            label="Iniciar Sesión"
            :loading="loading"
            :disabled="loading || !isFormValid"
            class="sga-auth-submit"
            fluid
          />
        </form>

        <div class="sga-auth-footer">
          TransAduanas SGA &copy; 2026 &bull; Acceso Restringido y Auditado
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export function resolvePostLoginRoute(redirectQuery, fallbackPath = '/login') {
  const redirectParam =
    typeof redirectQuery === 'string' &&
    redirectQuery.startsWith('/') &&
    !redirectQuery.startsWith('//')
      ? redirectQuery
      : null
  return redirectParam && redirectParam !== '/login' ? redirectParam : fallbackPath
}
</script>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Button from 'primevue/button'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Password from 'primevue/password'
import { useAuthStore } from '../stores/authStore'
import '../../../../../sgaUI/sga-auth.css'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: ''
})

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

    const targetPath = resolvePostLoginRoute(route.query.redirect, authStore.homePath)
    await router.push(targetPath)
  } catch (err) {
    errorMessage.value = err.message || 'Credenciales inválidas o servidor no disponible.'
  }
}
</script>
