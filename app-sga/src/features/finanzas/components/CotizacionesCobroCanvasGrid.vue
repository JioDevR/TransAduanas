<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando cartera de cotizaciones aprobadas...</span>
    </div>

    <div v-else-if="!cotizaciones || cotizaciones.length === 0" class="sga-canvas-state">
      <i class="pi pi-folder-open sga-state-icon"></i>
      <p class="sga-state-title">No se encontraron cotizaciones con este criterio</p>
      <p class="sga-state-desc">
        Las cotizaciones comerciales con aprobación técnica aparecerán aquí para control y cobranza de anticipos.
      </p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="c in cotizaciones"
        :key="c.idCotizacion"
        :class="['sga-canvas-card', { 'is-selected': selection?.idCotizacion === c.idCotizacion }]"
        @click="$emit('update:selection', c)"
        @dblclick="$emit('ver-historial', c)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ c.codigoCotizacion }}</span>
          <Tag
            :value="formatEstadoPagoCotizacionLabel(c.estadoPago)"
            :severity="formatEstadoPagoCotizacionSeverity(c.estadoPago)"
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
            <span class="sga-meta-label">TOTAL COTIZADO</span>
            <span class="sga-meta-val font-mono">{{ formatUsd(c.totalCotizado) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">ABONADO</span>
            <span class="sga-meta-val font-mono sga-text-success">{{ formatUsd(c.totalAbonado) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">SALDO PENDIENTE</span>
            <span class="sga-meta-val font-mono sga-text-warn">
              {{ formatUsd(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado) }}
            </span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">EXPEDIENTE</span>
            <span v-if="c.tieneExpediente" class="sga-meta-val font-mono sga-text-info">{{ c.codigoExpediente }}</span>
            <span v-else-if="c.habilitadoParaExpediente" class="sga-meta-val sga-text-success">Listo para Apertura</span>
            <span v-else class="sga-meta-val sga-text-warn">Requiere Anticipo</span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <Button
            type="button"
            label="Historial"
            icon="pi pi-list"
            severity="secondary"
            outlined
            size="small"
            @click.stop="$emit('ver-historial', c)"
          />
          <Button
            type="button"
            label="Abonar"
            icon="pi pi-wallet"
            size="small"
            @click.stop="$emit('registrar-abono', c)"
          />
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import {
  formatUsd,
  formatEstadoPagoCotizacionLabel,
  formatEstadoPagoCotizacionSeverity
} from '../utils/finanzasFormatters'

defineProps({
  cotizaciones: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'registrar-abono', 'ver-historial'])
</script>
