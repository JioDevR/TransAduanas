import { describe, it, expect } from 'vitest'
import { filtrarCotizacionesParaSupervision } from '../supervisionCotizacionUtils'

describe('supervisionCotizacionUtils', () => {
  it('excluye solicitudes del portal sin asesor o con totalCotizado en cero', () => {
    const input = [
      { idCotizacion: 1, idUsuarioComercial: null, totalCotizado: 0, estado: 'PENDIENTE' },
      { idCotizacion: 2, idUsuarioComercial: 7, totalCotizado: 0, estado: 'PENDIENTE' },
      { idCotizacion: 3, idUsuarioComercial: null, totalCotizado: 1200, estado: 'PENDIENTE' },
      { idCotizacion: 4, idUsuarioComercial: 7, totalCotizado: 2450.5, estado: 'PENDIENTE' },
      { idCotizacion: 5, idUsuarioComercial: 3, totalCotizado: 980, estado: 'APROBADA' }
    ]
    const res = filtrarCotizacionesParaSupervision(input)
    expect(res.map((c) => c.idCotizacion)).toEqual([4, 5])
  })

  it('retorna arreglo vacío cuando recibe entradas nulas o no iterables', () => {
    expect(filtrarCotizacionesParaSupervision(null)).toEqual([])
    expect(filtrarCotizacionesParaSupervision(undefined)).toEqual([])
  })
})
