import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNavigationStore } from '../navigationStore'

describe('navigationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('debe inicializarse con administracion-ti y navTick en 0', () => {
    const store = useNavigationStore()
    expect(store.activeModule).toBe('administracion-ti')
    expect(store.navTick).toBe(0)
  })

  it('debe cambiar de módulo e incrementar navTick cuando el destino es diferente', () => {
    const store = useNavigationStore()
    store.setActiveModule('comercial')
    expect(store.activeModule).toBe('comercial')
    expect(store.navTick).toBe(1)
  })

  it('NO debe incrementar navTick si se vuelve a asignar el mismo módulo (guarda contra bucles)', () => {
    const store = useNavigationStore()
    store.setActiveModule('comercial')
    expect(store.navTick).toBe(1)

    // Reasignación del mismo módulo
    store.setActiveModule('comercial')
    expect(store.navTick).toBe(1)

    // Reasignación con alias legado 'ventas' -> 'comercial'
    store.setActiveModule('ventas')
    expect(store.navTick).toBe(1)
  })

  it('NO debe incrementar navTick si la sub-opción comercial no cambió (guarda contra bucles)', () => {
    const store = useNavigationStore()
    store.setComercialSubOption('cotizaciones')
    expect(store.navTick).toBe(1)

    // Reasignar la misma opción
    store.setComercialSubOption('cotizaciones')
    expect(store.navTick).toBe(1)

    // Cambiar a otra opción sí incrementa navTick
    store.setComercialSubOption('clientes')
    expect(store.navTick).toBe(2)
  })

  it('NO debe incrementar navTick si la sub-opción de finanzas no cambió', () => {
    const store = useNavigationStore()
    store.setFinanzasSubOption('liquidaciones')
    expect(store.navTick).toBe(1)

    store.setFinanzasSubOption('liquidaciones')
    expect(store.navTick).toBe(1)
  })

  it('mapea correctamente alias antiguos a sus módulos canónicos', () => {
    const store = useNavigationStore()
    store.setActiveModule('admin')
    expect(store.activeModule).toBe('administracion-ti')

    store.setActiveModule('supervision')
    expect(store.activeModule).toBe('gestion-aduanera')

    store.setActiveModule('transporte')
    expect(store.activeModule).toBe('transporte-flota')
  })
})
