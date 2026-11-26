<template>
  <aside class="transito-aside-stack">
    <!-- Ruta Logística Internacional -->
    <section class="sga-card-surface transito-context-card">
      <div class="aside-title-row">
        <span class="sga-kicker">TRAVESÍA INTERNACIONAL</span>
        <Tag
          :value="expediente.modalidad || 'MARÍTIMO'"
          :icon="modalidadIcon"
          severity="secondary"
          rounded
        />
      </div>

      <div class="route-vertical">
        <div class="route-node">
          <span class="route-dot is-origin"></span>
          <div class="route-info">
            <span class="sga-micro-label">{{ originRoleLabel }}</span>
            <span class="route-port">{{ puertoOrigen }}</span>
            <span v-if="expediente.puertoEnvioPais" class="sga-cell-sub">{{ expediente.puertoEnvioPais }}</span>
          </div>
        </div>

        <div class="route-mid">
          <span class="sga-cell-sub font-mono">{{ journeyText }}</span>
        </div>

        <div class="route-node">
          <span class="route-dot is-dest"></span>
          <div class="route-info">
            <span class="sga-micro-label">{{ destinationRoleLabel }}</span>
            <span class="route-port">{{ puertoDestino }}</span>
            <span class="sga-cell-sub">{{ expediente.puertoDestinoPais || 'Perú' }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Especificaciones Reales de Carga -->
    <section class="sga-card-surface transito-context-card">
      <span class="sga-kicker">DETALLES DE CARGA</span>
      <div class="specs-stack">
        <div class="spec-row">
          <span class="sga-form-label">Consignatario</span>
          <span class="spec-val">{{ expediente.clienteRazonSocial || '—' }}</span>
        </div>
        <div class="spec-row">
          <span class="sga-form-label">RUC / Doc</span>
          <span class="spec-val font-mono">{{ expediente.clienteNumeroDocumento || '—' }}</span>
        </div>
        <div class="spec-row">
          <span class="sga-form-label">Mercancía</span>
          <span class="spec-val">{{ expediente.mercanciaNombre || 'Carga Comercial' }}</span>
        </div>
        <div class="spec-row">
          <span class="sga-form-label">Tipo de Carga</span>
          <span class="spec-val">{{ expediente.tipoCargaNombre || 'Carga General' }}</span>
        </div>
        <div v-if="expediente.naveViaje" class="spec-row">
          <span class="sga-form-label">Carrier / Nave</span>
          <span class="spec-val font-mono">{{ expediente.naveViaje }}</span>
        </div>
        <div v-if="expediente.numeroContenedor" class="spec-row">
          <span class="sga-form-label">Contenedor / Guía</span>
          <span class="spec-val font-mono">{{ expediente.numeroContenedor }}</span>
        </div>
      </div>
    </section>

    <!-- Orientación Operativa -->
    <div class="sga-banner is-info">
      <i class="pi pi-info-circle"></i>
      <div>
        <span class="sga-micro-label">GUÍA DE HITO ADUANERO</span>
        <p class="aside-guidance-text">{{ guidanceText }}</p>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'
import { calculateTransitDays, formatHumanShort } from '@/shared/utils/dateUtils'

const props = defineProps({
  expediente: { type: Object, required: true },
  activeMode: { type: String, default: 'salida' }
})

const isAereo = computed(() => (props.expediente?.modalidad || '').toUpperCase().includes('AER'))
const isTerrestre = computed(() => (props.expediente?.modalidad || '').toUpperCase().includes('TERR'))

const modalidadIcon = computed(() => {
  if (isAereo.value) return 'pi pi-send'
  if (isTerrestre.value) return 'pi pi-truck'
  return 'pi pi-compass'
})

const originRoleLabel = computed(() => {
  if (isAereo.value) return 'AEROPUERTO ORIGEN'
  if (isTerrestre.value) return 'TERMINAL ORIGEN'
  return 'PUERTO ORIGEN (POL)'
})

const destinationRoleLabel = computed(() => {
  if (isAereo.value) return 'AEROPUERTO DESTINO'
  if (isTerrestre.value) return 'TERMINAL DESTINO'
  return 'PUERTO DESTINO (POD)'
})

const puertoOrigen = computed(() => {
  const exp = props.expediente
  return exp?.puertoEnvioNombre || (exp?.puertoEnvioPais ? `Puerto (${exp.puertoEnvioPais})` : 'Punto de Origen')
})

const puertoDestino = computed(() => {
  return props.expediente?.puertoDestinoNombre || 'Callao / Lima'
})

const journeyText = computed(() => {
  const zarpe = props.expediente?.fechaZarpeEtd
  const eta = props.expediente?.fechaLlegadaEta
  if (zarpe && eta) {
    const diff = calculateTransitDays(zarpe, eta)
    if (diff === 0) return 'Tránsito el mismo día'
    if (diff > 0) return `~${diff} día${diff === 1 ? '' : 's'} de travesía`
  }
  if (zarpe) return `Zarpe: ${formatHumanShort(zarpe)}`
  if (eta) return `ETA: ${formatHumanShort(eta)}`
  return 'En programación'
})

const guidanceText = computed(() => {
  if (props.activeMode === 'salida') {
    return 'Al confirmar salida de origen, el expediente pasa a EN TRÁNSITO y notifica automáticamente al cliente B2B.'
  }
  if (props.activeMode === 'arribo') {
    return 'Al confirmar arribo a destino nacional, el Supervisor Aduanero podrá asignar DAM y Canal SUNAT.'
  }
  return 'Actualiza nave, contenedor, precinto o fechas ETD/ETA ante novedades de travesía o transbordo.'
})
</script>

<style scoped>
.transito-aside-stack {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.transito-context-card {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.aside-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.route-vertical {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.route-node {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.route-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-top: 0.2rem;
  flex-shrink: 0;
}

.route-dot.is-origin {
  background: var(--color-primary);
}

.route-dot.is-dest {
  background: var(--color-success-text);
}

.route-info {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  min-width: 0;
}

.route-port {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.route-mid {
  margin-left: 0.3rem;
  padding: 0.35rem 0 0.35rem 1rem;
  border-left: 2px dashed var(--color-border-strong);
}

.specs-stack {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.spec-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
}

.spec-val {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-main);
  text-align: right;
  overflow-wrap: anywhere;
}

.aside-guidance-text {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
}
</style>
