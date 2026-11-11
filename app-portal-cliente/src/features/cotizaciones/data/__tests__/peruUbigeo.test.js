import { describe, it, expect } from 'vitest'
import {
  PERU_UBIGEO_DATA,
  getDepartamentosPeru,
  getProvinciasPorDepartamento
} from '../peruUbigeo'

describe('peruUbigeo', () => {
  it('contiene los 25 departamentos y regiones oficiales del Perú', () => {
    expect(PERU_UBIGEO_DATA).toHaveLength(25)
    const deps = getDepartamentosPeru()
    expect(deps).toContain('CALLAO')
    expect(deps).toContain('LIMA')
    expect(deps).toContain('AREQUIPA')
  })

  it('obtiene las provincias correctas para un departamento dado (case-insensitive)', () => {
    const provsCallao = getProvinciasPorDepartamento('CALLAO')
    expect(provsCallao).toEqual(['Callao'])

    const provsLima = getProvinciasPorDepartamento('lima')
    expect(provsLima).toContain('Lima (Metropolitana)')
    expect(provsLima).toContain('Barranca')
  })

  it('retorna array vacío cuando no se proporciona departamento o no existe', () => {
    expect(getProvinciasPorDepartamento(null)).toEqual([])
    expect(getProvinciasPorDepartamento('')).toEqual([])
    expect(getProvinciasPorDepartamento('INEXISTENTE')).toEqual([])
  })
})
