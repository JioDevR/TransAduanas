<template>
  <section class="sga-card-surface sga-tracking-hero">
    <div class="sga-tracking-hero-top">
      <div class="sga-tracking-hero-main">
        <span class="sga-kicker">
          {{ isExport ? 'SEGUIMIENTO DE EXPORTACIÓN ADUANERA' : 'SEGUIMIENTO DE IMPORTACIÓN ADUANERA' }}
        </span>
        <h2 class="sga-tracking-cargo-title">
          {{ expediente.nombreMercancia || 'Carga General' }}
        </h2>
        <div class="sga-tracking-meta-row">
          <span>
            DAM SUNAT:
            <strong class="sga-mono">{{ expediente.numeroDam || 'En trámite de numeración' }}</strong>
            <button
              v-if="expediente.numeroDam"
              type="button"
              class="sga-copy-btn"
              title="Copiar número de DAM"
              aria-label="Copiar número de DAM"
              @click="$emit('copy-code', expediente.numeroDam, 'N° DAM')"
            >
              <i class="pi pi-copy"></i>
            </button>
          </span>
          <span class="sga-tracking-meta-sep" aria-hidden="true">•</span>
          <span>{{ formatRegimen(expediente.regimenAduanero) || (isExport ? 'Exportación' : 'Importación') }}</span>
          <template v-if="expediente.nombreTipoCarga">
            <span class="sga-tracking-meta-sep" aria-hidden="true">•</span>
            <span>{{ expediente.nombreTipoCarga }}</span>
          </template>
          <template v-if="expediente.numeroContenedor">
            <span class="sga-tracking-meta-sep" aria-hidden="true">•</span>
            <span>Contenedor: <strong class="sga-mono">{{ expediente.numeroContenedor }}</strong></span>
          </template>
        </div>
      </div>

      <div class="sga-tracking-hero-status">
        <div class="sga-tracking-tags">
          <Tag :value="phaseMeta.label" :severity="phaseMeta.severity" :icon="phaseMeta.icon" rounded />
          <Tag :value="canalMeta.label" :severity="canalMeta.severity" rounded />
        </div>
        <div class="sga-tracking-progress-summary">
          <span>
            {{ progress.completedCount }} de {{ progress.totalSteps }} hitos completados
          </span>
          <span class="sga-tracking-progress-pct">{{ progress.percentage }}%</span>
        </div>
      </div>
    </div>

    <!-- Barra continua de avance -->
    <div
      class="sga-progress-track"
      role="progressbar"
      :aria-valuenow="progress.percentage"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-label="`Avance del expediente: ${progress.percentage}%`"
    >
      <div
        :class="[
          'sga-progress-fill',
          {
            'is-finished': progress.isFinished,
            'is-cancelled': expediente.estadoExpediente === 'ANULADO'
          }
        ]"
        :style="{ width: `${progress.percentage}%` }"
      />
    </div>

    <!-- Tira de Ruta Logística -->
    <div class="sga-route-strip" aria-label="Resumen de ruta logística">
      <div
        v-for="node in routeNodes"
        :key="node.key"
        :class="[
          'sga-route-node',
          {
            'is-reached': node.status === 'reached',
            'is-active': node.status === 'active'
          }
        ]"
      >
        <span class="sga-route-icon">
          <i :class="node.icon"></i>
        </span>
        <div class="sga-route-meta">
          <span class="sga-route-kicker">{{ node.kicker }}</span>
          <span class="sga-route-place" :title="node.place">{{ node.place }}</span>
          <span class="sga-route-sub" :title="node.sub">{{ node.sub }}</span>
        </div>
      </div>
    </div>

    <!-- Stepper conectado de 6 hitos -->
    <nav class="sga-milestone-track" aria-label="Hitos operativos del expediente">
      <div
        v-for="step in timelineSteps"
        :key="step.num"
        :class="[
          'sga-milestone-item',
          step.completed ? 'is-completed' : (step.active ? 'is-active' : 'is-pending')
        ]"
      >
        <span class="sga-milestone-dot">
          <i v-if="step.completed" class="pi pi-check"></i>
          <span v-else>{{ step.num }}</span>
        </span>
        <div class="sga-milestone-body">
          <span class="sga-milestone-step">HITO 0{{ step.num }}</span>
          <span class="sga-milestone-label">{{ step.label }}</span>
          <span class="sga-milestone-detail sga-mono">{{ step.detail }}</span>
        </div>
      </div>
    </nav>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'
import { formatRegimen } from '@/shared/utils/formatters'
import {
  isExportExpediente,
  getExpedientePhase,
  getCanalMeta,
  buildTimelineSteps,
  calculateTrackingProgress,
  buildRouteSummary
} from '../../utils/expedienteTrackingUtils'

const props = defineProps({
  expediente: { type: Object, required: true }
})

defineEmits(['copy-code'])

const isExport = computed(() => isExportExpediente(props.expediente))
const phaseMeta = computed(() => getExpedientePhase(props.expediente))
const canalMeta = computed(() => getCanalMeta(props.expediente?.canalSunat))
const timelineSteps = computed(() => buildTimelineSteps(props.expediente))
const progress = computed(() => calculateTrackingProgress(timelineSteps.value))
const routeNodes = computed(() => buildRouteSummary(props.expediente))
</script>
