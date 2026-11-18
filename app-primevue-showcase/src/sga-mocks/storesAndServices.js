import { ref } from 'vue'

export const useAuthStore = () => ({
  user: { idUsuario: 1, nombre: 'Admin Demo', rol: 'ADMINISTRADOR_TI', idCliente: 1, razonSocial: 'Empresa Demo SAC' },
  token: 'demo-token',
  isAuthenticated: true,
  hasRole: () => true,
  logout: () => {}
})

export const useNavigationStore = () => ({
  isSidebarCollapsed: ref(false),
  toggleSidebar: () => {}
})

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
