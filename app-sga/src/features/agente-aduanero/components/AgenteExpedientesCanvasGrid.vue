<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando expedientes asignados...</span>
    </div>

    <div v-else-if="!expedientes || expedientes.length === 0" class="sga-canvas-state">
      <i class="pi pi-folder-open sga-state-icon"></i>
      <p class="sga-state-title">
        {{ mode === 'manage' ? 'No hay expedientes abiertos para gestionar' : 'No hay expedientes en esta vista' }}
      </p>
      <p class="sga-state-desc">
        {{
          mode === 'manage'
            ? 'Todos sus expedientes asignados están cerrados o no coinciden con los filtros seleccionados.'
            : 'No se encontraron despachos aduaneros que coincidan con los filtros seleccionados.'
        }}
      </p>
    </div>

    <div v-else :class="['sga-canvas-grid', mode === 'manage' ? 'agente-canvas-grid--compact' : 'agente-canvas-grid']">
      <article
        v-for="exp in expedientes"
        :key="exp.idExpediente"
        class="sga-canvas-card agente-canvas-card"
        @dblclick="$emit('detalle', exp)"
      >
        <header class="sga-card-top">
          <button
            type="button"
            class="sga-code-link font-mono"
            :title="mode === 'manage' ? 'Clic para gestionar hitos del expediente' : 'Clic para ver ficha en lectura'"
            @click="$emit('detalle', exp)"
          >
            {{ exp.codigoExpediente }}
          </button>

          <div class="agente-card-tags">
            <Tag
              :value="getCanalLabel(exp)"
              :severity="getCanalSeverity(exp.canalSunat)"
              class="sga-mini-tag"
              rounded
            />
            <Tag
              :value="formatEstadoAgente(exp.estadoExpediente)"
              :severity="getExpedienteSeverity(exp.estadoExpediente)"
              rounded
            />
          </div>
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="exp.clienteRazonSocial">
            {{ exp.clienteRazonSocial || 'Cliente no especificado' }}
          </h4>
          <div class="sga-client-meta">
            <Tag
              :value="formatRegimenShort(exp.regimenAduanero)"
              :severity="isExportacion(exp) ? 'success' : 'info'"
              class="sga-mini-tag"
              rounded
            />
            <span v-if="mode === 'manage'" class="sga-client-doc font-mono">
              DAM: {{ exp.numeroDam || 'Sin numerar' }}
            </span>
            <span v-else class="sga-client-doc font-mono">
              RUC/Doc: {{ exp.clienteNumeroDocumento || '—' }}
            </span>
          </div>
        </div>

        <!-- MODO LECTURA (Vista panorámica completa) -->
        <template v-if="mode === 'readonly'">
          <div class="sga-card-meta-grid">
            <div class="sga-meta-item">
              <span class="sga-meta-label">DAM SUNAT</span>
              <span class="sga-meta-val font-mono">{{ exp.numeroDam || 'Sin numerar' }}</span>
            </div>

            <div class="sga-meta-item">
              <span class="sga-meta-label">CONTENEDOR</span>
              <span class="sga-meta-val font-mono">{{ exp.numeroContenedor || 'Pendiente' }}</span>
            </div>

            <div class="sga-meta-item">
              <span class="sga-meta-label">NAVE / VIAJE</span>
              <span class="sga-meta-val" :title="exp.naveViaje">{{ exp.naveViaje || 'Pendiente' }}</span>
            </div>

            <div class="sga-meta-item">
              <span class="sga-meta-label">{{ isExportacion(exp) ? 'ZARPE / HITO' : 'ARRIBO / ETA' }}</span>
              <span class="sga-meta-val font-mono">{{ formatFechaHitoCard(exp) }}</span>
            </div>
          </div>

          <div class="agente-mini-stepper" :title="getProgresoResumen(exp)">
            <div class="mini-step is-done">
              <span class="mini-dot"></span>
              <span class="mini-lbl">Asignado</span>
            </div>
            <div :class="['mini-step', { 'is-done': hasZarpeConfirmado(exp) }]">
              <span class="mini-dot"></span>
              <span class="mini-lbl">Tránsito</span>
            </div>
            <div :class="['mini-step', { 'is-done': hasArriboConfirmado(exp) }]">
              <span class="mini-dot"></span>
              <span class="mini-lbl">Arribo</span>
            </div>
            <div :class="['mini-step', { 'is-done': hasCanalAsignado(exp) }]">
              <span class="mini-dot"></span>
              <span class="mini-lbl">Canal</span>
            </div>
            <div :class="['mini-step', { 'is-done': isLevanteAutorizado(exp) }]">
              <span class="mini-dot"></span>
              <span class="mini-lbl">Levante</span>
            </div>
          </div>
        </template>

        <!-- MODO GESTIÓN (Canvas simplificado enfocado en el siguiente hito) -->
        <template v-else>
          <div
            :class="[
              'manage-next-hito',
              getNextInfo(exp).isWaiting ? 'is-waiting' : 'is-ready'
            ]"
          >
            <i :class="[getNextInfo(exp).icon || 'pi pi-flag', 'manage-next-icon']"></i>
            <div class="manage-next-text">
              <span class="manage-next-kicker">
                {{ getNextInfo(exp).isWaiting ? 'EN ESPERA' : 'SIGUIENTE HITO' }}
              </span>
              <span class="manage-next-label">{{ getNextInfo(exp).label }}</span>
            </div>
          </div>
        </template>

        <footer class="sga-card-footer agente-card-footer">
          <span v-if="mode === 'manage'" class="sga-cell-sub font-mono">
            {{ exp.mercanciaNombre || getProgresoResumen(exp) }}
          </span>
          <AgenteAccionesCell
            :expediente="exp"
            :mode="mode"
            @documentos="$emit('documentos', $event)"
            @detalle="$emit('detalle', $event)"
          />
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Tag from 'primevue/tag'
import AgenteAccionesCell from './AgenteAccionesCell.vue'
import {
  isExportacion,
  hasZarpeConfirmado,
  hasArriboConfirmado,
  hasCanalAsignado,
  isLevanteAutorizado,
  getSiguienteAccionAgente
} from '../utils/agenteWorkflowUtils'
import {
  formatRegimenShort,
  formatEstadoAgente,
  getCanalLabel,
  getCanalSeverity,
  getExpedienteSeverity,
  getProgresoResumen,
  formatFechaHitoCard
} from '../utils/agenteFormatters'

