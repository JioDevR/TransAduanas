<template>
  <div class="user-management-view">
    <!-- 01. Formulario de Registro / Edición de Usuario a Pantalla Completa -->
    <UserRegistrationForm
      v-if="navigationStore.adminSubOption === 'create-user'"
      :user-to-edit="selectedUser"
      :roles="roles"
      :sedes="sedes"
      @cancel="navigationStore.setAdministracionTiSubOption('users')"
      @saved="handleUserSaved"
    />

    <!-- 02. Directorio Principal y Métricas de Personal (Sin Banners Invasivos) -->
    <div v-else class="users-master-container">
      <!-- 1. Barra de Herramientas y Acciones Rápidas -->
      <div class="user-action-toolbar">
        <div class="toolbar-left-info">
          <span class="badge-kicker">ADMINISTRACIÓN DE PERSONAL // SEGURIDAD & ACCESOS</span>
          <h3 class="toolbar-title">Directorio de Usuarios y Acreditaciones</h3>
        </div>

        <div class="toolbar-actions-group">
          <span class="count-badge font-mono">{{ users.length }} usuarios registrados</span>

          <button
            type="button"
            class="btn-outline-warm"
            @click="loadData"
            :disabled="loading"
            title="Recargar datos desde el servidor"
          >
            <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
            <span>Actualizar</span>
          </button>

          <button
            type="button"
            class="btn-outline-warm"
            @click="handleExportPdf"
            :disabled="exportingPdf"
            title="Descargar reporte oficial JasperReports"
          >
            <i :class="['pi', exportingPdf ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
            <span>Ver Reporte (PDF)</span>
          </button>

          <button
            type="button"
            class="btn-espresso"
            @click="openCreateDialog"
          >
            <i class="pi pi-plus"></i>
            <span>+ Nuevo Usuario</span>
          </button>
        </div>
      </div>

      <!-- 2. Grid de Métricas KPI Técnicas de Personal -->
      <div class="metrics-kpi-grid">
        <!-- Tarjeta 01: Total Usuarios -->
        <div class="kpi-card" @click="loadData">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">Personal Registrado</span>
            <i class="pi pi-users kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ users.length }}</span>
            <span class="kpi-badge accent">{{ roles.length }} Roles</span>
          </div>
          <div class="kpi-footer-note">
            <span>Cuentas de operadores aduaneros</span>
          </div>
        </div>

        <!-- Tarjeta 02: Operadores Activos -->
        <div class="kpi-card success-card">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">Operadores Activos</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-success font-mono">{{ activeUsersCount }}</span>
            <span class="kpi-badge success">Acceso Vigente</span>
          </div>
          <div class="kpi-footer-note">
            <span>Sesiones y credenciales habilitadas</span>
          </div>
        </div>

        <!-- Tarjeta 03: Desactivados / Baja -->
        <div class="kpi-card danger-card">
          <div class="kpi-card-header">
            <span class="kpi-num">03</span>
            <span class="kpi-label">Cuentas Suspendidas</span>
            <i class="pi pi-user-minus kpi-icon danger"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-danger font-mono">{{ inactiveUsersCount }}</span>
            <span class="kpi-badge danger">Baja Lógica</span>
          </div>
          <div class="kpi-footer-note">
            <span>Acceso revocado temporalmente</span>
          </div>
        </div>

        <!-- Tarjeta 04: Administradores TI -->
        <div class="kpi-card">
          <div class="kpi-card-header">
            <span class="kpi-num">04</span>
            <span class="kpi-label">Administradores TI</span>
            <i class="pi pi-shield kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ adminCount }}</span>
            <span class="kpi-badge info">Control Total</span>
          </div>
          <div class="kpi-footer-note">
            <span>Soporte, configuración y auditoría</span>
          </div>
        </div>
      </div>

      <!-- 3. Sección de Directorio y Seguimiento -->
      <div class="section-container table-section">
        <div class="table-section-heading">
          <span class="badge-kicker">SEGUIMIENTO DE CUENTAS & AUDITORÍA</span>
          <h3 class="section-title">Directorio de Operadores y Credenciales</h3>
        </div>

        <!-- Tabla Principal de Usuarios -->
        <UserTable
          :users="users"
          :roles="roles"
          :loading="loading"
          @edit="openEditDialog"
          @toggle-status="handleToggleStatus"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { userService } from '../services/userService'
import { ROLES } from '@/core/auth/roles'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { downloadBlob } from '@/shared/utils/download'
import UserTable from '../components/UserTable.vue'
import UserRegistrationForm from '../components/UserRegistrationForm.vue'

const navigationStore = useNavigationStore()
const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction } = useConfirmation()

const users = ref([])
const roles = ref([])
const sedes = ref([])
const loading = ref(false)
const exportingPdf = ref(false)

const selectedUser = ref(null)

const showToast = (message, type = 'success') => {
  if (type === 'error') {
    showError(message)
  } else if (type === 'info') {
    showInfo(message)
  } else {
    showSuccess(message)
  }
}

