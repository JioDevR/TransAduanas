<template>
  <div class="registration-form-view">
    <!-- 1. Barra Superior de Navegación & Acciones de la Ficha -->
    <div class="form-header-bar">
      <div class="header-breadcrumb-area">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('cancel')"
          :disabled="submitting"
          title="Regresar a la lista de usuarios"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver al Directorio de Personal</span>
        </button>

        <div class="header-titles">
          <div class="kicker-row">
            <span class="badge-kicker">PLANTILLA DE ALTA DE OPERADOR // SGA-FORM-00</span>
            <span :class="['mode-pill', isEditMode ? 'mode-edit' : 'mode-create']">
              <i :class="isEditMode ? 'pi pi-user-edit' : 'pi pi-user-plus'"></i>
              {{ isEditMode ? `MODO EDICIÓN (ID #${userToEdit?.idUsuario})` : 'NUEVO REGISTRO' }}
            </span>
          </div>
          <h2 class="form-main-title">
            {{ isEditMode ? `Ficha Técnica: @${form.username}` : 'Ficha de Registro y Acreditación de Personal' }}
          </h2>
          <p class="form-main-desc">
            {{
              isEditMode
                ? 'Actualice las credenciales, acreditaciones oficiales SUNAT y sede operativa del operador.'
                : 'Complete los datos legales, acreditaciones aduaneras SUNAT y credenciales para dar de alta al operador.'
            }}
          </p>
        </div>
      </div>

      <!-- Acciones de Cabecera -->
      <div class="header-actions">
        <button
          type="button"
          class="btn-outline-warm"
          @click="$emit('cancel')"
          :disabled="submitting"
        >
          <i class="pi pi-times"></i>
          <span>Descartar</span>
        </button>

        <button
          form="user-registration-form"
          type="submit"
          class="btn-espresso"
          :disabled="submitting"
        >
          <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-check"></i>
          <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Operador' }}</span>
        </button>
      </div>
    </div>

    <!-- Error Alert Banner -->
    <transition name="slide-fade">
      <div v-if="errorMessage" class="error-banner">
        <i class="pi pi-exclamation-triangle"></i>
        <div class="error-content">
          <strong>Error de Validación:</strong>
          <span>{{ errorMessage }}</span>
        </div>
        <button type="button" class="error-close-btn" @click="errorMessage = ''">
          <i class="pi pi-times"></i>
        </button>
      </div>
    </transition>

    <!-- 2. Grid Principal: Formulario Estructurado (68%) + Ficha Resumen Live (32%) -->
    <div class="form-content-grid">
      <!-- COLUMNA PRINCIPAL: BLOQUES DE CAMPOS -->
      <form id="user-registration-form" @submit.prevent="handleSubmit" class="form-blocks-column">
        <!-- ======================================================== -->
        <!-- BLOQUE 01: IDENTIDAD LEGAL & ACREDITACIÓN SUNAT -->
        <!-- ======================================================== -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Identificación Oficial & Acreditación SUNAT</h3>
              <p class="block-sub">
                Documentación de identidad legal y códigos de operador ante la autoridad aduanera.
              </p>
            </div>
          </div>

          <div class="block-body">
            <!-- Selector de Tipo de Documento -->
            <div class="field-row">
              <label class="field-label">Tipo de Documento de Identidad *</label>
              <div class="doc-type-pills">
                <button
                  v-for="tipo in tiposDocumento"
                  :key="tipo.id"
                  type="button"
                  :class="['doc-type-pill', { active: form.tipoDocumento === tipo.id }]"
                  @click="form.tipoDocumento = tipo.id"
                >
                  <i :class="tipo.icon"></i>
                  <span>{{ tipo.label }}</span>
                </button>
              </div>
            </div>

            <div class="fields-2col">
              <!-- Número de Documento -->
              <div class="field-group">
                <label for="f-doc-num" class="field-label">Número de Documento *</label>
                <div class="input-icon-box">
                  <i class="pi pi-id-card input-icon"></i>
                  <input
                    id="f-doc-num"
                    v-model.trim="form.numeroDocumento"
                    type="text"
                    :placeholder="docPlaceholder"
                    :maxlength="docMaxLength"
                    class="form-input"
                    required
                    :disabled="submitting"
                  />
                </div>
                <span class="field-hint">Ej: 8 dígitos para DNI, 12 para CE o 11 para RUC.</span>
              </div>

              <!-- Código SUNAT de Despacho (Destacado Especial) -->
              <div class="field-group highlight-sunat-group">
                <div class="label-with-badge">
                  <label for="f-cod-sunat" class="field-label">
                    Código de Acreditación SUNAT
                  </label>
                  <span class="sunat-kicker-badge">ADUANAS</span>
                </div>
                <div class="input-icon-box sunat-input-box">
                  <i class="pi pi-verified input-icon sunat-icon"></i>
                  <input
                    id="f-cod-sunat"
                    v-model.trim="form.codigoSunat"
                    type="text"
                    placeholder="Ej. SUNAT-DESP-0104 o AUX-8921"
                    class="form-input sunat-input"
                    :disabled="submitting"
                  />
                </div>
                <!-- Atajos de Prefijos SUNAT -->
                <div class="sunat-shortcuts">
                  <span class="shortcuts-hint">Prefijos frecuentes:</span>
                  <button
                    type="button"
                    class="chip-prefix"
                    @click="applySunatPrefix('SUNAT-DESP-')"
                    title="Auxiliar de Despacho SUNAT"
                  >
                    + DESP-
                  </button>
                  <button
                    type="button"
                    class="chip-prefix"
                    @click="applySunatPrefix('SUNAT-AUX-')"
                    title="Auxiliar Operativo"
                  >
                    + AUX-
                  </button>
                  <button
                    type="button"
                    class="chip-prefix"
                    @click="applySunatPrefix('SUNAT-LIQ-')"
                    title="Liquidador Aduanero"
                  >
                    + LIQ-
                  </button>
                  <button
                    type="button"
                    class="chip-prefix"
                    @click="applySunatPrefix('SUNAT-COM-')"
                    title="Comercial / Ventas"
                  >
                    + COM-
                  </button>
                </div>
              </div>
            </div>

            <!-- Nombres y Apellidos -->
            <div class="fields-2col">
              <div class="field-group">
                <label for="f-nombres" class="field-label">Nombres Completos *</label>
                <div class="input-icon-box">
                  <i class="pi pi-user input-icon"></i>
                  <input
                    id="f-nombres"
                    v-model.trim="form.nombres"
                    type="text"
                    placeholder="Ej. Carlos Juan"
                    class="form-input"
                    required
                    :disabled="submitting"
                  />
                </div>
              </div>

              <div class="field-group">
                <label for="f-apellidos" class="field-label">Apellidos Completos *</label>
                <div class="input-icon-box">
                  <i class="pi pi-user input-icon"></i>
                  <input
                    id="f-apellidos"
                    v-model.trim="form.apellidos"
                    type="text"
                    placeholder="Ej. Álvarez Ruiz"
                    class="form-input"
                    required
                    :disabled="submitting"
                  />
                </div>
              </div>
            </div>

            <!-- Cargo o Puesto en la Agencia -->
            <div class="field-group">
              <label for="f-cargo" class="field-label">Cargo / Puesto en la Agencia</label>
              <div class="input-icon-box">
                <i class="pi pi-briefcase input-icon"></i>
                <input
                  id="f-cargo"
                  v-model.trim="form.cargoTitulo"
                  type="text"
                  placeholder="Ej. Auxiliar de Despacho Aduanero"
                  class="form-input"
                  :disabled="submitting"
                />
              </div>
              <div class="cargo-quick-tags">
                <span class="shortcuts-hint">Cargos estándar:</span>
                <button
                  type="button"
                  class="chip-cargo"
                  @click="form.cargoTitulo = 'Auxiliar de Despacho Aduanero'"
                >
                  Auxiliar de Despacho
                </button>
                <button
                  type="button"
                  class="chip-cargo"
                  @click="form.cargoTitulo = 'Liquidador Aduanero Oficial'"
                >
                  Liquidador
                </button>
                <button
                  type="button"
                  class="chip-cargo"
                  @click="form.cargoTitulo = 'Ejecutivo Comercial de Ventas'"
                >
                  Ejecutivo Ventas
                </button>
                <button
                  type="button"
                  class="chip-cargo"
                  @click="form.cargoTitulo = 'Coordinador de Logística y Almacén'"
                >
                  Logística & Almacén
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- ======================================================== -->
        <!-- BLOQUE 02: SEGURIDAD & CREDENCIALES DE ACCESO TI -->
        <!-- ======================================================== -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Credenciales de Acceso & Contacto Institucional</h3>
              <p class="block-sub">
                Parámetros de autenticación al sistema SGA y canales de comunicación corporativa.
              </p>
            </div>
          </div>

          <div class="block-body">
            <div class="fields-2col">
              <!-- Username -->
              <div class="field-group">
                <div class="label-with-badge">
                  <label for="f-username" class="field-label">Nombre de Usuario (Username) *</label>
                  <span v-if="!isEditMode && isAutoUsername && form.username" class="auto-badge">
                    <i class="pi pi-bolt"></i> Auto-generado
                  </span>
                </div>
                <div class="input-icon-box">
                  <i class="pi pi-at input-icon"></i>
                  <input
                    id="f-username"
                    v-model.trim="form.username"
                    type="text"
                    placeholder="Ej. calvarez, c.alvarez"
                    class="form-input"
                    required
                    :disabled="isEditMode || submitting"
                    @input="isAutoUsername = false"
                  />
                  <button
                    v-if="!isEditMode && usernameSuggestions.length > 0"
                    type="button"
                    class="btn-regenerate-username"
                    @click="regenerateUsername"
                    title="Alternar otra combinación de nombre y apellido"
                  >
                    <i class="pi pi-sync"></i>
                  </button>
                </div>

                <!-- Sugerencias combinando Nombre y Apellido -->
                <div v-if="!isEditMode && usernameSuggestions.length > 0" class="username-combinations-row">
                  <span class="shortcuts-hint"><i class="pi pi-sparkles"></i> Combinaciones sugeridas (click para aplicar):</span>
                  <div class="combination-chips">
                    <button
                      v-for="sug in usernameSuggestions"
                      :key="sug"
                      type="button"
                      :class="['chip-combo', { active: form.username === sug }]"
                      @click="selectUsernameSuggestion(sug)"
                    >
                      @{{ sug }}
                    </button>
                  </div>
                </div>

                <span class="field-hint" v-if="isEditMode">
                  El nombre de usuario es el identificador único del sistema y no puede cambiarse.
                </span>
                <span class="field-hint" v-else>
                  Combinación sugerida automáticamente a partir de nombres y apellidos. Puede seleccionar otra o editar libremente.
                </span>
              </div>

              <!-- Contraseña -->
              <div class="field-group">
                <label for="f-password" class="field-label">
                  {{ isEditMode ? 'Nueva Contraseña (Opcional)' : 'Contraseña de Acceso *' }}
                </label>
                <div class="input-icon-box">
                  <i class="pi pi-lock input-icon"></i>
                  <input
                    id="f-password"
                    v-model="form.password"
                    :type="showPassword ? 'text' : 'password'"
                    :placeholder="isEditMode ? 'En blanco para conservar actual' : 'Mínimo 6 caracteres'"
                    class="form-input"
                    :required="!isEditMode"
                    :disabled="submitting"
                  />
                  <button
                    type="button"
                    class="toggle-eye-btn"
                    @click="showPassword = !showPassword"
                    title="Mostrar/ocultar contraseña"
                  >
                    <i :class="showPassword ? 'pi pi-eye-slash' : 'pi pi-eye'"></i>
                  </button>
                </div>
                <span class="field-hint" v-if="!isEditMode">
                  Debe contener al menos 6 caracteres (recomendado mayúsculas y números).
                </span>
              </div>
            </div>

            <div class="fields-2col">
              <!-- Correo Institucional -->
              <div class="field-group">
                <label for="f-correo" class="field-label">Correo Electrónico Institucional *</label>
                <div class="input-icon-box">
                  <i class="pi pi-envelope input-icon"></i>
                  <input
                    id="f-correo"
                    v-model.trim="form.correoInstitucional"
                    type="email"
                    placeholder="nombre@sga.pe"
                    class="form-input"
                    required
                    :disabled="submitting"
                  />
                </div>
                <span class="field-hint">Canal para notificaciones de despachos y auditorías.</span>
              </div>

              <!-- Teléfono Directo -->
              <div class="field-group">
                <label for="f-telefono" class="field-label">Teléfono / Celular de Contacto</label>
                <div class="input-icon-box">
                  <i class="pi pi-phone input-icon"></i>
                  <input
                    id="f-telefono"
                    v-model.trim="form.telefono"
                    type="text"
                    placeholder="+51 987 654 321"
                    class="form-input"
                    :disabled="submitting"
                  />
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- ======================================================== -->
        <!-- BLOQUE 03: ASIGNACIÓN DE ROL & SEDE TERRITORIAL -->
        <!-- ======================================================== -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Perfil Operativo & Asignación Territorial</h3>
              <p class="block-sub">
                Determinación del alcance funcional del operador y la sede territorial aduanera.
              </p>
            </div>
          </div>

          <div class="block-body">
            <!-- Selector Visual de Roles mediante Tarjetas Interactivas -->
            <div class="field-row">
              <div class="label-with-badge">
                <label class="field-label">Rol del Sistema (Perfil de Permisos) *</label>
                <span class="roles-count-pill">{{ roles.length }} roles disponibles</span>
              </div>

              <div class="role-cards-grid">
                <div
                  v-for="r in roles"
                  :key="r.idRol"
                  :class="['role-option-card', { active: Number(form.idRol) === Number(r.idRol) }]"
                  @click="form.idRol = r.idRol"
                >
                  <div class="role-card-top">
                    <span :class="['role-badge-tag', getRoleClass(r.nombreRol)]">
                      {{ ROLE_LABELS[r.nombreRol] || r.nombreRol }}
                    </span>
                    <span class="radio-indicator">
                      <i v-if="Number(form.idRol) === Number(r.idRol)" class="pi pi-check"></i>
                    </span>
                  </div>
                  <div class="role-card-desc">
                    {{ r.descripcion || getRoleDescription(r.nombreRol) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Selector de Sede Operativa -->
            <div class="field-group">
              <label for="f-sede" class="field-label">Sede Territorial Asignada *</label>
              <div class="input-icon-box">
                <i class="pi pi-building input-icon"></i>
                <select
                  id="f-sede"
                  v-model="form.idSede"
                  class="form-select"
                  required
                  :disabled="submitting"
                >
                  <option value="" disabled>Seleccione la sede operativa...</option>
                  <option v-for="s in sedes" :key="s.idSede" :value="s.idSede">
                    {{ s.codigoSede }} — {{ s.nombre }} ({{ s.distrito }})
                  </option>
                </select>
              </div>
              <span class="field-hint" v-if="selectedSedeDetails">
                Dirección física: {{ selectedSedeDetails.direccion }} | Teléfono: {{ selectedSedeDetails.telefono || '—' }}
              </span>
            </div>
          </div>
        </section>
      </form>

      <!-- ======================================================== -->
      <!-- COLUMNA LATERAL: FICHA CREDENCIAL EN VIVO & VALIDACIONES -->
      <!-- ======================================================== -->
      <aside class="form-sidebar-column">
        <!-- Tarjeta Ficha Credencial Fotocheck SGA -->
        <div class="credential-card-preview">
          <div class="credential-ribbon">
            <span class="ribbon-text">SGA • GESTIÓN ADUANERA</span>
            <span class="ribbon-id">FICHA OPERADOR</span>
          </div>

          <div class="credential-body">
            <div class="avatar-holder">
              <div class="credential-avatar" :style="avatarBgStyle">
                {{ previewInitials }}
              </div>
              <div v-if="form.codigoSunat" class="verified-dot" title="Acreditado SUNAT">
                <i class="pi pi-check"></i>
              </div>
            </div>

            <div class="credential-names">
              <h4 class="cred-fullname">
                {{ form.nombres || form.apellidos ? `${form.nombres} ${form.apellidos}` : 'Nombre del Operador' }}
              </h4>
              <div class="cred-username">@{{ form.username || 'usuario' }}</div>
              <div class="cred-cargo">{{ form.cargoTitulo || 'Sin cargo asignado' }}</div>
            </div>

            <div class="credential-divider"></div>

            <div class="credential-data-rows">
              <div class="cred-row">
                <span class="cred-k">Documento:</span>
                <span class="cred-v">
                  {{ form.tipoDocumento }}: {{ form.numeroDocumento || '—' }}
                </span>
              </div>

              <!-- Insignia SUNAT destacada -->
              <div class="cred-row sunat-row">
                <span class="cred-k">Acreditación:</span>
                <span v-if="form.codigoSunat" class="cred-sunat-badge">
                  <i class="pi pi-verified"></i> {{ form.codigoSunat }}
                </span>
                <span v-else class="cred-v text-muted">Sin código SUNAT</span>
              </div>

              <div class="cred-row">
                <span class="cred-k">Perfil Rol:</span>
                <span class="cred-v">
                  <span :class="['role-badge-tag-mini', getRoleClass(selectedRoleName)]">
                    {{ ROLE_LABELS[selectedRoleName] || selectedRoleName || 'NO ASIGNADO' }}
                  </span>
                </span>
              </div>

              <div class="cred-row">
                <span class="cred-k">Sede:</span>
                <span class="cred-v cred-sede">
                  {{ selectedSedeDetails ? selectedSedeDetails.codigoSede : 'Sin sede' }}
                </span>
              </div>

              <div class="cred-row">
                <span class="cred-k">Correo:</span>
                <span class="cred-v cred-email" :title="form.correoInstitucional">
                  {{ form.correoInstitucional || '—' }}
                </span>
              </div>
            </div>
          </div>

          <div class="credential-footer">
            <span class="footer-chip">PERÚ • ADUANAS</span>
            <span class="footer-status-pill" :class="{ ok: isFormReady }">
              <span class="status-circle"></span>
              {{ isFormReady ? 'Ficha Lista' : 'Incompleta' }}
            </span>
          </div>
        </div>

        <!-- Checklist de Calidad del Registro -->
        <div class="quality-checklist-card">
          <h4 class="checklist-title">
            <i class="pi pi-list-check"></i>
            <span>Requisitos para el Registro</span>
          </h4>
          <ul class="checklist-items">
            <li :class="{ complete: !!form.numeroDocumento && !!form.tipoDocumento }">
              <i :class="form.numeroDocumento ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Documento de Identidad</span>
            </li>
            <li :class="{ complete: !!form.nombres && !!form.apellidos }">
              <i :class="form.nombres && form.apellidos ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Nombres y Apellidos completos</span>
            </li>
            <li :class="{ complete: !!form.username }">
              <i :class="form.username ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Nombre de usuario único</span>
            </li>
            <li :class="{ complete: isEditMode || (!!form.password && form.password.length >= 6) }">
              <i :class="isEditMode || (form.password && form.password.length >= 6) ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Contraseña segura (mín. 6 caracteres)</span>
            </li>
            <li :class="{ complete: !!form.correoInstitucional && form.correoInstitucional.includes('@') }">
              <i :class="form.correoInstitucional && form.correoInstitucional.includes('@') ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Correo institucional válido</span>
            </li>
            <li :class="{ complete: !!form.idRol }">
              <i :class="form.idRol ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Perfil de Rol asignado</span>
            </li>
            <li :class="{ complete: !!form.idSede }">
              <i :class="form.idSede ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Sede territorial asignada</span>
            </li>
          </ul>
        </div>

        <!-- Botones de Acción Fijos del Panel Lateral -->
        <div class="sidebar-actions-card">
          <button
            type="button"
            class="btn-espresso btn-block"
            @click="handleSubmit"
            :disabled="submitting || !isFormReady"
          >
            <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
            <i v-else class="pi pi-save"></i>
            <span>{{ isEditMode ? 'Guardar Cambios' : 'Registrar Operador' }}</span>
          </button>

          <button
            type="button"
            class="btn-outline-warm btn-block"
            @click="$emit('cancel')"
            :disabled="submitting"
          >
            <i class="pi pi-arrow-left"></i>
            <span>Cancelar y Volver</span>
          </button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ROLES, ROLE_LABELS } from '@/core/auth/roles'

const props = defineProps({
  userToEdit: {
    type: Object,
    default: null
  },
  roles: {
    type: Array,
    default: () => []
  },
  sedes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['cancel', 'saved'])

const submitting = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)

const isEditMode = computed(() => !!props.userToEdit)

const tiposDocumento = [
  { id: 'DNI', label: 'DNI (Nacional)', icon: 'pi pi-id-card' },
  { id: 'CE', label: 'Carnet Extranjería (CE)', icon: 'pi pi-globe' },
  { id: 'PASAPORTE', label: 'Pasaporte', icon: 'pi pi-book' },
  { id: 'RUC', label: 'RUC Personal', icon: 'pi pi-building' }
]

const form = reactive({
  username: '',
  password: '',
  nombres: '',
  apellidos: '',
  correoInstitucional: '',
  idRol: '',
  idSede: '',
  tipoDocumento: 'DNI',
  numeroDocumento: '',
  telefono: '',
  cargoTitulo: '',
  codigoSunat: ''
})

const docPlaceholder = computed(() => {
  if (form.tipoDocumento === 'DNI') return 'Ej: 45892104 (8 dígitos)'
  if (form.tipoDocumento === 'CE') return 'Ej: 001293849102 (12 dígitos)'
  if (form.tipoDocumento === 'RUC') return 'Ej: 10458921041 (11 dígitos)'
  return 'Número de documento de viaje'
})

const docMaxLength = computed(() => {
  if (form.tipoDocumento === 'DNI') return 8
  if (form.tipoDocumento === 'RUC') return 11
  return 20
})

// Auto-generación inteligente de nombres de usuario combinando Nombre y Apellidos
const isAutoUsername = ref(true)
const selectedSuggestionIndex = ref(0)

watch(
  () => props.userToEdit,
  (user) => {
    errorMessage.value = ''
    if (user) {
      form.username = user.username || ''
      form.password = ''
      form.nombres = user.nombres || ''
      form.apellidos = user.apellidos || ''
      form.correoInstitucional = user.correoInstitucional || ''
      form.idRol = user.idRol || ''
      form.idSede = user.idSede || ''
      form.tipoDocumento = user.tipoDocumento || 'DNI'
      form.numeroDocumento = user.numeroDocumento || ''
      form.telefono = user.telefono || ''
      form.cargoTitulo = user.cargoTitulo || ''
      form.codigoSunat = user.codigoSunat || ''
    } else {
      form.username = ''
      form.password = ''
      form.nombres = ''
      form.apellidos = ''
      form.correoInstitucional = ''
      form.idRol = props.roles.length > 0 ? props.roles[0].idRol : ''
      form.idSede = props.sedes.length > 0 ? props.sedes[0].idSede : ''
      form.tipoDocumento = 'DNI'
      form.numeroDocumento = ''
      form.telefono = ''
      form.cargoTitulo = ''
      form.codigoSunat = ''
      isAutoUsername.value = true
      selectedSuggestionIndex.value = 0
    }
  },
  { immediate: true }
)

watch(
  () => props.roles,
  (newRoles) => {
    if (!props.userToEdit && newRoles.length > 0 && !form.idRol) {
      form.idRol = newRoles[0].idRol
    }
  }
)

watch(
  () => props.sedes,
  (newSedes) => {
    if (!props.userToEdit && newSedes.length > 0 && !form.idSede) {
      form.idSede = newSedes[0].idSede
    }
  }
)

const sanitizeForUsername = (str) => {
  if (!str) return ''
  return str
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9]/g, '')
    .toLowerCase()
}

