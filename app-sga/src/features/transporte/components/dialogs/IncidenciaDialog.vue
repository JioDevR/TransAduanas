<template>
  <Dialog
    :visible="visible"
    modal
    header="Bitácora // Registrar Incidencia en Ruta"
    :style="{ width: getDialogWidth('520px') }"
    :closable="!loading"
    @update:visible="(v) => !v && handleClose()"
  >
    <form v-if="despacho" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div class="sga-banner is-warn">
        <i class="pi pi-exclamation-triangle"></i>
        <div>
          Despacho <strong class="sga-mono">{{ despacho.codigoTracking }}</strong> ·
          {{ despacho.unidadPlacaTracto || 'Sin unidad' }}
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="incidencia-texto">Detalle de la eventualidad / novedad en ruta *</label>
        <Textarea
          id="incidencia-texto"
          v-model.trim="texto"
          rows="4"
          placeholder="Describa la incidencia (control aduanero SUNAT, congestión portuaria, desperfecto técnico, desvío autorizado)..."
          required
          fluid
        />
      </div>

      <div v-if="errorMessage" class="sga-banner is-danger">
        <i class="pi pi-exclamation-circle"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <div class="sga-dialog-footer">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          :disabled="loading"
          @click="handleClose"
        />
        <Button
          type="submit"
          label="Asentar Reporte"
          icon="pi pi-save"
          severity="warn"
          :loading="loading"
          :disabled="!texto"
        />
      </div>
    </form>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { useNotification } from '@/shared/composables/useNotification'
import { transporteFlotaService } from '../../services/transporteFlotaService'

const props = defineProps({
  visible: { type: Boolean, default: false },
  despacho: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()
const { showSuccess } = useNotification()

const texto = ref('')
const loading = ref(false)
const errorMessage = ref('')

watch(
  () => props.visible,
  (val) => {
    if (val) {
      texto.value = ''
      errorMessage.value = ''
    }
  }
)

const handleClose = () => {
  errorMessage.value = ''
  emit('close')
}

const handleSubmit = async () => {
  if (!props.despacho?.idDespachoTransporte || !texto.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    await transporteFlotaService.reportarIncidencia(props.despacho.idDespachoTransporte, texto.value)
    showSuccess(`Incidencia asentada en la bitácora de ${props.despacho.codigoTracking}.`)
    emit('saved')
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || err?.message || 'Error al reportar la incidencia.'
  } finally {
    loading.value = false
  }
}
</script>
