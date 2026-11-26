import { parseLocalDate } from '@/shared/utils/dateUtils'

function startOfDay(date = new Date()) {
  const d = date instanceof Date ? date : new Date(date)
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

export function diasParaVencer(fechaStr, refDate = new Date()) {
  if (!fechaStr) return null
  const target = parseLocalDate(fechaStr)
  if (!target || isNaN(target.getTime())) return null
  const today = startOfDay(refDate)
  return Math.round((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
}

export function isFechaVencida(fechaStr, refDate = new Date()) {
  const dias = diasParaVencer(fechaStr, refDate)
  return dias !== null && dias < 0
}

export function isFechaPorVencer(fechaStr, limiteDias = 30, refDate = new Date()) {
  const dias = diasParaVencer(fechaStr, refDate)
  return dias !== null && dias >= 0 && dias <= limiteDias
}

export function buildAlertasMtc(unidades = [], conductores = [], refDate = new Date(), limiteDias = 30) {
  const alertas = []

  for (const u of unidades) {
    if (u.estadoActivo === false) continue

    const soatDias = diasParaVencer(u.soatVencimiento, refDate)
    if (soatDias !== null && soatDias <= limiteDias) {
      const vencido = soatDias < 0
      alertas.push({
        id: `soat-${u.idUnidad || u.placaTracto}`,
        tipo: 'SOAT',
        targetTab: 'unidades',
        titulo: `${u.placaTracto} · SOAT ${vencido ? 'Vencido' : 'Por Vencer'}`,
        subtitulo: u.marcaModelo || 'Unidad de flota',
        vencimiento: u.soatVencimiento,
        diasRestantes: soatDias,
        vencido,
        severity: vencido ? 'danger' : 'warn'
      })
    }

    const citvDias = diasParaVencer(u.citvVencimiento, refDate)
    if (citvDias !== null && citvDias <= limiteDias) {
      const vencido = citvDias < 0
      alertas.push({
        id: `citv-${u.idUnidad || u.placaTracto}`,
        tipo: 'CITV',
        targetTab: 'unidades',
        titulo: `${u.placaTracto} · CITV ${vencido ? 'Vencido' : 'Por Vencer'}`,
        subtitulo: u.marcaModelo || 'Unidad de flota',
        vencimiento: u.citvVencimiento,
        diasRestantes: citvDias,
        vencido,
        severity: vencido ? 'danger' : 'warn'
      })
    }
  }

  for (const c of conductores) {
    if (c.estadoActivo === false) continue
    const licDias = diasParaVencer(c.fechaVencimientoLicencia, refDate)
    if (licDias !== null && licDias <= limiteDias) {
      const vencido = licDias < 0
      const nombre = c.nombreCompleto || `${c.nombres || ''} ${c.apellidos || ''}`.trim()
      alertas.push({
        id: `lic-${c.idConductor || c.numeroLicencia}`,
        tipo: 'LICENCIA',
        targetTab: 'conductores',
        titulo: `${nombre} · Brevete ${vencido ? 'Vencido' : 'Por Vencer'}`,
        subtitulo: `Lic. ${c.numeroLicencia || 'S/N'} (${c.tipoLicencia || 'MTC'})`,
        vencimiento: c.fechaVencimientoLicencia,
        diasRestantes: licDias,
        vencido,
        severity: vencido ? 'danger' : 'warn'
      })
    }
  }

  return alertas.sort((a, b) => a.diasRestantes - b.diasRestantes)
}

export function buildTransporteKpis(despachos = [], unidades = [], conductores = [], alertas = []) {
  const norm = (v) => String(v || '').toUpperCase()

  const despachosPendientes = despachos.filter((d) => norm(d.estadoTransporte) === 'PENDIENTE_ASIGNACION').length
  const despachosProgramados = despachos.filter((d) => norm(d.estadoTransporte) === 'PROGRAMADO').length
  const despachosEnRuta = despachos.filter((d) => norm(d.estadoTransporte) === 'EN_RUTA').length
  const despachosEntregados = despachos.filter((d) => norm(d.estadoTransporte) === 'ENTREGADO').length
  const despachosCancelados = despachos.filter((d) => norm(d.estadoTransporte) === 'CANCELADO').length

  const unidadesActivasList = unidades.filter((u) => u.estadoActivo !== false)
  const unidadesDisponibles = unidadesActivasList.filter((u) => norm(u.estadoOperativo) === 'DISPONIBLE').length
  const tractosDisponibles = unidadesActivasList.filter(
    (u) => norm(u.estadoOperativo) === 'DISPONIBLE' && norm(u.tipoVehiculo) === 'TRACTO'
  ).length
  const unidadesEnRuta = unidadesActivasList.filter((u) => norm(u.estadoOperativo) === 'EN_RUTA').length
  const unidadesMantenimiento = unidadesActivasList.filter((u) =>
    ['MANTENIMIENTO', 'FUERA_DE_SERVICIO'].includes(norm(u.estadoOperativo))
  ).length

  const conductoresActivosList = conductores.filter((c) => c.estadoActivo !== false)
  const conductoresDisponibles = conductoresActivosList.filter((c) => norm(c.estadoOperativo) === 'DISPONIBLE').length
  const conductoresEnRuta = conductoresActivosList.filter((c) => norm(c.estadoOperativo) === 'EN_RUTA').length
  const licenciasA3c = conductoresActivosList.filter((c) => String(c.tipoLicencia || '').toUpperCase() === 'A-IIIC').length

  const alertasVencidas = alertas.filter((a) => a.vencido).length
  const alertasPorVencer = alertas.filter((a) => !a.vencido).length

  return {
    despachosTotal: despachos.length,
    despachosPendientes,
    despachosProgramados,
    despachosEnRuta,
    despachosEntregados,
    despachosCancelados,
    unidadesTotal: unidades.length,
    unidadesActivas: unidadesActivasList.length,
    unidadesDisponibles,
    tractosDisponibles,
    unidadesEnRuta,
    unidadesMantenimiento,
    conductoresTotal: conductores.length,
    conductoresActivos: conductoresActivosList.length,
    conductoresDisponibles,
    conductoresEnRuta,
    licenciasA3c,
    alertasTotal: alertas.length,
    alertasVencidas,
    alertasPorVencer
  }
}
