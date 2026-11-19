<script setup>
import { ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import Card from 'primevue/card'
import Menubar from 'primevue/menubar'
import Breadcrumb from 'primevue/breadcrumb'
import Menu from 'primevue/menu'
import TieredMenu from 'primevue/tieredmenu'
import PanelMenu from 'primevue/panelmenu'
import ContextMenu from 'primevue/contextmenu'
import MegaMenu from 'primevue/megamenu'

const toast = useToast()
const ctxMenu = ref()

const menubarItems = [
  { label: 'Inicio', icon: 'pi pi-home' },
  {
    label: 'Operaciones',
    icon: 'pi pi-briefcase',
    items: [
      { label: 'Cotizaciones', icon: 'pi pi-file' },
      { label: 'Expedientes', icon: 'pi pi-folder' }
    ]
  },
  { label: 'Reportes', icon: 'pi pi-chart-bar' }
]

const breadcrumbHome = { icon: 'pi pi-home' }
const breadcrumbItems = [{ label: 'Aduanas' }, { label: 'Expedientes' }, { label: 'EXP-2026-001' }]

const simpleMenuItems = [
  { label: 'Editar', icon: 'pi pi-pencil', command: () => toast.add({ severity: 'info', summary: 'Editar', life: 2000 }) },
  { label: 'Duplicar', icon: 'pi pi-copy', command: () => toast.add({ severity: 'info', summary: 'Duplicar', life: 2000 }) },
  { separator: true },
  { label: 'Anular', icon: 'pi pi-ban', command: () => toast.add({ severity: 'warn', summary: 'Anulado', life: 2000 }) }
]

const panelMenuItems = [
  {
    label: 'Catálogos',
    icon: 'pi pi-database',
    items: [
      { label: 'Puertos', icon: 'pi pi-map-marker' },
      { label: 'Mercancías', icon: 'pi pi-box' }
    ]
  },
  {
    label: 'Seguridad',
    icon: 'pi pi-shield',
    items: [
      { label: 'Usuarios', icon: 'pi pi-users' },
      { label: 'Roles', icon: 'pi pi-key' }
    ]
  }
]

const megaItems = [
  {
    label: 'Módulos',
    icon: 'pi pi-th-large',
    items: [
      [{ label: 'Comercial', items: [{ label: 'Clientes' }, { label: 'Cotizaciones' }] }],
      [{ label: 'Aduanas', items: [{ label: 'DAM' }, { label: 'Canales' }] }]
    ]
  },
  { label: 'Ayuda', icon: 'pi pi-question-circle' }
]

const onRightClick = (event) => ctxMenu.value.show(event)
</script>

<template>
  <div>
    <h2 class="section-title">Menús y Navegación</h2>
    <p class="section-desc">Menubar, Breadcrumb, MegaMenu, Menu, TieredMenu, PanelMenu y ContextMenu.</p>

    <div class="demo-box">
      <Menubar :model="menubarItems" />
      <Breadcrumb :home="breadcrumbHome" :model="breadcrumbItems" />
      <MegaMenu :model="megaItems" />

      <div class="showcase-grid">
        <Card>
          <template #title>Menu</template>
          <template #content><Menu :model="simpleMenuItems" /></template>
        </Card>

        <Card>
          <template #title>TieredMenu</template>
          <template #content><TieredMenu :model="menubarItems" /></template>
        </Card>

        <Card>
          <template #title>PanelMenu</template>
          <template #content><PanelMenu :model="panelMenuItems" /></template>
        </Card>

        <Card>
          <template #title>ContextMenu (Clic derecho aquí)</template>
          <template #content>
            <div
              @contextmenu="onRightClick"
              style="padding: 2rem; border: 2px dashed var(--p-surface-300); border-radius: 8px; text-align: center; cursor: context-menu;"
            >
              Haz clic derecho en esta área para abrir el ContextMenu
            </div>
            <ContextMenu ref="ctxMenu" :model="simpleMenuItems" />
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>
