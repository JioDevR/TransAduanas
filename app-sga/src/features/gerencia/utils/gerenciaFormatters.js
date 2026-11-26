/**
 * Utilidades puras de formateo, severidades PrimeVue 4 y clasificación
 * de entidades maestras para el módulo de Gerencia General.
 */

export const SEDE_TIPO_LABELS = {
  SEDE_CENTRAL: 'Sede Central',
  AGENCIA: 'Agencia Operativa',
  TERMINAL: 'Terminal / Almacén'
}

export const SEDE_TIPO_ICONS = {
  SEDE_CENTRAL: 'pi pi-building',
  AGENCIA: 'pi pi-briefcase',
  TERMINAL: 'pi pi-box'
}

export const PUERTO_TIPO_LABELS = {
  MARITIMO: 'Marítimo',
  AEREO: 'Aéreo',
  TERRESTRE: 'Terrestre',
  FLUVIAL: 'Fluvial'
}

export const PUERTO_TIPO_ICONS = {
  MARITIMO: 'pi pi-compass',
  AEREO: 'pi pi-send',
  TERRESTRE: 'pi pi-truck',
  FLUVIAL: 'pi pi-directions'
}

export const DEFAULT_MERCANCIA_CATEGORIES = [
  'AGROEXPORTACION',
  'AGROPECUARIO',
  'ALIMENTOS',
  'AUTOPARTES',
  'BEBIDAS',
  'CALZADO',
  'CONFECCIONES',
  'COSMETICA',
  'ELECTROHOGAR',
  'ENTRETENIMIENTO',
  'EQUIPAMIENTO',
  'FARMACIA',
  'FERRETERIA',
  'HOGAR',
  'JUGUETERIA',
  'LICORES',
  'MADERA',
  'MAQUINARIA',
  'NUTRICION',
  'PESCA',
  'QUIMICOS',
  'QUIMICOS / IMO',
  'SEGURIDAD',
  'TABACO',
  'TECNOLOGIA',
  'TRANSPORTE',
  'VEHICULOS'
]

export function formatSedeTipo(tipo) {
  return SEDE_TIPO_LABELS[tipo] || tipo || 'Sucursal'
}

export function getSedeTipoIcon(tipo) {
  return SEDE_TIPO_ICONS[tipo] || 'pi pi-home'
}

export function getSedeEstadoSeverity(estado) {
  switch (estado) {
    case 'OPERATIVO':
      return 'success'
    case 'MANTENIMIENTO':
      return 'warn'
    case 'INACTIVO':
    default:
      return 'danger'
  }
}

export function formatPuertoTipo(tipo) {
  return PUERTO_TIPO_LABELS[tipo] || tipo || 'Marítimo'
}

export function getPuertoTipoIcon(tipo) {
  return PUERTO_TIPO_ICONS[tipo] || 'pi pi-compass'
}

export function getPuertoTipoSeverity(tipo) {
  switch (tipo) {
    case 'AEREO':
      return 'info'
    case 'TERRESTRE':
      return 'warn'
    case 'FLUVIAL':
      return 'secondary'
    case 'MARITIMO':
    default:
      return 'contrast'
  }
}

export function getCatalogoEstadoSeverity(estado) {
  return estado === 'ACTIVO' ? 'success' : 'secondary'
}

export function classifyTipoCargaRegimen(codigo) {
  const code = String(codigo || '').trim().toUpperCase()
  if (code.startsWith('IMP')) return 'IMP'
  if (code.startsWith('EXP')) return 'EXP'
  if (code.startsWith('GEN')) return 'GEN'
  return 'ESP'
}

export function getTipoCargaRegimenLabel(codigo) {
  const key = classifyTipoCargaRegimen(codigo)
  const map = {
    IMP: 'Importación',
    EXP: 'Exportación',
    GEN: 'Uso General',
    ESP: 'Especial / Regulada'
  }
  return map[key]
}

export function getTipoCargaRegimenSeverity(codigo) {
  const key = classifyTipoCargaRegimen(codigo)
  switch (key) {
    case 'IMP':
      return 'info'
    case 'EXP':
      return 'success'
    case 'ESP':
      return 'warn'
    case 'GEN':
    default:
      return 'secondary'
  }
}

export function mergeNormalizedCategories(...lists) {
  const unique = new Set()
  for (const list of lists) {
    if (!Array.isArray(list)) continue
    for (const item of list) {
      const normalized = String(item || '').trim().toUpperCase()
      if (normalized) unique.add(normalized)
    }
  }
  return Array.from(unique).sort((a, b) => a.localeCompare(b, 'es'))
}

export function getPermisoVuceSeverity(requierePermiso) {
  return requierePermiso ? 'warn' : 'success'
}

export function formatArancelPct(pct) {
  return `${Number(pct || 0).toFixed(2)}%`
}
