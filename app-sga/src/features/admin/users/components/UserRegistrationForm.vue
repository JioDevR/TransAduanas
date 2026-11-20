<template>
  <div class="user-registration-sga fade-in">
    <!-- 1. Barra de acciones sgaUI (Título único en Topbar) -->
    <header class="sga-form-header">
      <div class="sga-dash-meta">
        <Button
          label="Volver al directorio"
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          size="small"
          :disabled="submitting"
          @click="$emit('cancel')"
        />
        <Tag
          :value="isEditMode ? `Editando @${form.username}` : 'Nuevo Registro'"
          :severity="isEditMode ? 'warn' : 'info'"
          rounded
        />
      </div>

      <div class="sga-form-actions">
        <Button
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          :disabled="submitting"
          @click="$emit('cancel')"
        />
        <Button
          :label="isEditMode ? 'Guardar Cambios' : 'Registrar Operador'"
          icon="pi pi-check"
          size="small"
          :loading="submitting"
          @click="handleSubmit"
        />
      </div>
    </header>

    <div v-if="errorMessage" class="sga-banner is-danger">
      <i class="pi pi-exclamation-triangle"></i>
      <div class="usr-banner-body">
        <strong>Error de validación:</strong> {{ errorMessage }}
      </div>
      <Button icon="pi pi-times" text severity="danger" size="small" @click="errorMessage = ''" />
    </div>

    <!-- 2. Layout Split-Screen sgaUI -->
    <div class="sga-split-layout">
      <form class="sga-split-main" @submit.prevent="handleSubmit">
        <!-- Bloque 01: Identidad y SUNAT -->
        <section class="sga-card-surface usr-form-card">
          <div class="usr-section-head">
            <span class="sga-kicker">PASO 01 · IDENTIDAD LEGAL Y SUNAT</span>
            <h3>Identificación del Operador</h3>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label">Tipo de Documento *</label>
            <SelectButton
              v-model="form.tipoDocumento"
              :options="tiposDocumento"
              optionLabel="label"
              optionValue="id"
              :allowEmpty="false"
              size="small"
            />
          </div>

          <div class="usr-fields-2col">
            <div class="sga-form-field">
              <label class="sga-form-label" for="f-doc-num">Número de Documento *</label>
              <InputText
                id="f-doc-num"
                v-model.trim="form.numeroDocumento"
                :placeholder="docPlaceholder"
                :maxlength="docMaxLength"
                class="sga-mono"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="f-cod-sunat">Código Acreditación SUNAT</label>
              <InputText
                id="f-cod-sunat"
                v-model.trim="form.codigoSunat"
                placeholder="Ej. SUNAT-DESP-0104"
                class="sga-mono"
                size="small"
                fluid
                :disabled="submitting"
              />
              <div class="usr-prefix-row">
                <Button
                  type="button"
                  label="SUNAT-DESP-"
                  severity="secondary"
                  outlined
                  size="small"
                  @click="applySunatPrefix('SUNAT-DESP-')"
                />
                <Button
                  type="button"
                  label="AUX-"
                  severity="secondary"
                  outlined
                  size="small"
                  @click="applySunatPrefix('AUX-')"
                />
              </div>
            </div>
          </div>

          <div class="usr-fields-2col">
            <div class="sga-form-field">
              <label class="sga-form-label" for="f-nombres">Nombres *</label>
              <InputText
                id="f-nombres"
                v-model.trim="form.nombres"
                placeholder="Nombres completos"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="f-apellidos">Apellidos *</label>
              <InputText
                id="f-apellidos"
                v-model.trim="form.apellidos"
                placeholder="Apellidos completos"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>
          </div>

          <div class="usr-fields-2col">
            <div class="sga-form-field">
              <label class="sga-form-label" for="f-cargo">Cargo Institucional</label>
              <InputText
                id="f-cargo"
                v-model.trim="form.cargoTitulo"
                placeholder="Ej. Especialista Aduanero Senior"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="f-telefono">Teléfono / Celular</label>
              <InputText
                id="f-telefono"
                v-model.trim="form.telefono"
                placeholder="+51 999 888 777"
                class="sga-mono"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>
          </div>
        </section>

        <!-- Bloque 02: Credenciales -->
        <section class="sga-card-surface usr-form-card">
          <div class="usr-section-head">
            <span class="sga-kicker">PASO 02 · CREDENCIALES DE ACCESO</span>
            <h3>Cuenta y Correo Institucional</h3>
          </div>

          <div class="usr-fields-2col">
            <div class="sga-form-field">
              <label class="sga-form-label" for="f-username">Nombre de Usuario *</label>
              <div class="usr-inline-input">
                <InputText
                  id="f-username"
                  v-model.trim="form.username"
                  placeholder="ej. calvarez"
                  class="sga-mono"
                  size="small"
                  fluid
                  :disabled="isEditMode || submitting"
                  @input="isAutoUsername = false"
                />
                <Button
                  v-if="!isEditMode && usernameSuggestions.length > 0"
                  type="button"
                  icon="pi pi-refresh"
                  severity="secondary"
                  outlined
                  size="small"
                  title="Alternar sugerencia"
                  @click="regenerateUsername"
                />
              </div>
              <div v-if="!isEditMode && usernameSuggestions.length > 0" class="usr-prefix-row">
                <Button
                  v-for="sug in usernameSuggestions.slice(0, 4)"
                  :key="sug"
                  type="button"
                  :label="sug"
                  :severity="form.username === sug ? 'primary' : 'secondary'"
                  :outlined="form.username !== sug"
                  size="small"
                  @click="selectUsernameSuggestion(sug)"
                />
              </div>
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="f-correo">Correo Institucional *</label>
              <InputText
                id="f-correo"
                v-model.trim="form.correoInstitucional"
                type="email"
                placeholder="nombre@aduanas-sga.pe"
                class="sga-mono"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="f-password">
              {{ isEditMode ? 'Nueva Contraseña (opcional)' : 'Contraseña Inicial *' }}
            </label>
            <Password
              inputId="f-password"
              v-model="form.password"
              toggleMask
              :feedback="false"
              :placeholder="isEditMode ? 'Dejar vacío para mantener la actual' : 'Mínimo 6 caracteres'"
              autocomplete="new-password"
              size="small"
              fluid
              :disabled="submitting"
            />
          </div>
        </section>

        <!-- Bloque 03: Rol y Sede -->
        <section class="sga-card-surface usr-form-card">
          <div class="usr-section-head">
            <span class="sga-kicker">PASO 03 · PERFIL Y SEDE</span>
            <h3>Rol Operativo y Sede Territorial</h3>
          </div>

          <div class="usr-fields-2col">
            <div class="sga-form-field">
              <label class="sga-form-label" for="f-rol">Rol del Sistema *</label>
              <Select
                inputId="f-rol"
                v-model="form.rol"
                :options="roleOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="Seleccione un rol..."
                size="small"
                fluid
                :disabled="submitting"
              />
              <small v-if="form.rol" class="sga-text-muted">{{ getRoleDescription(form.rol) }}</small>
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="f-sede">Sede Territorial Asignada *</label>
              <Select
                inputId="f-sede"
                v-model="form.idSede"
                :options="sedeOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="Seleccione la sede..."
                size="small"
                fluid
                :disabled="submitting"
              />
              <small v-if="selectedSedeDetails" class="sga-text-muted">
                {{ selectedSedeDetails.direccion }}
              </small>
            </div>
          </div>
        </section>
      </form>

      <!-- Columna Lateral Sticky: Resumen Ficha -->
      <aside class="sga-split-aside">
        <div class="sga-card-surface usr-aside-card">
          <div class="usr-aside-head">
            <span class="sga-kicker">RESUMEN EN VIVO</span>
            <Tag
              :value="isFormReady ? 'Ficha Lista' : 'Incompleta'"
              :severity="isFormReady ? 'success' : 'warn'"
              rounded
            />
          </div>

          <div class="usr-preview-identity">
            <div class="usr-preview-avatar">{{ previewInitials }}</div>
            <div>
              <strong>{{ form.nombres || form.apellidos ? `${form.nombres} ${form.apellidos}` : 'Nombre del Operador' }}</strong>
              <div class="sga-mono sga-text-muted">@{{ form.username || 'usuario' }}</div>
              <small class="sga-text-muted">{{ form.cargoTitulo || 'Sin cargo asignado' }}</small>
            </div>
          </div>

          <div class="sga-card-meta-grid">
            <div class="sga-meta-item">
              <span class="sga-meta-label">DOCUMENTO</span>
              <span class="sga-meta-val sga-mono">{{ form.tipoDocumento }}: {{ form.numeroDocumento || '—' }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">SUNAT</span>
              <span class="sga-meta-val sga-mono">{{ form.codigoSunat || 'Sin código' }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">ROL</span>
              <span class="sga-meta-val">{{ ROLE_LABELS[form.rol] || form.rol || '—' }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">SEDE</span>
              <span class="sga-meta-val sga-mono">{{ selectedSedeDetails?.codigoSede || '—' }}</span>
            </div>
            <div class="sga-meta-item full-span">
              <span class="sga-meta-label">CORREO</span>
              <span class="sga-meta-val sga-mono">{{ form.correoInstitucional || '—' }}</span>
            </div>
          </div>

          <ul class="usr-checklist">
            <li :class="{ ok: !!form.numeroDocumento && !!form.tipoDocumento }">
              <i :class="form.numeroDocumento ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Documento de identidad</span>
            </li>
            <li :class="{ ok: !!form.nombres && !!form.apellidos }">
              <i :class="form.nombres && form.apellidos ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Nombres y apellidos</span>
            </li>
            <li :class="{ ok: !!form.username }">
              <i :class="form.username ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Usuario único</span>
            </li>
            <li :class="{ ok: isEditMode || (!!form.password && form.password.length >= 6) }">
              <i :class="isEditMode || (form.password && form.password.length >= 6) ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Contraseña válida (mín. 6)</span>
            </li>
            <li :class="{ ok: !!form.rol && !!form.idSede }">
              <i :class="form.rol && form.idSede ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Rol y sede asignados</span>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Select from 'primevue/select'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'
import { ROLES, ROLE_LABELS } from '@/core/auth/roles'

const props = defineProps({
  userToEdit: { type: Object, default: null },
  roles: { type: Array, default: () => [] },
  sedes: { type: Array, default: () => [] }
})

const emit = defineEmits(['cancel', 'saved'])

const submitting = ref(false)
const errorMessage = ref('')
const isEditMode = computed(() => !!props.userToEdit)

const tiposDocumento = [
  { id: 'DNI', label: 'DNI' },
  { id: 'CE', label: 'CE' },
  { id: 'PASAPORTE', label: 'Pasaporte' },
  { id: 'RUC', label: 'RUC' }
]

const form = reactive({
  username: '',
  password: '',
  nombres: '',
  apellidos: '',
  correoInstitucional: '',
  rol: '',
  idSede: '',
  tipoDocumento: 'DNI',
  numeroDocumento: '',
  telefono: '',
  cargoTitulo: '',
  codigoSunat: ''
})

const roleOptions = computed(() =>
  (props.roles || []).map((r) => {
    const code = r.nombreRol || r.rol
    return { label: ROLE_LABELS[code] || code, value: code }
  })
)

const sedeOptions = computed(() =>
  (props.sedes || []).map((s) => ({
    label: `${s.codigoSede} — ${s.nombre} (${s.distrito || ''})`,
    value: s.idSede
  }))
)

const docPlaceholder = computed(() => {
  if (form.tipoDocumento === 'DNI') return '8 dígitos'
  if (form.tipoDocumento === 'CE') return 'Hasta 12 dígitos'
  if (form.tipoDocumento === 'RUC') return '11 dígitos'
  return 'Número de documento'
})

const docMaxLength = computed(() => {
  if (form.tipoDocumento === 'DNI') return 8
  if (form.tipoDocumento === 'RUC') return 11
  return 20
})

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
      form.rol = user.rol || user.nombreRol || ''
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
      form.rol = props.roles.length > 0 ? (props.roles[0].nombreRol || props.roles[0].rol) : ''
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

watch(() => props.roles, (newRoles) => {
  if (!props.userToEdit && newRoles.length > 0 && !form.rol) {
    form.rol = newRoles[0].nombreRol || newRoles[0].rol
  }
})

watch(() => props.sedes, (newSedes) => {
  if (!props.userToEdit && newSedes.length > 0 && !form.idSede) {
    form.idSede = newSedes[0].idSede
  }
})

const sanitizeForUsername = (str) =>
  (str || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9]/g, '').toLowerCase()

const usernameSuggestions = computed(() => {
  if (!form.nombres || !form.apellidos) return []
  const firstNames = form.nombres.trim().split(/\s+/).map(sanitizeForUsername).filter(Boolean)
  const lastNames = form.apellidos.trim().split(/\s+/).map(sanitizeForUsername).filter(Boolean)
  if (!firstNames.length || !lastNames.length) return []
  const n1 = firstNames[0]
  const n2 = firstNames[1] || ''
  const a1 = lastNames[0]
  const a2 = lastNames[1] || ''
  const combos = [`${n1.charAt(0)}${a1}`, `${n1.charAt(0)}.${a1}`, `${n1}.${a1.charAt(0)}`]
  if (n2) combos.push(`${n1.charAt(0)}${n2.charAt(0)}${a1}`)
  if (a2) combos.push(`${n1.charAt(0)}${a1}${a2.charAt(0)}`)
  return [...new Set(combos)]
})

watch([() => form.nombres, () => form.apellidos], () => {
  if (!isEditMode.value && isAutoUsername.value && usernameSuggestions.value.length > 0) {
    const idx = selectedSuggestionIndex.value % usernameSuggestions.value.length
    form.username = usernameSuggestions.value[idx]
    if (!form.correoInstitucional || form.correoInstitucional.endsWith('@aduanas-sga.pe')) {
      form.correoInstitucional = `${form.username}@aduanas-sga.pe`
    }
  }
})

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

const selectedSedeDetails = computed(() =>
  form.idSede ? props.sedes.find((s) => Number(s.idSede) === Number(form.idSede)) : null
)

const previewInitials = computed(() => {
  const init = ((form.nombres?.charAt(0) || '') + (form.apellidos?.charAt(0) || '')).toUpperCase()
  return init || form.username?.substring(0, 2).toUpperCase() || 'OP'
})

const isFormReady = computed(() =>
  Boolean(
    form.numeroDocumento &&
      form.tipoDocumento &&
      form.nombres &&
      form.apellidos &&
      form.username &&
      (isEditMode.value || (form.password && form.password.length >= 6)) &&
      form.correoInstitucional?.includes('@') &&
      form.rol &&
      form.idSede
  )
)

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
  if (!form.numeroDocumento) return (errorMessage.value = 'El número de documento de identidad es obligatorio.')
  if (!form.nombres || !form.apellidos) return (errorMessage.value = 'Los nombres y apellidos son obligatorios.')
  if (!form.username) return (errorMessage.value = 'El nombre de usuario es obligatorio.')
  if (!isEditMode.value && (!form.password || form.password.length < 6)) {
    return (errorMessage.value = 'La contraseña debe tener al menos 6 caracteres.')
  }
  if (!form.correoInstitucional || !form.correoInstitucional.includes('@')) {
    return (errorMessage.value = 'Ingrese un correo institucional válido.')
  }
  if (!form.rol) return (errorMessage.value = 'Debe seleccionar un rol del sistema.')
  if (!form.idSede) return (errorMessage.value = 'Debe asignar una sede operativa territorial.')

  submitting.value = true
  const payload = {
    rol: form.rol,
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
    payload,
    done: () => {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.user-registration-sga {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.usr-header-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
}

.usr-banner-body {
  flex: 1;
}

.usr-form-card,
.usr-aside-card {
  padding: var(--sga-card-padding-y, 1.75rem) var(--sga-card-padding-x, 2rem);
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.usr-section-head h3 {
  margin: 0.15rem 0 0;
  font-size: 1.05rem;
  font-weight: 600;
}

.usr-fields-2col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.usr-inline-input {
  display: flex;
  gap: 0.4rem;
  align-items: center;
}

.usr-prefix-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-top: 0.25rem;
}

.usr-aside-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.usr-preview-identity {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.usr-preview-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-primary-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.usr-checklist {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

.usr-checklist li {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.usr-checklist li.ok {
  color: var(--color-success-text);
}
</style>
