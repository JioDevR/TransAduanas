<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('660px'), maxWidth: '95vw' }"
    header="Solicitar Recojo de Mercancía en Planta (Exportación)"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="solicitud-recojo-exportacion-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial || 'Exportador asignado' }}
          </span>
        </div>
        <Tag
          value="EXPORTACIÓN"
          severity="info"
          class="sga-mini-tag"
          rounded
        />
      </div>

      <div class="sga-banner is-info">
        <i class="pi pi-truck"></i>
        <div>
          <strong>Solicitud de Recojo en Planta hacia Zona Primaria (Externo)</strong>
          <p class="recojo-banner-desc">
            Registre los datos de recojo en planta y terminal de destino para la coordinación con el operador de transporte externo.
          </p>
        </div>
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field full-span">
          <label for="recojo-planta" class="sga-form-label">Dirección de Recojo en Planta / Fábrica *</label>
          <InputText
            id="recojo-planta"
            v-model.trim="form.direccionRecojoPlanta"
            placeholder="Ej. Av. Nicolás Ayllón 2890, Ate Vitarte"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="recojo-distrito" class="sga-form-label">Distrito de Planta *</label>
          <InputText
            id="recojo-distrito"
            v-model.trim="form.distritoRecojo"
            placeholder="Ej. Callao / Ate / Lurín"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="recojo-terminal" class="sga-form-label">Terminal / Almacén de Destino *</label>
          <InputText
            id="recojo-terminal"
            v-model.trim="form.almacenDestinoTerminal"
            placeholder="Ej. DP World Callao / APM Terminals"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="recojo-contacto" class="sga-form-label">Contacto en Planta Exportadora</label>
          <InputText
            id="recojo-contacto"
            v-model.trim="form.contactoPlanta"
            placeholder="Responsable de despacho y teléfono"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="recojo-fecha" class="sga-form-label">Fecha y Hora Programada de Recojo *</label>
          <InputText
            id="recojo-fecha"
            v-model="form.fechaProgramadaRecojo"
            type="datetime-local"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="recojo-obs" class="sga-form-label">Observaciones / Requisitos de Unidad</label>
          <Textarea
            id="recojo-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Ej. Contenedor refrigerado de 40 pies, acceso por puerta posterior..."
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
        form="solicitud-recojo-exportacion-form"
        label="Registrar Solicitud de Recojo"
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
import { expedienteService } from '@/shared/services/expedienteService'
import { getNowDateTimeInput, toLocalDateTimePayload } from '@/shared/utils/dateUtils'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()

const loading = ref(false)
const errorMessage = ref('')

const form = ref({
  direccionRecojoPlanta: '',
  distritoRecojo: '',
  almacenDestinoTerminal: 'DP World Callao - Terminal Portuario',
  contactoPlanta: '',
  fechaProgramadaRecojo: '',
  observaciones: ''
})

const getTomorrowISOString = () => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  tomorrow.setHours(8, 0, 0, 0)
  return getNowDateTimeInput(tomorrow)
}

watch(
  () => [props.visible, props.expediente],
  ([vis, exp]) => {
    if (vis && exp) {
      errorMessage.value = ''
      form.value = {
        direccionRecojoPlanta: exp.clienteDireccion || 'Planta Principal Explotación / Embalaje',
        distritoRecojo: 'Callao',
        almacenDestinoTerminal: 'DP World Callao - Terminal Portuario',
        contactoPlanta: exp.clienteContacto || exp.clienteRazonSocial || '',
        fechaProgramadaRecojo: getTomorrowISOString(),
        observaciones: 'Coordinar con planta para ingreso de unidad y pesaje en balanza.'
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
  loading.value = true
  errorMessage.value = ''

  try {
    const payload = {
      direccionRecojoPlanta: form.value.direccionRecojoPlanta,
      distritoRecojo: form.value.distritoRecojo,
      almacenDestinoTerminal: form.value.almacenDestinoTerminal,
      contactoPlanta: form.value.contactoPlanta,
      fechaProgramadaRecojo: toLocalDateTimePayload(form.value.fechaProgramadaRecojo),
      observaciones: form.value.observaciones
    }

    const res = await expedienteService.solicitarRecojoExportacion(props.expediente.idExpediente, payload)
    emit('saved', res)
    handleClose()
  } catch (err) {
    console.error('Error al solicitar recojo en planta:', err)
    errorMessage.value = err.response?.data?.message || err.message || 'Error al registrar solicitud de recojo externo.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.recojo-banner-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  line-height: 1.4;
}

.full-span {
  grid-column: 1 / -1;
}
</style>
