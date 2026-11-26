import { formatDate, formatDateTime } from '@/shared/utils/dateUtils'

export const ESTADO_DESPACHO_OPTIONS = [
  { label: 'Todos los estados', value: 'ALL' },
  { label: 'Pendiente Asignación', value: 'PENDIENTE_ASIGNACION' },
  { label: 'Solicitud Aceptada', value: 'ACEPTADO' },
  { label: 'Programado', value: 'PROGRAMADO' },
  { label: 'En Ruta', value: 'EN_RUTA' },
  { label: 'Entregado', value: 'ENTREGADO' },
  { label: 'Cancelado', value: 'CANCELADO' }
]

export const TIPO_TRASLADO_OPTIONS = [
  { label: 'Entrega de Importación (Puerto → Cliente)', value: 'ENTREGA_IMPORTACION' },
  { label: 'Recojo de Exportación (Planta → Terminal)', value: 'RECOJO_EXPORTACION' }
]

export const TIPO_VEHICULO_OPTIONS = [
  { label: 'Tracto Remolcador', value: 'TRACTO' },
  { label: 'Plataforma / Semirremolque', value: 'PLATAFORMA' },
  { label: 'Furgón Cerrado / Reefer', value: 'FURGON' },
  { label: 'Camión Baranda', value: 'CAMION_BARANDA' }
]

export const ESTADO_FLOTA_OPTIONS = [
  { label: 'Disponible', value: 'DISPONIBLE' },
  { label: 'En Ruta', value: 'EN_RUTA' },
  { label: 'En Mantenimiento', value: 'MANTENIMIENTO' },
  { label: 'Fuera de Servicio', value: 'FUERA_DE_SERVICIO' }
]

export const CATEGORIA_LICENCIA_OPTIONS = [
  { label: 'A-IIIc (Tracto / Todo Pesado)', value: 'A-IIIc' },
  { label: 'A-IIIb (Camión Pesado)', value: 'A-IIIb' },
  { label: 'A-IIb (Camión Mediano)', value: 'A-IIb' },
  { label: 'A-IV (Especial MATPEL)', value: 'A-IV' }
]

export const ESTADO_CONDUCTOR_OPTIONS = [
  { label: 'Disponible', value: 'DISPONIBLE' },
  { label: 'En Ruta', value: 'EN_RUTA' },
  { label: 'En Descanso', value: 'DESCANSO' },
  { label: 'Suspendido', value: 'SUSPENDIDO' }
]

export function formatEstadoDespacho(estado) {
  const map = {
    PENDIENTE_ASIGNACION: 'Pendiente Asignación',
    ACEPTADO: 'Aceptado por Flota',
    PROGRAMADO: 'Programado',
    EN_RUTA: 'En Ruta Activa',
    ENTREGADO: 'Entregado Conforme',
    CANCELADO: 'Cancelado'
  }
  return map[String(estado || '').toUpperCase()] || (estado || '—')
}

export function getEstadoDespachoSeverity(estado) {
  switch (String(estado || '').toUpperCase()) {
    case 'PENDIENTE_ASIGNACION':
      return 'warn'
    case 'ACEPTADO':
      return 'info'
    case 'PROGRAMADO':
      return 'info'
    case 'EN_RUTA':
      return 'primary'
    case 'ENTREGADO':
      return 'success'
    case 'CANCELADO':
      return 'danger'
    default:
      return 'secondary'
  }
}

export function formatTipoTraslado(tipo) {
  const map = {
    ENTREGA_IMPORTACION: 'Entrega Importación',
    RECOJO_EXPORTACION: 'Recojo Exportación'
  }
  return map[String(tipo || '').toUpperCase()] || (tipo || 'Traslado')
}

export function getTipoTrasladoSeverity(tipo) {
  return String(tipo || '').toUpperCase() === 'RECOJO_EXPORTACION' ? 'warn' : 'info'
}

export function formatTipoVehiculo(tipo) {
  const map = {
    TRACTO: 'Tracto',
    PLATAFORMA: 'Plataforma',
    FURGON: 'Furgón',
    CAMION_BARANDA: 'Camión Baranda'
  }
  return map[String(tipo || '').toUpperCase()] || (tipo || '—')
}

export function formatEstadoFlota(estado) {
  const map = {
    DISPONIBLE: 'Disponible',
    EN_RUTA: 'En Ruta',
    MANTENIMIENTO: 'Mantenimiento',
    FUERA_DE_SERVICIO: 'Fuera de Servicio'
  }
  return map[String(estado || '').toUpperCase()] || (estado || '—')
}

export function getEstadoFlotaSeverity(estado) {
  switch (String(estado || '').toUpperCase()) {
    case 'DISPONIBLE':
      return 'success'
    case 'EN_RUTA':
      return 'info'
    case 'MANTENIMIENTO':
      return 'warn'
    case 'FUERA_DE_SERVICIO':
      return 'danger'
    default:
      return 'secondary'
  }
}

export function formatEstadoConductor(estado) {
  const map = {
    DISPONIBLE: 'Disponible',
    EN_RUTA: 'En Ruta',
    DESCANSO: 'Descanso',
    SUSPENDIDO: 'Suspendido'
  }
  return map[String(estado || '').toUpperCase()] || (estado || '—')
}

export function getEstadoConductorSeverity(estado) {
  switch (String(estado || '').toUpperCase()) {
    case 'DISPONIBLE':
      return 'success'
    case 'EN_RUTA':
      return 'info'
    case 'DESCANSO':
      return 'warn'
    case 'SUSPENDIDO':
      return 'danger'
    default:
      return 'secondary'
  }
}

export function formatPesoKg(kg) {
  const num = Number(kg || 0)
  return `${num.toLocaleString('es-PE')} kg`
}

export function formatKilometraje(km) {
  const num = Number(km || 0)
  return `${num.toLocaleString('es-PE')} km`
}

export { formatDate, formatDateTime }
