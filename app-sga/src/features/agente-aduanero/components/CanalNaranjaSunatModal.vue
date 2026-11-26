<template>
  <Dialog
    :visible="visible"
    modal
    :closable="!loading"
    :dismissableMask="!loading"
    :style="{ width: getDialogWidth('560px'), maxWidth: '95vw' }"
    header="Canal NARANJA — Transmisión de Documentos a SUNAT"
    @update:visible="(val) => { if (!val && !loading) handleClose() }"
  >
    <form id="canal-naranja-form" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div v-if="expediente" class="sga-dialog-context">
        <div class="sga-dialog-context-meta">
          <span class="sga-code-cell font-mono">{{ expediente.codigoExpediente }}</span>
          <span v-if="expediente.clienteRazonSocial" class="sga-dialog-context-sub">
            {{ expediente.clienteRazonSocial }}
          </span>
        </div>
        <Tag
          :value="expediente.numeroDam ? `DAM ${expediente.numeroDam}` : 'DAM S/N'"
          severity="warn"
          class="font-mono sga-mini-tag"
          rounded
        />
      </div>

      <div class="sga-banner is-warn">
        <i class="pi pi-cloud-upload"></i>
        <div>
          <strong>Teledespacho Documentario SUNAT (Canal Naranja)</strong>
          <p class="sunat-banner-desc">
            Envíe el legajo digital sustentatorio al especialista SUNAT para habilitar el levante documentario.
          </p>
        </div>
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger" role="alert">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-form-field">
        <label for="sunat-docs" class="sga-form-label">Documentos Transmitidos a SUNAT *</label>
        <InputText
          id="sunat-docs"
          v-model.trim="form.documentosIncluidos"
          required
          placeholder="Factura Comercial, Packing List, BL/AWB, Certificado de Origen"
          size="small"
          fluid
        />
      </div>

      <div class="sga-form-field">
        <label for="sunat-obs" class="sga-form-label">Sustento / Observaciones para Especialista SUNAT</label>
        <Textarea
          id="sunat-obs"
          v-model.trim="form.observaciones"
          rows="3"
          autoResize
          placeholder="Documentación sustentatoria completa conforme a subpartida arancelaria..."
          size="small"
          fluid
        />
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
        form="canal-naranja-form"
        label="Enviar Documentos a SUNAT"
        icon="pi pi-cloud-upload"
        severity="warn"
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

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()

const loading = ref(false)
const errorMessage = ref('')
const form = ref({
  documentosIncluidos: 'Factura Comercial, Packing List, BL/AWB y DAM',
  observaciones: ''
})

watch(
  () => props.visible,
  (vis) => {
    if (vis) {
      errorMessage.value = ''
      form.value = {
        documentosIncluidos: 'Factura Comercial, Packing List, BL/AWB y DAM',
        observaciones: 'Revisión documentaria Canal Naranja transmitida directamente a SUNAT.'
      }
    }
  }
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
    const res = await expedienteService.enviarSunatSimulado(props.expediente.idExpediente, form.value)
    emit('saved', res)
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || 'No se pudo transmitir documentos a SUNAT.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.sunat-banner-desc {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  line-height: 1.4;
}
</style>
