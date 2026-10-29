<template>
  <div class="gerencia-conductores-view">
    <!-- 1. Toolbar de Acciones Warm Editorial (Sin Banners Invasivos) -->
    <div class="conductores-action-toolbar">
      <div class="toolbar-left-info">
        <span class="badge-kicker">PLANTILLA DE CONDUCTORES // TRANSPORTE MTC</span>
        <h3 class="toolbar-title">Padrón de Choferes Profesionales</h3>
      </div>

      <div class="toolbar-actions-group">
        <span class="count-badge font-mono">{{ filteredConductores.length }} de {{ conductoresList.length }} choferes</span>

        <button
          type="button"
          class="btn-outline-warm"
          @click="loadConductores"
          :disabled="loading"
          title="Recargar padrón de conductores"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>

        <button
          type="button"
          class="btn-espresso"
          @click="openCreateModal"
        >
          <i class="pi pi-user-plus"></i>
          <span>+ Ingresar Conductor</span>
        </button>
      </div>
    </div>

    <!-- 2. Grid de Métricas KPI Técnicas (Estándar Editorial Warm) -->
    <div class="metrics-kpi-grid">
      <div class="kpi-card success-card" @click="filterCondicion = 'DISPONIBLE'">
        <div class="kpi-card-header">
          <span class="kpi-num">01</span>
          <span class="kpi-label">Conductores Disponibles</span>
          <i class="pi pi-check-circle kpi-icon success"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono text-success">{{ conductoresDisponiblesCount }}</span>
          <span class="kpi-badge success">En Espera</span>
        </div>
        <div class="kpi-footer-note">
          <span>Listos para asignación de ruta</span>
        </div>
      </div>

      <div class="kpi-card" @click="filterCondicion = 'EN_RUTA'">
        <div class="kpi-card-header">
          <span class="kpi-num">02</span>
          <span class="kpi-label">Choferes en Ruta</span>
          <i class="pi pi-compass kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ conductoresEnRutaCount }}</span>
          <span class="kpi-badge info">En Tránsito</span>
        </div>
        <div class="kpi-footer-note">
          <span>Despachos aduaneros activos</span>
        </div>
      </div>

      <div class="kpi-card" @click="filterCategoria = 'A-IIIc'">
        <div class="kpi-card-header">
          <span class="kpi-num">03</span>
          <span class="kpi-label">Licencias A-IIIc</span>
          <i class="pi pi-id-card kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ conductoresA3cCount }}</span>
          <span class="kpi-badge accent">Tractocamión</span>
        </div>
        <div class="kpi-footer-note">
          <span>Habilitación para carga pesada</span>
        </div>
      </div>

      <div class="kpi-card" @click="filterCondicion = ''; filterCategoria = ''">
        <div class="kpi-card-header">
          <span class="kpi-num">04</span>
          <span class="kpi-label">Padrón Acreditado</span>
          <i class="pi pi-users kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ conductoresList.length }}</span>
          <span class="kpi-badge warning">Total Padrón</span>
        </div>
        <div class="kpi-footer-note">
          <span>Personal acreditado por el MTC</span>
        </div>
      </div>
    </div>

    <!-- 3. Alertas Preventivas de Licencias MTC -->
    <div v-if="alertasLicencias.length > 0" class="alerts-container">
      <div v-for="alerta in alertasLicencias" :key="alerta.id" class="alert-item" :class="alerta.tipo">
        <i class="pi pi-exclamation-triangle alert-icon"></i>
        <div class="alert-content">
          <strong>Vigencia de Licencia MTC: {{ alerta.conductor }}</strong>
          <span>{{ alerta.mensaje }}</span>
        </div>
      </div>
    </div>

    <!-- 4. Padrón de Conductores y Tabla -->
    <div class="table-card">
      <div class="table-toolbar">
        <div class="search-input-wrapper">
          <i class="pi pi-search search-icon"></i>
          <input
            v-model="searchQuery"
            type="text"
            class="table-search-input"
            placeholder="Buscar por DNI, nombre o licencia..."
          />
          <button
            v-if="searchQuery"
            type="button"
            class="clear-search-btn"
            @click="searchQuery = ''"
            title="Limpiar búsqueda"
          >
            <i class="pi pi-times"></i>
          </button>
        </div>

        <div class="filter-group">
          <!-- Filtro Categoría -->
          <select v-model="filterCategoria" class="filter-select">
            <option value="">Todas las Categorías</option>
            <option value="A-IIIc">Cat. A-IIIc (Tractocamión / Articulado)</option>
            <option value="A-IIIb">Cat. A-IIIb (Remolcador / Rígido)</option>
            <option value="A-IIIa">Cat. A-IIIa (Pesado)</option>
            <option value="A-IIb">Cat. A-IIb</option>
          </select>

          <!-- Filtro Operativo -->
          <select v-model="filterCondicion" class="filter-select">
            <option value="">Cualquier Condición</option>
            <option value="DISPONIBLE">Disponible</option>
            <option value="EN_RUTA">En Ruta</option>
            <option value="DESCANSO">En Descanso</option>
            <option value="SUSPENDIDO">Suspendido</option>
          </select>
        </div>
      </div>

      <!-- Tabla Conductores -->
      <div class="data-table-container">
        <DataTable
          :value="filteredConductores"
          :loading="loading"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idConductor"
          :paginator="filteredConductores.length > 15"
          :rows="15"
          :rowsPerPageOptions="[15, 30, 50]"
        >
          <template #empty>
            <div class="empty-state-box">
              <i class="pi pi-users empty-icon"></i>
              <p class="empty-title">No hay conductores registrados</p>
              <p class="empty-desc">Registre los choferes profesionales con licencia MTC vigente para asignar despachos.</p>
            </div>
          </template>

          <Column header="Conductor">
            <template #body="{ data: c }">
              <div class="driver-name-cell">
                <span class="driver-name">{{ c.nombres }} {{ c.apellidos }}</span>
                <span class="driver-dni font-mono">DNI: {{ c.numeroDocumento }}</span>
              </div>
            </template>
          </Column>

          <Column header="Licencia MTC">
            <template #body="{ data: c }">
              <div class="license-cell">
                <span class="license-num font-mono font-bold">{{ c.numeroLicencia }}</span>
                <span class="license-cat">{{ c.categoriaLicencia || 'A-IIIc' }}</span>
              </div>
            </template>
          </Column>

          <Column header="Vencimiento Licencia">
            <template #body="{ data: c }">
              <span :class="['doc-date font-mono', getLicenseDateClass(c.fechaVencimientoLicencia)]">
                {{ formatDate(c.fechaVencimientoLicencia) }}
              </span>
            </template>
          </Column>

          <Column header="Teléfono Móvil">
            <template #body="{ data: c }">
              <div class="phone-cell">
                <i class="pi pi-phone text-subtle"></i>
                <span class="font-mono text-xs">{{ c.telefono || '— Sin telf —' }}</span>
              </div>
            </template>
          </Column>

          <Column header="Cuenta SGA Vinculada">
            <template #body="{ data: c }">
              <div v-if="c.idUsuario" class="user-linked-cell">
                <Tag severity="info" value="SGA Habilitado" rounded class="tag-xs" />
                <span class="user-alias font-mono">{{ c.usuarioUsername || ('User #' + c.idUsuario) }}</span>
              </div>
              <div v-else>
                <Tag severity="secondary" value="Sin Acceso App" rounded class="tag-xs" />
              </div>
            </template>
          </Column>

          <Column header="Operatividad">
            <template #body="{ data: c }">
              <Tag
                :severity="getCondicionSeverity(c.estadoOperativo)"
                :value="c.estadoOperativo || 'DISPONIBLE'"
                rounded
                class="tag-xs"
              />
            </template>
          </Column>

          <Column header="Estado">
            <template #body="{ data: c }">
              <Tag
                :severity="c.estadoActivo ? 'success' : 'danger'"
                :value="c.estadoActivo ? 'ACTIVO' : 'INACTIVO'"
                rounded
                class="tag-xs"
              />
            </template>
          </Column>

          <Column header="Acciones" headerStyle="width: 110px; text-align: right;" bodyStyle="text-align: right;">
            <template #body="{ data: c }">
              <div class="action-buttons-cell">
                <button
                  type="button"
                  class="action-btn edit-action"
                  title="Editar Conductor"
                  @click="openEditModal(c)"
                >
                  <i class="pi pi-pencil"></i>
                </button>

                <button
                  v-if="c.estadoActivo"
                  type="button"
                  class="action-btn delete-action"
                  title="Dar de Baja Conductor"
                  @click="toggleEstado(c)"
                >
                  <i class="pi pi-trash"></i>
                </button>
                <button
                  v-else
                  type="button"
                  class="action-btn reactivate-action"
                  title="Reactivar Conductor"
                  @click="toggleEstado(c)"
                >
                  <i class="pi pi-refresh"></i>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <!-- MODAL: CREAR / EDITAR CONDUCTOR -->
    <Dialog
      v-model:visible="showModal"
      modal
      :header="isEditing ? 'EDITAR CONDUCTOR // PADRÓN MTC' : 'NUEVO CONDUCTOR // ALTA PROFESIONAL MTC'"
      :style="{ width: '640px' }"
      class="custom-modal-editorial"
    >
      <form @submit.prevent="saveConductor" class="modal-form">
        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Nombres *</label>
            <input
              v-model="form.nombres"
              type="text"
              class="form-input"
              placeholder="Ej: Juan Carlos"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">Apellidos *</label>
            <input
              v-model="form.apellidos"
              type="text"
              class="form-input"
              placeholder="Ej: Pérez Quispe"
              required
            />
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">N° Documento (DNI) *</label>
            <input
              v-model="form.numeroDocumento"
              type="text"
              class="form-input font-mono font-bold"
              placeholder="8 dígitos"
              maxlength="15"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">Teléfono Móvil *</label>
            <input
              v-model="form.telefono"
              type="text"
              class="form-input font-mono"
              placeholder="+51 987 654 321"
              required
            />
          </div>
        </div>

        <div class="form-grid-3">
          <div class="form-group">
            <label class="form-label">N° Licencia MTC *</label>
            <input
              v-model="form.numeroLicencia"
              type="text"
              class="form-input text-uppercase font-mono font-bold"
              placeholder="Q-12345678"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">Categoría MTC *</label>
            <select v-model="form.categoriaLicencia" class="form-select" required>
              <option value="A-IIIc">A-IIIc (Tractocamión)</option>
              <option value="A-IIIb">A-IIIb (Remolcador)</option>
              <option value="A-IIIa">A-IIIa (Carga Pesada)</option>
              <option value="A-IIb">A-IIb</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Vencimiento Licencia *</label>
            <input
              v-model="form.fechaVencimientoLicencia"
              type="date"
              class="form-input font-mono"
              required
            />
          </div>
        </div>

        <!-- Vinculación de Usuario SGA -->
        <div class="form-grid-2 user-link-box">
          <div class="form-group">
            <label class="form-label">Cuenta de Acceso SGA (Para App Chofer)</label>
            <select v-model="form.idUsuario" class="form-select font-mono">
              <option :value="null">— Sin cuenta vinculada —</option>
              <option
                v-for="u in usuariosDisponibles"
                :key="u.idUsuario"
                :value="u.idUsuario"
              >
                {{ u.username }} ({{ u.nombres }} {{ u.apellidos }})
              </option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Estado Operativo</label>
            <select v-model="form.estadoOperativo" class="form-select">
              <option value="DISPONIBLE">Disponible</option>
              <option value="EN_RUTA">En Ruta</option>
              <option value="DESCANSO">En Descanso</option>
              <option value="SUSPENDIDO">Suspendido</option>
            </select>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Observaciones y Acreditaciones</label>
          <textarea
            v-model="form.observaciones"
            rows="2"
            class="form-textarea"
            placeholder="Acreditación portuaria APN, cursos de mercancías peligrosas MATPEL..."
          ></textarea>
        </div>

        <div class="modal-footer-actions">
          <button type="button" class="btn-outline-warm" @click="showModal = false" :disabled="saving">
            Cancelar
          </button>
          <button type="submit" class="btn-espresso" :disabled="saving">
            <i v-if="saving" class="pi pi-spin pi-spinner"></i>
            <span>{{ isEditing ? 'Guardar Cambios' : 'Registrar Conductor' }}</span>
          </button>
        </div>
      </form>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import { transporteFlotaService } from '@/features/transporte/services/transporteFlotaService'