const usernameSuggestions = computed(() => {
  if (!form.nombres || !form.apellidos) return []

  const firstNames = form.nombres.trim().split(/\s+/).map(sanitizeForUsername).filter(Boolean)
  const lastNames = form.apellidos.trim().split(/\s+/).map(sanitizeForUsername).filter(Boolean)

  if (firstNames.length === 0 || lastNames.length === 0) return []

  const n1 = firstNames[0]
  const n2 = firstNames.length > 1 ? firstNames[1] : ''
  const a1 = lastNames[0]
  const a2 = lastNames.length > 1 ? lastNames[1] : ''

  const combos = []

  // Combinación 1: Inicial primer nombre + primer apellido (ej. calvarez)
  combos.push(`${n1.charAt(0)}${a1}`)

  // Combinación 2: Inicial primer nombre + . + primer apellido (ej. c.alvarez)
  combos.push(`${n1.charAt(0)}.${a1}`)

  // Combinación 3: Primer nombre + . + inicial apellido (ej. carlos.a)
  combos.push(`${n1}.${a1.charAt(0)}`)

  // Combinación 4: Inicial primer nombre + inicial segundo nombre + primer apellido (ej. cjalvarez)
  if (n2) {
    combos.push(`${n1.charAt(0)}${n2.charAt(0)}${a1}`)
  }

  // Combinación 5: Inicial primer nombre + primer apellido + inicial segundo apellido (ej. calvarezr)
  if (a2) {
    combos.push(`${n1.charAt(0)}${a1}${a2.charAt(0)}`)
  }

  // Combinación 6: 3 letras nombre + 3 letras apellido (ej. caralv)
  if (n1.length >= 3 && a1.length >= 3) {
    combos.push(`${n1.substring(0, 3)}${a1.substring(0, 3)}`)
  }

  return [...new Set(combos)]
})

