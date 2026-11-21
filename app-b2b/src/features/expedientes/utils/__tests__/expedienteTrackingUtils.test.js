import { describe, it, expect } from 'vitest'
import {
  isExportExpediente,
  getExpedientePhase,
  getCanalMeta,
  getAlmacenAlertClass,
  calculateLeadTime,
  buildTimelineSteps,
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

  it('clasifica alertas de días libres de almacén y calcula lead time', () => {
    expect(getAlmacenAlertClass(2)).toBe('is-danger')
    expect(getAlmacenAlertClass(5)).toBe('is-warn')
    expect(getAlmacenAlertClass(12)).toBe('is-info')
    expect(calculateLeadTime({ fechaApertura: '2026-11-01', fechaCierre: '2026-11-11' })).toBe('10 días de ciclo')
    expect(calculateLeadTime({ fechaApertura: '2026-11-01' })).toBeNull()
  })

  it('construye los 6 pasos del timeline operativo', () => {
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
  })

  it('asigna íconos correctos según MIME de documento', () => {
    expect(getDocumentIcon('application/pdf')).toBe('pi pi-file-pdf')
    expect(getDocumentIcon('image/png')).toBe('pi pi-image')
    expect(getDocumentIcon('application/vnd.ms-excel')).toBe('pi pi-file-excel')
    expect(getDocumentIcon(null)).toBe('pi pi-file')
  })
})
