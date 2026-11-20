<template>
  <section class="sga-list-panel">
    <!-- Barra superior sgaUI: conteo a la izquierda, acciones a la derecha -->
    <div class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag :value="`${filteredUsers.length} de ${users.length} cuentas B2B`" severity="secondary" />
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
          label="Nuevo Usuario Portal"
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
              placeholder="Buscar usuario, empresa, RUC o correo..."
              size="small"
              fluid
            />
          </IconField>

          <Select
            v-model="selectedClienteFilter"
            :options="clienteOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Todas las Empresas"
            filter
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
            v-model="securityFilter"
            :options="securityOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Acceso: Todos"
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
        <p class="sga-empty-desc">Cargando cuentas corporativas B2B...</p>
      </div>

      <div v-else-if="paginatedUsers.length === 0" class="sga-empty-state">
        <i class="pi pi-users sga-empty-icon"></i>
        <p class="sga-empty-title">No se encontraron usuarios B2B</p>
        <p class="sga-empty-desc">Prueba ajustando los filtros o aprovisionando un nuevo usuario B2B.</p>
      </div>

      <div v-else class="sga-canvas-grid">
        <article v-for="user in paginatedUsers" :key="user.idUsuarioCliente" class="sga-canvas-card">
          <div class="sga-card-top">
            <div class="ptl-card-identity">
              <div class="ptl-avatar" :style="getWarmAvatarStyle(user.username)">
                {{ getInitials(user.nombres, user.apellidos, 'UC') }}
              </div>
              <span class="sga-card-code sga-mono">@{{ user.username }}</span>
            </div>
            <Tag
              :value="user.estadoActivo ? 'Activo' : 'Suspendido'"
              :severity="user.estadoActivo ? 'success' : 'danger'"
              rounded
            />
          </div>

          <div class="sga-card-client">
            <h4 class="sga-client-title">{{ user.razonSocialCliente || '—' }}</h4>
            <span class="sga-client-doc sga-mono">{{ user.tipoDocumentoCliente || 'RUC' }}: {{ user.rucCliente }}</span>
          </div>

          <div class="sga-card-meta-grid">
            <div class="sga-meta-item">
              <span class="sga-meta-label">REPRESENTANTE</span>
              <span class="sga-meta-val">{{ user.contactoRepresentante || user.nombres || '—' }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">ÚLTIMO ACCESO</span>
              <span class="sga-meta-val sga-mono">
                {{ user.ultimoAcceso ? formatAccessDate(user.ultimoAcceso) : 'Sin accesos' }}
              </span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">TELÉFONO</span>
              <span class="sga-meta-val sga-mono">{{ user.telefonoContacto || '—' }}</span>
            </div>
            <div class="sga-meta-item">
              <span class="sga-meta-label">CLAVE</span>
              <span class="sga-meta-val">{{ user.debeCambiarPassword ? 'Cambio pendiente' : 'Vigente' }}</span>
            </div>
            <div class="sga-meta-item full-span">
              <span class="sga-meta-label">CORREO</span>
              <span class="sga-meta-val sga-mono">{{ user.correoElectronico }}</span>
            </div>
          </div>

          <div class="sga-card-footer">
            <div class="ptl-actions">
              <Button label="Editar" size="small" outlined severity="secondary" @click="$emit('edit', user)" />
              <Button label="Auditar" size="small" outlined severity="secondary" @click="$emit('audit', user)" />
            </div>
            <div class="ptl-actions">
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
        item-label="cuentas B2B"
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
import { getInitials, getWarmAvatarStyle } from '../../utils/adminFormatters'

const props = defineProps({
  users: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['edit', 'audit', 'reset-password', 'toggle-status', 'refresh', 'create'])

const searchQuery = ref('')
const selectedClienteFilter = ref('ALL')
const statusFilter = ref('ALL')
const securityFilter = ref('ALL')
const sortBy = ref('RECENTS')

const clienteOptions = computed(() => {
  const map = new Map()
  props.users.forEach((u) => {
    if (u.idCliente && !map.has(u.idCliente)) {
      map.set(u.idCliente, {
        label: `${u.razonSocialCliente} (${u.rucCliente})`,
        value: String(u.idCliente)
      })
    }
  })
  return [
    { label: 'Todas las Empresas', value: 'ALL' },
    ...Array.from(map.values()).sort((a, b) => a.label.localeCompare(b.label))
  ]
})

const statusOptions = [
  { label: 'Todos los Estados', value: 'ALL' },
  { label: 'Solo Activos', value: 'ACTIVE' },
  { label: 'Solo Suspendidos', value: 'INACTIVE' }
]

const securityOptions = [
  { label: 'Acceso: Todos', value: 'ALL' },
  { label: 'Clave pendiente de cambio', value: 'PASS_PENDING' },
  { label: 'Sin accesos aún', value: 'NO_ACCESS' },
  { label: 'Con acceso registrado', value: 'WITH_ACCESS' }
]

const sortOptions = [
  { label: 'Orden: Más recientes', value: 'RECENTS' },
  { label: 'Orden: Empresa A–Z', value: 'EMPRESA_ASC' },
  { label: 'Orden: Usuario A–Z', value: 'USER_ASC' },
  { label: 'Orden: Último acceso', value: 'ACCESS_DESC' }
]

const hasActiveFilters = computed(() =>
  Boolean(
    searchQuery.value ||
      selectedClienteFilter.value !== 'ALL' ||
      statusFilter.value !== 'ALL' ||
      securityFilter.value !== 'ALL' ||
      sortBy.value !== 'RECENTS'
  )
)

const resetFilters = () => {
  searchQuery.value = ''
  selectedClienteFilter.value = 'ALL'
  statusFilter.value = 'ALL'
  securityFilter.value = 'ALL'
  sortBy.value = 'RECENTS'
}

const filteredUsers = computed(() => {
  const list = props.users.filter((user) => {
    if (statusFilter.value === 'ACTIVE' && !user.estadoActivo) return false
    if (statusFilter.value === 'INACTIVE' && user.estadoActivo) return false
    if (selectedClienteFilter.value !== 'ALL' && user.idCliente !== Number(selectedClienteFilter.value)) return false

    if (securityFilter.value === 'PASS_PENDING' && !user.debeCambiarPassword) return false
    if (securityFilter.value === 'NO_ACCESS' && user.ultimoAcceso) return false
    if (securityFilter.value === 'WITH_ACCESS' && !user.ultimoAcceso) return false

    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase().trim()
      return (
        (user.username || '').toLowerCase().includes(q) ||
        (user.contactoRepresentante || user.nombres || '').toLowerCase().includes(q) ||
        (user.correoElectronico || '').toLowerCase().includes(q) ||
        (user.rucCliente || '').toLowerCase().includes(q) ||
        (user.razonSocialCliente || '').toLowerCase().includes(q)
      )
    }
    return true
  })

  return [...list].sort((a, b) => {
    if (sortBy.value === 'EMPRESA_ASC') {
      return (a.razonSocialCliente || '').localeCompare(b.razonSocialCliente || '', 'es')
    }
    if (sortBy.value === 'USER_ASC') {
      return (a.username || '').localeCompare(b.username || '', 'es')
    }
    if (sortBy.value === 'ACCESS_DESC') {
      const ta = a.ultimoAcceso ? new Date(a.ultimoAcceso).getTime() : 0
      const tb = b.ultimoAcceso ? new Date(b.ultimoAcceso).getTime() : 0
      return tb - ta
    }
    return (b.idUsuarioCliente || 0) - (a.idUsuarioCliente || 0)
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
} = usePagination(filteredUsers, 12)

watch([searchQuery, selectedClienteFilter, statusFilter, securityFilter, sortBy], () => {
  resetPage()
})

const formatAccessDate = (val) => {
  if (!val) return '—'
  return new Date(val).toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.ptl-card-identity {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.ptl-avatar {
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

.ptl-actions {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}
</style>