import { userService } from '@/features/admin/users/services/userService'
import { useNotification } from '@/shared/composables/useNotification'

const { showSuccess, showError, showInfo } = useNotification()

const conductoresList = ref([])
const usuariosList = ref([])
const loading = ref(false)
const saving = ref(false)
const showModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)

const filterCategoria = ref('')
const filterCondicion = ref('')
const searchQuery = ref('')

const form = ref({
  nombres: '',
  apellidos: '',
  numeroDocumento: '',
  numeroLicencia: '',
  categoriaLicencia: 'A-IIIc',
  fechaVencimientoLicencia: '',
  telefono: '',
  idUsuario: null,
  estadoOperativo: 'DISPONIBLE',
  observaciones: ''
})

const conductoresDisponiblesCount = computed(() =>
  conductoresList.value.filter(c => c.estadoActivo && c.estadoOperativo === 'DISPONIBLE').length
)
const conductoresEnRutaCount = computed(() =>
  conductoresList.value.filter(c => c.estadoOperativo === 'EN_RUTA').length
)
const conductoresA3cCount = computed(() =>
  conductoresList.value.filter(c => c.categoriaLicencia === 'A-IIIc').length
)

const alertasLicencias = computed(() => {
  const alerts = []
  const today = new Date()
  const in30Days = new Date()
  in30Days.setDate(today.getDate() + 30)

  conductoresList.value.forEach(c => {
    if (!c.estadoActivo || !c.fechaVencimientoLicencia) return

    const licDate = new Date(c.fechaVencimientoLicencia)
    if (licDate < today) {
      alerts.push({
        id: `lic-exp-${c.idConductor}`,
        conductor: `${c.nombres} ${c.apellidos}`,
        tipo: 'danger',
        mensaje: `Licencia ${c.numeroLicencia} (${c.categoriaLicencia}) vencida el ${formatDate(c.fechaVencimientoLicencia)}.`
      })
    } else if (licDate <= in30Days) {
      alerts.push({
        id: `lic-warn-${c.idConductor}`,
        conductor: `${c.nombres} ${c.apellidos}`,
        tipo: 'warning',
        mensaje: `Licencia ${c.numeroLicencia} próxima a vencer el ${formatDate(c.fechaVencimientoLicencia)}.`
      })
    }
  })

  return alerts.slice(0, 3)
})

