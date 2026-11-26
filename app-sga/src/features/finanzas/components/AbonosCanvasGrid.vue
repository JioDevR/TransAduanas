<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando abonos y comprobantes B2B...</span>
    </div>

    <div v-else-if="!anticipos || anticipos.length === 0" class="sga-canvas-state">
      <i class="pi pi-verified sga-state-icon"></i>
      <p class="sga-state-title">No hay abonos con este criterio</p>
      <p class="sga-state-desc">
        Los comprobantes y pagos registrados desde el Portal B2B o caja aparecerán aquí para revisión contable.
      </p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="a in anticipos"
        :key="a.idPago"
        :class="['sga-canvas-card', { 'is-selected': selection?.idPago === a.idPago }]"
        @click="$emit('update:selection', a)"
        @dblclick="$emit('preview-voucher', a)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ a.codigoPago }}</span>
          <Tag
            :value="getEstadoAnticipoLabel(a.estadoPago)"
            :severity="getEstadoAnticipoSeverity(a.estadoPago)"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="a.clienteRazonSocial">
            {{ a.clienteRazonSocial || 'Cliente no asignado' }}
          </h4>
          <div class="sga-client-meta">
            <span class="sga-client-doc font-mono">Cot: {{ a.codigoCotizacion }}</span>
            <span class="sga-client-doc font-mono">· Doc: {{ a.clienteNumeroDocumento || '—' }}</span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">BANCO DESTINO</span>
            <span class="sga-meta-val">{{ a.bancoDestino || '—' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">N° OPERACIÓN</span>
            <span class="sga-meta-val font-mono">{{ a.numeroOperacion || '—' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">MODALIDAD</span>
            <span class="sga-meta-val">{{ formatTipoAbono(a.tipoAbono) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">FECHA OP.</span>
            <span class="sga-meta-val font-mono">{{ a.fechaOperacion || formatDate(a.fechaRegistro) }}</span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-meta-label">ABONO REPORTADO</span>
            <span v-if="a.moneda === 'USD' && a.montoEquivalentePen" class="sga-cell-sub font-mono">
              Equiv: {{ formatPen(a.montoEquivalentePen) }}
            </span>
          </div>
          <span class="sga-total-amount font-mono sga-text-success">
            {{ a.moneda === 'PEN' ? formatPen(a.montoAbonado) : formatUsd(a.montoAbonado) }}
          </span>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Tag from 'primevue/tag'
import { formatDate } from '@/shared/utils/dateUtils'
import {
  formatPen,
  formatUsd,
  formatTipoAbono,
  getEstadoAnticipoLabel,
  getEstadoAnticipoSeverity
} from '../utils/finanzasFormatters'

defineProps({
  anticipos: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'preview-voucher'])
</script>
