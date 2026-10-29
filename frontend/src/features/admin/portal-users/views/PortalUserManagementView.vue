<template>
  <div class="portal-user-management-view">
    <!-- 01. Formulario de Creación / Edición -->
    <PortalUserForm
      v-if="navigationStore.administracionTiSubOption === 'create-portal-user' || isEditing"
      :user-to-edit="selectedUser"
      @cancel="handleFormCancel"
      @saved="handleUserSaved"
    />

    <!-- 02. Directorio Principal y Métricas B2B -->
    <div v-else class="portal-users-master-container">
      <!-- 1. Barra de Herramientas y Acciones Rápidas (Estándar Editorial Cálido) -->
      <div class="user-action-toolbar">
        <div class="toolbar-left-info">
          <span class="badge-kicker">ADMINISTRACIÓN B2B // CONTROL DE ACCESOS PORTAL CLIENTES</span>
          <h3 class="toolbar-title">Directorio de Usuarios del Portal y Acreditaciones</h3>
        </div>

        <div class="toolbar-actions-group">
          <span class="count-badge font-mono">{{ users.length }} cuentas registradas</span>

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
            class="btn-espresso"
            @click="openCreateView"
          >
            <i class="pi pi-plus"></i>
            <span>+ Nuevo Usuario Portal</span>
          </button>
        </div>
      </div>

      <!-- 2. Grid de Métricas KPI Técnicas B2B -->
      <div class="metrics-kpi-grid">
        <!-- Tarjeta 01: Total Cuentas B2B -->
        <div class="kpi-card" @click="loadData">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">Cuentas Registradas</span>
            <i class="pi pi-users kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ users.length }}</span>
            <span class="kpi-badge accent">{{ uniqueEmpresasCount }} Empresas</span>
          </div>
          <div class="kpi-footer-note">
            <span>Usuarios de clientes en portal</span>
          </div>
        </div>

        <!-- Tarjeta 02: Cuentas Activas -->
        <div class="kpi-card success-card">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">Accesos Vigentes</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-success font-mono">{{ activeUsersCount }}</span>
            <span class="kpi-badge success">Habilitados</span>
          </div>
          <div class="kpi-footer-note">
            <span>Sesiones y credenciales activas</span>
          </div>
        </div>

        <!-- Tarjeta 03: Desactivados / Baja Lógica -->
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

        <!-- Tarjeta 04: Claves Pendientes de Cambio -->
        <div class="kpi-card">
          <div class="kpi-card-header">
            <span class="kpi-num">04</span>
            <span class="kpi-label">Primer Ingreso Pendiente</span>
            <i class="pi pi-key kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ pendingPasswordResetCount }}</span>
            <span class="kpi-badge info">Por Actualizar</span>
          </div>
          <div class="kpi-footer-note">
            <span>Requieren cambio de contraseña</span>
          </div>
        </div>
      </div>

      <!-- 3. Sección de Directorio y Seguimiento -->
      <div class="section-container table-section">
        <div class="table-section-heading">
          <span class="badge-kicker">SEGUIMIENTO DE CUENTAS & AUDITORÍA B2B</span>
          <h3 class="section-title">Directorio de Representantes y Credenciales del Portal</h3>
        </div>

        <!-- Tabla Principal de Usuarios del Portal -->
        <PortalUserTable
          :users="users"
          :loading="loading"
          @edit="openEdit"
          @audit="openAudit"
          @reset-password="openResetPassword"
          @toggle-status="handleToggleStatus"
        />
      </div>
    </div>

    <!-- Modales de Auditoría y Reseteo de Contraseña -->
    <PortalUserAuditModal
      v-model:visible="showAuditModal"
      :user="userToAudit"
    />

    <PortalUserResetPasswordModal
      v-model:visible="showResetPasswordModal"
      :user="userToReset"
      @saved="loadData"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
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

const activeUsersCount = computed(() => users.value.filter((u) => u.estadoActivo).length)
const inactiveUsersCount = computed(() => users.value.filter((u) => !u.estadoActivo).length)
const pendingPasswordResetCount = computed(() => users.value.filter((u) => u.debeCambiarPassword).length)
const uniqueEmpresasCount = computed(() => {
  const set = new Set()
  users.value.forEach((u) => {
    if (u.idCliente) set.add(u.idCliente)
  })
  return set.size
})

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
  const actionText = isActivating ? 'reactivar' : 'desactivar (baja lógica)'

  const confirmed = await confirmAction({
    header: isActivating ? 'Reactivar Cuenta del Portal' : 'Desactivar Cuenta del Portal',
    message: `¿Está seguro de que desea ${actionText} la cuenta de @${user.username}?`,
    acceptLabel: isActivating ? 'Sí, Reactivar' : 'Sí, Desactivar',
    acceptClass: isActivating ? 'p-button-success' : 'p-button-danger'
  })
  if (!confirmed) {
    return
  }

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
    const msg = err.response?.data?.message || 'Error al modificar el estado del usuario'
    showError(msg)
  }
}
</script>

<style scoped>
.portal-user-management-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

.portal-users-master-container {
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
