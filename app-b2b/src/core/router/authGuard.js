/**
 * Resuelve si el guard de navegación debe redirigir al usuario según autenticación y primer acceso.
 */
export function resolveAuthRouteRedirect({
  path,
  requiresAuth,
  isAuthenticated,
  debeCambiarPassword
}) {
  if (requiresAuth && !isAuthenticated) {
    return '/login'
  }

  if (!isAuthenticated) {
    return null
  }

  if (debeCambiarPassword) {
    return path !== '/cambiar-password-obligatorio' ? '/cambiar-password-obligatorio' : null
  }

  if (path === '/cambiar-password-obligatorio' || path === '/login') {
    return '/dashboard'
  }

  return null
}
