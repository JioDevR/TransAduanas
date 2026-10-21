<template>
  <div class="user-table-container">
    <!-- Barra de Filtros y Búsqueda -->
    <div class="table-toolbar">
      <div class="search-box">
        <i class="pi pi-search search-icon"></i>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Buscar por usuario, nombre, documento, código SUNAT o cargo..."
          class="search-input"
        />
        <button
          v-if="searchQuery"
          @click="searchQuery = ''"
          class="clear-search-btn"
          type="button"
          title="Limpiar búsqueda"
        >
          <i class="pi pi-times"></i>
        </button>
      </div>

      <div class="filters-group">
        <!-- Filtro por Rol -->
        <select v-model="selectedRoleFilter" class="filter-select">
          <option value="">Todos los Roles</option>
          <option v-for="rol in roles" :key="rol.idRol" :value="rol.nombreRol">
            {{ ROLE_LABELS[rol.nombreRol] || rol.nombreRol }}
          </option>
        </select>

        <!-- Filtro por Estado -->
        <select v-model="statusFilter" class="filter-select">
          <option value="ALL">Todos los Estados</option>
          <option value="ACTIVE">Solo Activos</option>
          <option value="INACTIVE">Solo Inactivos</option>
        </select>
      </div>
    </div>

    <!-- Contenedor de la Tabla con PrimeVue DataTable -->
    <div class="table-responsive">
      <DataTable
        :value="paginatedUsers"
        :loading="loading"
        responsiveLayout="scroll"
        class="custom-datatable"
        dataKey="idUsuario"
        :rowClass="(data) => ({ 'row-inactive': !data.estadoActivo })"
      >
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-users empty-icon"></i>
            <p class="empty-title">No se encontraron usuarios</p>
            <p class="empty-desc">Prueba cambiando los filtros o agregando un nuevo usuario.</p>
          </div>
        </template>

        <template #loading>
          <div class="loading-state">
            <i class="pi pi-spin pi-spinner"></i>
            <span>Cargando usuarios del sistema...</span>
          </div>
        </template>

        <!-- Columna: Usuario / Nombre -->
        <Column header="Usuario / Nombre">
          <template #body="{ data: user }">
            <div class="user-info-cell">
              <div class="user-avatar" :style="getAvatarBg(user.username)">
                {{ getInitials(user.nombres, user.apellidos) }}
              </div>
              <div>
                <div class="user-username-row">
                  <span class="user-username">@{{ user.username }}</span>
                  <span v-if="user.numeroDocumento" class="user-doc-badge">
                    {{ user.tipoDocumento || 'DNI' }}: {{ user.numeroDocumento }}
                  </span>
                </div>
                <div class="user-name">{{ user.nombres }} {{ user.apellidos }}</div>
              </div>
            </div>
          </template>
        </Column>

        <!-- Columna: Rol Asignado -->
        <Column header="Rol Asignado">
          <template #body="{ data: user }">
            <UserRoleBadge :roleName="user.nombreRol" />
          </template>
        </Column>

        <!-- Columna: Sede Operativa -->
        <Column header="Sede Operativa">
          <template #body="{ data: user }">
            <div class="sede-cell">
              <span class="sede-code">{{ user.codigoSede || '—' }}</span>
              <span class="sede-name">{{ user.nombreSede || 'Sin sede' }}</span>
            </div>
          </template>
        </Column>

        <!-- Columna: Contacto -->
        <Column header="Contacto">
          <template #body="{ data: user }">
            <div class="contact-cell">
              <div class="contact-email">
                <i class="pi pi-envelope"></i>
                <span>{{ user.correoInstitucional }}</span>
              </div>
              <div v-if="user.telefono" class="contact-phone">
                <i class="pi pi-phone"></i>
                <span>{{ user.telefono }}</span>
              </div>
            </div>
          </template>
        </Column>

        <!-- Columna: Cargo -->
        <Column header="Cargo">
          <template #body="{ data: user }">
            <div class="cargo-cell">
              <span class="cargo-text">{{ user.cargoTitulo || '—' }}</span>
              <span v-if="user.codigoSunat" class="sunat-table-badge" title="Operador acreditado ante SUNAT">
                <i class="pi pi-verified"></i> {{ user.codigoSunat }}
              </span>
              <span v-else class="sunat-table-none">Sin código SUNAT</span>
            </div>
          </template>
        </Column>

        <!-- Columna: Estado -->
        <Column header="Estado">
          <template #body="{ data: user }">
            <Tag
              :value="user.estadoActivo ? 'Activo' : 'Desactivado'"
              :severity="user.estadoActivo ? 'success' : 'danger'"
              rounded
            />
          </template>
        </Column>

        <!-- Columna: Acciones -->
        <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: user }">
            <div class="actions-wrapper">
              <button
                type="button"
                class="action-btn-outline"
                title="Editar datos y rol del operador"
                @click="$emit('edit', user)"
              >
                Editar
              </button>

              <button
                v-if="user.estadoActivo"
                type="button"
                class="action-btn-outline btn-danger-action"
                title="Desactivar usuario (baja lógica)"
                @click="$emit('toggle-status', user)"
              >
                Eliminar
              </button>
              <button
                v-else
                type="button"
                class="action-btn-outline btn-reactivate-action"
                title="Reactivar usuario"
                @click="$emit('toggle-status', user)"
              >
                Activar
              </button>
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Paginador Reutilizable con Estilo Cálido -->
    <BasePagination
      v-if="filteredUsers.length > 0"
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="totalItems"
      :start-index="startIndex"
      :end-index="endIndex"
      item-label="usuarios"
      @update:current-page="goToPage($event)"
      @prev="prevPage"
      @next="nextPage"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import UserRoleBadge from './UserRoleBadge.vue'