const usuariosDisponibles = computed(() => {
  return usuariosList.value.filter(u => u.estadoActivo)
})

const filteredConductores = computed(() => {
  return conductoresList.value.filter(c => {
    const matchCat = !filterCategoria.value || c.categoriaLicencia === filterCategoria.value
    const matchCond = !filterCondicion.value || c.estadoOperativo === filterCondicion.value
    const query = searchQuery.value.trim().toLowerCase()
    const matchSearch =
      !query ||
      (c.nombres && c.nombres.toLowerCase().includes(query)) ||
      (c.apellidos && c.apellidos.toLowerCase().includes(query)) ||
      (c.numeroDocumento && c.numeroDocumento.includes(query)) ||
      (c.numeroLicencia && c.numeroLicencia.toLowerCase().includes(query))

    return matchCat && matchCond && matchSearch
  })
})

const loadConductores = async () => {
  loading.value = true
  try {
    const data = await transporteFlotaService.getConductores(false)
    conductoresList.value = Array.isArray(data) ? data : []
  } catch (err) {
    showError(err.message || 'Error al conectar con los conductores')
  } finally {
    loading.value = false
  }
}

const loadUsuarios = async () => {
  try {
    const data = await userService.getUsers()
    usuariosList.value = Array.isArray(data) ? data : []
  } catch {
    // Silencioso
  }
}

