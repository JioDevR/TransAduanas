import { defineStore } from 'pinia'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { ROLE_MODULE_MAP } from '@/core/auth/roles'

export const useNavigationStore = defineStore('navigation', {
  state: () => ({
    // Módulo activo: 'administracion-ti' | 'gerencia-general' | 'comercial' | 'gestion-aduanera' | 'agente-aduanero' | 'transporte-flota' | 'finanzas' | 'legal-auditoria'
    activeModule: 'administracion-ti',
    // Sub-opciones en Administración TI: 'users' | 'about' | 'logs'
    administracionTiSubOption: 'users',
    // Sub-opciones en Gerencia General: 'overview' | 'sedes' | 'catalogos' | 'flota' | 'conductores'
    gerenciaGeneralSubOption: 'sedes',
    // Sub-opciones en Recepción Comercial: 'dashboard' | 'cotizaciones' | 'clientes' | 'ordenes'
    comercialSubOption: 'dashboard',
    // Sub-opciones en Gestión Aduanera: 'cotizaciones' | 'expedientes' | 'declaraciones' | 'clasificacion' | 'tributos'
    gestionAduaneraSubOption: 'cotizaciones',
    // Sub-opciones en Agente Aduanero: 'almacen' | 'expedientes' | 'despacho'
    agenteAduaneroSubOption: 'almacen',
    // Sub-opciones en Transporte y Flota: 'despachos' | 'unidades' | 'conductores'
    transporteFlotaSubOption: 'despachos',
    // Sub-opciones en Legal y Auditoría: 'expedientes' | 'auditorias'
    legalAuditoriaSubOption: 'expedientes',
    // Sub-opciones en Finanzas: 'liquidacion' | 'facturacion' | 'anticipos'
    finanzasSubOption: 'liquidacion',
    // Disparadores de acciones modales directas
    comercialActionTrigger: null,
    administracionTiActionTrigger: null,
    finanzasActionTrigger: null,
    // Contador reactivo para forzar actualización de vistas en clics repetidos
    navTick: 0
  }),

  getters: {
    /**
     * Retorna el módulo efectivo. Si el usuario puede conmutar (Admin TI o Gerente General),
     * respeta activeModule. De lo contrario, está anclado a su rol.
     */
    currentModule: (state) => {
      const authStore = useAuthStore()
      if (authStore.canSwitchModules) {
        return state.activeModule
      }
      return ROLE_MODULE_MAP[authStore.userRole] || 'comercial'
    },

    moduleTitle: (state) => {
      const titles = {
        'administracion-ti': 'Administración TI',
        'gerencia-general': 'Gerencia General',
        'comercial': 'Recepción Comercial',
        'gestion-aduanera': 'Gestión Aduanera',
        'agente-aduanero': 'Agente Aduanero',
        'transporte-flota': 'Transporte y Flota',
        'finanzas': 'Finanzas',
        'legal-auditoria': 'Legal y Auditoría'
      }
      return titles[state.currentModule] || 'Operaciones'
    },

    // Getters de compatibilidad con nombres antiguos
    adminSubOption: (state) => state.administracionTiSubOption,
    gerenciaSubOption: (state) => state.gerenciaGeneralSubOption,
    ventasSubOption: (state) => state.comercialSubOption,
    supervisionSubOption: (state) => state.gestionAduaneraSubOption,
    aduanasSubOption: (state) => state.agenteAduaneroSubOption,
    transporteSubOption: (state) => state.transporteFlotaSubOption,
    legalSubOption: (state) => state.legalAuditoriaSubOption,
    liquidadorSubOption: (state) => state.gestionAduaneraSubOption,
    ventasActionTrigger: (state) => state.comercialActionTrigger,
    adminActionTrigger: (state) => state.administracionTiActionTrigger
  },

  actions: {
    setActiveModule(moduleName) {
      // Mapeo defensivo de módulos antiguos a nuevos
      const mapping = {
        admin: 'administracion-ti',
        gerencia: 'gerencia-general',
        ventas: 'comercial',
        supervision: 'gestion-aduanera',
        liquidador: 'gestion-aduanera',
        aduanas: 'agente-aduanero',
        logistica: 'agente-aduanero',
        transporte: 'transporte-flota',
        legal: 'legal-auditoria'
      }
      this.activeModule = mapping[moduleName] || moduleName
      this.navTick++
    },

    setAdministracionTiSubOption(option) {
      this.administracionTiSubOption = option
      this.navTick++
    },
    setAdminSubOption(option) {
      this.setAdministracionTiSubOption(option)
    },

    setGerenciaGeneralSubOption(option) {
      this.gerenciaGeneralSubOption = option
      this.navTick++
    },
    setGerenciaSubOption(option) {
      this.setGerenciaGeneralSubOption(option)
    },

    setComercialSubOption(option) {
      this.comercialSubOption = option
      this.navTick++
    },
    setVentasSubOption(option) {
      this.setComercialSubOption(option)
    },

    setGestionAduaneraSubOption(option) {
      this.gestionAduaneraSubOption = option
      this.navTick++
    },
    setSupervisionSubOption(option) {
      this.setGestionAduaneraSubOption(option)
    },
    setLiquidadorSubOption(option) {
      this.setGestionAduaneraSubOption(option)
    },

    setAgenteAduaneroSubOption(option) {
      this.agenteAduaneroSubOption = option
      this.navTick++
    },
    setAduanasSubOption(option) {
      this.setAgenteAduaneroSubOption(option)
    },

    setTransporteFlotaSubOption(option) {
      this.transporteFlotaSubOption = option
      this.navTick++
    },
    setTransporteSubOption(option) {
      this.setTransporteFlotaSubOption(option)
    },

    setLegalAuditoriaSubOption(option) {
      this.legalAuditoriaSubOption = option
      this.navTick++
    },
    setLegalSubOption(option) {
      this.setLegalAuditoriaSubOption(option)
    },

    setFinanzasSubOption(option) {
      this.finanzasSubOption = option
      this.navTick++
    },

    triggerComercialAction(action) {
      this.comercialActionTrigger = action
      this.navTick++
    },
    triggerVentasAction(action) {
      this.triggerComercialAction(action)
    },

    triggerAdministracionTiAction(action) {
      this.administracionTiActionTrigger = action
      this.navTick++
    },
    triggerAdminAction(action) {
      this.triggerAdministracionTiAction(action)
    },

    triggerFinanzasAction(action) {
      this.finanzasActionTrigger = action
      this.navTick++
    }
  }
})
