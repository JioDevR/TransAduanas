import { definePreset } from '@primevue/themes'
import Aura from '@primevue/themes/aura'

/**
 * SGA Theme — Warm Editorial Pro (Unificado para app-sga y app-portal-cliente)
 *
 * - Acento primario funcional: Terracota / Durazno pastel (#d48252 en claro, #f0b892 en oscuro)
 * - Superficies cálidas: Arena / Latte / Espresso
 * - Polaridad correcta de PrimeVue 4 Aura en modo oscuro (0 = texto claro, 950 = fondo oscuro)
 */
export const SgaThemePreset = definePreset(Aura, {
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
    formField: {
      borderRadius: '10px',
      paddingX: '0.85rem',
      paddingY: '0.55rem'
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '#fbf9f6',
          100: '#f6f3ee',
          200: '#ede6db',
          300: '#e4dacb',
          400: '#cfc0ab',
          500: '#998c7e',
          600: '#6f6356',
          700: '#484039',
          800: '#352f2a',
          900: '#2b2520',
          950: '#1c1815'
        },
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
        surface: {
          0: '#ffffff',
          50: '#f6f2ea',
          100: '#ede6db',
          200: '#d6cbbb',
          300: '#c4bcab',
          400: '#968e7e',
          500: '#6f6759',
          600: '#5c564a',
          700: '#3f3a33',
          800: '#2e2a25',
          900: '#1f1c19',
          950: '#161412'
        },
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
  },
  components: {
    button: {
      root: {
        borderRadius: '10px'
      }
    },
    card: {
      root: {
        borderRadius: '14px'
      },
      body: {
        padding: '1.25rem',
        gap: '0.75rem'
      }
    },
    panel: {
      root: {
        borderRadius: '14px'
      }
    },
    dialog: {
      root: {
        borderRadius: '14px'
      }
    }
  }
})

export default SgaThemePreset
