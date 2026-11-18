<template>
  <div class="cabina-ruta-panel">
    <!-- Selector de viaje para Supervisores / Administradores -->
    <div v-if="despachosActivos.length > 1" class="supervisor-trip-selector">
      <div class="selector-label-group">
        <i class="pi pi-compass text-amber-600"></i>
        <span class="font-bold text-xs uppercase tracking-wider text-secondary">
          Selector de Viaje Activo (Torre de Control):
        </span>
      </div>
      <div class="trip-pills">
        <button
          v-for="d in despachosActivos"
          :key="d.idDespachoTransporte"
          type="button"
          :class="['trip-pill font-mono', { active: currentDespacho?.idDespachoTransporte === d.idDespachoTransporte }]"
          @click="seleccionarViaje(d)"
        >
          <span class="font-bold">{{ d.codigoTracking }}</span>
          <span class="pill-plate">{{ d.unidad?.placaTracto || 'Sin tracto' }}</span>
          <span :class="['pill-status', d.estadoTransporte.toLowerCase()]">{{ d.estadoTransporte }}</span>
        </button>
      </div>
    </div>

    <!-- Si no hay despacho cargado -->
    <div v-if="!currentDespacho" class="no-trip-card">
      <div class="no-trip-icon">
        <i class="pi pi-truck"></i>
      </div>
      <h3>Sin viaje activo asignado</h3>
      <p>No tienes ningún traslado en curso ni programado en este momento.</p>
      <div class="no-trip-actions">
        <button type="button" class="btn-outline-warm" @click="$emit('go-despachos')">
          <i class="pi pi-list"></i>
          <span>Ver Bandeja de Despachos</span>
        </button>
      </div>
    </div>

    <!-- Tarjeta Principal de Cabina Táctica -->
    <div v-else class="cabin-tactical-layout">
      <!-- Tarjeta Superior: Estado de Ruta y Tracking -->
      <div class="cabin-hero-card">
        <div class="hero-header">
          <div class="hero-left">
            <span class="badge-kicker">HOJA DE RUTA // CABINA DIGITAL MTC</span>
            <h2 class="hero-tracking font-mono">{{ currentDespacho.codigoTracking }}</h2>
            <span class="hero-subtitle">
              Expediente: <strong class="font-mono">{{ currentDespacho.expediente?.codigoExpediente || 'S/N' }}</strong>
              — Cliente: {{ currentDespacho.expediente?.cliente?.razonSocial || 'Cliente General' }}
            </span>
          </div>
          <div class="hero-right">
            <Tag
              :value="currentDespacho.estadoTransporte"
              :severity="currentDespacho.estadoTransporte === 'EN_RUTA' ? 'primary' : 'warn'"
              class="tag-lg font-mono font-bold"
            />
          </div>
        </div>

        <!-- Trípode de Itinerario: Origen -> Destino -->
        <div class="itinerary-grid">
          <div class="itinerary-node origen">
            <div class="node-icon-box">
              <i class="pi pi-map-marker"></i>
            </div>
            <div class="node-info">
              <span class="node-kicker">PUNTO DE PARTIDA</span>
              <h4 class="node-address">{{ currentDespacho.direccionPartida }}</h4>
              <span class="node-time font-mono">
                Salida: {{ formatDateTime(currentDespacho.fechaSalidaRuta || currentDespacho.fechaProgramada) }}
              </span>
            </div>
          </div>

          <div class="itinerary-divider">
            <div class="divider-line"></div>
            <div class="divider-truck-badge">
              <i class="pi pi-send"></i>
            </div>
          </div>

          <div class="itinerary-node destino">
            <div class="node-icon-box destino-box">
              <i class="pi pi-flag"></i>
            </div>
            <div class="node-info">
              <span class="node-kicker">PUNTO DE ENTREGA CONFORME</span>
              <h4 class="node-address">
                {{ currentDespacho.distritoLlegada ? currentDespacho.distritoLlegada + ' - ' : '' }}
                {{ currentDespacho.direccionLlegada }}
              </h4>
              <span class="node-time font-mono">
                Programada: {{ formatDateTime(currentDespacho.fechaProgramada) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Grid de Datos de Recursos y Contacto -->
      <div class="cabin-details-grid">
        <!-- Tarjeta de Unidad & Carreta -->
        <div class="detail-card">
          <div class="card-title-row">
            <i class="pi pi-truck text-emerald-600"></i>
            <h4>Vehículo Asignado</h4>
          </div>
          <div class="detail-rows">
            <div class="detail-row">
              <span class="label">Placa Tracto:</span>
              <span class="value font-mono font-bold">{{ currentDespacho.unidad?.placaTracto || 'Sin asignar' }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Semirremolque:</span>
              <span class="value font-mono">{{ currentDespacho.unidad?.placaCarreta || 'Plataforma Propia' }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Marca & Modelo:</span>
              <span class="value">{{ currentDespacho.unidad?.marcaModelo || '—' }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Odómetro Base:</span>
              <span class="value font-mono">{{ currentDespacho.unidad?.kilometrajeActual || 0 }} km</span>
            </div>
          </div>
        </div>

        <!-- Tarjeta de Chofer & Guía GRE-T -->
        <div class="detail-card">
          <div class="card-title-row">
            <i class="pi pi-user text-amber-600"></i>
            <h4>Chofer & Documentación</h4>
          </div>
          <div class="detail-rows">
            <div class="detail-row">
              <span class="label">Conductor:</span>
              <span class="value font-bold">
                {{ currentDespacho.conductor ? (currentDespacho.conductor.nombres + ' ' + currentDespacho.conductor.apellidos) : 'Sin asignar' }}
              </span>
            </div>
            <div class="detail-row">
              <span class="label">Licencia MTC:</span>
              <span class="value font-mono">{{ currentDespacho.conductor?.numeroLicencia || '—' }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Guía Remisión GRE-T:</span>
              <span class="value font-mono font-bold text-amber-700">
                {{ currentDespacho.guiaRemisionTransporte || 'Por emitir en SUNAT' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Tarjeta de Receptor & Contacto -->
        <div class="detail-card">
          <div class="card-title-row">
            <i class="pi pi-phone text-blue-600"></i>
            <h4>Recepción en Destino</h4>
          </div>
          <div class="detail-rows">
            <div class="detail-row">
              <span class="label">Contacto:</span>
              <span class="value font-semibold">{{ currentDespacho.contactoRecepcion || 'Recepción de Planta' }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Receptor Conforme:</span>
              <span class="value">{{ currentDespacho.receptorNombre || 'Por registrar en entrega' }}</span>
            </div>
            <div v-if="currentDespacho.receptorDni" class="detail-row">
              <span class="label">DNI Receptor:</span>
              <span class="value font-mono">{{ currentDespacho.receptorDni }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Barra Inferior de Acciones Tácticas del Viaje -->
      <div class="cabin-actions-bar">
        <button
          v-if="currentDespacho.estadoTransporte === 'PROGRAMADO'"
          type="button"
          class="btn-cabin-action primary-send"
          @click="$emit('iniciar-ruta', currentDespacho)"
        >
          <i class="pi pi-send"></i>
          <span>Iniciar Ruta de Traslado</span>
        </button>

        <button
          v-if="currentDespacho.estadoTransporte === 'EN_RUTA'"
          type="button"
          class="btn-cabin-action success-delivery"
          @click="$emit('confirmar-entrega', currentDespacho)"
        >
          <i class="pi pi-check-circle"></i>
          <span>Confirmar Entrega Conforme</span>
        </button>

        <button
          type="button"
          class="btn-cabin-action outline-incident"
          @click="$emit('reportar-incidencia', currentDespacho)"
        >
          <i class="pi pi-exclamation-triangle"></i>
          <span>Bitácora / Incidencia en Ruta</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Tag from 'primevue/tag'

const props = defineProps({
  despacho: {
    type: Object,
    default: null
  },
  despachosList: {
    type: Array,
    default: () => []
  },
  preselectedId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['iniciar-ruta', 'confirmar-entrega', 'reportar-incidencia', 'go-despachos'])

const internalDespacho = ref(null)

const despachosActivos = computed(() => {
  return props.despachosList.filter(d => ['PROGRAMADO', 'EN_RUTA'].includes((d.estadoTransporte || '').toUpperCase()))
})

const currentDespacho = computed(() => {
  return internalDespacho.value || props.despacho || despachosActivos.value[0] || null
})

watch(() => props.preselectedId, (newId) => {
  if (newId) {
    const found = props.despachosList.find(d => String(d.idDespachoTransporte) === String(newId))
    if (found) internalDespacho.value = found
  }
}, { immediate: true })

watch(() => props.despacho, (newVal) => {
  if (newVal) internalDespacho.value = newVal
}, { immediate: true })

const seleccionarViaje = (d) => {
  internalDespacho.value = d
}

const formatDateTime = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  return d.toLocaleDateString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.cabina-ruta-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.supervisor-trip-selector {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md, 0.5rem);
}

.selector-label-group {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.trip-pills {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.trip-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.75rem;
  border-radius: 9999px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.trip-pill:hover {
  border-color: var(--color-primary);
}

.trip-pill.active {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

.pill-plate {
  font-size: 0.6875rem;
  opacity: 0.85;
}

.pill-status {
  font-size: 0.625rem;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  text-transform: uppercase;
  font-weight: 700;
}

.pill-status.en_ruta { background: var(--color-info-bg); color: var(--color-info-text); }
.pill-status.programado { background: var(--color-warning-bg); color: var(--color-warning-text); }
.trip-pill.active .pill-status { background: rgba(255, 255, 255, 0.2); color: var(--color-primary-text); }

.no-trip-card {
  padding: 4rem 1rem;
  text-align: center;
  background: var(--color-bg-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.no-trip-icon {
  font-size: 3rem;
  color: var(--color-text-subtle);
}

.cabin-tactical-layout {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.cabin-hero-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.hero-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.badge-kicker {
  font-family: var(--font-mono);
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-tracking {
  margin: 0.25rem 0;
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.hero-subtitle {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.itinerary-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 1.5rem;
  padding: 1.25rem;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border-subtle);
}

@media (max-width: 768px) {
  .itinerary-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  .itinerary-divider {
    display: none;
  }
}

.itinerary-node {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
}

.node-icon-box {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  background: var(--color-info-bg);
  color: var(--color-info-text);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.125rem;
  flex-shrink: 0;
}

.node-icon-box.destino-box {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.node-kicker {
  font-family: var(--font-mono);
  font-size: 0.625rem;
  font-weight: 700;
  color: var(--color-text-subtle);
  letter-spacing: 0.06em;
}

.node-address {
  margin: 0.2rem 0;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
  line-height: 1.3;
}

.node-time {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.itinerary-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  width: 100px;
}

.divider-line {
  position: absolute;
  width: 100%;
  height: 2px;
  background: var(--color-border);
  border-style: dashed;
}

.divider-truck-badge {
  position: relative;
  z-index: 1;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  color: var(--color-accent-kicker);
}

.cabin-details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}

.detail-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.25rem;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.875rem;
}

.card-title-row h4 {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.detail-rows {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8125rem;
}

.detail-row .label {
  color: var(--color-text-muted);
}

.detail-row .value {
  color: var(--color-text-main);
}

.cabin-actions-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
}

.btn-cabin-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--radius-md, 0.5rem);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.primary-send {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.primary-send:hover {
  filter: brightness(0.95);
}

.success-delivery {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.success-delivery:hover {
  filter: brightness(0.95);
}

.outline-incident {
  background: transparent;
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.outline-incident:hover {
  background: var(--color-warning-bg);
}
</style>
