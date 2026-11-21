<template>
  <Dialog
    :visible="visible"
    modal
    dismissableMask
    :style="{ width: 'min(940px, 95vw)' }"
    :header="dialogHeader"
    @update:visible="(val) => { if (!val) $emit('close') }"
  >
    <div v-if="expediente" class="tracking-dialog-body">
      <!-- Cabecera DAM + Semáforo SUNAT -->
      <section class="sga-card-surface dam-summary-card">
        <div class="dam-summary-main">
          <span class="sga-kicker">DECLARACIÓN ADUANERA DE MERCANCÍAS</span>
          <h3 class="dam-number sga-mono">
            {{ expediente.numeroDam || 'En Proceso de Numeración' }}
          </h3>
          <div class="dam-meta-row">
            <span class="sga-mono meta-chip">{{ expediente.codigoExpediente }}</span>
            <span>Cotización: <strong class="sga-mono">{{ expediente.codigoCotizacion || '—' }}</strong></span>
            <span>Agente SGA: <strong>{{ expediente.agenteAduaneroNombre || 'Asignado' }}</strong></span>
          </div>
        </div>

        <div class="canal-box">
          <Tag
            :value="canalMeta.label"
            :severity="canalMeta.severity"
            rounded
          />
          <p class="canal-desc">{{ canalMeta.description }}</p>
        </div>
      </section>

      <!-- Línea de Tiempo Operativa (6 pasos sgaUI) -->
      <section class="timeline-block">
        <span class="sga-kicker">LÍNEA DE TIEMPO OPERATIVA</span>
        <nav class="sga-stepper-bar timeline-stepper-6" aria-label="Hitos del expediente">
          <div
            v-for="step in timelineSteps"
            :key="step.num"
            :class="[
              'sga-stepper-item timeline-step',
              { 'is-completed': step.completed, 'is-active': !step.completed && step.active }
            ]"
          >
            <span class="sga-step-circle">
              <i v-if="step.completed" class="pi pi-check"></i>
              <span v-else>{{ step.num }}</span>
            </span>
            <div class="sga-step-meta">
              <span class="sga-step-kicker">{{ step.label }}</span>
              <span class="sga-step-title">{{ step.detail }}</span>
            </div>
          </div>
        </nav>
      </section>

      <!-- Bloques Operativos: Almacén Temporal & Transporte Local -->
      <section class="ops-two-cols">
        <article class="sga-card-surface op-card">
          <div class="op-head">
            <span class="op-icon"><i class="pi pi-building"></i></span>
            <div>
              <h4 class="op-title">Depósito Temporal & Almacenaje</h4>
              <span class="op-sub">{{ expediente.depositoTemporal || 'DP World / APM Terminals' }}</span>
            </div>
          </div>

          <template v-if="expediente.tieneAlmacen">
            <div class="op-rows">
              <div class="op-row">
                <span>Ubicación / Bahía</span>
                <strong class="sga-mono">{{ expediente.ubicacionBahia || 'Bahía Primaria' }}</strong>
              </div>
              <div class="op-row">
                <span>Días Libres Pactados</span>
                <strong class="sga-mono">{{ expediente.diasLibresAlmacen || 15 }} días</strong>
              </div>
            </div>

            <div :class="['sga-banner', getAlmacenAlertClass(expediente.diasRestantesAlmacen)]">
              <i class="pi pi-clock"></i>
              <div class="almacen-banner-text">
                <strong>
                  {{ expediente.diasRestantesAlmacen != null ? expediente.diasRestantesAlmacen : 15 }} días restantes de sobreestadía libre
                </strong>
                <span class="sga-mono">
                  Límite de retiro: {{ expediente.fechaLimiteRetiro || 'Consultar agente' }}
                </span>
              </div>
            </div>
          </template>

          <p v-else class="op-empty">
            Carga con despacho directo o en tránsito internacional hacia puerto de destino.
          </p>
        </article>

        <article class="sga-card-surface op-card">
          <div class="op-head">
            <span class="op-icon"><i class="pi pi-truck"></i></span>
            <div>
              <h4 class="op-title">Transporte Local & Distribución</h4>
              <span class="op-sub">Despacho de última milla a planta</span>
            </div>
          </div>

          <template v-if="expediente.tieneTransporte">
            <div class="op-rows">
              <div class="op-row">
                <span>Código de Traslado</span>
                <strong class="sga-mono">{{ expediente.codigoTracking || 'DSP-ACTIVO' }}</strong>
              </div>
              <div class="op-row">
                <span>Dirección de Entrega</span>
                <strong>{{ expediente.direccionLlegada || 'Planta del cliente' }}</strong>
              </div>
              <div v-if="expediente.fechaProgramadaTransporte" class="op-row">
                <span>Fecha Programada</span>
                <strong class="sga-mono">{{ formatDateTime(expediente.fechaProgramadaTransporte) }}</strong>
              </div>
              <div class="op-row">
                <span>Estado de Traslado</span>
                <Tag :value="expediente.estadoTransporte || 'PROGRAMADO'" severity="info" rounded />
              </div>
            </div>
          </template>

          <p v-else class="op-empty">
            Coordinación de flota terrestre o retiro en terminal una vez validado el levante aduanero.
          </p>
        </article>
      </section>

      <!-- Bóveda Documental del Despacho -->
      <section class="sga-card-surface docs-card">
        <div class="docs-head">
          <div>
            <span class="sga-kicker">BÓVEDA DOCUMENTAL DEL DESPACHO</span>
            <h4 class="op-title">Documentación digital, DAM timbrada y actas de levante</h4>
          </div>
          <Tag
            v-if="documentos.length > 0"
            :value="`${documentos.length} archivo(s)`"
            severity="secondary"
            class="sga-mono"
            rounded
          />
        </div>

        <div v-if="loadingDocs" class="docs-state">
          <i class="pi pi-spin pi-spinner"></i>
          <span>Consultando bóveda de documentos digitales...</span>
        </div>

        <div v-else-if="documentos.length === 0" class="docs-state">
          <i class="pi pi-folder-open"></i>
          <span>No se registran documentos adjuntos en este expediente todavía.</span>
        </div>

        <ul v-else class="docs-list">
          <li v-for="doc in documentos" :key="doc.idDocumento" class="doc-row">
            <div class="doc-info">
              <i :class="[getDocumentIcon(doc.formatoMime), 'doc-icon']"></i>
              <div class="doc-meta">
                <strong class="doc-name">{{ doc.nombreOriginal || doc.descripcion || 'Documento Oficial' }}</strong>
                <span class="doc-sub sga-mono">
                  {{ formatBytes(doc.tamanoBytes) }} • {{ formatDate(doc.fechaSubida) }}
                </span>
              </div>
            </div>

            <div class="doc-actions">
              <Button
                type="button"
                label="Ver"
                icon="pi pi-eye"
                severity="secondary"
                outlined
                size="small"
                @click="$emit('preview-doc', doc)"
              />
              <Button
                type="button"
                label="Descargar"
                icon="pi pi-download"
                size="small"
                @click="$emit('download-doc', doc)"
              />
            </div>
          </li>
        </ul>
      </section>
    </div>
  </Dialog>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import { formatBytes, formatDate, formatDateTime } from '@/shared/utils/formatters'