const openCreateModal = () => {
  isEditing.value = false
  editingId.value = null
  form.value = {
    nombres: '',
    apellidos: '',
    numeroDocumento: '',
    numeroLicencia: '',
    categoriaLicencia: 'A-IIIc',
    fechaVencimientoLicencia: '',
    telefono: '',
    idUsuario: null,
    estadoOperativo: 'DISPONIBLE',
    observaciones: ''
  }
  showModal.value = true
}

const openEditModal = (c) => {
  isEditing.value = true
  editingId.value = c.idConductor
  form.value = {
    nombres: c.nombres || '',
    apellidos: c.apellidos || '',
    numeroDocumento: c.numeroDocumento || '',
    numeroLicencia: c.numeroLicencia || '',
    categoriaLicencia: c.categoriaLicencia || 'A-IIIc',
    fechaVencimientoLicencia: c.fechaVencimientoLicencia ? c.fechaVencimientoLicencia.substring(0, 10) : '',
    telefono: c.telefono || '',
    idUsuario: c.idUsuario || null,
    estadoOperativo: c.estadoOperativo || 'DISPONIBLE',
    observaciones: c.observaciones || ''
  }
  showModal.value = true
}

const saveConductor = async () => {
  saving.value = true
  try {
    const payload = { ...form.value }
    if (!payload.telefono) delete payload.telefono
    if (!payload.idUsuario) delete payload.idUsuario

    if (isEditing.value) {
      await transporteFlotaService.updateConductor(editingId.value, payload)
      showSuccess(`Conductor ${payload.nombres} actualizado exitosamente`)
    } else {
      await transporteFlotaService.createConductor(payload)
      showSuccess(`Conductor ${payload.nombres} registrado en el padrón`)
    }
    showModal.value = false
    await loadConductores()
  } catch (err) {
    showError(err.message || 'Error al procesar conductor')
  } finally {
    saving.value = false
  }
}

