<template>
  <div class="gerencia-flota-view">
    <!-- Hero Banner Card -->
    <HeroBannerCard
      kicker="GERENCIA GENERAL // GESTIÓN DE ACTIVOS & TRANSPORTE"
      title="Parque Automotor & Flota de Unidades MTC"
      description="Alta, control técnico y mantenimiento de tractos, plataformas y carretas autorizadas para el transporte de carga aduanera y contenedores."
    />

    <!-- Métricas Rápidas de Flota -->
    <div class="metrics-grid">
      <MetricCard index="01" label="Tractos Disponibles" :value="tractosDisponiblesCount" severity="success" highlight />
      <MetricCard index="02" label="Unidades en Ruta" :value="unidadesEnRutaCount" severity="pending" />
      <MetricCard index="03" label="En Taller / Mantenimiento" :value="unidadesMantenimientoCount" severity="warning" />
      <MetricCard index="04" label="Total Parque Automotor" :value="unidadesList.length" severity="accent" />
    </div>

    <!-- Alertas Preventivas de Vencimiento SOAT / CITV -->
    <div v-if="alertasVencimiento.length > 0" class="alerts-container">
      <div v-for="alerta in alertasVencimiento" :key="alerta.id" class="alert-item" :class="alerta.tipo">
        <i class="pi pi-exclamation-triangle alert-icon"></i>
        <div class="alert-content">
          <strong>Atención Técnica: Placa {{ alerta.placa }}</strong>
          <span>{{ alerta.mensaje }}</span>
        </div>
      </div>
    </div>

    <!-- Sección de Gestión y Tabla -->
    <div class="flota-table-card">
      <div class="card-header-bar">
        <div class="header-titles">
          <span class="badge-kicker">PADRÓN VEHICULAR</span>
          <h3 class="section-title">Registro de Unidades de Carga Pesada</h3>
        </div>

        <div class="header-actions">
          <!-- Filtro Tipo -->
          <select v-model="filterTipoVehiculo" class="filter-select">
            <option value="">Todos los Tipos</option>
            <option value="TRACTO">Tracto Camión</option>
            <option value="PLATAFORMA">Plataforma / Carreta</option>
            <option value="FURGON">Furgón Cerrado</option>
            <option value="CAMION_BARANDA">Camión Baranda</option>
          </select>

          <!-- Filtro Condición -->
          <select v-model="filterCondicion" class="filter-select">
            <option value="">Cualquier Condición</option>
            <option value="DISPONIBLE">Disponible</option>
            <option value="EN_RUTA">En Ruta</option>
            <option value="MANTENIMIENTO">En Mantenimiento</option>
            <option value="FUERA_DE_SERVICIO">Fuera de Servicio</option>
          </select>

          <!-- Buscador -->
          <div class="search-input-wrap">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model="searchQuery"
              type="text"
              class="search-input"
              placeholder="Buscar por placa o marca..."
            />
          </div>

          <button type="button" class="btn-refresh" @click="loadUnidades" :disabled="loading">
            <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
            <span>Actualizar</span>
          </button>

          <button type="button" class="btn-primary-add" @click="openCreateModal">
            <i class="pi pi-plus"></i>
            <span>+ Ingresar Vehículo</span>
          </button>
        </div>
      </div>

      <!-- Tabla de Unidades -->
      <div class="table-container">
        <DataTable
          :value="filteredUnidades"
          :loading="loading"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idUnidad"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-truck empty-icon"></i>
              <p class="empty-title">No hay unidades registradas</p>
              <p class="empty-desc">Registre los vehículos de carga pesada autorizados por el MTC para operar.</p>
            </div>
          </template>

          <Column header="Placa Tracto">
            <template #body="{ data: u }">
              <div class="plate-cell">
                <i class="pi pi-truck"></i>
                <span class="font-mono font-bold">{{ u.placaTracto }}</span>
              </div>
            </template>
          </Column>

          <Column header="Placa Carreta">
            <template #body="{ data: u }">
              <span class="font-mono text-xs">{{ u.placaCarreta || '— S/C —' }}</span>
            </template>
          </Column>

          <Column header="Marca / Modelo">
            <template #body="{ data: u }">
              <div class="vehicle-info">
                <span class="vehicle-title">{{ u.marcaModelo }}</span>
                <span class="vehicle-type">{{ formatTipo(u.tipoVehiculo) }}</span>
              </div>
            </template>
          </Column>

          <Column header="Capacidad">
            <template #body="{ data: u }">
              <span class="font-mono text-xs font-bold">{{ Number(u.capacidadCargaKg || 0).toLocaleString('es-PE') }} kg</span>
            </template>
          </Column>

          <Column header="SOAT MTC">
            <template #body="{ data: u }">
              <span :class="['doc-date', getDocDateClass(u.fechaVencimientoSoat)]">
                {{ formatDate(u.fechaVencimientoSoat) }}
              </span>
            </template>
          </Column>

          <Column header="Rev. Técnica (CITV)">
            <template #body="{ data: u }">
              <span :class="['doc-date', getDocDateClass(u.fechaVencimientoCitv)]">
                {{ formatDate(u.fechaVencimientoCitv) }}
              </span>
            </template>
          </Column>

          <Column header="Operatividad">
            <template #body="{ data: u }">
              <Tag
                :severity="getCondicionSeverity(u.estadoOperativo)"
                :value="u.estadoOperativo || 'DISPONIBLE'"
                rounded
                class="tag-xs"
              />
            </template>
          </Column>

          <Column header="Estado">
            <template #body="{ data: u }">
              <Tag
                :severity="u.estadoActivo ? 'success' : 'danger'"
                :value="u.estadoActivo ? 'ACTIVO' : 'INACTIVO'"
                rounded
                class="tag-xs"
              />
            </template>
          </Column>

          <Column header="Acciones" headerStyle="width: 110px; text-align: right;" bodyStyle="text-align: right;">
            <template #body="{ data: u }">
              <div class="action-buttons-cell">
                <button
                  type="button"
                  class="btn-row-action"
                  title="Editar Unidad"
                  @click="openEditModal(u)"
                >
                  <i class="pi pi-pencil"></i>
                </button>

                <button
                  v-if="u.estadoActivo"
                  type="button"
                  class="btn-row-action btn-row-danger"
                  title="Desactivar Unidad"
                  @click="toggleEstado(u)"
                >
                  <i class="pi pi-trash"></i>
                </button>
                <button
                  v-else
                  type="button"
                  class="btn-row-action btn-row-success"
                  title="Reactivar Unidad"
                  @click="toggleEstado(u)"
                >
                  <i class="pi pi-refresh"></i>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <!-- MODAL: CREAR / EDITAR UNIDAD -->
    <Dialog
      v-model:visible="showModal"
      modal
      :header="isEditing ? 'EDITAR UNIDAD // PARQUE AUTOMOTOR' : 'NUEVO VEHÍCULO // ALTA DE FLOTA MTC'"
      :style="{ width: '640px' }"
      class="custom-modal-editorial"
    >
      <form @submit.prevent="saveUnidad" class="modal-form">
        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Placa Tracto (Principal) *</label>
            <input
              v-model="form.placaTracto"
              type="text"
              class="form-input text-uppercase font-mono font-bold"
              placeholder="Ej: ABC-789"
              maxlength="15"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">Placa Carreta / Remolque</label>
            <input
              v-model="form.placaCarreta"
              type="text"
              class="form-input text-uppercase font-mono"
              placeholder="Ej: RTA-452 (Opcional)"
              maxlength="15"
            />
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Marca y Modelo *</label>
            <input
              v-model="form.marcaModelo"
              type="text"
              class="form-input"
              placeholder="Ej: Volvo FH 500 / Scania G410"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">Tipo de Vehículo *</label>
            <select v-model="form.tipoVehiculo" class="form-select" required>
              <option value="TRACTO">Tracto Camión</option>
              <option value="PLATAFORMA">Plataforma / Carreta</option>
              <option value="FURGON">Furgón Cerrado</option>
              <option value="CAMION_BARANDA">Camión Baranda</option>
            </select>
          </div>
        </div>

        <div class="form-grid-3">
          <div class="form-group">
            <label class="form-label">Capacidad (Kg) *</label>
            <input
              v-model.number="form.capacidadCargaKg"
              type="number"
              step="100"
              min="500"
              class="form-input font-mono"
              placeholder="32000"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">Año Fabricación</label>
            <input
              v-model.number="form.anioFabricacion"
              type="number"
              min="1990"
              max="2030"
              class="form-input font-mono"
              placeholder="2022"
            />
          </div>

          <div class="form-group">
            <label class="form-label">Estado Operativo</label>
            <select v-model="form.estadoOperativo" class="form-select">
              <option value="DISPONIBLE">Disponible</option>
              <option value="EN_RUTA">En Ruta</option>
              <option value="MANTENIMIENTO">En Mantenimiento</option>
              <option value="FUERA_DE_SERVICIO">Fuera de Servicio</option>
            </select>
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Vencimiento SOAT MTC</label>
            <input
              v-model="form.fechaVencimientoSoat"
              type="date"
              class="form-input font-mono"
            />
          </div>

          <div class="form-group">
            <label class="form-label">Vencimiento CITV (Rev. Técnica)</label>
            <input
              v-model="form.fechaVencimientoCitv"
              type="date"
              class="form-input font-mono"
            />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Observaciones Mecánicas / Documentarias</label>
          <textarea
            v-model="form.observaciones"
            class="form-textarea"
            rows="2"
            placeholder="Detalles de inspección técnica, pólizas o revisiones programadas..."
          ></textarea>
        </div>

        <div class="modal-footer-actions">
          <button type="button" class="btn-cancel" @click="showModal = false">
            Cancelar
          </button>
          <button type="submit" class="btn-save" :disabled="saving">
            <i v-if="saving" class="pi pi-spin pi-spinner"></i>
            <span>{{ isEditing ? 'Guardar Cambios' : 'Dar de Alta Vehículo' }}</span>
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
import { useNotification } from '@/shared/composables/useNotification'

