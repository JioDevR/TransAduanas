import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/core/stores/authStore'
import { useNotification } from '@/shared/composables/useNotification'
import { evaluatePasswordRules, isPasswordChangeValid } from '../utils/passwordRules'

/**
 * Composable de estado, validación y envío para el cambio obligatorio de contraseña B2B.
 */
export function useMandatoryPasswordForm() {
  const router = useRouter()
  const authStore = useAuthStore()
  const { showSuccess } = useNotification()

  const loading = ref(false)
  const errorMessage = ref('')

  const form = reactive({
    passwordActual: '',
    nuevaPassword: '',
    confirmarPassword: ''
  })

  const rules = computed(() => evaluatePasswordRules(form))
  const isFormValid = computed(() => isPasswordChangeValid(form))

  const handleSubmit = async () => {
    errorMessage.value = ''
    if (!isFormValid.value || loading.value) return

    loading.value = true
    try {
      const result = await authStore.cambiarPassword(form.passwordActual, form.nuevaPassword)
      if (result.success) {
        showSuccess('Contraseña actualizada. Bienvenido al portal.', 'Acceso Habilitado')
        await router.push('/dashboard')
      } else {
        errorMessage.value = result.message || 'Error al actualizar la contraseña'
      }
    } catch (err) {
      errorMessage.value = err.message || 'Error de comunicación con el servidor'
    } finally {
      loading.value = false
    }
  }

  const handleLogout = async () => {
    authStore.logout()
    await router.push('/login')
  }

  return {
    authStore,
    form,
    rules,
    isFormValid,
    loading,
    errorMessage,
    handleSubmit,
    handleLogout
  }
}
