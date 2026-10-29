<template>
  <div class="notificaciones-view">
    <!-- Header de Sección -->
    <div class="view-header">
      <div class="header-titles">
        <span class="view-kicker">CENTRO DE COMUNICACIONES B2B</span>
        <h1 class="view-title">Notificaciones & Avisos Operativos</h1>
        <p class="view-subtitle">
          Historial de avisos oficiales, cotizaciones aprobadas, credenciales de acceso y trazabilidad aduanera.
        </p>
      </div>
      <div class="header-actions">
        <button 
          class="btn-secondary" 
          @click="cargarNotificaciones" 
          :disabled="loading"
          title="Refrescar lista"
        >
          <i :class="loading ? 'pi pi-spin pi-spinner' : 'pi pi-refresh'"></i>
          <span>Actualizar</span>
        </button>
        <button 
          class="btn-primary" 
          @click="marcarTodasLeidas" 
          :disabled="totalNoLeidas === 0 || loading"
          title="Marcar todas como leídas"
        >
          <i class="pi pi-check-double"></i>
          <span>Marcar todas como leídas</span>
        </button>
      </div>
    </div>

    <!-- Métricas KPI con numeración 01..04 estándar Warm Editorial -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">01</span>
          <div class="kpi-icon-box">
            <i class="pi pi-bell"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL NOTIFICACIONES</span>
          <span class="kpi-value font-mono">{{ notificaciones.length }}</span>
        </div>
      </div>

      <div class="kpi-card" :class="{ 'warning-highlight': totalNoLeidas > 0 }">
        <div class="kpi-top">
          <span class="kpi-idx">02</span>
          <div class="kpi-icon-box" :class="totalNoLeidas > 0 ? 'warning' : ''">
            <i class="pi pi-inbox"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">PENDIENTES POR LEER</span>
          <span class="kpi-value font-mono">{{ totalNoLeidas }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">03</span>
          <div class="kpi-icon-box success">
            <i class="pi pi-check-circle"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">COTIZACIONES APROBADAS</span>
          <span class="kpi-value font-mono">{{ totalAprobaciones }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">04</span>
          <div class="kpi-icon-box accent">
            <i class="pi pi-shield"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">SEGURIDAD Y ACCESOS</span>
          <span class="kpi-value font-mono">{{ totalSeguridad }}</span>
        </div>
      </div>
    </div>

    <!-- Barra de Filtros -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="pi pi-search"></i>
        <input 
          v-model="filtroTexto" 
          type="text" 
          placeholder="Buscar por asunto o contenido del mensaje..." 
          class="search-input"
        />
      </div>

      <div class="filter-group">
        <label>Lectura:</label>
        <select v-model="filtroEstado" class="custom-select">
          <option value="TODAS">Todas las notificaciones</option>
          <option value="NO_LEIDAS">Solo No Leídas</option>
          <option value="LEIDAS">Solo Leídas</option>
        </select>
      </div>

      <div class="filter-group">
        <label>Categoría:</label>
        <select v-model="filtroTipo" class="custom-select">
          <option value="">Todas las categorías</option>
          <option value="ACCESO_CREADO">Accesos & Cuenta</option>
          <option value="COTIZACION_APROBADA">Cotizaciones Aprobadas</option>
          <option value="COTIZACION_OBSERVADA">Cotizaciones Observadas</option>
          <option value="DAM">Despacho Aduanero DAM</option>
          <option value="PAGO">Cobros y Facturación</option>
          <option value="SISTEMA">Avisos del Sistema</option>
        </select>
      </div>
    </div>

    <!-- Lista de Notificaciones -->
    <div class="notifications-container">
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando notificaciones...</span>
      </div>

      <div v-else-if="notificacionesFiltradas.length === 0" class="empty-state">
        <div class="empty-icon-box">
          <i class="pi pi-inbox"></i>
        </div>
        <h3>No se encontraron notificaciones</h3>
        <p v-if="filtroTexto || filtroEstado !== 'TODAS' || filtroTipo">
          Pruebe ajustando o limpiando los filtros seleccionados.
        </p>
        <p v-else>
          Su bandeja se encuentra al día. Los avisos importantes aparecerán aquí en cuanto ocurran.
        </p>
      </div>

      <div v-else class="notifications-list">
        <article 
          v-for="notif in paginatedNotificaciones" 
          :key="notif.idNotificacion"
          class="notif-card"
          :class="{ 'is-unread': !notif.leido }"
        >
          <!-- Ícono de Categoría -->
          <div class="notif-icon-col">
            <div class="notif-avatar" :class="getTipoColorClass(notif.tipo)">
              <i :class="getTipoIcon(notif.tipo)"></i>
            </div>
          </div>

          <!-- Contenido Principal -->
          <div class="notif-content-col">
            <div class="notif-meta-row">
              <span class="notif-type-tag" :class="getTipoColorClass(notif.tipo)">
                {{ getTipoLabel(notif.tipo) }}
              </span>
              <span class="notif-date font-mono">
                <i class="pi pi-calendar"></i>
                {{ formatFecha(notif.fechaCreacion) }}
              </span>
              <span v-if="!notif.leido" class="unread-pill">Nueva</span>
            </div>

            <h3 class="notif-title">{{ notif.titulo }}</h3>
            <p class="notif-message">{{ notif.mensaje }}</p>

            <div class="notif-footer-actions">
              <router-link 
                v-if="notif.enlace" 
                :to="notif.enlace" 
                class="btn-link-action"
                @click="onLinkClick(notif)"
              >
                <span>Ver detalles asociados</span>
                <i class="pi pi-arrow-right"></i>
              </router-link>

              <button 
                v-if="!notif.leido"
                type="button" 
                class="btn-mark-read"
                @click="marcarComoLeida(notif)"
                title="Marcar como leída"
              >
                <i class="pi pi-check"></i>
                <span>Marcar como leída</span>
              </button>
            </div>
          </div>
        </article>

        <!-- Paginación Reutilizable -->
        <BasePagination 
          v-if="totalItems > pageSize"
          v-model:currentPage="currentPage"
          :total-pages="totalPages"
          :total-items="totalItems"
          :start-index="startIndex"
          :end-index="endIndex"
          item-label="notificaciones"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { portalNotificationService } from '../services/portalNotificationService'
import { useNotification } from '@/shared/composables/useNotification'
import BasePagination from '@/shared/components/BasePagination.vue'

const { showSuccess, showError } = useNotification()

const notificaciones = ref([])
const loading = ref(false)
const filtroTexto = ref('')
const filtroEstado = ref('TODAS')
const filtroTipo = ref('')

// Paginación
const currentPage = ref(1)
const pageSize = ref(8)

const totalItems = computed(() => notificacionesFiltradas.value.length)
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value) || 1)
const startIndex = computed(() => (totalItems.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const endIndex = computed(() => Math.min(currentPage.value * pageSize.value, totalItems.value))

const paginatedNotificaciones = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return notificacionesFiltradas.value.slice(start, start + pageSize.value)
})