watch(
  [() => form.nombres, () => form.apellidos],
  () => {
    if (!isEditMode.value && isAutoUsername.value) {
      if (usernameSuggestions.value.length > 0) {
        const idx = selectedSuggestionIndex.value % usernameSuggestions.value.length
        form.username = usernameSuggestions.value[idx]
        if (!form.correoInstitucional || form.correoInstitucional.endsWith('@aduanas-sga.pe')) {
          form.correoInstitucional = `${form.username}@aduanas-sga.pe`
        }
      }
    }
  }
)

const selectUsernameSuggestion = (sug) => {
  form.username = sug
  isAutoUsername.value = false
  if (!form.correoInstitucional || form.correoInstitucional.endsWith('@aduanas-sga.pe')) {
    form.correoInstitucional = `${sug}@aduanas-sga.pe`
  }
}

const regenerateUsername = () => {
  if (usernameSuggestions.value.length > 0) {
    selectedSuggestionIndex.value = (selectedSuggestionIndex.value + 1) % usernameSuggestions.value.length
    form.username = usernameSuggestions.value[selectedSuggestionIndex.value]
    isAutoUsername.value = true
    if (!form.correoInstitucional || form.correoInstitucional.endsWith('@aduanas-sga.pe')) {
      form.correoInstitucional = `${form.username}@aduanas-sga.pe`
    }
  }
}

