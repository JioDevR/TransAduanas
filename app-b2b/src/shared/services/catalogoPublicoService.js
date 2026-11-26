import apiClient from '@/core/api/apiClient'

const cache = new Map()

async function cachedGet(key, url, params = {}) {
  if (cache.has(key)) return cache.get(key)
  const promise = apiClient
    .get(url, { params })
    .then((res) => (Array.isArray(res.data?.data) ? res.data.data : []))
    .catch((err) => {
      cache.delete(key)
      throw err
    })
  cache.set(key, promise)
  return promise
}

export const catalogoPublicoService = {
  obtenerBancos(uso) {
    return cachedGet(`bancos:${uso || 'ALL'}`, '/catalogos/bancos', uso ? { uso } : {})
  },

  obtenerCuentasRecaudadoras(uso = 'B2B', moneda) {
    const params = {}
    if (uso) params.uso = uso
    if (moneda) params.moneda = moneda
    return cachedGet(`cuentas:${uso || 'ALL'}:${moneda || 'ALL'}`, '/catalogos/cuentas-recaudadoras', params)
  },

  obtenerMetodosPago(uso = 'B2B') {
    return cachedGet(`metodos:${uso || 'ALL'}`, '/catalogos/metodos-pago', uso ? { uso } : {})
  },

  obtenerCategoriasDocumento() {
    return cachedGet('cat-docs', '/catalogos/categorias-documento')
  },

  obtenerUbigeo() {
    return cachedGet('ubigeo', '/catalogos/ubigeo')
  },

  clearCache() {
    cache.clear()
  }
}

export default catalogoPublicoService
