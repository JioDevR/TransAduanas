import { ROLES } from '@/core/auth/roles'

export const adminRoutes = [
  {
    path: '/administracion-ti',
    redirect: '/administracion-ti/usuarios'
  },
  {
    path: '/administracion-ti/usuarios',
    name: 'AdminUsers',
    component: () => import('../views/UserManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL],
      title: 'Administración TI - Usuarios - SGA'
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
  }
]

export default adminRoutes
