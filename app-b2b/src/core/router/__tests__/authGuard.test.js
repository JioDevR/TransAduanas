import { describe, it, expect } from 'vitest'
import { resolveAuthRouteRedirect } from '../authGuard'

describe('resolveAuthRouteRedirect', () => {
  it('redirige a /login cuando la ruta requiere auth y no hay sesión', () => {
    expect(resolveAuthRouteRedirect({
      path: '/dashboard',
      requiresAuth: true,
      isAuthenticated: false,
      debeCambiarPassword: false
    })).toBe('/login')
  })

  it('permite permanecer en /login sin sesión', () => {
    expect(resolveAuthRouteRedirect({
      path: '/login',
      requiresAuth: false,
      isAuthenticated: false,
      debeCambiarPassword: false
    })).toBeNull()
  })

  it('fuerza /cambiar-password-obligatorio cuando debeCambiarPassword es true', () => {
    expect(resolveAuthRouteRedirect({
      path: '/dashboard',
      requiresAuth: true,
      isAuthenticated: true,
      debeCambiarPassword: true
    })).toBe('/cambiar-password-obligatorio')

    expect(resolveAuthRouteRedirect({
      path: '/login',
      requiresAuth: false,
      isAuthenticated: true,
      debeCambiarPassword: true
    })).toBe('/cambiar-password-obligatorio')
  })

  it('evita bucle infinito cuando ya está en /cambiar-password-obligatorio con debeCambiarPassword true', () => {
    expect(resolveAuthRouteRedirect({
      path: '/cambiar-password-obligatorio',
      requiresAuth: true,
      isAuthenticated: true,
      debeCambiarPassword: true
    })).toBeNull()
  })

  it('redirige a /dashboard si ya regularizó clave e intenta entrar a /login o /cambiar-password-obligatorio', () => {
    expect(resolveAuthRouteRedirect({
      path: '/cambiar-password-obligatorio',
      requiresAuth: true,
      isAuthenticated: true,
      debeCambiarPassword: false
    })).toBe('/dashboard')

    expect(resolveAuthRouteRedirect({
      path: '/login',
      requiresAuth: false,
      isAuthenticated: true,
      debeCambiarPassword: false
    })).toBe('/dashboard')
  })

  it('permite navegar a rutas internas cuando la sesión está regularizada', () => {
    expect(resolveAuthRouteRedirect({
      path: '/cotizaciones',
      requiresAuth: true,
      isAuthenticated: true,
      debeCambiarPassword: false
    })).toBeNull()
  })
})
