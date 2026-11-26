/**
 * Reglas puras de flujo aduanero y visibilidad de acciones para el Agente Aduanero.
 * Fuente única de verdad compartida entre composables, tabla, canvas e informe.
 */

export function isExportacion(exp) {
  return (exp?.regimenAduanero || '').toUpperCase().includes('EXPORT')
}

export function isExpedienteCerrado(exp) {
  const est = exp?.estadoExpediente
  return est === 'CERRADO' || est === 'ANULADO'
}

export function hasCanalAsignado(exp) {
  const c = exp?.canalSunat
  return c === 'VERDE' || c === 'NARANJA' || c === 'ROJO'
}

export function hasArriboConfirmado(exp) {
  return Boolean(exp?.fechaArriboAduana)
}

export function hasZarpeConfirmado(exp) {
  if (!exp) return false
  const obs = exp.observaciones || ''
  if (obs.includes('SALIDA ORIGEN CONFIRMADA')) return true
  if (hasArriboConfirmado(exp) || hasCanalAsignado(exp)) return true
  const est = exp.estadoExpediente
  return est === 'EN_TRAMITE' || est === 'INSPECCION' || est === 'LEVANTE_AUTORIZADO' || est === 'CERRADO'
}

export function isLevanteAutorizado(exp) {
  return exp?.estadoExpediente === 'LEVANTE_AUTORIZADO' || Boolean(exp?.fechaLevanteAutorizacion)
}

export function isDiligenciaCompletada(exp) {
  if (!exp) return false
  if (exp.canalSunat === 'VERDE') return true
  return Boolean(exp.detalleInspeccion) || isLevanteAutorizado(exp)
}

export function isEmbarqueConfirmado(exp) {
  const obs = exp?.observaciones || ''
  return obs.includes('EMBARQUE CONFIRMADO') || obs.includes('EMBARCADO')
}

export function isRecepcionExteriorConfirmada(exp) {
  const obs = exp?.observaciones || ''
  return obs.includes('RECEPCIÓN DESTINO EXTERIOR')
}

export function isRetiroDirectoRegistrado(exp) {
  const obs = exp?.observaciones || ''
  return obs.includes('RETIRO DIRECTO CLIENTE')
}

/**
 * Determina si un expediente pertenece a la cola operativa indicada.
 * @param {Object} exp
 * @param {'todos'|'expedientes'|'transito'|'inspeccion'|'levante'} tab
 */
export function belongsToAgentePhase(exp, tab = 'todos') {
  if (!exp) return false
  const normalized = !tab || tab === 'expedientes' ? 'todos' : tab
  const cerrado = isExpedienteCerrado(exp)
  const levante = isLevanteAutorizado(exp)

  switch (normalized) {
    case 'todos':
      return true
    case 'transito':
      return exp.estadoExpediente === 'ASIGNADO' || exp.estadoExpediente === 'EN_TRAMITE'
    case 'inspeccion':
      if (cerrado || levante) return false
      return exp.canalSunat === 'ROJO' || exp.canalSunat === 'NARANJA' || exp.estadoExpediente === 'INSPECCION'
    case 'levante':
      return !cerrado && levante
    default:
      return true
  }
}

/**
 * Calcula qué acciones operativas están habilitadas para un expediente.
 * @param {Object} exp
 */
