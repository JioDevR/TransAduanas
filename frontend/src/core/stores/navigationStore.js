import { defineStore } from 'pinia'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { ROLE_MODULE_MAP } from '@/core/auth/roles'

export const useNavigationStore = defineStore('navigation', {
  state: () => ({
    // Módulo activo: 'administracion-ti' | 'gerencia-general' | 'comercial' | 'gestion-aduanera' | 'agente-aduanero' | 'transporte-flota' | 'finanzas' | 'legal-auditoria'
    activeModule: 'administracion-ti',
    // Sub-opciones en Administración TI: 'dashboard' | 'users' | 'sedes' | 'catalogos' | 'logs' | 'about'
    administracionTiSubOption: 'dashboard',
    // Sub-opciones en Gerencia General: 'dashboard' | 'sedes' | 'puertos' | 'tipos-carga' | 'mercancias' | 'flota' | 'conductores'
    gerenciaGeneralSubOption: 'dashboard',
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
    // Sub-opciones en Finanzas: 'dashboard' | 'pre-expedientes' | 'liquidaciones' | 'conciliacion'
    finanzasSubOption: 'dashboard',
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
      const target = mapping[moduleName] || moduleName
      if (this.activeModule === target) return
      this.activeModule = target
      this.navTick++
    },

    setAdministracionTiSubOption(option) {
      if (this.administracionTiSubOption === option) return
      this.administracionTiSubOption = option
      this.navTick++
    },
    setAdminSubOption(option) {
      this.setAdministracionTiSubOption(option)
    },

    setGerenciaGeneralSubOption(option) {
      const target = option === 'catalogos' ? 'puertos' : option
      if (this.gerenciaGeneralSubOption === target) return
      this.gerenciaGeneralSubOption = target
      this.navTick++
    },
    setGerenciaSubOption(option) {
      this.setGerenciaGeneralSubOption(option)
    },

    setComercialSubOption(option) {
      if (this.comercialSubOption === option) return
      this.comercialSubOption = option
      this.navTick++
    },
    setVentasSubOption(option) {
      this.setComercialSubOption(option)
    },

    setGestionAduaneraSubOption(option) {
      if (this.gestionAduaneraSubOption === option) return
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
      if (this.agenteAduaneroSubOption === option) return
      this.agenteAduaneroSubOption = option
      this.navTick++
    },
    setAduanasSubOption(option) {
      this.setAgenteAduaneroSubOption(option)
    },

    setTransporteFlotaSubOption(option) {
      if (this.transporteFlotaSubOption === option) return
      this.transporteFlotaSubOption = option
      this.navTick++
    },
    setTransporteSubOption(option) {
      this.setTransporteFlotaSubOption(option)
    },

    setLegalAuditoriaSubOption(option) {
      if (this.legalAuditoriaSubOption === option) return
      this.legalAuditoriaSubOption = option
      this.navTick++
    },
    setLegalSubOption(option) {
      this.setLegalAuditoriaSubOption(option)
    },

    setFinanzasSubOption(option) {
      if (this.finanzasSubOption === option && !this.finanzasActionTrigger) return
      this.finanzasSubOption = option
      this.finanzasActionTrigger = null
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
