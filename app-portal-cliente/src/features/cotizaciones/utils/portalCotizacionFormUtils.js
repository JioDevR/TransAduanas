/**
 * Constantes y utilidades para el formulario de registro de cotizaciones en el Portal B2B.
 */

export const REGIMEN_OPTIONS = [
  {
    value: 'IMPORTACION_DEFINITIVA',
    codigo: 'REG-10',
    label: 'Importación para el Consumo',
    desc: 'Ingreso legal de mercancías para consumo definitivo en el país previo pago de tributos arancelarios.'
  },
  {
    value: 'EXPORTACION_DEFINITIVA',
    codigo: 'REG-40',
    label: 'Exportación Definitiva',
    desc: 'Salida de mercancías nacionales o nacionalizadas del territorio aduanero para uso o consumo en el exterior.'
  }
]

export const DESCRIPCIONES_TIPO_CARGA_FALLBACK = {
  CARGA_FCL: 'Contenedor exclusivo de 20 o 40 pies para un único consignatario',
  CARGA_LCL: 'Carga agrupada en contenedor compartido para múltiples importadores',
  CARGA_REEFER: 'Mercancías con cadena de frío continua y atmósfera controlada',
  CARGA_RO_RO: 'Vehículos y maquinaria autopropulsada embarcada por rampas',
  CARGA_PROYECTO: 'Bienes sobredimensionados o con peso extrapesado fuera de estándar',
  CARGA_GRANEL_SOLIDO: 'Sólidos transportados a granel en bodegas sin empaque unitario',
  CARGA_GRANEL_LIQUIDO: 'Líquidos transportados en buques tanque o tanques cisterna ISO',
  CARGA_PELIGROSA_IMO: 'Sustancias peligrosas reguladas según código marítimo internacional IMO',
  CARGA_VALORADA: 'Carga de alto valor comercial sujeta a protocolo de seguridad y custodia',
  CARGA_AEREA_SUELTA: 'Bultos y cajas sueltas estibadas en bodegas inferiores del avión (belly cargo)',
  CARGA_AEREA_ULD_PALLET: 'Pallet aeronáutico de aluminio estandarizado IATA con red de sujeción',
  CARGA_AEREA_ULD_CONT: 'Contenedor liviano adaptado a la curvatura del fuselaje de la aeronave',
  CARGA_AEREA_COURIER: 'Envíos de entrega rápida y comercio electrónico internacional',
  CARGA_AEREA_PERECIBLE: 'Fármacos y perecibles en contenedor activo refrigerado con monitoreo continuo',
  CARGA_AEREA_DGR_IATA: 'Mercancías peligrosas reguladas bajo la estricta normativa técnica IATA DGR',
  CARGA_AEREA_VALORADA: 'Metales preciosos, joyas y divisas en bóveda blindada aeroportuaria con escolta',
  CARGA_AEREA_AVI: 'Transporte aéreo de mascotas y animales vivos en cabina o bodega presurizada'
}

export const CATEGORIAS_DOCUMENTO = [
  { value: 'GENERAL', label: 'Documento General / Sustento' },
  { value: 'FACTURA', label: 'Factura Comercial' },
  { value: 'PACKING_LIST', label: 'Lista de Empaque (Packing List)' },
  { value: 'TRANSPORTE_BL', label: 'Documento de Transporte (BL / AWB)' },
  { value: 'CERTIFICADO', label: 'Certificado de Origen / Calidad' },
  { value: 'PERMISO_ESPECIAL', label: 'Permiso Especial / Registro Regulatorio' },
  { value: 'IDENTIFICACION', label: 'Documento de Identificación / RUC' }
]

CATEGORIAS_DOCUMENTO.GENERAL = 'GENERAL'
CATEGORIAS_DOCUMENTO.FACTURA = 'FACTURA'
CATEGORIAS_DOCUMENTO.PACKING_LIST = 'PACKING_LIST'
CATEGORIAS_DOCUMENTO.TRANSPORTE_BL = 'TRANSPORTE_BL'
CATEGORIAS_DOCUMENTO.CERTIFICADO = 'CERTIFICADO'
CATEGORIAS_DOCUMENTO.PERMISO_ESPECIAL = 'PERMISO_ESPECIAL'
CATEGORIAS_DOCUMENTO.IDENTIFICACION = 'IDENTIFICACION'

/**
 * Normaliza una cadena de texto para comparaciones y búsquedas insensibles a mayúsculas y diacríticos.
 * @param {string} text
 * @returns {string}
 */
