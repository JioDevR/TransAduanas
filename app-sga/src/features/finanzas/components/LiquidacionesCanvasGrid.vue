<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando liquidaciones financieras...</span>
    </div>

    <div v-else-if="!liquidaciones || liquidaciones.length === 0" class="sga-canvas-state">
      <i class="pi pi-receipt sga-state-icon"></i>
      <p class="sga-state-title">No hay liquidaciones registradas</p>
      <p class="sga-state-desc">Emita una liquidación aduanera vinculada a un expediente operativo.</p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="l in liquidaciones"
        :key="l.idLiquidacion"
        :class="['sga-canvas-card', { 'is-selected': selection?.idLiquidacion === l.idLiquidacion }]"
        @click="$emit('update:selection', l)"
        @dblclick="$emit('detalle', l)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ l.codigoLiquidacion }}</span>
          <Tag
            :value="formatEstadoLiquidacion(l.estadoPago)"
            :severity="getEstadoLiquidacionSeverity(l.estadoPago)"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="l.clienteRazonSocial">
            {{ l.clienteRazonSocial || 'Cliente no asignado' }}
          </h4>
          <div class="sga-client-meta">
            <span class="sga-client-doc font-mono">{{ l.codigoExpediente || 'EXP-SGA' }}</span>
            <span class="sga-client-doc font-mono">· DAM: {{ l.numeroDam || 'S/N' }}</span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">TRIBUTOS SUNAT</span>
            <span class="sga-meta-val font-mono">{{ formatPen(l.totalTributosSunat) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">SERVICIOS + IGV</span>
            <span class="sga-meta-val font-mono">
              {{ formatPen(Number(l.subtotalImponible || 0) + Number(l.igvMonto || 0)) }}
            </span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">FACTURA</span>
            <span class="sga-meta-val font-mono">{{ l.numeroFactura || 'Sin factura' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">SPOT 12%</span>
            <span class="sga-meta-val font-mono">
              {{ l.aplicaDetraccion ? formatPen(l.montoDetraccion) : 'No aplica' }}
            </span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-total-amount font-mono">{{ formatPen(l.totalGeneralPen) }}</span>
            <span class="sga-cell-sub font-mono">{{ formatUsd(l.totalGeneralUsd) }}</span>
          </div>
          <Button
            v-if="l.estadoPago === 'PENDIENTE'"
            type="button"
            label="Cobrar"
            icon="pi pi-dollar"
            severity="success"
            size="small"
            @click.stop="$emit('pagar', l)"
          />
          <Button
            v-else
            type="button"
            label="Ver"
            icon="pi pi-eye"
            severity="secondary"
            outlined
            size="small"
            @click.stop="$emit('detalle', l)"
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
  formatPen,
  formatUsd,
  formatEstadoLiquidacion,
  getEstadoLiquidacionSeverity
} from '../utils/finanzasFormatters'

defineProps({
  liquidaciones: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'detalle', 'pagar'])
</script>
