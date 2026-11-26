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
 * @param {'todos'|'expedientes'|'transito'|'inspeccion'|'levante'|'cerrados'} tab
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
    case 'cerrados':
      return cerrado
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
    !exp.transporteSolicitado &&
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
    !isCerrado && !isExport && hasLevante && Boolean(exp.servicioTransporte) && !exp.transporteSolicitado

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
 * Construye el flujo secuencial de hitos ejecutables por el Agente Aduanero
 * y su impacto directo en el Tracking del Portal B2B.
 * @param {Object} exp
 */
export function buildAgenteChecklistHitos(exp = {}) {
  const acc = getAccionesPermitidasAgente(exp)
  const { isExport, isCerrado, hasZarpe, hasArribo, hasCanal, hasLevante } = acc

  if (isExport) {
    const embarcado = isEmbarqueConfirmado(exp)
    const recepcionExterior = isRecepcionExteriorConfirmada(exp)

    const hito1Secondary = []
    if (acc.puedeRecojoExportacion && acc.puedeIngresoOrigen) {
      hito1Secondary.push({ event: 'ingreso-origen', label: 'Ingreso Almacén Central', icon: 'pi pi-inbox', severity: 'secondary' })
    }

    const hito1Action = acc.puedeRecojoExportacion
      ? { event: 'recojo-exportacion', label: 'Solicitar Recojo en Planta', icon: 'pi pi-truck', severity: 'primary' }
      : (acc.puedeIngresoOrigen
        ? { event: 'ingreso-origen', label: 'Confirmar Ingreso a Almacén Central', icon: 'pi pi-inbox', severity: 'primary' }
        : null)

    const hito2Secondary = []
    if (acc.puedeMarcarRevisionAduana) {
      hito2Secondary.push({
        event: 'marcar-estado',
        payload: { codigoEstado: 'REVISION_ADUANA', titulo: 'Revisión Aduana' },
        label: 'Marcar Revisión Aduana',
        icon: 'pi pi-search',
        severity: 'info'
      })
    }
    if (acc.puedeEditarTransito) {
      hito2Secondary.push({ event: 'transito', label: 'Editar Tránsito', icon: 'pi pi-globe', severity: 'secondary' })
    }

    const hito3Secondary = []
    if (acc.puedeMarcarEsperaTransito) {
      hito3Secondary.push({
        event: 'marcar-estado',
        payload: { codigoEstado: 'ESPERA_TRANSITO', titulo: 'En Espera de Tránsito' },
        label: 'Marcar Espera de Tránsito',
        icon: 'pi pi-clock',
        severity: 'warn'
      })
    }

    const hito4Action = acc.puedeRecepcionExterior
      ? { event: 'recepcion-exterior', label: 'Confirmar Recepción en Destino', icon: 'pi pi-check-circle', severity: 'success' }
      : (acc.puedeFinalizarExpediente
        ? { event: 'finalizar-expediente', label: 'Finalizar Expediente', icon: 'pi pi-lock', severity: 'success' }
        : null)

    return [
      {
        id: 'exp-ingreso',
        num: 1,
        kicker: 'HITO 01 · ORIGEN',
        title: 'Recojo & Almacén Central',
        description: hasArribo
          ? 'Carga recibida en almacén central / terminal de exportación.'
          : 'Coordine el recojo en planta del exportador o registre el ingreso directo al terminal.',
        b2bEffect: 'Actualiza los hitos "Expediente & Recojo" y "Almacén Central" en el Portal B2B.',
        status: hasArribo ? 'completed' : (!isCerrado ? 'current' : 'pending'),
        date: exp.fechaArriboAduana || null,
        action: hito1Action,
        secondaryActions: hito1Secondary
      },
      {
        id: 'exp-aduana',
        num: 2,
        kicker: 'HITO 02 · SUNAT',
        title: 'Revisión Aduanera & Levante',
        description: hasLevante
          ? `Levante autorizado (${exp.canalSunat || 'SUNAT'}).`
          : (hasCanal
            ? `Canal ${exp.canalSunat} asignado; registre acta de diligencia si aplica.`
            : 'En espera de numeración DAM / Canal SUNAT por Gestión Aduanera.'),
        b2bEffect: 'Muestra el Canal SUNAT y la autorización de levante en tiempo real al cliente.',
        status: hasLevante ? 'completed' : (hasArribo && !isCerrado ? (hasCanal ? 'current' : 'waiting') : 'pending'),
        date: exp.fechaLevanteAutorizacion || exp.fechaAsignacionCanal || null,
        action: acc.puedeDiligencia
          ? { event: 'diligencia', label: 'Registrar Diligencia SUNAT', icon: 'pi pi-search-plus', severity: 'warn' }
          : null,
        secondaryActions: hito2Secondary
      },
      {
        id: 'exp-embarque',
        num: 3,
        kicker: 'HITO 03 · SALIDA',
        title: 'Vuelo / Embarque Internacional',
        description: embarcado
          ? `Embarque confirmado (${exp.numeroVuelo || exp.naveViaje || 'Despachado'}).`
          : 'Confirme la nave o vuelo de salida internacional una vez otorgado el levante.',
        b2bEffect: 'Activa el hito "Vuelo / Embarque" con nave, vuelo y ETD en el Portal B2B.',
        status: embarcado ? 'completed' : (hasLevante && !isCerrado ? 'current' : 'pending'),
        date: embarcado ? exp.fechaZarpeEtd : null,
        action: acc.puedeEmbarque
          ? { event: 'embarque', label: 'Confirmar Vuelo / Embarque', icon: 'pi pi-send', severity: 'success' }
          : null,
        secondaryActions: hito3Secondary
      },
      {
        id: 'exp-cierre',
        num: 4,
        kicker: 'HITO 04 · DESTINO',
        title: 'Recepción Exterior & Cierre',
        description: isCerrado
          ? 'Expediente cerrado conforme y archivado.'
          : (recepcionExterior
            ? 'Conformidad en destino registrada; listo para cierre formal.'
            : 'Registre la conformidad en país de destino y cierre el expediente.'),
        b2bEffect: 'Completa el último hito "Destino & Finalizado" del tracking B2B.',
        status: isCerrado ? 'completed' : (embarcado ? 'current' : 'pending'),
        date: exp.fechaCierre || null,
        action: hito4Action,
        secondaryActions: []
      }
    ]
  }

  // Flujo Importación
  const hito1Secondary = []
  if (acc.puedeVerificacionOrigen) {
    hito1Secondary.push({ event: 'verificacion-origen', label: 'Verificar en Origen', icon: 'pi pi-check-square', severity: 'info' })
  }
  if (acc.puedeEditarTransito && !hasArribo) {
    hito1Secondary.push({ event: 'transito', label: 'Editar Tránsito', icon: 'pi pi-globe', severity: 'secondary' })
  }

  const hito2Secondary = []
  if (acc.puedeEditarTransito && hasArribo && !hasLevante) {
    hito2Secondary.push({ event: 'transito', label: 'Actualizar Tránsito', icon: 'pi pi-globe', severity: 'secondary' })
  }

  let hito3Action = null
  const hito3Secondary = []
  if (hasCanal && !hasLevante && !isCerrado) {
    if (exp.canalSunat === 'ROJO') {
      if (acc.puedeCuadrillaCitaRojo) {
        hito3Action = { event: 'cuadrilla-rojo', label: 'Solicitar Cuadrilla / Cita Rojo', icon: 'pi pi-users', severity: 'danger' }
        hito3Secondary.push({ event: 'finalizar-rojo', label: 'Finalizar Revisión Rojo', icon: 'pi pi-check-circle', severity: 'danger' })
      } else if (acc.puedeFinalizarRojo) {
        hito3Action = { event: 'finalizar-rojo', label: 'Finalizar Revisión Rojo', icon: 'pi pi-check-circle', severity: 'danger' }
      }
      if (acc.puedeDiligencia) {
        hito3Secondary.push({ event: 'diligencia', label: 'Registrar Diligencia', icon: 'pi pi-search-plus', severity: 'warn' })
      }
    } else if (exp.canalSunat === 'NARANJA') {
      if (acc.puedeEnviarSunatNaranja) {
        hito3Action = { event: 'enviar-sunat', label: 'Transmitir Docs a SUNAT (Naranja)', icon: 'pi pi-cloud-upload', severity: 'warn' }
        if (acc.puedeDiligencia) {
          hito3Secondary.push({ event: 'diligencia', label: 'Registrar Diligencia', icon: 'pi pi-search-plus', severity: 'warn' })
        }
      } else if (acc.puedeDiligencia) {
        hito3Action = { event: 'diligencia', label: 'Registrar Diligencia SUNAT', icon: 'pi pi-search-plus', severity: 'warn' }
      }
    }
  }

  const despachoRealizado =
    Boolean(exp.transporteSolicitado) ||
    isRetiroDirectoRegistrado(exp) ||
    Boolean(exp.entregadoAlmacenVerde)

  let hito4Action = null
  const hito4Secondary = []
  if (acc.puedeEntregadoAlmacenVerde) {
    hito4Action = { event: 'entregado-almacen-verde', label: 'Entregado Almacén (Verde)', icon: 'pi pi-check-square', severity: 'success' }
  } else if (acc.puedeSolicitarTransporte) {
    hito4Action = { event: 'solicitar-transporte', label: 'Registrar Transporte Externo', icon: 'pi pi-truck', severity: 'success' }
  } else if (acc.puedeRetiroDirecto) {
    hito4Action = { event: 'retiro-directo', label: 'Acta Retiro Directo', icon: 'pi pi-check', severity: 'secondary' }
  }
  if (acc.puedeSolicitarTransporte && hito4Action?.event !== 'solicitar-transporte') {
    hito4Secondary.push({ event: 'solicitar-transporte', label: 'Registrar Transporte Externo', icon: 'pi pi-truck', severity: 'success' })
  }
  if (acc.puedeRetiroDirecto && hito4Action?.event !== 'retiro-directo') {
    hito4Secondary.push({ event: 'retiro-directo', label: 'Acta Retiro Directo', icon: 'pi pi-check', severity: 'secondary' })
  }
  if (acc.puedeIngresoAlmacen) {
    hito4Secondary.push({ event: 'ingreso-almacen', label: 'Balanza VGM', icon: 'pi pi-box', severity: 'secondary' })
  }

  let hito5Action = null
  const hito5Secondary = []
  if (acc.puedeInformeConformidad) {
    hito5Action = { event: 'informe-conformidad', label: 'Emitir Informe de Conformidad', icon: 'pi pi-file-check', severity: 'info' }
    if (acc.puedeFinalizarExpediente) {
      hito5Secondary.push({ event: 'finalizar-expediente', label: 'Finalizar Expediente', icon: 'pi pi-lock', severity: 'success' })
    }
  } else if (acc.puedeFinalizarExpediente) {
    hito5Action = { event: 'finalizar-expediente', label: 'Finalizar Expediente', icon: 'pi pi-lock', severity: 'success' }
  }
  if (acc.tieneInformeConformidad) {
    hito5Secondary.push({ event: 'descargar-informe', label: 'PDF Conformidad', icon: 'pi pi-file-pdf', severity: 'secondary' })
  }

  return [
    {
      id: 'imp-salida-origen',
      num: 1,
      kicker: 'HITO 01 · ORIGEN',
      title: 'Salida de Origen & Embarque',
      description: hasZarpe
        ? `Embarque confirmado (${exp.naveViaje || 'En tránsito internacional'}).`
        : 'Registre el envío de origen exitoso (nave/vuelo, contenedor, precinto y ETD/ETA) para habilitar el seguimiento en tránsito.',
      b2bEffect: 'Cambia el Portal B2B a "En Tránsito Internacional" y muestra la ruta con ETD/ETA.',
      status: hasZarpe ? 'completed' : (!isCerrado ? 'current' : 'pending'),
      date: hasZarpe ? exp.fechaZarpeEtd : null,
      action: acc.puedeSalidaOrigen
        ? { event: 'salida-origen', label: 'Registrar Envío de Origen Exitoso', icon: 'pi pi-send', severity: 'primary' }
        : null,
      secondaryActions: hito1Secondary
    },
    {
      id: 'imp-arribo',
      num: 2,
      kicker: 'HITO 02 · ARRIBO',
      title: 'Arribo a Puerto / Aeropuerto',
      description: hasArribo
        ? 'Llegada a terminal peruano confirmada.'
        : (hasZarpe
          ? 'Confirme el arribo real al puerto/aeropuerto y terminal de llegada.'
          : 'Se habilita tras registrar la salida de origen.'),
      b2bEffect: 'Pasa el Portal B2B a "Arribado en Puerto" y habilita la asignación de Canal SUNAT.',
      status: hasArribo ? 'completed' : (hasZarpe && !isCerrado ? 'current' : 'pending'),
      date: exp.fechaArriboAduana || null,
      action: acc.puedeConfirmarArribo
        ? { event: 'arribo-destino', label: 'Confirmar Arribo a Destino', icon: 'pi pi-compass', severity: 'success' }
        : null,
      secondaryActions: hito2Secondary
    },
    {
      id: 'imp-canal',
      num: 3,
      kicker: 'HITO 03 · SUNAT',
      title: 'Canal SUNAT & Aforo',
      description: hasLevante
        ? `Levante autorizado (Canal ${exp.canalSunat || 'SUNAT'}).`
        : (hasCanal
          ? `Canal ${exp.canalSunat} asignado; complete las diligencias requeridas para obtener el levante.`
          : (hasArribo
            ? 'Esperando que Gestión Aduanera registre la DAM y el Canal SUNAT.'
            : 'Pendiente de arribo a puerto.')),
      b2bEffect: 'Actualiza el semáforo de Canal SUNAT, citas de aforo y levante en el Portal B2B.',
      status: hasLevante || (hasCanal && exp.canalSunat === 'VERDE')
        ? 'completed'
        : (hasCanal && !isCerrado ? 'current' : (hasArribo && !isCerrado ? 'waiting' : 'pending')),
      date: exp.fechaLevanteAutorizacion || exp.fechaAsignacionCanal || null,
      action: hito3Action,
      secondaryActions: hito3Secondary
    },
    {
      id: 'imp-despacho',
      num: 4,
      kicker: 'HITO 04 · ENTREGA',
      title: 'Retiro de Terminal & Transporte',
      description: despachoRealizado || isCerrado
        ? 'Despacho local o retiro en almacén gestionado.'
        : (hasLevante
          ? (exp.servicioTransporte ? 'Registre los datos de solicitud para traslado con transporte externo.' : 'Registre el acta de retiro directo del cliente.')
          : 'Disponible tras obtener el levante aduanero.'),
      b2bEffect: 'Muestra los datos de la solicitud de traslado externo en Portal B2B.',
      status: despachoRealizado || isCerrado ? 'completed' : (hasLevante ? 'current' : 'pending'),
      date: exp.fechaEntregaAlmacenVerde || null,
      action: hito4Action,
      secondaryActions: hito4Secondary
    },
    {
      id: 'imp-cierre',
      num: 5,
      kicker: 'HITO 05 · CIERRE',
      title: 'Conformidad & Cierre Formal',
      description: isCerrado
        ? 'Expediente cerrado conforme y archivado.'
        : (hasLevante
          ? 'Emita el informe de conformidad de entrega y cierre formalmente el expediente.'
          : 'Se habilita al concluir el levante y despacho.'),
      b2bEffect: 'Habilita la descarga del Informe de Conformidad PDF y cierra el tracking en Portal B2B.',
      status: isCerrado ? 'completed' : (hasLevante ? 'current' : 'pending'),
      date: exp.fechaCierre || exp.fechaInformeConformidad || null,
      action: hito5Action,
      secondaryActions: hito5Secondary
    }
  ]
}

