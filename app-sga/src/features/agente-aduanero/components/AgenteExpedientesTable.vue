<template>
  <div class="content-table-card">
    <div class="card-header-bar">
      <div>
        <span class="badge-kicker">BANDEJA OPERATIVA DE CAMPO</span>
        <h3 class="section-title">
          Expedientes Aduaneros Asignados ({{ expedientes.length }})
        </h3>
      </div>
    </div>

    <div class="table-container">
      <DataTable
        :value="expedientes"
        :loading="loading"
        responsiveLayout="scroll"
        class="custom-datatable"
        dataKey="idExpediente"
        :paginator="expedientes.length > 10"
        :rows="10"
      >
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-folder-open empty-icon"></i>
            <p class="empty-title">No hay despachos aduaneros asignados</p>
            <p class="empty-desc">
              Los expedientes son formalizados por el Supervisor Aduanero y asignados al especialista aduanero.
            </p>
          </div>
        </template>

        <template #loading>
          <div class="loading-state">
            <i class="pi pi-spin pi-spinner"></i>
            <span>Cargando expedientes asignados...</span>
          </div>
        </template>

        <!-- Columna 1: Código Expediente y Régimen -->
        <Column header="Expediente" style="min-width: 160px;">
          <template #body="{ data: exp }">
            <div class="exp-code-badge" @click="$emit('detalle', exp)" title="Ver ficha técnica completa">
              <i class="pi pi-folder"></i>
              <span class="font-bold font-mono">{{ exp.codigoExpediente }}</span>
            </div>
            <div class="client-sub font-mono text-2xs mt-1">
              Cotiz: {{ exp.codigoCotizacion || '—' }}
            </div>
            <div class="mt-1">
              <span :class="['badge-regimen', isExport(exp) ? 'badge-export' : 'badge-import']">
                {{ isExport(exp) ? 'EXPORTACIÓN' : 'IMPORTACIÓN' }}
              </span>
            </div>
          </template>
        </Column>

        <!-- Columna 2: Cliente / Consignatario -->
        <Column header="Cliente" style="min-width: 180px;">
          <template #body="{ data: exp }">
            <div class="client-cell">
              <span class="client-name font-bold">{{ exp.clienteRazonSocial || '—' }}</span>
              <span class="client-sub font-mono">Doc: {{ exp.clienteNumeroDocumento || 'S/N' }}</span>
            </div>
          </template>
        </Column>

        <!-- Columna 3: Embarque y Tránsito -->
        <Column header="Embarque & Tránsito" style="min-width: 200px;">
          <template #body="{ data: exp }">
            <div class="client-cell">
              <span class="font-bold text-xs font-main">{{ exp.naveViaje || (isExport(exp) ? 'Nave pendiente de asignación' : 'Sin nave confirmada') }}</span>
              <span class="client-sub font-mono text-2xs">
                Cont: {{ exp.numeroContenedor || 'S/N' }} | Prec: {{ exp.precintoAduanero || 'S/N' }}
              </span>
              <span v-if="exp.fechaZarpeEtd && isExport(exp)" class="client-sub text-2xs text-success">
                Zarpe: {{ formatDateShort(exp.fechaZarpeEtd) }}
              </span>
              <span v-else-if="exp.fechaLlegadaEta" class="client-sub text-2xs text-pending">
                ETA: {{ formatDateShort(exp.fechaLlegadaEta) }}
              </span>
            </div>
          </template>
        </Column>

        <!-- Columna 4: DAM & Canal SUNAT -->
        <Column header="DAM & Canal SUNAT" style="min-width: 150px;">
          <template #body="{ data: exp }">
            <div class="client-cell">
              <span class="font-mono text-xs font-bold font-main">{{ exp.numeroDam || 'Sin DAM numerada' }}</span>
              <Tag
                :value="exp.canalSunat || 'PENDIENTE'"
                :severity="getCanalSeverity(exp.canalSunat)"
                rounded
              />
            </div>
          </template>
        </Column>

        <!-- Columna 5: Estado del Trámite -->
        <Column header="Estado Trámite" style="min-width: 130px;">
          <template #body="{ data: exp }">
            <Tag
              :value="exp.estadoExpediente || 'ASIGNADO'"
              :severity="getExpedienteSeverity(exp.estadoExpediente)"
              rounded
            />
          </template>
        </Column>

        <!-- Columna 6: Tramo Terrestre -->
        <Column header="Tramo Terrestre" style="min-width: 180px;">
          <template #body="{ data: exp }">
            <!-- EXPORTACIÓN -->
            <div v-if="isExport(exp)" class="client-cell">
              <div v-if="exp.servicioTransporte">
                <span class="text-xs font-bold text-info">
                  <i class="pi pi-truck text-xs"></i> Recojo en Planta SGA
                </span>
                <span v-if="exp.codigoTrackingTransporte" class="client-sub font-mono text-2xs">
                  {{ exp.codigoTrackingTransporte }} ({{ exp.estadoTransporte || 'PROGRAMADO' }})
                </span>
                <span v-else class="text-warning text-2xs font-bold">
                  Pendiente Orden Recojo
                </span>
              </div>
              <div v-else>
                <span class="text-xs text-muted-dash">
                  <i class="pi pi-building text-xs"></i> Ingreso por Exportador
                </span>
                <span class="client-sub text-2xs">Flete Terrestre Propio</span>
              </div>
            </div>

            <!-- IMPORTACIÓN -->
            <div v-else class="client-cell">
              <div v-if="exp.servicioTransporte">
                <span class="text-xs font-bold text-info">
                  <i class="pi pi-truck text-xs"></i> Flota SGA Contratada
                </span>
                <span v-if="exp.codigoTrackingTransporte" class="client-sub font-mono text-2xs">
                  {{ exp.codigoTrackingTransporte }} ({{ exp.estadoTransporte || 'PROGRAMADO' }})
                </span>
                <span v-else-if="exp.estadoExpediente === 'LEVANTE_AUTORIZADO'" class="text-warning text-2xs font-bold">
                  Requiere Solicitar Envío
                </span>
                <span v-else class="client-sub text-2xs">
                  Pendiente de Levante
                </span>
              </div>
              <div v-else>
                <span class="text-xs text-muted-dash">
                  <i class="pi pi-building text-xs"></i> Retiro en Terminal
                </span>
                <span class="client-sub text-2xs">Transporte por Cliente</span>
              </div>
            </div>
          </template>
        </Column>

        <!-- Columna 7: Acciones Contextuales -->
        <Column header="Acciones de Campo" headerClass="text-right" bodyClass="text-right" style="min-width: 240px;">
          <template #body="{ data: exp }">
            <AgenteAccionesCell
              :expediente="exp"
              @transito="$emit('transito', $event)"
              @diligencia="$emit('diligencia', $event)"
              @solicitar-transporte="$emit('solicitar-transporte', $event)"
              @retiro-directo="$emit('retiro-directo', $event)"
              @ingreso-almacen="$emit('ingreso-almacen', $event)"
              @recojo-exportacion="$emit('recojo-exportacion', $event)"
              @ingreso-origen="$emit('ingreso-origen', $event)"
              @embarque="$emit('embarque', $event)"
              @recepcion-exterior="$emit('recepcion-exterior', $event)"
              @documentos="$emit('documentos', $event)"
              @detalle="$emit('detalle', $event)"
            />
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import AgenteAccionesCell from './AgenteAccionesCell.vue'