const applySunatPrefix = (prefix) => {
  if (!form.codigoSunat || !form.codigoSunat.startsWith('SUNAT-')) {
    form.codigoSunat = prefix
  }
}

const selectedSedeDetails = computed(() => {
  if (!form.idSede) return null
  return props.sedes.find((s) => Number(s.idSede) === Number(form.idSede))
})

const selectedRoleName = computed(() => {
  if (!form.idRol) return ''
  const r = props.roles.find((role) => Number(role.idRol) === Number(form.idRol))
  return r ? r.nombreRol : ''
})

const previewInitials = computed(() => {
  const n = form.nombres?.charAt(0) || ''
  const a = form.apellidos?.charAt(0) || ''
  const init = (n + a).toUpperCase()
  return init || form.username?.substring(0, 2).toUpperCase() || 'OP'
})

const avatarBgStyle = computed(() => {
  return {
    background: '#352f2a',
    color: '#efe7dd'
  }
})

const isFormReady = computed(() => {
  const hasDoc = !!form.numeroDocumento && !!form.tipoDocumento
  const hasNames = !!form.nombres && !!form.apellidos
  const hasUsername = !!form.username
  const hasPassword = isEditMode.value || (!!form.password && form.password.length >= 6)
  const hasEmail = !!form.correoInstitucional && form.correoInstitucional.includes('@')
  const hasRol = !!form.idRol
  const hasSede = !!form.idSede
  return hasDoc && hasNames && hasUsername && hasPassword && hasEmail && hasRol && hasSede
})