watch([filtroTexto, filtroEstado, filtroTipo], () => {
  currentPage.value = 1
})

async function cargarNotificaciones() {
  loading.value = true
  try {
    const data = await portalNotificationService.getNotificaciones()
    notificaciones.value = data || []
  } catch (error) {
    showError(error.response?.data?.message || 'Error al cargar las notificaciones')
  } finally {
    loading.value = false
  }
}

async function marcarComoLeida(notif) {
  try {
    await portalNotificationService.marcarComoLeida(notif.idNotificacion)
    notif.leido = true
    showSuccess('Notificación marcada como leída', 'Bandeja Actualizada', 2500)
  } catch (error) {
    showError('No se pudo marcar la notificación como leída')
  }
}

async function marcarTodasLeidas() {
  try {
    await portalNotificationService.marcarTodasComoLeidas()
    notificaciones.value.forEach(n => { n.leido = true })
    showSuccess('Todas las notificaciones han sido marcadas como leídas', 'Operación Exitosa', 3000)
  } catch (error) {
    showError('No se pudieron marcar todas las notificaciones como leídas')
  }
}

function onLinkClick(notif) {
  if (!notif.leido) {
    marcarComoLeida(notif)
  }
}

const totalNoLeidas = computed(() => {
  return notificaciones.value.filter(n => !n.leido).length
})

