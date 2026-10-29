export const landingRoutes = [
  {
    path: '/',
    name: 'Landing',
    component: () => import('../views/LandingView.vue'),
    meta: {
      requiresAuth: false,
      title: 'TransAduanas - Sistema de Gestión Aduanera'
    }
  }
]

export default landingRoutes