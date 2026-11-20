<template>
  <Dialog
    :visible="visible"
    modal
    header="Restablecer Contraseña de Acceso B2B"
    :style="{ width: '480px', maxWidth: '95vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="user" class="sga-dialog-body">
      <div class="sga-banner is-info">
        <i class="pi pi-key"></i>
        <div>
          <strong class="sga-mono">@{{ user.username }}</strong>
          <div>{{ user.contactoRepresentante || user.nombres }} · {{ user.razonSocialCliente }}</div>
        </div>
      </div>

      <div class="sga-form-field">
        <label class="sga-form-label" for="new-password">Nueva Contraseña Temporal *</label>
        <Password
          inputId="new-password"
          v-model="password"
          toggleMask
          :feedback="false"
          placeholder="Ingrese mínimo 6 caracteres..."
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

      <label class="ptl-check-label" for="force-change">
        <Checkbox inputId="force-change" v-model="forceChange" binary :disabled="submitting" />
        <span>Exigir cambio obligatorio de contraseña en el próximo ingreso al portal</span>
      </label>

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
          label="Aplicar Nueva Contraseña"
          icon="pi pi-check"
          size="small"
          :loading="submitting"
          :disabled="!password || password.length < 6"
          @click="handleSave"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import Message from 'primevue/message'
import Password from 'primevue/password'
import { portalUserService } from '../services/portalUserService'
import { useNotification } from '@/shared/composables/useNotification'

const props = defineProps({
  visible: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

const emit = defineEmits(['update:visible', 'saved'])
const { showSuccess, showError } = useNotification()

const password = ref('')
const forceChange = ref(true)
const submitting = ref(false)
const errorMessage = ref('')

watch(
  () => props.visible,
  (val) => {
    if (val) {
      password.value = ''
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
.ptl-check-label {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  font-size: 0.85rem;
  cursor: pointer;
}
</style>
