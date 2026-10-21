<template>
  <div class="tipo-carga-registration-form-view">
    <!-- 1. Header Bar de Navegación y Acciones -->
    <div class="form-header-bar">
      <div class="header-titles">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('cancel')"
          title="Regresar al catálogo de tipos de carga"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver al Catálogo de Tipos de Carga</span>
        </button>

        <div class="kicker-row">
          <span class="badge-kicker">TABLAS MAESTRAS ADUANERAS</span>
          <span :class="['mode-pill', isEditMode ? 'mode-edit' : 'mode-create']">
            <i :class="isEditMode ? 'pi pi-pencil' : 'pi pi-plus'"></i>
            <span>{{ isEditMode ? 'MODO EDICIÓN' : 'MODO REGISTRO' }}</span>
          </span>
        </div>

        <h1 class="form-main-title">
          {{ isEditMode ? `Editar Clasificación: ${form.codigo}` : 'Registrar Nuevo Tipo de Carga' }}
        </h1>
        <p class="form-main-desc">
          Configure los parámetros técnicos de cubicaje, contenedor sugerido y tara de peso para las declaraciones de aduanas y manifiestos de carga.
        </p>
      </div>

      <div class="header-actions">
        <button
          type="button"
          class="btn-outline-warm"
          @click="$emit('cancel')"
          :disabled="submitting"
        >
          <span>Cancelar</span>
        </button>
        <button
          type="button"
          class="btn-espresso"
          @click="handleSubmit"
          :disabled="submitting || !isFormReady"
        >
          <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-check"></i>
          <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Tipo de Carga' }}</span>
        </button>
      </div>
    </div>

    <!-- Banner de Error si la validación falla -->
    <div v-if="errorMessage" class="error-banner">
      <i class="pi pi-exclamation-triangle"></i>
      <div class="error-content">
        <strong>Error:</strong>
        <span>{{ errorMessage }}</span>
      </div>
      <button type="button" class="error-close-btn" @click="errorMessage = ''">
        <i class="pi pi-times"></i>
      </button>
    </div>

    <!-- 2. Grid Principal -->
    <div class="form-content-grid">
      <!-- Columna Principal: Bloques de Formulario -->
      <div class="form-blocks-column">
        <!-- BLOQUE 01: IDENTIFICACIÓN Y NOMENCLATURA -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Identificación y Código Técnico de Carga</h3>
              <p class="block-sub">Nomenclatura oficial utilizada en las declaraciones únicas de aduanas (DUA) y manifiestos.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Código Identificador -->
            <div class="field-group highlight-group">
              <div class="label-with-badge">
                <label for="tc-codigo" class="field-label">Código Identificador Único *</label>
                <span class="kicker-mini-badge">ISO 6346 / SUNAT</span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-tag input-icon"></i>
                <input
                  id="tc-codigo"
                  v-model.trim="form.codigo"
                  type="text"
                  placeholder="Ej. CONT-20-DRY, REEFER-40, GRANEL-SOLIDO"
                  class="form-input code-input"
                  required
                  maxlength="50"
                  :disabled="isEditMode || submitting"
                  @input="handleCodeInput"
                />
              </div>
              <p class="field-helper-text">
                Código en mayúsculas sin espacios ni caracteres especiales. Use guiones (-) o guion bajo (_).
              </p>

              <!-- Atajos de Prefijo para Código Rápido -->
              <div v-if="!isEditMode" class="quick-shortcuts-row">
                <span class="shortcuts-label">Atajos de formato:</span>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="applyCodePrefix('CONT-20-')"
                >
                  CONT-20-
                </button>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="applyCodePrefix('CONT-40-')"
                >
                  CONT-40-
                </button>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="applyCodePrefix('REEFER-40-')"
                >
                  REEFER-40-
                </button>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="applyCodePrefix('GRANEL-')"
                >
                  GRANEL-
                </button>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="applyCodePrefix('IMO-PELIGROSO-')"
                >
                  IMO-PELIGROSO-
                </button>
              </div>
            </div>

            <!-- Nombre / Descripción de la Carga -->
            <div class="field-group">
              <label for="tc-nombre" class="field-label">Nombre Descriptivo de la Mercancía o Tipo de Carga *</label>
              <div class="input-icon-box">
                <i class="pi pi-box input-icon"></i>
                <input
                  id="tc-nombre"
                  v-model.trim="form.nombre"
                  type="text"
                  placeholder="Ej. Contenedor Dry Estándar 20 Pies - Carga General Seca"
                  class="form-input"
                  required
                  maxlength="150"
                  :disabled="submitting"
                />
              </div>
              <p class="field-helper-text">
                Denominación clara del tipo de carga para los oficiales de aforo y vistas aduaneros.
              </p>
            </div>
          </div>
        </section>

        <!-- BLOQUE 02: ESPECIFICACIONES TÉCNICAS Y EMBALAJE -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Especificaciones Técnicas, Contenedor y Tara</h3>
              <p class="block-sub">Determine el tipo de embalaje sugerido y el peso de tara de referencia en kilogramos.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Selector de Contenedor Sugerido (Interactive Cards) -->
            <div class="field-group">
              <label class="field-label">Contenedor / Embalaje Sugerido</label>
              <div class="container-options-grid">
                <div
                  v-for="opt in containerOptions"
                  :key="opt.value"
                  :class="['container-option-card', { selected: form.tipoContenedorSugerido === opt.value }]"
                  @click="selectContainerOption(opt)"
                >
                  <div class="option-icon-box">
                    <i :class="opt.icon"></i>
                  </div>
                  <div class="option-info">
                    <span class="option-title">{{ opt.label }}</span>
                    <span class="option-desc">{{ opt.desc }}</span>
                  </div>
                  <div class="option-radio-indicator">
                    <i v-if="form.tipoContenedorSugerido === opt.value" class="pi pi-check-circle"></i>
                    <span v-else class="radio-circle"></span>
                  </div>
                </div>
              </div>

              <!-- Input manual personalizado -->
              <div class="custom-container-input-box">
                <label for="tc-contenedor-custom" class="sub-label">O especifique un embalaje personalizado:</label>
                <div class="input-icon-box">
                  <i class="pi pi-pencil input-icon"></i>
                  <input
                    id="tc-contenedor-custom"
                    v-model.trim="form.tipoContenedorSugerido"
                    type="text"
                    placeholder="Ej. Big Bag 1000kg, Iso-Tanque 24000L, Pallet Madera Estándar"
                    class="form-input"
                    maxlength="50"
                    :disabled="submitting"
                  />
                </div>
              </div>
            </div>

            <!-- Tara Sugerida en Kilogramos -->
            <div class="field-group highlight-group">
              <div class="label-with-badge">
                <label for="tc-tara" class="field-label">Tara Sugerida de Referencia (kg) *</label>
                <span class="kicker-mini-badge">PESO VACÍO</span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-compass input-icon"></i>
                <input
                  id="tc-tara"
                  v-model.number="form.taraSugeridaKg"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="2200.00"
                  class="form-input number-input"
                  required
                  :disabled="submitting"
                />
                <span class="input-suffix">KG</span>
              </div>
              <p class="field-helper-text">
                Peso en kilogramos del contenedor o estructura vacía para descontar del peso bruto aduanero.
              </p>

              <!-- Presets rápidos de Tara -->
              <div class="quick-shortcuts-row">
                <span class="shortcuts-label">Presets de tara estándar:</span>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="form.taraSugeridaKg = 2200"
                >
                  2,200 kg (20' Dry)
                </button>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="form.taraSugeridaKg = 3800"
                >
                  3,800 kg (40' HC)
                </button>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="form.taraSugeridaKg = 4500"
                >
                  4,500 kg (40' Reefer)
                </button>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="form.taraSugeridaKg = 3100"
                >
                  3,100 kg (Flat Rack)
                </button>
                <button
                  type="button"
                  class="shortcut-chip"
                  @click="form.taraSugeridaKg = 0"
                >
                  0 kg (Granel/Suelta)
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 03: ESTADO OPERATIVO -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Estado Operativo en el Sistema</h3>
              <p class="block-sub">Determine si este tipo de carga está disponible inmediatamente para operaciones y aduanas.</p>
            </div>
          </div>

          <div class="block-body">
            <div class="status-selection-grid">
              <div
                :class="['status-option-card', { selected: form.estado === 'ACTIVO' }]"
                @click="form.estado = 'ACTIVO'"
              >
                <div class="status-card-header">
                  <span class="status-indicator-dot dot-active"></span>
                  <span class="status-card-title">ACTIVO</span>
                </div>
                <p class="status-card-desc">
                  Habilitado para selección en nuevos manifiestos, control de aforo y declaraciones aduaneras.
                </p>
              </div>

              <div
                :class="['status-option-card', { selected: form.estado === 'INACTIVO' }]"
                @click="form.estado = 'INACTIVO'"
              >
                <div class="status-card-header">
                  <span class="status-indicator-dot dot-inactive"></span>
                  <span class="status-card-title">INACTIVO</span>
                </div>
                <p class="status-card-desc">
                  Suspendido temporalmente. No podrá seleccionarse en nuevas operaciones aduaneras.
                </p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- Columna Lateral Sticky: Previsualización en Vivo y Validación -->
      <aside class="form-sidebar-column">
        <!-- Tarjeta de Previsualización Técnica de Carga -->
        <div class="preview-credential-card">
          <div class="card-ribbon">
            <i class="pi pi-shield"></i>
            <span>FICHA TÉCNICA CLASIFICADORA</span>
          </div>

          <div class="card-avatar-box">
            <div class="avatar-circle">
              <i class="pi pi-box"></i>
            </div>
          </div>

          <div class="card-identity">
            <h4 class="card-port-name">
              {{ form.nombre || 'Nombre de la Carga' }}
            </h4>
            <div class="card-code-pill">
              <span>{{ form.codigo || 'CÓDIGO' }}</span>
            </div>
          </div>

          <div class="card-details-list">
            <div class="card-detail-item">
              <span class="detail-label">Embalaje Sugerido:</span>
              <span class="detail-val">{{ form.tipoContenedorSugerido || 'No especificado' }}</span>
            </div>

            <div class="card-detail-item">
              <span class="detail-label">Tara de Referencia:</span>
              <span class="detail-val highlight-val">{{ formatNumber(form.taraSugeridaKg) }} kg</span>
            </div>

            <div class="card-detail-item">
              <span class="detail-label">Estado Operativo:</span>
              <span :class="['status-pill-badge', form.estado === 'ACTIVO' ? 'badge-active' : 'badge-inactive']">
                {{ form.estado }}
              </span>
            </div>

            <div class="card-detail-item">
              <span class="detail-label">Régimen Aduanero:</span>
              <span class="detail-val">General / Declarable</span>
            </div>
          </div>

          <div class="card-footer-stamp">
            <i class="pi pi-check-circle"></i>
            <span>SISTEMA DE GESTIÓN ADUANERA • TABLA MAESTRA</span>
          </div>
        </div>

        <!-- Tarjeta de Calidad y Validación -->
        <div class="checklist-card">
          <h4 class="checklist-title">
            <i class="pi pi-list-check"></i>
            <span>Control de Integridad de Datos</span>
          </h4>

          <ul class="checklist-items">
            <li :class="['checklist-row', { valid: !!form.codigo && form.codigo.length >= 3 }]">
              <i :class="form.codigo && form.codigo.length >= 3 ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Código técnico válido (mín. 3 car.)</span>
            </li>
            <li :class="['checklist-row', { valid: !!form.nombre && form.nombre.length >= 4 }]">
              <i :class="form.nombre && form.nombre.length >= 4 ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Descripción formal de la mercancía</span>
            </li>
            <li :class="['checklist-row', { valid: form.taraSugeridaKg !== null && form.taraSugeridaKg >= 0 }]">
              <i :class="form.taraSugeridaKg !== null && form.taraSugeridaKg >= 0 ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Tara calibrada en kg (≥ 0 kg)</span>
            </li>
            <li :class="['checklist-row', { valid: !!form.estado }]">
              <i :class="form.estado ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Estado operativo asignado</span>
            </li>
          </ul>

          <div class="sidebar-action-buttons">
            <button
              type="button"
              class="btn-espresso full-width"
              @click="handleSubmit"
              :disabled="submitting || !isFormReady"
            >
              <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
              <i v-else class="pi pi-save"></i>
              <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Tipo de Carga' }}</span>
            </button>

            <button
              type="button"
              class="btn-outline-warm full-width"
              @click="$emit('cancel')"
              :disabled="submitting"
            >
              <span>Descartar y Regresar</span>
            </button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import catalogoService from '../services/catalogoService'

const props = defineProps({
  tipoToEdit: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['cancel', 'saved'])

const submitting = ref(false)
const errorMessage = ref('')

const defaultForm = () => ({
  codigo: '',
  nombre: '',
  tipoContenedorSugerido: "20' Dry Standard",
  taraSugeridaKg: 2200,
  estado: 'ACTIVO'
})

const form = ref(defaultForm())

const isEditMode = computed(() => !!props.tipoToEdit && !!props.tipoToEdit.idTipoCarga)

const containerOptions = [
  {
    value: "20' Dry Standard",
    label: "20' Dry Standard",
    desc: 'Carga general seca en contenedor cerrado',
    tara: 2200,
    icon: 'pi pi-box'
  },
  {
    value: "40' High Cube",
    label: "40' High Cube",
    desc: 'Gran volumen y altura extendida (9.6 ft)',
    tara: 3800,
    icon: 'pi pi-server'
  },
  {
    value: "40' Reefer Refrigerado",
    label: "40' Reefer Refrigerado",
    desc: 'Mercancías perecibles con atmósfera controlada',
    tara: 4500,
    icon: 'pi pi-snowflake'
  },
  {
    value: 'Open Top / Flat Rack',
    label: 'Open Top / Flat Rack',
    desc: 'Maquinaria pesada o sobredimensionada',
    tara: 3100,
    icon: 'pi pi-truck'
  },
  {
    value: 'Tanque Cisterna / ISO Tank',
    label: 'Tanque Cisterna / ISO Tank',
    desc: 'Líquidos a granel y productos químicos',
    tara: 3600,
    icon: 'pi pi-compass'
  },
  {
    value: 'Carga Suelta / Palletizada',
    label: 'Carga Suelta / Palletizada',
    desc: 'Mercancía fraccionada o no containerizada',
    tara: 0,
    icon: 'pi pi-inbox'
  }
]

watch(
  () => props.tipoToEdit,
  (newVal) => {
    errorMessage.value = ''
    if (newVal && newVal.idTipoCarga) {
      form.value = {
        codigo: newVal.codigo || '',
        nombre: newVal.nombre || '',
        tipoContenedorSugerido: newVal.tipoContenedorSugerido || '',
        taraSugeridaKg: newVal.taraSugeridaKg !== undefined && newVal.taraSugeridaKg !== null ? newVal.taraSugeridaKg : 0,
        estado: newVal.estado || 'ACTIVO'
      }
    } else {
      form.value = defaultForm()
    }
  },
  { immediate: true }
)

const isFormReady = computed(() => {
  return (
    form.value.codigo &&
    form.value.codigo.trim().length >= 3 &&
    form.value.nombre &&
    form.value.nombre.trim().length >= 3 &&
    form.value.estado
  )
})

const handleCodeInput = (e) => {
  form.value.codigo = form.value.codigo.toUpperCase().replace(/\s+/g, '_')
}

const applyCodePrefix = (prefix) => {
  if (!form.value.codigo) {
    form.value.codigo = prefix
  } else if (!form.value.codigo.startsWith(prefix)) {
    form.value.codigo = prefix + form.value.codigo.replace(/^[A-Z0-9]+-/, '')
  }
}

const selectContainerOption = (opt) => {
  form.value.tipoContenedorSugerido = opt.value
  if (opt.tara !== undefined) {
    form.value.taraSugeridaKg = opt.tara
  }
}

const formatNumber = (num) => {
  if (num === null || num === undefined || isNaN(num)) return '0.00'
  return Number(num).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!isFormReady.value) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*)'
    return
  }

  submitting.value = true
  try {
    const payload = {
      codigo: form.value.codigo,
      nombre: form.value.nombre,
      estado: form.value.estado
    }

    let result
    if (isEditMode.value) {
      result = await catalogoService.updateTipoCarga(props.tipoToEdit.idTipoCarga, payload)
    } else {
      result = await catalogoService.createTipoCarga(payload)
    }

    emit('saved', {
      isEdit: isEditMode.value,
      data: result.data || result
    })
  } catch (error) {
    console.error('Error al guardar tipo de carga:', error)
    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.data ||
      'Error al procesar la solicitud. Verifique que el código no esté duplicado.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.tipo-carga-registration-form-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  max-width: 1400px;
  margin: 0 auto;
}

/* 1. Header Bar */
.form-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-6, 1.5rem);
  padding: var(--space-6, 1.5rem) var(--space-8, 2rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  box-shadow: var(--shadow-sm);
  flex-wrap: wrap;
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  max-width: 800px;
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  padding: 0;
  margin-bottom: var(--space-1, 0.25rem);
  transition: color var(--transition-fast);
}

.btn-back-link:hover {
  color: var(--color-primary);
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.badge-kicker {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-primary);
  background: var(--color-primary-subtle);
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-sm, 0.25rem);
}

