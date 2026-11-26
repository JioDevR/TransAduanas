import { ref, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'

/**
 * Breakpoints oficiales del sistema de diseño sgaUI (px).
 * Deben mantenerse sincronizados con sgaUI/sga-responsive.css.
 */
export const SGA_BREAKPOINTS = Object.freeze({
  mobile: 640,
  tablet: 900,
  tabletLg: 1200
})

/**
 * Calcula el estado responsivo completo para un ancho de viewport dado.
 * Función pura reutilizable y testeable fuera del navegador.
 *
 * @param {number} width
 */
export function resolveViewportState(width = 1280) {
  const safeWidth = Number.isFinite(width) && width > 0 ? width : 1280
  const isMobile = safeWidth <= SGA_BREAKPOINTS.mobile
  const isTablet = safeWidth > SGA_BREAKPOINTS.mobile && safeWidth <= SGA_BREAKPOINTS.tablet
  const isTabletOrBelow = safeWidth <= SGA_BREAKPOINTS.tablet
  const isTabletLg = safeWidth > SGA_BREAKPOINTS.tablet && safeWidth <= SGA_BREAKPOINTS.tabletLg
  const isDesktop = safeWidth > SGA_BREAKPOINTS.tabletLg

  let tier = 'desktop'
  if (isMobile) tier = 'mobile'
  else if (isTablet) tier = 'tablet'
  else if (isTabletLg) tier = 'tablet-lg'

  return {
    width: safeWidth,
    tier,
    isMobile,
    isTablet,
    isTabletOrBelow,
    isTabletLg,
    isDesktop
  }
}

/**
 * Composable reactivo estándar de sgaUI para adaptar lógica de vistas,
 * drawers, modales y tablas según la resolución de pantalla.
 */
export function useResponsive() {
  const getWindowWidth = () => (typeof window !== 'undefined' ? window.innerWidth : 1280)
  const windowWidth = ref(getWindowWidth())

  const state = computed(() => resolveViewportState(windowWidth.value))

  const isMobile = computed(() => state.value.isMobile)
  const isTablet = computed(() => state.value.isTablet)
  const isTabletOrBelow = computed(() => state.value.isTabletOrBelow)
  const isTabletLg = computed(() => state.value.isTabletLg)
  const isDesktop = computed(() => state.value.isDesktop)
  const viewportTier = computed(() => state.value.tier)

  const getDialogWidth = (desktopWidth = '540px') => {
    return isMobile.value ? '94vw' : desktopWidth
  }

  let rafId = null
  const updateWidth = () => {
    if (typeof window === 'undefined') return
    if (rafId !== null) window.cancelAnimationFrame(rafId)
    rafId = window.requestAnimationFrame(() => {
      windowWidth.value = window.innerWidth
      rafId = null
    })
  }

  if (getCurrentInstance()) {
    onMounted(() => {
      windowWidth.value = getWindowWidth()
      if (typeof window !== 'undefined') {
        window.addEventListener('resize', updateWidth, { passive: true })
      }
    })

    onUnmounted(() => {
      if (typeof window !== 'undefined') {
        window.removeEventListener('resize', updateWidth)
        if (rafId !== null) window.cancelAnimationFrame(rafId)
      }
    })
  }

  return {
    windowWidth,
    viewportTier,
    isMobile,
    isTablet,
    isTabletOrBelow,
    isTabletLg,
    isDesktop,
    getDialogWidth,
    breakpoints: SGA_BREAKPOINTS
  }
}
