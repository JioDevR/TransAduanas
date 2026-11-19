import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json'
  },
  timeout: 30000
})

// Interceptor de petición para adjuntar token JWT
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sga_portal_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Interceptor de respuesta para manejar 401 / 403
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('sga_portal_token')
      localStorage.removeItem('sga_portal_user')

      const currentPath = window.location.pathname
      const isAlreadyOnLogin = currentPath === '/login' || currentPath.endsWith('/login')

      if (!isAlreadyOnLogin) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