const getRoleClass = (roleName) => {
  const map = {
    [ROLES.ADMINISTRADOR_TI]: 'role-admin',
    [ROLES.GERENTE_GENERAL]: 'role-gerente',
    [ROLES.AGENTE_COMERCIAL]: 'role-ventas',
    [ROLES.SUPERVISOR_ADUANERO]: 'role-supervision',
    [ROLES.AGENTE_ADUANERO]: 'role-agente',
    [ROLES.SUPERVISOR_TRANSPORTE]: 'role-transporte',
    [ROLES.CONTADOR]: 'role-finanzas',
    [ROLES.ASESOR_LEGAL]: 'role-legal'
  }
  return map[roleName] || 'role-default'
}

const getRoleDescription = (roleName) => {
  const map = {
    [ROLES.ADMINISTRADOR_TI]: 'Administración técnica, soporte de infraestructura y cuentas de usuario.',
    [ROLES.GERENTE_GENERAL]: 'Supervisión ejecutiva integral, gestión de sedes y catálogos auxiliares.',
    [ROLES.AGENTE_COMERCIAL]: 'Cotizaciones comerciales, prospectos y directorio de clientes.',
    [ROLES.SUPERVISOR_ADUANERO]: 'Aforo aduanero, liquidación tributaria y revisión técnica.',
    [ROLES.AGENTE_ADUANERO]: 'Gestión aduanera, control de almacén, bultos y despachos.',
    [ROLES.SUPERVISOR_TRANSPORTE]: 'Despacho terrestre, guías de remisión y control de flota de carga.',
    [ROLES.CONTADOR]: 'Cobranza, facturación, cuentas por pagar y reportes contables.',
    [ROLES.ASESOR_LEGAL]: 'Auditoría aduanera, recursos de reclamación y cumplimiento normativo.'
  }
  return map[roleName] || 'Operador especializado del sistema aduanero.'
}