const toggleEstado = async (c) => {
  try {
    if (c.estadoActivo) {
      await transporteFlotaService.deleteConductor(c.idConductor)
      showInfo(`Conductor ${c.nombres} desactivado del padrón`)
    } else {
      await transporteFlotaService.reactivarConductor(c.idConductor)
      showSuccess(`Conductor ${c.nombres} reactivado`)
    }
    await loadConductores()
  } catch (err) {
    showError(err.message || 'Error al cambiar estado')
  }
}

const formatDate = (d) => {
  if (!d) return '— No registrado —'
  const date = new Date(d)
  return isNaN(date.getTime()) ? d : date.toLocaleDateString('es-PE')
}

const getLicenseDateClass = (d) => {
  if (!d) return 'text-muted'
  const date = new Date(d)
  const today = new Date()
  const in30Days = new Date()
  in30Days.setDate(today.getDate() + 30)

  if (date < today) return 'text-danger'
  if (date <= in30Days) return 'text-warning'
  return 'text-success'
}

const getCondicionSeverity = (c) => {
  switch (c) {
    case 'DISPONIBLE': return 'success'
    case 'EN_RUTA': return 'info'
    case 'DESCANSO': return 'warning'
    case 'SUSPENDIDO': return 'danger'
    default: return 'secondary'
  }
}

onMounted(() => {
  loadConductores()
  loadUsuarios()
})
</script>

<style scoped>
.gerencia-conductores-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR EDITORIAL
   ========================================================================== */
.conductores-action-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-4, 1rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle);
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.badge-kicker {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  text-transform: uppercase;
}

.toolbar-title {
  margin: 0;
  font-size: var(--font-size-lg, 1.125rem);
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.015em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.count-badge {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-pill, 9999px);
}

/* ==========================================================================
   2. REJILLA DE TARJETAS KPI (WARM EDITORIAL)
   ========================================================================== */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-3, 0.75rem);
  cursor: pointer;
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.kpi-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-accent-kicker);
  box-shadow: var(--shadow-elevated);
}

.kpi-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kpi-num {
  font-family: var(--font-mono, monospace);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}

.kpi-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-right: auto;
  margin-left: 0.65rem;
}

.kpi-icon {
  font-size: 1.1rem;
  color: var(--color-text-subtle);
}

.kpi-icon.success { color: var(--color-success-text); }
.kpi-icon.warning { color: var(--color-warning-text); }

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.kpi-badge {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kpi-badge.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  border: 1px solid var(--color-border-subtle);
}

.kpi-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.kpi-badge.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.kpi-badge.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.kpi-footer-note {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-subtle);
  border-top: 1px dashed var(--color-border-subtle);
  padding-top: var(--space-2, 0.5rem);
}

/* ==========================================================================
   3. ALERTAS PREVENTIVAS
   ========================================================================== */
