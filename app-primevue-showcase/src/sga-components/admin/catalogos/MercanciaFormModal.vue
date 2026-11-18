<template>
  <BaseModal
    :visible="visible"
    :title="isEditMode ? 'Editar Mercancía Arancelaria' : 'Registrar Nueva Mercancía'"
    :subtitle="isEditMode ? `Actualizando clasificación ${form.codigo}` : 'Catálogo aduanero de partidas, requisitos VUCE y arancel base'"
    :icon="isEditMode ? 'pi pi-pencil' : 'pi pi-plus'"
    max-width="620px"
    :error-message="errorMessage"
    :busy="loading"
    @close="handleClose"
  >
    <form @submit.prevent="handleSubmit" class="form-body">
      <div class="form-grid">
        <!-- Código Arancelario / Nomenclatura -->
        <div class="field-item">
          <label for="merc-codigo">Partida / Código Único *</label>
          <InputText
            id="merc-codigo"
            v-model.trim="form.codigo"
            placeholder="Ej. 3004.90.29.00"
            class="code-input"
            required
            maxlength="30"
            fluid
          />
          <span class="field-hint">Código de subpartida nacional o código de producto</span>
        </div>

        <!-- Categoría -->
        <div class="field-item">
          <label for="merc-categoria">Categoría Arancelaria *</label>
          <Select
            inputId="merc-categoria"
            v-model="form.categoria"
            :options="categoryOptions"
            filter
            placeholder="Seleccione categoría"
            fluid
          />
        </div>

        <!-- Nombre de la Mercancía -->
        <div class="field-item full-width">
          <label for="merc-nombre">Descripción Comercial de la Mercancía *</label>
          <InputText
            id="merc-nombre"
            v-model.trim="form.nombre"
            placeholder="Ej. Paracetamol 500mg en tabletas recubiertas"
            required
            maxlength="150"
            fluid
          />
        </div>

        <!-- Requiere Permiso VUCE -->
        <div class="field-item">
          <label for="merc-permiso">¿Requiere Permiso Especial? *</label>
          <Select
            inputId="merc-permiso"
            v-model="form.requierePermiso"
            :options="permisoOptions"
            optionLabel="label"
            optionValue="value"
            fluid
          />
        </div>

        <!-- Entidad Reguladora -->
        <div class="field-item" v-if="form.requierePermiso">
          <label for="merc-entidad">Entidad Reguladora VUCE *</label>
          <Select
            inputId="merc-entidad"
            v-model="form.entidadReguladora"
            :options="entidadOptions"
            optionLabel="label"
            optionValue="value"
            fluid
          />
        </div>

        <!-- Arancel Base % -->
        <div class="field-item">
          <label for="merc-arancel">Arancel Base Ad-Valorem (%) *</label>
          <InputNumber
            inputId="merc-arancel"
            v-model="form.arancelBasePct"
            :min="0"
            :max="100"
            :minFractionDigits="2"
            :maxFractionDigits="2"
            suffix="%"
            placeholder="0.00%"
            fluid
          />
          <span class="field-hint">Tasa arancelaria porcentual (ej. 0.00%, 6.00%, 11.00%)</span>
        </div>

        <!-- Estado Operativo -->
        <div class="field-item">
          <label for="merc-estado">Estado *</label>
          <Select
            inputId="merc-estado"
            v-model="form.estado"
            :options="estadoOptions"
            optionLabel="label"
            optionValue="value"
            fluid
          />
        </div>
      </div>

      <div class="modal-actions">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          @click="handleClose"
          :disabled="loading"
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

const permisoOptions = [
  { label: 'No (Libre Comercialización)', value: false },
  { label: 'Sí (Carga Restringida SUNAT)', value: true }
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
  visible: {
    type: Boolean,
    default: false
  },
  mercanciaToEdit: {
    type: Object,
    default: null
  },
  existingCategories: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')

const defaultCategories = [
  'Agroexportacion',
  'Agropecuario',
  'Alimentos',
  'Autopartes',
  'Bebidas',
  'Calzado',
  'Confecciones',
  'Cosmetica',
  'Electrohogar',
  'Entretenimiento',
  'Equipamiento',
  'Farmacia',
  'Ferreteria',
  'Hogar',
  'Jugueteria',
  'Licores',
  'Madera',
  'Maquinaria',
  'Nutricion',
  'Pesca',
  'Quimicos',
  'Quimicos / IMO',
  'Seguridad',
  'Tabaco',
  'Tecnologia',
  'Transporte',
  'Vehiculos'
]

const categoryOptions = computed(() => {
  const merged = new Set([...props.existingCategories, ...defaultCategories])
  if (form.value?.categoria) merged.add(form.value.categoria)
  return Array.from(merged).filter(Boolean).sort((a, b) => a.localeCompare(b))
})

const defaultForm = () => ({
  codigo: '',
  nombre: '',
  categoria: 'Tecnologia',
  requierePermiso: false,
  entidadReguladora: 'DIGEMID',
  arancelBasePct: 0.0,
  estado: 'ACTIVO'
})

const form = ref(defaultForm())

const isEditMode = computed(() => !!props.mercanciaToEdit && !!props.mercanciaToEdit.idMercancia)

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      errorMessage.value = ''
      if (props.mercanciaToEdit) {
        form.value = {
          codigo: props.mercanciaToEdit.codigo || '',
          nombre: props.mercanciaToEdit.nombre || '',
          categoria: props.mercanciaToEdit.categoria || 'GENERAL',
          requierePermiso: props.mercanciaToEdit.requierePermiso ?? false,
          entidadReguladora: props.mercanciaToEdit.entidadReguladora || 'DIGEMID',
          arancelBasePct: props.mercanciaToEdit.arancelBasePct !== undefined ? Number(props.mercanciaToEdit.arancelBasePct) : 0,
          estado: props.mercanciaToEdit.estado || 'ACTIVO'
        }
      } else {
        form.value = defaultForm()
      }
    }
  }
)

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!form.value.codigo || !form.value.nombre || !form.value.categoria) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*)'
    return
  }

  loading.value = true
  try {
    const payload = {
      codigo: form.value.codigo,
      nombre: form.value.nombre,
      categoria: form.value.categoria,
      requierePermiso: form.value.requierePermiso,
      entidadReguladora: form.value.requierePermiso ? form.value.entidadReguladora : null,
      arancelBasePct: Number(form.value.arancelBasePct || 0),
      estado: form.value.estado
    }

    let result
    if (isEditMode.value) {
      result = await catalogoService.updateMercancia(props.mercanciaToEdit.idMercancia, payload)
    } else {
      result = await catalogoService.createMercancia(payload)
    }

    emit('saved', {
      isEdit: isEditMode.value,
      data: result
    })
    handleClose()
  } catch (error) {
    console.error('Error al guardar mercancía:', error)
    errorMessage.value =
      error.response?.data?.message ||
      'Error al procesar la solicitud. Verifique que el código no esté duplicado.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.code-input {
  font-weight: 700;
  letter-spacing: 0.05em;
}
</style>