const handleSubmit = () => {
  errorMessage.value = ''

  if (!form.numeroDocumento) {
    errorMessage.value = 'El número de documento de identidad es obligatorio.'
    return
  }
  if (!form.nombres || !form.apellidos) {
    errorMessage.value = 'Los nombres y apellidos son obligatorios.'
    return
  }
  if (!form.username) {
    errorMessage.value = 'El nombre de usuario es obligatorio.'
    return
  }
  if (!isEditMode.value && (!form.password || form.password.length < 6)) {
    errorMessage.value = 'La contraseña debe tener al menos 6 caracteres.'
    return
  }
  if (!form.correoInstitucional || !form.correoInstitucional.includes('@')) {
    errorMessage.value = 'Ingrese un correo institucional válido.'
    return
  }
  if (!form.idRol) {
    errorMessage.value = 'Debe seleccionar un rol del sistema.'
    return
  }
  if (!form.idSede) {
    errorMessage.value = 'Debe asignar una sede operativa territorial.'
    return
  }

  submitting.value = true

  const payload = {
    idRol: Number(form.idRol),
    idSede: Number(form.idSede),
    nombres: form.nombres.trim(),
    apellidos: form.apellidos.trim(),
    correoInstitucional: form.correoInstitucional.trim(),
    tipoDocumento: form.tipoDocumento,
    numeroDocumento: form.numeroDocumento.trim(),
    telefono: form.telefono ? form.telefono.trim() : null,
    cargoTitulo: form.cargoTitulo ? form.cargoTitulo.trim() : null,
    codigoSunat: form.codigoSunat ? form.codigoSunat.trim().toUpperCase() : null
  }

  if (!isEditMode.value) {
    payload.username = form.username.trim()
    payload.password = form.password
  } else if (form.password && form.password.trim().length > 0) {
    payload.password = form.password
  }

  emit('saved', {
    isEdit: isEditMode.value,
    userId: props.userToEdit?.idUsuario,
    payload
  })

  submitting.value = false
}
</script>

