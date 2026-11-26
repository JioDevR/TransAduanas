<template>
  <BaseModal
    :visible="visible"
    :title="isEditMode ? 'Editar Mercancía Arancelaria' : 'Registrar Nueva Mercancía'"
    :subtitle="isEditMode ? `Actualizando partida ${form.codigo}` : 'Catálogo aduanero de partidas, permisos VUCE y arancel base'"
    :icon="isEditMode ? 'pi pi-pencil' : 'pi pi-plus'"
    max-width="640px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form class="sga-dialog-body" @submit.prevent="handleSubmit">
      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="merc-codigo">Partida / Código Único *</label>
          <InputText
            id="merc-codigo"
            v-model.trim="form.codigo"
            placeholder="Ej. 3004.90.29.00"
            class="sga-mono"
            :disabled="isEditMode || loading"
            required
            maxlength="30"
            fluid
          />
          <small class="field-hint">
            {{ isEditMode ? 'Código arancelario inmutable en edición.' : 'Código de subpartida nacional o producto' }}
          </small>
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="merc-categoria">Categoría Arancelaria *</label>
          <Select
            inputId="merc-categoria"
            v-model="form.categoria"
            :options="categoryOptions"
            filter
            placeholder="Seleccione categoría"
            :disabled="loading"
            fluid
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="merc-nombre">Descripción Comercial de la Mercancía *</label>
        <InputText
          id="merc-nombre"
          v-model.trim="form.nombre"
          placeholder="Ej. Paracetamol 500mg en tabletas recubiertas"
          :disabled="loading"
          required
          maxlength="150"
          fluid
        />
      </div>

      <div class="sga-grid-2">
        <div class="sga-form-field">
          <label class="sga-form-label" for="merc-permiso">¿Requiere Permiso VUCE? *</label>
          <Select
            inputId="merc-permiso"
            v-model="form.requierePermiso"
            :options="permisoOptions"
            optionLabel="label"
            optionValue="value"
            :disabled="loading"
            fluid
          />
        </div>

        <div v-if="form.requierePermiso" class="sga-form-field">
          <label class="sga-form-label" for="merc-entidad">Entidad Reguladora VUCE *</label>
          <Select
            inputId="merc-entidad"
            v-model="form.entidadReguladora"
            :options="entidadOptions"
            optionLabel="label"
            optionValue="value"
            :disabled="loading"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="merc-arancel">Arancel Base Ad-Valorem (%) *</label>
          <InputNumber
            inputId="merc-arancel"
            v-model="form.arancelBasePct"
            :min="0"
            :max="100"
            :minFractionDigits="2"
            :maxFractionDigits="2"
            suffix="%"
            placeholder="0.00%"
            :disabled="loading"
            fluid
          />
        </div>

        <div class="sga-form-field">
          <label class="sga-form-label" for="merc-estado">Estado *</label>
          <Select
            inputId="merc-estado"
            v-model="form.estado"
            :options="estadoOptions"
            optionLabel="label"
            optionValue="value"
            :disabled="loading"
            fluid
          />
        </div>
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
          :label="isEditMode ? 'Guardar Cambios' : 'Registrar Mercancía'"
          icon="pi pi-check"
          :loading="loading"
        />
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import catalogoService from '../services/catalogoService'
import BaseModal from '@/shared/components/BaseModal.vue'
import {
  DEFAULT_MERCANCIA_CATEGORIES,
  mergeNormalizedCategories
} from '@/features/gerencia/utils/gerenciaFormatters'

const permisoOptions = [
  { label: 'No (Libre Comercialización)', value: false },
  { label: 'Sí (Carga Restringida VUCE)', value: true }
]

const entidadOptions = [
  { label: 'DIGEMID (Medicamentos y Cosméticos)', value: 'DIGEMID' },
  { label: 'DIGESA (Alimentos y Bebidas)', value: 'DIGESA' },
  { label: 'SENASA (Agro y Animal)', value: 'SENASA' },
  { label: 'SANIPES (Pesca y Acuicultura)', value: 'SANIPES' },
  { label: 'SUCAMEC (Explosivos y Armas)', value: 'SUCAMEC' },
  { label: 'PRODUCE (Insumos Químicos)', value: 'PRODUCE' },
  { label: 'MTC (Equipos de Telecomunicación)', value: 'MTC' }
]

const estadoOptions = [
  { label: 'ACTIVO (Habilitado)', value: 'ACTIVO' },
  { label: 'INACTIVO (Desactivado)', value: 'INACTIVO' }
]

const props = defineProps({
  visible: { type: Boolean, default: false },
  mercanciaToEdit: { type: Object, default: null },
  existingCategories: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')

const defaultForm = () => ({
  codigo: '',
  nombre: '',
  categoria: 'TECNOLOGIA',
  requierePermiso: false,
  entidadReguladora: 'DIGEMID',
  arancelBasePct: 0.0,
  estado: 'ACTIVO'
})

const form = ref(defaultForm())
const isEditMode = computed(() => Boolean(props.mercanciaToEdit?.idMercancia))

const categoryOptions = computed(() =>
  mergeNormalizedCategories(
    DEFAULT_MERCANCIA_CATEGORIES,
    props.existingCategories,
    form.value.categoria ? [form.value.categoria] : []
  )
)

watch(
  () => props.visible,
  (newVal) => {
    if (!newVal) return
    errorMessage.value = ''
    if (props.mercanciaToEdit?.idMercancia) {
      form.value = {
        codigo: props.mercanciaToEdit.codigo || '',
        nombre: props.mercanciaToEdit.nombre || '',
        categoria: String(props.mercanciaToEdit.categoria || 'GENERAL').trim().toUpperCase(),
        requierePermiso: Boolean(props.mercanciaToEdit.requierePermiso),
        entidadReguladora: props.mercanciaToEdit.entidadReguladora || 'DIGEMID',
        arancelBasePct: Number(props.mercanciaToEdit.arancelBasePct ?? 0),
        estado: props.mercanciaToEdit.estado || 'ACTIVO'
      }
    } else {
      form.value = defaultForm()
    }
  },
  { immediate: true }
)

const handleClose = () => emit('close')

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!form.value.codigo || !form.value.nombre || !form.value.categoria) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*).'
    return
  }

  loading.value = true
  try {
    const payload = {
      codigo: form.value.codigo.trim().toUpperCase(),
      nombre: form.value.nombre.trim(),
      categoria: String(form.value.categoria).trim().toUpperCase(),
      requierePermiso: Boolean(form.value.requierePermiso),
      entidadReguladora: form.value.requierePermiso ? form.value.entidadReguladora : null,
      arancelBasePct: Number(form.value.arancelBasePct || 0),
      estado: form.value.estado
    }

    const result = isEditMode.value
      ? await catalogoService.updateMercancia(props.mercanciaToEdit.idMercancia, payload)
      : await catalogoService.createMercancia(payload)

    emit('saved', { isEdit: isEditMode.value, data: result?.data || result })
    handleClose()
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.message ||
      'Error al procesar la solicitud. Verifique que el código no esté duplicado.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.field-hint {
  color: var(--sga-text-muted);
  font-size: 0.78rem;
}
</style>
