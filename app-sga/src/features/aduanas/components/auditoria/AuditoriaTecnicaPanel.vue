<template>
  <div class="sga-auditoria-view fade-in">
    <header class="sga-form-header">
      <div class="sga-auditoria-header-left">
        <Button
          type="button"
          label="Volver"
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          size="small"
          @click="$emit('back')"
        />

        <h2 class="sga-form-title">
          {{ data.isExpedienteView ? 'Auditoría de Expediente' : 'Auditoría Técnica' }}
          <span class="font-mono sga-audit-code">· {{ data.codigoPrincipal }}</span>
        </h2>

        <div class="sga-auditoria-badges">
          <Tag
            :value="data.isAirModal ? 'Aéreo' : 'Marítimo'"
            :icon="data.isAirModal ? 'pi pi-send' : 'pi pi-compass'"
            severity="secondary"
            rounded
          />
          <Tag
            :value="getEstadoAuditoriaLabel(data.estadoNormalizado)"
            :severity="getEstadoAuditoriaSeverity(data.estadoNormalizado)"
            rounded
          />
          <Tag
            v-if="data.canalSunat"
            :value="`Canal ${data.canalSunat}`"
            :severity="getCanalSeverity(data.canalSunat)"
            rounded
          />
          <Tag
            v-if="data.codigoExpediente && !data.isExpedienteView"
            :value="data.codigoExpediente"
            icon="pi pi-folder"
            severity="info"
            class="font-mono"
            rounded
          />
        </div>
      </div>

      <div class="sga-form-actions">
        <template v-if="data.isAuditable">
          <Button
            type="button"
            label="Rechazar"
            icon="pi pi-times"
            severity="danger"
            outlined
            size="small"
            :disabled="processing"
            @click="$emit('reject', data)"
          />
          <Button
            v-if="data.isObservable"
            type="button"
            label="Observar"
            icon="pi pi-file-edit"
            severity="warn"
            outlined
            size="small"
            :disabled="processing"
            @click="$emit('observe', data)"
          />
          <Button
            type="button"
            label="Aprobar Propuesta"
            icon="pi pi-check"
            size="small"
            :loading="processing"
            @click="$emit('approve', data)"
          />
        </template>

        <template v-else-if="data.canAperturar">
          <Button
            v-if="data.tienePagoAprobado"
            type="button"
            label="Aperturar Expediente"
            icon="pi pi-folder-open"
            size="small"
            @click="$emit('open-apertura', data)"
          />
          <Tag
            v-else
            value="Requiere Pago Finanzas"
            icon="pi pi-lock"
            severity="warn"
            rounded
          />
        </template>
      </div>
    </header>

    <div v-if="data.motivoObservacion" class="sga-banner is-warn">
      <i class="pi pi-exclamation-triangle"></i>
      <div>
        <span class="sga-micro-label">MOTIVO DE OBSERVACIÓN / REVISIÓN PREVIA</span>
        <p class="sga-banner-text">{{ data.motivoObservacion }}</p>
      </div>
    </div>

    <div class="sga-split-layout">
      <div class="sga-split-main">
        <AuditoriaFichaGeneralCard :data="data" />

        <AuditoriaControlSunatCard
          v-if="data.isExpedienteView || data.numeroDam"
          :data="data"
        />

        <AuditoriaLiquidacionCard
          :data="data"
          :liquidacion="liquidacion"
        />

        <AuditoriaSectionCard
          v-if="data.observaciones"
          step="04"
          title="Observaciones Operativas & Anotaciones"
          subtitle="Instrucciones registradas para la operación aduanera."
        >
          <p class="sga-obs-text">{{ data.observaciones }}</p>
        </AuditoriaSectionCard>

        <AuditoriaSectionCard
          :step="data.observaciones ? '05' : '04'"
          title="Documentos Sustentatorios y Anexos Aduaneros"
          subtitle="Facturas comerciales, listas de empaque, conocimientos de embarque y resoluciones SUNAT."
        >
          <DocumentosUploadPanel
            mode="live"
            :entidad-tipo="data.entidadTipo"
            :entidad-id="data.entidadId"
            :codigo-entidad="data.codigoPrincipal"
            :titulo-entidad="`${data.codigoPrincipal} — ${data.clienteRazonSocial || 'Cliente'}`"
            @docs-loaded="docsCount = $event"
            @updated="$emit('doc-updated')"
          />
        </AuditoriaSectionCard>
      </div>

      <aside class="sga-split-aside">
        <AuditoriaResumenAside
          :data="data"
          :liquidacion="liquidacion"
          :checklist="checklist"
        />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import DocumentosUploadPanel from '@/shared/components/DocumentosUploadPanel.vue'
import AuditoriaSectionCard from './AuditoriaSectionCard.vue'
import AuditoriaFichaGeneralCard from './AuditoriaFichaGeneralCard.vue'
import AuditoriaControlSunatCard from './AuditoriaControlSunatCard.vue'
import AuditoriaLiquidacionCard from './AuditoriaLiquidacionCard.vue'
import AuditoriaResumenAside from './AuditoriaResumenAside.vue'
import {
  getEstadoAuditoriaLabel,
  getEstadoAuditoriaSeverity,
  getCanalSeverity
} from '../../utils/aduanasFormatters'
import {
  normalizeAuditoriaData,
  calcularLiquidacionAduanera,
  evaluarChecklistAuditoria
} from '../../utils/auditoriaCotizacionUtils'

const props = defineProps({
  cotizacion: { type: Object, default: null },
  expediente: { type: Object, default: null },
  processing: { type: Boolean, default: false }
})

defineEmits(['back', 'approve', 'observe', 'reject', 'open-apertura', 'doc-updated'])

const docsCount = ref(0)

const data = computed(() => normalizeAuditoriaData(props.cotizacion, props.expediente))
const liquidacion = computed(() => calcularLiquidacionAduanera(data.value))
const checklist = computed(() => evaluarChecklistAuditoria(data.value, docsCount.value))
</script>

<style scoped>
.sga-auditoria-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
}

.sga-auditoria-header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.sga-audit-code {
  color: var(--p-primary-color);
}

.sga-auditoria-badges {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.sga-banner-text {
  margin: 0.2rem 0 0;
}

.sga-obs-text {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--color-text-main);
  white-space: pre-line;
}
</style>
