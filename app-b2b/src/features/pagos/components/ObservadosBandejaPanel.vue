<template>
  <section v-if="anticipos.length > 0" class="observados-section" aria-label="Comprobantes observados">
    <div class="sga-panel-header">
      <div class="sga-panel-title-group">
        <h2 class="sga-panel-title">Requieren Corrección</h2>
        <Tag
          :value="`${anticipos.length} observado(s)`"
          severity="danger"
          rounded
          class="sga-mono"
        />
      </div>
    </div>

    <div class="observed-grid">
      <article
        v-for="p in anticipos"
        :key="p.idPago"
        class="sga-card-surface obs-card"
      >
        <header class="obs-head">
          <div>
            <span class="sga-kicker sga-mono">PAGO OBSERVADO // {{ p.codigoPago }}</span>
            <h3 class="obs-title sga-mono">Cotización: {{ p.codigoCotizacion || 'N/A' }}</h3>
            <span class="obs-date sga-mono">Fecha operación: {{ p.fechaOperacion || 'N/A' }}</span>
          </div>

          <div class="obs-amount-col">
            <span class="sga-micro-label">MONTO DECLARADO</span>
            <strong class="obs-amount sga-mono">$ {{ formatCurrency(p.montoAbonado) }} USD</strong>
            <Tag value="OBSERVADO POR FINANZAS" severity="danger" rounded />
          </div>
        </header>

        <div class="sga-banner is-danger">
          <i class="pi pi-exclamation-triangle"></i>
          <div class="dictamen-body">
            <strong>Motivo de la Observación Contable:</strong>
            <p>
              {{ p.observacionFinanzas || 'El voucher bancario es ilegible o el número de operación no coincide con los extractos bancarios.' }}
            </p>
          </div>
        </div>

        <div class="obs-meta-grid">
          <div class="meta-cell">
            <span class="sga-micro-label">BANCO EMISOR</span>
            <strong>{{ p.bancoOrigen || 'N/A' }}</strong>
          </div>
          <div class="meta-cell">
            <span class="sga-micro-label">BANCO DESTINO SGA</span>
            <strong>{{ p.bancoDestino }}</strong>
          </div>
          <div class="meta-cell">
            <span class="sga-micro-label">N° OPERACIÓN</span>
            <strong class="sga-mono">{{ p.numeroOperacion }}</strong>
          </div>
        </div>

        <footer class="obs-footer">
          <Button
            v-if="p.idDocumentoComprobante"
            type="button"
            label="Ver Voucher Observado"
            icon="pi pi-file"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('view-voucher', p.idDocumentoComprobante)"
          />
          <Button
            type="button"
            label="Subsanar Abono"
            icon="pi pi-pencil"
            severity="danger"
            size="small"
            @click="$emit('fix-payment', p.idPago)"
          />
        </footer>
      </article>
    </div>
  </section>
</template>

<script setup>
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { formatCurrency } from '@/shared/utils/formatters'

defineProps({
  anticipos: { type: Array, default: () => [] }
})

defineEmits(['fix-payment', 'view-voucher'])
</script>

<style scoped>
.observados-section {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.observed-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.obs-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  border-color: var(--color-danger-border);
}

.obs-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
}

.obs-title {
  margin: 0.25rem 0 0.15rem;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.obs-date {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.obs-amount-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.obs-amount {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.dictamen-body {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.dictamen-body p {
  margin: 0;
  font-size: 0.84rem;
}

.obs-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
  padding: 0.75rem 0.9rem;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.meta-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.meta-cell strong {
  font-size: 0.84rem;
  color: var(--color-text-main);
}

.obs-footer {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 0.5rem;
}

@media (max-width: 720px) {
  .obs-meta-grid {
    grid-template-columns: 1fr;
  }

  .obs-amount-col {
    align-items: flex-start;
  }
}
</style>