defineProps({
  expedientes: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  mode: {
    type: String,
    default: 'readonly' // 'readonly' | 'manage'
  }
})

defineEmits(['detalle', 'documentos'])

const getNextInfo = (exp) => getSiguienteAccionAgente(exp)
</script>

<style scoped>
.agente-canvas-grid {
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 340px), 1fr));
}

.agente-canvas-grid--compact {
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 310px), 1fr));
}

.agente-canvas-card {
  cursor: default;
}

.sga-code-link {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-primary);
  cursor: pointer;
  text-align: left;
}

.sga-code-link:hover {
  text-decoration: underline;
}

.agente-card-tags {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.agente-mini-stepper {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.25rem;
  padding: 0.25rem 0;
}

.mini-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  min-width: 0;
}

.mini-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-bg-surface);
  border: 2px solid var(--color-border-strong);
}

.mini-step.is-done .mini-dot {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.mini-lbl {
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--color-text-subtle);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.mini-step.is-done .mini-lbl {
  color: var(--color-text-main);
  font-weight: 700;
}

.manage-next-hito {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.6rem 0.8rem;
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
}

.manage-next-hito.is-waiting {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
}

.manage-next-icon {
  font-size: 0.95rem;
  color: var(--color-primary);
  flex-shrink: 0;
}

.manage-next-hito.is-waiting .manage-next-icon {
  color: var(--color-warning-text);
}

.manage-next-text {
  display: flex;
  flex-direction: column;
  gap: 0.08rem;
  min-width: 0;
}

.manage-next-kicker {
  font-size: 0.6rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--color-accent-kicker);
}

.manage-next-hito.is-waiting .manage-next-kicker {
  color: var(--color-warning-text);
}

.manage-next-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agente-card-footer {
  margin-top: auto;
  justify-content: space-between;
  gap: 0.5rem;
}
</style>
