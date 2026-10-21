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
          <input
            id="merc-codigo"
            v-model.trim="form.codigo"
            type="text"
            placeholder="Ej. 3004.90.29.00"
            class="text-input code-input"
            required
            maxlength="30"
          />
          <span class="field-hint">Código de subpartida nacional o código de producto</span>
        </div>

        <!-- Categoría -->
        <div class="field-item">
          <label for="merc-categoria">Categoría Arancelaria *</label>
          <select id="merc-categoria" v-model="form.categoria" class="select-input" required>
            <option value="FARMACOS">Fármacos y Medicamentos</option>
            <option value="ALIMENTOS">Alimentos y Perecibles</option>
            <option value="TECNOLOGIA">Tecnología y Electrónica</option>
            <option value="TEXTILES">Textiles y Calzado</option>
            <option value="MAQUINARIA">Maquinaria y Repuestos</option>
            <option value="QUIMICOS">Sustancias Químicas</option>
            <option value="GENERAL">Carga General Miscelánea</option>
          </select>
        </div>

        <!-- Nombre de la Mercancía -->
        <div class="field-item full-width">
          <label for="merc-nombre">Descripción Comercial de la Mercancía *</label>
          <input
            id="merc-nombre"
            v-model.trim="form.nombre"
            type="text"
            placeholder="Ej. Paracetamol 500mg en tabletas recubiertas"
            class="text-input"
            required
            maxlength="150"
          />
        </div>

        <!-- Requiere Permiso VUCE -->
        <div class="field-item">
          <label for="merc-permiso">¿Requiere Permiso Especial? *</label>
          <select id="merc-permiso" v-model="form.requierePermiso" class="select-input" required>
            <option :value="false">No (Libre Comercialización)</option>
            <option :value="true">Sí (Carga Restringida SUNAT)</option>
          </select>
        </div>

        <!-- Entidad Reguladora -->
        <div class="field-item" v-if="form.requierePermiso">
          <label for="merc-entidad">Entidad Reguladora VUCE *</label>
          <select id="merc-entidad" v-model="form.entidadReguladora" class="select-input" required>
            <option value="DIGEMID">DIGEMID (Medicamentos y Cosméticos)</option>
            <option value="DIGESA">DIGESA (Alimentos y Bebidas)</option>
            <option value="SENASA">SENASA (Agro y Animal)</option>
            <option value="SANIPES">SANIPES (Pesca y Acuicultura)</option>
            <option value="SUCAMEC">SUCAMEC (Explosivos y Armas)</option>
            <option value="PRODUCE">PRODUCE (Insumos Químicos)</option>
            <option value="MTC">MTC (Equipos de Telecomunicación)</option>
          </select>
        </div>

        <!-- Arancel Base % -->
        <div class="field-item">
          <label for="merc-arancel">Arancel Base Ad-Valorem (%) *</label>
          <input
            id="merc-arancel"
            v-model.number="form.arancelBasePct"
            type="number"
            step="0.01"
            min="0"
            max="100"
            placeholder="0.00"
            class="text-input"
            required
          />
          <span class="field-hint">Tasa arancelaria porcentual (ej. 0.00%, 6.00%, 11.00%)</span>
        </div>

        <!-- Estado Operativo -->
        <div class="field-item">
          <label for="merc-estado">Estado *</label>
          <select id="merc-estado" v-model="form.estado" class="select-input" required>
            <option value="ACTIVO">ACTIVO (Habilitado)</option>
            <option value="INACTIVO">INACTIVO (Desactivado)</option>
          </select>
        </div>
      </div>

      <div class="modal-actions">
        <button type="button" class="btn-modal-cancel" @click="handleClose" :disabled="loading">
          Cancelar
        </button>
        <button type="submit" class="btn-modal-save" :disabled="loading">
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-check"></i>
          <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Mercancía' }}</span>
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import mercanciaService from '../services/mercanciaService'
import BaseModal from '@/shared/components/BaseModal.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  mercanciaToEdit: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const loading = ref(false)
const errorMessage = ref('')

const defaultForm = () => ({
  codigo: '',
  nombre: '',
  categoria: 'GENERAL',
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
      result = await mercanciaService.updateMercancia(props.mercanciaToEdit.idMercancia, payload)
    } else {
      result = await mercanciaService.createMercancia(payload)
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
