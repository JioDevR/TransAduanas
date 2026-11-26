<template>
  <Dialog
    :visible="visible"
    modal
    :header="esReasignacion ? 'Reasignar Recursos de Transporte' : 'Asignar Conductor y Unidad MTC'"
    :style="{ width: getDialogWidth('560px') }"
    :closable="!loading"
    @update:visible="(v) => !v && handleClose()"
  >
    <form v-if="despacho" class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div class="sga-banner is-info">
        <i class="pi pi-truck"></i>
        <div>
          <strong class="sga-mono">{{ despacho.codigoTracking }}</strong> ·
          <span class="sga-mono">{{ despacho.codigoExpediente || 'EXP' }}</span>
          <div class="sga-cell-sub">
            {{ despacho.clienteRazonSocial || 'Cliente' }} → {{ despacho.distritoLlegada }} ({{ despacho.direccionLlegada }})
          </div>
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="asignar-conductor">Conductor Profesional (MTC) *</label>
        <Select
          inputId="asignar-conductor"
          v-model="form.idConductor"
          :options="conductorOptions"
          optionLabel="label"
          optionValue="value"
          optionDisabled="disabled"
          filter
          placeholder="Seleccione un conductor disponible con brevete vigente..."
          fluid
        />
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="asignar-unidad">Unidad Vehicular (Tracto / Furgón) *</label>
        <Select
          inputId="asignar-unidad"
          v-model="form.idUnidad"
          :options="unidadOptions"
          optionLabel="label"
          optionValue="value"
          optionDisabled="disabled"
          filter
          placeholder="Seleccione una unidad disponible con SOAT y CITV vigentes..."
          fluid
        />
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="asignar-gret">Guía de Remisión (GRE-T)</label>
          <InputText
            id="asignar-gret"
            v-model.trim="form.guiaRemisionTransporte"
            placeholder="Ej. GRT-001-000148"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="asignar-fecha">Fecha Programada</label>
          <InputText
            id="asignar-fecha"
            v-model="form.fechaProgramada"
            type="datetime-local"
            fluid
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="asignar-obs">Observaciones de Despacho</label>
        <Textarea
          id="asignar-obs"
          v-model.trim="form.observaciones"
          rows="2"
          placeholder="Indicaciones de carga, precintos o ventanas horarias..."
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
          :label="esReasignacion ? 'Guardar Reasignación' : 'Confirmar Asignación'"
          icon="pi pi-check"
          :loading="loading"
          :disabled="!form.idConductor || !form.idUnidad"
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
import { getNowDateTimeInput } from '@/shared/utils/dateUtils'
import { transporteFlotaService } from '../../services/transporteFlotaService'
import { isFechaVencida } from '../../utils/transporteAlerts'

const props = defineProps({
  visible: { type: Boolean, default: false },
  despacho: { type: Object, default: null },
  unidades: { type: Array, default: () => [] },
  conductores: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()
const { showSuccess } = useNotification()

const loading = ref(false)
const errorMessage = ref('')

const form = ref({
  idConductor: null,
  idUnidad: null,
  guiaRemisionTransporte: '',
  fechaProgramada: '',
  observaciones: ''
})

const esReasignacion = computed(() => props.despacho?.estadoTransporte === 'PROGRAMADO')

const conductorOptions = computed(() =>
  props.conductores
    .filter((c) => c.estadoActivo !== false)
    .map((c) => {
      const licVencida = isFechaVencida(c.fechaVencimientoLicencia)
      const disponible = String(c.estadoOperativo || '').toUpperCase() === 'DISPONIBLE'
      const razones = []
      if (!disponible) razones.push(c.estadoOperativo)
      if (licVencida) razones.push('Licencia Vencida')
      return {
        value: c.idConductor,
        label: `${c.nombreCompleto || `${c.nombres} ${c.apellidos}`} (${c.tipoLicencia})${razones.length ? ` — BLOQUEADO: ${razones.join(', ')}` : ''}`,
        disabled: !disponible || licVencida
      }
    })
)

const unidadOptions = computed(() =>
  props.unidades
    .filter((u) => u.estadoActivo !== false)
    .map((u) => {
      const soatVenc = isFechaVencida(u.soatVencimiento)
      const citvVenc = isFechaVencida(u.citvVencimiento)
      const disponible = String(u.estadoOperativo || '').toUpperCase() === 'DISPONIBLE'
      const razones = []
      if (!disponible) razones.push(u.estadoOperativo)
      if (soatVenc) razones.push('SOAT Vencido')
      if (citvVenc) razones.push('CITV Vencido')
      return {
        value: u.idUnidad,
        label: `${u.placaTracto} · ${u.marcaModelo} (${u.tipoVehiculo})${razones.length ? ` — BLOQUEADO: ${razones.join(', ')}` : ''}`,
        disabled: !disponible || soatVenc || citvVenc
      }
    })
)

watch(
  () => [props.visible, props.despacho],
  ([vis, d]) => {
    if (vis && d) {
      errorMessage.value = ''
      form.value = {
        idConductor: d.idConductor || null,
        idUnidad: d.idUnidad || null,
        guiaRemisionTransporte: d.guiaRemisionTransporte || '',
        fechaProgramada: d.fechaProgramada ? getNowDateTimeInput(d.fechaProgramada) : '',
        observaciones: d.observaciones || ''
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
    const fechaISO = form.value.fechaProgramada
      ? (form.value.fechaProgramada.length === 16 ? `${form.value.fechaProgramada}:00` : form.value.fechaProgramada)
      : null

    await transporteFlotaService.asignarRecursos(props.despacho.idDespachoTransporte, {
      idConductor: form.value.idConductor,
      idUnidad: form.value.idUnidad,
      guiaRemisionTransporte: form.value.guiaRemisionTransporte || null,
      fechaProgramada: fechaISO,
      observaciones: form.value.observaciones || null
    })
    showSuccess('Recursos asignados correctamente. El despacho pasó a PROGRAMADO.')
    emit('saved')
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || err?.message || 'Error al asignar recursos.'
  } finally {
    loading.value = false
  }
}
</script>
