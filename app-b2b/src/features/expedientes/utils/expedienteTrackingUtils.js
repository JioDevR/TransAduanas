import { formatDate, formatDateTime } from '@/shared/utils/formatters'

export function isExportExpediente(exp) {
  const reg = (exp?.regimenAduanero || exp?.nombreTipoCarga || '').toUpperCase()
  return reg.includes('EXPORT')
}

export function isZarpeConfirmado(exp) {
  if (!exp) return false
  const obs = exp.observaciones || ''
  if (obs.includes('SALIDA ORIGEN CONFIRMADA') || obs.includes('EMBARQUE CONFIRMADO')) return true
  if (exp.fechaArriboAduana || (exp.canalSunat && exp.canalSunat !== 'PENDIENTE') || exp.fechaLevanteAutorizacion) {
    return true
  }
  return exp.estadoExpediente !== 'ASIGNADO'
}

export function getExpedientePhase(exp) {
  if (!exp) {
    return { label: 'En Proceso', icon: 'pi pi-clock', severity: 'secondary' }
  }
  if (exp.estadoExpediente === 'CERRADO') {
    return { label: 'Cerrado Conforme', icon: 'pi pi-check-circle', severity: 'success' }
  }
  if (exp.estadoExpediente === 'ANULADO') {
    return { label: 'Anulado', icon: 'pi pi-times-circle', severity: 'danger' }
  }
  if (exp.estadoExpediente === 'LEVANTE_AUTORIZADO' || exp.fechaLevanteAutorizacion) {
    return { label: 'Levante Autorizado', icon: 'pi pi-check-circle', severity: 'success' }
  }
  if (exp.estadoExpediente === 'INSPECCION' || exp.canalSunat === 'NARANJA' || exp.canalSunat === 'ROJO') {
    return {
      label: exp.canalSunat === 'ROJO' ? 'Aforo Físico SUNAT' : 'Revisión Documentaria',
      icon: 'pi pi-search',
      severity: 'warn'
    }
  }
  if (exp.fechaArriboAduana) {
    return {
      label: isExportExpediente(exp) ? 'En Almacén Central / Terminal' : 'Arribado en Puerto',
      icon: 'pi pi-compass',
      severity: 'info'
    }
  }
  if (isZarpeConfirmado(exp)) {
    return { label: 'En Tránsito Internacional', icon: 'pi pi-send', severity: 'info' }
  }
  if (exp.pendienteAsignacionAgente) {
    return { label: 'Por Asignar Agente', icon: 'pi pi-user-plus', severity: 'warn' }
  }
  return { label: 'Apertura / Pendiente Salida', icon: 'pi pi-clock', severity: 'secondary' }
}

export function getCanalMeta(canal) {
  switch (String(canal || 'PENDIENTE').toUpperCase()) {
    case 'VERDE':
      return {
        label: 'Canal VERDE',
        severity: 'success',
        description: 'Levante automático e inmediato sin revisión documental ni física.'
      }
    case 'NARANJA':
      return {
        label: 'Canal NARANJA',
        severity: 'warn',
        description: 'Revisión documentaria técnica obligatoria ante especialista SUNAT.'
      }
    case 'ROJO':
      return {
        label: 'Canal ROJO',
        severity: 'danger',
        description: 'Reconocimiento físico obligatorio de bultos en depósito temporal.'
      }
    default:
      return {
        label: 'Canal PENDIENTE',
        severity: 'secondary',
        description: 'Pendiente de asignación por el sistema SUNAT.'
      }
  }
}

export function getAlmacenAlertClass(days) {
  if (days == null) return 'is-info'
  if (Number(days) <= 3) return 'is-danger'
  if (Number(days) <= 7) return 'is-warn'
  return 'is-info'
}

export function calculateLeadTime(exp) {
  if (!exp?.fechaApertura || !exp?.fechaCierre) return null
  const inicio = new Date(exp.fechaApertura)
  const fin = new Date(exp.fechaCierre)
  if (isNaN(inicio.getTime()) || isNaN(fin.getTime())) return null
  const dias = Math.max(1, Math.round((fin - inicio) / (1000 * 60 * 60 * 24)))
  return `${dias} días de ciclo`
}

