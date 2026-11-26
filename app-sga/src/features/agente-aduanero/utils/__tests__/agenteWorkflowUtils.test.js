import { describe, it, expect } from 'vitest'
import {
  isExportacion,
  isExpedienteCerrado,
  hasCanalAsignado,
  hasZarpeConfirmado,
  hasArriboConfirmado,
  isDiligenciaCompletada,
  isLevanteAutorizado,
  isEmbarqueConfirmado,
  isRecepcionExteriorConfirmada,
  isRetiroDirectoRegistrado,
  belongsToAgentePhase,
  getAccionesPermitidasAgente,
  extractTerminalBitacora
} from '../agenteWorkflowUtils'

describe('agenteWorkflowUtils', () => {
  it('detecta correctamente régimen de exportación e importación desde regimenAduanero', () => {
    expect(isExportacion({ regimenAduanero: 'EXPORTACION_DEFINITIVA' })).toBe(true)
    expect(isExportacion({ regimenAduanero: 'IMPORTACION_DEFINITIVA' })).toBe(false)
    expect(isExportacion({})).toBe(false)
  })

  it('no considera CANAL PENDIENTE como canal asignado', () => {
    expect(hasCanalAsignado({ canalSunat: null })).toBe(false)
    expect(hasCanalAsignado({ canalSunat: 'PENDIENTE' })).toBe(false)
    expect(hasCanalAsignado({ canalSunat: 'VERDE' })).toBe(true)
    expect(hasCanalAsignado({ canalSunat: 'NARANJA' })).toBe(true)
    expect(hasCanalAsignado({ canalSunat: 'ROJO' })).toBe(true)
  })

  it('detecta zarpe, arribo, diligencia con detalleInspeccion y levante', () => {
    expect(hasZarpeConfirmado({ estadoExpediente: 'ASIGNADO', canalSunat: 'PENDIENTE' })).toBe(false)
    expect(hasZarpeConfirmado({ estadoExpediente: 'EN_TRAMITE' })).toBe(true)
    expect(hasArriboConfirmado({ fechaArriboAduana: '2026-11-20T10:00:00' })).toBe(true)
    expect(isDiligenciaCompletada({ canalSunat: 'VERDE' })).toBe(true)
    expect(isDiligenciaCompletada({ canalSunat: 'ROJO', detalleInspeccion: 'Aforo conforme' })).toBe(true)
    expect(isDiligenciaCompletada({ canalSunat: 'ROJO' })).toBe(false)
    expect(isLevanteAutorizado({ estadoExpediente: 'LEVANTE_AUTORIZADO' })).toBe(true)
    expect(isExpedienteCerrado({ estadoExpediente: 'CERRADO' })).toBe(true)
  })

  it('detecta [EMBARQUE CONFIRMADO], recepción exterior y retiro directo en observaciones', () => {
    const expEmbarcado = {
      observaciones: '[EMBARQUE CONFIRMADO] Nave/Viaje: MSC CAPELLA | Zarpe ETD: 2026-11-20'
    }
    expect(isEmbarqueConfirmado(expEmbarcado)).toBe(true)
    expect(isRecepcionExteriorConfirmada(expEmbarcado)).toBe(false)

    const expEntregado = {
      observaciones: `${expEmbarcado.observaciones}\n[RECEPCIÓN DESTINO EXTERIOR] Consignatario: ACME`
    }
    expect(isRecepcionExteriorConfirmada(expEntregado)).toBe(true)

    expect(isRetiroDirectoRegistrado({ observaciones: '[RETIRO DIRECTO CLIENTE] Terminal: DPW' })).toBe(true)
  })

  it('clasifica correctamente las fases sin fugar cerrados ni levantes a inspeccion/levante', () => {
    const expAsignado = { estadoExpediente: 'ASIGNADO', canalSunat: 'PENDIENTE' }
    const expAforo = { estadoExpediente: 'INSPECCION', canalSunat: 'ROJO' }
    const expLevanteRojo = {
      estadoExpediente: 'LEVANTE_AUTORIZADO',
      canalSunat: 'ROJO',
      fechaLevanteAutorizacion: '2026-11-20T12:00:00'
    }
    const expCerrado = {
      estadoExpediente: 'CERRADO',
      canalSunat: 'ROJO',
      fechaLevanteAutorizacion: '2026-11-20T12:00:00'
    }

    expect(belongsToAgentePhase(expAsignado, 'transito')).toBe(true)
    expect(belongsToAgentePhase(expAforo, 'inspeccion')).toBe(true)
    expect(belongsToAgentePhase(expLevanteRojo, 'inspeccion')).toBe(false)
    expect(belongsToAgentePhase(expLevanteRojo, 'levante')).toBe(true)
    expect(belongsToAgentePhase(expCerrado, 'inspeccion')).toBe(false)
    expect(belongsToAgentePhase(expCerrado, 'levante')).toBe(false)
    expect(belongsToAgentePhase(expCerrado, 'todos')).toBe(true)
  })

  it('habilita Entrega Exterior en Exportación tras [EMBARQUE CONFIRMADO]', () => {
    const expExportEmbarcado = {
      regimenAduanero: 'EXPORTACION_DEFINITIVA',
      estadoExpediente: 'LEVANTE_AUTORIZADO',
      canalSunat: 'VERDE',
      observaciones: '[EMBARQUE CONFIRMADO] Nave/Viaje: MAERSK 101'
    }
    const acciones = getAccionesPermitidasAgente(expExportEmbarcado)
    expect(acciones.puedeEmbarque).toBe(false)
    expect(acciones.puedeRecepcionExterior).toBe(true)
  })

  it('bloquea Retiro Directo duplicado en Importación cuando ya fue registrado', () => {
    const expConRetiro = {
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      estadoExpediente: 'LEVANTE_AUTORIZADO',
      servicioTransporte: false,
      observaciones: '[RETIRO DIRECTO CLIENTE] Terminal: DPW'
    }
    const acciones = getAccionesPermitidasAgente(expConRetiro)
    expect(acciones.puedeRetiroDirecto).toBe(false)
  })

  it('extrae terminal desde la bitácora de observaciones', () => {
    const obsImport = '[ARRIBO A PUERTO DE DESTINO] Arribo registrado el 20/11/2026 10:00 en terminal: DP World Callao. Detalle: OK'
    expect(extractTerminalBitacora(obsImport)).toBe('DP World Callao')

    const obsExport = '[INGRESO A ZONA PRIMARIA / ALMACÉN ORIGEN] Terminal: APM Terminals Callao | Ticket/VGM: TKT-10 | Fecha: 2026-11-20'
    expect(extractTerminalBitacora(obsExport)).toBe('APM Terminals Callao')
  })

  it('habilita acciones diferenciadas para canales ROJO, NARANJA, VERDE y estados manuales', () => {
    const rojo = getAccionesPermitidasAgente({
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      canalSunat: 'ROJO',
      estadoExpediente: 'INSPECCION',
      cuadrillaSolicitada: false
    })
    expect(rojo.puedeCuadrillaCitaRojo).toBe(true)
    expect(rojo.puedeFinalizarRojo).toBe(true)
    expect(rojo.puedeEnviarSunatNaranja).toBe(false)

    const naranja = getAccionesPermitidasAgente({
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      canalSunat: 'NARANJA',
      estadoExpediente: 'INSPECCION',
      envioSunatSimuladoOk: false
    })
    expect(naranja.puedeEnviarSunatNaranja).toBe(true)

    const verde = getAccionesPermitidasAgente({
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      canalSunat: 'VERDE',
      estadoExpediente: 'LEVANTE_AUTORIZADO',
      entregadoAlmacenVerde: false
    })
    expect(verde.puedeEntregadoAlmacenVerde).toBe(true)
    expect(verde.puedeInformeConformidad).toBe(true)
    expect(verde.puedeFinalizarExpediente).toBe(true)

    const exportPendiente = getAccionesPermitidasAgente({
      regimenAduanero: 'EXPORTACION_DEFINITIVA',
      estadoExpediente: 'ASIGNADO'
    })
    expect(exportPendiente.puedeMarcarRevisionAduana).toBe(true)
    expect(exportPendiente.puedeMarcarEsperaTransito).toBe(true)
  })
})
