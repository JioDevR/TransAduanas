<template>
  <SgaListShell v-model:viewMode="viewMode" title="Directorio de Operadores">
    <template #header-right>
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
    </template>

    <template #filters>
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
    </template>

    <template #actions>
      <Tag :value="`${filteredUsers.length} operadores`" severity="secondary" />
    </template>

    <div v-if="viewMode === 'table'">
      <DataTable
        :value="paginatedUsers"
        :loading="loading"
        size="small"
        stripedRows
        responsiveLayout="scroll"
        dataKey="idUsuario"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-users sga-empty-icon"></i>
            <p class="sga-empty-title">No se encontraron usuarios</p>
            <p class="sga-empty-desc">Prueba ajustando los filtros o registrando un nuevo operador.</p>
          </div>
        </template>

        <Column header="Usuario / Operador">
          <template #body="{ data: user }">
            <div class="usr-info-cell">
              <div class="usr-avatar" :style="getAvatarBg(user.username)">
                {{ getInitials(user.nombres, user.apellidos) }}
              </div>
              <div class="usr-stack">
                <div class="usr-name-row">
                  <strong class="sga-mono">@{{ user.username }}</strong>
                  <Tag
                    v-if="user.numeroDocumento"
                    :value="`${user.tipoDocumento || 'DNI'}: ${user.numeroDocumento}`"
                    severity="secondary"
                  />
                </div>
                <span>{{ user.nombres }} {{ user.apellidos }}</span>
              </div>
            </div>
          </template>
        </Column>

        <Column header="Rol">
          <template #body="{ data: user }">
            <UserRoleBadge :roleName="user.nombreRol" />
          </template>
        </Column>

        <Column header="Sede">
          <template #body="{ data: user }">
            <div class="usr-stack">
              <strong class="sga-mono">{{ user.codigoSede || '—' }}</strong>
              <small class="sga-text-muted">{{ user.nombreSede || 'Sin sede' }}</small>
            </div>
          </template>
        </Column>

        <Column header="Contacto">
          <template #body="{ data: user }">
            <div class="usr-stack">
              <span class="sga-mono">{{ user.correoInstitucional }}</span>
              <small v-if="user.telefono" class="sga-mono sga-text-muted">{{ user.telefono }}</small>
            </div>
          </template>
        </Column>

        <Column header="Cargo / SUNAT">
          <template #body="{ data: user }">
            <div class="usr-stack">
              <span>{{ user.cargoTitulo || '—' }}</span>
              <Tag v-if="user.codigoSunat" :value="user.codigoSunat" severity="info" />
            </div>
          </template>
        </Column>

        <Column header="Estado" style="width: 110px">
          <template #body="{ data: user }">
            <Tag
              :value="user.estadoActivo ? 'Activo' : 'Inactivo'"
              :severity="user.estadoActivo ? 'success' : 'danger'"
              rounded
            />
          </template>
        </Column>

        <Column header="Acciones" style="width: 240px">
          <template #body="{ data: user }">
            <div class="usr-actions">
              <Button
                label="Editar"
                severity="secondary"
                outlined
                size="small"
                @click="$emit('edit', user)"
              />
              <Button
                icon="pi pi-key"
                severity="secondary"
                outlined
                size="small"
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
          </template>
        </Column>
      </DataTable>
    </div>

    <div v-else class="sga-canvas-grid">
      <article v-for="user in paginatedUsers" :key="user.idUsuario" class="sga-canvas-card">
        <div class="sga-card-top">
          <span class="sga-card-code sga-mono">@{{ user.username }}</span>
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
            <span v-if="user.codigoSunat" class="sga-client-doc sga-mono">{{ user.codigoSunat }}</span>
          </div>
        </div>
        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">SEDE</span>
            <span class="sga-meta-val">{{ user.codigoSede || '—' }} · {{ user.nombreSede || 'Sin sede' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">CARGO</span>
            <span class="sga-meta-val">{{ user.cargoTitulo || '—' }}</span>
          </div>
          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">CORREO</span>
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
  </SgaListShell>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
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

const viewMode = ref('table')
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
} = usePagination(filteredUsers, 15)

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
.usr-info-cell {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.usr-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  flex-shrink: 0;
}

.usr-stack {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.usr-name-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.usr-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}
</style>