const { showSuccess, showError, showInfo } = useNotification()

const unidadesList = ref([])
const loading = ref(false)
const saving = ref(false)
const showModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)

const filterTipoVehiculo = ref('')
const filterCondicion = ref('')
const searchQuery = ref('')

const form = ref({
  placaTracto: '',
  placaCarreta: '',
  marcaModelo: '',
  tipoVehiculo: 'TRACTO',
  capacidadCargaKg: 30000,
  anioFabricacion: new Date().getFullYear(),
  fechaVencimientoSoat: '',
  fechaVencimientoCitv: '',
  estadoOperativo: 'DISPONIBLE',
  observaciones: ''
})

const tractosDisponiblesCount = computed(() =>
  unidadesList.value.filter(u => u.estadoActivo && u.estadoOperativo === 'DISPONIBLE').length
)
const unidadesEnRutaCount = computed(() =>
  unidadesList.value.filter(u => u.estadoOperativo === 'EN_RUTA').length
)
const unidadesMantenimientoCount = computed(() =>
  unidadesList.value.filter(u => u.estadoOperativo === 'MANTENIMIENTO' || u.estadoOperativo === 'FUERA_DE_SERVICIO').length
)

const alertasVencimiento = computed(() => {
  const alerts = []
  const hoy = new Date()
  const en30Dias = new Date()
  en30Dias.setDate(hoy.getDate() + 30)

  unidadesList.value.forEach(u => {
    if (u.fechaVencimientoSoat) {
      const fSoat = new Date(u.fechaVencimientoSoat)
      if (fSoat < hoy) {
        alerts.push({
          id: `soat-venc-${u.idUnidad}`,
          placa: u.placaTracto,
          tipo: 'danger',
          mensaje: `SOAT vencido el ${formatDate(u.fechaVencimientoSoat)}. Unidad no autorizada para circular.`
        })
      } else if (fSoat <= en30Dias) {
        alerts.push({
          id: `soat-prox-${u.idUnidad}`,
          placa: u.placaTracto,
          tipo: 'warning',
          mensaje: `SOAT por vencer el ${formatDate(u.fechaVencimientoSoat)}.`
        })
      }
    }
    if (u.fechaVencimientoCitv) {
      const fCitv = new Date(u.fechaVencimientoCitv)
      if (fCitv < hoy) {
        alerts.push({
          id: `citv-venc-${u.idUnidad}`,
          placa: u.placaTracto,
          tipo: 'danger',
          mensaje: `Revisión Técnica (CITV) vencida el ${formatDate(u.fechaVencimientoCitv)}.`
        })
      }
    }
  })
  return alerts
})

