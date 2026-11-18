<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando solicitudes sin asignar...</span>
    </div>

    <div v-else-if="!solicitudes || solicitudes.length === 0" class="sga-canvas-state">
      <i class="pi pi-inbox sga-state-icon"></i>
      <p class="sga-state-title">No hay solicitudes pendientes por asignar</p>
      <p class="sga-state-desc">Las nuevas cotizaciones enviadas desde el Portal B2B aparecerán aquí.</p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="s in solicitudes"
        :key="s.idCotizacion"
        :class="['sga-canvas-card', { 'is-selected': selection?.idCotizacion === s.idCotizacion }]"
        @click="$emit('update:selection', s)"
        @dblclick="$emit('take-item', s)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ s.codigoCotizacion }}</span>
          <Tag
            :value="getCotizacionLabel(s.estado)"
            :severity="getCotizacionSeverity(s.estado)"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="s.clienteRazonSocial">{{ s.clienteRazonSocial || 'Cliente N/A' }}</h4>
          <div class="sga-client-meta">
            <span class="sga-client-doc font-mono">{{ s.clienteNumeroDocumento }}</span>
            <Tag value="Portal B2B" severity="info" class="sga-mini-tag" />
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">RÉGIMEN</span>
            <span class="sga-meta-val">{{ formatRegimenShort(s.regimenAduanero) }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">EMISIÓN</span>
            <span class="sga-meta-val font-mono">{{ s.fechaEmision || '-' }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">MERCANCÍA</span>
            <span class="sga-meta-val" :title="s.mercanciaNombre">{{ s.mercanciaNombre || 'Por definir' }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">RUTA</span>
            <span class="sga-meta-val" :title="`${s.puertoEnvioNombre || 'Origen'} → ${s.puertoDestinoNombre || 'Destino'}`">
              {{ s.puertoEnvioNombre || 'Origen' }} → {{ s.puertoDestinoNombre || 'Destino' }}
            </span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-fob-sub font-mono">FOB: ${{ formatUsd(s.valorFob) }}</span>
            <span v-if="Number(s.montoFleteEstimado || 0) > 0" class="sga-fob-sub font-mono">
              Flete decl.: ${{ formatUsd(s.montoFleteEstimado) }}
            </span>
          </div>

          <div class="sga-footer-total">
            <Tag
              v-if="Number(s.totalCotizado || 0) <= 0"
              value="Por liquidar"
              severity="warn"
              rounded
            />
            <span v-else class="sga-total-amount font-mono">${{ formatUsd(s.totalCotizado) }}</span>
          </div>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Tag from 'primevue/tag'
import { formatUsd, formatRegimenShort } from '../utils/comercialFormatters'

defineProps({
  solicitudes: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  getCotizacionLabel: { type: Function, required: true },
  getCotizacionSeverity: { type: Function, required: true }
})

defineEmits(['update:selection', 'take-item'])
</script>

<style scoped>
.sga-fob-sub {
  font-size: 0.75rem;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-subtle);
}
</style>
