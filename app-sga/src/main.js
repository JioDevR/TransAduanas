import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'

import App from './App.vue'
import router from './core/router'
import { initTheme } from '@/shared/composables/useTheme'
import SgaThemePreset from '../../sgaUI/sgaThemePreset'
import '../../sgaUI/sga-theme.css'
import './style.css'

// Inicializar tema (claro/oscuro) leyendo localStorage antes de renderizar
initTheme()

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: SgaThemePreset,
    options: {
      darkModeSelector: '.dark-theme'
    }
  }
})
app.use(ToastService)
app.use(ConfirmationService)

app.mount('#app')
