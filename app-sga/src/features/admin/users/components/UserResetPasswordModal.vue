<template>
  <Dialog
    :visible="visible"
    modal
    header="Restablecer Contraseña de Personal Interno"
    :style="{ width: '480px', maxWidth: '95vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="user" class="sga-dialog-body">
      <div class="sga-banner is-info">
        <i class="pi pi-key"></i>
        <div>
          <strong>{{ user.nombres }} {{ user.apellidos }}</strong>
          <div class="sga-mono">@{{ user.username }} · {{ user.nombreRol }}</div>
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="internal-new-password">Nueva Contraseña Temporal *</label>
        <Password
          inputId="internal-new-password"
          v-model="password"
          toggleMask
          :feedback="false"
          placeholder="Mínimo 6 caracteres..."
          autocomplete="new-password"
          :disabled="submitting"
          inputClass="sga-mono"
          size="small"
          fluid
        />
        <div>
          <Button
            type="button"
            label="Generar contraseña segura"
            icon="pi pi-bolt"
            severity="secondary"
            outlined
            size="small"
            :disabled="submitting"
            @click="generateRandomPassword"
          />
        </div>
      </div>

      <Message v-if="errorMessage" severity="error" :closable="false">
        {{ errorMessage }}
      </Message>
    </div>

    <template #footer>
      <div class="sga-dialog-footer">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          :disabled="submitting"
          @click="$emit('update:visible', false)"
        />
        <Button
          type="button"
          label="Aplicar Contraseña"
          icon="pi pi-check"
          size="small"
          :loading="submitting"
          :disabled="!password || password.trim().length < 6"
          @click="handleSave"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Message from 'primevue/message'
import Password from 'primevue/password'
import { userService } from '../services/userService'
import { useNotification } from '@/shared/composables/useNotification'

const props = defineProps({
  visible: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

const emit = defineEmits(['update:visible', 'saved'])
const { showSuccess, showError } = useNotification()

const password = ref('')
const submitting = ref(false)
const errorMessage = ref('')

watch(
  () => props.visible,
  (val) => {
    if (val) {
      password.value = ''
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
}

const handleSave = async () => {
  if (!password.value || password.value.trim().length < 6) {
    errorMessage.value = 'La contraseña debe contener al menos 6 caracteres.'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    await userService.resetPassword(props.user.idUsuario, password.value.trim())
    showSuccess(`Contraseña restablecida para @${props.user.username}`)
    emit('update:visible', false)
    emit('saved')
  } catch (err) {
    const msg = err.response?.data?.message || 'No se pudo restablecer la contraseña.'
    errorMessage.value = msg
    showError(msg)
  } finally {
    submitting.value = false
  }
}
</script>
