<template>
  <div class="gerencia-conductores-view">
    <!-- Hero Banner Card -->
    <HeroBannerCard
      kicker="GERENCIA GENERAL // PLANTILLA DE CONDUCTORES MTC"
      title="Padrón de Choferes Profesionales y Asignación de Accesos"
      description="Alta y control de choferes autorizados por el MTC, control de vigencia de licencias especiales (A-IIIb / A-IIIc) y vinculación con cuentas de acceso al SGA para la terminal móvil de ruta."
    />

    <!-- Métricas de Conductores -->
    <div class="metrics-grid">
      <MetricCard index="01" label="Conductores Disponibles" :value="conductoresDisponiblesCount" severity="success" highlight />
      <MetricCard index="02" label="Choferes en Ruta" :value="conductoresEnRutaCount" severity="pending" />
      <MetricCard index="03" label="Licencias A-IIIc" :value="conductoresA3cCount" severity="accent" />
      <MetricCard index="04" label="Total Padrón Activo" :value="conductoresList.length" severity="accent" />
    </div>

    <!-- Alertas Preventivas de Licencias MTC -->
    <div v-if="alertasLicencias.length > 0" class="alerts-container">
      <div v-for="alerta in alertasLicencias" :key="alerta.id" class="alert-item" :class="alerta.tipo">
        <i class="pi pi-exclamation-triangle alert-icon"></i>
        <div class="alert-content">
          <strong>Vigencia de Licencia MTC: {{ alerta.conductor }}</strong>
          <span>{{ alerta.mensaje }}</span>
        </div>
      </div>
    </div>

    <!-- Padrón de Conductores -->
    <div class="conductores-table-card">
      <div class="card-header-bar">
        <div class="header-titles">
          <span class="badge-kicker">PERSONAL DE TRANSPORTE</span>
          <h3 class="section-title">Plantilla de Choferes Autorizados</h3>
        </div>

        <div class="header-actions">
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

          <!-- Búsqueda -->
          <div class="search-input-wrap">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model="searchQuery"
              type="text"
              class="search-input"
              placeholder="Buscar por DNI, nombre o licencia..."
            />
          </div>

          <button type="button" class="btn-refresh" @click="loadConductores" :disabled="loading">
            <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
            <span>Actualizar</span>
          </button>

          <button type="button" class="btn-primary-add" @click="openCreateModal">
            <i class="pi pi-user-plus"></i>
            <span>+ Ingresar Conductor</span>
          </button>
        </div>
      </div>

      <!-- Tabla Conductores -->
      <div class="table-container">
        <DataTable
          :value="filteredConductores"
          :loading="loading"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idConductor"
        >
          <template #empty>
            <div class="empty-state">
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
              <span :class="['doc-date', getLicenseDateClass(c.fechaVencimientoLicencia)]">
                {{ formatDate(c.fechaVencimientoLicencia) }}
              </span>
            </template>
          </Column>

          <Column header="Teléfono Móvil">
            <template #body="{ data: c }">
              <div class="phone-cell">
                <i class="pi pi-phone text-muted"></i>
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
                  class="btn-row-action"
                  title="Editar Conductor"
                  @click="openEditModal(c)"
                >
                  <i class="pi pi-pencil"></i>
                </button>

                <button
                  v-if="c.estadoActivo"
                  type="button"
                  class="btn-row-action btn-row-danger"
                  title="Dar de Baja Conductor"
                  @click="toggleEstado(c)"
                >
                  <i class="pi pi-trash"></i>
                </button>
                <button
                  v-else
                  type="button"
                  class="btn-row-action btn-row-success"
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
                {{ u.username }} ({{ u.rolNombre }}) - {{ u.nombres }} {{ u.apellidos }}
              </option>
            </select>
            <span class="field-hint">Permite al chofer ingresar a la "Cabina de Ruta" con su usuario y contraseña.</span>
          </div>

          <div class="form-group">
            <label class="form-label">Condición Operativa</label>
            <select v-model="form.estadoOperativo" class="form-select">
              <option value="DISPONIBLE">Disponible</option>
              <option value="EN_RUTA">En Ruta</option>
              <option value="DESCANSO">En Descanso</option>
              <option value="SUSPENDIDO">Suspendido</option>
            </select>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Observaciones / Récord de Conductor MTC</label>
          <textarea
            v-model="form.observaciones"
            class="form-textarea"
            rows="2"
            placeholder="Puntos en récord MTC, experiencia en rutas portuarias, antecedentes..."
          ></textarea>
        </div>

        <div class="modal-footer-actions">
          <button type="button" class="btn-cancel" @click="showModal = false">
            Cancelar
          </button>
          <button type="submit" class="btn-save" :disabled="saving">
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
import HeroBannerCard from '@/shared/components/HeroBannerCard.vue'
import MetricCard from '@/shared/components/MetricCard.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import { transporteFlotaService } from '@/features/roles-modules/services/transporteFlotaService'
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
  const hoy = new Date()
  const en30Dias = new Date()
  en30Dias.setDate(hoy.getDate() + 30)

  conductoresList.value.forEach(c => {
    if (c.fechaVencimientoLicencia) {
      const fLic = new Date(c.fechaVencimientoLicencia)
      const nombre = `${c.nombres} ${c.apellidos} (Lic. ${c.numeroLicencia})`
      if (fLic < hoy) {
        alerts.push({
          id: `lic-venc-${c.idConductor}`,
          conductor: nombre,
          tipo: 'danger',
          mensaje: `Licencia VENCIDA el ${formatDate(c.fechaVencimientoLicencia)}. Inhabilitado para conducir.`
        })
      } else if (fLic <= en30Dias) {
        alerts.push({
          id: `lic-prox-${c.idConductor}`,
          conductor: nombre,
          tipo: 'warning',
          mensaje: `Licencia por vencer el ${formatDate(c.fechaVencimientoLicencia)}.`
        })
      }
    }
  })
  return alerts
})

