import axios from 'axios'

// Configuración base del cliente HTTP hacia el backend SGA
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8085/api/v1',
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 15000
})

// Interceptor de petición: Inyecta el token Bearer si existe
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sga_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

let isRedirecting = false

// Interceptor de respuesta: Manejo centralizado de 401 (no autenticado) y errores
apiClient.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response && error.response.status === 401 && !error.config?.url?.includes('/auth/login')) {
      // Token expirado o inválido: limpiar sesión y redirigir al login
      localStorage.removeItem('sga_token')
      localStorage.removeItem('sga_user')
      if (!isRedirecting && window.location.pathname !== '/login') {
        isRedirecting = true
        window.location.replace('/login')
      }
    }
    // Nota: Código 403 (Forbidden) indica falta de permisos sobre un recurso específico,
    // por lo que NO se desloguea al usuario para permitir manejo de notificación visual en vista.
    return Promise.reject(error)
  }
)

export default apiClient
