import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import AppLayout from '@/core/layouts/AppLayout.vue'

import authRoutes from '@/features/auth/routes/auth.routes'
import adminRoutes from '@/features/admin/users/routes/admin.routes'
import gerenciaRoutes from '@/features/gerencia/routes/gerencia.routes'
import rolesRoutes from '@/features/roles-modules/routes/roles.routes'

const routes = [
  ...authRoutes,
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Root',
        redirect: () => {
          const authStore = useAuthStore()
          if (!authStore.isAuthenticated) {
            return '/login'
          }
          return authStore.homePath
        }
      },
      ...adminRoutes,
      ...gerenciaRoutes,
      ...rolesRoutes
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Guardia de Navegación Global
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // Actualizar título de documento
  if (to.meta?.title) {
    document.title = to.meta.title
  } else {
    document.title = 'SGA - Sistema de Gestión Aduanera'
  }

  const isAuth = authStore.isAuthenticated

  // Si ingresa por defecto a la raíz '/'
  if (to.path === '/') {
    if (!isAuth) {
      return next('/login')
    }
    return next(authStore.homePath)
  }

  // Si ya está autenticado e intenta acceder al login
  if (to.path === '/login' && isAuth) {
    return next(authStore.homePath)
  }

  // Si cualquier ruta requiere autenticación y no hay sesión activa
  const requiresAuth = to.matched.some((record) => record.meta?.requiresAuth)
  if (requiresAuth && !isAuth) {
    return next({ path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} })
  }

  // Validación de permisos por rol
  if (to.meta?.roles && to.meta.roles.length > 0) {
    const userRole = authStore.userRole
    if (!to.meta.roles.includes(userRole)) {
      const targetHome = authStore.homePath
      if (to.path === targetHome || targetHome === '/login') {
        authStore.logout()
        return next('/login')
      }
      return next(targetHome)
    }
  }

  // Sincronizar activeModule con la ruta actual
  const navStore = useNavigationStore()
  if (to.path.startsWith('/administracion-ti')) {
    navStore.setActiveModule('administracion-ti')
    if (to.path.includes('acerca-de')) {
      navStore.setAdministracionTiSubOption('about')
    } else if (to.path.includes('logs')) {
      navStore.setAdministracionTiSubOption('logs')
    } else {
      navStore.setAdministracionTiSubOption('users')
    }
  } else if (to.path.startsWith('/gerencia-general')) {
    navStore.setActiveModule('gerencia-general')
    if (to.path.includes('sedes')) {
      navStore.setGerenciaGeneralSubOption('sedes')
    } else if (to.path.includes('catalogos')) {
      navStore.setGerenciaGeneralSubOption('catalogos')
    } else if (to.path.includes('resumen')) {
      navStore.setGerenciaGeneralSubOption('overview')
    } else if (to.path.includes('flota')) {
      navStore.setGerenciaGeneralSubOption('flota')
    } else if (to.path.includes('conductores')) {
      navStore.setGerenciaGeneralSubOption('conductores')
    }
  } else if (to.path.startsWith('/recepcion-comercial')) {
    navStore.setActiveModule('comercial')
  } else if (to.path.startsWith('/gestion-aduanera')) {
    navStore.setActiveModule('gestion-aduanera')
    if (to.path.includes('liquidacion')) {
      navStore.setGestionAduaneraSubOption('declaraciones')
    }
  } else if (to.path.startsWith('/agente-aduanero')) {
    navStore.setActiveModule('agente-aduanero')
  } else if (to.path.startsWith('/transporte-flota')) {
    navStore.setActiveModule('transporte-flota')
  } else if (to.path.startsWith('/legal-auditoria')) {
    navStore.setActiveModule('legal-auditoria')
  } else if (to.path.startsWith('/finanzas')) {
    navStore.setActiveModule('finanzas')
  }

  next()
})

export default router
