<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando cotizaciones listas para apertura...</span>
    </div>

    <div v-else-if="!cotizaciones || cotizaciones.length === 0" class="sga-canvas-state">
      <i class="pi pi-check-circle sga-state-icon"></i>
      <p class="sga-state-title">No hay cotizaciones pendientes de apertura</p>
      <p class="sga-state-desc">
        Las cotizaciones aprobadas técnicamente con abono verificado en Finanzas aparecerán aquí para formalizar su expediente.
      </p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="c in cotizaciones"
        :key="c.idCotizacion"
        :class="['sga-canvas-card', { 'is-selected': selection?.idCotizacion === c.idCotizacion }]"
        @click="$emit('update:selection', c)"
        @dblclick="$emit('open-apertura', c)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ c.codigoCotizacion }}</span>
          <Tag value="Listo para Apertura" severity="success" rounded />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="c.clienteRazonSocial">
            {{ c.clienteRazonSocial || 'Cliente no asignado' }}
          </h4>
          <div class="sga-client-meta">
            <span class="sga-client-doc font-mono">RUC/Doc: {{ c.clienteNumeroDocumento || '—' }}</span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">RÉGIMEN</span>
            <span class="sga-meta-val">{{ formatRegimenShort(c.regimenAduanero) }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">TOTAL COTIZADO</span>
            <span class="sga-meta-val font-mono">${{ formatUsd(c.totalCotizado) }}</span>
          </div>

          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">RUTA / PUERTOS</span>
            <span class="sga-meta-val sga-route-val">
              <span>{{ c.puertoEnvioNombre || 'Origen' }}</span>
              <i class="pi pi-arrow-right sga-route-arrow"></i>
              <span>{{ c.puertoDestinoNombre || 'Destino' }}</span>
            </span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-pay-status sga-text-success">
              <i class="pi pi-check-circle"></i>
              Abono Verificado
            </span>
            <span class="sga-pay-sub font-mono">
              ${{ formatUsd(c.totalAbonado) }} USD
            </span>
          </div>

          <div class="sga-footer-total">
            <span class="sga-total-amount font-mono">${{ formatUsd(c.totalCotizado) }}</span>
          </div>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Tag from 'primevue/tag'
import { formatUsd, formatRegimenShort } from '../utils/aduanasFormatters'

defineProps({
  cotizaciones: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'open-apertura'])
</script>

<style scoped>
.sga-route-val {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.sga-route-arrow {
  font-size: 0.7rem;
  color: var(--color-text-subtle);
}

.sga-pay-status {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.sga-pay-sub {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
}
</style>
