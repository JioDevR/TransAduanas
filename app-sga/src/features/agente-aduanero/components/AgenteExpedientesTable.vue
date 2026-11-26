<template>
  <DataTable
    :value="expedientes"
    :loading="loading"
    responsiveLayout="scroll"
    dataKey="idExpediente"
    paginator
    :rows="15"
    :rowsPerPageOptions="[15, 30, 50]"
    currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} expedientes"
    paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
    @row-dblclick="onRowDblClick"
  >
    <template #empty>
      <div class="sga-empty-state">
        <i class="pi pi-folder-open sga-empty-icon"></i>
        <p class="sga-empty-title">No hay despachos aduaneros con este criterio</p>
        <p class="sga-empty-desc">
          Los expedientes son formalizados por el Supervisor Aduanero y asignados al especialista aduanero.
        </p>
      </div>
    </template>

    <Column header="Expediente" style="min-width: 165px;">
      <template #body="{ data: exp }">
        <div class="sga-cell-stack">
          <button
            type="button"
            class="sga-code-link font-mono"
            title="Ver informe técnico completo"
            @click="$emit('detalle', exp)"
          >
            {{ exp.codigoExpediente }}
          </button>
          <span class="sga-cell-sub font-mono">{{ exp.codigoCotizacion || '—' }}</span>
          <Tag
            :value="formatRegimenShort(exp.regimenAduanero)"
            :severity="isExportacion(exp) ? 'success' : 'info'"
            class="sga-mini-tag"
            rounded
          />
        </div>
      </template>
    </Column>

    <Column header="Cliente" style="min-width: 185px;">
      <template #body="{ data: exp }">
        <div class="sga-cell-stack">
          <span class="sga-cell-primary">{{ exp.clienteRazonSocial || '—' }}</span>
          <span class="sga-cell-sub font-mono">Doc: {{ exp.clienteNumeroDocumento || 'S/N' }}</span>
        </div>
      </template>
    </Column>

    <Column header="Embarque & Tránsito" style="min-width: 195px;">
      <template #body="{ data: exp }">
        <div class="sga-cell-stack">
          <span class="sga-cell-primary">
            {{ exp.naveViaje || (isExportacion(exp) ? 'Nave por asignar' : 'Sin nave confirmada') }}
          </span>
          <span class="sga-cell-sub font-mono">
            Cont: {{ exp.numeroContenedor || 'S/N' }} · Prec: {{ exp.precintoAduanero || 'S/N' }}
          </span>
          <span v-if="exp.fechaZarpeEtd && isExportacion(exp)" class="sga-cell-sub sga-text-success">
            Zarpe: {{ formatDateShort(exp.fechaZarpeEtd) }}
          </span>
          <span v-else-if="exp.fechaLlegadaEta" class="sga-cell-sub sga-text-info">
            ETA: {{ formatDateShort(exp.fechaLlegadaEta) }}
          </span>
        </div>
      </template>
    </Column>

    <Column header="DAM & Canal SUNAT" style="min-width: 150px;">
      <template #body="{ data: exp }">
        <div class="sga-cell-stack">
          <span class="sga-cell-primary font-mono">{{ exp.numeroDam || 'Sin DAM aún' }}</span>
          <Tag
            :value="exp.canalSunat || 'PENDIENTE'"
            :severity="getCanalSeverity(exp.canalSunat)"
            rounded
          />
        </div>
      </template>
    </Column>

    <Column header="Estado Trámite" style="min-width: 135px;">
      <template #body="{ data: exp }">
        <Tag
          :value="formatEstadoAgente(exp.estadoExpediente)"
          :severity="getExpedienteSeverity(exp.estadoExpediente)"
          rounded
        />
      </template>
    </Column>

    <Column header="Tramo Terrestre" style="min-width: 175px;">
      <template #body="{ data: exp }">
        <div v-if="exp.servicioTransporte" class="sga-cell-stack">
          <span class="sga-cell-primary sga-text-info">
            <i class="pi pi-truck"></i>
            {{ isExportacion(exp) ? 'Recojo Planta SGA' : 'Flota SGA' }}
          </span>
          <span v-if="exp.codigoTrackingTransporte" class="sga-cell-sub font-mono">
            {{ exp.codigoTrackingTransporte }} ({{ exp.estadoTransporte || 'PROGRAMADO' }})
          </span>
          <span v-else class="sga-cell-sub sga-text-warn">
            {{ isExportacion(exp) ? 'Pendiente Orden Recojo' : 'Pendiente Envío' }}
          </span>
        </div>
        <div v-else class="sga-cell-stack">
          <span class="sga-cell-muted">
            {{ isExportacion(exp) ? 'Ingreso por Exportador' : 'Retiro en Terminal' }}
          </span>
          <span class="sga-cell-sub">Transporte propio</span>
        </div>
      </template>
    </Column>

    <Column header="Acciones de Campo" headerClass="text-right" bodyClass="text-right" style="min-width: 240px;">
      <template #body="{ data: exp }">
        <AgenteAccionesCell
          :expediente="exp"
          :active-tab="activeTab"
          @salida-origen="$emit('salida-origen', $event)"
          @arribo-destino="$emit('arribo-destino', $event)"
          @transito="$emit('transito', $event)"
          @diligencia="$emit('diligencia', $event)"
          @solicitar-transporte="$emit('solicitar-transporte', $event)"
          @retiro-directo="$emit('retiro-directo', $event)"
          @ingreso-almacen="$emit('ingreso-almacen', $event)"
          @recojo-exportacion="$emit('recojo-exportacion', $event)"
          @ingreso-origen="$emit('ingreso-origen', $event)"
          @embarque="$emit('embarque', $event)"
          @recepcion-exterior="$emit('recepcion-exterior', $event)"
          @verificacion-origen="$emit('verificacion-origen', $event)"
          @cuadrilla-rojo="$emit('cuadrilla-rojo', $event)"
          @finalizar-rojo="$emit('finalizar-rojo', $event)"
          @enviar-sunat="$emit('enviar-sunat', $event)"
          @entregado-almacen-verde="$emit('entregado-almacen-verde', $event)"
          @informe-conformidad="$emit('informe-conformidad', $event)"
          @finalizar-expediente="$emit('finalizar-expediente', $event)"
          @documentos="$emit('documentos', $event)"
          @detalle="$emit('detalle', $event)"
        />
      </template>
    </Column>
  </DataTable>
</template>

<script setup>
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import AgenteAccionesCell from './AgenteAccionesCell.vue'
import { isExportacion } from '../utils/agenteWorkflowUtils'
import {
  formatRegimenShort,
  formatEstadoAgente,
  formatDateShort,
  getCanalSeverity,
  getExpedienteSeverity
} from '../utils/agenteFormatters'

defineProps({
  expedientes: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  activeTab: { type: String, default: 'todos' }
})

const emit = defineEmits([
  'salida-origen',
  'arribo-destino',
  'transito',
  'diligencia',
  'solicitar-transporte',
  'retiro-directo',
  'ingreso-almacen',
  'recojo-exportacion',
  'ingreso-origen',
  'embarque',
  'recepcion-exterior',
  'verificacion-origen',
  'cuadrilla-rojo',
  'finalizar-rojo',
  'enviar-sunat',
  'entregado-almacen-verde',
  'informe-conformidad',
  'finalizar-expediente',
  'documentos',
  'detalle'
])

const onRowDblClick = (event) => {
  if (event?.data) emit('detalle', event.data)
}
</script>

<style scoped>
.sga-code-link {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary);
  cursor: pointer;
  text-align: left;
}

.sga-code-link:hover {
  text-decoration: underline;
}
</style>
