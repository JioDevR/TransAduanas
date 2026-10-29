import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import AppLayout from '@/core/layouts/AppLayout.vue'

import authRoutes from '@/features/auth/routes/auth.routes'
import adminRoutes from '@/features/admin/users/routes/admin.routes'
import gerenciaRoutes from '@/features/gerencia/routes/gerencia.routes'
import rolesRoutes from '@/core/router/roles.routes'

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
  if (to?.meta?.title) {
    document.title = to.meta.title
  } else {
    document.title = 'SGA - Sistema de Gestión Aduanera'
  }

  const isAuth = authStore.isAuthenticated
  const toPath = typeof to?.path === 'string' ? to.path : ''

  // Si ingresa por defecto a la raíz '/'
  if (toPath === '/') {
    if (!isAuth) {
      return next('/login')
    }
    return next(authStore.homePath)
  }

  // Si ya está autenticado e intenta acceder al login
  if (toPath === '/login' && isAuth) {
    return next(authStore.homePath)
  }

  // Si cualquier ruta requiere autenticación y no hay sesión activa
  const requiresAuth = to?.matched?.some((record) => record.meta?.requiresAuth)
  if (requiresAuth && !isAuth) {
    return next({ path: '/login', query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : {} })
  }

  // Validación de permisos por rol
  if (to?.meta?.roles && to.meta.roles.length > 0) {
    const userRole = authStore.userRole
    if (!to.meta.roles.includes(userRole)) {
      const targetHome = authStore.homePath
      if (toPath === targetHome || targetHome === '/login') {
        authStore.logout()
        return next('/login')
      }
      return next(targetHome)
    }
  }

  // Sincronizar activeModule con la ruta actual de manera segura
  if (!toPath) {
    return next()
  }

  const navStore = useNavigationStore()
  if (toPath.startsWith('/administracion-ti')) {
    if (navStore.activeModule !== 'administracion-ti') {
      navStore.setActiveModule('administracion-ti')
    }
    if (toPath.includes('dashboard')) {
      navStore.setAdministracionTiSubOption('dashboard')
    } else if (toPath.includes('usuarios-portal')) {
      navStore.setAdministracionTiSubOption('portal-users')
    } else if (toPath.includes('usuarios')) {
      navStore.setAdministracionTiSubOption('users')
    } else if (toPath.includes('logs')) {
      navStore.setAdministracionTiSubOption('logs')
    } else if (toPath.includes('acerca-de')) {
      navStore.setAdministracionTiSubOption('about')
    } else {
      navStore.setAdministracionTiSubOption('dashboard')
    }
  } else if (toPath.startsWith('/gerencia-general')) {
    if (navStore.activeModule !== 'gerencia-general') {
      navStore.setActiveModule('gerencia-general')
    }
    if (toPath.includes('sedes')) {
      navStore.setGerenciaGeneralSubOption('sedes')
    } else if (toPath.includes('puertos') || toPath.includes('catalogos')) {
      navStore.setGerenciaGeneralSubOption('puertos')
    } else if (toPath.includes('tipos-carga')) {
      navStore.setGerenciaGeneralSubOption('tipos-carga')
    } else if (toPath.includes('mercancias')) {
      navStore.setGerenciaGeneralSubOption('mercancias')
    } else if (toPath.includes('dashboard') || toPath.includes('resumen')) {
      navStore.setGerenciaGeneralSubOption('dashboard')
    } else if (toPath.includes('flota')) {
      navStore.setGerenciaGeneralSubOption('flota')
    } else if (toPath.includes('conductores')) {
      navStore.setGerenciaGeneralSubOption('conductores')
    } else {
      navStore.setGerenciaGeneralSubOption('dashboard')
    }
  } else if (toPath.startsWith('/recepcion-comercial')) {
    if (navStore.activeModule !== 'comercial') {
      navStore.setActiveModule('comercial')
    }
    if (to?.query?.tab) {
      navStore.setComercialSubOption(to.query.tab)
    }
  } else if (toPath.startsWith('/gestion-aduanera')) {
    if (navStore.activeModule !== 'gestion-aduanera') {
      navStore.setActiveModule('gestion-aduanera')
    }
    if (to?.query?.tab) {
      navStore.setGestionAduaneraSubOption(to.query.tab)
    } else if (toPath.includes('liquidacion')) {
      navStore.setGestionAduaneraSubOption('declaraciones')
    }
  } else if (toPath.startsWith('/agente-aduanero')) {
    if (navStore.activeModule !== 'agente-aduanero') {
      navStore.setActiveModule('agente-aduanero')
    }
    if (to?.query?.tab) {
      navStore.setAgenteAduaneroSubOption(to.query.tab)
    }
  } else if (toPath.startsWith('/transporte-flota')) {
    if (navStore.activeModule !== 'transporte-flota') {
      navStore.setActiveModule('transporte-flota')
    }
    if (to?.query?.tab) {
      navStore.setTransporteFlotaSubOption(to.query.tab)
    }
  } else if (toPath.startsWith('/legal-auditoria')) {
    if (navStore.activeModule !== 'legal-auditoria') {
      navStore.setActiveModule('legal-auditoria')
    }
    if (to?.query?.tab) {
      navStore.setLegalAuditoriaSubOption(to.query.tab)
    }
  } else if (toPath.startsWith('/finanzas')) {
    if (navStore.activeModule !== 'finanzas') {
      navStore.setActiveModule('finanzas')
    }
    if (to?.query?.tab) {
      navStore.setFinanzasSubOption(to.query.tab)
    }
  }

  next()
})

export default router
