import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'

vi.stubGlobal('localStorage', {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
})

vi.mock('primevue/dialog', () => ({
  default: {
    name: 'Dialog',
    props: ['visible'],
    render() {
      return this.visible ? h('div', { class: 'p-dialog-stub' }, [this.$slots.header?.(), this.$slots.default?.()]) : null
    }
  }
}))

vi.mock('@/features/admin/sedes/services/sedeService', () => ({
  default: {
    getSedes: vi.fn().mockResolvedValue([]),
    deleteSede: vi.fn().mockResolvedValue({}),
    reactivateSede: vi.fn().mockResolvedValue({}),
    downloadPdf: vi.fn().mockResolvedValue(new Blob(['pdf']))
  }
}))

vi.mock('@/features/admin/catalogos/services/catalogoService', () => ({
  default: {
    getPuertos: vi.fn().mockResolvedValue([]),
    deletePuerto: vi.fn().mockResolvedValue({}),
    reactivarPuerto: vi.fn().mockResolvedValue({}),
    downloadPuertosPdf: vi.fn().mockResolvedValue(new Blob(['pdf'])),
    getTiposCarga: vi.fn().mockResolvedValue([]),
    deleteTipoCarga: vi.fn().mockResolvedValue({}),
    reactivarTipoCarga: vi.fn().mockResolvedValue({}),
    downloadTiposCargaPdf: vi.fn().mockResolvedValue(new Blob(['pdf'])),
    getMercancias: vi.fn().mockResolvedValue([]),
    deleteMercancia: vi.fn().mockResolvedValue({}),
    reactivarMercancia: vi.fn().mockResolvedValue({}),
    downloadMercanciasPdf: vi.fn().mockResolvedValue(new Blob(['pdf']))
  }
}))

import SedesManagementView from '@/features/admin/sedes/views/SedesManagementView.vue'
import SedeRegistrationForm from '@/features/admin/sedes/components/SedeRegistrationForm.vue'
import PuertosManagementView from '../PuertosManagementView.vue'
import PuertoRegistrationForm from '../../components/PuertoRegistrationForm.vue'
import TiposCargaManagementView from '../TiposCargaManagementView.vue'
import TipoCargaRegistrationForm from '../../components/TipoCargaRegistrationForm.vue'
import MercanciasManagementView from '../MercanciasManagementView.vue'
import MercanciaFormModal from '../../components/MercanciaFormModal.vue'
import { useAuthStore } from '@/features/auth/stores/authStore'

async function renderView(Comp, props = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  auth.user = { idUsuario: 1, rol: 'GERENTE_GENERAL', nombres: 'Ana', apellidos: 'Torres' }

  const app = createSSRApp({
    render: () => h(Comp, props)
  })
  app.use(pinia)
  app.use(PrimeVue)
  app.use(ToastService)
  app.use(ConfirmationService)
  return renderToString(app)
}

describe('Sub-opciones de Gerencia General (Sedes, Puertos, TiposCarga, Mercancias)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('SedesManagementView usa SgaListShell, sga-kpis-grid y respeta Single-Title Rule', async () => {
    const html = await renderView(SedesManagementView)
    expect(html).toContain('sga-list-panel')
    expect(html).toContain('sga-kpis-grid')
    expect(html).not.toContain('<h1')
    expect(html).not.toContain('Red de Sedes e Infraestructura')
  })

  it('PuertosManagementView usa SgaListShell y sga-kpis-grid sin h1 redundante', async () => {
    const html = await renderView(PuertosManagementView)
    expect(html).toContain('sga-list-panel')
    expect(html).toContain('sga-kpis-grid')
    expect(html).not.toContain('<h1')
    expect(html).not.toContain('Catálogo de Puertos y Terminales')
  })

  it('TiposCargaManagementView usa SgaListShell y NO renderiza campos fantasma de contenedor/tara', async () => {
    const html = await renderView(TiposCargaManagementView)
    expect(html).toContain('sga-list-panel')
    expect(html).toContain('sga-kpis-grid')
    expect(html).not.toContain('<h1')
    expect(html).not.toContain('CONTENEDOR SUGERIDO')
    expect(html).not.toContain('TARA SUGERIDA')
  })

  it('MercanciasManagementView usa SgaListShell y sga-kpis-grid sin h1 redundante', async () => {
    const html = await renderView(MercanciasManagementView)
    expect(html).toContain('sga-list-panel')
    expect(html).toContain('sga-kpis-grid')
    expect(html).not.toContain('<h1')
    expect(html).not.toContain('Partidas Arancelarias y Mercancías')
  })

  it('Formularios full-screen usan sga-forms.css, deshabilitan código en edición y omiten campos fantasma en Tipos de Carga', async () => {
    const sedeHtml = await renderView(SedeRegistrationForm, {
      sedeToEdit: { idSede: 1, codigoSede: 'LIM-01', nombre: 'Lima', tipo: 'SEDE_CENTRAL', distrito: 'Lima', direccion: 'Av. 1', estado: 'OPERATIVO' }
    })
    expect(sedeHtml).toContain('sga-form-header')
    expect(sedeHtml).toContain('sga-split-layout')
    expect(sedeHtml).toContain('MODO EDICIÓN')
    expect(sedeHtml).toContain('disabled')

    const puertoHtml = await renderView(PuertoRegistrationForm, {
      puertoToEdit: { idPuerto: 2, codigoPuerto: 'PECLL', nombre: 'Callao', pais: 'Perú', tipo: 'MARITIMO', estado: 'ACTIVO' }
    })
    expect(puertoHtml).toContain('sga-form-header')
    expect(puertoHtml).toContain('PECLL')

    const tipoCargaHtml = await renderView(TipoCargaRegistrationForm, {
      tipoToEdit: { idTipoCarga: 3, codigo: 'IMP-FCL', nombre: 'Contenedor FCL', estado: 'ACTIVO' }
    })
    expect(tipoCargaHtml).toContain('sga-form-header')
    expect(tipoCargaHtml).not.toContain('tipoContenedorSugerido')
    expect(tipoCargaHtml).not.toContain('Tara Sugerida')

    const mercHtml = await renderView(MercanciaFormModal, {
      visible: true,
      mercanciaToEdit: { idMercancia: 4, codigo: '3004.90', nombre: 'Fármaco', categoria: 'FARMACIA', requierePermiso: false, arancelBasePct: 0, estado: 'ACTIVO' }
    })
    expect(mercHtml).toContain('sga-dialog-body')
    expect(mercHtml).toContain('3004.90')
    expect(mercHtml).toContain('disabled')
  })
})
