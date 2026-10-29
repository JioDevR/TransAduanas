<template>
  <div class="portal-user-table-container">
    <!-- Barra de Filtros y Búsqueda -->
    <div class="table-toolbar">
      <div class="search-box">
        <i class="pi pi-search search-icon"></i>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Buscar por usuario, nombre, correo, RUC, empresa o cargo..."
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
        <!-- Filtro por Empresa Cliente -->
        <select v-model="selectedClienteFilter" class="filter-select">
          <option value="">Todas las Empresas</option>
          <option v-for="c in availableClientes" :key="c.idCliente" :value="c.idCliente">
            {{ c.razonSocial }} ({{ c.numeroDocumento }})
          </option>
        </select>

        <!-- Filtro por Estado -->
        <select v-model="statusFilter" class="filter-select">
          <option value="ALL">Todos los Estados</option>
          <option value="ACTIVE">Solo Activos</option>
          <option value="INACTIVE">Solo Inactivos (Baja Lógica)</option>
        </select>
      </div>
    </div>

    <!-- Contenedor de la Tabla PrimeVue DataTable -->
    <div class="table-responsive">
      <DataTable
        :value="paginatedUsers"
        :loading="loading"
        responsiveLayout="scroll"
        class="custom-datatable"
        dataKey="idUsuarioCliente"
        :rowClass="(data) => ({ 'row-inactive': !data.estadoActivo })"
      >
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-users empty-icon"></i>
            <p class="empty-title">No se encontraron usuarios del portal</p>
            <p class="empty-desc">Prueba ajustando los filtros o creando un nuevo usuario corporativo.</p>
          </div>
        </template>

        <template #loading>
          <div class="loading-state">
            <i class="pi pi-spin pi-spinner"></i>
            <span>Cargando usuarios del portal B2B...</span>
          </div>
        </template>

        <!-- Columna 01: Empresa Cliente -->
        <Column header="Empresa Cliente / RUC">
          <template #body="{ data: user }">
            <div class="empresa-cell">
              <span class="empresa-name font-bold">{{ user.razonSocialCliente || '—' }}</span>
              <div class="empresa-doc-row">
                <span class="doc-badge font-mono">{{ user.tipoDocumentoCliente || 'RUC' }}: {{ user.rucCliente }}</span>
              </div>
            </div>
          </template>
        </Column>

        <!-- Columna 02: Usuario / Nombre -->
        <Column header="Usuario / Representante">
          <template #body="{ data: user }">
            <div class="user-info-cell">
              <div class="user-avatar" :style="getAvatarBg(user.username)">
                {{ getInitials(user.nombres, user.apellidos) }}
              </div>
              <div>
                <div class="user-username-row">
                  <span class="user-username font-mono">@{{ user.username }}</span>
                </div>
                <div class="user-name">{{ user.contactoRepresentante || user.nombres || 'Representante Legal' }}</div>
              </div>
            </div>
          </template>
        </Column>

        <!-- Columna 03: Contacto & Notificación -->
        <Column header="Contacto & Notificación">
          <template #body="{ data: user }">
            <div class="contact-cell">
              <div class="contact-email">
                <i class="pi pi-envelope"></i>
                <span class="font-mono">{{ user.correoElectronico }}</span>
              </div>
              <div v-if="user.telefonoContacto" class="contact-phone">
                <i class="pi pi-phone"></i>
                <span>{{ user.telefonoContacto }}</span>
              </div>
            </div>
          </template>
        </Column>

        <!-- Columna 05: Último Acceso -->
        <Column header="Último Acceso">
          <template #body="{ data: user }">
            <div class="access-cell font-mono">
              <span v-if="user.ultimoAcceso" class="access-date">
                {{ formatAccessDate(user.ultimoAcceso) }}
              </span>
              <span v-else class="never-access-badge">
                Sin accesos
              </span>
            </div>
          </template>
        </Column>

        <!-- Columna 06: Estado -->
        <Column header="Estado">
          <template #body="{ data: user }">
            <Tag
              :value="user.estadoActivo ? 'Activo' : 'Suspendido'"
              :severity="user.estadoActivo ? 'success' : 'danger'"
              rounded
            />
          </template>
        </Column>

        <!-- Columna 07: Acciones -->
        <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: user }">
            <div class="actions-wrapper">
              <button
                type="button"
                class="action-btn-outline"
                title="Editar datos del usuario"
                @click="$emit('edit', user)"
              >
                Editar
              </button>

              <button
                type="button"
                class="action-btn-outline btn-audit-action"
                title="Ver trazabilidad y auditoría"
                @click="$emit('audit', user)"
              >
                Auditar
              </button>

              <button
                type="button"
                class="action-btn-outline btn-key-action"
                title="Restablecer contraseña de acceso"
                @click="$emit('reset-password', user)"
              >
                <i class="pi pi-key"></i>
              </button>

              <button
                v-if="user.estadoActivo"
                type="button"
                class="action-btn-outline btn-danger-action"
                title="Suspender cuenta (baja lógica por estado)"
                @click="$emit('toggle-status', user)"
              >
                Eliminar
              </button>
              <button
                v-else
                type="button"
                class="action-btn-outline btn-reactivate-action"
                title="Reactivar cuenta de cliente"
                @click="$emit('toggle-status', user)"
              >
                Activar
              </button>
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Paginador Reutilizable BasePagination -->
    <BasePagination
      v-if="filteredUsers.length > 0"
      :current-page="currentPage"
      :total-pages="totalPages"
      :total-items="totalItems"
      :start-index="startIndex"
      :end-index="endIndex"
      item-label="usuarios del portal"
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
import BasePagination from '@/shared/components/BasePagination.vue'
import { usePagination } from '@/shared/composables/usePagination'

