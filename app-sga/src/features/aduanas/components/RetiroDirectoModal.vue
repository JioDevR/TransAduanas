<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('620px'), maxWidth: '95vw' }"
    header="Registrar Retiro Directo por el Cliente"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="retiro-directo-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial || expediente.cliente?.razonSocial || 'Cliente asignado' }}
          </span>
        </div>
        <Tag
          v-if="expediente.numeroDam"
          :value="`DAM ${expediente.numeroDam}`"
          severity="secondary"
          class="sga-mini-tag font-mono"
          rounded
        />
      </div>

      <div class="sga-banner is-warn">
        <i class="pi pi-user-edit"></i>
        <div>
          <strong>Retiro Directo por el Cliente (Sin Transporte SGA)</strong>
          <p class="retiro-banner-desc">
            Registre la conformidad cuando el consignatario retira la carga directamente del terminal portuario o almacén con su propia movilidad.
          </p>
        </div>
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label for="retiro-terminal" class="sga-form-label">Terminal / Almacén de Retiro *</label>
          <InputText
            id="retiro-terminal"
            v-model.trim="form.terminalAlmacen"
            placeholder="Ej. DP World Callao / Almacén Neptunia"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="retiro-acta" class="sga-form-label">N° Acta / Guía de Salida</label>
          <InputText
            id="retiro-acta"
            v-model.trim="form.numeroActaRetiro"
            placeholder="Ej. ACTA-2026-00981"
            class="font-mono"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="retiro-receptor" class="sga-form-label">Nombre Completo del Receptor *</label>
          <InputText
            id="retiro-receptor"
            v-model.trim="form.receptorNombre"
            placeholder="Persona autorizada por el cliente"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="retiro-dni" class="sga-form-label">DNI / Documento del Receptor *</label>
          <InputText
            id="retiro-dni"
            v-model.trim="form.receptorDni"
            placeholder="Ej. 45891234"
            class="font-mono"
            required
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="retiro-fecha" class="sga-form-label">Fecha y Hora de Retiro</label>
          <InputText
            id="retiro-fecha"
            v-model="form.fechaRetiro"
            type="datetime-local"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field full-span">
          <label for="retiro-obs" class="sga-form-label">Observaciones de Entrega / Precintos</label>
          <Textarea
            id="retiro-obs"
            v-model.trim="form.observaciones"
            rows="2"
            autoResize
            placeholder="Ej. Carga retirada con carta poder y placa ABC-123 sin incidencias..."
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
        form="retiro-directo-form"
        label="Confirmar Retiro Directo"
        icon="pi pi-check"
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

const getNowDateTimeInput = () => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

const form = ref({
  terminalAlmacen: '',
  numeroActaRetiro: '',
  receptorNombre: '',
  receptorDni: '',
  fechaRetiro: getNowDateTimeInput(),
  observaciones: ''
})

watch(
  () => [props.visible, props.expediente],
  ([vis, exp]) => {
    if (vis && exp) {
      errorMessage.value = ''
      form.value = {
        terminalAlmacen: exp.terminalOrigen || exp.depositoTemporal || '',
        numeroActaRetiro: '',
        receptorNombre: '',
        receptorDni: '',
        fechaRetiro: getNowDateTimeInput(),
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
  if (!form.value.terminalAlmacen || !form.value.receptorNombre || !form.value.receptorDni) {
    errorMessage.value = 'Complete el terminal de retiro, nombre y documento del receptor.'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const fechaRaw = form.value.fechaRetiro
    const payload = {
      terminalAlmacen: form.value.terminalAlmacen,
      numeroActaRetiro: form.value.numeroActaRetiro || null,
      receptorNombre: form.value.receptorNombre,
      receptorDni: form.value.receptorDni,
      fechaRetiro: fechaRaw ? (fechaRaw.length === 16 ? `${fechaRaw}:00` : fechaRaw) : null,
      observaciones: form.value.observaciones || null
    }

    const updated = await expedienteService.registrarRetiroDirecto(props.expediente.idExpediente, payload)
    emit('saved', updated)
    handleClose()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar el retiro directo.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.retiro-banner-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  line-height: 1.4;
}

.full-span {
  grid-column: 1 / -1;
}
</style>
