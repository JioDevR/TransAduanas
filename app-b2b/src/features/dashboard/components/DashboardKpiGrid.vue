<template>
  <section class="sga-kpis-grid" aria-label="Indicadores principales">
    <article class="sga-kpi-card" @click="$emit('navigate', '/cotizaciones')">
      <div class="sga-kpi-head">
        <span class="sga-kpi-index">01</span>
        <span class="sga-kpi-label">Cotizaciones</span>
        <i class="pi pi-file-edit sga-kpi-icon"></i>
      </div>
      <div class="sga-kpi-stat">
        <span class="sga-kpi-value">{{ cotizacionStats.total }}</span>
        <span class="sga-kpi-pill" :class="{ 'is-success': cotizacionStats.aprobada > 0 }">
          {{ cotizacionStats.aprobada }} aprobadas
        </span>
      </div>
      <p class="sga-kpi-foot">
        {{ cotizacionStats.pendiente }} en evaluación ·
        <strong class="sga-mono">$ {{ formatCurrency(cotizacionStats.totalCotizadoUsd) }}</strong>
      </p>
    </article>

    <article class="sga-kpi-card" @click="$emit('navigate', '/expedientes')">
      <div class="sga-kpi-head">
        <span class="sga-kpi-index">02</span>
        <span class="sga-kpi-label">Expedientes Activos</span>
        <i class="pi pi-box sga-kpi-icon"></i>
      </div>
      <div class="sga-kpi-stat">
        <span class="sga-kpi-value">{{ expedienteStats.abiertos }}</span>
        <span class="sga-kpi-pill is-success">
          {{ expedienteStats.conLevante }} con levante
        </span>
      </div>
      <p class="sga-kpi-foot">
        {{ expedienteStats.cerrados }} finalizados de {{ expedienteStats.total }} totales
      </p>
    </article>

    <article
      class="sga-kpi-card"
      :class="cobrosStats.saldoPendienteUsd > 0 ? 'is-warn' : 'is-success'"
      @click="$emit('navigate', '/pagos')"
    >
      <div class="sga-kpi-head">
        <span class="sga-kpi-index">03</span>
        <span class="sga-kpi-label">Saldo Pendiente (USD)</span>
        <i class="pi pi-wallet sga-kpi-icon"></i>
      </div>
      <div class="sga-kpi-stat">
        <span class="sga-kpi-value is-compact">$ {{ formatCurrency(cobrosStats.saldoPendienteUsd) }}</span>
        <span
          class="sga-kpi-pill"
          :class="cobrosStats.amortizacionPct >= 100 ? 'is-success' : 'is-warn'"
        >
          {{ cobrosStats.amortizacionPct }}% pagado
        </span>
      </div>
      <p class="sga-kpi-foot">
        Amortizado: <strong class="sga-mono">$ {{ formatCurrency(cobrosStats.totalAbonadoUsd) }}</strong>
      </p>
    </article>

    <article
      class="sga-kpi-card"
      :class="alertasTotales > 0 ? 'is-danger' : 'is-success'"
      @click="$emit('navigate', alertasTotales > 0 && cobrosStats.pagosObservados > 0 ? '/pagos?tab=observados' : '/expedientes')"
    >
      <div class="sga-kpi-head">
        <span class="sga-kpi-index">04</span>
        <span class="sga-kpi-label">Atención Requerida</span>
        <i
          :class="[
            'pi sga-kpi-icon',
            alertasTotales > 0 ? 'pi-exclamation-triangle' : 'pi-check-circle'
          ]"
        ></i>
      </div>
      <div class="sga-kpi-stat">
        <span class="sga-kpi-value">{{ alertasTotales }}</span>
        <span
          class="sga-kpi-pill"
          :class="alertasTotales > 0 ? 'is-danger' : 'is-success'"
        >
          {{ alertasTotales > 0 ? 'Requiere acción' : 'Al día' }}
        </span>
      </div>
      <p class="sga-kpi-foot">
        {{ cobrosStats.pagosObservados }} pagos obs. · {{ cotizacionStats.observada }} cot. obs.
      </p>
    </article>
  </section>
</template>

<script setup>
import { formatCurrency } from '@/shared/utils/formatters'

defineProps({
  cotizacionStats: { type: Object, required: true },
  expedienteStats: { type: Object, required: true },
  cobrosStats: { type: Object, required: true },
  alertasTotales: { type: Number, default: 0 }
})

defineEmits(['navigate'])
</script>