defineProps({
  expedientes: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits([
  'transito',
  'diligencia',
  'solicitar-transporte',
  'retiro-directo',
  'ingreso-almacen',
  'recojo-exportacion',
  'ingreso-origen',
  'embarque',
  'recepcion-exterior',
  'documentos',
  'detalle'
])

const isExport = (exp) => {
  return (exp?.regimenAduanero || '').toUpperCase().includes('EXPORT')
}

const formatDateShort = (dateStr) => {
  if (!dateStr) return '—'
  const clean = dateStr.includes('T') ? dateStr.split('T')[0] : dateStr.split(' ')[0]
  const parts = clean.split('-')
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`
  }
  return dateStr
}

const getCanalSeverity = (canal) => {
  const map = {
    VERDE: 'success',
    NARANJA: 'warning',
    ROJO: 'danger',
    PENDIENTE: 'secondary'
  }
  return map[canal] || 'secondary'
}

const getExpedienteSeverity = (est) => {
  const map = {
    ASIGNADO: 'info',
    EN_TRAMITE: 'info',
    INSPECCION: 'warning',
    LEVANTE_AUTORIZADO: 'success',
    CERRADO: 'secondary',
    ANULADO: 'danger'
  }
  return map[est] || 'info'
}
</script>

<style scoped>
.content-table-card {
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 8px;
  overflow: hidden;
}

.card-header-bar {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border, #444138);
  background: var(--color-bg-subtle, #2e2d27);
}

.badge-kicker {
  font-size: 0.6875rem;
  font-family: var(--font-mono, monospace);
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #e0c287);
  text-transform: uppercase;
}

.section-title {
  margin: 0.25rem 0 0 0;
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--color-text-main, #f6f2ea);
}

.table-container {
  overflow-x: auto;
  background: var(--color-bg-surface, #282723);
}

.empty-state {
  padding: 3rem 1.5rem;
  text-align: center;
  color: var(--color-text-muted, #c4bcab);
}

.empty-icon {
  font-size: 2.5rem;
  margin-bottom: 0.75rem;
  color: var(--color-border, #444138);
}

.empty-title {
  font-weight: 700;
  font-size: 1rem;
  margin: 0 0 0.25rem 0;
  color: var(--color-text-main, #f6f2ea);
}

.empty-desc {
  font-size: 0.8125rem;
  margin: 0;
}

.loading-state {
  padding: 2.5rem;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  color: var(--color-text-muted, #c4bcab);
}

.exp-code-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
  color: var(--color-primary, #d6b476);
  font-size: 0.8125rem;
  transition: opacity 0.15s ease;
}

.exp-code-badge:hover {
  text-decoration: underline;
  opacity: 0.85;
}

.badge-regimen {
  display: inline-block;
  font-size: 0.625rem;
  font-family: var(--font-mono, monospace);
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 3px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.badge-import {
  background: var(--color-info-bg, #172635);
  color: var(--color-info-text, #93c5fd);
  border: 1px solid var(--color-info-border, #254b6d);
}

.badge-export {
  background: var(--color-success-bg, #1c2b1f);
  color: var(--color-success-text, #7ee79d);
  border: 1px solid var(--color-success-border, #2d5738);
}

.client-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.font-main {
  color: var(--color-text-main, #f6f2ea);
}

.client-name {
  font-size: 0.8125rem;
  color: var(--color-text-main, #f6f2ea);
}

.client-sub {
  font-size: 0.6875rem;
  color: var(--color-text-muted, #c4bcab);
}

.text-2xs {
  font-size: 0.625rem;
}

.text-info {
  color: var(--color-info-text, #93c5fd);
}

.text-success {
  color: var(--color-success-text, #7ee79d);
}

.text-pending {
  color: var(--color-info-text, #93c5fd);
}

.text-warning {
  color: var(--color-warning-text, #fcd34d);
}

.text-muted-dash {
  color: var(--color-text-subtle, #968e7e);
}

.mt-1 {
  margin-top: 0.25rem;
}
</style>