/**
 * Devuelve la siguiente acción operativa prioritaria para mostrar en bandeja y cabecera de ficha.
 * @param {Object} exp
 */
export function getSiguienteAccionAgente(exp = {}) {
  if (isExpedienteCerrado(exp)) {
    return {
      event: null,
      isCerrado: true,
      label: exp.estadoExpediente === 'ANULADO' ? 'Anulado' : 'Cerrado Conforme',
      icon: 'pi pi-lock',
      severity: 'success',
      hitoTitle: 'Ciclo Completado',
      b2bEffect: 'El cliente visualiza el expediente cerrado con historial completo en Portal B2B.'
    }
  }

  const hitos = buildAgenteChecklistHitos(exp)
  const nextWithAction = hitos.find(h => Boolean(h.action))
  if (nextWithAction) {
    return {
      ...nextWithAction.action,
      hitoTitle: nextWithAction.title,
      b2bEffect: nextWithAction.b2bEffect,
      isWaiting: false
    }
  }

  const waitingHito = hitos.find(h => h.status === 'waiting')
  if (waitingHito) {
    return {
      event: null,
      isWaiting: true,
      label: 'Esperando Canal SUNAT',
      icon: 'pi pi-clock',
      severity: 'warn',
      hitoTitle: waitingHito.title,
      waitingText: waitingHito.description,
      b2bEffect: waitingHito.b2bEffect
    }
  }

  return {
    event: null,
    isWaiting: false,
    label: 'Sin acción pendiente',
    icon: 'pi pi-info-circle',
    severity: 'secondary',
    hitoTitle: 'Seguimiento',
    b2bEffect: ''
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
