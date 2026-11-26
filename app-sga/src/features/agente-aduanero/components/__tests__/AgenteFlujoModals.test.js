import { describe, it, expect } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import PrimeVue from 'primevue/config'

import CanalRojoCitaModal from '../CanalRojoCitaModal.vue'
import CanalNaranjaSunatModal from '../CanalNaranjaSunatModal.vue'
import InformeConformidadModal from '../InformeConformidadModal.vue'
import TransitoOperacionModal from '../TransitoOperacionModal.vue'
import DiligenciaCampoModal from '@/features/aduanas/components/DiligenciaCampoModal.vue'
import SolicitudTransporteModal from '@/features/aduanas/components/SolicitudTransporteModal.vue'
import RetiroDirectoModal from '@/features/aduanas/components/RetiroDirectoModal.vue'
import IngresoAlmacenModal from '@/features/almacen/components/IngresoAlmacenModal.vue'
import SolicitudRecojoExportacionModal from '@/features/aduanas/components/SolicitudRecojoExportacionModal.vue'
import ConfirmarIngresoOrigenModal from '@/features/aduanas/components/ConfirmarIngresoOrigenModal.vue'
import ConfirmarEmbarqueExportacionModal from '@/features/aduanas/components/ConfirmarEmbarqueExportacionModal.vue'
import RecepcionDestinoExteriorModal from '@/features/aduanas/components/RecepcionDestinoExteriorModal.vue'
import CierreExpedienteModal from '@/features/aduanas/components/CierreExpedienteModal.vue'

const sampleImportacion = {
  idExpediente: 101,
  codigoExpediente: 'EXP-2026-0101',
  clienteRazonSocial: 'Importadora Pacífico SAC',
  regimenAduanero: 'IMPORTACION_DEFINITIVA',
  modalidad: 'MARITIMO',
  estadoExpediente: 'CANAL_ROJO',
  canalSunat: 'ROJO',
  numeroDam: '118-2026-10-001920',
  numeroContenedor: 'MSKU9021840'
}

const sampleExportacion = {
  idExpediente: 202,
  codigoExpediente: 'EXP-2026-0202',
  clienteRazonSocial: 'Agroexportadora del Sur SAC',
  regimenAduanero: 'EXPORTACION_DEFINITIVA',
  modalidad: 'MARITIMO',
  estadoExpediente: 'EMBARQUE_PENDIENTE',
  canalSunat: 'VERDE',
  numeroDam: '118-2026-40-004120'
}

const renderModalSSR = async (Component, props) => {
  const app = createSSRApp({
    render: () => h(Component, props)
  })
  app.use(PrimeVue)
  return renderToString(app)
}

describe('Diálogos del Flujo Operativo del Agente Aduanero (sgaUI)', () => {
  const modalCases = [
    ['CanalRojoCitaModal', CanalRojoCitaModal, { visible: true, expediente: sampleImportacion }],
    ['CanalNaranjaSunatModal', CanalNaranjaSunatModal, { visible: true, expediente: sampleImportacion }],
    ['InformeConformidadModal', InformeConformidadModal, { visible: true, expediente: sampleImportacion }],
    ['TransitoOperacionModal', TransitoOperacionModal, { visible: true, expediente: sampleImportacion, initialMode: 'transito' }],
    ['DiligenciaCampoModal', DiligenciaCampoModal, { visible: true, expediente: sampleImportacion }],
    ['SolicitudTransporteModal', SolicitudTransporteModal, { visible: true, expediente: sampleImportacion }],
    ['RetiroDirectoModal', RetiroDirectoModal, { visible: true, expediente: sampleImportacion }],
    ['IngresoAlmacenModal', IngresoAlmacenModal, { visible: true, expediente: sampleImportacion }],
    ['SolicitudRecojoExportacionModal', SolicitudRecojoExportacionModal, { visible: true, expediente: sampleExportacion }],
    ['ConfirmarIngresoOrigenModal', ConfirmarIngresoOrigenModal, { visible: true, expediente: sampleExportacion }],
    ['ConfirmarEmbarqueExportacionModal', ConfirmarEmbarqueExportacionModal, { visible: true, expediente: sampleExportacion }],
    ['RecepcionDestinoExteriorModal', RecepcionDestinoExteriorModal, { visible: true, expediente: sampleExportacion }],
    ['CierreExpedienteModal', CierreExpedienteModal, { visible: true, expediente: sampleImportacion }]
  ]

  it.each(modalCases)('%s se exporta y renderiza en SSR sin errores con expediente válido', async (_name, Component, props) => {
    expect(Component).toBeDefined()
    const html = await renderModalSSR(Component, props)
    expect(html).toBeTypeOf('string')
  })

  it.each(modalCases)('%s renderiza sin errores cuando expediente es null', async (_name, Component) => {
    const html = await renderModalSSR(Component, { visible: false, expediente: null })
    expect(html).toBeTypeOf('string')
  })
})
