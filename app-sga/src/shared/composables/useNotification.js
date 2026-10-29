import { useToast } from 'primevue/usetoast'

/**
 * Composable reutilizable para notificaciones Toast mediante PrimeVue ToastService.
 */
export function useNotification() {
  const toast = useToast()

  const showSuccess = (message, title = 'Operación Exitosa', life = 3500) => {
    toast.add({
      severity: 'success',
      summary: title,
      detail: message,
      life
    })
  }

  const showError = (message, title = 'Error', life = 5000) => {
    toast.add({
      severity: 'error',
      summary: title,
      detail: message || 'Ocurrió un error inesperado al procesar la solicitud.',
      life
    })
  }

  const showInfo = (message, title = 'Información', life = 3500) => {
    toast.add({
      severity: 'info',
      summary: title,
      detail: message,
      life
    })
  }

  const showWarn = (message, title = 'Advertencia', life = 4000) => {
    toast.add({
      severity: 'warn',
      summary: title,
      detail: message,
      life
    })
  }

  return {
    toast,
    showSuccess,
    showError,
    showInfo,
    showWarn
  }
}

export default useNotification
