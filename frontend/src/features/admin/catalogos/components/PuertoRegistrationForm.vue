<template>
  <div class="puerto-registration-form-view">
    <!-- 1. Header Bar de Navegación y Acciones -->
    <div class="form-header-bar">
      <div class="header-titles">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('cancel')"
          title="Regresar a catálogos auxiliares"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver al Catálogo de Puertos</span>
        </button>

        <div class="kicker-row">
          <span class="badge-kicker">CATÁLOGO MAESTRO ADUANERO</span>
          <span :class="['mode-pill', isEditMode ? 'mode-edit' : 'mode-create']">
            <i :class="isEditMode ? 'pi pi-pencil' : 'pi pi-plus'"></i>
            <span>{{ isEditMode ? 'MODO EDICIÓN' : 'MODO REGISTRO' }}</span>
          </span>
        </div>

        <h1 class="form-main-title">
          {{ isEditMode ? `Editar Puerto: ${form.codigoPuerto}` : 'Registrar Nuevo Puerto o Terminal' }}
        </h1>
        <p class="form-main-desc">
          Defina la codificación UN/LOCODE o SUNAT, modalidad de transporte internacional y ubicación geográfica del punto de control aduanero.
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
          <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Puerto' }}</span>
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
        <!-- BLOQUE 01: NOMENCLATURA Y CÓDIGO -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Identificación y Código Portuario Internacional</h3>
              <p class="block-sub">Estándar oficial UN/LOCODE o código SUNAT de punto de arribo aduanero.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Código del Puerto -->
            <div class="field-group highlight-group">
              <div class="label-with-badge">
                <label for="puerto-codigo" class="field-label">Código Internacional / SUNAT *</label>
                <span class="kicker-mini-badge">UN/LOCODE</span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-tag input-icon"></i>
                <input
                  id="puerto-codigo"
                  v-model.trim="form.codigoPuerto"
                  type="text"
                  placeholder="Ej. PECLL, PEPAI, USLAX, CNSHA"
                  class="form-input code-input"
                  required
                  maxlength="20"
                  :disabled="isEditMode || submitting"
                  @input="form.codigoPuerto = form.codigoPuerto.toUpperCase()"
                />
              </div>

              <!-- Atajos de Códigos Frecuentes -->
              <div v-if="!isEditMode" class="shortcuts-row">
                <span class="shortcuts-hint">Frecuentes:</span>
                <button type="button" class="chip-shortcut" @click="form.codigoPuerto = 'PECLL'">
                  PECLL (Callao)
                </button>
                <button type="button" class="chip-shortcut" @click="form.codigoPuerto = 'PEPAI'">
                  PEPAI (Paita)
                </button>
                <button type="button" class="chip-shortcut" @click="form.codigoPuerto = 'PEMAT'">
                  PEMAT (Matarani)
                </button>
                <button type="button" class="chip-shortcut" @click="form.codigoPuerto = 'USLAX'">
                  USLAX (Los Angeles)
                </button>
                <button type="button" class="chip-shortcut" @click="form.codigoPuerto = 'CNSHA'">
                  CNSHA (Shanghai)
                </button>
              </div>
              <span class="field-hint" v-if="isEditMode">
                El código de puerto es inmutable para asegurar la integridad de cotizaciones y despachos previos.
              </span>
            </div>

            <!-- Nombre Oficial del Puerto -->
            <div class="field-group">
              <label for="puerto-nombre" class="field-label">Nombre Oficial del Puerto o Terminal *</label>
              <div class="input-icon-box">
                <i class="pi pi-compass input-icon"></i>
                <input
                  id="puerto-nombre"
                  v-model.trim="form.nombre"
                  type="text"
                  placeholder="Ej. Puerto del Callao — Terminal Muelle Sur / DP World"
                  class="form-input"
                  required
                  maxlength="150"
                  :disabled="submitting"
                />
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 02: MODALIDAD Y JURISDICCIÓN -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Modalidad de Transporte y Jurisdicción</h3>
              <p class="block-sub">Tipo de vía aduanera (marítimo, aéreo, terrestre o fluvial) y país de ubicación.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Selector Interactivo de Tipo de Puerto -->
            <div class="field-group">
              <label class="field-label">Modalidad de Vía Aduanera *</label>
              <div class="type-cards-grid">
                <div
                  v-for="t in tipoPuertoOptions"
                  :key="t.value"
                  :class="['type-option-card', { active: form.tipo === t.value }]"
                  @click="form.tipo = t.value"
                >
                  <div class="type-card-top">
                    <span class="type-card-badge">
                      <i :class="t.icon"></i>
                      <span>{{ t.label }}</span>
                    </span>
                    <div class="radio-indicator">
                      <i v-if="form.tipo === t.value" class="pi pi-check"></i>
                    </div>
                  </div>
                  <p class="type-card-desc">{{ t.desc }}</p>
                </div>
              </div>
            </div>

            <!-- País -->
            <div class="field-group">
              <label for="puerto-pais" class="field-label">País de Ubicación *</label>
              <div class="input-icon-box">
                <i class="pi pi-globe input-icon"></i>
                <input
                  id="puerto-pais"
                  v-model.trim="form.pais"
                  type="text"
                  placeholder="Ej. Perú, Chile, Estados Unidos, China"
                  class="form-input"
                  required
                  maxlength="50"
                  :disabled="submitting"
                />
              </div>

              <!-- Atajos de Países Frecuentes -->
              <div class="shortcuts-row">
                <span class="shortcuts-hint">Países frecuentes:</span>
                <button type="button" class="chip-shortcut" @click="form.pais = 'Perú'">Perú</button>
                <button type="button" class="chip-shortcut" @click="form.pais = 'Chile'">Chile</button>
                <button type="button" class="chip-shortcut" @click="form.pais = 'Estados Unidos'">Estados Unidos</button>
                <button type="button" class="chip-shortcut" @click="form.pais = 'China'">China</button>
                <button type="button" class="chip-shortcut" @click="form.pais = 'Colombia'">Colombia</button>
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 03: HABILITACIÓN OPERATIVA -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Disponibilidad para Despacho Aduanero</h3>
              <p class="block-sub">Controle si el puerto aparece habilitado para nuevas cotizaciones y tránsitos aduaneros.</p>
            </div>
          </div>

          <div class="block-body">
            <div class="status-pills-row">
              <button
                type="button"
                :class="['status-pill-btn', { active: form.estado === 'ACTIVO' }]"
                @click="form.estado = 'ACTIVO'"
              >
                <span class="pill-dot"></span>
                <span>ACTIVO (Habilitado para Tránsitos y Despachos)</span>
              </button>
              <button
                type="button"
                :class="['status-pill-btn', { active: form.estado === 'INACTIVO' }]"
                @click="form.estado = 'INACTIVO'"
              >
                <span class="pill-dot"></span>
                <span>INACTIVO (Suspendido Temporalmente)</span>
              </button>
            </div>
          </div>
        </section>
      </div>

      <!-- Columna Lateral: Ficha Técnica Portuaria en Vivo -->
      <div class="form-sidebar-column">
        <div class="puerto-card-preview">
          <div class="preview-ribbon">
            <span class="ribbon-text">PUERTO ADUANERO</span>
            <span class="ribbon-id">{{ form.codigoPuerto || 'SIN CÓDIGO' }}</span>
          </div>

          <div class="preview-body">
            <div class="avatar-holder">
              <div class="puerto-avatar">
                <i :class="selectedTipoIcon"></i>
              </div>
              <div :class="['status-indicator-dot', form.estado === 'ACTIVO' ? 'ok' : 'warn']">
                <i class="pi pi-check" v-if="form.estado === 'ACTIVO'"></i>
                <i class="pi pi-ban" v-else></i>
              </div>
            </div>

            <h4 class="preview-name">{{ form.nombre || 'Nombre del Puerto' }}</h4>
            <span class="preview-tipo">{{ selectedTipoLabel }}</span>
            <span class="preview-pais">{{ form.pais ? `Jurisdicción: ${form.pais}` : 'País sin definir' }}</span>

            <div class="preview-divider"></div>

            <div class="preview-data-rows">
              <div class="data-row">
                <span class="data-k">Código:</span>
                <span class="data-v">{{ form.codigoPuerto || '—' }}</span>
              </div>
              <div class="data-row">
                <span class="data-k">Modalidad:</span>
                <span class="data-v">{{ selectedTipoLabel }}</span>
              </div>
              <div class="data-row">
                <span class="data-k">País:</span>
                <span class="data-v">{{ form.pais || '—' }}</span>
              </div>
              <div class="data-row">
                <span class="data-k">Estado:</span>
                <span class="data-v">{{ form.estado }}</span>
              </div>
            </div>
          </div>

          <div class="preview-footer">
            <span class="footer-chip">CATÁLOGO OFICIAL SUNAT</span>
            <span :class="['footer-status-pill', { ok: form.estado === 'ACTIVO' }]">
              <span class="status-circle"></span>
              <span>{{ form.estado }}</span>
            </span>
          </div>
        </div>

        <!-- Checklist de Calidad -->
        <div class="quality-checklist-card">
          <h5 class="checklist-title">
            <i class="pi pi-check-circle text-success"></i>
            <span>Verificación Portuaria</span>
          </h5>
          <ul class="checklist-items">
            <li :class="{ complete: !!form.codigoPuerto }">
              <i :class="form.codigoPuerto ? 'pi pi-check text-success' : 'pi pi-circle text-muted'"></i>
              <span>Código internacional o SUNAT asignado</span>
            </li>
            <li :class="{ complete: !!form.nombre }">
              <i :class="form.nombre ? 'pi pi-check text-success' : 'pi pi-circle text-muted'"></i>
              <span>Nombre oficial de la instalación</span>
            </li>
            <li :class="{ complete: !!form.pais }">
              <i :class="form.pais ? 'pi pi-check text-success' : 'pi pi-circle text-muted'"></i>
              <span>País de origen / destino registrado</span>
            </li>
            <li :class="{ complete: !!form.tipo }">
              <i :class="form.tipo ? 'pi pi-check text-success' : 'pi pi-circle text-muted'"></i>
              <span>Vía aduanera configurada</span>
            </li>
          </ul>
        </div>

        <!-- Botones de Acción en Barra Lateral -->
        <div class="sidebar-actions-card">
          <button
            type="button"
            class="btn-espresso btn-block"
            @click="handleSubmit"
            :disabled="submitting || !isFormReady"
          >
            <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
            <i v-else class="pi pi-check"></i>
            <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Puerto' }}</span>
          </button>
          <button
            type="button"
            class="btn-outline-warm btn-block"
            @click="$emit('cancel')"
            :disabled="submitting"
          >
            <span>Cancelar y Volver</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'
