import { ROLES } from '@/core/auth/roles'

export const adminRoutes = [
  {
    path: '/administracion-ti',
    redirect: '/administracion-ti/dashboard'
  },
  {
    path: '/administracion-ti/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/features/admin/dashboard/views/AdminDashboardView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL],
      title: 'Administración TI - Dashboard de Control - SGA'
    }
  },
  {
    path: '/administracion-ti/usuarios',
    name: 'AdminUsers',
    component: () => import('../views/UserManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL],
      title: 'Administración TI - Personal Interno - SGA'
    }
  },
  {
    path: '/administracion-ti/usuarios-portal',
    name: 'AdminPortalUsers',
    component: () => import('@/features/admin/portal-users/views/PortalUserManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL],
      title: 'Administración TI - Usuarios Portal Clientes B2B - SGA'
    }
  },

  {
    path: '/administracion-ti/logs',
    name: 'AdminLogs',
    component: () => import('@/features/admin/views/SystemLogsView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI],
      title: 'Bitácora y Consola de Logs TI - SGA'
    }
  },
  {
    path: '/administracion-ti/acerca-de',
    name: 'AdminAbout',
    component: () => import('@/features/admin/about/views/AboutSystemView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL],
      title: 'Acerca del Sistema - SGA'
    }
  }
]

export default adminRoutes
