import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/core/api/apiClient'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('sga_portal_token') || null)
  const user = ref(JSON.parse(localStorage.getItem('sga_portal_user') || 'null'))
  const loading = ref(false)
  const error = ref(null)

  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const empresaNombre = computed(() => user.value?.razonSocial || 'Empresa Cliente')
  const ruc = computed(() => user.value?.ruc || '')
  const usuarioNombre = computed(() => {
    if (!user.value) return ''
    return user.value.contactoRepresentante || user.value.nombres || user.value.username
  })

  async function login(usernameOrRuc, password) {
    loading.value = true
    error.value = null
    try {
      const response = await apiClient.post('/portal/auth/login', {
        username: usernameOrRuc.trim(),
        password
      })

      const data = response.data?.data
      if (!data || !data.token) {
        throw new Error('Respuesta de autenticación inválida')
      }

      token.value = data.token
      user.value = {
        idUsuarioCliente: data.idUsuarioCliente,
        username: data.username,
        contactoRepresentante: data.contactoRepresentante || data.nombres,
        nombres: data.nombres,
        correoElectronico: data.correoElectronico,
        idCliente: data.idCliente,
        ruc: data.ruc,
        razonSocial: data.razonSocial,
        debeCambiarPassword: data.debeCambiarPassword
      }

      localStorage.setItem('sga_portal_token', data.token)
      localStorage.setItem('sga_portal_user', JSON.stringify(user.value))

      return { success: true }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Error al iniciar sesión'
      error.value = msg
      return { success: false, message: msg }
    } finally {
      loading.value = false
    }
  }

  async function fetchPerfil() {
    if (!token.value) return
    try {
      const response = await apiClient.get('/portal/perfil')
      if (response.data?.data) {
        const perfil = response.data.data
        user.value = { ...user.value, ...perfil }
        localStorage.setItem('sga_portal_user', JSON.stringify(user.value))
      }
    } catch (err) {
      console.error('Error al actualizar perfil de cliente', err)
    }
  }

  async function cambiarPassword(passwordActual, nuevaPassword) {
    loading.value = true
    error.value = null
    try {
      const response = await apiClient.post('/portal/auth/cambiar-password', {
        passwordActual,
        nuevaPassword
      })
      if (user.value) {
        user.value.debeCambiarPassword = false
        localStorage.setItem('sga_portal_user', JSON.stringify(user.value))
      }
      return { success: true, message: response.data?.message || 'Contraseña actualizada' }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Error al actualizar contraseña'
      error.value = msg
      return { success: false, message: msg }
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('sga_portal_token')
    localStorage.removeItem('sga_portal_user')
  }

  return {
    token,
    user,
    loading,
    error,
    isAuthenticated,
    empresaNombre,
    ruc,
    usuarioNombre,
    login,
    fetchPerfil,
    cambiarPassword,
    logout
  }
})
