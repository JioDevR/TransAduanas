function toNumber(val) {
  const num = Number(val)
  return Number.isFinite(num) ? num : 0
}

function sortByDateDesc(list, field) {
  return [...list].sort((a, b) => {
    const da = a?.[field] ? new Date(a[field]).getTime() : 0
    const db = b?.[field] ? new Date(b[field]).getTime() : 0
    return db - da
  })
}

/**
 * Calcula métricas y agrupaciones de cotizaciones para el dashboard B2B.
 * @param {Array} cotizaciones
 */
export function computeCotizacionStats(cotizaciones = []) {
  const list = Array.isArray(cotizaciones) ? cotizaciones : []

  let pendiente = 0
  let aprobada = 0
  let observada = 0
  let rechazada = 0
  let aprobadasPorAceptar = 0
  let totalCotizadoUsd = 0

  for (const item of list) {
    const est = String(item?.estado || 'PENDIENTE').toUpperCase()
    totalCotizadoUsd += toNumber(item?.totalCotizado)

    if (est === 'APROBADA') {
      aprobada += 1
      if (!item?.aceptadaPorCliente) {
        aprobadasPorAceptar += 1
      }
    } else if (est === 'OBSERVADA') {
      observada += 1
    } else if (est === 'RECHAZADA' || est === 'ANULADA') {
      rechazada += 1
    } else {
      pendiente += 1
    }
  }

  const total = list.length
  const aprobacionPct = total > 0 ? Math.round((aprobada / total) * 100) : 0
  const recientes = sortByDateDesc(list, 'fechaEmision').slice(0, 4)

  return {
    total,
    pendiente,
    aprobada,
    observada,
    rechazada,
    aprobadasPorAceptar,
    aprobacionPct,
    totalCotizadoUsd,
    recientes
  }
}

/**
 * Calcula métricas de expedientes aduaneros, etapas y canales SUNAT.
 * @param {Array} expedientes
 */
export function computeExpedienteStats(expedientes = []) {
  const list = Array.isArray(expedientes) ? expedientes : []

  const porEstado = {
    ASIGNADO: 0,
    EN_TRAMITE: 0,
    INSPECCION: 0,
    LEVANTE_AUTORIZADO: 0,
    CERRADO: 0,
    ANULADO: 0
  }

  const canales = {
    VERDE: 0,
    NARANJA: 0,
    ROJO: 0,
    PENDIENTE: 0
  }

  let abiertos = 0
  let conLevante = 0
  let enTransporte = 0
  let alertaAlmacen = 0

  for (const exp of list) {
    const est = String(exp?.estadoExpediente || 'ASIGNADO').toUpperCase()
    if (Object.prototype.hasOwnProperty.call(porEstado, est)) {
      porEstado[est] += 1
    } else {
      porEstado.EN_TRAMITE += 1
    }

    if (est !== 'CERRADO' && est !== 'ANULADO') {
      abiertos += 1
      if (
        exp?.diasRestantesAlmacen !== null &&
        exp?.diasRestantesAlmacen !== undefined &&
        Number(exp.diasRestantesAlmacen) <= 3
      ) {
        alertaAlmacen += 1
      }
    }

    if (Boolean(exp?.fechaLevanteAutorizacion) || est === 'LEVANTE_AUTORIZADO') {
      conLevante += 1
    }

    if (exp?.tieneTransporte && est !== 'CERRADO' && est !== 'ANULADO') {
      enTransporte += 1
    }

    const canal = String(exp?.canalSunat || 'PENDIENTE').toUpperCase()
    if (Object.prototype.hasOwnProperty.call(canales, canal)) {
      canales[canal] += 1
    } else {
      canales.PENDIENTE += 1
    }
  }

  const recientes = sortByDateDesc(list, 'fechaApertura').slice(0, 4)

  return {
    total: list.length,
    abiertos,
    cerrados: porEstado.CERRADO,
    conLevante,
    enTransporte,
    alertaAlmacen,
    porEstado,
    canales,
    recientes
  }
}

/**
 * Consolida el estado financiero USD, comprobantes y próximas obligaciones pendientes.
 * @param {{ resumenPagos?: Object|null, obligaciones?: Array, anticipos?: Array }} params
 */
export function computeCobrosStats({ resumenPagos = null, obligaciones = [], anticipos = [] } = {}) {
  const obList = Array.isArray(obligaciones) ? obligaciones : []
  const antList = Array.isArray(anticipos) ? anticipos : []

  const fallbackCotizado = obList.reduce((acc, o) => acc + toNumber(o?.totalContratadoUsd), 0)
  const fallbackAbonado = obList.reduce((acc, o) => acc + toNumber(o?.totalAmortizadoUsd), 0)
  const fallbackSaldo = obList.reduce((acc, o) => acc + toNumber(o?.saldoPendienteUsd), 0)

  const totalCotizadoUsd = resumenPagos ? toNumber(resumenPagos.totalCotizadoUsd) : fallbackCotizado
  const totalAbonadoUsd = resumenPagos ? toNumber(resumenPagos.totalAbonadoUsd) : fallbackAbonado
  const saldoPendienteUsd = resumenPagos ? toNumber(resumenPagos.saldoPendienteUsd) : fallbackSaldo

  const amortizacionPct = totalCotizadoUsd > 0
    ? Math.min(100, Math.round((totalAbonadoUsd / totalCotizadoUsd) * 100))
    : 0

  let pagosValidados = 0
  let pagosEnRevision = 0
  let pagosObservados = 0

  for (const ant of antList) {
    const est = String(ant?.estadoPago || '').toUpperCase()
    if (est === 'OBSERVADO' || est === 'RECHAZADO') {
      pagosObservados += 1
    } else if (est === 'PENDIENTE_REVISION') {
      pagosEnRevision += 1
    } else if (est === 'APROBADO' || est === 'ADELANTO_APROBADO' || est === 'PAGADO_TOTAL' || est === 'VALIDADO') {
      pagosValidados += 1
    }
  }

  if (resumenPagos?.pagosPendientesRevision != null && antList.length === 0) {
    pagosEnRevision = toNumber(resumenPagos.pagosPendientesRevision)
  }

  const proximasObligaciones = obList
    .filter(o => toNumber(o?.saldoPendienteUsd) > 0)
    .slice(0, 4)

  return {
    totalCotizadoUsd,
    totalAbonadoUsd,
    saldoPendienteUsd,
    amortizacionPct,
    pagosValidados,
    pagosEnRevision,
    pagosObservados,
    totalVouchers: antList.length,
    totalObligacionesPendientes: obList.filter(o => toNumber(o?.saldoPendienteUsd) > 0).length,
    proximasObligaciones
  }
}

export function formatEstadoExpediente(estado) {
  switch (String(estado || '').toUpperCase()) {
    case 'ASIGNADO':
      return 'Asignado'
    case 'EN_TRAMITE':
      return 'En Trámite'
    case 'INSPECCION':
      return 'En Inspección'
    case 'LEVANTE_AUTORIZADO':
      return 'Levante Autorizado'
    case 'CERRADO':
      return 'Cerrado'
    case 'ANULADO':
      return 'Anulado'
    default:
      return estado || 'En Proceso'
  }
}

export function getExpedienteStatusClass(estado) {
  switch (String(estado || '').toUpperCase()) {
    case 'LEVANTE_AUTORIZADO':
    case 'CERRADO':
      return 'success'
    case 'EN_TRAMITE':
    case 'ASIGNADO':
      return 'info'
    case 'INSPECCION':
      return 'warning'
    case 'ANULADO':
      return 'danger'
    default:
      return 'neutral'
  }
}
