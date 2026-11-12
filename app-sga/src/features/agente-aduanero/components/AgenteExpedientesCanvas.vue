<template>
  <div class="agente-expedientes-canvas">
    <!-- Estado de carga -->
    <div v-if="loading" class="canvas-loading-state">
      <i class="pi pi-spin pi-spinner text-3xl"></i>
      <p>Cargando expedientes asignados...</p>
    </div>

    <!-- Estado vacío -->
    <div v-else-if="!expedientes || expedientes.length === 0" class="canvas-empty-state">
      <div class="empty-icon-wrap">
        <i class="pi pi-folder-open text-4xl"></i>
      </div>
      <h4>No hay expedientes en esta vista</h4>
      <p>No se encontraron despachos aduaneros que coincidan con los filtros seleccionados.</p>
    </div>

    <!-- Cuadrícula / Canvas de Expedientes -->
    <div v-else class="expedientes-grid">
      <article
        v-for="exp in expedientes"
        :key="exp.idExpediente"
        class="expediente-canvas-card"
        :class="getCardClass(exp)"
      >
        <!-- Cabecera de la Tarjeta -->
        <header class="card-header">
          <div class="header-badges">
            <span :class="['regimen-pill', isExport(exp) ? 'export' : 'import']">
              <i :class="isExport(exp) ? 'pi pi-arrow-up-right' : 'pi pi-arrow-down-left'"></i>
              <span>{{ isExport(exp) ? 'EXPORTACIÓN' : 'IMPORTACIÓN' }}</span>
            </span>

            <span
              v-if="exp.canalSunat"
              :class="['canal-pill', `canal-${exp.canalSunat.toLowerCase()}`]"
              :title="`Canal de control SUNAT: ${exp.canalSunat}`"
            >
              <i :class="getCanalIcon(exp.canalSunat)"></i>
              <span>CANAL {{ exp.canalSunat }}</span>
            </span>
            <span v-else-if="exp.fechaArriboAduana" class="canal-pill canal-pendiente" title="Arribado, pendiente de asignación por Supervisor">
              <i class="pi pi-clock"></i>
              <span>CANAL PENDIENTE</span>
            </span>
          </div>

          <div class="header-status">
            <span :class="['estado-chip', getEstadoChipClass(exp.estadoExpediente)]">
              {{ formatEstado(exp.estadoExpediente) }}
            </span>
          </div>
        </header>

        <!-- Identificación Principal del Despacho -->
        <div class="card-identity">
          <div class="code-and-dam">
            <h3 class="codigo-expediente" @click="$emit('detalle', exp)" title="Clic para ver informe completo">
              {{ exp.codigoExpediente }}
            </h3>
            <span class="cotizacion-ref" v-if="exp.codigoCotizacion">
              Ref: {{ exp.codigoCotizacion }}
            </span>
          </div>

          <div class="dam-badge">
            <span class="dam-lbl">DAM SUNAT</span>
            <span class="dam-val">{{ exp.numeroDam || 'Sin numerar' }}</span>
          </div>
        </div>

        <!-- Información del Cliente -->
        <div class="card-client-section">
          <div class="client-name" :title="exp.clienteRazonSocial">
            <i class="pi pi-building client-icon"></i>
            <span>{{ exp.clienteRazonSocial || 'Cliente no especificado' }}</span>
          </div>
          <span class="client-ruc font-mono" v-if="exp.clienteNumeroDocumento">
            RUC: {{ exp.clienteNumeroDocumento }}
          </span>
        </div>

        <!-- Ficha de Datos de Travesía / Logística -->
        <div class="card-logistics-box">
          <div class="logistics-row">
            <div class="logistics-col">
              <span class="log-lbl"><i class="pi pi-globe"></i> Nave / Buque:</span>
              <span class="log-val" :title="exp.naveViaje">{{ exp.naveViaje || 'Pendiente' }}</span>
            </div>
            <div class="logistics-col">
              <span class="log-lbl"><i class="pi pi-box"></i> Contenedor:</span>
              <span class="log-val font-mono" :title="exp.numeroContenedor">{{ exp.numeroContenedor || 'Pendiente' }}</span>
            </div>
          </div>

          <div class="logistics-row secondary">
            <div class="logistics-col">
              <span class="log-lbl">Precinto:</span>
              <span class="log-val font-mono">{{ exp.precintoAduanero || '—' }}</span>
            </div>
            <div class="logistics-col">
              <span class="log-lbl">{{ isExport(exp) ? 'Embarque:' : 'Arribo a Destino:' }}</span>
              <span class="log-val date-val">
                {{ getFechaHito(exp) }}
              </span>
            </div>
          </div>
        </div>

        <!-- Barra de Progreso Operativo (Mini Stepper) -->
        <div class="card-stepper-progress" :title="getProgressTitle(exp)">
          <div class="stepper-track">
            <div
              class="stepper-step"
              :class="{ done: true }"
              title="1. Asignado"
            >
              <div class="step-dot"></div>
              <span class="step-name">Asignado</span>
            </div>
            <div
              class="stepper-step"
              :class="{ done: hasZarpeConfirmado(exp) }"
              title="2. En Tránsito / Salida"
            >
              <div class="step-dot"></div>
              <span class="step-name">Tránsito</span>
            </div>
            <div
              class="stepper-step"
              :class="{ done: !!exp.fechaArriboAduana }"
              title="3. Arribo a Destino"
            >
              <div class="step-dot"></div>
              <span class="step-name">Arribo</span>
            </div>
            <div
              class="stepper-step"
              :class="{ done: !!exp.canalSunat }"
              title="4. Canal SUNAT"
            >
              <div class="step-dot"></div>
              <span class="step-name">Canal</span>
            </div>
            <div
              class="stepper-step"
              :class="{ done: exp.estadoExpediente === 'LEVANTE_AUTORIZADO' || !!exp.fechaLevanteAutorizacion }"
              title="5. Levante / Despacho"
            >
              <div class="step-dot"></div>
              <span class="step-name">Levante</span>
            </div>
          </div>
        </div>

        <!-- Acciones de la Tarjeta Segregadas por Tab -->
        <footer class="card-actions-footer">
          <!-- CASO 1: PESTAÑA 'EXPEDIENTES' / 'TODOS' -> SOLO BOTÓN VER INFORME -->
          <template v-if="isTabExpedientes">
            <button
              type="button"
              class="btn-informe-completo full-width-btn"
              @click="$emit('detalle', exp)"
              title="Ver informe técnico completo del expediente"
            >
              <i class="pi pi-file-edit"></i>
              <span>Ver Informe</span>
            </button>
            <button
              type="button"
              class="btn-icon-secondary"
              @click="$emit('documentos', exp)"
              title="Bóveda de Documentos Digitales"
            >
              <i class="pi pi-paperclip"></i>
            </button>
          </template>

          <!-- CASO 2: PESTAÑA 'TRÁNSITO' -> SOLO BOTONES DE TRÁNSITO Y ARRIBO -->
          <template v-else-if="activeTab === 'transito'">
            <button
              type="button"
              class="btn-informe-completo"
              @click="$emit('detalle', exp)"
              title="Ver informe técnico"
            >
              <i class="pi pi-file-edit"></i>
              <span>Informe</span>
            </button>

            <div class="contextual-action-btns">
              <button
                type="button"
                class="btn-icon-secondary"
                @click="$emit('documentos', exp)"
                title="Bóveda de Documentos"
              >
                <i class="pi pi-paperclip"></i>
              </button>

              <template v-if="!isExport(exp)">
                <!-- 1. Salida de Origen (solo si aún no ha zarpe) -->
                <button
                  v-if="!hasZarpeConfirmado(exp) && !isCerrado(exp)"
                  type="button"
                  class="btn-card-action btn-action-accent"
                  @click="$emit('salida-origen', exp)"
                  title="Confirmar salida real en origen y fecha de zarpe"
                >
                  <i class="pi pi-send"></i>
                  <span>Salida Origen</span>
                </button>

                <!-- 2. Confirmar Arribo a Puerto (solo si ya zarpó y no ha llegado a puerto) -->
                <button
                  v-if="hasZarpeConfirmado(exp) && !exp.fechaArriboAduana && !isCerrado(exp)"
                  type="button"
                  class="btn-card-action btn-action-success"
                  @click="$emit('arribo-destino', exp)"
                  title="Confirmar arribo al puerto de destino y terminal"
                >
                  <i class="pi pi-compass"></i>
                  <span>Confirmar Arribo</span>
                </button>

                <!-- 3. Editar datos de travesía -->
                <button
                  v-if="hasZarpeConfirmado(exp) && !isCerrado(exp)"
                  type="button"
                  class="btn-card-action"
                  @click="$emit('transito', exp)"
                  title="Actualizar datos de travesía, buque o precinto"
                >
                  <i class="pi pi-globe"></i>
                  <span>Tránsito</span>
                </button>

                <!-- Indicador de espera si ya arribó -->
                <span
                  v-if="exp.fechaArriboAduana && !exp.canalSunat && !isCerrado(exp)"
                  class="pill-waiting-canal"
                  title="Mercancía en puerto nacional. Esperando asignación de Canal SUNAT por Supervisor"
                >
                  <i class="pi pi-spin pi-spinner"></i>
                  <span>Esperando Canal</span>
                </span>
              </template>

              <!-- Exportación en Tránsito -->
              <template v-else>
                <button
                  v-if="exp.servicioTransporte && !exp.idDespachoTransporte && (exp.estadoExpediente === 'ASIGNADO' || exp.estadoExpediente === 'EN_TRAMITE')"
                  type="button"
                  class="btn-card-action btn-action-accent"
                  @click="$emit('recojo-exportacion', exp)"
                  title="Solicitar Recojo de Carga en Planta"
                >
                  <i class="pi pi-truck"></i>
                  <span>Recojo</span>
                </button>

                <button
                  v-if="exp.estadoExpediente === 'ASIGNADO' || (exp.estadoExpediente === 'EN_TRAMITE' && !exp.fechaArriboAduana)"
                  type="button"
                  class="btn-card-action"
                  @click="$emit('ingreso-origen', exp)"
                  title="Confirmar Ingreso a Depósito Temporal (VGM)"
                >
                  <i class="pi pi-inbox"></i>
                  <span>Ingreso Almacén</span>
                </button>

                <button
                  v-if="exp.estadoExpediente !== 'CERRADO'"
                  type="button"
                  class="btn-card-action"
                  @click="$emit('transito', exp)"
                  title="Actualizar datos de embarque"
                >
                  <i class="pi pi-globe"></i>
                  <span>Tránsito</span>
                </button>
              </template>
            </div>
          </template>

          <!-- CASO 3: PESTAÑA 'INSPECCIÓN' / 'AFORO' -> SOLO DILIGENCIA -->
          <template v-else-if="activeTab === 'inspeccion'">
            <button
              type="button"
              class="btn-informe-completo"
              @click="$emit('detalle', exp)"
              title="Ver informe técnico"
            >
              <i class="pi pi-file-edit"></i>
              <span>Informe</span>
            </button>

            <div class="contextual-action-btns">
              <button
                type="button"
                class="btn-icon-secondary"
                @click="$emit('documentos', exp)"
                title="Bóveda de Documentos"
              >
                <i class="pi pi-paperclip"></i>
              </button>

              <button
                v-if="!isCerrado(exp)"
                type="button"
                class="btn-card-action btn-action-warning"
                @click="$emit('diligencia', exp)"
                title="Registrar Acta de Diligencia / Aforo ante SUNAT"
              >
                <i class="pi pi-search-plus"></i>
                <span>Registrar Diligencia</span>
              </button>
            </div>
          </template>

          <!-- CASO 4: PESTAÑA 'LEVANTE' -> SOLO RETIRO Y TRANSPORTE -->
          <template v-else-if="activeTab === 'levante'">
            <button
              type="button"
              class="btn-informe-completo"
              @click="$emit('detalle', exp)"
              title="Ver informe técnico"
            >
              <i class="pi pi-file-edit"></i>
              <span>Informe</span>
            </button>

            <div class="contextual-action-btns">
              <button
                type="button"
                class="btn-icon-secondary"
                @click="$emit('documentos', exp)"
                title="Bóveda de Documentos"
              >
                <i class="pi pi-paperclip"></i>
              </button>

              <template v-if="!isExport(exp)">
                <button
                  v-if="exp.servicioTransporte && !exp.idDespachoTransporte"
                  type="button"
                  class="btn-card-action btn-action-success"
                  @click="$emit('solicitar-transporte', exp)"
                  title="Solicitar Traslado Terrestre a Transporte y Flota"
                >
                  <i class="pi pi-truck"></i>
                  <span>Pedir Envío</span>
                </button>

                <button
                  v-if="!exp.servicioTransporte"
                  type="button"
                  class="btn-card-action"
                  @click="$emit('retiro-directo', exp)"
                  title="Registrar Acta de Retiro Directo en Terminal"
                >
                  <i class="pi pi-check"></i>
                  <span>Retiro Directo</span>
                </button>
              </template>

              <template v-else>
                <button
                  v-if="!exp.observaciones?.includes('EMBARCADO')"
                  type="button"
                  class="btn-card-action btn-action-success"
                  @click="$emit('embarque', exp)"
                  title="Confirmar Embarque Internacional"
                >
                  <i class="pi pi-send"></i>
                  <span>Embarque</span>
                </button>
                <button
                  v-else-if="!exp.observaciones?.includes('RECEPCIÓN DESTINO EXTERIOR')"
                  type="button"
                  class="btn-card-action btn-action-success"
                  @click="$emit('recepcion-exterior', exp)"
                  title="Confirmar Entrega en Destino Exterior"
                >
                  <i class="pi pi-check-circle"></i>
                  <span>Entrega Exterior</span>
                </button>
              </template>
            </div>
          </template>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  expedientes: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  activeTab: {
    type: String,
    default: 'todos'
  }
})

