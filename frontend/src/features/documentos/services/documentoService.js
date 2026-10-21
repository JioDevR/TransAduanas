import apiClient from '@/core/api/apiClient'

export const documentoService = {
  /**
   * Subir uno o varios documentos
   * @param {File[]} files Lista de archivos
   * @param {Object} metadata { categoria, descripcion, entidadTipo, idEntidad, idUsuario }
   */
  async upload(files, { categoria = 'GENERAL', descripcion = '', entidadTipo = 'GENERAL', idEntidad = null, idUsuario = null } = {}) {
    const formData = new FormData()

    for (const file of files) {
      formData.append('files', file)
    }

    if (categoria) formData.append('categoria', categoria)
    if (descripcion) formData.append('descripcion', descripcion)
    if (entidadTipo) formData.append('entidadTipo', entidadTipo)
    if (idEntidad) formData.append('idEntidad', idEntidad)
    if (idUsuario) formData.append('idUsuario', idUsuario)

    const response = await apiClient.post('/documentos/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data.data
  },

  /**
   * Listar documentos con filtros
   * @param {Object} params { entidadTipo, idCotizacion, idExpediente, idCliente, categoria }
   */
  async getDocumentos(params = {}) {
    const response = await apiClient.get('/documentos', { params })
    return response.data.data
  },

  /**
   * Obtener metadatos de un documento por ID
   */
  async getDocumentoById(id) {
    const response = await apiClient.get(`/documentos/${id}`)
    return response.data.data
  },

  /**
   * Descargar archivo binario
   */
  async downloadDocumento(id, nombreOriginal = 'documento') {
    const response = await apiClient.get(`/documentos/${id}/descargar`, {
      responseType: 'blob'
    })
    const blob = new Blob([response.data], { type: response.headers['content-type'] })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = nombreOriginal
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(link.href)
  },

  /**
   * Obtener Blob URL para previsualización inline en navegador
   */
  async getPreviewBlobUrl(id) {
    const response = await apiClient.get(`/documentos/${id}/preview`, {
      responseType: 'blob'
    })
    const mimeType = response.headers['content-type'] || 'application/pdf'
    const blob = new Blob([response.data], { type: mimeType })
    return {
      url: window.URL.createObjectURL(blob),
      mimeType
    }
  },

  /**
   * Anular / Eliminar documento
   */
  async deleteDocumento(id) {
    const response = await apiClient.delete(`/documentos/${id}`)
    return response.data.data
  }
}

export default documentoService