import BasePagination from '@/shared/components/BasePagination.vue'
import { usePagination } from '@/shared/composables/usePagination'
import { ROLE_LABELS } from '@/core/auth/roles'

const props = defineProps({
  users: {
    type: Array,
    default: () => []
  },
  roles: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['edit', 'toggle-status'])

const searchQuery = ref('')
const selectedRoleFilter = ref('')
const statusFilter = ref('ALL')

const filteredUsers = computed(() => {
  return props.users.filter((user) => {
    const q = searchQuery.value.toLowerCase().trim()
    const matchesSearch =
      !q ||
      (user.username && user.username.toLowerCase().includes(q)) ||
      (user.nombres && user.nombres.toLowerCase().includes(q)) ||
      (user.apellidos && user.apellidos.toLowerCase().includes(q)) ||
      (user.correoInstitucional && user.correoInstitucional.toLowerCase().includes(q)) ||
      (user.cargoTitulo && user.cargoTitulo.toLowerCase().includes(q)) ||
      (user.numeroDocumento && user.numeroDocumento.includes(q)) ||
      (user.codigoSunat && user.codigoSunat.toLowerCase().includes(q))

    const matchesRole =
      !selectedRoleFilter.value || user.nombreRol === selectedRoleFilter.value

    let matchesStatus = true
    if (statusFilter.value === 'ACTIVE') {
      matchesStatus = user.estadoActivo === true
    } else if (statusFilter.value === 'INACTIVE') {
      matchesStatus = user.estadoActivo === false
    }

    return matchesSearch && matchesRole && matchesStatus
  })
})

// Gestión de Paginación mediante composable desacoplado
const {
  currentPage,
  totalPages,
  totalItems,
  paginatedItems: paginatedUsers,
  startIndex,
  endIndex,
  prevPage,
  nextPage,
  goToPage,
  resetPage
} = usePagination(filteredUsers, 8)

// Resetear a primera página al buscar o cambiar filtros
watch([searchQuery, selectedRoleFilter, statusFilter], () => {
  resetPage()
})

const getInitials = (nombres = '', apellidos = '') => {
  const n = nombres.trim().charAt(0) || ''
  const a = apellidos.trim().charAt(0) || ''
  return (n + a).toUpperCase() || 'U'
}

const getAvatarBg = (username = '') => {
  let hash = 0
  for (let i = 0; i < username.length; i++) {
    hash = username.charCodeAt(i) + ((hash << 5) - hash)
  }
  // Tonos cálidos tierra
  const warmHues = [25, 35, 45, 18, 195, 140]
  const hue = warmHues[Math.abs(hash) % warmHues.length]
  return {
    background: `hsl(${hue}, 45%, 38%)`,
    color: '#ffffff'
  }
}
</script>

<style scoped>
.user-table-container {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-subtle);
}

.table-toolbar {
  padding: 1.15rem 1.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-bg-subtle);
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 260px;
  max-width: 440px;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.95rem;
  color: var(--color-text-subtle);
  font-size: 0.9rem;
  pointer-events: none;
}

