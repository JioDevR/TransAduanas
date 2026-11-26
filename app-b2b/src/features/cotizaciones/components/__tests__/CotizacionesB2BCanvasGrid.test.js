import { describe, it, expect } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import PrimeVue from 'primevue/config'
import CotizacionesB2BCanvasGrid from '../CotizacionesB2BCanvasGrid.vue'

describe('CotizacionesB2BCanvasGrid', () => {
  it('renderiza el botón para descargar proforma PDF junto a las acciones de cada cotización', async () => {
    const app = createSSRApp({
      render() {
        return h(CotizacionesB2BCanvasGrid, {
          loading: false,
          cotizaciones: [
            {
              idCotizacion: 1,
              codigoCotizacion: 'COT-2026-0001',
              nombreMercancia: 'Maquinaria',
              codigoMercancia: '8429.51.00',
              regimenAduanero: 'IMPORTACION_DEFINITIVA',
              nombrePuertoEnvio: 'Shanghai',
              nombrePuertoDestino: 'Callao',
              valorFob: 50000,
              totalCotizado: 12500,
              estado: 'APROBADA',
              estadoPago: 'PENDIENTE',
              idUsuarioComercial: 4
            }
          ]
        })
      }
    })
    app.use(PrimeVue)

    const html = await renderToString(app)
    expect(html).toContain('COT-2026-0001')
    expect(html).toContain('Descargar proforma PDF COT-2026-0001')
  })
})
