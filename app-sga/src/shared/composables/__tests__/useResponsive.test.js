import { describe, it, expect } from 'vitest'
import { SGA_BREAKPOINTS, resolveViewportState, useResponsive } from '../useResponsive'

describe('useResponsive (Estándar sgaUI)', () => {
  it('clasifica correctamente los 4 tiers de resolución oficiales', () => {
    expect(resolveViewportState(375)).toMatchObject({
      tier: 'mobile',
      isMobile: true,
      isTablet: false,
      isTabletOrBelow: true,
      isDesktop: false
    })

    expect(resolveViewportState(768)).toMatchObject({
      tier: 'tablet',
      isMobile: false,
      isTablet: true,
      isTabletOrBelow: true,
      isDesktop: false
    })

    expect(resolveViewportState(1024)).toMatchObject({
      tier: 'tablet-lg',
      isMobile: false,
      isTablet: false,
      isTabletOrBelow: false,
      isTabletLg: true,
      isDesktop: false
    })

    expect(resolveViewportState(1440)).toMatchObject({
      tier: 'desktop',
      isMobile: false,
      isTabletOrBelow: false,
      isDesktop: true
    })
  })

  it('respeta los límites exactos de SGA_BREAKPOINTS', () => {
    expect(resolveViewportState(SGA_BREAKPOINTS.mobile).tier).toBe('mobile')
    expect(resolveViewportState(SGA_BREAKPOINTS.mobile + 1).tier).toBe('tablet')
    expect(resolveViewportState(SGA_BREAKPOINTS.tablet).tier).toBe('tablet')
    expect(resolveViewportState(SGA_BREAKPOINTS.tablet + 1).tier).toBe('tablet-lg')
    expect(resolveViewportState(SGA_BREAKPOINTS.tabletLg).tier).toBe('tablet-lg')
    expect(resolveViewportState(SGA_BREAKPOINTS.tabletLg + 1).tier).toBe('desktop')
  })

  it('ajusta el ancho de diálogo dinámicamente en entorno SSR/Node sin window', () => {
    const { viewportTier, isDesktop, getDialogWidth } = useResponsive()
    expect(viewportTier.value).toBe('desktop')
    expect(isDesktop.value).toBe(true)
    expect(getDialogWidth('540px')).toBe('540px')
  })
})
