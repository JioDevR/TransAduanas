<script setup>
import { ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import Card from 'primevue/card'
import Button from 'primevue/button'
import ButtonGroup from 'primevue/buttongroup'
import SplitButton from 'primevue/splitbutton'
import SpeedDial from 'primevue/speeddial'

const toast = useToast()
const loading = ref(false)
const simulateLoad = () => {
  loading.value = true
  setTimeout(() => (loading.value = false), 1200)
}

const splitItems = [
  { label: 'Actualizar', icon: 'pi pi-refresh', command: () => toast.add({ severity: 'info', summary: 'Actualizado', life: 2000 }) },
  { label: 'Exportar PDF', icon: 'pi pi-file-pdf', command: () => toast.add({ severity: 'success', summary: 'PDF generado', life: 2000 }) },
  { label: 'Eliminar', icon: 'pi pi-trash', command: () => toast.add({ severity: 'warn', summary: 'Eliminado', life: 2000 }) }
]

const speedItems = [
  { label: 'Agregar', icon: 'pi pi-pencil', command: () => toast.add({ severity: 'info', summary: 'Editar', life: 2000 }) },
  { label: 'Subir', icon: 'pi pi-upload', command: () => toast.add({ severity: 'success', summary: 'Subir archivo', life: 2000 }) },
  { label: 'Compartir', icon: 'pi pi-share-alt', command: () => toast.add({ severity: 'secondary', summary: 'Compartido', life: 2000 }) }
]
</script>

<template>
  <div>
    <h2 class="section-title">Botones y Acciones</h2>
    <p class="section-desc">Variantes de Button, ButtonGroup, SplitButton y SpeedDial.</p>

    <div class="showcase-grid">
      <Card>
        <template #title>Severidades & Variantes</template>
        <template #content>
          <div class="demo-box">
            <div class="demo-row">
              <Button label="Primary" />
              <Button label="Secondary" severity="secondary" />
              <Button label="Success" severity="success" />
              <Button label="Info" severity="info" />
              <Button label="Warn" severity="warn" />
              <Button label="Danger" severity="danger" />
              <Button label="Contrast" severity="contrast" />
            </div>
            <div class="demo-row">
              <Button label="Outlined" outlined />
              <Button label="Text" text />
              <Button label="Raised" raised />
              <Button label="Rounded" rounded />
              <Button icon="pi pi-check" rounded aria-label="Check" />
              <Button label="Loading" icon="pi pi-save" :loading="loading" @click="simulateLoad" />
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>ButtonGroup & SplitButton</template>
        <template #content>
          <div class="demo-box">
            <ButtonGroup>
              <Button label="Guardar" icon="pi pi-check" />
              <Button label="Cancelar" icon="pi pi-times" severity="secondary" />
              <Button label="Borrar" icon="pi pi-trash" severity="danger" />
            </ButtonGroup>

            <div class="demo-row">
              <SplitButton label="Acciones" icon="pi pi-plus" :model="splitItems" @click="toast.add({ severity: 'info', summary: 'Click principal', life: 2000 })" />
              <SplitButton label="Success" severity="success" :model="splitItems" rounded />
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>SpeedDial</template>
        <template #content>
          <div style="position: relative; height: 150px; display: flex; align-items: center; justify-content: center;">
            <SpeedDial :model="speedItems" direction="right" />
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>
