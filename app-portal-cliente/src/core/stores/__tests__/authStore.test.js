import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../authStore'
import apiClient from '@/core/api/apiClient'

vi.mock('@/core/api/apiClient', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn()
  }
}))

let storage = {}
global.localStorage = {
  getItem: vi.fn((key) => storage[key] ?? null),
  setItem: vi.fn((key, value) => { storage[key] = String(value) }),
  removeItem: vi.fn((key) => { delete storage[key] }),
  clear: vi.fn(() => { storage = {} })
}

describe('portal-cliente authStore', () => {
  beforeEach(() => {
    storage = {}
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Estado Inicial', () => {
    it('debe inicializarse en estado no autenticado cuando localStorage está vacío', () => {
      const auth = useAuthStore()

      expect(auth.token).toBeNull()
      expect(auth.user).toBeNull()
      expect(auth.loading).toBe(false)
      expect(auth.error).toBeNull()
      expect(auth.isAuthenticated).toBe(false)
      expect(auth.empresaNombre).toBe('Empresa Cliente')
      expect(auth.ruc).toBe('')
      expect(auth.usuarioNombre).toBe('')
    })

    it('debe restaurar token y usuario desde localStorage cuando existen datos válidos', () => {
      const mockUser = {
        idUsuarioCliente: 1,
        username: 'cliente_demo',
        nombres: 'Juan Pérez',
        ruc: '20123456789',
        razonSocial: 'Importadora Los Andes S.A.C.',
        contactoRepresentante: 'Juan Pérez'
      }
      storage['sga_portal_token'] = 'valid-jwt-token'
      storage['sga_portal_user'] = JSON.stringify(mockUser)

      const auth = useAuthStore()

      expect(auth.token).toBe('valid-jwt-token')
      expect(auth.user).toEqual(mockUser)
      expect(auth.isAuthenticated).toBe(true)
      expect(auth.empresaNombre).toBe('Importadora Los Andes S.A.C.')
      expect(auth.ruc).toBe('20123456789')
      expect(auth.usuarioNombre).toBe('Juan Pérez')
    })

    it('debe recuperarse elegantemente a null y limpiar localStorage si el JSON está corrupto', () => {
      storage['sga_portal_user'] = '{ corrupted_json: invalid syntax'
      storage['sga_portal_token'] = 'some-token'

      const auth = useAuthStore()

      expect(auth.user).toBeNull()
      expect(auth.isAuthenticated).toBe(false)
      expect(global.localStorage.removeItem).toHaveBeenCalledWith('sga_portal_user')
      expect(storage['sga_portal_user']).toBeUndefined()
    })
  })

  describe('Flujo de Login', () => {
    it('debe autenticar con éxito, actualizar estado reactivo y persistir en localStorage', async () => {
      const auth = useAuthStore()
      const loginPayload = {
        token: 'new-token-123',
        idUsuarioCliente: 10,
        username: 'acme_user',
        contactoRepresentante: 'Carlos Santana',
        nombres: 'Carlos Santana',
        correoElectronico: 'carlos@acme.com',
        idCliente: 5,
        ruc: '20999888777',
        razonSocial: 'ACME Corp',
        debeCambiarPassword: false
      }

      apiClient.post.mockResolvedValueOnce({
        data: {
          success: true,
          data: loginPayload
        }
      })

      const result = await auth.login('acme_user ', 'secret123')

      expect(result).toEqual({ success: true })
      expect(apiClient.post).toHaveBeenCalledWith('/portal/auth/login', {
        username: 'acme_user',
        password: 'secret123'
      })
      expect(auth.token).toBe('new-token-123')
      expect(auth.user).toMatchObject({
        idUsuarioCliente: 10,
        username: 'acme_user',
        ruc: '20999888777',
        razonSocial: 'ACME Corp'
      })
      expect(auth.isAuthenticated).toBe(true)
      expect(auth.loading).toBe(false)
      expect(auth.error).toBeNull()
      expect(global.localStorage.setItem).toHaveBeenCalledWith('sga_portal_token', 'new-token-123')
      expect(global.localStorage.setItem).toHaveBeenCalledWith('sga_portal_user', expect.any(String))
    })

    it('debe manejar error cuando la API retorna credenciales inválidas', async () => {
      const auth = useAuthStore()
      apiClient.post.mockRejectedValueOnce({
        response: {
          data: {
            message: 'Credenciales inválidas'
          }
        }
      })

      const result = await auth.login('bad_user', 'wrong_pass')

      expect(result).toEqual({ success: false, message: 'Credenciales inválidas' })
      expect(auth.token).toBeNull()
      expect(auth.user).toBeNull()
      expect(auth.isAuthenticated).toBe(false)
      expect(auth.error).toBe('Credenciales inválidas')
      expect(auth.loading).toBe(false)
    })

    it('debe manejar respuesta inválida sin token', async () => {
      const auth = useAuthStore()
      apiClient.post.mockResolvedValueOnce({
        data: {
          data: null
        }
      })

      const result = await auth.login('user', 'pass')

      expect(result.success).toBe(false)
      expect(result.message).toBe('Respuesta de autenticación inválida')
      expect(auth.error).toBe('Respuesta de autenticación inválida')
    })
  })

  describe('Flujo de Logout', () => {
    it('debe limpiar estado reactivo y eliminar llaves de localStorage al ejecutar logout()', () => {
      storage['sga_portal_token'] = 'token-to-clear'
      storage['sga_portal_user'] = JSON.stringify({ username: 'demo' })

      const auth = useAuthStore()
      expect(auth.isAuthenticated).toBe(true)

      auth.logout()

      expect(auth.token).toBeNull()
      expect(auth.user).toBeNull()
      expect(auth.isAuthenticated).toBe(false)
      expect(global.localStorage.removeItem).toHaveBeenCalledWith('sga_portal_token')
      expect(global.localStorage.removeItem).toHaveBeenCalledWith('sga_portal_user')
      expect(storage['sga_portal_token']).toBeUndefined()
      expect(storage['sga_portal_user']).toBeUndefined()
    })
  })

  describe('Perfil y Cambio de Contraseña', () => {
    it('fetchPerfil no ejecuta llamado si no hay token', async () => {
      const auth = useAuthStore()
      await auth.fetchPerfil()
      expect(apiClient.get).not.toHaveBeenCalled()
    })

    it('fetchPerfil actualiza usuario y localStorage si hay token', async () => {
      storage['sga_portal_token'] = 'active-token'
      storage['sga_portal_user'] = JSON.stringify({ idUsuarioCliente: 1, username: 'demo' })
      const auth = useAuthStore()

      apiClient.get.mockResolvedValueOnce({
        data: {
          data: { direccion: 'Av. Las Palmas 123', telefono: '999888777' }
        }
      })

      await auth.fetchPerfil()

      expect(auth.user.direccion).toBe('Av. Las Palmas 123')
      expect(global.localStorage.setItem).toHaveBeenCalledWith(
        'sga_portal_user',
        expect.stringContaining('Av. Las Palmas 123')
      )
    })

    it('cambiarPassword actualiza debeCambiarPassword y guarda en storage', async () => {
      storage['sga_portal_token'] = 'active-token'
      storage['sga_portal_user'] = JSON.stringify({ idUsuarioCliente: 1, debeCambiarPassword: true })
      const auth = useAuthStore()

      apiClient.post.mockResolvedValueOnce({
        data: { message: 'Contraseña actualizada' }
      })

      const res = await auth.cambiarPassword('old123', 'new456')

      expect(res.success).toBe(true)
      expect(auth.user.debeCambiarPassword).toBe(false)
      expect(global.localStorage.setItem).toHaveBeenCalledWith(
        'sga_portal_user',
        expect.stringContaining('"debeCambiarPassword":false')
      )
    })
  })
})
