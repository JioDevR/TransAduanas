<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando cotizaciones comerciales...</span>
    </div>

    <div v-else-if="!cotizaciones || cotizaciones.length === 0" class="sga-canvas-state">
      <i class="pi pi-file-edit sga-state-icon"></i>
      <p class="sga-state-title">No hay cotizaciones registradas</p>
      <p class="sga-state-desc">Haga clic en "+ Nueva Cotización" para emitir una nueva oferta comercial.</p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="c in cotizaciones"
        :key="c.idCotizacion"
        :class="['sga-canvas-card', { 'is-selected': selection?.idCotizacion === c.idCotizacion }]"
        @click="$emit('update:selection', c)"
        @dblclick="$emit('open-item', c)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ c.codigoCotizacion }}</span>
          <Tag
            :value="getCotizacionLabel(c.estado)"
            :severity="getCotizacionSeverity(c.estado)"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="c.clienteRazonSocial">{{ c.clienteRazonSocial || 'Cliente N/A' }}</h4>
          <div class="sga-client-meta">
            <span class="sga-client-doc font-mono">{{ c.clienteNumeroDocumento }}</span>
            <Tag
              :value="c.clienteTieneUsuario ? 'Portal B2B' : 'Presencial'"
              :severity="c.clienteTieneUsuario ? 'info' : 'secondary'"
              class="sga-mini-tag"
            />
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">RÉGIMEN</span>
            <span class="sga-meta-val">{{ formatRegimenShort(c.regimenAduanero) }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">EMISIÓN</span>
            <span class="sga-meta-val font-mono">{{ c.fechaEmision || '-' }}</span>
          </div>

          <div v-if="!isAgenteComercial" class="sga-meta-item">
            <span class="sga-meta-label">ASESOR</span>
            <span class="sga-meta-val">{{ c.usuarioComercialNombre || 'Sin Asignar' }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">EXPEDIENTE</span>
            <span v-if="c.tieneExpediente" class="sga-meta-val font-mono sga-text-success">
              <i class="pi pi-folder"></i> {{ c.codigoExpediente }}
            </span>
            <span v-else-if="c.estado === 'APROBADA' && c.tienePagoAprobado" class="sga-meta-val sga-text-success">
              Pago Validado
            </span>
            <span v-else-if="c.estado === 'APROBADA'" class="sga-meta-val sga-text-warn">
              Requiere Pago
            </span>
            <span v-else class="sga-meta-val sga-text-muted">—</span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <button
              v-if="isCotizacionObservada(c) && c.motivoObservacion"
              type="button"
              class="sga-obs-link"
              @click.stop="$emit('view-observation', c)"
            >
              <i class="pi pi-exclamation-triangle"></i>
              <span>Ver motivo</span>
            </button>
            <template v-else-if="c.estado === 'APROBADA'">
              <span :class="['sga-pay-status', c.totalAbonado > 0 ? 'sga-text-success' : 'sga-text-info']">
                {{ getEstadoPagoLabel(c.estadoPago) }}
              </span>
              <span v-if="c.totalAbonado > 0" class="sga-pay-sub font-mono">
                Abono: ${{ formatUsd(c.totalAbonado) }}
              </span>
            </template>
            <span v-else class="sga-text-muted sga-pay-sub">
              {{ c.validezDias ? `Vigencia ${c.validezDias}d` : 'En gestión' }}
            </span>
          </div>

          <div class="sga-footer-total">
            <Tag
              v-if="Number(c.totalCotizado || 0) <= 0"
              value="Por liquidar"
              severity="warn"
              rounded
            />
            <span v-else class="sga-total-amount font-mono">${{ formatUsd(c.totalCotizado) }}</span>
          </div>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Tag from 'primevue/tag'
import { formatUsd, formatRegimenShort, isCotizacionObservada } from '../utils/comercialFormatters'

defineProps({
  cotizaciones: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  isAgenteComercial: { type: Boolean, default: false },
  getCotizacionLabel: { type: Function, required: true },
  getCotizacionSeverity: { type: Function, required: true },
  getEstadoPagoLabel: { type: Function, required: true }
})

defineEmits(['update:selection', 'open-item', 'view-observation'])
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

.sga-obs-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  background: transparent;
  border: none;
  padding: 0;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-warning-text);
  cursor: pointer;
}
</style>
