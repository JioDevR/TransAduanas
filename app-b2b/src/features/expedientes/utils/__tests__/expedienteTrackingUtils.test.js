import { describe, it, expect } from 'vitest'
import {
  isExportExpediente,
  getExpedientePhase,
  getCanalMeta,
  getAlmacenAlertClass,
  calculateLeadTime,
  calculateElapsedDays,
  formatEstadoExpedienteLabel,
  buildTimelineSteps,
  calculateTrackingProgress,
  buildRouteSummary,
  getEventToneMeta,
  sortTrackingEvents,
  getDocumentIcon
} from '../expedienteTrackingUtils'

describe('expedienteTrackingUtils', () => {
  it('detecta exportaciones por régimen o tipo de carga', () => {
    expect(isExportExpediente({ regimenAduanero: 'EXPORTACION_DEFINITIVA' })).toBe(true)
    expect(isExportExpediente({ regimenAduanero: 'IMPORTACION_DEFINITIVA' })).toBe(false)
  })

  it('determina correctamente la fase operativa del expediente', () => {
    expect(getExpedientePhase({ estadoExpediente: 'CERRADO' }).label).toBe('Cerrado Conforme')
    expect(getExpedientePhase({ estadoExpediente: 'LEVANTE_AUTORIZADO' }).label).toBe('Levante Autorizado')
    expect(getExpedientePhase({ estadoExpediente: 'INSPECCION', canalSunat: 'ROJO' }).label).toBe('Aforo Físico SUNAT')
    expect(getExpedientePhase({ estadoExpediente: 'EN_TRAMITE', canalSunat: 'NARANJA' }).label).toBe('Revisión Documentaria')
    expect(getExpedientePhase({ estadoExpediente: 'ASIGNADO' }).label).toBe('Apertura / Pendiente Salida')
  })

  it('retorna severidad y descripción correcta para canales SUNAT', () => {
    expect(getCanalMeta('VERDE')).toMatchObject({ label: 'Canal VERDE', severity: 'success' })
    expect(getCanalMeta('NARANJA')).toMatchObject({ label: 'Canal NARANJA', severity: 'warn' })
    expect(getCanalMeta('ROJO')).toMatchObject({ label: 'Canal ROJO', severity: 'danger' })
    expect(getCanalMeta(null)).toMatchObject({ label: 'Canal PENDIENTE', severity: 'secondary' })
  })

  it('clasifica alertas de días libres de almacén y calcula lead time / días en curso', () => {
    expect(getAlmacenAlertClass(2)).toBe('is-danger')
    expect(getAlmacenAlertClass(5)).toBe('is-warn')
    expect(getAlmacenAlertClass(12)).toBe('is-info')
    expect(calculateLeadTime({ fechaApertura: '2026-11-01', fechaCierre: '2026-11-11' })).toBe('10 días de ciclo')
    expect(calculateLeadTime({ fechaApertura: '2026-11-01' })).toBeNull()

    const now = new Date('2026-11-10T12:00:00Z')
    expect(calculateElapsedDays({ estadoExpediente: 'EN_CANAL', fechaApertura: '2026-11-05T12:00:00Z' }, now)).toBe('5 días en curso')
    expect(calculateElapsedDays({ estadoExpediente: 'ASIGNADO', fechaApertura: '2026-11-10T10:00:00Z' }, now)).toBe('Hoy')
    expect(calculateElapsedDays({ estadoExpediente: 'CERRADO', fechaApertura: '2026-11-01', fechaCierre: '2026-11-06' }, now)).toBe('5 días de ciclo')
  })

  it('formatea etiquetas de estado del expediente', () => {
    expect(formatEstadoExpedienteLabel('LEVANTE_AUTORIZADO')).toBe('Levante Autorizado')
    expect(formatEstadoExpedienteLabel('CERRADO')).toBe('Cerrado Conforme')
    expect(formatEstadoExpedienteLabel(null)).toBe('En Proceso')
  })

  it('construye los 6 pasos del timeline operativo y calcula progreso', () => {
    const steps = buildTimelineSteps({
      estadoExpediente: 'LEVANTE_AUTORIZADO',
      fechaZarpeEtd: '2026-11-02',
      fechaArriboAduana: '2026-11-10T12:00:00',
      numeroDam: '118-2026-10-001234',
      canalSunat: 'VERDE',
      fechaLevanteAutorizacion: '2026-11-12T10:00:00'
    })

    expect(steps).toHaveLength(6)
    expect(steps.slice(0, 5).every(s => s.completed)).toBe(true)
    expect(steps[5].completed).toBe(false)
    expect(steps[5].active).toBe(true)

    const progress = calculateTrackingProgress(steps)
    expect(progress.completedCount).toBe(5)
    expect(progress.totalSteps).toBe(6)
    expect(progress.percentage).toBe(83)
    expect(progress.isFinished).toBe(false)
    expect(progress.activeStep?.label).toBe('Cierre Formal')

    expect(calculateTrackingProgress([])).toEqual({
      completedCount: 0,
      totalSteps: 0,
      percentage: 0,
      activeStep: null,
      isFinished: false
    })
  })

  it('construye la tira de ruta logística para importación y exportación', () => {
    const impRoute = buildRouteSummary({
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      contactoExteriorNombre: 'Shenzhen Co.',
      fechaZarpeEtd: '2026-11-01',
      naveViaje: 'MSC CAPELLA',
      numeroContenedor: 'MSKU9087651',
      fechaArriboAduana: '2026-11-10T08:00:00',
      depositoTemporal: 'DP World Callao',
      distritoLlegada: 'Ate Vitarte',
      estadoExpediente: 'EN_CANAL'
    })
    expect(impRoute).toHaveLength(4)
    expect(impRoute[0]).toMatchObject({ kicker: 'ORIGEN EXTERIOR', place: 'Shenzhen Co.', status: 'reached' })
    expect(impRoute[1]).toMatchObject({ kicker: 'TRÁNSITO INTERNACIONAL', place: 'MSC CAPELLA', status: 'reached' })
    expect(impRoute[2]).toMatchObject({ kicker: 'ADUANA & TERMINAL', place: 'DP World Callao', status: 'active' })
    expect(impRoute[3]).toMatchObject({ kicker: 'ENTREGA EN PLANTA', place: 'Ate Vitarte', status: 'pending' })

    const expRoute = buildRouteSummary({
      regimenAduanero: 'EXPORTACION_DEFINITIVA',
      direccionPartida: 'Av. Industrial 450',
      depositoTemporal: 'Talma Callao',
      numeroVuelo: 'LA-2462',
      contactoExteriorNombre: 'Miami Fresh LLC',
      estadoExpediente: 'LEVANTE_AUTORIZADO',
      fechaArriboAduana: '2026-11-10T08:00:00',
      fechaLevanteAutorizacion: '2026-11-11T10:00:00'
    })
    expect(expRoute).toHaveLength(4)
    expect(expRoute[0].kicker).toBe('PLANTA / RECOJO')
    expect(expRoute[2].place).toBe('LA-2462')
    expect(expRoute[3].place).toBe('Miami Fresh LLC')
  })

  it('ordena los eventos cronológicamente (más reciente primero) y asigna tono', () => {
    expect(getEventToneMeta('LEVANTE_AUTORIZADO').tone).toBe('success')
    expect(getEventToneMeta('CUADRILLA_CITA_ROJO').tone).toBe('danger')
    expect(getEventToneMeta('CANAL_ASIGNADO').tone).toBe('warn')
    expect(getEventToneMeta('ARRIBO_PUERTO_AEROPUERTO').tone).toBe('info')
    expect(getEventToneMeta('OTRO').tone).toBe('neutral')

    const sorted = sortTrackingEvents([
      { idEvento: 1, codigoEstado: 'EXPEDIENTE_CREADO', fechaEvento: '2026-11-01T10:00:00' },
      { idEvento: 3, codigoEstado: 'LEVANTE_AUTORIZADO', fechaEvento: '2026-11-05T15:00:00' },
      { idEvento: 2, codigoEstado: 'CANAL_ASIGNADO', fechaEvento: '2026-11-03T12:00:00' }
    ])
    expect(sorted.map(e => e.idEvento)).toEqual([3, 2, 1])
    expect(sorted[0].toneMeta.tone).toBe('success')
    expect(sortTrackingEvents(null)).toEqual([])
  })

  it('asigna íconos correctos según MIME de documento', () => {
    expect(getDocumentIcon('application/pdf')).toBe('pi pi-file-pdf')
    expect(getDocumentIcon('image/png')).toBe('pi pi-image')
    expect(getDocumentIcon('application/vnd.ms-excel')).toBe('pi pi-file-excel')
    expect(getDocumentIcon(null)).toBe('pi pi-file')
  })

  it('construye timeline específico de exportación y fase pendiente de agente', () => {
    expect(getExpedientePhase({ estadoExpediente: 'ASIGNADO', pendienteAsignacionAgente: true }).label).toBe('Por Asignar Agente')

    const stepsExport = buildTimelineSteps({
      regimenAduanero: 'EXPORTACION_DEFINITIVA',
      estadoExpediente: 'LEVANTE_AUTORIZADO',
      fechaArriboAduana: '2026-11-10T09:00:00',
      numeroDam: '118-2026-40-000111',
      canalSunat: 'VERDE',
      fechaLevanteAutorizacion: '2026-11-11T10:00:00',
      numeroVuelo: 'LA-2462',
      observaciones: '[EMBARQUE CONFIRMADO] Vuelo LA-2462'
    })
    expect(stepsExport).toHaveLength(6)
    expect(stepsExport[1].label).toBe('Almacén Central')
    expect(stepsExport[4].label).toBe('Vuelo / Embarque')
    expect(stepsExport[4].completed).toBe(true)
    expect(stepsExport[4].detail).toBe('LA-2462')
  })
})
