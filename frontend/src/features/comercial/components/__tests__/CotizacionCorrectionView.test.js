import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import CotizacionCorrectionView from '../CotizacionCorrectionView.vue'
import { useCotizacionCorrection } from '../../composables/useCotizacionCorrection'

describe('CotizacionCorrectionView Component & Flow', () => {
  it('exporta un componente Vue 3 válido con props y emits esperados', () => {
    expect(CotizacionCorrectionView).toBeDefined()
    expect(typeof CotizacionCorrectionView).toBe('object')
    // Verificar que el componente esté definido como un objeto de componente Vue
    expect(CotizacionCorrectionView.props || CotizacionCorrectionView.__name).toBeDefined()
  })

  it('procesa el banner de observación y precarga los datos de cotización observada', () => {
    const mockCotizacion = {
      idCotizacion: 101,
      codigoCotizacion: 'COT-2026-0101',
      estado: 'OBSERVADA',
      motivoObservacion: 'El flete marítimo no incluye THC en destino. Subsanar documento.',
      idCliente: 8,
      clienteRazonSocial: 'COMERCIAL ANDINA S.A.C.',
      clienteNumeroDocumento: '20100456789',
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      idPuertoEnvio: 12,
      idPuertoDestino: 1,
      valorFob: 25000,
      montoFleteEstimado: 3200,
      montoSeguroEstimado: 180,
      montoHonorariosEstimado: 450,
      tasaAdValorem: 6,
      tasaPercepcion: 3.5,
      observaciones: 'Propuesta preliminar'
    }

    const {
      form,
      motivoObservacion,
      valorCif,
      montoAdValorem,
      montoIgv,
      montoPercepcion,
      validateCorrection,
      buildSubmissionPayload
    } = useCotizacionCorrection({
      cotizacion: ref(mockCotizacion),
      puertos: ref([
        { idPuerto: 1, codigoPuerto: 'PECAL-DPW', nombre: 'Callao Muelle Sur', pais: 'Perú', tipo: 'MARITIMO' },
        { idPuerto: 12, codigoPuerto: 'CNSHA', nombre: 'Puerto de Shanghái', pais: 'China', tipo: 'MARITIMO' }
      ])
    })

    // 1. Verificación del motivo de observación
    expect(motivoObservacion.value).toBe('El flete marítimo no incluye THC en destino. Subsanar documento.')

    // 2. Verificación de cálculo CIF (25000 + 3200 + 180 = 28380)
    expect(valorCif.value).toBe(28380)

    // 3. Verificación de Ad-Valórem 6% (28380 * 0.06 = 1702.80)
    expect(montoAdValorem.value).toBe(1702.80)

    // 4. Verificación de IGV/IPM 18% ((28380 + 1702.80) * 0.18 = 5414.90)
    expect(montoIgv.value).toBe(5414.90)

    // 5. Verificación de Percepción 3.5% ((28380 + 1702.80 + 5414.90) * 0.035 = 1242.42)
    expect(montoPercepcion.value).toBe(1242.42)

    // 6. Validación de campos obligatorios
    expect(validateCorrection().valid).toBe(true)

    // 7. Generación de payload de subsanación
    const payload = buildSubmissionPayload()
    expect(payload.idCotizacion).toBe(101)
    expect(payload.codigoCotizacion).toBe('COT-2026-0101')
  })

  it('bloquea la subsanación si el valor FOB es cero o negativo', () => {
    const invalidCotizacion = {
      idCotizacion: 102,
      codigoCotizacion: 'COT-2026-0102',
      estado: 'OBSERVADA',
      motivoObservacion: 'Corregir importes',
      idCliente: 3,
      valorFob: 0,
      montoFleteEstimado: 500,
      montoSeguroEstimado: 50
    }

    const { validateCorrection } = useCotizacionCorrection({
      cotizacion: ref(invalidCotizacion)
    })

    const result = validateCorrection()
    expect(result.valid).toBe(false)
    expect(result.error).toContain('FOB')
  })

  it('incorpora la nota de subsanación comercial dentro de las observaciones al enviar', () => {
    const mockCotizacion = {
      idCotizacion: 103,
      codigoCotizacion: 'COT-2026-0103',
      estado: 'OBSERVADA',
      motivoObservacion: 'Ajustar seguro de transporte.',
      idCliente: 4,
      valorFob: 10000,
      montoFleteEstimado: 1000,
      montoSeguroEstimado: 100,
      observaciones: 'Nota original del vendedor'
    }

    const { notaSubsanacion, buildSubmissionPayload } = useCotizacionCorrection({
      cotizacion: ref(mockCotizacion)
    })

    notaSubsanacion.value = 'Se ajustó seguro conforme a póliza marítima N° 98765.'
    const payload = buildSubmissionPayload()

    expect(payload.observaciones).toContain('Nota original del vendedor')
    expect(payload.observaciones).toContain('[Subsanación Comercial]: Se ajustó seguro conforme a póliza marítima N° 98765.')
  })

  it('exhonera de tributos si la cotización se corrige a régimen de Exportación Definitiva', () => {
    const mockCotizacion = {
      idCotizacion: 104,
      codigoCotizacion: 'COT-2026-0104',
      estado: 'OBSERVADA',
      motivoObservacion: 'El cliente solicita exportación en vez de importación',
      idCliente: 2,
      regimenAduanero: 'IMPORTACION_DEFINITIVA',
      valorFob: 50000,
      montoFleteEstimado: 2500,
      montoSeguroEstimado: 350
    }

    const { form, montoAdValorem, montoIgv, montoPercepcion, recalcularLiquidacion } = useCotizacionCorrection({
      cotizacion: ref(mockCotizacion)
    })

    // Cambiar régimen a exportación
    form.value.regimenAduanero = 'EXPORTACION_DEFINITIVA'
    recalcularLiquidacion()

    expect(montoAdValorem.value).toBe(0)
    expect(montoIgv.value).toBe(0)
    expect(montoPercepcion.value).toBe(0)
    expect(form.value.montoTributosEstimado).toBe(0)
  })
})
