<template>
  <div class="gerencia-flota-view">
    <!-- 1. Toolbar de Acciones Warm Editorial (Sin Banners Invasivos) -->
    <div class="flota-action-toolbar">
      <div class="toolbar-left-info">
        <span class="badge-kicker">GESTIÓN DE ACTIVOS & TRANSPORTE // FLOTA MTC</span>
        <h3 class="toolbar-title">Parque Automotor & Unidades de Carga</h3>
      </div>

      <div class="toolbar-actions-group">
        <span class="count-badge font-mono">{{ filteredUnidades.length }} de {{ unidadesList.length }} unidades</span>

        <button
          type="button"
          class="btn-outline-warm"
          @click="loadUnidades"
          :disabled="loading"
          title="Recargar inventario de flota"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>

        <button
          type="button"
          class="btn-espresso"
          @click="openCreateModal"
        >
          <i class="pi pi-plus"></i>
          <span>+ Ingresar Vehículo</span>
        </button>
      </div>
    </div>

    <!-- 2. Grid de Métricas KPI Técnicas (Estándar Editorial Warm) -->
    <div class="metrics-kpi-grid">
      <div class="kpi-card success-card" @click="filterCondicion = 'DISPONIBLE'">
        <div class="kpi-card-header">
          <span class="kpi-num">01</span>
          <span class="kpi-label">Tractos Disponibles</span>
          <i class="pi pi-truck kpi-icon success"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono text-success">{{ tractosDisponiblesCount }}</span>
          <span class="kpi-badge success">En Patio</span>
        </div>
        <div class="kpi-footer-note">
          <span>Listos para asignación de ruta</span>
        </div>
      </div>

      <div class="kpi-card" @click="filterCondicion = 'EN_RUTA'">
        <div class="kpi-card-header">
          <span class="kpi-num">02</span>
          <span class="kpi-label">Unidades en Ruta</span>
          <i class="pi pi-compass kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ unidadesEnRutaCount }}</span>
          <span class="kpi-badge info">En Tránsito</span>
        </div>
        <div class="kpi-footer-note">
          <span>Despachos aduaneros activos</span>
        </div>
      </div>

      <div class="kpi-card warning-card" @click="filterCondicion = 'MANTENIMIENTO'">
        <div class="kpi-card-header">
          <span class="kpi-num">03</span>
          <span class="kpi-label">En Mantenimiento</span>
          <i class="pi pi-wrench kpi-icon warning"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono text-warning">{{ unidadesMantenimientoCount }}</span>
          <span class="kpi-badge warning">En Taller</span>
        </div>
        <div class="kpi-footer-note">
          <span>Preventivo y correctivo</span>
        </div>
      </div>

      <div class="kpi-card" @click="filterCondicion = ''">
        <div class="kpi-card-header">
          <span class="kpi-num">04</span>
          <span class="kpi-label">Parque Automotor</span>
          <i class="pi pi-database kpi-icon"></i>
        </div>
        <div class="kpi-main-stat">
          <span class="kpi-value font-mono">{{ unidadesList.length }}</span>
          <span class="kpi-badge accent">Total Flota</span>
        </div>
        <div class="kpi-footer-note">
          <span>Habilitaciones técnicas MTC</span>
        </div>
      </div>
    </div>

    <!-- 3. Alertas Preventivas de Vencimiento SOAT / CITV -->
    <div v-if="alertasVencimiento.length > 0" class="alerts-container">
      <div v-for="alerta in alertasVencimiento" :key="alerta.id" class="alert-item" :class="alerta.tipo">
        <i class="pi pi-exclamation-triangle alert-icon"></i>
        <div class="alert-content">
          <strong>Atención Técnica: Placa {{ alerta.placa }}</strong>
          <span>{{ alerta.mensaje }}</span>
        </div>
      </div>
    </div>

    <!-- 4. Sección de Gestión y Tabla -->
    <div class="table-card">
      <div class="table-toolbar">
        <div class="search-input-wrapper">
          <i class="pi pi-search search-icon"></i>
          <input
            v-model="searchQuery"
            type="text"
            class="table-search-input"
            placeholder="Buscar por placa, marca o modelo..."
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
        </div>
      </div>

      <!-- Tabla de Unidades -->
      <div class="data-table-container">
        <DataTable
          :value="filteredUnidades"
          :loading="loading"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idUnidad"
          :paginator="filteredUnidades.length > 10"
          :rows="10"
          :rowsPerPageOptions="[10, 20, 50]"
        >
          <template #empty>
            <div class="empty-state-box">
              <i class="pi pi-truck empty-icon"></i>
              <p class="empty-title">No hay unidades registradas</p>
              <p class="empty-desc">Registre los vehículos de carga pesada autorizados por el MTC para operar.</p>
            </div>
          </template>

          <Column header="Placa Tracto">
            <template #body="{ data: u }">
              <div class="plate-cell">
                <i class="pi pi-truck plate-icon"></i>
                <span class="font-mono font-bold plate-badge">{{ u.placaTracto }}</span>
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
              <span :class="['doc-date font-mono', getDocDateClass(u.fechaVencimientoSoat)]">
                {{ formatDate(u.fechaVencimientoSoat) }}
              </span>
            </template>
          </Column>

          <Column header="Rev. Técnica (CITV)">
            <template #body="{ data: u }">
              <span :class="['doc-date font-mono', getDocDateClass(u.fechaVencimientoCitv)]">
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
                  class="action-btn edit-action"
                  title="Editar Unidad"
                  @click="openEditModal(u)"
                >
                  <i class="pi pi-pencil"></i>
                </button>

                <button
                  v-if="u.estadoActivo"
                  type="button"
                  class="action-btn delete-action"
                  title="Desactivar Unidad"
                  @click="toggleEstado(u)"
                >
                  <i class="pi pi-trash"></i>
                </button>
                <button
                  v-else
                  type="button"
                  class="action-btn reactivate-action"
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
          <label class="form-label">Observaciones y Mantenimiento</label>
          <textarea
            v-model="form.observaciones"
            rows="2"
            class="form-textarea"
            placeholder="Notas técnicas, vigencia de pólizas, kilometraje actual..."
          ></textarea>
        </div>

        <div class="modal-footer-actions">
          <button type="button" class="btn-outline-warm" @click="showModal = false" :disabled="saving">
            Cancelar
          </button>
          <button type="submit" class="btn-espresso" :disabled="saving">
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

