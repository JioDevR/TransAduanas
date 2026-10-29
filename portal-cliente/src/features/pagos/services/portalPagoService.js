import apiClient from '@/core/api/apiClient'

/**
 * Servicio de dominio para Cobros, Pagos y Conciliación Financiera en el Portal del Cliente.
 */
export const portalPagoService = {
  /**
   * Obtiene el resumen financiero consolidado (saldos pendientes, anticipos, liquidaciones).
   */
  async obtenerResumen() {
    const response = await apiClient.get('/portal/pagos/resumen')
    return response.data?.data || null
  },

  /**
   * Lista las obligaciones financieras pendientes de pago.
   */
  async listarObligaciones() {
    const response = await apiClient.get('/portal/pagos/obligaciones-pendientes')
    return response.data?.data || []
  },

  /**
   * Lista el historial de anticipos y vouchers registrados.
   */
  async listarAnticipos() {
    const response = await apiClient.get('/portal/pagos/anticipos')
    return response.data?.data || []
  },

  /**
   * Lista las liquidaciones finales de despacho aduanero.
   */
  async listarLiquidaciones() {
    const response = await apiClient.get('/portal/pagos/liquidaciones')
    return response.data?.data || []
  },

  /**
   * Sube un archivo de voucher bancario / comprobante de pago.
   * @param {File|Blob} file
   */
  async subirComprobante(file) {
    const formData = new FormData()
    formData.append('file', file)
    const response = await apiClient.post('/portal/pagos/upload-comprobante', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data?.data || response.data
  },

  /**
   * Registra un pago / voucher bancario. Soporta tanto payload JSON como FormData con archivo.
   * @param {Object|FormData} payload
   */
  async registrarPago(payload) {
    // Si viene como FormData con archivo
    if (payload instanceof FormData) {
      const file = payload.get('file') || payload.get('comprobante')
      let idDocumentoComprobante = payload.get('idDocumentoComprobante')

      if (file && file instanceof File && !idDocumentoComprobante) {
        const docRes = await this.subirComprobante(file)
        idDocumentoComprobante = docRes?.idDocumento || docRes?.id
      }

      const jsonPayload = {
        tipoObligacion: payload.get('tipoObligacion'),
        idReferenciaObligacion: payload.get('idReferenciaObligacion') ? Number(payload.get('idReferenciaObligacion')) : null,
        monto: payload.get('monto') ? Number(payload.get('monto')) : null,
        moneda: payload.get('moneda') || 'USD',
        fechaPago: payload.get('fechaPago'),
        bancoOrigen: payload.get('bancoOrigen'),
        numeroOperacion: payload.get('numeroOperacion'),
        idDocumentoComprobante: idDocumentoComprobante ? Number(idDocumentoComprobante) : null,
        observaciones: payload.get('observaciones') || ''
      }
      return this.registrarVoucher(jsonPayload)
    }

    return this.registrarVoucher(payload)
  },

  /**
   * Envía el payload JSON de registro de voucher bancario a revisión contable.
   * @param {Object} payload
   */
  async registrarVoucher(payload) {
    try {
      const response = await apiClient.post('/portal/pagos/registrar-voucher', payload)
      return response.data?.data || response.data
    } catch (err) {
      if (err.response?.status === 404) {
        // Fallback a ruta alternativa si estuviera configurada
        const response = await apiClient.post('/portal/pagos/registrar', payload)
        return response.data?.data || response.data
      }
      throw err
    }
  },

  /**
   * Corrige un voucher observado por Finanzas.
   * @param {number|string} idPago - ID del pago o anticipo a subsanar
   * @param {Object|FormData} payload
   */
  async corregirPago(idPago, payload) {
    let jsonPayload = payload

    if (payload instanceof FormData) {
      const file = payload.get('file') || payload.get('comprobante')
      let idDocumentoComprobante = payload.get('idDocumentoComprobante')

      if (file && file instanceof File) {
        const docRes = await this.subirComprobante(file)
        idDocumentoComprobante = docRes?.idDocumento || docRes?.id
      }

      jsonPayload = {
        monto: payload.get('monto') ? Number(payload.get('monto')) : null,
        fechaPago: payload.get('fechaPago'),
        bancoOrigen: payload.get('bancoOrigen'),
        numeroOperacion: payload.get('numeroOperacion'),
        idDocumentoComprobante: idDocumentoComprobante ? Number(idDocumentoComprobante) : null,
        sustentoSubsanacion: payload.get('sustentoSubsanacion') || payload.get('observaciones') || ''
      }
    }

    const response = await apiClient.put(`/portal/pagos/anticipos/${idPago}/corregir`, jsonPayload)
    return response.data?.data || response.data
  },

  /**
   * Descarga el archivo binario del comprobante de pago.
   * @param {number|string} idDocumento
   */
  async descargarComprobante(idDocumento) {
    const response = await apiClient.get(`/documentos/${idDocumento}/descargar`, {
      responseType: 'blob'
    })
    return response.data
  },

  /**
   * Previsualiza el comprobante de pago en una pestaña o visor.
   * @param {number|string} idDocumento
   */
  async previsualizarComprobante(idDocumento) {
    const response = await apiClient.get(`/documentos/${idDocumento}/preview`, {
      responseType: 'blob'
    })
    return response.data
  }
}

export default portalPagoService
