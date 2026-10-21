<template>
  <BaseModal
    :visible="visible"
    title="Asignar Recursos al Despacho"
    subtitle="Vincular chofer profesional y tracto/plataforma para salida a ruta"
    icon="pi pi-user-check"
    max-width="540px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <div v-if="despacho" class="despacho-summary mb-4">
        <div class="summary-line">
          <span class="text-xs text-muted">Tracking:</span>
          <span class="font-mono font-semibold">{{ despacho.numeroTracking }}</span>
        </div>
        <div class="summary-line">
          <span class="text-xs text-muted">Expediente:</span>
          <span>{{ despacho.codigoExpediente }}</span>
        </div>
      </div>

      <div class="form-grid">
        <div class="field-item full-width">
          <label for="asig-cond">Conductor Autorizado *</label>
          <select id="asig-cond" v-model="form.idConductor" class="select-input" required>
            <option :value="null" disabled>Seleccione Conductor...</option>
            <option v-for="c in conductoresList" :key="c.idConductor" :value="c.idConductor">
              {{ c.nombres }} {{ c.apellidos }} (Brevete: {{ c.numeroLicencia }} - {{ c.tipoLicencia }})
            </option>
          </select>
        </div>

        <div class="field-item full-width">
          <label for="asig-unid">Unidad de Transporte *</label>
          <select id="asig-unid" v-model="form.idUnidad" class="select-input" required>
            <option :value="null" disabled>Seleccione Tracto...</option>
            <option v-for="u in unidadesList" :key="u.idUnidad" :value="u.idUnidad">
              Placa: {{ u.placaTracto }} {{ u.placaCarreta ? '/ ' + u.placaCarreta : '' }} — {{ u.marcaModelo }}
            </option>
          </select>
        </div>

        <div class="field-item full-width">
          <label for="asig-guia">Guía de Remisión de Transporte (GRE-T) *</label>
          <input
            id="asig-guia"
            v-model.trim="form.guiaRemisionTransporte"
            type="text"
            placeholder="Ej. T001-0004523"
            class="text-input font-mono uppercase"
            required
            maxlength="30"
          />
        </div>

        <div class="field-item full-width">
          <label for="asig-obs">Observaciones / Instrucciones de Asignación</label>
          <input
            id="asig-obs"
            v-model.trim="form.observaciones"
            type="text"
            placeholder="Ej. Asignación para retiro directo de almacén"
            class="text-input"
          />
        </div>
      </div>

      <div class="modal-actions">
        <button type="button" class="btn-modal-cancel" @click="handleClose" :disabled="loading">
          Cancelar
        </button>
        <button type="submit" class="btn-modal-save" :disabled="loading">
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-check"></i>
          <span>Confirmar Asignación</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { transporteFlotaService } from '../services/transporteFlotaService'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  despacho: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')
const conductoresList = ref([])
const unidadesList = ref([])

const form = ref({
  idConductor: null,
  idUnidad: null,
  guiaRemisionTransporte: '',
  observaciones: ''
})

const loadAux = async () => {
  try {
    const [conds, unids] = await Promise.all([
      transporteFlotaService.getConductores(true),
      transporteFlotaService.getUnidades({ soloActivos: true })
    ])
    conductoresList.value = conds || []
    unidadesList.value = unids || []
  } catch (err) {
    console.error('Error cargando recursos de transporte:', err)
  }
}

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      errorMessage.value = ''
      loadAux()
      form.value = {
        idConductor: props.despacho?.idConductor || null,
        idUnidad: props.despacho?.idUnidad || null,
        guiaRemisionTransporte: props.despacho?.guiaRemisionTransporte || '',
        observaciones: ''
      }
    }
  }
)

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  if (!props.despacho) return
  errorMessage.value = ''
  if (!form.value.idConductor || !form.value.idUnidad || !form.value.guiaRemisionTransporte) {
    errorMessage.value = 'Por favor complete el conductor, la unidad y el número de guía de remisión'
    return
  }

  loading.value = true
  try {
    const payload = {
      idConductor: form.value.idConductor,
      idUnidad: form.value.idUnidad,
      guiaRemisionTransporte: form.value.guiaRemisionTransporte.toUpperCase(),
      observaciones: form.value.observaciones || null
    }

    const res = await transporteFlotaService.asignarRecursos(props.despacho.idDespacho, payload)
    emit('saved', res)
    handleClose()
  } catch (err) {
    console.error('Error al asignar recursos:', err)
    errorMessage.value =
      err.response?.data?.message || 'Error al asignar recursos al despacho.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.uppercase {
  text-transform: uppercase;
}
.despacho-summary {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
}
</style>
