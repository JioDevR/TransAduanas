<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando cuentas por pagar y saldos pendientes...</span>
    </div>

    <div v-else-if="!obligaciones || obligaciones.length === 0" class="sga-canvas-state">
      <i class="pi pi-check-circle sga-state-icon sga-text-success"></i>
      <p class="sga-state-title">No registra saldos pendientes de pago</p>
      <p class="sga-state-desc">
        Todas sus operaciones, cotizaciones aprobadas y despachos aduaneros están al día.
      </p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="o in obligaciones"
        :key="`${o.idReferencia}-${o.tipoObligacion}`"
        class="sga-canvas-card"
      >
        <header class="sga-card-top">
          <span class="sga-card-code sga-mono">{{ o.codigo }}</span>
          <Tag
            :value="formatEstadoPago(o.estadoPago)"
            :severity="getPagoTagSeverity(o.estadoPago)"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="o.descripcion">
            {{ o.descripcion || 'Operación de Comercio Exterior' }}
          </h4>
          <div class="sga-client-meta">
            <Tag
              :value="getTipoObligacionMeta(o.tipoObligacion).label"
              :severity="getTipoObligacionMeta(o.tipoObligacion).severity"
              class="sga-mini-tag"
            />
            <span v-if="o.numeroDam" class="sga-client-doc sga-mono">
              DAM {{ o.numeroDam }}
            </span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">TOTAL CONTRATADO</span>
            <span class="sga-meta-val sga-mono">$ {{ formatCurrency(o.totalContratadoUsd) }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">AMORTIZADO</span>
            <span class="sga-meta-val sga-mono sga-text-success">
              $ {{ formatCurrency(o.totalAmortizadoUsd) }}
            </span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-kicker">SALDO PENDIENTE</span>
            <span class="sga-total-amount sga-mono sga-text-warn">
              $ {{ formatCurrency(o.saldoPendienteUsd) }}
            </span>
          </div>

          <Button
            v-if="o.estadoPago === 'OBSERVADO' || o.estadoPago === 'RECHAZADO'"
            type="button"
            label="Subsanar"
            icon="pi pi-pencil"
            severity="danger"
            size="small"
            @click="$emit('fix-payment', o)"
          />
          <Button
            v-else
            type="button"
            :label="isCotizacionObligacion(o.tipoObligacion) ? 'Abonar' : 'Amortizar'"
            :icon="isCotizacionObligacion(o.tipoObligacion) ? 'pi pi-wallet' : 'pi pi-credit-card'"
            size="small"
            @click="$emit('pay', o)"
          />
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { formatCurrency, formatEstadoPago } from '@/shared/utils/formatters'
import { getPagoTagSeverity, getTipoObligacionMeta, isCotizacionObligacion } from '../utils/pagoFormUtils'

defineProps({
  obligaciones: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['pay', 'fix-payment'])
</script>
