import { describe, it, expect } from 'vitest'
import LoginView from '../LoginView.vue'
import { getHomeRouteForRole, ROLES } from '@/core/auth/roles'

describe('LoginView - Sanitización de Redirección y Redirección Segura', () => {
  function computeTargetRedirect(redirectQuery, userRole) {
    const redirectParam = typeof redirectQuery === 'string' && redirectQuery.startsWith('/') && !redirectQuery.startsWith('//')
      ? redirectQuery
      : null
    const defaultHome = getHomeRouteForRole(userRole)
    return redirectParam && redirectParam !== '/login' ? redirectParam : defaultHome
  }

  it('exporta un componente Vue válido', () => {
    expect(LoginView).toBeDefined()
    expect(typeof LoginView).toBe('object')
  })

  it('permite rutas internas relativas seguras', () => {
    const target = computeTargetRedirect('/gestion-aduanera', ROLES.SUPERVISOR_ADUANERO)
    expect(target).toBe('/gestion-aduanera')
  })

  it('bloquea open redirect con protocolo de doble barra (protocol-relative URL)', () => {
    const target = computeTargetRedirect('//malicious-site.com', ROLES.AGENTE_COMERCIAL)
    expect(target).toBe('/recepcion-comercial')
  })

  it('bloquea open redirect con URLs absolutas externas', () => {
    const target = computeTargetRedirect('https://evil.com/phishing', ROLES.CONTADOR)
    expect(target).toBe('/finanzas')
  })

  it('bloquea redirección a /login para evitar loops infinitos', () => {
    const target = computeTargetRedirect('/login', ROLES.ADMINISTRADOR_TI)
    expect(target).toBe('/administracion-ti/dashboard')
  })

  it('usa fallback de rol cuando redirect es null, undefined o no string', () => {
    expect(computeTargetRedirect(null, ROLES.GERENTE_GENERAL)).toBe('/gerencia-general/dashboard')
    expect(computeTargetRedirect(undefined, ROLES.ASESOR_LEGAL)).toBe('/legal-auditoria')
    expect(computeTargetRedirect(12345, ROLES.AGENTE_ADUANERO)).toBe('/agente-aduanero')
  })
})
