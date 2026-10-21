import { ROLES } from '@/core/auth/roles'

export const rolesRoutes = [
  {
    path: '/recepcion-comercial',
    name: 'RecepcionComercialModule',
    component: () => import('../views/RecepcionComercialView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.AGENTE_COMERCIAL],
      title: 'Recepción Comercial - SGA'
    }
  },
  {
    path: '/gestion-aduanera',
    name: 'GestionAduaneraModule',
    component: () => import('../views/GestionAduaneraView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.SUPERVISOR_ADUANERO],
      title: 'Gestión Aduanera - SGA'
    }
  },
  {
    path: '/gestion-aduanera/liquidacion',
    name: 'LiquidacionAduaneraModule',
    component: () => import('../views/LiquidacionAduaneraView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.SUPERVISOR_ADUANERO],
      title: 'Liquidación Aduanera - SGA'
    }
  },
  {
    path: '/agente-aduanero',
    name: 'AgenteAduaneroModule',
    component: () => import('../views/AgenteAduaneroView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.SUPERVISOR_ADUANERO, ROLES.AGENTE_ADUANERO],
      title: 'Agente Aduanero - SGA'
    }
  },
  {
    path: '/transporte-flota',
    name: 'TransporteFlotaModule',
    component: () => import('../views/TransporteFlotaView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.SUPERVISOR_TRANSPORTE],
      title: 'Transporte y Flota - SGA'
    }
  },
  {
    path: '/finanzas',
    name: 'FinanzasModule',
    component: () => import('../views/FinanzasView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.CONTADOR],
      title: 'Finanzas - SGA'
    }
  },
  {
    path: '/legal-auditoria',
    name: 'LegalAuditoriaModule',
    component: () => import('../views/LegalAuditoriaView.vue'),
    meta: {
      requiresAuth: true,
      roles: [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL, ROLES.ASESOR_LEGAL],
      title: 'Legal y Auditoría - SGA'
    }
  }
]

export default rolesRoutes
