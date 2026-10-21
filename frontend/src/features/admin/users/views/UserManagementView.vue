<template>
  <div class="user-management-view">


    <!-- VISTA 00: USUARIOS & CONTROL TI -->
    <!-- 01. Formulario de Registro / Edición de Usuario -->
    <UserRegistrationForm
      v-if="navigationStore.adminSubOption === 'create-user'"
      :user-to-edit="selectedUser"
      :roles="roles"
      :sedes="sedes"
      @cancel="navigationStore.setAdminSubOption('users')"
      @saved="handleUserSaved"
    />

    <!-- 02. Vista Acerca de (Información del Proyecto y Compilación) -->
    <AboutSystemView
      v-else-if="navigationStore.adminSubOption === 'about'"
    />

    <!-- 03. Sub-opción Usuarios: Directorio y Métricas Específicas de Personal -->
    <template v-else>
        <!-- 1. Hero Banner Card -->
        <HeroBannerCard
          title="Control y administración de usuarios del sistema"
          kicker="GESTIÓN DE OPERACIONES & ACCESOS"
          description="Seguimiento de cada cuenta de operador aduanero, asignación de perfiles de acceso y supervisión de sedes territoriales."
        />

        <!-- 2. Tarjetas de Métricas Rápidas con Números Indexados (01, 02, 03, 04) -->
        <div class="metrics-grid">
          <MetricCard index="01" label="Usuarios del Sistema" :value="users.length" />
          <MetricCard index="02" label="Operadores Activos" :value="activeUsersCount" severity="success" />
          <MetricCard index="03" label="Desactivados / Baja" :value="inactiveUsersCount" severity="danger" />
          <MetricCard index="04" label="Roles en Sistema" :value="roles.length" severity="accent" />
        </div>

    <!-- 3. Sección: Órdenes y Usuarios por Estado -->
    <div class="section-container">
      <div class="section-header-row">
        <div>
          <span class="badge-kicker">ADMINISTRACIÓN DE PERSONAL</span>
          <h3 class="section-title">Usuarios por estado de cuenta</h3>
        </div>

        <div class="section-controls">
          <span class="count-badge">{{ users.length }} usuarios</span>

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
            <span>Ver Reporte</span>
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

      <!-- Tarjetas de Distribución por Estado (Estilo Columnas de la referencia) -->
      <div class="status-columns-grid">
        <div class="status-col-card">
          <div class="status-col-header">
            <span class="status-col-name">Activos</span>
            <span class="status-col-pill active-pill">{{ activeUsersCount }}</span>
          </div>
          <div class="status-col-body">
            <span class="status-sub">Operadores con acceso vigente y sesiones autorizadas</span>
          </div>
        </div>

        <div class="status-col-card">
          <div class="status-col-header">
            <span class="status-col-name">Desactivados</span>
            <span class="status-col-pill inactive-pill">{{ inactiveUsersCount }}</span>
          </div>
          <div class="status-col-body">
            <span class="status-sub">Cuentas con baja lógica o acceso suspendido</span>
          </div>
        </div>

        <div class="status-col-card">
          <div class="status-col-header">
            <span class="status-col-name">Administradores</span>
            <span class="status-col-pill admin-pill-col">{{ adminCount }}</span>
          </div>
          <div class="status-col-body">
            <span class="status-sub">Control total de configuraciones, TI y auditoría</span>
          </div>
        </div>

        <div class="status-col-card">
          <div class="status-col-header">
            <span class="status-col-name">Sedes Registradas</span>
            <span class="status-col-pill sedes-pill">{{ sedes.length }}</span>
          </div>
          <div class="status-col-body">
            <span class="status-sub">Puntos de control y aduanas descentralizadas</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. Sección de Directorio y Seguimiento -->
    <div class="section-container table-section">
      <div class="table-section-heading">
        <span class="badge-kicker">SEGUIMIENTO</span>
        <h3 class="section-title">Directorio de usuarios y credenciales</h3>
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
  </template>
</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, defineAsyncComponent } from 'vue'
import { userService } from '../services/userService'
import { ROLES } from '@/core/auth/roles'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { downloadBlob } from '@/shared/utils/download'
import HeroBannerCard from '@/shared/components/HeroBannerCard.vue'
import MetricCard from '@/shared/components/MetricCard.vue'
import UserTable from '../components/UserTable.vue'
import UserRegistrationForm from '../components/UserRegistrationForm.vue'

// Vista Acerca de para Administrador TI
const AboutSystemView = defineAsyncComponent(() => import('@/features/admin/about/views/AboutSystemView.vue'))

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

const handleUserSaved = async ({ isEdit, userId, payload }) => {
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
  gap: 2rem;
  max-width: 1440px;
  margin: 0 auto;
}

/* 1. Hero Banner Card */
.hero-banner-card {
  position: relative;
  background: linear-gradient(135deg, #1c1917 0%, #292524 60%, #453406 100%);
  border: 1px solid rgba(234, 179, 8, 0.25);
  border-radius: var(--radius-lg, 12px);
  padding: 1.75rem 2rem;
  color: #fafaf9;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.hero-content {
  max-width: 780px;
  position: relative;
  z-index: 2;
}

.badge-kicker {
  display: inline-block;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #eab308;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
}

.hero-title {
  font-size: 1.65rem;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: -0.015em;
  margin: 0.15rem 0 0.45rem;
  line-height: 1.25;
}

.hero-desc {
  font-size: 0.875rem;
  color: #d6d3d1;
  line-height: 1.5;
  margin: 0;
}

.hero-watermark {
  position: absolute;
  right: 1.5rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 7rem;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.03);
  user-select: none;
  pointer-events: none;
}

/* 2. Grid de Métricas */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}

.metric-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1.35rem 1.45rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  box-shadow: var(--shadow-subtle);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
}

.metric-top {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.metric-idx {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 6px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
}

.metric-label {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  font-weight: 600;
}

.metric-val {
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-text-main);
  line-height: 1;
  letter-spacing: -0.02em;
}

/* 3. Secciones Generales */
.section-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 1rem;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.section-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.count-badge {
  font-size: 0.75rem;
  font-weight: 600;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
}

/* Status Columns Grid */
.status-columns-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}

.status-col-card {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  min-height: 105px;
  transition: transform var(--transition-fast), border-color var(--transition-fast);
}

.status-col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-col-name {
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.status-col-pill {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.15rem 0.55rem;
  border-radius: var(--radius-pill);
  min-width: 24px;
  text-align: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
}

.active-pill {
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.inactive-pill {
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
}

.admin-pill-col {
  color: var(--color-accent-kicker);
}

.sedes-pill {
  color: var(--color-text-muted);
}

.status-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.4;
}

/* 4. Tabla Sección */
.table-section {
  margin-top: 0.5rem;
}

.table-section-heading {
  margin-bottom: 0.35rem;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.25s ease;
}
.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}

@media (max-width: 1024px) {
  .metrics-grid,
  .status-columns-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-grid,
  .status-columns-grid {
    grid-template-columns: 1fr;
  }
  .hero-banner-card {
    padding: 1.5rem;
  }
  .hero-watermark {
    display: none;
  }
}

</style>
