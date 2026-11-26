import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAdminDashboard, formatBytes, formatUptime } from '../useAdminDashboard'
import { sistemaService } from '../../services/sistemaService'
import { userService } from '../../users/services/userService'

const mockAuthState = { isAdministradorTI: true }

vi.mock('@/features/auth/stores/authStore', () => ({
  useAuthStore: () => mockAuthState
}))

vi.mock('../../services/sistemaService', () => ({
  sistemaService: {
    getDashboard: vi.fn(),
    getEstado: vi.fn(),
    getAlmacenamiento: vi.fn()
  }
}))

vi.mock('../../users/services/userService', () => ({
  userService: {
    getUsers: vi.fn()
  }
}))

describe('useAdminDashboard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockAuthState.isAdministradorTI = true
  })

  it('carga dashboard completo para ADMINISTRADOR_TI y calcula series/gráficos', async () => {
    sistemaService.getDashboard.mockResolvedValueOnce({
      sistema: { bdConectada: true, bdLatenciaMs: 4 },
      usuariosTotal: 10,
      usuariosActivos: 8,
      usuariosInactivos: 2,
      distribucionRoles: { ADMINISTRADOR_TI: 2, AGENTE_COMERCIAL: 8 },
      actividadDiaria: [{ fecha: '2026-11-20', exitos: 5, fallos: 1 }]
    })
    sistemaService.getAlmacenamiento.mockResolvedValueOnce({
      disponible: true,
      porEntidad: [{ clave: 'EXPEDIENTE', etiqueta: 'EXPEDIENTE', cantidad: 3, tamanoBytes: 1024 }],
      porExtension: [{ clave: 'PDF', etiqueta: 'PDF', cantidad: 3, tamanoBytes: 1024 }]
    })

    const dash = useAdminDashboard()
    await dash.loadDashboard()

    expect(sistemaService.getDashboard).toHaveBeenCalledWith({ dias: 7, modulo: 'TODOS' })
    expect(dash.isBackendHealthy.value).toBe(true)
    expect(dash.activeUsersPct.value).toBe(80)
    expect(dash.rolesChartData.value.labels.length).toBe(2)
    expect(dash.actividadChartData.value.labels).toEqual(['11-20'])
    expect(dash.storageEntidadChartData.value.labels).toEqual(['EXPEDIENTE'])
    expect(dash.storageExtensionChartData.value.labels).toEqual(['PDF (3)'])
  })

  it('hace fallback seguro para GERENTE_GENERAL sin llamar /admin/sistema/dashboard', async () => {
    mockAuthState.isAdministradorTI = false
    sistemaService.getEstado.mockResolvedValueOnce({ bdConectada: true })
    sistemaService.getAlmacenamiento.mockResolvedValueOnce({ disponible: true })
    userService.getUsers.mockResolvedValueOnce([
      { nombreRol: 'GERENTE_GENERAL', estadoActivo: true },
      { nombreRol: 'CONTADOR', estadoActivo: false }
    ])

    const dash = useAdminDashboard()
    await dash.loadDashboard()

    expect(sistemaService.getDashboard).not.toHaveBeenCalled()
    expect(sistemaService.getEstado).toHaveBeenCalled()
    expect(dash.dashboardData.value.usuariosTotal).toBe(2)
    expect(dash.activeUsersPct.value).toBe(50)
  })

  it('formatea bytes y uptime correctamente', () => {
    expect(formatBytes(512)).toBe('512 B')
    expect(formatBytes(2048)).toBe('2.0 KB')
    expect(formatUptime(3665)).toBe('1h 1m')
  })
})
