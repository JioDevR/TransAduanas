<template>
  <Dialog
    :visible="visible"
    modal
    header="Confirmar Entrega Conforme en Destino"
    :style="{ width: getDialogWidth('580px') }"
    :closable="!loading"
    @update:visible="(v) => !v && handleClose()"
  >
    <form v-if="despacho" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div class="sga-banner is-success">
        <i class="pi pi-check-circle"></i>
        <div>
          <strong class="sga-mono">{{ despacho.codigoTracking }}</strong> · Unidad
          <strong class="sga-mono">{{ despacho.unidadPlacaTracto || '—' }}</strong>
          <div class="sga-cell-sub">
            Destino: {{ despacho.direccionLlegada }} ({{ despacho.distritoLlegada }})
          </div>
        </div>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="entrega-fecha">Fecha y Hora de Recepción *</label>
          <InputText
            id="entrega-fecha"
            v-model="form.fechaEntregaConforme"
            type="datetime-local"
            required
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="entrega-km">
            Kilometraje Llegada
            <span v-if="despacho.unidadKilometrajeActual != null" class="sga-mono">
              (Mín. {{ Number(despacho.unidadKilometrajeActual).toLocaleString('es-PE') }} km)
            </span>
          </label>
          <InputNumber
            inputId="entrega-km"
            v-model="form.kilometrajeLlegada"
            :min="despacho.unidadKilometrajeActual || 0"
            suffix=" km"
            :useGrouping="false"
            fluid
          />
        </div>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="entrega-receptor">Nombre Completo del Receptor *</label>
          <InputText
            id="entrega-receptor"
            v-model.trim="form.receptorNombre"
            placeholder="Ej. Roberto Huamán Cárdenas"
            required
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="entrega-dni">DNI del Receptor *</label>
          <InputText
            id="entrega-dni"
            v-model.trim="form.receptorDni"
            placeholder="8 dígitos"
            maxlength="15"
            required
            fluid
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="entrega-contacto">Teléfono / Contacto en Planta</label>
        <InputText
          id="entrega-contacto"
          v-model.trim="form.contactoRecepcion"
          placeholder="Teléfono o correo del almacén receptor"
          fluid
        />
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="entrega-incidencias">Novedades de Descarga (Opcional)</label>
        <Textarea
          id="entrega-incidencias"
          v-model.trim="form.detalleIncidencia"
          rows="2"
          placeholder="Registrar observaciones de descarga o novedades si las hubiera..."
          fluid
        />
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="entrega-obs">Observaciones de Cierre</label>
        <InputText
          id="entrega-obs"
          v-model.trim="form.observaciones"
          placeholder="Ej. GRE-T firmada y precintos verificados"
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
          label="Confirmar Entrega y Liberar Unidad"
          icon="pi pi-check"
          severity="success"
          :loading="loading"
        />
      </div>
    </form>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { useNotification } from '@/shared/composables/useNotification'
import { getNowDateTimeInput } from '@/shared/utils/dateUtils'
import { transporteFlotaService } from '../../services/transporteFlotaService'

const props = defineProps({
  visible: { type: Boolean, default: false },
  despacho: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()
const { showSuccess } = useNotification()

const loading = ref(false)
const errorMessage = ref('')

const form = ref({
  fechaEntregaConforme: '',
  receptorNombre: '',
  receptorDni: '',
  contactoRecepcion: '',
  kilometrajeLlegada: null,
  detalleIncidencia: '',
  observaciones: ''
})

watch(
  () => [props.visible, props.despacho],
  ([vis, d]) => {
    if (vis && d) {
      errorMessage.value = ''
      form.value = {
        fechaEntregaConforme: getNowDateTimeInput(new Date()),
        receptorNombre: '',
        receptorDni: '',
        contactoRecepcion: d.contactoRecepcion || '',
        kilometrajeLlegada: d.unidadKilometrajeActual ?? null,
        detalleIncidencia: '',
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
  if (!props.despacho?.idDespachoTransporte) return
  loading.value = true
  errorMessage.value = ''
  try {
    const fechaISO = form.value.fechaEntregaConforme?.length === 16
      ? `${form.value.fechaEntregaConforme}:00`
      : form.value.fechaEntregaConforme

    await transporteFlotaService.confirmarEntrega(props.despacho.idDespachoTransporte, {
      fechaEntregaConforme: fechaISO,
      receptorNombre: form.value.receptorNombre,
      receptorDni: form.value.receptorDni,
      contactoRecepcion: form.value.contactoRecepcion || null,
      kilometrajeLlegada: form.value.kilometrajeLlegada,
      detalleIncidencia: form.value.detalleIncidencia || null,
      observaciones: form.value.observaciones || null
    })
    showSuccess(`Entrega confirmada para ${props.despacho.codigoTracking}. Unidad y conductor liberados.`)
    emit('saved')
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || err?.message || 'No se pudo confirmar la entrega.'
  } finally {
    loading.value = false
  }
}
</script>
