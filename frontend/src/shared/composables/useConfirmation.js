import { useConfirm } from 'primevue/useconfirm'

/**
 * Composable reutilizable para confirmaciones mediante PrimeVue ConfirmationService.
 * Soporta invocación asíncrona (con await) o por callbacks convencionales.
 */
export function useConfirmation() {
  const confirm = useConfirm()

  /**
   * Abre un diálogo de confirmación y retorna una Promesa que resuelve a `true` si el usuario acepta o `false` si cancela.
   */
  const confirmAction = ({
    message,
    header = 'Confirmar Acción',
    icon = 'pi pi-exclamation-triangle',
    acceptLabel = 'Aceptar',
    rejectLabel = 'Cancelar',
    acceptClass = 'p-button-danger',
    rejectClass = 'p-button-secondary p-button-text'
  }) => {
    return new Promise((resolve) => {
      confirm.require({
        message,
        header,
        icon,
        acceptLabel,
        rejectLabel,
        acceptClass,
        rejectClass,
        accept: () => resolve(true),
        reject: () => resolve(false),
        onHide: () => resolve(false)
      })
    })
  }

  /**
   * Diálogo especializado para eliminaciones / bajas lógicas.
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
