import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/core/stores/authStore'
import PortalLayout from '@/core/layouts/PortalLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/features/auth/views/PortalLoginView.vue'),
    meta: { title: 'Iniciar Sesión — Portal TransAduanas', public: true }
  },
  {
    path: '/cambiar-password-obligatorio',
    name: 'CambioPasswordObligatorio',
    component: () => import('@/features/auth/views/PortalCambioPasswordObligatorioView.vue'),
    meta: { title: 'Actualización Obligatoria de Contraseña — Portal TransAduanas', requiresAuth: true }
  },
  {
    path: '/',
    component: PortalLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/cotizaciones'
      },
      {
        path: 'cotizaciones',
        name: 'Cotizaciones',
        component: () => import('@/features/cotizaciones/views/CotizacionesListView.vue'),
        meta: { title: 'Mis Cotizaciones — Portal TransAduanas' }
      },
      {
        path: 'expedientes',
        name: 'Expedientes',
        component: () => import('@/features/expedientes/views/ExpedientesTrackingView.vue'),
        meta: { title: 'Expedientes & Tracking DAM — Portal TransAduanas' }
      },
      {
        path: 'pagos',
        name: 'Pagos',
        component: () => import('@/features/pagos/views/CobrosPagosView.vue'),
        meta: { title: 'Cobros y Pagos — Portal TransAduanas' }
      },
      {
        path: 'pagos/registrar',
        name: 'RegistrarPago',
        component: () => import('@/features/pagos/views/RegistrarPagoView.vue'),
        meta: { title: 'Registrar Pago / Abono Bancario — Portal TransAduanas' }
      },
      {
        path: 'notificaciones',
        name: 'Notificaciones',
        component: () => import('@/features/notificaciones/views/PortalNotificacionesView.vue'),
        meta: { title: 'Notificaciones & Avisos — Portal TransAduanas' }
      }
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

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const isAuth = authStore.isAuthenticated

  if (to.meta?.title) {
    document.title = to.meta.title
  }

  // Si no está autenticado y la ruta requiere autenticación
  if (to.matched.some(record => record.meta?.requiresAuth) && !isAuth) {
    return next('/login')
  }

  // Si está autenticado
  if (isAuth) {
    const debeCambiar = authStore.user?.debeCambiarPassword === true

    // Si debe cambiar contraseña por ser primer acceso
    if (debeCambiar) {
      if (to.path !== '/cambiar-password-obligatorio') {
        return next('/cambiar-password-obligatorio')
      }
      return next()
    }

    // Si ya cambió la contraseña e intenta ir a pantalla de cambio obligatorio o login
    if (!debeCambiar && (to.path === '/cambiar-password-obligatorio' || to.path === '/login')) {
      return next('/cotizaciones')
    }
  }

  next()
})

export default router
