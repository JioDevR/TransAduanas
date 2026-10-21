import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { definePreset } from '@primevue/themes'
import Aura from '@primevue/themes/aura'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'

import App from './App.vue'
import router from './core/router'
import { initTheme } from '@/shared/composables/useTheme'
import './style.css'

// Inicializar tema (claro/oscuro) leyendo localStorage antes de renderizar
initTheme()

// Preset personalizado para el Sistema de Gestión Aduanera (SGA)
// Mantiene los tonos cálidos: espresso profundo (#352f2a), arena, latte y café.
const SgaPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#f6f3ee',
      100: '#ede6db',
      200: '#e4dacb',
      300: '#cfc0ab',
      400: '#9a7f60',
      500: '#352f2a',
      600: '#2b2520',
      700: '#231e1a',
      800: '#1c1815',
      900: '#120f0d',
      950: '#0a0807'
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
        }
      },
      dark: {
        surface: {
          0: '#191815',
          50: '#1e1e1b',
          100: '#282723',
          200: '#2e2d27',
          300: '#383630',
          400: '#444138',
          500: '#5c564a',
          600: '#968e7e',
          700: '#c4bcab',
          800: '#ede6db',
          900: '#f6f2ea',
          950: '#ffffff'
        }
      }
    }
  }
})

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: SgaPreset,
    options: {
      darkModeSelector: '.dark-theme'
    }
  }
})
app.use(ToastService)
app.use(ConfirmationService)

app.mount('#app')