<style scoped>
.registration-form-view {
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

/* Tarjetas de Bloques Indexados */
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

/* Filas y Campos */
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

.form-input,
.form-select {
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

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(214, 180, 118, 0.2);
}

.form-input:disabled,
.form-select:disabled {
  background: var(--color-bg-subtle);
  color: var(--color-text-subtle);
  cursor: not-allowed;
}

.toggle-eye-btn {
  position: absolute;
  right: 0.75rem;
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.25rem;
}

.auto-badge {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.btn-regenerate-username {
  position: absolute;
  right: 0.75rem;
  background: transparent;
  border: none;
  color: var(--color-accent-kicker);
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  transition: transform var(--transition-fast);
}

.btn-regenerate-username:hover {
  transform: rotate(180deg);
}

.username-combinations-row {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-top: 0.35rem;
}

.combination-chips {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.chip-combo {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-sm);
  font-size: 0.725rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.chip-combo:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.chip-combo.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
}

/* Píldoras de Tipo de Documento */
.doc-type-pills {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.doc-type-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.5rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-main);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.doc-type-pill:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-border-strong);
}

.doc-type-pill.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

/* Campo Destacado SUNAT */
.highlight-sunat-group {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.85rem 1rem;
}

.sunat-kicker-badge {
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #92400e;
  background: #fef3c7;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  border: 1px solid #fde68a;
}

