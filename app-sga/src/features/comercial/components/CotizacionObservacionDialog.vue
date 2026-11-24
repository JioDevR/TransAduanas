<template>
  <Dialog
    :visible="visible"
    modal
    header="Observación de Supervisión Aduanera"
    :style="{ width: '540px', maxWidth: '95vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="cotizacion" class="sga-dialog-body">
      <div class="sga-obs-context">
        <div class="sga-obs-meta-row">
          <div class="sga-obs-code-group">
            <Tag
              :value="cotizacion.codigoCotizacion || `COT-${cotizacion.idCotizacion}`"
              severity="warn"
              class="font-mono"
              rounded
            />
            <span v-if="cotizacion.fechaEmision" class="sga-obs-date font-mono">
              {{ cotizacion.fechaEmision }}
            </span>
          </div>

          <span v-if="Number(cotizacion.totalCotizado || 0) > 0" class="sga-obs-amount font-mono">
            ${{ formatUsd(cotizacion.totalCotizado) }}
          </span>
        </div>

        <div class="sga-obs-client">
          <strong class="sga-obs-client-name">
            {{ cotizacion.clienteRazonSocial || 'Cliente Asignado' }}
          </strong>
          <span v-if="cotizacion.clienteNumeroDocumento" class="sga-obs-client-doc font-mono">
            RUC/Doc: {{ cotizacion.clienteNumeroDocumento }}
          </span>
        </div>

        <div>
          <Tag
            v-if="cotizacion.clienteTieneUsuario"
            value="Cuenta B2B Activa · Notificado en Portal"
            icon="pi pi-id-card"
            severity="info"
            rounded
          />
          <Tag
            v-else
            value="Prospecto sin Portal B2B · Coordinación directa"
            icon="pi pi-user"
            severity="secondary"
            rounded
          />
        </div>
      </div>

      <div class="sga-banner is-warn">
        <i class="pi pi-exclamation-triangle sga-obs-banner-icon"></i>
        <div class="sga-obs-banner-content">
          <span class="sga-micro-label">INDICACIONES DE SUPERVISIÓN ADUANERA</span>
          <p class="sga-obs-reason">
            {{ cotizacion.motivoObservacion || 'Se requieren ajustes técnicos o documentarios en la cotización antes de su aprobación.' }}
          </p>
        </div>
      </div>

      <p class="sga-obs-hint">
        Subsane los puntos indicados desde <strong>Corregir Cotización</strong>. Al guardar los cambios, la propuesta volverá automáticamente a Supervisión Aduanera para su reevaluación.
      </p>
    </div>

    <template #footer>
      <div class="sga-dialog-footer sga-obs-footer">
        <Button
          v-if="canNotifyWhatsApp"
          type="button"
          label="Notificar WhatsApp"
          icon="pi pi-whatsapp"
          severity="success"
          outlined
          size="small"
          class="sga-obs-whatsapp-btn"
          @click="$emit('whatsapp', cotizacion)"
        />
        <Button
          type="button"
          label="Cerrar"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('update:visible', false)"
        />
        <Button
          type="button"
          label="Corregir Cotización"
          icon="pi pi-pencil"
          size="small"
          @click="$emit('edit', cotizacion)"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { formatUsd } from '../utils/comercialFormatters'

defineProps({
  visible: { type: Boolean, default: false },
  cotizacion: { type: Object, default: null },
  canNotifyWhatsApp: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'edit', 'whatsapp'])
</script>

<style scoped>
.sga-obs-context {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: var(--sga-subpanel-padding, 1rem 1.15rem);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
}

.sga-obs-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.sga-obs-code-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sga-obs-date {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-obs-amount {
  font-size: 0.95rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-main);
}

.sga-obs-client {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.sga-obs-client-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.sga-obs-client-doc {
  font-size: 0.78rem;
  color: var(--color-text-subtle);
}

.sga-obs-banner-icon {
  margin-top: 0.15rem;
  flex-shrink: 0;
}

.sga-obs-banner-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.sga-obs-reason {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 500;
  line-height: 1.5;
  white-space: pre-line;
}

.sga-obs-hint {
  margin: 0;
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--color-text-muted);
}

.sga-obs-footer {
  width: 100%;
  flex-wrap: wrap;
}

.sga-obs-whatsapp-btn {
  margin-right: auto;
}
</style>
