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

export const getRegimenLabel = (val) => {
  const match = REGIMEN_OPTIONS.find((r) => r.value === val)
  return match ? `${match.label} (${match.codigo})` : val
}

export const getTipoCargaDescripcionFallback = () => ''

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