export function calculateElapsedDays(exp, now = new Date()) {
  if (!exp) return '—'
  if (exp.estadoExpediente === 'CERRADO') {
    return calculateLeadTime(exp) || 'Cerrado'
  }
  if (!exp.fechaApertura) return '—'
  const inicio = new Date(exp.fechaApertura)
  if (isNaN(inicio.getTime())) return '—'
  const diffDays = Math.floor((now.getTime() - inicio.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays <= 0) return 'Hoy'
  if (diffDays === 1) return '1 día en curso'
  return `${diffDays} días en curso`
}

export function formatEstadoExpedienteLabel(estado) {
  const map = {
    ASIGNADO: 'Asignado',
    NUMERADO: 'DAM Numerada',
    EN_CANAL: 'En Canal SUNAT',
    INSPECCION: 'En Inspección',
    LEVANTE_AUTORIZADO: 'Levante Autorizado',
    CUSTODIA_ALMACEN: 'Custodia Almacén',
    EN_DESPACHO: 'En Despacho',
    CERRADO: 'Cerrado Conforme',
    FINALIZADO: 'Finalizado',
    ANULADO: 'Anulado',
    EN_TRAMITE: 'En Trámite'
  }
  if (!estado) return 'En Proceso'
  return map[String(estado).toUpperCase()] || String(estado).replace(/_/g, ' ')
}

export function buildTimelineSteps(exp) {
  if (!exp) return []
  const isExport = isExportExpediente(exp)
  const zarpe = isZarpeConfirmado(exp)
  const arribado = Boolean(exp.fechaArriboAduana)
  const numerado = Boolean(exp.numeroDam)
  const levante = Boolean(exp.fechaLevanteAutorizacion || exp.estadoExpediente === 'LEVANTE_AUTORIZADO')
  const cerrado = exp.estadoExpediente === 'CERRADO'

  if (isExport) {
    const obs = exp.observaciones || ''
    const embarcado = obs.includes('EMBARQUE CONFIRMADO') || Boolean(exp.fechaZarpeEtd && levante)
    const recepcionExterior = obs.includes('RECEPCIÓN DESTINO EXTERIOR') || Boolean(exp.idDocumentoConformidadDestino)

    return [
      {
        num: 1,
        label: 'Expediente & Recojo',
        detail: exp.tieneTransporte ? (exp.estadoTransporte || 'Transporte activo') : 'Ingreso directo',
        completed: true,
        active: false
      },
      {
        num: 2,
        label: 'Almacén Central',
        detail: arribado ? formatDateTime(exp.fechaArriboAduana) : 'Por ingresar',
        completed: arribado,
        active: !arribado
      },
      {
        num: 3,
        label: 'Revisión Aduana',
        detail: numerado ? (exp.canalSunat || 'Con DAM') : (exp.enRevisionAduana ? 'En revisión' : 'Pendiente'),
        completed: numerado,
        active: arribado && !numerado
      },
      {
        num: 4,
        label: 'Levante & Tránsito',
        detail: levante ? (exp.enEsperaTransito ? 'En espera tránsito' : 'Autorizado') : 'En gestión',
        completed: levante,
        active: numerado && !levante
      },
      {
        num: 5,
        label: 'Vuelo / Embarque',
        detail: embarcado ? (exp.numeroVuelo || exp.naveViaje || 'Embarcado') : 'Por embarcar',
        completed: embarcado,
        active: levante && !embarcado
      },
      {
        num: 6,
        label: 'Destino & Finalizado',
        detail: cerrado ? (exp.fechaCierre ? formatDate(exp.fechaCierre) : 'Finalizado') : (recepcionExterior ? 'Conformidad OK' : 'En curso'),
        completed: cerrado || recepcionExterior,
        active: embarcado && !cerrado && !recepcionExterior
      }
    ]
  }

  return [
    {
      num: 1,
      label: 'Cotización',
      detail: exp.verificacionOrigenOk ? 'Verificado origen' : 'Aprobada',
      completed: true,
      active: false
    },
    {
      num: 2,
      label: 'Salida Origen',
      detail: zarpe ? (exp.fechaZarpeEtd ? formatDate(exp.fechaZarpeEtd) : 'Confirmado') : 'Por confirmar',
      completed: zarpe,
      active: !zarpe
    },
    {
      num: 3,
      label: 'Arribo Puerto',
      detail: arribado ? formatDateTime(exp.fechaArriboAduana) : 'En navegación',
      completed: arribado,
      active: zarpe && !arribado
    },
    {
      num: 4,
      label: 'Canal SUNAT',
      detail: exp.canalSunat || 'Asignando',
      completed: numerado,
      active: arribado && !numerado
    },
    {
      num: 5,
      label: 'Levante SUNAT',
      detail: levante ? 'Autorizado' : 'En aforo',
      completed: levante,
      active: numerado && !levante
    },
    {
      num: 6,
      label: 'Cierre Formal',
      detail: exp.fechaCierre ? formatDate(exp.fechaCierre) : (cerrado ? 'Cerrado' : 'En curso'),
      completed: cerrado,
      active: levante && !cerrado
    }
  ]
}

export function calculateTrackingProgress(steps) {
  if (!Array.isArray(steps) || steps.length === 0) {
    return {
      completedCount: 0,
      totalSteps: 0,
      percentage: 0,
      activeStep: null,
      isFinished: false
    }
  }
  const totalSteps = steps.length
  const completedCount = steps.filter(s => s.completed).length
  const activeStep = steps.find(s => !s.completed && s.active) || steps.find(s => !s.completed) || null
  const isFinished = completedCount === totalSteps
  const percentage = Math.round((completedCount / totalSteps) * 100)

  return {
    completedCount,
    totalSteps,
    percentage,
    activeStep,
    isFinished
  }
}

export function buildRouteSummary(exp) {
  if (!exp) return []
  const isExport = isExportExpediente(exp)
  const zarpe = isZarpeConfirmado(exp)
  const arribado = Boolean(exp.fechaArriboAduana)
  const levante = Boolean(exp.fechaLevanteAutorizacion || exp.estadoExpediente === 'LEVANTE_AUTORIZADO')
  const cerrado = exp.estadoExpediente === 'CERRADO'

  if (isExport) {
    const obs = exp.observaciones || ''
    const embarcado = obs.includes('EMBARQUE CONFIRMADO') || Boolean(exp.fechaZarpeEtd && levante)
    const recepcionExterior = obs.includes('RECEPCIÓN DESTINO EXTERIOR') || Boolean(exp.idDocumentoConformidadDestino)

    return [
      {
        key: 'origin',
        kicker: 'PLANTA / RECOJO',
        place: exp.direccionPartida || 'Planta exportadora',
        sub: exp.tieneTransporte ? (exp.estadoTransporte || 'PROGRAMADO') : 'Ingreso directo a terminal',
        icon: 'pi pi-map-marker',
        status: arribado ? 'reached' : 'active'
      },
      {
        key: 'customs',
        kicker: 'ALMACÉN & ADUANA',
        place: exp.depositoTemporal || 'Almacén Central / SUNAT',
        sub: arribado ? `Ingreso ${formatDate(exp.fechaArriboAduana)}` : 'Por ingresar a terminal',
        icon: 'pi pi-building',
        status: levante ? 'reached' : (arribado ? 'active' : 'pending')
      },
      {
        key: 'transit',
        kicker: 'EMBARQUE INTERNACIONAL',
        place: exp.numeroVuelo || exp.naveViaje || 'Por programar',
        sub: exp.fechaZarpeEtd ? `ETD: ${formatDate(exp.fechaZarpeEtd)}` : (exp.numeroContenedor || 'Vuelo / Nave'),
        icon: 'pi pi-send',
        status: embarcado ? 'reached' : (levante ? 'active' : 'pending')
      },
      {
        key: 'destination',
        kicker: 'DESTINO EXTERIOR',
        place: exp.contactoExteriorNombre || 'Consignatario Exterior',
        sub: cerrado
          ? `Finalizado ${formatDate(exp.fechaCierre)}`
          : (exp.fechaLlegadaEta ? `ETA: ${formatDate(exp.fechaLlegadaEta)}` : 'En tránsito a destino'),
        icon: 'pi pi-flag',
        status: cerrado || recepcionExterior ? 'reached' : (embarcado ? 'active' : 'pending')
      }
    ]
  }

  return [
    {
      key: 'origin',
      kicker: 'ORIGEN EXTERIOR',
      place: exp.contactoExteriorNombre || 'Proveedor Exterior',
      sub: exp.fechaZarpeEtd ? `ETD: ${formatDate(exp.fechaZarpeEtd)}` : (zarpe ? 'Salida confirmada' : 'ETD por confirmar'),
      icon: 'pi pi-flag',
      status: zarpe ? 'reached' : 'active'
    },
    {
      key: 'transit',
      kicker: 'TRÁNSITO INTERNACIONAL',
      place: exp.numeroVuelo || exp.naveViaje || 'En coordinación',
      sub: exp.numeroContenedor
        ? `Cont. ${exp.numeroContenedor}`
        : (exp.tipoTransporteInternacional || 'Flete internacional'),
      icon: exp.numeroVuelo ? 'pi pi-send' : 'pi pi-globe',
      status: arribado ? 'reached' : (zarpe ? 'active' : 'pending')
    },
    {
      key: 'customs',
      kicker: 'ADUANA & TERMINAL',
      place: exp.depositoTemporal || 'Puerto / Aduana Perú',
      sub: arribado
        ? `Arribo ${formatDate(exp.fechaArriboAduana)}`
        : (exp.fechaLlegadaEta ? `ETA: ${formatDate(exp.fechaLlegadaEta)}` : 'ETA por confirmar'),
      icon: 'pi pi-building',
      status: levante ? 'reached' : (arribado ? 'active' : 'pending')
    },
    {
      key: 'destination',
      kicker: 'ENTREGA EN PLANTA',
      place: exp.distritoLlegada || exp.direccionLlegada || 'Planta del cliente',
      sub: cerrado
        ? `Cierre ${formatDate(exp.fechaCierre)}`
        : (exp.fechaEntregaConforme
          ? `Entregado ${formatDate(exp.fechaEntregaConforme)}`
          : (exp.tieneTransporte ? (exp.estadoTransporte || 'PROGRAMADO') : 'Despacho final')),
      icon: 'pi pi-map-marker',
      status: cerrado || exp.fechaEntregaConforme ? 'reached' : (levante ? 'active' : 'pending')
    }
  ]
}

export function getEventToneMeta(codigoEstado) {
  const code = String(codigoEstado || '').toUpperCase()

  if (
    code.includes('FINALIZADO') ||
    code.includes('LEVANTE') ||
    code.includes('CONFORME') ||
    code.includes('VERDE') ||
    code.includes('ROJO_FINALIZADA') ||
    code.includes('VERIFICACION')
  ) {
    return { icon: 'pi pi-check', tone: 'success' }
  }
  if (code.includes('ROJO') || code.includes('CITA') || code.includes('ANULAD')) {
    return { icon: 'pi pi-exclamation-triangle', tone: 'danger' }
  }
  if (code.includes('CANAL') || code.includes('SUNAT') || code.includes('CUADRILLA')) {
    return { icon: 'pi pi-shield', tone: 'warn' }
  }
  if (
    code.includes('TRANSPORTE') ||
    code.includes('RECOGIDO') ||
    code.includes('SALIDA') ||
    code.includes('VUELO') ||
    code.includes('EMBARQUE') ||
    code.includes('ARRIBO') ||
    code.includes('ALMACEN')
  ) {
    return { icon: 'pi pi-truck', tone: 'info' }
  }
  if (code.includes('AGENTE')) {
    return { icon: 'pi pi-user', tone: 'info' }
  }
  return { icon: 'pi pi-clock', tone: 'neutral' }
}

export function sortTrackingEvents(eventos) {
  if (!Array.isArray(eventos) || eventos.length === 0) return []
  return [...eventos]
    .sort((a, b) => {
      const ta = a?.fechaEvento ? new Date(a.fechaEvento).getTime() : 0
      const tb = b?.fechaEvento ? new Date(b.fechaEvento).getTime() : 0
      if (tb !== ta) return tb - ta
      return (Number(b?.idEvento) || 0) - (Number(a?.idEvento) || 0)
    })
    .map(ev => ({
      ...ev,
      toneMeta: getEventToneMeta(ev?.codigoEstado)
    }))
}

export function getDocumentIcon(mime) {
  if (!mime) return 'pi pi-file'
  if (mime.includes('pdf')) return 'pi pi-file-pdf'
  if (mime.includes('image')) return 'pi pi-image'
  if (mime.includes('sheet') || mime.includes('excel')) return 'pi pi-file-excel'
  return 'pi pi-file'
}
