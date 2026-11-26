<template>
  <div class="portal-user-management-view fade-in">
    <PortalUserForm
      v-if="navigationStore.administracionTiSubOption === 'create-portal-user' || isEditing"
      :user-to-edit="selectedUser"
      @cancel="handleFormCancel"
      @saved="handleUserSaved"
    />

    <PortalUserTable
      v-else
      :users="users"
      :loading="loading"
      @edit="openEdit"
      @audit="openAudit"
      @reset-password="openResetPassword"
      @toggle-status="handleToggleStatus"
      @refresh="loadData"
      @create="openCreateView"
    />

    <PortalUserAuditModal v-model:visible="showAuditModal" :user="userToAudit" />

    <PortalUserResetPasswordModal
      v-model:visible="showResetPasswordModal"
      :user="userToReset"
      @saved="loadData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { portalUserService } from '../services/portalUserService'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import PortalUserTable from '../components/PortalUserTable.vue'
import PortalUserForm from '../components/PortalUserForm.vue'
import PortalUserAuditModal from '../components/PortalUserAuditModal.vue'
import PortalUserResetPasswordModal from '../components/PortalUserResetPasswordModal.vue'

const navigationStore = useNavigationStore()
const { showSuccess, showError } = useNotification()
const { confirmAction } = useConfirmation()

const users = ref([])
const loading = ref(false)
const isEditing = ref(false)
const selectedUser = ref(null)

const showAuditModal = ref(false)
const userToAudit = ref(null)
const showResetPasswordModal = ref(false)
const userToReset = ref(null)

onMounted(() => {
  loadData()
})

watch(
  () => navigationStore.administracionTiSubOption,
  (val) => {
    if (val === 'portal-users') {
      isEditing.value = false
      selectedUser.value = null
      loadData()
    }
  }
)

const loadData = async () => {
  loading.value = true
  try {
    users.value = await portalUserService.getPortalUsers()
  } catch (err) {
    showError('Error al recuperar los usuarios del portal de clientes')
  } finally {
    loading.value = false
  }
}

const openCreateView = () => {
  selectedUser.value = null
  isEditing.value = false
  navigationStore.setAdministracionTiSubOption('create-portal-user')
}

const openEdit = (user) => {
  selectedUser.value = { ...user }
  isEditing.value = true
}

const handleFormCancel = () => {
  isEditing.value = false
  selectedUser.value = null
  navigationStore.setAdministracionTiSubOption('portal-users')
}

const handleUserSaved = () => {
  isEditing.value = false
  selectedUser.value = null
  navigationStore.setAdministracionTiSubOption('portal-users')
  loadData()
}

const openAudit = (user) => {
  userToAudit.value = user
  showAuditModal.value = true
}

const openResetPassword = (user) => {
  userToReset.value = user
  showResetPasswordModal.value = true
}

const handleToggleStatus = async (user) => {
  const isActivating = !user.estadoActivo
  const confirmed = await confirmAction({
    header: isActivating ? 'Reactivar Cuenta del Portal' : 'Desactivar Cuenta del Portal',
    message: `¿Está seguro de que desea ${isActivating ? 'reactivar' : 'desactivar (baja lógica)'} la cuenta de @${user.username}?`,
    acceptLabel: isActivating ? 'Sí, Reactivar' : 'Sí, Desactivar',
    acceptClass: isActivating ? 'p-button-success' : 'p-button-danger'
  })
  if (!confirmed) return

  try {
    if (isActivating) {
      await portalUserService.reactivatePortalUser(user.idUsuarioCliente)
      showSuccess(`Usuario @${user.username} reactivado exitosamente`)
    } else {
      await portalUserService.deletePortalUser(user.idUsuarioCliente)
      showSuccess(`Usuario @${user.username} desactivado (baja lógica por estado)`)
    }
    await loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al modificar el estado del usuario')
  }
}
</script>

<style scoped>
.portal-user-management-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
</style>