import catalogoService from '../services/catalogoService'

const props = defineProps({
  puertoToEdit: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['cancel', 'saved'])

const submitting = ref(false)
const errorMessage = ref('')

const isEditMode = computed(() => !!props.puertoToEdit)

const form = reactive({
  codigoPuerto: '',
  nombre: '',
  pais: 'Perú',
  tipo: 'MARITIMO',
  estado: 'ACTIVO'
})

const tipoPuertoOptions = [
  { value: 'MARITIMO', label: 'Marítimo', icon: 'pi pi-compass', desc: 'Terminal portuario de mar, muelles y puertos comerciales.' },
  { value: 'AEREO', label: 'Aéreo', icon: 'pi pi-send', desc: 'Aeropuerto internacional y terminales aéreos de carga.' },
  { value: 'TERRESTRE', label: 'Terrestre', icon: 'pi pi-truck', desc: 'Punto de control fronterizo o terminal aduanero de carreteras.' },
  { value: 'FLUVIAL', label: 'Fluvial / Lacustre', icon: 'pi pi-map', desc: 'Puertos fluviales en cuencas amazónicas o lacustres.' }
]

watch(
  () => props.puertoToEdit,
  (puerto) => {
    errorMessage.value = ''
    if (puerto) {
      form.codigoPuerto = puerto.codigoPuerto || ''
      form.nombre = puerto.nombre || ''
      form.pais = puerto.pais || 'Perú'
      form.tipo = puerto.tipo || 'MARITIMO'
      form.estado = puerto.estado || 'ACTIVO'
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

const selectedTipoLabel = computed(() => {
  const opt = tipoPuertoOptions.find((t) => t.value === form.tipo)
  return opt ? opt.label : form.tipo
})

const selectedTipoIcon = computed(() => {
  const opt = tipoPuertoOptions.find((t) => t.value === form.tipo)
  return opt ? opt.icon : 'pi pi-compass'
})

const isFormReady = computed(() => {
  return !!form.codigoPuerto && !!form.nombre && !!form.pais && !!form.tipo
})

const handleSubmit = async () => {
  errorMessage.value = ''

  if (!form.codigoPuerto || !form.nombre || !form.pais) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*).'
    return
  }

  submitting.value = true
  try {
    let result
    if (isEditMode.value) {
      result = await catalogoService.updatePuerto(props.puertoToEdit.idPuerto, {
        codigoPuerto: form.codigoPuerto.trim().toUpperCase(),
        nombre: form.nombre.trim(),
        pais: form.pais.trim(),
        tipo: form.tipo,
        estado: form.estado
      })
    } else {
      result = await catalogoService.createPuerto({
        codigoPuerto: form.codigoPuerto.trim().toUpperCase(),
        nombre: form.nombre.trim(),
        pais: form.pais.trim(),
        tipo: form.tipo,
        estado: form.estado
      })
    }

    emit('saved', {
      isEdit: isEditMode.value,
      data: result.data || result
    })
  } catch (err) {
    console.error('Error al guardar puerto:', err)
    errorMessage.value =
      err.response?.data?.message ||
      err.response?.data?.data ||
      'No se pudo registrar el puerto. Verifique que el código no esté duplicado.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.puerto-registration-form-view {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  max-width: 1440px;
  margin: 0 auto;
  animation: fadeIn 0.25s ease-out;
}

/* 1. Header Bar */
.form-header-bar {
  background: var(--color-bg-hero);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: 1.75rem 2.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  font-size: 0.825rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  margin-bottom: 0.5rem;
  transition: color var(--transition-fast);
}

.btn-back-link:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
}

.mode-pill {
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill);
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.mode-create {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.mode-edit {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.form-main-title {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.01em;
  margin: 0 0 0.35rem;
}

.form-main-desc {
  font-size: 0.875rem;
  color: var(--color-text-subtle);
  margin: 0;
  max-width: 700px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

/* Error Banner */
.error-banner {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger-text);
  padding: 1rem 1.25rem;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 0.85rem;
  font-size: 0.875rem;
}

.error-content {
  flex: 1;
  display: flex;
  gap: 0.35rem;
}

.error-close-btn {
  background: transparent;
  border: none;
  color: var(--color-danger-text);
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
}

/* 2. Grid Principal */
.form-content-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.75rem;
  align-items: start;
}

.form-blocks-column {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-block-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1.75rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.block-header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.block-idx {
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.block-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.25rem;
}

.block-sub {
  font-size: 0.825rem;
  color: var(--color-text-subtle);
  margin: 0;
}

.block-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.highlight-group {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1rem;
}

.kicker-mini-badge {
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  background: var(--color-bg-surface);
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  border: 1px solid var(--color-border);
}

.field-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.label-with-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.field-hint {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
}

.input-icon-box {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 0.85rem;
  color: var(--color-text-subtle);
  font-size: 0.9rem;
  pointer-events: none;
}

.form-input {
  width: 100%;
  padding: 0.65rem 0.85rem 0.65rem 2.4rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-main);
  font-size: 0.875rem;
  font-family: inherit;
  transition: all var(--transition-fast);
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(214, 180, 118, 0.2);
}

.form-input:disabled {
  background: var(--color-bg-subtle);
  color: var(--color-text-subtle);
  cursor: not-allowed;
}

.code-input {
  font-family: monospace;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.shortcuts-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  flex-wrap: wrap;
  margin-top: 0.35rem;
}

.shortcuts-hint {
  font-size: 0.7rem;
  color: var(--color-text-subtle);
  font-weight: 500;
}

.chip-shortcut {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--color-text-main);
  padding: 0.15rem 0.45rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.chip-shortcut:hover {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

/* Tarjetas de Selección de Modalidad */
.type-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
  margin-top: 0.25rem;
}

.type-option-card {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.85rem 1rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  transition: all var(--transition-fast);
}

.type-option-card:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-border-strong);
  transform: translateY(-1px);
}

.type-option-card.active {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary), 0 3px 8px rgba(0, 0, 0, 0.1);
}

