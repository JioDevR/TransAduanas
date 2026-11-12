/**
 * Utilidades de flujo de trabajo y clasificación para Gestión Aduanera.
 */

/**
 * Clasifica una lista de cotizaciones en las 4 sub-colas operativas más el histórico.
 *
 * @param {Array} cotizaciones
 * @returns {{
 *   nuevas: Array,
 *   subsanadas: Array,
 *   pendientesPago: Array,
 *   listasApertura: Array,
 *   historico: Array
 * }}
 */
export function clasificarCotizacionesAduaneras(cotizaciones = []) {
  if (!Array.isArray(cotizaciones)) {
    return {
      nuevas: [],
      subsanadas: [],
      pendientesPago: [],
      listasApertura: [],
      historico: []
    }
  }

  const nuevas = []
  const subsanadas = []
  const pendientesPago = []
  const listasApertura = []
  const historico = []

  cotizaciones.forEach((c) => {
    if (!c) return

    // 1. Histórico: rechazadas, anuladas o que ya tienen un expediente formalizado
    if (['RECHAZADA', 'ANULADA'].includes(c.estado) || c.tieneExpediente) {
      historico.push(c)
      return
    }

    // 2. Aprobadas
    if (c.estado === 'APROBADA') {
      if (c.tienePagoAprobado) {
        listasApertura.push(c)
      } else {
        pendientesPago.push(c)
      }
      return
    }

    // 3. Subsanaciones / Reingresos: observadas o pendientes que provienen de una observación previa
    const esSubsanada =
      c.estado === 'OBSERVADA' ||
      (c.estado === 'PENDIENTE' && (Boolean(c.motivoObservacion) || Boolean(c.fechaRevision)))

    if (esSubsanada) {
      subsanadas.push(c)
      return
    }

    // 4. Nuevas para primera revisión técnica
    if (c.estado === 'PENDIENTE' || c.estado === 'ENVIADA' || c.estado === 'EN_REVISION') {
      nuevas.push(c)
    } else {
      historico.push(c)
    }
  })

  return {
    nuevas,
    subsanadas,
    pendientesPago,
    listasApertura,
    historico
  }
}

/**
 * Filtra expedientes por fase operativa de despacho.
 *
 * @param {Array} expedientes
 * @param {string} fase - 'TODOS' | 'PENDIENTE_CANAL' | 'CONTROL_SUNAT' | 'LEVANTE_AUTORIZADO' | 'CERRADOS'
 * @returns {Array}
 */
export function filtrarExpedientesPorFase(expedientes = [], fase = 'TODOS') {
  if (!Array.isArray(expedientes)) return []
  if (!fase || fase === 'TODOS') return expedientes

  return expedientes.filter((exp) => {
    if (!exp) return false

    const esCerrado = exp.estadoExpediente === 'CERRADO' || exp.estadoExpediente === 'ANULADO'
    const tieneLevante = Boolean(exp.fechaLevanteAutorizacion) || exp.estadoExpediente === 'LEVANTE_AUTORIZADO'
    const tieneCanalAsignado = ['VERDE', 'NARANJA', 'ROJO'].includes(exp.canalSunat)

    switch (fase) {
      case 'CERRADOS':
        return esCerrado

      case 'LEVANTE_AUTORIZADO':
        return tieneLevante && !esCerrado

      case 'CONTROL_SUNAT':
        return tieneCanalAsignado && !tieneLevante && !esCerrado

      case 'PENDIENTE_CANAL':
        return (!exp.canalSunat || exp.canalSunat === 'PENDIENTE') && !tieneLevante && !esCerrado

      default:
        return true
    }
  })
}

/**
 * Determina qué acciones están permitidas para un expediente según su estado.
 *
 * @param {Object} exp
 * @returns {{
 *   puedeAuditar: boolean,
 *   puedeDocs: boolean,
 *   puedeActualizarTransito: boolean,
 *   puedeAsignarCanal: boolean,
 *   puedeLevante: boolean,
 *   puedeRetiro: boolean,
 *   puedeCerrar: boolean
 * }}
 */
export function obtenerAccionesPermitidasExpediente(exp = {}) {
  if (!exp) {
    return {
      puedeAuditar: false,
      puedeDocs: false,
      puedeActualizarTransito: false,
      puedeAsignarCanal: false,
      puedeLevante: false,
      puedeRetiro: false,
      puedeCerrar: false
    }
  }

  const esCerrado = exp.estadoExpediente === 'CERRADO' || exp.estadoExpediente === 'ANULADO'
  const tieneLevante = Boolean(exp.fechaLevanteAutorizacion) || exp.estadoExpediente === 'LEVANTE_AUTORIZADO'
  const esExportacion = Boolean(
    (exp.regimenAduanero && exp.regimenAduanero.toUpperCase().includes('EXPORT')) ||
    (exp.cotizacion?.regimenAduanero && exp.cotizacion.regimenAduanero.toUpperCase().includes('EXPORT'))
  )
  const haArribado = Boolean(exp.fechaArriboAduana)

  return {
    puedeAuditar: true,
    puedeDocs: true,
    puedeActualizarTransito: !esCerrado && !tieneLevante,
    puedeAsignarCanal: !esCerrado && !tieneLevante && (esExportacion || haArribado),
    requiereArriboParaCanal: !esCerrado && !tieneLevante && !esExportacion && !haArribado,
    esExportacion,
    haArribado,
    puedeLevante: !esCerrado && !tieneLevante,
    puedeRetiro: !esCerrado && tieneLevante && !exp.servicioTransporte,
    puedeCerrar: !esCerrado && tieneLevante
  }
}