const props = defineProps({
  users: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['edit', 'audit', 'reset-password', 'toggle-status'])

const searchQuery = ref('')
const selectedClienteFilter = ref('')
const statusFilter = ref('ALL')

const availableClientes = computed(() => {
  const map = new Map()
  props.users.forEach((u) => {
    if (u.idCliente && !map.has(u.idCliente)) {
      map.set(u.idCliente, {
        idCliente: u.idCliente,
        razonSocial: u.razonSocialCliente,
        numeroDocumento: u.rucCliente
      })
    }
  })
  return Array.from(map.values()).sort((a, b) => (a.razonSocial || '').localeCompare(b.razonSocial || ''))
})

const filteredUsers = computed(() => {
  return props.users.filter((user) => {
    if (statusFilter.value === 'ACTIVE' && !user.estadoActivo) return false
    if (statusFilter.value === 'INACTIVE' && user.estadoActivo) return false

    if (selectedClienteFilter.value && user.idCliente !== Number(selectedClienteFilter.value)) {
      return false
    }

    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      const matchUsername = (user.username || '').toLowerCase().includes(q)
      const matchRepresentante = (user.contactoRepresentante || user.nombres || '').toLowerCase().includes(q)
      const matchEmail = (user.correoElectronico || '').toLowerCase().includes(q)
      const matchRuc = (user.rucCliente || '').toLowerCase().includes(q)
      const matchRazon = (user.razonSocialCliente || '').toLowerCase().includes(q)

      return (
        matchUsername ||
        matchRepresentante ||
        matchEmail ||
        matchRuc ||
        matchRazon
      )
    }

    return true
  })
})

const {
  currentPage,
  totalPages,
  totalItems,
  startIndex,
  endIndex,
  paginatedItems: paginatedUsers,
  goToPage,
  nextPage,
  prevPage,
  resetPage
} = usePagination(filteredUsers, 10)

watch([searchQuery, selectedClienteFilter, statusFilter], () => {
  resetPage()
})

const getInitials = (nombres = '', apellidos = '') => {
  const n = (nombres || '').trim().charAt(0)
  const a = (apellidos || '').trim().charAt(0)
  return (n + a).toUpperCase() || 'UC'
}

const getAvatarBg = (username = '') => {
  let hash = 0
  for (let i = 0; i < username.length; i++) {
    hash = username.charCodeAt(i) + ((hash << 5) - hash)
  }
  const warmHues = [25, 35, 45, 18, 195, 140]
  const hue = warmHues[Math.abs(hash) % warmHues.length]
  return {
    background: `hsl(${hue}, 45%, 38%)`,
    color: '#ffffff'
  }
}

const formatAccessDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  return d.toLocaleDateString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.portal-user-table-container {
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
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  background-color: var(--color-bg-surface) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md) !important;
  padding: 0.6rem 2.25rem 0.6rem 0.85rem !important;
  color: var(--color-text-main) !important;
  font-size: 0.825rem !important;
  font-weight: 500;
  outline: none;
  cursor: pointer;
  transition: border-color var(--transition-fast);
}

.filter-select:focus {
  border-color: var(--color-primary) !important;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

/* Estilos DataTable PrimeVue con estética cálida */
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

.empresa-cell {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.empresa-name {
  color: var(--color-text-main);
  font-size: 0.875rem;
}

.doc-badge {
  font-size: 0.725rem;
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  border: 1px solid var(--color-border-subtle);
  padding: 0.1rem 0.45rem;
  border-radius: 4px;
  display: inline-block;
  align-self: flex-start;
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

.user-username-row {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.user-username {
  font-size: 0.825rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.user-name {
  font-size: 0.825rem;
  color: var(--color-text-muted);
  margin-top: 0.1rem;
}

.contact-cell {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  font-size: 0.8rem;
}

.contact-email,
.contact-phone,
.contact-cargo {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--color-text-muted);
}

.contact-email span {
  color: var(--color-text-main);
}

.access-cell {
  font-size: 0.8rem;
}

.access-date {
  color: var(--color-success-text);
}

.never-access-badge {
  color: var(--color-text-subtle);
  font-size: 0.75rem;
}

.actions-wrapper {
  display: inline-flex;
  gap: 0.45rem;
}

.action-btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-size: 0.785rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn-outline:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.btn-audit-action:hover {
  border-color: var(--color-info-border);
  color: var(--color-info-text);
  background: var(--color-info-bg);
}

.btn-key-action {
  padding: 0.35rem 0.5rem;
}

.btn-key-action:hover {
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
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

.empty-state,
.loading-state {
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

.text-right {
  text-align: right;
}

.font-bold {
  font-weight: 700;
}
</style>
