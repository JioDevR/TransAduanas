<template>
  <Dialog
    :visible="visible"
    modal
    :style="{ width: 'min(620px, 95vw)' }"
    :header="isEditMode ? `Completar Datos — ${expediente?.codigoExpediente || ''}` : 'Generar Expediente Simple'"
    @update:visible="(val) => { if (!val) $emit('close') }"
  >
    <form class="simple-exp-form" @submit.prevent="handleSubmit">
      <div v-if="!isEditMode" class="sga-form-field">
        <label for="simple-cot" class="sga-form-label">Cotización Aprobada con Anticipo Validado *</label>
        <Select
          id="simple-cot"
          v-model="form.idCotizacion"
          :options="cotizacionOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Seleccione una cotización lista"
          size="small"
          fluid
        />
      </div>

      <div class="two-col">
        <div class="sga-form-field">
          <label for="simple-contacto-nombre" class="sga-form-label">Contacto Exterior (Nombre / Empresa) *</label>
          <InputText
            id="simple-contacto-nombre"
            v-model.trim="form.contactoExteriorNombre"
            placeholder="Ej. Shenzhen Mobile Trading Co."
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="simple-contacto-tel" class="sga-form-label">Teléfono Contacto Exterior</label>
          <InputText
            id="simple-contacto-tel"
            v-model.trim="form.contactoExteriorTelefono"
            placeholder="Ej. +86 755 8899 1122"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="simple-contacto-mail" class="sga-form-label">Correo Contacto Exterior</label>
          <InputText
            id="simple-contacto-mail"
            v-model.trim="form.contactoExteriorCorreo"
            type="email"
            placeholder="export@supplier.com"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="simple-contenedor" class="sga-form-label">N° Contenedor / Guía (Opcional)</label>
          <InputText
            id="simple-contenedor"
            v-model.trim="form.numeroContenedor"
            placeholder="Ej. MSKU9087651"
            size="small"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label for="simple-nave" class="sga-form-label">Nave / Aerolínea (Opcional)</label>
          <InputText
            id="simple-nave"
            v-model.trim="form.naveViaje"
            placeholder="Ej. MSC CAPELLA / LATAM CARGO"
            size="small"
            fluid
          />
        </div>

        <template v-if="!isEditMode">
          <div class="sga-form-field">
            <label for="simple-etd" class="sga-form-label">Fecha Zarpe Estimada (ETD)</label>
            <InputText id="simple-etd" v-model="form.fechaZarpeEtd" type="date" size="small" fluid />
          </div>
          <div class="sga-form-field">
            <label for="simple-eta" class="sga-form-label">Fecha Llegada Estimada (ETA)</label>
            <InputText id="simple-eta" v-model="form.fechaLlegadaEta" type="date" size="small" fluid />
          </div>
        </template>
      </div>

      <div class="sga-form-field">
        <label for="simple-obs" class="sga-form-label">Instrucciones o Datos Adicionales</label>
        <Textarea
          id="simple-obs"
          v-model.trim="form.observaciones"
          rows="3"
          placeholder="Dirección de recojo/entrega, notas de embalaje o referencias del proveedor exterior..."
          fluid
        />
      </div>

      <div class="modal-actions">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          :disabled="saving"
          @click="$emit('close')"
        />
        <Button
          type="submit"
          :label="isEditMode ? 'Guardar Datos' : 'Generar Expediente'"
          icon="pi pi-check"
          size="small"
          :loading="saving"
          :disabled="!canSubmit"
        />
      </div>
    </form>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null },
  cotizacionesElegibles: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'submit'])

const isEditMode = computed(() => Boolean(props.expediente?.idExpediente))

const form = ref({
  idCotizacion: null,
  contactoExteriorNombre: '',
  contactoExteriorTelefono: '',
  contactoExteriorCorreo: '',
  numeroContenedor: '',
  naveViaje: '',
  fechaZarpeEtd: '',
  fechaLlegadaEta: '',
  observaciones: ''
})

const cotizacionOptions = computed(() =>
  (props.cotizacionesElegibles || []).map((c) => ({
    value: c.idCotizacion,
    label: `${c.codigoCotizacion} — ${c.nombreMercancia || 'Mercancía'} ($ ${Number(c.totalCotizado || 0).toFixed(2)})`
  }))
)

const canSubmit = computed(() => {
  if (!form.value.contactoExteriorNombre) return false
  if (!isEditMode.value && !form.value.idCotizacion) return false
  return true
})

watch(
  () => [props.visible, props.expediente, props.cotizacionesElegibles],
  ([vis]) => {
    if (!vis) return
    if (props.expediente) {
      form.value = {
        idCotizacion: props.expediente.idCotizacion || null,
        contactoExteriorNombre: props.expediente.contactoExteriorNombre || '',
        contactoExteriorTelefono: props.expediente.contactoExteriorTelefono || '',
        contactoExteriorCorreo: props.expediente.contactoExteriorCorreo || '',
        numeroContenedor: props.expediente.numeroContenedor || '',
        naveViaje: props.expediente.naveViaje || '',
        fechaZarpeEtd: '',
        fechaLlegadaEta: '',
        observaciones: props.expediente.observaciones || ''
      }
    } else {
      const firstId = props.cotizacionesElegibles?.[0]?.idCotizacion || null
      form.value = {
        idCotizacion: firstId,
        contactoExteriorNombre: '',
        contactoExteriorTelefono: '',
        contactoExteriorCorreo: '',
        numeroContenedor: '',
        naveViaje: '',
        fechaZarpeEtd: '',
        fechaLlegadaEta: '',
        observaciones: ''
      }
    }
  },
  { immediate: true }
)

function handleSubmit() {
  if (!canSubmit.value) return
  emit('submit', {
    isEdit: isEditMode.value,
    idExpediente: props.expediente?.idExpediente || null,
    payload: {
      idCotizacion: form.value.idCotizacion,
      contactoExteriorNombre: form.value.contactoExteriorNombre,
      contactoExteriorTelefono: form.value.contactoExteriorTelefono || null,
      contactoExteriorCorreo: form.value.contactoExteriorCorreo || null,
      numeroContenedor: form.value.numeroContenedor || null,
      naveViaje: form.value.naveViaje || null,
      fechaZarpeEtd: form.value.fechaZarpeEtd || null,
      fechaLlegadaEta: form.value.fechaLlegadaEta || null,
      observaciones: form.value.observaciones || null
    }
  })
}
</script>

<style scoped>
.simple-exp-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
  padding-top: 0.5rem;
}

@media (max-width: 640px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
