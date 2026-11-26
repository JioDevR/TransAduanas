import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ExpedienteTrackingDetailView from '../ExpedienteTrackingDetailView.vue'
import TrackingHeroCard from '../../components/tracking/TrackingHeroCard.vue'
import TrackingTimelineFeed from '../../components/tracking/TrackingTimelineFeed.vue'
import TrackingOperacionGrid from '../../components/tracking/TrackingOperacionGrid.vue'
import TrackingResumenAside from '../../components/tracking/TrackingResumenAside.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ params: { id: '15' } })
}))

vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showInfo: vi.fn()
  })
}))

const mockExpedienteImportacion = {
  idExpediente: 15,
  codigoExpediente: 'EXP-2026-0015',
  idCotizacion: 42,
  codigoCotizacion: 'COT-2026-0042',
  regimenAduanero: 'IMPORTACION_DEFINITIVA',
  nombreMercancia: 'Maquinaria Textil CNC',
  nombreTipoCarga: 'Contenedor FCL 40',
  agenteAduaneroNombre: 'Carlos Mendoza',
  numeroDam: '118-2026-10-004512',
  canalSunat: 'NARANJA',
  fechaAsignacionCanal: '2026-11-12T10:30:00',
  naveViaje: 'MSC CAPELLA',
  numeroContenedor: 'MSKU9087651',
  precintoAduanero: 'PR-7788',
  fechaZarpeEtd: '2026-11-01',
  fechaLlegadaEta: '2026-11-11',
  fechaArriboAduana: '2026-11-11T16:40:00',
  envioSunatSimuladoOk: true,
  codigoTransmisionSunat: 'TX-SUNAT-9921',
  tieneAlmacen: true,
  depositoTemporal: 'DP World Callao',
  ubicacionBahia: 'Zona B-14',
  cantidadBultos: 12,
  pesoBrutoKg: 4850,
  diasLibresAlmacen: 15,
  diasRestantesAlmacen: 6,
  fechaLimiteRetiro: '2026-11-26',
  estadoAlmacen: 'CUSTODIA',
  tieneTransporte: true,
  tipoTraslado: 'ENTREGA_IMPORTACION',
  direccionPartida: 'Terminal Portuario Callao',
  direccionLlegada: 'Av. Nicolás Ayllón 3200',
  distritoLlegada: 'Ate',
  estadoTransporte: 'SOLICITADO',
  contactoExteriorNombre: 'Shanghai Textile Machinery',
  contactoExteriorCorreo: 'ops@shanghaitex.cn',
  estadoExpediente: 'INSPECCION',
  fechaApertura: '2026-11-01T09:00:00',
  informeConformidadTexto: 'Carga entregada sin observaciones.',
  receptorConformidadNombre: 'Ana Torres',
  receptorConformidadDni: '45128901',
  eventos: [
    {
      idEvento: 1,
      codigoEstado: 'EXPEDIENTE_CREADO',
      titulo: 'Expediente Aperturado',
      descripcion: 'Carpeta digital abierta en SGA.',
      usuarioNombre: 'Gestión Aduanera',
      fechaEvento: '2026-11-01T09:00:00'
    },
    {
      idEvento: 2,
      codigoEstado: 'CANAL_ASIGNADO',
      titulo: 'Canal SUNAT Asignado: NARANJA',
      descripcion: 'Revisión documentaria en curso.',
      usuarioNombre: 'Carlos Mendoza',
      fechaEvento: '2026-11-12T10:30:00'
    }
  ]
}

const mockDocumentos = [
  {
    idDocumento: 101,
    nombreOriginal: 'DAM-118-2026-10-004512.pdf',
    formatoMime: 'application/pdf',
    tamanoBytes: 245120,
    fechaSubida: '2026-11-12T11:00:00'
  }
]

describe('ExpedienteTrackingDetailView & Subcomponentes sgaUI', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renderiza ExpedienteTrackingDetailView en SSR sin errores', async () => {
    const app = createSSRApp({
      render: () => h(ExpedienteTrackingDetailView)
    })
    app.use(PrimeVue)

    const html = await renderToString(app)
    expect(html).toContain('sga-tracking-view')
    expect(html).toContain('Volver')
  })

  it('renderiza TrackingHeroCard con progreso, ruta logística y 6 hitos', async () => {
    const app = createSSRApp({
      render: () => h(TrackingHeroCard, { expediente: mockExpedienteImportacion })
    })
    app.use(PrimeVue)

    const html = await renderToString(app)
    expect(html).toContain('Maquinaria Textil CNC')
    expect(html).toContain('118-2026-10-004512')
    expect(html).toContain('sga-route-strip')
    expect(html).toContain('sga-milestone-track')
    expect(html).toContain('4 de 6 hitos completados')
    expect(html).toContain('HITO 06')
  })

  it('renderiza TrackingTimelineFeed ordenado cronológicamente y con estado vacío seguro', async () => {
    const appWithEvents = createSSRApp({
      render: () => h(TrackingTimelineFeed, { eventos: mockExpedienteImportacion.eventos })
    })
    appWithEvents.use(PrimeVue)
    const htmlEvents = await renderToString(appWithEvents)
    expect(htmlEvents).toContain('2 evento(s)')
    expect(htmlEvents).toContain('Canal SUNAT Asignado: NARANJA')
    expect(htmlEvents.indexOf('Canal SUNAT Asignado: NARANJA')).toBeLessThan(
      htmlEvents.indexOf('Expediente Aperturado')
    )

    const appEmpty = createSSRApp({
      render: () => h(TrackingTimelineFeed, { eventos: [] })
    })
    appEmpty.use(PrimeVue)
    const htmlEmpty = await renderToString(appEmpty)
    expect(htmlEmpty).toContain('0 evento(s)')
    expect(htmlEmpty).toContain('Aún no se registran eventos detallados')
  })

  it('renderiza TrackingOperacionGrid y TrackingResumenAside con bóveda documental', async () => {
    const appGrid = createSSRApp({
      render: () => h(TrackingOperacionGrid, { expediente: mockExpedienteImportacion })
    })
    appGrid.use(PrimeVue)
    const htmlGrid = await renderToString(appGrid)
    expect(htmlGrid).toContain('Control Aduanero &amp; SUNAT')
    expect(htmlGrid).toContain('DP World Callao')
    expect(htmlGrid).toContain('SOLICITADO')
    expect(htmlGrid).toContain('Av. Nicolás Ayllón 3200')
    expect(htmlGrid).toContain('Informe de Conformidad al Cliente')

    const appAside = createSSRApp({
      render: () =>
        h(TrackingResumenAside, {
          expediente: mockExpedienteImportacion,
          documentos: mockDocumentos,
          loadingDocs: false
        })
    })
    appAside.use(PrimeVue)
    const htmlAside = await renderToString(appAside)
    expect(htmlAside).toContain('EXP-2026-0015')
    expect(htmlAside).toContain('Carlos Mendoza')
    expect(htmlAside).toContain('DAM-118-2026-10-004512.pdf')
  })
})
