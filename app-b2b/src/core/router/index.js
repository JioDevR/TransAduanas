import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/core/stores/authStore'
import { resolveAuthRouteRedirect } from '@/core/router/authGuard'
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
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/features/dashboard/views/PortalDashboardView.vue'),
        meta: { title: 'Resumen General — Portal TransAduanas' }
      },
      {
        path: 'cotizaciones',
        name: 'Cotizaciones',
        component: () => import('@/features/cotizaciones/views/CotizacionesListView.vue'),
        meta: { title: 'Mis Cotizaciones — Portal TransAduanas' }
      },
      {
        path: 'cotizaciones/:id',
        name: 'CotizacionDetalle',
        component: () => import('@/features/cotizaciones/views/CotizacionDetalleView.vue'),
        meta: { title: 'Detalle de Cotización — Portal TransAduanas' }
      },
      {
        path: 'expedientes',
        name: 'Expedientes',
        component: () => import('@/features/expedientes/views/ExpedientesTrackingView.vue'),
        meta: { title: 'Expedientes & Tracking DAM — Portal TransAduanas' }
      },
      {
        path: 'expedientes/:id',
        name: 'ExpedienteTrackingDetalle',
        component: () => import('@/features/expedientes/views/ExpedienteTrackingDetailView.vue'),
        meta: { title: 'Tracking de Expediente — Portal TransAduanas' }
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
        path: 'pagos/subsanar/:id',
        name: 'SubsanarPago',
        component: () => import('@/features/pagos/views/SubsanarPagoView.vue'),
        meta: { title: 'Subsanar Abono Bancario — Portal TransAduanas' }
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

  if (to.meta?.title) {
    document.title = to.meta.title
  }

  const redirectTarget = resolveAuthRouteRedirect({
    path: to.path,
    requiresAuth: to.matched.some(record => record.meta?.requiresAuth),
    isAuthenticated: authStore.isAuthenticated,
    debeCambiarPassword: authStore.user?.debeCambiarPassword === true
  })

  if (redirectTarget) {
    return next(redirectTarget)
  }

  next()
})

export default router