.mode-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 0.2rem 0.6rem;
  border-radius: var(--radius-full, 9999px);
}

.mode-create {
  background: var(--color-success-subtle);
  color: var(--color-success);
}

.mode-edit {
  background: var(--color-warning-subtle);
  color: var(--color-warning);
}

.form-main-title {
  font-family: var(--font-sans);
  font-size: var(--font-size-2xl, 1.75rem);
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1.2;
  margin: 0;
}

.form-main-desc {
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.65rem 1.25rem;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-subtle);
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.65rem 1.4rem;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  border-radius: var(--radius-md, 0.5rem);
  border: none;
  background: var(--color-primary);
  color: var(--color-primary-contrast);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-sm);
}

.btn-espresso:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
}

.btn-espresso:disabled,
.btn-outline-warm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* Banner de Error */
.error-banner {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background: var(--color-danger-subtle);
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-lg, 0.75rem);
  color: var(--color-danger);
  font-size: var(--font-size-sm, 0.875rem);
}

.error-content {
  flex: 1;
}

.error-close-btn {
  background: none;
  border: none;
  color: var(--color-danger);
  cursor: pointer;
  padding: 0.25rem;
}

/* 2. Grid de Contenido */
.form-content-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: var(--space-6, 1.5rem);
  align-items: start;
}

