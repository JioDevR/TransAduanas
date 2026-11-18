<template>
  <Dialog
    :visible="visible"
    modal
    header="Restablecer Contraseña de Acceso B2B"
    :style="{ width: '480px', maxWidth: '95vw' }"
    class="portal-reset-dialog"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="user" class="reset-modal-body">
      <div class="reset-target-info">
        <i class="pi pi-key key-icon"></i>
        <div>
          <h4 class="target-title">Restableciendo credenciales para:</h4>
          <span class="target-username font-mono">@{{ user.username }} ({{ user.nombres }} {{ user.apellidos }})</span>
          <div class="target-empresa font-mono text-muted">Empresa: {{ user.razonSocialCliente }}</div>
        </div>
      </div>

      <div class="form-field-group">
        <label class="field-label" for="new-password">Nueva Contraseña Temporal *</label>
        <Password
          inputId="new-password"
          v-model="password"
          toggleMask
          :feedback="false"
          placeholder="Ingrese mínimo 6 caracteres..."
          autocomplete="new-password"
          :disabled="submitting"
          inputClass="font-mono"
          fluid
        />

        <Button
          type="button"
          label="Generar contraseña segura automática"
          icon="pi pi-bolt"
          severity="secondary"
          outlined
          size="small"
          @click="generateRandomPassword"
          :disabled="submitting"
        />
      </div>

      <div class="checkbox-field-group">
        <label class="custom-checkbox-label" for="force-change">
          <Checkbox
            inputId="force-change"
            v-model="forceChange"
            binary
            :disabled="submitting"
          />
          <span>Exigir cambio obligatorio de contraseña en el próximo ingreso al portal</span>
        </label>
      </div>

      <Message v-if="errorMessage" severity="error" :closable="false">
        {{ errorMessage }}
      </Message>
    </div>

    <template #footer>
      <div class="dialog-footer-actions">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          @click="$emit('update:visible', false)"
          :disabled="submitting"
        />
        <Button
          type="button"
          label="Aplicar Nueva Contraseña"
          icon="pi pi-check"
          :loading="submitting"
          @click="handleSave"
          :disabled="!password || password.length < 6"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Password from 'primevue/password'
import Checkbox from 'primevue/checkbox'
import Message from 'primevue/message'
import { portalUserService } from '../services/portalUserService'
import { useNotification } from '@/shared/composables/useNotification'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  user: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'saved'])
const { showSuccess, showError } = useNotification()

const password = ref('')
const showPassword = ref(false)
const forceChange = ref(true)
const submitting = ref(false)
const errorMessage = ref('')

watch(
  () => props.visible,
  (val) => {
    if (val) {
      password.value = ''
      showPassword.value = false
      forceChange.value = true
      errorMessage.value = ''
    }
  }
)

const generateRandomPassword = () => {
  const chars = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789!@#$%&*'
  let result = ''
  for (let i = 0; i < 10; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  password.value = result
  showPassword.value = true
}

const handleSave = async () => {
  if (!password.value || password.value.trim().length < 6) {
    errorMessage.value = 'La contraseña debe contener al menos 6 caracteres'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await portalUserService.resetPassword(props.user.idUsuarioCliente, {
      nuevaPassword: password.value.trim(),
      forzarCambioPrimerInicio: forceChange.value
    })
    showSuccess(`Contraseña actualizada para @${props.user.username}`)
    emit('update:visible', false)
    emit('saved')
  } catch (err) {
    const msg = err.response?.data?.message || 'Error al restablecer contraseña'
    errorMessage.value = msg
    showError(msg)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.reset-modal-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 0.5rem 0;
}

.reset-target-info {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  padding: 0.85rem 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
}

.key-icon {
  font-size: 1.5rem;
  color: var(--color-accent-kicker);
  margin-top: 0.2rem;
}

.target-title {
  margin: 0;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  font-weight: 600;
  text-transform: uppercase;
}

.target-username {
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.target-empresa {
  font-size: var(--font-size-xs, 0.75rem);
  margin-top: 0.2rem;
  color: var(--color-text-subtle);
}

.form-field-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field-label {
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  color: var(--color-text-main);
}

.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.custom-input {
  width: 100%;
  padding: 0.65rem 2.5rem 0.65rem 0.85rem;
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main);
  transition: border-color var(--transition-fast, 150ms);
}

.custom-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(216, 185, 125, 0.2);
}

.toggle-visibility-btn {
  position: absolute;
  right: 0.75rem;
  background: none;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toggle-visibility-btn:hover {
  color: var(--color-text-main);
}

.btn-generate-password {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: none;
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-sm, 6px);
  padding: 0.4rem 0.75rem;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-accent-kicker);
  cursor: pointer;
  align-self: flex-start;
  transition: all var(--transition-fast, 150ms);
}

.btn-generate-password:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-primary);
}

.checkbox-field-group {
  padding: 0.25rem 0;
}

.custom-checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main);
  cursor: pointer;
}

.custom-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--color-primary);
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-danger-text);
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm, 6px);
}

.dialog-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.5rem;
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.5rem 0.95rem;
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.5rem 0.95rem;
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}
</style>
