import apiClient from '@/core/api/apiClient'

export const auditoriaService = {
  /**
   * Lista eventos de auditoría TI paginados con filtros opcionales
   * @param {Object} params { buscar, modulo, resultado, fechaDesde, fechaHasta, page, size }
   */
  async listarEventos(params = {}) {
    const cleanParams = {}
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        cleanParams[key] = val
      }
    })
    const response = await apiClient.get('/admin/auditoria', { params: cleanParams })
    return response.data.data
  },

  /**
   * Descarga el reporte PDF de auditoría TI con los filtros aplicados
   */
  async downloadPdf(params = {}) {
    const cleanParams = Object.fromEntries(
      Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined)
    )
    const response = await apiClient.get('/admin/auditoria/exportar-pdf', {
      params: cleanParams,
      responseType: 'blob'
    })
    return response.data
  }
}

export default auditoriaService
