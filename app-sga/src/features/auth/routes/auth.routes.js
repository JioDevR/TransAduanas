export const authRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: {
      requiresAuth: false,
      title: 'TransAduanas - SGA | Iniciar Sesión'
    }
  }
]

export default authRoutes
