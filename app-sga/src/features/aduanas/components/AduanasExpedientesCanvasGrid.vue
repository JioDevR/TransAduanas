<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando expedientes aduaneros...</span>
    </div>

    <div v-else-if="!expedientes || expedientes.length === 0" class="sga-canvas-state">
      <i class="pi pi-folder-open sga-state-icon"></i>
      <p class="sga-state-title">No hay expedientes aduaneros con este criterio</p>
      <p class="sga-state-desc">
        Los expedientes se generan a partir de cotizaciones comerciales aprobadas con pago verificado.
      </p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="exp in expedientes"
        :key="exp.idExpediente"
        :class="['sga-canvas-card', { 'is-selected': selection?.idExpediente === exp.idExpediente }]"
        @click="$emit('update:selection', exp)"
        @dblclick="$emit('inspect', exp)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ exp.codigoExpediente }}</span>
          <Tag
            :value="exp.estadoExpediente || 'ASIGNADO'"
            :severity="getExpedienteSeverity(exp.estadoExpediente)"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="exp.clienteRazonSocial">
            {{ exp.clienteRazonSocial || 'Cliente no asignado' }}
          </h4>
          <div class="sga-client-meta">
            <span class="sga-client-doc font-mono">RUC/Doc: {{ exp.clienteNumeroDocumento || '—' }}</span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">RÉGIMEN</span>
            <span class="sga-meta-val">{{ formatRegimenShort(exp.regimenAduanero) }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">DAM / CANAL SUNAT</span>
            <div class="sga-dam-inline">
              <span class="sga-meta-val font-mono">{{ exp.numeroDam || 'Sin DAM' }}</span>
              <Tag
                :value="exp.canalSunat || 'PENDIENTE'"
                :severity="getCanalSeverity(exp.canalSunat)"
                rounded
              />
            </div>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">CONTENEDOR / NAVE</span>
            <span class="sga-meta-val font-mono">{{ exp.numeroContenedor || '—' }}</span>
            <span class="sga-sub-val">{{ exp.naveViaje || 'Sin nave asignada' }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">AGENTE ADUANERO</span>
            <span class="sga-meta-val">{{ exp.agenteAduaneroNombre || 'Sin asignar' }}</span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-cot-ref font-mono">{{ exp.codigoCotizacion || '—' }}</span>
          </div>

          <div class="sga-footer-total">
            <span v-if="exp.fechaLevanteAutorizacion" class="sga-levante-ok">
              <i class="pi pi-check-circle"></i>
              Levante Autorizado
            </span>
            <span v-else class="sga-levante-pending">Levante Pendiente</span>
          </div>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Tag from 'primevue/tag'
import {
  formatRegimenShort,
  getCanalSeverity,
  getExpedienteSeverity
} from '../utils/aduanasFormatters'

defineProps({
  expedientes: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'inspect'])
</script>

<style scoped>
.sga-dam-inline {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.sga-sub-val {
  font-size: 0.72rem;
  color: var(--color-text-subtle);
}

.sga-cot-ref {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-subtle);
}

.sga-levante-ok {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-success-text);
}

.sga-levante-pending {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}
</style>