export function getAccionesPermitidasAgente(exp = {}) {
  const isExport = isExportacion(exp)
  const isCerrado = isExpedienteCerrado(exp)
  const hasZarpe = hasZarpeConfirmado(exp)
  const hasArribo = hasArriboConfirmado(exp)
  const hasCanal = hasCanalAsignado(exp)
  const hasLevante = isLevanteAutorizado(exp)

  const puedeSalidaOrigen = !isCerrado && !hasLevante && !isExport && !hasZarpe
  const puedeConfirmarArribo = !isCerrado && !hasLevante && !isExport && hasZarpe && !hasArribo
  const puedeEditarTransito = !isCerrado && !hasLevante && (isExport || hasZarpe)

  const puedeRecojoExportacion =
    !isCerrado &&
    isExport &&
    Boolean(exp.servicioTransporte) &&
    !exp.idDespachoTransporte &&
    (exp.estadoExpediente === 'ASIGNADO' || exp.estadoExpediente === 'EN_TRAMITE')

  const puedeIngresoOrigen =
    !isCerrado &&
    isExport &&
    (exp.estadoExpediente === 'ASIGNADO' || (exp.estadoExpediente === 'EN_TRAMITE' && !hasArribo))

  const puedeDiligencia =
    !isCerrado &&
    !hasLevante &&
    (exp.canalSunat === 'ROJO' || exp.canalSunat === 'NARANJA' || exp.estadoExpediente === 'INSPECCION')

  const puedeSolicitarTransporte =
    !isCerrado && !isExport && hasLevante && Boolean(exp.servicioTransporte) && !exp.idDespachoTransporte

  const puedeRetiroDirecto =
    !isCerrado && !isExport && hasLevante && !exp.servicioTransporte && !isRetiroDirectoRegistrado(exp)

  const puedeIngresoAlmacen = !isCerrado && !isExport && (hasArribo || hasLevante)

  const puedeEmbarque = !isCerrado && isExport && hasLevante && !isEmbarqueConfirmado(exp)

  const puedeRecepcionExterior =
    !isCerrado && isExport && hasLevante && isEmbarqueConfirmado(exp) && !isRecepcionExteriorConfirmada(exp)

  const puedeVerificacionOrigen = !isCerrado && !isExport && !exp.verificacionOrigenOk && !hasArribo
  const puedeMarcarRevisionAduana = !isCerrado && isExport && !exp.enRevisionAduana && !hasLevante
  const puedeMarcarEsperaTransito = !isCerrado && isExport && !exp.enEsperaTransito && !isEmbarqueConfirmado(exp)
  const puedeCuadrillaCitaRojo = !isCerrado && !isExport && exp.canalSunat === 'ROJO' && !exp.cuadrillaSolicitada && !hasLevante
  const puedeFinalizarRojo = !isCerrado && !isExport && exp.canalSunat === 'ROJO' && !hasLevante
  const puedeEnviarSunatNaranja = !isCerrado && !isExport && exp.canalSunat === 'NARANJA' && !exp.envioSunatSimuladoOk && !hasLevante
  const puedeEntregadoAlmacenVerde = !isCerrado && !isExport && exp.canalSunat === 'VERDE' && !exp.entregadoAlmacenVerde
  const puedeInformeConformidad = !isCerrado && !isExport && hasLevante && !exp.informeConformidadTexto
  const tieneInformeConformidad = Boolean(exp.informeConformidadTexto)
  const puedeFinalizarExpediente = !isCerrado && hasLevante && (!isExport || isRecepcionExteriorConfirmada(exp))

  return {
    isExport,
    isCerrado,
    hasZarpe,
    hasArribo,
    hasCanal,
    hasLevante,
    esperandoCanal: !isCerrado && hasArribo && !hasCanal,
    puedeSalidaOrigen,
    puedeConfirmarArribo,
    puedeEditarTransito,
    puedeRecojoExportacion,
    puedeIngresoOrigen,
    puedeDiligencia,
    puedeSolicitarTransporte,
    puedeRetiroDirecto,
    puedeIngresoAlmacen,
    puedeEmbarque,
    puedeRecepcionExterior,
    puedeVerificacionOrigen,
    puedeMarcarRevisionAduana,
    puedeMarcarEsperaTransito,
    puedeCuadrillaCitaRojo,
    puedeFinalizarRojo,
    puedeEnviarSunatNaranja,
    puedeEntregadoAlmacenVerde,
    puedeInformeConformidad,
    tieneInformeConformidad,
    puedeFinalizarExpediente
  }
}

/**
 * Extrae el último terminal portuario/depósito registrado en la bitácora del expediente.
 * @param {string} observaciones
 * @returns {string|null}
 */
export function extractTerminalBitacora(observaciones) {
  if (!observaciones) return null
  const text = String(observaciones)
  const lines = text.split('\n').reverse()

  for (const line of lines) {
    const arriboMatch = line.match(/\[ARRIBO A PUERTO DE DESTINO\].*?en terminal:\s*([^.\n]+)/i)
    if (arriboMatch && arriboMatch[1]) {
      const cleaned = arriboMatch[1].replace(/\. Detalle:.*$/i, '').trim()
      if (cleaned) return cleaned
    }
    const ingresoMatch = line.match(/\[INGRESO A ZONA PRIMARIA[^\]]*\]\s*Terminal:\s*([^|\n]+)/i)
    if (ingresoMatch && ingresoMatch[1]) {
      const cleaned = ingresoMatch[1].trim()
      if (cleaned) return cleaned
    }
  }
  return null
}
