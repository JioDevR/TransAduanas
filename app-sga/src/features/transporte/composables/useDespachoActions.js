import { transporteFlotaService } from '../services/transporteFlotaService'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { useNotification } from '@/shared/composables/useNotification'

export function useDespachoActions({ onReload } = {}) {
  const { confirmAction } = useConfirmation()
  const { showSuccess, showError } = useNotification()

  const aceptarSolicitud = (despacho) => {
    if (!despacho?.idDespachoTransporte) return
    confirmAction({
      header: 'Aceptar Solicitud de Transporte',
      message: `¿Confirma la aceptación de la solicitud ${despacho.codigoTracking} para programar la unidad y conductor?`,
      acceptLabel: 'Aceptar Solicitud',
      rejectLabel: 'Cancelar',
      onAccept: async () => {
        try {
          await transporteFlotaService.aceptarSolicitud(despacho.idDespachoTransporte)
          showSuccess(`Solicitud ${despacho.codigoTracking} aceptada por Transporte. Ahora puede asignar unidad y conductor.`)
          await onReload?.()
        } catch (err) {
          showError(err?.response?.data?.message || err?.message || 'No se pudo aceptar la solicitud de transporte')
        }
      }
    })
  }

  const iniciarRuta = (despacho) => {
    if (!despacho?.idDespachoTransporte) return
    confirmAction({
      header: 'Confirmar Salida a Ruta',
      message: `¿Autorizar salida a ruta del despacho ${despacho.codigoTracking} con la unidad ${despacho.unidadPlacaTracto || 'asignada'}? Se notificará al cliente B2B.`,
      acceptLabel: 'Iniciar Ruta',
      rejectLabel: 'Cancelar',
      onAccept: async () => {
        try {
          await transporteFlotaService.iniciarRuta(despacho.idDespachoTransporte)
          showSuccess(`Despacho ${despacho.codigoTracking} en tránsito activo (EN RUTA).`)
          await onReload?.()
        } catch (err) {
          showError(err?.response?.data?.message || err?.message || 'No se pudo iniciar la ruta del despacho')
        }
      }
    })
  }

  const anularDespacho = (despacho) => {
    if (!despacho?.idDespachoTransporte) return
    confirmAction({
      header: 'Anular Despacho Terrestre',
      message: `¿Desea cancelar el despacho ${despacho.codigoTracking}? Los recursos asignados serán liberados y el expediente quedará habilitado para una nueva programación.`,
      acceptLabel: 'Anular Despacho',
      rejectLabel: 'Volver',
      acceptClass: 'p-button-danger',
      onAccept: async () => {
        try {
          await transporteFlotaService.deleteDespacho(despacho.idDespachoTransporte)
          showSuccess(`Despacho ${despacho.codigoTracking} anulado correctamente.`)
          await onReload?.()
        } catch (err) {
          showError(err?.response?.data?.message || err?.message || 'No se pudo anular el despacho')
        }
      }
    })
  }

  return {
    aceptarSolicitud,
    iniciarRuta,
    anularDespacho
  }
}
