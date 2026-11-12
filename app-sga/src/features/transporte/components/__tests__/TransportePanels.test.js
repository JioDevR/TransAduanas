import { describe, it, expect } from 'vitest'
import TransporteHeaderToolbar from '../TransporteHeaderToolbar.vue'
import TransporteDashboardPanel from '../TransporteDashboardPanel.vue'
import TransporteDespachosTable from '../TransporteDespachosTable.vue'
import CabinaRutaConductorPanel from '../CabinaRutaConductorPanel.vue'
import FlotaManagementPanel from '../FlotaManagementPanel.vue'
import ConductoresManagementPanel from '../ConductoresManagementPanel.vue'

describe('Transporte y Flota - Componentes Modulares', () => {
  it('exporta TransporteHeaderToolbar como componente válido', () => {
    expect(TransporteHeaderToolbar).toBeDefined()
    expect(typeof TransporteHeaderToolbar).toBe('object')
    expect(TransporteHeaderToolbar.__name).toBe('TransporteHeaderToolbar')
  })

  it('exporta TransporteDashboardPanel como componente válido', () => {
    expect(TransporteDashboardPanel).toBeDefined()
    expect(typeof TransporteDashboardPanel).toBe('object')
    expect(TransporteDashboardPanel.__name).toBe('TransporteDashboardPanel')
  })

  it('exporta TransporteDespachosTable como componente válido', () => {
    expect(TransporteDespachosTable).toBeDefined()
    expect(typeof TransporteDespachosTable).toBe('object')
    expect(TransporteDespachosTable.__name).toBe('TransporteDespachosTable')
  })

  it('exporta CabinaRutaConductorPanel como componente válido', () => {
    expect(CabinaRutaConductorPanel).toBeDefined()
    expect(typeof CabinaRutaConductorPanel).toBe('object')
    expect(CabinaRutaConductorPanel.__name).toBe('CabinaRutaConductorPanel')
  })

  it('exporta FlotaManagementPanel como componente válido', () => {
    expect(FlotaManagementPanel).toBeDefined()
    expect(typeof FlotaManagementPanel).toBe('object')
    expect(FlotaManagementPanel.__name).toBe('FlotaManagementPanel')
  })

  it('exporta ConductoresManagementPanel como componente válido', () => {
    expect(ConductoresManagementPanel).toBeDefined()
    expect(typeof ConductoresManagementPanel).toBe('object')
    expect(ConductoresManagementPanel.__name).toBe('ConductoresManagementPanel')
  })
})