@media (max-width: 1080px) {
  .form-content-grid {
    grid-template-columns: 1fr;
  }
}

/* Bloques de Formulario */
.form-blocks-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.form-block-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem) var(--space-8, 2rem);
  box-shadow: var(--shadow-sm);
}

.block-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4, 1rem);
  margin-bottom: var(--space-6, 1.5rem);
  padding-bottom: var(--space-4, 1rem);
  border-bottom: 1px solid var(--color-border-subtle);
}

.block-idx {
  font-family: var(--font-mono);
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-subtle);
  width: 2.25rem;
  height: 2.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md, 0.5rem);
  flex-shrink: 0;
}

.block-header-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.block-title {
  font-size: var(--font-size-md, 1.05rem);
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
}

.block-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
}

.block-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
}

/* Grupos de Campos */
.field-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.label-with-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.field-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-main);
}

.sub-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted);
  margin-bottom: 0.25rem;
  display: block;
}

.kicker-mini-badge {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-sm, 0.25rem);
}

.input-icon-box {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 0.9rem;
  color: var(--color-text-muted);
  font-size: 0.95rem;
  pointer-events: none;
}

.input-suffix {
  position: absolute;
  right: 0.9rem;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  color: var(--color-text-muted);
  pointer-events: none;
}

.form-input {
  width: 100%;
  padding: 0.7rem 0.9rem 0.7rem 2.5rem;
  font-size: var(--font-size-sm, 0.875rem);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  transition: all var(--transition-fast);
}