const activeUsersCount = computed(() => {
  return users.value.filter((u) => u.estadoActivo).length
})

const inactiveUsersCount = computed(() => {
  return users.value.filter((u) => !u.estadoActivo).length
})

const adminCount = computed(() => {
  return users.value.filter((u) => u.nombreRol === ROLES.ADMINISTRADOR_TI).length
})

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
    showToast(
      err.response?.data?.message || 'Error al conectar con la base de datos de SGA.',
      'error'
    )
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

const handleUserSaved = async ({ isEdit, userId, payload, done }) => {
  try {
    if (isEdit) {
      await userService.updateUser(userId, payload)
      showToast('Ficha del operador y acreditaciones actualizadas exitosamente.')
    } else {
      await userService.createUser(payload)
      showToast('Nuevo operador registrado y acreditado exitosamente.')
    }
    navigationStore.setAdministracionTiSubOption('users')
    await loadData()
  } catch (err) {
    const errorMsg =
      err.response?.data?.message ||
      err.response?.data?.error ||
      'No se pudo completar la operación.'
    showToast(errorMsg, 'error')
  } finally {
    done?.()
  }
}

// Escuchar acciones desde el Sidebar (ej. Exportar PDF)
watch(
  () => navigationStore.administracionTiActionTrigger,
  (action) => {
    if (action === 'export-pdf') {
      handleExportPdf()
    }
  }
)

const handleToggleStatus = async (user) => {
  const isActivo = user.estadoActivo
  const confirmMsg = isActivo
    ? `¿Está seguro de desactivar la cuenta del usuario @${user.username}? Esta es una baja lógica.`
    : `¿Desea reactivar la cuenta del usuario @${user.username}?`

  const confirmed = await confirmAction({
    message: confirmMsg,
    header: isActivo ? 'Confirmar Desactivación' : 'Confirmar Reactivación',
    icon: isActivo ? 'pi pi-user-minus' : 'pi pi-user-plus',
    acceptLabel: isActivo ? 'Desactivar' : 'Reactivar',
    acceptClass: isActivo ? 'p-button-danger' : 'p-button-success'
  })

  if (!confirmed) return

  try {
    if (isActivo) {
      await userService.deleteUser(user.idUsuario)
      showToast(`Usuario @${user.username} desactivado correctamente.`, 'info')
    } else {
      await userService.reactivateUser(user.idUsuario)
      showToast(`Usuario @${user.username} reactivado correctamente.`, 'success')
    }
    await loadData()
  } catch (err) {
    showToast(
      err.response?.data?.message || 'Error al actualizar estado del usuario.',
      'error'
    )
  }
}

const handleExportPdf = async () => {
  exportingPdf.value = true
  try {
    const blob = await userService.downloadPdf()
    downloadBlob(blob, `reporte-usuarios-sga-${Date.now()}.pdf`)
    showToast('Reporte PDF descargado exitosamente.')
  } catch (err) {
    showToast('No se pudo generar el reporte PDF desde JasperReports.', 'error')
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
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

.users-master-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR DE ACCIONES (ESTÁNDAR EDITORIAL CÁLIDO)
   ========================================================================== */
.user-action-toolbar {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1rem 1.35rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  box-shadow: var(--shadow-subtle);
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  text-transform: uppercase;
}

.toolbar-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.count-badge {
  font-size: 0.75rem;
  font-weight: 600;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  padding: 0.4rem 0.75rem;
  border-radius: var(--radius-pill, 9999px);
}

/* ==========================================================================
   2. REJILLA DE MÉTRICAS KPI (ESTÁNDAR EDITORIAL WARM)
   ========================================================================== */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem 1.45rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: var(--shadow-subtle);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
  user-select: none;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-border-strong);
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-num {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 4px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
}

.kpi-label {
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--color-text-muted);
  flex: 1;
}

.kpi-icon {
  font-size: 1.05rem;
  color: var(--color-text-subtle);
}

.kpi-icon.success {
  color: var(--color-success-text);
}

.kpi-icon.danger {
  color: var(--color-danger-text);
}

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-value {
  font-size: 2.15rem;
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1;
  letter-spacing: -0.02em;
}

.kpi-badge {
  font-size: 0.725rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  border: 1px solid transparent;
}

.kpi-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.kpi-badge.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
}

.kpi-badge.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  border-color: var(--color-border-subtle);
}

.kpi-badge.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border-color: var(--color-info-border);
}

.kpi-footer-note {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-border-subtle);
}

/* ==========================================================================
   3. SECCIÓN TABLA DE USUARIOS
   ========================================================================== */
.section-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.table-section-heading {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

/* Botones estándar */
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

.text-success {
  color: var(--color-success-text) !important;
}

.text-danger {
  color: var(--color-danger-text) !important;
}

/* Responsive */
@media (max-width: 1024px) {
  .metrics-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-kpi-grid {
    grid-template-columns: 1fr;
  }
  .toolbar-actions-group {
    width: 100%;
  }
}
</style>
