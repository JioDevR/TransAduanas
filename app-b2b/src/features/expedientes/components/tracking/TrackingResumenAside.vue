<template>
  <aside class="sga-split-aside tracking-aside-stack">
    <!-- Ficha Ejecutiva del Expediente -->
    <section class="sga-card-surface sga-tracking-card">
      <header class="sga-tracking-card-head">
        <div class="sga-tracking-card-title">
          <span class="sga-tracking-card-icon"><i class="pi pi-id-card"></i></span>
          <div>
            <h3 class="sga-tracking-card-heading">Resumen del Expediente</h3>
            <span class="sga-tracking-card-sub">Datos administrativos y responsables</span>
          </div>
        </div>
      </header>

      <div class="sga-kv-list">
        <div class="sga-kv-row">
          <span class="sga-kv-label">Código</span>
          <span class="sga-kv-val sga-mono">{{ expediente.codigoExpediente }}</span>
        </div>

        <div class="sga-kv-row">
          <span class="sga-kv-label">Estado Operativo</span>
          <span class="sga-kv-val">{{ formatEstadoExpedienteLabel(expediente.estadoExpediente) }}</span>
        </div>

        <div class="sga-kv-row">
          <span class="sga-kv-label">Cotización Origen</span>
          <span class="sga-kv-val">
            <Button
              v-if="expediente.idCotizacion"
              type="button"
              :label="expediente.codigoCotizacion || `#${expediente.idCotizacion}`"
              link
              size="small"
              class="aside-link-btn sga-mono"
              @click="$emit('open-cotizacion', expediente.idCotizacion)"
            />
            <span v-else class="sga-mono">{{ expediente.codigoCotizacion || '—' }}</span>
          </span>
        </div>

        <div class="sga-kv-row">
          <span class="sga-kv-label">Agente Aduanero SGA</span>
          <span class="sga-kv-val">
            <template v-if="expediente.agenteAduaneroNombre">
              {{ expediente.agenteAduaneroNombre }}
            </template>
            <Tag v-else value="Por asignar" severity="warn" class="sga-mini-tag" rounded />
          </span>
        </div>

        <div class="sga-kv-row">
          <span class="sga-kv-label">Fecha de Apertura</span>
          <span class="sga-kv-val sga-mono">{{ formatDate(expediente.fechaApertura) }}</span>
        </div>

        <div v-if="expediente.fechaCierre" class="sga-kv-row">
          <span class="sga-kv-label">Fecha de Cierre</span>
          <span class="sga-kv-val sga-mono sga-text-success">{{ formatDate(expediente.fechaCierre) }}</span>
        </div>

        <div class="sga-kv-row">
          <span class="sga-kv-label">Tiempo de Ciclo</span>
          <span class="sga-kv-val sga-mono">{{ calculateElapsedDays(expediente) }}</span>
        </div>
      </div>
    </section>

    <!-- Bóveda Documental del Despacho -->
    <section class="sga-card-surface sga-tracking-card">
      <header class="sga-tracking-card-head">
        <div class="sga-tracking-card-title">
          <span class="sga-tracking-card-icon"><i class="pi pi-folder-open"></i></span>
          <div>
            <h3 class="sga-tracking-card-heading">Bóveda Documental</h3>
            <span class="sga-tracking-card-sub">DAM timbrada, BL y actas</span>
          </div>
        </div>
        <Tag
          v-if="documentos.length > 0"
          :value="`${documentos.length}`"
          severity="secondary"
          class="sga-mono"
          rounded
        />
      </header>

      <div v-if="loadingDocs" class="sga-inline-empty">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Consultando bóveda digital...</span>
      </div>

      <div v-else-if="documentos.length === 0" class="sga-inline-empty">
        <i class="pi pi-folder"></i>
        <span>No se registran documentos adjuntos en este expediente todavía.</span>
      </div>

      <ul v-else class="sga-doc-list">
        <li v-for="doc in documentos" :key="doc.idDocumento" class="sga-doc-item">
          <div class="sga-doc-info">
            <i :class="[getDocumentIcon(doc.formatoMime), 'sga-doc-icon']"></i>
            <div class="sga-doc-meta">
              <span class="sga-doc-name" :title="doc.nombreOriginal || doc.descripcion">
                {{ doc.nombreOriginal || doc.descripcion || 'Documento Oficial' }}
              </span>
              <span class="sga-doc-sub sga-mono">
                {{ formatBytes(doc.tamanoBytes) }} • {{ formatDate(doc.fechaSubida) }}
              </span>
            </div>
          </div>

          <div class="sga-doc-actions">
            <Button
              type="button"
              icon="pi pi-eye"
              severity="secondary"
              outlined
              size="small"
              title="Previsualizar documento"
              aria-label="Previsualizar documento"
              @click="$emit('preview-doc', doc)"
            />
            <Button
              type="button"
              icon="pi pi-download"
              size="small"
              title="Descargar documento"
              aria-label="Descargar documento"
              @click="$emit('download-doc', doc)"
            />
          </div>
        </li>
      </ul>
    </section>
  </aside>
</template>

<script setup>
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { formatBytes, formatDate } from '@/shared/utils/formatters'
import {
  calculateElapsedDays,
  formatEstadoExpedienteLabel,
  getDocumentIcon
} from '../../utils/expedienteTrackingUtils'

defineProps({
  expediente: { type: Object, required: true },
  documentos: { type: Array, default: () => [] },
  loadingDocs: { type: Boolean, default: false }
})

defineEmits(['preview-doc', 'download-doc', 'open-cotizacion'])
</script>

<style scoped>
.tracking-aside-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.aside-link-btn {
  padding: 0;
  font-size: 0.8125rem;
  font-weight: 700;
}
</style>