.number-input {
  padding-right: 2.75rem;
  font-weight: 700;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  background: var(--color-bg-surface);
  box-shadow: 0 0 0 2px var(--color-primary-subtle);
}

.code-input {
  font-family: monospace;
  font-weight: 800;
  letter-spacing: 0.06em;
  font-size: var(--font-size-md, 1rem);
}

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.field-helper-text {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.4;
}

/* Quick shortcuts */
.quick-shortcuts-row {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  flex-wrap: wrap;
  margin-top: 0.25rem;
}

.shortcuts-label {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.shortcut-chip {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full, 9999px);
  padding: 0.2rem 0.55rem;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.shortcut-chip:hover {
  background: var(--color-primary-subtle);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* Contenedor Interactive Options Grid */
.container-options-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--space-3, 0.75rem);
  margin-bottom: var(--space-3, 0.75rem);
}

.container-option-card {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-3, 0.75rem) var(--space-4, 1rem);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  background: var(--color-bg-subtle);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.container-option-card:hover {
  border-color: var(--color-border-subtle);
  background: var(--color-bg-surface);
}

.container-option-card.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-subtle);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.option-icon-box {
  width: 2rem;
  height: 2rem;
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  flex-shrink: 0;
  font-size: 0.95rem;
}

.container-option-card.selected .option-icon-box {
  background: var(--color-primary);
  color: var(--color-primary-contrast);
  border-color: var(--color-primary);
}

