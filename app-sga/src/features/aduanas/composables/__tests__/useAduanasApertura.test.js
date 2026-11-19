import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useAduanasApertura } from '../useAduanasApertura'

const mockListas = [
  {
    idCotizacion: 10,
    codigoCotizacion: 'COT-0010',
    clienteRazonSocial: 'Importadora Andina SAC',
    clienteNumeroDocumento: '20111222333',
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    puertoEnvioNombre: 'Shanghai',
    puertoDestinoNombre: 'Callao',
    totalCotizado: 2500
  },
  {
    idCotizacion: 15,
    codigoCotizacion: 'COT-0015',
    clienteRazonSocial: 'Agroexportaciones Sur EIRL',
    clienteNumeroDocumento: '20444555666',
    regimenAduanero: 'EXPORTACION_DEFINITIVA',
    puertoEnvioNombre: 'Paita',
    puertoDestinoNombre: 'Rotterdam',
    totalCotizado: 4800
  }
]

describe('useAduanasApertura', () => {
  it('filtra por texto de búsqueda en cliente, código o puertos', () => {
    const { searchQuery, filteredApertura, hasActiveFilters } = useAduanasApertura({
      cotizacionesListas: ref(mockListas)
    })

    expect(filteredApertura.value).toHaveLength(2)
    expect(hasActiveFilters.value).toBe(false)

    searchQuery.value = 'rotterdam'
    expect(filteredApertura.value).toHaveLength(1)
    expect(filteredApertura.value[0].codigoCotizacion).toBe('COT-0015')
    expect(hasActiveFilters.value).toBe(true)
  })

  it('filtra por régimen aduanero y restablece filtros', () => {
    const { filterRegimen, sortBy, filteredApertura, resetFilters } = useAduanasApertura({
      cotizacionesListas: ref(mockListas)
    })

    filterRegimen.value = 'IMPORTACION_DEFINITIVA'
    expect(filteredApertura.value).toHaveLength(1)
    expect(filteredApertura.value[0].idCotizacion).toBe(10)

    sortBy.value = 'AMOUNT_ASC'
    resetFilters()
    expect(filterRegimen.value).toBe('ALL')
    expect(sortBy.value).toBe('RECENT')
    expect(filteredApertura.value).toHaveLength(2)
  })

  it('ordena por monto descendente y por código ascendente', () => {
    const { sortBy, filteredApertura } = useAduanasApertura({
      cotizacionesListas: ref(mockListas)
    })

    sortBy.value = 'AMOUNT_DESC'
    expect(filteredApertura.value.map((c) => c.idCotizacion)).toEqual([15, 10])

    sortBy.value = 'CODE_ASC'
    expect(filteredApertura.value.map((c) => c.codigoCotizacion)).toEqual(['COT-0010', 'COT-0015'])
  })
})
