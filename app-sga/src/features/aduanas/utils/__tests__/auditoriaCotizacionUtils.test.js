import { describe, it, expect } from 'vitest'
import {
  normalizeAuditoriaData,
  calcularLiquidacionAduanera,
  evaluarChecklistAuditoria
} from '../auditoriaCotizacionUtils'

describe('auditoriaCotizacionUtils', () => {
  it('normaliza una cotización revisable en modalidad aérea', () => {
    const res = normalizeAuditoriaData(
      {
        idCotizacion: 42,
        codigoCotizacion: 'COT-2026-042',
        estado: 'PENDIENTE',
        modalidad: 'AEREO',
        clienteRazonSocial: 'Importaciones Andinas SAC'
      },
      null
    )

    expect(res.isExpedienteView).toBe(false)
    expect(res.isAirModal).toBe(true)
    expect(res.entidadTipo).toBe('COTIZACION')
    expect(res.entidadId).toBe(42)
    expect(res.codigoPrincipal).toBe('COT-2026-042')
    expect(res.isAuditable).toBe(true)
    expect(res.isObservable).toBe(true)
    expect(res.canAperturar).toBe(false)
  })

  it('normaliza un expediente y deshabilita acciones de revisión de cotización', () => {
    const res = normalizeAuditoriaData(null, {
      idExpediente: 15,
      codigoExpediente: 'EXP-2026-015',
      estadoExpediente: 'CON_CANAL',
      puertoEnvioTipo: 'MARITIMO'
    })

    expect(res.isExpedienteView).toBe(true)
    expect(res.isAirModal).toBe(false)
    expect(res.entidadTipo).toBe('EXPEDIENTE')
    expect(res.entidadId).toBe(15)
    expect(res.codigoPrincipal).toBe('EXP-2026-015')
    expect(res.isAuditable).toBe(false)
    expect(res.canAperturar).toBe(false)
  })

  it('infiere modalidad aérea por aeropuerto/tipo de carga si el DTO no trae modalidad explícita', () => {
    const res = normalizeAuditoriaData(
      {
        idCotizacion: 5,
        codigoCotizacion: 'COT-2026-0005',
        estado: 'APROBADA',
        puertoEnvioNombre: 'Aeropuerto Internacional de Viracopos-Campinas',
        puertoDestinoNombre: 'Aeropuerto Internacional Jorge Chávez',
        tipoCargaNombre: 'Contenedor Aéreo ULD (AKE / LD3 / DQF)'
      },
      null
    )
    expect(res.isAirModal).toBe(true)
  })

  it('calcula CIF, Ad-Valórem, IGV, Percepción y Total Tributos correctamente', () => {
    const liq = calcularLiquidacionAduanera({
      montoFobEstimado: 10000,
      montoFleteEstimado: 1500,
      montoSeguroEstimado: 500,
      tasaAdValorem: 6,
      tasaPercepcion: 3.5
    })

    // CIF = 12,000
    expect(liq.valorCif).toBe(12000)
    // AdValorem = 6% de 12,000 = 720
    expect(liq.montoAdValorem).toBe(720)
    // IGV = 18% de (12,000 + 720) = 2,289.60
    expect(liq.montoIgv).toBeCloseTo(2289.6, 2)
    // Percepcion = 3.5% de (12,000 + 720 + 2,289.60) = 525.336
    expect(liq.montoPercepcion).toBeCloseTo(525.336, 3)
    expect(liq.totalTributos).toBeCloseTo(720 + 2289.6 + 525.336, 3)
  })

  it('respeta montos precalculados cuando existen en el DTO', () => {
    const liq = calcularLiquidacionAduanera({
      valorFob: 5000,
      fleteInternacional: 400,
      seguroEstimado: 100,
      valorCif: 5600,
      montoAdValorem: 300,
      montoIgv: 1062,
      montoPercepcion: 0
    })

    expect(liq.valorCif).toBe(5600)
    expect(liq.montoAdValorem).toBe(300)
    expect(liq.montoIgv).toBe(1062)
    expect(liq.totalTributos).toBe(1362)
  })

  it('evalúa dinámicamente los criterios del checklist según los datos y documentos', () => {
    const completo = evaluarChecklistAuditoria(
      {
        clienteNumeroDocumento: '20554433221',
        regimenAduanero: 'IMPORTACION_DEFINITIVA',
        montoFobEstimado: 2500,
        mercanciaCodigo: '8471.30.00.00',
        tipoCargaNombre: 'Carga Contenedorizada'
      },
      3
    )
    expect(completo.every((item) => item.valid)).toBe(true)
    expect(completo.find((i) => i.id === 'docs')?.detail).toBe('3 archivo(s)')

    const incompleto = evaluarChecklistAuditoria(
      {
        clienteNumeroDocumento: '',
        regimenAduanero: 'IMPORTACION_DEFINITIVA',
        montoFobEstimado: 0,
        mercanciaCodigo: '',
        tipoCargaNombre: ''
      },
      0
    )
    expect(incompleto.find((i) => i.id === 'ruc')?.valid).toBe(false)
    expect(incompleto.find((i) => i.id === 'regimen')?.valid).toBe(true)
    expect(incompleto.find((i) => i.id === 'cif')?.valid).toBe(false)
    expect(incompleto.find((i) => i.id === 'nandina')?.valid).toBe(false)
    expect(incompleto.find((i) => i.id === 'carga')?.valid).toBe(false)
    expect(incompleto.find((i) => i.id === 'docs')?.valid).toBe(false)
  })
})
