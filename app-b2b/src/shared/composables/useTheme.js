import { ref } from 'vue'

const STORAGE_KEY = 'sga-portal-theme'
const isDark = ref(false)

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

export function initTheme() {
  if (typeof window === 'undefined') return
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'dark') {
    applyTheme(true)
  } else if (saved === 'light') {
    applyTheme(false)
  } else {
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
    applyTheme(prefersDark)
  }
}

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
