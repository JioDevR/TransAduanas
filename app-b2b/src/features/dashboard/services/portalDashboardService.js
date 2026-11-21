import apiClient from '@/core/api/apiClient'
import { portalCotizacionService } from '@/features/cotizaciones/services/portalCotizacionService'
import { portalExpedienteService } from '@/features/expedientes/services/portalExpedienteService'
import { portalPagoService } from '@/features/pagos/services/portalPagoService'

/**
 * Servicio agregador para el Dashboard Ejecutivo del Portal B2B.
 */
export const portalDashboardService = {
  /**
   * Obtiene el perfil empresarial del cliente autenticado.
   * @returns {Promise<Object|null>}
   */
  async obtenerPerfil() {
    const response = await apiClient.get('/portal/perfil')
    return response.data?.data || null
  },

  /**
   * Carga en paralelo todas las fuentes de datos del dashboard con tolerancia a fallos parciales.
   */
  async cargarDatosDashboard() {
    const [
      perfilRes,
      cotizacionesRes,
      expedientesRes,
      resumenPagosRes,
      obligacionesRes,
      anticiposRes
    ] = await Promise.allSettled([
      this.obtenerPerfil(),
      portalCotizacionService.listarCotizaciones(),
      portalExpedienteService.listarExpedientes(),
      portalPagoService.obtenerResumen(),
      portalPagoService.listarObligaciones(),
      portalPagoService.listarAnticipos()
    ])

    const errors = {}
    if (perfilRes.status === 'rejected') errors.perfil = true
    if (cotizacionesRes.status === 'rejected') errors.cotizaciones = true
    if (expedientesRes.status === 'rejected') errors.expedientes = true
    if (
      resumenPagosRes.status === 'rejected' &&
      obligacionesRes.status === 'rejected' &&
      anticiposRes.status === 'rejected'
    ) {
      errors.pagos = true
    }

    return {
      perfil: perfilRes.status === 'fulfilled' ? perfilRes.value : null,
      cotizaciones: cotizacionesRes.status === 'fulfilled' ? (cotizacionesRes.value || []) : [],
      expedientes: expedientesRes.status === 'fulfilled' ? (expedientesRes.value || []) : [],
      resumenPagos: resumenPagosRes.status === 'fulfilled' ? resumenPagosRes.value : null,
      obligaciones: obligacionesRes.status === 'fulfilled' ? (obligacionesRes.value || []) : [],
      anticipos: anticiposRes.status === 'fulfilled' ? (anticiposRes.value || []) : [],
      errors
    }
  }
}

export default portalDashboardService
