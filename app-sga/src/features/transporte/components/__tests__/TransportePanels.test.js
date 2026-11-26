import { describe, it, expect } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import TransporteDashboardPanel from '../TransporteDashboardPanel.vue'
import DespachosPanel from '../DespachosPanel.vue'
import DespachosCanvasGrid from '../DespachosCanvasGrid.vue'
import FlotaPanel from '../FlotaPanel.vue'
import FlotaCanvasGrid from '../FlotaCanvasGrid.vue'
import ConductoresPanel from '../ConductoresPanel.vue'
import ConductoresCanvasGrid from '../ConductoresCanvasGrid.vue'
import DespachoDetalleDialog from '../dialogs/DespachoDetalleDialog.vue'
import CrearDespachoDialog from '../dialogs/CrearDespachoDialog.vue'
import AsignarRecursosDialog from '../dialogs/AsignarRecursosDialog.vue'
import ConfirmarEntregaDialog from '../dialogs/ConfirmarEntregaDialog.vue'
import IncidenciaDialog from '../dialogs/IncidenciaDialog.vue'
import UnidadFormDialog from '../dialogs/UnidadFormDialog.vue'
import ConductorFormDialog from '../dialogs/ConductorFormDialog.vue'
import { buildTransporteKpis } from '../../utils/transporteAlerts'

async function renderComponent(comp, props = {}) {
  const app = createSSRApp({
    render: () => h(comp, props)
  })
  app.use(createPinia())
  app.use(PrimeVue)
  app.use(ToastService)
  app.use(ConfirmationService)
  return renderToString(app)
}

describe('Módulo Transporte y Flota - Paneles y Diálogos', () => {
  const sampleDespacho = {
    idDespachoTransporte: 1,
    codigoTracking: 'TRK-2026-0001',
    tipoTraslado: 'ENTREGA_IMPORTACION',
    codigoExpediente: 'EXP-2026-0001',
    clienteRazonSocial: 'Camposol S.A.',
    unidadPlacaTracto: 'V1A-780',
    conductorNombre: 'Raul Cardenas',
    direccionPartida: 'DP World Callao',
    direccionLlegada: 'Planta Chao',
    distritoLlegada: 'Chao',
    estadoTransporte: 'EN_RUTA'
  }

  const sampleUnidad = {
    idUnidad: 1,
    placaTracto: 'V1A-780',
    marcaModelo: 'Volvo FH 500',
    tipoVehiculo: 'TRACTO',
    capacidadCargaKg: 32000,
    kilometrajeActual: 145000,
    estadoOperativo: 'DISPONIBLE',
    estadoActivo: true
  }

  const sampleConductor = {
    idConductor: 1,
    nombres: 'Raul',
    apellidos: 'Cardenas',
    numeroDocumento: '60123456',
    numeroLicencia: 'Q60123456',
    tipoLicencia: 'A-IIIc',
    estadoOperativo: 'DISPONIBLE',
    estadoActivo: true
  }

  it('renderiza TransporteDashboardPanel con KPIs y alertas', async () => {
    const kpis = buildTransporteKpis([sampleDespacho], [sampleUnidad], [sampleConductor], [])
    const html = await renderComponent(TransporteDashboardPanel, {
      despachos: [sampleDespacho],
      alertasMtc: [],
      kpis
    })
    expect(html).toContain('CONTROL LOGÍSTICO MTC')
    expect(html).toContain('Despachos En Tránsito')
    expect(html).toContain('TRK-2026-0001')
  })

  it('renderiza DespachosPanel, FlotaPanel y ConductoresPanel con SgaListShell', async () => {
    const htmlDesp = await renderComponent(DespachosPanel, { despachos: [sampleDespacho] })
    expect(htmlDesp).toContain('Monitoreo &amp; Bandeja de Despachos Terrestres')
    expect(htmlDesp).toContain('TRK-2026-0001')

    const htmlFlota = await renderComponent(FlotaPanel, { unidades: [sampleUnidad] })
    expect(htmlFlota).toContain('Parque Automotor &amp; Flota de Transporte MTC')
    expect(htmlFlota).toContain('V1A-780')

    const htmlCond = await renderComponent(ConductoresPanel, { conductores: [sampleConductor] })
    expect(htmlCond).toContain('Directorio de Conductores Profesionales MTC')
    expect(htmlCond).toContain('Q60123456')
    expect(htmlCond).not.toContain('App Chofer')
  })

  it('renderiza las vistas CanvasGrid correctamente', async () => {
    const htmlDespCanvas = await renderComponent(DespachosCanvasGrid, { despachos: [sampleDespacho] })
    expect(htmlDespCanvas).toContain('TRK-2026-0001')

    const htmlFlotaCanvas = await renderComponent(FlotaCanvasGrid, { unidades: [sampleUnidad] })
    expect(htmlFlotaCanvas).toContain('V1A-780')

    const htmlCondCanvas = await renderComponent(ConductoresCanvasGrid, { conductores: [sampleConductor] })
    expect(htmlCondCanvas).toContain('Q60123456')
  })

  it('exporta todos los diálogos operativos y maestros correctamente', () => {
    for (const dlg of [
      DespachoDetalleDialog,
      CrearDespachoDialog,
      AsignarRecursosDialog,
      ConfirmarEntregaDialog,
      IncidenciaDialog,
      UnidadFormDialog,
      ConductorFormDialog
    ]) {
      expect(dlg).toBeDefined()
      expect(typeof dlg).toBe('object')
    }
  })
})
