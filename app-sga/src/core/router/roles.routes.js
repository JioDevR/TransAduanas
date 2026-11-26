import { ROLES } from '@/core/auth/roles'

export const rolesRoutes = [
  {
    path: '/recepcion-comercial',
    name: 'RecepcionComercialModule',
    component: () => import('@/features/comercial/views/RecepcionComercialView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.AGENTE_COMERCIAL],
      title: 'Recepción Comercial - SGA'
    }
  },
  {
    path: '/gestion-aduanera',
    name: 'GestionAduaneraModule',
    component: () => import('@/features/aduanas/views/GestionAduaneraView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.SUPERVISOR_ADUANERO, ROLES.AGENTE_ADUANERO],
      title: 'Gestión Aduanera - SGA'
    }
  },
  {
    path: '/gestion-aduanera/apertura/:idCotizacion',
    name: 'AperturaExpedienteFormView',
    component: () => import('@/features/aduanas/views/AperturaExpedienteFormView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.SUPERVISOR_ADUANERO, ROLES.AGENTE_ADUANERO],
      title: 'Apertura de Expediente Aduanero - SGA'
    }
  },
  {
    path: '/agente-aduanero',
    name: 'AgenteAduaneroModule',
    component: () => import('@/features/agente-aduanero/views/AgenteAduaneroView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.SUPERVISOR_ADUANERO, ROLES.AGENTE_ADUANERO],
      title: 'Agente Aduanero - SGA'
    }
  },
  {
    path: '/finanzas',
    name: 'FinanzasModule',
    component: () => import('@/features/finanzas/views/FinanzasView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.CONTADOR],
      title: 'Finanzas - SGA'
    }
  }
]

export default rolesRoutes
