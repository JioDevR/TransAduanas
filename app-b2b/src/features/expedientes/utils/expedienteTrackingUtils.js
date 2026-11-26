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

export function getDocumentIcon(mime) {
  if (!mime) return 'pi pi-file'
  if (mime.includes('pdf')) return 'pi pi-file-pdf'
  if (mime.includes('image')) return 'pi pi-image'
  if (mime.includes('sheet') || mime.includes('excel')) return 'pi pi-file-excel'
  return 'pi pi-file'
}
