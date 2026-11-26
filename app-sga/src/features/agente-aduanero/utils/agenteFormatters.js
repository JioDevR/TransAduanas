import {
  formatRegimenShort,
  formatRegimenFull,
  getCanalSeverity
} from '@/features/aduanas/utils/aduanasFormatters'
import { formatDateShort, formatDateTime } from '@/shared/utils/dateUtils'
import { hasCanalAsignado, hasArriboConfirmado, hasZarpeConfirmado, isLevanteAutorizado } from './agenteWorkflowUtils'

export { formatRegimenShort, formatRegimenFull, getCanalSeverity, formatDateShort, formatDateTime }

const ESTADO_LABELS = {
  ASIGNADO: 'Asignado',
  EN_TRAMITE: 'En Tránsito',
  INSPECCION: 'En Inspección',
  LEVANTE_AUTORIZADO: 'Levante Autorizado',
  CERRADO: 'Cerrado',
  ANULADO: 'Anulado'
}

export function formatEstadoAgente(estado) {
  return ESTADO_LABELS[estado] || (estado ? String(estado).replace(/_/g, ' ') : 'Asignado')
}

export function getExpedienteSeverity(estado) {
  switch (estado) {
    case 'LEVANTE_AUTORIZADO':
      return 'success'
    case 'INSPECCION':
      return 'warn'
    case 'CERRADO':
      return 'secondary'
    case 'ANULADO':
      return 'danger'
    case 'ASIGNADO':
    case 'EN_TRAMITE':
    default:
      return 'info'
  }
}

export function getCanalLabel(exp) {
  if (!exp) return 'Pendiente'
  if (hasCanalAsignado(exp)) return `Canal ${exp.canalSunat}`
  if (hasArriboConfirmado(exp)) return 'Canal Pendiente'
  return 'En espera de arribo'
}

export function getCanalHelpText(exp) {
  if (!exp) return 'Pendiente de asignación.'
  if (exp.canalSunat === 'VERDE') return 'Levante automático sin revisión documental ni física.'
  if (exp.canalSunat === 'NARANJA') return 'Revisión documentaria por especialista de aduanas.'
  if (exp.canalSunat === 'ROJO') return 'Requiere aforo físico de mercancía en terminal.'
  if (hasArriboConfirmado(exp)) return 'Mercancía en puerto. Supervisor asignará el canal oficial.'
  return 'Se asignará una vez confirmado el arribo de la nave a destino.'
}

export function getProgresoResumen(exp) {
  if (!exp) return 'Sin datos'
  if (exp.estadoExpediente === 'CERRADO') return 'Expediente Cerrado con Conformidad'
  if (isLevanteAutorizado(exp)) return 'Fase 5/5: Levante Autorizado'
  if (exp.canalSunat === 'ROJO' || exp.canalSunat === 'NARANJA') {
    return `En Gestión SUNAT: Diligencia Canal ${exp.canalSunat}`
  }
  if (hasCanalAsignado(exp)) return 'Fase 4/5: Canal SUNAT asignado'
  if (hasArriboConfirmado(exp)) return 'Fase 3/5: Arribado (esperando Canal)'
  if (hasZarpeConfirmado(exp)) return 'Fase 2/5: En tránsito internacional'
  return 'Fase 1/5: Asignado al agente'
}

export function formatFechaHitoCard(exp) {
  if (!exp) return 'Por programar'
  if (exp.fechaArriboAduana) return formatDateTime(exp.fechaArriboAduana)
  if (exp.fechaLlegadaEta) return `ETA: ${formatDateShort(exp.fechaLlegadaEta)}`
  if (exp.fechaZarpeEtd) return `ETD: ${formatDateShort(exp.fechaZarpeEtd)}`
  return 'Por programar'
}