.type-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.type-card-badge {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.type-card-badge i {
  color: var(--color-accent-kicker);
}

.radio-indicator {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1.5px solid var(--color-border-strong);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  color: #ffffff;
  transition: all var(--transition-fast);
}

.type-option-card.active .radio-indicator {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.type-card-desc {
  font-size: 0.725rem;
  color: var(--color-text-muted);
  line-height: 1.35;
  margin: 0;
}

/* Pills de Estado */
.status-pills-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.status-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.55rem 0.95rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-main);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.status-pill-btn .pill-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.status-pill-btn:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-border-strong);
}

.status-pill-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

/* 3. Columna Lateral */
.form-sidebar-column {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  position: sticky;
  top: 90px;
}

.puerto-card-preview {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.preview-ribbon {
  background: var(--color-primary);
  color: var(--color-primary-text);
  padding: 0.65rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ribbon-text {
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.ribbon-id {
  font-size: 0.6rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
}

.preview-body {
  padding: 1.5rem 1.25rem 1.25rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.avatar-holder {
  position: relative;
  margin-bottom: 0.85rem;
}

.puerto-avatar {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.6rem;
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  border: 3px solid var(--color-border);
}

.status-indicator-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  border: 2px solid var(--color-bg-surface);
}

.status-indicator-dot.ok {
  background: #059669;
  color: #ffffff;
}

.status-indicator-dot.warn {
  background: #b91c1c;
  color: #ffffff;
}

.preview-name {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.2rem;
  line-height: 1.3;
}

.preview-tipo {
  font-size: 0.775rem;
  font-weight: 600;
  color: var(--color-accent-kicker);
}

.preview-pais {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 0.25rem;
  font-style: italic;
}

.preview-divider {
  width: 100%;
  height: 1px;
  background: var(--color-border-subtle);
  margin: 1rem 0;
}

.preview-data-rows {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  text-align: left;
}

.data-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.75rem;
  gap: 0.5rem;
}

.data-k {
  color: var(--color-text-subtle);
  font-weight: 600;
}

.data-v {
  font-weight: 600;
  color: var(--color-text-main);
}

.preview-footer {
  background: var(--color-bg-subtle);
  border-top: 1px solid var(--color-border-subtle);
  padding: 0.65rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.footer-chip {
  font-size: 0.625rem;
  font-weight: 700;
  color: var(--color-text-subtle);
  letter-spacing: 0.08em;
}

.footer-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.65rem;
  font-weight: 700;
  color: #854d0e;
}

:global(.dark-theme) .footer-status-pill {
  color: #fde68a;
}

.footer-status-pill.ok {
  color: #15803d;
}

:global(.dark-theme) .footer-status-pill.ok {
  color: #86efac;
}

.status-circle {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* Checklist Card */
.quality-checklist-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.15rem;
}

.checklist-title {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.checklist-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.checklist-items li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-subtle);
  transition: color var(--transition-fast);
}

.checklist-items li.complete {
  color: var(--color-text-main);
  font-weight: 500;
}

.text-success {
  color: #16a34a;
}
.text-muted {
  color: #b5a491;
}

/* Acciones en Sidebar */
.sidebar-actions-card {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.btn-block {
  width: 100%;
  justify-content: center;
  padding: 0.75rem 1rem;
}

@media (max-width: 1080px) {
  .form-content-grid {
    grid-template-columns: 1fr;
  }
  .form-sidebar-column {
    position: static;
  }
}

@media (max-width: 640px) {
  .form-header-bar {
    flex-direction: column;
    align-items: flex-start;
    padding: 1.25rem;
  }
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  .type-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
