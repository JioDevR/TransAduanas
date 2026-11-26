<template>
  <div class="sede-form-view fade-in">
    <CatalogoFormHeader
      :is-edit-mode="isEditMode"
      kicker="INFRAESTRUCTURA TERRITORIAL"
      :subtitle="isEditMode ? `Editando sede ${form.codigoSede}` : 'Registrar nueva sede operativa o agencia'"
      :submit-label="isEditMode ? 'Guardar Cambios' : 'Registrar Sede'"
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
          <span class="sga-kicker">01 · IDENTIFICACIÓN &amp; CLASIFICACIÓN</span>
          <div class="sga-grid-2">
            <div class="sga-form-field">
              <label class="sga-form-label" for="sede-codigo">Código de Sede *</label>
              <InputText
                id="sede-codigo"
                v-model.trim="form.codigoSede"
                placeholder="Ej. LIM-CENTRAL-01"
                :disabled="isEditMode || submitting"
                class="sga-mono"
                maxlength="50"
                fluid
                @input="form.codigoSede = form.codigoSede.toUpperCase()"
              />
              <div v-if="!isEditMode" class="prefijos-row">
                <Button
                  v-for="p in ['LIM-', 'CALL-', 'AQP-', 'TRJ-']"
                  :key="p"
                  type="button"
                  :label="`+${p}`"
                  size="small"
                  severity="secondary"
                  text
                  @click="applyPrefix(p)"
                />
              </div>
              <small v-else class="field-hint">Código inmutable tras el registro inicial.</small>
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="sede-nombre">Nombre Comercial / Referencia *</label>
              <InputText
                id="sede-nombre"
                v-model.trim="form.nombre"
                placeholder="Ej. Sede Central Operativa Callao"
                :disabled="submitting"
                maxlength="100"
                fluid
              />
            </div>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label">Tipo de Instalación *</label>
            <SelectButton
              v-model="form.tipo"
              :options="tipoOptions"
              optionLabel="label"
              optionValue="value"
              :allowEmpty="false"
              :disabled="submitting"
            />
          </div>
        </section>

        <section class="sga-card-surface sga-form-section">
          <span class="sga-kicker">02 · UBICACIÓN GEOGRÁFICA &amp; CONTACTO</span>
          <div class="sga-grid-2">
            <div class="sga-form-field">
              <label class="sga-form-label" for="sede-distrito">Distrito / Jurisdicción *</label>
              <InputText
                id="sede-distrito"
                v-model.trim="form.distrito"
                placeholder="Ej. Callao / San Isidro"
                :disabled="submitting"
                maxlength="100"
                fluid
              />
            </div>
            <div class="sga-form-field">
              <label class="sga-form-label" for="sede-telefono">Teléfono / Central</label>
              <InputText
                id="sede-telefono"
                v-model.trim="form.telefono"
                placeholder="Ej. (01) 420-8800"
                :disabled="submitting"
                maxlength="50"
                fluid
              />
            </div>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="sede-direccion">Dirección Física Exacta *</label>
            <InputText
              id="sede-direccion"
              v-model.trim="form.direccion"
              placeholder="Ej. Av. Elmer Faucett 2840, Callao"
              :disabled="submitting"
              maxlength="255"
              fluid
            />
          </div>
        </section>

        <section class="sga-card-surface sga-form-section">
          <span class="sga-kicker">03 · ESTADO OPERATIVO</span>
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
          <span class="sga-kicker">VISTA PREVIA DE SEDE</span>
          <div class="preview-top">
            <Tag :value="form.codigoSede || 'SIN CÓDIGO'" severity="contrast" class="sga-mono" />
            <Tag :value="form.estado" :severity="getSedeEstadoSeverity(form.estado)" rounded />
          </div>
          <p class="preview-title">{{ form.nombre || 'Nombre de Sede' }}</p>
          <p class="preview-sub">
            <i :class="getSedeTipoIcon(form.tipo)"></i> {{ formatSedeTipo(form.tipo) }} · {{ form.distrito || 'Distrito' }}
          </p>
          <div class="preview-meta">
            <span><strong>Dirección:</strong> {{ form.direccion || 'Pendiente' }}</span>
            <span><strong>Teléfono:</strong> {{ form.telefono || 'Sin registro' }}</span>
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
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'
import sedeService from '../services/sedeService'
import CatalogoFormHeader from '@/features/admin/catalogos/components/CatalogoFormHeader.vue'
import {
  formatSedeTipo,
  getSedeTipoIcon,
  getSedeEstadoSeverity
} from '@/features/gerencia/utils/gerenciaFormatters'

const props = defineProps({
  sedeToEdit: { type: Object, default: null }
})

const emit = defineEmits(['cancel', 'saved'])

const submitting = ref(false)
const errorMessage = ref('')

const tipoOptions = [
  { value: 'SEDE_CENTRAL', label: 'Sede Central' },
  { value: 'AGENCIA', label: 'Agencia Operativa' },
  { value: 'TERMINAL', label: 'Terminal / Almacén' }
]

const estadoOptions = [
  { value: 'OPERATIVO', label: 'Operativo' },
  { value: 'MANTENIMIENTO', label: 'Mantenimiento' },
  { value: 'INACTIVO', label: 'Inactivo' }
]

const form = reactive({
  codigoSede: '',
  nombre: '',
  tipo: 'SEDE_CENTRAL',
  distrito: '',
  direccion: '',
  telefono: '',
  estado: 'OPERATIVO'
})

const isEditMode = computed(() => Boolean(props.sedeToEdit?.idSede))

watch(
  () => props.sedeToEdit,
  (val) => {
    errorMessage.value = ''
    if (val?.idSede) {
      form.codigoSede = val.codigoSede || ''
      form.nombre = val.nombre || ''
      form.tipo = val.tipo || 'SEDE_CENTRAL'
      form.distrito = val.distrito || ''
      form.direccion = val.direccion || ''
      form.telefono = val.telefono || ''
      form.estado = val.estado || 'OPERATIVO'
    } else {
      form.codigoSede = ''
      form.nombre = ''
      form.tipo = 'SEDE_CENTRAL'
      form.distrito = ''
      form.direccion = ''
      form.telefono = ''
      form.estado = 'OPERATIVO'
    }
  },
  { immediate: true }
)

const applyPrefix = (prefix) => {
  if (!form.codigoSede.startsWith(prefix)) {
    form.codigoSede = prefix
  }
}

const isFormReady = computed(
  () => Boolean(form.codigoSede && form.nombre && form.distrito && form.direccion && form.tipo)
)

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!isFormReady.value) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*).'
    return
  }

  submitting.value = true
  try {
    const basePayload = {
      nombre: form.nombre.trim(),
      tipo: form.tipo,
      distrito: form.distrito.trim(),
      direccion: form.direccion.trim(),
      telefono: form.telefono ? form.telefono.trim() : null,
      estado: form.estado
    }
    const result = isEditMode.value
      ? await sedeService.updateSede(props.sedeToEdit.idSede, basePayload)
      : await sedeService.createSede({
          ...basePayload,
          codigoSede: form.codigoSede.trim().toUpperCase()
        })

    emit('saved', { isEdit: isEditMode.value, data: result?.data || result })
  } catch (err) {
    errorMessage.value =
      err?.response?.data?.message ||
      'No se pudo guardar la sede. Verifique que el código no esté duplicado.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.sede-form-view {
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

.preview-meta {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding-top: 0.65rem;
  border-top: 1px solid var(--sga-border-light);
  font-size: 0.82rem;
  color: var(--sga-text-muted);
}
</style>