import {
  getCanalMeta,
  getAlmacenAlertClass,
  buildTimelineSteps,
  getDocumentIcon
} from '../utils/expedienteTrackingUtils'

const props = defineProps({
  visible: { type: Boolean, default: false },
  expediente: { type: Object, default: null },
  documentos: { type: Array, default: () => [] },
  loadingDocs: { type: Boolean, default: false }
})

defineEmits(['close', 'preview-doc', 'download-doc'])

const dialogHeader = computed(() => {
  if (!props.expediente) return 'Tracking de Expediente'
  return `Expediente ${props.expediente.codigoExpediente} — ${props.expediente.nombreMercancia || 'Carga General'}`
})

const canalMeta = computed(() => getCanalMeta(props.expediente?.canalSunat))
const timelineSteps = computed(() => buildTimelineSteps(props.expediente))
</script>

<style scoped>
.tracking-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
  font-family: var(--font-sans);
}

.dam-summary-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 1.15rem 1.35rem;
}

.dam-summary-main {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.dam-number {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.dam-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.meta-chip {
  padding: 0.12rem 0.5rem;
  border-radius: var(--radius-pill);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  font-weight: 700;
  color: var(--color-text-main);
}

.canal-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.35rem;
  max-width: 280px;
  text-align: right;
}

.canal-desc {
  margin: 0;
  font-size: 0.75rem;
  line-height: 1.35;
  color: var(--color-text-muted);
}

.timeline-block {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.timeline-stepper-6 {
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  padding: 0.85rem 1rem;
}

.timeline-step {
  cursor: default;
  padding: 0.25rem;
}

.timeline-step:hover {
  background: transparent;
}

.ops-two-cols {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.op-card,
.docs-card {
  padding: 1.15rem 1.35rem;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.op-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.op-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-accent-kicker);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.op-title {
  margin: 0;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.op-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.op-rows {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.op-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.op-row strong {
  color: var(--color-text-main);
  text-align: right;
}

.almacen-banner-text {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  font-size: 0.78rem;
}

.op-empty {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
  line-height: 1.45;
}

.docs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.docs-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem;
  font-size: 0.82rem;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md);
}

.docs-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.doc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.doc-info {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  min-width: 0;
}

.doc-icon {
  font-size: 1.15rem;
  color: var(--color-accent-kicker);
  flex-shrink: 0;
}

.doc-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.doc-name {
  font-size: 0.82rem;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.doc-actions {
  display: flex;
  gap: 0.4rem;
  flex-shrink: 0;
}

@media (max-width: 780px) {
  .ops-two-cols {
    grid-template-columns: 1fr;
  }

  .canal-box {
    align-items: flex-start;
    text-align: left;
  }

  .doc-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