defineEmits([
  'detalle',
  'salida-origen',
  'arribo-destino',
  'transito',
  'diligencia',
  'solicitar-transporte',
  'retiro-directo',
  'ingreso-almacen',
  'recojo-exportacion',
  'ingreso-origen',
  'embarque',
  'recepcion-exterior',
  'documentos'
])

const isTabExpedientes = computed(() => {
  return props.activeTab === 'todos' || props.activeTab === 'expedientes' || !props.activeTab
})

const isExport = (exp) => {
  return (exp?.regimenAduanero || '').toUpperCase().includes('EXPORT')
}

const isCerrado = (exp) => {
  return exp?.estadoExpediente === 'CERRADO' || exp?.estadoExpediente === 'ANULADO'
}

const hasZarpeConfirmado = (exp) => {
  const obs = exp?.observaciones || ''
  return (
    obs.includes('SALIDA ORIGEN CONFIRMADA') ||
    exp?.estadoExpediente === 'EN_TRAMITE' ||
    exp?.estadoExpediente === 'INSPECCION' ||
    exp?.estadoExpediente === 'LEVANTE_AUTORIZADO'
  )
}

const formatEstado = (st) => {
  const map = {
    ASIGNADO: 'Asignado',
    EN_TRAMITE: 'En Tránsito',
    INSPECCION: 'En Inspección',
    LEVANTE_AUTORIZADO: 'Levante Autorizado',
    REGULARIZADO: 'Regularizado',
    CERRADO: 'Cerrado',
    ANULADO: 'Anulado'
  }
  return map[st] || st || 'Asignado'
}

