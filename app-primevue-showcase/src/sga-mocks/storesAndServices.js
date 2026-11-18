import { ref, reactive, computed } from 'vue'
import { ROLES, ROLE_MODULE_MAP } from '../sga-components/roles.js'

const demoUser = reactive({
  idUsuario: 1,
  username: 'admin.demo',
  nombres: 'Carlos',
  apellidos: 'Mendoza',
  nombre: 'Carlos Mendoza',
  rol: ROLES.ADMINISTRADOR_TI,
  codigoSede: 'LIM-01',
  nombreSede: 'Sede Central Callao',
  idCliente: 1,
  razonSocial: 'Empresa Demo SAC'
})

const authStoreSingleton = {
  get user() {
    return demoUser
  },
  token: 'demo-token',
  isAuthenticated: true,
  get userRole() {
    return demoUser.rol
  },
  get userFullName() {
    return `${demoUser.nombres} ${demoUser.apellidos}`.trim()
  },
  get canSwitchModules() {
    return [ROLES.ADMINISTRADOR_TI, ROLES.GERENTE_GENERAL].includes(demoUser.rol)
  },
  hasRole: () => true,
  setDemoRole: (newRole) => {
    demoUser.rol = newRole
    navState.activeModule = ROLE_MODULE_MAP[newRole] || 'administracion-ti'
  },
  logout: () => {}
}

export const useAuthStore = () => authStoreSingleton

const navState = reactive({
  isSidebarCollapsed: false,
  activeModule: 'administracion-ti',
  administracionTiSubOption: 'dashboard',
  gerenciaGeneralSubOption: 'dashboard',
  comercialSubOption: 'dashboard',
  gestionAduaneraSubOption: 'dashboard',
  agenteAduaneroSubOption: 'dashboard',
  transporteFlotaSubOption: 'dashboard',
  legalAuditoriaSubOption: 'expedientes',
  finanzasSubOption: 'dashboard',
  navTick: 0
})

const currentModuleComputed = computed(() => {
  if (authStoreSingleton.canSwitchModules) {
    return navState.activeModule
  }
  return ROLE_MODULE_MAP[demoUser.rol] || 'comercial'
})

const moduleTitleComputed = computed(() => {
  const titles = {
    'administracion-ti': 'Administración TI',
    'gerencia-general': 'Gerencia General',
    comercial: 'Recepción Comercial',
    'gestion-aduanera': 'Gestión Aduanera',
    'agente-aduanero': 'Agente Aduanero',
    'transporte-flota': 'Transporte y Flota',
    finanzas: 'Finanzas',
    'legal-auditoria': 'Legal y Auditoría'
  }
  return titles[currentModuleComputed.value] || 'Operaciones'
})

const navStoreSingleton = {
  get isSidebarCollapsed() {
    return navState.isSidebarCollapsed
  },
  set isSidebarCollapsed(v) {
    navState.isSidebarCollapsed = v
  },
  get activeModule() {
    return navState.activeModule
  },
  set activeModule(v) {
    navState.activeModule = v
  },
  get currentModule() {
    return currentModuleComputed.value
  },
  get moduleTitle() {
    return moduleTitleComputed.value
  },
  get administracionTiSubOption() { return navState.administracionTiSubOption },
  get gerenciaGeneralSubOption() { return navState.gerenciaGeneralSubOption },
  get comercialSubOption() { return navState.comercialSubOption },
  get gestionAduaneraSubOption() { return navState.gestionAduaneraSubOption },
  get agenteAduaneroSubOption() { return navState.agenteAduaneroSubOption },
  get transporteFlotaSubOption() { return navState.transporteFlotaSubOption },
  get legalAuditoriaSubOption() { return navState.legalAuditoriaSubOption },
  get finanzasSubOption() { return navState.finanzasSubOption },
  get navTick() { return navState.navTick },
  toggleSidebar: () => {
    navState.isSidebarCollapsed = !navState.isSidebarCollapsed
  },
  setActiveModule: (m) => { navState.activeModule = m; navState.navTick++ },
  setAdministracionTiSubOption: (o) => { navState.administracionTiSubOption = o; navState.navTick++ },
  setGerenciaGeneralSubOption: (o) => { navState.gerenciaGeneralSubOption = o; navState.navTick++ },
  setComercialSubOption: (o) => { navState.comercialSubOption = o; navState.navTick++ },
  setGestionAduaneraSubOption: (o) => { navState.gestionAduaneraSubOption = o; navState.navTick++ },
  setAgenteAduaneroSubOption: (o) => { navState.agenteAduaneroSubOption = o; navState.navTick++ },
  setTransporteFlotaSubOption: (o) => { navState.transporteFlotaSubOption = o; navState.navTick++ },
  setLegalAuditoriaSubOption: (o) => { navState.legalAuditoriaSubOption = o; navState.navTick++ },
  setFinanzasSubOption: (o) => { navState.finanzasSubOption = o; navState.navTick++ },
  triggerComercialAction: () => { navState.navTick++ },
  triggerAdministracionTiAction: () => { navState.navTick++ },
  triggerFinanzasAction: () => { navState.navTick++ }
}

export const useNavigationStore = () => navStoreSingleton

const createServiceProxy = () =>
  new Proxy(
    {},
    {
      get: () => async () => ({ data: [], content: [] })
    }
  )

export const catalogoService = createServiceProxy()
export const sedeService = createServiceProxy()
export const userService = createServiceProxy()
export const expedienteService = createServiceProxy()
export const gestionAduaneraService = createServiceProxy()
export const documentoService = createServiceProxy()
export const finanzasService = createServiceProxy()
export const transporteFlotaService = createServiceProxy()
export const portalUserService = createServiceProxy()
export const almacenService = createServiceProxy()
export const comercialService = createServiceProxy()
export const portalCotizacionService = createServiceProxy()
export default createServiceProxy()