.option-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  flex: 1;
}

.option-title {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.option-desc {
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
  line-height: 1.3;
}

.option-radio-indicator {
  color: var(--color-primary);
  font-size: 1rem;
}

.radio-circle {
  display: inline-block;
  width: 0.9rem;
  height: 0.9rem;
  border-radius: 50%;
  border: 1.5px solid var(--color-border);
}

.custom-container-input-box {
  margin-top: var(--space-2, 0.5rem);
}

/* Estado Operativo Grid */
.status-selection-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4, 1rem);
}

@media (max-width: 640px) {
  .status-selection-grid {
    grid-template-columns: 1fr;
  }
}

.status-option-card {
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  background: var(--color-bg-subtle);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.status-option-card:hover {
  border-color: var(--color-border-subtle);
  background: var(--color-bg-surface);
}

.status-option-card.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-subtle);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.status-card-header {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  margin-bottom: var(--space-2, 0.5rem);
}

.status-indicator-dot {
  width: 0.65rem;
  height: 0.65rem;
  border-radius: 50%;
}

.dot-active {
  background: var(--color-success);
}

.dot-inactive {
  background: var(--color-danger);
}

.status-card-title {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--color-text-main);
}

.status-card-desc {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  line-height: 1.4;
  margin: 0;
}

/* 3. Columna Lateral Sticky */
.form-sidebar-column {
  position: sticky;
  top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
}

