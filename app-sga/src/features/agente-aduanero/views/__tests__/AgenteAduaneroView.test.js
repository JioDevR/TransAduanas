import { describe, it, expect } from 'vitest'
import AgenteAduaneroView from '../AgenteAduaneroView.vue'
import AgenteDashboardPanel from '../../components/AgenteDashboardPanel.vue'
import AgenteHeaderToolbar from '../../components/AgenteHeaderToolbar.vue'
import AgenteKpiGrid from '../../components/AgenteKpiGrid.vue'
import AgenteExpedientesTable from '../../components/AgenteExpedientesTable.vue'
import AgenteExpedientesCanvas from '../../components/AgenteExpedientesCanvas.vue'
import AgenteExpedienteInformeView from '../../components/AgenteExpedienteInformeView.vue'
import AgenteOperacionTransitoView from '../../components/AgenteOperacionTransitoView.vue'
import AgenteAccionesCell from '../../components/AgenteAccionesCell.vue'

describe('Portal Agente Aduanero - Definición de Componentes y Vistas', () => {
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

  it('exporta AgenteExpedientesCanvas como un componente Vue 3 válido para el canvas de expedientes', () => {
    expect(AgenteExpedientesCanvas).toBeDefined()
    expect(typeof AgenteExpedientesCanvas).toBe('object')
    expect(AgenteExpedientesCanvas.__name).toBe('AgenteExpedientesCanvas')
  })

  it('exporta AgenteExpedienteInformeView como un componente Vue 3 válido para el informe a pantalla completa', () => {
    expect(AgenteExpedienteInformeView).toBeDefined()
    expect(typeof AgenteExpedienteInformeView).toBe('object')
    expect(AgenteExpedienteInformeView.__name).toBe('AgenteExpedienteInformeView')
  })

  it('exporta AgenteOperacionTransitoView como un componente Vue 3 válido para la vista completa de operaciones', () => {
    expect(AgenteOperacionTransitoView).toBeDefined()
    expect(typeof AgenteOperacionTransitoView).toBe('object')
    expect(AgenteOperacionTransitoView.__name).toBe('AgenteOperacionTransitoView')
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
