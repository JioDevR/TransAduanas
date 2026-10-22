import { catalogoService } from './catalogoService'

/**
 * mercanciaService consolidado: delega en catalogoService normalizando la respuesta.
 */
export const mercanciaService = {
  async getMercancias(params = {}) {
    const res = await catalogoService.getMercancias(params)
    return res?.data ?? res
  },

  async getMercanciaById(id) {
    const res = await catalogoService.getMercanciaById(id)
    return res?.data ?? res
  },

  async getMercanciaByCodigo(codigo) {
    const res = await catalogoService.getMercanciaByCodigo(codigo)
    return res?.data ?? res
  },

  async createMercancia(data) {
    const res = await catalogoService.createMercancia(data)
    return res?.data ?? res
  },

  async updateMercancia(id, data) {
    const res = await catalogoService.updateMercancia(id, data)
    return res?.data ?? res
  },

  async deleteMercancia(id) {
    return catalogoService.deleteMercancia(id)
  },

  async reactivarMercancia(id) {
    return catalogoService.reactivarMercancia(id)
  }
}

export default mercanciaService
