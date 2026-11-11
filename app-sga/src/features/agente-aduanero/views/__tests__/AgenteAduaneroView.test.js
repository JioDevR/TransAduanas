import { describe, it, expect } from 'vitest'
import AgenteAduaneroView from '../AgenteAduaneroView.vue'
import AgenteDashboardPanel from '../../components/AgenteDashboardPanel.vue'
import AgenteHeaderToolbar from '../../components/AgenteHeaderToolbar.vue'
import AgenteKpiGrid from '../../components/AgenteKpiGrid.vue'
import AgenteExpedientesTable from '../../components/AgenteExpedientesTable.vue'
import AgenteAccionesCell from '../../components/AgenteAccionesCell.vue'

describe('Portal Agente Aduanero - Definición de Componentes', () => {
  it('exporta AgenteAduaneroView como un componente Vue 3 válido', () => {
    expect(AgenteAduaneroView).toBeDefined()
    expect(typeof AgenteAduaneroView).toBe('object')
    expect(AgenteAduaneroView.__name).toBe('AgenteAduaneroView')
  })

  it('exporta AgenteDashboardPanel como un componente Vue 3 válido', () => {
    expect(AgenteDashboardPanel).toBeDefined()
    expect(typeof AgenteDashboardPanel).toBe('object')
    expect(AgenteDashboardPanel.__name).toBe('AgenteDashboardPanel')
  })

  it('exporta AgenteHeaderToolbar como un componente Vue 3 válido', () => {
    expect(AgenteHeaderToolbar).toBeDefined()
    expect(typeof AgenteHeaderToolbar).toBe('object')
    expect(AgenteHeaderToolbar.__name).toBe('AgenteHeaderToolbar')
  })

  it('exporta AgenteKpiGrid como un componente Vue 3 válido', () => {
    expect(AgenteKpiGrid).toBeDefined()
    expect(typeof AgenteKpiGrid).toBe('object')
    expect(AgenteKpiGrid.__name).toBe('AgenteKpiGrid')
  })

  it('exporta AgenteExpedientesTable como un componente Vue 3 válido', () => {
    expect(AgenteExpedientesTable).toBeDefined()
    expect(typeof AgenteExpedientesTable).toBe('object')
    expect(AgenteExpedientesTable.__name).toBe('AgenteExpedientesTable')
  })

  it('exporta AgenteAccionesCell como un componente Vue 3 válido', () => {
    expect(AgenteAccionesCell).toBeDefined()
    expect(typeof AgenteAccionesCell).toBe('object')
    expect(AgenteAccionesCell.__name).toBe('AgenteAccionesCell')
  })
})