const getEstadoChipClass = (st) => {
  const map = {
    ASIGNADO: 'chip-info',
    EN_TRAMITE: 'chip-transit',
    INSPECCION: 'chip-warning',
    LEVANTE_AUTORIZADO: 'chip-success',
    REGULARIZADO: 'chip-success',
    CERRADO: 'chip-neutral',
    ANULADO: 'chip-danger'
  }
  return map[st] || 'chip-neutral'
}

const getCanalIcon = (canal) => {
  if (canal === 'VERDE') return 'pi pi-check-circle'
  if (canal === 'NARANJA') return 'pi pi-file'
  if (canal === 'ROJO') return 'pi pi-exclamation-triangle'
  return 'pi pi-clock'
}

const getCardClass = (exp) => {
  if (exp.canalSunat === 'ROJO') return 'border-canal-rojo'
  if (exp.canalSunat === 'NARANJA') return 'border-canal-naranja'
  if (exp.canalSunat === 'VERDE') return 'border-canal-verde'
  return ''
}

const getFechaHito = (exp) => {
  if (exp.fechaArriboAduana) {
    const d = new Date(exp.fechaArriboAduana)
    return isNaN(d) ? exp.fechaArriboAduana : `${d.toLocaleDateString()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  }
  if (exp.fechaLlegadaEta) return `ETA: ${exp.fechaLlegadaEta}`
  if (exp.fechaZarpeEtd) return `ETD: ${exp.fechaZarpeEtd}`
  return 'Por programar'
}

const getProgressTitle = (exp) => {
  if (exp.estadoExpediente === 'LEVANTE_AUTORIZADO') return 'Fase 5/5: Levante Autorizado'
  if (exp.canalSunat) return 'Fase 4/5: Canal SUNAT asignado'
  if (exp.fechaArriboAduana) return 'Fase 3/5: Arribado a destino'
  if (hasZarpeConfirmado(exp)) return 'Fase 2/5: En tránsito'
  return 'Fase 1/5: Asignado al agente'
}
</script>

<style scoped>
.agente-expedientes-canvas {
  width: 100%;
}

.canvas-loading-state,
.canvas-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3.5rem 1rem;
  background: var(--color-bg-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md, 8px);
  text-align: center;
  gap: 0.75rem;
  color: var(--color-text-muted);
}

.empty-icon-wrap {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-subtle);
}

.canvas-empty-state h4 {
  margin: 0;
  font-size: var(--font-size-lg, 1.125rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.canvas-empty-state p {
  margin: 0;
  font-size: var(--font-size-base, 0.875rem);
  color: var(--color-text-muted);
  max-width: 420px;
}

/* Grilla de Canvas */
.expedientes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 1.25rem;
}

.expediente-canvas-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 10px);
  padding: 1.15rem;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  transition: transform var(--transition-normal, 0.2s ease), box-shadow var(--transition-normal, 0.2s ease), border-color var(--transition-normal, 0.2s ease);
  position: relative;
  box-shadow: var(--shadow-subtle);
  color: var(--color-text-main);
}

.expediente-canvas-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-border-strong);
}

.border-canal-verde {
  border-left: 4px solid var(--color-success-text);
}

.border-canal-naranja {
  border-left: 4px solid var(--color-warning-text);
}

.border-canal-rojo {
  border-left: 4px solid var(--color-danger-text);
}

/* Header */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.header-badges {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.regimen-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.regimen-pill.import {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.regimen-pill.export {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.canal-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 800;
  letter-spacing: 0.03em;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

.canal-verde {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.canal-naranja {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.canal-rojo {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.canal-pendiente {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.estado-chip {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 600;
  padding: 0.2rem 0.55rem;
  border-radius: 12px;
}

.chip-info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
}

.chip-transit {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.chip-warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.chip-success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.chip-neutral {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

.chip-danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
}

/* Identity */
.card-identity {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
  padding-bottom: 0.6rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.code-and-dam {
  display: flex;
  flex-direction: column;
}

.codigo-expediente {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-text-main);
  cursor: pointer;
  letter-spacing: -0.01em;
  transition: color var(--transition-fast, 0.15s ease);
}

.codigo-expediente:hover {
  color: var(--color-accent-copper);
  text-decoration: underline;
}

.cotizacion-ref {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-subtle);
  font-family: var(--font-mono);
}

.dam-badge {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
}

.dam-lbl {
  font-size: 0.62rem;
  font-weight: 700;
  color: var(--color-text-subtle);
  letter-spacing: 0.05em;
}

.dam-val {
  font-size: 0.78rem;
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--color-text-main);
}

/* Client */
.card-client-section {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.client-name {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.client-icon {
  font-size: 0.85rem;
  color: var(--color-text-subtle);
  flex-shrink: 0;
}

.client-ruc {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-subtle);
  padding-left: 1.3rem;
}

/* Logistics Box */
.card-logistics-box {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm, 6px);
  padding: 0.65rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.logistics-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.logistics-col {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
}

.log-lbl {
  font-size: 0.68rem;
  color: var(--color-text-subtle);
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.log-val {
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.date-val {
  color: var(--color-accent-kicker);
}

/* Stepper Progress */
.card-stepper-progress {
  padding: 0.35rem 0;
}

.stepper-track {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
}

.stepper-track::before {
  content: '';
  position: absolute;
  top: 6px;
  left: 6px;
  right: 6px;
  height: 2px;
  background: var(--color-border);
  z-index: 1;
}

.stepper-step {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
}

.step-dot {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: var(--color-bg-surface);
  border: 2px solid var(--color-border);
  transition: all var(--transition-normal, 0.2s ease);
}

.stepper-step.done .step-dot {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.step-name {
  font-size: 0.62rem;
  color: var(--color-text-subtle);
  font-weight: 600;
}

.stepper-step.done .step-name {
  color: var(--color-text-main);
  font-weight: 700;
}

/* Footer Actions */
.card-actions-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-top: auto;
  padding-top: 0.65rem;
  border-top: 1px solid var(--color-border-subtle);
}

.btn-informe-completo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.45rem 0.85rem;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-primary-text);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-informe-completo:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  transform: translateY(-1px);
}

.full-width-btn {
  flex: 1;
}

.contextual-action-btns {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.btn-icon-secondary {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-icon-secondary:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  border-color: var(--color-border-strong);
}

.btn-card-action {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.4rem 0.65rem;
  border-radius: var(--radius-sm, 6px);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-card-action:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.btn-action-accent {
  border-color: var(--color-info-border);
  background: var(--color-info-bg);
  color: var(--color-info-text);
}

.btn-action-accent:hover {
  filter: brightness(0.95);
}

.btn-action-success {
  border-color: var(--color-success-border);
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.btn-action-success:hover {
  filter: brightness(0.95);
}

.btn-action-warning {
  border-color: var(--color-warning-border);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.btn-action-warning:hover {
  filter: brightness(0.95);
}

.pill-waiting-canal {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.35rem 0.55rem;
  border-radius: 6px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.font-mono {
  font-family: var(--font-mono);
}
</style>