const tractosDisponiblesCount = computed(() => {
  return unidadesList.value.filter(
    u => u.tipoVehiculo === 'TRACTO' && u.estadoOperativo === 'DISPONIBLE' && u.estadoActivo
  ).length
})

const unidadesEnRutaCount = computed(() => {
  return unidadesList.value.filter(u => u.estadoOperativo === 'EN_RUTA' && u.estadoActivo).length
})

const unidadesMantenimientoCount = computed(() => {
  return unidadesList.value.filter(
    u => (u.estadoOperativo === 'MANTENIMIENTO' || u.estadoOperativo === 'FUERA_DE_SERVICIO') && u.estadoActivo
  ).length
})

const alertasVencimiento = computed(() => {
  const alerts = []
  const today = new Date()
  const in30Days = new Date()
  in30Days.setDate(today.getDate() + 30)

  unidadesList.value.forEach(u => {
    if (!u.estadoActivo) return

    if (u.fechaVencimientoSoat) {
      const soatDate = new Date(u.fechaVencimientoSoat)
      if (soatDate < today) {
        alerts.push({
          id: `soat-exp-${u.idUnidad}`,
          placa: u.placaTracto,
          tipo: 'danger',
          mensaje: `SOAT vencido el ${formatDate(u.fechaVencimientoSoat)}. Unidad no autorizada para circular.`
        })
      } else if (soatDate <= in30Days) {
        alerts.push({
          id: `soat-warn-${u.idUnidad}`,
          placa: u.placaTracto,
          tipo: 'warning',
          mensaje: `SOAT por vencer el ${formatDate(u.fechaVencimientoSoat)}.`
        })
      }
    }

    if (u.fechaVencimientoCitv) {
      const citvDate = new Date(u.fechaVencimientoCitv)
      if (citvDate < today) {
        alerts.push({
          id: `citv-exp-${u.idUnidad}`,
          placa: u.placaTracto,
          tipo: 'danger',
          mensaje: `Revisión Técnica CITV vencida el ${formatDate(u.fechaVencimientoCitv)}.`
        })
      } else if (citvDate <= in30Days) {
        alerts.push({
          id: `citv-warn-${u.idUnidad}`,
          placa: u.placaTracto,
          tipo: 'warning',
          mensaje: `Revisión Técnica CITV próxima a vencer el ${formatDate(u.fechaVencimientoCitv)}.`
        })
      }
    }
  })

  return alerts.slice(0, 3)
})