const usuariosDisponibles = computed(() => {
  // Retornar usuarios con rol SUPERVISOR_TRANSPORTE o sin conductor asignado, o el que ya está editando
  return usuariosList.value.filter(u => {
    if (!u.estadoActivo) return false
    return true
  })
})

const filteredConductores = computed(() => {
  return conductoresList.value.filter(c => {
    if (filterCategoria.value && c.categoriaLicencia !== filterCategoria.value) return false
    if (filterCondicion.value && c.estadoOperativo !== filterCondicion.value) return false
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase().trim()
      const nom = `${c.nombres || ''} ${c.apellidos || ''}`.toLowerCase()
      const dni = (c.numeroDocumento || '').toLowerCase()
      const lic = (c.numeroLicencia || '').toLowerCase()
      if (!nom.includes(q) && !dni.includes(q) && !lic.includes(q)) return false
    }
    return true
  })
})

const loadConductores = async () => {
  loading.value = true
  try {
    const data = await transporteFlotaService.getConductores(false)
    conductoresList.value = data || []
  } catch (err) {
    showError('Error al cargar conductores MTC')
  } finally {
    loading.value = false
  }
}

const loadUsuarios = async () => {
  try {
    const data = await userService.getUsers(true)
    usuariosList.value = data || []
  } catch {
    // Silencioso si no tiene permiso completo
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
    nombres: c.nombres,
    apellidos: c.apellidos,
    numeroDocumento: c.numeroDocumento,
    numeroLicencia: c.numeroLicencia,
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
    const payload = {
      nombres: form.value.nombres.trim(),
      apellidos: form.value.apellidos.trim(),
      numeroDocumento: form.value.numeroDocumento.trim(),
      numeroLicencia: form.value.numeroLicencia.toUpperCase().trim(),
      categoriaLicencia: form.value.categoriaLicencia,
      fechaVencimientoLicencia: form.value.fechaVencimientoLicencia,
      telefono: form.value.telefono.trim(),
      idUsuario: form.value.idUsuario ? Number(form.value.idUsuario) : null,
      estadoOperativo: form.value.estadoOperativo,
      observaciones: form.value.observaciones || null
    }

    if (isEditing.value) {
      await transporteFlotaService.updateConductor(editingId.value, payload)
      showSuccess('Conductor MTC actualizado')
    } else {
      await transporteFlotaService.createConductor(payload)
      showSuccess('Conductor MTC registrado exitosamente')
    }
    showModal.value = false
    await loadConductores()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al guardar conductor')
  } finally {
    saving.value = false
  }
}

const toggleEstado = async (c) => {
  try {
    if (c.estadoActivo) {
      await transporteFlotaService.deleteConductor(c.idConductor)
      showInfo(`Conductor ${c.nombres} ${c.apellidos} dado de baja`)
    } else {
      await transporteFlotaService.reactivarConductor(c.idConductor)
      showSuccess(`Conductor ${c.nombres} ${c.apellidos} reactivado`)
    }
    await loadConductores()
  } catch (err) {
    showError('Error al cambiar estado del conductor')
  }
}

const formatDate = (val) => {
  if (!val) return '— No reg. —'
  try {
    const d = new Date(val)
    return d.toLocaleDateString('es-PE', { year: 'numeric', month: '2-digit', day: '2-digit' })
  } catch {
    return val
  }
}

