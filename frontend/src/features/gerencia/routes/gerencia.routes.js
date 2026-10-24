import { ROLES } from '@/core/auth/roles'

export const gerenciaRoutes = [
  {
    path: '/gerencia-general',
    redirect: '/gerencia-general/dashboard'
  },
  {
    path: '/gerencia-general/dashboard',
    name: 'GerenciaDashboard',
    component: () => import('@/features/gerencia/views/GerenciaDashboardView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.GERENTE_GENERAL, ROLES.ADMINISTRADOR_TI],
      title: 'Gerencia General - Dashboard Ejecutivo - SGA'
    }
  },
  {
    path: '/gerencia-general/sedes',
    name: 'GerenciaSedes',
    component: () => import('@/features/admin/sedes/views/SedesManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.GERENTE_GENERAL, ROLES.ADMINISTRADOR_TI],
      title: 'Gerencia General - Sedes - SGA'
    }
  },
  {
    path: '/gerencia-general/puertos',
    name: 'GerenciaPuertos',
    component: () => import('@/features/admin/catalogos/views/PuertosManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.GERENTE_GENERAL, ROLES.ADMINISTRADOR_TI],
      title: 'Gerencia General - Puertos y Terminales - SGA'
    }
  },
  {
    path: '/gerencia-general/tipos-carga',
    name: 'GerenciaTiposCarga',
    component: () => import('@/features/admin/catalogos/views/TiposCargaManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.GERENTE_GENERAL, ROLES.ADMINISTRADOR_TI],
      title: 'Gerencia General - Tipos de Carga - SGA'
    }
  },
  {
    path: '/gerencia-general/mercancias',
    name: 'GerenciaMercancias',
    component: () => import('@/features/admin/catalogos/views/MercanciasManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.GERENTE_GENERAL, ROLES.ADMINISTRADOR_TI],
      title: 'Gerencia General - Mercancías y Partidas - SGA'
    }
  },
  {
    path: '/gerencia-general/catalogos',
    redirect: '/gerencia-general/puertos'
  },
  {
    path: '/gerencia-general/resumen',
    redirect: '/gerencia-general/dashboard'
  },
  {
    path: '/gerencia-general/flota',
    name: 'GerenciaFlota',
    component: () => import('@/features/gerencia/views/GerenciaFlotaView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.GERENTE_GENERAL, ROLES.ADMINISTRADOR_TI],
      title: 'Gerencia General - Flota - SGA'
    }
  },
  {
    path: '/gerencia-general/conductores',
    name: 'GerenciaConductores',
    component: () => import('@/features/gerencia/views/GerenciaConductoresView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.GERENTE_GENERAL, ROLES.ADMINISTRADOR_TI],
      title: 'Gerencia General - Conductores - SGA'
    }
  }
]

export default gerenciaRoutes
