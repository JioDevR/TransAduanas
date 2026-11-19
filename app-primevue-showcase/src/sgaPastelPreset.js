import { definePreset } from '@primevue/themes'
import SgaThemePreset from '../../sgaUI/sgaThemePreset'

/**
 * Variante Pastel de SGA Theme (Prueba en app-primevue-showcase)
 * Suaviza el acento cobrizo hacia un terracota/durazno pastel cálido.
 */
export const SgaPastelPreset = definePreset(SgaThemePreset, {
  semantic: {
    primary: {
      50: '#fefaf7',
      100: '#fdf1e8',
      200: '#fae0cc',
      300: '#f4c8a6',
      400: '#f0b892',
      500: '#d48252',
      600: '#bf6e40',
      700: '#a35c35',
      800: '#824728',
      900: '#63351d',
      950: '#3b1e0f'
    },
    focusRing: {
      width: '2px',
      style: 'solid',
      color: '{primary.300}',
      offset: '2px'
    },
    colorScheme: {
      light: {
        primary: {
          color: '#d48252',
          contrastColor: '#ffffff',
          hoverColor: '#bf6e40',
          activeColor: '#a35c35'
        },
        highlight: {
          background: '#fdf1e8',
          focusBackground: '#fae0cc',
          color: '#824728',
          focusColor: '#63351d'
        }
      },
      dark: {
        primary: {
          color: '#f0b892',
          contrastColor: '#161412',
          hoverColor: '#f6cbb0',
          activeColor: '#fae0cc'
        },
        highlight: {
          background: 'rgba(240, 184, 146, 0.16)',
          focusBackground: 'rgba(240, 184, 146, 0.24)',
          color: '#f6f2ea',
          focusColor: '#ffffff'
        }
      }
    }
  }
})

export default SgaPastelPreset
