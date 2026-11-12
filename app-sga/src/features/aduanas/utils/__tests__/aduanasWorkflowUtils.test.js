import { describe, it, expect } from 'vitest'
import {
  clasificarCotizacionesAduaneras,
  filtrarExpedientesPorFase,
  obtenerAccionesPermitidasExpediente
} from '../aduanasWorkflowUtils'

describe('aduanasWorkflowUtils', () => {
  it('clasifica cotizaciones en las 5 sub-colas correctamente', () => {
    const data = [
      { id: 1, estado: 'PENDIENTE', motivoObservacion: null, fechaRevision: null },
      { id: 2, estado: 'OBSERVADA', motivoObservacion: 'Corregir partida' },
      { id: 3, estado: 'PENDIENTE', motivoObservacion: 'Corregir partida', fechaRevision: '2026-11-10' },
      { id: 4, estado: 'APROBADA', tienePagoAprobado: false, tieneExpediente: false },
      { id: 5, estado: 'APROBADA', tienePagoAprobado: true, tieneExpediente: false },
      { id: 6, estado: 'RECHAZADA' },
      { id: 7, estado: 'APROBADA', tienePagoAprobado: true, tieneExpediente: true, codigoExpediente: 'EXP-001' }
    ]

    const resultado = clasificarCotizacionesAduaneras(data)
    expect(resultado.nuevas.map((c) => c.id)).toEqual([1])
    expect(resultado.subsanadas.map((c) => c.id)).toEqual([2, 3])
    expect(resultado.pendientesPago.map((c) => c.id)).toEqual([4])
    expect(resultado.listasApertura.map((c) => c.id)).toEqual([5])
    expect(resultado.historico.map((c) => c.id)).toEqual([6, 7])
  })

  it('filtra expedientes por fase operativa', () => {
    const expedientes = [
      { id: 1, canalSunat: 'PENDIENTE', fechaLevanteAutorizacion: null, estadoExpediente: 'EN_TRAMITE' },
      { id: 2, canalSunat: 'VERDE', fechaLevanteAutorizacion: null, estadoExpediente: 'EN_TRAMITE' },
      { id: 3, canalSunat: 'ROJO', fechaLevanteAutorizacion: '2026-11-11', estadoExpediente: 'LEVANTE_AUTORIZADO' },
      { id: 4, canalSunat: 'VERDE', fechaLevanteAutorizacion: '2026-11-11', estadoExpediente: 'CERRADO' }
    ]

    expect(filtrarExpedientesPorFase(expedientes, 'PENDIENTE_CANAL').map((e) => e.id)).toEqual([1])
    expect(filtrarExpedientesPorFase(expedientes, 'CONTROL_SUNAT').map((e) => e.id)).toEqual([2])
    expect(filtrarExpedientesPorFase(expedientes, 'LEVANTE_AUTORIZADO').map((e) => e.id)).toEqual([3])
    expect(filtrarExpedientesPorFase(expedientes, 'CERRADOS').map((e) => e.id)).toEqual([4])
  })

  it('determina acciones permitidas contextualmente', () => {
    // 1. Expediente en tránsito de importación sin arribar: no puede asignar canal aún
    const expTransitoSinArribo = { estadoExpediente: 'EN_TRAMITE', fechaLevanteAutorizacion: null, fechaArriboAduana: null }
    const acciones1 = obtenerAccionesPermitidasExpediente(expTransitoSinArribo)
    expect(acciones1.puedeActualizarTransito).toBe(true)
    expect(acciones1.puedeAsignarCanal).toBe(false)
    expect(acciones1.requiereArriboParaCanal).toBe(true)
    expect(acciones1.puedeLevante).toBe(true)
    expect(acciones1.puedeCerrar).toBe(false)

    // 2. Expediente de importación con arribo confirmado: habilitado para asignar canal SUNAT
    const expArribado = { estadoExpediente: 'EN_TRAMITE', fechaLevanteAutorizacion: null, fechaArriboAduana: '2026-11-11T12:00:00' }
    const accionesArribado = obtenerAccionesPermitidasExpediente(expArribado)
    expect(accionesArribado.puedeAsignarCanal).toBe(true)
    expect(accionesArribado.requiereArriboParaCanal).toBe(false)

    // 3. Expediente de exportación: puede asignar canal sin requerir arribo internacional
    const expExport = { estadoExpediente: 'EN_TRAMITE', regimenAduanero: 'EXPORTACION_DEFINITIVA', fechaLevanteAutorizacion: null }
    const accionesExport = obtenerAccionesPermitidasExpediente(expExport)
    expect(accionesExport.puedeAsignarCanal).toBe(true)

    // 4. Expediente con levante autorizado
    const expLevante = { estadoExpediente: 'LEVANTE_AUTORIZADO', fechaLevanteAutorizacion: '2026-11-11' }
    const acciones2 = obtenerAccionesPermitidasExpediente(expLevante)
    expect(acciones2.puedeActualizarTransito).toBe(false)
    expect(acciones2.puedeLevante).toBe(false)
    expect(acciones2.puedeCerrar).toBe(true)
  })
})
