<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('660px'), maxWidth: '95vw' }"
    header="Solicitar Transporte y Despacho a Destino"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="solicitud-transporte-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial || expediente.cliente?.razonSocial || 'Cliente asignado' }}
          </span>
        </div>
        <Tag
          v-if="expediente.tipoCarga"
          :value="expediente.tipoCarga"
          severity="info"
          class="sga-mini-tag"
          rounded
        />
      </div>

      <div class="sga-banner is-success">
        <i class="pi pi-truck"></i>
        <div>
          <strong>Handoff Operativo a Supervisor de Transporte y Flota</strong>
          <p class="transporte-banner-desc">
            Al registrar la solicitud, el Área de Flota recibirá la orden de retiro y asignará unidad y chofer para el despacho.
          </p>
        </div>
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field full-span">
          <label for="transp-partida" class="sga-form-label">Punto de Partida (Terminal / Puerto / Almacén) *</label>
          <InputText
            id="transp-partida"
            v-model.trim="form.direccionPartida"
            placeholder="Ej. APM Terminals Callao - Puerta 4 / Depósito Neptunia"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="transp-llegada" class="sga-form-label">Dirección de Llegada (Planta / Almacén Cliente) *</label>
          <InputText
            id="transp-llegada"
            v-model.trim="form.direccionLlegada"
            placeholder="Ej. Av. Argentina 2850, Zona Industrial"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="transp-distrito" class="sga-form-label">Distrito de Llegada *</label>
          <InputText
            id="transp-distrito"
            v-model.trim="form.distritoLlegada"
            placeholder="Ej. Callao / Cercado de Lima / Lurín"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="transp-contacto" class="sga-form-label">Contacto de Recepción en Destino</label>
          <InputText
            id="transp-contacto"
            v-model.trim="form.contactoRecepcion"
            placeholder="Ej. Ing. Carlos Rojas · 987 654 321"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="transp-fecha" class="sga-form-label">Fecha y Hora Programada *</label>
          <InputText
            id="transp-fecha"
            v-model="form.fechaProgramada"
            type="datetime-local"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="transp-obs" class="sga-form-label">Instrucciones / Requisitos de Unidad</label>
          <Textarea
            id="transp-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Ej. Requiere plataforma de 40 pies, chofer con SCTR y EPP completo..."
            size="small"
            fluid
          />
        </div>
      </div>
    </form>

    <template #footer>
      <Button
        type="button"
        label="Cancelar"
        severity="secondary"
        outlined
        size="small"
        :disabled="loading"
        @click="handleClose"
      />
      <Button
        type="submit"
        form="solicitud-transporte-form"
        label="Enviar Solicitud a Transporte"
        icon="pi pi-send"
        size="small"
        :loading="loading"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { expedienteService } from '../services/expedienteService'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()

const loading = ref(false)
const errorMessage = ref('')

const getTomorrowMorningInput = () => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  const pad = (n) => String(n).padStart(2, '0')
  return `${tomorrow.getFullYear()}-${pad(tomorrow.getMonth() + 1)}-${pad(tomorrow.getDate())}T08:00`
}

const form = ref({
  direccionPartida: '',
  direccionLlegada: '',
  distritoLlegada: '',
  contactoRecepcion: '',
  fechaProgramada: getTomorrowMorningInput(),
  observaciones: ''
})

watch(
  () => [props.visible, props.expediente],
  ([vis, exp]) => {
    if (vis && exp) {
      errorMessage.value = ''
      form.value = {
        direccionPartida: exp.terminalOrigen || exp.depositoTemporal || '',
        direccionLlegada: exp.direccionEntrega || exp.clienteDireccion || '',
        distritoLlegada: exp.distritoEntrega || '',
        contactoRecepcion: '',
        fechaProgramada: getTomorrowMorningInput(),
        observaciones: ''
      }
    }
  },
  { immediate: true }
)

const handleClose = () => {
  errorMessage.value = ''
  emit('close')
}

const handleSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  if (!form.value.direccionPartida || !form.value.direccionLlegada || !form.value.distritoLlegada || !form.value.fechaProgramada) {
    errorMessage.value = 'Complete los campos obligatorios de partida, llegada, distrito y fecha programada.'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const fechaRaw = form.value.fechaProgramada
    const payload = {
      direccionPartida: form.value.direccionPartida,
      direccionLlegada: form.value.direccionLlegada,
      distritoLlegada: form.value.distritoLlegada,
      contactoRecepcion: form.value.contactoRecepcion || null,
      fechaProgramada: fechaRaw.length === 16 ? `${fechaRaw}:00` : fechaRaw,
      observaciones: form.value.observaciones || null
    }

    const updated = await expedienteService.solicitarTransporte(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al solicitar el transporte.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.transporte-banner-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  line-height: 1.4;
}

.full-span {
  grid-column: 1 / -1;
}
</style>