const totalAprobaciones = computed(() => {
  return notificaciones.value.filter(n => n.tipo === 'COTIZACION_APROBADA').length
})

const totalSeguridad = computed(() => {
  return notificaciones.value.filter(n => n.tipo === 'ACCESO_CREADO' || n.tipo === 'SEGURIDAD').length
})

const notificacionesFiltradas = computed(() => {
  return notificaciones.value.filter(n => {
    // Filtro estado
    if (filtroEstado.value === 'NO_LEIDAS' && n.leido) return false
    if (filtroEstado.value === 'LEIDAS' && !n.leido) return false

    // Filtro tipo
    if (filtroTipo.value && n.tipo !== filtroTipo.value) return false

    // Filtro texto
    if (filtroTexto.value) {
      const q = filtroTexto.value.toLowerCase()
      const matchTitulo = n.titulo?.toLowerCase().includes(q)
      const matchMensaje = n.mensaje?.toLowerCase().includes(q)
      if (!matchTitulo && !matchMensaje) return false
    }

    return true
  })
})

function getTipoIcon(tipo) {
  switch (tipo) {
    case 'ACCESO_CREADO':
      return 'pi pi-key'
    case 'COTIZACION_APROBADA':
      return 'pi pi-check-circle'
    case 'COTIZACION_OBSERVADA':
      return 'pi pi-exclamation-triangle'
    case 'DAM':
      return 'pi pi-box'
    case 'PAGO':
      return 'pi pi-credit-card'
    case 'SEGURIDAD':
      return 'pi pi-lock'
    default:
      return 'pi pi-bell'
  }
}

function getTipoColorClass(tipo) {
  switch (tipo) {
    case 'ACCESO_CREADO':
    case 'SEGURIDAD':
      return 'accent'
    case 'COTIZACION_APROBADA':
      return 'success'
    case 'COTIZACION_OBSERVADA':
      return 'danger'
    case 'DAM':
      return 'info'
    case 'PAGO':
      return 'warning'
    default:
      return 'neutral'
  }
}

function getTipoLabel(tipo) {
  switch (tipo) {
    case 'ACCESO_CREADO':
      return 'Credencial de Acceso'
    case 'COTIZACION_APROBADA':
      return 'Cotización Aprobada'
    case 'COTIZACION_OBSERVADA':
      return 'Cotización Observada'
    case 'DAM':
      return 'Despacho DAM'
    case 'PAGO':
      return 'Facturación y Pagos'
    case 'SEGURIDAD':
      return 'Seguridad'
    default:
      return 'Aviso General'
  }
}

function formatFecha(fechaStr) {
  if (!fechaStr) return '-'
  try {
    const d = new Date(fechaStr)
    return d.toLocaleString('es-PE', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return fechaStr
  }
}

onMounted(() => {
  cargarNotificaciones()
})
</script>

<style scoped>
.notificaciones-view {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  width: 100%;
}

/* Header */
.view-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.view-kicker {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-accent-kicker);
  margin-bottom: 0.35rem;
  display: block;
}

.view-title {
  font-size: 1.85rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--color-text-main);
  margin: 0 0 0.5rem 0;
  font-family: var(--font-sans);
}

.view-subtitle {
  font-size: 0.92rem;
  color: var(--color-text-muted);
  max-width: 680px;
  margin: 0;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.btn-primary, .btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.65rem 1.25rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 1.15rem 1.35rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.75rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-border-strong);
}

