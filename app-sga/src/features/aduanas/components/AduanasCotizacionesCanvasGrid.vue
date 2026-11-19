<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando cotizaciones para supervisión aduanera...</span>
    </div>

    <div v-else-if="!cotizaciones || cotizaciones.length === 0" class="sga-canvas-state">
      <i class="pi pi-check-circle sga-state-icon"></i>
      <p class="sga-state-title">No hay cotizaciones con este criterio</p>
      <p class="sga-state-desc">Las cotizaciones emitidas por los ejecutivos comerciales aparecerán aquí para su validación técnica.</p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="c in cotizaciones"
        :key="c.idCotizacion"
        :class="['sga-canvas-card', { 'is-selected': selection?.idCotizacion === c.idCotizacion }]"
        @click="$emit('update:selection', c)"
        @dblclick="$emit('inspect', c)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ c.codigoCotizacion }}</span>
          <Tag
            :value="getEstadoCotizacionLabel(c.estado)"
            :severity="getEstadoCotizacionSeverity(c.estado)"
            rounded
          />
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
            <span class="sga-meta-label">EMISIÓN</span>
            <span class="sga-meta-val font-mono">{{ c.fechaEmision || '—' }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">COMERCIAL</span>
            <span class="sga-meta-val">{{ c.usuarioComercialNombre || 'Asesor Comercial' }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">EXPEDIENTE</span>
            <span v-if="c.tieneExpediente" class="sga-meta-val font-mono sga-text-success">
              <i class="pi pi-folder"></i> {{ c.codigoExpediente }}
            </span>
            <span v-else-if="c.estado === 'APROBADA' && c.tienePagoAprobado" class="sga-meta-val sga-text-success">
              Listo para Expediente
            </span>
            <span v-else-if="c.estado === 'APROBADA'" class="sga-meta-val sga-text-warn">
              Requiere Pago Finanzas
            </span>
            <span v-else class="sga-meta-val sga-text-muted">—</span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <template v-if="c.estado === 'APROBADA'">
              <span :class="['sga-pay-status', c.totalAbonado > 0 ? 'sga-text-success' : 'sga-text-info']">
                {{ getEstadoPagoLabel(c.estadoPago) }}
              </span>
              <span v-if="c.totalAbonado > 0" class="sga-pay-sub font-mono">
                Abono: ${{ formatUsd(c.totalAbonado) }}
              </span>
            </template>
            <span v-else class="sga-pay-sub">
              {{ c.motivoObservacion ? 'Con observación previa' : 'En evaluación técnica' }}
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
import {
  formatUsd,
  formatRegimenShort,
  getEstadoCotizacionLabel,
  getEstadoCotizacionSeverity,
  getEstadoPagoLabel
} from '../utils/aduanasFormatters'

defineProps({
  cotizaciones: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'inspect'])
</script>

<style scoped>
.sga-pay-status {
  font-size: 0.75rem;
  font-weight: 600;
}

.sga-pay-sub {
  font-size: 0.72rem;
  color: var(--color-text-subtle);
}
</style>
