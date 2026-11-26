import { FilterService } from '@primevue/core/api'

export const REGIMEN_OPTIONS = [
  {
    value: 'IMPORTACION_DEFINITIVA',
    codigo: 'REG-10',
    label: 'Importación para el Consumo',
    desc: 'Ingreso legal de mercancías para consumo definitivo previo pago de tributos.'
  },
  {
    value: 'EXPORTACION_DEFINITIVA',
    codigo: 'REG-40',
    label: 'Exportación Definitiva',
    desc: 'Salida de mercancías del territorio aduanero para uso o consumo en el exterior.'
  }
]

const DESCRIPCIONES_TIPO_CARGA_FALLBACK = {
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

export const getRegimenLabel = (val) => {
  const match = REGIMEN_OPTIONS.find((r) => r.value === val)
  return match ? `${match.label} (${match.codigo})` : val
}

export const getTipoCargaDescripcionFallback = (codigo) => {
  return (codigo && DESCRIPCIONES_TIPO_CARGA_FALLBACK[codigo]) || ''
}

export const normalizeSearchText = (text) => {
  if (!text) return ''
  return text
    .toString()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

let smartFilterRegistered = false
export const ensureSmartFilterRegistered = () => {
  if (smartFilterRegistered) return
  FilterService.register('smart', (value, filter) => {
    if (filter === undefined || filter === null || filter === '') return true
    if (value === undefined || value === null) return false
    const normVal = normalizeSearchText(value)
    const tokens = normalizeSearchText(filter).split(/\s+/).filter(Boolean)
    if (tokens.length === 0) return true
    return tokens.every((tok) => normVal.includes(tok))
  })
  smartFilterRegistered = true
}

export const unwrapCatalogoList = (res) => {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.data)) return res.data
  return []
}

export const mapPuertosCatalogo = (res) => {
  return unwrapCatalogoList(res).map((p) => ({
    ...p,
    searchCorpus: `${p.codigoPuerto || ''} ${p.nombre || ''} ${p.pais || ''} ${p.tipo || ''}`
  }))
}

export const mapTiposCargaCatalogo = (res) => {
  return unwrapCatalogoList(res).map((t) => {
    const desc = t.descripcion || getTipoCargaDescripcionFallback(t.codigo)
    return {
      ...t,
      descripcion: desc,
      searchCorpus: `${t.codigo || ''} ${t.nombre || ''} ${desc}`
    }
  })
}

export const mapMercanciasCatalogo = (res) => {
  return unwrapCatalogoList(res).map((m) => ({
    ...m,
    searchCorpus: `${m.codigo || ''} ${m.nombre || ''} ${m.categoria || ''} ${m.entidadReguladora || ''}`
  }))
}