.kpi-card.warning-highlight {
  border-color: var(--color-warning-border);
  background: var(--color-warning-bg);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.kpi-idx {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 6px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
  font-family: var(--font-mono);
}

.kpi-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.kpi-icon-box.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.kpi-icon-box.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.kpi-icon-box.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
}

.kpi-content {
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.kpi-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin-top: 0.15rem;
}

/* Filtros */
.filters-bar {
  display: flex;
  gap: 1rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 0.85rem 1.25rem;
  align-items: center;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 260px;
  position: relative;
  display: flex;
  align-items: center;
}

.search-box i {
  position: absolute;
  left: 0.85rem;
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

.search-input {
  width: 100%;
  padding: 0.55rem 0.85rem 0.55rem 2.4rem;
  border-radius: 6px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-base);
  font-size: 0.85rem;
  color: var(--color-text-main);
  outline: none;
  transition: border-color 0.2s ease;
}

.search-input:focus {
  border-color: var(--color-primary);
  background-color: var(--color-bg-surface);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-group label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.custom-select {
  padding: 0.55rem 0.95rem;
  border-radius: 6px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-base);
  font-size: 0.84rem;
  color: var(--color-text-main);
  outline: none;
  cursor: pointer;
}

.custom-select:focus {
  border-color: var(--color-primary);
  background-color: var(--color-bg-surface);
}

/* Lista de Notificaciones */
.notifications-container {
  display: flex;
  flex-direction: column;
}

.loading-state, .empty-state {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 3.5rem 2rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  color: var(--color-text-muted);
}

.empty-icon-box {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  margin-bottom: 0.25rem;
}

.empty-state h3 {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.empty-state p {
  font-size: 0.88rem;
  margin: 0;
  max-width: 480px;
  line-height: 1.45;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.notif-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  display: flex;
  gap: 1.25rem;
  transition: all 0.2s ease;
  position: relative;
}

.notif-card:hover {
  border-color: var(--color-border-strong);
  box-shadow: 0 3px 8px rgba(43, 37, 32, 0.04);
}

.notif-card.is-unread {
  background: var(--color-bg-subtle);
  border-left: 4px solid var(--color-accent-kicker);
}

.notif-icon-col {
  flex-shrink: 0;
}

.notif-avatar {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.15rem;
}

.notif-avatar.neutral {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.notif-avatar.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.notif-avatar.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
}

.notif-avatar.info {
  background: var(--color-bg-subtle);
  color: var(--color-info-text, var(--color-primary));
}

.notif-avatar.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.notif-avatar.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
}

.notif-content-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.notif-meta-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.notif-type-tag {
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

.notif-type-tag.neutral {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.notif-type-tag.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.notif-type-tag.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
}

.notif-type-tag.info {
  background: var(--color-bg-subtle);
  color: var(--color-info-text, var(--color-primary));
}

.notif-type-tag.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.notif-type-tag.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
}

.notif-date {
  font-size: 0.76rem;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.unread-pill {
  font-size: 0.65rem;
  font-weight: 700;
  background: var(--color-accent-kicker);
  color: #ffffff;
  padding: 0.15rem 0.45rem;
  border-radius: 10px;
  letter-spacing: 0.04em;
}

.notif-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  line-height: 1.35;
}

.notif-message {
  font-size: 0.88rem;
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.55;
}

.notif-footer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.35rem;
  padding-top: 0.65rem;
  border-top: 1px dashed var(--color-border-subtle);
  flex-wrap: wrap;
}

.btn-link-action {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-accent-kicker);
  text-decoration: none;
  transition: gap 0.2s ease, color 0.2s ease;
}

.btn-link-action:hover {
  color: var(--color-text-main);
  gap: 0.65rem;
}

.btn-mark-read {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.78rem;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.btn-mark-read:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}
</style>
