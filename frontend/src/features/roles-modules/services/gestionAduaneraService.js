import { expedienteService } from './expedienteService'
import { comercialService } from './comercialService'

export const gestionAduaneraService = {
  // ==========================================
  // BANDEJA DE AUDITORÍA Y COTIZACIONES (Consolidados via comercialService)
  // ==========================================
  getCotizaciones(params = {}) {
    return comercialService.getCotizaciones(params)
  },

  getCotizacionById(id) {
    return comercialService.getCotizacionById(id)
  },

  revisarCotizacion(id, payload) {
    return comercialService.revisarCotizacion(id, payload)
  },

  cambiarEstadoCotizacion(id, { estado, motivoObservacion = '' }) {
    return comercialService.updateCotizacionEstado(id, { estado, motivoObservacion })
  },

  // ==========================================
  // GESTIÓN DE EXPEDIENTES ADUANEROS (Consolidados via expedienteService)
  // ==========================================
  getExpedientes(params = {}) {
    return expedienteService.getExpedientes(params)
  },

  getExpedienteById(id) {
    return expedienteService.getExpedienteById(id)
  },

  getExpedienteByCotizacion(idCotizacion) {
    return expedienteService.getExpedienteByCotizacion(idCotizacion)
  },

  crearExpediente(data) {
    return expedienteService.createExpediente(data)
  },

  asignarCanal(id, payload) {
    return expedienteService.asignarCanal(id, payload)
  },

  autorizarLevante(id, payload) {
    return expedienteService.autorizarLevante(id, payload)
  }
}

export const supervisionService = gestionAduaneraService
export default gestionAduaneraService