export function normalizeSearchText(text) {
  if (text === null || text === undefined) return ''
  return String(text)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

/**
 * Evalúa si un valor coincide con un filtro multicriterio tokenizado por espacios.
 * @param {unknown} value
 * @param {string} filter
 * @returns {boolean}
 */
export function matchesSmartFilter(value, filter) {
  if (!filter || !String(filter).trim()) return true
  if (value === null || value === undefined) return false
  const target = normalizeSearchText(String(value))
  const tokens = normalizeSearchText(String(filter)).split(/\s+/).filter(Boolean)
  return tokens.every((token) => target.includes(token))
}

/**
 * Crea la estructura inicial con valores predeterminados para el formulario de solicitud B2B.
 * @returns {Object}
 */
export function createDefaultPortalSolicitudForm() {
  return {
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    validezDias: 15,
    idPuertoEnvio: null,
    idPuertoDestino: null,
    idTipoCarga: null,
    idMercancia: null,
    servicioTransporte: false,
    origenDepartamento: 'CALLAO',
    origenProvincia: 'Callao',
    destinoDepartamento: 'LIMA',
    destinoProvincia: 'Lima (Metropolitana)',
    valorFob: 0,
    montoFleteEstimado: 0,
    observaciones: ''
  }
}

/**
 * Valida los datos del formulario de cotización B2B según el paso correspondiente.
 * @param {Object} form
 * @returns {{ valid: boolean, step?: number, error?: string }}
 */
export function validatePortalCotizacionForm(form) {
  if (!form) {
    return { valid: false, step: 1, error: 'Formulario no inicializado' }
  }

  // Paso 1: Régimen y Logística
  if (!form.regimenAduanero || !REGIMEN_OPTIONS.some((r) => r.value === form.regimenAduanero)) {
    return { valid: false, step: 1, error: 'Debe seleccionar un régimen aduanero oficial' }
  }

  const validez = Number(form.validezDias)
  if (form.validezDias === null || form.validezDias === undefined || isNaN(validez) || validez < 1 || validez > 90) {
    return { valid: false, step: 1, error: 'La validez de la cotización debe estar entre 1 y 90 días' }
  }

  if (form.servicioTransporte) {
    if (
      !form.origenDepartamento ||
      !form.origenProvincia ||
      !form.destinoDepartamento ||
      !form.destinoProvincia
    ) {
      return {
        valid: false,
        step: 1,
        error: 'Debe seleccionar departamentos y provincias de origen y destino para el servicio de transporte'
      }
    }
  }

  // Paso 3: Valores Declarados
  const fob = Number(form.valorFob)
  if (form.valorFob === null || form.valorFob === undefined || isNaN(fob) || fob <= 0) {
    return { valid: false, step: 3, error: 'El valor FOB estimado debe ser mayor a 0' }
  }

  if (form.montoFleteEstimado !== null && form.montoFleteEstimado !== undefined && form.montoFleteEstimado !== '') {
    const flete = Number(form.montoFleteEstimado)
    if (isNaN(flete) || flete < 0) {
      return { valid: false, step: 3, error: 'El flete internacional estimado no puede ser negativo' }
    }
  }

  return { valid: true }
}

/**
 * Normaliza y construye el payload limpio para enviar al endpoint /portal/cotizaciones/solicitar.
 * @param {Object} form
 * @returns {Object}
 */
export function buildPortalSolicitudPayload(form) {
  const f = form || {}
  const servicioTransporte = Boolean(f.servicioTransporte)

  const rawFob = Number(f.valorFob)
  const valorFob = !isNaN(rawFob) && rawFob > 0 ? rawFob : 0

  const rawFlete = Number(f.montoFleteEstimado)
  const montoFleteEstimado = !isNaN(rawFlete) && rawFlete > 0 ? rawFlete : 0

  const rawValidez = Number(f.validezDias)
  const validezDias = !isNaN(rawValidez) && rawValidez >= 1 && rawValidez <= 90 ? rawValidez : 15

  return {
    regimenAduanero: f.regimenAduanero || 'IMPORTACION_DEFINITIVA',
    idPuertoEnvio: f.idPuertoEnvio ? Number(f.idPuertoEnvio) : null,
    idPuertoDestino: f.idPuertoDestino ? Number(f.idPuertoDestino) : null,
    idTipoCarga: f.idTipoCarga ? Number(f.idTipoCarga) : null,
    idMercancia: f.idMercancia ? Number(f.idMercancia) : null,
    servicioTransporte,
    origenDepartamento: servicioTransporte ? (f.origenDepartamento || null) : null,
    origenProvincia: servicioTransporte ? (f.origenProvincia || null) : null,
    destinoDepartamento: servicioTransporte ? (f.destinoDepartamento || null) : null,
    destinoProvincia: servicioTransporte ? (f.destinoProvincia || null) : null,
    valorFob,
    montoFleteEstimado,
    validezDias,
    observaciones: f.observaciones ? String(f.observaciones).trim() : ''
  }
}

/**
 * Determina si una cotización se encuentra pendiente de liquidación por parte de un asesor comercial.
 * @param {Object} cotizacion
 * @returns {boolean}
 */
export function esPendienteLiquidacion(cotizacion) {
  if (!cotizacion) return true
  const tieneAsesor = cotizacion.idUsuarioComercial !== null && cotizacion.idUsuarioComercial !== undefined
  const total = Number(cotizacion.totalCotizado || 0)
  return !tieneAsesor || total <= 0
}

export default {
  REGIMEN_OPTIONS,
  DESCRIPCIONES_TIPO_CARGA_FALLBACK,
  CATEGORIAS_DOCUMENTO,
  normalizeSearchText,
  matchesSmartFilter,
  createDefaultPortalSolicitudForm,
  validatePortalCotizacionForm,
  buildPortalSolicitudPayload,
  esPendienteLiquidacion
}
