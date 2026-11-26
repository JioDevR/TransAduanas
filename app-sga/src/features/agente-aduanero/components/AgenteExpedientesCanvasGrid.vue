<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando expedientes asignados...</span>
    </div>

    <div v-else-if="!expedientes || expedientes.length === 0" class="sga-canvas-state">
      <i class="pi pi-folder-open sga-state-icon"></i>
      <p class="sga-state-title">No hay expedientes en esta vista</p>
      <p class="sga-state-desc">
        No se encontraron despachos aduaneros que coincidan con los filtros seleccionados.
      </p>
    </div>

    <div v-else class="sga-canvas-grid agente-canvas-grid">
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
            title="Clic para abrir informe completo"
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
            <span class="sga-client-doc font-mono">
              RUC/Doc: {{ exp.clienteNumeroDocumento || '—' }}
            </span>
            <Tag
              :value="formatRegimenShort(exp.regimenAduanero)"
              :severity="isExportacion(exp) ? 'success' : 'info'"
              class="sga-mini-tag"
              rounded
            />
          </div>
        </div>

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

        <!-- Mini Stepper Operativo (corregido: PENDIENTE no marca Canal) -->
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

        <footer class="sga-card-footer agente-card-footer">
          <AgenteAccionesCell
            :expediente="exp"
            :active-tab="activeTab"
            @salida-origen="$emit('salida-origen', $event)"
            @arribo-destino="$emit('arribo-destino', $event)"
            @transito="$emit('transito', $event)"
            @diligencia="$emit('diligencia', $event)"
            @solicitar-transporte="$emit('solicitar-transporte', $event)"
            @retiro-directo="$emit('retiro-directo', $event)"
            @ingreso-almacen="$emit('ingreso-almacen', $event)"
            @recojo-exportacion="$emit('recojo-exportacion', $event)"
            @ingreso-origen="$emit('ingreso-origen', $event)"
            @embarque="$emit('embarque', $event)"
            @recepcion-exterior="$emit('recepcion-exterior', $event)"
            @verificacion-origen="$emit('verificacion-origen', $event)"
            @cuadrilla-rojo="$emit('cuadrilla-rojo', $event)"
            @finalizar-rojo="$emit('finalizar-rojo', $event)"
            @enviar-sunat="$emit('enviar-sunat', $event)"
            @entregado-almacen-verde="$emit('entregado-almacen-verde', $event)"
            @informe-conformidad="$emit('informe-conformidad', $event)"
            @finalizar-expediente="$emit('finalizar-expediente', $event)"
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
  isLevanteAutorizado
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
  activeTab: { type: String, default: 'todos' }
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
  'verificacion-origen',
  'cuadrilla-rojo',
  'finalizar-rojo',
  'enviar-sunat',
  'entregado-almacen-verde',
  'informe-conformidad',
  'finalizar-expediente',
  'documentos'
])
</script>

<style scoped>
.agente-canvas-grid {
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 340px), 1fr));
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

.agente-card-footer {
  margin-top: auto;
  justify-content: flex-end;
}
</style>
