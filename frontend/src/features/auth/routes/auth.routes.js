export const authRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: {
      requiresAuth: false,
      title: 'Iniciar Sesión - SGA'
    }
  }
]

export default authRoutes
