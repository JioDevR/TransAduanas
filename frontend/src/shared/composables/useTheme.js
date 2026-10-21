import { ref, onMounted } from 'vue'

const STORAGE_KEY = 'sga-theme'
const isDark = ref(false)

/**
 * Aplica el tema en el elemento raíz <html> y persiste en localStorage.
 */
function applyTheme(dark) {
  isDark.value = dark
  if (typeof document !== 'undefined') {
    if (dark) {
      document.documentElement.classList.add('dark-theme')
    } else {
      document.documentElement.classList.remove('dark-theme')
    }
  }
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light')
  }
}

/**
 * Inicializa el tema leyendo la preferencia guardada o la del sistema operativo.
 */
export function initTheme() {
  if (typeof window === 'undefined') return
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'dark') {
    applyTheme(true)
  } else if (saved === 'light') {
    applyTheme(false)
  } else {
    // Si no hay preferencia guardada, verificar el sistema
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
    applyTheme(prefersDark)
  }
}

/**
 * Composable para alternar y consultar el modo claro / modo oscuro en todo el SGA.
 */
export function useTheme() {
  const toggleTheme = () => {
    applyTheme(!isDark.value)
  }

  const setTheme = (theme) => {
    applyTheme(theme === 'dark')
  }

  return {
    isDark,
    toggleTheme,
    setTheme
  }
}

export default useTheme
