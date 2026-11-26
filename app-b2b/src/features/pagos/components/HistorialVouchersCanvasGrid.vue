<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando bitácora de vouchers bancarios...</span>
    </div>

    <div v-else-if="!anticipos || anticipos.length === 0" class="sga-canvas-state">
      <i class="pi pi-folder-open sga-state-icon"></i>
      <p class="sga-state-title">No se encontraron comprobantes</p>
      <p class="sga-state-desc">
        No existen registros que coincidan con los filtros o aún no ha registrado transferencias en USD.
      </p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="p in anticipos"
        :key="p.idPago"
        class="sga-canvas-card"
      >
        <header class="sga-card-top">
          <span class="sga-card-code sga-mono">{{ p.codigoPago }}</span>
          <Tag
            :value="formatEstadoPago(p.estadoPago)"
            :severity="getPagoTagSeverity(p.estadoPago)"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title sga-mono">{{ p.codigoCotizacion || 'Cotización' }}</h4>
          <div class="sga-client-meta">
            <Tag :value="p.tipoAbono || 'ABONO'" severity="secondary" class="sga-mini-tag" />
            <span class="sga-client-doc sga-mono">{{ p.fechaOperacion || '—' }}</span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">BANCO ORIGEN / DESTINO</span>
            <span class="sga-meta-val">
              {{ p.bancoOrigen || 'Emisor' }} → {{ p.bancoDestino || 'SGA' }}
            </span>
          </div>

          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">N° OPERACIÓN</span>
            <span class="sga-meta-val sga-mono">{{ p.numeroOperacion || '—' }}</span>
          </div>
        </div>

        <div
          v-if="(p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO') && p.observacionFinanzas"
          class="sga-banner is-danger obs-mini-banner"
        >
          <i class="pi pi-exclamation-triangle"></i>
          <span>{{ p.observacionFinanzas }}</span>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-kicker">MONTO ABONADO</span>
            <span class="sga-total-amount sga-mono">$ {{ formatCurrency(p.montoAbonado) }}</span>
          </div>

          <div class="voucher-actions">
            <Button
              v-if="p.estadoPago === 'OBSERVADO' || p.estadoPago === 'RECHAZADO'"
              type="button"
              label="Subsanar"
              icon="pi pi-pencil"
              severity="danger"
              size="small"
              @click="$emit('fix-payment', p.idPago)"
            />
            <Button
              v-if="p.idComprobanteFactura"
              type="button"
              :label="p.numeroFacturaEmitida || 'Factura PDF'"
              icon="pi pi-file-pdf"
              severity="success"
              outlined
              size="small"
              @click="$emit('view-factura', p.idComprobanteFactura, p.numeroFacturaEmitida)"
            />
            <Button
              v-if="p.idDocumentoComprobante"
              type="button"
              label="Voucher"
              icon="pi pi-file"
              severity="secondary"
              outlined
              size="small"
              @click="$emit('view-voucher', p.idDocumentoComprobante)"
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
import { formatCurrency, formatEstadoPago } from '@/shared/utils/formatters'
import { getPagoTagSeverity } from '../utils/pagoFormUtils'

defineProps({
  anticipos: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['fix-payment', 'view-voucher', 'view-factura'])
</script>

<style scoped>
.obs-mini-banner {
  padding: 0.45rem 0.65rem;
  font-size: 0.74rem;
}

.voucher-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
</style>