/* Tarjeta Ficha Técnica */
.preview-credential-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.card-ribbon {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--color-primary);
  background: var(--color-primary-subtle);
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-full, 9999px);
  margin-bottom: var(--space-4, 1rem);
}

.card-avatar-box {
  margin-bottom: var(--space-3, 0.75rem);
}

.avatar-circle {
  width: 4rem;
  height: 4rem;
  border-radius: var(--radius-lg, 0.75rem);
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  border: 2px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
}

.card-identity {
  margin-bottom: var(--space-4, 1rem);
}

.card-port-name {
  font-family: var(--font-sans);
  font-size: var(--font-size-md, 1.1rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 var(--space-2, 0.5rem) 0;
  line-height: 1.3;
}

.card-code-pill {
  display: inline-block;
  font-family: monospace;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  letter-spacing: 0.06em;
  padding: 0.25rem 0.75rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full, 9999px);
  color: var(--color-text-main);
}

.card-details-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-3, 0.75rem);
  margin-bottom: var(--space-5, 1.25rem);
  padding: var(--space-4, 1rem);
  background: var(--color-bg-subtle);
  border-radius: var(--radius-lg, 0.75rem);
  border: 1px solid var(--color-border-subtle);
}

.card-detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-xs, 0.75rem);
  gap: var(--space-2, 0.5rem);
}

.detail-label {
  color: var(--color-text-muted);
  font-weight: 600;
}

.detail-val {
  color: var(--color-text-main);
  font-weight: 700;
  text-align: right;
}

.highlight-val {
  color: var(--color-primary);
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 800;
}

.status-pill-badge {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  padding: 0.15rem 0.55rem;
  border-radius: var(--radius-full, 9999px);
}

.badge-active {
  background: var(--color-success-subtle);
  color: var(--color-success);
}

.badge-inactive {
  background: var(--color-danger-subtle);
  color: var(--color-danger);
}

.card-footer-stamp {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
  font-weight: 700;
}

/* Tarjeta Checklist */
.checklist-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-5, 1.25rem);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
}

.checklist-title {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-main);
  margin: 0;
}

.checklist-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.checklist-row {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  transition: color var(--transition-fast);
}

.checklist-row.valid {
  color: var(--color-success);
  font-weight: 600;
}

.sidebar-action-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  margin-top: var(--space-2, 0.5rem);
}

.full-width {
  width: 100%;
}
</style>
