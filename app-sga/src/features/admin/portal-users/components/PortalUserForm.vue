<template>
  <div class="portal-user-form-sga fade-in">
    <header class="sga-form-header">
      <div class="sga-dash-meta">
        <Button
          label="Volver a Usuarios B2B"
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          size="small"
          :disabled="submitting"
          @click="$emit('cancel')"
        />
        <Tag
          :value="isEditMode ? `Editando @${form.username}` : 'Nueva Cuenta B2B'"
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
          :label="isEditMode ? 'Guardar Cambios' : 'Crear Cuenta B2B'"
          icon="pi pi-check"
          size="small"
          :loading="submitting"
          @click="handleSubmit"
        />
      </div>
    </header>

    <div v-if="errorMessage" class="sga-banner is-danger">
      <i class="pi pi-exclamation-triangle"></i>
      <div class="ptl-banner-body">{{ errorMessage }}</div>
      <Button icon="pi pi-times" text severity="danger" size="small" @click="errorMessage = ''" />
    </div>

    <div class="sga-split-layout">
      <form class="sga-split-main" @submit.prevent="handleSubmit">
        <section class="sga-card-surface ptl-form-card">
          <div>
            <span class="sga-kicker">PASO 01 · EMPRESA Y CREDENCIALES</span>
            <h3>Vinculación Corporativa</h3>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="ptl-cliente">Empresa Cliente *</label>
            <Select
              inputId="ptl-cliente"
              v-model="form.idCliente"
              :options="clienteOptions"
              optionLabel="label"
              optionValue="value"
              filter
              placeholder="Seleccione empresa cliente..."
              size="small"
              fluid
              :disabled="isEditMode || submitting || loadingClientes"
            />
          </div>

          <div class="ptl-fields-2col">
            <div class="sga-form-field">
              <label class="sga-form-label" for="ptl-username">Usuario de Acceso *</label>
              <InputText
                id="ptl-username"
                v-model.trim="form.username"
                placeholder="ej. 20370146994"
                class="sga-mono"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>

            <div v-if="!isEditMode" class="sga-form-field">
              <label class="sga-form-label" for="ptl-password">Contraseña Temporal *</label>
              <div class="ptl-inline-pass">
                <Password
                  inputId="ptl-password"
                  v-model="form.password"
                  toggleMask
                  :feedback="false"
                  placeholder="Mínimo 6 caracteres"
                  autocomplete="new-password"
                  size="small"
                  fluid
                  :disabled="submitting"
                />
                <Button
                  type="button"
                  icon="pi pi-bolt"
                  severity="secondary"
                  outlined
                  size="small"
                  title="Generar contraseña"
                  @click="generateRandomPassword"
                />
              </div>
            </div>
          </div>

          <div class="ptl-checks-row">
            <label class="ptl-check-label" for="ptl-force-pass">
              <Checkbox inputId="ptl-force-pass" v-model="form.debeCambiarPassword" binary :disabled="submitting" />
              <span>Exigir cambio de contraseña en próximo inicio de sesión</span>
            </label>

            <label v-if="isEditMode" class="ptl-check-label" for="ptl-active">
              <Checkbox inputId="ptl-active" v-model="form.estadoActivo" binary :disabled="submitting" />
              <span>Cuenta activa en el Portal</span>
            </label>
          </div>
        </section>

        <section class="sga-card-surface ptl-form-card">
          <div>
            <span class="sga-kicker">PASO 02 · REPRESENTANTE Y NOTIFICACIONES</span>
            <h3>Datos de Contacto</h3>
          </div>

          <div class="sga-form-field">
            <label class="sga-form-label" for="ptl-rep">Representante / Contacto *</label>
            <InputText
              id="ptl-rep"
              v-model.trim="form.contactoRepresentante"
              placeholder="Nombre completo del representante"
              size="small"
              fluid
              :disabled="submitting"
            />
          </div>

          <div class="ptl-fields-2col">
            <div class="sga-form-field">
              <label class="sga-form-label" for="ptl-email">Correo Electrónico *</label>
              <InputText
                id="ptl-email"
                v-model.trim="form.correoElectronico"
                type="email"
                placeholder="operaciones@empresa.com"
                class="sga-mono"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>

            <div class="sga-form-field">
              <label class="sga-form-label" for="ptl-tel">Teléfono de Contacto</label>
              <InputText
                id="ptl-tel"
                v-model.trim="form.telefonoContacto"
                placeholder="+51 987 654 321"
                class="sga-mono"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>
          </div>
        </section>
      </form>

      <aside class="sga-split-aside">
        <div class="sga-card-surface ptl-form-card">
          <div class="ptl-aside-head">
            <span class="sga-kicker">VISTA PREVIA B2B</span>
            <Tag
              :value="form.estadoActivo ? 'Activa' : 'Suspendida'"
              :severity="form.estadoActivo ? 'success' : 'danger'"
              rounded
            />
          </div>

          <div class="ptl-preview-identity">
            <div class="ptl-preview-avatar">{{ previewInitials }}</div>
            <div>
              <strong>{{ form.contactoRepresentante || 'Representante Legal' }}</strong>
              <div class="sga-mono sga-text-muted">@{{ form.username || 'usuario' }}</div>
            </div>
          </div>

          <div class="sga-card-meta-grid">
            <div class="sga-meta-item full-span">
              <span class="sga-meta-label">EMPRESA VINCULADA</span>
              <span class="sga-meta-val">{{ previewClienteName }}</span>
            </div>
            <div class="sga-meta-item full-span">
              <span class="sga-meta-label">CORREO</span>
              <span class="sga-meta-val sga-mono">{{ form.correoElectronico || '—' }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">TELÉFONO</span>
              <span class="sga-meta-val sga-mono">{{ form.telefonoContacto || '—' }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">PRIMER LOGIN</span>
              <span class="sga-meta-val">{{ form.debeCambiarPassword ? 'Cambiar clave' : 'Directo' }}</span>
            </div>
          </div>

          <div class="sga-banner is-info">
            <i class="pi pi-shield"></i>
            <span>Autenticación aislada exclusiva para el Portal B2B.</span>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { portalUserService } from '../services/portalUserService'
import { useNotification } from '@/shared/composables/useNotification'

const props = defineProps({
  userToEdit: { type: Object, default: null }
})

const emit = defineEmits(['cancel', 'saved'])
const { showSuccess, showError } = useNotification()

const isEditMode = computed(() => !!props.userToEdit)
const clientes = ref([])
const loadingClientes = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  idCliente: '',
  username: '',
  password: '',
  contactoRepresentante: '',
  correoElectronico: '',
  telefonoContacto: '',
  estadoActivo: true,
  debeCambiarPassword: true
})

