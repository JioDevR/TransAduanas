<template>
  <div class="tab-container fade-in">
    <div class="tab-content-section">
      <div class="section-header-row">
        <div>
          <span class="badge-kicker">CONCILIACIÓN Y TESORERÍA</span>
          <h3 class="section-title">Registro de Cobranzas y Operaciones Bancarias</h3>
        </div>

        <div class="section-controls">
          <button type="button" class="btn-outline-warm" @click="$emit('refresh')" :disabled="loading">
            <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
            <span>Actualizar</span>
          </button>
        </div>
      </div>

      <div class="table-container">
        <DataTable
          :value="pagosRealizadosList"
          :loading="loading"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idLiquidacion"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-wallet empty-icon"></i>
              <p class="empty-title">No hay pagos registrados aún</p>
              <p class="empty-desc">Los cobros registrados desde las liquidaciones se consolidarán en esta bitácora.</p>
            </div>
          </template>

          <Column header="Fecha Pago">
            <template #body="{ data: p }">
              <span class="font-mono text-xs">{{ formatDateTime(p.fechaPago) }}</span>
            </template>
          </Column>

          <Column header="Liquidación / Factura">
            <template #body="{ data: p }">
              <div class="client-cell">
                <span class="font-mono font-bold text-xs">{{ p.codigoLiquidacion }}</span>
                <span class="client-sub">Factura: {{ p.numeroFactura || 'S/N' }}</span>
              </div>
            </template>
          </Column>

          <Column header="Cliente">
            <template #body="{ data: p }">
              <span class="client-name">{{ p.clienteRazonSocial }}</span>
            </template>
          </Column>

          <Column header="Banco & Operación">
            <template #body="{ data: p }">
              <div class="client-cell">
                <span class="font-bold text-xs text-primary">{{ p.bancoDestino || p.metodoPago || 'TRANSFERENCIA' }}</span>
                <span class="font-mono text-xs text-muted">Op: {{ p.numeroOperacion || '—' }}</span>
              </div>
            </template>
          </Column>

          <Column header="Monto Cobrado">
            <template #body="{ data: p }">
              <span class="font-mono font-bold text-sm text-success">
                {{ formatCurrencyPen(p.totalGeneralPen) }}
              </span>
            </template>
          </Column>

          <Column header="Voucher / Docs" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: p }">
              <button
                type="button"
                class="btn-action-outline"
                title="Ver Vouchers y Comprobantes"
                @click="$emit('open-documentos', p)"
              >
                <i class="pi pi-file-pdf"></i>
                <span>Ver Voucher</span>
              </button>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<script setup>
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

defineProps({
  pagosRealizadosList: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  formatDateTime: {
    type: Function,
    required: true
  },
  formatCurrencyPen: {
    type: Function,
    required: true
  }
})

defineEmits(['refresh', 'open-documentos'])
</script>
