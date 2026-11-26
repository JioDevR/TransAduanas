<template>
  <aside class="sga-card-surface informe-aside-card">
    <div class="aside-head">
      <span class="sga-kicker">RESUMEN OPERATIVO</span>
      <div class="aside-code-row">
        <span class="aside-code font-mono">{{ expediente.codigoExpediente }}</span>
        <Tag
          :value="formatEstadoAgente(expediente.estadoExpediente)"
          :severity="getExpedienteSeverity(expediente.estadoExpediente)"
          rounded
        />
      </div>
      <span class="sga-cell-sub">
        Apertura: {{ formatDateTime(expediente.fechaApertura) }}
      </span>
    </div>

    <div :class="['sga-banner', canalBannerClass]">
      <i :class="canalIcon"></i>
      <div>
        <span class="sga-micro-label">{{ getCanalLabel(expediente) }}</span>
        <p class="aside-banner-text">{{ getCanalHelpText(expediente) }}</p>
      </div>
    </div>

    <div class="aside-metrics">
      <div class="aside-row">
        <span class="sga-form-label">Declaración DAM</span>
        <span class="font-mono aside-val">{{ expediente.numeroDam || 'Por numerar' }}</span>
      </div>
      <div class="aside-row">
        <span class="sga-form-label">Fase Actual</span>
        <span class="aside-val">{{ getProgresoResumen(expediente) }}</span>
      </div>
      <div class="aside-row">
        <span class="sga-form-label">Modalidad</span>
        <span class="aside-val">{{ expediente.modalidad || 'MARÍTIMO' }}</span>
      </div>
      <div v-if="expediente.valorFob != null" class="aside-row">
        <span class="sga-form-label">Valor FOB</span>
        <span class="font-mono aside-val">${{ formatUsd(expediente.valorFob) }}</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'
import { formatUsd } from '@/features/aduanas/utils/aduanasFormatters'
import {
  formatEstadoAgente,
  formatDateTime,
  getExpedienteSeverity,
  getCanalLabel,
  getCanalHelpText,
  getProgresoResumen
} from '../../utils/agenteFormatters'

const props = defineProps({
  expediente: { type: Object, required: true }
})

const canalBannerClass = computed(() => {
  const c = props.expediente?.canalSunat
  if (c === 'VERDE') return 'is-success'
  if (c === 'NARANJA') return 'is-warn'
  if (c === 'ROJO') return 'is-danger'
  return 'is-info'
})

const canalIcon = computed(() => {
  const c = props.expediente?.canalSunat
  if (c === 'VERDE') return 'pi pi-check-circle'
  if (c === 'NARANJA') return 'pi pi-file'
  if (c === 'ROJO') return 'pi pi-exclamation-triangle'
  return 'pi pi-clock'
})
</script>

<style scoped>
.informe-aside-card {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.aside-head {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.aside-code-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.aside-code {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-primary);
}

.aside-banner-text {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
}

.aside-metrics {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}

.aside-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
}

.aside-val {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-main);
  text-align: right;
}
</style>
