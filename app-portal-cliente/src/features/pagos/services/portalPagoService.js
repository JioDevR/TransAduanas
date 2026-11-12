import apiClient from '@/core/api/apiClient'
import {
  descargarDocumentoBlob,
  previsualizarDocumentoBlob
} from '@/features/cotizaciones/services/portalCotizacionService'

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
   * Obtiene los datos de un pago/anticipo específico por su ID.
   * @param {number|string} idPago
   */
  async obtenerPagoPorId(idPago) {
    const anticipos = await this.listarAnticipos()
    return anticipos.find(p => p.idPago === Number(idPago)) || null
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
   * Envía el payload JSON de registro de voucher bancario a revisión contable.
   * Alineado con PortalRegistroVoucherDTO en backend.
   * @param {Object} payload
   */
  async registrarVoucher(payload) {
    const jsonPayload = {
      tipoObligacion: payload.tipoObligacion,
      idReferencia: payload.idReferencia != null ? Number(payload.idReferencia) : (payload.idReferenciaObligacion != null ? Number(payload.idReferenciaObligacion) : null),
      montoAbonado: payload.montoAbonado != null ? Number(payload.montoAbonado) : (payload.monto != null ? Number(payload.monto) : null),
      moneda: payload.moneda || 'USD',
      metodoPago: payload.metodoPago || 'TRANSFERENCIA_BANCARIA',
      bancoOrigen: payload.bancoOrigen || '',
      bancoDestino: payload.bancoDestino || 'BCP Dolares TransAduanas',
      numeroOperacion: payload.numeroOperacion || '',
      fechaOperacion: payload.fechaOperacion || payload.fechaPago,
      observaciones: payload.observaciones || '',
      idDocumentoComprobante: payload.idDocumentoComprobante != null ? Number(payload.idDocumentoComprobante) : null
    }

    try {
      const response = await apiClient.post('/portal/pagos/registrar-voucher', jsonPayload)
      return response.data?.data || response.data
    } catch (err) {
      if (err.response?.status === 404) {
        // Fallback a ruta alternativa si estuviera configurada
        const response = await apiClient.post('/portal/pagos/registrar', jsonPayload)
        return response.data?.data || response.data
      }
      throw err
    }
  },

  /**
   * Corrige un voucher observado por Finanzas.
   * Alineado con PortalCorreccionPagoDTO en backend.
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
        montoAbonado: payload.get('montoAbonado') ? Number(payload.get('montoAbonado')) : (payload.get('monto') ? Number(payload.get('monto')) : null),
        moneda: payload.get('moneda') || 'USD',
        metodoPago: payload.get('metodoPago') || 'TRANSFERENCIA_BANCARIA',
        bancoOrigen: payload.get('bancoOrigen') || '',
        bancoDestino: payload.get('bancoDestino') || 'BCP Dolares TransAduanas',
        numeroOperacion: payload.get('numeroOperacion') || '',
        fechaOperacion: payload.get('fechaOperacion') || payload.get('fechaPago'),
        idDocumentoComprobante: idDocumentoComprobante ? Number(idDocumentoComprobante) : null,
        observaciones: payload.get('observaciones') || payload.get('sustentoSubsanacion') || ''
      }
    } else if (payload && typeof payload === 'object') {
      jsonPayload = {
        montoAbonado: payload.montoAbonado != null ? Number(payload.montoAbonado) : (payload.monto != null ? Number(payload.monto) : null),
        moneda: payload.moneda || 'USD',
        metodoPago: payload.metodoPago || 'TRANSFERENCIA_BANCARIA',
        bancoOrigen: payload.bancoOrigen || '',
        bancoDestino: payload.bancoDestino || 'BCP Dolares TransAduanas',
        numeroOperacion: payload.numeroOperacion || '',
        fechaOperacion: payload.fechaOperacion || payload.fechaPago,
        idDocumentoComprobante: payload.idDocumentoComprobante != null ? Number(payload.idDocumentoComprobante) : null,
        observaciones: payload.observaciones ?? payload.sustentoSubsanacion ?? ''
      }
    }

    const response = await apiClient.put(`/portal/pagos/anticipos/${idPago}/corregir`, jsonPayload)
    return response.data?.data || response.data
  },

  /**
   * Descarga el archivo binario del comprobante de pago.
   * @param {number|string} idDocumento
   */
  descargarComprobante: descargarDocumentoBlob,

  /**
   * Previsualiza el comprobante de pago en una pestaña o visor.
   * @param {number|string} idDocumento
   */
  previsualizarComprobante: previsualizarDocumentoBlob
}

export default portalPagoService
