import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/core/stores/authStore'
import { useMandatoryPasswordForm } from '../useMandatoryPasswordForm'

const mockPush = vi.fn()
const mockShowSuccess = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush })
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: mockShowSuccess
  })
}))

global.localStorage = {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}

describe('useMandatoryPasswordForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('envía contraseñas sin aplicar trim, muestra notificación y navega a /dashboard', async () => {
    const authStore = useAuthStore()
    const cambiarSpy = vi.spyOn(authStore, 'cambiarPassword').mockResolvedValueOnce({ success: true })

    const { form, isFormValid, handleSubmit, errorMessage } = useMandatoryPasswordForm()
    form.passwordActual = ' temp '
    form.nuevaPassword = ' mi clave '
    form.confirmarPassword = ' mi clave '

    expect(isFormValid.value).toBe(true)

    await handleSubmit()

    expect(cambiarSpy).toHaveBeenCalledWith(' temp ', ' mi clave ')
    expect(mockShowSuccess).toHaveBeenCalledWith(
      'Contraseña actualizada. Bienvenido al portal.',
      'Acceso Habilitado'
    )
    expect(mockPush).toHaveBeenCalledWith('/dashboard')
    expect(errorMessage.value).toBe('')
  })

  it('muestra el mensaje de error si cambiarPassword falla y no navega', async () => {
    const authStore = useAuthStore()
    vi.spyOn(authStore, 'cambiarPassword').mockResolvedValueOnce({
      success: false,
      message: 'La contraseña actual ingresada es incorrecta'
    })

    const { form, handleSubmit, errorMessage, loading } = useMandatoryPasswordForm()
    form.passwordActual = 'wrong1'
    form.nuevaPassword = 'nuevaClave1'
    form.confirmarPassword = 'nuevaClave1'

    await handleSubmit()

    expect(errorMessage.value).toBe('La contraseña actual ingresada es incorrecta')
    expect(mockShowSuccess).not.toHaveBeenCalled()
    expect(mockPush).not.toHaveBeenCalled()
    expect(loading.value).toBe(false)
  })

  it('handleLogout limpia sesión y redirige a /login', async () => {
    const authStore = useAuthStore()
    const logoutSpy = vi.spyOn(authStore, 'logout')

    const { handleLogout } = useMandatoryPasswordForm()
    await handleLogout()

    expect(logoutSpy).toHaveBeenCalledTimes(1)
    expect(mockPush).toHaveBeenCalledWith('/login')
  })
})