:global(.dark-theme) .sunat-kicker-badge {
  background: rgba(245, 158, 11, 0.2);
  color: #fde68a;
  border-color: rgba(245, 158, 11, 0.4);
}

.sunat-icon {
  color: #b45309;
}

:global(.dark-theme) .sunat-icon {
  color: #fde68a;
}

.sunat-input {
  border-color: var(--color-border-strong);
  font-weight: 600;
  letter-spacing: 0.02em;
}

.sunat-shortcuts,
.cargo-quick-tags {
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

.chip-prefix,
.chip-cargo {
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

.chip-prefix:hover,
.chip-cargo:hover {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

/* Selector Interactivo de Roles */
.roles-count-pill {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--color-text-subtle);
}

.role-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
  margin-top: 0.35rem;
}

.role-option-card {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.85rem 1rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  transition: all var(--transition-fast);
}

.role-option-card:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-border-strong);
  transform: translateY(-1px);
}

.role-option-card.active {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary), 0 3px 8px rgba(0, 0, 0, 0.1);
}

.role-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.role-option-card.active .radio-indicator {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.role-card-desc {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.35;
}

/* Badges de Roles */
.role-badge-tag {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

.role-badge-tag-mini {
  font-size: 0.625rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 3px;
}

.role-admin {
  background: #352f2a;
  color: #efe7dd;
}
.role-ventas {
  background: #d97706;
  color: #ffffff;
}
.role-logistica {
  background: #0284c7;
  color: #ffffff;
}
.role-liquidador {
  background: #059669;
  color: #ffffff;
}
.role-legal {
  background: #7c3aed;
  color: #ffffff;
}
.role-finanzas {
  background: #b45309;
  color: #ffffff;
}
.role-transporte {
  background: #4b5563;
  color: #ffffff;
}
.role-default {
  background: #8c7e6e;
  color: #ffffff;
}

:global(.dark-theme) .role-admin {
  background: #3e3831;
  color: #f6f2ea;
  border: 1px solid #5c5348;
}
:global(.dark-theme) .role-ventas {
  background: #542b08;
  color: #fed7aa;
  border: 1px solid #7c3a0b;
}
:global(.dark-theme) .role-logistica {
  background: #083c5a;
  color: #bae6fd;
  border: 1px solid #0369a1;
}
:global(.dark-theme) .role-liquidador {
  background: #064e3b;
  color: #a7f3d0;
  border: 1px solid #047857;
}
:global(.dark-theme) .role-legal {
  background: #3c1a6e;
  color: #ddd6fe;
  border: 1px solid #5b21b6;
}
:global(.dark-theme) .role-finanzas {
  background: #4d2306;
  color: #fde68a;
  border: 1px solid #78350f;
}
:global(.dark-theme) .role-transporte {
  background: #1f2937;
  color: #e5e7eb;
  border: 1px solid #374151;
}
:global(.dark-theme) .role-default {
  background: #3a3630;
  color: #e5e0d8;
  border: 1px solid #4a453e;
}

/* ======================================================== */
/* 3. Columna Lateral: Ficha Credencial en Vivo */
/* ======================================================== */
.form-sidebar-column {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  position: sticky;
  top: 90px;
}

.credential-card-preview {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.credential-ribbon {
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

.credential-body {
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

.credential-avatar {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  font-weight: 800;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
  border: 3px solid var(--color-border);
}

.verified-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 22px;
  height: 22px;
  background: #059669;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  border: 2px solid var(--color-bg-surface);
}

.cred-fullname {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.15rem;
}

.cred-username {
  font-size: 0.775rem;
  font-weight: 600;
  color: var(--color-text-subtle);
}

.cred-cargo {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 0.25rem;
  font-style: italic;
}

.credential-divider {
  width: 100%;
  height: 1px;
  background: var(--color-border-subtle);
  margin: 1rem 0;
}

.credential-data-rows {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  text-align: left;
}

.cred-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.75rem;
  gap: 0.5rem;
}

.cred-k {
  color: var(--color-text-subtle);
  font-weight: 600;
}

.cred-v {
  font-weight: 600;
  color: var(--color-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cred-sunat-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

:global(.dark-theme) .cred-sunat-badge {
  background: rgba(245, 158, 11, 0.2);
  color: #fde68a;
  border-color: rgba(245, 158, 11, 0.4);
}

.credential-footer {
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

/* Botones de acción en sidebar */
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
  .role-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
