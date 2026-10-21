<template>
  <Dialog
    :visible="visible"
    modal
    :header="`Auditoría Integral de Expediente: ${expediente?.codigoExpediente || ''}`"
    :style="{ width: '840px', maxWidth: '95vw' }"
    class="custom-sga-dialog"
    @update:visible="val => !val && handleClose()"
  >
    <div v-if="expediente" class="expediente-detail-body">
      <!-- Banner Superior de Estado y Canal SUNAT -->
      <div class="exp-header-banner">
        <div class="banner-left">
          <span class="badge-kicker">CONTROL ADUANERO OPERATIVO</span>
          <div class="banner-title-row">
            <span class="exp-code-display">{{ expediente.codigoExpediente }}</span>
            <Tag
              :value="expediente.estadoExpediente || 'ASIGNADO'"
              :severity="getEstadoSeverity(expediente.estadoExpediente)"
              rounded
            />
            <Tag
              v-if="expediente.canalSunat"
              :value="`CANAL ${expediente.canalSunat}`"
              :severity="getCanalSeverity(expediente.canalSunat)"
              rounded
            />
          </div>
          <span class="banner-sub">
            Aperturado el {{ formatDateTime(expediente.fechaApertura) }}
          </span>
        </div>

        <div class="banner-right">
          <div class="dam-box">
            <span class="dam-label">DECLARACIÓN DAM SUNAT</span>
            <span class="dam-number">{{ expediente.numeroDam || 'Pendiente de numeración' }}</span>
          </div>
        </div>
      </div>

      <!-- Ficha de Datos en Grilla Modular -->
      <div class="detail-grid">
        <!-- 1. Cotización y Cliente -->
        <div class="detail-card">
          <div class="card-header">
            <i class="pi pi-building"></i>
            <span>1. Cliente & Operación Comercial</span>
          </div>
          <div class="card-content">
            <div class="info-row">
              <span class="info-label">Cliente Importador/Exportador:</span>
              <span class="info-val strong">{{ expediente.clienteRazonSocial || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">RUC / Documento:</span>
              <span class="info-val font-mono">{{ expediente.clienteNumeroDocumento || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Cotización Origen:</span>
              <span class="info-val doc-link">{{ expediente.codigoCotizacion || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Régimen Aduanero:</span>
              <span class="info-val regimen-badge">{{ formatRegimen(expediente.regimenAduanero) }}</span>
            </div>
          </div>
        </div>

        <!-- 2. Agente Aduanero y Contacto Exterior -->
        <div class="detail-card">
          <div class="card-header">
            <i class="pi pi-user-edit"></i>
            <span>2. Especialistas & Contacto Exterior</span>
          </div>
          <div class="card-content">
            <div class="info-row">
              <span class="info-label">Agente Aduanero Responsable:</span>
              <span class="info-val strong">{{ expediente.agenteAduaneroNombre || 'Agente Asignado' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Contacto en el Exterior:</span>
              <span class="info-val">{{ expediente.contactoExteriorNombre || 'No especificado' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Teléfono Exterior:</span>
              <span class="info-val font-mono">{{ expediente.contactoExteriorTelefono || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Correo Exterior:</span>
              <span class="info-val">{{ expediente.contactoExteriorCorreo || '—' }}</span>
            </div>
          </div>
        </div>

        <!-- 3. Transporte Internacional & Carga -->
        <div class="detail-card">
          <div class="card-header">
            <i class="pi pi-box"></i>
            <span>3. Datos de Transporte & Carga</span>
          </div>
          <div class="card-content">
            <div class="info-row">
              <span class="info-label">N° Contenedor:</span>
              <span class="info-val font-mono strong">{{ expediente.numeroContenedor || 'Sin contenedor asignado' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Precinto Aduanero:</span>
              <span class="info-val font-mono">{{ expediente.precintoAduanero || 'Pendiente' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Nave / Embarcación / Viaje:</span>
              <span class="info-val">{{ expediente.naveViaje || 'Sin nave asignada' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Fecha Zarpe (ETD):</span>
              <span class="info-val">{{ expediente.fechaZarpeEtd || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Fecha Arribo Estimada (ETA):</span>
              <span class="info-val">{{ expediente.fechaLlegadaEta || '—' }}</span>
            </div>
          </div>
        </div>

        <!-- 4. Diligencia SUNAT & Levante Aduanero -->
        <div class="detail-card highlight-card">
          <div class="card-header">
            <i class="pi pi-shield"></i>
            <span>4. Despacho Aduanero & Levante SUNAT</span>
          </div>
          <div class="card-content">
            <div class="info-row">
              <span class="info-label">Canal de Control SUNAT:</span>
              <Tag
                :value="expediente.canalSunat || 'PENDIENTE'"
                :severity="getCanalSeverity(expediente.canalSunat)"
                rounded
              />
            </div>
            <div class="info-row">
              <span class="info-label">Fecha Asignación Canal:</span>
              <span class="info-val">{{ formatDateTime(expediente.fechaAsignacionCanal) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Estado de Levante:</span>
              <span v-if="expediente.fechaLevanteAutorizacion" class="text-success font-bold">
                <i class="pi pi-check-circle"></i> Levante Autorizado ({{ formatDateTime(expediente.fechaLevanteAutorizacion) }})
              </span>
              <span v-else class="text-warning font-bold">
                <i class="pi pi-clock"></i> Pendiente de Levante
              </span>
            </div>
            <div v-if="expediente.detalleInspeccion" class="info-row full-col">
              <span class="info-label">Diligencia / Inspección Aduanera:</span>
              <p class="inspection-text">{{ expediente.detalleInspeccion }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Observaciones e Instrucciones -->
      <div v-if="expediente.observaciones" class="obs-card">
        <i class="pi pi-comment"></i>
        <div>
          <span class="obs-title">Instrucciones Operativas / Observaciones:</span>
          <p class="obs-text">{{ expediente.observaciones }}</p>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-actions-row">
        <button
          type="button"
          class="btn-outline-warm"
          style="border-color: var(--color-accent-copper, #c2410c); color: var(--color-accent-copper, #c2410c);"
          @click="emit('open-docs', expediente)"
        >
          <i class="pi pi-paperclip"></i>
          <span>Documentos Adjuntos</span>
        </button>
        <button type="button" class="btn-outline-warm" @click="handleClose">
          <span>Cerrar</span>
        </button>
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  expediente: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'open-docs'])

const handleClose = () => {
  emit('close')
}

const formatDateTime = (val) => {
  if (!val) return '—'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return val
    return d.toLocaleDateString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return val
  }
}

const formatRegimen = (reg) => {
  if (!reg) return 'IMPORTACIÓN DEFINITIVA'
  const labels = {
    IMPORTACION_DEFINITIVA: 'Importación Definitiva (10)',
    EXPORTACION_DEFINITIVA: 'Exportación Definitiva (40)',
    DEPOSITO_ADUANERO: 'Depósito Aduanero (70)',
    TRANSITO_ADUANERO: 'Tránsito Aduanero (80)',
    ADMISION_TEMPORAL: 'Admisión Temporal (20)'
  }
  return labels[reg] || reg
}

const getEstadoSeverity = (estado) => {
  switch (estado) {
    case 'LEVANTE_AUTORIZADO':
    case 'CERRADO':
      return 'success'
    case 'CON_CANAL':
    case 'EN_TRAMITE':
      return 'info'
    case 'ASIGNADO':
      return 'warn'
    case 'ANULADO':
      return 'danger'
    default:
      return 'secondary'
  }
}

const getCanalSeverity = (canal) => {
  switch (canal) {
    case 'VERDE':
      return 'success'
    case 'NARANJA':
      return 'warn'
    case 'ROJO':
      return 'danger'
    default:
      return 'secondary'
  }
}
</script>

<style scoped>
.expediente-detail-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 0.5rem 0;
}

.exp-header-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.5rem;
  padding: 1.25rem 1.5rem;
  background: var(--color-bg-subtle, #f5f2eb);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  flex-wrap: wrap;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #9a7f60);
  display: block;
  margin-bottom: 0.25rem;
}

.banner-title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.exp-code-display {
  font-family: var(--font-mono, monospace);
  font-size: 1.375rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.banner-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  display: block;
  margin-top: 0.25rem;
}

.dam-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  background: var(--color-bg-surface, #ffffff);
  border: 1px dashed var(--color-border-strong, #c4bcab);
  padding: 0.625rem 1rem;
  border-radius: var(--radius-md, 8px);
}

.dam-label {
  font-size: 0.625rem;
  font-weight: 800;
  color: var(--color-text-subtle);
  letter-spacing: 0.06em;
}

.dam-number {
  font-family: var(--font-mono, monospace);
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

.detail-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  overflow: hidden;
}

.highlight-card {
  border-color: var(--color-border-strong, #d5c8b6);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background: var(--color-bg-subtle, #f5f2eb);
  border-bottom: 1px solid var(--color-border);
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.card-content {
  padding: 0.875rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
  font-size: 0.8125rem;
}

.info-row.full-col {
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
}

.info-label {
  color: var(--color-text-muted);
  font-weight: 500;
}

.info-val {
  color: var(--color-text-main);
  text-align: right;
}

.info-val.strong {
  font-weight: 700;
}

.font-mono {
  font-family: var(--font-mono, monospace);
}

.doc-link {
  font-weight: 700;
  color: var(--color-primary, #352f2a);
}

.regimen-badge {
  font-size: 0.75rem;
  background: var(--color-bg-subtle);
  padding: 0.125rem 0.5rem;
  border-radius: var(--radius-xs, 4px);
  border: 1px solid var(--color-border);
}

.inspection-text {
  font-size: 0.8125rem;
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm, 6px);
  margin-top: 0.25rem;
  width: 100%;
}

.obs-card {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  padding: 0.875rem 1.25rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
}

.obs-card i {
  color: var(--color-accent-kicker);
  margin-top: 0.125rem;
}

.obs-title {
  display: block;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-subtle);
  margin-bottom: 0.25rem;
}

.obs-text {
  font-size: 0.8125rem;
  color: var(--color-text-main);
  margin: 0;
}

.dialog-actions-row {
  display: flex;
  justify-content: flex-end;
  padding-top: 0.5rem;
}

.text-success {
  color: var(--color-success-text, #1e6638);
}

.text-warning {
  color: var(--color-warning-text, #8c5b16);
}
</style>
