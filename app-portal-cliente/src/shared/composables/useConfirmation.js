import { useConfirm } from 'primevue/useconfirm'

/**
 * Composable reutilizable para diálogos de confirmación mediante PrimeVue ConfirmationService.
 * Soporta invocación asíncrona (Promise/await) o mediante callbacks.
 */
export function useConfirmation() {
  const confirm = useConfirm()

  /**
   * Abre un diálogo de confirmación y retorna una Promesa que resuelve a `true` si el usuario acepta o `false` si cancela.
   */
  const confirmAction = (optionsOrMessage = {}, headerArg = 'Confirmar Acción') => {
    const options =
      typeof optionsOrMessage === 'string'
        ? { message: optionsOrMessage, header: headerArg }
        : (optionsOrMessage || {})

    return new Promise((resolve) => {
      confirm.require({
        message: options.message || '¿Está seguro de continuar?',
        header: options.header || 'Confirmar Acción',
        icon: options.icon || 'pi pi-exclamation-triangle',
        acceptLabel: options.acceptLabel || 'Aceptar',
        rejectLabel: options.rejectLabel || 'Cancelar',
        acceptClass: options.acceptClass || 'p-button-danger',
        rejectClass: options.rejectClass || 'p-button-secondary p-button-text',
        accept: async () => {
          if (typeof options.onAccept === 'function') {
            await options.onAccept()
          }
          resolve(true)
        },
        reject: () => {
          if (typeof options.onReject === 'function') {
            options.onReject()
          }
          resolve(false)
        },
        onHide: () => {
          if (typeof options.onHide === 'function') {
            options.onHide()
          }
          resolve(false)
        }
      })
    })
  }

  /**
   * Diálogo especializado para confirmaciones críticas o cancelaciones.
   */
  const confirmDelete = (
    message = '¿Está seguro de que desea eliminar este registro? Esta acción no se puede deshacer.',
    header = 'Confirmar Eliminación'
  ) => {
    return confirmAction({
      message,
      header,
      icon: 'pi pi-trash',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptClass: 'p-button-danger'
    })
  }

  return {
    confirm,
    confirmAction,
    confirmDelete
  }
}

export default useConfirmation
