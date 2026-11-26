<template>
  <AuditoriaSectionCard
    step="03"
    title="Liquidación Económica & Valoración Aduanera"
    subtitle="Base imponible FOB/CIF y liquidación de tributos, aranceles y servicios aduaneros."
  >
    <div class="sga-cif-grid">
      <div class="sga-cif-box">
        <span class="sga-micro-label">VALOR FOB</span>
        <span class="sga-cif-val font-mono">${{ formatUsd(liquidacion.valorFob) }}</span>
        <span class="sga-cif-sub">Mercancía declarada</span>
      </div>

      <div class="sga-cif-box">
        <span class="sga-micro-label">FLETE INTERNACIONAL</span>
        <span class="sga-cif-val font-mono">${{ formatUsd(liquidacion.fleteInternacional) }}</span>
        <span class="sga-cif-sub">Flete naviero / aéreo</span>
      </div>

      <div class="sga-cif-box">
        <span class="sga-micro-label">SEGURO ESTIMADO</span>
        <span class="sga-cif-val font-mono">${{ formatUsd(liquidacion.seguroEstimado) }}</span>
        <span class="sga-cif-sub">Póliza o tabla SUNAT</span>
      </div>

      <div class="sga-cif-box is-highlight">
        <span class="sga-micro-label">VALOR CIF (BASE SUNAT)</span>
        <span class="sga-cif-val font-mono">${{ formatUsd(liquidacion.valorCif) }}</span>
        <span class="sga-cif-sub">Base imponible tributaria</span>
      </div>
    </div>

    <div class="sga-breakdown-wrap">
      <table class="sga-breakdown-table">
        <thead>
          <tr>
            <th>Concepto Arancelario / Operativo</th>
            <th class="text-center">Tasa Aplicada</th>
            <th class="text-right">Monto Liquidado ($ USD)</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>
              <strong class="sga-concept-name">Arancel Ad-Valórem</strong>
              <span class="sga-concept-sub">Tasa arancelaria según partida NANDINA</span>
            </td>
            <td class="text-center font-mono">{{ liquidacion.tasaAdValorem }}%</td>
            <td class="text-right font-mono sga-amount-cell">${{ formatUsd(liquidacion.montoAdValorem) }}</td>
          </tr>

          <tr>
            <td>
              <strong class="sga-concept-name">Impuesto General a las Ventas (IGV 16% + IPM 2%)</strong>
              <span class="sga-concept-sub">Tasa oficial SUNAT sobre base CIF + Ad-Valórem</span>
            </td>
            <td class="text-center font-mono">18.00%</td>
            <td class="text-right font-mono sga-amount-cell">${{ formatUsd(liquidacion.montoIgv) }}</td>
          </tr>

          <tr v-if="liquidacion.tasaPercepcion > 0 || liquidacion.montoPercepcion > 0">
            <td>
              <strong class="sga-concept-name">Percepción SUNAT</strong>
              <span class="sga-concept-sub">Régimen de percepción para importaciones</span>
            </td>
            <td class="text-center font-mono">{{ liquidacion.tasaPercepcion }}%</td>
            <td class="text-right font-mono sga-amount-cell">${{ formatUsd(liquidacion.montoPercepcion) }}</td>
          </tr>

          <tr>
            <td>
              <strong class="sga-concept-name">Honorarios de Agencia Aduanera (SGA)</strong>
              <span class="sga-concept-sub">Servicios integrales de agenciamiento aduanero</span>
            </td>
            <td class="text-center sga-text-muted">—</td>
            <td class="text-right font-mono sga-amount-cell">${{ formatUsd(data.montoHonorariosEstimado) }}</td>
          </tr>

          <tr v-if="Number(data.costoTransporteInterno || 0) > 0">
            <td>
              <strong class="sga-concept-name">Flete Terrestre Nacional</strong>
              <span class="sga-concept-sub">Transporte local de carga (Ruta nacional)</span>
            </td>
            <td class="text-center sga-text-muted">—</td>
            <td class="text-right font-mono sga-amount-cell">${{ formatUsd(data.costoTransporteInterno) }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr class="sga-breakdown-total-row">
            <td colspan="2">
              <span class="sga-micro-label">TOTAL ESTIMADO A LIQUIDAR (EXCLUYE FOB)</span>
            </td>
            <td class="text-right">
              <span class="sga-total-amount font-mono">${{ formatUsd(data.totalCotizado) }} USD</span>
            </td>
          </tr>
        </tfoot>
      </table>
    </div>

    <div class="sga-payment-strip">
      <div class="sga-strip-col">
        <span class="sga-micro-label">ESTADO DEL PAGO</span>
        <div>
          <Tag
            :value="data.estadoPago ? getEstadoPagoLabel(data.estadoPago) : data.tienePagoAprobado ? 'Pago Validado' : 'Pendiente'"
            :severity="data.estadoPago ? getEstadoPagoSeverity(data.estadoPago) : data.tienePagoAprobado ? 'success' : 'warn'"
            rounded
          />
        </div>
      </div>

      <div class="sga-strip-col">
        <span class="sga-micro-label">TOTAL ABONADO / ANTICIPO</span>
        <span class="sga-strip-amount font-mono sga-text-success">${{ formatUsd(data.totalAbonado) }}</span>
      </div>

      <div class="sga-strip-col">
        <span class="sga-micro-label">SALDO PENDIENTE</span>
        <span
          class="sga-strip-amount font-mono"
          :class="Number(data.saldoPendiente || 0) > 0 ? 'sga-text-warn' : 'sga-text-muted'"
        >
          ${{ formatUsd(data.saldoPendiente) }}
        </span>
      </div>
    </div>
  </AuditoriaSectionCard>
</template>

<script setup>
import Tag from 'primevue/tag'
import AuditoriaSectionCard from './AuditoriaSectionCard.vue'
import { formatUsd, getEstadoPagoLabel, getEstadoPagoSeverity } from '../../utils/aduanasFormatters'

defineProps({
  data: {
    type: Object,
    required: true
  },
  liquidacion: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.sga-cif-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.sga-cif-box {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.9rem 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
}

.sga-cif-box.is-highlight {
  background: var(--p-highlight-background);
  border-color: var(--p-primary-color);
}

.sga-cif-val {
  font-size: 1.05rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-main);
}

.sga-cif-sub {
  font-size: 0.72rem;
  color: var(--color-text-subtle);
}

.sga-breakdown-wrap {
  width: 100%;
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
}

.sga-breakdown-table {
  width: 100%;
  border-collapse: collapse;
  font-variant-numeric: tabular-nums;
}

.sga-breakdown-table th {
  text-align: left;
  padding: 0.65rem 0.9rem;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border);
}

.sga-breakdown-table td {
  padding: 0.7rem 0.9rem;
  border-bottom: 1px solid var(--color-border-subtle);
  font-size: 0.875rem;
  color: var(--color-text-main);
}

.sga-concept-name {
  display: block;
  font-weight: 600;
}

.sga-concept-sub {
  display: block;
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-breakdown-total-row td {
  border-bottom: none;
  background: var(--color-bg-subtle);
  padding: 0.85rem 0.9rem;
}

.sga-payment-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  padding: var(--sga-subpanel-padding, 1rem 1.25rem);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
}

.sga-strip-col {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.sga-strip-amount {
  font-size: 1rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.text-center {
  text-align: center;
}

.text-right {
  text-align: right;
}

@media (max-width: 900px) {
  .sga-cif-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .sga-cif-grid,
  .sga-payment-strip {
    grid-template-columns: 1fr;
  }
}
</style>
