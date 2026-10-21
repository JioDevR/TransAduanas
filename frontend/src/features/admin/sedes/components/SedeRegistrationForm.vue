<template>
  <div class="sede-registration-form-view">
    <!-- 1. Header Bar de Navegación y Acciones -->
    <div class="form-header-bar">
      <div class="header-titles">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('cancel')"
          title="Regresar al directorio de sedes"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver al Directorio de Sedes</span>
        </button>

        <div class="kicker-row">
          <span class="badge-kicker">INFRAESTRUCTURA & CONTROL TERRITORIAL</span>
          <span :class="['mode-pill', isEditMode ? 'mode-edit' : 'mode-create']">
            <i :class="isEditMode ? 'pi pi-pencil' : 'pi pi-plus'"></i>
            <span>{{ isEditMode ? 'MODO EDICIÓN' : 'MODO REGISTRO' }}</span>
          </span>
        </div>

        <h1 class="form-main-title">
          {{ isEditMode ? `Editar Sede: ${form.codigoSede}` : 'Registrar Nueva Sede o Terminal' }}
        </h1>
        <p class="form-main-desc">
          Configure los parámetros de la instalación aduanera, jurisdicción distrital, canales de enlace y habilitación operativa.
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
          <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Sede' }}</span>
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

    <!-- 2. Grid Principal: Bloques Indexados + Ficha Lateral -->
    <div class="form-content-grid">
      <!-- Columna Principal: Bloques de Formulario -->
      <div class="form-blocks-column">
        <!-- BLOQUE 01: IDENTIFICACIÓN Y TIPO -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Identificación y Clasificación Territorial</h3>
              <p class="block-sub">Código asignado por TI, nombre representativo y tipo de instalación.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Código de Sede con Atajos -->
            <div class="field-group highlight-group">
              <div class="label-with-badge">
                <label for="s-codigo" class="field-label">Código de Sede *</label>
                <span class="kicker-mini-badge">IDENTIFICADOR TI</span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-barcode input-icon"></i>
                <input
                  id="s-codigo"
                  v-model.trim="form.codigoSede"
                  type="text"
                  placeholder="Ej. SEDE-CALLAO-01 o LIM-CENTRAL"
                  class="form-input code-input"
                  required
                  maxlength="50"
                  :disabled="isEditMode || submitting"
                  @input="form.codigoSede = form.codigoSede.toUpperCase()"
                />
              </div>

              <!-- Atajos de Prefijo -->
              <div v-if="!isEditMode" class="shortcuts-row">
                <span class="shortcuts-hint">Prefijos frecuentes:</span>
                <button type="button" class="chip-shortcut" @click="applyPrefix('LIM-CENTRAL')">
                  + LIM-CENTRAL
                </button>
                <button type="button" class="chip-shortcut" @click="applyPrefix('SEDE-CALLAO-')">
                  + CALLAO-
                </button>
                <button type="button" class="chip-shortcut" @click="applyPrefix('AGENCIA-PAITA-')">
                  + PAITA-
                </button>
                <button type="button" class="chip-shortcut" @click="applyPrefix('TERM-MATARANI-')">
                  + MATARANI-
                </button>
              </div>
              <span class="field-hint" v-if="isEditMode">
                El código de sede no puede modificarse una vez creada la instalación.
              </span>
            </div>

            <!-- Nombre Oficial -->
            <div class="field-group">
              <label for="s-nombre" class="field-label">Nombre Oficial de la Sede *</label>
              <div class="input-icon-box">
                <i class="pi pi-building input-icon"></i>
                <input
                  id="s-nombre"
                  v-model.trim="form.nombre"
                  type="text"
                  placeholder="Ej. Sede Callao Muelle Sur — Despacho Marítimo"
                  class="form-input"
                  required
                  maxlength="100"
                  :disabled="submitting"
                />
              </div>
            </div>

            <!-- Selector Interactivo de Tipo de Instalación -->
            <div class="field-group">
              <label class="field-label">Tipo de Instalación Aduanera *</label>
              <div class="type-cards-grid">
                <div
                  v-for="t in tipoOptions"
                  :key="t.value"
                  :class="['type-option-card', { active: form.tipo === t.value }]"
                  @click="form.tipo = t.value"
                >
                  <div class="type-card-top">
                    <span class="type-card-badge">{{ t.label }}</span>
                    <div class="radio-indicator">
                      <i v-if="form.tipo === t.value" class="pi pi-check"></i>
                    </div>
                  </div>
                  <p class="type-card-desc">{{ t.desc }}</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 02: UBICACIÓN TERRITORIAL Y CONTACTO -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Ubicación Territorial y Canales de Enlace</h3>
              <p class="block-sub">Jurisdicción geográfica, dirección operativa y teléfonos de contacto.</p>
            </div>
          </div>

          <div class="block-body">
            <div class="fields-2col">
              <!-- Distrito -->
              <div class="field-group">
                <label for="s-distrito" class="field-label">Distrito / Jurisdicción *</label>
                <div class="input-icon-box">
                  <i class="pi pi-map-marker input-icon"></i>
                  <input
                    id="s-distrito"
                    v-model.trim="form.distrito"
                    type="text"
                    placeholder="Ej. Callao, Miraflores, Paita"
                    class="form-input"
                    required
                    maxlength="100"
                    :disabled="submitting"
                  />
                </div>
                <!-- Atajos distritales -->
                <div class="shortcuts-row">
                  <span class="shortcuts-hint">Rápidos:</span>
                  <button type="button" class="chip-shortcut" @click="form.distrito = 'Callao'">Callao</button>
                  <button type="button" class="chip-shortcut" @click="form.distrito = 'Miraflores'">Miraflores</button>
                  <button type="button" class="chip-shortcut" @click="form.distrito = 'Paita'">Paita</button>
                  <button type="button" class="chip-shortcut" @click="form.distrito = 'San Isidro'">San Isidro</button>
                </div>
              </div>

              <!-- Teléfono -->
              <div class="field-group">
                <label for="s-telefono" class="field-label">Teléfono / Central de Contacto</label>
                <div class="input-icon-box">
                  <i class="pi pi-phone input-icon"></i>
                  <input
                    id="s-telefono"
                    v-model.trim="form.telefono"
                    type="tel"
                    placeholder="Ej. (01) 456-7890 / +51 987654321"
                    class="form-input"
                    maxlength="50"
                    :disabled="submitting"
                  />
                </div>
                <span class="field-hint">Central telefónica de la sede para operadores y clientes.</span>
              </div>
            </div>

            <!-- Dirección Completa -->
            <div class="field-group">
              <label for="s-direccion" class="field-label">Dirección Fiscal / Operativa Completa *</label>
              <div class="input-icon-box">
                <i class="pi pi-compass input-icon"></i>
                <input
                  id="s-direccion"
                  v-model.trim="form.direccion"
                  type="text"
                  placeholder="Ej. Av. Guardia Chalaca 1250, Terminal Portuario Muelle Sur"
                  class="form-input"
                  required
                  maxlength="255"
                  :disabled="submitting"
                />
              </div>
            </div>
          </div>
        </section>

        <!-- BLOQUE 03: ESTADO OPERATIVO -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Habilitación y Estado Operativo</h3>
              <p class="block-sub">Determine si la sede está lista para despachos aduaneros y asignación de personal.</p>
            </div>
          </div>

          <div class="block-body">
            <div class="status-pills-row">
              <button
                v-for="st in estadoOptions"
                :key="st.value"
                type="button"
                :class="['status-pill-btn', { active: form.estado === st.value }]"
                @click="form.estado = st.value"
              >
                <span class="pill-dot"></span>
                <span>{{ st.label }}</span>
              </button>
            </div>
            <p class="status-explanation">
              {{ selectedEstadoDesc }}
            </p>
          </div>
        </section>
      </div>

      <!-- Columna Lateral: Ficha en Vivo y Checklist -->
      <div class="form-sidebar-column">
        <!-- Tarjeta de Ficha de Sede en Vivo -->
        <div class="sede-card-preview">
          <div class="preview-ribbon">
            <span class="ribbon-text">SEDE ADUANERA</span>
            <span class="ribbon-id">{{ form.codigoSede || 'SIN CÓDIGO' }}</span>
          </div>

          <div class="preview-body">
            <div class="avatar-holder">
              <div class="sede-avatar">
                <i :class="selectedTipoIcon"></i>
              </div>
              <div :class="['status-indicator-dot', form.estado === 'OPERATIVO' ? 'ok' : 'warn']">
                <i class="pi pi-check" v-if="form.estado === 'OPERATIVO'"></i>
                <i class="pi pi-clock" v-else></i>
              </div>
            </div>

            <h4 class="preview-name">{{ form.nombre || 'Nombre de la Sede' }}</h4>
            <span class="preview-tipo">{{ selectedTipoLabel }}</span>
            <span class="preview-distrito">{{ form.distrito ? `Jurisdicción: ${form.distrito}` : 'Sin distrito asignado' }}</span>

            <div class="preview-divider"></div>

            <div class="preview-data-rows">
              <div class="data-row">
                <span class="data-k">Código:</span>
                <span class="data-v">{{ form.codigoSede || '—' }}</span>
              </div>
              <div class="data-row">
                <span class="data-k">Tipo:</span>
                <span class="data-v">{{ selectedTipoLabel }}</span>
              </div>
              <div class="data-row">
                <span class="data-k">Distrito:</span>
                <span class="data-v">{{ form.distrito || '—' }}</span>
              </div>
              <div class="data-row">
                <span class="data-k">Teléfono:</span>
                <span class="data-v">{{ form.telefono || 'Sin teléfono' }}</span>
              </div>
              <div class="data-row">
                <span class="data-k">Dirección:</span>
                <span class="data-v data-v-truncate" :title="form.direccion">{{ form.direccion || '—' }}</span>
              </div>
            </div>
          </div>

          <div class="preview-footer">
            <span class="footer-chip">INFRAESTRUCTURA SGA</span>
            <span :class="['footer-status-pill', { ok: form.estado === 'OPERATIVO' }]">
              <span class="status-circle"></span>
              <span>{{ form.estado }}</span>
            </span>
          </div>
        </div>

        <!-- Checklist de Calidad -->
        <div class="quality-checklist-card">
          <h5 class="checklist-title">
            <i class="pi pi-check-circle text-success"></i>
            <span>Verificación de Infraestructura</span>
          </h5>
          <ul class="checklist-items">
            <li :class="{ complete: !!form.codigoSede }">
              <i :class="form.codigoSede ? 'pi pi-check text-success' : 'pi pi-circle text-muted'"></i>
              <span>Código territorial definido</span>
            </li>
            <li :class="{ complete: !!form.nombre }">
              <i :class="form.nombre ? 'pi pi-check text-success' : 'pi pi-circle text-muted'"></i>
              <span>Nombre representativo</span>
            </li>
            <li :class="{ complete: !!form.distrito }">
              <i :class="form.distrito ? 'pi pi-check text-success' : 'pi pi-circle text-muted'"></i>
              <span>Jurisdicción distrital</span>
            </li>
            <li :class="{ complete: !!form.direccion }">
              <i :class="form.direccion ? 'pi pi-check text-success' : 'pi pi-circle text-muted'"></i>
              <span>Dirección física completa</span>
            </li>
            <li :class="{ complete: !!form.tipo }">
              <i :class="form.tipo ? 'pi pi-check text-success' : 'pi pi-circle text-muted'"></i>
              <span>Modalidad de instalación asignada</span>
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
            <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Sede' }}</span>
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
import { ref, reactive, computed, watch } from 'vue'
import sedeService from '../services/sedeService'

