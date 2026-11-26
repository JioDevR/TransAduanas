import { formatCurrency, formatEstado, getEstadoSeverity } from '@/shared/utils/formatters'

export const CUENTAS_RECAUDADORAS_SGA = [
  { label: 'BCP Cta Cte USD (191-0023412-1-44)', value: 'BCP Dólares Cta Cte 191-0023412-1-44' },
  { label: 'BCP Cta Cte Soles (191-0023412-0-43)', value: 'BCP Soles Cta Cte 191-0023412-0-43' },
  { label: 'BBVA Cta Cte USD (0011-0182-0100034567)', value: 'BBVA Dólares Cta Cte 0011-0182-0100034567' },
  { label: 'BBVA Cta Cte Soles (0011-0182-0100034568)', value: 'BBVA Soles Cta Cte 0011-0182-0100034568' },
  { label: 'Interbank Cta Cte USD (200-3001245678)', value: 'Interbank Dólares Cta Cte 200-3001245678' },
  { label: 'Scotiabank Soles (000-4567890)', value: 'Scotiabank Soles Cta Cte 000-4567890' }
]

export const METODOS_PAGO_COTIZACION = [
  { label: 'Transferencia Bancaria Directa', value: 'TRANSFERENCIA_BANCARIA' },
  { label: 'Depósito en Ventanilla / Agente', value: 'DEPOSITO_VENTANILLA' },
  { label: 'Transferencia Interbancaria (CCI)', value: 'TRANSFERENCIA_CCI' },
  { label: 'Cheque de Gerencia', value: 'CHEQUE_GERENCIA' },
  { label: 'Pago con Tarjeta / Link Digital', value: 'PAGO_LINK' },
  { label: 'Carta Fianza Bancaria', value: 'CARTA_FIANZA' }
]

export const METODOS_PAGO_LIQUIDACION = [
  { label: 'Transferencia Bancaria', value: 'TRANSFERENCIA_BANCARIA' },
  { label: 'Depósito en Ventanilla', value: 'DEPOSITO_EN_VENTANILLA' },
  { label: 'Pago Electrónico / Pasarela', value: 'PAGO_ELECTRONICO' },
  { label: 'Cheque de Gerencia', value: 'CHEQUE' }
]

export const BANCOS_DESTINO_LIQUIDACION = [
  { label: 'Banco de Crédito (BCP)', value: 'BCP' },
  { label: 'BBVA Perú', value: 'BBVA' },
  { label: 'Interbank', value: 'INTERBANK' },
  { label: 'Scotiabank', value: 'SCOTIABANK' },
  { label: 'Banco de la Nación (Detracciones)', value: 'BANCO_NACION' }
]

export const MOTIVOS_OBSERVACION_ABONO = [
  { label: 'Voucher ilegible o incompleto', value: 'Voucher ilegible o incompleto' },
  { label: 'Monto abonado no coincide con cotización', value: 'Monto abonado no coincide con cotización' },
  { label: 'N° de Operación no figura en extracto bancario', value: 'N° de Operación no figura en extracto bancario' },
  { label: 'Comprobante no corresponde a TransAduanas', value: 'Comprobante no corresponde a TransAduanas' },
  { label: 'Moneda o cuenta destino incorrecta', value: 'Moneda o cuenta destino incorrecta' }
]

export function formatPen(val) {
  return formatCurrency(val, 'PEN')
}

export function formatUsd(val) {
  return formatCurrency(val, 'USD')
}

export function formatEstadoLiquidacion(est) {
  if (!est) return 'Desconocido'
  if (String(est).toUpperCase() === 'EN_CONCILIACION') return 'En Conciliación'
  return formatEstado(est)
}

export function getEstadoLiquidacionSeverity(est) {
  if (String(est).toUpperCase() === 'EN_CONCILIACION') return 'info'
  return getEstadoSeverity(est)
}

export function formatTipoAbono(tipo) {
  switch (String(tipo || '').toUpperCase()) {
    case 'ADELANTO':
      return 'Adelanto / Anticipo'
    case 'PAGO_TOTAL':
      return 'Pago Total (100%)'
    case 'GARANTIA_CREDITO':
      return 'Línea de Crédito'
    default:
      return tipo || 'Abono'
  }
}

export function getEstadoAnticipoLabel(st) {
  switch (String(st || '').toUpperCase()) {
    case 'PENDIENTE_REVISION':
      return 'Pendiente Revisión'
    case 'APROBADO':
      return 'Aprobado / Conforme'
    case 'OBSERVADO':
      return 'Observado'
    case 'RECHAZADO':
      return 'Rechazado'
    default:
      return st || 'Pendiente'
  }
}

export function getEstadoAnticipoSeverity(st) {
  switch (String(st || '').toUpperCase()) {
    case 'APROBADO':
      return 'success'
    case 'PENDIENTE_REVISION':
      return 'warn'
    case 'OBSERVADO':
    case 'RECHAZADO':
      return 'danger'
    default:
      return 'info'
  }
}

export function formatEstadoPagoCotizacionLabel(st) {
  switch (String(st || '').toUpperCase()) {
    case 'PENDIENTE_REVISION':
      return 'En Revisión'
    case 'ADELANTO_APROBADO':
      return 'Adelanto Conforme'
    case 'PAGADO_TOTAL':
      return 'Cancelado 100%'
    case 'OBSERVADO':
      return 'Observado'
    case 'RECHAZADO':
      return 'Rechazado'
    case 'SIN_PAGO':
    default:
      return 'Sin Abono'
  }
}

export function formatEstadoPagoCotizacionSeverity(st) {
  switch (String(st || '').toUpperCase()) {
    case 'PAGADO_TOTAL':
    case 'ADELANTO_APROBADO':
      return 'success'
    case 'PENDIENTE_REVISION':
      return 'warn'
    case 'OBSERVADO':
    case 'RECHAZADO':
      return 'danger'
    case 'SIN_PAGO':
    default:
      return 'secondary'
  }
}

export function formatMetodoPagoLabel(metodo) {
  const map = {
    TRANSFERENCIA_BANCARIA: 'Transferencia Bancaria',
    DEPOSITO_VENTANILLA: 'Depósito en Ventanilla',
    DEPOSITO_EN_VENTANILLA: 'Depósito en Ventanilla',
    TRANSFERENCIA_CCI: 'Transferencia CCI',
    CHEQUE_GERENCIA: 'Cheque de Gerencia',
    CHEQUE: 'Cheque de Gerencia',
    PAGO_LINK: 'Tarjeta / Link Digital',
    PAGO_ELECTRONICO: 'Pago Electrónico',
    CARTA_FIANZA: 'Carta Fianza'
  }
  return map[String(metodo || '').toUpperCase()] || (metodo ? String(metodo).replace(/_/g, ' ') : 'Transferencia')
}
