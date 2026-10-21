import { ROLES } from '@/core/auth/roles'

export const gerenciaRoutes = [
  {
    path: '/gerencia-general',
    redirect: '/gerencia-general/sedes'
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
    path: '/gerencia-general/catalogos',
    name: 'GerenciaCatalogos',
    component: () => import('@/features/admin/catalogos/views/CatalogosManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.GERENTE_GENERAL, ROLES.ADMINISTRADOR_TI],
      title: 'Gerencia General - Catálogos Auxiliares - SGA'
    }
  },
  {
    path: '/gerencia-general/resumen',
    name: 'GerenciaResumen',
    component: () => import('@/features/admin/overview/views/AdminOverviewView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.GERENTE_GENERAL, ROLES.ADMINISTRADOR_TI],
      title: 'Gerencia General - Resumen Ejecutivo - SGA'
    }
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
