import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/core/stores/authStore'
import PortalLayout from '@/core/layouts/PortalLayout.vue'
import PortalLoginView from '@/features/auth/views/PortalLoginView.vue'
import CotizacionesListView from '@/features/cotizaciones/views/CotizacionesListView.vue'
import ExpedientesTrackingView from '@/features/expedientes/views/ExpedientesTrackingView.vue'
import CobrosPagosView from '@/features/pagos/views/CobrosPagosView.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: PortalLoginView,
    meta: { title: 'Iniciar Sesión — Portal TransAduanas', public: true }
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
        component: CotizacionesListView,
        meta: { title: 'Mis Cotizaciones — Portal TransAduanas' }
      },
      {
        path: 'expedientes',
        name: 'Expedientes',
        component: ExpedientesTrackingView,
        meta: { title: 'Expedientes & Tracking DAM — Portal TransAduanas' }
      },
      {
        path: 'pagos',
        name: 'Pagos',
        component: CobrosPagosView,
        meta: { title: 'Cobros y Pagos — Portal TransAduanas' }
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

  if (to.path === '/login' && isAuth) {
    return next('/cotizaciones')
  }

  if (to.matched.some(record => record.meta?.requiresAuth) && !isAuth) {
    return next('/login')
  }

  next()
})

export default router