.alerts-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.15rem;
  border-radius: var(--radius-md, 8px);
  font-size: var(--font-size-sm, 0.8125rem);
}

.alert-item.warning {
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  color: var(--color-warning-text);
}

.alert-item.danger {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger-text);
}

.alert-icon {
  font-size: 1.15rem;
}

.alert-content {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

/* ==========================================================================
   4. TABLA Y TOOLBAR DE FILTROS
   ========================================================================== */
.table-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.85rem;
  padding: 1rem 1.25rem;
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border-subtle);
}

.search-input-wrapper {
  position: relative;
  flex: 1;
  min-width: 260px;
}

.search-icon {
  position: absolute;
  left: 0.85rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-subtle);
  font-size: 0.85rem;
}

.table-search-input {
  width: 100%;
  padding: 0.55rem 2.25rem 0.55rem 2.15rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  font-size: var(--font-size-sm, 0.8125rem);
  outline: none;
  transition: border-color var(--transition-fast);
}

.table-search-input:focus {
  border-color: var(--color-primary);
}

.clear-search-btn {
  position: absolute;
  right: 0.65rem;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.25rem;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.filter-select {
  padding: 0.55rem 2.25rem 0.55rem 0.85rem;
  border: 1px solid var(--color-border);
  background-color: var(--color-bg-surface);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 500;
  cursor: pointer;
  outline: none;
  appearance: none !important;
  background-image: url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%236f6356%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E");
  background-repeat: no-repeat !important;
  background-position: right 0.75rem center !important;
  background-size: 0.65rem auto !important;
}

.filter-select:focus {
  border-color: var(--color-primary);
}

.driver-name-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.driver-name {
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--color-text-main);
}

.driver-dni {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
}

.license-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.license-num {
  font-size: 0.8rem;
  color: var(--color-text-main);
}

.license-cat {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
}

.phone-cell {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.user-linked-cell {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  align-items: flex-start;
}

.user-alias {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
}

.action-buttons-cell {
  display: inline-flex;
  gap: 0.4rem;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm, 6px);
  cursor: pointer;
  border: 1px solid transparent;
  transition: all var(--transition-fast);
}

.edit-action {
  background: var(--color-bg-subtle);
  border-color: var(--color-border);
  color: var(--color-text-main);
}

.edit-action:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.delete-action {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.delete-action:hover {
  filter: brightness(1.1);
}

.reactivate-action {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.reactivate-action:hover {
  filter: brightness(1.1);
}

.empty-state-box {
  padding: 3rem 1rem;
  text-align: center;
  color: var(--color-text-subtle);
}

.empty-icon {
  font-size: 2.25rem;
  margin-bottom: 0.75rem;
  color: var(--color-accent-kicker);
}

.empty-title {
  font-weight: 700;
  font-size: 0.95rem;
  color: var(--color-text-main);
  margin: 0 0 0.25rem;
}

.empty-desc {
  font-size: 0.825rem;
  margin: 0;
}

/* Modales */
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-top: 0.5rem;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.85rem;
}

.form-grid-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.85rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.form-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.form-input, .form-select, .form-textarea {
  padding: 0.55rem 0.75rem;
  border: 1px solid var(--color-border);
  background-color: var(--color-bg-surface);
  color: var(--color-text-main);
  border-radius: var(--radius-md, 8px);
  font-size: var(--font-size-sm, 0.8125rem);
  outline: none;
}

.form-input:focus, .form-select:focus, .form-textarea:focus {
  border-color: var(--color-primary);
}

.text-uppercase {
  text-transform: uppercase;
}

.user-link-box {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md, 8px);
  padding: 0.75rem;
}

.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
  margin-top: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border-subtle);
}

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
  transition: all var(--transition-fast);
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
  transition: all var(--transition-fast);
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.text-success { color: var(--color-success-text) !important; }
.text-warning { color: var(--color-warning-text) !important; }
.text-danger  { color: var(--color-danger-text) !important; }
.text-muted   { color: var(--color-text-muted) !important; }
.text-subtle  { color: var(--color-text-subtle) !important; }

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
  .form-grid-2,
  .form-grid-3 {
    grid-template-columns: 1fr;
  }
}
</style>
