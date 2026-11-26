<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando comprobantes emitidos...</span>
    </div>

    <div v-else-if="!facturas || facturas.length === 0" class="sga-canvas-state">
      <i class="pi pi-file-pdf sga-state-icon"></i>
      <p class="sga-state-title">No hay facturas o boletas emitidas</p>
      <p class="sga-state-desc">
        Los comprobantes se generan automáticamente al aprobar un abono o confirmar una liquidación pagada.
      </p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="f in facturas"
        :key="f.idComprobante"
        :class="['sga-canvas-card', { 'is-selected': selection?.idComprobante === f.idComprobante }]"
        @click="$emit('update:selection', f)"
        @dblclick="$emit('preview-pdf', f)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ f.numeroCompleto }}</span>
          <Tag
            :value="f.estado"
            :severity="f.estado === 'ANULADA' ? 'danger' : 'success'"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="f.clienteRazonSocial">
            {{ f.clienteRazonSocial }}
          </h4>
          <div class="sga-client-meta">
            <Tag :value="f.tipoComprobante" severity="secondary" class="sga-mini-tag" />
            <span class="sga-client-doc font-mono">Doc: {{ f.clienteNumeroDocumento }}</span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">REFERENCIA</span>
            <span class="sga-meta-val font-mono">{{ f.codigoReferencia }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">FECHA EMISIÓN</span>
            <span class="sga-meta-val font-mono">{{ formatDateTime(f.fechaEmision) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">BASE GRAVADA</span>
            <span class="sga-meta-val font-mono">{{ formatMonto(f.subtotalGravado, f.moneda) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">IGV (18%)</span>
            <span class="sga-meta-val font-mono">{{ formatMonto(f.igvMonto, f.moneda) }}</span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-total-amount font-mono">{{ formatMonto(f.totalComprobante, f.moneda) }}</span>
            <span v-if="f.moneda === 'USD'" class="sga-cell-sub font-mono">
              Equiv: {{ formatPen(f.totalEquivalentePen) }}
            </span>
          </div>
          <div class="sga-row-actions">
            <Button
              type="button"
              icon="pi pi-eye"
              severity="secondary"
              outlined
              size="small"
              title="Ver PDF"
              @click.stop="$emit('preview-pdf', f)"
            />
            <Button
              type="button"
              icon="pi pi-download"
              size="small"
              title="Descargar PDF"
              @click.stop="$emit('download-pdf', f)"
            />
          </div>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { formatDateTime } from '@/shared/utils/dateUtils'
import { formatPen, formatUsd } from '../utils/finanzasFormatters'

defineProps({
  facturas: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'preview-pdf', 'download-pdf'])

function formatMonto(val, moneda) {
  return String(moneda).toUpperCase() === 'USD' ? formatUsd(val) : formatPen(val)
}
</script>

<style scoped>
.sga-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}
</style>
