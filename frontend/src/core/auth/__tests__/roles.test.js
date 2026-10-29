import { describe, it, expect } from 'vitest'
import { ROLES, ROLE_HOME_PATH, getHomeRouteForRole } from '../roles'

describe('roles.js - getHomeRouteForRole', () => {
  it('retorna la ruta correcta para cada rol configurado', () => {
    Object.entries(ROLE_HOME_PATH).forEach(([role, expectedPath]) => {
      expect(getHomeRouteForRole(role)).toBe(expectedPath)
    })
  })

  it('retorna /login para roles desconocidos o undefined', () => {
    expect(getHomeRouteForRole(undefined)).toBe('/login')
    expect(getHomeRouteForRole(null)).toBe('/login')
    expect(getHomeRouteForRole('ROL_INEXISTENTE')).toBe('/login')
  })
})
