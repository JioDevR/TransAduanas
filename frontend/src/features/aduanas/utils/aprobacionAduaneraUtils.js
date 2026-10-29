/**
 * Retorna la configuración de la notificación Toast con las credenciales del cliente
 * tras la aprobación técnica de una cotización aduanera.
 *
 * @param {Object} cotizacion - Datos de la cotización aprobada
 * @returns {{ title: string, message: string, life: number }}
 */
export function getCredencialesPostAprobacionNotification(cotizacion = {}) {
  const username = cotizacion?.clienteNumeroDocumento || 'RUC del cliente'

  if (!cotizacion?.clienteTieneUsuario) {
    return {
      title: 'Credenciales de Acceso Creadas',
      message: `Se aprobó el expediente y se generaron las credenciales de acceso para ${username}.`,
      life: 8000
    }
  }

  return {
    title: 'Acceso Portal B2B',
    message: `El cliente ya cuenta con credenciales activas en el Portal B2B (Usuario: ${username}).`,
    life: 5000
  }
}
