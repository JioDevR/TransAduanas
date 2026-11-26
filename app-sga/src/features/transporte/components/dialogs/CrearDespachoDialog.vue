<template>
  <Dialog
    :visible="visible"
    modal
    header="Programar Nuevo Despacho Terrestre"
    :style="{ width: getDialogWidth('680px') }"
    :closable="!loading"
    @update:visible="(v) => !v && handleClose()"
  >
    <form class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="crear-tipo-traslado">Tipo de Traslado *</label>
          <Select
            inputId="crear-tipo-traslado"
            v-model="form.tipoTraslado"
            :options="TIPO_TRASLADO_OPTIONS"
            optionLabel="label"
            optionValue="value"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="crear-fecha-prog">Fecha y Hora Programada *</label>
          <InputText
            id="crear-fecha-prog"
            v-model="form.fechaProgramada"
            type="datetime-local"
            required
            fluid
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="crear-expediente">Expediente Aduanero Disponible *</label>
        <Select
          inputId="crear-expediente"
          v-model="form.idExpediente"
          :options="expedientesDisponibles"
          optionLabel="label"
          optionValue="value"
          :loading="loadingExpedientes"
          filter
          placeholder="Seleccione expediente sin despacho activo..."
          emptyMessage="No hay expedientes elegibles sin despacho activo"
          fluid
          @change="onExpedienteChange"
        />
        <small v-if="form.tipoTraslado === 'ENTREGA_IMPORTACION'" class="sga-cell-sub">
          Solo se muestran expedientes con Levante Autorizado y sin despacho activo.
        </small>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="crear-conductor">Conductor Asignado (Opcional)</label>
          <Select
            inputId="crear-conductor"
            v-model="form.idConductor"
            :options="conductorOptions"
            optionLabel="label"
            optionValue="value"
            optionDisabled="disabled"
            showClear
            placeholder="Dejar sin asignar (Pendiente)"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="crear-unidad">Unidad de Transporte (Opcional)</label>
          <Select
            inputId="crear-unidad"
            v-model="form.idUnidad"
            :options="unidadOptions"
            optionLabel="label"
            optionValue="value"
            optionDisabled="disabled"
            showClear
            placeholder="Dejar sin asignar (Pendiente)"
            fluid
          />
        </div>
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="crear-gret">Guía de Remisión Transportista (GRE-T)</label>
          <InputText
            id="crear-gret"
            v-model.trim="form.guiaRemisionTransporte"
            placeholder="Ej. GRT-001-000145"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="crear-distrito">Distrito de Destino *</label>
          <InputText
            id="crear-distrito"
            v-model.trim="form.distritoLlegada"
            placeholder="Ej. Callao / Lurín / Ate"
            required
            fluid
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="crear-partida">Dirección de Partida / Recojo *</label>
        <InputText
          id="crear-partida"
          v-model.trim="form.direccionPartida"
          placeholder="Ej. Depósito Aduanero DP World Callao"
          required
          fluid
        />
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="crear-llegada">Dirección de Llegada / Entrega *</label>
        <InputText
          id="crear-llegada"
          v-model.trim="form.direccionLlegada"
          placeholder="Ej. Planta Industrial / Terminal Portuario"
          required
          fluid
        />
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="crear-contacto">Contacto en Destino</label>
          <InputText
            id="crear-contacto"
            v-model.trim="form.contactoRecepcion"
            placeholder="Nombre y teléfono de contacto"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="crear-obs">Observaciones</label>
          <InputText
            id="crear-obs"
            v-model.trim="form.observaciones"
            placeholder="Instrucciones especiales de carga o acceso"
            fluid
          />
        </div>
      </div>

      <div v-if="parcialAsignacion" class="sga-banner is-warn">
        <i class="pi pi-exclamation-triangle"></i>
        <span>Para programar directamente en estado Programado debe seleccionar tanto conductor como unidad, o dejar ambos vacíos.</span>
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
          label="Programar Despacho"
          icon="pi pi-check"
          :loading="loading"
          :disabled="parcialAsignacion || !form.idExpediente"
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
import { useResponsive } from '@/shared/composables/useResponsive'
import { useNotification } from '@/shared/composables/useNotification'
import { getNowDateTimeInput } from '@/shared/utils/dateUtils'
import { expedienteService } from '../../services/expedienteService'
import { transporteFlotaService } from '../../services/transporteFlotaService'
import { TIPO_TRASLADO_OPTIONS } from '../../utils/transporteFormatters'
import { isFechaVencida } from '../../utils/transporteAlerts'

const props = defineProps({
  visible: { type: Boolean, default: false },
  unidades: { type: Array, default: () => [] },
  conductores: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'saved'])
const { getDialogWidth } = useResponsive()
const { showSuccess } = useNotification()

const loading = ref(false)
const loadingExpedientes = ref(false)
const errorMessage = ref('')
const expedientesRaw = ref([])

const form = ref({
  idExpediente: null,
  tipoTraslado: 'ENTREGA_IMPORTACION',
  idConductor: null,
  idUnidad: null,
  guiaRemisionTransporte: '',
  fechaProgramada: '',
  direccionPartida: '',
  direccionLlegada: '',
  distritoLlegada: '',
  contactoRecepcion: '',
  observaciones: ''
})

