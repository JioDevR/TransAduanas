<template>
  <Dialog
    :visible="visible"
    modal
    :header="cotizacion ? `Historial de Abonos — ${cotizacion.codigoCotizacion}` : 'Historial de Abonos'"
    :style="{ width: getDialogWidth('880px') }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="cotizacion" class="sga-dialog-body">
      <div class="sga-hist-summary-grid">
        <div class="sga-meta-item">
          <span class="sga-meta-label">CLIENTE IMPORTADOR</span>
          <span class="sga-meta-val">{{ cotizacion.clienteRazonSocial }}</span>
          <span class="sga-cell-sub font-mono">Doc: {{ cotizacion.clienteNumeroDocumento || '—' }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">TOTAL COTIZADO</span>
          <span class="sga-total-amount font-mono">{{ formatUsd(cotizacion.totalCotizado) }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">TOTAL ABONADO</span>
          <span class="sga-total-amount font-mono sga-text-success">{{ formatUsd(cotizacion.totalAbonado) }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">SALDO PENDIENTE</span>
          <span class="sga-total-amount font-mono sga-text-warn">
            {{ formatUsd(cotizacion.saldoPendiente != null ? cotizacion.saldoPendiente : cotizacion.totalCotizado) }}
          </span>
        </div>
      </div>

      <div class="sga-hist-header">
        <span class="sga-form-label">Comprobantes y Depósitos Bancarios Registrados</span>
        <Button
          type="button"
          label="Agregar Abono"
          icon="pi pi-plus"
          size="small"
          @click="$emit('add-abono', cotizacion)"
        />
      </div>

      <DataTable
        :value="pagos"
        :loading="loading"
        responsiveLayout="scroll"
        dataKey="idPago"
      >
        <template #empty>
          <div class="sga-empty-state">
            <i class="pi pi-wallet sga-empty-icon"></i>
            <p class="sga-empty-title">Sin pagos registrados</p>
            <p class="sga-empty-desc">Aún no se han registrado abonos bancarios para esta cotización.</p>
          </div>
        </template>

        <Column header="N° PAGO">
          <template #body="{ data: p }">
            <div class="sga-cell-stack">
              <span class="sga-code-cell font-mono">{{ p.codigoPago }}</span>
              <span class="sga-cell-sub font-mono">{{ formatDateTime(p.fechaRegistro) }}</span>
            </div>
          </template>
        </Column>

        <Column header="MONTO">
          <template #body="{ data: p }">
            <div class="sga-cell-stack">
              <span class="sga-amount-cell font-mono sga-text-success">
                {{ p.moneda === 'PEN' ? formatPen(p.montoAbonado) : formatUsd(p.montoAbonado) }}
              </span>
              <span v-if="p.moneda === 'USD' && p.montoEquivalentePen" class="sga-cell-sub font-mono">
                ≈ {{ formatPen(p.montoEquivalentePen) }}
              </span>
            </div>
          </template>
        </Column>

        <Column header="BANCO & OPERACIÓN">
          <template #body="{ data: p }">
            <div class="sga-cell-stack">
              <span class="sga-cell-primary">{{ p.bancoDestino }}</span>
              <span class="sga-cell-sub font-mono">Op: {{ p.numeroOperacion }} ({{ p.fechaOperacion }})</span>
            </div>
          </template>
        </Column>

        <Column header="ESTADO">
          <template #body="{ data: p }">
            <Tag
              :value="getEstadoAnticipoLabel(p.estadoPago)"
              :severity="getEstadoAnticipoSeverity(p.estadoPago)"
              rounded
            />
          </template>
        </Column>

        <Column header="ACCIONES" headerClass="text-right" bodyClass="text-right">
          <template #body="{ data: p }">
            <div class="sga-row-actions">
              <Button
                v-if="p.idDocumentoComprobante || p.documentoComprobanteUrl"
                type="button"
                label="Voucher"
                icon="pi pi-file-pdf"
                severity="secondary"
                outlined
                size="small"
                @click="$emit('preview-voucher', p)"
              />
              <template v-if="p.estadoPago === 'PENDIENTE_REVISION'">
                <Button
                  type="button"
                  icon="pi pi-check"
                  severity="success"
                  outlined
                  size="small"
                  title="Aprobar abono"
                  @click="$emit('validate', p, 'APROBAR')"
                />
                <Button
                  type="button"
                  icon="pi pi-eye"
                  severity="warn"
                  outlined
                  size="small"
                  title="Observar abono"
                  @click="$emit('validate', p, 'OBSERVAR')"
                />
              </template>
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <template #footer>
      <div class="sga-dialog-footer">
        <Button
          type="button"
          label="Cerrar"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('update:visible', false)"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import { useResponsive } from '@/shared/composables/useResponsive'
import { formatDateTime } from '@/shared/utils/dateUtils'
import {
  formatPen,
  formatUsd,
  getEstadoAnticipoLabel,
  getEstadoAnticipoSeverity
} from '../../utils/finanzasFormatters'

defineProps({
  visible: { type: Boolean, default: false },
  cotizacion: { type: Object, default: null },
  pagos: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'add-abono', 'preview-voucher', 'validate'])

const { getDialogWidth } = useResponsive()
</script>

<style scoped>
.sga-hist-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.75rem;
  padding: 0.9rem 1rem;
  border-radius: 10px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.sga-hist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.sga-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.35rem;
}
</style>
