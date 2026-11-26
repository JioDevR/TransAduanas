<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando cotizaciones de su empresa...</span>
    </div>

    <div v-else-if="!cotizaciones || cotizaciones.length === 0" class="sga-canvas-state">
      <i class="pi pi-folder-open sga-state-icon"></i>
      <p class="sga-state-title">No se encontraron cotizaciones</p>
      <p class="sga-state-desc">Ajuste los filtros o solicite una nueva cotización logística.</p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="c in cotizaciones"
        :key="c.idCotizacion"
        class="sga-canvas-card"
        tabindex="0"
        role="button"
        @click="$emit('view-detail', c)"
        @keydown.enter.self.prevent="$emit('view-detail', c)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code sga-mono">{{ c.codigoCotizacion }}</span>
          <Tag
            :value="formatEstadoCotizacion(c.estado)"
            :severity="getEstadoSeverity(c.estado)"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="c.nombreMercancia">
            {{ c.nombreMercancia || 'Mercancía General' }}
          </h4>
          <div class="sga-client-meta">
            <span class="sga-client-doc sga-mono">{{ c.codigoMercancia || 'Sin partida' }}</span>
            <Tag
              :value="formatRegimen(c.regimenAduanero) || 'Aduana'"
              severity="secondary"
              class="sga-mini-tag"
            />
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">RUTA LOGÍSTICA</span>
            <span class="sga-meta-val">
              {{ c.nombrePuertoEnvio || 'Origen' }} → {{ c.nombrePuertoDestino || 'Destino' }}
            </span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">VALOR FOB</span>
            <span class="sga-meta-val sga-mono">$ {{ formatCurrency(c.valorFob) }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">ESTADO PAGO</span>
            <span :class="['sga-meta-val', getPagoTextClass(c.estadoPago)]">
              {{ formatEstadoPago(c.estadoPago) }}
            </span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-kicker">TOTAL ESTIMADO</span>
            <Tag
              v-if="esPendienteLiquidacion(c)"
              value="Por cotizar"
              severity="warn"
              rounded
            />
            <span v-else class="sga-total-amount sga-mono">$ {{ formatCurrency(c.totalCotizado) }}</span>
          </div>

          <div class="b2b-card-actions" @click.stop @keydown.enter.stop>
            <Button
              v-if="c.estado === 'APROBADA' && !c.aceptadaPorCliente"
              type="button"
              label="Revisar y Autorizar"
              icon="pi pi-check-circle"
              severity="success"
              outlined
              size="small"
              @click="$emit('view', c)"
            />
            <Button
              v-else-if="c.estado === 'APROBADA' && (c.estadoPago === 'OBSERVADO' || c.estadoPago === 'RECHAZADO')"
              type="button"
              label="Subsanar"
              icon="pi pi-pencil"
              severity="danger"
              outlined
              size="small"
              @click="$emit('fix-payment', c)"
            />
            <Button
              v-else-if="c.estado === 'APROBADA' && c.estadoPago !== 'PAGADO_TOTAL'"
              type="button"
              label="Abonar"
              icon="pi pi-wallet"
              severity="success"
              outlined
              size="small"
              @click="$emit('pay', c)"
            />
            <Button
              type="button"
              icon="pi pi-file-pdf"
              severity="secondary"
              outlined
              size="small"
              :aria-label="`Descargar proforma PDF ${c.codigoCotizacion || ''}`.trim()"
              title="Descargar Proforma PDF"
              :loading="downloadingPdfId === c.idCotizacion"
              :disabled="downloadingPdfId != null"
              @click="$emit('download-pdf', c)"
            />
            <Button
              type="button"
              icon="pi pi-eye"
              severity="secondary"
              outlined
              size="small"
              aria-label="Ver detalle"
              title="Ver detalle"
              @click="$emit('view-detail', c)"
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
import { esPendienteLiquidacion } from '../utils/portalCotizacionFormUtils'
import {
  formatCurrency,
  formatRegimen,
  formatEstadoCotizacion,
  formatEstadoPago
} from '@/shared/utils/formatters'

defineProps({
  cotizaciones: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  downloadingPdfId: { type: [Number, String], default: null }
})

defineEmits(['view-detail', 'pay', 'fix-payment', 'download-pdf'])

function getEstadoSeverity(estado) {
  switch (estado) {
    case 'APROBADA':
      return 'success'
    case 'PENDIENTE':
    case 'EN_REVISION':
      return 'warn'
    case 'OBSERVADA':
    case 'RECHAZADA':
      return 'danger'
    default:
      return 'secondary'
  }
}

function getPagoTextClass(estadoPago) {
  switch (estadoPago) {
    case 'PAGADO_TOTAL':
    case 'APROBADO':
    case 'ADELANTO_APROBADO':
      return 'sga-text-success'
    case 'OBSERVADO':
    case 'RECHAZADO':
      return 'sga-text-danger'
    case 'PENDIENTE_REVISION':
      return 'sga-text-info'
    default:
      return 'sga-text-warn'
  }
}
</script>

<style scoped>
.b2b-card-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.sga-text-danger {
  color: var(--color-danger-text);
  font-weight: 600;
}
</style>