const filteredUnidades = computed(() => {
  return unidadesList.value.filter(u => {
    const matchTipo = !filterTipoVehiculo.value || u.tipoVehiculo === filterTipoVehiculo.value
    const matchCond = !filterCondicion.value || u.estadoOperativo === filterCondicion.value
    const query = searchQuery.value.trim().toLowerCase()
    const matchSearch =
      !query ||
      (u.placaTracto && u.placaTracto.toLowerCase().includes(query)) ||
      (u.placaCarreta && u.placaCarreta.toLowerCase().includes(query)) ||
      (u.marcaModelo && u.marcaModelo.toLowerCase().includes(query))

    return matchTipo && matchCond && matchSearch
  })
})

const loadUnidades = async () => {
  loading.value = true
  try {
    const data = await transporteFlotaService.getUnidades({ soloActivos: false })
    unidadesList.value = Array.isArray(data) ? data : []
  } catch (err) {
    showError(err.message || 'Error al conectar con la flota')
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
    placaTracto: u.placaTracto || '',
    placaCarreta: u.placaCarreta || '',
    marcaModelo: u.marcaModelo || '',
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
    const payload = { ...form.value }
    if (!payload.placaCarreta) delete payload.placaCarreta
    if (!payload.fechaVencimientoSoat) delete payload.fechaVencimientoSoat
    if (!payload.fechaVencimientoCitv) delete payload.fechaVencimientoCitv

    if (isEditing.value) {
      await transporteFlotaService.updateUnidad(editingId.value, payload)
      showSuccess(`Unidad ${payload.placaTracto} actualizada exitosamente`)
    } else {
      await transporteFlotaService.createUnidad(payload)
      showSuccess(`Vehículo ${payload.placaTracto} ingresado al padrón`)
    }
    showModal.value = false
    await loadUnidades()
  } catch (err) {
    showError(err.message || 'Error al procesar vehículo')
  } finally {
    saving.value = false
  }
}

const toggleEstado = async (u) => {
  try {
    if (u.estadoActivo) {
      await transporteFlotaService.deleteUnidad(u.idUnidad)
      showInfo(`Unidad ${u.placaTracto} desactivada del padrón`)
    } else {
      await transporteFlotaService.reactivarUnidad(u.idUnidad)
      showSuccess(`Unidad ${u.placaTracto} reactivada en servicio`)
    }
    await loadUnidades()
  } catch (err) {
    showError(err.message || 'Error al cambiar estado')
  }
}

const formatTipo = (t) => {
  const map = {
    TRACTO: 'Tracto Camión',
    PLATAFORMA: 'Plataforma / Carreta',
    FURGON: 'Furgón Cerrado',
    CAMION_BARANDA: 'Camión Baranda'
  }
  return map[t] || t
}

const formatDate = (d) => {
  if (!d) return '— No registrado —'
  const date = new Date(d)
  return isNaN(date.getTime()) ? d : date.toLocaleDateString('es-PE')
}

const getDocDateClass = (d) => {
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
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR EDITORIAL
   ========================================================================== */
.flota-action-toolbar {
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

.plate-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.plate-icon {
  color: var(--color-accent-kicker);
  font-size: 0.95rem;
}

.plate-badge {
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.vehicle-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.vehicle-title {
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--color-text-main);
}

.vehicle-type {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
}

.doc-date {
  font-size: 0.775rem;
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