const filteredUnidades = computed(() => {
  return unidadesList.value.filter(u => {
    if (filterTipoVehiculo.value && u.tipoVehiculo !== filterTipoVehiculo.value) return false
    if (filterCondicion.value && u.estadoOperativo !== filterCondicion.value) return false
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase().trim()
      const tracto = (u.placaTracto || '').toLowerCase()
      const carreta = (u.placaCarreta || '').toLowerCase()
      const marca = (u.marcaModelo || '').toLowerCase()
      if (!tracto.includes(q) && !carreta.includes(q) && !marca.includes(q)) return false
    }
    return true
  })
})

const loadUnidades = async () => {
  loading.value = true
  try {
    const data = await transporteFlotaService.getUnidades({ soloActivos: false })
    unidadesList.value = data || []
  } catch (err) {
    showError('Error al cargar unidades de transporte')
  } finally {
    loading.value = false
  }
}

const openCreateModal = () => {
  isEditing.value = false
  editingId.value = null
  form.value = {
    placaTracto: '',
    placaCarreta: '',
    marcaModelo: '',
    tipoVehiculo: 'TRACTO',
    capacidadCargaKg: 30000,
    anioFabricacion: new Date().getFullYear(),
    fechaVencimientoSoat: '',
    fechaVencimientoCitv: '',
    estadoOperativo: 'DISPONIBLE',
    observaciones: ''
  }
  showModal.value = true
}