const parcialAsignacion = computed(() => {
  const hasC = Boolean(form.value.idConductor)
  const hasU = Boolean(form.value.idUnidad)
  return hasC !== hasU
})

const expedientesDisponibles = computed(() => {
  const isImp = form.value.tipoTraslado === 'ENTREGA_IMPORTACION'
  return expedientesRaw.value
    .filter((e) => {
      if (e.idDespachoTransporte) return false
      const est = String(e.estadoExpediente || '').toUpperCase()
      if (est === 'ANULADO' || est === 'CERRADO') return false
      if (isImp) return est === 'LEVANTE_AUTORIZADO'
      return true
    })
    .map((e) => ({
      value: e.idExpediente,
      label: `${e.codigoExpediente} — ${e.clienteRazonSocial || 'Cliente'} (${e.estadoExpediente})`,
      raw: e
    }))
})

const conductorOptions = computed(() =>
  props.conductores
    .filter((c) => c.estadoActivo !== false)
    .map((c) => {
      const licVencida = isFechaVencida(c.fechaVencimientoLicencia)
      const disponible = String(c.estadoOperativo || '').toUpperCase() === 'DISPONIBLE'
      const razones = []
      if (!disponible) razones.push(c.estadoOperativo)
      if (licVencida) razones.push('Lic. Vencida')
      return {
        value: c.idConductor,
        label: `${c.nombreCompleto || `${c.nombres} ${c.apellidos}`} · ${c.tipoLicencia}${razones.length ? ` [${razones.join(', ')}]` : ''}`,
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
        label: `${u.placaTracto} · ${u.marcaModelo} (${u.tipoVehiculo})${razones.length ? ` [${razones.join(', ')}]` : ''}`,
        disabled: !disponible || soatVenc || citvVenc
      }
    })
)

const resetForm = () => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  tomorrow.setHours(8, 0, 0, 0)

  form.value = {
    idExpediente: null,
    tipoTraslado: 'ENTREGA_IMPORTACION',
    idConductor: null,
    idUnidad: null,
    guiaRemisionTransporte: '',
    fechaProgramada: getNowDateTimeInput(tomorrow),
    direccionPartida: '',
    direccionLlegada: '',
    distritoLlegada: '',
    contactoRecepcion: '',
    observaciones: ''
  }
  errorMessage.value = ''
}

const loadExpedientes = async () => {
  loadingExpedientes.value = true
  try {
    const data = await expedienteService.getExpedientes(true)
    expedientesRaw.value = Array.isArray(data) ? data : []
  } catch {
    errorMessage.value = 'No se pudo cargar la lista de expedientes.'
  } finally {
    loadingExpedientes.value = false
  }
}

const onExpedienteChange = () => {
  const item = expedientesDisponibles.value.find((x) => x.value === form.value.idExpediente)?.raw
  if (!item) return
  if (form.value.tipoTraslado === 'ENTREGA_IMPORTACION') {
    if (!form.value.direccionPartida) {
      form.value.direccionPartida = item.puertoDestinoNombre
        ? `Terminal Portuario ${item.puertoDestinoNombre}`
        : 'Terminal Portuario Callao'
    }
    if (!form.value.distritoLlegada) {
      form.value.distritoLlegada = item.destinoProvincia || 'Lima'
    }
  } else {
    if (!form.value.direccionLlegada) {
      form.value.direccionLlegada = item.puertoEnvioNombre
        ? `Terminal Portuario ${item.puertoEnvioNombre}`
        : 'Terminal Portuario Callao'
    }
    if (!form.value.distritoLlegada) {
      form.value.distritoLlegada = 'Callao'
    }
  }
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      resetForm()
      loadExpedientes()
    }
  }
)

const handleClose = () => {
  errorMessage.value = ''
  emit('close')
}

const handleSubmit = async () => {
  if (parcialAsignacion.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const fechaISO = form.value.fechaProgramada?.length === 16
      ? `${form.value.fechaProgramada}:00`
      : form.value.fechaProgramada

    await transporteFlotaService.createDespacho({
      idExpediente: form.value.idExpediente,
      tipoTraslado: form.value.tipoTraslado,
      idConductor: form.value.idConductor || null,
      idUnidad: form.value.idUnidad || null,
      guiaRemisionTransporte: form.value.guiaRemisionTransporte || null,
      fechaProgramada: fechaISO,
      direccionPartida: form.value.direccionPartida,
      direccionLlegada: form.value.direccionLlegada,
      distritoLlegada: form.value.distritoLlegada,
      contactoRecepcion: form.value.contactoRecepcion || null,
      observaciones: form.value.observaciones || null
    })
    showSuccess('Despacho de transporte programado exitosamente.')
    emit('saved')
    handleClose()
  } catch (err) {
    errorMessage.value = err?.response?.data?.message || err?.message || 'Error al programar el despacho.'
  } finally {
    loading.value = false
  }
}
</script>
