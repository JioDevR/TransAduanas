<template>
  <Dialog
    :visible="visible"
    modal
    :header="esEdicion ? `Editar Conductor · ${conductor?.nombres} ${conductor?.apellidos}` : 'Registrar Nuevo Conductor MTC'"
    :style="{ width: getDialogWidth('600px') }"
    :closable="!loading"
    @update:visible="(v) => !v && handleClose()"
  >
    <form class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="cond-nombres">Nombres *</label>
          <InputText
            id="cond-nombres"
            v-model.trim="form.nombres"
            placeholder="Ej. Raúl Alberto"
            required
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="cond-apellidos">Apellidos *</label>
          <InputText
            id="cond-apellidos"
            v-model.trim="form.apellidos"
            placeholder="Ej. Cárdenas Meléndez"
            required
            fluid
          />
        </div>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="cond-dni">DNI / Carné de Extranjería</label>
          <InputText
            id="cond-dni"
            v-model.trim="form.numeroDocumento"
            placeholder="8 a 15 caracteres"
            maxlength="15"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="cond-telefono">Teléfono de Contacto</label>
          <InputText
            id="cond-telefono"
            v-model.trim="form.telefono"
            placeholder="Ej. +51 987 667 001"
            maxlength="20"
            fluid
          />
        </div>
      </div>

      <div class="sga-grid-3">
        <div class="sga-form-field">
          <label class="sga-form-label" for="cond-licencia">N° Licencia MTC *</label>
          <InputText
            id="cond-licencia"
            v-model.trim="form.numeroLicencia"
            placeholder="Ej. Q60123456"
            maxlength="20"
            required
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="cond-cat">Categoría MTC *</label>
          <Select
            inputId="cond-cat"
            v-model="form.tipoLicencia"
            :options="CATEGORIA_LICENCIA_OPTIONS"
            optionLabel="label"
            optionValue="value"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="cond-venc">Vencimiento Licencia</label>
          <InputText
            id="cond-venc"
            v-model="form.fechaVencimientoLicencia"
            type="date"
            fluid
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="cond-estado">Estado Operativo</label>
        <Select
          inputId="cond-estado"
          v-model="form.estadoOperativo"
          :options="ESTADO_CONDUCTOR_OPTIONS"
          optionLabel="label"
          optionValue="value"
          fluid
        />
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="cond-obs">Observaciones / Certificaciones</label>
        <Textarea
          id="cond-obs"
          v-model.trim="form.observaciones"
          rows="2"
          placeholder="Certificación MATPEL, experiencia en rutas interprovinciales, cadena de frío..."
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
          :label="esEdicion ? 'Guardar Cambios' : 'Registrar Conductor'"
          icon="pi pi-check"
          :loading="loading"
        />
      </div>
    </form>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import { useResponsive } from '@/shared/composables/useResponsive'
import { useNotification } from '@/shared/composables/useNotification'
import { transporteFlotaService } from '../../services/transporteFlotaService'
import { CATEGORIA_LICENCIA_OPTIONS, ESTADO_CONDUCTOR_OPTIONS } from '../../utils/transporteFormatters'

const props = defineProps({
  visible: { type: Boolean, default: false },
  conductor: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()
const { showSuccess } = useNotification()

const loading = ref(false)
const errorMessage = ref('')

const esEdicion = computed(() => Boolean(props.conductor?.idConductor))

const form = ref({
  nombres: '',
  apellidos: '',
  numeroDocumento: '',
  telefono: '',
  numeroLicencia: '',
  tipoLicencia: 'A-IIIc',
  fechaVencimientoLicencia: '',
  estadoOperativo: 'DISPONIBLE',
  observaciones: ''
})

watch(
  () => [props.visible, props.conductor],
  ([vis, c]) => {
    if (!vis) return
    errorMessage.value = ''
    if (c?.idConductor) {
      form.value = {
        nombres: c.nombres || '',
        apellidos: c.apellidos || '',
        numeroDocumento: c.numeroDocumento || '',
        telefono: c.telefono || '',
        numeroLicencia: c.numeroLicencia || '',
        tipoLicencia: c.tipoLicencia || 'A-IIIc',
        fechaVencimientoLicencia: c.fechaVencimientoLicencia
          ? String(c.fechaVencimientoLicencia).slice(0, 10)
          : '',
        estadoOperativo: c.estadoOperativo || 'DISPONIBLE',
        observaciones: c.observaciones || ''
      }
    } else {
      form.value = {
        nombres: '',
        apellidos: '',
        numeroDocumento: '',
        telefono: '',
        numeroLicencia: '',
        tipoLicencia: 'A-IIIc',
        fechaVencimientoLicencia: '',
        estadoOperativo: 'DISPONIBLE',
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
  loading.value = true
  errorMessage.value = ''
  try {
    const payload = {
      nombres: form.value.nombres,
      apellidos: form.value.apellidos,
      numeroDocumento: form.value.numeroDocumento || null,
      telefono: form.value.telefono || null,
      numeroLicencia: form.value.numeroLicencia.toUpperCase(),
      tipoLicencia: form.value.tipoLicencia,
      fechaVencimientoLicencia: form.value.fechaVencimientoLicencia || null,
      estadoOperativo: form.value.estadoOperativo,
      estadoActivo: props.conductor?.estadoActivo ?? true,
      observaciones: form.value.observaciones || null
    }

    if (esEdicion.value) {
      await transporteFlotaService.updateConductor(props.conductor.idConductor, payload)
      showSuccess(`Conductor ${payload.nombres} ${payload.apellidos} actualizado correctamente.`)
    } else {
      await transporteFlotaService.createConductor(payload)
      showSuccess(`Conductor ${payload.nombres} ${payload.apellidos} registrado en el padrón MTC.`)
    }
    emit('saved')
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || err?.message || 'Error al guardar el conductor.'
  } finally {
    loading.value = false
  }
}
</script>
