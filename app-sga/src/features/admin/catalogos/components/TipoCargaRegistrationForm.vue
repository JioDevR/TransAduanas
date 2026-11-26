<template>
  <div class="tipo-carga-form-view fade-in">
    <CatalogoFormHeader
      :is-edit-mode="isEditMode"
      kicker="CATÁLOGO MAESTRO · CLASIFICACIÓN DE CARGA"
      :subtitle="isEditMode ? `Editando tipo de carga ${form.codigo}` : 'Registrar nueva clasificación de tipo de carga'"
      :submit-label="isEditMode ? 'Guardar Cambios' : 'Registrar Tipo de Carga'"
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
          <span class="sga-kicker">01 · CÓDIGO &amp; DENOMINACIÓN OFICIAL</span>
          <div class="sga-grid-2">
            <div class="sga-form-field">
              <label class="sga-form-label" for="tc-codigo">Código Estándar *</label>
              <InputText
                id="tc-codigo"
                v-model.trim="form.codigo"
                placeholder="Ej. IMP-FCL-40"
                :disabled="isEditMode || submitting"
                class="sga-mono"
                maxlength="30"
                fluid
                @input="handleCodeInput"
              />
              <div v-if="!isEditMode" class="prefijos-row">
                <Button
                  v-for="p in ['IMP-', 'EXP-', 'GEN-', 'ESP-']"
                  :key="p"
                  type="button"
                  :label="`+${p}`"
                  size="small"
                  severity="secondary"
                  text
                  @click="applyCodePrefix(p)"
                />
              </div>
              <small v-else class="field-hint">Código único inmutable en edición.</small>
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="tc-nombre">Nombre Comercial / Aduanero *</label>
              <InputText
                id="tc-nombre"
                v-model.trim="form.nombre"
                placeholder="Ej. Contenedor Completo FCL 40'"
                :disabled="submitting"
                maxlength="100"
                fluid
              />
            </div>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="tc-descripcion">Descripción y Alcance Operativo</label>
            <Textarea
              id="tc-descripcion"
              v-model="form.descripcion"
              rows="4"
              maxlength="255"
              placeholder="Indique criterios operativos, restricciones o casos de uso en cotizaciones y despachos..."
              :disabled="submitting"
              fluid
            />
            <small class="field-hint">{{ (form.descripcion || '').length }}/255 caracteres</small>
          </div>
        </section>

        <section class="sga-card-surface sga-form-section">
          <span class="sga-kicker">02 · ESTADO EN CATÁLOGO</span>
          <SelectButton
            v-model="form.estado"
            :options="estadoOptions"
            optionLabel="label"
            optionValue="value"
            :allowEmpty="false"
            :disabled="submitting"
          />
        </section>
      </form>

      <aside class="sga-split-aside">
        <div class="sga-card-surface sga-preview-card">
          <span class="sga-kicker">VISTA PREVIA DEL CATÁLOGO</span>
          <div class="preview-top">
            <Tag :value="form.codigo || 'CÓDIGO'" severity="contrast" class="sga-mono" />
            <Tag
              :value="getTipoCargaRegimenLabel(form.codigo)"
              :severity="getTipoCargaRegimenSeverity(form.codigo)"
              rounded
            />
          </div>
          <p class="preview-title">{{ form.nombre || 'Nombre de Clasificación' }}</p>
          <p class="preview-desc">{{ form.descripcion || 'Sin descripción detallada.' }}</p>
          <div class="preview-footer">
            <span>Estado:</span>
            <Tag :value="form.estado" :severity="getCatalogoEstadoSeverity(form.estado)" rounded />
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'
import catalogoService from '../services/catalogoService'
import CatalogoFormHeader from './CatalogoFormHeader.vue'
import {
  getTipoCargaRegimenLabel,
  getTipoCargaRegimenSeverity,
  getCatalogoEstadoSeverity
} from '@/features/gerencia/utils/gerenciaFormatters'

const props = defineProps({
  tipoToEdit: { type: Object, default: null }
})

const emit = defineEmits(['cancel', 'saved'])

const submitting = ref(false)
const errorMessage = ref('')

const estadoOptions = [
  { value: 'ACTIVO', label: 'Activo' },
  { value: 'INACTIVO', label: 'Inactivo' }
]

const form = reactive({
  codigo: '',
  nombre: '',
  descripcion: '',
  estado: 'ACTIVO'
})

const isEditMode = computed(() => Boolean(props.tipoToEdit?.idTipoCarga))

watch(
  () => props.tipoToEdit,
  (val) => {
    errorMessage.value = ''
    if (val?.idTipoCarga) {
      form.codigo = val.codigo || ''
      form.nombre = val.nombre || ''
      form.descripcion = val.descripcion || ''
      form.estado = val.estado || 'ACTIVO'
    } else {
      form.codigo = ''
      form.nombre = ''
      form.descripcion = ''
      form.estado = 'ACTIVO'
    }
  },
  { immediate: true }
)

const handleCodeInput = () => {
  form.codigo = String(form.codigo || '').toUpperCase().replace(/\s+/g, '_')
}

const applyCodePrefix = (prefix) => {
  if (!form.codigo) {
    form.codigo = prefix
  } else if (!form.codigo.startsWith(prefix)) {
    form.codigo = prefix + form.codigo.replace(/^[A-Z0-9]+-/, '')
  }
}

const isFormReady = computed(
  () => Boolean(form.codigo.trim().length >= 3 && form.nombre.trim().length >= 3 && form.estado)
)

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!isFormReady.value) {
    errorMessage.value = 'Complete el código y nombre (mínimo 3 caracteres cada uno).'
    return
  }

  submitting.value = true
  try {
    const payload = {
      codigo: form.codigo.trim().toUpperCase(),
      nombre: form.nombre.trim(),
      descripcion: form.descripcion ? form.descripcion.trim() : null,
      estado: form.estado
    }
    const result = isEditMode.value
      ? await catalogoService.updateTipoCarga(props.tipoToEdit.idTipoCarga, payload)
      : await catalogoService.createTipoCarga(payload)

    emit('saved', { isEdit: isEditMode.value, data: result?.data || result })
  } catch (err) {
    errorMessage.value =
      err?.response?.data?.message ||
      'No se pudo guardar el tipo de carga. Verifique que el código no esté duplicado.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.tipo-carga-form-view {
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

.preview-desc {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.45;
  color: var(--sga-text-muted);
}

.preview-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 0.65rem;
  border-top: 1px solid var(--sga-border-light);
  font-size: 0.82rem;
  color: var(--sga-text-muted);
}
</style>
