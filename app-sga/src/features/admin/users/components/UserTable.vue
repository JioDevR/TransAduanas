<template>
  <section class="sga-list-panel">
    <!-- Barra superior sgaUI: conteo a la izquierda, acciones a la derecha (sin título duplicado) -->
    <div class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag :value="`${filteredUsers.length} operadores`" severity="secondary" />
      </div>

      <div class="sga-dash-actions">
        <Button
          label="Actualizar"
          icon="pi pi-sync"
          severity="secondary"
          outlined
          size="small"
          :loading="loading"
          @click="$emit('refresh')"
        />
        <Button
          label="Nuevo Usuario"
          icon="pi pi-plus"
          size="small"
          @click="$emit('create')"
        />
      </div>
    </div>

    <!-- Contenedor sgaUI de Filtros + Tarjetas Canvas -->
    <div class="sga-card-surface sga-table-card">
      <div class="sga-table-toolbar">
        <div class="sga-toolbar-filters">
          <div class="sga-filter-search">
            <InputText
              v-model="searchQuery"
              placeholder="Buscar por usuario, nombre, DNI, SUNAT o cargo..."
              size="small"
              fluid
            />
          </div>

          <Select
            v-model="selectedRoleFilter"
            :options="roleOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            style="min-width: 190px"
          />

          <Select
            v-model="statusFilter"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            style="min-width: 160px"
          />
        </div>
      </div>

      <div v-if="loading && paginatedUsers.length === 0" class="sga-empty-state">
        <i class="pi pi-spin pi-spinner sga-empty-icon"></i>
        <p class="sga-empty-desc">Cargando usuarios internos...</p>
      </div>

      <div v-else-if="paginatedUsers.length === 0" class="sga-empty-state">
        <i class="pi pi-users sga-empty-icon"></i>
        <p class="sga-empty-title">No se encontraron usuarios</p>
        <p class="sga-empty-desc">Prueba ajustando los filtros o registrando un nuevo operador.</p>
      </div>

      <div v-else class="sga-canvas-grid">
        <article v-for="user in paginatedUsers" :key="user.idUsuario" class="sga-canvas-card">
          <div class="sga-card-top">
            <div class="usr-card-identity">
              <div class="usr-avatar" :style="getAvatarBg(user.username)">
                {{ getInitials(user.nombres, user.apellidos) }}
              </div>
              <span class="sga-card-code sga-mono">@{{ user.username }}</span>
            </div>
            <Tag
              :value="user.estadoActivo ? 'Activo' : 'Inactivo'"
              :severity="user.estadoActivo ? 'success' : 'danger'"
              rounded
            />
          </div>

          <div class="sga-card-client">
            <h4 class="sga-client-title">{{ user.nombres }} {{ user.apellidos }}</h4>
            <div class="sga-client-meta">
              <UserRoleBadge :roleName="user.nombreRol" />
              <Tag v-if="user.codigoSunat" :value="user.codigoSunat" severity="info" />
            </div>
          </div>

          <div class="sga-card-meta-grid">
            <div class="sga-meta-item">
              <span class="sga-meta-label">DOCUMENTO</span>
              <span class="sga-meta-val sga-mono">
                {{ user.numeroDocumento ? `${user.tipoDocumento || 'DNI'}: ${user.numeroDocumento}` : '—' }}
              </span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">SEDE</span>
              <span class="sga-meta-val">{{ user.codigoSede || '—' }} · {{ user.nombreSede || 'Sin sede' }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">CARGO</span>
              <span class="sga-meta-val">{{ user.cargoTitulo || '—' }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">TELÉFONO</span>
              <span class="sga-meta-val sga-mono">{{ user.telefono || '—' }}</span>
            </div>
            <div class="sga-meta-item full-span">
              <span class="sga-meta-label">CORREO INSTITUCIONAL</span>
              <span class="sga-meta-val sga-mono">{{ user.correoInstitucional }}</span>
            </div>
          </div>

          <div class="sga-card-footer">
            <Button label="Editar" size="small" outlined severity="secondary" @click="$emit('edit', user)" />
            <div class="usr-actions">
              <Button
                icon="pi pi-key"
                size="small"
                outlined
                severity="secondary"
                title="Restablecer contraseña"
                @click="$emit('reset-password', user)"
              />
              <Button
                :label="user.estadoActivo ? 'Baja' : 'Activar'"
                :severity="user.estadoActivo ? 'danger' : 'success'"
                outlined
                size="small"
                @click="$emit('toggle-status', user)"
              />
            </div>
          </div>
        </article>
      </div>

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
  </section>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import BasePagination from '@/shared/components/BasePagination.vue'
import { usePagination } from '@/shared/composables/usePagination'
import { ROLE_LABELS } from '@/core/auth/roles'
import UserRoleBadge from './UserRoleBadge.vue'

const props = defineProps({
  users: { type: Array, default: () => [] },
  roles: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['edit', 'toggle-status', 'reset-password', 'refresh', 'create'])

const searchQuery = ref('')
const selectedRoleFilter = ref('')
const statusFilter = ref('ALL')

const roleOptions = computed(() => [
  { label: 'Todos los Roles', value: '' },
  ...(props.roles || []).map((r) => ({
    label: ROLE_LABELS[r.nombreRol] || r.nombreRol,
    value: r.nombreRol
  }))
])

const statusOptions = [
  { label: 'Todos los Estados', value: 'ALL' },
  { label: 'Solo Activos', value: 'ACTIVE' },
  { label: 'Solo Inactivos', value: 'INACTIVE' }
]

const filteredUsers = computed(() =>
  props.users.filter((user) => {
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

    const matchesRole = !selectedRoleFilter.value || user.nombreRol === selectedRoleFilter.value
    const matchesStatus =
      statusFilter.value === 'ALL' ||
      (statusFilter.value === 'ACTIVE' && user.estadoActivo === true) ||
      (statusFilter.value === 'INACTIVE' && user.estadoActivo === false)

    return matchesSearch && matchesRole && matchesStatus
  })
)

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
} = usePagination(filteredUsers, 12)

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
  const warmHues = [25, 35, 45, 18, 195, 140]
  const hue = warmHues[Math.abs(hash) % warmHues.length]
  return { background: `hsl(${hue}, 45%, 38%)`, color: '#ffffff' }
}
</script>

<style scoped>
.usr-card-identity {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.usr-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: 700;
  flex-shrink: 0;
}

.usr-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}
</style>