const clienteOptions = computed(() =>
  (clientes.value || []).map((c) => ({
    label: `${c.razonSocial} (${c.numeroDocumento})`,
    value: c.idCliente
  }))
)

onMounted(async () => {
  await loadClientes()
  if (isEditMode.value) {
    populateEditData(props.userToEdit)
  }
})

watch(
  () => props.userToEdit,
  (val) => {
    if (val) populateEditData(val)
  }
)

const loadClientes = async () => {
  loadingClientes.value = true
  try {
    clientes.value = await portalUserService.getClientesActivos()
  } catch (err) {
    showError('No se pudo cargar la lista de empresas clientes')
  } finally {
    loadingClientes.value = false
  }
}

watch(
  () => form.idCliente,
  (newId) => {
    if (!isEditMode.value && newId) {
      const cli = clientes.value.find((c) => c.idCliente === Number(newId))
      if (cli) {
        if (!form.username) form.username = cli.numeroDocumento || ''
        if (!form.contactoRepresentante) form.contactoRepresentante = cli.contactoRepresentante || ''
        if (!form.correoElectronico) form.correoElectronico = cli.correoContacto || ''
        if (!form.telefonoContacto) form.telefonoContacto = cli.telefonoContacto || ''
      }
    }
  }
)

const populateEditData = (u) => {
  form.idCliente = u.idCliente
  form.username = u.username
  form.contactoRepresentante = u.contactoRepresentante || u.nombres || ''
  form.correoElectronico = u.correoElectronico || ''
  form.telefonoContacto = u.telefonoContacto || ''
  form.estadoActivo = u.estadoActivo
  form.debeCambiarPassword = u.debeCambiarPassword
}

