import apiClient from '@/core/api/apiClient'

export const finanzasService = {
  /**
   * Listar todas las liquidaciones financieras con filtros
   * @param {Object} params { soloPendientes, estado }
   */
  async getLiquidaciones(params = {}) {
    const response = await apiClient.get('/finanzas/liquidaciones', { params })
    return response.data.data
  },

  /**
   * Obtener métricas y KPIs consolidados para el dashboard
   */
  async getMetricas() {
    const response = await apiClient.get('/finanzas/liquidaciones/metricas')
    return response.data.data
  },

  /**
   * Pre-calcular estimados a partir de un expediente aduanero
   * @param {Number} idExpediente
   */
  async calcularEstimado(idExpediente) {
    const response = await apiClient.get(`/finanzas/liquidaciones/calcular-estimado/${idExpediente}`)
    return response.data.data
  },

  /**
   * Obtener liquidación por ID
   * @param {Number} id
   */
  async getLiquidacionById(id) {
    const response = await apiClient.get(`/finanzas/liquidaciones/${id}`)
    return response.data.data
  },

  /**
   * Buscar por código correlativo (ej. LIQ-2026-0001)
   * @param {String} codigo
   */
  async getLiquidacionByCodigo(codigo) {
    const response = await apiClient.get(`/finanzas/liquidaciones/codigo/${codigo}`)
    return response.data.data
  },

  /**
   * Buscar liquidación vinculada a un expediente
   * @param {Number} idExpediente
   */
  async getLiquidacionByExpediente(idExpediente) {
    const response = await apiClient.get(`/finanzas/liquidaciones/expediente/${idExpediente}`)
    return response.data.data
  },

  /**
   * Emitir nueva liquidación financiera
   * @param {Object} data LiquidacionFinancieraRequestDTO
   */
  async emitirLiquidacion(data) {
    const response = await apiClient.post('/finanzas/liquidaciones', data)
    return response.data.data
  },

  /**
   * Actualizar liquidación pendiente
   * @param {Number} id
   * @param {Object} data LiquidacionFinancieraRequestDTO
   */
  async updateLiquidacion(id, data) {
    const response = await apiClient.put(`/finanzas/liquidaciones/${id}`, data)
    return response.data.data
  },

  /**
   * Registrar cobro / pago bancario
   * @param {Number} id
   * @param {Object} data LiquidacionPagoDTO { metodoPago, bancoDestino, numeroOperacion, numeroFactura, idDocumentoVoucher, fechaPago, observaciones }
   */
  async registrarPago(id, data) {
    const response = await apiClient.patch(`/finanzas/liquidaciones/${id}/pago`, data)
    return response.data.data
  },

  /**
   * Anular liquidación financiera (baja lógica)
   * @param {Number} id
   */
  async anularLiquidacion(id) {
    const response = await apiClient.delete(`/finanzas/liquidaciones/${id}`)
    return response.data
  },

  // ==========================================
  // VALIDACIÓN DE ANTICIPOS Y PAGOS PRE-EXPEDIENTE
  // ==========================================
  async getAnticipos(params = {}) {
    const response = await apiClient.get('/finanzas/anticipos', { params })
    return response.data.data
  },

  async getAnticiposPendientes() {
    const response = await apiClient.get('/finanzas/anticipos/pendientes')
    return response.data.data
  },

  async getAnticipoById(idPago) {
    const response = await apiClient.get(`/finanzas/anticipos/${idPago}`)
    return response.data.data
  },

  async validarAnticipo(idPago, payload) {
    const response = await apiClient.patch(`/finanzas/anticipos/${idPago}/validar`, payload)
    return response.data.data
  }
}

export default finanzasService