const props = defineProps({
  sedeToEdit: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['cancel', 'saved'])

const submitting = ref(false)
const errorMessage = ref('')

const isEditMode = computed(() => !!props.sedeToEdit)

const form = reactive({
  codigoSede: '',
  nombre: '',
  tipo: 'SEDE_CENTRAL',
  distrito: '',
  direccion: '',
  telefono: '',
  estado: 'OPERATIVO'
})

const tipoOptions = [
  { value: 'SEDE_CENTRAL', label: 'Sede Central', desc: 'Oficina principal corporativa y administración TI.' },
  { value: 'AGENCIA', label: 'Agencia Aduanera', desc: 'Despacho de comercio exterior y atención a clientes.' },
  { value: 'SUCURSAL', label: 'Sucursal Regional', desc: 'Oficina descentralizada en provincia o frontera.' },
  { value: 'TERMINAL', label: 'Terminal / Depósito', desc: 'Almacén fiscal de contenedores y carga suelta.' }
]

const estadoOptions = [
  { value: 'OPERATIVO', label: 'OPERATIVO (Activo)' },
  { value: 'INACTIVO', label: 'INACTIVO (Suspendido)' },
  { value: 'MANTENIMIENTO', label: 'MANTENIMIENTO (En adecuación)' }
]

watch(
  () => props.sedeToEdit,
  (sede) => {
    errorMessage.value = ''
    if (sede) {
      form.codigoSede = sede.codigoSede || ''
      form.nombre = sede.nombre || ''
      form.tipo = sede.tipo || 'SEDE_CENTRAL'
      form.distrito = sede.distrito || ''
      form.direccion = sede.direccion || ''
      form.telefono = sede.telefono || ''
      form.estado = sede.estado || 'OPERATIVO'
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

const selectedTipoLabel = computed(() => {
  const opt = tipoOptions.find((t) => t.value === form.tipo)
  return opt ? opt.label : form.tipo
})

const selectedTipoIcon = computed(() => {
  switch (form.tipo) {
    case 'SEDE_CENTRAL':
      return 'pi pi-building'
    case 'AGENCIA':
      return 'pi pi-briefcase'
    case 'TERMINAL':
      return 'pi pi-box'
    default:
      return 'pi pi-home'
  }
})

const selectedEstadoDesc = computed(() => {
  switch (form.estado) {
    case 'OPERATIVO':
      return 'La sede está plenamente habilitada para asignación de colaboradores y operaciones aduaneras.'
    case 'MANTENIMIENTO':
      return 'La instalación se encuentra en proceso de inspección o adecuación técnica; se restringe el nuevo personal.'
    case 'INACTIVO':
    default:
      return 'La sede está suspendida temporalmente; no aparecerá disponible para nuevos despachos.'
  }
})

const isFormReady = computed(() => {
  return !!form.codigoSede && !!form.nombre && !!form.distrito && !!form.direccion && !!form.tipo
})

const handleSubmit = async () => {
  errorMessage.value = ''

  if (!form.codigoSede || !form.nombre || !form.distrito || !form.direccion) {
    errorMessage.value = 'Por favor complete todos los campos obligatorios (*).'
    return
  }

  submitting.value = true
  try {
    let result
    if (isEditMode.value) {
      result = await sedeService.updateSede(props.sedeToEdit.idSede, {
        nombre: form.nombre.trim(),
        tipo: form.tipo,
        distrito: form.distrito.trim(),
        direccion: form.direccion.trim(),
        telefono: form.telefono ? form.telefono.trim() : null,
        estado: form.estado
      })
    } else {
      result = await sedeService.createSede({
        codigoSede: form.codigoSede.trim().toUpperCase(),
        nombre: form.nombre.trim(),
        tipo: form.tipo,
        distrito: form.distrito.trim(),
        direccion: form.direccion.trim(),
        telefono: form.telefono ? form.telefono.trim() : null,
        estado: form.estado
      })
    }

    emit('saved', {
      isEdit: isEditMode.value,
      data: result.data || result
    })
  } catch (err) {
    console.error('Error al guardar sede:', err)
    errorMessage.value =
      err.response?.data?.message ||
      err.response?.data?.data ||
      'No se pudo completar el registro de la sede. Verifique que el código no esté duplicado.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.sede-registration-form-view {
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

.fields-2col {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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

/* Tarjetas de Selección de Tipo */
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

.status-explanation {
  font-size: 0.8rem;
  color: var(--color-text-subtle);
  margin: 0;
  line-height: 1.4;
}

/* 3. Columna Lateral: Ficha en Vivo */
.form-sidebar-column {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  position: sticky;
  top: 90px;
}

.sede-card-preview {
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

.sede-avatar {
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
  background: #d97706;
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

.preview-distrito {
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

.data-v-truncate {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  .fields-2col,
  .type-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