const getLicenseDateClass = (val) => {
  if (!val) return 'text-muted'
  const hoy = new Date()
  const d = new Date(val)
  if (d < hoy) return 'text-danger font-bold'
  const en30 = new Date()
  en30.setDate(hoy.getDate() + 30)
  if (d <= en30) return 'text-warning font-semibold'
  return 'text-success'
}

const getCondicionSeverity = (cond) => {
  switch (cond) {
    case 'DISPONIBLE': return 'success'
    case 'EN_RUTA': return 'info'
    case 'DESCANSO': return 'secondary'
    case 'SUSPENDIDO': return 'danger'
    default: return 'warning'
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
  gap: 1.5rem;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.alerts-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 4px;
  font-size: 0.825rem;
}
.alert-item.warning {
  background: #fdf5ea;
  border: 1px solid #e8c697;
  color: #8c5b1b;
}
.alert-item.danger {
  background: #faebeb;
  border: 1px solid #e89797;
  color: #9c2828;
}
.alert-icon {
  font-size: 1.1rem;
}
.alert-content {
  display: flex;
  flex-direction: column;
}

.conductores-table-card {
  background: var(--color-bg-card, #ede6db);
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 6px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.badge-kicker {
  font-family: var(--font-mono, monospace);
  font-size: 0.65rem;
  letter-spacing: 0.08em;
  color: var(--color-accent, #9a7f60);
  font-weight: 700;
  display: block;
}

.section-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.filter-select {
  padding: 0.45rem 0.65rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.8rem;
  color: var(--color-text-primary, #352f2a);
}

.search-input-wrap {
  position: relative;
}
.search-icon {
  position: absolute;
  left: 0.6rem;
  top: 50%;
  transform: translateY(-50%);
  color: #8c827a;
  font-size: 0.8rem;
}
.search-input {
  padding: 0.45rem 0.65rem 0.45rem 1.8rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.8rem;
  width: 220px;
}

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.75rem;
  background: transparent;
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  color: var(--color-text-primary, #352f2a);
}
.btn-refresh:hover {
  background: #ffffff;
}

.btn-primary-add {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.85rem;
  background: var(--color-accent, #9a7f60);
  border: none;
  border-radius: 4px;
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s ease;
}
.btn-primary-add:hover {
  opacity: 0.9;
}

.table-container {
  background: #ffffff;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--color-border, #e2d9cd);
}

.driver-name-cell {
  display: flex;
  flex-direction: column;
}
.driver-name {
  font-weight: 700;
  font-size: 0.825rem;
}
.driver-dni {
  font-size: 0.725rem;
  color: #6b635b;
}

.license-cell {
  display: flex;
  flex-direction: column;
}
.license-num {
  font-size: 0.8rem;
}
.license-cat {
  font-size: 0.7rem;
  color: var(--color-accent, #9a7f60);
  font-weight: 600;
}

.phone-cell {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.user-linked-cell {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.user-alias {
  font-size: 0.7rem;
  color: #555;
}

.doc-date {
  font-family: var(--font-mono, monospace);
  font-size: 0.775rem;
}
.text-danger { color: #b42318; }
.text-warning { color: #b54708; }
.text-success { color: #027a48; }
.text-muted { color: #8c827a; }

.action-buttons-cell {
  display: inline-flex;
  gap: 0.35rem;
}

.btn-row-action {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: 1px solid var(--color-border, #e2d9cd);
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  color: #352f2a;
}
.btn-row-action:hover {
  background: #f6f3ee;
}
.btn-row-danger {
  color: #b42318;
}
.btn-row-danger:hover {
  background: #faebeb;
  border-color: #e89797;
}
.btn-row-success {
  color: #027a48;
}
.btn-row-success:hover {
  background: #eef8f3;
  border-color: #a6dbbe;
}

.tag-xs {
  font-size: 0.675rem;
  padding: 0.15rem 0.4rem;
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
  gap: 0.75rem;
}
.form-grid-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.75rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.user-link-box {
  background: #fdfbf7;
  padding: 0.75rem;
  border-radius: 4px;
  border: 1px dashed var(--color-border, #e2d9cd);
}
.field-hint {
  font-size: 0.675rem;
  color: #7a7065;
  margin-top: 0.2rem;
}
.form-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}
.form-input, .form-select, .form-textarea {
  padding: 0.45rem 0.65rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.8rem;
}
.text-uppercase {
  text-transform: uppercase;
}
.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border, #e2d9cd);
}
.btn-cancel {
  padding: 0.45rem 0.85rem;
  background: transparent;
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-save {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 1rem;
  background: var(--color-accent, #9a7f60);
  border: none;
  border-radius: 4px;
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
</style>
