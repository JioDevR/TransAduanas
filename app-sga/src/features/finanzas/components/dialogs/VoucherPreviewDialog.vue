<template>
  <Dialog
    :visible="visible"
    modal
    :header="anticipo ? `Comprobante Bancario — ${anticipo.codigoPago}` : 'Previsualización de Voucher'"
    :style="{ width: getDialogWidth('860px') }"
    @update:visible="$emit('update:visible', $event)"
    @hide="$emit('close')"
  >
    <div v-if="anticipo" class="sga-dialog-body">
      <!-- Metadatos del Abono -->
      <div class="sga-voucher-meta-grid">
        <div class="sga-meta-item">
          <span class="sga-meta-label">COTIZACIÓN</span>
          <span class="sga-meta-val font-mono">{{ anticipo.codigoCotizacion }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">CLIENTE IMPORTADOR</span>
          <span class="sga-meta-val" :title="anticipo.clienteRazonSocial">
            {{ anticipo.clienteRazonSocial || 'Cliente' }}
          </span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">ENTIDAD / N° OP.</span>
          <span class="sga-meta-val font-mono">{{ anticipo.bancoDestino }} — {{ anticipo.numeroOperacion }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">MONTO ABONADO</span>
          <span class="sga-meta-val font-mono sga-text-success">
            {{ anticipo.moneda === 'PEN' ? formatPen(anticipo.montoAbonado) : formatUsd(anticipo.montoAbonado) }}
          </span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">ESTADO</span>
          <Tag
            :value="getEstadoAnticipoLabel(anticipo.estadoPago)"
            :severity="getEstadoAnticipoSeverity(anticipo.estadoPago)"
            rounded
          />
        </div>
      </div>

      <!-- Visor -->
      <div class="sga-voucher-viewer">
        <div v-if="loading" class="sga-canvas-state">
          <i class="pi pi-spin pi-spinner sga-state-icon"></i>
          <span>Cargando comprobante bancario digital...</span>
        </div>

        <template v-else-if="blobUrl">
          <iframe
            v-if="mimeType.includes('pdf')"
            :src="blobUrl"
            class="sga-voucher-iframe"
            title="Voucher Bancario PDF"
          />
          <div v-else class="sga-voucher-img-wrap">
            <img :src="blobUrl" alt="Voucher Bancario" class="sga-voucher-img" />
          </div>
        </template>

        <div v-else class="sga-canvas-state">
          <i class="pi pi-file-excel sga-state-icon"></i>
          <p class="sga-state-title">No se pudo cargar la vista previa</p>
          <p class="sga-state-desc">El archivo no está disponible o su formato no admite vista previa.</p>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="sga-voucher-footer">
        <Button
          v-if="anticipo?.idDocumentoComprobante"
          type="button"
          label="Descargar"
          icon="pi pi-download"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('download')"
        />

        <div class="sga-dialog-footer">
          <Button
            type="button"
            label="Cerrar"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('close')"
          />

          <template v-if="anticipo?.estadoPago === 'PENDIENTE_REVISION'">
            <Button
              type="button"
              label="Rechazar"
              icon="pi pi-ban"
              severity="danger"
              outlined
              size="small"
              @click="$emit('validate', anticipo, 'RECHAZAR')"
            />
            <Button
              type="button"
              label="Observar"
              icon="pi pi-eye"
              severity="warn"
              outlined
              size="small"
              @click="$emit('validate', anticipo, 'OBSERVAR')"
            />
            <Button
              type="button"
              label="Aprobar Abono"
              icon="pi pi-check"
              severity="success"
              size="small"
              @click="$emit('validate', anticipo, 'APROBAR')"
            />
          </template>
        </div>
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import { useResponsive } from '@/shared/composables/useResponsive'
import {
  formatPen,
  formatUsd,
  getEstadoAnticipoLabel,
  getEstadoAnticipoSeverity
} from '../../utils/finanzasFormatters'

defineProps({
  visible: { type: Boolean, default: false },
  anticipo: { type: Object, default: null },
  blobUrl: { type: String, default: null },
  mimeType: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'close', 'download', 'validate'])

const { getDialogWidth } = useResponsive()
</script>

<style scoped>
.sga-voucher-meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  border-radius: 10px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.sga-voucher-viewer {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-bg-base);
  min-height: 420px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.sga-voucher-iframe {
  width: 100%;
  height: 500px;
  border: none;
}

.sga-voucher-img-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1rem;
  max-height: 500px;
  overflow: auto;
}

.sga-voucher-img {
  max-width: 100%;
  max-height: 460px;
  border-radius: 8px;
  object-fit: contain;
}

.sga-voucher-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  flex-wrap: wrap;
}
</style>
