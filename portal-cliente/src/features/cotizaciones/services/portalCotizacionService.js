import apiClient from '@/core/api/apiClient'

/**
 * Servicio de dominio para el módulo de Cotizaciones del Portal del Cliente B2B.
 */
export const portalCotizacionService = {
  /**
   * Obtiene la lista de cotizaciones del cliente autenticado.
   */
  async listarCotizaciones() {
    const response = await apiClient.get('/portal/cotizaciones')
    return response.data?.data || []
  },

  /**
   * Obtiene el detalle técnico y desglose arancelario de una cotización específica.
   * @param {number|string} id - ID de la cotización
   */
  async obtenerDetalle(id) {
    const response = await apiClient.get(`/portal/cotizaciones/${id}`)
    return response.data?.data || null
  },

  /**
   * Envía una solicitud de nueva cotización logística a evaluación comercial.
   * @param {Object} payload - Datos de la solicitud
   */
  async solicitarCotizacion(payload) {
    const response = await apiClient.post('/portal/cotizaciones/solicitar', payload)
    return response.data?.data || response.data
  },

  /**
   * Acepta una cotización emitida para iniciar el proceso de despacho aduanero.
   * @param {number|string} id - ID de la cotización
   */
  async aceptarCotizacion(id) {
    try {
      const response = await apiClient.patch(`/portal/cotizaciones/${id}/aceptar`)
      return response.data?.data || response.data
    } catch (err) {
      // Fallback a POST por compatibilidad si el backend requiere POST
      if (err.response?.status === 405) {
        const response = await apiClient.post(`/portal/cotizaciones/${id}/aceptar`)
        return response.data?.data || response.data
      }
      throw err
    }
  },

  /**
   * Obtiene el catálogo de puertos aduaneros activos.
   */
  async obtenerPuertos(soloActivos = true) {
    const response = await apiClient.get('/catalogos/puertos', {
      params: { soloActivos }
    })
    return response.data?.data || []
  },

  /**
   * Obtiene el catálogo de mercancías / subpartidas arancelarias.
   */
  async obtenerMercancias(soloActivos = true) {
    const response = await apiClient.get('/catalogos/mercancias', {
      params: { soloActivos }
    })
    return response.data?.data || []
  },

  /**
   * Obtiene el catálogo de tipos de carga.
   */
  async obtenerTiposCarga(soloActivos = true) {
    const response = await apiClient.get('/catalogos/tipos-carga', {
      params: { soloActivos }
    })
    return response.data?.data || []
  }
}

export default portalCotizacionService