const selectedClienteObj = computed(() =>
  form.idCliente ? clientes.value.find((c) => c.idCliente === Number(form.idCliente)) || null : null
)

const previewClienteName = computed(() => {
  if (isEditMode.value) return props.userToEdit?.razonSocialCliente || '—'
  return selectedClienteObj.value?.razonSocial || 'Seleccione una empresa'
})

const previewInitials = computed(() => {
  const parts = (form.contactoRepresentante || '').trim().split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  if (parts.length === 1 && parts[0]) return parts[0].substring(0, 2).toUpperCase()
  return 'B2B'
})

const generateRandomPassword = () => {
  const chars = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789!@#$%&*'
  let result = ''
  for (let i = 0; i < 10; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  form.password = result
}

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!isEditMode.value && !form.idCliente) return (errorMessage.value = 'Debe seleccionar una empresa cliente vinculada')
  if (!form.username || form.username.trim().length < 3) {
    return (errorMessage.value = 'El nombre de usuario debe contener al menos 3 caracteres')
  }
  if (!isEditMode.value && (!form.password || form.password.trim().length < 6)) {
    return (errorMessage.value = 'La contraseña temporal debe tener al menos 6 caracteres')
  }
  if (!form.contactoRepresentante || !form.correoElectronico) {
    return (errorMessage.value = 'Por favor complete todos los campos obligatorios (*)')
  }

  submitting.value = true
  try {
    if (isEditMode.value) {
      await portalUserService.updatePortalUser(props.userToEdit.idUsuarioCliente, {
        username: form.username.trim().toLowerCase(),
        contactoRepresentante: form.contactoRepresentante.trim(),
        correoElectronico: form.correoElectronico.trim().toLowerCase(),
        telefonoContacto: form.telefonoContacto ? form.telefonoContacto.trim() : null,
        estadoActivo: form.estadoActivo,
        debeCambiarPassword: form.debeCambiarPassword
      })
      showSuccess(`Usuario @${form.username} actualizado exitosamente`)
    } else {
      await portalUserService.createPortalUser({
        idCliente: Number(form.idCliente),
        username: form.username.trim().toLowerCase(),
        password: form.password.trim(),
        contactoRepresentante: form.contactoRepresentante.trim(),
        correoElectronico: form.correoElectronico.trim().toLowerCase(),
        telefonoContacto: form.telefonoContacto ? form.telefonoContacto.trim() : null,
        debeCambiarPassword: form.debeCambiarPassword
      })
      showSuccess(`Usuario @${form.username} aprovisionado correctamente`)
    }
    emit('saved')
  } catch (err) {
    const msg = err.response?.data?.message || 'Error al guardar el usuario en el portal'
    errorMessage.value = msg
    showError(msg)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.portal-user-form-sga {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.ptl-header-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
}

.ptl-banner-body {
  flex: 1;
}

.ptl-form-card {
  padding: var(--sga-card-padding-y, 1.75rem) var(--sga-card-padding-x, 2rem);
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.ptl-form-card h3 {
  margin: 0.15rem 0 0;
  font-size: 1.05rem;
  font-weight: 600;
}

.ptl-fields-2col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.ptl-inline-pass {
  display: flex;
  gap: 0.4rem;
  align-items: center;
}

.ptl-checks-row {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.ptl-check-label {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.ptl-aside-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ptl-preview-identity {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.ptl-preview-avatar {
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
</style>
