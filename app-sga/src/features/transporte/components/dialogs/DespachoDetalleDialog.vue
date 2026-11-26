<template>
  <Dialog
    :visible="visible"
    modal
    :header="despacho ? `Hoja de Ruta // ${despacho.codigoTracking}` : 'Detalle del Despacho'"
    :style="{ width: getDialogWidth('760px') }"
    @update:visible="(v) => !v && $emit('close')"
  >
    <div v-if="despacho" class="sga-dialog-body">
      <div class="sga-summary-card">
        <div class="sga-dash-header">
          <div class="sga-dash-meta">
            <Tag
              :value="formatEstadoDespacho(despacho.estadoTransporte)"
              :severity="getEstadoDespachoSeverity(despacho.estadoTransporte)"
              rounded
            />
            <Tag
              :value="formatTipoTraslado(despacho.tipoTraslado)"
              :severity="getTipoTrasladoSeverity(despacho.tipoTraslado)"
              rounded
            />
            <span class="sga-code-cell font-mono">{{ despacho.codigoExpediente || 'EXP-SGA' }}</span>
          </div>
          <span class="sga-cell-sub font-mono">
            Prog: {{ formatDateTime(despacho.fechaProgramada) }}
          </span>
        </div>

        <div class="detalle-cliente-row">
          <div>
            <span class="sga-micro-label">CLIENTE / IMPORTADOR-EXPORTADOR</span>
            <div class="sga-cell-primary">{{ despacho.clienteRazonSocial || 'Cliente no especificado' }}</div>
          </div>
          <div v-if="despacho.mercanciaNombre">
            <span class="sga-micro-label">MERCANCÍA & CONTENEDOR</span>
            <div class="sga-cell-meta">
              {{ despacho.mercanciaNombre }}
              <span v-if="despacho.numeroContenedor" class="font-mono"> · {{ despacho.numeroContenedor }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="sga-grid-2">
        <div class="sga-card-surface detalle-box">
          <span class="sga-micro-label">ORIGEN / PARTIDA</span>
          <p class="detalle-text">{{ despacho.direccionPartida }}</p>
          <span v-if="despacho.fechaSalidaRuta" class="sga-cell-sub font-mono">
            Salida: {{ formatDateTime(despacho.fechaSalidaRuta) }}
          </span>
        </div>

        <div class="sga-card-surface detalle-box">
          <span class="sga-micro-label">DESTINO ({{ despacho.distritoLlegada }})</span>
          <p class="detalle-text">{{ despacho.direccionLlegada }}</p>
          <span v-if="despacho.fechaEntregaConforme" class="sga-cell-sub font-mono">
            Entrega: {{ formatDateTime(despacho.fechaEntregaConforme) }}
          </span>
        </div>
      </div>

      <div class="sga-grid-3">
        <div class="sga-card-surface detalle-box">
          <span class="sga-micro-label">UNIDAD VEHICULAR</span>
          <template v-if="despacho.unidadPlacaTracto">
            <strong class="sga-code-cell font-mono">{{ despacho.unidadPlacaTracto }}</strong>
            <span class="sga-cell-sub">{{ despacho.unidadMarcaModelo }}</span>
            <span v-if="despacho.unidadPlacaCarreta" class="sga-cell-sub font-mono">
              Carreta: {{ despacho.unidadPlacaCarreta }}
            </span>
          </template>
          <span v-else class="sga-cell-muted">Pendiente de asignación</span>
        </div>

        <div class="sga-card-surface detalle-box">
          <span class="sga-micro-label">CONDUCTOR & GRE-T</span>
          <template v-if="despacho.conductorNombre">
            <strong class="sga-cell-primary">{{ despacho.conductorNombre }}</strong>
            <span class="sga-cell-sub font-mono">Lic: {{ despacho.conductorLicencia || '—' }}</span>
            <span class="sga-cell-sub font-mono">GRE-T: {{ despacho.guiaRemisionTransporte || 'Sin emitir' }}</span>
          </template>
          <span v-else class="sga-cell-muted">Pendiente de asignación</span>
        </div>

        <div class="sga-card-surface detalle-box">
          <span class="sga-micro-label">RECEPCIÓN EN DESTINO</span>
          <span class="sga-cell-meta">{{ despacho.contactoRecepcion || 'Sin contacto registrado' }}</span>
          <span v-if="despacho.receptorNombre" class="sga-cell-sub">
            Recibió: <strong>{{ despacho.receptorNombre }}</strong> (DNI {{ despacho.receptorDni || '—' }})
          </span>
        </div>
      </div>

      <div v-if="despacho.detalleIncidencia" class="sga-banner is-warn">
        <i class="pi pi-exclamation-triangle"></i>
        <div class="detalle-bitacora">
          <span class="sga-micro-label">BITÁCORA DE INCIDENCIAS</span>
          <pre class="detalle-pre">{{ despacho.detalleIncidencia }}</pre>
        </div>
      </div>

      <div v-if="despacho.observaciones" class="sga-card-surface detalle-box">
        <span class="sga-micro-label">OBSERVACIONES OPERATIVAS</span>
        <p class="detalle-text">{{ despacho.observaciones }}</p>
      </div>

      <div class="sga-dialog-footer">
        <Button
          v-if="despacho.estadoTransporte === 'PENDIENTE_ASIGNACION' || despacho.estadoTransporte === 'PROGRAMADO'"
          type="button"
          :label="despacho.estadoTransporte === 'PROGRAMADO' ? 'Reasignar' : 'Asignar Recursos'"
          icon="pi pi-user-plus"
          severity="secondary"
          outlined
          @click="$emit('asignar', despacho)"
        />
        <Button
          v-if="despacho.estadoTransporte === 'PROGRAMADO'"
          type="button"
          label="Iniciar Ruta"
          icon="pi pi-send"
          @click="$emit('iniciar-ruta', despacho)"
        />
        <Button
          v-if="despacho.estadoTransporte === 'PROGRAMADO' || despacho.estadoTransporte === 'EN_RUTA'"
          type="button"
          label="Incidencia"
          icon="pi pi-exclamation-triangle"
          severity="warn"
          outlined
          @click="$emit('incidencia', despacho)"
        />
        <Button
          v-if="despacho.estadoTransporte === 'EN_RUTA'"
          type="button"
          label="Confirmar Entrega"
          icon="pi pi-check"
          severity="success"
          @click="$emit('entregar', despacho)"
        />
        <Button
          v-if="despacho.estadoTransporte !== 'ENTREGADO' && despacho.estadoTransporte !== 'CANCELADO'"
          type="button"
          label="Anular"
          icon="pi pi-ban"
          severity="danger"
          outlined
          @click="$emit('anular', despacho)"
        />
        <Button
          type="button"
          label="Cerrar"
          severity="secondary"
          @click="$emit('close')"
        />
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { useResponsive } from '@/shared/composables/useResponsive'
import {
  formatDateTime,
  formatEstadoDespacho,
  getEstadoDespachoSeverity,
  formatTipoTraslado,
  getTipoTrasladoSeverity
} from '../../utils/transporteFormatters'

defineProps({
  visible: { type: Boolean, default: false },
  despacho: { type: Object, default: null }
})

defineEmits(['close', 'asignar', 'iniciar-ruta', 'incidencia', 'entregar', 'anular'])
const { getDialogWidth } = useResponsive()
</script>

<style scoped>
.detalle-cliente-row {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  margin-top: 0.85rem;
}

.detalle-box {
  padding: 0.9rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detalle-text {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-main);
  line-height: 1.4;
}

.detalle-bitacora {
  min-width: 0;
  flex: 1;
}

.detalle-pre {
  margin: 0.25rem 0 0;
  font-family: var(--font-mono);
  font-size: 0.78rem;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