.search-input {
  width: 100%;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.6rem 2.2rem 0.6rem 2.5rem;
  color: var(--color-text-main);
  font-size: 0.85rem;
  outline: none;
  transition: all var(--transition-fast);
}

.search-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(216, 185, 125, 0.2);
}

.clear-search-btn {
  position: absolute;
  right: 0.65rem;
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.25rem;
}

.filters-group {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.filter-select {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.6rem 0.95rem;
  color: var(--color-text-main);
  font-size: 0.825rem;
  font-weight: 500;
  outline: none;
  cursor: pointer;
  transition: border-color var(--transition-fast);
}

.filter-select:focus {
  border-color: var(--color-primary);
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

/* Estilos DataTable PrimeVue con estética cálida de la referencia */
:deep(.custom-datatable.p-datatable) {
  width: 100%;
  font-family: inherit;
  font-size: 0.86rem;
  background: transparent;
}

:deep(.custom-datatable .p-datatable-table) {
  width: 100%;
  border-collapse: collapse;
}

:deep(.custom-datatable .p-datatable-thead > tr > th) {
  background: var(--color-bg-subtle) !important;
  color: var(--color-text-muted) !important;
  font-weight: 700 !important;
  font-size: 0.725rem !important;
  text-transform: uppercase !important;
  letter-spacing: 0.06em !important;
  padding: 0.95rem 1.25rem !important;
  border-bottom: 1px solid var(--color-border) !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
}

:deep(.custom-datatable .p-datatable-tbody > tr) {
  border-bottom: 1px solid var(--color-border-subtle) !important;
  transition: background var(--transition-fast) !important;
  background: var(--color-bg-surface) !important;
}

:deep(.custom-datatable .p-datatable-tbody > tr:hover) {
  background: var(--color-bg-subtle) !important;
}

:deep(.custom-datatable .p-datatable-tbody > tr.row-inactive) {
  background: var(--color-bg-muted) !important;
  opacity: 0.75;
}

:deep(.custom-datatable .p-datatable-tbody > tr > td) {
  padding: 0.95rem 1.25rem !important;
  vertical-align: middle !important;
  border: none !important;
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.825rem;
  letter-spacing: -0.02em;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(45, 35, 25, 0.15);
}

.user-username {
  font-weight: 700;
  color: var(--color-text-main);
  font-size: 0.875rem;
}

.user-name {
  color: var(--color-text-muted);
  font-size: 0.785rem;
  margin-top: 0.1rem;
}

.sede-cell {
  display: flex;
  flex-direction: column;
}

.sede-code {
  font-weight: 700;
  color: var(--color-text-main);
  font-size: 0.8rem;
}

.sede-name {
  color: var(--color-text-subtle);
  font-size: 0.75rem;
}

.contact-cell {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  font-size: 0.785rem;
}

.contact-email,
.contact-phone {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--color-text-muted);
}

.contact-email i,
.contact-phone i {
  color: var(--color-text-subtle);
  font-size: 0.75rem;
}

.user-username-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.user-doc-badge {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--color-text-subtle);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.cargo-cell {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.cargo-text {
  color: var(--color-text-main);
  font-size: 0.825rem;
  font-weight: 500;
}

.sunat-table-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  width: fit-content;
}

:global(.dark-theme) .sunat-table-badge {
  background: #362e1c;
  color: #f7d270;
  border-color: #63522b;
}

.sunat-table-none {
  font-size: 0.65rem;
  color: var(--color-text-subtle);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill);
  font-size: 0.75rem;
  font-weight: 600;
}

.status-badge.active {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-badge.inactive {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.text-right {
  text-align: right;
}

.actions-wrapper {
  display: inline-flex;
  gap: 0.5rem;
}

/* Botones Outline de Acción idénticos a [Editar] [Eliminar] de la imagen */
.action-btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.35rem 0.8rem;
  border-radius: 6px;
  font-size: 0.785rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn-outline:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
  color: var(--color-text-main);
}

.btn-danger-action:hover {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.btn-reactivate-action:hover {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 3.5rem 1.5rem;
  color: var(--color-text-muted);
}

.loading-state i {
  font-size: 1.5rem;
  display: block;
  margin-bottom: 0.75rem;
  color: var(--color-primary);
}

.empty-icon {
  font-size: 2.25rem;
  color: var(--color-text-subtle);
  margin-bottom: 0.75rem;
}

.empty-title {
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.empty-desc {
  font-size: 0.8rem;
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
}

</style>
