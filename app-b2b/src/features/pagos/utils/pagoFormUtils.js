export const CUENTAS_BANCARIAS_USD = [
  {
    id: 'bcp-usd',
    banco: 'Banco de Crédito BCP',
    nombre: 'BCP Dolares TransAduanas',
    numeroCuenta: '191-289381-1-09',
    cci: '002-19100289381109-54'
  },
  {
    id: 'bbva-usd',
    banco: 'BBVA Perú',
    nombre: 'BBVA Dolares TransAduanas',
    numeroCuenta: '0011-0182-010049281',
    cci: '011-182-00010049281-22'
  },
  {
    id: 'interbank-usd',
    banco: 'Interbank',
    nombre: 'Interbank Dolares TransAduanas',
    numeroCuenta: '200-300189281',
    cci: '003-200-00300189281-19'
  }
]

export const BANCOS_ORIGEN_OPTIONS = [
  { label: 'Banco de Crédito del Perú (BCP)', value: 'BCP' },
  { label: 'BBVA Perú', value: 'BBVA' },
  { label: 'Interbank', value: 'INTERBANK' },
  { label: 'Scotiabank Perú', value: 'SCOTIABANK' },
  { label: 'BanBif', value: 'BANBIF' },
  { label: 'Banco Pichincha', value: 'BANCO_PICHINCHA' },
  { label: 'Banco de Comercio', value: 'BANCO_COMERCIO' },
  { label: 'Transferencia Internacional / Swift', value: 'TRANSFERENCIA_EXTERIOR' }
]

export const METODOS_PAGO_OPTIONS = [
  { label: 'Transferencia Bancaria Vía Web/App', value: 'TRANSFERENCIA_BANCARIA' },
  { label: 'Transferencia Interbancaria (CCI)', value: 'TRANSFERENCIA_INTERBANCARIA' },
  { label: 'Depósito en Ventanilla / Agente', value: 'DEPOSITO_EN_VENTANILLA' },
  { label: 'Transferencia Swift Exterior', value: 'TRANSFERENCIA_INTERNACIONAL' }
]

export function calcularMontoPorcentaje(saldoMax, pct) {
  const max = Number(saldoMax || 0)
  if (!Number.isFinite(max) || max <= 0) return 0
  if (pct === 100) return Number((Math.round((max * 100) + Number.EPSILON) / 100).toFixed(2))
  return Number((Math.round((max * pct) + Number.EPSILON) / 100).toFixed(2))
}

export function getPagoTagSeverity(estadoPago) {
  switch (String(estadoPago || '').toUpperCase()) {
    case 'PAGADO':
    case 'CONCILIADO':
    case 'PAGADO_TOTAL':
    case 'APROBADO':
    case 'ADELANTO_APROBADO':
    case 'VALIDADO':
      return 'success'
    case 'PENDIENTE_REVISION':
      return 'info'
    case 'OBSERVADO':
    case 'RECHAZADO':
      return 'danger'
    default:
      return 'warn'
  }
}

export const MIN_ANTICIPO_PCT = 30

export function isCotizacionObligacion(tipoObligacion) {
  return tipoObligacion === 'COTIZACION' || tipoObligacion === 'ANTICIPO_COTIZACION'
}

export function getTipoObligacionMeta(tipoObligacion) {
  if (isCotizacionObligacion(tipoObligacion)) {
    return { label: 'Anticipo Pre-Expediente', severity: 'info' }
  }
  return { label: 'Despacho Aduanero', severity: 'secondary' }
}

export function calcularMinimoAnticipoMayor30(totalContratado) {
  const total = Number(totalContratado || 0)
  if (!Number.isFinite(total) || total <= 0) return 0
  const base30 = Number((Math.round((total * 30) + Number.EPSILON) / 100).toFixed(2))
  return Number((base30 + 0.01).toFixed(2))
}

export function validarRegistroPagoForm({
  montoAbonado,
  saldoPendienteMax,
  totalContratadoUsd = 0,
  totalAmortizadoUsd = 0,
  numeroOperacion,
  bancoOrigen,
  archivoVoucher
}) {
  const monto = Number(montoAbonado || 0)
  const saldo = Number(saldoPendienteMax || 0)
  const totalContratado = Number(totalContratadoUsd || 0)
  const totalAmortizado = Number(totalAmortizadoUsd || 0)

  if (!Number.isFinite(monto) || monto <= 0) {
    return { valid: false, error: 'Por favor ingrese un monto válido a abonar.' }
  }
  if (saldo > 0 && monto > saldo) {
    return { valid: false, error: 'El monto ingresado excede el saldo pendiente de la obligación.' }
  }
  if (totalAmortizado === 0 && totalContratado > 0 && saldo > 0 && monto < saldo) {
    const umbral30 = Number((Math.round((totalContratado * 30) + Number.EPSILON) / 100).toFixed(2))
    if (monto <= umbral30) {
      return {
        valid: false,
        error: `El anticipo inicial debe ser mayor al ${MIN_ANTICIPO_PCT}% del total cotizado (> $ ${umbral30.toFixed(2)} USD) o cancelar el 100% del saldo.`
      }
    }
  }
  if (!String(bancoOrigen || '').trim()) {
    return { valid: false, error: 'Debe seleccionar el banco emisor.' }
  }
  if (!String(numeroOperacion || '').trim()) {
    return { valid: false, error: 'Debe registrar el número de operación bancaria.' }
  }
  if (!archivoVoucher) {
    return { valid: false, error: 'Debe adjuntar la constancia o voucher bancario en formato PDF o Imagen.' }
  }
  return { valid: true }
}

export function validarSubsanacionPagoForm({
  montoAbonado,
  numeroOperacion,
  bancoOrigen,
  observaciones,
  tieneComprobantePrevio,
  nuevoArchivo
}) {
  const monto = Number(montoAbonado || 0)
  if (!Number.isFinite(monto) || monto <= 0) {
    return { valid: false, error: 'Debe ingresar un monto abonado válido mayor a 0.' }
  }
  if (!String(numeroOperacion || '').trim()) {
    return { valid: false, error: 'Debe registrar el número de operación bancaria.' }
  }
  if (!String(bancoOrigen || '').trim()) {
    return { valid: false, error: 'Debe indicar el banco emisor.' }
  }
  if (!String(observaciones || '').trim()) {
    return { valid: false, error: 'Debe incluir un sustento o aclaración de la corrección para Finanzas.' }
  }
  if (!tieneComprobantePrevio && !nuevoArchivo) {
    return { valid: false, error: 'Debe adjuntar un archivo de voucher bancario para subsanar el registro.' }
  }
  return { valid: true }
}
