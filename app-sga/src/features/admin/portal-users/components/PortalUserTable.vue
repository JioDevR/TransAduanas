<template>
  <SgaListShell v-model:viewMode="viewMode" title="Cuentas Corporativas B2B">
    <template #filters>
      <div class="sga-filter-search">
        <InputText
          v-model="searchQuery"
          placeholder="Buscar por usuario, representante, correo, RUC o empresa..."
          size="small"
          fluid
        />
      </div>

      <Select
        v-model="selectedClienteFilter"
        :options="clienteOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        style="min-width: 220px"
      />

      <Select
        v-model="statusFilter"
        :options="statusOptions"
        optionLabel="label"
        optionValue="value"
        size="small"
        style="min-width: 170px"
      />
    </template>

    <template #actions>
      <Tag :value="`${filteredUsers.length} cuentas`" severity="secondary" />
    </template>

    <div v-if="viewMode === 'table'">
      <DataTable
        :value="paginatedUsers"
        :loading="loading"
        size="small"
        stripedRows
        responsiveLayout="scroll"
        dataKey="idUsuarioCliente"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-users sga-empty-icon"></i>
            <p class="sga-empty-title">No se encontraron usuarios del portal</p>
            <p class="sga-empty-desc">Prueba ajustando los filtros o aprovisionando un nuevo usuario B2B.</p>
          </div>
        </template>

        <Column header="Empresa / RUC">
          <template #body="{ data: user }">
            <div class="ptl-stack">
              <strong>{{ user.razonSocialCliente || '—' }}</strong>
              <small class="sga-mono sga-text-muted">{{ user.tipoDocumentoCliente || 'RUC' }}: {{ user.rucCliente }}</small>
            </div>
          </template>
        </Column>

        <Column header="Usuario / Representante">
          <template #body="{ data: user }">
            <div class="ptl-info-cell">
              <div class="ptl-avatar" :style="getAvatarBg(user.username)">
                {{ getInitials(user.nombres, user.apellidos) }}
              </div>
              <div class="ptl-stack">
                <strong class="sga-mono">@{{ user.username }}</strong>
                <span>{{ user.contactoRepresentante || user.nombres || 'Representante Legal' }}</span>
              </div>
            </div>
          </template>
        </Column>

        <Column header="Contacto">
          <template #body="{ data: user }">
            <div class="ptl-stack">
              <span class="sga-mono">{{ user.correoElectronico }}</span>
              <small v-if="user.telefonoContacto" class="sga-mono sga-text-muted">{{ user.telefonoContacto }}</small>
            </div>
          </template>
        </Column>

        <Column header="Último Acceso" style="width: 150px">
          <template #body="{ data: user }">
            <span v-if="user.ultimoAcceso" class="sga-mono">{{ formatAccessDate(user.ultimoAcceso) }}</span>
            <Tag v-else value="Sin accesos" severity="secondary" />
          </template>
        </Column>

        <Column header="Estado" style="width: 115px">
          <template #body="{ data: user }">
            <Tag
              :value="user.estadoActivo ? 'Activo' : 'Suspendido'"
              :severity="user.estadoActivo ? 'success' : 'danger'"
              rounded
            />
          </template>
        </Column>

        <Column header="Acciones" style="width: 280px">
          <template #body="{ data: user }">
            <div class="ptl-actions">
              <Button label="Editar" size="small" outlined severity="secondary" @click="$emit('edit', user)" />
              <Button label="Auditar" size="small" outlined severity="secondary" @click="$emit('audit', user)" />
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
          </template>
        </Column>
      </DataTable>
    </div>

    <div v-else class="sga-canvas-grid">
      <article v-for="user in paginatedUsers" :key="user.idUsuarioCliente" class="sga-canvas-card">
        <div class="sga-card-top">
          <span class="sga-card-code sga-mono">@{{ user.username }}</span>
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
            <span class="sga-meta-val sga-mono">{{ user.ultimoAcceso ? formatAccessDate(user.ultimoAcceso) : 'Sin accesos' }}</span>
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
      item-label="usuarios del portal"
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

const props = defineProps({
  users: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['edit', 'audit', 'reset-password', 'toggle-status'])

const viewMode = ref('table')
const searchQuery = ref('')
const selectedClienteFilter = ref('')
const statusFilter = ref('ALL')

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
    { label: 'Todas las Empresas', value: '' },
    ...Array.from(map.values()).sort((a, b) => a.label.localeCompare(b.label))
  ]
})

const statusOptions = [
  { label: 'Todos los Estados', value: 'ALL' },
  { label: 'Solo Activos', value: 'ACTIVE' },
  { label: 'Solo Suspendidos', value: 'INACTIVE' }
]

const filteredUsers = computed(() =>
  props.users.filter((user) => {
    if (statusFilter.value === 'ACTIVE' && !user.estadoActivo) return false
    if (statusFilter.value === 'INACTIVE' && user.estadoActivo) return false
    if (selectedClienteFilter.value && user.idCliente !== Number(selectedClienteFilter.value)) return false
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
)

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
  return { background: `hsl(${hue}, 45%, 38%)`, color: '#ffffff' }
}

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
.ptl-info-cell {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.ptl-avatar {
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

.ptl-stack {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.ptl-actions {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}
</style>
