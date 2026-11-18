<script setup>
import { ref, computed } from 'vue'
import { definePreset, usePreset } from '@primevue/themes'
import Aura from '@primevue/themes/aura'
import Lara from '@primevue/themes/lara'
import Nora from '@primevue/themes/nora'
import Material from '@primevue/themes/material'
import SgaThemePreset from '../../sgaUI/sgaThemePreset'
import SgaPastelPreset from './sgaPastelPreset'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'

import FormInputsSection from './sections/FormInputsSection.vue'
import ButtonsSection from './sections/ButtonsSection.vue'
import DataSection from './sections/DataSection.vue'
import PanelsSection from './sections/PanelsSection.vue'
import OverlaysSection from './sections/OverlaysSection.vue'
import MenusSection from './sections/MenusSection.vue'
import MiscSection from './sections/MiscSection.vue'
import SgaComponentsSection from './sections/SgaComponentsSection.vue'
import SgaMastersSection from './sections/SgaMastersSection.vue'
import SgaShellBaseSection from './sections/SgaShellBaseSection.vue'

// Preset exacto de app-sga (Warm Editorial: espresso, arena, latte y café)
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

const darkMode = ref(false)
const activeTab = ref('forms')
const activePreset = ref('SGA Pastel')

const presets = {
  Aura,
  Lara,
  Nora,
  Material,
  'SGA (Actual)': SgaPreset,
  'SGA Theme': SgaThemePreset,
  'SGA Pastel': SgaPastelPreset
}
const presetOptions = Object.keys(presets)

const changePreset = (name) => {
  if (!name || !presets[name]) return
  activePreset.value = name
  usePreset(presets[name])
}

const categories = [
  { id: 'forms', label: 'Formularios e Inputs', icon: 'pi pi-pencil', component: FormInputsSection },
  { id: 'buttons', label: 'Botones y Acciones', icon: 'pi pi-bolt', component: ButtonsSection },
  { id: 'data', label: 'Datos y Tablas', icon: 'pi pi-table', component: DataSection },
  { id: 'panels', label: 'Paneles y Layouts', icon: 'pi pi-th-large', component: PanelsSection },
  { id: 'overlays', label: 'Modales y Popups', icon: 'pi pi-window-maximize', component: OverlaysSection },
  { id: 'menus', label: 'Menús y Navegación', icon: 'pi pi-bars', component: MenusSection },
  { id: 'misc', label: 'Mensajes y Media', icon: 'pi pi-sparkles', component: MiscSection },
  { id: 'sga-custom', label: 'Componentes SGA', icon: 'pi pi-box', component: SgaComponentsSection },
  { id: 'sga-masters', label: 'Maestros SGA (Variantes)', icon: 'pi pi-sitemap', component: SgaMastersSection },
  { id: 'sga-shell', label: 'Shell Base SGA', icon: 'pi pi-desktop', component: SgaShellBaseSection }
]

const currentComponent = computed(() => {
  return categories.find((c) => c.id === activeTab.value)?.component || FormInputsSection
})

const toggleDarkMode = () => {
  darkMode.value = !darkMode.value
  document.documentElement.classList.toggle('app-dark', darkMode.value)
}
</script>

<template>
  <Toast />
  <ConfirmDialog />

  <div class="layout">
    <header class="topbar">
      <div class="brand">
        <i class="pi pi-prime" style="font-size: 1.6rem; color: var(--p-primary-color)" />
        <div>
          <strong>PrimeVue 4 Showcase</strong>
          <div style="font-size: 0.78rem; opacity: 0.7">Catálogo interactivo independiente (Aura Preset)</div>
        </div>
        <Tag value="v4.5.5" severity="success" rounded />
      </div>

      <div class="topbar-actions">
        <SelectButton
          :modelValue="activePreset"
          :options="presetOptions"
          :allowEmpty="false"
          size="small"
          @update:modelValue="changePreset"
        />
        <Button
          :icon="darkMode ? 'pi pi-sun' : 'pi pi-moon'"
          :label="darkMode ? 'Modo Claro' : 'Modo Oscuro'"
          severity="secondary"
          outlined
          size="small"
          @click="toggleDarkMode"
        />
      </div>
    </header>

    <nav class="nav-strip">
      <Button
        v-for="cat in categories"
        :key="cat.id"
        :label="cat.label"
        :icon="cat.icon"
        :severity="activeTab === cat.id ? 'primary' : 'secondary'"
        :outlined="activeTab !== cat.id"
        size="small"
        @click="activeTab = cat.id"
      />
    </nav>

    <main class="content">
      <component :is="currentComponent" />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.9rem 1.5rem;
  border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
  background: var(--p-surface-0, #ffffff);
  position: sticky;
  top: 0;
  z-index: 20;
}

.app-dark .topbar {
  background: var(--p-surface-900, #18181b);
  border-color: var(--p-surface-700, #27272a);
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.topbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.nav-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.85rem 1.5rem;
  border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
  background: var(--p-surface-0, #ffffff);
}

.app-dark .nav-strip {
  background: var(--p-surface-900, #18181b);
  border-color: var(--p-surface-700, #27272a);
}

.content {
  padding: 1.5rem;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
}
</style>
