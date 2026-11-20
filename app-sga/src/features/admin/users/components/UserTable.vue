<template>
  <section class="sga-list-panel">
    <!-- Barra superior sgaUI: conteo a la izquierda, acciones a la derecha -->
    <div class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag :value="`${activeUsersCount} cuentas activas`" severity="secondary" />
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
          <IconField class="sga-filter-search">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model.trim="searchQuery"
              placeholder="Buscar usuario, nombre, DNI o SUNAT..."
              size="small"
              fluid
            />
          </IconField>

          <Select
            v-model="selectedRoleFilter"
            :options="roleOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Todos los Roles"
            size="small"
            class="sga-filter-select"
          />

          <Select
            v-model="selectedSedeFilter"
            :options="sedeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Todas las Sedes"
            size="small"
            class="sga-filter-select"
          />

          <Select
            v-model="statusFilter"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Todos los Estados"
            size="small"
            class="sga-filter-select"
          />

          <Select
            v-model="sortBy"
            :options="sortOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            class="sga-filter-select sga-sort-select"
          />

          <Button
            v-if="hasActiveFilters"
            type="button"
            label="Limpiar"
            icon="pi pi-filter-slash"
            severity="secondary"
            outlined
            size="small"
            title="Limpiar filtros"
            @click="resetFilters"
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
              <div class="usr-avatar" :style="getWarmAvatarStyle(user.username)">
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
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import BasePagination from '@/shared/components/BasePagination.vue'
import { usePagination } from '@/shared/composables/usePagination'
import { ROLE_LABELS } from '@/core/auth/roles'
import { getInitials, getWarmAvatarStyle } from '../../utils/adminFormatters'
import UserRoleBadge from './UserRoleBadge.vue'

const props = defineProps({
  users: { type: Array, default: () => [] },
  roles: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['edit', 'toggle-status', 'reset-password', 'refresh', 'create'])

const searchQuery = ref('')
const selectedRoleFilter = ref('ALL')
const selectedSedeFilter = ref('ALL')
const statusFilter = ref('ALL')
const sortBy = ref('RECENTS')

const activeUsersCount = computed(() => props.users.filter((u) => u.estadoActivo).length)

const roleOptions = computed(() => [
  { label: 'Todos los Roles', value: 'ALL' },
  ...(props.roles || []).map((r) => ({
    label: ROLE_LABELS[r.nombreRol] || r.nombreRol,
    value: r.nombreRol
  }))
])

const sedeOptions = computed(() => {
  const map = new Map()
  props.users.forEach((u) => {
    if (u.codigoSede && !map.has(u.codigoSede)) {
      map.set(u.codigoSede, {
        label: `${u.codigoSede} — ${u.nombreSede || 'Sede'}`,
        value: u.codigoSede
      })
    }
  })
  return [
    { label: 'Todas las Sedes', value: 'ALL' },
    ...Array.from(map.values()).sort((a, b) => a.label.localeCompare(b.label))
  ]
})

const statusOptions = [
  { label: 'Todos los Estados', value: 'ALL' },
  { label: 'Solo Activos', value: 'ACTIVE' },
  { label: 'Solo Inactivos', value: 'INACTIVE' }
]

const sortOptions = [
  { label: 'Orden: Más recientes', value: 'RECENTS' },
  { label: 'Orden: Nombre A–Z', value: 'NOMBRE_ASC' },
  { label: 'Orden: Nombre Z–A', value: 'NOMBRE_DESC' },
  { label: 'Orden: Usuario A–Z', value: 'USER_ASC' }
]

const hasActiveFilters = computed(() =>
  Boolean(
    searchQuery.value ||
      selectedRoleFilter.value !== 'ALL' ||
      selectedSedeFilter.value !== 'ALL' ||
      statusFilter.value !== 'ALL' ||
      sortBy.value !== 'RECENTS'
  )
)

const resetFilters = () => {
  searchQuery.value = ''
  selectedRoleFilter.value = 'ALL'
  selectedSedeFilter.value = 'ALL'
  statusFilter.value = 'ALL'
  sortBy.value = 'RECENTS'
}

const filteredUsers = computed(() => {
  const list = props.users.filter((user) => {
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

    const matchesRole = selectedRoleFilter.value === 'ALL' || user.nombreRol === selectedRoleFilter.value
    const matchesSede = selectedSedeFilter.value === 'ALL' || user.codigoSede === selectedSedeFilter.value
    const matchesStatus =
      statusFilter.value === 'ALL' ||
      (statusFilter.value === 'ACTIVE' && user.estadoActivo === true) ||
      (statusFilter.value === 'INACTIVE' && user.estadoActivo === false)

    return matchesSearch && matchesRole && matchesSede && matchesStatus
  })

  return [...list].sort((a, b) => {
    const nameA = `${a.nombres || ''} ${a.apellidos || ''}`.trim()
    const nameB = `${b.nombres || ''} ${b.apellidos || ''}`.trim()
    if (sortBy.value === 'NOMBRE_DESC') return nameB.localeCompare(nameA, 'es')
    if (sortBy.value === 'USER_ASC') return (a.username || '').localeCompare(b.username || '', 'es')
    if (sortBy.value === 'NOMBRE_ASC') return nameA.localeCompare(nameB, 'es')
    return (b.idUsuario || 0) - (a.idUsuario || 0)
  })
})

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

watch([searchQuery, selectedRoleFilter, selectedSedeFilter, statusFilter, sortBy], () => {
  resetPage()
})
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
