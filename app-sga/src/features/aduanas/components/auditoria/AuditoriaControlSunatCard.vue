<template>
  <AuditoriaSectionCard
    icon="pi pi-shield"
    title="Control Aduanero SUNAT, Declaración DAM & Levante"
    subtitle="Trazabilidad de despacho aduanero, canal de aforo asignado y datos de manifiesto."
  >
    <template #head-right>
      <Tag
        v-if="data.canalSunat"
        :value="`CANAL ${data.canalSunat}`"
        :severity="getCanalSeverity(data.canalSunat)"
        rounded
      />
    </template>

    <div class="sga-audit-grid-3">
      <div class="sga-audit-field">
        <span class="sga-form-label">Declaración Aduanera (DAM)</span>
        <span class="sga-audit-val font-mono is-strong sga-text-accent">
          {{ data.numeroDam || 'Pendiente de numeración' }}
        </span>
      </div>

      <div class="sga-audit-field">
        <span class="sga-form-label">Estado de Levante Aduanero</span>
        <span v-if="data.fechaLevanteAutorizacion" class="sga-audit-val is-strong sga-text-success">
          <i class="pi pi-check-circle"></i>
          Levante Autorizado ({{ formatIsoDate(data.fechaLevanteAutorizacion) }})
        </span>
        <span v-else class="sga-audit-val is-strong sga-text-warn">
          <i class="pi pi-clock"></i>
          Pendiente de Levante
        </span>
      </div>

      <div class="sga-audit-field">
        <span class="sga-form-label">Nave / Embarcación / N° Vuelo</span>
        <span class="sga-audit-val">{{ data.naveViaje || 'Sin nave asignada' }}</span>
      </div>

      <div class="sga-audit-field">
        <span class="sga-form-label">N° Contenedor</span>
        <span class="sga-audit-val font-mono is-strong">{{ data.numeroContenedor || 'Sin contenedor asignado' }}</span>
      </div>

      <div class="sga-audit-field">
        <span class="sga-form-label">Precinto Aduanero</span>
        <span class="sga-audit-val font-mono">{{ data.precintoAduanero || 'Pendiente' }}</span>
      </div>

      <div class="sga-audit-field">
        <span class="sga-form-label">Fechas Estimadas Tránsito</span>
        <span class="sga-audit-val font-mono">
          ETD: {{ formatIsoDate(data.fechaZarpeEtd) }} | ETA: {{ formatIsoDate(data.fechaLlegadaEta) }}
        </span>
      </div>

      <div v-if="data.detalleInspeccion" class="sga-audit-field sga-col-span-full">
        <span class="sga-form-label">Diligencia / Incidencias de Inspección Técnica</span>
        <div class="sga-banner is-info">
          <i class="pi pi-info-circle"></i>
          <span>{{ data.detalleInspeccion }}</span>
        </div>
      </div>
    </div>
  </AuditoriaSectionCard>
</template>

<script setup>
import Tag from 'primevue/tag'
import AuditoriaSectionCard from './AuditoriaSectionCard.vue'
import { formatIsoDate, getCanalSeverity } from '../../utils/aduanasFormatters'

defineProps({
  data: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.sga-audit-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1.125rem 1.25rem;
}

.sga-audit-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 0;
}

.sga-col-span-full {
  grid-column: 1 / -1;
}

.sga-audit-val {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.875rem;
  color: var(--color-text-main);
  word-break: break-word;
}

.sga-audit-val.is-strong {
  font-weight: 600;
}

.sga-text-accent {
  color: var(--p-primary-color);
}

@media (max-width: 900px) {
  .sga-audit-grid-3 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .sga-audit-grid-3 {
    grid-template-columns: 1fr;
  }
}
</style>
