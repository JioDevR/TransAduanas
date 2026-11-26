import { isCotizacionRevisable, isCotizacionObservable } from './aduanasFormatters'

/**
 * Normaliza los datos de entrada (cotización o expediente) para la vista de Auditoría Técnica.
 *
 * @param {Object|null} cotizacion
 * @param {Object|null} expediente
 * @returns {Object}
 */
export function normalizeAuditoriaData(cotizacion = null, expediente = null) {
  const raw = expediente || cotizacion || {}
  const isExpedienteView = Boolean(expediente || cotizacion?.codigoExpediente)
  const estado = raw.estado || raw.estadoExpediente || ''
  const modalidadRaw = raw.modalidad || raw.puertoEnvioTipo || raw.puertoDestinoTipo || ''
  const textoInferencia = `${raw.puertoEnvioNombre || ''} ${raw.puertoDestinoNombre || ''} ${raw.tipoCargaNombre || ''}`.toUpperCase()
  const isAirModal = modalidadRaw
    ? String(modalidadRaw).toUpperCase() === 'AEREO'
    : textoInferencia.includes('AEROPUERTO') || textoInferencia.includes('AÉREO') || textoInferencia.includes('AEREO')

  const entidadTipo = isExpedienteView ? 'EXPEDIENTE' : 'COTIZACION'
  const entidadId = isExpedienteView
    ? raw.idExpediente || raw.idCotizacion || null
    : raw.idCotizacion || null

  const codigoPrincipal = isExpedienteView
    ? raw.codigoExpediente || (raw.idExpediente ? `EXP-${raw.idExpediente}` : 'EXPEDIENTE')
    : raw.codigoCotizacion || (raw.idCotizacion ? `COT-${raw.idCotizacion}` : 'COTIZACIÓN')

  const isAuditable = !isExpedienteView && isCotizacionRevisable(estado)
  const isObservable = !isExpedienteView && isCotizacionObservable(estado)
  const canAperturar = !isExpedienteView && estado === 'APROBADA' && !raw.tieneExpediente

  return {
    ...raw,
    isExpedienteView,
    isAirModal,
    estadoNormalizado: estado,
    entidadTipo,
    entidadId,
    codigoPrincipal,
    isAuditable,
    isObservable,
    canAperturar
  }
}

/**
 * Calcula la liquidación aduanera estimada (CIF, Ad-Valórem, IGV, Percepción y Total Tributos).
 *
 * @param {Object} data
 * @returns {{
 *   valorFob: number,
 *   fleteInternacional: number,
 *   seguroEstimado: number,
 *   valorCif: number,
 *   tasaAdValorem: number,
 *   montoAdValorem: number,
 *   montoIgv: number,
 *   tasaPercepcion: number,
 *   montoPercepcion: number,
 *   totalTributos: number
 * }}
 */
export function calcularLiquidacionAduanera(data = {}) {
  const valorFob = Number(data?.valorFob ?? data?.montoFobEstimado ?? 0)
  const fleteInternacional = Number(data?.fleteInternacional ?? data?.montoFleteEstimado ?? 0)
  const seguroEstimado = Number(data?.seguroEstimado ?? data?.montoSeguroEstimado ?? 0)

  const valorCif = data?.valorCif
    ? Number(data.valorCif)
    : valorFob + fleteInternacional + seguroEstimado

  const tasaAdValorem = Number(data?.tasaAdValorem ?? 0)
  const montoAdValorem = data?.montoAdValorem
    ? Number(data.montoAdValorem)
    : (valorCif * tasaAdValorem) / 100

  const montoIgv = data?.montoIgv
    ? Number(data.montoIgv)
    : ((valorCif + montoAdValorem) * 18) / 100

  const tasaPercepcion = Number(data?.tasaPercepcion ?? 0)
  const montoPercepcion = data?.montoPercepcion
    ? Number(data.montoPercepcion)
    : tasaPercepcion > 0
      ? ((valorCif + montoAdValorem + montoIgv) * tasaPercepcion) / 100
      : 0

  const totalTributos = montoAdValorem + montoIgv + montoPercepcion

  return {
    valorFob,
    fleteInternacional,
    seguroEstimado,
    valorCif,
    tasaAdValorem,
    montoAdValorem,
    montoIgv,
    tasaPercepcion,
    montoPercepcion,
    totalTributos
  }
}

/**
 * Evalúa dinámicamente los criterios del checklist normativo de auditoría aduanera.
 *
 * @param {Object} data
 * @param {number|null} docsCount
 * @returns {Array<{ id: string, label: string, valid: boolean, detail: string }>}
 */
export function evaluarChecklistAuditoria(data = {}, docsCount = 0) {
  const { valorCif } = calcularLiquidacionAduanera(data)
  const docNum = String(data?.clienteNumeroDocumento || '').trim()
  const nandina = String(data?.mercanciaCodigo || '').trim()
  const tipoCarga = String(data?.tipoCargaNombre || '').trim()
  const hasDocs = Number(docsCount || 0) > 0

  return [
    {
      id: 'ruc',
      label: 'Documento / RUC del cliente registrado',
      valid: docNum.length >= 8,
      detail: docNum.length >= 8 ? docNum : 'Pendiente'
    },
    {
      id: 'regimen',
      label: 'Régimen aduanero SUNAT definido',
      valid: Boolean(data?.regimenAduanero),
      detail: data?.regimenAduanero ? 'Asignado' : 'Sin régimen'
    },
    {
      id: 'cif',
      label: 'Base imponible CIF sustentada',
      valid: valorCif > 0,
      detail: valorCif > 0 ? 'Conforme' : 'CIF en $0.00'
    },
    {
      id: 'nandina',
      label: 'Partida arancelaria NANDINA identificada',
      valid: Boolean(nandina),
      detail: nandina || 'Por clasificar'
    },
    {
      id: 'carga',
      label: 'Tipo de carga y naturaleza verificada',
      valid: Boolean(tipoCarga),
      detail: tipoCarga || 'Sin especificar'
    },
    {
      id: 'docs',
      label: 'Documentos sustentatorios adjuntos',
      valid: hasDocs,
      detail: hasDocs ? `${docsCount} archivo(s)` : 'Sin adjuntos'
    }
  ]
}
