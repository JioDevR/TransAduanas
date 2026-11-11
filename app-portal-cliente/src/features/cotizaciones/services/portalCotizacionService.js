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
  },

  /**
   * Sube secuencialmente una lista de documentos adjuntos asociados a una cotización,
   * capturando fallos individuales sin abortar el resto del lote.
   * @param {number|string} idCotizacion - ID de la cotización
   * @param {Array<{ file: File, categoria?: string, descripcion?: string }>} queuedDocs - Lista de documentos
   * @returns {Promise<{ uploaded: Array, failed: Array<{ name: string, error: string }> }>}
   */
  async subirDocumentosCotizacion(idCotizacion, queuedDocs = []) {
    const uploaded = []
    const failed = []

    if (!queuedDocs || !Array.isArray(queuedDocs) || queuedDocs.length === 0) {
      return { uploaded, failed }
    }

    for (const doc of queuedDocs) {
      const file = doc.file || doc
      const fileName = file?.name || doc?.name || 'documento'

      try {
        const formData = new FormData()
        formData.append('files', file)
        formData.append('categoria', doc.categoria || 'GENERAL')
        formData.append('descripcion', doc.descripcion || '')
        formData.append('entidadTipo', 'COTIZACION')
        formData.append('idEntidad', String(idCotizacion))

        const response = await apiClient.post('/documentos/upload', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })

        const savedDocs = response.data?.data || response.data
        if (Array.isArray(savedDocs)) {
          uploaded.push(...savedDocs)
        } else if (savedDocs) {
          uploaded.push(savedDocs)
        }
      } catch (err) {
        failed.push({
          name: fileName,
          error: err.response?.data?.message || err.message || 'Error al subir el archivo'
        })
      }
    }

    return { uploaded, failed }
  },

  /**
   * Obtiene la lista de documentos adjuntos registrados para una cotización.
   * @param {number|string} idCotizacion - ID de la cotización
   * @returns {Promise<Array>}
   */
  async listarDocumentosCotizacion(idCotizacion) {
    const response = await apiClient.get('/documentos', {
      params: {
        entidadTipo: 'COTIZACION',
        idCotizacion
      }
    })
    return response.data?.data || []
  },

  /**
   * Descarga el archivo binario de un documento adjunto.
   * @param {number|string} idDocumento - ID del documento
   * @returns {Promise<Blob>}
   */
  async descargarDocumento(idDocumento) {
    const response = await apiClient.get(`/documentos/${idDocumento}/descargar`, {
      responseType: 'blob'
    })
    return response.data
  },

  /**
   * Obtiene el binario de un documento para su previsualización inline en navegador.
   * @param {number|string} idDocumento - ID del documento
   * @returns {Promise<Blob>}
   */
  async previsualizarDocumento(idDocumento) {
    const response = await apiClient.get(`/documentos/${idDocumento}/preview`, {
      responseType: 'blob'
    })
    return response.data
  }
}

export default portalCotizacionService
