<template>
  <div class="puerto-form-view fade-in">
    <CatalogoFormHeader
      :is-edit-mode="isEditMode"
      kicker="CATÁLOGO MAESTRO · NODOS ADUANEROS"
      :subtitle="isEditMode ? `Editando puerto ${form.codigoPuerto}` : 'Registrar nuevo puerto o terminal aduanero'"
      :submit-label="isEditMode ? 'Guardar Cambios' : 'Registrar Puerto'"
      :submitting="submitting"
      :ready="isFormReady"
      @cancel="$emit('cancel')"
      @submit="handleSubmit"
    />

    <div v-if="errorMessage" class="sga-banner is-danger">
      <i class="pi pi-exclamation-circle"></i>
      <span>{{ errorMessage }}</span>
    </div>

    <div class="sga-split-layout">
      <form class="sga-split-main" @submit.prevent="handleSubmit">
        <section class="sga-card-surface sga-form-section">
          <span class="sga-kicker">01 · CÓDIGO UN/LOCODE &amp; IDENTIFICACIÓN</span>
          <div class="sga-grid-2">
            <div class="sga-form-field">
              <label class="sga-form-label" for="puerto-codigo">Código de Puerto / IATA *</label>
              <InputText
                id="puerto-codigo"
                v-model.trim="form.codigoPuerto"
                placeholder="Ej. PECLL, USNYC, CNSHA"
                :disabled="isEditMode || submitting"
                class="sga-mono"
                maxlength="20"
                fluid
                @input="form.codigoPuerto = form.codigoPuerto.toUpperCase()"
              />
              <div v-if="!isEditMode" class="prefijos-row">
                <Button
                  v-for="p in ['PE', 'US', 'CN', 'DE', 'CL']"
                  :key="p"
                  type="button"
                  :label="`+${p}`"
                  size="small"
                  severity="secondary"
                  text
                  @click="applyPrefix(p)"
                />
              </div>
              <small v-else class="field-hint">Código UN/LOCODE inmutable tras el registro.</small>
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="puerto-pais">País / Jurisdicción *</label>
              <InputText
                id="puerto-pais"
                v-model.trim="form.pais"
                placeholder="Ej. Perú, Estados Unidos, China"
                :disabled="submitting"
                maxlength="50"
                fluid
              />
            </div>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="puerto-nombre">Nombre Oficial del Puerto / Terminal *</label>
            <InputText
              id="puerto-nombre"
              v-model.trim="form.nombre"
              placeholder="Ej. Terminal Portuario del Callao"
              :disabled="submitting"
              maxlength="150"
              fluid
            />
          </div>
        </section>

        <section class="sga-card-surface sga-form-section">
          <span class="sga-kicker">02 · MODALIDAD Y ESTADO</span>
          <div class="sga-form-field">
            <label class="sga-form-label">Tipo de Puerto *</label>
            <SelectButton
              v-model="form.tipo"
              :options="tipoOptions"
              optionLabel="label"
              optionValue="value"
              :allowEmpty="false"
              :disabled="submitting"
            />
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label">Estado Operativo *</label>
            <SelectButton
              v-model="form.estado"
              :options="estadoOptions"
              optionLabel="label"
              optionValue="value"
              :allowEmpty="false"
              :disabled="submitting"
            />
          </div>
        </section>
      </form>

      <aside class="sga-split-aside">
        <div class="sga-card-surface sga-preview-card">
          <span class="sga-kicker">VISTA PREVIA UN/LOCODE</span>
          <div class="preview-top">
            <Tag :value="form.codigoPuerto || 'LOCODE'" severity="contrast" class="sga-mono" />
            <Tag :value="form.estado" :severity="getCatalogoEstadoSeverity(form.estado)" rounded />
          </div>
          <p class="preview-title">{{ form.nombre || 'Nombre del Puerto' }}</p>
          <p class="preview-sub">
            <i :class="getPuertoTipoIcon(form.tipo)"></i> {{ formatPuertoTipo(form.tipo) }} · {{ form.pais || 'País' }}
          </p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'
import catalogoService from '../services/catalogoService'
import CatalogoFormHeader from './CatalogoFormHeader.vue'
import {
  formatPuertoTipo,
  getPuertoTipoIcon,
  getCatalogoEstadoSeverity
} from '@/features/gerencia/utils/gerenciaFormatters'

const props = defineProps({
  puertoToEdit: { type: Object, default: null }
})

const emit = defineEmits(['cancel', 'saved'])

const submitting = ref(false)
const errorMessage = ref('')

const tipoOptions = [
  { value: 'MARITIMO', label: 'Marítimo' },
  { value: 'AEREO', label: 'Aéreo' },
  { value: 'TERRESTRE', label: 'Terrestre' },
  { value: 'FLUVIAL', label: 'Fluvial' }
]

const estadoOptions = [
  { value: 'ACTIVO', label: 'Activo' },
  { value: 'INACTIVO', label: 'Inactivo' }
]

const form = reactive({
  codigoPuerto: '',
  nombre: '',
  pais: 'Perú',
  tipo: 'MARITIMO',
  estado: 'ACTIVO'
})

const isEditMode = computed(() => Boolean(props.puertoToEdit?.idPuerto))

watch(
  () => props.puertoToEdit,
  (val) => {
    errorMessage.value = ''
    if (val?.idPuerto) {
      form.codigoPuerto = val.codigoPuerto || ''
      form.nombre = val.nombre || ''
      form.pais = val.pais || ''
      form.tipo = val.tipo || val.tipoPuerto || 'MARITIMO'
      form.estado = val.estado || 'ACTIVO'
    } else {
      form.codigoPuerto = ''
      form.nombre = ''
      form.pais = 'Perú'
      form.tipo = 'MARITIMO'
      form.estado = 'ACTIVO'
    }
  },
  { immediate: true }
)

const applyPrefix = (prefix) => {
  if (!form.codigoPuerto.startsWith(prefix)) {
    form.codigoPuerto = prefix
  }
}

const isFormReady = computed(
  () => Boolean(form.codigoPuerto && form.nombre && form.pais && form.tipo)
)

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!isFormReady.value) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*).'
    return
  }

  submitting.value = true
  try {
    const payload = {
      codigoPuerto: form.codigoPuerto.trim().toUpperCase(),
      nombre: form.nombre.trim(),
      pais: form.pais.trim(),
      tipo: form.tipo,
      estado: form.estado
    }
    const result = isEditMode.value
      ? await catalogoService.updatePuerto(props.puertoToEdit.idPuerto, payload)
      : await catalogoService.createPuerto(payload)

    emit('saved', { isEdit: isEditMode.value, data: result?.data || result })
  } catch (err) {
    errorMessage.value =
      err?.response?.data?.message ||
      'No se pudo guardar el puerto. Verifique que el código no esté duplicado.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.puerto-form-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-form-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1rem;
}

.prefijos-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.field-hint {
  color: var(--sga-text-muted);
  font-size: 0.78rem;
}

.sga-preview-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.preview-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.preview-title {
  margin: 0;
  font-weight: 700;
  font-size: 1.05rem;
  color: var(--sga-text-primary);
}

.preview-sub {
  margin: 0;
  font-size: 0.85rem;
  color: var(--sga-text-muted);
}
</style>
