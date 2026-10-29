import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../authStore'
import { ROLES } from '@/core/auth/roles'

// Mocking localStorage for Node testing environment
let storage = {}
global.localStorage = {
  getItem: vi.fn((k) => storage[k] || null),
  setItem: vi.fn((k, v) => { storage[k] = String(v) }),
  removeItem: vi.fn((k) => { delete storage[k] }),
  clear: vi.fn(() => { storage = {} })
}

describe('authStore', () => {
  beforeEach(() => {
    storage = {}
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('debe inicializarse en estado no autenticado cuando localStorage está vacío', () => {
    const auth = useAuthStore()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.token).toBeNull()
    expect(auth.user).toBeNull()
    expect(auth.canSwitchModules).toBe(false)
    expect(auth.homePath).toBe('/login')
  })

  it('permite conmutar módulos exclusivamente a Administrador TI y Gerente General', () => {
    const auth = useAuthStore()

    // Caso 1: Administrador TI
    auth.user = { rol: ROLES.ADMINISTRADOR_TI, nombres: 'Admin', apellidos: 'SGA' }
    expect(auth.canSwitchModules).toBe(true)
    expect(auth.isAdministradorTI).toBe(true)
    expect(auth.isAdmin).toBe(true)
    expect(auth.homePath).toBe('/administracion-ti/dashboard')

    // Caso 2: Gerente General
    auth.user = { rol: ROLES.GERENTE_GENERAL, nombres: 'Gerente', apellidos: 'SGA' }
    expect(auth.canSwitchModules).toBe(true)
    expect(auth.isGerenteGeneral).toBe(true)
    expect(auth.homePath).toBe('/gerencia-general/dashboard')

    // Caso 3: Agente Comercial (Rol Operativo estricto)
    auth.user = { rol: ROLES.AGENTE_COMERCIAL, nombres: 'Ventas', apellidos: 'SGA' }
    expect(auth.canSwitchModules).toBe(false)
    expect(auth.homePath).toBe('/recepcion-comercial')

    // Caso 4: Contador (Finanzas)
    auth.user = { rol: ROLES.CONTADOR, nombres: 'Finanzas', apellidos: 'SGA' }
    expect(auth.canSwitchModules).toBe(false)
    expect(auth.homePath).toBe('/finanzas')
  })

  it('calcula userFullName correctamente', () => {
    const auth = useAuthStore()
    auth.user = { nombres: 'Carlos', apellidos: 'Mendoza' }
    expect(auth.userFullName).toBe('Carlos Mendoza')

    auth.user = null
    expect(auth.userFullName).toBe('')
  })

  it('limpia estado y localStorage al ejecutar logout()', () => {
    storage['sga_token'] = 'mock_token'
    storage['sga_user'] = JSON.stringify({ rol: ROLES.ADMINISTRADOR_TI })

    const auth = useAuthStore()
    auth.token = 'mock_token'
    auth.user = { rol: ROLES.ADMINISTRADOR_TI }

    auth.logout()

    expect(auth.token).toBeNull()
    expect(auth.user).toBeNull()
    expect(global.localStorage.removeItem).toHaveBeenCalledWith('sga_token')
    expect(global.localStorage.removeItem).toHaveBeenCalledWith('sga_user')
  })
})
