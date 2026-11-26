import { describe, it, expect } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import PrimeVue from 'primevue/config'
import CanalLevanteModal from '../CanalLevanteModal.vue'

describe('CanalLevanteModal (sgaUI)', () => {
  const sampleExp = {
    idExpediente: 5,
    codigoExpediente: 'EXP-2026-0005',
    clienteRazonSocial: 'Importadora Pacífico SAC',
    numeroDam: '',
    canalSunat: 'PENDIENTE'
  }

  it('renderiza el modo asignación de canal SUNAT sin errores SSR', async () => {
    const app = createSSRApp({
      render: () =>
        h(CanalLevanteModal, {
          visible: true,
          expediente: sampleExp,
          actionType: 'canal'
        })
    })
    app.use(PrimeVue)

    const html = await renderToString(app)
    expect(html).toBeTypeOf('string')
  })

  it('exporta CanalLevanteModal correctamente', () => {
    expect(CanalLevanteModal).toBeDefined()
    expect(typeof CanalLevanteModal).toBe('object')
  })
})
