<script setup>
import { ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Drawer from 'primevue/drawer'
import Popover from 'primevue/popover'
import ConfirmPopup from 'primevue/confirmpopup'

const confirm = useConfirm()
const toast = useToast()

const dialogVisible = ref(false)
const drawerVisible = ref(false)
const drawerPos = ref('right')
const op = ref()

const togglePopover = (event) => op.value.toggle(event)

const openConfirmDialog = () => {
  confirm.require({
    message: '¿Deseas aprobar este expediente aduanero?',
    header: 'Confirmación',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Sí, aprobar',
    rejectLabel: 'Cancelar',
    accept: () => toast.add({ severity: 'success', summary: 'Aprobado', life: 2500 }),
    reject: () => toast.add({ severity: 'info', summary: 'Cancelado', life: 2500 })
  })
}

const openConfirmPopup = (event) => {
  confirm.require({
    target: event.currentTarget,
    group: 'popup',
    message: '¿Confirmar acción rápida?',
    icon: 'pi pi-question-circle',
    accept: () => toast.add({ severity: 'success', summary: 'Confirmado vía Popup', life: 2500 })
  })
}
</script>

<template>
  <div>
    <h2 class="section-title">Overlays, Modales y Popups</h2>
    <p class="section-desc">Dialog, Drawer, Popover, ConfirmDialog, ConfirmPopup y directiva v-tooltip.</p>

    <ConfirmPopup group="popup" />

    <div class="showcase-grid">
      <Card>
        <template #title>Dialog & Drawer</template>
        <template #content>
          <div class="demo-row">
            <Button label="Abrir Dialog" icon="pi pi-external-link" @click="dialogVisible = true" />
            <Button label="Drawer Derecho" severity="secondary" icon="pi pi-arrow-left" @click="drawerPos = 'right'; drawerVisible = true" />
            <Button label="Drawer Izquierdo" severity="secondary" icon="pi pi-arrow-right" @click="drawerPos = 'left'; drawerVisible = true" />
          </div>
        </template>
      </Card>

      <Card>
        <template #title>ConfirmDialog, ConfirmPopup & Popover</template>
        <template #content>
          <div class="demo-row">
            <Button label="ConfirmDialog" severity="warn" icon="pi pi-check-square" @click="openConfirmDialog" />
            <Button label="ConfirmPopup" severity="danger" icon="pi pi-trash" @click="openConfirmPopup($event)" />
            <Button label="Popover" severity="info" icon="pi pi-comment" @click="togglePopover" />
          </div>

          <Popover ref="op">
            <div style="padding: 0.5rem; max-width: 240px">
              <strong>Popover de PrimeVue</strong>
              <p style="margin: 0.4rem 0 0 0; font-size: 0.85rem">Ideal para detalles rápidos o mini-formularios contextuales.</p>
            </div>
          </Popover>
        </template>
      </Card>

      <Card>
        <template #title>Tooltip (v-tooltip)</template>
        <template #content>
          <div class="demo-row">
            <Button label="Top" v-tooltip.top="'Tooltip arriba'" />
            <Button label="Bottom" severity="secondary" v-tooltip.bottom="'Tooltip abajo'" />
            <Button label="Right" severity="info" v-tooltip.right="'Tooltip derecha'" />
          </div>
        </template>
      </Card>
    </div>

    <Dialog v-model:visible="dialogVisible" modal header="Dialog de PrimeVue" :style="{ width: '26rem' }">
      <p style="margin: 0">Este es un modal estándar de PrimeVue con soporte de arrastre, cierre con ESC y slots de header/footer.</p>
      <template #footer>
        <Button label="Cerrar" severity="secondary" @click="dialogVisible = false" />
        <Button label="Aceptar" @click="dialogVisible = false" />
      </template>
    </Dialog>

    <Drawer v-model:visible="drawerVisible" :position="drawerPos" header="Panel Lateral (Drawer)">
      <p>Contenido deslizable desde el borde <strong>{{ drawerPos }}</strong>.</p>
    </Drawer>
  </div>
</template>
