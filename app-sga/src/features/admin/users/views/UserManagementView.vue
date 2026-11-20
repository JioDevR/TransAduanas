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

    <template v-else>
      <!-- Barra de Estado y Acciones sgaUI (Título único en Topbar) -->
      <header class="sga-dash-header">
        <div class="sga-dash-meta">
          <Tag :value="`${users.length} operadores registrados`" severity="secondary" />
        </div>

        <div class="sga-dash-actions">
          <Button
            label="Actualizar"
            icon="pi pi-sync"
            severity="secondary"
            outlined
            size="small"
            :loading="loading"
            @click="loadData"
          />
          <Button
            label="Reporte PDF"
            icon="pi pi-file-pdf"
            severity="secondary"
            outlined
            size="small"
            :loading="exportingPdf"
            @click="handleExportPdf"
          />
          <Button
            label="Nuevo Usuario"
            icon="pi pi-plus"
            size="small"
            @click="openCreateDialog"
          />
        </div>
      </header>

      <!-- Tabla Dual SgaListShell -->
      <UserTable
        :users="users"
        :roles="roles"
        :loading="loading"
        @edit="openEditDialog"
        @toggle-status="handleToggleStatus"
        @reset-password="openResetPassword"
      />
    </template>

    <UserResetPasswordModal
      v-model:visible="showResetModal"
      :user="userToReset"
      @saved="loadData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { userService } from '../services/userService'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { openBlobInNewTab } from '@/shared/utils/download'
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
const exportingPdf = ref(false)

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

watch(
  () => navigationStore.administracionTiActionTrigger,
  (action) => {
    if (action === 'export-pdf') handleExportPdf()
  }
)

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

const handleExportPdf = async () => {
  if (exportingPdf.value) return
  exportingPdf.value = true
  try {
    const blobData = await userService.downloadPdf(true)
    openBlobInNewTab(blobData, 'reporte-usuarios-activos.pdf')
  } catch (err) {
    showError('No se pudo generar el reporte PDF de usuarios internos.')
  } finally {
    exportingPdf.value = false
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