const openEditModal = (u) => {
  isEditing.value = true
  editingId.value = u.idUnidad
  form.value = {
    placaTracto: u.placaTracto,
    placaCarreta: u.placaCarreta || '',
    marcaModelo: u.marcaModelo,
    tipoVehiculo: u.tipoVehiculo || 'TRACTO',
    capacidadCargaKg: u.capacidadCargaKg || 30000,
    anioFabricacion: u.anioFabricacion || new Date().getFullYear(),
    fechaVencimientoSoat: u.fechaVencimientoSoat ? u.fechaVencimientoSoat.substring(0, 10) : '',
    fechaVencimientoCitv: u.fechaVencimientoCitv ? u.fechaVencimientoCitv.substring(0, 10) : '',
    estadoOperativo: u.estadoOperativo || 'DISPONIBLE',
    observaciones: u.observaciones || ''
  }
  showModal.value = true
}

const saveUnidad = async () => {
  saving.value = true
  try {
    const payload = {
      placaTracto: form.value.placaTracto.toUpperCase().trim(),
      placaCarreta: form.value.placaCarreta ? form.value.placaCarreta.toUpperCase().trim() : null,
      marcaModelo: form.value.marcaModelo.trim(),
      tipoVehiculo: form.value.tipoVehiculo,
      capacidadCargaKg: Number(form.value.capacidadCargaKg),
      anioFabricacion: form.value.anioFabricacion ? Number(form.value.anioFabricacion) : null,
      fechaVencimientoSoat: form.value.fechaVencimientoSoat || null,
      fechaVencimientoCitv: form.value.fechaVencimientoCitv || null,
      estadoOperativo: form.value.estadoOperativo,
      observaciones: form.value.observaciones || null
    }

    if (isEditing.value) {
      await transporteFlotaService.updateUnidad(editingId.value, payload)
      showSuccess('Unidad vehicular actualizada')
    } else {
      await transporteFlotaService.createUnidad(payload)
      showSuccess('Vehículo ingresado a la flota')
    }
    showModal.value = false
    await loadUnidades()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al guardar unidad vehicular')
  } finally {
    saving.value = false
  }
}

const toggleEstado = async (u) => {
  try {
    if (u.estadoActivo) {
      await transporteFlotaService.deleteUnidad(u.idUnidad)
      showInfo(`Unidad ${u.placaTracto} dada de baja`)
    } else {
      await transporteFlotaService.reactivarUnidad(u.idUnidad)
      showSuccess(`Unidad ${u.placaTracto} reactivada`)
    }
    await loadUnidades()
  } catch (err) {
    showError('Error al cambiar estado de la unidad')
  }
}

const formatTipo = (tipo) => {
  const map = {
    TRACTO: 'Tracto Camión',
    PLATAFORMA: 'Plataforma / Carreta',
    FURGON: 'Furgón Cerrado',
    CAMION_BARANDA: 'Camión Baranda'
  }
  return map[tipo] || tipo || 'Tracto'
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

const getDocDateClass = (val) => {
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
    case 'MANTENIMIENTO': return 'warning'
    case 'FUERA_DE_SERVICIO': return 'danger'
    default: return 'secondary'
  }
}

onMounted(() => {
  loadUnidades()
})
</script>

<style scoped>
.gerencia-flota-view {
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

.flota-table-card {
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
  width: 200px;
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

.plate-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.vehicle-info {
  display: flex;
  flex-direction: column;
}
.vehicle-title {
  font-weight: 700;
  font-size: 0.825rem;
}
.vehicle-type {
  font-size: 0.725rem;
  color: #6b635b;
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
