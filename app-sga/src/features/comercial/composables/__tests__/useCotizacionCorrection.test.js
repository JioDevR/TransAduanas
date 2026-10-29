import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useCotizacionCorrection } from '../useCotizacionCorrection'

describe('useCotizacionCorrection', () => {
  const sampleCotizacion = {
    idCotizacion: 42,
    codigoCotizacion: 'COT-2026-0042',
    estado: 'OBSERVADA',
    motivoObservacion: 'Corregir flete marítimo y adjuntar packing list actualizado.',
    idCliente: 5,
    clienteRazonSocial: 'IMPORTACIONES PERU S.A.C.',
    clienteNumeroDocumento: '20556677881',
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    idPuertoEnvio: 12, // Shanghai
    idPuertoDestino: 1, // Callao Muelle Sur
    idTipoCarga: 2,
    idMercancia: 3,
    valorFob: 10000,
    montoFleteEstimado: 1200,
    montoSeguroEstimado: 85,
    montoHonorariosEstimado: 250,
    costoTransporteInterno: 150,
    servicioTransporte: true,
    tasaAdValorem: 6,
    tasaPercepcion: 3.5,
    validezDias: 15,
    observaciones: 'Cotización inicial enviada al cliente'
  }

  const samplePuertos = [
    { idPuerto: 1, codigoPuerto: 'PECAL-DPW', nombre: 'Callao Muelle Sur', pais: 'Perú', tipo: 'MARITIMO' },
    { idPuerto: 12, codigoPuerto: 'CNSHA', nombre: 'Puerto de Shanghái', pais: 'China', tipo: 'MARITIMO' },
    { idPuerto: 11, codigoPuerto: 'PEAQP', nombre: 'Aeropuerto Internacional Jorge Chávez', pais: 'Perú', tipo: 'AEREO' }
  ]

  it('inicializa el formulario correctamente a partir de la cotización observada', () => {
    const cotizacionRef = ref(sampleCotizacion)
    const puertosRef = ref(samplePuertos)
    const { form, motivoObservacion, valorCif } = useCotizacionCorrection({
      cotizacion: cotizacionRef,
      puertos: puertosRef
    })

    expect(motivoObservacion.value).toBe('Corregir flete marítimo y adjuntar packing list actualizado.')
    expect(form.value.codigoCotizacion).toBe('COT-2026-0042')
    expect(form.value.valorFob).toBe(10000)
    expect(form.value.montoFleteEstimado).toBe(1200)
    // CIF = 10000 + 1200 + 85 = 11285
    expect(valorCif.value).toBe(11285)
  })

  it('recalcula automáticamente CIF y tributos al modificar el flete o FOB', () => {
    const cotizacionRef = ref(sampleCotizacion)
    const puertosRef = ref(samplePuertos)
    const { form, valorCif, montoAdValorem, montoIgv } = useCotizacionCorrection({
      cotizacion: cotizacionRef,
      puertos: puertosRef
    })

    form.value.montoFleteEstimado = 2000
    // CIF = 10000 + 2000 + 85 = 12085
    expect(valorCif.value).toBe(12085)
    // Ad-Valorem 6% de 12085 = 725.10
    expect(montoAdValorem.value).toBe(725.10)
    // Base IGV = 12085 + 725.10 = 12810.10 * 0.18 = 2305.82
    expect(montoIgv.value).toBe(2305.82)
  })

  it('valida que el valor FOB sea mayor a 0 y arma el payload para reenviar a supervisión', () => {
    const cotizacionRef = ref(sampleCotizacion)
    const puertosRef = ref(samplePuertos)
    const { form, notaSubsanacion, buildSubmissionPayload, validateCorrection } = useCotizacionCorrection({
      cotizacion: cotizacionRef,
      puertos: puertosRef
    })

    form.value.valorFob = 0
    const invalidRes = validateCorrection()
    expect(invalidRes.valid).toBe(false)
    expect(invalidRes.error).toContain('FOB')

    form.value.valorFob = 15000
    notaSubsanacion.value = 'Se ajustó flete a $1,500 y se adjuntó nuevo packing list.'
    const validRes = validateCorrection()
    expect(validRes.valid).toBe(true)

    const payload = buildSubmissionPayload()
    expect(payload.idCotizacion).toBe(42)
    expect(payload.valorFob).toBe(15000)
    expect(payload.observaciones).toContain('Se ajustó flete')
  })
})
