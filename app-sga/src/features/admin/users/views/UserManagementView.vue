<template>
  <div class="user-management-view fade-in">
    <UserRegistrationForm
      v-if="navigationStore.adminSubOption === 'create-user'"
      :user-to-edit="selectedUser"
      :roles="roles"
      :sedes="sedes"
      @cancel="navigationStore.setAdministracionTiSubOption('users')"
      @saved="handleUserSaved"
    />

    <!-- Tabla Dual SgaListShell -->
    <UserTable
      v-else
      :users="users"
      :roles="roles"
      :loading="loading"
      @edit="openEditDialog"
      @toggle-status="handleToggleStatus"
      @reset-password="openResetPassword"
      @refresh="loadData"
      @create="openCreateDialog"
    />

    <UserResetPasswordModal
      v-model:visible="showResetModal"
      :user="userToReset"
      @saved="loadData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userService } from '../services/userService'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import UserTable from '../components/UserTable.vue'
import UserRegistrationForm from '../components/UserRegistrationForm.vue'
import UserResetPasswordModal from '../components/UserResetPasswordModal.vue'

const navigationStore = useNavigationStore()
const { showSuccess, showError } = useNotification()
const { confirmAction } = useConfirmation()

const users = ref([])
const roles = ref([])
const sedes = ref([])
const loading = ref(false)

const selectedUser = ref(null)
const showResetModal = ref(false)
const userToReset = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const [usersData, rolesData, sedesData] = await Promise.all([
      userService.getUsers(false),
      userService.getRoles(),
      userService.getSedes()
    ])
    users.value = usersData || []
    roles.value = rolesData || []
    sedes.value = sedesData || []
  } catch (err) {
    showError(err.response?.data?.message || 'Error al conectar con la base de datos de SGA.')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  selectedUser.value = null
  navigationStore.setAdministracionTiSubOption('create-user')
}

const openEditDialog = (user) => {
  selectedUser.value = { ...user }
  navigationStore.setAdministracionTiSubOption('create-user')
}

const openResetPassword = (user) => {
  userToReset.value = user
  showResetModal.value = true
}

const handleUserSaved = async ({ isEdit, userId, payload, done }) => {
  try {
    if (isEdit) {
      await userService.updateUser(userId, payload)
      showSuccess('Ficha del operador y acreditaciones actualizadas exitosamente.')
    } else {
      await userService.createUser(payload)
      showSuccess('Nuevo operador registrado y acreditado exitosamente.')
    }
    navigationStore.setAdministracionTiSubOption('users')
    await loadData()
  } catch (err) {
    showError(err.response?.data?.message || err.response?.data?.error || 'No se pudo completar la operación.')
  } finally {
    done?.()
  }
}

const handleToggleStatus = async (user) => {
  const isActivo = user.estadoActivo
  const confirmed = await confirmAction({
    message: isActivo
      ? `¿Está seguro de desactivar la cuenta del usuario @${user.username}? Esta es una baja lógica.`
      : `¿Desea reactivar la cuenta del usuario @${user.username}?`,
    header: isActivo ? 'Confirmar Desactivación' : 'Confirmar Reactivación',
    icon: isActivo ? 'pi pi-user-minus' : 'pi pi-user-plus',
    acceptLabel: isActivo ? 'Desactivar' : 'Reactivar',
    acceptClass: isActivo ? 'p-button-danger' : 'p-button-success'
  })
  if (!confirmed) return

  try {
    if (isActivo) {
      await userService.deleteUser(user.idUsuario)
      showSuccess(`Cuenta @${user.username} desactivada correctamente.`)
    } else {
      await userService.reactivateUser(user.idUsuario)
      showSuccess(`Cuenta @${user.username} reactivada correctamente.`)
    }
    await loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al modificar el estado del usuario.')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-management-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
</style>
