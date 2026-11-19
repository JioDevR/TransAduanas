<script setup>
import { ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import ProgressBar from 'primevue/progressbar'
import ProgressSpinner from 'primevue/progressspinner'
import Skeleton from 'primevue/skeleton'
import Badge from 'primevue/badge'
import OverlayBadge from 'primevue/overlaybadge'
import Tag from 'primevue/tag'
import Chip from 'primevue/chip'
import Avatar from 'primevue/avatar'
import AvatarGroup from 'primevue/avatargroup'
import MeterGroup from 'primevue/metergroup'
import Inplace from 'primevue/inplace'
import InputText from 'primevue/inputtext'
import FileUpload from 'primevue/fileupload'
import Carousel from 'primevue/carousel'

const toast = useToast()
const inplaceText = ref('Haz clic para editar este texto')
const meterValues = [
  { label: 'Marítimo', color: '#3b82f6', value: 55 },
  { label: 'Aéreo', color: '#10b981', value: 25 },
  { label: 'Terrestre', color: '#f59e0b', value: 20 }
]
const carouselItems = [
  { title: 'Puerto del Callao', tag: 'Marítimo', desc: 'Principal terminal portuario del Perú.' },
  { title: 'Aeropuerto Jorge Chávez', tag: 'Aéreo', desc: 'Carga aérea prioritaria y courier.' },
  { title: 'Puerto de Chancay', tag: 'Megapuerto', desc: 'Hub logístico del Pacífico Sur.' }
]

const showToast = (severity) => {
  toast.add({ severity, summary: `Toast ${severity.toUpperCase()}`, detail: 'Notificación generada con PrimeVue ToastService', life: 3000 })
}
</script>

<template>
  <div>
    <h2 class="section-title">Mensajes, Feedback, Media y Miscelánea</h2>
    <p class="section-desc">Toast, Message, ProgressBar, Skeleton, Badge, Tag, Chip, Avatar, MeterGroup, Inplace, FileUpload y Carousel.</p>

    <div class="showcase-grid">
      <Card>
        <template #title>Toast & Message</template>
        <template #content>
          <div class="demo-box">
            <div class="demo-row">
              <Button label="Success" severity="success" size="small" @click="showToast('success')" />
              <Button label="Info" severity="info" size="small" @click="showToast('info')" />
              <Button label="Warn" severity="warn" size="small" @click="showToast('warn')" />
              <Button label="Error" severity="danger" size="small" @click="showToast('error')" />
            </div>
            <Message severity="info">Mensaje informativo inline (Message).</Message>
            <Message severity="warn">Advertencia de validación pendiente.</Message>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>Tag, Chip, Badge & Avatar</template>
        <template #content>
          <div class="demo-box">
            <div class="demo-row">
              <Tag value="Success" severity="success" />
              <Tag value="Warn" severity="warn" rounded />
              <Tag value="Danger" severity="danger" icon="pi pi-times" />
              <Chip label="Aduanas" icon="pi pi-briefcase" removable />
            </div>
            <div class="demo-row">
              <Badge value="4" />
              <Badge value="12" severity="danger" />
              <OverlayBadge value="9+" severity="warn">
                <i class="pi pi-bell" style="font-size: 1.5rem" />
              </OverlayBadge>
              <AvatarGroup>
                <Avatar label="JD" shape="circle" />
                <Avatar label="MA" shape="circle" style="background-color: #10b981; color: #fff" />
                <Avatar label="+3" shape="circle" />
              </AvatarGroup>
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>ProgressBar, MeterGroup & Spinner</template>
        <template #content>
          <div class="demo-box">
            <ProgressBar :value="72" />
            <ProgressBar mode="indeterminate" style="height: 6px" />
            <MeterGroup :value="meterValues" />
            <div class="demo-row">
              <ProgressSpinner style="width: 36px; height: 36px" strokeWidth="4" />
              <span>Cargando datos...</span>
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>Skeleton & Inplace</template>
        <template #content>
          <div class="demo-box">
            <Skeleton width="100%" height="1.5rem" />
            <Skeleton width="70%" height="1rem" />
            <Inplace>
              <template #display>{{ inplaceText }}</template>
              <template #content="{ closeCallback }">
                <div class="demo-row">
                  <InputText v-model="inplaceText" autofocus />
                  <Button icon="pi pi-check" @click="closeCallback" />
                </div>
              </template>
            </Inplace>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>FileUpload (básico)</template>
        <template #content>
          <FileUpload mode="basic" name="demo[]" accept="image/*,.pdf" :maxFileSize="1000000" chooseLabel="Seleccionar Archivo" @select="showToast('info')" />
        </template>
      </Card>

      <Card>
        <template #title>Carousel</template>
        <template #content>
          <Carousel :value="carouselItems" :numVisible="1" :numScroll="1" circular>
            <template #item="{ data }">
              <div style="padding: 1rem; border: 1px solid var(--p-surface-200); border-radius: 8px;">
                <Tag :value="data.tag" severity="info" style="margin-bottom: 0.5rem" />
                <h4 style="margin: 0 0 0.25rem 0">{{ data.title }}</h4>
                <p style="margin: 0; font-size: 0.9rem; opacity: 0.8">{{ data.desc }}</p>
              </div>
            </template>
          </Carousel>
        </template>
      </Card>
    </div>
  </div>
</template>
