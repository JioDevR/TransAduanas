/**
 * Utilidades para supervisión aduanera de cotizaciones.
 */

/**
 * Filtra las cotizaciones visibles en la supervisión aduanera.
 * Excluye aquellas solicitudes provenientes del portal B2B u otros orígenes
 * que aún no cuenten con asesor comercial asignado o cuyo total cotizado sea $0.00.
 *
 * @param {Array} cotizaciones
 * @returns {Array}
 */
export function filtrarCotizacionesParaSupervision(cotizaciones = []) {
  if (!Array.isArray(cotizaciones)) {
    return []
  }
  return cotizaciones.filter(
    (c) => !!c?.idUsuarioComercial && Number(c?.totalCotizado || 0) > 0
  )
}
