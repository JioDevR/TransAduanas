/**
 * Cálculos puros de KPIs ejecutivos y datasets Chart.js para Gerencia General.
 */

export function unwrapListPayload(res) {
  if (Array.isArray(res)) return res
  if (Array.isArray(res?.data)) return res.data
  if (Array.isArray(res?.content)) return res.content
  return []
}

export function computeGerenciaDashboardMetrics(sources = {}) {
  const sedes = unwrapListPayload(sources.sedes)
  const puertos = unwrapListPayload(sources.puertos)
  const tiposCarga = unwrapListPayload(sources.tiposCarga)
  const mercancias = unwrapListPayload(sources.mercancias)
  const unidades = unwrapListPayload(sources.unidades)
  const conductores = unwrapListPayload(sources.conductores)

  const sedesCount = sedes.length
  const sedesOperativasCount = sedes.filter((s) => s.estado === 'OPERATIVO').length
  const sedesMantenimientoCount = sedes.filter((s) => s.estado === 'MANTENIMIENTO').length
  const distritosCount = new Set(sedes.map((s) => s.distrito).filter(Boolean)).size

  const puertosCount = puertos.length
  const puertosActivosCount = puertos.filter((p) => p.estado === 'ACTIVO').length
  const puertosMaritimosCount = puertos.filter((p) => (p.tipo || p.tipoPuerto) === 'MARITIMO').length
  const puertosAereosCount = puertos.filter((p) => (p.tipo || p.tipoPuerto) === 'AEREO').length
  const puertosOtrosCount = Math.max(0, puertosCount - puertosMaritimosCount - puertosAereosCount)

  const cargasCount = tiposCarga.length
  const cargasActivasCount = tiposCarga.filter((t) => t.estado === 'ACTIVO').length

  const mercanciasCount = mercancias.length
  const mercanciasActivasCount = mercancias.filter((m) => m.estado === 'ACTIVO').length
  const mercanciasVuceCount = mercancias.filter((m) => Boolean(m.requierePermiso)).length

  const unidadesCount = unidades.length
  const flotaOperativaCount = unidades.filter(
    (u) => u.estadoOperativo === 'DISPONIBLE' || u.estadoOperativo === 'EN_RUTA'
  ).length
  const flotaTallerCount = unidades.filter(
    (u) => u.estadoOperativo === 'MANTENIMIENTO' || u.estadoOperativo === 'FUERA_DE_SERVICIO'
  ).length
  const totalKg = unidades.reduce((acc, u) => acc + (Number(u.capacidadCargaKg) || 0), 0)
  const totalCapacidadToneladas = Math.round(totalKg / 1000)

  const conductoresCount = conductores.length
  const conductoresDisponiblesCount = conductores.filter(
    (c) => c.estadoOperativo === 'DISPONIBLE'
  ).length

  const coberturaChartData = {
    labels: ['Sedes Territoriales', 'Puertos & Terminales', 'Tipos de Carga', 'Partidas Arancelarias'],
    datasets: [
      {
        data: [sedesCount, puertosCount, cargasCount, mercanciasCount],
        backgroundColor: ['#4a3728', '#8c5a3c', '#c88a4b', '#3a6b56'],
        borderWidth: 0
      }
    ]
  }

  const totalByDomain = [
    sedesCount,
    puertosCount,
    cargasCount,
    mercanciasCount,
    unidadesCount,
    conductoresCount
  ]
  const activeByDomain = [
    sedesOperativasCount,
    puertosActivosCount,
    cargasActivasCount,
    mercanciasActivasCount,
    flotaOperativaCount,
    conductoresDisponiblesCount
  ]
  const inactiveByDomain = totalByDomain.map((tot, i) => Math.max(0, tot - activeByDomain[i]))

  const disponibilidadChartData = {
    labels: ['Sedes', 'Puertos', 'Tipos Carga', 'Mercancías', 'Flota', 'Conductores'],
    datasets: [
      {
        label: 'Habilitados / Operativos',
        data: activeByDomain,
        backgroundColor: '#3a6b56',
        borderRadius: 6
      },
      {
        label: 'Inactivos / Mantenimiento',
        data: inactiveByDomain,
        backgroundColor: '#d6cfc7',
        borderRadius: 6
      }
    ]
  }

  return {
    sedesCount,
    sedesOperativasCount,
    sedesMantenimientoCount,
    distritosCount,
    puertosCount,
    puertosActivosCount,
    puertosMaritimosCount,
    puertosAereosCount,
    puertosOtrosCount,
    cargasCount,
    cargasActivasCount,
    mercanciasCount,
    mercanciasActivasCount,
    mercanciasVuceCount,
    unidadesCount,
    flotaOperativaCount,
    flotaTallerCount,
    totalCapacidadToneladas,
    conductoresCount,
    conductoresDisponiblesCount,
    coberturaChartData,
    disponibilidadChartData
  }
}
